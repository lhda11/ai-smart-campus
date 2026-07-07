const api = require('../../utils/api');

Page({
  data: {
    rebuilding: false,
    rebuildResult: ''
  },

  async rebuildIndex() {
    wx.showModal({
      title: '确认重建',
      content: '此操作将重新向量化所有文档并重建索引，可能需要一段时间。确定继续吗？',
      success: async (res) => {
        if (res.confirm) {
          this.doRebuild();
        }
      }
    });
  },

  async doRebuild() {
    this.setData({ rebuilding: true, rebuildResult: '' });

    try {
      const result = await api.admin.rebuildIndex();
      this.setData({
        rebuilding: false,
        rebuildResult: typeof result === 'string' ? result : '重建完成'
      });
      wx.showToast({ title: '重建成功', icon: 'success' });
    } catch (err) {
      this.setData({
        rebuilding: false,
        rebuildResult: '重建失败: ' + (err.message || '未知错误')
      });
      wx.showToast({ title: err.message || '重建失败', icon: 'none' });
    }
  }
});
