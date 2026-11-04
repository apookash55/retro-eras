package dev.retroeras.worldgen.release;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import dev.retroeras.worldgen.release.structure.ReleaseStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * Generates terrain with Minecraft 1.6.4's own algorithm ({@link ReleaseTerrain}) and places biomes with its GenLayer
 * chain ({@link ReleaseBiomeSource}). Decoration (trees, ores, flowers) still runs through the biomes' modern feature
 * lists. The world is 256 tall but, as in 1.6.4, terrain stops at y = 127.
 */
public final class ReleaseChunkGenerator extends ChunkGenerator {
	private static final MapCodec<ReleaseBiomeSource> RELEASE_BIOME_SOURCE = BiomeSource.CODEC.comapFlatMap(
			source -> source instanceof ReleaseBiomeSource release
					? DataResult.success(release)
					: DataResult.error(() -> "The 1.6.4 chunk generator needs a retroeras:release biome source"),
			source -> source).fieldOf("biome_source");
	public static final MapCodec<ReleaseChunkGenerator> CODEC = RELEASE_BIOME_SOURCE.xmap(ReleaseChunkGenerator::new, generator -> generator.releaseBiomes);

	private final ReleaseBiomeSource releaseBiomes;
	private volatile @Nullable ReleaseTerrain terrain;
	private volatile @Nullable ReleaseStructures structures;

	public ReleaseChunkGenerator(ReleaseBiomeSource biomeSource) {
		super(biomeSource);
		this.releaseBiomes = biomeSource;
	}

	/** Terrain for the world seed, created on first use. Also seeds the biome source. */
	public ReleaseTerrain terrain(long seed) {
		ReleaseTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new ReleaseTerrain(seed);
					terrain = current;
				}
			}
		}
		releaseBiomes.init(current);
		return current;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
		return CompletableFuture.supplyAsync(() -> {
			ChunkPos pos = chunk.getPos();
			int[] biomes = terrain(randomState.seed()).chunkBiomes(pos.x(), pos.z());
			chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> {
				int x = QuartPos.toBlock(quartX) - pos.getMinBlockX() + 2;
				int z = QuartPos.toBlock(quartZ) - pos.getMinBlockZ() + 2;
				return releaseBiomes.holder(ReleaseBiome.byIdOrOcean(biomes[x + z * 16]));
			});
			return chunk;
		}, Util.backgroundExecutor().forName("createBiomes"));
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
			BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
		return CompletableFuture.supplyAsync(() -> {
			ChunkPos pos = chunk.getPos();
			ReleaseTerrain.Chunk generated = terrain(randomState.seed()).generate(pos.x(), pos.z(), true);
			fill(chunk, generated);
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	private static void fill(ChunkAccess chunk, ReleaseTerrain.Chunk generated) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int top = Math.min(ReleaseTerrain.HEIGHT, chunk.getMaxY() + 1);
		int bottom = Math.max(0, chunk.getMinY());

		Set<LevelChunkSection> sections = new HashSet<>();
		for (int y = bottom; y < top; y += 16) {
			LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
			section.acquire();
			sections.add(section);
		}
		try {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = top - 1; y >= bottom; y--) {
						BlockState state = toState(generated.get(x, y, z));
						if (state.isAir()) {
							continue;
						}
						chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, SectionPos.sectionRelative(y), z, state, false);
						oceanFloor.update(x, y, z, state);
						worldSurface.update(x, y, z, state);
					}
				}
			}
		} finally {
			sections.forEach(LevelChunkSection::release);
		}
	}

	static BlockState toState(byte id) {
		return switch (id) {
			case ReleaseTerrain.STONE -> Blocks.STONE.defaultBlockState();
			case ReleaseTerrain.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case ReleaseTerrain.DIRT -> Blocks.DIRT.defaultBlockState();
			case ReleaseTerrain.BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case ReleaseTerrain.WATER -> Blocks.WATER.defaultBlockState();
			case ReleaseTerrain.LAVA -> Blocks.LAVA.defaultBlockState();
			case ReleaseTerrain.SAND -> Blocks.SAND.defaultBlockState();
			case ReleaseTerrain.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			case ReleaseTerrain.SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
			case ReleaseTerrain.ICE -> Blocks.ICE.defaultBlockState();
			case ReleaseTerrain.MYCELIUM -> Blocks.MYCELIUM.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		ReleaseTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		for (int y = ReleaseTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(toState(generated.get(x & 15, y, z & 15)))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		ReleaseTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		BlockState[] column = new BlockState[ReleaseTerrain.HEIGHT];
		for (int y = 0; y < ReleaseTerrain.HEIGHT; y++) {
			column[y] = toState(generated.get(x & 15, y, z & 15));
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Minecraft 1.6.4 terrain, seed " + randomState.seed());
	}

	/** 1.6.4's structures go in before the biome features, as in its populate step. */
	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		ChunkPos pos = chunk.getPos();
		structures(level.getSeed()).populate(level, pos.x(), pos.z());
		super.applyBiomeDecoration(level, chunk, structureManager);
	}

	/** Structures for the world seed, created on first use. */
	public ReleaseStructures structures(long seed) {
		ReleaseStructures current = structures;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = structures;
				if (current == null || current.seed() != seed) {
					current = new ReleaseStructures(terrain(seed));
					structures = current;
				}
			}
		}
		return current;
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		ChunkPos center = region.getCenter();
		BlockPos sourcePos = center.getWorldPosition().atY(region.getMaxY());
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
		random.setDecorationSeed(region.getSeed(), center.getMinBlockX(), center.getMinBlockZ());
		NaturalSpawner.spawnMobsForChunkGeneration(region, sourcePos, center, random);
	}

	@Override
	public int getGenDepth() {
		return ReleaseTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return ReleaseTerrain.SEA_LEVEL;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
