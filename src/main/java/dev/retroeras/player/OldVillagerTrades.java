package dev.retroeras.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jspecify.annotations.Nullable;

import dev.retroeras.worldgen.release.structure.LegacyLoot;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 1.6.4's trades (EntityVillager.addDefaultEquipmentAndRecipies), ported. A villager starts with one and adds another each
 * time its newest one is used. There are no levels: which trades can come up depends on how many it has, each kind's chance
 * rising by a fifth per trade until it folds back from 0.9. Every trade has seven uses until the villager restocks.
 */
public final class OldVillagerTrades {
	private static final int USES = 7;

	private OldVillagerTrades() {
	}

	/** Adds {@code count} new trades for {@code profession}, as 1.6.4 did when the villager was first spoken to or had traded. */
	public static void add(ResourceKey<VillagerProfession> profession, MerchantOffers offers, int count, Random random, RegistryAccess registries) {
		float rate = (float) Math.floor(offers.size()) * 0.2F;
		List<MerchantOffer> candidates = new ArrayList<>();
		Candidates list = new Candidates(candidates, random, rate);
		if (profession == VillagerProfession.FARMER) {
			farmer(list);
		} else if (profession == VillagerProfession.LIBRARIAN) {
			librarian(list, registries);
		} else if (profession == VillagerProfession.CLERIC) {
			priest(list, registries);
		} else if (profession == VillagerProfession.ARMORER) {
			blacksmith(list);
		} else {
			butcher(list);
		}
		if (candidates.isEmpty()) {
			list.sell(Items.GOLD_INGOT, 8, 10, 1.0F, true);
		}
		Collections.shuffle(candidates, random);
		for (int i = 0; i < count && i < candidates.size(); i++) {
			merge(offers, candidates.get(i));
		}
	}

