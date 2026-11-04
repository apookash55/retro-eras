package dev.retroeras.player;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.retroeras.Era;
import dev.retroeras.RetroEras;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps each era to the items it had, from data/retroeras/era_items/&lt;era&gt;.json.
 * <ul>
 * <li>Loot (block drops, mob drops, chest loot) first swaps modern drops for the era's ({@code replace}, such as raw
 * iron for iron ore), then loses anything the era did not have.</li>
 * <li>Crafting gives nothing for newer items (see CraftingMenuMixin), and newer items on the ground cannot be picked
 * up (see ItemEntityMixin).</li>
 * <li>Anything newer that still reaches the inventory (the creative menu, a furnace, a command) is taken away and
 * kept for the player's modern inventory, where it comes back the next time they are in the modern world.</li>
 * </ul>
 */
public final class EraItems {
	private record Rules(Set<Item> allowed, Map<Item, Item> replace) {
	}

	/** Items taken from the player in an era, waiting to be given back in the modern world. */
	public static final AttachmentType<List<ItemStack>> RETURNED = AttachmentRegistry.<List<ItemStack>>builder()
			.persistent(ItemStack.CODEC.listOf())
			.copyOnDeath()
			.initializer(List::of)
			.buildAndRegister(RetroEras.id("returned_items"));

	private static final Map<Era, Rules> RULES = new EnumMap<>(Era.class);
	private static final List<String> UNKNOWN = new ArrayList<>();

	private EraItems() {
	}

	public static void init() {
		for (Era era : Era.values()) {
			RULES.put(era, load(era));
		}

		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			Era era = Era.fromDimension(context.getLevel().dimension());
			if (era != null) {
				filterDrops(era, drops);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Era era = Era.fromDimension(player.level().dimension());
				if (era != null) {
					takeNewerItems(player, era);
				}
			}
		});
	}

	/** Whether {@code item} existed in {@code era}. */
	public static boolean allowed(Era era, Item item) {
		return item == Items.AIR || RULES.get(era).allowed().contains(item);
	}

	/** Whether {@code player} may have {@code stack} where they are now. */
	public static boolean allowed(Player player, ItemStack stack) {
		Era era = Era.fromDimension(player.level().dimension());
		return era == null || allowed(era, stack.getItem());
	}

	/** Every item named in an era's list, for tests: ids that are missing from the game are logged and skipped. */
	public static Set<Item> allowedItems(Era era) {
		return RULES.get(era).allowed();
	}

	/** Ids in the era lists that are not items in this version of the game. */
	public static List<String> unknownIds() {
		return List.copyOf(UNKNOWN);
	}

	static void filterDrops(Era era, List<ItemStack> drops) {
		Rules rules = RULES.get(era);
		for (int i = drops.size() - 1; i >= 0; i--) {
			ItemStack stack = drops.get(i);
			Item replacement = rules.replace().get(stack.getItem());
			if (replacement != null) {
				drops.set(i, new ItemStack(replacement, stack.getCount()));
			} else if (!allowed(era, stack.getItem())) {
				drops.remove(i);
			}
		}
	}

	private static void takeNewerItems(ServerPlayer player, Era era) {
		Inventory inventory = player.getInventory();
		List<ItemStack> taken = new ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && !allowed(era, stack.getItem())) {
				taken.add(stack);
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		ItemStack carried = player.containerMenu.getCarried();
		if (!carried.isEmpty() && !allowed(era, carried.getItem())) {
			taken.add(carried);
			player.containerMenu.setCarried(ItemStack.EMPTY);
		}
		if (taken.isEmpty()) {
			return;
		}
		player.containerMenu.broadcastChanges();
		// The creative menu hands out stacks endlessly, so only survival items are worth keeping.
		if (!player.isCreative()) {
			List<ItemStack> returned = new ArrayList<>(player.getAttachedOrCreate(RETURNED));
			returned.addAll(taken);
			player.setAttached(RETURNED, List.copyOf(returned));
		}
		player.sendOverlayMessage(Component.translatable(player.isCreative() ? "retroeras.item.too_new.creative" : "retroeras.item.too_new",
				taken.getFirst().getHoverName(), Component.translatable("retroeras.era." + era.getName())).withStyle(ChatFormatting.GRAY));
	}

	/** Gives back what was taken in the eras, once the player is in the modern world again. */
	public static void giveBack(ServerPlayer player) {
		List<ItemStack> returned = player.getAttachedOrCreate(RETURNED);
		if (returned.isEmpty() || Era.fromDimension(player.level().dimension()) != null) {
			return;
		}
		player.setAttached(RETURNED, List.of());
		for (ItemStack stack : returned) {
			player.getInventory().placeItemBackInInventory(stack.copy(), Prediction.SERVER_ONLY);
		}
		player.containerMenu.broadcastChanges();
	}

	private static Rules load(Era era) {
		String path = "/data/retroeras/era_items/" + era.getName() + ".json";
		JsonObject json;
		try (InputStream in = EraItems.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing " + path);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				json = JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}

		Set<Item> allowed = new LinkedHashSet<>();
		for (JsonElement id : json.getAsJsonArray("items")) {
			Item item = item(id.getAsString(), path);
			if (item != null) {
				allowed.add(item);
			}
		}
		Map<Item, Item> replace = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("replace").entrySet()) {
			Item from = item(entry.getKey(), path);
			Item to = item(entry.getValue().getAsString(), path);
			if (from != null && to != null) {
				replace.put(from, to);
			}
		}
		return new Rules(Set.copyOf(new HashSet<>(allowed)), Map.copyOf(replace));
	}

	private static @Nullable Item item(String id, String path) {
		return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id)).orElseGet(() -> {
			RetroEras.LOGGER.warn("Unknown item {} in {}", id, path);
			UNKNOWN.add(id);
			return null;
		});
	}

}
