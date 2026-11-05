package dev.retroeras.worldgen.nether;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * ChunkProviderHell.populate: lava springs, fire, glowstone and mushrooms, and from 1.6.4 nether quartz and hidden lava.
 * Beta and 1.6.4 drew these from whatever state the generator's random was left in by the last chunk it made, so they
 * were never reproducible from the seed; here each chunk gets its own seeded random instead.
 */
final class NetherDecorator {
	private static final float[] SIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; i++) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
		}
	}

	private final WorldGenLevel level;
	private final Random random;
	private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

	private NetherDecorator(WorldGenLevel level, Random random) {
		this.level = level;
		this.random = random;
	}

	static void decorate(WorldGenLevel level, long seed, int chunkX, int chunkZ, NetherTerrain.Version version) {
		Random random = new Random(seed);
		long xScale = random.nextLong() / 2L * 2L + 1L;
		long zScale = random.nextLong() / 2L * 2L + 1L;
		random.setSeed(chunkX * xScale + chunkZ * zScale ^ seed);
		new NetherDecorator(level, random).populate(chunkX * 16, chunkZ * 16, version);
	}

	private void populate(int x, int z, NetherTerrain.Version version) {
		boolean release = version == NetherTerrain.Version.RELEASE;
		for (int i = 0; i < 8; i++) {
			lavaSpring(x + random.nextInt(16) + 8, random.nextInt(120) + 4, z + random.nextInt(16) + 8, false);
		}
		int fires = random.nextInt(random.nextInt(10) + 1) + 1;
		for (int i = 0; i < fires; i++) {
			fire(x + random.nextInt(16) + 8, random.nextInt(120) + 4, z + random.nextInt(16) + 8);
		}
		int glowstone = random.nextInt(random.nextInt(10) + 1);
		for (int i = 0; i < glowstone; i++) {
			glowstone(x + random.nextInt(16) + 8, random.nextInt(120) + 4, z + random.nextInt(16) + 8);
		}
		for (int i = 0; i < 10; i++) {
			glowstone(x + random.nextInt(16) + 8, random.nextInt(128), z + random.nextInt(16) + 8);
		}
		if (random.nextInt(1) == 0) {
			flowers(Blocks.BROWN_MUSHROOM, x + random.nextInt(16) + 8, random.nextInt(128), z + random.nextInt(16) + 8);
		}
		if (random.nextInt(1) == 0) {
			flowers(Blocks.RED_MUSHROOM, x + random.nextInt(16) + 8, random.nextInt(128), z + random.nextInt(16) + 8);
		}
		if (release) {
			for (int i = 0; i < 16; i++) {
				quartz(x + random.nextInt(16), random.nextInt(108) + 10, z + random.nextInt(16), 13);
			}
			for (int i = 0; i < 16; i++) {
				lavaSpring(x + random.nextInt(16), random.nextInt(108) + 10, z + random.nextInt(16), true);
			}
		}
	}

	private BlockState get(int x, int y, int z) {
		return level.getBlockState(cursor.set(x, y, z));
	}

	private boolean is(int x, int y, int z, Block block) {
		return get(x, y, z).is(block);
	}

	private boolean isAir(int x, int y, int z) {
		return get(x, y, z).isAir();
	}

	private void set(int x, int y, int z, BlockState state) {
		level.setBlock(cursor.set(x, y, z), state, 2);
	}

	/**
	 * WorldGenHellLava: lava pouring from a hole in a netherrack wall with exactly one open side. 1.6.4's hidden springs
	 * sit in netherrack with no open side at all, waiting to be dug into.
	 */
	private void lavaSpring(int x, int y, int z, boolean hidden) {
		if (!is(x, y + 1, z, Blocks.NETHERRACK)) {
			return;
		}
		if (!isAir(x, y, z) && !is(x, y, z, Blocks.NETHERRACK)) {
			return;
		}
		int rock = 0;
		int open = 0;
		int[][] sides = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
		for (int[] side : sides) {
			if (is(x + side[0], y + side[1], z + side[2], Blocks.NETHERRACK)) {
				rock++;
			}
		}
		for (int[] side : sides) {
			if (isAir(x + side[0], y + side[1], z + side[2])) {
				open++;
			}
		}
		if (!hidden && rock == 4 && open == 1 || hidden && rock == 5) {
			set(x, y, z, Blocks.LAVA.defaultBlockState());
			level.scheduleTick(cursor.set(x, y, z), Fluids.LAVA, 0);
		}
	}

	/** WorldGenFire: fire on netherrack. */
	private void fire(int x, int y, int z) {
		for (int i = 0; i < 64; i++) {
			int fx = x + random.nextInt(8) - random.nextInt(8);
			int fy = y + random.nextInt(4) - random.nextInt(4);
			int fz = z + random.nextInt(8) - random.nextInt(8);
			if (isAir(fx, fy, fz) && is(fx, fy - 1, fz, Blocks.NETHERRACK)) {
				set(fx, fy, fz, Blocks.FIRE.defaultBlockState());
			}
		}
	}

	/** WorldGenLightStone: a cluster of glowstone hanging from the netherrack roof. */
	private void glowstone(int x, int y, int z) {
		if (!isAir(x, y, z) || !is(x, y + 1, z, Blocks.NETHERRACK)) {
			return;
		}
		BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();
		set(x, y, z, glowstone);
		for (int i = 0; i < 1500; i++) {
			int gx = x + random.nextInt(8) - random.nextInt(8);
			int gy = y - random.nextInt(12);
			int gz = z + random.nextInt(8) - random.nextInt(8);
			if (!isAir(gx, gy, gz)) {
				continue;
			}
			int touching = 0;
			if (is(gx - 1, gy, gz, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (is(gx + 1, gy, gz, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (is(gx, gy - 1, gz, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (is(gx, gy + 1, gz, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (is(gx, gy, gz - 1, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (is(gx, gy, gz + 1, Blocks.GLOWSTONE)) {
				touching++;
			}
			if (touching == 1) {
				set(gx, gy, gz, glowstone);
			}
		}
	}

	/** WorldGenFlowers: mushrooms wherever they can grow. */
	private void flowers(Block block, int x, int y, int z) {
		BlockState state = block.defaultBlockState();
		for (int i = 0; i < 64; i++) {
			int fx = x + random.nextInt(8) - random.nextInt(8);
			int fy = y + random.nextInt(4) - random.nextInt(4);
			int fz = z + random.nextInt(8) - random.nextInt(8);
			if (fy < NetherTerrain.HEIGHT - 1 && isAir(fx, fy, fz) && state.canSurvive(level, cursor.set(fx, fy, fz))) {
				set(fx, fy, fz, state);
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

	/** WorldGenMinable: a vein of nether quartz in netherrack, as 1.6.4 laid out ore veins. */
	private void quartz(int x, int y, int z, int size) {
		BlockState ore = Blocks.NETHER_QUARTZ_ORE.defaultBlockState();
		float angle = random.nextFloat() * (float) Math.PI;
		double startX = x + 8 + sin(angle) * size / 8.0F;
		double endX = x + 8 - sin(angle) * size / 8.0F;
		double startZ = z + 8 + cos(angle) * size / 8.0F;
		double endZ = z + 8 - cos(angle) * size / 8.0F;
		double startY = y + random.nextInt(3) - 2;
		double endY = y + random.nextInt(3) - 2;
		for (int i = 0; i <= size; i++) {
			double cx = startX + (endX - startX) * i / size;
			double cy = startY + (endY - startY) * i / size;
			double cz = startZ + (endZ - startZ) * i / size;
			double spread = random.nextDouble() * size / 16.0;
			double width = (sin(i * (float) Math.PI / size) + 1.0F) * spread + 1.0;
			double height = (sin(i * (float) Math.PI / size) + 1.0F) * spread + 1.0;
			int minX = floor(cx - width / 2.0);
			int minY = floor(cy - height / 2.0);
			int minZ = floor(cz - width / 2.0);
			int maxX = floor(cx + width / 2.0);
			int maxY = floor(cy + height / 2.0);
			int maxZ = floor(cz + width / 2.0);
			for (int bx = minX; bx <= maxX; bx++) {
				double nx = (bx + 0.5 - cx) / (width / 2.0);
				if (nx * nx >= 1.0) {
					continue;
				}
				for (int by = minY; by <= maxY; by++) {
					double ny = (by + 0.5 - cy) / (height / 2.0);
					if (nx * nx + ny * ny >= 1.0) {
						continue;
					}
					for (int bz = minZ; bz <= maxZ; bz++) {
						double nz = (bz + 0.5 - cz) / (width / 2.0);
						if (nx * nx + ny * ny + nz * nz < 1.0 && is(bx, by, bz, Blocks.NETHERRACK)) {
							set(bx, by, bz, ore);
						}
					}
				}
			}
		}
	}
}
