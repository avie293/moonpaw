package cc.avie.moonpaw.client.presence;

import cc.avie.moonpaw.Moonpaw;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Talks to the local Discord app. A message is a frame of two little endian ints (opcode and
 * length) followed by JSON. On Windows it runs over a named pipe, everywhere else over a unix socket.
 *
 * <p>Not thread safe and only one thread may use it: a named pipe blocks a write while another
 * thread is reading, so every request reads its answer before the next one is sent.
 */
final class DiscordIpcClient implements Closeable {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private static final int OP_CLOSE = 2;
	private static final int OP_PING = 3;
	private static final int OP_PONG = 4;

	private static final int HEADER_BYTES = 8;
	private static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
	/** Discord uses one pipe per running app, numbered from 0 to 9. */
	private static final int PIPE_COUNT = 10;
	private static final int CLOSE_INVALID_CLIENT_ID = 4000;

	private final DataInputStream in;
	private final OutputStream out;
	private final Closeable connection;

	DiscordIpcClient(InputStream in, OutputStream out, Closeable connection) {
		this.in = new DataInputStream(in);
		this.out = out;
		this.connection = connection;
	}

	/** Opens the first pipe of a running Discord. Fails if there is none. */
	static DiscordIpcClient open() throws IOException {
		boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
		for (int pipe = 0; pipe < PIPE_COUNT; pipe++) {
			DiscordIpcClient client = windows ? openWindows(pipe) : openUnix(pipe);
			if (client != null) {
				return client;
			}
		}
		throw new IOException("Discord isn't running");
	}

	private static DiscordIpcClient openWindows(int pipe) {
		try {
			RandomAccessFile file = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + pipe, "rw");
			return new DiscordIpcClient(new FileInputStream(file.getFD()), new FileOutputStream(file.getFD()), file);
		} catch (IOException e) {
			return null;
		}
	}

	private static DiscordIpcClient openUnix(int pipe) {
		// The Snap and Flatpak versions of Discord put their socket into a subfolder
		for (String folder : List.of("", "app/com.discordapp.Discord/", "snap.discord/")) {
			for (Path base : unixBaseFolders()) {
				Path socket = base.resolve(folder + "discord-ipc-" + pipe);
				if (!Files.exists(socket)) {
					continue;
				}
				try {
					SocketChannel channel = SocketChannel.open(UnixDomainSocketAddress.of(socket));
					return new DiscordIpcClient(Channels.newInputStream(channel), Channels.newOutputStream(channel), channel);
				} catch (IOException ignored) {
					// A leftover socket of a Discord that isn't running anymore
				}
			}
		}
		return null;
	}

	private static List<Path> unixBaseFolders() {
		return Stream.of(
				System.getenv("XDG_RUNTIME_DIR"),
				System.getenv("TMPDIR"),
				System.getenv("TMP"),
				System.getenv("TEMP"),
				"/tmp"
			)
			.filter(folder -> folder != null && !folder.isEmpty())
			.map(Path::of)
			.toList();
	}

	void handshake(String applicationId) throws IOException {
		JsonObject handshake = new JsonObject();
		handshake.addProperty("v", 1);
		handshake.addProperty("client_id", applicationId);
		write(OP_HANDSHAKE, handshake);

		try {
			readFrame();
		} catch (DiscordClosedException e) {
			if (e.code == CLOSE_INVALID_CLIENT_ID) {
				throw new InvalidApplicationIdException(applicationId);
			}
			throw e;
		}
	}

	void setActivity(DiscordActivity activity, long processId) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", processId);
		args.add("activity", activity.toJson());

		String nonce = UUID.randomUUID().toString();
		JsonObject command = new JsonObject();
		command.addProperty("cmd", "SET_ACTIVITY");
		command.add("args", args);
		command.addProperty("nonce", nonce);
		write(OP_FRAME, command);

		JsonObject answer;
		do {
			answer = readFrame();
		} while (!answer.has("nonce") || answer.get("nonce").isJsonNull() || !nonce.equals(answer.get("nonce").getAsString()));

		// A rejected activity is the fault of this one request, the connection is still fine
		if (answer.has("evt") && !answer.get("evt").isJsonNull() && "ERROR".equals(answer.get("evt").getAsString())) {
			Moonpaw.LOGGER.warn("Discord rejected the activity: {}", answer.get("data"));
		}
	}

	/** Reads until there is a normal frame, pings are answered on the way. */
	private JsonObject readFrame() throws IOException {
		while (true) {
			byte[] header = new byte[HEADER_BYTES];
			in.readFully(header);
			ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
			int opcode = headerBuffer.getInt();
			int length = headerBuffer.getInt();
			if (length < 0 || length > MAX_PAYLOAD_BYTES) {
				throw new IOException("Invalid frame length from Discord: " + length);
			}

			byte[] payload = new byte[length];
			in.readFully(payload);

			switch (opcode) {
				case OP_FRAME -> {
					return parse(payload);
				}
				case OP_PING -> writeRaw(OP_PONG, payload);
				case OP_CLOSE -> {
					JsonObject close = parse(payload);
					throw new DiscordClosedException(
						close.has("code") ? close.get("code").getAsInt() : 0,
						close.has("message") ? close.get("message").getAsString() : ""
					);
				}
				default -> {
					// Nothing else is expected, skipping it keeps the connection in sync
				}
			}
		}
	}

	private static JsonObject parse(byte[] payload) throws IOException {
		try {
			return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (RuntimeException e) {
			throw new IOException("Invalid JSON from Discord", e);
		}
	}

	private void write(int opcode, JsonObject json) throws IOException {
		writeRaw(opcode, json.toString().getBytes(StandardCharsets.UTF_8));
	}

	private void writeRaw(int opcode, byte[] payload) throws IOException {
		// One single write: on a pipe in message mode Discord would read a split frame as two messages
		ByteBuffer frame = ByteBuffer.allocate(HEADER_BYTES + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		frame.putInt(opcode).putInt(payload.length).put(payload);
		out.write(frame.array());
		out.flush();
	}

	@Override
	public void close() throws IOException {
		connection.close();
	}

	/** Discord closed the connection on purpose. */
	static class DiscordClosedException extends EOFException {
		final int code;

		DiscordClosedException(int code, String message) {
			super("Discord closed the connection: " + code + " " + message);
			this.code = code;
		}
	}

	/** Retrying is pointless, this application doesn't exist. */
	static final class InvalidApplicationIdException extends IOException {
		InvalidApplicationIdException(String applicationId) {
			super("Discord doesn't know the application " + applicationId);
		}
	}
}
