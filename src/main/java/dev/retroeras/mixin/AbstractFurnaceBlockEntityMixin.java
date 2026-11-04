package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import dev.retroeras.Era;
import dev.retroeras.player.EraItems;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Furnaces, smokers and blast furnaces in an era do not smelt anything into an item the era did not have. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin {
	@WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn(Lnet/minecraft/core/NonNullList;ILnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean retroeras$onlyEraResults(NonNullList<ItemStack> items, int maxStackSize, ItemStack result, Operation<Boolean> original,
			@Local(argsOnly = true) ServerLevel level) {
		Era era = Era.fromDimension(level.dimension());
		return (era == null || EraItems.allowed(era, result.getItem())) && original.call(items, maxStackSize, result);
	}
}
