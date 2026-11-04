package dev.retroeras.worldgen.release;

/**
 * Minecraft 1.6.4's biome placement (GenLayer and its subclasses), ported exactly. Biomes are built by repeatedly
 * zooming a coarse land/ocean map, assigning biomes, adding hills, shores and rivers, and finally jittering cell edges
 * with a Voronoi zoom. Arrays are laid out {@code x + z * width}.
 * <p>
 * Each layer keeps a per-position seed while it runs, just like the original, so the whole stack is used under one lock.
 */
final class ReleaseLayers {
	private static final int OCEAN = ReleaseBiome.OCEAN.id;
	private static final int PLAINS = ReleaseBiome.PLAINS.id;
	private static final int DESERT = ReleaseBiome.DESERT.id;
	private static final int EXTREME_HILLS = ReleaseBiome.EXTREME_HILLS.id;
	private static final int FOREST = ReleaseBiome.FOREST.id;
	private static final int TAIGA = ReleaseBiome.TAIGA.id;
	private static final int SWAMPLAND = ReleaseBiome.SWAMPLAND.id;
	private static final int RIVER = ReleaseBiome.RIVER.id;
	private static final int FROZEN_OCEAN = ReleaseBiome.FROZEN_OCEAN.id;
	private static final int FROZEN_RIVER = ReleaseBiome.FROZEN_RIVER.id;
	private static final int ICE_PLAINS = ReleaseBiome.ICE_PLAINS.id;
	private static final int ICE_MOUNTAINS = ReleaseBiome.ICE_MOUNTAINS.id;
	private static final int MUSHROOM_ISLAND = ReleaseBiome.MUSHROOM_ISLAND.id;
	private static final int MUSHROOM_ISLAND_SHORE = ReleaseBiome.MUSHROOM_ISLAND_SHORE.id;
	private static final int BEACH = ReleaseBiome.BEACH.id;
	private static final int DESERT_HILLS = ReleaseBiome.DESERT_HILLS.id;
	private static final int FOREST_HILLS = ReleaseBiome.FOREST_HILLS.id;
	private static final int TAIGA_HILLS = ReleaseBiome.TAIGA_HILLS.id;
	private static final int EXTREME_HILLS_EDGE = ReleaseBiome.EXTREME_HILLS_EDGE.id;
	private static final int JUNGLE = ReleaseBiome.JUNGLE.id;
	private static final int JUNGLE_HILLS = ReleaseBiome.JUNGLE_HILLS.id;

	/** Quarter-resolution biomes, used for terrain heights (GenLayer chain output 0). */
	private final Layer generation;
	/** Block-resolution biomes, used for surface blocks and chunk biomes (chain output 1). */
	private final Layer blocks;

	ReleaseLayers(long seed) {
		Layer land = new Island(1L);
		land = new FuzzyZoom(2000L, land);
		land = new AddIsland(1L, land);
		land = new Zoom(2001L, land);
		land = new AddIsland(2L, land);
		land = new AddSnow(2L, land);
		land = new Zoom(2002L, land);
		land = new AddIsland(3L, land);
		land = new Zoom(2003L, land);
		land = new AddIsland(4L, land);
		land = new AddMushroomIsland(5L, land);
		int biomeSize = 4;

		Layer rivers = Zoom.magnify(1000L, land, 0);
		rivers = new RiverInit(100L, rivers);
		rivers = Zoom.magnify(1000L, rivers, biomeSize + 2);
		rivers = new River(1L, rivers);
		rivers = new Smooth(1000L, rivers);

		Layer biomes = Zoom.magnify(1000L, land, 0);
		biomes = new Biome(200L, biomes);
		biomes = Zoom.magnify(1000L, biomes, 2);
		biomes = new Hills(1000L, biomes);
		for (int i = 0; i < biomeSize; i++) {
			biomes = new Zoom(1000L + i, biomes);
			if (i == 0) {
				biomes = new AddIsland(3L, biomes);
			}
			if (i == 1) {
				biomes = new Shore(1000L, biomes);
				biomes = new SwampRivers(1000L, biomes);
			}
		}
		biomes = new Smooth(1000L, biomes);

		RiverMix mixed = new RiverMix(100L, biomes, rivers);
		VoronoiZoom voronoi = new VoronoiZoom(10L, mixed);
		mixed.initWorldGenSeed(seed);
		voronoi.initWorldGenSeed(seed);
		generation = mixed;
		blocks = voronoi;
	}

