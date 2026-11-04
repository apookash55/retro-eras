package dev.retroeras.worldgen.cavegame;

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

/** Generates the finite Cave game world ({@link CavegameTerrain}); chunks outside it stay empty. No mobs spawn. */
public final class CavegameChunkGenerator extends ChunkGenerator {
	public static final MapCodec<CavegameChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
			.xmap(CavegameChunkGenerator::new, generator -> generator.biomeSource);

	private volatile @Nullable CavegameTerrain terrain;

	public CavegameChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	public CavegameTerrain terrain(long seed) {
		CavegameTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new CavegameTerrain(seed);
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
		ChunkPos pos = chunk.getPos();
		if (!CavegameTerrain.inWorld(pos.getMinBlockX(), pos.getMinBlockZ())) {
			return CompletableFuture.completedFuture(chunk);
		}
		return CompletableFuture.supplyAsync(() -> {
			fill(chunk, terrain(randomState.seed()));
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	private static void fill(ChunkAccess chunk, CavegameTerrain terrain) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		int top = Math.min(CavegameTerrain.HEIGHT, chunk.getMaxY() + 1);

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
						BlockState state = toState(terrain.get(minX + x, y, minZ + z));
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
			case CavegameTerrain.ROCK -> Blocks.STONE.defaultBlockState();
			case CavegameTerrain.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case CavegameTerrain.DIRT -> Blocks.DIRT.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		CavegameTerrain current = terrain(randomState.seed());
		for (int y = CavegameTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(toState(current.get(x, y, z)))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		CavegameTerrain current = terrain(randomState.seed());
		BlockState[] column = new BlockState[CavegameTerrain.HEIGHT];
		for (int y = 0; y < CavegameTerrain.HEIGHT; y++) {
			column[y] = toState(current.get(x, y, z));
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("Cave game rd-161348 terrain, seed " + randomState.seed());
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return CavegameTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
