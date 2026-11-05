package dev.retroeras.worldgen.release.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * 1.6.4's nether fortresses (MapGenNetherBridge, StructureNetherBridgePieces): long bridges of nether brick with castle
 * rooms, blaze spawners, nether wart and chests, one in about a third of each 16x16-chunk region.
 */
public final class Fortress extends LegacyStructureFeature {
	/** The loot of ComponentNetherBridgePiece. */
	private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.DIAMOND, 1, 3, 5),
			LegacyLoot.of(Items.IRON_INGOT, 1, 5, 5),
			LegacyLoot.of(Items.GOLD_INGOT, 1, 3, 15),
			LegacyLoot.of(Items.GOLDEN_SWORD, 1, 1, 5),
			LegacyLoot.of(Items.GOLDEN_CHESTPLATE, 1, 1, 5),
			LegacyLoot.of(Items.FLINT_AND_STEEL, 1, 1, 5),
			LegacyLoot.of(Items.NETHER_WART, 3, 7, 5),
			LegacyLoot.of(Items.SADDLE, 1, 1, 10),
			LegacyLoot.of(Items.GOLDEN_HORSE_ARMOR, 1, 1, 8),
			LegacyLoot.of(Items.IRON_HORSE_ARMOR, 1, 1, 5),
			LegacyLoot.of(Items.DIAMOND_HORSE_ARMOR, 1, 1, 3)
	};

	public Fortress(long seed) {
		super(seed);
	}

	/** One fortress per 16x16-chunk region, in a third of them, at a random chunk 4 to 11 chunks into the region. */
	@Override
	protected boolean canSpawnAt(Random random, int chunkX, int chunkZ) {
		int regionX = chunkX >> 4;
		int regionZ = chunkZ >> 4;
		random.setSeed(regionX ^ regionZ << 4 ^ seed);
		random.nextInt();
		if (random.nextInt(3) != 0) {
			return false;
		}
		return chunkX == (regionX << 4) + 4 + random.nextInt(8) && chunkZ == (regionZ << 4) + 4 + random.nextInt(8);
	}

	@Override
	protected LegacyStart createStart(Random random, int chunkX, int chunkZ) {
		return new FortressStart(random, chunkX, chunkZ);
	}

	/**
	 * MapGenStructure.isInsideStructure and hasStructureAt as the 1.6.4 Nether used them for spawning: inside a piece, or
	 * anywhere in a fortress's bounds standing on nether brick.
	 */
	public boolean spawnsFortressMobs(BlockPos pos, boolean onNetherBrick) {
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		for (int cx = chunkX - 8; cx <= chunkX + 8; cx++) {
			for (int cz = chunkZ - 8; cz <= chunkZ + 8; cz++) {
				LegacyStart start = startAt(cx, cz).orElse(null);
				if (start == null || !start.isValid() || !start.box().contains(pos.getX(), pos.getY(), pos.getZ())) {
					continue;
				}
				if (onNetherBrick) {
					return true;
				}
				for (LegacyPiece piece : start.pieces()) {
					if (piece.box().contains(pos.getX(), pos.getY(), pos.getZ())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** MapGenNetherBridge.Start: laid out from a bridge crossing, then raised to between y = 48 and 70. */
	private static final class FortressStart extends LegacyStart {
		FortressStart(Random random, int chunkX, int chunkZ) {
			Start start = new Start(random, (chunkX << 4) + 2, (chunkZ << 4) + 2);
			pieces.add(start);
			start.addChildren(start, pieces, random);
			List<LegacyPiece> pending = start.pending;
			while (!pending.isEmpty()) {
				pending.remove(random.nextInt(pending.size())).addChildren(start, pieces, random);
			}
			updateBox();
			setRandomHeight(random, 48, 70);
		}
	}

	/** StructureNetherBridgePieceWeight. */
	private static final class PieceWeight {
		final Class<? extends FortressPiece> type;
		final int weight;
		int placed;
		final int limit;
		final boolean repeatable;

		PieceWeight(Class<? extends FortressPiece> type, int weight, int limit, boolean repeatable) {
			this.type = type;
			this.weight = weight;
			this.limit = limit;
			this.repeatable = repeatable;
		}

		PieceWeight(Class<? extends FortressPiece> type, int weight, int limit) {
			this(type, weight, limit, false);
		}

		boolean canSpawnMore() {
			return limit == 0 || placed < limit;
		}
	}

	/**
	 * The bridge pieces and the castle pieces. 1.6.4 kept these in static arrays and only reset the counts for each new
	 * fortress, so they are rebuilt here for each one instead, which comes to the same thing.
	 */
	private static List<PieceWeight> bridgeWeights() {
		List<PieceWeight> weights = new ArrayList<>();
		weights.add(new PieceWeight(BridgeStraight.class, 30, 0, true));
		weights.add(new PieceWeight(BridgeCrossing.class, 10, 4));
		weights.add(new PieceWeight(RoomCrossing.class, 10, 4));
		weights.add(new PieceWeight(StairsRoom.class, 10, 3));
		weights.add(new PieceWeight(Throne.class, 5, 2));
		weights.add(new PieceWeight(CastleEntrance.class, 5, 1));
		return weights;
	}

	private static List<PieceWeight> castleWeights() {
		List<PieceWeight> weights = new ArrayList<>();
		weights.add(new PieceWeight(Corridor.class, 25, 0, true));
		weights.add(new PieceWeight(CorridorCrossing.class, 15, 5));
		weights.add(new PieceWeight(CorridorRightTurn.class, 5, 10));
		weights.add(new PieceWeight(CorridorLeftTurn.class, 5, 10));
		weights.add(new PieceWeight(CorridorStairs.class, 10, 3, true));
		weights.add(new PieceWeight(CorridorBalcony.class, 7, 2));
		weights.add(new PieceWeight(StalkRoom.class, 5, 2));
		return weights;
	}

	private static @Nullable FortressPiece create(Class<? extends FortressPiece> type, List<LegacyPiece> pieces, Random random, int x, int y, int z,
			int direction, int depth) {
		if (type == BridgeStraight.class) {
			return BridgeStraight.create(pieces, random, x, y, z, direction, depth);
		} else if (type == BridgeCrossing.class) {
			return BridgeCrossing.create(pieces, random, x, y, z, direction, depth);
		} else if (type == RoomCrossing.class) {
			return RoomCrossing.create(pieces, random, x, y, z, direction, depth);
		} else if (type == StairsRoom.class) {
			return StairsRoom.create(pieces, random, x, y, z, direction, depth);
		} else if (type == Throne.class) {
			return Throne.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CastleEntrance.class) {
			return CastleEntrance.create(pieces, random, x, y, z, direction, depth);
		} else if (type == Corridor.class) {
			return Corridor.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CorridorRightTurn.class) {
			return CorridorRightTurn.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CorridorLeftTurn.class) {
			return CorridorLeftTurn.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CorridorStairs.class) {
			return CorridorStairs.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CorridorBalcony.class) {
			return CorridorBalcony.create(pieces, random, x, y, z, direction, depth);
		} else if (type == CorridorCrossing.class) {
			return CorridorCrossing.create(pieces, random, x, y, z, direction, depth);
		} else if (type == StalkRoom.class) {
			return StalkRoom.create(pieces, random, x, y, z, direction, depth);
		}
		return null;
	}

	/** ComponentNetherBridgePiece: a fortress piece, and how it picks the pieces leading off it. */
	private abstract static class FortressPiece extends LegacyPiece {
		protected FortressPiece(int depth) {
			super(depth);
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

		/** getNextComponent: a weighted random piece, or a dead end if none fits. */
		private static @Nullable FortressPiece nextPiece(Start start, List<PieceWeight> weights, List<LegacyPiece> pieces, Random random, int x, int y,
				int z, int direction, int depth) {
			int total = totalWeight(weights);
			boolean tryPieces = total > 0 && depth <= 30;
			int attempts = 0;
			while (attempts < 5 && tryPieces) {
				attempts++;
				int roll = random.nextInt(total);
				for (PieceWeight weight : weights) {
					roll -= weight.weight;
					if (roll < 0) {
						if (!weight.canSpawnMore() || weight == start.lastWeight && !weight.repeatable) {
							break;
						}
						FortressPiece piece = create(weight.type, pieces, random, x, y, z, direction, depth);
						if (piece != null) {
							weight.placed++;
							start.lastWeight = weight;
							if (!weight.canSpawnMore()) {
								weights.remove(weight);
							}
							return piece;
						}
					}
				}
			}
			return BridgeEnd.create(pieces, random, x, y, z, direction, depth);
		}

		/** getNextComponent: the next piece, unless it would reach more than 112 blocks from the start. */
		private static @Nullable LegacyPiece next(Start start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth,
				boolean castle) {
			if (Math.abs(x - start.box().minX) <= 112 && Math.abs(z - start.box().minZ) <= 112) {
				FortressPiece piece = nextPiece(start, castle ? start.castleWeights : start.bridgeWeights, pieces, random, x, y, z, direction, depth + 1);
				if (piece != null) {
					pieces.add(piece);
					start.pending.add(piece);
				}
				return piece;
			}
			return BridgeEnd.create(pieces, random, x, y, z, direction, depth);
		}

		/** getNextComponentNormal: the piece straight ahead. */
		protected @Nullable LegacyPiece nextForward(LegacyPiece start, List<LegacyPiece> pieces, Random random, int offset, int dy, boolean castle) {
			return switch (direction) {
				case 0 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.maxZ + 1, direction, depth(), castle);
				case 1 -> next((Start) start, pieces, random, box.minX - 1, box.minY + dy, box.minZ + offset, direction, depth(), castle);
				case 2 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.minZ - 1, direction, depth(), castle);
				case 3 -> next((Start) start, pieces, random, box.maxX + 1, box.minY + dy, box.minZ + offset, direction, depth(), castle);
				default -> null;
			};
		}

		/** getNextComponentX: a piece off the negative side. */
		protected @Nullable LegacyPiece nextLeft(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset, boolean castle) {
			return switch (direction) {
				case 0, 2 -> next((Start) start, pieces, random, box.minX - 1, box.minY + dy, box.minZ + offset, 1, depth(), castle);
				case 1, 3 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.minZ - 1, 2, depth(), castle);
				default -> null;
			};
		}

		/** getNextComponentZ: a piece off the positive side. */
		protected @Nullable LegacyPiece nextRight(LegacyPiece start, List<LegacyPiece> pieces, Random random, int dy, int offset, boolean castle) {
			return switch (direction) {
				case 0, 2 -> next((Start) start, pieces, random, box.maxX + 1, box.minY + dy, box.minZ + offset, 3, depth(), castle);
				case 1, 3 -> next((Start) start, pieces, random, box.minX + offset, box.minY + dy, box.maxZ + 1, 0, depth(), castle);
				default -> null;
			};
		}

		protected static boolean isAboveGround(@Nullable LegacyBox box) {
			return box != null && box.minY > 10;
		}
	}

	/** ComponentNetherBridgeStartPiece: the bridge crossing a fortress starts from, and what it needs while it is laid out. */
	private static final class Start extends BridgeCrossing {
		final List<PieceWeight> bridgeWeights = bridgeWeights();
		final List<PieceWeight> castleWeights = castleWeights();
		@Nullable PieceWeight lastWeight;
		final List<LegacyPiece> pending = new ArrayList<>();

		Start(Random random, int x, int z) {
			super(random, x, z);
		}
	}

	private static class BridgeCrossing extends FortressPiece {
		public BridgeCrossing(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		protected BridgeCrossing(Random var1, int var2, int var3) {
			super(0);
			this.direction = var1.nextInt(4);
			switch (this.direction) {
				case 0:
				case 2:
					this.box = new LegacyBox(var2, 64, var3, var2 + 19 - 1, 73, var3 + 19 - 1);
					break;
				default:
					this.box = new LegacyBox(var2, 64, var3, var2 + 19 - 1, 73, var3 + 19 - 1);
			}
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 8, 3, false);
			nextLeft(var1, var2, var3, 3, 8, false);
			nextRight(var1, var2, var3, 3, 8, false);
		}

		static @Nullable BridgeCrossing create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -8, -3, 0, 19, 10, 19, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new BridgeCrossing(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 7, 3, 0, 11, 4, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 3, 7, 18, 4, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 5, 0, 10, 7, 18, 0, 0, false);
			fill(world, area, 0, 5, 8, 18, 7, 10, 0, 0, false);
			fill(world, area, 7, 5, 0, 7, 5, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 7, 5, 11, 7, 5, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 0, 11, 5, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 11, 11, 5, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 7, 7, 5, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 7, 18, 5, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 11, 7, 5, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 11, 18, 5, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 7, 2, 0, 11, 2, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 7, 2, 13, 11, 2, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 7, 0, 0, 11, 1, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 7, 0, 15, 11, 1, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var4 = 7; var4 <= 11; var4++) {
				for (int var5 = 0; var5 <= 2; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, 18 - var5, area);
				}
			}

			fill(world, area, 0, 2, 7, 5, 2, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 13, 2, 7, 18, 2, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 0, 7, 3, 1, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 15, 0, 7, 18, 1, 11, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var6 = 0; var6 <= 2; var6++) {
				for (int var7 = 7; var7 <= 11; var7++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var6, -1, var7, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, 18 - var6, -1, var7, area);
				}
			}

			return true;
		}
	}

	private static class BridgeEnd extends FortressPiece {
		private int b;

		public BridgeEnd(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
			this.b = var2.nextInt();
		}

		static @Nullable BridgeEnd create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -3, 0, 5, 10, 8, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new BridgeEnd(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			Random var4 = new Random(this.b);

			for (int var5 = 0; var5 <= 4; var5++) {
				for (int var6 = 3; var6 <= 4; var6++) {
					int var7 = var4.nextInt(8);
					fill(world, area, var5, var6, 0, var5, var6, var7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				}
			}

			int var8 = var4.nextInt(8);
			fill(world, area, 0, 5, 0, 0, 5, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			var8 = var4.nextInt(8);
			fill(world, area, 4, 5, 0, 4, 5, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var10 = 0; var10 <= 4; var10++) {
				int var12 = var4.nextInt(5);
				fill(world, area, var10, 2, 0, var10, 2, var12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			}

			for (int var11 = 0; var11 <= 4; var11++) {
				for (int var13 = 0; var13 <= 1; var13++) {
					int var14 = var4.nextInt(3);
					fill(world, area, var11, var13, 0, var11, var13, var14, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				}
			}

			return true;
		}
	}

	private static class BridgeStraight extends FortressPiece {
		public BridgeStraight(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 3, false);
		}

		static @Nullable BridgeStraight create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -3, 0, 5, 10, 19, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new BridgeStraight(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 3, 0, 4, 4, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 5, 0, 3, 7, 18, 0, 0, false);
			fill(world, area, 0, 5, 0, 0, 5, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 5, 0, 4, 5, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 4, 2, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 13, 4, 2, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 0, 0, 4, 1, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 0, 15, 4, 1, 18, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var4 = 0; var4 <= 4; var4++) {
				for (int var5 = 0; var5 <= 2; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, 18 - var5, area);
				}
			}

			fill(world, area, 0, 1, 1, 0, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 4, 0, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 14, 0, 4, 14, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 1, 17, 0, 4, 17, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 1, 1, 4, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 3, 4, 4, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 3, 14, 4, 4, 14, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 1, 17, 4, 4, 17, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			return true;
		}
	}

	private static class CorridorStairs extends FortressPiece {
		public CorridorStairs(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 0, true);
		}

		static @Nullable CorridorStairs create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, -7, 0, 5, 14, 10, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CorridorStairs(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			int var4 = meta(LegacyBlocks.NETHER_BRICK_STAIRS, 2);

			for (int var5 = 0; var5 <= 9; var5++) {
				int var6 = Math.max(1, 7 - var5);
				int var7 = Math.min(Math.max(var6 + 5, 14 - var5), 13);
				int var8 = var5;
				fill(world, area, 0, 0, var8, 4, var6, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				fill(world, area, 1, var6 + 1, var8, 3, var7 - 1, var8, 0, 0, false);
				if (var5 <= 6) {
					place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var4, 1, var6 + 1, var8, area);
					place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var4, 2, var6 + 1, var8, area);
					place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var4, 3, var6 + 1, var8, area);
				}

				fill(world, area, 0, var7, var8, 4, var7, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				fill(world, area, 0, var6 + 1, var8, 0, var7 - 1, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				fill(world, area, 4, var6 + 1, var8, 4, var7 - 1, var8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				if ((var5 & 1) == 0) {
					fill(world, area, 0, var6 + 2, var8, 0, var6 + 3, var8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
					fill(world, area, 4, var6 + 2, var8, 4, var6 + 3, var8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				}

				for (int var9 = 0; var9 <= 4; var9++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var9, -1, var8, area);
				}
			}

			return true;
		}
	}

	private static class CorridorBalcony extends FortressPiece {
		public CorridorBalcony(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			byte var4 = 1;
			if (this.direction == 1 || this.direction == 2) {
				var4 = 5;
			}

			nextLeft(var1, var2, var3, 0, var4, var3.nextInt(8) > 0);
			nextRight(var1, var2, var3, 0, var4, var3.nextInt(8) > 0);
		}

		static @Nullable CorridorBalcony create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -3, 0, 0, 9, 7, 9, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CorridorBalcony(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 8, 1, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 8, 5, 8, 0, 0, false);
			fill(world, area, 0, 6, 0, 8, 6, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 2, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 2, 0, 8, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 3, 0, 1, 4, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 7, 3, 0, 7, 4, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 2, 4, 8, 2, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 1, 4, 2, 2, 4, 0, 0, false);
			fill(world, area, 6, 1, 4, 7, 2, 4, 0, 0, false);
			fill(world, area, 0, 3, 8, 8, 3, 8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 6, 0, 3, 7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 8, 3, 6, 8, 3, 7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 4, 0, 5, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 3, 4, 8, 5, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 3, 5, 2, 5, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 3, 5, 7, 5, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 4, 5, 1, 5, 5, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 7, 4, 5, 7, 5, 5, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);

			for (int var4 = 0; var4 <= 5; var4++) {
				for (int var5 = 0; var5 <= 8; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var5, -1, var4, area);
				}
			}

			return true;
		}
	}

	private static class CastleEntrance extends FortressPiece {
		public CastleEntrance(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 5, 3, true);
		}

		static @Nullable CastleEntrance create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -5, -3, 0, 13, 14, 13, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CastleEntrance(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 3, 0, 12, 4, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 0, 12, 13, 12, 0, 0, false);
			fill(world, area, 0, 5, 0, 1, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 0, 12, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 11, 4, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 5, 11, 10, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 9, 11, 7, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 0, 4, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 5, 0, 10, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 9, 0, 7, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 11, 2, 10, 12, 10, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 8, 0, 7, 8, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);

			for (byte var4 = 1; var4 <= 11; var4 += 2) {
				fill(world, area, var4, 10, 0, var4, 11, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, var4, 10, 12, var4, 11, 12, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 0, 10, var4, 0, 11, var4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 12, 10, var4, 12, 11, var4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, var4, 13, 0, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, var4, 13, 12, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, 0, 13, var4, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, 12, 13, var4, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, var4 + 1, 13, 0, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, var4 + 1, 13, 12, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, var4 + 1, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 12, 13, var4 + 1, area);
			}

			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 0, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 12, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 0, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 12, 13, 0, area);

			for (byte var7 = 3; var7 <= 9; var7 += 2) {
				fill(world, area, 1, 7, var7, 1, 8, var7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 11, 7, var7, 11, 8, var7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			}

			fill(world, area, 4, 2, 0, 8, 2, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 4, 12, 2, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 0, 0, 8, 1, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 0, 9, 8, 1, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 0, 4, 3, 1, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 9, 0, 4, 12, 1, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var8 = 4; var8 <= 8; var8++) {
				for (int var5 = 0; var5 <= 2; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var8, -1, var5, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var8, -1, 12 - var5, area);
				}
			}

			for (int var9 = 0; var9 <= 2; var9++) {
				for (int var11 = 4; var11 <= 8; var11++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var9, -1, var11, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, 12 - var9, -1, var11, area);
				}
			}

			fill(world, area, 5, 5, 5, 7, 5, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 1, 6, 6, 4, 6, 0, 0, false);
			place(world, LegacyBlocks.NETHER_BRICKS, 0, 6, 0, 6, area);
			place(world, LegacyBlocks.FLOWING_LAVA, 0, 6, 5, 6, area);
			int var10 = worldX(6, 6);
			int var12 = worldY(5);
			int var6 = worldZ(6, 6);
			// 1.6.4 ticked this lava at once so it flowed while the piece was built; here it flows on its first tick.
			if (area.contains(var10, var12, var6)) {
				world.level().scheduleTick(new BlockPos(var10, var12, var6), Fluids.LAVA, 0);
			}

			return true;
		}
	}

	private static class CorridorCrossing extends FortressPiece {
		public CorridorCrossing(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 0, true);
			nextLeft(var1, var2, var3, 0, 1, true);
			nextRight(var1, var2, var3, 0, 1, true);
		}

		static @Nullable CorridorCrossing create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, 0, 0, 5, 7, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CorridorCrossing(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 4, 1, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 4, 5, 4, 0, 0, false);
			fill(world, area, 0, 2, 0, 0, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 2, 0, 4, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 4, 0, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 2, 4, 4, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 6, 0, 4, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var4 = 0; var4 <= 4; var4++) {
				for (int var5 = 0; var5 <= 4; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
				}
			}

			return true;
		}
	}

	private static class CorridorLeftTurn extends FortressPiece {
		private boolean b;

		public CorridorLeftTurn(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
			this.b = var2.nextInt(3) == 0;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextLeft(var1, var2, var3, 0, 1, true);
		}

		static @Nullable CorridorLeftTurn create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, 0, 0, 5, 7, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CorridorLeftTurn(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 4, 1, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 4, 5, 4, 0, 0, false);
			fill(world, area, 4, 2, 0, 4, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 3, 1, 4, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 3, 3, 4, 4, 3, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 2, 0, 0, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 4, 3, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 3, 4, 1, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 3, 3, 4, 3, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICKS, false);
			if (this.b) {
				int var4 = worldY(2);
				int var5 = worldX(3, 3);
				int var6 = worldZ(3, 3);
				if (area.contains(var5, var4, var6)) {
					this.b = false;
					chest(world, area, random, 3, 2, 3, LOOT, 2 + random.nextInt(4));
				}
			}

			fill(world, area, 0, 6, 0, 4, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var7 = 0; var7 <= 4; var7++) {
				for (int var8 = 0; var8 <= 4; var8++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var7, -1, var8, area);
				}
			}

			return true;
		}
	}

	private static class Corridor extends FortressPiece {
		public Corridor(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 1, 0, true);
		}

		static @Nullable Corridor create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, 0, 0, 5, 7, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new Corridor(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 4, 1, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 4, 5, 4, 0, 0, false);
			fill(world, area, 0, 2, 0, 0, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 2, 0, 4, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 3, 1, 0, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 3, 0, 4, 3, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 3, 1, 4, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 3, 3, 4, 4, 3, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 6, 0, 4, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var4 = 0; var4 <= 4; var4++) {
				for (int var5 = 0; var5 <= 4; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
				}
			}

			return true;
		}
	}

	private static class CorridorRightTurn extends FortressPiece {
		private boolean b;

		public CorridorRightTurn(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
			this.b = var2.nextInt(3) == 0;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextRight(var1, var2, var3, 0, 1, true);
		}

		static @Nullable CorridorRightTurn create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -1, 0, 0, 5, 7, 5, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new CorridorRightTurn(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 4, 1, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 4, 5, 4, 0, 0, false);
			fill(world, area, 0, 2, 0, 0, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 3, 1, 0, 4, 1, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 3, 3, 0, 4, 3, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 4, 2, 0, 4, 5, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 2, 4, 4, 5, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 3, 4, 1, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 3, 3, 4, 3, 4, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICKS, false);
			if (this.b) {
				int var4 = worldY(2);
				int var5 = worldX(1, 3);
				int var6 = worldZ(1, 3);
				if (area.contains(var5, var4, var6)) {
					this.b = false;
					chest(world, area, random, 1, 2, 3, LOOT, 2 + random.nextInt(4));
				}
			}

			fill(world, area, 0, 6, 0, 4, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var7 = 0; var7 <= 4; var7++) {
				for (int var8 = 0; var8 <= 4; var8++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var7, -1, var8, area);
				}
			}

			return true;
		}
	}

	private static class StalkRoom extends FortressPiece {
		public StalkRoom(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 5, 3, true);
			nextForward(var1, var2, var3, 5, 11, true);
		}

		static @Nullable StalkRoom create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -5, -3, 0, 13, 14, 13, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new StalkRoom(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 3, 0, 12, 4, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 0, 12, 13, 12, 0, 0, false);
			fill(world, area, 0, 5, 0, 1, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 11, 5, 0, 12, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 11, 4, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 5, 11, 10, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 9, 11, 7, 12, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 0, 4, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 8, 5, 0, 10, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 9, 0, 7, 12, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 11, 2, 10, 12, 10, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (byte var4 = 1; var4 <= 11; var4 += 2) {
				fill(world, area, var4, 10, 0, var4, 11, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, var4, 10, 12, var4, 11, 12, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 0, 10, var4, 0, 11, var4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 12, 10, var4, 12, 11, var4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, var4, 13, 0, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, var4, 13, 12, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, 0, 13, var4, area);
				place(world, LegacyBlocks.NETHER_BRICKS, 0, 12, 13, var4, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, var4 + 1, 13, 0, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, var4 + 1, 13, 12, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, var4 + 1, area);
				place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 12, 13, var4 + 1, area);
			}

			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 0, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 12, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 0, 13, 0, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 12, 13, 0, area);

			for (byte var9 = 3; var9 <= 9; var9 += 2) {
				fill(world, area, 1, 7, var9, 1, 8, var9, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
				fill(world, area, 11, 7, var9, 11, 8, var9, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			}

			int var10 = meta(LegacyBlocks.NETHER_BRICK_STAIRS, 3);

			for (int var5 = 0; var5 <= 6; var5++) {
				int var6 = var5 + 4;

				for (int var7 = 5; var7 <= 7; var7++) {
					place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var10, var7, 5 + var5, var6, area);
				}

				if (var6 >= 5 && var6 <= 8) {
					fill(world, area, 5, 5, var6, 7, var5 + 4, var6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				} else if (var6 >= 9 && var6 <= 10) {
					fill(world, area, 5, 8, var6, 7, var5 + 4, var6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
				}

				if (var5 >= 1) {
					fill(world, area, 5, 6 + var5, var6, 7, 9 + var5, var6, 0, 0, false);
				}
			}

			for (int var11 = 5; var11 <= 7; var11++) {
				place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var10, var11, 12, 11, area);
			}

			fill(world, area, 5, 6, 7, 5, 7, 7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 7, 6, 7, 7, 7, 7, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 5, 13, 12, 7, 13, 12, 0, 0, false);
			fill(world, area, 2, 5, 2, 3, 5, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 9, 3, 5, 10, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 4, 2, 5, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 9, 5, 2, 10, 5, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 9, 5, 9, 10, 5, 10, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 10, 5, 4, 10, 5, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			int var12 = meta(LegacyBlocks.NETHER_BRICK_STAIRS, 0);
			int var13 = meta(LegacyBlocks.NETHER_BRICK_STAIRS, 1);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var13, 4, 5, 2, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var13, 4, 5, 3, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var13, 4, 5, 9, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var13, 4, 5, 10, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var12, 8, 5, 2, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var12, 8, 5, 3, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var12, 8, 5, 9, area);
			place(world, LegacyBlocks.NETHER_BRICK_STAIRS, var12, 8, 5, 10, area);
			fill(world, area, 3, 4, 4, 4, 4, 8, LegacyBlocks.SOUL_SAND, LegacyBlocks.SOUL_SAND, false);
			fill(world, area, 8, 4, 4, 9, 4, 8, LegacyBlocks.SOUL_SAND, LegacyBlocks.SOUL_SAND, false);
			fill(world, area, 3, 5, 4, 4, 5, 8, LegacyBlocks.NETHER_WART, LegacyBlocks.NETHER_WART, false);
			fill(world, area, 8, 5, 4, 9, 5, 8, LegacyBlocks.NETHER_WART, LegacyBlocks.NETHER_WART, false);
			fill(world, area, 4, 2, 0, 8, 2, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 4, 12, 2, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 0, 0, 8, 1, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 4, 0, 9, 8, 1, 12, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 0, 4, 3, 1, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 9, 0, 4, 12, 1, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);

			for (int var14 = 4; var14 <= 8; var14++) {
				for (int var8 = 0; var8 <= 2; var8++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var14, -1, var8, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var14, -1, 12 - var8, area);
				}
			}

			for (int var15 = 0; var15 <= 2; var15++) {
				for (int var16 = 4; var16 <= 8; var16++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var15, -1, var16, area);
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, 12 - var15, -1, var16, area);
				}
			}

			return true;
		}
	}

	private static class Throne extends FortressPiece {
		private boolean b;

		public Throne(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		static @Nullable Throne create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -2, 0, 0, 7, 8, 9, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new Throne(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 2, 0, 6, 7, 7, 0, 0, false);
			fill(world, area, 1, 0, 0, 5, 1, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 2, 1, 5, 2, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 3, 2, 5, 3, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 4, 3, 5, 4, 7, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 2, 0, 1, 4, 2, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 2, 0, 5, 4, 2, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 5, 2, 1, 5, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 5, 2, 5, 5, 3, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 3, 0, 5, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 5, 3, 6, 5, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 5, 8, 5, 5, 8, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 1, 6, 3, area);
			place(world, LegacyBlocks.NETHER_BRICK_FENCE, 0, 5, 6, 3, area);
			fill(world, area, 0, 6, 3, 0, 6, 8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 6, 6, 3, 6, 6, 8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 1, 6, 8, 5, 7, 8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 2, 8, 8, 4, 8, 8, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			if (!this.b) {
				if (spawner(world, area, 3, 5, 5, EntityTypes.BLAZE)) {
					this.b = true;
				}
			}

			for (int var8 = 0; var8 <= 6; var8++) {
				for (int var9 = 0; var9 <= 6; var9++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var8, -1, var9, area);
				}
			}

			return true;
		}
	}

	private static class RoomCrossing extends FortressPiece {
		public RoomCrossing(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextForward(var1, var2, var3, 2, 0, false);
			nextLeft(var1, var2, var3, 0, 2, false);
			nextRight(var1, var2, var3, 0, 2, false);
		}

		static @Nullable RoomCrossing create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -2, 0, 0, 7, 9, 7, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new RoomCrossing(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 6, 1, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 6, 7, 6, 0, 0, false);
			fill(world, area, 0, 2, 0, 1, 6, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 6, 1, 6, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 2, 0, 6, 6, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 2, 6, 6, 6, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 0, 6, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 5, 0, 6, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 2, 0, 6, 6, 1, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 2, 5, 6, 6, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 6, 0, 4, 6, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 0, 4, 5, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 2, 6, 6, 4, 6, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 6, 4, 5, 6, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 0, 6, 2, 0, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 5, 2, 0, 5, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 6, 6, 2, 6, 6, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 5, 2, 6, 5, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);

			for (int var4 = 0; var4 <= 6; var4++) {
				for (int var5 = 0; var5 <= 6; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
				}
			}

			return true;
		}
	}

	private static class StairsRoom extends FortressPiece {
		public StairsRoom(int var1, Random var2, LegacyBox var3, int var4) {
			super(var1);
			this.direction = var4;
			this.box = var3;
		}

		@Override
		public void addChildren(LegacyPiece var1, List<LegacyPiece> var2, Random var3) {
			nextRight(var1, var2, var3, 6, 2, false);
		}

		static @Nullable StairsRoom create(List<LegacyPiece> var0, Random var1, int var2, int var3, int var4, int var5, int var6) {
			LegacyBox var7 = LegacyBox.oriented(var2, var3, var4, -2, 0, 0, 7, 11, 7, var5);
			return isAboveGround(var7) && LegacyPiece.findIntersecting(var0, var7) == null ? new StairsRoom(var6, var1, var7, var5) : null;
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			fill(world, area, 0, 0, 0, 6, 1, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 0, 6, 10, 6, 0, 0, false);
			fill(world, area, 0, 2, 0, 1, 8, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 5, 2, 0, 6, 8, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 2, 1, 0, 8, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 2, 1, 6, 8, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 2, 6, 5, 8, 6, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 0, 3, 2, 0, 5, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 6, 3, 2, 6, 5, 2, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			fill(world, area, 6, 3, 4, 6, 5, 4, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);
			place(world, LegacyBlocks.NETHER_BRICKS, 0, 5, 2, 5, area);
			fill(world, area, 4, 2, 5, 4, 3, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 3, 2, 5, 3, 4, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 2, 5, 2, 5, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 2, 5, 1, 6, 5, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 1, 7, 1, 5, 7, 4, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 6, 8, 2, 6, 8, 4, 0, 0, false);
			fill(world, area, 2, 6, 0, 4, 8, 0, LegacyBlocks.NETHER_BRICKS, LegacyBlocks.NETHER_BRICKS, false);
			fill(world, area, 2, 5, 0, 4, 5, 0, LegacyBlocks.NETHER_BRICK_FENCE, LegacyBlocks.NETHER_BRICK_FENCE, false);

			for (int var4 = 0; var4 <= 6; var4++) {
				for (int var5 = 0; var5 <= 6; var5++) {
					fillDownwards(world, LegacyBlocks.NETHER_BRICKS, 0, var4, -1, var5, area);
				}
			}

			return true;
		}
	}
}
