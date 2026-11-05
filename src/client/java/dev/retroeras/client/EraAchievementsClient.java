package dev.retroeras.client;

import dev.retroeras.Era;
import dev.retroeras.player.EraAchievements;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * "Taking Inventory" was a client-side achievement, earned by opening the inventory, which the server never hears
 * about. The client tells it whenever the inventory opens in an era.
 */
public final class EraAchievementsClient {
	private EraAchievementsClient() {
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			boolean inventory = screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
			if (inventory && client.level != null && Era.fromDimension(client.level.dimension()) != null
					&& ClientPlayNetworking.canSend(EraAchievements.OpenedInventory.TYPE)) {
				ClientPlayNetworking.send(new EraAchievements.OpenedInventory());
			}
		});
	}
}
