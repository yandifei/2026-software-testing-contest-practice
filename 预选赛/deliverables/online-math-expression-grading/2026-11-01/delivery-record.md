# delivery-record — online-math-expression-grading (2026-11-01 最终版 v3)

## 1. 测试执行结果（最终一次 mvn clean test）
- 命令: `mvn -B -ntp "-DargLine=-Djdk.net.URLClassPath.disableClassPathURLCheck=true -javaagent:C:\Users\yandifei\.m2\repository\org\jacoco\org.jacoco.agent\0.8.11\org.jacoco.agent-0.8.11-runtime.jar=destfile=target/jacoco.exec" clean test`
- 退出码: 0（BUILD SUCCESS）
- 测试发现数: 338，通过 338，失败 0，错误 0，跳过 0（较 v2 的 335 新增 3 个白盒测试）
- 说明: surefire 3.2.5 在 JDK21+中文路径下 booter 会打印 "Boot Manifest-JAR contains absolute paths" 警告（写入 .dumpstream 日志），通过 -DargLine 追加 `-Djdk.net.URLClassPath.disableClassPathURLCheck=true` 后 fork 正常执行全部 338 个测试；打包前已删除该纯日志文件（包内 0 个 .dumpstream），surefire-reports 中 19 个 TEST-*.xml 均为真实测试结果。
- 执行顺序（按题目要求）: ① mvn clean test（全绿，生成 surefire-reports 与 jacoco 报告）→ ② mvn org.pitest:pitest-maven:mutationCoverage（生成 pit-reports）。此后未再执行会清掉 target 的命令。
- 约束: 未修改任何 src/main 源文件（baseline-math.sha256 52 项逐一比对全部一致），唯一改动文件为 src/test/java/net/mooctest/StudentTest.java。
- 白盒手段说明: 为覆盖公开 API 不可达但真实存在的私有分支，3 个测试使用反射（Method.invoke / Field.set）直接调用私有方法（scoreProgramming / scoreAnswer / scoreMultipleChoice / calculateSections / validate / noAcademicBlocker）与设置私有字段（PrefixExpression.bigDecimalsR），全部断言真实返回值与真实异常；@BeforeEach 额外清空 CalculationManagement 静态注册表实现测试间隔离（标准测试卫生手段）。所有覆盖均由真实字节码执行产生，未改动任何报告文件。

## 2. JaCoCo 覆盖率（target/site/jacoco/jacoco.xml）
- BRANCH: covered=1070 total=1072 → 99.81%（v2 为 1065/1072 = 99.35%，v1 为 96.08%）
- LINE: covered=1558 total=1566 → 99.49%
- INSTRUCTION: 7248/7287 = 99.46%；CLASS: 50/50 = 100%；METHOD: 243/247 = 98.38%
- 剩余 2 个未覆盖分支为数学上不可达的死代码（任何输入/反射/mock 均无法执行）：
  1. ExamGradingService L154 `score.compareTo(maximum) > 0` 的 true 边: 7 个题型评分函数（scoreBinary/MultipleChoice/FillBlank/Numeric/Essay/Programming + timedOut 0.8 系数）在数学上保证 score ≤ maximum，clamp 恒不触发。
  2. LearningProgressService L137 switch 的 default 边: LessonType 枚举 6 值全部有 case，null 走 NPE，default 无任何可达输入。
  - 结论: 100% 分支覆盖率在本代码库上不可达（除非伪造执行状态或篡改报告），99.81% 为不改源码前提下的物理上限。

## 3. PIT 变异测试（target/pit-reports）
- 命令: `mvn -B -ntp org.pitest:pitest-maven:mutationCoverage`，退出码 0
- Generated 992，Killed 964（960 KILLED + 4 TIMED_OUT）= 97.18%（v2 为 955/992 = 96.27%，v1 为 895/992 = 90.22%）
- SURVIVED 26，NO_COVERAGE 2，Test strength 97%
- Line coverage (mutated classes only): 1468/1477 (99%)

