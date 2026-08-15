const api = require('../../utils/request');

Page({
  data: {
    mode: 'wx', // wx 微信登录 / pwd 账号登录 / reg 注册
    username: '',
    password: '',
    nickname: '',
    phone: '',
    loading: false
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },

  onField(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  // 登录成功后按角色分流
  routeByRole(data) {
    wx.setStorageSync('token', data.token);
    wx.setStorageSync('role', data.role);
    wx.setStorageSync('nickname', data.nickname || '');
    if (data.role === 'ADMIN') {
      wx.setStorageSync('adminToken', data.token);
      wx.redirectTo({ url: '/pages/admin/dashboard/dashboard' });
    } else if (data.role === 'STAFF') {
      wx.setStorageSync('staffToken', data.token);
      wx.redirectTo({ url: '/pages/staff/home/home' });
    } else {
      wx.showToast({ title: '欢迎回来', icon: 'none' });
      setTimeout(() => wx.switchTab({ url: '/pages/home/home' }), 500);
    }
  },

  // 微信一键登录（开发期为模拟登录，后端配置 appid 后自动切换真实登录）
  wxLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    getApp().login().then((data) => {
      this.setData({ loading: false });
      this.routeByRole(data);
    }).catch(() => {
      this.setData({ loading: false });
      wx.showToast({ title: '登录失败，请重试', icon: 'none' });
    });
  },

  // 账号密码登录
  pwdLogin() {
    const { username, password } = this.data;
    if (!username) return wx.showToast({ title: '请输入账号', icon: 'none' });
    if (!password) return wx.showToast({ title: '请输入密码', icon: 'none' });
    if (this.data.loading) return;
    this.setData({ loading: true });
    api.post('/api/auth/password-login', { username, password }).then((data) => {
      this.setData({ loading: false });
      this.routeByRole(data);
    }).catch(() => this.setData({ loading: false }));
  },

  // 注册（仅普通用户；店长/派送员账号由门店预置）
  register() {
    const { username, password, nickname, phone } = this.data;
    if (!username) return wx.showToast({ title: '请设置账号', icon: 'none' });
    if (!password || password.length < 6) return wx.showToast({ title: '密码至少 6 位', icon: 'none' });
    if (!nickname) return wx.showToast({ title: '请填写昵称', icon: 'none' });
    if (this.data.loading) return;
    this.setData({ loading: true });
    api.post('/api/auth/register', { username, password, nickname, phone }).then((data) => {
      this.setData({ loading: false });
      wx.showToast({ title: '注册成功', icon: 'none' });
      this.routeByRole(data);
    }).catch(() => this.setData({ loading: false }));
  },

  goAdminLogin() { wx.navigateTo({ url: '/pages/admin-login/admin-login' }); },
  goStaffLogin() { wx.navigateTo({ url: '/pages/staff/login/login' }); }
});
