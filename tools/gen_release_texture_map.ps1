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
  $kept = @($to | Where-Object { $modern.Contains($_) })
  foreach ($missing in @($to | Where-Object { -not $modern.Contains($_) })) { $dropped.Add("$from -> $missing") }
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
  @('snowman', 'entity/snow_golem/snow_golem'), @('cat/ocelot', 'entity/cat/ocelot'), @('chicken', 'entity/chicken/chicken_temperate'), @('sheep/sheep', 'entity/sheep/sheep'),
  @('sheep/sheep_fur', 'entity/sheep/sheep_wool'), @('wolf/wolf', 'entity/wolf/wolf'), @('wolf/wolf_tame', 'entity/wolf/wolf_tame'),
  @('wolf/wolf_angry', 'entity/wolf/wolf_angry'), @('arrow', 'entity/projectiles/arrow'), @('experience_orb', 'entity/experience/experience_orb'),
  @('enchanting_table_book', 'entity/enchantment/enchanting_table_book')
)
foreach ($e in $entities) { Add-File "assets/minecraft/textures/entity/$($e[0]).png" @($e[1]) }
foreach ($e in @(@('pig/pig', 'entity/pig/pig_temperate'), @('cow/cow', 'entity/cow/cow_temperate'), @('cow/mooshroom', 'entity/cow/mooshroom_red'), @('zombie/zombie', 'entity/zombie/zombie'))) {
  Add-File "assets/minecraft/textures/entity/$($e[0]).png" @($e[1]) 'over_modern'
}
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

# Same-named environment, GUI and misc textures.
foreach ($name in @('environment/clouds', 'environment/rain', 'environment/snow', 'font/ascii', 'colormap/grass', 'colormap/foliage',
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

$mapping = [ordered]@{
  _comment = 'Generated by tools/gen_release_texture_map.ps1 from the player''s own 1.6.4 jar; edit that script instead. Maps 1.6.4 textures onto 26.3 names. Villagers wear their 1.6.4 job outfits. Water, lava, beds, redstone dust, chests, horses, boats, minecarts, the clock, the compass and the inventory screen stay modern.'
  jar = '1.6.4'
  files = $files
  regions = $regions
  clear = $clear
}
$mapping | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8NoBOM $OutFile
Write-Host "Mapped $($files.Count) files and $($regions.Count) regions; dropped $($dropped.Count) with no 26.3 counterpart:"
$dropped | ForEach-Object { Write-Host "  $_" }

# Particles, buttons, worn armour and paintings are shared with the other eras.
node (Join-Path $PSScriptRoot 'add_shared_textures.js')
