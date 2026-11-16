/*
 * # 开发者测试比赛要求
 * 
 * 本题包含在线教育业务、数学表达式引擎，以及表达式答案和考试成绩判定流程。
 * 
 * - 使用 JDK 21、Maven 3.9.6及以上、JUnit 5。
 * - 测试代码只写在`src/test/java/net/mooctest/StudentTest.java`中，不要包含`main()` 方法。
 * - 不要修改`src/main/java`中的被测源代码或`pom.xml`。
 * - 答案提交一份`.java`测试脚本（文件名可修改）及一个项目的`zip`压缩包（压缩要求见后文），判分以最后一次提交的`.java`测试脚本为准，`zip`压缩包仅用作分数异议时的辅助判断。
 * 
 * ## ZIP 提交结构
 * 
 * 提交的 ZIP 内部结构如下。打开 ZIP 后，第一层应直接看到 `pom.xml`、`开发者测试比赛要求.md`、`src/` 和 `target/`，不要额外套一层项目目录。
 * 
 * ```text
 * math-assessment.zip
 * ├── pom.xml
 * ├── 开发者测试比赛要求.md
 * ├── src/
 * │   ├── main/
 * │   │   ├── java/
 * │   │   │   └── net/mooctest/…       # 题目提供的被测代码
 * │   │   └── resources/
 * │   │       └── application.yml
 * │   └── test/
 * │       └── java/
 * │           └── net/
 * │               └── mooctest/
 * │                   └── StudentTest.java
 * └── target/                         # 必须包含以下两套完整报告
 *     ├── pit-reports/
 *     │   ├── index.html              # 必需：PIT 变异测试报告入口
 *     │   └── …                       # 报告的其他文件和子目录
 *     └── site/
 *         └── jacoco/
 *             ├── index.html          # 必需：JaCoCo 覆盖率报告入口
 *             └── …                   # 报告的其他文件和子目录
 * ```
 * 
 * 
 * ## 作弊与违规说明
 * 
 * 依据《开发者测试赛项说明2026-省赛》：
 * 
 * - **作弊或协助他人作弊的选手将被取消比赛资格。**
 * - **重点（第 2 点）：绝对禁止利用评分工具的运行特征，或改变被测程序的执行逻辑，人为提高覆盖率或变异杀死率。** 通过篡改字节码、干预函数返回结果等方式误导 JaCoCo 或 PIT，将被判为作弊，包括但不限于以下行为：
 *     - **违规示例一：根据变异痕迹让测试失败。** 比较运行中的方法指令与原始字节码，仅因检测到指令变化就判定测试失败，即使程序的业务行为没有变化。
 *     - **违规示例二：运行时改写被测程序以改变评分结果。** 临时替换被测方法的实现，使程序进入原实现无法到达的路径，从而获得额外的覆盖或变异杀死记录。
 * 
 * 允许通过字节码解析、插入探针等方式分析未覆盖的分支或未杀死的变异体，但不得用于上述违规改分行为。被测代码可能存在逻辑不可达的情况，**覆盖率或变异杀死率的理论上限可能低于 100%**，不得因此使用违规手段提高分数。
 */
package net.mooctest;

import net.mooctest.grading.expression.ExpressionAnswerGradingService;
import net.mooctest.grading.expression.ExpressionAnswerStatus;
import net.mooctest.grading.expression.ExpressionAnswerSubmission;
import net.mooctest.grading.expression.ExpressionQuestion;
import net.mooctest.grading.expression.MathematicalExpressionEvaluator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StudentTest {
    @Test
    void arithmeticExpressionCanBeGraded() {
        ExpressionQuestion question = new ExpressionQuestion(
                "q1", "math", "(2+3)*4", BigDecimal.TEN, BigDecimal.ZERO, true);
        ExpressionAnswerGradingService service =
                new ExpressionAnswerGradingService(new MathematicalExpressionEvaluator());
        assertEquals(ExpressionAnswerStatus.EXACT,
                service.evaluate(new ExpressionAnswerSubmission(question, "10+10")).status());
    }
}
