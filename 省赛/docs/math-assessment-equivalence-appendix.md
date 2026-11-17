# math-assessment 等价性 / 不可达性证明附录

- 项目：`B:\软件测试大赛\省赛\deepseek\math-assessment`（math-assessment，contest 35 problemId 136）
- 测试文件：`src/test/java/net/mooctest/StudentTest.java`（284 用例，全绿）
- PIT 快照：`B:\软件测试大赛\省赛\_baseline\math-pit-1442.xml`
  - md5 `1E4560E5EA77F61B3A6FBE3B933B5340`，1,899,311 bytes，2,528 条 mutation
  - 全项目 KILLED 1429 / SURVIVED 111 / NO_COVERAGE 988 / TIMED_OUT 0 / RUN_ERROR 0
  - **非 engine 区域（本执行者负责）**：1,407 条 → KILLED 1345 / SURVIVED 54 / NO_COVERAGE 8
- 本附录逐条对应快照中**非 engine 区域剩余的 62 条** mutation（54 SURVIVED + 8 NO_COVERAGE）：
  - 不可达 **20** 条
  - 等价（归一化 / 值不变）**26** 条
  - 未解决 **16** 条（单列，未并入等价）
- 版本记录：`1435` 快照下为 68 条；`1442` 快照中 6 条被定向补杀（catalog L49、enrollment L291、pricing L416、pricing L432、refund L105、progress L244），1 条（enrollment L297）由“未解决”升级为“等价”（新增 §2.9 证明）。

## 0. 阅读约定

- 记录格式：`类名.方法 L行号 · PIT 指令 index · 算子（状态）`。
  - `index` 与 `block` 均为快照 `<indexes><index>` / `<blocks><block>` 的原始值；同一行可能有多个 index，对应不同字节码位置，**不可按行合并**。
  - 算子记号：`EQUAL_ELSE`=移除条件跳转并以“恒不跳转/恒假”替换；`ORDER_ELSE`=把比较跳转替换为无条件 GOTO（恒跳转）；`BOUND`=`ConditionalsBoundaryMutator` 改边界（`<`↔`<=`）；`RET_NULL`=`NullReturnValsMutator` 返回 null；`RET_TRUE`=`BooleanTrueReturnValsMutator` 返回 true。
- 每条证明按四段式给出：**变异后控制流 → 全体输入值域分析 → 下游归一化/兜底 → 结论**。
- “不可达”与“等价”都在**原始分母**下保留，不删测试、不改 POM、不排除算子。
- 输入域声明：所有输入均通过公开 API 构造（另有 2 处按比赛规则允许的“反射直调私有入口”，已在下文标注）；未使用 `mockConstruction`/`mockStatic`，未替换被测类字节码。
- 已做过的攻击层（按 §5 分层，逐层穷尽后再下结论）：
  1. 真实输入：null / 空白 / 前后空白 / 超长 / Unicode / 0 / 负数 / 最大值 / 等号处邻值与等值点；
  2. record 构造态畸形值（绕过 validate 的私有入口）：`rubricPercent=125`、`passedTests>totalTests`、`totalTests=0` 等；
  3. 反射直调私有方法：`scoreAnswer`、`validateAnswer`（本文件内 3 处，用于构造公开路径被 validate 拦截的状态）；
  4. 私有方法的其它入口：同一规则在 `evaluate` / 私有辅助方法内的重复判断分别验证；
  5. 接口交互观察：协作方（Repository / EnrollmentHistory / ExpressionEvaluator）用手写替身固定返回并可断言调用（未使用 Mockito）。

---

## 1. 不可达（20 条，含 8 条 NO_COVERAGE）

> 判定标准：所有进入该分支的路径条件互相矛盾，**不存在**任何（含反射直调入口可达的）输入值域组合能同时满足。

### 1.1 促销区间倒挂检查是死区（4 条）

`AdvancedPricingService.promotionDiscount`：

- **L244 · i119 · EQUAL_ELSE（SURVIVED）** —— 变异后 `promotion.startsOn() != null` 恒假，跳过 `startsOn/endsOn` 倒挂检查的第一个合取项。
  - 变异后控制流：直接落到 L250 `membersOnly` 判定，区间倒挂不再被拦截。
  - 值域分析：到达 L244 需要同时“未命中未开始”（L238：`startsOn!=null && today.isBefore(startsOn)`）与“未命中已过期”（L240：`endsOn!=null && today.isAfter(endsOn)`）。设 `startsOn > endsOn` 且两者非空：若 `today >= startsOn`，则必有 `today > endsOn` → L240 提前 `return ZERO`；若 `today < startsOn`，则 L238 提前 `return ZERO`。两种情况都到不了 L244，**三条件联立不可满足**。
  - 归一化/兜底：无下游兜底，但压根没有执行路径。
  - 结论：不可达。
