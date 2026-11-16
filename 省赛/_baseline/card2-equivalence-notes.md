# card-approval 剩余 35 个未杀变异：等价/不可达证明（card2 / CardProbeTest）

最终成绩（`mvn -B org.pitest:pitest-maven:mutationCoverage`，StudentTest + CardProbeTest 全量）：

| 指标 | r4 基线 | 本轮 | 增量 |
|---|---|---|---|
| KILLED | 1216 | **1285** | **+69** |
| SURVIVED | 102 | 34 | −68 |
| NO_COVERAGE | 2 | 1 | −1 |
| 总变异 | 1320 | 1320 | — |
| 变异得分 | 92.1% | **97.3%** | +5.2pt |

剩余 35 个全部为**逻辑等价**或**记录构造器禁止的不可达分支**。下面每条给出证明要点；注意除
`ExpensePolicy.allowedAmount L222` 外，其余 34 条都已**被测试覆盖**（是 SURVIVED 而不是
NO_COVERAGE），即"跑到了、但行为确实相同"。

## A. 取值域上不可能触发（6 条）

1. **AdvancedLeavePolicy.roundHalfDay L304 idx=6 ConditionalsBoundary / ORDER_ELSE**
   `if (value <= 0) return 0;`
   - 边界 `<=` → `<`：仅 `value == 0` 时不同；`value==0` 时原实现 `return 0`，变异体
     `Math.ceil(0*2)/2.0 = 0.0`，结果相同。
   - ORDER_ELSE（强制走 return 后的语句）：仅 `value < 0` 时不同；`roundHalfDay` 唯一调用点是
     `roundHalfDay(requestedHours/8.0)`，而 `AdvancedLeaveRequest` 强制 `requestedHours > 0`
     （`requestedHours <= 0` 直接抛 IllegalArgumentException），故 `value` 恒 `> 0`。

2. **AdvancedLeavePolicy.validateUnit L173 idx=91/95 ORDER_ELSE**（2 条）
   `if (chargedDays > 0 && chargedDays < 0.5)`
   `chargedDays` 只可能来自三处：整日循环计数（整数）、半日分支（0 或 0.5）、
   `roundHalfDay`（`Math.ceil(v*2)/2.0`，必为 0.5 的整数倍）。因此不存在 `0 < chargedDays < 0.5`
   的取值，整个 then 分支是死代码；两条 ORDER_ELSE 与两条边界变异中，"强制分支不成立"的两个
   与"强制分支成立"以外的情形均与原文等价（两条边界变异已被 CardProbeTest 杀掉）。

3. **AttendancePolicy.positiveMinutes L197 idx=13 ConditionalsBoundary**
   `if (minutes <= 0) return 0;` → `<`：仅 `minutes == 0` 时不同，此时 `Math.toIntExact(0)` 也是 0。

4. **AttendancePolicy.positiveMinutes L197 idx=13 ORDER_ELSE**
   仅 `minutes < 0` 时不同（返回负值）。三个调用点的负值都被吸收：
   - `lateMinutes = positiveMinutes(scheduledStart, clockIn) - grace` 之后紧跟 `Math.max(0, ...)`；
   - `earlyMinutes` 之后 `if (earlyMinutes > grace) ... else earlyMinutes = 0;`；
   - `totalPresence = positiveMinutes(clockIn, clockOut)`，而 `PunchRecord` 保证 `clockOut >= clockIn`。

5. **OvertimePolicy.roundDownToHalfHour L92 idx=5 ORDER_ELSE**
   `if (minutes < 30) return 0; return minutes - minutes % 30;`
   对 `minutes < 30`，`minutes - minutes%30 == 0`，与 `return 0` 相同；`minutes >= 30` 同路。

6. **ExpensePolicy.allowedAmount L221 idx=33 EQUAL_ELSE / L222 idx=37 NullReturnVals**（2 条）
   `if (cap.signum() == 0) return ZERO;`
   `categoryCap` 的 base 最小为 `OTHER=100`、`gradeMultiplier` 最小为 1，再乘 1.5/1.25，`money()`
   后不可能为 0（也不可能为负），故该分支不可达；L222 的 `return ZERO` 因此是死代码，
   永远 NO_COVERAGE。

## B. 记录构造器的前置不变量使分支不可达（4 条）

7. **ExpensePolicy.validateClaimLevel L107 idx=61 EQUAL_ELSE**
   该 `if (claim.clientBillable())` 只在 `costCenter` 为 null/空白时才会执行；但
   `ExpenseClaim` 紧凑构造器明确 `clientBillable && costCenter.isBlank()` → 抛异常。
   故 `clientBillable` 恒为 false，FATAL 分支不可达。

