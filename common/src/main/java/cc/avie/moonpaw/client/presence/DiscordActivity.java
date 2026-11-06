package cc.avie.moonpaw.client.presence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * The status shown on a Discord profile. Discord wants 2 to 128 characters in details and state.
 *
 * @param startedAt unix time in seconds, Discord shows the time since then
 */
public record DiscordActivity(
	String details,
	String state,
	String largeImage,
	String largeImageText,
	long startedAt,
	List<Button> buttons
) {
	/** Discord allows two of these and only shows them to other people, not on your own profile. */
	public record Button(String label, String url) {
	}

	JsonObject toJson() {
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", largeImage);
		assets.addProperty("large_text", largeImageText);

		JsonObject timestamps = new JsonObject();
		timestamps.addProperty("start", startedAt);

		JsonArray buttonsJson = new JsonArray();
		for (Button button : buttons) {
			JsonObject buttonJson = new JsonObject();
			buttonJson.addProperty("label", button.label());
			buttonJson.addProperty("url", button.url());
			buttonsJson.add(buttonJson);
		}

		JsonObject activity = new JsonObject();
		activity.addProperty("details", details);
		activity.addProperty("state", state);
		activity.add("assets", assets);
		activity.add("timestamps", timestamps);
		activity.add("buttons", buttonsJson);
		return activity;
	}
}
