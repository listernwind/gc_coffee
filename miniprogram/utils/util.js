const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

function money(v) {
  return Number(v || 0).toFixed(2);
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n;
}

function fmtDate(d) {
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

function fmtTime(d) {
  return fmtDate(d) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

/** "2025-08-17" -> "8月17日 周日" */
function dateLabel(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr.replace(/-/g, '/'));
  return (d.getMonth() + 1) + '月' + d.getDate() + '日 ' + WEEK[d.getDay()];
}

/** 明天的日期（次日派送起点） */
function tomorrow() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return fmtDate(d);
}

/** 饮品订单状态 */
const DRINK_STATUS = {
  PAID: '待制作', MAKING: '制作中', READY: '待取餐', FINISHED: '已完成', CANCELLED: '已取消'
};

/** 咖啡液预订状态 */
const RESERVE_STATUS = {
  PENDING: '待确认', CONFIRMED: '已确认', DELIVERING: '派送中', DELIVERED: '已送达', CANCELLED: '已取消'
};

/** 优惠券类型描述 */
function couponLabel(c) {
  if (c.type === 'DISCOUNT') {
    const rate = Math.round(Number(c.value) * 10);
    return rate + '折';
  }
  if (c.type === 'CASH') return '¥' + money(c.value);
  return '减¥' + money(c.value);
}

function couponDesc(c) {
  const label = couponLabel(c);
  const min = Number(c.minAmount || 0);
  return min > 0 ? '满' + money(min) + '可用 · ' + label : '无门槛 · ' + label;
}

/** 等级配色 */
function levelColor(name) {
  if (name.indexOf('黑金') >= 0) return '#3B2B20';
  if (name.indexOf('金卡') >= 0) return '#C08A4E';
  if (name.indexOf('银卡') >= 0) return '#8E9AA8';
  return '#B9AB99';
}

module.exports = {
  money, fmtDate, fmtTime, dateLabel, tomorrow,
  DRINK_STATUS, RESERVE_STATUS, couponLabel, couponDesc, levelColor
};