	/** Biome ids at quarter resolution (4 blocks per cell). */
	synchronized int[] generationBiomes(int x, int z, int width, int height) {
		return generation.get(x, z, width, height);
	}

	/** Biome ids per block column. */
	synchronized int[] blockBiomes(int x, int z, int width, int height) {
		return blocks.get(x, z, width, height);
	}

	/** GenLayer. */
	private abstract static class Layer {
		private static final long MULTIPLIER = 6364136223846793005L;
		private static final long INCREMENT = 1442695040888963407L;

		protected Layer parent;
		private final long baseSeed;
		private long worldGenSeed;
		private long chunkSeed;

		Layer(long seed) {
			long base = seed;
			base = base * (base * MULTIPLIER + INCREMENT) + seed;
			base = base * (base * MULTIPLIER + INCREMENT) + seed;
			base = base * (base * MULTIPLIER + INCREMENT) + seed;
			baseSeed = base;
		}

		void initWorldGenSeed(long seed) {
			worldGenSeed = seed;
			if (parent != null) {
				parent.initWorldGenSeed(seed);
			}
			worldGenSeed = worldGenSeed * (worldGenSeed * MULTIPLIER + INCREMENT) + baseSeed;
			worldGenSeed = worldGenSeed * (worldGenSeed * MULTIPLIER + INCREMENT) + baseSeed;
			worldGenSeed = worldGenSeed * (worldGenSeed * MULTIPLIER + INCREMENT) + baseSeed;
		}

		void initChunkSeed(long x, long z) {
			chunkSeed = worldGenSeed;
			chunkSeed = chunkSeed * (chunkSeed * MULTIPLIER + INCREMENT) + x;
			chunkSeed = chunkSeed * (chunkSeed * MULTIPLIER + INCREMENT) + z;
			chunkSeed = chunkSeed * (chunkSeed * MULTIPLIER + INCREMENT) + x;
			chunkSeed = chunkSeed * (chunkSeed * MULTIPLIER + INCREMENT) + z;
		}

		int nextInt(int bound) {
			int value = (int) ((chunkSeed >> 24) % bound);
			if (value < 0) {
				value += bound;
			}
			chunkSeed = chunkSeed * (chunkSeed * MULTIPLIER + INCREMENT) + worldGenSeed;
			return value;
		}

		abstract int[] get(int x, int z, int width, int height);
	}

