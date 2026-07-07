const api = require('../../utils/api');
const storage = require('../../utils/storage');
const { HOT_SEARCHES } = require('../../utils/constants');
const app = getApp();

Page({
  data: {
    keyword: '',
    results: [],
    searchHistory: [],
    hotSearches: HOT_SEARCHES,
    searched: false,
    searching: false,
    statusBarHeight: app.globalData.statusBarHeight || 20
  },

  onLoad() {
    this.loadHistory();
  },

  onShow() {
    this.loadHistory();
  },

  loadHistory() {
    this.setData({ searchHistory: storage.getSearchHistory() });
  },

  async doSearch(e) {
    const keyword = e.detail.value;
    if (!keyword || !keyword.trim()) return;

    this.setData({ keyword: keyword.trim(), searching: true, searched: true, results: [] });

    try {
      const data = await api.search.search(keyword.trim());
      storage.addSearchHistory(keyword.trim());
      this.setData({
        results: data || [],
        searching: false,
        searchHistory: storage.getSearchHistory()
      });
    } catch (err) {
      this.setData({ searching: false });
      wx.showToast({ title: err.message || '搜索失败', icon: 'none' });
    }
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onClear() {
    this.setData({ keyword: '', results: [], searched: false });
  },

  onCancel() {
    wx.navigateBack();
  },

  tapHistory(e) {
    const keyword = e.currentTarget.dataset.keyword;
    this.doSearch({ detail: { value: keyword } });
  },

  tapHotSearch(e) {
    const keyword = e.currentTarget.dataset.keyword;
    this.doSearch({ detail: { value: keyword } });
  },

  clearHistory() {
    wx.showModal({
      title: '提示',
      content: '确定要清除搜索历史吗？',
      success: (res) => {
        if (res.confirm) {
          storage.clearSearchHistory();
          this.setData({ searchHistory: [] });
        }
      }
    });
  },

  onItemTap(e) {
    wx.navigateTo({
      url: '/pages/announcement-detail/announcement-detail?id=' + e.detail.id
    });
  }
});
