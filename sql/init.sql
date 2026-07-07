-- ============================================================
-- AI智慧园区 - 数据库初始化脚本
-- Database: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_smart_campus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_smart_campus;

-- 用户表 (学号工号登录)
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid          VARCHAR(128) UNIQUE COMMENT '微信openid（保留字段）',
    student_id      VARCHAR(32) NOT NULL UNIQUE COMMENT '学号/工号',
    password        VARCHAR(256) NOT NULL COMMENT '密码(BCrypt)',
    first_login     TINYINT DEFAULT 1 COMMENT '是否首次登录 1:是 0:否',
    nickname        VARCHAR(64) COMMENT '用户昵称',
    avatar_url      VARCHAR(512) COMMENT '头像URL',
    interest_tags   VARCHAR(512) COMMENT '兴趣标签(JSON数组)',
    role            TINYINT DEFAULT 1 COMMENT '1:学生 2:教职工 3:社团负责人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id)
) ENGINE=InnoDB COMMENT '用户表';

-- 公告/文档表
CREATE TABLE IF NOT EXISTS t_announcement (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(256) NOT NULL COMMENT '标题',
    content         TEXT NOT NULL COMMENT '正文',
    summary         VARCHAR(512) COMMENT '摘要(AI生成)',
    category        VARCHAR(32) NOT NULL COMMENT '分类: notice/activity/policy',
    file_url        VARCHAR(512) COMMENT '附件地址(MinIO)',
    vector_id       VARCHAR(128) COMMENT 'Redis向量文档ID',
    status          TINYINT DEFAULT 1 COMMENT '0:已删除 1:正常',
    author_id       BIGINT NOT NULL COMMENT '发布者ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_status (category, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB COMMENT '公告/文档表';

-- 社团表
CREATE TABLE IF NOT EXISTS t_club (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(128) NOT NULL COMMENT '社团名称',
    description     TEXT COMMENT '社团介绍',
    tags            VARCHAR(512) COMMENT '标签(JSON数组): 科技/文艺/体育/志愿...',
    logo_url        VARCHAR(512) COMMENT '社团Logo(MinIO)',
    member_count    INT DEFAULT 0 COMMENT '当前成员数',
    status          TINYINT DEFAULT 1 COMMENT '0:已注销 1:正常',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT '社团表';

-- 纳新活动表
CREATE TABLE IF NOT EXISTS t_recruitment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_id         BIGINT NOT NULL COMMENT '社团ID',
    title           VARCHAR(256) NOT NULL COMMENT '纳新主题',
    requirement     TEXT COMMENT '纳新要求',
    quota           INT NOT NULL DEFAULT 0 COMMENT '名额',
    enrolled_count  INT DEFAULT 0 COMMENT '已报名人数',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间',
    status          TINYINT DEFAULT 1 COMMENT '0:下架 1:进行中 2:已满员 3:已结束',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_club_status (club_id, status),
    INDEX idx_status_time (status, end_time)
) ENGINE=InnoDB COMMENT '纳新活动表';

-- 报名记录表
CREATE TABLE IF NOT EXISTS t_application (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    recruitment_id  BIGINT NOT NULL COMMENT '纳新活动ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    name            VARCHAR(64) NOT NULL COMMENT '姓名',
    student_id      VARCHAR(32) NOT NULL COMMENT '学号',
    phone           VARCHAR(20) NOT NULL COMMENT '手机号',
    status          TINYINT DEFAULT 0 COMMENT '0:已提交 1:已通过 2:未通过',
    remark          VARCHAR(256) COMMENT '备注',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_recruitment (user_id, recruitment_id)
) ENGINE=InnoDB COMMENT '报名记录表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS t_conversation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    title           VARCHAR(128) DEFAULT '新的对话' COMMENT '会话标题',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, updated_at DESC)
) ENGINE=InnoDB COMMENT '对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role            VARCHAR(16) NOT NULL COMMENT '角色: user/assistant/system',
    content         TEXT NOT NULL COMMENT '消息内容',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id, created_at)
) ENGINE=InnoDB COMMENT '对话消息表';

