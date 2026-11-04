package dev.retroeras.client;

import dev.retroeras.client.entity.CaveZombieRenderer;
import dev.retroeras.client.texture.EraTextureSwitcher;
import dev.retroeras.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class RetroErasClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EraTextureSwitcher.init();
		EraHud.init();
		EraLighting.init();
		EntityRendererRegistry.register(ModEntities.CAVE_ZOMBIE, CaveZombieRenderer::new);
	}
}
