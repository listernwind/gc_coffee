/**
 * 图标渲染器：将每个 SVG 作为顶层文档导航（data: URL）+ 全页截图
 * （本机 Chrome 无头对页面内 SVG/img 渲染不可靠，顶层文档导航渲染正确）
 * 用法：node test/tools/render-icons.js <json清单> <输出目录>
 */
import fs from 'fs';
import path from 'path';

const [listPath, outDir] = process.argv.slice(2);
if (!listPath || !outDir) {
  console.error('用法: node render-icons.js <json> <outdir>');
  process.exit(1);
}
fs.mkdirSync(outDir, { recursive: true });

const items = JSON.parse(fs.readFileSync(listPath, 'utf-8'));
const puppeteer = (await import('puppeteer-core')).default;
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars']
});
try {
  const page = await browser.newPage();
  await page.setViewport({ width: 96, height: 96, deviceScaleFactor: 1 });
  await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }]);

  let done = 0;
  for (const it of items) {
    const url = 'data:image/svg+xml;base64,' + Buffer.from(it.svg).toString('base64');
    await page.goto(url, { waitUntil: 'load' });
    await page.screenshot({ path: path.join(outDir, it.name + '.png') });
    done++;
  }
  console.log(`rendered ${done}/${items.length} icons via Chrome`);
} finally {
  await browser.close();
}
