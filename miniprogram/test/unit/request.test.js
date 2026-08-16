/**
 * utils/request.js 单元测试（mock wx.request / 存储 / 全局 app）
 */
describe('request 封装', () => {
  let req;
  let wxMock;
  let lastRequest;

  const baseUrl = 'http://test.local:8080';

  beforeEach(() => {
    lastRequest = null;
    wxMock = {
      storage: { token: 'U_TOKEN', adminToken: 'A_TOKEN', staffToken: 'S_TOKEN', role: 'USER', nickname: 'n' },
      removed: [],
      relaunched: null,
      toasts: [],
      getStorageSync: (k) => wxMock.storage[k] || '',
      removeStorageSync: (k) => { wxMock.removed.push(k); delete wxMock.storage[k]; },
      setStorageSync: (k, v) => { wxMock.storage[k] = v; },
      reLaunch: (o) => { wxMock.relaunched = o.url; },
      showToast: (o) => wxMock.toasts.push(o),
      request: (opts) => { lastRequest = opts; }
    };
    global.wx = wxMock;
    global.getApp = () => ({ globalData: { baseUrl } });
    jest.resetModules();
    req = require('../../utils/request');
  });

  test('用户接口携带用户 token', () => {
    const p = req.get('/api/user/me');
    expect(lastRequest.url).toBe(baseUrl + '/api/user/me');
    expect(lastRequest.header.Authorization).toBe('Bearer U_TOKEN');
  });

  test('管理接口携带店长 token', () => {
    req.get('/api/admin/stats/overview');
    expect(lastRequest.header.Authorization).toBe('Bearer A_TOKEN');
  });

  test('派送接口携带派送员 token', () => {
    req.get('/api/staff/tasks');
    expect(lastRequest.header.Authorization).toBe('Bearer S_TOKEN');
  });

  test('code=0 时 resolve data', async () => {
    const p = req.get('/api/user/me');
    lastRequest.success({ data: { code: 0, msg: 'ok', data: { id: 1 } } });
    await expect(p).resolves.toEqual({ id: 1 });
  });

  test('401 用户端：清理 token 并回登录页', () => {
    const p = req.get('/api/user/me');
    lastRequest.success({ data: { code: 401, msg: '请先登录' } });
    return p.catch((e) => {
      expect(e.code).toBe(401);
      expect(wxMock.removed).toContain('token');
      expect(wxMock.relaunched).toBe('/pages/login/login');
    });
  });

  test('401 管理端：清理店长 token，不回登录页', () => {
    const p = req.get('/api/admin/stats/overview');
    lastRequest.success({ data: { code: 401, msg: '过期' } });
    return p.catch(() => {
      expect(wxMock.removed).toContain('adminToken');
      expect(wxMock.relaunched).toBeNull();
      expect(wxMock.toasts.some((t) => t.title.includes('店长'))).toBe(true);
    });
  });

  test('401 派送端：清理派送员 token', () => {
    const p = req.get('/api/staff/tasks');
    lastRequest.success({ data: { code: 401, msg: '过期' } });
    return p.catch(() => {
      expect(wxMock.removed).toContain('staffToken');
      expect(wxMock.relaunched).toBeNull();
    });
  });

  test('403 提示无权限', () => {
    const p = req.get('/api/admin/stats/overview');
    lastRequest.success({ data: { code: 403, msg: '无管理权限' } });
    return p.catch(() => {
      expect(wxMock.toasts.some((t) => t.title === '无管理权限')).toBe(true);
    });
  });

  test('业务错误 toast 展示 msg', () => {
    const p = req.post('/api/coffee/reserve', {});
    lastRequest.success({ data: { code: 1, msg: '余额不足，请先充值' } });
    return p.catch(() => {
      expect(wxMock.toasts.some((t) => t.title === '余额不足，请先充值')).toBe(true);
    });
  });

  test('网络异常 toast', () => {
    const p = req.get('/api/user/me');
    lastRequest.fail();
    return p.catch((e) => {
      expect(e.message).toBe('network');
      expect(wxMock.toasts.some((t) => t.title.includes('网络异常'))).toBe(true);
    });
  });

  test('POST/PUT/DELETE 方法与数据', () => {
    req.post('/api/drink/order', { items: [1] });
    expect(lastRequest.method).toBe('POST');
    expect(lastRequest.data).toEqual({ items: [1] });
    req.put('/api/user/me', { nickname: 'x' });
    expect(lastRequest.method).toBe('PUT');
    req.del('/api/admin/catalog/drink-products/1');
    expect(lastRequest.method).toBe('DELETE');
  });
});
