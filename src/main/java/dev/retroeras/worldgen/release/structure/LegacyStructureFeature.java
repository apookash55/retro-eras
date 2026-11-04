package dev.retroeras.worldgen.release.structure;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.6.4's MapGenStructure: decides which chunks start a structure and places the structures reaching into each chunk
 * being populated. Starts are worked out from the seed alone, so they are cached here rather than saved with the world.
 */
public abstract class LegacyStructureFeature {
	/** MapGenBase.range: how many chunks away a structure can start and still reach a chunk. */
	private static final int RANGE = 8;

	protected final long seed;
	private final long xScale;
	private final long zScale;
	private final ConcurrentHashMap<Long, Optional<LegacyStart>> starts = new ConcurrentHashMap<>();

	protected LegacyStructureFeature(long seed) {
		this.seed = seed;
		Random random = new Random(seed);
		this.xScale = random.nextLong();
		this.zScale = random.nextLong();
	}

	/** World.setRandomSeed: the random 1.6.4 used to place one structure per region of chunks. */
	protected Random regionRandom(int regionX, int regionZ, int salt) {
		return new Random(regionX * 341873128712L + regionZ * 132897987541L + seed + salt);
	}

	/** canSpawnStructureAtCoords, given the generator's random already seeded for this chunk. */
	protected abstract boolean canSpawnAt(Random random, int chunkX, int chunkZ);

	/** getStructureStart. */
	protected abstract LegacyStart createStart(Random random, int chunkX, int chunkZ);

	/** The structure that starts in this chunk, seeded the way MapGenBase.generate seeded recursiveGenerate. */
	public Optional<LegacyStart> startAt(int chunkX, int chunkZ) {
		return starts.computeIfAbsent(((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL), key -> {
			Random random = new Random((long) chunkX * xScale ^ (long) chunkZ * zScale ^ seed);
			random.nextInt();
			return canSpawnAt(random, chunkX, chunkZ) ? Optional.of(createStart(random, chunkX, chunkZ)) : Optional.empty();
		});
	}

	/**
	 * generateStructuresInChunk: places every structure reaching into the 16x16 area that 1.6.4 populated for this chunk,
	 * which starts 8 blocks in from the chunk's corner.
	 */
	public boolean generate(LegacyWorld world, Random random, int chunkX, int chunkZ) {
		int x = (chunkX << 4) + 8;
		int z = (chunkZ << 4) + 8;
		boolean placed = false;
		for (int cx = chunkX - RANGE; cx <= chunkX + RANGE; cx++) {
			for (int cz = chunkZ - RANGE; cz <= chunkZ + RANGE; cz++) {
				Optional<LegacyStart> start = startAt(cx, cz);
				if (start.isPresent() && start.get().isValid() && start.get().box().intersectsXZ(x, z, x + 15, z + 15)) {
					start.get().place(world, random, new LegacyBox(x, z, x + 15, z + 15));
					placed = true;
				}
			}
		}
		return placed;
	}
}
