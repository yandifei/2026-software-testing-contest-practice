# Research：白盒补测实验（2026-10-31）

## 范围与结果

本轮按用户新授权进行本地白盒研究：只读取两个实例私有字段，受控设置 ID 生成器的静态计数器并在 finally 中恢复。生产源码、POM、默认变异范围、原始报告均未改动。这是本地研究材料，不据此认定赛事平台允许反射。

2026-10-31 22:58（Asia/Shanghai）完整验证成功：

| 指标 | 前一公开接口轮次 | 本轮白盒实验 |
| --- | ---: | ---: |
| JUnit 通过 | 84 | **87** |
| JaCoCo 分支 | 239/240 | **240/240（100%）** |
| JaCoCo 行 | 405/405 | **405/405（100%）** |
| PIT DEFAULTS KILLED | 279/305 | **284/305（93.11%）** |
| SURVIVED | 25 | **21** |
| NO_COVERAGE | 1 | **0** |

失败/错误/跳过均为 0；无 TIMED_OUT、RUN_ERROR 等其他变异状态。PIT 自身行覆盖是 399/401，不能与 JaCoCo 的 405/405 混报；两工具对私有空构造器等统计不同。

- 日志：`verification-whitebox-2026-10-31.log`。
- 本轮报告：`project/target/site/jacoco/jacoco.xml`、`project/target/pit-reports/mutations.xml`。
- 测试 SHA-256：`6d92ef1799fbe96a085904bda26937630db4921dbbea523c181c7afff94cdb07`。
- 运行前后 15 个生产源码/POM 的清单和 SHA-256 与官方原包基线一致。
- 命令仍为原 Maven JUnit/JaCoCo/PIT 插件，JaCoCo 0.8.11 使用相对 `target/jacoco.exec` 输出；仅请求 HTML/XML 报告，没有改变测试或生产目标过滤、变异算子和分母。

## 本轮新增观察点

1. **ID 静态状态可控性。** 用 COUNTER=0 和 Long.MIN_VALUE 两个 seed，覆盖正/负号归一化分支。断言计数恰好增加一次，结果为正；再从结果的两种可能原始符号反解时间位域，核对低 20 位与捕获的调用前后时间范围。该判定只比较内容，不通过性能、等待或超时杀变异，不循环扫描毫秒范围。它检出位移方向、遗漏取负和遗漏条件共 3 个原先未杀死项。finally 恢复原计数器，原 POM 使用顺序测试；未来启用并行测试时需要为所有访问该全局状态的测试设置一致的隔离策略。
2. **只读观察隐藏副作用。** 对 Researcher 的 assignedCount，确认成功分配依次为 1、2，容量不足和 null 请求不再增加；同时断言公开 capacity。检出计数加法改减法变异，不为了制造差异改写计数器。
3. **保留 IEEE-754 位级差异。** 对 Budget.reserveRatio 只读观察，确认 -0.0 被 `<0` 原条件保留。`Double.doubleToRawLongBits` 区分 ±0，检出下端点边界变异；普通储备金 getter 会被最低 1000 遮蔽这一区别。

前轮新增的有限成本求和溢出测试仍保留：它通过公开 NaN/1000 差异检出负比例裁剪被移除。加入白盒观察后 PIT 可能选择另一个更短测试作为 killingTest，这不抹除前轮已经实测的公开输入反例。

## 相比前一公开接口轮次新增的 5 个 KILLED

下表身份来自本轮原始 mutations.xml；index 是 PIT 指令索引，不是报告行号。算子统一前缀为 `org.pitest.mutationtest.engine.gregor.mutators.`，测试类均为 `net.mooctest.BudgetTest`。

| 类/方法/源码行 | 算子及 index | 改变 | killingTest 方法 |
| --- | --- | --- | --- |
| Researcher.assignTask:71 | MathMutator，16 | 整数加改减 | mutationBoundary_successfulAssignmentsIncrementInternalCountAndRejectedOnesDoNot |
| Budget.setReserveRatio:66 | ConditionalsBoundaryMutator，6 | 下边界改变 | mutationBoundary_reserveRatioStoresClampedValueEvenWhenMinimumReserveMasksIt |
| IdGenerator.nextId:14 | InvertNegsMutator，25 | 去掉取负 | idGenerator_controlledCounterCoversBothSignsAndPreservesTimestampBits |
| IdGenerator.nextId:13 | MathMutator，14 | 左移改右移 | idGenerator_controlledCounterCoversBothSignsAndPreservesTimestampBits |
| IdGenerator.nextId:14 | RemoveConditionalMutator_ORDER_ELSE，23 | 删除负值判断 | idGenerator_controlledCounterCoversBothSignsAndPreservesTimestampBits |

