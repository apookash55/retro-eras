package dev.retroeras.worldgen.infdev;

import java.util.Random;

/**
 * Infdev 20100618's chunk provider (terrain, surface and caves), ported faithfully so a seed gives the same landscape
 * it did in Infdev. Chunks come out as block ids in the old {@code x << 11 | z << 7 | y} layout.
 * <p>
 * It is the ancestor of Beta's generator but has no climate: hill size comes straight from noise, the noise and depth
 * constants differ, there is no fade towards the top of the world, no sandstone or ice, and caves are rarer and seed
 * their tunnels from the generator's shared random. The original was single-threaded and reused one Random across
 * terrain and caves; each call here gets its own so chunks can be generated in parallel with identical results.
 */
public final class InfdevTerrain {
	public static final int HEIGHT = 128;
	public static final int SEA_LEVEL = 64;

	static final byte AIR = 0;
	static final byte STONE = 1;
	static final byte GRASS = 2;
	static final byte DIRT = 3;
	static final byte BEDROCK = 7;
	static final byte WATER = 9;
	static final byte LAVA = 10;
	static final byte SAND = 12;
	static final byte GRAVEL = 13;

	private static final float[] SIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}

	private final long seed;
	private final InfdevNoise.Octaves minLimitNoise;
	private final InfdevNoise.Octaves maxLimitNoise;
	private final InfdevNoise.Octaves mainNoise;
	private final InfdevNoise.Octaves beachNoise;
	private final InfdevNoise.Octaves surfaceNoise;
	private final InfdevNoise.Octaves scaleNoise;
	private final InfdevNoise.Octaves depthNoise;
	private final InfdevNoise.Octaves forestNoise;

	public InfdevTerrain(long seed) {
		this.seed = seed;
		Random random = new Random(seed);
		minLimitNoise = new InfdevNoise.Octaves(random, 16);
		maxLimitNoise = new InfdevNoise.Octaves(random, 16);
		mainNoise = new InfdevNoise.Octaves(random, 8);
		beachNoise = new InfdevNoise.Octaves(random, 4);
		surfaceNoise = new InfdevNoise.Octaves(random, 4);
		scaleNoise = new InfdevNoise.Octaves(random, 10);
		depthNoise = new InfdevNoise.Octaves(random, 16);
		forestNoise = new InfdevNoise.Octaves(random, 8);
	}

	public long seed() {
		return seed;
	}

	/** Infdev's per-chunk tree count before its random part, used to drive tree density (dense forests where high). */
	public double forestNoise(int chunkX, int chunkZ) {
		return forestNoise.sample((chunkX << 4) * 0.5, (chunkZ << 4) * 0.5) / 8.0;
	}

	public record Chunk(byte[] blocks) {
		public byte get(int x, int y, int z) {
			return blocks[x << 11 | z << 7 | y];
		}
	}

	public Chunk generate(int chunkX, int chunkZ, boolean withCaves) {
		Random random = new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
		byte[] blocks = new byte[16 * 16 * HEIGHT];
		generateTerrain(chunkX, chunkZ, blocks);
		replaceSurface(chunkX, chunkZ, blocks, random);
		if (withCaves) {
			carveCaves(chunkX, chunkZ, blocks, random);
		}
		return new Chunk(blocks);
	}

	private void generateTerrain(int chunkX, int chunkZ, byte[] blocks) {
		int x = chunkX << 2;
		int z = chunkZ << 2;
		double[] scale = scaleNoise.sample(x, 0, z, 5, 1, 5, 1.0, 0.0, 1.0);
		double[] depth = depthNoise.sample(x, 0, z, 5, 1, 5, 100.0, 0.0, 100.0);
		double[] main = mainNoise.sample(x, 0, z, 5, 17, 5, 8.555150000000001, 4.277575000000001, 8.555150000000001);
		double[] min = minLimitNoise.sample(x, 0, z, 5, 17, 5, 684.412, 684.412, 684.412);
		double[] max = maxLimitNoise.sample(x, 0, z, 5, 17, 5, 684.412, 684.412, 684.412);
		double[] density = new double[425];

		int index = 0;
		int column = 0;
		for (int ix = 0; ix < 5; ix++) {
			for (int iz = 0; iz < 5; iz++) {
				double scaleValue = (scale[column] + 256.0) / 512.0;
				if (scaleValue > 1.0) {
					scaleValue = 1.0;
				}
				double depthValue = depth[column] / 8000.0;
				if (depthValue < 0.0) {
					depthValue = -depthValue;
				}
				depthValue = depthValue * 3.0 - 3.0;
				if (depthValue < 0.0) {
					depthValue /= 2.0;
					if (depthValue < -1.0) {
						depthValue = -1.0;
					}
					depthValue = depthValue / 1.4 / 2.0;
					scaleValue = 0.0;
				} else {
					if (depthValue > 1.0) {
						depthValue = 1.0;
					}
					depthValue /= 6.0;
				}
				scaleValue += 0.5;
				depthValue = depthValue * 17.0 / 16.0;
				double centre = 8.5 + depthValue * 4.0;
				column++;

				for (int iy = 0; iy < 17; iy++) {
					double falloff = (iy - centre) * 12.0 / scaleValue;
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
					density[index] = value - falloff;
					index++;
				}
			}
		}

		for (int cx = 0; cx < 4; cx++) {
			for (int cz = 0; cz < 4; cz++) {
				for (int cy = 0; cy < 16; cy++) {
					double d000 = density[(cx * 5 + cz) * 17 + cy];
					double d010 = density[(cx * 5 + cz + 1) * 17 + cy];
					double d100 = density[((cx + 1) * 5 + cz) * 17 + cy];
					double d110 = density[((cx + 1) * 5 + cz + 1) * 17 + cy];
					double d001 = density[(cx * 5 + cz) * 17 + cy + 1];
					double d011 = density[(cx * 5 + cz + 1) * 17 + cy + 1];
					double d101 = density[((cx + 1) * 5 + cz) * 17 + cy + 1];
					double d111 = density[((cx + 1) * 5 + cz + 1) * 17 + cy + 1];

					for (int sy = 0; sy < 8; sy++) {
						double ty = sy / 8.0;
						double y00 = d000 + (d001 - d000) * ty;
						double y01 = d010 + (d011 - d010) * ty;
						double y10 = d100 + (d101 - d100) * ty;
						double y11 = d110 + (d111 - d110) * ty;
						for (int sx = 0; sx < 4; sx++) {
							double tx = sx / 4.0;
							double z0 = y00 + (y10 - y00) * tx;
							double z1 = y01 + (y11 - y01) * tx;
							int blockIndex = sx + (cx << 2) << 11 | (cz << 2) << 7 | (cy << 3) + sy;
							for (int sz = 0; sz < 4; sz++) {
								double tz = sz / 4.0;
								double value = z0 + (z1 - z0) * tz;
								byte block = AIR;
								if ((cy << 3) + sy < SEA_LEVEL) {
									block = WATER;
								}
								if (value > 0.0) {
									block = STONE;
								}
								blocks[blockIndex] = block;
								blockIndex += 128;
							}
						}
					}
				}
			}
		}
	}

	private void replaceSurface(int chunkX, int chunkZ, byte[] blocks, Random random) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				double worldX = (chunkX << 4) + x;
				double worldZ = (chunkZ << 4) + z;
				boolean sandBeach = beachNoise.sample(worldX * 0.03125, worldZ * 0.03125, 0.0) + random.nextDouble() * 0.2 > 0.0;
				boolean gravelBeach = beachNoise.sample(worldZ * 0.03125, 109.0134, worldX * 0.03125) + random.nextDouble() * 0.2 > 3.0;
				int depth = (int) (surfaceNoise.sample(worldX * 0.03125 * 2.0, worldZ * 0.03125 * 2.0) / 3.0 + 3.0 + random.nextDouble() * 0.25);
				int index = x << 11 | z << 7 | 127;
				int remaining = -1;
				byte top = GRASS;
				byte filler = DIRT;

				for (int y = 127; y >= 0; y--) {
					if (y <= random.nextInt(6) - 1) {
						blocks[index] = BEDROCK;
					} else if (blocks[index] == AIR) {
						remaining = -1;
					} else if (blocks[index] == STONE) {
						if (remaining == -1) {
							if (depth <= 0) {
								top = AIR;
								filler = STONE;
							} else if (y >= 60 && y <= 65) {
								top = GRASS;
								filler = DIRT;
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
							blocks[index] = y >= 63 ? top : filler;
						} else if (remaining > 0) {
							remaining--;
							blocks[index] = filler;
						}
					}
					index--;
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

	/** Infdev's inline cave pass. {@code random} is the generator's shared Random, which tunnels also draw seeds from. */
	private void carveCaves(int chunkX, int chunkZ, byte[] blocks, Random random) {
		random.setSeed(seed);
		long xMultiplier = (random.nextLong() / 2L << 1) + 1L;
		long zMultiplier = (random.nextLong() / 2L << 1) + 1L;
		for (int sourceX = chunkX - 8; sourceX <= chunkX + 8; sourceX++) {
			for (int sourceZ = chunkZ - 8; sourceZ <= chunkZ + 8; sourceZ++) {
				random.setSeed(sourceX * xMultiplier + sourceZ * zMultiplier ^ seed);
				int count = random.nextInt(random.nextInt(random.nextInt(40) + 1) + 1);
				if (random.nextInt(10) != 0) {
					count = 0;
				}
				for (int i = 0; i < count; i++) {
					double x = (sourceX << 4) + random.nextInt(16);
					double y = random.nextInt(random.nextInt(120) + 8);
					double z = (sourceZ << 4) + random.nextInt(16);
					int tunnels = 1;
					if (random.nextInt(4) == 0) {
						carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, 1.0F + random.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5);
						tunnels = 1 + random.nextInt(4);
					}
					for (int j = 0; j < tunnels; j++) {
						float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
						float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
						float width = random.nextFloat() * 2.0F + random.nextFloat();
						carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 1.0);
					}
				}
			}
		}
	}

	/**
	 * One tunnel. Infdev compiled the second branch of a fork as a loop restart rather than a call, which re-creates the
	 * tunnel's Random from the shared one; the loop below reproduces that.
	 */
	private void carveTunnel(Random shared, int chunkX, int chunkZ, byte[] blocks, double x, double y, double z,
			float width, float yaw, float pitch, int step, int length, double verticalScale) {
		restart:
		while (true) {
			double centreX = (chunkX << 4) + 8;
			double centreZ = (chunkZ << 4) + 8;
			float yawChange = 0.0F;
			float pitchChange = 0.0F;
			Random random = new Random(shared.nextLong());
			if (length <= 0) {
				length = 112 - random.nextInt(28);
			}
			boolean room = false;
			if (step == -1) {
				step = length / 2;
				room = true;
			}
			int branchStep = random.nextInt(length / 2) + length / 4;
			boolean steep = random.nextInt(6) == 0;

			for (; step < length; step++) {
				double radius = 1.5 + sin(step * (float) Math.PI / length) * width;
				double radiusY = radius * verticalScale;
				float cosPitch = cos(pitch);
				float sinPitch = sin(pitch);
				x += cos(yaw) * cosPitch;
				y += sinPitch;
				z += sin(yaw) * cosPitch;
				pitch *= steep ? 0.92F : 0.7F;
				pitch += pitchChange * 0.1F;
				yaw += yawChange * 0.1F;
				pitchChange *= 0.9F;
				yawChange *= 0.75F;
				pitchChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
				yawChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

				if (!room && step == branchStep && width > 1.0F) {
					carveTunnel(shared, chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - (float) (Math.PI / 2), pitch / 3.0F, step, length, 1.0);
					width = random.nextFloat() * 0.5F + 0.5F;
					yaw = yaw + (float) (Math.PI / 2);
					pitch = pitch / 3.0F;
					verticalScale = 1.0;
					continue restart;
				}
				if (!room && random.nextInt(4) == 0) {
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

				int minX = Math.max(floor(x - radius) - (chunkX << 4) - 1, 0);
				int maxX = Math.min(floor(x + radius) - (chunkX << 4) + 1, 16);
				int minY = floor(y - radiusY) - 1;
				int maxY = Math.min(floor(y + radiusY) + 1, 120);
				int minZ = Math.max(floor(z - radius) - (chunkZ << 4) - 1, 0);
				int maxZ = Math.min(floor(z + radius) - (chunkZ << 4) + 1, 16);
				if (minY <= 0) {
					minY = 1;
				}

				boolean water = false;
				for (int bx = minX; !water && bx < maxX; bx++) {
					for (int bz = minZ; !water && bz < maxZ; bz++) {
						for (int by = maxY + 1; !water && by >= minY - 1; by--) {
							int index = ((bx << 4) + bz << 7) + by;
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
					double nx = (bx + (chunkX << 4) + 0.5 - x) / radius;
					for (int bz = minZ; bz < maxZ; bz++) {
						double nz = (bz + (chunkZ << 4) + 0.5 - z) / radius;
						int index = ((bx << 4) + bz << 7) + maxY;
						boolean grass = false;
						for (int by = maxY - 1; by >= minY; by--) {
							double ny = (by + 0.5 - y) / radiusY;
							if (ny > -0.7 && nx * nx + ny * ny + nz * nz < 1.0) {
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
											blocks[index - 1] = GRASS;
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
			return;
		}
	}
}
