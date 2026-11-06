package dev.retroeras.worldgen.end;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
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
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * 1.6.4's End ({@link EndTerrain}), with BiomeEndDecorator's decoration: an obsidian pillar with an ender crystal in one
 * chunk in five, and the dragon, placed when the chunk at the origin is first made. Chunks are not generated twice, so
 * the dragon does not come back once it is dead, as in 1.6.4.
 */
public final class EndChunkGenerator extends ChunkGenerator {
	public static final MapCodec<EndChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
			.xmap(EndChunkGenerator::new, generator -> generator.biomeSource);

	private volatile @Nullable EndTerrain terrain;

	public EndChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	public EndTerrain terrain(long seed) {
		EndTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new EndTerrain(seed);
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
			fill(chunk, terrain(randomState.seed()).generate(pos.x(), pos.z()));
			return chunk;
		}, Util.backgroundExecutor().forName("buildTerrain"));
	}

	private static void fill(ChunkAccess chunk, EndTerrain.Chunk generated) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		BlockState endStone = Blocks.END_STONE.defaultBlockState();
		Set<LevelChunkSection> sections = new HashSet<>();
		for (int y = 0; y < EndTerrain.HEIGHT; y += 16) {
			LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
			section.acquire();
			sections.add(section);
		}
		try {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = EndTerrain.HEIGHT - 1; y >= 0; y--) {
						if (generated.get(x, y, z)) {
							chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, SectionPos.sectionRelative(y), z, endStone, false);
							oceanFloor.update(x, y, z, endStone);
							worldSurface.update(x, y, z, endStone);
						}
					}
				}
			}
		} finally {
			sections.forEach(LevelChunkSection::release);
		}
	}

	/** BiomeEndDecorator: the old game used the world's running random, so here each chunk gets its own from the seed. */
	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		ChunkPos pos = chunk.getPos();
		Random random = new Random(level.getSeed());
		long xScale = random.nextLong() / 2L * 2L + 1L;
		long zScale = random.nextLong() / 2L * 2L + 1L;
		random.setSeed(pos.x() * xScale + pos.z() * zScale ^ level.getSeed());
		int blockX = pos.getMinBlockX();
		int blockZ = pos.getMinBlockZ();
		if (random.nextInt(5) == 0) {
			int x = blockX + random.nextInt(16) + 8;
			int z = blockZ + random.nextInt(16) + 8;
			spike(level, random, x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
		}
		if (pos.x() == 0 && pos.z() == 0) {
			EnderDragon dragon = EntityTypes.ENDER_DRAGON.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (dragon != null) {
				dragon.snapTo(0.0, 128.0, 0.0, random.nextFloat() * 360.0F, 0.0F);
				level.addFreshEntity(dragon);
			}
		}
	}

	/** WorldGenSpikes: an obsidian pillar 6 to 37 blocks tall on flat end stone, with a crystal burning on top. */
	private static void spike(WorldGenLevel level, Random random, int x, int y, int z) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		if (!level.getBlockState(cursor.set(x, y, z)).isAir() || !level.getBlockState(cursor.set(x, y - 1, z)).is(Blocks.END_STONE)) {
			return;
		}
		int height = random.nextInt(32) + 6;
		int radius = random.nextInt(4) + 1;
		for (int bx = x - radius; bx <= x + radius; bx++) {
			for (int bz = z - radius; bz <= z + radius; bz++) {
				int dx = bx - x;
				int dz = bz - z;
				if (dx * dx + dz * dz <= radius * radius + 1 && !level.getBlockState(cursor.set(bx, y - 1, bz)).is(Blocks.END_STONE)) {
					return;
				}
			}
		}
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		for (int by = y; by < y + height && by < EndTerrain.HEIGHT; by++) {
			for (int bx = x - radius; bx <= x + radius; bx++) {
				for (int bz = z - radius; bz <= z + radius; bz++) {
					int dx = bx - x;
					int dz = bz - z;
					if (dx * dx + dz * dz <= radius * radius + 1) {
						level.setBlock(cursor.set(bx, by, bz), obsidian, 2);
					}
				}
			}
		}
		EndCrystal crystal = EntityTypes.END_CRYSTAL.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (crystal != null) {
			// The old crystal stood half its height up, so it burns on top of the bedrock rather than in it.
			crystal.snapTo(x + 0.5, y + height + 1, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			level.addFreshEntity(crystal);
		}
		level.setBlock(cursor.set(x, y + height, z), Blocks.BEDROCK.defaultBlockState(), 2);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		EndTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4);
		for (int y = EndTerrain.HEIGHT - 1; y >= 0; y--) {
			if (generated.get(x & 15, y, z & 15)) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		EndTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4);
		BlockState[] column = new BlockState[EndTerrain.HEIGHT];
		for (int y = 0; y < EndTerrain.HEIGHT; y++) {
			column[y] = generated.get(x & 15, y, z & 15) ? Blocks.END_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add("1.6.4 End, seed " + randomState.seed());
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return 256;
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
