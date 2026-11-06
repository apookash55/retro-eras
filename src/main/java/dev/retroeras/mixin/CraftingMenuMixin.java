package dev.retroeras.mixin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;
import dev.retroeras.player.EraItems;
import dev.retroeras.player.OldRecipes;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Crafting (in the inventory or at a table) gives nothing for items newer than the era the player is in, and follows the
 * era's own recipes ({@link OldRecipes}).
 */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
	private static void retroeras$onlyEraItems(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
			ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
		ItemStack result = resultSlots.getItem(0);
		if (result.isEmpty()) {
			return;
		}
		ItemStack changed = result;
		if (!EraItems.allowed(player, result)) {
			changed = ItemStack.EMPTY;
		} else {
			Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container.asCraftInput(), level, recipeHint);
			if (recipe.isPresent()) {
				Identifier id = recipe.get().id().identifier();
				Era era = Era.fromDimension(level.dimension());
				if (era != null) {
					changed = OldRecipes.result(era, id, result);
				} else if (!OldRecipes.worksOutsideEras(id)) {
					changed = ItemStack.EMPTY;
				}
			}
		}
		if (changed != result) {
			resultSlots.setItem(0, changed);
			menu.setRemoteSlot(0, changed);
			((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, changed));
		}
	}
}
