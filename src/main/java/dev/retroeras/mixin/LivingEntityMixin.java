package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.retroeras.player.EraCombat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 1.6.4's sword block: damage that armour could stop is cut to (1 + damage) / 2. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float retroeras$swordBlock(float damage, ServerLevel level, DamageSource source) {
		if ((Object) this instanceof Player player && damage > 0.0F && !source.is(DamageTypeTags.BYPASSES_ARMOR)
				&& player.isUsingItem() && EraCombat.canSwordBlock(player, player.getUseItem())) {
			return (1.0F + damage) * 0.5F;
		}
		return damage;
	}
}
