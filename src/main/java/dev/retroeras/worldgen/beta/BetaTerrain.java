package dev.retroeras.worldgen.beta;

import java.util.Random;

/**
 * Beta 1.7.3's ChunkProviderGenerate terrain, surface and cave passes, ported faithfully so a seed produces the same
 * landscape it did in Beta. Chunks are produced as Beta block ids in Beta's own array layout
 * ({@code x << 11 | z << 7 | y}); the chunk generator maps those to modern block states.
 * <p>
 * Thread-safe: the noise generators are immutable after construction and every call uses its own buffers.
 */
public final class BetaTerrain {
	public static final int HEIGHT = 128;
	public static final int SEA_LEVEL = 64;

	static final byte AIR = 0;
	static final byte STONE = 1;
	static final byte GRASS = 2;
	static final byte DIRT = 3;
	static final byte BEDROCK = 7;
	static final byte WATER = 9;
	static final byte LAVA = 11;
	static final byte SAND = 12;
	static final byte GRAVEL = 13;
	static final byte SANDSTONE = 24;
	static final byte ICE = 79;

	private final long seed;
	private final BetaOctaveNoise minLimitNoise;
	private final BetaOctaveNoise maxLimitNoise;
	private final BetaOctaveNoise mainNoise;
	private final BetaOctaveNoise beachNoise;
	private final BetaOctaveNoise surfaceNoise;
	private final BetaOctaveNoise scaleNoise;
	private final BetaOctaveNoise depthNoise;
	private final BetaClimate climate;
	private final BetaCaves caves;
	private final boolean sandstone;

	public BetaTerrain(long seed) {
		this(seed, true);
	}

	/** {@code sandstone} false generates Alpha 1.2.6's terrain, which had no sandstone under its sand. */
	public BetaTerrain(long seed, boolean sandstone) {
		this.seed = seed;
		this.sandstone = sandstone;
		// Same construction order as Beta, since every generator draws from the one Random.
		Random random = new Random(seed);
		minLimitNoise = new BetaOctaveNoise(random, 16);
		maxLimitNoise = new BetaOctaveNoise(random, 16);
		mainNoise = new BetaOctaveNoise(random, 8);
		beachNoise = new BetaOctaveNoise(random, 4);
		surfaceNoise = new BetaOctaveNoise(random, 4);
		scaleNoise = new BetaOctaveNoise(random, 10);
		depthNoise = new BetaOctaveNoise(random, 16);
		climate = new BetaClimate(seed);
		caves = new BetaCaves(seed);
	}

	public long seed() {
		return seed;
	}

	public BetaClimate climate() {
		return climate;
	}

	/** A generated chunk: Beta block ids, plus the climate the chunk was built with. */
	public record Chunk(byte[] blocks, BetaClimate.Area climate) {
		public byte get(int x, int y, int z) {
			return blocks[x << 11 | z << 7 | y];
		}
	}

	/** Beta's provideChunk, without lighting and decoration. */
	public Chunk generate(int chunkX, int chunkZ, boolean withCaves) {
		Random random = new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
		byte[] blocks = new byte[16 * 16 * HEIGHT];
		BetaClimate.Area area = climate.sample(chunkX * 16, chunkZ * 16, 16, 16);
		generateTerrain(chunkX, chunkZ, blocks, area);
		replaceBlocksForBiome(chunkX, chunkZ, blocks, area.biomes(), random);
		if (withCaves) {
			caves.carve(chunkX, chunkZ, blocks);
		}
		return new Chunk(blocks, area);
	}

