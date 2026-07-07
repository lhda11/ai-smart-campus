const api = require('../../utils/api');
const { formatTime } = require('../../utils/util');

Page({
  data: {
    conversations: [],
    loading: true,
    isEmpty: false
  },

  // 滑动删除触摸状态
  _touchStartX: 0,
  _touchStartY: 0,
  _swipedId: null,

  onShow() {
    this.fetchConversations();
  },

  onPullDownRefresh() {
    this.fetchConversations().then(() => wx.stopPullDownRefresh());
  },

  async fetchConversations() {
    this.setData({ loading: true });

    try {
      const data = await api.chat.getConversations();
      const list = (data || []).map(c => ({
        ...c,
        _swiped: false,
        displayTime: formatTime(c.createdAt || c.updatedAt)
      }));
      this.setData({
        conversations: list,
        loading: false,
        isEmpty: list.length === 0
      });
    } catch (err) {
      this.setData({ loading: false, isEmpty: this.data.conversations.length === 0 });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  // 新建对话
  newChat() {
    wx.navigateTo({ url: '/pages/chat-detail/chat-detail' });
  },

  // 进入对话详情
  openChat(e) {
    // 如果当前有滑开的项，先收回
    if (this._swipedId) {
      this.resetSwipe();
      return;
    }
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: '/pages/chat-detail/chat-detail?convId=' + id });
  },

  // 触摸开始
  onTouchStart(e) {
    this._touchStartX = e.touches[0].clientX;
    this._touchStartY = e.touches[0].clientY;
  },

  // 触摸移动
  onTouchMove(e) {
    // 预留，但不在此处做实时跟随，微信小程序触摸事件在列表上性能有限
  },

  // 触摸结束
  onTouchEnd(e) {
    const deltaX = e.changedTouches[0].clientX - this._touchStartX;
    const deltaY = Math.abs(e.changedTouches[0].clientY - this._touchStartY);

    // 只处理水平滑动（X > 4倍 Y 且大于 40px）
    if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > deltaY * 4) {
      const { id } = e.currentTarget.dataset;

      // 先关闭之前的
      if (this._swipedId && this._swipedId !== id) {
        this.resetSwipe();
      }

      if (deltaX < -40) {
        // 向左滑动 → 显示删除
        const conversations = this.data.conversations.map(c => ({
          ...c,
          _swiped: c.id === id
        }));
        this.setData({ conversations });
        this._swipedId = id;
      } else if (deltaX > 40) {
        // 向右滑动 → 关闭
        this.resetSwipe();
      }
    }
  },

  // 重置所有滑动状态
  resetSwipe() {
    if (!this._swipedId) return;
    const conversations = this.data.conversations.map(c => ({
      ...c,
      _swiped: false
    }));
    this.setData({ conversations });
    this._swipedId = null;
  },

  // 删除对话
  deleteChat(e) {
    const { id } = e.currentTarget.dataset;
    wx.showModal({
      title: '确认删除',
      content: '删除后对话记录不可恢复',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.chat.deleteConversation(id);
            const conversations = this.data.conversations.filter(c => c.id !== id);
            this.setData({
              conversations,
              isEmpty: conversations.length === 0
            });
            this._swipedId = null;
            wx.showToast({ title: '已删除', icon: 'success' });
          } catch (err) {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          }
        } else {
          this.resetSwipe();
        }
      }
    });
  },

  getTime(isoStr) {
    return formatTime(isoStr);
  },

  // 一键清空所有对话
  onClearAll() {
    wx.showModal({
      title: '确认清空',
      content: '将删除所有对话记录，此操作不可恢复',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.chat.deleteAllConversations();
            this.setData({
              conversations: [],
              isEmpty: true
            });
            this._swipedId = null;
            wx.showToast({ title: '已全部清空', icon: 'success' });
          } catch (err) {
            wx.showToast({ title: err.message || '操作失败', icon: 'none' });
          }
        }
      }
    });
  }
});
