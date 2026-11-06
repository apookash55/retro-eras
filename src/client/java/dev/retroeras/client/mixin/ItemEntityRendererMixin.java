package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.retroeras.client.EraDrawing;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * Dropped items before 1.6.4 (RenderItem in Beta): anything that is not a block is a flat picture that always turns to face
 * you, with up to four of them scattered for a large stack. Blocks stay small spinning cubes, as they always were.
 */
@Mixin(ItemEntityRenderer.class)
abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
	@Shadow
	@Final
	private RandomSource random;

	protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
	private void retroeras$oldStackSize(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
		if (EraDrawing.flatItems()) {
			int count = entity.getItem().getCount();
			state.count = count > 20 ? 4 : count > 5 ? 3 : count > 1 ? 2 : 1;
		}
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"), cancellable = true)
	private void retroeras$flatItem(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (!EraDrawing.flatItems() || state.item.isEmpty()) {
			return;
		}
		AABB box = state.item.getModelBoundingBox();
		if (box.getZsize() > 0.0625) {
			return;
		}
		ci.cancel();
		poseStack.pushPose();
		float bob = Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
		poseStack.translate(0.0F, bob + 0.0625F - (float) box.minY, 0.0F);
		random.setSeed(187L);
		for (int i = 0; i < state.count; i++) {
			poseStack.pushPose();
			if (i > 0) {
				poseStack.translate((random.nextFloat() * 2.0F - 1.0F) * 0.15F, (random.nextFloat() * 2.0F - 1.0F) * 0.15F, (random.nextFloat() * 2.0F - 1.0F) * 0.15F);
			}
			poseStack.rotateDegrees(Axis.YP, 180.0F - camera.yRot);
			poseStack.scale(1.0F, 1.0F, 0.01F);
			state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			poseStack.popPose();
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
