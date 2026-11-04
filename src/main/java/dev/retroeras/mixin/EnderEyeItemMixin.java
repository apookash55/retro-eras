package dev.retroeras.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.retroeras.worldgen.release.ReleaseChunkGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.levelgen.structure.Structure;

/** In Old Release, eyes of ender lead to its 1.6.4 strongholds rather than modern ones, which it does not have. */
@Mixin(EnderEyeItem.class)
abstract class EnderEyeItemMixin {
	@WrapOperation(method = "use", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"))
	private @Nullable BlockPos retroeras$oldStrongholds(ServerLevel level, TagKey<Structure> tag, BlockPos from, int radius, boolean skipKnown,
			Operation<BlockPos> original) {
		if (level.getChunkSource().getGenerator() instanceof ReleaseChunkGenerator generator) {
			return generator.structures(level.getSeed()).stronghold().nearest(from);
		}
		return original.call(level, tag, from, radius, skipKnown);
	}
}