8. **ExpensePolicy.validateCategoryRules L293 idx=92 ORDER_ELSE**
   `case MEAL: if (item.attendeeCount() > 0) violations.add(...)`
   `ExpenseItem` 规定 `category != CLIENT_ENTERTAINMENT && attendeeCount > 0` → 抛异常；
   MEAL 的 `attendeeCount` 恒为 0，分支不可达。

9. **TypeChecker.checkType L24 idx=12 EQUAL_ELSE**
   `if (!number.matches("[0-9]{12,19}")) return OTHER;`
   去掉该早退后，循环里 `matchesType(...)` 自身仍有相同的正则守卫（L44），对非数字输入逐个返回
   false，最终仍落到方法末尾的 `return CreditCardType.OTHER`，结果与原文一致。

10. **Validator.validate L80 idx=44 BooleanTrueReturnVals**
    `return isValid;` 前有 `if (!isValid) throw new InvalidCardException(...)`，
    可达的返回点必然 `isValid == true`，故"返回 true"与"返回 isValid"等价。

## C. 逻辑冗余 / 被上游条件遮蔽（13 条）

11. **ApprovalWorkflowEngine.ensureActive L180 idx=19 / L181 idx=25 EQUAL_ELSE**（2 条）
    `if (state != PENDING && state != IN_PROGRESS) throw "workflow is not active";`
    紧邻其上的 `if (snapshot.terminal()) throw "terminal workflow cannot accept actions";` 已经把
    所有非 PENDING/IN_PROGRESS 的状态拦下（WorkflowState 其余 6 个取值在
    `WorkflowSnapshot.isTerminal` 里全为 terminal）。因此这一行永远不会是抛出者：把它改成"永不抛"
    与原文等价；改成"永远抛"会让 PENDING/IN_PROGRESS 也被拒（CardProbeTest 的
    `workflowActiveStatesAcceptActions` 已覆盖该方向，故变异体实测为 SURVIVED ⇒ 属前者）。

12. **OvertimePolicy.evaluate L59 idx=195 ConditionalsBoundary / ORDER_ELSE**（2 条）
    `if (rawOvertimeMinutes > 360) review = true;`
    当 `raw > 360` 时 `payable = roundDownToHalfHour(raw) >= 360 > 240`，上面
    `if (payable > 240) { payable = 240; review = true; }` 已经把 `review` 置 true 并覆盖了
    `reason`。该行对任何可达输入都不改变结果（边界 360 同理）。

13. **AttendancePolicy.evaluate L75 idx=287 ConditionalsBoundary**
    `if (lateMinutes <= 60) lateMinutes = 0; else lateMinutes -= 60;`（FLEXIBLE 班次）
    唯一差异输入是 `lateMinutes == 60`，此时两支都得到 0（`60-60`）。
    （`lateMinutes` 在进入该块前已 `Math.max(0, …)`，故不存在负值。）

14. **AccessPolicy** 与 **TaskPolicy**、**ExpensePolicy L80/L255/L256/L396/L399**、**LeavePolicy**、
    **Shift**、**DateChecker**、**DinersClubInternational/InterPayment/Visa checkLength**、
    **OvertimePolicy L40**、**AWE start/cancel/withdraw/delegate/nextApplicableStep** 等均已杀掉
    （见 CardProbeTest 每条 @DisplayName 里的 `类.方法 L行号 idx 算子` 标注）。

## D. 已在 AGENT-BRIEF 中登记的等价（17 条，本轮按要求未再投入）

- `VerveValidator.checkIINRanges` L53/54/60/61/62 + `checkLength` L34 —— 8 条（IIN 区间重复扫描）
- `MaestroValidator.checkLength` L33×2 + L37 —— 3 条（长度区间被 IIN 检查兜住/返回值冗余）
- `MaestroUKValidator.checkLength` L34×2 + L38 —— 3 条
- `LuhnValidator.algorithmCheck` L55/L62/L76 —— 3 条（重复扫描与取负模 10 等价）

主测试文件已给出证明，本轮沿用，未重复编写用例。

## 备注：本轮发现的 main 代码缺陷（未修，仅供记录）

`Shift` 记录类：紧凑构造器里 `if (type == ShiftType.NIGHT && !crossesMidnight())` 调用
`crossesMidnight()` 时 `this.end` 尚未赋值（紧凑构造器中字段在构造器体之后才写入），
任何 `ShiftType.NIGHT` 实例都会抛 NullPointerException。因此 `OvertimePolicy` 的
NIGHT 分支（factor 1.5）在实际运行中不可达。CardProbeTest 用"跨午夜的 STANDARD 班次"
覆盖 `crossesMidnight()`/`scheduledEnd()`。按约定未修改 `src/main/**`。
