const api = require('../../utils/api');
const validator = require('../../utils/validator');

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    },
    recruitmentId: {
      type: Number,
      value: null
    },
    clubName: {
      type: String,
      value: ''
    }
  },

  data: {
    form: {
      name: '',
      studentId: '',
      phone: ''
    },
    errors: {},
    submitting: false
  },

  observers: {
    'visible'(val) {
      if (val) {
        // 重置表单
        this.setData({
          form: { name: '', studentId: '', phone: '' },
          errors: {},
          submitting: false
        });
      }
    }
  },

  methods: {
    onInput(e) {
      const { field } = e.currentTarget.dataset;
      const { value } = e.detail;
      this.setData({
        ['form.' + field]: value
      });
      // 实时清除该字段错误
      if (this.data.errors[field]) {
        const errors = { ...this.data.errors };
        delete errors[field];
        this.setData({ errors });
      }
    },

    onClose() {
      if (this.data.submitting) return;
      this.triggerEvent('close');
    },

    preventBubble() {
      // 阻止冒泡，防止点击内容区关闭
    },

    async onSubmit() {
      // 前端校验
      const errors = validator.applyForm(this.data.form);
      if (Object.keys(errors).length > 0) {
        this.setData({ errors });
        return;
      }

      this.setData({ submitting: true });

      try {
        await api.apply.submit({
          recruitmentId: this.data.recruitmentId,
          name: this.data.form.name,
          studentId: this.data.form.studentId,
          phone: this.data.form.phone
        });
        wx.showToast({ title: '报名成功！', icon: 'success' });
        this.triggerEvent('succeed');
      } catch (err) {
        // 后端校验错误 (code=400)
        if (err.code === 400) {
          const serverErrors = validator.parseServerErrors(err.message);
          if (Object.keys(serverErrors).length > 0) {
            this.setData({ errors: serverErrors });
          } else {
            wx.showToast({ title: err.message, icon: 'none' });
          }
        } else {
          wx.showToast({ title: err.message || '报名失败', icon: 'none' });
        }
      } finally {
        this.setData({ submitting: false });
      }
    }
  }
});
