/**
 * 统一请求封装：自动携带 token，/api/admin 前缀自动使用店长 token
 */
function request(path, method, data) {
  const isAdmin = path.indexOf('/api/admin') === 0;
  const token = wx.getStorageSync(isAdmin ? 'adminToken' : 'token') || '';
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
        } else if (body && body.code === 401) {
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          reject(body);
        } else {
          wx.showToast({ title: (body && body.msg) || '请求失败', icon: 'none' });
          reject(body);
        }
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
