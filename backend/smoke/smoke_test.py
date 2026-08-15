import json, urllib.request, urllib.error

BASE = 'http://127.0.0.1:8080'

def req(path, method='GET', data=None, token=None, raw=False):
    r = urllib.request.Request(BASE + path, method=method)
    r.add_header('Content-Type', 'application/json')
    if token: r.add_header('Authorization', 'Bearer ' + token)
    body = json.dumps(data).encode() if data is not None else None
    try:
        with urllib.request.urlopen(r, body) as resp:
            txt = resp.read().decode()
            return txt if raw else json.loads(txt)
    except urllib.error.HTTPError as e:
        return {'http_error': e.code, 'body': e.read().decode()}

ok_count = 0
def check(name, cond, extra=''):
    global ok_count
    if cond: ok_count += 1; print(f'PASS  {name} {extra}')
    else: print(f'FAIL  {name} {extra}')

# 1. 健康检查
check('health', req('/api/public/health')['code'] == 0)

# 2. 微信登录（模拟）
login = req('/api/auth/login', 'POST', {'code': 'mock_smoke_' + str(int(__import__('time').time())), 'nickname': '测试用户'})
token = login.get('data', {}).get('token')
check('wx login', token is not None, login.get('data', {}).get('role', ''))

# 3. 个人中心
me = req('/api/user/me', token=token)['data']
check('user me', me['nickname'] == '测试用户', f"balance={me['balance']} level={me['levelName']}")

# 4. 公开设置 + 咖啡液产品
settings = req('/api/public/settings')['data']
check('settings slots', len(json.loads(settings['delivery_slots'])) == 7)
products = req('/api/coffee/products', token=token)['data']
check('coffee products', len(products) == 3, str([p['name'] for p in products]))
pkgs = req('/api/coffee/packages', token=token)['data']
check('packages', len(pkgs) == 2)
pkg30 = [p for p in pkgs if p['bottleCount'] == 30][0]

# 5. 充值 200
after = req('/api/user/recharge', 'POST', {'amount': 200}, token)['data']
check('recharge', float(after) == 200.0, str(after))

# 6. 余额购买月卡 30瓶 ¥360 → 余额不足，应报错
r = req('/api/coffee/package/buy', 'POST', {'packageId': pkg30['id'], 'payType': 'BALANCE'}, token)
check('package buy insufficient', r.get('code') == 1 and '余额不足' in r.get('msg', ''), str(r.get('msg')))

# 7. 模拟支付购买月卡
up = req('/api/coffee/package/buy', 'POST', {'packageId': pkg30['id'], 'payType': 'WX_MOCK'}, token)['data']
check('package buy ok', up['totalQuota'] == 30 and up['usedQuota'] == 0, f"remain={up['totalQuota'] - up['usedQuota']}")

# 8. 预订 5 瓶全部走额度
slot = json.loads(settings['delivery_slots'])[0]
res = req('/api/coffee/reserve', 'POST', {
    'productId': products[0]['id'], 'quantity': 5, 'usePackage': True, 'payType': 'WX_MOCK',
    'timeSlot': slot, 'address': '3栋2单元501', 'contactName': '测试用户', 'contactPhone': '13800000000'
}, token)
r = res['data'] if res.get('code') == 0 else res
check('reserve 5 quota', res.get('code') == 0 and r['packageUsed'] == 5 and r['amount'] == 0,
      f"packageUsed={r.get('packageUsed')} amount={r.get('amount')}")

# 9. 预订 28 瓶：剩25额度 + 3瓶按次支付 (15*3=45)
res2 = req('/api/coffee/reserve', 'POST', {
    'productId': products[0]['id'], 'quantity': 28, 'usePackage': True, 'payType': 'BALANCE',
    'timeSlot': slot, 'address': '3栋2单元501', 'contactName': '测试用户', 'contactPhone': '13800000000'
}, token)
r2 = res2['data'] if res2.get('code') == 0 else res2
check('reserve 28 mixed', res2.get('code') == 0 and r2['packageUsed'] == 25 and r2['payQuantity'] == 3 and float(r2['amount']) == 45.0,
      f"quota={r2.get('packageUsed')} pay={r2.get('payQuantity')} amount={r2.get('amount')}")

# 10. 月度详情
monthly = req('/api/coffee/monthly', token=token)['data']
check('monthly summary', float(monthly['spend']) == 360.0 + 45.0 and monthly['quotaRemain'] == 0,
      f"spend={monthly['spend']} remain={monthly['quotaRemain']} total={monthly['quotaTotal']}")

# 11. 预订记录 + 取消第2单（退款45 + 恢复额度25）
records = req('/api/coffee/reservations', token=token)['data']['records']
r2id = records[0]['id']
cancel = req(f'/api/coffee/reservations/{r2id}/cancel', 'POST', {}, token)
check('cancel reserve', cancel['code'] == 0 and cancel['data']['status'] == 'CANCELLED')
me2 = req('/api/user/me', token=token)['data']
check('refund to balance', float(me2['balance']) == 200.0, f"balance={me2['balance']}")
monthly2 = req('/api/coffee/monthly', token=token)['data']
check('quota restored', monthly2['quotaRemain'] == 25 and float(monthly2['spend']) == 360.0,
      f"remain={monthly2['quotaRemain']} spend={monthly2['spend']}")

