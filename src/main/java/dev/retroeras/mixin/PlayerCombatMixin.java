package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.retroeras.player.EraCombat;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Pre-1.9 melee in the eras: era weapon damage, no sweeps, and no critical hits before Beta 1.8. */
@Mixin(Player.class)
abstract class PlayerCombatMixin {
	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double retroeras$eraDamage(Player player, Holder<Attribute> attribute, Operation<Double> original) {
		double value = original.call(player, attribute);
		return attribute.is(Attributes.ATTACK_DAMAGE) ? EraCombat.attackDamage(player, value) : value;
	}

	@Inject(method = "isSweepAttack", at = @At("HEAD"), cancellable = true)
	private void retroeras$noSweep(boolean fullStrength, boolean critical, boolean knockback, CallbackInfoReturnable<Boolean> cir) {
		if (EraCombat.usesOldCombat((Player) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canCriticalAttack", at = @At("HEAD"), cancellable = true)
	private void retroeras$noCrit(Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (!EraCombat.canCrit((Player) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
