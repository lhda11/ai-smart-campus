Component({
  properties: {
    placeholder: {
      type: String,
      value: '搜索公告、文档...'
    },
    value: {
      type: String,
      value: ''
    },
    autoFocus: {
      type: Boolean,
      value: true
    }
  },

  data: {
    inputValue: ''
  },

  lifetimes: {
    attached() {
      this.setData({ inputValue: this.data.value });
    }
  },

  observers: {
    'value'(val) {
      if (val !== this.data.inputValue) {
        this.setData({ inputValue: val });
      }
    }
  },

  methods: {
    onInput(e) {
      const value = e.detail.value;
      this.setData({ inputValue: value });
      this.triggerEvent('input', { value });
    },

    onConfirm(e) {
      const value = e.detail.value.trim();
      if (value) {
        this.triggerEvent('search', { value });
      }
    },

    onClear() {
      this.setData({ inputValue: '' });
      this.triggerEvent('clear');
    },

    onCancel() {
      this.triggerEvent('cancel');
    },

    focus() {
      // 由父页面调用
    }
  }
});
