# 赛事交付记录

## 要求

| 字段 | 记录 |
| --- | --- |
| 赛事、阶段、题目与工程 | 2026软件测试大赛-开发者测试-个人-预选赛；题目 corporate-card-expense-approval（企业卡报销审批），工程名 corporate-card-expense-approval_1793512054906 |
| 来源 URL、核验时间及范围 | 题包由用户于 2026-11-01 14:10 前后从慕测平台下载并解压至工作区；本记录核验时间 2026-11-01（Asia/Shanghai） |
| 截止时间与时区 | 2026-11-01 17:00（Asia/Shanghai），预选赛时段 14:00–17:00 |
| 项目相对位置 | 预选赛/corporate-card-expense-approval_1793512054906 |
| Java、Maven、JUnit 和插件版本 | JDK 21.0.9（A:\MC\JAVA SDK）、Maven 3.9.11、JUnit Jupiter 5.10.2、Mockito 5.12.0、JaCoCo 0.8.11、PIT 1.15.3 + pitest-junit5 1.2.1 |
| 允许修改范围与测试数量要求 | 仅允许编写测试代码；单一 Java 测试文件（StudentTest.java，顶层类名与文件名一致）；禁止修改 src/main 与 pom.xml |
| 上传槽、类型、容量及 target 要求 | Java 测试脚本 1 个 + 项目 ZIP 1 个；ZIP 与下载题包结构一致（条目根为 pom.xml/src/target）；保留执行后 target |
| 执行轮数约束与未决规则 | 未限定轮数；上传大小上限未在页面上核实，ZIP 实际 0.63MB、Java 45KB，远低于常规 20MB/2MB 上限 |

## 验证

| 字段 | 记录 |
| --- | --- |
| 原题包来源和基线清单 | 题包解压目录；基线 = pom.xml + src/main 全部文件 SHA-256（baseline-card.sha256，58 个文件） |
| 源码/POM 清单与哈希比较 | 打包前与解包后各比对一次，均 0 差异 |
| 测试版本或哈希 | StudentTest.java（工程内与交付副本字节一致，205 个测试方法） |
| 命令、时间、退出码 | `mvn -B -ntp test`（14:42，退出码 0）；`mvn -B -ntp org.pitest:pitest-maven:mutationCoverage`（14:43，退出码 0） |
| 发现/通过/失败/错误/跳过数 | 205 / 205 / 0 / 0 / 0 |
| JaCoCo 分支 covered/total | 1134/1180（96.1%） |
| 行覆盖率及工具 | 1398/1407（99.4%），JaCoCo LINE |
| PIT 杀死数/总数、算子及目标 | 699/801（87.3%），DEFAULTS 算子，targetClasses net.mooctest.*；另有 12 个 NO_COVERAGE，测试强度 89%，PIT Line Coverage 1202/1213 |
| PIT 其他状态及存活项分析 | 90 个未杀死（不含 NO_COVERAGE）：TypeChecker 18 个为被前置校验器遮蔽的品牌（ChinaUnionPay/DinersClub/RuPay/InterPayment/InstaPayment/Dankort/NPS/Verve/LankaPay 永远不可达）；Verve 7 个为重复扫描段（第二段与第一段区间相同，650002-650027 文档区间实际不生效）；Luhn 1 个为有效号求和符号取反仍模 10 为 0 的等价变异；AccessPolicy 多数为 deny-overrides 下仅改变理由列表而不改变 allowed 结果的等价变异；ExpensePolicy 部分为阈值比较的边界等价；均已按行定位记录 |
| 同版本报告位置 | target/surefire-reports、target/site/jacoco、target/pit-reports（与 205 测试版本同轮生成） |

## 交付

| 文件 | 相对位置 | 字节数 | SHA-256 |
| --- | --- | --- | --- |
| 测试 Java | deliverables/corporate-card-expense-approval/2026-11-01/StudentTest.java | 46596 | 与工程内字节一致 |
| 项目 ZIP | deliverables/corporate-card-expense-approval/2026-11-01/corporate-card-expense-approval_1793512054906.zip | 661907 | 9099F5A0E478646F91B3B13699A5AFB934248CA369F4B53C72812E0439994952 |

ZIP 条目 344 个，顶层为 pom.xml、src、target，无越界路径；解包后 src/main 与 pom.xml 哈希与基线 0 差异，包内 StudentTest.java 与工程内字节一致；target 保留 surefire-reports、site/jacoco、pit-reports 与 jacoco.exec。未在解包副本复跑（同一构建产物已由上述两命令生成）。

上传/提交状态：见浏览器回执截图与时间，另行补充。
