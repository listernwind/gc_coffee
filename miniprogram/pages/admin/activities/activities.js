const api = require('../../../utils/request');

Page({
  data: {
    tabs: [{ key: 'activity', label: '月度/季度活动' }, { key: 'poster', label: '海报' }],
    activeTab: 'activity',
    activities: [],
    posters: [],
    showForm: false,
    editing: null,
    form: {},
    isPoster: false
  },

  onShow() { this.load(); },

  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.key }); },

  load() {
    api.get('/api/admin/marketing/activities').then((list) => {
      this.setData({ activities: (list || []).map((a) => Object.assign({}, a, {
        typeLabel: a.type === 'MONTHLY' ? '月度' : '季度',
        period: (a.startAt || '').substring(0, 10) + ' ~ ' + (a.endAt || '').substring(0, 10)
      })) });
    }).catch(() => {});
    api.get('/api/admin/marketing/posters').then((list) => this.setData({ posters: list || [] })).catch(() => {});
  },

  openForm(e) {
    const item = e.currentTarget.dataset.item;
    const isPoster = this.data.activeTab === 'poster';
    const base = isPoster
      ? { title: '', image: '', linkType: 'NONE', linkId: null, active: 1, sort: 0 }
      : { title: '', type: 'MONTHLY', subtitle: '', content: '', image: '', active: 1, sort: 0, startAt: '', endAt: '' };
    this.setData({ editing: item || null, form: item ? Object.assign({}, base, item) : base, showForm: true, isPoster });
  },

  closeForm() { this.setData({ showForm: false }); },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  onType(e) {
    this.setData({ 'form.type': e.detail.value ? 'QUARTERLY' : 'MONTHLY' });
  },

  onActive(e) {
    this.setData({ 'form.active': e.detail.value ? 1 : 0 });
  },

  save() {
    const url = this.data.isPoster ? '/api/admin/marketing/posters' : '/api/admin/marketing/activities';
    const form = Object.assign({}, this.data.form);
    if (this.data.isPoster) {
      form.linkType = form.linkId && Number(form.linkId) > 0 ? 'ACTIVITY' : 'NONE';
    }
    api.post(url, form).then(() => {
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ showForm: false });
      this.load();
    }).catch(() => {});
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    const url = this.data.activeTab === 'poster' ? '/api/admin/marketing/posters' : '/api/admin/marketing/activities';
    wx.showModal({
      title: '删除',
      content: '确定删除吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.del(url + '/' + id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' });
          this.load();
        }).catch(() => {});
      }
    });
  }
});
