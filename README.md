# Retro Eras

A Fabric mod for Minecraft 26.3 that adds a dimension for each historical era of Minecraft, so you can visit them all from one world.

| Era | Dimension | Status |
| --- | --- | --- |
| Old Release (1.6.4) | `retroeras:old_release` | **playable** |
| Beta (1.7.3) | `retroeras:beta` | **playable** |
| Alpha (1.2.6) | `retroeras:alpha` | **playable** |
| Pre-Alpha (Infdev 20100618) | `retroeras:pre_alpha` | **playable** |
| Cave game (rd-161348) | `retroeras:cavegame` | **playable** |

## Playing the prototype

```
./gradlew runClient
```

Build a nether-portal-shaped frame out of **mossy cobblestone** (at least 4 wide and 5 tall) and light it with flint and steel. Walking through takes you to the Beta dimension, where a matching portal is built for the trip back. Each era keeps its own inventory, ender chest and XP: what you carry in the overworld stays there.

For Old Release (1.6.4), build it out of **stone bricks**.

For Alpha (1.2.6), build it out of **pumpkins**, a nod to Alpha 1.2's Halloween Update.

For Pre-Alpha (Infdev), build it out of **bricks**, a nod to Infdev's brick pyramids.

For Cave game, build the same frame out of **cobblestone**. You arrive inside its 256×256 world wherever you light the portal.

## Installing in your own game

