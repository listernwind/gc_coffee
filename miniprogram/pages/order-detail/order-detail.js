const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    id: null,
    order: null,
    items: [],
    statusLabel: '',
    statusClass: ''
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    if (!this.data.id) return;
    api.get('/api/drink/orders/' + this.data.id).then((data) => {
      const order = data.order;
      this.setData({
        order,
        items: data.items || [],
        statusLabel: util.DRINK_STATUS[order.status] || order.status,
        statusClass: 'st-' + order.status
      });
    }).catch(() => {});
  },

  cancel() {
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单吗？款项将原路退回（模拟支付退回余额/原方式）。',
      success: (res) => {
        if (!res.confirm) return;
        api.post('/api/drink/orders/' + this.data.id + '/cancel').then(() => {
          wx.showToast({ title: '已取消', icon: 'none' });
          this.onShow();
        }).catch(() => {});
      }
    });
  }
});
