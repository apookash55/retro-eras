param(
  [string]$OldJar = (Join-Path $env:APPDATA '.minecraft\versions\1.6.4\1.6.4.jar'),
  [string]$ModernJar = (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.3\minecraft-client-only.jar'),
  [string]$OutFile = (Join-Path $PSScriptRoot '..\src\client\resources\assets\retroeras\era_textures\old_release.json')
)
# Builds the texture mapping for the Old Release (1.6.4) era. 1.6 already keeps one file per texture, so blocks and
# items map by name, through the renames of 1.13's "flattening". Every target is checked against the modern jar, and
# anything that does not exist there is dropped and reported.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Entries($jar, $prefix) {
  $zip = [IO.Compression.ZipFile]::OpenRead($jar)
  try { @($zip.Entries | Where-Object { $_.FullName -like "$prefix*" -and $_.FullName -like '*.png' } | ForEach-Object { $_.FullName }) } finally { $zip.Dispose() }
}
$modern = [Collections.Generic.HashSet[string]]::new()
foreach ($e in (Entries $ModernJar 'assets/minecraft/textures/')) { [void]$modern.Add(($e -replace '^assets/minecraft/textures/', '' -replace '\.png$', '')) }

# Animated or tinted-twice textures kept modern on purpose, and ones whose layout changed.
$skipBlocks = '^(water_|lava_|bed_|redstone_dust_|leaves_.*_opaque|command_block|destroy_stage|glass_pane_top)'
$blockRenames = [ordered]@{
  '^anvil_base$' = 'anvil'; '^anvil_top_damaged_0$' = 'anvil_top'; '^anvil_top_damaged_1$' = 'chipped_anvil_top'; '^anvil_top_damaged_2$' = 'damaged_anvil_top'
  '^brick$' = 'bricks'; '^(carrots|potatoes|cocoa|nether_wart|wheat)_stage_(\d)$' = '$1_stage$2'; '^cobblestone_mossy$' = 'mossy_cobblestone'
  '^comparator_off$' = 'comparator'; '^deadbush$' = 'dead_bush'; '^(dispenser|dropper)_front_horizontal$' = '$1_front'
  '^door_iron_lower$' = 'iron_door_bottom'; '^door_iron_upper$' = 'iron_door_top'; '^door_wood_lower$' = 'oak_door_bottom'; '^door_wood_upper$' = 'oak_door_top'
  '^endframe_(eye|side|top)$' = 'end_portal_frame_$1'; '^farmland_dry$' = 'farmland'; '^farmland_wet$' = 'farmland_moist'
  '^fire_layer_(\d)$' = 'fire_$1'; '^flower_dandelion$' = 'dandelion'; '^flower_rose$' = 'poppy'; '^furnace_front_off$' = 'furnace_front'
  '^grass_side$' = 'grass_block_side'; '^grass_side_overlay$' = 'grass_block_side_overlay'; '^grass_side_snowed$' = 'grass_block_snow'; '^grass_top$' = 'grass_block_top'
  '^hardened_clay$' = 'terracotta'; '^hardened_clay_stained_silver$' = 'light_gray_terracotta'; '^hardened_clay_stained_(\w+)$' = '$1_terracotta'
  '^itemframe_background$' = 'item_frame'; '^leaves_(\w+)$' = '$1_leaves'; '^log_(\w+)_top$' = '$1_log_top'; '^log_(\w+)$' = '$1_log'
  '^melon_stem_connected$' = 'attached_melon_stem'; '^melon_stem_disconnected$' = 'melon_stem'; '^mob_spawner$' = 'spawner'
  '^mushroom_block_skin_(brown|red)$' = '$1_mushroom_block'; '^mushroom_block_skin_stem$' = 'mushroom_stem'; '^mushroom_(brown|red)$' = '$1_mushroom'
  '^nether_brick$' = 'nether_bricks'; '^noteblock$' = 'note_block'; '^piston_top_normal$' = 'piston_top'; '^planks_(\w+)$' = '$1_planks'
  '^portal$' = 'nether_portal'; '^pumpkin_face_off$' = 'carved_pumpkin'; '^pumpkin_face_on$' = 'jack_o_lantern'
  '^pumpkin_stem_connected$' = 'attached_pumpkin_stem'; '^pumpkin_stem_disconnected$' = 'pumpkin_stem'
  '^quartz_block_chiseled$' = 'chiseled_quartz_block'; '^quartz_block_chiseled_top$' = 'chiseled_quartz_block_top'
  '^quartz_block_lines$' = 'quartz_pillar_side'; '^quartz_block_lines_top$' = 'quartz_pillar_top'; '^quartz_ore$' = 'nether_quartz_ore'
  '^rail_activator$' = 'activator_rail'; '^rail_activator_powered$' = 'activator_rail_on'; '^rail_detector$' = 'detector_rail'
  '^rail_detector_powered$' = 'detector_rail_on'; '^rail_golden$' = 'powered_rail'; '^rail_golden_powered$' = 'powered_rail_on'
  '^rail_normal$' = 'rail'; '^rail_normal_turned$' = 'rail_corner'; '^redstone_lamp_off$' = 'redstone_lamp'; '^redstone_torch_on$' = 'redstone_torch'
  '^reeds$' = 'sugar_cane'; '^repeater_off$' = 'repeater'; '^sandstone_carved$' = 'chiseled_sandstone'; '^sandstone_normal$' = 'sandstone'
  '^sandstone_smooth$' = 'cut_sandstone'; '^sapling_(\w+)$' = '$1_sapling'; '^stonebrick$' = 'stone_bricks'
  '^stonebrick_(carved)$' = 'chiseled_stone_bricks'; '^stonebrick_(cracked|mossy)$' = '$1_stone_bricks'
  '^stone_slab_side$' = 'smooth_stone_slab_side'; '^stone_slab_top$' = 'smooth_stone'; '^tallgrass$' = 'short_grass'; '^torch_on$' = 'torch'
  '^trapdoor$' = 'oak_trapdoor'; '^trip_wire$' = 'tripwire'; '^trip_wire_source$' = 'tripwire_hook'; '^waterlily$' = 'lily_pad'; '^web$' = 'cobweb'
  '^wool_colored_silver$' = 'light_gray_wool'; '^wool_colored_(\w+)$' = '$1_wool'
}
$skipItems = '^(clock|compass|quiver|ruby|skull_|spawn_egg|bed$|potion_overlay|empty_armor_slot)'
$itemRenames = [ordered]@{
  '^apple_golden$' = 'golden_apple'; '^beef_cooked$' = 'cooked_beef'; '^beef_raw$' = 'beef'; '^boat$' = 'oak_boat'
  '^book_enchanted$' = 'enchanted_book'; '^book_normal$' = 'book'; '^book_writable$' = 'writable_book'; '^book_written$' = 'written_book'
  '^bow_standby$' = 'bow'; '^bucket_empty$' = 'bucket'; '^bucket_(lava|milk|water)$' = '$1_bucket'; '^carrot_golden$' = 'golden_carrot'
  '^chicken_cooked$' = 'cooked_chicken'; '^chicken_raw$' = 'chicken'; '^door_iron$' = 'iron_door'; '^door_wood$' = 'oak_door'
  '^dye_powder_silver$' = 'light_gray_dye'; '^dye_powder_black$' = 'ink_sac|black_dye'; '^dye_powder_blue$' = 'lapis_lazuli|blue_dye'
  '^dye_powder_brown$' = 'cocoa_beans|brown_dye'; '^dye_powder_white$' = 'bone_meal|white_dye'; '^dye_powder_(\w+)$' = '$1_dye'
  '^fireball$' = 'fire_charge'; '^fireworks$' = 'firework_rocket'; '^fireworks_charge$' = 'firework_star'; '^fireworks_charge_overlay$' = 'firework_star_overlay'
  '^fishing_rod_uncast$' = 'fishing_rod'; '^fish_cooked$' = 'cooked_cod'; '^fish_raw$' = 'cod'
  '^gold_(axe|boots|chestplate|helmet|hoe|horse_armor|leggings|pickaxe|shovel|sword)$' = 'golden_$1'
  '^map_empty$' = 'map'; '^map_filled$' = 'filled_map'; '^melon$' = 'melon_slice'; '^melon_speckled$' = 'glistering_melon_slice'
  '^minecart_normal$' = 'minecart'; '^minecart_(\w+)$' = '$1_minecart'; '^netherbrick$' = 'nether_brick'
  '^porkchop_cooked$' = 'cooked_porkchop'; '^porkchop_raw$' = 'porkchop'; '^potato_baked$' = 'baked_potato'; '^potato_poisonous$' = 'poisonous_potato'
  '^potion_bottle_drinkable$' = 'potion'; '^potion_bottle_empty$' = 'glass_bottle'; '^potion_bottle_splash$' = 'splash_potion'
  '^record_(\w+)$' = 'music_disc_$1'; '^redstone_dust$' = 'redstone'; '^reeds$' = 'sugar_cane'; '^seeds_(\w+)$' = '$1_seeds'
  '^sign$' = 'oak_sign'; '^slimeball$' = 'slime_ball'; '^spider_eye_fermented$' = 'fermented_spider_eye'; '^wood_(\w+)$' = 'wooden_$1'
}

