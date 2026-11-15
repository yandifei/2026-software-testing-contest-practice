# 案例：2026-11-15 省赛（contest 35）全记录

> 用途：方法论的实证锚点。任何"这条经验在别处也成立吗"的问题，先回到这里对账：条件、数字、失败尝试都写清楚，不把单题结论固化成通用规则。

## 1. 事实

| 项 | 值 |
| --- | --- |
| 赛事 | 2026软件测试大赛-开发者测试-个人-省赛（mooctest contest 35，DEVELOPER_TEST，NATIONAL），2026-11-15 14:00–18:00 +08:00 |
| 题目 | ① `2026-开发者-CorCard-省赛`（card-approval，135）：OA 考勤/请假/任务/报销/权限/审批 + 信用卡校验，103 个类、PIT 1320 变异、分支 2013；② `2026-开发者-Math-省赛`（math-assessment，136）：在线教育业务 + 内嵌数学表达式引擎（`expression.engine` 55 个类），133 个类、PIT 2528 变异、分支 2659 |
| 评分权重 | 每题 mutation 25 / branch 25 / method 25 / statement 25；每题占 50% |
| 交付 | 每题 `StudentTest.java`（≤2MB）+ 项目 ZIP（≤20MB，首层含 `target/pit-reports`、`target/site/jacoco`） |
| 环境 | JDK 21.0.9（系统 `JAVA_HOME` 指向 Java 8，需显式指定）、Maven 3.9.11、JUnit 5.10.2、Mockito 5.12.0、JaCoCo 0.8.11、PIT 1.15.3 |

## 2. 最终成绩（本地实测，平台未出分）

| 指标 | Card | Math |
| --- | --- | --- |
| 用例（全绿） | 258 | 588 |
| METHOD | 275/275 = **100%** | 715/725 = 98.62% |
| LINE | 2360/2364 = **99.83%** | 3993/4050 = 98.59% |
| BRANCH | 2003/2013 = **99.50%** | 2616/2659 = 98.38% |
| PIT KILLED | 1296/1320 = **98.18%** | 2427/2528 = **96.00%** |
| 残余 | SURVIVED 23、NO_COVERAGE 1 | SURVIVED 70、NO_COVERAGE 27、TIMED_OUT 4 |
| 测试文件 | 8,341 行 / 476,181 B | 11,734 行 / 660,451 B |

起点：骨架测试时 Card 只有 12/1320、Math 87/2528。

## 3. 演化过程（每一跳的原因）

| 时点 | 动作 | 数字 |
| --- | --- | --- |
| 14:24–14:25 | （另一个工具会话抢同一个侧边浏览器提交了它的版本） | 被后续覆盖 |
| 14:51 / 15:00 | 第一版：公开 API 广覆盖 | Card 1216/1320、Math 1423/2528 |
| 15:11 | Math 合并引擎 A 组 probe（163 用例） | Math 2397/2528、LINE 98.4% |
| 15:18 | Math 合并引擎 B 组 probe（59 用例） | 2397→（后续 2418） |
| 15:22–15:26 | Card 合并补杀 probe（75 用例） | 1216→1285 |
| 15:47 | Card 分支专项（63 用例） | BRANCH 95.43%→99.30%，1285→1286 |
| 16:00–16:18 | Math 分支专项 + 定向补杀（+13 用例） | BRANCH 94.1%→98.38%，KILLED→2427 |
| 16:22–16:25 | Card 最后一轮（+11 用例，含 `-0.0`、反射私有静态、mock record） | 1286→1296，BRANCH→99.50% |

## 4. 真正产生杀死的手段（按产出排序）

