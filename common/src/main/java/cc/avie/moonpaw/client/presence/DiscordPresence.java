package cc.avie.moonpaw.client.presence;

import cc.avie.moonpaw.Moonpaw;
import cc.avie.moonpaw.client.presence.DiscordIpcClient.InvalidApplicationIdException;
import java.io.IOException;

/**
 * Keeps the status on Discord up to date from its own thread, so a slow or missing Discord never
 * blocks the game. It reconnects when Discord gets started later or restarts. There is no need to
 * clean up on exit: Discord removes the status when the connection of the game closes.
 */
public final class DiscordPresence {
	private static final long POLL_MILLIS = 500;
	private static final long RECONNECT_MILLIS = 15_000;
	/** Discord allows 5 updates in 20 seconds. */
	private static final long MIN_UPDATE_MILLIS = 5_000;
	/** A write is the only way to notice that Discord went away. */
	private static final long KEEPALIVE_MILLIS = 60_000;

	private final String applicationId;
	private volatile DiscordActivity wanted;

	private DiscordPresence(String applicationId) {
		this.applicationId = applicationId;
	}

	/** With an empty id nothing connects and {@link #show} does nothing. */
	public static DiscordPresence start(String applicationId) {
		DiscordPresence presence = new DiscordPresence(applicationId);
		if (applicationId.isEmpty()) {
			Moonpaw.LOGGER.info("Discord presence is off, there is no application id set");
		} else {
			Thread thread = new Thread(presence::run, "Moonpaw Discord Presence");
			thread.setDaemon(true);
			thread.start();
		}
		return presence;
	}

	/** Shows this activity as soon as possible, a newer one replaces it if it is still waiting. */
	public void show(DiscordActivity activity) {
		wanted = activity;
	}

	private void run() {
		try {
			while (true) {
				try (DiscordIpcClient client = DiscordIpcClient.open()) {
					client.handshake(applicationId);
					Moonpaw.LOGGER.info("Connected to Discord");
					keepUpdating(client);
				} catch (InvalidApplicationIdException e) {
					Moonpaw.LOGGER.warn("Discord presence is off: {}", e.getMessage());
					return;
				} catch (IOException e) {
					// The usual case is Discord not running, which isn't worth more than a debug line
					Moonpaw.LOGGER.debug("No connection to Discord: {}", e.getMessage());
				}
				Thread.sleep(RECONNECT_MILLIS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Only returns by throwing, when the connection is gone. */
	private void keepUpdating(DiscordIpcClient client) throws IOException, InterruptedException {
		long processId = ProcessHandle.current().pid();
		DiscordActivity sent = null;
		long sentAt = 0;

		while (true) {
			DiscordActivity activity = wanted;
			long sinceSent = System.currentTimeMillis() - sentAt;
			boolean changed = !java.util.Objects.equals(activity, sent) && sinceSent >= MIN_UPDATE_MILLIS;
			boolean keepalive = sinceSent >= KEEPALIVE_MILLIS;
			if (activity != null && (changed || keepalive)) {
				client.setActivity(activity, processId);
				sent = activity;
				sentAt = System.currentTimeMillis();
			}
			Thread.sleep(POLL_MILLIS);
		}
	}
}
