package cc.avie.moonpaw.client;

import cc.avie.moonpaw.client.presence.DiscordPresence;
import cc.avie.moonpaw.client.presence.MoonpawPresence;
import net.minecraft.client.Minecraft;

public final class MoonpawClient {
	private static final int PRESENCE_UPDATE_TICKS = 20;

	private static String modVersion;
	private static long startedAt;
	private static DiscordPresence presence;
	private static int ticks;

	private MoonpawClient() {
	}

	public static void init(String version) {
		modVersion = version;
		startedAt = System.currentTimeMillis() / 1000;
		presence = DiscordPresence.start(MoonpawPresence.APPLICATION_ID);
	}

	/** Called by the loader at the end of every client tick. */
	public static void tick(Minecraft minecraft) {
		// The translations aren't there yet while the game is still loading
		if (++ticks % PRESENCE_UPDATE_TICKS == 0 && minecraft.isGameLoadFinished()) {
			presence.show(MoonpawPresence.current(minecraft, modVersion, startedAt));
		}
	}
}
