package com.nianjianchaxun.yushitong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.nianjianchaxun.yushitong.core.*
import com.nianjianchaxun.yushitong.ui.glass.GlassPanel
import com.nianjianchaxun.yushitong.ui.theme.LocalGlass

// 状态色：针对深色背景调亮，保证对比度
val Green = Color(0xFF34C759)
val Amber = Color(0xFFFF9F0A)
val Red = Color(0xFFFF453A)
val Gray = Color(0xFF8A94A6)
val Dark = Color(0xFFF2F6FC)

fun stateColor(s: NodeState): Color = when (s) {
    NodeState.DONE -> Gray
    NodeState.FUTURE -> Dark
    NodeState.NEAR -> Amber
    NodeState.CURRENT -> Amber
    NodeState.OVERDUE -> Red
}

@Composable
fun StatusCard(result: InspectionResult) {
    val urgent = result.urgentDays
    val color = when {
        result.status == NodeState.OVERDUE -> Red
        urgent != null -> Amber
        result.status == NodeState.NEAR || result.status == NodeState.CURRENT -> Green
        else -> Gray
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        result.scrapped -> "已达到报废年限"
                        result.status == NodeState.OVERDUE ->
                            if (result.overdueNode?.type == NodeType.LABEL_RENEWAL)
                                "已逾期（未申领免检合格标志）" else "已逾期（未年检）"
                        urgent != null -> "现在即刻年检"
                        result.status == NodeState.NEAR || result.status == NodeState.CURRENT -> "即将到期"
                        else -> "状态正常"
                    },
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // 当月到期：剩余时间单独放大、红色显示
                if (urgent != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "剩余 ",
                            color = Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "$urgent",
                            color = Red,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            " 天",
                            color = Red,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(result.statusText, color = LocalGlass.current.textSecondary, fontSize = 14.sp)
            if (!result.exempt) {
                Spacer(Modifier.height(4.dp))
                Text("该车不适用免检标志", color = Gray, fontSize = 12.sp)
            }

            // 报废年限提示：不限状态，始终展示
            result.scrapNote?.let { note ->
                Spacer(Modifier.height(8.dp))
                val scrapBg = if (result.scrapped) Color(0xFFFEF2F2) else Color(0xFFFFF7ED)
                val scrapColor = if (result.scrapped) Red else Color(0xFF9A3412)
                Column(
                    Modifier.fillMaxWidth()
                        .background(scrapBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (result.scrapped) "⚠ " else "♻ ", color = scrapColor, fontSize = 12.sp)
                        Text(
                            note,
                            color = scrapColor,
                            fontSize = 12.sp,
                            fontWeight = if (result.scrapped) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (result.scrapDetail.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        result.scrapDetail.forEach { line ->
                            Text(line, color = Color(0xFFB91C1C), fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // 免检办理流程
            if (result.exemptDetail.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0x1A3B82F6), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("免检期办理流程", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    result.exemptDetail.forEach { line ->
                        Text("· $line", color = Color(0xFF93C5FD), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
            // 逾期后果提醒
            if (result.status == NodeState.OVERDUE && !result.scrapped && result.notes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        if (result.overdueNode?.type == NodeType.LABEL_RENEWAL)
                            "⚠ 免检合格标志逾期未申领，请尽快补办"
                        else "⚠ 车辆已逾期未年检，请立即办理",
                        color = Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    result.notes.forEach { n ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("· ", color = Red, fontSize = 12.sp)
                            Text(n, color = Color(0xFFB91C1C), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NodeRow(
    node: InspectionNode,
    showEarliest: Boolean = false,
    highlight: Boolean = false,
    /** 该节点已被「达到报废年限」替代显示 */
    scrapOverride: String? = null,
    /** SCRAP 节点：距报废剩余月数（负数表示已超期） */
    scrapMonthsLeft: Int? = null,
    /** SCRAP 节点：距报废剩余天数 */
    scrapDaysLeft: Int? = null
) {
    // 报废节点始终标红；当前需要办理的周期放大高亮
    val isScrap = node.type == NodeType.SCRAP || scrapOverride != null
    val isActive = highlight ||
        node.state == NodeState.CURRENT || node.state == NodeState.OVERDUE
    val tokens = LocalGlass.current
    val c = when {
        isScrap || isActive -> Red
        node.state == NodeState.DONE -> tokens.textTertiary
        else -> tokens.textPrimary
    }
    val big = isActive || isScrap
    val titleSize = if (big) 30.sp else 15.sp
    val subSize = if (big) 26.sp else 13.sp
    val weight = if (isScrap || isActive) FontWeight.Bold else FontWeight.Medium

    Row(
        Modifier.fillMaxWidth()
            .then(
                if (big) {
                    Modifier.background(
                        if (!tokens.isDark) Color(0xFFFEF2F2) else Color(0x1AFF453A),
                        RoundedCornerShape(8.dp)
                    )
                        .padding(horizontal = 8.dp)
                } else Modifier
            )
            .padding(vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Box(Modifier.size(if (isActive) 12.dp else 10.dp).background(c, CircleShape))
            Box(Modifier.width(2.dp).height(46.dp).background(c.copy(alpha = 0.25f)))
        }
        Column(Modifier.padding(start = 6.dp)) {
            Text(
                scrapOverride ?: when (node.type) {
                    NodeType.INSPECTION -> "第 ${node.yearIndex} 年 · 上线检测"
                    NodeType.LABEL_RENEWAL -> "第 ${node.yearIndex} 年 · 申领免检合格标志"
                    NodeType.SCRAP -> "已达到报废年限"
                },
                color = c,
                fontSize = titleSize,
                fontWeight = weight
            )
            Text(
                when {
                    scrapOverride != null -> "请立即办理报废手续"
                    node.type == NodeType.SCRAP && scrapDaysLeft != null && scrapDaysLeft > 0 ->
                        "还有 $scrapDaysLeft 天报废，请提前办理"
                    node.type == NodeType.SCRAP && scrapMonthsLeft != null && scrapMonthsLeft > 0 ->
                        "剩余 $scrapMonthsLeft 个月达到报废年限"
                    node.type == NodeType.SCRAP -> "已到报废年限，请立即办理报废手续"
                    else -> "截止 " + fmt(node.deadline)
                },
                color = c,
                fontSize = subSize,
                fontWeight = weight
            )
            if (showEarliest) {
                Text("可提前至 " + fmt(node.earliest) + " 起办理", color = Red, fontSize = 12.sp)
            }
        }
    }
}

fun fmt(d: java.time.LocalDate): String =
    "%d年%02d月%02d日".format(d.year, d.monthValue, d.dayOfMonth)

/** 只读日期显示框，点击弹出滚轮选择器 */
@Composable
fun DateField(value: java.time.LocalDate?, hasError: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (hasError) Modifier.border(1.5.dp, Red, RoundedCornerShape(10.dp)) else Modifier
        ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasError) Color(0x33EF4444) else LocalGlass.current.innerFill
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (hasError) Red else LocalGlass.current.glassBorder
        ),
        onClick = onClick
    ) {
        // 文字居中，下拉三角贴右侧
        Box(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            // 可用宽度扣除三角占位，文字超宽时自适应缩小字号，不换行
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val availDp = maxWidth.value - 24f
                val text0 = value?.let { fmt(it) }
                    ?: if (hasError) "请选择注册日期" else "选择注册日期"
                val naturalWidth = text0.fold(0f) { acc, c -> acc + charEm(c) } * 15f
                val scale = if (naturalWidth > availDp && naturalWidth > 0f)
                    (availDp / naturalWidth).coerceIn(0.6f, 1f) else 1f
                Text(
                    text = text0,
                    fontSize = (15f * scale).sp,
                    color = when {
                        hasError -> Red
                        value == null -> LocalGlass.current.textPlaceholder
                        else -> LocalGlass.current.textPrimary
                    },
                    fontWeight = if (hasError) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
            }
            Text(
                "▾",
                fontSize = 18.sp,
                color = LocalGlass.current.textSecondary,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
fun FieldLabel(text: String, optional: Boolean = false, required: Boolean = false) {
    Row(Modifier.padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = LocalGlass.current.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (required) {
            Text(
                " *",
                color = Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (optional) {
            Text("  选填", color = Gray, fontSize = 11.sp)
        }
    }
}

/** 未填写时的左右抖动提示：errorTick 每次+1 触发一次抖动 */
@Composable
fun ShakeBox(errorTick: Int, content: @Composable () -> Unit) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            // 左右摆动两次，然后回正；捕获异常避免动画被打断导致崩溃
            runCatching {
                val pattern = listOf(-16f, 16f, -16f, 16f, 0f)
                pattern.forEach { target ->
                    offsetX.animateTo(target, tween(durationMillis = 110))
                }
            }
        }
    }
    // fillMaxWidth 保证在 weight 约束下有确定宽度，避免测量异常
    Box(
        Modifier.fillMaxWidth().offset { IntOffset(offsetX.value.roundToInt(), 0) }
    ) {
        content()
    }
}

@Composable
fun CheckRow(
    text: String,
    checked: Boolean,
    compact: Boolean = false,
    fontSize: TextUnit? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.then(if (compact) Modifier else Modifier.fillMaxWidth())
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            modifier = if (compact) Modifier.size(18.dp) else Modifier
        )
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        val fs = fontSize ?: if (compact) 12.sp else 14.sp
        Text(
            text,
            color = LocalGlass.current.textPrimary,
            fontSize = fs,
            lineHeight = (fs.value + 4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TypeSelector(
    selected: VehicleType,
    onSelect: (VehicleType) -> Unit
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val tokens = LocalGlass.current
    val shape = RoundedCornerShape(10.dp)

    Box(Modifier.fillMaxWidth()) {
        // 与日期字段同款玻璃底，保证视觉统一
        // 文字居中，下拉三角贴右侧
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.Transparent)
                .border(0.8.dp, tokens.glassBorder, shape)
                .clickable { expanded = true }
                .height(52.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                selected.label,
                color = tokens.textPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Text(
                "▾",
                color = tokens.textSecondary,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(tokens.opaqueTop)
        ) {
            VehicleType.values().forEach { t ->
                val isCurrent = t == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            t.label,
                            color = if (isCurrent) {
                                if (tokens.isDark) Color(0xFF7AB0FF) else Color(0xFF2563EB)
                            } else {
                                tokens.textPrimary
                            },
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = { onSelect(t); expanded = false }
                )
            }
        }
    }
}

@Composable
fun TimelineCard(result: InspectionResult) {
    GlassPanel(Modifier.fillMaxWidth(), cornerRadius = 16.dp, opaque = true) {
        Column(Modifier.padding(16.dp)) {
            Text("年检时间轴", color = LocalGlass.current.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            // 只在最近的两个周期显示「可提前办理」提示
            val hintNodes = result.nodes
                .filter { it.state != NodeState.DONE && it.state != NodeState.OVERDUE }
                .sortedBy { it.deadline }
                .take(2)
                .toSet()
            val activeNode = result.currentNode
            result.nodes.forEach {
                NodeRow(
                    it,
                    showEarliest = it in hintNodes,
                    highlight = it === activeNode,
                    scrapOverride = if (result.scrapped && it === activeNode) "已达到报废年限" else null,
                    scrapMonthsLeft = result.scrapMonthsLeft,
                    scrapDaysLeft = result.scrapDaysLeft
                )
            }

        }
    }
}

@Composable
fun MyCarRow(
    record: com.nianjianchaxun.yushitong.data.VehicleRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = LocalGlass.current.glassSurface),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, LocalGlass.current.glassBorder),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (record.alias.isNotBlank()) record.alias
                    else if (record.plate.isNotBlank()) record.plate else "未命名车辆",
                    color = LocalGlass.current.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    (record.type?.label ?: record.typeName) + " · 注册 " + record.registerDate,
                    color = Gray, fontSize = 12.sp
                )
            }
            TextButton(onClick = onDelete) {
                Text("删除", color = Gray, fontSize = 12.sp)
            }
        }
    }
}

/** 悬浮提示：淡入 → 停留 2 秒 → 淡出（可自定义文案与颜色） */
@Composable
fun SuccessToast(
    message: String = "查询成功",
    warning: Boolean = false,
    onFinish: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        runCatching {
            alpha.animateTo(1f, tween(220))
            kotlinx.coroutines.delay(2000)
            alpha.animateTo(0f, tween(420))
        }
        onFinish()
    }
    Box(
        Modifier.graphicsLayer { this.alpha = alpha.value }
            .background(
                if (warning) Color(0xE6D97706) else Color(0xE62E7D32),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Text(message, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 整块可点击的复选框：
 * 未选中时是描边圆角矩形；选中后整块填充浅绿色，再次点击恢复原样。
 * 以此作为「是否成功勾选」的直观反馈。
 */
@Composable
fun ColorCheckBox(
    text: String,
    checked: Boolean,
    fontSize: TextUnit = 14.sp,
    onChange: (Boolean) -> Unit
) {
    val tokens = LocalGlass.current
    val onColor = if (tokens.isDark) Color(0xFF7BE0A6) else Color(0xFF1F7A45)
    val fillColor = if (tokens.isDark) Color(0x3322C55E) else Color(0x3340C777)
    val borderColor = if (checked) (if (tokens.isDark) Color(0xFF4ADE80) else Color(0xFF40C777))
                      else tokens.glassBorder
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) fillColor else Color.Transparent)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .height(52.dp)
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (checked) onColor else tokens.textPrimary,
            fontSize = fontSize,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
