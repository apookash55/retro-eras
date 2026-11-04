param(
  [string]$DataDir = (Join-Path $PSScriptRoot '..\src\main\resources\data\retroeras'),
  [string]$Jar = (Join-Path $env:APPDATA '.minecraft\versions\1.6.4\1.6.4.jar')
)
# Generates the Old Release (1.6.4) era's biomes, features and dimension from one table, so every biome shares the same
# feature order (Minecraft rejects biomes whose feature lists disagree on order).
#
# Values follow 1.6.4's BiomeGenBase and BiomeDecorator: temperatures and rainfall, trees per chunk and tree mixes,
# flowers, grass, reeds, cacti, mushrooms, lily pads, emeralds and silverfish in Extreme Hills, lakes and dungeons.
# Grass and foliage colours are read from the jar's own colormaps (ColorizerGrass lookup, swamp mixing included) and sky
# colours use BiomeGenBase.getSkyColorByTemp. Ore placements are shared with the Beta era, which used the same ones.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Write-Json($relative, $value) {
  $path = Join-Path $DataDir $relative
  New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
  $value | ConvertTo-Json -Depth 20 | Set-Content -Encoding utf8NoBOM $path
}
function Uniform([int]$min, [int]$max) { @{ type = 'minecraft:uniform'; min_inclusive = @{ absolute = $min }; max_inclusive = @{ absolute = $max } } }
function Weighted([int]$count) { @{ type = 'minecraft:count'; count = @{ type = 'minecraft:weighted_list'; distribution = @(@{ data = $count; weight = 9 }, @{ data = $count + 1; weight = 1 }) } } }

# --- Colours from the 1.6.4 jar ----------------------------------------------------------------------------------------
$zip = [IO.Compression.ZipFile]::OpenRead($Jar)
function Load($name) { $s = $zip.GetEntry($name).Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Dispose(); [Drawing.Bitmap]::FromStream($m) }
$grassMap = Load 'assets/minecraft/textures/colormap/grass.png'
$foliageMap = Load 'assets/minecraft/textures/colormap/foliage.png'
$zip.Dispose()

function ColormapRgb($img, [double]$t, [double]$r) {
  $t = [Math]::Min([Math]::Max($t, 0.0), 1.0); $r = [Math]::Min([Math]::Max($r, 0.0), 1.0) * $t
  $c = $img.GetPixel([int][Math]::Floor((1 - $t) * 255), [int][Math]::Floor((1 - $r) * 255))
  ([int]$c.R -shl 16) -bor ([int]$c.G -shl 8) -bor [int]$c.B
}
function Hex([int]$rgb) { '#{0:x6}' -f $rgb }
# BiomeGenSwamp: ((colour & 0xFEFEFE) + 0x4E0E4E) / 2
function SwampMix([int]$rgb) { (($rgb -band 0xFEFEFE) + 0x4E0E4E) -shr 1 }
function SkyColor([double]$temp) {
  $t = [Math]::Max(-1.0, [Math]::Min(1.0, $temp / 3.0))
  $h = [float](0.62222224 - $t * 0.05); $s = [float](0.5 + $t * 0.1); $v = 1.0
  # java.awt.Color.HSBtoRGB
  $hh = ($h - [Math]::Floor($h)) * 6.0; $f = $hh - [Math]::Floor($hh)
  $p = $v * (1 - $s); $q = $v * (1 - $s * $f); $u = $v * (1 - $s * (1 - $f))
  switch ([int][Math]::Floor($hh)) {
    0 { $r = $v; $g = $u; $b = $p } 1 { $r = $q; $g = $v; $b = $p } 2 { $r = $p; $g = $v; $b = $u }
    3 { $r = $p; $g = $q; $b = $v } 4 { $r = $u; $g = $p; $b = $v } default { $r = $v; $g = $p; $b = $q }
  }
  '#{0:x2}{1:x2}{2:x2}' -f [int][Math]::Floor($r * 255 + 0.5), [int][Math]::Floor($g * 255 + 0.5), [int][Math]::Floor($b * 255 + 0.5)
}

