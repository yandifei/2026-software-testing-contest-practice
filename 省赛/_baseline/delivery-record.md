# 赛事交付记录（2026 省赛 · 开发者测试 · 个人）

## 要求

| 字段 | 记录 |
| --- | --- |
| 赛事、阶段、题目与工程 | 2026软件测试大赛-开发者测试-个人-省赛（mooctest contest 35）；题目 1：2026-开发者-CorCard-省赛（card-approval，problemId 135）；题目 2：2026-开发者-Math-省赛（math-assessment，problemId 136） |
| 来源 URL、核验时间及范围 | https://www.mooctest.net/student/contest/35 ；2026-11-15 14:00–14:20（Asia/Shanghai）读取标题、时间、题目卡片、附件名、上传槽（file-135-0/1、file-136-0/1）；题包内 `开发者测试比赛要求.md` 同页读取 |
| 截止时间与时区 | 2026-11-15 18:00 +08:00 |
| 项目相对位置 | `原始项目/card-approval/`、`原始项目/math-assessment/`（原始 ZIP 同目录保留：card-approval.zip、math-assessment.zip） |
| Java、Maven、JUnit 和插件版本 | JDK 21.0.9（`A:\MC\JAVA SDK`）、Maven 3.9.11、JUnit 5.10.2、JaCoCo 0.8.11、PIT 1.15.3 + pitest-junit5-plugin 1.2.1、Mockito 5.12.0 |
| 允许修改范围与测试数量要求 | 只允许 `src/test/java/net/mooctest/StudentTest.java` 单文件；禁止改 `src/main/**` 与 `pom.xml`；无 `main()` |
| 上传槽、类型、容量及 target 要求 | 每题：文件1 测试脚本 .java ≤2.0MB、文件2 项目 ZIP ≤20.0MB；ZIP 第一层直接为 pom.xml / 开发者测试比赛要求.md / src/ / target/，target 必须含 pit-reports/index.html 与 site/jacoco/index.html |
| 执行轮数约束与未决规则 | 用户未限定轮数；判分以最后一次提交的 .java 为准，ZIP 为分数异议辅助材料。未决：平台内部评分口径（分母是否含 NO_COVERAGE）、是否允许 mock 构造 record/enum（本记录按“禁止 mockConstruction/MockedStatic”自我约束执行） |

## 验证

| 字段 | 记录 |
| --- | --- |
| 原题包来源和基线清单 | 附件下载 URL（OSS 预签名，来源 `/api/contests/35/problems/{135,136}/files/url`）；ZIP SHA-256：card-approval.zip `7278AAE7…12737`（856,036 B）、math-assessment.zip `8417882C…EAA58`（1,642,842 B）；解包用 GBK 文件名修复脚本 |
| 源码/POM 清单与哈希比较 | `_baseline/card-approval.json`（105 文件）、`_baseline/math-assessment.json`（136 文件）；打包前用 `_tools/verify_baseline.py` 复核 src/main 与 pom.xml 完全一致、测试目录仅 StudentTest.java |
| 测试版本或哈希 | 见下方两题表格（最终版 SHA-256） |
| 命令、时间、退出码 | `mvn -B clean test`、`mvn -B org.pitest:pitest-maven:mutationCoverage`（JAVA_HOME=`A:\MC\JAVA SDK`） |
| 发现/通过/失败/错误/跳过数 | 见下方两题表格 |
| JaCoCo 分支 covered/total | 见下方两题表格 |
| 行覆盖率及工具 | JaCoCo LINE，见下方表格 |
| PIT 杀死数/总数、算子及目标 | DEFAULTS 算子；targetClasses=net.mooctest.*；targetTests=net.mooctest.* |
| PIT 其他状态及存活项分析 | 见下方表格与残余分析 |
| 同版本报告位置 | `原始项目/<项目>/target/pit-reports/`、`原始项目/<项目>/target/site/jacoco/` |

## 交付

| 文件 | 相对位置 | 字节数 | SHA-256 |
| --- | --- | --- | --- |
| Card 测试 Java | `deliverables/card-approval/StudentTest.java` | 待填 | 待填 |
| Card 项目 ZIP | `deliverables/card-approval/card-approval.zip` | 待填 | 待填 |
| Math 测试 Java | `deliverables/math-assessment/StudentTest.java` | 待填 | 待填 |
| Math 项目 ZIP | `deliverables/math-assessment/math-assessment.zip` | 待填 | 待填 |

ZIP 条目/解包检查、两处测试字节一致性、target 保留情况：由 `_tools/package_project.py` 校验（顶层 pom.xml/src/target、单测试文件、字节一致、报告入口齐全）。

## 提交状态

| 题目 | 上传 | 提交 | 回执 |
| --- | --- | --- | --- |
| Card | 未提交 | — | — |
| Math | 未提交 | — | — |
