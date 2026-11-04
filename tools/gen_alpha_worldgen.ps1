param([string]$DataDir = (Join-Path $PSScriptRoot '..\src\main\resources\data\retroeras'))
# Generates the Alpha era's biomes, tree and ore features and dimension from one table. Alpha 1.2.6 had the same
# climate, biome table and terrain as Beta 1.7.3 (BetaChunkGenerator with sandstone off), but a plainer decorator:
# only oak and big oak trees (none at all on plains, desert or tundra), dandelions, roses, mushrooms, reeds, pumpkins
# and cacti, no tall grass, ferns, dead bushes, lapis, wolves or squid (ChunkProviderGenerate.populate in a1.2.6).
#
# Colours are Beta's, which Alpha shared: grass/foliage are its colormaps sampled at each biome's Beta climate
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
# Alpha picked one tree type per chunk: a big oak 1 time in 10 (1 in 3 in rainforests), otherwise oak.
$treeMixes = [ordered]@{
  default    = @{ default = 'minecraft:oak_checked'; features = @(@{ chance = 0.1; feature = 'minecraft:fancy_oak_checked' }) }
  rainforest = @{ default = 'minecraft:oak_checked'; features = @(@{ chance = 0.4; feature = 'minecraft:fancy_oak_checked' }) }
}
foreach ($mix in $treeMixes.Keys) {
  Write-Json "worldgen/feature/alpha/trees_$mix.json" ([ordered]@{ type = 'minecraft:random_selector'; default = $treeMixes[$mix].default; features = $treeMixes[$mix].features })
}

# Placed tree features: [feature, count]. A count of 0 means "1 tree in 1 of 10 chunks", like vanilla plains.
# Alpha's count was a noise value averaging about 2, plus 5 in forests, rainforests and taigas and 2 in seasonal
# forests; other biomes only got the 1-in-10 chance of a tree, and plains, deserts and tundra none at all.
$treePlacements = [ordered]@{
  trees_sparse     = @('default', 0)
  trees_seasonal   = @('default', 4)
  trees_forest     = @('default', 7)
  trees_rainforest = @('rainforest', 7)
}
foreach ($name in $treePlacements.Keys) {
  $mix, $count = $treePlacements[$name]
  $countPlacement = if ($count -eq 0) {
    @{ type = 'minecraft:count'; count = @{ type = 'minecraft:weighted_list'; distribution = @(@{ data = 0; weight = 9 }, @{ data = 1; weight = 1 }) } }
  } else {
    @{ type = 'minecraft:count'; count = @{ type = 'minecraft:weighted_list'; distribution = @(@{ data = $count; weight = 9 }, @{ data = $count + 1; weight = 1 }) } }
  }
  Write-Json "worldgen/placed_feature/alpha/$name.json" ([ordered]@{
    feature = "retroeras:alpha/trees_$mix"
    placement = @(
      $countPlacement,
      @{ type = 'minecraft:in_square' },
      @{ type = 'minecraft:surface_water_depth_filter'; max_water_depth = 0 },
      @{ type = 'minecraft:heightmap'; heightmap = 'OCEAN_FLOOR' },
      @{ type = 'minecraft:biome' }
    )
  })
}

# Cacti reuse Beta's flowerless cactus feature (retroeras:beta/patch_cactus).

# --- Ores, spread over y=0..127 the way Alpha placed them (no lapis yet) ------------------------------------
$ores = [ordered]@{
  ore_dirt     = @('minecraft:ore_dirt', 20, (Uniform 0 127))
  ore_gravel   = @('minecraft:ore_gravel', 10, (Uniform 0 127))
  ore_coal     = @('minecraft:ore_coal', 20, (Uniform 0 127))
  ore_iron     = @('minecraft:ore_iron', 20, (Uniform 0 63))
  ore_gold     = @('minecraft:ore_gold', 2, (Uniform 0 31))
  ore_redstone = @('minecraft:ore_redstone', 8, (Uniform 0 15))
  ore_diamond  = @('minecraft:ore_diamond_small', 1, (Uniform 0 15))
}
foreach ($name in $ores.Keys) {
  $feature, $count, $height = $ores[$name]
  Write-Json "worldgen/placed_feature/alpha/$name.json" ([ordered]@{
    feature = $feature
    placement = @(@{ type = 'minecraft:count'; count = $count }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = $height }, @{ type = 'minecraft:biome' })
  })
}
$oreList = @($ores.Keys | ForEach-Object { "retroeras:alpha/$_" }) + @('minecraft:disk_sand', 'minecraft:disk_clay', 'minecraft:disk_gravel')

