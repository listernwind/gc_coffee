const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    month: '',
    summary: null,
    records: [],
    percent: 0
  },

  onLoad(options) {
    if (options.month) this.setData({ month: options.month });
  },

  onShow() {
    this.load();
  },

  load() {
    api.get('/api/coffee/monthly', { month: this.data.month }).then((data) => {
      const records = (data.records || []).map((r) => Object.assign({}, r, {
        statusLabel: util.RESERVE_STATUS[r.status] || r.status,
        statusClass: 'st-' + r.status,
        dateLabel: util.dateLabel(r.deliveryDate)
      }));
      const total = data.quotaTotal || 0;
      const percent = total > 0 ? Math.round((data.quotaUsed / total) * 100) : 0;
      this.setData({ summary: data, records, percent });
    }).catch(() => {});
  }
});