-- ============================================================
-- 知识库表（校园设施、办事流程、院系介绍等静态知识）
-- ============================================================
CREATE TABLE IF NOT EXISTS t_knowledge (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    category        VARCHAR(32) NOT NULL COMMENT '分类: facility/procedure/department',
    name            VARCHAR(128) NOT NULL COMMENT '条目名称',
    content         TEXT NOT NULL COMMENT '条目内容',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1 COMMENT '0:禁用 1:启用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category, status),
    UNIQUE KEY uk_category_name (category, name)
) ENGINE=InnoDB COMMENT '知识库表';

-- ============================================================
-- 知识库初始数据（与校园知识库文档同步）
-- ============================================================
INSERT INTO t_knowledge (category, name, content, sort_order) VALUES

-- 一、校园设施
('facility', '图书馆',
 '长春师范大学图书馆由新馆和旧馆组成，共26644平方米，4615个阅览座位。\n新馆在主楼后方，共6层；旧馆在外语楼广场附近，共4层。\n馆藏纸质图书207.93万册，电子图书41.7万余种。\n阅览室实行藏借阅一体化管理，周开放时间98小时。\n新馆取消入馆预约，凭校园卡直接刷卡入馆；旧馆各自修室（考研专座除外）启用座位管理系统，可通过网上预约或现场预约机预约座位。\n移动端可通过微信公众号长师在线获取图书馆服务。\n图书馆电话：86168513（办公室）、86168160（办公室）。', 1),

('facility', '弘毅堂',
 '弘毅堂是主校区最大的食堂，共5层（地下一层为库房，地上四层）。\n一至三层为学生餐厅，四层为教职工餐厅。\n午餐和晚餐免费提供汤品，有设立清真窗口。', 2),

('facility', '敦品阁',
 '敦品阁是主校区主要食堂之一，共2层。', 3),

('facility', '体育馆',
 '体育馆位于主校区，体育学院电话0431-86168070。\n校内体育设施包括：室内体育馆1个、乒乓球馆、健身中心、武道馆、舞蹈馆、体能训练实验室、\n400米标准田径场、2个11人制人工草坪足球场、灯光综合运动场、\n14个室外灯光篮球场、6个网球场、4个排球场、2个沙滩排球场、4个气排球场。', 4),

('facility', '校医院',
 '校医院位于主校区，24小时急诊，提供基本医疗服务。门诊时间请关注校内通知。\n校医院急诊电话：86168120，预防保健室：86168475。', 5),

('facility', '快递站',
 '主校区快递分布在多地：中通快递在弘毅堂右侧靠近栅栏；极兔、邮政位于5舍美食城二楼；\n京东、韵达位于七舍外围；申通位于六舍楼下。\n寄件地址：吉林省长春市二道区长吉北路677号长春师范大学。\n乐群校区菜鸟驿站位于食堂1楼。', 6),

-- 二、办事流程
('procedure', '校园卡补办',
 '校园卡补办流程：\n① 丢卡后立即挂失，可拨打语音服务电话0431-86168456挂失，或在校内自助圈存机、登录http://card.ccsfu.edu.cn挂失\n② 如已挂失，需先通过圈存机或上述网站解挂\n③ 确保余额≥20元（可通过掌上校园App充值），到自助补卡机办理，自动扣费20元立等可取\n④ 自助补卡机位置：主校区三食堂1楼；乐群校区食堂2楼半\n⑤ 教职工需带身份证到理科实验楼3楼一卡通管理办公室办理\n校园卡服务中心电话：0431-86168090，办公时间8:00-11:00、13:30-16:00。', 1),

('procedure', '成绩单打印',
 '成绩单打印流程（校内）：\n① 登录学校门户网站\n② 点击"成绩单"图标进入电子成绩单系统\n③ 选择"成绩单（教务）"类型\n④ 等待生成后滚动到底部，点击"电子档下载"\n⑤ A4纸彩色打印，右上角二维码可扫码验证真伪。\n如有出国等需要纸质盖章成绩单：\n① 到学院办公室开具在校证明\n② 学院盖章\n③ 到综合楼教务处学籍科（1016室）加盖公章。', 2),

