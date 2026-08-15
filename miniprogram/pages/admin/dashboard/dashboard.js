const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    overview: null,
    trend: [],
    top: null
  },

  onShow() {
    this.load();
  },

  load() {
    api.get('/api/admin/stats/overview').then((o) => {
      this.setData({
        overview: Object.assign({}, o, {
          todaySalesText: util.moneyInt(o.todaySales),
          todayDrinkText: util.moneyInt(o.todayDrink),
          todayCoffeeText: util.moneyInt(o.todayCoffee),
          todayRechargeText: util.moneyInt(o.todayRecharge),
          monthSalesText: util.moneyInt(o.monthSales)
        })
      });
    }).catch(() => {});
    api.get('/api/admin/stats/trend', { days: 7 }).then((list) => {
      const vals = (list || []).map((x) => Number(x.sales));
      const max = Math.max.apply(null, vals.concat([1]));
      const trend = (list || []).map((x) => ({
        date: x.date,
        shortDate: (x.date || '').substring(5),
        sales: Number(x.sales),
        salesText: util.moneyInt(x.sales),
        orders: x.orders,
        barH: Math.round(Math.max(6, Number(x.sales) / max * 120))
      }));
      this.setData({ trend });
    }).catch(() => {});
    api.get('/api/admin/stats/top', { n: 5 }).then((t) => {
      const all = (t.drinks || []).concat(t.coffees || []);
      const maxQty = Math.max.apply(null, all.map((x) => x.quantity).concat([1]));
      const decorate = (arr, unit) => (arr || []).map((x) => ({
        name: x.name, quantity: x.quantity, unit,
        pct: Math.round(x.quantity / maxQty * 100)
      }));
      this.setData({
        top: {
          drinks: decorate(t.drinks, '杯'),
          coffees: decorate(t.coffees, '瓶')
        }
      });
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
