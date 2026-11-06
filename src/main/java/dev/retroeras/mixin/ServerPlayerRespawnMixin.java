package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.player.EraEnd;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;

/** After the credits from an era's End, the player goes back into that era instead of to the overworld. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerRespawnMixin {
	@Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
	private void retroeras$backToEra(boolean consumeSpawnBlock, TeleportTransition.PostTeleportTransition post, CallbackInfoReturnable<TeleportTransition> cir) {
		TeleportTransition transition = EraEnd.afterCredits((ServerPlayer) (Object) this, post);
		if (transition != null) {
			cir.setReturnValue(transition);
		}
	}
}
