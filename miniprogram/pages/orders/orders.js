const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    tabs: [
      { key: 'ALL', label: '全部' },
      { key: 'PAID', label: '待制作' },
      { key: 'MAKING', label: '制作中' },
      { key: 'READY', label: '待取餐' },
      { key: 'FINISHED', label: '已完成' }
    ],
    activeTab: 'ALL',
    orders: [],
    page: 1,
    hasMore: true
  },

  onLoad(options) {
    if (options.status) this.setData({ activeTab: options.status });
  },

  onShow() {
    this.setData({ orders: [], page: 1, hasMore: true });
    this.load();
  },

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.key, orders: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/drink/orders', {
      page: this.data.page,
      size: 10,
      status: this.data.activeTab
    }).then((data) => {
      const list = (data.records || []).map((o) => Object.assign({}, o, {
        statusLabel: util.DRINK_STATUS[o.status] || o.status,
        statusClass: 'st-' + o.status
      }));
      this.setData({
        orders: this.data.orders.concat(list),
        page: this.data.page + 1,
        hasMore: this.data.orders.length + list.length < data.total
      });
    }).catch(() => {});
  },

  onReachBottom() {
    this.load();
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id });
  }
});
