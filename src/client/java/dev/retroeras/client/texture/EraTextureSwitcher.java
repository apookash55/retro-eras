package dev.retroeras.client.texture;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Turns an era's texture pack on while the player is in that era's dimension and off everywhere else, then reloads
 * resources. The selection is only changed in memory, so it is never saved into options.txt.
 */
public final class EraTextureSwitcher {
	private static final Map<Era, String> PACKS = new EnumMap<>(Era.class);
	private static volatile @Nullable Era activeEra;

	private EraTextureSwitcher() {
	}

	public static void init() {
		for (Era era : Era.values()) {
			EraTexturePack.prepare(era).ifPresent(pack -> PACKS.put(era, pack.packId()));
		}
		if (PACKS.isEmpty()) {
			return;
		}

		// The game may have saved an era pack as selected if the pack screen was opened while in an era.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> apply(client, null));
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> apply(client, Era.fromDimension(level.dimension())));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(() -> apply(client, null)));
	}

	/** The era whose pack is selected, for what loads with the resources. Null outside an era, or if that era has no pack. */
	public static @Nullable Era activeEra() {
		return activeEra;
	}

	/** Selects only {@code current}'s pack (or none) and reloads resources if that changed anything. */
	private static void apply(Minecraft client, @Nullable Era current) {
		activeEra = current != null && PACKS.containsKey(current) ? current : null;
		PackRepository repository = client.getResourcePackRepository();
		boolean changed = false;
		for (Map.Entry<Era, String> entry : PACKS.entrySet()) {
			boolean want = entry.getKey() == current;
			boolean selected = repository.getSelectedIds().contains(entry.getValue());
			if (want && !selected) {
				changed |= repository.addPack(entry.getValue());
			} else if (!want && selected) {
				changed |= repository.removePack(entry.getValue());
			}
		}

		if (changed) {
			RetroEras.LOGGER.info("Switching textures for {}", current == null ? Era.MODERN : current.getName());
			client.reloadResourcePacks();
		}
	}
}