$files = [Collections.Generic.List[object]]::new()
$dropped = [Collections.Generic.List[string]]::new()
function Add-File($from, [string[]]$to, $transform) {
  # A target may end in |rot270, |flipx and the like: the texture it names is the part before that.
  $kept = @($to | Where-Object { $modern.Contains($_.Split('|')[0]) })
  foreach ($missing in @($to | Where-Object { -not $modern.Contains($_.Split('|')[0]) })) { $dropped.Add("$from -> $missing") }
  if ($kept.Count -gt 0) {
    $entry = [ordered]@{ from = $from; to = $kept }
    if ($transform) { $entry.transform = $transform }
    $files.Add($entry)
  }
}
function Rename($name, $renames) {
  foreach ($pattern in $renames.Keys) { if ($name -match $pattern) { return ($name -replace $pattern, $renames[$pattern]) } }
  $name
}

foreach ($path in (Entries $OldJar 'assets/minecraft/textures/blocks/')) {
  $name = [IO.Path]::GetFileNameWithoutExtension($path)
  if ($name -match $skipBlocks) { continue }
  Add-File $path @((Rename $name $blockRenames).Split('|') | ForEach-Object { "block/$_" })
}
foreach ($path in (Entries $OldJar 'assets/minecraft/textures/items/')) {
  $name = [IO.Path]::GetFileNameWithoutExtension($path)
  if ($name -match $skipItems) { continue }
  Add-File $path @((Rename $name $itemRenames).Split('|') | ForEach-Object { "item/$_" })
}

