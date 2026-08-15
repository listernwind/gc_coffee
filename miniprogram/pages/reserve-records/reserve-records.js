const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    records: [],
    page: 1,
    hasMore: true
  },

  onShow() {
    this.setData({ records: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/coffee/reservations', { page: this.data.page, size: 10 }).then((data) => {
      const list = (data.records || []).map((r) => Object.assign({}, r, {
        statusLabel: util.RESERVE_STATUS[r.status] || r.status,
        statusClass: 'st-' + r.status,
        dateLabel: util.dateLabel(r.deliveryDate)
      }));
      this.setData({
        records: this.data.records.concat(list),
        page: this.data.page + 1,
        hasMore: this.data.records.length + list.length < data.total
      });
    }).catch(() => {});
  },

  onReachBottom() {
    this.load();
  },

  cancel(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消预订',
      content: '确定取消该预订吗？已支付的金额将退回，额度将恢复。',
      success: (res) => {
        if (!res.confirm) return;
        api.post('/api/coffee/reservations/' + id + '/cancel').then(() => {
          wx.showToast({ title: '已取消', icon: 'none' });
          this.setData({ records: [], page: 1, hasMore: true });
          this.load();
        }).catch(() => {});
      }
    });
  },

  goMonthly() {
    wx.navigateTo({ url: '/pages/monthly/monthly' });
  }
});
