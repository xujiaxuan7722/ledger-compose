# ledger-compose 技术说明书（简历 / 面试用）

> 用途：投不同岗位时，从这里取素材改写项目描述。每个技术点都按「是什么 → 项目里怎么用 → 对应文件 → 面试能讲什么」组织，最后一章给出按岗位切换的话术。
> 仓库：github.com/xujiaxuan7722/ledger-compose（私有）。本文对应 commit `da8f7eb`（2026-08-21）。

---

## 0. 一句话与硬数据

**一句话**：一个用现代 Android 技术栈（Kotlin + Jetpack Compose + Hilt + Room）从零重写的个人记账 App，覆盖记账 / 流水 / 统计图表 / 预算提醒 / 搜索筛选 / 分类账户管理 / CSV 导入导出，带 82 个自动化测试、数据库迁移测试与 CI。

| 指标 | 数值 | 备注 |
|---|---|---|
| 主代码 | 55 个 Kotlin 文件 / 约 3,450 行 | `domain` 349 · `data` 636 · `ui` 2,335 · `di` 86 |
| 测试代码 | 23 个文件 / 约 1,340 行 | 占总代码 28% |
| JVM 单元测试 | **66 个** | 领域纯函数 + ViewModel（Fake 仓库） |
| 仪器测试 | **16 个** | Room DAO 9 + 数据库迁移 1 + Compose UI 端到端 6 |
| 功能迭代 | 9 个语义化 commit | 每个 commit = 一个完整功能 + 测试 |
| 技术版本 | Kotlin 2.1.10 · AGP 8.7.3 · Compose BOM 2024.12 · Hilt 2.53 · Room 2.6.1 · Coroutines 1.10 | minSdk 26 / targetSdk 35 |

**重写背景（面试故事的开头）**：原课程设计是一个 8 个 Activity 互跳、用 SharedPreferences 存 JSON、邮箱当主键、零测试的"银行账户模拟器"。重写时先纠正需求（记账 ≠ 银行，允许超支；本地 App 不需要假登录），再建数据模型，再搭架构，最后补测试与工程化。

---

## 1. 技术栈总表

| 层 | 技术 | 在项目中的用途 | 关键文件 |
|---|---|---|---|
| 语言 | Kotlin 2.1（value class、sealed interface、data class、扩展函数、协程） | 全部代码 | — |
| UI | Jetpack Compose + Material 3 | 全部界面，无 XML 布局 | `ui/**/*Screen.kt` |
| UI | Navigation Compose | 单 Activity + 路由 + 底部导航 + 可选参数 | `ui/navigation/LedgerNavGraph.kt` |
| UI | Compose Canvas | 自绘环形图 / 分组柱状图（零第三方图表库） | `ui/statistics/Charts.kt` |
| 状态 | ViewModel + StateFlow + 单向数据流（UDF） | 每屏一个 `UiState`，UI 只读状态、只发事件 | `ui/*/**ViewModel.kt` |
| 异步 | Kotlin Coroutines + Flow（`combine`/`flatMapLatest`/`stateIn`） | 数据库变化自动驱动 UI | 所有 ViewModel |
| 依赖注入 | Hilt（Dagger） | 仓库绑定、数据库提供、调度器/时钟限定符、测试替身 | `di/*.kt`, `androidTest/di/TestDatabaseModule.kt` |
| 持久化 | Room（KSP 注解处理） | 4 张表、外键、唯一索引、`Flow` 查询、`AutoMigration`、schema 导出 | `data/local/*.kt`, `app/schemas/` |
| 架构 | 分层 + Repository 模式 + 领域模型/实体分离 | 依赖方向 ui → domain ← data | `data/*Repository.kt`, `domain/**` |
| 文件 | Storage Access Framework（SAF） | CSV 导出/导入走系统文件选择器，无存储权限 | `ui/transactions/TransactionsScreen.kt`, `data/CsvTransfer.kt` |
| 测试 | JUnit4 + Truth + kotlinx-coroutines-test | 单元测试 | `src/test/**` |
| 测试 | Room in-memory + MigrationTestHelper | DAO / 迁移测试 | `src/androidTest/data/local/*` |
| 测试 | Compose UI Test + Hilt Testing（`HiltTestRunner`、`@TestInstallIn`） | 端到端 UI 测试 | `src/androidTest/ui/*` |
| 构建 | Gradle Kotlin DSL + Version Catalog + KSP + Compose Compiler 插件 | 依赖集中管理 | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| 质量 | Android Lint、R8 混淆（release）、GitHub Actions CI | 每次 push 跑 lint + 单测 + 打包 | `.github/workflows/ci.yml` |
| 设计 | Material 3 自定义配色/Shapes、深浅色双主题、边到边（edge-to-edge）与 WindowInsets | 视觉参考原项目重设计 | `ui/theme/Theme.kt` |