Retro Eras is a Fabric mod for Minecraft 26.3. It needs Fabric Loader 0.20.1 or newer, Fabric API 0.165.0+26.3, and Java 25 (the launcher's own runtime for 26.3 is fine).

1. Build it with `./gradlew build`. The mod is `build/libs/retroeras-<version>.jar`.
2. Install Fabric for 26.3 (the Fabric installer from fabricmc.net, or the version profile from `meta.fabricmc.net`), and put the mod and Fabric API in that profile's `mods` folder.
3. Play each old version once in the launcher (Beta 1.7.3, Alpha 1.2.6, Infdev 20100618, rd-161348 and 1.6.4) so its jar and sounds are on your computer. The mod reads its textures and sounds from them, and an era whose jar is missing keeps modern textures.

On this machine there is a **Retro Eras** profile in the Minecraft launcher with its own game folder, `.minecraft/profiles/retro-eras`, so its worlds and settings are kept apart from your normal ones. After changing the mod, `./gradlew installMod` copies the new jar into that profile (`-PmodsDir=<folder>` points it somewhere else).

## Digging down through the eras

The eras are stacked on top of each other, newest at the top: the overworld, then Old Release, Beta, Alpha, Pre-Alpha and Cave game at the bottom (`EraLayers`).
- **Going down:** the very lowest layer of bedrock can be mined, slowly (as long as obsidian takes), with a diamond pickaxe or better. The rest of the bedrock stays unbreakable, so you have to find a spot where the layers above it have gaps. Fall through and you drop into the sky of the next older era over the same spot (inside the map, for Cave game). That landing does not hurt. Infdev's bedrock already has holes, so you can fall from Pre-Alpha into Cave game without digging.
- **Going up:** get above the top of an older era's world (pillar up to the build limit and jump, or fly) and you come up in the next newer era just above its bedrock. If the rock is solid there, a small pocket is dug for you.
- Your inventory changes with the era, just as it does through a portal.

## Gameplay in every era

- **Combat is pre-1.9.** There is no attack cooldown, so every click is a full-strength hit and the attack indicator never shows. There are no sweep attacks and no offhand: anything put in the offhand goes back into your inventory. Critical hits arrived in Beta 1.8, so only Old Release has them.
- **Weapons deal their era's damage.** In Beta and Infdev, swords dealt 4 + 2 per tier (wood and gold 4, stone 6, iron 8, diamond 10). In 1.6.4 they dealt 4 + 1 per tier (4, 5, 6, 7). Axes, pickaxes and shovels dealt 3, 2 and 1 plus their tier in all of them, and everything else 1. Strength and weakness still apply on top.
- **Only the era's items.** Each era has a list of the items it had, in `src/main/resources/data/retroeras/era_items/`: about 120 for Infdev, 220 for Beta and 410 for 1.6.4 (including creative-only blocks and its spawn eggs). In an era:
  - Crafting gives nothing for a newer item, and newer items on the ground cannot be picked up.
  - Drops follow the era. Iron and gold ore drop themselves rather than raw ore, Beta and Infdev zombies drop feathers, and anything the era did not have (like raw beef in Beta) is not dropped. This applies to chest loot too.
  - Anything newer that still gets into your inventory, from a furnace, a command or the creative menu, is taken away with a message. Survival items are kept for you and come back the next time you are in the modern world; creative-menu items are just removed.
- **Sword blocking in Old Release.** Holding right-click with a sword raises it, as from Beta 1.8 to 1.8.9, and cuts damage that armour could stop to (1 + damage) / 2.
- **Furnaces** in an era do not smelt anything into an item the era did not have.
- **1.6.4 hunger.** Old Release has no fast healing from saturation (added in 1.11). You heal half a heart every 4 seconds while your hunger bar is at 18 or more, which costs 3 exhaustion as it did in 1.6.4.

## Fog

The eras have the fog of the versions they come from (`EraFog`): it starts a quarter of the way out and closes in completely at the old render distance, Far (256 blocks), Normal (128), Short (64) or Tiny (32). The older the era, the closer the fog: 1.6.4 starts on Far, Beta on Normal, Infdev on Short and Alpha on Tiny. Cave game had almost no fog, so it starts on Far. Press **R** (the "Render Distance (in eras)" key, which you can rebind under Controls) to cycle it, as F did in Beta; Shift+R goes back. "Modern" turns the old fog off. Each era remembers its own setting in `config/retroeras.properties` (`fog.beta=normal`, and so on).

Leaves in the eras are still, too: no falling leaves, dripping water or rustling, which old versions did not have.

## Old textures and sounds

The mod never ships Mojang's old textures. On startup it reads them from your own copies of the old client jars, which the launcher stores under `.minecraft/versions/`: `1.6.4`, `b1.7.3`, `a1.2.6`, `inf-20100618` and `rd-161348`. It cuts them into resource packs at `resourcepacks/retroeras_<era>/` and turns each pack on only while you are in that era. You can point it at another jar with `<era>.jar=<path>` (for example `beta.jar=...`) in `config/retroeras.properties`. If a jar is missing, that era keeps modern textures.

The tile-to-texture mappings live in `src/client/resources/assets/retroeras/era_textures/`. For Beta, it covers about 300 blocks, items, mobs, HUD icons, the font, sky and colormaps. The player skin, pig, cow, chicken, sheep, wolf and zombie are converted too. Textures whose layout changed after Beta are rebuilt from Beta's own: chests and double chests are painted onto 26.3's chest model from Beta's chest tiles, beds and redstone dust are turned to fit 26.3's faces, leather armour and water have the colour 26.3 tints them with divided out, and the inventory screen is Beta's with its crafting grid moved to where 26.3 puts it (and no offhand slot or recipe book button). Beta and Infdev drew water, lava, fire and the nether portal with code each tick; the mod runs ports of that code (`LegacyTextureEffects`) and records the animations into the pack. 1.6.4 already stores one file per texture, so its mapping (about 490 textures plus HUD regions) is generated by `tools/gen_release_texture_map.ps1` through the 1.13 renames; animated textures keep their `.mcmeta`.

Sounds come the same way, from your own copies of the old sound files, which the launcher keeps in `.minecraft/assets/` (the `pre-1.6` index for Beta and Infdev, `legacy` for 1.6.4); they go into the same era pack. Mappings live in `src/client/resources/assets/retroeras/era_sounds/` and are generated by `tools/gen_era_sounds.ps1`, which checks every event against 26.3.
- **Beta and Infdev** use the old `newsound` set: footsteps, the original "oof" hurt sound, the old mob noises, chests, doors, fire, water, portals and cave ambience. As in those versions, breaking and placing a block plays its footstep sound.
- **1.6.4** uses its own set, with separate dig sounds, per-mob sounds, horses, bats, villagers, fireworks and anvils.
- Sounds that did not exist yet, like the 1.9 combat swings, are silent. Music is limited to the era's tracks: Infdev had only the first three, and Beta and 1.6.4 the twelve game tracks of Volume Alpha.
- **Cave game** had no sound at all, so everything is silent while you are there.
If the sound files are missing (the launcher downloads them the first time you play that version), the era keeps modern sounds.

## Layout

- `src/main/java/dev/retroeras/Era.java`: the list of eras and their dimension keys.
- `src/main/java/dev/retroeras/portal/`: era portals (frame detection, lighting, linking and travel).
- `src/main/java/dev/retroeras/player/EraInventories.java`: the separate inventory, ender chest and XP for each era.
- `src/main/java/dev/retroeras/player/EraRules.java`, `EraCombat.java` and `EraItems.java`: hunger, combat and the item lists.
- `src/client/java/dev/retroeras/client/texture/`: builds each era's texture pack from the old jar and switches it on and off.
- `src/main/java/dev/retroeras/worldgen/`: each era's terrain generator.
- `src/main/java/dev/retroeras/entity/`: Cave game's zombies.
- `src/main/resources/data/retroeras/`: each era's dimension, dimension type, biomes and features.
- `tools/`: scripts that generate the Beta and 1.6.4 biome files, the 1.6.4 texture mapping and the portal textures.
- `src/gametest/`: tests. `./gradlew runGameTest` runs the headless server tests. `./gradlew runClientGameTest` opens the game, visits each era and saves screenshots to `build/run/clientGameTest/screenshots/`.

## Old Release (1.6.4)

- Terrain is a port of Minecraft 1.6.4's own generator (`src/main/java/dev/retroeras/worldgen/release/`): its noise (with 1.6's coordinate wrapping), the full GenLayer biome chain (islands, zooms, snow, mushroom islands, rivers, hills, shores, swamp rivers and the Voronoi zoom), biome-blended heights, the surface with ice on cold water, caves and ravines. A seed gives the same land and biomes it did in 1.6.4. The world is 256 tall but, as in 1.6.4, terrain stops at y=127.
- It has 1.6.4's 21 overworld biomes, from oceans and rivers to jungles, extreme hills and mushroom islands. Their grass, leaf and sky colours come from 1.6.4's colormaps and sky formula (swamps mixed the way 1.6 did). Trees, flowers, grass, reeds, cacti, mushrooms, lily pads, melons and vines follow 1.6.4's decorator, with emeralds and silverfish stone in Extreme Hills, water and lava lakes, and dungeons. The biome files come from `tools/gen_release_worldgen.ps1`.
- Mobs follow 1.6.4's lists: the farm animals, horses on plains, wolves in forests and taigas, ocelots in jungles, mooshrooms on mushroom islands, bats, and the usual monsters including endermen.
- Hunger, sprinting, XP and smooth lighting are all back, as in 1.6.4. Inventories are still kept separate from your modern ones.
- Structures are a port of 1.6.4's own (`src/main/java/dev/retroeras/worldgen/release/structure/`): mineshafts, villages in plains and deserts, desert pyramids, jungle temples, witch huts and the three strongholds. They are laid out from the seed exactly as 1.6.4 laid them out, and built with its blocks, chest loot, villagers, witch and spawners while the chunks generate. Eyes of ender lead to these strongholds. 1.6.4 bugs are kept: strongholds never have right turns, and desert village wells stay cobblestone.
- Villagers keep 1.6.4's five professions (farmer, librarian, priest, blacksmith and butcher, as farmer, librarian, cleric, armorer and butcher) but trade as 26.3 villagers do.

