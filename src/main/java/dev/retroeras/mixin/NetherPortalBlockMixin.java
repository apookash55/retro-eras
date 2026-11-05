package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;

/**
 * An obsidian portal in an era that had a Nether (Alpha 1.2 to 1.6.4) leads to that era's own Nether, and back from it
 * to the era. Everything else, finding or building the exit portal and the 8:1 scale, is vanilla's.
 */
@Mixin(NetherPortalBlock.class)
abstract class NetherPortalBlockMixin {
	@ModifyVariable(method = "getPortalDestination", at = @At("STORE"), ordinal = 0)
	private ResourceKey<Level> retroeras$eraNether(ResourceKey<Level> vanilla, ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
		Era era = Era.fromDimension(currentLevel.dimension());
		if (era == null || era.getNether() == null) {
			return vanilla;
		}
		return era.isNether(currentLevel.dimension()) ? era.getDimension() : era.getNether();
	}

	@ModifyVariable(method = "getPortalDestination", at = @At("STORE"), ordinal = 0)
	private boolean retroeras$toEraNether(boolean vanilla, @Local(ordinal = 1) ServerLevel newLevel) {
		Era era = Era.fromDimension(newLevel.dimension());
		return era != null && era.getNether() != null ? era.isNether(newLevel.dimension()) : vanilla;
	}
}
