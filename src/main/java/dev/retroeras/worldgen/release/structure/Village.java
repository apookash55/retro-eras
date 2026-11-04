package dev.retroeras.worldgen.release.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.retroeras.worldgen.release.ReleaseBiome;
import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;

/** 1.6.4's villages (MapGenVillage, StructureVillagePieces), in plains and deserts. */
public final class Village extends LegacyStructureFeature {
	private static final Set<ReleaseBiome> BIOMES = Set.of(ReleaseBiome.PLAINS, ReleaseBiome.DESERT);
	private static final int SPACING = 32;
	private static final int SEPARATION = 8;
	/** 1.6.4's professions: farmer, librarian, priest, blacksmith and butcher. */
	private static final List<ResourceKey<VillagerProfession>> PROFESSIONS = List.of(VillagerProfession.FARMER, VillagerProfession.LIBRARIAN,
			VillagerProfession.CLERIC, VillagerProfession.ARMORER, VillagerProfession.BUTCHER);

	private final ReleaseTerrain terrain;

	public Village(long seed, ReleaseTerrain terrain) {
		super(seed);
		this.terrain = terrain;
	}

	@Override
	protected boolean canSpawnAt(Random random, int chunkX, int chunkZ) {
		int x = chunkX < 0 ? chunkX - (SPACING - 1) : chunkX;
		int z = chunkZ < 0 ? chunkZ - (SPACING - 1) : chunkZ;
		int regionX = x / SPACING;
		int regionZ = z / SPACING;
		Random region = regionRandom(regionX, regionZ, 10387312);
		int startX = regionX * SPACING + region.nextInt(SPACING - SEPARATION);
		int startZ = regionZ * SPACING + region.nextInt(SPACING - SEPARATION);
		return chunkX == startX && chunkZ == startZ && terrain.areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 0, BIOMES);
	}

	@Override
	protected LegacyStart createStart(Random random, int chunkX, int chunkZ) {
		return new VillageStart(terrain, random, chunkX, chunkZ, 0);
	}

	/** MathHelper.getRandomIntegerInRange. */
	private static int nextIntBetween(Random random, int min, int max) {
		return min >= max ? min : random.nextInt(max - min + 1) + min;
	}

	/** StructureVillageStart: a village is only kept if it has more than two buildings. */
	private static final class VillageStart extends LegacyStart {
		private final boolean valid;

		VillageStart(ReleaseTerrain terrain, Random random, int chunkX, int chunkZ, int size) {
			List<PieceWeight> weights = createWeights(random, size);
			Start start = new Start(terrain, 0, random, (chunkX << 4) + 2, (chunkZ << 4) + 2, weights, size);
			pieces.add(start);
			start.addChildren(start, pieces, random);
			List<LegacyPiece> roads = start.pendingRoads;
			List<LegacyPiece> houses = start.pendingHouses;
			while (!roads.isEmpty() || !houses.isEmpty()) {
				if (roads.isEmpty()) {
					houses.remove(random.nextInt(houses.size())).addChildren(start, pieces, random);
				} else {
					roads.remove(random.nextInt(roads.size())).addChildren(start, pieces, random);
				}
			}
			updateBox();
			int buildings = 0;
			for (LegacyPiece piece : pieces) {
				if (!(piece instanceof RoadPiece)) {
					buildings++;
				}
			}
			valid = buildings > 2;
		}

		@Override
		public boolean isValid() {
			return valid;
		}
	}

	/** StructureVillagePieceWeight: how likely a building is, and how many a village may have. */
	private static final class PieceWeight {
		final Class<? extends VillagePiece> type;
		final int weight;
		int placed;
		final int limit;

		PieceWeight(Class<? extends VillagePiece> type, int weight, int limit) {
			this.type = type;
			this.weight = weight;
			this.limit = limit;
		}

		boolean canSpawnMore() {
			return limit == 0 || placed < limit;
		}
	}

	/** getStructureVillageWeightedPieceList. */
	private static List<PieceWeight> createWeights(Random random, int size) {
		List<PieceWeight> weights = new ArrayList<>();
		weights.add(new PieceWeight(SmallHouse.class, 4, nextIntBetween(random, 2 + size, 4 + size * 2)));
		weights.add(new PieceWeight(Church.class, 20, nextIntBetween(random, size, 1 + size)));
		weights.add(new PieceWeight(Library.class, 20, nextIntBetween(random, size, 2 + size)));
		weights.add(new PieceWeight(Hut.class, 3, nextIntBetween(random, 2 + size, 5 + size * 3)));
		weights.add(new PieceWeight(Butcher.class, 15, nextIntBetween(random, size, 2 + size)));
		weights.add(new PieceWeight(DoubleFarm.class, 3, nextIntBetween(random, 1 + size, 4 + size)));
		weights.add(new PieceWeight(Farm.class, 3, nextIntBetween(random, 2 + size, 4 + size * 2)));
		weights.add(new PieceWeight(Blacksmith.class, 15, nextIntBetween(random, 0, 1 + size)));
		weights.add(new PieceWeight(BigHouse.class, 8, nextIntBetween(random, size, 3 + size * 2)));
		Iterator<PieceWeight> iterator = weights.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().limit == 0) {
				iterator.remove();
			}
		}
		return weights;
	}

	private static int totalWeight(List<PieceWeight> weights) {
		boolean any = false;
		int total = 0;
		for (PieceWeight weight : weights) {
			if (weight.limit > 0 && weight.placed < weight.limit) {
				any = true;
			}
			total += weight.weight;
		}
		return any ? total : -1;
	}

	private static @Nullable VillagePiece create(Start start, PieceWeight weight, List<LegacyPiece> pieces, Random random, int x, int y, int z,
			int direction, int depth) {
		Class<? extends VillagePiece> type = weight.type;
		if (type == SmallHouse.class) {
			return SmallHouse.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Church.class) {
			return Church.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Library.class) {
			return Library.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Hut.class) {
			return Hut.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Butcher.class) {
			return Butcher.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == DoubleFarm.class) {
			return DoubleFarm.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Farm.class) {
			return Farm.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == Blacksmith.class) {
			return Blacksmith.create(start, pieces, random, x, y, z, direction, depth);
		} else if (type == BigHouse.class) {
			return BigHouse.create(start, pieces, random, x, y, z, direction, depth);
		}
		return null;
	}

	/** getNextVillageComponent: a weighted random building, or a lamp post if none fits. */
	private static @Nullable VillagePiece nextBuilding(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction,
			int depth) {
		int total = totalWeight(start.weights);
		if (total <= 0) {
			return null;
		}
		for (int attempt = 0; attempt < 5; attempt++) {
			int roll = random.nextInt(total);
			for (PieceWeight weight : start.weights) {
				roll -= weight.weight;
				if (roll < 0) {
					if (!weight.canSpawnMore() || weight == start.lastWeight && start.weights.size() > 1) {
						break;
					}
					VillagePiece piece = create(start, weight, pieces, random, x, y, z, direction, depth);
					if (piece != null) {
						weight.placed++;
						start.lastWeight = weight;
						if (!weight.canSpawnMore()) {
							start.weights.remove(weight);
						}
						return piece;
					}
				}
			}
		}
		LegacyBox box = Lamp.create(start, pieces, random, x, y, z, direction);
		return box != null ? new Lamp(start, depth, random, box, direction) : null;
	}

	/** getNextVillageStructureComponent. */
	private static @Nullable LegacyPiece nextHouse(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth) {
		if (depth > 50) {
			return null;
		}
		if (Math.abs(x - start.box().minX) <= 112 && Math.abs(z - start.box().minZ) <= 112) {
			VillagePiece piece = nextBuilding(start, pieces, random, x, y, z, direction, depth + 1);
			if (piece != null && start.fitsBiome(piece.box())) {
				pieces.add(piece);
				start.pendingHouses.add(piece);
				return piece;
			}
		}
		return null;
	}

	/** getNextComponentVillagePath. */
	private static @Nullable LegacyPiece nextPath(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth) {
		if (depth > 3 + start.size) {
			return null;
		}
		if (Math.abs(x - start.box().minX) <= 112 && Math.abs(z - start.box().minZ) <= 112) {
			LegacyBox box = Road.create(start, pieces, random, x, y, z, direction);
			if (box != null && box.minY > 10) {
				Road road = new Road(start, depth, random, box, direction);
				if (start.fitsBiome(road.box())) {
					pieces.add(road);
					start.pendingRoads.add(road);
					return road;
				}
			}
		}
		return null;
	}

	/** ComponentVillage: a village piece that settles onto the ground and, in deserts, swaps wood and cobblestone for sandstone. */
	private abstract static class VillagePiece extends LegacyPiece {
		protected int averageGroundLevel = -1;
		private int villagersSpawned;
		private boolean inDesert;

		protected VillagePiece(@Nullable Start start, int depth) {
			super(depth);
			if (start != null) {
				inDesert = start.inDesert;
			}
		}

		/** getNextComponentNN: a piece off this one's negative side. */
		protected @Nullable LegacyPiece nextNegative(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset) {
			return switch (direction) {
				case 0, 2 -> nextHouse((Start) start, pieces, random, box.minX - 1, box.minY + dy, box.minZ + offset, 1, depth());
				case 1, 3 -> nextHouse((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.minZ - 1, 2, depth());
				default -> null;
			};
		}

		/** getNextComponentPP: a piece off this one's positive side. */
		protected @Nullable LegacyPiece nextPositive(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset) {
			return switch (direction) {
				case 0, 2 -> nextHouse((Start) start, pieces, random, box.maxX + 1, box.minY + dy, box.minZ + offset, 3, depth());
				case 1, 3 -> nextHouse((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.maxZ + 1, 0, depth());
				default -> null;
			};
		}

		/** getAverageGroundLevel, over the part of the piece inside {@code area}; -1 if none of it is. */
		protected int groundLevel(LegacyWorld world, LegacyBox area) {
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
			return count == 0 ? -1 : total / count;
		}

		protected static boolean isAboveGround(@Nullable LegacyBox box) {
			return box != null && box.minY > 10;
		}

		/** spawnVillagers: up to {@code count} villagers in a row, each only once. */
		protected void spawnVillagers(LegacyWorld world, LegacyBox area, int x, int y, int z, int count) {
			for (int i = villagersSpawned; i < count; i++) {
				int wx = worldX(x + i, z);
				int wy = worldY(y);
				int wz = worldZ(x + i, z);
				if (!area.contains(wx, wy, wz)) {
					break;
				}
				villagersSpawned++;
				Villager villager = EntityTypes.VILLAGER.create(world.level().getLevel(), EntitySpawnReason.STRUCTURE);
				if (villager != null) {
					villager.snapTo(wx + 0.5, wy, wz + 0.5, 0.0F, 0.0F);
					villager.setVillagerData(villager.getVillagerData().withProfession(world.level().registryAccess(),
							PROFESSIONS.get(profession(i))));
					// With some experience they keep their trade without a job site block, which 1.6.4 did not have.
					villager.setVillagerXp(1);
					world.addEntity(villager);
				}
			}
		}

		/** getVillagerType: the profession of the {@code index}th villager. */
		protected int profession(int index) {
			return 0;
		}

		/** getBiomeSpecificBlock. */
		protected int biomeBlock(int id, int meta) {
			if (inDesert) {
				if (id == LegacyBlocks.LOG || id == LegacyBlocks.COBBLESTONE || id == LegacyBlocks.PLANKS || id == LegacyBlocks.GRAVEL) {
					return LegacyBlocks.SANDSTONE;
				}
				if (id == LegacyBlocks.OAK_STAIRS || id == LegacyBlocks.COBBLESTONE_STAIRS) {
					return LegacyBlocks.SANDSTONE_STAIRS;
				}
			}
			return id;
		}

		/** getBiomeSpecificBlockMetadata. */
		protected int biomeMeta(int id, int meta) {
			if (inDesert) {
				if (id == LegacyBlocks.LOG || id == LegacyBlocks.COBBLESTONE) {
					return 0;
				}
				if (id == LegacyBlocks.PLANKS) {
					return 2;
				}
			}
			return meta;
		}

		@Override
		protected void place(LegacyWorld world, int id, int meta, int x, int y, int z, LegacyBox area) {
			super.place(world, biomeBlock(id, meta), biomeMeta(id, meta), x, y, z, area);
		}

		@Override
		protected void fillDownwards(LegacyWorld world, int id, int meta, int x, int y, int z, LegacyBox area) {
			super.fillDownwards(world, biomeBlock(id, meta), biomeMeta(id, meta), x, y, z, area);
		}
	}

	/** A road piece, which does not count as a building. */
	private abstract static class RoadPiece extends VillagePiece {
		protected RoadPiece(@Nullable Start start, int depth) {
			super(start, depth);
		}
	}

	/** ComponentVillageStartPiece: the well, plus what the village needs while it is laid out. */
	private static final class Start extends Well {
		private final ReleaseTerrain terrain;
		final boolean inDesert;
		final int size;
		@Nullable PieceWeight lastWeight;
		final List<PieceWeight> weights;
		final List<LegacyPiece> pendingHouses = new ArrayList<>();
		final List<LegacyPiece> pendingRoads = new ArrayList<>();

		Start(ReleaseTerrain terrain, int depth, Random random, int x, int z, List<PieceWeight> weights, int size) {
			super(null, 0, random, x, z);
			this.terrain = terrain;
			this.weights = weights;
			this.size = size;
			ReleaseBiome biome = terrain.biomeAt(x, z);
			this.inDesert = biome == ReleaseBiome.DESERT || biome == ReleaseBiome.DESERT_HILLS;
		}

		/** Whether every biome under a new piece, and a margin round it, allows villages. */
		boolean fitsBiome(LegacyBox box) {
			int centerX = (box.minX + box.maxX) / 2;
			int centerZ = (box.minZ + box.maxZ) / 2;
			int span = Math.max(box.maxX - box.minX, box.maxZ - box.minZ);
			return terrain.areBiomesViable(centerX, centerZ, span / 2 + 4, BIOMES);
		}
	}


	/** ComponentVillageWell: the well every village starts from. */
	private static class Well extends VillagePiece {

		public Well(Start var1, int var2, Random var3, int var4, int var5) {
			super(var1, var2);
			this.direction = var3.nextInt(4);
			switch (this.direction) {
				case 0:
				case 2:
					this.box = new LegacyBox(var4, 64, var5, var4 + 6 - 1, 78, var5 + 6 - 1);
					break;
				default:
					this.box = new LegacyBox(var4, 64, var5, var4 + 6 - 1, 78, var5 + 6 - 1);
			}
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextPath((Start)var1, var2, var3, this.box.minX - 1, this.box.maxY - 4, this.box.minZ + 1, 1, depth());
			nextPath((Start)var1, var2, var3, this.box.maxX + 1, this.box.maxY - 4, this.box.minZ + 1, 3, depth());
			nextPath((Start)var1, var2, var3, this.box.minX + 1, this.box.maxY - 4, this.box.minZ - 1, 2, depth());
			nextPath((Start)var1, var2, var3, this.box.minX + 1, this.box.maxY - 4, this.box.maxZ + 1, 0, depth());
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 3, 0);
			}

			fill(world, area, 1, 0, 1, 4, 12, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.FLOWING_WATER, false);
			place(world, 0, 0, 2, 12, 2, area);
			place(world, 0, 0, 3, 12, 2, area);
			place(world, 0, 0, 2, 12, 3, area);
			place(world, 0, 0, 3, 12, 3, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 13, 1, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 14, 1, area);
			place(world, LegacyBlocks.FENCE, 0, 4, 13, 1, area);
			place(world, LegacyBlocks.FENCE, 0, 4, 14, 1, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 13, 4, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 14, 4, area);
			place(world, LegacyBlocks.FENCE, 0, 4, 13, 4, area);
			place(world, LegacyBlocks.FENCE, 0, 4, 14, 4, area);
			fill(world, area, 1, 15, 1, 4, 15, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);

			for (int var4 = 0; var4 <= 5; var4++) {
				for (int var5 = 0; var5 <= 5; var5++) {
					if (var5 == 0 || var5 == 5 || var4 == 0 || var4 == 5) {
						place(world, LegacyBlocks.GRAVEL, 0, var5, 11, var4, area);
						clearUpwards(world, var5, 12, var4, area);
					}
				}
			}

			return true;
		}
	}


	/** ComponentVillagePathGen: a gravel road with buildings and more roads leading off it. */
	private static final class Road extends RoadPiece {
		private int a;

		public Road(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
			this.a = Math.max(var4.xSpan(), var4.zSpan());
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			boolean var4 = false;

			for (int var5 = var3.nextInt(5); var5 < this.a - 8; var5 += 2 + var3.nextInt(5)) {
				LegacyPiece var6 = nextNegative(var1, var2, var3, 0, var5);
				if (var6 != null) {
					var5 += Math.max(var6.box().xSpan(), var6.box().zSpan());
					var4 = true;
				}
			}

			for (int var7 = var3.nextInt(5); var7 < this.a - 8; var7 += 2 + var3.nextInt(5)) {
				LegacyPiece var8 = nextPositive(var1, var2, var3, 0, var7);
				if (var8 != null) {
					var7 += Math.max(var8.box().xSpan(), var8.box().zSpan());
					var4 = true;
				}
			}

			if (var4 && var3.nextInt(3) > 0) {
				switch (this.direction) {
					case 0:
						nextPath((Start)var1, var2, var3, this.box.minX - 1, this.box.minY, this.box.maxZ - 2, 1, depth());
						break;
					case 1:
						nextPath((Start)var1, var2, var3, this.box.minX, this.box.minY, this.box.minZ - 1, 2, depth());
						break;
					case 2:
						nextPath((Start)var1, var2, var3, this.box.minX - 1, this.box.minY, this.box.minZ, 1, depth());
						break;
					case 3:
						nextPath((Start)var1, var2, var3, this.box.maxX - 2, this.box.minY, this.box.minZ - 1, 2, depth());
				}
			}

			if (var4 && var3.nextInt(3) > 0) {
				switch (this.direction) {
					case 0:
						nextPath((Start)var1, var2, var3, this.box.maxX + 1, this.box.minY, this.box.maxZ - 2, 3, depth());
						break;
					case 1:
						nextPath((Start)var1, var2, var3, this.box.minX, this.box.minY, this.box.maxZ + 1, 0, depth());
						break;
					case 2:
						nextPath((Start)var1, var2, var3, this.box.maxX + 1, this.box.minY, this.box.minZ, 3, depth());
						break;
					case 3:
						nextPath((Start)var1, var2, var3, this.box.maxX - 2, this.box.minY, this.box.maxZ + 1, 0, depth());
				}
			}
		}

		static LegacyBox create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6) {
			for (int var7 = 7 * nextIntBetween(var2, 3, 5); var7 >= 7; var7 -= 7) {
				LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 3, 3, var7, var6);
				if (LegacyPiece.findIntersecting(var1, var8) == null) {
					return var8;
				}
			}

			return null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			int var4 = biomeBlock(LegacyBlocks.GRAVEL, 0);

			for (int var5 = this.box.minX; var5 <= this.box.maxX; var5++) {
				for (int var6 = this.box.minZ; var6 <= this.box.maxZ; var6++) {
					if (area.contains(var5, 64, var6)) {
						int var7 = world.getTopSolidOrLiquidBlock(var5, var6) - 1;
						world.setBlock(var5, var7, var6, var4, 0);
					}
				}
			}

			return true;
		}
	}


	/** ComponentVillageTorch: a lamp post. */
	private static final class Lamp extends VillagePiece {

		public Lamp(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static LegacyBox create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 3, 4, 2, var6);
			return LegacyPiece.findIntersecting(var1, var7) != null ? null : var7;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 4 - 1, 0);
			}

			fill(world, area, 0, 0, 0, 2, 3, 1, 0, 0, false);
			place(world, LegacyBlocks.FENCE, 0, 1, 0, 0, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 1, 0, area);
			place(world, LegacyBlocks.FENCE, 0, 1, 2, 0, area);
			place(world, LegacyBlocks.WOOL, 15, 1, 3, 0, area);
			place(world, LegacyBlocks.TORCH, 0, 0, 3, 0, area);
			place(world, LegacyBlocks.TORCH, 0, 1, 3, 1, area);
			place(world, LegacyBlocks.TORCH, 0, 2, 3, 0, area);
			place(world, LegacyBlocks.TORCH, 0, 1, 3, -1, area);
			return true;
		}
	}


	/** ComponentVillageHouse4_Garden: a small house, sometimes with a roof terrace. */
	private static final class SmallHouse extends VillagePiece {
		private boolean a;

		public SmallHouse(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
			this.a = var3.nextBoolean();
		}

		static SmallHouse create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 5, 6, 5, var6);
			return LegacyPiece.findIntersecting(var1, var8) != null ? null : new SmallHouse(var0, var7, var2, var8, var6);
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 6 - 1, 0);
			}

			fill(world, area, 0, 0, 0, 4, 0, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 4, 0, 4, 4, 4, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 4, 1, 3, 4, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 1, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 2, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 3, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 1, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 2, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 3, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 1, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 2, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 3, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 1, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 2, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 3, 4, area);
			fill(world, area, 0, 1, 1, 0, 3, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 4, 1, 1, 4, 3, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 1, 4, 3, 3, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 2, 2, area);
			place(world, LegacyBlocks.PLANKS, 0, 1, 1, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 1, 2, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 1, 3, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 2, 3, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 3, 3, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 3, 2, 0, area);
			place(world, LegacyBlocks.PLANKS, 0, 3, 1, 0, area);
			if (getBlock(world, 2, 0, -1, area) == 0 && getBlock(world, 2, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 2, 0, -1, area);
			}

			fill(world, area, 1, 1, 1, 3, 3, 3, 0, 0, false);
			if (this.a) {
				place(world, LegacyBlocks.FENCE, 0, 0, 5, 0, area);
				place(world, LegacyBlocks.FENCE, 0, 1, 5, 0, area);
				place(world, LegacyBlocks.FENCE, 0, 2, 5, 0, area);
				place(world, LegacyBlocks.FENCE, 0, 3, 5, 0, area);
				place(world, LegacyBlocks.FENCE, 0, 4, 5, 0, area);
				place(world, LegacyBlocks.FENCE, 0, 0, 5, 4, area);
				place(world, LegacyBlocks.FENCE, 0, 1, 5, 4, area);
				place(world, LegacyBlocks.FENCE, 0, 2, 5, 4, area);
				place(world, LegacyBlocks.FENCE, 0, 3, 5, 4, area);
				place(world, LegacyBlocks.FENCE, 0, 4, 5, 4, area);
				place(world, LegacyBlocks.FENCE, 0, 4, 5, 1, area);
				place(world, LegacyBlocks.FENCE, 0, 4, 5, 2, area);
				place(world, LegacyBlocks.FENCE, 0, 4, 5, 3, area);
				place(world, LegacyBlocks.FENCE, 0, 0, 5, 1, area);
				place(world, LegacyBlocks.FENCE, 0, 0, 5, 2, area);
				place(world, LegacyBlocks.FENCE, 0, 0, 5, 3, area);
			}

			if (this.a) {
				int var4 = meta(LegacyBlocks.LADDER, 3);
				place(world, LegacyBlocks.LADDER, var4, 3, 1, 3, area);
				place(world, LegacyBlocks.LADDER, var4, 3, 2, 3, area);
				place(world, LegacyBlocks.LADDER, var4, 3, 3, 3, area);
				place(world, LegacyBlocks.LADDER, var4, 3, 4, 3, area);
			}

			place(world, LegacyBlocks.TORCH, 0, 2, 3, 1, area);

			for (int var6 = 0; var6 < 5; var6++) {
				for (int var5 = 0; var5 < 5; var5++) {
					clearUpwards(world, var5, 6, var6, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var5, -1, var6, area);
				}
			}

			spawnVillagers(world, area, 1, 1, 2, 1);
			return true;
		}
	}


	/** ComponentVillageChurch. */
	private static final class Church extends VillagePiece {

		public Church(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static Church create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 5, 12, 9, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Church(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 12 - 1, 0);
			}

			fill(world, area, 1, 1, 1, 3, 3, 7, 0, 0, false);
			fill(world, area, 1, 5, 1, 3, 9, 3, 0, 0, false);
			fill(world, area, 1, 0, 0, 3, 0, 8, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 1, 0, 3, 10, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 1, 1, 0, 10, 3, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 4, 1, 1, 4, 10, 3, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 0, 4, 0, 4, 7, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 4, 0, 4, 4, 4, 7, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 1, 8, 3, 4, 8, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 5, 4, 3, 10, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 5, 5, 3, 5, 7, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 9, 0, 4, 9, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 4, 0, 4, 4, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			place(world, LegacyBlocks.COBBLESTONE, 0, 0, 11, 2, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 4, 11, 2, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 2, 11, 0, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 2, 11, 4, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 1, 1, 6, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 1, 1, 7, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 2, 1, 7, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 3, 1, 6, area);
			place(world, LegacyBlocks.COBBLESTONE, 0, 3, 1, 7, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 1, 1, 5, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 2, 1, 6, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 3, 1, 5, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 1), 1, 2, 7, area);
			place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 0), 3, 2, 7, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 3, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 3, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 6, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 7, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 6, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 7, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 6, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 7, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 6, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 7, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 3, 6, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 3, 6, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 3, 8, area);
			place(world, LegacyBlocks.TORCH, 0, 2, 4, 7, area);
			place(world, LegacyBlocks.TORCH, 0, 1, 4, 6, area);
			place(world, LegacyBlocks.TORCH, 0, 3, 4, 6, area);
			place(world, LegacyBlocks.TORCH, 0, 2, 4, 5, area);
			int var4 = meta(LegacyBlocks.LADDER, 4);

			for (int var5 = 1; var5 <= 9; var5++) {
				place(world, LegacyBlocks.LADDER, var4, 3, var5, 3, area);
			}

			place(world, 0, 0, 2, 1, 0, area);
			place(world, 0, 0, 2, 2, 0, area);
			door(world, area, random, 2, 1, 0, meta(LegacyBlocks.OAK_DOOR, 1));
			if (getBlock(world, 2, 0, -1, area) == 0 && getBlock(world, 2, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 2, 0, -1, area);
			}

			for (int var7 = 0; var7 < 9; var7++) {
				for (int var6 = 0; var6 < 5; var6++) {
					clearUpwards(world, var6, 12, var7, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var6, -1, var7, area);
				}
			}

			spawnVillagers(world, area, 2, 1, 2, 1);
			return true;
		}

		@Override
		protected int profession(int var1) {
			return 2;
		}
	}


	/** ComponentVillageHouse1: the library. */
	private static final class Library extends VillagePiece {

		public Library(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static Library create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 9, 9, 6, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Library(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 9 - 1, 0);
			}

			fill(world, area, 1, 1, 1, 7, 5, 4, 0, 0, false);
			fill(world, area, 0, 0, 0, 8, 0, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 5, 0, 8, 5, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 6, 1, 8, 6, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 7, 2, 8, 7, 3, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			int var4 = meta(LegacyBlocks.OAK_STAIRS, 3);
			int var5 = meta(LegacyBlocks.OAK_STAIRS, 2);

			for (int var6 = -1; var6 <= 2; var6++) {
				for (int var7 = 0; var7 <= 8; var7++) {
					place(world, LegacyBlocks.OAK_STAIRS, var4, var7, 6 + var6, var6, area);
					place(world, LegacyBlocks.OAK_STAIRS, var5, var7, 6 + var6, 5 - var6, area);
				}
			}

			fill(world, area, 0, 1, 0, 0, 1, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 1, 5, 8, 1, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 8, 1, 0, 8, 1, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 2, 1, 0, 7, 1, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 2, 0, 0, 4, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 2, 5, 0, 4, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 8, 2, 5, 8, 4, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 8, 2, 0, 8, 4, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 2, 1, 0, 4, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 2, 5, 7, 4, 5, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 8, 2, 1, 8, 4, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 2, 0, 7, 4, 0, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 2, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 2, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 6, 2, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 3, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 3, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 6, 3, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 3, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 3, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 3, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 3, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 5, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 3, 2, 5, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 2, 5, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 6, 2, 5, area);
			fill(world, area, 1, 4, 1, 7, 4, 1, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 4, 4, 7, 4, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 3, 4, 7, 3, 4, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
			place(world, LegacyBlocks.PLANKS, 0, 7, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, meta(LegacyBlocks.OAK_STAIRS, 0), 7, 1, 3, area);
			int var9 = meta(LegacyBlocks.OAK_STAIRS, 3);
			place(world, LegacyBlocks.OAK_STAIRS, var9, 6, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, var9, 5, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, var9, 4, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, var9, 3, 1, 4, area);
			place(world, LegacyBlocks.FENCE, 0, 6, 1, 3, area);
			place(world, LegacyBlocks.WOODEN_PRESSURE_PLATE, 0, 6, 2, 3, area);
			place(world, LegacyBlocks.FENCE, 0, 4, 1, 3, area);
			place(world, LegacyBlocks.WOODEN_PRESSURE_PLATE, 0, 4, 2, 3, area);
			place(world, LegacyBlocks.CRAFTING_TABLE, 0, 7, 1, 1, area);
			place(world, 0, 0, 1, 1, 0, area);
			place(world, 0, 0, 1, 2, 0, area);
			door(world, area, random, 1, 1, 0, meta(LegacyBlocks.OAK_DOOR, 1));
			if (getBlock(world, 1, 0, -1, area) == 0 && getBlock(world, 1, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 1, 0, -1, area);
			}

			for (int var10 = 0; var10 < 6; var10++) {
				for (int var8 = 0; var8 < 9; var8++) {
					clearUpwards(world, var8, 9, var10, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var8, -1, var10, area);
				}
			}

			spawnVillagers(world, area, 2, 1, 2, 1);
			return true;
		}

		@Override
		protected int profession(int var1) {
			return 1;
		}
	}


	/** ComponentVillageWoodHut: a hut. */
	private static final class Hut extends VillagePiece {
		private boolean a;
		private int b;

		public Hut(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
			this.a = var3.nextBoolean();
			this.b = var3.nextInt(3);
		}

		static Hut create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 4, 6, 5, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Hut(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 6 - 1, 0);
			}

			fill(world, area, 1, 1, 1, 3, 5, 4, 0, 0, false);
			fill(world, area, 0, 0, 0, 3, 0, 4, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 0, 1, 2, 0, 3, LegacyBlocks.DIRT, LegacyBlocks.DIRT, false);
			if (this.a) {
				fill(world, area, 1, 4, 1, 2, 4, 3, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			} else {
				fill(world, area, 1, 5, 1, 2, 5, 3, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			}

			place(world, LegacyBlocks.LOG, 0, 1, 4, 0, area);
			place(world, LegacyBlocks.LOG, 0, 2, 4, 0, area);
			place(world, LegacyBlocks.LOG, 0, 1, 4, 4, area);
			place(world, LegacyBlocks.LOG, 0, 2, 4, 4, area);
			place(world, LegacyBlocks.LOG, 0, 0, 4, 1, area);
			place(world, LegacyBlocks.LOG, 0, 0, 4, 2, area);
			place(world, LegacyBlocks.LOG, 0, 0, 4, 3, area);
			place(world, LegacyBlocks.LOG, 0, 3, 4, 1, area);
			place(world, LegacyBlocks.LOG, 0, 3, 4, 2, area);
			place(world, LegacyBlocks.LOG, 0, 3, 4, 3, area);
			fill(world, area, 0, 1, 0, 0, 3, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 3, 1, 0, 3, 3, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 0, 1, 4, 0, 3, 4, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 3, 1, 4, 3, 3, 4, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 0, 1, 1, 0, 3, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 3, 1, 1, 3, 3, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 1, 0, 2, 3, 0, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 1, 4, 2, 3, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 3, 2, 2, area);
			if (this.b > 0) {
				place(world, LegacyBlocks.FENCE, 0, this.b, 1, 3, area);
				place(world, LegacyBlocks.WOODEN_PRESSURE_PLATE, 0, this.b, 2, 3, area);
			}

			place(world, 0, 0, 1, 1, 0, area);
			place(world, 0, 0, 1, 2, 0, area);
			door(world, area, random, 1, 1, 0, meta(LegacyBlocks.OAK_DOOR, 1));
			if (getBlock(world, 1, 0, -1, area) == 0 && getBlock(world, 1, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 1, 0, -1, area);
			}

			for (int var4 = 0; var4 < 5; var4++) {
				for (int var5 = 0; var5 < 4; var5++) {
					clearUpwards(world, var5, 6, var4, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var5, -1, var4, area);
				}
			}

			spawnVillagers(world, area, 1, 1, 2, 1);
			return true;
		}
	}


	/** ComponentVillageHall: the butcher. */
	private static final class Butcher extends VillagePiece {

		public Butcher(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static Butcher create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 9, 7, 11, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Butcher(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 7 - 1, 0);
			}

			fill(world, area, 1, 1, 1, 7, 4, 4, 0, 0, false);
			fill(world, area, 2, 1, 6, 8, 4, 10, 0, 0, false);
			fill(world, area, 2, 0, 6, 8, 0, 10, LegacyBlocks.DIRT, LegacyBlocks.DIRT, false);
			place(world, LegacyBlocks.COBBLESTONE, 0, 6, 0, 6, area);
			fill(world, area, 2, 1, 6, 2, 1, 10, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
			fill(world, area, 8, 1, 6, 8, 1, 10, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
			fill(world, area, 3, 1, 10, 7, 1, 10, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
			fill(world, area, 1, 0, 1, 7, 0, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 0, 0, 0, 3, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 8, 0, 0, 8, 3, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 0, 0, 7, 1, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 0, 5, 7, 1, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 2, 0, 7, 3, 0, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 2, 5, 7, 3, 5, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 4, 1, 8, 4, 1, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 4, 4, 8, 4, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 5, 2, 8, 5, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.PLANKS, 0, 0, 4, 2, area);
			place(world, LegacyBlocks.PLANKS, 0, 0, 4, 3, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 4, 2, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 4, 3, area);
			int var4 = meta(LegacyBlocks.OAK_STAIRS, 3);
			int var5 = meta(LegacyBlocks.OAK_STAIRS, 2);

			for (int var6 = -1; var6 <= 2; var6++) {
				for (int var7 = 0; var7 <= 8; var7++) {
					place(world, LegacyBlocks.OAK_STAIRS, var4, var7, 4 + var6, var6, area);
					place(world, LegacyBlocks.OAK_STAIRS, var5, var7, 4 + var6, 5 - var6, area);
				}
			}

			place(world, LegacyBlocks.LOG, 0, 0, 2, 1, area);
			place(world, LegacyBlocks.LOG, 0, 0, 2, 4, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 1, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 3, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 5, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 3, 2, 5, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 2, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 6, 2, 5, area);
			place(world, LegacyBlocks.FENCE, 0, 2, 1, 3, area);
			place(world, LegacyBlocks.WOODEN_PRESSURE_PLATE, 0, 2, 2, 3, area);
			place(world, LegacyBlocks.PLANKS, 0, 1, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, meta(LegacyBlocks.OAK_STAIRS, 3), 2, 1, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, meta(LegacyBlocks.OAK_STAIRS, 1), 1, 1, 3, area);
			fill(world, area, 5, 0, 1, 7, 0, 3, LegacyBlocks.DOUBLE_SLAB, LegacyBlocks.DOUBLE_SLAB, false);
			place(world, LegacyBlocks.DOUBLE_SLAB, 0, 6, 1, 1, area);
			place(world, LegacyBlocks.DOUBLE_SLAB, 0, 6, 1, 2, area);
			place(world, 0, 0, 2, 1, 0, area);
			place(world, 0, 0, 2, 2, 0, area);
			place(world, LegacyBlocks.TORCH, 0, 2, 3, 1, area);
			door(world, area, random, 2, 1, 0, meta(LegacyBlocks.OAK_DOOR, 1));
			if (getBlock(world, 2, 0, -1, area) == 0 && getBlock(world, 2, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 2, 0, -1, area);
			}

			place(world, 0, 0, 6, 1, 5, area);
			place(world, 0, 0, 6, 2, 5, area);
			place(world, LegacyBlocks.TORCH, 0, 6, 3, 4, area);
			door(world, area, random, 6, 1, 5, meta(LegacyBlocks.OAK_DOOR, 1));

			for (int var8 = 0; var8 < 5; var8++) {
				for (int var9 = 0; var9 < 9; var9++) {
					clearUpwards(world, var9, 7, var8, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var9, -1, var8, area);
				}
			}

			spawnVillagers(world, area, 4, 1, 2, 2);
			return true;
		}

		@Override
		protected int profession(int var1) {
			return var1 == 0 ? 4 : 0;
		}
	}


	/** ComponentVillageField: a double farm. */
	private static final class DoubleFarm extends VillagePiece {
		private int a;
		private int b;
		private int c;
		private int d;

		public DoubleFarm(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
			this.a = randomCrop(var3);
			this.b = randomCrop(var3);
			this.c = randomCrop(var3);
			this.d = randomCrop(var3);
		}

		private int randomCrop(Random var1) {
			switch (var1.nextInt(5)) {
				case 0:
					return LegacyBlocks.CARROTS;
				case 1:
					return LegacyBlocks.POTATOES;
				default:
					return LegacyBlocks.WHEAT;
			}
		}

		static DoubleFarm create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 13, 4, 9, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new DoubleFarm(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 4 - 1, 0);
			}

			fill(world, area, 0, 1, 0, 12, 4, 8, 0, 0, false);
			fill(world, area, 1, 0, 1, 2, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 4, 0, 1, 5, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 7, 0, 1, 8, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 10, 0, 1, 11, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 0, 0, 0, 0, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 6, 0, 0, 6, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 12, 0, 0, 12, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 0, 0, 11, 0, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 0, 8, 11, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 3, 0, 1, 3, 0, 7, LegacyBlocks.FLOWING_WATER, LegacyBlocks.FLOWING_WATER, false);
			fill(world, area, 9, 0, 1, 9, 0, 7, LegacyBlocks.FLOWING_WATER, LegacyBlocks.FLOWING_WATER, false);

			for (int var4 = 1; var4 <= 7; var4++) {
				place(world, this.a, nextIntBetween(random, 2, 7), 1, 1, var4, area);
				place(world, this.a, nextIntBetween(random, 2, 7), 2, 1, var4, area);
				place(world, this.b, nextIntBetween(random, 2, 7), 4, 1, var4, area);
				place(world, this.b, nextIntBetween(random, 2, 7), 5, 1, var4, area);
				place(world, this.c, nextIntBetween(random, 2, 7), 7, 1, var4, area);
				place(world, this.c, nextIntBetween(random, 2, 7), 8, 1, var4, area);
				place(world, this.d, nextIntBetween(random, 2, 7), 10, 1, var4, area);
				place(world, this.d, nextIntBetween(random, 2, 7), 11, 1, var4, area);
			}

			for (int var6 = 0; var6 < 9; var6++) {
				for (int var5 = 0; var5 < 13; var5++) {
					clearUpwards(world, var5, 4, var6, area);
					fillDownwards(world, LegacyBlocks.DIRT, 0, var5, -1, var6, area);
				}
			}

			return true;
		}
	}


	/** ComponentVillageField2: a farm. */
	private static final class Farm extends VillagePiece {
		private int a;
		private int b;

		public Farm(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
			this.a = randomCrop(var3);
			this.b = randomCrop(var3);
		}

		private int randomCrop(Random var1) {
			switch (var1.nextInt(5)) {
				case 0:
					return LegacyBlocks.CARROTS;
				case 1:
					return LegacyBlocks.POTATOES;
				default:
					return LegacyBlocks.WHEAT;
			}
		}

		static Farm create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 7, 4, 9, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Farm(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 4 - 1, 0);
			}

			fill(world, area, 0, 1, 0, 6, 4, 8, 0, 0, false);
			fill(world, area, 1, 0, 1, 2, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 4, 0, 1, 5, 0, 7, LegacyBlocks.FARMLAND, LegacyBlocks.FARMLAND, false);
			fill(world, area, 0, 0, 0, 0, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 6, 0, 0, 6, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 0, 0, 5, 0, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 1, 0, 8, 5, 0, 8, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 3, 0, 1, 3, 0, 7, LegacyBlocks.FLOWING_WATER, LegacyBlocks.FLOWING_WATER, false);

			for (int var4 = 1; var4 <= 7; var4++) {
				place(world, this.a, nextIntBetween(random, 2, 7), 1, 1, var4, area);
				place(world, this.a, nextIntBetween(random, 2, 7), 2, 1, var4, area);
				place(world, this.b, nextIntBetween(random, 2, 7), 4, 1, var4, area);
				place(world, this.b, nextIntBetween(random, 2, 7), 5, 1, var4, area);
			}

			for (int var6 = 0; var6 < 9; var6++) {
				for (int var5 = 0; var5 < 7; var5++) {
					clearUpwards(world, var5, 4, var6, area);
					fillDownwards(world, LegacyBlocks.DIRT, 0, var5, -1, var6, area);
				}
			}

			return true;
		}
	}


	/** ComponentVillageHouse2: the blacksmith. */
	private static final class Blacksmith extends VillagePiece {
		private static final LegacyLoot[] LOOT = {
				LegacyLoot.of(Items.DIAMOND, 1, 3, 3),
				LegacyLoot.of(Items.IRON_INGOT, 1, 5, 10),
				LegacyLoot.of(Items.GOLD_INGOT, 1, 3, 5),
				LegacyLoot.of(Items.BREAD, 1, 3, 15),
				LegacyLoot.of(Items.APPLE, 1, 3, 15),
				LegacyLoot.of(Items.IRON_PICKAXE, 1, 1, 5),
				LegacyLoot.of(Items.IRON_SWORD, 1, 1, 5),
				LegacyLoot.of(Items.IRON_CHESTPLATE, 1, 1, 5),
				LegacyLoot.of(Items.IRON_HELMET, 1, 1, 5),
				LegacyLoot.of(Items.IRON_LEGGINGS, 1, 1, 5),
				LegacyLoot.of(Items.IRON_BOOTS, 1, 1, 5),
				LegacyLoot.of(Items.OBSIDIAN, 3, 7, 5),
				LegacyLoot.of(Items.OAK_SAPLING, 3, 7, 5),
				LegacyLoot.of(Items.SADDLE, 1, 1, 3),
				LegacyLoot.of(Items.IRON_HORSE_ARMOR, 1, 1, 1),
				LegacyLoot.of(Items.GOLDEN_HORSE_ARMOR, 1, 1, 1),
				LegacyLoot.of(Items.DIAMOND_HORSE_ARMOR, 1, 1, 1)};
		private boolean b;

		public Blacksmith(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static Blacksmith create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 10, 6, 7, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new Blacksmith(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 6 - 1, 0);
			}

			fill(world, area, 0, 1, 0, 9, 4, 6, 0, 0, false);
			fill(world, area, 0, 0, 0, 9, 0, 6, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 4, 0, 9, 4, 6, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 0, 5, 0, 9, 5, 6, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 1, 5, 1, 8, 5, 5, 0, 0, false);
			fill(world, area, 1, 1, 0, 2, 3, 0, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 1, 0, 0, 4, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 3, 1, 0, 3, 4, 0, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			fill(world, area, 0, 1, 6, 0, 4, 6, LegacyBlocks.LOG, LegacyBlocks.LOG, false);
			place(world, LegacyBlocks.PLANKS, 0, 3, 3, 1, area);
			fill(world, area, 3, 1, 2, 3, 3, 2, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 4, 1, 3, 5, 3, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 1, 1, 0, 3, 5, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 1, 6, 5, 3, 6, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 5, 1, 0, 5, 3, 0, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
			fill(world, area, 9, 1, 0, 9, 3, 0, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
			fill(world, area, 6, 1, 4, 9, 4, 6, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			place(world, LegacyBlocks.FLOWING_LAVA, 0, 7, 1, 5, area);
			place(world, LegacyBlocks.FLOWING_LAVA, 0, 8, 1, 5, area);
			place(world, LegacyBlocks.IRON_BARS, 0, 9, 2, 5, area);
			place(world, LegacyBlocks.IRON_BARS, 0, 9, 2, 4, area);
			fill(world, area, 7, 2, 4, 8, 2, 5, 0, 0, false);
			place(world, LegacyBlocks.COBBLESTONE, 0, 6, 1, 3, area);
			place(world, LegacyBlocks.FURNACE, 0, 6, 2, 3, area);
			place(world, LegacyBlocks.FURNACE, 0, 6, 3, 3, area);
			place(world, LegacyBlocks.DOUBLE_SLAB, 0, 8, 1, 1, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 6, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 4, 2, 6, area);
			place(world, LegacyBlocks.FENCE, 0, 2, 1, 4, area);
			place(world, LegacyBlocks.WOODEN_PRESSURE_PLATE, 0, 2, 2, 4, area);
			place(world, LegacyBlocks.PLANKS, 0, 1, 1, 5, area);
			place(world, LegacyBlocks.OAK_STAIRS, meta(LegacyBlocks.OAK_STAIRS, 3), 2, 1, 5, area);
			place(world, LegacyBlocks.OAK_STAIRS, meta(LegacyBlocks.OAK_STAIRS, 1), 1, 1, 4, area);
			if (!this.b) {
				int var4 = worldY(1);
				int var5 = worldX(5, 5);
				int var6 = worldZ(5, 5);
				if (area.contains(var5, var4, var6)) {
					this.b = true;
					chest(world, area, random, 5, 1, 5, LOOT, 3 + random.nextInt(6));
				}
			}

			for (int var7 = 6; var7 <= 8; var7++) {
				if (getBlock(world, var7, 0, -1, area) == 0 && getBlock(world, var7, -1, -1, area) != 0) {
					place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), var7, 0, -1, area);
				}
			}

			for (int var8 = 0; var8 < 7; var8++) {
				for (int var9 = 0; var9 < 10; var9++) {
					clearUpwards(world, var9, 6, var8, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var9, -1, var8, area);
				}
			}

			spawnVillagers(world, area, 7, 1, 1, 1);
			return true;
		}

		@Override
		protected int profession(int var1) {
			return 3;
		}
	}


	/** ComponentVillageHouse3: the big house. */
	private static final class BigHouse extends VillagePiece {

		public BigHouse(Start var1, int var2, Random var3, LegacyBox var4, int var5) {
			super(var1, var2);
			this.direction = var5;
			this.box = var4;
		}

		static BigHouse create(Start var0, List<LegacyPiece> var1, Random var2, int var3, int var4, int var5, int var6, int var7) {
			LegacyBox var8 = LegacyBox.oriented(var3, var4, var5, 0, 0, 0, 9, 7, 12, var6);
			return isAboveGround(var8) && LegacyPiece.findIntersecting(var1, var8) == null ? new BigHouse(var0, var7, var2, var8, var6) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (this.averageGroundLevel < 0) {
				this.averageGroundLevel = groundLevel(world, area);
				if (this.averageGroundLevel < 0) {
					return true;
				}

				this.box.offset(0, this.averageGroundLevel - this.box.maxY + 7 - 1, 0);
			}

			fill(world, area, 1, 1, 1, 7, 4, 4, 0, 0, false);
			fill(world, area, 2, 1, 6, 8, 4, 10, 0, 0, false);
			fill(world, area, 2, 0, 5, 8, 0, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 0, 1, 7, 0, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 0, 0, 0, 3, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 8, 0, 0, 8, 3, 10, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 0, 0, 7, 2, 0, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 0, 5, 2, 1, 5, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 2, 0, 6, 2, 3, 10, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 3, 0, 10, 7, 3, 10, LegacyBlocks.COBBLESTONE, LegacyBlocks.COBBLESTONE, false);
			fill(world, area, 1, 2, 0, 7, 3, 0, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 1, 2, 5, 2, 3, 5, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 4, 1, 8, 4, 1, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 4, 4, 3, 4, 4, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 0, 5, 2, 8, 5, 3, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			place(world, LegacyBlocks.PLANKS, 0, 0, 4, 2, area);
			place(world, LegacyBlocks.PLANKS, 0, 0, 4, 3, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 4, 2, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 4, 3, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 4, 4, area);
			int var4 = meta(LegacyBlocks.OAK_STAIRS, 3);
			int var5 = meta(LegacyBlocks.OAK_STAIRS, 2);

			for (int var6 = -1; var6 <= 2; var6++) {
				for (int var7 = 0; var7 <= 8; var7++) {
					place(world, LegacyBlocks.OAK_STAIRS, var4, var7, 4 + var6, var6, area);
					if ((var6 > -1 || var7 <= 1) && (var6 > 0 || var7 <= 3) && (var6 > 1 || var7 <= 4 || var7 >= 6)) {
						place(world, LegacyBlocks.OAK_STAIRS, var5, var7, 4 + var6, 5 - var6, area);
					}
				}
			}

			fill(world, area, 3, 4, 5, 3, 4, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 7, 4, 2, 7, 4, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 4, 5, 4, 4, 5, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 6, 5, 4, 6, 5, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			fill(world, area, 5, 6, 3, 5, 6, 10, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
			int var10 = meta(LegacyBlocks.OAK_STAIRS, 0);

			for (int var11 = 4; var11 >= 1; var11--) {
				place(world, LegacyBlocks.PLANKS, 0, var11, 2 + var11, 7 - var11, area);

				for (int var8 = 8 - var11; var8 <= 10; var8++) {
					place(world, LegacyBlocks.OAK_STAIRS, var10, var11, 2 + var11, var8, area);
				}
			}

			int var12 = meta(LegacyBlocks.OAK_STAIRS, 1);
			place(world, LegacyBlocks.PLANKS, 0, 6, 6, 3, area);
			place(world, LegacyBlocks.PLANKS, 0, 7, 5, 4, area);
			place(world, LegacyBlocks.OAK_STAIRS, var12, 6, 6, 4, area);

			for (int var13 = 6; var13 <= 8; var13++) {
				for (int var9 = 5; var9 <= 10; var9++) {
					place(world, LegacyBlocks.OAK_STAIRS, var12, var13, 12 - var13, var9, area);
				}
			}

			place(world, LegacyBlocks.LOG, 0, 0, 2, 1, area);
			place(world, LegacyBlocks.LOG, 0, 0, 2, 4, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 0, 2, 3, area);
			place(world, LegacyBlocks.LOG, 0, 4, 2, 0, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 2, 0, area);
			place(world, LegacyBlocks.LOG, 0, 6, 2, 0, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 1, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 2, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 3, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 4, area);
			place(world, LegacyBlocks.PLANKS, 0, 8, 2, 5, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 6, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 7, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 8, 2, 8, area);
			place(world, LegacyBlocks.LOG, 0, 8, 2, 9, area);
			place(world, LegacyBlocks.LOG, 0, 2, 2, 6, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 7, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 2, 2, 8, area);
			place(world, LegacyBlocks.LOG, 0, 2, 2, 9, area);
			place(world, LegacyBlocks.LOG, 0, 4, 4, 10, area);
			place(world, LegacyBlocks.GLASS_PANE, 0, 5, 4, 10, area);
			place(world, LegacyBlocks.LOG, 0, 6, 4, 10, area);
			place(world, LegacyBlocks.PLANKS, 0, 5, 5, 10, area);
			place(world, 0, 0, 2, 1, 0, area);
			place(world, 0, 0, 2, 2, 0, area);
			place(world, LegacyBlocks.TORCH, 0, 2, 3, 1, area);
			door(world, area, random, 2, 1, 0, meta(LegacyBlocks.OAK_DOOR, 1));
			fill(world, area, 1, 0, -1, 3, 2, -1, 0, 0, false);
			if (getBlock(world, 2, 0, -1, area) == 0 && getBlock(world, 2, -1, -1, area) != 0) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, meta(LegacyBlocks.COBBLESTONE_STAIRS, 3), 2, 0, -1, area);
			}

			for (int var14 = 0; var14 < 5; var14++) {
				for (int var16 = 0; var16 < 9; var16++) {
					clearUpwards(world, var16, 7, var14, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var16, -1, var14, area);
				}
			}

			for (int var15 = 5; var15 < 11; var15++) {
				for (int var17 = 2; var17 < 9; var17++) {
					clearUpwards(world, var17, 7, var15, area);
					fillDownwards(world, LegacyBlocks.COBBLESTONE, 0, var17, -1, var15, area);
				}
			}

			spawnVillagers(world, area, 4, 1, 2, 2);
			return true;
		}
	}

}
