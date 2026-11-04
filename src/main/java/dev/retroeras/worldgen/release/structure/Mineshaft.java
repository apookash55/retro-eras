package dev.retroeras.worldgen.release.structure;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.Items;

/** 1.6.4's abandoned mineshafts (MapGenMineshaft, StructureMineshaftPieces). */
public final class Mineshaft extends LegacyStructureFeature {
	private static final LegacyLoot[] LOOT = {
			LegacyLoot.of(Items.IRON_INGOT, 1, 5, 10),
			LegacyLoot.of(Items.GOLD_INGOT, 1, 3, 5),
			LegacyLoot.of(Items.REDSTONE, 4, 9, 5),
			LegacyLoot.of(Items.LAPIS_LAZULI, 4, 9, 5),
			LegacyLoot.of(Items.DIAMOND, 1, 2, 3),
			LegacyLoot.of(Items.COAL, 3, 8, 10),
			LegacyLoot.of(Items.BREAD, 1, 3, 15),
			LegacyLoot.of(Items.IRON_PICKAXE, 1, 1, 1),
			LegacyLoot.of(Items.RAIL, 4, 8, 1),
			LegacyLoot.of(Items.MELON_SEEDS, 2, 4, 10),
			LegacyLoot.of(Items.PUMPKIN_SEEDS, 2, 4, 10),
			LegacyLoot.of(Items.SADDLE, 1, 1, 3),
			LegacyLoot.of(Items.IRON_HORSE_ARMOR, 1, 1, 1)};

	public Mineshaft(long seed) {
		super(seed);
	}

	@Override
	protected boolean canSpawnAt(Random random, int chunkX, int chunkZ) {
		return random.nextDouble() < 0.01 && random.nextInt(80) < Math.max(Math.abs(chunkX), Math.abs(chunkZ));
	}

	@Override
	protected LegacyStart createStart(Random random, int chunkX, int chunkZ) {
		return new Start(random, chunkX, chunkZ);
	}

	private static final class Start extends LegacyStart {
		Start(Random random, int chunkX, int chunkZ) {
			Room room = new Room(0, random, (chunkX << 4) + 2, (chunkZ << 4) + 2);
			pieces.add(room);
			room.addChildren(room, pieces, random);
			updateBox();
			sinkBelowSeaLevel(random, 10);
		}
	}

	private static @Nullable LegacyPiece createPiece(List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth) {
		int roll = random.nextInt(100);
		if (roll >= 80) {
			LegacyBox box = Crossing.findBox(pieces, random, x, y, z, direction);
			if (box != null) {
				return new Crossing(depth, random, box, direction);
			}
		} else if (roll >= 70) {
			LegacyBox box = Stairs.findBox(pieces, random, x, y, z, direction);
			if (box != null) {
				return new Stairs(depth, random, box, direction);
			}
		} else {
			LegacyBox box = Corridor.findBox(pieces, random, x, y, z, direction);
			if (box != null) {
				return new Corridor(depth, random, box, direction);
			}
		}
		return null;
	}

	/** getNextMineShaftComponent: stops eight pieces deep, or 80 blocks from the start. */
	private static @Nullable LegacyPiece next(LegacyPiece start, List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction, int depth) {
		if (depth > 8) {
			return null;
		}
		if (Math.abs(x - start.box().minX) <= 80 && Math.abs(z - start.box().minZ) <= 80) {
			LegacyPiece piece = createPiece(pieces, random, x, y, z, direction, depth + 1);
			if (piece != null) {
				pieces.add(piece);
				piece.addChildren(start, pieces, random);
			}
			return piece;
		}
		return null;
	}

	/** ComponentMineshaftRoom: the dirt-floored cavern the shaft starts from. */
	private static final class Room extends LegacyPiece {
		private final List<LegacyBox> entrances = new LinkedList<>();

		Room(int depth, Random random, int x, int z) {
			super(depth);
			box = new LegacyBox(x, 50, z, x + 7 + random.nextInt(6), 54 + random.nextInt(6), z + 7 + random.nextInt(6));
		}

