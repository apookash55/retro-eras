package dev.retroeras.client;

import dev.retroeras.client.texture.EraTextureSwitcher;

import net.fabricmc.api.ClientModInitializer;

public class RetroErasClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EraTextureSwitcher.init();
		EraHud.init();
		EraLighting.init();
	}
}
