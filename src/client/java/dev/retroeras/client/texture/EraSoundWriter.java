package dev.retroeras.client.texture;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.retroeras.RetroEras;

/**
 * Adds an era's sounds to its pack, following assets/retroeras/era_sounds/&lt;era&gt;.json. Old Minecraft fetched its
 * sounds separately from the jar, so they are read from the launcher's asset store (an index of names to hashed
 * objects, like the launcher keeps for every version). Writes the sound files plus a sounds.json that replaces the
 * mapped 26.3 events, silences events that did not exist yet, and limits game music to the era's tracks.
 */
final class EraSoundWriter {
	private static final Pattern UNSAFE = Pattern.compile("[^a-z0-9/._-]");

	private EraSoundWriter() {
	}

	/** Finds the asset folder holding the mapping's index, in the game directory or the launcher's. */
	static @Nullable Path findAssets(JsonObject sounds, List<Path> roots) {
		String index = sounds.get("index").getAsString();
		return roots.stream().map(root -> root.resolve("assets"))
				.filter(assets -> Files.isRegularFile(assets.resolve("indexes").resolve(index + ".json")))
				.findFirst().orElse(null);
	}

	/** Writes the sounds into {@code packDir} and returns how many sound files were copied. */
	static int write(JsonObject sounds, Path assets, Path packDir) throws IOException {
		String prefix = sounds.get("prefix").getAsString();
		JsonObject objects;
		try (Reader reader = Files.newBufferedReader(assets.resolve("indexes").resolve(sounds.get("index").getAsString() + ".json"))) {
			objects = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("objects");
		}

		JsonObject soundsJson = new JsonObject();
		int copied = 0;
		for (Map.Entry<String, JsonElement> event : sounds.getAsJsonObject("events").entrySet()) {
			JsonArray entries = new JsonArray();
			for (JsonElement name : event.getValue().getAsJsonArray()) {
				Pattern files = Pattern.compile(Pattern.quote(prefix + name.getAsString()) + "\\d*\\.ogg");
				for (Map.Entry<String, JsonElement> object : objects.entrySet()) {
					if (!files.matcher(object.getKey()).matches()) {
						continue;
					}
					String hash = object.getValue().getAsJsonObject().get("hash").getAsString();
					Path source = assets.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
					if (!Files.isRegularFile(source)) {
						continue;
					}
					// Old names can hold spaces and capitals, which 26.3 resource paths do not allow.
					String path = "retroeras/" + UNSAFE.matcher(object.getKey().substring(prefix.length()).toLowerCase(Locale.ROOT)).replaceAll("_");
					Path target = packDir.resolve("assets/minecraft/sounds").resolve(path);
					if (!Files.exists(target)) {
						Files.createDirectories(target.getParent());
						Files.copy(source, target);
						copied++;
					}
					entries.add(path.substring(0, path.length() - ".ogg".length()));
				}
			}
			soundsJson.add(event.getKey(), replacement(entries));
		}

		List<JsonElement> tracks = new ArrayList<>();
		for (JsonElement track : sounds.getAsJsonArray("music")) {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", track.getAsString());
			entry.addProperty("stream", true);
			tracks.add(entry);
		}
		for (String musicEvent : List.of("music.game", "music.creative")) {
			JsonArray entries = new JsonArray();
			tracks.forEach(entries::add);
			soundsJson.add(musicEvent, replacement(entries));
		}

		Files.createDirectories(packDir.resolve("assets/minecraft"));
		Files.writeString(packDir.resolve("assets/minecraft/sounds.json"), new GsonBuilder().setPrettyPrinting().create().toJson(soundsJson));
		RetroEras.LOGGER.debug("Wrote {} sound events", soundsJson.size());
		return copied;
	}

	private static JsonObject replacement(JsonArray entries) {
		JsonObject definition = new JsonObject();
		definition.addProperty("replace", true);
		definition.add("sounds", entries);
		return definition;
	}
}
