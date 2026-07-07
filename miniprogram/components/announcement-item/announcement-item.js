const { CATEGORY_MAP } = require('../../utils/constants');
const { formatTime } = require('../../utils/util');

Component({
  properties: {
    item: {
      type: Object,
      value: {}
    },
    showCategory: {
      type: Boolean,
      value: true
    },
    loading: {
      type: Boolean,
      value: false
    }
  },

  computed: {},

  methods: {
    onTap() {
      this.triggerEvent('tap', { id: this.data.item.id });
    },

    getCategoryInfo(category) {
      return CATEGORY_MAP[category] || { label: category, color: 'badge-notice' };
    },

    getTime(isoStr) {
      return formatTime(isoStr);
    }
  }
});
