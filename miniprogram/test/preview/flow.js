/**
 * 预览页功能流测试（puppeteer-core 驱动本机 Chrome + 内置断言）
 * 覆盖：登录 → 首页 → 点单加购 → 预订试算 → 月度 → 会员 → 派送工作台 → 店长看板
 * 运行：npm run test:flow（需先启动预览服务 python3 preview/server.py）
 */
const path = require('path');
const fs = require('fs');

const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const URL = process.env.PREVIEW_URL || 'http://127.0.0.1:8090/';
const SHOT_DIR = path.resolve(__dirname, '../screenshots/flow');
fs.mkdirSync(SHOT_DIR, { recursive: true });

let passed = 0;
let failed = 0;
function check(name, cond, extra = '') {
  if (cond) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}  ${extra}`); }
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  const puppeteer = (await import('puppeteer-core')).default;
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-gpu', '--force-device-scale-factor=2']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2 });
  await page.goto(URL, { waitUntil: 'networkidle0' });

  const goScreen = async (name) => {
    await page.evaluate((n) => go(n), name);
    await sleep(400);
  };

  // 1. 登录页
  check('登录页品牌', (await page.$eval('.b-name', (el) => el.textContent)).includes('GC Coffee'));
  await page.$$eval('.mtab', (els) => els[1].click());
  check('账号登录模式', (await page.$('#lm-pwd')) !== null);
  await page.$$eval('.mtab', (els) => els[2].click());
  check('注册模式', (await page.$('#lm-reg')) !== null);
  await page.$$eval('.mtab', (els) => els[0].click());
  await page.click('.wx-btn');
  await sleep(500);
  check('微信登录进入首页', (await page.evaluate(() => document.querySelector('.screen.active').id)) === 'scr-home');
  await page.screenshot({ path: path.join(SHOT_DIR, '01-login-to-home.png') });

  // 2. 首页
  check('两大主入口', (await page.$$eval('.hero-card', (els) => els.length)) === 2);
  const heroes = await page.$$eval('.hero-title', (els) => els.map((e) => e.textContent).join(','));
  check('入口文案', heroes.includes('咖啡液预订') && heroes.includes('饮品下单'));
  check('tabbar 可见', (await page.$eval('.tabbar', (el) => getComputedStyle(el).display)) === 'flex');
  await page.screenshot({ path: path.join(SHOT_DIR, '02-home.png') });

  // 3. 点单加购
  await goScreen('menu');
  await page.click('.sbtn.plus');
  await page.click('.sbtn.plus');
  check('加购计数=2', (await page.$eval('#cartCnt', (el) => el.textContent)).trim() === '2');
  const amt = parseFloat((await page.$eval('#cartAmt', (el) => el.textContent)).replace('¥', ''));
  check('购物车金额>0', amt > 0, `amt=${amt}`);
  await page.click('.sbtn.minus');
  check('减购计数=1', (await page.$eval('#cartCnt', (el) => el.textContent)).trim() === '1');
  await page.screenshot({ path: path.join(SHOT_DIR, '03-menu-cart.png') });

  // 4. 预订试算
  await goScreen('reserve');
  await page.click('#scr-reserve .sbtn.plus');
  await page.click('#scr-reserve .sbtn.plus');
  check('数量=3', (await page.$eval('#qty', (el) => el.textContent)).trim() === '3');
  check('额度抵扣应付 0', (await page.$eval('#resAmt', (el) => el.textContent)) === '¥0.00');
  check('试算描述', (await page.$eval('#resDesc', (el) => el.textContent)).includes('额度抵 3 瓶'));
  await page.click('#pkgSwitch');
  check('关闭额度全额支付 45', (await page.$eval('#resAmt', (el) => el.textContent)) === '¥45.00');
  await page.screenshot({ path: path.join(SHOT_DIR, '04-reserve-calc.png') });

  // 5. 月度详情
  await goScreen('monthly');
  const nums = await page.$$eval('.ov-num', (els) => els.map((e) => e.textContent));
  check('月度花费/剩余', nums[0].includes('360') && nums[1] === '25', nums.join('|'));
  check('额度进度条', (await page.$eval('.ov-bar-in', (el) => el.style.width)) === '17%');
  await page.screenshot({ path: path.join(SHOT_DIR, '05-monthly.png') });

  // 6. 会员
  await goScreen('member');
  const mc = await page.$$eval('.mc-num', (els) => els.map((e) => e.textContent));
  check('余额/积分', mc[0].includes('200') && mc[1] === '500', mc.join('|'));
  await page.screenshot({ path: path.join(SHOT_DIR, '06-member.png') });

  // 7. 派送员工作台
  await goScreen('staff-login');
  await page.click('#scr-staff-login .btn-primary');
  await sleep(400);
  check('派送任务卡', (await page.$$eval('.task', (els) => els.length)) >= 2);
  const claimed = await page.$$eval('.task', (els) => {
    const btn = els[0].querySelector('.t-actions .btn');
    if (btn) { btn.click(); return true; }
    return false;
  });
  check('认领操作', claimed);
  await page.screenshot({ path: path.join(SHOT_DIR, '07-staff-claim.png') });

  // 8. 店长看板
  await goScreen('admin');
  check('营业额大卡', (await page.$eval('.bc-num', (el) => el.textContent)).includes('¥'));
  check('指标卡 6 张', (await page.$$eval('.stat', (els) => els.length)) === 6);
  await page.screenshot({ path: path.join(SHOT_DIR, '08-admin-dash.png') });

  await browser.close();
  console.log(`\n===== 功能流测试 ${passed}/${passed + failed} passed =====`);
  console.log('截图证据:', SHOT_DIR);
  process.exitCode = failed > 0 ? 1 : 0;
})().catch((e) => {
  console.error('运行失败：', e.message);
  console.error('请确认预览服务已启动：python3 preview/server.py');
  process.exit(2);
});
