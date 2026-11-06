package dev.retroeras.player;

import java.util.Set;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SlabBlock;

/**
 * The recipes that were different in the eras, from CraftingManager in each jar. Some only made fewer: three slabs and one
 * sign before 1.6.4, one door until 1.8, and two ladders in Beta but one before it. Others had another shape, and live in
 * data/retroeras/recipe, working only in their eras: fences from six sticks until 1.8, books from three paper and golden
 * apples from eight blocks of gold before 1.0, and wool and glowstone from nine in Alpha.
 */
public final class OldRecipes {
	private static final Set<Era> BEFORE_BETA = Set.of(Era.ALPHA, Era.PRE_ALPHA);
	private static final Set<Era> BEFORE_1_0 = Set.of(Era.BETA, Era.ALPHA, Era.PRE_ALPHA);
	private static final Set<Era> BEFORE_1_8 = Set.of(Era.OLD_RELEASE, Era.BETA, Era.ALPHA, Era.PRE_ALPHA);

	private OldRecipes() {
	}

	/**
	 * What crafting gives in {@code era}, or empty if it does not work there. {@code recipe} is the recipe that matched.
	 */
	public static ItemStack result(Era era, Identifier recipe, ItemStack vanilla) {
		if (recipe.getNamespace().equals(RetroEras.MOD_ID)) {
			return oldRecipeEras(recipe.getPath()).contains(era) ? vanilla : ItemStack.EMPTY;
		}
		if (replacedByOldRecipe(era, vanilla.getItem())) {
			return ItemStack.EMPTY;
		}
		int count = oldCount(era, vanilla);
		return count > 0 && count != vanilla.getCount() ? vanilla.copyWithCount(count) : vanilla;
	}

	/** Outside the eras only today's recipes work. */
	public static boolean worksOutsideEras(Identifier recipe) {
		return !recipe.getNamespace().equals(RetroEras.MOD_ID);
	}

	private static Set<Era> oldRecipeEras(String name) {
		return switch (name) {
			case "old_fence" -> BEFORE_1_8;
			case "old_book", "old_golden_apple" -> BEFORE_1_0;
			case "old_wool", "old_glowstone" -> BEFORE_BETA;
			default -> Set.of();
		};
	}

	private static boolean replacedByOldRecipe(Era era, Item item) {
		if (item == Items.OAK_FENCE) {
			return BEFORE_1_8.contains(era);
		}
		if (item == Items.BOOK || item == Items.GOLDEN_APPLE) {
			return BEFORE_1_0.contains(era);
		}
		if (item == Items.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE) || item == Items.GLOWSTONE) {
			return BEFORE_BETA.contains(era);
		}
		return false;
	}

	private static int oldCount(Era era, ItemStack stack) {
		if (stack.is(ItemTags.WOODEN_DOORS) || stack.is(Items.IRON_DOOR)) {
			return BEFORE_1_8.contains(era) ? 1 : 0;
		}
		if (BEFORE_1_0.contains(era)) {
			if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof SlabBlock) {
				return 3;
			}
			if (stack.is(ItemTags.SIGNS)) {
				return 1;
			}
			if (stack.is(Items.LADDER)) {
				return era == Era.BETA ? 2 : 1;
			}
		}
		return 0;
	}
}
