/**
 * 日期工具（JS 版）
 * 全部使用 Date 对象，与 Kotlin 的 LocalDate/YearMonth 语义对齐
 */

/** 某年某月的天数（month 为 1-12） */
function daysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

/** 构造本地 Date（避免时区偏移） */
function mkDate(year, month, day) {
  const d = new Date(year, month - 1, day);
  d.setHours(0, 0, 0, 0);
  return d;
}

/** 当月最后一天 */
function endOfMonth(date) {
  const y = date.getFullYear();
  const m = date.getMonth() + 1;
  return mkDate(y, m, daysInMonth(y, m));
}

/** 当月 1 号 */
function startOfMonth(date) {
  return mkDate(date.getFullYear(), date.getMonth() + 1, 1);
}

/** 加若干年（保持月/日，月末自动收敛） */
function addYears(date, n) {
  const y = date.getFullYear() + n;
  const m = date.getMonth() + 1;
  const d = Math.min(date.getDate(), daysInMonth(y, m));
  return mkDate(y, m, d);
}

/** 加若干月（保持日，月末自动收敛） */
function addMonths(date, n) {
  const total = date.getFullYear() * 12 + (date.getMonth()) + n;
  const y = Math.floor(total / 12);
  const m = (total % 12) + 1;
  const d = Math.min(date.getDate(), daysInMonth(y, m));
  return mkDate(y, m, d);
}

/** b - a 的天数差（正数表示 b 在 a 之后） */
function daysBetween(a, b) {
  const ms = mkDate(b.getFullYear(), b.getMonth() + 1, b.getDate()).getTime()
    - mkDate(a.getFullYear(), a.getMonth() + 1, a.getDate()).getTime();
  return Math.round(ms / 86400000);
}

/** b - a 的月数差（按年月计算，忽略日） */
function monthsBetween(a, b) {
  return (b.getFullYear() - a.getFullYear()) * 12 + (b.getMonth() - a.getMonth());
}

/** 格式化为「2026年3月31日」 */
function fmtCN(date) {
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
}

/** 格式化为「2026-03-31」 */
function fmtISO(date) {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

/** a 是否早于 b */
function isBefore(a, b) {
  return a.getTime() < b.getTime();
}

/** a 是否不早于 b（即 a >= b） */
function notBefore(a, b) {
  return a.getTime() >= b.getTime();
}

/** 解析 "2020-03" 或 "2020-03-15"，失败返回 null */
function parseDate(str) {
  if (!str) return null;
  const t = String(str).trim().replace(/\//g, '-').replace(/\./g, '-');
  let m = t.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  if (m) {
    const y = +m[1], mo = +m[2], d = +m[3];
    if (mo < 1 || mo > 12 || d < 1 || d > daysInMonth(y, mo)) return null;
    return mkDate(y, mo, d);
  }
  m = t.match(/^(\d{4})-(\d{1,2})$/);
  if (m) {
    const y = +m[1], mo = +m[2];
    if (mo < 1 || mo > 12) return null;
    return mkDate(y, mo, 1);
  }
  return null;
}

/** 节点类型 */
const NodeType = {
  INSPECTION: 'INSPECTION',
  LABEL_RENEWAL: 'LABEL_RENEWAL',
  SCRAP: 'SCRAP'
};

/** 节点状态 */
const NodeState = {
  DONE: 'DONE',
  CURRENT: 'CURRENT',
  NEAR: 'NEAR',
  FUTURE: 'FUTURE',
  OVERDUE: 'OVERDUE'
};

module.exports = {
  daysInMonth, mkDate, endOfMonth, startOfMonth,
  addYears, addMonths, daysBetween, monthsBetween,
  fmtCN, fmtISO, isBefore, notBefore, parseDate,
  NodeType, NodeState
};