## Beta prototype

- The world runs from y=0 to 127, with sea level at 64 and clouds at about y=108.
- Terrain is a port of Beta 1.7.3's own generator (`src/main/java/dev/retroeras/worldgen/beta/`): its Perlin and simplex noise, density field, climate and biome lookup, sand and gravel beaches, and caves. A seed gives the same landscape it gave in Beta 1.7.3. Trees, ores and plants still come from the biomes' feature lists, so they differ from Beta in detail.
- It has Beta's 10 biomes: rainforest, seasonal forest, forest, swampland, savanna, shrubland, plains, taiga, tundra and desert. Their grass, leaf and sky colours are computed from Beta's own colormaps and sky formula. Trees use Beta's mixes, and there are no bee nests, leaf litter, fallen logs, cactus flowers, copper, granite, diorite, andesite, tuff, geodes, glow lichen or ravines. Ores are spread over y=0–127 the way Beta placed them. The biome files come from `tools/gen_beta_worldgen.ps1`, so edit that script and re-run it rather than editing the JSON.
- Mobs are limited to Beta's: pigs, sheep, cows, chickens and wolves, plus zombies, skeletons, spiders, creepers and slimes.
- Gameplay follows Beta: there is no hunger, no sprinting and no natural healing, and food heals you straight away. The hunger and XP bars are hidden, and smooth lighting is switched off while you are there. Your modern hunger is saved and given back when you leave.