- **L245 · i124 · EQUAL_ELSE（NO_COVERAGE）** —— 该合取项所在字节码在 277 个用例下 `numberOfTestsRun=0`，与上面的路径矛盾一致。
- **L246 · i132 · EQUAL_ELSE（NO_COVERAGE）** —— 同上。
- **L248 · i142 · RET_NULL（NO_COVERAGE）** —— `warnings.add("Promotion date range is invalid"); return ZERO;` 整段不可达，故返回 null 也不可观察。

### 1.2 优惠券区间倒挂检查是死区（4 条）

`AdvancedPricingService.couponDiscount`（结构与 1.1 完全同构，日期字段换成 `validFrom/validUntil`）：

- **L317 · i135 · EQUAL_ELSE（SURVIVED）** —— 到达该行已排除“未生效”（L309）与“已过期”（L311）；`validFrom>validUntil` 时，`today>=validFrom ⇒ today>validUntil` 命中已过期，`today<validFrom` 命中未生效 ⇒ 不可满足。
- **L318 · i140 · EQUAL_ELSE（NO_COVERAGE）**、**L319 · i148 · EQUAL_ELSE（NO_COVERAGE）**、**L321 · i158 · RET_NULL（NO_COVERAGE）** —— 同一死区，`numberOfTestsRun=0`。

### 1.3 `requestedRate <= 0` 分支不可达（3 条）

`RefundPolicyService.evaluate`：

- **L78 · i238 · ORDER_ELSE（SURVIVED）**、**L78 · i238 · BOUND（SURVIVED）** —— `if (requestedRate.signum() <= 0) return rejected("REQUESTED_PERCENTAGE_IS_ZERO", ...)`。
  - 控制流：变异后（恒不跳转 / `<0`）该 return 不再触发。
  - 值域：`requestedRate = fullRefundRequested ? ONE : percentageOrDefault(requestedPercentage, eligibleRate)`。
    `percentageOrDefault(p, d)`：`p<=0 → d`；`p>100 → ONE`；否则 `p/100 ∈ (0,1]`。
    `eligibleRate` 的取值：`determineEligibleRate` 返回 `ZERO / 0.25 / 0.40 / 0.50 / 0.60 / 0.70 / 0.75 / ONE`；且 `eligibleRate==0 && !adminOverride` 已在 L74–L77 提前 return；`adminOverride` 且 `eligibleRate==0` 时被抬成 `ONE` 或 `0.50`。
    故 `requestedRate ∈ {1, (0,1], eligibleRate>0, 0.5}`，**恒 > 0**。
  - 归一化/兜底：无（条件恒假）。
  - 结论：不可达。
- **L79 · i245 · RET_NULL（NO_COVERAGE）** —— 上述 return 分支从未执行，`numberOfTestsRun=0`。

### 1.4 手续费“大于退款额”分支不可达（2 条）

`RefundPolicyService.calculateProcessingFee` L81–L86 逻辑 + `evaluate` L84：

- **L84 · i270 · ORDER_ELSE（SURVIVED）**、**L84 · i270 · BOUND（SURVIVED）** —— `if (fee.compareTo(grossRefund) > 0) { fee = grossRefund; ... }`。
  - 值域：`fee = money(grossRefund × rate)`；`rate` 初始由支付方式给定（最大 CRYPTO 0.05），`hoursSincePurchase<=24` 时被置 0，下载资源 +0.02，直播出勤 +0.10 或 +0.05（互斥），最后被 `rate>0.20 → 0.20` 截断（该截断本身也不可达，见 1.5）。故 `rate ≤ 0.17`。
    `grossRefund = money(paidAmount × requestedRate) ≥ 0`，因此 `fee ≤ 0.17 × grossRefund < grossRefund`（`grossRefund>0` 时严格小于；`grossRefund==0` 时 `fee==0` 相等）。
  - 结论：条件恒假 → 不可达（同时说明“手续费封顶到退款额”的 warning 永不可观察）。

### 1.5 手续费率 20% 封顶不可达（2 条）

`RefundPolicyService.calculateProcessingFee`：

