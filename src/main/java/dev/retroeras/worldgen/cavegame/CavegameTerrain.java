package dev.retroeras.worldgen.cavegame;

import java.util.Random;

/**
 * The world of pre-classic Cave game rd-161348 (com.mojang.minecraft.level.Level.generateMap), ported exactly except
 * that it is seeded: the original used unseeded Randoms, so every launch made a new map. The world is a finite
 * 256x256x64 block of grass over dirt over rock; outside it is void.
 */
public final class CavegameTerrain {
	public static final int SIZE = 256;
	public static final int HEIGHT = 64;

	public static final byte AIR = 0;
	public static final byte ROCK = 1;
	public static final byte GRASS = 2;
	public static final byte DIRT = 3;

	private final long seed;
	/** Per column (x + z * SIZE): the grass layer's y and the highest rock y. */
	private final int[] grassHeight = new int[SIZE * SIZE];
	private final int[] rockHeight = new int[SIZE * SIZE];

	public CavegameTerrain(long seed) {
		this.seed = seed;
		int[] heightmap1 = diamondSquare(0, new Random(seed));
		int[] heightmap2 = diamondSquare(0, new Random(seed * 31L + 1L));
		int[] blend = diamondSquare(1, new Random(seed * 31L + 2L));
		int[] rockMap = diamondSquare(1, new Random(seed * 31L + 3L));

		for (int i = 0; i < SIZE * SIZE; i++) {
			int dh1 = heightmap1[i];
			int dh2 = blend[i] < 128 ? dh1 : heightmap2[i];
			int dh = Math.max(dh1, dh2) / 8 + HEIGHT / 3;
			int rh = rockMap[i] / 8 + HEIGHT / 3;
			grassHeight[i] = dh;
			rockHeight[i] = Math.min(rh, dh - 2);
		}
	}

	public long seed() {
		return seed;
	}

	public static boolean inWorld(int x, int z) {
		return x >= 0 && z >= 0 && x < SIZE && z < SIZE;
	}

	/** The block at a world position, following generateMap's layer order: grass, then dirt below it, then rock. */
	public byte get(int x, int y, int z) {
		if (!inWorld(x, z) || y < 0 || y >= HEIGHT) {
			return AIR;
		}
		int column = x + z * SIZE;
		byte id = AIR;
		if (y == grassHeight[column]) {
			id = GRASS;
		}
		if (y < grassHeight[column]) {
			id = DIRT;
		}
		if (y <= rockHeight[column]) {
			id = ROCK;
		}
		return id;
	}

	/** PerlinNoiseFilter.read: a wrapping diamond-square height map in roughly 0..255. */
	private static int[] diamondSquare(int levels, Random random) {
		int width = SIZE;
		int height = SIZE;
		int fuzz = 16;
		int[] tmp = new int[width * height];
		int step = width >> levels;

		for (int y = 0; y < height; y += step) {
			for (int x = 0; x < width; x += step) {
				tmp[x + y * width] = (random.nextInt(256) - 128) * fuzz;
			}
		}

		for (int stepX = width >> levels; stepX > 1; stepX /= 2) {
			int val = 256 * (stepX << levels);
			int ss = stepX / 2;

			for (int y = 0; y < height; y += stepX) {
				for (int x = 0; x < width; x += stepX) {
					int ul = tmp[(x) % width + (y) % height * width];
					int ur = tmp[(x + stepX) % width + (y) % height * width];
					int dl = tmp[(x) % width + (y + stepX) % height * width];
					int dr = tmp[(x + stepX) % width + (y + stepX) % height * width];
					int m = (ul + dl + ur + dr) / 4 + random.nextInt(val * 2) - val;
					tmp[x + ss + (y + ss) * width] = m;
				}
			}

			for (int y = 0; y < height; y += stepX) {
				for (int x = 0; x < width; x += stepX) {
					int c = tmp[x + y * width];
					int r = tmp[(x + stepX) % width + y * width];
					int d = tmp[x + (y + stepX) % width * width];
					int mu = tmp[(x + ss & width - 1) + (y + ss - stepX & height - 1) * width];
					int ml = tmp[(x + ss - stepX & width - 1) + (y + ss & height - 1) * width];
					int m = tmp[(x + ss) % width + (y + ss) % height * width];
					int u = (c + r + m + mu) / 4 + random.nextInt(val * 2) - val;
					int l = (c + d + m + ml) / 4 + random.nextInt(val * 2) - val;
					tmp[x + ss + y * width] = u;
					tmp[x + (y + ss) * width] = l;
				}
			}
		}

		int[] result = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result[x + y * width] = tmp[x % width + y % height * width] / 512 + 128;
			}
		}
		return result;
	}
}
