const api = require('../../utils/request');

Page({
  data: { records: [], page: 1, hasMore: true },

  onShow() {
    this.setData({ records: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/user/points-records', { page: this.data.page, size: 20 }).then((data) => {
      const list = (data.records || []).map((r) => Object.assign({}, r, {
        absPoints: Math.abs(r.changePoints),
        positive: r.changePoints >= 0
      }));
      this.setData({
        records: this.data.records.concat(list),
        page: this.data.page + 1,
        hasMore: this.data.records.length + list.length < data.total
      });
    }).catch(() => {});
  },

  onReachBottom() { this.load(); }
});
