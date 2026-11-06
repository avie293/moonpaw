package cc.avie.moonpaw.neoforge;

import cc.avie.moonpaw.Moonpaw;
import cc.avie.moonpaw.client.MoonpawClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Moonpaw.MOD_ID, dist = Dist.CLIENT)
public class MoonpawNeoForgeClient {
	public MoonpawNeoForgeClient(ModContainer container) {
		MoonpawClient.init(container.getModInfo().getVersion().toString());
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> MoonpawClient.tick(Minecraft.getInstance()));
	}
}
