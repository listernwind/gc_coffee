const api = require('../../../utils/request');

Page({
  data: {
    staffs: [],
    showForm: false,
    editing: null,
    form: {}
  },

  onShow() { this.load(); },

  load() {
    api.get('/api/admin/staff').then((list) => {
      this.setData({ staffs: list || [] });
    }).catch(() => {});
  },

  openForm(e) {
    const item = e.currentTarget.dataset.item;
    const base = { username: '', password: '', nickname: '', phone: '', status: 1 };
    this.setData({ editing: item || null, form: item ? Object.assign({}, base, item, { password: '' }) : base, showForm: true });
  },

  closeForm() { this.setData({ showForm: false }); },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  save() {
    const f = this.data.form;
    if (!f.username) return wx.showToast({ title: '请填写账号', icon: 'none' });
    if (!f.nickname) return wx.showToast({ title: '请填写姓名', icon: 'none' });
    if (!this.data.editing && !f.password) return wx.showToast({ title: '请设置初始密码', icon: 'none' });
    api.post('/api/admin/staff', f).then(() => {
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ showForm: false });
      this.load();
    }).catch(() => {});
  },

  toggleStatus(e) {
    const s = e.currentTarget.dataset.item;
    const next = s.status === 1 ? 0 : 1;
    wx.showModal({
      title: next === 0 ? '禁用派送员' : '启用派送员',
      content: next === 0 ? '禁用后该派送员将无法登录工作台。' : '确定恢复该派送员的使用吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.post('/api/admin/staff/' + s.id + '/status', { status: next }).then(() => {
          wx.showToast({ title: '已更新', icon: 'none' });
          this.load();
        }).catch(() => {});
      }
    });
  }
});
