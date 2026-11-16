# 防错方法论：本技能执行者已犯错误清单

适用：每次编写与验证测试前通读；把"未犯过的错"当作待检查项。每条含症状、根因、检查法与适用边界。来源：2026-10-28 至 2026-11-01 四轮实战（Research/Credit/corporate-card-expense-approval/online-math-expression-grading）中的真实错误。

## 1. 输入值域穷举不完整（本技能最大的两次错误）

**症状**：两次把 TypeChecker 被遮蔽的卡类型判为"数学不可达"，用户出示反例后均被推翻（第一次字母数位 10-35 突破 Maestro 六位区间；第二次 Unicode 数字字符 U+1375=40 突破字母上限）。

**根因**：证明"不可达"时对 `Character.getNumericValue` 只枚举了 0-9（或 0-35），且把遮蔽者区间的位数搞错（把 7 位数 6,210,000 误判为落在六位区间 [600000,699999] 内）。

**检查法**（写路径证明前逐项过）：
- getNumericValue 全域：'0'-'9'→0-9；'A'-'z'→10-35；全角数字；**Unicode 数字字符**（埃塞俄比亚 ፵=40、፶=50、፷=60、፸=70、፹=80、፺=90、፻=100 等）；无效字符→-1/-2。JavaDoc 无 35 上限。
- 每个"区间"写出十进制位数与边界，逐位核对（六位区间只能容纳六位解析；两位数值字符会把解析推到 7+ 位）。
- 前导零错位：目标校验器解析多位含前导零时，遮蔽者解析更少位可能被同一个零"错开"到区间外。
- 溢出边界：多字母使长前缀解析超过 10 位 → parseInt 抛 NumberFormatException → 正常路径提前终止，遮蔽者必须"解析成功且落空"。
- 长度边界：每个校验器的长度区间与目标的包含关系逐条核对。

**结论表述纪律**：攻击层穷尽前，只写"当前攻击层下未找到反例"，不写"数学不可达"。同一结论被推翻一次后，下一轮证明必须显式枚举被遗漏的值域维度。

## 2. 变异语义靠记忆而不靠反汇编

**症状**：把 EQUAL_ELSE 一律理解为"条件恒假"，多个可杀变异被误判为等价；手工 patch 字节码做实验时忘了 pop 操作数 → VerifyError；patch 偏移用错（分支操作数相对指令起始地址，不是结束地址）。

**根因**：PIT 的 EQUAL_IF/EQUAL_ELSE 对 `if_acmpeq`（相等跳转）与 `if_acmpne`（不等跳转）两个方向作用相反。

**检查法**：分析存活变异前，从本地 pitest jar 反汇编 mutator（javap），确认：EQUAL_IF=移除跳转（操作数 pop/pop2 后直落）、EQUAL_ELSE=替换为无条件 GOTO（先 pop/pop2）。手工模拟变异必须同步 pop 操作数并保持代码长度与栈帧一致。每条变异分析写出"被变异指令的 opcode → 变异后控制流"。

## 3. 下游顶包与兜底逻辑（断言看似覆盖，实则被替代）

**症状**：assertThrows 只查异常类型，被变异跳过的检查由下游构造器抛同类型异常顶包（withdraw/cancel 的过去时间检查被 WorkflowSnapshot "updatedAt must not be before createdAt" 顶包）；REJECT 短路被 canStillSucceed 兜底出相同终态；start 空步检查与快照构造器抛相同消息。

**检查法**：对每个"检查被跳过"的变异，追踪跳过后的完整执行路径：下一个构造器/校验器会不会抛同类型异常？兜底逻辑会不会复算出相同结果？两条路径的消息是否相同？修复手段按序：断言异常消息原文 → 构造"兜底不生效"的场景（多审批人剩余票）→ 构造下游消息不同的输入（如空 workflowId 使下游先抛别项校验）。

## 4. 报告卫生与干净重跑

**症状**：删除测试后 jacoco 分支数不降反升（1177），干净重跑实为 1175；mutations.xml 解析到一半报 unclosed token（并行运行正在写）；把"总数−SURVIVED"当杀死数（漏掉 NO_COVERAGE，把 769 报成 779）；PIT 直接 goal 调用不触发编译（改完测试跑 PIT 用的是旧 test-classes）。

**检查法**：
- KILLED/SURVIVED/NO_COVERAGE/TIMED_OUT 各状态分别计数，禁止减法推导。
- 任何"惊喜提升"先怀疑 jacoco.exec 脏数据：删除 exec 后干净重跑，数字必须与当轮测试哈希对应。
- 解析 XML 失败先查是否有运行中的构建；等构建退出后再解析。
- `mvn org.pitest:pitest-maven:mutationCoverage` 前必须先 `mvn test`（或 test-compile）。
- 副本实验改 pom 时，验证替换真的落在目标字段（sed 的地址语义、多次出现的参数都要核对）。