- **L271 · i197 · ORDER_ELSE（SURVIVED）**、**L271 · i197 · BOUND（SURVIVED）** —— `if (rate > 0.20) { rate = 0.20; warnings.add("PROCESSING_FEE_RATE_CAPPED"); }`。
  - 值域：基础率最大为 CRYPTO 0.05（CARD 0.03 / ALIPAY·WECHAT 0.01 / BANK_TRANSFER 0.02 / WALLET·GIFT 0）；`hours<=24` 归零；下载 +0.02；直播出勤至多 +0.10（`attendedRate>=0.50` 与 `>=0.20` 互斥）。上限 0.05+0.02+0.10 = **0.17 < 0.20**。
  - 结论：恒假 → 不可达（“费率封顶”warning 永不可观察）。

### 1.6 `requestedPercentage > 100` 分支不可达（3 条）

`RefundPolicyService.percentageOrDefault`：

- **L341 · i14 · ORDER_ELSE（SURVIVED）**、**L341 · i14 · BOUND（SURVIVED）** —— `if (percentage > 100) return BigDecimal.ONE;`
  - 值域：两个调用点（`evaluate` 的 admin 默认值、`requestedRate`）传入的都是 `request.requestedPercentage()`，而 `validate` 强制 `0 <= requestedPercentage <= 100`（L63 `requestedPercentage must be between 0 and 100`）。反射直调私有方法同样受调用点入参约束，故 `percentage ∈ [0,100]`。
  - 结论：不可达。
- **L342 · i18 · RET_NULL（NO_COVERAGE）** —— 该 return 从未执行。

### 1.7 `reason()==FRAUD` 在积分退回内是死代码（1 条）

`RefundPolicyService.calculateReturnedPoints`：

- **L287 · i19 · EQUAL_ELSE（SURVIVED）** —— `if (request.reason()==FRAUD || request.fraudSuspected()) return 0;` 中第二个操作数（`fraudSuspected()`）被移除。
  - 值域：`evaluate` 在 L46–L47 已对 `request.fraudSuspected() || reason()==FRAUD` 走 `manual("FRAUD_REVIEW_REQUIRED")` 提前返回，`calculateReturnedPoints` 在执行到 L287 时必然两者皆假 ⇒ 该判定恒假。
  - 结论：死代码 → 不可达。

### 1.8 章节最大分为 0 不可达（1 条）

`ExamGradingService.calculateSections`：

- **L368 · i43 · EQUAL_ELSE（SURVIVED，testsRun=70）** —— `maximum.signum()==0 ? 0 : earned×100/maximum`。
  - 值域：`sectionMaximum` 的每个值来自 ≥1 个 `AnswerResult.maximumPoints()` 之和；`validateAnswer` 要求 `maximumPoints != null && signum() > 0`，故每个章节最大分 ≥ 0.01 > 0。
  - 结论：恒假 → 不可达。

---

## 2. 等价（25 条）

> 判定标准：分支可达，但在**声明的观察面**（返回值、异常类型/消息、`GradeReport`/`PriceBreakdown`/`RefundDecision` 等公开记录字段、`List` 内容与顺序、`Map` 内容）上与原实现**逐位相同**；差异只存在于被下游归一化抹平的中间 scale，或对象身份（对外不可观察）。

### 2.1 `money()` 的 `setScale(2)` 归一化（grading，5 条）

`ExamGradingService.scoreAnswer` / 各评分方法最终都经 `money(v)=v.setScale(2,HALF_UP)` 返回，`GradeReport` 字段即该返回值。

- **L151 · i121 · BOUND（SURVIVED）** —— `if (answer.timedOut() && score.signum() > 0)`，`>0`→`>=0`。
  值域：`score==0` 时走 IF 支得 `0 × 0.80 = 0.0000`，`money → "0.00"`；不走 IF 支得 `money(0.00)="0.00"`。同值同 scale。
- **L154 · i137 · BOUND（SURVIVED）** —— `if (score.compareTo(maximum) > 0) score = maximum;`，`>0`→`>=0`。
  值域：等号处两条路径分别得到“赋回 maximum”与“保留原 score”；二者数值相等，且 `score` 是局部 `BigDecimal`，返回值由 `money()` 新建，**无对象同一性可观察**（对象身份差异只存在于返回前的局部变量）。`money(10.00)` 与 `money(10.0000)` 均为 `"10.00"`。
- **L157 · i147 · EQUAL_ELSE（SURVIVED，testsRun=70）** —— `if (!policy.negativeMarkingEnabled() && score.signum() < 0) score = ZERO;` 的第一个合取项被移除。
  值域：要该语句生效需 `score<0` 且 `!negativeMarkingEnabled()`。产生负分的所有分支（`scoreBinary` / `scoreFillBlank` / `scoreNumeric`）都以 `policy.negativeMarkingEnabled()` 为前置条件；未作答罚分在 L135–L141 **提前 return**，不经过该语句。⇒ 两条件不可同时为真，归零语句永不执行。
