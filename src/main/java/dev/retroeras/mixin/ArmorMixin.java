package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import dev.retroeras.player.EraArmor;

import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * A player's armour follows the era's rules ({@link EraArmor}) instead of 26.3's toughness formula. The old games added
 * up the armour before wearing it down for the hit, so the total is taken first.
 */
@Mixin(LivingEntity.class)
abstract class ArmorMixin {
	@Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"))
	private void retroeras$armourBeforeWear(DamageSource source, float damage, CallbackInfoReturnable<Float> cir,
			@Share("armour") LocalIntRef armour) {
		armour.set((Object) this instanceof Player player ? EraArmor.points(player) : -1);
	}

	@WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
	private float retroeras$eraArmour(LivingEntity entity, float damage, DamageSource source, float armourValue, float toughness, Operation<Float> original,
			@Share("armour") LocalIntRef armour) {
		if (armour.get() >= 0) {
			return damage * (25 - Math.min(armour.get(), 20)) / 25.0F;
		}
		return original.call(entity, damage, source, armourValue, toughness);
	}
}
