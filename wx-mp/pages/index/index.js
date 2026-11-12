const { calculate, NodeType } = require('../../utils/inspection');
const { VehicleTypeList, findType } = require('../../utils/vehicleType');

/** 格式化日期为「2026年3月31日」 */
function fmtCN(d) {
  if (!d) return '';
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
}

/** 节点标题 */
function nodeTitle(n) {
  if (n.type === NodeType.SCRAP) return '已达到报废年限';
  if (n.type === NodeType.LABEL_RENEWAL) return `第 ${n.yearIndex} 年 · 申领免检合格标志`;
  return `第 ${n.yearIndex} 年 · 上线检测`;
}

const STORE_KEY = 'vehicles';

function today() {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

Page({
  data: {
    isLight: false,
    typeIndex: 0,
    typeLabels: VehicleTypeList.map(t => t.label),
    typeLabel: '',
    todayStr: '',
    dateValue: '',
    dateISO: '',
    dateError: false,
    shake: false,
    periodDone: false,
    isVan: false,
    hadAccident: false,
    hadModify: false,
    result: null,
    showToast: false,
    myCars: []
  },

  onLoad() {
    const info = wx.getSystemInfoSync();
    const d = today();
    this.setData({
      isLight: info.theme === 'light',
      typeLabel: VehicleTypeList[0].label,
      todayStr: `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
    });
    this.loadMyCars();
    wx.enableAlertBeforeUnload({
      message: '是否退出年检查询？'
    });
  },

  onShow() {
    this.loadMyCars();
  },

  loadMyCars() {
    const list = wx.getStorageSync(STORE_KEY) || [];
    const cars = list.filter(v => v.isMyCar).map(v => {
      const t = findType(v.typeKey);
      let statusColor = 'gray';
      try {
        const r = calculate({
          type: t,
          registerDate: new Date(v.registerDate),
          currentPeriodDone: false
        }, today());
        if (r.status === 'OVERDUE') statusColor = 'red';
        else if (r.status === 'CURRENT' || r.status === 'NEAR') statusColor = 'amber';
        else statusColor = 'green';
      } catch (e) {
        statusColor = 'gray';
      }
      return {
        id: v.id,
        plate: v.plate,
        typeName: t ? t.label : '',
        statusColor
      };
    });
    this.setData({ myCars: cars });
  },

  onPickType(e) {
    const idx = Number(e.detail.value);
    this.setData({
      typeIndex: idx,
      typeLabel: VehicleTypeList[idx].label
    });
  },

  onPickDate(e) {
    const iso = e.detail.value;
    const parts = iso.split('-');
    this.setData({
      dateValue: `${parts[0]}年${Number(parts[1])}月${Number(parts[2])}日`,
      dateISO: iso,
      dateError: false
    });
  },

  togglePeriodDone() {
    this.setData({ periodDone: !this.data.periodDone });
  },

  toggleVan() {
    this.setData({ isVan: !this.data.isVan });
  },

  toggleAccident() {
    this.setData({ hadAccident: !this.data.hadAccident });
  },

  toggleModify() {
    this.setData({ hadModify: !this.data.hadModify });
  },

  onQuery() {
    if (!this.data.dateISO) {
      this.setData({ dateError: true, shake: false });
      setTimeout(() => this.setData({ shake: true }), 20);
      setTimeout(() => this.setData({ shake: false }), 700);
      return;
    }

    const parts = this.data.dateISO.split('-');
    const regDate = new Date(
      Number(parts[0]),
      Number(parts[1]) - 1,
      Number(parts[2])
    );

    const type = VehicleTypeList[this.data.typeIndex];

    const r = calculate({
      type,
      registerDate: regDate,
      currentPeriodDone: this.data.periodDone,
      isVan: this.data.isVan,
      hadFatalAccident: this.data.hadAccident,
      hadIllegalModification: this.data.hadModify
    }, today());

    // 节点状态色 + 日期文本
    const nodes = r.nodes.map(n => ({
      key: n.yearIndex + '_' + n.type,
      yearIndex: n.yearIndex,
      title: nodeTitle(n),
      sub: n.type === NodeType.SCRAP
        ? '请立即办理报废手续'
        : '截止 ' + fmtCN(n.deadline),
      hint: '',
      color: this.stateColor(n.state),
      isActive: n === r.currentNode ||
        n.state === 'CURRENT' || n.state === 'OVERDUE',
      isScrap: n.type === NodeType.SCRAP
    }));

    // 只给最近两个未完成节点显示「可提前办理」
    const pending = nodes
      .filter(n => n.color !== 'gray' && n.hint === '')
      .slice(0, 2);
    pending.forEach(p => {
      const origin = r.nodes.find(o => (o.yearIndex + '_' + o.type) === p.key);
      if (origin) p.hint = '可提前至 ' + fmtCN(origin.earliest) + ' 起办理';
    });

    let statusTitle = '正常';
    if (r.scrapped) statusTitle = '已达到报废年限';
    else if (r.status === 'OVERDUE') statusTitle = '已逾期';
    else if (r.status === 'CURRENT') statusTitle = r.urgentDays != null ? '现在即刻年检' : '现在可以办理';
    else if (r.status === 'NEAR') statusTitle = '即将到期';

    const result = {
      statusTitle,
      statusText: r.statusText,
      statusColor: this.stateColor(r.status),
      showOverdueNotes: r.status === 'OVERDUE' && !r.scrapped,
      urgentDays: r.urgentDays,
      notes: r.notes,
      scrapNote: r.scrapNote,
      scrapDetail: r.scrapDetail,
      scrapped: r.scrapped,
      exemptDetail: r.exemptDetail,
      exempt: r.exempt,
      nodes
    };

    result.timelineNodes = nodes;
    this.setData({ result, showToast: true });
    setTimeout(() => this.setData({ showToast: false }), 2000);
  },

  stateColor(s) {
    if (s === 'OVERDUE') return 'red';
    if (s === 'CURRENT' || s === 'NEAR') return 'green';
    if (s === 'DONE') return 'gray';
    return 'gray';
  },

  /** 点击「我的车辆」回填表单 */
  onPickCar(e) {
    const id = e.currentTarget.dataset.id;
    const list = wx.getStorageSync(STORE_KEY) || [];
    const v = list.find(x => x.id === id);
    if (!v) return;

    const t = findType(v.typeKey);
    const idx = VehicleTypeList.findIndex(x => x.name === v.typeKey);
    const parts = v.registerDate.split('-');

    this.setData({
      typeIndex: idx >= 0 ? idx : 0,
      typeLabel: t ? t.label : this.data.typeLabel,
      dateValue: `${parts[0]}年${Number(parts[1])}月${Number(parts[2])}日`,
      dateISO: v.registerDate,
      dateError: false,
      result: null
    });
  },

  resetResult() {
    this.setData({ result: null });
  }
});
