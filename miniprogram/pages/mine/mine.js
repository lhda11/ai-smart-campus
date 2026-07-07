const app = getApp();
const storage = require('../../utils/storage');

Page({
  data: {
    userName: '同学',
    userAvatar: '',
    studentId: '',
    roleName: '学生',
    applicationCount: 0,
    userTagsPreview: ''
  },

  onShow() {
    const loginState = storage.getLoginState();
    const userInfo = loginState && loginState.userInfo;

    // 从登录信息获取用户名
    let userName = '同学';
    if (userInfo) {
      userName = userInfo.nickname || userInfo.studentId || '同学';
    } else if (app.globalData.userName) {
      userName = app.globalData.userName;
    }
    this.setData({ userName });

    // 设置学号和角色
    let studentId = '';
    let roleName = '学生';
    if (userInfo) {
      studentId = userInfo.studentId || '';
      const role = userInfo.role || 1;
      roleName = role === 1 ? '学生' : role === 2 ? '教职工' : '社团负责人';
    } else if (app.globalData.studentId) {
      studentId = app.globalData.studentId;
      roleName = app.globalData.role === 1 ? '学生' : app.globalData.role === 2 ? '教职工' : '社团负责人';
    }
    this.setData({ studentId, roleName });

    // 加载标签预览
    const tags = storage.getUserTags();
    this.setData({
      userTagsPreview: tags.length > 0 ? tags.slice(0, 3).join(' / ') + (tags.length > 3 ? '...' : '') : '未设置'
    });
  },

  editProfile() {
    wx.showToast({ title: 'Demo版本，暂不支持编辑', icon: 'none' });
  },

  goMyApplications() {
    wx.navigateTo({ url: '/pages/my-applications/my-applications' });
  },

  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/admin' });
  },

  showAbout() {
    wx.showModal({
      title: '关于长春师范大学校园助手',
      content: '长春师范大学AI校园助手 v1.0\n\n基于大模型驱动的校园服务平台，提供智能问答、社团推荐、资料检索等功能。\n\n学校地址：吉林省长春市长吉北线677号\n官网：https://www.ccsfu.edu.cn\n\n后端: SpringBoot + SpringAI\n前端: 微信小程序原生',
      showCancel: false,
      confirmText: '知道了'
    });
  },

  showTagsEdit() {
    wx.navigateTo({ url: '/pages/discover/discover' });
    wx.showToast({ title: '点击标签区域编辑', icon: 'none' });
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      confirmColor: '#E07A3E',
      success: (res) => {
        if (res.confirm) {
          storage.clearLoginState();
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
