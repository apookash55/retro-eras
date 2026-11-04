package dev.retroeras.worldgen;

import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaBiomeSource;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.cavegame.CavegameChunkGenerator;
import dev.retroeras.worldgen.infdev.InfdevChunkGenerator;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModWorldgen {
	private ModWorldgen() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("beta"), BetaChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, RetroEras.id("beta"), BetaBiomeSource.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("cavegame"), CavegameChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("infdev"), InfdevChunkGenerator.CODEC);

		// Seed the Beta generator as soon as its level loads, so biome lookups (e.g. /locate) work before any chunk generates.
		ServerLevelEvents.LOAD.register((server, level) -> {
			if (level.getChunkSource().getGenerator() instanceof BetaChunkGenerator beta) {
				beta.terrain(level.getSeed());
			}
		});
	}
}
