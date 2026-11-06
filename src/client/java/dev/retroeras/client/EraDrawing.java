package dev.retroeras.client;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Which era the world is being drawn for, for the renderer mixins that change how something looks. */
public final class EraDrawing {
	private EraDrawing() {
	}

	private static @Nullable Era era() {
		ClientLevel level = Minecraft.getInstance().level;
		return level == null ? null : Era.fromDimension(level.dimension());
	}

	/** Dropped items were flat pictures until 1.6.4's fancy graphics gave them depth. */
	public static boolean flatItems() {
		Era era = era();
		return era != null && era != Era.OLD_RELEASE;
	}

	/** The stars were larger before 1.6.4. */
	public static boolean oldStars() {
		Era era = era();
		return era != null && era != Era.OLD_RELEASE;
	}
}
