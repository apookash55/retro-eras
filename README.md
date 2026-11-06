# Retro Eras

A Fabric mod for Minecraft 26.3 that adds a dimension for each historical era of Minecraft, so you can visit them all from one world.

| Era | Dimension | Status |
| --- | --- | --- |
| Old Release (1.6.4) | `retroeras:old_release` | **playable** |
| Beta (1.7.3) | `retroeras:beta` | **playable** |
| Alpha (1.2.6) | `retroeras:alpha` | **playable** |
| Pre-Alpha (Infdev 20100618) | `retroeras:pre_alpha` | **playable** |
| Classic (0.30) | `retroeras:classic` | **playable** |
| Cave game (rd-132211) | `retroeras:cavegame` | **playable** |

1.6.4, Beta and Alpha each have their own Nether too: `retroeras:old_release_nether`, `retroeras:beta_nether` and `retroeras:alpha_nether`.

All six eras can be played in single player. The latest release is **v0.3.0**, on the GitHub Releases page of `apookash55/retro-eras`.

## Getting to an era

Build a nether-portal-shaped frame out of **mossy cobblestone** (at least 4 wide and 5 tall) and light it with flint and steel. Walking through takes you to the Beta dimension, where a matching portal is built for the trip back. Each era keeps its own inventory, ender chest and XP: what you carry in the overworld stays there.

For Old Release (1.6.4), build it out of **stone bricks**.

For Alpha (1.2.6), build it out of **pumpkins**, a nod to Alpha 1.2's Halloween Update.

For Pre-Alpha (Infdev), build it out of **bricks**, a nod to Infdev's brick pyramids.

For Classic (0.30), build it out of **sponge**, a block Classic added. Like Cave game, you arrive inside its 256×256 level.

For Cave game, build the same frame out of **cobblestone**. You arrive inside its 256×256 world wherever you light the portal.

## Installing in your own game

