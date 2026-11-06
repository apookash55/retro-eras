package dev.retroeras.worldgen.beta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
 * Generates terrain with Beta 1.7.3's own algorithm ({@link BetaTerrain}) and places biomes with its climate
 * ({@link BetaBiomeSource}). Decoration is ported too ({@link BetaDecorator}).
 */
public final class BetaChunkGenerator extends ChunkGenerator {
	private static final MapCodec<BetaBiomeSource> BETA_BIOME_SOURCE = BiomeSource.CODEC.comapFlatMap(
			source -> source instanceof BetaBiomeSource beta
					? DataResult.success(beta)
					: DataResult.error(() -> "The Beta chunk generator needs a retroeras:beta biome source"),
			source -> source).fieldOf("biome_source");
	/** {@code sandstone: false} is Alpha 1.2.6, whose generator was the same apart from Beta 1.2's sandstone under sand. */
	public static final MapCodec<BetaChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BETA_BIOME_SOURCE.forGetter(generator -> generator.betaBiomes),
			Codec.BOOL.optionalFieldOf("sandstone", true).forGetter(generator -> generator.sandstone)
	).apply(instance, BetaChunkGenerator::new));

	private final BetaBiomeSource betaBiomes;
	private final boolean sandstone;
	private volatile @Nullable BetaTerrain terrain;

	public BetaChunkGenerator(BetaBiomeSource biomeSource, boolean sandstone) {
		super(biomeSource);
		this.betaBiomes = biomeSource;
		this.sandstone = sandstone;
	}

	/** Terrain for the world seed, created on first use. Also seeds the biome source. */
	public BetaTerrain terrain(long seed) {
		BetaTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new BetaTerrain(seed, sandstone);
					terrain = current;
				}
			}
		}
		betaBiomes.init(seed);
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
			BetaClimate.Area area = terrain(randomState.seed()).climate().sample(pos.getMinBlockX(), pos.getMinBlockZ(), 16, 16);
			chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> {
				int x = QuartPos.toBlock(quartX) - pos.getMinBlockX() + 2;
				int z = QuartPos.toBlock(quartZ) - pos.getMinBlockZ() + 2;
				return betaBiomes.holder(area.biomes()[x * 16 + z]);
			});
			return chunk;
		}, Util.backgroundExecutor().forName("createBiomes"));
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
			BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
		return CompletableFuture.supplyAsync(() -> {
			ChunkPos pos = chunk.getPos();
			BetaTerrain.Chunk generated = terrain(randomState.seed()).generate(pos.x(), pos.z(), true);
			fill(chunk, generated);
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	private static void fill(ChunkAccess chunk, BetaTerrain.Chunk generated) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int top = Math.min(BetaTerrain.HEIGHT, chunk.getMaxY() + 1);
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
			case BetaTerrain.STONE -> Blocks.STONE.defaultBlockState();
			case BetaTerrain.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case BetaTerrain.DIRT -> Blocks.DIRT.defaultBlockState();
			case BetaTerrain.BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case BetaTerrain.WATER -> Blocks.WATER.defaultBlockState();
			case BetaTerrain.LAVA -> Blocks.LAVA.defaultBlockState();
			case BetaTerrain.SAND -> Blocks.SAND.defaultBlockState();
			case BetaTerrain.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			case BetaTerrain.SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
			case BetaTerrain.ICE -> Blocks.ICE.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		BetaTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		for (int y = BetaTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(toState(generated.get(x & 15, y, z & 15)))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		BetaTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		BlockState[] column = new BlockState[BetaTerrain.HEIGHT];
		for (int y = 0; y < BetaTerrain.HEIGHT; y++) {
			column[y] = toState(generated.get(x & 15, y, z & 15));
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		BetaDecorator.decorate(level, chunk.getPos(), terrain(level.getSeed()), !sandstone);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Beta 1.7.3 terrain, seed " + randomState.seed());
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
		return BetaTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return BetaTerrain.SEA_LEVEL;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
