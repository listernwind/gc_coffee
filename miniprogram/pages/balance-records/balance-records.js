const api = require('../../utils/request');
const util = require('../../utils/util');

const TYPES = {
  RECHARGE: ['充值', '+'],
  CONSUME: ['消费', '-'],
  REFUND: ['退款', '+'],
  PACKAGE: ['套餐', '-'],
  ADJUST: ['调整', '']
};

Page({
  data: { records: [], page: 1, hasMore: true },

  onShow() {
    this.setData({ records: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/user/balance-records', { page: this.data.page, size: 20 }).then((data) => {
      const list = (data.records || []).map((r) => {
        const t = TYPES[r.bizType] || [r.bizType, ''];
        return Object.assign({}, r, {
          typeLabel: t[0],
          sign: t[1],
          positive: Number(r.changeAmount) >= 0,
          absAmount: Math.abs(Number(r.changeAmount)).toFixed(2)
        });
      });
      this.setData({
        records: this.data.records.concat(list),
        page: this.data.page + 1,
        hasMore: this.data.records.length + list.length < data.total
      });
    }).catch(() => {});
  },

  onReachBottom() { this.load(); }
});
