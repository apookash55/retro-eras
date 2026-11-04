package dev.retroeras.test;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.beta.BetaTerrain;
import dev.retroeras.worldgen.infdev.InfdevChunkGenerator;
import dev.retroeras.worldgen.infdev.InfdevTerrain;
import dev.retroeras.worldgen.release.ReleaseChunkGenerator;
import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The Far Lands: about 12,550,821 blocks from the origin, the old noise overflowed its int casts and the terrain turned
 * into walls of stone reaching the top of the world. The noise ports keep those casts, so Beta, Alpha and Infdev should
 * still have them, and the land near the origin should not. The check is how much of y=100..127 is solid.
 */
public class FarLandsTest {
	private static final long SEED = 404L;
	/** Chunk x of 12,550,880, a little past where the Far Lands start. */
	private static final int FAR_CHUNK = 784_430;
	private static final int TOP = 100;

	@GameTest(maxTicks = 2400)
	public void farLandsInBetaAlphaAndInfdev(GameTestHelper helper) {
		BetaTerrain beta = ((BetaChunkGenerator) TestDimensions.load(helper, Era.BETA).generator()).terrain(SEED);
		BetaTerrain alpha = ((BetaChunkGenerator) TestDimensions.load(helper, Era.ALPHA).generator()).terrain(SEED);
		InfdevTerrain infdev = ((InfdevChunkGenerator) TestDimensions.load(helper, Era.PRE_ALPHA).generator()).terrain(SEED);
		ReleaseTerrain release = ((ReleaseChunkGenerator) TestDimensions.load(helper, Era.OLD_RELEASE).generator()).terrain(SEED);

		check(helper, "Beta", (cx, cz) -> top(beta.generate(cx, cz, false)::get), true);
		check(helper, "Alpha", (cx, cz) -> top(alpha.generate(cx, cz, false)::get), true);
		check(helper, "Infdev", (cx, cz) -> top(infdev.generate(cx, cz, false)::get), true);
		double releaseFar = check(helper, "1.6.4", (cx, cz) -> top(release.generate(cx, cz, false)::get), false);
		RetroEras.LOGGER.info("1.6.4 at the Far Lands: {}% of y={}..127 solid", Math.round(releaseFar * 100), TOP);
		helper.succeed();
	}

	private interface Blocks {
		byte get(int x, int y, int z);
	}

	private interface Sampler {
		double sample(int chunkX, int chunkZ);
	}

	/** Fraction of y=TOP..127 that is solid in a chunk. */
	private static double top(Blocks chunk) {
		int solid = 0;
		int total = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = TOP; y < 128; y++) {
					if (chunk.get(x, y, z) != 0) {
						solid++;
					}
					total++;
				}
			}
		}
		return (double) solid / total;
	}

	/** Averages over a few chunks near the origin and in the Far Lands; returns the Far Lands fraction. */
	private static double check(GameTestHelper helper, String era, Sampler sampler, boolean expectFarLands) {
		double near = 0;
		double far = 0;
		for (int i = 0; i < 4; i++) {
			near += sampler.sample(i * 3, i * 2) / 4;
			far += sampler.sample(FAR_CHUNK + i * 3, i * 2) / 4;
		}
		RetroEras.LOGGER.info("{}: {}% of y={}..127 solid near the origin, {}% in the Far Lands", era, Math.round(near * 100), TOP, Math.round(far * 100));
		if (expectFarLands) {
			helper.assertTrue(near < 0.1, era + " should be open sky above y=" + TOP + " near the origin, " + near + " solid");
			helper.assertTrue(far > 0.3, era + " should have Far Lands walls at x=12,550,880, only " + far + " solid above y=" + TOP);
		}
		return far;
	}
}
