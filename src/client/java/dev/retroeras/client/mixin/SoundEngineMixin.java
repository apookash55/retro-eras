package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.retroeras.Era;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;

/** Cave game had no sound at all, so nothing plays while the player is in it: no blocks, music or interface clicks. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void retroeras$silenceCaveGame(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && Era.fromDimension(client.level.dimension()) == Era.CAVEGAME) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}
