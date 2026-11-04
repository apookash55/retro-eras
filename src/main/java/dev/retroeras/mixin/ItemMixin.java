package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.player.EraCombat;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Sword blocking in Old Release: holding right-click with a sword raises it, as in 1.6.4. The damage it saves is in
 * LivingEntityMixin. 26.3 still draws the old sword-block pose for any non-shield item used with the block animation.
 */
@Mixin(Item.class)
abstract class ItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void retroeras$swordBlock(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (EraCombat.canSwordBlock(player, player.getItemInHand(hand))) {
			player.startUsingItem(hand);
			cir.setReturnValue(InteractionResult.CONSUME);
		}
	}

	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void retroeras$swordBlockDuration(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
		if (user instanceof Player player && EraCombat.canSwordBlock(player, stack)) {
			cir.setReturnValue(72000);
		}
	}

	/** Only matters while the sword is in use, which only happens in Old Release. */
	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void retroeras$swordBlockAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
		if (stack.is(ItemTags.SWORDS)) {
			cir.setReturnValue(ItemUseAnimation.BLOCK);
		}
	}
}
