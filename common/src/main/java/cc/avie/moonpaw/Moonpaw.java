package cc.avie.moonpaw;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Moonpaw {
	public static final String MOD_ID = "moonpaw";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Moonpaw() {
	}

	/** Runs on the client and on dedicated servers. */
	public static void init() {
		LOGGER.info("Moonpaw is awake");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
