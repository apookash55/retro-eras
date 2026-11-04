package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.cavegame.CavegameChunkGenerator;
import dev.retroeras.worldgen.cavegame.CavegameTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/** Checks the Cave game world: a finite 256x256 map of grass over dirt over rock, with only void outside. */
public class CavegameTerrainTest {
	private static final long SEED = 404L;

	@GameTest
	public void cavegameTerrainGenerates(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.CAVEGAME);
		helper.assertTrue(stem.type().value().minY() == 0 && stem.type().value().height() == 64, "Cave game is 64 blocks tall");
		helper.assertTrue(stem.type().value().hasFixedTime(), "Cave game has no day/night cycle");
		helper.assertTrue(stem.generator() instanceof CavegameChunkGenerator, "Cave game should use its own generator, not " + stem.generator());
		CavegameTerrain terrain = ((CavegameChunkGenerator) stem.generator()).terrain(SEED);

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (int x = 0; x < CavegameTerrain.SIZE; x++) {
			for (int z = 0; z < CavegameTerrain.SIZE; z++) {
				int grass = -1;
				for (int y = CavegameTerrain.HEIGHT - 1; y >= 0; y--) {
					if (terrain.get(x, y, z) != CavegameTerrain.AIR) {
						grass = y;
						break;
					}
				}
				helper.assertTrue(grass > 0, "Hole in the world at " + x + "," + z);
				helper.assertTrue(terrain.get(x, grass, z) == CavegameTerrain.GRASS || terrain.get(x, grass, z) == CavegameTerrain.ROCK,
						"Top of " + x + "," + z + " should be grass or bare rock");
				helper.assertTrue(terrain.get(x, 0, z) == CavegameTerrain.ROCK, "Rock should reach the bottom at " + x + "," + z);
				min = Math.min(min, grass);
				max = Math.max(max, grass);
			}
		}
		RetroEras.LOGGER.info("Cave game terrain (seed {}): surface between y={} and y={}", SEED, min, max);
		helper.assertTrue(max - min >= 4, "Cave game rd-161348 terrain should be hilly, got " + min + ".." + max);

		for (BlockPos outside : new BlockPos[] {new BlockPos(-1, 30, 10), new BlockPos(256, 30, 10), new BlockPos(10, 30, 300), new BlockPos(-500, 30, -500)}) {
			helper.assertTrue(terrain.get(outside.getX(), outside.getY(), outside.getZ()) == CavegameTerrain.AIR, "Expected void outside the world at " + outside);
		}

		CavegameTerrain again = new CavegameTerrain(SEED);
		for (int i = 0; i < 1000; i++) {
			int x = (i * 37) % CavegameTerrain.SIZE;
			int z = (i * 101) % CavegameTerrain.SIZE;
			int y = i % CavegameTerrain.HEIGHT;
			helper.assertTrue(again.get(x, y, z) == terrain.get(x, y, z), "Cave game generation is not deterministic");
		}
		helper.succeed();
	}
}
