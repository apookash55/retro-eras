package dev.retroeras.worldgen.beta;

import java.util.Random;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ChunkProviderGenerate.populate from Beta 1.7.3 and Alpha 1.2.6, and all it placed: lakes, dungeons, clay, ores, trees,
 * flowers, grass, reeds, pumpkins, cacti, springs and snow, in the old order from the old chunk random. Beta 1.7.3 added
 * lapis, tall grass, ferns, dead bushes, birches, spruces and a dungeon's cocoa beans to Alpha's list, and Alpha picked one
 * kind of tree for a whole chunk.
 */
final class BetaDecorator {
	private static final Direction[] PUMPKIN_FACING = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

	private final BetaWorld world;
	private final Random random;
	private final BetaTerrain terrain;
	private final boolean alpha;
	private final int blockX;
	private final int blockZ;

	private BetaDecorator(WorldGenLevel level, ChunkPos pos, BetaTerrain terrain, boolean alpha) {
		this.world = new BetaWorld(level, pos);
		this.terrain = terrain;
		this.alpha = alpha;
		this.blockX = pos.x() * 16;
		this.blockZ = pos.z() * 16;
		this.random = new Random(terrain.seed());
		long xScale = random.nextLong() / 2L * 2L + 1L;
		long zScale = random.nextLong() / 2L * 2L + 1L;
		random.setSeed(pos.x() * xScale + pos.z() * zScale ^ terrain.seed());
	}

	static void decorate(WorldGenLevel level, ChunkPos pos, BetaTerrain terrain, boolean alpha) {
		new BetaDecorator(level, pos, terrain, alpha).run();
	}

	private int x() {
		return blockX + random.nextInt(16) + 8;
	}

	private int z() {
		return blockZ + random.nextInt(16) + 8;
	}

	private void run() {
		BetaBiome biome = terrain.climate().biomeAt(blockX + 16, blockZ + 16);
		if (random.nextInt(4) == 0) {
			int x = x();
			int y = random.nextInt(128);
			lake(Blocks.WATER, x, y, z());
		}
		if (random.nextInt(8) == 0) {
			int x = x();
			int y = random.nextInt(random.nextInt(120) + 8);
			int z = z();
			if (y < 64 || random.nextInt(10) == 0) {
				lake(Blocks.LAVA, x, y, z);
			}
		}
		for (int i = 0; i < 8; i++) {
			int x = x();
			int y = random.nextInt(128);
			dungeon(x, y, z());
		}
		for (int i = 0; i < 10; i++) {
			int x = blockX + random.nextInt(16);
			int y = random.nextInt(128);
			clay(x, y, blockZ + random.nextInt(16));
		}
		ores(Blocks.DIRT, 20, 128, 32);
		ores(Blocks.GRAVEL, 10, 128, 32);
		ores(Blocks.COAL_ORE, 20, 128, 16);
		ores(Blocks.IRON_ORE, 20, 64, 8);
		ores(Blocks.GOLD_ORE, 2, 32, 8);
		ores(Blocks.REDSTONE_ORE, 8, 16, 7);
		ores(Blocks.DIAMOND_ORE, 1, 16, 7);
		if (!alpha) {
			int x = blockX + random.nextInt(16);
			int y = random.nextInt(16) + random.nextInt(16);
			ore(Blocks.LAPIS_ORE, x, y, blockZ + random.nextInt(16), 6);
		}
		trees(biome);
		if (!alpha) {
			for (int i = yellowFlowers(biome); i > 0; i--) {
				int x = x();
				int y = random.nextInt(128);
				flowers(Blocks.DANDELION, x, y, z());
			}
			for (int i = tallGrass(biome); i > 0; i--) {
				Block plant = biome == BetaBiome.RAINFOREST && random.nextInt(3) != 0 ? Blocks.FERN : Blocks.SHORT_GRASS;
				int x = x();
				int y = random.nextInt(128);
				tallGrass(plant, x, y, z());
			}
			if (biome == BetaBiome.DESERT) {
				for (int i = 0; i < 2; i++) {
					int x = x();
					int y = random.nextInt(128);
					deadBushes(x, y, z());
				}
			}
		} else {
			for (int i = 0; i < 2; i++) {
				int x = x();
				int y = random.nextInt(128);
				flowers(Blocks.DANDELION, x, y, z());
			}
		}
		if (random.nextInt(2) == 0) {
			int x = x();
			int y = random.nextInt(128);
			flowers(Blocks.POPPY, x, y, z());
		}
		if (random.nextInt(4) == 0) {
			int x = x();
			int y = random.nextInt(128);
			flowers(Blocks.BROWN_MUSHROOM, x, y, z());
		}
		if (random.nextInt(8) == 0) {
			int x = x();
			int y = random.nextInt(128);
			flowers(Blocks.RED_MUSHROOM, x, y, z());
		}
		for (int i = 0; i < 10; i++) {
			int x = x();
			int y = random.nextInt(128);
			reeds(x, y, z());
		}
		if (random.nextInt(32) == 0) {
			int x = x();
			int y = random.nextInt(128);
			pumpkins(x, y, z());
		}
		if (biome == BetaBiome.DESERT) {
			for (int i = 0; i < 10; i++) {
				int x = x();
				int y = random.nextInt(128);
				cacti(x, y, z());
			}
		}
		for (int i = 0; i < 50; i++) {
			int x = x();
			int y = random.nextInt(random.nextInt(120) + 8);
			spring(Blocks.WATER, x, y, z());
		}
		for (int i = 0; i < 20; i++) {
			int x = x();
			int y = random.nextInt(random.nextInt(random.nextInt(112) + 8) + 8);
			spring(Blocks.LAVA, x, y, z());
		}
		snow();
	}

