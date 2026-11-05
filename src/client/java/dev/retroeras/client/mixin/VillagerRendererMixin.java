package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.client.EraVillagers;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;

/** In 1.6.4 a baby villager was its parent's model at half size, in its job's outfit (see {@link EraVillagers}). */
@Mixin(VillagerRenderer.class)
abstract class VillagerRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
			at = @At("TAIL"))
	private void retroeras$oldBabies(Villager entity, VillagerRenderState state, float partialTicks, CallbackInfo ci) {
		EraVillagers.oldBaby(state);
	}
}
