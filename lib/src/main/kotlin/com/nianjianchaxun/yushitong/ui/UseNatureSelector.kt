package com.nianjianchaxun.yushitong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nianjianchaxun.yushitong.core.UseNature
import com.nianjianchaxun.yushitong.ui.theme.LocalGlass

/** 使用性质选择器：默认非营运 */
@Composable
fun UseNatureSelector(
    selected: UseNature,
    onSelect: (UseNature) -> Unit
) {
    val tokens = LocalGlass.current
    var open by remember { mutableStateOf(false) }

    Box {
        // 文字居中，下拉三角贴右侧
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Transparent)
                .border(0.8.dp, tokens.glassBorder, RoundedCornerShape(10.dp))
                .clickable { open = true }
                .height(52.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                selected.label,
                fontSize = 15.sp,
                color = tokens.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Text(
                "▾",
                fontSize = 18.sp,
                color = tokens.textSecondary,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            // 不设置会用 Material 默认浅色 surface，深色模式下白字落在白底上
            modifier = Modifier.background(tokens.opaqueTop)
        ) {
            UseNature.values().forEach { n ->
                DropdownMenuItem(
                    text = {
                        Text(
                            n.label,
                            fontSize = 15.sp,
                            color = if (n == selected) tokens.accent else tokens.textPrimary,
                            fontWeight = if (n == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(n)
                        open = false
                    }
                )
            }
        }
    }
}
