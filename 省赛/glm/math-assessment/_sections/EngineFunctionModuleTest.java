package net.mooctest;

import net.mooctest.expression.engine.BooleanCalculation;
import net.mooctest.expression.engine.BooleanCalculation2;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.Calculation;
import net.mooctest.expression.engine.CalculationComplexResults;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
import net.mooctest.expression.engine.ComplexCalculation;
import net.mooctest.expression.engine.ComplexExpression;
import net.mooctest.expression.engine.ConstantRegion;
import net.mooctest.expression.engine.CumulativeCalculation;
import net.mooctest.expression.engine.DynamicClassLoader;
import net.mooctest.expression.engine.DynamicFunctionCompiler;
import net.mooctest.expression.engine.EquationSolver;
import net.mooctest.expression.engine.EquationSolverExpression;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.ExpressionFunction;
import net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets;
import net.mooctest.expression.engine.FastSumOfIntervalsBrackets;
import net.mooctest.expression.engine.FunctionExpression;
import net.mooctest.expression.engine.FunctionFormulaCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation2;
import net.mooctest.expression.engine.FunctionPackage;
import net.mooctest.expression.engine.Functions;
import net.mooctest.expression.engine.InMemoryJavaFileObject;
import net.mooctest.expression.engine.JvmCalculation;
import net.mooctest.expression.engine.JvmExpression;
import net.mooctest.expression.engine.JvmExpressionFunction;
import net.mooctest.expression.engine.LogResults;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.ManyToOneNumberFunction;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PackExpression;
import net.mooctest.expression.engine.ParsedSignature;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.SingletonEquationSolving;
import net.mooctest.expression.engine.SingletonEquationSolvingTwo;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Function/complex/equation/interval engine coverage that the preselection
 * suite could not provide: the new problem restores the full framework classes
 * whose stubbed variants were tested before.
 */
class EngineFunctionModuleTest {

    @BeforeEach
    void resetFunctionRegistries() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        try {
            Field calculations = CalculationManagement.class.getDeclaredField("STRING_CALCULATION_HASH_MAP");
            calculations.setAccessible(true);
            ((Map<?, ?>) calculations.get(null)).clear();
            Field functions = CalculationManagement.class.getDeclaredField("STRING_FUNCTION_HASH_MAP");
            functions.setAccessible(true);
            ((Map<?, ?>) functions.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot reset component registries", e);
        }
    }

