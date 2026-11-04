package dev.retroeras.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.portal.EraPortalBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	private static final Map<Era, EraPortalBlock> PORTALS = new EnumMap<>(Era.class);
	private static final Map<Era, ResourceKey<PoiType>> PORTAL_POIS = new EnumMap<>(Era.class);

	private ModBlocks() {
	}

	public static void init() {
		for (Era era : Era.values()) {
			String name = era.getName() + "_portal";
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, RetroEras.id(name));
			BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
					.noCollision()
					.strength(-1.0F)
					.sound(SoundType.GLASS)
					.lightLevel(state -> 11)
					.pushReaction(PushReaction.IMMOVEABLE)
					.setId(key);
			EraPortalBlock portal = Registry.register(BuiltInRegistries.BLOCK, key, new EraPortalBlock(era, properties));
			PORTALS.put(era, portal);

			// A POI per era lets portals find their partner in the other dimension without scanning blocks.
			ResourceKey<PoiType> poi = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, RetroEras.id(name));
			PoiTypes.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, poi, Set.copyOf(portal.getStateDefinition().getPossibleStates()), 0, 1);
			PORTAL_POIS.put(era, poi);
		}
	}

	public static EraPortalBlock portal(Era era) {
		return PORTALS.get(era);
	}

	public static ResourceKey<PoiType> portalPoi(Era era) {
		return PORTAL_POIS.get(era);
	}
}
