/**
 * 本地缓存封装
 */
const STORAGE_KEYS = {
  USER_TAGS: 'userTags',
  SEARCH_HISTORY: 'searchHistory',
  LAST_ANNOUNCEMENT: 'lastAnnouncement',
  CONVERSATIONS_CACHE: 'conversationsCache',
  LOGIN_STATE: 'loginState'
};

const storage = {
  // ---- 用户兴趣标签 ----
  getUserTags() {
    return wx.getStorageSync(STORAGE_KEYS.USER_TAGS) || [];
  },

  setUserTags(tags) {
    wx.setStorageSync(STORAGE_KEYS.USER_TAGS, tags);
  },

  // ---- 搜索历史 (最多10条，去重前置) ----
  getSearchHistory() {
    return wx.getStorageSync(STORAGE_KEYS.SEARCH_HISTORY) || [];
  },

  addSearchHistory(keyword) {
    if (!keyword || !keyword.trim()) return;
    let history = this.getSearchHistory();
    history = history.filter(h => h !== keyword);
    history.unshift(keyword);
    if (history.length > 10) history = history.slice(0, 10);
    wx.setStorageSync(STORAGE_KEYS.SEARCH_HISTORY, history);
  },

  clearSearchHistory() {
    wx.setStorageSync(STORAGE_KEYS.SEARCH_HISTORY, []);
  },

  // ---- 最新公告缓存 (首页展示) ----
  setLastAnnouncement(announcement) {
    wx.setStorageSync(STORAGE_KEYS.LAST_ANNOUNCEMENT, announcement);
  },

  getLastAnnouncement() {
    return wx.getStorageSync(STORAGE_KEYS.LAST_ANNOUNCEMENT) || null;
  },

  // ---- 登录状态 ----
  setLoginState(userInfo) {
    wx.setStorageSync(STORAGE_KEYS.LOGIN_STATE, {
      loggedIn: true,
      userInfo,
      timestamp: Date.now()
    });
  },

  isLoggedIn() {
    const state = wx.getStorageSync(STORAGE_KEYS.LOGIN_STATE);
    return !!(state && state.loggedIn && state.userInfo && state.userInfo.token);
  },

  getLoginState() {
    return wx.getStorageSync(STORAGE_KEYS.LOGIN_STATE) || null;
  },

  /** 获取当前 Token */
  getToken() {
    const state = this.getLoginState();
    return (state && state.userInfo && state.userInfo.token) || null;
  },

  /** 获取当前用户ID */
  getUserId() {
    const state = this.getLoginState();
    return (state && state.userInfo && state.userInfo.userId) || null;
  },

  /** 获取学号/工号 */
  getStudentId() {
    const state = this.getLoginState();
    return (state && state.userInfo && state.userInfo.studentId) || null;
  },

  clearLoginState() {
    wx.setStorageSync(STORAGE_KEYS.LOGIN_STATE, { loggedIn: false });
  },

  // ---- 会话列表缓存 ----
  setConversationsCache(list) {
    wx.setStorageSync(STORAGE_KEYS.CONVERSATIONS_CACHE, list);
  },

  getConversationsCache() {
    return wx.getStorageSync(STORAGE_KEYS.CONVERSATIONS_CACHE) || [];
  }
};

module.exports = storage;
