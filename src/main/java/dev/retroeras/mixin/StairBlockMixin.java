package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;

/** Stairs only joined into corners from 1.4: before that they stayed straight, whatever stood next to them. */
@Mixin(StairBlock.class)
abstract class StairBlockMixin {
	@Inject(method = "getStairsShape", at = @At("HEAD"), cancellable = true)
	private static void retroeras$noCorners(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<StairsShape> cir) {
		if (level instanceof Level world) {
			Era era = Era.fromDimension(world.dimension());
			if (era != null && era != Era.OLD_RELEASE) {
				cir.setReturnValue(StairsShape.STRAIGHT);
			}
		}
	}
}
