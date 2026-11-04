package dev.retroeras.worldgen.release;

import org.jspecify.annotations.Nullable;

/**
 * Minecraft 1.6.4's overworld biomes (BiomeGenBase), with the values its terrain generator reads: numeric id, base
 * height, height variation, temperature and the surface blocks.
 */
public enum ReleaseBiome {
	OCEAN(0, "ocean", -1.0F, 0.4F, 0.5F, Surface.GRASS),
	PLAINS(1, "plains", 0.1F, 0.3F, 0.8F, Surface.GRASS),
	DESERT(2, "desert", 0.1F, 0.2F, 2.0F, Surface.SAND),
	EXTREME_HILLS(3, "extreme_hills", 0.3F, 1.5F, 0.2F, Surface.GRASS),
	FOREST(4, "forest", 0.1F, 0.3F, 0.7F, Surface.GRASS),
	TAIGA(5, "taiga", 0.1F, 0.4F, 0.05F, Surface.GRASS),
	SWAMPLAND(6, "swampland", -0.2F, 0.1F, 0.8F, Surface.GRASS),
	RIVER(7, "river", -0.5F, 0.0F, 0.5F, Surface.GRASS),
	FROZEN_OCEAN(10, "frozen_ocean", -1.0F, 0.5F, 0.0F, Surface.GRASS),
	FROZEN_RIVER(11, "frozen_river", -0.5F, 0.0F, 0.0F, Surface.GRASS),
	ICE_PLAINS(12, "ice_plains", 0.1F, 0.3F, 0.0F, Surface.GRASS),
	ICE_MOUNTAINS(13, "ice_mountains", 0.3F, 1.3F, 0.0F, Surface.GRASS),
	MUSHROOM_ISLAND(14, "mushroom_island", 0.2F, 1.0F, 0.9F, Surface.MYCELIUM),
	MUSHROOM_ISLAND_SHORE(15, "mushroom_island_shore", -1.0F, 0.1F, 0.9F, Surface.MYCELIUM),
	BEACH(16, "beach", 0.0F, 0.1F, 0.8F, Surface.SAND),
	DESERT_HILLS(17, "desert_hills", 0.3F, 0.8F, 2.0F, Surface.SAND),
	FOREST_HILLS(18, "forest_hills", 0.3F, 0.7F, 0.7F, Surface.GRASS),
	TAIGA_HILLS(19, "taiga_hills", 0.3F, 0.8F, 0.05F, Surface.GRASS),
	EXTREME_HILLS_EDGE(20, "extreme_hills_edge", 0.2F, 0.8F, 0.2F, Surface.GRASS),
	JUNGLE(21, "jungle", 0.2F, 0.4F, 1.2F, Surface.GRASS),
	JUNGLE_HILLS(22, "jungle_hills", 1.8F, 0.5F, 1.2F, Surface.GRASS);

	/** Top and filler blocks, as Release block ids (see {@link ReleaseTerrain}). */
	enum Surface {
		GRASS(ReleaseTerrain.GRASS, ReleaseTerrain.DIRT),
		SAND(ReleaseTerrain.SAND, ReleaseTerrain.SAND),
		MYCELIUM(ReleaseTerrain.MYCELIUM, ReleaseTerrain.DIRT);

		final byte top;
		final byte filler;

		Surface(byte top, byte filler) {
			this.top = top;
			this.filler = filler;
		}
	}

	private static final ReleaseBiome[] BY_ID = new ReleaseBiome[256];

	static {
		for (ReleaseBiome biome : values()) {
			BY_ID[biome.id] = biome;
		}
	}

	final int id;
	private final String name;
	final float baseHeight;
	final float heightVariation;
	final float temperature;
	final Surface surface;

	ReleaseBiome(int id, String name, float baseHeight, float heightVariation, float temperature, Surface surface) {
		this.id = id;
		this.name = name;
		this.baseHeight = baseHeight;
		this.heightVariation = heightVariation;
		this.temperature = temperature;
		this.surface = surface;
	}

	/** The biome's path under retroeras:release/. */
	public String id() {
		return name;
	}

	static @Nullable ReleaseBiome byId(int id) {
		return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
	}

	/** Like byId, falling back to ocean for ids the overworld never produces (hell, sky). */
	static ReleaseBiome byIdOrOcean(int id) {
		ReleaseBiome biome = byId(id);
		return biome == null ? OCEAN : biome;
	}
}
