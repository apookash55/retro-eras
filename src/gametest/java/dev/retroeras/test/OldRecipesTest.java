package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.player.OldRecipes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The amounts and old shapes of the era recipes, from CraftingManager in each version's jar. */
public class OldRecipesTest {
	private static final Identifier MODERN = Identifier.withDefaultNamespace("anything");

	@GameTest
	public void oldRecipes(GameTestHelper helper) {
		count(helper, Era.BETA, Items.OAK_SLAB, 6, 3);
		count(helper, Era.OLD_RELEASE, Items.OAK_SLAB, 6, 6);
		count(helper, Era.BETA, Items.LADDER, 3, 2);
		count(helper, Era.ALPHA, Items.LADDER, 3, 1);
		count(helper, Era.OLD_RELEASE, Items.LADDER, 3, 3);
		count(helper, Era.OLD_RELEASE, Items.OAK_DOOR, 3, 1);
		count(helper, Era.BETA, Items.IRON_DOOR, 3, 1);
		count(helper, Era.BETA, Items.OAK_SIGN, 3, 1);
		count(helper, Era.OLD_RELEASE, Items.OAK_SIGN, 3, 3);
		count(helper, Era.BETA, Items.OAK_FENCE, 3, 0);
		count(helper, Era.BETA, Items.BOOK, 1, 0);
		count(helper, Era.OLD_RELEASE, Items.BOOK, 1, 1);
		count(helper, Era.ALPHA, Items.GLOWSTONE, 1, 0);
		count(helper, Era.BETA, Items.GLOWSTONE, 1, 1);
		old(helper, Era.OLD_RELEASE, "old_fence", true);
		old(helper, Era.BETA, "old_book", true);
		old(helper, Era.OLD_RELEASE, "old_book", false);
		old(helper, Era.ALPHA, "old_wool", true);
		old(helper, Era.BETA, "old_wool", false);
		helper.assertTrue(!OldRecipes.worksOutsideEras(dev.retroeras.RetroEras.id("old_fence")), "The old recipes should not work outside the eras");
		helper.succeed();
	}

	private static void count(GameTestHelper helper, Era era, net.minecraft.world.item.Item item, int modern, int expected) {
		ItemStack result = OldRecipes.result(era, MODERN, new ItemStack(item, modern));
		helper.assertTrue(result.getCount() == expected, era.getName() + " should give " + expected + " of " + item + ", gave " + result.getCount());
	}

	private static void old(GameTestHelper helper, Era era, String recipe, boolean works) {
		ItemStack result = OldRecipes.result(era, dev.retroeras.RetroEras.id(recipe), new ItemStack(Items.STICK));
		helper.assertTrue(result.isEmpty() != works, recipe + " in " + era.getName() + " should " + (works ? "" : "not ") + "work");
	}
}
