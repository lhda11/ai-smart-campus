const api = require('../../utils/api');
const { APPLICATION_STATUS } = require('../../utils/constants');
const { formatTime, maskPhone } = require('../../utils/util');

Page({
  data: {
    applications: [],
    loading: true,
    isEmpty: false
  },

  onShow() {
    this.fetchApplications();
  },

  onPullDownRefresh() {
    this.fetchApplications().then(() => wx.stopPullDownRefresh());
  },

  async fetchApplications() {
    this.setData({ loading: true });

    try {
      const data = await api.apply.myApplications();
      this.setData({
        applications: data || [],
        loading: false,
        isEmpty: !data || data.length === 0
      });
    } catch (err) {
      this.setData({ loading: false, isEmpty: this.data.applications.length === 0 });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  getStatusInfo(status) {
    return APPLICATION_STATUS[status] || { label: '未知', color: '#777777' };
  },

  getTime(isoStr) {
    return formatTime(isoStr);
  },

  getPhoneMasked(phone) {
    return maskPhone(phone);
  },

  goDiscover() {
    wx.switchTab({ url: '/pages/discover/discover' });
  }
});
