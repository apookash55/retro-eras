package dev.retroeras.worldgen.release.structure;

import java.util.List;
import java.util.Random;

import dev.retroeras.worldgen.release.ReleaseTerrain;

import net.minecraft.world.level.WorldGenLevel;

/**
 * 1.6.4's structures for one world seed, placed in the order ChunkProviderGenerate.populate placed them: mineshafts,
 * villages, strongholds, then temples and witch huts.
 */
public final class ReleaseStructures {
	private final long seed;
	private final Stronghold stronghold;
	private final List<LegacyStructureFeature> features;

	public ReleaseStructures(ReleaseTerrain terrain) {
		this.seed = terrain.seed();
		this.stronghold = new Stronghold(seed, terrain);
		this.features = List.of(new Mineshaft(seed), new Village(seed, terrain), stronghold, new Temple(seed, terrain));
	}

	public Stronghold stronghold() {
		return stronghold;
	}

	public List<LegacyStructureFeature> features() {
		return features;
	}

	public long seed() {
		return seed;
	}

	/** Places the structures 1.6.4 placed while populating this chunk, using populate's own random. */
	public void populate(WorldGenLevel level, int chunkX, int chunkZ) {
		Random random = new Random(seed);
		long xScale = random.nextLong() / 2L * 2L + 1L;
		long zScale = random.nextLong() / 2L * 2L + 1L;
		random.setSeed(chunkX * xScale + chunkZ * zScale ^ seed);
		LegacyWorld world = new LegacyWorld(level, seed);
		for (LegacyStructureFeature feature : features) {
			feature.generate(world, random, chunkX, chunkZ);
		}
	}
}
