package dev.retroeras.test;

import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaBiome;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.beta.BetaClimate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Beta's and Alpha's decoration on real chunks: ores in the old amounts and heights, trees that keep their leaves, and
 * what Beta 1.7.3 added missing from Alpha.
 */
final class BetaDecorationCheck {
	private static final int RADIUS = 4;

	private int coal;
	private int iron;
	private int gold;
	private int redstone;
	private int diamond;
	private int lapis;
	private int betaPlants;
	private int betaTrees;
	private int logs;
	private int leaves;
	private int doomedLeaves;
	private int highDiamond;
	private int highGold;

	private BetaDecorationCheck() {
	}

	/** Checks the chunks around the nearest forest, the climate's own idea of where one is, and returns its centre. */
	static BlockPos check(ServerLevel level, boolean alpha) {
		BetaDecorationCheck counts = new BetaDecorationCheck();
		String era = alpha ? "Alpha" : "Beta";
		BetaClimate climate = ((BetaChunkGenerator) level.getChunkSource().getGenerator()).terrain(level.getSeed()).climate();
		BlockPos forest = findForest(climate);
		int centreX = forest.getX() >> 4;
		int centreZ = forest.getZ() >> 4;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int cx = centreX - RADIUS; cx < centreX + RADIUS; cx++) {
			for (int cz = centreZ - RADIUS; cz < centreZ + RADIUS; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = 0; y < 128; y++) {
							counts.add(chunk.getBlockState(pos.set(x, y, z)), y);
						}
					}
				}
			}
		}
		RetroEras.LOGGER.info("{} decoration around the forest at {}: {}", era, forest, counts);
		require(counts.logs > 100 && counts.leaves > 300, era + " should have many trees in a forest: " + counts);
		require(counts.coal > 300 && counts.iron > 150, era + " should have plenty of coal and iron: " + counts);
		require(counts.gold > 0 && counts.redstone > 0 && counts.diamond > 0, era + " should have gold, redstone and diamonds: " + counts);
		require(counts.highGold == 0 && counts.highDiamond == 0, era + "'s gold and diamonds should stay low: " + counts);
		require(counts.doomedLeaves * 20 < counts.leaves, era + "'s leaves should not decay: " + counts);
		if (alpha) {
			require(counts.lapis == 0 && counts.betaPlants == 0 && counts.betaTrees == 0, "Alpha had no lapis, tall grass, dead bushes, birches or spruces: " + counts);
		} else {
			require(counts.lapis > 0, "Beta should have lapis: " + counts);
			require(counts.betaPlants > 0 && counts.betaTrees > 0, "Beta's forests should have tall grass and birches: " + counts);
		}
		return forest;
	}

	/** The centre of a block of forest, spiralling out from the origin. */
	private static BlockPos findForest(BetaClimate climate) {
		for (int radius = 0; radius < 256; radius += 4) {
			for (int step = 0; step < Math.max(1, radius * 2); step++) {
				double angle = step * Math.PI / Math.max(1, radius);
				int x = (int) (Math.cos(angle) * radius) * 16;
				int z = (int) (Math.sin(angle) * radius) * 16;
				if (climate.biomeAt(x, z) == BetaBiome.FOREST && climate.biomeAt(x + 48, z) == BetaBiome.FOREST && climate.biomeAt(x - 48, z) == BetaBiome.FOREST
						&& climate.biomeAt(x, z + 48) == BetaBiome.FOREST && climate.biomeAt(x, z - 48) == BetaBiome.FOREST) {
					return new BlockPos(x, 0, z);
				}
			}
		}
		throw new AssertionError("No forest within 4096 blocks of the origin");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private void add(BlockState state, int y) {
		Block block = state.getBlock();
		if (block == Blocks.COAL_ORE) {
			coal++;
		} else if (block == Blocks.IRON_ORE) {
			iron++;
		} else if (block == Blocks.GOLD_ORE) {
			gold++;
			highGold += y > 40 ? 1 : 0;
		} else if (block == Blocks.REDSTONE_ORE) {
			redstone++;
		} else if (block == Blocks.DIAMOND_ORE) {
			diamond++;
			highDiamond += y > 24 ? 1 : 0;
		} else if (block == Blocks.LAPIS_ORE) {
			lapis++;
		} else if (block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.DEAD_BUSH) {
			betaPlants++;
		} else if (block == Blocks.BIRCH_LOG || block == Blocks.SPRUCE_LOG) {
			betaTrees++;
			logs++;
		} else if (block == Blocks.OAK_LOG) {
			logs++;
		} else if (state.is(BlockTags.LEAVES)) {
			leaves++;
			doomedLeaves += state.getValue(LeavesBlock.DISTANCE) == 7 ? 1 : 0;
		}
	}

	@Override
	public String toString() {
		return "coal " + coal + ", iron " + iron + ", gold " + gold + ", redstone " + redstone + ", diamond " + diamond + ", lapis " + lapis
				+ ", plants " + betaPlants + ", logs " + logs + " (" + betaTrees + " birch/spruce), leaves " + leaves + " (" + doomedLeaves + " would decay)";
	}
}
