package dev.retroeras.worldgen.beta;

import java.util.Arrays;
import java.util.Random;

/** Beta 1.7.3's 2D simplex noise (NoiseGenerator2) and its octave wrapper (NoiseGeneratorOctaves2), used for climate. */
final class BetaSimplexNoise {
	private static final int[][] GRADIENTS = {
			{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1},
			{1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
	};
	private static final double SKEW = 0.5 * (Math.sqrt(3.0) - 1.0);
	private static final double UNSKEW = (3.0 - Math.sqrt(3.0)) / 6.0;

	private final int[] permutations = new int[512];
	private final double xOffset;
	private final double yOffset;

	BetaSimplexNoise(Random random) {
		xOffset = random.nextDouble() * 256.0;
		yOffset = random.nextDouble() * 256.0;
		random.nextDouble(); // Beta draws a third offset it never uses; keep the random sequence identical.
		for (int i = 0; i < 256; i++) {
			permutations[i] = i;
		}
		for (int i = 0; i < 256; i++) {
			int j = random.nextInt(256 - i) + i;
			int swap = permutations[i];
			permutations[i] = permutations[j];
			permutations[j] = swap;
			permutations[i + 256] = permutations[i];
		}
	}

	private static int wrap(double value) {
		return value > 0.0 ? (int) value : (int) value - 1;
	}

	private static double dot(int[] gradient, double x, double y) {
		return gradient[0] * x + gradient[1] * y;
	}

	void add(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double amplitude) {
		int index = 0;
		for (int ix = 0; ix < sizeX; ix++) {
			double px = (x + ix) * scaleX + xOffset;
			for (int iz = 0; iz < sizeZ; iz++) {
				double pz = (z + iz) * scaleZ + yOffset;
				double skew = (px + pz) * SKEW;
				int i = wrap(px + skew);
				int j = wrap(pz + skew);
				double unskew = (i + j) * UNSKEW;
				double x0 = px - (i - unskew);
				double y0 = pz - (j - unskew);
				int i1;
				int j1;
				if (x0 > y0) {
					i1 = 1;
					j1 = 0;
				} else {
					i1 = 0;
					j1 = 1;
				}
				double x1 = x0 - i1 + UNSKEW;
				double y1 = y0 - j1 + UNSKEW;
				double x2 = x0 - 1.0 + 2.0 * UNSKEW;
				double y2 = y0 - 1.0 + 2.0 * UNSKEW;
				int ii = i & 255;
				int jj = j & 255;
				int g0 = permutations[ii + permutations[jj]] % 12;
				int g1 = permutations[ii + i1 + permutations[jj + j1]] % 12;
				int g2 = permutations[ii + 1 + permutations[jj + 1]] % 12;
				out[index++] += 70.0 * (corner(g0, x0, y0) + corner(g1, x1, y1) + corner(g2, x2, y2)) * amplitude;
			}
		}
	}

	private static double corner(int gradient, double x, double y) {
		double t = 0.5 - x * x - y * y;
		if (t < 0.0) {
			return 0.0;
		}
		t *= t;
		return t * t * dot(GRADIENTS[gradient], x, y);
	}

	/** NoiseGeneratorOctaves2. */
	static final class Octaves {
		private final BetaSimplexNoise[] octaves;

		Octaves(Random random, int count) {
			octaves = new BetaSimplexNoise[count];
			for (int i = 0; i < count; i++) {
				octaves[i] = new BetaSimplexNoise(random);
			}
		}

		double[] sample(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double lacunarity) {
			return sample(out, x, z, sizeX, sizeZ, scaleX, scaleZ, lacunarity, 0.5);
		}

		double[] sample(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double lacunarity, double persistence) {
			scaleX /= 1.5;
			scaleZ /= 1.5;
			if (out == null || out.length < sizeX * sizeZ) {
				out = new double[sizeX * sizeZ];
			} else {
				Arrays.fill(out, 0.0);
			}
			double amplitude = 1.0;
			double frequency = 1.0;
			for (BetaSimplexNoise octave : octaves) {
				octave.add(out, x, z, sizeX, sizeZ, scaleX * frequency, scaleZ * frequency, 0.55 / amplitude);
				frequency *= lacunarity;
				amplitude *= persistence;
			}
			return out;
		}
	}
}
