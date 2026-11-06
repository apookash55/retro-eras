param([string]$DataDir = (Join-Path $PSScriptRoot '..\src\main\resources\data\retroeras'))
# Generates the Alpha era's biomes and dimension from one table. Alpha 1.2.6 had the same
# climate, biome table and terrain as Beta 1.7.3 (BetaChunkGenerator with sandstone off), but a plainer decorator:
# only oak and big oak trees (none at all on plains, desert or tundra), dandelions, roses, mushrooms, reeds, pumpkins
# and cacti, no tall grass, ferns, dead bushes, lapis, wolves or squid (ChunkProviderGenerate.populate in a1.2.6).
#
# Colours are Beta's, which Alpha shared: grass/foliage are its colormaps sampled at each biome's Beta climate
# (ColorizerGrass: x = (1 - temp) * 255, y = (1 - humidity * temp) * 255), and sky is BiomeGenBase.getSkyColorByTemp.
# Decoration is not here: it is a port of Alpha's populate (BetaDecorator), so the biomes have no feature lists.

$ErrorActionPreference = 'Stop'
function Write-Json($relative, $value) {
  $path = Join-Path $DataDir $relative
  New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
  $value | ConvertTo-Json -Depth 20 | Set-Content -Encoding utf8NoBOM $path
}
function Uniform($min, $max) { @{ type = 'minecraft:uniform'; min_inclusive = @{ absolute = $min }; max_inclusive = @{ absolute = $max } } }

# --- Biomes ----------------------------------------------------------------------------------------------------------
# Rain and snowfall came in Beta 1.5, so no Alpha biome has precipitation; cold ones still get snow cover when generated.
# name: modern temperature (controls snow and rain), downfall, precipitation, grass, foliage, sky, extra vegetation
$biomes = [ordered]@{
  tundra          = @(-0.5, 0.5, $false, '#83b593', '#64a278', '#7fa2ff', @())
  taiga           = @(-0.5, 0.4, $false, '#8ab87c', '#6da55b', '#7da4ff', @('retroeras:alpha/trees_forest'))
  swampland       = @(0.6,  0.9, $false, '#83bd66', '#65ab3f', '#7aa6ff', @('retroeras:alpha/trees_sparse'))
  forest          = @(0.7,  0.8, $false, '#82bf54', '#63ae2a', '#79a7ff', @('retroeras:alpha/trees_forest'))
  shrubland       = @(0.8,  0.3, $false, '#99bb5b', '#81a932', '#79a7ff', @('retroeras:alpha/trees_sparse'))
  savanna         = @(0.9,  0.1, $false, '#a6b866', '#8fa541', '#79a7ff', @('retroeras:alpha/trees_sparse'))
  plains          = @(0.9,  0.4, $false, '#94bf49', '#7aae1d', '#77a9ff', @())
  seasonal_forest = @(0.9,  0.7, $false, '#6bc63e', '#46b70d', '#77a9ff', @('retroeras:alpha/trees_seasonal'))
  rainforest      = @(0.95, 0.95, $false,'#4ccc35', '#21be02', '#77a9ff', @('retroeras:alpha/trees_rainforest'))
  desert          = @(2.0,  0.0, $false, '#b2b952', '#9fa726', '#77a9ff', @('retroeras:beta/patch_cactus'))
}

function Spawn($type, $weight, $count) { [ordered]@{ type = $type; weight = $weight; count = $count } }
# Alpha's mobs: no wolves (Beta 1.4) and no squid (Beta 1.2).
$monsters = @((Spawn 'minecraft:spider' 10 4), (Spawn 'minecraft:zombie' 10 4), (Spawn 'minecraft:skeleton' 10 4), (Spawn 'minecraft:creeper' 10 4), (Spawn 'minecraft:slime' 10 4))
$creatures = @((Spawn 'minecraft:sheep' 12 4), (Spawn 'minecraft:pig' 10 4), (Spawn 'minecraft:chicken' 10 4), (Spawn 'minecraft:cow' 8 4))

foreach ($name in $biomes.Keys) {
  $temp, $downfall, $precip, $grass, $foliage, $sky, $extra = $biomes[$name]
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
    features = @()
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