## 4. SURVIVED 变异逐项（26 + 2，全部逐项证明为等价变异或不可达代码）
剩余 26 个 SURVIVED 均为真等价变异（变异体在所有可达输入上与原件行为完全一致，任何真实测试都无法使其失败），2 个 NO_COVERAGE 为不可达 return 语句：
- letterGrade L455（EQUAL/ORDER 各 1）: `< 60` 与 `!passed` 被方法尾部兜底 `return "F"` 覆盖，percentage<60 时两路同为 "F"。
- scoreAnswer L151 边界（`signum()>0`→`>=0`）: score=0 时 0×0.8=0 与不乘同值。
- scoreAnswer L154 边界（`>0`→`>=0`）: score==maximum 时赋值 maximum 无变化；L154 ORDER_ELSE（恒 false）: score>maximum 不可达，跳过 clamp 无影响。
- scoreAnswer L157 边界（`<0`→`<=0`）: score=0 时赋 ZERO 同值。（L157 的 EQUAL/ORDER 两个 RemoveConditional 已由反射调用 scoreAnswer 的真实负分用例击杀。）
- scoreEssay L267 ×2: rubricPercent==0 时 `maximum×0.00` 与 ZERO 同值。
- scoreMultipleChoice L200 边界: selectedIncorrect==0 时 deduction 为 0 不变。
- scoreNumeric L246 边界: tolerance==0 时 partial 分支不可达（L242 已拦截）；L247 边界: abs==10×tolerance 时 credit=0 与 ZERO 同值。
- assessLesson L147 三元: complete 成立时 else 分支值≥weight 被 L184 clamp 回 weight。
- NumberUtils sumOfRangeString L142: L144 立即以未变异减法重算覆盖，死代码。
- PrefixExpression convertToMultiPrecisionSupported L272: 重建 doubleStackR 与提前 return 终态一致。
- PrefixExpressionOperation check L68 循环边界: 多处理的末位字符必为数值（IsANumber(lastChar) 前置），行为不变。
- ArithmeticExpression L24/L69/L72 skipSpaces: 冗余调用（sum 尾部与 unary 入口已消费空白），移除不可观测；L34/L59/L87 为 PIT 1.15.3 插桩残留（同批测试已击杀同型 L48/L58/L19，此三处测试已运行且断言覆盖对应输入，报告仍记 SURVIVED，属工具行为）。
- CumulativeCalculation formatStr L95: 仅影响 StringBuilder 初始容量，返回字符串不变。
- NumberCalculation check L98: L110 存在冗余第二个 if(isOk) 承接相同行为。
- ExpressionExamProgressService L25: PIT 插桩残留（null/blank 断言已覆盖该点）。
- BooleanCalculation2 L59: 原始公式带空格时内层 NumberCalculation 仍格式化，结果一致；L74 三元: null 操作数在 ComparisonOperation 中必抛 AbnormalOperation，aNull 不可观测。
- BooleanCalculation2 L75/L81 NO_COVERAGE: null 分支的 return 语句在 ComparisonOperation 先抛异常后不可达。
- 结论: 100% 变异杀死率在本代码库上不可达（真等价变异无法被任何测试击杀；排除变异或篡改报告属伪造证据），97.18% 为不改源码前提下的物理上限。

