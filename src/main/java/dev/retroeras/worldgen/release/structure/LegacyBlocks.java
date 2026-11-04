package dev.retroeras.worldgen.release.structure;

import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Turns 1.6.4 block ids and metadata (as the ported structure code uses them) into 26.3 block states, and back again as
 * far as the structure code needs to read the world.
 */
public final class LegacyBlocks {
	public static final int AIR = 0;
	public static final int STONE = 1;
	public static final int GRASS = 2;
	public static final int DIRT = 3;
	public static final int COBBLESTONE = 4;
	public static final int PLANKS = 5;
	public static final int FLOWING_WATER = 8;
	public static final int WATER = 9;
	public static final int FLOWING_LAVA = 10;
	public static final int LAVA = 11;
	public static final int SAND = 12;
	public static final int GRAVEL = 13;
	public static final int LOG = 17;
	public static final int LEAVES = 18;
	public static final int GLASS = 20;
	public static final int DISPENSER = 23;
	public static final int SANDSTONE = 24;
	public static final int STICKY_PISTON = 29;
	public static final int WEB = 30;
	public static final int TALL_GRASS = 31;
	public static final int PISTON = 33;
	public static final int WOOL = 35;
	public static final int DANDELION = 37;
	public static final int ROSE = 38;
	public static final int BROWN_MUSHROOM = 39;
	public static final int RED_MUSHROOM = 40;
	public static final int DOUBLE_SLAB = 43;
	public static final int SLAB = 44;
	public static final int BRICKS = 45;
	public static final int TNT = 46;
	public static final int BOOKSHELF = 47;
	public static final int MOSSY_COBBLESTONE = 48;
	public static final int OBSIDIAN = 49;
	public static final int TORCH = 50;
	public static final int SPAWNER = 52;
	public static final int OAK_STAIRS = 53;
	public static final int CHEST = 54;
	public static final int REDSTONE_WIRE = 55;
	public static final int CRAFTING_TABLE = 58;
	public static final int WHEAT = 59;
	public static final int FARMLAND = 60;
	public static final int FURNACE = 61;
	public static final int LIT_FURNACE = 62;
	public static final int OAK_DOOR = 64;
	public static final int LADDER = 65;
	public static final int RAIL = 66;
	public static final int COBBLESTONE_STAIRS = 67;
	public static final int LEVER = 69;
	public static final int STONE_PRESSURE_PLATE = 70;
	public static final int IRON_DOOR = 71;
	public static final int WOODEN_PRESSURE_PLATE = 72;
	public static final int STONE_BUTTON = 77;
	public static final int CACTUS = 81;
	public static final int FENCE = 85;
	public static final int REPEATER = 93;
	public static final int POWERED_REPEATER = 94;
	public static final int TRAPDOOR = 96;
	public static final int MONSTER_EGG = 97;
	public static final int STONE_BRICKS = 98;
	public static final int IRON_BARS = 101;
	public static final int GLASS_PANE = 102;
	public static final int VINE = 106;
	public static final int FENCE_GATE = 107;
	public static final int BRICK_STAIRS = 108;
	public static final int STONE_BRICK_STAIRS = 109;
	public static final int NETHER_BRICK_STAIRS = 114;
	public static final int CAULDRON = 118;
	public static final int END_PORTAL_FRAME = 120;
	public static final int SANDSTONE_STAIRS = 128;
	public static final int TRIPWIRE_HOOK = 131;
	public static final int TRIPWIRE = 132;
	public static final int SPRUCE_STAIRS = 134;
	public static final int BIRCH_STAIRS = 135;
	public static final int JUNGLE_STAIRS = 136;
	public static final int FLOWER_POT = 140;
	public static final int CARROTS = 141;
	public static final int POTATOES = 142;
	public static final int STAINED_CLAY = 159;

