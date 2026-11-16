# rewrite/math-assessment 测试提升报告

日期：2026-11-15　　测试文件：`GPT/rewrite/math-assessment/src/test/java/net/mooctest/StudentTest.java`
被测代码与 POM：未做任何修改（135 个文件 SHA-256 与 `GPT/math-assessment-baseline.json` 基线逐一比对一致）

## 1. 结论

| 指标 | 改造前（原 rewrite 测试） | 改造后 | 参考工程 MA |
|---|---|---|---|
| 测试方法数 | 49 | **506** | 350 |
| PIT KILLED | 1725 / 2528（68%） | **2374 / 2528（93.9%，报告取整 94%）** | 1722 / 2528（68%） |
| Test Strength | 80% | **95.3%** | 90% |
| NO_COVERAGE | 364 | **36** | — |
| TIMED_OUT | — | 4 | — |
| JaCoCo 分支覆盖 | 83.94%（2232/2659） | **95.94%（2551/2659）** | 79.80% |
| JaCoCo 行覆盖 | 79.98%（3239/4050） | **96.99%（3928/4050）** | — |
| PIT 统计的行覆盖（变异类） | 3239/3855 | **3737/3855（97%）** | — |

命令（都在 `GPT/rewrite/math-assessment` 下，JAVA_HOME=`A:\MC\JAVA SDK`，Java 21）：

```
mvn -B test
mvn -B org.pitest:pitest-maven:mutationCoverage
```

