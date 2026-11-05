package dev.retroeras.worldgen.cavegame;

/**
 * The world of the very first Cave game build, rd-132211 (com.mojang.rubydung.level.Level), ported exactly: a flat
 * 256x256x64 slab, solid from the bottom up to two thirds of the height (y = 42). The top layer is grass, and everything
 * under it rock. There were no hills, dirt, caves or trees yet; outside the slab is void. The seed changes nothing,
 * since every rd-132211 world was the same.
 */
public final class CavegameTerrain {
	public static final int SIZE = 256;
	public static final int HEIGHT = 64;
	/** {@code depth * 2 / 3}: the top solid layer, the only one drawn with the grass texture. */
	public static final int SURFACE = HEIGHT * 2 / 3;

	public static final byte AIR = 0;
	public static final byte ROCK = 1;
	public static final byte GRASS = 2;

	private final long seed;

	public CavegameTerrain(long seed) {
		this.seed = seed;
	}

	public long seed() {
		return seed;
	}

	public static boolean inWorld(int x, int z) {
		return x >= 0 && z >= 0 && x < SIZE && z < SIZE;
	}

	/** The block at a world position: grass on the top layer, rock below it, air above and outside. */
	public byte get(int x, int y, int z) {
		if (!inWorld(x, z) || y < 0 || y > SURFACE) {
			return AIR;
		}
		return y == SURFACE ? GRASS : ROCK;
	}
}
