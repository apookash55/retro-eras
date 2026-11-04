package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Builds a little scene in Beta with the textures whose layout changed since Beta (chests, a bed, redstone dust, water,
 * lava, fire, leather armour and the inventory screen) and saves screenshots of it.
 */
public class BetaTexturesClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamemode creative @a");
			server.runCommand("time set 6000");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			for (String dimension : new String[] {"minecraft:overworld", Era.BETA.getDimension().identifier().toString()}) {
			String name = dimension.equals("minecraft:overworld") ? "modern" : "beta";
			server.runCommand("execute in " + dimension + " run tp @a 40 120 40");
			context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals(dimension) && client.gui.overlay() == null, 1200);
			context.waitTicks(60);

			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				BlockPos origin = new BlockPos(40, 100, 40);
				for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, -1, -6), origin.offset(10, 8, 12))) {
					level.setBlock(pos, pos.getY() == origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
				}
				BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
				level.setBlock(origin.offset(-3, 0, 4), chest, 2);
				level.setBlock(origin.offset(0, 0, 4), chest.setValue(ChestBlock.TYPE, ChestType.LEFT), 2);
				level.setBlock(origin.offset(1, 0, 4), chest.setValue(ChestBlock.TYPE, ChestType.RIGHT), 2);
				BlockState bed = Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
				level.setBlock(origin.offset(3, 0, 4), bed.setValue(BedBlock.PART, BedPart.FOOT), 2);
				level.setBlock(origin.offset(4, 0, 4), bed.setValue(BedBlock.PART, BedPart.HEAD), 2);
				for (int x = -3; x <= 3; x++) {
					level.setBlock(origin.offset(x, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
				}
				for (int x = -6; x <= -4; x++) {
					for (int z = 6; z <= 8; z++) {
						level.setBlock(origin.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 3);
					}
				}
				level.setBlock(origin.offset(-2, 1, 8), Blocks.WATER.defaultBlockState(), 3);
				for (int x = 4; x <= 6; x++) {
					for (int z = 6; z <= 8; z++) {
						level.setBlock(origin.offset(x, -1, z), Blocks.LAVA.defaultBlockState(), 3);
					}
				}
				level.setBlock(origin.offset(0, 0, 8), Blocks.NETHERRACK.defaultBlockState(), 3);
				level.setBlock(origin.offset(0, 1, 8), Blocks.FIRE.defaultBlockState(), 3);
				player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
				player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
				player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
				player.getInventory().setItem(0, new ItemStack(Items.LEATHER_HELMET));
				player.getInventory().setItem(1, new ItemStack(Items.CHEST));
				player.getInventory().setItem(2, new ItemStack(Items.REDSTONE));
				RetroEras.LOGGER.info("Beta scene: player {} in {}, chest {}, bed {}, water {}", player.blockPosition(), level.dimension(),
						level.getBlockState(origin.offset(-3, 0, 4)), level.getBlockState(origin.offset(3, 0, 4)), level.getBlockState(origin.offset(-5, -1, 7)));
			});
			server.runCommand("execute in " + dimension + " run tp @a 40 100 37 0 30");
			context.waitTicks(100);
			shot(context, "retroeras_" + name + "_props");

			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			shot(context, "retroeras_" + name + "_leather");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			}

			server.runCommand("gamemode survival @a");
			context.waitTicks(10);
			context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
			context.waitTicks(20);
			shot(context, "retroeras_beta_inventory");
			context.setScreen(() -> null);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		RetroEras.LOGGER.info("Saved screenshot {}", context.takeScreenshot(name).toAbsolutePath());
	}
}
