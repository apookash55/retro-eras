package dev.retroeras.client.particle;

import dev.retroeras.Era;
import dev.retroeras.block.ModBlocks;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * An era portal's particles: the nether portal's swirl, coloured like the era's portal texture instead of purple.
 * The colours are each texture's average, scaled so the strongest channel is full, as the nether portal's purple is.
 */
public final class EraPortalParticle extends PortalParticle {
	private EraPortalParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite, int color) {
		super(level, x, y, z, xd, yd, zd, sprite);
		// PortalParticle picked a random brightness and tinted it purple; keep the brightness, change the tint.
		float brightness = bCol;
		rCol = brightness * ((color >> 16) & 0xFF) / 255.0F;
		gCol = brightness * ((color >> 8) & 0xFF) / 255.0F;
		bCol = brightness * (color & 0xFF) / 255.0F;
	}

	private static int color(Era era) {
		return switch (era) {
			case OLD_RELEASE -> 0xC4C4FF; // grey-blue
			case BETA -> 0x77FF69;        // green
			case ALPHA -> 0xFF950F;       // pumpkin orange
			case PRE_ALPHA -> 0xFF7947;   // brick red
			case CAVEGAME -> 0x80BEFF;    // sky blue
		};
	}

	public static void init() {
		for (Era era : Era.values()) {
			int color = color(era);
			ParticleProviderRegistry.getInstance().register(ModBlocks.portalParticle(era), sprites ->
					(options, level, x, y, z, xd, yd, zd, random) -> new EraPortalParticle(level, x, y, z, xd, yd, zd, sprites.get(random), color));
		}
	}
}
