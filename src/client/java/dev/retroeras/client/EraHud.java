package dev.retroeras.client;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Hides HUD elements that did not exist yet in the era the player is in: the hunger and experience bars before Beta
 * 1.8, and the hearts, armor and air bars in eras without survival. Shows the version name where old versions did.
 */
public final class EraHud {
	/** Whether the version name is shown, saved as hud.version in config/retroeras.properties. */
	private static boolean showVersion = true;

	private EraHud() {
	}

	public static boolean showsVersion() {
		return showVersion;
	}

	public static void setShowVersion(boolean show) {
		showVersion = show;
		EraConfig.update(properties -> properties.setProperty("hud.version", Boolean.toString(show)));
	}

	public static void init() {
		showVersion = !"false".equalsIgnoreCase(EraConfig.read().getProperty("hud.version", "true").trim());
		hideWhen(VanillaHudElements.FOOD_BAR, era -> !era.hasHunger());
		hideWhen(VanillaHudElements.EXPERIENCE_LEVEL, era -> !era.hasHunger());
		hideWhen(VanillaHudElements.INFO_BAR, era -> !era.hasHunger());
		hideWhen(VanillaHudElements.HEALTH_BAR, era -> !era.isSurvival());
		hideWhen(VanillaHudElements.ARMOR_BAR, era -> !era.isSurvival());
		hideWhen(VanillaHudElements.AIR_BAR, era -> !era.isSurvival());
		HudElementRegistry.addLast(RetroEras.id("version"), (graphics, delta) -> {
			Minecraft client = Minecraft.getInstance();
			Era era = currentEra();
			String text = era == null || !showVersion ? null : versionText(era);
			if (text != null && !client.getDebugOverlay().showDebugScreen()) {
				graphics.text(client.font, text, 2, 2, 0xFFFFFFFF, true);
			}
		});
	}

	/**
	 * The version name old Minecraft kept in the top left corner. Classic, Alpha and Infdev always showed it; Beta 1.7.3 and 1.6.4
	 * only showed it on the F3 screen, and Cave game showed nothing.
	 */
	private static @Nullable String versionText(Era era) {
		return switch (era) {
			case ALPHA -> "Minecraft Alpha v1.2.6";
			case PRE_ALPHA -> "Minecraft Infdev";
			case CLASSIC -> "0.30";
			default -> null;
		};
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
