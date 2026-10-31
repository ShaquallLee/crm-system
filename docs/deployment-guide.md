# CRM 客户管理系统 · 部署指南

| 项目 | 内容 |
|---|---|
| 适用版本 | 1.0.0（含 v2 生产化升级） |
| 技术栈 | Spring Boot 3.2.5 + Java 17 + MySQL 8.0 + Vue 3 / Vite |
| 文档更新 | 2026-10-31 |
| 目标读者 | 运维 / 开发 / 实施人员 |

本文档覆盖两种部署形态：

1. **本机直接运行**（开发、演示、单机使用）——与当前开发环境一致；
2. **Linux 服务器生产部署**（Nginx 静态托管前端 + 反向代理后端 + systemd 守护）。

请根据实际场景选择对应章节。生产环境请务必完成第 10 章「安全加固清单」。

---

## 1. 部署架构

```text
浏览器
  │  HTTPS
  ▼
Nginx（80/443）
  ├── /            → 前端静态资源（frontend/dist，history 路由）
  └── /api/**      → 反向代理 → Spring Boot（127.0.0.1:8080，context-path=/api）
                                    │
                                    ▼
                              MySQL 8.0（127.0.0.1:3306，库名 crm）
```

| 组件 | 端口 | 说明 |
|---|---|---|
| Nginx | 80 / 443 | 生产入口；开发环境可不用 |
| 后端 | 8080 | 上下文路径 `/api`，健康检查 `/api/actuator/health` |
| 前端 dev server | 5173 | 仅开发使用，`/api` 代理到 8080 |
| MySQL | 3306 | 库名 `crm`，字符集 `utf8mb4` |

> 后端带 `@Scheduled` 每日 09:00 的逾期跟进扫描任务。**请保持单实例部署**；如需多实例，需要额外的分布式锁，否则会产生重复告警日志。

---

## 2. 环境要求

| 依赖 | 版本要求 | 校验命令 |
|---|---|---|
| JDK | 17（LTS） | `java -version` |
| Maven | 3.8+（推荐 3.9） | `mvn -v` |
| MySQL | 8.0+（当前验证版本 8.0.28） | `mysql --version` |
| Node.js | 20.19+ 或 22.12+（推荐 22 LTS） | `node -v` |
| npm | 10+ | `npm -v` |

服务器资源参考：2 核 4G 起步（后端 JVM 512MB~1GB，MySQL 独立部署时按数据量评估）。磁盘主要占用为日志与数据库，建议预留 20GB 以上。

系统时区统一为 `Asia/Shanghai`（配置中已固定 Jackson 与 JDBC 时区，服务器也建议同步设置）。

---

## 3. 部署前准备

以 Linux 服务器为例，建议目录规划：

```bash
sudo mkdir -p /opt/crm/{backend,frontend,logs} /opt/crm/backup
sudo useradd -r -s /bin/false crm || true
```

| 目录 | 用途 |
|---|---|
| `/opt/crm/backend/crm-backend-1.0.0.jar` | 后端可执行包 |
| `/opt/crm/frontend/` | 前端构建产物（`dist` 内容） |
| `/opt/crm/logs/` | 后端日志（prod profile 写入） |
| `/opt/crm/crm.env` | 环境变量文件（含数据库密码、JWT 密钥，权限 600） |
| `/opt/crm/backup/` | 数据库备份 |

同时在本地开发机准备源码：`backend/`（Java 工程）与 `frontend/`（Vue 工程）。

---

## 4. 数据库部署

### 4.1 创建数据库与应用账号

```sql
-- 以 root 登录 MySQL 执行
CREATE DATABASE IF NOT EXISTS crm
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 应用专用账号（最小权限），密码请替换为强密码
CREATE USER 'crm'@'127.0.0.1' IDENTIFIED BY 'Crm@2026#pwd';
GRANT SELECT, INSERT, UPDATE, DELETE ON crm.* TO 'crm'@'127.0.0.1';
FLUSH PRIVILEGES;
```

> 应用账号只需要 DML 权限；建表/升级由 DBA 使用 root 执行。

### 4.2 初始化表结构（二选一）

**A. 全新安装（仅空库使用，脚本会 `DROP TABLE` 后重建，切勿在有数据的库上执行）**

```bash
mysql -u root -p crm < backend/sql/schema.sql
```

