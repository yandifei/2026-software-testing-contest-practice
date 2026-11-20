# 2026-11-15 省赛：跨套件变异身份对账（相互借鉴2 合并线）

本文记录一条与 [合并增强线](case-2026-11-15-merge-sweep.md) 不同的执行线：手上已有四份**同源**测试套件（GPT / deepseek / glm / 相互借鉴），任务不是"把 A 并入 B"，而是**判断合并版有没有丢东西、别家高分能不能借、以及一个更高的外部成绩差在哪**。所有数字来自本机实际构建，`src/main` 与 `pom.xml` 全程未改。

**核心结论：合并版的"剩余项全部等价"不能当证据；用变异身份集合对账，这一轮查出 11 条（card）与 8 条（math）已经被别的套件杀死的变异被错误地归入"等价"。**

## 0. 本轮五条线的对照（严格 KILLED，非 PIT 首页 detected）

| 执行线 | card | math | 说明 |
| --- | --- | --- | --- |
| GPT rewrite | 1123/1320 | 2374/2528 | math 含 10 处 `mockStatic` + 1 处 `mockConstruction`；card 用例过少 |
| deepseek | 1296/1320 | 2427/2528（+4 超时） | math 0 mock，业务+引擎最均衡；两题都是单套最强 |
| glm | 1265/1320 | 2324/2528（+6 超时） | 用例数最多（815/1084）但断言弱；math 有 2 处连续 stub |
| 相互借鉴（上一合并版） | 1293/1320 | 2419/2528（+4 超时） | 合并时丢了 11/8 条已杀死用例（见第 2 节） |
| **相互借鉴2（本线）** | **1299/1320** | **2436/2528（+3 超时）** | 补回不依赖伪造状态的缺口；剩余项逐条给出等价或环境不可达证据 |

口径提醒：PIT 首页的 "Killed" 含 TIMED_OUT，严格 KILLED 不含；两者必须分列。上表用严格 KILLED。

## 1. 条件

- 工作区 `B:\软件测试大赛\省赛`，五份项目：`GPT\rewrite`、`deepseek`、`glm`、`相互借鉴`（合并版）、`相互借鉴2`（本线交付）。
- 五份 `src/main`（card 103 文件 / math 134 文件）与 `pom.xml` **逐字节相同**：
  card `B6E1C542186340FD` / `B93789CB0B1B7ABE`；math `960DC91F85EEAD96` / `9A1AECCF19C94109`。
  因此五份报告可以按同一变异身份集合比较，不需要重跑。
- 构建：JDK 21、Maven 3.9.11、JUnit 5.10.2、PIT 1.15.3、mutator `DEFAULTS`、`targetClasses=net.mooctest.*`。
- 数据集：card 1320 个变异 / 2013 分支；math 2528 个变异 / 2659 分支。

## 2. 对账矩阵（脚本实测）

`python -B scripts/mutation_union.py --provider ... --baseline <合并版>`：

| 项目 | 并集 | 全部套件都杀 | 无人杀死 | 合并版 KILLED | 最强单套 KILLED | 合并版漏杀 |
| --- | --- | --- | --- | --- | --- | --- |
| card | 1320 | 1098 | 16 | 1293（相互借鉴） | 1296（deepseek） | **11** |
| math | 2528 | 2419 | 101 | 2419（相互借鉴） | 2427（deepseek） | **8** |

card 的 11 条里 10 条来自 deepseek 的 `CardProbeTests3`（`probe3PositiveMinutesClampsReversedSpan`、`probe3RoundHalfDayNegativeZeroBoundary`、`probe3ValidateUnitMinimumCharge`、`probe3EnsureActiveRejectsUnknownActiveState` 等），1 条来自 GPT 的 `parserChecksumAndDateUtilities`；math 的 8 条全部来自 deepseek 的 `MathProbeTests`（`numberSumOfRangeSameExtremeDouble`、`refundFourteenDayProgressEdge`、`refundProcessingFeeIgnoresZeroTotalSessions` 等）。

也就是说：**合并版不是"杀不动"，是把已经杀掉的用例删了，再把删掉造成的缺口写成等价证明。**

## 3. 两类"假缺口"

### 3.1 用例被删（可借，且必须先补）

对账脚本直接给 `killingTest` 名字，按名字回原套件取用例即可。本线补回其中**不依赖伪造状态**的部分：card 的反射探针 8 条（其中 6 条实际命中）、math 的编译前路径 1 条；剩下 4 条 card 缺口需要 mock 输入 record（见 3.2），按要求没有引入。

