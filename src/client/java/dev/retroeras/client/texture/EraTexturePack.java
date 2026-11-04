package dev.retroeras.client.texture;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.SharedConstants;
import net.minecraft.util.Util;

/**
 * Builds a resource pack for an era out of the player's own copy of that era's Minecraft client jar, following the
 * mapping in assets/retroeras/era_textures/&lt;era&gt;.json. Mojang's textures are never shipped with the mod: they are
 * read from the jar the launcher downloaded and written to resourcepacks/retroeras_&lt;era&gt;/ on this machine only.
 */
public final class EraTexturePack {
	private static final String MARKER = ".retroeras-source";
	private static final String CONFIG = "retroeras.properties";

	private final Era era;
	private final JsonObject mapping;
	private final Path packDir;

	private EraTexturePack(Era era, JsonObject mapping, Path packDir) {
		this.era = era;
		this.mapping = mapping;
		this.packDir = packDir;
	}

	/** The pack id Minecraft gives a folder in resourcepacks/. */
	public String packId() {
		return "file/" + packDir.getFileName();
	}

	/** Loads the mapping for {@code era} and makes sure its pack is generated. Empty if the era has no mapping or no jar. */
	public static Optional<EraTexturePack> prepare(Era era) {
		JsonObject mapping = readMapping(era);
		if (mapping == null) {
			return Optional.empty();
		}

		String version = mapping.get("jar").getAsString();
		Path jar = findJar(era, version);
		if (jar == null) {
			RetroEras.LOGGER.warn("No Minecraft {} jar found, so the {} era will use modern textures. Install {} in the launcher "
					+ "or set {}.jar in config/{}", version, era.getName(), version, era.getName(), CONFIG);
			return Optional.empty();
		}

		EraTexturePack pack = new EraTexturePack(era, mapping, FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("retroeras_" + era.getName()));
		try {
			pack.generateIfStale(jar);
		} catch (IOException e) {
			RetroEras.LOGGER.error("Could not build the {} texture pack from {}", era.getName(), jar, e);
			return Optional.empty();
		}
		return Optional.of(pack);
	}

	private static JsonObject readMapping(Era era) {
		String path = "/assets/" + RetroEras.MOD_ID + "/era_textures/" + era.getName() + ".json";
		try (InputStream in = EraTexturePack.class.getResourceAsStream(path)) {
			if (in == null) {
				return null;
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException e) {
			RetroEras.LOGGER.error("Could not read texture mapping {}", path, e);
			return null;
		}
	}

	/** Looks for the jar in the config override, this game directory, then the default launcher directory. */
	private static Path findJar(Era era, String version) {
		Path gameDir = FabricLoader.getInstance().getGameDir();
		List<Path> candidates = new ArrayList<>();

		Path config = FabricLoader.getInstance().getConfigDir().resolve(CONFIG);
		if (Files.isRegularFile(config)) {
			Properties properties = new Properties();
			try (Reader reader = Files.newBufferedReader(config)) {
				properties.load(reader);
				String override = properties.getProperty(era.getName() + ".jar");
				if (override != null && !override.isBlank()) {
					candidates.add(Path.of(override.trim()));
				}
			} catch (IOException e) {
				RetroEras.LOGGER.warn("Could not read config/{}", CONFIG, e);
			}
		}

		candidates.add(gameDir.resolve("versions").resolve(version).resolve(version + ".jar"));
		candidates.add(launcherDir().resolve("versions").resolve(version).resolve(version + ".jar"));
		return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(null);
	}

	private static Path launcherDir() {
		String home = System.getProperty("user.home");
		return switch (Util.getPlatform()) {
			case WINDOWS -> Path.of(System.getenv().getOrDefault("APPDATA", home), ".minecraft");
			case OSX -> Path.of(home, "Library", "Application Support", "minecraft");
			default -> Path.of(home, ".minecraft");
		};
	}

	private void generateIfStale(Path jar) throws IOException {
		String source = jar.toAbsolutePath() + "|" + Files.size(jar) + "|" + Files.getLastModifiedTime(jar).toMillis() + "|" + mapping.hashCode()
				+ "|" + SharedConstants.RESOURCE_PACK_FORMAT_MAJOR;
		Path marker = packDir.resolve(MARKER);
		if (Files.isRegularFile(marker) && Files.readString(marker).equals(source)) {
			return;
		}

		deleteRecursively(packDir);
		Files.createDirectories(packDir);
		int written;
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			written = writeAtlases(zip) + writeFiles(zip) + writeRegions(zip) + writeCleared();
			ZipEntry icon = zip.getEntry("pack.png");
			if (icon != null) {
				try (InputStream in = zip.getInputStream(icon)) {
					Files.copy(in, packDir.resolve("pack.png"));
				}
			}
		}

		int format = SharedConstants.RESOURCE_PACK_FORMAT_MAJOR;
		Files.writeString(packDir.resolve("pack.mcmeta"), """
				{
				  "pack": {
				    "description": "Retro Eras: %s textures from your own %s jar. Switched on automatically in that era.",
				    "min_format": %d,
				    "max_format": %d
				  }
				}
				""".formatted(era.getName(), mapping.get("jar").getAsString(), format, format));
		Files.writeString(marker, source);
		RetroEras.LOGGER.info("Built {} era texture pack with {} textures from {}", era.getName(), written, jar);
	}

