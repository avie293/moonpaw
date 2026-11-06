package cc.avie.moonpaw.client.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordIpcClientTest {
	private static final DiscordActivity ACTIVITY = new DiscordActivity(
		"Im Hauptmenü", "Werwolf ist in Entwicklung", "logo", "Moonpaw 1.0.0", 1234,
		List.of(new DiscordActivity.Button("Discord", "https://example.com"))
	);

	private static byte[] frame(int opcode, String json) {
		byte[] payload = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode).putInt(payload.length).put(payload);
		return buffer.array();
	}

	private record Frame(int opcode, String payload) {
		JsonObject json() {
			return JsonParser.parseString(payload).getAsJsonObject();
		}
	}

	private static Frame readFrame(InputStream in) throws IOException {
		byte[] header = in.readNBytes(8);
		ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
		int opcode = buffer.getInt();
		int length = buffer.getInt();
		return new Frame(opcode, new String(in.readNBytes(length), StandardCharsets.UTF_8));
	}

	private static DiscordIpcClient client(byte[] answers, OutputStream out) {
		return new DiscordIpcClient(new ByteArrayInputStream(answers), out, () -> { });
	}

	@Test
	void handshakeSendsVersionAndApplicationId() throws IOException {
		ByteArrayOutputStream written = new ByteArrayOutputStream();

		client(frame(1, "{\"evt\":\"READY\"}"), written).handshake("123");

		Frame handshake = readFrame(new ByteArrayInputStream(written.toByteArray()));
		assertEquals(0, handshake.opcode());
		assertEquals(1, handshake.json().get("v").getAsInt());
		assertEquals("123", handshake.json().get("client_id").getAsString());
	}

	@Test
	void unknownApplicationIsReportedSoItIsNotRetried() {
		byte[] close = frame(2, "{\"code\":4000,\"message\":\"Invalid Client ID\"}");

		assertThrows(DiscordIpcClient.InvalidApplicationIdException.class,
			() -> client(close, new ByteArrayOutputStream()).handshake("123"));
	}

	@Test
	void otherClosesDuringHandshakeCanBeRetried() {
		byte[] close = frame(2, "{\"code\":1000,\"message\":\"Bye\"}");

		IOException thrown = assertThrows(IOException.class,
			() -> client(close, new ByteArrayOutputStream()).handshake("123"));
		assertEquals(DiscordIpcClient.DiscordClosedException.class, thrown.getClass());
	}

	@Test
	void lostConnectionIsAnIoException() {
		assertThrows(EOFException.class,
			() -> client(new byte[0], new ByteArrayOutputStream()).handshake("123"));
	}

	@Test
	void absurdFrameLengthIsRejected() {
		ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		header.putInt(1).putInt(Integer.MAX_VALUE);

		assertThrows(IOException.class,
			() -> client(header.array(), new ByteArrayOutputStream()).handshake("123"));
	}

	@Test
	void setActivitySendsTheActivityAndWaitsForItsAnswer() throws Exception {
		// Discord answers with the nonce of the request, so the fake has to read it first
		PipedOutputStream toClient = new PipedOutputStream();
		PipedInputStream clientIn = new PipedInputStream(toClient);
		PipedInputStream fromClient = new PipedInputStream();
		PipedOutputStream clientOut = new PipedOutputStream(fromClient);
		DiscordIpcClient client = new DiscordIpcClient(clientIn, clientOut, () -> { });

		Frame[] received = new Frame[2];
		Thread discord = new Thread(() -> {
			try {
				received[0] = readFrame(fromClient);
				String nonce = received[0].json().get("nonce").getAsString();
				// Things that aren't the answer have to be skipped, the ping has to get its pong
				toClient.write(frame(1, "{\"evt\":\"SOMETHING\",\"nonce\":null}"));
				toClient.write(frame(3, "{\"ping\":1}"));
				received[1] = readFrame(fromClient);
				toClient.write(frame(1, "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + nonce + "\"}"));
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		});
		discord.start();

		client.setActivity(ACTIVITY, 42);
		discord.join();

		assertEquals(1, received[0].opcode());
		JsonObject command = received[0].json();
		assertEquals("SET_ACTIVITY", command.get("cmd").getAsString());
		JsonObject args = command.getAsJsonObject("args");
		assertEquals(42, args.get("pid").getAsLong());
		JsonObject activity = args.getAsJsonObject("activity");
		assertEquals("Im Hauptmenü", activity.get("details").getAsString());
		assertEquals("Werwolf ist in Entwicklung", activity.get("state").getAsString());
		assertEquals("logo", activity.getAsJsonObject("assets").get("large_image").getAsString());
		assertEquals("Moonpaw 1.0.0", activity.getAsJsonObject("assets").get("large_text").getAsString());
		assertEquals(1234, activity.getAsJsonObject("timestamps").get("start").getAsLong());
		assertEquals("Discord", activity.getAsJsonArray("buttons").get(0).getAsJsonObject().get("label").getAsString());

		assertEquals(4, received[1].opcode());
		assertEquals("{\"ping\":1}", received[1].payload());
	}

	@Test
	void rejectedActivityKeepsTheConnection() throws Exception {
		PipedOutputStream toClient = new PipedOutputStream();
		PipedInputStream clientIn = new PipedInputStream(toClient);
		PipedInputStream fromClient = new PipedInputStream();
		PipedOutputStream clientOut = new PipedOutputStream(fromClient);
		DiscordIpcClient client = new DiscordIpcClient(clientIn, clientOut, () -> { });

		Thread discord = new Thread(() -> {
			try {
				String nonce = readFrame(fromClient).json().get("nonce").getAsString();
				toClient.write(frame(1, "{\"evt\":\"ERROR\",\"nonce\":\"" + nonce + "\",\"data\":{\"code\":4000,\"message\":\"nope\"}}"));
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		});
		discord.start();

		client.setActivity(ACTIVITY, 42);
		discord.join();
	}
}