# --- Trees --------------------------------------------------------------------------------------------------------------
function Inline($feature) { @{ feature = $feature; placement = @() } }
# BiomeGenJungle.getRandomWorldGenForTrees: big oak 1/10, else bush 1/2, else huge jungle tree 1/3, else jungle tree.
Write-Json 'worldgen/feature/release/trees_jungle.json' ([ordered]@{
  type = 'minecraft:random_selector'
  default = (Inline 'minecraft:jungle_tree')
  features = @(
    @{ chance = 0.1; feature = (Inline 'minecraft:fancy_oak') },
    @{ chance = 0.5; feature = (Inline 'minecraft:jungle_bush') },
    @{ chance = 0.33333334; feature = (Inline 'minecraft:mega_jungle_tree') }
  )
})
$treePlacements = [ordered]@{
  trees_forest = @('retroeras:beta/trees_forest', 10, 'minecraft:oak_sapling')
  trees_taiga  = @('retroeras:beta/trees_taiga', 10, 'minecraft:spruce_sapling')
  trees_jungle = @('retroeras:release/trees_jungle', 50, 'minecraft:jungle_sapling')
}
foreach ($name in $treePlacements.Keys) {
  $feature, $count, $sapling = $treePlacements[$name]
  Write-Json "worldgen/placed_feature/release/$name.json" ([ordered]@{
    feature = $feature
    placement = @(
      (Weighted $count),
      @{ type = 'minecraft:in_square' },
      @{ type = 'minecraft:surface_water_depth_filter'; max_water_depth = 0 },
      @{ type = 'minecraft:heightmap'; heightmap = 'OCEAN_FLOOR' },
      @{ type = 'minecraft:biome' },
      @{ type = 'minecraft:block_predicate_filter'; predicate = @{ type = 'minecraft:would_survive'; state = $sapling } }
    )
  })
}

# --- Ores, lakes ---------------------------------------------------------------------------------------------------------
Write-Json 'worldgen/placed_feature/release/ore_emerald.json' ([ordered]@{
  feature = 'minecraft:ore_emerald'
  placement = @(@{ type = 'minecraft:count'; count = @{ type = 'minecraft:uniform'; min_inclusive = 3; max_inclusive = 8 } }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = (Uniform 4 31) }, @{ type = 'minecraft:biome' })
})
Write-Json 'worldgen/placed_feature/release/ore_infested.json' ([ordered]@{
  feature = 'minecraft:ore_infested'
  placement = @(@{ type = 'minecraft:count'; count = 7 }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = (Uniform 0 63) }, @{ type = 'minecraft:biome' })
})
Write-Json 'worldgen/feature/release/lake_water.json' ([ordered]@{
  type = 'minecraft:lake'
  barrier = @{ id = 'minecraft:stone' }
  can_place_feature = @{ type = 'minecraft:true' }
  can_replace_with_air_or_fluid = @{ type = 'minecraft:not'; predicate = @{ type = 'minecraft:matching_block_tag'; tag = 'minecraft:features_cannot_replace' } }
  can_replace_with_barrier = @{ type = 'minecraft:not'; predicate = @{ type = 'minecraft:matching_block_tag'; tag = 'minecraft:lava_pool_stone_cannot_replace' } }
  fluid = @{ id = 'minecraft:water'; properties = @{ level = '0' } }
})
# ChunkProviderGenerate.populate: a water lake in 1 of 4 chunks at any height, a lava lake in 1 of 8 biased low.
Write-Json 'worldgen/placed_feature/release/lake_water.json' ([ordered]@{
  feature = 'retroeras:release/lake_water'
  placement = @(@{ type = 'minecraft:rarity_filter'; chance = 4 }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = (Uniform 0 127) }, @{ type = 'minecraft:biome' })
})
Write-Json 'worldgen/placed_feature/release/lake_lava.json' ([ordered]@{
  feature = 'minecraft:lake_lava'
  placement = @(@{ type = 'minecraft:rarity_filter'; chance = 8 }, @{ type = 'minecraft:in_square' }, @{ type = 'minecraft:height_range'; height = @{ type = 'minecraft:biased_to_bottom'; min_inclusive = @{ absolute = 0 }; max_inclusive = @{ absolute = 127 }; inner = 8 } }, @{ type = 'minecraft:biome' })
})
$ores = @('retroeras:beta/ore_dirt', 'retroeras:beta/ore_gravel', 'retroeras:beta/ore_coal', 'retroeras:beta/ore_iron', 'retroeras:beta/ore_gold',
  'retroeras:beta/ore_redstone', 'retroeras:beta/ore_diamond', 'retroeras:beta/ore_lapis', 'retroeras:release/ore_emerald', 'retroeras:release/ore_infested',
  'minecraft:disk_sand', 'minecraft:disk_clay', 'minecraft:disk_gravel')

