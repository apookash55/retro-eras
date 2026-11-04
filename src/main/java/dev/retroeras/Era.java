package dev.retroeras;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The historical eras the mod recreates. Each era is a datapack dimension under data/retroeras/dimension/, reached by
 * lighting a portal inside a frame of the era's frame block.
 * Only BETA has a dimension so far; add the others here as their worldgen lands.
 */
public enum Era {
	BETA("beta", Blocks.MOSSY_COBBLESTONE);

	/** Inventory group for every dimension that is not an era (overworld, nether, end, other mods). */
	public static final String MODERN = "modern";

	private final String name;
	private final ResourceKey<Level> dimension;
	private final Block frame;

	Era(String name, Block frame) {
		this.name = name;
		this.dimension = ResourceKey.create(Registries.DIMENSION, RetroEras.id(name));
		this.frame = frame;
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
