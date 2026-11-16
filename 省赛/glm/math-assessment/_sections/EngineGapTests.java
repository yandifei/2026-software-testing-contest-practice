package net.mooctest;

import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.ComplexCalculation;
import net.mooctest.expression.engine.ComplexExpression;
import net.mooctest.expression.engine.DynamicFunctionCompiler;
import net.mooctest.expression.engine.EquationSolver;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.ExpressionFunction;
import net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets;
import net.mooctest.expression.engine.FastSumOfIntervalsBrackets;
import net.mooctest.expression.engine.FunctionExpression;
import net.mooctest.expression.engine.FunctionFormulaCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation2;
import net.mooctest.expression.engine.FunctionPackage;
import net.mooctest.expression.engine.InMemoryJavaFileObject;
import net.mooctest.expression.engine.JvmCalculation;
import net.mooctest.expression.engine.JvmExpressionFunction;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.ManyToOneNumberFunction;
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
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Second-pass engine coverage: targets the branch sides the first pass missed
 * (instance re-use, uppercase function letters, nested extractions, cached
 * lookups, availability consumption and registry guard arms).
 */
class EngineGapTests {

    @BeforeEach
    void resetFunctionRegistries() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        try {
            Field calculations = CalculationManagement.class.getDeclaredField("STRING_CALCULATION_HASH_MAP");
            calculations.setAccessible(true);
            ((java.util.Map<?, ?>) calculations.get(null)).clear();
            Field functions = CalculationManagement.class.getDeclaredField("STRING_FUNCTION_HASH_MAP");
            functions.setAccessible(true);
            ((java.util.Map<?, ?>) functions.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot reset component registries", e);
        }
    }

