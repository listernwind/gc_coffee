const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    shopName: 'GC Coffee 咖啡小馆',
    notice: '',
    today: '',
    week: '',
    posters: [],
    hasPackage: false,
    remain: 0,
    packageName: ''
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
    const now = new Date();
    this.setData({
      today: util.fmtDate(now),
      week: '周' + '日一二三四五六'.charAt(now.getDay())
    });
    this.loadHome();
    this.loadCoffeeCard();
  },

  loadHome() {
    api.get('/api/public/home').then((data) => {
      const s = data.settings || {};
      this.setData({
        shopName: s.shop_name || 'GC Coffee 咖啡小馆',
        notice: s.shop_notice || '',
        posters: data.posters || []
      });
    }).catch(() => {});
  },

  loadCoffeeCard() {
    api.get('/api/coffee/home-card').then((data) => {
      this.setData({
        hasPackage: data.hasPackage,
        remain: data.remain,
        packageName: data.packageName
      });
    }).catch(() => {});
  },

  goReserve() { wx.navigateTo({ url: '/pages/reserve/reserve' }); },
  goMenu() { wx.switchTab({ url: '/pages/menu/menu' }); },
  goMonthly() { wx.navigateTo({ url: '/pages/monthly/monthly' }); },
  goReserveRecords() { wx.navigateTo({ url: '/pages/reserve-records/reserve-records' }); },
  goActivity() { wx.navigateTo({ url: '/pages/activity/activity' }); },
  goCoupons() { wx.navigateTo({ url: '/pages/coupons/coupons' }); },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }); },

  onPosterTap(e) {
    const p = e.currentTarget.dataset.item;
    if (p.linkType === 'ACTIVITY' && p.linkId) {
      wx.navigateTo({ url: '/pages/activity-detail/activity-detail?id=' + p.linkId });
    }
  }
});
