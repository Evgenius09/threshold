package io.github.evgenius09.threshold;

import io.github.evgenius09.threshold.registry.ModAttachments;
import io.github.evgenius09.threshold.registry.ModItems;
import io.github.evgenius09.threshold.registry.ModMenus;
import io.github.evgenius09.threshold.registry.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Threshold implements ModInitializer {
	public static final String MOD_ID = "threshold_gates";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.init();
		ModAttachments.init();
		ModMenus.init();
		ModNetworking.init();
		LOGGER.info("Threshold loaded");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
