# 单元测试交付与本轮结果

## 最终覆盖补齐（21:11 更新）

再次读取 PIT HTML 后，定位其漏测的两行为 `GraphUtils`、`IdGenerator` 的私有空构造函数。新增 `UtilityConstructionTest` 两个测试，通过反射执行构造函数；这两项主要补齐工具统计，不增加业务行为校验。

- JUnit：**86 / 86 通过**，失败/错误/跳过均为 0。
- PIT 行覆盖率：**401 / 401，100%**。
- JaCoCo 分支覆盖率：**240 / 240，100%**。
- PIT 变异杀死率：**284 / 305，93.11%**，剩余 21 个的分析见 `MUTATION_ANALYSIS.md`。
- 验证耗时：8.297 秒；日志：`verification-final.log`。
- 本轮通过命令行指定 agent 的相对输出路径 `target/jacoco-final.exec`，消除了此前中文路径导致的文件错位；未修改 pom.xml。JaCoCo 报告已由本轮测试直接生成。

以下为前两轮历史记录。当前 HTML 报告以以上最终结果为准。变异率未达到 100%，没有修改报告数字或缩减原始变异统计范围。

## 最新补强结果

用户追加要求读取 PIT 报告并继续提升后，新增 `MutationBoundaryTest` 的 8 个测试并完成一轮验证。

| 指标 | 最新结果 |
| --- | --- |
| JUnit | 84 / 84 通过，失败/错误/跳过均为 0 |
| PIT DEFAULTS | 284 / 305，93.11%，比首次多杀死 10 个变异 |
| 剩余变异 | 21，逐项分析见 `MUTATION_ANALYSIS.md` |
| JaCoCo 分支 / 行覆盖 | 240 / 240、405 / 405，均为 100% |
| Maven 验证耗时 | 8.807 秒 |

新增检查覆盖 P90 与异常最大值的区别、超额容量释放零小时、内部任务计数、空匹配结果可修改性、浮点负零，以及被最低储备金掩盖的内部比率状态。内部字段通过反射只读观察；没有为杀死变异而注入非法状态。

生产代码和 pom.xml 的 SHA-256 仍与首次执行前一致。没有删减变异算子、排除类或修改生成的 PIT 报告。完整 305 个变异的原始杀死率为 93.11%，**并未达到 100%**。剩余变异的行为等价分析不是修改官方得分的依据。

最新日志为 `verification-refined.log`。本轮覆盖文件同样因中文路径问题写入乱码目录的 `jacoco-refined.ex`；已复制为 `target/jacoco-refined.exec` 并仅生成报告，日志为 `coverage-refined.log`。当前 `target/pit-reports`、`target/site/jacoco` 和 `mutation-survivors.csv` 均已更新为补强后的结果。

## 首次实测结果（历史记录）

验证日期：2026-10-21。Java 21、JUnit 5.10.2、Maven 3.9.11；沿用原 pom.xml。

| 指标 | 结果 |
| --- | --- |
| JUnit 用例（含参数化展开） | 76，通过 76，失败/错误/跳过均为 0 |
| 新增测试类 | 9；保留原 BudgetTest |
| JaCoCo 分支覆盖 | 240 / 240，100% |
| JaCoCo 行覆盖 | 405 / 405，100% |
| JaCoCo 指令覆盖 | 2160 / 2160，100% |
| PIT DEFAULTS 变异杀死率 | 274 / 305，89.84% |
| 存活变异 | 31；无未覆盖、超时或运行错误变异 |
| Maven 本轮耗时 | 13.488 秒，包含构建和变异分析 |
| PIT 分析耗时 | 约 4 秒 |
| Surefire 各测试类报告耗时之和 | 约 0.242 秒，不含 JVM 启动等开销 |

实际执行了一轮 JUnit 和一轮 PIT。测试完成后未修改测试，也没有重跑测试。PIT 内部为评估变异体运行了 675 次测试调用，这属于变异分析机制。

首次 Maven 启动在执行任何测试前，因沙箱无法写入 `.m2` 缓存失败；取得环境权限后才完成上述验证。生产源文件和 pom.xml 的执行前后 SHA-256 一致。

## 测试范围与设计

- BudgetBoundaryTest：空值、上下限、通胀、储备金、零成本、并列最优、取整，以及 21 个容量下对所有子集穷举的独立背包最优值校验。
- TaskBehaviorTest：初始状态、属性归一化、技能合并、依赖去重与防御性复制、时间约束、状态变迁、进度边界。
- ResearcherBehaviorTest：技能等级、精确容量分配与释放、评分加权、分配与完成、零时长与技能校验缺陷刻画。
- ResourceBehaviorTest：非法区间、预约重叠与端点、取消、集合复制、后继预约遗漏与冲突占位实现。
- GraphSchedulerTest：空图、独立任务、依赖链、共享依赖、环、外部依赖与精确排期。
- RiskBehaviorTest：风险阈值、三级排序、确定/不可能事件、可控随机输入、分位数、种子黄金序列。
- MatchingEngineTest：空输入、评分、最佳候选与同分稳定性、容量不足、单次唯一分配、优先级排序。
- ProjectReportTest：项目成员、状态统计、预算替换、委托分析与完整文本报告。
- IdGeneratorTest：无符号范围、解析异常、计数器与符号分支、异常链。反射仅用于控制难以自然触发的内部状态；共享计数器在 finally 中恢复。

