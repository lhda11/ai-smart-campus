/**
 * 统一 API 请求层
 * 处理 ApiResult<T> 包络、SSE 流式、文件上传、错误标准化
 */
const { BASE_URL } = require('./constants');
const storage = require('./storage');

// ===== 基础请求 =====
function request(method, path, data, options = {}) {
  const { showLoading = true, loadingText = '加载中...', silent = false } = options;

  if (showLoading) {
    wx.showLoading({ title: loadingText, mask: true });
  }

  return new Promise((resolve, reject) => {
    // 获取 token
    const header = { 'Content-Type': 'application/json' };
    const loginState = storage.getLoginState();
    if (loginState && loginState.userInfo && loginState.userInfo.token) {
      header['Authorization'] = 'Bearer ' + loginState.userInfo.token;
    }

    wx.request({
      url: BASE_URL + path,
      method,
      header,
      data,
      success(res) {
        const body = res.data;
        // 检查 HTTP 200 + ApiResult code 200
        if (res.statusCode === 200 && body && body.code === 200) {
          resolve(body.data);
        } else if (res.statusCode === 401 || (body && body.code === 401)) {
          // Token 过期，清除登录态并跳转登录页
          storage.clearLoginState();
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          setTimeout(() => {
            wx.reLaunch({ url: '/pages/login/login' });
          }, 1500);
          reject({ code: 401, message: '登录已过期' });
        } else if (body && body.code) {
          reject({ code: body.code, message: body.message || '请求失败' });
        } else {
          reject({ code: res.statusCode, message: '服务器响应异常' });
        }
      },
      fail(err) {
        reject({
          code: -1,
          message: '网络连接失败，请检查网络设置和后端服务是否启动'
        });
      },
      complete() {
        if (showLoading) wx.hideLoading();
      }
    });
  });
}

// ===== 便捷方法 =====
function get(path, params) {
  return request('GET', buildUrl(path, params), undefined, { showLoading: false });
}

function post(path, data, options) {
  return request('POST', path, data, options);
}

function del(path) {
  return request('DELETE', path, undefined, { showLoading: false });
}

// ===== 文件上传 =====
function upload(path, { title, category, filePath }, onProgress) {
  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '上传中...', mask: true });

    // 获取 token 用于文件上传
    const header = {};
    const loginState = storage.getLoginState();
    if (loginState && loginState.userInfo && loginState.userInfo.token) {
      header['Authorization'] = 'Bearer ' + loginState.userInfo.token;
    }

    const uploadTask = wx.uploadFile({
      url: BASE_URL + path,
      filePath,
      name: 'file',
      header,
      formData: { title, category },
      success(res) {
        try {
          const body = JSON.parse(res.data);
          if (body.code === 200) {
            resolve(body.data);
          } else {
            reject({ code: body.code || -1, message: body.message || '上传失败' });
          }
        } catch (e) {
          reject({ code: -1, message: '响应解析失败' });
        }
      },
      fail(err) {
        reject({ code: -1, message: '上传失败，请重试' });
      },
      complete() {
        wx.hideLoading();
      }
    });

    if (onProgress) {
      uploadTask.onProgressUpdate((res) => {
        onProgress(res.progress);
      });
    }
  });
}

// ===== SSE 流式请求 =====
function sseRequest(path, data, callbacks) {
  const { onChunk, onDone, onError, onConvId } = callbacks;

  // 获取 token
  const header = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream'
  };
  const loginState = storage.getLoginState();
  if (loginState && loginState.userInfo && loginState.userInfo.token) {
    header['Authorization'] = 'Bearer ' + loginState.userInfo.token;
  }

  const requestTask = wx.request({
    url: BASE_URL + path,
    method: 'POST',
    enableChunked: true,
    header,
    data,
    success(res) {
      // 流式结束
      if (onDone) onDone();
      if (requestTask._convId && onConvId) onConvId(requestTask._convId);
    },
    fail(err) {
      if (onError) onError({ code: -1, message: '连接中断，请重试' });
    }
  });

  // 存储 convId 和缓冲
  requestTask._convId = null;
  let buffer = '';

  requestTask.onChunkReceived((res) => {
    try {
      // 将 ArrayBuffer 转为字符串
      const chunk = arrayBufferToString(res.data);
      buffer += chunk;

      // 按行解析 SSE
      const lines = buffer.split('\n');
      // 最后一个元素可能不完整，保留
      buffer = lines.pop() || '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith(':')) continue; // 注释行或空行

        if (trimmed.startsWith('data:')) {
          const data = trimmed.substring(5).trim();
          if (data === '[DONE]') {
            if (onDone) onDone();
            return;
          }
          // 尝试解析 convId
          try {
            const parsed = JSON.parse(data);
            if (parsed.convId && !requestTask._convId) {
              requestTask._convId = parsed.convId;
            }
          } catch (e) {
            // data 是纯文本 token
          }
          if (onChunk) onChunk(data);
        }
      }
    } catch (e) {
      console.error('SSE parse error:', e);
    }
  });

  // 返回 RequestTask 用于 abort
  return requestTask;
}

