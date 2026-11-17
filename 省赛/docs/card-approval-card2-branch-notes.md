# card-approval 分支覆盖补强（CardProbeTest2）

## 结果（`mvn -B clean test`，StudentTest + CardProbeTest + CardProbeTest2 三套一起）

| 指标 | 补之前 | 补之后 | 变化 |
|---|---|---|---|
| **BRANCH** | 1921 / 2013 = 95.43% | **1999 / 2013 = 99.30%** | **+78** |
| LINE | 2356 / 2364 | 2356 / 2364 | 0（本来就是 99.66%）|
| METHOD | 275 / 275 | 275 / 275 | 0 |
| INSTRUCTION | 11353 / 11385 | 11353 / 11385 | 0 |
| 用例数 | 184 | **247** | +63 |

分支缺口从 92 条降到 14 条；32 个缺口文件里 **23 个已经 100% 分支覆盖**。
按 `mutation 25 / branch 25 / method 25 / statement 25` 的权重，branch 一项从 23.86 分升到 24.82 分。

## 逐文件缺口（before → after）

| 文件 | 缺前 | 缺后 | 本轮补上 |
|---|---|---|---|
| AccessPolicy | 10 | 0 | 10 |
| AdvancedLeavePolicy | 9 | 2 | 7 |
| AttendancePolicy | 7 | 0 | 7 |
| ExpensePolicy | 7 | 4 | 3 |
| ExpenseCardReview | 6 | 0 | 6 |
| TaskPolicy | 6 | 2 | 4 |
| ApprovalWorkflowEngine | 5 | 1 | 4 |
| CorporateCardExpenseService | 5 | 0 | 5 |
| ApprovalStep | 3 | 0 | 3 |
| ExpenseClaim | 3 | 0 | 3 |
| OvertimePolicy | 3 | 0 | 3 |
| Shift | 3 | 3 | 0 |
| AdvancedLeaveRequest / MaestroUK / Maestro / WorkflowSnapshot / Verve | 各 2 | 见下 | 7 |
| AccessContext / AttendanceInput / CardEvidence / ChinaUnionPay / DateRange /
  DinersClubInternational / ExpenseItem / InterPayment / SubmitLeaveCommand / Task /
  TaskContext / TaskStateMachine / Validator / Visa / WorkflowAction | 各 1 | 见下 | 11 |

## 用例 → 缺口对照（63 个用例，每组 @DisplayName 里带 `类.方法 L行号`）

**构造器/校验器非法入参（null 与 blank 两条分支各打一次，断言异常消息原文）**
- `expenseRecordGuards` → `ExpenseItem.<init>` L24、`ExpenseClaim.<init>` L21/L24/L41
- `expenseCardReviewGuards` → `ExpenseCardReview.<init>` L19 + 派生标志 L27/L28/L32（6 条）
- `approvalStepNullApprover` / `workflowRecordGuards` → `ApprovalStep.<init>` L19/L22/L79、
  `WorkflowSnapshot.<init>` L20/L23
- `workflowActionGuard` → `WorkflowAction.<init>` L13
- `taskAndTaskContextGuards` → `Task.<init>` L82（id/title/creatorId 三条消息）、`TaskContext.<init>` L21
- `shiftNightAndAttendanceInputGuards` → `Shift.<init>` L36、`AttendanceInput.<init>` L23
- `remainingRecordGuards` → `SubmitLeaveCommand.<init>` L16、`AccessContext.<init>` L30、`CardEvidence.<init>` L9
- `advancedLeaveRequestGuards` / `advancedLeaveRequestNullEmployee` → `AdvancedLeaveRequest.<init>` L26/L47

**AccessPolicy（10）**：`employeeAllows L141`、`departmentManagerAllows L172`、`humanResourcesAllows
L196/L204`（两个方向都测）、`financeAllows L228`、`administratorAllows L250`、`scopeAllows
L287/L288/L289/L295/L296`（FINANCE / AUDITOR / EMPLOYEE / HUMAN_RESOURCES 四条路径把 DEPARTMENT
作用域守卫的每个操作数都翻一遍）。

**AttendancePolicy（7）**：`evaluate L31`（无打卡的已批假）、`L42`（公休日/有打卡两种短路）、
`L60`（已批准的补卡仍不完整）、`L85`（手工打卡未审批）、`L175/L176`（HYBRID × remotePunch ×
locationVerified 三种组合）。

**ExpensePolicy（3）** + `CorporateCardExpenseService`（5）+ `ExpenseCardReview`（6）：
服务层用 6 组 claim/actor 把 `ready = allowed && !rejected && issues.isEmpty()` 与
`audit = expense.auditRequired() || access.auditRequired() || !issues.isEmpty()` 的每个操作数
正反各走一次（guest 被拒、个人消费被拒、企业卡缺凭证、大额审计、管理员审计、全部干净）。

**TaskPolicy（4）**：`calculatePriority L56`（24<hours≤72 用 HIGH 20+5=25 与 20 区分）、
`requiresEscalation L130`（BLOCKED 近日/久悬）、`L136`（IN_REVIEW 的 dueAt null / 10h 后 / 2h 后）、
`allowedActions L243`（ARCHIVED 由管理员 RESTORE）、`isOverdue L269`（CANCELLED 不逾期）。

