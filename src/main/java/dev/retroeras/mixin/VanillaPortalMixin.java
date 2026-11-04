package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Nether and End portals inside an era lead nowhere until the eras have their own Nether and End. Without this, a
 * portal placed by command, or the End portal of a 1.6.4 stronghold, would carry you to the modern dimensions and out of
 * the era. Fire already cannot light a Nether portal in an era, since vanilla only allows that in the overworld and Nether.
 */
@Mixin({NetherPortalBlock.class, EndPortalBlock.class})
abstract class VanillaPortalMixin {
	@Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
	private void retroeras$noModernDimensions(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
		Era era = Era.fromDimension(level.dimension());
		if (era != null) {
			if (entity instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("retroeras.portal.closed", Component.translatable("retroeras.era." + era.getName())));
			}
			cir.setReturnValue(null);
		}
	}
}
