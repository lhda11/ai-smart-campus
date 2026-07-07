const storage = require('./utils/storage');

App({
  onLaunch() {
    // 初始化本地缓存
    const userTags = wx.getStorageSync('userTags') || [];
    const searchHistory = wx.getStorageSync('searchHistory') || [];
    if (!wx.getStorageSync('userTags')) wx.setStorageSync('userTags', userTags);
    if (!wx.getStorageSync('searchHistory')) wx.setStorageSync('searchHistory', searchHistory);

    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync();
    this.globalData.statusBarHeight = systemInfo.statusBarHeight;
    this.globalData.windowHeight = systemInfo.windowHeight;
    this.globalData.windowWidth = systemInfo.windowWidth;

    // 从登录状态同步用户信息
    const loginState = storage.getLoginState();
    if (loginState && loginState.userInfo) {
      const info = loginState.userInfo;
      this.globalData.userId = info.userId;
      this.globalData.userName = info.nickname || info.studentId || '同学';
      this.globalData.userAvatar = info.avatarUrl || '';
      this.globalData.studentId = info.studentId;
      this.globalData.role = info.role;
    }
  },

  globalData: {
    userId: null,
    userName: '同学',
    userAvatar: '',
    studentId: '',
    role: 1,
    statusBarHeight: 0,
    windowHeight: 0,
    windowWidth: 0
  }
});
