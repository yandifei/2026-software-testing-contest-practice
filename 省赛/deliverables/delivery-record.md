# 赛事交付记录 · 2026 省赛 · 开发者测试 · 个人

> 本文件留在 ZIP 之外，不随参赛包上传。所有数字均可由 `_tools/` 下脚本从对应报告复核。

## 一、要求

| 字段 | 记录 |
| --- | --- |
| 赛事、阶段、题目与工程 | 2026软件测试大赛-开发者测试-个人-省赛（mooctest contest 35，DEVELOPER_TEST，NATIONAL）；题1 `2026-开发者-CorCard-省赛`（card-approval，problemId 135）；题2 `2026-开发者-Math-省赛`（math-assessment，problemId 136） |
| 来源 URL、核验时间及范围 | https://www.mooctest.net/student/contest/35 ；2026-11-15 14:00–14:20 读取页面标题/时间/题目卡片/上传槽；`/api/contests/35` 读取 scoringWeights；题包内《开发者测试比赛要求.md》逐字读取 |
| 截止时间与时区 | 2026-11-15 18:00 +08:00（页面倒计时以整小时向下取整显示） |
| 项目相对位置 | 原始题包与解压件：`原始项目/`；开发与报告：`deepseek/card-approval/`、`deepseek/math-assessment/` |
| Java、Maven、JUnit 与插件 | JDK 21.0.9（`A:\MC\JAVA SDK`，系统默认 JAVA_HOME 指向 Java 8，全程显式指定）、Maven 3.9.11、JUnit 5.10.2、Mockito 5.12.0、JaCoCo 0.8.11、PIT 1.15.3 + pitest-junit5 1.2.1 |
| 允许修改范围与测试数量要求 | 仅 `src/test/java/net/mooctest/StudentTest.java` 单文件；禁改 `src/main/**` 与 `pom.xml`；无 `main()`；JUnit 5 注解、语义化命名 |
| 上传槽、类型、容量与 target 要求 | 每题：文件1 `.java` ≤2.0MB、文件2 `zip` ≤20.0MB；ZIP 首层必须是 `pom.xml`、`开发者测试比赛要求.md`、`src/`、`target/`（target 内含 `pit-reports/index.html` 与 `site/jacoco/index.html`） |
| 评分权重（平台 API 实测） | 每题 `mutation 25 / coverage_branch 25 / coverage_method 25 / coverage_statement 25`；每题占总分 50% |
| 执行轮数约束与未决规则 | 用户未限轮数，但要求"无法提升就停手提交"；判分以最后一次提交的 `.java` 为准，ZIP 为分数异议辅助材料。未决：平台 statement 口径（JaCoCo LINE 为代理）、平台是否使用更强算子集 |

## 二、基线（可复现）

| 项 | Card | Math |
| --- | --- | --- |
| 原始 ZIP | `card-approval.zip` 856,036 B，SHA-256 `7278AAE73C4B4BDBD5B1B19C5E51483C261A43873E28D3AF564A46147EE12737` | `math-assessment.zip` 1,642,842 B，SHA-256 `8417882C7D5BB1B141E57C7ECB62FBE42D7F11E30A4B27CCDFD1F18AD26EAA58` |
| 解包 | `_tools/extract_zip.py`（GBK 文件名修复），无越界条目 | 同 |
| 源码/POM 基线 | `_baseline/card-approval.json`（105 文件） | `_baseline/math-assessment.json`（136 文件） |
| 基线校验（每次打包前） | `_tools/verify_baseline.py`：`src/main` + `pom.xml` 与基线逐文件 SHA-256 一致、测试目录仅 `StudentTest.java` → **每次均 OK** | 同 → **每次均 OK** |
| 题包自带旧报告（骨架测试） | 1320 变异 / KILLED 12；BRANCH 9/2013 | 2528 变异 / KILLED 87；BRANCH 100/2659 |

## 三、最终结果

### Card（card-approval）

| 指标 | 数值 | 百分比 |
| --- | --- | --- |
| 测试文件 | `deepseek/card-approval/src/test/java/net/mooctest/StudentTest.java`，8,341 行；SHA-256 `73F2C2A7A5DB5834572BCAD4AF0BC66504BF042C4D01315448EE850D8C886CDC` | 476,181 B ≤ 2.0 MB |
| 用例 | 258（109 主体 + 75 + 63 + 11 四段） | 0 fail / 0 error / 0 skip |
| METHOD | 275 / 275 | **100.00%** |
| LINE | 2360 / 2364 | **99.83%** |
| BRANCH | 2003 / 2013 | **99.50%** |
| INSTRUCTION | 11372 / 11385 | 99.89% |
| CLASS | 97 / 97 | 100% |
| PIT（DEFAULTS，`net.mooctest.*`） | KILLED 1296 / 1320 | **98.18%** |
| PIT 其他状态 | SURVIVED 23、NO_COVERAGE 1、TIMED_OUT 0、RUN_ERROR 0 | — |
| 报告 | `target/site/jacoco/`、`target/pit-reports/`（XML + HTML 齐全） | — |

### Math（math-assessment）

| 指标 | 数值 | 百分比 |
| --- | --- | --- |
| 测试文件 | `deepseek/math-assessment/src/test/java/net/mooctest/StudentTest.java`，11,734 行；SHA-256 `B4C926C3336B2B293713A5672DD2527117A4687361B32BDA7A055E81ECD52AAD` | 660,451 B ≤ 2.0 MB |
| 用例 | 588（575 主体 + 13 补杀） | 0 fail / 0 error / 0 skip |
| METHOD | 715 / 725 | **98.62%** |
| LINE | 3993 / 4050 | **98.59%** |
| BRANCH | 2616 / 2659 | **98.38%** |
| INSTRUCTION | 19641 / 19934 | 98.53% |
| CLASS | 171 / 171 | 100% |
| PIT（DEFAULTS，`net.mooctest.*`） | KILLED 2427 / 2528 | **96.00%** |
| PIT 其他状态 | SURVIVED 70、NO_COVERAGE 27、TIMED_OUT 4、RUN_ERROR 0 | — |
| 报告 | 同左 | — |

