package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cave game's saplings (rd-161348's Bush tile) never grew. They only checked that they were in sunlight on dirt or grass,
 * and vanished if not.
 */
@Mixin(SaplingBlock.class)
abstract class SaplingBlockMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void retroeras$caveGameBush(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (Era.fromDimension(level.dimension()) != Era.CAVEGAME) {
			return;
		}
		ci.cancel();
		BlockState below = level.getBlockState(pos.below());
		if (!level.canSeeSky(pos) || !below.is(Blocks.DIRT) && !below.is(Blocks.GRASS_BLOCK)) {
			level.removeBlock(pos, false);
		}
	}

	@Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
	private void retroeras$neverGrow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo ci) {
		if (Era.fromDimension(level.dimension()) == Era.CAVEGAME) {
			ci.cancel();
		}
	}
}