# Entities whose 1.6.4 layout still matches 26.3. Horses, chests, boats and minecarts changed shape since.
$skins = @('steve', 'alex', 'ari', 'efe', 'kai', 'makena', 'noor', 'sunny', 'zuri') | ForEach-Object { "entity/player/wide/$_"; "entity/player/slim/$_" }
$entities = @(
  @('creeper/creeper', 'entity/creeper/creeper'), @('creeper/creeper_armor', 'entity/creeper/creeper_armor'),
  @('skeleton/skeleton', 'entity/skeleton/skeleton'), @('skeleton/wither_skeleton', 'entity/skeleton/wither_skeleton'),
  @('spider/spider', 'entity/spider/spider'), @('spider/cave_spider', 'entity/spider/cave_spider'), @('spider_eyes', 'entity/spider/spider_eyes'),
  @('slime/slime', 'entity/slime/slime'), @('slime/magmacube', 'entity/slime/magmacube'), @('squid', 'entity/squid/squid'),
  @('enderman/enderman', 'entity/enderman/enderman'), @('enderman/enderman_eyes', 'entity/enderman/enderman_eyes'),
  @('blaze', 'entity/blaze/blaze'), @('silverfish', 'entity/silverfish/silverfish'), @('witch', 'entity/witch/witch'), @('iron_golem', 'entity/iron_golem/iron_golem'),
  @('snowman', 'entity/snow_golem/snow_golem'), @('cat/ocelot', 'entity/cat/ocelot'), @('chicken', 'entity/chicken/chicken_temperate', 'entity/chicken/chicken_temperate_baby'), @('sheep/sheep', 'entity/sheep/sheep', 'entity/sheep/sheep_baby'),
  @('sheep/sheep_fur', 'entity/sheep/sheep_wool', 'entity/sheep/sheep_wool_baby'), @('wolf/wolf', 'entity/wolf/wolf', 'entity/wolf/wolf_baby'), @('wolf/wolf_tame', 'entity/wolf/wolf_tame', 'entity/wolf/wolf_tame_baby'),
  @('wolf/wolf_angry', 'entity/wolf/wolf_angry', 'entity/wolf/wolf_angry_baby'), @('arrow', 'entity/projectiles/arrow'), @('experience_orb', 'entity/experience/experience_orb'),
  @('enchanting_table_book', 'entity/enchantment/enchanting_table_book'), @('enderdragon/dragon', 'entity/enderdragon/dragon'),
  @('enderdragon/dragon_exploding', 'entity/enderdragon/dragon_exploding'), @('enderdragon/dragon_eyes', 'entity/enderdragon/dragon_eyes'),
  @('endercrystal/endercrystal', 'entity/end_crystal/end_crystal'), @('endercrystal/endercrystal_beam', 'entity/end_crystal/end_crystal_beam'), @('end_portal', 'entity/end_portal/end_portal'),
  @('boat', 'entity/boat/oak'), @('minecart', 'entity/minecart/minecart')
)
foreach ($e in $entities) { Add-File "assets/minecraft/textures/entity/$($e[0]).png" @($e[1..($e.Length - 1)]) }
# Old cows and pigs are on 64 by 32 models (OldMobModels), and babies wear the adult's texture.
foreach ($e in @(@('pig/pig', 'entity/pig/pig_temperate'), @('cow/cow', 'entity/cow/cow_temperate'), @('cow/mooshroom', 'entity/cow/mooshroom_red'))) {
  Add-File "assets/minecraft/textures/entity/$($e[0]).png" @($e[1], "$($e[1])_baby")
}
Add-File 'assets/minecraft/textures/entity/zombie/zombie.png' @('entity/zombie/zombie') 'over_modern'
Add-File 'assets/minecraft/textures/entity/steve.png' $skins 'legacy_skin'
$ghast = [ordered]@{ from = 'assets/minecraft/textures/entity/ghast/ghast.png'; to = @('entity/ghast/ghast'); scale = 2 }
$ghastShooting = [ordered]@{ from = 'assets/minecraft/textures/entity/ghast/ghast_shooting.png'; to = @('entity/ghast/ghast_shooting'); scale = 2 }
$files.Add($ghast); $files.Add($ghastShooting)