脚本会创建全部表并写入初始数据：3 个角色（管理员/销售经理/销售代表）与默认账号 `admin / admin123`。

**B. 已有旧库升级（仅执行一次）**

```bash
mysql -u root -p crm < backend/sql/upgrade_v2.sql
```

新增内容：`sys_user.manager_id / login_fail_count / locked_until / token_version` 字段与索引，以及 `token_blacklist`、`login_log`、`audit_log` 三张表。

> ⚠️ `upgrade_v2.sql` 中的 `ALTER TABLE ... ADD COLUMN` **不具备幂等性**，重复执行会因字段已存在而报错；请先备份再执行，且只执行一次。

### 4.3 校验

```bash
mysql -u root -p crm -e "SHOW TABLES;"
mysql -u root -p crm -e "SELECT id, username, role_id FROM sys_user;"
```

预期表：`sys_role`、`sys_user`、`customer`、`contact`、`opportunity`、`activity`、`opportunity_stage_log`、`token_blacklist`、`login_log`、`audit_log`（共 10 张）。

### 4.4 备份（升级前必做）

```bash
mysqldump -u root -p --single-transaction --routines crm | gzip > /opt/crm/backup/crm_before_upgrade_$(date +%F_%H%M).sql.gz
```

---

## 5. 后端部署

### 5.1 构建

```bash
cd backend
mvn clean package -DskipTests
# 产物：target/crm-backend-1.0.0.jar
```

> 若服务器无外网，可在内网构建机执行 `mvn clean package` 后仅拷贝 jar 到服务器运行（无需在服务器安装 Maven）。

### 5.2 环境变量（生产必配）

创建 `/opt/crm/crm.env`（`chmod 600`）：

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://127.0.0.1:3306/crm?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=crm
DB_PASSWORD=Crm@2026#pwd
JWT_SECRET=请替换为至少32字节的随机字符串
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=https://crm.example.com
LOG_FILE=/opt/crm/logs/crm.log
```

生成 JWT 密钥：

```bash
openssl rand -base64 48
```

| 变量 | 必填 | 说明 | 开发默认值 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 是 | 生产必须为 `prod` | `dev` |
| `DB_URL` | 是 | JDBC 连接串，需保留 `serverTimezone=Asia/Shanghai` | localhost:3306/crm |
| `DB_USERNAME` / `DB_PASSWORD` | 是 | 数据库账号 | `root` / `root` |
| `JWT_SECRET` | 是 | 签名密钥，长度 ≥ 32 字节；修改后所有旧 Token 失效 | 内置开发值（不可用于生产） |
| `JWT_EXPIRATION` | 否 | Token 有效期（毫秒），默认 24 小时 | `86400000` |
| `CORS_ALLOWED_ORIGINS` | 是 | 允许的前端来源，多个用英文逗号分隔 | `http://localhost:5173,http://127.0.0.1:5173` |
| `LOG_FILE` | 否 | 日志文件路径 | `logs/crm.log` |

> 注意：若前端与后端由 Nginx 同域反向代理（推荐），浏览器请求同源，CORS 实际不触发，但仍建议配置为正式域名，避免直连场景被拒。

### 5.3 启动方式

**方式一：systemd（生产推荐）**

`/etc/systemd/system/crm.service`：

```ini
[Unit]
Description=CRM Backend
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=crm
WorkingDirectory=/opt/crm/backend
EnvironmentFile=/opt/crm/crm.env
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/crm/backend/crm-backend-1.0.0.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now crm
sudo systemctl status crm
sudo journalctl -u crm -f          # 查看启动日志
```

> `SuccessExitStatus=143` 用于兼容 Spring Boot 优雅停机（SIGTERM）。停机等待时间上限 20 秒（`spring.lifecycle.timeout-per-shutdown-phase`）。

**方式二：nohup（临时/轻量）**

```bash
cd /opt/crm/backend
set -a && source /opt/crm/crm.env && set +a
nohup java -Xms512m -Xmx1024m -jar crm-backend-1.0.0.jar > /dev/null 2>&1 &
```

### 5.4 启动校验

```bash
curl -i http://127.0.0.1:8080/api/actuator/health
# 期望 HTTP 200，返回 {"status":"UP"}

curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# 期望 code=200 且返回 data.token
```

日志位置：`/opt/crm/logs/crm.log`（50MB 滚动，保留 30 天）。

---

## 6. 前端部署

### 6.1 构建

