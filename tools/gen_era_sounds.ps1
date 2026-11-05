param(
  [string]$ModernSounds = (Join-Path $env:TEMP 'modern_sounds.json'),
  [string]$Assets = (Join-Path $env:APPDATA '.minecraft\assets'),
  [string]$OutDir = (Join-Path $PSScriptRoot '..\src\client\resources\assets\retroeras\era_sounds')
)
# Builds each era's sound mapping: which old sounds (from Mojang's legacy asset indexes) replace which 26.3 sound events,
# which events are silenced because the sound did not exist yet, and which music tracks the era had. Every event is
# checked against 26.3's sounds.json and every old sound against its index; anything missing is reported and dropped.
#
# Beta and Infdev used the "newsound" set, where breaking and placing a block reused its footstep sound. 1.6.4 used its
# "sound" set, which added separate dig sounds and per-mob folders.

$ErrorActionPreference = 'Stop'
$modern = (Get-Content -Raw $ModernSounds | ConvertFrom-Json).PSObject.Properties.Name
$modernSet = [Collections.Generic.HashSet[string]]::new([string[]]$modern)

# 26.3 block sound types -> the old material whose sounds they used.
$materials = [ordered]@{
  stone = 'stone'; grass = 'grass'; gravel = 'gravel'; wood = 'wood'; sand = 'sand'; wool = 'cloth'; snow = 'snow'; glass = 'stone'
  metal = 'stone'; ladder = 'wood'; netherrack = 'stone'; nether_bricks = 'stone'; nether_ore = 'stone'; bone_block = 'stone'
  basalt = 'stone'; lantern = 'stone'; anvil = 'stone'; chain = 'stone'; scaffolding = 'wood'; crop = 'grass'; hanging_roots = 'grass'
  nether_wart = 'grass'; sweet_berry_bush = 'grass'; vine = 'grass'; lily_pad = 'grass'; bamboo = 'wood'; wet_grass = 'grass'; coral_block = 'stone'
}
$blockEvents = @('break', 'step', 'place', 'hit', 'fall')
# Volume Alpha's game tracks, by the names 26.3 gives them (calm1-3, hal1-4, nuance1-2 and piano1-3 in the old jars).
$music = @('minecraft', 'clark', 'sweden', 'subwoofer_lullaby', 'living_mice', 'haggstrom', 'danny', 'key', 'oxygene', 'dry_hands',
  'wet_hands', 'mice_on_venus') | ForEach-Object { "minecraft:music/game/$_" }
$attackSounds = @('entity.player.attack.strong', 'entity.player.attack.weak', 'entity.player.attack.sweep', 'entity.player.attack.crit',
  'entity.player.attack.knockback', 'entity.player.attack.nodamage')

