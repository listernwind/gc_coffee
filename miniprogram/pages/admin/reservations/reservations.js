const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    dates: [],
    date: '',
    statuses: [
      { key: 'ALL', label: '全部' },
      { key: 'PENDING', label: '待确认' },
      { key: 'CONFIRMED', label: '已确认' },
      { key: 'DELIVERING', label: '派送中' },
      { key: 'DELIVERED', label: '已送达' }
    ],
    status: 'ALL',
    records: [],
    page: 1,
    hasMore: true
  },

  onLoad() {
    const days = ['全部'];
    for (let i = 0; i < 7; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      days.push(util.fmtDate(d));
    }
    this.setData({ dates: days });
  },

  onShow() {
    this.setData({ records: [], page: 1, hasMore: true });
    this.load();
  },

  selectDate(e) {
    const idx = Number(e.currentTarget.dataset.index);
    this.setData({ date: idx === 0 ? '' : this.data.dates[idx] });
    this.reload();
  },

  selectStatus(e) {
    this.setData({ status: e.currentTarget.dataset.key });
    this.reload();
  },

  reload() {
    this.setData({ records: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/admin/ops/reservations', {
      page: this.data.page, size: 15,
      date: this.data.date || undefined,
      status: this.data.status
    }).then((data) => {
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

  onReachBottom() { this.load(); },

  setStatus(e) {
    const { id, status, label } = e.currentTarget.dataset;
    wx.showModal({
      title: '状态变更',
      content: '将预订变更为「' + label + '」？',
      success: (res) => {
        if (!res.confirm) return;
        api.put('/api/admin/ops/reservations/' + id + '/status', { status }).then(() => {
          wx.showToast({ title: '已更新', icon: 'none' });
          this.reload();
        }).catch(() => {});
      }
    });
  },

  cancel(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消预订',
      content: '取消后金额将退回用户余额，额度将恢复。',
      success: (res) => {
        if (!res.confirm) return;
        api.put('/api/admin/ops/reservations/' + id + '/cancel', {}).then(() => {
          wx.showToast({ title: '已取消', icon: 'none' });
          this.reload();
        }).catch(() => {});
      }
    });
  }
});
