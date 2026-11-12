package com.nianjianchaxun.yushitong.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import com.nianjianchaxun.yushitong.ui.theme.LocalGlass

private const val ITEM_H = 44

/** 单列滚轮：中间固定选中，上下各留一个空位保证首尾项也能滚到中间 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    weight: Float = 1f,
    onSelect: (Int) -> Unit
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    // 实时跟随：滚动过程中就按当前居中的项回调，无需等待吸附动画结束
    val centeredIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                selectedIndex
            } else {
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull { item ->
                    kotlin.math.abs(item.offset + item.size / 2 - viewportCenter)
                }?.index?.coerceIn(0, items.lastIndex) ?: selectedIndex
            }
        }
    }

    LaunchedEffect(centeredIndex) {
        if (centeredIndex != selectedIndex) onSelect(centeredIndex)
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = Modifier.weight(weight).height((ITEM_H * 5).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = (ITEM_H * 2).dp)
    ) {
        items(items.size) { i ->
            val on = i == selectedIndex
            Box(
                Modifier.height(ITEM_H.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    items[i],
                    fontSize = if (on) 19.sp else 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) {
                        if (LocalGlass.current.isDark) Color(0xFF7AB0FF) else Color(0xFF2563EB)
                    } else {
                        LocalGlass.current.textSecondary
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 日期滚动选择弹窗 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val start = initial ?: LocalDate.now()
    var year by remember { mutableIntStateOf(start.year) }
    var month by remember { mutableIntStateOf(start.monthValue) }
    var day by remember { mutableIntStateOf(start.dayOfMonth) }

    val thisYear = LocalDate.now().year
    val years = remember { (1990..thisYear).toList() }
    val months = remember { (1..12).toList() }
    val maxDay = remember(month, year) { YearMonth.of(year, month).lengthOfMonth() }
    val days = remember(maxDay) { (1..maxDay).toList() }

    // 月份/年份变化导致天数变少时，修正选中日
    LaunchedEffect(maxDay) {
        if (day > maxDay) day = maxDay
    }

    val tokens = LocalGlass.current
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (tokens.isDark) Color(0xFF1B2740) else Color(0xFFFFFFFF))
                .padding(bottom = 16.dp)
        ) {
            Text(
                "选择注册日期",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = LocalGlass.current.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(years.map { "${it}年" }, years.indexOf(year)) { year = years[it] }
                WheelColumn(months.map { "${it}月" }, months.indexOf(month)) { month = months[it] }
                WheelColumn(days.map { "${it}日" }, days.indexOf(day)) { day = days[it] }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, tokens.glassBorder
                    )
                ) {
                    Text("取消", color = tokens.textPrimary)
                }
                Button(
                    onClick = { onConfirm(LocalDate.of(year, month, day)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) { Text("确定") }
            }
        }
    }
}

}
