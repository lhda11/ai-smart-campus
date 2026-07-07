const api = require('../../utils/api');
const storage = require('../../utils/storage');
const { INTEREST_TAGS } = require('../../utils/constants');

Page({
  data: {
    clubs: [],
    loading: true,
    isEmpty: false,
    // 标签相关
    userTags: [],
    tagPickerVisible: false,
    allTags: INTEREST_TAGS,
    selectedTags: [],
    // 推荐模式: 'jaccard' | 'ai'
    recommendMode: 'jaccard'
  },

  onLoad() {
    const tags = storage.getUserTags();
    this.setData({ userTags: tags, selectedTags: [...tags] });
  },

  onShow() {
    this.loadClubs();
  },

  onPullDownRefresh() {
    this.loadClubs().then(() => wx.stopPullDownRefresh());
  },

  async loadClubs() {
    const { userTags, recommendMode } = this.data;
    this.setData({ loading: true });

    try {
      let data;
      if (userTags.length > 0) {
        if (recommendMode === 'ai') {
          data = await api.club.recommendAI(userTags);
        } else {
          data = await api.club.recommend(userTags);
        }
      } else {
        data = await api.club.listAll();
      }

      this.setData({
        clubs: data || [],
        loading: false,
        isEmpty: !data || data.length === 0
      });
    } catch (err) {
      this.setData({ loading: false, isEmpty: this.data.clubs.length === 0 });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  // 切换推荐模式
  setModeJaccard() {
    if (this.data.recommendMode === 'jaccard') return;
    this.setData({ recommendMode: 'jaccard' });
    this.loadClubs();
  },

  setModeAI() {
    if (this.data.recommendMode === 'ai') return;
    this.setData({ recommendMode: 'ai' });
    this.loadClubs();
  },

  // 打开标签选择器
  openTagPicker() {
    this.setData({
      tagPickerVisible: true,
      selectedTags: [...this.data.userTags]
    });
  },

  // 关闭标签选择器
  closeTagPicker() {
    this.setData({ tagPickerVisible: false });
  },

  // 切换标签选中
  toggleTag(e) {
    const tag = e.currentTarget.dataset.tag;
    let selected = [...this.data.selectedTags];
    const idx = selected.indexOf(tag);
    if (idx >= 0) {
      selected.splice(idx, 1);
    } else {
      if (selected.length >= 5) {
        wx.showToast({ title: '最多选择5个标签', icon: 'none' });
        return;
      }
      selected.push(tag);
    }
    this.setData({ selectedTags: selected });
  },

  // 确认标签选择
  confirmTags() {
    const { selectedTags } = this.data;
    storage.setUserTags(selectedTags);
    this.setData({
      userTags: selectedTags,
      tagPickerVisible: false
    });
    this.loadClubs();
  },

  // 跳转社团详情
  onClubTap(e) {
    wx.navigateTo({
      url: '/pages/club-detail/club-detail?id=' + e.detail.id
    });
  }
});
