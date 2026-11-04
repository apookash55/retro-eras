# Retro Eras

A Fabric mod for Minecraft 26.3 that adds a dimension for each historical era of Minecraft, so you can visit them all from one world.

| Era | Dimension | Status |
| --- | --- | --- |
| Old Release (1.0 – 1.7) | `retroeras:old_release` | planned |
| Beta / Alpha | `retroeras:beta` | **playable** |
| Pre-Alpha (Infdev 20100618) | `retroeras:pre_alpha` | **playable** |
| Cave game (rd-161348) | `retroeras:cavegame` | **playable** |

## Playing the prototype

```
./gradlew runClient
```

Build a nether-portal-shaped frame out of **mossy cobblestone** (at least 4 wide and 5 tall) and light it with flint and steel. Walking through takes you to the Beta dimension, where a matching portal is built for the trip back. Each era keeps its own inventory, ender chest and XP: what you carry in the overworld stays there.

For Pre-Alpha (Infdev), build it out of **bricks**, a nod to Infdev's brick pyramids.

For Cave game, build the same frame out of **cobblestone**. You arrive inside its 256×256 world wherever you light the portal.

## Old textures

The mod never ships Mojang's old textures. On startup it reads them from your own copies of the old client jars, which the launcher stores under `.minecraft/versions/`: `b1.7.3`, `inf-20100618` and `rd-161348`. It cuts them into resource packs at `resourcepacks/retroeras_<era>/` and turns each pack on only while you are in that era. You can point it at another jar with `<era>.jar=<path>` (for example `beta.jar=...`) in `config/retroeras.properties`. If a jar is missing, that era keeps modern textures.

The tile-to-texture mappings live in `src/client/resources/assets/retroeras/era_textures/`. For Beta, it covers about 300 blocks, items, mobs, HUD icons, the font, sky and colormaps. The player skin, pig, cow, chicken, sheep, wolf and zombie are converted too. Some textures stay modern for now because their layout changed after Beta: chests, beds, redstone dust, leather armor, water, lava, fire and the inventory screen.

## Layout

- `src/main/java/dev/retroeras/Era.java`: the list of eras and their dimension keys.
- `src/main/java/dev/retroeras/portal/`: era portals (frame detection, lighting, linking and travel).
- `src/main/java/dev/retroeras/player/EraInventories.java`: the separate inventory, ender chest and XP for each era.
- `src/client/java/dev/retroeras/client/texture/`: builds each era's texture pack from the old jar and switches it on and off.
- `src/main/java/dev/retroeras/worldgen/`: each era's terrain generator.
- `src/main/resources/data/retroeras/`: each era's dimension, dimension type, biomes and features.
- `tools/`: scripts that generate the Beta biome files and the portal textures.
- `src/gametest/`: tests. `./gradlew runGameTest` runs the headless server tests. `./gradlew runClientGameTest` opens the game, visits each era and saves screenshots to `build/run/clientGameTest/screenshots/`.

## Beta prototype

- The world runs from y=0 to 127, with sea level at 64 and clouds at about y=108.
- Terrain is a port of Beta 1.7.3's own generator (`src/main/java/dev/retroeras/worldgen/beta/`): its Perlin and simplex noise, density field, climate and biome lookup, sand and gravel beaches, and caves. A seed gives the same landscape it gave in Beta 1.7.3. Trees, ores and plants still come from the biomes' feature lists, so they differ from Beta in detail.
- It has Beta's 10 biomes: rainforest, seasonal forest, forest, swampland, savanna, shrubland, plains, taiga, tundra and desert. Their grass, leaf and sky colours are computed from Beta's own colormaps and sky formula. Trees use Beta's mixes, and there are no bee nests, leaf litter, fallen logs, cactus flowers, copper, granite, diorite, andesite, tuff, geodes, glow lichen or ravines. Ores are spread over y=0–127 the way Beta placed them. The biome files come from `tools/gen_beta_worldgen.ps1`, so edit that script and re-run it rather than editing the JSON.
- Mobs are limited to Beta's: pigs, sheep, cows, chickens and wolves, plus zombies, skeletons, spiders, creepers and slimes.
- Gameplay follows Beta: there is no hunger, no sprinting and no natural healing, and food heals you straight away. The hunger and XP bars are hidden, and smooth lighting is switched off while you are there. Your modern hunger is saved and given back when you leave.

## Pre-Alpha (Infdev 20100618)

- Terrain is a port of Infdev 20100618's own generator (`src/main/java/dev/retroeras/worldgen/infdev/`), the last Infdev build and the ancestor of Beta's. It has no biomes or climate: hill size comes straight from noise, so the land is rougher than Beta's and reaches higher. Sand and gravel beaches, caves, and Infdev's bedrock floor with about one hole in six are all ported, and a seed gives the same landscape it did in Infdev.
- One biome with Infdev's green grass and leaves (their colour is part of the texture, so nothing is tinted). It has oak trees whose density varies across the land, dandelions, roses, mushrooms and springs. Ores follow Infdev: no redstone or lapis yet.
- Mobs are Infdev's six: pigs and sheep, plus zombies, skeletons, creepers and spiders. The sky is Infdev's #88BBFF.
- Rules are the same as Beta: health but no hunger, sprinting or natural healing.

## Cave game

- The world is a port of rd-161348's level generator: two diamond-square height maps give hills of grass over dirt over rock. It is 256×256 blocks and 64 tall, with void past the edges, just like the original. The original made a new random map on every launch; here it is seeded from your world seed.
- The sky is the pale blue (#80CCFF) and the fog the near-white (#FEFBFA) of the original. The sun never moves, there is no rain and there are no clouds.
- As in Cave game, there is no health, no hunger and nothing to survive. Only falling off the edge hurts. Blocks break instantly and drop nothing, and your hotbar always holds 64 each of the five blocks Cave game let you place (stone, dirt, cobblestone, planks and saplings). No mobs spawn.

## Next steps

- Build the last era, Old Release (1.0 – 1.7).