# Events shared by every pre-1.6 era, written as event -> old sound names (relative to the set's prefix).
$preReleaseCommon = [ordered]@{
  'entity.player.hurt' = 'random/hurt'; 'entity.player.death' = 'random/hurt'; 'entity.generic.hurt' = 'random/hurt'
  'entity.player.hurt_drown' = 'random/hurt'; 'entity.player.hurt_on_fire' = 'random/hurt'
  'entity.player.small_fall' = 'damage/fallsmall'; 'entity.player.big_fall' = 'damage/fallbig'
  'entity.generic.small_fall' = 'damage/fallsmall'; 'entity.generic.big_fall' = 'damage/fallbig'
  'entity.generic.eat' = 'random/eat'; 'entity.player.burp' = 'random/burp'; 'entity.item.pickup' = 'random/pop'; 'entity.item.break' = 'random/break'
  'entity.arrow.shoot' = 'random/bow'; 'entity.arrow.hit' = 'random/bowhit'; 'entity.generic.explode' = 'random/explode'
  'block.chest.open' = 'random/chestopen'; 'block.chest.close' = 'random/chestclosed'
  'block.wooden_door.open' = 'random/door_open'; 'block.wooden_door.close' = 'random/door_close'
  'block.wooden_trapdoor.open' = 'random/door_open'; 'block.wooden_trapdoor.close' = 'random/door_close'
  'block.lever.click' = 'random/click'; 'block.stone_button.click_on' = 'random/click'; 'block.stone_button.click_off' = 'random/click'
  'block.wooden_button.click_on' = 'random/click'; 'block.wooden_button.click_off' = 'random/click'
  'block.stone_pressure_plate.click_on' = 'random/click'; 'block.stone_pressure_plate.click_off' = 'random/click'
  'block.wooden_pressure_plate.click_on' = 'random/click'; 'block.wooden_pressure_plate.click_off' = 'random/click'
  'entity.tnt.primed' = 'random/fuse'; 'block.fire.ambient' = 'fire/fire'; 'item.flintandsteel.use' = 'fire/ignite'; 'block.fire.extinguish' = 'random/fizz'
  'block.water.ambient' = 'liquid/water'; 'block.lava.ambient' = 'liquid/lava'; 'block.lava.pop' = 'liquid/lavapop'
  'entity.generic.splash' = 'random/splash'; 'entity.player.splash' = 'random/splash'
  'block.portal.ambient' = 'portal/portal'; 'block.portal.travel' = 'portal/travel'; 'block.portal.trigger' = 'portal/trigger'
  'ambient.cave' = 'ambient/cave/cave'; 'weather.rain' = 'ambient/weather/rain'; 'weather.rain.above' = 'ambient/weather/rain'
  'entity.lightning_bolt.thunder' = 'ambient/weather/thunder'
  'block.piston.extend' = 'tile/piston/out'; 'block.piston.contract' = 'tile/piston/in'
  'block.note_block.harp' = 'note/harp'; 'block.note_block.basedrum' = 'note/bd'; 'block.note_block.snare' = 'note/snare'
  'block.note_block.hat' = 'note/hat'; 'block.note_block.bass' = 'note/bassattack'
  'entity.zombie.ambient' = 'mob/zombie'; 'entity.zombie.hurt' = 'mob/zombiehurt'; 'entity.zombie.death' = 'mob/zombiedeath'
  'entity.zombie.attack_wooden_door' = 'mob/zombie/wood'; 'entity.zombie.break_wooden_door' = 'mob/zombie/woodbreak'
  'entity.skeleton.ambient' = 'mob/skeleton'; 'entity.skeleton.hurt' = 'mob/skeletonhurt'; 'entity.skeleton.death' = 'mob/skeletondeath'
  'entity.spider.ambient' = 'mob/spider'; 'entity.spider.hurt' = 'mob/spider'; 'entity.spider.death' = 'mob/spiderdeath'
  'entity.creeper.hurt' = 'mob/creeper'; 'entity.creeper.death' = 'mob/creeperdeath'; 'entity.creeper.primed' = 'random/fuse'
  'entity.pig.ambient' = 'mob/pig'; 'entity.pig.hurt' = 'mob/pig'; 'entity.pig.death' = 'mob/pigdeath'
  'entity.cow.ambient' = 'mob/cow'; 'entity.cow.hurt' = 'mob/cowhurt'; 'entity.cow.death' = 'mob/cowhurt'
  'entity.sheep.ambient' = 'mob/sheep'; 'entity.sheep.hurt' = 'mob/sheep'; 'entity.sheep.death' = 'mob/sheep'
  'entity.chicken.ambient' = 'mob/chicken'; 'entity.chicken.hurt' = 'mob/chickenhurt'; 'entity.chicken.death' = 'mob/chickenhurt'; 'entity.chicken.egg' = 'mob/chickenplop'
  'entity.slime.squish' = 'mob/slime'; 'entity.slime.jump' = 'mob/slime'; 'entity.slime.hurt' = 'mob/slime'; 'entity.slime.death' = 'mob/slime'; 'entity.slime.attack' = 'mob/slimeattack'
  'entity.wolf.ambient' = 'mob/wolf/bark'; 'entity.wolf.growl' = 'mob/wolf/growl'; 'entity.wolf.hurt' = 'mob/wolf/hurt'; 'entity.wolf.death' = 'mob/wolf/death'
  'entity.wolf.howl' = 'mob/wolf/howl'; 'entity.wolf.pant' = 'mob/wolf/panting'; 'entity.wolf.shake' = 'mob/wolf/shake'; 'entity.wolf.whine' = 'mob/wolf/whine'
  'entity.ghast.ambient' = 'mob/ghast/moan'; 'entity.ghast.hurt' = 'mob/ghast/scream'; 'entity.ghast.death' = 'mob/ghast/death'
  'entity.ghast.shoot' = 'mob/ghast/fireball'; 'entity.ghast.warn' = 'mob/ghast/charge'
  'entity.zombified_piglin.ambient' = 'mob/zombiepig/zpig'; 'entity.zombified_piglin.angry' = 'mob/zombiepig/zpigangry'
  'entity.zombified_piglin.hurt' = 'mob/zombiepig/zpighurt'; 'entity.zombified_piglin.death' = 'mob/zombiepig/zpigdeath'
}
# Mobs in pre-1.6 eras had no footstep sounds of their own; they used the ground's, which is mostly grass.
foreach ($mob in 'zombie', 'skeleton', 'spider', 'pig', 'cow', 'sheep', 'chicken', 'wolf') { $preReleaseCommon["entity.$mob.step"] = 'step/grass' }

