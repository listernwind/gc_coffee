const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    tabs: [
      { key: 'UNUSED', label: '可用' },
      { key: 'USED', label: '已用' },
      { key: 'EXPIRED', label: '已过期' }
    ],
    activeTab: 'UNUSED',
    coupons: [],
    templates: [],
    profile: null
  },

  onShow() {
    this.loadMine();
    this.loadTemplates();
    api.get('/api/user/me').then((u) => this.setData({ profile: u })).catch(() => {});
  },

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.key });
    this.loadMine();
  },

  loadMine() {
    api.get('/api/coupon/mine', { status: this.data.activeTab }).then((list) => {
      this.setData({ coupons: (list || []).map((c) => this.decorate(c)) });
    }).catch(() => {});
  },

  loadTemplates() {
    api.get('/api/coupon/templates').then((list) => {
      this.setData({ templates: (list || []).map((t) => Object.assign({}, t, {
        label: util.couponLabel(t),
        desc: util.couponDesc(t)
      })) });
    }).catch(() => {});
  },

  decorate(c) {
    return Object.assign({}, c, {
      label: util.couponLabel(c),
      desc: util.couponDesc(c),
      expire: '有效期至 ' + (c.expireAt || '').substring(0, 10)
    });
  },

  claim(e) {
    const id = e.currentTarget.dataset.id;
    api.post('/api/coupon/' + id + '/claim').then(() => {
      wx.showToast({ title: '领取成功', icon: 'none' });
      this.loadTemplates();
      this.loadMine();
    }).catch(() => {});
  },

  redeem(e) {
    const id = e.currentTarget.dataset.id;
    const points = e.currentTarget.dataset.points;
    wx.showModal({
      title: '积分兑换',
      content: '使用 ' + points + ' 积分兑换该优惠券？',
      success: (res) => {
        if (!res.confirm) return;
        api.post('/api/coupon/' + id + '/redeem').then(() => {
          wx.showToast({ title: '兑换成功', icon: 'none' });
          this.loadTemplates();
          this.loadMine();
          api.get('/api/user/me').then((u) => this.setData({ profile: u })).catch(() => {});
        }).catch(() => {});
      }
    });
  }
});
