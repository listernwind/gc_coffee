const api = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    categories: [],
    activeCat: 0,
    cart: [],
    cartMap: {},
    cartCount: 0,
    cartAmount: '0.00',
    showCart: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
    this.loadMenu();
    this.syncCart();
  },

  loadMenu() {
    api.get('/api/drink/menu').then((list) => {
      const categories = (list || []).map((c) => ({
        id: c.id, name: c.name,
        products: (c.products || []).map((p) => Object.assign({}, p, {
          tagsList: (p.tags || '').split(',').filter(Boolean)
        }))
      }));
      this.setData({ categories });
    }).catch(() => {});
  },

  // 购物车：globalData.cart 与页面同步
  syncCart() {
    const cart = getApp().globalData.cart || [];
    const count = cart.reduce((s, i) => s + i.qty, 0);
    const amount = cart.reduce((s, i) => s + i.price * i.qty, 0);
    const cartMap = {};
    cart.forEach((i) => { cartMap[i.productId] = i.qty; });
    this.setData({ cart, cartMap, cartCount: count, cartAmount: util.money(amount) });
  },

  selectCat(e) {
    this.setData({ activeCat: Number(e.currentTarget.dataset.index) });
  },

  addToCart(e) {
    const p = e.currentTarget.dataset.item;
    const cart = getApp().globalData.cart || [];
    const hit = cart.find((i) => i.productId === p.id);
    if (hit) hit.qty += 1;
    else cart.push({ productId: p.id, name: p.name, price: p.price, image: p.image, qty: 1 });
    getApp().globalData.cart = cart;
    this.syncCart();
  },

  // 购物车浮层中的加号
  addOne(e) {
    const id = Number(e.currentTarget.dataset.id);
    const cart = getApp().globalData.cart || [];
    const hit = cart.find((i) => i.productId === id);
    if (hit) hit.qty += 1;
    getApp().globalData.cart = cart;
    this.syncCart();
  },

  minus(e) {
    const id = Number(e.currentTarget.dataset.id);
    const cart = getApp().globalData.cart || [];
    const idx = cart.findIndex((i) => i.productId === id);
    if (idx >= 0) {
      cart[idx].qty -= 1;
      if (cart[idx].qty <= 0) cart.splice(idx, 1);
      getApp().globalData.cart = cart;
      this.syncCart();
    }
  },

  toggleCart() {
    if (this.data.cartCount > 0) this.setData({ showCart: !this.data.showCart });
  },

  clearCart() {
    getApp().globalData.cart = [];
    this.syncCart();
    this.setData({ showCart: false });
  },

  goCheckout() {
    if (this.data.cartCount === 0) {
      wx.showToast({ title: '先选一杯吧', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/checkout/checkout' });
  }
});