```bash
cd frontend
npm ci                              # 首次或依赖变更时
npm run build                       # 产物输出到 dist/
```

**接口地址**：构建时读取 `VITE_API_BASE_URL`，未设置时默认 `/api`（与 Nginx 反向代理配合最省事）。

- 同域反代（推荐）：无需设置，保持默认 `/api`；
- 前后端分离部署：在 `frontend/.env.production` 写入
  ```bash
  VITE_API_BASE_URL=https://api.example.com/api
  ```
  同时把该域名加入后端 `CORS_ALLOWED_ORIGINS`。

将 `dist/` 内容上传到服务器：

```bash
rsync -av --delete frontend/dist/ /opt/crm/frontend/
```

### 6.2 Nginx 配置

`/etc/nginx/conf.d/crm.conf`：

```nginx
server {
    listen 80;
    server_name crm.example.com;

    root /opt/crm/frontend;
    index index.html;

    # 与后端上传上限保持一致（后端 max-request-size=20MB）
    client_max_body_size 20m;

    gzip on;
    gzip_types text/css application/javascript application/json image/svg+xml;
    gzip_min_length 1024;

    # 前端 history 路由：未匹配到静态文件时回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;      # Excel 导入导出留足时间
        proxy_send_timeout 60s;
    }
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

> 这些 `X-Forwarded-*` 头是审计日志记录真实客户端 IP 的依据（后端 `IpUtil` 会读取 `X-Forwarded-For`），请勿省略。
> 生产建议再配置 443/HTTPS（证书可用 certbot 或企业证书），并把 HTTP 重定向到 HTTPS。

### 6.3 校验

浏览器访问 `https://crm.example.com`，确认：

- 可打开登录页并登录；
- 刷新任意子页面（如 `/customers`）不出现 404（history 回退生效）；
- 客户页「导出 Excel」可下载文件。

---

## 7. 本机快速部署（macOS 开发/演示）

适用于当前开发机，无需 Nginx。

```bash
# 1) 数据库（首次）建库建表
mysql -u root -p < backend/sql/schema.sql

# 若已有旧库，改为执行升级脚本（仅一次）
mysql -u root -p crm < backend/sql/upgrade_v2.sql

# 2) 后端（默认 dev profile，连接 localhost:3306/crm，账号 root/root）
cd backend
mvn spring-boot:run
# 或使用 JDK 绝对路径：
# JAVA_HOME="$HOME/jdk/jdk-17.0.21+9/Contents/Home" PATH="$JAVA_HOME/bin:$HOME/maven/bin:$PATH" mvn spring-boot:run

# 3) 前端
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，默认账号 `admin / admin123`。

开发环境说明：

- 前端 5173 通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`；
- 后端 dev profile 会打印 SQL 日志，便于排查；
- 本机数据库默认账号可在 `DB_USERNAME` / `DB_PASSWORD` 环境变量中覆盖。

---

## 8. 首次使用与初始化配置

1. 用 `admin / admin123` 登录后**立即修改密码**（右上角头像菜单 → 修改密码，要求 8-32 位且含字母与数字）。
2. 进入「用户管理」创建销售经理、销售代表账号，并为销售代表设置「直属上级」。
3. 数据权限规则（无需额外配置，随角色与上级关系自动生效）：

| 角色 | 可见数据范围 |
|---|---|
| 管理员（ADMIN） | 全部数据 |
| 销售经理（MANAGER） | 本人 + 全部直属/间接下属的数据 |
| 销售代表（SALES） | 仅本人负责的数据 |

4. 通过「客户管理 → 下载导入模板」准备 Excel，再使用「导入 Excel」批量导入（单次 ≤ 1000 行）。
5. 逾期提醒：跟进记录填写「下次跟进时间」后，顶部铃铛每 5 分钟刷新；每日 09:00 后端也会扫描并在日志中输出告警。

---

## 9. 日常运维

### 9.1 启停与重启

```bash
sudo systemctl start crm
sudo systemctl stop crm          # 优雅停机，最多等待 20 秒
sudo systemctl restart crm
sudo systemctl status crm
```

### 9.2 日志

| 内容 | 位置 |
|---|---|
| 应用日志 | `/opt/crm/logs/crm.log`（prod） |
| systemd 输出 | `journalctl -u crm` |
| 业务审计 | 系统内「审计日志」页面（仅管理员），或查 `audit_log` / `login_log` 表 |

