App({
  globalData: {
    // 后端地址：
    // - 开发者工具（模拟器）用 127.0.0.1
    // - 手机真机预览用电脑的局域网 IP（手机和电脑需连同一个 WiFi）
    // 如果局域网 IP 变了，只改下面这个常量即可
    lanIp: '192.168.31.135',
    baseUrl: '',
    // 后端配置了真实微信 appid/secret 后，改为 true 走 wx.login 真实登录
    useRealWxLogin: false,
    userInfo: null,
    cart: [] // {productId, name, price, image, descText, qty}
  },

  onLaunch() {
    let platform = 'devtools';
    try {
      platform = wx.getSystemInfoSync().platform;
    } catch (e) { /* ignore */ }
    this.globalData.baseUrl = platform === 'devtools'
      ? 'http://127.0.0.1:8080'
      : 'http://' + this.globalData.lanIp + ':8080';
    // 本地模拟模式：生成稳定的 mock openid（后端未配置 appid 时 code 直接作为 openid）
    let mockUid = wx.getStorageSync('mock_uid');
    if (!mockUid) {
      mockUid = 'u' + Date.now() + Math.floor(Math.random() * 10000);
      wx.setStorageSync('mock_uid', mockUid);
    }
    this.globalData.mockUid = mockUid;
    this.login();
  },

  // 登录：mock 模式用稳定 code（同一设备始终同一用户）；真实模式用 wx.login code
  login(mockCode) {
    const req = require('./utils/request');
    const doLogin = (code, nickname, avatar) => {
      return req.post('/api/auth/login', { code, nickname, avatar }).then((data) => {
        wx.setStorageSync('token', data.token);
        wx.setStorageSync('role', data.role);
        return data;
      });
    };
    if (mockCode) {
      return doLogin(mockCode);
    }
    if (!this.globalData.useRealWxLogin) {
      return doLogin('mock_' + this.globalData.mockUid);
    }
    return new Promise((resolve) => {
      wx.login({
        success: (res) => doLogin(res.code).then(resolve),
        fail: () => resolve(null)
      });
    });
  }
});
