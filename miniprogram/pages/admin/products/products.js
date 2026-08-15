const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    tabs: [
      { key: 'drink', label: '饮品' },
      { key: 'coffee', label: '咖啡液' },
      { key: 'package', label: '月卡套餐' }
    ],
    activeTab: 'drink',
    categories: [],
    drinkProducts: [],
    coffeeProducts: [],
    packages: [],
    editing: null, // 正在编辑的对象
    showForm: false,
    form: {}
  },

  onShow() { this.load(); },

  load() {
    api.get('/api/admin/catalog/drink-categories').then((list) => this.setData({ categories: list || [] })).catch(() => {});
    api.get('/api/admin/catalog/drink-products').then((list) => this.setData({ drinkProducts: list || [] })).catch(() => {});
    api.get('/api/admin/catalog/coffee-products').then((list) => this.setData({ coffeeProducts: list || [] })).catch(() => {});
    api.get('/api/admin/catalog/coffee-packages').then((list) => this.setData({ packages: list || [] })).catch(() => {});
  },

  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.key }); },

  // 新增/编辑：kind 决定提交的接口
  openForm(e) {
    const item = e.currentTarget.dataset.item;
    const kind = this.data.activeTab;
    const cats = this.data.categories || [];
    const base = {
      active: 1, sort: 0, price: '', name: '', categoryId: cats.length ? cats[0].id : '',
      spec: '', descText: '', stock: -1, tags: '', bottleCount: 30
    };
    const form = item ? Object.assign({}, base, item) : base;
    if (form.tags && typeof form.tags === 'string') form.tagsText = form.tags;
    const catIndex = cats.findIndex((c) => c.id === form.categoryId);
    form.categoryIndex = catIndex >= 0 ? catIndex : 0;
    this.setData({ editing: item || null, form, showForm: true, kind });
  },

  onCategory(e) {
    const idx = Number(e.detail.value);
    const cat = this.data.categories[idx];
    if (cat) this.setData({ 'form.categoryId': cat.id, 'form.categoryIndex': idx });
  },

  closeForm() { this.setData({ showForm: false }); },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  onSwitch(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value ? 1 : 0 });
  },

  save() {
    const kind = this.data.kind;
    const form = Object.assign({}, this.data.form);
    if (form.tagsText !== undefined) {
      form.tags = form.tagsText;
      delete form.tagsText;
    }
    const url = {
      drink: '/api/admin/catalog/drink-products',
      coffee: '/api/admin/catalog/coffee-products',
      package: '/api/admin/catalog/coffee-packages'
    }[kind];
    api.post(url, form).then(() => {
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ showForm: false });
      this.load();
    }).catch(() => {});
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    const url = {
      drink: '/api/admin/catalog/drink-products',
      coffee: '/api/admin/catalog/coffee-products',
      package: '/api/admin/catalog/coffee-packages'
    }[this.data.activeTab];
    wx.showModal({
      title: '删除',
      content: '确定删除该商品吗？',
      success: (res) => {
        if (!res.confirm) return;
        api.del(url + '/' + id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' });
          this.load();
        }).catch(() => {});
      }
    });
  },

  // 分类管理
  addCategory() {
    wx.showModal({
      title: '新增分类',
      editable: true,
      placeholderText: '分类名称',
      success: (res) => {
        if (res.confirm && res.content) {
          api.post('/api/admin/catalog/drink-categories', { name: res.content, sort: 99 }).then(() => this.load()).catch(() => {});
        }
      }
    });
  }
});
