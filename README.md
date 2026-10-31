# CRM 客户管理系统

基于 Java (Spring Boot) + Vue.js 的客户关系管理系统。

## 功能模块

- **用户与权限**：JWT 登录、角色管理（管理员/销售经理/销售代表）、数据权限
- **客户管理**：客户 CRUD、多条件筛选、客户详情（联系人/商机/跟进时间线）、**Excel 批量导入/导出**
- **联系人管理**：每个客户多联系人，支持主联系人标记
- **商机管理**：商机 CRUD、6 阶段流转、阶段变更日志
- **跟进活动**：电话/会议/邮件/拜访记录、时间线展示、待办跟进、**到期提醒（铃铛 + 每日定时告警）**
- **数据看板**：核心指标卡片、商机漏斗图、负责人排行、近 7 天活动趋势
- **安全与审计**：登录失败锁定、Token 即时失效（登出/改密/停用）、密码强度策略、操作日志与登录日志

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus + JWT + EasyExcel |
| 数据库 | MySQL 8.0 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |

## 快速启动

### 1. 数据库

```bash
# 全新安装：执行建表脚本（已包含 v2 字段与表）
mysql -u root -p < backend/sql/schema.sql

# 已有旧库升级：执行增量脚本（新增 manager_id / 登录锁定 / Token 版本 / 审计与登录日志表）
mysql -u root -p crm < backend/sql/upgrade_v2.sql
```

默认账号：`admin` / `admin123`

> 密码策略：8-32 位，必须同时包含字母和数字（`admin123` 仅用于初始化，上线后请立即修改）。

### 2. 后端

```bash
cd backend
mvn spring-boot:run          # 默认 dev profile
```

后端启动在 `http://localhost:8080/api`

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:5173`

## 生产运行

完整的服务器部署、Nginx 配置、systemd 守护、备份与升级回滚流程见 [部署指南](docs/deployment-guide.md)。

以 `prod` profile 启动，数据库与密钥全部通过环境变量注入（不要提交到代码库）：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/crm?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
export DB_USERNAME="crm_user"
export DB_PASSWORD="<强密码>"
export JWT_SECRET="<至少 32 字节的随机字符串>"
export JWT_EXPIRATION="86400000"                       # Token 有效期（毫秒）
export CORS_ALLOWED_ORIGINS="https://crm.example.com"  # 多个用逗号分隔
export SPRING_PROFILES_ACTIVE=prod

java -jar backend/target/crm-backend-1.0.0.jar
```

| 环境变量 | 说明 | 默认（仅 dev） |
|---|---|---|
| `DB_URL` | JDBC 连接串 | `jdbc:mysql://localhost:3306/crm...` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `root` / `root` |
| `JWT_SECRET` | JWT 签名密钥（生产必须替换） | 开发内置值 |
| `JWT_EXPIRATION` | Token 有效期毫秒 | `86400000` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端源，逗号分隔 | `http://localhost:5173` |

生产要点：

- 前端执行 `npm run build` 后部署 `frontend/dist`，并将 `VITE_API_BASE_URL` 指向后端地址
- `prod` profile 日志写入 `logs/crm.log`（50MB 滚动，保留 30 天）
- 健康检查：`GET /api/actuator/health`
- 每天 09:00 定时扫描逾期跟进并在 `logs/crm.log` 输出告警；前端铃铛每 5 分钟轮询提醒接口

## 安全与审计

- **登录锁定**：连续 5 次密码错误锁定 15 分钟，管理员可在审计日志中追溯
- **Token 即时失效**：登出写黑名单；修改密码、管理员重置密码、禁用用户会递增 `token_version`，旧 Token 立即 401
- **数据权限**：管理员看全部；销售经理看自己及所有直属/间接下属（`sys_user.manager_id`）；销售仅看自己
- **审计日志**：`/audit` 页面（仅管理员）可查询操作日志（模块/动作/耗时/IP/结果）与登录日志
- 分页参数上限 100 条，避免大数据量拖垮服务

## 目录结构

```
crm/
├── backend/                 # 后端
│   ├── sql/schema.sql       # 建表脚本
│   └── src/main/java/com/crm/
│       ├── controller/      # 控制器
│       ├── service/         # 业务接口
│       ├── service/impl/    # 业务实现
│       ├── mapper/          # MyBatis Mapper
│       ├── entity/          # 实体类
│       ├── dto/             # 数据传输对象
│       ├── security/        # 安全相关（JWT、用户上下文）
│       ├── common/          # 通用类（统一响应、异常处理）
│       ├── config/          # 配置类
│       └── util/            # 工具类
└── frontend/                # 前端
    └── src/
        ├── api/             # API 接口
        ├── router/          # 路由
        ├── store/           # Pinia 状态
        ├── views/           # 页面
        ├── layout/          # 布局
        └── utils/           # 工具（请求封装）
```