		@Override
		public void addChildren(LegacyPiece start, List<LegacyPiece> pieces, Random random) {
			int depth = depth();
			int heightRange = box.ySpan() - 3 - 1;
			if (heightRange <= 0) {
				heightRange = 1;
			}

			int offset = 0;
			while (offset < box.xSpan()) {
				offset += random.nextInt(box.xSpan());
				if (offset + 3 > box.xSpan()) {
					break;
				}
				LegacyPiece piece = next(start, pieces, random, box.minX + offset, box.minY + random.nextInt(heightRange) + 1, box.minZ - 1, 2, depth);
				if (piece != null) {
					LegacyBox b = piece.box();
					entrances.add(new LegacyBox(b.minX, b.minY, box.minZ, b.maxX, b.maxY, box.minZ + 1));
				}
				offset += 4;
			}

			offset = 0;
			while (offset < box.xSpan()) {
				offset += random.nextInt(box.xSpan());
				if (offset + 3 > box.xSpan()) {
					break;
				}
				LegacyPiece piece = next(start, pieces, random, box.minX + offset, box.minY + random.nextInt(heightRange) + 1, box.maxZ + 1, 0, depth);
				if (piece != null) {
					LegacyBox b = piece.box();
					entrances.add(new LegacyBox(b.minX, b.minY, box.maxZ - 1, b.maxX, b.maxY, box.maxZ));
				}
				offset += 4;
			}

			offset = 0;
			while (offset < box.zSpan()) {
				offset += random.nextInt(box.zSpan());
				if (offset + 3 > box.zSpan()) {
					break;
				}
				LegacyPiece piece = next(start, pieces, random, box.minX - 1, box.minY + random.nextInt(heightRange) + 1, box.minZ + offset, 1, depth);
				if (piece != null) {
					LegacyBox b = piece.box();
					entrances.add(new LegacyBox(box.minX, b.minY, b.minZ, box.minX + 1, b.maxY, b.maxZ));
				}
				offset += 4;
			}

			offset = 0;
			while (offset < box.zSpan()) {
				offset += random.nextInt(box.zSpan());
				if (offset + 3 > box.zSpan()) {
					break;
				}
				LegacyPiece piece = next(start, pieces, random, box.maxX + 1, box.minY + random.nextInt(heightRange) + 1, box.minZ + offset, 3, depth);
				if (piece != null) {
					LegacyBox b = piece.box();
					entrances.add(new LegacyBox(box.maxX - 1, b.minY, b.minZ, box.maxX, b.maxY, b.maxZ));
				}
				offset += 4;
			}
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}
			fill(world, area, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, LegacyBlocks.DIRT, 0, true);
			fill(world, area, box.minX, box.minY + 1, box.minZ, box.maxX, Math.min(box.minY + 3, box.maxY), box.maxZ, 0, 0, false);
			for (LegacyBox entrance : entrances) {
				fill(world, area, entrance.minX, entrance.maxY - 2, entrance.minZ, entrance.maxX, entrance.maxY, entrance.maxZ, 0, 0, false);
			}
			dome(world, area, box.minX, box.minY + 4, box.minZ, box.maxX, box.maxY, box.maxZ, 0, false);
			return true;
		}
	}

	/** ComponentMineshaftCorridor: a timbered tunnel, sometimes with rails, cobwebs and a cave spider spawner. */
	private static final class Corridor extends LegacyPiece {
		private final boolean hasRails;
		private final boolean hasSpiders;
		private boolean spawnerPlaced;
		private final int sections;

		Corridor(int depth, Random random, LegacyBox box, int direction) {
			super(depth);
			this.direction = direction;
			this.box = box;
			hasRails = random.nextInt(3) == 0;
			hasSpiders = !hasRails && random.nextInt(23) == 0;
			sections = direction != 2 && direction != 0 ? box.xSpan() / 5 : box.zSpan() / 5;
		}

		static @Nullable LegacyBox findBox(List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction) {
			LegacyBox box = new LegacyBox(x, y, z, x, y + 2, z);
			int count;
			for (count = random.nextInt(3) + 2; count > 0; count--) {
				int length = count * 5;
				switch (direction) {
					case 0 -> {
						box.maxX = x + 2;
						box.maxZ = z + (length - 1);
					}
					case 1 -> {
						box.minX = x - (length - 1);
						box.maxZ = z + 2;
					}
					case 2 -> {
						box.maxX = x + 2;
						box.minZ = z - (length - 1);
					}
					case 3 -> {
						box.maxX = x + (length - 1);
						box.maxZ = z + 2;
					}
					default -> {
					}
				}
				if (findIntersecting(pieces, box) == null) {
					break;
				}
			}
			return count > 0 ? box : null;
		}

		@Override
		public void addChildren(LegacyPiece start, List<LegacyPiece> pieces, Random random) {
			int depth = depth();
			int roll = random.nextInt(4);
			switch (direction) {
				case 0 -> {
					if (roll <= 1) {
						next(start, pieces, random, box.minX, box.minY - 1 + random.nextInt(3), box.maxZ + 1, direction, depth);
					} else if (roll == 2) {
						next(start, pieces, random, box.minX - 1, box.minY - 1 + random.nextInt(3), box.maxZ - 3, 1, depth);
					} else {
						next(start, pieces, random, box.maxX + 1, box.minY - 1 + random.nextInt(3), box.maxZ - 3, 3, depth);
					}
				}
				case 1 -> {
					if (roll <= 1) {
						next(start, pieces, random, box.minX - 1, box.minY - 1 + random.nextInt(3), box.minZ, direction, depth);
					} else if (roll == 2) {
						next(start, pieces, random, box.minX, box.minY - 1 + random.nextInt(3), box.minZ - 1, 2, depth);
					} else {
						next(start, pieces, random, box.minX, box.minY - 1 + random.nextInt(3), box.maxZ + 1, 0, depth);
					}
				}
				case 2 -> {
					if (roll <= 1) {
						next(start, pieces, random, box.minX, box.minY - 1 + random.nextInt(3), box.minZ - 1, direction, depth);
					} else if (roll == 2) {
						next(start, pieces, random, box.minX - 1, box.minY - 1 + random.nextInt(3), box.minZ, 1, depth);
					} else {
						next(start, pieces, random, box.maxX + 1, box.minY - 1 + random.nextInt(3), box.minZ, 3, depth);
					}
				}
				case 3 -> {
					if (roll <= 1) {
						next(start, pieces, random, box.maxX + 1, box.minY - 1 + random.nextInt(3), box.minZ, direction, depth);
					} else if (roll == 2) {
						next(start, pieces, random, box.maxX - 3, box.minY - 1 + random.nextInt(3), box.minZ - 1, 2, depth);
					} else {
						next(start, pieces, random, box.maxX - 3, box.minY - 1 + random.nextInt(3), box.maxZ + 1, 0, depth);
					}
				}
				default -> {
				}
			}

			if (depth < 8) {
				if (direction != 2 && direction != 0) {
					for (int x = box.minX + 3; x + 3 <= box.maxX; x += 5) {
						int side = random.nextInt(5);
						if (side == 0) {
							next(start, pieces, random, x, box.minY, box.minZ - 1, 2, depth + 1);
						} else if (side == 1) {
							next(start, pieces, random, x, box.minY, box.maxZ + 1, 0, depth + 1);
						}
					}
				} else {
					for (int z = box.minZ + 3; z + 3 <= box.maxZ; z += 5) {
						int side = random.nextInt(5);
						if (side == 0) {
							next(start, pieces, random, box.minX - 1, box.minY, z, 1, depth + 1);
						} else if (side == 1) {
							next(start, pieces, random, box.maxX + 1, box.minY, z, 3, depth + 1);
						}
					}
				}
			}
		}

		/** The mineshaft's chest is a minecart with a chest, standing on a rail. */
		private void minecartChest(LegacyWorld world, LegacyBox area, Random random, int x, int y, int z, LegacyLoot[] loot, int count) {
			int wx = worldX(x, z);
			int wy = worldY(y);
			int wz = worldZ(x, z);
			if (area.contains(wx, wy, wz) && world.getBlockId(wx, wy, wz) == 0) {
				world.setBlock(wx, wy, wz, LegacyBlocks.RAIL, meta(LegacyBlocks.RAIL, random.nextBoolean() ? 1 : 0));
				MinecartChest cart = EntityTypes.CHEST_MINECART.create(world.level().getLevel(), EntitySpawnReason.CHUNK_GENERATION);
				if (cart != null) {
					cart.setPos(wx + 0.5F, wy + 0.5F, wz + 0.5F);
					LegacyLoot.fill(random, loot, cart, count, world.level().registryAccess());
					world.addEntity(cart);
				}
			}
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}
			int length = sections * 5 - 1;
			fill(world, area, 0, 0, 0, 2, 1, length, 0, 0, false);
			fillRandomly(world, area, random, 0.8F, 0, 2, 0, 2, 2, length, 0, 0, false);
			if (hasSpiders) {
				fillRandomly(world, area, random, 0.6F, 0, 0, 0, 2, 1, length, LegacyBlocks.WEB, 0, false);
			}

			for (int section = 0; section < sections; section++) {
				int z = 2 + section * 5;
				fill(world, area, 0, 0, z, 0, 1, z, LegacyBlocks.FENCE, 0, false);
				fill(world, area, 2, 0, z, 2, 1, z, LegacyBlocks.FENCE, 0, false);
				if (random.nextInt(4) == 0) {
					fill(world, area, 0, 2, z, 0, 2, z, LegacyBlocks.PLANKS, 0, false);
					fill(world, area, 2, 2, z, 2, 2, z, LegacyBlocks.PLANKS, 0, false);
				} else {
					fill(world, area, 0, 2, z, 2, 2, z, LegacyBlocks.PLANKS, 0, false);
				}
				placeRandomly(world, area, random, 0.1F, 0, 2, z - 1, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.1F, 2, 2, z - 1, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.1F, 0, 2, z + 1, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.1F, 2, 2, z + 1, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.05F, 0, 2, z - 2, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.05F, 2, 2, z - 2, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.05F, 0, 2, z + 2, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.05F, 2, 2, z + 2, LegacyBlocks.WEB, 0);
				placeRandomly(world, area, random, 0.05F, 1, 2, z - 1, LegacyBlocks.TORCH, 0);
				placeRandomly(world, area, random, 0.05F, 1, 2, z + 1, LegacyBlocks.TORCH, 0);
				if (random.nextInt(100) == 0) {
					minecartChest(world, area, random, 2, 0, z - 1, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 3 + random.nextInt(4));
				}
				if (random.nextInt(100) == 0) {
					minecartChest(world, area, random, 0, 0, z + 1, LegacyLoot.with(LOOT, LegacyLoot.enchantedBook(random)), 3 + random.nextInt(4));
				}

				if (hasSpiders && !spawnerPlaced) {
					int y = worldY(0);
					int along = z - 1 + random.nextInt(3);
					int wx = worldX(1, along);
					int wz = worldZ(1, along);
					if (area.contains(wx, y, wz)) {
						spawnerPlaced = true;
						world.setBlock(wx, y, wz, LegacyBlocks.SPAWNER, 0);
						if (world.getBlockEntity(wx, y, wz) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
							spawner.setEntityId(EntityTypes.CAVE_SPIDER, world.level().getRandom());
						}
					}
				}
			}

			for (int x = 0; x <= 2; x++) {
				for (int z = 0; z <= length; z++) {
					if (getBlock(world, x, -1, z, area) == 0) {
						place(world, LegacyBlocks.PLANKS, 0, x, -1, z, area);
					}
				}
			}

			if (hasRails) {
				for (int z = 0; z <= length; z++) {
					if (getBlock(world, 1, -1, z, area) > 0 && isOpaque(world, 1, -1, z, area)) {
						placeRandomly(world, area, random, 0.7F, 1, 0, z, LegacyBlocks.RAIL, meta(LegacyBlocks.RAIL, 0));
					}
				}
			}
			return true;
		}
	}

	/** ComponentMineshaftCross: a crossing, sometimes two storeys tall. */
	private static final class Crossing extends LegacyPiece {
		private final int corridorDirection;
		private final boolean multiFloor;

		Crossing(int depth, Random random, LegacyBox box, int direction) {
			super(depth);
			this.corridorDirection = direction;
			this.box = box;
			this.multiFloor = box.ySpan() > 3;
		}

		static @Nullable LegacyBox findBox(List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction) {
			LegacyBox box = new LegacyBox(x, y, z, x, y + 2, z);
			if (random.nextInt(4) == 0) {
				box.maxY += 4;
			}
			switch (direction) {
				case 0 -> {
					box.minX = x - 1;
					box.maxX = x + 3;
					box.maxZ = z + 4;
				}
				case 1 -> {
					box.minX = x - 4;
					box.minZ = z - 1;
					box.maxZ = z + 3;
				}
				case 2 -> {
					box.minX = x - 1;
					box.maxX = x + 3;
					box.minZ = z - 4;
				}
				case 3 -> {
					box.maxX = x + 4;
					box.minZ = z - 1;
					box.maxZ = z + 3;
				}
				default -> {
				}
			}
			return findIntersecting(pieces, box) != null ? null : box;
		}

		@Override
		public void addChildren(LegacyPiece start, List<LegacyPiece> pieces, Random random) {
			int depth = depth();
			switch (corridorDirection) {
				case 0 -> {
					next(start, pieces, random, box.minX + 1, box.minY, box.maxZ + 1, 0, depth);
					next(start, pieces, random, box.minX - 1, box.minY, box.minZ + 1, 1, depth);
					next(start, pieces, random, box.maxX + 1, box.minY, box.minZ + 1, 3, depth);
				}
				case 1 -> {
					next(start, pieces, random, box.minX + 1, box.minY, box.minZ - 1, 2, depth);
					next(start, pieces, random, box.minX + 1, box.minY, box.maxZ + 1, 0, depth);
					next(start, pieces, random, box.minX - 1, box.minY, box.minZ + 1, 1, depth);
				}
				case 2 -> {
					next(start, pieces, random, box.minX + 1, box.minY, box.minZ - 1, 2, depth);
					next(start, pieces, random, box.minX - 1, box.minY, box.minZ + 1, 1, depth);
					next(start, pieces, random, box.maxX + 1, box.minY, box.minZ + 1, 3, depth);
				}
				case 3 -> {
					next(start, pieces, random, box.minX + 1, box.minY, box.minZ - 1, 2, depth);
					next(start, pieces, random, box.minX + 1, box.minY, box.maxZ + 1, 0, depth);
					next(start, pieces, random, box.maxX + 1, box.minY, box.minZ + 1, 3, depth);
				}
				default -> {
				}
			}
			if (multiFloor) {
				if (random.nextBoolean()) {
					next(start, pieces, random, box.minX + 1, box.minY + 3 + 1, box.minZ - 1, 2, depth);
				}
				if (random.nextBoolean()) {
					next(start, pieces, random, box.minX - 1, box.minY + 3 + 1, box.minZ + 1, 1, depth);
				}
				if (random.nextBoolean()) {
					next(start, pieces, random, box.maxX + 1, box.minY + 3 + 1, box.minZ + 1, 3, depth);
				}
				if (random.nextBoolean()) {
					next(start, pieces, random, box.minX + 1, box.minY + 3 + 1, box.maxZ + 1, 0, depth);
				}
			}
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}
			if (multiFloor) {
				fill(world, area, box.minX + 1, box.minY, box.minZ, box.maxX - 1, box.minY + 3 - 1, box.maxZ, 0, 0, false);
				fill(world, area, box.minX, box.minY, box.minZ + 1, box.maxX, box.minY + 3 - 1, box.maxZ - 1, 0, 0, false);
				fill(world, area, box.minX + 1, box.maxY - 2, box.minZ, box.maxX - 1, box.maxY, box.maxZ, 0, 0, false);
				fill(world, area, box.minX, box.maxY - 2, box.minZ + 1, box.maxX, box.maxY, box.maxZ - 1, 0, 0, false);
				fill(world, area, box.minX + 1, box.minY + 3, box.minZ + 1, box.maxX - 1, box.minY + 3, box.maxZ - 1, 0, 0, false);
			} else {
				fill(world, area, box.minX + 1, box.minY, box.minZ, box.maxX - 1, box.maxY, box.maxZ, 0, 0, false);
				fill(world, area, box.minX, box.minY, box.minZ + 1, box.maxX, box.maxY, box.maxZ - 1, 0, 0, false);
			}
			fill(world, area, box.minX + 1, box.minY, box.minZ + 1, box.minX + 1, box.maxY, box.minZ + 1, LegacyBlocks.PLANKS, 0, false);
			fill(world, area, box.minX + 1, box.minY, box.maxZ - 1, box.minX + 1, box.maxY, box.maxZ - 1, LegacyBlocks.PLANKS, 0, false);
			fill(world, area, box.maxX - 1, box.minY, box.minZ + 1, box.maxX - 1, box.maxY, box.minZ + 1, LegacyBlocks.PLANKS, 0, false);
			fill(world, area, box.maxX - 1, box.minY, box.maxZ - 1, box.maxX - 1, box.maxY, box.maxZ - 1, LegacyBlocks.PLANKS, 0, false);
			for (int x = box.minX; x <= box.maxX; x++) {
				for (int z = box.minZ; z <= box.maxZ; z++) {
					if (getBlock(world, x, box.minY - 1, z, area) == 0) {
						place(world, LegacyBlocks.PLANKS, 0, x, box.minY - 1, z, area);
					}
				}
			}
			return true;
		}
	}

	/** ComponentMineshaftStairs: a five-block drop. */
	private static final class Stairs extends LegacyPiece {
		Stairs(int depth, Random random, LegacyBox box, int direction) {
			super(depth);
			this.direction = direction;
			this.box = box;
		}

		static @Nullable LegacyBox findBox(List<LegacyPiece> pieces, Random random, int x, int y, int z, int direction) {
			LegacyBox box = new LegacyBox(x, y - 5, z, x, y + 2, z);
			switch (direction) {
				case 0 -> {
					box.maxX = x + 2;
					box.maxZ = z + 8;
				}
				case 1 -> {
					box.minX = x - 8;
					box.maxZ = z + 2;
				}
				case 2 -> {
					box.maxX = x + 2;
					box.minZ = z - 8;
				}
				case 3 -> {
					box.maxX = x + 8;
					box.maxZ = z + 2;
				}
				default -> {
				}
			}
			return findIntersecting(pieces, box) != null ? null : box;
		}

		@Override
		public void addChildren(LegacyPiece start, List<LegacyPiece> pieces, Random random) {
			int depth = depth();
			switch (direction) {
				case 0 -> next(start, pieces, random, box.minX, box.minY, box.maxZ + 1, 0, depth);
				case 1 -> next(start, pieces, random, box.minX - 1, box.minY, box.minZ, 1, depth);
				case 2 -> next(start, pieces, random, box.minX, box.minY, box.minZ - 1, 2, depth);
				case 3 -> next(start, pieces, random, box.maxX + 1, box.minY, box.minZ, 3, depth);
				default -> {
				}
			}
		}

		@Override
		public boolean place(LegacyWorld world, Random random, LegacyBox area) {
			if (hasLiquid(world, area)) {
				return false;
			}
			fill(world, area, 0, 5, 0, 2, 7, 1, 0, 0, false);
			fill(world, area, 0, 0, 7, 2, 2, 8, 0, 0, false);
			for (int step = 0; step < 5; step++) {
				fill(world, area, 0, 5 - step - (step < 4 ? 1 : 0), 2 + step, 2, 7 - step, 2 + step, 0, 0, false);
			}
			return true;
		}
	}
}
