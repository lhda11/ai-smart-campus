Component({
  properties: {
    club: {
      type: Object,
      value: {}
    },
    showRecruitmentBadge: {
      type: Boolean,
      value: true
    }
  },

  methods: {
    onTap() {
      this.triggerEvent('tap', { id: this.data.club.id });
    },

    getTags(tagsStr) {
      if (!tagsStr) return [];
      try {
        const arr = JSON.parse(tagsStr);
        return Array.isArray(arr) ? arr : [];
      } catch (e) {
        return tagsStr.split(',').map(t => t.trim()).filter(Boolean);
      }
    }
  }
});
