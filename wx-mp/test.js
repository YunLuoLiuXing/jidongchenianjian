/**
 * 核心算法验证脚本
 * 用 Node 直接运行：node test.js
 * 验证结果需与 Android Kotlin 版一致
 */

const { calculate } = require('./utils/inspection');
const { VehicleTypes } = require('./utils/vehicleType');
const { fmtCN } = require('./utils/date');

function findTypeByKey(k) {
  return VehicleTypes[k] || null;
}

let pass = 0, fail = 0;

function check(name, actual, expected) {
  const ok = actual === expected;
  if (ok) { pass++; console.log(`  ✓ ${name}`); }
  else {
    fail++;
    console.log(`  ✗ ${name}`);
    console.log(`     期望: ${expected}`);
    console.log(`     实际: ${actual}`);
  }
}

function run(title, query, today, asserts) {
  console.log(`\n【${title}】`);
  const r = calculate(query, today);
  asserts(r);
  return r;
}

// ========== 场景1：2015-09-02 注册，今天 2026-10-30 ==========
run('2015-09-02 注册 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2015, 8, 2)
}, new Date(2026, 9, 30), (r) => {
  check('状态为逾期', r.status, 'OVERDUE');
  check('逾期天数 30 天', r.overdueNode && Math.floor((new Date(2026,9,30) - r.overdueNode.deadline) / 86400000), 30);
  check('逾期节点是第11年', r.overdueNode && r.overdueNode.yearIndex, 11);
  check('第11年截止 2026-09-30', r.overdueNode && fmtCN(r.overdueNode.deadline), '2026年9月30日');
  check('第11年可提前 2026-07-01', r.overdueNode && fmtCN(r.overdueNode.earliest), '2026年7月1日');
  check('含逾期后果提示', r.notes.some(n => n.includes('罚款并记分')), true);
});

// ========== 场景2：2020-03-15 注册 ==========
run('2020-03-15 注册 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2020, 2, 15)
}, new Date(2026, 9, 30), (r) => {
  check('状态为逾期', r.status, 'OVERDUE');
  check('第6年截止 2026-03-31', r.overdueNode && fmtCN(r.overdueNode.deadline), '2026年3月31日');
  check('逾期 213 天', r.overdueNode && Math.floor((new Date(2026,9,30) - r.overdueNode.deadline) / 86400000), 213);
});

// ========== 场景3：2015-12-05 注册，可提前办理 ==========
run('2015-12-05 注册 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2015, 11, 5)
}, new Date(2026, 9, 30), (r) => {
  check('状态为可办理', r.status, 'CURRENT');
  check('有提前办理提示', r.notes.some(n => n.includes('提前办理期')), true);
  check('窗口截止 2026-12-31', r.currentNode && fmtCN(r.currentNode.deadline), '2026年12月31日');
  check('窗口起始 2026-10-01', r.currentNode && fmtCN(r.currentNode.earliest), '2026年10月1日');
});

// ========== 场景4：2015-10-05 注册，即刻年检 ==========
run('2015-10-05 注册 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2015, 9, 5)
}, new Date(2026, 9, 30), (r) => {
  check('有即刻年检剩余天数', r.urgentDays != null, true);
  check('剩余 1 天', r.urgentDays, 1);
  check('截止 2026-10-31', r.currentNode && fmtCN(r.currentNode.deadline), '2026年10月31日');
});

// ========== 场景5：勾选已年检 ==========
run('2016-09-15 注册 + 勾选已年检 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2016, 8, 15),
  currentPeriodDone: true
}, new Date(2026, 9, 30), (r) => {
  check('不显示逾期', r.status === 'OVERDUE', false);
  check('无逾期节点', r.overdueNode == null, true);
});

run('2016-09-15 注册 + 不勾选 / 2026-10-30', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2016, 8, 15)
}, new Date(2026, 9, 30), (r) => {
  check('显示逾期', r.status, 'OVERDUE');
  check('逾期节点是第10年', r.overdueNode && r.overdueNode.yearIndex, 10);
});

// ========== 场景6：教练车报废 ==========
run('教练车 2015-06-01 注册（报废10年）/ 2026-10-30', {
  type: findTypeByKey('COACH'),
  registerDate: new Date(2015, 5, 1)
}, new Date(2026, 9, 30), (r) => {
  const last = r.nodes[r.nodes.length - 1];
  check('最后一期为 SCRAP', last.type, 'SCRAP');
  check('最后一期为第10年', last.yearIndex, 10);
  check('时间轴只有10期', r.nodes.length, 10);
  check('状态为已报废', r.scrapped, true);
  check('状态文案含报废', r.statusText.includes('报废'), true);
  check('有报废流程', r.scrapDetail.length > 0, true);
  check('无逾期后果提示', r.notes.some(n => n.includes('罚款并记分')), false);
});

// ========== 场景7：营运载客报废去重 ==========
run('营运载客 2015-06-01 注册（报废10年）/ 2026-10-30', {
  type: findTypeByKey('OPERATING_PASSENGER'),
  registerDate: new Date(2015, 5, 1)
}, new Date(2026, 9, 30), (r) => {
  const scrapNodes = r.nodes.filter(n => n.type === 'SCRAP');
  check('只有 1 个报废节点', scrapNodes.length, 1);
  check('报废节点是第10年', scrapNodes[0] && scrapNodes[0].yearIndex, 10);
});

// ========== 场景8：临近报废（12个月内） ==========
run('教练车 2016-11-01 注册 / 2026-10-30（差1个月报废）', {
  type: findTypeByKey('COACH'),
  registerDate: new Date(2016, 10, 1)
}, new Date(2026, 9, 30), (r) => {
  check('临近报废', r.scrapApproaching, true);
  check('未到报废', r.scrapped, false);
  check('剩余月数 1', r.scrapMonthsLeft, 1);
  check('有报废流程', r.scrapDetail.length > 0, true);
  check('无免检流程', r.exemptDetail.length, 0);
});

// ========== 场景9：非营运车时间轴滚动延伸 ==========
run('2020-01-01 注册 / 2026 年查询', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2020, 0, 1)
}, new Date(2026, 9, 30), (r) => {
  check('时间轴 20 年', r.nodes[r.nodes.length - 1].yearIndex, 20);
});

run('2020-01-01 注册 / 2045 年查询（应延伸）', {
  type: findTypeByKey('SMALL_PASSENGER'),
  registerDate: new Date(2020, 0, 1)
}, new Date(2045, 5, 1), (r) => {
  check('时间轴延伸至 40 年', r.nodes[r.nodes.length - 1].yearIndex, 40);
});

// ========== 场景10：摩托车 ==========
run('摩托车 2015-06-01 注册（报废13年）/ 2026-10-30', {
  type: findTypeByKey('MOTORCYCLE'),
  registerDate: new Date(2015, 5, 1)
}, new Date(2026, 9, 30), (r) => {
  const last = r.nodes[r.nodes.length - 1];
  check('最后是第13年', last.yearIndex, 13);
  check('最后是 SCRAP', last.type, 'SCRAP');
});

console.log(`\n================================`);
console.log(`通过 ${pass} 项，失败 ${fail} 项`);
console.log(`================================`);
process.exit(fail > 0 ? 1 : 0);
