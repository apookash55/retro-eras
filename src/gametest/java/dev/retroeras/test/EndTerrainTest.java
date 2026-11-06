package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.worldgen.end.EndChunkGenerator;
import dev.retroeras.worldgen.end.EndTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

/** 1.6.4's End: a single island about 100 blocks around the origin, in the middle heights, and void everywhere else. */
public class EndTerrainTest {
	private static final long SEED = 505L;

	@GameTest(maxTicks = 2400)
	public void oldEnd(GameTestHelper helper) {
		helper.assertTrue(Era.OLD_RELEASE.getEnd() != null, "1.6.4 should have an End");
		helper.assertTrue(Era.BETA.getEnd() == null, "Beta had no End");
		Identifier file = Era.OLD_RELEASE.getEnd().identifier().withPath(path -> "dimension/" + path + ".json");
		LevelStem stem = TestDimensions.load(helper, file);
		helper.assertTrue(stem.type().value().height() == 256, "1.6.4's End should be 256 high");
		helper.assertTrue(!stem.type().value().hasEnderDragonFight(), "1.6.4's End should have no modern dragon fight");
		helper.assertTrue(stem.generator() instanceof EndChunkGenerator, "1.6.4's End should use the old generator");
		EndTerrain terrain = ((EndChunkGenerator) stem.generator()).terrain(SEED);

		EndTerrain.Chunk centre = terrain.generate(0, 0);
		int top = -1;
		int bottom = -1;
		for (int y = 0; y < EndTerrain.HEIGHT; y++) {
			if (centre.get(0, y, 0)) {
				top = y;
				bottom = bottom < 0 ? y : bottom;
			}
		}
		helper.assertTrue(top >= 40 && top <= 80, "The island's top at the origin should be in the middle heights, found " + top);
		helper.assertTrue(bottom >= 8, "The island should float, its underside at the origin is at " + bottom);

		// The pull of the island is gone by 12.5 cells of 8 blocks; the noise can carry it only a little further.
		for (int[] far : new int[][] {{16, 0}, {-16, 0}, {0, 16}, {0, -16}, {40, 40}}) {
			EndTerrain.Chunk chunk = terrain.generate(far[0], far[1]);
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < EndTerrain.HEIGHT; y++) {
						helper.assertTrue(!chunk.get(x, y, z), "Chunk " + far[0] + ", " + far[1] + " should be void: 1.6.4 had no outer islands");
					}
				}
			}
		}
		helper.succeed();
	}
}