## Alpha (1.2.6)

- Alpha 1.2.6, the last Alpha, generated terrain with the same code Beta 1.7.3 later used, so a seed gives the same landscape in both eras. The one difference is underground: Beta 1.2 added a few blocks of sandstone under sand, and Alpha has none. Its 10 biomes, climate, colours and caves are the same as Beta's.
- Its decoration is plainer: only oak and big oak trees, thick in forests, rainforests and taigas, rare elsewhere, and none at all on plains, deserts or tundra. Dandelions, roses, mushrooms, reeds, pumpkins and cacti; no tall grass, ferns, dead bushes or lapis. The biome files come from `tools/gen_alpha_worldgen.ps1`.
- Mobs are Alpha's: pigs, sheep, cows and chickens, plus zombies, skeletons, spiders, creepers and slimes. No wolves or squid yet.
- Textures and sounds come from your own `a1.2.6` jar, with the same rebuilt chests, inventory screen and animations as Beta. Its mapping is built from Beta's by `tools/gen_alpha_texture_map.ps1`, which leaves out the tiles Alpha did not have yet and turns its red redstone dust grey for 26.3 to tint.
- Rules are Beta's: no hunger, sprinting or natural healing, and Beta's weapon damage. "Minecraft Alpha v1.2.6" sits in the top left corner, as it did.

## Pre-Alpha (Infdev 20100618)

- Terrain is a port of Infdev 20100618's own generator (`src/main/java/dev/retroeras/worldgen/infdev/`), the last Infdev build and the ancestor of Beta's. It has no biomes or climate: hill size comes straight from noise, so the land is rougher than Beta's and reaches higher. Sand and gravel beaches, caves, and Infdev's bedrock floor with about one hole in six are all ported, and a seed gives the same landscape it did in Infdev.
- One biome with Infdev's green grass and leaves (their colour is part of the texture, so nothing is tinted). It has oak trees whose density varies across the land, dandelions, roses, mushrooms and springs. Ores follow Infdev: no redstone or lapis yet.
- Mobs are Infdev's six: pigs and sheep, plus zombies, skeletons, creepers and spiders. The sky is Infdev's #88BBFF.
- Rules are the same as Beta: health but no hunger, sprinting or natural healing. "Minecraft Infdev" sits in the top left corner, as it did.

## Cave game

- The world is a port of rd-161348's level generator: two diamond-square height maps give hills of grass over dirt over rock. It is 256×256 blocks and 64 tall, with void past the edges, just like the original. The original made a new random map on every launch; here it is seeded from your world seed.
- The sky is the pale blue (#80CCFF) and the fog the near-white (#FEFBFA) of the original. The sun never moves, there is no rain and there are no clouds.
- Ten of Cave game's zombies wander the map in slowly drifting circles, hopping now and then. As in rd-161348 they wear its player texture (char.png), and nothing can hurt them except falling off the edge; new ones drop in from above to keep there being ten.
- Saplings never grow, and vanish if they are not in sunlight on dirt or grass, as rd-161348's did.
- As in Cave game, there is no health, no hunger and nothing to survive. Only falling off the edge hurts. Blocks break instantly and drop nothing, and your hotbar always holds 64 each of the five blocks Cave game let you place (stone, dirt, cobblestone, planks and saplings). No other mobs spawn.

## Next steps

- Witches only spawn once per hut; 1.6.4 also kept spawning them inside it.
- Villages trade as 26.3 villagers do, rather than with 1.6.4's trade lists.
