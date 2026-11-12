package com.nianjianchaxun.yushitong.core

/**
 * 使用性质（行驶证登记口径）
 *
 * 影响三个方面：
 * 1. 年检周期 —— 营运载客 5 年内每年、超 5 年每 6 个月；货运 10 年内每年、超 10 年每 6 个月
 * 2. 强制报废年限 —— 营运载客 8 年、货运 15 年，非营运小微型客车无强制报废
 * 3. 免检资格 —— 仅「非营运」且非面包车可享 6 年内每 2 年申领标志
 */
enum class UseNature(
    val label: String,
    /** 是否按营运周期计算年检 */
    val operatingCycle: Boolean = false,
    /** 年检周期分组：PASSENGER 营运载客 / FREIGHT 货运 / PRIVATE 私家车 */
    val cycleGroup: String = "PRIVATE",
    /** 强制报废年限，null 表示无 */
    val scrapYears: Int? = null,
    /** 是否享受免检政策 */
    val exempt: Boolean = false
) {
    /** 非营运 —— 默认，享受免检 */
    NON_OPERATING("非营运", false, "PRIVATE", null, true),

    /** 营转非 —— 周期同私家车，报废按营运 8 年，不免检 */
    OPERATING_TO_PRIVATE("营转非", false, "PRIVATE", 8, false),

    /** 货运 —— 按载货汽车计算 */
    FREIGHT("货运", true, "FREIGHT", 15, false),

    /** 租赁 —— 按营运载客计算 */
    LEASING("租赁", true, "PASSENGER", 8, false),

    /** 出租客运 —— 按营运载客计算 */
    TAXI("出租客运", true, "PASSENGER", 8, false),

    /** 预约出租客运 —— 按营运载客计算 */
    RIDE_HAILING("预约出租客运", true, "PASSENGER", 8, false),

    /** 预约出租转非 —— 同营转非 */
    RIDE_HAILING_TO_PRIVATE("预约出租转非", false, "PRIVATE", 8, false);

    companion object {
        /** 所有选项（下拉展示用） */
        fun all(): List<UseNature> = values().toList()

        fun fromName(name: String?): UseNature =
            values().firstOrNull { it.name == name } ?: NON_OPERATING

        fun fromLabel(label: String?): UseNature =
            values().firstOrNull { it.label == label } ?: NON_OPERATING
    }
}
