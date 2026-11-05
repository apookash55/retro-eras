package dev.retroeras.client;

import dev.retroeras.Era;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * How villagers look in 1.6.4. Their outfits come from the era pack: each job's 1.6.4 skin, with 26.3's biome outfits
 * and job-level badges cleared. 26.3 also gave babies a model of their own with a big head; 1.6.4 simply drew the adult
 * at half size, so in that era babies are drawn as small adults, wearing their job's outfit like their parents.
 */
public final class EraVillagers {
	private EraVillagers() {
	}

	public static void oldBaby(LivingEntityRenderState state) {
		Minecraft client = Minecraft.getInstance();
		if (state.isBaby && client.level != null && Era.fromDimension(client.level.dimension()) == Era.OLD_RELEASE) {
			state.isBaby = false;
			state.scale *= 0.5F;
		}
	}
}