    @AfterEach
    void restoreEngineFlags() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
    }

    private static void assertDecimalEquals(String expected, Object actual) {
        BigDecimal actualValue;
        try {
            actualValue = new BigDecimal(String.valueOf(actual));
        } catch (NumberFormatException e) {
            throw new AssertionError("unparseable actual value: <" + actual + ">", e);
        }
        assertEquals(0, new BigDecimal(expected).compareTo(actualValue),
                "expected " + expected + " but was " + actual);
    }

    /** Custom comparison function used to test function registry behaviour. */
    private static ManyToOneNumberFunction constantFunction(String name, double value) {
        return new ManyToOneNumberFunction(name) {
            @Override
            public double run(double... numbers) {
                return value;
            }
        };
    }

    @Nested
    class FunctionFormulaCalculation2Tests {

        @Test
        void mathPackageFunctionsCalculateThroughFormulas() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Math");
            assertDecimalEquals("7", c.calculation("1+sum(1,2,3)", true).getResult());
            assertDecimalEquals("1024", c.calculation("pow(2,10)", true).getResult());
            assertDecimalEquals("4", c.calculation("sqrt(9)+1", true).getResult());
            assertDecimalEquals("120", c.calculation("factorial(5)", true).getResult());
            assertDecimalEquals("6", c.calculation("avg(2,4)*2", true).getResult());
            // The shipped moduleLength implementation calls the pow function with a
            // single argument, so evaluating it through the formula fails with the
            // library's own index fault — a real behavioral quirk worth pinning.
            assertThrows(ArrayIndexOutOfBoundsException.class,
                    () -> c.calculation("moduleLength(3,4)", true).getResult());
        }

        @Test
        void trigonometricFunctionsCalculate() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Trig");
            assertDecimalEquals("0", c.calculation("sin(0)", true).getResult());
            assertDecimalEquals("1", c.calculation("cos(0)", true).getResult());
            assertDecimalEquals("0", c.calculation("tan(0)", true).getResult());
            assertDecimalEquals("1", c.calculation("sec(0)", true).getResult());
            assertDecimalEquals("1", c.calculation("csc(1.5707963267948966)", true).getResult());
            assertEquals(1, Math.round(c.calculation("cot(0.7853981633974483)", true).getResult().doubleValue()));
        }

        @Test
        void branchPackageFunctionsSelectBetweenResults() {
            Mathematical_Expression.register_function(FunctionPackage.BRANCH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Branch");
            assertDecimalEquals("7", c.calculation("ifEq(1,1,7,9)", true).getResult());
            assertDecimalEquals("9", c.calculation("ifEq(1,2,7,9)", true).getResult());
            assertDecimalEquals("7", c.calculation("ifGt(2,1,7,9)", true).getResult());
            assertDecimalEquals("9", c.calculation("ifGt(1,2,7,9)", true).getResult());
            assertDecimalEquals("7", c.calculation("ifLt(1,2,7,9)", true).getResult());
            assertDecimalEquals("9", c.calculation("ifLt(2,1,7,9)", true).getResult());
            assertDecimalEquals("7", c.calculation("ifGe(2,2,7,9)", true).getResult());
            assertDecimalEquals("9", c.calculation("ifGe(1,2,7,9)", true).getResult());
            assertDecimalEquals("7", c.calculation("ifLe(2,2,7,9)", true).getResult());
            assertDecimalEquals("9", c.calculation("ifLe(3,2,7,9)", true).getResult());
        }

        @Test
        void datePackageFunctionsConvertUnits() {
            Mathematical_Expression.register_function(FunctionPackage.DATE);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Date");
            assertDecimalEquals("1", c.calculation("msToS(1000)", true).getResult());
            assertDecimalEquals("2000", c.calculation("sToMs(2)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToMin(60000)", true).getResult());
            assertDecimalEquals("120000", c.calculation("minToMs(2)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToHour(3600000)", true).getResult());
            assertDecimalEquals("7200000", c.calculation("hourToMs(2)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToDay(86400000)", true).getResult());
            // String.valueOf(172800000.0) yields "1.728E8", and the bracket parser
            // concatenates the trailing exponent digits instead of honoring the
            // scientific notation — a deterministic quirk of the shipped engine.
            assertDecimalEquals("1.7288", c.calculation("dayToMs(2)", true).getResult());
            assertDecimalEquals("864000", c.calculation("dayToMs(0.01)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToWeek(604800000)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToMonth(2592000000)", true).getResult());
            assertDecimalEquals("1", c.calculation("msToYear(31536000000)", true).getResult());
        }

        @Test
        void functionArityErrorsAreDescriptive() {
            UnsupportedOperationException avg = assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck("avg")).run(2, 4));
            assertTrue(avg.getMessage().contains("求均值前的求和函数"));
            UnsupportedOperationException module = assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck("moduleLength")).run(3, 4));
            assertTrue(module.getMessage().contains("pow"));
        }

        @Test
        void branchFunctionsRejectWrongArity() {
            assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifEq")).run(1, 2, 3));
            assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifGt")).run(1, 2, 3, 4, 5));
            assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifLt")).run(1));
            assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifGe")).run());
            assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.BRANCH.getFunctionByNameNoCheck("ifLe")).run(1, 2));
        }

        @Test
        void directFunctionRunsReturnIndependentExpectedValues() {
            ManyToOneNumberFunction sum = (ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck("sum");
            assertEquals(6.0, sum.run(1, 2, 3));
            assertEquals(0.0, sum.run());
            ManyToOneNumberFunction dayToMs = (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck("dayToMs");
            assertEquals(1.728E8, dayToMs.run(2));
            ManyToOneNumberFunction weekToMs = (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck("weekToMs");
            assertEquals(1.0, weekToMs.run(1) / 604800000.0);
            ManyToOneNumberFunction monthToMs = (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck("monthToMs");
            assertEquals(1.0, monthToMs.run(1) / 2592000000.0);
            ManyToOneNumberFunction yearToMs = (ManyToOneNumberFunction) FunctionPackage.DATE.getFunctionByNameNoCheck("yearToMs");
            assertEquals(1.0, yearToMs.run(1) / 31536000000.0);
        }

        @Test
        void functionPackageMetadataIsExposed() {
            assertEquals("Math", FunctionPackage.MATH.getName());
            assertEquals("Date", FunctionPackage.DATE.getName());
            assertEquals("Branch", FunctionPackage.BRANCH.getName());
            assertNull(FunctionPackage.MATH.getFunctionByNameNoCheck("missing"));
            HashMap<String, net.mooctest.expression.engine.Function> mathFunctions = FunctionPackage.MATH.getFunctionMap();
            assertEquals(12, mathFunctions.size());
            assertTrue(mathFunctions.containsKey("sum"));
            assertTrue(mathFunctions.containsKey("csc"));
        }

        @Test
        void checkRejectsUnbalancedFunctionBrackets() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Check");
            WrongFormat wrong = assertThrows(WrongFormat.class, () -> c.check("sum(1,2"));
            assertTrue(wrong.getMessage().contains("Missing function bracket logarithm: 1"));
            WrongFormat wrong2 = assertThrows(WrongFormat.class, () -> c.check("sum(1,2)+pow(2,3"));
            assertTrue(wrong2.getMessage().contains("Missing function bracket logarithm: 1"));
            assertDoesNotThrow(() -> c.check("1+2*3"));
        }

        @Test
        void checkPopulatesSharedPoolAndClearCacheDropsIt() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Pool");
            assertFalse(c.isCache("sum(1,2)+1"));
            assertDoesNotThrow(() -> c.check("sum(1,2)+1"));
            assertTrue(c.isCache("sum(1,2)+1"));
            c.clearCache();
            assertFalse(c.isCache("sum(1,2)+1"));
        }

        @Test
        void sharedPoolReturnsSameResultInstance() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Shared");
            CalculationNumberResults first = c.calculation("sum(9,9)", true);
            assertDecimalEquals("18", first.getResult());
            CalculationNumberResults second = c.calculation("sum(9,9)", true);
            assertSame(first, second);
            c.clearCache();
            CalculationNumberResults third = c.calculation("sum(9,9)", true);
            assertNotSame(first, third);
            assertDecimalEquals("18", third.getResult());
        }

        @Test
        void sharedPoolCanBeDisabled() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2NoPool");
            assertTrue(c.isStartSharedPool());
            c.setStartSharedPool(false);
            assertFalse(c.isStartSharedPool());
            assertDecimalEquals("6", c.calculation("sum(3,3)", true).getResult());
            assertDecimalEquals("6", c.calculation("sum(3,3)", true).getResult());
            assertFalse(c.isCache("sum(3,3)"));
        }

        @Test
        void explainProducesResultAndLogStructure() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Explain");
            LogResults log = c.explain("sum(1,2)+1", true);
            assertDecimalEquals("4", log.getResult());
        }

        @Test
        void staticParameterExtractionLocatesFunctions() {
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction("sum(1,2)+pow(2,3)", start, end, names);
            assertEquals(2, names.size());
            assertEquals(List.of("sum", "pow"), List.copyOf(names));
            assertEquals(List.of(4, 13), List.copyOf(start));
            assertEquals(List.of(7, 16), List.copyOf(end));
        }

        @Test
        void extractionWithBuilderReplacesFunctionsWithZero() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Builder");
            StringBuilder builder = new StringBuilder();
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            c.FunctionParameterExtraction("sum(1,2)+1", start, end, names, builder);
            assertEquals("0+1", builder.toString());
            assertEquals(1, names.size());
            assertEquals("sum", names.pop());
        }

        @Test
        void compiledExpressionSubCompilesToBracketExpression() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Compile");
            FunctionExpression fe = c.compile("sum(1,2)+1", true);
            assertFalse(fe.notNeedSubCompile());
            assertTrue(fe.isBigDecimal());
            assertTrue(fe.isUnBigDecimal());
            assertDoesNotThrow(fe::convertToMultiPrecisionSupported);
            NameExpression sub = fe.subCompile(true);
            assertDecimalEquals("4", sub.calculationCache(false).getResult());
            NameExpression subBig = fe.subCompileBigDecimals(true);
            assertDecimalEquals("4", subBig.calculationBigDecimalsCache(false).getResult());
        }

        @Test
        void compiledExpressionWithoutFunctionsNeedsNoSubCompile() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Plain");
            FunctionExpression fe = c.compile("1+2", true);
            assertTrue(fe.notNeedSubCompile());
            assertDecimalEquals("3", fe.calculationCache(true).getResult());
            UnsupportedOperationException unsupported = assertThrows(UnsupportedOperationException.class,
                    () -> fe.subCompile(true));
            assertTrue(unsupported.getMessage().contains("此表达式不需要深度编译了"));
        }

        @Test
        void compiledExpressionStaysUsableAcrossCopiedCalculations() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2Reuse");
            FunctionExpression fe = c.compile("sum(2,3)+1", true);
            assertDecimalEquals("6", fe.calculationCache(true).getResult());
            assertDecimalEquals("6", fe.calculationCache(true).getResult());
            assertDecimalEquals("6", fe.calculationCache(false).getResult());
            assertDecimalEquals("6", fe.calculationCache(true).getResult());
        }

        @Test
        void setFunNameRedirectsFunctionCall() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2FunName");
            FunctionExpression fe = c.compile("sum(1,2)+1", true);
            fe.setFunName(0, "pow");
            NameExpression sub = fe.subCompile(true);
            assertDecimalEquals("2", sub.calculationCache(false).getResult());
        }

        @Test
        void calculationWithoutFormatStillComputes() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("engFfc2NoFormat");
            assertDecimalEquals("4", c.calculation("sum(1, 2)+1", true).getResult());
            assertDecimalEquals("10", c.calculation("10", true).getResult());
        }

        @Test
        void getInstanceRejectsForeignComponentUnderSameName() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardFfc2");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> FunctionFormulaCalculation2.getInstance("engTypeGuardFfc2"));
            assertTrue(ex.getMessage().contains("不属于 FunctionFormulaCalculation2 类型"));
        }
    }

    @Nested
    class FunctionFormulaCalculationTests {

        @Test
        void singleParameterFunctionsAreInlinedBeforeBracketCalculation() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("engFfc1");
            assertDecimalEquals("1", c.calculation("1+sin(0)", true).getResult());
            assertDecimalEquals("2", c.calculation("cos(0)*2", true).getResult());
            assertDecimalEquals("4", c.calculation("sum(3)+1", true).getResult());
        }

        @Test
        void formatStrRemovesAllWhitespace() {
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("engFfc1Format");
            assertEquals("1+2", c.formatStr(" 1 + 2 "));
            assertEquals("1*2", c.formatStr("1\t*\t2"));
        }

        @Test
        void checkRejectsUnbalancedBrackets() {
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("engFfc1Check");
            WrongFormat wrong = assertThrows(WrongFormat.class, () -> c.check("sin(1"));
            assertTrue(wrong.getMessage().contains("Missing function bracket logarithm: 1"));
            WrongFormat wrong2 = assertThrows(WrongFormat.class, () -> c.check("sin(1)+cos(2"));
            assertTrue(wrong2.getMessage().contains("Missing function bracket logarithm: 1"));
        }

        @Test
        void getInstanceRejectsForeignComponentUnderSameName() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardFfc1");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> FunctionFormulaCalculation.getInstance("engTypeGuardFfc1"));
            assertTrue(ex.getMessage().contains("不属于 FunctionFormulaCalculation 类型"));
        }

        @Test
        void calculationDefaultsToFormattedMode() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("engFfc1Default");
            assertDecimalEquals("1", c.calculation("1+sin(0)").getResult());
        }
    }

    @Nested
    class ExpressionFunctionTests {

        @Test
        void parsedFunctionExplainsAndRuns() throws WrongFormat {
            ExpressionFunction ef = ExpressionFunction.parse("f(x) = x * 2 + x");
            assertEquals(1, ef.getParamSize());
            assertEquals("3.0* 2 +3.0", ef.explain(3));
            assertEquals(9.0, ef.run(3));
            assertEquals("f(x) = x * 2 + x", ef.toString());
        }

        @Test
        void multiParameterFunctionKeepsParameterOrder() throws WrongFormat {
            ExpressionFunction ef = ExpressionFunction.parse("g(a, b) = a * b - a + b");
            assertEquals(2, ef.getParamSize());
            assertEquals("3.0*4.0-3.0+4.0", ef.explain(3, 4));
            assertEquals(13.0, ef.run(3, 4));
            assertEquals(7.0, ef.run(2, 3));
        }

        @Test
        void functionRejectsWrongArity() throws WrongFormat {
            ExpressionFunction ef = ExpressionFunction.parse("f(x) = x + 1");
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> ef.explain(1, 2));
            assertTrue(ex.getMessage().contains("期望参数数量为：1"));
            assertTrue(ex.getMessage().contains("实际参数数量为：2"));
            UnsupportedOperationException run = assertThrows(UnsupportedOperationException.class,
                    () -> ef.run());
            assertTrue(run.getMessage().contains("期望参数数量为：1"));
            assertTrue(run.getMessage().contains("实际参数数量为：0"));
        }

        @Test
        void parseRejectsExpressionsWithoutEquals() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> ExpressionFunction.parse("f(x) x*2"));
            assertTrue(ex.getMessage().contains("您的表达式不属于函数"));
            UnsupportedOperationException noEq = assertThrows(UnsupportedOperationException.class,
                    () -> ExpressionFunction.parse("f(x)"));
            assertTrue(noEq.getMessage().contains("您的表达式不属于函数"));
        }

        @Test
        void parseRejectsMissingFunctionName() {
            WrongFormat ex = assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("=x+1"));
            assertTrue(ex.getMessage().contains("您的函数名提取失败"));
        }

        @Test
        void parseRejectsUnknownFormalParameter() {
            WrongFormat trailing = assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("f(x) = x + y"));
            assertEquals("Unknown formal parameter [y] comes from [x + y].", trailing.getMessage());
            WrongFormat middle = assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("f(x) = x + y * 2"));
            assertEquals("Unknown formal parameter [y or y ] comes from [x + y * 2].", middle.getMessage());
        }

        @Test
        void parseAcceptsRegisteredFunctionCallsInsideBody() throws WrongFormat {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            ExpressionFunction ef = ExpressionFunction.parse("f(x) = sin(x) + x");
            assertEquals(1, ef.getParamSize());
            assertEquals(0.0, ef.run(0));
        }

        @Test
        void serializationRoundTripPreservesFunction() throws Exception {
            ExpressionFunction ef = ExpressionFunction.parse("f(x) = x * 3 + 1");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                ef.saveTo(out);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                ManyToOneNumberFunction restored = ExpressionFunction.readFrom(in);
                assertEquals(10.0, restored.run(3));
            }
        }

        @Test
        void registryRegistrationRejectsDuplicateNames() throws WrongFormat {
            assertTrue(Mathematical_Expression.register_function("ffA(x) = x + 1"));
            assertFalse(Mathematical_Expression.register_function("ffA(x) = x + 2"));
            assertEquals(5.0, Mathematical_Expression.getFunction("ffA").run(4));
        }
    }

    @Nested
    class FunctionRegistryTests {

        @Test
        void calculationRegistrationLifecycle() {
            BracketsCalculation2 component = BracketsCalculation2.getInstance("engRegistryCalc");
            assertTrue(CalculationManagement.unregister("engRegistryCalc"));
            assertFalse(CalculationManagement.unregister("engRegistryCalc"));
            assertFalse(CalculationManagement.isRegister("engRegistryCalc"));
            assertTrue(CalculationManagement.register(component, true));
            assertTrue(CalculationManagement.isRegister("engRegistryCalc"));
            assertFalse(CalculationManagement.register(component, true));
            assertTrue(CalculationManagement.register(component, false));
            assertSame(component, CalculationManagement.getCalculationByName("engRegistryCalc"));
            assertNull(CalculationManagement.getCalculationByName("engMissingCalc"));
        }

        @Test
        void functionRegistrationLifecycle() {
            ManyToOneNumberFunction fn = constantFunction("engRegistryFn", 42);
            assertFalse(CalculationManagement.isFunctionExist("engRegistryFn"));
            assertTrue(CalculationManagement.register(fn));
            assertFalse(CalculationManagement.register(fn));
            assertTrue(CalculationManagement.isFunctionExist("engRegistryFn"));
            assertSame(fn, CalculationManagement.getFunctionByName("engRegistryFn"));
            assertTrue(CalculationManagement.getFunctionMap().contains("engRegistryFn"));
            assertTrue(CalculationManagement.unregisterF("engRegistryFn"));
            assertFalse(CalculationManagement.unregisterF("engRegistryFn"));
            assertFalse(CalculationManagement.isFunctionExist("engRegistryFn"));
        }

        @Test
        void getFunctionByNameRejectsMissingFunction() {
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> CalculationManagement.getFunctionByName("engMissingFn"));
            assertTrue(ex.getMessage().contains("engMissingFn"));
            assertNull(CalculationManagement.getFunctionByNameNoCheck("engMissingFn"));
        }

        @Test
        void functionPackageRegistrationRegistersEveryFunction() {
            Mathematical_Expression.register_function(FunctionPackage.DATE);
            assertTrue(CalculationManagement.isFunctionExist("msToS"));
            assertTrue(CalculationManagement.isFunctionExist("yearToMs"));
            assertSame(FunctionPackage.DATE.getFunctionByNameNoCheck("msToS"),
                    CalculationManagement.getFunctionByNameNoCheck("msToS"));
        }

        @Test
        void functionToStringAndSerializationFlag() {
            ManyToOneNumberFunction fn = constantFunction("engFnMeta", 1);
            assertEquals("engFnMeta", fn.getName());
            assertTrue(fn.AllowSerialization());
        }
    }

    @Nested
    class MathematicalExpressionPortalTests {

        @Test
        void getInstanceReturnsComponentPerEnumConstant() {
            assertInstanceOf(BracketsCalculation2.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.bracketsCalculation2, "engPortal1"));
            assertInstanceOf(CumulativeCalculation.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.cumulativeCalculation, "engPortal2"));
            assertInstanceOf(FastMultiplyOfIntervalsBrackets.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.fastMultiplyOfIntervalsBrackets, "engPortal3"));
            assertInstanceOf(FastSumOfIntervalsBrackets.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.fastSumOfIntervalsBrackets, "engPortal4"));
            assertInstanceOf(FunctionFormulaCalculation.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.functionFormulaCalculation, "engPortal5"));
            assertInstanceOf(FunctionFormulaCalculation2.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.functionFormulaCalculation2, "engPortal6"));
            assertInstanceOf(PrefixExpressionOperation.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.prefixExpressionOperation, "engPortal7"));
            assertInstanceOf(BooleanCalculation2.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.booleanCalculation2, "engPortal8"));
            assertInstanceOf(ComplexCalculation.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.complexCalculation, "engPortal9"));
            assertInstanceOf(JvmCalculation.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.jvmCalculation, "engPortal10"));
            assertInstanceOf(SingletonEquationSolving.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.singleEquationSolving, "engPortal11"));
            assertInstanceOf(SingletonEquationSolvingTwo.class,
                    Mathematical_Expression.getInstance(Mathematical_Expression.singleEquationSolving2, "engPortal12"));
        }

        @Test
        void getInstanceWithoutNameUsesEnumName() {
            Calculation byEnum = Mathematical_Expression.getInstance(Mathematical_Expression.bracketsCalculation2);
            assertEquals("bracketsCalculation2", byEnum.getName());
        }

        @Test
        void annotatedClassRegistersEveryFunctionExpression() throws WrongFormat {
            assertTrue(Mathematical_Expression.register_function(AnnotatedFunctions.class));
            assertNotNull(Mathematical_Expression.getFunction("annFnOne"));
            assertNotNull(Mathematical_Expression.getFunction("annFnTwo"));
            assertFalse(Mathematical_Expression.register_function(PlainClass.class));
        }

        @Test
        void annotatedJvmClassRegistersMethodSources() {
            assertTrue(Mathematical_Expression.register_jvm_function(AnnotatedFunctions.class));
            assertTrue(JvmExpressionFunction.getRegisteredMethods().containsKey("double annFnOne"));
            assertFalse(Mathematical_Expression.register_jvm_function(PlainClass.class));
        }

        @Test
        void jvmFunctionRegistrationAcceptsMethodSource() {
            JvmExpressionFunction.registerJvmFunction(
                    "public static double engJvmA(double x) { return x + x; }");
            JvmExpressionFunction.registerFunction("engJvmB(x) = x * 3");
            Map<String, String> methods = JvmExpressionFunction.getRegisteredMethods();
            assertTrue(methods.containsKey("double engJvmA"));
            assertTrue(methods.containsKey("double engJvmB"));
            assertEquals("public static double engJvmC(double x) { return x + x; }",
                    JvmExpressionFunction.convertToStaticMethodSource("engJvmC(x) = x + x"));
        }

        @Test
        void jvmFunctionRegistrationRejectsMalformedSources() {
            IllegalArgumentException signature = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.registerJvmFunction("static double f(double x) { return 1; }"));
            assertTrue(signature.getMessage().contains("函数方法签名格式错误"));
            IllegalArgumentException format = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.registerJvmFunction("public static double f"));
            assertTrue(format.getMessage().contains("函数方法格式错误"));
        }

        @Test
        void functionFileRoundTripRegistersSavedFunctions() throws Exception {
            ExpressionFunction fn = ExpressionFunction.parse("engSavedFn(x) = x * 4");
            File target = File.createTempFile("engFn", ".ser");
            try {
                Mathematical_Expression.saveFunction(fn, target);
                assertTrue(Mathematical_Expression.register_function(target));
                assertEquals(8.0, Mathematical_Expression.getFunction("engSavedFn").run(2));
            } finally {
                assertTrue(target.delete());
            }
        }

        @Test
        void functionFileWithForeignObjectIsRejected() throws Exception {
            File target = File.createTempFile("engForeign", ".ser");
            try {
                try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(target.toPath()))) {
                    out.writeObject("not a function");
                }
                assertFalse(Mathematical_Expression.register_function(target));
            } finally {
                assertTrue(target.delete());
            }
        }

        @Test
        void functionStreamRegistrationCountsSuccessesAndFailures() throws Exception {
            ExpressionFunction first = ExpressionFunction.parse("engStreamFn(x) = x + 7");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject(first);
                out.writeObject(first);
            }
            try (InputStream in = new ByteArrayInputStream(buffer.toByteArray())) {
                Map.Entry<Integer, Integer> result = Mathematical_Expression.register_function(in);
                assertEquals(1, result.getKey());
                assertEquals(1, result.getValue());
            }
        }

        @Test
        void emptyFunctionStreamFailsAtStreamHeader() {
            // An empty byte array has no ObjectInputStream stream header, so the
            // constructor itself fails before any registration attempt.
            assertThrows(java.io.EOFException.class,
                    () -> Mathematical_Expression.register_function(new ByteArrayInputStream(new byte[0])));
        }

        @Test
        void unregisterFunctionRemovesItFromRegistry() throws WrongFormat {
            Mathematical_Expression.register_function("engUnregFn(x) = x - 1");
            assertTrue(Mathematical_Expression.unregister_function("engUnregFn"));
            assertFalse(Mathematical_Expression.unregister_function("engUnregFn"));
            assertNull(CalculationManagement.getFunctionByNameNoCheck("engUnregFn2"));
        }

        @Test
        void optionsManageCachesAndPrecision() {
            assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
            Mathematical_Expression.Options.setUseBigDecimal(false);

            Mathematical_Expression.Options.cacheCalculation(123456, 9.0);
            assertEquals(9.0, Mathematical_Expression.Options.getCacheCalculation(123456));
            assertNull(Mathematical_Expression.Options.getCacheCalculation(654321));

            Mathematical_Expression.Options.setUseCache(true);
            assertTrue(Mathematical_Expression.Options.isUseCache());
            Mathematical_Expression.Options.cacheCalculation("engOptFormula", null);
            Mathematical_Expression.Options.setUseCache(false);
            assertFalse(Mathematical_Expression.Options.isUseCache());
            assertNull(Mathematical_Expression.Options.getCacheCalculation("engOptFormula"));
            assertNull(Mathematical_Expression.Options.getCacheCalculation(123456));
        }

        @Test
        void optionsFormulaCacheStoresCompiledExpressions() {
            Mathematical_Expression.Options.setUseCache(true);
            PackExpression compiled = BracketsCalculation2.getInstance("engPortalCache").compile("1+2", true);
            Mathematical_Expression.Options.cacheCalculation("engPortalFormula", compiled);
            assertSame(compiled, Mathematical_Expression.Options.getCacheCalculation("engPortalFormula"));
            Mathematical_Expression.Options.setUseCache(false);
            assertNull(Mathematical_Expression.Options.getCacheCalculation("engPortalFormula"));
        }

        @Test
        void booleanCalculationOverloadWiresNumberComponent() throws Exception {
            BooleanCalculation2 bc = BooleanCalculation2.getInstance("engBoolOverload");
            bc.setCalculation(Mathematical_Expression.cumulativeCalculation);
            Field delegate = BooleanCalculation.class.getDeclaredField("numberCalculation");
            delegate.setAccessible(true);
            assertInstanceOf(CumulativeCalculation.class, delegate.get(bc));
            assertThrows(ClassCastException.class, () -> bc.setCalculation(Mathematical_Expression.booleanCalculation2));
        }

        @Test
        void functionPackageValuesExposeEveryEntry() {
            assertEquals(3, FunctionPackage.values().length);
            assertSame(FunctionPackage.MATH, FunctionPackage.valueOf("MATH"));
        }

        @Functions({"annFnOne(x) = x + 1", "annFnTwo(x) = x * 2"})
        static class AnnotatedFunctions {
        }

        static class PlainClass {
        }
    }

    @Nested
    class JvmEngineTests {

        @Test
        void dynamicCompilationFailsWithoutFunctionLibraryDependency() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> DynamicFunctionCompiler.compile("engDynFn", "x*2", new String[]{"x"}));
            assertEquals("动态函数生成失败", ex.getMessage());
            assertNotNull(ex.getCause());
        }

        @Test
        void jvmFunctionParseFailsAtDynamicCompilationStage() {
            RuntimeException valid = assertThrows(RuntimeException.class,
                    () -> JvmExpressionFunction.parse("engP(x) = x * 2"));
            assertEquals("动态函数生成失败", valid.getMessage());
        }

        @Test
        void jvmFunctionSignatureValidationRejectsBadSignatures() {
            IllegalArgumentException noEquals = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.parse("engP(x) x*2"));
            assertTrue(noEquals.getMessage().contains("签名必须包含 '='"));
            IllegalArgumentException twoEquals = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.parse("engP(x) = x = 2"));
            assertTrue(twoEquals.getMessage().contains("签名不能包含多个 '='"));
            IllegalArgumentException noParams = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.parse("engP = x*2"));
            assertTrue(noParams.getMessage().contains("函数定义需要包含参数列表"));
            IllegalArgumentException digitName = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.parse("engP2(x) = x*2"));
            assertTrue(digitName.getMessage().contains("函数名称不能包含数字"));
        }

        @Test
        void reflectiveJvmFunctionDelegatesRunsAndExplains() throws Exception {
            JvmExpressionFunction jf = reflectiveJvmFunction("f", "x*2", numbers -> numbers[0] * 2);
            assertEquals(10.0, jf.run(5));
            assertEquals("5.0*2", jf.explain(new double[]{5}));
            assertEquals("f(x)=x*2", jf.toString());
            assertEquals(1, jf.getParamSize());
            IllegalArgumentException runArity = assertThrows(IllegalArgumentException.class,
                    () -> jf.run(1, 2));
            assertTrue(runArity.getMessage().contains("参数数量不匹配: 应为 1 个, 实际为 2"));
            IllegalArgumentException explainArity = assertThrows(IllegalArgumentException.class,
                    () -> jf.explain(new double[0]));
            assertTrue(explainArity.getMessage().contains("参数数量不匹配 需要 1 个, 实际为 0"));
            IllegalArgumentException objectArity = assertThrows(IllegalArgumentException.class,
                    () -> jf.explain(new Object[]{1.0, 2.0}));
            assertTrue(objectArity.getMessage().contains("参数数量不匹配 需要 1 个, 实际为 2"));
            assertEquals("5.0*2", jf.explain(new Object[]{5.0}));
        }

        @Test
        void reflectiveJvmExpressionCalculatesAndClones() throws Exception {
            JvmExpressionFunction jf = reflectiveJvmFunction("f", "x*2", numbers -> numbers[0] * 2);
            JvmExpression je = reflectiveJvmExpression("engJvmExpr", new double[]{5.0}, jf, "x*2");
            assertDecimalEquals("10", je.calculation(false).getResult());
            assertEquals(1, je.getLength());
            assertEquals(5.0, je.getParamNumber(0));
            je.setParamNumber(0, 7.5);
            assertEquals(7.5, je.getParamNumber(0));
            assertEquals(7.5, je.iterator().sum());
            assertEquals("7.5*2", je.getExpressionStrAndNumber());
            assertFalse(je.isBigDecimal());
            assertTrue(je.isUnBigDecimal());
            UnsupportedOperationException precision = assertThrows(UnsupportedOperationException.class,
                    je::convertToMultiPrecisionSupported);
            assertTrue(precision.getMessage().contains("does not support BigDecimal"));
            UnsupportedOperationException bigDecimal = assertThrows(UnsupportedOperationException.class,
                    () -> je.calculationBigDecimals(false));
            assertTrue(bigDecimal.getMessage().contains("does not support BigDecimal"));
            JvmExpression clone = je.cloneExpression();
            assertEquals("engJvmExpr_clone", clone.getCalculationName());
            clone.setParamNumber(0, 100);
            assertEquals(7.5, je.getParamNumber(0));
        }

        @Test
        void jvmCalculationComponentRejectsBigDecimalAndFailsToCompile() {
            JvmCalculation c = JvmCalculation.getInstance("engJvmCalc");
            assertEquals("engJvmCalc", c.getName());
            assertEquals(" 1 + 2 ", c.formatStr(" 1 + 2 "));
            assertDoesNotThrow(() -> c.check("any thing"));
            assertDoesNotThrow(() -> c.check(null));
            UnsupportedOperationException bigDecimal = assertThrows(UnsupportedOperationException.class,
                    () -> c.compileBigDecimal("1+2", true));
            assertTrue(bigDecimal.getMessage().contains("不支持 BigDecimal"));
            RuntimeException compile = assertThrows(RuntimeException.class,
                    () -> c.calculation("1+2", true));
            assertEquals("动态函数生成失败", compile.getMessage());
        }

        @Test
        void jvmCalculationInstanceRegistryGuardsTypes() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardJvm");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> JvmCalculation.getInstance("engTypeGuardJvm"));
            assertTrue(ex.getMessage().contains("不属于  DynamicEquationSolver 类型"));
        }

        @Test
        void singletonEquationSolvingComponentValidatesEquations() {
            SingletonEquationSolving s = SingletonEquationSolving.getInstance("engSes");
            assertEquals("engSes", s.getName());
            assertEquals(" 2 * x ", s.formatStr(" 2 * x "));
            UnsupportedOperationException bigDecimal = assertThrows(UnsupportedOperationException.class,
                    () -> s.compileBigDecimal("2*x=4", true));
            assertTrue(bigDecimal.getMessage().contains("不支持BigDecimal运算"));
            IllegalArgumentException badFormat = assertThrows(IllegalArgumentException.class,
                    () -> s.calculation("2*x", true));
            assertTrue(badFormat.getMessage().contains("方程表达式格式应为 '表达式=值'"));
            RuntimeException compile = assertThrows(RuntimeException.class,
                    () -> s.calculation("2*x=4", true));
            assertEquals("动态函数生成失败", compile.getMessage());
        }

        @Test
        void singletonEquationSolvingTwoValidatesEquations() {
            SingletonEquationSolvingTwo s = SingletonEquationSolvingTwo.getInstance("engSes2");
            IllegalArgumentException badFormat = assertThrows(IllegalArgumentException.class,
                    () -> s.calculation("x*2+1", true));
            assertTrue(badFormat.getMessage().contains("方程表达式格式应为 '表达式=值'"));
            RuntimeException compile = assertThrows(RuntimeException.class,
                    () -> s.calculation("2*x=4", true));
            assertEquals("动态函数生成失败", compile.getMessage());
            UnsupportedOperationException bigDecimal = assertThrows(UnsupportedOperationException.class,
                    () -> s.compileBigDecimal("2*x=4", true));
            assertTrue(bigDecimal.getMessage().contains("不支持BigDecimal运算"));
        }

        @Test
        void equationSolverCompileRejectsEquationsWithoutEquals() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EquationSolver.compile("2*x", "engSolver"));
            assertTrue(ex.getMessage().contains("方程表达式格式应为 '表达式=值'，实际：2*x"));
        }

        @Test
        void equationSolverCompileRejectsNonNumericRightSide() {
            assertThrows(NumberFormatException.class, () -> EquationSolver.compile("2*x=abc", "engSolverNan"));
        }

        @Test
        void equationSolverCompileFailsAtDynamicCompilation() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> EquationSolver.compile("2*x=10", "engSolverDyn"));
            assertEquals("动态函数生成失败", ex.getMessage());
        }

        @Test
        void newtonMethodConvergesInTwoStepsForLinearEquation() {
            LinearSolver solver = new LinearSolver("2*x", 10, numbers -> numbers[0] * 2);
            solver.setNewtonInitialX(2);
            double[] solution = solver.solveNewton(1e-9, 100);
            assertEquals(5.0, solution[0], 1e-9);
            assertEquals(2, (int) solution[1]);
        }

        @Test
        void newtonMethodReportsDerivativeUnderflow() {
            LinearSolver solver = new LinearSolver("7", 10, numbers -> 7.0);
            ArithmeticException ex = assertThrows(ArithmeticException.class,
                    () -> solver.solveNewton(1e-9, 100));
            assertEquals("导数过小 (|f'(x)| < 1e-12) 在 x=2.000000 处停止迭代", ex.getMessage());
        }

        @Test
        void newtonMethodReportsIterationExhaustion() {
            LinearSolver solver = new LinearSolver("2*x", 10, numbers -> numbers[0] * 2);
            solver.setNewtonInitialX(2);
            ArithmeticException ex = assertThrows(ArithmeticException.class,
                    () -> solver.solveNewton(0, 2));
            assertEquals("牛顿法在 2 次迭代后未收敛 (阈值 tol=0.00e+00)", ex.getMessage());
        }

        @Test
        void bisectionMethodConvergesWithinBracketingInterval() {
            LinearSolver solver = new LinearSolver("2*x", 10, numbers -> numbers[0] * 2);
            solver.setBisectionLeft(0);
            solver.setBisectionRight(10);
            double[] solution = solver.solveBisection(1e-9, 100);
            assertEquals(5.0, solution[0], 1e-9);
            assertEquals(1, (int) solution[1]);
        }

        @Test
        void bisectionMethodRejectsSameSignedInterval() {
            LinearSolver solver = new LinearSolver("2*x", 10, numbers -> numbers[0] * 2);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> solver.solveBisection(1e-9, 100));
            assertEquals("二分法初始区间 f(1.000000)=-8.000000 与 f(3.000000)=-4.000000 同号，无法保证有根",
                    ex.getMessage());
        }

        @Test
        void autoDetectIntervalBracketsPositiveRootForBisection() {
            LinearSolver solver = new LinearSolver("20-x", 0, numbers -> 20 - numbers[0]);
            solver.detectInterval();
            solver.setUseNewton(false);
            double[] solution = solver.solveBisection(1e-9, 200);
            assertEquals(20.0, solution[0], 1e-6);
            solver.setUseNewton(true);
            assertEquals(20.0, solver.solveNewton(1e-9, 100)[0], 1e-9);
            assertEquals(19.5, solver.getNewtonInitialX(), 1e-12);
            assertEquals(19.0, solver.getBisectionLeft(), 1e-12);
            assertEquals(20.0, solver.getBisectionRight(), 1e-12);
        }

        @Test
        void autoDetectIntervalFailsWhenRefinementCannotStep() throws Exception {
            Constructor<EquationSolverExpression> ctor = EquationSolverExpression.class.getDeclaredConstructor(
                    String.class, double.class, String.class, HashSet.class, JvmExpression.class, Object[].class);
            ctor.setAccessible(true);
            Constructor<ParsedSignature> signatureCtor = ParsedSignature.class
                    .getDeclaredConstructor(String.class, String[].class, String.class);
            signatureCtor.setAccessible(true);
            ParsedSignature signature = signatureCtor.newInstance("f", new String[]{"x"}, "-x-0.5");
            Constructor<JvmExpressionFunction> fnCtor = JvmExpressionFunction.class
                    .getDeclaredConstructor(ParsedSignature.class, ManyToOneNumberFunction.class);
            fnCtor.setAccessible(true);
            JvmExpressionFunction fn = fnCtor.newInstance(signature, new ManyToOneNumberFunction("f") {
                @Override
                public double run(double... numbers) {
                    return -numbers[0] - 0.5;
                }
            });
            Constructor<JvmExpression> exprCtor = JvmExpression.class
                    .getDeclaredConstructor(String.class, double[].class, JvmExpressionFunction.class, String.class);
            exprCtor.setAccessible(true);
            JvmExpression jvmExpression = exprCtor.newInstance("engNoRootJvm", new double[]{0.0}, fn, "-x-0.5");
            // The sign change is detected between 0 and -1, but the linear refinement
            // steps by +1 and can never reach a negative right bound.
            java.lang.reflect.InvocationTargetException wrapped = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> ctor.newInstance("-x-0.5", 0.0, "engEseNoRoot",
                            new HashSet<>(List.of(0)), jvmExpression, new Object[]{"x"}));
            assertInstanceOf(EquationSolver.NoRootFoundException.class, wrapped.getCause());
        }

        @Test
        void equationSolverExposesConfigurationAndPrecisionFlags() {
            LinearSolver solver = new LinearSolver("2*x", 10, numbers -> numbers[0] * 2);
            assertEquals(10.0, solver.getResult());
            assertTrue(solver.isUseNewton());
            solver.setUseNewton(false);
            assertFalse(solver.isUseNewton());
            solver.setTol(0.5);
            assertEquals(0.5, solver.getTol());
            solver.setMaxIter(50);
            assertEquals(50, solver.getMaxIter());
            solver.setNewtonInitialX(-3);
            assertEquals(-3, solver.getNewtonInitialX());
            solver.setBisectionLeft(-2);
            solver.setBisectionRight(2);
            assertEquals(-2, solver.getBisectionLeft());
            assertEquals(2, solver.getBisectionRight());
            assertFalse(solver.isBigDecimal());
            assertTrue(solver.isUnBigDecimal());
            UnsupportedOperationException precision = assertThrows(UnsupportedOperationException.class,
                    solver::convertToMultiPrecisionSupported);
            assertTrue(precision.getMessage().contains("不支持 BigDecimal 类型计算"));
            UnsupportedOperationException bigDecimal = assertThrows(UnsupportedOperationException.class,
                    () -> solver.calculationBigDecimals(false));
            assertTrue(bigDecimal.getMessage().contains("不支持高精度计算"));
            solver.setUseNewton(true);
            solver.setTol(1e-9);
            // The numerical derivative carries an epsilon, so convergence lands a
            // fraction away from the exact root after three Newton steps.
            double solved = ((Number) solver.calculation(false).getResult()).doubleValue();
            assertEquals(5.0, solved, 1e-6);
            assertEquals(3, solver.calculation(false).getResultLayers());
        }

        @Test
        void equationSolverExpressionSolvesAndMutatesKnownNumbers() throws Exception {
            EquationSolverExpression solver = reflectiveEquationSolverExpression("x*2", 10.0,
                    numbers -> numbers[0] * numbers[1], new double[]{0.0, 2.0});
            assertEquals(5.0, solver.calculation(false).getResult(), 1e-9);
            assertTrue(solver.indexIsUnKnownNumber(0));
            assertFalse(solver.indexIsUnKnownNumber(1));
            IllegalArgumentException unknownIndex = assertThrows(IllegalArgumentException.class,
                    () -> solver.setKnownNumber(0, 1));
            assertTrue(unknownIndex.getMessage().contains("未知数索引不能被修改"));

            solver.setUnKnownNumber(0, 4);
            assertFalse(solver.indexIsUnKnownNumber(0));
            ArithmeticException stuck = assertThrows(ArithmeticException.class,
                    () -> solver.solveNewton(1e-9, 100));
            assertTrue(stuck.getMessage().contains("导数过小"));

            IllegalArgumentException knownIndex = assertThrows(IllegalArgumentException.class,
                    () -> solver.setUnKnownNumber(1, 9));
            assertTrue(knownIndex.getMessage().contains("setUnKnownNumber(1, 9.0)"));

            assertEquals("4.0*2 = 10.0", solver.explain());
            assertNotNull(solver.getJvmExpression());
            EquationSolverExpression clone = solver.cloneExpression();
            assertTrue(clone.getCalculationName().endsWith("_clone"));
        }

        @Test
        void inMemoryClassObjectRoundTripsBytesThroughClassLoader() throws Exception {
            Constructor<InMemoryJavaFileObject> objectCtor = InMemoryJavaFileObject.class
                    .getDeclaredConstructor(String.class, javax.tools.JavaFileObject.Kind.class);
            objectCtor.setAccessible(true);
            InMemoryJavaFileObject source = objectCtor.newInstance(
                    "eng.test.Generated", javax.tools.JavaFileObject.Kind.SOURCE);
            byte[] payload = "class Generated {}".getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream out = source.openOutputStream()) {
                out.write(payload);
            }
            java.lang.reflect.Method getBytes = InMemoryJavaFileObject.class.getDeclaredMethod("getBytes");
            getBytes.setAccessible(true);
            assertArrayEquals(payload, (byte[]) getBytes.invoke(source));

            Constructor<DynamicClassLoader> loaderCtor = DynamicClassLoader.class.getDeclaredConstructor();
            loaderCtor.setAccessible(true);
            DynamicClassLoader loader = loaderCtor.newInstance();
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("eng.missing.NeverCompiled"));

            InMemoryJavaFileObject broken = objectCtor.newInstance(
                    "eng.mismatch.NotTheRequestedName", javax.tools.JavaFileObject.Kind.CLASS);
            try (java.io.OutputStream out = broken.openOutputStream()) {
                out.write("not real bytecode".getBytes(StandardCharsets.UTF_8));
            }
            java.lang.reflect.Method addClass = DynamicClassLoader.class
                    .getDeclaredMethod("addClass", String.class, InMemoryJavaFileObject.class);
            addClass.setAccessible(true);
            addClass.invoke(loader, "eng.mismatch.NotTheRequestedName", broken);
            // The bytes are registered but are not valid class data for the requested
            // name, so defineClass rejects them at load time.
            assertThrows(ClassFormatError.class,
                    () -> loader.loadClass("eng.mismatch.NotTheRequestedName"));
        }

        private JvmExpressionFunction reflectiveJvmFunction(String name, String expression,
                                                            java.util.function.Function<double[], Double> delegateFn) throws Exception {
            Constructor<ParsedSignature> signatureCtor = ParsedSignature.class
                    .getDeclaredConstructor(String.class, String[].class, String.class);
            signatureCtor.setAccessible(true);
            ParsedSignature signature = signatureCtor.newInstance(name, new String[]{"x"}, expression);
            Constructor<JvmExpressionFunction> ctor = JvmExpressionFunction.class
                    .getDeclaredConstructor(ParsedSignature.class, ManyToOneNumberFunction.class);
            ctor.setAccessible(true);
            return ctor.newInstance(signature, new ManyToOneNumberFunction(name) {
                @Override
                public double run(double... numbers) {
                    return delegateFn.apply(numbers);
                }
            });
        }

        private JvmExpression reflectiveJvmExpression(String name, double[] numbers,
                                                      JvmExpressionFunction fn, String expression) throws Exception {
            Constructor<JvmExpression> ctor = JvmExpression.class
                    .getDeclaredConstructor(String.class, double[].class, JvmExpressionFunction.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(name, numbers, fn, expression);
        }

        private EquationSolverExpression reflectiveEquationSolverExpression(
                String leftExpression, double result,
                java.util.function.Function<double[], Double> ignored,
                double[] initialValues) throws Exception {
            // Two-parameter signature: the unknown "x" is parameter 0, constants follow.
            Constructor<ParsedSignature> signatureCtor = ParsedSignature.class
                    .getDeclaredConstructor(String.class, String[].class, String.class);
            signatureCtor.setAccessible(true);
            ParsedSignature signature = signatureCtor.newInstance("f",
                    new String[]{"x", "c"}, leftExpression);
            Constructor<JvmExpressionFunction> fnCtor = JvmExpressionFunction.class
                    .getDeclaredConstructor(ParsedSignature.class, ManyToOneNumberFunction.class);
            fnCtor.setAccessible(true);
            JvmExpressionFunction fn = fnCtor.newInstance(signature, new ManyToOneNumberFunction("f") {
                @Override
                public double run(double... numbers) {
                    // x * c for "x*2", and -x - c for "-x-0.5".
                    if ("-x-0.5".equals(leftExpression)) {
                        return -numbers[1] - numbers[0];
                    }
                    return numbers[0] * numbers[1];
                }
            });
            Constructor<JvmExpression> exprCtor = JvmExpression.class
                    .getDeclaredConstructor(String.class, double[].class, JvmExpressionFunction.class, String.class);
            exprCtor.setAccessible(true);
            JvmExpression jvmExpression = exprCtor.newInstance("engEseJvm", initialValues.clone(), fn, leftExpression);
            HashSet<Integer> unknownIndexes = new HashSet<>(List.of(0));
            Object[] numbers = new Object[initialValues.length];
            for (int i = 0; i < initialValues.length; i++) {
                numbers[i] = initialValues[i];
            }
            numbers[0] = "x";
            Constructor<EquationSolverExpression> ctor = EquationSolverExpression.class.getDeclaredConstructor(
                    String.class, double.class, String.class, HashSet.class, JvmExpression.class, Object[].class);
            ctor.setAccessible(true);
            return ctor.newInstance(leftExpression, result, "engEse", unknownIndexes, jvmExpression, numbers);
        }

        /** Test hook exposing the protected interval auto-detection. */
        static final class LinearSolver extends EquationSolver {
            LinearSolver(String expression, double result, java.util.function.Function<double[], Double> fn) {
                super(expression, result, "engLinearSolver", new ManyToOneNumberFunction("engLinearSolver") {
                    @Override
                    public double run(double... numbers) {
                        return fn.apply(numbers);
                    }
                });
            }

            void detectInterval() {
                autoDetectInterval();
            }
        }
    }

    @Nested
    class ComplexNumberTests {

        @Test
        void complexExpressionSplitsRealAndImaginaryParts() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplex");
            ComplexExpression compiled = cc.compile("1+2i", true);
            CalculationComplexResults result = compiled.calculation(false);
            assertDecimalEquals("1", result.getResult());
            assertEquals("1.0 + 2.0i", result.toString());
            assertEquals(2, result.getResultLayers());
        }

        @Test
        void complexExpressionSupportsBracketedRealPart() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexBrackets");
            CalculationComplexResults result = cc.compile("(1+2)*2+1i", true).calculation(false);
            assertDecimalEquals("6", result.getResult());
        }

        @Test
        void complexExpressionSupportsNegativeImaginarySign() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexNegative");
            CalculationComplexResults result = cc.compile("5-3i", true).calculation(false);
            assertDecimalEquals("5", result.getResult());
        }

        @Test
        void complexCalculationBigDecimalPathMatches() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexBig");
            Mathematical_Expression.Options.setUseBigDecimal(true);
            try {
                assertDecimalEquals("1", cc.calculation("1+2i", true).getResult());
                assertDecimalEquals("6", cc.calculation("(1+2)*2+1i", true).getResult());
            } finally {
                Mathematical_Expression.Options.setUseBigDecimal(false);
            }
        }

        @Test
        void complexCheckRejectsMissingImaginaryPart() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexCheck");
            WrongFormat wrong = assertThrows(WrongFormat.class, () -> cc.check("2+3"));
            assertTrue(wrong.getMessage().contains("The imaginary part of the complex number is not found"));
            RuntimeException notComplex = assertThrows(RuntimeException.class, () -> cc.compile("2", true));
            assertTrue(notComplex.getMessage().contains("is not a complex expression"));
        }

        @Test
        void complexExplainProducesComplexResults() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexExplain");
            LogResults log = cc.explain("1+2i", true);
            assertInstanceOf(CalculationComplexResults.class, log.getResult());
            assertDecimalEquals("1", ((CalculationComplexResults) log.getResult()).getResult());
        }

        @Test
        void complexFormatStrRemovesSpaces() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexFormat");
            assertEquals("1+2i", cc.formatStr(" 1 + 2i "));
        }

        @Test
        void complexGetInstanceRejectsForeignComponent() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardComplex");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> ComplexCalculation.getInstance("engTypeGuardComplex"));
            assertTrue(ex.getMessage().contains("不属于 ComplexCalculation 类型"));
        }

        @Test
        void complexExpressionExposesBothParts() {
            ComplexCalculation cc = ComplexCalculation.getInstance("engComplexParts");
            ComplexExpression compiled = cc.compile("1+2i", true);
            assertNotNull(compiled.getFunctionExpression1());
            assertNotNull(compiled.getFunctionExpression2());
            assertTrue(compiled.isBigDecimal());
            assertTrue(compiled.isUnBigDecimal());
            assertDoesNotThrow(compiled::convertToMultiPrecisionSupported);
            assertDecimalEquals("1", compiled.calculationBigDecimals(false).getResult());
            assertDecimalEquals("1", compiled.calculation(true).getResult());
        }
    }

    @Nested
    class IntervalComponentTests {

        @Test
        void fastSumAddsEveryValueInRange() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSum");
            assertDecimalEquals("55", fs.calculation("1,10").getResult());
            assertDecimalEquals("55", fs.calculation("1,10", true).getResult());
        }

        @Test
        void fastSumSupportsStepTwoAfterCacheClear() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumStep");
            assertDecimalEquals("55", fs.calculation("1,10").getResult());
            fs.step = 2;
            fs.clearCache();
            assertDecimalEquals("25", fs.calculation("1,10", true).getResult());
        }

        @Test
        void fastSumWithEqualBoundsReturnsTheBound() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumEqual");
            assertDecimalEquals("5", fs.calculation("5,5").getResult());
        }

        @Test
        void fastSumCheckRejectsWrongPartCount() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumCheck");
            WrongFormat three = assertThrows(WrongFormat.class, () -> fs.check("1,10,20"));
            assertTrue(three.getMessage().contains("区间求和表达式解析错误"));
            assertTrue(three.getMessage().contains("Number of expressions you provide => 3"));
            WrongFormat one = assertThrows(WrongFormat.class, () -> fs.check("1"));
            assertTrue(one.getMessage().contains("Number of expressions you provide => 1"));
            assertDoesNotThrow(() -> fs.check("1,10"));
        }

        @Test
        void fastSumSharedPoolTracksOwnership() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumPool");
            assertFalse(fs.isCache("1,10"));
            assertDoesNotThrow(() -> fs.check("1,10"));
            assertTrue(fs.isCache("1,10"));
            assertTrue(fs.isStartSharedPool());
            fs.setStartSharedPool(false);
            assertFalse(fs.isStartSharedPool());
            assertTrue(fs.isCache("1,10"));
            fs.clearCache();
            assertFalse(fs.isCache("1,10"));
            assertDecimalEquals("55", fs.calculation("1,10", true).getResult());
        }

        @Test
        void fastSumCompilesToBracketExpression() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumCompile");
            PackExpression compiled = fs.compile("1,10", true);
            assertDecimalEquals("55", compiled.calculation(true).getResult());
            PackExpression compiledBig = fs.compileBigDecimal("1,10", true);
            assertDecimalEquals("55", compiledBig.calculationBigDecimals(true).getResult());
        }

        @Test
        void fastSumCalculatesFromPrecomputedResults() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumResults");
            CalculationNumberResults start = new CalculationNumberResults(1, 1, "a");
            CalculationNumberResults end = new CalculationNumberResults(1, 10, "b");
            CalculationNumberResults summed = fs.calculation(start, end);
            assertDecimalEquals("55", summed.getResult());
            assertEquals(2, summed.getResultLayers());
        }

        @Test
        void fastSumCalculatesFromEqualFormulas() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("engFastSumSame");
            assertDecimalEquals("10", fs.calculation("5+5", "5+5").getResult());
        }

        @Test
        void fastSumGetInstanceRejectsForeignComponent() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardFastSum");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> FastSumOfIntervalsBrackets.getInstance("engTypeGuardFastSum"));
            assertTrue(ex.getMessage().contains("不属于 FastSumOfIntervalsBrackets 类型"));
        }

        @Test
        void fastMultiplyMultipliesEveryValueInRange() {
            FastMultiplyOfIntervalsBrackets fm = FastMultiplyOfIntervalsBrackets.getInstance("engFastMul");
            assertEquals(2, fm.step);
            assertDecimalEquals("3840", fm.calculation("2,10").getResult());
            assertDecimalEquals("945", fm.calculation("1,9", true).getResult());
            assertDecimalEquals("5", fm.calculation("5,5", true).getResult());
        }

        @Test
        void fastMultiplyCompilesToBracketExpression() {
            FastMultiplyOfIntervalsBrackets fm = FastMultiplyOfIntervalsBrackets.getInstance("engFastMulCompile");
            PackExpression compiled = fm.compile("2,10", true);
            assertDecimalEquals("3840", compiled.calculation(true).getResult());
            PackExpression compiledBig = fm.compileBigDecimal("2,10", true);
            assertDecimalEquals("3840", compiledBig.calculationBigDecimals(true).getResult());
        }

        @Test
        void fastMultiplyCalculatesFromPrecomputedResults() {
            FastMultiplyOfIntervalsBrackets fm = FastMultiplyOfIntervalsBrackets.getInstance("engFastMulResults");
            CalculationNumberResults start = new CalculationNumberResults(1, 2, "a");
            CalculationNumberResults end = new CalculationNumberResults(1, 10, "b");
            assertDecimalEquals("3840", fm.calculation(start, end).getResult());
        }

        @Test
        void fastMultiplyGetInstanceRejectsForeignComponent() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("engTypeGuardFastMul");
            assertNotNull(foreign);
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> FastMultiplyOfIntervalsBrackets.getInstance("engTypeGuardFastMul"));
            assertTrue(ex.getMessage().contains("不属于 FastMultiplyOfIntervalsBrackets 类型"));
        }
    }

    @Nested
    class EngineUtilityTests {

        @Test
        void numberUtilsFactorialFollowsLibrarySemantics() {
            assertEquals(120.0, NumberUtils.factorial(5));
            assertEquals(1.0, NumberUtils.factorial(1));
            assertEquals(0.0, NumberUtils.factorial(0));
            assertEquals(2.0, NumberUtils.factorial(2));
        }

        @Test
        void numberUtilsCalculatesEveryOperator() {
            assertEquals(5.0, NumberUtils.calculation('+', 2, 3));
            assertEquals(-1.0, NumberUtils.calculation('-', 2, 3));
            assertEquals(6.0, NumberUtils.calculation('*', 2, 3));
            assertEquals(3.5, NumberUtils.calculation('/', 7, 2));
            assertEquals(1.0, NumberUtils.calculation('%', 7, 2));
            assertEquals(1024.0, NumberUtils.calculation('^', 2, 10));
            net.mooctest.expression.engine.AbnormalOperation bad = assertThrows(
                    net.mooctest.expression.engine.AbnormalOperation.class,
                    () -> NumberUtils.calculation('?', 2, 3));
            assertTrue(bad.getMessage().contains("错误的计算模式 = [?]"));
        }

        @Test
        void numberUtilsPriorityComparisonOnlyDemotesPlusAndMinus() {
            assertTrue(NumberUtils.PriorityComparison('+', '*'));
            assertTrue(NumberUtils.PriorityComparison('-', '/'));
            assertTrue(NumberUtils.PriorityComparison('+', '%'));
            assertTrue(NumberUtils.PriorityComparison('-', '^'));
            assertFalse(NumberUtils.PriorityComparison('*', '+'));
            assertFalse(NumberUtils.PriorityComparison('+', '+'));
        }

        @Test
        void numberUtilsComparisonOperationCoversEveryOperator() {
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.LESS_THAN_SIGN, 1, 2));
            assertFalse(NumberUtils.ComparisonOperation(ConstantRegion.LESS_THAN_SIGN, 2, 1));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.GREATER_THAN_SIGN, 2, 1));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.LESS_THAN_OR_EQUAL_TO_SIGN, 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.GREATER_THAN_OR_EQUAL_TO_SIGN, 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.EQUAL_SIGN1, 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.EQUAL_SIGN2, 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.NOT_EQUAL_SIGN1, 1, 2));
            assertTrue(NumberUtils.ComparisonOperation(ConstantRegion.NOT_EQUAL_SIGN2, 1, 2));
            assertFalse(NumberUtils.ComparisonOperation(ConstantRegion.NOT_EQUAL_SIGN2, 2, 2));
            net.mooctest.expression.engine.AbnormalOperation bad = assertThrows(
                    net.mooctest.expression.engine.AbnormalOperation.class,
                    () -> NumberUtils.ComparisonOperation("~", 1, 2));
            assertTrue(bad.getMessage().contains("Bad comparison operator => ~"));
        }

        @Test
        void numberUtilsSumOfRangeHandlesBoundsAndDirection() {
            assertEquals(55.0, NumberUtils.sumOfRange(1.0, 10.0));
            assertEquals(5.0, NumberUtils.sumOfRange(5.0, 5.0));
            assertEquals(55.0, NumberUtils.sumOfRange(10.0, 1.0));
            // The deprecated integer variant has no absolute-value guard: a reversed
            // range produces the negative algebraic series value.
            assertEquals(-44, NumberUtils.sumOfRange(10, 1));
            assertEquals(55, NumberUtils.sumOfRange(1, 10));
            assertEquals(5, NumberUtils.sumOfRange(5, 5));
        }

        @Test
        void numberUtilsSteppedSumMatchesSeriesValues() {
            assertEquals(55.0, NumberUtils.sumOfRange(1, 10, 1));
            assertEquals(25.0, NumberUtils.sumOfRange(1, 10, 2));
            assertEquals(25.0, NumberUtils.sumOfRange(1, 9, 2));
            assertEquals(22.0, NumberUtils.sumOfRange(1, 10, 3));
            assertEquals(15.0, NumberUtils.sumOfRange(1, 10, 4));
            assertEquals(5.0, NumberUtils.sumOfRange(5, 5, 3));
        }

        @Test
        void numberUtilsRangeStringDocumentsTheSeries() {
            assertEquals("(1.0+10.0)*10.0/2", NumberUtils.sumOfRangeString(1, 10));
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5));
            assertEquals("5.0*1.0+5.0*(5.0-1)*2.0/2", NumberUtils.sumOfRangeString(1, 10, 2));
            assertEquals("(1.0+10.0)*10.0/2", NumberUtils.sumOfRangeString(1, 10, 1));
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5, 3));
        }

        @Test
        void numberUtilsMultiplyOfRangeHandlesStepsAndBounds() {
            assertEquals(3840.0, NumberUtils.MultiplyOfRange(2, 10, 2));
            assertEquals(945.0, NumberUtils.MultiplyOfRange(1, 9, 2));
            assertEquals(45.0, NumberUtils.MultiplyOfRange(1, 10, 4));
            assertEquals(5.0, NumberUtils.MultiplyOfRange(5, 5, 2));
        }

        @Test
        void numberUtilsMultiplyOfRangeStringDocumentsTheProduct() {
            assertEquals("3840.0", NumberUtils.MultiplyOfRangeString(2, 10, 2));
            assertEquals("5.0", NumberUtils.MultiplyOfRangeString(5, 5, 2));
        }

        @Test
        void strUtilsNumberConversionsCoverFactorialAndCache() {
            assertEquals(12.5, StrUtils.stringToDouble("12.5"));
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
            UnsupportedOperationException negative = assertThrows(UnsupportedOperationException.class,
                    () -> StrUtils.stringToDouble("-3!"));
            assertTrue(negative.getMessage().contains("负数不支持计算阶乘:-3.0"));

            Mathematical_Expression.Options.setUseCache(true);
            try {
                Mathematical_Expression.Options.cacheCalculation("7.25".hashCode(), 99.0);
                assertEquals(99.0, StrUtils.stringToDouble("7.25"));
                Mathematical_Expression.Options.cacheCalculation("8.5".hashCode(), new BigDecimal("88.5"));
                assertEquals(0, new BigDecimal("88.5").compareTo(StrUtils.stringToBigDecimal("8.5")));
            } finally {
                Mathematical_Expression.Options.setUseCache(false);
            }
            assertEquals(0, new BigDecimal("7.25").compareTo(StrUtils.stringToBigDecimal("7.25")));
            assertEquals(24.0, StrUtils.stringToBigDecimal("4!").doubleValue());
            UnsupportedOperationException bigNegative = assertThrows(UnsupportedOperationException.class,
                    () -> StrUtils.stringToBigDecimal("-2!"));
            assertTrue(bigNegative.getMessage().contains("负数不支持计算阶乘:-2"));
        }

        @Test
        void strUtilsCharacterClassification() {
            assertTrue(StrUtils.IsAnOperator('+'));
            assertTrue(StrUtils.IsAnOperator('-'));
            assertTrue(StrUtils.IsAnOperator('*'));
            assertTrue(StrUtils.IsAnOperator('/'));
            assertTrue(StrUtils.IsAnOperator('%'));
            assertTrue(StrUtils.IsAnOperator('^'));
            assertFalse(StrUtils.IsAnOperator('a'));
            assertTrue(StrUtils.IsANumber("123"));
            assertFalse(StrUtils.IsANumber("12a"));
            assertTrue(StrUtils.IsANumber('7'));
            assertFalse(StrUtils.IsANumber('x'));
            assertTrue(StrUtils.containsNumber("a1b"));
            assertFalse(StrUtils.containsNumber("abc"));
            assertEquals('a', StrUtils.containsEnChar("123a456"));
            assertEquals('Z', StrUtils.containsEnChar("Z"));
            assertNull(StrUtils.containsEnChar("123"));
        }

        @Test
        void strUtilsSplitsWithAndWithoutBracketAwareness() {
            assertEquals(List.of("a", "b", "c"), StrUtils.splitByChar("a,b,c", ','));
            assertEquals(List.of("a", "", "b"), StrUtils.splitByChar("a,,b", ','));
            assertEquals(List.of("x", "y"), StrUtils.splitByChar(" x , y ", ','));
            assertEquals(List.of("sum(1,2)", "3"), StrUtils.splitByCharWhereNoB("sum(1,2),3", ','));
            assertEquals(List.of("(a,b)", "c"), StrUtils.splitByCharWhereNoB("(a,b),c", ','));
            assertEquals(List.of("1+2"), StrUtils.splitByCharWhereNoB("1+2", ','));
        }

        @Test
        void strUtilsRemovesEmptyCharacters() {
            assertEquals("1+2", StrUtils.removeEmpty(" 1 + 2 "));
            assertEquals("", StrUtils.removeEmpty("   "));
        }

        @Test
        void strUtilsRandomStringUsesLowercaseAlphabet() {
            for (int length : new int[]{0, 1, 32}) {
                String random = StrUtils.randomString(length);
                assertEquals(length, random.length());
                for (char c : random.toCharArray()) {
                    assertTrue(c >= 'a' && c <= 'z', "unexpected char " + c);
                }
            }
        }
    }
}
