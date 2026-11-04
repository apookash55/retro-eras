package dev.retroeras.worldgen.release;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Places biomes with Minecraft 1.6.4's GenLayer chain. The JSON names the biome to use for each 1.6.4 biome:
 * {@code "biomes": {"plains": "retroeras:release/plains", ...}}. Like the Beta source, it is handed the world seed by
 * its chunk generator.
 */
public final class ReleaseBiomeSource extends BiomeSource {
	public static final MapCodec<ReleaseBiomeSource> CODEC = Codec.unboundedMap(Codec.STRING, Biome.CODEC)
			.comapFlatMap(ReleaseBiomeSource::create, source -> source.byName)
			.fieldOf("biomes");

	private final Map<String, Holder<Biome>> byName;
	private final Map<ReleaseBiome, Holder<Biome>> byRelease = new EnumMap<>(ReleaseBiome.class);
	private volatile ReleaseTerrain terrain;

	private ReleaseBiomeSource(Map<String, Holder<Biome>> byName) {
		this.byName = byName;
		for (ReleaseBiome biome : ReleaseBiome.values()) {
			byRelease.put(biome, byName.get(biome.id()));
		}
	}

	private static DataResult<ReleaseBiomeSource> create(Map<String, Holder<Biome>> byName) {
		List<String> missing = Stream.of(ReleaseBiome.values()).map(ReleaseBiome::id).filter(id -> !byName.containsKey(id)).toList();
		return missing.isEmpty()
				? DataResult.success(new ReleaseBiomeSource(Map.copyOf(byName)))
				: DataResult.error(() -> "Release biome source is missing biomes for " + missing);
	}

	void init(ReleaseTerrain terrain) {
		this.terrain = terrain;
	}

	Holder<Biome> holder(ReleaseBiome biome) {
		return byRelease.get(biome);
	}

	@Override
	protected MapCodec<ReleaseBiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return byRelease.values().stream();
	}

	@Override
	public BiomeResolver createResolver(Climate.Sampler sampler) {
		return (quartX, quartY, quartZ) -> {
			ReleaseTerrain current = terrain;
			if (current == null) {
				return byRelease.get(ReleaseBiome.PLAINS);
			}
			return holder(current.biomeAt(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2));
		};
	}

	@Override
	public void addDebugInfo(List<String> result, BlockPos feetPos, Climate.Sampler sampler) {
		ReleaseTerrain current = terrain;
		if (current != null) {
			result.add("1.6.4 biome: " + current.biomeAt(feetPos.getX(), feetPos.getZ()).id());
		}
	}
}
