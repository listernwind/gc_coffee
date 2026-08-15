Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/home/home', text: '首页', key: 'home' },
      { pagePath: '/pages/menu/menu', text: '点单', key: 'cup' },
      { pagePath: '/pages/member/member', text: '会员', key: 'gem' },
      { pagePath: '/pages/profile/profile', text: '我的', key: 'user' }
    ]
  },
  methods: {
    switchTab(e) {
      const { path, index } = e.currentTarget.dataset;
      if (Number(index) === this.data.selected) return;
      wx.switchTab({ url: path });
    }
  }
});
