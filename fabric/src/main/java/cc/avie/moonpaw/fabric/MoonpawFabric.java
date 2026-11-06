package cc.avie.moonpaw.fabric;

import cc.avie.moonpaw.Moonpaw;
import net.fabricmc.api.ModInitializer;

public class MoonpawFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Moonpaw.init();
	}
}
