package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;
import dev.retroeras.player.EraEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A player walking into the exit portal of an era's End sees the credits, as vanilla's own End does the first time. */
@Mixin(EndPortalBlock.class)
abstract class EndPortalBlockMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void retroeras$credits(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player && entity.canUsePortal(false)) {
			Era era = Era.fromDimension(level.dimension());
			if (era != null && era.isEnd(level.dimension())) {
				EraEnd.leave(player, era);
				ci.cancel();
			}
		}
	}
}
