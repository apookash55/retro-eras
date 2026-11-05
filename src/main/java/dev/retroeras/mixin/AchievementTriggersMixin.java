package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.player.EraAchievements;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

/** The crafting achievements, earned when the item is taken out of the crafting grid, as SlotCrafting.onCrafting did. */
@Mixin(ResultSlot.class)
abstract class AchievementTriggersMixin {
	@Shadow
	@Final
	private Player player;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void retroeras$crafted(ItemStack carried, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer) {
			EraAchievements.onCraft(serverPlayer, carried);
		}
	}
}
