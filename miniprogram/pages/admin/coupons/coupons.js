const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    coupons: [],
    showForm: false,
    editing: null,
    form: {}
  },

  onShow() { this.load(); },

  load() {
    api.get('/api/admin/marketing/coupons').then((list) => {
      this.setData({ coupons: (list || []).map((c) => Object.assign({}, c, {
        typeLabel: c.type === 'FULL_REDUCTION' ? '满减' : c.type === 'DISCOUNT' ? '折扣' : '现金券',
        label: util.couponLabel(c)
      })) });
    }).catch(() => {});
  },

  openForm(e) {
    const item = e.currentTarget.dataset.item;
    const base = {
      name: '', type: 'FULL_REDUCTION', value: '', minAmount: 0,
      validDays: 7, totalCount: 0, perUserLimit: 1, redeemPoints: 0, active: 1
    };
    this.setData({ editing: item || null, form: item ? Object.assign({}, base, item) : base, showForm: true });
  },

  closeForm() { this.setData({ showForm: false }); },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  onType(e) {
    const types = ['FULL_REDUCTION', 'DISCOUNT', 'CASH'];
    this.setData({ 'form.type': types[Number(e.detail.value)] });
  },

  onActive(e) {
    this.setData({ 'form.active': e.detail.value ? 1 : 0 });
  },

  save() {
    const form = Object.assign({}, this.data.form);
    api.post('/api/admin/marketing/coupons', form).then(() => {
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ showForm: false });
      this.load();
    }).catch(() => {});
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除优惠券',
      content: '确定删除该券模板吗？已发放的券不受影响。',
      success: (res) => {
        if (!res.confirm) return;
        api.del('/api/admin/marketing/coupons/' + id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' });
          this.load();
        }).catch(() => {});
      }
    });
  }
});
