// ===== API 地址配置 =====
const BASE_URL = 'http://localhost:8088';

// ===== 公告分类映射 =====
const CATEGORY_MAP = {
  notice: { label: '通知', color: 'badge-notice' },
  activity: { label: '活动', color: 'badge-activity' },
  policy: { label: '制度', color: 'badge-policy' }
};

const CATEGORY_TABS = [
  { key: 'notice', label: '通知' },
  { key: 'activity', label: '活动' },
  { key: 'policy', label: '制度' }
];

// ===== 兴趣标签调色板 =====
const INTEREST_TAGS = [
  '科技', '文艺', '体育', '志愿', '学术',
  '创业', '音乐', '舞蹈', '摄影', '电竞',
  '辩论', '环保', '编程', '绘画', '读书'
];

// ===== 快捷问题 =====
const QUICK_QUESTIONS = [
  '图书馆开放时间？',
  '校园卡丢了怎么补办？',
  '怎么打印成绩单？',
  '学校有哪些院系？',
  '宿舍怎么报修？',
  '校园网怎么办理？'
];

// ===== 报名状态映射 =====
const APPLICATION_STATUS = {
  0: { label: '已提交', color: '#E07A3E' },
  1: { label: '已通过', color: '#34C759' },
  2: { label: '未通过', color: '#777777' }
};

// ===== 纳新状态映射 =====
const RECRUITMENT_STATUS = {
  0: { label: '已下架', color: '#777777' },
  1: { label: '进行中', color: '#34C759' },
  2: { label: '已满员', color: '#FF9500' },
  3: { label: '已结束', color: '#777777' }
};

// ===== 图标定义 (Unicode 符号 + 背景色类) =====
const ICON_MAP = {
  chat:      { symbol: '\u{1F4AC}', boxClass: 'icon-box-primary' },
  club:      { symbol: '\u{2663}',  boxClass: 'icon-box-accent' },
  board:     { symbol: '\u{2630}',  boxClass: 'icon-box-orange' },
  search:    { symbol: '\u{2316}',  boxClass: 'icon-box-green' },
  apply:     { symbol: '\u{270E}',  boxClass: 'icon-box-primary' },
  tags:      { symbol: '\u{2606}',  boxClass: 'icon-box-accent' },
  admin:     { symbol: '\u{2699}',  boxClass: 'icon-box-gray' },
  about:     { symbol: '\u{24D8}',  boxClass: 'icon-box-green' },
  upload:    { symbol: '\u{21E7}',  boxClass: 'icon-box-primary' },
  download:  { symbol: '\u{21E9}',  boxClass: 'icon-box-green' },
  file:      { symbol: '\u{1F4C4}', boxClass: 'icon-box-orange' },
  user:      { symbol: '\u{263A}',  boxClass: 'icon-box-primary' },
  time:      { symbol: '\u{25F7}',  boxClass: 'icon-box-gray' },
  location:  { symbol: '\u{27B5}',  boxClass: 'icon-box-orange' },
  phone:     { symbol: '\u{260E}',  boxClass: 'icon-box-green' },
  idcard:    { symbol: '\u{25A3}',  boxClass: 'icon-box-primary' },
  star:      { symbol: '\u{2605}',  boxClass: 'icon-box-orange' },
  heart:     { symbol: '\u{2661}',  boxClass: 'icon-box-red' },
  check:     { symbol: '\u{2713}',  boxClass: 'icon-box-green' },
  close:     { symbol: '\u{2717}',  boxClass: 'icon-box-red' },
  arrow:     { symbol: '\u{203A}',  boxClass: 'icon-box-gray' },
  bullet:    { symbol: '\u{25CF}',  boxClass: '' },
};

// ===== 文件类型图标 =====
const FILE_TYPE_MAP = {
  pdf:  { icon: 'PDF',  color: '#FF3B30', bg: '#FFEBEE' },
  docx: { icon: 'DOC', color: '#E07A3E', bg: '#FCE8D5' },
  doc:  { icon: 'DOC', color: '#E07A3E', bg: '#FCE8D5' },
  txt:  { icon: 'TXT', color: '#8E8E93', bg: '#F5F5F5' },
};

// ===== 热门搜索关键词 =====
const HOT_SEARCHES = [
  '图书馆', '奖学金', '补办校园卡', '宿舍报修',
  '成绩单', '弘毅堂', '校园网', '院系设置'
];

module.exports = {
  BASE_URL,
  CATEGORY_MAP,
  CATEGORY_TABS,
  INTEREST_TAGS,
  QUICK_QUESTIONS,
  APPLICATION_STATUS,
  RECRUITMENT_STATUS,
  ICON_MAP,
  FILE_TYPE_MAP,
  HOT_SEARCHES
};