---

## 2. 架构与数据流

```
ui/            Compose 屏幕 + ViewModel（UiState / 事件）        ← 依赖 domain、data 接口
domain/        纯 Kotlin：模型 + 业务纯函数（无 Android 依赖）   ← 不依赖任何人
data/          Repository 接口 + Room 实现 + 映射 + 用例（CSV）   ← 依赖 domain
di/            Hilt 模块：接口→实现绑定、数据库、调度器、Clock
```

**数据流（核心卖点）**：Room DAO 返回 `Flow<List<Entity>>` → Repository `map` 成领域模型 → ViewModel 用 `combine` 把多条流（月份、流水、分类、账户、预算）合成一个 `StateFlow<UiState>` → Compose `collectAsStateWithLifecycle()` 订阅。任何一次写入（记账、改分类、导入 CSV）会自动刷新所有正在展示的页面，**没有任何"返回时手动刷新"的代码**。这与原项目"每个 Activity 各自 new ViewModel、onResume 里重新读 SP、发广播 + 延时 300ms"形成鲜明对比。

**单向数据流（UDF）**：UI 只做两件事——渲染 `UiState`、把用户操作转成 ViewModel 方法调用；ViewModel 不持有任何 View 引用。一次性事件（保存成功/错误提示）用状态字段 + `consume*()` 模式，避免事件丢失。

---

## 3. 逐项技术详解

### 3.1 Kotlin 语言特性

| 特性 | 项目用法 | 面试点 |
|---|---|---|
| `@JvmInline value class Money(val cents: Long)` | 金额以"分"为单位的整数，运行时零开销，编译期防止把 `Long` 当金额乱传；提供 `parse()`（BigDecimal 四舍五入到分）、`format()`（千分位两位小数）、运算符重载 | 为什么金额不用 Double（浮点误差 0.1+0.2）、value class 与 data class 区别、装箱时机 |
| `data class` | 领域模型、UiState、Room 实体；`copy()` 做不可变更新 | 不可变性对 Compose 重组的意义 |
| `sealed interface UiMessage / CsvEvent` | 受限的一次性事件类型，`when` 穷尽 | 代数数据类型消灭非法状态 |
| 扩展函数 | `List<Transaction>.summarize()` / `.shareByCategory()` / `.applyFilter()`；`LocalTransaction.toDomain()` | 领域逻辑放在纯函数里便于测试 |
| 协程 `suspend` + `Flow` | 仓库接口全部是 `suspend` 或返回 `Flow` | 主线程安全（main-safe）如何保证 |
| `java.time`（Instant / YearMonth / ZoneId / Clock） | 时间统一存 UTC 毫秒，显示时按时区；`Clock` 通过 Hilt 注入，测试里 `Clock.fixed()` 固定时间 | 为什么不用 `System.currentTimeMillis()` 直写（不可测）、时区边界 |

### 3.2 Jetpack Compose（声明式 UI）

