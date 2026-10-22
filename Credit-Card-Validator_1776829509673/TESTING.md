# 信用卡校验项目测试结果

最新验证：2026-10-23，Oracle Java 21.0.9、Maven 3.9.11、JUnit Jupiter 5.10.2、JaCoCo 0.8.11、PIT 1.15.3。

**在生产源码不变的约束下，原始分支覆盖率为 98.73%，严格变异杀死率为 96.69%。两个指标无法达到 100%：剩余 3 个分支不可达，9 个变异与原实现等价。** 已逐项复核并保留全部分母，未排除生产类、分支或变异体。

## 最新实测结果

| 指标 | 结果 |
| --- | --- |
| JUnit 用例 | 156，通过 156，失败 0，错误 0，跳过 0 |
| 单元测试耗时 | 各测试类报告合计 0.319 秒 |
| JaCoCo 分支覆盖 | 233 / 236，98.73% |
| JaCoCo 行覆盖 | 494 / 497，99.40% |
| JaCoCo 指令覆盖 | 1995 / 1999，99.80% |
| 方法、类覆盖 | 98 / 98 个方法、32 / 32 个类，均为 100% |
| PIT 变异体 | 272 |
| PIT KILLED | 263，严格杀死率 96.69% |
| PIT SURVIVED | 9，逐项等价分析见下文 |
| PIT TIMED_OUT、NO_COVERAGE、RUN_ERROR | 均为 0 |
| 完整 Maven 验证耗时 | 11.055 秒，BUILD SUCCESS |

PIT 使用项目原有 DEFAULTS 配置。此次 263 个检出全部是 KILLED，不再将 PIT 超时状态合计为杀死数。赛事实际变异集可能不同，上述结果仅对应本项目和这套配置。

## 测试设计与本轮增强

全部测试位于 `src/test/java/net/mooctest/`。

| 文件 | 用例数 | 检查范围 |
| --- | ---: | --- |
| CardFormatTest.java | 22 | 全部 22 种卡类型；0～21 位长度；号段端点、内部值和相邻非法值；组合校验及空值异常 |
| CreditCardValidatorTest.java | 55 | 公共入口、卡类型识别、CVV、有效期、构造器初始化、异常输入及识别终止性 |
| LuhnValidatorTest.java | 43 | 已知样本、数位和、100 个两位串及 1000 个三位串、固定种子生成号码的逐位替换、非数字字符的两种奇偶位置 |
| ParserAndDateTest.java | 26 | 数字顺序、前导零、IIN 长度及溢出、日期切片、原地修改、比较和格式化 |
| KnownDefectsTest.java | 10 | 已知缺陷、注释差异及 API 当前行为限制 |

循环内的数据组属于同一测试方法，不额外计入 JUnit 用例数。测试使用真实生产类，没有 Mockito 替代业务逻辑、禁用用例、修改字节码或反射注入不真实状态。Luhn 的独立数字映射表不调用被测算法计算预期值。

本轮新增 11 组非数字字符参数，覆盖 `Character.getNumericValue` 返回 -1、-2 的情况。例如 `-0` 原实现返回 false，而强制进入 `sumOfDigits` 的变异会抛出异常，因此能被杀死。这些现状断言并不表示含非数字字符的号码应被业务接受。

所有能进入未知卡类型回退路径的正常分类断言，增加了 2 秒的 JUnit 抢占式超时约束。该操作正常只需毫秒；删除回退分支导致无限循环时，测试自身失败，PIT 将该变异记录为 KILLED。PIT 的进程超时常量设为 5000 毫秒，留出测试断言报告失败的时间。抢占式超时不能强制终止忽略中断的错误 Java 循环，因此变异验证由 PIT 的独立 JVM 隔离并回收；未使用 Thread.stop。

`KnownDefectsTest` 的 `known-defect` 标签仅用于标识，全部参与执行；这些断言记录现状，不是正确业务规则的声明。

## 不可达分支证明

| 位置 | 证明 |
| --- | --- |
| TypeChecker.java:20，while 的 false 分支 | 局部变量 type 初始为 null；循环内每次赋值后立即 break，因此无法回到循环入口且 type 非 null |
| TypeChecker.java:56，if 的 false 分支 | 能到达此处意味着之前四个识别分支均未赋值；若赋值早已 break，所以此处 type 必为 null |
| VerveValidator.java:62，第二次扫描的命中分支 | 第一轮已用同一个局部整数 IIN 遍历相同区间；只有未命中才进入第二轮，重复扫描不可能命中 |

上述循环局部变量和区间在扫描中不受输入对象或外部回调修改。合法输入、子类化或并发调用均无法改变这些控制流不变量。JaCoCo 未覆盖的 3 行分别是 TypeChecker 第 61 行的循环回跳，以及 Verve 第 63、64 行的不可达赋值与 break。

因此，保持当前字节码对应逻辑不变时，可由测试覆盖的分支上限是 **233/236**。本报告不通过过滤这些分支将统计改成 100%。

## 九个存活变异的等价证明

下表逐项对应 `reports/mutations.xml` 中的 SURVIVED 记录。

