package io.github.evgenius09.threshold.item;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.core.Element;
import io.github.evgenius09.threshold.core.Organ;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class OrganItem extends Item {
	private final Organ organ;

	public OrganItem(Organ organ) {
		super(new Item.Properties().stacksTo(16));
		this.organ = organ;
	}

	public Organ organ() {
		return organ;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Element element = organ.element();
		Component name = Component.translatable("element." + Threshold.MOD_ID + "." + element.name().toLowerCase(Locale.ROOT));
		// The whole line carries the element color, not only the element name.
		tooltip.add(Component.translatable("item." + Threshold.MOD_ID + ".organ.element", name)
				.withStyle(style -> style.withColor(element.color())));
		tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
