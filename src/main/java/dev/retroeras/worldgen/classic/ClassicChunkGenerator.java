package dev.retroeras.worldgen.classic;

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
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * Generates the finite Classic 0.30 level ({@link ClassicTerrain}). Around it lies what Classic drew past the edge of
 * the map: bedrock up to two blocks below sea level and still water above it, stretching off to the horizon.
 */
public final class ClassicChunkGenerator extends ChunkGenerator {
	public static final MapCodec<ClassicChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
			.xmap(ClassicChunkGenerator::new, generator -> generator.biomeSource);
	private static final int EDGE_GROUND = ClassicTerrain.WATER_LEVEL - 2;

	private volatile @Nullable ClassicTerrain terrain;

	public ClassicChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	public ClassicTerrain terrain(long seed) {
		ClassicTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new ClassicTerrain(seed);
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
			fill(chunk, terrain(randomState.seed()));
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	/** The block at a position, including the sea and bedrock past the edge of the map. */
	private static BlockState stateAt(ClassicTerrain terrain, int x, int y, int z) {
		if (ClassicTerrain.inWorld(x, z)) {
			return toState(terrain.get(x, y, z));
		}
		if (y < EDGE_GROUND) {
			return Blocks.BEDROCK.defaultBlockState();
		}
		return y < ClassicTerrain.WATER_LEVEL ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
	}

	private static void fill(ChunkAccess chunk, ClassicTerrain terrain) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		int top = Math.min(ClassicTerrain.HEIGHT, chunk.getMaxY() + 1);
		Set<LevelChunkSection> sections = new HashSet<>();
		for (int y = 0; y < top; y += 16) {
			LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
			section.acquire();
			sections.add(section);
		}
		try {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = top - 1; y >= 0; y--) {
						BlockState state = stateAt(terrain, minX + x, y, minZ + z);
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
			case ClassicTerrain.STONE -> Blocks.STONE.defaultBlockState();
			case ClassicTerrain.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case ClassicTerrain.DIRT -> Blocks.DIRT.defaultBlockState();
			case ClassicTerrain.WATER, ClassicTerrain.WATER_FLOWING -> Blocks.WATER.defaultBlockState();
			case ClassicTerrain.LAVA, ClassicTerrain.LAVA_FLOWING -> Blocks.LAVA.defaultBlockState();
			case ClassicTerrain.SAND -> Blocks.SAND.defaultBlockState();
			case ClassicTerrain.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			case ClassicTerrain.GOLD_ORE -> Blocks.GOLD_ORE.defaultBlockState();
			case ClassicTerrain.IRON_ORE -> Blocks.IRON_ORE.defaultBlockState();
			case ClassicTerrain.COAL_ORE -> Blocks.COAL_ORE.defaultBlockState();
			case ClassicTerrain.LOG -> Blocks.OAK_LOG.defaultBlockState();
			case ClassicTerrain.LEAVES -> Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
			case ClassicTerrain.DANDELION -> Blocks.DANDELION.defaultBlockState();
			case ClassicTerrain.ROSE -> Blocks.POPPY.defaultBlockState();
			case ClassicTerrain.BROWN_MUSHROOM -> Blocks.BROWN_MUSHROOM.defaultBlockState();
			case ClassicTerrain.RED_MUSHROOM -> Blocks.RED_MUSHROOM.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		ClassicTerrain current = terrain(randomState.seed());
		for (int y = ClassicTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(stateAt(current, x, y, z))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		ClassicTerrain current = terrain(randomState.seed());
		BlockState[] column = new BlockState[ClassicTerrain.HEIGHT];
		for (int y = 0; y < ClassicTerrain.HEIGHT; y++) {
			column[y] = stateAt(current, x, y, z);
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Classic 0.30 level, seed " + randomState.seed());
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return ClassicTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return ClassicTerrain.WATER_LEVEL;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