| 手段 | 具体做法 | 产出 |
| --- | --- | --- |
| 精确边界输入 | 退款时间窗 168h/336h/720h **恰好等于**边界；请假天数 3/5/10/30；加班 240/360/361；金额 2000.00/6000 | Card AdvancedLeavePolicy +19、Math RefundPolicy +6 |
| 枚举/角色矩阵 + **断言 reasons 精确列表** | `AccessPolicy` 7 个角色 × 资源类型 × 动作，断言 `decision.reasons()` 的**内容与顺序**（只断言 `allowed` 会被下游"另一条拒绝"顶包） | Card AccessPolicy 48→0 |
| 反射直调私有/包私有方法 | `roundHalfDay(-0.0)`、`validateUnit(req, 0.25d, list)`、`positiveMinutes(18:00, 09:00)`、`roundDownToHalfHour(-40)`、`percentageOrDefault(150)`、`calculateProcessingFee(attended=5,total=0)` | Card +8、Math +5 |
| record/mock 构造非法状态 | `mock(ExpenseItem)` 造 MEAL+attendeeCount、`mock(ExpenseClaim)` 造 billable+空成本中心、`mock(WorkflowSnapshot)` 造 terminal=false+WITHDRAWN；Math 用 `AnswerResult(passed>total)`、`rubricPercent=125` | Card +4、Math +4 |
| 对象同一性与 scale | 断言 `money()` 归一化**之前**的 `toString()/scale()`；`assertSame/assertNotSame` | Math +2 |
| `-0.0` 区分边界变异 | `<`→`<=` 在 `0.0` 无差异，`-0.0` 走 `Math.ceil(-0.0*2)/2` 得 `-0.0`（符号位可观察） | Card +1 |
| NaN/溢出构造 | `sumOfRange(start==end==Double.MAX_VALUE)`、`step=NaN`、两个 `Double.MAX_VALUE` 相加溢出为 Infinity | Math +3 |
| 下游异常消息 / 兜底排除 | 断言异常消息原文，避免"下游构造器抛同类型异常顶包" | Card 多处 |

**没有采用**（题包《作弊与违规说明》第 2 点禁止"干预函数返回结果、使程序进入原实现无法到达的路径"）：`mockConstruction`、`mockStatic/MockedStatic`、字节码替换、mutant 探测、改报告/POM。

## 5. 残余项（证明而非"再试试"）

| 题 | 类别 | 代表项 |
| --- | --- | --- |
| Card | 等价 15 | `Verve` 8（重复扫描死代码）、`Luhn` 3（取负模 10 同真值）、`Maestro/MaestroUK` 6（循环重复赋值）、`ExpensePolicy.cap.signum()==0`（cap 最小 80）、`Validator` `if(!isValid) throw` 之后恒 true、`TypeChecker.checkType` 提前返回 OTHER |
| Card | 不可达 9 | `Shift` 紧凑构造器读未赋值的 `this.end` → 任何 NIGHT 班次构造即 NPE，NIGHT 分支永不可达；`ExpensePolicy` clientBillable 必须带 costCenter；`AdvancedLeavePolicy` `(0,0.5)` 空域；`switch` 覆盖全部枚举常量的 default |
| Math | 不可达 ~41 | 促销/优惠券区间倒挂死区（`today` 必 `isAfter(endsOn)`，先被 expired 命中）；`validate` 前置使 `percentage>100` 不可达；章节 `maximum.signum()==0`；枚举 switch default；引擎动态编译出口（生成源码 import 不存在的包，必抛） |
| Math | 等价 ~30 | `money()` 归一化后值相同；`(2·MAX)` 溢出只在单侧；短路死操作数（`length ≤ sb.length()` 恒真） |
| Math | 未解决 | 已覆盖但未构造出区分输入，如实保留（附录 §3） |

## 6. AI 执行中的矛盾与错误（本轮真实发生）