/**
 * ArrayBuffer → UTF-8 字符串
 */
function arrayBufferToString(buffer) {
  // 微信返回的可能是 ArrayBuffer
  if (typeof buffer === 'string') return buffer;
  if (buffer instanceof ArrayBuffer) {
    const uint8 = new Uint8Array(buffer);
    let result = '';
    // 分块解码，避免栈溢出
    const chunkSize = 8192;
    for (let i = 0; i < uint8.length; i += chunkSize) {
      const slice = uint8.subarray(i, Math.min(i + chunkSize, uint8.length));
      result += String.fromCharCode.apply(null, slice);
    }
    try {
      return decodeURIComponent(escape(result));
    } catch (e) {
      return result;
    }
  }
  return String(buffer);
}

// ===== 构建查询字符串 =====
function buildUrl(path, params) {
  if (!params) return path;
  const entries = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '');
  if (entries.length === 0) return path;
  const qs = entries
    .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
    .join('&');
  return path + '?' + qs;
}

// ===== 认证 API =====
const auth = {
  /** 学号/工号登录 */
  login(studentId, password) {
    return post('/api/auth/login', { studentId, password }, { silent: true });
  },
  /** 修改密码 */
  changePassword(oldPassword, newPassword) {
    return post('/api/auth/change-password', { oldPassword, newPassword }, { silent: true });
  },
  /** 获取当前用户信息 */
  me() {
    return get('/api/auth/me');
  }
};

// ===== 模块化 API =====

const chat = {
  /** SSE 流式对话 */
  send(message, conversationId, callbacks) {
    return sseRequest('/api/chat/send', { message, conversationId }, callbacks);
  },
  /** 同步对话（测试用） */
  sendTest(message, conversationId) {
    return post('/api/chat/send-test', { message, conversationId });
  },
  /** 带上下文的同步对话 */
  sendSync(message, conversationId) {
    return post('/api/chat/send-sync', { message, conversationId });
  },
  /** RAG 增强问答 */
  sendRAG(message, conversationId) {
    return post('/api/chat/send-rag', { message, conversationId });
  },
  /** 获取会话列表 */
  getConversations() {
    return get('/api/chat/conversations');
  },
  /** 获取消息历史 */
  getMessages(convId) {
    return get('/api/chat/messages', { convId });
  },
  /** 删除会话 */
  deleteConversation(convId) {
    return del('/api/chat/conversations/' + convId);
  },
  /** 一键清空所有对话 */
  deleteAllConversations() {
    return del('/api/chat/conversations');
  }
};

const search = {
  /** 语义搜索 */
  search(keyword) {
    return post('/api/search', { keyword });
  }
};

const club = {
  /** 获取所有社团 */
  listAll() {
    return get('/api/club');
  },
  /** 标签推荐 (Jaccard) */
  recommend(tags) {
    return get('/api/club/recommend', { tags: Array.isArray(tags) ? tags.join(',') : tags });
  },
  /** AI 精排推荐 */
  recommendAI(tags) {
    return get('/api/club/recommend/ai', { tags: Array.isArray(tags) ? tags.join(',') : tags });
  },
  /** 社团详情 */
  detail(id) {
    return get('/api/club/' + id);
  },
  /** 社团纳新活动 */
  recruitments(clubId) {
    return get('/api/club/' + clubId + '/recruitments');
  }
};

const apply = {
  /** 提交报名 */
  submit(data) {
    return post('/api/apply', data);
  },
  /** 我的报名记录 */
  myApplications() {
    return get('/api/apply/my');
  }
};

const announcement = {
  /** 分类列表 */
  list(category = 'notice', page = 1, size = 10) {
    return get('/api/announcements', { category, page, size });
  },
  /** 详情 */
  detail(id) {
    return get('/api/announcements/' + id);
  },
  /** 删除 */
  delete(id) {
    return del('/api/announcements/' + id);
  },
  /** 上传 */
  upload(title, category, filePath, onProgress) {
    return upload('/api/announcements/upload', { title, category, filePath }, onProgress);
  }
};

const admin = {
  /** 重建向量索引 */
  rebuildIndex() {
    return post('/api/admin/rebuild-index', undefined);
  }
};

module.exports = {
  request,
  get,
  post,
  del,
  upload,
  sseRequest,
  auth,
  chat,
  search,
  club,
  apply,
  announcement,
  admin
};
