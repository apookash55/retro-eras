package dev.retroeras.worldgen.beta;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The world as Beta's decoration code saw it. Beta could reach any chunk; 26.3 only gives a chunk's decoration its direct
 * neighbours, so the rare block beyond them (the tip of a big tree's branch) reads as air and is not placed.
 */
final class BetaWorld {
	/** The world was 128 high. */
	private static final int HEIGHT = BetaTerrain.HEIGHT;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final WorldGenLevel level;
	private final int centerX;
	private final int centerZ;

	BetaWorld(WorldGenLevel level, ChunkPos center) {
		this.level = level;
		this.centerX = center.x();
		this.centerZ = center.z();
	}

	WorldGenLevel level() {
		return level;
	}

	private boolean reachable(int x, int z) {
		return Math.abs((x >> 4) - centerX) <= 1 && Math.abs((z >> 4) - centerZ) <= 1;
	}

	BlockState get(int x, int y, int z) {
		return y < 0 || y >= HEIGHT || !reachable(x, z) ? AIR : level.getBlockState(new BlockPos(x, y, z));
	}

	boolean isAir(int x, int y, int z) {
		return get(x, y, z).isAir();
	}

	void set(int x, int y, int z, BlockState state) {
		if (y >= 0 && y < HEIGHT && reachable(x, z)) {
			level.setBlock(new BlockPos(x, y, z), state, 2);
		}
	}

	/** Material.isSolid: everything except air, liquids and plants. Leaves count. */
	boolean isSolid(int x, int y, int z) {
		BlockState state = get(x, y, z);
		return state.isSolid() || state.is(BlockTags.LEAVES);
	}

	boolean isWater(int x, int y, int z) {
		return get(x, y, z).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
	}

	/** Block.opaqueCubeLookup. */
	boolean isOpaque(int x, int y, int z) {
		return get(x, y, z).isSolidRender();
	}

	/** World.getHeightValue: one above the highest block that stops any light. */
	int height(int x, int z) {
		return reachable(x, z) ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) : 0;
	}

	/**
	 * Whether a plant here gets daylight. Decoration in 26.3 runs before lighting, so this stands in for Beta's light checks
	 * of 8 or more for flowers and under 13 for mushrooms: nothing opaque overhead, though leaves are allowed.
	 */
	boolean isOutdoors(int x, int y, int z) {
		int top = height(x, z);
		for (int above = y + 1; above < top; above++) {
			if (isOpaque(x, above, z)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gives a finished tree's leaves their distance from its logs. Beta's leaves worked this out when they were ticked, but
	 * 26.3's store it, and would all fall off unless it is set now.
	 */
	void settleLeaves(List<BlockPos> logs) {
		Map<BlockPos, Integer> distances = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos log : logs) {
			distances.put(log, 0);
			queue.add(log);
		}
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			int distance = distances.get(pos) + 1;
			if (distance > 6) {
				continue;
			}
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (!distances.containsKey(next)) {
					BlockState state = get(next.getX(), next.getY(), next.getZ());
					if (state.getBlock() instanceof LeavesBlock) {
						distances.put(next, distance);
						queue.add(next);
						if (state.getValue(LeavesBlock.DISTANCE) > distance) {
							level.setBlock(next, state.setValue(LeavesBlock.DISTANCE, distance), 2);
						}
					}
				}
			}
		}
	}
}
