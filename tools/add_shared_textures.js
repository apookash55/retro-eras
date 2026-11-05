// Adds the textures every old era shares to its era_textures mapping: particles, buttons, worn armour and paintings.
// Their layouts barely changed from Classic to 1.6.4, only where each jar keeps them. Entries it adds are tagged
// "shared": true and replaced on every run. Run with node from the project root:
//   node tools/add_shared_textures.js
// tools/gen_release_texture_map.ps1 runs it after writing old_release.json, and tools/gen_alpha_texture_map.ps1 copies
// Beta's, so run it before those.
const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, '..', 'src', 'client', 'resources', 'assets', 'retroeras', 'era_textures');

// Where each era's jar keeps the shared sheets. 1.6 moved everything under assets/minecraft/textures/. The hand-written
// mappings ("compact") keep one entry per line; the generated ones are plain JSON.
const sources = {
  classic: { compact: true, particles: 'particles.png', gui: 'gui/gui.png' },
  pre_alpha: { compact: true, particles: 'particles.png', gui: 'gui/gui.png', armour: 'old', paintings: 'art/kz.png' },
  beta: { compact: true, particles: 'particles.png', gui: 'gui/gui.png', armour: 'old', paintings: 'art/kz.png' },
  old_release: {
    particles: 'assets/minecraft/textures/particle/particles.png', gui: 'assets/minecraft/textures/gui/widgets.png', armour: 'release',
    paintings: 'assets/minecraft/textures/painting/paintings_kristoffer_zetterstrand.png', explosion: 'assets/minecraft/textures/entity/explosion.png',
  },
};

// particles.png: a 16x16 grid of 8x8 sprites, with the indices the old EntityFX classes used (1.6.4's listed; older
// jars leave the later ones blank, which "skip_empty" leaves as 26.3's).
function particleTiles() {
  const tiles = {};
  const set = (index, ...targets) => { tiles[index] = (tiles[index] || []).concat(targets.map(t => `particle/${t}`)); };
  for (let i = 0; i < 8; i++) set(i, `generic_${i}`);
  for (let i = 0; i < 4; i++) set(19 + i, `splash_${i}`);
  set(32, 'bubble');
  set(48, 'flame');
  set(49, 'lava');
  set(64, 'note');
  set(65, 'critical_hit', 'enchanted_hit');
  set(80, 'heart');
  set(81, 'angry');
  set(82, 'glint');
  set(112, 'drip_fall');
  set(113, 'drip_hang');
  set(114, 'drip_land');
  for (let i = 0; i < 8; i++) set(128 + i, `effect_${i}`, `spell_${i}`);
  for (let i = 0; i < 26; i++) set(225 + i, `sga_${String.fromCharCode(97 + i)}`);
  return tiles;
}

// The old painting sheet, as EnumArt laid it out: [26.3 name, width, height, u, v] in pixels.
const paintings = [
  ['kebab', 16, 16, 0, 0], ['aztec', 16, 16, 16, 0], ['alban', 16, 16, 32, 0], ['aztec2', 16, 16, 48, 0], ['bomb', 16, 16, 64, 0],
  ['plant', 16, 16, 80, 0], ['wasteland', 16, 16, 96, 0], ['pool', 32, 16, 0, 32], ['courbet', 32, 16, 32, 32], ['sea', 32, 16, 64, 32],
  ['sunset', 32, 16, 96, 32], ['creebet', 32, 16, 128, 32], ['wanderer', 16, 32, 0, 64], ['graham', 16, 32, 16, 64],
  ['match', 32, 32, 0, 128], ['bust', 32, 32, 32, 128], ['stage', 32, 32, 64, 128], ['void', 32, 32, 96, 128],
  ['skull_and_roses', 32, 32, 128, 128], ['wither', 32, 32, 160, 128], ['fighters', 64, 32, 0, 96], ['pointer', 64, 64, 0, 192],
  ['pigscene', 64, 64, 64, 192], ['burning_skull', 64, 64, 128, 192], ['skeleton', 64, 48, 192, 64], ['donkey_kong', 64, 48, 192, 112],
  ['back', 16, 16, 192, 0],
];

