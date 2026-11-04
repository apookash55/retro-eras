package dev.retroeras.worldgen.beta;

import java.util.Random;

/** Beta 1.7.3's WorldChunkManager: per-block temperature and humidity from simplex noise, and the biome they select. */
public final class BetaClimate {
	private final long seed;
	private final BetaSimplexNoise.Octaves temperatureNoise;
	private final BetaSimplexNoise.Octaves humidityNoise;
	private final BetaSimplexNoise.Octaves weirdnessNoise;

	public BetaClimate(long seed) {
		this.seed = seed;
		temperatureNoise = new BetaSimplexNoise.Octaves(new Random(seed * 9871L), 4);
		humidityNoise = new BetaSimplexNoise.Octaves(new Random(seed * 39811L), 4);
		weirdnessNoise = new BetaSimplexNoise.Octaves(new Random(seed * 543321L), 2);
	}

	public long seed() {
		return seed;
	}

	/** Climate for a block-aligned area, indexed {@code x * sizeZ + z} like Beta's arrays. */
	public record Area(double[] temperature, double[] humidity, BetaBiome[] biomes) {
	}

	/** Beta's loadBlockGeneratorData. */
	public Area sample(int x, int z, int sizeX, int sizeZ) {
		double[] temperature = temperatureNoise.sample(null, x, z, sizeX, sizeZ, 0.025, 0.025, 0.25);
		double[] humidity = humidityNoise.sample(null, x, z, sizeX, sizeZ, 0.05, 0.05, 1.0 / 3.0);
		double[] weirdness = weirdnessNoise.sample(null, x, z, sizeX, sizeZ, 0.25, 0.25, 0.5882352941176471);
		BetaBiome[] biomes = new BetaBiome[sizeX * sizeZ];
		for (int i = 0; i < sizeX * sizeZ; i++) {
			double weird = weirdness[i] * 1.1 + 0.5;
			double t = (temperature[i] * 0.15 + 0.7) * (1.0 - 0.01) + weird * 0.01;
			double h = (humidity[i] * 0.15 + 0.5) * (1.0 - 0.002) + weird * 0.002;
			t = 1.0 - (1.0 - t) * (1.0 - t);
			t = Math.clamp(t, 0.0, 1.0);
			h = Math.clamp(h, 0.0, 1.0);
			temperature[i] = t;
			humidity[i] = h;
			biomes[i] = BetaBiome.lookup(t, h);
		}
		return new Area(temperature, humidity, biomes);
	}

	public BetaBiome biomeAt(int x, int z) {
		return sample(x, z, 1, 1).biomes()[0];
	}
}
