const api = require('../../utils/request');

Page({
  data: {
    profile: null,
    isAdmin: false,
    showAddress: false,
    address: '',
    phone: ''
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 });
    }
    this.load();
  },

  load() {
    api.get('/api/user/me').then((u) => {
      this.setData({ profile: u, isAdmin: u.role === 'ADMIN' });
    }).catch(() => {});
  },

  goOrders(e) {
    const status = e.currentTarget.dataset.status || '';
    wx.navigateTo({ url: '/pages/orders/orders?status=' + status });
  },
  goReserveRecords() { wx.navigateTo({ url: '/pages/reserve-records/reserve-records' }); },
  goMonthly() { wx.navigateTo({ url: '/pages/monthly/monthly' }); },
  goCoupons() { wx.navigateTo({ url: '/pages/coupons/coupons' }); },
  goActivity() { wx.navigateTo({ url: '/pages/activity/activity' }); },
  goBalance() { wx.navigateTo({ url: '/pages/balance-records/balance-records' }); },
  goPoints() { wx.navigateTo({ url: '/pages/points-records/points-records' }); },
  goRecharge() { wx.navigateTo({ url: '/pages/recharge/recharge' }); },
  goMember() { wx.switchTab({ url: '/pages/member/member' }); },

  // 管理后台：店长 token 有效直接进，无效/过期则引导登录
  goAdmin() {
    if (!wx.getStorageSync('adminToken')) {
      wx.navigateTo({ url: '/pages/admin-login/admin-login' });
      return;
    }
    api.get('/api/admin/stats/overview').then(() => {
      wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' });
    }).catch(() => {
      wx.navigateTo({ url: '/pages/admin-login/admin-login' });
    });
  },

  // 派送员工作台：token 有效直接进，无效/过期则引导登录
  goStaff() {
    if (!wx.getStorageSync('staffToken')) {
      wx.navigateTo({ url: '/pages/staff/login/login' });
      return;
    }
    api.get('/api/staff/me').then(() => {
      wx.navigateTo({ url: '/pages/staff/home/home' });
    }).catch(() => {
      wx.navigateTo({ url: '/pages/staff/login/login' });
    });
  },

  openAddress() {
    this.setData({ showAddress: true, address: this.data.profile.defaultAddress || '', phone: this.data.profile.phone || '' });
  },
  closeAddress() { this.setData({ showAddress: false }); },
  onAddr(e) { this.setData({ address: e.detail.value }); },
  onPhone(e) { this.setData({ phone: e.detail.value }); },

  saveAddress() {
    api.put('/api/user/me', {
      nickname: this.data.profile.nickname,
      avatar: this.data.profile.avatar,
      phone: this.data.phone,
      defaultAddress: this.data.address
    }).then(() => {
      wx.setStorageSync('defaultAddress', this.data.address);
      wx.setStorageSync('contactName', this.data.profile.nickname);
      wx.setStorageSync('contactPhone', this.data.phone);
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ showAddress: false });
      this.load();
    }).catch(() => {});
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需重新登录才能下单预订。',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('token');
        wx.removeStorageSync('role');
        wx.removeStorageSync('nickname');
        wx.reLaunch({ url: '/pages/login/login' });
      }
    });
  },

  // 开发辅助：切换演示账号（mock 模式）
  switchDemo() {
    wx.showModal({
      title: '切换演示账号',
      content: '切换为预置演示用户（余额200元/积分500/已购月卡），方便体验完整流程。',
      success: (res) => {
        if (!res.confirm) return;
        getApp().login('mock_demo').then(() => {
          wx.showToast({ title: '已切换', icon: 'none' });
          this.load();
        });
      }
    });
  }
});
