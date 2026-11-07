package dev.retroeras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** Lets the era's mob rules see what a monster is looking for. */
@Mixin(NearestAttackableTargetGoal.class)
public interface TargetGoalAccessor {
	@Accessor("targetType")
	Class<?> retroeras$targetType();
}
