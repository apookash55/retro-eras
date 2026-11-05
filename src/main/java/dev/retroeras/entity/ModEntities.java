package dev.retroeras.entity;

import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The zombies of Cave game rd-161348. Cave game is now rd-132211, the first build, which had none, so they no longer
 * spawn. The type stays registered so worlds from Retro Eras 0.2.0 still load, and any zombie left in one is removed.
 */
public final class ModEntities {
	private static final ResourceKey<EntityType<?>> CAVE_ZOMBIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, RetroEras.id("cave_zombie"));
	public static final EntityType<CaveZombie> CAVE_ZOMBIE = Registry.register(BuiltInRegistries.ENTITY_TYPE, CAVE_ZOMBIE_KEY,
			EntityType.Builder.of(CaveZombie::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build(CAVE_ZOMBIE_KEY));

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CAVE_ZOMBIE, CaveZombie.createAttributes());
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.getType() == CAVE_ZOMBIE) {
				entity.discard();
			}
		});
	}
}
