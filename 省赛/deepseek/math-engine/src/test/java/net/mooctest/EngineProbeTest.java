package net.mooctest;

import net.mooctest.expression.engine.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.lingyuzhao.varFormatter.core.VarFormatter;

import javax.tools.JavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内嵌表达式引擎（net.mooctest.expression.engine）的补充覆盖测试。
 *
 * <p>目标：用真实表达式/方程输入驱动每一个类、构造器与枚举内部类（FunctionPackage$1..$31），
 * 覆盖分支并给出独立手算的期望值。</p>
 *
 * <p>约定：</p>
 * <ul>
 *   <li>不修改被测代码；仅通过公开 API + 规则允许的反射直调（私有/包私有入口）触达逻辑。</li>
 *   <li>不依赖当前时间、时区、随机数与网络；{@code StrUtils.randomString} 只断言长度与字符集。</li>
 *   <li>全局静态状态（函数注册表、Options 开关、共享池）在每个用例前后复位。</li>
 * </ul>
 */
@DisplayName("expression.engine 补充覆盖探针")
public class EngineProbeTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------ 全局状态

    @BeforeEach
    void resetEngineState() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.register_function(FunctionPackage.MATH);
        Mathematical_Expression.register_function(new QFunction());
        Mathematical_Expression.register_function(new ProbeAddFunction());
        FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME).clearCache();
    }

    @AfterEach
    void clearEngineState() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.register_function(FunctionPackage.MATH);
        FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME).clearCache();
    }

    // ------------------------------------------------------------------ 辅助类型

    /** 单字母函数（旧版 FunctionFormulaCalculation 的 check 只接受单字母函数名）。 */
    static final class QFunction extends ManyToOneNumberFunction {
        QFunction() {
            super("q");
        }

        @Override
        public double run(double... numbers) {
            return numbers[0] * 2;
        }
    }

    /** 可被序列化的探测函数。 */
    static final class ProbeAddFunction extends ManyToOneNumberFunction {
        ProbeAddFunction() {
            super("probeAdd");
        }

        @Override
        public double run(double... numbers) {
            return numbers[0] + 1;
        }
    }

    /** 使用父类无参构造（默认名 f）的函数。 */
    static final class DefaultNamedFunction extends ManyToOneNumberFunction {
        @Override
        public double run(double... numbers) {
            return numbers.length;
        }
    }

    static final class NegLinearFunction extends ManyToOneNumberFunction {
        NegLinearFunction() {
            super("neg");
        }

        @Override
        public double run(double... numbers) {
            return 2 - numbers[0];
        }
    }

    static final class ParabolaFunction extends ManyToOneNumberFunction {
        ParabolaFunction() {
            super("parabola");
        }

        @Override
        public double run(double... numbers) {
            return 5 - (numbers[0] - 2) * (numbers[0] - 2);
        }
    }

    static final class SquareFunction extends ManyToOneNumberFunction {
        SquareFunction() {
            super("square");
        }

        @Override
        public double run(double... numbers) {
            return numbers[0] * numbers[0];
        }
    }

    @Functions({"probeAnnoA(x)=x+1", "probeAnnoB(x)=x*2"})
    static final class AnnotatedFunctionsHolder {
    }

    static final class PlainHolder {
    }

    /** 暴露 protected 求解入口的求解器（不经过动态编译）。 */
    static final class ExposedEquationSolver extends EquationSolver {
        ExposedEquationSolver(String expr, double result, ManyToOneNumberFunction function) {
            super(expr, result, "EngineProbeSolver", function);
        }

        double evalAt(double x) {
            return eval(x);
        }

        void detectInterval() {
            autoDetectInterval();
        }
    }

    /** 用于 CalculationManagement 注册分支的组件实现。 */
    static final class ProbeCalculation implements Calculation {
        private final String name;

        ProbeCalculation(String name) {
            this.name = name;
        }

        @Override
        public String formatStr(String string) {
            return string.trim();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void check(String string) {
        }

        @Override
        public CalculationResults calculation(String Formula) {
            return new CalculationNumberResults(0, 1.0, name);
        }

        @Override
        public CalculationResults calculation(String Formula, boolean formatRequired) {
            return calculation(Formula);
        }
    }

    // ------------------------------------------------------------------ 工具方法

    private static String uniq(String prefix) {
        return "EngineProbe_" + prefix + "_" + SEQ.incrementAndGet();
    }

    private static ManyToOneNumberFunction mathFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck(name);
    }

    private static ManyToOneNumberFunction dateFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck(name);
    }

    private static ManyToOneNumberFunction branchFn(String name) {
        return (ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck(name);
    }

    private static BracketsCalculation2 brackets() {
        return BracketsCalculation2.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method m = type.getDeclaredMethod(name, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Constructor<?> constructor(Class<?> type, Class<?>... parameterTypes) {
        try {
            Constructor<?> c = type.getDeclaredConstructor(parameterTypes);
            c.setAccessible(true);
            return c;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object call(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 反射调用并返回被调用方抛出的异常，正常返回时给出 null。 */
    private static Throwable failure(Method m, Object target, Object... args) {
        try {
            m.invoke(target, args);
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object instantiate(Constructor<?> c, Object... args) {
        try {
            return c.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void assertMessageContains(Throwable throwable, String keyword) {
        assertNotNull(throwable, "expected a throwable");
        String message = throwable.getMessage();
        assertNotNull(message, "expected a message on " + throwable.getClass().getName());
        assertTrue(message.contains(keyword),
                () -> "message should contain <" + keyword + "> but was: " + message);
    }

    private static double numeric(CalculationResults results) {
        return ((Number) results.getResult()).doubleValue();
    }

    private static Object loadClass(ClassLoader loader, String name) {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    // ================================================================== StrUtils

    @Test
    @DisplayName("StrUtils.removeEmpty/containsNumber/containsEnChar 删除空格与字符探测")
    void strUtilsRemoveEmptyAndCharDetection() {
        assertEquals("1+2", StrUtils.removeEmpty(" 1 + 2 "));
        assertEquals("", StrUtils.removeEmpty(""));
        assertEquals("", StrUtils.removeEmpty("   "));

        assertTrue(StrUtils.containsNumber("abc1"));
        assertFalse(StrUtils.containsNumber("abc"));
        assertFalse(StrUtils.containsNumber(""));

        assertEquals(Character.valueOf('a'), StrUtils.containsEnChar("1a2"));
        assertEquals(Character.valueOf('Z'), StrUtils.containsEnChar("12Z"));
        assertNull(StrUtils.containsEnChar("123"));
    }

    @Test
    @DisplayName("StrUtils.IsANumber 对字符与字符串的判定")
    void strUtilsIsANumber() {
        assertTrue(StrUtils.IsANumber('0'));
        assertTrue(StrUtils.IsANumber('9'));
        assertFalse(StrUtils.IsANumber('/'));
        assertFalse(StrUtils.IsANumber(' '));

        assertTrue(StrUtils.IsANumber("0123456789"));
        assertTrue(StrUtils.IsANumber(""));
        assertFalse(StrUtils.IsANumber("12a"));
        assertFalse(StrUtils.IsANumber("-1"));
    }

    @Test
    @DisplayName("StrUtils.IsAnOperator 仅接受六种算术运算符")
    void strUtilsIsAnOperator() {
        for (char c : new char[]{'+', '-', '*', '/', '%', '^'}) {
            assertTrue(StrUtils.IsAnOperator(c));
        }
        for (char c : new char[]{'(', ')', '.', '!', '='}) {
            assertFalse(StrUtils.IsAnOperator(c));
        }
    }

    @Test
    @DisplayName("StrUtils.splitByChar 与 splitByCharWhereNoB 的切分差异")
    void strUtilsSplit() {
        assertEquals(List.of("1", "2", "3"), StrUtils.splitByChar("1,2,3", ','));
        assertEquals(List.of("1", "2", ""), StrUtils.splitByChar("1,2,", ','));
        assertEquals(List.of("1", "2"), StrUtils.splitByChar(" 1 , 2 ", ','));

        // 括号内部的逗号不会被当作分隔符
        assertEquals(List.of("f(1,2)", "3"), StrUtils.splitByCharWhereNoB("f(1,2),3", ','));
        assertEquals(List.of("(1+2)"), StrUtils.splitByCharWhereNoB("(1+2)", ','));
        assertEquals(List.of("1", "2", "3"), StrUtils.splitByCharWhereNoB("1,2,3", ','));
    }

    @Test
    @DisplayName("StrUtils.stringToDouble 普通数值与阶乘")
    void strUtilsStringToDouble() {
        assertEquals(12.5, StrUtils.stringToDouble("12.5"), 0.0);
        assertEquals(-3.0, StrUtils.stringToDouble("-3"), 0.0);
        assertEquals(120.0, StrUtils.stringToDouble("5!"), 0.0);
        assertEquals(1.0, StrUtils.stringToDouble("1!"), 0.0);
        assertEquals(0.0, StrUtils.stringToDouble("0!"), 0.0);
    }

    @Test
    @DisplayName("StrUtils.stringToDouble 负数阶乘被拒绝")
    void strUtilsStringToDoubleNegativeFactorial() {
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> StrUtils.stringToDouble("-3!"));
        assertMessageContains(exception, "负数不支持计算阶乘");
    }

    @Test
    @DisplayName("StrUtils.stringToDouble 在开启缓存时写缓存并复用")
    void strUtilsStringToDoubleCache() {
        Mathematical_Expression.Options.setUseCache(true);
        try {
            assertEquals(2.5, StrUtils.stringToDouble("2.5"), 0.0);
            assertEquals(2.5, Mathematical_Expression.Options
                    .getCacheCalculation("2.5".hashCode()).doubleValue(), 0.0);
            assertEquals(2.5, StrUtils.stringToDouble("2.5"), 0.0);

            assertEquals(3628800.0, StrUtils.stringToDouble("10!"), 0.0);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("10!".hashCode()));
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
        assertNull(Mathematical_Expression.Options.getCacheCalculation("2.5".hashCode()));
    }

    @Test
    @DisplayName("StrUtils.stringToBigDecimal 保留输入 scale 与阶乘")
    void strUtilsStringToBigDecimal() {
        BigDecimal value = StrUtils.stringToBigDecimal("0.10");
        assertEquals("0.10", value.toString());
        assertEquals(2, value.scale());
        assertEquals(0, StrUtils.stringToBigDecimal("12.50").compareTo(new BigDecimal("12.5")));
        assertEquals(120.0, StrUtils.stringToBigDecimal("5!").doubleValue(), 0.0);
    }

    @Test
    @DisplayName("StrUtils.stringToBigDecimal 负数阶乘与缓存")
    void strUtilsStringToBigDecimalNegativeAndCache() {
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> StrUtils.stringToBigDecimal("-2!")), "负数不支持计算阶乘");

        Mathematical_Expression.Options.setUseCache(true);
        try {
            BigDecimal first = StrUtils.stringToBigDecimal("2.50");
            assertSame(first, StrUtils.stringToBigDecimal("2.50"));
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
    }

    @Test
    @DisplayName("StrUtils.randomString 长度与小写字母字符集")
    void strUtilsRandomString() {
        String generated = StrUtils.randomString(8);
        assertEquals(8, generated.length());
        assertTrue(generated.matches("[a-z]{8}"));
        assertEquals("", StrUtils.randomString(0));
    }

    // ================================================================== NumberUtils

    @Test
    @DisplayName("NumberUtils.factorial 与 divideByTen")
    void numberUtilsFactorialAndDivideByTen() {
        assertEquals(0.0, NumberUtils.factorial(0), 0.0);
        assertEquals(1.0, NumberUtils.factorial(1), 0.0);
        assertEquals(120.0, NumberUtils.factorial(5), 0.0);
        assertEquals(10, NumberUtils.divideByTen(100));
        assertEquals(0, NumberUtils.divideByTen(9));
    }

    @Test
    @DisplayName("NumberUtils.calculation 六种运算符与非法运算符")
    void numberUtilsCalculation() {
        assertEquals(5.0, NumberUtils.calculation('+', 2, 3), 0.0);
        assertEquals(-1.0, NumberUtils.calculation('-', 2, 3), 0.0);
        assertEquals(6.0, NumberUtils.calculation('*', 2, 3), 0.0);
        assertEquals(2.0, NumberUtils.calculation('/', 6, 3), 0.0);
        assertEquals(1.0, NumberUtils.calculation('%', 7, 3), 0.0);
        assertEquals(8.0, NumberUtils.calculation('^', 2, 3), 0.0);

        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> NumberUtils.calculation('&', 1, 2)), "错误的计算模式");
    }

    @Test
    @DisplayName("NumberUtils.PriorityComparison 优先级矩阵")
    void numberUtilsPriorityComparison() {
        assertTrue(NumberUtils.PriorityComparison('+', '*'));
        assertTrue(NumberUtils.PriorityComparison('+', '/'));
        assertTrue(NumberUtils.PriorityComparison('+', '%'));
        assertTrue(NumberUtils.PriorityComparison('+', '^'));
        assertTrue(NumberUtils.PriorityComparison('-', '*'));
        assertFalse(NumberUtils.PriorityComparison('+', '+'));
        assertFalse(NumberUtils.PriorityComparison('+', '-'));
        assertFalse(NumberUtils.PriorityComparison('*', '+'));
        assertFalse(NumberUtils.PriorityComparison('^', '/'));
    }

    @Test
    @DisplayName("NumberUtils.ComparisonOperation 全部比较运算符与非法运算符")
    void numberUtilsComparisonOperation() {
        assertTrue(NumberUtils.ComparisonOperation("<", 1, 2));
        assertFalse(NumberUtils.ComparisonOperation("<", 2, 1));
        assertTrue(NumberUtils.ComparisonOperation(">", 2, 1));
        assertFalse(NumberUtils.ComparisonOperation(">", 1, 2));
        assertTrue(NumberUtils.ComparisonOperation("<=", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation(">=", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation("=", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation("==", 2, 2));
        assertTrue(NumberUtils.ComparisonOperation("!=", 2, 3));
        assertTrue(NumberUtils.ComparisonOperation("<>", 2, 3));

        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> NumberUtils.ComparisonOperation("~", 1, 2)), "错误的运算符");
    }

    @Test
    @DisplayName("NumberUtils.sumOfRange 无步长（int/double/字符串）")
    void numberUtilsSumOfRange() {
        assertEquals(5, NumberUtils.sumOfRange(5, 5));
        assertEquals(55, NumberUtils.sumOfRange(1, 10));
        assertEquals(2.0, NumberUtils.sumOfRange(2.0, 2.0), 0.0);
        assertEquals(55.0, NumberUtils.sumOfRange(1.0, 10.0), 0.0);
        assertEquals("2.0", NumberUtils.sumOfRangeString(2.0, 2.0));
        assertEquals("(1.0+10.0)*10.0/2", NumberUtils.sumOfRangeString(1.0, 10.0));
    }

    @Test
    @DisplayName("NumberUtils.sumOfRange 带步长（1+3+5 与字符串形式）")
    void numberUtilsSumOfRangeWithStep() {
        assertEquals(9.0, NumberUtils.sumOfRange(1.0, 5.0, 2.0), 0.0);
        assertEquals(55.0, NumberUtils.sumOfRange(1.0, 10.0, 1.0), 0.0);
        assertEquals(4.0, NumberUtils.sumOfRange(4.0, 4.0, 3.0), 0.0);
        assertEquals("(1.0+10.0)*10.0/2", NumberUtils.sumOfRangeString(1.0, 10.0, 1.0));
        assertEquals("4.0", NumberUtils.sumOfRangeString(4.0, 4.0, 2.0));
        assertEquals("3.0*1.0+3.0*(3.0-1)*2.0/2", NumberUtils.sumOfRangeString(1.0, 6.0, 2.0));
    }

    @Test
    @DisplayName("NumberUtils.MultiplyOfRange 区间累乘与字符串形式")
    void numberUtilsMultiplyOfRange() {
        assertEquals(3.0, NumberUtils.MultiplyOfRange(3.0, 3.0, 1.0), 0.0);
        assertEquals(48.0, NumberUtils.MultiplyOfRange(2.0, 6.0, 2.0), 0.0);
        assertEquals(15.0, NumberUtils.MultiplyOfRange(1.0, 5.0, 2.0), 0.0);
        assertEquals("3.0", NumberUtils.MultiplyOfRangeString(3.0, 3.0, 1.0));
        assertEquals("48.0", NumberUtils.MultiplyOfRangeString(2.0, 6.0, 2.0));
    }

    // ================================================================== 常量/函数基础/结果对象

    @Test
    @DisplayName("ConstantRegion 常量与符号名称映射")
    void constantRegionConstants() {
        assertEquals(1.57F, ConstantRegion.VERSION, 1e-6);
        assertEquals("null", ConstantRegion.STRING_NULL);
        assertEquals('(', ConstantRegion.LEFT_BRACKET);
        assertEquals(')', ConstantRegion.RIGHT_BRACKET);
        assertEquals('.', ConstantRegion.DECIMAL_POINT);
        assertEquals(' ', ConstantRegion.EMPTY);
        assertEquals("", ConstantRegion.NO_CHAR);
        assertEquals('+', ConstantRegion.PLUS_SIGN);
        assertEquals('-', ConstantRegion.MINUS_SIGN);
        assertEquals('*', ConstantRegion.MULTIPLICATION_SIGN);
        assertEquals('/', ConstantRegion.DIVISION_SIGN);
        assertEquals('%', ConstantRegion.REMAINDER_SIGN);
        assertEquals('!', ConstantRegion.FACTORIAL_SIGN);
        assertEquals('^', ConstantRegion.POW_SIGN);
        assertEquals(',', ConstantRegion.COMMA);
        assertEquals(">", ConstantRegion.GREATER_THAN_SIGN);
        assertEquals("<", ConstantRegion.LESS_THAN_SIGN);
        assertEquals("=", ConstantRegion.EQUAL_SIGN1);
        assertEquals("==", ConstantRegion.EQUAL_SIGN2);
        assertEquals("!=", ConstantRegion.NOT_EQUAL_SIGN1);
        assertEquals("<>", ConstantRegion.NOT_EQUAL_SIGN2);
        assertEquals(">=", ConstantRegion.GREATER_THAN_OR_EQUAL_TO_SIGN);
        assertEquals("<=", ConstantRegion.LESS_THAN_OR_EQUAL_TO_SIGN);
        assertEquals("!", ConstantRegion.NEGATE_SIGN);
        assertEquals("-", ConstantRegion.MINUS_SIGN_STR);
        assertEquals("[a-zA-Z]+", ConstantRegion.REGULAR_PURE_LETTER);
        assertEquals(4, ConstantRegion.SIGN_NAME.size());
        assertEquals("pow", ConstantRegion.SIGN_NAME.get('^'));
        assertEquals("_LB", ConstantRegion.SIGN_NAME.get('('));
        assertEquals("RB_", ConstantRegion.SIGN_NAME.get(')'));
        assertEquals("", ConstantRegion.SIGN_NAME.get(' '));
    }

    @Test
    @DisplayName("ManyToOneNumberFunction 名称校验、默认构造与序列化开关")
    void manyToOneNumberFunctionContract() {
        assertEquals("probeAdd", new ProbeAddFunction().getName());
        assertEquals("probeAdd", new ProbeAddFunction().toString());
        assertTrue(new ProbeAddFunction().AllowSerialization());
        assertEquals(2.0, new ProbeAddFunction().run(1.0), 0.0);

        // 无参 protected 构造 -> 默认名 f
        DefaultNamedFunction defaultNamed = new DefaultNamedFunction();
        assertEquals("f", defaultNamed.getName());
        assertEquals(3.0, defaultNamed.run(0, 0, 0), 0.0);

        assertMessageContains(assertThrows(ExtractException.class,
                () -> new ManyToOneNumberFunction("has1digit") {
                    @Override
                    public double run(double... numbers) {
                        return 0;
                    }
                }), "函数名称中只能包含字母");

        assertMessageContains(assertThrows(ExtractException.class,
                () -> new ManyToOneNumberFunction("") {
                    @Override
                    public double run(double... numbers) {
                        return 0;
                    }
                }), "函数名称中只能包含字母");
    }

    @Test
    @DisplayName("AbnormalOperation 四个构造器")
    void abnormalOperationConstructors() {
        assertNull(new AbnormalOperation().getMessage());
        assertEquals("boom", new AbnormalOperation("boom").getMessage());
        IllegalStateException cause = new IllegalStateException("cause");
        assertSame(cause, new AbnormalOperation("boom", cause).getCause());
        assertSame(cause, new AbnormalOperation(cause).getCause());
    }

    @Test
    @DisplayName("WrongFormat 四个构造器")
    void wrongFormatConstructors() {
        assertNull(new WrongFormat().getMessage());
        assertEquals("bad", new WrongFormat("bad").getMessage());
        IllegalStateException cause = new IllegalStateException("cause");
        assertSame(cause, new WrongFormat("bad", cause).getCause());
        assertSame(cause, new WrongFormat(cause).getCause());
    }

    @Test
    @DisplayName("ExtractException 四个构造器")
    void extractExceptionConstructors() {
        assertNull(new ExtractException().getMessage());
        assertEquals("bad", new ExtractException("bad").getMessage());
        IllegalStateException cause = new IllegalStateException("cause");
        assertSame(cause, new ExtractException("bad", cause).getCause());
        assertSame(cause, new ExtractException(cause).getCause());
    }

    @Test
    @DisplayName("CalculationNumberResults 取值/比较/字符串")
    void calculationNumberResults() {
        CalculationNumberResults results = new CalculationNumberResults(3, 2.5, "sourceA");
        assertEquals(3, results.getResultLayers());
        assertEquals(2.5, results.getResult(), 0.0);
        assertEquals("sourceA", results.getCalculationSourceName());
        assertEquals(2.5, results.getBigDecimalResult().doubleValue(), 0.0);
        assertEquals("CalculationNumberResults{result=2.5, source='sourceA'}", results.toString());

        results.setSource("sourceB");
        assertEquals("sourceB", results.getCalculationSourceName());

        CalculationNumberResults smaller = new CalculationNumberResults(0, 1.0, "s");
        assertEquals(1, results.compareTo(null));
        assertEquals(2, results.compareTo(smaller));
        assertEquals(-1, smaller.compareTo(results));
        assertEquals(0, results.compareTo(new CalculationNumberResults(0, 2.5, "x")));
        assertEquals(2, results.compare(results, smaller));

        // 接口默认方法
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> results.explain(VarFormatter.JSON)), "no execution process record");
    }

    @Test
    @DisplayName("CalculationBooleanResults 取值/比较/字符串与默认高精度拒绝")
    void calculationBooleanResults() {
        CalculationBooleanResults truth = new CalculationBooleanResults(true, "boolSrc", 4, 1.0, 2.0);
        assertEquals(4, truth.getResultLayers());
        assertEquals(Boolean.TRUE, truth.getResult());
        assertEquals("boolSrc", truth.getCalculationSourceName());
        assertEquals(1.0, truth.getLeft(), 0.0);
        assertEquals(2.0, truth.getRight(), 0.0);
        assertEquals("CalculationBooleanResults{result=true, source='boolSrc', Layers=4, left=1.0, right=2.0}",
                truth.toString());

        truth.setSource("other");
        assertEquals("other", truth.getCalculationSourceName());

        CalculationBooleanResults falsy = new CalculationBooleanResults(false, "f", 0, 0, 0);
        assertEquals(1, truth.compareTo(null));
        assertEquals(1, truth.compareTo(falsy));
        assertEquals(-1, falsy.compareTo(truth));
        assertEquals(0, truth.compareTo(new CalculationBooleanResults(true, "t", 0, 0, 0)));
        assertEquals(1, truth.compare(truth, falsy));

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                truth::getBigDecimalResult), "getBigDecimalResult");
    }

    @Test
    @DisplayName("CalculationBigDecimalResults 保留 BigDecimal 的 scale")
    void calculationBigDecimalResults() {
        CalculationBigDecimalResults results =
                new CalculationBigDecimalResults(2, new BigDecimal("1.50"), "bigSrc");
        assertEquals(1.5, results.getResult(), 0.0);
        assertEquals(2, results.getResultLayers());
        assertEquals("1.50", results.getBigDecimalResult().toString());
        assertEquals(2, results.getBigDecimalResult().scale());
        assertEquals("1.50", results.getBigDecimalResult().toPlainString());
    }

    @Test
    @DisplayName("CalculationComplexResults 复数实部虚部与字符串")
    void calculationComplexResults() {
        CalculationComplexResults results = new CalculationComplexResults(3, 1.0, -2.0, "complexSrc");
        assertEquals(3, results.getResultLayers());
        assertEquals(1.0, results.getResult(), 0.0);
        assertEquals(1.0, results.toComplexNumber().getReal(), 0.0);
        assertEquals(-2.0, results.toComplexNumber().getImaginary(), 0.0);
        assertEquals("1.0 - 2.0i", results.toString());
        assertEquals("complexSrc", results.getCalculationSourceName());
    }

    @Test
    @DisplayName("CalculationOptimized 六种 BigDecimal 运算与非法运算符")
    void calculationOptimized() {
        assertEquals("4.0", CalculationOptimized.calculation('+',
                new BigDecimal("1.5"), new BigDecimal("2.5")).toString());
        assertEquals("-1.0", CalculationOptimized.calculation('-',
                new BigDecimal("1.5"), new BigDecimal("2.5")).toString());
        assertEquals("3.75", CalculationOptimized.calculation('*',
                new BigDecimal("1.5"), new BigDecimal("2.5")).toString());
        assertEquals("0.6", CalculationOptimized.calculation('/',
                new BigDecimal("1.5"), new BigDecimal("2.5")).toString());
        assertEquals("3", CalculationOptimized.calculation('%',
                new BigDecimal("7"), new BigDecimal("4")).toString());
        assertEquals("8", CalculationOptimized.calculation('^',
                new BigDecimal("2"), new BigDecimal("3")).toString());
        // scale 由 BigDecimal 原样保留：0.10 + 0.20 = 0.30
        assertEquals("0.30", CalculationOptimized.calculation('+',
                new BigDecimal("0.10"), new BigDecimal("0.20")).toString());
        assertEquals("0.3", CalculationOptimized.calculation('+',
                new BigDecimal("0.10"), new BigDecimal("0.20")).stripTrailingZeros().toString());

        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> CalculationOptimized.calculation('&',
                        BigDecimal.ONE, BigDecimal.ONE)), "错误的计算模式");
    }

    @Test
    @DisplayName("LogResults 结果读写、来源与不支持的操作")
    void logResults() {
        LogResults logResults = new LogResults("EngineProbeLog");
        assertEquals("EngineProbeLog", logResults.getName());
        assertEquals("EngineProbeLog", logResults.getCalculationSourceName());
        assertNull(logResults.getResult());
        logResults.setResult(42.0);
        assertEquals(42.0, logResults.getResult());
        assertEquals("{}", logResults.explain(VarFormatter.JSON));

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                logResults::getResultLayers), "does not support obtaining the number of layers");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> logResults.setSource("x")), "does not support setting the source");
    }

    @Test
    @DisplayName("Calculation 默认 explain 与 CalculationResults 默认高精度方法")
    void defaultInterfaceMethods() {
        Calculation calculation = SingletonEquationSolving.getInstance(uniq("CalcDefault"));
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> calculation.explain("1+2", true)), "does not have the ability to perform process analysis");

        CalculationNumberResults numberResults = new CalculationNumberResults(0, 1.0, "s");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> numberResults.explain(VarFormatter.JSON)), "no execution process record");
    }

    // ================================================================== CalculationManagement

    @Test
    @DisplayName("CalculationManagement 注册/查询/注销计算组件")
    void calculationManagementCalculationRegistry() {
        String name = uniq("mgmtCalc");
        ProbeCalculation calculation = new ProbeCalculation(name);

        assertTrue(CalculationManagement.register(calculation, true));
        assertFalse(CalculationManagement.register(calculation, true));
        assertTrue(CalculationManagement.isRegister(name));
        assertSame(calculation, CalculationManagement.getCalculationByName(name));

        assertTrue(CalculationManagement.unregister(name));
        assertFalse(CalculationManagement.unregister(name));
        assertFalse(CalculationManagement.isRegister(name));
        assertNull(CalculationManagement.getCalculationByName(name));

        ProbeCalculation other = new ProbeCalculation(uniq("mgmtCalc2"));
        assertTrue(CalculationManagement.register(other));
        assertTrue(CalculationManagement.unregister(other.getName()));
    }

    @Test
    @DisplayName("CalculationManagement 函数注册表：注册/重复注册/查询/注销")
    void calculationManagementFunctionRegistry() {
        String name = uniq("mgmtFn");
        assertTrue(CalculationManagement.unregisterF("q"));
        QFunction qFunction = new QFunction();
        assertTrue(CalculationManagement.register(qFunction));
        assertFalse(CalculationManagement.register(new QFunction()));
        assertTrue(CalculationManagement.isFunctionExist("q"));
        assertSame(qFunction, CalculationManagement.getFunctionByName("q"));
        assertNotNull(CalculationManagement.getFunctionMap());
        assertTrue(CalculationManagement.getFunctionMap().contains("q"));

        assertTrue(CalculationManagement.unregisterF("q"));
        assertFalse(CalculationManagement.unregisterF("q"));
        Mathematical_Expression.register_function(new QFunction());
    }

    @Test
    @DisplayName("CalculationManagement.getFunctionByName 未注册时抛 ExtractException")
    void calculationManagementMissingFunction() {
        assertMessageContains(assertThrows(ExtractException.class,
                () -> CalculationManagement.getFunctionByName(uniq("missingFn"))), "没有被注册到管理者中");
        assertNull(CalculationManagement.getFunctionByNameNoCheck(uniq("missingFn")));
        assertFalse(CalculationManagement.isFunctionExist(uniq("missingFn")));
    }

    @Test
    @DisplayName("CalculationManagement.register(FunctionPackage) 批量注册")
    void calculationManagementFunctionPackageRegister() {
        Mathematical_Expression.unregister_function("sin");
        try {
            assertFalse(CalculationManagement.isFunctionExist("sin"));
            CalculationManagement.register(FunctionPackage.MATH);
            assertTrue(CalculationManagement.isFunctionExist("sin"));
            assertNotNull(CalculationManagement.getFunctionByNameNoCheck("sin"));
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
    }

    /** 生成只含字母的函数名（ManyToOneNumberFunction 不允许数字与下划线）。 */
    private static String fnName(String prefix) {
        int value = SEQ.incrementAndGet();
        StringBuilder builder = new StringBuilder(prefix);
        while (value > 0) {
            builder.append((char) ('a' + value % 26));
            value /= 26;
        }
        return builder.toString();
    }

    // ================================================================== Mathematical_Expression / Options / 序列化

    @Test
    @DisplayName("Mathematical_Expression 枚举全部常量都能获取计算组件")
    void mathematicalExpressionEnumInstances() {
        Mathematical_Expression[] values = Mathematical_Expression.values();
        assertEquals(12, values.length);
        for (Mathematical_Expression value : values) {
            Calculation calculation = Mathematical_Expression.getInstance(value);
            assertNotNull(calculation);
            assertEquals(value.toString(), calculation.getName());
            assertSame(calculation, Mathematical_Expression.getInstance(value));
        }

        Calculation named = Mathematical_Expression.getInstance(
                Mathematical_Expression.cumulativeCalculation, uniq("meCum"));
        assertTrue(named instanceof CumulativeCalculation);
        assertEquals(named.getName(), Mathematical_Expression.getInstance(
                Mathematical_Expression.cumulativeCalculation, named.getName()).getName());
        assertSame(named, Mathematical_Expression.getInstance(
                Mathematical_Expression.cumulativeCalculation, named.getName()));
    }

    @Test
    @DisplayName("register_function(String) 注册、重复注册与注销")
    void registerFunctionByString() throws Exception {
        String name = fnName("probeReg");
        String expression = name + "(x)=x+1";
        assertTrue(Mathematical_Expression.register_function(expression));
        assertTrue(CalculationManagement.isFunctionExist(name));
        assertEquals(3.0, CalculationManagement.<ManyToOneNumberFunction>getFunctionByName(name).run(2.0), 0.0);

        assertFalse(Mathematical_Expression.register_function(expression));
        assertTrue(Mathematical_Expression.unregister_function(name));
        assertFalse(CalculationManagement.isFunctionExist(name));

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> Mathematical_Expression.register_function("notAFunction")), "不属于函数");
    }

    @Test
    @DisplayName("register_function(Class) 依据 @Functions 注解批量注册")
    void registerFunctionByClass() throws Exception {
        assertTrue(Mathematical_Expression.register_function(AnnotatedFunctionsHolder.class));
        assertTrue(CalculationManagement.isFunctionExist("probeAnnoA"));
        assertTrue(CalculationManagement.isFunctionExist("probeAnnoB"));
        assertEquals(2.0, CalculationManagement.<ManyToOneNumberFunction>getFunctionByName("probeAnnoA").run(1.0), 0.0);
        assertEquals(6.0, CalculationManagement.<ManyToOneNumberFunction>getFunctionByName("probeAnnoB").run(3.0), 0.0);
        assertFalse(Mathematical_Expression.register_function(PlainHolder.class));
    }

    @Test
    @DisplayName("register_jvm_function 字符串与注解类两条路径")
    void registerJvmFunction() {
        Mathematical_Expression.register_jvm_function("probeJvmOne(x) = x + 1");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double probeJvmOne"));
        assertTrue(Mathematical_Expression.register_jvm_function(AnnotatedFunctionsHolder.class));
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double probeAnnoA"));
        assertFalse(Mathematical_Expression.register_jvm_function(PlainHolder.class));
    }

    @Test
    @DisplayName("register_function(Function) 与 unregister_function(Function)")
    void registerFunctionInstance() {
        String name = fnName("probeFn");
        ManyToOneNumberFunction function = new ManyToOneNumberFunction(name) {
            @Override
            public double run(double... numbers) {
                return numbers[0] * 10;
            }
        };
        assertTrue(Mathematical_Expression.register_function(function));
        assertSame(function, Mathematical_Expression.getFunction(name));
        assertEquals(50.0, Mathematical_Expression.getFunction(name).run(5.0), 0.0);
        assertTrue(Mathematical_Expression.unregister_function(function));
        assertFalse(CalculationManagement.isFunctionExist(name));
    }

    @Test
    @DisplayName("register_function(FunctionPackage) 与函数表查询")
    void registerFunctionPackage() {
        Mathematical_Expression.register_function(FunctionPackage.MATH);
        assertNotNull(Mathematical_Expression.getFunction("sum"));
        assertTrue(Mathematical_Expression.getFunctionMap().contains("sum"));
        assertTrue(Mathematical_Expression.getFunctionMap().contains("probeAdd"));
        assertEquals(6.0, Mathematical_Expression.getFunction("sum").run(1.0, 2.0, 3.0), 0.0);
    }

    @Test
    @DisplayName("saveFunction(File) 与 register_function(File) 的序列化往返")
    void saveAndRegisterFunctionFile() throws Exception {
        File file = tempDir.resolve("probeAdd.fn").toFile();
        Mathematical_Expression.saveFunction(new ProbeAddFunction(), file);
        assertTrue(file.isFile());
        assertTrue(file.length() > 0);

        Mathematical_Expression.unregister_function("probeAdd");
        assertFalse(CalculationManagement.isFunctionExist("probeAdd"));
        assertTrue(Mathematical_Expression.register_function(file));
        assertTrue(CalculationManagement.isFunctionExist("probeAdd"));
        assertEquals(5.0, CalculationManagement.<ManyToOneNumberFunction>getFunctionByName("probeAdd").run(4.0), 0.0);

        File plainFile = tempDir.resolve("plain.fn").toFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(plainFile))) {
            out.writeObject("just a string, not a function");
        }
        assertFalse(Mathematical_Expression.register_function(plainFile));
    }

    @Test
    @DisplayName("saveFunction(OutputStream) 与 register_function(InputStream) 的计数二元组")
    void saveAndRegisterFunctionStream() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Mathematical_Expression.saveFunction(buffer, new ProbeAddFunction());
        assertTrue(buffer.size() > 0);

        Mathematical_Expression.unregister_function("probeAdd");
        java.util.Map.Entry<Integer, Integer> entry =
                Mathematical_Expression.register_function(new ByteArrayInputStream(buffer.toByteArray()));
        assertEquals(1, entry.getKey());
        assertEquals(0, entry.getValue());

        // 使用静态嵌套类（可序列化），匿名内部类会捕获测试实例导致 NotSerializableException
        Mathematical_Expression.unregister_function("probeAdd");
        ByteArrayOutputStream mixed = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(mixed)) {
            out.writeObject(new ProbeAddFunction());
            out.writeObject("not a function");
        }
        java.util.Map.Entry<Integer, Integer> mixedEntry =
                Mathematical_Expression.register_function(new ByteArrayInputStream(mixed.toByteArray()));
        assertEquals(1, mixedEntry.getKey());
        assertEquals(1, mixedEntry.getValue());
    }

    @Test
    @DisplayName("Options 的 BigDecimal 开关、按 int/字符串缓存与清理")
    void optionsSwitchesAndCaches() {
        assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
        Mathematical_Expression.Options.setUseBigDecimal(true);
        assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
        Mathematical_Expression.Options.setUseBigDecimal(false);
        assertFalse(Mathematical_Expression.Options.isUseBigDecimal());

        assertEquals(Boolean.FALSE, Mathematical_Expression.Options.isUseCache());
        Mathematical_Expression.Options.setUseCache(true);
        assertEquals(Boolean.TRUE, Mathematical_Expression.Options.isUseCache());

        Mathematical_Expression.Options.cacheCalculation(4321, 4.5);
        assertEquals(4.5, Mathematical_Expression.Options.getCacheCalculation(4321).doubleValue(), 0.0);

        PackExpression pack = new PackExpression("pack", PrefixExpression.compile("1+2", "packInner"), "packName");
        Mathematical_Expression.Options.cacheCalculation("formulaKey", pack);
        assertSame(pack, Mathematical_Expression.Options.getCacheCalculation("formulaKey"));

        Mathematical_Expression.Options.setUseCache(false);
        assertEquals(Boolean.FALSE, Mathematical_Expression.Options.isUseCache());
        assertNull(Mathematical_Expression.Options.getCacheCalculation(4321));
        assertNull(Mathematical_Expression.Options.getCacheCalculation("formulaKey"));
    }

    @Test
    @DisplayName("Options 缓存开启后表达式编译复用同一对象")
    void optionsExpressionCacheReuse() {
        Mathematical_Expression.Options.setUseCache(true);
        try {
            PrefixExpression prefix = PrefixExpression.compile("1+2", "cachedPrefix");
            assertSame(prefix, PrefixExpression.compile("1+2", "cachedPrefix"));
            PrefixExpression bigPrefix = PrefixExpression.compileBigDecimal("1+2", "cachedBigPrefix");
            assertSame(bigPrefix, PrefixExpression.compileBigDecimal("1+2", "cachedBigPrefix"));

            PackExpression bracketPack = brackets().compile("(1+2)*3", true);
            assertSame(bracketPack, brackets().compile("(1+2)*3", true));
            PackExpression bigBracketPack = brackets().compileBigDecimal("(1+2)*3", true);
            assertSame(bigBracketPack, brackets().compileBigDecimal("(1+2)*3", true));

            CumulativeCalculation cumulative = CumulativeCalculation.getInstance(uniq("cachedCum"));
            PackExpression cumulativePack = cumulative.compile("n[0,2,1](n)", true);
            assertSame(cumulativePack, cumulative.compile("n[0,2,1](n)", true));
            assertEquals(3.0, numeric(cumulativePack.calculation(false)), 0.0);
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
    }

    // ================================================================== PrefixExpression / PrefixExpressionOperation

    @Test
    @DisplayName("PrefixExpression.isBackIsOpt 收集数值、小数点、负号与阶乘符号")
    void prefixExpressionIsBackIsOpt() {
        StringBuilder builder = new StringBuilder();
        PrefixExpression.isBackIsOpt(builder, '7');
        assertEquals("7", builder.toString());

        builder.setLength(0);
        PrefixExpression.isBackIsOpt(builder, '.');
        assertEquals(".", builder.toString());

        builder.setLength(0);
        PrefixExpression.isBackIsOpt(builder, '-');
        assertEquals("-", builder.toString());

        builder.setLength(0);
        PrefixExpression.isBackIsOpt(builder, '!');
        assertEquals("!", builder.toString());

        builder.setLength(0);
        PrefixExpression.isBackIsOpt(builder, '+');
        PrefixExpression.isBackIsOpt(builder, '*');
        assertEquals("", builder.toString());
    }

    @Test
    @DisplayName("PrefixExpression.compile 未格式化时按从左到右求值（1+2*3 => 9）")
    void prefixExpressionRawCompile() {
        PrefixExpression expression = PrefixExpression.compile("1+2*3", "rawPrefix");
        assertFalse(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        assertTrue(expression.getExpressionStr().startsWith("1+2*3 -> "));

        // 引擎按前缀栈顺序计算：(1+2)*3 = 9
        assertEquals(9.0, numeric(expression.calculation(true)), 0.0);
        assertEquals(2, expression.calculation(true).getResultLayers());
        assertEquals(9.0, numeric(expression.calculation(true)), 0.0);
    }

    @Test
    @DisplayName("PrefixExpression.compileBigDecimal 高精度栈与结果")
    void prefixExpressionBigDecimalCompile() {
        PrefixExpression expression = PrefixExpression.compileBigDecimal("1+2*3", "bigPrefix");
        assertTrue(expression.isBigDecimal());
        assertFalse(expression.isUnBigDecimal());
        assertEquals(9.0, numeric(expression.calculationBigDecimals(true)), 0.0);
        assertEquals(2, expression.calculationBigDecimals(true).getResultLayers());
        assertEquals(3, expression.getBigDecimalsR(false).size());
        assertTrue(expression.getExpressionStr().contains(" -> "));
    }

    @Test
    @DisplayName("PrefixExpression 栈访问：isCopy 语义与副本对象身份")
    void prefixExpressionStackAccess() {
        PrefixExpression expression = PrefixExpression.compile("1+2*3", "stackPrefix");
        Stack<Double> same = expression.getDoubleStack(false);
        assertSame(same, expression.getDoubleStack(false));
        Stack<Double> copy = expression.getDoubleStack(true);
        assertNotSame(same, copy);
        assertEquals(same, copy);
        assertEquals(3, copy.size());

        Stack<Character> characters = expression.getCharacterStack(false);
        assertSame(characters, expression.getCharacterStack(false));
        assertNotSame(characters, expression.getCharacterStack(true));
        assertEquals(List.of('+', '*'), new ArrayList<>(characters));

        assertNull(expression.getBigDecimalsR(false));
    }

    @Test
    @DisplayName("PrefixExpression 计算后 isCopy=false 令组件不可用")
    void prefixExpressionBecomesUnavailable() {
        PrefixExpression expression = PrefixExpression.compile("1+2", "consumedPrefix");
        assertEquals(3.0, expression.calculationCache(false).getResult(), 0.0);
        assertFalse(expression.isAvailable());
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> expression.getDoubleStack(true)), "不再可用");
        assertThrows(UnsupportedOperationException.class, () -> expression.getCharacterStack(false));
        assertThrows(UnsupportedOperationException.class, () -> expression.getBigDecimalsR(false));
    }

    @Test
    @DisplayName("PrefixExpression 由非精度升级为多精度支持")
    void prefixExpressionConvertFromDouble() {
        PrefixExpression expression = PrefixExpression.compile("1+2", "convertPrefix");
        assertFalse(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());

        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        // 已支持双模式时再次调用直接返回
        expression.convertToMultiPrecisionSupported();

        assertEquals(3.0, numeric(expression.calculationBigDecimals(true)), 0.0);
        assertEquals(3.0, numeric(expression.calculation(true)), 0.0);
    }

    @Test
    @DisplayName("PrefixExpression 由高精度升级为多精度支持")
    void prefixExpressionConvertFromBigDecimal() {
        PrefixExpression expression = PrefixExpression.compileBigDecimal("1+2", "convertBigPrefix");
        assertTrue(expression.isBigDecimal());
        assertFalse(expression.isUnBigDecimal());

        expression.convertToMultiPrecisionSupported();
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        expression.convertToMultiPrecisionSupported();

        assertEquals(3.0, numeric(expression.calculation(true)), 0.0);
        assertEquals(3.0, numeric(expression.calculationBigDecimals(true)), 0.0);
    }

    @Test
    @DisplayName("PrefixExpression 模式不匹配时的两种拒绝")
    void prefixExpressionModeMismatch() {
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> PrefixExpression.compileBigDecimal("1+2", "bigOnly").calculation(true)),
                "高精度的类型");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> PrefixExpression.compile("1+2", "doubleOnly").calculationBigDecimals(true)),
                "不是一个高精度的类型");
    }

    @Test
    @DisplayName("PrefixExpression.calculationBigDecimalsCache 缓存路径")
    void prefixExpressionBigDecimalCache() {
        PrefixExpression expression = PrefixExpression.compileBigDecimal("1+2", "bigCachePrefix");
        assertEquals(3.0, expression.calculationBigDecimalsCache(true).getResult(), 0.0);
        assertEquals(3.0, expression.calculationBigDecimalsCache(true).getResult(), 0.0);

        PrefixExpression consumed = PrefixExpression.compileBigDecimal("4+5", "bigCacheConsumed");
        assertEquals(9.0, consumed.calculationBigDecimalsCache(false).getResult(), 0.0);
        assertFalse(consumed.isAvailable());
    }

    @Test
    @DisplayName("PrefixExpressionOperation.check 的通过/末位非数值/括号/未知符号/缺操作数分支")
    void prefixExpressionOperationCheck() throws Exception {
        PrefixExpressionOperation operation =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        operation.check("1+2");
        operation.check("12 ");
        operation.check("1-2");

        assertMessageContains(assertThrows(WrongFormat.class, () -> operation.check("1+")),
                "最后一个字符应是一个数值");
        assertMessageContains(assertThrows(WrongFormat.class, () -> operation.check("(1)+2")),
                "只能解析无括号");
        assertMessageContains(assertThrows(WrongFormat.class, () -> operation.check("1+2a3")),
                "未知符号");
        assertMessageContains(assertThrows(WrongFormat.class, () -> operation.check("1++2")),
                "缺失了一个运算数值");
    }

    @Test
    @DisplayName("PrefixExpressionOperation 格式化与两种计算模式（1/3 在 BigDecimal 下抛异常）")
    void prefixExpressionOperationCalculationModes() {
        PrefixExpressionOperation operation =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        assertEquals("1-2+0", operation.formatStr("1+-2"));
        assertEquals(7.0, operation.calculation("1+2*3", true).getResult(), 0.0);
        assertEquals(2, operation.calculation("1+2*3", true).getResultLayers());

        // 默认 isUseBigDecimal()==false -> 走 BigDecimal 编译分支，1/3 无法精确表示
        assertThrows(ArithmeticException.class, () -> operation.calculation("1/3", true));

        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            assertEquals(1.0 / 3.0, operation.calculation("1/3", true).getResult(), 1e-12);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
    }

    @Test
    @DisplayName("PrefixExpressionOperation.explain 两级与 addResult 分支")
    void prefixExpressionOperationExplain() throws Exception {
        PrefixExpressionOperation operation =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        LogResults summary = operation.explain("1+2*3", true);
        assertEquals(7.0, summary.getResult());

        Method explain3 = method(PrefixExpressionOperation.class,
                "explain", String.class, boolean.class, boolean.class);
        LogResults noResult = (LogResults) call(explain3, operation, "1+2*3", true, false);
        assertEquals(7.0, noResult.getResult());
        LogResults withResult = (LogResults) call(explain3, operation, "1+2*3", true, true);
        assertEquals(7.0, withResult.getResult());

        LogResults raw = operation.explain("1+2*3", false);
        assertEquals(9.0, raw.getResult());
    }

    @Test
    @DisplayName("PrefixExpressionOperation.compile 的 formatRequired 开关")
    void prefixExpressionOperationCompile() {
        PrefixExpressionOperation operation =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        assertEquals(9.0, operation.compile("1+2*3", false).calculation(true).getResult(), 0.0);
        assertEquals(7.0, operation.compile("1+2*3", true).calculation(true).getResult(), 0.0);
        assertEquals(7.0, operation.compileBigDecimal("1+2*3", true)
                .calculationBigDecimals(true).getResult(), 0.0);
        assertEquals(9.0, operation.compileBigDecimal("1+2*3", false)
                .calculationBigDecimals(true).getResult(), 0.0);
    }

    @Test
    @DisplayName("PrefixExpressionOperation.getInstance 复用与类型冲突")
    void prefixExpressionOperationGetInstance() {
        PrefixExpressionOperation shared =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        assertSame(shared, PrefixExpressionOperation.getInstance(
                CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME));

        PrefixExpressionOperation fresh = PrefixExpressionOperation.getInstance(uniq("prefixOp"));
        assertSame(fresh, PrefixExpressionOperation.getInstance(fresh.getName()));

        assertMessageContains(assertThrows(ExtractException.class,
                () -> PrefixExpressionOperation.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 PrefixExpressionOperation 类型");
    }

    // ================================================================== BracketsCalculation / BracketsCalculation2 / BracketsExpression

    @Test
    @DisplayName("括号计算：1+2*3=7、(1+2)*3=9 与层数")
    void bracketsCalculationValues() {
        CalculationNumberResults precedence = brackets().calculation("1+2*3");
        assertEquals(7.0, precedence.getResult(), 0.0);
        assertEquals(2, precedence.getResultLayers());
        assertEquals(CalculationManagement.BRACKETS_CALCULATION_2_NAME
                + ":{PrefixExpressionOperation}", precedence.getCalculationSourceName());

        CalculationNumberResults nested = brackets().calculation("(1+2)*3");
        assertEquals(9.0, nested.getResult(), 0.0);
        // 括号先折叠为 3.0，剩余 "3.0*3+0" 只有一个 +0 尾缀运算符
        assertEquals(1, nested.getResultLayers());
    }

    @Test
    @DisplayName("括号计算对空串/残缺输入的宽容行为")
    void bracketsCalculationLaxInputs() {
        assertEquals(0.0, brackets().calculation("").getResult(), 0.0);
        assertEquals(1.0, brackets().calculation("1+").getResult(), 0.0);
        assertEquals(0.0, brackets().calculation("((1+2)").getResult(), 0.0);
    }

    @Test
    @DisplayName("括号计算 null 输入抛 NPE")
    void bracketsCalculationNull() {
        assertThrows(NullPointerException.class, () -> brackets().calculation(null));
    }

    @Test
    @DisplayName("BracketsCalculation.formatStr 去空白")
    void bracketsCalculationFormatStr() {
        assertEquals("1+2", brackets().formatStr(" 1 + 2 "));
        assertEquals("(1+2)*3", brackets().formatStr("(1 + 2) * 3"));
        assertEquals("", brackets().formatStr("   "));
    }

    @Test
    @DisplayName("BracketsCalculation2.explain 的过程记录结果")
    void bracketsCalculationExplain() {
        assertEquals(7.0, brackets().explain("1+2*3", true).getResult());
        assertEquals(9.0, brackets().explain("(1+2)*3", true).getResult());
        assertEquals(9.0, brackets().explain("(1+2)*3", false).getResult());
    }

    @Test
    @DisplayName("BracketsCalculation2.compile/compileBigDecimal 生成包装表达式")
    void bracketsCalculationCompile() {
        PackExpression doublePack = brackets().compile("(1+2)*3", true);
        assertEquals(9.0, numeric(doublePack.calculation(false)), 0.0);
        assertTrue(doublePack.getExpressionStr().contains(" -> "));
        assertEquals(CalculationManagement.BRACKETS_CALCULATION_2_NAME + ":{PrefixExpressionOperation}",
                doublePack.getCalculationName());

        PackExpression bigPack = brackets().compileBigDecimal("(1+2)*3", true);
        assertEquals(9.0, numeric(bigPack.calculationBigDecimals(false)), 0.0);
    }

    @Test
    @DisplayName("NumberCalculation.check 的合法性/空串/末位/括号/未知符号/重复运算符")
    void numberCalculationCheck() throws Exception {
        BracketsCalculation2 calculation = brackets();
        calculation.check("(1+2)");
        calculation.check("1 + 2 ");
        calculation.check("1!+2");

        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("")),
                "表达式为空字符串");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1+")),
                "最后一个字符不是一个数值");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("(1+2")),
                "括号对数");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1+2)")),
                "括号对数");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1&2")),
                "未知符号");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1**2")),
                "缺失了一个运算数值");
    }

    @Test
    @DisplayName("已废弃的 BracketsCalculation.calculation2 二元公式解析")
    void bracketsCalculationDeprecatedCalculation2() {
        BracketsCalculation calculation = brackets();
        // calculation2 的实参顺序是反的：a 是运算符右侧、b 是左侧
        assertEquals(3.0, calculation.calculation2("1+2"), 0.0);
        assertEquals(-1.0, calculation.calculation2("3-2"), 0.0);
        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> calculation.calculation2("12")), "公式格式错误");
    }

    @Test
    @DisplayName("BigDecimal 模式下的括号计算（1/3 无法精确表示）")
    void bracketsCalculationBigDecimalMode() {
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            CalculationNumberResults results = brackets().calculation("1+2*3");
            assertEquals(7.0, results.getResult(), 0.0);
            assertEquals(2, results.getResultLayers());
            assertThrows(ArithmeticException.class, () -> brackets().calculation("1/3"));
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
    }

    @Test
    @DisplayName("BracketsCalculation2.getInstance 复用与类型冲突")
    void bracketsCalculationGetInstance() {
        BracketsCalculation2 shared =
                BracketsCalculation2.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME);
        assertSame(shared, BracketsCalculation2.getInstance(
                CalculationManagement.BRACKETS_CALCULATION_2_NAME));

        String wrongTypeName = PrefixExpressionOperation.getInstance(uniq("bracketWrong")).getName();
        assertMessageContains(assertThrows(ExtractException.class,
                () -> BracketsCalculation2.getInstance(wrongTypeName)),
                "不属于 BracketsCalculation2 类型");
    }

    /** 直接暴露 protected 表达式缓冲与状态的可观测子类。 */
    static final class ProbeNameExpression extends NameExpression {
        ProbeNameExpression(String expression, String name) {
            super(expression, name);
        }

        @Override
        public CalculationResults calculation(boolean isCopy) {
            return new CalculationNumberResults(1, 3.0, getCalculationName());
        }

        @Override
        public CalculationResults calculationBigDecimals(boolean isCopy) {
            return new CalculationBigDecimalResults(1, new BigDecimal("3.00"), getCalculationName());
        }

        @Override
        public boolean isBigDecimal() {
            return true;
        }

        @Override
        public boolean isUnBigDecimal() {
            return true;
        }

        @Override
        public void convertToMultiPrecisionSupported() {
        }

        String builderText() {
            return getExpressionStrBuilder().toString();
        }
    }

    static final class ProbeSubCompileExpression extends SubCompileExpression {
        private final boolean notNeed;
        private final NameExpression compiled;

        ProbeSubCompileExpression(String expression, String name, boolean notNeed, NameExpression compiled) {
            super(expression, name);
            this.notNeed = notNeed;
            this.compiled = compiled;
        }

        @Override
        public NameExpression subCompile(boolean isCopy) {
            return compiled;
        }

        @Override
        public NameExpression subCompileBigDecimals(boolean isCopy) {
            return compiled;
        }

        @Override
        public boolean notNeedSubCompile() {
            return notNeed;
        }

        @Override
        public boolean isBigDecimal() {
            return true;
        }

        @Override
        public boolean isUnBigDecimal() {
            return true;
        }

        @Override
        public void convertToMultiPrecisionSupported() {
        }
    }

    // ================================================================== NameExpression / PackExpression / SubCompileExpression

    @Test
    @DisplayName("NameExpression 名称、表达式缓冲、缓存与可用性开关")
    void nameExpressionBasics() {
        ProbeNameExpression expression = new ProbeNameExpression("1+2", "neName");
        assertEquals("1+2", expression.getExpressionStr());
        assertEquals("1+2", expression.builderText());
        assertEquals("neName", expression.getCalculationName());
        assertTrue(expression.isAvailable());
        assertNull(expression.getCache());

        CalculationResults first = expression.calculationCache(false);
        assertEquals(3.0, numeric(first), 0.0);
        assertSame(first, expression.getCache());
        assertFalse(expression.isAvailable());
        // 缓存命中不再重新计算
        assertSame(first, expression.calculationCache(false));
    }

    @Test
    @DisplayName("NameExpression 高精度缓存与手工设置缓存")
    void nameExpressionBigDecimalCache() {
        ProbeNameExpression expression = new ProbeNameExpression("1+2", "neBigName");
        CalculationResults results = expression.calculationBigDecimalsCache(true);
        assertEquals(3.0, numeric(results), 0.0);
        assertTrue(expression.isAvailable());
        assertEquals("3.00", ((CalculationBigDecimalResults) expression.getCache())
                .getBigDecimalResult().toString());

        CalculationNumberResults manual = new CalculationNumberResults(0, 9.0, "manual");
        expression.setCache(manual);
        assertSame(manual, expression.getCache());
    }

    @Test
    @DisplayName("PackExpression 包装/还原/计算")
    void packExpression() {
        PrefixExpression inner = PrefixExpression.compile("1+2", "packInner");
        PackExpression pack = new PackExpression("wrapper", inner, "packName");
        assertEquals("wrapper -> 1+2 -> 1.0+0", pack.getExpressionStr());
        assertEquals("packName:{packInner}", pack.getCalculationName());
        assertSame(inner, pack.dismantling());
        assertFalse(pack.isBigDecimal());
        assertTrue(pack.isUnBigDecimal());
        assertEquals(3.0, numeric(pack.calculation(true)), 0.0);

        pack.convertToMultiPrecisionSupported();
        assertTrue(pack.isBigDecimal());
        assertEquals(3.0, numeric(pack.calculationBigDecimals(true)), 0.0);

        PrefixExpression unnamedInner = PrefixExpression.compile("4+5", "packInner2");
        PackExpression unnamed = new PackExpression(null, unnamedInner, "p2");
        assertEquals("4+5 -> 4.0+0", unnamed.getExpressionStr());
        assertEquals("p2:{packInner2}", unnamed.getCalculationName());
    }

    @Test
    @DisplayName("SubCompileExpression.notNeedSubCompile=true 时直接走括号计算")
    void subCompileExpressionWithoutCompile() {
        ProbeSubCompileExpression expression =
                new ProbeSubCompileExpression("1+2", "sceDirect", true, null);
        assertTrue(expression.notNeedSubCompile());
        assertEquals(3.0, numeric(expression.calculation(true)), 0.0);
        assertEquals(3.0, numeric(expression.calculationBigDecimals(true)), 0.0);
    }

    @Test
    @DisplayName("SubCompileExpression.notNeedSubCompile=false 时委托 subCompile")
    void subCompileExpressionWithCompile() {
        ProbeNameExpression compiled = new ProbeNameExpression("1+2", "sceCompiled");
        ProbeSubCompileExpression expression =
                new ProbeSubCompileExpression("1+2", "sceDelegating", false, compiled);
        assertFalse(expression.notNeedSubCompile());
        assertEquals(3.0, numeric(expression.calculation(true)), 0.0);
        assertEquals(3.0, numeric(expression.calculationBigDecimals(true)), 0.0);
    }

    // ================================================================== FunctionExpression

    @Test
    @DisplayName("FunctionExpression.compile 的元数据与浅层计算")
    void functionExpressionCompileAndCalculate() {
        FunctionExpression expression = FunctionExpression.compile("q(1+1)", uniq("fe"));
        assertEquals("q(1+1)", expression.getExpressionStr());
        assertEquals(List.of("q"), new ArrayList<>(expression.getFunNames()));
        assertFalse(expression.notNeedSubCompile());
        assertTrue(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        expression.convertToMultiPrecisionSupported();

        CalculationNumberResults results = expression.calculationCache(true);
        assertEquals(4.0, results.getResult(), 0.0);
        assertEquals(expression.getCalculationName(), results.getCalculationSourceName());
        assertEquals(4.0, expression.calculationBigDecimalsCache(true).getResult(), 0.0);
    }

    @Test
    @DisplayName("FunctionExpression.static subCompile 的数值/无函数/含函数三条路径")
    void functionExpressionStaticSubCompile() {
        assertEquals("123", FunctionExpression.subCompile("123",
                new Stack<>(), new Stack<>(), new Stack<>()).toString());
        assertEquals("1+2", FunctionExpression.subCompile("1+2",
                new Stack<>(), new Stack<>(), new Stack<>()).toString());

        Stack<Integer> start = new Stack<>();
        Stack<Integer> end = new Stack<>();
        Stack<String> names = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction("q(1+1)", start, end, names);
        assertEquals("4.0", FunctionExpression.subCompile("q(1+1)", start, end, names).toString());
    }

    @Test
    @DisplayName("FunctionExpression.setFunName 通过引用改写表达式文本")
    void functionExpressionSetFunName() {
        FunctionExpression expression = FunctionExpression.compile("q(1+1)", "renameFe");
        assertEquals(List.of("q"), new ArrayList<>(expression.getFunNames()));
        expression.setFunName(0, "probeAdd");
        assertEquals("probeAdd(1+1)", expression.getExpressionStr());
        assertEquals(List.of("probeAdd"), new ArrayList<>(expression.getFunNames()));
    }

    @Test
    @DisplayName("FunctionExpression 的 start/end/names 三栈拷贝语义与不可用分支")
    void functionExpressionProtectedStacks() {
        FunctionExpression expression = FunctionExpression.compile("q(1+1)", "stackFe");
        Method getStart = method(FunctionExpression.class, "getStart", boolean.class);
        Method getEnd = method(FunctionExpression.class, "getEnd", boolean.class);
        Method getFunNames = method(FunctionExpression.class, "getFunNames", boolean.class);

        assertEquals(List.of(2), new ArrayList<>((Stack<Integer>) call(getStart, expression, false)));
        assertEquals(List.of(5), new ArrayList<>((Stack<Integer>) call(getEnd, expression, false)));
        assertEquals(List.of("q"), new ArrayList<>((Stack<String>) call(getFunNames, expression, false)));

        // isCopy=true 每次都给出新副本
        assertNotSame(call(getStart, expression, true), call(getStart, expression, true));
        // isCopy=false 返回同一引用
        assertSame(call(getStart, expression, false), call(getStart, expression, false));

        call(method(NameExpression.class, "closeAvailable"), expression);
        assertFalse(expression.isAvailable());
        assertMessageContains(failure(getStart, expression, false), "不再可用");
        assertMessageContains(failure(getEnd, expression, true), "不再可用");
        assertMessageContains(failure(getFunNames, expression, false), "不再可用");
    }

    @Test
    @DisplayName("FunctionExpression.subCompile/subCompileBigDecimals 与消费后的状态")
    void functionExpressionDeepCompile() {
        FunctionExpression doubleExpression = FunctionExpression.compile("q(1+1)", "deepFe");
        NameExpression compiled = doubleExpression.subCompile(true);
        assertNotNull(compiled);
        assertEquals(4.0, numeric(compiled.calculation(true)), 0.0);

        FunctionExpression bigExpression = FunctionExpression.compile("q(1+1)", "deepBigFe");
        NameExpression bigCompiled = bigExpression.subCompileBigDecimals(true);
        assertNotNull(bigCompiled);
        assertEquals(4.0, numeric(bigCompiled.calculationBigDecimals(true)), 0.0);

        FunctionExpression consumed = FunctionExpression.compile("q(1+1)", "consumedFe");
        assertEquals(4.0, consumed.calculationCache(false).getResult(), 0.0);
        assertFalse(consumed.isAvailable());
        // 栈已被消费 -> notNeedSubCompile() 变 true，子类入口抛出“不需要深度编译”
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> consumed.subCompile(false)), "不需要深度编译");

        FunctionExpression plain = FunctionExpression.compile("1+2", "plainFe");
        assertTrue(plain.notNeedSubCompile());
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> plain.subCompile(true)), "不需要深度编译");
        assertEquals(3.0, numeric(plain.calculation(true)), 0.0);
        assertEquals(3.0, numeric(plain.calculationBigDecimals(true)), 0.0);
    }

    // ================================================================== FunctionFormulaCalculation / FunctionFormulaCalculation2

    @Test
    @DisplayName("FunctionFormulaCalculation2 数字短路与共享池结果复用")
    void functionFormulaCalculation2NumberAndSharedResults() {
        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        assertTrue(calculation.isStartSharedPool());

        CalculationNumberResults numeric = calculation.calculation("7", true);
        assertEquals(7.0, numeric.getResult(), 0.0);
        assertEquals(0, numeric.getResultLayers());
        assertEquals("7", numeric.getCalculationSourceName());

        CalculationNumberResults first = calculation.calculation("sum(1,2)", true);
        assertEquals(3.0, numeric(first), 0.0);
        assertSame(first, calculation.calculation("sum(1,2)", true));
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2.check 缓存共享池与括号不闭环")
    void functionFormulaCalculation2Check() throws Exception {
        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.check("sum(1,2)");
        assertTrue(calculation.isCache("sum(1,2)"));
        // 命中共享池后直接返回
        calculation.check("sum(1,2)");
        assertEquals(3.0, calculation.calculation("sum(1,2)", true).getResult(), 0.0);

        calculation.clearCache();
        assertFalse(calculation.isCache("sum(1,2)"));

        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("sum(1,2")), "缺少起始或结束括号");
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2.explain 的多层展开")
    void functionFormulaCalculation2Explain() {
        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        assertEquals(3.0, calculation.explain("sum(1,2)", true).getResult());
        // 参数是子表达式时会写入子日志
        assertEquals(4.0, calculation.explain("sum(1+1,2)", true).getResult());
        assertEquals(3.0, calculation.explain("1+2", true).getResult());
        assertEquals(3.0, calculation.explain("1+2", false).getResult());
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2 关闭共享池后的计算与检查")
    void functionFormulaCalculation2WithoutSharedPool() throws Exception {
        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        calculation.setStartSharedPool(false);
        try {
            assertFalse(calculation.isStartSharedPool());
            assertEquals(3.0, calculation.calculation("sum(1,2)", true).getResult(), 0.0);
            assertEquals(4.0, calculation.calculation("sum(2,2)", true).getResult(), 0.0);
            calculation.check("sum(1,2)");
            calculation.check("sum(1,2)");
            assertFalse(calculation.isCache("sum(1,2)"));
            assertFalse(calculation.isStartSharedPool());
        } finally {
            calculation.setStartSharedPool(true);
        }
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2.compile/compileBigDecimal")
    void functionFormulaCalculation2Compile() {
        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        FunctionExpression compiled = calculation.compile("sum(1,2)", true);
        assertEquals(3.0, numeric(compiled.calculation(false)), 0.0);
        FunctionExpression bigCompiled = calculation.compileBigDecimal("sum(1,2)", true);
        assertEquals(3.0, numeric(bigCompiled.calculationBigDecimals(false)), 0.0);
    }

    @Test
    @DisplayName("FunctionParameterExtraction 静态与实例两种入口")
    void functionParameterExtraction() {
        Stack<Integer> start = new Stack<>();
        Stack<Integer> end = new Stack<>();
        Stack<String> names = new Stack<>();
        FunctionFormulaCalculation2.FunctionParameterExtraction("sum(1,2)+3", start, end, names);
        assertEquals(List.of(4), new ArrayList<>(start));
        assertEquals(List.of(7), new ArrayList<>(end));
        assertEquals(List.of("sum"), new ArrayList<>(names));

        start.clear();
        end.clear();
        names.clear();
        // 嵌套函数只登记最外层（内层字母出现在 count>0 的作用域中）
        FunctionFormulaCalculation2.FunctionParameterExtraction("sum(min(1,2),3)", start, end, names);
        assertEquals(List.of(4), new ArrayList<>(start));
        assertEquals(List.of(14), new ArrayList<>(end));
        assertEquals(List.of("sum"), new ArrayList<>(names));

        start.clear();
        end.clear();
        names.clear();
        // 同级两个函数分别登记
        FunctionFormulaCalculation2.FunctionParameterExtraction("num(1+2)*num(3)", start, end, names);
        assertEquals(List.of(4, 13), new ArrayList<>(start));
        assertEquals(List.of(7, 14), new ArrayList<>(end));
        assertEquals(List.of("num", "num"), new ArrayList<>(names));

        FunctionFormulaCalculation2 calculation =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        StringBuilder builder = new StringBuilder();
        calculation.FunctionParameterExtraction("sum(1,2)+3",
                new Stack<>(), new Stack<>(), new Stack<>(), builder);
        assertEquals("0+3", builder.toString());
    }

    @Test
    @DisplayName("旧版 FunctionFormulaCalculation 的计算与检查（单字母函数名）")
    void functionFormulaCalculationLegacy() throws Exception {
        FunctionFormulaCalculation calculation = FunctionFormulaCalculation.getInstance(uniq("legacyFf"));
        assertEquals("1+2", calculation.formatStr(" 1 + 2 "));
        assertEquals(0.0, calculation.calculation("sin(0)", true).getResult(), 0.0);
        assertEquals(Math.sin(1), calculation.calculation("sin(1)", true).getResult(), 1e-12);
        assertEquals(4.0, calculation.calculation("q(2)", true).getResult(), 0.0);
        assertEquals(5.0, calculation.calculation("q(2.5)", true).getResult(), 0.0);
        assertEquals(4.0, calculation.calculation("q(2)").getResult(), 0.0);

        calculation.check("q(1+1)");
        calculation.check("q(12)");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("q(1+1")), "缺少起始或结束括号");
        // 单字母之外的函数名会把名字长度算进实参区间，得到空实参
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("sin(1)")), "表达式为空字符串");
    }

    @Test
    @DisplayName("两个函数计算组件的 getInstance 复用与类型冲突")
    void functionFormulaCalculationGetInstance() {
        FunctionFormulaCalculation legacy = FunctionFormulaCalculation.getInstance(uniq("legacyShared"));
        assertSame(legacy, FunctionFormulaCalculation.getInstance(legacy.getName()));

        FunctionFormulaCalculation2 modern =
                FunctionFormulaCalculation2.getInstance(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME);
        assertSame(modern, FunctionFormulaCalculation2.getInstance(
                CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME));

        assertMessageContains(assertThrows(ExtractException.class,
                () -> FunctionFormulaCalculation.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 FunctionFormulaCalculation 类型");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> FunctionFormulaCalculation2.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 FunctionFormulaCalculation2 类型");
    }

    // ================================================================== CumulativeCalculation

    @Test
    @DisplayName("累加计算 formatStr 与 250 的手算结果")
    void cumulativeCalculationFormatAndResult() {
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cum"));
        assertEquals("((1+0.0*0.0))+((1+2.0*2.0))+((1+4.0*4.0))"
                        + "+((1+6.0*6.0))+((1+8.0*8.0))+((1+10.0*10.0))",
                calculation.formatStr("n[0,10,2](1+n*n)"));

        // 展开后按“非格式化前缀”从左到右求值：
        // (1+0)*0=0, (1+2)*2=6, (1+4)*4=20, (1+6)*6=42, (1+8)*8=72, (1+10)*10=110 -> 250
        assertEquals(250.0, calculation.calculation("n[0,10,2](1+n*n)").getResult(), 0.0);
        // n -> 1,2,3 时每个括号内都是纯数字，累加为 6
        assertEquals(6.0, calculation.calculation("n[1,3,1](n)").getResult(), 0.0);
        assertEquals(6.0, calculation.explain("n[1,3,1](n)", true).getResult());
        assertEquals(6.0, numeric(calculation.compile("n[1,3,1](n)", true).calculation(false)), 0.0);
        assertEquals(6.0, numeric(calculation.compileBigDecimal("n[1,3,1](n)", true)
                .calculationBigDecimals(false)), 0.0);
    }

    @Test
    @DisplayName("累加计算 check 的三条分支")
    void cumulativeCalculationCheck() throws Exception {
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cumCheck"));
        calculation.check("n[0,10,1](1+n)");
        calculation.check("m[0,2,1](1+(m)*m)");
        calculation.check("m[0,2,1](1+(m)*m)");

        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("n[0,10](1+n)")), "区间的配置不正确");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("1+2")), "公式的格式似乎不正确");
    }

    @Test
    @DisplayName("累加计算 formatRequired=false 时的原始前缀求值现状")
    void cumulativeCalculationWithoutFormat() {
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cumRaw"));
        // 未先格式化时，变量字符进入前缀解析，触发字符串下标异常（记录现状）
        assertThrows(StringIndexOutOfBoundsException.class,
                () -> calculation.calculation("m[1,2,1](m)", false));
    }

    // ================================================================== FastSumOfIntervalsBrackets / FastMultiplyOfIntervalsBrackets

    @Test
    @DisplayName("区间求和：1..10=55、两端相等退化、layers 累加")
    void fastSumCalculationValues() {
        FastSumOfIntervalsBrackets calculation = FastSumOfIntervalsBrackets.getInstance(uniq("fastSum"));
        assertEquals(1, calculation.step);

        CalculationNumberResults results = calculation.calculation("1,10");
        assertEquals(55.0, results.getResult(), 0.0);
        assertEquals(2, results.getResultLayers());
        assertEquals(calculation.getName(), results.getCalculationSourceName());

        assertEquals(55.0, calculation.calculation("1", "10").getResult(), 0.0);
        assertEquals(2.0, calculation.calculation("2,2").getResult(), 0.0);
        assertEquals(3.0, calculation.calculation("3,3").getResult(), 0.0);
    }

    @Test
    @DisplayName("区间求和共享池：命中返回同一结果对象")
    void fastSumSharedPool() throws Exception {
        FastSumOfIntervalsBrackets calculation = FastSumOfIntervalsBrackets.getInstance(uniq("fastSumPool"));
        calculation.check("3,7");
        assertTrue(calculation.isCache("3,7"));

        CalculationNumberResults first = calculation.calculation("3,7", true);
        assertEquals(25.0, first.getResult(), 0.0);
        assertSame(first, calculation.calculation("3,7", true));

        calculation.clearCache();
        assertFalse(calculation.isCache("3,7"));
    }

    @Test
    @DisplayName("区间求和 check：缓存命中、两端相等、非法表达式数")
    void fastSumCheck() throws Exception {
        FastSumOfIntervalsBrackets calculation = FastSumOfIntervalsBrackets.getInstance(uniq("fastSumCheck"));
        calculation.check("3,7");
        calculation.check("3,7");
        calculation.check("4,4");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("1")), "需要两个以逗号分割的表达式");
    }

    @Test
    @DisplayName("区间求和 compile/compileBigDecimal 与结果对象入口")
    void fastSumCompile() {
        FastSumOfIntervalsBrackets calculation = FastSumOfIntervalsBrackets.getInstance(uniq("fastSumCompile"));
        PackExpression pack = calculation.compile("1,10", true);
        assertEquals(55.0, numeric(pack.calculation(false)), 0.0);
        assertEquals("(1.0+10.0)*10.0/2 -> 11.0*10.0/2+0 -> 55.0+0", pack.getExpressionStr());

        PackExpression bigPack = calculation.compileBigDecimal("1,10", true);
        assertEquals(55.0, numeric(bigPack.calculationBigDecimals(false)), 0.0);

        CalculationNumberResults start = new CalculationNumberResults(0, 1.0, "start");
        CalculationNumberResults end = new CalculationNumberResults(0, 10.0, "end");
        CalculationNumberResults summed = calculation.calculation(start, end);
        assertEquals(55.0, summed.getResult(), 0.0);
        assertEquals(0, summed.getResultLayers());
        assertEquals(calculation.getName(), summed.getCalculationSourceName());
    }

    @Test
    @DisplayName("区间求和关闭共享池后的检查与计算")
    void fastSumSharedPoolSwitch() throws Exception {
        FastSumOfIntervalsBrackets calculation = FastSumOfIntervalsBrackets.getInstance(uniq("fastSumOff"));
        calculation.setStartSharedPool(false);
        try {
            assertFalse(calculation.isStartSharedPool());
            // 关闭共享池后只按逗号切分公式
            assertEquals(25.0, calculation.calculation("3,7", true).getResult(), 0.0);
            assertMessageContains(assertThrows(WrongFormat.class,
                    () -> calculation.check("1")), "需要两个以逗号分割的表达式");
        } finally {
            calculation.setStartSharedPool(true);
        }
    }

    @Test
    @DisplayName("区间累乘：step 默认 2，1*3*5=15、2*4*6=48")
    void fastMultiplyCalculation() {
        FastMultiplyOfIntervalsBrackets calculation =
                FastMultiplyOfIntervalsBrackets.getInstance(uniq("fastMul"));
        assertEquals(2, calculation.step);
        assertEquals(15.0, calculation.calculation("1,5").getResult(), 0.0);
        assertEquals(48.0, calculation.calculation("2,6").getResult(), 0.0);
        assertEquals(3.0, calculation.calculation("3,3").getResult(), 0.0);
        assertEquals(15.0, numeric(calculation.compile("1,5", true).calculation(false)), 0.0);
        assertEquals(48.0, numeric(calculation.compileBigDecimal("2,6", true)
                .calculationBigDecimals(false)), 0.0);

        CalculationNumberResults start = new CalculationNumberResults(0, 2.0, "s");
        CalculationNumberResults end = new CalculationNumberResults(0, 6.0, "e");
        assertEquals(48.0, calculation.calculation(start, end).getResult(), 0.0);
    }

    @Test
    @DisplayName("两个区间组件的 getInstance 类型冲突")
    void intervalComponentsGetInstance() {
        FastSumOfIntervalsBrackets sum = FastSumOfIntervalsBrackets.getInstance(uniq("fastSumShared"));
        assertSame(sum, FastSumOfIntervalsBrackets.getInstance(sum.getName()));
        FastMultiplyOfIntervalsBrackets multiply =
                FastMultiplyOfIntervalsBrackets.getInstance(uniq("fastMulShared"));
        assertSame(multiply, FastMultiplyOfIntervalsBrackets.getInstance(multiply.getName()));

        assertMessageContains(assertThrows(ExtractException.class,
                () -> FastSumOfIntervalsBrackets.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 FastSumOfIntervalsBrackets 类型");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> FastMultiplyOfIntervalsBrackets.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 FastMultiplyOfIntervalsBrackets 类型");
    }

    // ================================================================== BooleanCalculation / BooleanCalculation2

    @Test
    @DisplayName("比较表达式计算：1<2、2>=2、2==2、2<>3 及左右值")
    void booleanCalculationValues() {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("bool"));
        CalculationBooleanResults less = calculation.calculation("1<2", true);
        assertEquals(Boolean.TRUE, less.getResult());
        assertEquals(2, less.getResultLayers());
        assertEquals(1.0, less.getLeft(), 0.0);
        assertEquals(2.0, less.getRight(), 0.0);
        assertEquals(calculation.getName(), less.getCalculationSourceName());

        assertEquals(Boolean.FALSE, calculation.calculation("2<2", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("2>=2", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("3>2", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("1<=2", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("2==2", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("2<>3", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("2!=3", true).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("1 < 2", false).getResult());
        assertEquals(Boolean.TRUE, calculation.calculation("1<2").getResult());
        assertEquals(Boolean.FALSE, calculation.calculation("1 > 2", false).getResult());
    }

    @Test
    @DisplayName("比较表达式 null 操作数三支都会走到 ComparisonOperation 的异常")
    void booleanCalculationNullOperands() {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("boolNull"));
        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> calculation.calculation("null=null", true)), "错误的运算符");
        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> calculation.calculation("null<2", true)), "错误的运算符");
        assertMessageContains(assertThrows(AbnormalOperation.class,
                () -> calculation.calculation("2<null", true)), "错误的运算符");
    }

    @Test
    @DisplayName("比较表达式的 check 与 formatStr（null 操作数跳过检查）")
    void booleanCalculationCheck() throws Exception {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("boolCheck"));
        assertEquals("1<2", calculation.formatStr(" 1 < 2 "));
        calculation.check("1<2");
        calculation.check("null<2");
        calculation.check("2<null");

        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1")),
                "数量不正确的比较运算符");
        assertMessageContains(assertThrows(WrongFormat.class, () -> calculation.check("1=2=3")),
                "数量不正确的比较运算符");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> BooleanCalculation2.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 BooleanCalculation2 类型");
    }

    @Test
    @DisplayName("BooleanCalculation.setCalculation 的两个重载")
    void booleanCalculationSetCalculation() {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("boolSet"));
        calculation.setCalculation((NumberCalculation) Mathematical_Expression.getInstance(
                Mathematical_Expression.bracketsCalculation2));
        assertEquals(Boolean.TRUE, calculation.calculation("3<4", true).getResult());

        calculation.setCalculation(Mathematical_Expression.bracketsCalculation2);
        assertEquals(Boolean.FALSE, calculation.calculation("4<3", true).getResult());
    }

    // ================================================================== ComplexCalculation / ComplexExpression

    @Test
    @DisplayName("复数 check：1+2i、(1+2)+3i、缺虚部、无顶层运算符、空实部")
    void complexCalculationCheck() throws Exception {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complex"));
        calculation.check("1+2i");
        calculation.check("(1+2)+3i");

        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("1+2")), "imaginary part");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> calculation.check("1i")), "is not a complex expression");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("++2i")), "表达式为空字符串");
    }

    @Test
    @DisplayName("复数计算与 explain 的实部/虚部")
    void complexCalculationValues() {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complexValues"));
        CalculationNumberResults plus = calculation.calculation("1+2i", true);
        assertEquals(1.0, plus.getResult(), 0.0);
        assertEquals(2, plus.getResultLayers());
        assertEquals(3.0, calculation.calculation("3-2i", true).getResult(), 0.0);

        LogResults explain = calculation.explain("1+2i", true);
        assertTrue(explain.getResult() instanceof CalculationComplexResults);
        CalculationComplexResults complex = (CalculationComplexResults) explain.getResult();
        assertEquals(1.0, complex.toComplexNumber().getReal(), 0.0);
        assertEquals(2.0, complex.toComplexNumber().getImaginary(), 0.0);

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> calculation.explain("1+2", true)), "imaginary part");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> calculation.explain("1i", true)), "is not a complex expression");
    }

    @Test
    @DisplayName("复数 formatStr 与 compile 的 formatRequired 开关")
    void complexCalculationFormatAndCompile() {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complexCompile"));
        assertEquals("1+2i", calculation.formatStr(" 1 + 2 i "));
        assertEquals(1.0, calculation.compile("1+2i", false).calculationCache(false).getResult(), 0.0);
        assertEquals(1.0, calculation.calculation("1+2i", false).getResult(), 0.0);
        assertEquals(3.0, calculation.compileBigDecimal("3-2i", true)
                .calculationBigDecimals(false).getResult(), 0.0);

        assertMessageContains(assertThrows(ExtractException.class,
                () -> ComplexCalculation.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "不属于 ComplexCalculation 类型");
    }

    @Test
    @DisplayName("复数计算的 BigDecimal 模式分支")
    void complexCalculationBigDecimalMode() {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complexBig"));
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            assertEquals(1.0, calculation.calculation("1+2i", true).getResult(), 0.0);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
    }

    @Test
    @DisplayName("ComplexExpression 编译与实部虚部（3-(2)i 的虚部为 -2）")
    void complexExpressionCompile() {
        ComplexExpression minus = ComplexExpression.compile("3-(2)i", uniq("complexExpr"));
        assertTrue(minus.isBigDecimal());
        assertTrue(minus.isUnBigDecimal());
        minus.convertToMultiPrecisionSupported();
        assertEquals("3", minus.getFunctionExpression1().getExpressionStr());
        // 虚部子串是 "-(2)"（含顶层运算符），再被 '-' 取负 -> +2
        CalculationComplexResults minusResult = minus.calculation(false);
        assertEquals(3.0, minusResult.getResult(), 0.0);
        assertEquals(2.0, minusResult.toComplexNumber().getImaginary(), 0.0);

        ComplexExpression negative = ComplexExpression.compile("3+(-2)i", uniq("complexExprNegative"));
        assertEquals(3.0, negative.calculationBigDecimals(false).getResult(), 0.0);
        assertEquals(-2.0, negative.calculationBigDecimals(false).toComplexNumber().getImaginary(), 0.0);

        ComplexExpression cached = ComplexExpression.compile("3-(2)i", uniq("complexExprCache"));
        CalculationComplexResults fromCache = cached.calculationCache(false);
        assertEquals(3.0, fromCache.getResult(), 0.0);
        assertSame(fromCache, cached.calculationBigDecimalsCache(false));
    }

    @Test
    @DisplayName("ComplexExpression 构造器直连与非法表达式")
    void complexExpressionConstructor() {
        ComplexExpression manual = new ComplexExpression("manual", "manualComplex",
                FunctionExpression.compile("1", "manualLeft"),
                FunctionExpression.compile("2", "manualRight"), '-');
        assertEquals("manual", manual.getExpressionStr());
        assertEquals("manualComplex", manual.getCalculationName());
        assertEquals(-2.0, manual.calculation(false).toComplexNumber().getImaginary(), 0.0);

        assertMessageContains(assertThrows(RuntimeException.class,
                () -> ComplexExpression.compile("1i", uniq("badComplex"))), "is not a complex expression");
    }

    // ================================================================== EquationSolver / EquationSolverExpression

    @Test
    @DisplayName("EquationSolver.compile 的方程格式校验")
    void equationSolverCompileFormat() {
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> EquationSolver.compile("x+1", uniq("eqBad"))), "方程表达式格式应为");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> EquationSolver.compile("x=1=2", uniq("eqBadMulti"))), "方程表达式格式应为");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> EquationSolverExpression.compile("x+1", uniq("eseBad"))), "方程表达式格式应为");
    }

    @Test
    @DisplayName("动态编译入口在本副本中恒定失败（生成源码 import 了不存在的包）")
    void dynamicCompilationAlwaysFails() {
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> EquationSolver.compile("2-x=0", uniq("eqJvm"))), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> EquationSolverExpression.compile("3*x+2=8", uniq("eseJvm"))), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> JvmExpression.compile("1+2", uniq("jvmExpr"))), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> DynamicFunctionCompiler.compile("probeDyn", "1+2", new String[]{"a"})),
                "动态函数生成失败");
    }

    @Test
    @DisplayName("EquationSolver 默认值与 eval，及负方向的 NoRootFoundException")
    void equationSolverEvalAndNoRoot() {
        ExposedEquationSolver solver = new ExposedEquationSolver("x*x", 4, new SquareFunction());
        assertEquals(9.0, solver.evalAt(3), 0.0);
        assertEquals(4.0, solver.evalAt(-2), 0.0);
        assertEquals(4.0, solver.getResult(), 0.0);
        assertEquals(2.0, solver.getNewtonInitialX(), 0.0);
        assertEquals(1.0, solver.getBisectionLeft(), 0.0);
        assertEquals(3.0, solver.getBisectionRight(), 0.0);
        assertEquals(1e-12, solver.getTol(), 0.0);
        assertEquals(100, solver.getMaxIter());
        assertTrue(solver.isUseNewton());
        assertFalse(solver.isBigDecimal());
        assertTrue(solver.isUnBigDecimal());

        // x*x=4 的符号变化发生在 -2（负方向），线性细化没有正向步长可用
        assertMessageContains(assertThrows(EquationSolver.NoRootFoundException.class,
                solver::detectInterval), "线性细化在");
        assertEquals("no root", new EquationSolver.NoRootFoundException("no root").getMessage());
    }

    @Test
    @DisplayName("EquationSolver 自动区间 + 牛顿/二分（2-x=0 的根为 2）")
    void equationSolverNewtonAndBisection() {
        ExposedEquationSolver solver = new ExposedEquationSolver("2-x", 0, new NegLinearFunction());
        solver.detectInterval();
        assertEquals(1.5, solver.getNewtonInitialX(), 1e-12);
        assertEquals(1.0, solver.getBisectionLeft(), 1e-12);
        assertEquals(2.0, solver.getBisectionRight(), 1e-12);

        double[] newton = solver.solveNewton(1e-12, 100);
        assertEquals(2.0, newton[0], 1e-9);
        assertTrue(newton[1] >= 1 && newton[1] <= 100);

        // 区间 [1,2]，maxIter=3 且 tol=0：只迭代 3 次，返回第 3 次中点
        double[] bisection = solver.solveBisection(0, 3);
        assertEquals(1.875, bisection[0], 1e-12);
        assertEquals(3.0, bisection[1], 1e-12);

        CalculationNumberResults results = solver.calculation(false);
        assertEquals(2.0, results.getResult(), 1e-9);
        assertEquals("EngineProbeSolver", results.getCalculationSourceName());
        assertTrue(results.getResultLayers() >= 1);
    }

    @Test
    @DisplayName("EquationSolver 牛顿法导数过小分支")
    void equationSolverNewtonZeroDerivative() {
        ExposedEquationSolver solver =
                new ExposedEquationSolver("5-(x-2)*(x-2)", 0, new ParabolaFunction());
        solver.detectInterval();
        assertEquals(4.5, solver.getNewtonInitialX(), 1e-12);
        solver.setNewtonInitialX(2.0);
        assertMessageContains(assertThrows(ArithmeticException.class,
                () -> solver.solveNewton(1e-9, 10)), "导数过小");
    }

    @Test
    @DisplayName("EquationSolver 牛顿法未收敛分支")
    void equationSolverNewtonNotConverged() {
        ExposedEquationSolver solver =
                new ExposedEquationSolver("5-(x-2)*(x-2)", 0, new ParabolaFunction());
        solver.detectInterval();
        solver.setNewtonInitialX(4.5);
        assertMessageContains(assertThrows(ArithmeticException.class,
                () -> solver.solveNewton(0.0, 1)), "未收敛");
    }

    @Test
    @DisplayName("EquationSolver 二分法同号区间与迭代耗尽")
    void equationSolverBisectionBranches() {
        ExposedEquationSolver solver =
                new ExposedEquationSolver("5-(x-2)*(x-2)", 0, new ParabolaFunction());
        solver.detectInterval();
        solver.setUseNewton(false);
        solver.setBisectionLeft(2.0);
        solver.setBisectionRight(3.0);
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> solver.solveBisection(1e-12, 10)), "同号");

        // fa=1、fb=-4，迭代 3 次：1.5、4.25、4.125
        solver.setBisectionLeft(4.0);
        solver.setBisectionRight(5.0);
        double[] exhausted = solver.solveBisection(0, 3);
        assertEquals(4.125, exhausted[0], 1e-12);
        assertEquals(3.0, exhausted[1], 1e-12);
    }

    @Test
    @DisplayName("EquationSolver 二分法模式的 calculation 与所有 setter/getter")
    void equationSolverCalculationAndAccessors() {
        ExposedEquationSolver solver = new ExposedEquationSolver("2-x", 0, new NegLinearFunction());
        solver.detectInterval();
        solver.setUseNewton(false);
        assertEquals(2.0, solver.calculation(false).getResult(), 1e-9);

        solver.setTol(1e-6);
        solver.setMaxIter(7);
        solver.setNewtonInitialX(1.0);
        solver.setBisectionLeft(0.0);
        solver.setBisectionRight(4.0);
        assertEquals(1e-6, solver.getTol(), 0.0);
        assertEquals(7, solver.getMaxIter());
        assertEquals(1.0, solver.getNewtonInitialX(), 0.0);
        assertEquals(0.0, solver.getBisectionLeft(), 0.0);
        assertEquals(4.0, solver.getBisectionRight(), 0.0);
        assertFalse(solver.isUseNewton());
        solver.setUseNewton(true);
        assertTrue(solver.isUseNewton());

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> solver.calculationBigDecimals(false)), "不支持高精度计算");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                solver::convertToMultiPrecisionSupported), "不支持 BigDecimal 类型计算");
    }

    @Test
    @DisplayName("EquationSolverExpression 的已知/未知数切换、explain 与克隆")
    void equationSolverExpressionBehaviour() {
        ParsedSignature signature = (ParsedSignature) instantiate(
                constructor(ParsedSignature.class, String.class, String[].class, String.class),
                "probeFn", new String[]{"a", "b"}, "a+b*2");
        ManyToOneNumberFunction delegate = new ManyToOneNumberFunction("probeSum") {
            @Override
            public double run(double... numbers) {
                double sum = 0;
                for (double value : numbers) {
                    sum += value;
                }
                return sum;
            }
        };
        JvmExpressionFunction function = (JvmExpressionFunction) instantiate(
                constructor(JvmExpressionFunction.class, ParsedSignature.class, ManyToOneNumberFunction.class),
                signature, delegate);
        JvmExpression jvm = (JvmExpression) instantiate(
                constructor(JvmExpression.class, String.class, double[].class,
                        JvmExpressionFunction.class, String.class),
                "EngineProbeJvmExpr", new double[]{3, 7}, function, "3+7");

        HashSet<Integer> unknown = new HashSet<>();
        unknown.add(0);
        EquationSolverExpression expression = (EquationSolverExpression) instantiate(
                constructor(EquationSolverExpression.class, String.class, double.class, String.class,
                        HashSet.class, JvmExpression.class, Object[].class),
                "x+7", 10.0, uniq("ese"), unknown, jvm, new Object[]{"x", 7.0});

        assertEquals(10.0, expression.getResult(), 0.0);
        assertTrue(expression.indexIsUnKnownNumber(0));
        assertFalse(expression.indexIsUnKnownNumber(1));
        assertSame(jvm, expression.getJvmExpression());
        assertEquals(2, expression.getJvmExpression().getLength());
        assertEquals("x+7.0*2 = 10.0", expression.explain());

        // eval 会把未知数位置写入 x，函数是求和 -> x + 7 = 10
        assertEquals(3.0, expression.calculation(false).getResult(), 1e-9);

        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> expression.setKnownNumber(0, 5)), "未知数索引不能被修改");
        expression.setKnownNumber(1, 9);
        assertEquals("x+9.0*2 = 10.0", expression.explain());

        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> expression.setUnKnownNumber(1, 3)), "setUnKnownNumber");
        expression.setUnKnownNumber(0, 3);
        assertFalse(expression.indexIsUnKnownNumber(0));
        assertEquals("3.0+9.0*2 = 10.0", expression.explain());

        EquationSolverExpression clone = expression.cloneExpression();
        assertEquals(expression.getCalculationName() + "_clone", clone.getCalculationName());
        assertEquals(10.0, clone.getResult(), 0.0);
    }

    @Test
    @DisplayName("SingletonEquationSolving(SingletonEquationSolvingTwo) 的入口与拒绝分支")
    void singletonEquationSolvers() {
        SingletonEquationSolving solving = SingletonEquationSolving.getInstance(uniq("singletonOne"));
        assertEquals(" 1 + 2 ", solving.formatStr(" 1 + 2 "));
        assertSame(solving, SingletonEquationSolving.getInstance(solving.getName()));
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> solving.calculation("2-x=0", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> solving.compile("2-x=0", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> solving.compileBigDecimal("2-x=0", true)), "不支持BigDecimal运算");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> SingletonEquationSolving.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "DynamicEquationSolver");

        SingletonEquationSolvingTwo solvingTwo =
                SingletonEquationSolvingTwo.getInstance(uniq("singletonTwo"));
        assertEquals(" 3 + 4 ", solvingTwo.formatStr(" 3 + 4 "));
        assertSame(solvingTwo, SingletonEquationSolvingTwo.getInstance(solvingTwo.getName()));
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> solvingTwo.calculation("2-x=0", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> solvingTwo.compile("2-x=0", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> solvingTwo.compileBigDecimal("2-x=0", true)), "不支持BigDecimal运算");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> SingletonEquationSolvingTwo.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "DynamicEquationSolver");
    }

    // ================================================================== Jvm 系列 / 动态编译支撑类

    @Test
    @DisplayName("JvmCalculation 的委托、格式化与拒绝分支")
    void jvmCalculation() {
        JvmCalculation calculation = JvmCalculation.getInstance(uniq("jvmCalc"));
        assertEquals(" 1 + 2 ", calculation.formatStr(" 1 + 2 "));
        calculation.check("anything");
        assertSame(calculation, JvmCalculation.getInstance(calculation.getName()));
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> calculation.calculation("1+2", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> calculation.compile("1+2", true)), "动态函数生成失败");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> calculation.compileBigDecimal("1+2", true)), "不支持 BigDecimal 类型的计算");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> JvmCalculation.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "DynamicEquationSolver");
    }

    @Test
    @DisplayName("JvmExpressionFunction 静态工具与签名解析错误")
    void jvmExpressionFunctionStaticUtilities() throws Exception {
        assertEquals("public static double f(double x) { return x + x; }",
                JvmExpressionFunction.convertToStaticMethodSource("f(x) = x + x"));

        JvmExpressionFunction.registerFunction("probeJvmStatic(x) = x * 2");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double probeJvmStatic"));
        JvmExpressionFunction.registerJvmFunction(
                "public static double probeJvmRaw(double x) { return x + 3; }");
        assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double probeJvmRaw"));

        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("f(x)")), "签名必须包含");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("f(x)=x=1")), "不能包含多个");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("fx=1")), "函数定义需要包含参数列表");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("f1(x)=x")), "函数名称不能包含数字");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.registerJvmFunction("nonsense")), "函数方法格式错误");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.registerJvmFunction("static double f(double x) { return x; }")),
                "函数方法签名格式错误");

        assertMessageContains(assertThrows(RuntimeException.class,
                () -> JvmExpressionFunction.parse("probeSig(x) = x + 1")), "动态函数生成失败");
        ExpressionFunction parsed = ExpressionFunction.parse("probeParsed(x)=x+1");
        assertMessageContains(assertThrows(RuntimeException.class,
                () -> JvmExpressionFunction.parse(parsed)), "动态函数生成失败");
    }

    @Test
    @DisplayName("JvmExpressionFunction 的 run/explain 双重重载与参数校验")
    void jvmExpressionFunctionBehaviour() {
        ParsedSignature signature = (ParsedSignature) instantiate(
                constructor(ParsedSignature.class, String.class, String[].class, String.class),
                "probeFn", new String[]{"a", "b"}, "a+b*2");
        assertEquals("probeFn", signature.getName());
        assertEquals(List.of("a", "b"), Arrays.asList(signature.getParamNames()));
        assertEquals("a+b*2", signature.getExpression());
        assertEquals("probeFn(a,b)=a+b*2", signature.toString());

        ManyToOneNumberFunction delegate = new ManyToOneNumberFunction("probeSum") {
            @Override
            public double run(double... numbers) {
                double sum = 0;
                for (double value : numbers) {
                    sum += value;
                }
                return sum;
            }
        };
        JvmExpressionFunction function = (JvmExpressionFunction) instantiate(
                constructor(JvmExpressionFunction.class, ParsedSignature.class, ManyToOneNumberFunction.class),
                signature, delegate);
        assertEquals("probeFn", function.getName());
        assertEquals("probeFn(a,b)=a+b*2", function.toString());
        assertEquals(3.0, function.run(1, 2), 0.0);
        assertEquals("1.0+2.0*2", function.explain(new double[]{1, 2}));
        assertEquals("1+2*2", function.explain(new Object[]{1, 2}));
        // matcher 在 explain 之后被 reset，可重复调用
        assertEquals("1.0+2.0*2", function.explain(new double[]{1, 2}));

        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> function.run(1)), "参数数量不匹配");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> function.explain(new double[]{1})), "参数数量不匹配");
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> function.explain(new Object[]{1})), "参数数量不匹配");
    }

    @Test
    @DisplayName("JvmExpression 的取值/写入/克隆/字符串与不支持的高精度")
    void jvmExpressionBehaviour() {
        ParsedSignature signature = (ParsedSignature) instantiate(
                constructor(ParsedSignature.class, String.class, String[].class, String.class),
                "probeFn", new String[]{"a", "b"}, "a+b*2");
        ManyToOneNumberFunction delegate = new ManyToOneNumberFunction("probeSum") {
            @Override
            public double run(double... numbers) {
                double sum = 0;
                for (double value : numbers) {
                    sum += value;
                }
                return sum;
            }
        };
        JvmExpressionFunction function = (JvmExpressionFunction) instantiate(
                constructor(JvmExpressionFunction.class, ParsedSignature.class, ManyToOneNumberFunction.class),
                signature, delegate);
        JvmExpression expression = (JvmExpression) instantiate(
                constructor(JvmExpression.class, String.class, double[].class,
                        JvmExpressionFunction.class, String.class),
                "EngineProbeJvmValue", new double[]{1, 7}, function, "1+7");

        assertEquals(2, expression.getLength());
        assertEquals(1.0, expression.getParamNumber(0), 0.0);
        assertEquals(7.0, expression.getParamNumber(1), 0.0);
        assertEquals(8.0, expression.iterator().sum(), 0.0);
        assertEquals("1+7", expression.getExpressionStr());
        assertFalse(expression.isBigDecimal());
        assertTrue(expression.isUnBigDecimal());
        assertEquals("1.0+7.0*2", expression.getExpressionStrAndNumber());
        assertEquals(8.0, numeric(expression.calculation(true)), 0.0);
        assertEquals(1, expression.calculation(true).getResultLayers());
        assertSame(function, expression.getJvmExpressionFunction());

        expression.setParamNumber(1, 20.0);
        assertEquals(20.0, expression.getParamNumber(1), 0.0);
        assertEquals("1.0+20.0*2", expression.getExpressionStrAndNumber());
        assertEquals(21.0, numeric(expression.calculation(true)), 0.0);

        JvmExpression clone = expression.cloneExpression();
        assertEquals("EngineProbeJvmValue_clone", clone.getCalculationName());
        assertEquals(2, clone.getLength());
        assertEquals(20.0, clone.getParamNumber(1), 0.0);
        assertNotSame(expression, clone);

        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> expression.calculationBigDecimals(true)), "does not support BigDecimal");
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                expression::convertToMultiPrecisionSupported), "does not support BigDecimal mode");
    }

    @Test
    @DisplayName("InMemoryJavaFileObject 与 DynamicClassLoader 的字节存取/查找")
    void inMemoryFileObjectAndClassLoader() throws Exception {
        InMemoryJavaFileObject fileObject = (InMemoryJavaFileObject) instantiate(
                constructor(InMemoryJavaFileObject.class, String.class, JavaFileObject.Kind.class),
                "probe.Tiny", JavaFileObject.Kind.CLASS);
        byte[] payload = new byte[]{1, 2, 3};
        try (java.io.OutputStream out = fileObject.openOutputStream()) {
            out.write(payload);
        }
        byte[] read = (byte[]) call(method(InMemoryJavaFileObject.class, "getBytes"), fileObject);
        assertEquals(Arrays.toString(payload), Arrays.toString(read));

        DynamicClassLoader loader = (DynamicClassLoader) instantiate(constructor(DynamicClassLoader.class));
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("probe.Absent"));
        call(method(DynamicClassLoader.class, "addClass", String.class, InMemoryJavaFileObject.class),
                loader, "probe.Tiny", fileObject);
        assertThrows(ClassFormatError.class, () -> loader.loadClass("probe.Tiny"));
    }

    // ================================================================== FunctionPackage（含 $1..$31 内部类）

    @Test
    @DisplayName("FunctionPackage 三个包的元数据")
    void functionPackageMetadata() {
        assertEquals(3, FunctionPackage.values().length);
        assertSame(FunctionPackage.MATH, FunctionPackage.valueOf("MATH"));
        assertEquals("Math", FunctionPackage.MATH.getName());
        assertEquals("Date", FunctionPackage.DATE.getName());
        assertEquals("Branch", FunctionPackage.BRANCH.getName());
        assertEquals(12, FunctionPackage.MATH.getFunctionMap().size());
        assertEquals(14, FunctionPackage.DATE.getFunctionMap().size());
        assertEquals(5, FunctionPackage.BRANCH.getFunctionMap().size());
        assertNull(FunctionPackage.MATH.getFunctionByNameNoCheck("nope"));
        assertNotNull(FunctionPackage.MATH.getFunctionMap().get("sum"));
    }

    @Test
    @DisplayName("Math 包各函数（moduleLength 的 pow 单参调用会越界）")
    void functionPackageMathFunctions() {
        assertEquals(6.0, mathFn("sum").run(1, 2, 3), 0.0);
        assertEquals(1024.0, mathFn("pow").run(2, 10), 0.0);
        assertEquals(3.0, mathFn("sqrt").run(9), 0.0);
        assertEquals(120.0, mathFn("factorial").run(5), 0.0);
        assertEquals(2.5, mathFn("avg").run(1, 2, 3, 4), 0.0);
        assertEquals(1.0, mathFn("sin").run(Math.PI / 2), 1e-12);
        assertEquals(1.0, mathFn("cos").run(0), 1e-12);
        assertEquals(0.0, mathFn("tan").run(0), 1e-12);
        assertEquals(1.0, mathFn("cot").run(Math.PI / 4), 1e-12);
        assertEquals(1.0, mathFn("sec").run(0), 1e-12);
        assertEquals(1.0, mathFn("csc").run(Math.PI / 2), 1e-12);
        // moduleLength 内 pow.run(number) 只给一个参数，pow 取 numbers[1] 越界（记录现状）
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> mathFn("moduleLength").run(3, 4));
    }

    @Test
    @DisplayName("Math 包函数缺少依赖时给出安装提示")
    void functionPackageMathMissingDependencies() {
        Mathematical_Expression.unregister_function("sum");
        try {
            assertMessageContains(assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("avg").run(1, 2)), "sum 函数位于 Math 函数包中");
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }

        Mathematical_Expression.unregister_function("pow");
        try {
            assertMessageContains(assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("moduleLength").run(1, 2)), "pow 函数位于 Math 函数包中");
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }

        Mathematical_Expression.unregister_function("sqrt");
        try {
            assertMessageContains(assertThrows(UnsupportedOperationException.class,
                    () -> mathFn("moduleLength").run(1, 2)), "sqrt 函数位于 Math 函数包中");
        } finally {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
    }

    @Test
    @DisplayName("Date 包 14 个换算函数")
    void functionPackageDateFunctions() {
        assertEquals(2.0, dateFn("msToS").run(2000), 1e-9);
        assertEquals(2000.0, dateFn("sToMs").run(2), 1e-9);
        assertEquals(2.0, dateFn("msToMin").run(120000), 1e-9);
        assertEquals(120000.0, dateFn("minToMs").run(2), 1e-9);
        assertEquals(2.0, dateFn("msToHour").run(7200000), 1e-9);
        assertEquals(7200000.0, dateFn("hourToMs").run(2), 1e-9);
        assertEquals(1.0, dateFn("msToDay").run(86400000), 1e-9);
        assertEquals(8.64E7, dateFn("dayToMs").run(1), 1e-9);
        assertEquals(1.0, dateFn("msToWeek").run(604800000), 1e-9);
        assertEquals(6.048E8, dateFn("weekToMs").run(1), 1e-9);
        assertEquals(1.0, dateFn("msToMonth").run(2592000000L), 1e-9);
        assertEquals(2.592E9, dateFn("monthToMs").run(1), 1e-9);
        assertEquals(1.0, dateFn("msToYear").run(31536000000L), 1e-9);
        assertEquals(3.1536E10, dateFn("yearToMs").run(1), 1e-9);
    }

    @Test
    @DisplayName("Branch 包 5 个条件函数的真假两路与参数校验")
    void functionPackageBranchFunctions() {
        assertEquals(10.0, branchFn("ifEq").run(1, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifEq").run(1, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifGt").run(2, 1, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifGt").run(1, 1, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifLt").run(1, 2, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifLt").run(2, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifGe").run(2, 2, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifGe").run(1, 2, 10, 20), 0.0);
        assertEquals(10.0, branchFn("ifLe").run(2, 2, 10, 20), 0.0);
        assertEquals(20.0, branchFn("ifLe").run(3, 2, 10, 20), 0.0);

        for (String name : List.of("ifEq", "ifGt", "ifLt", "ifGe", "ifLe")) {
            assertMessageContains(assertThrows(UnsupportedOperationException.class,
                    () -> branchFn(name).run(1, 2, 3)), "参数个数必须为4");
        }
    }

    // ================================================================== 第二轮：私有构造器/剩余分支补充

    /** 60 - x*x，用于触发线性细化的多步扫描（根位于 7 与 8 之间）。 */
    static final class DecayFunction extends ManyToOneNumberFunction {
        DecayFunction() {
            super("decay");
        }

        @Override
        public double run(double... numbers) {
            return 60 - numbers[0] * numbers[0];
        }
    }

    static final class ProbeFunctionFormulaCalculation extends FunctionFormulaCalculation {
        ProbeFunctionFormulaCalculation() {
            super();
        }
    }

    static final class ProbeFunctionFormulaCalculation2 extends FunctionFormulaCalculation2 {
        ProbeFunctionFormulaCalculation2() {
            super();
        }
    }

    /** 与默认 BracketsCalculation2 结果不同的数值组件，用于观察 setCalculation 是否生效。 */
    static final class ConstantNumberCalculation extends NumberCalculation {
        ConstantNumberCalculation() {
            super("probeConstant");
        }

        @Override
        public String formatStr(String string) {
            return string;
        }

        @Override
        public CalculationNumberResults calculation(String Formula, boolean formatRequired) {
            return new CalculationNumberResults(1, 5.0, getName());
        }
    }

    /** 4 - 2x，导数不是 ±1，用于观察牛顿更新公式。 */
    static final class ScaledLinearFunction extends ManyToOneNumberFunction {
        ScaledLinearFunction() {
            super("scaledLinear");
        }

        @Override
        public double run(double... numbers) {
            return 4 - 2 * numbers[0];
        }
    }

    /** -x，用于观察二分法 fa*fm 的 0 值分支。 */
    static final class NegateFunction extends ManyToOneNumberFunction {
        NegateFunction() {
            super("negateX");
        }

        @Override
        public double run(double... numbers) {
            return -numbers[0];
        }
    }

    static final class IdentityFunction extends ManyToOneNumberFunction {
        IdentityFunction() {
            super("identityX");
        }

        @Override
        public double run(double... numbers) {
            return numbers[0];
        }
    }

    // ================================================================== 第三轮：针对 PIT 幸存变异体

    @Test
    @DisplayName("setCalculation 替换数值组件后比较结果随之改变")
    void booleanCalculationSetCalculationChangesResult() {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("boolSwitch"));
        assertEquals(Boolean.TRUE, calculation.calculation("1<2", true).getResult());

        calculation.setCalculation(new ConstantNumberCalculation());
        // 组件恒返回 5.0 -> 5 < 5 为 false
        assertEquals(Boolean.FALSE, calculation.calculation("1<2", true).getResult());

        calculation.setCalculation(Mathematical_Expression.bracketsCalculation2);
        assertEquals(Boolean.TRUE, calculation.calculation("1<2", true).getResult());
    }

    @Test
    @DisplayName("BooleanCalculation.check 对左右操作数分别做数值检查并统计运算符个数")
    void booleanCalculationCheckOperands() throws Exception {
        BooleanCalculation2 calculation = BooleanCalculation2.getInstance(uniq("boolOperands"));
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("1+<2")), "最后一个字符");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("2<1+")), "最后一个字符");
        WrongFormat none = assertThrows(WrongFormat.class, () -> calculation.check("1"));
        assertTrue(none.getMessage().contains("[0]"));
    }

    @Test
    @DisplayName("BracketsCalculation2 新注册路径与非 BigDecimal 除法")
    void bracketsCalculationFreshInstanceAndDivision() {
        BracketsCalculation2 fresh = BracketsCalculation2.getInstance(uniq("bracketsFresh"));
        assertNotNull(fresh);
        assertSame(fresh, BracketsCalculation2.getInstance(fresh.getName()));
        assertEquals(fresh.getName(), BracketsCalculation2.getInstance(fresh.getName()).getName());

        assertEquals(1.0 / 3.0, brackets().calculation("1/3").getResult(), 1e-12);
        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            assertThrows(ArithmeticException.class, () -> brackets().calculation("1/3"));
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
    }

    @Test
    @DisplayName("BracketsCalculation2.explain 的日志前缀与子节点")
    void bracketsCalculationExplainLogStructure() {
        LogResults simple = brackets().explain("1+2", true);
        assertEquals("f_" + "1+2".hashCode() + "(\"1+2\")", simple.getPrefix());
        assertFalse(simple.isEmpty());

        LogResults nested = brackets().explain("(1+2)*3", true);
        assertEquals("f_" + "(1+2)*3".hashCode() + "(\"(1+2)*3\")", nested.getPrefix());
        // 括号子表达式与最终前缀表达式各写入一个子日志
        assertTrue(nested.size() >= 2);
    }

    @Test
    @DisplayName("ComplexCalculation 的日志属性与非法虚部检查")
    void complexCalculationLogAndInvalidImaginary() throws Exception {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complexMore"));
        // 虚部 "+2a3" 会被函数计算组件判定为缺少函数括号
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("1+2a3i")), "缺少起始或结束括号");

        LogResults explain = calculation.explain("1+2i", true);
        assertNotNull(explain.getPrefix());
        assertTrue(explain.getPrefix().contains("1+2i"));
        assertFalse(explain.isNameJoin());
        assertEquals("1.0 + 2.0i", explain.getResult().toString());
    }

    @Test
    @DisplayName("ComplexExpression 两个方向的虚部与 layers")
    void complexExpressionImaginaryDirections() {
        ComplexExpression minus = ComplexExpression.compile("3-(2)i", uniq("ceMinus"));
        assertEquals(2.0, minus.calculation(false).toComplexNumber().getImaginary(), 0.0);

        ComplexExpression minusBig = ComplexExpression.compile("3-(2)i", uniq("ceMinusBig"));
        CalculationComplexResults big = minusBig.calculationBigDecimals(false);
        assertEquals(2.0, big.toComplexNumber().getImaginary(), 0.0);
        assertEquals(2, big.getResultLayers());

        ComplexExpression negative = ComplexExpression.compile("3+(-2)i", uniq("ceNegative"));
        assertEquals(-2.0, negative.calculation(false).toComplexNumber().getImaginary(), 0.0);

        ComplexExpression negativeBig = ComplexExpression.compile("3+(-2)i", uniq("ceNegativeBig"));
        assertEquals(-2.0, negativeBig.calculationBigDecimals(false)
                .toComplexNumber().getImaginary(), 0.0);
    }

    @Test
    @DisplayName("CumulativeCalculation 的内部公式校验与容量溢出")
    void cumulativeCalculationCheckAndCapacity() throws Exception {
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cumCap"));
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("n[0,10,1](1+*)")), "缺失了一个运算数值");

        // 下降且极大的区间令容量 (int)(len*|end-start|)+1 回绕为负
        assertThrows(NegativeArraySizeException.class,
                () -> calculation.formatStr("n[1e300,-1e300,1](1+n)"));
    }

    @Test
    @DisplayName("动态编译失败的根因是 javac 诊断而不是空源码")
    void dynamicCompilationFailureCause() {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> DynamicFunctionCompiler.compile("probeCause", "1+2", new String[]{"a"}));
        assertMessageContains(failure, "动态函数生成失败");
        assertNotNull(failure.getCause());
        assertMessageContains(failure.getCause(), "编译错误");
    }

    @Test
    @DisplayName("EquationSolver 牛顿法：单次迭代收敛与 tol 边界")
    void equationSolverNewtonBoundaries() {
        ExposedEquationSolver solver = new ExposedEquationSolver("2-x", 0, new NegLinearFunction());

        solver.setNewtonInitialX(2.0);
        double[] once = solver.solveNewton(1e-12, 1);
        assertEquals(2.0, once[0], 1e-12);
        assertEquals(1.0, once[1], 1e-12);

        solver.setNewtonInitialX(1.5);
        // fx = 0.5 与 tol 相等：严格小于不成立，需要再迭代一次
        double[] boundary = solver.solveNewton(0.5, 3);
        assertEquals(2.0, boundary[0], 1e-9);
        assertEquals(2.0, boundary[1], 1e-9);
    }

    @Test
    @DisplayName("EquationSolver 牛顿更新公式：f(x)=4-2x 从 1.5 收敛到 2")
    void equationSolverNewtonUpdateFormula() {
        ExposedEquationSolver solver = new ExposedEquationSolver("4-2*x", 0, new ScaledLinearFunction());
        solver.setNewtonInitialX(1.5);
        double[] result = solver.solveNewton(1e-9, 50);
        assertEquals(2.0, result[0], 1e-9);
    }

    @Test
    @DisplayName("EquationSolver 二分法：非零右值、tol 边界与零积分支")
    void equationSolverBisectionMoreBranches() {
        ExposedEquationSolver root = new ExposedEquationSolver("x", 3, new IdentityFunction());
        root.setBisectionLeft(1.0);
        root.setBisectionRight(4.0);
        assertEquals(3.0, root.solveBisection(1e-9, 100)[0], 1e-9);

        // |fm| == tol：严格小于不成立，迭代耗尽后返回最后的中点
        ExposedEquationSolver boundary = new ExposedEquationSolver("x", 4, new IdentityFunction());
        boundary.setBisectionLeft(1.0);
        boundary.setBisectionRight(5.0);
        double[] boundaryResult = boundary.solveBisection(1.0, 100);
        assertEquals(3.0, boundaryResult[0], 1e-12);
        assertEquals(100.0, boundaryResult[1], 1e-12);

        // fa*fm == 0 时收缩右端
        ExposedEquationSolver zeroProduct = new ExposedEquationSolver("x", 3, new IdentityFunction());
        zeroProduct.setBisectionLeft(1.0);
        zeroProduct.setBisectionRight(5.0);
        double[] zeroProductResult = zeroProduct.solveBisection(0, 2);
        assertEquals(2.0, zeroProductResult[0], 1e-12);
        assertEquals(2.0, zeroProductResult[1], 1e-12);

        // 循环条件 (b-a)/2 > tol 的边界
        ExposedEquationSolver loopBoundary = new ExposedEquationSolver("x", 1.5, new IdentityFunction());
        loopBoundary.setBisectionLeft(1.0);
        loopBoundary.setBisectionRight(2.0);
        double[] loopResult = loopBoundary.solveBisection(0.5, 7);
        assertEquals(1.0, loopResult[0], 1e-12);
        assertEquals(7.0, loopResult[1], 1e-12);
    }

    @Test
    @DisplayName("EquationSolver 二分法初值符号：右值非零且区间同号")
    void equationSolverBisectionSignWithNonZeroResult() {
        ExposedEquationSolver solver = new ExposedEquationSolver("x", 3, new IdentityFunction());
        solver.setBisectionLeft(1.0);
        solver.setBisectionRight(2.0);
        // f(1)-3=-2, f(2)-3=-1 同号 -> 拒绝；若把右端算成 f(2)+3=5 则会误判为有根
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> solver.solveBisection(1e-9, 10)), "同号");
    }

    @Test
    @DisplayName("EquationSolver 二分法 fa*fm 的零值分支（fa 为正）")
    void equationSolverBisectionZeroProductPositiveLeft() {
        ExposedEquationSolver solver = new ExposedEquationSolver("-x", -3, new NegateFunction());
        solver.setBisectionLeft(1.0);
        solver.setBisectionRight(5.0);
        double[] result = solver.solveBisection(0, 2);
        // 第一次 mid=3 命中 fa*fm==0 -> 收缩右端；第二次 mid=2
        assertEquals(2.0, result[0], 1e-12);
        assertEquals(2.0, result[1], 1e-12);
    }

    @Test
    @DisplayName("EquationSolver 强制牛顿模式下 calculation 抛未收敛异常")
    void equationSolverCalculationNewtonOnly() {
        ExposedEquationSolver solver =
                new ExposedEquationSolver("5-(x-2)*(x-2)", 0, new ParabolaFunction());
        solver.setNewtonInitialX(4.5);
        solver.setTol(0);
        solver.setMaxIter(1);
        assertThrows(ArithmeticException.class, () -> solver.calculation(false));
    }

    @Test
    @DisplayName("EquationSolver 线性细化第二组输入（result=10）")
    void equationSolverRefineWalkSecondInput() {
        ExposedEquationSolver solver = new ExposedEquationSolver("60-x*x", 10, new DecayFunction());
        solver.detectInterval();
        assertEquals(7.0, solver.getBisectionLeft(), 1e-12);
        assertEquals(8.0, solver.getBisectionRight(), 1e-12);
        assertEquals(7.5, solver.getNewtonInitialX(), 1e-12);
    }

    @Test
    @DisplayName("EquationSolverExpression 的 setKnown/setUnKnown 真正写入 JvmExpression 参数")
    void equationSolverExpressionParamWrites() {
        HashSet<Integer> unknown = new HashSet<>();
        unknown.add(0);
        EquationSolverExpression expression = reflectiveEquationSolverExpression(
                "x+7", 10.0, uniq("eseWrite"), unknown,
                new double[]{3, 7}, "3+7", "x+7");
        expression.setKnownNumber(1, 9);
        assertEquals(9.0, expression.getJvmExpression().getParamNumber(1), 0.0);
        expression.setUnKnownNumber(0, 3);
        assertEquals(3.0, expression.getJvmExpression().getParamNumber(0), 0.0);
    }

    @Test
    @DisplayName("EquationSolverExpression 构造期自动区间检测失败（负方向细化）")
    void equationSolverExpressionAutoDetectFailure() {
        HashSet<Integer> unknown = new HashSet<>();
        unknown.add(0);
        Constructor<?> ctor = constructor(EquationSolverExpression.class, String.class, double.class,
                String.class, HashSet.class, JvmExpression.class, Object[].class);
        JvmExpression jvm = buildJvmExpression(new double[]{0, 3}, "3-0",
                new ManyToOneNumberFunction("probeSubtract") {
                    @Override
                    public double run(double... numbers) {
                        return numbers[1] - numbers[0];
                    }
                });
        Throwable cause = null;
        try {
            ctor.newInstance("x+7", 10.0, uniq("eseDetect"), unknown, jvm, new Object[]{"x", 7.0});
        } catch (InvocationTargetException exception) {
            cause = exception.getCause();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        assertMessageContains(cause, "线性细化在");
    }

    @Test
    @DisplayName("区间累乘结果的层数为两输入层数之和")
    void fastMultiplyLayerAddition() {
        FastMultiplyOfIntervalsBrackets calculation =
                FastMultiplyOfIntervalsBrackets.getInstance(uniq("fastMulLayers"));
        CalculationNumberResults start = new CalculationNumberResults(3, 2.0, "start");
        CalculationNumberResults end = new CalculationNumberResults(4, 6.0, "end");
        CalculationNumberResults results = calculation.calculation(start, end);
        assertEquals(7, results.getResultLayers());
        assertEquals(48.0, results.getResult(), 0.0);
    }

    // ------------------------------------------------------------------ 第三轮辅助

    private static JvmExpression buildJvmExpression(double[] numbers, String expression,
                                                   ManyToOneNumberFunction delegate) {
        ParsedSignature signature = (ParsedSignature) instantiate(
                constructor(ParsedSignature.class, String.class, String[].class, String.class),
                delegate.getName(), new String[]{"a", "b"}, "a+b*2");
        JvmExpressionFunction function = (JvmExpressionFunction) instantiate(
                constructor(JvmExpressionFunction.class, ParsedSignature.class,
                        ManyToOneNumberFunction.class), signature, delegate);
        return (JvmExpression) instantiate(
                constructor(JvmExpression.class, String.class, double[].class,
                        JvmExpressionFunction.class, String.class),
                "EngineProbeJvm_" + SEQ.incrementAndGet(), numbers, function, expression);
    }

    private static EquationSolverExpression reflectiveEquationSolverExpression(
            String expression, double result, String name, HashSet<Integer> unknown,
            double[] numbers, String jvmExpression, String delegateDescription) {
        JvmExpression jvm = buildJvmExpression(numbers, jvmExpression,
                new ManyToOneNumberFunction("probeSum") {
                    @Override
                    public double run(double... values) {
                        double sum = 0;
                        for (double value : values) {
                            sum += value;
                        }
                        return sum;
                    }
                });
        Object[] objectNumbers = new Object[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            objectNumbers[i] = numbers[i];
        }
        for (Integer index : unknown) {
            objectNumbers[index] = "x";
        }
        return (EquationSolverExpression) instantiate(
                constructor(EquationSolverExpression.class, String.class, double.class, String.class,
                        HashSet.class, JvmExpression.class, Object[].class),
                expression, result, name, unknown, jvm, objectNumbers);
    }

    @Test
    @DisplayName("工具类的隐式无参构造器（覆盖缺口扫描）")
    void utilityClassesImplicitConstructors() throws Exception {
        List<Class<?>> utilityTypes = new ArrayList<>(List.of(
                StrUtils.class, NumberUtils.class, ConstantRegion.class,
                CalculationOptimized.class, CalculationManagement.class,
                DynamicFunctionCompiler.class, Mathematical_Expression.Options.class));
        for (Class<?> type : utilityTypes) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance(), () -> "cannot instantiate " + type.getName());
        }
    }

    @Test
    @DisplayName("ExpressionFunction 解析、运行、解释与序列化")
    void expressionFunctionContract() throws Exception {
        ExpressionFunction function = ExpressionFunction.parse("efProbe(x)=x+1");
        assertEquals("efProbe", function.getName());
        assertEquals(1, function.getParamSize());
        assertEquals("efProbe(x)=x+1", function.toString());
        assertEquals(3.0, function.run(2.0), 0.0);
        assertEquals("2.0+1", function.explain(2.0));
        assertEquals("5.0+1", function.explain(5.0));
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> function.explain(1.0, 2.0)), "参数不正确");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            function.saveTo(out);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
            assertEquals("efProbe", ExpressionFunction.readFrom(in).getName());
        }
    }

    @Test
    @DisplayName("ExpressionFunction.parse 的错误分支")
    void expressionFunctionParseErrors() {
        assertMessageContains(assertThrows(UnsupportedOperationException.class,
                () -> ExpressionFunction.parse("moo")), "不属于函数");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> ExpressionFunction.parse(")=x")), "请您将函数名字写上");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> ExpressionFunction.parse("ab=x")), "函数名提取失败");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> ExpressionFunction.parse("efB(x)=y")), "Unknown formal parameter");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> ExpressionFunction.parse("efC(x)=y+1")), "Unknown formal parameter");
        assertMessageContains(assertThrows(ExtractException.class,
                () -> ExpressionFunction.parse("(x)=x")), "函数名称中只能包含字母");
        // 没有右括号 -> params 为空，函数体变成空字符串
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> ExpressionFunction.parse("efD(x=x")), "表达式为空字符串");
    }

    @Test
    @DisplayName("EquationSolver 区间内部有根时的二分立即命中")
    void equationSolverBisectionInteriorRoot() {
        ExposedEquationSolver solver = new ExposedEquationSolver("2-x", 0, new NegLinearFunction());
        solver.detectInterval();
        solver.setBisectionLeft(0.0);
        solver.setBisectionRight(4.0);
        double[] result = solver.solveBisection(1e-12, 10);
        assertEquals(2.0, result[0], 1e-12);
        assertEquals(1.0, result[1], 1e-12);
    }

    @Test
    @DisplayName("EquationSolver 线性细化在区间内逐步推进（4..8 找到 7..8）")
    void equationSolverRefineLinearWalk() {
        ExposedEquationSolver solver = new ExposedEquationSolver("60-x*x", 3, new DecayFunction());
        solver.detectInterval();
        assertEquals(7.0, solver.getBisectionLeft(), 1e-12);
        assertEquals(8.0, solver.getBisectionRight(), 1e-12);
        assertEquals(7.5, solver.getNewtonInitialX(), 1e-12);
    }

    @Test
    @DisplayName("FunctionFormulaCalculation 无参构造、嵌套括号与空格公式")
    void functionFormulaCalculationNestedAndSpaced() throws Exception {
        FunctionFormulaCalculation calculation = new ProbeFunctionFormulaCalculation();
        // 无参构造生成的名字带 _temp 后缀
        assertTrue(calculation.getName().endsWith("_temp"));
        calculation.check("q(1+1) ");
        // 旧版 check 不支持嵌套括号，但会先走完 ')' 的计数分支
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> calculation.check("q((1+1))")), "括号对数");

        assertEquals(4.0, calculation.calculation("q((2))", true).getResult(), 0.0);
        assertEquals(7.0, calculation.calculation("q(2) + 3", true).getResult(), 0.0);
        assertEquals(5.0, calculation.calculation("q(2.5)", true).getResult(), 0.0);
        assertEquals(3.0, calculation.calculation("1 + 2", true).getResult(), 0.0);
    }

    @Test
    @DisplayName("FunctionFormulaCalculation2 无参构造、函数提取变体与 BigDecimal 分支")
    void functionFormulaCalculation2Variants() {
        FunctionFormulaCalculation2 calculation = new ProbeFunctionFormulaCalculation2();
        assertTrue(calculation.getName().endsWith("_temp"));

        Stack<Integer> start = new Stack<>();
        Stack<Integer> end = new Stack<>();
        Stack<String> names = new Stack<>();
        // 大写函数名走 BA..BZ 分支
        FunctionFormulaCalculation2.FunctionParameterExtraction("Sum(1,2)", start, end, names);
        assertEquals(List.of(4), new ArrayList<>(start));
        assertEquals(List.of(7), new ArrayList<>(end));
        assertEquals(List.of("Sum"), new ArrayList<>(names));

        start.clear();
        end.clear();
        names.clear();
        FunctionFormulaCalculation2.FunctionParameterExtraction("sum((1+2),3)", start, end, names);
        assertEquals(List.of(4), new ArrayList<>(start));
        assertEquals(List.of(11), new ArrayList<>(end));

        StringBuilder builder = new StringBuilder();
        calculation.FunctionParameterExtraction("sum(1,2) + 3",
                new Stack<>(), new Stack<>(), new Stack<>(), builder);
        assertEquals("0+3", builder.toString());

        Mathematical_Expression.Options.setUseBigDecimal(true);
        try {
            assertEquals(3.0, calculation.calculation("sum(1,2)", true).getResult(), 0.0);
        } finally {
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }

        // 同名函数重复出现时复用已建的 DataObj
        assertEquals(10.0, calculation.explain("sum(1,2)+sum(3,4)", true).getResult());
    }

    @Test
    @DisplayName("ComplexExpression 顶层运算符前的括号扫描与虚部访问")
    void complexExpressionBracketFirst() {
        ComplexExpression nested = ComplexExpression.compile("(1+2)+3i", uniq("complexNested"));
        assertEquals(3.0, nested.calculation(false).getResult(), 0.0);
        assertEquals("+3", nested.getFunctionExpression2().getExpressionStr());
        assertEquals(3.0, numeric(nested.getFunctionExpression2().calculation(true)), 0.0);
    }

    @Test
    @DisplayName("ComplexCalculation 的 compileBigDecimal(false) 与带括号 explain")
    void complexCalculationNestedExplain() {
        ComplexCalculation calculation = ComplexCalculation.getInstance(uniq("complexNestedCalc"));
        assertEquals(1.0, calculation.compileBigDecimal("1+2i", false)
                .calculationBigDecimals(false).getResult(), 0.0);

        LogResults explain = calculation.explain("(1+2)+3i", true);
        assertTrue(explain.getResult() instanceof CalculationComplexResults);
        assertEquals(3.0, ((CalculationComplexResults) explain.getResult()).getResult(), 0.0);
        assertEquals(3.0, ((CalculationComplexResults) explain.getResult())
                .toComplexNumber().getImaginary(), 0.0);
    }

    @Test
    @DisplayName("CumulativeCalculation 的 getInstance 类型冲突与 formatRequired=false 分支")
    void cumulativeCalculationEdgeBranches() {
        assertMessageContains(assertThrows(ExtractException.class,
                () -> CumulativeCalculation.getInstance(CalculationManagement.BRACKETS_CALCULATION_2_NAME)),
                "CumulativeCalculation");
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cumEdge"));
        assertThrows(StringIndexOutOfBoundsException.class,
                () -> calculation.explain("m[1,2,1](m)", false));
        assertThrows(StringIndexOutOfBoundsException.class,
                () -> calculation.compile("m[1,2,1](m)", false));
    }

    @Test
    @DisplayName("StrUtils 阶乘缓存与高位字符探测")
    void strUtilsFactorialCacheAndHighChars() {
        Mathematical_Expression.Options.setUseCache(true);
        try {
            assertEquals(24.0, StrUtils.stringToBigDecimal("4!").doubleValue(), 0.0);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("4!".hashCode()));
        } finally {
            Mathematical_Expression.Options.setUseCache(false);
        }
        assertNull(StrUtils.containsEnChar("~"));
        assertNull(StrUtils.containsEnChar("["));
    }

    @Test
    @DisplayName("PrefixExpressionOperation.check 的小数点与右括号分支")
    void prefixExpressionOperationCheckMore() throws Exception {
        PrefixExpressionOperation operation =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
        operation.check("1.5+2");
        assertMessageContains(assertThrows(WrongFormat.class,
                () -> operation.check("1)+2")), "只能解析无括号");
    }

    @Test
    @DisplayName("NumberCalculation.check 的阶乘末位与连续符号分支")
    void numberCalculationCheckMore() throws Exception {
        BracketsCalculation2 calculation = brackets();
        calculation.check("1!");
        calculation.check("1*+2");
        calculation.check("1--2");
    }

    @Test
    @DisplayName("BracketsCalculation2.explain 跳过空格")
    void bracketsCalculationExplainSpaces() {
        assertEquals(3.0, brackets().explain("1 + 2", true).getResult());
        assertEquals(9.0, brackets().explain("( 1 + 2 ) * 3", true).getResult());
    }

    @Test
    @DisplayName("区间求和 formatStr 去空白")
    void fastSumFormatStr() {
        FastSumOfIntervalsBrackets calculation =
                FastSumOfIntervalsBrackets.getInstance(uniq("fastSumFormat"));
        assertEquals("1,10", calculation.formatStr(" 1,10 "));
        assertEquals("2,6", FastMultiplyOfIntervalsBrackets
                .getInstance(uniq("fastMulFormat")).formatStr(" 2,6 "));
    }

    @Test
    @DisplayName("moduleLength 在单参 pow 下可用（3,4 -> 5）")
    void functionPackageModuleLengthWithCompatiblePow() {
        Mathematical_Expression.unregister_function("pow");
        Mathematical_Expression.register_function(new ManyToOneNumberFunction("pow") {
            @Override
            public double run(double... numbers) {
                return numbers[0] * numbers[0];
            }
        });
        try {
            assertEquals(5.0, mathFn("moduleLength").run(3, 4), 1e-12);
            assertEquals(Math.sqrt(13.0), mathFn("moduleLength").run(2, 3), 1e-12);
        } finally {
            Mathematical_Expression.unregister_function("pow");
            Mathematical_Expression.register_function(FunctionPackage.MATH);
        }
    }

    @Test
    @DisplayName("JvmExpressionFunction 静态方法源码的多参/空参与缺右括号")
    void jvmExpressionFunctionStaticSourceVariants() {
        assertEquals("public static double f(double x, double y) { return x + y; }",
                JvmExpressionFunction.convertToStaticMethodSource("f(x, y) = x + y"));
        assertEquals("public static double f() { return 1; }",
                JvmExpressionFunction.convertToStaticMethodSource("f() = 1"));
        assertMessageContains(assertThrows(IllegalArgumentException.class,
                () -> JvmExpressionFunction.parse("f(x=1")), "函数定义需要包含参数列表");
    }

    @Test
    @DisplayName("CumulativeCalculation 的 compileBigDecimal(false) 分支")
    void cumulativeCalculationCompileBigDecimalRaw() {
        CumulativeCalculation calculation = CumulativeCalculation.getInstance(uniq("cumRawBig"));
        assertEquals(6.0, numeric(calculation.compileBigDecimal("n[1,3,1](n)", true)
                .calculationBigDecimals(false)), 0.0);
        assertThrows(StringIndexOutOfBoundsException.class,
                () -> calculation.compileBigDecimal("m[1,2,1](m)", false));
    }

    @Test
    @DisplayName("DynamicClassLoader 用真实编译产物定义类字节")
    void dynamicClassLoaderDefinesGeneratedClass() throws Exception {
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);

        InMemoryJavaFileObject classFile = (InMemoryJavaFileObject) instantiate(
                constructor(InMemoryJavaFileObject.class, String.class, JavaFileObject.Kind.class),
                "EngineProbeGenerated", JavaFileObject.Kind.CLASS);
        JavaFileObject source = new javax.tools.SimpleJavaFileObject(
                java.net.URI.create("string:///EngineProbeGenerated.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return "public class EngineProbeGenerated {"
                        + " public static int answer() { return 42; } }";
            }
        };
        javax.tools.JavaFileManager standard = compiler.getStandardFileManager(null, null, null);
        javax.tools.JavaFileManager manager =
                new javax.tools.ForwardingJavaFileManager<javax.tools.JavaFileManager>(standard) {
                    @Override
                    public JavaFileObject getJavaFileForOutput(javax.tools.JavaFileManager.Location location,
                                                               String className, JavaFileObject.Kind kind,
                                                               javax.tools.FileObject sibling) {
                        return classFile;
                    }
                };
        Boolean compiled = compiler.getTask(null, manager, null, null, null, List.of(source)).call();
        assertEquals(Boolean.TRUE, compiled);

        DynamicClassLoader loader = (DynamicClassLoader) instantiate(constructor(DynamicClassLoader.class));
        call(method(DynamicClassLoader.class, "addClass", String.class, InMemoryJavaFileObject.class),
                loader, "EngineProbeGenerated", classFile);
        Class<?> generated = loader.loadClass("EngineProbeGenerated");
        assertEquals("EngineProbeGenerated", generated.getName());
        assertEquals(42, generated.getMethod("answer").invoke(null));
    }

    @Test
    @DisplayName("ExpressionFunction 依据“名字+后续字符”判断函数调用")
    void expressionFunctionSkipsRegisteredFunctionName() {
        // parse 用 group + 下一个字符 作为函数存在性判断依据，因此注册 "fun+" 可走 isFunctionExist 的 true 分支
        CalculationManagement.register((Function) () -> "fun+");
        try {
            // 该分支会继续往下构建表达式，最终因为缺少函数括号而在校验阶段失败
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("efFun(x)=x+fun+x"));
        } finally {
            CalculationManagement.unregisterF("fun+");
        }
    }
}
