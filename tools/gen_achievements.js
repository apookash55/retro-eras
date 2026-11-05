// Generates the eras' achievements as advancement files, from the AchievementList of Beta 1.7.3 and of 1.6.4 (decompiled
// from the player's own jars; names and descriptions are from their en_US language files). Run with node from the
// project root:  node tools/gen_achievements.js
//
// Every achievement has a single "impossible" criterion: dev.retroeras.player.EraAchievements awards it from code, at the
// moment the old game did, and only once its parent is earned, as the old "Requires" rule had it. Alpha, Infdev,
// Classic and Cave game had no achievements.
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', 'src', 'main', 'resources');
const advancementDir = path.join(root, 'data', 'retroeras', 'advancement');
const langFile = path.join(root, 'assets', 'retroeras', 'lang', 'en_us.json');

// [id, old name, icon, parent, special, title, description]
const beta = [
  ['open_inventory', 'openInventory', 'book', null, false, 'Taking Inventory', "Press 'E' to open your inventory."],
  ['mine_wood', 'mineWood', 'oak_log', 'open_inventory', false, 'Getting Wood', 'Attack a tree until a block of wood pops out'],
  ['build_work_bench', 'buildWorkBench', 'crafting_table', 'mine_wood', false, 'Benchmarking', 'Craft a workbench with four blocks of planks'],
  ['build_pickaxe', 'buildPickaxe', 'wooden_pickaxe', 'build_work_bench', false, 'Time to Mine!', 'Use planks and sticks to make a pickaxe'],
  ['build_furnace', 'buildFurnace', 'furnace', 'build_pickaxe', false, 'Hot Topic', 'Construct a furnace out of eight stone blocks'],
  ['acquire_iron', 'acquireIron', 'iron_ingot', 'build_furnace', false, 'Acquire Hardware', 'Smelt an iron ingot'],
  ['build_hoe', 'buildHoe', 'wooden_hoe', 'build_work_bench', false, 'Time to Farm!', 'Use planks and sticks to make a hoe'],
  ['make_bread', 'makeBread', 'bread', 'build_hoe', false, 'Bake Bread', 'Turn wheat into bread'],
  ['bake_cake', 'bakeCake', 'cake', 'build_hoe', false, 'The Lie', 'Wheat, sugar, milk and eggs!'],
  ['build_better_pickaxe', 'buildBetterPickaxe', 'stone_pickaxe', 'build_pickaxe', false, 'Getting an Upgrade', 'Construct a better pickaxe'],
  ['cook_fish', 'cookFish', 'cooked_cod', 'build_furnace', false, 'Delicious Fish', 'Catch and cook fish!'],
  ['on_a_rail', 'onARail', 'rail', 'acquire_iron', true, 'On A Rail', 'Travel by minecart at least 1 km from where you started'],
  ['build_sword', 'buildSword', 'wooden_sword', 'build_work_bench', false, 'Time to Strike!', 'Use planks and sticks to make a sword'],
  ['kill_enemy', 'killEnemy', 'bone', 'build_sword', false, 'Monster Hunter', 'Attack and destroy a monster'],
  ['kill_cow', 'killCow', 'leather', 'build_sword', false, 'Cow Tipper', 'Harvest some leather'],
  ['fly_pig', 'flyPig', 'saddle', 'kill_cow', true, 'When Pigs Fly', 'Fly a pig off a cliff'],
];
const release = beta.concat([
  ['snipe_skeleton', 'snipeSkeleton', 'bow', 'kill_enemy', true, 'Sniper Duel', 'Kill a skeleton with an arrow from more than 50 meters'],
  ['diamonds', 'diamonds', 'diamond', 'acquire_iron', false, 'DIAMONDS!', 'Acquire diamonds with your iron tools'],
  ['portal', 'portal', 'obsidian', 'diamonds', false, 'We Need to Go Deeper', 'Build a portal to the Nether'],
  ['ghast', 'ghast', 'ghast_tear', 'portal', true, 'Return to Sender', 'Destroy a Ghast with a fireball'],
  ['blaze_rod', 'blazeRod', 'blaze_rod', 'portal', false, 'Into Fire', 'Relieve a Blaze of its rod'],
  ['potion', 'potion', 'potion', 'blaze_rod', false, 'Local Brewery', 'Brew a potion'],
  ['the_end', 'theEnd', 'ender_eye', 'blaze_rod', true, 'The End?', 'Locate the End'],
  ['the_end2', 'theEnd2', 'dragon_egg', 'the_end', true, 'The End.', 'Defeat the Ender Dragon'],
  ['enchantments', 'enchantments', 'enchanting_table', 'diamonds', false, 'Enchanter', 'Use a book, obsidian and diamonds to construct an enchantment table'],
  ['overkill', 'overkill', 'diamond_sword', 'enchantments', true, 'Overkill', 'Deal eight hearts of damage in a single hit'],
  ['bookcase', 'bookcase', 'bookshelf', 'enchantments', false, 'Librarian', 'Build some bookshelves to improve your enchantment table'],
]);

const lang = JSON.parse(fs.readFileSync(langFile, 'utf8'));
for (const key of Object.keys(lang)) {
  if (key.startsWith('advancements.retroeras.')) {
    delete lang[key];
  }
}

for (const [era, list] of [['beta', beta], ['old_release', release]]) {
  const dir = path.join(advancementDir, era);
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  for (const [id, , icon, parent, special, title, description] of list) {
    const key = `advancements.retroeras.${era}.${id}`;
    const display = {
      icon: { id: `minecraft:${icon}` },
      title: { translate: `${key}.title` },
      description: { translate: `${key}.description` },
      frame: special ? 'challenge' : 'task',
      show_toast: true,
      announce_to_chat: false,
      hidden: false,
    };
    if (!parent) {
      display.background = 'minecraft:block/stone';
    }
    const advancement = {};
    if (parent) {
      advancement.parent = `retroeras:${era}/${parent}`;
    }
    advancement.display = display;
    advancement.criteria = { achieved: { trigger: 'minecraft:impossible' } };
    fs.writeFileSync(path.join(dir, `${id}.json`), JSON.stringify(advancement, null, 2) + '\n');
    lang[`${key}.title`] = title;
    lang[`${key}.description`] = description;
  }
  console.log(`${era}: ${list.length} achievements`);
}
fs.writeFileSync(langFile, JSON.stringify(lang, null, 2) + '\n');
