package io.github.evgenius09.threshold.client;

import io.github.evgenius09.threshold.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ThresholdClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.ANATOMY, AnatomyScreen::new);
		ModKeys.init();
	}
}
