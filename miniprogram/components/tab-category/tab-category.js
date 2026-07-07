Component({
  properties: {
    tabs: {
      type: Array,
      value: []
    },
    activeKey: {
      type: String,
      value: ''
    }
  },

  methods: {
    onTap(e) {
      const key = e.currentTarget.dataset.key;
      if (key !== this.data.activeKey) {
        this.triggerEvent('change', { key });
      }
    }
  }
});
