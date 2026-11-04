package dev.retroeras.client;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Turns smooth lighting off while the player is in an era older than Beta 1.8, which defaulted to flat per-face
 * lighting, and puts the player's own setting back when they leave. Only the in-memory value changes; options.txt is untouched.
 */
public final class EraLighting {
	/** The player's smooth lighting setting from before they entered an era, or null while not overridden. */
	private static Boolean saved;

	private EraLighting() {
	}

	public static void init() {
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> apply(client, usesFlatLighting(Era.fromDimension(level.dimension()))));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(() -> apply(client, false)));
	}

	/** Eras before Beta 1.8 defaulted to flat lighting; 1.6.4 already had smooth lighting on by default. */
	private static boolean usesFlatLighting(@Nullable Era era) {
		return era != null && !era.hasHunger();
	}

	private static void apply(Minecraft client, boolean inEra) {
		OptionInstance<Boolean> smoothLighting = client.options.ambientOcclusion();
		if (inEra && saved == null) {
			saved = smoothLighting.get();
			smoothLighting.set(false);
		} else if (!inEra && saved != null) {
			smoothLighting.set(saved);
			saved = null;
		}
	}
}
