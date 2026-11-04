param([string]$DataDir = (Join-Path $PSScriptRoot '..\src\main\resources\data\retroeras'))
# Generates the Beta era's biomes, tree/ore/cactus features and dimension from one table, so every biome shares the
# same feature order (Minecraft rejects biomes whose feature lists disagree on order).
#
# Colours come from Beta 1.7.3 itself: grass/foliage are its colormaps sampled at each biome's Beta climate
# (ColorizerGrass: x = (1 - temp) * 255, y = (1 - humidity * temp) * 255), and sky is BiomeGenBase.getSkyColorByTemp.
# Tree mixes follow BiomeGenBase.getRandomWorldGenForTrees. Only vanilla features that existed in Beta are used:
# no bee nests, leaf litter, fallen logs, cactus flowers, copper, granite/diorite/andesite or tuff.

$ErrorActionPreference = 'Stop'
function Write-Json($relative, $value) {
  $path = Join-Path $DataDir $relative
  New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
  $value | ConvertTo-Json -Depth 20 | Set-Content -Encoding utf8NoBOM $path
}
function Uniform($min, $max) { @{ type = 'minecraft:uniform'; min_inclusive = @{ absolute = $min }; max_inclusive = @{ absolute = $max } } }

# --- Trees: random selectors over vanilla's plain (decorator-free) trees -------------------------------------------
$treeMixes = [ordered]@{
  default    = @{ default = 'minecraft:oak_checked'; features = @(@{ chance = 0.1; feature = 'minecraft:fancy_oak_checked' }) }
  forest     = @{ default = 'minecraft:oak_checked'; features = @(@{ chance = 0.2; feature = 'minecraft:birch_checked' }, @{ chance = 0.1; feature = 'minecraft:fancy_oak_checked' }) }
  rainforest = @{ default = 'minecraft:oak_checked'; features = @(@{ chance = 0.33333334; feature = 'minecraft:fancy_oak_checked' }) }
  taiga      = @{ default = 'minecraft:spruce_checked'; features = @(@{ chance = 0.33333334; feature = 'minecraft:pine_checked' }) }
}
foreach ($mix in $treeMixes.Keys) {
  Write-Json "worldgen/feature/beta/trees_$mix.json" ([ordered]@{ type = 'minecraft:random_selector'; default = $treeMixes[$mix].default; features = $treeMixes[$mix].features })
}

# Placed tree features: [feature, count]. A count of 0 means "1 tree in 1 of 10 chunks", like vanilla plains.
$treePlacements = [ordered]@{
  trees_sparse     = @('default', 0)
  trees_shrubland  = @('default', 1)
  trees_swamp      = @('default', 2)
  trees_seasonal   = @('default', 3)
  trees_forest     = @('forest', 6)
  trees_rainforest = @('rainforest', 7)
  trees_taiga      = @('taiga', 6)
}
foreach ($name in $treePlacements.Keys) {
  $mix, $count = $treePlacements[$name]
  $countPlacement = if ($count -eq 0) {
    @{ type = 'minecraft:count'; count = @{ type = 'minecraft:weighted_list'; distribution = @(@{ data = 0; weight = 9 }, @{ data = 1; weight = 1 }) } }
  } else {
    @{ type = 'minecraft:count'; count = @{ type = 'minecraft:weighted_list'; distribution = @(@{ data = $count; weight = 9 }, @{ data = $count + 1; weight = 1 }) } }
  }
  Write-Json "worldgen/placed_feature/beta/$name.json" ([ordered]@{
    feature = "retroeras:beta/trees_$mix"
    placement = @(
      $countPlacement,
      @{ type = 'minecraft:in_square' },
      @{ type = 'minecraft:surface_water_depth_filter'; max_water_depth = 0 },
      @{ type = 'minecraft:heightmap'; heightmap = 'OCEAN_FLOOR' },
      @{ type = 'minecraft:biome' }
    )
  })
}

