package dev.retroeras.worldgen.infdev;

import java.util.Arrays;
import java.util.Random;

/**
 * Infdev 20100618's improved Perlin noise and its octave wrapper, ported exactly. Unlike Beta 1.7.3 it has no
 * separate 2D path for bulk sampling: everything goes through the 3D code, including its quirk of only recomputing
 * lattice corners when the integer Y changes. It also offers single-point sampling, which the surface pass uses.
 */
final class InfdevNoise {
	private final int[] permutations = new int[512];
	private final double xCoord;
	private final double yCoord;
	private final double zCoord;

	InfdevNoise(Random random) {
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

	double sample(double x, double y, double z) {
		x += xCoord;
		y += yCoord;
		z += zCoord;
		int fx = floor(x);
		int fy = floor(y);
		int fz = floor(z);
		int cx = fx & 255;
		int cy = fy & 255;
		int cz = fz & 255;
		x -= fx;
		y -= fy;
		z -= fz;
		double u = fade(x);
		double v = fade(y);
		double w = fade(z);
		int a = permutations[cx] + cy;
		int aa = permutations[a] + cz;
		int ab = permutations[a + 1] + cz;
		int b = permutations[cx + 1] + cy;
		int ba = permutations[b] + cz;
		int bb = permutations[b + 1] + cz;
		return lerp(w,
				lerp(v,
						lerp(u, grad(permutations[aa], x, y, z), grad(permutations[ba], x - 1.0, y, z)),
						lerp(u, grad(permutations[ab], x, y - 1.0, z), grad(permutations[bb], x - 1.0, y - 1.0, z))),
				lerp(v,
						lerp(u, grad(permutations[aa + 1], x, y, z - 1.0), grad(permutations[ba + 1], x - 1.0, y, z - 1.0)),
						lerp(u, grad(permutations[ab + 1], x, y - 1.0, z - 1.0), grad(permutations[bb + 1], x - 1.0, y - 1.0, z - 1.0))));
	}

	/** Adds this octave into {@code out}, laid out x-major then z then y. */
	void add(double[] out, int x, int y, int z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ, double amplitude) {
		int index = 0;
		double inverse = 1.0 / amplitude;
		int lastY = -1;
		double x1 = 0.0;
		double x2 = 0.0;
		double x3 = 0.0;
		double x4 = 0.0;
		for (int ix = 0; ix < sizeX; ix++) {
			double px = (x + ix) * scaleX + xCoord;
			int fx = floor(px);
			int cx = fx & 255;
			px -= fx;
			double ux = fade(px);
			for (int iz = 0; iz < sizeZ; iz++) {
				double pz = (z + iz) * scaleZ + zCoord;
				int fz = floor(pz);
				int cz = fz & 255;
				pz -= fz;
				double uz = fade(pz);
				for (int iy = 0; iy < sizeY; iy++) {
					double py = (y + iy) * scaleY + yCoord;
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

	private static double grad(int hash, double x, double y, double z) {
		int h = hash & 15;
		double u = h < 8 ? x : y;
		double v = h < 4 ? y : h != 12 && h != 14 ? z : x;
		return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
	}

	/** Octaves that halve in frequency and double in amplitude, like Beta's. */
	static final class Octaves {
		private final InfdevNoise[] octaves;

		Octaves(Random random, int count) {
			octaves = new InfdevNoise[count];
			for (int i = 0; i < count; i++) {
				octaves[i] = new InfdevNoise(random);
			}
		}

		double sample(double x, double z) {
			return sample(x, z, 0.0);
		}

		double sample(double x, double y, double z) {
			double total = 0.0;
			double frequency = 1.0;
			for (InfdevNoise octave : octaves) {
				total += octave.sample(x * frequency, y * frequency, z * frequency) / frequency;
				frequency /= 2.0;
			}
			return total;
		}

		double[] sample(int x, int y, int z, int sizeX, int sizeY, int sizeZ, double scaleX, double scaleY, double scaleZ) {
			double[] out = new double[sizeX * sizeY * sizeZ];
			Arrays.fill(out, 0.0);
			double frequency = 1.0;
			for (InfdevNoise octave : octaves) {
				octave.add(out, x, y, z, sizeX, sizeY, sizeZ, scaleX * frequency, scaleY * frequency, scaleZ * frequency, frequency);
				frequency /= 2.0;
			}
			return out;
		}
	}
}
