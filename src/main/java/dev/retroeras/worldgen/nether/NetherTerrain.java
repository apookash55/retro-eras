package dev.retroeras.worldgen.nether;

import java.util.Random;

import dev.retroeras.worldgen.LegacyOctaves;
import dev.retroeras.worldgen.beta.BetaOctaveNoise;
import dev.retroeras.worldgen.release.ReleaseNoise;

/**
 * The Nether's ChunkProviderHell, ported from Beta 1.7.3 (which Alpha 1.2's Halloween Update had already used) and from
 * 1.6.4. The two are the same algorithm: a 128-high density field closed off at the top and bottom, lava below y = 32,
 * soul sand and gravel shelves around y = 64, bedrock at both ends and MapGenCavesHell's tunnels. 1.6.4's noise wraps its
 * coordinates and samples the gravel noise at y = 109 rather than 109.0134, so the same seed gives a slightly different Nether.
 */
public final class NetherTerrain {
	public static final int HEIGHT = 128;
	public static final int LAVA_LEVEL = 32;

	static final byte AIR = 0;
	static final byte GRASS = 2;
	static final byte DIRT = 3;
	static final byte BEDROCK = 7;
	static final byte FLOWING_LAVA = 10;
	static final byte LAVA = 11;
	static final byte GRAVEL = 13;
	static final byte NETHERRACK = 87;
	static final byte SOUL_SAND = 88;

	/** Which version's Nether: Beta's (also Alpha's) or 1.6.4's. */
	public enum Version {
		BETA(BetaOctaveNoise::legacy, 109.0134),
		RELEASE(ReleaseNoise::legacy, 109.0);

		private final LegacyOctaves.Factory octaves;
		private final double gravelY;

		Version(LegacyOctaves.Factory octaves, double gravelY) {
			this.octaves = octaves;
			this.gravelY = gravelY;
		}
	}

	private final long seed;
	private final LegacyOctaves minLimit;
	private final LegacyOctaves maxLimit;
	private final LegacyOctaves main;
	private final LegacyOctaves surface;
	private final LegacyOctaves depth;
	private final double gravelY;
	private final NetherCaves caves;

	public NetherTerrain(long seed, Version version) {
		this.seed = seed;
		this.gravelY = version.gravelY;
		Random random = new Random(seed);
		minLimit = version.octaves.create(random, 16);
		maxLimit = version.octaves.create(random, 16);
		main = version.octaves.create(random, 8);
		surface = version.octaves.create(random, 4);
		depth = version.octaves.create(random, 4);
		// The scale (10 octaves) and depth (16) noises came next and were sampled too, but nothing used their values.
		this.caves = new NetherCaves(seed);
	}

	public long seed() {
		return seed;
	}

	/** One generated chunk, in the old x-z-y byte layout. */
	public record Chunk(byte[] blocks) {
		public byte get(int x, int y, int z) {
			return blocks[(x * 16 + z) * HEIGHT + y];
		}
	}

	/** provideChunk: terrain, then shelves and bedrock, then caves. */
	public Chunk generate(int chunkX, int chunkZ, boolean withCaves) {
		Random random = new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
		byte[] blocks = new byte[16 * 16 * HEIGHT];
		shape(chunkX, chunkZ, blocks);
		surface(random, chunkX, chunkZ, blocks);
		if (withCaves) {
			caves.carve(chunkX, chunkZ, blocks);
		}
		return new Chunk(blocks);
	}

	/** generateNetherTerrain: a 5x17x5 density field stretched over the chunk; solid where positive, lava below y = 32. */
	private void shape(int chunkX, int chunkZ, byte[] blocks) {
		int cells = 4;
		int sizeX = cells + 1;
		int sizeY = 17;
		int sizeZ = cells + 1;
		double[] density = density(chunkX * cells, 0, chunkZ * cells, sizeX, sizeY, sizeZ);
		for (int cx = 0; cx < cells; cx++) {
			for (int cz = 0; cz < cells; cz++) {
				for (int cy = 0; cy < 16; cy++) {
					double stepY = 0.125;
					double d000 = density[((cx + 0) * sizeZ + cz + 0) * sizeY + cy + 0];
					double d010 = density[((cx + 0) * sizeZ + cz + 1) * sizeY + cy + 0];
					double d100 = density[((cx + 1) * sizeZ + cz + 0) * sizeY + cy + 0];
					double d110 = density[((cx + 1) * sizeZ + cz + 1) * sizeY + cy + 0];
					double dy000 = (density[((cx + 0) * sizeZ + cz + 0) * sizeY + cy + 1] - d000) * stepY;
					double dy010 = (density[((cx + 0) * sizeZ + cz + 1) * sizeY + cy + 1] - d010) * stepY;
					double dy100 = (density[((cx + 1) * sizeZ + cz + 0) * sizeY + cy + 1] - d100) * stepY;
					double dy110 = (density[((cx + 1) * sizeZ + cz + 1) * sizeY + cy + 1] - d110) * stepY;
					for (int sy = 0; sy < 8; sy++) {
						double stepX = 0.25;
						double x0 = d000;
						double x1 = d010;
						double dx0 = (d100 - d000) * stepX;
						double dx1 = (d110 - d010) * stepX;
						for (int sx = 0; sx < 4; sx++) {
							int index = sx + cx * 4 << 11 | 0 + cz * 4 << 7 | cy * 8 + sy;
							double stepZ = 0.25;
							double value = x0;
							double dz = (x1 - x0) * stepZ;
							for (int sz = 0; sz < 4; sz++) {
								byte block = AIR;
								if (cy * 8 + sy < LAVA_LEVEL) {
									block = LAVA;
								}
								if (value > 0.0) {
									block = NETHERRACK;
								}
								blocks[index] = block;
								index += HEIGHT;
								value += dz;
							}
							x0 += dx0;
							x1 += dx1;
						}
						d000 += dy000;
						d010 += dy010;
						d100 += dy100;
						d110 += dy110;
					}
				}
			}
		}
	}

