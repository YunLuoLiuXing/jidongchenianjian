package com.nianjianchaxun.yushitong.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nianjianchaxun.yushitong.core.VehicleType

/** 新能源/混动号牌判定：8 位且号段含 D 或 F */
fun isNewEnergyPlate(plate: String): Boolean {
    val p = plate.trim().uppercase()
    return p.length >= 8 && (p.contains('D') || p.contains('F'))
}

/** 号牌底色（按真实号牌规则） */
enum class PlateColor { BLUE, YELLOW, GREEN, WHITE, BLACK }

/**
 * 根据车辆类型 + 号牌号段推导号牌颜色，匹配真实号牌观感：
 * - 摩托车：黄底黑字（普通）/ 新能源摩托车绿底黑字
 * - 大型载客/中型载客/重型货车/挂车/专项作业车：黄底黑字
 * - 新能源汽车（小型/大型均含）8 位号牌：绿底黑字
 * - 警用/军用号段：白底黑字
 * - 使馆、领事、境外号牌：黑底白字
 * - 其余小型载客/轻型货车/小型面包车等：蓝底白字
 */
fun plateColorOf(plate: String, type: VehicleType?): PlateColor {
    val p = plate.trim().uppercase()
    // 号段优先：白底（警/军）、黑底（使馆/领事）
    if (p.startsWith("WJ") || p.contains("警")) return PlateColor.WHITE
    if (p.startsWith("使") || p.startsWith("领")) return PlateColor.BLACK

    if (type == null) return if (isNewEnergyPlate(p)) PlateColor.GREEN else PlateColor.BLUE

    // 新能源号牌（8 位）不分车型均为绿底
    if (isNewEnergyPlate(p)) return PlateColor.GREEN

    return when (type) {
        VehicleType.MOTORCYCLE,
        VehicleType.COACH -> PlateColor.YELLOW
        VehicleType.LARGE_PASSENGER,
        VehicleType.MEDIUM_PASSENGER,
        VehicleType.OPERATING_PASSENGER,
        VehicleType.OPERATING_TRUCK,
        VehicleType.TRAILER,
        VehicleType.SPECIAL_OPERATION -> PlateColor.YELLOW
        else -> PlateColor.BLUE
    }
}

/**
 * 车牌样式徽标：贴合真实号牌观感
 * - 燃油车：蓝底白字
 * - 新能源/混动：绿底黑字
 * - 外围白色实心边框（模拟号牌白边），内层深色细描边
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlateBadge(
    plate: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onRemind: (() -> Unit)? = null,
    vehicleType: VehicleType? = null
) {
    val color = plateColorOf(plate, vehicleType)
    val bg = when (color) {
        PlateColor.GREEN -> Brush.verticalGradient(listOf(Color(0xFF64E58A), Color(0xFF2FBF62), Color(0xFF1FA452)))
        PlateColor.YELLOW -> Brush.verticalGradient(listOf(Color(0xFFF7D65A), Color(0xFFE9BE2E), Color(0xFFD4A81C)))
        PlateColor.WHITE -> Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F2F5), Color(0xFFE2E6EC)))
        PlateColor.BLACK -> Brush.verticalGradient(listOf(Color(0xFF3A3F47), Color(0xFF22262C), Color(0xFF12151A)))
        PlateColor.BLUE -> Brush.verticalGradient(listOf(Color(0xFF2E77DB), Color(0xFF1B57B8), Color(0xFF12408C)))
    }
    // 绿底/黄底/白底用黑字，蓝底/黑底用白字
    val textColor = when (color) {
        PlateColor.GREEN -> Color(0xFF0A1A10)
        PlateColor.YELLOW -> Color(0xFF161616)
        PlateColor.WHITE -> Color(0xFF161616)
        PlateColor.BLUE -> Color.White
        PlateColor.BLACK -> Color.White
    }
    val shown = plate.trim().ifBlank { "未填写" }

    // 外层：白色号牌边框（与提醒按钮横向并排）
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFF7F9FC))
            .border(1.dp, Color(0xFFC9D2DE), RoundedCornerShape(7.dp))
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() }
                    )
                } else Modifier
            )
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(bg)
                .border(0.8.dp, Color.Black.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            // 号牌文字：省份简称+发牌代码（如 川A）后插入一个圆点分隔
            val dotColor = when (color) {
                PlateColor.BLUE -> Color.White
                PlateColor.YELLOW -> Color(0xFF161616)
                PlateColor.GREEN -> Color(0xFF0A1A10)
                PlateColor.WHITE -> Color(0xFF161616)
                PlateColor.BLACK -> Color.White
            }
            val prefixLen = if (shown.length >= 2) 2 else shown.length
            val prefix = shown.take(prefixLen)
            val rest = shown.drop(prefixLen)
            // 按实际可用宽度自适应缩放字号，防止 8 位号牌被裁切
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // maxWidth 已是扣除内边距后的真实可用宽度
                val availDp = maxWidth.value
                // 估算字符宽度：中文 1.0em，字母数字约 0.62em，圆点约 0.5em，字距 0.6sp
                // 字符宽度总和（em），并加上 letterSpacing 的额外占位
                val letterCount = prefix.length + rest.length
                val emWidth: Float = prefix.fold(0f) { acc, c -> acc + charEm(c) } +
                        rest.fold(0f) { acc, c -> acc + charEm(c) } +
                        0.5f +
                        letterCount * (0.6f / 22f)
                val naturalWidth = emWidth * fontSize.value
                // 容差预留：实测字宽略大于估算值，按 94% 可用宽度反推字号
                val usableDp = availDp * 0.94f
                val scale = if (naturalWidth > usableDp && naturalWidth > 0f)
                    (usableDp / naturalWidth).coerceIn(0.45f, 1f) else 1f
                val fs = (fontSize.value * scale).sp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        prefix,
                        color = textColor,
                        fontSize = fs,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = (0.6f * scale).sp
                    )
                    Text(
                        "·",
                        color = dotColor,
                        fontSize = (fs.value * 0.85f).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = (fs.value * 0.10f).dp)
                    )
                    Text(
                        rest,
                        color = textColor,
                        fontSize = fs,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = (0.6f * scale).sp
                    )
                }
            }
        }
    }

    if (onRemind != null) {
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                .clickable { onRemind.invoke() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "提醒",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
    }
}

/**
 * 字符宽度估算（单位 em），按实际字体权重分档：
 * 中文全角 1.0 / 宽字母（W M）0.92 / 窄字母（I J l 1）0.40
 * 数字 0.58 / 其余字母 0.70（加粗后实测值）
 */
internal fun charEm(c: Char): Float = when {
    c.code > 0x2E7F -> 1.0f
    c in "WMQ" -> 0.92f
    c in "IJl1" -> 0.40f
    c in '0'..'9' -> 0.58f
    else -> 0.70f
}
