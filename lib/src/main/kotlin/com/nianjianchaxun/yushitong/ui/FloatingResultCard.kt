package com.nianjianchaxun.yushitong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nianjianchaxun.yushitong.core.InspectionResult
import com.nianjianchaxun.yushitong.core.NodeState

/** 四种结果状态 */
enum class ResultKind {
    NORMAL,      // 车辆状态正常 —— 绿色
    EXPIRING,    // 满足提前3个月检验 —— 黄色
    OVERDUE,     // 逾期未检验 —— 橙色
    SCRAPPED     // 已达到报废时间 —— 红色
}

/** 根据计算结果判定属于哪一种 */
fun resolveResultKind(r: InspectionResult): ResultKind = when {
    // 红色：已达报废年限
    r.scrapped -> ResultKind.SCRAPPED
    // 红色：临近报废（可提前 3 个月办理报废）
    r.scrapApproaching -> ResultKind.SCRAPPED
    // 橙色：逆期未检验（免检标志逆期同样属于此类）
    r.status == NodeState.OVERDUE -> ResultKind.OVERDUE
    // 黄色：进入提前办理窗口 / 本月内到检
    r.urgentDays != null -> ResultKind.EXPIRING
    r.status == NodeState.CURRENT || r.status == NodeState.NEAR -> ResultKind.EXPIRING
    // 绿色：车辆状态正常
    else -> ResultKind.NORMAL
}

/** 每种状态的标题背景渐变与文字色 */
private data class KindStyle(
    val titleTop: Color,
    val titleBottom: Color,
    val bodyBg: Color,
    val titleText: Color,
    val bodyText: Color
)

private fun styleOf(kind: ResultKind, isDark: Boolean): KindStyle = when (kind) {
    ResultKind.NORMAL -> KindStyle(
        titleTop = Color(0xFF34C759),
        titleBottom = Color(0xFF248A3D),
        bodyBg = if (isDark) Color(0xFF12281A) else Color(0xFFF0FBF3),
        titleText = Color.White,
        bodyText = if (isDark) Color(0xFFA8E6BC) else Color(0xFF1B6B33),
    )
    ResultKind.EXPIRING -> KindStyle(
        titleTop = Color(0xFFFFD60A),
        titleBottom = Color(0xFFE0A800),
        bodyBg = if (isDark) Color(0xFF2B2410) else Color(0xFFFFFBEB),
        titleText = Color(0xFF3D2E00),
        bodyText = if (isDark) Color(0xFFF5E6A8) else Color(0xFF7A5C00),
    )
    ResultKind.OVERDUE -> KindStyle(
        titleTop = Color(0xFFFF9F0A),
        titleBottom = Color(0xFFC77700),
        bodyBg = if (isDark) Color(0xFF2E1F0A) else Color(0xFFFFF7ED),
        titleText = Color.White,
        bodyText = if (isDark) Color(0xFFFFD9A8) else Color(0xFF8A4B00),
    )
    ResultKind.SCRAPPED -> KindStyle(
        titleTop = Color(0xFFFF453A),
        titleBottom = Color(0xFFC1121F),
        bodyBg = if (isDark) Color(0xFF2E1214) else Color(0xFFFEF2F2),
        titleText = Color.White,
        bodyText = if (isDark) Color(0xFFFFB3AE) else Color(0xFF9B1C1C),
    )
}

/** 按实际状态生成标题（本月到期 / 报废倒计时等动态文案） */
fun resolveTitle(result: InspectionResult, kind: ResultKind, plate: String = ""): String {
    if (plate.isNotBlank()) return plate
    // 报废倒计时优先：临近报废或已报废
    if (kind == ResultKind.SCRAPPED) {
        return result.scrapTitle ?: "车辆达到报废时间"
    }
    // 本月即将到期：urgentDays 表示本月内到期
    if (kind == ResultKind.EXPIRING && result.urgentDays != null) {
        return "车辆本月即将到期"
    }
    return when (kind) {
        ResultKind.NORMAL -> "车辆状态正常"
        ResultKind.EXPIRING -> "满足提前 3 个月检验"
        ResultKind.OVERDUE -> "车辆逾期未检验"
        ResultKind.SCRAPPED -> "车辆达到报废时间"
    }
}

/**
 * 悬浮结果卡片：标题带彩色背景，正文浅色底，整体浮在屏幕上方。
 */
@Composable
fun FloatingResultCard(
    result: InspectionResult,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onRemind: ((String) -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    plate: String = ""
) {
    val kind = resolveResultKind(result)
    val st = styleOf(kind, isDark)
    val title = resolveTitle(result, kind, plate)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier
            .fillMaxWidth()
            .shadow(24.dp, shape, clip = false)
            .clip(shape)
            .background(st.bodyBg)
    ) {
        // 标题条：彩色背景，居中显示
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(st.titleTop, st.titleBottom))
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                color = st.titleText,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                result.statusText,
                color = st.bodyText,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )

            // 剩余天数：提醒时间放大一倍
            result.urgentDays?.let { d ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "剩余 $d 天",
                    color = if (isDark) Color(0xFFFF453A) else Color(0xFFC1121F),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            result.scrapNote?.let { note ->
                Spacer(Modifier.height(10.dp))
                Text(note, color = st.bodyText, fontSize = 15.sp, lineHeight = 23.sp)
            }

            if (result.scrapDetail.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                result.scrapDetail.forEach { line ->
                    Text(line, color = st.bodyText, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }

            if (result.status == NodeState.OVERDUE && !result.scrapped && result.notes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                result.notes.forEach { n ->
                    Text("· $n", color = st.bodyText, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }

            if (result.exemptDetail.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "免检期办理流程",
                    color = st.bodyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                result.exemptDetail.forEach { line ->
                    Text("· $line", color = st.bodyText, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }

            if (result.inspectDetail.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "年检办理流程",
                    color = st.bodyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                result.inspectDetail.forEach { line ->
                    Text("· $line", color = st.bodyText, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        // 底部操作区：创建提醒 | 保存信息，下方居中圆形关闭按钮
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                onRemind?.let { remind ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(st.titleTop, st.titleBottom)))
                            .clickable { remind(title) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "创建提醒",
                            color = st.titleText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                onSave?.let { save ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(st.bodyText.copy(alpha = 0.10f))
                            .clickable { save() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "保存信息",
                            color = st.bodyText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            onClose?.let { close ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(st.bodyText.copy(alpha = 0.10f))
                        .clickable { close() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "X",
                        color = st.bodyText.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}