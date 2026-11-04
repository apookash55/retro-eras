package dev.retroeras.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.player.EraItems;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Crafting (in the inventory or at a table) gives nothing for items newer than the era the player is in. */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
	private static void retroeras$onlyEraItems(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
			ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
		ItemStack result = resultSlots.getItem(0);
		if (result.isEmpty() || EraItems.allowed(player, result)) {
			return;
		}
		resultSlots.setItem(0, ItemStack.EMPTY);
		menu.setRemoteSlot(0, ItemStack.EMPTY);
		((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
	}
}
