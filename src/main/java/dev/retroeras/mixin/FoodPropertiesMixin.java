package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.player.EraRules;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

/** In hunger-free eras food heals directly, by its nutrition value, the way it did before Beta 1.8. */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
	@Shadow
	public abstract int nutrition();

	@Inject(method = "onConsume", at = @At("HEAD"))
	private void retroeras$healInHungerlessEras(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
		if (!level.isClientSide() && user instanceof Player player && !EraRules.hasHunger(player)) {
			player.heal(nutrition());
		}
	}
}
