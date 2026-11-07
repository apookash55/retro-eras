package dev.retroeras.player;

import java.util.List;

import dev.retroeras.Era;
import dev.retroeras.mixin.MobAccessor;
import dev.retroeras.mixin.TargetGoalAccessor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;

/**
 * How mobs behaved in the eras, set when a mob loads. Mobs before 1.0 went by a simple wander-and-chase routine: this keeps
 * 26.3's pathfinding but takes away what that routine could not do. Animals cannot be tempted, bred or led, sheep do not
 * graze, monsters only want players, cannot break doors, flee the sun, avoid cats or call for help, and notice you from 16
 * blocks. Zombies are never babies. 1.6.4's villagers run on goals, not the brain.
 */
public final class OldMobRules {
	private static final List<ResourceKey<VillagerProfession>> OLD_PROFESSIONS = List.of(VillagerProfession.FARMER, VillagerProfession.LIBRARIAN,
			VillagerProfession.CLERIC, VillagerProfession.ARMORER, VillagerProfession.BUTCHER);

	private OldMobRules() {
	}

	public static void apply(Mob mob, Era era) {
		if (era == Era.OLD_RELEASE) {
			if (mob instanceof Villager villager) {
				oldVillager(villager);
			}
			return;
		}
		MobAccessor goals = (MobAccessor) mob;
		goals.retroeras$goalSelector().removeAllGoals(goal -> goal instanceof TemptGoal || goal instanceof BreedGoal || goal instanceof FollowParentGoal
				|| goal instanceof EatBlockGoal || goal instanceof FleeSunGoal || goal instanceof RestrictSunGoal || goal instanceof BreakDoorGoal
				|| goal instanceof MoveThroughVillageGoal || goal instanceof RemoveBlockGoal
				|| goal instanceof AvoidEntityGoal<?> && mob instanceof Enemy);
		if (mob instanceof Enemy) {
			goals.retroeras$targetSelector().removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?> target
					&& !Player.class.isAssignableFrom(((TargetGoalAccessor) target).retroeras$targetType()));
			setBase(mob, Attributes.FOLLOW_RANGE, 16.0);
			setBase(mob, Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
		}
		if (mob instanceof Zombie zombie && zombie.isBaby()) {
			zombie.setBaby(false);
		}
		if (mob instanceof Enemy) {
			// Monsters had no armour to spawn with, and only skeletons held anything.
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				mob.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
			}
			if (mob instanceof Zombie) {
				mob.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
			}
		}
	}

	private static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	private static void oldVillager(Villager villager) {
		if (villager.level() instanceof net.minecraft.server.level.ServerLevel level) {
			Holder<VillagerProfession> profession = villager.getVillagerData().profession();
			if (profession.unwrapKey().filter(OLD_PROFESSIONS::contains).isEmpty()) {
				villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), OLD_PROFESSIONS.get(villager.getRandom().nextInt(OLD_PROFESSIONS.size()))));
			}
		}
		MobAccessor goals = (MobAccessor) villager;
		if (!goals.retroeras$goalSelector().getAvailableGoals().isEmpty()) {
			return;
		}
		goals.retroeras$goalSelector().addGoal(0, new FloatGoal(villager));
		goals.retroeras$goalSelector().addGoal(1, new AvoidEntityGoal<>(villager, Zombie.class, 8.0F, 0.6, 0.6));
		goals.retroeras$goalSelector().addGoal(1, new TradeWithPlayerGoal(villager));
		goals.retroeras$goalSelector().addGoal(1, new LookAtTradingPlayerGoal(villager));
		goals.retroeras$goalSelector().addGoal(4, new OpenDoorGoal(villager, true));
		goals.retroeras$goalSelector().addGoal(9, new LookAtPlayerGoal(villager, Player.class, 3.0F, 1.0F));
		goals.retroeras$goalSelector().addGoal(9, new WaterAvoidingRandomStrollGoal(villager, 0.6));
		goals.retroeras$goalSelector().addGoal(10, new RandomLookAroundGoal(villager));
	}
}
