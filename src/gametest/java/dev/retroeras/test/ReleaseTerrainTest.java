package dev.retroeras.test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.release.ReleaseBiome;
import dev.retroeras.worldgen.release.ReleaseChunkGenerator;
import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/** Runs the Minecraft 1.6.4 generator over a large area and checks the result looks like 1.6.4. */
public class ReleaseTerrainTest {
	private static final long SEED = 404L;

	@GameTest(maxTicks = 2400)
	public void releaseTerrainGenerates(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.OLD_RELEASE);
		helper.assertTrue(stem.type().value().minY() == 0 && stem.type().value().height() == 256, "Old Release dimension should span y=0..255");
		helper.assertTrue(stem.generator() instanceof ReleaseChunkGenerator, "Old Release should use the 1.6.4 generator, not " + stem.generator());
		ReleaseTerrain terrain = ((ReleaseChunkGenerator) stem.generator()).terrain(SEED);

		int columns = 0;
		int ocean = 0;
		int minHeight = Integer.MAX_VALUE;
		int maxHeight = Integer.MIN_VALUE;
		long totalHeight = 0;
		long caveAir = 0;
		Map<ReleaseBiome, Integer> biomes = new EnumMap<>(ReleaseBiome.class);

		for (int chunkX = -96; chunkX <= 96; chunkX += 6) {
			for (int chunkZ = -96; chunkZ <= 96; chunkZ += 6) {
				ReleaseTerrain.Chunk chunk = terrain.generate(chunkX, chunkZ, true);
				for (int x = 0; x < 16; x += 4) {
					for (int z = 0; z < 16; z += 4) {
						int top = topSolid(chunk, x, z);
						helper.assertTrue(chunk.get(x, 0, z) == 7, "Missing bedrock floor at chunk " + chunkX + "," + chunkZ);
						columns++;
						totalHeight += top;
						minHeight = Math.min(minHeight, top);
						maxHeight = Math.max(maxHeight, top);
						if (top < ReleaseTerrain.SEA_LEVEL - 1) {
							ocean++;
						}
						for (int y = 12; y < top - 4; y++) {
							if (chunk.get(x, y, z) == 0) {
								caveAir++;
							}
						}
						biomes.merge(chunk.biome(x, z), 1, Integer::sum);
					}
				}
			}
		}

		int oceanPercent = ocean * 100 / columns;
		RetroEras.LOGGER.info("Minecraft 1.6.4 terrain (seed {}): {} columns, height min={} avg={} max={}, {}% below sea level, {} cave air blocks",
				SEED, columns, minHeight, totalHeight / columns, maxHeight, oceanPercent, caveAir);
		RetroEras.LOGGER.info("Minecraft 1.6.4 biomes: {}", biomes);

		helper.assertTrue(biomes.size() >= 12, "Expected most 1.6.4 biomes in a 3000-block area, got " + biomes.keySet());
		helper.assertTrue(biomes.containsKey(ReleaseBiome.RIVER) || biomes.containsKey(ReleaseBiome.FROZEN_RIVER), "No rivers generated");
		helper.assertTrue(biomes.containsKey(ReleaseBiome.OCEAN), "No oceans generated");
		helper.assertTrue(oceanPercent >= 10 && oceanPercent <= 80, "Expected oceans and rivers, got " + oceanPercent + "% below sea level");
		helper.assertTrue(maxHeight >= 95 && maxHeight < 128, "1.6.4 hills rise high but stop below 128, highest was " + maxHeight);
		helper.assertTrue(minHeight <= 50, "1.6.4 oceans should be deep, lowest was " + minHeight);
		helper.assertTrue(caveAir > 0, "No caves generated");

		ReleaseTerrain.Chunk again = new ReleaseTerrain(SEED).generate(3, -7, true);
		helper.assertTrue(Arrays.equals(again.blocks(), terrain.generate(3, -7, true).blocks()), "1.6.4 generation is not deterministic");
		helper.succeed();
	}

	private static int topSolid(ReleaseTerrain.Chunk chunk, int x, int z) {
		for (int y = ReleaseTerrain.HEIGHT - 1; y > 0; y--) {
			byte block = chunk.get(x, y, z);
			if (block != 0 && block != 9 && block != 79) {
				return y;
			}
		}
		return 0;
	}
}
