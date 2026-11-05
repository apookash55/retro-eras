package dev.retroeras.worldgen.nether;

import java.util.Random;

/**
 * MapGenCavesHell, the same in Beta 1.7.3 and 1.6.4: fewer, wider and flatter tunnels than the overworld's, cut through
 * netherrack anywhere from the floor to the roof, and never into lava.
 */
final class NetherCaves {
	private static final int RANGE = 8;
	private static final float[] SIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}

	private final long seed;

	NetherCaves(long seed) {
		this.seed = seed;
	}

	/** MathHelper.sin and cos, from the same lookup table the old game used. */
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
		int count = random.nextInt(random.nextInt(random.nextInt(10) + 1) + 1);
		if (random.nextInt(5) != 0) {
			count = 0;
		}
		for (int i = 0; i < count; i++) {
			double x = sourceX * 16 + random.nextInt(16);
			double y = random.nextInt(NetherTerrain.HEIGHT);
			double z = sourceZ * 16 + random.nextInt(16);
			int tunnels = 1;
			if (random.nextInt(4) == 0) {
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, 1.0F + random.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5);
				tunnels += random.nextInt(4);
			}
			for (int j = 0; j < tunnels; j++) {
				float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
				float pitch = (random.nextFloat() - 0.5F) * 2.0F / 8.0F;
				float width = random.nextFloat() * 2.0F + random.nextFloat();
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, width * 2.0F, yaw, pitch, 0, 0, 0.5);
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
			double radius = 1.5 + sin(step * (float) Math.PI / length) * width * 1.0F;
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
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - (float) (Math.PI / 2), pitch / 3.0F, step, length, 1.0);
				carveTunnel(random, chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + (float) (Math.PI / 2), pitch / 3.0F, step, length, 1.0);
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

			int minX = Math.max(floor(x - radius) - chunkX * 16 - 1, 0);
			int maxX = Math.min(floor(x + radius) - chunkX * 16 + 1, 16);
			int minY = Math.max(floor(y - radiusY) - 1, 1);
			int maxY = Math.min(floor(y + radiusY) + 1, 120);
			int minZ = Math.max(floor(z - radius) - chunkZ * 16 - 1, 0);
			int maxZ = Math.min(floor(z + radius) - chunkZ * 16 + 1, 16);

			// Skip this step entirely if it would break into lava.
			boolean lava = false;
			for (int bx = minX; !lava && bx < maxX; bx++) {
				for (int bz = minZ; !lava && bz < maxZ; bz++) {
					for (int by = maxY + 1; !lava && by >= minY - 1; by--) {
						if (by < 0 || by >= NetherTerrain.HEIGHT) {
							continue;
						}
						byte block = blocks[(bx * 16 + bz) * NetherTerrain.HEIGHT + by];
						if (block == NetherTerrain.FLOWING_LAVA || block == NetherTerrain.LAVA) {
							lava = true;
						}
						if (by != minY - 1 && bx != minX && bx != maxX - 1 && bz != minZ && bz != maxZ - 1) {
							by = minY;
						}
					}
				}
			}
			if (lava) {
				continue;
			}

			for (int bx = minX; bx < maxX; bx++) {
				double nx = (bx + chunkX * 16 + 0.5 - x) / radius;
				for (int bz = minZ; bz < maxZ; bz++) {
					double nz = (bz + chunkZ * 16 + 0.5 - z) / radius;
					int index = (bx * 16 + bz) * NetherTerrain.HEIGHT + maxY;
					for (int by = maxY - 1; by >= minY; by--) {
						double ny = (by + 0.5 - y) / radiusY;
						if (ny > -0.7 && nx * nx + ny * ny + nz * nz < 1.0) {
							byte block = blocks[index];
							if (block == NetherTerrain.NETHERRACK || block == NetherTerrain.DIRT || block == NetherTerrain.GRASS) {
								blocks[index] = NetherTerrain.AIR;
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
