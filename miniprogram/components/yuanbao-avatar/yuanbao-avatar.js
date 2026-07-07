Component({
  properties: {
    size: {
      type: String,
      value: 'md'
    },
    expression: {
      type: String,
      value: 'normal'
    },
    animated: {
      type: Boolean,
      value: true
    },
    variant: {
      type: String,
      value: 'cat'
    }
  },

  data: {
    innerExpression: 'normal',
    isBouncing: false
  },

  lifetimes: {
    attached() {
      this.setData({ innerExpression: this.properties.expression });
      if (this.properties.animated) {
        this.startBlinkLoop();
      }
    },

    detached() {
      if (this._blinkTimer) {
        clearTimeout(this._blinkTimer);
      }
    }
  },

  observers: {
    'expression': function(val) {
      this.setData({ innerExpression: val });
    }
  },

  methods: {
    startBlinkLoop() {
      const loop = () => {
        const delay = 2000 + Math.random() * 2500;
        this._blinkTimer = setTimeout(() => {
          this.setData({ innerExpression: 'blink' });
          setTimeout(() => {
            this.setData({ innerExpression: this.properties.expression });
            loop();
          }, 180);
        }, delay);
      };
      loop();
    },

    onTap() {
      if (this.data.isBouncing) return;
      this.setData({ isBouncing: true, innerExpression: 'happy' });
      setTimeout(() => {
        this.setData({ isBouncing: false, innerExpression: this.properties.expression });
      }, 650);
      this.triggerEvent('tap');
    }
  }
});
