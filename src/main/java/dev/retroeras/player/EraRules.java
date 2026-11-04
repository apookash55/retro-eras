package dev.retroeras.player;

import dev.retroeras.Era;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Gameplay rules for eras that predate the hunger system (everything before Beta 1.8): no hunger, no sprinting, no
 * natural regeneration, and food heals instantly instead (see FoodPropertiesMixin).
 */
public final class EraRules {
	/**
	 * Food level held constant in hunger-free eras. Vanilla needs more than 6 to sprint and at least 18 to regenerate,
	 * and only starves at 0, so 6 turns all three off.
	 */
	private static final int HUNGERLESS_FOOD_LEVEL = 6;

	private EraRules() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (Era.fromDimension(level.dimension()) == null) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				FoodData food = player.getFoodData();
				food.setFoodLevel(HUNGERLESS_FOOD_LEVEL);
				food.setSaturation(0.0F);
			}
		});
	}

	public static boolean hasHunger(Player player) {
		return Era.fromDimension(player.level().dimension()) == null;
	}
}
