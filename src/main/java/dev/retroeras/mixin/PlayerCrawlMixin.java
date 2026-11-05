package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * Crawling came in 1.13 too: in the eras a player never drops to a crawl to fit through a gap. Where the wanted pose
 * does not fit, crouching is as low as it goes.
 */
@Mixin(Player.class)
abstract class PlayerCrawlMixin {
	@Shadow
	private Pose getDesiredPose() {
		throw new AssertionError();
	}

	@Shadow
	protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void retroeras$noCrawling(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (Era.fromDimension(self.level().dimension()) == null) {
			return;
		}
		ci.cancel();
		Pose desired = getDesiredPose();
		if (desired == Pose.SWIMMING) {
			desired = Pose.STANDING;
		}
		if (!self.isSpectator() && !self.isPassenger() && !canPlayerFitWithinBlocksAndEntitiesWhen(desired)
				&& canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
			desired = Pose.CROUCHING;
		}
		self.setPose(desired);
	}
}