    @AfterEach
    void restoreEngineFlags() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
    }

    private static void makeUnavailable(Object expression) {
        try {
            Field available = Class.forName(expression.getClass().getSuperclass().getName()
                    .equals("net.mooctest.expression.engine.SubCompileExpression")
                    ? "net.mooctest.expression.engine.NameExpression"
                    : expression.getClass().getSuperclass().getName())
                    .getDeclaredField("available");
            available.setAccessible(true);
            available.set(expression, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot close availability", e);
        }
    }

    @Nested
    class InstanceRegistryGapTests {

        @Test
        void doubleGetInstanceReturnsSameComponent() {
            assertSame(SingletonEquationSolving.getInstance("gapSes"),
                    SingletonEquationSolving.getInstance("gapSes"));
            assertSame(SingletonEquationSolvingTwo.getInstance("gapSes2"),
                    SingletonEquationSolvingTwo.getInstance("gapSes2"));
            assertSame(FastMultiplyOfIntervalsBrackets.getInstance("gapFm"),
                    FastMultiplyOfIntervalsBrackets.getInstance("gapFm"));
            assertSame(FastSumOfIntervalsBrackets.getInstance("gapFs"),
                    FastSumOfIntervalsBrackets.getInstance("gapFs"));
            assertSame(JvmCalculation.getInstance("gapJvm"),
                    JvmCalculation.getInstance("gapJvm"));
            assertSame(ComplexCalculation.getInstance("gapCc"),
                    ComplexCalculation.getInstance("gapCc"));
            assertSame(FunctionFormulaCalculation.getInstance("gapFfc1"),
                    FunctionFormulaCalculation.getInstance("gapFfc1"));
        }

        @Test
        void singletonEquationSolversRejectForeignComponents() {
            BracketsCalculation2 foreign = BracketsCalculation2.getInstance("gapGuardSes");
            assertNotNull(foreign);
            assertTrue(assertThrows(ExtractException.class,
                    () -> SingletonEquationSolving.getInstance("gapGuardSes"))
                    .getMessage().contains("DynamicEquationSolver"));
            assertTrue(assertThrows(ExtractException.class,
                    () -> SingletonEquationSolvingTwo.getInstance("gapGuardSes"))
                    .getMessage().contains("DynamicEquationSolver"));
        }

        @Test
        void jvmFunctionParseFromExpressionFunctionUsesSignatureToString() throws Exception {
            ExpressionFunction ef = ExpressionFunction.parse("gapP(x) = x * 2");
            RuntimeException ex = assertThrows(RuntimeException.class, () -> JvmExpressionFunction.parse(ef));
            assertEquals("动态函数生成失败", ex.getMessage());
        }

        @Test
        void emptyParameterListIsAcceptedBySignatureParser() {
            // f() = 1+1 parses (zero parameters) and only fails at dynamic compilation.
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> JvmExpressionFunction.parse("gapNoParams() = 1+1"));
            assertEquals("动态函数生成失败", ex.getMessage());
        }

        @Test
        void compilerIteratesRegisteredJvmMethodsWhenGeneratingSources() {
            JvmExpressionFunction.registerFunction("gapIterFn(x) = x + 1");
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> DynamicFunctionCompiler.compile("gapIter", "x*2", new String[]{"x"}));
            assertEquals("动态函数生成失败", ex.getMessage());
        }

        @Test
        void moduleLengthReportsMissingSqrtWhenPowExists() {
            CalculationManagement.register(new ManyToOneNumberFunction("pow") {
                @Override
                public double run(double... numbers) {
                    // Tolerate the single-argument call style moduleLength uses.
                    return numbers.length > 1
                            ? Math.pow(numbers[0], numbers[1])
                            : numbers[0] * numbers[0];
                }
            });
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> ((ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck("moduleLength")).run(3, 4));
            assertTrue(ex.getMessage().contains("sqrt"));
        }
    }

    @Nested
    class FunctionFormulaGapTests {

        @Test
        void extractionAcceptsUppercaseLettersAndNestedCalls() {
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction(
                    "SUM(1,pow(2,3))+x", start, end, names);
            assertEquals(1, names.size());
            assertEquals("SUM", names.pop());
            assertFalse(start.isEmpty());
            assertFalse(end.isEmpty());
        }

        @Test
        void extractionWithBuilderHandlesUppercaseAndNesting() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2Builder");
            StringBuilder builder = new StringBuilder();
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            c.FunctionParameterExtraction("SUM(1,2)+1", start, end, names, builder);
            assertEquals("0+1", builder.toString());
            assertEquals("SUM", names.pop());
        }

        @Test
        void checkIsIdempotentAndSkipsWorkWhenCached() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2CheckTwice");
            assertDoesNotThrow(() -> c.check("sum(1,2)+1"));
            assertDoesNotThrow(() -> c.check("sum(1,2)+1"));
            assertTrue(c.isCache("sum(1,2)+1"));
        }

        @Test
        void checkWithoutSharedPoolStillValidates() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2CheckNoPool");
            c.setStartSharedPool(false);
            assertDoesNotThrow(() -> c.check("sum(1,2)+1"));
            assertFalse(c.isCache("sum(1,2)+1"));
            WrongFormat wrong = assertThrows(WrongFormat.class,
                    () -> c.check("sum(1,2"));
            assertTrue(wrong.getMessage().contains("Missing function bracket logarithm"));
        }

        @Test
        void calculationSupportsBigDecimalMode() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2Big");
            Mathematical_Expression.Options.setUseBigDecimal(true);
            try {
                assertEquals(0, new java.math.BigDecimal("7").compareTo(
                        new java.math.BigDecimal(String.valueOf(c.calculation("1+sum(1,2,3)", true).getResult()))));
            } finally {
                Mathematical_Expression.Options.setUseBigDecimal(false);
            }
        }

        @Test
        void explainReusesLogEntryForRepeatedFunctionNames() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2ExplainTwice");
            net.mooctest.expression.engine.LogResults log = c.explain("sum(1,2)+sum(3,4)", true);
            assertEquals(0, new java.math.BigDecimal("10").compareTo(
                    new java.math.BigDecimal(String.valueOf(log.getResult()))));
        }

        @Test
        void explainAttachesNestedNonNumberArgumentsToTheLog() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFfc2ExplainNested");
            net.mooctest.expression.engine.LogResults log = c.explain("sum(1+1,2)", true);
            assertEquals(0, new java.math.BigDecimal("4").compareTo(
                    new java.math.BigDecimal(String.valueOf(log.getResult()))));
        }

        @Test
        void singleParameterCalculationHandlesUppercaseUnknownAndNonIntegerResults() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gapFfc1Calc");
            // Function names are case sensitive: the uppercase spelling is unknown.
            ExtractException upper = assertThrows(ExtractException.class,
                    () -> c.calculation("1+COS(0)", true));
            assertTrue(upper.getMessage().contains("COS"));
            // Non-integer function results are appended with full double precision.
            assertEquals(0, new java.math.BigDecimal("1.4142135623730951").compareTo(
                    new java.math.BigDecimal(String.valueOf(c.calculation("sqrt(2)", true).getResult()))));
            // Whitespace outside the function body is dropped by the inliner.
            assertEquals(0, new java.math.BigDecimal("2").compareTo(
                    new java.math.BigDecimal(String.valueOf(c.calculation("1 + cos(0)", true).getResult()))));
        }

        @Test
        void singleParameterCheckValidatesWellFormedFormulas() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gapFfc1CheckOk");
            // The single-parameter check appends its "0" placeholder at the END of
            // the buffer, so a function followed by more terms leaves a leading "+"
            // and is rejected — a real quirk of the shipped implementation.
            WrongFormat trailing = assertThrows(WrongFormat.class, () -> c.check("sin(1)+1"));
            assertTrue(trailing.getMessage().contains("空字符串"));
            // The +2 offset in the inner-formula slice empties the checked string
            // for any function call, so even a lone well-formed call is rejected.
            WrongFormat lone = assertThrows(WrongFormat.class, () -> c.check("cos(2)"));
            assertTrue(lone.getMessage().contains("空字符串"));
            assertDoesNotThrow(() -> c.check("1+2"));
            WrongFormat wrong = assertThrows(WrongFormat.class, () -> c.check("sin(1)+cos(2"));
            assertTrue(wrong.getMessage().contains("Missing function bracket logarithm: 1"));
        }
    }

    @Nested
    class FunctionExpressionGapTests {

        @Test
        void staticSubCompileShortCircuitsNumbersAndEmptyFunctions() {
            StringBuilder numeric = FunctionExpression.subCompile(
                    "123", new Stack<>(), new Stack<>(), new Stack<>());
            assertEquals("123", numeric.toString());
            StringBuilder noFunctions = FunctionExpression.subCompile(
                    "1+2", new Stack<>(), new Stack<>(), new Stack<>());
            assertEquals("1+2", noFunctions.toString());
        }

        @Test
        void subCompileBigDecimalsRejectsAlreadyFlatExpressions() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFeBig");
            FunctionExpression fe = c.compile("1+2", true);
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> fe.subCompileBigDecimals(true));
            assertTrue(ex.getMessage().contains("此表达式不需要深度编译了"));
        }

        @Test
        void calculationBigDecimalsUsesBracketPathWhenNoFunctionsRemain() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFeFlat");
            FunctionExpression fe = c.compile("1+2", true);
            assertEquals(0, new java.math.BigDecimal("3").compareTo(
                    new java.math.BigDecimal(String.valueOf(fe.calculationBigDecimals(true).getResult()))));
        }

        @Test
        void closedExpressionsRejectEveryDeepCompileAccessor() throws Exception {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFeClosed");
            FunctionExpression fe = c.compile("sum(1,2)+pow(2,3)", true);
            makeUnavailable(fe);
            assertThrows(UnsupportedOperationException.class, () -> fe.subCompile(true));
            assertThrows(UnsupportedOperationException.class, () -> fe.subCompileBigDecimals(true));
            assertThrows(UnsupportedOperationException.class, fe::getFunNames);
        }

        @Test
        void nonCopyConsumptionClosesTheExpressionForFutureCompiles() throws Exception {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gapFeConsume");
            FunctionExpression fe = c.compile("sum(1,2)+pow(2,3)", true);
            fe.calculationCache(false);
            Field available = Class.forName("net.mooctest.expression.engine.NameExpression")
                    .getDeclaredField("available");
            available.setAccessible(true);
            assertFalse((Boolean) available.get(fe));
        }
    }

    @Nested
    class ComplexAndIntervalGapTests {

        @Test
        void complexBigDecimalCompilationAndExplainErrorPath() {
            ComplexCalculation cc = ComplexCalculation.getInstance("gapCcPaths");
            ComplexExpression compiled = cc.compileBigDecimal("1+2i", true);
            assertEquals(0, new java.math.BigDecimal("1").compareTo(
                    new java.math.BigDecimal(String.valueOf(compiled.calculationBigDecimals(false).getResult()))));
            ComplexExpression raw = cc.compileBigDecimal(" 1 + 2i ", false);
            assertNotNull(raw.getFunctionExpression2());
            UnsupportedOperationException explain = assertThrows(UnsupportedOperationException.class,
                    () -> cc.explain("2+3", true));
            assertTrue(explain.getMessage().contains("The imaginary part of the complex number is not found"));
        }

        @Test
        void fastSumCheckShortCircuitsOnRepeatedOwner() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("gapFsCheck");
            assertDoesNotThrow(() -> fs.check("1,10"));
            assertDoesNotThrow(() -> fs.check("1,10"));
            assertTrue(fs.isCache("1,10"));
        }

        @Test
        void fastSumCheckSkipsRightValidationWhenBoundsEqual() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("gapFsEqual");
            assertDoesNotThrow(() -> fs.check("5+5,5+5"));
        }

        @Test
        void fastSumCalculationReusesSharedResultInstance() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("gapFsShare");
            assertDoesNotThrow(() -> fs.check("1,10"));
            net.mooctest.expression.engine.CalculationNumberResults first = fs.calculation("1,10", true);
            assertEquals(0, new java.math.BigDecimal("55").compareTo(
                    new java.math.BigDecimal(String.valueOf(first.getResult()))));
            assertSame(first, fs.calculation("1,10", true));
        }

        @Test
        void fastSumCalculationWithoutSharedPoolSplitsManually() {
            FastSumOfIntervalsBrackets fs = FastSumOfIntervalsBrackets.getInstance("gapFsNoPool");
            fs.setStartSharedPool(false);
            assertEquals(0, new java.math.BigDecimal("55").compareTo(
                    new java.math.BigDecimal(String.valueOf(fs.calculation("1,10", true).getResult()))));
        }

        @Test
        void equationSolverBisectionExitsByIterationLimitAndUpperHalf() {
            EquationSolverProbed solver = new EquationSolverProbed("2*x", 10, numbers -> numbers[0] * 2);
            solver.setBisectionLeft(0);
            solver.setBisectionRight(8);
            double[] solution = solver.solveBisection(1e-4, 3);
            assertEquals(5.0, solution[0], 0.01);
            assertEquals(3, (int) solution[1]);
        }

        @Test
        void equationSolverCalculationRespectsBisectionSwitch() {
            EquationSolverProbed solver = new EquationSolverProbed("2*x", 10, numbers -> numbers[0] * 2);
            solver.setUseNewton(false);
            solver.setBisectionLeft(0);
            solver.setBisectionRight(10);
            assertEquals(0, new java.math.BigDecimal("5").compareTo(
                    new java.math.BigDecimal(String.valueOf(solver.calculation(false).getResult()))));
        }

        @Test
        void equationSolverExpressionSetKnownNumberUpdatesConstants() throws Exception {
            java.lang.reflect.Constructor<net.mooctest.expression.engine.ParsedSignature> signatureCtor =
                    net.mooctest.expression.engine.ParsedSignature.class
                            .getDeclaredConstructor(String.class, String[].class, String.class);
            signatureCtor.setAccessible(true);
            net.mooctest.expression.engine.ParsedSignature signature =
                    signatureCtor.newInstance("f", new String[]{"x", "c"}, "x*c");
            java.lang.reflect.Constructor<JvmExpressionFunction> fnCtor = JvmExpressionFunction.class
                    .getDeclaredConstructor(net.mooctest.expression.engine.ParsedSignature.class, ManyToOneNumberFunction.class);
            fnCtor.setAccessible(true);
            JvmExpressionFunction fn = fnCtor.newInstance(signature, new ManyToOneNumberFunction("f") {
                @Override
                public double run(double... numbers) {
                    return numbers[0] * numbers[1];
                }
            });
            java.lang.reflect.Constructor<net.mooctest.expression.engine.JvmExpression> exprCtor =
                    net.mooctest.expression.engine.JvmExpression.class
                            .getDeclaredConstructor(String.class, double[].class, JvmExpressionFunction.class, String.class);
            exprCtor.setAccessible(true);
            net.mooctest.expression.engine.JvmExpression jvmExpression =
                    exprCtor.newInstance("gapEseJvm", new double[]{0.0, 2.0}, fn, "x*c");
            java.lang.reflect.Constructor<net.mooctest.expression.engine.EquationSolverExpression> ctor =
                    net.mooctest.expression.engine.EquationSolverExpression.class.getDeclaredConstructor(
                            String.class, double.class, String.class, java.util.HashSet.class,
                            net.mooctest.expression.engine.JvmExpression.class, Object[].class);
            ctor.setAccessible(true);
            net.mooctest.expression.engine.EquationSolverExpression solver = ctor.newInstance(
                    "x*c", 10.0, "gapEse", new java.util.HashSet<>(java.util.List.of(0)),
                    jvmExpression, new Object[]{"x", 2.0});
            solver.setKnownNumber(1, 3);
            assertEquals("x*3.0 = 10.0", solver.explain());
        }

        @Test
        void saveFunctionSkipsFunctionsThatDisallowSerialization() throws Exception {
            ManyToOneNumberFunction restricted = new ManyToOneNumberFunction("gapNoSerial") {
                @Override
                public double run(double... numbers) {
                    return 1;
                }

                @Override
                public boolean AllowSerialization() {
                    return false;
                }
            };
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Mathematical_Expression.saveFunction(buffer, restricted);
            // A fresh ObjectOutputStream always emits its 4-byte stream header even
            // when no object is written, so "skipped" means header-only output.
            assertEquals(4, buffer.size());
        }

        @Test
        void streamRegistrationCountsNonFunctionObjectsAsErrors() throws Exception {
            ExpressionFunction fn = ExpressionFunction.parse("gapStreamFn(x) = x + 2");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                out.writeObject("not a function");
                out.writeObject(fn);
            }
            try (ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray())) {
                java.util.Map.Entry<Integer, Integer> result = Mathematical_Expression.register_function(in);
                assertEquals(1, result.getKey());
                assertEquals(1, result.getValue());
            }
        }

        @Test
        void manyToOneFunctionToStringExposesName() {
            ManyToOneNumberFunction fn = new ManyToOneNumberFunction("gapToString") {
                @Override
                public double run(double... numbers) {
                    return 0;
                }
            };
            assertEquals("gapToString", fn.toString());
            assertEquals("gapToString", fn.getName());
        }

        @Test
        void containsEnCharRejectsBoundaryNonLetters() {
            assertNull(StrUtils.containsEnChar("["));
            assertNull(StrUtils.containsEnChar("`"));
            assertNull(StrUtils.containsEnChar("{"));
            assertNull(StrUtils.containsEnChar("@"));
            assertEquals('A', StrUtils.containsEnChar("[A]"));
            assertEquals('z', StrUtils.containsEnChar("`z{"));
        }
    }

    /** Test hook naming the protected solver constructor. */
    static final class EquationSolverProbed extends EquationSolver {
        EquationSolverProbed(String expression, double result, java.util.function.Function<double[], Double> fn) {
            super(expression, result, "gapSolver", new ManyToOneNumberFunction("gapSolver") {
                @Override
                public double run(double... numbers) {
                    return fn.apply(numbers);
                }
            });
        }
    }
}
