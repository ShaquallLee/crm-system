-- CRM v2 生产化升级脚本（在已有 crm 库上执行一次）
-- 执行方式：mysql -u root -p crm < backend/sql/upgrade_v2.sql
USE crm;

-- 1. 用户：直属上级、登录失败锁定
ALTER TABLE sys_user
  ADD COLUMN manager_id BIGINT NULL COMMENT '直属上级ID' AFTER role_id,
  ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数' AFTER status,
  ADD COLUMN locked_until DATETIME NULL COMMENT '锁定截止时间' AFTER login_fail_count,
  ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'Token 版本，改密/重置后自增使旧 Token 失效' AFTER locked_until;

ALTER TABLE sys_user ADD INDEX idx_manager (manager_id);

-- 2. Token 黑名单（登出后立即失效）
CREATE TABLE IF NOT EXISTS token_blacklist (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  token_hash CHAR(64) NOT NULL UNIQUE COMMENT 'Token SHA-256',
  expire_time DATETIME NOT NULL COMMENT 'Token 过期时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token 黑名单';

-- 3. 登录日志
CREATE TABLE IF NOT EXISTS login_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username VARCHAR(64) COMMENT '登录名',
  user_id BIGINT COMMENT '用户ID',
  ip VARCHAR(64) COMMENT 'IP',
  user_agent VARCHAR(255) COMMENT 'User-Agent',
  success TINYINT NOT NULL COMMENT '是否成功 1是 0否',
  message VARCHAR(255) COMMENT '结果说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_login_time (create_time),
  INDEX idx_login_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- 4. 业务操作审计日志
CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT COMMENT '操作人ID',
  username VARCHAR(64) COMMENT '操作人',
  module VARCHAR(64) COMMENT '模块',
  action VARCHAR(64) COMMENT '动作',
  method VARCHAR(8) COMMENT 'HTTP 方法',
  uri VARCHAR(255) COMMENT '请求路径',
  ip VARCHAR(64) COMMENT 'IP',
  params TEXT COMMENT '请求参数（已脱敏）',
  success TINYINT NOT NULL COMMENT '是否成功 1是 0否',
  error_msg VARCHAR(500) COMMENT '失败原因',
  cost_ms BIGINT COMMENT '耗时毫秒',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_audit_time (create_time),
  INDEX idx_audit_user (user_id),
  INDEX idx_audit_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