### 9.3 数据库备份与恢复

每日备份（crontab，凌晨 2 点）：

```bash
0 2 * * * mysqldump -u root -p'<密码>' --single-transaction crm | gzip > /opt/crm/backup/crm_$(date +\%F).sql.gz
# 保留 30 天
0 3 * * * find /opt/crm/backup -name 'crm_*.sql.gz' -mtime +30 -delete
```

恢复：

```bash
gunzip -c /opt/crm/backup/crm_2026-10-30.sql.gz | mysql -u root -p crm
```

> 建议将备份文件定期复制到异地或对象存储；生产库恢复前先停后端服务。

### 9.4 版本升级

```bash
# 1) 备份（数据库 + 旧 jar）
mysqldump ... > /opt/crm/backup/crm_before_release.sql
cp /opt/crm/backend/crm-backend-1.0.0.jar /opt/crm/backup/crm-backend-1.0.0.jar.bak

# 2) 若有数据库变更脚本，先用 root 执行（只执行一次）
mysql -u root -p crm < backend/sql/upgrade_v2.sql

# 3) 替换 jar 与前端静态资源
sudo systemctl stop crm
scp target/crm-backend-1.0.0.jar /opt/crm/backend/
rsync -av --delete frontend/dist/ /opt/crm/frontend/
sudo systemctl start crm

# 4) 冒烟校验
curl -s http://127.0.0.1:8080/api/actuator/health
```

### 9.5 回滚

```bash
sudo systemctl stop crm
cp /opt/crm/backup/crm-backend-1.0.0.jar.bak /opt/crm/backend/crm-backend-1.0.0.jar
# 如数据库也做了变更：先恢复数据库备份
gunzip -c /opt/crm/backup/crm_before_release.sql.gz | mysql -u root -p crm
sudo systemctl start crm
```

---

## 10. 安全加固清单

上线前逐项确认：

- [ ] 默认账号 `admin/admin123` 已修改为强密码
- [ ] `JWT_SECRET` 已替换为随机值（≥ 32 字节），未写入代码仓库
- [ ] 数据库使用专用账号（仅 DML 权限），root 仅用于运维
- [ ] `CORS_ALLOWED_ORIGINS` 仅包含正式域名，不含通配符
- [ ] 已启用 HTTPS，HTTP 自动跳转
- [ ] Nginx 与后端仅监听必要地址，8080/3306 不对公网开放（云安全组限制）
- [ ] `/opt/crm/crm.env` 权限为 `600`，所有者 `crm`
- [ ] 已配置每日数据库备份并验证过恢复流程
- [ ] 已确认审计日志可查询（操作日志 + 登录日志），登录锁定策略符合预期
- [ ] 服务器时间与时区为 `Asia/Shanghai` 且已开启 NTP
- [ ] 单实例部署（或已评估定时任务重复执行的影响）

内置安全机制（无需额外配置）：

- 登录连续失败 5 次锁定 15 分钟；
- 登出、修改密码、管理员重置密码、禁用用户后旧 Token 立即失效；
- 越权访问返回 403；分页单页上限 100 条；
- 上传上限 10MB/请求 20MB；Excel 单次导入上限 1000 行、导出上限 10000 行。

---

## 11. 常见问题排查

