/**
 * 表单校验工具
 */
const validator = {
  /**
   * 校验手机号 (中国)
   */
  phone(value) {
    if (!value) return '请输入手机号';
    if (!/^1[3-9]\d{9}$/.test(value)) return '手机号格式不正确';
    return '';
  },

  /**
   * 校验非空
   */
  required(value, label) {
    if (!value || (typeof value === 'string' && value.trim() === '')) {
      return `${label || '此项'}不能为空`;
    }
    return '';
  },

  /**
   * 校验长度
   */
  length(value, label, min, max) {
    const len = (value || '').length;
    if (min && len < min) return `${label || '此项'}最少${min}个字符`;
    if (max && len > max) return `${label || '此项'}最多${max}个字符`;
    return '';
  },

  /**
   * 校验学号 (非空 + 数字)
   */
  studentId(value) {
    if (!value) return '请输入学号';
    if (!/^\d+$/.test(value)) return '学号格式不正确';
    return '';
  },

  /**
   * 校验报名表单
   */
  applyForm(data) {
    const errors = {};
    const nameErr = this.required(data.name, '姓名') || this.length(data.name, '姓名', 2, 50);
    if (nameErr) errors.name = nameErr;

    const sidErr = this.studentId(data.studentId);
    if (sidErr) errors.studentId = sidErr;

    const phoneErr = this.phone(data.phone);
    if (phoneErr) errors.phone = phoneErr;

    return errors;
  },

  /**
   * 解析后端返回的校验错误 (分号分隔的 "field: message" 格式)
   */
  parseServerErrors(message) {
    if (!message) return {};
    const errors = {};
    message.split(';').forEach(part => {
      const trimmed = part.trim();
      const colonIdx = trimmed.indexOf(':');
      if (colonIdx > 0) {
        const field = trimmed.substring(0, colonIdx).trim();
        const msg = trimmed.substring(colonIdx + 1).trim();
        errors[field] = msg;
      } else if (trimmed) {
        errors._general = trimmed;
      }
    });
    return errors;
  }
};

module.exports = validator;
