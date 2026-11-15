# 多执行者并行与测试文件合并

> 2026-11-15 省赛实测：两题、四个执行者（2 个主执行者 + 2 个专项补测执行者），最终 Card 258 用例 / Math 588 用例，全部合进单个 `StudentTest.java`。本文记录可复用的分工、合并与验证纪律。

## 1. 何时值得并行

| 场景 | 是否并行 | 理由 |
| --- | --- | --- |
| 一题内单个"大空白包"（如 60 个类、511 个方法的表达式引擎） | 是 | 一个执行者写 500 个方法的覆盖要几十轮，拆两半能翻倍吞吐 |
| 同一题同一测试文件的存量补杀 | 否 | 同一文件双写必然互相覆盖 |
| 两题（各自独立工程） | 是 | 目录、报告、PIT 全独立，无冲突 |
| 只差 1-2 个变异点 | 否 | 协调成本 > 收益 |

## 2. 目录与所有权（硬规则）

```text
<workspace>/
├── 原始项目/<题>-<原包>            # 原始 ZIP + 解包件，只读，不在此开发
├── deepseek/<题>/                  # 主执行者工作区（唯一可写 StudentTest.java 的地方）
├── deepseek/<题>-probe/            # 专项补测副本（只新增自己的 *ProbeTest.java）
├── deepseek/<题>-probe2/           # 第二个专项副本
├── _baseline/                      # 基线 JSON、报告快照、测试文件备份、等价证明
├── _snapshot/                      # 测试文件定时快照（防外部覆盖）
└── _tools/                         # 脚本（见 scripts/）
```

- **每个副本独立 `target/`**：两个执行者绝不能在同一个工程目录里各跑一次 `mvn`（会互相写 `target/jacoco.exec`、`target/pit-reports/mutations.xml`）。
- **只有一个执行者能写 `StudentTest.java`**；专项执行者只写自己的 `XxxProbeTest.java`，由集成者合并。
- 副本用 `robocopy <src> <dst> /E /XD target .git .idea` 建，别带 `target/`（避免继承旧报告和锁）。

## 3. 合并模式：probe 文件 → `@Nested` 内部类

主文件保持一个顶层类 `StudentTest`，每个 probe 变成它的一个 `@Nested` 内部类：

```java
class StudentTest {
    ...主执行者的成员...
    @org.junit.jupiter.api.Nested
    class ExpressionEngineTests {   // ← EngineProbeTest 的类体原样搬进来
        ...probe 的 @Test 与私有辅助方法...
    }
}
```

为什么用 `@Nested` 而不是把方法平铺：

1. **命名空间隔离**：probe 的 `assertMoney`、`card()` 等辅助方法不会和主文件重名（平铺会编译失败）。
2. **生命周期隔离**：probe 内的 `@BeforeEach/@AfterEach` 只作用于它自己的用例，不会污染主文件用例；`@TempDir` 也照常工作。
3. 合并只是文本插入，不改任何断言语义。

要求（下达给 probe 执行者时写死）：
- 只写自包含 `@Test/@ParameterizedTest` + 私有辅助方法；不加 `@BeforeAll/@AfterAll`；不依赖外部文件；
- 辅助方法用可识别前缀（如 `eng*`、`ep*`、`probe3*`），便于冲突排查；
- 若类名不能匹配 surefire 默认 `*Test`（如 `CardProbeTest3`），可临时加 `-Dtest=`，**不要**靠末尾加别名类来让 surefire 选中（见 §5）。

## 4. 合并脚本的两个真实坑

`scripts/merge_engine.py` 是本次沉淀的正确实现，它修掉了两个把文件搞坏的问题：

**坑 1：花括号计数必须跳过字符串与注释。**
第一版用 `src.count('{')` 逐字符配平，遇到 `assertEquals("{", ...)`、`"}"` 字符字面量、注释里的 `{` 就错位，产出**不平衡的文件**，编译报 `需要 class、interface、enum 或 record`（定位在文件末尾多出的 `}`）。正确做法：状态机区分 `code / line_comment / block_comment / string / char / text_block`。

**坑 2：只取文件里第一个顶层类，忽略其后的别名类。**
probe 文件末尾常见 `class CardProbeTest3Test extends CardProbeTest3 {}`（为让 surefire 默认 include 选中）。合并时**只搬第一个顶层类的类体**，别名类不要搬，否则 JUnit 会重复执行一遍（实测会出现"用例数正好翻倍"的假象）。

**幂等重建**优于增量合并：始终用「基线备份（主文件冻结版）+ 各 probe 文件」重新生成合并结果，不要在上一次合并结果上继续插。已验证的快照命名：

```text
_baseline/<题>-StudentTest-premerge-<hhmm>.java.bak      # 合并前的主文件
_baseline/<题>-StudentTest-<hhmm>.java.bak               # 每次定稿
```

## 5. surefire / PIT 的命名与统计陷阱

