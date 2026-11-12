/**
 * 年检规则引擎（全部按 2022 年 10 月新规口径）
 * 与 Android Kotlin 版 InspectionCalculator.kt 逐行对应
 *
 * 核心口径：
 * - 周期按注册「月」计算，截止日 = 该月最后一天
 *   例：2020-03-15 注册 → 第 6 年截止 2026-03-31（不是 03-15）
 * - 最早办理日 = 截止月往前 2 个月的首日
 *   例：2026-03 截止 → 最早 2026-01-01 起可办
 */

const { endOfMonth, addYears, addMonths, daysBetween: dayDiff, fmtCN } = require('./date');
const { VehicleTypes } = require('./vehicleType');

// ===== 节点类型 =====
const NodeType = {
  INSPECTION: 'INSPECTION',       // 上线检测
  LABEL_RENEWAL: 'LABEL_RENEWAL', // 免检期申领检验标志
  SCRAP: 'SCRAP'                  // 达到强制报废年限
};

// ===== 节点状态 =====
const NodeState = {
  DONE: 'DONE',           // 已完成（历史节点）
  CURRENT: 'CURRENT',     // 当前办理窗口内
  NEAR: 'NEAR',           // 临近（30 天内）
  OVERDUE: 'OVERDUE',     // 已逾期
  FUTURE: 'FUTURE'        // 未来
};

// ===== 常量 =====
const LABEL_YEARS = [2, 4, 8];       // 免检期申领标志的年份
const INSPECT_YEARS = [6, 10];       // 需要上线检测的年份
const FREE_LIMIT_YEAR = 10;          // 免检车型第 10 年后每年上线
const EARLY_MONTHS = 2;              // 提前办理月数
const NEAR_DAYS = 30;                // 临近天数阈值

/**
 * 计算时间轴要覆盖的年限
 * - 有强制报废年限的车型：取报废年限
 * - 无强制报废年限：按 20 年滚动延伸，至少覆盖到「当前年份 + 5 年」
 */
function resolveTimelineYears(type, regDate, today) {
  if (type.scrapYears != null) return type.scrapYears;
  const elapsed = today.getFullYear() - regDate.getFullYear() + 1;
  const needed = Math.max(elapsed + 5, 20);
  return Math.ceil(needed / 20) * 20;
}

/** 创建节点（deadline 为当月最后一天，earliest 提前 2 个月首日） */
function buildNode(yearIndex, type, deadline) {
  const earliest = addMonths(new Date(deadline.getFullYear(), deadline.getMonth(), 1), -EARLY_MONTHS);
  return {
    yearIndex,
    type,
    deadline,
    earliest,
    state: NodeState.FUTURE
  };
}

/** 按注册月 + N 年生成节点 */
function makeNode(yearIndex, type, regMonth) {
  const deadline = endOfMonth(addYears(regMonth, yearIndex));
  return buildNode(yearIndex, type, deadline);
}

/** 生成时间轴节点 */
function buildNodes(query, exempt, today) {
  const regDate = query.registerDate;
  const years = resolveTimelineYears(query.type, regDate, today);
  const nodes = [];

  // 免检车型（非营运小微型客车 / 摩托车）
  if (exempt) {
    for (let y = 1; y <= years; y++) {
      let node = null;
      if (LABEL_YEARS.indexOf(y) >= 0) {
        node = makeNode(y, NodeType.LABEL_RENEWAL, regDate);
      } else if (INSPECT_YEARS.indexOf(y) >= 0) {
        node = makeNode(y, NodeType.INSPECTION, regDate);
      } else if (y > FREE_LIMIT_YEAR) {
        node = makeNode(y, NodeType.INSPECTION, regDate);
      }
      if (node) nodes.push(node);
    }
    return applyScrap(nodes, query);
  }

  const t = query.type;

  // 营运载客汽车 / 面包车：5 年内每年 1 次，超 5 年每 6 个月 1 次
  if (t.name === 'OPERATING_PASSENGER' || t.name === 'VAN') {
    for (let y = 1; y <= years; y++) {
      const deadline = endOfMonth(addYears(regDate, y));
      nodes.push(buildNode(y, NodeType.INSPECTION, deadline));
      if (y > 5) {
        // 补一个上半年节点，形成 6 个月间隔
        const h1 = endOfMonth(addMonths(deadline, -6));
        nodes.push(buildNode(y, NodeType.INSPECTION, h1));
      }
    }
    return applyScrap(nodes, query);
  }

  // 载货汽车：10 年内每年 1 次，超 10 年每 6 个月 1 次
  if (t.name === 'OPERATING_TRUCK' || t.name === 'NON_OPERATING_TRUCK') {
    for (let y = 1; y <= years; y++) {
      const deadline = endOfMonth(addYears(regDate, y));
      nodes.push(buildNode(y, NodeType.INSPECTION, deadline));
      if (y > 10) {
        const h1 = endOfMonth(addMonths(deadline, -6));
        nodes.push(buildNode(y, NodeType.INSPECTION, h1));
      }
    }
    return applyScrap(nodes, query);
  }

  // 其他（挂车、专项作业车、教练车等）：每年 1 次
  for (let y = 1; y <= years; y++) {
    const deadline = endOfMonth(addYears(regDate, y));
    nodes.push(buildNode(y, NodeType.INSPECTION, deadline));
  }
  return applyScrap(nodes, query);
}

