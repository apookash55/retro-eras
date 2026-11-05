package dev.retroeras.test;

import java.util.Optional;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.nether.NetherChunkGenerator;
import dev.retroeras.worldgen.nether.NetherTerrain;
import dev.retroeras.worldgen.release.structure.Fortress;
import dev.retroeras.worldgen.release.structure.LegacyStart;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * The eras' Nethers: Beta's (also Alpha's) and 1.6.4's ChunkProviderHell, closed in by bedrock with a lava sea below
 * y = 32 and soul sand and gravel shelves, and 1.6.4's nether fortresses.
 */
public class NetherTerrainTest {
	private static final long SEED = 404L;
	private static final byte BEDROCK = 7;
	private static final byte LAVA = 11;
	private static final byte GRAVEL = 13;
	private static final byte NETHERRACK = 87;
	private static final byte SOUL_SAND = 88;

	@GameTest(maxTicks = 2400)
	public void eraNethers(GameTestHelper helper) {
		NetherChunkGenerator beta = generator(helper, Era.BETA);
		NetherChunkGenerator alpha = generator(helper, Era.ALPHA);
		NetherChunkGenerator release = generator(helper, Era.OLD_RELEASE);
		helper.assertTrue(beta.version() == NetherTerrain.Version.BETA && alpha.version() == NetherTerrain.Version.BETA,
				"Alpha and Beta should share Beta's Nether");
		helper.assertTrue(release.version() == NetherTerrain.Version.RELEASE, "Old Release should use 1.6.4's Nether");
		checkTerrain(helper, "Beta", beta.terrain(SEED));
		checkTerrain(helper, "1.6.4", release.terrain(SEED));
		helper.assertTrue(beta.fortress(SEED) == null, "Beta's Nether had no fortresses");

		Fortress fortress = release.fortress(SEED);
		helper.assertTrue(fortress != null, "1.6.4's Nether should have fortresses");
		int found = 0;
		int pieces = 0;
		for (int chunkX = -64; chunkX < 64; chunkX++) {
			for (int chunkZ = -64; chunkZ < 64; chunkZ++) {
				Optional<LegacyStart> start = fortress.startAt(chunkX, chunkZ);
				if (start.isPresent()) {
					found++;
					pieces += start.get().pieces().size();
					helper.assertTrue(start.get().box().minY >= 48 && start.get().box().minY <= 70,
							"A fortress should be raised to between y = 48 and 70, this one starts at " + start.get().box().minY);
				}
			}
		}
		RetroEras.LOGGER.info("1.6.4 Nether (seed {}): {} fortresses in 128x128 chunks, {} pieces on average", SEED, found, found == 0 ? 0 : pieces / found);
		// One region in three of 16x16 chunks holds a fortress, so about 21 here.
		helper.assertTrue(found >= 10 && found <= 35, "Expected about 21 fortresses in 64 regions, found " + found);
		helper.assertTrue(pieces / found >= 20, "Fortresses should be long, averaging only " + pieces / found + " pieces");
		helper.succeed();
	}

	private static NetherChunkGenerator generator(GameTestHelper helper, Era era) {
		Identifier file = era.getNether().identifier().withPath(path -> "dimension/" + path + ".json");
		LevelStem stem = TestDimensions.load(helper, file);
		helper.assertTrue(stem.type().value().height() == 128, era.getName() + "'s Nether should be 128 high");
		helper.assertTrue(stem.generator() instanceof NetherChunkGenerator, era.getName() + "'s Nether should use the old Nether generator");
		return (NetherChunkGenerator) stem.generator();
	}

	private static void checkTerrain(GameTestHelper helper, String name, NetherTerrain terrain) {
		int netherrack = 0;
		int lava = 0;
		int soulSand = 0;
		int gravel = 0;
		int total = 0;
		for (int chunkX = -8; chunkX <= 8; chunkX += 4) {
			for (int chunkZ = -8; chunkZ <= 8; chunkZ += 4) {
				NetherTerrain.Chunk chunk = terrain.generate(chunkX, chunkZ, true);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						helper.assertTrue(chunk.get(x, 0, z) == BEDROCK && chunk.get(x, 127, z) == BEDROCK,
								name + " Nether should have bedrock at y = 0 and 127");
						for (int y = 0; y < NetherTerrain.HEIGHT; y++) {
							byte block = chunk.get(x, y, z);
							total++;
							if (block == NETHERRACK) {
								netherrack++;
							} else if (block == LAVA && y < NetherTerrain.LAVA_LEVEL) {
								lava++;
							} else if (block == SOUL_SAND) {
								soulSand++;
							} else if (block == GRAVEL) {
								gravel++;
							}
						}
					}
				}
			}
		}
		RetroEras.LOGGER.info("{} Nether (seed {}): {}% netherrack, {} lava, {} soul sand, {} gravel", name, SEED, netherrack * 100 / total, lava,
				soulSand, gravel);
		helper.assertTrue(netherrack > total / 4 && netherrack < total * 9 / 10, name + " Nether should be mostly but not all netherrack");
		helper.assertTrue(lava > 0, name + " Nether should have a lava sea below y = 32");
		helper.assertTrue(soulSand > 0 && gravel > 0, name + " Nether should have soul sand and gravel shelves");
	}
}