### 3.2 违规 mock 换来的击杀（不可借）

对四份套件逐个统计（注释里的字样要排除）：

| 套件 | `mockStatic` | `mockConstruction` | 连续 stub 伪状态 | 对结果的影响 |
| --- | --- | --- | --- | --- |
| GPT math | 10（`DynamicFunctionCompiler`） | 1（`ComplexExpression`） | 0 | `SingletonEquationSolving`/`ComplexCalculation` 等 7 条 NO_COVERAGE 变 KILLED |
| glm math | 0 | 0 | 2（`stubbedRecordReachesInvertedPromotionWindowGuard` / `...Coupon...`） | 促销/优惠券区间倒挂死区 8 条变 KILLED |
| deepseek math | 0 | 0 | 0 | — |
| 相互借鉴 card | 0 | 0 | 0（仅端口接口 mock） | — |

这两类击杀的共同点：**让程序进入了原实现不可达的路径**。glm 的手法是连续 `thenReturn`，让同一个 record 的访问器前后返回矛盾值（L236 读到过去日期、L246 读到未来日期），从而"通过"区间倒挂检查；GPT 的手法是替换静态编译器，让动态编译"成功"。

审计规则：**先用 grep 数 `mockStatic(`/`mockConstruction(`/`thenReturn` 连续链，再看它集中在哪些包**；命中区域对应的击杀一律不计入可借集合，除非能给出"没有替换被测实现"的具体论证。

## 4. 一个更高外部成绩的差距定位（师妹 2486/2528）

手上有别人更高的报告时，先做**逐包 delta**，不要从头猜：

| 包 | 本线 | 对方 | 差 |
| --- | --- | --- | --- |
| expression.engine | 1067/1121 | 1105/1121 | **+38** |
| pricing.service | 185/199 | 189/199 | +4 |
| progress.service | 196/198 | 198/198 | +2 |
| enrollment.service | 204/206 | 206/206 | +2 |
| scheduling.service | 230/231 | 231/231 | +1 |

差额 47 条里 38 条集中在引擎，于是只问一个问题：**引擎那批 NO_COVERAGE 在本题包里可达吗？**

证据链：
1. `DynamicFunctionCompiler` 生成源码硬编码 `import io.github.beardedManZhao.mathematicalExpression...`；
2. 扫描整个 `.m2` 仓库（全量 jar 的条目名），**不存在该包**；
3. 本地套件里已有一条用例断言"动态编译入口在本副本中恒定失败"；
4. 因此 `JvmCalculation.calculation/compile`、`SingletonEquationSolving.*`、`EquationSolver.autoDetectInterval`、`DynamicFunctionCompiler$3` 等 21 条 NO_COVERAGE 与一批 SURVIVED，**真实 API 永远走不到**；要拿这 38 条只能 `mockStatic(DynamicFunctionCompiler)`。

结论：不追。把差距写成"环境不可达 + 违规路径"，而不是"我们还能再提 38 条"。

## 5. 两次提交事故

| 现象 | 证据 | 原因 | 恢复 | 防复发 |
| --- | --- | --- | --- | --- |
| 上传后才发现脚本名 `card-approval-StudentTest.java`，类名却是 `StudentTest` | 页面回执文件名 | 打包时用了"项目-文件名"前缀 | 立即用 `StudentTest.java` 重交（以最后一次为准） | 冻结时就用 `类名.java`；上传前比对 `文件名 == public class 名` |
| 为赶截止 kill 了正在写报告的 PIT，ZIP 里是 2413 的残缺报告 | `mutations.xml` 状态和 2413 ≠ 2436，PIT 进程被强杀 | 把"释放文件锁"当成"结束构建" | 无法补跑（截止），只能说明 `.java` 计分、ZIP 仅辅助 | 顺序固定：冻结测试 → 跑完 PIT → 打包 → 提交；**任何情况下不 kill 正在写报告的 PIT 去打包** |

第二条的教训比数字更重要：**归档产物只能来自"跑完并严格解析"的报告**。中断跑的 2413 既不能当成绩，也不能当"退步"证据，只能当事故记录。

## 6. 生成式注释的代价

为提升可读性，对 262 + 595 个测试方法批量生成 `Given/When/Then` 注释，踩到四类问题：

