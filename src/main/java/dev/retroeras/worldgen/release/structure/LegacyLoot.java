package dev.retroeras.worldgen.release.structure;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** 1.6.4's WeightedRandomChestContent: an item, a stack size range and a weight. */
public record LegacyLoot(Function<HolderLookup.Provider, ItemStack> stack, int min, int max, int weight) {
	/** The enchantments 1.6.4 had, for its random enchanted books. */
	private static final List<String> ENCHANTMENTS = List.of("protection", "fire_protection", "feather_falling", "blast_protection",
			"projectile_protection", "respiration", "aqua_affinity", "thorns", "sharpness", "smite", "bane_of_arthropods", "knockback",
			"fire_aspect", "looting", "efficiency", "silk_touch", "unbreaking", "fortune", "power", "punch", "flame", "infinity");

	public static LegacyLoot of(Item item, int min, int max, int weight) {
		return new LegacyLoot(registries -> new ItemStack(item), min, max, weight);
	}

	/** Item.enchantedBook.func_92112_a: a random enchanted book with its own stack size and weight. */
	public static LegacyLoot enchantedBook(Random random, int min, int max, int weight) {
		LegacyLoot book = enchantedBook(random);
		return new LegacyLoot(book.stack(), min, max, weight);
	}

	/** Item.enchantedBook.func_92114_b: a book with one random enchantment, chosen when the chest is filled. */
	public static LegacyLoot enchantedBook(Random random) {
		String name = ENCHANTMENTS.get(random.nextInt(ENCHANTMENTS.size()));
		int roll = random.nextInt(5);
		return new LegacyLoot(registries -> {
			Holder<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT)
					.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(name)));
			int level = 1 + roll % enchantment.value().getMaxLevel();
			return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
		}, 1, 1, 1);
	}

	public static LegacyLoot[] with(LegacyLoot[] list, LegacyLoot... extra) {
		LegacyLoot[] all = new LegacyLoot[list.length + extra.length];
		System.arraycopy(list, 0, all, 0, list.length);
		System.arraycopy(extra, 0, all, list.length, extra.length);
		return all;
	}

	/** WeightedRandomChestContent.generateChestContents. */
	public static void fill(Random random, LegacyLoot[] list, Container container, int count, HolderLookup.Provider registries) {
		for (int i = 0; i < count; i++) {
			LegacyLoot loot = pick(random, list);
			int size = loot.min + random.nextInt(loot.max - loot.min + 1);
			ItemStack stack = loot.stack.apply(registries);
			if (stack.getMaxStackSize() >= size) {
				container.setItem(random.nextInt(container.getContainerSize()), stack.copyWithCount(size));
			} else {
				for (int n = 0; n < size; n++) {
					container.setItem(random.nextInt(container.getContainerSize()), stack.copyWithCount(1));
				}
			}
		}
	}

	/** WeightedRandom.getRandomItem. */
	private static LegacyLoot pick(Random random, LegacyLoot[] list) {
		int total = 0;
		for (LegacyLoot loot : list) {
			total += loot.weight;
		}
		int roll = random.nextInt(total);
		for (LegacyLoot loot : list) {
			roll -= loot.weight;
			if (roll < 0) {
				return loot;
			}
		}
		return list[list.length - 1];
	}
}