| 现象 | 可能原因 | 处理方式 |
|---|---|---|
| 启动即失败，日志报数据库连接异常 | `DB_URL` / 账号密码错误、MySQL 未启动、账号无权限 | 用 `mysql -h 127.0.0.1 -u crm -p crm` 手工验证；检查 `GRANT` 与网络策略 |
| 访问接口 404 | 漏了上下文路径 `/api` | 正确地址为 `http://host:8080/api/xxx`，健康检查为 `/api/actuator/health` |
| 前端页面刷新 404 | Nginx 未配置 history 回退 | 补充 `try_files $uri $uri/ /index.html;` |
| 浏览器报 CORS 跨域错误 | 前端域名不在允许列表，或直连后端未过 Nginx | 把正式域名加入 `CORS_ALLOWED_ORIGINS` 并重启后端；优先使用同域反代 |
| 登录提示「账号已锁定」 | 连续 5 次密码错误 | 等待 15 分钟，或 DBA 执行：`UPDATE sys_user SET login_fail_count=0, locked_until=NULL WHERE username='xxx';` |
| 登录后很快 401 | Token 过期；密码被修改/重置；账号被禁用 | 重新登录；管理员确认是否执行过重置或禁用 |
| 接口返回 403 | 数据权限限制（访问非本人/非本团队数据） | 确认账号角色与「直属上级」配置是否正确 |
| 客户导入失败 | 未使用模板、列名不符、超过 1000 行、级别非 A/B/C/D | 下载最新模板；查看返回的逐行错误信息 |
| 上传报 413 / 超过大小 | Nginx 或后端上传上限 | Nginx `client_max_body_size 20m` 与后端 `max-request-size` 保持一致 |
| 中文乱码 / 时间偏差 8 小时 | 库字符集非 utf8mb4 或时区参数缺失 | 检查库/表 `utf8mb4`；`DB_URL` 保留 `serverTimezone=Asia/Shanghai` |
| 日志文件未生成 | prod profile 未生效或目录无写权限 | 确认 `SPRING_PROFILES_ACTIVE=prod`、`LOG_FILE` 指向可写目录 |
| 端口占用 | 8080/5173 已被其他进程使用 | `lsof -i :8080`（macOS/Linux）确认后停止冲突进程 |
| Excel 导出/导入超时 | 数据量大、反向代理超时过短 | Nginx `proxy_read_timeout` 调至 60s 以上；缩小导出条件范围 |
| 提醒铃铛无数据 | 跟进记录未填「下次跟进时间」或超出 3 天范围 | 在客户详情补充下次跟进时间；确认后端定时任务与前端的轮询正常 |

查看后端最近错误：

```bash
tail -n 200 /opt/crm/logs/crm.log
# 或
journalctl -u crm -n 200 --no-pager
```

---

## 12. 上线验收清单

- [ ] `GET /api/actuator/health` 返回 200 且 `status=UP`
- [ ] 管理员登录成功，登录日志中可查到本次记录
- [ ] 修改密码后再次用旧页面操作会被要求重新登录（旧 Token 失效）
- [ ] 创建「经理 + 下属销售」，销售只能看到自己的客户，经理能看到下属客户，其他销售看不到
- [ ] 客户新增/编辑/删除后，「审计日志 → 操作日志」有对应记录（含操作人、IP、耗时）
- [ ] 客户导出 Excel 可正常打开；模板下载可用；导入 2 行数据成功、错误行有明确提示
- [ ] 商机新增时客户为下拉选择，阶段流转正常
- [ ] 跟进记录填写下次跟进时间后，铃铛显示逾期/今日/即将分类
- [ ] 页面刷新、浏览器前进后退均正常（history 路由）
- [ ] 服务器重启后 `crm` 服务自动拉起（`systemctl is-enabled crm`）
- [ ] 数据库备份任务已生效，且完成一次恢复演练

---

## 附录 A：配置与端口速查

| 项 | 值 |
|---|---|
| 后端包名 | `target/crm-backend-1.0.0.jar` |
| 后端上下文路径 | `/api` |
| 健康检查 | `GET /api/actuator/health` |
| 登录接口 | `POST /api/auth/login`，Body `{"username":"...","password":"..."}` |
| 前端构建产物 | `frontend/dist/` |
| 前端接口基址 | 构建变量 `VITE_API_BASE_URL`，默认 `/api` |
| 数据库 | `crm`，`utf8mb4` |
| 建表脚本 | `backend/sql/schema.sql`（会重建表，仅空库使用） |
| 升级脚本 | `backend/sql/upgrade_v2.sql`（旧库执行一次） |
| 日志滚动 | 单文件 50MB，保留 30 天 |

## 附录 B：关键配置文件

| 文件 | 作用 |
|---|---|
| `backend/src/main/resources/application.yml` | 端口、上下文路径、数据源、JWT、CORS、上传与分页等全局配置（支持环境变量覆盖） |
| `backend/src/main/resources/application-dev.yml` | 开发环境：SQL 日志、`com.crm` DEBUG |
| `backend/src/main/resources/application-prod.yml` | 生产环境：INFO 级别、文件日志与滚动策略 |
| `frontend/src/utils/request.js` | Axios 基址与 401/403 统一处理 |
| `frontend/vite.config.js` | 开发端口 5173 与 `/api` 代理 |

部署过程中如遇到本文档未覆盖的问题，请附带 `crm.log` 相关时间段日志与复现步骤提交给开发人员排查。