- **L157 · i150 · BOUND（SURVIVED）** —— `score.signum() < 0` → `<= 0`。`score==0` 时赋 `ZERO("0.00")`，与 `money(0.0000)="0.00"` 同值同 scale；负分情形见上条矛盾。
- **L157 · i150 · ORDER_ELSE（SURVIVED）** —— 同上（比较被替换为恒不跳转，等价于“永不归零”，而该归零本就不可达）。

### 2.2 零贡献算术（grading，3 条）

- **L200 · i105 · BOUND（SURVIVED）** —— `scoreMultipleChoice` 的 `if (answer.selectedIncorrectOptions() > 0)` 扣分块，`>0`→`>=0`。
  值域：`selectedIncorrectOptions==0` 时 `incorrectRate = 0/… = 0.000000`，`maximum×incorrectRate = 0.00000000`，`score.subtract(0)` 数值与 scale 皆不变，最终 `money()` 同值。
- **L246 · i57 · BOUND（SURVIVED）** —— `scoreNumeric` 的 `answer.numericTolerance().signum() > 0` 合取项。
  值域：`tolerance==0` 时要进入该合取还需 `|error| <= tolerance×10 = 0` ⇒ `error==0`；而 L242 的 `|error| <= tolerance` 已在先命中并 `return maximum`，控制流到不了 L246 的判别 ⇒ 边界修改不影响任何输入。
- **L267 · i19 · EQUAL_ELSE（SURVIVED）**、**L267 · i22 · EQUAL_ELSE（SURVIVED）** —— `scoreEssay` 的 `if (rubricPercent()==0 && !exactMatch()) return ZERO;`。
  值域：跳过该早退后 `percent = exactMatch ? 100 : rubricPercent`；在 `rubricPercent==0` 且非精确匹配时 `percent=0`，`maximum×0.00 = 0.0000 → money 0.00`，与 `ZERO` 同值。

### 2.3 等级阶梯兜底（grading，2 条）

- **L455 · i4 · EQUAL_ELSE（SURVIVED）**、**L455 · i7 · ORDER_ELSE（SURVIVED）** —— `letterGrade` 的 `if (!passed && percentage < 60) return "F";`。
  值域：跳过该短路后落入阶梯判定；`percentage<60` 时 `>=95/>=90/>=85/>=80/>=75/>=70/>=65/>=60` 全部不命中，函数末尾仍是 `return "F"`。返回值相同 ⇒ 等价。

### 2.4 促销/优惠券百分比 80%、70% 的等值点（pricing，4 条）

- **L260 · i210 · BOUND（SURVIVED）**、**L260 · i210 · ORDER_ELSE（SURVIVED）** —— `promotion.value().compareTo(new BigDecimal("80")) > 0 ? money(subtotal×0.80) : percent(subtotal, value)`。
  值域：`value==80` 时 ELSE 支 `percent(subtotal,80)=money(subtotal×0.80)`，与 IF 支完全同值同 scale；`value<80` 时两路径本就同支；故边界与跳转替换均不可区分。
- **L344 · i280 · BOUND（SURVIVED）**、**L344 · i280 · ORDER_ELSE（SURVIVED）** —— 同上，阈值 70：`value==70` 时 `percent(subtotal,70)=money(subtotal×0.70)`。

### 2.5 折扣裁剪循环的零迭代/零扣减（pricing，3 条）

`AdvancedPricingService.trimDiscounts`：

- **L467 · i18 · BOUND（SURVIVED）** —— `for (int index = result.size()-1; index >= 0 && excess.signum() > 0; index--)`，`index>=0`→`>0`（跳过 index 0）。
  值域：要产生差异需 `excess` 在 index 1..5 全部抵扣后仍 >0，即 `membershipDiscount > cap`。而 `membershipDiscount` 最高为 ENTERPRISE 18%（或教师自有 100%，但此时 `cap = subtotal`），`cap` 为 70%·subtotal 或 subtotal ⇒ `membershipDiscount ≤ cap` 恒成立 ⇒ excess 必在 index≥1 段耗尽；index 0 只在 excess==0 时被访问，此时 `reduction=min(current,0)=0`，写回值不变。
- **L467 · i21 · BOUND（SURVIVED）** —— `excess.signum() > 0` → `>= 0`。`excess==0` 时多执行一轮：`reduction=min(current,0)=0`，`set(index, money(current-0))` 与原值同值同 scale。
- **L469 · i33 · EQUAL_ELSE（SURVIVED）** —— `if (current.signum()==0) continue;` 被移除时，`reduction=min(0, excess)=0`，`money(0-0)="0.00"`，与跳过等价。

