package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.player.EraItems;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Items newer than the era stay on the ground. */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void retroeras$onlyEraItems(Player player, CallbackInfo ci) {
		if (!player.level().isClientSide() && !EraItems.allowed(player, this.getItem())) {
			ci.cancel();
		}
	}

	/** The pick-up achievements: wood, leather, diamonds and blaze rods, as EntityItem.onCollideWithPlayer awarded them. */
	@Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onItemPickup(Lnet/minecraft/world/entity/item/ItemEntity;)V"))
	private void retroeras$pickupAchievements(Player player, CallbackInfo ci) {
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			dev.retroeras.player.EraAchievements.onPickup(serverPlayer, this.getItem());
		}
	}
}
