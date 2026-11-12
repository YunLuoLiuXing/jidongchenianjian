package com.nianjianchaxun.yushitong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.nianjianchaxun.yushitong.ui.MainScreen
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showExit by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf<com.nianjianchaxun.yushitong.core.InspectionResult?>(null) }

            // 拦截返回键：看结果时返回查询页，否则弹窗确认退出
            androidx.activity.compose.BackHandler(enabled = true) {
                if (result != null) {
                    result = null
                } else {
                    showExit = true
                }
            }

            MainScreen(result = result, onResultChange = { result = it })

            if (showExit) {
                val tokens = com.nianjianchaxun.yushitong.ui.theme.LocalGlass.current
                AlertDialog(
                    onDismissRequest = { showExit = false },
                    containerColor = tokens.opaqueTop,
                    titleContentColor = tokens.textPrimary,
                    textContentColor = tokens.textSecondary,
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Text(
                            "退出确认",
                            color = tokens.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Text(
                            "是否退出年检查询？",
                            color = tokens.textSecondary,
                            fontSize = 15.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showExit = false
                            finishAndRemoveTask()
                        }) {
                            Text(
                                "确定退出",
                                color = if (tokens.isDark) Color(0xFFFF453A) else Color(0xFFD92D20),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExit = false }) {
                            Text(
                                "取消",
                                color = tokens.textPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                )
            }
        }
    }
}
