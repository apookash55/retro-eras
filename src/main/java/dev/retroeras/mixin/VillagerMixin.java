package dev.retroeras.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.retroeras.Era;
import dev.retroeras.player.OldVillagerTrades;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

/**
 * Villagers in 1.6.4 ran on plain goals, not today's brain, so they do not sleep, claim job blocks or gossip. They have no
 * levels or experience either: using a villager's newest trade makes it add another and restock two seconds later, with the
 * old burst of regeneration. See OldVillagerTrades and OldMobRules.
 */
@Mixin(Villager.class)
abstract class VillagerMixin {
	@Unique
	private int retroeras$restockIn;

	@Unique
	private boolean retroeras$inOldRelease() {
		Level level = ((Villager) (Object) this).level();
		return Era.fromDimension(level.dimension()) == Era.OLD_RELEASE;
	}

	@Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
	private void retroeras$oldTrades(ServerLevel level, CallbackInfo ci) {
		if (retroeras$inOldRelease()) {
			ci.cancel();
			retroeras$addTrade(level);
		}
	}

	@Unique
	private void retroeras$addTrade(ServerLevel level) {
		Villager villager = (Villager) (Object) this;
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		ResourceKey<VillagerProfession> key = profession.unwrapKey().orElse(VillagerProfession.FARMER);
		OldVillagerTrades.add(key, villager.getOffers(), 1, new Random(villager.getRandom().nextLong()), level.registryAccess());
	}

	@Inject(method = "rewardTradeXp", at = @At("HEAD"), cancellable = true)
	private void retroeras$noExperience(MerchantOffer offer, CallbackInfo ci) {
		if (retroeras$inOldRelease()) {
			ci.cancel();
			var offers = ((Villager) (Object) this).getOffers();
			if (!offers.isEmpty() && offers.getLast() == offer) {
				retroeras$restockIn = 40;
			}
		}
	}

	@WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
	private void retroeras$noBrain(Brain<?> brain, ServerLevel level, LivingEntity entity, Operation<Void> original) {
		if (!retroeras$inOldRelease()) {
			original.call(brain, level, entity);
			return;
		}
		Villager villager = (Villager) (Object) this;
		if (retroeras$restockIn > 0 && !villager.isTrading() && --retroeras$restockIn == 0) {
			OldVillagerTrades.restock(villager.getOffers());
			retroeras$addTrade(level);
			villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
		}
	}
}
