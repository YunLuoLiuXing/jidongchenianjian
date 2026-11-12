package com.nianjianchaxun.yushitong.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nianjianchaxun.yushitong.ui.theme.LocalGlass

/**
 * 液态玻璃面板：多层渐变模拟材质厚度，顶部高光 + 底部内阴影营造折射感。
 * 不使用纯 backdrop blur（Compose 无原生支持），改用「半透明叠加 + 边缘光」等价表达。
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 10.dp,
    /** true 时不透明实底（内容密集区使用，避免背景干扰阅读） */
    opaque: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlass.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(
                if (opaque) {
                    Brush.verticalGradient(listOf(tokens.opaqueTop, tokens.opaqueBottom))
                } else {
                    Brush.verticalGradient(
                        listOf(tokens.surfaceTop, tokens.surfaceMid, tokens.surfaceBottom)
                    )
                }
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(tokens.borderTop, tokens.borderBottom)
                ),
                shape = shape
            )
            .padding(1.dp)
            .clip(RoundedCornerShape(cornerRadius - 1.dp))
            .background(if (opaque) Color.Transparent else tokens.innerFill)
    ) {
        content()
    }
}

/** 玻璃按钮：主/次两种层级，带按压缩放反馈 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val tokens = LocalGlass.current
    val interaction = androidx.compose.foundation.interaction.MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btnScale")
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (primary) 12.dp else 4.dp, shape, clip = false)
            .clip(shape)
            .background(
                if (primary) {
                    Brush.verticalGradient(listOf(tokens.accentTop, tokens.accentBottom))
                } else {
                    Brush.verticalGradient(listOf(tokens.surfaceTop, tokens.surfaceBottom))
                }
            )
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (primary) 0.35f else 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 15.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text,
            color = if (primary) Color.White else tokens.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
