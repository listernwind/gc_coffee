const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    products: [],
    productId: null,
    quantity: 1,
    usePackage: true,
    packageInfo: null, // {hasPackage, remain, packageName}
    days: [], // [{date, label, week}]
    dayIndex: 0,
    slots: [],
    slotIndex: -1,
    community: '',
    address: '',
    contactName: '',
    contactPhone: '',
    payType: 'WX_MOCK',
    balance: '0.00',
    // 结算预览
    packageUsed: 0,
    payQty: 0,
    amount: '0.00',
    submitting: false
  },

  onLoad() {
    this.setData({
      address: wx.getStorageSync('defaultAddress') || '',
      contactName: wx.getStorageSync('contactName') || '',
      contactPhone: wx.getStorageSync('contactPhone') || ''
    });
  },

  onShow() {
    this.loadBase();
  },

  loadBase() {
    api.get('/api/coffee/products').then((products) => {
      this.setData({ products: products || [] });
      if (!this.data.productId && products.length) {
        this.setData({ productId: products[0].id });
        this.preview();
      }
    }).catch(() => {});
    api.get('/api/public/settings').then((s) => {
      const slots = JSON.parse(s.delivery_slots || '[]');
      this.setData({ community: s.community_name || '', slots, slotIndex: slots.length ? 0 : -1 });
      if (!slots.length) {
        wx.showToast({ title: '门店暂未配置派送时段，请联系店主', icon: 'none' });
      }
    }).catch(() => {});
    // 可选派送日期：由后端计算（次日开始，超过截止时间顺延到后天）
    api.get('/api/coffee/delivery-days', { n: 7 }).then((list) => {
      const days = (list || []).map((date) => {
        const d = new Date(date.replace(/-/g, '/'));
        const diff = Math.round((d - new Date(util.fmtDate(new Date()).replace(/-/g, '/')))) / 86400000;
        return {
          date,
          label: (d.getMonth() + 1) + '/' + d.getDate(),
          week: diff === 1 ? '明天' : diff === 2 ? '后天' : '周' + '日一二三四五六'.charAt(d.getDay())
        };
      });
      this.setData({ days });
    }).catch(() => {});
    api.get('/api/coffee/package/current').then((up) => {
      this.setData({
        packageInfo: up ? {
          hasPackage: true,
          remain: up.totalQuota - up.usedQuota,
          name: up.packageName,
          month: up.month
        } : { hasPackage: false, remain: 0 }
      });
      this.preview();
    }).catch(() => {});
    api.get('/api/user/me').then((u) => {
      this.setData({ balance: util.money(u.balance || 0) });
    }).catch(() => {});
  },

  selectProduct(e) {
    this.setData({ productId: Number(e.currentTarget.dataset.id) });
    this.preview();
  },

  changeQty(e) {
    const delta = Number(e.currentTarget.dataset.delta);
    this.setData({ quantity: Math.max(1, this.data.quantity + delta) });
    this.preview();
  },

  togglePackage(e) {
    this.setData({ usePackage: e.detail.value });
    this.preview();
  },

  selectDay(e) {
    this.setData({ dayIndex: Number(e.currentTarget.dataset.index) });
  },

  selectSlot(e) {
    this.setData({ slotIndex: Number(e.currentTarget.dataset.index) });
  },

  selectPay(e) {
    this.setData({ payType: e.currentTarget.dataset.type });
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  // 计算额度抵扣与应付
  preview() {
    const { products, productId, quantity, usePackage, packageInfo } = this.data;
    const product = products.find((p) => p.id === productId);
    if (!product) return;
    let packageUsed = 0;
    if (usePackage && packageInfo && packageInfo.hasPackage) {
      packageUsed = Math.min(quantity, packageInfo.remain);
    }
    const payQty = quantity - packageUsed;
    this.setData({
      packageUsed,
      payQty,
      amount: util.money(payQty * Number(product.price))
    });
  },

  submit() {
    const { productId, quantity, usePackage, days, dayIndex, slots, slotIndex,
      address, contactName, contactPhone, payType, community } = this.data;
    if (!productId) return wx.showToast({ title: '请选择产品', icon: 'none' });
    if (!slots.length) return wx.showToast({ title: '门店暂未配置派送时段，请联系店主', icon: 'none' });
    if (!days.length) return wx.showToast({ title: '暂无可选派送日期', icon: 'none' });
    if (slotIndex < 0) return wx.showToast({ title: '请选择派送时段', icon: 'none' });
    if (!address.trim()) return wx.showToast({ title: '请填写楼栋门牌号', icon: 'none' });
    if (!contactName.trim()) return wx.showToast({ title: '请填写联系人', icon: 'none' });
    if (!/^1\d{10}$/.test(contactPhone.trim())) return wx.showToast({ title: '请填写正确的手机号', icon: 'none' });

    wx.showModal({
      title: '确认预订',
      content: (community || '') + ' ' + address + '\n' + days[dayIndex].date + ' ' + slots[slotIndex]
        + (this.data.packageUsed > 0 ? '\n额度抵扣 ' + this.data.packageUsed + ' 瓶' : '')
        + (this.data.payQty > 0 ? '\n需支付 ' + this.data.payQty + ' 瓶 ¥' + this.data.amount : ''),
      success: (res) => {
        if (!res.confirm) return;
        this.doSubmit();
      }
    });
  },

  doSubmit() {
    if (this.data.submitting) return;
    const { days, dayIndex, slots, slotIndex } = this.data;
    this.setData({ submitting: true });
    api.post('/api/coffee/reserve', {
      productId: this.data.productId,
      quantity: this.data.quantity,
      usePackage: this.data.usePackage,
      payType: this.data.payType,
      deliveryDate: days[dayIndex].date,
      timeSlot: slots[slotIndex],
      address: this.data.address.trim(),
      contactName: this.data.contactName.trim(),
      contactPhone: this.data.contactPhone.trim()
    }).then(() => {
      wx.showToast({ title: '预订成功，次日送达', icon: 'none' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/reserve-records/reserve-records' });
      }, 900);
    }).catch(() => {
      this.setData({ submitting: false });
    });
  }
});
