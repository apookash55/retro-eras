package dev.retroeras.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

import dev.retroeras.RetroEras;

import net.fabricmc.loader.api.FabricLoader;

/**
 * config/retroeras.properties, shared by the fog, the HUD and the texture packs. Every write re-reads the file first so
 * settings the player added by hand, like &lt;era&gt;.jar, are kept.
 */
public final class EraConfig {
	public static final String FILE = "retroeras.properties";
	private static final String HEADER = "Retro Eras. fog.<era> is far, normal, short, tiny or modern; hud.version is true or false; "
			+ "<era>.jar points at an old client jar.";

	private EraConfig() {
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE);
	}

	public static Properties read() {
		Properties properties = new Properties();
		Path file = path();
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			} catch (IOException e) {
				RetroEras.LOGGER.warn("Could not read config/{}", FILE, e);
			}
		}
		return properties;
	}

	/** Reads the file, lets {@code change} edit it and writes it back. */
	public static void update(Consumer<Properties> change) {
		Properties properties = read();
		change.accept(properties);
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				properties.store(writer, HEADER);
			}
		} catch (IOException e) {
			RetroEras.LOGGER.warn("Could not save config/{}", FILE, e);
		}
	}
}
