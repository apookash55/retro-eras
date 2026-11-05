package dev.retroeras.worldgen.classic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Classic 0.30's LevelGenerator, ported faithfully: a finite 256x256 world, 64 high, built in the steps its loading
 * screen named. Raising (two distorted noise height maps), Eroding, Soiling (dirt over rock, with lava on the bottom
 * layer), Carving (worm caves and coal, iron and gold veins), Watering (a sea flooded in from the edges at y = 31),
 * Melting (lava pools), Growing (sand and gravel beaches, grass), Planting (flowers, mushrooms and trees).
 * Classic seeded every new level from the clock; here the seed is the world's.
 */
public final class ClassicTerrain {
	public static final int SIZE = 256;
	public static final int HEIGHT = 64;
	public static final int WATER_LEVEL = 32;

	static final byte AIR = 0;
	static final byte STONE = 1;
	static final byte GRASS = 2;
	static final byte DIRT = 3;
	static final byte WATER_FLOWING = 8;
	static final byte WATER = 9;
	static final byte LAVA_FLOWING = 10;
	static final byte LAVA = 11;
	static final byte SAND = 12;
	static final byte GRAVEL = 13;
	static final byte GOLD_ORE = 14;
	static final byte IRON_ORE = 15;
	static final byte COAL_ORE = 16;
	static final byte LOG = 17;
	static final byte LEAVES = 18;
	static final byte DANDELION = 37;
	static final byte ROSE = 38;
	static final byte BROWN_MUSHROOM = 39;
	static final byte RED_MUSHROOM = 40;

