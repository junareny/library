# 阅间电子图书馆

提供注册登录、图书检索、详情查看、借阅归还和当前借阅管理，支持中英文切换。前端使用原生 HTML、CSS、JavaScript，由 Spring Boot 同源提供，无需独立构建。

## 技术栈

Java 21、Maven 3.9+、Spring Boot 3.3.13、Spring Security JWT、MyBatis-Plus 3.5.9、Bucket4j 8.22.0、MySQL 5.7、Flyway、Springdoc、JUnit 5。

## 核心设计

- 按认证、图书、借阅和用户划分业务域。Controller 处理请求，AppService 编排业务与事务，TableService/Mapper 负责数据访问；BO 为请求对象，VO 为响应对象，TPO 为数据库实体。
- 三张业务表：`users`、`books`、`loans`。每人最多借 5 本，同一本书不能重复持有；借期为 30 天，逾期仍占用库存和额度。
- 借还事务采用 `READ_COMMITTED`，先锁定用户行，再校验额度、条件更新库存和借阅状态，失败整体回滚。生成列唯一索引防止重复活跃借阅，触发器补充 MySQL 5.7 的约束检查。
- JWT 有效期 2 小时，密码使用 BCrypt 哈希。书目允许匿名查询，借还接口从令牌获取用户身份。
- 接口采用 POST，统一返回 `{code, msg, body}`；分页数据为 `{total, data}`，页码从 1 开始。错误使用 HTTP 状态和错误枚举，消息根据 `Accept-Language` 返回中文或英文。

```text
src/main/java/com/rxs/library/<业务域>/
  controller/
  application/service/
  domain/bo/、vo/、entity/、service/
  infrastructure/mapper/
    xml/
src/main/resources/
  db/migration/       # Flyway 建表与初始数据
  static/             # 前端页面、样式和脚本
  messages*.properties
```

## 本地运行与 JAR 部署

1. 安装 Java 21、Maven 和 MySQL 5.7，配置 `JAVA_HOME`。创建独立的 UTF-8 数据库及专用账号，授权建表、索引、外键、触发器和读写操作。首次启动由 Flyway 自动初始化表结构和示例图书。
2. 在项目根目录配置环境变量。以下为 PowerShell 示例，连接信息由实际部署环境提供：

```powershell
$env:DB_URL = 'jdbc:mysql://<主机>:<端口>/<库名>?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = '<数据库账号>'
$env:DB_PASSWORD = '<数据库密码>'
$env:SERVER_PORT = '8080'

# 首次生成 JWT 密钥，部署时安全保存，后续启动复用。
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

`JWT_SECRET` 为至少 32 个随机字节的 Base64 表示。上面的 JDBC 参数适用于本地调试；生产连接应按环境配置 TLS。

3. 开发运行或打包部署，选择执行：

```powershell
# 开发运行
mvn spring-boot:run

# 构建 JAR，跳过需要独立数据库的测试
mvn -DskipTests package
java -jar target/library-1.0.0.jar
```

应用默认端口为 `8080`，可通过 `SERVER_PORT` 修改。首页为 `/`，接口文档为 `/swagger-ui/index.html`，OpenAPI 为 `/v3/api-docs`。

也可将环境变量设置写入本地 `.local/dev-env.ps1`，使用 `./scripts/Run.ps1` 加载并运行。Spring Boot 不会自动读取 `.env`；真实连接、账号、密码和安装路径不得提交到仓库。

## Docker Compose 部署

复制 `.env.example` 为 `.env`，填写独立随机的 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD` 和 Base64 编码的 `JWT_SECRET`，按需设置 `SERVER_PORT`。Compose 已定义容器内部的数据库地址和账号，无需填写模板中的 `DB_URL`、`DB_USERNAME`。

```bash
docker compose up --build -d
docker compose logs -f app
docker compose down
```

数据库使用命名卷持久化且不发布宿主机端口；应用仅绑定回环地址。远程访问需另外配置反向代理及 HTTPS。Compose 部署尚未在本项目环境中完成实测。

## 接口限流

使用 Bucket4j 内存令牌桶，默认开启，规则在 `application.yml` 的 `app.rate-limit` 下配置：

| 接口 | 计数身份 | 桶容量 | 连续补满周期 |
| --- | --- | --- | --- |
| 登录 | 请求 IP | 20 | 1 分钟 |
| 注册 | 请求 IP | 5 | 1 分钟 |
| 借阅、归还（共享额度） | 已认证用户 | 10 | 1 分钟 |

允许桶容量内的初始突发，令牌持续补充，非固定窗口计数。超限返回 HTTP `429`、`TOO_MANY_REQUESTS`、中英文提示及整秒 `Retry-After`，被拒绝的请求不执行借还事务。设置 `app.rate-limit.enabled=false` 可关闭。

最多保留 `max-buckets` 个身份桶（默认 10000），空闲满一个周期后可回收；达到上限时拒绝新身份，不淘汰活跃桶。额度仅在当前实例内有效，应用重启后重置，多实例部署需改用共享存储。

IP 使用直连地址，不读取客户端传入的转发头。部署反向代理后，来自同一代理的请求可能共享额度，需按实际拓扑配置可信代理和客户端地址解析。

## 测试与规范

集成测试使用真实 MySQL 5.7，需配置 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD`。测试库名必须以 `_test` 结尾，使用独立账号；测试会清理用户、借阅记录并重置库存，不可指向业务库。共享测试库的用例应串行执行。

```powershell
mvn validate                           # 格式与代码规范检查
mvn spotless:apply                     # 自动格式化
mvn -Dtest=LocalizedMessageTest test    # 无需数据库的消息单元测试
mvn clean verify                       # 全部测试与打包
```

已有 `.local/dev-env.ps1` 时，可执行 `./scripts/Export-TestConfig.ps1` 生成本地测试配置，再从 IDE 直接运行测试；工作目录设为项目根目录。配置文件与 `docs` 目录仅保留在本地，不提交 Git。

## 当前范围

未提供电子书上传、在线阅读、管理后台、预约或续借；未实现刷新令牌、主动撤销令牌和请求幂等键。生产部署需补充密钥管理、备份、监控和访问限流。
