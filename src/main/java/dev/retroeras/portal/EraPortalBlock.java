package dev.retroeras.portal;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The portal surface for one era. Walking into it moves you between the overworld and that era's dimension, linking to
 * the nearest existing portal of the same era or building a new one at the surface.
 */
public class EraPortalBlock extends Block implements Portal {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));
	private static final int SEARCH_RADIUS = 64;
	private static final int TRANSITION_TICKS = 80;

	private final Era era;

	public EraPortalBlock(Era era, Properties properties) {
		super(properties);
		this.era = era;
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	public Era getEra() {
		return era;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		Direction.Axis updateAxis = directionToNeighbour.getAxis();
		Direction.Axis axis = state.getValue(AXIS);
		boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
		return !wrongAxis && !neighbourState.is(this) && !EraPortalShape.find(level, pos, era.getFrame(), this, axis).isComplete()
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		return entity instanceof Player player && !player.getAbilities().invulnerable ? TRANSITION_TICKS : 1;
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
		boolean leavingEra = currentLevel.dimension() == era.getDimension();
		ServerLevel newLevel = currentLevel.getServer().getLevel(leavingEra ? Level.OVERWORLD : era.getDimension());
		if (newLevel == null) {
			return null;
		}

		BlockPos approximateExit = newLevel.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
		Direction.Axis entryAxis = currentLevel.getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);

		BlockPos exitPos = findExitPortal(newLevel, approximateExit).orElse(null);
		TeleportTransition.PostTeleportTransition post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
		if (exitPos == null) {
			if (entity.isSpectator()) {
				return null;
			}
			exitPos = buildExitPortal(newLevel, approximateExit, entryAxis);
		}

		BlockState exitState = newLevel.getBlockState(exitPos);
		BlockUtil.FoundRectangle exit = BlockUtil.getLargestRectangleAround(exitPos, exitState.getValue(AXIS), EraPortalShape.MAX_WIDTH,
				Direction.Axis.Y, EraPortalShape.MAX_HEIGHT, pos -> newLevel.getBlockState(pos) == exitState);
		return transitionTo(newLevel, exit, exitState.getValue(AXIS), entryAxis, entity, post);
	}

	private Optional<BlockPos> findExitPortal(ServerLevel level, BlockPos near) {
		PoiManager poi = level.getPoiManager();
		poi.ensureLoadedAndValid(level, near, SEARCH_RADIUS);
		return poi.getInSquare(type -> type.is(ModBlocks.portalPoi(era)), near, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
				.map(PoiRecord::getPos)
				.filter(level.getWorldBorder()::isWithinBounds)
				.filter(pos -> level.getBlockState(pos).is(this))
				.min(Comparator.<BlockPos>comparingDouble(pos -> pos.distSqr(near)).thenComparingInt(BlockPos::getY));
	}

	/** Builds a 4x5 frame with a 2x3 portal standing on the surface above {@code near}, and returns a portal block in it. */
	private BlockPos buildExitPortal(ServerLevel level, BlockPos near, Direction.Axis axis) {
		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction forward = right.getClockWise();
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ());
		int y = Math.clamp(surface, level.getMinY() + 2, level.getMaxY() - 6);
		BlockPos origin = new BlockPos(near.getX(), y, near.getZ());

		BlockState frame = era.getFrame().defaultBlockState();
		BlockState portal = defaultBlockState().setValue(AXIS, axis);
		for (int side = -1; side <= 1; side++) {
			for (int across = -1; across <= 2; across++) {
				for (int up = -1; up <= 3; up++) {
					BlockPos pos = origin.relative(right, across).relative(forward, side).relative(Direction.UP, up);
					if (up == -1) {
						// Solid floor under the whole footprint so you never step out over a drop.
						level.setBlockAndUpdate(pos, frame);
					} else if (side == 0) {
						boolean edge = across == -1 || across == 2 || up == 3;
						level.setBlock(pos, edge ? frame : portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					} else {
						level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		return origin;
	}

	private static TeleportTransition transitionTo(ServerLevel level, BlockUtil.FoundRectangle exit, Direction.Axis exitAxis, Direction.Axis entryAxis,
			Entity entity, TeleportTransition.PostTeleportTransition post) {
		EntityDimensions dimensions = entity.getDimensions(entity.getPose());
		boolean xAligned = exitAxis == Direction.Axis.X;
		double right = exit.axis1Size / 2.0;
		Vec3 target = Vec3.atLowerCornerOf(exit.minCorner).add(xAligned ? right : 0.5, 0.0, xAligned ? 0.5 : right);
		Vec3 safe = PortalShape.findCollisionFreePosition(target, level, entity, dimensions);
		float yaw = entity.getYRot() + (exitAxis == entryAxis ? 0 : 90);
		return new TeleportTransition(level, safe, Vec3.ZERO, yaw, entity.getXRot(), Relative.union(Relative.DELTA), post);
	}

	@Override
	public Portal.Transition getLocalTransition() {
		return Portal.Transition.CONFUSION;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(100) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
					0.5F, random.nextFloat() * 0.4F + 0.8F, false);
		}

		for (int i = 0; i < 4; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + random.nextDouble();
			double z = pos.getZ() + random.nextDouble();
			double xa = (random.nextFloat() - 0.5) * 0.5;
			double ya = (random.nextFloat() - 0.5) * 0.5;
			double za = (random.nextFloat() - 0.5) * 0.5;
			int flip = random.nextInt(2) * 2 - 1;
			if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
				x = pos.getX() + 0.5 + 0.25 * flip;
				xa = random.nextFloat() * 2.0F * flip;
			} else {
				z = pos.getZ() + 0.5 + 0.25 * flip;
				za = random.nextFloat() * 2.0F * flip;
			}
			level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return ItemStack.EMPTY;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return switch (rotation) {
			case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
			default -> state;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}
}
