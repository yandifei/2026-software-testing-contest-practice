package net.mooctest;

import net.mooctest.expression.engine.ComplexCalculation;
import net.mooctest.expression.engine.ComplexExpression;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.FunctionExpression;
import net.mooctest.expression.engine.FunctionFormulaCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation2;
import net.mooctest.expression.engine.FunctionPackage;
import net.mooctest.expression.engine.JvmExpressionFunction;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.ManyToOneNumberFunction;
import net.mooctest.expression.engine.WrongFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fourth engine pass, with dead-branch documentation.
 *
 * DEAD branches (verified unreachable, no test possible through any access
 * level available to this suite):
 * - ExpressionFunction L131 `!IsANumber(group)` false side: the matcher only
 *   yields pure-letter groups, which are never numbers.
 * - ExpressionFunction L209 `expression.size() > index` false side: the final
 *   unconditional add makes size always index+1.
 * - FunctionFormulaCalculation2 L210 `size1 == names.size()` false side and
 *   L216 loop-exit-with-unequal-stacks: start/end/name stacks only diverge
 *   when the size guard already threw.
 * - EquationSolverExpression L89/L93 loops: EquationSolverExpression.compile
 *   always fails at JvmExpressionFunction.parse before the loops run; the
 *   reflective construction used elsewhere bypasses compile entirely.
 * - JvmExpressionFunction L180/L217 `replacement != null` false side: the
 *   matcher only matches registered parameter names, always present in the map.
 * - DynamicFunctionCompiler L85 empty-diagnostics side: the generated source
 *   imports the missing mathematicalExpression package, so diagnostics are
 *   never empty in this environment.
 */
class EngineGapTests3 {

