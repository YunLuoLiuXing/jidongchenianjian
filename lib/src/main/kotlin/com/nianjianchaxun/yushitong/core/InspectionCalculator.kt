package com.nianjianchaxun.yushitong.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** 检测节点类型 */
enum class NodeType {
    /** 上线检测 */
    INSPECTION,

    /** 免检期申领检验标志 */
    LABEL_RENEWAL,

    /** 达到强制报废年限（替代该年的检测节点） */
    SCRAP
}

/** 节点状态 */
enum class NodeState {
    /** 历史节点，已办结/已失效（浅灰显示） */
    DONE,

    /** 正常未来节点（黑色显示） */
    FUTURE,

    /** 当前处于可办理窗口（橙色显示） */
    CURRENT,

    /** 临近到期，30 天内（黄色显示） */
    NEAR,

    /** 已逾期，需要尽快处理（红色显示） */
    OVERDUE
}

/**
 * 一个年检节点
 * @param yearIndex 第几年（从注册年起算，1-based）
 * @param type 节点类型
 * @param deadline 截止日期（该月最后一天）
 * @param earliest 最早可办理日期（截止月往前 2 个月首日）
 * @param state 状态
 */
data class InspectionNode(
    val yearIndex: Int,
    val type: NodeType,
    val deadline: LocalDate,
    val earliest: LocalDate,
    val state: NodeState
)

/** 查询输入 */
data class VehicleQuery(
    val type: VehicleType,
    val registerDate: LocalDate,
    val seats: Int? = null,
    val weightKg: Int? = null,
    val isOperating: Boolean? = null,
    val isVan: Boolean = false,
    val hadFatalAccident: Boolean = false,
    val hadIllegalModification: Boolean = false,
    /** 当前周期的年检已完成（勾选后跳过本期，直接看下一期） */
    val currentPeriodDone: Boolean = false,
    /** 使用性质（影响年检周期、报废年限、免检资格） */
    val useNature: UseNature = UseNature.NON_OPERATING,
    /** 非营运小微型客车默认列到第 20 年 */
    val timelineYears: Int = 20
)

/** 查询结果 */
data class InspectionResult(
    val nodes: List<InspectionNode>,
    /** 当前正在办理窗口内的节点 */
    val currentNode: InspectionNode?,
    /** 下一个待办节点 */
    val nextNode: InspectionNode?,
    /** 最近一个已逾期节点 */
    val overdueNode: InspectionNode?,
    val statusText: String,
    val status: NodeState,
    /** 说明性备注（免检排除原因、参数补全提示等） */
    val notes: List<String>,
    /** 是否享有免检政策 */
    val exempt: Boolean,
    /** 立即办理场景的剩余天数（>=0 时表示"即刻年检"，UI 用红色大字显示） */
    val urgentDays: Int? = null,
    /** 报废到期的年月文本，如「2030年6月」（为空表示无强制报废年限） */
    val scrapYearMonth: String? = null,
    /** 强制报废标题（不限状态，始终展示） */
    val scrapNote: String? = null,
    /** 报废手续/流程说明 */
    val scrapDetail: List<String> = emptyList(),
    /** 是否已达到强制报废年限 */
    val scrapped: Boolean = false,
    /** 是否临近报废（12 个月内，含已超期） */
    val scrapApproaching: Boolean = false,
    /** 距报废剩余月数（已超期为负） */
    val scrapMonthsLeft: Int? = null,
    /** 距报废剩余天数（仅未超期时有值） */
    val scrapDaysLeft: Int? = null,
    /** 免检期的办理流程说明（仅免检车型显示） */
    val exemptDetail: List<String> = emptyList(),

    /** 上线年检的办理流程说明（需上线检测时显示） */
    val inspectDetail: List<String> = emptyList(),

    /** 报废倒计时标题（例：离达到报废时间还有 2 个月） */
    val scrapTitle: String? = null,

    /** 当前办理事项名称（用于日历标题，如：办理车辆免检） */
    val remindLabel: String = "年检"
)

/**
 * 年检规则引擎（全部按 2022 年 10 月新规口径）
 *
 * 核心口径：
 * - 周期按注册「月」计算，截止日 = 该月最后一天
 *   例：2020-03-15 注册 → 第 6 年截止 2026-03-31（不是 03-15）
 * - 最早办理日 = 截止月往前 2 个月的首日
 *   例：2026-03 截止 → 最早 2026-01-01 起可办
 */
