const { QUICK_QUESTIONS } = require('../../utils/constants');
const storage = require('../../utils/storage');
const api = require('../../utils/api');

Page({
  data: {
    greeting: '',
    quickQuestions: QUICK_QUESTIONS,
    lastAnnouncement: null,
    showSkeleton: false
  },

  onLoad() {
    this.updateGreeting();
  },

  onShow() {
    this.updateGreeting();
    this.loadLatestAnnouncement();
  },

  onPullDownRefresh() {
    this.loadLatestAnnouncement().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  loadLatestAnnouncement() {
    return api.announcement.list('notice', 1, 1)
      .then(list => {
        const ann = Array.isArray(list) && list.length > 0 ? list[0] : null;
        this.setData({ lastAnnouncement: ann });
        if (ann) storage.setLastAnnouncement(ann);
      })
      .catch(() => {
        // 网络异常时回退到本地缓存
        const ann = storage.getLastAnnouncement();
        if (ann) this.setData({ lastAnnouncement: ann });
      });
  },

  updateGreeting() {
    const hour = new Date().getHours();
    let greeting;
    if (hour < 6) greeting = '夜深了';
    else if (hour < 9) greeting = '早上好';
    else if (hour < 12) greeting = '上午好';
    else if (hour < 14) greeting = '中午好';
    else if (hour < 18) greeting = '下午好';
    else greeting = '晚上好';
    this.setData({ greeting });
  },

  // 快捷功能导航
  onAvatarTap() {
    wx.navigateTo({
      url: '/pages/chat-detail/chat-detail?question=' + encodeURIComponent('你好，园宝！')
    });
  },

  goChat() {
    wx.switchTab({ url: '/pages/chat/chat' });
  },

  goDiscover() {
    wx.switchTab({ url: '/pages/discover/discover' });
  },

  goBoard() {
    wx.switchTab({ url: '/pages/board/board' });
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/search' });
  },

  // 快捷问题
  tapQuickQuestion(e) {
    const question = e.currentTarget.dataset.question;
    wx.navigateTo({
      url: '/pages/chat-detail/chat-detail?question=' + encodeURIComponent(question)
    });
  },

  // 跳转公告详情
  goAnnouncement() {
    if (this.data.lastAnnouncement) {
      wx.navigateTo({
        url: '/pages/announcement-detail/announcement-detail?id=' + this.data.lastAnnouncement.id
      });
    } else {
      wx.switchTab({ url: '/pages/board/board' });
    }
  }
});
