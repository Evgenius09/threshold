package io.github.evgenius09.threshold.registry;

import io.github.evgenius09.threshold.Threshold;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	// Proves the mod is loaded; removed once v0.1 adds a real item (tech.md, section 6).
	public static final Item DEBUG_SHARD =
			Registry.register(BuiltInRegistries.ITEM, Threshold.id("debug_shard"), new Item(new Item.Properties()));

	private ModItems() {
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(DEBUG_SHARD));
	}
}