### 本地估算得分（仅参考，平台口径未公开）

按 `mutation 25 + branch 25 + method 25 + statement(以 LINE 代理) 25`，两题各占 50%：
- Card ≈ 24.55 + 24.88 + 25.00 + 24.96 = 99.39 → 49.69
- Math ≈ 24.00 + 24.60 + 24.66 + 24.65 = 97.91 → 48.95
- 合计 ≈ **98.6 / 100**（平台按自己的 statement 口径与算子集重算，可能不同）

## 四、残余项（如实，附证明文件）

| 题 | 类别 | 数量 | 证明文件 |
| --- | --- | --- | --- |
| Card | 等价（重复扫描 / 取负模 10 / 赋值后立即返回 / 上游已拦截） | 15 | `_baseline/card-equivalence-appendix.md`、`card2-branch-notes.md` |
| Card | 不可达（构造器不变量、紧凑构造器 NPE、`(0,0.5)` 空域、switch 全覆盖 default） | 9 | 同上 |
| Math | 不可达（区间倒挂死区、`validate` 前置、章节 max=0、枚举 switch default、动态编译出口、`(2·MAX)` 溢出仅一侧） | ~41 | `_baseline/math-equivalence-appendix.md` |
| Math | 等价（`money()` 归一化、零贡献算术、同 index 另一侧算子、短路死操作数） | ~30 | 同上 |
| Math | 未解决（已覆盖但未构造出区分输入，如实保留） | 见附录 §3 | 同上 |

> 题包说明明确「覆盖率或变异杀死率的理论上限可能低于 100%」；本交付**保留原始分母**，未做分母缩减、算子排除、报告篡改、字节码替换或 mutant 探测。

## 五、方法边界声明（合规）

- 白盒手段：真实输入、反射直调私有/包私有方法、`Mockito.mock()` 构造 record/enum 的非法状态、`@TempDir` 临时文件 —— 均不改变被测实现。
- **未使用** `mockConstruction` / `mockStatic` / `MockedStatic`（会替换被测协作者或静态实现，属赛题《作弊与违规说明》第 2 点违规示例二）。
- 未改 `src/main/**`、未改 `pom.xml`、未新增其它测试文件、未改报告；每次打包前后均跑基线哈希校验。
- 已知被测实现缺陷（如实记录，未修改源码）：`Shift` 紧凑构造器读取未赋值的 `this.end`，任何 `NIGHT` 班次构造即 NPE；表达式引擎 `DynamicFunctionCompiler` 生成源码 import 了不存在的包，动态编译入口必抛 `RuntimeException("动态函数生成失败")`。

## 六、交付文件

| 文件 | 位置 | 字节数 | SHA-256 |
| --- | --- | --- | --- |
| Card 测试 Java | `deliverables/card-approval/StudentTest.java` | 476,181 | `73F2C2A7A5DB5834572BCAD4AF0BC66504BF042C4D01315448EE850D8C886CDC` |
| Card 项目 ZIP | `deliverables/card-approval/card-approval.zip` | 1,191,812 | `677B270AB488BE6C70D1124748AFACDD8BB683432B767623F37089C7B077B4E6` |
| Math 测试 Java | `deliverables/math-assessment/StudentTest.java` | 660,451 | `B4C926C3336B2B293713A5672DD2527117A4687361B32BDA7A055E81ECD52AAD` |
| Math 项目 ZIP | `deliverables/math-assessment/math-assessment.zip` | 2,352,193 | `B2B2FE28EF15A4CA9AE38A9317A23F7C60A5761DEB33832CEEDB02A40454570F` |

ZIP 结构校验（`_tools/package_project.py`）：首层 `pom.xml` / `开发者测试比赛要求.md` / `src` / `target`；`target/pit-reports/index.html` 与 `target/site/jacoco/index.html` 存在；`src/test` 仅一个 `StudentTest.java` 且与磁盘文件字节一致（Card 557 条目 / 1.14 MiB；Math 936 条目 / 2.24 MiB）。

## 七、提交记录（平台回执）

| 题目 | 最近提交时间（平台） | 文件1（java） | 文件2（zip） | 页面状态 |
| --- | --- | --- | --- | --- |
| Card | 2026-11-15 16:25:17 | 476,181 B | 1,191,812 B | 已提交，进度 2/2 |
| Math | 2026-11-15 16:25:21 | 660,451 B | 2,352,193 B | 已提交，进度 2/2 |

提交历史（本轮）：Card 14:51 → 15:22 → 15:26 → 15:47 → 16:22 → **16:25**；Math 15:00 → 15:11 → 15:48 → 16:18 → **16:25**（更早的 14:24/14:25 两次非本会话提交，已被覆盖）。

与上一次提交（Card 15:47、Math 15:48）的对比：Card PIT 97.42%→**98.18%**（+10 杀死）、BRANCH 99.30%→**99.50%**、LINE 99.66%→**99.83%**、用例 247→258；Math PIT 95.65%→**96.00%**（+9 杀死）、BRANCH 98.35%→**98.38%**、用例 575→588。两题均为提升后版本。

未出分：页面「当前排名：待公布」，状态为**评分中/待公布**。平台成绩与本地指标分开记录。