# --- Biomes ----------------------------------------------------------------------------------------------------------------
# One global vegetation order; each biome takes a subset, which keeps the order consistent across biomes.
$vegetationOrder = @(
  'retroeras:beta/trees_sparse', 'retroeras:release/trees_forest', 'retroeras:release/trees_taiga', 'minecraft:trees_swamp',
  'retroeras:release/trees_jungle', 'minecraft:mushroom_island_vegetation',
  'minecraft:flower_default', 'minecraft:patch_grass_normal', 'minecraft:patch_grass_plain', 'minecraft:patch_grass_forest',
  'minecraft:patch_grass_taiga_2', 'minecraft:patch_grass_jungle', 'minecraft:patch_dead_bush', 'minecraft:patch_dead_bush_2',
  'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:brown_mushroom_swamp', 'minecraft:red_mushroom_swamp',
  'minecraft:patch_sugar_cane', 'minecraft:patch_sugar_cane_desert', 'minecraft:patch_sugar_cane_swamp', 'minecraft:patch_pumpkin',
  'retroeras:beta/patch_cactus', 'minecraft:patch_melon', 'minecraft:patch_waterlily', 'minecraft:vines'
)
$common = @('minecraft:flower_default', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal', 'minecraft:patch_sugar_cane', 'minecraft:patch_pumpkin')
$desert = @('minecraft:patch_dead_bush_2', 'minecraft:patch_sugar_cane_desert', 'retroeras:beta/patch_cactus', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal')

function Spawn($type, $weight, $min, $max) { [ordered]@{ type = $type; weight = $weight; count = if ($min -eq $max) { $min } else { @{ type = 'minecraft:uniform'; min_inclusive = $min; max_inclusive = $max } } } }
$animals = @((Spawn 'minecraft:sheep' 12 4 4), (Spawn 'minecraft:pig' 10 4 4), (Spawn 'minecraft:chicken' 10 4 4), (Spawn 'minecraft:cow' 8 4 4))
$monsters = @((Spawn 'minecraft:spider' 10 4 4), (Spawn 'minecraft:zombie' 10 4 4), (Spawn 'minecraft:skeleton' 10 4 4), (Spawn 'minecraft:creeper' 10 4 4),
  (Spawn 'minecraft:slime' 10 4 4), (Spawn 'minecraft:enderman' 1 1 4))

# name: temperature, rainfall, precipitation, vegetation, creatures (animals|none|horses|wolves|ocelots|mooshrooms), swamp?, emeralds?, water lakes?
$biomes = [ordered]@{
  ocean                 = @(0.5, 0.5, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'none', $false, $false, $true)
  plains                = @(0.8, 0.4, $true, (@('minecraft:patch_grass_plain') + $common), 'horses', $false, $false, $true)
  desert                = @(2.0, 0.0, $false, $desert, 'none', $false, $false, $false)
  extreme_hills         = @(0.2, 0.3, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'animals', $false, $true, $true)
  forest                = @(0.7, 0.8, $true, (@('retroeras:release/trees_forest', 'minecraft:patch_grass_forest') + $common), 'wolves', $false, $false, $true)
  taiga                 = @(0.05, 0.8, $true, (@('retroeras:release/trees_taiga', 'minecraft:patch_grass_taiga_2') + $common), 'wolves', $false, $false, $true)
  swampland             = @(0.8, 0.9, $true, @('minecraft:trees_swamp', 'minecraft:patch_grass_normal', 'minecraft:patch_dead_bush', 'minecraft:brown_mushroom_swamp',
                            'minecraft:red_mushroom_swamp', 'minecraft:patch_sugar_cane_swamp', 'minecraft:patch_pumpkin', 'minecraft:patch_waterlily'), 'animals', $true, $false, $true)
  river                 = @(0.5, 0.5, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'none', $false, $false, $true)
  frozen_ocean          = @(0.0, 0.5, $true, @('minecraft:patch_grass_normal'), 'none', $false, $false, $true)
  frozen_river          = @(0.0, 0.5, $true, @('minecraft:patch_grass_normal'), 'none', $false, $false, $true)
  ice_plains            = @(0.0, 0.5, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'animals', $false, $false, $true)
  ice_mountains         = @(0.0, 0.5, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'animals', $false, $false, $true)
  mushroom_island       = @(0.9, 1.0, $true, @('minecraft:mushroom_island_vegetation', 'minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'), 'mooshrooms', $false, $false, $true)
  mushroom_island_shore = @(0.9, 1.0, $true, @('minecraft:brown_mushroom_normal', 'minecraft:red_mushroom_normal'), 'mooshrooms', $false, $false, $true)
  beach                 = @(0.8, 0.4, $true, @(), 'none', $false, $false, $true)
  desert_hills          = @(2.0, 0.0, $false, $desert, 'none', $false, $false, $false)
  forest_hills          = @(0.7, 0.8, $true, (@('retroeras:release/trees_forest', 'minecraft:patch_grass_forest') + $common), 'wolves', $false, $false, $true)
  taiga_hills           = @(0.05, 0.8, $true, (@('retroeras:release/trees_taiga', 'minecraft:patch_grass_taiga_2') + $common), 'wolves', $false, $false, $true)
  extreme_hills_edge    = @(0.2, 0.3, $true, (@('retroeras:beta/trees_sparse', 'minecraft:patch_grass_normal') + $common), 'animals', $false, $true, $true)
  jungle                = @(1.2, 0.9, $true, (@('retroeras:release/trees_jungle', 'minecraft:patch_grass_jungle', 'minecraft:patch_melon', 'minecraft:vines') + $common), 'ocelots', $false, $false, $true)
  jungle_hills          = @(1.2, 0.9, $true, (@('retroeras:release/trees_jungle', 'minecraft:patch_grass_jungle', 'minecraft:patch_melon', 'minecraft:vines') + $common), 'ocelots', $false, $false, $true)
}

Get-ChildItem (Join-Path $DataDir 'worldgen/biome/release') -Filter *.json -ErrorAction SilentlyContinue |
  Where-Object { -not $biomes.Contains($_.BaseName) } | Remove-Item

foreach ($name in $biomes.Keys) {
  $temp, $rain, $precip, $wanted, $creatureKind, $swamp, $emeralds, $waterLakes = $biomes[$name]
  $grass = ColormapRgb $grassMap $temp $rain
  $foliage = ColormapRgb $foliageMap $temp $rain
  if ($swamp) { $grass = SwampMix $grass; $foliage = SwampMix $foliage }

  $vegetation = @($vegetationOrder | Where-Object { $wanted -contains $_ })
  $biomeOres = @($ores | Where-Object { $emeralds -or ($_ -notlike 'retroeras:release/ore_*') })
  $lakes = @(@($(if ($waterLakes) { 'retroeras:release/lake_water' }), 'retroeras:release/lake_lava') | Where-Object { $_ })

  # @(...) keeps an empty list an array; a bare if-expression would turn it into null.
  $creatures = @(switch ($creatureKind) {
    'animals' { $animals }
    'horses' { $animals; Spawn 'minecraft:horse' 5 2 6 }
    'wolves' { $animals; Spawn 'minecraft:wolf' 5 4 4 }
    'ocelots' { $animals; Spawn 'minecraft:chicken' 10 4 4; Spawn 'minecraft:ocelot' 2 1 1 }
    'mooshrooms' { Spawn 'minecraft:mooshroom' 8 4 8 }
  })
  $biomeMonsters = @(if ($creatureKind -ne 'mooshrooms') { $monsters })

  Write-Json "worldgen/biome/release/$name.json" ([ordered]@{
    attributes = [ordered]@{
      'minecraft:gameplay/natural_mob_spawns' = [ordered]@{
        modifier = 'overlay'
        argument = [ordered]@{ spawn_costs = @{}; spawns_by_category = [ordered]@{
          creature = $creatures; monster = $biomeMonsters
          water_creature = @((Spawn 'minecraft:squid' 10 4 4)); ambient = @((Spawn 'minecraft:bat' 10 8 8))
        } }
      }
      'minecraft:visual/sky_color' = (SkyColor $temp)
    }
    carvers = @()
    downfall = $rain
    effects = [ordered]@{ water_color = $(if ($swamp) { '#617b64' } else { '#3f76e4' }); grass_color = (Hex $grass); foliage_color = (Hex $foliage) }
    features = @(
      @(), $lakes, @(), @('minecraft:monster_room'), @(), @(),
      $biomeOres, @(), @('minecraft:spring_water', 'minecraft:spring_lava'), $vegetation, @('minecraft:freeze_top_layer')
    )
    has_precipitation = $precip
    temperature = $temp
  })
}
$grassMap.Dispose(); $foliageMap.Dispose()

# --- Dimension ---------------------------------------------------------------------------------------------------------------
$biomeMap = [ordered]@{}
foreach ($name in $biomes.Keys) { $biomeMap[$name] = "retroeras:release/$name" }
Write-Json 'dimension/old_release.json' ([ordered]@{
  type = 'retroeras:old_release'
  generator = [ordered]@{ type = 'retroeras:release'; biome_source = [ordered]@{ type = 'retroeras:release'; biomes = $biomeMap } }
})
Write-Host "Generated $($biomes.Count) 1.6.4 biomes into $DataDir"
