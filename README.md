# 阅间电子图书馆 V1.0

基于 Java 21、Spring Boot 3 和 MySQL 5.7 的图书目录与借还管理应用。提供中英文前端、注册登录、书籍搜索、详情、借阅归还和当前借阅管理。电子书文件、在线阅读、管理后台、预约、续借和罚金不在此版本范围内。

## 技术版本

- Java 21，Maven 3.9+
- Spring Boot 3.3.13，Spring Security 6，MyBatis-Plus 3.5.9（Boot 3 starter + 分页插件），Lombok
- MySQL 5.7.43 已用于本地验证；Compose 使用 MySQL 5.7.44
- Flyway（版本由 Spring Boot 管理），Springdoc 2.6.0
- JUnit 5、MockMvc，使用真实 MySQL 的集成和并发测试

## 快速运行

安装 Java 21、Maven 和兼容的 MySQL 实例，在项目根目录运行。通过环境变量配置连接与密钥，实际地址、库名、账号和安装路径不写入仓库。

```powershell
$env:DB_URL = '<完整 JDBC 连接地址>'
$env:DB_USERNAME = '<数据库账号>'
$env:DB_PASSWORD = '<数据库密码>'
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
mvn spring-boot:run
```

访问应用配置的地址，首页路径为 `/`，Swagger UI 为 `/swagger-ui/index.html`，OpenAPI JSON 为 `/v3/api-docs`。可以通过 `SERVER_PORT` 指定应用端口。

## 前端使用

前端使用原生 HTML、CSS 和 JavaScript，由 Spring Boot 同源提供，无需单独安装 Node.js、启动前端服务或设置跨域。文件位于 `src/main/resources/static`，会随 JAR 一起打包。

首页可匿名浏览 10 本馆藏，按书名搜索、筛选可借书籍、翻页和查看详情。点击右上角“登录 / 注册”可创建账号；密码输入框明确提示至少 8 个字符。登录后可借阅图书，通过左侧“我的借阅”查看借阅日、应还日、剩余天数及逾期状态，并确认归还。

令牌及用户名保存在当前浏览器标签页的 sessionStorage 中；关闭标签页或点击退出后不再保留该会话。密码不保存。令牌过期或被服务端拒绝时清除登录状态；网络失败不显示虚假的操作成功。借阅响应不确定时重新查询当前借阅确认状态。退出是本地退出，不撤销已签发的 JWT。

布局适配电脑和手机；封面为程序绘制的装饰图案，不代表原书真实封面。加载、空列表、失败和库存不足均有独立状态。

后台启动脚本先复制构建产物到 `.local/runtime` 再运行，避免 Windows 锁定 target 中的 JAR，允许服务运行期间重新构建。重启后才会使用新的构建结果。

开发账号、测试账号密码及 JWT 密钥均随机生成，写入被 Git 忽略的 `.local/dev-env.ps1`。root 客户端凭据也仅保存在 `.local` 私有配置中，不写入源码或应用配置。不要提交、分享或输出该目录。开发和测试账号分别只能访问各自的数据库。

## 数据库配置

预先创建独立的 UTF-8 数据库和专用账号；账号需要建表、索引、外键、触发器及读写权限，以供 Flyway 迁移。开发库与测试库必须隔离。

连接配置通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 注入。`JWT_SECRET` 必须是至少 32 个随机字节的 Base64 表示，缺失或格式错误会导致启动失败。

可以将私有环境配置保存在被 Git 忽略的 `.local/dev-env.ps1`，再通过 `scripts/Run.ps1` 加载；Spring Boot 不会自动读取 `.env`。直接执行 Maven 时需自行设置环境变量。

数据库辅助脚本从 `MYSQL_HOME` 或 `-MySqlHome` 参数读取安装位置。脚本中的库名和账号仅为通用初始化示例，不代表实际部署配置；使用前按自己的隔离环境调整。隔离实例使用独立数据目录，通过 `-IsolatedDatabase` 显式选择。

## 构建和测试

后端代码格式参考阿里巴巴 Java 开发手册，使用四空格缩进、明确导入、完整大括号、命名常量及关键 Javadoc。格式化运行 `mvn spotless:apply`，检查运行 `mvn validate`；检查同时接入正常构建。具体范围见 [Java 格式约定](docs/java-style.md)。

```powershell
# 在项目根目录执行
.\scripts\Run.ps1 -Test
.\scripts\Run.ps1 -Package
```

