# 实现报告：课序课程作业提交平台

## 交付概况

本项目已从仅供本机展示的 JDK `HttpServer` 实验程序，改造成 Spring Boot 单体课程作业平台。学生、教师和管理员使用同一中文 Web 应用；关系数据写入 PostgreSQL，上传内容写入独立的本地文件卷。此前源码中的演示账号和明文密码已从运行代码中移除。

## 已实现内容

- **账户与权限**：学生通过注册页创建账户，所有新账户固定为学生角色；BCrypt（强度 12）存储密码。管理员由环境变量首次引导，后台可将学生设为教师，不能通过公开注册或后台表单授予管理员。登录使用服务端会话；启用 CSRF、会话固定保护、退出时清除会话、HttpOnly/SameSite Cookie、可配置 Secure Cookie，以及 CSP、Referrer-Policy 和 `nosniff` 响应头。
- **课程与作业**：教师创建课程，系统生成邀请码；学生凭邀请码加入。教师仅能为本人课程发布作业，设定课程时区下的截止时间并选择是否接受迟交。学生只能查看已加入课程；教师只能查看本人课程及其中学生。
- **提交与版本**：每个学生每份作业使用一条提交记录；每次重新提交建立递增版本，保留原始文件名、提交时间、迟交标记、字节数和 SHA-256 摘要。禁止迟交时，过期上传会被拒绝。
- **文件保护**：只接收 PDF、DOC、DOCX，单文件最大 10 MiB、单请求最大 12 MiB；检查扩展名和基本签名。磁盘文件名由随机键生成，拒绝路径穿越和覆盖。下载强制作为附件并设置 `nosniff`，每次读取均检查提交本人或课程教师权限。`FileStorage` 接口与本地实现分离，应用容器通过独立命名卷保存上传。
- **工程配置**：Java 21、Spring Boot 4.1.1、Maven Wrapper 3.3.4（固定 Maven 3.9.16 并校验分发包摘要）、PostgreSQL 18.6、Flyway 12.4.0。Flyway V1 建立用户、课程、选课、作业、提交与版本表；Hibernate 启动时验证结构，不负责自动建表。
- **运行和协作**：提供 Dockerfile 与 Docker Compose（数据库健康检查、应用等待数据库健康、独立数据库/上传命名卷、只读根文件系统、非 root 应用用户）、不含密钥的 `.env.example`、Windows/Linux Wrapper 启动脚本和 GitHub Actions Java 21 `verify` 工作流。`README.md` 已改为中文，说明启动、角色流程、安全配置和限制。

## 技术选择

| 组件 | 版本或方案 | 采用原因 |
|---|---|---|
| Java | 21 LTS | 当前本机 JDK 可用，Spring Boot 支持 |
| Spring Boot | 4.1.1 | 稳定版，支持 Web MVC、安全、JPA 与 Thymeleaf |
| Maven | Wrapper 3.3.4 / Maven 3.9.16 | 固定构建入口，不要求预装 Maven |
| PostgreSQL | 18.6 | 当前受支持的数据库主版本，数据使用独立命名卷 |
| Flyway | 12.4.0 | 版本化迁移；PostgreSQL 数据库模块与核心版本对齐 |
| H2（仅测试） | 2.3.232 | 本机无 Docker 时仍可运行迁移、Web 和业务测试 |
| Testcontainers | 2.0.5 | 有 Docker 的环境自动对 PostgreSQL 18.6 执行迁移冒烟测试 |

参考：[Spring Boot 系统要求](https://docs.spring.io/spring-boot/system-requirements.html)、[Boot 依赖版本表](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)、[Maven Wrapper](https://maven.apache.org/tools/wrapper/)、[PostgreSQL 版本支持政策](https://www.postgresql.org/support/versioning/)、[Flyway PostgreSQL 支持](https://documentation.red-gate.com/fd/postgresql-database-277579325.html)。

## 自动化测试与本机验证

在 JDK 21.0.11 环境执行 `mvnw.cmd -B -ntp verify`，结果为 **BUILD SUCCESS**：共报告 10 项，9 项通过，1 项跳过，失败和错误均为 0。通过项覆盖：

- Flyway 在 H2 上执行 V1、JPA 结构校验、登录/注册/管理员用户页/工作台/课程页模板渲染。
- 注册用户始终为学生、密码为 BCrypt 哈希；无 CSRF 令牌的写请求被拒绝；学生无法访问管理员页面或创建课程。
- 邀请码加入、教师建课与发布作业、禁迟交拒绝、迟交状态和版本递增、课程归属下载授权。
- PDF/DOC/DOCX 签名与扩展名检查、文件名净化、本地存储 SHA-256、路径键限制、拒绝覆盖和超限时清除不完整文件。

PostgreSQL 18.6 Testcontainers 测试已加入构建，在有 Docker 的环境运行；本机没有 Docker，因此该项按 `disabledWithoutDocker` 跳过，没有声称本机验证了 PostgreSQL 或 Compose 容器启动。另以隔离 H2 测试运行时启动实际 Java Web 进程，经 HTTP 检查 `/login`、`/register` 和 CSS 均返回 200，并确认响应含 CSP。

## 安全检查与限制

- 发布候选的源码、配置和文档扫描未发现旧演示账户、提交人个人标识或常见凭据字面量；`.env`、构建目录、日志、上传目录和原始 `testfiles/` 均由 `.gitignore` 排除。`.env.example` 不含实际密码或令牌。
- 上传类型检查是基础文件签名验证，不是完整文档解析或恶意软件扫描；系统暂不包含病毒扫描、评分、评论、邮件找回密码、请求速率限制和对象存储实现。
- 生产环境应启用 HTTPS 与 `APP_COOKIE_SECURE=true`，使用强且独立的数据库/管理员密码，并做好数据库和上传卷的访问控制及备份。
- 本机没有 Docker，因此 Docker 镜像构建、Compose 启动和 PostgreSQL 容器测试尚未在本机执行；GitHub Actions 结果需在仓库推送后由远端工作流产生。
- 仓库未附带开源许可证；公开可见不授予额外的复制、分发或商用权利。
