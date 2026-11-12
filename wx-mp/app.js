App({
  globalData: {
    theme: 'dark'
  },
  onLaunch() {
    // 读取系统主题
    const info = wx.getSystemInfoSync();
    this.globalData.theme = info.theme === 'light' ? 'light' : 'dark';
  }
});
