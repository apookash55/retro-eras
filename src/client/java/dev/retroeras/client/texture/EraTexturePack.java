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

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.client.EraLightmap;

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
	private final @Nullable JsonObject sounds;
	private final Path packDir;

	private EraTexturePack(Era era, JsonObject mapping, @Nullable JsonObject sounds, Path packDir) {
		this.era = era;
		this.mapping = mapping;
		this.sounds = sounds;
		this.packDir = packDir;
	}

	/** The pack id Minecraft gives a folder in resourcepacks/. */
	public String packId() {
		return "file/" + packDir.getFileName();
	}

	/** Loads the mapping for {@code era} and makes sure its pack is generated. Empty if the era has no mapping or no jar. */
	public static Optional<EraTexturePack> prepare(Era era) {
		JsonObject mapping = readMapping("era_textures", era);
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

		EraTexturePack pack = new EraTexturePack(era, mapping, readMapping("era_sounds", era), FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("retroeras_" + era.getName()));
		try {
			pack.generateIfStale(jar);
			pack.writeShaders();
			pack.writeLang();
		} catch (IOException e) {
			RetroEras.LOGGER.error("Could not build the {} texture pack from {}", era.getName(), jar, e);
			return Optional.empty();
		}
		return Optional.of(pack);
	}

	/**
	 * Copies the era's replacement core shaders into the pack: the grey, max-of-sky-and-block lightmap of the eras before
	 * Beta 1.8. Written on every start rather than only when the pack is rebuilt, so shader changes in the mod apply.
	 */
	/**
	 * The era's own words, from its language file: Beta 1.7.3 and 1.6.4 announced "Achievement get!" and called the
	 * screen "Achievements". Written on every start, like the shaders.
	 */
	private void writeLang() throws IOException {
		Path target = packDir.resolve("assets/minecraft/lang/en_us.json");
		if (era != Era.BETA && era != Era.OLD_RELEASE) {
			Files.deleteIfExists(target);
			return;
		}
		JsonObject lang = new JsonObject();
		for (String toast : List.of("task", "goal", "challenge")) {
			lang.addProperty("advancements.toast." + toast, "Achievement get!");
		}
		lang.addProperty("gui.advancements", "Achievements");
		Files.createDirectories(target.getParent());
		Files.writeString(target, lang.toString());
	}

	private void writeShaders() throws IOException {
		Path target = packDir.resolve("assets/minecraft/shaders/core/lightmap.fsh");
		if (!EraLightmap.usesOldLightmap(era)) {
			Files.deleteIfExists(target);
			return;
		}
		try (InputStream in = EraTexturePack.class.getResourceAsStream("/assets/" + RetroEras.MOD_ID + "/era_shaders/lightmap.fsh")) {
			if (in == null) {
				throw new IOException("The mod's era_shaders/lightmap.fsh is missing");
			}
			Files.createDirectories(target.getParent());
			Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static @Nullable JsonObject readMapping(String folder, Era era) {
		String path = "/assets/" + RetroEras.MOD_ID + "/" + folder + "/" + era.getName() + ".json";
		try (InputStream in = EraTexturePack.class.getResourceAsStream(path)) {
			if (in == null) {
				return null;
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException e) {
			RetroEras.LOGGER.error("Could not read mapping {}", path, e);
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
		Path assets = sounds == null ? null : EraSoundWriter.findAssets(sounds, List.of(FabricLoader.getInstance().getGameDir(), launcherDir()));
		String source = jar.toAbsolutePath() + "|" + Files.size(jar) + "|" + Files.getLastModifiedTime(jar).toMillis() + "|" + mapping.hashCode()
				+ "|" + (sounds == null ? 0 : sounds.hashCode()) + "|" + assets + "|" + SharedConstants.RESOURCE_PACK_FORMAT_MAJOR;
		Path marker = packDir.resolve(MARKER);
		if (Files.isRegularFile(marker) && Files.readString(marker).equals(source)) {
			return;
		}

		deleteRecursively(packDir);
		Files.createDirectories(packDir);
		int written;
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			written = writeAtlases(zip) + writeFiles(zip) + writeRegions(zip) + writeCleared() + writeComposed(zip) + writeEffects();
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
				    "description": "Retro Eras: %s textures and sounds from your own %s. Switched on automatically in that era.",
				    "min_format": %d,
				    "max_format": %d
				  }
				}
				""".formatted(era.getName(), mapping.get("jar").getAsString(), format, format));
		int soundFiles = 0;
		if (sounds != null && assets != null) {
			soundFiles = EraSoundWriter.write(sounds, assets, packDir);
		} else if (sounds != null) {
			RetroEras.LOGGER.warn("No '{}' sound assets found, so the {} era will use modern sounds. Play {} once in the launcher to download them",
					sounds.get("index").getAsString(), era.getName(), mapping.get("jar").getAsString());
		}
		Files.writeString(marker, source);
		RetroEras.LOGGER.info("Built {} era pack with {} textures and {} sounds from {}", era.getName(), written, soundFiles, jar);
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
			BufferedImage transformed = switch (transform) {
				case "legacy_skin" -> legacySkin(image);
				case "villager" -> villager(image);
				default -> image;
			};
			written += writeAll(transformed, targets);
			copyAnimation(zip, file.get("from").getAsString(), targets);
		}
		return written;
	}

	/** Copies an animated texture's .mcmeta (1.6+ jars have them), so the strip animates instead of showing squashed. */
	private void copyAnimation(ZipFile zip, String from, JsonArray targets) throws IOException {
		ZipEntry meta = zip.getEntry(from + ".mcmeta");
		if (meta == null) {
			return;
		}
		byte[] bytes;
		try (InputStream in = zip.getInputStream(meta)) {
			bytes = in.readAllBytes();
		}
		for (JsonElement target : targets) {
			Files.write(packDir.resolve("assets/minecraft/textures/" + path(target.getAsString()) + ".png.mcmeta"), bytes);
		}
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
	 * An old villager skin for 26.3's villager model, which shares its layout but added a hat and a hat brim. The old
	 * skins have stray pixels where the hat now is, which 26.3 would draw as a hood, so the hat (32,0 to 64,19) and brim
	 * (30,47 to 64,64) areas are cleared.
	 */
	private static BufferedImage villager(BufferedImage image) {
		if (image.getWidth() != 64 || image.getHeight() != 64) {
			return image;
		}
		BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(32, 0, 32, 19);
		g.fillRect(30, 47, 34, 17);
		g.dispose();
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

	/**
	 * Writes {@code image} to every target. A target may end in |-separated adjustments applied to that copy only:
	 * flipx, rot90, rot180, rot270 (clockwise), grey, or untint:RRGGBB, which divides out a colour 26.3 multiplies the texture by
	 * (old leather and water had their colour drawn in; 26.3 draws them grey and tints them).
	 */
	private int writeAll(BufferedImage image, JsonArray targets) throws IOException {
		for (JsonElement target : targets) {
			String[] parts = target.getAsString().split("\\|");
			BufferedImage adjusted = image;
			for (int i = 1; i < parts.length; i++) {
				adjusted = adjust(adjusted, parts[i]);
			}
			Path out = packDir.resolve("assets/minecraft/textures/" + parts[0] + ".png");
			Files.createDirectories(out.getParent());
			ImageIO.write(adjusted, "png", out.toFile());
		}
		return targets.size();
	}

	/** A target's texture path, without its adjustments. */
	private static String path(String target) {
		int bar = target.indexOf('|');
		return bar < 0 ? target : target.substring(0, bar);
	}

	private static BufferedImage adjust(BufferedImage image, String adjustment) {
		if (adjustment.startsWith("untint:")) {
			int tint = Integer.parseInt(adjustment.substring("untint:".length()), 16);
			BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					int argb = image.getRGB(x, y);
					out.setRGB(x, y, argb & 0xFF000000 | untint(argb >> 16, tint >> 16) << 16 | untint(argb >> 8, tint >> 8) << 8 | untint(argb, tint));
				}
			}
			return out;
		}
		if (adjustment.equals("grey")) {
			// Brightest channel as grey, so a texture drawn in one colour can be tinted (Alpha's red redstone dust).
			BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					int argb = image.getRGB(x, y);
					int v = Math.max(argb >> 16 & 255, Math.max(argb >> 8 & 255, argb & 255));
					out.setRGB(x, y, argb & 0xFF000000 | v << 16 | v << 8 | v);
				}
			}
			return out;
		}
		int w = image.getWidth();
		int h = image.getHeight();
		int turns = switch (adjustment) {
			case "rot90" -> 1;
			case "rot180" -> 2;
			case "rot270" -> 3;
			case "flipx" -> 0;
			default -> throw new IllegalArgumentException("Unknown texture adjustment " + adjustment);
		};
		BufferedImage out = new BufferedImage(turns % 2 == 1 ? h : w, turns % 2 == 1 ? w : h, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int argb = image.getRGB(x, y);
				switch (turns) {
					case 1 -> out.setRGB(h - 1 - y, x, argb);
					case 2 -> out.setRGB(w - 1 - x, h - 1 - y, argb);
					case 3 -> out.setRGB(y, w - 1 - x, argb);
					default -> out.setRGB(w - 1 - x, y, argb);
				}
			}
		}
		return out;
	}

	private static int untint(int channel, int tint) {
		int c = channel & 255;
		int t = Math.max(tint & 255, 1);
		return Math.min(255, c * 255 / t);
	}

	/**
	 * Builds textures whose 26.3 layout has no single old counterpart (chest models, the inventory screen) from parts of
	 * old textures: each part copies a rectangle from an atlas tile or a file to a place in the new texture, optionally
	 * scaled and shaded, or fills a rectangle with a colour.
	 */
	private int writeComposed(ZipFile zip) throws IOException {
		int written = 0;
		for (JsonElement element : array("compose")) {
			JsonObject compose = element.getAsJsonObject();
			BufferedImage out;
			if (compose.has("base")) {
				BufferedImage base = read(zip, compose.get("base").getAsString());
				if (base == null) {
					continue;
				}
				out = crop(base, 0, 0, base.getWidth(), base.getHeight());
			} else {
				JsonArray size = compose.getAsJsonArray("size");
				out = new BufferedImage(size.get(0).getAsInt(), size.get(1).getAsInt(), BufferedImage.TYPE_INT_ARGB);
			}
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			boolean complete = true;
			for (JsonElement partElement : compose.getAsJsonArray("parts")) {
				JsonObject part = partElement.getAsJsonObject();
				JsonArray dst = part.getAsJsonArray("dst");
				int dx = dst.get(0).getAsInt();
				int dy = dst.get(1).getAsInt();
				if (part.has("fill")) {
					g.setComposite(AlphaComposite.Src);
					g.setColor(new java.awt.Color((int) Long.parseLong(part.get("fill").getAsString(), 16), true));
					g.fillRect(dx, dy, dst.get(2).getAsInt(), dst.get(3).getAsInt());
					g.setComposite(AlphaComposite.SrcOver);
					continue;
				}
				BufferedImage source = read(zip, part.has("file") ? part.get("file").getAsString() : "terrain.png");
				if (source == null) {
					complete = false;
					break;
				}
				JsonArray src = part.getAsJsonArray("src");
				int sx = src.get(0).getAsInt();
				int sy = src.get(1).getAsInt();
				if (part.has("tile")) {
					int tile = source.getWidth() / 16;
					int index = part.get("tile").getAsInt();
					sx += (index % 16) * tile;
					sy += (index / 16) * tile;
				}
				BufferedImage piece = crop(source, sx, sy, src.get(2).getAsInt(), src.get(3).getAsInt());
				if (part.has("shade")) {
					piece = shade(piece, part.get("shade").getAsFloat());
				}
				int dw = dst.size() > 2 ? dst.get(2).getAsInt() : piece.getWidth();
				int dh = dst.size() > 3 ? dst.get(3).getAsInt() : piece.getHeight();
				g.setComposite(AlphaComposite.Src);
				g.drawImage(piece, dx, dy, dx + dw, dy + dh, 0, 0, piece.getWidth(), piece.getHeight(), null);
				g.setComposite(AlphaComposite.SrcOver);
			}
			g.dispose();
			if (complete) {
				written += writeAll(out, compose.getAsJsonArray("to"));
			}
		}
		return written;
	}

	private static BufferedImage shade(BufferedImage image, float factor) {
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int argb = image.getRGB(x, y);
				int r = (int) ((argb >> 16 & 255) * factor);
				int gr = (int) ((argb >> 8 & 255) * factor);
				int b = (int) ((argb & 255) * factor);
				out.setRGB(x, y, argb & 0xFF000000 | Math.min(r, 255) << 16 | Math.min(gr, 255) << 8 | Math.min(b, 255));
			}
		}
		return out;
	}

	/** Writes the animations old versions drew with code (water, lava, fire, the portal), see LegacyTextureEffects. */
	private int writeEffects() throws IOException {
		int written = 0;
		for (JsonElement element : array("effects")) {
			JsonObject effect = element.getAsJsonObject();
			BufferedImage strip = LegacyTextureEffects.strip(effect.get("effect").getAsString(), effect.get("frames").getAsInt(),
					effect.has("seed") ? effect.get("seed").getAsLong() : 0L);
			int scale = effect.has("scale") ? effect.get("scale").getAsInt() : 1;
			if (scale != 1) {
				strip = scale(strip, scale);
			}
			JsonArray targets = effect.getAsJsonArray("to");
			written += writeAll(strip, targets);
			for (JsonElement target : targets) {
				Files.writeString(packDir.resolve("assets/minecraft/textures/" + path(target.getAsString()) + ".png.mcmeta"),
						"{\"animation\": {\"frametime\": 1}}\n");
			}
		}
		return written;
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
