package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.client.EraLightmap;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

/** Gives the old era lightmap Beta's stepped daylight in place of 26.3's smooth sky light factor. */
@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void retroeras$steppedDaylight(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
		EraLightmap.apply(state, partialTicks);
	}
}
