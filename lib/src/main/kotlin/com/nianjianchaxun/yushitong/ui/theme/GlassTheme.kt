package com.nianjianchaxun.yushitong.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass 视觉令牌
 *
 * 设计取向：克制、清晰、真实材质层级
 * - 背景为低饱和冷灰，靠明度差而非霓虹色建立空间
 * - 玻璃层 = 半透明填充 + 边缘高光 + 内阴影，模拟真实玻璃厚度
 * - 强调色只用于状态语义，不做装饰
 */
data class GlassTokens(
    val isDark: Boolean,
    // 背景（自上而下的层次）
    val bgTop: Color,
    val bgBottom: Color,
    val bgGlow1: Color,
    val bgGlow2: Color,
    // 页面背景渐变（3 段，营造空间纵深）
    val pageGradient: List<Color>,
    // 不透明面板底色（时间轴等内容密集区）
    val opaqueTop: Color,
    val opaqueBottom: Color,
    // 玻璃层（多层渐变）
    val surfaceTop: Color,
    val surfaceMid: Color,
    val surfaceBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val innerFill: Color,
    // 强调按钮渐变
    val accentTop: Color,
    val accentBottom: Color,
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    // 文字
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** 输入框空态占位提示色（四个输入框统一） */
    val textPlaceholder: Color,
    // 语义色
    val accent: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val okSoft: Color,
    val warnSoft: Color,
    val dangerSoft: Color
)

private val DarkTokens = GlassTokens(
    isDark = true,
    bgTop = Color(0xFF14161B),
    bgBottom = Color(0xFF0B0D10),
    bgGlow1 = Color(0x1A3E6BB0),
    bgGlow2 = Color(0x142E7D6B),
    pageGradient = listOf(Color(0xFF0B1220), Color(0xFF131C2E), Color(0xFF0A101C)),
    opaqueTop = Color(0xFF1B2740),
    opaqueBottom = Color(0xFF141C2E),
    surfaceTop = Color(0x26FFFFFF),
    surfaceMid = Color(0x14FFFFFF),
    surfaceBottom = Color(0x0DFFFFFF),
    borderTop = Color(0x4DFFFFFF),
    borderBottom = Color(0x0FFFFFFF),
    innerFill = Color(0x0AFFFFFF),
    accentTop = Color(0xFF4C8EF5),
    accentBottom = Color(0xFF2A62C4),
    glassSurface = Color(0x14FFFFFF),
    glassSurfaceStrong = Color(0x20FFFFFF),
    glassBorder = Color(0x24FFFFFF),
    glassHighlight = Color(0x33FFFFFF),
    textPrimary = Color(0xFFF2F4F7),
    textSecondary = Color(0xFFA8B0BD),
    textPlaceholder = Color(0xFFBFC7D4),
    textTertiary = Color(0xFF6E7887),
    accent = Color(0xFF5B9DFF),
    ok = Color(0xFF4ADE9B),
    warn = Color(0xFFF0A94B),
    danger = Color(0xFFFF6B6B),
    okSoft = Color(0x1F4ADE9B),
    warnSoft = Color(0x1FF0A94B),
    dangerSoft = Color(0x1FFF6B6B)
)

private val LightTokens = GlassTokens(
    isDark = false,
    bgTop = Color(0xFFF2F4F7),
    bgBottom = Color(0xFFE4E8EE),
    bgGlow1 = Color(0x1A7FA8D8),
    bgGlow2 = Color(0x1480BFA8),
    pageGradient = listOf(Color(0xFFEAF0F9), Color(0xFFF4F7FC), Color(0xFFE6EDF7)),
    opaqueTop = Color(0xFFFDFEFF),
    opaqueBottom = Color(0xFFF1F5FB),
    surfaceTop = Color(0xF2FFFFFF),
    surfaceMid = Color(0xD9FFFFFF),
    surfaceBottom = Color(0xC7FFFFFF),
    borderTop = Color(0xFFFFFFFF),
    borderBottom = Color(0x140F172A),
    innerFill = Color(0x66FFFFFF),
    accentTop = Color(0xFF3D7DE8),
    accentBottom = Color(0xFF1F55BC),
    glassSurface = Color(0xB3FFFFFF),
    glassSurfaceStrong = Color(0xD9FFFFFF),
    glassBorder = Color(0x1F0F172A),
    glassHighlight = Color(0xCCFFFFFF),
    textPrimary = Color(0xFF15181D),
    textSecondary = Color(0xFF5A6372),
    textTertiary = Color(0xFF8D96A5),
    textPlaceholder = Color(0xFF6B7484),
    accent = Color(0xFF2A6FDB),
    ok = Color(0xFF1B8A5A),
    warn = Color(0xFFB5701A),
    danger = Color(0xFFC4342F),
    okSoft = Color(0x1F1B8A5A),
    warnSoft = Color(0x1FB5701A),
    dangerSoft = Color(0x1FC4342F)
)

val LocalGlass = staticCompositionLocalOf { DarkTokens }

@Composable
fun NianJianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    CompositionLocalProvider(LocalGlass provides tokens) {
        content()
    }
}
