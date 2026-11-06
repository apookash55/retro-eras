package dev.retroeras.worldgen;

import dev.retroeras.RetroEras;
import dev.retroeras.worldgen.beta.BetaBiomeSource;
import dev.retroeras.worldgen.beta.BetaChunkGenerator;
import dev.retroeras.worldgen.cavegame.CavegameChunkGenerator;
import dev.retroeras.worldgen.infdev.InfdevChunkGenerator;
import dev.retroeras.worldgen.release.ReleaseBiomeSource;
import dev.retroeras.worldgen.release.ReleaseChunkGenerator;

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
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("classic"), dev.retroeras.worldgen.classic.ClassicChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("infdev"), InfdevChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("release"), ReleaseChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, RetroEras.id("release"), ReleaseBiomeSource.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("nether"), dev.retroeras.worldgen.nether.NetherChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, RetroEras.id("end"), dev.retroeras.worldgen.end.EndChunkGenerator.CODEC);

		// Seed the Beta and 1.6.4 generators as soon as their level loads, so biome lookups (e.g. /locate) work before any chunk generates.
		ServerLevelEvents.LOAD.register((server, level) -> {
			if (level.getChunkSource().getGenerator() instanceof BetaChunkGenerator beta) {
				beta.terrain(level.getSeed());
			} else if (level.getChunkSource().getGenerator() instanceof ReleaseChunkGenerator release) {
				release.terrain(level.getSeed());
			}
		});
	}
}