function sharedFor(source) {
  const atlases = [{ file: source.particles, skip_empty: true, shared: true, tiles: particleTiles() }];
  // gui.png (widgets.png from 1.6): the disabled, normal and highlighted buttons, 200x20 each, under the hotbar.
  const regions = [
    { from: source.gui, x: 0, y: 46, w: 200, h: 20, to: ['gui/sprites/widget/button_disabled'], shared: true },
    { from: source.gui, x: 0, y: 66, w: 200, h: 20, to: ['gui/sprites/widget/button'], shared: true },
    { from: source.gui, x: 0, y: 86, w: 200, h: 20, to: ['gui/sprites/widget/button_highlighted'], shared: true },
  ];
  const files = [];
  if (source.paintings) {
    for (const [name, w, h, u, v] of paintings) {
      regions.push({ from: source.paintings, x: u, y: v, w, h, to: [`painting/${name}`], shared: true });
    }
  }
  if (source.explosion) {
    // 1.6.4's large explosion: a 4x4 sheet of 32x32 frames.
    for (let i = 0; i < 16; i++) {
      regions.push({ from: source.explosion, x: (i % 4) * 32, y: Math.floor(i / 4) * 32, w: 32, h: 32, to: [`particle/explosion_${i}`], shared: true });
    }
  }
  // Worn armour: same 64x32 layout as now, layer 1 for helmet, chestplate and boots and layer 2 for leggings. Leather is
  // brown in the old sheets, so 26.3's leather tint is divided out of it.
  const materials = source.armour === 'old'
    ? [['cloth', 'leather', '|untint:A06540'], ['chain', 'chainmail', ''], ['iron', 'iron', ''], ['gold', 'gold', ''], ['diamond', 'diamond', '']]
    : source.armour === 'release'
      ? [['leather', 'leather', ''], ['chainmail', 'chainmail', ''], ['iron', 'iron', ''], ['gold', 'gold', ''], ['diamond', 'diamond', '']]
      : [];
  for (const [old, modern, suffix] of materials) {
    const from = source.armour === 'old' ? n => `armor/${old}_${n}.png` : n => `assets/minecraft/textures/models/armor/${old}_layer_${n}.png`;
    files.push({ from: from(1), to: [`entity/equipment/humanoid/${modern}${suffix}`], shared: true });
    files.push({ from: from(2), to: [`entity/equipment/humanoid_leggings/${modern}${suffix}`], shared: true });
  }
  if (source.armour === 'release') {
    files.push({ from: 'assets/minecraft/textures/models/armor/leather_layer_1_overlay.png', to: ['entity/equipment/humanoid/leather_overlay'], shared: true });
    files.push({ from: 'assets/minecraft/textures/models/armor/leather_layer_2_overlay.png', to: ['entity/equipment/humanoid_leggings/leather_overlay'], shared: true });
  }
  return { atlases, regions, files };
}

// The 1.6.4 spawn eggs: one grey egg, tinted with the mob's colour, and its spots tinted with a second (EntityList).
// 26.3 draws a picture per mob instead. 1.6.4 had one egg for every kind of horse.
const eggs = [
  ['creeper', '0DA70B', '000000'], ['skeleton', 'C1C1C1', '494949'], ['spider', '342D27', 'A80E0E'], ['zombie', '00AFAF', '799C65'],
  ['slime', '51A03E', '7EBF6E'], ['ghast', 'F9F9F9', 'BCBCBC'], ['zombified_piglin', 'EA9393', '4C7129'], ['enderman', '161616', '000000'],
  ['cave_spider', '0C424E', 'A80E0E'], ['silverfish', '6E6E6E', '303030'], ['blaze', 'F6B201', 'FFF87E'], ['magma_cube', '340000', 'FCFC00'],
  ['bat', '4C3E30', '0F0F0F'], ['witch', '340000', '51A03E'], ['pig', 'F0A5A2', 'DB635F'], ['sheep', 'E7E7E7', 'FFB5B5'],
  ['cow', '443626', 'A1A1A1'], ['chicken', 'A1A1A1', 'FF0000'], ['squid', '223B4D', '708899'], ['wolf', 'D7D3D3', 'CEAF96'],
  ['mooshroom', 'A00F10', 'B7B7B7'], ['ocelot', 'EFDE7D', '564434'], ['horse', 'C09E7D', 'EEE500'], ['donkey', 'C09E7D', 'EEE500'],
  ['mule', 'C09E7D', 'EEE500'], ['villager', '563C33', 'BD8B72'],
];

