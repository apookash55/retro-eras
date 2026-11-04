package dev.retroeras.player;

import java.util.List;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Gameplay rules for the eras.
 * <ul>
 * <li>Every era predates the hunger system (added in Beta 1.8): no hunger, no sprinting, no natural regeneration, and
 * food heals instantly instead (see FoodPropertiesMixin).</li>
 * <li>Eras without survival ({@link Era#isSurvival()}) also have no damage except falling into the void, break blocks
 * instantly without drops, and keep the player's hotbar stocked with that era's building blocks.</li>
 * </ul>
 */
public final class EraRules {
	/**
	 * Food level held constant in hunger-free eras. Vanilla needs more than 6 to sprint and at least 18 to regenerate,
	 * and only starves at 0, so 6 turns all three off.
	 */
	private static final int HUNGERLESS_FOOD_LEVEL = 6;
	private static final Identifier INSTANT_BREAK = RetroEras.id("instant_break");
	/** Cave game's five placeable blocks, on number keys 1-5 just like rd-161348 (grass could not be placed). */
	private static final List<Item> CAVEGAME_KIT = List.of(Items.STONE, Items.DIRT, Items.COBBLESTONE, Items.OAK_PLANKS, Items.OAK_SAPLING);

	private EraRules() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			Era era = Era.fromDimension(level.dimension());
			if (era == null) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				FoodData food = player.getFoodData();
				food.setFoodLevel(HUNGERLESS_FOOD_LEVEL);
				food.setSaturation(0.0F);
				if (era == Era.CAVEGAME && level.getGameTime() % 20 == 0) {
					restockKit(player, CAVEGAME_KIT);
				}
			}
		});

		// Instant breaking is an attribute modifier, so keep it in step with where each player is.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Era era = eraOf(player);
				setInstantBreak(player, era != null && !era.isSurvival());
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Era era = entity instanceof Player player ? eraOf(player) : null;
			return era == null || era.isSurvival() || source.is(DamageTypes.FELL_OUT_OF_WORLD);
		});

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			Era era = eraOf(player);
			if (era == null || era.isSurvival() || level.isClientSide()) {
				return true;
			}
			level.destroyBlock(pos, false, player);
			return false;
		});
	}

	private static @Nullable Era eraOf(Player player) {
		return Era.fromDimension(player.level().dimension());
	}

	public static boolean hasHunger(Player player) {
		return eraOf(player) == null;
	}

	private static void setInstantBreak(ServerPlayer player, boolean enabled) {
		AttributeInstance breakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
		if (breakSpeed == null || breakSpeed.hasModifier(INSTANT_BREAK) == enabled) {
			return;
		}
		if (enabled) {
			breakSpeed.addTransientModifier(new AttributeModifier(INSTANT_BREAK, 1000.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else {
			breakSpeed.removeModifier(INSTANT_BREAK);
		}
	}

	/** Puts a full stack of each kit block in the first hotbar slots, replacing anything else there. */
	private static void restockKit(ServerPlayer player, List<Item> kit) {
		for (int slot = 0; slot < kit.size(); slot++) {
			ItemStack current = player.getInventory().getItem(slot);
			Item item = kit.get(slot);
			if (!current.is(item) || current.getCount() < item.getDefaultMaxStackSize()) {
				player.getInventory().setItem(slot, new ItemStack(item, item.getDefaultMaxStackSize()));
			}
		}
	}
}
