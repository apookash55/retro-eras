package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;
import dev.retroeras.player.EraEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * End portals in eras without an End, and Nether portals in Infdev and Cave game, which had no Nether, lead nowhere.
 * Without this, a portal placed by command would carry you to the modern dimensions and out of the era. Nether portals in
 * Alpha, Beta and 1.6.4 go to the era's own Nether (see NetherPortalBlockMixin), and a 1.6.4 stronghold's End portal goes
 * to 1.6.4's own End (see EraEnd).
 */
@Mixin({NetherPortalBlock.class, EndPortalBlock.class})
abstract class VanillaPortalMixin {
	@Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
	private void retroeras$noModernDimensions(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
		Era era = Era.fromDimension(level.dimension());
		if (era == null) {
			return;
		}
		if ((Object) this instanceof EndPortalBlock && era.getEnd() != null) {
			cir.setReturnValue(EraEnd.destination(level, entity, era));
		} else if (!((Object) this instanceof NetherPortalBlock && era.getNether() != null)) {
			if (entity instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("retroeras.portal.closed", Component.translatable("retroeras.era." + era.getName())));
			}
			cir.setReturnValue(null);
		}
	}
}
