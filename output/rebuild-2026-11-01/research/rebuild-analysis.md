# Research 全新测试重建记录（2026-11-01）

## 独立重建范围

本目录从原题生产源码及 POM 开始，测试目录原为空。逐个阅读 14 个生产源码文件后新写一个顶层 `ResearchRebuiltTest.java`，没有复制或拼接旧测试代码。沿用已经确认的语义知识，例如等价端点与可恢复白盒状态，不把“重写代码”误作必须重复踩坑。

所有生产源码/POM 执行后与本目录 `source-baseline.json` 的 15 个文件及 SHA-256 完全一致。测试采用 UTF-8，无 BOM；仅一个测试文件。

## 两轮实际验证

| 轮次 | JUnit | JaCoCo 分支 | JaCoCo 行 | PIT KILLED | 诊断 |
| --- | ---: | ---: | ---: | ---: | --- |
| 首次完整重建 | 21/21 | 240/240 | 405/405 | 282/305 | 两项可区分断言缺口 |
| 按报告补测 | **24/24** | **240/240（100%）** | **405/405（100%）** | **284/305（93.11%）** | 21 SURVIVED，无其他状态 |

最终 0 失败、0 错误、0 跳过；Surefire 测试用时 0.070 秒，完整 Maven 构建 16.038 秒。本轮 PIT 行覆盖 401/401，与 JaCoCo 分母不同。

两轮均执行题包原插件：`mvn clean test org.pitest:pitest-maven:mutationCoverage`。Java 21、Maven 3.9.11；使用题包同版本 JaCoCo 0.8.11 agent 和相对输出 `target/jacoco.exec` 解决中文路径问题；报告格式为 HTML,XML，未改默认变异算子和范围。没有失败后跳过测试，也没有重复执行无新假设的轮次。

最终测试 30,103 字节，SHA-256：`e478781ff57841186968da1bdd1ee376b631d1cde3fa1d054fe45ab0b65f9dbb`。

日志：`verification-rebuild-round1.log`、`verification-rebuild.log`。证据：`project/target/surefire-reports/TEST-net.mooctest.ResearchRebuiltTest.xml`、`project/target/site/jacoco/jacoco.xml`、`project/target/pit-reports/mutations.xml`。

## 测试判定与新发现

- 预算优化使用 6 个条目的 64 个子集穷举参照，遍历 0–24 的容量。独立计算最优值，再检查实际成本、价值、重复项和输入顺序保持，避免复制生产动态规划。
- 预算、任务进度、风险保留负零位模式测试；有限大成本相加溢出，用 NaN 与最低储备金的不同公开结果检查裁剪传播。
- 风险模拟使用可控抽样，覆盖恰等于概率、10/20 场景仅一次事件的分位数；随机生成器以 seed=1 的首轮手工位运算结果 1082269761 验证，兼顾复现性和范围。
- 图和调度测试独立节点、共享依赖、依赖链、环与空图；实际反向依赖顺序、关键路径截断作为实现刻画。
- 新的资源观察：listBookings 创建新 List，但内部是 TreeMap 的活 Map.Entry。合法地调用 snapshot.get(0).setValue(更晚的结束时间) 会改变实际预约冲突结果；清空快照列表却不影响预约。说明“容器复制”不等于“元素隔离”。用例未写入无效时间区间或私有状态，这是一条真实公开接口别名复现。
- Project 的任务/人员视图是可变的 live values，而风险列表是副本，分别断言实际语义。报告采用完整固定字符串验证状态计数、成本、路径和风险统计，避免只测存在片段。
- ID 静态计数器设置 0/Long.MIN_VALUE 覆盖两种符号，通过结果反解时间位并核对调用区间；始终 finally 恢复。assignedCount/reserveRatio 只读观察。此处是本地白盒研究，平台反射许可仍须独立核对。

## 首轮存活项带来的两次定向修正

1. **Researcher.canAssign:66 的 >=→>。** 首轮有“足够”和“不足”，缺少正工时恰好用尽容量。新增 `exactAvailableCapacityStillAcceptsTheTask`，断言先可分配、分配后容量为零、再次不可分配。PIT 明确报告该方法杀死条件边界变异。
2. **Risk.compareTo:41 的删除优先级返回。** 首轮高优先级恰好也具有高 impact，删除优先级判断后第二排序键仍掩盖错误。新增 `riskPriorityOutranksContradictoryImpactOrder`，让高优先级项的 impact 更小，检查双向比较。PIT 明确报告该方法杀死删除条件变异。

这两项说明：分支已到达且覆盖100%，仍可能缺少能区分错误的输入。边界矩阵必须覆盖精确等值；多字段优先级应故意设置后续字段的排序方向与首要字段相反。

## 为何重写后仍不是原始变异100%

剩余21项与现有实现的语义限制一致；没有将它们从305分母中剔除：

| 分组 | 数量 | 依据 |
| --- | ---: | --- |
| Researcher.addSkill 的 0/10 端点与冗余裁剪 | 4 | 相同整数赋回，或 Math.max/min 再次执行同一裁剪 |
| Researcher.releaseHours 的 40 端点 | 1 | 等值赋回 |
| Researcher.updateRating 的 0/100 端点 | 2 | 非零端点相同；正常可达 rating 从 +0 开始，后续相加消除负零差别 |
| Task.dependsOn 的 null 提前返回 | 1 | 合法依赖集合不存在 null，contains(null) 也是 false |
| Task.requireSkill 的 0/10、updateProgress 的1端点 | 3 | 内部状态与位模式相同 |
| BudgetOptimizer 的 ±0 容量 | 1 | 转为同一整数容量 |
| RiskAnalyzer 最差值相等赋回与分位数上界 | 2 | 等值结果一致；普通可分配 n 下分位索引始终小于两个上界，极限 int 溢出属于资源不可实现边界，不能宣称所有数学域等价 |
| Risk.clamp 的1端点 | 1 | 相同 double 赋回 |
| Budget 的通胀两端点、最低储备金1000、比例上限0.5 | 4 | 等值赋回，白盒读取也没有差别 |
| GraphUtils 最长值相等赋回 | 1 | 同一 int 值 |
| IdGenerator 的零取负边界 | 1 | long -0 与0相同 |

未向任务依赖集合注入 null，也未写入原程序不可达的 rating=-0 来制造杀死率；没有检测 PIT 环境、检查变异字节码、修改报告或缩小分母。全新重建证明测试组织方式可改变，但相同行为的变异不会因此产生可观察差异。

## 可复用研究点

1. 将测试输入从单字段边界扩展为多字段“相反趋势”组合，防止后续规则掩盖前置规则的失效。
2. 对返回集合检查容器与元素两个层次，分别验证 clear/add 和 Entry.setValue 等能力；用实际后续行为验证引用逃逸。
3. 从单文件中组织模块测试和独立 oracle，测试方法数与断言场景数分开汇报：本轮24个测试方法包含容量穷举和多端点矩阵。
4. 报告先比较新增 killed 身份，再分析剩余项；大规模重写只在改进测试结构或独立复核时有价值，不是消除等价变异的手段。
