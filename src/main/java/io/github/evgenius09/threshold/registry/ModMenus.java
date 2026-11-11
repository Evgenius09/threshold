package io.github.evgenius09.threshold.registry;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.menu.AnatomyMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<AnatomyMenu> ANATOMY = Registry.register(BuiltInRegistries.MENU, Threshold.id("anatomy"),
			new MenuType<>(AnatomyMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}

	// Touching the class runs the static registration above.
	public static void init() {
	}
}