	private static final float[] SIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}

	private final long seed;
	private final int width = SIZE;
	private final int depth = SIZE;
	private final int height = HEIGHT;
	private final int waterLevel = WATER_LEVEL;
	private final Random random;
	private final byte[] blocks;
	private int[] floodStack = new int[1048576];

	public ClassicTerrain(long seed) {
		this.seed = seed;
		this.random = new Random(seed);
		this.blocks = new byte[width * depth * height];
		generate();
	}

	public long seed() {
		return seed;
	}

	/** Whether a block column is inside the finite world. */
	public static boolean inWorld(int x, int z) {
		return x >= 0 && z >= 0 && x < SIZE && z < SIZE;
	}

	/** The block at a world position; air outside the level. */
	public byte get(int x, int y, int z) {
		if (!inWorld(x, z) || y < 0 || y >= height) {
			return AIR;
		}
		return blocks[(y * depth + z) * width + x];
	}

	private static float sin(float value) {
		return SIN_TABLE[(int) (value * 10430.378F) & 65535];
	}

	private static float cos(float value) {
		return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
	}

	private int index(int x, int y, int z) {
		return (y * depth + z) * width + x;
	}

	private void generate() {
		int[] heights = raise();
		erode(heights);
		soil(heights);
		carve();
		ore(COAL_ORE, 90, 1, 4);
		ore(IRON_ORE, 70, 2, 4);
		ore(GOLD_ORE, 50, 3, 4);
		water();
		melt();
		grow(heights);
		plant(heights);
		mushrooms(heights);
		trees(heights);
	}

	/** Raising: the higher of two distorted noise maps, or the lower one where a third noise says so. */
	private int[] raise() {
		ClassicNoise.Distorted low = new ClassicNoise.Distorted(new ClassicNoise.Octaves(random, 8), new ClassicNoise.Octaves(random, 8));
		ClassicNoise.Distorted high = new ClassicNoise.Distorted(new ClassicNoise.Octaves(random, 8), new ClassicNoise.Octaves(random, 8));
		ClassicNoise.Octaves select = new ClassicNoise.Octaves(random, 6);
		int[] heights = new int[width * depth];
		float scale = 1.3F;
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				double lowHeight = low.sample(x * scale, z * scale) / 6.0 + -4;
				double highHeight = high.sample(x * scale, z * scale) / 5.0 + 10.0 + -4;
				if (select.sample(x, z) / 8.0 > 0.0) {
					highHeight = lowHeight;
				}
				double value = Math.max(lowHeight, highHeight) / 2.0;
				if (value < 0.0) {
					value *= 0.8;
				}
				heights[x + z * width] = (int) value;
			}
		}
		return heights;
	}

	/** Eroding: flattens odd heights to even ones where a noise is high. */
	private void erode(int[] heights) {
		ClassicNoise.Distorted erosion = new ClassicNoise.Distorted(new ClassicNoise.Octaves(random, 8), new ClassicNoise.Octaves(random, 8));
		ClassicNoise.Distorted parity = new ClassicNoise.Distorted(new ClassicNoise.Octaves(random, 8), new ClassicNoise.Octaves(random, 8));
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				double strength = erosion.sample(x << 1, z << 1) / 8.0;
				int odd = parity.sample(x << 1, z << 1) > 0.0 ? 1 : 0;
				if (strength > 2.0) {
					int value = heights[x + z * width];
					heights[x + z * width] = ((value - odd) / 2 << 1) + odd;
				}
			}
		}
	}

	/** Soiling: rock below, a few blocks of dirt on top, flowing lava on the bottom layer. */
	private void soil(int[] heights) {
		ClassicNoise.Octaves dirtNoise = new ClassicNoise.Octaves(random, 8);
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				int dirtThickness = (int) (dirtNoise.sample(x, z) / 24.0) - 4;
				int dirtTop = heights[x + z * width] + waterLevel;
				int rockTop = dirtTop + dirtThickness;
				heights[x + z * width] = Math.max(dirtTop, rockTop);
				if (heights[x + z * width] > height - 2) {
					heights[x + z * width] = height - 2;
				}
				if (heights[x + z * width] < 1) {
					heights[x + z * width] = 1;
				}
				for (int y = 0; y < height; y++) {
					byte block = AIR;
					if (y <= dirtTop) {
						block = DIRT;
					}
					if (y <= rockTop) {
						block = STONE;
					}
					if (y == 0) {
						block = LAVA_FLOWING;
					}
					blocks[index(x, y, z)] = block;
				}
			}
		}
	}

	/** Carving: worm caves through rock. */
	private void carve() {
		int count = width * depth * height / 256 / 64 << 1;
		for (int i = 0; i < count; i++) {
			float x = random.nextFloat() * width;
			float y = random.nextFloat() * height;
			float z = random.nextFloat() * depth;
			int length = (int) ((random.nextFloat() + random.nextFloat()) * 200.0F);
			float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
			float yawChange = 0.0F;
			float pitch = random.nextFloat() * (float) Math.PI * 2.0F;
			float pitchChange = 0.0F;
			float size = random.nextFloat() * random.nextFloat();
			for (int step = 0; step < length; step++) {
				x += sin(yaw) * cos(pitch);
				z += cos(yaw) * cos(pitch);
				y += sin(pitch);
				yaw += yawChange * 0.2F;
				yawChange = yawChange * 0.9F + (random.nextFloat() - random.nextFloat());
				pitch = (pitch + pitchChange * 0.5F) * 0.5F;
				pitchChange = pitchChange * 0.75F + (random.nextFloat() - random.nextFloat());
				if (random.nextFloat() < 0.25F) {
					continue;
				}
				float cx = x + (random.nextFloat() * 4.0F - 2.0F) * 0.2F;
				float cy = y + (random.nextFloat() * 4.0F - 2.0F) * 0.2F;
				float cz = z + (random.nextFloat() * 4.0F - 2.0F) * 0.2F;
				float radius = (height - cy) / height;
				radius = 1.2F + (radius * 3.5F + 1.0F) * size;
				radius = sin(step * (float) Math.PI / length) * radius;
				for (int bx = (int) (cx - radius); bx <= (int) (cx + radius); bx++) {
					for (int by = (int) (cy - radius); by <= (int) (cy + radius); by++) {
						for (int bz = (int) (cz - radius); bz <= (int) (cz + radius); bz++) {
							float dx = bx - cx;
							float dy = by - cy;
							float dz = bz - cz;
							if (dx * dx + dy * dy * 2.0F + dz * dz < radius * radius && bx >= 1 && by >= 1 && bz >= 1 && bx < width - 1 && by < height - 1
									&& bz < depth - 1) {
								int index = index(bx, by, bz);
								if (blocks[index] == STONE) {
									blocks[index] = AIR;
								}
							}
						}
					}
				}
			}
		}
	}

	/** An ore vein: a worm through rock, thicker for more common ores. */
	private void ore(byte ore, int abundance, int stage, int stages) {
		int count = width * depth * height / 256 / 64 * abundance / 100;
		for (int i = 0; i < count; i++) {
			float x = random.nextFloat() * width;
			float y = random.nextFloat() * height;
			float z = random.nextFloat() * depth;
			int length = (int) ((random.nextFloat() + random.nextFloat()) * 75.0F * abundance / 100.0F);
			float yaw = random.nextFloat() * (float) Math.PI * 2.0F;
			float yawChange = 0.0F;
			float pitch = random.nextFloat() * (float) Math.PI * 2.0F;
			float pitchChange = 0.0F;
			for (int step = 0; step < length; step++) {
				x += sin(yaw) * cos(pitch);
				z += cos(yaw) * cos(pitch);
				y += sin(pitch);
				yaw += yawChange * 0.2F;
				yawChange = yawChange * 0.9F + (random.nextFloat() - random.nextFloat());
				pitch = (pitch + pitchChange * 0.5F) * 0.5F;
				pitchChange = pitchChange * 0.9F + (random.nextFloat() - random.nextFloat());
				float radius = sin(step * (float) Math.PI / length) * abundance / 100.0F + 1.0F;
				for (int bx = (int) (x - radius); bx <= (int) (x + radius); bx++) {
					for (int by = (int) (y - radius); by <= (int) (y + radius); by++) {
						for (int bz = (int) (z - radius); bz <= (int) (z + radius); bz++) {
							float dx = bx - x;
							float dy = by - y;
							float dz = bz - z;
							if (dx * dx + dy * dy * 2.0F + dz * dz < radius * radius && bx >= 1 && by >= 1 && bz >= 1 && bx < width - 1 && by < height - 1
									&& bz < depth - 1) {
								int index = index(bx, by, bz);
								if (blocks[index] == STONE) {
									blocks[index] = ore;
								}
							}
						}
					}
				}
			}
		}
	}

	/** Watering: the sea, flooded in from every edge at y = 31, and a few springs inland. */
	private void water() {
		for (int x = 0; x < width; x++) {
			flood(x, height / 2 - 1, 0, WATER);
			flood(x, height / 2 - 1, depth - 1, WATER);
		}
		for (int z = 0; z < depth; z++) {
			flood(0, height / 2 - 1, z, WATER);
			flood(width - 1, height / 2 - 1, z, WATER);
		}
		int springs = width * depth / 8000;
		for (int i = 0; i < springs; i++) {
			int x = random.nextInt(width);
			int y = waterLevel - 1 - random.nextInt(2);
			int z = random.nextInt(depth);
			if (blocks[index(x, y, z)] == AIR) {
				flood(x, y, z, WATER);
			}
		}
	}

	/** Melting: lava pools deep underground. */
	private void melt() {
		int count = width * depth * height / 20000;
		for (int i = 0; i < count; i++) {
			int x = random.nextInt(width);
			int y = (int) (random.nextFloat() * random.nextFloat() * (waterLevel - 3));
			int z = random.nextInt(depth);
			if (blocks[index(x, y, z)] == AIR) {
				flood(x, y, z, LAVA);
			}
		}
	}

	/** Growing: sand or gravel at the water's edge, grass on top of the rest. */
	private void grow(int[] heights) {
		ClassicNoise.Octaves sandNoise = new ClassicNoise.Octaves(random, 8);
		ClassicNoise.Octaves gravelNoise = new ClassicNoise.Octaves(random, 8);
		for (int x = 0; x < width; x++) {
			for (int z = 0; z < depth; z++) {
				boolean sand = sandNoise.sample(x, z) > 8.0;
				boolean gravel = gravelNoise.sample(x, z) > 12.0;
				int y = heights[x + z * width];
				int index = index(x, y, z);
				int above = blocks[index(x, y + 1, z)] & 255;
				if ((above == WATER_FLOWING || above == WATER) && y <= height / 2 - 1 && gravel) {
					blocks[index] = GRAVEL;
				}
				if (above == AIR) {
					byte top = GRASS;
					if (y <= height / 2 - 1 && sand) {
						top = SAND;
					}
					blocks[index] = top;
				}
			}
		}
	}

	/** Planting: patches of dandelions or roses on grass. */
	private void plant(int[] heights) {
		int patches = width * depth / 3000;
		for (int i = 0; i < patches; i++) {
			int kind = random.nextInt(2);
			int x = random.nextInt(width);
			int z = random.nextInt(depth);
			for (int j = 0; j < 10; j++) {
				int fx = x;
				int fz = z;
				for (int k = 0; k < 5; k++) {
					fx += random.nextInt(6) - random.nextInt(6);
					fz += random.nextInt(6) - random.nextInt(6);
					if ((kind < 2 || random.nextInt(4) == 0) && fx >= 0 && fz >= 0 && fx < width && fz < depth) {
						int y = heights[fx + fz * width] + 1;
						if ((blocks[index(fx, y, fz)] & 255) == AIR && (blocks[index(fx, y - 1, fz)] & 255) == GRASS) {
							blocks[index(fx, y, fz)] = kind == 0 ? DANDELION : ROSE;
						}
					}
				}
			}
		}
	}

	/** Brown and red mushrooms on rock in caves. */
	private void mushrooms(int[] heights) {
		int patches = width * depth * height / 2000;
		for (int i = 0; i < patches; i++) {
			int kind = random.nextInt(2);
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			int z = random.nextInt(depth);
			for (int j = 0; j < 20; j++) {
				int mx = x;
				int my = y;
				int mz = z;
				for (int k = 0; k < 5; k++) {
					mx += random.nextInt(6) - random.nextInt(6);
					my += random.nextInt(2) - random.nextInt(2);
					mz += random.nextInt(6) - random.nextInt(6);
					if ((kind < 2 || random.nextInt(4) == 0) && mx >= 0 && mz >= 0 && my >= 1 && mx < width && mz < depth
							&& my < heights[mx + mz * width] - 1 && (blocks[index(mx, my, mz)] & 255) == AIR
							&& (blocks[index(mx, my - 1, mz)] & 255) == STONE) {
						blocks[index(mx, my, mz)] = kind == 0 ? BROWN_MUSHROOM : RED_MUSHROOM;
					}
				}
			}
		}
	}

	/** Trees, grown by the level after it was made, using the level's own random. */
	private void trees(int[] heights) {
		Random levelRandom = new Random(seed ^ 0x5DEECE66DL);
		int patches = width * depth / 4000;
		for (int i = 0; i < patches; i++) {
			int x = random.nextInt(width);
			int z = random.nextInt(depth);
			for (int j = 0; j < 20; j++) {
				int tx = x;
				int tz = z;
				for (int k = 0; k < 20; k++) {
					tx += random.nextInt(6) - random.nextInt(6);
					tz += random.nextInt(6) - random.nextInt(6);
					if (tx >= 0 && tz >= 0 && tx < width && tz < depth) {
						int y = heights[tx + tz * width] + 1;
						if (random.nextInt(4) == 0) {
							growTree(levelRandom, tx, y, tz);
						}
					}
				}
			}
		}
	}

	/** Level.maybeGrowTree: a 4-6 block oak, if there is room and grass below. */
	private boolean growTree(Random random, int x, int y, int z) {
		int trunk = random.nextInt(3) + 4;
		boolean room = true;
		for (int ty = y; ty <= y + 1 + trunk; ty++) {
			int radius = 1;
			if (ty == y) {
				radius = 0;
			}
			if (ty >= y + 1 + trunk - 2) {
				radius = 2;
			}
			for (int tx = x - radius; tx <= x + radius && room; tx++) {
				for (int tz = z - radius; tz <= z + radius && room; tz++) {
					if (tx >= 0 && ty >= 0 && tz >= 0 && tx < width && ty < height && tz < depth) {
						if ((blocks[index(tx, ty, tz)] & 255) != AIR) {
							room = false;
						}
					} else {
						room = false;
					}
				}
			}
		}
		if (!room) {
			return false;
		}
		if ((blocks[index(x, y - 1, z)] & 255) != GRASS || y >= height - trunk - 1) {
			return false;
		}
		blocks[index(x, y - 1, z)] = DIRT;
		for (int ly = y - 3 + trunk; ly <= y + trunk; ly++) {
			int fromTop = ly - (y + trunk);
			int radius = 1 - fromTop / 2;
			for (int lx = x - radius; lx <= x + radius; lx++) {
				int dx = lx - x;
				for (int lz = z - radius; lz <= z + radius; lz++) {
					int dz = lz - z;
					if (Math.abs(dx) != radius || Math.abs(dz) != radius || random.nextInt(2) != 0 && fromTop != 0) {
						set(lx, ly, lz, LEAVES);
					}
				}
			}
		}
		for (int ty = 0; ty < trunk; ty++) {
			set(x, y + ty, z, LOG);
		}
		return true;
	}

	private void set(int x, int y, int z, byte block) {
		if (x >= 0 && y >= 0 && z >= 0 && x < width && y < height && z < depth) {
			blocks[index(x, y, z)] = block;
		}
	}

	/**
	 * LevelGenerator.floodFill: a scanline fill of the air connected to (x, y, z) sideways and downwards. Lava poured onto
	 * water below turns it to rock.
	 */
	private long flood(int x, int y, int z, byte block) {
		List<int[]> spill = new ArrayList<>();
		int top = 0;
		int xBits = 1;
		int zBits = 1;
		while (1 << xBits < width) {
			xBits++;
		}
		while (1 << zBits < depth) {
			zBits++;
		}
		int zMask = depth - 1;
		int xMask = width - 1;
		top++;
		floodStack[0] = ((y << zBits) + z << xBits) + x;
		long filled = 0L;
		int layer = width * depth;
		while (top > 0) {
			int packed = floodStack[--top];
			if (top == 0 && !spill.isEmpty()) {
				floodStack = spill.remove(spill.size() - 1);
				top = floodStack.length;
			}
			int pz = packed >> xBits & zMask;
			int py = packed >> xBits + zBits;
			int start;
			int end = start = packed & xMask;
			while (start > 0 && blocks[packed - 1] == AIR) {
				start--;
				packed--;
			}
			while (end < width && blocks[packed + end - start] == AIR) {
				end++;
			}
			boolean lastBack = false;
			boolean lastFront = false;
			boolean lastBelow = false;
			filled += end - start;
			for (int i = start; i < end; i++) {
				blocks[packed] = block;
				if (pz > 0) {
					boolean open = blocks[packed - width] == AIR;
					if (open && !lastBack) {
						if (top == floodStack.length) {
							spill.add(floodStack);
							floodStack = new int[1048576];
							top = 0;
						}
						floodStack[top++] = packed - width;
					}
					lastBack = open;
				}
				if (pz < depth - 1) {
					boolean open = blocks[packed + width] == AIR;
					if (open && !lastFront) {
						if (top == floodStack.length) {
							spill.add(floodStack);
							floodStack = new int[1048576];
							top = 0;
						}
						floodStack[top++] = packed + width;
					}
					lastFront = open;
				}
				if (py > 0) {
					byte below = blocks[packed - layer];
					if ((block == LAVA_FLOWING || block == LAVA) && (below == WATER_FLOWING || below == WATER)) {
						blocks[packed - layer] = STONE;
					}
					boolean open = below == AIR;
					if (open && !lastBelow) {
						if (top == floodStack.length) {
							spill.add(floodStack);
							floodStack = new int[1048576];
							top = 0;
						}
						floodStack[top++] = packed - layer;
					}
					lastBelow = open;
				}
				packed++;
			}
		}
		return filled;
	}
}
