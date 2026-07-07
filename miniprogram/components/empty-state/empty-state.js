Component({
  properties: {
    icon: {
      type: String,
      value: '◈'
    },
    useAvatar: {
      type: Boolean,
      value: false
    },
    text: {
      type: String,
      value: '暂无内容'
    },
    subtitle: {
      type: String,
      value: ''
    },
    buttonText: {
      type: String,
      value: ''
    }
  },

  methods: {
    onAction() {
      this.triggerEvent('action');
    }
  }
});