$releaseEvents = [ordered]@{
  'entity.player.hurt' = 'damage/hit'; 'entity.player.death' = 'damage/hit'; 'entity.generic.hurt' = 'damage/hit'
  'entity.player.hurt_drown' = 'damage/hit'; 'entity.player.hurt_on_fire' = 'damage/hit'
  'entity.player.small_fall' = 'damage/fallsmall'; 'entity.player.big_fall' = 'damage/fallbig'
  'entity.generic.small_fall' = 'damage/fallsmall'; 'entity.generic.big_fall' = 'damage/fallbig'
  'entity.generic.eat' = 'random/eat'; 'entity.player.burp' = 'random/burp'; 'entity.generic.drink' = 'random/drink'
  'entity.player.levelup' = 'random/levelup'; 'entity.experience_orb.pickup' = 'random/orb'; 'entity.item.pickup' = 'random/pop'
  'entity.item.break' = 'random/break'; 'entity.arrow.shoot' = 'random/bow'; 'entity.arrow.hit' = 'random/bowhit'
  'entity.arrow.hit_player' = 'random/successful_hit'; 'entity.generic.explode' = 'random/explode'
  'block.anvil.break' = 'random/anvil_break'; 'block.anvil.land' = 'random/anvil_land'; 'block.anvil.use' = 'random/anvil_use'
  'block.chest.open' = 'random/chestopen'; 'block.chest.close' = 'random/chestclosed'
  'block.wooden_door.open' = 'random/door_open'; 'block.wooden_door.close' = 'random/door_close'
  'block.wooden_trapdoor.open' = 'random/door_open'; 'block.wooden_trapdoor.close' = 'random/door_close'
  'block.lever.click' = 'random/click'; 'block.stone_button.click_on' = 'random/click'; 'block.stone_button.click_off' = 'random/click'
  'block.wooden_button.click_on' = 'random/wood_click'; 'block.wooden_button.click_off' = 'random/wood_click'
  'block.stone_pressure_plate.click_on' = 'random/click'; 'block.stone_pressure_plate.click_off' = 'random/click'
  'block.wooden_pressure_plate.click_on' = 'random/click'; 'block.wooden_pressure_plate.click_off' = 'random/click'
  'entity.tnt.primed' = 'random/fuse'; 'block.fire.ambient' = 'fire/fire'; 'item.flintandsteel.use' = 'fire/ignite'; 'block.fire.extinguish' = 'random/fizz'
  'block.water.ambient' = 'liquid/water'; 'block.lava.ambient' = 'liquid/lava'; 'block.lava.pop' = 'liquid/lavapop'
  'entity.generic.splash' = 'liquid/splash'; 'entity.player.splash' = 'liquid/splash'; 'entity.player.swim' = 'liquid/swim'
  'block.portal.ambient' = 'portal/portal'; 'block.portal.travel' = 'portal/travel'; 'block.portal.trigger' = 'portal/trigger'
  'ambient.cave' = 'ambient/cave/cave'; 'weather.rain' = 'ambient/weather/rain'; 'weather.rain.above' = 'ambient/weather/rain'
  'entity.lightning_bolt.thunder' = 'ambient/weather/thunder'
  'block.piston.extend' = 'tile/piston/out'; 'block.piston.contract' = 'tile/piston/in'
  'block.note_block.harp' = 'note/harp'; 'block.note_block.basedrum' = 'note/bd'; 'block.note_block.snare' = 'note/snare'
  'block.note_block.hat' = 'note/hat'; 'block.note_block.bass' = 'note/bassattack'; 'block.note_block.pling' = 'note/pling'
  'entity.minecart.riding' = 'minecart/base'; 'entity.minecart.inside' = 'minecart/inside'
  'entity.firework_rocket.launch' = 'fireworks/launch'; 'entity.firework_rocket.blast' = 'fireworks/blast'; 'entity.firework_rocket.blast_far' = 'fireworks/blast_far'
  'entity.firework_rocket.large_blast' = 'fireworks/largeBlast'; 'entity.firework_rocket.large_blast_far' = 'fireworks/largeBlast_far'
  'entity.firework_rocket.twinkle' = 'fireworks/twinkle'; 'entity.firework_rocket.twinkle_far' = 'fireworks/twinkle_far'
  'entity.bat.ambient' = 'mob/bat/idle'; 'entity.bat.hurt' = 'mob/bat/hurt'; 'entity.bat.death' = 'mob/bat/death'; 'entity.bat.takeoff' = 'mob/bat/takeoff'; 'entity.bat.loop' = 'mob/bat/loop'
  'entity.blaze.ambient' = 'mob/blaze/breathe'; 'entity.blaze.hurt' = 'mob/blaze/hit'; 'entity.blaze.death' = 'mob/blaze/death'
  'entity.cat.ambient' = 'mob/cat/meow'; 'entity.cat.hurt' = 'mob/cat/hitt'; 'entity.cat.death' = 'mob/cat/hitt'; 'entity.cat.purr' = 'mob/cat/purr'
  'entity.cat.purreow' = 'mob/cat/purreow'; 'entity.cat.hiss' = 'mob/cat/hiss'; 'entity.ocelot.ambient' = 'mob/cat/meow'
  'entity.ocelot.hurt' = 'mob/cat/hitt'; 'entity.ocelot.death' = 'mob/cat/hitt'
  'entity.chicken.ambient' = 'mob/chicken/say'; 'entity.chicken.hurt' = 'mob/chicken/hurt'; 'entity.chicken.death' = 'mob/chicken/hurt'
  'entity.chicken.step' = 'mob/chicken/step'; 'entity.chicken.egg' = 'mob/chicken/plop'
  'entity.cow.ambient' = 'mob/cow/say'; 'entity.cow.hurt' = 'mob/cow/hurt'; 'entity.cow.death' = 'mob/cow/hurt'; 'entity.cow.step' = 'mob/cow/step'
  'entity.mooshroom.shear' = 'mob/sheep/shear'
  'entity.creeper.hurt' = 'mob/creeper/say'; 'entity.creeper.death' = 'mob/creeper/death'; 'entity.creeper.primed' = 'random/fuse'
  'entity.ender_dragon.ambient' = 'mob/enderdragon/growl'; 'entity.ender_dragon.hurt' = 'mob/enderdragon/hit'
  'entity.ender_dragon.death' = 'mob/enderdragon/end'; 'entity.ender_dragon.flap' = 'mob/enderdragon/wings'
  'entity.enderman.ambient' = 'mob/endermen/idle'; 'entity.enderman.hurt' = 'mob/endermen/hit'; 'entity.enderman.death' = 'mob/endermen/death'
  'entity.enderman.teleport' = 'mob/endermen/portal'; 'entity.enderman.scream' = 'mob/endermen/scream'; 'entity.enderman.stare' = 'mob/endermen/stare'
  'entity.ghast.ambient' = 'mob/ghast/moan'; 'entity.ghast.hurt' = 'mob/ghast/scream'; 'entity.ghast.death' = 'mob/ghast/death'
  'entity.ghast.shoot' = 'mob/ghast/fireball'; 'entity.ghast.warn' = 'mob/ghast/charge'
  'entity.horse.ambient' = 'mob/horse/idle'; 'entity.horse.hurt' = 'mob/horse/hit'; 'entity.horse.death' = 'mob/horse/death'
  'entity.horse.angry' = 'mob/horse/angry'; 'entity.horse.armor' = 'mob/horse/armor'; 'entity.horse.breathe' = 'mob/horse/breathe'
  'entity.horse.gallop' = 'mob/horse/gallop'; 'entity.horse.jump' = 'mob/horse/jump'; 'entity.horse.land' = 'mob/horse/land'
  'entity.horse.saddle' = 'mob/horse/leather'; 'entity.horse.step' = 'mob/horse/soft'; 'entity.horse.step_wood' = 'mob/horse/wood'
  'entity.donkey.ambient' = 'mob/horse/donkey/idle'; 'entity.donkey.angry' = 'mob/horse/donkey/angry'
  'entity.donkey.hurt' = 'mob/horse/donkey/hit'; 'entity.donkey.death' = 'mob/horse/donkey/death'
  'entity.iron_golem.attack' = 'mob/irongolem/throw'; 'entity.iron_golem.hurt' = 'mob/irongolem/hit'; 'entity.iron_golem.death' = 'mob/irongolem/death'
  'entity.iron_golem.step' = 'mob/irongolem/walk'
  'entity.magma_cube.jump' = 'mob/magmacube/jump'; 'entity.magma_cube.squish' = 'mob/magmacube/big'; 'entity.magma_cube.hurt' = 'mob/magmacube/small'
  'entity.magma_cube.death' = 'mob/magmacube/big'
  'entity.pig.ambient' = 'mob/pig/say'; 'entity.pig.hurt' = 'mob/pig/say'; 'entity.pig.death' = 'mob/pig/death'; 'entity.pig.step' = 'mob/pig/step'
  'entity.sheep.ambient' = 'mob/sheep/say'; 'entity.sheep.hurt' = 'mob/sheep/say'; 'entity.sheep.death' = 'mob/sheep/say'
  'entity.sheep.step' = 'mob/sheep/step'; 'entity.sheep.shear' = 'mob/sheep/shear'
  'entity.silverfish.ambient' = 'mob/silverfish/say'; 'entity.silverfish.hurt' = 'mob/silverfish/hit'; 'entity.silverfish.death' = 'mob/silverfish/kill'
  'entity.silverfish.step' = 'mob/silverfish/step'
  'entity.skeleton.ambient' = 'mob/skeleton/say'; 'entity.skeleton.hurt' = 'mob/skeleton/hurt'; 'entity.skeleton.death' = 'mob/skeleton/death'
  'entity.skeleton.step' = 'mob/skeleton/step'
  'entity.slime.attack' = 'mob/slime/attack'; 'entity.slime.squish' = 'mob/slime/big'; 'entity.slime.jump' = 'mob/slime/big'
  'entity.slime.hurt' = 'mob/slime/small'; 'entity.slime.death' = 'mob/slime/big'
  'entity.spider.ambient' = 'mob/spider/say'; 'entity.spider.hurt' = 'mob/spider/say'; 'entity.spider.death' = 'mob/spider/death'
  'entity.spider.step' = 'mob/spider/step'
  'entity.villager.ambient' = 'mob/villager/idle'; 'entity.villager.hurt' = 'mob/villager/hit'; 'entity.villager.death' = 'mob/villager/death'
  'entity.villager.trade' = 'mob/villager/haggle'; 'entity.villager.yes' = 'mob/villager/yes'; 'entity.villager.no' = 'mob/villager/no'
  'entity.wither.ambient' = 'mob/wither/idle'; 'entity.wither.hurt' = 'mob/wither/hurt'; 'entity.wither.death' = 'mob/wither/death'
  'entity.wither.shoot' = 'mob/wither/shoot'; 'entity.wither.spawn' = 'mob/wither/spawn'
  'entity.wolf.ambient' = 'mob/wolf/bark'; 'entity.wolf.growl' = 'mob/wolf/growl'; 'entity.wolf.hurt' = 'mob/wolf/hurt'; 'entity.wolf.death' = 'mob/wolf/death'
  'entity.wolf.howl' = 'mob/wolf/howl'; 'entity.wolf.pant' = 'mob/wolf/panting'; 'entity.wolf.shake' = 'mob/wolf/shake'
  'entity.wolf.whine' = 'mob/wolf/whine'; 'entity.wolf.step' = 'mob/wolf/step'
  'entity.zombie.ambient' = 'mob/zombie/say'; 'entity.zombie.hurt' = 'mob/zombie/hurt'; 'entity.zombie.death' = 'mob/zombie/death'
  'entity.zombie.step' = 'mob/zombie/step'; 'entity.zombie.attack_wooden_door' = 'mob/zombie/wood'; 'entity.zombie.attack_iron_door' = 'mob/zombie/metal'
  'entity.zombie.break_wooden_door' = 'mob/zombie/woodbreak'; 'entity.zombie.infect' = 'mob/zombie/infect'
  'entity.zombie_villager.cure' = 'mob/zombie/remedy'; 'entity.zombie_villager.converted' = 'mob/zombie/unfect'
  'entity.zombified_piglin.ambient' = 'mob/zombiepig/zpig'; 'entity.zombified_piglin.angry' = 'mob/zombiepig/zpigangry'
  'entity.zombified_piglin.hurt' = 'mob/zombiepig/zpighurt'; 'entity.zombified_piglin.death' = 'mob/zombiepig/zpigdeath'
}

