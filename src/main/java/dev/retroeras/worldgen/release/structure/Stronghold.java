package dev.retroeras.worldgen.release.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.retroeras.worldgen.release.ReleaseBiome;
import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** 1.6.4's strongholds (MapGenStronghold, StructureStrongholdPieces): three per world, in a ring round the origin. */
public final class Stronghold extends LegacyStructureFeature {
	private static final Set<ReleaseBiome> BIOMES = Set.of(ReleaseBiome.DESERT, ReleaseBiome.FOREST, ReleaseBiome.EXTREME_HILLS,
			ReleaseBiome.SWAMPLAND, ReleaseBiome.TAIGA, ReleaseBiome.ICE_PLAINS, ReleaseBiome.ICE_MOUNTAINS, ReleaseBiome.DESERT_HILLS,
			ReleaseBiome.FOREST_HILLS, ReleaseBiome.EXTREME_HILLS_EDGE, ReleaseBiome.JUNGLE, ReleaseBiome.JUNGLE_HILLS);
	private static final int COUNT = 3;
	private static final double DISTANCE = 32.0;
	private static final Stones STONES = new Stones();

	private final ReleaseTerrain terrain;
	private volatile int @Nullable [][] positions;

	public Stronghold(long seed, ReleaseTerrain terrain) {
		super(seed);
		this.terrain = terrain;
	}

	/** The chunks the strongholds start in, worked out once per world as 1.6.4 did. */
	private int[][] positions() {
		int[][] current = positions;
		if (current != null) {
			return current;
		}
		synchronized (this) {
			if (positions == null) {
				Random random = new Random(seed);
				double angle = random.nextDouble() * Math.PI * 2.0;
				int ring = 1;
				int spread = 3;
				int[][] found = new int[COUNT][];
				for (int i = 0; i < COUNT; i++) {
					double distance = (1.25 * ring + random.nextDouble()) * (DISTANCE * ring);
					int chunkX = (int) Math.round(Math.cos(angle) * distance);
					int chunkZ = (int) Math.round(Math.sin(angle) * distance);
					int[] biomePos = terrain.findBiomePosition((chunkX << 4) + 8, (chunkZ << 4) + 8, 112, BIOMES, random);
					if (biomePos != null) {
						chunkX = biomePos[0] >> 4;
						chunkZ = biomePos[1] >> 4;
					}
					found[i] = new int[] {chunkX, chunkZ};
					angle += Math.PI * 2 * ring / spread;
					if (i == spread) {
						ring += 2 + random.nextInt(5);
						spread = spread + 1 + random.nextInt(2);
					}
				}
				positions = found;
			}
			return positions;
		}
	}

	/** The block positions of the strongholds, for tests and for finding them. */
	public List<int[]> strongholdBlocks() {
		List<int[]> blocks = new ArrayList<>();
		for (int[] chunk : positions()) {
			blocks.add(new int[] {(chunk[0] << 4) + 8, (chunk[1] << 4) + 8});
		}
		return blocks;
	}

	@Override
	protected boolean canSpawnAt(Random random, int chunkX, int chunkZ) {
		for (int[] position : positions()) {
			if (chunkX == position[0] && chunkZ == position[1]) {
				return true;
			}
		}
		return false;
	}

	/** getStructureStart: a stronghold is laid out again until it has a portal room. */
	@Override
	protected LegacyStart createStart(Random random, int chunkX, int chunkZ) {
		StrongholdStart start = new StrongholdStart(random, chunkX, chunkZ);
		while (start.start.portalRoom == null) {
			start = new StrongholdStart(random, chunkX, chunkZ);
		}
		return start;
	}

	/** getNearestInstance: the nearest stronghold's portal room, for eyes of ender. */
	public @Nullable BlockPos nearest(BlockPos from) {
		BlockPos nearest = null;
		double best = Double.MAX_VALUE;
		for (int[] position : positions()) {
			Optional<LegacyStart> start = startAt(position[0], position[1]);
			if (start.isPresent() && start.get() instanceof StrongholdStart stronghold) {
				BlockPos target = stronghold.target();
				double distance = target.distSqr(from);
				if (distance < best) {
					best = distance;
					nearest = target;
				}
			}
		}
		return nearest;
	}

	/** MapGenStronghold.Start. */
	private static final class StrongholdStart extends LegacyStart {
		private final Start start;

		StrongholdStart(Random random, int chunkX, int chunkZ) {
			start = new Start(0, random, (chunkX << 4) + 2, (chunkZ << 4) + 2);
			pieces.add(start);
			start.addChildren(start, pieces, random);
			List<LegacyPiece> pending = start.pending;
			while (!pending.isEmpty()) {
				pending.remove(random.nextInt(pending.size())).addChildren(start, pieces, random);
			}
			updateBox();
			sinkBelowSeaLevel(random, 10);
		}

		/** The middle of the portal room, which is where 1.6.4's eyes of ender led. */
		BlockPos target() {
			LegacyBox target = start.portalRoom != null ? start.portalRoom.box() : start.box();
			return new BlockPos(target.centerX(), target.centerY(), target.centerZ());
		}
	}

	/** StructureStrongholdPieceWeight: how likely a room is, how many a stronghold may have, and how deep it must be. */
	private static class PieceWeight {
		final Class<? extends StrongholdPiece> type;
		final int weight;
		int placed;
		final int limit;
		private final int minDepth;

		PieceWeight(Class<? extends StrongholdPiece> type, int weight, int limit, int minDepth) {
			this.type = type;
			this.weight = weight;
			this.limit = limit;
			this.minDepth = minDepth;
		}

		boolean canSpawnMoreAt(int depth) {
			return canSpawnMore() && depth > minDepth;
		}

		boolean canSpawnMore() {
			return limit == 0 || placed < limit;
		}
	}