| # | 错误 | 表现 | 根因 | 已固化的对策 |
| --- | --- | --- | --- | --- |
| 1 | **方向性误判**：以为只拼变异+分支 | 前 3 小时按"变异/分支"排优先级，忽略了 method/statement 各 25 分 | 只读页面文案，没读 `/api/contests/{id}` 的 `scoringWeights` | 开局第一步就取权重（`scoring-and-submission.md` §1） |
| 2 | **合并脚本写坏文件** | 合并后编译报"需要 class、interface、enum 或 record"，末尾多一个 `}` | 花括号计数没跳过字符串/注释字面量 | 状态机解析 + 合并后必须编译（`multi-agent-integration.md` §4） |
| 3 | **把别名类一起合并** | 用例数正好翻倍 | probe 末尾的 `class XxxTest extends Xxx {}` 被当成员搬入 | 只取第一个顶层类体 |
| 4 | **同一 target 并发跑 PIT** | `mutations.xml` 变成两个报告的拼接体（中间出现 `</mutations>` 后接内容），XML 解析器报 mismatched tag | 两个 PIT 同时写同一路径 | 一次只跑一个 PIT；跑完立刻按时间戳复制快照；解析用容错脚本 |
| 5 | **半写报告当真** | 某次报告只有 839/1320 条变异，被误读为"杀死率 96.78%" | PIT 运行中/被外部进程占用时文件不完整 | 报告必须"条数=期望分母"才采信；否则重跑；`mvn clean` 被锁时改用 `mvn -B test` |
| 6 | **外部覆盖导致成果丢失** | 用户重新解压原始包覆盖工作副本，两个执行者 20 分钟成果被还原成骨架（69/81 行） | 没有版本控制、没有快照 | 每 20 秒轮询测试文件哈希做快照（`_snapshot/`）；每轮大改前手工 `.bak` |
| 7 | **共享浏览器被别的会话抢走** | `sidebar command failed: guest 2 is not held by this conversation`；随后发现平台上有非本会话的提交 | 多个 AI 工具会话共用同一个侧边浏览器 | 会话复位 + 重开页面；提交前读 `submittedAt`/大小确认归属；收尾再交一次 |
| 8 | **环境变量陷阱** | Maven 默认用 Java 8 跑，enforcer 会直接失败 | 系统 `JAVA_HOME` 指向 Java 8，JDK 21 在别处 | 每次构建显式 `JAVA_HOME`/`PATH`（`workflow.md` §1） |
| 9 | **中文 ZIP 条目名乱码** | 解压出 `�����߲��Ա���Ҫ��.md` | ZIP 条目名 GBK、解压按 UTF-8 | `extract_zip.py` 的 cp437→gbk 回退 |
| 10 | **误把"覆盖到"当"杀死"** | 大量 SURVIVED 在已覆盖代码上（testsRun 正常） | 断言太弱（只断言 `allowed=false`，被另一条拒绝顶包） | 断言可观察面：reasons 精确列表、异常消息原文、scale/toString、对象身份 |

## 7. 未解决/待研究

1. 平台 statement 用 JaCoCo LINE 还是 Clover？本地只能给代理值，报告须标注口径（本轮用 LINE 代理）。
2. 平台是否用比 DEFAULTS 更强的算子集？本轮未做 STRONGER 复测；若时间允许，应在副本 POM 上复测，暴露 `EQUAL_IF` 类可杀点。
3. `TIMED_OUT` 4 项是死循环变异，PIT 归属只认主线程（见 `error-prevention.md` §11），未强行转 KILLED，如实保留。
4. 引擎动态编译出口（`DynamicFunctionCompiler` 生成源码 import 不存在的包）导致一大批 return 不可达——这是被测工程自身的构建缺陷，不是测试缺口；应写进交付说明而不是硬凑。

## 8. 另一执行线（glm 工作区）的并行记录与互补结论

同一时段另一条执行线（glm 工作区，独立 target）采用**不同管线**，结果互补。数字并排不是排名，而是"两种策略的实测对照"：

| 指标 | 集成线（本文 §2） | glm 线（移植+并行分段） |
| --- | --- | --- |
| Math 用例 / KILLED | 588 / 2427（96.0%） | 1084 / 2330（92.2%） |
| Card 用例 / KILLED | 258 / 1296（98.2%） | 889 / 1265（95.8%） |
| Math 分支 | 2616/2659（98.4%） | 2617/2659（98.4%） |
| Card 分支 | 2003/2013（99.5%） | 1996/2013（99.2%） |

