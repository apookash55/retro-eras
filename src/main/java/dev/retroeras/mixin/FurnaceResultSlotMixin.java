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
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;

/** "Acquire Hardware" and "Delicious Fish", earned when the item is taken out of the furnace, as SlotFurnace did. */
@Mixin(FurnaceResultSlot.class)
abstract class FurnaceResultSlotMixin {
	@Shadow
	@Final
	private Player player;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void retroeras$smelted(ItemStack carried, CallbackInfo ci) {
		if (player instanceof ServerPlayer serverPlayer && !carried.isEmpty()) {
			EraAchievements.onSmelt(serverPlayer, carried);
		}
	}
}
