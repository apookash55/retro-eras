package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.retroeras.player.EraMobs;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

/**
 * Breeding came in Beta 1.8: in the older eras, holding an animal's food out to it does nothing. Wolves still take bones
 * and heal from meat, since Wolf handles those itself before falling back to this.
 */
@Mixin(Animal.class)
abstract class AnimalMixin {
	@WrapOperation(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Animal;isFood(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean retroeras$noBreeding(Animal animal, ItemStack stack, Operation<Boolean> original) {
		return EraMobs.canBreed(animal.level()) && original.call(animal, stack);
	}
}
