package dev.retroeras.worldgen.end;

import java.util.Random;

import dev.retroeras.worldgen.LegacyOctaves;
import dev.retroeras.worldgen.release.ReleaseNoise;

/**
 * 1.6.4's End (ChunkProviderEnd), ported: one island of end stone floating in the void around the origin, shaped by the
 * same three noises as the other terrain and a pull that falls away with distance from the centre. There are no outer
 * islands, which came in 1.9. The generator's surface pass looks for stone and finds none, so it does nothing.
 */
public final class EndTerrain {
	public static final int HEIGHT = 128;

	private final long seed;
	private final LegacyOctaves minLimit;
	private final LegacyOctaves maxLimit;
	private final LegacyOctaves main;

	public EndTerrain(long seed) {
		this.seed = seed;
		Random random = new Random(seed);
		minLimit = ReleaseNoise.legacy(random, 16);
		maxLimit = ReleaseNoise.legacy(random, 16);
		main = ReleaseNoise.legacy(random, 8);
		// A 10 and a 16 octave noise came next and were sampled too, but nothing used their values.
	}

	public long seed() {
		return seed;
	}

	/** One generated chunk, in the old x-z-y layout: true where there is end stone. */
	public record Chunk(boolean[] solid) {
		public boolean get(int x, int y, int z) {
			return solid[(x * 16 + z) * HEIGHT + y];
		}
	}

	public Chunk generate(int chunkX, int chunkZ) {
		boolean[] solid = new boolean[16 * 16 * HEIGHT];
		int cells = 2;
		int sizeX = cells + 1;
		int sizeY = 33;
		int sizeZ = cells + 1;
		double[] density = density(chunkX * cells, 0, chunkZ * cells, sizeX, sizeY, sizeZ);
		for (int cx = 0; cx < cells; cx++) {
			for (int cz = 0; cz < cells; cz++) {
				for (int cy = 0; cy < 32; cy++) {
					double stepY = 0.25;
					double d000 = density[((cx + 0) * sizeZ + cz + 0) * sizeY + cy + 0];
					double d010 = density[((cx + 0) * sizeZ + cz + 1) * sizeY + cy + 0];
					double d100 = density[((cx + 1) * sizeZ + cz + 0) * sizeY + cy + 0];
					double d110 = density[((cx + 1) * sizeZ + cz + 1) * sizeY + cy + 0];
					double dy000 = (density[((cx + 0) * sizeZ + cz + 0) * sizeY + cy + 1] - d000) * stepY;
					double dy010 = (density[((cx + 0) * sizeZ + cz + 1) * sizeY + cy + 1] - d010) * stepY;
					double dy100 = (density[((cx + 1) * sizeZ + cz + 0) * sizeY + cy + 1] - d100) * stepY;
					double dy110 = (density[((cx + 1) * sizeZ + cz + 1) * sizeY + cy + 1] - d110) * stepY;
					for (int sy = 0; sy < 4; sy++) {
						double stepX = 0.125;
						double x0 = d000;
						double x1 = d010;
						double dx0 = (d100 - d000) * stepX;
						double dx1 = (d110 - d010) * stepX;
						for (int sx = 0; sx < 8; sx++) {
							int index = sx + cx * 8 << 11 | 0 + cz * 8 << 7 | cy * 4 + sy;
							double stepZ = 0.125;
							double value = x0;
							double dz = (x1 - x0) * stepZ;
							for (int sz = 0; sz < 8; sz++) {
								solid[index] = value > 0.0;
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
		return new Chunk(solid);
	}

	private double[] density(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		double[] out = new double[sizeX * sizeY * sizeZ];
		double xzScale = 684.412 * 2.0;
		double yScale = 684.412;
		double[] mainValues = main.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale / 80.0, yScale / 160.0, xzScale / 80.0);
		double[] minValues = minLimit.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		double[] maxValues = maxLimit.sample(null, x, y, z, sizeX, sizeY, sizeZ, xzScale, yScale, xzScale);
		int index = 0;
		for (int ix = 0; ix < sizeX; ix++) {
			for (int iz = 0; iz < sizeZ; iz++) {
				// The island: solid near the centre, falling away with distance from it.
				float cellX = ix + x;
				float cellZ = iz + z;
				float island = 100.0F - (float) Math.sqrt(cellX * cellX + cellZ * cellZ) * 8.0F;
				island = Math.clamp(island, -100.0F, 80.0F);
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
					value -= 8.0;
					value += island;
					// Cut off above the middle of the world, and below y = 32, into the void.
					int top = 2;
					if (iy > sizeY / 2 - top) {
						double fade = Math.clamp((iy - (sizeY / 2 - top)) / 64.0F, 0.0, 1.0);
						value = value * (1.0 - fade) + -3000.0 * fade;
					}
					int bottom = 8;
					if (iy < bottom) {
						double fade = (bottom - iy) / (bottom - 1.0F);
						value = value * (1.0 - fade) + -30.0 * fade;
					}
					out[index] = value;
					index++;
				}
			}
		}
		return out;
	}
}
