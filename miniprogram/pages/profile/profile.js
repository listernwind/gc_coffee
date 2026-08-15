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

  // 管理后台：有店长 token 直接进，否则先登录
  goAdmin() {
    if (wx.getStorageSync('adminToken')) {
      wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' });
    } else {
      wx.navigateTo({ url: '/pages/admin-login/admin-login' });
    }
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
