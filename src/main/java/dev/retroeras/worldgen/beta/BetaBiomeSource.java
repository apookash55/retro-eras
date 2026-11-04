package dev.retroeras.worldgen.beta;

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
 * Places biomes with Beta 1.7.3's own climate noise and lookup table. The JSON names which biome to use for each Beta
 * biome: {@code "biomes": {"rainforest": "retroeras:beta/rainforest", ...}}.
 * <p>
 * Biome sources are not given the world seed, so {@link BetaChunkGenerator} hands it over via {@link #init(long)}.
 */
public final class BetaBiomeSource extends BiomeSource {
	public static final MapCodec<BetaBiomeSource> CODEC = Codec.unboundedMap(Codec.STRING, Biome.CODEC)
			.comapFlatMap(BetaBiomeSource::create, source -> source.byName)
			.fieldOf("biomes");

	private final Map<String, Holder<Biome>> byName;
	private final Map<BetaBiome, Holder<Biome>> byBeta = new EnumMap<>(BetaBiome.class);
	private volatile BetaClimate climate;

	private BetaBiomeSource(Map<String, Holder<Biome>> byName) {
		this.byName = byName;
		for (BetaBiome biome : BetaBiome.values()) {
			byBeta.put(biome, byName.get(biome.id()));
		}
	}

	private static DataResult<BetaBiomeSource> create(Map<String, Holder<Biome>> byName) {
		List<String> missing = Stream.of(BetaBiome.values()).map(BetaBiome::id).filter(id -> !byName.containsKey(id)).toList();
		return missing.isEmpty()
				? DataResult.success(new BetaBiomeSource(Map.copyOf(byName)))
				: DataResult.error(() -> "Beta biome source is missing biomes for " + missing);
	}

	/** Sets the world seed; cheap to call repeatedly with the same seed. */
	void init(long seed) {
		BetaClimate current = climate;
		if (current == null || current.seed() != seed) {
			climate = new BetaClimate(seed);
		}
	}

	Holder<Biome> holder(BetaBiome biome) {
		return byBeta.get(biome);
	}

	@Override
	protected MapCodec<BetaBiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return byBeta.values().stream();
	}

	@Override
	public BiomeResolver createResolver(Climate.Sampler sampler) {
		return this::biomeAtQuart;
	}

	private Holder<Biome> biomeAtQuart(int quartX, int quartY, int quartZ) {
		BetaClimate current = climate;
		if (current == null) {
			return byBeta.get(BetaBiome.PLAINS);
		}
		return holder(current.biomeAt(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2));
	}

	@Override
	public void addDebugInfo(List<String> result, BlockPos feetPos, Climate.Sampler sampler) {
		BetaClimate current = climate;
		if (current != null) {
			BetaClimate.Area area = current.sample(feetPos.getX(), feetPos.getZ(), 1, 1);
			result.add(String.format("Beta climate: T %.3f H %.3f -> %s", area.temperature()[0], area.humidity()[0], area.biomes()[0].id()));
		}
	}
}
