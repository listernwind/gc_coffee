const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    items: [],
    total: '0.00',
    discount: '0.00',
    payAmount: '0.00',
    payType: 'WX_MOCK',
    balance: '0.00',
    balanceEnough: true,
    couponId: null,
    couponLabel: '',
    usableCoupons: [],
    showCoupons: false,
    remark: '',
    submitting: false
  },

  onLoad() {
    const items = getApp().globalData.cart || [];
    if (!items.length) {
      wx.showToast({ title: '购物车是空的', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 600);
      return;
    }
    this.setData({ items });
    this.refresh();
  },

  refresh() {
    const items = this.data.items;
    const total = items.reduce((s, i) => s + i.price * i.qty, 0);
    this.setData({ total: util.money(total) });
    this.applyCoupon(total);
  },

  // 计算优惠与应付
  applyCoupon(total) {
    const { couponId, usableCoupons } = this.data;
    let discount = 0;
    let label = '';
    if (couponId) {
      const c = usableCoupons.find((x) => x.id === couponId);
      if (c) {
        const min = Number(c.minAmount || 0);
        if (total < min) {
          this.setData({ couponId: null, couponLabel: '' });
          return this.applyCoupon(total);
        }
        discount = Number(apiDiscount(c, total));
        label = c.name + ' -' + util.money(discount);
      }
    }
    const pay = Math.max(total - discount, 0);
    this.setData({
      discount: util.money(discount),
      payAmount: util.money(pay),
      balanceEnough: Number(this.data.balance) >= pay
    });
    if (label) this.setData({ couponLabel: label });
  },

  onShow() {
    this.loadBalance();
    this.loadCoupons();
  },

  loadBalance() {
    api.get('/api/user/me').then((u) => {
      const balance = Number(u.balance || 0);
      this.setData({
        balance: util.money(balance),
        balanceEnough: balance >= Number(this.data.payAmount)
      });
    }).catch(() => {});
  },

  loadCoupons() {
    const total = Number(this.data.total);
    api.get('/api/coupon/usable', { amount: total }).then((list) => {
      const withDesc = (list || []).map((c) => Object.assign({}, c, { desc: util.couponDesc(c) }));
      this.setData({ usableCoupons: withDesc });
    }).catch(() => {});
  },

  selectPay(e) {
    this.setData({ payType: e.currentTarget.dataset.type });
  },

  toggleCoupons() {
    if (this.data.usableCoupons.length) this.setData({ showCoupons: !this.data.showCoupons });
  },

  chooseCoupon(e) {
    const id = Number(e.currentTarget.dataset.id);
    this.setData({ couponId: id || null, showCoupons: false });
    this.applyCoupon(Number(this.data.total));
  },

  onRemark(e) {
    this.setData({ remark: e.detail.value });
  },

  submit() {
    if (this.data.submitting) return;
    const items = this.data.items.map((i) => ({ productId: i.productId, quantity: i.qty }));
    const payload = {
      items,
      payType: this.data.payType,
      couponId: this.data.couponId || null,
      remark: this.data.remark
    };
    this.setData({ submitting: true });
    api.post('/api/drink/order', payload).then((order) => {
      getApp().globalData.cart = [];
      wx.showToast({ title: '下单成功', icon: 'none' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/order-detail/order-detail?id=' + order.id });
      }, 800);
    }).catch(() => {
      this.setData({ submitting: false });
    });
  }
});

function apiDiscount(c, total) {
  if (c.type === 'DISCOUNT') return total * (1 - Number(c.value));
  return Math.min(Number(c.value), total);
}
