package dev.retroeras.worldgen.beta;

import java.util.Arrays;
import java.util.Random;

/** Beta 1.7.3's NoiseGeneratorOctaves: Perlin octaves that halve in frequency and double in amplitude. */
final class BetaOctaveNoise {
	private final BetaPerlinNoise[] octaves;

	BetaOctaveNoise(Random random, int count) {
		octaves = new BetaPerlinNoise[count];
		for (int i = 0; i < count; i++) {
			octaves[i] = new BetaPerlinNoise(random);
		}
	}

	double[] sample(double[] out, double x, double y, double z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ) {
		if (out == null || out.length < sizeX * sizeY * sizeZ) {
			out = new double[sizeX * sizeY * sizeZ];
		} else {
			Arrays.fill(out, 0.0);
		}
		double frequency = 1.0;
		for (BetaPerlinNoise octave : octaves) {
			octave.add(out, x, y, z, sizeX, sizeY, sizeZ, scaleX * frequency, scaleY * frequency, scaleZ * frequency, frequency);
			frequency /= 2.0;
		}
		return out;
	}

	/** Beta's 2D form (func_4109_a), which samples at y = 10 with a single y step. */
	double[] sample2D(double[] out, int x, int z, int sizeX, int sizeZ, double scaleX, double scaleZ) {
		return sample(out, x, 10.0, z, sizeX, 1, sizeZ, scaleX, 1.0, scaleZ);
	}
}
