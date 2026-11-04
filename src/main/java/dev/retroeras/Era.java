package dev.retroeras;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The historical eras the mod recreates. Each era is a datapack dimension under data/retroeras/dimension/, reached by
 * lighting a portal inside a frame of the era's frame block.
 */
public enum Era {
	/** Old Release: Minecraft 1.6.4, the last version before 1.7's biome overhaul. Hunger, sprinting and XP are back. */
	OLD_RELEASE("old_release", Blocks.STONE_BRICKS, 0, true, true),
	BETA("beta", Blocks.MOSSY_COBBLESTONE, 0, true, false),
	/** Alpha 1.2.6, the last Alpha: Beta's terrain without sandstone, and only oak trees. Pumpkins came with its Halloween Update. */
	ALPHA("alpha", Blocks.PUMPKIN, 0, true, false),
	/** Pre-Alpha: Infdev 20100618, the last Infdev build. Bricks recall Infdev's brick pyramids. */
	PRE_ALPHA("pre_alpha", Blocks.BRICKS, 0, true, false),
	/** Pre-classic "Cave game" (rd-161348): a finite 256x256 world where you build without health or survival. */
	CAVEGAME("cavegame", Blocks.COBBLESTONE, 256, false, false);

	/** Inventory group for every dimension that is not an era (overworld, nether, end, other mods). */
	public static final String MODERN = "modern";

	private final String name;
	private final ResourceKey<Level> dimension;
	private final Block frame;
	private final int worldSize;
	private final boolean survival;
	private final boolean hunger;

	Era(String name, Block frame, int worldSize, boolean survival, boolean hunger) {
		this.name = name;
		this.dimension = ResourceKey.create(Registries.DIMENSION, RetroEras.id(name));
		this.frame = frame;
		this.worldSize = worldSize;
		this.survival = survival;
		this.hunger = hunger;
	}

	public String getName() {
		return name;
	}

	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	public Block getFrame() {
		return frame;
	}

	/** Width of a finite world spanning x and z from 0 to this size, or 0 for an infinite one. */
	public int getWorldSize() {
		return worldSize;
	}

	/** Whether the era had health and damage. Eras without them hide the hearts and only the void can hurt you. */
	public boolean isSurvival() {
		return survival;
	}

	/**
	 * Whether the era had hunger, sprinting and experience (added in Beta 1.8). Eras without them hide those bars, hold
	 * hunger still, heal from food instantly and default to flat lighting.
	 */
	public boolean hasHunger() {
		return hunger;
	}

	/** Moves {@code pos} inside a finite world, keeping a margin from the edge; infinite worlds return it unchanged. */
	public BlockPos clampToWorld(BlockPos pos) {
		if (worldSize == 0) {
			return pos;
		}
		int margin = 8;
		return new BlockPos(Mth.clamp(pos.getX(), margin, worldSize - margin), pos.getY(), Mth.clamp(pos.getZ(), margin, worldSize - margin));
	}

	public static @Nullable Era fromDimension(ResourceKey<Level> dimension) {
		for (Era era : values()) {
			if (era.dimension == dimension) {
				return era;
			}
		}
		return null;
	}

	/** The name of the inventory group a player in this dimension uses. */
	public static String inventoryGroup(ResourceKey<Level> dimension) {
		Era era = fromDimension(dimension);
		return era == null ? MODERN : era.name;
	}
}