# --- Cactus without the 1.21.5 cactus flower ----------------------------------------------------------------------
Write-Json 'worldgen/feature/beta/cactus.json' ([ordered]@{
  type = 'minecraft:block_column'
  allowed_placement = @{ type = 'minecraft:matching_block_tag'; tag = 'minecraft:air' }
  direction = 'up'
  layers = @(@{ height = @{ type = 'minecraft:biased_to_bottom'; min_inclusive = 1; max_inclusive = 3 }; provider = @{ id = 'minecraft:cactus'; properties = @{ age = '0' } } })
  prioritize_tip = $false
})
$offset = @{ type = 'minecraft:trapezoid'; min = -7; max = 7; plateau = 0 }
Write-Json 'worldgen/placed_feature/beta/patch_cactus.json' ([ordered]@{
  feature = 'retroeras:beta/cactus'
  placement = @(
    @{ type = 'minecraft:rarity_filter'; chance = 6 },
    @{ type = 'minecraft:in_square' },
    @{ type = 'minecraft:heightmap'; heightmap = 'MOTION_BLOCKING' },
    @{ type = 'minecraft:biome' },
    @{ type = 'minecraft:count'; count = 10 },
    @{ type = 'minecraft:offset'; x = $offset; y = @{ type = 'minecraft:trapezoid'; min = -3; max = 3; plateau = 0 }; z = $offset },
    @{ type = 'minecraft:block_predicate_filter'; predicate = @{ type = 'minecraft:all_of'; predicates = @(
      @{ type = 'minecraft:matching_block_tag'; tag = 'minecraft:air' },
      @{ type = 'minecraft:would_survive'; state = 'minecraft:cactus' }
    ) } }
  )
})

# --- Ores, spread over Beta's y=0..127 the way ChunkProviderGenerate placed them ------------------------------------
$ores = [ordered]@{
  ore_dirt     = @('minecraft:ore_dirt', 20, (Uniform 0 127))
  ore_gravel   = @('minecraft:ore_gravel', 10, (Uniform 0 127))
  ore_coal     = @('minecraft:ore_coal', 20, (Uniform 0 127))
  ore_iron     = @('minecraft:ore_iron', 20, (Uniform 0 63))
  ore_gold     = @('minecraft:ore_gold', 2, (Uniform 0 31))
  ore_redstone = @('minecraft:ore_redstone', 8, (Uniform 0 15))
  ore_diamond  = @('minecraft:ore_diamond_small', 1, (Uniform 0 15))
  ore_lapis    = @('minecraft:ore_lapis', 1, @{ type = 'minecraft:trapezoid'; min_inclusive = @{ absolute = 0 }; max_inclusive = @{ absolute = 32 } })
}
foreach ($name in $ores.Keys) {
  $feature, $count, $height = $ores[$name]
  Write-Json "worldgen/placed_feature/beta/$name.json" ([ordered]@{
    feature = $feature
    placement = @(@{ type = 'minecraft:count'; count = $count }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = $height }, @{ type = 'minecraft:biome' })
  })
}
$oreList = @($ores.Keys | ForEach-Object { "retroeras:beta/$_" }) + @('minecraft:disk_sand', 'minecraft:disk_clay', 'minecraft:disk_gravel')

