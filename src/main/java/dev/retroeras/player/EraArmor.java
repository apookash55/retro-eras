package dev.retroeras.player;


import dev.retroeras.Era;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;

/**
 * How armour protected players in the eras. Old armour took 4% off a hit per armour point, flat: a hit is cut to
 * damage x (25 - armour) / 25, with no toughness and no letting heavy blows through. In 1.6.4 the points were those of
 * each piece, as now. Before 1.0 every material gave the same points (3 for a helmet, 8 for a chestplate, 6 for
 * leggings and 3 for boots) and only lasted longer or shorter; the total shrank as the pieces wore down
 * (InventoryPlayer.getTotalArmorValue).
 */
public final class EraArmor {
	private EraArmor() {
	}

	/**
	 * The player's armour points by the era's rules, or -1 to keep 26.3's. A hit is then cut to damage x (25 - points) / 25.
	 */
	public static int points(Player player) {
		Era era = Era.fromDimension(player.level().dimension());
		if (era == null || !era.isSurvival()) {
			return -1;
		}
		return era.hasHunger() ? player.getArmorValue() : betaArmour(player);
	}

	/** Before 1.0 no damage got past armour: it softened falls, fire and drowning too. */
	public static boolean softensEverything(Player player) {
		Era era = Era.fromDimension(player.level().dimension());
		return era != null && era.isSurvival() && !era.hasHunger();
	}

	/** Beta's total: the slots' fixed points, scaled by how much durability the pieces have left. */
	private static int betaArmour(Player player) {
		int points = 0;
		int left = 0;
		int full = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = player.getItemBySlot(slot);
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			if (stack.isEmpty() || equippable == null || equippable.slot() != slot || !stack.isDamageableItem()) {
				continue;
			}
			left += stack.getMaxDamage() - stack.getDamageValue();
			full += stack.getMaxDamage();
			points += switch (slot) {
				case HEAD, FEET -> 3;
				case CHEST -> 8;
				default -> 6;
			};
		}
		return full == 0 ? 0 : (points - 1) * left / full + 1;
	}
}
