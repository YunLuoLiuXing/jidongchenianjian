package com.nianjianchaxun.yushitong.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.nianjianchaxun.yushitong.core.InspectionResult
import java.io.File
import java.io.FileOutputStream

/** 把查询结果渲染成图片，保存到相册，返回图片 Uri */
fun renderResultImage(ctx: Context, result: InspectionResult, plate: String): Uri? = runCatching {
    val w = 1080
    val pad = 56
    val lines = mutableListOf<Pair<String, Boolean>>()
    lines += (if (plate.isNotBlank()) plate else "年检查询") to true
    lines += result.statusText to false
    result.scrapNote?.let { lines += it to false }
    if (result.exemptDetail.isNotEmpty()) {
        lines += "免检期办理流程" to false
        result.exemptDetail.forEach { lines += "· $it" to false }
    }
    if (result.inspectDetail.isNotEmpty()) {
        lines += "年检办理流程" to false
        result.inspectDetail.forEach { lines += "· $it" to false }
    }

    val titlePaint = Paint().apply {
        color = AColor.parseColor("#1565C0")
        textSize = 64f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint().apply {
        color = AColor.parseColor("#222222")
        textSize = 38f
        isAntiAlias = true
    }

    val lineHeight = 64
    val h = pad * 2 + 120 + lines.size * lineHeight
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AColor.WHITE)

    var y = pad.toFloat() + 60
    lines.forEachIndexed { i, (text, bold) ->
        val p = if (bold) titlePaint else bodyPaint
        val maxWidth = w - pad * 2
        canvas.drawText(ellipsis(text, p, maxWidth.toFloat()), pad.toFloat(), y, p)
        y += if (bold) lineHeight + 24 else lineHeight
    }

    saveToGallery(ctx, bmp)
}.getOrNull()

/** 超宽文本截断加省略号 */
private fun ellipsis(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var s = text
    while (s.length > 1 && paint.measureText("$s...") > maxWidth) {
        s = s.dropLast(1)
    }
    return "$s..."
}

/** 保存到相册，兼容 Android 10 下 */
private fun saveToGallery(ctx: Context, bmp: Bitmap): Uri? {
    val name = "nianjian_${System.currentTimeMillis()}.png"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/年检查询")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            ctx.contentResolver.openOutputStream(it)?.use { os ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
        }
        uri
    } else {
        val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "")
        dir.mkdirs()
        val f = File(dir, name)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Uri.fromFile(f)
    }
}
