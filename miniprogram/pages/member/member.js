const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    profile: null,
    levelColor: '#B9AB99',
    nextLabel: '',
    packageInfo: null,
    coupons: []
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
    this.load();
  },

  load() {
    api.get('/api/user/me').then((u) => {
      const totalSpend = Number(u.totalSpend || 0);
      let nextLabel = '';
      if (u.nextLevelNeed !== null && u.nextLevelNeed !== undefined) {
        nextLabel = '再消费 ¥' + util.money(u.nextLevelNeed) + ' 升级' + nextLevelName(u.levelName);
      } else {
        nextLabel = '已是最高等级';
      }
      this.setData({
        profile: u,
        levelColor: util.levelColor(u.levelName),
        nextLabel
      });
    }).catch(() => {});
    api.get('/api/coffee/package/current').then((up) => {
      this.setData({
        packageInfo: up ? {
          name: up.packageName,
          remain: up.totalQuota - up.usedQuota,
          total: up.totalQuota,
          month: up.month
        } : null
      });
    }).catch(() => {});
    api.get('/api/coupon/mine', { status: 'UNUSED' }).then((list) => {
      this.setData({ coupons: list || [] });
    }).catch(() => {});
  },

  goRecharge() { wx.navigateTo({ url: '/pages/recharge/recharge' }); },
  goBalanceRecords() { wx.navigateTo({ url: '/pages/balance-records/balance-records' }); },
  goPointsRecords() { wx.navigateTo({ url: '/pages/points-records/points-records' }); },
  goCoupons() { wx.navigateTo({ url: '/pages/coupons/coupons' }); },
  goMonthly() { wx.navigateTo({ url: '/pages/monthly/monthly' }); },
  goReserve() { wx.navigateTo({ url: '/pages/reserve/reserve' }); },
  goActivity() { wx.navigateTo({ url: '/pages/activity/activity' }); }
});

function nextLevelName(cur) {
  if (cur.indexOf('普通') >= 0) return '银卡会员';
  if (cur.indexOf('银卡') >= 0) return '金卡会员';
  if (cur.indexOf('金卡') >= 0) return '黑金会员';
  return '';
}
