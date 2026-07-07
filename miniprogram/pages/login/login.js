const storage = require('../../utils/storage');
const api = require('../../utils/api');

Page({
  data: {
    agreePrivacy: false,
    studentId: '',
    password: '',
    loading: false,
    errorMsg: '',
    showChangePwdForm: false,
    changePwd: {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  },

  onLoad() {
    // 已登录则直接进首页
    if (storage.isLoggedIn()) {
      wx.switchTab({ url: '/pages/home/home' });
    }
  },

  onPrivacyChange(e) {
    this.setData({ agreePrivacy: e.detail.value.length > 0 });
  },

  onStudentIdInput(e) {
    this.setData({ studentId: e.detail.value, errorMsg: '' });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value, errorMsg: '' });
  },

  onOldPwdInput(e) {
    this.setData({ 'changePwd.oldPassword': e.detail.value, errorMsg: '' });
  },

  onNewPwdInput(e) {
    this.setData({ 'changePwd.newPassword': e.detail.value, errorMsg: '' });
  },

  onConfirmPwdInput(e) {
    this.setData({ 'changePwd.confirmPassword': e.detail.value, errorMsg: '' });
  },

  // 学号工号登录
  onAccountLogin() {
    if (!this.validatePrivacy()) return;

    const { studentId, password } = this.data;
    if (!studentId.trim()) {
      this.setData({ errorMsg: '请输入学号/工号' });
      return;
    }
    if (!password.trim()) {
      this.setData({ errorMsg: '请输入密码' });
      return;
    }

    this.setData({ loading: true, errorMsg: '' });

    api.auth.login(studentId.trim(), password.trim())
      .then(res => {
        this.handleLoginSuccess(res);
      })
      .catch(err => {
        this.setData({
          loading: false,
          errorMsg: err.message || '登录失败，请检查学号/工号和密码'
        });
      });
  },

  // 登录成功后处理
  handleLoginSuccess(loginRes) {
    // 存储登录状态
    storage.setLoginState({
      token: loginRes.token,
      userId: loginRes.userId,
      studentId: loginRes.studentId,
      nickname: loginRes.nickname,
      role: loginRes.role
    });

    // 检查是否首次登录，需要修改密码
    if (loginRes.firstLogin) {
      this.setData({
        showChangePwdForm: true,
        errorMsg: '',
        changePwd: { oldPassword: '', newPassword: '', confirmPassword: '' }
      });
      this.setData({ loading: false });
      wx.showToast({ title: '首次登录，请修改密码', icon: 'none', duration: 2000 });
      return;
    }

    wx.showToast({ title: '登录成功', icon: 'success', duration: 800 });
    setTimeout(() => {
      wx.switchTab({ url: '/pages/home/home' });
    }, 900);
  },

  // 首次登录修改密码
  onChangePassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data.changePwd;

    if (!oldPassword.trim()) {
      this.setData({ errorMsg: '请输入原密码' });
      return;
    }
    if (!newPassword.trim() || newPassword.length < 6) {
      this.setData({ errorMsg: '新密码至少6位' });
      return;
    }
    if (newPassword !== confirmPassword) {
      this.setData({ errorMsg: '两次密码输入不一致' });
      return;
    }

    this.setData({ loading: true, errorMsg: '' });

    api.auth.changePassword(oldPassword, newPassword)
      .then(res => {
        // 更新 token
        const state = storage.getLoginState();
        if (state && state.userInfo) {
          state.userInfo.token = res.token;
          storage.setLoginState(state.userInfo);
        }
        wx.showToast({ title: '密码修改成功', icon: 'success', duration: 800 });
        setTimeout(() => {
          wx.switchTab({ url: '/pages/home/home' });
        }, 900);
      })
      .catch(err => {
        this.setData({
          loading: false,
          errorMsg: err.message || '密码修改失败'
        });
      });
  },

  validatePrivacy() {
    if (!this.data.agreePrivacy) {
      wx.showToast({ title: '请先同意用户协议与隐私政策', icon: 'none' });
      return false;
    }
    return true;
  },

  // 用户协议/隐私政策
  openAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '本协议为示例内容，实际项目请替换为正式协议。',
      showCancel: false
    });
  },

  openPrivacy() {
    wx.showModal({
      title: '隐私政策',
      content: '本政策为示例内容，实际项目请替换为正式隐私政策。',
      showCancel: false
    });
  },

  onAvatarTap() {
    wx.showToast({ title: '我是园宝，你的园区助手~', icon: 'none' });
  }
});
