const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    tabs: [{ key: 'MONTHLY', label: '月度活动' }, { key: 'QUARTERLY', label: '季度活动' }],
    activeTab: 'MONTHLY',
    activities: [],
    posters: []
  },

  onShow() {
    this.load();
  },

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.key });
    this.load();
  },

  load() {
    api.get('/api/activity/list', { type: this.data.activeTab }).then((list) => {
      const now = new Date().getTime();
      this.setData({ activities: (list || []).map((a) => Object.assign({}, a, {
        status: a.endAt ? (new Date(a.endAt.replace(/-/g, '/')).getTime() >= now ? '进行中' : '已结束') : '',
        period: (a.startAt || '').substring(0, 10) + ' ~ ' + (a.endAt || '').substring(0, 10)
      })) });
    }).catch(() => {});
    if (this.data.activeTab === 'MONTHLY') {
      api.get('/api/activity/posters').then((posters) => this.setData({ posters: posters || [] })).catch(() => {});
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/activity-detail/activity-detail?id=' + e.currentTarget.dataset.id });
  }
});