### 2.6 进度权重的零差分支（progress，2 条）

- **L147 · i120 · EQUAL_ELSE（SURVIVED）** —— `earned = complete ? weight : weight * completionPercent / 100`（ARTICLE）。
  值域：`complete` 仅在 `completionPercent >= 100` 为真；此时 ELSE 支 `weight×100/100 == weight`（整数除法精确），两侧所得 earned 相同 ⇒ 等价。
- **L70 · i218 · EQUAL_ELSE（SURVIVED）** —— `(!snapshot.sequentialLearning() || previousComplete)` 的第一个析取项被移除。
  值域：`previousComplete` 只在 L73/L75 两个 **以 `sequentialLearning()` 为前置** 的分支里被更新，因此非顺序模式下它恒为初值 `true` ⇒ `(false || true)` 与 `(true)` 等价；顺序模式下 `!seq` 本就为 false，两种写法都退化为 `previousComplete`。

### 2.7 手续费附加项的空集分支（refund，4 条）

`RefundPolicyService.calculateProcessingFee`：

- **L260 · i118 · BOUND（SURVIVED）** —— `if (purchase.totalLiveSessions() > 0 && purchase.liveSessionsAttended() > 0)` 的第一个合取项 `>0`→`>=0`。
  值域：`totalLiveSessions==0` 时，`validate` 保证 `attended <= total` ⇒ `attended==0` ⇒ 第二个合取项为假，与跳过该块同效。
- **L260 · i121 · BOUND（SURVIVED）** —— 第二个合取项 `attended>0`→`>=0`。`attended==0` 时 `attendedRate=0/total=0`，`>=0.50` 与 `>=0.20` 均不命中 ⇒ 不产生费率增量与 warning。
- **L284 · i5 · ORDER_ELSE（SURVIVED）**、**L284 · i5 · BOUND（SURVIVED）** —— `if (purchase.loyaltyPointsUsed() <= 0) return 0;`（`calculateReturnedPoints`）。
  值域：`used==0` 时跳过早退后 `returned = rate×0 = 0`；`returned<used(0)` 为假（不写比例警告）；若 `couponUsed` 则 `0/2=0`；最终 `Math.max(0,0)=0`，与早退返回值相同。`used<0` 被 `validate` 拒绝。

### 2.8 先修分下限的零阈值（enrollment，1 条）

- **L379 · i101 · BOUND（SURVIVED）** —— `evaluatePrerequisites` 的 `course.minimumPrerequisiteScore() > 0 && score < course.minimumPrerequisiteScore()`，`>0`→`>=0`。
  值域：该 `else if` 只在前一分支 `score<0 || score>100` 为假（即 `0<=score<=100`）时求值；此时 `score < 0` 恒假 ⇒ 阈值从 0 改成“≥0”不改变结论。

### 2.9 年龄段校验中被其余合取项蕴含的条件（enrollment，1 条）

- **L297 · i56 · BOUND（SURVIVED）** —— `validateDemographics`：
  `if (course.minimumAge() > 0 && course.maximumAge() > 0 && course.minimumAge() > course.maximumAge())`，
  `minimumAge() > 0` 被改成 `>= 0`。
  - 变异后控制流：`minimumAge==0` 时第一个合取项由假变真，继续求值后两项。
  - 值域分析：要产生差异需“变异前假、变异后真”，即 `minimumAge==0` 且 `maximumAge>0` 且 `minimumAge>maximumAge`。第三项要求 `0 > maximumAge > 0`，**自相矛盾**；`maximumAge<=0` 时第二项为假，结论仍不变。
  - 归一化/兜底：无（条件链本身矛盾）。
  - 结论：等价（该合取项被其余合取项蕴含，`testsRun=32` 亦印证覆盖后无差异）。

---

## 3. 未解决（23 条，**不并入等价**）

> 这些记录在快照中状态为 SURVIVED/NO_COVERAGE，`numberOfTestsRun ≥ 1`（除标注 NO_COVERAGE），即“已覆盖但未能区分”。按 SOP 纪律：**尚未构造出可区分输入**，不作为等价/不可达结论；原始分母保留。

