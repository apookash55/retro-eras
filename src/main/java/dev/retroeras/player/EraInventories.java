package dev.retroeras.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Gives every player a separate inventory, ender chest, experience and hunger for each era, so nothing from the modern game
 * comes with you into an old era (or back out). The overworld, nether, end and other mods' dimensions share the
 * "modern" set. The live inventory always belongs to the group named in {@link #ACTIVE_GROUP}; the others are stored.
 */
public final class EraInventories {
	/** A stored copy of everything that is kept per era. */
	public record Snapshot(List<ItemStack> inventory, List<ItemStack> enderChest, int xpLevel, float xpProgress, int totalXp,
			int foodLevel, float saturation) {
		public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inventory").forGetter(Snapshot::inventory),
				ItemStack.OPTIONAL_CODEC.listOf().fieldOf("ender_chest").forGetter(Snapshot::enderChest),
				Codec.INT.fieldOf("xp_level").forGetter(Snapshot::xpLevel),
				Codec.FLOAT.fieldOf("xp_progress").forGetter(Snapshot::xpProgress),
				Codec.INT.fieldOf("total_xp").forGetter(Snapshot::totalXp),
				Codec.INT.optionalFieldOf("food_level", 20).forGetter(Snapshot::foodLevel),
				Codec.FLOAT.optionalFieldOf("saturation", 5.0F).forGetter(Snapshot::saturation)
		).apply(instance, Snapshot::new));

		static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), 0, 0.0F, 0, 20, 5.0F);
	}

	/** Which group the player's live inventory currently belongs to. */
	public static final AttachmentType<String> ACTIVE_GROUP = AttachmentRegistry.<String>builder()
			.persistent(Codec.STRING)
			.copyOnDeath()
			.initializer(() -> Era.MODERN)
			.buildAndRegister(RetroEras.id("active_inventory_group"));

	/** Stored snapshots for the groups the player is not currently in. */
	public static final AttachmentType<Map<String, Snapshot>> STORED = AttachmentRegistry.<Map<String, Snapshot>>builder()
			.persistent(Codec.unboundedMap(Codec.STRING, Snapshot.CODEC))
			.copyOnDeath()
			.initializer(Map::of)
			.buildAndRegister(RetroEras.id("stored_inventories"));

	private EraInventories() {
	}

	public static void init() {
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> sync(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerPlayerEvents.JOIN.register(EraInventories::sync);
	}

	/** Swaps the player's live inventory if they are now in a different group than the one it belongs to. */
	public static void sync(ServerPlayer player) {
		switchTo(player, Era.inventoryGroup(player.level().dimension()));
	}

	/** Makes {@code target} the player's live inventory group, storing the current one. */
	public static void switchTo(ServerPlayer player, String target) {
		String active = player.getAttachedOrCreate(ACTIVE_GROUP);
		if (target.equals(active)) {
			return;
		}

		Map<String, Snapshot> stored = new HashMap<>(player.getAttachedOrCreate(STORED));
		stored.put(active, capture(player));
		restore(player, stored.getOrDefault(target, Snapshot.EMPTY));
		stored.remove(target);

		player.setAttached(STORED, Map.copyOf(stored));
		player.setAttached(ACTIVE_GROUP, target);
		RetroEras.LOGGER.debug("Swapped {}'s inventory from {} to {}", player.getName().getString(), active, target);
	}

	private static Snapshot capture(ServerPlayer player) {
		return new Snapshot(copy(player.getInventory()), copy(player.getEnderChestInventory()),
				player.experienceLevel, player.experienceProgress, player.totalExperience,
				player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
	}

	private static void restore(ServerPlayer player, Snapshot snapshot) {
		fill(player.getInventory(), snapshot.inventory());
		fill(player.getEnderChestInventory(), snapshot.enderChest());
		player.experienceLevel = snapshot.xpLevel();
		player.experienceProgress = snapshot.xpProgress();
		player.totalExperience = snapshot.totalXp();
		player.getFoodData().setFoodLevel(snapshot.foodLevel());
		player.getFoodData().setSaturation(snapshot.saturation());
		// Changing level, respawning and joining all reset the player's last-sent XP, so the bar resends next tick.
		player.containerMenu.broadcastChanges();
	}

	private static List<ItemStack> copy(Container container) {
		List<ItemStack> items = new ArrayList<>(container.getContainerSize());
		for (int i = 0; i < container.getContainerSize(); i++) {
			items.add(container.getItem(i).copy());
		}
		return items;
	}

	private static void fill(Container container, List<ItemStack> items) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			container.setItem(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
		}
	}
}
