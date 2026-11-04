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

import java.util.Optional;

import dev.retroeras.entity.ModEntities;
import dev.retroeras.worldgen.release.ReleaseChunkGenerator;
import dev.retroeras.worldgen.release.structure.LegacyBox;
import dev.retroeras.worldgen.release.structure.LegacyStart;
import dev.retroeras.worldgen.release.structure.LegacyStructureFeature;
import dev.retroeras.worldgen.release.structure.ReleaseStructures;
import dev.retroeras.worldgen.release.structure.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
			checkSwordBlocking(context, server);
			checkFurnace(context, server);
			checkCaveGame(context, server);
			checkStructures(context, server);
		}
	}

	/** In Old Release, a raised sword cuts a 6-point hit to (1 + 6) / 2. */
	private static void checkSwordBlocking(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		server.runCommand("effect clear @a minecraft:resistance");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.startUsingItem(InteractionHand.MAIN_HAND);
			check(player.isUsingItem(), "Right-clicking a sword in Old Release should raise it");
			player.setHealth(20.0F);
			player.hurtServer(player.level(), player.damageSources().cactus(), 6.0F);
			check(Math.abs(player.getHealth() - 16.5F) < 0.01F, "A blocked 6-point hit should cost 3.5, health is " + player.getHealth());
			player.stopUsingItem();
		});
		server.runCommand("effect give @a minecraft:resistance infinite 255 true");
	}

	/** A Beta furnace will not smelt stone into smooth stone, which Beta did not have. */
	private static void checkFurnace(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		BlockPos furnace = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos pos = player.blockPosition().above(3);
			player.level().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
			if (player.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity entity) {
				entity.setItem(0, new ItemStack(Items.STONE));
				entity.setItem(1, new ItemStack(Items.COAL));
			}
			return pos;
		});
		context.waitTicks(260);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().getBlockEntity(furnace) instanceof AbstractFurnaceBlockEntity entity && entity.getItem(2).isEmpty()
					&& entity.getItem(0).is(Items.STONE), "A Beta furnace should leave the stone alone");
		});
	}

	/** Cave game keeps ten zombies wandering, and its saplings never grow into trees. */
	private static void checkCaveGame(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.CAVEGAME);
		context.waitTicks(220);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = player.level();
			int zombies = level.getEntities(ModEntities.CAVE_ZOMBIE, zombie -> true).size();
			check(zombies >= 1, "Cave game should spawn its zombies, found " + zombies);
			BlockPos pos = player.blockPosition();
			level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(pos, Blocks.OAK_SAPLING.defaultBlockState());
			((SaplingBlock) Blocks.OAK_SAPLING).advanceTree(level, pos, level.getBlockState(pos), level.getRandom());
			check(level.getBlockState(pos).is(Blocks.OAK_SAPLING), "A Cave game sapling should never grow");
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		});
	}

	/** Visits a 1.6.4 village and stronghold where the generator says they are, checks they were built, and takes pictures. */
	private static void checkStructures(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		BlockPos[] targets = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(Era.OLD_RELEASE.getDimension());
			ReleaseStructures structures = ((ReleaseChunkGenerator) level.getChunkSource().getGenerator()).structures(level.getSeed());
			BlockPos village = null;
			for (LegacyStructureFeature feature : structures.features()) {
				if (!(feature instanceof Village)) {
					continue;
				}
				search:
				for (int radius = 0; radius <= 64; radius++) {
					for (int chunkX = -radius; chunkX <= radius; chunkX++) {
						for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
							if (Math.max(Math.abs(chunkX), Math.abs(chunkZ)) != radius) {
								continue;
							}
							Optional<LegacyStart> start = feature.startAt(chunkX, chunkZ);
							if (start.isPresent() && start.get().isValid()) {
								LegacyBox box = start.get().pieces().getFirst().box();
								village = new BlockPos(box.centerX(), 0, box.centerZ());
								break search;
							}
						}
					}
				}
			}
			return new BlockPos[] {village, structures.stronghold().nearest(BlockPos.ZERO)};
		});
		check(targets[0] != null, "No 1.6.4 village within 64 chunks of the origin");
		check(targets[1] != null, "No 1.6.4 stronghold");

		String dimension = Era.OLD_RELEASE.getDimension().identifier().toString();
		BlockPos village = targets[0];
		server.runCommand("execute in " + dimension + " run tp @a " + village.getX() + " 120 " + village.getZ() + " 45 50");
		context.waitTicks(200);
		int[] found = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(Era.OLD_RELEASE.getDimension());
			int villagers = level.getEntitiesOfClass(Villager.class, new AABB(village).inflate(96, 128, 96)).size();
			int built = 0;
			for (BlockPos pos : BlockPos.betweenClosed(village.offset(-24, 50, -24), village.offset(24, 110, 24))) {
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.GLASS_PANE) || state.is(Blocks.OAK_DOOR) || state.is(Blocks.GRAVEL) || state.is(Blocks.WATER) && pos.getY() > 63) {
					built++;
				}
			}
			return new int[] {villagers, built};
		});
		RetroEras.LOGGER.info("1.6.4 village at {}: {} villagers, {} village blocks", village, found[0], found[1]);
		takeScreenshot(context, "retroeras_old_release_village");
		check(found[0] > 0, "The village at " + village + " should have villagers");
		check(found[1] > 20, "The village at " + village + " should have roads, doors and windows, found " + found[1]);

		BlockPos portal = targets[1];
		server.runCommand("execute in " + dimension + " run tp @a " + portal.getX() + " " + (portal.getY() + 1) + " " + portal.getZ());
		context.waitTicks(200);
		int frames = server.computeOnServer(s -> {
			ServerLevel level = s.getLevel(Era.OLD_RELEASE.getDimension());
			int count = 0;
			for (BlockPos pos : BlockPos.betweenClosed(portal.offset(-12, -8, -12), portal.offset(12, 8, 12))) {
				if (level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) {
					count++;
				}
			}
			return count;
		});
		RetroEras.LOGGER.info("1.6.4 stronghold portal room at {}: {} portal frames", portal, frames);
		takeScreenshot(context, "retroeras_old_release_stronghold");
		check(frames == 12, "The stronghold portal room should have 12 portal frames, found " + frames);
	}

	private static void takeScreenshot(ClientGameTestContext context, String name) {
		RetroEras.LOGGER.info("Saved screenshot {}", context.takeScreenshot(name).toAbsolutePath());
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