- **全部界面无 XML**：流水、记一笔、统计、预算、搜索、管理六个屏幕。
- **状态提升与可组合性**：屏幕级 Composable 只拿 `UiState` + 回调；内部拆成 `HeroCard`、`QuickActions`、`TransactionRow`、`DonutChart`、`TrendBarChart`、`MonthSwitcher` 等可复用组件。
- **Material 3 组件**：`Scaffold`、`CenterAlignedTopAppBar`、`NavigationBar`、`SingleChoiceSegmentedButtonRow`、`FilterChip`、`FlowRow`、`DatePickerDialog`、`AlertDialog`、`SnackbarHost`、`LinearProgressIndicator`、`DropdownMenu`。
- **Canvas 自绘图表**：`drawArc` 画环形占比图、`drawRect` + `nativeCanvas.drawText` 画分组柱状图；纵轴自适应最大值、横轴连续月份。**没有引入 MPAndroidChart/Vico**，减少依赖并展示对绘制 API 的掌握。
- **主题系统**：`lightColorScheme/darkColorScheme` 自定义全套 token（主色 #4272F4、浅灰背景、白卡片、浅蓝图标底）、统一 `Shapes`、渐变 `Brush`；**深浅色双主题**。
- **边到边 + WindowInsets**：`enableEdgeToEdge()`；外层 Scaffold `contentWindowInsets = WindowInsets(0)`，状态栏交给 TopAppBar、导航栏交给底部栏，避免 inset 被算两次（踩过坑，见第 5 章）。
- **生命周期感知订阅**：`collectAsStateWithLifecycle()` 配合 `stateIn(SharingStarted.WhileSubscribed(5_000))`，页面进入后台 5 秒后停止上游收集，省电省数据库查询。
- **副作用 API**：`LaunchedEffect` 处理"保存完成后返回""显示 Snackbar 后消费事件"；`rememberLauncherForActivityResult` 接入 SAF。

### 3.3 Navigation Compose

- 单 `MainActivity` + `NavHost`；路由常量集中在 `LedgerRoutes`。
- **可选参数**：`addEdit?transactionId={id}&kind={kind}`，`navArgument` 设 `nullable = true`；同一个页面承担新建/编辑/预选收支类型三种入口。
- **底部导航**：`NavigationBar` 只在顶层路由显示（用 `currentBackStackEntryAsState` + `hierarchy` 判断）；切换时 `popUpTo(startDestination){saveState=true}` + `launchSingleTop` + `restoreState` 保留各 Tab 状态。
- 参数读取：ViewModel 通过 `SavedStateHandle` 取路由参数，UI 层不关心。

### 3.4 Hilt 依赖注入

- `@HiltAndroidApp` / `@AndroidEntryPoint` / `@HiltViewModel`；ViewModel 通过 `hiltViewModel()` 获取。
- **接口绑定**：`@Binds` 把 `DefaultTransactionRepository` 绑到 `TransactionRepository` 接口（共 4 个仓库）；**对象提供**：`@Provides` 建 Room 数据库与各 DAO。
- **限定符**：自定义 `@IoDispatcher`、`@ApplicationScope`；`Clock` 作为可注入依赖（测试可换固定时钟）。
- **作用域**：仓库与数据库 `@Singleton`；`ApplicationScope = CoroutineScope(SupervisorJob() + IO)` 供 Application 启动时种子数据。
- **测试替身**：`@TestInstallIn(components=[SingletonComponent::class], replaces=[DatabaseModule::class])` 用内存数据库替换真实数据库；`HiltTestRunner` 指向 `HiltTestApplication`；测试里 `@Inject` 拿到 `DefaultDataSeeder` 种数据。

### 3.5 Room 持久化

- **4 张表**：`transactions`（外键 → categories/accounts，`RESTRICT` 删除策略；`occurred_at`/`category_id`/`account_id` 索引）、`categories`、`accounts`、`budgets`（`year_month + category_id` **唯一索引**，`category_id` 为 NULL 表示总预算）。
- **实体与领域模型分离**：`LocalTransaction`（Room 注解）↔ `Transaction`（纯 Kotlin），`ModelMapping.kt` 映射；领域层不沾 Android。
- **响应式查询**：DAO 返回 `Flow<List<…>>`；区间查询半开 `[start, end)`；`@Upsert`；聚合 `COUNT`/`MAX`。
- **版本演进**：v1 → v2 新增 `budgets` 表，用 `@Database(autoMigrations=[AutoMigration(from=1,to=2)])`；`exportSchema = true` 把 `1.json`/`2.json` 进仓库；**`MigrationTest` 用 `MigrationTestHelper` 验证旧数据保留、新表可写**。
- **引用完整性**：删除被流水引用的分类/账户前先 `COUNT` 检查，抛 `InUseException` 给 UI 友好提示；账户提供"归档"替代删除。
- **首启种子**：`DefaultDataSeeder.seedIfEmpty()` 写入 13 个内置分类、4 个账户（固定 id，便于测试与迁移）。