## 5. 白盒手段分层（穷尽一层再进下一层）

真实输入（含字母/Unicode 数位、前导零、null/blank、超长/超短）→ Mockito mock record 绕过构造器不变量 → mock 枚举常量构造不存在状态 → 反射触达包私有类（构造器与方法都要 setAccessible）→ mockConstruction 让遮蔽者拒绝。每层收益分别标注"真实输入可达"与"环境可达"。禁止层（伪造证据，官方会取消成绩）：运行时替换被测类字节码、探测 PIT mutant 身份后人为失败、改 POM 缩分母/排除算子、篡改报告。已证等价/不可达的点换攻击层再试一次，而不是直接归档。

## 6. 并发会话冲突与基线纪律

**症状**：两个会话并行编辑同一测试文件与 target，行号、测试数、幸存清单在分析过程中漂移（文件从 2842 行涨到 3509 行）；旧报告被半写覆盖。

**检查法**：开始分析时记录测试文件 md5/行数；每次 Edit 前重查 mtime；报告数字一律用自己发起的干净重跑；对方运行结束后用自己的运行覆盖。共享文件合并时保留两个工作区的分区标记，避免互相覆盖对方新增测试。

## 7. 全覆盖扫描：零覆盖方法与构造器

**症状**：CreditCardParser 的公开默认构造器从未被实例化（3 条指令 0 覆盖），长期漏在行覆盖缺口里；等价清单也因此漏项。

**检查法**：每轮跑完全量扫描——所有类中 INSTRUCTION covered=0 的方法、所有未调用构造器、所有非公开类（反射候选）、所有 mi>0 的行。enum <clinit> 随类加载执行，不是缺口。

## 8. 平台口径未知时的探针

本地 pom 的 DEFAULTS 之外，平台可能用更强算子。副本 pom 把 mutators 换成 STRONGER（或 ALL）复测，暴露 EQUAL_IF 类新存活；对可杀的补测试（只增不减），回头重跑 DEFAULTS 确认不回退。平台分数页显示的口径（分母是否含 NO_COVERAGE、是否算 Test Strength）以当场页面为准，不臆测。

## 9. 把"公开路径上冗余/归一化"误判为等价（上游遮蔽）

**症状**（2026-11-01，online-math-expression-grading 实测）：把 ArithmeticExpression.atom 的 skipSpaces 删除判为等价——理由是在公开 evaluate() 路径上 unary 入口已先消费空白，调用冗余。参考方案用反射直调 `atom("  12.50")` 一击杀死。同类误判还有：scoreEssay 的 `rubricPercent==0` 提前返回被 money() 的 setScale(2) 归一化后"值相等"；scoreAnswer 三条边界变异被 money() 抹平 scale 后判等价。

**根因**：等价性证明只沿公开入口的路径条件推导，没有穷举"每个私有方法自身的所有入口状态"；对"值相等"的断言（compareTo/assertEquals）无法区分对象分配差异。

**检查法**（判等价前逐项过）：
- 对每个私有方法：它的每个入口（公开链、其他私有方法直调、反射直调）分别做可达性攻击；"上游已处理"只对该上游成立。
- 变异后值相同但**对象不同**（新分配 vs 复用静态常量）时，用 assertSame/assertNotSame 建立身份断言；money()/setScale(2) 对已是 scale-2 的对象返回 this 本身，是身份可观察的关键。
- 归一化（setScale/formatStr）发生在哪一层要画出来：归一化之前的私有返回值的 scale/toString 仍是可观察面。
- 官方参考的等价清单与自己清单逐项对账：对方杀掉的而自己标等价的，优先假设自己漏了攻击面。

## 10. record 构造态的非法值域未穷举（validate 只在公开路径生效）

**症状**：把 ExamGradingService L154 的 clamp true 边证明为"数学不可达"——理由是所有题型 score≤maximum。实际上 `AnswerResult(passedTests=4, totalTests=2)` 是 record 完全允许的构造态，只有 grade() 的 validate 拒绝它；反射直调 scoreAnswer 时 passRate=2.0，score=2×maximum，clamp 触发，还连带杀死 L154 的 ORDER_ELSE。同类：rubricPercent=125（essay 超额分）、completionPercent=Integer.MAX_VALUE（ARTICLE 三元 else 分支 10×MAX int 溢出为 -10→0，与 true 分支的 weight 不同）、下降区间 [1e300,-1e300]（formatStr 的 StringBuilder 容量 `(int)(len×|end−start|)+1` 溢出为 MIN_VALUE 抛 NegativeArraySizeException——"容量不可观察"被异常推翻）。