	/** StructureStrongholdPieces.prepareStructurePieces. */
	private static List<PieceWeight> createWeights() {
		List<PieceWeight> weights = new ArrayList<>();
		weights.add(new PieceWeight(Straight.class, 40, 0, Integer.MIN_VALUE));
		weights.add(new PieceWeight(Prison.class, 5, 5, Integer.MIN_VALUE));
		weights.add(new PieceWeight(LeftTurn.class, 20, 0, Integer.MIN_VALUE));
		weights.add(new PieceWeight(RightTurn.class, 20, 0, Integer.MIN_VALUE));
		weights.add(new PieceWeight(RoomCrossing.class, 10, 6, Integer.MIN_VALUE));
		weights.add(new PieceWeight(StraightStairs.class, 5, 5, Integer.MIN_VALUE));
		weights.add(new PieceWeight(SpiralStairs.class, 5, 5, Integer.MIN_VALUE));
		weights.add(new PieceWeight(Crossing.class, 5, 4, Integer.MIN_VALUE));
		weights.add(new PieceWeight(ChestCorridor.class, 5, 4, Integer.MIN_VALUE));
		weights.add(new PieceWeight(Library.class, 10, 2, 4));
		weights.add(new PieceWeight(PortalRoom.class, 20, 1, 5));
		return weights;
	}

	private static @Nullable StrongholdPiece create(Class<? extends StrongholdPiece> type, List<LegacyPiece> pieces, Random random, int x, int y, int z,
			int direction, int depth) {
		if (type == Straight.class) {
			return Straight.create(pieces, random, x, y, z, direction, depth);
		} else if (type == Prison.class) {
			return Prison.create(pieces, random, x, y, z, direction, depth);
		} else if (type == LeftTurn.class) {
			return LeftTurn.create(pieces, random, x, y, z, direction, depth);
		} else if (type == RightTurn.class) {
			// 1.6.4 called LeftTurn's factory here, so right turns never appeared.
			return LeftTurn.create(pieces, random, x, y, z, direction, depth);
		} else if (type == RoomCrossing.class) {
			return RoomCrossing.create(pieces, random, x, y, z, direction, depth);
		} else if (type == StraightStairs.class) {
			return StraightStairs.create(pieces, random, x, y, z, direction, depth);
		} else if (type == SpiralStairs.class) {
			return SpiralStairs.create(pieces, random, x, y, z, direction, depth);
		} else if (type == Crossing.class) {
			return Crossing.create(pieces, random, x, y, z, direction, depth);
		} else if (type == ChestCorridor.class) {
			return ChestCorridor.create(pieces, random, x, y, z, direction, depth);
		} else if (type == Library.class) {
			return Library.create(pieces, random, x, y, z, direction, depth);
		} else if (type == PortalRoom.class) {
			return PortalRoom.create(pieces, random, x, y, z, direction, depth);
		}
		return null;
	}

	/** getStrongholdComponentFromWeightedPiece: a weighted random room, or a filler corridor if none fits. */
	private static @Nullable StrongholdPiece nextPiece(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction,
			int depth) {
		boolean any = false;
		int total = 0;
		for (PieceWeight weight : start.weights) {
			if (weight.limit > 0 && weight.placed < weight.limit) {
				any = true;
			}
			total += weight.weight;
		}
		if (!any) {
			return null;
		}
		if (start.forcedType != null) {
			StrongholdPiece piece = create(start.forcedType, pieces, random, x, y, z, direction, depth);
			start.forcedType = null;
			if (piece != null) {
				return piece;
			}
		}
		for (int attempt = 0; attempt < 5; attempt++) {
			int roll = random.nextInt(total);
			for (PieceWeight weight : start.weights) {
				roll -= weight.weight;
				if (roll < 0) {
					if (!weight.canSpawnMoreAt(depth) || weight == start.lastWeight) {
						break;
					}
					StrongholdPiece piece = create(weight.type, pieces, random, x, y, z, direction, depth);
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
		LegacyBox box = Filler.create(pieces, random, x, y, z, direction);
		return box != null && box.minY > 1 ? new Filler(depth, random, box, direction) : null;
	}

	/** getNextValidComponent. */
	private static @Nullable LegacyPiece next(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth) {
		if (depth > 50) {
			return null;
		}
		if (Math.abs(x - start.box().minX) <= 112 && Math.abs(z - start.box().minZ) <= 112) {
			StrongholdPiece piece = nextPiece(start, pieces, random, x, y, z, direction, depth + 1);
			if (piece != null) {
				pieces.add(piece);
				start.pending.add(piece);
			}
			return piece;
		}
		return null;
	}

	/** EnumDoor: what fills a doorway. */
	private enum Door {
		OPENING, WOOD_DOOR, GRATES, IRON_DOOR
	}

	/** ComponentStronghold: a stronghold room, entered through a doorway. */
	private abstract static class StrongholdPiece extends LegacyPiece {
		protected Door entryDoor = Door.OPENING;

		protected StrongholdPiece(int depth) {
			super(depth);
		}

		/** placeDoor: fills a 3x3 doorway at (x, y, z). */
		protected void entrance(LegacyWorld world, Random random, LegacyBox area, Door door, int x, int y, int z) {
			switch (door) {
				case WOOD_DOOR -> {
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y + 1, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 1, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y + 1, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y, z, area);
					place(world, LegacyBlocks.OAK_DOOR, 0, x + 1, y, z, area);
					place(world, LegacyBlocks.OAK_DOOR, 8, x + 1, y + 1, z, area);
				}
				case GRATES -> {
					place(world, 0, 0, x + 1, y, z, area);
					place(world, 0, 0, x + 1, y + 1, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x, y, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x, y + 1, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x, y + 2, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x + 1, y + 2, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x + 2, y + 2, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x + 2, y + 1, z, area);
					place(world, LegacyBlocks.IRON_BARS, 0, x + 2, y, z, area);
				}
				case IRON_DOOR -> {
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y + 1, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 1, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y + 2, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y + 1, z, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, x + 2, y, z, area);
					place(world, LegacyBlocks.IRON_DOOR, 0, x + 1, y, z, area);
					place(world, LegacyBlocks.IRON_DOOR, 8, x + 1, y + 1, z, area);
					place(world, LegacyBlocks.STONE_BUTTON, meta(LegacyBlocks.STONE_BUTTON, 4), x + 2, y + 1, z + 1, area);
					place(world, LegacyBlocks.STONE_BUTTON, meta(LegacyBlocks.STONE_BUTTON, 3), x + 2, y + 1, z - 1, area);
				}
				default -> fill(world, area, x, y, z, x + 3 - 1, y + 3 - 1, z, 0, 0, false);
			}
		}

		/** getRandomDoor. */
		protected Door randomDoor(Random random) {
			return switch (random.nextInt(5)) {
				case 2 -> Door.WOOD_DOOR;
				case 3 -> Door.GRATES;
				case 4 -> Door.IRON_DOOR;
				default -> Door.OPENING;
			};
		}

		/** getNextComponentNormal: the piece straight ahead. */
		protected @Nullable LegacyPiece nextForward(LegacyPiece start, List<LegacyPiece> pieces, Random random, int offset, int dy) {
			return switch (direction) {
				case 0 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.maxZ + 1, direction, depth());
				case 1 -> next((Start) start, pieces, random, box.minX - 1, box.minY + dy, box.minZ + offset, direction, depth());
				case 2 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.minZ - 1, direction, depth());
				case 3 -> next((Start) start, pieces, random, box.maxX + 1, box.minY + dy, box.minZ + offset, direction, depth());
				default -> null;
			};
		}

