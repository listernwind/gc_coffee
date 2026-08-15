const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    overview: null,
    trend: [],
    top: null,
    maxSales: 0
  },

  onShow() {
    this.load();
  },

  load() {
    api.get('/api/admin/stats/overview').then((o) => {
      this.setData({ overview: o });
    }).catch(() => {});
    api.get('/api/admin/stats/trend', { days: 14 }).then((list) => {
      const max = Math.max.apply(null, (list || []).map((x) => Number(x.sales)).concat([1]));
      const trend = (list || []).map((x) => Object.assign({}, x, { shortDate: (x.date || '').substring(5) }));
      this.setData({ trend, maxSales: max });
    }).catch(() => {});
    api.get('/api/admin/stats/top', { n: 5 }).then((t) => {
      this.setData({ top: t });
    }).catch(() => {});
  },

  go(e) {
    wx.navigateTo({ url: e.currentTarget.dataset.url });
  },

  exit() {
    wx.removeStorageSync('adminToken');
    wx.switchTab({ url: '/pages/profile/profile' });
  }
});
