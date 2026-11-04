package dev.retroeras.worldgen.infdev;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
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
 * Generates terrain with Infdev 20100618's own algorithm ({@link InfdevTerrain}). Infdev had no biomes, so the
 * dimension uses one fixed biome whose feature list provides the decoration (oak trees, flowers, ores, springs).
 */
public final class InfdevChunkGenerator extends ChunkGenerator {
	public static final MapCodec<InfdevChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
			.xmap(InfdevChunkGenerator::new, generator -> generator.biomeSource);

	private volatile @Nullable InfdevTerrain terrain;

	public InfdevChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	public InfdevTerrain terrain(long seed) {
		InfdevTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new InfdevTerrain(seed);
					terrain = current;
				}
			}
		}
		return current;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
			BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
		return CompletableFuture.supplyAsync(() -> {
			ChunkPos pos = chunk.getPos();
			fill(chunk, terrain(randomState.seed()).generate(pos.x(), pos.z(), true));
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	private static void fill(ChunkAccess chunk, InfdevTerrain.Chunk generated) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int top = Math.min(InfdevTerrain.HEIGHT, chunk.getMaxY() + 1);
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

	private static BlockState toState(byte id) {
		return switch (id) {
			case InfdevTerrain.STONE -> Blocks.STONE.defaultBlockState();
			case InfdevTerrain.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case InfdevTerrain.DIRT -> Blocks.DIRT.defaultBlockState();
			case InfdevTerrain.BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case InfdevTerrain.WATER -> Blocks.WATER.defaultBlockState();
			case InfdevTerrain.LAVA -> Blocks.LAVA.defaultBlockState();
			case InfdevTerrain.SAND -> Blocks.SAND.defaultBlockState();
			case InfdevTerrain.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		InfdevTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		for (int y = InfdevTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(toState(generated.get(x & 15, y, z & 15)))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		InfdevTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		BlockState[] column = new BlockState[InfdevTerrain.HEIGHT];
		for (int y = 0; y < InfdevTerrain.HEIGHT; y++) {
			column[y] = toState(generated.get(x & 15, y, z & 15));
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Infdev 20100618 terrain, seed " + randomState.seed());
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
		return InfdevTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return InfdevTerrain.SEA_LEVEL;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
