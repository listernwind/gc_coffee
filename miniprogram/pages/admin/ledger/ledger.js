const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    types: [
      { key: 'ALL', label: '全部' },
      { key: 'DRINK', label: '饮品' },
      { key: 'COFFEE', label: '咖啡液' },
      { key: 'RECHARGE', label: '充值' },
      { key: 'PACKAGE', label: '套餐' }
    ],
    type: 'ALL',
    startDate: '',
    endDate: '',
    rows: [],
    summary: null,
    loading: false
  },

  onLoad() {
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    this.setData({
      startDate: util.fmtDate(first),
      endDate: util.fmtDate(now)
    });
  },

  onShow() {
    this.load();
  },

  selectType(e) {
    this.setData({ type: e.currentTarget.dataset.key });
    this.load();
  },

  onStart(e) { this.setData({ startDate: e.detail.value }); },
  onEnd(e) { this.setData({ endDate: e.detail.value }); },

  load() {
    this.setData({ loading: true });
    api.get('/api/admin/stats/ledger', {
      startDate: this.data.startDate,
      endDate: this.data.endDate,
      type: this.data.type
    }).then((data) => {
      const rows = (data.rows || []).map((r) => Object.assign({}, r, {
        amountText: util.money(r.amount),
        statusText: r.type === 'COFFEE' ? (util.RESERVE_STATUS[r.status] || r.status) : (r.status === 'OK' ? '正常' : (util.DRINK_STATUS[r.status] || r.status))
      }));
      this.setData({ rows, summary: data.summary, loading: false });
    }).catch(() => this.setData({ loading: false }));
  },

  // 复制 CSV 到剪贴板
  copyCsv() {
    const url = getApp().globalData.baseUrl + '/api/admin/stats/ledger/csv'
      + '?startDate=' + this.data.startDate + '&endDate=' + this.data.endDate + '&type=' + this.data.type;
    wx.request({
      url,
      header: { 'Authorization': 'Bearer ' + wx.getStorageSync('adminToken') },
      success(res) {
        if (res.statusCode === 200) {
          wx.setClipboardData({
            data: res.data,
            success: () => wx.showToast({ title: '台账已复制，粘贴到 Excel 即可', icon: 'none' })
          });
        } else {
          wx.showToast({ title: '导出失败', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '网络异常', icon: 'none' })
    });
  }
});