	/** GenLayerIsland: 1 in 10 cells start as land, and the origin always does. */
	private static final class Island extends Layer {
		Island(long seed) {
			super(seed);
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(x + dx, z + dz);
					out[dx + dz * width] = nextInt(10) == 0 ? 1 : 0;
				}
			}
			if (x > -width && x <= 0 && z > -height && z <= 0) {
				out[-x + -z * width] = 1;
			}
			return out;
		}
	}

	/** GenLayerZoom: doubles resolution, picking corners by majority (or at random). */
	private static class Zoom extends Layer {
		Zoom(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		static Layer magnify(long seed, Layer layer, int times) {
			Layer result = layer;
			for (int i = 0; i < times; i++) {
				result = new Zoom(seed + i, result);
			}
			return result;
		}

		@Override
		final int[] get(int x, int z, int width, int height) {
			int parentX = x >> 1;
			int parentZ = z >> 1;
			int parentWidth = (width >> 1) + 3;
			int parentHeight = (height >> 1) + 3;
			int[] in = parent.get(parentX, parentZ, parentWidth, parentHeight);
			int[] zoomed = new int[parentWidth * 2 * parentHeight * 2];
			int zoomedWidth = parentWidth << 1;

			for (int pz = 0; pz < parentHeight - 1; pz++) {
				int index = (pz << 1) * zoomedWidth;
				int topLeft = in[(pz) * parentWidth];
				int bottomLeft = in[(pz + 1) * parentWidth];
				for (int px = 0; px < parentWidth - 1; px++) {
					initChunkSeed(px + parentX << 1, pz + parentZ << 1);
					int topRight = in[px + 1 + (pz) * parentWidth];
					int bottomRight = in[px + 1 + (pz + 1) * parentWidth];
					zoomed[index] = topLeft;
					zoomed[index++ + zoomedWidth] = choose(topLeft, bottomLeft);
					zoomed[index] = choose(topLeft, topRight);
					zoomed[index++ + zoomedWidth] = chooseCorner(topLeft, topRight, bottomLeft, bottomRight);
					topLeft = topRight;
					bottomLeft = bottomRight;
				}
			}

			int[] out = new int[width * height];
			for (int row = 0; row < height; row++) {
				System.arraycopy(zoomed, (row + (z & 1)) * (parentWidth << 1) + (x & 1), out, row * width, width);
			}
			return out;
		}

		private int choose(int a, int b) {
			return nextInt(2) == 0 ? a : b;
		}

		int chooseCorner(int a, int b, int c, int d) {
			if (b == c && c == d) {
				return b;
			} else if (a == b && a == c) {
				return a;
			} else if (a == b && a == d) {
				return a;
			} else if (a == c && a == d) {
				return a;
			} else if (a == b && c != d) {
				return a;
			} else if (a == c && b != d) {
				return a;
			} else if (a == d && b != c) {
				return a;
			} else if (b == a && c != d) {
				return b;
			} else if (b == c && a != d) {
				return b;
			} else if (b == d && a != c) {
				return b;
			} else if (c == a && b != d) {
				return c;
			} else if (c == b && a != d) {
				return c;
			} else if (c == d && a != b) {
				return c;
			} else if (d == a && b != c) {
				return c;
			} else if (d == b && a != c) {
				return c;
			} else if (d == c && a != b) {
				return c;
			}
			return pickAny(a, b, c, d);
		}

		int pickAny(int a, int b, int c, int d) {
			int pick = nextInt(4);
			return pick == 0 ? a : pick == 1 ? b : pick == 2 ? c : d;
		}
	}

	/** GenLayerFuzzyZoom: a zoom whose corners are always picked at random. */
	private static final class FuzzyZoom extends Zoom {
		FuzzyZoom(long seed, Layer parent) {
			super(seed, parent);
		}

		@Override
		int chooseCorner(int a, int b, int c, int d) {
			return pickAny(a, b, c, d);
		}
	}

	/** GenLayerAddIsland: grows and erodes coastlines. */
	private static final class AddIsland extends Layer {
		AddIsland(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					int nw = in[dx + (dz) * w];
					int ne = in[dx + 2 + (dz) * w];
					int sw = in[dx + (dz + 2) * w];
					int se = in[dx + 2 + (dz + 2) * w];
					int centre = in[dx + 1 + (dz + 1) * w];
					initChunkSeed(dx + x, dz + z);
					int value;
					if (centre != 0 || nw == 0 && ne == 0 && sw == 0 && se == 0) {
						if (centre > 0 && (nw == 0 || ne == 0 || sw == 0 || se == 0)) {
							if (nextInt(5) == 0) {
								value = centre == ICE_PLAINS ? FROZEN_OCEAN : 0;
							} else {
								value = centre;
							}
						} else {
							value = centre;
						}
					} else {
						int bound = 1;
						int chosen = 1;
						if (nw != 0 && nextInt(bound++) == 0) {
							chosen = nw;
						}
						if (ne != 0 && nextInt(bound++) == 0) {
							chosen = ne;
						}
						if (sw != 0 && nextInt(bound++) == 0) {
							chosen = sw;
						}
						if (se != 0 && nextInt(bound++) == 0) {
							chosen = se;
						}
						if (nextInt(3) == 0) {
							value = chosen;
						} else {
							value = chosen == ICE_PLAINS ? FROZEN_OCEAN : 0;
						}
					}
					out[dx + dz * width] = value;
				}
			}
			return out;
		}
	}

	/** GenLayerAddSnow: 1 in 5 land cells become snowy (marked as ice plains). */
	private static final class AddSnow extends Layer {
		AddSnow(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					int centre = in[dx + 1 + (dz + 1) * w];
					initChunkSeed(dx + x, dz + z);
					if (centre == 0) {
						out[dx + dz * width] = 0;
					} else {
						out[dx + dz * width] = nextInt(5) == 0 ? ICE_PLAINS : 1;
					}
				}
			}
			return out;
		}
	}

	/** GenLayerAddMushroomIsland: rare mushroom islands in open ocean. */
	private static final class AddMushroomIsland extends Layer {
		AddMushroomIsland(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					int nw = in[dx + (dz) * w];
					int ne = in[dx + 2 + (dz) * w];
					int sw = in[dx + (dz + 2) * w];
					int se = in[dx + 2 + (dz + 2) * w];
					int centre = in[dx + 1 + (dz + 1) * w];
					initChunkSeed(dx + x, dz + z);
					if (centre == 0 && nw == 0 && ne == 0 && sw == 0 && se == 0 && nextInt(100) == 0) {
						out[dx + dz * width] = MUSHROOM_ISLAND;
					} else {
						out[dx + dz * width] = centre;
					}
				}
			}
			return out;
		}
	}

	/** GenLayerRiverInit: random 2 or 3 on land, which the river layer turns into edges. */
	private static final class RiverInit extends Layer {
		RiverInit(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int[] in = parent.get(x, z, width, height);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(dx + x, dz + z);
					out[dx + dz * width] = in[dx + dz * width] > 0 ? nextInt(2) + 2 : 0;
				}
			}
			return out;
		}
	}

	/** GenLayerRiver: a river wherever neighbouring cells differ, -1 elsewhere. */
	private static final class River extends Layer {
		River(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					int west = in[dx + (dz + 1) * w];
					int east = in[dx + 2 + (dz + 1) * w];
					int north = in[dx + 1 + (dz) * w];
					int south = in[dx + 1 + (dz + 2) * w];
					int centre = in[dx + 1 + (dz + 1) * w];
					if (centre != 0 && west != 0 && east != 0 && north != 0 && south != 0
							&& centre == west && centre == north && centre == east && centre == south) {
						out[dx + dz * width] = -1;
					} else {
						out[dx + dz * width] = RIVER;
					}
				}
			}
			return out;
		}
	}

	/** GenLayerSmooth: removes single-cell bumps along straight edges. */
	private static final class Smooth extends Layer {
		Smooth(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					int west = in[dx + (dz + 1) * w];
					int east = in[dx + 2 + (dz + 1) * w];
					int north = in[dx + 1 + (dz) * w];
					int south = in[dx + 1 + (dz + 2) * w];
					int centre = in[dx + 1 + (dz + 1) * w];
					if (west == east && north == south) {
						initChunkSeed(dx + x, dz + z);
						centre = nextInt(2) == 0 ? west : north;
					} else {
						if (west == east) {
							centre = west;
						}
						if (north == south) {
							centre = north;
						}
					}
					out[dx + dz * width] = centre;
				}
			}
			return out;
		}
	}

	/** GenLayerBiome: turns land into the six (seven with jungle) base biomes; snowy land stays taiga or ice plains. */
	private static final class Biome extends Layer {
		private static final int[] BIOMES = {DESERT, FOREST, EXTREME_HILLS, SWAMPLAND, PLAINS, TAIGA, JUNGLE};

		Biome(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int[] in = parent.get(x, z, width, height);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(dx + x, dz + z);
					int value = in[dx + dz * width];
					if (value == 0) {
						out[dx + dz * width] = 0;
					} else if (value == MUSHROOM_ISLAND) {
						out[dx + dz * width] = value;
					} else if (value == 1) {
						out[dx + dz * width] = BIOMES[nextInt(BIOMES.length)];
					} else {
						int biome = BIOMES[nextInt(BIOMES.length)];
						out[dx + dz * width] = biome == TAIGA ? biome : ICE_PLAINS;
					}
				}
			}
			return out;
		}
	}

	/** GenLayerHills: 1 in 3 cells surrounded by the same biome become its hills variant. */
	private static final class Hills extends Layer {
		Hills(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(dx + x, dz + z);
					int centre = in[dx + 1 + (dz + 1) * w];
					if (nextInt(3) == 0) {
						int hills = centre;
						if (centre == DESERT) {
							hills = DESERT_HILLS;
						} else if (centre == FOREST) {
							hills = FOREST_HILLS;
						} else if (centre == TAIGA) {
							hills = TAIGA_HILLS;
						} else if (centre == PLAINS) {
							hills = FOREST;
						} else if (centre == ICE_PLAINS) {
							hills = ICE_MOUNTAINS;
						} else if (centre == JUNGLE) {
							hills = JUNGLE_HILLS;
						}
						if (hills == centre) {
							out[dx + dz * width] = centre;
						} else {
							int north = in[dx + 1 + (dz) * w];
							int east = in[dx + 2 + (dz + 1) * w];
							int west = in[dx + (dz + 1) * w];
							int south = in[dx + 1 + (dz + 2) * w];
							out[dx + dz * width] = north == centre && east == centre && west == centre && south == centre ? hills : centre;
						}
					} else {
						out[dx + dz * width] = centre;
					}
				}
			}
			return out;
		}
	}

	/** GenLayerShore: beaches, mushroom shores and extreme hills edges. */
	private static final class Shore extends Layer {
		Shore(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(dx + x, dz + z);
					int centre = in[dx + 1 + (dz + 1) * w];
					int north = in[dx + 1 + (dz) * w];
					int east = in[dx + 2 + (dz + 1) * w];
					int west = in[dx + (dz + 1) * w];
					int south = in[dx + 1 + (dz + 2) * w];
					int value;
					if (centre == MUSHROOM_ISLAND) {
						value = north != OCEAN && east != OCEAN && west != OCEAN && south != OCEAN ? centre : MUSHROOM_ISLAND_SHORE;
					} else if (centre != OCEAN && centre != RIVER && centre != SWAMPLAND && centre != EXTREME_HILLS) {
						value = north != OCEAN && east != OCEAN && west != OCEAN && south != OCEAN ? centre : BEACH;
					} else if (centre == EXTREME_HILLS) {
						value = north == EXTREME_HILLS && east == EXTREME_HILLS && west == EXTREME_HILLS && south == EXTREME_HILLS ? centre : EXTREME_HILLS_EDGE;
					} else {
						value = centre;
					}
					out[dx + dz * width] = value;
				}
			}
			return out;
		}
	}

	/** GenLayerSwampRivers: a few river cells inside swamps and jungles. */
	private static final class SwampRivers extends Layer {
		SwampRivers(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int w = width + 2;
			int[] in = parent.get(x - 1, z - 1, w, height + 2);
			int[] out = new int[width * height];
			for (int dz = 0; dz < height; dz++) {
				for (int dx = 0; dx < width; dx++) {
					initChunkSeed(dx + x, dz + z);
					int centre = in[dx + 1 + (dz + 1) * w];
					boolean keep = (centre != SWAMPLAND || nextInt(6) != 0) && (centre != JUNGLE && centre != JUNGLE_HILLS || nextInt(8) != 0);
					out[dx + dz * width] = keep ? centre : RIVER;
				}
			}
			return out;
		}
	}

	/** GenLayerRiverMix: lays the river branch over the biome branch. */
	private static final class RiverMix extends Layer {
		private final Layer biomes;
		private final Layer rivers;

		RiverMix(long seed, Layer biomes, Layer rivers) {
			super(seed);
			this.biomes = biomes;
			this.rivers = rivers;
		}

		@Override
		void initWorldGenSeed(long seed) {
			biomes.initWorldGenSeed(seed);
			rivers.initWorldGenSeed(seed);
			super.initWorldGenSeed(seed);
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			int[] biome = biomes.get(x, z, width, height);
			int[] river = rivers.get(x, z, width, height);
			int[] out = new int[width * height];
			for (int i = 0; i < width * height; i++) {
				if (biome[i] == OCEAN) {
					out[i] = biome[i];
				} else if (river[i] >= 0) {
					if (biome[i] == ICE_PLAINS) {
						out[i] = FROZEN_RIVER;
					} else if (biome[i] != MUSHROOM_ISLAND && biome[i] != MUSHROOM_ISLAND_SHORE) {
						out[i] = river[i];
					} else {
						out[i] = MUSHROOM_ISLAND_SHORE;
					}
				} else {
					out[i] = biome[i];
				}
			}
			return out;
		}
	}

	/** GenLayerVoronoiZoom: 4x zoom that jitters each cell's centre, giving biome edges their wobble. */
	private static final class VoronoiZoom extends Layer {
		VoronoiZoom(long seed, Layer parent) {
			super(seed);
			this.parent = parent;
		}

		@Override
		int[] get(int x, int z, int width, int height) {
			x -= 2;
			z -= 2;
			int shift = 2;
			int cell = 1 << shift;
			int parentX = x >> shift;
			int parentZ = z >> shift;
			int parentWidth = (width >> shift) + 3;
			int parentHeight = (height >> shift) + 3;
			int[] in = parent.get(parentX, parentZ, parentWidth, parentHeight);
			int zoomedWidth = parentWidth << shift;
			int zoomedHeight = parentHeight << shift;
			int[] zoomed = new int[zoomedWidth * zoomedHeight];

			for (int pz = 0; pz < parentHeight - 1; pz++) {
				int topLeft = in[(pz) * parentWidth];
				int bottomLeft = in[(pz + 1) * parentWidth];
				for (int px = 0; px < parentWidth - 1; px++) {
					double jitter = cell * 0.9;
					initChunkSeed(px + parentX << shift, pz + parentZ << shift);
					double x00 = (nextInt(1024) / 1024.0 - 0.5) * jitter;
					double z00 = (nextInt(1024) / 1024.0 - 0.5) * jitter;
					initChunkSeed(px + parentX + 1 << shift, pz + parentZ << shift);
					double x10 = (nextInt(1024) / 1024.0 - 0.5) * jitter + cell;
					double z10 = (nextInt(1024) / 1024.0 - 0.5) * jitter;
					initChunkSeed(px + parentX << shift, pz + parentZ + 1 << shift);
					double x01 = (nextInt(1024) / 1024.0 - 0.5) * jitter;
					double z01 = (nextInt(1024) / 1024.0 - 0.5) * jitter + cell;
					initChunkSeed(px + parentX + 1 << shift, pz + parentZ + 1 << shift);
					double x11 = (nextInt(1024) / 1024.0 - 0.5) * jitter + cell;
					double z11 = (nextInt(1024) / 1024.0 - 0.5) * jitter + cell;
					int topRight = in[px + 1 + (pz) * parentWidth];
					int bottomRight = in[px + 1 + (pz + 1) * parentWidth];

					for (int dz = 0; dz < cell; dz++) {
						int index = ((pz << shift) + dz) * zoomedWidth + (px << shift);
						for (int dx = 0; dx < cell; dx++) {
							double d00 = (dz - z00) * (dz - z00) + (dx - x00) * (dx - x00);
							double d10 = (dz - z10) * (dz - z10) + (dx - x10) * (dx - x10);
							double d01 = (dz - z01) * (dz - z01) + (dx - x01) * (dx - x01);
							double d11 = (dz - z11) * (dz - z11) + (dx - x11) * (dx - x11);
							if (d00 < d10 && d00 < d01 && d00 < d11) {
								zoomed[index++] = topLeft;
							} else if (d10 < d00 && d10 < d01 && d10 < d11) {
								zoomed[index++] = topRight;
							} else if (d01 < d00 && d01 < d10 && d01 < d11) {
								zoomed[index++] = bottomLeft;
							} else {
								zoomed[index++] = bottomRight;
							}
						}
					}
					topLeft = topRight;
					bottomLeft = bottomRight;
				}
			}

			int[] out = new int[width * height];
			for (int row = 0; row < height; row++) {
				System.arraycopy(zoomed, (row + (z & cell - 1)) * zoomedWidth + (x & cell - 1), out, row * width, width);
			}
			return out;
		}
	}
}
