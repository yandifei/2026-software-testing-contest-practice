# 2026-11-15 省赛：合并增强与并行补测（GPT rewrite 线）

本文记录一条可复现的执行线：把同源工程的另一份测试整体并入短套件，再按 PIT 缺口并行补测，最后全量复核。所有数字来自本机实际构建，源码/POM 全程未改。**它是案例，不是上限，也不能直接搬到新题包。**

## 1. 条件

- 工作区 `B:\软件测试大赛\省赛\GPT`，两工程：`math-assessment`（简称 A）与 `rewrite/math-assessment`（简称 B）。
- 两工程 `src/main`（134 个文件）与 `pom.xml` **逐字节相同**，差异仅在 `src/test/java/net/mooctest/StudentTest.java`。
- 构建：JDK 21（`mvn -version` 显示 Maven 默认 JDK 8，必须在当前进程设 `JAVA_HOME`），Spring Boot 3.5.16 parent，JUnit 5.10.2，Mockito 5.12.0，PIT 1.15.3，mutator `DEFAULTS`，`targetClasses=net.mooctest.*`。
- 数据集：2528 个变异，2659 个 JaCoCo 分支，4050 行。

## 2. 三阶段实测

| 阶段 | 测试数 | JaCoCo 行 | JaCoCo 分支 | PIT KILLED | NO_COVERAGE | 全量 PIT 耗时 |
| --- | --- | --- | --- | --- | --- | --- |
| B 原始 | 49 | 79.98% | 83.94% | 1725（68%） | 364 | ≈6 分钟 |
| A 原始（对照） | 350 | 72.67% | 79.80% | 1722（68%） | — | — |
| A∪B 合并 | 399 | 88.44% | 91.27% | 2058（81%） | 217 | ≈8.5 分钟 |
| 补门户/引擎缺口 | 410 | 91.85% | 93.31% | — | — | — |
| 并行补测后（交付） | 506 | 96.99% | 95.94% | 2374（93.9%） | 36 | ≈19 分钟 |

交付时的完整状态分解：**2374 KILLED + 114 SURVIVED + 36 NO_COVERAGE + 4 TIMED_OUT = 2528**。PIT 控制台把 TIMED_OUT 计入 detected，报 `Killed 2378 (94%)`；严格 KILLED 只认 2374，两个数必须分开写。Test Strength = 2374/(2528−36) ≈ 95%。

按包（KILLED，合并前 → 交付后）：`course.web` 1/7 → 7/7；`course.service` 22/31 → 31/31；`course.domain` 2/4 → 4/4；`config` 0/1 → 1/1；`expression.engine` 550/1121 → 1012/1121；`grading.service` 242/291 → 280/291；`scheduling.service` 197/231 → 231/231；`enrollment.service` 185/206 → 204/206；`refund.service` 165/204 → 192/204；`pricing.service` 153/199 → 186/199。

## 3. 收益从哪里来

1. **同源合并**（+333 KILLED，分支 +7.33 个百分点）：A 的 350 个测试整体并入 B，零重设计。合并前按包取最大值相加估出上界 1813，实测并集 2058——聚合估值是**下界**，不能当上限（见 [研究](coverage-mutation-research.md) 第 11 节）。
2. **NO_COVERAGE 补测**（399 → 410）：门户 API（12 个枚举组件、注册/注销/文件与流序列化）、注册表簿记、动态类加载、函数包依赖诊断、区间组件、字面量比较分支。分支 91.27% → 93.31%。
3. **SURVIVED 边界断言**（410 → 506，四路并行）：方程式求解器 43→41 杀；复数/函数表达式 40→38 杀；定价+退款 75→21 存；排课+报名+判分 45→13 存。这批几乎全部是 `ConditionalsBoundaryMutator`（阈值相等）与 `RemoveConditionalMutator_EQUAL_ELSE`（条件恒假），只有精确边界输入 + 精确输出断言才能杀死。

## 4. 途中失败与恢复

