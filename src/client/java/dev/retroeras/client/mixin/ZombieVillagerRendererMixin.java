package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.client.EraVillagers;

import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/** In 1.6.4 a baby zombie villager was the adult model at half size (see {@link EraVillagers}). */
@Mixin(ZombieVillagerRenderer.class)
abstract class ZombieVillagerRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;Lnet/minecraft/client/renderer/entity/state/ZombieVillagerRenderState;F)V",
			at = @At("TAIL"))
	private void retroeras$oldBabies(ZombieVillager entity, ZombieVillagerRenderState state, float partialTicks, CallbackInfo ci) {
		EraVillagers.oldBaby(state);
	}
}
