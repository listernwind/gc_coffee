const api = require('../../utils/request');

Page({
  data: { activity: null },

  onLoad(options) {
    this.setData({ id: options.id });
    // 详情接口支持已结束活动（海报跳转可能指向历史活动）
    api.get('/api/activity/detail', { id: options.id }).then((a) => {
      this.setData({
        activity: Object.assign({}, a, {
          period: (a.startAt || '').substring(0, 10) + ' ~ ' + (a.endAt || '').substring(0, 10)
        })
      });
    }).catch(() => {
      // 兜底：从列表查找
      api.get('/api/activity/list', { type: 'ALL' }).then((list) => {
        const a = (list || []).find((x) => String(x.id) === String(options.id));
        if (a) {
          this.setData({
            activity: Object.assign({}, a, {
              period: (a.startAt || '').substring(0, 10) + ' ~ ' + (a.endAt || '').substring(0, 10)
            })
          });
        }
      }).catch(() => {});
    });
  }
});