/**
 * 有强制报废年限的车型：把报废年限那一期替换为报废节点
 * 报废年限之后的节点不再生成（截断）
 */
function applyScrap(nodes, query) {
  const scrap = query.type.scrapYears;
  if (scrap == null) return nodes;

  const scrapDeadline = endOfMonth(addYears(query.registerDate, scrap));
  // 报废年之前的节点原样保留
  const kept = nodes.filter(n => n.yearIndex < scrap);
  // 报废当期只保留最后一条，并改造为报废节点
  const sameYear = nodes.filter(n => n.yearIndex === scrap);
  const last = sameYear.length ? sameYear[sameYear.length - 1]
    : buildNode(scrap, NodeType.INSPECTION, scrapDeadline);

  const scrapNode = Object.assign({}, last, {
    type: NodeType.SCRAP,
    deadline: scrapDeadline,
    earliest: addMonths(new Date(scrapDeadline.getFullYear(), scrapDeadline.getMonth(), 1), -EARLY_MONTHS)
  });

  return kept.concat([scrapNode]);
}

/** 状态判定：先判过期/临近/窗口内/未来，逾期在汇总阶段统一修正 */
function resolveState(node, today) {
  const daysToDeadline = dayDiff(today, node.deadline);
  if (daysToDeadline < 0) return NodeState.DONE;
  if (daysToDeadline >= 0 && daysToDeadline <= NEAR_DAYS && today >= node.earliest) {
    return NodeState.NEAR;
  }
  if (today >= node.earliest) return NodeState.CURRENT;
  return NodeState.FUTURE;
}

/**
 * 逾期修正：在「已过截止日」的节点中取最晚一期标为 OVERDUE
 * - 若有节点处于办理窗口内（CURRENT）或临近（NEAR），说明在正常办理，不标逾期
 * - clearedThrough：勾选「当前周期已年检」时，该日期及更早的节点都不算逾期
 */
function applyOverdue(nodes, today, clearedThrough) {
  if (nodes.some(n => n.state === NodeState.CURRENT || n.state === NodeState.NEAR)) {
    return nodes;
  }
  const candidates = nodes.filter(n => {
    if (!(n.deadline < today)) return false;
    if (clearedThrough && !(n.deadline > clearedThrough)) return false;
    return true;
  });
  if (!candidates.length) return nodes;

  const target = candidates.reduce((a, b) => (b.deadline > a.deadline ? b : a));
  return nodes.map(n => (n === target ? Object.assign({}, n, { state: NodeState.OVERDUE }) : n));
}

/** 判断是否享有免检政策 */
function isExemptEligible(query, notes) {
  const t = query.type;
  const operating = query.isOperating != null ? query.isOperating : t.isOperating;

  const baseEligible =
    (t.name === 'SMALL_PASSENGER' && !operating) ||
    (t.name === 'MICRO_PASSENGER' && !operating) ||
    t.name === 'MOTORCYCLE';

  if (!baseEligible) return false;

  if (t.name === 'VAN' || query.isVan) {
    notes.push('面包车不适用免检标志，已按每 6 个月上线 1 次计算');
    return false;
  }
  if (query.hadFatalAccident) {
    notes.push('曾发生人员伤亡交通事故，不适用免检标志，已按常规周期计算');
    return false;
  }
  if (query.hadIllegalModification) {
    notes.push('曾因非法改装被处罚，不适用免检标志，已按常规周期计算');
    return false;
  }
  return true;
}

/** 描述节点 */
function describe(n) {
  if (n.type === NodeType.INSPECTION) return `第 ${n.yearIndex} 年上线检测`;
  if (n.type === NodeType.LABEL_RENEWAL) return `第 ${n.yearIndex} 年申领免检合格标志`;
  return `第 ${n.yearIndex} 年达到报废年限`;
}

