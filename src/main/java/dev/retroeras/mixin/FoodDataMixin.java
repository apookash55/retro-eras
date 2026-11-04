package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 1.6.4's hunger in the Old Release era. It had no fast healing from saturation (added in 1.11): you healed one
 * half-heart every 4 seconds while the bar was at 18 or more, which cost 3 exhaustion rather than 1.11's 6.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
	@Shadow
	private int foodLevel;
	@Shadow
	private float saturationLevel;
	@Shadow
	private float exhaustionLevel;
	@Shadow
	private int tickTimer;

	@Shadow
	public abstract void addExhaustion(float exhaustion);

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldReleaseHunger(ServerPlayer player, CallbackInfo ci) {
		if (Era.fromDimension(player.level().dimension()) != Era.OLD_RELEASE) {
			return;
		}
		ci.cancel();
		ServerLevel level = player.level();
		Difficulty difficulty = level.getDifficulty();
		if (this.exhaustionLevel > 4.0F) {
			this.exhaustionLevel -= 4.0F;
			if (this.saturationLevel > 0.0F) {
				this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
			} else if (difficulty != Difficulty.PEACEFUL) {
				this.foodLevel = Math.max(this.foodLevel - 1, 0);
			}
		}
		if (level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION) && this.foodLevel >= 18 && player.isHurt()) {
			if (++this.tickTimer >= 80) {
				player.heal(1.0F);
				this.addExhaustion(3.0F);
				this.tickTimer = 0;
			}
		} else if (this.foodLevel <= 0) {
			if (++this.tickTimer >= 80) {
				if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
					player.hurtServer(level, player.damageSources().starve(), 1.0F);
				}
				this.tickTimer = 0;
			}
		} else {
			this.tickTimer = 0;
		}
	}
}
