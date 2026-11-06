package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableMap;

import dev.retroeras.Era;
import dev.retroeras.client.entity.OldMobModels;
import dev.retroeras.client.texture.EraTextureSwitcher;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.LayerDefinitions;

/** The era's old mob models, loaded along with its textures whenever the resources reload on the way into an era. */
@Mixin(EntityModelSet.class)
abstract class EntityModelSetMixin {
	@Inject(method = "vanilla", at = @At("HEAD"), cancellable = true)
	private static void retroeras$oldModels(CallbackInfoReturnable<EntityModelSet> cir) {
		Era era = EraTextureSwitcher.activeEra();
		if (era != null) {
			cir.setReturnValue(new EntityModelSet(ImmutableMap.copyOf(OldMobModels.forEra(LayerDefinitions.createRoots(), era))));
		}
	}
}
