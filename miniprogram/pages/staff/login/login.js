const api = require('../../../utils/request');

Page({
  data: {
    username: 'staff1',
    password: '',
    loading: false
  },

  onUser(e) { this.setData({ username: e.detail.value }); },
  onPass(e) { this.setData({ password: e.detail.value }); },

  login() {
    const { username, password } = this.data;
    if (!username || !password) {
      return wx.showToast({ title: '请输入账号密码', icon: 'none' });
    }
    this.setData({ loading: true });
    api.post('/api/auth/staff-login', { username, password }).then((data) => {
      wx.setStorageSync('staffToken', data.token);
      wx.setStorageSync('staffName', data.nickname);
      wx.showToast({ title: '登录成功', icon: 'none' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/staff/home/home' });
      }, 500);
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  backHome() {
    wx.switchTab({ url: '/pages/home/home' });
  }
});