// What each era still lacked after its own mapping: Infdev's chest, leather armour and minecart icons; Beta's map, which
// was the one map item; 1.6.4's spawn eggs and enchanted golden apple.
function extrasFor(era, beta) {
  const atlases = [];
  const compose = [];
  const files = [];
  const clear = [];
  if (era === 'pre_alpha') {
    atlases.push({ file: 'gui/items.png', shared: true, tiles: {
      0: ['item/leather_helmet|untint:A06540'], 16: ['item/leather_chestplate|untint:A06540'], 32: ['item/leather_leggings|untint:A06540'],
      48: ['item/leather_boots|untint:A06540'], 135: ['item/minecart'],
    } });
    // Infdev's chest used the same terrain tiles as Beta's, so Beta's chest models are painted the same way.
    for (const c of beta.compose.filter(c => !c.shared && c.to.some(t => t.startsWith('entity/chest/')))) {
      compose.push({ ...c, shared: true });
    }
  }
  if (era === 'beta') {
    atlases.push({ file: 'gui/items.png', shared: true, tiles: { 60: ['item/filled_map'] } });
    clear.push('item/filled_map_markings');
  }
  if (era === 'old_release') {
    files.push({ from: 'assets/minecraft/textures/items/apple_golden.png', to: ['item/enchanted_golden_apple'], shared: true });
    for (const [mob, base, spots] of eggs) {
      compose.push({ to: [`item/${mob}_spawn_egg`], size: [16, 16], shared: true, parts: [
        { file: 'assets/minecraft/textures/items/spawn_egg.png', src: [0, 0, 16, 16], dst: [0, 0], tint: base },
        { file: 'assets/minecraft/textures/items/spawn_egg_overlay.png', src: [0, 0, 16, 16], dst: [0, 0], tint: spots, over: true },
      ] });
    }
  }
  return { atlases, compose, files, clear };
}

function compact(value, indent) {
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value);
  }
  const inner = indent + '  ';
  if (Array.isArray(value)) {
    if (value.every(v => v === null || typeof v !== 'object')) {
      return '[' + value.map(v => JSON.stringify(v)).join(', ') + ']';
    }
    return '[\n' + value.map(v => inner + compact(v, inner)).join(',\n') + '\n' + indent + ']';
  }
  const entries = Object.entries(value);
  const flat = '{ ' + entries.map(([k, v]) => JSON.stringify(k) + ': ' + compact(v, inner)).join(', ') + ' }';
  if (!flat.includes('\n') && flat.length + indent.length <= 150 && entries.length <= 8) {
    return flat;
  }
  return '{\n' + entries.map(([k, v]) => inner + JSON.stringify(k) + ': ' + compact(v, inner)).join(',\n') + '\n' + indent + '}';
}

const beta = JSON.parse(fs.readFileSync(path.join(dir, 'beta.json'), 'utf8'));
for (const [era, source] of Object.entries(sources)) {
  const file = path.join(dir, `${era}.json`);
  const map = JSON.parse(fs.readFileSync(file, 'utf8'));
  const shared = sharedFor(source);
  const extras = extrasFor(era, beta);
  shared.atlases.push(...extras.atlases);
  shared.files.push(...extras.files);
  map.compose = (map.compose || []).filter(c => !c.shared).concat(extras.compose);
  if (map.compose.length === 0) {
    delete map.compose;
  }
  if (extras.clear.length) {
    map.clear = [...new Set([...(map.clear || []), ...extras.clear])];
  }
  // Replace what an earlier run added, and the older hand-written armour lines these supersede.
  const replaced = new Set(shared.files.flatMap(f => f.to.map(t => t.split('|')[0])));
  map.atlases = (map.atlases || []).filter(a => !a.shared).concat(shared.atlases);
  map.regions = (map.regions || []).filter(r => !r.shared).concat(shared.regions);
  map.files = (map.files || []).filter(f => !f.shared && !f.to.every(t => replaced.has(t.split('|')[0]))).concat(shared.files);
  if (map.clear) {
    map.clear = map.clear.filter(c => !replaced.has(c));
  }
  fs.writeFileSync(file, (source.compact ? compact(map, '') : JSON.stringify(map, null, 2)) + '\n');
  console.log(`${era}: ${shared.files.length} files, ${shared.regions.length} regions, particles`);
}
