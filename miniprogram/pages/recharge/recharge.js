const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    amounts: [50, 100, 200, 300, 500],
    amount: 100,
    custom: '',
    submitting: false
  },

  select(e) {
    this.setData({ amount: Number(e.currentTarget.dataset.amount), custom: '' });
  },

  onCustom(e) {
    const v = e.detail.value;
    this.setData({ custom: v, amount: v ? Number(v) : 0 });
  },

  submit() {
    const amount = this.data.amount;
    if (!amount || amount <= 0) {
      return wx.showToast({ title: '请输入充值金额', icon: 'none' });
    }
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    wx.showLoading({ title: '模拟支付中...' });
    // 模拟微信支付：直接调用后端充值接口
    api.post('/api/user/recharge', { amount }).then((balance) => {
      wx.hideLoading();
      wx.showModal({
        title: '充值成功',
        content: '已到账 ¥' + util.money(amount) + '，当前余额 ¥' + util.money(balance),
        showCancel: false,
        success: () => wx.navigateBack()
      });
    }).catch(() => {
      wx.hideLoading();
      this.setData({ submitting: false });
    });
  }
});
