package dev.retroeras.worldgen.beta;

/** Beta 1.7.3's biomes, chosen from temperature and humidity through its 64x64 lookup table (BiomeGenBase.getBiome). */
public enum BetaBiome {
	RAINFOREST("rainforest", false),
	SWAMPLAND("swampland", false),
	SEASONAL_FOREST("seasonal_forest", false),
	FOREST("forest", false),
	SAVANNA("savanna", false),
	SHRUBLAND("shrubland", false),
	TAIGA("taiga", false),
	DESERT("desert", true),
	PLAINS("plains", false),
	TUNDRA("tundra", false);

	private static final BetaBiome[] LOOKUP = new BetaBiome[64 * 64];

	static {
		for (int t = 0; t < 64; t++) {
			for (int h = 0; h < 64; h++) {
				LOOKUP[t + h * 64] = compute(t / 63.0F, h / 63.0F);
			}
		}
	}

	private final String id;
	private final boolean sandy;

	BetaBiome(String id, boolean sandy) {
		this.id = id;
		this.sandy = sandy;
	}

	/** The biome's path under retroeras:beta/. */
	public String id() {
		return id;
	}

	/** Desert uses sand as both top and filler block; everything else is grass over dirt. */
	boolean sandy() {
		return sandy;
	}

	static BetaBiome lookup(double temperature, double humidity) {
		return LOOKUP[(int) (temperature * 63.0) + (int) (humidity * 63.0) * 64];
	}

	private static BetaBiome compute(float temperature, float humidity) {
		humidity *= temperature;
		if (temperature < 0.1F) {
			return TUNDRA;
		}
		if (humidity < 0.2F) {
			if (temperature < 0.5F) {
				return TUNDRA;
			}
			return temperature < 0.95F ? SAVANNA : DESERT;
		}
		if (humidity > 0.5F && temperature < 0.7F) {
			return SWAMPLAND;
		}
		if (temperature < 0.5F) {
			return TAIGA;
		}
		if (temperature < 0.97F) {
			return humidity < 0.35F ? SHRUBLAND : FOREST;
		}
		if (humidity < 0.45F) {
			return PLAINS;
		}
		return humidity < 0.9F ? SEASONAL_FOREST : RAINFOREST;
	}
}
