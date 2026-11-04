package dev.retroeras.worldgen.release.structure;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/**
 * 1.6.4's StructureComponent: one piece of a structure, laid out in its own coordinates (x across, y up, z along) and
 * turned to face {@link #direction} (0 south, 1 west, 2 north, 3 east, or -1 for pieces in world coordinates).
 */
public abstract class LegacyPiece {
	/** BlockDirectional and its kin, whose low metadata bits are a 1.6.4 Direction. */
	private static final Set<Integer> DIRECTIONAL = Set.of(26, 86, 91, 93, 94, 107, 120, 127, 145, 149, 150);

	protected LegacyBox box;
	protected int direction = -1;
	/** How many pieces away from the start this one is. */
	protected int depth;

	protected LegacyPiece(int depth) {
		this.depth = depth;
	}

	/** buildComponent: adds the pieces leading off this one. */
	public void addChildren(LegacyPiece start, List<LegacyPiece> pieces, Random random) {
	}

	/** addComponentParts: places the blocks that fall inside {@code area}; false drops the piece from its structure. */
	public abstract boolean place(LegacyWorld world, Random random, LegacyBox area);

	public LegacyBox box() {
		return box;
	}

	public int depth() {
		return depth;
	}

	public static @Nullable LegacyPiece findIntersecting(List<LegacyPiece> pieces, LegacyBox box) {
		for (LegacyPiece piece : pieces) {
			if (piece.box() != null && piece.box().intersects(box)) {
				return piece;
			}
		}
		return null;
	}