	private static int yellowFlowers(BetaBiome biome) {
		return switch (biome) {
			case FOREST, TAIGA -> 2;
			case SEASONAL_FOREST -> 4;
			case PLAINS -> 3;
			default -> 0;
		};
	}

	private static int tallGrass(BetaBiome biome) {
		return switch (biome) {
			case FOREST, SEASONAL_FOREST -> 2;
			case RAINFOREST, PLAINS -> 10;
			case TAIGA -> 1;
			default -> 0;
		};
	}

	private void trees(BetaBiome biome) {
		int noise = (int) ((terrain.treeNoise(blockX * 0.5, blockZ * 0.5) / 8.0 + random.nextDouble() * 4.0 + 4.0) / 3.0);
		int count = random.nextInt(10) == 0 ? 1 : 0;
		count += switch (biome) {
			case FOREST, RAINFOREST, TAIGA -> noise + 5;
			case SEASONAL_FOREST -> noise + 2;
			case DESERT, TUNDRA, PLAINS -> -20;
			default -> 0;
		};
		BetaTrees.Tree chunkTree = null;
		if (alpha) {
			chunkTree = random.nextInt(10) == 0 ? new BetaTrees.BigTree() : BetaTrees.OAK;
			if (biome == BetaBiome.RAINFOREST && random.nextInt(3) == 0) {
				chunkTree = new BetaTrees.BigTree();
			}
		}
		for (int i = 0; i < count; i++) {
			int x = x();
			int z = z();
			BetaTrees.Tree tree = alpha ? chunkTree : tree(biome);
			tree.place(world, random, x, world.height(x, z), z);
		}
	}

	/** BiomeGenBase.getRandomWorldGenForTrees. */
	private BetaTrees.Tree tree(BetaBiome biome) {
		return switch (biome) {
			case RAINFOREST -> random.nextInt(3) == 0 ? new BetaTrees.BigTree() : BetaTrees.OAK;
			case FOREST -> random.nextInt(5) == 0 ? BetaTrees.BIRCH : random.nextInt(3) == 0 ? new BetaTrees.BigTree() : BetaTrees.OAK;
			case TAIGA -> random.nextInt(3) == 0 ? BetaTrees.PINE : BetaTrees.SPRUCE;
			default -> random.nextInt(10) == 0 ? new BetaTrees.BigTree() : BetaTrees.OAK;
		};
	}

