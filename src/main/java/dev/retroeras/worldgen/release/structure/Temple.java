package dev.retroeras.worldgen.release.structure;

import java.util.Random;
import java.util.Set;

import dev.retroeras.worldgen.release.ReleaseBiome;
import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.Items;

/** 1.6.4's scattered features (MapGenScatteredFeature): desert pyramids, jungle temples and witch huts. */
public final class Temple extends LegacyStructureFeature {
	private static final Set<ReleaseBiome> BIOMES = Set.of(ReleaseBiome.DESERT, ReleaseBiome.DESERT_HILLS, ReleaseBiome.JUNGLE,
			ReleaseBiome.JUNGLE_HILLS, ReleaseBiome.SWAMPLAND);
	private static final int SPACING = 32;
	private static final int SEPARATION = 8;
	/** Direction.offsetX and offsetZ. */
	private static final int[] OFFSET_X = {0, -1, 0, 1};
	private static final int[] OFFSET_Z = {1, 0, -1, 0};
	private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.DIAMOND, 1, 3, 3),
			LegacyLoot.of(Items.IRON_INGOT, 1, 5, 10),
			LegacyLoot.of(Items.GOLD_INGOT, 2, 7, 15),
			LegacyLoot.of(Items.EMERALD, 1, 3, 2),
			LegacyLoot.of(Items.BONE, 4, 6, 20),
			LegacyLoot.of(Items.ROTTEN_FLESH, 3, 7, 16),
			LegacyLoot.of(Items.SADDLE, 1, 1, 3),
			LegacyLoot.of(Items.IRON_HORSE_ARMOR, 1, 1, 1),
			LegacyLoot.of(Items.GOLDEN_HORSE_ARMOR, 1, 1, 1),
			LegacyLoot.of(Items.DIAMOND_HORSE_ARMOR, 1, 1, 1)};
	private static final LegacyLoot[] ARROWS = {LegacyLoot.of(Items.ARROW, 2, 7, 30)};

	private final ReleaseTerrain terrain;

	public Temple(long seed, ReleaseTerrain terrain) {
		super(seed);
		this.terrain = terrain;
	}

	@Override
	protected boolean canSpawnAt(Random random, int chunkX, int chunkZ) {
		int x = chunkX < 0 ? chunkX - (SPACING - 1) : chunkX;
		int z = chunkZ < 0 ? chunkZ - (SPACING - 1) : chunkZ;
		int regionX = x / SPACING;
		int regionZ = z / SPACING;
		Random region = regionRandom(regionX, regionZ, 14357617);
		int startX = regionX * SPACING + region.nextInt(SPACING - SEPARATION);
		int startZ = regionZ * SPACING + region.nextInt(SPACING - SEPARATION);
		return chunkX == startX && chunkZ == startZ && BIOMES.contains(terrain.biomeAt(chunkX * 16 + 8, chunkZ * 16 + 8));
	}

	@Override
	protected LegacyStart createStart(Random random, int chunkX, int chunkZ) {
		ReleaseBiome biome = terrain.biomeAt(chunkX * 16 + 8, chunkZ * 16 + 8);
		LegacyStart start = new LegacyStart() {
		};
		if (biome == ReleaseBiome.JUNGLE || biome == ReleaseBiome.JUNGLE_HILLS) {
			start.pieces.add(new JunglePyramid(random, chunkX * 16, chunkZ * 16));
		} else if (biome == ReleaseBiome.SWAMPLAND) {
			start.pieces.add(new WitchHut(random, chunkX * 16, chunkZ * 16));
		} else {
			start.pieces.add(new DesertPyramid(random, chunkX * 16, chunkZ * 16));
		}
		start.updateBox();
		return start;
	}

	/** ComponentScatteredFeature: a fixed-size piece that settles onto the average ground level the first time it is placed. */
	private abstract static class Feature extends LegacyPiece {
		protected final int width;
		protected final int height;
		protected final int length;
		private int groundLevel = -1;

		Feature(Random random, int x, int y, int z, int width, int height, int length) {
			super(0);
			this.width = width;
			this.height = height;
			this.length = length;
			this.direction = random.nextInt(4);
			box = direction == 0 || direction == 2
					? new LegacyBox(x, y, z, x + width - 1, y + height - 1, z + length - 1)
					: new LegacyBox(x, y, z, x + length - 1, y + height - 1, z + width - 1);
		}

		/** func_74935_a. */
		protected boolean adjustHeight(LegacyWorld world, LegacyBox area, int offset) {
			if (groundLevel >= 0) {
				return true;
			}
			int total = 0;
			int count = 0;
			for (int z = box.minZ; z <= box.maxZ; z++) {
				for (int x = box.minX; x <= box.maxX; x++) {
					if (area.contains(x, 64, z)) {
						total += Math.max(world.getTopSolidOrLiquidBlock(x, z), 64);
						count++;
					}
				}
			}
			if (count == 0) {
				return false;
			}
			groundLevel = total / count;
			box.offset(0, groundLevel - box.minY + offset, 0);
			return true;
		}
	}

	/** ComponentScatteredFeatureDesertPyramid. */
	private static final class DesertPyramid extends Feature {
		private final boolean[] placedChests = new boolean[4];

		DesertPyramid(Random random, int x, int z) {
			super(random, x, 64, z, 21, 15, 21);
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, -4, 0, this.width - 1, 0, this.length - 1, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
		
			for (int var4 = 1; var4 <= 9; var4++) {
				fill(world, area, var4, var4, var4, this.width - 1 - var4, var4, this.length - 1 - var4, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
				fill(world, area, var4 + 1, var4, var4 + 1, this.width - 2 - var4, var4, this.length - 2 - var4, 0, 0, false);
			}
		
			for (int var13 = 0; var13 < this.width; var13++) {
				for (int var5 = 0; var5 < this.length; var5++) {
					fillDownwards(world, LegacyBlocks.SANDSTONE, 0, var13, -5, var5, area);
				}
			}
		
			int var14 = meta(LegacyBlocks.SANDSTONE_STAIRS, 3);
			int var15 = meta(LegacyBlocks.SANDSTONE_STAIRS, 2);
			int var6 = meta(LegacyBlocks.SANDSTONE_STAIRS, 0);
			int var7 = meta(LegacyBlocks.SANDSTONE_STAIRS, 1);
			byte var8 = 1;
			byte var9 = 11;
			fill(world, area, 0, 0, 0, 4, 9, 4, LegacyBlocks.SANDSTONE, 0, false);
			fill(world, area, 1, 10, 1, 3, 10, 3, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, 2, 10, 0, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var15, 2, 10, 4, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var6, 0, 10, 2, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var7, 4, 10, 2, area);
			fill(world, area, this.width - 5, 0, 0, this.width - 1, 9, 4, LegacyBlocks.SANDSTONE, 0, false);
			fill(world, area, this.width - 4, 10, 1, this.width - 2, 10, 3, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, this.width - 3, 10, 0, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var15, this.width - 3, 10, 4, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var6, this.width - 5, 10, 2, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var7, this.width - 1, 10, 2, area);
			fill(world, area, 8, 0, 0, 12, 4, 4, LegacyBlocks.SANDSTONE, 0, false);
			fill(world, area, 9, 1, 0, 11, 3, 4, 0, 0, false);
			place(world, LegacyBlocks.SANDSTONE, 2, 9, 1, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 9, 2, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 9, 3, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 10, 3, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 11, 3, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 11, 2, 1, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 11, 1, 1, area);
			fill(world, area, 4, 1, 1, 8, 3, 3, LegacyBlocks.SANDSTONE, 0, false);
			fill(world, area, 4, 1, 2, 8, 2, 2, 0, 0, false);
			fill(world, area, 12, 1, 1, 16, 3, 3, LegacyBlocks.SANDSTONE, 0, false);
			fill(world, area, 12, 1, 2, 16, 2, 2, 0, 0, false);
			fill(world, area, 5, 4, 5, this.width - 6, 4, this.length - 6, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, 9, 4, 9, 11, 4, 11, 0, 0, false);
			fill(world, area, 8, 1, 8, 8, 3, 8, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 12, 1, 8, 12, 3, 8, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 8, 1, 12, 8, 3, 12, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 12, 1, 12, 12, 3, 12, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 1, 1, 5, 4, 4, 11, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, this.width - 5, 1, 5, this.width - 2, 4, 11, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, 6, 7, 9, 6, 7, 11, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, this.width - 7, 7, 9, this.width - 7, 7, 11, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, 5, 5, 9, 5, 7, 11, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, this.width - 6, 5, 9, this.width - 6, 7, 11, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			place(world, 0, 0, 5, 5, 10, area);
			place(world, 0, 0, 5, 6, 10, area);
			place(world, 0, 0, 6, 6, 10, area);
			place(world, 0, 0, this.width - 6, 5, 10, area);
			place(world, 0, 0, this.width - 6, 6, 10, area);
			place(world, 0, 0, this.width - 7, 6, 10, area);
			fill(world, area, 2, 4, 4, 2, 6, 4, 0, 0, false);
			fill(world, area, this.width - 3, 4, 4, this.width - 3, 6, 4, 0, 0, false);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, 2, 4, 5, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, 2, 3, 4, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, this.width - 3, 4, 5, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var14, this.width - 3, 3, 4, area);
			fill(world, area, 1, 1, 3, 2, 2, 3, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, this.width - 3, 1, 3, this.width - 2, 2, 3, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, 0, 1, 1, 2, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, 0, this.width - 2, 1, 2, area);
			place(world, LegacyBlocks.SLAB, 1, 1, 2, 2, area);
			place(world, LegacyBlocks.SLAB, 1, this.width - 2, 2, 2, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var7, 2, 1, 2, area);
			place(world, LegacyBlocks.SANDSTONE_STAIRS, var6, this.width - 3, 1, 2, area);
			fill(world, area, 4, 3, 5, 4, 3, 18, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, this.width - 5, 3, 5, this.width - 5, 3, 17, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, 3, 1, 5, 4, 2, 16, 0, 0, false);
			fill(world, area, this.width - 6, 1, 5, this.width - 5, 2, 16, 0, 0, false);
		
			for (byte var10 = 5; var10 <= 17; var10 += 2) {
				place(world, LegacyBlocks.SANDSTONE, 2, 4, 1, var10, area);
				place(world, LegacyBlocks.SANDSTONE, 1, 4, 2, var10, area);
				place(world, LegacyBlocks.SANDSTONE, 2, this.width - 5, 1, var10, area);
				place(world, LegacyBlocks.SANDSTONE, 1, this.width - 5, 2, var10, area);
			}
		
			place(world, LegacyBlocks.WOOL, var8, 10, 0, 7, area);
			place(world, LegacyBlocks.WOOL, var8, 10, 0, 8, area);
			place(world, LegacyBlocks.WOOL, var8, 9, 0, 9, area);
			place(world, LegacyBlocks.WOOL, var8, 11, 0, 9, area);
			place(world, LegacyBlocks.WOOL, var8, 8, 0, 10, area);
			place(world, LegacyBlocks.WOOL, var8, 12, 0, 10, area);
			place(world, LegacyBlocks.WOOL, var8, 7, 0, 10, area);
			place(world, LegacyBlocks.WOOL, var8, 13, 0, 10, area);
			place(world, LegacyBlocks.WOOL, var8, 9, 0, 11, area);
			place(world, LegacyBlocks.WOOL, var8, 11, 0, 11, area);
			place(world, LegacyBlocks.WOOL, var8, 10, 0, 12, area);
			place(world, LegacyBlocks.WOOL, var8, 10, 0, 13, area);
			place(world, LegacyBlocks.WOOL, var9, 10, 0, 10, area);
		
			for (int var16 = 0; var16 <= this.width - 1; var16 += this.width - 1) {
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 2, 1, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 2, 2, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 2, 3, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 3, 1, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 3, 2, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 3, 3, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 4, 1, area);
				place(world, LegacyBlocks.SANDSTONE, 1, var16, 4, 2, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 4, 3, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 5, 1, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 5, 2, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 5, 3, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 6, 1, area);
				place(world, LegacyBlocks.SANDSTONE, 1, var16, 6, 2, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 6, 3, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 7, 1, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 7, 2, area);
				place(world, LegacyBlocks.WOOL, var8, var16, 7, 3, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 8, 1, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 8, 2, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var16, 8, 3, area);
			}
		
			for (int var17 = 2; var17 <= this.width - 3; var17 += this.width - 3 - 2) {
				place(world, LegacyBlocks.SANDSTONE, 2, var17 - 1, 2, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17, 2, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 + 1, 2, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 - 1, 3, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17, 3, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 + 1, 3, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 - 1, 4, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 1, var17, 4, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 + 1, 4, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 - 1, 5, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17, 5, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 + 1, 5, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 - 1, 6, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 1, var17, 6, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 + 1, 6, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 - 1, 7, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17, 7, 0, area);
				place(world, LegacyBlocks.WOOL, var8, var17 + 1, 7, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 - 1, 8, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17, 8, 0, area);
				place(world, LegacyBlocks.SANDSTONE, 2, var17 + 1, 8, 0, area);
			}
		
			fill(world, area, 8, 4, 0, 12, 6, 0, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			place(world, 0, 0, 8, 6, 0, area);
			place(world, 0, 0, 12, 6, 0, area);
			place(world, LegacyBlocks.WOOL, var8, 9, 5, 0, area);
			place(world, LegacyBlocks.SANDSTONE, 1, 10, 5, 0, area);
			place(world, LegacyBlocks.WOOL, var8, 11, 5, 0, area);
			fill(world, area, 8, -14, 8, 12, -11, 12, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 8, -10, 8, 12, -10, 12, LegacyBlocks.SANDSTONE, 1, LegacyBlocks.SANDSTONE, 1, false);
			fill(world, area, 8, -9, 8, 12, -9, 12, LegacyBlocks.SANDSTONE, 2, LegacyBlocks.SANDSTONE, 2, false);
			fill(world, area, 8, -8, 8, 12, -1, 12, LegacyBlocks.SANDSTONE, LegacyBlocks.SANDSTONE, false);
			fill(world, area, 9, -11, 9, 11, -1, 11, 0, 0, false);
			place(world, LegacyBlocks.STONE_PRESSURE_PLATE, 0, 10, -11, 10, area);
			fill(world, area, 9, -13, 9, 11, -13, 11, LegacyBlocks.TNT, 0, false);
			place(world, 0, 0, 8, -11, 10, area);
			place(world, 0, 0, 8, -10, 10, area);
			place(world, LegacyBlocks.SANDSTONE, 1, 7, -10, 10, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 7, -11, 10, area);
			place(world, 0, 0, 12, -11, 10, area);
			place(world, 0, 0, 12, -10, 10, area);
			place(world, LegacyBlocks.SANDSTONE, 1, 13, -10, 10, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 13, -11, 10, area);
			place(world, 0, 0, 10, -11, 8, area);
			place(world, 0, 0, 10, -10, 8, area);
			place(world, LegacyBlocks.SANDSTONE, 1, 10, -10, 7, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 10, -11, 7, area);
			place(world, 0, 0, 10, -11, 12, area);
			place(world, 0, 0, 10, -10, 12, area);
			place(world, LegacyBlocks.SANDSTONE, 1, 10, -10, 13, area);
			place(world, LegacyBlocks.SANDSTONE, 2, 10, -11, 13, area);
		
			for (int var18 = 0; var18 < 4; var18++) {
				if (!placedChests[var18]) {
					int var11 = OFFSET_X[var18] * 2;
					int var12 = OFFSET_Z[var18] * 2;
					placedChests[var18] = chest(world, area, random, 10 + var11, -11, 10 + var12, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 2 + random.nextInt(5));
				}
			}
		
			return true;
		}
	}

	/** ComponentScatteredFeatureJunglePyramid. */
	private static final class JunglePyramid extends Feature {
		private static final Stones STONES = new Stones();
		private boolean placedMainChest;
		private boolean placedHiddenChest;
		private boolean placedTrap1;
		private boolean placedTrap2;

		JunglePyramid(Random random, int x, int z) {
			super(random, x, 64, z, 12, 10, 15);
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (!adjustHeight(world, area, 0)) {
				return false;
			}
		
			int var4 = meta(LegacyBlocks.COBBLESTONE_STAIRS, 3);
			int var5 = meta(LegacyBlocks.COBBLESTONE_STAIRS, 2);
			int var6 = meta(LegacyBlocks.COBBLESTONE_STAIRS, 0);
			int var7 = meta(LegacyBlocks.COBBLESTONE_STAIRS, 1);
			fill(world, area, 0, -4, 0, this.width - 1, 0, this.length - 1, false, random, STONES);
			fill(world, area, 2, 1, 2, 9, 2, 2, false, random, STONES);
			fill(world, area, 2, 1, 12, 9, 2, 12, false, random, STONES);
			fill(world, area, 2, 1, 3, 2, 2, 11, false, random, STONES);
			fill(world, area, 9, 1, 3, 9, 2, 11, false, random, STONES);
			fill(world, area, 1, 3, 1, 10, 6, 1, false, random, STONES);
			fill(world, area, 1, 3, 13, 10, 6, 13, false, random, STONES);
			fill(world, area, 1, 3, 2, 1, 6, 12, false, random, STONES);
			fill(world, area, 10, 3, 2, 10, 6, 12, false, random, STONES);
			fill(world, area, 2, 3, 2, 9, 3, 12, false, random, STONES);
			fill(world, area, 2, 6, 2, 9, 6, 12, false, random, STONES);
			fill(world, area, 3, 7, 3, 8, 7, 11, false, random, STONES);
			fill(world, area, 4, 8, 4, 7, 8, 10, false, random, STONES);
			fillAir(world, area, 3, 1, 3, 8, 2, 11);
			fillAir(world, area, 4, 3, 6, 7, 3, 9);
			fillAir(world, area, 2, 4, 2, 9, 5, 12);
			fillAir(world, area, 4, 6, 5, 7, 6, 9);
			fillAir(world, area, 5, 7, 6, 6, 7, 8);
			fillAir(world, area, 5, 1, 2, 6, 2, 2);
			fillAir(world, area, 5, 2, 12, 6, 2, 12);
			fillAir(world, area, 5, 5, 1, 6, 5, 1);
			fillAir(world, area, 5, 5, 13, 6, 5, 13);
			place(world, 0, 0, 1, 5, 5, area);
			place(world, 0, 0, 10, 5, 5, area);
			place(world, 0, 0, 1, 5, 9, area);
			place(world, 0, 0, 10, 5, 9, area);
		
			for (byte var8 = 0; var8 <= 14; var8 += 14) {
				fill(world, area, 2, 4, var8, 2, 5, var8, false, random, STONES);
				fill(world, area, 4, 4, var8, 4, 5, var8, false, random, STONES);
				fill(world, area, 7, 4, var8, 7, 5, var8, false, random, STONES);
				fill(world, area, 9, 4, var8, 9, 5, var8, false, random, STONES);
			}
		
			fill(world, area, 5, 6, 0, 6, 6, 0, false, random, STONES);
		
			for (byte var10 = 0; var10 <= 11; var10 += 11) {
				for (byte var9 = 2; var9 <= 12; var9 += 2) {
					fill(world, area, var10, 4, var9, var10, 5, var9, false, random, STONES);
				}
		
				fill(world, area, var10, 6, 5, var10, 6, 5, false, random, STONES);
				fill(world, area, var10, 6, 9, var10, 6, 9, false, random, STONES);
			}
		
			fill(world, area, 2, 7, 2, 2, 9, 2, false, random, STONES);
			fill(world, area, 9, 7, 2, 9, 9, 2, false, random, STONES);
			fill(world, area, 2, 7, 12, 2, 9, 12, false, random, STONES);
			fill(world, area, 9, 7, 12, 9, 9, 12, false, random, STONES);
			fill(world, area, 4, 9, 4, 4, 9, 4, false, random, STONES);
			fill(world, area, 7, 9, 4, 7, 9, 4, false, random, STONES);
			fill(world, area, 4, 9, 10, 4, 9, 10, false, random, STONES);
			fill(world, area, 7, 9, 10, 7, 9, 10, false, random, STONES);
			fill(world, area, 5, 9, 7, 6, 9, 7, false, random, STONES);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 5, 9, 6, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 6, 9, 6, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var5, 5, 9, 8, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var5, 6, 9, 8, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 4, 0, 0, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 5, 0, 0, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 6, 0, 0, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 7, 0, 0, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 4, 1, 8, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 4, 2, 9, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 4, 3, 10, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 7, 1, 8, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 7, 2, 9, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 7, 3, 10, area);
			fill(world, area, 4, 1, 9, 4, 1, 9, false, random, STONES);
			fill(world, area, 7, 1, 9, 7, 1, 9, false, random, STONES);
			fill(world, area, 4, 1, 10, 7, 2, 10, false, random, STONES);
			fill(world, area, 5, 4, 5, 6, 4, 5, false, random, STONES);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var6, 4, 4, 5, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, var7, 7, 4, 5, area);
		
			for (int var11 = 0; var11 < 4; var11++) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, var5, 5, 0 - var11, 6 + var11, area);
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, var5, 6, 0 - var11, 6 + var11, area);
				fillAir(world, area, 5, 0 - var11, 7 + var11, 6, 0 - var11, 9 + var11);
			}
		
			fillAir(world, area, 1, -3, 12, 10, -1, 13);
			fillAir(world, area, 1, -3, 1, 3, -1, 13);
			fillAir(world, area, 1, -3, 1, 9, -1, 5);
		
			for (byte var12 = 1; var12 <= 13; var12 += 2) {
				fill(world, area, 1, -3, var12, 1, -2, var12, false, random, STONES);
			}
		
			for (byte var13 = 2; var13 <= 12; var13 += 2) {
				fill(world, area, 1, -1, var13, 3, -1, var13, false, random, STONES);
			}
		
			fill(world, area, 2, -2, 1, 5, -2, 1, false, random, STONES);
			fill(world, area, 7, -2, 1, 9, -2, 1, false, random, STONES);
			fill(world, area, 6, -3, 1, 6, -3, 1, false, random, STONES);
			fill(world, area, 6, -1, 1, 6, -1, 1, false, random, STONES);
			place(world, LegacyBlocks.TRIPWIRE_HOOK, meta(LegacyBlocks.TRIPWIRE_HOOK, 3) | 4, 1, -3, 8, area);
			place(world, LegacyBlocks.TRIPWIRE_HOOK, meta(LegacyBlocks.TRIPWIRE_HOOK, 1) | 4, 4, -3, 8, area);
			place(world, LegacyBlocks.TRIPWIRE, 4, 2, -3, 8, area);
			place(world, LegacyBlocks.TRIPWIRE, 4, 3, -3, 8, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 7, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 6, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 5, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 4, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 3, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 2, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 5, -3, 1, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 4, -3, 1, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 3, -3, 1, area);
			if (!placedTrap1) {
				placedTrap1 = dispenser(world, area, random, 3, -2, 1, 2, ARROWS, 2);
			}
		
			place(world, LegacyBlocks.VINE, 15, 3, -2, 2, area);
			place(world, LegacyBlocks.TRIPWIRE_HOOK, meta(LegacyBlocks.TRIPWIRE_HOOK, 2) | 4, 7, -3, 1, area);
			place(world, LegacyBlocks.TRIPWIRE_HOOK, meta(LegacyBlocks.TRIPWIRE_HOOK, 0) | 4, 7, -3, 5, area);
			place(world, LegacyBlocks.TRIPWIRE, 4, 7, -3, 2, area);
			place(world, LegacyBlocks.TRIPWIRE, 4, 7, -3, 3, area);
			place(world, LegacyBlocks.TRIPWIRE, 4, 7, -3, 4, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 8, -3, 6, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 9, -3, 6, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 9, -3, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 9, -3, 4, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 9, -2, 4, area);
			if (!placedTrap2) {
				placedTrap2 = dispenser(world, area, random, 9, -2, 3, 4, ARROWS, 2);
			}
		
			place(world, LegacyBlocks.VINE, 15, 8, -1, 3, area);
			place(world, LegacyBlocks.VINE, 15, 8, -2, 3, area);
			if (!placedMainChest) {
				placedMainChest = chest(world, area, random, 8, -3, 3, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 2 + random.nextInt(5));
			}
		
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 9, -3, 2, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 8, -3, 1, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 4, -3, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 5, -2, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 5, -1, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 6, -3, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 7, -2, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 7, -1, 5, area);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 8, -3, 5, area);
			fill(world, area, 9, -1, 1, 9, -1, 5, false, random, STONES);
			fillAir(world, area, 8, -3, 8, 10, -1, 10);
			place(world, LegacyBlocks.STONE_BRICKS, 3, 8, -2, 11, area);
			place(world, LegacyBlocks.STONE_BRICKS, 3, 9, -2, 11, area);
			place(world, LegacyBlocks.STONE_BRICKS, 3, 10, -2, 11, area);
			place(world, LegacyBlocks.LEVER, leverMeta(meta(LegacyBlocks.LEVER, 2)), 8, -2, 12, area);
			place(world, LegacyBlocks.LEVER, leverMeta(meta(LegacyBlocks.LEVER, 2)), 9, -2, 12, area);
			place(world, LegacyBlocks.LEVER, leverMeta(meta(LegacyBlocks.LEVER, 2)), 10, -2, 12, area);
			fill(world, area, 8, -3, 8, 8, -3, 10, false, random, STONES);
			fill(world, area, 10, -3, 8, 10, -3, 10, false, random, STONES);
			place(world, LegacyBlocks.MOSSY_COBBLESTONE, 0, 10, -2, 9, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 8, -2, 9, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 8, -2, 10, area);
			place(world, LegacyBlocks.REDSTONE_WIRE, 0, 10, -1, 9, area);
			place(world, LegacyBlocks.STICKY_PISTON, 1, 9, -2, 8, area);
			place(world, LegacyBlocks.STICKY_PISTON, meta(LegacyBlocks.STICKY_PISTON, 4), 10, -2, 8, area);
			place(world, LegacyBlocks.STICKY_PISTON, meta(LegacyBlocks.STICKY_PISTON, 4), 10, -1, 8, area);
			place(world, LegacyBlocks.REPEATER, meta(LegacyBlocks.REPEATER, 2), 10, -2, 10, area);
			if (!placedHiddenChest) {
				placedHiddenChest = chest(world, area, random, 9, -3, 10, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 2 + random.nextInt(5));
			}
		
			return true;
		}
	}

	/** BlockLever.invertMetadata: the lever metadata for a lever on the given side of a block. */
	private static int leverMeta(int side) {
		return switch (side) {
			case 0 -> 0;
			case 1 -> 5;
			case 2 -> 4;
			case 3 -> 3;
			case 4 -> 2;
			case 5 -> 1;
			default -> -1;
		};
	}

	/** The jungle temple's mix of cobblestone and mossy cobblestone. */
	private static final class Stones extends LegacyPiece.BlockSelector {
		@Override
		public void select(Random random, int x, int y, int z, boolean edge) {
			id = random.nextFloat() < 0.4F ? LegacyBlocks.COBBLESTONE : LegacyBlocks.MOSSY_COBBLESTONE;
		}
	}

	/** ComponentScatteredFeatureSwampHut. */
	private static final class WitchHut extends Feature {
		private boolean hasWitch;

		WitchHut(Random random, int x, int z) {
			super(random, x, 64, z, 7, 5, 9);
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (!adjustHeight(world, area, 0)) {
				return false;
			}
		
			fill(world, area, 1, 1, 1, 5, 1, 7, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 1, 4, 2, 5, 4, 7, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 2, 1, 0, 4, 1, 0, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 2, 2, 2, 3, 3, 2, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 1, 2, 3, 1, 3, 6, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 5, 2, 3, 5, 3, 6, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 2, 2, 7, 4, 3, 7, LegacyBlocks.PLANKS, 1, LegacyBlocks.PLANKS, 1, false);
			fill(world, area, 1, 0, 2, 1, 3, 2, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 5, 0, 2, 5, 3, 2, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 0, 7, 1, 3, 7, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 5, 0, 7, 5, 3, 7, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			place(world, LegacyBlocks.FENCE, 0, 2, 3, 2, area);
			place(world, LegacyBlocks.FENCE, 0, 3, 3, 7, area);
			place(world, 0, 0, 1, 3, 4, area);
			place(world, 0, 0, 5, 3, 4, area);
			place(world, 0, 0, 5, 3, 5, area);
			place(world, LegacyBlocks.FLOWER_POT, 7, 1, 3, 5, area);
			place(world, LegacyBlocks.CRAFTING_TABLE, 0, 3, 2, 6, area);
			place(world, LegacyBlocks.CAULDRON, 0, 4, 2, 6, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 2, 1, area);
			place(world, LegacyBlocks.FENCE, 0, 5, 2, 1, area);
			int var4 = meta(LegacyBlocks.OAK_STAIRS, 3);
			int var5 = meta(LegacyBlocks.OAK_STAIRS, 1);
			int var6 = meta(LegacyBlocks.OAK_STAIRS, 0);
			int var7 = meta(LegacyBlocks.OAK_STAIRS, 2);
			fill(world, area, 0, 4, 1, 6, 4, 1, LegacyBlocks.SPRUCE_STAIRS, var4, LegacyBlocks.SPRUCE_STAIRS, var4, false);
			fill(world, area, 0, 4, 2, 0, 4, 7, LegacyBlocks.SPRUCE_STAIRS, var6, LegacyBlocks.SPRUCE_STAIRS, var6, false);
			fill(world, area, 6, 4, 2, 6, 4, 7, LegacyBlocks.SPRUCE_STAIRS, var5, LegacyBlocks.SPRUCE_STAIRS, var5, false);
			fill(world, area, 0, 4, 8, 6, 4, 8, LegacyBlocks.SPRUCE_STAIRS, var7, LegacyBlocks.SPRUCE_STAIRS, var7, false);
		
			for (byte var8 = 2; var8 <= 7; var8 += 5) {
				for (byte var9 = 1; var9 <= 5; var9 += 4) {
					fillDownwards(world, LegacyBlocks.LOG, 0, var9, -1, var8, area);
				}
			}
		
			if (!hasWitch) {
				int var12 = worldX(2, 5);
				int var13 = worldY(2);
				int var10 = worldZ(2, 5);
				if (area.contains(var12, var13, var10)) {
					hasWitch = true;
					Witch witch = EntityTypes.WITCH.create(world.level().getLevel(), EntitySpawnReason.STRUCTURE);
					if (witch != null) {
						witch.snapTo(var12 + 0.5, var13, var10 + 0.5, 0.0F, 0.0F);
						witch.setPersistenceRequired();
						world.addEntity(witch);
					}
				}
			}
		
			return true;
		}
	}
}