### 3.6 协程与 Flow 运用

- `flatMapLatest`：月份切换 → 自动取消旧月份的数据库订阅、订阅新月份。
- `combine` 最多 5 路流：月份 + 流水 + 分类 + 账户 + 预算 → 一个 `UiState`。
- `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`：冷流转热流、配置变更（旋转）不重新查询。
- `MutableStateFlow.update {}`：线程安全的不可变状态更新。
- Repository 用 `withContext(ioDispatcher)`，调度器可注入。
- **测试**：`MainCoroutineRule`（`Dispatchers.setMain(UnconfinedTestDispatcher())`），`runTest(mainRule.dispatcher)`；踩过"`stateIn(WhileSubscribed)` 没有订阅者就不计算"的坑（见第 5 章）。

### 3.7 测试体系（测试金字塔）

| 层 | 数量 | 工具 | 覆盖什么 |
|---|---|---|---|
| 领域纯函数单测 | ~25 | JUnit4 + Truth | `Money` 解析/格式化/四舍五入；月度汇总；分类占比与趋势；预算进度；搜索过滤；CSV 导出/解析（BOM、CRLF、引号转义、坏行报告） |
| ViewModel 单测 | ~30 | + Fake 仓库 + `MainCoroutineRule` + `Clock.fixed` | 六个 ViewModel：状态组合、校验顺序、切换收支清分类、月份窗口、超支横幅、编辑回填、删除 |
| 用例单测 | 2 | Fake 仓库 | CSV 导入按名称匹配/自动建分类、导出再导入计数一致 |
| DAO 仪器测试 | 9 | Room in-memory | 半开区间、排序、upsert/delete、外键拒绝、唯一索引、计数 |
| 迁移测试 | 1 | `MigrationTestHelper` | v1→v2 数据保留 |
| Compose UI 测试 | 6 | `createAndroidComposeRule` + Hilt | 记一笔端到端、校验提示、收入入口、底部导航、菜单、搜索入口 |

**Fake 而非 Mock**：`FakeTransactionRepository` 等用 `MutableStateFlow<Map>` 实现仓库接口，行为真实、可观察，比 Mockito 打桩更接近集成测试且不依赖 mock 框架。
**CI**：GitHub Actions 每次 push 跑 `lint + testDebugUnitTest + assembleDebug`，产物上传。

### 3.8 Gradle / 工程化

- Kotlin DSL；`libs.versions.toml` 版本目录统一管理 30+ 依赖；KSP 替代 kapt（Room/Hilt 注解处理更快）；Kotlin 2.x 的 Compose Compiler Gradle 插件。
- `ksp { arg("room.schemaLocation", …) }` 导出 schema；`sourceSets.androidTest.assets.srcDir(schemas)` 供迁移测试读取。
- release 开启 R8 `isMinifyEnabled + isShrinkResources`（已验证可打包）。
- Lint 0 error；`.gitignore` 正确；MIT License；README 含架构图/截图/运行方式。

### 3.9 Storage Access Framework（CSV）

- `ActivityResultContracts.CreateDocument("text/csv")` / `OpenDocument()`，通过 `contentResolver` 读写 `Uri`，**不申请任何存储权限**，兼容 Android 10+ 分区存储。
- 导出：UTF-8 BOM（Excel 直接打开不乱码）+ 表头 + RFC4180 风格转义。
- 导入：解析容忍 BOM/CRLF/引号/千分位；逐行校验，坏行收集到 `errors` 不影响好行；按名称匹配分类（区分收支类型）/账户，缺失自动创建。

