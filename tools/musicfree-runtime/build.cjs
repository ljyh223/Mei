const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');
const output = path.resolve(__dirname, '../../app/src/main/assets/musicfree');
fs.mkdirSync(output, { recursive: true });
esbuild.buildSync({
  entryPoints: [path.join(__dirname, 'modules.cjs')],
  outfile: path.join(output, 'modules.js'),
  bundle: true, platform: 'browser', format: 'iife', target: 'es2020',
  legalComments: 'inline'
});
const notices = ['MusicFree compatibility modules bundled for Mei. Rebuild with npm ci && npm run build in tools/musicfree-runtime.\n'];
const visited = new Set();
function collect(directory) {
  for (const item of fs.readdirSync(directory, { withFileTypes: true })) {
    if (item.name.startsWith('.')) continue;
    const location = path.join(directory, item.name);
    if (item.isDirectory()) { collect(location); continue; }
    if (item.name !== 'package.json') continue;
    const info = JSON.parse(fs.readFileSync(location, 'utf8'));
    if (!info.name || !info.version || info.name.includes('esbuild')) continue;
    const key = info.name + '@' + info.version;
    if (visited.has(key)) continue;
    visited.add(key);
    const base = path.dirname(location);
    const license = fs.readdirSync(base).find(name => /^licen[sc]e(?:\.|$)/i.test(name));
    notices.push(key + ' — ' + (info.license || '') + '\n' + (license ? fs.readFileSync(path.join(base, license), 'utf8') : 'See package metadata for license.') + '\n');
  }
}
collect(path.join(__dirname, 'node_modules'));
fs.writeFileSync(path.join(output, 'THIRD_PARTY_NOTICES.txt'), notices.join('\n'));
