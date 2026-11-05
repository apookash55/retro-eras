package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.retroeras.Era;
import dev.retroeras.player.EraAchievements;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

/**
 * The advancements screen shows only where you are: an era's own achievements in that era, 26.3's advancements outside
 * the eras, and nothing in the eras that had no achievements.
 */
@Mixin(AdvancementsScreen.class)
abstract class AdvancementsScreenMixin {
	@WrapOperation(method = "onAdvancementsUpdated", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;create(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;ILnet/minecraft/advancements/AdvancementNode;)Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;"))
	private AdvancementTab retroeras$eraTabsOnly(Minecraft minecraft, AdvancementsScreen screen, int index, AdvancementNode root, Operation<AdvancementTab> original) {
		Era here = minecraft.level == null ? null : Era.fromDimension(minecraft.level.dimension());
		if (EraAchievements.owner(root.holder().id()) != here) {
			return null;
		}
		return original.call(minecraft, screen, index, root);
	}
}
