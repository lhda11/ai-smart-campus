Component({
  properties: {
    status: {
      type: String,
      value: 'loading' // 'loading' | 'noMore' | 'error' | 'hidden'
    },
    loadingText: {
      type: String,
      value: '正在加载...'
    },
    noMoreText: {
      type: String,
      value: '— 没有更多了 —'
    },
    errorText: {
      type: String,
      value: '加载失败，点击重试'
    }
  },

  methods: {
    onRetry() {
      this.triggerEvent('retry');
    }
  }
});