/**
 * 主入口
 * @param {Object} query 查询条件
 *   { type, registerDate: Date, seats, weightKg, isOperating,
 *     isVan, hadFatalAccident, hadIllegalModification, currentPeriodDone }
 * @param {Date} today 基准日期（默认今天）
 */
function calculate(query, today) {
  const baseDate = today || new Date();

  // 统一把 today 归零到当天 0 点，避免时分秒干扰日期比较
  const todayD = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
  const q = Object.assign({}, query, {
    registerDate: new Date(
      query.registerDate.getFullYear(),
      query.registerDate.getMonth(),
      1
    )
  });

  const notes = [];
  const exempt = isExemptEligible(q, notes);

  // 生成节点并判定状态
  const rawNodes = buildNodes(q, exempt, todayD);
  let resolved = rawNodes.map(n => Object.assign({}, n, { state: resolveState(n, todayD) }));

  // 勾选「当前周期已年检或已申领免检标志」
  let clearedThrough = null;
  if (q.currentPeriodDone) {
    const cand = resolved.filter(n =>
      n.state === NodeState.DONE || n.state === NodeState.CURRENT || n.state === NodeState.NEAR
    );
    if (cand.length) {
      const done = cand.reduce((a, b) => (b.deadline > a.deadline ? b : a));
      clearedThrough = done.deadline;
      resolved = resolved.map(n =>
        n === done ? Object.assign({}, n, { state: NodeState.DONE }) : n
      );
    }
  }

  const marked = applyOverdue(resolved, todayD, clearedThrough);

  // 报废判定（最高优先级）
  const scrap = q.type.scrapYears;
  const scrapDate = scrap != null ? addYears(q.registerDate, scrap) : null;
  const scrapped = scrapDate != null && todayD >= scrapDate;

  const overdue = marked.filter(n => n.state === NodeState.OVERDUE);
  const current = marked.filter(n => n.state === NodeState.CURRENT);
  const near = marked.filter(n => n.state === NodeState.NEAR);
  const future = marked.filter(n => n.state === NodeState.FUTURE);

  // 即刻年检：当月到期
  const urgent = (current.concat(near))
    .filter(n => n.deadline.getFullYear() === todayD.getFullYear()
      && n.deadline.getMonth() === todayD.getMonth())
    .sort((a, b) => a.deadline - b.deadline)[0] || null;

  const urgentDays = urgent ? dayDiff(todayD, urgent.deadline) : null;

  // 报废剩余月数
  let monthsLeft = null;
  if (scrapDate) {
    monthsLeft = (scrapDate.getFullYear() - todayD.getFullYear()) * 12
      + (scrapDate.getMonth() - todayD.getMonth());
  }
  const scrapApproaching = scrapDate != null && (scrapped || (monthsLeft != null && monthsLeft <= 12));

  // 状态汇总
  let status, statusText;
  if (scrapped) {
    status = NodeState.OVERDUE;
    statusText = `已达到报废年限（${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月已满 ${scrap} 年），请立即办理报废手续`;
  } else if (overdue.length) {
    const n = overdue.reduce((a, b) => (b.deadline > a.deadline ? b : a));
    const days = dayDiff(n.deadline, todayD);
    const action = n.type === NodeType.LABEL_RENEWAL ? '请尽快补领免检合格标志' : '请尽快上线检测';
    status = NodeState.OVERDUE;
    statusText = `已逾期 ${days} 天 · ${describe(n)}，原截止 ${fmtCN(n.deadline)}，${action}`;
  } else if (urgent) {
    status = NodeState.CURRENT;
    statusText = `您的爱车现在即可办理年检，请于 ${fmtCN(urgent.deadline)}（本月最后一天）前完成${describe(urgent)}`;
  } else if (current.length) {
    const n = current[0];
    const days = dayDiff(todayD, n.deadline);
    status = NodeState.CURRENT;
    statusText = `您的爱车满足提前 3 个月检测，可在 ${fmtCN(n.earliest)} 至 ${fmtCN(n.deadline)} 期间办理${describe(n)}，还剩 ${days} 天`;
  } else if (near.length) {
    const n = near[0];
    const days = dayDiff(todayD, n.deadline);
    status = NodeState.NEAR;
    statusText = `${describe(n)}截止 ${fmtCN(n.deadline)}，还有 ${days} 天`;
  } else if (future.length) {
    const n = future[0];
    const days = dayDiff(todayD, n.earliest);
    status = NodeState.FUTURE;
    statusText = `下次${describe(n)}可于 ${fmtCN(n.earliest)} 起办理（还有 ${days} 天）`;
  } else {
    status = NodeState.FUTURE;
    statusText = '所选年限内无待办事项';
  }

  const nextNode = urgent
    || current.concat(near, future).sort((a, b) => a.deadline - b.deadline)[0]
    || null;

  // 可提前办理提示（临近报废时不显示）
  if (current.length && !scrapApproaching) {
    const n = current[0];
    const days = dayDiff(todayD, n.deadline);
    notes.push(`您的爱车已进入提前办理期，可提前 3 个月办理${describe(n)}`);
    notes.push(`办理窗口：${fmtCN(n.earliest)} 起 至 ${fmtCN(n.deadline)}（当月最后一天）截止`);
    notes.push(`距离截止还有 ${days} 天，请在此日期前完成`);
  }

  // 报废年限提示
  let scrapNote = null;
  if (scrap != null && scrapDate) {
    if (scrapped) {
      scrapNote = `该车已达到报废年限，请立即办理报废手续（${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月已满 ${scrap} 年）`;
    } else if (monthsLeft != null && monthsLeft <= 12) {
      scrapNote = `还有 ${monthsLeft} 个月达到报废年限（${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月满 ${scrap} 年），请提前准备报废`;
    } else if (monthsLeft != null && monthsLeft <= 24) {
      scrapNote = `该车将于 ${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月 达到 ${scrap} 年强制报废年限（还剩 ${monthsLeft} 个月）`;
    } else {
      scrapNote = `${q.type.label} 强制报废年限 ${scrap} 年（${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月到期）`;
    }
  }

  // 报废手续流程
  const scrapDetail = scrapApproaching
    ? [
        scrapped
          ? '请立即办理报废手续，逾期上路将被收缴并强制报废'
          : '请提前办理报废手续，避免到期后继续上路',
        '1. 将车辆交售给有资质的报废机动车回收拆解企业',
        '2. 取得《报废机动车回收证明》',
        '3. 携带行驶证、登记证书、号牌、车主身份证明',
        '4. 到车管所办理注销登记，缴回号牌和行驶证',
        '5. 报废后不得再上道路行驶，否则将被收缴并强制报废'
      ]
    : [];

  // 免检办理流程（临近报废时不显示）
  const exemptDetail = (exempt && !scrapApproaching)
    ? [
        '免检≠不办手续：每 2 年仍需申领一次检验合格标志',
        '线上：交管12123 App → 免检车申领检验标志',
        '线下：车管所/检测站窗口，携带行驶证、交强险凭证',
        '申领前需处理完交通违法和交通事故'
      ]
    : [];

  // 距报废剩余天数
  const scrapDaysLeft = (scrapDate && !scrapped) ? dayDiff(todayD, scrapDate) : null;

  // 逾期后果提示（报废车辆不再涉及年检）
  if (overdue.length && !scrapped) {
    const od = overdue.reduce((a, b) => (b.deadline > a.deadline ? b : a));
    if (od.type === NodeType.LABEL_RENEWAL) {
      notes.push('逾期未申领免检合格标志，属于未按规定办理检验手续');
      notes.push('上路行驶被交警查到，可依法罚款并记分');
      notes.push('逾期期间发生交通事故，保险公司有权拒绝理赔');
      notes.push('连续 3 个检验周期未办理，车辆将被强制报废');
    } else {
      notes.push('逾期未年检上路，交警可依法罚款并记分');
      notes.push('逾期期间发生交通事故，保险公司有权拒绝理赔');
      notes.push('连续 3 个检验周期未年检，车辆将被强制报废');
    }
  }

  // 参数补全提示
  if (q.weightKg == null &&
    (q.type.name === 'OPERATING_TRUCK' || q.type.name === 'NON_OPERATING_TRUCK')) {
    notes.push('提示：补充「总质量」可更精准判断货车类别');
  }

  return {
    nodes: marked,
    currentNode: urgent || current[0] || null,
    nextNode,
    overdueNode: overdue.length ? overdue.reduce((a, b) => (b.deadline > a.deadline ? b : a)) : null,
    statusText,
    status,
    notes,
    exempt,
    urgentDays,
    scrapYearMonth: scrapDate ? `${scrapDate.getFullYear()}年${scrapDate.getMonth() + 1}月` : null,
    scrapNote,
    scrapDetail,
    scrapped,
    scrapApproaching,
    scrapMonthsLeft: monthsLeft,
    scrapDaysLeft,
    exemptDetail
  };
}

module.exports = {
  calculate,
  resolveTimelineYears,
  buildNodes,
  NodeType,
  NodeState
};