### 3.10 产品与交互设计

- 视觉参考原项目：蓝色主色、渐变结余卡、三个快捷按钮、白卡片流水、绿/红金额、底部三 Tab。
- 记账路径 ≤ 3 次点击：首页「记支出」→ 金额 → 分类（chips）→ 保存（账户/日期有默认）。
- 超支提醒：流水页顶部横幅实时计算，而非推送；预算页进度条红色提示。
- 删除保护、空状态文案、错误提示本地化（全部走 `strings.xml`）。

---

## 4. 关键设计决策与取舍（面试"为什么"）

| 决策 | 选择 | 放弃的方案 | 理由 |
|---|---|---|---|
| 金额类型 | `Long` 分 + value class | `Double` / `BigDecimal` 字段 | 无精度问题、零开销、数据库存整数易聚合 |
| 时间 | `Instant` 存 epoch 毫秒，显示按 `ZoneId` | 字符串 `yyyy-MM-dd HH:mm` | 可比较、可区间查询、跨时区正确 |
| 主键 | UUID 字符串 | 邮箱/自增 | 与业务字段解耦，改名不丢数据，便于导入导出合并 |
| 模块 | 单模块按包分层 | 多模块（data/domain/ui） | 课程/个人规模下多模块是负担；依赖方向靠包约定 + Code Review 即可 |
| DI | Hilt | Koin / 手写 | 官方、编译期校验、测试替身（`@TestInstallIn`）支持最好 |
| 状态 | MVVM + UDF（单 `UiState`） | 完整 MVI 框架 | 够用且简单；避免引入 Reducer/Intent 样板 |
| 图表 | Canvas 自绘 | MPAndroidChart / Vico | 需求简单、零依赖、可完全控制样式 |
| 搜索 | 全量 `observeAll` + 内存过滤 | SQL 动态条件 / Paging | 数据量小；README 已注明大数据量应改分页（知道边界） |
| 登录 | 不做 | 用户名密码 | 纯本地 App 的假登录无安全价值；真需要用应用锁（PIN/生物识别） |
| 文件 | SAF | `WRITE_EXTERNAL_STORAGE` | 无权限、适配分区存储、用户自选位置 |
| 测试替身 | Fake | Mockito | 行为真实、可复用、测试更接近集成 |

---

## 5. 踩坑与解决（面试故事素材）

1. **`stateIn(WhileSubscribed)` 在单测里"不动"**：`runTest` 默认 `StandardTestDispatcher`，`launch { uiState.collect }` 被挂起没真正订阅，10 个测试全挂。改为 `runTest(mainRule.dispatcher)`（Unconfined）。教训：热流的启动策略和测试调度器要一起理解。
2. **Room `@Upsert` 遇唯一索引冲突不抛错**：它退化为按主键 UPDATE（0 行），静默吞掉。预算表改用 `@Insert` 让冲突显式失败，仓库层先删后插。教训：别凭直觉猜注解语义，写 DAO 测试验证。
3. **嵌套 Scaffold 状态栏 inset 算两次**：底部导航引入外层 Scaffold 后顶部多出一条空白。外层与顶层页面内层都 `contentWindowInsets = WindowInsets(0)`，把 inset 交给 TopAppBar/NavigationBar 各自处理。
4. **Compose UI 测试时序**：保存是异步写库 + 返回动画，`waitForIdle` 等不到；写 `waitUntilDisplayed()` 轮询到节点可见；同一金额在多处出现要用 `onAllNodesWithText().onFirst()`。
5. **Lint `ByteOrderMark` error 让 CI 红**：CSV 源码里直接写了 BOM 字符字面量，改为 `﻿` 转义。
6. **顶栏图标堆到 5 个把标题挤掉**：功能堆出来的 UI 债，改底部导航 + ⋮ 菜单。
7. **（环境）`unzip 6.0` 静默截断 >4GB 文件导致模拟器起不来**、Java 不读 `HTTP_PROXY`、后台脚本被"杀"后仍继续执行破坏性步骤——工程环境方面的经验。

---

## 6. 按岗位切换的简历话术

