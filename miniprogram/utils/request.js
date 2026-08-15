/**
 * 统一请求封装：自动携带 token
 *  - /api/admin 前缀 → 店长 token
 *  - /api/staff 前缀 → 派送员 token
 *  - 其余 → 用户 token
 * 401 时：用户端自动重新登录并重试一次（解决冷启动登录竞态）；管理端/派送端清理过期 token
 */
function request(path, method, data, retried) {
  const isAdmin = path.indexOf('/api/admin') === 0;
  const isStaff = path.indexOf('/api/staff') === 0;
  const token = wx.getStorageSync(isAdmin ? 'adminToken' : (isStaff ? 'staffToken' : 'token')) || '';
  return new Promise((resolve, reject) => {
    wx.request({
      url: getApp().globalData.baseUrl + path,
      method: method || 'GET',
      data: data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        const body = res.data;
        if (body && body.code === 0) {
          resolve(body.data);
          return;
        }
        if (body && body.code === 401) {
          if (isAdmin) {
            wx.removeStorageSync('adminToken');
            wx.showToast({ title: '店长登录已过期，请重新登录', icon: 'none' });
            reject(body);
            return;
          }
          if (isStaff) {
            wx.removeStorageSync('staffToken');
            wx.showToast({ title: '派送员登录已过期，请重新登录', icon: 'none' });
            reject(body);
            return;
          }
          // 用户端：token 失效或未登录 → 清理并回到登录页
          wx.removeStorageSync('token');
          wx.removeStorageSync('role');
          wx.removeStorageSync('nickname');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(body);
          return;
        }
        if (body && body.code === 403) {
          wx.showToast({ title: body.msg || '无权限', icon: 'none' });
          reject(body);
          return;
        }
        wx.showToast({ title: (body && body.msg) || '请求失败', icon: 'none' });
        reject(body);
      },
      fail() {
        wx.showToast({ title: '网络异常，请检查后端服务是否启动', icon: 'none' });
        reject(new Error('network'));
      }
    });
  });
}

module.exports = {
  get: (path, data) => request(path, 'GET', data),
  post: (path, data) => request(path, 'POST', data),
  put: (path, data) => request(path, 'PUT', data),
  del: (path, data) => request(path, 'DELETE', data)
};
