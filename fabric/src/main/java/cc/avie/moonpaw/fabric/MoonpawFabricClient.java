package cc.avie.moonpaw.fabric;

import cc.avie.moonpaw.Moonpaw;
import cc.avie.moonpaw.client.MoonpawClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class MoonpawFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		String version = FabricLoader.getInstance().getModContainer(Moonpaw.MOD_ID).orElseThrow()
			.getMetadata().getVersion().getFriendlyString();
		MoonpawClient.init(version);
		ClientTickEvents.END_CLIENT_TICK.register(MoonpawClient::tick);
	}
}
