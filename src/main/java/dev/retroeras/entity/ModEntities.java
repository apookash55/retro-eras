package dev.retroeras.entity;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	/** rd-161348 started with ten zombies. */
	public static final int CAVE_ZOMBIES = 10;

	private static final ResourceKey<EntityType<?>> CAVE_ZOMBIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, RetroEras.id("cave_zombie"));
	public static final EntityType<CaveZombie> CAVE_ZOMBIE = Registry.register(BuiltInRegistries.ENTITY_TYPE, CAVE_ZOMBIE_KEY,
			EntityType.Builder.of(CaveZombie::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build(CAVE_ZOMBIE_KEY));

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CAVE_ZOMBIE, CaveZombie.createAttributes());

		// Keep Cave game stocked with its ten zombies, dropping new ones in from above a random spot as the original did.
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (Era.fromDimension(level.dimension()) != Era.CAVEGAME || level.players().isEmpty() || level.getGameTime() % 100 != 0) {
				return;
			}
			if (level.getEntities(CAVE_ZOMBIE, zombie -> true).size() < CAVE_ZOMBIES) {
				spawnCaveZombie(level);
			}
		});
	}

	private static void spawnCaveZombie(ServerLevel level) {
		int size = Era.CAVEGAME.getWorldSize();
		// A few tries, as the map may not all be loaded around the players.
		BlockPos pos = null;
		for (int attempt = 0; attempt < 10 && pos == null; attempt++) {
			int x = level.getRandom().nextInt(size);
			int z = level.getRandom().nextInt(size);
			if (level.hasChunk(x >> 4, z >> 4)) {
				pos = new BlockPos(x, level.getMaxY() + 10, z);
			}
		}
		if (pos == null) {
			return;
		}
		CaveZombie zombie = CAVE_ZOMBIE.create(level, EntitySpawnReason.NATURAL);
		if (zombie != null) {
			zombie.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
			level.addFreshEntity(zombie);
		}
	}
}
