-- ============================================================
-- AI智慧园区 - 数据库迁移脚本 v1→v2
-- 将旧 t_user 表（基于 openid）迁移到新表（基于 student_id）
-- ============================================================

-- 1. 先备份旧数据
CREATE TABLE IF NOT EXISTS t_user_backup AS SELECT * FROM t_user;

-- 2. 删旧表重建（如果数据不重要）
-- DROP TABLE IF EXISTS t_user;

-- 或：增量迁移（保留现有数据）
-- 新增列
ALTER TABLE t_user
  ADD COLUMN student_id VARCHAR(32) UNIQUE COMMENT '学号/工号' AFTER id,
  ADD COLUMN password VARCHAR(256) COMMENT '密码(BCrypt)' AFTER student_id,
  ADD COLUMN first_login TINYINT DEFAULT 1 COMMENT '是否首次登录 1:是 0:否' AFTER password,
  MODIFY COLUMN openid VARCHAR(128) UNIQUE COMMENT '微信openid（保留字段）';

-- 给现有用户设置默认学号和密码（密码为 123456 的 BCrypt）
UPDATE t_user SET
  student_id = CONCAT('S', LPAD(id, 5, '0')),
  password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  first_login = 1
WHERE student_id IS NULL;

-- 添加 NOT NULL 和 INDEX
ALTER TABLE t_user
  MODIFY student_id VARCHAR(32) NOT NULL,
  MODIFY password VARCHAR(256) NOT NULL,
  ADD INDEX idx_student_id (student_id);
