const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    keyword: '',
    users: [],
    page: 1,
    hasMore: true,
    adjust: null, // 正在调整的用户
    adjustType: 'balance', // balance / points
    adjustAmount: '',
    adjustRemark: ''
  },

  onShow() {
    this.setData({ users: [], page: 1, hasMore: true });
    this.load();
  },

  onKeyword(e) { this.setData({ keyword: e.detail.value }); },

  search() {
    this.setData({ users: [], page: 1, hasMore: true });
    this.load();
  },

  load() {
    if (!this.data.hasMore) return;
    api.get('/api/admin/members', {
      page: this.data.page, size: 15, keyword: this.data.keyword
    }).then((data) => {
      const list = (data.records || []).map((u) => Object.assign({}, u, {
        registerDate: (u.createdAt || '').substring(0, 10)
      }));
      this.setData({
        users: this.data.users.concat(list),
        page: this.data.page + 1,
        hasMore: this.data.users.length + list.length < data.total
      });
    }).catch(() => {});
  },

  onReachBottom() { this.load(); },

  openAdjust(e) {
    const user = e.currentTarget.dataset.user;
    const type = e.currentTarget.dataset.type;
    this.setData({
      adjust: user, adjustType: type,
      adjustAmount: '', adjustRemark: ''
    });
  },

  closeAdjust() { this.setData({ adjust: null }); },

  onAmount(e) { this.setData({ adjustAmount: e.detail.value }); },
  onRemark(e) { this.setData({ adjustRemark: e.detail.value }); },

  submitAdjust() {
    const { adjust, adjustType, adjustAmount, adjustRemark } = this.data;
    const url = adjustType === 'balance' ? '/api/admin/members/' + adjust.id + '/balance' : '/api/admin/members/' + adjust.id + '/points';
    const payload = adjustType === 'balance'
      ? { amount: Number(adjustAmount), remark: adjustRemark }
      : { points: Number(adjustAmount), remark: adjustRemark };
    if (!payload.amount && !payload.points) {
      return wx.showToast({ title: '请输入调整数值', icon: 'none' });
    }
    api.post(url, payload).then(() => {
      wx.showToast({ title: '已调整', icon: 'none' });
      this.setData({ adjust: null, users: [], page: 1, hasMore: true });
      this.load();
    }).catch(() => {});
  },

  toggleStatus(e) {
    const user = e.currentTarget.dataset.user;
    const next = user.status === 1 ? 0 : 1;
    wx.showModal({
      title: next === 0 ? '禁用会员' : '启用会员',
      content: next === 0 ? '禁用后该会员将无法登录。' : '确定恢复该会员的使用吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.post('/api/admin/members/' + user.id + '/status', { status: next }).then(() => {
          wx.showToast({ title: '已更新', icon: 'none' });
          this.setData({ users: [], page: 1, hasMore: true });
          this.load();
        }).catch(() => {});
      }
    });
  }
});