ID 的 XOR 改 AND（MathMutator，index 16）本轮也由新 ID 测试杀死，但前轮已为 KILLED，未计入新增 5 个。不能只统计 killingTest 名字改变来计算新增贡献。

## 21 个剩余变异及证明边界

| 位置 | 数量 | 原始与变异关系 |
| --- | ---: | --- |
| Researcher.addSkill:30/31 边界 | 2 | int 恰为 0/10 时赋回同一个值，内部 Map 也相同。 |
| Researcher.addSkill:30/31 移除条件 | 2 | 后续 Math.max/Math.min 对全部 int 重复执行相同裁剪。 |
| Researcher.releaseHours:54 | 1 | capacity==40 时是否赋回 40 不改变任何内部状态。 |
| Researcher.updateRating:58 | 1 | 只在 -0.0 输入上可能改变中间符号；构造器 rating 为 +0，可达更新均为非负或 NaN，加法消除 -0 的影响。若反射强行写入不可由构造器/公开更新到达的 rating=-0 会扩大状态域，本轮未采用此方式。 |
| Researcher.updateRating:59 | 1 | 100 等值赋回，没有内部位级差异。 |
| Task.dependsOn:66 | 1 | 正常 API 禁止 null 依赖、getter 返回副本，内部集合不含 null；删除提前 false 与 contains(null) 相同。没有往私有集合注入非法 null。 |
| Task.requireSkill:53/54 | 2 | 0/10 等值赋回，内部状态也相同。 |
| Task.updateProgress:85 | 1 | 1.0 等值赋回，内部位模式相同。 |
| BudgetOptimizer.optimize:24 | 1 | ±0 最终转为相同整数容量，局部变量不能通过正常对象观察区分。 |
| RiskAnalyzer.simulate:46 | 1 | 相等时赋回同值；scenario 从 +0 开始相加，不会从正常 Risk 得到 -0。NaN 对原比较和变异比较均为 false。 |
| RiskAnalyzer.simulate:50 | 1 | 普通可分配数组大小 n 下 floor(0.9*n)<=n-1，min 一直取分位数索引；n+1 的 int 溢出边界需实际创建 Integer.MAX_VALUE 个元素的本地 ArrayList，当前 JVM 无可执行输入。保留资源边界限定，不声称无条件数学等价。 |
| Risk.clamp:18 | 1 | 1.0 等值赋回。 |
| Budget.forecastCost:53/54 | 2 | -0.5/1.0 等值赋回，后续乘法输入完全相同。 |
| Budget.requiredReserve:61 | 1 | 1000 等值赋回。 |
| Budget.setReserveRatio:67 | 1 | 0.5 等值赋回，反射读取也不会产生差异。 |
| GraphUtils.longestPathDuration:46 | 1 | int best 等于 v 时是否赋回相同值没有区别。 |
| IdGenerator.nextId:14 | 1 | long 零取负仍是零，即使控制 COUNTER 使局部结果为零也无法区分端点变化。 |

因此，**分支已实际达到 100%，原始变异杀死率没有达到 100%**。白盒观察扩大了可观察范围，但不能令“赋回完全相同值”的变异产生行为差异。继续增加相同端点输入、反复重跑或缩小分母都不会真实消除这些项。

## 可复用经验

- 区分输入可控性和结果可观察性：全局计数器阻碍到达，隐藏字段阻碍观察；应针对具体阶段选择手段。
- 白盒观察先读后写：能只读证明副作用时不要设置字段；确需受控状态时写明状态域、共享性及恢复策略。
- 原始指标和实验权限必须分开记录：本地用户授权不等于平台赛事许可，平台提交时重新核对规则。
- 结果独立于执行耗时：用内容、状态和位模式断言；时钟相关结果可捕获调用区间，不将快慢或变异 JVM 特征作为判定。
- 研究课题：约束状态域下的观测等价、依赖注入缺失时的可控性、浮点符号传播、不可变分母下的真实可达上限，以及可审计的等价证明。
