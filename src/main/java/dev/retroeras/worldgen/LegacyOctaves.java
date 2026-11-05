package dev.retroeras.worldgen;

import java.util.Random;

/**
 * An old NoiseGeneratorOctaves, so generators shared between versions (the Nether) can use Beta's or 1.6.4's. The two
 * differ in that 1.6.4 wraps its coordinates to keep terrain intact far from the origin.
 */
@FunctionalInterface
public interface LegacyOctaves {
	double[] sample(double[] out, double x, double y, double z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ);

	/** Creates {@code count} octaves from {@code random}, in the order the old constructors drew from it. */
	@FunctionalInterface
	interface Factory {
		LegacyOctaves create(Random random, int count);
	}
}