	private void generateTerrain(int chunkX, int chunkZ, byte[] blocks, BetaClimate.Area area) {
		int cells = 4;
		int sizeXZ = cells + 1;
		int sizeY = 17;
		double[] density = sampleDensity(chunkX * cells, 0, chunkZ * cells, sizeXZ, sizeY, sizeXZ, area);
		double[] temperature = area.temperature();

		for (int cx = 0; cx < cells; cx++) {
			for (int cz = 0; cz < cells; cz++) {
				for (int cy = 0; cy < 16; cy++) {
					double yStep = 0.125;
					double d000 = density[((cx) * sizeXZ + cz) * sizeY + cy];
					double d010 = density[((cx) * sizeXZ + cz + 1) * sizeY + cy];
					double d100 = density[((cx + 1) * sizeXZ + cz) * sizeY + cy];
					double d110 = density[((cx + 1) * sizeXZ + cz + 1) * sizeY + cy];
					double dy000 = (density[((cx) * sizeXZ + cz) * sizeY + cy + 1] - d000) * yStep;
					double dy010 = (density[((cx) * sizeXZ + cz + 1) * sizeY + cy + 1] - d010) * yStep;
					double dy100 = (density[((cx + 1) * sizeXZ + cz) * sizeY + cy + 1] - d100) * yStep;
					double dy110 = (density[((cx + 1) * sizeXZ + cz + 1) * sizeY + cy + 1] - d110) * yStep;

					for (int sy = 0; sy < 8; sy++) {
						double xStep = 0.25;
						double z0 = d000;
						double z1 = d010;
						double dz0 = (d100 - d000) * xStep;
						double dz1 = (d110 - d010) * xStep;
						for (int sx = 0; sx < 4; sx++) {
							int index = (sx + cx * 4) << 11 | (cz * 4) << 7 | (cy * 8 + sy);
							double zStep = 0.25;
							double value = z0;
							double dValue = (z1 - z0) * zStep;
							for (int sz = 0; sz < 4; sz++) {
								int y = cy * 8 + sy;
								byte block = AIR;
								if (y < SEA_LEVEL) {
									double t = temperature[(cx * 4 + sx) * 16 + (cz * 4 + sz)];
									block = t < 0.5 && y >= SEA_LEVEL - 1 ? ICE : WATER;
								}
								if (value > 0.0) {
									block = STONE;
								}
								blocks[index] = block;
								index += 128;
								value += dValue;
							}
							z0 += dz0;
							z1 += dz1;
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

	/** Beta's func_4061_a: the coarse density field the terrain is interpolated from. */
	private double[] sampleDensity(int x, int y, int z, int sizeX, int sizeY, int sizeZ, BetaClimate.Area area) {
		double xzScale = 684.412;
		double yScale = 684.412;
		double[] temperature = area.temperature();
		double[] humidity = area.humidity();
		double[] scale = scaleNoise.sample2D(null, x, z, sizeX, sizeZ, 1.121, 1.121);
		double[] depth = depthNoise.sample2D(null, x, z, sizeX, sizeZ, 200.0, 200.0);
		double[] main = mainNoise.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale / 80.0, yScale / 160.0, xzScale / 80.0);
		double[] min = minLimitNoise.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] max = maxLimitNoise.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] out = new double[sizeX * sizeY * sizeZ];

		int index = 0;
		int column = 0;
		int step = 16 / sizeX;
		for (int ix = 0; ix < sizeX; ix++) {
			int bx = ix * step + step / 2;
			for (int iz = 0; iz < sizeZ; iz++) {
				int bz = iz * step + step / 2;
				double t = temperature[bx * 16 + bz];
				double h = humidity[bx * 16 + bz] * t;
				double dryness = 1.0 - h;
				dryness *= dryness;
				dryness *= dryness;
				dryness = 1.0 - dryness;

				double scaleValue = (scale[column] + 256.0) / 512.0;
				scaleValue *= dryness;
				if (scaleValue > 1.0) {
					scaleValue = 1.0;
				}
				double depthValue = depth[column] / 8000.0;
				if (depthValue < 0.0) {
					depthValue = -depthValue * 0.3;
				}
				depthValue = depthValue * 3.0 - 2.0;
				if (depthValue < 0.0) {
					depthValue /= 2.0;
					if (depthValue < -1.0) {
						depthValue = -1.0;
					}
					depthValue /= 1.4;
					depthValue /= 2.0;
					scaleValue = 0.0;
				} else {
					if (depthValue > 1.0) {
						depthValue = 1.0;
					}
					depthValue /= 8.0;
				}
				if (scaleValue < 0.0) {
					scaleValue = 0.0;
				}
				scaleValue += 0.5;
				depthValue = depthValue * sizeY / 16.0;
				double centre = sizeY / 2.0 + depthValue * 4.0;
				column++;

				for (int iy = 0; iy < sizeY; iy++) {
					double value;
					double falloff = (iy - centre) * 12.0 / scaleValue;
					if (falloff < 0.0) {
						falloff *= 4.0;
					}
					double low = min[index] / 512.0;
					double high = max[index] / 512.0;
					double blend = (main[index] / 10.0 + 1.0) / 2.0;
					if (blend < 0.0) {
						value = low;
					} else if (blend > 1.0) {
						value = high;
					} else {
						value = low + (high - low) * blend;
					}
					value -= falloff;
					if (iy > sizeY - 4) {
						double top = (float) (iy - (sizeY - 4)) / 3.0F;
						value = value * (1.0 - top) + -10.0 * top;
					}
					out[index] = value;
					index++;
				}
			}
		}
		return out;
	}

	/** Beta's replaceBlocksForBiome: bedrock, grass/dirt or sand tops, sand and gravel beaches, and sandstone. */
	private void replaceBlocksForBiome(int chunkX, int chunkZ, byte[] blocks, BetaBiome[] biomes, Random random) {
		double scale = 0.03125;
		double[] sand = beachNoise.sample(null, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, scale, scale, 1.0);
		double[] gravel = beachNoise.sample(null, chunkX * 16, 109.0134, chunkZ * 16, 16, 1, 16, scale, 1.0, scale);
		double[] stone = surfaceNoise.sample(null, chunkX * 16, chunkZ * 16, 0.0, 16, 16, 1, scale * 2.0, scale * 2.0, scale * 2.0);

		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				BetaBiome biome = biomes[z + x * 16];
				boolean sandBeach = sand[z + x * 16] + random.nextDouble() * 0.2 > 0.0;
				boolean gravelBeach = gravel[z + x * 16] + random.nextDouble() * 0.2 > 3.0;
				int depth = (int) (stone[z + x * 16] / 3.0 + 3.0 + random.nextDouble() * 0.25);
				int remaining = -1;
				byte top = biome.sandy() ? SAND : GRASS;
				byte filler = biome.sandy() ? SAND : DIRT;

				for (int y = HEIGHT - 1; y >= 0; y--) {
					int index = (x * 16 + z) * HEIGHT + y;
					if (y <= random.nextInt(5)) {
						blocks[index] = BEDROCK;
						continue;
					}
					byte block = blocks[index];
					if (block == AIR) {
						remaining = -1;
						continue;
					}
					if (block != STONE) {
						continue;
					}
					if (remaining == -1) {
						if (depth <= 0) {
							top = AIR;
							filler = STONE;
						} else if (y >= SEA_LEVEL - 4 && y <= SEA_LEVEL + 1) {
							top = biome.sandy() ? SAND : GRASS;
							filler = biome.sandy() ? SAND : DIRT;
							if (gravelBeach) {
								top = AIR;
								filler = GRAVEL;
							}
							if (sandBeach) {
								top = SAND;
								filler = SAND;
							}
						}
						if (y < SEA_LEVEL && top == AIR) {
							top = WATER;
						}
						remaining = depth;
						blocks[index] = y >= SEA_LEVEL - 1 ? top : filler;
						continue;
					}
					if (remaining > 0) {
						remaining--;
						blocks[index] = filler;
						if (sandstone && remaining == 0 && filler == SAND) {
							remaining = random.nextInt(4);
							filler = SANDSTONE;
						}
					}
				}
			}
		}
	}
}