glm 线管线的可复用部分：

1. **题包家族哈希对比先行**：新题常是旧题扩展。逐文件 `cmp` 新旧 `src/main`（math-assessment 49 个旧类中 43 个逐字节相同；card-approval 103 个中 53 个相同），旧套件整体移植，只重写差异类的测试——一行代码没写就把 Math 铺到 326 个旧用例。
2. **模块分段并行生产**：六个并行子代理各写一个自包含段文件（新业务模块），纪律为"只 javac 自验、不跑 maven（避免并发写 target）、交付后允许继续修订但集成以最新文件为准"；集成用横幅定位的容器替换合并（`@Nested` 嵌套容器隔离辅助方法与生命周期钩子）。
3. **引擎残项四轮递进**：函数框架新类（旧题是精简桩）从 0 覆盖起步，四轮补杀把 engine 分支推到 824/850，并固化一批**题包自带缺陷/怪癖**为刻画测试（见下表）。
4. **跨会话变异级对账**（详见 `multi-agent-integration.md` §8）：定位出 engine +92 / refund +13 的移植清单——这是 glm 线变异分偏低（92.2%）的直接原因与后续上限所在；**用例多 ≠ 杀死多**，两线并集才是上限。

glm 线固化的题包怪癖/缺陷目录（刻画测试的素材，同族题可直接对照）：

| 位置 | 行为 | 处置 |
| --- | --- | --- |
| `FunctionPackage.moduleLength` | 循环内单参调用 `pow` → 有依赖时 AIOOBE、无依赖时抛带函数名的 UnsupportedOperationException | 两种输入都固化为刻画测试（题包缺陷，不修源码） |
| `FunctionFormulaCalculation.check` | "0" 占位符追加在缓冲区**末尾** → 任何函数调用都被拒（"空字符串"）；但 `sin( 1 )` 因内切片带空格反而通过 | 不一致行为逐个固化 |
| `FastSumOfIntervalsBrackets` | `CurrentOwner` 不随共享池开关清理，`isCache` 在关池后仍 true | 固化 + 注释 |
| 括号公式路径的 `dayToMs(2)` | `String.valueOf` 产生 `1.728E8`，括号解析器拼接成 **1.7288** | 科学计数法拼接 quirk 固化 |
| `BooleanCalculation.setCalculation(enum)` | 对 `booleanCalculation2` 常量抛 ClassCastException（它不是 NumberCalculation） | 固化 + 常量可用侧覆盖 |
| `TimeWindow` | 无紧凑构造校验，`start=null` 可达（与"record 必拒 null"的直觉相反） | 验证分支可达性前先读记录定义 |
| `ManyToOneNumberFunction` 构造器 | 名称只收纯字母——含数字的组件名（测试用的 "p11solver" 等）构造即抛 | 测试内组件命名只用字母 |
| `ExpressionFunction.parse` | 拒绝消息两种格式（`[y]` 与 `[y or y)]`，取决于参数是否在表达式末尾） | 消息原文断言覆盖两分支 |
| 旧套件注册表重置 | 全框架版注册表字段名是 `STRING_CALCULATION_HASH_MAP`/`STRING_FUNCTION_HASH_MAP`（精简桩版是 `CALCULATIONS`） | 移植先 diff 重置钩子 |
| 引擎动态编译 | 依赖缺 `mathematicalExpression` 包 → `DynamicFunctionCompiler` 必抛"动态函数生成失败"，与其相关的 compile 路径按**保证失败**测试；探针先行（见防错 §25） | 平台同 pom → 同失败，稳定可测 |

glm 线独有的错误（其余与本文 §6 重合，已并入防错 §23–§25）：段合并后未删独立副本导致 PIT 报"幽灵测试类"未过绿套件检查（两轮 PIT 白跑）；子代理交付后又修订文件，合并快照过期挂 2 例；八次"读源码推算期望值"被探针推翻。
