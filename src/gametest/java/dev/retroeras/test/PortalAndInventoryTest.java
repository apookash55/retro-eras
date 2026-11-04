package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.block.ModBlocks;
import dev.retroeras.player.EraInventories;
import dev.retroeras.portal.EraPortalShape;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PortalAndInventoryTest {
	/** Builds a 4x5 frame (2x3 opening) along the X axis with its bottom-left corner at (1, 1, 3). */
	private static void buildFrame(GameTestHelper helper, Block frame) {
		for (int x = 1; x <= 4; x++) {
			for (int y = 1; y <= 5; y++) {
				boolean edge = x == 1 || x == 4 || y == 1 || y == 5;
				helper.setBlock(x, y, 3, edge ? frame : Blocks.AIR);
			}
		}
	}

	@GameTest
	public void betaFrameLightsAndBreaks(GameTestHelper helper) {
		Era era = Era.BETA;
		Block portal = ModBlocks.portal(era);
		buildFrame(helper, era.getFrame());

		var shape = EraPortalShape.findEmpty(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3)), era.getFrame(), portal, Direction.Axis.X);
		helper.assertTrue(shape.isPresent(), "A complete mossy cobblestone frame should be lightable");
		shape.get().createPortalBlocks(helper.getLevel());
		for (int x = 2; x <= 3; x++) {
			for (int y = 2; y <= 4; y++) {
				helper.assertBlockPresent(portal, x, y, 3);
			}
		}

		// Breaking any frame block collapses the whole portal, like a nether portal.
		helper.setBlock(1, 3, 3, Blocks.AIR);
		helper.succeedWhen(() -> {
			for (int x = 2; x <= 3; x++) {
				for (int y = 2; y <= 4; y++) {
					helper.assertBlockNotPresent(portal, x, y, 3);
				}
			}
		});
	}

	@GameTest
	public void wrongFrameDoesNotLight(GameTestHelper helper) {
		buildFrame(helper, Blocks.OBSIDIAN);
		var shape = EraPortalShape.findEmpty(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3)),
				Era.BETA.getFrame(), ModBlocks.portal(Era.BETA), Direction.Axis.X);
		helper.assertTrue(shape.isEmpty(), "An obsidian frame must not light a Beta portal");
		helper.succeed();
	}

	@GameTest
	public void inventoriesAreSeparatePerEra(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(Era.MODERN.equals(player.getAttachedOrCreate(EraInventories.ACTIVE_GROUP)), "Overworld players start in the modern group");

		player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
		player.getEnderChestInventory().setItem(0, new ItemStack(Items.COPPER_INGOT));
		player.setExperienceLevels(5);

		EraInventories.switchTo(player, Era.BETA.getName());
		helper.assertTrue(player.getInventory().isEmpty(), "Modern items leaked into Beta: " + player.getInventory().getItem(0));
		helper.assertTrue(player.getEnderChestInventory().isEmpty(), "Modern ender chest leaked into Beta");
		helper.assertTrue(player.experienceLevel == 0, "Modern XP leaked into Beta");

		player.getInventory().setItem(4, new ItemStack(Items.STICK));
		EraInventories.switchTo(player, Era.MODERN);
		helper.assertTrue(player.getInventory().getItem(0).is(Items.DIAMOND) && player.getInventory().getItem(0).getCount() == 3,
				"Modern inventory was not restored");
		helper.assertTrue(player.getInventory().getItem(4).isEmpty(), "Beta items leaked into modern");
		helper.assertTrue(player.getEnderChestInventory().getItem(0).is(Items.COPPER_INGOT), "Modern ender chest was not restored");
		helper.assertTrue(player.experienceLevel == 5, "Modern XP was not restored");

		EraInventories.switchTo(player, Era.BETA.getName());
		helper.assertTrue(player.getInventory().getItem(4).is(Items.STICK), "Beta inventory was not restored");
		helper.succeed();
	}
}