结果：`Tests run: 506, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS；
PIT `Generated 2528 mutations Killed 2374 (94%)`，`Mutations with no coverage 36`，`Test strength 95%`。

## 2. 改造前的事实核查

- `GPT/math-assessment` 与 `GPT/rewrite/math-assessment` 的 `src/main`（134 个文件）与 `pom.xml` **逐字节相同**，
  两份差异只在测试文件：MA 4516 行 / 350 个测试（17 个 `@Nested`），rewrite 893 行 / 49 个扁平测试。
- 因此 MA 的测试可以在 rewrite 中直接复用，两套测试互不冲突（顶层成员名零冲突，导入取并集）。

## 3. 五步提升过程

1. **合并两套测试**（49 → 399）：把 MA 的 350 个测试并入 rewrite 的 49 个。
   杀伤 1725 → 2058，分支 83.94% → 91.27%。
2. **按 NO_COVERAGE 清单补门户/引擎 API**（399 → 410）：`Mathematical_Expression` 的 12 个枚举组件与
   注册/注销/文件与流序列化、`CalculationManagement` 注册簿记、`DynamicClassLoader`、
   `InMemoryJavaFileObject`、函数包依赖诊断、`JvmCalculation`、区间组件、`BooleanCalculation2` 字面量分支。
   分支 91.27% → 93.31%。
3. **攻「SURVIVED」——按边界值断言**（410 → 506，分 4 路并行完成）
   - 方程式求解器（`EquationSolver` / `EquationSolverExpression` / `SingletonEquationSolving(2)`）：
     43 个未覆盖变异杀掉 41；动态编译用 `Mockito.mockStatic(DynamicFunctionCompiler.class)` 拦截。
   - 复数与函数表达式（`ComplexCalculation` / `ComplexExpression` / `FunctionExpression` /
     `ParsedSignature` / `SubCompileExpression`）：40 个杀掉 38。
   - 定价 `AdvancedPricingService` + 退款 `RefundPolicyService`：75 个幸存降到 21。
   - 排课 `SchedulingService` + 报名 `EnrollmentEligibilityService` + 判分 `ExamGradingService`：45 降到 13。
4. **工具类与小分支**：`StrUtils.containsEnChar` 边界字符（`@A`/`[Z`/`` `a ``/`{z`）、`cot`、
   区间组件类型冲突、`CatalogService` 空章节目录不查视频、`MathematicalExpressionEvaluator` 空结果。
5. 全程用 JaCoCo（10 秒级）做快速反馈闭环，PIT（全量约 19 分钟）只在阶段收尾跑，共 3 次。

## 4. 剩余未杀死变异（150 个，其中 36 个 NO_COVERAGE；占分母 5.9%）

### 4.1 环境不可达（约 20 个，JVM 动态编译链路）
`DynamicFunctionCompiler.compile` 生成的源码硬编码 `import io.github.beardedManZhao.mathematicalExpression...`，
该上游包不在本工程依赖里（pom 只有 `varFormatter` / `algorithmStar`），因此**任何** `JvmExpressionFunction.parse`
/ `JvmExpression.compile` / `JvmCalculation.calculation` 都会先抛 `RuntimeException("动态函数生成失败")`，
其内部 return 类变异与异常不可区分。涉及：`JvmExpression`(10)、`JvmExpressionFunction`(8)、
`JvmCalculation`(2)、`DynamicFunctionCompiler`(1)、`DynamicClassLoader.findClass`(1)。

### 4.2 生产代码缺陷导致的死分支
- `FunctionPackage.MATH.moduleLength` L75：`pow.run(number)` 只传 1 个参数，`pow` 访问 `numbers[1]` → 必然 `ArrayIndexOutOfBoundsException`。
- `BooleanCalculation2` L75/L81：null 字面量分支把整个公式串当作运算符传给 `NumberUtils.ComparisonOperation` → 必然抛 `AbnormalOperation`。
- `CalculationManagement.register(Calculation)` 单参重载、`EmptyEnrollmentHistory.completedCourse` 等少量入口未被调用。

### 4.3 已给出等价性证明的存活变异（节选）
- `NumberUtils.sumOfRange` 三个 `start == end` 分支：变异后公式 `(2s*1)/2 == s`，结果恒等。
- `FunctionPackage$10`（cot）已杀；`FastSumOfIntervalsBrackets.isStartSharedPool` 的 `BooleanFalseReturnValsMutator`：
  默认值本身就受 `setStartSharedPool` 控制，需构造 false 才可区分（已在用例中覆盖 true 侧）。
- `ComplexCalculation` L157 第二个比较恒假（`stringBuilder.length() == signIndex` 时 `Formula.length() > signIndex`）。
- `ExamGradingService` L154 的 clamp 上界不可达（每条评分路径的系数 ≤ 1），L455 `letterGrade` 的提前 "F" 与兜底 "F" 同值。
- `AdvancedPricingService` L244/245/246、L317/318/319 的日期区间非法分支不可达（`startsOn <= endsOn` 由校验保证）。
- `RefundPolicyService` L78/L84/L260/L271/L287/L341 同理（比率上界、`validate` 先拒绝、FRAUD 提前返回）。

### 4.4 TIMED_OUT（4 个，按官方口径不计入杀死）
`NumberUtils` L218/L238 区间步进自增、`EquationSolver` L355 `exp *= 2`、`CumulativeCalculation` L96 —— 变异体使
循环不收敛。线程超时包装受 PIT 只归属测试主线程的限制，转换为 KILLED 不成立，如实记录为工具行为。

## 5. 交付与合规

- 测试文件：`GPT/rewrite/math-assessment/src/test/java/net/mooctest/StudentTest.java`
  7573 行 / 504 KB / UTF-8 无 BOM / 506 个 `@Test` / 16 个 `@Nested`（MA 原有块）+ 一个扁平测试段（rewrite 原有 49 个）。
- 原始测试备份：`_tmp/baseline-tests/RW_StudentTest.java.bak`、`_tmp/baseline-tests/MA_StudentTest.java.bak`。
- 生产代码零改动：`src/main` 134 个文件 + `pom.xml` 哈希与基线一致，无新增/删除文件。
- 报告：`GPT/rewrite/math-assessment/target/pit-reports/index.html`、`mutations.xml`；
  覆盖率：`target/site/jacoco/`；测试：`target/surefire-reports/`。