**根因**："不可达"证明只枚举了经过 validate 的合法状态空间；私有方法没有 validate 前置，record 全参数空间都是可达输入。

**检查法**：
- 每个"score≤max""count 非负"类证明前，列出 record 每个 int/枚举字段的全值域（负值、0、互斥关系违反如 passed>total、selectedCorrect>correctOptionCount、rubric>100、Integer.MAX_VALUE），逐一在私有入口测试。
- 容量/长度类"不可观察"结论，先试 int/long 溢出的中间值：`(int)` 饱和后 +1 回绕为 MIN_VALUE、length×|end−start| 超过 2^31 时 new StringBuilder(负) 抛异常——异常本身是可观察差异。
- 变异体在畸形输入上可能死循环：用线程超时包装（见第 11 条）转成确定性失败，而不是靠 PIT 自身的 TIMED_OUT。

## 11. 线程超时包装的两个 PIT 机制坑（timeoutConstant 与覆盖归属）

**症状**：用 daemon 线程 + task.get(5s) 包装死循环变异，结果 PIT 仍报 TIMED_OUT 而非 KILLED；且 `start += step` 循环自增指令的变异 testsRun=0（PIT 认为没有任何测试覆盖该指令）。后来把等待降到 1 秒（< PIT 默认 3s timeoutConstant）后，等式条件的变异转为 KILLED，但自增指令仍 TIMED_OUT。

**根因**：PIT 的 mutation 超时（默认 timeoutConstant=3000ms×factor）先于测试自身 5 秒超时触发；PIT 的覆盖归属只认**测试主线程**执行的指令，daemon 线程上执行的代码不归属任何测试（testsRun=0 → 不跑测试 → 该变异永远 TIMED_OUT/无归属）。

**检查法**：
- 线程包装的等待上限必须小于 PIT timeoutConstant（默认 3000ms），建议 1 秒；原实现即时返回，1 秒余量充足。
- 每个循环行（条件行与自增行）必须至少有一个**主线程**测试执行过，否则自增指令变异无归属、不可杀；归属成立后，超时包装测试在 1 秒处抛 AssertionError 即 KILLED。
- 口径核对：官方缺口说明文档明示"分母含全部变异，分子只计 KILLED，TIMED_OUT 均为 0"——TIMED_OUT 在严格口径下不算杀死，能转 KILLED 的必须转。
- 无归属（testsRun=0）的变异是 PIT 工具行为，如实记录，不靠改报告或改 POM 凑数。

## 12. "插桩残留"标签的授予门槛（先重构测试表述再归档）

**症状**：ArithmeticExpression L87/L59、ExpressionExamProgressService L25 被标为"PIT 插桩残留"（同型相邻变异已被杀、击杀测试已运行且通过）。参考方案全部杀死：L87/L59 用反射直调 atom/unary 的表述，L25 用 null/blank 循环 + 异常消息断言。

**根因**：把"我的测试表述没杀死"直接归因于工具，没有尝试同一行为的其他表述（不同入口、消息断言、身份断言、静态桩）。

**检查法**：授予"残留"标签前，至少换三种表述：①直调私有入口（绕开公开路径的遮蔽）；②断言异常消息原文/对象身份；③MockedStatic 桩掉抛异常的依赖。三种都失败且同型相邻变异已被杀，才写"疑似工具残留"，并保留"未解决"而非"已证明等价"。

## 13. 枚举 switch default 的字节码假设（先 javap 再设计）

**症状**：假设 JDK 21 枚举 switch 走 typeSwitch invokedynamic（null → default 分支），用 Mockito 顺序桩（type() 先 VIDEO 后 null）试图触达 assessLesson L137 的 default——实测抛 NPE：`Cannot invoke LessonType.ordinal()`，本编译产物是 ordinal() 旧式翻译，default 只被伪造 ordinal 触达。

**根因**：对编译器的 switch 翻译策略凭版本记忆推断，未验证实际字节码。

**检查法**：涉及枚举 switch 的 default 分支触达实验前，先 `javap -c` 确认是 typeSwitch（null→default）还是 ordinal()/SwitchMap（null→NPE）；伪造 ordinal（Unsafe.allocateInstance+putInt）属禁止层——制造类型系统中不存在的对象，是伪造执行痕迹。default 不可达就写路径证明并保留分母。

