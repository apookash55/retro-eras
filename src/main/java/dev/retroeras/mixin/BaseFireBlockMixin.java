package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

/** Fire lights an obsidian portal in an era that had a Nether, and in that Nether, as it does in the overworld. */
@Mixin(BaseFireBlock.class)
abstract class BaseFireBlockMixin {
	@Inject(method = "inPortalDimension", at = @At("HEAD"), cancellable = true)
	private static void retroeras$eraPortals(Level level, CallbackInfoReturnable<Boolean> cir) {
		Era era = Era.fromDimension(level.dimension());
		if (era != null && era.getNether() != null) {
			cir.setReturnValue(true);
		}
	}
}
