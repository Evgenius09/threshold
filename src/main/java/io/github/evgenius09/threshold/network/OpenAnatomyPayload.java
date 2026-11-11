package io.github.evgenius09.threshold.network;

import io.github.evgenius09.threshold.Threshold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenAnatomyPayload() implements CustomPacketPayload {
	public static final OpenAnatomyPayload INSTANCE = new OpenAnatomyPayload();
	public static final CustomPacketPayload.Type<OpenAnatomyPayload> TYPE = new CustomPacketPayload.Type<>(Threshold.id("open_anatomy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenAnatomyPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