**AdvancedLeavePolicy（7）**：`validateDates L88`（补假 + 紧急 SICK / 补假 + 非紧急 / 补假 + 非 SICK
三路）、`L101`（紧急豁免）、`validateEmployment L135`（实习生非年假）、`validateAnnualLeave L202`
（远程一天假的三个操作数）、`validatePersonalLeave L249`（三个操作数）。

**ApprovalWorkflowEngine（4）**：`act L60`（截止前/截止后）、`act L66`（请求人/他人审批跳过步）、
`delegate L111`（转给请求人且 skip / 转给非请求人 / 非 skip 步转给请求人）、`withdraw L132`
（urgent / 非 urgent 的首次审批后撤回）。

**card（7）**：Verve/Maestro/MaestroUK 的长度上下界（15/20、11/20 位）、Visa 12/20 位、
InterPayment 15/20 位、Diners 15/20 位、ChinaUnionPay 15/20 位、`Validator.checkExpirationDate L94`
（过期卡 + 单参构造）。

## 剩余 14 条：构造器不变量/死代码导致的不可达（附证明）

1. **ExpensePolicy L107（1）** `if (claim.clientBillable())` 位于 `costCenter` 为空的分支内，
   但 `ExpenseClaim` 紧凑构造器禁止 `clientBillable && costCenter.isBlank()`，该组合无法构造。
2. **ExpensePolicy L221（1）** `if (cap.signum() == 0)`：`categoryCap` 的 base 最小 100、
   `gradeMultiplier` 最小 1，再乘 1.5/1.25 后 `money()` 不可能为 0。
3. **ExpensePolicy L272（1）** `switch (item.category())` 覆盖全部 11 个枚举常量，default 不可达。
4. **ExpensePolicy L293（1）** `case MEAL` 内的 `attendeeCount > 0`：`ExpenseItem` 只允许
   CLIENT_ENTERTAINMENT 带 attendeeCount，MEAL 恒为 0。
5. **Shift L36（3）** `if (type == ShiftType.NIGHT && !crossesMidnight())`：紧凑构造器里
   `crossesMidnight()` 读 `this.end`（字段在构造器体之后才赋值）→ 任何 NIGHT 班次必抛 NPE，
   `!crossesMidnight()` 与后续分支永远不可达。`shiftNightAndAttendanceInputGuards` 已把
   "NIGHT 分支被走到"这一步覆盖（断言 NPE）。
6. **AdvancedLeavePolicy L32（1）** `switch (request.type())` 覆盖三个枚举常量，default 不可达。
7. **AdvancedLeavePolicy L173（1）** `chargedDays > 0 && chargedDays < 0.5` 的 `true` 侧：
   chargedDays 只能是整数、0.5 或 0，`(0, 0.5)` 为空域。
8. **TaskPolicy L130（1）** `BLOCKED && startedAt != null` 的 `startedAt != null` 假分支：
   `Task` 构造器要求 BLOCKED 必须有 startedAt。
9. **TaskPolicy L184（1）** `switch (task.status())` 覆盖 8 个状态，default 不可达。
10. **ApprovalWorkflowEngine L181（1）** 前面 `if (snapshot.terminal()) throw;` 已拦下所有
    非 PENDING/IN_PROGRESS 的状态（其余 6 个 WorkflowState 全是 terminal），该条件永不为真。
11. **Validator L94（1）** `expirationMonth != null && expirationYear != null` 的
    `expirationYear` 假分支：两个字段只能由同一个构造器同时赋值。
12. **VerveValidator L62（1）** 第二个 `for` 循环与第一个完全相同，只有在 IIN 不在区间内时才会
    进入，因此循环体的 `IIN == i` 恒为假（重复扫描死代码）。

结论：**可达分支已全部覆盖（1999/1999）**，剩余 14 条均属不可达。

## 备注：surefire 命名

`CardProbeTest2.java` 里的类名以 `2` 结尾，不匹配 surefire 默认 include（`**/*Test.class`），
所以文件末尾附了一个 2 行的别名类 `CardProbeTest2Test extends CardProbeTest2`
（只继承用例、不新增用例），让 `mvn -B clean test` 能跑到它。**合并进 StudentTest 作为 @Nested
类之后请删掉这个别名类**，否则会重复执行一次。

## 附带收益：mutation 也 +1（无回归）

用新增的 branch 用例一起跑全量 PIT（`mvn -B org.pitest:pitest-maven:mutationCoverage`）：

| 指标 | 上一版 | 本版 | 变化 |
|---|---|---|---|
| KILLED / 1320 | 1285 | **1286** | **+1** |
| SURVIVED | 34 | 33 | −1 |
| NO_COVERAGE | 1 | 1 | 0 |

新杀：`card.MaestroValidator.checkLength L37 BooleanTrueReturnValsMutator`
—— 11 位号（低于 12 位下限）让 `checkLength()` 返回 false，从而与"return true"变异体区分开。
已逐条比对：**没有任何旧 mutant 从 KILLED 退回**（0 回归）。

组合后的两项得分：mutation 1286/1320 = 97.42%，branch 1999/2013 = 99.30%。
