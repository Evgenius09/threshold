package io.github.evgenius09.threshold.menu;

import io.github.evgenius09.threshold.Threshold;
import io.github.evgenius09.threshold.body.BodyData;
import io.github.evgenius09.threshold.core.BodySlot;
import io.github.evgenius09.threshold.item.OrganItem;
import io.github.evgenius09.threshold.registry.ModAttachments;
import io.github.evgenius09.threshold.registry.ModMenus;
import io.github.evgenius09.threshold.system.SystemMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AnatomyMenu extends AbstractContainerMenu {
	private static final int BODY_SLOTS = BodySlot.values().length;
	private static final int[][] CELLS = {{8, 24}, {8, 60}, {152, 24}, {152, 60}};

	public AnatomyMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(BODY_SLOTS));
	}

	public AnatomyMenu(int containerId, Inventory inventory, ServerPlayer owner) {
		this(containerId, inventory, new BodyContainer(owner));
	}

	private AnatomyMenu(int containerId, Inventory inventory, Container body) {
		super(ModMenus.ANATOMY, containerId);
		for (int i = 0; i < BODY_SLOTS; i++) addSlot(new OrganSlot(body, i, CELLS[i][0], CELLS[i][1]));
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + 18 * col, 94 + 18 * row));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + 18 * col, 152));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		boolean toBody = index >= BODY_SLOTS;
		if (toBody && !(stack.getItem() instanceof OrganItem)) return ItemStack.EMPTY;
		if (toBody ? !moveItemStackTo(stack, 0, BODY_SLOTS, false) : !moveItemStackTo(stack, BODY_SLOTS, slots.size(), true))
			return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		// Vanilla repeats quick-move while the result is non-empty, which would fill every free cell from one stack.
		return toBody ? ItemStack.EMPTY : copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.isAlive();
	}

	private static class OrganSlot extends Slot {
		OrganSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.getItem() instanceof OrganItem;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	private static class BodyContainer implements Container {
		private final ServerPlayer player;

		BodyContainer(ServerPlayer player) {
			this.player = player;
		}

		private BodyData data() {
			return player.getAttachedOrCreate(ModAttachments.BODY);
		}

		private void store(List<ItemStack> organs) {
			BodyData d = data();
			BodyData next = new BodyData(organs, d.selected(), d.load(), d.skills(), d.haloTicks(), d.seenOrgan(), d.damageDealt());
			player.setAttached(ModAttachments.BODY, new BodyData(next.organs(), next.body().keepOrSteel(d.selected()),
					d.load(), d.skills(), d.haloTicks(), d.seenOrgan(), d.damageDealt()));
		}

		@Override
		public int getContainerSize() {
			return BODY_SLOTS;
		}

		@Override
		public boolean isEmpty() {
			return data().organs().stream().allMatch(ItemStack::isEmpty);
		}

		// Vanilla expects the live stack here and may shrink it in place.
		@Override
		public ItemStack getItem(int slot) {
			return data().organs().get(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			ItemStack old = getItem(slot);
			List<ItemStack> organs = new ArrayList<>(data().organs());
			organs.set(slot, stack);
			store(organs);
			if (stack.getItem() instanceof OrganItem && (old.isEmpty() || old.getItem() != stack.getItem())) {
				String name = BodySlot.values()[slot].name().toLowerCase(Locale.ROOT);
				SystemMessages.send(player, "graft", stack.getHoverName(), Component.translatable("slot." + Threshold.MOD_ID + "." + name));
			}
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			ItemStack current = getItem(slot);
			if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;
			ItemStack taken = current.split(amount);
			setChanged();
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack current = getItem(slot);
			List<ItemStack> organs = new ArrayList<>(data().organs());
			organs.set(slot, ItemStack.EMPTY);
			store(organs);
			return current;
		}

		// Vanilla shrinks the stack from getItem in place, so every change event writes fresh copies (dead stacks become EMPTY).
		@Override
		public void setChanged() {
			List<ItemStack> organs = new ArrayList<>();
			for (ItemStack stack : data().organs()) organs.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
			store(organs);
		}

		@Override
		public boolean stillValid(Player player) {
			return player.isAlive();
		}

		@Override
		public void clearContent() {
			store(List.of());
		}
	}
}
