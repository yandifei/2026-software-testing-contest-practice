# Research：2026-10-31 缺口复核与真实改进

## 本轮结果

- 完整命令执行一次：`mvn clean test org.pitest:pitest-maven:mutationCoverage`，使用题包 JaCoCo 0.8.11 agent 的相对输出 `target/jacoco.exec`，仅增加 `HTML,XML` 报告格式；未修改 POM、目标或算子。
- 2026-10-31 14:35（Asia/Shanghai）构建成功，84 个用例全部通过，0 失败/错误/跳过。
- JaCoCo 分支 **239/240（99.58%）**；行 **405/405（100%）**。
- PIT DEFAULTS：**KILLED 279/305（91.48%）**，SURVIVED 25，NO_COVERAGE 1。比 2026-10-29 的 278/305 多杀死 1 个，分母不变。
- 日志：`verification-2026-10-31.log`；本轮报告：`project/target/site/jacoco/jacoco.xml`、`project/target/pit-reports/mutations.xml`。
- 测试 SHA-256：`90b88e45768d3b68cd852b715218d5f447705345ec541b107f01631bb83d6dd5`。
- 执行前后 15 个生产源码/POM 文件的完整清单和 SHA-256 均与官方原包基线相同。

## 新增的有效区分输入

`budgetReserve_characterizesFiniteCostOverflowAndNegativeRatioClamping` 使用公开 API 创建两个成本均为 `Double.MAX_VALUE` 的预算项。输入本身都是有限正数，但求和溢出成正无穷。

当储备比设为 -0.25：原实现先裁剪成 +0，`+Infinity * +0` 得到 NaN，`NaN < 1000` 为 false，所以返回 NaN；移除负值裁剪的变异则产生负无穷，随后被最低储备金裁剪成 1000。断言 `Double.isNaN` 明确区分二者。再将比例改成 0.25 和 0，分别断言正无穷和 NaN，检查状态重设与传播。

PIT 报告实际确认 `Budget.java:66` 的 `RemoveConditionalMutator_ORDER_ELSE` 被这个新增测试杀死。没有通过反射读写私有字段，也没有直接构造无穷成本、改变源码或检测 PIT 环境。

这是实现刻画：它暴露有限输入总和溢出后出现非有限储备金的业务风险，不代表 NaN 是正确业务需求。历史判断“最低 1000 总会掩盖负比例”只对未溢出的普通非负预算成立，不能推及全部 Java double 输入及运算结果。

## 剩余 26 项逐组分析

| 源码/方法/行号 | 项数 | 分类与依据 |
| --- | ---: | --- |
| Researcher.addSkill:30/31 | 4 | 两个端点变异仅在 0/10 赋回相同 int；两个删除条件变异仍被紧随的 Math.max/Math.min 完整裁剪，对全部 int 输入状态相同。 |
| Researcher.releaseHours:54 | 1 | 仅 capacity==40 时改变是否再次写入 40，无返回值或状态差异。 |
| Researcher.updateRating:58/59 | 2 | 上端点仅赋回相同 100。下端点只可能改变 -0/+0，公开可达 rating 从 +0 开始，非负加权更新消除该符号差异；NaN 两种比较均为 false。 |
| Researcher.assignTask:71 | 1 | assignedCount 加/减不同，但该私有字段从不被任何公开行为读取；公开接口下不可观察，不能冒称整个内部状态等价。 |
| Task.dependsOn:66 | 1 | 私有依赖集合唯一加入入口拒绝 null，getter 返回副本；公开可达集合不含 null，contains(null) 与提前 false 相同。 |
| Task.requireSkill:53/54 | 2 | 0/10 等值赋回；内部 Map 状态相同。 |
| Task.updateProgress:85 | 1 | p==1 时是否赋回相同 double，无区别。 |
| BudgetOptimizer.optimize:24 | 1 | -0 与 +0 都转为容量 0，后续选择行为相同；其余输入不受此端点变异影响。 |
| RiskAnalyzer.simulate:46 | 1 | 相等时赋回相同最差值；普通 Risk 构造器限制影响为非负，scenario 从 +0 累加，不能用 -0 制造不同状态。NaN 不满足任一比较。 |
| RiskAnalyzer.simulate:50 | 1 | 正常可分配 n 个场景时 floor(0.9*n) <= n-1，原 min 和变异 min 都取分位索引；n+1 的 int 溢出只发生在 n=Integer.MAX_VALUE，该实现需实际创建同规模 ArrayList，当前环境不可实现。归为资源边界限制，不将其称为全数学域等价。 |
| Risk.clamp:18 | 1 | v==1 时赋回同一 double；NaN 两边条件都为 false。 |
| Budget.forecastCost:53/54 | 2 | -0.5、1 等值赋回；后续运算相同，包括已经溢出的预算。 |
| Budget.requiredReserve:61 | 1 | r==1000 时赋回同一值，其余输入的分支行为相同。 |
| Budget.setReserveRatio:66 | 1 | 边界变异仅改变 -0/+0 私有状态；对正常有限非负成本，总被最低 1000 遮蔽；正无穷乘两者都是 NaN。本轮未找到公开结果的可移植区分方式，不以私有状态不同冒充公开杀死。 |
| Budget.setReserveRatio:67 | 1 | r==0.5 时赋回相同值。 |
| GraphUtils.longestPathDuration:46 | 1 | v==best 时赋回相同 int。 |
| IdGenerator.nextId:14 边界 | 1 | long 零的相反数仍是零，其他值条件不变，属于等价变异。 |
| IdGenerator.nextId:13 移位 | 1 | 左移/右移本身不等价；现有正值、唯一性、往返解析断言不能区分。需要对未知内部计数器与时钟建立独立公开可观察契约，本轮未获得可靠无时序假设的判定。保留为断言/可观测性缺口。 |
| IdGenerator.nextId:14 去负号/删条件 | 2 | 负值路径本轮未发生，其中去负号为 NO_COVERAGE；初始化计数器来自 System.nanoTime，与墙上时钟共同决定符号。公开 API 不提供可控时钟或计数器；属于控制性缺口，不是不可达证明，也不是等价。 |

## 分支与 100% 的边界

本轮唯一未覆盖分支为 `IdGenerator.java:14` 的负值归一化分支。其是否发生依赖运行环境初始计数器和时钟，本轮不能保证通过公开输入稳定触发。没有改写私有计数器、系统时间或增加特殊生产实现。

即便以后获准可控状态注入并覆盖该分支，多个整数端点“判断改变但最终状态相同”的等价变异仍在原始 305 分母中，因而不能据此承诺原始杀死率 100%。本轮不把等价项剔除后的数字当原始结果。

## 可复用方法与研究方向

1. 跟踪“到达 → 状态差异 → 传播 → 公开观察”四个步骤。覆盖只证明到达；必须证明差异能传播到断言。
2. 遇到兜底值掩盖差异，检查输入组合和中间运算溢出。有限输入不保证有限中间值；先建立独立预期，再用 PIT 指认新增杀死项。
3. 浮点边界应区分 ±0、最小正/负子正规数、最大有限值、运算溢出、NaN 与无穷；按 API 契约和合法公开路径选择，不机械塞入所有特殊值。
4. 等价证明写明输入域、公开可达状态、观察接口及资源限制；“没找到输入”不是等价证明。每个证明应能被新的反例修正。
5. 可研究：路径条件自动提取、依赖状态的可控性量化、变异差异传播追踪、IEEE-754 反例合成、资源限制下的等价判定。研究结论需与实际交付指标分开。
