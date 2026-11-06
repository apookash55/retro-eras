package dev.retroeras.player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Stacks the eras on top of each other, newest at the top: the overworld, then Old Release, Beta, Pre-Alpha and Cave
 * game at the bottom (Alpha sits between Beta and Pre-Alpha). Falling out of the bottom of one drops you into the sky of the next older one; climbing above the
 * top of an older one brings you up just over the bedrock of the next newer one. To fall through, the very lowest
 * bedrock layer can be mined, slowly, with a diamond pickaxe.
 */
public final class EraLayers {
	/** Newest first. */
	private static final List<ResourceKey<Level>> STACK = List.of(Level.OVERWORLD, Era.OLD_RELEASE.getDimension(), Era.BETA.getDimension(), Era.ALPHA.getDimension(),
			Era.PRE_ALPHA.getDimension(), Era.CLASSIC.getDimension(), Era.CAVEGAME.getDimension());
	/** How far below the world a player falls before going down a layer. */
	private static final int FALL_MARGIN = 16;
	/** How far above the build limit a player must get before going up a layer: a jump from the top block is enough. */
	private static final double CLIMB_MARGIN = 2.0;
	/** Bedrock at the bottom takes as long as obsidian. */
	private static final float BEDROCK_HARDNESS = 50.0F;

	/** Players who came down from above and have not landed yet; their landing does not hurt. */
	private static final Set<UUID> FALLING_IN = ConcurrentHashMap.newKeySet();

	private EraLayers() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
				tick(server, player);
			}
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof Player player && FALLING_IN.contains(player.getUUID())) {
				if (source.is(DamageTypes.FALL)) {
					FALLING_IN.remove(player.getUUID());
					return false;
				}
				// Falling through the world on the way down is not a reason to take void damage either.
				return !source.is(DamageTypes.FELL_OUT_OF_WORLD);
			}
			return true;
		});
	}

	public static @Nullable ResourceKey<Level> below(ResourceKey<Level> dimension) {
		int index = STACK.indexOf(dimension);
		return index >= 0 && index + 1 < STACK.size() ? STACK.get(index + 1) : null;
	}

	public static @Nullable ResourceKey<Level> above(ResourceKey<Level> dimension) {
		int index = STACK.indexOf(dimension);
		return index > 0 ? STACK.get(index - 1) : null;
	}

	/**
	 * How fast {@code player} breaks {@code state}, if it is the lowest bedrock layer of a world with another era below;
	 * null otherwise. A diamond pickaxe (or better) breaks it as fast as it breaks obsidian.
	 */
	public static @Nullable Float bedrockProgress(BlockState state, Player player, BlockPos pos) {
		Level level = player.level();
		if (!state.is(Blocks.BEDROCK) || pos.getY() != level.getMinY() || below(level.dimension()) == null) {
			return null;
		}
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		if (!player.hasCorrectToolForDrops(obsidian)) {
			return 0.0F;
		}
		return player.getDestroySpeed(obsidian) / BEDROCK_HARDNESS / 30.0F;
	}

	private static void tick(MinecraftServer server, ServerPlayer player) {
		ServerLevel level = player.level();
		// The on-ground flag can still be left over from the level above for a moment after arriving, so look for real ground.
		boolean landed = player.onGround() && !level.noCollision(player, player.getBoundingBox().move(0.0, -0.1, 0.0));
		if (FALLING_IN.contains(player.getUUID()) && (landed || player.isInWater() || player.getAbilities().flying)) {
			FALLING_IN.remove(player.getUUID());
		}
		if (player.getY() < level.getMinY() - FALL_MARGIN) {
			ResourceKey<Level> down = below(level.dimension());
			ServerLevel target = down == null ? null : server.getLevel(down);
			if (target != null) {
				goDown(player, target);
			}
		} else if (player.getY() > level.getMaxY() + 1 + CLIMB_MARGIN) {
			ResourceKey<Level> up = above(level.dimension());
			ServerLevel target = up == null ? null : server.getLevel(up);
			if (target != null) {
				goUp(player, target);
			}
		}
	}

	/** Drops the player into the sky of the older era, over the same spot (inside its map, for a finite era). */
	private static void goDown(ServerPlayer player, ServerLevel target) {
		BlockPos column = clamp(target, player.blockPosition());
		target.getChunk(column.getX() >> 4, column.getZ() >> 4);
		// High enough to see the old world coming, low enough not to fall for half a minute.
		int surface = target.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
		Vec3 arrival = new Vec3(column.getX() + 0.5, Math.min(target.getMaxY() + 1, surface + 48), column.getZ() + 0.5);
		FALLING_IN.add(player.getUUID());
		player.resetFallDistance();
		teleport(player, target, arrival);
		RetroEras.LOGGER.debug("{} fell from {} into {}", player.getName().getString(), player.level().dimension(), target.dimension());
	}

	/** Brings the player up into the newer era just above its bedrock, digging a small pocket if there is no room. */
	private static void goUp(ServerPlayer player, ServerLevel target) {
		BlockPos column = clamp(target, player.blockPosition());
		target.getChunk(column.getX() >> 4, column.getZ() >> 4);
		BlockPos arrival = pocketAboveBedrock(target, column.getX(), column.getZ());
		teleport(player, target, Vec3.atBottomCenterOf(arrival));
	}

	/** The first two-high air gap above the bedrock floor; if the ground is solid there, one is dug. */
	static BlockPos pocketAboveBedrock(ServerLevel level, int x, int z) {
		int y = level.getMinY();
		while (y < level.getMaxY() && level.getBlockState(new BlockPos(x, y, z)).is(Blocks.BEDROCK)) {
			y++;
		}
		BlockPos floor = new BlockPos(x, y, z);
		for (int search = 0; search < 32 && floor.getY() + search + 2 <= level.getMaxY(); search++) {
			BlockPos feet = floor.above(search);
			if (isSafeGap(level, feet)) {
				return feet;
			}
		}
		BlockPos feet = floor.above();
		level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
		level.setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
		return feet;
	}

	private static boolean isSafeGap(ServerLevel level, BlockPos feet) {
		BlockState below = level.getBlockState(feet.below());
		return below.isSolid() && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
	}

	private static BlockPos clamp(ServerLevel target, BlockPos pos) {
		Era era = Era.fromDimension(target.dimension());
		return era == null ? pos : era.clampToWorld(pos);
	}

	private static void teleport(ServerPlayer player, ServerLevel target, Vec3 position) {
		player.teleport(new TeleportTransition(target, position, Vec3.ZERO, player.getYRot(), player.getXRot(), Relative.union(Relative.DELTA),
				TeleportTransition.DO_NOTHING));
	}

	/** For tests: whether this player's next landing is forgiven. */
	public static boolean isFallingIn(Player player) {
		return FALLING_IN.contains(player.getUUID());
	}
}
