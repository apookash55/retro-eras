package dev.retroeras.test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaBiome;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.beta.BetaTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/** Runs the Beta 1.7.3 generator over a large area and checks the result looks like Beta. */
public class BetaTerrainTest {
	private static final long SEED = 404L;

	@GameTest(maxTicks = 2400)
	public void betaTerrainGenerates(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.BETA);
		helper.assertTrue(stem.type().value().minY() == 0 && stem.type().value().height() == 128, "Beta dimension should span y=0..127");
		helper.assertTrue(stem.generator() instanceof BetaChunkGenerator, "Beta should use the Beta 1.7.3 generator, not " + stem.generator());
		BetaTerrain terrain = ((BetaChunkGenerator) stem.generator()).terrain(SEED);

		int columns = 0;
		int ocean = 0;
		int beach = 0;
		int minHeight = Integer.MAX_VALUE;
		int maxHeight = Integer.MIN_VALUE;
		long totalHeight = 0;
		long caveAir = 0;
		Map<BetaBiome, Integer> biomes = new EnumMap<>(BetaBiome.class);

		for (int chunkX = -64; chunkX <= 64; chunkX += 4) {
			for (int chunkZ = -64; chunkZ <= 64; chunkZ += 4) {
				BetaTerrain.Chunk chunk = terrain.generate(chunkX, chunkZ, true);
				for (int x = 0; x < 16; x += 4) {
					for (int z = 0; z < 16; z += 4) {
						int top = topSolid(chunk, x, z);
						helper.assertTrue(chunk.get(x, 0, z) == 7, "Missing bedrock floor at chunk " + chunkX + "," + chunkZ);
						columns++;
						totalHeight += top;
						minHeight = Math.min(minHeight, top);
						maxHeight = Math.max(maxHeight, top);
						if (top < BetaTerrain.SEA_LEVEL - 1) {
							ocean++;
						}
						byte surface = chunk.get(x, top, z);
						if ((surface == 12 || surface == 13) && top >= 60 && top <= 65) {
							beach++;
						}
						for (int y = 12; y < top - 4; y++) {
							if (chunk.get(x, y, z) == 0) {
								caveAir++;
							}
						}
						biomes.merge(chunk.climate().biomes()[x * 16 + z], 1, Integer::sum);
					}
				}
			}
		}

		int oceanPercent = ocean * 100 / columns;
		RetroEras.LOGGER.info("Beta 1.7.3 terrain (seed {}): {} columns, height min={} avg={} max={}, {}% ocean, {}% beach, {} cave air blocks",
				SEED, columns, minHeight, totalHeight / columns, maxHeight, oceanPercent, beach * 100 / columns, caveAir);
		RetroEras.LOGGER.info("Beta 1.7.3 biomes: {}", biomes);

		helper.assertTrue(oceanPercent >= 10 && oceanPercent <= 75, "Beta worlds have plenty of ocean, got " + oceanPercent + "%");
		helper.assertTrue(maxHeight >= 90, "Beta hills should rise above y=90, highest was " + maxHeight);
		helper.assertTrue(minHeight <= 50, "Beta oceans should go below y=50, lowest was " + minHeight);
		helper.assertTrue(beach > 0, "No sand or gravel beaches generated");
		helper.assertTrue(caveAir > 0, "No caves generated");
		helper.assertTrue(biomes.size() >= 6, "Expected most Beta biomes in a 2000-block area, got " + biomes.keySet());

		BetaTerrain.Chunk again = new BetaTerrain(SEED).generate(3, -7, true);
		helper.assertTrue(Arrays.equals(again.blocks(), terrain.generate(3, -7, true).blocks()), "Beta generation is not deterministic");
		helper.succeed();
	}

	private static int topSolid(BetaTerrain.Chunk chunk, int x, int z) {
		for (int y = BetaTerrain.HEIGHT - 1; y > 0; y--) {
			byte block = chunk.get(x, y, z);
			if (block != 0 && block != 9 && block != 79) {
				return y;
			}
		}
		return 0;
	}
}
