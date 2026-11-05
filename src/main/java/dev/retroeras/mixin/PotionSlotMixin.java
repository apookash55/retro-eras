package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.player.EraAchievements;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** "Local Brewery": taking a brewed potion, anything but a water bottle, out of a brewing stand. */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
abstract class PotionSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void retroeras$brewed(Player player, ItemStack carried, CallbackInfo ci) {
		PotionContents contents = carried.get(DataComponents.POTION_CONTENTS);
		if (player instanceof ServerPlayer serverPlayer && contents != null && contents.potion().isPresent() && !contents.is(Potions.WATER)) {
			EraAchievements.award(serverPlayer, "potion");
		}
	}
}