('procedure', '宿舍报修',
 '宿舍报修流程：\n① 先联系宿舍楼管理员登记\n② 管理员上报后勤服务处\n③ 维修人员上门处理。\n紧急报修可拨打后勤服务处报修热线：0431-86168900\n动力保障科（水电）：0431-86168565\n工程维修科：0431-86168932、86168173、86168569\n也可通过"今日校园"App或企业微信在线报修。', 3),

('procedure', '学生证补办',
 '学生证补办流程：\n① 准备1张一寸蓝底照片\n② 从辅导员处领取补办申请表，辅导员签字\n③ 到学院办公室加盖学院公章\n④ 每周四下午到综合楼1016室（学生工作处学籍管理科）办理。', 4),

('procedure', '校园网办理',
 '校园网办理：\n需先办理校园网开户，开通后获得账号密码方可使用。\n费用30元/月，1月、2月、7月、8月免费，全年最高200元。\n采用"先付费后使用"模式，学生公寓内可使用有线和"CCSFU-Stu"无线网络。\n教学楼和公共区域有"CCSFU"免费WiFi（教师使用）。\n缴费通过长春师范大学学生自助缴费平台进行。\n网络中心位于理科实验楼3楼（101室），电话0431-86168112。\n网络故障报修：0431-86168913、86168700。\n信息化中心用户服务中心电话：0431-86168090、86168858。', 5),

-- 三、院系设置
('department', '文学院',
 '文学院：开设汉语言文学、汉语国际教育专业。', 1),

('department', '外国语学院',
 '外国语学院：开设英语、日语、韩语、俄语、西班牙语、法语、德语、阿拉伯语等专业。', 2),

('department', '经济管理与法学院',
 '经济管理与法学院（原政法学院与经济管理学院合并）：开设会计学、经济与金融专业。', 3),

('department', '历史文化学院',
 '历史文化学院：开设历史学、人文教育、文物与博物馆学专业。', 4),

('department', '教育学院',
 '教育学院：开设小学教育、学前教育、应用心理学、教育技术学专业。', 5),

('department', '科学教育学院、国际教师教育学院',
 '科学教育学院、国际教师教育学院：培养科学教育类及国际教师教育人才。', 6),

('department', '数学学院',
 '数学学院：开设数学与应用数学、数据科学与大数据技术专业。', 7),

('department', '物理学院',
 '物理学院：开设物理学、应用物理学、科学教育专业。', 8),

('department', '化学学院',
 '化学学院：开设化学、环境科学专业。', 9),

('department', '生命科学学院',
 '生命科学学院：开设生物科学、生物技术专业。', 10),

('department', '人工智能学院',
 '人工智能学院（原计算机科学与技术学院（软件学院））：开设计算机科学与技术、人工智能（校企合作）等专业。', 11),

('department', '地理科学学院',
 '地理科学学院：开设地理科学、地理信息科学、人文地理与城乡规划专业。', 12),

('department', '体育与健康学院',
 '体育与健康学院（原体育学院）：开设体育教育、运动训练专业。', 13),

('department', '音乐学院',
 '音乐学院：开设音乐学、舞蹈学专业。', 14),

('department', '美术学院',
 '美术学院：开设美术学、书法学、环境设计、数字媒体艺术专业。', 15),

('department', '传媒学院',
 '传媒学院：开设广告学、播音与主持艺术、广播电视编导专业。', 16),

('department', '马克思主义学院',
 '马克思主义学院：负责全校思想政治理论课教学。', 17),

('department', '工程学院',
 '工程学院：开设机械设计制造及其自动化、车辆工程、智能制造工程专业。', 18),

('department', '文旅与电影学院',
 '文旅与电影学院：负责文化旅游与电影相关专业教学。', 19);
