package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.player.EraAchievements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps each era's achievements to that era: 26.3's advancements (recipe unlocks included) are not earned in the eras,
 * an era's achievements only there, and nothing at all in the eras that had none.
 */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At("HEAD"), cancellable = true)
	private void retroeras$ownEraOnly(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (!EraAchievements.mayEarn(player, holder.id())) {
			cir.setReturnValue(false);
		}
	}
}
