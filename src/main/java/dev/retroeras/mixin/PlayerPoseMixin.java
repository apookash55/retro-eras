package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Swimming came in 1.13: in the eras a player never swims, even sprinting under water in 1.6.4. See PlayerCrawlMixin. */
@Mixin(Entity.class)
abstract class PlayerPoseMixin {
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void retroeras$noSwimming(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player && Era.fromDimension(self.level().dimension()) != null) {
			self.setSwimming(false);
			ci.cancel();
		}
	}
}
