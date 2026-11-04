package dev.retroeras.client;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Hides HUD elements that did not exist yet in the era the player is in: the hunger and experience bars in every era,
 * and the hearts, armor and air bars in eras without survival.
 */
public final class EraHud {
	private EraHud() {
	}

	public static void init() {
		hideWhen(VanillaHudElements.FOOD_BAR, era -> true);
		hideWhen(VanillaHudElements.EXPERIENCE_LEVEL, era -> true);
		hideWhen(VanillaHudElements.INFO_BAR, era -> true);
		hideWhen(VanillaHudElements.HEALTH_BAR, era -> !era.isSurvival());
		hideWhen(VanillaHudElements.ARMOR_BAR, era -> !era.isSurvival());
		hideWhen(VanillaHudElements.AIR_BAR, era -> !era.isSurvival());
	}

	private static void hideWhen(Identifier element, Predicate<Era> hidden) {
		HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
			Era era = currentEra();
			if (era == null || !hidden.test(era)) {
				original.extractRenderState(graphics, delta);
			}
		});
	}

	private static @Nullable Era currentEra() {
		Minecraft client = Minecraft.getInstance();
		return client.level == null ? null : Era.fromDimension(client.level.dimension());
	}
}
