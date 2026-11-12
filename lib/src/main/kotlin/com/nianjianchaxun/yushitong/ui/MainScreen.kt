package com.nianjianchaxun.yushitong.ui

import android.Manifest

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import com.nianjianchaxun.yushitong.ui.theme.GlassTokens
import com.nianjianchaxun.yushitong.ui.theme.LocalGlass
import com.nianjianchaxun.yushitong.ui.theme.NianJianTheme
import com.nianjianchaxun.yushitong.ui.glass.GlassButton
import com.nianjianchaxun.yushitong.ui.glass.GlassPanel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nianjianchaxun.yushitong.core.*
import com.nianjianchaxun.yushitong.data.VehicleStore
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    result: InspectionResult?,
    onResultChange: (InspectionResult?) -> Unit
) {
    NianJianTheme {
        MainScreenContent(result, onResultChange)
    }
}

@Composable
private fun MainScreenContent(
    result: InspectionResult?,
    onResultChange: (InspectionResult?) -> Unit
) {
    val ctx = LocalContext.current
    val store = remember { VehicleStore(ctx) }

    var type by remember { mutableStateOf(VehicleType.SMALL_PASSENGER) }
    var dateText by remember { mutableStateOf<LocalDate?>(null) }
    var plate by remember { mutableStateOf("") }
    var isVan by remember { mutableStateOf(false) }
    var hadAccident by remember { mutableStateOf(false) }
    var hadModify by remember { mutableStateOf(false) }
    var periodDone by remember { mutableStateOf(false) }
    var useNature by remember { mutableStateOf(UseNature.NON_OPERATING) }
    var error by remember { mutableStateOf("") }
    var myCars by remember { mutableStateOf(store.myCars()) }
    // 提醒弹窗：牌照缺少注册日期时，让用户确认用当前查询条件创建
    var remindCar by remember { mutableStateOf<String?>(null) }
    // 提醒弹窗：牌照缺少注册日期时，让用户确认用当前查询条件
    var showDatePicker by remember { mutableStateOf(false) }
    var showFloating by remember { mutableStateOf(false) }
    var editingId by remember { mutableLongStateOf(0L) }
    var savePlate by remember { mutableStateOf("") }
    var saveOwner by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var dateErrorTick by remember { mutableIntStateOf(0) }
    var showQueryHint by remember { mutableStateOf(false) }
    var queryHint by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var detailCar by remember { mutableStateOf<com.nianjianchaxun.yushitong.data.VehicleRecord?>(null) }
    var saveDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDialogDatePicker by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var showSaveHint by remember { mutableStateOf(false) }
    var saveHint by remember { mutableStateOf("") }
    var saveHintWarning by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val tokens = LocalGlass.current

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(tokens.pageGradient))) {
        // 结果弹窗出现时，背景整体模糊，降低杂乱感
        val pageModifier = if (showFloating) {
            Modifier.fillMaxSize().blur(16.dp)
        } else {
            Modifier.fillMaxSize()
        }
        Column(pageModifier.verticalScroll(rememberScrollState())) {
            // 顶部标题
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "机动车年检查询",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text("按注册月份精确计算年检与报废节点", fontSize = 12.sp, color = tokens.textSecondary)
            }

            // 查询表单
            GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("查询条件", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = tokens.textPrimary)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("车辆类型", fontSize = 14.sp, color = tokens.textSecondary)
                                Text(" *", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            }
                            Spacer(Modifier.height(6.dp))
                            TypeSelector(type) { type = it }
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("注册日期", fontSize = 14.sp, color = tokens.textSecondary)
                                Text(" *", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            }
                            Spacer(Modifier.height(6.dp))
                            ShakeBox(dateErrorTick) {
                                DateField(dateText, dateError) { showDatePicker = true }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("使用性质", fontSize = 14.sp, color = tokens.textSecondary)
                            Spacer(Modifier.height(6.dp))
                            UseNatureSelector(useNature) { useNature = it }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("车辆号牌", fontSize = 14.sp, color = tokens.textSecondary)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = plate,
                                onValueChange = { plate = it.uppercase() },
                                placeholder = {
                                    Text(
                                        "如 京A12345",
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                shape = RoundedCornerShape(10.dp),
                                colors = themeFieldColors(tokens),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // 当前周期已年检：与下方复选项交互一致（整块点击、选中变浅绿）
                    ColorCheckBox(
                        text = "当前周期已年检或已申领免检合格标志",
                        checked = periodDone,
                        fontSize = 15.sp
                    ) { periodDone = it }
                    Spacer(Modifier.height(12.dp))
                    // 两项排除免检条件：并排平行，整块点击，选中后整块变浅绿色
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            ColorCheckBox(
                                text = "曾发生人员伤亡事故",
                                checked = hadAccident,
                                fontSize = 15.sp
                            ) { hadAccident = it }
                        }
                        Box(Modifier.weight(1f)) {
                            ColorCheckBox(
                                text = "曾因非法改装被处罚",
                                checked = hadModify,
                                fontSize = 15.sp
                            ) { hadModify = it }
                        }
                    }

                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(14.dp))
                    GlassButton(
                        text = "开始查询",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (dateText == null) {
                                dateError = true
                                dateErrorTick++
                                queryHint = "请输入车辆注册日期"
                                showQueryHint = true
                                return@GlassButton
                            }
                            dateError = false
                            val q = VehicleQuery(
                                type = type,
                                useNature = useNature,
                                registerDate = dateText!!,
                                seats = null,
                                weightKg = null,
                                isOperating = type.isOperating,
                                isVan = isVan,
                                hadFatalAccident = hadAccident,
                                hadIllegalModification = hadModify,
                                currentPeriodDone = periodDone
                            )
                            onResultChange(InspectionCalculator.calculate(q, today))
                            showFloating = true
                            showToast = true
                        }
                    )

                    // 我的车辆：每行 3 个牌照
                    if (myCars.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("我的车辆", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = tokens.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        // 牌照过长时自动降列：8 位号牌（教练车/新能源）放不进 3 列宽度
                        val maxPlateLen = myCars.maxOfOrNull { it.plate.trim().length } ?: 0
                        val gridCols = when {
                            maxPlateLen <= 7 -> 3
                            maxPlateLen == 8 -> 2
                            else -> 1
                        }
                        myCars.chunked(gridCols).forEach { rowCars ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCars.forEach { v ->
                                    val ci = myCars.indexOf(v)
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .then(
                                                if (dragIndex == ci) Modifier.graphicsLayer {
                                                    scaleX = 1.06f
                                                    scaleY = 1.06f
                                                    alpha = 0.85f
                                                } else Modifier
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        PlateBadge(
                                            plate = v.plate,
                                            vehicleType = VehicleType.fromName(v.typeName),
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 21.sp,
                                            onClick = {
                                                if (dragIndex >= 0) {
                                                    if (dragIndex != ci) {
                                                        val list = myCars.toMutableList()
                                                        val moved = list.removeAt(dragIndex)
                                                        list.add(ci, moved)
                                                        myCars = list
                                                        store.updateOrder(list)
                                                    }
                                                    dragIndex = -1
                                                } else {
                                                    detailCar = v
                                                }
                                            },
                                            onLongClick = {
                                                dragIndex = ci
                                            }
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            v.owner.ifBlank { "未填写" },
                                            fontSize = 13.sp,
                                            color = tokens.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                repeat(gridCols - rowCars.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 查询结果常驻悬浮卡片
    if (showFloating) {
        val shown = result
        if (shown != null) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showFloating = false },
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (tokens.isDark) Color(0x99000000) else Color(0x66000000)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showFloating = false },
                    contentAlignment = Alignment.Center
                ) {
                    FloatingResultCard(
                        result = shown,
                        isDark = tokens.isDark,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        plate = plate,
                        onClose = { showFloating = false },
                        onRemind = { title -> createCalendarReminder(ctx, shown, title) },
                        onSave = {
                            savePlate = plate
                            saveOwner = ""
                            saveDate = dateText
                            editingId = 0L
                            showSaveDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showToast) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.Center,
            properties = androidx.compose.ui.window.PopupProperties(focusable = false)
        ) {
            SuccessToast(onFinish = { showToast = false })
        }
    }

    if (showSaveHint) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.Center,
            properties = androidx.compose.ui.window.PopupProperties(focusable = false)
        ) {
            SuccessToast(message = saveHint, warning = saveHintWarning, onFinish = { showSaveHint = false })
        }
    }

    // 查询校验提示：2 秒后自动淡出（页面内叠层，不依赖 Popup）
    if (showQueryHint) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showQueryHint = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SuccessToast(
                    message = queryHint,
                    warning = true,
                    onFinish = { showQueryHint = false }
                )
            }
        }
    }

    // 车辆详情弹窗
    detailCar?.let { car ->
        val d = parseDate(car.registerDate)
        val vt = VehicleType.fromName(car.typeName)
        val nextText = if (d != null && vt != null) {
            runCatching {
                val r = InspectionCalculator.calculate(
                    VehicleQuery(
                        type = vt,
                        useNature = UseNature.fromName(car.useNature),
                        registerDate = d,
                        seats = null,
                        weightKg = null,
                        isOperating = vt.isOperating,
                        isVan = false,
                        hadFatalAccident = false,
                        hadIllegalModification = false,
                        currentPeriodDone = false
                    ),
                    today
                )
                val n = r.currentNode ?: r.nextNode
                if (n != null) {
                    val action = if (n.type.name.contains("LABEL")) "申领免检标志" else "上线检测"
                    "${n.deadline.year}年${n.deadline.monthValue}月 · $action"
                } else "无需办理"
            }.getOrElse { "未知" }
        } else "未知"

        AlertDialog(
            onDismissRequest = { detailCar = null },
            containerColor = tokens.opaqueTop,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlateBadge(plate = car.plate, fontSize = 22.5.sp, vehicleType = vt)
                    Spacer(Modifier.width(10.dp))
                    Text("车辆信息", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = tokens.textPrimary)
                }
            },
            text = {
                Column {
                    InfoRow("车辆号牌", car.plate)
                    InfoRow("车主姓名", car.owner.ifBlank { "未填写" })
                    InfoRow("车辆类型", vt?.label ?: car.typeName)
                    InfoRow("使用性质", UseNature.fromName(car.useNature).label)
                    InfoRow("下次年审", nextText, large = true)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        savePlate = car.plate
                        saveOwner = car.owner
                        saveDate = parseDate(car.registerDate)
                        type = vt ?: type
                        useNature = UseNature.fromName(car.useNature)
                        editingId = car.id
                        detailCar = null
                        showSaveDialog = true
                    }) { Text("修改", color = tokens.accent) }
                    TextButton(onClick = {
                        val rq = if (vt != null && d != null) {
                            VehicleQuery(
                                type = vt,
                                registerDate = d,
                                useNature = UseNature.fromName(car.useNature)
                            )
                        } else null
                        if (rq == null) {
                            remindCar = car.plate
                            detailCar = null
                        } else {
                            createCalendarReminder(
                                ctx,
                                InspectionCalculator.query(rq),
                                "年检提醒：${nextText}"
                            )
                        }
                    }) { Text("提醒", color = tokens.accent, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        store.delete(car.id)
                        myCars = store.myCars()
                        detailCar = null
                    }) { Text("删除", color = tokens.danger) }
                    TextButton(onClick = { detailCar = null }) { Text("取消", color = tokens.textSecondary) }
                }
            }
        )
    }

    // 保存/修改车辆信息弹窗
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = tokens.opaqueTop,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    if (editingId > 0L) "修改车辆信息" else "保存车辆信息",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = savePlate,
                        onValueChange = { savePlate = it.uppercase() },
                        label = { Text("车牌号码") },
                        singleLine = true,
                        colors = themeFieldColors(tokens),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = saveOwner,
                        onValueChange = { saveOwner = it },
                        label = { Text("车主或使用人") },
                        singleLine = true,
                        colors = themeFieldColors(tokens),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("车辆类型", fontSize = 13.sp, color = tokens.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    TypeSelector(type) { type = it }
                    Spacer(Modifier.height(12.dp))
                    Text("使用性质", fontSize = 13.sp, color = tokens.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    UseNatureSelector(useNature) { useNature = it }
                    Spacer(Modifier.height(12.dp))
                    Text("注册日期", fontSize = 13.sp, color = tokens.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    DateField(saveDate) { showDialogDatePicker = true }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (savePlate.isBlank()) {
                        saveHint = "请先录入车牌号"
                        saveHintWarning = true
                        showSaveHint = true
                        return@TextButton
                    }
                    val dd = saveDate
                    if (dd == null) {
                        saveHint = "请选择注册日期"
                        saveHintWarning = true
                        showSaveHint = true
                        return@TextButton
                    }
                    if (editingId > 0L) {
                        store.updateInfo(editingId, savePlate, saveOwner, type.name, dd.toString(), useNature.name)
                        saveHint = "修改成功"
                    } else {
                        store.saveWithOwner(savePlate, type.name, dd.toString(), saveOwner, useNature.name)
                        saveHint = "保存成功"
                    }
                    saveHintWarning = false
                    myCars = store.myCars()
                    showSaveDialog = false
                    showSaveHint = true
                }) { Text("保存", color = tokens.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("取消", color = tokens.textSecondary) }
            }
        )
    }

    if (showDialogDatePicker) {
        DatePickerSheet(
            initial = saveDate,
            onConfirm = { saveDate = it; showDialogDatePicker = false },
            onDismiss = { showDialogDatePicker = false }
        )
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = dateText,
            onConfirm = { dateText = it; showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

    // 提醒弹窗：牌照缺少注册日期时，用当前查询条件创建日历提醒
    if (remindCar != null) {
        AlertDialog(
            onDismissRequest = { remindCar = null },
            containerColor = tokens.opaqueTop,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "创建年检提醒",
                    fontSize = 17.sp,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "牌照 ${remindCar} 尚未保存注册日期\n将使用当前查询条件创建提醒：\n车辆类型：${type.label}\n注册日期：${dateText?.let { "%d年%d月%d日".format(it.year, it.monthValue, it.dayOfMonth) } ?: "未选择"}\n使用性质：${useNature.label}",
                    fontSize = 14.sp,
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val d = dateText
                    if (d != null) {
                        val rq = VehicleQuery(type = type, registerDate = d, useNature = useNature)
                        createCalendarReminder(ctx, InspectionCalculator.query(rq), "年检提醒：${remindCar}")
                    }
                    remindCar = null
                }) { Text("创建提醒", color = tokens.accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { remindCar = null }) { Text("取消", color = tokens.textSecondary) }
            }
        )
    }
}