1. **方向错**：类级描述取了类里第一个用例的 `@DisplayName`（应在 `class` 关键字之前找）；
2. **重复插入**：`@Nested` 处插了"测试组"，紧跟的 `class` 行又被插成"辅助类型"——需要"已写该类文档"的状态位；
3. **语义误标**：把 `map.put(...)`、`builder.add(...)` 当 `When`；纯断言型用例的 `When` 为空，必须给一致兜底文案（"输入在断言调用中直接给出"）；
4. **不幂等**：脚本重跑会在已有 Javadoc 前再插一份；必须识别"上一行是注释"。

另有一次纯工具事故：PowerShell 里 `param($Output)` 与内部 `$output` 同名（大小写不敏感），列表被字符串覆盖，输出文件被写成 1 行。**改写类脚本必须用样例先跑、再看输出行数与首尾。**

顺序约束：注释改动会移动行号，**必须在最终 PIT 之前完成**；已跑完再改注释，等于报告里的行号全部失效。

## 7. AI 自相矛盾点（本轮实际发生的五种）

1. **自述与身份集矛盾**：报告写"剩余 27 条均等价"，对账显示其中 11 条是别的套件已杀死的。根因是把"我没写"写成"不可杀"。避免：任何"等价"结论都附变异身份 + 已尝试输入；对账脚本兜底。
2. **标准不一致**：一边以"禁止 mock"为由拒绝 GPT 的引擎击杀，一边又保留了继承来的 1 处 `mock(Shift.class)`（真实构造器对 NIGHT 抛 NPE）。根因是继承来的代码没被同一标准审查。避免：合并后对整份文件重跑一次 mock 审计，不只看新增部分。
3. **目标与副作用矛盾**：为提升可读性批量加注释，却让已跑完的 PIT 行号引用失效；生成器还因方向错/重复插入产出错误文档。避免：注释先于最终 PIT；生成器幂等 + 样例自验。
4. **局部最优伤全局**：为了"按时提交"，强杀正在写报告的 PIT，把 2413 的残缺报告打进 ZIP。避免：打包顺序固定，宁可不重交也不交残缺证据。
5. **承诺与证据矛盾**：前期说"目标 100%"，证据却显示 16/101 条无人可杀。避免：先量化合规上限，再承诺；"没找到反例"只写"已攻击未证明"。

## 8. 交付数字（本线）

| 项目 | 用例 | 行数 | 分支 | 严格 KILLED | PIT 耗时 |
| --- | --- | --- | --- | --- | --- |
| card-approval | 262 | 11,255 | 2000/2013 = 99.35% | 1299/1320 = 98.41% | 4 分 27 秒 |
| math-assessment | 595 | 18,391 | 2616/2659 = 98.38% | 2436/2528 = 96.36%（+1 条未完成全量验证） | 6 分 57 秒 |

两题 `mvn test` 全绿；`src/main`/`pom.xml` 哈希与题包一致；提交时间 card 17:32、math 17:53。

## 9. 可迁移清单

1. **先判同源**（`src/main` + `pom.xml` 哈希），再比报告；不同源的报告不进同一张表。
2. **对账用身份集合，不用总数**：`scripts/mutation_union.py` 直接输出"全部杀 / 无人杀 / 基线漏杀 + killingTest"。
3. **合并版的"等价清单"是假设**：其中混着"用例被删"，用 killingTest 反查即可证伪。
4. **借高分前先审计 mock**：`mockStatic`/`mockConstruction`/连续 `thenReturn` 的命中区域，击杀不可借。
5. **外部更高成绩先做逐包 delta**，把差额收敛到一个包，再做可达性证明（依赖是否在 classpath、构造器是否允许、分支是否死路）。
6. **环境不可达要留证据**：包缺失/编译必失败这类结论，要给出"扫描范围 + 命中 0 + 套件内已有断言"三件套。
7. **提交前两查**：文件名 == 类名；ZIP 内测试文件与上传脚本字节一致。
8. **打包顺序固定**：冻结 → 完整 PIT → 严格解析 → 打包；不在 PIT 写入期间 kill 进程。
9. **生成式改写要幂等 + 样例自验 + 先于最终 PIT**。

## 10. 不可迁移

16/101 这类"无人可杀"的具体条目、11/8 这两个差额、4 分 27 秒/6 分 57 秒的耗时、本环境"动态编译必失败"的结论。换题包或换依赖清单后，必须重新建立基线、重新跑 PIT、重新对账。