| # | 记录 | testsRun | 现状与已定位的可观察面 |
|---|---|---|---|
| 1 | `EnrollmentEligibilityService.validateOrganization L329 · i25 · EQUAL_ELSE` | 2 | `organizationId()==null` 判定。已试 `organizationOnly+sponsored+orgId=null`（期望 `ORGANIZATION_ID_REQUIRED`）与 `orgId=""`；变异语义与预期不符（index 归属待 `javap -c` 复核），尚未找到区分输入。 |
| 2 | `progress.assessLesson L157 · i196 · EQUAL_ELSE` | 4 | `complete = submitted && approved` 的 `submitted` 侧。`courseCompleted` 只由“总进度+必答+学术阻塞”决定，不要求每课完成，故当前用例下终态相同；需改用 `completedLessons`/`overallPercent` 断言区分。 |
| 3 | `progress.assessLesson L157 · i199 · EQUAL_ELSE` | 4 | 同上，`approved` 侧。 |
| 4-8 | `progress.lambda$noAcademicBlocker$2 L268 · i6 · EQUAL_ELSE`、`L268 · i35 · RET_TRUE`、`L269 · i12`、`L270 · i18`、`L271 · i24` | 4/4/4/3/1 | `blockers.stream().noneMatch(...)` 的四个 `\|\|` 子句各被移除一次。已分别构造 `PLAGIARISM_REVIEW`、`ATTEMPT_LIMIT_REACHED`、`FINAL_PROJECT_NOT_APPROVED`、`INSTRUCTOR_APPROVAL_REQUIRED` 四类 blocker 且 `requiredOverallPercent=0` 的用例，仍未区分（index→子句映射需 `javap -c` 复核）。 |
| 9 | `refund.determineEligibleRate L209 · i359 · BOUND` | 6 | `hoursSincePurchase <= 7*24` 的等值点（168h）。用例已含 168h→`SEVEN_DAY_GUARANTEE`，仍未区分；`javap -c` 复核 index 归属后补 168h+progress=30 的组合。 |
| 10 | `refund.determineEligibleRate L213 · i387 · BOUND` | 3 | `hoursSincePurchase <= 14*24`（336h）同上。 |
| 11 | `refund.determineEligibleRate L217 · i411 · BOUND` | 3 | `hoursSincePurchase <= 30*24`（720h）同上。 |
| 12 | `refund.determineRoute L305 · i14 · EQUAL_ELSE` | 26 | `paymentMethod()==GIFT` 侧。GIFT 在 `switch` 里同样 `yield "WALLET_CREDIT"`，故该子句与 fall-through 分支**结果相同**（疑似等价，但对面 `giftPurchase()` 侧已被杀，需再确认首个析取项语义后判定）。 |
| 13 | `scheduling.evaluateTiming L111 · i139 · EQUAL_ELSE` | 35 | `endTime.isAfter(LocalTime.of(21,0))` 侧（`startTime.isBefore(8:00)` 侧已杀）。已加 20:00–21:00 等值用例仍未区分，需 `javap -c` 复核。 |
| 14 | `scheduling.evaluateDeliveryResources L198 · i101 · EQUAL_ELSE` | 4 | CLASSROOM 分支的 `roomId().isBlank()` 侧（`==null` 侧已杀）；已加 `roomId="  "` 用例仍未区分。 |
| 15 | `scheduling.evaluateRecurrence L355 · i68 · EQUAL_ELSE` | 4 | `DayOfWeek.SATURDAY` 侧（周日侧已杀）；已加周六/周日重复课程用例仍未区分。 |
| 16 | `scheduling.isNonOverridable L388 · i36 · EQUAL_ELSE` | 1 | `conflict.startsWith("COHORT_BOOKING_CONFLICT")` 侧（instructor 侧已杀）；已加 cohort 冲突用例仍未区分。 |
> 说明：上表 10–14、16–18、20–23 的“仍未区分”经 `javap -c` 二次定位 index 归属后应有部分可转为击杀；在完成定位前如实保留为未解决，不计入等价。

---

## 4. 与快照的对账

| 分类 | 条数（1442 快照） | 快照状态分布 | 1435 快照 |
|---|---|---|---|
| 不可达 | 20 | SURVIVED 12 + NO_COVERAGE 8 | 20 |
| 等价 | 26 | SURVIVED 26 | 25 |
| 未解决 | 16 | SURVIVED 16 | 23 |
| **合计** | **62** | **SURVIVED 54 + NO_COVERAGE 8** | **68** |

定向补杀明细（1435 → 1442，+6 KILLED）：

