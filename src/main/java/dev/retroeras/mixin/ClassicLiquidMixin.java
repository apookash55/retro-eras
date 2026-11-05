package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;

/**
 * Classic's water and lava (LiquidTile) spread into every open block beside and below them and never thinned out, so a
 * single spring could flood a cave or the whole map. In Classic each flowing block loses nothing as it spreads.
 */
@Mixin({WaterFluid.class, LavaFluid.class})
abstract class ClassicLiquidMixin {
	@Inject(method = "getDropOff", at = @At("HEAD"), cancellable = true)
	private void retroeras$endlessInClassic(LevelReader reader, CallbackInfoReturnable<Integer> cir) {
		Level level = reader instanceof Level direct ? direct : reader instanceof WorldGenLevel region ? region.getLevel() : null;
		if (level != null && Era.fromDimension(level.dimension()) == Era.CLASSIC) {
			cir.setReturnValue(0);
		}
	}
}
