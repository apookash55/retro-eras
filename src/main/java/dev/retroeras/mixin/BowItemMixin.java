package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Before Beta 1.8 a bow fired the moment you clicked: no drawing it back, and it never wore out. Beta's arrow
 * (EntityArrow) left at speed 1.5 and always hit for 4, so the base damage here is set to give 4 at that speed.
 */
@Mixin(BowItem.class)
abstract class BowItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void retroeras$instantShot(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		Era era = Era.fromDimension(level.dimension());
		if (era == null || era.hasHunger()) {
			return;
		}
		ItemStack bow = player.getItemInHand(hand);
		ItemStack projectile = player.getProjectile(bow);
		if (projectile.isEmpty()) {
			cir.setReturnValue(InteractionResult.FAIL);
			return;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
				1.0F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide()) {
			Arrow arrow = new Arrow(level, player, projectile.copyWithCount(1), bow);
			arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			arrow.setBaseDamage(4.0 / 1.5 - 0.01);
			level.addFreshEntity(arrow);
			if (!player.hasInfiniteMaterials()) {
				projectile.shrink(1);
			}
			player.awardStat(Stats.ITEM_USED.get(bow.getItem()));
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
