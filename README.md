# Retro Eras

A Fabric mod for Minecraft 26.3 that adds a dimension for each historical era of Minecraft, so you can visit them all from one world.

| Era | Dimension | Status |
| --- | --- | --- |
| Old Release (1.0 – 1.7) | `retroeras:old_release` | planned |
| Beta / Alpha | `retroeras:beta` | **prototype** |
| Pre-Alpha (Indev / Infdev) | `retroeras:pre_alpha` | planned |
| Cavegame (rd-132211 era) | `retroeras:cavegame` | planned |

## Playing the prototype

```
./gradlew runClient
```

Build a nether-portal-shaped frame out of **mossy cobblestone** (at least 4 wide and 5 tall) and light it with flint and steel. Walking through takes you to the Beta dimension, where a matching portal is built for the trip back. Each era keeps its own inventory, ender chest and XP: what you carry in the overworld stays there.

## Old textures

The mod never ships Mojang's old textures. On startup it reads them from your own Beta 1.7.3 client jar, which the launcher stores at `.minecraft/versions/b1.7.3/b1.7.3.jar`. It cuts them into a resource pack at `resourcepacks/retroeras_beta/` and turns that pack on only while you are in the Beta dimension. You can point it at another jar with `beta.jar=<path>` in `config/retroeras.properties`.

The tile-to-texture mapping lives in `src/client/resources/assets/retroeras/era_textures/beta.json`. It covers about 300 blocks, items, mobs, HUD icons, the font, sky and colormaps. The player skin, pig, cow, chicken, sheep, wolf and zombie are converted too. Some textures stay modern for now because their layout changed after Beta: chests, beds, redstone dust, leather armor, water, lava, fire and the inventory screen.

## Layout

- `src/main/java/dev/retroeras/Era.java`: the list of eras and their dimension keys.
- `src/main/java/dev/retroeras/portal/`: era portals (frame detection, lighting, linking and travel).
- `src/main/java/dev/retroeras/player/EraInventories.java`: the separate inventory, ender chest and XP for each era.
- `src/client/java/dev/retroeras/client/texture/`: builds each era's texture pack from the old jar and switches it on and off.
- `src/main/resources/data/retroeras/`: each era's dimension, dimension type, noise settings, material (surface) rules and biomes. The worldgen is all data-driven JSON.
- `src/gametest/`: tests. `./gradlew runGameTest` runs the headless server tests. `./gradlew runClientGameTest` opens the game, visits each era and saves screenshots to `build/run/clientGameTest/screenshots/`.

## Beta prototype

- The world runs from y=0 to 127, with sea level at 64 and clouds at about y=108.
- Terrain is a port of Beta 1.7.3's own generator (`src/main/java/dev/retroeras/worldgen/beta/`): its Perlin and simplex noise, density field, climate and biome lookup, sand and gravel beaches, and caves. A seed gives the same landscape it gave in Beta 1.7.3. Trees, ores and plants still come from the biomes' feature lists, so they differ from Beta in detail.
- It has Beta's 10 biomes: rainforest, seasonal forest, forest, swampland, savanna, shrubland, plains, taiga, tundra and desert. Their grass, leaf and sky colours are computed from Beta's own colormaps and sky formula. Trees use Beta's mixes, and there are no bee nests, leaf litter, fallen logs, cactus flowers, copper, granite, diorite, andesite, tuff, geodes, glow lichen or ravines. Ores are spread over y=0–127 the way Beta placed them. The biome files come from `tools/gen_beta_worldgen.ps1`, so edit that script and re-run it rather than editing the JSON.
- Mobs are limited to Beta's: pigs, sheep, cows, chickens and wolves, plus zombies, skeletons, spiders, creepers and slimes.
- Gameplay follows Beta: there is no hunger, no sprinting and no natural healing, and food heals you straight away. The hunger and XP bars are hidden, and smooth lighting is switched off while you are there. Your modern hunger is saved and given back when you leave.

## Next steps

- Build the remaining eras: Old Release, Pre-Alpha (Indev finite island and Infdev) and Cavegame (a flat grass and stone slab with no mobs).
