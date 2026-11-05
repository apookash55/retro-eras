package dev.retroeras.worldgen.nether;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.retroeras.worldgen.release.structure.Fortress;
import dev.retroeras.worldgen.release.structure.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
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
 * The old Nether ({@link NetherTerrain}), with ChunkProviderHell's own decoration and, for 1.6.4, its nether fortresses.
 * {@code version} is {@code beta} for the Nether of Alpha 1.2 to Beta 1.7.3, or {@code release} for 1.6.4's.
 */
public final class NetherChunkGenerator extends ChunkGenerator {
	private static final Codec<NetherTerrain.Version> VERSION = Codec.STRING.xmap(
			name -> NetherTerrain.Version.valueOf(name.toUpperCase(Locale.ROOT)), version -> version.name().toLowerCase(Locale.ROOT));
	public static final MapCodec<NetherChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
			VERSION.fieldOf("version").forGetter(generator -> generator.version)
	).apply(instance, NetherChunkGenerator::new));

	/** MapGenNetherBridge's spawn list. Skeletons spawned in the 1.6.4 Nether were wither skeletons four times in five. */
	private static final WeightedList<MobSpawnSettings.SpawnerData> FORTRESS_MOBS = WeightedList.<MobSpawnSettings.SpawnerData>builder()
			.add(spawner(EntityTypes.BLAZE, 2, 3), 10)
			.add(spawner(EntityTypes.ZOMBIFIED_PIGLIN, 4, 4), 5)
			.add(spawner(EntityTypes.WITHER_SKELETON, 4, 4), 8)
			.add(spawner(EntityTypes.SKELETON, 4, 4), 2)
			.add(spawner(EntityTypes.MAGMA_CUBE, 4, 4), 3)
			.build();

	private final NetherTerrain.Version version;
	private volatile @Nullable NetherTerrain terrain;
	private volatile @Nullable Fortress fortress;

	public NetherChunkGenerator(BiomeSource biomeSource, NetherTerrain.Version version) {
		super(biomeSource);
		this.version = version;
	}

	private static MobSpawnSettings.SpawnerData spawner(EntityType<?> type, int min, int max) {
		return new MobSpawnSettings.SpawnerData(type, UniformInt.of(min, max));
	}

	public NetherTerrain.Version version() {
		return version;
	}

	/** Terrain for the world seed, created on first use. */
	public NetherTerrain terrain(long seed) {
		NetherTerrain current = terrain;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = terrain;
				if (current == null || current.seed() != seed) {
					current = new NetherTerrain(seed, version);
					terrain = current;
				}
			}
		}
		return current;
	}

	/** 1.6.4's fortresses for the world seed, or null for the Beta Nether, which had none. */
	public @Nullable Fortress fortress(long seed) {
		if (version != NetherTerrain.Version.RELEASE) {
			return null;
		}
		Fortress current = fortress;
		if (current == null || current.seed() != seed) {
			synchronized (this) {
				current = fortress;
				if (current == null || current.seed() != seed) {
					current = new Fortress(seed);
					fortress = current;
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

	private static void fill(ChunkAccess chunk, NetherTerrain.Chunk generated) {
		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int top = Math.min(NetherTerrain.HEIGHT, chunk.getMaxY() + 1);
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
			case NetherTerrain.NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
			case NetherTerrain.SOUL_SAND -> Blocks.SOUL_SAND.defaultBlockState();
			case NetherTerrain.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
			case NetherTerrain.BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case NetherTerrain.LAVA, NetherTerrain.FLOWING_LAVA -> Blocks.LAVA.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
	}

	/** 1.6.4 placed its fortresses before the rest of the decoration, as here. */
	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		ChunkPos pos = chunk.getPos();
		long seed = level.getSeed();
		Fortress fortress = fortress(seed);
		if (fortress != null) {
			java.util.Random random = new java.util.Random(seed);
			long xScale = random.nextLong() / 2L * 2L + 1L;
			long zScale = random.nextLong() / 2L * 2L + 1L;
			random.setSeed(pos.x() * xScale + pos.z() * zScale ^ seed ^ 0x5DEECE66DL);
			fortress.generate(new LegacyWorld(level, seed), random, pos.x(), pos.z());
		}
		NetherDecorator.decorate(level, seed, pos.x(), pos.z(), version);
		super.applyBiomeDecoration(level, chunk, structureManager);
	}

	/** In 1.6.4 blazes, wither skeletons and magma cubes spawn in fortresses rather than the Nether's own mobs. */
	@Override
	public WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(Level level, StructureManager structureManager, MobCategory category, BlockPos pos) {
		Fortress fortress = category == MobCategory.MONSTER && level instanceof net.minecraft.server.level.ServerLevel server ? fortress(server.getSeed()) : null;
		if (fortress != null && fortress.spawnsFortressMobs(pos, level.getBlockState(pos.below()).is(Blocks.NETHER_BRICKS))) {
			return FORTRESS_MOBS;
		}
		return super.getMobsAt(level, structureManager, category, pos);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
		NetherTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		for (int y = NetherTerrain.HEIGHT - 1; y >= 0; y--) {
			if (type.isOpaque().test(toState(generated.get(x & 15, y, z & 15)))) {
				return y + 1;
			}
		}
		return getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
		NetherTerrain.Chunk generated = terrain(randomState.seed()).generate(x >> 4, z >> 4, false);
		BlockState[] column = new BlockState[NetherTerrain.HEIGHT];
		for (int y = 0; y < NetherTerrain.HEIGHT; y++) {
			column[y] = toState(generated.get(x & 15, y, z & 15));
		}
		return new NoiseColumn(0, column);
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		result.add((version == NetherTerrain.Version.RELEASE ? "1.6.4" : "Beta 1.7.3") + " Nether, seed " + randomState.seed());
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
		return NetherTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return NetherTerrain.LAVA_LEVEL;
	}

	@Override
	public int getMinY() {
		return 0;
	}
}
