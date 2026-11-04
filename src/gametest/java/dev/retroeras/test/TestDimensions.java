package dev.retroeras.test;

import java.io.IOException;
import java.io.Reader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import dev.retroeras.Era;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * The game-test server builds its own fixed set of levels and ignores datapack dimensions, so era tests load the
 * dimension file themselves and drive its generator directly.
 */
final class TestDimensions {
	private TestDimensions() {
	}

	/** Decodes data/retroeras/dimension/&lt;era&gt;.json with the same codec the game uses when it loads the world. */
	static LevelStem load(GameTestHelper helper, Era era) {
		Identifier file = era.getDimension().identifier().withPath(path -> "dimension/" + path + ".json");
		try (Reader reader = helper.getLevel().getServer().getResourceManager().openAsReader(file)) {
			JsonElement json = JsonParser.parseReader(reader);
			return LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess()), json)
					.getOrThrow(error -> helper.assertionException("Invalid " + file + ": " + error));
		} catch (IOException e) {
			throw helper.assertionException("Could not read " + file + ": " + e);
		}
	}
}