# Villagers: 26.3 layers a base skin, a biome outfit, a job outfit and a job-level badge. 1.6.4 had one whole skin per
# job, in the same layout, so each job's outfit becomes the old skin and the biome outfits and badges are cleared.
# 1.6.4's five jobs covered what 26.3 later split up: the blacksmith became the armourer, toolsmith and weaponsmith, the
# farmer sold fish, wool and arrows, the butcher leather and the librarian maps, so those jobs wear the old outfits too.
$villager = 'assets/minecraft/textures/entity/villager'
$jobs = [ordered]@{
  'farmer' = @('farmer', 'fisherman', 'shepherd', 'fletcher'); 'librarian' = @('librarian', 'cartographer'); 'priest' = @('cleric')
  'smith' = @('armorer', 'toolsmith', 'weaponsmith'); 'butcher' = @('butcher', 'leatherworker'); 'villager' = @('nitwit', 'mason')
}
foreach ($old in $jobs.Keys) { Add-File "$villager/$old.png" @($jobs[$old] | ForEach-Object { "entity/villager/profession/$_" }) 'villager' }
Add-File "$villager/villager.png" @('entity/villager/villager') 'villager'
Add-File 'assets/minecraft/textures/entity/zombie/zombie_villager.png' @('entity/zombie_villager/zombie_villager') 'villager'
$clear = [Collections.Generic.List[string]]::new()
foreach ($name in $modern) {
  if ($name -match '^entity/(zombie_)?villager/(type|profession_level)/' -or $name -match '^entity/zombie_villager/profession/') { $clear.Add($name) }
}