/** 详情行：左标签右值 */
@androidx.compose.runtime.Composable
private fun InfoRow(label: String, value: String, large: Boolean = false) {
    val size = if (large) 18.sp else 17.sp
    Row(
        Modifier.fillMaxWidth().padding(vertical = if (large) 8.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = size, color = LocalGlass.current.textSecondary)
        Text(value, fontSize = size, fontWeight = FontWeight.SemiBold, color = LocalGlass.current.textPrimary)
    }
}

/** 统一的输入框配色 */
@androidx.compose.runtime.Composable
private fun themeFieldColors(t: GlassTokens) = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = t.textPrimary,
    unfocusedTextColor = t.textPrimary,
    focusedContainerColor = t.surfaceTop,
    unfocusedContainerColor = t.surfaceTop,
    focusedBorderColor = t.accent,
    unfocusedBorderColor = t.glassBorder,
    focusedLabelColor = t.accent,
    unfocusedLabelColor = t.textSecondary,
    cursorColor = t.accent,
    // 占位提示文字跟随主题，避免深色模式下跌落为默认深灰看不清
    focusedPlaceholderColor = t.textPlaceholder,
    unfocusedPlaceholderColor = t.textPlaceholder,
    disabledPlaceholderColor = t.textPlaceholder
)

fun parseDate(s: String): LocalDate? {
    val t = s.trim().replace("/", "-").replace(".", "-")
    return runCatching {
        when {
            Regex("^\\d{4}-\\d{1,2}-\\d{1,2}$").matches(t) -> t.split("-").let {
                LocalDate.of(it[0].toInt(), it[1].toInt(), it[2].toInt())
            }
            Regex("^\\d{4}-\\d{1,2}$").matches(t) -> t.split("-").let {
                LocalDate.of(it[0].toInt(), it[1].toInt(), 1)
            }
            else -> null
        }
    }.getOrNull()
}
