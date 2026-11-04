package dev.retroeras.test;

import java.nio.file.Path;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/**
 * Opens a world, looks around the overworld and each era from a few spots, and saves screenshots so the look of each era
 * can be checked by eye. Screenshots land in build/run/clientGameTest/screenshots/.
 */
public class EraScreenshotClientTest implements FabricClientGameTest {
	/** Spot centres far enough apart to land in different biomes. */
	private static final int[][] SPOTS = {{40, 40}, {1500, -800}, {-2200, 1300}};
	/** Spots for finite eras, which only exist between 0 and their world size. */
	private static final int[][] FINITE_SPOTS = {{64, 64}, {128, 128}, {192, 192}};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			// Survival, so the HUD shows hearts, hunger and experience; resistance keeps the player alive between shots.
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("effect give @a minecraft:resistance infinite 255 true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("give @a minecraft:iron_pickaxe");
			world.getServer().runCommand("give @a minecraft:bread 8");

			String overworld = Level.OVERWORLD.identifier().toString();
			shoot(context, world, overworld, "overworld", SPOTS[0]);
			for (Era era : Era.values()) {
				String dimension = era.getDimension().identifier().toString();
				int[][] spots = era.getWorldSize() > 0 ? FINITE_SPOTS : SPOTS;
				for (int i = 0; i < spots.length; i++) {
					shoot(context, world, dimension, era.getName() + "_" + i, spots[i]);
				}
				thirdPerson(context, era.getName());
			}
			shoot(context, world, overworld, "overworld_after", SPOTS[0]);
		}
	}

	/** Stands on dry land near {@code spot} in {@code dimension}, waits for chunks and textures, and takes two shots. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String dimension, String name, int[] spot) {
		world.getServer().runCommand("execute in " + dimension + " run tp @a " + spot[0] + " 60 " + spot[1] + " 135 0");
		context.waitFor(client -> inDimension(client, dimension) && client.gui.overlay() == null, 1200);
		context.waitTicks(60);
		// spreadplayers only lands on dry ground, so every shot starts on land rather than in an ocean or a cave.
		world.getServer().runCommand("execute in " + dimension + " run spreadplayers " + spot[0] + " " + spot[1] + " 0 40 false @a");
		context.waitTicks(20);
		world.getServer().runCommand("execute in " + dimension + " as @a at @s run tp @s ~ ~ ~ 135 0");
		context.waitTicks(120);
		RetroEras.LOGGER.info("Shooting {} at {}", name, context.computeOnClient(client -> client.player.blockPosition()));

		world.getServer().runCommand("execute in " + dimension + " as @a at @s run tp @s ~ ~ ~ 135 10");
		context.waitTicks(40);
		log(context.takeScreenshot("retroeras_" + name + "_landscape"));

		world.getServer().runCommand("execute in " + dimension + " as @a at @s run tp @s ~ ~ ~ 135 60");
		context.waitTicks(30);
		log(context.takeScreenshot("retroeras_" + name + "_ground"));
	}

	private static void thirdPerson(ClientGameTestContext context, String name) {
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(20);
		log(context.takeScreenshot("retroeras_" + name + "_player"));
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
	}

	private static boolean inDimension(Minecraft client, String dimension) {
		return client.level != null && client.level.dimension().identifier().toString().equals(dimension);
	}

	private static void log(Path screenshot) {
		RetroEras.LOGGER.info("Saved screenshot {}", screenshot.toAbsolutePath());
	}
}