# 1.6.4 had no offhand slot or recipe book.
foreach ($name in @('gui/sprites/container/slot/shield', 'gui/sprites/recipe_book/button', 'gui/sprites/recipe_book/button_highlighted')) { $clear.Add($name) }

# Same-named environment, GUI and misc textures.
foreach ($name in @('environment/clouds', 'environment/end_sky', 'environment/rain', 'environment/snow', 'font/ascii', 'colormap/grass', 'colormap/foliage',
  'misc/pumpkinblur', 'misc/shadow', 'misc/underwater', 'misc/vignette', 'gui/container/crafting_table', 'gui/container/generic_54',
  'gui/container/furnace', 'gui/container/dispenser', 'gui/container/hopper')) {
  Add-File "assets/minecraft/textures/$name.png" @($name)
}
Add-File 'assets/minecraft/textures/environment/sun.png' @('environment/celestial/sun')

# 1.6 keeps the moon phases on one 4x2 sheet; 26.3 has one texture per phase, in the same order.
$regions = [Collections.Generic.List[object]]::new()
$phases = @('full_moon', 'waning_gibbous', 'third_quarter', 'waning_crescent', 'new_moon', 'waxing_crescent', 'first_quarter', 'waxing_gibbous')
for ($i = 0; $i -lt 8; $i++) {
  $regions.Add([ordered]@{ from = 'assets/minecraft/textures/environment/moon_phases.png'; x = ($i % 4) * 32; y = [Math]::Floor($i / 4) * 32; w = 32; h = 32; to = @("environment/celestial/moon/$($phases[$i])") })
}
$icons = 'assets/minecraft/textures/gui/icons.png'
foreach ($r in @(
  @('assets/minecraft/textures/gui/widgets.png', 0, 0, 182, 22, 'hud/hotbar'), @('assets/minecraft/textures/gui/widgets.png', 0, 22, 24, 23, 'hud/hotbar_selection'),
  @($icons, 0, 0, 15, 15, 'hud/crosshair'), @($icons, 16, 0, 9, 9, 'hud/heart/container'), @($icons, 52, 0, 9, 9, 'hud/heart/full'),
  @($icons, 61, 0, 9, 9, 'hud/heart/half'), @($icons, 16, 9, 9, 9, 'hud/armor_empty'), @($icons, 25, 9, 9, 9, 'hud/armor_half'),
  @($icons, 34, 9, 9, 9, 'hud/armor_full'), @($icons, 16, 18, 9, 9, 'hud/air'), @($icons, 25, 18, 9, 9, 'hud/air_bursting'),
  @($icons, 16, 27, 9, 9, 'hud/food_empty'), @($icons, 52, 27, 9, 9, 'hud/food_full'), @($icons, 61, 27, 9, 9, 'hud/food_half'),
  @($icons, 0, 64, 182, 5, 'hud/experience_bar_background'), @($icons, 0, 69, 182, 5, 'hud/experience_bar_progress'))) {
  $target = "gui/sprites/$($r[5])"
  if ($modern.Contains($target)) { $regions.Add([ordered]@{ from = $r[0]; x = $r[1]; y = $r[2]; w = $r[3]; h = $r[4]; to = @($target) }) } else { $dropped.Add("$($r[0]) region -> $target") }
}

