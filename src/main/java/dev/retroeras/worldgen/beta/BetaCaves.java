package dev.retroeras.worldgen.beta;

import java.util.Random;

/**
 * Beta 1.7.3's MapGenCaves (with MapGenBase's chunk seeding), ported faithfully. Caves never cut through sand or
 * gravel, stop at water, and fill with lava below y = 10.
 */
final class BetaCaves {
	private static final int RANGE = 8;
	// Beta wrote these as float literals, slightly off from (float) Math.PI; the exact values shape the tunnels.
	private static final float PI = 3.141593F;
	private static final float HALF_PI = 1.570796F;
	private static final float[] SIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}

	private final long seed;

	BetaCaves(long seed) {
		this.seed = seed;
	}

	/** Beta's MathHelper.sin/cos, which use a lookup table; cave shapes depend on its exact values. */
	private static float sin(float value) {
		return SIN_TABLE[(int) (value * 10430.38F) & 65535];
	}

	private static float cos(float value) {
		return SIN_TABLE[(int) (value * 10430.38F + 16384.0F) & 65535];
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	void carve(int chunkX, int chunkZ, byte[] blocks) {
		Random random = new Random(seed);
		long xMultiplier = random.nextLong() / 2L * 2L + 1L;
		long zMultiplier = random.nextLong() / 2L * 2L + 1L;
		for (int sourceX = chunkX - RANGE; sourceX <= chunkX + RANGE; sourceX++) {
			for (int sourceZ = chunkZ - RANGE; sourceZ <= chunkZ + RANGE; sourceZ++) {
				random.setSeed(sourceX * xMultiplier + sourceZ * zMultiplier ^ seed);
				carveFrom(random, sourceX, sourceZ, chunkX, chunkZ, blocks);
			}
		}
	}

	private void carveFrom(Random random, int sourceX, int sourceZ, int chunkX, int chunkZ, byte[] blocks) {
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
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, 1.0F + random.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5);
				tunnels += random.nextInt(4);
			}
			for (int j = 0; j < tunnels; j++) {
				float yaw = random.nextFloat() * PI * 2.0F;
				float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
				float width = random.nextFloat() * 2.0F + random.nextFloat();
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 1.0);
			}
		}
	}

	private void carveTunnel(Random parentRandom, int chunkX, int chunkZ, byte[] blocks, double x, double y, double z,
			float width, float yaw, float pitch, int step, int length, double verticalScale) {
		double centreX = chunkX * 16 + 8;
		double centreZ = chunkZ * 16 + 8;
		float yawChange = 0.0F;
		float pitchChange = 0.0F;
		Random random = new Random(parentRandom.nextLong());
		if (length <= 0) {
			int max = RANGE * 16 - 16;
			length = max - random.nextInt(max / 4);
		}
		boolean room = false;
		if (step == -1) {
			step = length / 2;
			room = true;
		}
		int branchStep = random.nextInt(length / 2) + length / 4;
		boolean steep = random.nextInt(6) == 0;

		for (; step < length; step++) {
			double radius = 1.5 + sin(step * PI / length) * width * 1.0F;
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
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - HALF_PI, pitch / 3.0F, step, length, 1.0);
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + HALF_PI, pitch / 3.0F, step, length, 1.0);
				return;
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

			int minX = floor(x - radius) - chunkX * 16 - 1;
			int maxX = floor(x + radius) - chunkX * 16 + 1;
			int minY = floor(y - radiusY) - 1;
			int maxY = floor(y + radiusY) + 1;
			int minZ = floor(z - radius) - chunkZ * 16 - 1;
			int maxZ = floor(z + radius) - chunkZ * 16 + 1;
			minX = Math.max(minX, 0);
			maxX = Math.min(maxX, 16);
			minY = Math.max(minY, 1);
			maxY = Math.min(maxY, 120);
			minZ = Math.max(minZ, 0);
			maxZ = Math.min(maxZ, 16);

			// Skip this step entirely if it would break into water.
			boolean water = false;
			for (int bx = minX; !water && bx < maxX; bx++) {
				for (int bz = minZ; !water && bz < maxZ; bz++) {
					for (int by = maxY + 1; !water && by >= minY - 1; by--) {
						if (by < 0 || by >= BetaTerrain.HEIGHT) {
							continue;
						}
						if (blocks[(bx * 16 + bz) * BetaTerrain.HEIGHT + by] == BetaTerrain.WATER) {
							water = true;
						}
						if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) {
							by = minY;
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
					int index = (bx * 16 + bz) * BetaTerrain.HEIGHT + maxY;
					boolean grass = false;
					if (nx * nx + nz * nz >= 1.0) {
						continue;
					}
					for (int by = maxY - 1; by >= minY; by--) {
						double ny = (by + 0.5 - y) / radiusY;
						if (ny > -0.7 && nx * nx + ny * ny + nz * nz < 1.0) {
							byte block = blocks[index];
							if (block == BetaTerrain.GRASS) {
								grass = true;
							}
							if (block == BetaTerrain.STONE || block == BetaTerrain.DIRT || block == BetaTerrain.GRASS) {
								if (by < 10) {
									blocks[index] = BetaTerrain.LAVA;
								} else {
									blocks[index] = BetaTerrain.AIR;
									if (grass && blocks[index - 1] == BetaTerrain.DIRT) {
										blocks[index - 1] = BetaTerrain.GRASS;
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
