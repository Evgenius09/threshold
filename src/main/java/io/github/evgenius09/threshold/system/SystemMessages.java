package io.github.evgenius09.threshold.system;

import io.github.evgenius09.threshold.Threshold;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SystemMessages {
	private SystemMessages() {
	}

	public static void send(ServerPlayer player, String id, Object... args) {
		player.sendSystemMessage(Component.translatable("message." + Threshold.MOD_ID + ".system." + id, args)
				.withStyle(style -> style.withColor(0xA8B4C0)));
	}
}
