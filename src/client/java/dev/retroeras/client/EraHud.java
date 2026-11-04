package dev.retroeras.client;

import dev.retroeras.Era;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Hides HUD elements that did not exist yet in the era the player is in: the hunger bar and the experience bar. */
public final class EraHud {
	private EraHud() {
	}

	public static void init() {
		hideInEras(VanillaHudElements.FOOD_BAR);
		hideInEras(VanillaHudElements.EXPERIENCE_LEVEL);
		hideInEras(VanillaHudElements.INFO_BAR);
	}

	private static void hideInEras(Identifier element) {
		HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
			if (!inEra()) {
				original.extractRenderState(graphics, delta);
			}
		});
	}

	private static boolean inEra() {
		Minecraft client = Minecraft.getInstance();
		return client.level != null && Era.fromDimension(client.level.dimension()) != null;
	}
}
