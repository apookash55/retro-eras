package dev.retroeras.test;

import java.util.HashMap;
import java.util.Map;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.classic.ClassicChunkGenerator;
import dev.retroeras.worldgen.classic.ClassicTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Classic 0.30's level: 256x256 and 64 high, flowing lava on the bottom layer, a sea flooded in from the edges at
 * y = 31, grass and sand on top, ore veins, flowers and trees, and the same level for the same seed.
 */
public class ClassicTerrainTest {
	private static final long SEED = 404L;
	private static final byte GRASS = 2;
	private static final byte WATER = 9;
	private static final byte LAVA_FLOWING = 10;
	private static final byte SAND = 12;
	private static final byte COAL_ORE = 16;
	private static final byte LOG = 17;
	private static final byte LEAVES = 18;

	@GameTest(maxTicks = 2400)
	public void classicLevel(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.CLASSIC);
		helper.assertTrue(stem.type().value().height() == 64, "Classic should be 64 high");
		helper.assertTrue(stem.generator() instanceof ClassicChunkGenerator, "Classic should use the Classic generator, not " + stem.generator());
		ClassicTerrain terrain = ((ClassicChunkGenerator) stem.generator()).terrain(SEED);

		Map<Byte, Integer> top = new HashMap<>();
		Map<Byte, Integer> all = new HashMap<>();
		for (int x = 0; x < ClassicTerrain.SIZE; x++) {
			for (int z = 0; z < ClassicTerrain.SIZE; z++) {
				helper.assertTrue(terrain.get(x, 0, z) == LAVA_FLOWING, "Classic's bottom layer should be lava");
				for (int y = ClassicTerrain.HEIGHT - 1; y >= 0; y--) {
					byte block = terrain.get(x, y, z);
					all.merge(block, 1, Integer::sum);
				}
				for (int y = ClassicTerrain.HEIGHT - 1; y >= 0; y--) {
					byte block = terrain.get(x, y, z);
					if (block != 0 && block != LEAVES && block != LOG && block < 37) {
						top.merge(block, 1, Integer::sum);
						break;
					}
				}
			}
		}
		int columns = ClassicTerrain.SIZE * ClassicTerrain.SIZE;
		RetroEras.LOGGER.info("Classic level (seed {}): tops {}; {} coal, {} logs", SEED, top, all.getOrDefault(COAL_ORE, 0), all.getOrDefault(LOG, 0));
		helper.assertTrue(top.getOrDefault(GRASS, 0) > columns / 4, "Most of a Classic level should be grass");
		helper.assertTrue(top.getOrDefault(WATER, 0) > 0, "A Classic level should have a sea");
		helper.assertTrue(top.getOrDefault(SAND, 0) > 0, "A Classic level should have sand beaches");
		helper.assertTrue(all.getOrDefault(COAL_ORE, 0) > 100, "A Classic level should have coal veins");
		helper.assertTrue(all.getOrDefault(LOG, 0) > 20, "A Classic level should have trees");

		// The sea reaches the edge of the map at y = 31 wherever the land is low enough.
		int edgeWater = 0;
		for (int x = 0; x < ClassicTerrain.SIZE; x++) {
			if (terrain.get(x, ClassicTerrain.WATER_LEVEL - 1, 0) == WATER) {
				edgeWater++;
			}
		}
		helper.assertTrue(edgeWater > 0, "The sea should flood in from the edge of the map");

		ClassicTerrain again = new ClassicTerrain(SEED);
		for (int i = 0; i < 2000; i++) {
			int x = (i * 37) % ClassicTerrain.SIZE;
			int y = (i * 13) % ClassicTerrain.HEIGHT;
			int z = (i * 101) % ClassicTerrain.SIZE;
			helper.assertTrue(terrain.get(x, y, z) == again.get(x, y, z), "The same seed should make the same Classic level");
		}
		helper.succeed();
	}
}
