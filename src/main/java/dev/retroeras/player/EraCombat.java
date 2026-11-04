package dev.retroeras.player;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Combat as it was before 1.9 (see PlayerCombatMixin for the hooks into {@link Player#attack}).
 * <ul>
 * <li>No attack cooldown: every click is a full-strength hit, and the attack indicator never shows.</li>
 * <li>No sweep attacks, and no offhand: anything put there goes back into the inventory.</li>
 * <li>Critical hits only from Beta 1.8 on, so only in Old Release.</li>
 * <li>Weapons and tools deal their era's damage. Beta and Infdev swords dealt 4 + 2 per tier, 1.6.4's 4 + 1 per
 * tier; pickaxes, axes and shovels dealt 2, 3 and 1 plus their tier in all of them, and everything else 1.</li>
 * </ul>
 */
public final class EraCombat {
	private static final Identifier NO_COOLDOWN = RetroEras.id("no_attack_cooldown");

	private EraCombat() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Era era = eraOf(player);
				setNoCooldown(player, era != null);
				if (era != null) {
					emptyOffhand(player);
				}
			}
		});
	}

	static @Nullable Era eraOf(Player player) {
		return Era.fromDimension(player.level().dimension());
	}

	/** Whether this player is in an era, so 1.9's combat rules (sweeps, cooldowns) do not apply. */
	public static boolean usesOldCombat(Player player) {
		return eraOf(player) != null;
	}

	/** Critical hits were added in Beta 1.8. */
	public static boolean canCrit(Player player) {
		Era era = eraOf(player);
		return era == null || era == Era.OLD_RELEASE;
	}

	/**
	 * Replaces the held item's modern damage with its era damage, keeping everything else (strength, weakness) that
	 * went into {@code modern}.
	 */
	public static double attackDamage(Player player, double modern) {
		Era era = eraOf(player);
		if (era == null) {
			return modern;
		}
		ItemStack stack = player.getMainHandItem();
		double eraBonus = eraDamage(era, stack.getItem()) - 1.0;
		double modernBonus = 0.0;
		for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(Attributes.ATTACK_DAMAGE) && entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)
					&& entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
				modernBonus += entry.modifier().amount();
			}
		}
		return Math.max(0.0, modern - modernBonus + eraBonus);
	}

	/** Damage in half-hearts that the item dealt in that era, counting the fist's 1. */
	public static double eraDamage(Era era, Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		int tier;
		if (path.startsWith("wooden_") || path.startsWith("golden_")) {
			tier = 0;
		} else if (path.startsWith("stone_")) {
			tier = 1;
		} else if (path.startsWith("iron_")) {
			tier = 2;
		} else if (path.startsWith("diamond_")) {
			tier = 3;
		} else {
			return 1.0;
		}
		if (path.endsWith("_sword")) {
			return era == Era.OLD_RELEASE ? 4 + tier : 4 + 2 * tier;
		} else if (path.endsWith("_axe")) {
			return 3 + tier;
		} else if (path.endsWith("_pickaxe")) {
			return 2 + tier;
		} else if (path.endsWith("_shovel")) {
			return 1 + tier;
		}
		return 1.0;
	}

	private static void setNoCooldown(ServerPlayer player, boolean enabled) {
		AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
		if (attackSpeed == null || attackSpeed.hasModifier(NO_COOLDOWN) == enabled) {
			return;
		}
		if (enabled) {
			// The cooldown lasts 20 / attack speed ticks, so this makes it a fraction of a tick.
			attackSpeed.addTransientModifier(new AttributeModifier(NO_COOLDOWN, 1000.0, AttributeModifier.Operation.ADD_VALUE));
		} else {
			attackSpeed.removeModifier(NO_COOLDOWN);
		}
	}

	/** The offhand came with 1.9; whatever lands there goes back into the main inventory (or is dropped). */
	private static void emptyOffhand(ServerPlayer player) {
		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) {
			return;
		}
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.getInventory().placeItemBackInInventory(offhand, Prediction.SERVER_ONLY);
	}
}
