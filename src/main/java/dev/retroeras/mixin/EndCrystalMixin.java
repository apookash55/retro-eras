package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.retroeras.player.EraEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.6.4's crystal in an era's End: it keeps a fire burning where it stands, which modern ones only do during a dragon
 * fight, and any crystal destroyed explodes, so one blast can set off the next.
 */
@Mixin(EndCrystal.class)
abstract class EndCrystalMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void retroeras$keepFire(CallbackInfo ci) {
		EndCrystal crystal = (EndCrystal) (Object) this;
		if (crystal.level() instanceof ServerLevel level && EraEnd.isEraEnd(level)) {
			BlockPos pos = crystal.blockPosition();
			if (!level.getBlockState(pos).is(Blocks.FIRE)) {
				level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
			}
		}
	}

	@ModifyExpressionValue(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean retroeras$chainExplosions(boolean isExplosion, ServerLevel level) {
		return isExplosion && !EraEnd.isEraEnd(level);
	}
}
