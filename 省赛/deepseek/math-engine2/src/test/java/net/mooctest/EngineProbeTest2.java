package net.mooctest;

import net.mooctest.expression.engine.AbnormalOperation;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.Calculation;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.FastSumOfIntervalsBrackets;
import net.mooctest.expression.engine.Function;
import net.mooctest.expression.engine.FunctionExpression;
import net.mooctest.expression.engine.FunctionFormulaCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation2;
import net.mooctest.expression.engine.FunctionPackage;
import net.mooctest.expression.engine.Functions;
import net.mooctest.expression.engine.JvmCalculation;
import net.mooctest.expression.engine.JvmExpression;
import net.mooctest.expression.engine.JvmExpressionFunction;
import net.mooctest.expression.engine.LogResults;
import net.mooctest.expression.engine.ManyToOneNumberFunction;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.NumberCalculation;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PackExpression;
import net.mooctest.expression.engine.PrefixExpression;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.SingletonEquationSolving;
import net.mooctest.expression.engine.SingletonEquationSolvingTwo;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.lingyuzhao.varFormatter.core.VarFormatter;
import top.lingyuzhao.varFormatter.utils.DataObj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B 组（第二半类）表达式引擎探针测试。
 * <p>
 * 覆盖 {@code net.mooctest.expression.engine} 中 B 组 27 个类的公开/可达方法：
 * FastSumOfIntervalsBrackets, Function, FunctionExpression, FunctionFormulaCalculation,
 * FunctionFormulaCalculation2, FunctionPackage, Functions, InMemoryJavaFileObject,
 * JvmCalculation, JvmExpression, JvmExpressionFunction, LogResults, ManyToOneNumberFunction,
 * Mathematical_Expression, NameExpression, NumberCalculation, NumberUtils, PackExpression,
 * ParsedSignature, PrefixExpression, PrefixExpressionOperation, SharedCalculation,
 * SingletonEquationSolving, SingletonEquationSolvingTwo, StrUtils, SubCompileExpression, WrongFormat。
 * <p>
 * 所有断言均使用独立手算常量；不依赖系统时间/时区/locale/随机值。
 */
class EngineProbeTest2 {

    /**
     * 每轮测试都重新注册内置函数包，保证函数名始终存在（注册表为全局静态状态）。
     */
    @BeforeAll
    static void registerBuiltInFunctionPackages() {
        Mathematical_Expression.register_function(FunctionPackage.MATH);
        Mathematical_Expression.register_function(FunctionPackage.DATE);
        Mathematical_Expression.register_function(FunctionPackage.BRANCH);
    }

