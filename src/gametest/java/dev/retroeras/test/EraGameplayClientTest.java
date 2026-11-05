package dev.retroeras.test;

import java.util.List;

import org.jspecify.annotations.Nullable;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;
import dev.retroeras.player.EraItems;
import dev.retroeras.client.EraFog;
import dev.retroeras.client.EraSettingsScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonInfo;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.Optional;

import dev.retroeras.entity.ModEntities;
import dev.retroeras.player.EraLayers;
import dev.retroeras.worldgen.release.ReleaseChunkGenerator;
import dev.retroeras.worldgen.release.structure.LegacyBox;
import dev.retroeras.worldgen.release.structure.LegacyStart;
import dev.retroeras.worldgen.release.structure.LegacyStructureFeature;
import dev.retroeras.worldgen.release.structure.ReleaseStructures;
import dev.retroeras.worldgen.release.structure.Village;
import dev.retroeras.worldgen.nether.NetherChunkGenerator;
import dev.retroeras.worldgen.release.structure.Fortress;
import net.minecraft.resources.ResourceKey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import dev.retroeras.player.EraAchievements;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
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
			checkHits(context, server, Era.ALPHA, Items.STONE_SWORD, 6.0F);
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
			checkLayers(context, server);
			checkVanillaPortals(context, server);
			checkSettingsScreen(context);
			checkEraNether(context, server);
			checkSheep(context, server);
			nightScreenshot(context, server, Era.BETA);
			nightScreenshot(context, server, null);
			checkBreeding(context, server, Era.BETA, false);
			checkBreeding(context, server, Era.OLD_RELEASE, true);
			checkAchievements(context, server);
			villagerScreenshot(context, server);
			checkOldRules(context, server);
			checkWitchHut(context, server);
			checkClassicWater(context, server);
		}
	}

	/**
	 * The eras are stacked: only the very lowest bedrock can be mined, with a diamond pickaxe; falling out of the bottom of
	 * Beta lands in Alpha's sky, and climbing out of the top of Alpha comes up just above Beta's bedrock.
	 */
	private static void checkLayers(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			int bottom = player.level().getMinY();
			BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
			Float lowest = EraLayers.bedrockProgress(bedrock, player, new BlockPos(0, bottom, 0));
			check(lowest != null && lowest > 0.003F && lowest < 0.01F, "Bottom bedrock should take about as long as obsidian, progress " + lowest);
			check(EraLayers.bedrockProgress(bedrock, player, new BlockPos(0, bottom + 1, 0)) == null, "Bedrock above the bottom layer stays unbreakable");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
			Float iron = EraLayers.bedrockProgress(bedrock, player, new BlockPos(0, bottom, 0));
			check(iron != null && iron == 0.0F, "An iron pickaxe should not break bedrock, progress " + iron);
		});

		server.runCommand("execute in " + Era.BETA.getDimension().identifier() + " run tp @a 40 -30 40");
		String alpha = Era.ALPHA.getDimension().identifier().toString();
		context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals(alpha), 200);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().dimension() == Era.ALPHA.getDimension(), "Falling out of Beta should land in Alpha");
			check(player.getY() > 64 && EraLayers.isFallingIn(player), "Should arrive in Alpha's sky with a soft landing, at y=" + player.getY());
		});
		context.waitFor(client -> client.player != null && client.player.onGround(), 1200);
		context.waitTicks(10);
		server.runOnServer(s -> check(!EraLayers.isFallingIn(player(s)), "The soft landing should be used up once on the ground"));

		server.runCommand("execute in " + alpha + " run tp @a 40 135 40");
		String beta = Era.BETA.getDimension().identifier().toString();
		context.waitFor(client -> client.level != null && client.level.dimension().identifier().toString().equals(beta), 200);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().dimension() == Era.BETA.getDimension(), "Climbing out of Alpha should come up in Beta");
			BlockPos feet = player.blockPosition();
			check(feet.getY() < 40, "Should come up near Beta's bedrock, at " + feet);
			check(player.level().getBlockState(feet).isAir() && player.level().getBlockState(feet.above()).isAir(), "Should come up in an air pocket at " + feet);
			RetroEras.LOGGER.info("Came up from Alpha into Beta at {}", feet);
		});
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

	/** rd-132211's Cave game: you stand on its flat grass at y = 42, with no zombies, holding its one block. */
	private static void checkCaveGame(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.CAVEGAME);
		context.waitTicks(220);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = player.level();
			int zombies = level.getEntities(ModEntities.CAVE_ZOMBIE, zombie -> true).size();
			check(zombies == 0, "rd-132211 had no zombies, found " + zombies);
			check(player.blockPosition().getY() == 43, "The player should stand on the flat grass at y = 42, is at " + player.blockPosition());
			check(level.getBlockState(player.blockPosition().below()).is(Blocks.GRASS_BLOCK), "The top of the Cave game slab should be grass");
			check(player.getInventory().getItem(0).is(Items.STONE) && player.getInventory().getItem(1).isEmpty(),
					"Cave game should hand out its one block, stone, and nothing else");
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

	/**
	 * End portals inside an era lead nowhere, rather than out to the modern End, and so do Nether portals in Infdev,
	 * which had no Nether.
	 */
	private static void checkVanillaPortals(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		BlockPos feet = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos pos = player.blockPosition();
			player.level().setBlock(pos, Blocks.END_PORTAL.defaultBlockState(), 2);
			return pos;
		});
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().dimension() == Era.OLD_RELEASE.getDimension(), "An End portal in Old Release should not lead out, player is in " + player.level().dimension());
			player.level().setBlock(feet, Blocks.AIR.defaultBlockState(), 2);
		});

		go(context, server, Era.PRE_ALPHA);
		BlockPos infdevFeet = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos pos = player.blockPosition();
			player.level().setBlock(pos, Blocks.NETHER_PORTAL.defaultBlockState(), 2);
			player.level().setBlock(pos.above(), Blocks.NETHER_PORTAL.defaultBlockState(), 2);
			return pos;
		});
		context.waitTicks(120);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().dimension() == Era.PRE_ALPHA.getDimension(), "A Nether portal in Infdev should not lead out, player is in " + player.level().dimension());
			player.level().setBlock(infdevFeet, Blocks.AIR.defaultBlockState(), 2);
			player.level().setBlock(infdevFeet.above(), Blocks.AIR.defaultBlockState(), 2);
		});
	}

	/**
	 * Fire lights an obsidian portal in Beta, and it leads to Beta's own Nether, where the Beta inventory comes along. Then
	 * a look inside a 1.6.4 nether fortress.
	 */
	private static void checkEraNether(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		BlockPos inside = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			player.getInventory().add(new ItemStack(Items.GLOWSTONE_DUST, 7));
			BlockPos base = player.blockPosition().offset(2, 0, 0);
			ServerLevel level = (ServerLevel) player.level();
			for (int dz = -1; dz <= 2; dz++) {
				for (int dy = -1; dy <= 3; dy++) {
					boolean frame = dz == -1 || dz == 2 || dy == -1 || dy == 3;
					level.setBlockAndUpdate(base.offset(0, dy, dz), frame ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(base, Blocks.FIRE.defaultBlockState());
			check(level.getBlockState(base).is(Blocks.NETHER_PORTAL), "Fire in an obsidian frame should light a Nether portal in Beta");
			return base;
		});
		server.runOnServer(s -> player(s).teleportTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5));
		ResourceKey<Level> betaNether = Era.BETA.getNether();
		context.waitFor(client -> client.level != null && client.level.dimension() == betaNether, 400);
		context.waitTicks(40);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().dimension() == betaNether, "A Beta portal should lead to Beta's Nether, player is in " + player.level().dimension());
			check(player.getInventory().countItem(Items.GLOWSTONE_DUST) == 7, "The Beta inventory should come along to Beta's Nether");
			check(player.level().getChunkSource().getGenerator() instanceof NetherChunkGenerator, "Beta's Nether should use the old Nether generator");
		});
		takeScreenshot(context, "beta_nether");

		// Stand on a 1.6.4 fortress, high in the air so the camera sees it from above.
		ServerLevel releaseNether = server.computeOnServer(s -> s.getLevel(Era.OLD_RELEASE.getNether()));
		BlockPos fortress = server.computeOnServer(s -> {
			Fortress fortresses = ((NetherChunkGenerator) releaseNether.getChunkSource().getGenerator()).fortress(releaseNether.getSeed());
			for (int radius = 0; radius < 64; radius++) {
				for (int chunkX = -radius; chunkX <= radius; chunkX++) {
					for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
						Optional<LegacyStart> start = fortresses.startAt(chunkX, chunkZ);
						if (start.isPresent()) {
							LegacyBox box = start.get().pieces().getFirst().box();
							return new BlockPos(box.centerX(), box.maxY + 1, box.centerZ());
						}
					}
				}
			}
			throw new AssertionError("No 1.6.4 fortress within 64 chunks");
		});
		server.runCommand("execute in " + Era.OLD_RELEASE.getNether().identifier() + " run tp @a " + fortress.getX() + " " + fortress.getY() + " " + fortress.getZ()
				+ " 30 40");
		context.waitFor(client -> client.level != null && client.level.dimension() == Era.OLD_RELEASE.getNether() && client.gui.overlay() == null, 1200);
		context.waitTicks(100);
		server.runOnServer(s -> {
			ServerLevel level = s.getLevel(Era.OLD_RELEASE.getNether());
			int bricks = 0;
			for (BlockPos pos : BlockPos.betweenClosed(fortress.offset(-12, -6, -12), fortress.offset(12, 2, 12))) {
				if (level.getBlockState(pos).is(Blocks.NETHER_BRICKS)) {
					bricks++;
				}
			}
			check(bricks > 100, "A 1.6.4 fortress should be built of nether brick, found " + bricks + " bricks around " + fortress);
		});
		takeScreenshot(context, "old_release_fortress");
	}

	/** Options has a "Retro Eras..." button opening the settings screen, and changing the fog there takes effect. */
	private static void checkSettingsScreen(ClientGameTestContext context) {
		EraFog.Distance before = EraFog.distance(Era.BETA);
		context.setScreen(() -> new OptionsScreen(null, Minecraft.getInstance().options));
		takeScreenshot(context, "options");
		context.clickScreenButton("retroeras.settings.button");
		context.waitForScreen(EraSettingsScreen.class);
		takeScreenshot(context, "settings");
		// The fog buttons sit inside the options list, where clickScreenButton does not look.
		boolean pressed = context.computeOnClient(client -> pressCycleButton(client.gui.screen(), "Beta 1.7.3"));
		check(pressed, "The settings screen should have a Beta 1.7.3 fog button");
		check(EraFog.distance(Era.BETA) != before, "Clicking Beta's fog button should change its fog from " + before);
		context.runOnClient(client -> EraFog.setDistance(Era.BETA, before));
		context.setScreen(() -> null);
	}

	/** A torch-lit spot at midnight, to compare Beta's grey lightmap with 26.3's warm and blue one. */
	private static void nightScreenshot(ClientGameTestContext context, TestServerContext server, @Nullable Era era) {
		go(context, server, era);
		server.runCommand("time set midnight");
		server.runCommand("execute as @a at @s run setblock ^ ^ ^3 minecraft:torch");
		server.runCommand("execute as @a at @s run setblock ^2 ^ ^5 minecraft:torch");
		context.waitTicks(40);
		takeScreenshot(context, (era == null ? "overworld" : era.getName()) + "_night");
		server.runCommand("time set noon");
	}

	private static boolean done(MinecraftServer server, String id) {
		AdvancementHolder holder = server.getAdvancements().get(Identifier.parse(id));
		check(holder != null, "No advancement " + id);
		return player(server).getAdvancements().getOrStartProgress(holder).isDone();
	}

	/**
	 * Beta's achievements: opening the inventory gives "Taking Inventory", an achievement cannot come before its parent,
	 * 26.3's advancements are not earned in an era, and nothing is earned in Alpha, which had no achievements.
	 */
	private static void checkAchievements(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		server.runCommand("advancement revoke @a everything");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			EraAchievements.onCraft(player, new ItemStack(Items.CRAFTING_TABLE));
			check(!done(s, "retroeras:beta/build_work_bench"), "Benchmarking should need Getting Wood first");
		});
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.setScreen(() -> null);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(done(s, "retroeras:beta/open_inventory"), "Opening the inventory in Beta should give Taking Inventory");
			ItemEntity log = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.OAK_LOG));
			player.level().addFreshEntity(log);
		});
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(done(s, "retroeras:beta/mine_wood"), "Picking up wood in Beta should give Getting Wood");
			// Holding a crafting table is what 26.3's first advancement asks for.
			player.getInventory().add(new ItemStack(Items.CRAFTING_TABLE));
			EraAchievements.onCraft(player, new ItemStack(Items.CRAFTING_TABLE));
			check(done(s, "retroeras:beta/build_work_bench"), "Crafting a workbench after Getting Wood should give Benchmarking");
			check(!done(s, "minecraft:story/root"), "26.3's advancements should not be earned in Beta");
			check(!done(s, "retroeras:old_release/open_inventory"), "1.6.4's achievements should not be earned in Beta");
		});
		context.setScreen(() -> new AdvancementsScreen(Minecraft.getInstance().player.connection.getAdvancements()));
		context.waitTicks(10);
		takeScreenshot(context, "beta_achievements");
		context.setScreen(() -> null);

		go(context, server, Era.ALPHA);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.setScreen(() -> null);
		server.runOnServer(s -> check(!done(s, "retroeras:old_release/open_inventory") && !done(s, "minecraft:story/root"),
				"Nothing should be earned in Alpha, which had no achievements"));
		server.runCommand("advancement revoke @a everything");
	}

	/**
	 * Beta: no experience orbs, a bow that fires on click, bread that does not stack, and armour by the old rules; 1.6.4's
	 * armour; and no rain in Alpha.
	 */
	private static void checkOldRules(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.BETA);
		server.runCommand("effect clear @a minecraft:resistance");
		server.runCommand("execute as @a at @s run summon minecraft:experience_orb ~ ~1 ~ {Value:10,Tags:[\"retroeras_target\"]}");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			player.getInventory().setItem(0, new ItemStack(Items.BOW));
			player.getInventory().setItem(1, new ItemStack(Items.ARROW, 10));
			player.getInventory().setItem(2, new ItemStack(Items.BREAD, 5));
			player.getInventory().setSelectedSlot(0);
			Items.BOW.use(player.level(), player, InteractionHand.MAIN_HAND);
			check(!player.isUsingItem(), "A Beta bow should fire at once, not be drawn back");
			check(player.getInventory().getItem(1).getCount() == 9, "A Beta bow shot should use one arrow");
			check(!player.level().getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(4)).isEmpty(), "A Beta bow shot should fire an arrow");

			// Full iron armour, unworn: Beta counted 20 points for any full set, so a 10-point hit comes to 2.
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
			player.setHealth(20.0F);
			player.hurtServer(player.level(), player.damageSources().cactus(), 10.0F);
			check(Math.abs(player.getHealth() - 18.0F) < 0.01F, "Beta armour should cut a 10-point hit to 2, health is " + player.getHealth());
		});
		context.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.level().getEntitiesOfClass(ExperienceOrb.class, player.getBoundingBox().inflate(6)).isEmpty(), "Beta had no experience orbs");
			int breadSlots = 0;
			for (int slot = 0; slot < 36; slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (stack.is(Items.BREAD)) {
					check(stack.getCount() == 1, "Bread did not stack in Beta, found a stack of " + stack.getCount());
					breadSlots++;
				}
			}
			check(breadSlots == 5, "Five bread should take five slots in Beta, took " + breadSlots);
			player.getInventory().clearContent();
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
		});
		server.runCommand("kill @e[type=minecraft:arrow]");

		go(context, server, Era.OLD_RELEASE);
		server.runCommand("effect clear @a minecraft:resistance");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			// Full iron is 15 points in 1.6.4, 4% each and no toughness: a 10-point hit comes to 4.
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
			player.setHealth(20.0F);
		});
		// 26.3 counts newly worn armour's points on the next tick.
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.hurtServer(player.level(), player.damageSources().cactus(), 10.0F);
			check(Math.abs(player.getHealth() - 16.0F) < 0.01F, "1.6.4 armour should cut a 10-point hit to 4, health is " + player.getHealth());
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
			player.setHealth(20.0F);
		});
		server.runCommand("effect give @a minecraft:resistance infinite 255 true");

		go(context, server, Era.ALPHA);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(!player.level().getBiome(player.blockPosition()).value().hasPrecipitation(), "Alpha had no rain or snowfall");
		});
	}

	/** Inside a 1.6.4 witch hut only witches spawn. */
	private static void checkWitchHut(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		server.runOnServer(s -> {
			ServerLevel level = s.getLevel(Era.OLD_RELEASE.getDimension());
			ReleaseChunkGenerator generator = (ReleaseChunkGenerator) level.getChunkSource().getGenerator();
			ReleaseStructures structures = generator.structures(level.getSeed());
			BlockPos hut = null;
			search:
			for (int chunkX = -256; chunkX < 256; chunkX += 1) {
				for (int chunkZ = -256; chunkZ < 256; chunkZ += 1) {
					Optional<LegacyStart> start = structures.temple().startAt(chunkX, chunkZ);
					if (start.isPresent()) {
						LegacyBox box = start.get().pieces().getFirst().box();
						BlockPos centre = new BlockPos(box.centerX(), box.centerY(), box.centerZ());
						if (structures.temple().inWitchHut(centre)) {
							hut = centre;
							break search;
						}
					}
				}
			}
			check(hut != null, "No 1.6.4 witch hut within 256 chunks");
			var mobs = generator.getMobsAt(level, level.structureManager(), MobCategory.MONSTER, hut);
			check(mobs.unwrap().size() == 1 && mobs.unwrap().getFirst().value().type() == EntityTypes.WITCH, "Only witches should spawn in a witch hut, got " + mobs);
			RetroEras.LOGGER.info("1.6.4 witch hut at {}", hut);
		});
	}

	/** Classic's water spreads without end: a spring on a long ledge runs much further than 26.3's seven blocks. */
	private static void checkClassicWater(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.CLASSIC);
		BlockPos start = server.computeOnServer(s -> {
			ServerPlayer player = player(s);
			BlockPos base = new BlockPos(player.blockPosition().getX(), 60, player.blockPosition().getZ());
			for (int i = -1; i <= 16; i++) {
				player.level().setBlockAndUpdate(base.offset(i, -1, 0), Blocks.STONE.defaultBlockState());
			}
			player.level().setBlockAndUpdate(base, Blocks.WATER.defaultBlockState());
			return base;
		});
		context.waitTicks(200);
		server.runOnServer(s -> {
			ServerLevel level = s.getLevel(Era.CLASSIC.getDimension());
			check(level.getFluidState(start.offset(12, 0, 0)).is(net.minecraft.tags.FluidTags.WATER),
					"Classic water should spread more than 7 blocks along a ledge");
			for (int i = -1; i <= 16; i++) {
				level.setBlockAndUpdate(start.offset(i, -1, 0), Blocks.AIR.defaultBlockState());
			}
		});
	}

	/** 1.6.4's five villager jobs in their old outfits, and a baby farmer as a small adult. */
	private static void villagerScreenshot(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.OLD_RELEASE);
		server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 10");
		String[] jobs = {"farmer", "librarian", "cleric", "armorer", "butcher", "nitwit"};
		for (int i = 0; i < jobs.length; i++) {
			server.runCommand("execute as @a at @s run summon minecraft:villager ^" + (i - 2.5) * 1.2 + " ^ ^4 {NoAI:1b,Rotation:[180f,0f],Tags:[\"retroeras_target\"],"
					+ "VillagerData:{profession:\"minecraft:" + jobs[i] + "\",level:1,type:\"minecraft:plains\"}}");
		}
		server.runCommand("execute as @a at @s run summon minecraft:villager ^ ^ ^2.5 {NoAI:1b,Age:-24000,Rotation:[180f,0f],Tags:[\"retroeras_target\"],"
				+ "VillagerData:{profession:\"minecraft:farmer\",level:1,type:\"minecraft:plains\"}}");
		context.waitTicks(40);
		takeScreenshot(context, "old_release_villagers");
		server.runCommand("kill @e[tag=retroeras_target]");
	}

	/** Presses the first cycle button under {@code parent} whose label starts with {@code name}. */
	private static boolean pressCycleButton(GuiEventListener parent, String name) {
		if (parent instanceof CycleButton<?> button && button.getMessage().getString().startsWith(name)) {
			button.onPress(new MouseButtonInfo(0, 0));
			return true;
		}
		if (parent instanceof ContainerEventHandler container) {
			for (GuiEventListener child : container.children()) {
				if (pressCycleButton(child, name)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Alpha's sheep are all white, and hitting one knocks one to three wool off it. */
	private static void checkSheep(ClientGameTestContext context, TestServerContext server) {
		go(context, server, Era.ALPHA);
		server.runCommand("execute as @a at @s run summon minecraft:sheep ~2 ~ ~ {NoAI:1b,Color:14b,Tags:[\"retroeras_target\"]}");
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			Sheep sheep = player.level().getEntitiesOfClass(Sheep.class, player.getBoundingBox().inflate(6)).getFirst();
			check(sheep.getColor() == DyeColor.WHITE, "Alpha sheep should be white, this one is " + sheep.getColor());
			player.attack(sheep);
			check(sheep.isSheared(), "Hitting an Alpha sheep should knock its wool off");
			int wool = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6)).stream()
					.filter(item -> item.getItem().is(Items.WOOL.pick(DyeColor.WHITE))).mapToInt(item -> item.getItem().getCount()).sum();
			check(wool >= 1 && wool <= 3, "Hitting an Alpha sheep should drop 1 to 3 white wool, dropped " + wool);
		});
		server.runCommand("kill @e[tag=retroeras_target]");
		server.runCommand("kill @e[type=minecraft:item]");
	}

	/** Breeding came in Beta 1.8: a Beta cow ignores wheat, a 1.6.4 cow falls in love. */
	private static void checkBreeding(ClientGameTestContext context, TestServerContext server, Era era, boolean breeds) {
		go(context, server, era);
		server.runCommand("execute as @a at @s run summon minecraft:cow ~2 ~ ~ {NoAI:1b,Tags:[\"retroeras_target\"]}");
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
			Cow cow = player.level().getEntitiesOfClass(Cow.class, player.getBoundingBox().inflate(6)).getFirst();
			cow.mobInteract(player, InteractionHand.MAIN_HAND);
			check(cow.isInLove() == breeds, "A " + era.getName() + " cow " + (breeds ? "should" : "should not") + " fall in love with wheat");
			check(player.getMainHandItem().getCount() == (breeds ? 3 : 4), "Wheat used in " + era.getName() + ": " + (4 - player.getMainHandItem().getCount()));
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		server.runCommand("kill @e[tag=retroeras_target]");
	}

	private static void takeScreenshot(ClientGameTestContext context, String name) {
		RetroEras.LOGGER.info("Saved screenshot {}", context.takeScreenshot(name).toAbsolutePath());
	}

	private static void go(ClientGameTestContext context, TestServerContext server, @Nullable Era era) {
		String dimension = (era == null ? Level.OVERWORLD : era.getDimension()).identifier().toString();
		server.runCommand("execute in " + dimension + " run tp @a 40 60 40");
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
				// Forget any suffocation or fall damage the golem took where it was summoned, so it cannot shorten this hit.
				golem.setInvulnerableTime(0);
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
