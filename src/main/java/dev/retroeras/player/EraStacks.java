package dev.retroeras.player;

import java.util.Map;

import dev.retroeras.Era;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Old stack sizes. Before Beta 1.8 food that healed you did not stack, cookies stacked to eight, and signs and doors did
 * not stack; doors only stacked from 1.8, so not in 1.6.4 either. 26.3 cannot make a stack size depend on where you
 * are, so stacks in an era inventory are given the era's limit, and anything over it is split into further slots, or
 * dropped if there is no room.
 */
public final class EraStacks {
	private static final Map<Item, Integer> BEFORE_BETA_1_8 = Map.ofEntries(Map.entry(Items.APPLE, 1), Map.entry(Items.GOLDEN_APPLE, 1),
			Map.entry(Items.BREAD, 1), Map.entry(Items.PORKCHOP, 1), Map.entry(Items.COOKED_PORKCHOP, 1), Map.entry(Items.COD, 1),
			Map.entry(Items.COOKED_COD, 1), Map.entry(Items.MUSHROOM_STEW, 1), Map.entry(Items.COOKIE, 8), Map.entry(Items.OAK_SIGN, 1),
			Map.entry(Items.OAK_DOOR, 1), Map.entry(Items.IRON_DOOR, 1));
	private static final Map<Item, Integer> RELEASE_1_6_4 = Map.of(Items.OAK_DOOR, 1, Items.IRON_DOOR, 1);

	private EraStacks() {
	}

	/** The era's stack size for {@code item}, or 0 if it is the same as 26.3's. */
	public static int limit(Era era, Item item) {
		if (!era.isSurvival()) {
			return 0;
		}
		return (era.hasHunger() ? RELEASE_1_6_4 : BEFORE_BETA_1_8).getOrDefault(item, 0);
	}

	/** Gives the player's stacks the era's limits, and splits any that are over them. */
	public static void apply(ServerPlayer player, Era era) {
		Inventory inventory = player.getInventory();
		boolean changed = false;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);
			int limit = stack.isEmpty() ? 0 : limit(era, stack.getItem());
			if (limit == 0) {
				continue;
			}
			Integer current = stack.get(DataComponents.MAX_STACK_SIZE);
			if (current == null || current != limit) {
				stack.set(DataComponents.MAX_STACK_SIZE, limit);
				changed = true;
			}
			if (stack.getCount() > limit) {
				ItemStack excess = stack.split(stack.getCount() - limit);
				while (!excess.isEmpty()) {
					ItemStack part = excess.split(limit);
					int free = inventory.getFreeSlot();
					if (free >= 0) {
						inventory.setItem(free, part);
					} else {
						player.drop(part, false, Prediction.SERVER_ONLY);
					}
				}
				changed = true;
			}
		}
		if (changed) {
			player.containerMenu.broadcastChanges();
		}
	}
}