	/** MerchantRecipeList.addToListWithCheck: the same trade replaces the old one, at the new price, rather than repeating. */
	private static void merge(MerchantOffers offers, MerchantOffer offer) {
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer existing = offers.get(i);
			if (ItemStack.isSameItem(existing.getResult(), offer.getResult()) && ItemStack.isSameItem(existing.getBaseCostA(), offer.getBaseCostA())
					&& existing.getItemCostB().isPresent() == offer.getItemCostB().isPresent()) {
				offers.set(i, offer);
				return;
			}
		}
		offers.add(offer);
	}

	/** Refreshes what has run out once the newest trade has been used. */
	public static void restock(MerchantOffers offers) {
		if (offers.size() > 1) {
			for (MerchantOffer offer : offers) {
				if (offer.isOutOfStock()) {
					offer.resetUses();
				}
			}
		}
	}

	private static void farmer(Candidates l) {
		l.sell(Items.WHEAT, 18, 22, 0.9F, false);
		l.sell(Items.WOOL.pick(DyeColor.WHITE), 14, 22, 0.5F, false);
		l.sell(Items.CHICKEN, 14, 18, 0.5F, false);
		l.sell(Items.COOKED_COD, 9, 13, 0.4F, false);
		l.buy(Items.BREAD, -4, -2, 0.9F);
		l.buy(Items.MELON_SLICE, -8, -4, 0.3F);
		l.buy(Items.APPLE, -8, -4, 0.3F);
		l.buy(Items.COOKIE, -10, -7, 0.3F);
		l.buy(Items.SHEARS, 3, 4, 0.3F);
		l.buy(Items.FLINT_AND_STEEL, 3, 4, 0.3F);
		l.buy(Items.COOKED_CHICKEN, -8, -6, 0.3F);
		l.buy(Items.ARROW, -12, -8, 0.5F);
		if (l.random.nextFloat() < l.chance(0.5F)) {
			l.out.add(new MerchantOffer(new ItemCost(Items.GRAVEL, 10), Optional.of(new ItemCost(Items.EMERALD)), new ItemStack(Items.FLINT, 4 + l.random.nextInt(2)), 0, USES, 0, 0.0F));
		}
	}

	private static void librarian(Candidates l, RegistryAccess registries) {
		l.sell(Items.PAPER, 24, 36, 0.8F, false);
		l.sell(Items.BOOK, 11, 13, 0.8F, false);
		l.sell(Items.WRITTEN_BOOK, 1, 1, 0.3F, false);
		l.buy(Items.BOOKSHELF, 3, 4, 0.8F);
		l.buy(Items.GLASS, -5, -3, 0.2F);
		l.buy(Items.COMPASS, 10, 12, 0.2F);
		l.buy(Items.CLOCK, 10, 12, 0.2F);
		if (l.random.nextFloat() < l.chance(0.07F)) {
			ItemStack book = LegacyLoot.enchantedBook(l.random).stack().apply(registries);
			ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
			int level = stored.entrySet().stream().mapToInt(entry -> entry.getIntValue()).findFirst().orElse(1);
			int price = 2 + l.random.nextInt(5 + level * 10) + 3 * level;
			l.out.add(new MerchantOffer(new ItemCost(Items.BOOK), Optional.of(new ItemCost(Items.EMERALD, Math.min(price, 64))), book, 0, USES, 0, 0.0F));
		}
	}

	private static void priest(Candidates l, RegistryAccess registries) {
		l.buy(Items.ENDER_EYE, 7, 11, 0.3F);
		l.buy(Items.EXPERIENCE_BOTTLE, -4, -1, 0.2F);
		l.buy(Items.REDSTONE, -4, -1, 0.4F);
		l.buy(Items.GLOWSTONE, -3, -1, 0.3F);
		for (Item item : new Item[] {Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE}) {
			if (l.random.nextFloat() < l.chance(0.05F)) {
				ItemStack enchanted = EnchantmentHelper.enchantItem(net.minecraft.util.RandomSource.create(l.random.nextLong()), new ItemStack(item), 5 + l.random.nextInt(15), registries, Optional.empty());
				l.out.add(new MerchantOffer(new ItemCost(item), Optional.of(new ItemCost(Items.EMERALD, 2 + l.random.nextInt(3))), enchanted, 0, USES, 0, 0.0F));
			}
		}
	}

	private static void blacksmith(Candidates l) {
		l.sell(Items.COAL, 16, 24, 0.7F, false);
		l.sell(Items.IRON_INGOT, 8, 10, 0.5F, false);
		l.sell(Items.GOLD_INGOT, 8, 10, 0.5F, false);
		l.sell(Items.DIAMOND, 4, 6, 0.5F, false);
		l.buy(Items.IRON_SWORD, 7, 11, 0.5F);
		l.buy(Items.DIAMOND_SWORD, 12, 14, 0.5F);
		l.buy(Items.IRON_AXE, 6, 8, 0.3F);
		l.buy(Items.DIAMOND_AXE, 9, 12, 0.3F);
		l.buy(Items.IRON_PICKAXE, 7, 9, 0.5F);
		l.buy(Items.DIAMOND_PICKAXE, 10, 12, 0.5F);
		l.buy(Items.IRON_SHOVEL, 4, 6, 0.2F);
		l.buy(Items.DIAMOND_SHOVEL, 7, 8, 0.2F);
		l.buy(Items.IRON_HOE, 4, 6, 0.2F);
		l.buy(Items.DIAMOND_HOE, 7, 8, 0.2F);
		l.buy(Items.IRON_BOOTS, 4, 6, 0.2F);
		l.buy(Items.DIAMOND_BOOTS, 7, 8, 0.2F);
		l.buy(Items.IRON_HELMET, 4, 6, 0.2F);
		l.buy(Items.DIAMOND_HELMET, 7, 8, 0.2F);
		l.buy(Items.IRON_CHESTPLATE, 10, 14, 0.2F);
		l.buy(Items.DIAMOND_CHESTPLATE, 16, 19, 0.2F);
		l.buy(Items.IRON_LEGGINGS, 8, 10, 0.2F);
		l.buy(Items.DIAMOND_LEGGINGS, 11, 14, 0.2F);
		l.buy(Items.CHAINMAIL_BOOTS, 5, 7, 0.1F);
		l.buy(Items.CHAINMAIL_HELMET, 5, 7, 0.1F);
		l.buy(Items.CHAINMAIL_CHESTPLATE, 11, 15, 0.1F);
		l.buy(Items.CHAINMAIL_LEGGINGS, 9, 11, 0.1F);
	}

	private static void butcher(Candidates l) {
		l.sell(Items.COAL, 16, 24, 0.7F, false);
		l.sell(Items.PORKCHOP, 14, 18, 0.5F, false);
		l.sell(Items.BEEF, 14, 18, 0.5F, false);
		l.buy(Items.SADDLE, 6, 8, 0.1F);
		l.buy(Items.LEATHER_CHESTPLATE, 4, 5, 0.3F);
		l.buy(Items.LEATHER_BOOTS, 2, 4, 0.3F);
		l.buy(Items.LEATHER_HELMET, 2, 4, 0.3F);
		l.buy(Items.LEATHER_LEGGINGS, 2, 4, 0.3F);
		l.buy(Items.COOKED_PORKCHOP, -7, -5, 0.3F);
		l.buy(Items.COOKED_BEEF, -7, -5, 0.3F);
	}

	private static final class Candidates {
		final List<MerchantOffer> out;
		final Random random;
		private final float rate;

		Candidates(List<MerchantOffer> out, Random random, float rate) {
			this.out = out;
			this.random = random;
			this.rate = rate;
		}

		/** func_82188_j: the chance plus the villager's rate, folding back down past 0.9. */
		float chance(float base) {
			float chance = base + rate;
			return chance > 0.9F ? 0.9F - (chance - 0.9F) : chance;
		}

		private static int between(Random random, int min, int max) {
			return min >= max ? min : min + random.nextInt(max - min);
		}

		/** The villager gives one emerald for between {@code min} and {@code max} of the item. */
		void sell(Item item, int min, int max, float base, boolean always) {
			if (always || random.nextFloat() < chance(base)) {
				out.add(new MerchantOffer(new ItemCost(item, between(random, min, max)), Optional.empty(), new ItemStack(Items.EMERALD), 0, USES, 0, 0.0F));
			}
		}

		/** The villager sells the item for emeralds, or if the numbers are negative, that many of it for one emerald. */
		void buy(Item item, int min, int max, float base) {
			if (random.nextFloat() < chance(base)) {
				int price = between(random, min, max);
				ItemCost cost = new ItemCost(Items.EMERALD, price < 0 ? 1 : price);
				out.add(new MerchantOffer(cost, Optional.empty(), new ItemStack(item, price < 0 ? -price : 1), 0, USES, 0, 0.0F));
			}
		}
	}
}
