# 本轮存活变异语义分析

当前单文件测试重新测得 284/305，21 个存活项；以下为原源码对应项的语义依据，保留原始统计分母。

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

