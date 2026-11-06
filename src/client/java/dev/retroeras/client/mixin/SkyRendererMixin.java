package dev.retroeras.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.buffers.GpuBuffer;

import dev.retroeras.client.EraDrawing;

import net.minecraft.client.renderer.SkyRenderer;

/**
 * Beta's stars, from the same seed in the same places as today's but about twice the size: 0.25 to 0.5, where 1.6.4 and
 * later have 0.15 to 0.25. Its sun, moon, clouds and sunrise are already what 26.3 draws.
 */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	@Unique
	private @Nullable GpuBuffer retroeras$oldStars;
	@Unique
	private boolean retroeras$buildingOldStars;

	@Shadow
	@Final
	private GpuBuffer starBuffer;

	@Shadow
	private native GpuBuffer buildStars();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void retroeras$buildOldStars(CallbackInfo ci) {
		retroeras$buildingOldStars = true;
		retroeras$oldStars = buildStars();
		retroeras$buildingOldStars = false;
	}

	@ModifyConstant(method = "buildStars", constant = @Constant(floatValue = 0.15F))
	private float retroeras$oldStarSize(float size) {
		return retroeras$buildingOldStars ? 0.25F : size;
	}

	@ModifyConstant(method = "buildStars", constant = @Constant(floatValue = 0.1F))
	private float retroeras$oldStarSpread(float spread) {
		return retroeras$buildingOldStars ? 0.25F : spread;
	}

	@ModifyExpressionValue(method = "renderStars", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/SkyRenderer;starBuffer:Lcom/mojang/renderpearl/api/buffers/GpuBuffer;"))
	private GpuBuffer retroeras$useOldStars(GpuBuffer stars) {
		return retroeras$oldStars != null && EraDrawing.oldStars() ? retroeras$oldStars : stars;
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void retroeras$closeOldStars(CallbackInfo ci) {
		if (retroeras$oldStars != null) {
			retroeras$oldStars.close();
		}
	}
}
