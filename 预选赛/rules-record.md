# 2026 开发者测试预选赛 · 当场规则记录

核验时间：2026-11-01 14:10–14:50（Asia/Shanghai）。来源：用户从慕测平台下载的预选赛题包（本次当场材料），结合 2026-10-28 练习赛历史页面快照（docs/mooctest-developer-sop/references/competition-rules.md）。

| 项目 | 本次题包/用户要求 | 历史官方资料（练习赛快照） |
| --- | --- | --- |
| 阶段与时间 | 开发者测试-个人-预选赛，2026-11-01 14:00–17:00 | 预选赛时间与官网赛程一致 |
| 环境 | pom.xml：JDK 21、Maven 3.9.6+、JUnit 5.10.2、Mockito 5.12.0、JaCoCo 0.8.11、PIT 1.15.3 | JDK 21 / Maven 3.9.6+ / JUnit 5.10.2 |
| 修改边界 | 题包 StudentTest.java 头部注释：只提交一个 Java 测试文件，可直接写进 StudentTest.java 或新建 XxxTest.java（顶层类名与文件名一致）；禁止改被测源码与 pom.xml | 禁止改 src/main 与 POM，仅允许测试代码 |
| 打包 | “上传项目压缩包时的打包方式，应与下载的题目模板打包方式一致”——题包解压后直接含 pom.xml/src/target，故 ZIP 条目根为这三项，保留执行后 target | 完整项目 ZIP，包内保留 target |
| 上传物 | 每题一个 Java 文件 + 一个项目 ZIP（槽位与大小上限以当场页面为准，未在页面上核实；本地包 0.63MB/45KB） | Java ≤2.0MB、ZIP ≤20.0MB（练习赛页面） |
| 题目 | 1) corporate-card-expense-approval_1793512054906（卡组织校验/报销策略/权限策略/审批工作流，58 主类）2) online-math-expression-grading_1793511951523（表达式引擎/判分服务/学习进度，51 主类） | — |
| 用户要求 | 每做完一题提交 Git；完成后自行完成浏览器上传提交 | — |
| 未决规则 | 评分实现、单文件执行方式、重交次数、ZIP 顶层命名模板（按题包结构推断） | AI/反射/额外资源许可仍待确认，本次测试未使用反射与额外资源 |
