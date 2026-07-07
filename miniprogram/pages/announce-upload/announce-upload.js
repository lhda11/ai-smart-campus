const api = require('../../utils/api');

Page({
  data: {
    title: '',
    category: 'notice',
    categoryIndex: 0,
    categories: [
      { value: 'notice', label: '通知', icon: '◈' },
      { value: 'activity', label: '活动', icon: '◆' },
      { value: 'policy', label: '制度', icon: '◎' }
    ],
    filePath: '',
    fileName: '',
    fileSize: '',
    fileExt: 'FILE',
    uploading: false,
    uploadProgress: 0
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value });
  },

  // 卡片式选择分类
  selectCategory(e) {
    const { cat, idx } = e.currentTarget.dataset;
    this.setData({
      category: cat,
      categoryIndex: parseInt(idx)
    });
  },

  chooseFile() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['txt', 'pdf', 'docx'],
      success: (res) => {
        const file = res.tempFiles[0];
        const ext = (file.name || '').split('.').pop().toUpperCase();
        this.setData({
          filePath: file.path,
          fileName: file.name,
          fileSize: this.formatFileSize(file.size),
          fileExt: ext || 'FILE'
        });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '选择文件失败', icon: 'none' });
        }
      }
    });
  },

  async submit() {
    const { title, category, filePath } = this.data;

    if (!title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }
    if (!filePath) {
      wx.showToast({ title: '请选择文件', icon: 'none' });
      return;
    }

    this.setData({ uploading: true, uploadProgress: 0 });

    try {
      await api.announcement.upload(
        title.trim(),
        category,
        filePath,
        (progress) => {
          this.setData({ uploadProgress: progress });
        }
      );
      wx.showToast({ title: '发布成功', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 1500);
    } catch (err) {
      this.setData({ uploading: false });
      wx.showToast({ title: err.message || '发布失败', icon: 'none' });
    }
  },

  formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
});