不启动外部服务、不依赖网络、不使用睡眠。风险模拟通过固定种子及仅替代随机输入的测试替身保持确定性。保留默认串行 JUnit 执行；ID 测试会暂时操作共享计数器，不应直接开启方法级并行执行。

## 已复现的业务风险

以下用例是**当前行为刻画**，其通过不代表业务规则正确。生产实现未修复。

| 位置 | 最小场景与观察结果 | 业务影响 |
| --- | --- | --- |
| GraphUtils / Scheduler | 时长 3 → 5 → 2 的依赖链返回逆序，关键路径为 5 而非 10；各任务最早开始时间为 0 | 工期被低估、任务可能在前置任务完成前开始 |
| Resource.isAvailable | 已预约 11:00–12:00，再查询 10:00–13:00 返回可用 | 仅检查前驱预约，遗漏后继预约重叠 |
| Resource.conflicts | 对已预约区间查询冲突仍返回 false | 冲突检查尚未实现 |
| Resource.isAvailable | 前一预约结束时刻恰等于后一开始时刻时拒绝 | 若业务采用半开区间，会错误拒绝相邻预约；端点规则需确认 |
| MatchingEngine.match | CRITICAL 和 LOW 竞争同一研究人员，LOW 先分配 | 双重 reversed 导致优先级顺序反向 |
| Researcher.canAssign | 无 physics 技能的人员仍可接要求 physics 10 的任务 | 必需技能没有被校验 |
| Researcher.assignTask | 零时长任务 canAssign 为 true，但 assignTask 为 false | 可分配判定与实际分配不一致 |
| BudgetOptimizer.optimize | 成本 1.49、限额 1.0，仍选择该条目 | 整数四舍五入可能使真实成本超预算 |
| Project.planAssignments | DONE 任务仍会被选中，消耗研究人员容量 | 若仅允许分配待处理任务，则缺少状态过滤 |

## 存活变异与结果边界

完整清单见 `mutation-survivors.csv` 与 PIT HTML。没有通过排除生产类、修改变异算子或过滤存活结果来提高指标。

存活变异中有多处在公开行为上等价或冗余的条件，例如裁剪函数将 `<` 改为 `<=` 时在边界仍赋回同一个值；`Researcher.addSkill` 的前置裁剪被后续 Math.min/Math.max 重复保障；最大值更新在相等时赋回相同值。`assignedCount` 的加减变异目前也没有公开查询行为可观察。

但不能把所有存活变异都认定为等价。至少存在以下测试不足：

- 风险分位数测试以 1、10 次采样为主，没有有效区分部分“90% 分位数退化为最大值”的变异，需要更大的、多层次样本才可区分。
- `releaseHours(0)` 尚未结合构造容量大于 40 的状态验证；该组合可能区分其零边界变异。

受本任务“一次写完、仅测试一次”约束，本轮不再调整用例。覆盖率达到 100% 并不意味着业务无缺陷，也不意味着变异杀死率达到 100%。本地 PIT DEFAULTS 结果不等同于比赛平台隐藏变异体得分。

## 运行与报告

本轮执行命令（已执行，不需要为查看结果再次运行）：

```powershell
mvn clean test org.pitest:pitest-maven:1.15.3:mutationCoverage '-DoutputFormats=XML,HTML' '-Dthreads=4' '-DtimestampedReports=false'
```

本轮 JaCoCo agent 因 Windows 中文路径编码问题，把执行文件写入桌面的乱码同名目录，且文件名变为 `jacoco.ex`。已读取该次运行的数据并复制到 `target/jacoco.exec`，再仅运行离线 `jacoco:report` 生成报告。此步骤不执行测试；原异常路径下的文件未删除。覆盖数据 session 时间与本轮 Surefire 执行一致。

- JUnit XML：`target/surefire-reports/`
- JaCoCo HTML：`target/site/jacoco/index.html`
- JaCoCo XML：`target/site/jacoco/jacoco.xml`
- PIT HTML：`target/pit-reports/index.html`
- PIT XML：`target/pit-reports/mutations.xml`
- 构建日志：`verification.log`
- 补充生成覆盖报告日志：`coverage-report.log`

PIT 报告行覆盖 399/401，JaCoCo 报告 405/405；两个工具对可执行行、过滤项与覆盖采集的口径不同，应分别引用，不能混为同一指标。
