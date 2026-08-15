const api = require('../../../utils/request');
const util = require('../../../utils/util');

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
    api.get('/api/admin/ops/orders', {
      page: this.data.page, size: 15,
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

  onReachBottom() { this.load(); },

  setStatus(e) {
    const { id, status, label } = e.currentTarget.dataset;
    api.put('/api/admin/ops/orders/' + id + '/status', { status }).then(() => {
      wx.showToast({ title: '已更新为' + label, icon: 'none' });
      this.setData({ orders: [], page: 1, hasMore: true });
      this.load();
    }).catch(() => {});
  }
});
