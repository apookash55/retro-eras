package dev.retroeras.portal;

import java.util.Optional;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rectangular portal inside a frame of {@code frame} blocks, filled with {@code portal} blocks.
 * Mirrors vanilla's PortalShape, which is hard-wired to obsidian and the nether portal.
 */
public final class EraPortalShape {
	private static final int MIN_WIDTH = 2;
	public static final int MAX_WIDTH = 21;
	private static final int MIN_HEIGHT = 3;
	public static final int MAX_HEIGHT = 21;

	private final Block frame;
	private final Block portal;
	private final Direction.Axis axis;
	private final Direction rightDir;
	private final BlockPos bottomLeft;
	private final int width;
	private final int height;
	private final int portalBlocks;

	private EraPortalShape(Block frame, Block portal, Direction.Axis axis, Direction rightDir, BlockPos bottomLeft,
			int width, int height, int portalBlocks) {
		this.frame = frame;
		this.portal = portal;
		this.axis = axis;
		this.rightDir = rightDir;
		this.bottomLeft = bottomLeft;
		this.width = width;
		this.height = height;
		this.portalBlocks = portalBlocks;
	}

	/** Finds an unlit, correctly sized frame around {@code pos}, trying {@code preferredAxis} first. */
	public static Optional<EraPortalShape> findEmpty(BlockGetter level, BlockPos pos, Block frame, Block portal, Direction.Axis preferredAxis) {
		Direction.Axis otherAxis = preferredAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		for (Direction.Axis axis : new Direction.Axis[] {preferredAxis, otherAxis}) {
			EraPortalShape shape = find(level, pos, frame, portal, axis);
			if (shape.isValid() && shape.portalBlocks == 0) {
				return Optional.of(shape);
			}
		}
		return Optional.empty();
	}

	public static EraPortalShape find(BlockGetter level, BlockPos pos, Block frame, Block portal, Direction.Axis axis) {
		Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
		BlockPos bottomLeft = calculateBottomLeft(level, frame, portal, rightDir, pos);
		if (bottomLeft == null) {
			return new EraPortalShape(frame, portal, axis, rightDir, pos, 0, 0, 0);
		}

		int width = getDistanceUntilEdgeAboveFrame(level, frame, portal, bottomLeft, rightDir);
		if (width < MIN_WIDTH || width > MAX_WIDTH) {
			return new EraPortalShape(frame, portal, axis, rightDir, bottomLeft, 0, 0, 0);
		}

		MutableInt portalBlocks = new MutableInt();
		int height = getDistanceUntilTop(level, frame, portal, bottomLeft, rightDir, width, portalBlocks);
		if (height < MIN_HEIGHT || height > MAX_HEIGHT || !hasTopFrame(level, frame, bottomLeft, rightDir, width, height)) {
			height = 0;
		}
		return new EraPortalShape(frame, portal, axis, rightDir, bottomLeft, width, height, portalBlocks.intValue());
	}

	private static @Nullable BlockPos calculateBottomLeft(BlockGetter level, Block frame, Block portal, Direction rightDir, BlockPos pos) {
		int minY = Math.max(level.getMinY(), pos.getY() - MAX_HEIGHT);
		while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()), portal)) {
			pos = pos.below();
		}

		Direction leftDir = rightDir.getOpposite();
		int edge = getDistanceUntilEdgeAboveFrame(level, frame, portal, pos, leftDir) - 1;
		return edge < 0 ? null : pos.relative(leftDir, edge);
	}

	private static int getDistanceUntilEdgeAboveFrame(BlockGetter level, Block frame, Block portal, BlockPos pos, Direction direction) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int distance = 0; distance <= MAX_WIDTH; distance++) {
			cursor.set(pos).move(direction, distance);
			BlockState state = level.getBlockState(cursor);
			if (!isEmpty(state, portal)) {
				return state.is(frame) ? distance : 0;
			}
			if (!level.getBlockState(cursor.move(Direction.DOWN)).is(frame)) {
				return 0;
			}
		}
		return 0;
	}

	private static int getDistanceUntilTop(BlockGetter level, Block frame, Block portal, BlockPos bottomLeft, Direction rightDir,
			int width, MutableInt portalBlocks) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int height = 0; height < MAX_HEIGHT; height++) {
			if (!level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, -1)).is(frame)
					|| !level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, width)).is(frame)) {
				return height;
			}

			for (int i = 0; i < width; i++) {
				BlockState state = level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i));
				if (!isEmpty(state, portal)) {
					return height;
				}
				if (state.is(portal)) {
					portalBlocks.increment();
				}
			}
		}
		return MAX_HEIGHT;
	}

	private static boolean hasTopFrame(BlockGetter level, Block frame, BlockPos bottomLeft, Direction rightDir, int width, int height) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = 0; i < width; i++) {
			if (!level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, i)).is(frame)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isEmpty(BlockState state, Block portal) {
		return state.isAir() || state.is(BlockTags.FIRE) || state.is(portal);
	}

	public boolean isValid() {
		return width >= MIN_WIDTH && width <= MAX_WIDTH && height >= MIN_HEIGHT && height <= MAX_HEIGHT;
	}

	public boolean isComplete() {
		return isValid() && portalBlocks == width * height;
	}

	public void createPortalBlocks(LevelAccessor level) {
		BlockState state = portal.defaultBlockState().setValue(EraPortalBlock.AXIS, axis);
		BlockPos.betweenClosed(bottomLeft, bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1))
				.forEach(pos -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
	}
}
