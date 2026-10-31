-- CRM 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS crm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE crm;

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  role_code VARCHAR(32) NOT NULL UNIQUE COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) COMMENT '描述',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(128) NOT NULL COMMENT '密码（加密）',
  real_name VARCHAR(64) COMMENT '真实姓名',
  phone VARCHAR(20) COMMENT '手机号',
  email VARCHAR(128) COMMENT '邮箱',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  manager_id BIGINT COMMENT '直属上级ID',
  status TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
  login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  locked_until DATETIME COMMENT '锁定截止时间',
  token_version INT NOT NULL DEFAULT 0 COMMENT 'Token 版本，改密/重置后自增使旧 Token 失效',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 客户表
-- ----------------------------
DROP TABLE IF EXISTS customer;
CREATE TABLE customer (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  name VARCHAR(128) NOT NULL COMMENT '客户名称',
  industry VARCHAR(64) COMMENT '行业',
  level VARCHAR(16) DEFAULT 'C' COMMENT '客户级别 A/B/C/D',
  source VARCHAR(64) COMMENT '客户来源',
  owner_id BIGINT COMMENT '负责人ID',
  address VARCHAR(255) COMMENT '地址',
  remark VARCHAR(500) COMMENT '备注',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_owner (owner_id),
  INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- ----------------------------
-- 联系人表
-- ----------------------------
DROP TABLE IF EXISTS contact;
CREATE TABLE contact (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  customer_id BIGINT NOT NULL COMMENT '客户ID',
  name VARCHAR(64) NOT NULL COMMENT '联系人姓名',
  position VARCHAR(64) COMMENT '职位',
  phone VARCHAR(32) COMMENT '电话',
  email VARCHAR(128) COMMENT '邮箱',
  is_primary TINYINT DEFAULT 0 COMMENT '是否主联系人 1是 0否',
  remark VARCHAR(255) COMMENT '备注',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- ----------------------------
-- 商机表
-- ----------------------------
DROP TABLE IF EXISTS opportunity;
CREATE TABLE opportunity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  customer_id BIGINT NOT NULL COMMENT '客户ID',
  name VARCHAR(128) NOT NULL COMMENT '商机名称',
  stage VARCHAR(32) DEFAULT 'LEAD' COMMENT '阶段: LEAD/REQUIREMENT/QUOTATION/NEGOTIATION/WON/LOST',
  amount DECIMAL(12,2) DEFAULT 0 COMMENT '预计金额',
  expected_date DATE COMMENT '预计成交日期',
  probability INT DEFAULT 0 COMMENT '赢单概率 0-100',
  owner_id BIGINT COMMENT '负责人ID',
  remark VARCHAR(500) COMMENT '备注',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_customer (customer_id),
  INDEX idx_stage (stage),
  INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机表';

-- ----------------------------
-- 商机阶段变更日志表
-- ----------------------------
DROP TABLE IF EXISTS opportunity_stage_log;
CREATE TABLE opportunity_stage_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  opportunity_id BIGINT NOT NULL COMMENT '商机ID',
  from_stage VARCHAR(32) COMMENT '原阶段',
  to_stage VARCHAR(32) NOT NULL COMMENT '新阶段',
  operator_id BIGINT COMMENT '操作人ID',
  remark VARCHAR(255) COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_opportunity (opportunity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机阶段变更日志表';

-- ----------------------------
-- 跟进活动表
-- ----------------------------
DROP TABLE IF EXISTS activity;
CREATE TABLE activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  customer_id BIGINT COMMENT '客户ID',
  opportunity_id BIGINT COMMENT '商机ID',
  type VARCHAR(32) NOT NULL COMMENT '类型: CALL/MEETING/EMAIL/VISIT',
  content VARCHAR(1000) NOT NULL COMMENT '跟进内容',
  activity_time DATETIME COMMENT '活动时间',
  next_follow_time DATETIME COMMENT '下次跟进时间',
  operator_id BIGINT COMMENT '操作人ID',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_customer (customer_id),
  INDEX idx_opportunity (opportunity_id),
  INDEX idx_next_follow (next_follow_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跟进活动表';

-- ----------------------------
-- Token 黑名单表（登出立即失效）
-- ----------------------------
DROP TABLE IF EXISTS token_blacklist;
CREATE TABLE token_blacklist (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  token_hash CHAR(64) NOT NULL UNIQUE COMMENT 'Token SHA-256',
  expire_time DATETIME NOT NULL COMMENT 'Token 过期时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token 黑名单';

-- ----------------------------
-- 登录日志表
-- ----------------------------
DROP TABLE IF EXISTS login_log;
CREATE TABLE login_log (
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

-- ----------------------------
-- 操作审计日志表
-- ----------------------------
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
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

-- ----------------------------
-- 初始数据
-- ----------------------------
-- 角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '系统最高权限'),
('MANAGER', '销售经理', '管理团队，查看团队数据'),
('SALES', '销售代表', '管理自己的客户与商机');

-- 管理员账号（密码: admin123，BCrypt 加密）
INSERT INTO sys_user (username, password, real_name, phone, email, role_id, status) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', '13800000000', 'admin@crm.com', 1, 1);
