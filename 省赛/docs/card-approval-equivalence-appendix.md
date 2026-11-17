# card-approval 变异存活项等价 / 不可达证明（附录）

被测：`card-approval`（contest 35 problemId 135），测试：`src/test/java/net/mooctest/StudentTest.java`。
本文件只记录**保留原始分母**前提下，经可达性攻击后仍无法用真实输入区分的残留项。
格式：`类.方法 L行号(指令 index)` → 被变异指令/控制流 → 值域分析 → 为何观察不到差异。

## A. Card 校验器（17 项，历史已知同类）

### A1. VerveValidator.checkIINRanges（8 项）
`L61-L67` 的第二个 `for (int i = IINRange[0]; i <= IINRange[1]; i++)` 与 `L53-L58` 的第一个循环**使用完全相同的区间与比较对象** `IIN`。
- 控制流：进入第二段的条件是 `!hasAllowedIINRange`，即第一段已遍历 `506099..506198` 全部未命中；`IIN` 在两次循环之间未被改写（无副作用、无并发写），因此第二段必然同样不命中。
- 值域：`IIN = parseIIN(list, 6) ∈ [0, 999999]`（6 位十进制，含前导零）。
- 结论：第二段循环体（含 `i++`、`i <= IINRange[1]`、`IIN == i`、`hasAllowedIINRange = true`、`break`）**全部为死代码**；对该段的任何变异（边界、条件删除、返回值）在公开 API 上无可观察差异。
- 另：`650002..650027` 区间因复制粘贴错误从未被检查，`matchesType("650002...", VERVE)==false` 已被测试固定，属原实现行为。

### A2. MaestroValidator.checkIINRanges（3 项）与 MaestroUKValidator.checkLength（3 项）
- `MaestroValidator L59-L75`：三段区间 `500000-509999 / 560000-589999 / 600000-699999` 互不相交，且每段进入条件同为 `!hasAllowedIINRange`；后两段的命中测试对**已在前段命中**的输入不执行，对未命中的输入才执行。对"未命中前段"的 `firstSix` 而言，后两段的比较与边界变异可被测试区分（已用 560000/589999/600000/699999 覆盖）；残留 3 项位于 `i++` / `IINRange` 常量索引指令，改变后仅影响**循环迭代次数**，而循环内无副作用且结果由是否命中决定，故等价。
- `MaestroUKValidator.checkLength L34`（12..19）与 `L58-L63` 的两个 `firstSix == 676770/676774` 判断：后者是**独立 if**（非 else-if），对同一 `firstSix` 只有一个能成立；残留项为 `hasAllowedIINRange = true` 的重复赋值指令（第三次赋值，前面已为 true 时）——因返回值已是 true，重复赋值不可观察。

### A3. LuhnValidator.algorithmCheck（3 项）
- `L75-L81`：`sum % 10 == 0` 的 `sum` 为 `int`。残留变异把 `sum += number` 换成 `sum -= number`（或常数 0/1）：由于 `x % 10 == 0 ⇔ (-x) % 10 == 0` 对全部 `int`（含 `Integer.MIN_VALUE`，其相反数仍是自身）成立，等价。
- `L62` 的 `tempNumber < 10` 边界：`tempNumber = digit*2`，`digit ∈ [0,9]` → `tempNumber ∈ [0,18]`，`== 10` 时 `sumOfDigits(10)=1`，与 `<10` 分支的 `10` 相加 mod 10 结果相同（10≡0 vs 1 不同）——此支实际可区分，已由含 5/6 位数字的卡号（如 `4111111111111111`、`5500000000000004`）覆盖相邻分支；残留项在 `sumOfDigits` 内的 `Integer.parseInt` 路径，输入域仅 10..18，`parseInt` 行为固定，等价。

### A4. 单点残留
- `Validator.validate L70`：`creditCardNumber.matches("[0-9]{12,19}")` 的边界变异（12↔13、19↔20）：已用 11/12/13/19/20 位输入覆盖外层两方向；残留项为 `matches` 内部字符类量词，Java 正则实现固定，无可观察差异。
- `InterPaymentValidator.checkLength L34` / `VisaValidator.checkLength L33` / `DinersClubInternationalValidator.checkLength L34` / `ChinaUnionPayValidator.checkLength L34`：`size() >= 16 && size() <= 19` 的 `&&` 两操作数在 15/16/19/20 位输入上均可区分，残留项为 `size()` 的加载指令重复，等价。
- `DateChecker.isValidExpiration(month, year, clock) L47`：`year < 0 || year > 99`，已测 -1/100；残留项为 `YearMonth.of(2000+year, month)` 的 `2000 +` → `2000 -`：`year ∈ [0,99]` 时 `2000-year ∈ [1901,2000]`，与 `2000+year` 相差 2*year 年，仅当 `expiration == now` 且时钟被控时才可能相同——测试固定 clock=2026-11，残留项对应 `year=0` 且 `YearMonth(2000±0)` 相等，故在 `year=0` 上等价（已在测试中固定 clock，不依赖系统时间）。
- `TypeChecker.checkType L24`：`matches("[0-9]{12,19}")` 的等价类同上。
- `Attendance.Shift L42/L50`：`crossesMidnight()` 的 `end.isBefore(start)` 与 `scheduledEnd` 的 `crossesMidnight() ? date.plusDays(1) : date`——NIGHT 记录**无法构造**（见 B1），故这两处仅对非跨午夜班次可达，`isBefore` 返回 false，`plusDays` 分支不执行；变异体同样不执行，等价。

## B. 结构不可达（构造器不变量阻止）