	private static final Block[] PLANK_TYPES = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS};
	private static final Block[] LOG_TYPES = {Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG};
	private static final Block[] SLABS = {Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLESTONE_SLAB,
			Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB};
	private static final Block[] POTS = {Blocks.FLOWER_POT, Blocks.POTTED_POPPY, Blocks.POTTED_DANDELION, Blocks.POTTED_OAK_SAPLING,
			Blocks.POTTED_SPRUCE_SAPLING, Blocks.POTTED_BIRCH_SAPLING, Blocks.POTTED_JUNGLE_SAPLING, Blocks.POTTED_RED_MUSHROOM,
			Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_CACTUS, Blocks.POTTED_DEAD_BUSH, Blocks.POTTED_FERN};

	/** Reverse lookups for the blocks the structure code reads back. Anything else solid reads as stone. */
	private static final Map<Block, Integer> IDS = Map.ofEntries(Map.entry(Blocks.STONE, STONE), Map.entry(Blocks.GRASS_BLOCK, GRASS),
			Map.entry(Blocks.DIRT, DIRT), Map.entry(Blocks.COBBLESTONE, COBBLESTONE), Map.entry(Blocks.OAK_PLANKS, PLANKS),
			Map.entry(Blocks.SAND, SAND), Map.entry(Blocks.GRAVEL, GRAVEL), Map.entry(Blocks.OAK_LOG, LOG), Map.entry(Blocks.OAK_LEAVES, LEAVES),
			Map.entry(Blocks.SANDSTONE, SANDSTONE), Map.entry(Blocks.CHEST, CHEST), Map.entry(Blocks.OAK_DOOR, OAK_DOOR),
			Map.entry(Blocks.IRON_DOOR, IRON_DOOR), Map.entry(Blocks.RAIL, RAIL), Map.entry(Blocks.DISPENSER, DISPENSER),
			Map.entry(Blocks.SHORT_GRASS, TALL_GRASS), Map.entry(Blocks.ICE, 79), Map.entry(Blocks.SNOW, 78), Map.entry(Blocks.MYCELIUM, 110),
			Map.entry(Blocks.BEDROCK, 7));

	private LegacyBlocks() {
	}

	/** The 26.3 state for a 1.6.4 block id and metadata. */
	public static BlockState state(int id, int meta) {
		return switch (id) {
			case AIR -> Blocks.AIR.defaultBlockState();
			case STONE -> Blocks.STONE.defaultBlockState();
			case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case DIRT -> Blocks.DIRT.defaultBlockState();
			case COBBLESTONE -> Blocks.COBBLESTONE.defaultBlockState();
			case PLANKS -> PLANK_TYPES[meta & 3].defaultBlockState();
			case 7 -> Blocks.BEDROCK.defaultBlockState();
			case FLOWING_WATER, WATER -> Blocks.WATER.defaultBlockState();
			case FLOWING_LAVA, LAVA -> Blocks.LAVA.defaultBlockState();
			case SAND -> Blocks.SAND.defaultBlockState();
			case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			case LOG -> LOG_TYPES[meta & 3].defaultBlockState().setValue(RotatedPillarBlock.AXIS,
					switch (meta & 12) {
						case 4 -> Direction.Axis.X;
						case 8 -> Direction.Axis.Z;
						default -> Direction.Axis.Y;
					});
			case LEAVES -> Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
			case GLASS -> Blocks.GLASS.defaultBlockState();
			case DISPENSER -> Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, facing6(meta & 7));
			case SANDSTONE -> switch (meta) {
				case 1 -> Blocks.CHISELED_SANDSTONE.defaultBlockState();
				case 2 -> Blocks.CUT_SANDSTONE.defaultBlockState();
				default -> Blocks.SANDSTONE.defaultBlockState();
			};
			case STICKY_PISTON -> Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, facing6(meta & 7));
			case PISTON -> Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, facing6(meta & 7));
			case WEB -> Blocks.COBWEB.defaultBlockState();
			case TALL_GRASS -> meta == 2 ? Blocks.FERN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
			case WOOL -> Blocks.WOOL.pick(DyeColor.VALUES.get(meta & 15)).defaultBlockState();
			case DANDELION -> Blocks.DANDELION.defaultBlockState();
			case ROSE -> Blocks.POPPY.defaultBlockState();
			case BROWN_MUSHROOM -> Blocks.BROWN_MUSHROOM.defaultBlockState();
			case RED_MUSHROOM -> Blocks.RED_MUSHROOM.defaultBlockState();
			case DOUBLE_SLAB -> switch (meta & 7) {
				case 1 -> Blocks.SANDSTONE.defaultBlockState();
				case 2 -> Blocks.OAK_PLANKS.defaultBlockState();
				case 3 -> Blocks.COBBLESTONE.defaultBlockState();
				case 4 -> Blocks.BRICKS.defaultBlockState();
				case 5 -> Blocks.STONE_BRICKS.defaultBlockState();
				case 6 -> Blocks.NETHER_BRICKS.defaultBlockState();
				case 7 -> Blocks.QUARTZ_BLOCK.defaultBlockState();
				default -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
			};
			case SLAB -> SLABS[meta & 7].defaultBlockState().setValue(SlabBlock.TYPE, (meta & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM);
			case BRICKS -> Blocks.BRICKS.defaultBlockState();
			case TNT -> Blocks.TNT.defaultBlockState();
			case BOOKSHELF -> Blocks.BOOKSHELF.defaultBlockState();
			case MOSSY_COBBLESTONE -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
			case TORCH -> meta >= 1 && meta <= 4
					? Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, switch (meta) {
						case 1 -> Direction.EAST;
						case 2 -> Direction.WEST;
						case 3 -> Direction.SOUTH;
						default -> Direction.NORTH;
					})
					: Blocks.TORCH.defaultBlockState();
			case SPAWNER -> Blocks.SPAWNER.defaultBlockState();
			case OAK_STAIRS -> stairs(Blocks.OAK_STAIRS, meta);
			case COBBLESTONE_STAIRS -> stairs(Blocks.COBBLESTONE_STAIRS, meta);
			case BRICK_STAIRS -> stairs(Blocks.BRICK_STAIRS, meta);
			case STONE_BRICK_STAIRS -> stairs(Blocks.STONE_BRICK_STAIRS, meta);
			case NETHER_BRICK_STAIRS -> stairs(Blocks.NETHER_BRICK_STAIRS, meta);
			case SANDSTONE_STAIRS -> stairs(Blocks.SANDSTONE_STAIRS, meta);
			case SPRUCE_STAIRS -> stairs(Blocks.SPRUCE_STAIRS, meta);
			case BIRCH_STAIRS -> stairs(Blocks.BIRCH_STAIRS, meta);
			case JUNGLE_STAIRS -> stairs(Blocks.JUNGLE_STAIRS, meta);
			case CHEST -> Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing4(meta, Direction.NORTH));
			case REDSTONE_WIRE -> Blocks.REDSTONE_WIRE.defaultBlockState();
			case CRAFTING_TABLE -> Blocks.CRAFTING_TABLE.defaultBlockState();
			case WHEAT -> Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, Math.min(meta, 7));
			case CARROTS -> Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, Math.min(meta, 7));
			case POTATOES -> Blocks.POTATOES.defaultBlockState().setValue(CropBlock.AGE, Math.min(meta, 7));
			case FARMLAND -> Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, Math.min(meta, 7));
			case FURNACE, LIT_FURNACE -> Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing4(meta, Direction.NORTH))
					.setValue(BlockStateProperties.LIT, id == LIT_FURNACE);
			case OAK_DOOR, IRON_DOOR -> door(id == OAK_DOOR ? Blocks.OAK_DOOR : Blocks.IRON_DOOR, meta);
			case LADDER -> Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing4(meta, Direction.NORTH));
			case RAIL -> Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, switch (meta) {
				case 1 -> RailShape.EAST_WEST;
				case 2 -> RailShape.ASCENDING_EAST;
				case 3 -> RailShape.ASCENDING_WEST;
				case 4 -> RailShape.ASCENDING_NORTH;
				case 5 -> RailShape.ASCENDING_SOUTH;
				case 6 -> RailShape.SOUTH_EAST;
				case 7 -> RailShape.SOUTH_WEST;
				case 8 -> RailShape.NORTH_WEST;
				case 9 -> RailShape.NORTH_EAST;
				default -> RailShape.NORTH_SOUTH;
			});
			case LEVER -> lever(meta);
			case STONE_PRESSURE_PLATE -> Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
			case WOODEN_PRESSURE_PLATE -> Blocks.OAK_PRESSURE_PLATE.defaultBlockState();
			case STONE_BUTTON -> Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL)
					.setValue(ButtonBlock.FACING, switch (meta & 7) {
						case 1 -> Direction.EAST;
						case 2 -> Direction.WEST;
						case 3 -> Direction.SOUTH;
						default -> Direction.NORTH;
					});
			case 78 -> Blocks.SNOW.defaultBlockState();
			case 79 -> Blocks.ICE.defaultBlockState();
			case CACTUS -> Blocks.CACTUS.defaultBlockState();
			case FENCE -> Blocks.OAK_FENCE.defaultBlockState();
			case REPEATER, POWERED_REPEATER -> Blocks.REPEATER.defaultBlockState()
					.setValue(RepeaterBlock.FACING, horizontal(meta & 3).getOpposite())
					.setValue(RepeaterBlock.DELAY, (meta >> 2) + 1);
			case TRAPDOOR -> Blocks.OAK_TRAPDOOR.defaultBlockState()
					.setValue(TrapDoorBlock.FACING, switch (meta & 3) {
						case 0 -> Direction.NORTH;
						case 1 -> Direction.SOUTH;
						case 2 -> Direction.WEST;
						default -> Direction.EAST;
					})
					.setValue(TrapDoorBlock.OPEN, (meta & 4) != 0).setValue(TrapDoorBlock.HALF, (meta & 8) != 0 ? Half.TOP : Half.BOTTOM);
			case MONSTER_EGG -> switch (meta) {
				case 1 -> Blocks.INFESTED_COBBLESTONE.defaultBlockState();
				case 2 -> Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
				default -> Blocks.INFESTED_STONE.defaultBlockState();
			};
			case STONE_BRICKS -> switch (meta) {
				case 1 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
				case 2 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
				case 3 -> Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
				default -> Blocks.STONE_BRICKS.defaultBlockState();
			};
			case IRON_BARS -> Blocks.IRON_BARS.defaultBlockState();
			case GLASS_PANE -> Blocks.GLASS_PANE.defaultBlockState();
			case VINE -> Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, (meta & 1) != 0).setValue(VineBlock.WEST, (meta & 2) != 0)
					.setValue(VineBlock.NORTH, (meta & 4) != 0).setValue(VineBlock.EAST, (meta & 8) != 0);
			case FENCE_GATE -> Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, horizontal(meta & 3))
					.setValue(FenceGateBlock.OPEN, (meta & 4) != 0);
			case CAULDRON -> meta <= 0 ? Blocks.CAULDRON.defaultBlockState()
					: Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Math.min(meta, 3));
			case END_PORTAL_FRAME -> Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, horizontal(meta & 3))
					.setValue(EndPortalFrameBlock.HAS_EYE, (meta & 4) != 0);
			case TRIPWIRE_HOOK -> Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, horizontal(meta & 3))
					.setValue(TripWireHookBlock.ATTACHED, (meta & 4) != 0).setValue(TripWireHookBlock.POWERED, (meta & 8) != 0);
			case TRIPWIRE -> Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.ATTACHED, (meta & 4) != 0);
			case FLOWER_POT -> POTS[meta >= 0 && meta < POTS.length ? meta : 0].defaultBlockState();
			case STAINED_CLAY -> Blocks.DYED_TERRACOTTA.pick(DyeColor.VALUES.get(meta & 15)).defaultBlockState();
			default -> Blocks.STONE.defaultBlockState();
		};
	}

	/** The 1.6.4 id a 26.3 state reads as, for the structure code's checks. */
	public static int id(BlockState state) {
		if (state.isAir()) {
			return AIR;
		}
		if (state.getFluidState().isSource() || !state.getFluidState().isEmpty()) {
			return state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? LAVA : WATER;
		}
		Integer id = IDS.get(state.getBlock());
		return id != null ? id : STONE;
	}

	/** Blocks whose shape depends on their neighbours, which 26.3 has to work out after the structure is placed. */
	public static boolean needsShapeUpdate(int id) {
		return switch (id) {
			case FENCE, FENCE_GATE, GLASS_PANE, IRON_BARS, REDSTONE_WIRE, TRIPWIRE, OAK_STAIRS, COBBLESTONE_STAIRS, BRICK_STAIRS, STONE_BRICK_STAIRS,
					NETHER_BRICK_STAIRS, SANDSTONE_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, CHEST -> true;
			default -> false;
		};
	}

	/** Legacy stairs: low two bits pick the side the stairs climb toward (east, west, south, north); bit 4 is upside down. */
	private static BlockState stairs(Block block, int meta) {
		Direction facing = switch (meta & 3) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			default -> Direction.NORTH;
		};
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
	}

	/** Legacy doors: the lower half holds the facing (0 east, 1 south, 2 west, 3 north) and open bit; the upper half bit 8 plus the hinge bit. */
	private static BlockState door(Block block, int meta) {
		BlockState state = block.defaultBlockState();
		if ((meta & 8) != 0) {
			return state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.HINGE, (meta & 1) != 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
		}
		Direction facing = switch (meta & 3) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.SOUTH;
			case 2 -> Direction.WEST;
			default -> Direction.NORTH;
		};
		return state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, (meta & 4) != 0);
	}

	private static BlockState lever(int meta) {
		BlockState state = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.POWERED, (meta & 8) != 0);
		return switch (meta & 7) {
			case 1 -> state.setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.EAST);
			case 2 -> state.setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST);
			case 3 -> state.setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.SOUTH);
			case 4 -> state.setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.NORTH);
			case 5 -> state.setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.NORTH);
			case 6 -> state.setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.EAST);
			case 7 -> state.setValue(LeverBlock.FACE, AttachFace.CEILING).setValue(LeverBlock.FACING, Direction.NORTH);
			default -> state.setValue(LeverBlock.FACE, AttachFace.CEILING).setValue(LeverBlock.FACING, Direction.EAST);
		};
	}

	/** Metadata 2-5 as north, south, west, east, the scheme ladders, chests and furnaces use. */
	private static Direction facing4(int meta, Direction fallback) {
		return switch (meta) {
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.WEST;
			case 5 -> Direction.EAST;
			default -> fallback;
		};
	}

	/** Metadata 0-5 as down, up, north, south, west, east, the scheme pistons and dispensers use. */
	private static Direction facing6(int meta) {
		return switch (meta) {
			case 0 -> Direction.DOWN;
			case 1 -> Direction.UP;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.WEST;
			default -> Direction.EAST;
		};
	}

	/** 1.6.4's Direction: 0 south, 1 west, 2 north, 3 east. */
	public static Direction horizontal(int direction) {
		return switch (direction & 3) {
			case 0 -> Direction.SOUTH;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}
}
