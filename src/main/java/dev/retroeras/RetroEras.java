package dev.retroeras;

import dev.retroeras.block.ModBlocks;
import dev.retroeras.player.EraCombat;
import dev.retroeras.player.EraInventories;
import dev.retroeras.player.EraItems;
import dev.retroeras.player.EraRules;
import dev.retroeras.portal.PortalIgnition;
import dev.retroeras.worldgen.ModWorldgen;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetroEras implements ModInitializer {
	public static final String MOD_ID = "retroeras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModWorldgen.init();
		ModBlocks.init();
		PortalIgnition.init();
		EraInventories.init();
		EraRules.init();
		EraCombat.init();
		EraItems.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
