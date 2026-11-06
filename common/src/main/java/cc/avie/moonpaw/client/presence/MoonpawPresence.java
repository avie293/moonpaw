package cc.avie.moonpaw.client.presence;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;

/**
 * What Moonpaw shows on Discord. This is the place to extend whenever the mod gets something new
 * that players can see or do. A Discord status is public, so it must never show hidden roles,
 * night targets, server addresses or world names.
 */
public final class MoonpawPresence {
	/** From the Discord Developer Portal. The presence stays off while this is empty. */
	public static final String APPLICATION_ID = "1568275950104346756";

	private static final String LOGO = "logo";
	private static final List<DiscordActivity.Button> BUTTONS = List.of(
		new DiscordActivity.Button("Discord", "https://discord.gg/57TZ8BJvd9")
	);

	private MoonpawPresence() {
	}

	public static DiscordActivity current(Minecraft minecraft, String version, long startedAt) {
		return new DiscordActivity(
			details(minecraft),
			state(minecraft),
			LOGO,
			"Moonpaw " + version,
			startedAt,
			BUTTONS
		);
	}

	private static String details(Minecraft minecraft) {
		if (minecraft.level == null) {
			return I18n.get("moonpaw.presence.menu");
		}
		if (minecraft.hasSingleplayerServer()) {
			return I18n.get(minecraft.getSingleplayerServer().isPublished()
				? "moonpaw.presence.lan"
				: "moonpaw.presence.singleplayer");
		}
		return I18n.get("moonpaw.presence.multiplayer");
	}

	private static String state(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		boolean alone = minecraft.hasSingleplayerServer() && !minecraft.getSingleplayerServer().isPublished();
		if (minecraft.level == null || connection == null || alone) {
			return I18n.get("moonpaw.presence.stage");
		}

		int players = connection.getOnlinePlayers().size();
		return players == 1
			? I18n.get("moonpaw.presence.players.one")
			: I18n.get("moonpaw.presence.players.many", players);
	}
}
