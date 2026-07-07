const api = require('../../utils/api');
const { CATEGORY_MAP, FILE_TYPE_MAP } = require('../../utils/constants');
const { formatTime } = require('../../utils/util');

Page({
  data: {
    id: null,
    announcement: null,
    loading: true,
    error: '',
    fileTypeLabel: 'FILE'
  },

  onLoad(options) {
    const id = options.id;
    if (!id) {
      this.setData({ error: '参数错误', loading: false });
      return;
    }
    this.setData({ id });
    this.fetchDetail();
  },

  async fetchDetail() {
    try {
      const data = await api.announcement.detail(this.data.id);
      // Determine file type
      let fileTypeLabel = 'FILE';
      if (data && data.fileUrl) {
        const ext = (data.fileUrl || '').split('.').pop().toLowerCase();
        fileTypeLabel = (FILE_TYPE_MAP[ext] || { icon: 'FILE' }).icon;
      }
      this.setData({
        announcement: data,
        loading: false,
        fileTypeLabel
      });
      wx.setNavigationBarTitle({ title: data ? data.title || '公告详情' : '公告详情' });
    } catch (err) {
      this.setData({
        error: err.message || '加载失败',
        loading: false
      });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  getCategoryLabel(cat) {
    return CATEGORY_MAP[cat] ? CATEGORY_MAP[cat].label : cat;
  },

  getTime(isoStr) {
    return formatTime(isoStr);
  },

  downloadFile() {
    const url = this.data.announcement.fileUrl;
    if (!url) {
      wx.showToast({ title: '无附件可下载', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '下载中...' });
    wx.downloadFile({
      url,
      success(res) {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.openDocument({ filePath: res.tempFilePath });
        } else {
          wx.showToast({ title: '下载失败', icon: 'none' });
        }
      },
      fail() {
        wx.hideLoading();
        wx.showToast({ title: '下载失败', icon: 'none' });
      }
    });
  },

  deleteAnnouncement() {
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复，确定要删除这条公告吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.announcement.delete(this.data.id);
            wx.showToast({ title: '删除成功', icon: 'success' });
            setTimeout(() => wx.navigateBack(), 1500);
          } catch (err) {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          }
        }
      }
    });
  }
});