| 类和行号 | 变异 | 等价原因 |
| --- | --- | --- |
| LuhnValidator:76 | 累加替换为累减 | 变异总和是原总和的 Java int 相反数；对任意数，是否能被 10 整除不因取反改变。即使发生 int 溢出，该关系仍在模 2^32 下成立；唯一取反仍相等的非零值 MIN_VALUE 在两边都不能被 10 整除 |
| VerveValidator:53 | 第一轮 `<=` 改成 `<` | 第一轮漏掉的上端点仍会被第二轮完整区间接受 |
| VerveValidator:61 | 第二轮 `<=` 改成 `<` | 所有能到第二轮的 IIN 本来就不在该区间内 |
| VerveValidator:54 | 删除第一轮命中判断 | 第二轮会完整执行同样的查找并返回同样结果 |
| VerveValidator:60 | 跳过第二轮外层条件分支 | 第二轮是冗余查找，不会改变返回值 |
| VerveValidator:62 | 删除第二轮命中判断 | 该命中分支原本不可达 |
| VerveValidator:53 | 跳过第一轮循环 | 第二轮完整执行相同查找 |
| VerveValidator:61 | 跳过第二轮循环 | 移除不影响结果的冗余查找 |
| Validator:64 | 将返回值替换为 true | false 已在前面的条件中抛异常；能正常返回时原返回值必为 true |

针对这些等价变异继续增加普通业务输入不会产生返回值或异常差异。通过毫秒级性能差、反射读取 class 字节或篡改覆盖数据区分它们，不属于可靠的业务单元测试。本报告不这样制造杀死数。

两个指标的限制已经由独立只读代码审查复核。本轮复核未发现其他正常可达分支或可区分的存活变异。

## 源码保护与执行历史

本项目此前尚未纳入父目录 Git 仓库；本次提交包含原样的生产源码和 pom.xml，以便仓库能够独立构建。**加入版本控制不等于修改生产代码。**

`reports/source-sha256.json` 保存首次测试编写前的 32 个生产源文件和 pom.xml 的相对路径及 SHA-256。本轮重新校验 33 个文件，改动数为 0。测试、报告和 Git 忽略配置是本轮修改范围。

| 阶段 | 结果 |
| --- | --- |
| 2026-10-22 初次任务 | 测试一次写完；首次 JVM 因 JaCoCo 中文绝对路径损坏启动失败，实际用例为 0；调整命令行路径后，145 个用例通过，261 KILLED、1 TIMED_OUT、10 SURVIVED |
| 2026-10-23 用户要求继续提高并推送 | 授权后的第二轮开发，新增非数字字符断言及终止性断言；执行一次 clean + test + PIT，156 个用例通过，263 KILLED、9 SURVIVED |

第一轮“一次编写”的约束和执行记录属于上一阶段；本轮按用户后续要求继续增强，不把两轮工作描述成只执行过一次测试。

## 复现

在本项目目录执行以下 PowerShell 命令；本地环境已安装相应 JaCoCo 依赖。通过相对 agent 输出路径规避本机含中文绝对路径的 JVM agent 编码问题，pom.xml 不变。

```powershell
$agentJar = Join-Path $env:USERPROFILE '.m2\repository\org\jacoco\org.jacoco.agent\0.8.11\org.jacoco.agent-0.8.11-runtime.jar'
mvn -B -ntp clean test org.pitest:pitest-maven:1.15.3:mutationCoverage `
  "-DargLine=-javaagent:${agentJar}=destfile=target/jacoco.exec" `
  '-DoutputFormats=XML,HTML' '-Dthreads=4' `
  '-DtimestampedReports=false' '-DtimeoutConstant=5000'
```

如果本地 Maven 仓库被自定义，应将 `$agentJar` 指向实际 agent jar。上述命令中的用户目录也应避免空格；路径含空格时需要对传给 Surefire 的完整 `-javaagent` 参数再加引号。

## 已发现的业务问题和能力边界

这些判断以项目源码和注释为依据，不将注释等同于已经外部核实的银行卡规范。

| 项目 | 当前行为与判断 |
| --- | --- |
| 年份比较 | 过去年份且月份大于当前月仍被接受；明确的日期逻辑缺陷 |
| 月份边界 | 00、13 月可能被接受；缺少范围检查 |
| 到期当月 | 返回 false；若采用月末到期规则则为边界缺陷，需确认业务约定 |
| Verve | 声明的 650002～650027 段被拒绝；重复扫描第一段导致遗漏 |
| UATP | 使用六位 IIN 与常量 1 比较；100000 开头拒绝，000001 开头接受 |
| Visa Electron | 四位、五位解析不能正确匹配声明的 417500 六位前缀；还会误收前导零或含字母输入 |
| Maestro UK | 仅接受 676770、676774 两端点，拒绝声明区间的内部值 |
| 注释差异 | China T-Union 实际长度 19、注释 16；InterPayment 实际前缀 636、注释 637～639 |
| 非数字与空卡号 | 空串、全零、部分字母及标点组合可通过校验和；调用方不能将校验和当作完整卡号校验 |
| CVV | 只检查长度，字母和空格可以通过；属于待业务确认的校验范围 |
| validate | 只检查 Luhn，不联合检查有效期和 CVV；与方法注释一致，是调用方需注意的契约 |
| 日期格式 | 尾随字符会被忽略，例如 12/99ignored |
| 类型支持 | TypeChecker 只识别四类，JCB 等返回 OTHER；符合该方法当前的支持范围说明 |

## 证据文件

已提交的可核验结果：

- [覆盖率原始 XML](reports/jacoco.xml)
- [完整变异体列表及状态](reports/mutations.xml)
- [JUnit 测试类计数与耗时](reports/test-summary.json)
- [原始生产源码与配置 SHA-256](reports/source-sha256.json)

本地重新运行后还可以查看 `target/site/jacoco/index.html`、`target/pit-reports/index.html` 和 `target/surefire-reports/`。target、IDE 文件、机器绝对路径冻结清单及执行日志不提交。