> 写法建议：**动词 + 技术 + 结果/数字**。下面每条都可直接用，按岗位挑 3–5 条。

### 6.1 Android / 移动端开发（主攻）
- 使用 **Kotlin + Jetpack Compose + Material 3** 独立开发记账 App，6 个功能页全部声明式 UI，自绘 Canvas 环形图/柱状图，支持深浅色主题与边到边布局。
- 采用 **MVVM + 单向数据流** 架构：Room `Flow` → Repository → ViewModel `combine`/`stateIn` → Compose，实现全局数据自动同步，消除手动刷新逻辑。
- 用 **Hilt** 完成依赖注入与测试替身（`@TestInstallIn` 内存库），**Room** 设计 4 表（外键/唯一索引/`AutoMigration`），并编写数据库迁移测试。
- 建立测试金字塔：**66 个单元测试 + 16 个仪器测试（DAO/迁移/Compose UI 端到端）**，GitHub Actions 持续集成，Lint 0 error，R8 混淆打包。
- 通过 **SAF** 实现零权限 CSV 导入导出；Navigation Compose 单 Activity 多路由 + 底部导航 + 可选参数。

### 6.2 Kotlin / Java 后端、服务端开发
- 领域驱动的分层设计：纯 Kotlin `domain` 层（无框架依赖）承载业务规则（金额 value class、月度汇总、预算进度、搜索过滤、CSV 解析），`data` 层实现 Repository 接口，依赖单向。
- 熟练使用 **Kotlin 协程与 Flow**（`flatMapLatest`/`combine`/`stateIn`、结构化并发、可注入调度器），理解冷/热流与生命周期。
- **Dagger/Hilt** 依赖注入：接口绑定、限定符、作用域、测试模块替换。
- 数据层：SQLite/Room 表设计（外键、索引、唯一约束、半开区间查询、聚合）、schema 版本化与迁移测试；金额/时间建模最佳实践（分 + UTC）。
- 工程化：Gradle Kotlin DSL、版本目录、KSP、CI、语义化提交；82 个自动化测试，Fake 优于 Mock 的测试策略。

### 6.3 测试开发 / QA 自动化
- 为 Android 应用搭建完整自动化测试体系：**JUnit4 + Truth** 单测 66 个（纯函数、ViewModel，Fake 仓库 + 固定 `Clock` 保证可重复），**Room in-memory** DAO 测试 9 个，**MigrationTestHelper** 迁移测试，**Compose UI Test + Hilt** 端到端测试 6 个。
- 设计可测架构：依赖注入 `Clock`/`Dispatcher`、接口化仓库、`@TestInstallIn` 替换数据库、`HiltTestRunner`。
- 解决异步 UI 测试稳定性问题（自定义 `waitUntilDisplayed` 轮询、多节点断言），解决协程测试调度器导致的假失败。
- CI 集成（GitHub Actions：lint + 单测 + 打包），测试覆盖核心业务规则（金额舍入、时区边界、外键约束、唯一索引、CSV 坏行处理）。

### 6.4 前端 / 跨端 / UI 工程
- 声明式 UI 思维（与 React/Flutter 同源）：组件化、状态提升、不可变状态、副作用隔离（`LaunchedEffect`）、生命周期感知订阅。
- 设计系统落地：自定义 Material 3 色彩 token、Shapes、深浅色主题、渐变与阴影、空状态/错误态文案；参考旧版视觉完成重设计并保持一致性。
- 自绘数据可视化（环形图、分组柱状图，自适应刻度）；响应式布局（`FlowRow`、`LazyColumn`、WindowInsets）。
- 端到端 UI 自动化测试；路由与导航状态管理（Tab 状态保存/恢复、可选参数）。

### 6.5 产品 / 数据分析 / 业务方向（技术作为加分）
- 从"需求纠偏"出发：识别原方案把记账做成"银行账户模拟器"、假登录等伪需求，重新定义核心价值（记得快 / 看得清 / 管得住），并据此设计数据模型与功能优先级（MVP → 统计 → 预算 → 管理 → 搜索 → 导入导出）。
- 设计并实现收支统计（分类占比、6 个月趋势）、预算执行与超支提醒、多维筛选；数据可导出 CSV 供二次分析。
- 以数据驱动迭代：9 次语义化交付，每次附测试与截图；用截图与指标支撑答辩/汇报。

