package io.github.evgenius09.threshold.registry;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.core.Organ;
import io.github.evgenius09.threshold.item.OrganItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item SHADOW_GLAND = register("shadow_gland", new OrganItem(Organ.SHADOW_GLAND));
	public static final Item THUNDER_FANG = register("thunder_fang", new OrganItem(Organ.THUNDER_FANG));

	private ModItems() {
	}

	private static Item register(String id, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, Threshold.id(id), item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(SHADOW_GLAND);
			entries.accept(THUNDER_FANG);
		});
	}
}
