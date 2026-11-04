package dev.retroeras.worldgen.release;

import java.util.Random;

/**
 * Minecraft 1.6.4's overworld terrain (ChunkProviderGenerate), surface, caves (MapGenCaves) and ravines
 * (MapGenRavine), ported faithfully so a seed gives the same landscape it did in 1.6.4. Terrain only reaches y = 127
 * in 1.6.4 even though the world is 256 tall. Chunks come out as block ids in the old {@code x << 11 | z << 7 | y}
 * layout, with the block-resolution biome ids alongside.
 * <p>
 * Structures (villages, temples, strongholds, mineshafts) are not ported.
 */
public final class ReleaseTerrain {
	public static final int HEIGHT = 128;
	public static final int SEA_LEVEL = 63;

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
	static final byte MYCELIUM = 110;

	private static final float[] SIN_TABLE = new float[65536];
	/** The parabolic 5x5 weights used to blend neighbouring biome heights. */
	private static final float[] BIOME_WEIGHTS = new float[25];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				BIOME_WEIGHTS[dx + 2 + (dz + 2) * 5] = 10.0F / (float) Math.sqrt(dx * dx + dz * dz + 0.2F);
			}
		}
	}

	private final long seed;
	private final ReleaseNoise.Octaves minLimitNoise;
	private final ReleaseNoise.Octaves maxLimitNoise;
	private final ReleaseNoise.Octaves mainNoise;
	private final ReleaseNoise.Octaves surfaceNoise;
	private final ReleaseNoise.Octaves depthNoise;
	private final ReleaseLayers layers;

	public ReleaseTerrain(long seed) {
		this.seed = seed;
		Random random = new Random(seed);
		minLimitNoise = new ReleaseNoise.Octaves(random, 16);
		maxLimitNoise = new ReleaseNoise.Octaves(random, 16);
		mainNoise = new ReleaseNoise.Octaves(random, 8);
		surfaceNoise = new ReleaseNoise.Octaves(random, 4);
		// 1.6.4 also builds a 10-octave scale noise here; it samples it but never uses the result.
		new ReleaseNoise.Octaves(random, 10);
		depthNoise = new ReleaseNoise.Octaves(random, 16);
		layers = new ReleaseLayers(seed);
	}

	public long seed() {
		return seed;
	}

	/** The biome at a block position, as the game sees it (block-resolution layer). */
	public ReleaseBiome biomeAt(int x, int z) {
		return ReleaseBiome.byIdOrOcean(layers.blockBiomes(x, z, 1, 1)[0]);
	}

	/** 1.6.4's WorldChunkManager.areBiomesViable: whether every quarter-resolution biome within {@code radius} is allowed. */
	public boolean areBiomesViable(int x, int z, int radius, java.util.Set<ReleaseBiome> allowed) {
		int minX = x - radius >> 2;
		int minZ = z - radius >> 2;
		int width = (x + radius >> 2) - minX + 1;
		int height = (z + radius >> 2) - minZ + 1;
		int[] biomes = layers.generationBiomes(minX, minZ, width, height);
		for (int i = 0; i < width * height; i++) {
			if (!allowed.contains(ReleaseBiome.byIdOrOcean(biomes[i]))) {
				return false;
			}
		}
		return true;
	}

	/** 1.6.4's WorldChunkManager.findBiomePosition: a random allowed spot within {@code range}, as {x, z}, or null. */
	public int @org.jspecify.annotations.Nullable [] findBiomePosition(int x, int z, int range, java.util.Set<ReleaseBiome> allowed, Random random) {
		int minX = x - range >> 2;
		int minZ = z - range >> 2;
		int width = (x + range >> 2) - minX + 1;
		int height = (z + range >> 2) - minZ + 1;
		int[] biomes = layers.generationBiomes(minX, minZ, width, height);
		int[] found = null;
		int count = 0;
		for (int i = 0; i < width * height; i++) {
			int bx = minX + i % width << 2;
			int bz = minZ + i / width << 2;
			if (allowed.contains(ReleaseBiome.byIdOrOcean(biomes[i])) && (found == null || random.nextInt(count + 1) == 0)) {
				found = new int[] {bx, bz};
				count++;
			}
		}
		return found;
	}

	/** Block-resolution biome ids for a 16x16 chunk, indexed {@code x + z * 16}. */
	public int[] chunkBiomes(int chunkX, int chunkZ) {
		return layers.blockBiomes(chunkX * 16, chunkZ * 16, 16, 16);
	}

	public record Chunk(byte[] blocks, int[] biomes) {
		public byte get(int x, int y, int z) {
			return blocks[x << 11 | z << 7 | y];
		}

		public ReleaseBiome biome(int x, int z) {
			return ReleaseBiome.byIdOrOcean(biomes[x + z * 16]);
		}
	}

	/** ChunkProviderGenerate.provideChunk, without structures, lighting and decoration. */
	public Chunk generate(int chunkX, int chunkZ, boolean withCarvers) {
		Random random = new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
		byte[] blocks = new byte[16 * 16 * HEIGHT];
		generateTerrain(chunkX, chunkZ, blocks);
		int[] biomes = chunkBiomes(chunkX, chunkZ);
		replaceBlocksForBiome(chunkX, chunkZ, blocks, biomes, random);
		if (withCarvers) {
			new Carver(seed, false).generate(chunkX, chunkZ, blocks, biomes);
			new Carver(seed, true).generate(chunkX, chunkZ, blocks, biomes);
		}
		return new Chunk(blocks, biomes);
	}

	private void generateTerrain(int chunkX, int chunkZ, byte[] blocks) {
		int[] biomes = layers.generationBiomes(chunkX * 4 - 2, chunkZ * 4 - 2, 10, 10);
		double[] density = sampleDensity(chunkX * 4, 0, chunkZ * 4, 5, 17, 5, biomes);

		for (int cx = 0; cx < 4; cx++) {
			for (int cz = 0; cz < 4; cz++) {
				for (int cy = 0; cy < 16; cy++) {
					double yStep = 0.125;
					double d000 = density[((cx) * 5 + cz) * 17 + cy];
					double d010 = density[((cx) * 5 + cz + 1) * 17 + cy];
					double d100 = density[((cx + 1) * 5 + cz) * 17 + cy];
					double d110 = density[((cx + 1) * 5 + cz + 1) * 17 + cy];
					double dy000 = (density[((cx) * 5 + cz) * 17 + cy + 1] - d000) * yStep;
					double dy010 = (density[((cx) * 5 + cz + 1) * 17 + cy + 1] - d010) * yStep;
					double dy100 = (density[((cx + 1) * 5 + cz) * 17 + cy + 1] - d100) * yStep;
					double dy110 = (density[((cx + 1) * 5 + cz + 1) * 17 + cy + 1] - d110) * yStep;

					for (int sy = 0; sy < 8; sy++) {
						double xStep = 0.25;
						double z0 = d000;
						double z1 = d010;
						double dz0 = (d100 - d000) * xStep;
						double dz1 = (d110 - d010) * xStep;
						for (int sx = 0; sx < 4; sx++) {
							int index = (sx + cx * 4) << 11 | (cz * 4) << 7 | (cy * 8 + sy);
							index -= 128;
							double zStep = 0.25;
							double value = z0;
							double dValue = (z1 - z0) * zStep;
							value -= dValue;
							for (int sz = 0; sz < 4; sz++) {
								index += 128;
								value += dValue;
								if (value > 0.0) {
									blocks[index] = STONE;
								} else if (cy * 8 + sy < SEA_LEVEL) {
									blocks[index] = WATER;
								} else {
									blocks[index] = AIR;
								}
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

	/** The coarse density field, with each column's height blended from the 5x5 biomes around it. */
	private double[] sampleDensity(int x, int y, int z, int sizeX, int sizeY, int sizeZ, int[] biomes) {
		double xzScale = 684.412;
		double yScale = 684.412;
		double[] depth = depthNoise.sample2D(x, z, sizeX, sizeZ, 200.0, 200.0);
		double[] main = mainNoise.sample(x, y, z, sizeX, sizeY, sizeZ, xzScale / 80.0, yScale / 160.0, xzScale / 80.0);
		double[] min = minLimitNoise.sample(x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] max = maxLimitNoise.sample(x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] out = new double[sizeX * sizeY * sizeZ];

		int index = 0;
		int column = 0;
		for (int ix = 0; ix < sizeX; ix++) {
			for (int iz = 0; iz < sizeZ; iz++) {
				float variationSum = 0.0F;
				float heightSum = 0.0F;
				float weightSum = 0.0F;
				ReleaseBiome centre = ReleaseBiome.byIdOrOcean(biomes[ix + 2 + (iz + 2) * (sizeX + 5)]);
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						ReleaseBiome biome = ReleaseBiome.byIdOrOcean(biomes[ix + dx + 2 + (iz + dz + 2) * (sizeX + 5)]);
						float weight = BIOME_WEIGHTS[dx + 2 + (dz + 2) * 5] / (biome.baseHeight + 2.0F);
						if (biome.baseHeight > centre.baseHeight) {
							weight /= 2.0F;
						}
						variationSum += biome.heightVariation * weight;
						heightSum += biome.baseHeight * weight;
						weightSum += weight;
					}
				}
				variationSum /= weightSum;
				heightSum /= weightSum;
				float variation = variationSum * 0.9F + 0.1F;
				float height = (heightSum * 4.0F - 1.0F) / 8.0F;

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
				} else {
					if (depthValue > 1.0) {
						depthValue = 1.0;
					}
					depthValue /= 8.0;
				}
				column++;

				for (int iy = 0; iy < sizeY; iy++) {
					double columnHeight = height;
					double columnVariation = variation;
					columnHeight += depthValue * 0.2;
					columnHeight = columnHeight * sizeY / 16.0;
					double centreY = sizeY / 2.0 + columnHeight * 4.0;
					double falloff = (iy - centreY) * 12.0 * 128.0 / 128.0 / columnVariation;
					if (falloff < 0.0) {
						falloff *= 4.0;
					}
					double low = min[index] / 512.0;
					double high = max[index] / 512.0;
					double blend = (main[index] / 10.0 + 1.0) / 2.0;
					double value;
					if (blend < 0.0) {
						value = low;
					} else if (blend > 1.0) {
						value = high;
					} else {
						value = low + (high - low) * blend;
					}
					value -= falloff;
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

	/** 1.6.4's replaceBlocksForBiome: bedrock, biome top and filler, ice on cold water, and sandstone under sand. */
	private void replaceBlocksForBiome(int chunkX, int chunkZ, byte[] blocks, int[] biomes, Random random) {
		double scale = 0.03125;
		double[] stone = surfaceNoise.sample(chunkX * 16, chunkZ * 16, 0, 16, 16, 1, scale * 2.0, scale * 2.0, scale * 2.0);

		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				ReleaseBiome biome = ReleaseBiome.byIdOrOcean(biomes[x + z * 16]);
				float temperature = biome.temperature;
				int depth = (int) (stone[z + x * 16] / 3.0 + 3.0 + random.nextDouble() * 0.25);
				int remaining = -1;
				byte top = biome.surface.top;
				byte filler = biome.surface.filler;

				for (int y = HEIGHT - 1; y >= 0; y--) {
					int index = (x * 16 + z) * HEIGHT + y;
					if (y <= random.nextInt(5)) {
						blocks[index] = BEDROCK;
						continue;
					}
					byte block = blocks[index];
					if (block == AIR) {
						remaining = -1;
					} else if (block == STONE) {
						if (remaining == -1) {
							if (depth <= 0) {
								top = AIR;
								filler = STONE;
							} else if (y >= SEA_LEVEL - 4 && y <= SEA_LEVEL + 1) {
								top = biome.surface.top;
								filler = biome.surface.filler;
							}
							if (y < SEA_LEVEL && top == AIR) {
								top = temperature < 0.15F ? ICE : WATER;
							}
							remaining = depth;
							blocks[index] = y >= SEA_LEVEL - 1 ? top : filler;
						} else if (remaining > 0) {
							remaining--;
							blocks[index] = filler;
							if (remaining == 0 && filler == SAND) {
								remaining = random.nextInt(4);
								filler = SANDSTONE;
							}
						}
					}
				}
			}
		}
	}

	private static float sin(float value) {
		return SIN_TABLE[(int) (value * 10430.378F) & 65535];
	}

	private static float cos(float value) {
		return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	/** MapGenBase with MapGenCaves or MapGenRavine. */
	private static final class Carver {
		private static final int RANGE = 8;

		private final long worldSeed;
		private final boolean ravine;
		private final Random random = new Random();
		private final float[] ravineWidths = new float[1024];

		Carver(long worldSeed, boolean ravine) {
			this.worldSeed = worldSeed;
			this.ravine = ravine;
		}

		void generate(int chunkX, int chunkZ, byte[] blocks, int[] biomes) {
			random.setSeed(worldSeed);
			long xMultiplier = random.nextLong();
			long zMultiplier = random.nextLong();
			for (int sourceX = chunkX - RANGE; sourceX <= chunkX + RANGE; sourceX++) {
				for (int sourceZ = chunkZ - RANGE; sourceZ <= chunkZ + RANGE; sourceZ++) {
					random.setSeed(sourceX * xMultiplier ^ sourceZ * zMultiplier ^ worldSeed);
					if (ravine) {
						startRavine(sourceX, sourceZ, chunkX, chunkZ, blocks, biomes);
					} else {
						startCaves(sourceX, sourceZ, chunkX, chunkZ, blocks, biomes);
					}
				}
			}
		}

		private void startCaves(int sourceX, int sourceZ, int chunkX, int chunkZ, byte[] blocks, int[] biomes) {
			int count = random.nextInt(random.nextInt(random.nextInt(40) + 1) + 1);
			if (random.nextInt(15) != 0) {
				count = 0;
			}
			for (int i = 0; i < count; i++) {
				double x = sourceX * 16 + random.nextInt(16);
				double y = random.nextInt(random.nextInt(120) + 8);
				double z = sourceZ * 16 + random.nextInt(16);
				int tunnels = 1;
				if (random.nextInt(4) == 0) {
					tunnel(random.nextLong(), chunkX, chunkZ, blocks, biomes, x, y, z, 1.0F + random.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5);
					tunnels += random.nextInt(4);
				}
				for (int j = 0; j < tunnels; j++) {
					float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
					float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
					float width = random.nextFloat() * 2.0F + random.nextFloat();
					if (random.nextInt(10) == 0) {
						width *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
					}
					tunnel(random.nextLong(), chunkX, chunkZ, blocks, biomes, x, y, z, width, yaw, pitch, 0, 0, 1.0);
				}
			}
		}

		private void startRavine(int sourceX, int sourceZ, int chunkX, int chunkZ, byte[] blocks, int[] biomes) {
			if (random.nextInt(50) != 0) {
				return;
			}
			double x = sourceX * 16 + random.nextInt(16);
			double y = random.nextInt(random.nextInt(40) + 8) + 20;
			double z = sourceZ * 16 + random.nextInt(16);
			float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
			float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
			float width = (random.nextFloat() * 2.0F + random.nextFloat()) * 2.0F;
			tunnel(random.nextLong(), chunkX, chunkZ, blocks, biomes, x, y, z, width, yaw, pitch, 0, 0, 3.0);
		}

		private void tunnel(long tunnelSeed, int chunkX, int chunkZ, byte[] blocks, int[] biomes, double x, double y, double z,
				float width, float yaw, float pitch, int step, int length, double verticalScale) {
			double centreX = chunkX * 16 + 8;
			double centreZ = chunkZ * 16 + 8;
			float yawChange = 0.0F;
			float pitchChange = 0.0F;
			Random rand = new Random(tunnelSeed);
			if (length <= 0) {
				int max = RANGE * 16 - 16;
				length = max - rand.nextInt(max / 4);
			}
			boolean room = false;
			if (step == -1) {
				step = length / 2;
				room = true;
			}

			int branchStep = 0;
			boolean steep = false;
			if (ravine) {
				float widthFactor = 1.0F;
				for (int i = 0; i < 128; i++) {
					if (i == 0 || rand.nextInt(3) == 0) {
						widthFactor = 1.0F + rand.nextFloat() * rand.nextFloat() * 1.0F;
					}
					ravineWidths[i] = widthFactor * widthFactor;
				}
			} else {
				branchStep = rand.nextInt(length / 2) + length / 4;
				steep = rand.nextInt(6) == 0;
			}

			for (; step < length; step++) {
				double radius = 1.5 + sin(step * (float) Math.PI / length) * width * 1.0F;
				double radiusY = radius * verticalScale;
				if (ravine) {
					radius *= rand.nextFloat() * 0.25 + 0.75;
					radiusY *= rand.nextFloat() * 0.25 + 0.75;
				}
				float cosPitch = cos(pitch);
				float sinPitch = sin(pitch);
				x += cos(yaw) * cosPitch;
				y += sinPitch;
				z += sin(yaw) * cosPitch;
				if (ravine) {
					pitch *= 0.7F;
					pitch += pitchChange * 0.05F;
					yaw += yawChange * 0.05F;
					pitchChange *= 0.8F;
					yawChange *= 0.5F;
				} else {
					pitch *= steep ? 0.92F : 0.7F;
					pitch += pitchChange * 0.1F;
					yaw += yawChange * 0.1F;
					pitchChange *= 0.9F;
					yawChange *= 0.75F;
				}
				pitchChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 2.0F;
				yawChange += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 4.0F;

				if (!ravine && !room && step == branchStep && width > 1.0F && length > 0) {
					tunnel(rand.nextLong(), chunkX, chunkZ, blocks, biomes, x, y, z, rand.nextFloat() * 0.5F + 0.5F,
							yaw - (float) (Math.PI / 2), pitch / 3.0F, step, length, 1.0);
					tunnel(rand.nextLong(), chunkX, chunkZ, blocks, biomes, x, y, z, rand.nextFloat() * 0.5F + 0.5F,
							yaw + (float) (Math.PI / 2), pitch / 3.0F, step, length, 1.0);
					return;
				}
				if (!room && rand.nextInt(4) == 0) {
					continue;
				}

				double dx = x - centreX;
				double dz = z - centreZ;
				double stepsLeft = length - step;
				double reach = width + 2.0F + 16.0F;
				if (dx * dx + dz * dz - stepsLeft * stepsLeft > reach * reach) {
					return;
				}
				if (x < centreX - 16.0 - radius * 2.0 || z < centreZ - 16.0 - radius * 2.0
						|| x > centreX + 16.0 + radius * 2.0 || z > centreZ + 16.0 + radius * 2.0) {
					continue;
				}

				int minX = Math.max(floor(x - radius) - chunkX * 16 - 1, 0);
				int maxX = Math.min(floor(x + radius) - chunkX * 16 + 1, 16);
				int minY = Math.max(floor(y - radiusY) - 1, 1);
				int maxY = Math.min(floor(y + radiusY) + 1, 120);
				int minZ = Math.max(floor(z - radius) - chunkZ * 16 - 1, 0);
				int maxZ = Math.min(floor(z + radius) - chunkZ * 16 + 1, 16);

				boolean water = false;
				for (int bx = minX; !water && bx < maxX; bx++) {
					for (int bz = minZ; !water && bz < maxZ; bz++) {
						for (int by = maxY + 1; !water && by >= minY - 1; by--) {
							int index = (bx * 16 + bz) * HEIGHT + by;
							if (by >= 0 && by < HEIGHT) {
								if (blocks[index] == WATER) {
									water = true;
								}
								if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) {
									by = minY;
								}
							}
						}
					}
				}
				if (water) {
					continue;
				}

				for (int bx = minX; bx < maxX; bx++) {
					double nx = (bx + chunkX * 16 + 0.5 - x) / radius;
					for (int bz = minZ; bz < maxZ; bz++) {
						double nz = (bz + chunkZ * 16 + 0.5 - z) / radius;
						int index = (bx * 16 + bz) * HEIGHT + maxY;
						boolean grass = false;
						if (nx * nx + nz * nz >= 1.0) {
							continue;
						}
						for (int by = maxY - 1; by >= minY; by--) {
							double ny = (by + 0.5 - y) / radiusY;
							boolean inside = ravine
									? (nx * nx + nz * nz) * ravineWidths[by] + ny * ny / 6.0 < 1.0
									: ny > -0.7 && nx * nx + ny * ny + nz * nz < 1.0;
							if (inside) {
								byte block = blocks[index];
								if (block == GRASS) {
									grass = true;
								}
								if (block == STONE || block == DIRT || block == GRASS) {
									if (by < 10) {
										blocks[index] = LAVA;
									} else {
										blocks[index] = AIR;
										if (grass && blocks[index - 1] == DIRT) {
											blocks[index - 1] = ReleaseBiome.byIdOrOcean(biomes[bx + bz * 16]).surface.top;
										}
									}
								}
							}
							index--;
						}
					}
				}
				if (room) {
					break;
				}
			}
		}
	}
}
