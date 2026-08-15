const api = require('../../utils/request');

Page({
  data: { activity: null },

  onLoad(options) {
    this.setData({ id: options.id });
    // 活动详情直接从列表接口拿（没有单独 detail 接口时用全部列表查找）
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
  }
});
