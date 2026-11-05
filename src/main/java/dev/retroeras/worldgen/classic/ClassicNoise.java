package dev.retroeras.worldgen.classic;

import java.util.Random;

/** Classic 0.30's 2D noise: ImprovedNoise sampled at z = 0, PerlinNoise octaves, and DistortedNoise. */
abstract class ClassicNoise {
	abstract double sample(double x, double y);

	/** ImprovedNoise: Ken Perlin's improved noise, read on the z = 0 plane. */
	static final class Improved extends ClassicNoise {
		private final int[] permutations = new int[512];

		Improved(Random random) {
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

		private static double fade(double t) {
			return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
		}

		private static double lerp(double t, double a, double b) {
			return a + t * (b - a);
		}

		private static double grad(int hash, double x, double y, double z) {
			int h = hash & 15;
			double u = h < 8 ? x : y;
			double v = h < 4 ? y : h != 12 && h != 14 ? z : x;
			return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
		}

		@Override
		double sample(double x, double y) {
			double z = 0.0;
			int xi = (int) Math.floor(x) & 255;
			int yi = (int) Math.floor(y) & 255;
			int zi = (int) Math.floor(z) & 255;
			x -= Math.floor(x);
			y -= Math.floor(y);
			z -= Math.floor(z);
			double u = fade(x);
			double v = fade(y);
			double w = fade(z);
			int a = permutations[xi] + yi;
			int aa = permutations[a] + zi;
			int ab = permutations[a + 1] + zi;
			int b = permutations[xi + 1] + yi;
			int ba = permutations[b] + zi;
			int bb = permutations[b + 1] + zi;
			return lerp(w,
					lerp(v, lerp(u, grad(permutations[aa], x, y, z), grad(permutations[ba], x - 1.0, y, z)),
							lerp(u, grad(permutations[ab], x, y - 1.0, z), grad(permutations[bb], x - 1.0, y - 1.0, z))),
					lerp(v, lerp(u, grad(permutations[aa + 1], x, y, z - 1.0), grad(permutations[ba + 1], x - 1.0, y, z - 1.0)),
							lerp(u, grad(permutations[ab + 1], x, y - 1.0, z - 1.0), grad(permutations[bb + 1], x - 1.0, y - 1.0, z - 1.0))));
		}
	}

	/** PerlinNoise: octaves that halve in frequency and double in amplitude. */
	static final class Octaves extends ClassicNoise {
		private final Improved[] octaves;

		Octaves(Random random, int count) {
			octaves = new Improved[count];
			for (int i = 0; i < count; i++) {
				octaves[i] = new Improved(random);
			}
		}

		@Override
		double sample(double x, double y) {
			double value = 0.0;
			double scale = 1.0;
			for (Improved octave : octaves) {
				value += octave.sample(x / scale, y / scale) * scale;
				scale *= 2.0;
			}
			return value;
		}
	}

	/** DistortedNoise: one noise sampled with its x shifted by another. */
	static final class Distorted extends ClassicNoise {
		private final ClassicNoise source;
		private final ClassicNoise distortion;

		Distorted(ClassicNoise source, ClassicNoise distortion) {
			this.source = source;
			this.distortion = distortion;
		}

		@Override
		double sample(double x, double y) {
			return source.sample(x + distortion.sample(x, y), y);
		}
	}
}