	/** isLiquidInStructureBoundingBox, over the box grown by one and clipped to {@code area}. */
	protected boolean hasLiquid(LegacyWorld world, LegacyBox area) {
		int minX = Math.max(box.minX - 1, area.minX);
		int minY = Math.max(box.minY - 1, area.minY);
		int minZ = Math.max(box.minZ - 1, area.minZ);
		int maxX = Math.min(box.maxX + 1, area.maxX);
		int maxY = Math.min(box.maxY + 1, area.maxY);
		int maxZ = Math.min(box.maxZ + 1, area.maxZ);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (world.isLiquid(x, minY, z) || world.isLiquid(x, maxY, z)) {
					return true;
				}
			}
		}
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				if (world.isLiquid(x, y, minZ) || world.isLiquid(x, y, maxZ)) {
					return true;
				}
			}
		}
		for (int z = minZ; z <= maxZ; z++) {
			for (int y = minY; y <= maxY; y++) {
				if (world.isLiquid(minX, y, z) || world.isLiquid(maxX, y, z)) {
					return true;
				}
			}
		}
		return false;
	}

	protected int worldX(int x, int z) {
		return switch (direction) {
			case 0, 2 -> box.minX + x;
			case 1 -> box.maxX - z;
			case 3 -> box.minX + z;
			default -> x;
		};
	}

	protected int worldY(int y) {
		return direction == -1 ? y : y + box.minY;
	}

	protected int worldZ(int x, int z) {
		return switch (direction) {
			case 0 -> box.minZ + z;
			case 1, 3 -> box.minZ + x;
			case 2 -> box.maxZ - z;
			default -> z;
		};
	}

	/** getMetadataWithOffset: turns a block's metadata to match the piece's direction. */
	protected int meta(int id, int meta) {
		if (id == LegacyBlocks.RAIL) {
			if (direction == 1 || direction == 3) {
				return meta == 1 ? 0 : 1;
			}
		} else if (id == LegacyBlocks.OAK_DOOR || id == LegacyBlocks.IRON_DOOR) {
			if (direction == 0) {
				if (meta == 0) {
					return 2;
				}
				if (meta == 2) {
					return 0;
				}
			} else if (direction == 1) {
				return meta + 1 & 3;
			} else if (direction == 3) {
				return meta + 3 & 3;
			}
		} else if (id == LegacyBlocks.COBBLESTONE_STAIRS || id == LegacyBlocks.OAK_STAIRS || id == LegacyBlocks.NETHER_BRICK_STAIRS
				|| id == LegacyBlocks.STONE_BRICK_STAIRS || id == LegacyBlocks.SANDSTONE_STAIRS) {
			if (direction == 0) {
				if (meta == 2) {
					return 3;
				}
				if (meta == 3) {
					return 2;
				}
			} else if (direction == 1) {
				if (meta == 0) {
					return 2;
				}
				if (meta == 1) {
					return 3;
				}
				if (meta == 2) {
					return 0;
				}
				if (meta == 3) {
					return 1;
				}
			} else if (direction == 3) {
				if (meta == 0) {
					return 2;
				}
				if (meta == 1) {
					return 3;
				}
				if (meta == 2) {
					return 1;
				}
				if (meta == 3) {
					return 0;
				}
			}
		} else if (id == LegacyBlocks.LADDER) {
			if (direction == 0) {
				if (meta == 2) {
					return 3;
				}
				if (meta == 3) {
					return 2;
				}
			} else if (direction == 1) {
				if (meta == 2) {
					return 4;
				}
				if (meta == 3) {
					return 5;
				}
				if (meta == 4) {
					return 2;
				}
				if (meta == 5) {
					return 3;
				}
			} else if (direction == 3) {
				if (meta == 2) {
					return 5;
				}
				if (meta == 3) {
					return 4;
				}
				if (meta == 4) {
					return 2;
				}
				if (meta == 5) {
					return 3;
				}
			}
		} else if (id == LegacyBlocks.STONE_BUTTON) {
			if (direction == 0) {
				if (meta == 3) {
					return 4;
				}
				if (meta == 4) {
					return 3;
				}
			} else if (direction == 1) {
				if (meta == 3) {
					return 1;
				}
				if (meta == 4) {
					return 2;
				}
				if (meta == 2) {
					return 3;
				}
				if (meta == 1) {
					return 4;
				}
			} else if (direction == 3) {
				if (meta == 3) {
					return 2;
				}
				if (meta == 4) {
					return 1;
				}
				if (meta == 2) {
					return 3;
				}
				if (meta == 1) {
					return 4;
				}
			}
		} else if (id == LegacyBlocks.TRIPWIRE_HOOK || DIRECTIONAL.contains(id)) {
			if (direction == 0) {
				if (meta == 0) {
					return 2;
				}
				if (meta == 2) {
					return 0;
				}
			} else if (direction == 1) {
				if (meta == 2) {
					return 1;
				}
				if (meta == 0) {
					return 3;
				}
				if (meta == 1) {
					return 2;
				}
				if (meta == 3) {
					return 0;
				}
			} else if (direction == 3) {
				if (meta == 2) {
					return 3;
				}
				if (meta == 0) {
					return 1;
				}
				if (meta == 1) {
					return 2;
				}
				if (meta == 3) {
					return 0;
				}
			}
		} else if (id == LegacyBlocks.PISTON || id == LegacyBlocks.STICKY_PISTON || id == LegacyBlocks.LEVER || id == LegacyBlocks.DISPENSER) {
			if (direction == 0) {
				if (meta == 2) {
					return 3;
				}
				if (meta == 3) {
					return 2;
				}
			} else if (direction == 1) {
				if (meta == 2) {
					return 4;
				}
				if (meta == 3) {
					return 5;
				}
				if (meta == 4) {
					return 2;
				}
				if (meta == 5) {
					return 3;
				}
			} else if (direction == 3) {
				if (meta == 2) {
					return 5;
				}
				if (meta == 3) {
					return 4;
				}
				if (meta == 4) {
					return 2;
				}
				if (meta == 5) {
					return 3;
				}
			}
		}
		return meta;
	}

	/** placeBlockAtCurrentPosition. */
	protected void place(LegacyWorld world, int id, int meta, int x, int y, int z, LegacyBox area) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz)) {
			world.setBlock(wx, wy, wz, id, meta);
		}
	}

	/** getBlockIdAtCurrentPosition: 0 outside {@code area}. */
	protected int getBlock(LegacyWorld world, int x, int y, int z, LegacyBox area) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		return area.contains(wx, wy, wz) ? world.getBlockId(wx, wy, wz) : 0;
	}

	/** Whether the block is an opaque cube (Block.opaqueCubeLookup), false outside {@code area}. */
	protected boolean isOpaque(LegacyWorld world, int x, int y, int z, LegacyBox area) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		return area.contains(wx, wy, wz) && world.isOpaqueCube(wx, wy, wz);
	}

	/** fillWithAir. */
	protected void fillAir(LegacyWorld world, LegacyBox area, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					place(world, 0, 0, x, y, z, area);
				}
			}
		}
	}

	/** fillWithBlocks: {@code edge} on the outside of the region, {@code inside} within; optionally only over non-air. */
	protected void fill(LegacyWorld world, LegacyBox area, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int edge, int inside,
			boolean onlyReplaceSolid) {
		fill(world, area, minX, minY, minZ, maxX, maxY, maxZ, edge, 0, inside, 0, onlyReplaceSolid);
	}

	/** fillWithMetadataBlocks. */
	protected void fill(LegacyWorld world, LegacyBox area, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int edge, int edgeMeta,
			int inside, int insideMeta, boolean onlyReplaceSolid) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!onlyReplaceSolid || getBlock(world, x, y, z, area) != 0) {
						if (y != minY && y != maxY && x != minX && x != maxX && z != minZ && z != maxZ) {
							place(world, inside, insideMeta, x, y, z, area);
						} else {
							place(world, edge, edgeMeta, x, y, z, area);
						}
					}
				}
			}
		}
	}

	/** fillWithRandomizedBlocks: each block chosen by {@code selector}. */
	protected void fill(LegacyWorld world, LegacyBox area, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean onlyReplaceSolid,
			Random random, BlockSelector selector) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!onlyReplaceSolid || getBlock(world, x, y, z, area) != 0) {
						selector.select(random, x, y, z, y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ);
						place(world, selector.id, selector.meta, x, y, z, area);
					}
				}
			}
		}
	}

	/** randomlyFillWithBlocks: each block is placed with probability {@code chance}. */
	protected void fillRandomly(LegacyWorld world, LegacyBox area, Random random, float chance, int minX, int minY, int minZ, int maxX, int maxY,
			int maxZ, int edge, int inside, boolean onlyReplaceSolid) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!(random.nextFloat() > chance) && (!onlyReplaceSolid || getBlock(world, x, y, z, area) != 0)) {
						if (y != minY && y != maxY && x != minX && x != maxX && z != minZ && z != maxZ) {
							place(world, inside, 0, x, y, z, area);
						} else {
							place(world, edge, 0, x, y, z, area);
						}
					}
				}
			}
		}
	}

	/** randomlyPlaceBlock. */
	protected void placeRandomly(LegacyWorld world, LegacyBox area, Random random, float chance, int x, int y, int z, int id, int meta) {
		if (random.nextFloat() < chance) {
			place(world, id, meta, x, y, z, area);
		}
	}

	/** randomlyRareFillWithBlocks: an ellipsoid, used for mineshaft room domes. */
	protected void dome(LegacyWorld world, LegacyBox area, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int id, boolean onlyReplaceSolid) {
		float width = maxX - minX + 1;
		float height = maxY - minY + 1;
		float length = maxZ - minZ + 1;
		float centerX = minX + width / 2.0F;
		float centerZ = minZ + length / 2.0F;
		for (int y = minY; y <= maxY; y++) {
			float dy = (y - minY) / height;
			for (int x = minX; x <= maxX; x++) {
				float dx = (x - centerX) / (width * 0.5F);
				for (int z = minZ; z <= maxZ; z++) {
					float dz = (z - centerZ) / (length * 0.5F);
					if (!onlyReplaceSolid || getBlock(world, x, y, z, area) != 0) {
						if (dx * dx + dy * dy + dz * dz <= 1.05F) {
							place(world, id, 0, x, y, z, area);
						}
					}
				}
			}
		}
	}

	/** clearCurrentPositionBlocksUpwards. */
	protected void clearUpwards(LegacyWorld world, int x, int y, int z, LegacyBox area) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz)) {
			while (!world.isAir(wx, wy, wz) && wy < 255) {
				world.setBlock(wx, wy, wz, 0, 0);
				wy++;
			}
		}
	}

	/** fillCurrentPositionBlocksDownwards. */
	protected void fillDownwards(LegacyWorld world, int id, int meta, int x, int y, int z, LegacyBox area) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz)) {
			while ((world.isAir(wx, wy, wz) || world.isLiquid(wx, wy, wz)) && wy > 1) {
				world.setBlock(wx, wy, wz, id, meta);
				wy--;
			}
		}
	}

	/** generateStructureChestContents. */
	protected boolean chest(LegacyWorld world, LegacyBox area, Random random, int x, int y, int z, LegacyLoot[] loot, int count) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz) && world.getBlockId(wx, wy, wz) != LegacyBlocks.CHEST) {
			world.setBlock(wx, wy, wz, LegacyBlocks.CHEST, chestFacing(world, wx, wy, wz));
			if (world.getBlockEntity(wx, wy, wz) instanceof ChestBlockEntity chest) {
				LegacyLoot.fill(random, loot, chest, count, world.level().registryAccess());
			}
			return true;
		}
		return false;
	}

	/** BlockChest.onBlockAdded: a lone chest turns away from the solid block behind it. */
	private static int chestFacing(LegacyWorld world, int x, int y, int z) {
		boolean north = world.isOpaqueCube(x, y, z - 1);
		boolean south = world.isOpaqueCube(x, y, z + 1);
		boolean west = world.isOpaqueCube(x - 1, y, z);
		boolean east = world.isOpaqueCube(x + 1, y, z);
		int facing = 3;
		if (north && !south) {
			facing = 3;
		}
		if (south && !north) {
			facing = 2;
		}
		if (west && !east) {
			facing = 5;
		}
		if (east && !west) {
			facing = 4;
		}
		return facing;
	}

	/** generateStructureDispenserContents. */
	protected boolean dispenser(LegacyWorld world, LegacyBox area, Random random, int x, int y, int z, int meta, LegacyLoot[] loot, int count) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz) && world.getBlockId(wx, wy, wz) != LegacyBlocks.DISPENSER) {
			world.setBlock(wx, wy, wz, LegacyBlocks.DISPENSER, meta(LegacyBlocks.DISPENSER, meta));
			if (world.getBlockEntity(wx, wy, wz) instanceof DispenserBlockEntity dispenser) {
				LegacyLoot.fill(random, loot, dispenser, count, world.level().registryAccess());
			}
			return true;
		}
		return false;
	}

	/** placeDoorAtCurrentPosition: an oak door, hinged the way ItemDoor.placeDoorBlock chose. */
	protected void door(LegacyWorld world, LegacyBox area, Random random, int x, int y, int z, int facing) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (area.contains(wx, wy, wz)) {
			placeDoor(world, wx, wy, wz, facing, LegacyBlocks.OAK_DOOR);
		}
	}

	/** ItemDoor.placeDoorBlock. */
	protected static void placeDoor(LegacyWorld world, int x, int y, int z, int facing, int door) {
		int dx = 0;
		int dz = 0;
		switch (facing) {
			case 0 -> dz = 1;
			case 1 -> dx = -1;
			case 2 -> dz = -1;
			case 3 -> dx = 1;
			default -> {
			}
		}
		int left = (world.isNormalCube(x - dx, y, z - dz) ? 1 : 0) + (world.isNormalCube(x - dx, y + 1, z - dz) ? 1 : 0);
		int right = (world.isNormalCube(x + dx, y, z + dz) ? 1 : 0) + (world.isNormalCube(x + dx, y + 1, z + dz) ? 1 : 0);
		boolean doorLeft = world.getBlockId(x - dx, y, z - dz) == door || world.getBlockId(x - dx, y + 1, z - dz) == door;
		boolean doorRight = world.getBlockId(x + dx, y, z + dz) == door || world.getBlockId(x + dx, y + 1, z + dz) == door;
		boolean mirrored = doorLeft && !doorRight || right > left;
		world.setBlock(x, y, z, door, facing);
		world.setBlock(x, y + 1, z, door, 8 | (mirrored ? 1 : 0));
	}

	/** Places a mob spawner for {@code type} at a piece position, if it is inside {@code area}. */
	protected boolean spawner(LegacyWorld world, LegacyBox area, int x, int y, int z, EntityType<?> type) {
		int wx = worldX(x, z);
		int wy = worldY(y);
		int wz = worldZ(x, z);
		if (!area.contains(wx, wy, wz)) {
			return false;
		}
		world.setBlock(wx, wy, wz, LegacyBlocks.SPAWNER, 0);
		BlockEntity entity = world.getBlockEntity(wx, wy, wz);
		if (entity instanceof SpawnerBlockEntity spawner) {
			spawner.setEntityId(type, world.level().getRandom());
		}
		return true;
	}

	/** BlockSelector: picks a block (id and metadata) for each position of a randomized fill. */
	public abstract static class BlockSelector {
		protected int id;
		protected int meta;

		public abstract void select(Random random, int x, int y, int z, boolean edge);
	}
}
