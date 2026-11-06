package dev.retroeras.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Getting into and out of 1.6.4's End, as EntityPlayerMP did it. A stronghold's portal leads to the obsidian platform at
 * (100, 48, 0). The exit portal always plays the credits, while you are out of the world, and then puts you at your bed
 * if it is in the era, or else on the surface at the era's centre.
 */
public final class EraEnd {
	/** Players watching the credits, and the era they go back to afterwards. */
	private static final Map<UUID, Era> LEAVING = new ConcurrentHashMap<>();

	private EraEnd() {
	}

	/** Whether {@code level} is an era's End, where the dragon and the crystals behave as they did in 1.6.4. */
	public static boolean isEraEnd(Level level) {
		Era era = Era.fromDimension(level.dimension());
		return era != null && era.isEnd(level.dimension());
	}

	/** Where an End portal takes {@code entity}: from an era to its End, or back for anything but a player. */
	public static @Nullable TeleportTransition destination(ServerLevel level, Entity entity, Era era) {
		if (era.isEnd(level.dimension())) {
			ServerLevel eraLevel = level.getServer().getLevel(era.getDimension());
			return eraLevel == null ? null : surface(eraLevel, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
		}
		ServerLevel end = era.getEnd() == null ? null : level.getServer().getLevel(era.getEnd());
		if (end == null) {
			return null;
		}
		Vec3 spawn = Vec3.atBottomCenterOf(ServerLevel.END_SPAWN_POINT);
		EndPlatformFeature.createEndPlatform(end, BlockPos.containing(spawn).below(), true);
		if (entity instanceof ServerPlayer) {
			spawn = spawn.subtract(0.0, 1.0, 0.0);
		}
		return new TeleportTransition(end, spawn, Vec3.ZERO, Direction.WEST.toYRot(), 0.0F, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
	}

	/** Leaves the End through its exit portal: every time, unlike now, the credits play. */
	public static void leave(ServerPlayer player, Era era) {
		EraAchievements.award(player, "the_end2");
		LEAVING.put(player.getUUID(), era);
		boolean seenCredits = player.seenCredits;
		player.showEndCredits();
		player.seenCredits = seenCredits;
	}

	/** Where a player who has just watched the credits goes, or null if they did not come from an era's End. */
	public static @Nullable TeleportTransition afterCredits(ServerPlayer player, TeleportTransition.PostTeleportTransition post) {
		Era era = LEAVING.remove(player.getUUID());
		if (era == null) {
			return null;
		}
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		if (respawn != null && respawn.respawnData().dimension() == era.getDimension()) {
			return null;
		}
		ServerLevel level = player.level().getServer().getLevel(era.getDimension());
		return level == null ? null : surface(level, post);
	}

	private static TeleportTransition surface(ServerLevel level, TeleportTransition.PostTeleportTransition post) {
		level.getChunk(0, 0);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
		return new TeleportTransition(level, new Vec3(0.5, y, 0.5), Vec3.ZERO, 0.0F, 0.0F, post);
	}

	/** The dragon's last act (generateExitPortal): a bedrock bowl around a pool of End portal, a torched pillar and the egg. */
	public static void buildExitPortal(ServerLevel level, int x, int z) {
		int floor = 64;
		int radius = 4;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int by = floor - 1; by <= floor + 32; by++) {
			for (int bx = x - radius; bx <= x + radius; bx++) {
				for (int bz = z - radius; bz <= z + radius; bz++) {
					double dx = bx - x;
					double dz = bz - z;
					double distance = dx * dx + dz * dz;
					if (distance > (radius - 0.5) * (radius - 0.5)) {
						continue;
					}
					boolean inner = distance <= (radius - 1 - 0.5) * (radius - 1 - 0.5);
					cursor.set(bx, by, bz);
					if (by < floor) {
						if (inner) {
							level.setBlock(cursor, Blocks.BEDROCK.defaultBlockState(), 2);
						}
					} else if (by > floor) {
						level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
					} else {
						level.setBlock(cursor, inner ? Blocks.END_PORTAL.defaultBlockState() : Blocks.BEDROCK.defaultBlockState(), 2);
					}
				}
			}
		}
		for (int by = floor; by <= floor + 3; by++) {
			level.setBlock(cursor.set(x, by, z), Blocks.BEDROCK.defaultBlockState(), 2);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			level.setBlock(new BlockPos(x, floor + 2, z).relative(side), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side), 2);
		}
		level.setBlock(cursor.set(x, floor + 4, z), Blocks.DRAGON_EGG.defaultBlockState(), 2);
	}
}
