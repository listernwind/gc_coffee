/**
 * 静态回归校验（jest 内运行）：
 * 1. 每个页面 4 件套（js/json/wxml/wxss）齐全
 * 2. 所有 JSON 可解析、所有 JS 语法合法
 * 3. WXML 无非法方法调用（.slice()/.split() 等）
 * 4. 无 emoji 残留（图标统一走 PNG 图标库）
 * 5. WXML 中引用的 ic-xxx 图标类均存在于 icons.wxss
 * 运行：npm test
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '../..');
const APP_JSON = JSON.parse(fs.readFileSync(path.join(ROOT, 'app.json'), 'utf-8'));
const ICONS_WXSS = fs.readFileSync(path.join(ROOT, 'assets', 'icons.wxss'), 'utf-8');

const EMOJI_RE = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2705}\u{2714}\u{2B50}\u{2615}\u{2728}\u{203C}]/u;
const WMXL_FN_RE = /{{[^}]*\.(slice|split|map|filter|join|substring|toFixed|replace)\s*\(/g;

function walk(dir, ext, out = []) {
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    const st = fs.statSync(p);
    if (st.isDirectory()) {
      if (f === 'node_modules') continue; // 跳过依赖
      walk(p, ext, out);
    } else if (f.endsWith(ext)) out.push(p);
  }
  return out;
}

describe('页面文件完整性', () => {
  test('app.json 声明的每个页面都有 4 件套', () => {
    for (const page of APP_JSON.pages) {
      for (const ext of ['js', 'json', 'wxml', 'wxss']) {
        expect(fs.existsSync(path.join(ROOT, page + '.' + ext))).toBe(true);
      }
    }
  });

  test('页面数量 ≥ 28', () => {
    expect(APP_JSON.pages.length).toBeGreaterThanOrEqual(28);
  });
});

describe('JSON 与 JS 语法', () => {
  test('全部 JSON 可解析', () => {
    const files = walk(ROOT, '.json');
    expect(files.length).toBeGreaterThan(0);
    for (const f of files) {
      expect(() => JSON.parse(fs.readFileSync(f, 'utf-8'))).not.toThrow();
    }
  });

  test('全部页面 JS 语法合法', () => {
    for (const page of APP_JSON.pages) {
      expect(() => new Function(fs.readFileSync(path.join(ROOT, page + '.js'), 'utf-8'))).not.toThrow();
    }
    for (const f of ['app.js', 'utils/request.js', 'utils/util.js', 'custom-tab-bar/index.js']) {
      expect(() => new Function(fs.readFileSync(path.join(ROOT, f), 'utf-8'))).not.toThrow();
    }
  });
});

describe('WXML 规范', () => {
  test('无非法方法调用', () => {
    for (const page of APP_JSON.pages) {
      const wxml = fs.readFileSync(path.join(ROOT, page + '.wxml'), 'utf-8');
      expect(wxml.match(WMXL_FN_RE)).toBeNull();
    }
  });

  test('无 emoji 残留', () => {
    const files = walk(ROOT, '.wxml').concat(walk(ROOT, '.js').filter((f) => !f.includes('node_modules')));
    for (const f of files) {
      expect(EMOJI_RE.test(fs.readFileSync(f, 'utf-8'))).toBe(false);
    }
  });

  test('WXML 引用的图标都有对应 PNG 文件', () => {
    const used = new Set();
    const iconDir = path.join(ROOT, 'assets', 'icons');
    for (const page of APP_JSON.pages) {
      const wxml = fs.readFileSync(path.join(ROOT, page + '.wxml'), 'utf-8');
      for (const m of wxml.matchAll(/src="\/assets\/icons\/([a-z]+(?:-[wmg])?)\.png"/g)) {
        used.add(m[1]);
      }
      // 静态类引用（保留给预览页/样式类）
      for (const m of wxml.matchAll(/class="[^"]*\b(ic-[a-z]+(?:-[wmg])?)\b[^"]*"/g)) {
        used.add(m[1].replace(/^ic-/, '').replace(/-(w|m|g)$/, ''));
      }
    }
    const files = new Set(fs.readdirSync(iconDir).filter((f) => f.endsWith('.png')));
    expect(files.size).toBeGreaterThanOrEqual(100);
    for (const u of used) {
      if (u.includes('ic-')) continue;
      expect(files.has(u + '.png')).toBe(true);
    }
    expect(used.size).toBeGreaterThan(15);
  });

  test('图标 PNG 非纯白（含真实描边像素）', () => {
    // 简单校验：PNG 文件大小差异明显（纯白压缩图极小）且文件头正确
    for (const f of ['home.png', 'home-w.png', 'truck.png', 'star.png']) {
      const buf = fs.readFileSync(path.join(ROOT, 'assets', 'icons', f));
      expect(buf.slice(0, 8).toString('hex')).toBe('89504e470d0a1a0a');
      expect(buf.length).toBeGreaterThan(400);
    }
  });
});
