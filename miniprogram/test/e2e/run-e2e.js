/**
 * 页面 E2E 自动化（miniprogram-automator，微信官方）
 *
 * 前置条件：
 * 1. 微信开发者工具 → 设置 → 安全设置 → 打开「服务端口」
 * 2. 后端已启动（http://127.0.0.1:8080）
 * 3. 开发者工具中已导入本项目并勾选「不校验合法域名」
 *
 * 运行：npm run test:e2e
 * 自定义 CLI 路径：MINI_CLI=/path/to/cli npm run test:e2e
 */
const path = require('path');
const automator = require('miniprogram-automator');

const PROJECT = path.resolve(__dirname, '../..');
const CLI = process.env.MINI_CLI || '/Applications/wechatwebdevtools.app/Contents/MacOS/cli';

let passed = 0;
let failed = 0;
function check(name, cond) {
  if (cond) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}`); }
}

(async () => {
  console.log('正在通过开发者工具 CLI 启动小程序（需要已开启服务端口）...');
  const mini = await automator.launch({ cliPath: CLI, projectPath: PROJECT });
  try {
    // 1. 登录页：三种模式 + 微信一键登录
    const login = await mini.reLaunch('/pages/login/login');
    await login.waitFor(800);
    check('登录页标题', (await login.$('.brand-name')).text().includes('GC Coffee'));
    check('微信一键登录按钮存在', await login.$('.wx-btn'));

    const wxBtn = await login.$('.wx-btn');
    await wxBtn.tap();
    await login.waitFor(1500);

    // 2. 首页：两大入口
    const home = await mini.currentPage();
    check('登录后进入首页', (home.path || '').includes('pages/home/home'));
    const heroes = await home.$$('.hero-card');
    check('首页两大主入口', heroes.length === 2);
    const titles = (await home.$('.hero-title')).text();
    check('入口文案', titles.includes('咖啡液预订'));

    // 3. 点单：加购
    const menu = await mini.switchTab('/pages/menu/menu');
    await menu.waitFor(1000);
    const plus = await menu.$('.step-btn.plus');
    check('菜单加购按钮', !!plus);
    await plus.tap();
    const cartCount = await menu.$('.cart-count');
    check('购物车计数出现', !!cartCount && (await cartCount.text()).trim() === '1');

    // 4. 店长登录 → 看板
    const adminLogin = await mini.reLaunch('/pages/admin-login/admin-login');
    await adminLogin.waitFor(800);
    await mini.callWxMethod('setStorageSync', 'adminToken', ''); // 清旧 token 走登录页
    const inputs = await adminLogin.$$('input');
    await inputs[1].input('admin123');
    await adminLogin.$('.btn-primary').tap();
    await adminLogin.waitFor(1500);
    const dash = await mini.currentPage();
    check('店长进入看板', (dash.path || '').includes('admin/dashboard'));
    check('看板营业额卡片', !!(await dash.$('.bc-num')));

    console.log(`\n===== E2E ${passed}/${passed + failed} passed =====`);
    process.exitCode = failed > 0 ? 1 : 0;
  } finally {
    await mini.close();
  }
})().catch((e) => {
  console.error('E2E 运行失败：', e.message);
  console.error('请确认：开发者工具已开启「服务端口」、已导入本项目、后端已启动');
  process.exit(2);
});
