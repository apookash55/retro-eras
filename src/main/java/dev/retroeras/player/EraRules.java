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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Gameplay rules for the eras.
 * <ul>
 * <li>Eras before Beta 1.8 ({@link Era#hasHunger()} false) have no hunger, no sprinting and no natural regeneration;
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
	/** rd-132211 let you place one block, its rock. */
	private static final List<Item> CAVEGAME_KIT = List.of(Items.STONE);
	/**
	 * Classic 0.30's block menu, as many of its 42 blocks as fit: its default hotbar first (stone, cobblestone, bricks, dirt,
	 * planks, log, leaves, glass, slab), then the rest. Its spring green and violet cloth had no modern wool.
	 */
	private static final List<Item> CLASSIC_KIT = List.of(Items.STONE, Items.COBBLESTONE, Items.BRICKS, Items.DIRT, Items.OAK_PLANKS, Items.OAK_LOG,
			Items.OAK_LEAVES, Items.GLASS, Items.SMOOTH_STONE_SLAB, Items.MOSSY_COBBLESTONE, Items.OAK_SAPLING, Items.DANDELION, Items.POPPY,
			Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.SAND, Items.GRAVEL, Items.SPONGE, Items.COAL_ORE, Items.IRON_ORE, Items.GOLD_ORE,
			Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.BOOKSHELF, Items.TNT, Items.OBSIDIAN, Items.WOOL.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.ORANGE),
			Items.WOOL.pick(DyeColor.YELLOW), Items.WOOL.pick(DyeColor.LIME), Items.WOOL.pick(DyeColor.GREEN), Items.WOOL.pick(DyeColor.CYAN),
			Items.WOOL.pick(DyeColor.BLUE), Items.WOOL.pick(DyeColor.PURPLE), Items.WOOL.pick(DyeColor.GRAY), Items.WOOL.pick(DyeColor.WHITE));

	private EraRules() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			Era era = Era.fromDimension(level.dimension());
			if (era == null) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				if (!era.hasHunger()) {
					FoodData food = player.getFoodData();
					food.setFoodLevel(HUNGERLESS_FOOD_LEVEL);
					food.setSaturation(0.0F);
				}
				if (era == Era.CAVEGAME && level.getGameTime() % 20 == 0) {
					restockKit(player, CAVEGAME_KIT);
				} else if (era == Era.CLASSIC && level.getGameTime() % 20 == 0) {
					restockKit(player, CLASSIC_KIT);
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
		Era era = eraOf(player);
		return era == null || era.hasHunger();
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