		/** getNextComponentX: a piece off the negative side. */
		protected @Nullable LegacyPiece nextLeft(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset) {
			return switch (direction) {
				case 0, 2 -> next((Start) start, pieces, random, box.minX - 1, box.minY + dy, box.minZ + offset, 1, depth());
				case 1, 3 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.minZ - 1, 2, depth());
				default -> null;
			};
		}

		/** getNextComponentZ: a piece off the positive side. */
		protected @Nullable LegacyPiece nextRight(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset) {
			return switch (direction) {
				case 0, 2 -> next((Start) start, pieces, random, box.maxX + 1, box.minY + dy, box.minZ + offset, 3, depth());
				case 1, 3 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.maxZ + 1, 0, depth());
				default -> null;
			};
		}

		protected static boolean isAboveGround(@Nullable LegacyBox box) {
			return box != null && box.minY > 10;
		}
	}

	/** StructureStrongholdStones: stone brick walls, cracked, mossy or hiding silverfish here and there. */
	private static final class Stones extends LegacyPiece.BlockSelector {
		@Override
		public void select(Random random, int x, int y, int z, boolean edge) {
			if (edge) {
				id = LegacyBlocks.STONE_BRICKS;
				float roll = random.nextFloat();
				if (roll < 0.2F) {
					meta = 2;
				} else if (roll < 0.5F) {
					meta = 1;
				} else if (roll < 0.55F) {
					id = LegacyBlocks.MONSTER_EGG;
					meta = 2;
				} else {
					meta = 0;
				}
			} else {
				id = 0;
				meta = 0;
			}
		}
	}

	/** ComponentStrongholdStairs2: the spiral staircase a stronghold starts from, and what it needs while it is laid out. */
	private static final class Start extends SpiralStairs {
		final List<PieceWeight> weights = createWeights();
		@Nullable PieceWeight lastWeight;
		@Nullable Class<? extends StrongholdPiece> forcedType;
		@Nullable PortalRoom portalRoom;
		final List<LegacyPiece> pending = new ArrayList<>();

		Start(int depth, Random random, int x, int z) {
			super(0, random, x, z);
		}
	}


	/** ComponentStrongholdStairs2: a spiral staircase. */
	private static class SpiralStairs extends StrongholdPiece {
		private boolean a;

		public SpiralStairs(int var1, Random var2, int var3, int var4) {
			super(var1);
			this.a = true;
			this.direction = var2.nextInt(4);
			this.entryDoor = Door.OPENING;
			switch (this.direction) {
				case 0:
				case 2:
					this.box = new LegacyBox(var3, 64, var4, var3 + 5 - 1, 74, var4 + 5 - 1);
					break;
				default:
					this.box = new LegacyBox(var3, 64, var4, var3 + 5 - 1, 74, var4 + 5 - 1);
			}
		}

		public SpiralStairs(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.a = false;
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			if (this.a) {
				((Start) var1).forcedType = Crossing.class;
			}

			nextForward(var1, var2, var3, 1, 1);
		}

		static @Nullable SpiralStairs create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -7, 0, 5, 11, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new SpiralStairs(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 10, 4, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 7, 0);
			entrance(world, random, area, Door.OPENING, 1, 1, 4);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 6, 1, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 5, 1, area);
			place(world, LegacyBlocks.SLAB, 0, 1, 6, 1, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 5, 2, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 4, 3, area);
			place(world, LegacyBlocks.SLAB, 0, 1, 5, 3, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 4, 3, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 3, 3, area);
			place(world, LegacyBlocks.SLAB, 0, 3, 4, 3, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 3, 2, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 2, 1, area);
			place(world, LegacyBlocks.SLAB, 0, 3, 3, 1, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 2, 1, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 1, 1, area);
			place(world, LegacyBlocks.SLAB, 0, 1, 2, 1, area);
			place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 1, 2, area);
			place(world, LegacyBlocks.SLAB, 0, 1, 1, 3, area);
			return true;
		}
	}


	/** ComponentStrongholdStraight: a corridor, sometimes with side openings. */
	private static class Straight extends StrongholdPiece {
		private boolean a;
		private boolean b;

		public Straight(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
			this.a = var2.nextInt(2) == 0;
			this.b = var2.nextInt(2) == 0;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 1);
			if (this.a) {
				nextLeft(var1, var2, var3, 1, 2);
			}

			if (this.b) {
				nextRight(var1, var2, var3, 1, 2);
			}
		}

		static @Nullable Straight create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, 7, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new Straight(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 4, 6, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 1, 0);
			entrance(world, random, area, Door.OPENING, 1, 1, 6);
			placeRandomly(world, area, random, 0.1F, 1, 2, 1, LegacyBlocks.TORCH, 0);
			placeRandomly(world, area, random, 0.1F, 3, 2, 1, LegacyBlocks.TORCH, 0);
			placeRandomly(world, area, random, 0.1F, 1, 2, 5, LegacyBlocks.TORCH, 0);
			placeRandomly(world, area, random, 0.1F, 3, 2, 5, LegacyBlocks.TORCH, 0);
			if (this.a) {
				fill(world, area, 0, 1, 2, 0, 3, 4, 0, 0, false);
			}

			if (this.b) {
				fill(world, area, 4, 1, 2, 4, 3, 4, 0, 0, false);
			}

			return true;
		}
	}


	/** ComponentStrongholdChestCorridor: a corridor with a chest on a stand. */
	private static class ChestCorridor extends StrongholdPiece {
		private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.ENDER_PEARL, 1, 1, 10),
			LegacyLoot.of(Items.DIAMOND, 1, 3, 3),
			LegacyLoot.of(Items.IRON_INGOT, 1, 5, 10),
			LegacyLoot.of(Items.GOLD_INGOT, 1, 3, 5),
			LegacyLoot.of(Items.REDSTONE, 4, 9, 5),
			LegacyLoot.of(Items.BREAD, 1, 3, 15),
			LegacyLoot.of(Items.APPLE, 1, 3, 15),
			LegacyLoot.of(Items.IRON_PICKAXE, 1, 1, 5),
			LegacyLoot.of(Items.IRON_SWORD, 1, 1, 5),
			LegacyLoot.of(Items.IRON_CHESTPLATE, 1, 1, 5),
			LegacyLoot.of(Items.IRON_HELMET, 1, 1, 5),
			LegacyLoot.of(Items.IRON_LEGGINGS, 1, 1, 5),
			LegacyLoot.of(Items.IRON_BOOTS, 1, 1, 5),
			LegacyLoot.of(Items.GOLDEN_APPLE, 1, 1, 1),
			LegacyLoot.of(Items.SADDLE, 1, 1, 1),
			LegacyLoot.of(Items.IRON_HORSE_ARMOR, 1, 1, 1),
			LegacyLoot.of(Items.GOLDEN_HORSE_ARMOR, 1, 1, 1),
			LegacyLoot.of(Items.DIAMOND_HORSE_ARMOR, 1, 1, 1)};
		private boolean b;

		public ChestCorridor(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 1);
		}

		static @Nullable ChestCorridor create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, 7, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new ChestCorridor(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 4, 6, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 1, 0);
			entrance(world, random, area, Door.OPENING, 1, 1, 6);
			fill(world, area, 3, 1, 2, 3, 1, 4, LegacyBlocks.STONE_BRICKS, LegacyBlocks.STONE_BRICKS, false);
			place(world, LegacyBlocks.SLAB, 5, 3, 1, 1, area);
			place(world, LegacyBlocks.SLAB, 5, 3, 1, 5, area);
			place(world, LegacyBlocks.SLAB, 5, 3, 2, 2, area);
			place(world, LegacyBlocks.SLAB, 5, 3, 2, 4, area);

			for (int var4 = 2; var4 <= 4; var4++) {
				place(world, LegacyBlocks.SLAB, 5, 2, 1, var4, area);
			}

			if (!this.b) {
				int var7 = worldY(2);
				int var5 = worldX(3, 3);
				int var6 = worldZ(3, 3);
				if (area.contains(var5, var7, var6)) {
					this.b = true;
					chest(world, area, random, 3, 2, 3, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 2 + random.nextInt(2));
				}
			}

			return true;
		}
	}


	/** ComponentStrongholdCorridor: a short filler corridor where nothing else fits. */
	private static class Filler extends StrongholdPiece {
		private int a;

		public Filler(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
			this.a = var4 != 2 && var4 != 0 ? var3.xSpan() : var3.zSpan();
		}

		static @Nullable LegacyBox create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5) {
			byte var6 = 3;
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, 4, var5);
			LegacyPiece var8 = LegacyPiece.findIntersecting(var0, var7);
			if (var8 == null) {
				return null;
			}

			if (var8.box().minY == var7.minY) {
				for (int var9 = 3; var9 >= 1; var9--) {
					var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, var9 - 1, var5);
					if (!var8.box().intersects(var7)) {
						return LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, var9, var5);
					}
				}
			}

			return null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			for (int var4 = 0; var4 < this.a; var4++) {
				place(world, LegacyBlocks.STONE_BRICKS, 0, 0, 0, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 0, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 0, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 0, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 4, 0, var4, area);

				for (int var5 = 1; var5 <= 3; var5++) {
					place(world, LegacyBlocks.STONE_BRICKS, 0, 0, var5, var4, area);
					place(world, 0, 0, 1, var5, var4, area);
					place(world, 0, 0, 2, var5, var4, area);
					place(world, 0, 0, 3, var5, var4, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 4, var5, var4, area);
				}

				place(world, LegacyBlocks.STONE_BRICKS, 0, 0, 4, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 4, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 4, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 4, var4, area);
				place(world, LegacyBlocks.STONE_BRICKS, 0, 4, 4, var4, area);
			}

			return true;
		}
	}


	/** ComponentStrongholdCrossing: a two-storey crossing with a fountain. */
	private static class Crossing extends StrongholdPiece {
		private boolean a;
		private boolean b;
		private boolean c;
		private boolean e;

		public Crossing(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
			this.a = var2.nextBoolean();
			this.b = var2.nextBoolean();
			this.c = var2.nextBoolean();
			this.e = var2.nextInt(3) > 0;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			int var4 = 3;
			int var5 = 5;
			if (this.direction == 1 || this.direction == 2) {
				var4 = 8 - var4;
				var5 = 8 - var5;
			}

			nextForward(var1, var2, var3, 5, 1);
			if (this.a) {
				nextLeft(var1, var2, var3, var4, 1);
			}

			if (this.b) {
				nextLeft(var1, var2, var3, var5, 7);
			}

			if (this.c) {
				nextRight(var1, var2, var3, var4, 1);
			}

			if (this.e) {
				nextRight(var1, var2, var3, var5, 7);
			}
		}

		static @Nullable Crossing create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -4, -3, 0, 10, 9, 11, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new Crossing(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 9, 8, 10, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 4, 3, 0);
			if (this.a) {
				fill(world, area, 0, 3, 1, 0, 5, 3, 0, 0, false);
			}

			if (this.c) {
				fill(world, area, 9, 3, 1, 9, 5, 3, 0, 0, false);
			}

			if (this.b) {
				fill(world, area, 0, 5, 7, 0, 7, 9, 0, 0, false);
			}

			if (this.e) {
				fill(world, area, 9, 5, 7, 9, 7, 9, 0, 0, false);
			}

			fill(world, area, 5, 1, 10, 7, 3, 10, 0, 0, false);
			fill(world, area, 1, 2, 1, 8, 2, 6, false, random, STONES);
			fill(world, area, 4, 1, 5, 4, 4, 9, false, random, STONES);
			fill(world, area, 8, 1, 5, 8, 4, 9, false, random, STONES);
			fill(world, area, 1, 4, 7, 3, 4, 9, false, random, STONES);
			fill(world, area, 1, 3, 5, 3, 3, 6, false, random, STONES);
			fill(world, area, 1, 3, 4, 3, 3, 4, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 1, 4, 6, 3, 4, 6, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 5, 1, 7, 7, 1, 8, false, random, STONES);
			fill(world, area, 5, 1, 9, 7, 1, 9, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 5, 2, 7, 7, 2, 7, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 4, 5, 7, 4, 5, 9, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 8, 5, 7, 8, 5, 9, LegacyBlocks.SLAB, LegacyBlocks.SLAB, false);
			fill(world, area, 5, 5, 7, 7, 5, 9, LegacyBlocks.DOUBLE_SLAB, LegacyBlocks.DOUBLE_SLAB, false);
			place(world, LegacyBlocks.TORCH, 0, 6, 5, 6, area);
			return true;
		}
	}


	/** ComponentStrongholdLeftTurn. */
	private static class LeftTurn extends StrongholdPiece {

		public LeftTurn(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			if (this.direction != 2 && this.direction != 3) {
				nextRight(var1, var2, var3, 1, 1);
			} else {
				nextLeft(var1, var2, var3, 1, 1);
			}
		}

		static @Nullable LeftTurn create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 5, 5, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new LeftTurn(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 4, 4, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 1, 0);
			if (this.direction != 2 && this.direction != 3) {
				fill(world, area, 4, 1, 1, 4, 3, 3, 0, 0, false);
			} else {
				fill(world, area, 0, 1, 1, 0, 3, 3, 0, 0, false);
			}

			return true;
		}
	}


	/**
	 * ComponentStrongholdRightTurn. In 1.6.4 it had no factory of its own, so its weight built left turns instead (see
	 * {@link Stronghold#create}); it is kept that way.
	 */
	private static class RightTurn extends LeftTurn {
		RightTurn(int depth, Random random, LegacyBox box, int direction) {
			super(depth, random, box, direction);
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			if (this.direction != 2 && this.direction != 3) {
				nextLeft(var1, var2, var3, 1, 1);
			} else {
				nextRight(var1, var2, var3, 1, 1);
			}
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 4, 4, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 1, 0);
			if (this.direction != 2 && this.direction != 3) {
				fill(world, area, 0, 1, 1, 0, 3, 3, 0, 0, false);
			} else {
				fill(world, area, 4, 1, 1, 4, 3, 3, 0, 0, false);
			}

			return true;
		}
	}


	/** ComponentStrongholdLibrary: one or two storeys of bookshelves. */
	private static class Library extends StrongholdPiece {
		private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.BOOK, 1, 3, 20),
			LegacyLoot.of(Items.PAPER, 2, 7, 20),
			LegacyLoot.of(Items.MAP, 1, 1, 1),
			LegacyLoot.of(Items.COMPASS, 1, 1, 1)};
		private boolean b;

		public Library(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
			this.b = var3.ySpan() > 6;
		}

		static @Nullable Library create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -4, -1, 0, 14, 11, 15, var5);
			if (!isAboveGround(var7) || LegacyPiece.findIntersecting(var0, var7) != null) {
				var7 = LegacyBox.oriented(var2, var3, var4, -4, -1, 0, 14, 6, 15, var5);
				if (!isAboveGround(var7) || LegacyPiece.findIntersecting(var0, var7) != null) {
					return null;
				}
			}

			return new Library(var6, var1, var7, var5);
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			byte var4 = 11;
			if (!this.b) {
				var4 = 6;
			}

			fill(world, area, 0, 0, 0, 13, var4 - 1, 14, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 4, 1, 0);
			fillRandomly(world, area, random, 0.07F, 2, 1, 1, 11, 4, 13, LegacyBlocks.WEB, LegacyBlocks.WEB, false);
			boolean var5 = true;
			byte var6 = 12;

			for (int var7 = 1; var7 <= 13; var7++) {
				if ((var7 - 1) % 4 == 0) {
					fill(world, area, 1, 1, var7, 1, 4, var7, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
					fill(world, area, 12, 1, var7, 12, 4, var7, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
					place(world, LegacyBlocks.TORCH, 0, 2, 3, var7, area);
					place(world, LegacyBlocks.TORCH, 0, 11, 3, var7, area);
					if (this.b) {
						fill(world, area, 1, 6, var7, 1, 9, var7, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
						fill(world, area, 12, 6, var7, 12, 9, var7, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
					}
				} else {
					fill(world, area, 1, 1, var7, 1, 4, var7, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
					fill(world, area, 12, 1, var7, 12, 4, var7, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
					if (this.b) {
						fill(world, area, 1, 6, var7, 1, 9, var7, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
						fill(world, area, 12, 6, var7, 12, 9, var7, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
					}
				}
			}

			for (byte var10 = 3; var10 < 12; var10 += 2) {
				fill(world, area, 3, 1, var10, 4, 3, var10, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
				fill(world, area, 6, 1, var10, 7, 3, var10, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
				fill(world, area, 9, 1, var10, 10, 3, var10, LegacyBlocks.BOOKSHELF, LegacyBlocks.BOOKSHELF, false);
			}

			if (this.b) {
				fill(world, area, 1, 5, 1, 3, 5, 13, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
				fill(world, area, 10, 5, 1, 12, 5, 13, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
				fill(world, area, 4, 5, 1, 9, 5, 2, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
				fill(world, area, 4, 5, 12, 9, 5, 13, LegacyBlocks.PLANKS, LegacyBlocks.PLANKS, false);
				place(world, LegacyBlocks.PLANKS, 0, 9, 5, 11, area);
				place(world, LegacyBlocks.PLANKS, 0, 8, 5, 11, area);
				place(world, LegacyBlocks.PLANKS, 0, 9, 5, 10, area);
				fill(world, area, 3, 6, 2, 3, 6, 12, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
				fill(world, area, 10, 6, 2, 10, 6, 10, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
				fill(world, area, 4, 6, 2, 9, 6, 2, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
				fill(world, area, 4, 6, 12, 8, 6, 12, LegacyBlocks.FENCE, LegacyBlocks.FENCE, false);
				place(world, LegacyBlocks.FENCE, 0, 9, 6, 11, area);
				place(world, LegacyBlocks.FENCE, 0, 8, 6, 11, area);
				place(world, LegacyBlocks.FENCE, 0, 9, 6, 10, area);
				int var11 = meta(LegacyBlocks.LADDER, 3);
				place(world, LegacyBlocks.LADDER, var11, 10, 1, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 2, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 3, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 4, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 5, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 6, 13, area);
				place(world, LegacyBlocks.LADDER, var11, 10, 7, 13, area);
				byte var8 = 7;
				byte var9 = 7;
				place(world, LegacyBlocks.FENCE, 0, var8 - 1, 9, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8, 9, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8 - 1, 8, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8, 8, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8 - 1, 7, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8, 7, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8 - 2, 7, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8 + 1, 7, var9, area);
				place(world, LegacyBlocks.FENCE, 0, var8 - 1, 7, var9 - 1, area);
				place(world, LegacyBlocks.FENCE, 0, var8 - 1, 7, var9 + 1, area);
				place(world, LegacyBlocks.FENCE, 0, var8, 7, var9 - 1, area);
				place(world, LegacyBlocks.FENCE, 0, var8, 7, var9 + 1, area);
				place(world, LegacyBlocks.TORCH, 0, var8 - 2, 8, var9, area);
				place(world, LegacyBlocks.TORCH, 0, var8 + 1, 8, var9, area);
				place(world, LegacyBlocks.TORCH, 0, var8 - 1, 8, var9 - 1, area);
				place(world, LegacyBlocks.TORCH, 0, var8 - 1, 8, var9 + 1, area);
				place(world, LegacyBlocks.TORCH, 0, var8, 8, var9 - 1, area);
				place(world, LegacyBlocks.TORCH, 0, var8, 8, var9 + 1, area);
			}

			chest(world, area, random, 3, 3, 5, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random, 1, 5, 2)), 1 + random.nextInt(4));
			if (this.b) {
				place(world, 0, 0, 12, 9, 1, area);
				chest(world, area, random, 12, 8, 1, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random, 1, 5, 2)), 1 + random.nextInt(4));
			}

			return true;
		}
	}


	/** ComponentStrongholdPortalRoom: the end portal room, with its silverfish spawner. */
	private static class PortalRoom extends StrongholdPiece {
		private boolean a;

		public PortalRoom(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			if (var1 != null) {
				((Start) var1).portalRoom = this;
			}
		}

		static @Nullable PortalRoom create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -4, -1, 0, 11, 8, 16, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new PortalRoom(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 10, 7, 15, false, random, STONES);
			entrance(world, random, area, Door.GRATES, 4, 1, 0);
			int var4 = 6;
			fill(world, area, 1, var4, 1, 1, var4, 14, false, random, STONES);
			fill(world, area, 9, var4, 1, 9, var4, 14, false, random, STONES);
			fill(world, area, 2, var4, 1, 8, var4, 2, false, random, STONES);
			fill(world, area, 2, var4, 14, 8, var4, 14, false, random, STONES);
			fill(world, area, 1, 1, 1, 2, 1, 4, false, random, STONES);
			fill(world, area, 8, 1, 1, 9, 1, 4, false, random, STONES);
			fill(world, area, 1, 1, 1, 1, 1, 3, LegacyBlocks.FLOWING_LAVA, LegacyBlocks.FLOWING_LAVA, false);
			fill(world, area, 9, 1, 1, 9, 1, 3, LegacyBlocks.FLOWING_LAVA, LegacyBlocks.FLOWING_LAVA, false);
			fill(world, area, 3, 1, 8, 7, 1, 12, false, random, STONES);
			fill(world, area, 4, 1, 9, 6, 1, 11, LegacyBlocks.FLOWING_LAVA, LegacyBlocks.FLOWING_LAVA, false);

			for (byte var5 = 3; var5 < 14; var5 += 2) {
				fill(world, area, 0, 3, var5, 0, 4, var5, LegacyBlocks.IRON_BARS, LegacyBlocks.IRON_BARS, false);
				fill(world, area, 10, 3, var5, 10, 4, var5, LegacyBlocks.IRON_BARS, LegacyBlocks.IRON_BARS, false);
			}

			for (byte var14 = 2; var14 < 9; var14 += 2) {
				fill(world, area, var14, 3, 15, var14, 4, 15, LegacyBlocks.IRON_BARS, LegacyBlocks.IRON_BARS, false);
			}

			int var15 = meta(LegacyBlocks.STONE_BRICK_STAIRS, 3);
			fill(world, area, 4, 1, 5, 6, 1, 7, false, random, STONES);
			fill(world, area, 4, 2, 6, 6, 2, 7, false, random, STONES);
			fill(world, area, 4, 3, 7, 6, 3, 7, false, random, STONES);

			for (int var6 = 4; var6 <= 6; var6++) {
				place(world, LegacyBlocks.STONE_BRICK_STAIRS, var15, var6, 1, 4, area);
				place(world, LegacyBlocks.STONE_BRICK_STAIRS, var15, var6, 2, 5, area);
				place(world, LegacyBlocks.STONE_BRICK_STAIRS, var15, var6, 3, 6, area);
			}

			byte var16 = 2;
			byte var7 = 0;
			byte var8 = 3;
			byte var9 = 1;
			switch (this.direction) {
				case 0:
					var16 = 0;
					var7 = 2;
					break;
				case 1:
					var16 = 1;
					var7 = 3;
					var8 = 0;
					var9 = 2;
				case 2:
				default:
					break;
				case 3:
					var16 = 3;
					var7 = 1;
					var8 = 0;
					var9 = 2;
			}

			place(world, LegacyBlocks.END_PORTAL_FRAME, var16 + (random.nextFloat() > 0.9F ? 4 : 0), 4, 3, 8, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var16 + (random.nextFloat() > 0.9F ? 4 : 0), 5, 3, 8, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var16 + (random.nextFloat() > 0.9F ? 4 : 0), 6, 3, 8, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var7 + (random.nextFloat() > 0.9F ? 4 : 0), 4, 3, 12, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var7 + (random.nextFloat() > 0.9F ? 4 : 0), 5, 3, 12, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var7 + (random.nextFloat() > 0.9F ? 4 : 0), 6, 3, 12, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var8 + (random.nextFloat() > 0.9F ? 4 : 0), 3, 3, 9, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var8 + (random.nextFloat() > 0.9F ? 4 : 0), 3, 3, 10, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var8 + (random.nextFloat() > 0.9F ? 4 : 0), 3, 3, 11, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var9 + (random.nextFloat() > 0.9F ? 4 : 0), 7, 3, 9, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var9 + (random.nextFloat() > 0.9F ? 4 : 0), 7, 3, 10, area);
			place(world, LegacyBlocks.END_PORTAL_FRAME, var9 + (random.nextFloat() > 0.9F ? 4 : 0), 7, 3, 11, area);
			if (!this.a) {
				var4 = worldY(3);
				int var10 = worldX(5, 6);
				int var11 = worldZ(5, 6);
				if (area.contains(var10, var4, var11)) {
					this.a = true;
					world.setBlock(var10, var4, var11, LegacyBlocks.SPAWNER, 0);
					if (world.getBlockEntity(var10, var4, var11) instanceof SpawnerBlockEntity spawner) {
						spawner.setEntityId(EntityTypes.SILVERFISH, world.level().getRandom());
					}
				}
			}

			return true;
		}
	}


	/** ComponentStrongholdPrison: iron-barred cells. */
	private static class Prison extends StrongholdPiece {

		public Prison(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 1);
		}

		static @Nullable Prison create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -1, 0, 9, 5, 11, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new Prison(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 8, 4, 10, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 1, 0);
			fill(world, area, 1, 1, 10, 3, 3, 10, 0, 0, false);
			fill(world, area, 4, 1, 1, 4, 3, 1, false, random, STONES);
			fill(world, area, 4, 1, 3, 4, 3, 3, false, random, STONES);
			fill(world, area, 4, 1, 7, 4, 3, 7, false, random, STONES);
			fill(world, area, 4, 1, 9, 4, 3, 9, false, random, STONES);
			fill(world, area, 4, 1, 4, 4, 3, 6, LegacyBlocks.IRON_BARS, LegacyBlocks.IRON_BARS, false);
			fill(world, area, 5, 1, 5, 7, 3, 5, LegacyBlocks.IRON_BARS, LegacyBlocks.IRON_BARS, false);
			place(world, LegacyBlocks.IRON_BARS, 0, 4, 3, 2, area);
			place(world, LegacyBlocks.IRON_BARS, 0, 4, 3, 8, area);
			place(world, LegacyBlocks.IRON_DOOR, meta(LegacyBlocks.IRON_DOOR, 3), 4, 1, 2, area);
			place(world, LegacyBlocks.IRON_DOOR, meta(LegacyBlocks.IRON_DOOR, 3) + 8, 4, 2, 2, area);
			place(world, LegacyBlocks.IRON_DOOR, meta(LegacyBlocks.IRON_DOOR, 3), 4, 1, 8, area);
			place(world, LegacyBlocks.IRON_DOOR, meta(LegacyBlocks.IRON_DOOR, 3) + 8, 4, 2, 8, area);
			return true;
		}
	}


	/** ComponentStrongholdRoomCrossing: a room with a pillar, a fountain or a storage loft. */
	private static class RoomCrossing extends StrongholdPiece {
		private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.IRON_INGOT, 1, 5, 10),
			LegacyLoot.of(Items.GOLD_INGOT, 1, 3, 5),
			LegacyLoot.of(Items.REDSTONE, 4, 9, 5),
			LegacyLoot.of(Items.COAL, 3, 8, 10),
			LegacyLoot.of(Items.BREAD, 1, 3, 15),
			LegacyLoot.of(Items.APPLE, 1, 3, 15),
			LegacyLoot.of(Items.IRON_PICKAXE, 1, 1, 1)};
		protected int a;

		public RoomCrossing(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
			this.a = var2.nextInt(5);
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 4, 1);
			nextLeft(var1, var2, var3, 1, 4);
			nextRight(var1, var2, var3, 1, 4);
		}

		static @Nullable RoomCrossing create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -4, -1, 0, 11, 7, 11, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new RoomCrossing(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 10, 6, 10, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 4, 1, 0);
			fill(world, area, 4, 1, 10, 6, 3, 10, 0, 0, false);
			fill(world, area, 0, 1, 4, 0, 3, 6, 0, 0, false);
			fill(world, area, 10, 1, 4, 10, 3, 6, 0, 0, false);
			switch (this.a) {
				case 0:
					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 1, 5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 2, 5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 3, 5, area);
					place(world, LegacyBlocks.TORCH, 0, 4, 3, 5, area);
					place(world, LegacyBlocks.TORCH, 0, 6, 3, 5, area);
					place(world, LegacyBlocks.TORCH, 0, 5, 3, 4, area);
					place(world, LegacyBlocks.TORCH, 0, 5, 3, 6, area);
					place(world, LegacyBlocks.SLAB, 0, 4, 1, 4, area);
					place(world, LegacyBlocks.SLAB, 0, 4, 1, 5, area);
					place(world, LegacyBlocks.SLAB, 0, 4, 1, 6, area);
					place(world, LegacyBlocks.SLAB, 0, 6, 1, 4, area);
					place(world, LegacyBlocks.SLAB, 0, 6, 1, 5, area);
					place(world, LegacyBlocks.SLAB, 0, 6, 1, 6, area);
					place(world, LegacyBlocks.SLAB, 0, 5, 1, 4, area);
					place(world, LegacyBlocks.SLAB, 0, 5, 1, 6, area);
					break;
				case 1:
					for (int var8 = 0; var8 < 5; var8++) {
						place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 1, 3 + var8, area);
						place(world, LegacyBlocks.STONE_BRICKS, 0, 7, 1, 3 + var8, area);
						place(world, LegacyBlocks.STONE_BRICKS, 0, 3 + var8, 1, 3, area);
						place(world, LegacyBlocks.STONE_BRICKS, 0, 3 + var8, 1, 7, area);
					}

					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 1, 5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 2, 5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 5, 3, 5, area);
					place(world, LegacyBlocks.FLOWING_WATER, 0, 5, 4, 5, area);
					break;
				case 2:
					for (int var4 = 1; var4 <= 9; var4++) {
						place(world, LegacyBlocks.COBBLESTONE, 0, 1, 3, var4, area);
						place(world, LegacyBlocks.COBBLESTONE, 0, 9, 3, var4, area);
					}

					for (int var5 = 1; var5 <= 9; var5++) {
						place(world, LegacyBlocks.COBBLESTONE, 0, var5, 3, 1, area);
						place(world, LegacyBlocks.COBBLESTONE, 0, var5, 3, 9, area);
					}

					place(world, LegacyBlocks.COBBLESTONE, 0, 5, 1, 4, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 5, 1, 6, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 5, 3, 4, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 5, 3, 6, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 4, 1, 5, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 6, 1, 5, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 4, 3, 5, area);
					place(world, LegacyBlocks.COBBLESTONE, 0, 6, 3, 5, area);

					for (int var6 = 1; var6 <= 3; var6++) {
						place(world, LegacyBlocks.COBBLESTONE, 0, 4, var6, 4, area);
						place(world, LegacyBlocks.COBBLESTONE, 0, 6, var6, 4, area);
						place(world, LegacyBlocks.COBBLESTONE, 0, 4, var6, 6, area);
						place(world, LegacyBlocks.COBBLESTONE, 0, 6, var6, 6, area);
					}

					place(world, LegacyBlocks.TORCH, 0, 5, 3, 5, area);

					for (int var7 = 2; var7 <= 8; var7++) {
						place(world, LegacyBlocks.PLANKS, 0, 2, 3, var7, area);
						place(world, LegacyBlocks.PLANKS, 0, 3, 3, var7, area);
						if (var7 <= 3 || var7 >= 7) {
							place(world, LegacyBlocks.PLANKS, 0, 4, 3, var7, area);
							place(world, LegacyBlocks.PLANKS, 0, 5, 3, var7, area);
							place(world, LegacyBlocks.PLANKS, 0, 6, 3, var7, area);
						}

						place(world, LegacyBlocks.PLANKS, 0, 7, 3, var7, area);
						place(world, LegacyBlocks.PLANKS, 0, 8, 3, var7, area);
					}

					place(world, LegacyBlocks.LADDER, meta(LegacyBlocks.LADDER, 4), 9, 1, 3, area);
					place(world, LegacyBlocks.LADDER, meta(LegacyBlocks.LADDER, 4), 9, 2, 3, area);
					place(world, LegacyBlocks.LADDER, meta(LegacyBlocks.LADDER, 4), 9, 3, 3, area);
					chest(world, area, random, 3, 4, 8, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 1 + random.nextInt(4));
			}

			return true;
		}
	}


	/** ComponentStrongholdStairsStraight: a straight staircase down. */
	private static class StraightStairs extends StrongholdPiece {

		public StraightStairs(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.entryDoor = randomDoor(var2);
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 1);
		}

		static @Nullable StraightStairs create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -7, 0, 5, 11, 8, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new StraightStairs(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}

			fill(world, area, 0, 0, 0, 4, 10, 7, true, random, STONES);
			entrance(world, random, area, this.entryDoor, 1, 7, 0);
			entrance(world, random, area, Door.OPENING, 1, 1, 7);
			int var4 = meta(LegacyBlocks.COBBLESTONE_STAIRS, 2);

			for (int var5 = 0; var5 < 6; var5++) {
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 1, 6 - var5, 1 + var5, area);
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 2, 6 - var5, 1 + var5, area);
				place(world, LegacyBlocks.COBBLESTONE_STAIRS, var4, 3, 6 - var5, 1 + var5, area);
				if (var5 < 5) {
					place(world, LegacyBlocks.STONE_BRICKS, 0, 1, 5 - var5, 1 + var5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 2, 5 - var5, 1 + var5, area);
					place(world, LegacyBlocks.STONE_BRICKS, 0, 3, 5 - var5, 1 + var5, area);
				}
			}

			return true;
		}
	}

}
