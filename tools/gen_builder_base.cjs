// Adds the Builder Base to the widget data, from the same clash-of-clans-data package:
//   - names       → merged into app/src/main/assets/names.json
//   - upgrade times → merged into app/src/main/assets/upgrade_times.json
//   - icons       → app/src/main/assets/icons/<dataId>.webp (64x64, like gen_icons.cjs)
// Builder Base IDs don't overlap the home village ones, so the files are simply merged.
//
// Usage (needs the `sharp` npm package on NODE_PATH):
//   node tools/gen_builder_base.cjs <path-to-unpacked>/package
const fs = require('fs'), path = require('path');
const sharp = require('sharp');

const ROOT = path.resolve(__dirname, '..');
const pkgDir = path.resolve(process.argv[2] || '');
const bb = path.join(pkgDir, 'data', 'builder');
if (!fs.existsSync(bb)) { console.error('usage: node tools/gen_builder_base.cjs <package>'); process.exit(1); }
const ASSETS = path.join(ROOT, 'app', 'src', 'main', 'assets');
const SIZE = 64;
const TIME_KEYS = ['buildTime', 'upgradeTime', 'researchTime'];
const secs = t => ((t.days * 24 + t.hours) * 60 + t.minutes) * 60 + (t.seconds || 0);

const items = [];
(function walk(d) {
  for (const f of fs.readdirSync(d).sort()) {
    const p = path.join(d, f);
    if (fs.statSync(p).isDirectory()) { if (f !== 'leagues') walk(p); }
    else if (p.endsWith('.json')) for (const o of [].concat(JSON.parse(fs.readFileSync(p, 'utf8'))))
      if (o && typeof o.dataId === 'number') items.push(o);
  }
})(bb);

const namesFile = path.join(ASSETS, 'names.json');
const timesFile = path.join(ASSETS, 'upgrade_times.json');
const names = JSON.parse(fs.readFileSync(namesFile, 'utf8'));
const times = JSON.parse(fs.readFileSync(timesFile, 'utf8'));

// Heroes and troops: their portrait. Buildings and traps: the highest level's picture.
function imageFor(o) {
  if (o.images && o.images.icon) return o.images.icon;
  const lv = (o.levels || []).filter(l => l.images && l.images.normal);
  return lv.length ? lv[lv.length - 1].images.normal : null;
}

(async () => {
  const missing = [];
  for (const o of items) {
    const prev = names.names[o.dataId];
    if (prev && prev !== o.name) throw new Error(`name conflict ${o.dataId}: ${prev} / ${o.name}`);
    names.names[o.dataId] = o.name;
    const byLevel = {};
    for (const l of o.levels || []) {
      const t = TIME_KEYS.map(k => l[k]).find(Boolean);
      if (t && typeof l.level === 'number') byLevel[l.level] = secs(t);
    }
    if (Object.keys(byLevel).length) times.times[o.dataId] = byLevel;

    const img = imageFor(o);
    if (!img) { missing.push(o.name); continue; }
    const src = sharp(path.join(pkgDir, img)).trim();
    const buf = await src.toBuffer();
    await sharp(buf)
      .resize(SIZE, SIZE, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
      .webp({ quality: 90, alphaQuality: 100 })
      .toFile(path.join(ASSETS, 'icons', `${o.dataId}.webp`));
  }
  names.source += ' + data/builder';
  times.source += ' + data/builder';
  fs.writeFileSync(namesFile, JSON.stringify(names, null, 1) + '\n');
  fs.writeFileSync(timesFile, JSON.stringify(times) + '\n');
  console.log(`Builder Base: ${items.length} items, ${items.length - missing.length} icons`);
  if (missing.length) console.log('no image:', missing.join(', '));
})();
