package dev.retroeras.player;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Old mob rules. Before Beta 1.8 animals could not be bred (see AnimalMixin). Before shears came in Beta 1.7 you got
 * wool by punching a sheep, and before dyes came in Beta 1.2 every sheep was white, so in Alpha and Infdev hitting a
 * sheep knocks one to three white wool off it and sheep spawn white.
 */
public final class EraMobs {
	private EraMobs() {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof Sheep sheep && entity.level() instanceof ServerLevel server && punchesWool(Era.fromDimension(server.dimension()))
					&& source.getEntity() instanceof LivingEntity && !sheep.isSheared() && !sheep.isBaby()) {
				sheep.setSheared(true);
				int count = 1 + sheep.getRandom().nextInt(3);
				for (int i = 0; i < count; i++) {
					ItemEntity wool = sheep.spawnAtLocation(server, new ItemStack(Items.WOOL.pick(sheep.getColor())), 1.0F);
					if (wool != null) {
						wool.setDeltaMovement(wool.getDeltaMovement().add((sheep.getRandom().nextFloat() - sheep.getRandom().nextFloat()) * 0.1F,
								sheep.getRandom().nextFloat() * 0.05F, (sheep.getRandom().nextFloat() - sheep.getRandom().nextFloat()) * 0.1F));
					}
				}
			}
			return true;
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Sheep sheep && onlyWhiteSheep(Era.fromDimension(level.dimension())) && sheep.getColor() != DyeColor.WHITE) {
				sheep.setColor(DyeColor.WHITE);
			}
			Era era = Era.fromDimension(level.dimension());
			if (era != null && entity instanceof net.minecraft.world.entity.Mob mob) {
				OldMobRules.apply(mob, era);
			}
		});
	}

	/** Whether animals could be bred: from Beta 1.8, so only Old Release among the eras. */
	public static boolean canBreed(Level level) {
		Era era = Era.fromDimension(level.dimension());
		return era == null || era.hasHunger();
	}

	/** Alpha and Infdev had no shears: sheep dropped their wool when hit. */
	private static boolean punchesWool(@Nullable Era era) {
		return era == Era.ALPHA || era == Era.PRE_ALPHA;
	}

	/** Coloured sheep and dyes came in Beta 1.2. */
	private static boolean onlyWhiteSheep(@Nullable Era era) {
		return era == Era.ALPHA || era == Era.PRE_ALPHA;
	}
}