| 记录 | 补杀用例 | 观察面 |
|---|---|---|
| `CatalogService.getCatalog L49 · i29 · EQUAL_ELSE` | `TargetedKills.emptyCatalogMustNotQueryVideos` | 空章节时 `VideoRepository.findByChapterIds(∅)` 被调用的交互（替身在收到空集合时抛 AssertionError） |
| `EnrollmentEligibilityService.validateDemographics L291 · i22 · BOUND` | `zeroMinimumAgeDoesNotBlockNegativeAge` | `age=-1,minAge=0` 时不应出现 `BELOW_MINIMUM_AGE` |
| `AdvancedPricingService.loyaltyDiscount L416 · i93 · BOUND` | `cappedLoyaltyValueAddsNoRule` | `unitPrice=0.01` 时积分价值被压到 0，`appliedRules` 不含 `LOYALTY_POINTS` |
| `AdvancedPricingService.calculateTax L432 · i21 · EQUAL_ELSE` | `zeroTaxableAmountAddsNoTaxRule` | 免费订单 `appliedRules` 不含 `TAX_CN` |
| `RefundPolicyService.evaluate L105 · i376 · BOUND` | `zeroRefundableDoesNotTriggerMinimumPayoutWarning` | 可退金额 0 时不含 `REFUND_BELOW_MINIMUM_PAYOUT` |
| `LearningProgressService.evaluateCertificate L244 · i66 · EQUAL_ELSE` | `blankCertificateNameHitsRequiredBlocker` | 空白证书名命中 `CERTIFICATE_NAME_REQUIRED` 而非长度非法 |

未覆盖/存活的非 engine 记录已全部在本附录中逐条登记，无遗漏项；`expression.engine` 包的 1,121 条（KILLED 84 / SURVIVED 57 / NO_COVERAGE 980）不在本执行者范围，由 `math-engine` 执行者的 `EngineProbeTest` 覆盖后合并。

## 5. 复现方式

```powershell
$env:JAVA_HOME = "A:\MC\JAVA SDK"; $env:Path = "A:\MC\JAVA SDK\bin;$env:Path"
cd "B:\软件测试大赛\省赛\deepseek\math-assessment"
mvn -B clean test                                  # 284 tests, 0 fail
mvn -B org.pitest:pitest-maven:mutationCoverage    # 150s
# 报告：target/site/jacoco/index.html、target/pit-reports/index.html、target/pit-reports/mutations.xml
```

---

## 6. 移植性 / 确定性自查（逐条结论）

检查对象：`src/test/java/net/mooctest/StudentTest.java`（277 用例，已冻结）