### 6.6 一段式项目描述（通用模板，约 120 字）
> **记账本 App（ledger-compose）** | Kotlin · Jetpack Compose · Hilt · Room · Coroutines/Flow | 2026.08
> 独立完成从需求纠偏、数据建模到上线级工程化的全流程：MVVM + 单向数据流，Room 4 表 + 自动迁移，Compose 全声明式 UI 与 Canvas 自绘图表，SAF 零权限 CSV 导入导出；66 个单测 + 16 个仪器测试（含 Compose UI 端到端、数据库迁移测试），GitHub Actions CI，Lint 0 error。

---

## 7. 高频面试问题与要点

| 问题 | 要点 |
|---|---|
| 为什么用 Compose 不用 XML？ | 声明式、状态驱动、组件复用、更少样板；Material 3 一等公民；结合 Flow 天然响应式。也会提 XML 的成熟生态与互操作。 |
| MVVM 和 MVI 的区别？你用的是？ | 用的 MVVM + UDF：单 `UiState`、事件方法；MVI 多了 Intent/Reducer 的约束，本项目规模下是负担。 |
| `stateIn` 的三种 `SharingStarted` 区别？ | `Eagerly`/`Lazily`/`WhileSubscribed(timeout)`；为什么选 5 秒（配置变更不重查、后台省资源）。 |
| `flatMapLatest` 与 `flatMapMerge/Concat`？ | 月份切换要取消旧订阅 → Latest。 |
| Room 迁移怎么做？ | `exportSchema` + `AutoMigration`（简单 DDL）/手写 `Migration`（复杂）；`MigrationTestHelper` 验证。 |
| 为什么金额用 Long？ | 浮点误差、聚合精度、数据库整数索引；展示层再格式化。 |
| Hilt 里 `@Binds` 和 `@Provides` 区别？ | 接口→实现（抽象方法，零开销）vs 需要构造逻辑的对象。 |
| 如何保证 UI 测试稳定？ | 内存库隔离、固定种子数据、轮询等待代替 sleep、语义匹配（文本/contentDescription）。 |
| 大数据量搜索怎么优化？ | SQL 条件下推 + Paging 3 + FTS；当前实现的边界清楚。 |
| 这个项目最难的地方？ | 讲第 5 章任意一条（推荐 `@Upsert` 语义或 `stateIn` 测试坑），说清现象→定位→修复→教训。 |
| 还能怎么改进？ | 删除撤销、账户间转账、预算结转、周期记账（WorkManager）、应用锁（BiometricPrompt）、Paging、Compose 截图测试。 |

---

## 8. 文件索引（快速定位代码）

```
app/src/main/java/com/dwt/ledger/
├── LedgerApplication.kt / MainActivity.kt
├── domain/model/   Money · Transaction · Category · Account · Budget · TransactionKind
├── domain/logic/   MonthlySummary · MonthRange · Statistics · BudgetProgress · TransactionFilter · Csv
├── data/local/     LedgerDatabase · LocalTransaction/Category/Account/Budget · 4 个 DAO
├── data/           *Repository 接口与 Default 实现 · ModelMapping · DefaultDataSeeder · CsvTransfer
├── di/             CoroutinesModule(IoDispatcher/ApplicationScope/Clock) · DataModules
└── ui/             theme · navigation · common(Icons/Formatters/MonthSwitcher)
                    transactions · addedit · statistics(Charts) · budget · search · manage · datatransfer
app/src/test/        16 个测试类（FakeRepositories、MainCoroutineRule）
app/src/androidTest/ HiltTestRunner · di/TestDatabaseModule · data/local(DAO/Migration) · ui(UI 测试)
app/schemas/         Room schema 1.json / 2.json
.github/workflows/   ci.yml
docs/                screenshots/ · TECH_NOTES.md（本文）
```