| 现象 | 原因 | 恢复 |
| --- | --- | --- |
| 所有 shell 命令无法执行（`Win32 5 grantWrite`） | 工作区目录缺少 WRITE_OWNER，DSH 无法下放写权限 | 用沙箱 ACL 诊断/修复脚本补当前用户完全控制；随后会话转为完全权限 |
| `skill` 工具报该技能不可用 | 磁盘上只有技能目录，未注册 | 直接读 `SKILL.md` 与 references，不反复重试 |
| 解析 PIT XML 全为 `None` | 把 `lineNumber`/`mutator` 当属性读，实际是子元素 | 重写解析器；先把"状态计数之和 == 总数"作为自检 |
| JaCoCo 行覆盖取到 0 条 | `<line>` 在 `package/sourcefile` 下，不在 `<method>` 下 | 改为按 sourcefile 统计 |
| `mutations.xml` 标签错乱 | 被中止的旧 PIT 子进程仍在写同一个 `target/pit-reports` | 按 `multiModuleProjectDirectory` 杀掉全部 java 进程，确认归零后重跑 |
| 用例断言"缺依赖应抛异常"却通过 | 别的测试先注册了依赖，全局 map 未被 `@BeforeEach` 清理 | 用例内显式 unregister 后再断言 |
| `register(x,true)` 断言失败 | `getInstance` 内部已自动注册 | 读实现，先 unregister 再 register |
| 拼接后编译失败 | mock 返回类型写成 `NameExpression`（实际 `PackExpression`） | 按声明改类型；拼接后一律先 `mvn test` |
| 拼接后运行失败 | 函数名 `gapstream1` 含数字，`ManyToOneNumberFunction` 只允许字母 | 改成纯字母名 |
| 白算区间乘积期望（12 应为 3） | 未核步长截断代码 | 对着 `MultiplyOfRange` 手算，并选能区分边界的输入 |
| 误以为"内部类不能有 static 成员，所以合并必然编译失败" | 该限制在 JDK 16（JEP 395）已放开；本场 JDK 21，原文件里 `@Nested` 类内本就声明了 `private static` 成员 | 正确理由是外层 `@BeforeEach` 会串进内层；语言约束一律先核对 `java -version` |

## 5. 剩余未杀死项的归属

- **环境不可达（约 20 个）**：`DynamicFunctionCompiler` 生成源码硬编码 import 了本工程 POM 中不存在的上游包，所有 JVM 动态编译入口必抛 `RuntimeException`；`JvmExpression`(10)、`JvmExpressionFunction`(8)、`JvmCalculation`(2)、`DynamicFunctionCompiler`(1)、`DynamicClassLoader.findClass`(1) 的 return 类变异与异常不可区分。归"环境不可达"，不称等价。
- **生产代码缺陷导致的死分支**：`FunctionPackage.MATH.moduleLength` 的 `pow.run(number)` 只传 1 个参数，必数组越界；`BooleanCalculation2` 的 null 字面量分支把整串公式当运算符传给比较函数，恒抛 `AbnormalOperation`。
- **等价证明**：区间求和 `start == end` 分支（变异后公式恒等于原值）、`ComplexCalculation` 第二个比较恒假、`ExamGradingService` 的 clamp 上界不可达、`AdvancedPricingService`/`RefundPolicyService` 的日期与比率非法分支不可达等。
- **TIMED_OUT（4 个）**：`NumberUtils` L218/L238、`EquationSolver` L355、`CumulativeCalculation` L96，变异体使循环不收敛；线程超时包装受覆盖归属限制，不能可靠转成 KILLED，如实保留。

## 6. 可迁移与不可迁移

可迁移：同源判定用哈希而非目录名；先量三指标再决定骨架；`@Nested` 不隔离外层钩子（JDK 16+ 已允许内部类声明 static 成员，不能靠"编译会失败"兜底）；片段文件 + 副本验证的并行协议；边界值才能杀边界变异；分层反馈控制时间成本。

不可迁移：1813/2058 这类数字、具体类名与方法、PIT 耗时（随机器与测试数变化）、本环境"动态编译必失败"的结论（换了依赖清单就可能不同）。复用前必须重新核对源码/POM/测试哈希并重新全量构建。
