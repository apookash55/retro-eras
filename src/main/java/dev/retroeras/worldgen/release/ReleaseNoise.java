package dev.retroeras.worldgen.release;

import java.util.Arrays;
import java.util.Random;

/**
 * Minecraft 1.6.4's improved Perlin noise and octaves, ported exactly. The Perlin sampler is Beta's (including the 2D
 * path and the Y-caching quirk); what changed is that the octave wrapper pre-scales the start coordinate and wraps it
 * to +-16777216, which keeps terrain intact far from the origin.
 */
final class ReleaseNoise {
	private final int[] permutations = new int[512];
	private final double xCoord;
	private final double yCoord;
	private final double zCoord;

	ReleaseNoise(Random random) {
		xCoord = random.nextDouble() * 256.0;
		yCoord = random.nextDouble() * 256.0;
		zCoord = random.nextDouble() * 256.0;
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

	/** Adds this octave into {@code out} (x-major, then z, then y). Start coordinates are already scaled. */
	void add(double[] out, double x, double y, double z, int sizeX, int sizeY, int sizeZ,
			double scaleX, double scaleY, double scaleZ, double amplitude) {
		double inverse = 1.0 / amplitude;
		int index = 0;
		if (sizeY == 1) {
			for (int ix = 0; ix < sizeX; ix++) {
				double px = x + ix * scaleX + xCoord;
				int fx = floor(px);
				int cx = fx & 255;
				px -= fx;
				double ux = fade(px);
				for (int iz = 0; iz < sizeZ; iz++) {
					double pz = z + iz * scaleZ + zCoord;
					int fz = floor(pz);
					int cz = fz & 255;
					pz -= fz;
					double uz = fade(pz);
					int a = permutations[cx];
					int aa = permutations[a] + cz;
					int b = permutations[cx + 1];
					int ba = permutations[b] + cz;
					double x1 = lerp(ux, grad2D(permutations[aa], px, pz), grad(permutations[ba], px - 1.0, 0.0, pz));
					double x2 = lerp(ux, grad(permutations[aa + 1], px, 0.0, pz - 1.0), grad(permutations[ba + 1], px - 1.0, 0.0, pz - 1.0));
					out[index++] += lerp(uz, x1, x2) * inverse;
				}
			}
			return;
		}

		int lastY = -1;
		double x1 = 0.0;
		double x2 = 0.0;
		double x3 = 0.0;
		double x4 = 0.0;
		for (int ix = 0; ix < sizeX; ix++) {
			double px = x + ix * scaleX + xCoord;
			int fx = floor(px);
			int cx = fx & 255;
			px -= fx;
			double ux = fade(px);
			for (int iz = 0; iz < sizeZ; iz++) {
				double pz = z + iz * scaleZ + zCoord;
				int fz = floor(pz);
				int cz = fz & 255;
				pz -= fz;
				double uz = fade(pz);
				for (int iy = 0; iy < sizeY; iy++) {
					double py = y + iy * scaleY + yCoord;
					int fy = floor(py);
					int cy = fy & 255;
					py -= fy;
					double uy = fade(py);
					if (iy == 0 || cy != lastY) {
						lastY = cy;
						int a = permutations[cx] + cy;
						int aa = permutations[a] + cz;
						int ab = permutations[a + 1] + cz;
						int b = permutations[cx + 1] + cy;
						int ba = permutations[b] + cz;
						int bb = permutations[b + 1] + cz;
						x1 = lerp(ux, grad(permutations[aa], px, py, pz), grad(permutations[ba], px - 1.0, py, pz));
						x2 = lerp(ux, grad(permutations[ab], px, py - 1.0, pz), grad(permutations[bb], px - 1.0, py - 1.0, pz));
						x3 = lerp(ux, grad(permutations[aa + 1], px, py, pz - 1.0), grad(permutations[ba + 1], px - 1.0, py, pz - 1.0));
						x4 = lerp(ux, grad(permutations[ab + 1], px, py - 1.0, pz - 1.0), grad(permutations[bb + 1], px - 1.0, py - 1.0, pz - 1.0));
					}
					out[index++] += lerp(uz, lerp(uy, x1, x2), lerp(uy, x3, x4)) * inverse;
				}
			}
		}
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	private static double fade(double t) {
		return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
	}

	private static double lerp(double t, double a, double b) {
		return a + t * (b - a);
	}

	private static double grad2D(int hash, double x, double z) {
		int h = hash & 15;
		double u = (1 - ((h & 8) >> 3)) * x;
		double v = h < 4 ? 0.0 : h != 12 && h != 14 ? z : x;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	private static double grad(int hash, double x, double y, double z) {
		int h = hash & 15;
		double u = h < 8 ? x : y;
		double v = h < 4 ? y : h != 12 && h != 14 ? z : x;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	/** 1.6.4's NoiseGeneratorOctaves. */
	static final class Octaves {
		private final ReleaseNoise[] octaves;

		Octaves(Random random, int count) {
			octaves = new ReleaseNoise[count];
			for (int i = 0; i < count; i++) {
				octaves[i] = new ReleaseNoise(random);
			}
		}

		double[] sample(int x, int y, int z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ) {
			double[] out = new double[sizeX * sizeY * sizeZ];
			Arrays.fill(out, 0.0);
			double frequency = 1.0;
			for (ReleaseNoise octave : octaves) {
				double px = x * frequency * scaleX;
				double py = y * frequency * scaleY;
				double pz = z * frequency * scaleZ;
				long wrapX = lfloor(px);
				long wrapZ = lfloor(pz);
				px -= wrapX;
				pz -= wrapZ;
				wrapX %= 16777216L;
				wrapZ %= 16777216L;
				px += wrapX;
				pz += wrapZ;
				octave.add(out, px, py, pz, sizeX, sizeY, sizeZ, scaleX * frequency, scaleY * frequency, scaleZ * frequency, frequency);
				frequency /= 2.0;
			}
			return out;
		}

		/** The 2D form, which samples at y = 10 with a single y step. */
		double[] sample2D(int x, int z, int sizeX, int sizeZ, double scaleX, double scaleZ) {
			return sample(x, 10, z, sizeX, 1, sizeZ, scaleX, 1.0, scaleZ);
		}

		private static long lfloor(double value) {
			long l = (long) value;
			return value < l ? l - 1L : l;
		}
	}
}