# Water and lava: same strips as 26.3's. 1.6.4's water had its colour drawn in, so 26.3's tint is divided back out.
Add-File 'assets/minecraft/textures/blocks/water_still.png' @('block/water_still|untint:3F76E4')
Add-File 'assets/minecraft/textures/blocks/water_flow.png' @('block/water_flow|untint:3F76E4')
Add-File 'assets/minecraft/textures/blocks/lava_still.png' @('block/lava_still')
Add-File 'assets/minecraft/textures/blocks/lava_flow.png' @('block/lava_flow')
# Horses: 1.6.4's own 128 by 128 model and textures (OldMobModels). Foals wore the adult's.
foreach ($name in @('horse_white', 'horse_creamy', 'horse_chestnut', 'horse_brown', 'horse_black', 'horse_gray', 'horse_darkbrown', 'horse_skeleton', 'horse_zombie', 'donkey', 'mule', 'horse_markings_white', 'horse_markings_whitefield', 'horse_markings_whitedots', 'horse_markings_blackdots')) {
  Add-File "assets/minecraft/textures/entity/horse/$name.png" @("entity/horse/$name", "entity/horse/$($name)_baby")
}
# 1.6 keeps the compass and clock as strips with one frame per angle, in 26.3's order.
for ($i = 0; $i -lt 32; $i++) { $regions.Add([ordered]@{ from = 'assets/minecraft/textures/items/compass.png'; x = 0; y = $i * 16; w = 16; h = 16; to = @(('item/compass_{0:d2}' -f $i)) }) }
for ($i = 0; $i -lt 64; $i++) { $regions.Add([ordered]@{ from = 'assets/minecraft/textures/items/clock.png'; x = 0; y = $i * 16; w = 16; h = 16; to = @(('item/clock_{0:d2}' -f $i)) }) }
# Map markers and potion icons shared sheets; 26.3 has one texture each.
$mapIcons = @('player', 'frame', 'red_marker', 'blue_marker', 'target_x', 'target_point', 'player_off_map', 'player_off_limits')
for ($i = 0; $i -lt 8; $i++) { $regions.Add([ordered]@{ from = 'assets/minecraft/textures/map/map_icons.png'; x = ($i % 4) * 8; y = [Math]::Floor($i / 4) * 8; w = 8; h = 8; to = @("map/decorations/$($mapIcons[$i])") }) }
Add-File 'assets/minecraft/textures/map/map_background.png' @('map/map_background')
$effects = @('speed', 'slowness', 'haste', 'mining_fatigue', 'strength', 'weakness', 'poison', 'regeneration', 'invisibility', 'hunger', 'jump_boost', 'nausea', 'night_vision', 'blindness', 'resistance', 'fire_resistance', 'water_breathing', 'wither', 'absorption')
for ($i = 0; $i -lt $effects.Count; $i++) { $regions.Add([ordered]@{ from = 'assets/minecraft/textures/gui/container/inventory.png'; x = ($i % 8) * 18; y = 198 + [Math]::Floor($i / 8) * 18; w = 18; h = 18; to = @("mob_effect/$($effects[$i])") + @(if ($effects[$i] -eq 'absorption') { 'mob_effect/health_boost' }) }) }
Add-File 'assets/minecraft/textures/blocks/bed_feet_top.png' @('block/red_bed_foot_up|rot270')
Add-File 'assets/minecraft/textures/blocks/bed_head_top.png' @('block/red_bed_head_up|rot270')
Add-File 'assets/minecraft/textures/blocks/bed_feet_end.png' @('block/red_bed_foot_south')
Add-File 'assets/minecraft/textures/blocks/bed_feet_side.png' @('block/red_bed_foot_east', 'block/red_bed_foot_west|flipx')
Add-File 'assets/minecraft/textures/blocks/bed_head_side.png' @('block/red_bed_head_east', 'block/red_bed_head_west|flipx')
Add-File 'assets/minecraft/textures/blocks/bed_head_end.png' @('block/bed_head_north')
Add-File 'assets/minecraft/textures/blocks/planks_oak.png' @('block/oak_planks', 'block/bed_down')