`-Package` 执行 `mvn verify`，包含集成测试并生成 `target/library-1.0.0.jar`。如只需打包而不执行数据库测试：`mvn -DskipTests package`。

脚本默认读取私有环境配置；使用 `-IsolatedDatabase` 时读取隔离环境配置。初始化辅助脚本发现同名库或账号时拒绝覆盖。

外部测试数据库必须配置 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD`。测试会清理该库的 users/loans，并重置书籍库存；数据库名必须以 `_test` 结尾，版本必须为 MySQL 5.7。请使用单独的空库和专用账号。测试初始化同样使用 Flyway，不使用 H2、Mockito 来替代数据库锁行为。

在 IDE 中直接运行 JUnit 前，执行一次 `./scripts/Export-TestConfig.ps1`，将现有本地环境导出至被 Git 忽略的 `.local/test-database.properties`。测试会自动加载该文件，环境变量仍可覆盖其中配置。IDE 运行配置的工作目录应为项目根目录；修改数据库连接后重新导出。`Run.ps1 -Test`、`Run.ps1 -Package` 和现有实例初始化脚本也会自动生成此文件；无需在 IDE 中填写密码。

测试覆盖：认证及输入校验、公开查询、借阅/归还闭环、越权隐藏、借阅上限、动态逾期、稳定排序、数据库触发器、插入失败与唯一冲突回滚、归还库存失败回滚，以及五类并发竞争。测试执行结果见 [验证记录](docs/verification.md)。

测试按认证、书目、借还业务、持久化、并发和国际化拆分，统一继承 `AbstractLibraryIntegrationTest`，共 28 项数据库集成测试。`LocalizedMessageTest` 包含 2 项无需数据库的单元测试，可独立运行 `mvn -Dtest=LocalizedMessageTest test`。所有用例提供中文展示名称；测试类按默认顺序执行，共享数据库的集成测试请勿开启并行执行。

应用启动后可运行 `.\scripts\Smoke-Test.ps1`，通过真实 HTTP 请求验证 7 个接口和 Swagger。脚本会创建一个随机名称的 smoke 用户并保留一条已归还历史记录，不输出密码或令牌。

## API 契约

接口采用 POST 动作接口、BO/VO、统一 `Result` / `PageListResult`，保持业务层与持久化层分离。

**本次属于接口契约变更：旧 `/api/*` 接口已移除，调用方需同步升级。** 成功 HTTP 状态统一为 200；业务错误保留 400/401/404/409/500/503。

| 方法 | 路径 | 认证 | 请求体 |
| --- | --- | --- | --- |
| POST | `/IAuthBiz/register` | 无 | `{username,password}` |
| POST | `/IAuthBiz/login` | 无 | `{username,password}` |
| POST | `/IBookBiz/queryBook/page` | 无 | `{"pageCurrent":1,"pageSize":20,"dto":{"title":"Java"}}` |
| POST | `/IBookBiz/queryBook` | 无 | `{"id":1}` |
| POST | `/ILoanBiz/borrowBook` | Bearer JWT | `{"bookId":1}` |
| POST | `/ILoanBiz/queryCurrentLoan/page` | Bearer JWT | `{"pageCurrent":1,"pageSize":20}` |
| POST | `/ILoanBiz/returnBook` | Bearer JWT | `{"id":123}` |

普通响应：`{"code":"0","msg":"操作成功","body":{...}}`。归还成功 body 为 null。

分页响应：`{"code":"0","msg":"查询成功","body":{"total":10,"data":[...]}}`。

注册和登录请求：`{"username":"alice","password":"password123"}`。用户名区分大小写，长度 3–50，不接受空白字符。密码至少 8 字符且最多 72 UTF-8 字节，以免 BCrypt 截断。未知请求字段会被拒绝，包括 `role` 或借阅请求中的 `userId`。

借阅请求：`{"bookId":1}`。借阅详情：`{id,bookId,borrowedAt,dueAt,returnedAt,status}`。用户身份仅从经过验证的 JWT `sub` 获取。

分页页码从 1 开始，默认 pageCurrent=1、pageSize=20，pageSize 允许 1–100；非法参数返回 400。dto 可省略；标题最大 255 字符。书籍按 ID 升序；当前借阅按 borrowedAt、id 降序。标题不区分大小写，`%`、`_`、`!` 按字面字符搜索，SQL 参数绑定。预置 10 本书，ID=3 无库存，ID=2 仅一本，其余为多份库存。

时间统一为 UTC ISO 8601。到期日为借阅时刻加 30×24 小时；恰好到期仍为 BORROWED，超过到期为 OVERDUE，已归还为 RETURNED。逾期仍占用库存和额度。

错误与成功均为 `application/json`，并保留 HTTP 状态。例如 HTTP 409：

```json
{"code":"OUT_OF_STOCK","msg":"暂无可借副本，请选择其他图书。","body":null}
```

| 状态 | code |
| --- | --- |
| 400 | VALIDATION_ERROR |
| 401 | UNAUTHORIZED |
| 404 | RESOURCE_NOT_FOUND，包括他人的借阅 |
| 409 | USERNAME_EXISTS / OUT_OF_STOCK / ALREADY_BORROWED / LOAN_LIMIT_EXCEEDED / ALREADY_RETURNED |
| 503 | SERVICE_UNAVAILABLE，临时数据库故障 |
| 500 | INTERNAL_ERROR，未预期错误或跨表库存不一致 |

## 调用示例

PowerShell 示例（响应中的业务数据从 body 读取）：

```powershell
$base = 'http://<应用主机>:<应用端口>'
$credentials = @{username='alice'; password='password123'} | ConvertTo-Json
Invoke-RestMethod "$base/IAuthBiz/register" -Method Post -ContentType application/json -Body $credentials
$login = Invoke-RestMethod "$base/IAuthBiz/login" -Method Post -ContentType application/json -Body $credentials
$headers = @{Authorization="Bearer $($login.body.accessToken)"}
Invoke-RestMethod "$base/IBookBiz/queryBook/page" -Method Post -ContentType application/json -Body '{"pageCurrent":1,"dto":{"title":"Java"}}'
$loan = Invoke-RestMethod "$base/ILoanBiz/borrowBook" -Method Post -Headers $headers -ContentType application/json -Body '{"bookId":1}'
Invoke-RestMethod "$base/ILoanBiz/queryCurrentLoan/page" -Method Post -Headers $headers -ContentType application/json -Body '{}'
$returnBody = @{id=$loan.body.id} | ConvertTo-Json
Invoke-RestMethod "$base/ILoanBiz/returnBook" -Method Post -Headers $headers -ContentType application/json -Body $returnBody
```

## 数据一致性及 MySQL 5.7 适配

三张表为 users、books、loans。用户名和 ISBN 唯一；Loan 的 STORED 生成列 `active_book_id` 仅在未归还时等于 book_id。`UNIQUE(user_id,active_book_id)` 限制同用户同书的活跃借阅，允许多次历史借阅。

MySQL 5.7 不执行 CHECK。根据用户确认的版本调整，以四个 BEFORE INSERT/UPDATE 触发器拒绝非法库存和借阅时间。触发器 `SIGNAL SQLSTATE '45000'` 中止当前语句，业务事务继续向外抛出异常并回滚。此变更替代原设计的 CHECK，不更改接口语义。Flyway V1 建表及触发器，V2 插入种子数据；本次迁移未更改这两个已执行脚本，也未改变数据库表结构。

借阅使用 READ_COMMITTED 事务：先锁定 User，再检查重复借阅和最多 5 本额度，然后条件扣减 Book 库存，再插入 Loan。归还先普通查询并验证归属，锁定 User 后重新查询 Loan，条件标记归还，再条件恢复库存。MyBatis 设置 `local-cache-scope: STATEMENT` 并关闭二级缓存，保证加锁后重新读取最新状态；库存与归还状态使用 XML 条件 UPDATE 并检查影响行数。任一步失败整笔回滚。所有新增业务路径必须遵循 User 先锁协议，不得绕过 Service。

InnoDB 外键、唯一索引可能引入额外锁，因此不承诺完全无死锁。发生死锁或锁超时应回滚；本版本不自动重试。跨表不变量为 `total_copies = available_copies + 当前未归还 Loan 数`，测试会验证。用户借阅上限由用户行锁协议保障，数据库管理员绕过业务直接写库不受此协议保护。

未实现 Idempotency-Key。借阅响应超时后应先查询 `/ILoanBiz/queryCurrentLoan/page` 确认，再决定重试；重试可能返回 409。重复归还返回 409，不会重复恢复库存。

## Docker Compose

需要先安装并启动 Docker。复制 `.env.example` 为 `.env`，填写独立随机 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD` 及 Base64 `JWT_SECRET` 后执行：

```bash
docker compose up --build -d
docker compose logs -f app
docker compose down
```

数据库不发布宿主机端口，应用仅绑定回环地址。数据库使用命名卷持久化；Compose 中使用通用示例库名和账号，实际连接参数通过环境变量注入。Compose 镜像构建与运行需在具备 Docker 的环境中验证。

## 已知限制

当前版本适配 MySQL 5.7。未实现限流、刷新令牌、主动注销撤销、邮件验证、RBAC、文件上传、在线阅读或生产监控。JWT HS256 有效期 2 小时，验证签名、算法、issuer、主体及签发/过期时间。无 Cookie Bearer 认证关闭 CSRF；生产部署需要 HTTPS、独立密钥管理、备份和升级维护方案。MyBatis-Plus 分页使用 MySQL 方言，Instant TypeHandler 明确按 UTC 读写 DATETIME(6)；后续升级框架时仍需重新验证。

后续可扩展管理员目录维护、审计、限流，以及更高版本数据库；新增库存修改路径须重新评审事务锁协议和并发测试。

## 后端目录与开发约定

```text
业务域（auth / book / loan / user）
  controller/*BizController
  application/service/*AppService
  domain/bo/*BO
  domain/vo/*VO
  domain/entity/*TPO
  domain/service/I*TableService
  domain/service/impl/*TableServiceImpl
  infrastructure/mapper/I*TableMapper
java/com/rxs/library/业务域/infrastructure/mapper/xml/I*TableMapper.xml
```

各业务域按实际需求提供上述层次。Controller 继承 AbstractController；AppService 编排业务与事务；TableService 继承 IService、ServiceImpl，Mapper 继承 BaseMapper。新增/读取/更新/删除采用 MyBatis-Plus CRUD，查询条件使用 Lambda Wrapper；行锁、库存及归还原子操作采用具名 XML SQL。数据库生成列不作为可写实体字段。

BO 是请求条件，VO 是对外响应，TPO 不直接返回给前端。实体使用 Lombok Data 与链式访问，身份认证服务使用构造器注入，密码哈希排除在 JSON 和 toString 之外。只读查询声明只读事务；写事务明确回滚范围。按现有功能提供查询、注册、借还接口，不扩展未经设计的公开数据删除接口。

详见 [迁移说明](docs/mybatis-plus-migration.md)。

## 中英文国际化

右上角“语言 / Language”可切换简体中文与 English，默认中文。语言偏好保存在 localStorage，刷新后保留；登录态仍保存在当前标签页的 sessionStorage。页面导航、按钮、弹窗、表单校验、业务错误、网络错误及日期显示随语言切换。图书标题、作者及用户输入保留原始内容，不自动翻译。

前端自动携带 `Accept-Language: zh-CN` 或 `Accept-Language: en`。独立调用 API 时也可设置此请求头：

```powershell
Invoke-RestMethod 'http://<应用主机>:<应用端口>/IBookBiz/queryBook/page' -Method Post -ContentType application/json -Headers @{'Accept-Language'='en'} -Body '{}'
```

后端保持 HTTP 状态和业务 code 稳定，只翻译 msg。英文地区变体（en-US / en-GB）使用英文，缺少请求头、非法或不支持的语言回退中文。响应携带 Content-Language 与 Vary: Accept-Language。

例如相同的 INVALID_TOKEN 错误对外 code 仍为 UNAUTHORIZED，中文 msg 为“登录已过期或令牌无效，请重新登录。”，英文为“Invalid or expired access token. Please sign in again.”。安全过滤器与控制器使用一致的语言解析，401/403 不会绕过国际化。

资源与扩展方式见 [国际化说明](docs/internationalization.md)。代码注释继续使用中文。

后端业务提示统一通过消息键读取资源；参数校验也使用相同资源，直接调用接口可得到具体的中英文规则。例如密码不足 8 字符时，中文返回“密码长度必须为 8–72 个字符。”，英文返回“Password must be 8–72 characters long.”，业务码均为 VALIDATION_ERROR。成功和分页提示由 LocalizedResultFactory 在构造结果时生成。


## XML 排版与错误枚举

所有 XML 统一 UTF-8、LF、四空格缩进，Mapper SQL 的列、条件和更新分别成行。XML 基础空白检查纳入 Spotless；具体约定见 docs/java-style.md。

错误集中定义在 LibraryErrorCode，绑定对外 code、国际化资源键和 HTTP 状态；业务代码使用 `throw new ApiException(LibraryErrorCode.OUT_OF_STOCK)`。成功结果使用 SuccessCode。对外错误码与中英文提示内容保留，资源键统一命名空间，前端调用协议保持兼容。