	/** WorldGenLakes. */
	private void lake(Block liquid, int x, int y, int z) {
		x -= 8;
		z -= 8;
		while (y > 0 && world.isAir(x, y, z)) {
			y--;
		}
		y -= 4;
		boolean[] shape = new boolean[2048];
		int blobs = random.nextInt(4) + 4;
		for (int blob = 0; blob < blobs; blob++) {
			double sizeX = random.nextDouble() * 6.0 + 3.0;
			double sizeY = random.nextDouble() * 4.0 + 2.0;
			double sizeZ = random.nextDouble() * 6.0 + 3.0;
			double centreX = random.nextDouble() * (16.0 - sizeX - 2.0) + 1.0 + sizeX / 2.0;
			double centreY = random.nextDouble() * (8.0 - sizeY - 4.0) + 2.0 + sizeY / 2.0;
			double centreZ = random.nextDouble() * (16.0 - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;
			for (int a = 1; a < 15; a++) {
				for (int b = 1; b < 15; b++) {
					for (int c = 1; c < 7; c++) {
						double dx = (a - centreX) / (sizeX / 2.0);
						double dy = (c - centreY) / (sizeY / 2.0);
						double dz = (b - centreZ) / (sizeZ / 2.0);
						if (dx * dx + dy * dy + dz * dz < 1.0) {
							shape[(a * 16 + b) * 8 + c] = true;
						}
					}
				}
			}
		}
		for (int a = 0; a < 16; a++) {
			for (int b = 0; b < 16; b++) {
				for (int c = 0; c < 8; c++) {
					if (lakeEdge(shape, a, b, c)) {
						BlockState state = world.get(x + a, y + c, z + b);
						if (c >= 4 && !state.getFluidState().isEmpty()) {
							return;
						}
						if (c < 4 && !world.isSolid(x + a, y + c, z + b) && !state.is(liquid)) {
							return;
						}
					}
				}
			}
		}
		for (int a = 0; a < 16; a++) {
			for (int b = 0; b < 16; b++) {
				for (int c = 0; c < 8; c++) {
					if (shape[(a * 16 + b) * 8 + c]) {
						world.set(x + a, y + c, z + b, c >= 4 ? Blocks.AIR.defaultBlockState() : liquid.defaultBlockState());
					}
				}
			}
		}
		for (int a = 0; a < 16; a++) {
			for (int b = 0; b < 16; b++) {
				for (int c = 4; c < 8; c++) {
					// Dirt left in daylight by the digging goes back to grass.
					if (shape[(a * 16 + b) * 8 + c] && world.get(x + a, y + c - 1, z + b).is(Blocks.DIRT) && world.isOutdoors(x + a, y + c, z + b)) {
						world.set(x + a, y + c - 1, z + b, Blocks.GRASS_BLOCK.defaultBlockState());
					}
				}
			}
		}
		if (liquid == Blocks.LAVA) {
			for (int a = 0; a < 16; a++) {
				for (int b = 0; b < 16; b++) {
					for (int c = 0; c < 8; c++) {
						if (lakeEdge(shape, a, b, c) && (c < 4 || random.nextInt(2) != 0) && world.isSolid(x + a, y + c, z + b)) {
							world.set(x + a, y + c, z + b, Blocks.STONE.defaultBlockState());
						}
					}
				}
			}
		}
	}

	private static boolean lakeEdge(boolean[] shape, int a, int b, int c) {
		return !shape[(a * 16 + b) * 8 + c]
				&& (a < 15 && shape[((a + 1) * 16 + b) * 8 + c]
						|| a > 0 && shape[((a - 1) * 16 + b) * 8 + c]
						|| b < 15 && shape[(a * 16 + b + 1) * 8 + c]
						|| b > 0 && shape[(a * 16 + b - 1) * 8 + c]
						|| c < 7 && shape[(a * 16 + b) * 8 + c + 1]
						|| c > 0 && shape[(a * 16 + b) * 8 + c - 1]);
	}

	/** WorldGenDungeons: a cobbled room, mossy on the floor, with a spawner and up to two chests. */
	private void dungeon(int x, int y, int z) {
		int high = 3;
		int sizeX = random.nextInt(2) + 2;
		int sizeZ = random.nextInt(2) + 2;
		int openings = 0;
		for (int bx = x - sizeX - 1; bx <= x + sizeX + 1; bx++) {
			for (int by = y - 1; by <= y + high + 1; by++) {
				for (int bz = z - sizeZ - 1; bz <= z + sizeZ + 1; bz++) {
					if ((by == y - 1 || by == y + high + 1) && !world.isSolid(bx, by, bz)) {
						return;
					}
					if ((bx == x - sizeX - 1 || bx == x + sizeX + 1 || bz == z - sizeZ - 1 || bz == z + sizeZ + 1)
							&& by == y && world.isAir(bx, by, bz) && world.isAir(bx, by + 1, bz)) {
						openings++;
					}
				}
			}
		}
		if (openings < 1 || openings > 5) {
			return;
		}
		for (int bx = x - sizeX - 1; bx <= x + sizeX + 1; bx++) {
			for (int by = y + high; by >= y - 1; by--) {
				for (int bz = z - sizeZ - 1; bz <= z + sizeZ + 1; bz++) {
					if (bx != x - sizeX - 1 && by != y - 1 && bz != z - sizeZ - 1 && bx != x + sizeX + 1 && by != y + high + 1 && bz != z + sizeZ + 1) {
						world.set(bx, by, bz, Blocks.AIR.defaultBlockState());
					} else if (by >= 0 && !world.isSolid(bx, by - 1, bz)) {
						world.set(bx, by, bz, Blocks.AIR.defaultBlockState());
					} else if (world.isSolid(bx, by, bz)) {
						world.set(bx, by, bz, by == y - 1 && random.nextInt(4) != 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					}
				}
			}
		}
		for (int chest = 0; chest < 2; chest++) {
			for (int attempt = 0; attempt < 3; attempt++) {
				int cx = x + random.nextInt(sizeX * 2 + 1) - sizeX;
				int cz = z + random.nextInt(sizeZ * 2 + 1) - sizeZ;
				if (world.isAir(cx, y, cz)) {
					int walls = 0;
					walls += world.isSolid(cx - 1, y, cz) ? 1 : 0;
					walls += world.isSolid(cx + 1, y, cz) ? 1 : 0;
					walls += world.isSolid(cx, y, cz - 1) ? 1 : 0;
					walls += world.isSolid(cx, y, cz + 1) ? 1 : 0;
					if (walls == 1) {
						world.set(cx, y, cz, Blocks.CHEST.defaultBlockState());
						if (world.level().getBlockEntity(new BlockPos(cx, y, cz)) instanceof Container container) {
							for (int i = 0; i < 8; i++) {
								ItemStack loot = dungeonLoot();
								if (loot != null) {
									container.setItem(random.nextInt(container.getContainerSize()), loot);
								}
							}
						}
						break;
					}
				}
			}
		}
		world.set(x, y, z, Blocks.SPAWNER.defaultBlockState());
		BlockEntity spawner = world.level().getBlockEntity(new BlockPos(x, y, z));
		if (spawner instanceof SpawnerBlockEntity entity) {
			entity.setEntityId(dungeonMob(), world.level().getRandom());
		}
	}

	private EntityType<?> dungeonMob() {
		return switch (random.nextInt(4)) {
			case 0 -> EntityTypes.SKELETON;
			case 3 -> EntityTypes.SPIDER;
			default -> EntityTypes.ZOMBIE;
		};
	}

	private @Nullable ItemStack dungeonLoot() {
		int pick = random.nextInt(11);
		return switch (pick) {
			case 0 -> new ItemStack(Items.SADDLE);
			case 1 -> new ItemStack(Items.IRON_INGOT, random.nextInt(4) + 1);
			case 2 -> new ItemStack(Items.BREAD);
			case 3 -> new ItemStack(Items.WHEAT, random.nextInt(4) + 1);
			case 4 -> new ItemStack(Items.GUNPOWDER, random.nextInt(4) + 1);
			case 5 -> new ItemStack(Items.STRING, random.nextInt(4) + 1);
			case 6 -> new ItemStack(Items.BUCKET);
			case 7 -> random.nextInt(100) == 0 ? new ItemStack(Items.GOLDEN_APPLE) : null;
			case 8 -> random.nextInt(2) == 0 ? new ItemStack(Items.REDSTONE, random.nextInt(4) + 1) : null;
			case 9 -> random.nextInt(10) == 0 ? new ItemStack(random.nextInt(2) == 0 ? Items.MUSIC_DISC_13 : Items.MUSIC_DISC_CAT) : null;
			default -> alpha ? null : new ItemStack(Items.COCOA_BEANS);
		};
	}

	/** WorldGenClay: from water, it turns sand to clay. */
	private void clay(int x, int y, int z) {
		if (world.isWater(x, y, z)) {
			vein(x, y, z, 32, Blocks.SAND, Blocks.CLAY);
		}
	}

	private void ores(Block ore, int count, int range, int size) {
		for (int i = 0; i < count; i++) {
			int x = blockX + random.nextInt(16);
			int y = random.nextInt(range);
			ore(ore, x, y, blockZ + random.nextInt(16), size);
		}
	}

	private void ore(Block ore, int x, int y, int z, int size) {
		vein(x, y, z, size, Blocks.STONE, ore);
	}

	/** WorldGenMinable: blobs along a short line through the point. */
	private void vein(int x, int y, int z, int size, Block replaces, Block with) {
		float angle = random.nextFloat() * (float) Math.PI;
		double x1 = x + 8 + Mth.sin(angle) * size / 8.0F;
		double x2 = x + 8 - Mth.sin(angle) * size / 8.0F;
		double z1 = z + 8 + Mth.cos(angle) * size / 8.0F;
		double z2 = z + 8 - Mth.cos(angle) * size / 8.0F;
		double y1 = y + random.nextInt(3) + 2;
		double y2 = y + random.nextInt(3) + 2;
		BlockState state = with.defaultBlockState();
		for (int i = 0; i <= size; i++) {
			double cx = x1 + (x2 - x1) * i / size;
			double cy = y1 + (y2 - y1) * i / size;
			double cz = z1 + (z2 - z1) * i / size;
			double scale = random.nextDouble() * size / 16.0;
			double diameter = (Mth.sin(i * (float) Math.PI / size) + 1.0F) * scale + 1.0;
			for (int bx = Mth.floor(cx - diameter / 2.0); bx <= Mth.floor(cx + diameter / 2.0); bx++) {
				double dx = (bx + 0.5 - cx) / (diameter / 2.0);
				for (int by = Mth.floor(cy - diameter / 2.0); by <= Mth.floor(cy + diameter / 2.0); by++) {
					double dy = (by + 0.5 - cy) / (diameter / 2.0);
					for (int bz = Mth.floor(cz - diameter / 2.0); bz <= Mth.floor(cz + diameter / 2.0); bz++) {
						double dz = (bz + 0.5 - cz) / (diameter / 2.0);
						if (dx * dx + dy * dy + dz * dz < 1.0 && world.get(bx, by, bz).is(replaces)) {
							world.set(bx, by, bz, state);
						}
					}
				}
			}
		}
	}

	private boolean onSoil(int x, int y, int z) {
		BlockState below = world.get(x, y - 1, z);
		return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.FARMLAND);
	}

	/** WorldGenFlowers, for flowers and mushrooms: flowers want daylight on soil, mushrooms the dark on anything opaque. */
	private void flowers(Block plant, int x, int y, int z) {
		boolean mushroom = plant == Blocks.BROWN_MUSHROOM || plant == Blocks.RED_MUSHROOM;
		for (int i = 0; i < 64; i++) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if (world.isAir(px, py, pz)) {
				boolean stays = mushroom
						? world.isOpaque(px, py - 1, pz) && !world.isOutdoors(px, py, pz)
						: onSoil(px, py, pz) && world.isOutdoors(px, py, pz);
				if (stays) {
					world.set(px, py, pz, plant.defaultBlockState());
				}
			}
		}
	}

