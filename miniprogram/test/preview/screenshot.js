/**
 * 页面截图巡检：驱动本机 Chrome 无头模式，逐个打开预览页屏幕并截图
 * 输出到 test/screenshots/，供智能体读图检查布局
 * 运行：node test/preview/screenshot.js [屏幕名]
 */
const path = require('path');
const fs = require('fs');

const OUT = path.resolve(__dirname, '../screenshots');
fs.mkdirSync(OUT, { recursive: true });

const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const URL = 'http://127.0.0.1:8090/';

// 预览页各屏幕：通过页面内路由切换
const SCREENS = ['login', 'home', 'menu', 'reserve', 'monthly', 'member', 'activity', 'admin', 'staff'];

(async () => {
  const puppeteer = (await import('puppeteer-core')).default;
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-gpu', '--force-device-scale-factor=2']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2 });

  for (const name of SCREENS) {
    await page.goto(URL, { waitUntil: 'networkidle0' });
    // 默认停在登录页；其余屏幕通过页面内路由切换
    if (name !== 'login') {
      const mapping = { home: 'home', menu: 'menu', reserve: 'reserve', monthly: 'monthly', member: 'member', activity: 'activity', admin: 'admin', staff: 'staff' };
      await page.evaluate((target) => {
        // 隐藏 tabbar 的页面需要先处理：直接调用页面内的 go()
        if (typeof go === 'function') {
          go(target);
          if (target === 'admin' || target === 'monthly') {
            // admin 通过 我的 面板进入，模拟 go('admin')
          }
        }
      }, mapping[name]);
      await new Promise((r) => setTimeout(r, 600));
    }
    await page.screenshot({ path: path.join(OUT, `${name}.png`) });
    console.log(`screenshot: ${name}.png`);
  }
  await browser.close();
  console.log('全部截图完成 ->', OUT);
})().catch((e) => {
  console.error('截图失败：', e.message);
  process.exit(1);
});