	private int writeAtlases(ZipFile zip) throws IOException {
		int written = 0;
		for (JsonElement atlasElement : array("atlases")) {
			JsonObject atlas = atlasElement.getAsJsonObject();
			BufferedImage image = read(zip, atlas.get("file").getAsString());
			if (image == null) {
				continue;
			}
			int tile = image.getWidth() / 16;
			for (Map.Entry<String, JsonElement> entry : atlas.getAsJsonObject("tiles").entrySet()) {
				int index = Integer.parseInt(entry.getKey());
				BufferedImage sub = crop(image, (index % 16) * tile, (index / 16) * tile, tile, tile);
				written += writeAll(sub, entry.getValue().getAsJsonArray());
			}
		}
		return written;
	}

	private int writeFiles(ZipFile zip) throws IOException {
		int written = 0;
		for (JsonElement element : array("files")) {
			JsonObject file = element.getAsJsonObject();
			BufferedImage image = read(zip, file.get("from").getAsString());
			if (image == null) {
				continue;
			}
			int scale = file.has("scale") ? file.get("scale").getAsInt() : 1;
			if (scale != 1) {
				image = scale(image, scale);
			}

			String transform = file.has("transform") ? file.get("transform").getAsString() : "none";
			JsonArray targets = file.getAsJsonArray("to");
			if (transform.equals("over_modern")) {
				// Each target may have its own modern texture underneath, so build them one at a time.
				for (JsonElement target : targets) {
					JsonArray single = new JsonArray();
					single.add(target);
					written += writeAll(overModern(image, target.getAsString()), single);
				}
				continue;
			}
			written += writeAll(transform.equals("legacy_skin") ? legacySkin(image) : image, targets);
		}
		return written;
	}

	/** Draws a 64x32 era texture over the top half of the modern 64x64 one, keeping the modern bottom half. */
	private static BufferedImage overModern(BufferedImage image, String target) throws IOException {
		BufferedImage out;
		try (InputStream in = EraTexturePack.class.getResourceAsStream("/assets/minecraft/textures/" + target + ".png")) {
			BufferedImage modern = in == null ? null : ImageIO.read(in);
			int width = modern == null ? image.getWidth() : modern.getWidth();
			int height = modern == null ? image.getHeight() : modern.getHeight();
			out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			if (modern != null) {
				g.drawImage(modern, 0, 0, null);
				// Clear the area the era texture covers so modern pixels never show through its transparent parts.
				g.setComposite(AlphaComposite.Clear);
				g.fillRect(0, 0, image.getWidth(), image.getHeight());
				g.setComposite(AlphaComposite.SrcOver);
			}
			g.drawImage(image, 0, 0, null);
			g.dispose();
		}
		return out;
	}

	/**
	 * Converts a 64x32 skin to the 64x64 layout, mirroring the right arm and leg into the new left-limb slots. This is
	 * the same conversion vanilla applies to legacy skins in SkinTextureDownloader.processLegacySkin.
	 */
	private static BufferedImage legacySkin(BufferedImage image) {
		if (image.getWidth() != 64 || image.getHeight() != 32) {
			return image;
		}
		BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		int[][] copies = {
				{4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4}, {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12},
				{8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12}, {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4},
				{40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12}
		};
		for (int[] c : copies) {
			// Each region is copied by (offsetX, offsetY) and flipped horizontally.
			for (int y = 0; y < c[5]; y++) {
				for (int x = 0; x < c[4]; x++) {
					int argb = out.getRGB(c[0] + x, c[1] + y);
					out.setRGB(c[0] + c[2] + (c[4] - 1 - x), c[1] + c[3] + y, argb);
				}
			}
		}
		// The base layers are opaque in vanilla; only the hat/jacket overlays may be transparent.
		makeOpaque(out, 0, 0, 32, 16);
		makeOpaque(out, 0, 16, 64, 32);
		makeOpaque(out, 16, 48, 48, 64);
		return out;
	}

	private static void makeOpaque(BufferedImage image, int x0, int y0, int x1, int y1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				image.setRGB(x, y, image.getRGB(x, y) | 0xFF000000);
			}
		}
	}

	private int writeRegions(ZipFile zip) throws IOException {
		int written = 0;
		for (JsonElement element : array("regions")) {
			JsonObject region = element.getAsJsonObject();
			BufferedImage image = read(zip, region.get("from").getAsString());
			if (image == null) {
				continue;
			}
			BufferedImage sub = crop(image, region.get("x").getAsInt(), region.get("y").getAsInt(), region.get("w").getAsInt(), region.get("h").getAsInt());
			written += writeAll(sub, region.getAsJsonArray("to"));
		}
		return written;
	}

	/** Writes fully transparent textures, for modern layers the era's textures already include (e.g. a grass overlay). */
	private int writeCleared() throws IOException {
		return writeAll(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), array("clear"));
	}

	private JsonArray array(String key) {
		return mapping.has(key) ? mapping.getAsJsonArray(key) : new JsonArray();
	}

	private static BufferedImage read(ZipFile zip, String name) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) {
			RetroEras.LOGGER.warn("Texture {} is not in the jar, skipping it", name);
			return null;
		}
		try (InputStream in = zip.getInputStream(entry)) {
			return ImageIO.read(in);
		}
	}

	private int writeAll(BufferedImage image, JsonArray targets) throws IOException {
		for (JsonElement target : targets) {
			Path out = packDir.resolve("assets/minecraft/textures/" + target.getAsString() + ".png");
			Files.createDirectories(out.getParent());
			ImageIO.write(image, "png", out.toFile());
		}
		return targets.size();
	}

	private static BufferedImage crop(BufferedImage image, int x, int y, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, w, h, x, y, x + w, y + h, null);
		g.dispose();
		return out;
	}

	private static BufferedImage scale(BufferedImage image, int factor) {
		BufferedImage out = new BufferedImage(image.getWidth() * factor, image.getHeight() * factor, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), null);
		g.dispose();
		return out;
	}

	private static void deleteRecursively(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}
}
