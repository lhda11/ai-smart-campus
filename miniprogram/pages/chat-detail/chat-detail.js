const api = require('../../utils/api');
const { QUICK_QUESTIONS } = require('../../utils/constants');
const app = getApp();

Page({
  data: {
    convId: null,
    messages: [],
    inputValue: '',
    isGenerating: false,
    scrollToView: '',
    keyboardHeight: 0,
    safeBottom: 0,
    showQuickQuestions: false,
    showScrollToBottom: false
  },

  _sseTask: null,
  _throttleTimer: null,
  _pendingMessages: null,

  onLoad(options) {
    this.convId = options.convId ? parseInt(options.convId) : null;
    // 解码 URL 参数中的快捷问题（微信 options 不一定自动解码）
    let question = options.question || '';
    try {
      if (question.indexOf('%') !== -1) {
        question = decodeURIComponent(question);
      }
    } catch (e) { /* ignore if already decoded */ }

    this.setData({
      convId: this.convId,
      showQuickQuestions: !this.convId,
      safeBottom: wx.getSystemInfoSync().safeArea ? wx.getSystemInfoSync().screenHeight - wx.getSystemInfoSync().safeArea.bottom : 0
    });

    if (this.convId) {
      this.loadMessages();
    }

    if (question) {
      setTimeout(() => this.sendMessage(question), 500);
    }
  },

  onUnload() {
    this.abortSSE();
    if (this._throttleTimer) {
      clearTimeout(this._throttleTimer);
    }
  },

  onHide() {
    this.abortSSE();
  },

  async loadMessages() {
    try {
      const messages = await api.chat.getMessages(this.convId);
      this.setData({
        messages: (messages || []).map(m => ({
          ...m,
          _isStreaming: false
        })),
        showQuickQuestions: !messages || messages.length === 0
      });
      this.scrollToBottom();
    } catch (err) {
      wx.showToast({ title: '加载消息失败', icon: 'none' });
    }
  },

  async sendMessage(text) {
    const message = text || this.data.inputValue.trim();
    if (!message || this.data.isGenerating) return;

    const userMsg = {
      id: Date.now(),
      conversationId: this.convId,
      role: 'user',
      content: message,
      createdAt: new Date().toISOString()
    };

    const aiMsg = {
      id: Date.now() + 1,
      conversationId: this.convId,
      role: 'assistant',
      content: '',
      _isStreaming: true
    };

    const messages = [...this.data.messages, userMsg, aiMsg];
    this.setData({
      messages,
      inputValue: '',
      showQuickQuestions: false,
      isGenerating: true
    });
    this.scrollToBottom();

    this._sseTask = api.chat.send(message, this.convId, {
      onChunk: (chunk) => {
        this.handleChunk(chunk);
      },
      onDone: () => {
        this.handleDone();
      },
      onError: (err) => {
        this.handleError(err);
      },
      onConvId: (convId) => {
        if (!this.convId && convId) {
          this.convId = convId;
          this.setData({ convId });
        }
      }
    });
  },

  handleChunk(chunk) {
    let text = chunk;
    try {
      const parsed = JSON.parse(chunk);
      if (parsed.content !== undefined) {
        text = parsed.content;
      }
      if (parsed.convId && !this.convId) {
        this.convId = parsed.convId;
      }
    } catch (e) {}

    const lastIdx = this.data.messages.length - 1;
    if (lastIdx < 0) return;

    if (!this._pendingMessages) {
      this._pendingMessages = [...this.data.messages];
    }
    const currentContent = this._pendingMessages[lastIdx].content || '';
    this._pendingMessages[lastIdx] = {
      ...this._pendingMessages[lastIdx],
      content: currentContent + text
    };

    if (!this._throttleTimer) {
      this._throttleTimer = setTimeout(() => {
        this.setData({ messages: this._pendingMessages });
        this.scrollToBottom();
        this._pendingMessages = null;
        this._throttleTimer = null;
      }, 80);
    }
  },

  handleDone() {
    if (this._throttleTimer) {
      clearTimeout(this._throttleTimer);
      this._throttleTimer = null;
    }
    if (this._pendingMessages) {
      this.setData({ messages: this._pendingMessages });
      this._pendingMessages = null;
    }

    const messages = [...this.data.messages];
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i]._isStreaming) {
        messages[i] = { ...messages[i], _isStreaming: false };
        break;
      }
    }

    if (this.convId && messages.length > 0) {
      messages.forEach(m => { m.conversationId = this.convId; });
    }

    this.setData({
      messages,
      isGenerating: false,
      convId: this.convId
    });
    this.scrollToBottom();
  },

  handleError(err) {
    this.abortSSE();

    const messages = [...this.data.messages];
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i]._isStreaming) {
        messages[i] = {
          ...messages[i],
          content: '抱歉，AI 服务暂时不可用，请稍后重试。',
          role: 'system',
          _isStreaming: false
        };
        break;
      }
    }

    this.setData({ messages, isGenerating: false });
    wx.showToast({ title: err.message || '连接中断', icon: 'none' });
  },

  abortSSE() {
    if (this._sseTask) {
      try { this._sseTask.abort(); } catch (e) {}
      this._sseTask = null;
    }
    this.setData({ isGenerating: false });
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  onConfirm() {
    this.sendMessage();
  },

  onSend() {
    this.sendMessage();
  },

  tapQuickQuestion(e) {
    const question = e.currentTarget.dataset.question;
    this.sendMessage(question);
  },

  // 滚动监听 - 显示/隐藏"回到底部"按钮
  onScroll(e) {
    // 微信小程序scroll-view的scroll事件返回scrollTop等信息
    if (e.detail) {
      const scrollTop = e.detail.scrollTop || 0;
      // 当滚动超过一屏时显示按钮
      this.setData({ showScrollToBottom: scrollTop < -100 || scrollTop > 400 });
    }
  },

  scrollToBottom() {
    this.setData({
      scrollToView: 'msg-' + (this.data.messages.length - 1),
      showScrollToBottom: false
    });
  },

  scrollToBottomForce() {
    this.scrollToBottom();
  },

  touchMode(e) {
    wx.showToast({ title: '当前使用 SSE 模式', icon: 'none' });
  },

  // 删除当前对话
  onDeleteConversation() {
    wx.showModal({
      title: '确认删除',
      content: '删除后当前对话记录不可恢复',
      success: async (res) => {
        if (res.confirm) {
          try {
            if (this.convId) {
              await api.chat.deleteConversation(this.convId);
            }
            wx.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => {
              wx.navigateBack();
            }, 300);
          } catch (err) {
            wx.showToast({ title: err.message || '删除失败', icon: 'none' });
          }
        }
      }
    });
  },

  // 复制最新一条助手回复
  onCopyLatest() {
    const msgs = this.data.messages;
    // 从后往前找第一条助手回复
    let content = '';
    for (let i = msgs.length - 1; i >= 0; i--) {
      if (msgs[i].role === 'assistant' && msgs[i].content) {
        content = msgs[i].content;
        break;
      }
    }
    if (!content) {
      wx.showToast({ title: '没有可复制的内容', icon: 'none' });
      return;
    }
    wx.setClipboardData({
      data: content,
      success: () => {
        wx.showToast({ title: '已复制到剪贴板', icon: 'success' });
      }
    });
  }
});
