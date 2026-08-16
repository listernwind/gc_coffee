/**
 * utils/util.js 单元测试
 * 运行：npm test（jest）
 */
const util = require('../../utils/util');

describe('金额格式化', () => {
  test('money 保留两位小数', () => {
    expect(util.money(0)).toBe('0.00');
    expect(util.money(15)).toBe('15.00');
    expect(util.money(15.5)).toBe('15.50');
    expect(util.money(15.556)).toBe('15.56');
    // 15.555 二进制浮点实为 15.55499...，JS 舍入得 15.55（浏览器/小程序行为一致）
    expect(util.money(15.555)).toBe('15.55');
    expect(util.money(undefined)).toBe('0.00');
    expect(util.money(null)).toBe('0.00');
  });

  test('moneyInt 整数去尾零', () => {
    expect(util.moneyInt(962)).toBe('962');
    expect(util.moneyInt(962.0)).toBe('962');
    expect(util.moneyInt(42.5)).toBe('42.50');
    expect(util.moneyInt(0)).toBe('0');
    expect(util.moneyInt('360.00')).toBe('360');
  });
});

describe('日期工具', () => {
  test('dateLabel 输出「x月x日 周x」', () => {
    const d = new Date(2026, 7, 17); // 2026-08-17 周一
    const week = '日一二三四五六'.charAt(d.getDay());
    expect(util.dateLabel('2026-08-17')).toBe(`8月17日 周${week}`);
  });

  test('fmtDate 输出 yyyy-MM-dd', () => {
    expect(util.fmtDate(new Date(2026, 7, 17))).toBe('2026-08-17');
  });

  test('tomorrow 为明天', () => {
    const t = new Date();
    t.setDate(t.getDate() + 1);
    expect(util.tomorrow()).toBe(util.fmtDate(t));
  });
});

describe('优惠券描述', () => {
  test('折扣券', () => {
    expect(util.couponLabel({ type: 'DISCOUNT', value: 0.9 })).toBe('9折');
    expect(util.couponDesc({ type: 'DISCOUNT', value: 0.9, minAmount: 0 })).toBe('无门槛 · 9折');
  });
  test('现金券', () => {
    expect(util.couponLabel({ type: 'CASH', value: 5 })).toBe('¥5.00');
  });
  test('满减券', () => {
    expect(util.couponLabel({ type: 'FULL_REDUCTION', value: 6 })).toBe('减¥6.00');
    expect(util.couponDesc({ type: 'FULL_REDUCTION', value: 6, minAmount: 30 })).toBe('满30.00可用 · 减¥6.00');
  });
});

describe('等级配色', () => {
  test('四个等级颜色', () => {
    expect(util.levelColor('黑金会员')).toBe('#3B2B20');
    expect(util.levelColor('金卡会员')).toBe('#C08A4E');
    expect(util.levelColor('银卡会员')).toBe('#8E9AA8');
    expect(util.levelColor('普通会员')).toBe('#B9AB99');
  });
});

describe('状态文案', () => {
  test('饮品状态', () => {
    expect(util.DRINK_STATUS.PAID).toBe('待制作');
    expect(util.DRINK_STATUS.READY).toBe('待取餐');
  });
  test('预订状态', () => {
    expect(util.RESERVE_STATUS.PENDING).toBe('待确认');
    expect(util.RESERVE_STATUS.DELIVERED).toBe('已送达');
  });
});