    @BeforeEach
    void resetGlobalOptionsAndSharedPools() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        final FunctionFormulaCalculation2 shared =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        shared.setStartSharedPool(true);
        shared.clearCache();
    }

    @AfterEach
    void restoreGlobalOptions() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
    }

    // ---------------------------------------------------------------- helpers

    private static ManyToOneNumberFunction mathFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck(name);
    }

    private static ManyToOneNumberFunction dateFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck(name);
    }

    private static ManyToOneNumberFunction branchFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck(name);
    }

    /**
     * 注册一个 BracketsCalculation2 实例，用于触发各组件 getInstance 的"类型不符"分支。
     */
    private static BracketsCalculation2 registeredBracketsProbe() {
        return BracketsCalculation2.getInstance("probeBracketInit");
    }

    /**
     * 调用反射方法并返回其抛出的业务异常（包装异常取 cause）。
     */
    private static Throwable invocationCause(Method method, Object target) {
        try {
            method.invoke(target, true);
            return null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            return e.getCause();
        } catch (ReflectiveOperationException e) {
            return e;
        }
    }

    @Functions({"aa(x)=x+1", "bb(x,y)=x*y"})
    static class ExpressionFunctionHolder {
    }

    @Functions({"kk(x)=x*x"})
    static class JvmFunctionHolder {
    }

    static class PlainHolder {
    }

    /**
     * 可序列化的测试函数，用于 Mathematical_Expression 的序列化注册用例。
     */
    static class ProbeFunction extends ManyToOneNumberFunction {

        ProbeFunction(String name) {
            super(name);
        }

        @Override
        public double run(double... numbers) {
            return numbers[0] + 1.0;
        }
    }

    /**
     * 抽象计算组件的测试实现，用于 NumberCalculation 的可达方法。
     */
    static class ProbeNumberCalculation extends NumberCalculation {

        ProbeNumberCalculation(String name) {
            super(name);
        }

        @Override
        public String formatStr(String string) {
            return string.trim();
        }

        @Override
        public CalculationNumberResults calculation(String Formula, boolean formatRequired) {
            return new CalculationNumberResults(1, 2.5, Formula + ":" + formatRequired);
        }
    }

    // ------------------------------------------------------- StrUtils 组

    @Test
    @DisplayName("StrUtils: 数值解析（含阶乘与 BigDecimal 的 toString/scale）")
    void strUtilsParseNumbers() {
        assertNotNull(new StrUtils());
        assertEquals("abc", StrUtils.removeEmpty("a b c"));
        assertEquals("", StrUtils.removeEmpty("   "));

        assertEquals(12.5, StrUtils.stringToDouble("12.5"), 0.0);
        assertEquals(120.0, StrUtils.stringToDouble("5!"), 0.0);
        // 0 的阶乘走 number <= 1 分支，结果应当是 0。
        assertEquals(0.0, StrUtils.stringToDouble("0!"), 0.0);
        assertEquals(0, BigDecimal.ZERO.compareTo(StrUtils.stringToBigDecimal("0!")));

        final UnsupportedOperationException negative =
                assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToDouble("-3!"));
        assertTrue(negative.getMessage().contains("负数"));

        final BigDecimal scaled = StrUtils.stringToBigDecimal("1.50");
        assertEquals("1.50", scaled.toString());
        assertEquals(2, scaled.scale());
        assertEquals(0, new BigDecimal("24").compareTo(StrUtils.stringToBigDecimal("4!")));
        assertEquals(0, new BigDecimal("7.25").compareTo(StrUtils.stringToBigDecimal("7.25")));
        assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToBigDecimal("-4!"));
    }

    @Test
    @DisplayName("StrUtils: 数值缓存命中路径（Double / BigDecimal）")
    void strUtilsCachePaths() {
        Mathematical_Expression.Options.setUseCache(true);
        try {
            assertEquals(2.5, StrUtils.stringToDouble("2.5"), 0.0);
            // 首次解析会写入缓存。
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("2.5".hashCode()));
            // 注入一个不同的缓存值，解析器必须优先返回缓存值。
            Mathematical_Expression.Options.cacheCalculation("2.5".hashCode(), 99.5);
            assertEquals(99.5, StrUtils.stringToDouble("2.5"), 0.0);

            assertEquals(120.0, StrUtils.stringToDouble("5!"), 0.0);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
            assertEquals(120.0, StrUtils.stringToDouble("5!"), 0.0);

            final BigDecimal first = StrUtils.stringToBigDecimal("2.5");
            final BigDecimal second = StrUtils.stringToBigDecimal("2.5");
            assertSame(first, second);
            assertEquals(2.5, first.doubleValue(), 0.0);
            assertEquals(120.0, StrUtils.stringToBigDecimal("5!").doubleValue(), 0.0);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
            assertEquals(120.0, StrUtils.stringToBigDecimal("5!").doubleValue(), 0.0);

            // BigDecimal 正常路径同样会写缓存。
            assertEquals(0, new BigDecimal("1.50").compareTo(StrUtils.stringToBigDecimal("1.50")));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1.50".hashCode()));
            assertSame(StrUtils.stringToBigDecimal("1.50"),
                    Mathematical_Expression.Options.getCacheCalculation("1.50".hashCode()));
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
    }

    @Test
    @DisplayName("StrUtils: 字符分类与英文/数字探测")
    void strUtilsClassifiers() {
        assertTrue(StrUtils.IsAnOperator('+'));
        assertTrue(StrUtils.IsAnOperator('-'));
        assertTrue(StrUtils.IsAnOperator('*'));
        assertTrue(StrUtils.IsAnOperator('/'));
        assertTrue(StrUtils.IsAnOperator('%'));
        assertTrue(StrUtils.IsAnOperator('^'));
        assertFalse(StrUtils.IsAnOperator('.'));
        assertFalse(StrUtils.IsAnOperator('!'));

        assertTrue(StrUtils.IsANumber('0'));
        assertTrue(StrUtils.IsANumber('9'));
        assertFalse(StrUtils.IsANumber('.'));
        assertTrue(StrUtils.IsANumber("123"));
        assertFalse(StrUtils.IsANumber("12a"));
        assertTrue(StrUtils.IsANumber(""));

        assertTrue(StrUtils.containsNumber("ab1"));
        assertFalse(StrUtils.containsNumber("abc"));
        assertEquals(Character.valueOf('A'), StrUtils.containsEnChar("1A2"));
        assertEquals(Character.valueOf('b'), StrUtils.containsEnChar("1b2"));
        // 字母 ASCII 区间的四个端点字符。
        assertEquals(Character.valueOf('A'), StrUtils.containsEnChar("A"));
        assertEquals(Character.valueOf('Z'), StrUtils.containsEnChar("Z"));
        assertEquals(Character.valueOf('a'), StrUtils.containsEnChar("a"));
        assertEquals(Character.valueOf('z'), StrUtils.containsEnChar("z"));
        assertNull(StrUtils.containsEnChar("123"));
        assertNull(StrUtils.containsEnChar(""));
        assertNull(StrUtils.containsEnChar("[{"));
        assertNull(StrUtils.containsEnChar("@`"));
    }

    @Test
    @DisplayName("StrUtils: 拆分工具与随机串形状")
    void strUtilsSplittersAndRandom() {
        assertEquals(Arrays.asList("a", "b", ""), StrUtils.splitByChar("a,b,", ','));
        assertEquals(Arrays.asList("1", "2", "3"), StrUtils.splitByChar("1, 2 ,3", ','));
        assertEquals(Arrays.asList("a", "(b,c)", "d"), StrUtils.splitByCharWhereNoB("a,(b,c),d", ','));
        assertEquals(Arrays.asList("1"), StrUtils.splitByCharWhereNoB("1", ','));

        final String random = StrUtils.randomString(8);
        assertEquals(8, random.length());
        assertTrue(random.matches("[a-z]+"));
        assertEquals(0, StrUtils.randomString(0).length());
    }

    // ----------------------------------------------------- NumberUtils 组

    @Test
    @DisplayName("NumberUtils: 六种运算、异常模式、阶乘与 deprecated 工具")
    void numberUtilsCalculationAndHelpers() {
        assertEquals(3.0, NumberUtils.calculation('+', 1, 2), 0.0);
        assertEquals(-1.0, NumberUtils.calculation('-', 1, 2), 0.0);
        assertEquals(6.0, NumberUtils.calculation('*', 2, 3), 0.0);
        assertEquals(0.5, NumberUtils.calculation('/', 1, 2), 0.0);
        assertEquals(1.0, NumberUtils.calculation('%', 5, 2), 0.0);
        assertEquals(8.0, NumberUtils.calculation('^', 2, 3), 0.0);

        final AbnormalOperation badMode =
                assertThrows(AbnormalOperation.class, () -> NumberUtils.calculation('&', 1, 2));
        assertTrue(badMode.getMessage().contains("错误的计算模式"));

        assertNotNull(new NumberUtils());
        assertEquals(1, NumberUtils.divideByTen(10));
        assertEquals(2, NumberUtils.divideByTen(25));
        assertEquals(120.0, NumberUtils.factorial(5), 0.0);
        assertEquals(0.0, NumberUtils.factorial(0), 0.0);
        assertEquals(1.0, NumberUtils.factorial(1), 0.0);
        assertEquals(2.0, NumberUtils.factorial(2), 0.0);
    }

    @Test
    @DisplayName("NumberUtils: 操作符优先级比较表")
    void numberUtilsPriorityComparison() {
        assertTrue(NumberUtils.PriorityComparison('+', '*'));
        assertTrue(NumberUtils.PriorityComparison('-', '/'));
        assertTrue(NumberUtils.PriorityComparison('+', '%'));
        assertTrue(NumberUtils.PriorityComparison('-', '^'));
        assertFalse(NumberUtils.PriorityComparison('+', '-'));
        assertFalse(NumberUtils.PriorityComparison('-', '+'));
        assertFalse(NumberUtils.PriorityComparison('*', '+'));
        assertFalse(NumberUtils.PriorityComparison('*', '/'));
        assertFalse(NumberUtils.PriorityComparison('(', ')'));
    }

    @Test
    @DisplayName("NumberUtils: 比较运算与非法运算符")
    void numberUtilsComparisonOperation() {
        assertTrue(NumberUtils.ComparisonOperation("<", 1, 2));
        assertFalse(NumberUtils.ComparisonOperation("<", 2, 1));
        assertTrue(NumberUtils.ComparisonOperation(">", 2, 1));
        assertFalse(NumberUtils.ComparisonOperation(">", 1, 2));
        assertFalse(NumberUtils.ComparisonOperation(">", 1, 1));
        assertFalse(NumberUtils.ComparisonOperation("<", 1, 1));
        assertTrue(NumberUtils.ComparisonOperation("<=", 1, 1));
        assertFalse(NumberUtils.ComparisonOperation("<=", 2, 1));
        assertTrue(NumberUtils.ComparisonOperation(">=", 1, 1));
        assertTrue(NumberUtils.ComparisonOperation("=", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation("==", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation("!=", 2, 3));
        assertTrue(NumberUtils.ComparisonOperation("<>", 2, 3));
        assertFalse(NumberUtils.ComparisonOperation("!=", 2, 2));

        final AbnormalOperation bad =
                assertThrows(AbnormalOperation.class, () -> NumberUtils.ComparisonOperation("?!", 1, 2));
        assertTrue(bad.getMessage().contains("Bad comparison operator"));
    }

    @Test
    @DisplayName("NumberUtils: 区间求和（int/double/步长/字符串形式）")
    void numberUtilsSumOfRange() {
        assertEquals(10, NumberUtils.sumOfRange(1, 4));
        assertEquals(5, NumberUtils.sumOfRange(5, 5));
        assertEquals(10.0, NumberUtils.sumOfRange(1.0, 4.0), 0.0);
        assertEquals(5.0, NumberUtils.sumOfRange(5.0, 5.0), 0.0);
        assertEquals(10.0, NumberUtils.sumOfRange(1.0, 4.0, 1.0), 0.0);
        assertEquals(5.0, NumberUtils.sumOfRange(5.0, 5.0, 2.0), 0.0);
        assertEquals(25.0, NumberUtils.sumOfRange(1.0, 10.0, 2.0), 0.0);
        // 起点非 1，用于区分 n*start 与 n/start 以及 n*start+... 的算术变异。
        assertEquals(30.0, NumberUtils.sumOfRange(2.0, 10.0, 2.0), 0.0);
        // 步长不能整除区间长度，用于区分 abs=end-start 与 end+start。
        assertEquals(12.0, NumberUtils.sumOfRange(1.0, 8.0, 3.0), 0.0);

        assertEquals("(1.0+4.0)*4.0/2", NumberUtils.sumOfRangeString(1.0, 4.0));
        assertEquals("5.0", NumberUtils.sumOfRangeString(5.0, 5.0));
        assertEquals("(1.0+4.0)*4.0/2", NumberUtils.sumOfRangeString(1.0, 4.0, 1.0));
        assertEquals("5.0", NumberUtils.sumOfRangeString(5.0, 5.0, 2.0));
        assertEquals("5.0*1.0+5.0*(5.0-1)*2.0/2", NumberUtils.sumOfRangeString(1.0, 10.0, 2.0));
        assertEquals("3.0*1.0+3.0*(3.0-1)*3.0/2", NumberUtils.sumOfRangeString(1.0, 8.0, 3.0));
    }

    @Test
    @DisplayName("NumberUtils: 区间累乘（double/字符串形式）")
    void numberUtilsMultiplyOfRange() {
        assertEquals(24.0, NumberUtils.MultiplyOfRange(1, 4, 1), 0.0);
        assertEquals(2.0, NumberUtils.MultiplyOfRange(2, 2, 1), 0.0);
        assertEquals(15.0, NumberUtils.MultiplyOfRange(1, 5, 2), 0.0);
        // 步长不能整除区间长度：尾部边界需要向下取整到 3。
        assertEquals(3.0, NumberUtils.MultiplyOfRange(1, 4, 2), 0.0);
        // 与 1 不同的起点，用于区分 % 运算中的 end-start 与 end+start。
        assertEquals(10.0, NumberUtils.MultiplyOfRange(2, 6, 3), 0.0);

        assertEquals("24.0", NumberUtils.MultiplyOfRangeString(1, 4, 1));
        assertEquals("2.0", NumberUtils.MultiplyOfRangeString(2, 2, 1));
        assertEquals("15.0", NumberUtils.MultiplyOfRangeString(1, 5, 2));
    }

    // ---------------------------------------------------------- WrongFormat

    @Test
    @DisplayName("WrongFormat: 四个构造器与 message/cause")
    void wrongFormatConstructors() {
        final WrongFormat noMessage = new WrongFormat();
        assertNull(noMessage.getMessage());
        final WrongFormat withMessage = new WrongFormat("格式错误");
        assertEquals("格式错误", withMessage.getMessage());

        final Throwable cause = new IllegalStateException("root");
        final WrongFormat withBoth = new WrongFormat("包装", cause);
        assertEquals("包装", withBoth.getMessage());
        assertSame(cause, withBoth.getCause());

        final WrongFormat onlyCause = new WrongFormat(cause);
        assertSame(cause, onlyCause.getCause());
        assertNotNull(onlyCause.getMessage());
    }

    // ----------------------------------------- ManyToOneNumberFunction

    @Test
    @DisplayName("ManyToOneNumberFunction: 名称校验、默认名、toString、可序列化判定")
    void manyToOneNumberFunctionContract() {
        final ManyToOneNumberFunction function = new ManyToOneNumberFunction("probeMTF") {
            @Override
            public double run(double... numbers) {
                return numbers.length == 0 ? -1.0 : numbers[0];
            }
        };
        assertEquals("probeMTF", function.getName());
        assertEquals("probeMTF", function.toString());
        assertTrue(function.AllowSerialization());
        assertEquals(5.0, function.run(5.0), 0.0);

        assertThrows(ExtractException.class, () -> new ManyToOneNumberFunction("bad name") {
            @Override
            public double run(double... numbers) {
                return 0;
            }
        });
        assertThrows(ExtractException.class, () -> new ManyToOneNumberFunction("name1") {
            @Override
            public double run(double... numbers) {
                return 0;
            }
        });

        final ManyToOneNumberFunction defaultNamed = new ManyToOneNumberFunction() {
            @Override
            public double run(double... numbers) {
                return 1.0;
            }
        };
        assertEquals("f", defaultNamed.getName());
    }

    // ------------------------------------------------------ FunctionPackage

    @Test
    @DisplayName("FunctionPackage: MATH 基础函数（sum/pow/sqrt/factorial/三角）")
    void functionPackageMathBasicFunctions() {
        assertEquals(6.0, mathFn("sum").run(1, 2, 3), 0.0);
        assertEquals(0.0, mathFn("sum").run(), 0.0);
        assertEquals(8.0, mathFn("pow").run(2, 3), 0.0);
        assertEquals(3.0, mathFn("sqrt").run(9), 0.0);
        assertEquals(120.0, mathFn("factorial").run(5), 0.0);
        assertEquals(0.0, mathFn("factorial").run(0), 0.0);

        assertEquals(0.0, mathFn("sin").run(0), 0.0);
        assertEquals(1.0, mathFn("cos").run(0), 0.0);
        assertEquals(0.0, mathFn("tan").run(0), 0.0);
        assertEquals(1.0 / Math.tan(1.0), mathFn("cot").run(1.0), 1e-15);
        assertEquals(1.0, mathFn("sec").run(0), 0.0);
        assertEquals(1.0 / Math.sin(1.0), mathFn("csc").run(1.0), 1e-15);
    }

    @Test
    @DisplayName("FunctionPackage: avg 依赖 sum；moduleLength 的 pow 单参缺陷")
    void functionPackageMathModuleLengthAndAvg() {
        assertEquals(4.0, mathFn("avg").run(2, 4, 6), 0.0);
        assertEquals(2.0, mathFn("avg").run(2), 0.0);

        // moduleLength 内部以单参数调用 pow，而 pow 需要两个参数，因此必然越界。
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> mathFn("moduleLength").run(3, 4));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> mathFn("moduleLength").run(5));
        // 空参数时不会进入 pow 调用，直接对 0 求平方根。
        assertEquals(0.0, mathFn("moduleLength").run(), 0.0);
    }

    @Test
    @DisplayName("FunctionPackage: DATE 全部 14 个换算函数")
    void functionPackageDateFunctions() {
        assertEquals(1.5, dateFn("msToS").run(1500), 0.0);
        assertEquals(2000.0, dateFn("sToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToMin").run(120000), 0.0);
        assertEquals(120000.0, dateFn("minToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToHour").run(7200000), 0.0);
        assertEquals(7200000.0, dateFn("hourToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToDay").run(172800000), 0.0);
        assertEquals(172800000.0, dateFn("dayToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToWeek").run(1209600000), 0.0);
        assertEquals(1209600000.0, dateFn("weekToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToMonth").run(5184000000L), 0.0);
        assertEquals(5184000000.0, dateFn("monthToMs").run(2), 0.0);
        assertEquals(2.0, dateFn("msToYear").run(63072000000L), 0.0);
        assertEquals(63072000000.0, dateFn("yearToMs").run(2), 0.0);
    }

    @Test
    @DisplayName("FunctionPackage: BRANCH 五个 if 函数的真/假与参数个数校验")
    void functionPackageBranchFunctions() {
        assertEquals(10.0, branchFn("ifEq").run(1, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifEq").run(1, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifGt").run(2, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifGt").run(1, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifLt").run(1, 2, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifLt").run(2, 1, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifGe").run(1, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifGe").run(1, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifLe").run(1, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifLe").run(2, 1, 10, 20), 0.0);

        final UnsupportedOperationException arity = assertThrows(UnsupportedOperationException.class,
                () -> branchFn("ifEq").run(1, 2, 3));
        assertTrue(arity.getMessage().contains("参数个数必须为4"));
        assertThrows(UnsupportedOperationException.class, () -> branchFn("ifGt").run(1, 2, 3));
        assertThrows(UnsupportedOperationException.class, () -> branchFn("ifLt").run(1, 2, 3));
        assertThrows(UnsupportedOperationException.class, () -> branchFn("ifGe").run(1, 2, 3));
        assertThrows(UnsupportedOperationException.class, () -> branchFn("ifLe").run(1, 2, 3));
    }

    @Test
    @DisplayName("FunctionPackage: 枚举注册表 API")
    void functionPackageRegistryApi() {
        assertEquals(3, FunctionPackage.values().length);
        assertEquals(FunctionPackage.MATH, FunctionPackage.valueOf("MATH"));
        assertEquals("Math", FunctionPackage.MATH.getName());
        assertEquals("Date", FunctionPackage.DATE.getName());
        assertEquals("Branch", FunctionPackage.BRANCH.getName());
        assertNotNull(FunctionPackage.MATH.getFunctionByNameNoCheck("sin"));
        assertNull(FunctionPackage.MATH.getFunctionByNameNoCheck("noSuchFunction"));
        assertEquals(12, FunctionPackage.MATH.getFunctionMap().size());
        assertEquals(14, FunctionPackage.DATE.getFunctionMap().size());
        assertEquals(5, FunctionPackage.BRANCH.getFunctionMap().size());
        assertEquals(FunctionPackage.MATH.getFunctionByNameNoCheck("sin"),
                FunctionPackage.MATH.getFunctionMap().get("sin"));
    }

    @Test
    @DisplayName("FunctionPackage: 缺少依赖函数时的 UnsupportedOperationException 导航信息")
    void functionPackageMissingDependencyMessages() {
        try {
            assertTrue(Mathematical_Expression.unregister_function("sum"));
            final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("avg").run(1, 2));
            assertTrue(ex.getMessage().contains("函数位于 Math 函数包中"));
            assertTrue(ex.getMessage().contains("double[]"));
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
        try {
            assertTrue(Mathematical_Expression.unregister_function("pow"));
            final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("moduleLength").run(3, 4));
            assertTrue(ex.getMessage().contains("pow"));
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
        try {
            assertTrue(Mathematical_Expression.unregister_function("sqrt"));
            final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("moduleLength").run(3, 4));
            assertTrue(ex.getMessage().contains("sqrt"));
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
    }

    // -------------------------------------------- Mathematical_Expression

    @Test
    @DisplayName("Mathematical_Expression: 枚举 12 个组件的实例化与复用")
    void mathematicalExpressionEnumInstances() {
        assertEquals(12, Mathematical_Expression.values().length);
        for (Mathematical_Expression component : Mathematical_Expression.values()) {
            // 强制走"未注册 -> 新建"分支，避免共享静态注册表掩盖该分支。
            CalculationManagement.unregister(component.name());
            final Calculation first = Mathematical_Expression.getInstance(component);
            assertNotNull(first);
            assertEquals(component.name(), first.getName());
            assertSame(first, Mathematical_Expression.getInstance(component));
        }
        assertEquals(Mathematical_Expression.jvmCalculation,
                Mathematical_Expression.valueOf("jvmCalculation"));

        final Calculation custom = Mathematical_Expression.getInstance(
                Mathematical_Expression.jvmCalculation, "probeMEJvm");
        assertTrue(custom instanceof JvmCalculation);
        assertEquals("probeMEJvm", custom.getName());
        assertSame(custom, Mathematical_Expression.getInstance(Mathematical_Expression.jvmCalculation, "probeMEJvm"));
        assertNotNull(Mathematical_Expression.getInstance(Mathematical_Expression.fastSumOfIntervalsBrackets));
    }

    @Test
    @DisplayName("Mathematical_Expression: 函数注册表查询与注销")
    void mathematicalExpressionRegistryApi() {
        assertTrue(CalculationManagement.isRegister(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME));
        assertNotNull(CalculationManagement.getCalculationByName(
                CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME));
        assertTrue(CalculationManagement.isFunctionExist("sin"));
        final Set<String> names = Mathematical_Expression.getFunctionMap();
        assertTrue(names.contains("sin"));
        assertTrue(names.contains("cos"));
        assertNotNull(Mathematical_Expression.getFunction("sin"));
        assertEquals("sin", Mathematical_Expression.getFunction("sin").getName());
        assertThrows(ExtractException.class, () -> Mathematical_Expression.getFunction("no_such_function_xyz"));

        assertTrue(Mathematical_Expression.register_function(new ProbeFunction("probeUnregFn")));
        assertTrue(Mathematical_Expression.unregister_function("probeUnregFn"));
        assertFalse(Mathematical_Expression.unregister_function("probeUnregFn"));

        final ProbeFunction function = new ProbeFunction("probeUnregFnTwo");
        assertTrue(Mathematical_Expression.register_function(function));
        assertEquals(4.0, Mathematical_Expression.getFunction("probeUnregFnTwo").run(3.0), 0.0);
        assertTrue(Mathematical_Expression.unregister_function(function));
        assertFalse(CalculationManagement.isFunctionExist("probeUnregFnTwo"));
    }

    @Test
    @DisplayName("Mathematical_Expression: 重复注册/注销返回值与 JVM 注册表写回")
    void mathematicalExpressionRegistrationEdgeCases() throws Exception {
        assertTrue(Mathematical_Expression.register_function(new ProbeFunction("probeDupFnA")));
        // 同名函数二次注册必须返回 false。
        assertFalse(Mathematical_Expression.register_function(new ProbeFunction("probeDupFnA")));
        assertTrue(Mathematical_Expression.unregister_function("probeDupFnA"));
        // 未注册函数的注销必须返回 false。
        assertFalse(Mathematical_Expression.unregister_function(new ProbeFunction("probeNeverFnB")));

        // 字符串重载必须真正写入 JVM 注册表。
        JvmExpressionFunction.getRegisteredMethods().remove("double jj");
        Mathematical_Expression.register_jvm_function("jj(x)=x*x");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double jj"));

        // 注解类重载必须遍历注解中的每一个函数表达式。
        JvmExpressionFunction.getRegisteredMethods().remove("double kk");
        assertTrue(Mathematical_Expression.register_jvm_function(JvmFunctionHolder.class));
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double kk"));

        // 字符串数学函数重载：同名二次注册必须返回 false。
        assertTrue(Mathematical_Expression.register_function("probeDupFnC(x)=x"));
        assertFalse(Mathematical_Expression.register_function("probeDupFnC(x)=x"));
        assertTrue(Mathematical_Expression.unregister_function("probeDupFnC"));
    }

    @Test
    @DisplayName("Mathematical_Expression: 数学函数表达式的注册与执行")
    void mathematicalExpressionRegisterExpressionFunctions() throws Exception {
        assertTrue(Mathematical_Expression.register_function(ExpressionFunctionHolder.class));
        assertTrue(CalculationManagement.isFunctionExist("aa"));
        assertTrue(CalculationManagement.isFunctionExist("bb"));
        assertEquals(3.0, Mathematical_Expression.getFunction("aa").run(2.0), 1e-12);
        assertEquals(6.0, Mathematical_Expression.getFunction("bb").run(2.0, 3.0), 1e-12);
        assertEquals(2, ExpressionFunctionHolder.class.getAnnotation(Functions.class).value().length);

        assertFalse(Mathematical_Expression.register_function(PlainHolder.class));
        assertFalse(Mathematical_Expression.register_jvm_function(PlainHolder.class));

        assertThrows(UnsupportedOperationException.class,
                () -> Mathematical_Expression.register_function("not a function"));
        final WrongFormat wrong = assertThrows(WrongFormat.class,
                () -> Mathematical_Expression.register_function("cc(x)=x+"));
        assertNotNull(wrong.getMessage());
    }

    @Test
    @DisplayName("Mathematical_Expression: JVM 函数注册（字符串与注解类）")
    void mathematicalExpressionRegisterJvmFunctions() {
        JvmExpressionFunction.getRegisteredMethods().remove("double jj");
        Mathematical_Expression.register_jvm_function("jj(x)=x*x");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double jj"));

        JvmExpressionFunction.getRegisteredMethods().remove("double kk");
        assertTrue(Mathematical_Expression.register_jvm_function(JvmFunctionHolder.class));
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double kk"));
        assertFalse(Mathematical_Expression.register_jvm_function(String.class));
    }

    @Test
    @DisplayName("Mathematical_Expression: 函数对象的序列化保存/文件与流注册")
    void mathematicalExpressionSerialization(@TempDir Path tempDir) throws Exception {
        final ProbeFunction function = new ProbeFunction("probeSerializedFn");
        final Path file = tempDir.resolve("probe-function.ser");
        Mathematical_Expression.saveFunction(function, file.toFile());
        assertTrue(file.toFile().isFile());
        assertTrue(file.toFile().length() > 0);

        // 文件注册成功并真正可用。
        assertTrue(Mathematical_Expression.register_function(file.toFile()));
        assertEquals(4.0, Mathematical_Expression.getFunction("probeSerializedFn").run(3.0), 0.0);
        // 重复注册同名函数返回 false。
        assertTrue(Mathematical_Expression.register_function(file.toFile()) == false);

        // 文件内不是 Function 时返回 false。
        final Path wrongFile = tempDir.resolve("not-a-function.ser");
        try (ObjectOutputStream out = new ObjectOutputStream(java.nio.file.Files.newOutputStream(wrongFile))) {
            out.writeObject("not a function");
        }
        assertFalse(Mathematical_Expression.register_function(wrongFile.toFile()));

        // 通过输出流保存，再通过输入流注册（混合 Function / 非 Function 计数）。
        final ProbeFunction streamFn = new ProbeFunction("probeStreamFn");
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Mathematical_Expression.saveFunction(buffer, streamFn);
        final Map.Entry<Integer, Integer> saved = Mathematical_Expression.register_function(
                new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals(1, saved.getKey().intValue());
        assertEquals(0, saved.getValue().intValue());

        final ProbeFunction mixed = new ProbeFunction("probeMixedFn");
        final ByteArrayOutputStream mixedBuffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(mixedBuffer)) {
            out.writeObject(mixed);
            out.writeObject("still not a function");
            out.writeObject(new ProbeFunction("probeMixedFn"));
        }
        final Map.Entry<Integer, Integer> mixedResult = Mathematical_Expression.register_function(
                new ByteArrayInputStream(mixedBuffer.toByteArray()));
        assertEquals(1, mixedResult.getKey().intValue());
        assertEquals(2, mixedResult.getValue().intValue());

        assertEquals(11.0, Mathematical_Expression.getFunction("probeStreamFn").run(10.0), 0.0);
        assertTrue(Mathematical_Expression.unregister_function("probeStreamFn"));
        assertTrue(Mathematical_Expression.unregister_function("probeMixedFn"));
        assertTrue(Mathematical_Expression.unregister_function("probeSerializedFn"));
    }

    @Test
    @DisplayName("Mathematical_Expression.Options: 缓存读写与两个开关（含清理）")
    void mathematicalExpressionOptions() {
        assertNotNull(new Mathematical_Expression.Options());
        Mathematical_Expression.Options.setUseBigDecimal(true);
        assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
        Mathematical_Expression.Options.setUseBigDecimal(false);
        assertFalse(Mathematical_Expression.Options.isUseBigDecimal());

        Mathematical_Expression.Options.setUseCache(true);
        assertTrue(Mathematical_Expression.Options.isUseCache());
        Mathematical_Expression.Options.cacheCalculation(20260101, 5.5);
        assertEquals(5.5, Mathematical_Expression.Options.getCacheCalculation(20260101).doubleValue(), 0.0);
        final PackExpression packed = new PackExpression(null,
                PrefixExpression.compile("1+2+0", "probeOptsInner"), "probeOpts");
        Mathematical_Expression.Options.cacheCalculation("probe-key", packed);
        assertSame(packed, Mathematical_Expression.Options.getCacheCalculation("probe-key"));
        assertNull(Mathematical_Expression.Options.getCacheCalculation(20260102));

        Mathematical_Expression.Options.setUseCache(false);
        assertFalse(Mathematical_Expression.Options.isUseCache());
        assertNull(Mathematical_Expression.Options.getCacheCalculation(20260101));
        assertNull(Mathematical_Expression.Options.getCacheCalculation("probe-key"));
    }

    // ------------------------------------------------- NameExpression/NumberCalculation

    @Test
    @DisplayName("NameExpression: 缓存、可用状态与 calculationCache 生命周期")
    void nameExpressionCacheLifecycle() {
        final NameExpression expression = new NameExpression("1+2*3", "probeNE") {
            @Override
            public boolean isBigDecimal() {
                return false;
            }

            @Override
            public boolean isUnBigDecimal() {
                return true;
            }

            @Override
            public void convertToMultiPrecisionSupported() {
                // 测试桩：无多精度形态
            }

            @Override
            public CalculationResults calculation(boolean isCopy) {
                return new CalculationNumberResults(2, 7.0, "inner");
            }

            @Override
            public CalculationResults calculationBigDecimals(boolean isCopy) {
                return new CalculationNumberResults(2, 7.0, "inner-bd");
            }
        };

        assertEquals("1+2*3", expression.getExpressionStr());
        assertEquals("probeNE", expression.getCalculationName());
        assertNull(expression.getCache());
        assertTrue(expression.isAvailable());

        final CalculationResults first = expression.calculationCache(true);
        assertEquals(7.0, ((CalculationNumberResults) first).getResult().doubleValue(), 0.0);
        assertSame(first, expression.getCache());
        assertTrue(expression.isAvailable());

        final CalculationResults cachedAgain = expression.calculationCache(true);
        assertSame(first, cachedAgain);

        expression.calculationCache(false);
        assertFalse(expression.isAvailable());

        final CalculationNumberResults replaced = new CalculationNumberResults(0, 1.0, "replaced");
        expression.setCache(replaced);
        assertSame(replaced, expression.getCache());
    }

    @Test
    @DisplayName("NumberCalculation: 格式检查各分支与 calculation 重载")
    void numberCalculationCheckAndCalculation() throws Exception {
        final ProbeNumberCalculation calculation = new ProbeNumberCalculation("probeNC");
        assertEquals("probeNC", calculation.getName());

        final CalculationNumberResults result = calculation.calculation("2*3");
        assertEquals(2.5, result.getResult().doubleValue(), 0.0);
        assertEquals("2*3:true", result.getCalculationSourceName());
        assertEquals(1, result.getResultLayers());

        calculation.check("1+2");
        calculation.check("1+2 ");
        calculation.check("3!");
        calculation.check("1+(2*3)");
        calculation.check("1+(2)*3");
        calculation.check("1+-2");
        calculation.check("1++2");
        calculation.check("1");

        final WrongFormat empty = assertThrows(WrongFormat.class, () -> calculation.check(""));
        assertTrue(empty.getMessage().contains("空字符串"));

        final WrongFormat tailOperator = assertThrows(WrongFormat.class, () -> calculation.check("1+"));
        assertTrue(tailOperator.getMessage().contains("最后一个字符不是一个数值"));

        final WrongFormat badBrackets = assertThrows(WrongFormat.class, () -> calculation.check("((1+2)"));
        assertTrue(badBrackets.getMessage().contains("括号"));
        // 括号差值为 1：格式化消息中必须出现 "[1]"。
        assertTrue(badBrackets.getMessage().contains("[1]"));

        final WrongFormat unknownSymbol = assertThrows(WrongFormat.class, () -> calculation.check("1a+2"));
        assertTrue(unknownSymbol.getMessage().contains("未知符号"));

        final WrongFormat doubleOperator = assertThrows(WrongFormat.class, () -> calculation.check("1+*2"));
        assertTrue(doubleOperator.getMessage().contains("多出了一个运算符"));
    }

    // ------------------------------------------------------------ LogResults

    @Test
    @DisplayName("LogResults: 结果读写、不支持方法、格式化器解释")
    void logResultsBehaviour() {
        final LogResults results = new LogResults("probeLog");
        assertNull(results.getResult());
        results.setResult(7.5);
        assertEquals(7.5, results.getResult());
        assertEquals("probeLog", results.getCalculationSourceName());

        assertThrows(UnsupportedOperationException.class, results::getResultLayers);
        assertThrows(UnsupportedOperationException.class, () -> results.setSource("other"));

        final String explained = results.explain(VarFormatter.JSON);
        assertNotNull(explained);
        assertFalse(explained.isEmpty());
    }

    // --------------------------------------------------------- PackExpression

    @Test
    @DisplayName("PackExpression: 包装/拆包/多精度转换与来源改写")
    void packExpressionWrapping() {
        final PrefixExpression inner = PrefixExpression.compile("1+2+0", "probePackInner");
        final PackExpression packed = new PackExpression("alias", inner, "probePack");
        assertEquals("alias -> 1+2+0 -> 3.0+0", packed.getExpressionStr());
        assertEquals("probePack:{probePackInner}", packed.getCalculationName());
        assertFalse(packed.isBigDecimal());
        assertTrue(packed.isUnBigDecimal());
        assertSame(inner, packed.dismantling());

        final CalculationResults result = packed.calculation(true);
        assertEquals(3.0, ((CalculationNumberResults) result).getResult().doubleValue(), 0.0);
        assertEquals("probePack:{probePackInner}", result.getCalculationSourceName());

        final PackExpression anonymousName = new PackExpression(null,
                PrefixExpression.compile("2*3+0", "probePackInner2"), "probePack2");
        assertEquals("2*3+0 -> 6.0+0", anonymousName.getExpressionStr());

        final PrefixExpression innerBigDecimal =
                PrefixExpression.compileBigDecimal("1.5+2.25+0", "probePackInnerBD");
        final PackExpression packedBigDecimal = new PackExpression("bd", innerBigDecimal, "probePackBD");
        assertTrue(packedBigDecimal.isBigDecimal());
        assertFalse(packedBigDecimal.isUnBigDecimal());
        final CalculationResults bigDecimalResult = packedBigDecimal.calculationBigDecimals(true);
        assertEquals(3.75, ((CalculationNumberResults) bigDecimalResult).getResult().doubleValue(), 0.0);
        assertEquals("probePackBD:{probePackInnerBD}", bigDecimalResult.getCalculationSourceName());

        final PackExpression doublePacked = new PackExpression("c",
                PrefixExpression.compile("1+1+0", "probePackInner3"), "probePack3");
        assertFalse(doublePacked.isBigDecimal());
        doublePacked.convertToMultiPrecisionSupported();
        assertTrue(doublePacked.isBigDecimal());
        assertTrue(doublePacked.isUnBigDecimal());
    }

    // ------------------------------------------------------- PrefixExpression

    @Test
    @DisplayName("PrefixExpression: isBackIsOpt 静态字符归类")
    void prefixExpressionIsBackIsOpt() {
        final StringBuilder builder = new StringBuilder();
        PrefixExpression.isBackIsOpt(builder, '7');
        PrefixExpression.isBackIsOpt(builder, '!');
        PrefixExpression.isBackIsOpt(builder, '.');
        PrefixExpression.isBackIsOpt(builder, '-');
        PrefixExpression.isBackIsOpt(builder, '+');
        PrefixExpression.isBackIsOpt(builder, 'x');
        assertEquals("7!.-", builder.toString());
    }

    @Test
    @DisplayName("PrefixExpression: 非精度编译、运算优先级折叠、栈与表达式串")
    void prefixExpressionDoubleCompilation() {
        final PrefixExpression expression = PrefixExpression.compile("1+2*3+0", "probePE");
        assertTrue(expression.isUnBigDecimal());
        assertFalse(expression.isBigDecimal());

        assertEquals(7.0, expression.calculation(true).getResult().doubleValue(), 0.0);
        assertEquals(2, expression.calculation(true).getResultLayers());
        assertEquals(3, expression.getDoubleStack(true).size());
        assertEquals(3, expression.getDoubleStack(false).size());
        assertEquals(2, expression.getCharacterStack(true).size());
        assertEquals(2, expression.getCharacterStack(false).size());
        assertEquals("1+2*3+0 -> 1.0+6.0+0", expression.getExpressionStr());
        assertThrows(UnsupportedOperationException.class, () -> expression.calculationBigDecimals(true));
    }

    @Test
    @DisplayName("PrefixExpression: BigDecimal 编译、精度分支与表达式串")
    void prefixExpressionBigDecimalCompilation() {
        final PrefixExpression expression = PrefixExpression.compileBigDecimal("1+2*3+0", "probePEBD");
        assertTrue(expression.isBigDecimal());
        assertFalse(expression.isUnBigDecimal());

        assertEquals(7.0, expression.calculationBigDecimals(true).getResult().doubleValue(), 0.0);
        assertEquals(2, expression.calculationBigDecimals(true).getResultLayers());
        assertNotNull(expression.getBigDecimalsR(true));
        // isCopy=true 必须返回副本：修改返回值不能影响内部栈。
        final Stack<BigDecimal> numberCopy = expression.getBigDecimalsR(true);
        final Stack<Character> operatorCopy = expression.getCharacterStack(true);
        final int numberSize = numberCopy.size();
        final int operatorSize = operatorCopy.size();
        numberCopy.pop();
        operatorCopy.pop();
        assertEquals(numberSize, expression.getBigDecimalsR(false).size());
        assertEquals(operatorSize, expression.getCharacterStack(false).size());
        assertEquals("1+2*3+0 -> 1+6+0", expression.getExpressionStr());
        assertThrows(UnsupportedOperationException.class, () -> expression.calculation(true));
    }

    @Test
    @DisplayName("PrefixExpression: 直接构造（double 栈）与两种精度互转")
    void prefixExpressionDirectStacks() {
        final Stack<Double> numberStack = new Stack<>();
        numberStack.push(3.0);
        numberStack.push(4.0);
        final Stack<Character> operatorStack = new Stack<>();
        operatorStack.push('+');
        final PrefixExpression expression =
                new PrefixExpression(null, numberStack, operatorStack, "3+4", "probePEDirect");
        assertEquals("probePEDirect", expression.getCalculationName());
        assertTrue(expression.isUnBigDecimal());
        assertFalse(expression.isBigDecimal());
        assertEquals(7.0, expression.calculation(true).getResult().doubleValue(), 0.0);
        assertEquals(1, expression.calculation(true).getResultLayers());
        assertEquals("3+4 -> 3.0+0", expression.getExpressionStr());

        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        assertEquals("3+4 -> 3+0", expression.getExpressionStr());
        assertEquals(7.0, expression.calculationBigDecimals(true).getResult().doubleValue(), 0.0);

        // 已经同时支持两种精度，再次转换直接返回。
        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
    }

    @Test
    @DisplayName("PrefixExpression: 直接构造（BigDecimal 栈）与反向精度互转")
    void prefixExpressionBigDecimalStacks() {
        final Stack<BigDecimal> numberStack = new Stack<>();
        numberStack.push(new BigDecimal("1.5"));
        numberStack.push(new BigDecimal("2.25"));
        final Stack<Character> operatorStack = new Stack<>();
        operatorStack.push('+');
        final PrefixExpression expression =
                new PrefixExpression(numberStack, null, operatorStack, "1.5+2.25", "probePEBDDirect");
        assertTrue(expression.isBigDecimal());
        assertFalse(expression.isUnBigDecimal());
        assertThrows(UnsupportedOperationException.class, () -> expression.calculation(true));
        assertEquals(3.75, expression.calculationBigDecimals(true).getResult().doubleValue(), 0.0);
        assertEquals("1.5+2.25 -> 1.5+0", expression.getExpressionStr());

        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isUnBigDecimal());
        assertTrue(expression.isBigDecimal());
        assertEquals(3.75, expression.calculation(true).getResult().doubleValue(), 0.0);

        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
    }

    @Test
    @DisplayName("PrefixExpression: 精度互转的幂等性（已存在的栈不会被替换）")
    void prefixExpressionPrecisionConversionIdentity() throws Exception {
        final Stack<BigDecimal> numberStack = new Stack<>();
        numberStack.push(new BigDecimal("1.5"));
        numberStack.push(new BigDecimal("2.25"));
        final Stack<Character> operatorStack = new Stack<>();
        operatorStack.push('+');
        final PrefixExpression expression =
                new PrefixExpression(numberStack, null, operatorStack, "1.5+2.25", "probePEConv");
        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isUnBigDecimal());

        final Field doubleStackField = PrefixExpression.class.getDeclaredField("doubleStackR");
        doubleStackField.setAccessible(true);
        final Object convertedStack = doubleStackField.get(expression);
        assertNotNull(convertedStack);
        expression.convertToMultiPrecisionSupported();
        assertSame(convertedStack, doubleStackField.get(expression));
    }

    @Test
    @DisplayName("PrefixExpression: 不可用状态下的栈访问异常")
    void prefixExpressionUnavailableBranches() {
        final PrefixExpression doubleExpression = PrefixExpression.compile("1+2+0", "probePEU");
        doubleExpression.calculationCache(false);
        assertFalse(doubleExpression.isAvailable());
        assertThrows(UnsupportedOperationException.class, () -> doubleExpression.getDoubleStack(true));
        assertThrows(UnsupportedOperationException.class, () -> doubleExpression.getCharacterStack(true));

        final PrefixExpression bigDecimalExpression =
                PrefixExpression.compileBigDecimal("1+2+0", "probePEUBD");
        bigDecimalExpression.calculationBigDecimalsCache(false);
        assertFalse(bigDecimalExpression.isAvailable());
        assertThrows(UnsupportedOperationException.class,
                () -> bigDecimalExpression.getBigDecimalsR(true));
        assertThrows(UnsupportedOperationException.class,
                () -> bigDecimalExpression.getCharacterStack(false));
    }

    @Test
    @DisplayName("PrefixExpression: 启用表达式缓存后的重复编译")
    void prefixExpressionCachedCompilation() {
        Mathematical_Expression.Options.setUseCache(true);
        try {
            final PrefixExpression first = PrefixExpression.compile("2*3+0", "probePECache");
            final PrefixExpression second = PrefixExpression.compile("2*3+0", "probePECache");
            assertSame(first, second);
            assertEquals(6.0, second.calculation(true).getResult().doubleValue(), 0.0);

            final PrefixExpression firstBigDecimal =
                    PrefixExpression.compileBigDecimal("2*3+0", "probePECacheBD");
            final PrefixExpression secondBigDecimal =
                    PrefixExpression.compileBigDecimal("2*3+0", "probePECacheBD");
            assertSame(firstBigDecimal, secondBigDecimal);
            assertEquals(6.0, secondBigDecimal.calculationBigDecimals(true).getResult().doubleValue(), 0.0);
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
    }

    // --------------------------------------------- PrefixExpressionOperation

    @Test
    @DisplayName("PrefixExpressionOperation: 格式检查与表达式格式化")
    void prefixExpressionOperationCheckAndFormat() throws Exception {
        CalculationManagement.unregister("probePEO");
        final PrefixExpressionOperation operation = PrefixExpressionOperation.getInstance("probePEO");
        assertEquals("probePEO", operation.getName());
        assertSame(operation, PrefixExpressionOperation.getInstance("probePEO"));

        operation.check("1+2");
        operation.check("1+-2");
        operation.check("12.5");
        operation.check("1 + 2");
        operation.check("1+2 ");

        final WrongFormat tail = assertThrows(WrongFormat.class, () -> operation.check("12+"));
        assertTrue(tail.getMessage().contains("最后一个字符"));
        final WrongFormat bracket = assertThrows(WrongFormat.class, () -> operation.check("1+(2"));
        assertTrue(bracket.getMessage().contains("括号"));
        assertTrue(bracket.getMessage().contains("只能解析无括号的数学表达式"));
        // 右括号出现在表达式中间同样属于"带括号"的非法输入。
        final WrongFormat rightBracket = assertThrows(WrongFormat.class,
                () -> operation.check("1+2)+3"));
        assertTrue(rightBracket.getMessage().contains("只能解析无括号的数学表达式"));
        final WrongFormat unknown = assertThrows(WrongFormat.class, () -> operation.check("1a2"));
        assertTrue(unknown.getMessage().contains("未知符号"));
        final WrongFormat doubled = assertThrows(WrongFormat.class, () -> operation.check("1+*2"));
        assertTrue(doubled.getMessage().contains("多出了一个运算符"));

        assertEquals("1+2+0", operation.formatStr("1+2"));
        assertEquals("7-3+0", operation.formatStr("7+-3"));
    }

    @Test
    @DisplayName("PrefixExpressionOperation: calculation 两种精度分支、compile/compileBigDecimal、explain")
    void prefixExpressionOperationCalculationAndExplain() throws Exception {
        final PrefixExpressionOperation operation = PrefixExpressionOperation.getInstance("probePEO");

        // 默认（非 BigDecimal 选项）走 calculationBigDecimal 分支。
        assertEquals(7.0, operation.calculation("1+2*3").getResult().doubleValue(), 1e-12);
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            // 开启 BigDecimal 选项后走 compile(...).calculationCache 分支。
            assertEquals(7.0, operation.calculation("1+2*3").getResult().doubleValue(), 1e-12);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
        assertEquals(4.0, operation.calculation("7+-3").getResult().doubleValue(), 1e-12);
        // 默认（非 BigDecimal 选项）走 BigDecimal 编译路径，0.1+0.2 精确等于 0.3。
        assertEquals(0.3, operation.calculation("0.1+0.2").getResult().doubleValue(), 0.0);
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            // 开启 BigDecimal 选项后走 double 编译路径，0.1+0.2 是二进制浮点结果。
            assertEquals(0.1 + 0.2,
                    operation.calculation("0.1+0.2").getResult().doubleValue(), 0.0);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }

        final NameExpression compiled = operation.compile("1+2+0", true);
        assertTrue(compiled instanceof PrefixExpression);
        assertFalse(((PrefixExpression) compiled).isBigDecimal());
        final NameExpression compiledBigDecimal = operation.compileBigDecimal("1+2+0", true);
        assertTrue(compiledBigDecimal instanceof PrefixExpression);
        assertTrue(((PrefixExpression) compiledBigDecimal).isBigDecimal());

        // formatRequired=false 的编译分支（公式已自带结尾数值）。
        final NameExpression rawCompiled = operation.compile("1+2+5", false);
        assertEquals(8.0, ((CalculationNumberResults) rawCompiled.calculation(true))
                .getResult().doubleValue(), 1e-12);
        final NameExpression rawCompiledBigDecimal = operation.compileBigDecimal("1+2+5", false);
        assertTrue(((PrefixExpression) rawCompiledBigDecimal).isBigDecimal());

        final LogResults explained = operation.explain("1+2*3", true);
        assertEquals(7.0, ((Number) explained.getResult()).doubleValue(), 1e-12);
        assertEquals("f_" + "1+2*3".hashCode(), explained.getCalculationSourceName());

        // 包可见的 explain(..., addResult=false) 只能反射调用，用于覆盖 addResult 分支。
        final Method explainWithoutResult = PrefixExpressionOperation.class
                .getDeclaredMethod("explain", String.class, boolean.class, boolean.class);
        explainWithoutResult.setAccessible(true);
        final Object noResultLog = explainWithoutResult.invoke(operation, "1+2*3", true, false);
        assertEquals(7.0, ((Number) ((LogResults) noResultLog).getResult()).doubleValue(), 1e-12);

        registeredBracketsProbe();
        assertThrows(ExtractException.class,
                () -> PrefixExpressionOperation.getInstance("probeBracketInit"));
    }

    @Test
    @DisplayName("PrefixExpressionOperation: explain 的过程日志结构（前缀/节点/结果）")
    void prefixExpressionOperationExplainStructure() {
        final PrefixExpressionOperation operation = PrefixExpressionOperation.getInstance("probePEO");
        final String formula = "1+2*3";
        final String base = "f_" + formula.hashCode();
        final LogResults results = operation.explain(formula, true);

        assertEquals(base + "(\"1+2*3+0\")", results.getPrefix());
        assertFalse(results.isNameJoin());
        assertEquals(7.0, ((Number) results.getResult()).doubleValue(), 1e-12);

        final Object priorityNode = results.get(base + "_优先");
        assertNotNull(priorityNode);
        assertTrue(priorityNode instanceof DataObj);

        final String foldText = "2.0 * 3.0";
        final String foldBase = "f_" + foldText.hashCode();
        final DataObj foldNode = (DataObj) ((DataObj) priorityNode).get(foldBase + "_计算");
        assertNotNull(foldNode);
        assertEquals(foldBase + "(\"" + foldText + "\")", foldNode.getPrefix());
        assertEquals(6.0, foldNode.get(foldBase));

        final Object finalNode = foldNode.get(base + "_最终");
        assertNotNull(finalNode);
        final String firstText = "1.0 + 6.0";
        final String firstBase = "f_" + firstText.hashCode();
        final DataObj firstCalc = (DataObj) ((DataObj) finalNode).get(firstBase + "_计算");
        assertNotNull(firstCalc);
        assertEquals(firstBase + "(\"" + firstText + "\")", firstCalc.getPrefix());

        final String secondText = "7.0 + 0.0";
        final String secondBase = "f_" + secondText.hashCode();
        final DataObj secondCalc = (DataObj) firstCalc.get(secondBase + "_计算");
        assertNotNull(secondCalc);
        assertEquals(secondBase + "(\"" + secondText + "\")", secondCalc.getPrefix());
        assertEquals(7.0, secondCalc.get("result"));
    }

    // --------------------------------------------- FunctionFormulaCalculation

    @Test
    @DisplayName("FunctionFormulaCalculation: 单参函数计算、格式检查与单参重载")
    void functionFormulaCalculationBasics() throws Exception {
        CalculationManagement.unregister("probeFFC");
        final FunctionFormulaCalculation calculation =
                FunctionFormulaCalculation.getInstance("probeFFC");
        assertEquals("probeFFC", calculation.getName());
        assertSame(calculation, FunctionFormulaCalculation.getInstance("probeFFC"));
        assertEquals("1+2*3", calculation.formatStr(" 1 + 2 * 3 "));

        calculation.check("sin(12)");
        calculation.check("1+sin(12)");
        calculation.check("sin(12)+cos(34)");
        // 大写字母 + 空白字符 + 嵌套括号的函数表达式检查分支。
        calculation.check("SIN(12)");
        calculation.check("1 + 2");
        // '~' 位于函数名识别范围之外，属于非法符号。
        assertThrows(WrongFormat.class, () -> calculation.check("sin(12)~"));
        assertThrows(WrongFormat.class, () -> calculation.check("sin((12))"));
        assertThrows(WrongFormat.class, () -> calculation.check("1)"));
        final WrongFormat tooShort = assertThrows(WrongFormat.class, () -> calculation.check("sin(1)"));
        assertTrue(tooShort.getMessage().contains("空字符串"));
        final WrongFormat unmatched = assertThrows(WrongFormat.class, () -> calculation.check("sin(1"));
        assertTrue(unmatched.getMessage().contains("括号"));
        // 缺函数括号的对数：start=2 个函数起点，end=1 个函数终点 -> |1-2| = 1。
        final WrongFormat unmatchedCount = assertThrows(WrongFormat.class,
                () -> calculation.check("sin(1)cos"));
        assertTrue(unmatchedCount.getMessage().contains("Missing function bracket logarithm: 1"));

        // 整数结果走整数拼接分支。
        assertEquals(2.0, calculation.calculation("sqrt(4)").getResult().doubleValue(), 1e-12);
        // 非整数结果走 double 拼接分支。
        assertEquals(Math.sqrt(2), calculation.calculation("sqrt(2)").getResult().doubleValue(), 1e-12);
        // 函数结果与表达式其余部分混合计算（含空白字符跳过分支）。
        assertEquals(3.0, calculation.calculation("sqrt(4)+1").getResult().doubleValue(), 1e-12);
        assertEquals(3.0, calculation.calculation("sqrt(4)+1", true).getResult().doubleValue(), 1e-12);
        assertEquals(3.0, calculation.calculation("1+sqrt(4)").getResult().doubleValue(), 1e-12);
        assertEquals(3.0, calculation.calculation("1 + sqrt(4)").getResult().doubleValue(), 1e-12);
        // 嵌套括号实参与大写函数名分支。
        assertEquals(2.0, calculation.calculation("sqrt((4))").getResult().doubleValue(), 1e-12);
        // '~' 不属于函数名字符，会被原样带到括号表达式中并被前缀解析器忽略。
        assertEquals(Math.sin(12.0),
                calculation.calculation("sin(12)~").getResult().doubleValue(), 1e-12);
        assertThrows(ExtractException.class, () -> calculation.calculation("SIN(4)"));
        // 字母 ASCII 区间端点 A/Z/a/z：正常均识别为函数名（未注册 -> ExtractException）。
        assertThrows(ExtractException.class, () -> calculation.calculation("A(1)"));
        assertThrows(ExtractException.class, () -> calculation.calculation("Z(1)"));
        assertThrows(ExtractException.class, () -> calculation.calculation("a(1)"));
        assertThrows(ExtractException.class, () -> calculation.calculation("z(1)"));
        // 同样四个端点字符在 check 中进入"单字符函数名"路径（参数仅 1 位 -> 空字符串格式错误）。
        for (String boundary : new String[]{"A(1)", "Z(1)", "a(1)", "z(1)"}) {
            final WrongFormat boundaryError = assertThrows(WrongFormat.class,
                    () -> calculation.check(boundary));
            assertTrue(boundaryError.getMessage().contains("空字符串"), boundary);
        }
    }

    @Test
    @DisplayName("FunctionFormulaCalculation/2: 受保护无参构造器（子类可达）")
    void legacyDefaultConstructorsAreReachableThroughSubclasses() {
        final FunctionFormulaCalculation legacy = new FunctionFormulaCalculation() {
        };
        assertEquals("functionFormulaCalculation_temp", legacy.getName());
        final FunctionFormulaCalculation2 modern = new FunctionFormulaCalculation2() {
        };
        assertEquals("functionFormulaCalculation2_temp", modern.getName());
        assertTrue(modern.isStartSharedPool());
        modern.setStartSharedPool(false);
        assertFalse(modern.isStartSharedPool());
    }

    @Test
    @DisplayName("FunctionFormulaCalculation: 名称被其它类型占用时抛出 ExtractException")
    void functionFormulaCalculationRegistryConflicts() {
        registeredBracketsProbe();
        assertThrows(ExtractException.class,
                () -> FunctionFormulaCalculation.getInstance("probeBracketInit"));
        assertThrows(ExtractException.class,
                () -> FunctionFormulaCalculation2.getInstance("probeBracketInit"));
    }

    // -------------------------------------------- FunctionFormulaCalculation2

    @Test
    @DisplayName("FunctionFormulaCalculation2: 函数形参索引提取（静态与带缓冲版本）")
    void functionFormulaCalculation2ParameterExtraction() {
        final Stack<Integer> start = new Stack<>();
        final Stack<Integer> end = new Stack<>();
        final Stack<String> names = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction("sin(1)+cos(2)", start, end, names);
        assertEquals(2, start.size());
        assertEquals(2, end.size());
        assertEquals(2, names.size());
        assertEquals(Integer.valueOf(4), start.get(0));
        assertEquals(Integer.valueOf(11), start.get(1));
        assertEquals(Integer.valueOf(5), end.get(0));
        assertEquals(Integer.valueOf(12), end.get(1));
        assertEquals("sin", names.get(0));
        assertEquals("cos", names.get(1));

        // 嵌套函数只会被识别为最外层的函数，内部交给递归计算。
        final Stack<Integer> nestedStart = new Stack<>();
        final Stack<Integer> nestedEnd = new Stack<>();
        final Stack<String> nestedNames = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction(
                "sin(cos(1))", nestedStart, nestedEnd, nestedNames);
        assertEquals(1, nestedStart.size());
        assertEquals(Integer.valueOf(4), nestedStart.get(0));
        assertEquals(Integer.valueOf(10), nestedEnd.get(0));
        assertEquals("sin", nestedNames.get(0));

        // 带公式缓冲的版本会把函数替换为 0。
        final Stack<Integer> bufferedStart = new Stack<>();
        final Stack<Integer> bufferedEnd = new Stack<>();
        final Stack<String> bufferedNames = new Stack<>();
        final StringBuilder formulaBuilder = new StringBuilder();
        final FunctionFormulaCalculation2 shared =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        shared.FunctionParameterExtraction(
                "sin(1)+cos(2)", bufferedStart, bufferedEnd, bufferedNames, formulaBuilder);
        assertEquals("0+0", formulaBuilder.toString());
        assertEquals(2, bufferedStart.size());
        assertEquals(2, bufferedEnd.size());
        assertEquals(2, bufferedNames.size());

        // 大写函数名与公式缓冲中的空白字符分支。
        final Stack<Integer> upperStart = new Stack<>();
        final Stack<Integer> upperEnd = new Stack<>();
        final Stack<String> upperNames = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction(
                "SIN(12)", upperStart, upperEnd, upperNames);
        assertEquals(1, upperStart.size());
        assertEquals("SIN", upperNames.get(0));

        final Stack<Integer> spaceStart = new Stack<>();
        final Stack<Integer> spaceEnd = new Stack<>();
        final Stack<String> spaceNames = new Stack<>();
        final StringBuilder spaceBuilder = new StringBuilder();
        shared.FunctionParameterExtraction(
                "sin(1) + 2", spaceStart, spaceEnd, spaceNames, spaceBuilder);
        assertEquals("0+2", spaceBuilder.toString());

        // '~' 在函数外：字母范围判定第二个区间的上界为 false 的分支。
        final Stack<Integer> tildeStart = new Stack<>();
        final Stack<Integer> tildeEnd = new Stack<>();
        final Stack<String> tildeNames = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction(
                "~sin(1)", tildeStart, tildeEnd, tildeNames);
        assertEquals(1, tildeStart.size());
        final Stack<Integer> tildeBufferedStart = new Stack<>();
        final Stack<Integer> tildeBufferedEnd = new Stack<>();
        final Stack<String> tildeBufferedNames = new Stack<>();
        final StringBuilder tildeBuilder = new StringBuilder();
        shared.FunctionParameterExtraction(
                "~sin(1)", tildeBufferedStart, tildeBufferedEnd, tildeBufferedNames, tildeBuilder);
        assertEquals("~0", tildeBuilder.toString());

        // 字母 ASCII 区间四个端点字符：A/Z/a/z 都必须被识别为函数名首字母。
        for (String boundary : new String[]{"A(1)", "Z(1)", "a(1)", "z(1)"}) {
            final Stack<Integer> edgeStart = new Stack<>();
            final Stack<Integer> edgeEnd = new Stack<>();
            final Stack<String> edgeNames = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction(
                    boundary, edgeStart, edgeEnd, edgeNames);
            assertEquals(1, edgeNames.size(), boundary);
            assertEquals(String.valueOf(boundary.charAt(0)), edgeNames.get(0));
            assertEquals(Integer.valueOf(2), edgeStart.get(0));
            assertEquals(Integer.valueOf(3), edgeEnd.get(0));

            final Stack<Integer> edgeBufferedStart = new Stack<>();
            final Stack<Integer> edgeBufferedEnd = new Stack<>();
            final Stack<String> edgeBufferedNames = new Stack<>();
            final StringBuilder edgeBuilder = new StringBuilder();
            shared.FunctionParameterExtraction(
                    boundary, edgeBufferedStart, edgeBufferedEnd, edgeBufferedNames, edgeBuilder);
            assertEquals("0", edgeBuilder.toString(), boundary);
        }
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2: check 的共享池、嵌套与异常分支")
    void functionFormulaCalculation2Check() throws Exception {
        final FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.clearCache();
        assertTrue(calculation.isStartSharedPool());

        calculation.check("sin(12)+cos(34)");
        assertTrue(calculation.isCache("sin(12)+cos(34)"));
        // 第二次命中共享池，直接返回。
        calculation.check("sin(12)+cos(34)");

        calculation.setStartSharedPool(false);
        assertFalse(calculation.isStartSharedPool());
        calculation.check("sin(12)+cos(34)");
        calculation.setStartSharedPool(true);
        // 大写函数名在共享池检查路径中同样被识别。
        calculation.check("SIN(12)");

        final WrongFormat unmatched = assertThrows(WrongFormat.class,
                () -> calculation.check("sin(1"));
        assertTrue(unmatched.getMessage().contains("括号"));
        // 缺函数括号的对数：start=2 个函数起点，end=1 个函数终点 -> |1-2| = 1。
        final WrongFormat unmatchedCount = assertThrows(WrongFormat.class,
                () -> calculation.check("sin(1)cos"));
        assertTrue(unmatchedCount.getMessage().contains("Missing function bracket logarithm: 1"));
        assertThrows(WrongFormat.class, () -> calculation.check("sin(cos(1))"));
        assertThrows(WrongFormat.class, () -> calculation.check("sin(12),cos(3)"));
        calculation.clearCache();
        assertFalse(calculation.isCache("sin(12)+cos(34)"));
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2: calculation 的缓存命中/未命中/数值与 BigDecimal 分支")
    void functionFormulaCalculation2Calculation() throws Exception {
        final FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.clearCache();

        final CalculationNumberResults pureNumber = calculation.calculation("12");
        assertEquals(12.0, pureNumber.getResult().doubleValue(), 0.0);
        assertEquals(0, pureNumber.getResultLayers());

        assertEquals(12.5, calculation.calculation("12.5").getResult().doubleValue(), 0.0);

        // check 预填共享池后，calculation 走 ShareHashMap 命中路径。
        calculation.check("sin(12)");
        final CalculationNumberResults first = calculation.calculation("sin(12)");
        assertEquals(Math.sin(12.0), first.getResult().doubleValue(), 1e-12);
        final CalculationNumberResults second = calculation.calculation("sin(12)");
        assertSame(first, second);
        // 清空缓存后必须重新计算，不能继续返回同一个结果对象。
        calculation.clearCache();
        assertFalse(calculation.isCache("sin(12)"));
        assertNotSame(first, calculation.calculation("sin(12)"));

        // 未命中路径 + 嵌套函数递归计算。
        calculation.clearCache();
        assertEquals(Math.sin(1.0), calculation.calculation("sin(1)").getResult().doubleValue(), 1e-12);
        assertEquals(Math.sin(Math.cos(1.0)),
                calculation.calculation("sin(cos(1))").getResult().doubleValue(), 1e-12);
        assertEquals(Math.sin(1.0) + 2.0,
                calculation.calculation("sin(1)+2").getResult().doubleValue(), 1e-12);

        // BigDecimal 选项分支。
        calculation.clearCache();
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            assertEquals(Math.sin(1.0),
                    calculation.calculation("sin(1)").getResult().doubleValue(), 1e-12);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
        // 关闭共享池后 calculation 不再读写共享池。
        calculation.clearCache();
        calculation.setStartSharedPool(false);
        assertFalse(calculation.isStartSharedPool());
        assertEquals(Math.sin(1.0),
                calculation.calculation("sin(1)").getResult().doubleValue(), 1e-12);
        calculation.setStartSharedPool(true);
        calculation.clearCache();
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2: explain 首层/非首层与嵌套函数过程日志")
    void functionFormulaCalculation2Explain() {
        final FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.clearCache();

        final LogResults firstLayer = calculation.explain("sqrt(4)+1", true);
        assertEquals(3.0, ((Number) firstLayer.getResult()).doubleValue(), 1e-12);

        final LogResults explicitFirst = calculation.explain("sqrt(4)+1", true, true, "layerA");
        assertEquals(3.0, ((Number) explicitFirst.getResult()).doubleValue(), 1e-12);
        assertEquals("layerA", explicitFirst.getCalculationSourceName());

        final LogResults nonFirst = calculation.explain("sqrt(4)+1", true, false, "layerB");
        assertEquals(3.0, ((Number) nonFirst.getResult()).doubleValue(), 1e-12);

        final LogResults nested = calculation.explain("sin(cos(1))", true);
        assertEquals(Math.sin(Math.cos(1.0)), ((Number) nested.getResult()).doubleValue(), 1e-12);

        final LogResults numberOnly = calculation.explain("8", true);
        assertEquals(8.0, ((Number) numberOnly.getResult()).doubleValue(), 1e-12);

        final LogResults twoFunctions = calculation.explain("sqrt(4)+sin(0)", true);
        assertEquals(2.0, ((Number) twoFunctions.getResult()).doubleValue(), 1e-12);

        // 同一个函数名出现两次时，日志节点会被复用（dataObj != null 分支）。
        final LogResults sameFunctionTwice = calculation.explain("sin(1)+sin(2)", true);
        assertEquals(Math.sin(1.0) + Math.sin(2.0),
                ((Number) sameFunctionTwice.getResult()).doubleValue(), 1e-12);
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2: explain 日志节点的前缀/嵌套/首层开关")
    void functionFormulaCalculation2ExplainStructure() {
        final FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.clearCache();

        final LogResults results = calculation.explain("sqrt(4)+1", true, true, "layerA");
        assertEquals(3.0, ((Number) results.getResult()).doubleValue(), 1e-12);
        assertEquals("layerA(\"sqrt(4)+1\")", results.getPrefix());
        assertFalse(results.isNameJoin());

        final String sqrtKey = "layerA" + "sqrt".hashCode() + "(\"sqrt\")";
        final Object node = results.get(sqrtKey);
        assertNotNull(node);
        final DataObj argumentNode = (DataObj) node;
        assertEquals("4".hashCode() + ":layer" + "(\"4\")", argumentNode.getPrefix());
        // 数值实参不会被放入子节点。
        assertNull(argumentNode.get("4".hashCode() + ":layer"));

        final String stepKey = "layerA" + "sqrt" + 5 + '_' + 6;
        final Object stepNode = argumentNode.get(stepKey);
        assertNotNull(stepNode);
        assertEquals(stepKey + "(\"sqrt(4)\")\n" + stepKey + "_r(\"sqrt([4.0],)\")",
                ((DataObj) stepNode).getPrefix());
        assertEquals("2.0", ((DataObj) stepNode).get(stepKey + "_r"));

        // 首层会把括号表达式的 explain 结果作为子节点。
        final String bracketKey = "f_" + "2.0+1".hashCode();
        assertNotNull(results.get(bracketKey));
        final LogResults nonFirst = calculation.explain("sqrt(4)+1", true, false, "layerA");
        assertEquals(results.size() - 1, nonFirst.size());
        assertNull(nonFirst.get(bracketKey));

        // 非数值实参会被放入实参节点。
        final LogResults expressionArgument = calculation.explain("sqrt(1+3)", true, true, "layerB");
        assertEquals(2.0, ((Number) expressionArgument.getResult()).doubleValue(), 1e-12);
        final DataObj expressionNode = (DataObj) expressionArgument.get(
                "layerB" + "sqrt".hashCode() + "(\"sqrt\")");
        assertNotNull(expressionNode);
        assertNotNull(expressionNode.get("1+3".hashCode() + ":layer"));
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2: compile / compileBigDecimal 与缓存结果来源")
    void functionFormulaCalculation2CompileAndCache() {
        final FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.clearCache();

        final FunctionExpression compiled = calculation.compile("sin(1)+1", true);
        assertEquals("sin(1)+1", compiled.getExpressionStr());
        final CalculationNumberResults result = compiled.calculationCache(true);
        assertEquals(Math.sin(1.0) + 1.0, result.getResult().doubleValue(), 1e-12);
        assertEquals(compiled.getCalculationName(), result.getCalculationSourceName());

        final FunctionExpression compiledBigDecimal = calculation.compileBigDecimal("sin(1)+1", true);
        assertNotNull(compiledBigDecimal);
        assertEquals(Math.sin(1.0) + 1.0,
                compiledBigDecimal.calculationBigDecimalsCache(true).getResult().doubleValue(), 1e-12);
    }

    // --------------------------------------------------- FunctionExpression

    @Test
    @DisplayName("FunctionExpression: 编译、深度编译、缓存与不可用分支")
    void functionExpressionLifecycle() throws Exception {
        final FunctionExpression expression = FunctionExpression.compile("sin(1)+2", "probeFE");
        assertFalse(expression.notNeedSubCompile());
        assertEquals(1, expression.getFunNames().size());
        assertEquals("sin", expression.getFunNames().peek());
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        expression.convertToMultiPrecisionSupported();

        final NameExpression subCompiled = expression.subCompile(true);
        assertNotNull(subCompiled);
        final CalculationNumberResults subResult =
                (CalculationNumberResults) subCompiled.calculationCache(true);
        assertEquals(Math.sin(1.0) + 2.0, subResult.getResult().doubleValue(), 1e-12);

        final NameExpression subCompiledBigDecimal = expression.subCompileBigDecimals(true);
        assertNotNull(subCompiledBigDecimal);
        final CalculationNumberResults subBigDecimalResult =
                (CalculationNumberResults) subCompiledBigDecimal.calculationBigDecimalsCache(true);
        assertEquals(Math.sin(1.0) + 2.0, subBigDecimalResult.getResult().doubleValue(), 1e-12);

        final CalculationNumberResults cached = expression.calculationCache(true);
        assertEquals(Math.sin(1.0) + 2.0, cached.getResult().doubleValue(), 1e-12);
        assertEquals("probeFE", cached.getCalculationSourceName());
        final CalculationNumberResults cachedBigDecimal = expression.calculationBigDecimalsCache(true);
        assertEquals(Math.sin(1.0) + 2.0, cachedBigDecimal.getResult().doubleValue(), 1e-12);
        assertEquals("probeFE", cachedBigDecimal.getCalculationSourceName());

        // 独立实例：先走 BigDecimal 缓存路径，来源必须被改写为表达式名称。
        final FunctionExpression bigDecimalOnly =
                FunctionExpression.compile("sin(1)+2", "probeFEBdSource");
        final CalculationNumberResults bigDecimalSource = bigDecimalOnly.calculationBigDecimalsCache(true);
        assertEquals(Math.sin(1.0) + 2.0, bigDecimalSource.getResult().doubleValue(), 1e-12);
        assertEquals("probeFEBdSource", bigDecimalSource.getCalculationSourceName());

        // 以 isCopy=false 计算之后表达式不可用，深度编译会抛出异常。
        final FunctionExpression closed = FunctionExpression.compile("sin(1)+2", "probeFEClosed");
        closed.calculationCache(false);
        assertFalse(closed.isAvailable());
        assertThrows(UnsupportedOperationException.class, () -> closed.subCompile(true));
        assertThrows(UnsupportedOperationException.class, () -> closed.subCompileBigDecimals(true));

        // 先缓存（isCopy=true 保留栈），再以缓存命中方式关闭可用性，
        // 这样 getStart/getEnd/getFunNames 的"不可用"分支都可以被单独触发。
        final FunctionExpression cachedClosed =
                FunctionExpression.compile("sin(1)+2", "probeFEClosedCached");
        cachedClosed.calculationCache(true);
        cachedClosed.calculationCache(false);
        assertFalse(cachedClosed.isAvailable());
        final Method getStart = FunctionExpression.class.getDeclaredMethod("getStart", boolean.class);
        final Method getEnd = FunctionExpression.class.getDeclaredMethod("getEnd", boolean.class);
        final Method getFunNames = FunctionExpression.class.getDeclaredMethod("getFunNames", boolean.class);
        getStart.setAccessible(true);
        getEnd.setAccessible(true);
        getFunNames.setAccessible(true);
        assertTrue(invocationCause(getStart, cachedClosed) instanceof UnsupportedOperationException);
        assertTrue(invocationCause(getEnd, cachedClosed) instanceof UnsupportedOperationException);
        assertTrue(invocationCause(getFunNames, cachedClosed) instanceof UnsupportedOperationException);

        // 可用状态下 isCopy=true 必须返回副本：修改返回值不能影响内部栈。
        final FunctionExpression copyProbe = FunctionExpression.compile("sin(1)+2", "probeFECopy");
        final Stack<Integer> startCopy = (Stack<Integer>) getStart.invoke(copyProbe, true);
        final Stack<Integer> endCopy = (Stack<Integer>) getEnd.invoke(copyProbe, true);
        final Stack<String> nameCopy = (Stack<String>) getFunNames.invoke(copyProbe, true);
        final int stackSize = startCopy.size();
        startCopy.pop();
        endCopy.pop();
        nameCopy.pop();
        assertEquals(stackSize, ((Stack<Integer>) getStart.invoke(copyProbe, false)).size());
        assertEquals(stackSize, ((Stack<Integer>) getEnd.invoke(copyProbe, false)).size());
        assertEquals(stackSize, ((Stack<String>) getFunNames.invoke(copyProbe, false)).size());

        // 不含函数的表达式无需深度编译。
        final FunctionExpression plain = FunctionExpression.compile("1+2+0", "probeFEPlain");
        assertTrue(plain.notNeedSubCompile());
        assertEquals(3.0, ((CalculationNumberResults) plain.calculation(true)).getResult().doubleValue(), 0.0);
        assertEquals(3.0,
                ((CalculationNumberResults) plain.calculationBigDecimals(true)).getResult().doubleValue(), 0.0);
        assertThrows(UnsupportedOperationException.class, () -> plain.subCompile(true));
        assertThrows(UnsupportedOperationException.class, () -> plain.subCompileBigDecimals(true));
    }

    @Test
    @DisplayName("FunctionExpression: 使用 setFunName 替换函数名参与计算")
    void functionExpressionSetFunName() {
        final FunctionExpression expression = FunctionExpression.compile("sin(0)+1", "probeFESet");
        expression.setFunName(0, "cos");
        assertEquals("cos(0)+1", expression.getExpressionStr());
        assertEquals(2.0, ((CalculationNumberResults) expression.calculation(true)).getResult().doubleValue(), 0.0);
    }

    @Test
    @DisplayName("FunctionExpression: 静态 subCompile 的三个提前返回与函数替换")
    void functionExpressionSubCompileStatics() {
        final Stack<Integer> emptyStart = new Stack<>();
        final Stack<Integer> emptyEnd = new Stack<>();
        final Stack<String> emptyNames = new Stack<>();
        assertEquals("123", FunctionExpression.subCompile(
                "123", emptyStart, emptyEnd, emptyNames).toString());
        assertEquals("1+2", FunctionExpression.subCompile(
                "1+2", emptyStart, emptyEnd, emptyNames).toString());

        final Stack<Integer> start = new Stack<>();
        final Stack<Integer> end = new Stack<>();
        final Stack<String> names = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction("sqrt(9)+1", start, end, names);
        assertEquals("3.0+1", FunctionExpression.subCompile(
                "sqrt(9)+1", start, end, names).toString());
    }

    // ---------------------------------------------------- JVM 动态编译相关
    //
    // 说明：本副本中的 DynamicFunctionCompiler 静态导入的是
    // "io.github.beardedManZhao.mathematicalExpression...ManyToOneNumberFunction"，
    // 该类型在本副本中并不存在，因此所有依赖动态编译的公开入口都会抛出
    // RuntimeException("动态函数生成失败")。以下用例锁定这一真实行为，
    // 并通过反射构造真实的 JvmExpressionFunction / JvmExpression 对象，
    // 覆盖它们与动态编译无关的全部逻辑（参数读写、解释、克隆、异常分支）。

    static class SumFunction extends ManyToOneNumberFunction {

        SumFunction(String name) {
            super(name);
        }

        @Override
        public double run(double... numbers) {
            double sum = 0;
            for (double number : numbers) {
                sum += number;
            }
            return sum;
        }
    }

    private static Object newParsedSignature(String name, String[] params, String expression)
            throws Exception {
        final Class<?> parsedClass =
                Class.forName("net.mooctest.expression.engine.ParsedSignature");
        final Constructor<?> constructor = parsedClass.getDeclaredConstructor(
                String.class, String[].class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(name, params, expression);
    }

    private static JvmExpressionFunction newJvmExpressionFunction(
            Object parsedSignature, ManyToOneNumberFunction delegate) throws Exception {
        final Class<?> parsedClass =
                Class.forName("net.mooctest.expression.engine.ParsedSignature");
        final Constructor<JvmExpressionFunction> constructor =
                JvmExpressionFunction.class.getDeclaredConstructor(parsedClass, ManyToOneNumberFunction.class);
        constructor.setAccessible(true);
        return constructor.newInstance(parsedSignature, delegate);
    }

    private static JvmExpression newJvmExpression(String calculationName, double[] numbers,
                                                  JvmExpressionFunction function, String expression)
            throws Exception {
        final Constructor<JvmExpression> constructor = JvmExpression.class.getDeclaredConstructor(
                String.class, double[].class, JvmExpressionFunction.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(calculationName, numbers, function, expression);
    }

    @Test
    @DisplayName("JvmExpression: 动态编译入口在本副本中因缺失类型而失败")
    void jvmExpressionCompileFailsOnMissingDynamicImport() {
        final RuntimeException single = assertThrows(RuntimeException.class,
                () -> JvmExpression.compile("1+2"));
        assertEquals("动态函数生成失败", single.getMessage());
        final RuntimeException named = assertThrows(RuntimeException.class,
                () -> JvmExpression.compile("3*4", "probeJvmName"));
        assertEquals("动态函数生成失败", named.getMessage());
    }

    @Test
    @DisplayName("JvmExpression / JvmExpressionFunction: 反射构造真实对象的完整 API")
    void jvmExpressionAndJvmExpressionFunctionApi() throws Exception {
        final Object parsed = newParsedSignature(
                "funProbe", new String[]{"param0", "param1"}, "param0+param1");
        final JvmExpressionFunction function = newJvmExpressionFunction(parsed, new SumFunction("sumProbe"));
        assertEquals("funProbe(param0,param1)=param0+param1", function.toString());
        assertEquals(3.0, function.run(1.0, 2.0), 0.0);
        assertEquals("1.0+2.0", function.explain(new double[]{1.0, 2.0}));
        assertEquals("1+2", function.explain(new Object[]{Integer.valueOf(1), Integer.valueOf(2)}));
        final IllegalArgumentException runArity =
                assertThrows(IllegalArgumentException.class, () -> function.run(1.0));
        assertTrue(runArity.getMessage().contains("参数数量不匹配"));
        assertThrows(IllegalArgumentException.class,
                () -> function.explain(new double[]{1.0, 2.0, 3.0}));
        assertThrows(IllegalArgumentException.class,
                () -> function.explain(new Object[]{Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)}));

        // ParsedSignature 的四个访问器（包可见构造，反射构造后验证）。
        final Class<?> parsedClass =
                Class.forName("net.mooctest.expression.engine.ParsedSignature");
        assertEquals("funProbe", parsedClass.getMethod("getName").invoke(parsed));
        assertArrayEquals(new String[]{"param0", "param1"},
                (String[]) parsedClass.getMethod("getParamNames").invoke(parsed));
        assertEquals("param0+param1", parsedClass.getMethod("getExpression").invoke(parsed));

        final JvmExpression jvmExpression = newJvmExpression(
                "probeJvm", new double[]{1.0, 2.0}, function, "param0+param1");
        assertEquals("probeJvm", jvmExpression.getCalculationName());
        assertEquals(2, jvmExpression.getLength());
        assertEquals(1.0, jvmExpression.getParamNumber(0), 0.0);
        assertEquals(2.0, jvmExpression.getParamNumber(1), 0.0);
        assertFalse(jvmExpression.isBigDecimal());
        assertTrue(jvmExpression.isUnBigDecimal());
        assertThrows(UnsupportedOperationException.class, jvmExpression::convertToMultiPrecisionSupported);
        assertThrows(UnsupportedOperationException.class,
                () -> jvmExpression.calculationBigDecimals(true));

        final CalculationNumberResults result = jvmExpression.calculation(true);
        assertEquals(3.0, result.getResult().doubleValue(), 0.0);
        assertEquals(1, result.getResultLayers());
        assertEquals("1.0+2.0", jvmExpression.getExpressionStrAndNumber());
        assertEquals(3.0, jvmExpression.iterator().sum(), 0.0);
        assertSame(function, jvmExpression.getJvmExpressionFunction());

        jvmExpression.setParamNumber(0, 10.0);
        assertEquals(12.0, jvmExpression.calculation(true).getResult().doubleValue(), 0.0);
        assertEquals("10.0+2.0", jvmExpression.getExpressionStrAndNumber());

        final JvmExpression clone = jvmExpression.cloneExpression();
        assertTrue(clone.getCalculationName().endsWith("_clone"));
        assertEquals(12.0, clone.calculation(true).getResult().doubleValue(), 0.0);
        assertEquals(jvmExpression.getLength(), clone.getLength());
    }

    @Test
    @DisplayName("JvmExpressionFunction: 签名解析错误分支、源码生成与注册")
    void jvmExpressionFunctionParsingAndRegistration() throws Exception {
        final RuntimeException broken = assertThrows(RuntimeException.class,
                () -> JvmExpressionFunction.parse("ee(x)=x+1"));
        assertEquals("动态函数生成失败", broken.getMessage());
        final RuntimeException brokenFromExpression = assertThrows(RuntimeException.class,
                () -> JvmExpressionFunction.parse(
                        net.mooctest.expression.engine.ExpressionFunction.parse("ff(x)=x+1")));
        assertEquals("动态函数生成失败", brokenFromExpression.getMessage());

        assertEquals("public static double zz(double x) { return x + 1; }",
                JvmExpressionFunction.convertToStaticMethodSource("zz(x)=x + 1"));
        // 零参数函数的源码生成（i == 0 分支）。
        assertEquals("public static double ff() { return 1; }",
                JvmExpressionFunction.convertToStaticMethodSource("ff()=1"));
        // 多参数函数的源码生成（i > 0 分支）。
        assertEquals("public static double gg(double x, double y) { return x+y; }",
                JvmExpressionFunction.convertToStaticMethodSource("gg(x,y)=x+y"));
        JvmExpressionFunction.registerFunction("dd(y)=y+2");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double dd"));
        assertFalse(JvmExpressionFunction.getRegisteredMethods().isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.registerJvmFunction("public static double noParen"));
        assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.registerJvmFunction("x(y) = y"));
        assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.parse("noEquals"));
        final IllegalArgumentException multipleEquals = assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("a=b=c"));
        assertTrue(multipleEquals.getMessage().contains("签名不能包含多个"));
        assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.parse("f=1"));
        assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.parse("ff(x=1"));
        assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.parse("f1(x)=x"));
        // 零参数签名可以解析，但动态编译仍然因缺失类型而失败。
        final RuntimeException zeroParam = assertThrows(RuntimeException.class,
                () -> JvmExpressionFunction.parse("ee()=1"));
        assertEquals("动态函数生成失败", zeroParam.getMessage());
    }

    @Test
    @DisplayName("InMemoryJavaFileObject: 反射覆盖字节码文件对象的写出与读取")
    void inMemoryJavaFileObjectApi() throws Exception {
        final Class<?> fileObjectClass =
                Class.forName("net.mooctest.expression.engine.InMemoryJavaFileObject");
        final Constructor<?> constructor = fileObjectClass.getDeclaredConstructor(
                String.class, javax.tools.JavaFileObject.Kind.class);
        constructor.setAccessible(true);
        final Object fileObject = constructor.newInstance(
                "probe.ClassName", javax.tools.JavaFileObject.Kind.CLASS);
        assertEquals("mem://probe/ClassName.class",
                ((javax.tools.JavaFileObject) fileObject).toUri().toString());

        final Method openOutputStream = fileObjectClass.getDeclaredMethod("openOutputStream");
        openOutputStream.setAccessible(true);
        final OutputStream outputStream = (OutputStream) openOutputStream.invoke(fileObject);
        outputStream.write(new byte[]{1, 2, 3});
        outputStream.flush();

        final Method getBytes = fileObjectClass.getDeclaredMethod("getBytes");
        getBytes.setAccessible(true);
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) getBytes.invoke(fileObject));
    }

    @Test
    @DisplayName("JvmCalculation: 实例化、动态编译失败、formatStr/check 与冲突分支")
    void jvmCalculationApi() {
        registeredBracketsProbe();
        CalculationManagement.unregister("probeJC");
        final JvmCalculation calculation = JvmCalculation.getInstance("probeJC");
        assertEquals("probeJC", calculation.getName());
        assertSame(calculation, JvmCalculation.getInstance("probeJC"));
        assertEquals(" 1 + 2 ", calculation.formatStr(" 1 + 2 "));
        calculation.check("anything");
        assertThrows(UnsupportedOperationException.class,
                () -> calculation.compileBigDecimal("1+2", true));
        final RuntimeException broken = assertThrows(RuntimeException.class,
                () -> calculation.calculation("1+2"));
        assertEquals("动态函数生成失败", broken.getMessage());
        assertThrows(ExtractException.class, () -> JvmCalculation.getInstance("probeBracketInit"));
    }

    // ------------------------------------------------- 方程求解器

    @Test
    @DisplayName("SingletonEquationSolving: 实例化、格式、动态编译失败与冲突分支")
    void singletonEquationSolvingApi() {
        registeredBracketsProbe();
        CalculationManagement.unregister("probeSES");
        final SingletonEquationSolving solving = SingletonEquationSolving.getInstance("probeSES");
        assertEquals("probeSES", solving.getName());
        assertSame(solving, SingletonEquationSolving.getInstance("probeSES"));
        assertEquals("x-2=0", solving.formatStr("x-2=0"));
        assertThrows(UnsupportedOperationException.class,
                () -> solving.compileBigDecimal("x-2=0", true));
        final RuntimeException broken = assertThrows(RuntimeException.class,
                () -> solving.calculation("x-2=0"));
        assertEquals("动态函数生成失败", broken.getMessage());
        assertThrows(ExtractException.class, () -> SingletonEquationSolving.getInstance("probeBracketInit"));
    }

    @Test
    @DisplayName("SingletonEquationSolvingTwo: 实例化、格式、动态编译失败与冲突分支")
    void singletonEquationSolvingTwoApi() {
        registeredBracketsProbe();
        CalculationManagement.unregister("probeSES2");
        final SingletonEquationSolvingTwo solving = SingletonEquationSolvingTwo.getInstance("probeSES2");
        assertEquals("probeSES2", solving.getName());
        assertSame(solving, SingletonEquationSolvingTwo.getInstance("probeSES2"));
        assertEquals("x-2=0", solving.formatStr("x-2=0"));
        assertThrows(UnsupportedOperationException.class,
                () -> solving.compileBigDecimal("x-2=0", true));
        final RuntimeException broken = assertThrows(RuntimeException.class,
                () -> solving.calculation("x-2=0"));
        assertEquals("动态函数生成失败", broken.getMessage());
        assertThrows(ExtractException.class,
                () -> SingletonEquationSolvingTwo.getInstance("probeBracketInit"));
    }
    // --------------------------------------- FastSumOfIntervalsBrackets

    @Test
    @DisplayName("FastSumOfIntervalsBrackets: 区间求和、共享池、边界校验与编译")
    void fastSumOfIntervalsBracketsApi() throws Exception {
        registeredBracketsProbe();
        CalculationManagement.unregister("probeFS");
        final FastSumOfIntervalsBrackets calculation =
                FastSumOfIntervalsBrackets.getInstance("probeFS");
        calculation.step = 1;
        calculation.clearCache();
        assertEquals("probeFS", calculation.getName());
        assertEquals("1,4", calculation.formatStr(" 1,4 "));
        assertTrue(calculation.isStartSharedPool());

        calculation.check("1,4");
        // 相同公式再次检查会命中共享池，直接返回。
        calculation.check("1,4");
        assertTrue(calculation.isCache("1,4"));
        // 共享池已经保存左右边界。
        assertEquals(10.0, calculation.calculation("1,4").getResult().doubleValue(), 0.0);
        // 共享池已经保存计算结果。
        assertEquals(10.0, calculation.calculation("1,4").getResult().doubleValue(), 0.0);
        // 只要共享池命中，即使步长变化也返回缓存结果（而不是重新计算）。
        calculation.step = 2;
        assertEquals(10.0, calculation.calculation("1,4").getResult().doubleValue(), 0.0);
        calculation.step = 1;
        // 共享池开启但公式与缓存不同 -> 重新切分计算。
        assertEquals(6.0, calculation.calculation("1,3").getResult().doubleValue(), 0.0);
        // 非共享池路径：重新按公式切分。
        calculation.setStartSharedPool(false);
        assertFalse(calculation.isStartSharedPool());
        assertEquals(10.0, calculation.calculation("1,4").getResult().doubleValue(), 0.0);
        // 单参重载 + 不同区间。
        assertEquals(6.0, calculation.calculation("1,3").getResult().doubleValue(), 0.0);

        final WrongFormat intervalError = assertThrows(WrongFormat.class,
                () -> calculation.check("1,2,3"));
        assertTrue(intervalError.getMessage().contains("区间求和表达式解析错误"));
        // 左右边界表达式相同时只校验一次。
        calculation.check("5,5");
        // 非法边界表达式必须被括号组件校验（校验调用不可省略）。
        assertThrows(WrongFormat.class, () -> calculation.check("1+,2"));
        assertThrows(WrongFormat.class, () -> calculation.check("1,2+"));
        assertThrows(WrongFormat.class, () -> calculation.check("1,1+"));

        // 两个边界公式相同的分支。
        assertEquals(5.0, calculation.calculation("5", "5").getResult().doubleValue(), 0.0);
        // 两个边界公式不同的分支。
        assertEquals(5.0, calculation.calculation("2", "3").getResult().doubleValue(), 0.0);
        // 直接传入结果对象。
        assertEquals(10.0, calculation.calculation(
                new CalculationNumberResults(0, 1.0, "s"),
                new CalculationNumberResults(0, 4.0, "e")).getResult().doubleValue(), 0.0);
        // 结果层数 = 左右边界表达式的层数之和。
        final CalculationNumberResults layered = calculation.calculation("1+1", "4");
        assertEquals(9.0, layered.getResult().doubleValue(), 0.0);
        // 两个边界表达式都会在末尾补 "+0"，因此各贡献 1 层。
        assertEquals(2, layered.getResultLayers());

        final PackExpression compiled = calculation.compile("1,4", true);
        assertEquals(10.0, ((CalculationNumberResults) compiled.calculation(true))
                .getResult().doubleValue(), 0.0);
        final PackExpression compiledBigDecimal = calculation.compileBigDecimal("1,4", true);
        assertEquals(10.0, ((CalculationNumberResults) compiledBigDecimal.calculationBigDecimals(true))
                .getResult().doubleValue(), 0.0);

        calculation.clearCache();
        assertFalse(calculation.isCache("1,4"));

        assertThrows(ExtractException.class,
                () -> FastSumOfIntervalsBrackets.getInstance("probeBracketInit"));
    }
}

