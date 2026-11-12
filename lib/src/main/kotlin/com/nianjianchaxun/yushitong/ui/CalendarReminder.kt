package com.nianjianchaxun.yushitong.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.nianjianchaxun.yushitong.core.InspectionResult
import java.time.LocalDate
import java.time.ZoneId

/**
 * 取本次查询应当提醒的日期：
 * 依次尝试 逾期节点 → 当前节点 → 下一节点
 */
private fun remindDateOf(r: InspectionResult): LocalDate? {
    r.overdueNode?.let { return it.deadline }
    r.currentNode?.let { return it.deadline }
    r.nextNode?.let { return it.deadline }
    return null
}

/** 跳转系统日历，自动填入年检提醒事件 */
fun createCalendarReminder(ctx: Context, result: InspectionResult, title: String) {
    // 先把结果窗口渲染成图片，保存到相册
    val imgUri = renderResultImage(ctx, result, title)

    val date = remindDateOf(result) ?: LocalDate.now()
    val zone = ZoneId.systemDefault()
    val begin = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    val end = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "办理车辆${result.remindLabel}：$title")
        putExtra(CalendarContract.Events.DESCRIPTION, buildDescription(result, title, imgUri))
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        putExtra(CalendarContract.Events.ALL_DAY, false)
        // 提前 30 天提醒办理
        putExtra(CalendarContract.Reminders.MINUTES, 30 * 24 * 60)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching { ctx.startActivity(intent) }
}

/** 事件备注：状态、办理事项、截止日 */
private fun buildDescription(r: InspectionResult, title: String, imgUri: android.net.Uri?): String = buildString {
    appendLine("车牌/标题：$title")
    appendLine("车辆状态：${r.statusText}")
    val n = r.currentNode ?: r.nextNode ?: r.overdueNode
    if (n != null) {
        appendLine("办理事项：${if (n.type.name.contains("LABEL")) "申领免检合格标志" else "上线检测"}")
        appendLine("截止日期：${n.deadline.year}年${n.deadline.monthValue}月${n.deadline.dayOfMonth}日")
    }
    // 结果图片已保存在相册，附上路径便于查看
    if (imgUri != null) {
        appendLine()
        appendLine("查询结果图片：$imgUri")
        appendLine("（已在相册「年检查询」文件夹中）")
    }
}
