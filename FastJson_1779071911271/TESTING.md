# FastJson 单元测试说明

本项目实际被测对象是 `net.mooctest` 包下的 JSON、JSONObject、JSONArray、TypeReference，
不是科研项目管理业务系统。测试不修改 `src/main/java`，不排除任何被测类、方法或变异算子。

## 环境与复现

- Java 21；本机已验证的路径：`A:\MC\JAVA SDK\bin\java.exe`。
- Maven 3.9.11；JUnit Jupiter 5.10.2。
- JaCoCo 0.8.11；PIT 1.15.3，JUnit 5 插件 1.2.1，`DEFAULTS` 变异算子。

在项目根目录运行：

```powershell
# 一次完成干净构建、所有测试、规范检查、变异测试和结果汇总
pwsh -File scripts/verify.ps1

# 日常快速反馈
mvn test

# 单独查看规范或变异结果（PIT 前先编译测试）
mvn checkstyle:check
mvn test org.pitest:pitest-maven:mutationCoverage
```

报告位置：

- `target/site/jacoco/index.html`：分支、行、指令、方法覆盖率。
- `target/pit-reports/index.html`：原始变异报告。
- `target/pit-reports/mutations.xml`：每个变异体的状态和杀死它的测试。
- `target/surefire-reports`：每个测试类的执行结果。
- `target/verification-summary.json`：精确计数与百分比。
- `target/mutation-review.csv`：所有非 `KILLED` 状态，包含超时。
- `target/source-hashes.csv`：此次验证使用的生产源码 SHA-256。

`scripts/verify.ps1` 先执行 `clean`，避免历史 JaCoCo 数据累计产生虚高覆盖率。
PIT 将超时变异计为检出，因此汇总同时提供 `KILLED/总数` 和 `detected/总数`，不混淆二者。
最终测量及剩余变异说明见 [测试结果](docs/测试结果.md)。

## 测试组织

| 文件 | 主要验证行为 |
| --- | --- |
| JSONArrayTest | List 委托、顺序和返回值、扩容补空、类型转换、克隆及反序列化 |
| JSONObjectTest | Map 委托、标量转换、动态代理、注解属性和 Java 对象转换 |
| JSONObjectSerializationTest | Java 原生序列化、反射失败回退、类型描述符与代理接口校验 |
| TypeReferenceTest | 缓存身份、泛型变量替换、数组、替换参数耗尽和多层递归 |
| JSONParsingTest | 各解析入口、引用解析、特性传递、编码、输入流和错误输入 |
| JSONSerializationTest | 线格式、过滤器、日期格式、字节数、字符集和输出异常 |
| JSONUtilityTest | 全局配置、对象树转换、缓冲缓存、验证器和 mixin 管理 |
| JSONMutationRegressionTest | 依据存活变异补充的身份、边界、内存开销和校验策略回归 |
| JSONResourceLifecycleTest | 缓冲归还复用、空输入快速返回的资源开销 |

测试以手工确定的结果、类型、身份、返回值和可观察副作用为断言，不仅检查“不抛异常”。
异常输入、空值和边界值与正常路径一起验证。命名含 `characterization` 的测试专门记录已有缺陷，
不表示这些结果是理想业务契约；修复相应生产逻辑时必须同步更新这些测试。

## 可重复性与构建配置

测试使用内存数据，不访问外部业务服务、不随机抽样、不依赖当前日期，也没有人为等待。
少量配置、线程缓存及安全输入流反射状态均在 `finally` 或 `AfterEach` 恢复。
测试在同一 JVM 内串行运行；PIT 使用 4 个独立 JVM 工作线程。

Java 21 对 `java.io` 的强封装使原项目反射读取 ObjectInputStream 字段的成功路径不可达。
Surefire 与 PIT 均设置 `--add-opens java.base/java.io=ALL-UNNAMED`，以同时验证成功和回退路径。
因此，若比赛平台只接收 Java 文件且不允许此 JVM 参数，不能直接复用这里的 100% 分支结论。

当前受限环境不能写入用户 `.m2`，所以 `.mvn/maven.config` 将仓库放在项目 `.mvn/repository`。
这只是依赖缓存，可从提交物中省略该大目录；Maven 会按需重新下载。
`workspace-repository` profile 使用相对 JaCoCo agent 路径，解决 Windows 中文路径的原生加载失败。
POM 保留原有 Java、JUnit、JaCoCo、PIT 和 Fastjson 版本。

Checkstyle 明确采用 `config/checkstyle-tests.xml`：120 字符行长、显式导入、无无用导入、
括号、空白、修饰符顺序等规则。它允许用于反序列化的公开 Bean 字段，以及独立、直观的期望值字面量。
这与 POM 初始隐式使用的 Sun 默认规则不同；本报告的规范通过是指这套已公开的测试规范。
JaCoCo/PIT 的范围和分母不因规范配置改变。
