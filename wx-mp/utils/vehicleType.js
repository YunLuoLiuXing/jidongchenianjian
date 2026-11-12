/**
 * 车辆类型枚举（对应行驶证「车辆类型」口径）
 * 字段与 Kotlin 版 VehicleType 完全一致
 *
 * scrapYears      强制报废年限（null = 无强制报废年限）
 * defaultSeats    默认核定载人数
 * defaultWeightKg 默认总质量
 * isSmallPassenger 是否属于免检政策适用的小微型载客汽车
 * isOperating     是否为营运车辆
 */

const VehicleTypeList = [
  // ===== 载客汽车 =====
  {
    name: 'SMALL_PASSENGER', label: '非营运小型载客汽车', group: '载客汽车',
    defaultSeats: 5, defaultWeightKg: null,
    isSmallPassenger: true, scrapYears: null, isOperating: false
  },
  {
    name: 'MICRO_PASSENGER', label: '非营运微型载客汽车', group: '载客汽车',
    defaultSeats: 4, defaultWeightKg: null,
    isSmallPassenger: true, scrapYears: null, isOperating: false
  },
  {
    name: 'MEDIUM_PASSENGER', label: '中型载客汽车', group: '载客汽车',
    defaultSeats: 15, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 20, isOperating: false
  },
  {
    name: 'LARGE_PASSENGER', label: '大型载客汽车', group: '载客汽车',
    defaultSeats: 40, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 20, isOperating: false
  },
  {
    name: 'OPERATING_PASSENGER', label: '营运载客汽车', group: '营运车辆',
    defaultSeats: 5, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 10, isOperating: true
  },

  // ===== 载货汽车 =====
  {
    name: 'NON_OPERATING_TRUCK', label: '非营运载货汽车', group: '载货汽车',
    defaultSeats: null, defaultWeightKg: 4500,
    isSmallPassenger: false, scrapYears: 15, isOperating: false
  },
  {
    name: 'OPERATING_TRUCK', label: '营运载货汽车', group: '载货汽车',
    defaultSeats: null, defaultWeightKg: 4500,
    isSmallPassenger: false, scrapYears: 15, isOperating: true
  },

  // ===== 特殊车型 =====
  {
    name: 'VAN', label: '面包车', group: '特殊车型',
    defaultSeats: 7, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: null, isOperating: false
  },
  {
    name: 'MINIBUS', label: '小型客车（9座以上）', group: '特殊车型',
    defaultSeats: 12, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 20, isOperating: false
  },

  // ===== 摩托车 =====
  {
    name: 'MOTORCYCLE', label: '摩托车', group: '摩托车',
    defaultSeats: 2, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 13, isOperating: false
  },

  // ===== 其他 =====
  {
    name: 'TRAILER', label: '挂车', group: '其他',
    defaultSeats: null, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 15, isOperating: false
  },
  {
    name: 'SPECIAL_OPERATION', label: '专项作业车', group: '其他',
    defaultSeats: null, defaultWeightKg: 4500,
    isSmallPassenger: false, scrapYears: 15, isOperating: false
  },
  {
    name: 'COACH', label: '教练车', group: '其他',
    defaultSeats: 5, defaultWeightKg: null,
    isSmallPassenger: false, scrapYears: 10, isOperating: false
  }
];

const VehicleTypes = {};
VehicleTypeList.forEach(v => { VehicleTypes[v.name] = v; });

/** 按分组返回，供 UI 分组展示 */
function groupedTypes() {
  const map = {};
  VehicleTypeList.forEach(v => {
    if (!map[v.group]) map[v.group] = [];
    map[v.group].push(v);
  });
  return map;
}

/** 按 name 查找 */
function findType(name) {
  return VehicleTypes[name] || null;
}

module.exports = { VehicleTypeList, VehicleTypes, groupedTypes, findType };
