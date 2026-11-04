package dev.retroeras.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.release.ReleaseTerrain;
import dev.retroeras.worldgen.release.structure.LegacyStart;
import dev.retroeras.worldgen.release.structure.LegacyStructureFeature;
import dev.retroeras.worldgen.release.structure.ReleaseStructures;
import dev.retroeras.worldgen.release.structure.Stronghold;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Lays out 1.6.4's structures for a seed without a world, and checks there are mineshafts, villages, temples and three
 * strongholds with portal rooms, all laid out the same way every time.
 */
public class ReleaseStructuresTest {
	private static final long SEED = 404L;
	private static final int RANGE = 96;

	@GameTest(maxTicks = 2400)
	public void releaseStructuresLayOut(GameTestHelper helper) {
		ReleaseTerrain terrain = new ReleaseTerrain(SEED);
		ReleaseStructures structures = new ReleaseStructures(terrain);
		Map<String, Integer> counts = new HashMap<>();
		Map<String, Integer> pieces = new HashMap<>();
		for (LegacyStructureFeature feature : structures.features()) {
			String name = feature.getClass().getSimpleName();
			for (int chunkX = -RANGE; chunkX <= RANGE; chunkX++) {
				for (int chunkZ = -RANGE; chunkZ <= RANGE; chunkZ++) {
					Optional<LegacyStart> start = feature.startAt(chunkX, chunkZ);
					if (start.isPresent() && start.get().isValid()) {
						counts.merge(name, 1, Integer::sum);
						pieces.merge(name, start.get().pieces().size(), Integer::sum);
						helper.assertTrue(start.get().box().minY >= 1, name + " at chunk " + chunkX + "," + chunkZ + " reaches below the world");
					}
				}
			}
		}
		RetroEras.LOGGER.info("1.6.4 structures within {} chunks of the origin (seed {}): {} (pieces {})", RANGE, SEED, counts, pieces);
		helper.assertTrue(counts.getOrDefault("Mineshaft", 0) >= 3, "Expected mineshafts, got " + counts);
		helper.assertTrue(counts.getOrDefault("Village", 0) >= 1, "Expected a village, got " + counts);
		helper.assertTrue(counts.getOrDefault("Temple", 0) >= 1, "Expected a temple or witch hut, got " + counts);
		helper.assertTrue(counts.getOrDefault("Stronghold", 0) == 3, "Expected 3 strongholds, got " + counts);

		Stronghold stronghold = structures.stronghold();
		BlockPos portal = stronghold.nearest(BlockPos.ZERO);
		helper.assertTrue(portal != null && portal.getY() < 63, "The nearest stronghold's portal room should be underground, got " + portal);
		double distance = Math.sqrt(portal.distSqr(BlockPos.ZERO));
		// 1.6.4 put its first ring of strongholds 640-1152 blocks out, give or take the biome search.
		helper.assertTrue(distance > 400 && distance < 1400, "Stronghold should be 640-1152 blocks out, got " + distance);

		// The same seed lays out the same structures.
		ReleaseStructures again = new ReleaseStructures(new ReleaseTerrain(SEED));
		helper.assertTrue(portal.equals(again.stronghold().nearest(BlockPos.ZERO)), "Stronghold layout is not deterministic");
		for (int i = 0; i < structures.features().size(); i++) {
			LegacyStructureFeature a = structures.features().get(i);
			LegacyStructureFeature b = again.features().get(i);
			for (int chunkX = -RANGE; chunkX <= RANGE; chunkX += 3) {
				for (int chunkZ = -RANGE; chunkZ <= RANGE; chunkZ += 3) {
					Optional<LegacyStart> first = a.startAt(chunkX, chunkZ);
					Optional<LegacyStart> second = b.startAt(chunkX, chunkZ);
					helper.assertTrue(first.isPresent() == second.isPresent()
							&& (first.isEmpty() || first.get().pieces().size() == second.get().pieces().size()
									&& first.get().box().toString().equals(second.get().box().toString())),
							a.getClass().getSimpleName() + " layout at " + chunkX + "," + chunkZ + " is not deterministic");
				}
			}
		}
		helper.succeed();
	}
}