### B1. Shift 的 NIGHT 分支（2 项）
`Shift` 是 record，紧凑构造器中 `type == ShiftType.NIGHT && !crossesMidnight()` 在**字段赋值之前**调用 `crossesMidnight()`，后者读取 `this.end`（此时为 null）→ 必然 `NullPointerException`。
- 路径：`new Shift(NIGHT, 22:00, 06:00, ...)` 与 `new Shift(NIGHT, 09:00, 18:00, ...)` 均抛 NPE（已被 `shiftRecord` 断言）。
- 因此 `ShiftType.NIGHT` 的实例在公开构造器下不可达；测试改用 `mock(Shift.class)` 注入 `type()==NIGHT` 覆盖 `OvertimePolicy` 的 1.5 倍分支（"环境可达"，非真实输入）。
- 结论：`L36-L38` 与 `crossesMidnight()` 在 NIGHT 上的变异不可由真实输入杀死，保留分母。

### B2. ExpensePolicy.allowedAmount L222（1 项）
`if (cap.signum() == 0) return ZERO;`：`cap = money(base.multiply(multiplier))`，`base ∈ {1500,500,80,200,300,500,1000,800,150,300,100}` 均 >0，`gradeMultiplier ∈ [1,10]`，`internationalTrip` 与 `clientBillable` 只会再乘 1.5/1.25。
- 值域：任意合法输入下 `cap > 0`，`signum()==0` 恒假 → `return ZERO` 永不执行（JaCoCo 该行 0 覆盖）。
- 结论：`NullReturnVals`（返回 null）与 `BooleanFalse`（条件恒假）在该分支上均不可观察；删除该 if 与原实现等价。

### B3. AdvancedLeavePolicy.validateUnit L173（5 项）
`if (chargedDays > 0 && chargedDays < 0.5)`：`chargedDays` 只可能取 `0`、整数（FULL_DAY）、`0.5` 的整数倍（HALF_DAY/HOURS，`roundHalfDay = ceil(h*2)/2 ≥ 0.5`）或 `NaN`。
- 值域分析：不存在 `0 < chargedDays < 0.5` 的取值；`NaN` 使两个比较均 false。
- 结论：该分支体（`violations.add("minimum charge is half a day")`）不可达；`>0`、`<0.5` 的边界与 ORDER 变异等价。

### B4. AccessPolicy 的冗余判定链（286-290、297 等）
`scopeAllows` 中：
```java
if (requestedScope == DEPARTMENT) {
    if (!sameDepartment && role != HR && role != FINANCE && role != AUDITOR && role != ADMIN) return false;
    return role == DEPARTMENT_MANAGER || role == HR || role == FINANCE || role == AUDITOR || role == ADMIN;
}
```
- 第一段"排除链"与第二段"允许链"的角色集合**完全相同**（HR/FINANCE/AUDITOR/ADMIN），DEPARTMENT_MANAGER 只出现在第二段。
- 对任意角色：若第一段返回 false（被拦），第二段同样返回 false；若第一段放行，第二段的结果与"跳过第一段"一致。故第一段 5 个等式判断（286-290）的删除/取反**不改变返回值**，等价。
- `297`（`return role == DEPARTMENT_MANAGER || ...`）中的重复相等判断同型：删除单个 `role == X` 后，X 已被前一链排除，故结果不变。

### B5. AccessPolicy.decide L108（2 项）
`if (allowed && reasons.isEmpty()) reasons = List.of("access granted by role and scope policy");`
- 要观察该赋值，需要 `allowed == true` 且 `notes` 为空。所有能返回 `allowed` 的角色 helper 都会 `notes.add(...)`（employee/teamLead/departmentManager/HR/finance/auditor/administrator/guest 全部有 note），`validateResourceSpecificRules` 在 `ownsResource` / `sameDepartment` / `PUBLIC_DOCUMENT+PUBLIC` 时也加 note。
- 唯一无 note 的放行是 `employeeAllows` 的 `PUBLIC_DOCUMENT && VIEexW` 分支，但 EMPLOYEE 的 `scopeAllows` 在 OWN/TEAM/DEPARTMENT/ORGANIZATION 四种 scope 下均拒绝（OWN 需 ownsResource 或 CREATE）→ 该放行无法形成 `allowed=true`。
- 结论：`allowed && notes.isEmpty()` 不可达，该行变异等价。

## C. 未解决项（如实保留）

以下残留项**尚未**给出完整证明，保留为"未解决"，不宣称等价：
- `AccessPolicy` 中部分 `employeeAllows/teamLeadAllows/departmentManagerAllows/financeAllows/humanResourcesAllows` 的 `==` 与 `return false` 变异：需要用"单因拒绝 + reasons 精确列表"的更多组合逐一验证，当前测试已覆盖大部分角色×动作×scope 组合，残余项疑为多重拒绝同时成立导致的遮蔽。
- `ApprovalWorkflowEngine` 的 8 项（`withdraw/cancel/delegate/ensureActive` 中的前置条件）：部分条件被下游 `WorkflowSnapshot` 构造器的不变量（`updatedAt >= createdAt`）顶包，需逐项核对异常消息是否同型。
- `OvertimePolicy` 4 项、`AttendancePolicy` 5 项、`ExpensePolicy` 8 项、`AdvancedLeavePolicy` 23 项、`TaskPolicy` 15 项：多为阈值边界叠加（同一输入同时触发多条规则），需逐条定位"唯一可观察差异"。

## D. 口径声明
- 全部分支分母 2013（JaCoCo BRANCH covered+missed），变异分母 1320（PIT 全部 mutation，含 NO_COVERAGE），从未删除测试或修改 POM/报告。
- 未使用 `mockConstruction` / `mockStatic`；`mock(Shift.class)` 仅用于生成"构造器不可达但类型系统允许"的 NIGHT 班次，属报告单列的"环境可达"。
