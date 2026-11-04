package dev.retroeras.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.network.chat.Component;

/**
 * The render distance fog of Alpha, Beta and 1.6.4: fog starting a quarter of the way out and closing in completely at
 * the render distance, which was Far (256 blocks), Normal (128), Short (64) or Tiny (32). Each era keeps its own choice,
 * saved in config/retroeras.properties as fog.&lt;era&gt;, and the Render Distance key (R by default) cycles it as F did then.
 * "Modern" leaves 26.3's own fog alone.
 */
public final class EraFog {
	public enum Distance {
		FAR(256), NORMAL(128), SHORT(64), TINY(32), MODERN(0);

		private final int blocks;

		Distance(int blocks) {
			this.blocks = blocks;
		}

		Distance next(boolean backwards) {
			Distance[] all = values();
			return all[(ordinal() + (backwards ? all.length - 1 : 1)) % all.length];
		}

		String label() {
			return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
		}
	}

	private static final String CONFIG = "retroeras.properties";
	private static final Map<Era, Distance> DISTANCES = new EnumMap<>(Era.class);
	private static final KeyMapping CYCLE = new KeyMapping("key.retroeras.render_distance", InputConstants.KEY_R,
			KeyMapping.Category.register(RetroEras.id("retroeras")));

	private EraFog() {
	}

	public static void init() {
		load();
		KeyMappingHelper.registerKeyMapping(CYCLE);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (CYCLE.consumeClick()) {
				Era era = currentEra(client);
				if (era == null || client.player == null) {
					continue;
				}
				Distance next = distance(era).next(client.hasShiftDown());
				DISTANCES.put(era, next);
				save();
				client.player.sendOverlayMessage(Component.translatable("retroeras.render_distance", next.label()));
			}
		});
	}

	/** The era's default: the older the era, the closer the fog. Cave game had almost none, so it stays on Far. */
	private static Distance defaultFor(Era era) {
		return switch (era) {
			case OLD_RELEASE, CAVEGAME -> Distance.FAR;
			case BETA -> Distance.NORMAL;
			case ALPHA -> Distance.TINY;
			case PRE_ALPHA -> Distance.SHORT;
		};
	}

	public static Distance distance(Era era) {
		return DISTANCES.getOrDefault(era, defaultFor(era));
	}

	private static @Nullable Era currentEra(Minecraft client) {
		return client.level == null ? null : Era.fromDimension(client.level.dimension());
	}

	/**
	 * Applies the era's fog to what 26.3 worked out. Fog only ever closes in, so water, lava and blindness, which are
	 * already closer, keep theirs.
	 */
	public static void apply(FogData fog) {
		Era era = currentEra(Minecraft.getInstance());
		if (era == null) {
			return;
		}
		Distance distance = distance(era);
		if (distance == Distance.MODERN) {
			return;
		}
		float end = distance.blocks;
		float start = end * 0.25F;
		if (fog.environmentalEnd > end) {
			fog.environmentalStart = Math.min(fog.environmentalStart, start);
			fog.environmentalEnd = end;
		}
		fog.renderDistanceStart = Math.min(fog.renderDistanceStart, start);
		fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, end);
		fog.skyEnd = Math.min(fog.skyEnd, end);
		fog.cloudEnd = Math.min(fog.cloudEnd, end);
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG);
	}

	private static Properties readProperties() {
		Properties properties = new Properties();
		Path file = configFile();
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			} catch (IOException e) {
				RetroEras.LOGGER.warn("Could not read config/{}", CONFIG, e);
			}
		}
		return properties;
	}

	private static void load() {
		Properties properties = readProperties();
		for (Era era : Era.values()) {
			String value = properties.getProperty("fog." + era.getName());
			if (value == null) {
				continue;
			}
			try {
				DISTANCES.put(era, Distance.valueOf(value.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				RetroEras.LOGGER.warn("Unknown fog.{} value '{}' in config/{}; use far, normal, short, tiny or modern", era.getName(), value, CONFIG);
			}
		}
	}

	private static void save() {
		Properties properties = readProperties();
		for (Map.Entry<Era, Distance> entry : DISTANCES.entrySet()) {
			properties.setProperty("fog." + entry.getKey().getName(), entry.getValue().name().toLowerCase(Locale.ROOT));
		}
		try {
			Files.createDirectories(configFile().getParent());
			try (Writer writer = Files.newBufferedWriter(configFile())) {
				properties.store(writer, "Retro Eras. fog.<era> is far, normal, short, tiny or modern; <era>.jar points at an old client jar.");
			}
		} catch (IOException e) {
			RetroEras.LOGGER.warn("Could not save config/{}", CONFIG, e);
		}
	}
}
