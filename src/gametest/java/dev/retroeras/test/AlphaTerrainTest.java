package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.beta.BetaTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Alpha 1.2.6 generated terrain the same way Beta 1.7.3 did, except that Beta 1.2 later put a few blocks of sandstone
 * under sand. Checks the Alpha dimension uses that generator with no sandstone, and otherwise matches Beta.
 */
public class AlphaTerrainTest {
	private static final long SEED = 404L;
	private static final byte SAND = 12;
	private static final byte SANDSTONE = 24;

	@GameTest(maxTicks = 2400)
	public void alphaTerrainIsBetaWithoutSandstone(GameTestHelper helper) {
		LevelStem alpha = TestDimensions.load(helper, Era.ALPHA);
		LevelStem beta = TestDimensions.load(helper, Era.BETA);
		helper.assertTrue(alpha.type().value().height() == 128, "Alpha should span y=0..127");
		helper.assertTrue(alpha.generator() instanceof BetaChunkGenerator, "Alpha should use the Beta/Alpha generator, not " + alpha.generator());
		BetaTerrain alphaTerrain = ((BetaChunkGenerator) alpha.generator()).terrain(SEED);
		BetaTerrain betaTerrain = ((BetaChunkGenerator) beta.generator()).terrain(SEED);

		int sandstoneInBeta = 0;
		int compared = 0;
		int same = 0;
		for (int chunkX = -32; chunkX <= 32; chunkX += 4) {
			for (int chunkZ = -32; chunkZ <= 32; chunkZ += 4) {
				BetaTerrain.Chunk a = alphaTerrain.generate(chunkX, chunkZ, true);
				BetaTerrain.Chunk b = betaTerrain.generate(chunkX, chunkZ, true);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = 0; y < BetaTerrain.HEIGHT; y++) {
							byte inAlpha = a.get(x, y, z);
							byte inBeta = b.get(x, y, z);
							helper.assertTrue(inAlpha != SANDSTONE, "Alpha had no sandstone, found some at chunk " + chunkX + "," + chunkZ);
							if (inBeta == SANDSTONE) {
								sandstoneInBeta++;
							}
							if (inAlpha == inBeta) {
								same++;
							}
							compared++;
						}
					}
				}
			}
		}
		RetroEras.LOGGER.info("Alpha terrain (seed {}): {} of {} blocks match Beta's; Beta has {} sandstone blocks there", SEED, same, compared, sandstoneInBeta);
		// Beta draws one extra random number per sandstone layer, which shifts later beach and bedrock choices a little.
		helper.assertTrue(same > compared * 0.95, "Alpha terrain should match Beta's apart from sandstone, only " + same + " of " + compared + " match");
		helper.assertTrue(sandstoneInBeta > 0, "The sample should include Beta sandstone to compare against");
		helper.succeed();
	}
}