	private double[] density(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		double[] out = new double[sizeX * sizeY * sizeZ];
		double xzScale = 684.412;
		double yScale = 2053.236;
		double[] mainValues = main.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale / 80.0, yScale / 60.0, xzScale / 80.0);
		double[] minValues = minLimit.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] maxValues = maxLimit.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);

		// The roof and floor: a gentle wave over the height, pulled sharply solid in the bottom and top four cells.
		double[] falloff = new double[sizeY];
		for (int i = 0; i < sizeY; i++) {
			falloff[i] = Math.cos(i * Math.PI * 6.0 / sizeY) * 2.0;
			double edge = i;
			if (i > sizeY / 2) {
				edge = sizeY - 1 - i;
			}
			if (edge < 4.0) {
				edge = 4.0 - edge;
				falloff[i] -= edge * edge * edge * 10.0;
			}
		}

		int index = 0;
		for (int ix = 0; ix < sizeX; ix++) {
			for (int iz = 0; iz < sizeZ; iz++) {
				for (int iy = 0; iy < sizeY; iy++) {
					double low = minValues[index] / 512.0;
					double high = maxValues[index] / 512.0;
					double blend = (mainValues[index] / 10.0 + 1.0) / 2.0;
					double value;
					if (blend < 0.0) {
						value = low;
					} else if (blend > 1.0) {
						value = high;
					} else {
						value = low + (high - low) * blend;
					}
					value -= falloff[iy];
					if (iy > sizeY - 4) {
						double top = (iy - (sizeY - 4)) / 3.0F;
						value = value * (1.0 - top) + -10.0 * top;
					}
					out[index] = value;
					index++;
				}
			}
		}
		return out;
	}

	/** replaceBlocksForBiome: bedrock at both ends, and soul sand or gravel shelves where the ground meets y = 60 to 65. */
	private void surface(Random random, int chunkX, int chunkZ, byte[] blocks) {
		int shelf = 64;
		double scale = 0.03125;
		double[] soulSandNoise = surface.sample(null, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, scale, scale, 1.0);
		double[] gravelNoise = surface.sample(null, chunkX * 16, gravelY, chunkZ * 16, 16, 1, 16, scale, 1.0, scale);
		double[] depthNoise = depth.sample(null, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, scale * 2.0, scale * 2.0, scale * 2.0);

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				boolean soulSand = soulSandNoise[x + z * 16] + random.nextDouble() * 0.2 > 0.0;
				boolean gravel = gravelNoise[x + z * 16] + random.nextDouble() * 0.2 > 0.0;
				int thickness = (int) (depthNoise[x + z * 16] / 3.0 + 3.0 + random.nextDouble() * 0.25);
				int run = -1;
				byte top = NETHERRACK;
				byte filler = NETHERRACK;
				for (int y = HEIGHT - 1; y >= 0; y--) {
					int index = (z * 16 + x) * HEIGHT + y;
					if (y >= HEIGHT - 1 - random.nextInt(5) || y <= random.nextInt(5)) {
						blocks[index] = BEDROCK;
						continue;
					}
					byte block = blocks[index];
					if (block == AIR) {
						run = -1;
					} else if (block == NETHERRACK) {
						if (run == -1) {
							if (thickness <= 0) {
								top = AIR;
								filler = NETHERRACK;
							} else if (y >= shelf - 4 && y <= shelf + 1) {
								top = NETHERRACK;
								filler = NETHERRACK;
								if (gravel) {
									top = GRAVEL;
									filler = NETHERRACK;
								}
								if (soulSand) {
									top = SOUL_SAND;
									filler = SOUL_SAND;
								}
							}
							if (y < shelf && top == AIR) {
								top = LAVA;
							}
							run = thickness;
							blocks[index] = y >= shelf - 1 ? top : filler;
						} else if (run > 0) {
							run--;
							blocks[index] = filler;
						}
					}
				}
			}
		}
	}
}
