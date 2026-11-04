package dev.retroeras.test;

import java.util.List;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.player.EraItems;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Checks era gameplay in real era dimensions, which only exist with the datapack loaded: weapon damage and the lack of an
 * attack cooldown, newer items being taken away and given back in the modern world, and era drops.
 */
public class EraGameplayClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("effect give @a minecraft:resistance infinite 255 true");
			server.runCommand("gamerule advance_time false");

			// Beta swords dealt 4 + 2 per tier, 1.6.4's 4 + 1, and axes 3 + tier everywhere; 26.3's stone axe deals 9.
			checkHits(context, server, Era.BETA, Items.DIAMOND_SWORD, 10.0F);
			checkHits(context, server, Era.PRE_ALPHA, Items.IRON_SWORD, 8.0F);
			checkHits(context, server, Era.OLD_RELEASE, Items.IRON_SWORD, 6.0F);
			checkHits(context, server, Era.OLD_RELEASE, Items.STONE_AXE, 4.0F);
			checkHits(context, server, null, Items.STONE_AXE, 9.0F);

			check(EraItems.unknownIds().isEmpty(), "Unknown ids in the era item lists: " + EraItems.unknownIds());
			checkNewerItems(context, server);
			checkOreDrops(context, server);
		}
	}

	private static void go(ClientGameTestContext context, TestServerContext server, @Nullable Era era) {
		String dimension = (era == null ? Level.OVERWORLD : era.getDimension()).identifier().toString();
		server.runCommand("execute in " + dimension + " run tp @a 40 200 40");
		context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals(dimension) && client.gui.overlay() == null, 1200);
		server.runCommand("execute in " + dimension + " run spreadplayers 40 40 0 20 false @a");
		context.waitTicks(40);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** Hits an iron golem (no armour, 100 health) twice, 12 ticks apart, and checks both hits deal {@code expected}. */
	private static void checkHits(ClientGameTestContext context, TestServerContext server, @Nullable Era era, Item weapon, float expected) {
		go(context, server, era);
		server.runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon)));
		server.runCommand("execute as @a at @s run summon minecraft:iron_golem ~2 ~ ~ {NoAI:1b,Tags:[\"retroeras_target\"]}");
		// Long enough for 26.3's slowest cooldown, so the first hit is full strength there too.
		context.waitTicks(40);
		int hits = era == null ? 1 : 2;
		for (int hit = 0; hit < hits; hit++) {
			float dealt = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				IronGolem golem = player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(6)).getFirst();
				float before = golem.getHealth();
				player.attack(golem);
				return before - golem.getHealth();
			});
			String where = era == null ? "the overworld" : era.getName();
			RetroEras.LOGGER.info("{} hit {} in {} dealt {}", weapon, hit + 1, where, dealt);
			if (Math.abs(dealt - expected) > 0.01F) {
				throw new AssertionError(weapon + " hit " + (hit + 1) + " in " + where + " dealt " + dealt + ", expected " + expected);
			}
			// Past the golem's hurt cooldown, but well inside 26.3's attack cooldown.
			context.waitTicks(12);
		}
		server.runCommand("kill @e[tag=retroeras_target]");
	}

	/** A netherite sword in Beta is taken away, and comes back in the overworld. */
	private static void checkNewerItems(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			player.getInventory().add(new ItemStack(Items.NETHERITE_SWORD));
			player.getInventory().add(new ItemStack(Items.COBBLESTONE, 10));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 3));
		});
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(!player.getInventory().contains(new ItemStack(Items.NETHERITE_SWORD)), "Beta should take the netherite sword away");
			check(player.getInventory().countItem(Items.COBBLESTONE) == 10, "Beta should leave cobblestone alone");
			check(player.getOffhandItem().isEmpty() && player.getInventory().countItem(Items.TORCH) == 3, "Beta has no offhand: torches should move to the inventory");
			List<ItemStack> returned = player.getAttachedOrCreate(EraItems.RETURNED);
			check(returned.size() == 1 && returned.getFirst().is(Items.NETHERITE_SWORD), "The sword should be kept for the modern inventory, got " + returned);
			check(!EraItems.allowed(Era.BETA, Items.CRAFTER) && EraItems.allowed(Era.OLD_RELEASE, Items.HOPPER) && !EraItems.allowed(Era.BETA, Items.HOPPER),
					"Item lists: crafter is in no era, hopper only in 1.6.4");
		});
		go(context, server, null);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.getInventory().countItem(Items.NETHERITE_SWORD) == 1, "The netherite sword should be back in the overworld");
			check(player.getAttachedOrCreate(EraItems.RETURNED).isEmpty(), "Nothing should be left waiting");
		});
	}

	/** Iron ore in 1.6.4 drops itself, not raw iron. */
	private static void checkOreDrops(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
			BlockPos pos = player.blockPosition().above(2);
			player.level().setBlockAndUpdate(pos, Blocks.IRON_ORE.defaultBlockState());
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos pos = player.blockPosition().above(2);
			check(player.gameMode.destroyBlock(pos), "Breaking the ore should succeed");
			List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
			check(drops.stream().anyMatch(drop -> drop.getItem().is(Items.IRON_ORE)), "1.6.4 iron ore should drop itself, got " + drops);
			check(drops.stream().noneMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "No raw iron in 1.6.4");
		});
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