# 12. 领券 + 核销下单
templates = req('/api/coupon/templates', token=token)['data']
claim = req(f"/api/coupon/{templates[0]['id']}/claim", 'POST', {}, token)['data']
check('claim coupon', claim['status'] == 'UNUSED', claim['name'])
menu = req('/api/drink/menu', token=token)['data']
p1 = menu[0]['products'][0]; p2 = menu[0]['products'][1]
order = req('/api/drink/order', 'POST', {
    'items': [{'productId': p1['id'], 'quantity': 2}, {'productId': p2['id'], 'quantity': 1}],
    'payType': 'WX_MOCK', 'couponId': claim['id']
}, token)
o = order['data'] if order.get('code') == 0 else order
total = float(p1['price']) * 2 + float(p2['price'])
check('drink order with coupon', order.get('code') == 0 and float(o['payAmount']) == total - 5.0,
      f"total={o.get('totalAmount')} pay={o.get('payAmount')} pickup={o.get('pickupCode')}")

# 13. 积分到账
me3 = req('/api/user/me', token=token)['data']
check('points earned', me3['points'] > 0, f"points={me3['points']}")

# 14. 店长登录
admin = req('/api/auth/admin-login', 'POST', {'username': 'admin', 'password': 'admin123'})
atk = admin.get('data', {}).get('token')
check('admin login', atk is not None)

# 15. 看板/趋势/畅销
ov = req('/api/admin/stats/overview', token=atk)['data']
check('overview', float(ov['todaySales']) >= 360.0, str(ov))
trend = req('/api/admin/stats/trend?days=7', token=atk)['data']
check('trend', len(trend) == 7, f"days={len(trend)}")
top = req('/api/admin/stats/top', token=atk)['data']
check('top products', len(top['drinks']) > 0, f"drinks={top['drinks']} coffees={top['coffees']}")

# 16. 台账
ledger = req('/api/admin/stats/ledger?startDate=2026-08-01&endDate=2026-08-31&type=ALL', token=atk)['data']
check('ledger', ledger['summary']['count'] >= 4, f"count={ledger['summary']['count']} income={ledger['summary']['income']}")
csv = req('/api/admin/stats/ledger/csv?type=ALL', token=atk, raw=True)
check('ledger csv', csv.startswith('\ufeff时间,单号') and '咖啡液' in csv)

# 17. 派送管理状态流转
resv = req('/api/admin/ops/reservations?status=ALL', token=atk)['data']['records']
target = [r for r in resv if r['status'] == 'PENDING'][0]
check('reservation list', 'nickname' in target and 'dateLabel' not in target, f"nick={target['nickname']}")
s1 = req(f"/api/admin/ops/reservations/{target['id']}/status", 'PUT', {'status': 'CONFIRMED'}, atk)
check('status CONFIRMED', s1['code'] == 0)
s2 = req(f"/api/admin/ops/reservations/{target['id']}/status", 'PUT', {'status': 'DELIVERING'}, atk)
check('status DELIVERING', s2['code'] == 0)
s3 = req(f"/api/admin/ops/reservations/{target['id']}/status", 'PUT', {'status': 'DELIVERED'}, atk)
check('status DELIVERED', s3['code'] == 0)

# 18. 饮品订单状态流转
ordrs = req('/api/admin/ops/orders?status=ALL', token=atk)['data']['records']
oid = ordrs[0]['id']
check('admin orders', len(ordrs) >= 1)
req(f'/api/admin/ops/orders/{oid}/status', 'PUT', {'status': 'MAKING'}, atk)
req(f'/api/admin/ops/orders/{oid}/status', 'PUT', {'status': 'READY'}, atk)
f1 = req(f'/api/admin/ops/orders/{oid}/status', 'PUT', {'status': 'FINISHED'}, atk)
check('order FINISHED', f1['code'] == 0)

# 19. 会员管理
members = req('/api/admin/members?keyword=' + __import__('urllib').parse.quote('测试'), token=atk)['data']
check('member search', members['total'] >= 1 and members['records'][0]['nickname'] == '测试用户', f"total={members['total']}")
uid = members['records'][0]['id']
adj = req(f'/api/admin/members/{uid}/points', 'POST', {'points': -50, 'remark': '测试扣减'}, atk)
check('adjust points', adj['code'] == 0)
me4 = req('/api/user/me', token=token)['data']
check('points adjusted', me4['points'] == me3['points'] - 50, f"points={me4['points']}")

# 20. 设置读写
setget = req('/api/admin/ops/settings', token=atk)['data']
check('settings get', 'shop_name' in setget and len(json.loads(setget['delivery_slots'])) == 7)
setput = req('/api/admin/ops/settings', 'PUT', {'shop_name': 'GC Coffee 测试店'}, atk)
check('settings put', setput['code'] == 0)
check('settings readback', req('/api/admin/ops/settings', token=atk)['data']['shop_name'] == 'GC Coffee 测试店')

# 21. 越权检查：用户 token 访问管理接口应 403
forbidden = req('/api/admin/stats/overview', token=token)
check('user blocked from admin', forbidden.get('code') == 403, str(forbidden.get('code')))

# 22. 非法预订：当日派送应被拒
bad = req('/api/coffee/reserve', 'POST', {
    'productId': products[0]['id'], 'quantity': 1, 'usePackage': False,
    'timeSlot': slot, 'address': 'x', 'contactName': 'x', 'contactPhone': '13800000000',
    'deliveryDate': __import__('datetime').date.today().isoformat()
}, token)
check('same-day delivery rejected', bad.get('code') == 1 and '次日' in bad.get('msg', ''), str(bad.get('msg')))

print(f'\n===== {ok_count}/37 checks passed =====')
