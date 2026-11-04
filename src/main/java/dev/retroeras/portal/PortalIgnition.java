package dev.retroeras.portal;

import java.util.Optional;

import dev.retroeras.Era;
import dev.retroeras.block.ModBlocks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Lights an era portal when flint and steel is used on the inside of a complete frame of that era's frame block,
 * the same way a nether portal is lit inside obsidian. Works in the overworld and in the era's own dimension.
 */
public final class PortalIgnition {
	private PortalIgnition() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(Items.FLINT_AND_STEEL) || player.isSpectator()) {
				return InteractionResult.PASS;
			}

			BlockState clicked = level.getBlockState(hit.getBlockPos());
			BlockPos inside = hit.getBlockPos().relative(hit.getDirection());
			for (Era era : Era.values()) {
				if (!clicked.is(era.getFrame()) || !canLightIn(level, era)) {
					continue;
				}

				Optional<EraPortalShape> shape = EraPortalShape.findEmpty(level, inside, era.getFrame(), ModBlocks.portal(era),
						player.getDirection().getClockWise().getAxis());
				if (shape.isEmpty()) {
					continue;
				}

				if (!level.isClientSide()) {
					shape.get().createPortalBlocks(level);
					level.playSound(null, inside, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
					level.gameEvent(player, GameEvent.BLOCK_PLACE, inside);
					stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	private static boolean canLightIn(Level level, Era era) {
		return level.dimension() == Level.OVERWORLD || level.dimension() == era.getDimension();
	}
}
