package com.nianjianchaxun.yushitong.core

/**
 * 车辆类型枚举（对应行驶证「车辆类型」口径）
 */
enum class VehicleType(
    val label: String,
    val group: String,
    val defaultSeats: Int? = null,
    val defaultWeightKg: Int? = null,
    /** 是否属于免检政策适用的小微型载客汽车 */
    val isSmallPassenger: Boolean = false,
    /** 强制报废年限（null = 无强制报废年限，如非营运小微型客车） */
    val scrapYears: Int? = null,
    /** 是否为营运车辆（影响检测频次） */
    val isOperating: Boolean = false
) {
    // ===== 载客汽车 =====
    SMALL_PASSENGER("小型载客汽车", "载客汽车", 5, null, true, null, false),
    MICRO_PASSENGER("微型载客汽车", "载客汽车", 4, null, true, null, false),
    MEDIUM_PASSENGER("中型载客汽车", "载客汽车", 15, null, false, 20, false),
    LARGE_PASSENGER("大型载客汽车", "载客汽车", 40, null, false, 20, false),

    // ===== 营运载客 =====
    OPERATING_PASSENGER("公路客运汽车", "营运车辆", 5, null, false, 10, true),

    // ===== 载货汽车 =====
    NON_OPERATING_TRUCK("轻型载货汽车", "载货汽车", null, 4500, false, 15, false),
    OPERATING_TRUCK("重型载货汽车", "载货汽车", null, 4500, false, 15, true),

    // ===== 特殊车型 =====
    VAN("小型面包车", "特殊车型", 7, null, false, null, false),
    MINIBUS("小型客车（9座以上）", "特殊车型", 12, null, false, 20, false),
    MOTORCYCLE("摩托车", "摩托车", 2, null, false, 13, false),
    TRAILER("挂车", "其他", null, null, false, 15, false),
    SPECIAL_OPERATION("专项作业车", "其他", null, 4500, false, 15, false),
    COACH("教练车", "其他", 5, null, false, 10, false);

    companion object {
        /** 所有类型（按分组展示用） */
        fun all(): List<VehicleType> = values().toList()

        /** 分组后的类型，用于下拉列表分组展示 */
        fun grouped(): Map<String, List<VehicleType>> =
            values().toList().groupBy { it.group }

        fun fromName(name: String): VehicleType? = values().firstOrNull { it.name == name }
    }
}