	private int groundBelow(int x, int y, int z) {
		while (y > 0) {
			BlockState state = world.get(x, y, z);
			if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
				break;
			}
			y--;
		}
		return y;
	}

	/** WorldGenTallGrass. */
	private void tallGrass(Block plant, int x, int y, int z) {
		y = groundBelow(x, y, z);
		for (int i = 0; i < 128; i++) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if (world.isAir(px, py, pz) && onSoil(px, py, pz) && world.isOutdoors(px, py, pz)) {
				world.set(px, py, pz, plant.defaultBlockState());
			}
		}
	}

	/** WorldGenDeadBush. */
	private void deadBushes(int x, int y, int z) {
		y = groundBelow(x, y, z);
		for (int i = 0; i < 4; i++) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if (world.isAir(px, py, pz) && world.get(px, py - 1, pz).is(Blocks.SAND) && world.isOutdoors(px, py, pz)) {
				world.set(px, py, pz, Blocks.DEAD_BUSH.defaultBlockState());
			}
		}
	}

	/** WorldGenReed: only on grass or dirt, as it was until sand was allowed later. */
	private void reeds(int x, int y, int z) {
		for (int i = 0; i < 20; i++) {
			int px = x + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(4) - random.nextInt(4);
			if (world.isAir(px, y, pz) && (world.isWater(px - 1, y - 1, pz) || world.isWater(px + 1, y - 1, pz) || world.isWater(px, y - 1, pz - 1) || world.isWater(px, y - 1, pz + 1))) {
				int height = 2 + random.nextInt(random.nextInt(3) + 1);
				for (int h = 0; h < height; h++) {
					if (reedCanStay(px, y + h, pz)) {
						world.set(px, y + h, pz, Blocks.SUGAR_CANE.defaultBlockState());
					}
				}
			}
		}
	}

	private boolean reedCanStay(int x, int y, int z) {
		BlockState below = world.get(x, y - 1, z);
		if (below.is(Blocks.SUGAR_CANE)) {
			return true;
		}
		return (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT))
				&& (world.isWater(x - 1, y - 1, z) || world.isWater(x + 1, y - 1, z) || world.isWater(x, y - 1, z - 1) || world.isWater(x, y - 1, z + 1));
	}

	/** WorldGenPumpkin: wild pumpkins had their faces, until shears came to carve them. */
	private void pumpkins(int x, int y, int z) {
		for (int i = 0; i < 64; i++) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if (world.isAir(px, py, pz) && world.get(px, py - 1, pz).is(Blocks.GRASS_BLOCK)) {
				world.set(px, py, pz, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, PUMPKIN_FACING[random.nextInt(4)]));
			}
		}
	}

	/** WorldGenCactus. */
	private void cacti(int x, int y, int z) {
		for (int i = 0; i < 10; i++) {
			int px = x + random.nextInt(8) - random.nextInt(8);
			int py = y + random.nextInt(4) - random.nextInt(4);
			int pz = z + random.nextInt(8) - random.nextInt(8);
			if (world.isAir(px, py, pz)) {
				int height = 1 + random.nextInt(random.nextInt(3) + 1);
				for (int h = 0; h < height; h++) {
					if (cactusCanStay(px, py + h, pz)) {
						world.set(px, py + h, pz, Blocks.CACTUS.defaultBlockState());
					}
				}
			}
		}
	}

	private boolean cactusCanStay(int x, int y, int z) {
		if (world.isSolid(x - 1, y, z) || world.isSolid(x + 1, y, z) || world.isSolid(x, y, z - 1) || world.isSolid(x, y, z + 1)) {
			return false;
		}
		BlockState below = world.get(x, y - 1, z);
		return below.is(Blocks.CACTUS) || below.is(Blocks.SAND);
	}

	/** WorldGenLiquids: a source with stone on three sides and air on the fourth, flowing straight away. */
	private void spring(Block liquid, int x, int y, int z) {
		if (!world.get(x, y + 1, z).is(Blocks.STONE) || !world.get(x, y - 1, z).is(Blocks.STONE)) {
			return;
		}
		BlockState here = world.get(x, y, z);
		if (!here.isAir() && !here.is(Blocks.STONE)) {
			return;
		}
		int stone = 0;
		int air = 0;
		for (int[] side : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			BlockState state = world.get(x + side[0], y, z + side[1]);
			stone += state.is(Blocks.STONE) ? 1 : 0;
			air += state.isAir() ? 1 : 0;
		}
		if (stone == 3 && air == 1) {
			BlockState source = liquid.defaultBlockState();
			world.set(x, y, z, source);
			world.level().scheduleTick(new BlockPos(x, y, z), source.getFluidState().getType(), 0);
		}
	}

	/** The last step: snow wherever it is cold enough for the height, on anything solid but ice. */
	private void snow() {
		double[] temperature = terrain.climate().sample(blockX + 8, blockZ + 8, 16, 16).temperature();
		for (int x = blockX + 8; x < blockX + 24; x++) {
			for (int z = blockZ + 8; z < blockZ + 24; z++) {
				int y = BetaTerrain.HEIGHT - 1;
				while (y > 0 && !blocksSnow(world.get(x, y, z))) {
					y--;
				}
				y++;
				double cold = temperature[(x - (blockX + 8)) * 16 + (z - (blockZ + 8))] - (y - 64) / 64.0 * 0.3;
				if (cold < 0.5 && y > 0 && y < BetaTerrain.HEIGHT && world.isAir(x, y, z)) {
					BlockState below = world.get(x, y - 1, z);
					if (below.getFluidState().isEmpty() && !below.is(Blocks.ICE)) {
						world.set(x, y, z, Blocks.SNOW.defaultBlockState());
						if (below.hasProperty(GrassBlock.SNOWY)) {
							world.set(x, y - 1, z, below.setValue(GrassBlock.SNOWY, true));
						}
					}
				}
			}
		}
	}

	/** Material.blocksMovement or isLiquid: what World.findTopSolidBlock stops at. Plants do not count. */
	private static boolean blocksSnow(BlockState state) {
		return state.isSolid() || state.is(net.minecraft.tags.BlockTags.LEAVES) || !state.getFluidState().isEmpty();
	}
}
