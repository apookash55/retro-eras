package dev.retroeras.worldgen.beta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Beta 1.7.3's trees, ported: oak, birch, big oak and the two kinds of spruce. Alpha had only the oak and the big oak. */
final class BetaTrees {
	private BetaTrees() {
	}

	@FunctionalInterface
	interface Tree {
		boolean place(BetaWorld world, Random random, int x, int y, int z);
	}

	private static boolean airOrLeaves(BlockState state) {
		return state.isAir() || state.is(BlockTags.LEAVES);
	}

	private static boolean onGround(BetaWorld world, int x, int y, int z) {
		BlockState ground = world.get(x, y - 1, z);
		return ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT);
	}

	private static void leaf(BetaWorld world, Block leaves, int x, int y, int z) {
		if (!world.isOpaque(x, y, z)) {
			world.set(x, y, z, leaves.defaultBlockState());
		}
	}

	private static void trunk(BetaWorld world, Block log, List<BlockPos> logs, int x, int y, int z, int height) {
		for (int i = 0; i < height; i++) {
			if (airOrLeaves(world.get(x, y + i, z))) {
				world.set(x, y + i, z, log.defaultBlockState());
				logs.add(new BlockPos(x, y + i, z));
			}
		}
		world.settleLeaves(logs);
	}

	/** WorldGenTrees, and WorldGenForest with a birch's log and leaves and one block more height. */
	private static Tree round(Block log, Block leaves, int minHeight) {
		return (world, random, x, y, z) -> {
			int height = random.nextInt(3) + minHeight;
			if (y < 1 || y + height + 1 > BetaTerrain.HEIGHT) {
				return false;
			}
			for (int by = y; by <= y + 1 + height; by++) {
				int radius = by == y ? 0 : by >= y + 1 + height - 2 ? 2 : 1;
				for (int bx = x - radius; bx <= x + radius; bx++) {
					for (int bz = z - radius; bz <= z + radius; bz++) {
						if (by >= BetaTerrain.HEIGHT || !airOrLeaves(world.get(bx, by, bz))) {
							return false;
						}
					}
				}
			}
			if (!onGround(world, x, y, z) || y >= BetaTerrain.HEIGHT - height - 1) {
				return false;
			}
			world.set(x, y - 1, z, Blocks.DIRT.defaultBlockState());
			for (int by = y - 3 + height; by <= y + height; by++) {
				int fromTop = by - (y + height);
				int radius = 1 - fromTop / 2;
				for (int bx = x - radius; bx <= x + radius; bx++) {
					for (int bz = z - radius; bz <= z + radius; bz++) {
						if ((Math.abs(bx - x) != radius || Math.abs(bz - z) != radius || random.nextInt(2) != 0 && fromTop != 0)) {
							leaf(world, leaves, bx, by, bz);
						}
					}
				}
			}
			trunk(world, log, new ArrayList<>(), x, y, z, height);
			return true;
		};
	}

	static final Tree OAK = round(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4);
	static final Tree BIRCH = round(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5);

	private static boolean clear(BetaWorld world, int x, int y, int z, int height, int bare, int radius) {
		if (y < 1 || y + height + 1 > BetaTerrain.HEIGHT) {
			return false;
		}
		for (int by = y; by <= y + 1 + height; by++) {
			int r = by - y < bare ? 0 : radius;
			for (int bx = x - r; bx <= x + r; bx++) {
				for (int bz = z - r; bz <= z + r; bz++) {
					if (by >= BetaTerrain.HEIGHT || !airOrLeaves(world.get(bx, by, bz))) {
						return false;
					}
				}
			}
		}
		return onGround(world, x, y, z) && y < BetaTerrain.HEIGHT - height - 1;
	}

	private static void spruceLayer(BetaWorld world, int x, int y, int z, int radius) {
		for (int bx = x - radius; bx <= x + radius; bx++) {
			for (int bz = z - radius; bz <= z + radius; bz++) {
				if (Math.abs(bx - x) != radius || Math.abs(bz - z) != radius || radius <= 0) {
					leaf(world, Blocks.SPRUCE_LEAVES, bx, y, bz);
				}
			}
		}
	}

	/** WorldGenTaiga1: the tall one with a tuft of leaves at the top. */
	static final Tree PINE = (world, random, x, y, z) -> {
		int height = random.nextInt(5) + 7;
		int bare = height - random.nextInt(2) - 3;
		int crown = height - bare;
		int radius = 1 + random.nextInt(crown + 1);
		if (!clear(world, x, y, z, height, bare, radius)) {
			return false;
		}
		world.set(x, y - 1, z, Blocks.DIRT.defaultBlockState());
		int layer = 0;
		for (int by = y + height; by >= y + bare; by--) {
			spruceLayer(world, x, by, z, layer);
			if (layer >= 1 && by == y + bare + 1) {
				layer--;
			} else if (layer < radius) {
				layer++;
			}
		}
		trunk(world, Blocks.SPRUCE_LOG, new ArrayList<>(), x, y, z, height - 1);
		return true;
	};

	/** WorldGenTaiga2: the bushy one, its layers widening in steps as they go down. */
	static final Tree SPRUCE = (world, random, x, y, z) -> {
		int height = random.nextInt(4) + 6;
		int bare = 1 + random.nextInt(2);
		int crown = height - bare;
		int radius = 2 + random.nextInt(2);
		if (!clear(world, x, y, z, height, bare, radius)) {
			return false;
		}
		world.set(x, y - 1, z, Blocks.DIRT.defaultBlockState());
		int layer = random.nextInt(2);
		int widest = 1;
		int next = 0;
		for (int down = 0; down <= crown; down++) {
			spruceLayer(world, x, y + height - down, z, layer);
			if (layer >= widest) {
				layer = next;
				next = 1;
				if (++widest > radius) {
					widest = radius;
				}
			} else {
				layer++;
			}
		}
		trunk(world, Blocks.SPRUCE_LOG, new ArrayList<>(), x, y, z, height - random.nextInt(3));
		return true;
	};

	/**
	 * WorldGenBigTree, ported. Beta asks for a new one for every tree; Alpha kept one for a whole chunk, and as it only picks
	 * its height once, every big tree in that chunk came out the same height.
	 */
	static final class BigTree implements Tree {
		private static final byte[] AXES = {2, 0, 0, 1, 2, 1};
		private static final int CLUSTER_HEIGHT = 5;

		private final Random random = new Random();
		private final int[] base = {0, 0, 0};
		private final List<BlockPos> logs = new ArrayList<>();
		private BetaWorld world;
		private int height;
		private int trunkHeight;
		private int[][] clusters;

		@Override
		public boolean place(BetaWorld world, Random outer, int x, int y, int z) {
			this.world = world;
			random.setSeed(outer.nextLong());
			base[0] = x;
			base[1] = y;
			base[2] = z;
			if (height == 0) {
				height = 5 + random.nextInt(12);
			}
			if (!canGrow()) {
				return false;
			}
			logs.clear();
			placeClusterPoints();
			for (int[] cluster : clusters) {
				for (int by = cluster[1]; by < cluster[1] + CLUSTER_HEIGHT; by++) {
					int layer = by - cluster[1];
					disc(cluster[0], by, cluster[2], layer != 0 && layer != CLUSTER_HEIGHT - 1 ? 3.0F : 2.0F);
				}
			}
			line(new int[] {base[0], base[1], base[2]}, new int[] {base[0], base[1] + trunkHeight, base[2]});
			int[] from = {base[0], base[1], base[2]};
			for (int[] cluster : clusters) {
				from[1] = cluster[3];
				if (from[1] - base[1] >= height * 0.2) {
					line(from, new int[] {cluster[0], cluster[1], cluster[2]});
				}
			}
			world.settleLeaves(logs);
			return true;
		}

		private void placeClusterPoints() {
			trunkHeight = (int) (height * 0.618);
			if (trunkHeight >= height) {
				trunkHeight = height - 1;
			}
			int perLayer = Math.max((int) (1.382 + Math.pow(height / 13.0, 2.0)), 1);
			int[][] points = new int[perLayer * height][4];
			int layerY = base[1] + height - CLUSTER_HEIGHT;
			int count = 1;
			int trunkTop = base[1] + trunkHeight;
			int layer = layerY - base[1];
			points[0][0] = base[0];
			points[0][1] = layerY;
			points[0][2] = base[2];
			points[0][3] = trunkTop;
			layerY--;
			while (layer >= 0) {
				float size = layerSize(layer);
				if (size >= 0.0F) {
					for (int i = 0; i < perLayer; i++) {
						double distance = size * (random.nextFloat() + 0.328);
						double angle = random.nextFloat() * 2.0 * 3.14159;
						int px = Mth.floor(distance * Math.sin(angle) + base[0] + 0.5);
						int pz = Mth.floor(distance * Math.cos(angle) + base[2] + 0.5);
						int[] point = {px, layerY, pz};
						if (blocked(point, new int[] {px, layerY + CLUSTER_HEIGHT, pz}) == -1) {
							int[] branch = {base[0], base[1], base[2]};
							double along = Math.sqrt(Math.pow(Math.abs(base[0] - point[0]), 2.0) + Math.pow(Math.abs(base[2] - point[2]), 2.0)) * 0.381;
							branch[1] = point[1] - along > trunkTop ? trunkTop : (int) (point[1] - along);
							if (blocked(branch, point) == -1) {
								points[count][0] = px;
								points[count][1] = layerY;
								points[count][2] = pz;
								points[count][3] = branch[1];
								count++;
							}
						}
					}
				}
				layerY--;
				layer--;
			}
			clusters = new int[count][4];
			System.arraycopy(points, 0, clusters, 0, count);
		}

		private float layerSize(int layer) {
			if (layer < height * 0.3) {
				return -1.618F;
			}
			float half = height / 2.0F;
			float offset = half - layer;
			float size;
			if (offset == 0.0F) {
				size = half;
			} else if (Math.abs(offset) >= half) {
				size = 0.0F;
			} else {
				size = (float) Math.sqrt(Math.pow(Math.abs(half), 2.0) - Math.pow(Math.abs(offset), 2.0));
			}
			return size * 0.5F;
		}

		private void disc(int x, int y, int z, float radius) {
			int extent = (int) (radius + 0.618);
			for (int a = -extent; a <= extent; a++) {
				for (int b = -extent; b <= extent; b++) {
					if (Math.sqrt(Math.pow(Math.abs(a) + 0.5, 2.0) + Math.pow(Math.abs(b) + 0.5, 2.0)) <= radius && airOrLeaves(world.get(x + a, y, z + b))) {
						world.set(x + a, y, z + b, Blocks.OAK_LEAVES.defaultBlockState());
					}
				}
			}
		}

		private void line(int[] from, int[] to) {
			int[] delta = new int[3];
			int major = 0;
			for (int i = 0; i < 3; i++) {
				delta[i] = to[i] - from[i];
				if (Math.abs(delta[i]) > Math.abs(delta[major])) {
					major = i;
				}
			}
			if (delta[major] == 0) {
				return;
			}
			int minorA = AXES[major];
			int minorB = AXES[major + 3];
			int step = delta[major] > 0 ? 1 : -1;
			double slopeA = (double) delta[minorA] / delta[major];
			double slopeB = (double) delta[minorB] / delta[major];
			int[] pos = new int[3];
			for (int i = 0, end = delta[major] + step; i != end; i += step) {
				pos[major] = Mth.floor(from[major] + i + 0.5);
				pos[minorA] = Mth.floor(from[minorA] + i * slopeA + 0.5);
				pos[minorB] = Mth.floor(from[minorB] + i * slopeB + 0.5);
				world.set(pos[0], pos[1], pos[2], Blocks.OAK_LOG.defaultBlockState());
				logs.add(new BlockPos(pos[0], pos[1], pos[2]));
			}
		}

		/** How far a line gets before hitting something other than air or leaves, or -1 if it gets through. */
		private int blocked(int[] from, int[] to) {
			int[] delta = new int[3];
			int major = 0;
			for (int i = 0; i < 3; i++) {
				delta[i] = to[i] - from[i];
				if (Math.abs(delta[i]) > Math.abs(delta[major])) {
					major = i;
				}
			}
			if (delta[major] == 0) {
				return -1;
			}
			int minorA = AXES[major];
			int minorB = AXES[major + 3];
			int step = delta[major] > 0 ? 1 : -1;
			double slopeA = (double) delta[minorA] / delta[major];
			double slopeB = (double) delta[minorB] / delta[major];
			int[] pos = new int[3];
			int end = delta[major] + step;
			int i = 0;
			for (; i != end; i += step) {
				pos[major] = from[major] + i;
				pos[minorA] = Mth.floor(from[minorA] + i * slopeA);
				pos[minorB] = Mth.floor(from[minorB] + i * slopeB);
				if (!airOrLeaves(world.get(pos[0], pos[1], pos[2]))) {
					break;
				}
			}
			return i == end ? -1 : Math.abs(i);
		}

		private boolean canGrow() {
			if (!onGround(world, base[0], base[1], base[2])) {
				return false;
			}
			int reached = blocked(new int[] {base[0], base[1], base[2]}, new int[] {base[0], base[1] + height - 1, base[2]});
			if (reached == -1) {
				return true;
			}
			if (reached < 6) {
				return false;
			}
			height = reached;
			return true;
		}
	}
}
