const api = require('../../utils/api');
const { RECRUITMENT_STATUS } = require('../../utils/constants');
const { formatTime } = require('../../utils/util');

Page({
  data: {
    clubId: null,
    club: null,
    recruitments: [],
    loading: true,
    // 报名弹窗
    applyModalVisible: false,
    applyRecruitmentId: null,
    applyClubName: ''
  },

  onLoad(options) {
    const id = options.id;
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' });
      return;
    }
    this.setData({ clubId: id });
    this.fetchData();
  },

  async fetchData() {
    try {
      const [club, recruitments] = await Promise.all([
        api.club.detail(this.data.clubId),
        api.club.recruitments(this.data.clubId)
      ]);
      this.setData({
        club,
        recruitments: recruitments || [],
        loading: false
      });
      wx.setNavigationBarTitle({ title: club.name || '社团详情' });
    } catch (err) {
      this.setData({ loading: false });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    }
  },

  getTags(tagsStr) {
    if (!tagsStr) return [];
    try {
      const arr = JSON.parse(tagsStr);
      return Array.isArray(arr) ? arr : [];
    } catch (e) {
      return tagsStr.split(',').map(t => t.trim()).filter(Boolean);
    }
  },

  getRecruitStatus(status) {
    return RECRUITMENT_STATUS[status] || { label: '未知', color: '#777777' };
  },

  getTime(isoStr) {
    return formatTime(isoStr);
  },

  // 打开报名弹窗
  openApply(e) {
    const { recruitmentId, quota, enrolled } = e.currentTarget.dataset;
    const clubName = this.data.club ? this.data.club.name : '';

    // 检查是否可报名
    if (enrolled >= quota) {
      wx.showToast({ title: '名额已满', icon: 'none' });
      return;
    }

    this.setData({
      applyModalVisible: true,
      applyRecruitmentId: recruitmentId,
      applyClubName: clubName
    });
  },

  onApplyClose() {
    this.setData({ applyModalVisible: false });
  },

  onApplySucceed() {
    this.setData({ applyModalVisible: false });
    this.fetchData(); // 刷新纳新数据
  }
});
