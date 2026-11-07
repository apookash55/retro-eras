package dev.retroeras.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.retroeras.Era;
import dev.retroeras.client.EraDrawing;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * The old pause menu (GuiIngameMenu): Back to Game, Achievements and Statistics side by side, Options, and quit further down,
 * with none of 26.3's feedback, bug report or player reporting buttons. Eras before Beta had no achievements or statistics.
 */
@Mixin(PauseScreen.class)
abstract class PauseScreenMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void retroeras$oldMenu(CallbackInfo ci) {
		Era era = EraDrawing.currentEra();
		if (era == null) {
			return;
		}
		Screen screen = (Screen) (Object) this;
		int left = screen.width / 2 - 100;
		int top = screen.height / 4 - 16;
		boolean achievements = era == Era.OLD_RELEASE || era == Era.BETA;
		for (GuiEventListener child : screen.children()) {
			if (!(child instanceof AbstractWidget widget)) {
				continue;
			}
			switch (key(widget.getMessage())) {
				case "menu.returnToGame" -> widget.setRectangle(200, 20, left, top + 24);
				case "gui.advancements" -> place(widget, achievements, 98, left, top + 48);
				case "gui.stats" -> place(widget, achievements, 98, left + 102, top + 48);
				case "menu.options" -> widget.setRectangle(era == Era.OLD_RELEASE ? 98 : 200, 20, left, top + 96);
				case "options.worldOptions.button" -> place(widget, era == Era.OLD_RELEASE, 98, left + 102, top + 96);
				case "menu.returnToMenu", "menu.disconnect" -> widget.setRectangle(200, 20, left, top + 120);
				case "menu.game" -> { }
				default -> widget.visible = false;
			}
		}
	}

	private static void place(AbstractWidget widget, boolean shown, int width, int x, int y) {
		widget.visible = shown;
		widget.setRectangle(width, 20, x, y);
	}

	private static String key(Component message) {
		return message.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}
}
