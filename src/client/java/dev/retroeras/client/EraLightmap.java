package dev.retroeras.client;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * The daylight of the eras before Beta 1.8. Their sky light dropped in whole steps, from 0 at noon to 11 at night (less
 * in rain and thunder), rather than fading smoothly. The era packs carry a lightmap shader that turns this stepped
 * value back into Beta's grey brightness table; this works out the step and hands it to the shader as the sky factor.
 */
public final class EraLightmap {
	private EraLightmap() {
	}

	/** Infdev, Alpha and Beta lit the world in grey with the brighter of sky and block light. 1.6.4 already had the coloured lightmap. */
	public static boolean usesOldLightmap(@Nullable Era era) {
		return era == Era.BETA || era == Era.ALPHA || era == Era.PRE_ALPHA;
	}

	/** Replaces the smooth sky factor with Beta's stepped one, while in an era that uses the old lightmap. */
	public static void apply(LightmapRenderState state, float partialTicks) {
		ClientLevel level = Minecraft.getInstance().level;
		if (!state.needsUpdate || level == null || !usesOldLightmap(Era.fromDimension(level.dimension()))) {
			return;
		}
		float sunAngle = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, Minecraft.getInstance().gameRenderer.mainCamera().blockPosition());
		state.skyFactor = 1.0F - skyDarkening(sunAngle / 360.0F, level.getRainLevel(partialTicks), level.getThunderLevel(partialTicks)) / 11.0F;
	}

	/**
	 * Beta's World.calculateSkylightSubtracted: how many levels the sky light loses, 0 to 11. {@code celestialAngle} is
	 * the part of the day since noon, 0 to 1.
	 */
	static int skyDarkening(float celestialAngle, float rain, float thunder) {
		float daylight = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
		daylight *= 1.0F - rain * 5.0F / 16.0F;
		daylight *= 1.0F - thunder * 5.0F / 16.0F;
		return (int) ((1.0F - daylight) * 11.0F);
	}
}