Retro Eras is a Fabric mod for Minecraft 26.3. It needs Fabric Loader 0.20.1 or newer, Fabric API 0.165.0+26.3, and Java 25 (the launcher's own runtime for 26.3 is fine).

1. Build it with `./gradlew build`. The mod is `build/libs/retroeras-<version>.jar`.
2. Install Fabric for 26.3 (the Fabric installer from fabricmc.net, or the version profile from `meta.fabricmc.net`), and put the mod and Fabric API in that profile's `mods` folder.
3. Play each old version once in the launcher (Beta 1.7.3, Alpha 1.2.6, Infdev 20100618, Classic c0.30_01c, rd-132211 and 1.6.4) so its jar and sounds are on your computer. The mod reads its textures and sounds from them, and an era whose jar is missing keeps modern textures.

Ready-made jars are on the GitHub Releases page. To publish a new one, set `version` in `gradle.properties`, commit, and push a matching tag (`git tag v0.4.0` then `git push origin v0.4.0`): GitHub Actions builds the mod and attaches the jar to a new release, with the install steps from `.github/release-notes.md`.

For development, `./gradlew runClient` starts the game with the mod from source.

On this machine there is a **Retro Eras** profile in the Minecraft launcher with its own game folder, `.minecraft/profiles/retro-eras`, so its worlds and settings are kept apart from your normal ones. After changing the mod, `./gradlew installMod` copies the new jar into that profile (`-PmodsDir=<folder>` points it somewhere else).

## Digging down through the eras

The eras are stacked on top of each other, newest at the top: the overworld, then Old Release, Beta, Alpha, Pre-Alpha, Classic and Cave game at the bottom (`EraLayers`). The Nethers are not part of the stack.
- **Going down:** the very lowest layer of bedrock can be mined, slowly (as long as obsidian takes), with a diamond pickaxe or better. The rest of the bedrock stays unbreakable, so you have to find a spot where the layers above it have gaps. Fall through and you drop into the sky of the next older era over the same spot (inside the map, for Classic and Cave game). That landing does not hurt. Infdev's bedrock already has holes, so you can fall from Pre-Alpha into Classic without digging. Classic's own level sits on lava, as it did, so the way down to Cave game is through the bedrock under the sea past the edge of its map.
- **Going up:** get above the top of an older era's world (pillar up to the build limit and jump, or fly) and you come up in the next newer era just above its bedrock. If the rock is solid there, a small pocket is dug for you.
- Your inventory changes with the era, just as it does through a portal.

## Gameplay in every era

- **Combat is pre-1.9.** There is no attack cooldown, so every click is a full-strength hit and the attack indicator never shows. There are no sweep attacks and no offhand: anything put in the offhand goes back into your inventory. Critical hits arrived in Beta 1.8, so only Old Release has them.
- **Weapons deal their era's damage.** In Alpha, Beta and Infdev, swords dealt 4 + 2 per tier (wood and gold 4, stone 6, iron 8, diamond 10). In 1.6.4 they dealt 4 + 1 per tier (4, 5, 6, 7). Axes, pickaxes and shovels dealt 3, 2 and 1 plus their tier in all of them, and everything else 1. Strength and weakness still apply on top.
- **Only the era's items.** Each era has a list of the items it had, in `src/main/resources/data/retroeras/era_items/`: about 120 for Infdev, 155 for Alpha, 220 for Beta and 410 for 1.6.4 (including creative-only blocks and its spawn eggs). In an era:
  - Crafting gives nothing for a newer item, and newer items on the ground cannot be picked up.
  - Drops follow the era. Iron and gold ore drop themselves rather than raw ore, Beta and Infdev zombies drop feathers, and anything the era did not have (like raw beef in Beta) is not dropped. This applies to chest loot too.
  - Anything newer that still gets into your inventory, from a furnace, a command or the creative menu, is taken away with a message. Survival items are kept for you and come back the next time you are in the modern world; creative-menu items are just removed.
- **Sword blocking in Old Release.** Holding right-click with a sword raises it, as from Beta 1.8 to 1.8.9, and cuts damage that armour could stop to (1 + damage) / 2.
- **Furnaces** in an era do not smelt anything into an item the era did not have.
- **1.6.4 hunger.** Old Release has no fast healing from saturation (added in 1.11). You heal half a heart every 4 seconds while your hunger bar is at 18 or more, which costs 3 exhaustion as it did in 1.6.4.
- **No experience before Beta 1.8.** In Beta, Alpha and Infdev no experience orbs appear, from mobs, ores or anything else.
- **Bows before Beta 1.8** fire the moment you click, with no drawing back, and never wear out. Arrows leave at Beta's speed and hit for 4.
- **Old armour.** Armour takes a flat 4% off a hit per point, with no toughness, so heavy blows are not let through. In 1.6.4 the points are each piece's. Before 1.0 any full set counted 20 points, whatever it was made of, and protected less as it wore down.
- **Old stack sizes.** Before Beta 1.8, food that heals you (apples, bread, pork, fish, golden apples and stew) does not stack, cookies stack to 8, and signs and doors do not stack. Doors do not stack in 1.6.4 either.
- **No swimming or crawling.** Both came in 1.13, so in the eras you never swim, even sprinting under water, and never drop to a crawl to fit through a gap.
- **No rain before Beta 1.5.** Alpha and Infdev never rain or snow; cold biomes still have their snow cover.
- **No breeding before Beta 1.8.** In Beta, Alpha and Infdev, holding wheat or seeds out to an animal does nothing. Wolves still take bones and heal from meat. Breeding works in Old Release.
- **Punching sheep.** Shears came in Beta 1.7, so in Alpha and Infdev hitting a sheep knocks one to three wool off it. Dyes came in Beta 1.2, so every sheep there is white.
- **Each era's own Nether.** An obsidian portal lit in 1.6.4, Beta or Alpha leads to that era's Nether, and back to the era; your era inventory comes with you. In Beta and Alpha, zombie pigmen drop cooked porkchops, as they did until Beta 1.8. Infdev, Classic and Cave game had no Nether, so a Nether portal there leads nowhere, rather than taking you to the modern Nether.
- **1.6.4's own End.** A stronghold's End portal leads to a port of 1.6.4's End: one island of end stone around the origin, with obsidian pillars and burning crystals, and an obsidian platform to arrive on. The dragon is 1.6.4's, not today's: it has no perching or breath, flies at random points or straight at a player, breaks through everything but obsidian, end stone and bedrock, throws you with its wings and does 10 damage with its head. A crystal nearby heals it and hurts it when destroyed, and only the head takes full damage. It dies where it was, and leaves the portal and egg under it. Walking into that portal plays the credits every time, then returns you to your bed in the era, or else to the surface at the era's centre. The dragon does not come back. Its health bar is still 26.3's.

## Fog

The eras have the fog of the versions they come from (`EraFog`): it starts a quarter of the way out and closes in completely at the old render distance, Far (256 blocks), Normal (128), Short (64) or Tiny (32). The older the era, the closer the fog: 1.6.4 starts on Far, Beta and Classic on Normal, Infdev on Short and Alpha on Tiny. Cave game had almost no fog, so it starts on Far. Press **R** (the "Render Distance (in eras)" key, which you can rebind under Controls) to cycle it, as F did in Beta; Shift+R goes back. "Modern" turns the old fog off. Each era remembers its own setting in `config/retroeras.properties` (`fog.beta=normal`, and so on).

Leaves in the eras are still, too: no falling leaves, dripping water or rustling, which old versions did not have.

## Achievements

Each era keeps its own (`EraAchievements`). Beta 1.7.3 has its 16 achievements and 1.6.4 its 27, with their own names, descriptions, icons and order, taken from those versions' achievement lists. They are advancements under `data/retroeras/advancement/<era>/`, generated by `tools/gen_achievements.js`.
- They are earned the way the old game awarded them: picking up wood, leather, diamonds or blaze rods; taking a crafted pickaxe, sword, hoe, workbench, furnace, bread, cake, enchantment table or bookshelf out of the crafting grid; taking iron or cooked fish out of a furnace; opening your inventory; killing a monster, a skeleton with an arrow from 50 blocks, or a ghast with its own fireball; hitting for eight hearts in one blow; riding a minecart 1 km from where you got in; flying a pig off a cliff; going through a portal to the era's Nether; brewing a potion. As then, each one needs the one before it ("Requires ..."). The End? comes from reaching 1.6.4's End and The End. from leaving it through the exit portal.
- 26.3's advancements are not earned in the eras, and an era's achievements only in that era. Alpha, Infdev, Classic and Cave game had no achievements, so nothing is earned there.
- The advancements screen shows only where you are: an era's achievements there, 26.3's outside. In Beta and 1.6.4 it is called "Achievements", and the popup says "Achievement get!".

## Settings

**Options > Retro Eras...** sets each era's fog and turns the version text on or off. Changes are saved straight away to `config/retroeras.properties`, which also takes `<era>.jar=<path>` by hand (see below).

## Lighting

Infdev, Alpha and Beta lit the world differently from 26.3 (`EraLightmap`). A block took the brighter of its sky light and its block light and shaded it in plain grey, rather than adding the two together with warm torchlight and blue nights. Daylight fell in whole steps from noon to night, a little more in rain and thunder, rather than fading smoothly. The era packs carry a replacement lightmap shader that does this, and smooth lighting is off there, as it was by default. 1.6.4 already had the coloured lightmap, so it keeps 26.3's. The Brightness option still works.

## The Far Lands

The terrain ports keep the original integer overflow, so about 12,550,821 blocks out from the centre the Beta, Alpha and Infdev worlds break into the Far Lands' walls of stone (`FarLandsTest` checks this). The 1.6.4 port wraps its noise coordinates, so its world carries on normally that far out.

## Old textures and sounds

The mod never ships Mojang's old textures. On startup it reads them from your own copies of the old client jars, which the launcher stores under `.minecraft/versions/`: `1.6.4`, `b1.7.3`, `a1.2.6`, `inf-20100618`, `c0.30_01c` and `rd-132211`. It cuts them into resource packs at `resourcepacks/retroeras_<era>/` and turns each pack on only while you are in that era. You can point it at another jar with `<era>.jar=<path>` (for example `beta.jar=...`) in `config/retroeras.properties`. If a jar is missing, that era keeps modern textures.

The tile-to-texture mappings live in `src/client/resources/assets/retroeras/era_textures/`. For Beta, it covers about 300 blocks, items, mobs, HUD icons, the font, sky and colormaps. The player skin, pig, cow, chicken, sheep, wolf and zombie are converted too. Textures whose layout changed after Beta are rebuilt from Beta's own: chests and double chests are painted onto 26.3's chest model from Beta's chest tiles, beds and redstone dust are turned to fit 26.3's faces, leather armour and water have the colour 26.3 tints them with divided out, and the inventory screen is Beta's with its crafting grid moved to where 26.3 puts it (and no offhand slot or recipe book button). Beta and Infdev drew water, lava, fire and the nether portal with code each tick; the mod runs ports of that code (`LegacyTextureEffects`) and records the animations into the pack. 1.6.4 already stores one file per texture, so its mapping (about 490 textures plus HUD regions) is generated by `tools/gen_release_texture_map.ps1` through the 1.13 renames; animated textures keep their `.mcmeta`.

Some textures every old version shared are added to each mapping by `tools/add_shared_textures.js`: the particles (each jar's particles.png, with 1.6.4's explosion; a particle a jar had no picture for yet keeps 26.3's), the buttons on the pause, death and options screens, the armour worn by players and mobs, and the paintings, cut from the one sheet they shared into 26.3's separate pictures. It also fills in what the eras still lacked: Infdev's chests, leather armour and minecart, Beta's map, and 1.6.4's enchanted golden apple and spawn eggs, which were one grey egg tinted in two colours for each mob.

Sounds come the same way, from your own copies of the old sound files, which the launcher keeps in `.minecraft/assets/` (the `pre-1.6` index for Beta and Infdev, `legacy` for 1.6.4); they go into the same era pack. Mappings live in `src/client/resources/assets/retroeras/era_sounds/` and are generated by `tools/gen_era_sounds.ps1`, which checks every event against 26.3.
- **Beta and Infdev** use the old `newsound` set: footsteps, the original "oof" hurt sound, the old mob noises, chests, doors, fire, water, portals and cave ambience. As in those versions, breaking and placing a block plays its footstep sound.
- **1.6.4** uses its own set, with separate dig sounds, per-mob sounds, horses, bats, villagers, fireworks and anvils.
- Sounds that did not exist yet, like the 1.9 combat swings, are silent. Music is limited to the era's tracks: Infdev had only the first three, and Beta and 1.6.4 the twelve game tracks of Volume Alpha.
- **Classic** had Infdev's footsteps but no music, so its music is silent.
- **Cave game** had no sound at all, so everything is silent while you are there.
If the sound files are missing (the launcher downloads them the first time you play that version), the era keeps modern sounds.

## Layout

- `src/main/java/dev/retroeras/Era.java`: the list of eras and their dimension keys.
- `src/main/java/dev/retroeras/portal/`: era portals (frame detection, lighting, linking and travel).
- `src/main/java/dev/retroeras/player/EraInventories.java`: the separate inventory, ender chest and XP for each era.
- `src/main/java/dev/retroeras/player/EraRules.java`, `EraCombat.java`, `EraItems.java` and `EraMobs.java`: hunger, combat, the item lists and mob rules.
- `src/main/java/dev/retroeras/player/EraLayers.java`: the stack of eras you dig down and climb up through.
- `src/client/java/dev/retroeras/client/texture/`: builds each era's texture pack from the old jar and switches it on and off.
- `src/client/java/dev/retroeras/client/`: the old fog (`EraFog`), lighting (`EraLightmap`), version text (`EraHud`) and the settings screen (`EraSettingsScreen`, saved by `EraConfig`).
- `src/main/java/dev/retroeras/worldgen/`: each era's terrain generator, with the eras' Nether in `nether/` and Classic in `classic/`.
- `src/main/java/dev/retroeras/entity/`: rd-161348's cave zombies, kept only so worlds from 0.2.0 still load.
- `src/main/resources/data/retroeras/`: each era's dimension, dimension type, biomes and features.
- `tools/`: scripts that generate the Beta, Alpha and 1.6.4 biome files, the 1.6.4 and Alpha texture mappings, the sound mappings, the portal textures and the achievements (`gen_achievements.js`, run with node).
- `.github/workflows/build.yml`: builds every push, and publishes a release for every `v*` tag.
- `src/gametest/`: tests. `./gradlew runGameTest` runs the headless server tests. `./gradlew runClientGameTest` opens the game, visits each era and saves screenshots to `build/run/clientGameTest/screenshots/`.

## Old Release (1.6.4)

- Terrain is a port of Minecraft 1.6.4's own generator (`src/main/java/dev/retroeras/worldgen/release/`): its noise (with 1.6's coordinate wrapping), the full GenLayer biome chain (islands, zooms, snow, mushroom islands, rivers, hills, shores, swamp rivers and the Voronoi zoom), biome-blended heights, the surface with ice on cold water, caves and ravines. A seed gives the same land and biomes it did in 1.6.4. The world is 256 tall but, as in 1.6.4, terrain stops at y=127.
- It has 1.6.4's 21 overworld biomes, from oceans and rivers to jungles, extreme hills and mushroom islands. Their grass, leaf and sky colours come from 1.6.4's colormaps and sky formula (swamps mixed the way 1.6 did). Trees, flowers, grass, reeds, cacti, mushrooms, lily pads, melons and vines follow 1.6.4's decorator, with emeralds and silverfish stone in Extreme Hills, water and lava lakes, and dungeons. The biome files come from `tools/gen_release_worldgen.ps1`.
- Mobs follow 1.6.4's lists: the farm animals, horses on plains, wolves in forests and taigas, ocelots in jungles, mooshrooms on mushroom islands, bats, and the usual monsters including endermen.
- Hunger, sprinting, XP and smooth lighting are all back, as in 1.6.4. Inventories are still kept separate from your modern ones.
- Structures are a port of 1.6.4's own (`src/main/java/dev/retroeras/worldgen/release/structure/`): mineshafts, villages in plains and deserts, desert pyramids, jungle temples, witch huts and the three strongholds. They are laid out from the seed exactly as 1.6.4 laid them out, and built with its blocks, chest loot, villagers, witch and spawners while the chunks generate. Eyes of ender lead to these strongholds. Only witches spawn inside a witch hut, and they keep spawning there, as in 1.6.4. 1.6.4 bugs are kept: strongholds never have right turns, and desert village wells stay cobblestone.
- Villagers keep 1.6.4's five professions (farmer, librarian, priest, blacksmith and butcher, as farmer, librarian, cleric, armorer and butcher) but trade as 26.3 villagers do. They wear 1.6.4's outfits, one whole skin per job, with 26.3's biome outfits and level badges taken off; the jobs 26.3 split off later wear the outfit of the 1.6.4 job they came from (a fletcher dresses as a farmer, a toolsmith as a blacksmith). Babies are half-size adults in their job's outfit, as they were, rather than 26.3's big-headed baby model.

## Beta (1.7.3)

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

## The Nether (1.6.4, Beta and Alpha)

- Terrain is a port of ChunkProviderHell (`src/main/java/dev/retroeras/worldgen/nether/`) from Beta 1.7.3, which Alpha 1.2's Halloween Update had already used, and from 1.6.4. It is 128 high and closed in by bedrock at both ends, with a lava sea below y = 32, soul sand and gravel shelves around y = 64 and the Nether's own wider, flatter caves. A seed gives the same Nether it did. Gravel shelves came out very rare in both versions; the ports sample the gravel noise the way the originals did, which seems to be why.
- Decoration follows the old Nether's: lava springs, fire on netherrack, glowstone clusters and mushrooms, and in 1.6.4 nether quartz and hidden lava pockets. The originals drew these from whatever the generator's random was left at, so they were never the same twice; here each chunk is seeded.
- 1.6.4's Nether has its nether fortresses (`Fortress`), laid out from the seed as 1.6.4 laid them out: bridges, castle corridors, nether wart rooms, blaze spawners and chests with 1.6.4's loot. Blazes, wither skeletons, zombie pigmen and magma cubes spawn in them.
- Mobs: ghasts and zombie pigmen, plus magma cubes in 1.6.4. Inventories stay the era's own: the Beta Nether is part of Beta.

## Classic (0.30)

- The level is a port of Classic 0.30's own generator (`src/main/java/dev/retroeras/worldgen/classic/`), with all the steps its loading screen named: raising the land from distorted noise, eroding, soiling, carving caves and coal, iron and gold veins, flooding the sea in from the edges, pouring lava pools, growing sand and gravel beaches and grass, and planting flowers, mushrooms and trees. It is 256×256 and 64 high, with flowing lava on the bottom layer as Classic had. Classic made a new level from the clock each time; here it is seeded from your world seed.
- Past the edge of the map lies what Classic drew there: bedrock up to two blocks below sea level, and still water over it.
- The sky is Classic's #99CCFF with white fog and clouds. The sun never moves and it never rains. No mobs spawn.
- As in Classic 0.30's building mode there is no health or hunger, blocks break instantly and drop nothing, and your inventory is kept stocked with 36 of Classic's 42 blocks, its default hotbar first. Its spring green and violet cloth have no modern wool. "0.30" sits in the top left corner, as it did.
- Classic's water and lava spread into every open block beside and below them without ever thinning out, so one spring can flood a cave or run off across the land.
- Textures come from your own `c0.30_01c` jar, with its animated water and lava.

## Cave game (rd-132211)

- This is the very first build of Minecraft the launcher offers, from 13 May 2009 (`src/main/java/dev/retroeras/worldgen/cavegame/`). Its world is a flat slab 256×256 blocks wide, solid from the bottom up to y = 42, two thirds of its 64-block height. The top layer is grass and everything under it rock. There are no hills, dirt, caves, water or trees yet, and void past the edges. Every rd-132211 world was the same, so the seed changes nothing.
- It had two textures: grass, drawn on every face of the top layer, and a cobble-like rock. Both come from your own `rd-132211` jar.
- The sky is the pale blue (#80CCFF) of the original. The sun never moves, there is no rain and there are no clouds.
- There is no health, no hunger and nothing to survive. Only falling off the edge hurts. Blocks break instantly and drop nothing, and your hotbar always holds 64 of the one block rd-132211 let you place, its rock. No mobs spawn; the zombies came a build later.

## Known gaps

- The mod is built and tested for single player only.
- The settings screen is under Options, not Mod Menu, so the mod needs no extra dependency.
- An Indev era is on hold: Mojang's launcher offers no Indev builds, so there is no jar of the player's own to port it from or take its textures from.

## Future plans

The aim is for each era to be as close to the real version as possible. What still differs, grouped by what it touches, with the eras it affects and a rough size. Next up is Beta's decorator.

### Worlds and dimensions

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| Beta's decorator (next) | Beta, Alpha | Medium | Beta's own trees, ores, flowers and dungeons, ported like its terrain, instead of feature lists. |

### Mobs

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| 1.6.4 villagers | 1.6.4 | Medium | 1.6.4's trade lists, and no sleeping, job blocks or gossip. Their outfits are done. |
| Old mob models | Alpha to 1.6.4 | Medium | 26.3 redesigned the cow, pig, sheep, chicken and wolf; the old textures are on the new shapes. |
| Old mob AI | All eras | Medium | The old wandering AI and monster targeting. Breeding and sheep wool are done. |

### Gameplay rules

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| Armour against falls and fire | Infdev, Alpha, Beta | Small | Before 1.0, armour also softened falls, fire and drowning; here it only takes off the damage 26.3 lets armour reduce. |
| Old recipes | Alpha, Beta | Small–Medium | Some recipes made different amounts, such as slabs and ladders. |

### How the world is drawn

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| Old sky | Infdev, Alpha, Beta | Small | Beta's stars, cloud speed and sunrise. The old lighting is done. |
| Flat dropped items | Cave game to Beta | Small | Dropped items were flat pictures that always faced you, not spinning models. |
| Cave game and Classic lighting | Cave game, Classic | Small | Cave game's darker shade with black fog for blocks out of the sun, and Classic's lit-or-shadow shading. |
| Old first-person view | All eras | Medium | The held item, arm swing and view bobbing are 26.3's. |
| Old block shapes | Alpha to 1.6.4 | Medium | Some blocks changed shape since, such as fences and stairs. |

### Screens

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| Map and status effect icons | Beta, 1.6.4 | Small | The map background and markers (misc/mapbg.png, mapicons.png), and 1.6.4's potion effect icons in the inventory. |
| Old screens | 1.6.4 and others | Medium | 1.6.4's inventory (its crafting grid sat elsewhere), the pause menu and the F3 screen are 26.3's. Beta's inventory is done. |

### Textures

Block textures, item icons, particles, buttons, worn armour and paintings are old in every era that had them. (Slabs, stairs, fences, buttons, pressure plates and carpets use their block's texture, so they look old too.) What is still 26.3's:

| Plan | Eras | Size | What it involves |
| --- | --- | --- | --- |
| Arrows, boats, minecarts and signs in the world | Infdev to 1.6.4 | Small–Medium | The entity textures for these (item/arrows.png, boat.png, cart.png, sign.png in Beta); 26.3's models changed shape, so some need rebuilding like the chests. |
| Chests, beds and skulls in 1.6.4 | 1.6.4 | Small–Medium | 1.6.4's chest, ender chest, trapped chest, bed and mob head textures, rebuilt for 26.3's models, as was done for Beta's chests and beds. |
| Compass and clock | Alpha to 1.6.4 | Small–Medium | Both were drawn by code each tick in the old versions, like water and fire; they need ports of that code. |
| 1.6.4 water, lava and horses | 1.6.4 | Medium | Left modern when 1.6.4's textures were mapped: its water and lava tint differently, and horses changed model. |

Multiplayer is out of scope for now.