| 现象 | 原因 | 处置 |
| --- | --- | --- |
| `mvn test` 只跑了主文件，probe 用例不执行 | surefire 默认 include `**/*Test.class`，`XxxProbeTest2`、`CardProbeTest3` 不匹配 | 开发期用 `-Dtest=XxxProbeTest2`；合并成 `@Nested` 后自动被主类带走 |
| surefire 报告里所有用例都挂在 `StudentTest$XxxProbeTests` 下，外层显示 `Tests run: 0` | JUnit 5 嵌套类的报告归因 | 只核对**总执行数**（应等于主文件用例数 + 各 probe 用例数），不要按文件归因判断 |
| PIT 用 `-DtargetTests=net.mooctest.XxxProbeTest` 能跑，但全量 PIT 不含 probe | PIT 的 `targetTests` 在 POM 里是 `net.mooctest.*`，合并前单独文件属于同一包，全量会带上；合并后自然包含 | 合并后必须重跑一次全量 PIT 取快照 |
| 合并后用例数没涨 | probe 文件被别名类重复定义 / 合并时漏了类体 | 比对 `@Test` 计数：`主 + Σprobe` |

## 6. 独立复核（不采信执行者自报数字）

集成者必须自己从**原始报告**算一遍，再与执行者汇报对账：

```powershell
python scripts/pit_robust.py <mutations.xml> summary     # 逐状态计数 + KILLED 率（容错拼接/半写 XML）
python scripts/jacoco_report.py <jacoco.xml> 0           # INSTRUCTION/LINE/BRANCH/METHOD/CLASS
python scripts/branch_gaps.py <jacoco.xml> 20 1          # 按类+方法列分支缺口（打分支阶段用）
```

本次四轮对账全部吻合（例：执行者报 KILLED 1216/1320，脚本复核 1216/1320；Card 合并补杀后报 1285，复核 1285）。**凡是"惊喜提升"先怀疑报告脏数据**，删除 `target/jacoco.exec` 或重跑后再确认。

**执行者的文件可能在交付报告之后继续修订**：某执行者交付"169 用例全绿"后，仍在继续修正自己的手算错误（LOW→HIGH 等期望值改动），集成者手中的合并快照是旧版——合并套件挂了 2 个用例，用最新文件重新替换合并才修复。规则：合并一律以**执行者最新文件**为准（看 mtime + 让执行者确认"这是最终版"），合并后重跑 `mvn -B test`，把合并失败清单回传给该执行者定位。

## 7. 时间盒与收尾

- 每个执行者给**硬截止**（如"16:45 到点停手汇报"），到点必须交当前结果，不允许"再跑一次 PIT"。
- 按预期收益排序合并：先并"能明确多杀 10+ 个"的 probe，收益 <5 个的宁可不并（每次合并都要重跑 test + PIT ≈ 5–8 分钟）。
- 平台"最后一次提交为准" → **先用当前最好版本提交一次保底**，再继续并新 probe；只有新版本四项指标都不回退才重交。

## 8. 跨会话变异级对账：把"参考同行"变成外科手术（2026-11-15 省赛实测）

多个 AI 工具会话并行做同一题时，各自的全量 PIT 报告（`mutations.xml`）可以按 **(mutatedClass, mutatedMethod, lineNumber, description) 四元组**做 join，得到精确的互补清单：

- **量化差距**：2026-11-15 实测四个会话的 Math 杀死率 95% / 97% / 96% / 93%（glm 线最低），逐包差距集中在 `expression.engine` +92、`refund.service` +13、`scheduling.service` +3、`course.service` +1——上限 ≈ `Σ max(各会话, 自己) − 自己`，即"再提多少分"有了一个可计算的估计，不再是拍脑袋。
- **定位到类**：幸存者按类聚簇（FunctionFormulaCalculation2 差 16、EquationSolver 差 15、RefundPolicyService 差 12、ComplexExpression 差 10……），直接到**最强会话**的测试文件里找对应类的用例移植，而不是泛泛"参考对方代码"。
- **识别方向性错误**：自己判"等价/不可达"、他人却杀死的变异，优先假设自己漏了攻击面（与防错方法论 §9 呼应）；多个会话都杀、只剩自己活的变异是最优先的移植对象。
- **操作要点**：join 用十几行 Python 即可（读四处 XML → 四元组字典 → 按状态对比 → 按类分组导出）；不同会话的变异总数可能差十几条（执行环境/测试类集差异），按四元组匹配而不是按行号顺序对位；报告要在分析中途被覆盖时快照先行（§6 的快照纪律同样适用）。
- **边界**：对账指出的是"去哪找"，移植时仍要逐条验证（跑自己的 mvn test + PIT 确认真杀）；对方杀死的手段若踩了当场规则的禁止层（如 MockedStatic 改静态返回），不得照搬（见 `coverage-mutation-research.md` 顶部的规则依赖提示）。

> 本轮实证：glm 线按此方法定位出 +92 的引擎差距并开始移植（未完成即收卷）；对照两组最终数字——并行集成线 Math 588 用例 / KILLED 2427（96.0%），glm 线 1084 用例 / 2330（92.2%）——**用例数量多不等于杀死率高**，"少而准"胜过"多而不够狠"，两条线的并集才是上限。
