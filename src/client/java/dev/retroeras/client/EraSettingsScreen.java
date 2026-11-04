package dev.retroeras.client;

import java.util.ArrayList;
import java.util.List;

import dev.retroeras.Era;

import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * Options &gt; Retro Eras: each era's fog and whether the version name is shown. Changes are saved to
 * config/retroeras.properties straight away, the same file the Render Distance key writes to.
 */
public class EraSettingsScreen extends OptionsSubScreen {
	public static final Component TITLE = Component.translatable("retroeras.settings");

	public EraSettingsScreen(Screen lastScreen, Options options) {
		super(lastScreen, options, TITLE);
	}

	@Override
	protected void addOptions() {
		if (list == null) {
			return;
		}
		list.addHeader(Component.translatable("retroeras.settings.fog"));
		List<AbstractWidget> fog = new ArrayList<>();
		for (Era era : Era.values()) {
			fog.add(CycleButton.<EraFog.Distance>builder(distance -> Component.literal(distance.label()), EraFog.distance(era))
					.withValues(List.of(EraFog.Distance.values()))
					.withTooltip(distance -> Tooltip.create(Component.translatable("retroeras.settings.fog.tooltip")))
					.create(Component.translatable("retroeras.era." + era.getName()), (button, distance) -> EraFog.setDistance(era, distance)));
		}
		list.addSmall(fog);

		list.addHeader(Component.translatable("retroeras.settings.interface"));
		list.addSmall(List.of(CycleButton.onOffBuilder(EraHud.showsVersion())
				.withTooltip(show -> Tooltip.create(Component.translatable("retroeras.settings.version.tooltip")))
				.create(Component.translatable("retroeras.settings.version"), (button, show) -> EraHud.setShowVersion(show))));
	}
}