# 1.6's chests were drawn flipped over, with the front one face round, so 26.3's chest has the same faces upside down and moved.
function Part($file, $sx, $sy, $w, $h, $dx, $dy) { [ordered]@{ file = $file; src = @($sx, $sy, $w, $h); dst = @($dx, $dy); flipy = $true } }
$compose = [Collections.Generic.List[object]]::new()
$chests = 'assets/minecraft/textures/entity/chest'
foreach ($name in @('normal', 'trapped', 'christmas', 'ender')) {
  $f = "$chests/$name.png"
  $parts = @((Part $f 0 0 6 5 0 0), (Part $f 28 0 14 14 14 0), (Part $f 14 0 14 14 28 0), (Part $f 28 14 14 5 0 14), (Part $f 42 14 14 5 14 14), (Part $f 0 14 14 5 28 14), (Part $f 14 14 14 5 42 14),
    (Part $f 28 19 14 14 14 19), (Part $f 14 19 14 14 28 19), (Part $f 28 33 14 10 0 33), (Part $f 42 33 14 10 14 33), (Part $f 0 33 14 10 28 33), (Part $f 14 33 14 10 42 33))
  $compose.Add([ordered]@{ to = @("entity/chest/$name"); size = @(64, 64); parts = $parts })
}
foreach ($name in @('normal', 'trapped', 'christmas')) {
  $f = "$chests/$($name)_double.png"
  foreach ($half in @(@('left', 0), @('right', 15))) {
    $o = $half[1]; $back = 15 - $o
    $parts = @((Part $f 0 0 6 5 0 0), (Part $f (44 + $o) 0 15 14 14 0), (Part $f (14 + $o) 0 15 14 29 0), (Part $f 44 14 14 5 0 14), (Part $f (58 + $back) 14 15 5 14 14), (Part $f 0 14 14 5 29 14), (Part $f (14 + $o) 14 15 5 43 14),
      (Part $f (44 + $o) 19 15 14 14 19), (Part $f (14 + $o) 19 15 14 29 19), (Part $f 44 33 14 10 0 33), (Part $f (58 + $back) 33 15 10 14 33), (Part $f 0 33 14 10 29 33), (Part $f (14 + $o) 33 15 10 43 33))
    $compose.Add([ordered]@{ to = @("entity/chest/$($name)_$($half[0])"); size = @(64, 64); parts = $parts })
  }
}
# 1.6.4's inventory, with its crafting grid moved to where 26.3 puts it, as Beta's is. There is no offhand slot, as in 1.6.4.
$inventory = 'assets/minecraft/textures/gui/container/inventory.png'
$compose.Add([ordered]@{ to = @('gui/container/inventory'); base = $inventory; parts = @(
  [ordered]@{ fill = 'FFC6C6C6'; dst = @(86, 24, 77, 39) },
  [ordered]@{ file = $inventory; src = @(86, 24, 77, 39); dst = @(96, 16) }) })
$signFile = 'assets/minecraft/textures/entity/sign.png'
$compose.Add([ordered]@{ to = @('block/oak_sign'); size = @(32, 32); parts = @(
  foreach ($p in @(@(2, 2, 24, 12, 0, 2), @(28, 2, 24, 12, 0, 16), @(26, 2, 2, 12, 24, 2), @(0, 2, 2, 12, 24, 16), @(2, 0, 24, 2, 0, 0), @(26, 0, 24, 2, 0, 28), @(2, 16, 2, 14, 28, 0), @(6, 16, 2, 14, 28, 16), @(4, 16, 2, 14, 30, 0), @(0, 16, 2, 14, 30, 16), @(4, 14, 2, 2, 28, 30))) {
    [ordered]@{ file = $signFile; src = @($p[0], $p[1], $p[2], $p[3]); dst = @($p[4], $p[5]) }
  }) })

$mapping = [ordered]@{
  _comment = 'Generated by tools/gen_release_texture_map.ps1 from the player''s own 1.6.4 jar; edit that script instead. Maps 1.6.4 textures onto 26.3 names. Villagers wear their 1.6.4 job outfits. The dragon, its crystals and the End''s sky and portal are 1.6.4''s. Redstone dust stays modern.'
  jar = '1.6.4'
  files = $files
  regions = $regions
  clear = $clear
  compose = $compose
}
$mapping | ConvertTo-Json -Depth 10 | Set-Content -Encoding utf8NoBOM $OutFile
Write-Host "Mapped $($files.Count) files and $($regions.Count) regions; dropped $($dropped.Count) with no 26.3 counterpart:"
$dropped | ForEach-Object { Write-Host "  $_" }

# Particles, buttons, worn armour and paintings are shared with the other eras.
node (Join-Path $PSScriptRoot 'add_shared_textures.js')
