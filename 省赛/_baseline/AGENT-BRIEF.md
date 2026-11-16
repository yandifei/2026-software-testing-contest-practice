# 省赛开发者测试 —— 执行者共用指令（必读）

你是本次省赛“开发者测试”赛项的执行者之一。本文件是所有执行者共用的硬约束与命令，禁止自行放宽。

## 赛事事实（2026-11-15 当场核对）

- 赛事：2026软件测试大赛-开发者测试-个人-省赛，contest 35，时间 2026-11-15 14:00–18:00（北京时间）。
- 两道题：`2026-开发者-CorCard-省赛`（题包 card-approval.zip，problemId 135）、`2026-开发者-Math-省赛`（math-assessment.zip，problemId 136）。
- 每题提交两个文件：`.java` 测试脚本（≤2.0MB）+ 项目 `zip`（≤20.0MB，第一层直接是 pom.xml、开发者测试比赛要求.md、src/、target/）。
- 判分以最后一次提交的 `.java` 为准，ZIP 仅作分数异议时的辅助材料。
- 环境：JDK 21、Maven 3.9.6+、JUnit 5.10.2。

## 硬约束（违反即取消资格或无效）

1. **测试代码只写在** `<项目>/src/test/java/net/mooctest/StudentTest.java` 一个文件里（包名 `net.mooctest`，public class StudentTest，无 `main` 方法）。
2. **不得修改** `src/main/**` 与 `pom.xml`。禁止新增/删除/移动被测源码。
3. 禁止：篡改字节码、改报告 XML、改 POM（缩分母/换算子/排除类）、运行时替换被测类字节码、探测 PIT mutant 身份后人为失败。
4. **Mock 政策（本赛题明确禁止“干预函数返回结果/使程序进入原实现无法到达的路径”）**：
   - 允许：真实输入、Mockito mock 构造 record/enum 以生成“公开构造器无法生成但类型系统允许”的输入状态、反射直调私有/包私有方法、反射读写字段（try/finally 恢复）。
   - **禁止：`Mockito.mockConstruction`、`Mockito.mockStatic`/`MockedStatic`** —— 它们替换被测协作者/静态方法的实现，属规则第 2 点违规示例二。宁可放弃该变异，也不得使用。
5. 不得依赖执行顺序：每个 @Test 独立；共享静态状态必须在 @BeforeEach/@AfterEach 恢复。
6. 不要用 `@Test(timeout=...)` 之外的手段杀超时类变异时，若用线程包装，等待上限必须 < 3000ms（建议 1000ms），且循环条件行与自增行必须有主线程测试执行过。

## 环境与命令

```powershell
$env:JAVA_HOME = "A:\MC\JAVA SDK"
$env:Path = "A:\MC\JAVA SDK\bin;$env:Path"
cd <项目目录>
mvn -B clean test                        # JUnit + JaCoCo（report 在 target/site/jacoco）
mvn -B org.pitest:pitest-maven:mutationCoverage   # 必须先 test，直接 goal 不编译
```
- JaCoCo 数据固定写 `target/jacoco.exec`（POM 已配 argLine，勿改）。
- PIT 报告：`target/pit-reports/mutations.xml`（分状态计数）+ HTML。
- 每次“惊喜提升”先怀疑 `target/jacoco.exec` 脏数据：`mvn -B clean test` 重跑。
- 中文路径一般无碍，但若 JaCoCo 报 missing execution data，先查 `target/jacoco.exec` 是否本轮生成。

## 统计口径（报告必须分开写）

- 分支：JaCoCo `BRANCH` covered/(covered+missed)，来自 `target/site/jacoco/jacoco.xml` 根节点 counter。
- 行：JaCoCo `LINE`。
- 变异：`mutations.xml` 中 KILLED / SURVIVED / NO_COVERAGE / TIMED_OUT / RUN_ERROR 分别计数，**禁止**用“总数−SURVIVED”。
- 判别：覆盖到 ≠ 杀死；SURVIVED 要逐项分类（缺输入/缺断言/未覆盖/等价/不可达）。

## 方法论（必须按此顺序，勿跳步）

1. 先读完被测源码（每轮至少覆盖要动的包），列出：公开 API 的正常/边界/异常/状态转移场景。
2. 先写“真实输入可达”的测试把行/分支覆盖拉满，再针对 PIT 存活项补断言。
3. 判“不可达/等价”前必须做可达性攻击（顺序）：Unicode/字母数字值域（`Character.getNumericValue` 无 35 上限，埃塞俄比亚数字 ፵=40 等）、前导零错位、溢出（MAX_VALUE、int 回绕、NaN/Infinity、-0.0）、下游异常消息顶包、对象同一性（assertSame/assertNotSame 与 scale/toString）、反射直调私有入口绕开上游归一化、线程超时包装死循环。
4. 等价/不可达要写路径证明：行号 → 变异后控制流 → 输入值域分析 → 下游兜底；保留原始分母，不许删测试/缩分母凑数。
5. 关键参考（务必读）：`B:\软件测试大赛\省赛\mooctest-developer-sop\references\` 下
   - `error-prevention.md`（13 条真实错误清单）
   - `coverage-mutation-research.md`（§8 可达性攻击手册、§9 数学题攻击手册）
   - `whitebox-research.md`（白盒手段收益与边界）
   - `testing-methodology.md`（报告脏数据、PIT 直接 goal、并发冲突）
   这些文档里的具体行号来自历史版本，必须按本题当前源码重新核对。

## 交付形态

- 最终只留一个 `src/test/java/net/mooctest/StudentTest.java`（UTF-8）。
- 文件头注释允许保留题目要求的说明；测试方法要有语义化命名与 @DisplayName。
- 报告给上级（用文字，不要贴大段代码）：
  1. 测试方法数 / 执行用例数 / 通过数；
  2. JaCoCo 分支 covered/total 与行覆盖；
  3. PIT KILLED/total 及各状态分布；
  4. 剩余 SURVIVED/NO_COVERAGE 的分类与证据（等价证明逐项写）；
  5. 已执行的命令、时间、退出码、报告路径；
  6. 未解决项与原因（不粉饰）。

## 与其他执行者的协作

- 每个执行者只动自己负责的项目目录，绝不跨项目写文件。
- 需要扩展测试时用 Edit 增量追加方法，不要整文件重写覆盖别人的成果。
- 定期 `git`-free 快照：每轮大改前把 StudentTest.java 复制到 `_baseline/<项目>-StudentTest-<时间>.java.bak`（工作区 `_baseline` 目录），便于回退。
