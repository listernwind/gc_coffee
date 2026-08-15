const api = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    staff: { name: '', phone: '' },
    tabs: [
      { key: 'today', label: '今天' },
      { key: 'tomorrow', label: '明天' },
      { key: 'all', label: '全部' }
    ],
    activeTab: 'today',
    tasks: [],
    pendingCount: 0,
    mineCount: 0,
    doneCount: 0
  },

  onShow() {
    if (!wx.getStorageSync('staffToken')) {
      wx.redirectTo({ url: '/pages/staff/login/login' });
      return;
    }
    this.load();
  },

  onPullDownRefresh() {
    this.loadTasks();
    setTimeout(() => wx.stopPullDownRefresh(), 500);
  },

  load() {
    api.get('/api/staff/me').then((s) => {
      this.setData({ staff: s });
    }).catch(() => {});
    this.loadTasks();
  },

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.key });
    this.loadTasks();
  },

  loadTasks() {
    api.get('/api/staff/tasks', { date: this.data.activeTab }).then((list) => {
      const me = wx.getStorageSync('staffName') || '';
      const tasks = (list || []).map((t) => Object.assign({}, t, {
        statusLabel: util.RESERVE_STATUS[t.status] || t.status,
        statusClass: 'st-' + t.status,
        dateLabel: util.dateLabel(t.deliveryDate),
        isMine: t.staffId && t.staffName === me
      }));
      this.setData({
        tasks,
        pendingCount: tasks.filter((t) => t.status === 'PENDING').length,
        mineCount: tasks.filter((t) => t.status === 'CONFIRMED' || t.status === 'DELIVERING').length,
        doneCount: tasks.filter((t) => t.status === 'DELIVERED').length
      });
    }).catch(() => {});
  },

  call(e) {
    const phone = e.currentTarget.dataset.phone;
    if (phone) wx.makePhoneCall({ phoneNumber: phone });
  },

  claim(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '认领订单',
      content: '认领后由你负责派送该单。',
      success: (res) => {
        if (!res.confirm) return;
        api.put('/api/staff/tasks/' + id + '/claim', {}).then(() => {
          wx.showToast({ title: '已认领', icon: 'none' });
          this.loadTasks();
        }).catch(() => {});
      }
    });
  },

  unclaim(e) {
    const id = e.currentTarget.dataset.id;
    api.put('/api/staff/tasks/' + id + '/unclaim', {}).then(() => {
      wx.showToast({ title: '已放弃认领', icon: 'none' });
      this.loadTasks();
    }).catch(() => {});
  },

  setStatus(e) {
    const { id, status, label } = e.currentTarget.dataset;
    wx.showModal({
      title: '状态更新',
      content: '将该单标记为「' + label + '」？',
      success: (res) => {
        if (!res.confirm) return;
        api.put('/api/staff/tasks/' + id + '/status', { status }).then(() => {
          wx.showToast({ title: '已更新', icon: 'none' });
          this.loadTasks();
        }).catch(() => {});
      }
    });
  },

  exit() {
    wx.removeStorageSync('staffToken');
    wx.removeStorageSync('staffName');
    wx.switchTab({ url: '/pages/profile/profile' });
  }
});
