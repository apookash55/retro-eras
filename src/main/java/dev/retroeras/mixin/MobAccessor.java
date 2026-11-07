package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/** Lets the era's mob rules change a mob's goals. */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("goalSelector")
	GoalSelector retroeras$goalSelector();

	@Accessor("targetSelector")
	GoalSelector retroeras$targetSelector();
}
