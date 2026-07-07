const { renderMarkdown } = require('../../utils/util');

Component({
  properties: {
    role: {
      type: String,
      value: 'user' // 'user' | 'assistant' | 'system'
    },
    content: {
      type: String,
      value: ''
    },
    isStreaming: {
      type: Boolean,
      value: false
    },
    createdAt: {
      type: String,
      value: ''
    },
    msgId: {
      type: String,
      value: ''
    }
  },

  data: {
    nodes: []
  },

  observers: {
    'content'(val) {
      // 将内容转为 WXML 渲染节点
      const nodes = renderMarkdown(val || '');
      this.setData({ nodes });
    }
  },

  lifetimes: {
    attached() {
      const nodes = renderMarkdown(this.data.content || '');
      this.setData({ nodes });
    }
  },

  methods: {
    // 复制本条消息内容
    onCopy() {
      wx.setClipboardData({
        data: this.data.content || '',
        success: () => {
          wx.showToast({ title: '已复制', icon: 'success' });
        }
      });
    }
  }
});