# --- Biomes ----------------------------------------------------------------------------------------------------------
# One global vegetation order; each biome picks a subset, which keeps the order consistent across biomes.
$vegetationOrder = @(
  'retroeras:beta/trees_sparse', 'retroeras:beta/trees_shrubland', 'retroeras:beta/trees_swamp', 'retroeras:beta/trees_seasonal',
  'retroeras:beta/trees_forest', 'retroeras:beta/trees_rainforest', 'retroeras:beta/trees_taiga',
  'minecraft:flower_default', 'minecraft:patch_grass_plain', 'minecraft:patch_grass_forest', 'minecraft:patch_grass_taiga_2',
  'minecraft:patch_dead_bush_2', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal',
  'minecraft:patch_sugar_cane', 'minecraft:patch_sugar_cane_desert', 'minecraft:patch_pumpkin', 'retroeras:beta/patch_cactus'
)
$common = @('minecraft:flower_default', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin')

# name: modern temperature (controls snow and rain), downfall, precipitation, Beta grass, Beta foliage, Beta sky, extra vegetation
$biomes = [ordered]@{
  tundra          = @(-0.5, 0.5, $true,  '#83b593', '#64a278', '#7fa2ff', @())
  taiga           = @(-0.5, 0.4, $true,  '#8ab87c', '#6da55b', '#7da4ff', @('retroeras:beta/trees_taiga', 'minecraft:patch_grass_taiga_2'))
  swampland       = @(0.6,  0.9, $true,  '#83bd66', '#65ab3f', '#7aa6ff', @('retroeras:beta/trees_swamp', 'minecraft:patch_grass_plain'))
  forest          = @(0.7,  0.8, $true,  '#82bf54', '#63ae2a', '#79a7ff', @('retroeras:beta/trees_forest', 'minecraft:patch_grass_forest'))
  shrubland       = @(0.8,  0.3, $true,  '#99bb5b', '#81a932', '#79a7ff', @('retroeras:beta/trees_shrubland', 'minecraft:patch_grass_plain'))
  savanna         = @(0.9,  0.1, $true,  '#a6b866', '#8fa541', '#79a7ff', @('retroeras:beta/trees_sparse', 'minecraft:patch_grass_plain'))
  plains          = @(0.9,  0.4, $true,  '#94bf49', '#7aae1d', '#77a9ff', @('retroeras:beta/trees_sparse', 'minecraft:patch_grass_plain'))
  seasonal_forest = @(0.9,  0.7, $true,  '#6bc63e', '#46b70d', '#77a9ff', @('retroeras:beta/trees_seasonal', 'minecraft:patch_grass_forest'))
  rainforest      = @(0.95, 0.95, $true, '#4ccc35', '#21be02', '#77a9ff', @('retroeras:beta/trees_rainforest', 'minecraft:patch_grass_forest'))
  desert          = @(2.0,  0.0, $false, '#b2b952', '#9fa726', '#77a9ff', @('minecraft:patch_dead_bush_2', 'minecraft:patch_sugar_cane_desert', 'retroeras:beta/patch_cactus'))
}

function Spawn($type, $weight, $count) { [ordered]@{ type = $type; weight = $weight; count = $count } }
$monsters = @((Spawn 'minecraft:spider' 10 4), (Spawn 'minecraft:zombie' 10 4), (Spawn 'minecraft:skeleton' 10 4), (Spawn 'minecraft:creeper' 10 4), (Spawn 'minecraft:slime' 10 4))
$creatures = @((Spawn 'minecraft:sheep' 12 4), (Spawn 'minecraft:pig' 10 4), (Spawn 'minecraft:chicken' 10 4), (Spawn 'minecraft:cow' 8 4))

# Old biome files from the first prototype that no longer exist in the table.
Get-ChildItem (Join-Path $DataDir 'worldgen/biome/beta') -Filter *.json -ErrorAction SilentlyContinue |
  Where-Object { -not $biomes.Contains($_.BaseName) } | Remove-Item

foreach ($name in $biomes.Keys) {
  $temp, $downfall, $precip, $grass, $foliage, $sky, $extra = $biomes[$name]
  $wanted = $common + $extra
  if ($name -eq 'desert') { $wanted = @('minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal') + $extra }
  $vegetation = @($vegetationOrder | Where-Object { $wanted -contains $_ })
  # @(...) keeps an empty list an array; a bare if-expression would turn it into null.
  $creatureSpawns = @(if ($name -eq 'desert') { } elseif ($name -eq 'taiga') { $creatures + @(Spawn 'minecraft:wolf' 2 4) } else { $creatures })
  Write-Json "worldgen/biome/beta/$name.json" ([ordered]@{
    attributes = [ordered]@{
      'minecraft:gameplay/natural_mob_spawns' = [ordered]@{
        modifier = 'overlay'
        argument = [ordered]@{ spawn_costs = @{}; spawns_by_category = [ordered]@{ creature = $creatureSpawns; monster = $monsters; water_creature = @((Spawn 'minecraft:squid' 10 4)) } }
      }
      'minecraft:visual/sky_color' = $sky
    }
    carvers = @('minecraft:cave')
    downfall = $downfall
    effects = [ordered]@{ water_color = '#3f76e4'; grass_color = $grass; foliage_color = $foliage }
    features = @(
      @(), @('minecraft:lake_lava_underground', 'minecraft:lake_lava_surface'), @(), @('minecraft:monster_room'), @(), @(),
      $oreList, @(), @('minecraft:spring_water', 'minecraft:spring_lava'), $vegetation, @('minecraft:freeze_top_layer')
    )
    has_precipitation = $precip
    temperature = $temp
  })
}

# --- Dimension: Beta 1.7.3's own terrain generator and climate-based biome placement (BetaChunkGenerator) --------
$biomeMap = [ordered]@{}
foreach ($name in $biomes.Keys) { $biomeMap[$name] = "retroeras:beta/$name" }
Write-Json 'dimension/beta.json' ([ordered]@{
  type = 'retroeras:beta'
  generator = [ordered]@{ type = 'retroeras:beta'; biome_source = [ordered]@{ type = 'retroeras:beta'; biomes = $biomeMap } }
})
Write-Host "Generated $($biomes.Count) Beta biomes into $DataDir"