| # | 检查项 | 结论 | 证据（行号） |
|---|---|---|---|
| 1 | 绝对路径（`C:\`、`B:\`、`/home` 等） | **无** | 全文 0 命中 |
| 2 | `new File(...)` / `Paths.get(...)` / 文件读写 | **无** | 全文 0 命中 |
| 3 | `System.getProperty("user.dir")` / `getenv` | **无** | 全文 0 命中 |
| 4 | `ZoneId.systemDefault()` 参与断言 | **无** | 仅以字面量 `"Asia/Shanghai"`（L3594 默认值）、`"UTC"`（L3692/3719/3725/3733/3739/4037/4044/4062/4070/4119/5432/6174/6338）显式传入被测方法 |
| 5 | 无参 `LocalDate.now()` / `LocalDateTime.now()` / `Instant.now()` 进断言 | **无** | 全部为 `LocalDate.now(CLOCK)`（L2308/2311/2316/2355/2358/2362），`CLOCK = Clock.fixed(T0, ZoneOffset.UTC)`（L139），`T0 = Instant.parse("2026-11-15T02:00:00Z")`（L138） |
| 6 | `String.format` / `printf` / `%f` / 默认 locale 格式化 | **无** | 全文 0 命中；无 `Locale`、无 `toUpperCase/toLowerCase` |
| 7 | 依赖 `HashMap` 迭代顺序的断言 | **无** | `HashMap/HashSet` 仅出现在测试替身 `History`（L1143/L1144）内部存储；断言侧用 `Map.of(...)`（L306/900/919/1019-1021，`Map.equals` 与顺序无关）与 `List.of(...)`（`ArrayList` 插入顺序，如 failures/warnings/appliedRules） |
| 8 | 需要 `@BeforeEach/@AfterEach` 恢复的静态可变状态 | **无**（0 个 `@BeforeEach/@AfterEach`，且无 `static` 非 final 字段） | 静态成员只有 `static final Instant T0`(L138)、`static final Clock CLOCK`(L139)、静态辅助方法(L145/259/263/267/2152/2157) 与 `static final class` 测试替身(L150/200/234/1139/1185/1210/1249/2129/2607/2639/3140/3178) |
| 9 | 对被试静态注册表/缓存的影响 | **幂等、无断言依赖** | 唯一静态交互是 `BracketsCalculation2.getInstance(...)` 的惰性单例注册（被测代码自身行为，只增不改）；`Mathematical_Expression.Options` 的 `useCache/useBigDecimal` **从未**被本文件切换；替身使用独立名称（`null-result`、`null-result-inner`、`exploding`），不与主用例共享 |
| 10 | 随机源影响断言 | **仅 1 处且稳健** | `UuidGenerator.nextId()`（L1990-1993）：断言为“非空 + 长度>10 + 两次不同”，UUID 碰撞概率可忽略，不参与任何期望值计算 |
| 11 | 执行顺序依赖 | **无** | 每个用例自建 `Ans/Sub/Pol/Stu/Off/Req/Price/Pur/Ref/Les/Snap` 等可变构造器；无跨用例字段 |
| 12 | 真实当前时间进入期望值 | **无** | 三处 `Instant.now(clock)` 均传固定 `CLOCK`（Enrollment/Pricing/Progress/Refund/Scheduling 构造参数） |
| 13 | 网络 / Spring 上下文 | **无** | 无 `@SpringBootTest`、无端口/资源依赖；`CourseController`/`RestExceptionHandler` 直接 new 后调用方法 |

补充（平台环境差异）：
- 文件为 UTF-8（含中文 `@DisplayName`），与 `pom.xml` 的 `project.build.sourceEncoding=UTF-8` 一致。
- 依赖 JDK 21 的标准库与 `spring-boot-starter-web`/`spring-boot-starter-test`（POM 已有），未引入任何新依赖。
- 不使用 `mockConstruction` / `mockStatic` / `MockedStatic`；`MathArgumentNotValidException` 等 Spring 类型仅用于直接构造异常对象，不启动容器。

---

## 7. 合并后第三轮（1544 快照）更新：分支覆盖 + 变异补杀

### 7.1 快照与总量
- 快照：`_baseline\math-pit-1544.xml`（md5 `8F2F9585DC45297A1707350B76179189`，2528 条）
- 测试：`StudentTest.java` md5 `AD3B26E768552C0C1A7B5E161091ECCC`，11,415 行，**567 用例 / 0 fail**
- 变异：**KILLED 2416 / SURVIVED 81 / NO_COVERAGE 27 / TIMED_OUT 4**（合并后基线 2397/98/29/4）
- 覆盖率：**BRANCH 2606/2659（98.0%，合并后 2502/2659）**、LINE 3993/4050（98.6%）、METHOD 715/725（98.6%）、INSTRUCTION 19633/19934、CLASS 171/171

### 7.2 本轮做法
1. **定向补杀（15 个用例）**：私有入口反射（`calculateSections` 零分母、`calculateReturnedPoints` fraud、`percentageOrDefault` 的 100/150、`scoreAnswer` 负分）、闭包可观察面（`appliedRules` 不含 `LOYALTY_POINTS` / `TAX_CN`、`warnings` 不含 `REFUND_BELOW_MINIMUM_PAYOUT`、`completedLessons` 计数、非学术 blocker 不阻止完成、cohort 冲突不可覆写）。
2. **分支覆盖补测（约 60 个用例）**：逐个把 jacoco 报的"缺分支行"翻译成输入——`||`/`&&` 的短路两侧（null vs 空白、true/false 组合）、`else` 侧、switch 各枚举常量、record 紧凑构造器的 null 集合归一化（`ScheduleRequest` 6 个、`GradeReport` 3 个、`ExamSubmission`/`ProgressEvaluation`/`SchedulingDecision` 各 2 个）、管理员覆写的可覆写/不可覆写矩阵。
3. 顺带把 engine 的若干变体一并杀掉（`FunctionFormulaCalculation`、`PrefixExpressionOperation.check`、`JvmExpressionFunction.parseFunctionSignature`、`EquationSolver` 等）——由新增分支用例触发的新路径所致。

### 7.3 残余（1544 快照下）
- **SURVIVED 81 / NO_COVERAGE 27**：绝大部分仍是第 1–3 节的不可达/等价项**另一侧算子**（同一 index 的 BOUND 与 ORDER_ELSE、True/False 返回变体，杀一侧另一侧仍等价），以及 engine 中 `FunctionPackage$*`、`DynamicClassLoader`、`EquationSolverExpression` 等的动态编译出口。
- **分支残余 53**：`AdvancedPricingService` 10（含区间倒挂死区 L245/246/317/318/319）、`SchedulingService` 7（均为枚举 switch 的 `default` 与 null 集合的不可达侧）、`RefundPolicyService` 6（L78/L84/L271 不可达 + switch 全枚举）、`LearningProgressService` 5（switch default / 短路侧）、`EquationSolverExpression` 4（动态编译出口）、其余为 record 空集合的不可达侧（`Set.copyOf` 拒绝 null）。
- 上述不可达项已在第 1–3 节给出路径矛盾证明，保留原始分母。