object InspectionCalculator {

    /** 免检期申领标志年份（小微型客车：第 2、4、8 年） */
    private val LABEL_YEARS = listOf(2, 4, 8)

    /** 上线检测年份（小微型客车：第 6、10 年） */
    private val INSPECT_YEARS = listOf(6, 10)

    /** 免检车型第 10 年之后每年上线 */
    private const val FREE_LIMIT_YEAR = 10

    /** 提前办理月数 */
    private const val EARLY_MONTHS = 2L

    /** 临近天数阈值 */
    private const val NEAR_DAYS = 30L

    fun calculate(query: VehicleQuery, today: LocalDate = LocalDate.now()): InspectionResult {
        val notes = mutableListOf<String>()
        val exempt = isExemptEligible(query, notes)

        // 生成节点并判定状态
        val rawNodes = buildNodes(query, exempt, today)
        var resolved = rawNodes.map { it.copy(state = resolveState(it, today)) }

        // 勾选「当前周期已年检」：把最近一个已过期/办理中的周期标记为已完成，判定自然顺延到下一期
        var clearedThrough: LocalDate? = null
        if (query.currentPeriodDone) {
            val done = resolved
                .filter { it.state == NodeState.DONE || it.state == NodeState.CURRENT || it.state == NodeState.NEAR }
                .maxByOrNull { it.deadline }
            if (done != null) {
                clearedThrough = done.deadline
                resolved = resolved.map { if (it === done) it.copy(state = NodeState.DONE) else it }
            }
        }

        val marked = applyOverdue(resolved, today, clearedThrough)

        // 报废判定（最高优先级）：达到强制报废年限后不再涉及年检
        val scrap = scrapYearsOf(query)
        val scrapDate = scrap?.let { YearMonth.from(query.registerDate).plusYears(it.toLong()) }
        val scrapped = scrapDate != null && today >= scrapDate.atDay(1)

        val overdue = marked.filter { it.state == NodeState.OVERDUE }
        val current = marked.filter { it.state == NodeState.CURRENT }
        val near = marked.filter { it.state == NodeState.NEAR }
        val future = marked.filter { it.state == NodeState.FUTURE }

        // 当月到期的办理节点：最紧急的常规场景，进入「即刻年检」模式
        val urgent = (current + near)
            .filter { it.deadline.year == today.year && it.deadline.month == today.month }
            .minByOrNull { it.deadline }
        val urgentDays = urgent?.let { ChronoUnit.DAYS.between(today, it.deadline).toInt() }

        val monthsLeft = if (scrapDate != null) ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(scrapDate)) else null
        // 临近报废：12 个月内（含已达报废），优先于年检提示
        val scrapApproaching = scrapDate != null && (
            ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(scrapDate)) <= 12
        )

        // 报废提前办理窗口：报废月往前推 2 个月的首日（含报废月共 3 个月）
        val scrapWindowStart = scrapDate?.minusMonths(2)?.atDay(1)
        val inScrapWindow = scrapWindowStart != null && !today.isBefore(scrapWindowStart)

        val (status, statusText) = when {
            scrapped -> {
                NodeState.OVERDUE to
                    "已达到报废年限（${scrapDate.year}年${scrapDate.monthValue}月已满 $scrap 年），请立即办理报废手续"
            }
            overdue.isNotEmpty() -> {
                val n = overdue.maxByOrNull { it.deadline } !!
                val days = ChronoUnit.DAYS.between(n.deadline, today)
                val action = if (n.type == NodeType.LABEL_RENEWAL)
                    "请尽快补领免检合格标志" else "请尽快上线检测"
                NodeState.OVERDUE to
                    "已逾期 $days 天 · ${describe(n)}，原截止 ${fmt(n.deadline)}，$action"
            }
            // 报废办理窗口：仅在即将报废月往前推 2 个月（含当月，共 3 个月）内提示办理
            scrapApproaching && inScrapWindow -> {
                NodeState.CURRENT to
                    "您的爱车满足提前 3 个月办理报废手续，于${scrapWindowStart.year}年${scrapWindowStart.monthValue}月起可办理"
            }
            // 即将报废但尚未进入提前窗口：告知何时可办
            scrapApproaching && scrapWindowStart != null -> {
                NodeState.NEAR to
                    "您的爱车于${scrapWindowStart.year}年${scrapWindowStart.monthValue}月即可满足提前 3 个月办理报废手续"
            }
            urgent != null -> {
                // 本月到期：按节点类型区分「免检标志」与「上线检测」措辞
                val actionText = if (urgent.type == NodeType.LABEL_RENEWAL) {
                    "申领免检合格标志"
                } else {
                    "办理年检"
                }
                NodeState.CURRENT to
                    "您的爱车现在即可${actionText}，请于 ${fmt(urgent.deadline)}（本月最后一天）前完成${describe(urgent)}"
            }
            current.isNotEmpty() -> {
                val n = current.first()
                val verb = if (n.type == NodeType.LABEL_RENEWAL) "办理免检" else "检测"
                NodeState.CURRENT to
                    "您的爱车满足提前 3 个月${verb}，可在 ${fmt(n.earliest)} 至 ${fmt(n.deadline)} 期间办理${describe(n)}"
            }
            near.isNotEmpty() -> {
                val n = near.first()
                val verb = if (n.type == NodeType.LABEL_RENEWAL) "办理免检" else "检测"
                NodeState.NEAR to
                    "您的爱车满足提前 3 个月${verb}，请在 ${fmt(n.deadline)} 前办理${describe(n)}"
            }
            future.isNotEmpty() -> {
                val n = future.first()
                val days = ChronoUnit.DAYS.between(today, n.earliest)
                val isRenew = n.type == NodeType.LABEL_RENEWAL
                val action = if (isRenew) "申领免检合格标志" else "上线检测"
                // 免检场景应提“办理免检”，而非“检测”
                val leadText = if (isRenew) "3 个月办理免检" else "3 个月检测"
                NodeState.FUTURE to
                    "下次${n.deadline.year}年${n.deadline.monthValue}月${action}可提前 $leadText，${fmt(n.earliest)} 起可办理（还有 $days 天）"
            }
            else -> NodeState.FUTURE to "所选年限内无待办事项"
        }
        val nextNode = (current + near + future).sortedBy { it.deadline }.firstOrNull()
            ?: future.firstOrNull()

        // 参数补全提示
        appendInputHints(query, exempt, notes)

        // 报废年限提示：不限状态，始终展示

        // 可提前办理提示（临近报废时不显示，改为提示办理报废）
        if (current.isNotEmpty() && !scrapApproaching) {
            val n = current.first()
            val days = ChronoUnit.DAYS.between(today, n.deadline)
            val leadVerb = if (n.type == NodeType.LABEL_RENEWAL) "申领免检合格标志" else "办理年检"
            notes += "您的爱车已进入提前办理期，可提前 3 个月${leadVerb}"
            notes += "办理窗口：${fmt(n.earliest)} 起 至 ${fmt(n.deadline)}（当月最后一天）截止"
            notes += "距离截止还有 $days 天，请在此日期前完成"
        }


        // 距报废截止日（当月最后一天）的剩余天数
        val daysToScrap = if (scrapDate != null)
            ChronoUnit.DAYS.between(today, scrapDate.atEndOfMonth()).toInt() else null

        // 剩余不足一个月时改用天数提示
        val scrapCountdown: String? = when {
            scrapped -> null
            daysToScrap == null -> null
            daysToScrap < 0 -> null
            daysToScrap <= 30 -> "还有 $daysToScrap 天"
            monthsLeft != null -> "还有 $monthsLeft 个月"
            else -> null
        }

        // 报废倒计时标题：不足 1 个月显示天数，否则显示月数
        val scrapTitle: String? = when {
            scrapped -> "已达到报废年限"
            daysToScrap == null || daysToScrap < 0 -> null
            daysToScrap <= 30 -> "离达到报废时间还有 $daysToScrap 天"
            monthsLeft != null -> "离达到报废时间还有 $monthsLeft 个月"
            else -> null
        }

        val scrapNote = if (scrap != null && scrapDate != null) {
            when {
                scrapped ->
                    "该车已达到报废年限，请立即办理报废手续（${scrapDate.year}年${scrapDate.monthValue}月已满 $scrap 年）"
                scrapCountdown != null && daysToScrap != null && daysToScrap <= 30 ->
                    "还有 $daysToScrap 天达到报废年限（${scrapDate.year}年${scrapDate.monthValue}月满 $scrap 年），请立即准备报废"
                monthsLeft != null && monthsLeft <= 12 ->
                    "还有 $monthsLeft 个月达到报废年限（${scrapDate.year}年${scrapDate.monthValue}月满 $scrap 年），请提前准备报废"
                monthsLeft != null && monthsLeft <= 24 ->
                    "该车将于 ${scrapDate.year}年${scrapDate.monthValue}月 达到 $scrap 年强制报废年限（还剩 $monthsLeft 个月）"
                else ->
                    "${query.type.label} 强制报废年限 $scrap 年（${scrapDate.year}年${scrapDate.monthValue}月到期）"
            }
        } else null

        // 报废手续流程（已报废或临近报废时显示）
        val scrapDetail = if (scrapApproaching) {
            listOf(
                if (scrapped) "请立即办理报废手续，逾期上路将被收缴并强制报废"
                else "请提前办理报废手续，避免到期后继续上路"
            ) + listOf(
                "1. 将车辆交售给有资质的报废机动车回收拆解企业",
                "2. 取得《报废机动车回收证明》",
                "3. 携带行驶证、登记证书、号牌、车主身份证明",
                "4. 到车管所办理注销登记，缴回号牌和行驶证",
                "5. 报废后不得再上道路行驶，否则将被收缴并强制报废"
            )
        } else emptyList()

        // 距报废剩余天数（供时间轴显示）
        val scrapDaysLeft = if (scrapDate != null && !scrapped)
            ChronoUnit.DAYS.between(today, scrapDate.atEndOfMonth()).toInt() else null

        // 当前周期（优先逆期 > 今月到期 > 可办理 > 即将到期）
        val activeNode = overdue.maxByOrNull { it.deadline }
            ?: urgent
            ?: current.firstOrNull()
            ?: near.firstOrNull()

        // 当前周期是否为“申领免检标志”
        val isLabelRenewalPeriod = activeNode?.type == NodeType.LABEL_RENEWAL

        // 当前办理事项（供日历标题使用）
        val remindLabel: String = when {
            scrapped || scrapApproaching -> "报废"
            activeNode?.type == NodeType.LABEL_RENEWAL -> "车辆免检"
            else -> "年检"
        }

        // 免检期的办理流程（仅当前周期需申领免检标志时显示）
        val exemptDetail = if (exempt && isLabelRenewalPeriod && !scrapped && !scrapApproaching) listOf(
            "免检≠不办手续：每 2 年仍需申领一次检验合格标志",
            "线上：交管12123 App → 免检车申领检验标志",
            "线下：车管所/检测站窗口，携带行驶证、交强险凭证、办理人身份证",
            "申领前需处理完交通违法和交通事故"
        ) else emptyList()

        // 上线年检的办理流程（当前周期需上线检测时显示）
        val isInspectPeriod = activeNode?.type == NodeType.INSPECTION
        val inspectDetail = if (isInspectPeriod && !scrapped && !scrapApproaching) {
            val base = mutableListOf(
                "携带行驶证、交强险凭证、办理人身份证",
                "车辆配备一件反光背心和三角警示牌",
                "提前处理完交通违法和交通事故",
                "前往检测站上线检测，合格后领取检验合格标志"
            )
            if (query.type.group == "载货汽车") {
                val w = query.weightKg
                if (w != null && w > 3500) {
                    base.add("总质量 $w kg 已超过 3500 kg，还需配备两个停车楔")
                } else {
                    base.add("货车总质量若大于 3500 kg，还需配备两个停车楔")
                }
            }
            base
        } else emptyList()
        // 逾期后果提示（报废车辆不再涉及年检，不显示逾期后果）
        val od = overdue.maxByOrNull { it.deadline }
        if (od != null && !scrapped) {
            if (od.type == NodeType.LABEL_RENEWAL) {
                notes += "逾期未申领免检合格标志，属于未按规定办理检验手续"
                notes += "上路行驶被交警查到，可依法罚款并记分"
                notes += "逾期期间发生交通事故，保险公司有权拒绝理赔"
                notes += "连续 3 个检验周期未办理，车辆将被强制报废"
            } else {
                notes += "逾期未年检上路，交警可依法罚款并记分"
                notes += "逾期期间发生交通事故，保险公司有权拒绝理赔"
                notes += "连续 3 个检验周期未年检，车辆将被强制报废"
            }
        }

        return InspectionResult(
            nodes = marked,
            currentNode = (urgent ?: current.firstOrNull()),
            nextNode = nextNode,
            overdueNode = overdue.maxByOrNull { it.deadline },
            statusText = statusText,
            status = status,
            notes = notes,
            exempt = exempt,
            urgentDays = urgentDays,
            scrapYearMonth = scrapDate?.let { "${it.year}年${it.monthValue}月" },
            scrapNote = scrapNote,
            scrapDetail = scrapDetail,
            scrapped = scrapped,
            scrapApproaching = scrapApproaching,
            scrapMonthsLeft = monthsLeft?.toInt(),
            scrapDaysLeft = scrapDaysLeft,
            exemptDetail = exemptDetail,
            inspectDetail = inspectDetail,
            scrapTitle = scrapTitle,
            remindLabel = remindLabel
        )
    }

    /**
     * 计算时间轴要覆盖的年限。
     * - 有强制报废年限的车型：取报废年限（到年限后不再生成节点）
     * - 无强制报废年限（非营运小微型客车、面包车）：按 20 年滚动延伸，
     *   至少覆盖到「当前年份 + 5 年」之后的下一个 20 年整数倍
     */
    private fun resolveTimelineYears(query: VehicleQuery, today: LocalDate): Int {
        val scrap = scrapYearsOf(query)
        if (scrap != null) return scrap

        val regYear = query.registerDate.year
        val elapsed = today.year - regYear + 1
        // 需要至少覆盖到今年之后 5 年，再向上取整到 20 的倍数
        val needed = maxOf(elapsed + 5, 20)
        return ((needed + 19) / 20) * 20
    }

    /** 生成时间轴节点 */
    private fun buildNodes(query: VehicleQuery, exempt: Boolean, today: LocalDate): List<InspectionNode> {
        val years = resolveTimelineYears(query, today)
        val regMonth = YearMonth.from(query.registerDate)
        val nodes = mutableListOf<InspectionNode>()

        // 免检车型（非营运小微型客车 / 摩托车）
        if (exempt) {
            for (y in 1..years) {
                val node = when {
                    y in LABEL_YEARS -> makeNode(y, NodeType.LABEL_RENEWAL, regMonth)
                    y in INSPECT_YEARS -> makeNode(y, NodeType.INSPECTION, regMonth)
                    y > FREE_LIMIT_YEAR -> makeNode(y, NodeType.INSPECTION, regMonth)
                    else -> null
                }
                if (node != null) nodes += node
            }
            return nodes
        }

        // 按使用性质分流：营运载客类（网约/出租/租赁）走营运周期
        val nature = query.useNature
        val isOperatingPassenger = nature in setOf(
            UseNature.RIDE_HAILING,
            UseNature.TAXI,
            UseNature.LEASING,
            UseNature.OPERATING_TO_PRIVATE,
            UseNature.RIDE_HAILING_TO_PRIVATE
        )

        // 营转非：周期同私家车（免检不适用，但按 6 年内每 2 年申领标志处理）
        val isOperatingToNon = nature == UseNature.OPERATING_TO_PRIVATE ||
            nature == UseNature.RIDE_HAILING_TO_PRIVATE

        if (isOperatingToNon) {
            for (y in 1..years) {
                val node = when {
                    y in LABEL_YEARS -> makeNode(y, NodeType.LABEL_RENEWAL, regMonth)
                    y in INSPECT_YEARS -> makeNode(y, NodeType.INSPECTION, regMonth)
                    y > FREE_LIMIT_YEAR -> makeNode(y, NodeType.INSPECTION, regMonth)
                    else -> null
                }
                if (node != null) nodes += node
            }
            return nodes
        }

        if (isOperatingPassenger) {
            for (y in 1..years) {
                val deadline = regMonth.plusYears(y.toLong()).atEndOfMonth()
                if (y <= 5) {
                    nodes += buildNode(y, NodeType.INSPECTION, deadline)
                } else {
                    nodes += buildNode(y, NodeType.INSPECTION, deadline)
                    val h1 = YearMonth.from(deadline).minusMonths(6).atEndOfMonth()
                    nodes += buildNode(y, NodeType.INSPECTION, h1)
                }
            }
            // 营运载客报废年限（8 年）由末尾逻辑处理
        }

        val t = query.type
        when (t) {
            // 营运载客汽车 / 面包车：5 年内每年 1 次，超 5 年每 6 个月 1 次
            VehicleType.OPERATING_PASSENGER, VehicleType.VAN -> {
                for (y in 1..years) {
                    val deadline = regMonth.plusYears(y.toLong()).atEndOfMonth()
                    if (y <= 5) {
                        nodes += buildNode(y, NodeType.INSPECTION, deadline)
                    } else {
                        nodes += buildNode(y, NodeType.INSPECTION, deadline)
                        val h1 = YearMonth.from(deadline).minusMonths(6).atEndOfMonth()
                        nodes += buildNode(y, NodeType.INSPECTION, h1)
                    }
                }
            }
            // 载货汽车：10 年内每年 1 次，超 10 年每 6 个月 1 次
            VehicleType.OPERATING_TRUCK, VehicleType.NON_OPERATING_TRUCK -> {
                for (y in 1..years) {
                    val deadline = regMonth.plusYears(y.toLong()).atEndOfMonth()
                    if (y <= 10) {
                        nodes += buildNode(y, NodeType.INSPECTION, deadline)
                    } else {
                        nodes += buildNode(y, NodeType.INSPECTION, deadline)
                        val h1 = YearMonth.from(deadline).minusMonths(6).atEndOfMonth()
                        nodes += buildNode(y, NodeType.INSPECTION, h1)
                    }
                }
            }
            // 其他（挂车、专项作业车、教练车等）：每年 1 次
            else -> {
                for (y in 1..years) {
                    val deadline = regMonth.plusYears(y.toLong()).atEndOfMonth()
                    nodes += buildNode(y, NodeType.INSPECTION, deadline)
                }
            }
        }

        // 有强制报废年限的车型：把报废年限那一期替换为报废节点
        val scrap = scrapYearsOf(query)
        if (scrap != null) {
            val scrapDeadline = YearMonth.from(query.registerDate)
                .plusYears(scrap.toLong()).atEndOfMonth()
            // 报废年之前的节点原样保留
            val kept = nodes.filter { it.yearIndex < scrap }
            // 报废当期只保留最后一条，并改造为报废节点
            val lastOfScrapYear = nodes.lastOrNull { it.yearIndex == scrap }
            val scrapNode = (lastOfScrapYear ?: InspectionNode(
                yearIndex = scrap,
                type = NodeType.INSPECTION,
                deadline = scrapDeadline,
                earliest = YearMonth.from(scrapDeadline).minusMonths(2).atDay(1),
                state = NodeState.FUTURE
            )).copy(type = NodeType.SCRAP, deadline = scrapDeadline)
            return kept + scrapNode
        }

        return nodes
    }

    private fun makeNode(yearIndex: Int, type: NodeType, regMonth: YearMonth): InspectionNode {
        val deadline = regMonth.plusYears(yearIndex.toLong()).atEndOfMonth()
        return buildNode(yearIndex, type, deadline)
    }

    private fun buildNode(yearIndex: Int, type: NodeType, deadline: LocalDate): InspectionNode {
        val earliest = YearMonth.from(deadline).minusMonths(EARLY_MONTHS).atDay(1)
        return InspectionNode(
            yearIndex = yearIndex,
            type = type,
            deadline = deadline,
            earliest = earliest,
            state = NodeState.FUTURE
        )
    }

    /**
     * 状态判定。
     * 关键：不能假设车主办过历史年检。
     * - 已过截止日 → EXPIRED（后续在 applyOverdue 里定谁是逾期）
     * - 窗口内 → CURRENT / NEAR
     * - 未到窗口 → FUTURE
     */
    private fun resolveState(node: InspectionNode, today: LocalDate): NodeState {
        if (node.deadline.isBefore(today)) return NodeState.DONE
        val daysToDeadline = ChronoUnit.DAYS.between(today, node.deadline)
        if (daysToDeadline in 0..NEAR_DAYS && !today.isBefore(node.earliest)) {
            return NodeState.NEAR
        }
        if (!today.isBefore(node.earliest)) return NodeState.CURRENT
        return NodeState.FUTURE
    }

    /**
     * 逾期修正：所有已过截止日的节点中，最晚那一期就是「当前该办却逾期」的。
     * 不假设车主办过任何一期——只要过了截止日没办就是逾期。
     */
    private fun applyOverdue(
        nodes: List<InspectionNode>,
        today: LocalDate,
        /** 勾选了「当前周期已年检」时，该周期及更早的节点都不算逾期 */
        clearedThrough: LocalDate? = null
    ): List<InspectionNode> {
        // 已有节点处于办理窗口内/临近，说明正在正常办理，不标逾期
        if (nodes.any { it.state == NodeState.CURRENT || it.state == NodeState.NEAR }) return nodes
        val target = nodes
            .filter { it.deadline.isBefore(today) }
            .filter { clearedThrough == null || it.deadline.isAfter(clearedThrough) }
            .maxByOrNull { it.deadline } ?: return nodes
        return nodes.map { if (it === target) it.copy(state = NodeState.OVERDUE) else it }
    }

    /** 判断是否享有免检政策 */
    private fun isExemptEligible(query: VehicleQuery, notes: MutableList<String>): Boolean {
        val t = query.type
        val operating = query.isOperating ?: t.isOperating

        // 仅「非营运」享受免检
        if (query.useNature != UseNature.NON_OPERATING) {
            notes += "${query.useNature.label}不适用免检政策"
            return false
        }

        // 只有非营运小微型载客汽车、摩托车享受免检
        val baseEligible = (t == VehicleType.SMALL_PASSENGER && !operating) ||
            (t == VehicleType.MICRO_PASSENGER && !operating) ||
            t == VehicleType.MOTORCYCLE
        if (!baseEligible) return false

        if (t == VehicleType.VAN || query.isVan) {
            notes += "面包车不适用免检标志，已按每 6 个月上线 1 次计算"
            return false
        }
        if (query.hadFatalAccident) {
            notes += "曾发生人员伤亡交通事故，不适用免检标志，已按常规周期计算"
            return false
        }
        if (query.hadIllegalModification) {
            notes += "曾因非法改装被处罚，不适用免检标志，已按常规周期计算"
            return false
        }
        return true
    }

    /** 参数补全提示 */
    private fun appendInputHints(query: VehicleQuery, exempt: Boolean, notes: MutableList<String>) {
        if (query.weightKg == null &&
            (query.type == VehicleType.OPERATING_TRUCK || query.type == VehicleType.NON_OPERATING_TRUCK)
        ) {
            notes += "提示：补充「总质量」可更精准判断货车类别"
        }
    }

    /** 对外入口 */
    fun query(query: VehicleQuery, today: LocalDate = LocalDate.now()): InspectionResult =
        calculate(query, today)

    private fun describe(n: InspectionNode): String = when (n.type) {
        NodeType.INSPECTION -> "第 ${n.yearIndex} 年上线检测"
        NodeType.LABEL_RENEWAL -> "第 ${n.yearIndex} 年申领免检合格标志"
        NodeType.SCRAP -> "第 ${n.yearIndex} 年达到报废年限"
    }

    private fun fmt(d: LocalDate): String =
        "%d年%d月%d日".format(d.year, d.monthValue, d.dayOfMonth)
}

/** 按使用性质推导报废年限（营运载客 8 年，营转非仍按营运） */
internal fun scrapYearsOf(query: VehicleQuery): Int? = when (query.useNature) {
    UseNature.RIDE_HAILING,
    UseNature.TAXI,
    UseNature.LEASING,
    UseNature.OPERATING_TO_PRIVATE,
    UseNature.RIDE_HAILING_TO_PRIVATE -> 8
    UseNature.FREIGHT -> query.type.scrapYears ?: 15
    UseNature.NON_OPERATING -> query.type.scrapYears
}