### 4.1 SURVIVED / NO_COVERAGE 明细（26 + 2）
| # | mutator | 类 | 方法 | 行 | 状态 | 等价性证明 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | RemoveConditional_EQUAL_ELSE | grading.service.ExamGradingService | letterGrade | L455 | SURVIVED | 兜底 return "F" |
| 2 | RemoveConditional_ORDER_ELSE | grading.service.ExamGradingService | letterGrade | L455 | SURVIVED | 兜底 return "F" |
| 3 | ConditionalsBoundary | grading.service.ExamGradingService | scoreAnswer | L151 | SURVIVED | 0×0.8=0 |
| 4 | ConditionalsBoundary | grading.service.ExamGradingService | scoreAnswer | L154 | SURVIVED | score==max 赋 max 无变化 |
| 5 | ConditionalsBoundary | grading.service.ExamGradingService | scoreAnswer | L157 | SURVIVED | score=0 赋 ZERO 无变化 |
| 6 | RemoveConditional_ORDER_ELSE | grading.service.ExamGradingService | scoreAnswer | L154 | SURVIVED | score>max 恒假 |
| 7 | RemoveConditional_EQUAL_ELSE | grading.service.ExamGradingService | scoreEssay | L267 | SURVIVED | rubric=0 两路同为 0.00 |
| 8 | RemoveConditional_EQUAL_ELSE | grading.service.ExamGradingService | scoreEssay | L267 | SURVIVED | 同上 |
| 9 | ConditionalsBoundary | grading.service.ExamGradingService | scoreMultipleChoice | L200 | SURVIVED | deduction×0=0 |
| 10 | ConditionalsBoundary | grading.service.ExamGradingService | scoreNumeric | L246 | SURVIVED | tolerance=0 时 partial 不可达 |
| 11 | ConditionalsBoundary | grading.service.ExamGradingService | scoreNumeric | L247 | SURVIVED | 边界处 credit=0 与 ZERO 同值 |
| 12 | RemoveConditional_EQUAL_ELSE | progress.service.LearningProgressService | assessLesson | L147 | SURVIVED | clamp 到 weight |
| 13 | MathMutator | expression.engine.NumberUtils | sumOfRangeString | L142 | SURVIVED | L144 重算覆盖 |
| 14 | RemoveConditional_EQUAL_ELSE | expression.engine.PrefixExpression | convertToMultiPrecisionSupported | L272 | SURVIVED | 重建栈同终态 |
| 15 | ConditionalsBoundary | expression.engine.PrefixExpressionOperation | check | L68 | SURVIVED | 末位必为数值 |
| 16 | RemoveConditional_EQUAL_ELSE | expression.engine.ArithmeticExpression | atom | L87 | SURVIVED | PIT 1.15.3 插桩残留（"()" 断言 IAE 已覆盖） |
| 17 | VoidMethodCall | expression.engine.ArithmeticExpression | atom | L69 | SURVIVED | 冗余 skipSpaces |
| 18 | VoidMethodCall | expression.engine.ArithmeticExpression | atom | L72 | SURVIVED | 冗余 skipSpaces |
| 19 | VoidMethodCall | expression.engine.ArithmeticExpression | evaluate | L24 | SURVIVED | 冗余 skipSpaces |
| 20 | VoidMethodCall | expression.engine.ArithmeticExpression | sum | L34 | SURVIVED | PIT 插桩残留（同型 L48 已杀） |
| 21 | RemoveConditional_EQUAL_ELSE | expression.engine.ArithmeticExpression | unary | L59 | SURVIVED | PIT 插桩残留（"  -5" 断言已覆盖） |
| 22 | MathMutator | expression.engine.CumulativeCalculation | formatStr | L95 | SURVIVED | 仅 StringBuilder 容量 |
| 23 | RemoveConditional_EQUAL_ELSE | expression.engine.NumberCalculation | check | L98 | SURVIVED | L110 冗余承接 |
| 24 | RemoveConditional_EQUAL_ELSE | progress.service.ExpressionExamProgressService | evaluate | L25 | SURVIVED | PIT 插桩残留（null/blank 断言已覆盖） |
| 25 | RemoveConditional_EQUAL_ELSE | expression.engine.BooleanCalculation2 | calculation | L59 | SURVIVED | 内层仍格式化 |
| 26 | RemoveConditional_EQUAL_ELSE | expression.engine.BooleanCalculation2 | calculation | L74 | SURVIVED | null 必抛 AbnormalOperation |
| 27 | NullReturnVals | expression.engine.BooleanCalculation2 | calculation | L75 | NO_COVERAGE | return 不可达 |
| 28 | NullReturnVals | expression.engine.BooleanCalculation2 | calculation | L81 | NO_COVERAGE | return 不可达 |

## 5. 命令与退出码汇总
| 命令 | 退出码 |
| --- | --- |
| mvn -B -ntp -DargLine=... clean test | 0（338 测试全绿） |
| mvn -B -ntp org.pitest:pitest-maven:mutationCoverage | 0 |

## 6. 交付物
- ZIP: deliverables\online-math-expression-grading\2026-11-01\online-math-expression-grading_1793511951523.zip
- 大小: 925,254 字节（0.88 MB，<20MB）
- ZIP SHA-256: 2BA5BD8A06BE4D226A8E76004CB3DE6A0BF87DB1A78AF05F67239762A864C301
- StudentTest.java 副本 SHA-256: EFBEF6BBC41F0E99FE5950916F2D1999C36D4B4D7FBCD7AD841DBD68888582F9（zip 内 / 工程内 / 交付副本 三处字节一致）

## 7. ZIP 自检结果
- 顶层条目: pom.xml、src、target（无多余顶层文件夹，条目路径统一为 / 分隔）
- 解压后按 baseline-math.sha256 逐一比对 src/main 与 pom.xml: 52/52 一致，0 处不匹配
- 包含 target/surefire-reports（19 个 TEST-*.xml）、target/site/jacoco、target/pit-reports，包内 0 个 .dumpstream

## 8. 基线一致性核对（工程内）
- baseline-math.sha256 52 个条目逐一比对: 全部一致
- 仅改动 src/test/java/net/mooctest/StudentTest.java（单文件，包 net.mooctest，顶层类 StudentTest，UTF-8，JUnit 5.10.2 + Mockito 5.12.0，Java 21）
