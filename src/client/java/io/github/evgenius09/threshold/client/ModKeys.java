package io.github.evgenius09.threshold.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.network.OpenAnatomyPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
	private ModKeys() {
	}

	public static void init() {
		KeyMapping anatomy = KeyBindingHelper.registerKeyBinding(new KeyMapping("key." + Threshold.MOD_ID + ".anatomy",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories." + Threshold.MOD_ID));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (anatomy.consumeClick())
				if (client.player != null) ClientPlayNetworking.send(OpenAnatomyPayload.INSTANCE);
		});
	}
}
