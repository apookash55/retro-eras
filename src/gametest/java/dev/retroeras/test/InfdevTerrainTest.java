package dev.retroeras.test;

import java.util.Arrays;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.infdev.InfdevChunkGenerator;
import dev.retroeras.worldgen.infdev.InfdevTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/** Runs the Infdev 20100618 generator over a large area and checks the result looks like Infdev. */
public class InfdevTerrainTest {
	private static final long SEED = 404L;

	@GameTest(maxTicks = 2400)
	public void infdevTerrainGenerates(GameTestHelper helper) {
		LevelStem stem = TestDimensions.load(helper, Era.PRE_ALPHA);
		helper.assertTrue(stem.type().value().minY() == 0 && stem.type().value().height() == 128, "Pre-Alpha dimension should span y=0..127");
		helper.assertTrue(stem.generator() instanceof InfdevChunkGenerator, "Pre-Alpha should use the Infdev generator, not " + stem.generator());
		InfdevTerrain terrain = ((InfdevChunkGenerator) stem.generator()).terrain(SEED);

		int columns = 0;
		int ocean = 0;
		int beach = 0;
		int minHeight = Integer.MAX_VALUE;
		int maxHeight = Integer.MIN_VALUE;
		long totalHeight = 0;
		long caveAir = 0;
		int bedrock = 0;

		for (int chunkX = -64; chunkX <= 64; chunkX += 4) {
			for (int chunkZ = -64; chunkZ <= 64; chunkZ += 4) {
				InfdevTerrain.Chunk chunk = terrain.generate(chunkX, chunkZ, true);
				for (int x = 0; x < 16; x += 4) {
					for (int z = 0; z < 16; z += 4) {
						int top = topSolid(chunk, x, z);
						if (chunk.get(x, 0, z) == 7) {
							bedrock++;
						}
						columns++;
						totalHeight += top;
						minHeight = Math.min(minHeight, top);
						maxHeight = Math.max(maxHeight, top);
						if (top < InfdevTerrain.SEA_LEVEL - 1) {
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
					}
				}
			}
		}

		int oceanPercent = ocean * 100 / columns;
		RetroEras.LOGGER.info("Infdev 20100618 terrain (seed {}): {} columns, height min={} avg={} max={}, {}% ocean, {}% beach, {} cave air blocks",
				SEED, columns, minHeight, totalHeight / columns, maxHeight, oceanPercent, beach * 100 / columns, caveAir);

		helper.assertTrue(oceanPercent >= 5 && oceanPercent <= 80, "Infdev worlds have oceans, got " + oceanPercent + "%");
		helper.assertTrue(maxHeight >= 85, "Infdev hills should rise high, highest was " + maxHeight);
		helper.assertTrue(minHeight <= 55, "Infdev oceans should go below y=55, lowest was " + minHeight);
		// Infdev's bedrock rule (y <= nextInt(6) - 1) leaves about 1 in 6 holes in the bottom layer, unlike Beta.
		int bedrockPercent = bedrock * 100 / columns;
		helper.assertTrue(bedrockPercent >= 75 && bedrockPercent <= 90, "Expected ~83% bedrock at y=0, got " + bedrockPercent + "%");
		helper.assertTrue(beach > 0, "No sand or gravel beaches generated");
		helper.assertTrue(caveAir > 0, "No caves generated");

		InfdevTerrain.Chunk again = new InfdevTerrain(SEED).generate(3, -7, true);
		helper.assertTrue(Arrays.equals(again.blocks(), terrain.generate(3, -7, true).blocks()), "Infdev generation is not deterministic");
		helper.succeed();
	}

	private static int topSolid(InfdevTerrain.Chunk chunk, int x, int z) {
		for (int y = InfdevTerrain.HEIGHT - 1; y > 0; y--) {
			byte block = chunk.get(x, y, z);
			if (block != 0 && block != 9) {
				return y;
			}
		}
		return 0;
	}
}
