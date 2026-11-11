package io.github.evgenius09.threshold.registry;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.menu.AnatomyMenu;
import io.github.evgenius09.threshold.network.OpenAnatomyPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(OpenAnatomyPayload.TYPE, OpenAnatomyPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenAnatomyPayload.TYPE, (payload, context) ->
				context.player().openMenu(new SimpleMenuProvider((id, inventory, player) -> new AnatomyMenu(id, inventory, context.player()),
						Component.translatable("container." + Threshold.MOD_ID + ".anatomy"))));
	}
}
