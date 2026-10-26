# PricingCalculator 单元测试说明

被测对象是 `net.mooctest` 包下的酒店定价类：PricingCalculator、Reservation、Guest、Season、RoomType、MemberLevel。
原文档的 FastJson 描述与当前源码不符，已按用户确认的酒店定价项目修正。
测试不修改 `src/main/java`，不排除任何被测类、方法或 DEFAULTS 变异算子，不使用 mock。

## 环境与复现

- Java 21；本机路径：`A:\MC\JAVA SDK`。
- Maven 3.9.11；JUnit Jupiter 5.10.2。
- JaCoCo 0.8.11；PIT 1.15.3，JUnit 5 插件 1.2.1，`DEFAULTS` 变异算子。
- 显式指定 Maven resources 插件 3.4.0，使用本机已有离线缓存。

通常运行：

```powershell
mvn test org.pitest:pitest-maven:mutationCoverage
```

Windows 自动激活 `windows-coverage-path`，将覆盖率数据写到系统临时目录，避免中文路径使 JaCoCo agent 启动失败。HTML/XML 报告仍输出到项目 `target`。`.mvn/jvm.config` 为 Maven 配置 UTF-8 控制台输出。
以下简化命令已验证成功，`-o` 使用本机已有依赖缓存：

```powershell
$env:JAVA_HOME = 'A:\MC\JAVA SDK'
mvn -o test org.pitest:pitest-maven:1.15.3:mutationCoverage
if ($LASTEXITCODE -ne 0) { throw '测试或指标门槛未通过' }
```

## IDEA 运行

已添加共享 Maven 运行配置 `.run/Pricing tests and mutations.run.xml`。在 IDEA 顶部运行配置列表选择 **Pricing tests and mutations**，点击普通运行按钮。
也可以编辑现有 Maven 运行配置，将命令行改为：

```text
test org.pitest:pitest-maven:1.15.3:mutationCoverage
```

`mutationCoverage` 执行变异测试并生成报告。`report` 只使用已有报告，用于 Maven site；执行 `clean` 后报告目录被删除，直接运行 `report` 会报 `could not find reports directory`。
项目不需要先运行 `report`。生成后直接打开 `target/pit-reports/index.html`。

模块附加配置曾把整个 `src` 标为生产源码目录，已修正为 `src/main/java`，`src/test/java` 保持测试目录。IDEA 的右键 JUnit 编译由 IDE 模块模型决定；本次直接验证的是共享配置中的 Maven 命令，不宣称已复测 IDE 内的右键运行。

## 测试范围

`src/test/java/net/mooctest/PricingCalculatorTest.java` 包含 13 个测试方法，参数化展开后执行 30 次：

- 三种房型的基础房价、三种会员等级的折扣。
- 全部十二个月的季节映射、系数和最终房价。
- 多床、多早餐人数、多晚住宿，费用按晚数累计。
- 旺季会员优惠与加床、早餐的组合；附加服务不参与房费折扣。
- 二月底入住跨三月，季节仍按入住月份确定。
- 零晚住宿，以及带小数的金额和浮点计算结果的金额规范化。
- Guest 姓名和等级，Reservation 的所有字段及入住月份。
- null 月份的现有异常行为。

预期金额使用手工计算的固定值，不调用被测代码生成预期值。
现有固定费率与整数数量无法产生任意半分金额；金额测试不宣称已验证所有 HALF_UP 舍入边界。

## 最新实测结果

2026-10-14，完整测试、覆盖率校验和变异测试命令返回 `BUILD SUCCESS`：

| 指标 | 结果 |
| --- | --- |
| JUnit 执行次数 | 30，通过 30，失败/错误/跳过均为 0 |
| JaCoCo 分支覆盖率 | 100%，3/3 |
| JaCoCo 行覆盖率 | 100%，60/60 |
| JaCoCo 方法覆盖率 | 100%，23/23 |
| JaCoCo 类覆盖率 | 100%，6/6 |
| PIT 变异杀死率 | 100%，25/25，全部 KILLED |
| PIT 存活、未覆盖、超时、运行错误 | 均为 0 |

六个生产类全部纳入统计。分支数量以 JaCoCo 字节码报告为准，不等于十二个月的数量。
100% 变异杀死率针对 PIT 1.15.3 的 DEFAULTS 算子生成的 25 个变异，不代表其他算子也已验证。

## 指标门槛与报告

- `mvn test` 自动校验 JaCoCo 分支覆盖率，要求 100%。覆盖率数据每次重新写入，不累计旧结果。
- PIT 要求变异杀死率和行覆盖率均为 100%；需显式运行 `org.pitest:pitest-maven:mutationCoverage`。
- Surefire 在没有测试时失败。
- JaCoCo HTML：`target/site/jacoco/index.html`；XML：`target/site/jacoco/jacoco.xml`。
- PIT HTML：`target/pit-reports/index.html`；XML：`target/pit-reports/mutations.xml`。
- 初次完整验证日志：`target/final-verification.log`；IDEA 同款 Maven 和事件监听插件验证日志：`target/idea-exact-command.log`。
- `target/report/mutations.xml` 是项目原有旧报告，最新结果以 `target/pit-reports` 为准。
