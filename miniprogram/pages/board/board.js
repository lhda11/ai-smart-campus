const api = require('../../utils/api');
const storage = require('../../utils/storage');
const { CATEGORY_TABS } = require('../../utils/constants');

Page({
  data: {
    tabs: CATEGORY_TABS,
    activeCategory: 'notice',
    announcements: [],
    page: 1,
    pageSize: 10,
    hasMore: true,
    loadStatus: 'hidden', // loading | noMore | error | hidden
    isEmpty: false,
    loading: true,
    statsText: ''
  },

  onLoad() {
    this.fetchList(true);
  },

  onShow() {
    if (!this.data.loading) {
      this.fetchList(true);
    }
  },

  onPullDownRefresh() {
    this.fetchList(true).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (this.data.hasMore && this.data.loadStatus !== 'loading') {
      this.loadMore();
    }
  },

  // 切换分类
  onCategoryChange(e) {
    const key = e.detail.key;
    this.setData({ activeCategory: key, page: 1, announcements: [] });
    this.fetchList(true);
  },

  // 获取列表
  async fetchList(reset = false) {
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true, isEmpty: false, loadStatus: 'loading' });

    try {
      const data = await api.announcement.list(
        this.data.activeCategory, page, this.data.pageSize
      );
      const list = data || [];
      const hasMore = list.length >= this.data.pageSize;

      // 同步缓存最新一条通知，供首页展示
      if (reset && this.data.activeCategory === 'notice' && list.length > 0) {
        storage.setLastAnnouncement(list[0]);
      }

      this.setData({
        announcements: reset ? list : [...this.data.announcements, ...list],
        page: reset ? 1 : page,
        hasMore,
        loadStatus: hasMore ? 'hidden' : 'noMore',
        isEmpty: list.length === 0 && reset,
        loading: false,
        statsText: `共 ${(reset ? list.length : this.data.announcements.length + list.length)} 条公告 · ${this.data.tabs.length} 个分类`
      });
    } catch (err) {
      this.setData({
        loadStatus: 'error',
        loading: false,
        isEmpty: this.data.announcements.length === 0
      });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  async loadMore() {
    const nextPage = this.data.page + 1;
    this.setData({ loadStatus: 'loading', page: nextPage });
    await this.fetchList(false);
  },

  onLoadMoreRetry() {
    this.loadMore();
  },

  // 跳转公告详情
  onAnnouncementTap(e) {
    wx.navigateTo({
      url: '/pages/announcement-detail/announcement-detail?id=' + e.detail.id
    });
  },

  // 跳转搜索
  goSearch() {
    wx.navigateTo({ url: '/pages/search/search' });
  },

  // 跳转上传
  goUpload() {
    wx.navigateTo({ url: '/pages/announce-upload/announce-upload' });
  }
});
