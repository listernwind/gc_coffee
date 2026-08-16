#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
前后端接口契约联调测试
用法：后端已启动（默认 http://127.0.0.1:8080）后执行：
    python3 backend/smoke/api_contract_test.py
校验内容：逐页模拟小程序前端实际发起的请求（路径/参数与 pages/*.js 一致），
并校验响应中前端 WXML/JS 所绑定字段的存在性与类型，发现不匹配即 FAIL。
"""
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = 'http://127.0.0.1:8080'
if len(sys.argv) > 1:
    BASE = sys.argv[1]

ok_count = 0
total = [0]


def req(path, method='GET', data=None, token=None, raw=False, params=None):
    url = BASE + path
    if params:
        url += '?' + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
    r = urllib.request.Request(url, method=method)
    r.add_header('Content-Type', 'application/json')
    if token:
        r.add_header('Authorization', 'Bearer ' + token)
    body = json.dumps(data).encode() if data is not None else None
    try:
        with urllib.request.urlopen(r, body) as resp:
            txt = resp.read().decode()
            return txt if raw else json.loads(txt)
    except urllib.error.HTTPError as e:
        return {'http_error': e.code, 'body': e.read().decode()}


def check(name, cond, extra=''):
    global ok_count
    total[0] += 1
    if cond:
        ok_count += 1
        print(f'PASS  {name}')
    else:
        print(f'FAIL  {name}  {extra}')


def has(d, *path, types=None):
    """校验 d[path...] 存在且类型符合（types 传 (str,int) 表示接受任一）"""
    cur = d
    for k in path:
        if not isinstance(cur, dict) or k not in cur:
            return False
        cur = cur[k]
    if types is not None:
        if not isinstance(cur, types):
            return False
    return True


def login_wx(code, nick='联调用户'):
    return req('/api/auth/login', 'POST', {'code': code, 'nickname': nick})['data']


# ==================== 登录 ====================
print('== 登录 ==')
demo = login_wx('mock_demo')
check('演示用户登录', has(demo, 'token', types=str) and demo['role'] == 'USER')
utk = demo['token']
admin = req('/api/auth/admin-login', 'POST', {'username': 'admin', 'password': 'admin123'})['data']
check('店长登录', has(admin, 'token', types=str) and admin['role'] == 'ADMIN')
atk = admin['token']
staff = req('/api/auth/staff-login', 'POST', {'username': 'staff1', 'password': 'staff123'})
if staff.get('code') != 0:
    # 预置账号可能被改过：用店长权限重置 staff1 密码（联调环境自愈）
    sl = req('/api/admin/staff', token=atk)['data']
    target = next((x for x in sl if x['username'] == 'staff1'), sl[0])
    req('/api/admin/staff', 'POST', {'id': target['id'], 'username': target['username'],
                                    'password': 'staff123', 'nickname': target.get('nickname', ''),
                                    'phone': target.get('phone', ''), 'status': 1}, atk)
    staff = req('/api/auth/staff-login', 'POST', {'username': target['username'], 'password': 'staff123'})
staff = staff.get('data')
check('派送员登录', has(staff, 'token', types=str) and staff['role'] == 'STAFF' and staff['nickname'] == '配送员小王')
stk = staff['token']
reg = req('/api/auth/register', 'POST', {'username': 'con' + str(int(time.time()))[-6:], 'password': '123456', 'nickname': '契约用户', 'phone': '13512345678'})['data']
check('注册并登录', has(reg, 'token', types=str) and reg['role'] == 'USER')

# ==================== 登录页/首页 ====================
print('== 首页（home）==')
home = req('/api/public/home')['data']
check('公开首页数据', has(home, 'settings', types=dict) and has(home, 'posters', types=list))
s = home['settings']
check('首页公告字段', isinstance(s.get('shop_notice'), str) and isinstance(s.get('shop_name'), str))
hc = req('/api/coffee/home-card', token=utk)['data']
check('咖啡液首页卡', has(hc, 'hasPackage', types=bool) and has(hc, 'remain', types=int) and has(hc, 'packageName', types=str))

# ==================== 点单（menu/checkout/orders） ====================
print('== 点单 ==')
menu = req('/api/drink/menu', token=utk)['data']
check('饮品菜单', isinstance(menu, list) and len(menu) > 0)
cat = menu[0]
ok_menu = has(cat, 'id', types=int) and has(cat, 'name', types=str) and isinstance(cat.get('products'), list)
if cat.get('products'):
    p0 = cat['products'][0]
    ok_menu = ok_menu and all(k in p0 for k in ('id', 'name', 'descText', 'price', 'tags', 'stock'))
check('菜单商品字段', ok_menu, str(p0 if cat.get('products') else ''))

total_amt = round(sum(float(p['price']) for c in menu for p in c['products'][:1]), 2)
usable = req('/api/coupon/usable', params={'amount': total_amt}, token=utk)['data']
check('结算可用券', isinstance(usable, list))
order = req('/api/drink/order', 'POST', {
    'items': [{'productId': menu[0]['products'][0]['id'], 'quantity': 1}],
    'payType': 'WX_MOCK'
}, utk)['data']
check('下单', has(order, 'id', types=int) and has(order, 'orderNo', types=str) and has(order, 'pickupCode', types=str) and order['status'] == 'PAID')

my_orders = req('/api/drink/orders', params={'page': 1, 'size': 10, 'status': 'ALL'}, token=utk)['data']
check('订单列表分页结构', has(my_orders, 'records', types=list) and has(my_orders, 'total', types=int))
detail = req(f"/api/drink/orders/{order['id']}", token=utk)['data']
check('订单详情', has(detail, 'order', types=dict) and has(detail, 'items', types=list) and has(detail, 'nickname', types=str))

# ==================== 咖啡液预订（reserve/records/monthly） ====================
print('== 咖啡液预订 ==')
prods = req('/api/coffee/products', token=utk)['data']
check('咖啡液产品', isinstance(prods, list) and all(k in prods[0] for k in ('id', 'name', 'spec', 'price')))
slots_raw = req('/api/public/settings')['data'].get('delivery_slots', '[]')
slots = json.loads(slots_raw)
check('派送时段为 JSON 数组', isinstance(slots, list) and len(slots) > 0)
days = req('/api/coffee/delivery-days', params={'n': 7}, token=utk)['data']
check('可派送日期 7 天', isinstance(days, list) and len(days) == 7 and all(isinstance(d, str) for d in days))
pkg = req('/api/coffee/package/current', token=utk)['data']
check('套餐额度', has(pkg, 'totalQuota', types=int) and has(pkg, 'usedQuota', types=int) and has(pkg, 'packageName', types=str))
r = req('/api/coffee/reserve', 'POST', {
    'productId': prods[0]['id'], 'quantity': 3, 'usePackage': True, 'payType': 'WX_MOCK',
    'deliveryDate': days[0], 'timeSlot': slots[0],
    'address': '3栋2单元501', 'contactName': '联调用户', 'contactPhone': '13800000000'
}, utk)['data']
check('预订（额度抵扣）', has(r, 'id', types=int) and r['packageUsed'] == 3 and r['amount'] == 0 and r['status'] == 'PENDING')
recs = req('/api/coffee/reservations', params={'page': 1, 'size': 10}, token=utk)['data']['records']
check('预订记录字段', recs and all(k in recs[0] for k in ('orderNo', 'productName', 'quantity', 'packageUsed', 'payQuantity', 'amount', 'status', 'deliveryDate', 'timeSlot', 'address', 'createdAt')))
mo = req('/api/coffee/monthly', params={'month': time.strftime('%Y-%m')}, token=utk)['data']
check('月度统计', has(mo, 'month', types=str) and has(mo, 'spend', types=(int, float)) and has(mo, 'quotaTotal', types=int) and has(mo, 'quotaRemain', types=int) and has(mo, 'packages', types=list) and has(mo, 'records', types=list))

# ==================== 会员（member/recharge/records/coupons） ====================
print('== 会员 ==')
me = req('/api/user/me', token=utk)['data']
check('用户资料', all(k in me for k in ('id', 'nickname', 'balance', 'points', 'totalSpend', 'levelName', 'defaultAddress')))
bal = req('/api/user/recharge', 'POST', {'amount': 100}, utk)['data']
check('充值返回余额', isinstance(bal, (int, float)) and float(bal) >= 100)
br = req('/api/user/balance-records', params={'page': 1, 'size': 20}, token=utk)['data']
check('余额流水', has(br, 'records', types=list) and all(k in br['records'][0] for k in ('changeAmount', 'bizType', 'remark', 'createdAt')))
pr = req('/api/user/points-records', params={'page': 1, 'size': 20}, token=utk)['data']
check('积分流水', has(pr, 'records', types=list))
tpls = req('/api/coupon/templates', token=utk)['data']
check('优惠券模板', isinstance(tpls, list) and all(k in tpls[0] for k in ('id', 'name', 'type', 'value', 'minAmount', 'redeemPoints')))
claimable = [t for t in tpls if t.get('redeemPoints', 0) <= 0][0]
c = req(f"/api/coupon/{claimable['id']}/claim", 'POST', {}, utk)['data']
check('领取优惠券', has(c, 'id', types=int) and c['status'] == 'UNUSED' and has(c, 'expireAt', types=str))
mine = req('/api/coupon/mine', params={'status': 'UNUSED'}, token=utk)['data']
check('我的优惠券', isinstance(mine, list) and all(k in mine[0] for k in ('name', 'type', 'value', 'minAmount', 'expireAt', 'source')))

# ==================== 活动 ====================
print('== 活动 ==')
acts = req('/api/activity/list', params={'type': 'MONTHLY'}, token=utk)['data']
check('月度活动', isinstance(acts, list) and all(k in acts[0] for k in ('id', 'title', 'type', 'subtitle', 'content', 'startAt', 'endAt')))
detail_act = req('/api/activity/detail', params={'id': acts[0]['id']}, token=utk)['data']
check('活动详情', has(detail_act, 'id', types=int) and has(detail_act, 'title', types=str))
posters = req('/api/activity/posters', token=utk)['data']
check('海报列表', isinstance(posters, list) and all(k in posters[0] for k in ('id', 'title', 'image', 'linkType', 'linkId')))

# ==================== 管理端 ====================
print('== 管理端 ==')
ov = req('/api/admin/stats/overview', token=atk)['data']
check('看板指标', all(k in ov for k in ('todaySales', 'todayDrink', 'todayCoffee', 'todayRecharge', 'todayOrders', 'todayReservations', 'monthSales', 'memberCount', 'pendingDelivery', 'makingOrders', 'today')))
trend = req('/api/admin/stats/trend', params={'days': 7}, token=atk)['data']
check('趋势 7 天', isinstance(trend, list) and len(trend) == 7 and all('date' in t and 'sales' in t and 'orders' in t for t in trend))
top = req('/api/admin/stats/top', params={'n': 5}, token=atk)['data']
check('畅销榜', has(top, 'drinks', types=list) and has(top, 'coffees', types=list))
led = req('/api/admin/stats/ledger', params={'startDate': time.strftime('%Y-%m-01'), 'endDate': time.strftime('%Y-%m-%d'), 'type': 'ALL'}, token=atk)['data']
check('台账结构', has(led, 'rows', types=list) and has(led, 'summary', types=dict) and all(k in led['summary'] for k in ('income', 'count')))
if led['rows']:
    check('台账行字段', all(k in led['rows'][0] for k in ('time', 'orderNo', 'type', 'user', 'content', 'payType', 'amount', 'status')))
csv = req('/api/admin/stats/ledger/csv', params={'type': 'ALL'}, token=atk, raw=True)
check('台账 CSV（BOM+表头）', csv.startswith('\ufeff') and '时间,单号,类型,用户' in csv)

resv = req('/api/admin/ops/reservations', params={'page': 1, 'size': 20, 'status': 'ALL'}, token=atk)['data']['records']
check('派送管理（含派送员）', isinstance(resv, list) and all(k in resv[0] for k in ('nickname', 'staffName', 'address', 'contactPhone')))
ords = req('/api/admin/ops/orders', params={'page': 1, 'size': 20, 'status': 'ALL'}, token=atk)['data']['records']
check('饮品订单管理', ords and all(k in ords[0] for k in ('nickname', 'pickupCode', 'payAmount')))

cats = req('/api/admin/catalog/drink-categories', token=atk)['data']
dps = req('/api/admin/catalog/drink-products', token=atk)['data']
cps = req('/api/admin/catalog/coffee-products', token=atk)['data']
pkgs = req('/api/admin/catalog/coffee-packages', token=atk)['data']
check('商品目录接口', isinstance(cats, list) and all(k in dps[0] for k in ('id', 'name', 'price', 'active', 'stock', 'categoryId')) and all(k in cps[0] for k in ('id', 'name', 'price', 'spec')) and all(k in pkgs[0] for k in ('id', 'name', 'bottleCount', 'price')))

mkt_c = req('/api/admin/marketing/coupons', token=atk)['data']
mkt_a = req('/api/admin/marketing/activities', token=atk)['data']
mkt_p = req('/api/admin/marketing/posters', token=atk)['data']
check('营销管理接口', isinstance(mkt_c, list) and isinstance(mkt_a, list) and isinstance(mkt_p, list) and all(k in mkt_a[0] for k in ('title', 'type', 'startAt', 'endAt', 'active')))

mems = req('/api/admin/members', params={'page': 1, 'size': 15, 'keyword': '演示'}, token=atk)['data']
check('会员搜索', has(mems, 'records', types=list) and has(mems, 'total', types=int) and all(k in mems['records'][0] for k in ('nickname', 'phone', 'balance', 'points', 'totalSpend', 'status')))
sl = req('/api/admin/staff', token=atk)['data']
check('派送员列表', isinstance(sl, list) and all(k in sl[0] for k in ('id', 'username', 'nickname', 'phone', 'status')))
st_set = req('/api/admin/ops/settings', token=atk)['data']
check('系统设置', all(k in st_set for k in ('shop_name', 'community_name', 'delivery_slots', 'level_gold')))

# ==================== 派送员工作台 ====================
print('== 派送员 ==')
sme = req('/api/staff/me', token=stk)['data']
check('派送员资料', has(sme, 'name', types=str) and has(sme, 'phone', types=str))
tasks = req('/api/staff/tasks', params={'date': 'tomorrow'}, token=stk)['data']
check('派送任务', isinstance(tasks, list) and len(tasks) >= 1)
if tasks:
    t0 = tasks[0]
    check('任务字段', all(k in t0 for k in ('id', 'orderNo', 'productName', 'quantity', 'status', 'deliveryDate', 'timeSlot', 'address', 'contactName', 'contactPhone', 'staffName', 'customer')))
    pend = [t for t in tasks if t['status'] == 'PENDING'][0]
    cl = req(f"/api/staff/tasks/{pend['id']}/claim", 'PUT', {}, stk)
    check('认领订单', cl['code'] == 0 and cl['data']['status'] == 'CONFIRMED')
    st1 = req(f"/api/staff/tasks/{pend['id']}/status", 'PUT', {'status': 'DELIVERING'}, stk)
    st2 = req(f"/api/staff/tasks/{pend['id']}/status", 'PUT', {'status': 'DELIVERED'}, stk)
    check('派送状态流转', st1['code'] == 0 and st2['code'] == 0 and st2['data']['status'] == 'DELIVERED')
    staff_name_visible = any(x.get('staffName') == '配送员小王' for x in
                             req('/api/admin/ops/reservations', params={'page': 1, 'size': 20, 'status': 'DELIVERED'}, token=atk)['data']['records'])
    check('店长可见派送员', staff_name_visible)

# ==================== 越权 ====================
print('== 越权 ==')
check('用户访问管理接口 403', req('/api/admin/stats/overview', token=utk).get('code') == 403)
check('用户访问派送接口 403', req('/api/staff/tasks', token=utk).get('code') == 403)
check('派送员访问管理接口 403', req('/api/admin/stats/overview', token=stk).get('code') == 403)
check('未登录 401', req('/api/user/me').get('code') == 401)

print(f'\n===== {ok_count}/{total[0]} contract checks passed =====')
