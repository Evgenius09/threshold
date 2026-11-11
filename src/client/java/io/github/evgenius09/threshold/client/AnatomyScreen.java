package io.github.evgenius09.threshold.client;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.item.OrganItem;
import io.github.evgenius09.threshold.menu.AnatomyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class AnatomyScreen extends AbstractContainerScreen<AnatomyMenu> {
	private static final ResourceLocation TEXTURE = Threshold.id("textures/gui/anatomy.png");
	private static final String[] SLOT_KEYS = {"arms", "legs", "back", "core"};
	private static final int LABEL_COLOR = 0xC0C0C8;

	public AnatomyScreen(AnatomyMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 176;
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		for (int i = 0; i < SLOT_KEYS.length; i++) {
			Slot slot = menu.slots.get(i);
			if (slot.getItem().getItem() instanceof OrganItem organ) {
				int x = leftPos + slot.x - 1;
				int y = topPos + slot.y - 1;
				graphics.fill(x, y, x + 18, y + 18, 0x80000000 | organ.organ().element().color());
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		for (int i = 0; i < SLOT_KEYS.length; i++) {
			Component label = Component.translatable("slot." + Threshold.MOD_ID + "." + SLOT_KEYS[i]);
			Slot slot = menu.slots.get(i);
			// Left cells get labels to their right, right cells to their left, so text never covers the silhouette edge.
			int x = slot.x < imageWidth / 2 ? 28 : 148 - font.width(label);
			graphics.drawString(font, label, x, slot.y + 5, LABEL_COLOR, false);
		}
	}

	// Screen.render already draws the background; a second renderBackground call would blur twice and crash in 1.21.
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}
}
