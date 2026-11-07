package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;
import dev.retroeras.client.EraDrawing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * The old F3 screen (GuiIngame): the version and frame rate, the player's position and facing, and memory on the right, in
 * place of 26.3's long lists. Its lines of chunk and entity counts told you about the old renderer, so they are not shown.
 */
@Mixin(DebugScreenOverlay.class)
abstract class DebugScreenOverlayMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldDebugScreen(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		Era era = EraDrawing.currentEra();
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (era == null || player == null || !((DebugScreenOverlay) (Object) this).showDebugScreen()) {
			return;
		}
		ci.cancel();
		Font font = client.font;
		int light = 0xFFE0E0E0;
		graphics.text(font, title(era) + " (" + client.getFps() + " fps)", 2, 2, 0xFFFFFFFF, true);
		int x = Mth.floor(player.getX());
		int z = Mth.floor(player.getZ());
		int facing = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3;
		if (era == Era.OLD_RELEASE) {
			graphics.text(font, String.format("x: %.5f (%d) // c: %d (%d)", player.getX(), x, x >> 4, x & 15), 2, 64, light, true);
			graphics.text(font, String.format("y: %.3f (feet pos, %.3f eyes pos)", player.getY(), player.getEyeY()), 2, 72, light, true);
			graphics.text(font, String.format("z: %.5f (%d) // c: %d (%d)", player.getZ(), z, z >> 4, z & 15), 2, 80, light, true);
			graphics.text(font, "f: " + facing + " / " + Mth.wrapDegrees(player.getYRot()), 2, 88, light, true);
		} else {
			graphics.text(font, "x: " + player.getX(), 2, 64, light, true);
			graphics.text(font, "y: " + player.getY(), 2, 72, light, true);
			graphics.text(font, "z: " + player.getZ(), 2, 80, light, true);
			graphics.text(font, "f: " + facing, 2, 88, light, true);
		}
		long max = Runtime.getRuntime().maxMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = total - Runtime.getRuntime().freeMemory();
		String memory = "Used memory: " + used * 100L / max + "% (" + used / 1024L / 1024L + "MB) of " + max / 1024L / 1024L + "MB";
		graphics.text(font, memory, graphics.guiWidth() - font.width(memory) - 2, 2, light, true);
		String allocated = "Allocated memory: " + total * 100L / max + "% (" + total / 1024L / 1024L + "MB)";
		graphics.text(font, allocated, graphics.guiWidth() - font.width(allocated) - 2, 12, light, true);
	}

	private static String title(Era era) {
		return switch (era) {
			case OLD_RELEASE -> "Minecraft 1.6.4";
			case BETA -> "Minecraft Beta 1.7.3";
			case ALPHA -> "Minecraft Alpha v1.2.6";
			case PRE_ALPHA -> "Minecraft Infdev";
			case CLASSIC -> "0.30";
			case CAVEGAME -> "rd-132211";
		};
	}
}