function Build($name, $index, $prefix, [bool]$digSounds, $events, [string[]]$tracks, [string[]]$silenced) {
  $objects = (Get-Content -Raw (Join-Path $Assets "indexes\$index.json") | ConvertFrom-Json).objects.PSObject.Properties.Name
  $all = [ordered]@{}
  foreach ($type in $materials.Keys) {
    $old = $materials[$type]
    foreach ($e in $blockEvents) {
      $sound = if ($digSounds -and ($e -eq 'break' -or $e -eq 'place')) { "dig/$old" } else { "step/$old" }
      if ($type -eq 'glass' -and $e -eq 'break') { $sound = 'random/glass' }
      $all["block.$type.$e"] = $sound
    }
  }
  foreach ($k in $events.Keys) { $all[$k] = $events[$k] }

  $mapped = [ordered]@{}
  $problems = [Collections.Generic.List[string]]::new()
  foreach ($event in $all.Keys) {
    if (-not $modernSet.Contains($event)) {
      # The material table generates every event for every type; some types simply have no such event.
      if ($event -notlike 'block.*') { $problems.Add("no 26.3 event $event") }
      continue
    }
    $pattern = '^' + [regex]::Escape("$prefix$($all[$event])") + '\d*\.ogg$'
    if (-not ($objects | Where-Object { $_ -match $pattern } | Select-Object -First 1)) { $problems.Add("no old sound $($all[$event]) for $event"); continue }
    $mapped[$event] = @($all[$event])
  }
  foreach ($event in $silenced) { if ($modernSet.Contains($event)) { $mapped[$event] = @() } else { $problems.Add("no 26.3 event $event") } }

  $out = [ordered]@{
    _comment = "Generated by tools/gen_era_sounds.ps1; edit that script instead. Maps 26.3 sound events to $name's sounds from the '$index' asset index. An empty list silences an event that did not exist yet."
    index = $index
    prefix = $prefix
    events = $mapped
    music = $tracks
  }
  New-Item -ItemType Directory -Force $OutDir | Out-Null
  $out | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8NoBOM (Join-Path $OutDir "$name.json")
  Write-Host ("{0}: {1} events mapped, {2} dropped" -f $name, $mapped.Count, $problems.Count)
  $problems | ForEach-Object { Write-Host "  $_" }
}

# Infdev only had the first three Minecraft tracks; Alpha 1.2.6, Beta 1.7.3 and 1.6.4 had all of Volume Alpha's game music.
Build 'beta' 'pre-1.6' 'newsound/' $false $preReleaseCommon $music $attackSounds
Build 'alpha' 'pre-1.6' 'newsound/' $false $preReleaseCommon $music $attackSounds
Build 'pre_alpha' 'pre-1.6' 'newsound/' $false $preReleaseCommon @($music | Select-Object -First 3) $attackSounds
# Classic 0.30 had the same footsteps as Infdev and no music at all.
Build 'classic' 'pre-1.6' 'newsound/' $false $preReleaseCommon @() $attackSounds
Build 'old_release' 'legacy' 'sound/' $true $releaseEvents $music $attackSounds
