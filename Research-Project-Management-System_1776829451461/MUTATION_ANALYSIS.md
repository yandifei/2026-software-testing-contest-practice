# PIT 存活变异分析

2026-10-21 最新报告的原始 DEFAULTS 指标为 **284/305 = 93.11%**。86 个测试通过，PIT 行覆盖率为 401/401，JaCoCo 分支覆盖率为 240/240；剩余 21 个变异均有覆盖。以下是源码语义分析，并非对比赛平台的等价变异认定或评分承诺。

## 再次复核：为什么持续重跑不能保证 100%

重新读取 `target/pit-reports/mutations.xml`，剩余项全部为 `SURVIVED`，而不是 `NO_COVERAGE`。各项都实际执行过覆盖其位置的测试。本次没有修改测试或重复运行相同测试，因为复核未定位新的可区分输入。

只需证明其中一个变异完全等价，就足以证明在“不修改生产代码、不排除变异、不改变统计口径”的约束下，305/305 无法达成。以 `Researcher.addSkill` 第 31 行为例：

```java
// 原实现
if (level > 10) level = 10;
// PIT 的条件边界变异
if (level >= 10) level = 10;
```

`level` 是 int，所有输入只有三种情况：

| 输入 | 原实现分支执行后 | 变异实现分支执行后 |
| --- | --- | --- |
| level < 10 | level 不变 | level 不变 |
| level == 10 | 保留 10 | 赋值为 10 |
| level > 10 | 赋值为 10 | 赋值为 10 |

两段代码在所有 int 输入下产生完全相同的局部变量和对象状态，后续语句也相同。增加边界用例、随机用例、重复执行次数，甚至观察最终私有字段，都不能区分这个变异。这里的原因不依赖“可能存在等价变异”的猜测，而是对所有输入的完整分类。

因此，原始 100% 目标在既定约束下不可实现。其他剩余变异的适用范围与原因在下表逐项说明。报告保留真实的 284/305，没有将人工等价分析换算成官方的 100% 指标。

## 本轮新增杀死的 10 个变异

| 方法 | 数量 | 区分方式 |
| --- | --- | --- |
| RiskAnalyzer.simulate | 1 | 20 个场景中仅 1 个异常值，P90=0、最大值=1，区分乘法变除法 |
| Researcher.releaseHours | 1 | 初始容量 41，释放 0 小时必须不改变容量 |
| Researcher.assignTask | 1 | 只读反射确认分配计数递增、失败时不递增 |
| MatchingEngine.match | 1 | 确认空结果仍为各次调用独立的可修改列表 |
| Budget.Item 构造函数 | 2 | 检查 cost/value 的 IEEE-754 负零位模式 |
| Risk.clamp | 1 | 检查负零被原样保留 |
| Task.updateProgress | 1 | 检查负零被原样保留 |
| Budget.setReserveRatio | 2 | 只读反射检查负数归零和负零保留，避免最低储备金掩盖状态差异 |

## 剩余 21 个变异

下表按源代码方法及行号标识；同一行可能有不同变异算子。可用 `mutation-survivors.csv` 查看完整算子名称。

| 方法 / 行 | 数量 | 存活原因与语义分析 |
| --- | --- | --- |
| Researcher.addSkill / 30、31，边界变异 | 2 | int 等于 0 或 10 时，进入分支赋回相同整数，后续状态一致 |
| Researcher.addSkill / 30、31，移除条件 | 2 | 后续 `Math.max(0, Math.min(10, level))` 已对全部 int 输入执行相同裁剪 |
| Researcher.releaseHours / 54 | 1 | capacity 等于 40 时，是否赋回 40 没有状态差异 |
| Researcher.updateRating / 58 | 1 | 非零输入行为相同；负零虽然可能被改为正零，但可达 rating 初始为正零、更新为非负值，后续加法消除了该符号差异；NaN 输入的比较分支也一致 |
| Researcher.updateRating / 59 | 1 | 等于 100 时，赋回相同 double 值 |
| Task.dependsOn / 66 | 1 | `addDependency` 禁止 null，且 getter 返回副本；公开 API 可达集合不含 null，所以省去 null 检查后 `contains(null)` 仍为 false |
| Task.requireSkill / 53、54 | 2 | int 在 0、10 处赋回自身，没有后续差异 |
| Task.updateProgress / 85 | 1 | 等于 1 时赋回自身 |
| BudgetOptimizer.optimize / 24 | 1 | 0 与负零均转成整数容量 0；是否执行 `limit = 0` 不影响结果 |
| RiskAnalyzer.simulate / 46 | 1 | scenario 等于 worst 时赋回相同值；正常 Risk 输入的场景累计为非负，零从正零开始累加 |
| RiskAnalyzer.simulate / 50，减法变加法 | 1 | 正常可分配 ArrayList 大小 n 下，`floor(0.9*n) <= n-1`，min 总选择分位数索引；n+1 仍大于它。int 极限溢出涉及不可实际分配的 ArrayList 大小，不是可执行的普通单元测试输入 |
| Risk.clamp / 18 | 1 | 等于 1 时赋回自身 |
| Budget.forecastCost / 53、54 | 2 | 等于 -0.5 或 1 时赋回自身 |
| Budget.requiredReserve / 61 | 1 | 等于 1000 时赋回自身 |
| Budget.setReserveRatio / 67 | 1 | 等于 0.5 时赋回自身 |
| GraphUtils.longestPathDuration / 46 | 1 | v 等于 best 时赋回相同 int |
| IdGenerator.nextId / 14 | 1 | long 零取负仍为零；其他输入的判断相同 |

没有使用读取变异字节码、探测 PIT 运行状态、依赖耗时差异或反射破坏对象不变量的方法制造“杀死”。尤其没有向 Task 的私有依赖集合塞入 null 来人为区分不可达状态。

## 为什么不把 HTML 改成 100%

在保持生产代码和全部 305 个变异统计口径的前提下，上述行为等价变异不能靠普通断言全部杀死。去掉条件边界算子、排除相关方法或直接修改报告能改变显示值，但不代表测试能力提升，因此没有执行。

当前可交付的事实是：原始杀死率从 **89.84% 提升至 93.11%**，本轮修补了已定位的 10 个可区分变异，剩余问题有逐项语义说明；不能将这一分析称为原始变异杀死率 100%。