    @BeforeEach
    void resetFunctionRegistries() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        try {
            Field calculations = net.mooctest.expression.engine.CalculationManagement.class
                    .getDeclaredField("STRING_CALCULATION_HASH_MAP");
            calculations.setAccessible(true);
            ((java.util.Map<?, ?>) calculations.get(null)).clear();
            Field functions = net.mooctest.expression.engine.CalculationManagement.class
                    .getDeclaredField("STRING_FUNCTION_HASH_MAP");
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

    @Nested
    class NameAndFunctionGaps {

        @Test
        void functionConstructorRejectsNamesWithNonLetters() {
            ExtractException ex = assertThrows(ExtractException.class,
                    () -> new ManyToOneNumberFunction("f2x") {
                        @Override
                        public double run(double... numbers) {
                            return 0;
                        }
                    });
            assertTrue(ex.getMessage().contains("ERROR NAME => f2x"));
        }

        @Test
        void moduleLengthWithNoArgumentsReturnsZero() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            ManyToOneNumberFunction moduleLength =
                    (ManyToOneNumberFunction) FunctionPackage.MATH.getFunctionByNameNoCheck("moduleLength");
            assertEquals(0.0, moduleLength.run());
        }

        @Test
        void staticMethodSourceSupportsMultipleParameters() {
            assertEquals("public static double f(double x, double y) { return x + y; }",
                    JvmExpressionFunction.convertToStaticMethodSource("f(x, y) = x + y"));
        }

        @Test
        void signatureParserRejectsMissingCloseParen() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> JvmExpressionFunction.parse("f(x = 1"));
            assertTrue(ex.getMessage().contains("函数定义需要包含参数列表"));
        }
    }

    @Nested
    class ComplexGaps {

        @Test
        void checkRejectsEmptyAndImaginaryOnlyExpressions() {
            ComplexCalculation cc = ComplexCalculation.getInstance("gap3Cc");
            RuntimeException empty = assertThrows(RuntimeException.class, () -> cc.check(""));
            assertTrue(empty.getMessage().contains("is not a complex expression"));
            // An imaginary-only tail leaves the bracket calculator with a bare "+"
            WrongFormat imaginary = assertThrows(WrongFormat.class, () -> cc.check("2+i"));
            assertTrue(imaginary.getMessage().contains("ERROR => + in +"));
        }

        @Test
        void checkTreatsSignsInsideBracketsAsOrdinaryCharacters() {
            ComplexCalculation cc = ComplexCalculation.getInstance("gap3CcBrackets");
            RuntimeException notComplex = assertThrows(RuntimeException.class, () -> cc.check("(1+2i)"));
            assertTrue(notComplex.getMessage().contains("(1+2i) is not a complex expression"));
        }

        @Test
        void bigDecimalPathNegatesImaginaryPartForMinusSign() {
            ComplexCalculation cc = ComplexCalculation.getInstance("gap3CcBig");
            ComplexExpression compiled = cc.compileBigDecimal("5-3i", true);
            assertEquals(0, new java.math.BigDecimal("5").compareTo(
                    new java.math.BigDecimal(String.valueOf(compiled.calculationBigDecimals(false).getResult()))));
        }
    }

    @Nested
    class FormulaGaps {

        @Test
        void builderExtractionCountsNestedFunctionsOnce() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gap3Builder");
            StringBuilder builder = new StringBuilder();
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            c.FunctionParameterExtraction("SUM(1,pow(2,3))+1", start, end, names, builder);
            assertEquals(1, names.size());
            assertEquals("SUM", names.pop());
            assertEquals("0+1", builder.toString());
        }

        @Test
        void singleParameterCheckCoversNestedClosedAndSpacedCalls() {
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gap3Ffc1");
            WrongFormat nested = assertThrows(WrongFormat.class, () -> c.check("sin(cos(1))"));
            assertTrue(nested.getMessage().contains("Wrong from"));
            // Inconsistent with "cos(2)" (rejected as empty): the space inside the
            // call widens the inner slice to "1 " which validates cleanly.
            assertDoesNotThrow(() -> c.check("sin( 1 )"));
            assertThrows(WrongFormat.class, () -> c.check("s|n(1"));
            assertDoesNotThrow(() -> c.check("1+2"));
        }

        @Test
        void fastSumStoresAndReusesSharedResult() throws WrongFormat {
            net.mooctest.expression.engine.FastSumOfIntervalsBrackets fs =
                    net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance("gap3Fs");
            fs.check("1,10");
            net.mooctest.expression.engine.CalculationNumberResults first = fs.calculation("1,10", true);
            assertEquals(0, new java.math.BigDecimal("55").compareTo(
                    new java.math.BigDecimal(String.valueOf(first.getResult()))));
            assertSame(first, fs.calculation("1,10", true));
        }

        @Test
        void bisectionReturnsLeftBoundWhenWindowNarrowerThanTolerance() {
            BisectionProbed solver = new BisectionProbed("2*x", 10);
            solver.setBisectionLeft(4.999999);
            solver.setBisectionRight(5.000001);
            double[] solution = solver.solveBisection(1e-3, 100);
            assertEquals(4.999999, solution[0], 1e-12);
            assertEquals(100, (int) solution[1]);
        }
    }

    @Nested
    class ClosedExpressionGaps {

        @Test
        void closedExpressionRejectsEndAndNameAccessorsDirectly() throws Exception {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gap3Closed");
            FunctionExpression fe = c.compile("sum(1,2)+pow(2,3)", true);
            ClosedProbe probe = new ClosedProbe(fe.getExpressionStr(), fe.getCalculationName());
            // Capture fresh stack clones while the expression is still open.
            probe.capture();
            Field available = Class.forName("net.mooctest.expression.engine.NameExpression")
                    .getDeclaredField("available");
            available.setAccessible(true);
            available.set(probe, false);
            assertThrows(UnsupportedOperationException.class, probe::touchStart);
            assertThrows(UnsupportedOperationException.class, probe::touchEnd);
            assertThrows(UnsupportedOperationException.class, probe::touchNames);
        }

        /** Subclass hook reaching the protected stack accessors directly. */
        static final class ClosedProbe extends FunctionExpression {
            ClosedProbe(String expression, String calculationName) {
                super(expression, calculationName, new Stack<>(), new Stack<>(), new Stack<>());
            }

            void capture() {
                getStart(true);
                getEnd(true);
                getFunNames(true);
            }

            void touchStart() {
                getStart(true);
            }

            void touchEnd() {
                getEnd(true);
            }

            void touchNames() {
                getFunNames(true);
            }
        }
    }

    /** Solver hook with a letters-only component name. */
    static class BisectionProbed extends net.mooctest.expression.engine.EquationSolver {
        BisectionProbed(String expression, double result) {
            super(expression, result, "gapSolverNarrow", new ManyToOneNumberFunction("gapSolverNarrow") {
                @Override
                public double run(double... numbers) {
                    return numbers[0] * 2;
                }
            });
        }
    }
}