# --- Biomes ----------------------------------------------------------------------------------------------------------
# One global vegetation order; each biome picks a subset, which keeps the order consistent across biomes.
$vegetationOrder = @(
  'retroeras:alpha/trees_sparse', 'retroeras:alpha/trees_seasonal', 'retroeras:alpha/trees_forest', 'retroeras:alpha/trees_rainforest',
  'minecraft:flower_default', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal',
  'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin', 'retroeras:beta/patch_cactus'
)
$common = @('minecraft:flower_default', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin')

# name: modern temperature (controls snow and rain), downfall, precipitation, grass, foliage, sky, extra vegetation
$biomes = [ordered]@{
  tundra          = @(-0.5, 0.5, $true,  '#83b593', '#64a278', '#7fa2ff', @())
  taiga           = @(-0.5, 0.4, $true,  '#8ab87c', '#6da55b', '#7da4ff', @('retroeras:alpha/trees_forest'))
  swampland       = @(0.6,  0.9, $true,  '#83bd66', '#65ab3f', '#7aa6ff', @('retroeras:alpha/trees_sparse'))
  forest          = @(0.7,  0.8, $true,  '#82bf54', '#63ae2a', '#79a7ff', @('retroeras:alpha/trees_forest'))
  shrubland       = @(0.8,  0.3, $true,  '#99bb5b', '#81a932', '#79a7ff', @('retroeras:alpha/trees_sparse'))
  savanna         = @(0.9,  0.1, $true,  '#a6b866', '#8fa541', '#79a7ff', @('retroeras:alpha/trees_sparse'))
  plains          = @(0.9,  0.4, $true,  '#94bf49', '#7aae1d', '#77a9ff', @())
  seasonal_forest = @(0.9,  0.7, $true,  '#6bc63e', '#46b70d', '#77a9ff', @('retroeras:alpha/trees_seasonal'))
  rainforest      = @(0.95, 0.95, $true, '#4ccc35', '#21be02', '#77a9ff', @('retroeras:alpha/trees_rainforest'))
  desert          = @(2.0,  0.0, $false, '#b2b952', '#9fa726', '#77a9ff', @('retroeras:beta/patch_cactus'))
}

function Spawn($type, $weight, $count) { [ordered]@{ type = $type; weight = $weight; count = $count } }
# Alpha's mobs: no wolves (Beta 1.4) and no squid (Beta 1.2).
$monsters = @((Spawn 'minecraft:spider' 10 4), (Spawn 'minecraft:zombie' 10 4), (Spawn 'minecraft:skeleton' 10 4), (Spawn 'minecraft:creeper' 10 4), (Spawn 'minecraft:slime' 10 4))
$creatures = @((Spawn 'minecraft:sheep' 12 4), (Spawn 'minecraft:pig' 10 4), (Spawn 'minecraft:chicken' 10 4), (Spawn 'minecraft:cow' 8 4))

foreach ($name in $biomes.Keys) {
  $temp, $downfall, $precip, $grass, $foliage, $sky, $extra = $biomes[$name]
  $wanted = $common + $extra
  $vegetation = @($vegetationOrder | Where-Object { $wanted -contains $_ })
  # @(...) keeps an empty list an array; a bare if-expression would turn it into null.
  $creatureSpawns = @(if ($name -eq 'desert') { } else { $creatures })
  Write-Json "worldgen/biome/alpha/$name.json" ([ordered]@{
    attributes = [ordered]@{
      'minecraft:gameplay/natural_mob_spawns' = [ordered]@{
        modifier = 'overlay'
        argument = [ordered]@{ spawn_costs = @{}; spawns_by_category = [ordered]@{ creature = $creatureSpawns; monster = $monsters } }
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

# --- Dimension: Beta's generator with Alpha's surface (no sandstone), and the same climate-based biome placement ---
$biomeMap = [ordered]@{}
foreach ($name in $biomes.Keys) { $biomeMap[$name] = "retroeras:alpha/$name" }
Write-Json 'dimension/alpha.json' ([ordered]@{
  type = 'retroeras:alpha'
  generator = [ordered]@{ type = 'retroeras:beta'; sandstone = $false; biome_source = [ordered]@{ type = 'retroeras:beta'; biomes = $biomeMap } }
})
Write-Host "Generated $($biomes.Count) Alpha biomes into $DataDir"
