package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.worldgen.cavegame.CavegameChunkGenerator;
import dev.retroeras.worldgen.cavegame.CavegameTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/** Checks the Cave game world of rd-132211: a flat 256x256 slab, grass on top of rock up to y = 42, void outside. */
public class CavegameTerrainTest {
	private static final long SEED = 404L;

	@GameTest
	public void cavegameTerrainGenerates(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.CAVEGAME);
		helper.assertTrue(stem.type().value().minY() == 0 && stem.type().value().height() == 64, "Cave game is 64 blocks tall");
		helper.assertTrue(stem.type().value().hasFixedTime(), "Cave game has no day/night cycle");
		helper.assertTrue(stem.generator() instanceof CavegameChunkGenerator, "Cave game should use its own generator, not " + stem.generator());
		CavegameTerrain terrain = ((CavegameChunkGenerator) stem.generator()).terrain(SEED);

		helper.assertTrue(CavegameTerrain.SURFACE == 42, "rd-132211's level was solid up to two thirds of 64, y = 42");
		for (int x = 0; x < CavegameTerrain.SIZE; x++) {
			for (int z = 0; z < CavegameTerrain.SIZE; z++) {
				helper.assertTrue(terrain.get(x, CavegameTerrain.SURFACE, z) == CavegameTerrain.GRASS, "The top layer should be grass at " + x + "," + z);
				helper.assertTrue(terrain.get(x, CavegameTerrain.SURFACE + 1, z) == CavegameTerrain.AIR, "Nothing should stand on the slab at " + x + "," + z);
				for (int y = 0; y < CavegameTerrain.SURFACE; y++) {
					helper.assertTrue(terrain.get(x, y, z) == CavegameTerrain.ROCK, "Everything under the grass should be rock at " + x + "," + y + "," + z);
				}
			}
		}

		for (BlockPos outside : new BlockPos[] {new BlockPos(-1, 30, 10), new BlockPos(256, 30, 10), new BlockPos(10, 30, 300), new BlockPos(-500, 30, -500)}) {
			helper.assertTrue(terrain.get(outside.getX(), outside.getY(), outside.getZ()) == CavegameTerrain.AIR, "Expected void outside the world at " + outside);
		}
		helper.succeed();
	}
}
