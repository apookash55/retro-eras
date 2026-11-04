package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves in the eras are still: no falling leaves, no water dripping from them and no rustling, none of which old
 * versions had.
 */
@Mixin({LeavesBlock.class, FallingParticlesLeavesBlock.class})
abstract class LeavesBlockMixin {
	@Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
	private void retroeras$stillLeaves(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (Era.fromDimension(level.dimension()) != null) {
			ci.cancel();
		}
	}
}
