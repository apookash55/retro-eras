package dev.retroeras.worldgen.release.structure;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The world as 1.6.4's structure code sees it: block ids and metadata, read and written through a 26.3 world. */
public final class LegacyWorld {
	private final WorldGenLevel level;
	private final long seed;

	public LegacyWorld(WorldGenLevel level, long seed) {
		this.level = level;
		this.seed = seed;
	}

	public WorldGenLevel level() {
		return level;
	}

	public long seed() {
		return seed;
	}

	private boolean outside(int y) {
		return y < level.getMinY() || y > level.getMaxY();
	}

	private BlockState state(int x, int y, int z) {
		return level.getBlockState(new BlockPos(x, y, z));
	}

	public int getBlockId(int x, int y, int z) {
		return outside(y) ? 0 : LegacyBlocks.id(state(x, y, z));
	}

	public boolean isAir(int x, int y, int z) {
		return outside(y) || state(x, y, z).isAir();
	}

	public boolean isLiquid(int x, int y, int z) {
		return !outside(y) && !state(x, y, z).getFluidState().isEmpty();
	}

	/** Block.opaqueCubeLookup: a full, opaque cube. */
	public boolean isOpaqueCube(int x, int y, int z) {
		if (outside(y)) {
			return false;
		}
		BlockPos pos = new BlockPos(x, y, z);
		return level.getBlockState(pos).isSolidRender();
	}

	public boolean isNormalCube(int x, int y, int z) {
		if (outside(y)) {
			return false;
		}
		BlockPos pos = new BlockPos(x, y, z);
		return level.getBlockState(pos).isRedstoneConductor(level, pos);
	}

	public void setBlock(int x, int y, int z, int id, int meta) {
		if (outside(y)) {
			return;
		}
		BlockPos pos = new BlockPos(x, y, z);
		level.setBlock(pos, LegacyBlocks.state(id, meta), 2);
		if (LegacyBlocks.needsShapeUpdate(id)) {
			level.getChunk(pos).markPosForPostProcessing(pos);
		}
	}

	/** World.getTopSolidOrLiquidBlock: one above the highest block that stops movement or is liquid, ignoring leaves. */
	public int getTopSolidOrLiquidBlock(int x, int z) {
		for (int y = Math.min(level.getMaxY(), 255); y >= level.getMinY(); y--) {
			BlockState state = state(x, y, z);
			if ((state.isSolid() || !state.getFluidState().isEmpty()) && !state.is(BlockTags.LEAVES)) {
				return y + 1;
			}
		}
		return -1;
	}

	public @Nullable BlockEntity getBlockEntity(int x, int y, int z) {
		return outside(y) ? null : level.getBlockEntity(new BlockPos(x, y, z));
	}

	public void addEntity(Entity entity) {
		level.addFreshEntity(entity);
	}
}
