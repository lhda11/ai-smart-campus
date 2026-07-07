/**
 * 格式化时间显示
 * 今天 → HH:mm
 * 昨天 → 昨天 HH:mm
 * 今年 → MM-DD HH:mm
 * 其他 → YYYY-MM-DD HH:mm
 */
function formatTime(isoStr) {
  if (!isoStr) return '';
  const date = new Date(isoStr.replace('T', ' ').substring(0, 19));
  const now = new Date();

  const pad = n => n.toString().padStart(2, '0');
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;

  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();

  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday =
    date.getFullYear() === yesterday.getFullYear() &&
    date.getMonth() === yesterday.getMonth() &&
    date.getDate() === yesterday.getDate();

  if (isToday) return time;
  if (isYesterday) return `昨天 ${time}`;

  const dateStr = `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  if (date.getFullYear() === now.getFullYear()) return `${dateStr} ${time}`;
  return `${date.getFullYear()}-${dateStr} ${time}`;
}

/**
 * 防抖
 */
function debounce(fn, delay = 300) {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

/**
 * 手机号中间4位脱敏: 138****5678
 */
function maskPhone(phone) {
  if (!phone || phone.length !== 11) return phone || '';
  return phone.substring(0, 3) + '****' + phone.substring(7);
}

/**
 * 简易Markdown → WXML节点数组
 * 支持: **bold**, *italic*, `code`, 有序列表, 代码块, 换行
 */
function renderMarkdown(text) {
  if (!text) return [{ type: 'text', content: '' }];

  const nodes = [];
  const lines = text.split('\n');
  let inCodeBlock = false;
  let codeBlockContent = '';

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // 代码块
    if (line.trim().startsWith('```')) {
      if (inCodeBlock) {
        nodes.push({ type: 'code-block', content: codeBlockContent });
        codeBlockContent = '';
        inCodeBlock = false;
      } else {
        inCodeBlock = true;
      }
      continue;
    }
    if (inCodeBlock) {
      codeBlockContent += (codeBlockContent ? '\n' : '') + line;
      continue;
    }

    // 空行
    if (line.trim() === '') {
      nodes.push({ type: 'br' });
      continue;
    }

    // 有序列表
    const listMatch = line.match(/^(\d+)[\.\)]\s+(.*)/);
    if (listMatch) {
      nodes.push({ type: 'list-item', content: parseInline(listMatch[2]) });
      continue;
    }

    // 标题
    const hMatch = line.match(/^#{1,3}\s+(.*)/);
    if (hMatch) {
      nodes.push({ type: 'heading', content: hMatch[1] });
      continue;
    }

    // 普通段落
    nodes.push({ type: 'paragraph', content: parseInline(line) });
  }

  // 未闭合的代码块
  if (inCodeBlock && codeBlockContent) {
    nodes.push({ type: 'code-block', content: codeBlockContent });
  }

  // 如果没有节点，返回原文本
  if (nodes.length === 0) {
    nodes.push({ type: 'text', content: text });
  }

  return nodes;
}

/**
 * 解析行内格式: **bold**, *italic*, `code`
 */
function parseInline(text) {
  const segments = [];
  const regex = /(\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`)/g;
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ type: 'text', content: text.substring(lastIndex, match.index) });
    }
    if (match[2]) {
      segments.push({ type: 'bold', content: match[2] });
    } else if (match[3]) {
      segments.push({ type: 'italic', content: match[3] });
    } else if (match[4]) {
      segments.push({ type: 'code', content: match[4] });
    }
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    segments.push({ type: 'text', content: text.substring(lastIndex) });
  }

  return segments.length > 0 ? segments : [{ type: 'text', content: text }];
}

module.exports = {
  formatTime,
  debounce,
  maskPhone,
  renderMarkdown
};
