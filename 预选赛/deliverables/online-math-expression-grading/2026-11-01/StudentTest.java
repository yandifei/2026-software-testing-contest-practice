package net.mooctest;

import net.mooctest.expression.engine.AbnormalOperation;
import net.mooctest.expression.engine.ArithmeticExpression;
import net.mooctest.expression.engine.BooleanCalculation;
import net.mooctest.expression.engine.BooleanCalculation2;
import net.mooctest.expression.engine.BracketsCalculation;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.BracketsExpression;
import net.mooctest.expression.engine.Calculation;
import net.mooctest.expression.engine.CalculationBooleanResults;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
import net.mooctest.expression.engine.CalculationOptimized;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.ConstantRegion;
import net.mooctest.expression.engine.CumulativeCalculation;
import net.mooctest.expression.engine.ExtractException;
import net.mooctest.expression.engine.LogResults;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.NumberCalculation;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PackExpression;
import net.mooctest.expression.engine.PrefixExpression;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.domain.QuestionType;
import net.mooctest.grading.expression.ExpressionAnswerEvaluation;
import net.mooctest.grading.expression.ExpressionAnswerGradingService;
import net.mooctest.grading.expression.ExpressionAnswerStatus;
import net.mooctest.grading.expression.ExpressionAnswerSubmission;
import net.mooctest.grading.expression.ExpressionEvaluationException;
import net.mooctest.grading.expression.ExpressionExamGrade;
import net.mooctest.grading.expression.ExpressionExamGradingService;
import net.mooctest.grading.expression.ExpressionExamSubmission;
import net.mooctest.grading.expression.ExpressionQuestion;
import net.mooctest.grading.expression.MathematicalExpressionEvaluator;
import net.mooctest.grading.service.ExamGradingService;
import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;
import net.mooctest.progress.service.ExpressionExamProgressService;
import net.mooctest.progress.service.LearningProgressService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.lingyuzhao.varFormatter.core.VarFormatter;
import top.lingyuzhao.varFormatter.utils.DataObj;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * StudentTest for the online-math-expression-grading preselection task.
 * Modules: expression engine, grading services, learning progress services.
 */
class StudentTest {

    @BeforeEach
    void resetEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
        // Isolate the static component registry between tests so every fresh-name
        // getInstance call deterministically exercises its registration path.
        try {
            Field registry = CalculationManagement.class.getDeclaredField("CALCULATIONS");
            registry.setAccessible(true);
            ((Map<?, ?>) registry.get(null)).clear();
        } catch (Exception e) {
            throw new AssertionError("cannot reset component registry", e);
        }
    }

    @AfterEach
    void restoreEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
    }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    /** Probe subclass exposing the protected expression-builder accessor of {@link NameExpression}. */
    private static final class ProbeNameExpression extends NameExpression {
        ProbeNameExpression() {
            super("1+2", "probe");
        }

        StringBuilder expressionBuilder() {
            return getExpressionStrBuilder();
        }

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
            throw new UnsupportedOperationException("probe");
        }

        @Override
        public CalculationResults calculation(boolean isCopy) {
            throw new UnsupportedOperationException("probe");
        }

        @Override
        public CalculationResults calculationBigDecimals(boolean isCopy) {
            throw new UnsupportedOperationException("probe");
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: string/number utilities
    // ------------------------------------------------------------------
    @Nested
    class EngineUtilsTests {

        @Test
        void splitByCharSplitsAndTrimsSegments() {
            assertEquals(List.of("1", "2", "3"), StrUtils.splitByChar("1,2,3", ','));
            assertEquals(List.of("1", "2", "3"), StrUtils.splitByChar(" 1 , 2 , 3 ", ','));
            assertEquals(List.of(""), StrUtils.splitByChar("", ','));
            assertEquals(List.of("abc"), StrUtils.splitByChar("abc", ','));
            assertEquals(List.of("1", "", "2"), StrUtils.splitByChar("1,,2", ','));
        }

        @Test
        void removeEmptyStripsSpaceCharacters() {
            assertEquals("123", StrUtils.removeEmpty("1 2 3"));
            assertEquals("abc", StrUtils.removeEmpty("abc"));
            assertEquals("", StrUtils.removeEmpty("   "));
        }

        @Test
        void stringToDoubleParsesPlainAndFactorialNumbers() {
            assertEquals(3.5, StrUtils.stringToDouble("3.5"));
            assertEquals(6.0, StrUtils.stringToDouble("3!"));
            assertEquals(0.0, StrUtils.stringToDouble("0!"));
            assertEquals(1.0, StrUtils.stringToDouble("1!"));
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
        }

        @Test
        void stringToDoubleRejectsNegativeFactorialAndMalformedText() {
            assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToDouble("-3!"));
            assertThrows(NumberFormatException.class, () -> StrUtils.stringToDouble("abc"));
            assertThrows(StringIndexOutOfBoundsException.class, () -> StrUtils.stringToDouble(""));
        }

        @Test
        void stringToDoubleUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(7.5, StrUtils.stringToDouble("7.5"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("7.5".hashCode()));
            assertEquals(7.5, StrUtils.stringToDouble("7.5"));
        }

        @Test
        void stringToDoubleCachesFactorialResultWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
            assertEquals(120.0, StrUtils.stringToDouble("5!"));
        }

        @Test
        void cacheLookupsReturnCachedValuesVerbatim() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.cacheCalculation("7.5".hashCode(), 999.0);
            assertEquals(999.0, StrUtils.stringToDouble("7.5"));
            Mathematical_Expression.Options.cacheCalculation("7.5".hashCode(), 7.5);

            Mathematical_Expression.Options.cacheCalculation("2.5".hashCode(), new BigDecimal("999"));
            assertEquals(0, new BigDecimal("999").compareTo(StrUtils.stringToBigDecimal("2.5")));
            Mathematical_Expression.Options.cacheCalculation("2.5".hashCode(), new BigDecimal("2.5"));
        }

        @Test
        void stringToBigDecimalComputesFactorialOfZero() {
            assertEquals(0, BigDecimal.ZERO.compareTo(StrUtils.stringToBigDecimal("0!")));
        }

        @Test
        void nameExpressionExposesExpressionBuilder() {
            ProbeNameExpression probe = new ProbeNameExpression();
            assertNotNull(probe.expressionBuilder());
            assertEquals("1+2", probe.expressionBuilder().toString());
        }

        @Test
        void stringToBigDecimalParsesPlainAndFactorialNumbers() {
            assertEquals(0, new BigDecimal("2.5").compareTo(StrUtils.stringToBigDecimal("2.5")));
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
            assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToBigDecimal("-3!"));
            assertThrows(NumberFormatException.class, () -> StrUtils.stringToBigDecimal("abc"));
        }

        @Test
        void stringToBigDecimalIgnoresNonBigDecimalCacheEntriesAndCachesOwnResult() {
            Mathematical_Expression.Options.setUseCache(true);
            StrUtils.stringToDouble("8.25");
            assertEquals(0, new BigDecimal("8.25").compareTo(StrUtils.stringToBigDecimal("8.25")));
            assertEquals(0, new BigDecimal("4.5").compareTo(StrUtils.stringToBigDecimal("4.5")));
            assertTrue(Mathematical_Expression.Options.getCacheCalculation("4.5".hashCode()) instanceof BigDecimal);
            assertEquals(0, new BigDecimal("4.5").compareTo(StrUtils.stringToBigDecimal("4.5")));
        }

        @Test
        void stringToBigDecimalUsesCacheForFactorialResults() {
            Mathematical_Expression.Options.setUseCache(true);
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("3!".hashCode()));
            assertEquals(0, BigDecimal.valueOf(6).compareTo(StrUtils.stringToBigDecimal("3!")));
        }

        @Test
        void isAnOperatorRecognizesOnlyArithmeticOperators() {
            for (char op : new char[]{'+', '-', '*', '/', '%', '^'}) {
                assertTrue(StrUtils.IsAnOperator(op));
            }
            for (char c : new char[]{'a', '=', '!', '(', ' ', '0'}) {
                assertFalse(StrUtils.IsAnOperator(c));
            }
        }

        @Test
        void isANumberValidatesDigitsOnly() {
            assertTrue(StrUtils.IsANumber("123"));
            assertTrue(StrUtils.IsANumber(""));
            assertFalse(StrUtils.IsANumber("12a"));
            assertFalse(StrUtils.IsANumber("1.5"));
            assertFalse(StrUtils.IsANumber(" 1"));
            assertTrue(StrUtils.IsANumber('0'));
            assertTrue(StrUtils.IsANumber('9'));
            assertFalse(StrUtils.IsANumber('a'));
            assertFalse(StrUtils.IsANumber('.'));
        }

        @Test
        void numberUtilsCalculationSupportsAllOperatorsAndRejectsUnknown() {
            assertEquals(5.0, NumberUtils.calculation('+', 2, 3));
            assertEquals(-1.0, NumberUtils.calculation('-', 2, 3));
            assertEquals(6.0, NumberUtils.calculation('*', 2, 3));
            assertEquals(2.0, NumberUtils.calculation('/', 6, 3));
            assertEquals(1.0, NumberUtils.calculation('%', 7, 3));
            assertEquals(8.0, NumberUtils.calculation('^', 2, 3));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.calculation('x', 1, 2));
        }

        @Test
        void priorityComparisonOnlyTreatsLowPriorityBeforeHighPriorityAsTrue() {
            assertTrue(NumberUtils.PriorityComparison('+', '*'));
            assertTrue(NumberUtils.PriorityComparison('+', '/'));
            assertTrue(NumberUtils.PriorityComparison('-', '%'));
            assertTrue(NumberUtils.PriorityComparison('-', '^'));
            assertFalse(NumberUtils.PriorityComparison('+', '+'));
            assertFalse(NumberUtils.PriorityComparison('-', '-'));
            assertFalse(NumberUtils.PriorityComparison('*', '+'));
            assertFalse(NumberUtils.PriorityComparison('/', '+'));
            assertFalse(NumberUtils.PriorityComparison('%', '-'));
            assertFalse(NumberUtils.PriorityComparison('^', '+'));
        }

        @Test
        void comparisonOperationSupportsEveryComparisonOperator() {
            assertTrue(NumberUtils.ComparisonOperation("<", 1, 2));
            assertFalse(NumberUtils.ComparisonOperation("<", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(">", 2, 1));
            assertTrue(NumberUtils.ComparisonOperation("<=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation(">=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation("=", 2, 2));
            assertTrue(NumberUtils.ComparisonOperation("==", 2, 2));
            assertFalse(NumberUtils.ComparisonOperation("==", 2, 3));
            assertTrue(NumberUtils.ComparisonOperation("!=", 2, 3));
            assertTrue(NumberUtils.ComparisonOperation("<>", 2, 3));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.ComparisonOperation("?", 1, 2));
        }

        @Test
        void comparisonOperationBoundaryCasesReturnFalse() {
            assertFalse(NumberUtils.ComparisonOperation(">", 2, 2));
            assertFalse(NumberUtils.ComparisonOperation("<=", 2, 1));
            assertFalse(NumberUtils.ComparisonOperation(">=", 1, 2));
            assertFalse(NumberUtils.ComparisonOperation("!=", 2, 2));
        }

        @Test
        void sumOfRangeStringWithOffsetStartAndStep() {
            assertEquals("4.0*2.0+4.0*(4.0-1)*2.0/2", NumberUtils.sumOfRangeString(2, 8, 2));
        }

        @Test
        void multiplyOfRangeWithEqualBoundsReturnsStartImmediately() {
            assertEquals(3.0, NumberUtils.MultiplyOfRange(3, 3, 0));
            assertEquals("3.0", NumberUtils.MultiplyOfRangeString(3, 3, 0));
        }

        @Test
        void multiplyOfRangeWithFarEndReachesFinalTerm() {
            assertEquals(280.0, NumberUtils.MultiplyOfRange(1, 10, 3));
        }

        @Test
        void factorialComputesRecursively() {
            assertEquals(0.0, NumberUtils.factorial(0));
            assertEquals(1.0, NumberUtils.factorial(1));
            assertEquals(120.0, NumberUtils.factorial(5));
            assertEquals(0.5, NumberUtils.factorial(0.5));
        }

        @Test
        void divideByTenUsesBitShiftDivision() {
            assertEquals(1, NumberUtils.divideByTen(10));
            assertEquals(0, NumberUtils.divideByTen(9));
            assertEquals(0, NumberUtils.divideByTen(0));
        }

        @Test
        void sumOfRangeStringBuildsArithmeticSeriesFormula() {
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5));
            assertEquals("(1.0+3.0)*3.0/2", NumberUtils.sumOfRangeString(1, 3));
            assertEquals("(3.0+1.0)*3.0/2", NumberUtils.sumOfRangeString(3, 1));
        }

        @Test
        void sumOfRangeStringWithStepDelegatesAndAdjustsForStep() {
            assertEquals("(1.0+3.0)*3.0/2", NumberUtils.sumOfRangeString(1, 3, 1));
            assertEquals("5.0", NumberUtils.sumOfRangeString(5, 5, 2));
            assertEquals("6.0*0.0+6.0*(6.0-1)*2.0/2", NumberUtils.sumOfRangeString(0, 10, 2));
            assertEquals("3.0*0.0+3.0*(3.0-1)*2.0/2", NumberUtils.sumOfRangeString(0, 5, 2));
        }

        @Test
        void multiplyOfRangeMultipliesElementsWithinReachableRange() {
            assertEquals(2.0, NumberUtils.MultiplyOfRange(2, 2, 1));
            assertEquals(6.0, NumberUtils.MultiplyOfRange(1, 3, 1));
            assertEquals(3.0, NumberUtils.MultiplyOfRange(1, 3, 2));
            assertEquals(3.0, NumberUtils.MultiplyOfRange(1, 4, 2));
        }

        @Test
        void multiplyOfRangeStringReturnsResultAsString() {
            assertEquals("2.0", NumberUtils.MultiplyOfRangeString(2, 2, 1));
            assertEquals("6.0", NumberUtils.MultiplyOfRangeString(1, 3, 1));
            assertEquals("3.0", NumberUtils.MultiplyOfRangeString(1, 4, 2));
        }

        @Test
        void constantRegionExposesConfiguredConstants() {
            assertEquals(1.57F, ConstantRegion.VERSION);
            assertEquals("null", ConstantRegion.STRING_NULL);
            assertEquals('(', ConstantRegion.LEFT_BRACKET);
            assertEquals(')', ConstantRegion.RIGHT_BRACKET);
            assertEquals(">", ConstantRegion.GREATER_THAN_SIGN);
            assertEquals("<", ConstantRegion.LESS_THAN_SIGN);
            assertEquals("!=", ConstantRegion.NOT_EQUAL_SIGN1);
            assertEquals("<>", ConstantRegion.NOT_EQUAL_SIGN2);
            assertEquals("<=", ConstantRegion.LESS_THAN_OR_EQUAL_TO_SIGN);
            assertEquals(">=", ConstantRegion.GREATER_THAN_OR_EQUAL_TO_SIGN);
            assertEquals("==", ConstantRegion.EQUAL_SIGN2);
            assertEquals("pow", ConstantRegion.SIGN_NAME.get('^'));
            assertEquals("_LB", ConstantRegion.SIGN_NAME.get('('));
            assertEquals("RB_", ConstantRegion.SIGN_NAME.get(')'));
            assertEquals("", ConstantRegion.SIGN_NAME.get(' '));
            assertNull(ConstantRegion.SIGN_NAME.get('+'));
        }

        @Test
        void calculationOptimizedSupportsAllBigDecimalOperations() {
            BigDecimal two = new BigDecimal("2");
            BigDecimal three = new BigDecimal("3");
            BigDecimal seven = new BigDecimal("7");
            assertEquals(0, new BigDecimal("5").compareTo(CalculationOptimized.calculation('+', two, three)));
            assertEquals(0, new BigDecimal("-1").compareTo(CalculationOptimized.calculation('-', two, three)));
            assertEquals(0, new BigDecimal("6").compareTo(CalculationOptimized.calculation('*', two, three)));
            assertEquals(0, new BigDecimal("3").compareTo(CalculationOptimized.calculation('/', new BigDecimal("6"), two)));
            assertEquals(0, new BigDecimal("1").compareTo(CalculationOptimized.calculation('%', seven, three)));
            assertEquals(0, new BigDecimal("8").compareTo(CalculationOptimized.calculation('^', two, three)));
            assertThrows(AbnormalOperation.class, () -> CalculationOptimized.calculation('x', two, three));
        }

        @Test
        void engineOptionsToggleBigDecimalAndCacheModes() {
            assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
            assertFalse(Mathematical_Expression.Options.isUseCache());
            Mathematical_Expression.Options.setUseCache(true);
            assertTrue(Mathematical_Expression.Options.isUseCache());
        }

        @Test
        void engineOptionsCacheStoresNumbersAndExpressionsAndClearOnDisable() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.cacheCalculation(42, 3.25);
            assertEquals(3.25, Mathematical_Expression.Options.getCacheCalculation(42).doubleValue());
            assertNull(Mathematical_Expression.Options.getCacheCalculation(99));

            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("optCacheInstance");
            PackExpression pack = BracketsExpression.compile("1+1", true, calculator);
            Mathematical_Expression.Options.cacheCalculation("formulaKey", pack);
            assertSame(pack, Mathematical_Expression.Options.getCacheCalculation("formulaKey"));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("missingKey"));

            Mathematical_Expression.Options.setUseCache(false);
            assertNull(Mathematical_Expression.Options.getCacheCalculation(42));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("formulaKey"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: ArithmeticExpression (decimal parser)
    // ------------------------------------------------------------------
    @Nested
    class ArithmeticExpressionTests {

        @Test
        void supportsAcceptsOnlyDecimalArithmeticCharacters() {
            assertTrue(ArithmeticExpression.supports("1+2"));
            assertTrue(ArithmeticExpression.supports("1.5*3-(2)+7 "));
            assertFalse(ArithmeticExpression.supports(null));
            assertFalse(ArithmeticExpression.supports(""));
            assertFalse(ArithmeticExpression.supports("2^3"));
            assertFalse(ArithmeticExpression.supports("1/2"));
            assertFalse(ArithmeticExpression.supports("abc"));
        }

        @Test
        void evaluateHandlesPrecedenceParenthesesAndUnarySigns() {
            assertEquals(0, new BigDecimal("3").compareTo(ArithmeticExpression.evaluate("1+2")));
            assertEquals(0, new BigDecimal("7").compareTo(ArithmeticExpression.evaluate("1+2*3")));
            assertEquals(0, new BigDecimal("9").compareTo(ArithmeticExpression.evaluate("(1+2)*3")));
            assertEquals(0, new BigDecimal("26").compareTo(ArithmeticExpression.evaluate("2*3+4*5")));
            assertEquals(0, new BigDecimal("5").compareTo(ArithmeticExpression.evaluate("10-3-2")));
            assertEquals(0, new BigDecimal("5").compareTo(ArithmeticExpression.evaluate("-5+10")));
            assertEquals(0, new BigDecimal("10").compareTo(ArithmeticExpression.evaluate("+5+5")));
            assertEquals(0, new BigDecimal("5").compareTo(ArithmeticExpression.evaluate("--5")));
            assertEquals(0, new BigDecimal("2").compareTo(ArithmeticExpression.evaluate("1*+2")));
            assertEquals(0, new BigDecimal("3.75").compareTo(ArithmeticExpression.evaluate("1.5+2.25")));
            assertEquals(0, new BigDecimal("0.3").compareTo(ArithmeticExpression.evaluate("0.1+0.2")));
            assertEquals(0, new BigDecimal("3").compareTo(ArithmeticExpression.evaluate(" 1 + 2 ")));
            assertEquals(0, new BigDecimal("1").compareTo(ArithmeticExpression.evaluate("1 ")));
        }

        @Test
        void evaluateRejectsUnsupportedAndMalformedExpressions() {
            IllegalArgumentException unsupported = assertThrows(IllegalArgumentException.class,
                    () -> ArithmeticExpression.evaluate("abc"));
            assertTrue(unsupported.getMessage().contains("unsupported"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("1+2)"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("(1+2"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("1+"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("()"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("2(3)"));
            assertThrows(IllegalArgumentException.class, () -> ArithmeticExpression.evaluate("1 2"));
            assertThrows(NumberFormatException.class, () -> ArithmeticExpression.evaluate("1..2"));
        }

        @Test
        void evaluateHandlesSpacesAroundOperatorsAndSigns() {
            assertEquals(0, new BigDecimal("3").compareTo(ArithmeticExpression.evaluate("1 + 2")));
            assertEquals(0, new BigDecimal("6").compareTo(ArithmeticExpression.evaluate("2 * 3")));
            assertEquals(0, new BigDecimal("-5").compareTo(ArithmeticExpression.evaluate("  -5")));
            assertEquals(0, new BigDecimal("3").compareTo(ArithmeticExpression.evaluate("1+2  ")));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: management registry and result objects
    // ------------------------------------------------------------------
    @Nested
    class ManagementAndResultsTests {

        @Test
        void registerAndLookupRoundTrip() {
            String name = "mgmtTest1";
            BracketsCalculation2 first = BracketsCalculation2.getInstance(name);
            assertTrue(CalculationManagement.isRegister(name));
            assertSame(first, CalculationManagement.getCalculationByName(name));
            assertFalse(CalculationManagement.register(first, true));
            assertTrue(CalculationManagement.register(first, false));
            Calculation fresh = mock(Calculation.class);
            when(fresh.getName()).thenReturn("mgmtFresh");
            assertTrue(CalculationManagement.register(fresh, true));
            assertTrue(CalculationManagement.isRegister("mgmtFresh"));
        }

        @Test
        void lookupMissingNameReturnsNullAndIsRegisterFalse() {
            assertNull(CalculationManagement.getCalculationByName("absent-name-xyz"));
            assertFalse(CalculationManagement.isRegister("absent-name-xyz"));
        }

        @Test
        void booleanCalculationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeBoolean";
            BracketsCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> BooleanCalculation2.getInstance(name));
        }

        @Test
        void bracketsCalculationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeBrackets";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> BracketsCalculation2.getInstance(name));
        }

        @Test
        void prefixOperationGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypePrefix";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> PrefixExpressionOperation.getInstance(name));
        }

        @Test
        void cumulativeGetInstanceRejectsWrongRegisteredType() {
            String name = "wrongTypeCumulative";
            BooleanCalculation2.getInstance(name);
            assertThrows(ExtractException.class, () -> CumulativeCalculation.getInstance(name));
        }

        @Test
        void cumulativeInstanceIsAlsoABracketsCalculation() {
            String name = "cumulativeAsBrackets";
            CumulativeCalculation.getInstance(name);
            assertTrue(BracketsCalculation2.getInstance(name) instanceof CumulativeCalculation);
        }

        @Test
        void calculationNumberResultsExposesStoredData() {
            CalculationNumberResults r = new CalculationNumberResults(3, 1.5, "src");
            assertEquals(3, r.getResultLayers());
            assertEquals(1.5, r.getResult());
            assertEquals("src", r.getCalculationSourceName());
            assertEquals(0, new BigDecimal("1.5").compareTo(r.getBigDecimalResult()));
            r.setSource("other");
            assertEquals("other", r.getCalculationSourceName());
            assertTrue(r.toString().contains("result=1.5"));
        }

        @Test
        void calculationNumberResultsCompareUsesCeilingOfDifference() {
            CalculationNumberResults a = new CalculationNumberResults(0, 1.5, "a");
            CalculationNumberResults b = new CalculationNumberResults(0, 1.0, "b");
            CalculationNumberResults c = new CalculationNumberResults(0, 2.0, "c");
            assertEquals(1, a.compareTo(b));
            assertEquals(0, a.compareTo(c));
            assertEquals(-1, b.compareTo(c));
            assertEquals(0, a.compareTo(new CalculationNumberResults(0, 1.5, "a2")));
            assertEquals(1, a.compareTo(null));
            assertEquals(-1, a.compare(b, c));
        }

        @Test
        void calculationBooleanResultsExposesStoredData() {
            CalculationBooleanResults r = new CalculationBooleanResults(true, "src", 2, 1.0, 2.0);
            assertTrue(r.getResult());
            assertEquals(2, r.getResultLayers());
            assertEquals("src", r.getCalculationSourceName());
            assertEquals(1.0, r.getLeft());
            assertEquals(2.0, r.getRight());
            r.setSource("other");
            assertEquals("other", r.getCalculationSourceName());
            assertTrue(r.toString().contains("result=true"));
            assertTrue(r.toString().contains("left=1.0"));
        }

        @Test
        void calculationBooleanResultsCompareByBooleanValue() {
            CalculationBooleanResults t1 = new CalculationBooleanResults(true, "a", 0, 0, 0);
            CalculationBooleanResults t2 = new CalculationBooleanResults(true, "b", 0, 0, 0);
            CalculationBooleanResults f = new CalculationBooleanResults(false, "c", 0, 0, 0);
            assertEquals(0, t1.compareTo(t2));
            assertEquals(1, t1.compareTo(f));
            assertEquals(-1, f.compareTo(t1));
            assertEquals(1, t1.compareTo(null));
            assertEquals(1, t1.compare(t1, f));
            assertEquals(-1, f.compare(f, t1));
        }

        @Test
        void logResultsStoresResultButRejectsUnsupportedOperations() {
            LogResults log = new LogResults("logName");
            assertNull(log.getResult());
            assertEquals("logName", log.getCalculationSourceName());
            log.setResult(3.0);
            assertEquals(3.0, log.getResult());
            assertThrows(UnsupportedOperationException.class, log::getResultLayers);
            assertThrows(UnsupportedOperationException.class, () -> log.setSource("x"));
            assertThrows(UnsupportedOperationException.class, log::getBigDecimalResult);
        }

        @Test
        void logResultsCanBeFormatted() {
            LogResults log = new LogResults("logName");
            log.setResult(1.0);
            String formatted = log.explain(VarFormatter.JSON);
            assertNotNull(formatted);
            assertNotEquals("", formatted);
        }

        @Test
        void defaultResultInterfaceMethodsThrowForUnsupportedResultTypes() {
            CalculationNumberResults number = new CalculationNumberResults(0, 1.0, "s");
            assertThrows(UnsupportedOperationException.class, () -> number.explain(VarFormatter.JSON));
            CalculationBooleanResults bool = new CalculationBooleanResults(true, "s", 0, 0, 0);
            assertThrows(UnsupportedOperationException.class, bool::getBigDecimalResult);
        }

        @Test
        void exceptionClassesCarryMessageAndCause() {
            AbnormalOperation a1 = new AbnormalOperation();
            AbnormalOperation a2 = new AbnormalOperation("m");
            AbnormalOperation a3 = new AbnormalOperation("m", new IllegalStateException("c"));
            AbnormalOperation a4 = new AbnormalOperation(new IllegalStateException("c"));
            assertNull(a1.getMessage());
            assertEquals("m", a2.getMessage());
            assertEquals("m", a3.getMessage());
            assertEquals("c", a3.getCause().getMessage());
            assertEquals("c", a4.getCause().getMessage());

            ExtractException e1 = new ExtractException();
            ExtractException e2 = new ExtractException("m");
            ExtractException e3 = new ExtractException("m", new IllegalStateException("c"));
            ExtractException e4 = new ExtractException(new IllegalStateException("c"));
            assertNull(e1.getMessage());
            assertEquals("m", e2.getMessage());
            assertEquals("m", e3.getMessage());
            assertEquals("c", e4.getCause().getMessage());

            WrongFormat w1 = new WrongFormat();
            WrongFormat w2 = new WrongFormat("m");
            WrongFormat w3 = new WrongFormat("m", new IllegalStateException("c"));
            WrongFormat w4 = new WrongFormat(new IllegalStateException("c"));
            assertNull(w1.getMessage());
            assertEquals("m", w2.getMessage());
            assertEquals("m", w3.getMessage());
            assertEquals("c", w4.getCause().getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: format validation of number calculations
    // ------------------------------------------------------------------
    @Nested
    class NumberCalculationCheckTests {

        private final BracketsCalculation2 calculator = BracketsCalculation2.getInstance("numberCheckInstance");

        @Test
        void checkRejectsEmptyExpression() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check(""));
            assertTrue(e.getMessage().contains("空"));
        }

        @Test
        void checkAcceptsValidExpressions() {
            assertDoesNotThrow(() -> calculator.check("1+2"));
            assertDoesNotThrow(() -> calculator.check("(1+2)"));
            assertDoesNotThrow(() -> calculator.check("1-2"));
            assertDoesNotThrow(() -> calculator.check("1--2"));
            assertDoesNotThrow(() -> calculator.check("1+-2"));
            assertDoesNotThrow(() -> calculator.check("1..2"));
            assertDoesNotThrow(() -> calculator.check("3!"));
            assertDoesNotThrow(() -> calculator.check("!"));
            assertDoesNotThrow(() -> calculator.check("1 + 2"));
            assertDoesNotThrow(() -> calculator.check("1+2 "));
            assertDoesNotThrow(() -> calculator.check("1+2  "));
            assertDoesNotThrow(() -> calculator.check("1.5*2^3/4%2"));
            assertDoesNotThrow(() -> calculator.check("2(3)"));
        }

        @Test
        void checkRejectsTrailingOperator() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check("1+"));
            assertTrue(e.getMessage().contains("最后一个字符"));
        }

        @Test
        void checkRejectsUnknownSymbolsAndDoubleOperators() {
            assertThrows(WrongFormat.class, () -> calculator.check("1a2"));
            assertThrows(WrongFormat.class, () -> calculator.check("1**2"));
        }

        @Test
        void checkRejectsUnbalancedParentheses() {
            assertThrows(WrongFormat.class, () -> calculator.check("(1+2"));
            assertThrows(WrongFormat.class, () -> calculator.check("1+2)"));
            assertThrows(WrongFormat.class, () -> calculator.check(")"));
        }

        @Test
        void checkReportsExactBracketCountDifference() {
            WrongFormat e = assertThrows(WrongFormat.class, () -> calculator.check("(1+2))"));
            assertTrue(e.getMessage().contains("[1]"));
        }

        @Test
        void checkHandlesMidExpressionClosingBracketAndPlusAfterOperator() {
            assertDoesNotThrow(() -> calculator.check("(1+2)*2"));
            assertDoesNotThrow(() -> calculator.check("1++2"));
        }

        @Test
        void prefixExpressionOperationCheckRejectsBracketsAndBadEndings() {
            PrefixExpressionOperation op = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
            assertDoesNotThrow(() -> op.check("1+2"));
            assertDoesNotThrow(() -> op.check("1--2"));
            assertDoesNotThrow(() -> op.check("1 2"));
            assertThrows(WrongFormat.class, () -> op.check("1+"));
            assertThrows(WrongFormat.class, () -> op.check("(1+2)"));
            assertThrows(WrongFormat.class, () -> op.check("(1+2"));
            assertThrows(WrongFormat.class, () -> op.check("1)+2"));
            assertThrows(WrongFormat.class, () -> op.check("1a2"));
            assertThrows(WrongFormat.class, () -> op.check("1**2"));
            assertThrows(WrongFormat.class, () -> op.check("1!2"));
        }

        @Test
        void prefixOperationCheckHandlesDecimalsAndTrailingSpaces() {
            PrefixExpressionOperation op = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);
            assertDoesNotThrow(() -> op.check("1.5+2"));
            assertDoesNotThrow(() -> op.check("1+2 "));
            assertDoesNotThrow(() -> op.check("1+2  "));
        }

        @Test
        void cumulativeCheckValidatesIntervalAndDelegatesInnerFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCheckInstance");
            assertDoesNotThrow(() -> calc.check("n[0,10,1](1+n)"));
            assertDoesNotThrow(() -> calc.check("x[0,10,1](1+x)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10](1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n(1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10,1]"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,2,1][0,2,1](1+n)"));
            assertThrows(WrongFormat.class, () -> calc.check("n[0,10,1,2](1+n)"));
        }

        @Test
        void booleanCalculationCheckAcceptsComparisonsAndRejectsNonBooleans() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanCheckInstance");
            for (String ok : new String[]{"1>2", "1<2", "1>=2", "1<=2", "1==2", "1!=2", "1<>2", "1=2", "1+2>3+4"}) {
                assertDoesNotThrow(() -> calc.check(ok), ok);
            }
            assertDoesNotThrow(() -> calc.check("null>1"));
            assertDoesNotThrow(() -> calc.check("1>null"));
            assertThrows(WrongFormat.class, () -> calc.check("1+2"));
            assertThrows(WrongFormat.class, () -> calc.check(""));
            assertThrows(WrongFormat.class, () -> calc.check("1>2>3"));
            assertThrows(WrongFormat.class, () -> calc.check(">1"));
            assertThrows(WrongFormat.class, () -> calc.check("1>"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: PrefixExpression compilation and evaluation
    // ------------------------------------------------------------------
    @Nested
    class PrefixExpressionTests {

        @Test
        void isBackIsOptAppendsNumberCharacters() {
            StringBuilder sb = new StringBuilder();
            PrefixExpression.isBackIsOpt(sb, '1');
            PrefixExpression.isBackIsOpt(sb, '2');
            PrefixExpression.isBackIsOpt(sb, '.');
            PrefixExpression.isBackIsOpt(sb, '5');
            PrefixExpression.isBackIsOpt(sb, '-');
            PrefixExpression.isBackIsOpt(sb, '!');
            PrefixExpression.isBackIsOpt(sb, 'a');
            assertEquals("12.5-!", sb.toString());
        }

        @Test
        void compileEvaluatesWithEnginePrecedenceRules() {
            assertEquals(3.0, PrefixExpression.compile("1+2", "t").calculationCache(false).getResult());
            assertEquals(9.0, PrefixExpression.compile("1+2*3", "t").calculationCache(false).getResult());
            assertEquals(5.0, PrefixExpression.compile("1*2+3", "t").calculationCache(false).getResult());
            assertEquals(-4.0, PrefixExpression.compile("1-2-3", "t").calculationCache(false).getResult());
            assertEquals(9.0, PrefixExpression.compile("2^3+1", "t").calculationCache(false).getResult());
            assertEquals(27.0, PrefixExpression.compile("1+2^3", "t").calculationCache(false).getResult());
        }

        @Test
        void compileStoresExpressionAndCalculationName() {
            PrefixExpression p = PrefixExpression.compile("1+2", "myName");
            assertEquals("myName", p.getCalculationName());
            assertEquals("1+2 -> 1.0+0", p.getExpressionStr());
        }

        @Test
        void compileBigDecimalEvaluatesWithBigDecimalStacks() {
            PrefixExpression p = PrefixExpression.compileBigDecimal("1+2", "t");
            assertTrue(p.isBigDecimal());
            assertFalse(p.isUnBigDecimal());
            CalculationNumberResults r = p.calculationBigDecimals(false);
            assertEquals(3.0, r.getResult());
            assertEquals(0, new BigDecimal("3").compareTo(r.getBigDecimalResult()));
            assertEquals("1+2 -> 1+0", p.getExpressionStr());
            assertEquals(2, p.getBigDecimalsR(true).size());
            assertEquals(2, p.getBigDecimalsR(false).size());
        }

        @Test
        void calculationRejectsWrongPrecisionMode() {
            PrefixExpression decimal = PrefixExpression.compileBigDecimal("1+2", "t");
            assertThrows(UnsupportedOperationException.class, () -> decimal.calculation(false));
            PrefixExpression plain = PrefixExpression.compile("1+2", "t");
            assertThrows(UnsupportedOperationException.class, () -> plain.calculationBigDecimals(false));
        }

        @Test
        void calculationCacheWithoutCopyDisablesExpression() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertTrue(p.isAvailable());
            assertEquals(3.0, p.calculationCache(false).getResult());
            assertFalse(p.isAvailable());
            assertEquals(3.0, p.calculationCache(false).getResult());
            assertThrows(UnsupportedOperationException.class, () -> p.getDoubleStack(false));
            assertThrows(UnsupportedOperationException.class, () -> p.getCharacterStack(false));
            assertThrows(UnsupportedOperationException.class, () -> p.getBigDecimalsR(false));
        }

        @Test
        void calculationCacheWithCopyKeepsExpressionUsable() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertEquals(3.0, p.calculationCache(true).getResult());
            assertTrue(p.isAvailable());
            assertEquals(3.0, p.calculationCache(true).getResult());
        }

        @Test
        void stackAccessorsSupportCopyAndRawAccess() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            Stack<Double> copy = p.getDoubleStack(true);
            assertEquals(2, copy.size());
            copy.push(99.0);
            assertEquals(2, p.getDoubleStack(true).size());
            assertEquals(2, p.getDoubleStack(false).size());
            assertEquals(1, p.getCharacterStack(false).size());
            assertEquals(1, p.getCharacterStack(true).size());
            assertNull(p.getBigDecimalsR(false));
        }

        @Test
        void convertToMultiPrecisionFromPlainMode() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            assertFalse(p.isBigDecimal());
            assertTrue(p.isUnBigDecimal());
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isBigDecimal());
            assertTrue(p.isUnBigDecimal());
            assertEquals(3.0, p.calculation(true).getResult());
            assertEquals(3.0, p.calculationBigDecimals(true).getResult());
        }

        @Test
        void convertToMultiPrecisionFromBigDecimalMode() {
            PrefixExpression p = PrefixExpression.compileBigDecimal("1+2", "t");
            assertTrue(p.isBigDecimal());
            assertFalse(p.isUnBigDecimal());
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isUnBigDecimal());
            assertEquals(3.0, p.calculation(true).getResult());
            assertEquals(3.0, p.calculationBigDecimals(true).getResult());
        }

        @Test
        void convertToMultiPrecisionIsIdempotentWhenBothModesSupported() {
            PrefixExpression p = PrefixExpression.compile("1+2", "t");
            p.convertToMultiPrecisionSupported();
            assertDoesNotThrow(p::convertToMultiPrecisionSupported);
            PrefixExpression q = PrefixExpression.compileBigDecimal("1+2", "t");
            q.convertToMultiPrecisionSupported();
            assertDoesNotThrow(q::convertToMultiPrecisionSupported);
        }

        @Test
        void stackAccessorsHonorCopyFlag() {
            PrefixExpression big = PrefixExpression.compileBigDecimal("1+2", "stackCopyName");
            Stack<BigDecimal> copy = big.getBigDecimalsR(true);
            assertNotSame(big.getBigDecimalsR(false), copy);
            PrefixExpression plain = PrefixExpression.compile("1+2", "stackCopyName2");
            Stack<Character> charsCopy = plain.getCharacterStack(true);
            assertNotSame(plain.getCharacterStack(false), charsCopy);
        }

        @Test
        void reflectionReachesBigDecimalStackEarlyReturn() throws Exception {
            PrefixExpression p = PrefixExpression.compile("1+2", "reflectName");
            assertFalse(p.isBigDecimal());
            Field bigDecimalsR = PrefixExpression.class.getDeclaredField("bigDecimalsR");
            bigDecimalsR.setAccessible(true);
            bigDecimalsR.set(p, new Stack<BigDecimal>());
            p.convertToMultiPrecisionSupported();
            assertFalse(p.isBigDecimal());
        }

        @Test
        void compileUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            PrefixExpression first = PrefixExpression.compile("1+2", "cachedName");
            PrefixExpression second = PrefixExpression.compile("1+2", "cachedName");
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2"));
        }

        @Test
        void compileBigDecimalUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            PrefixExpression first = PrefixExpression.compileBigDecimal("1+2", "cachedName");
            PrefixExpression second = PrefixExpression.compileBigDecimal("1+2", "cachedName");
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2uB"));
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: PrefixExpressionOperation component
    // ------------------------------------------------------------------
    @Nested
    class PrefixExpressionOperationTests {

        private final PrefixExpressionOperation op =
                PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME);

        @Test
        void formatStrReplacesAmbiguousPlusMinusAndAppendsZero() {
            assertEquals("1+2+0", op.formatStr("1+2"));
            assertEquals("1-2+0", op.formatStr("1+-2"));
            assertEquals("1-2-0+0", op.formatStr("1+-2-0"));
        }

        @Test
        void compileAppliesFormattingOnlyWhenRequired() {
            assertEquals("1+2+0 -> 3.0+0", op.compile("1+2", true).getExpressionStr());
            assertEquals("1+2 -> 1.0+0", op.compile("1+2", false).getExpressionStr());
            assertEquals("1+2+0 -> 3+0", op.compileBigDecimal("1+2", true).getExpressionStr());
            assertEquals("1+2 -> 1+0", op.compileBigDecimal("1+2", false).getExpressionStr());
        }

        @Test
        void calculationUsesBigDecimalPathByDefaultAndDoublePathWhenEnabled() {
            assertEquals(3.0, op.calculation("1+2", true).getResult());
            assertEquals(3.0, op.calculation("1+2", false).getResult());
            assertEquals(0, new BigDecimal("3").compareTo(op.calculation("1+2", true).getBigDecimalResult()));
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertEquals(3.0, op.calculation("1+2", true).getResult());
        }

        @Test
        void explainRecordsResultForSimpleExpression() {
            LogResults log = op.explain("1+2", true);
            assertEquals(3.0, log.getResult());
            assertTrue(log.getCalculationSourceName().startsWith("f_"));
            assertThrows(UnsupportedOperationException.class, log::getResultLayers);
            assertThrows(UnsupportedOperationException.class, () -> log.setSource("x"));
        }

        @Test
        void explainHandlesLowerPriorityOperators() {
            assertEquals(7.0, PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME)
                    .explain("1+2*3", true).getResult());
        }

        @Test
        void explainHandlesHigherPriorityOperators() {
            LogResults log = PrefixExpressionOperation.getInstance(CalculationManagement.PREFIX_EXPRESSION_OPERATION_NAME)
                    .explain("1*2+3", true);
            assertEquals(5.0, log.getResult());
        }

        @Test
        void getInstanceNeverReturnsNull() {
            PrefixExpressionOperation fresh = PrefixExpressionOperation.getInstance("peoFreshNullInstance");
            assertNotNull(fresh);
            PrefixExpressionOperation cached = PrefixExpressionOperation.getInstance("peoFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void checkReportsBracketMessagesForParentheses() {
            WrongFormat left = assertThrows(WrongFormat.class, () -> op.check("(1+2)+3"));
            assertTrue(left.getMessage().contains("括号"));
            WrongFormat right = assertThrows(WrongFormat.class, () -> op.check("1)+2"));
            assertTrue(right.getMessage().contains("括号"));
        }

        @Test
        void calculationCachesUnderBigDecimalKeyByDefault() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            op.calculation("9.5+0.5", false);
            assertNull(Mathematical_Expression.Options.getCacheCalculation("9.5+0.5"));
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("9.5+0.5uB"));
        }

        @Test
        void explainBuildsObservableLogTreeStructure() {
            LogResults log = op.explain("1+2", true);
            String name = log.getCalculationSourceName();
            assertNotEquals("", log.getPrefix());
            assertFalse(log.isNameJoin());
            List<DataObj> nodes = new ArrayList<>();
            collectTree(log, nodes);
            assertTrue(nodes.stream().anyMatch(d -> d.getName().equals(name + "_优先")));
            assertTrue(nodes.stream().anyMatch(d -> d.getName().equals(name + "_最终")));
            assertTrue(nodes.stream().anyMatch(d -> d.containsKey("result")
                    && new BigDecimal(d.get("result").toString()).compareTo(new BigDecimal("3")) == 0));
            assertTrue(nodes.stream().anyMatch(d -> d.containsKey("result") && !d.getPrefix().isEmpty()));

            LogResults prec = op.explain("1*2+3", true);
            List<DataObj> pnodes = new ArrayList<>();
            collectTree(prec, pnodes);
            assertTrue(pnodes.stream().anyMatch(d -> d.getName()
                    .equals("f_" + ("1.0 * 2.0").hashCode() + "_计算") && !d.getPrefix().isEmpty()));
        }

        private static void collectTree(DataObj node, List<DataObj> out) {
            out.add(node);
            for (Object value : node.values()) {
                if (value instanceof DataObj) {
                    collectTree((DataObj) value, out);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: bracket calculation components
    // ------------------------------------------------------------------
    @Nested
    class BracketsCalculationTests {

        @Test
        void formatStrRemovesWhitespace() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsFormatInstance");
            assertEquals("1+2", calculator.formatStr(" 1 + 2 "));
            assertEquals("1+2", calculator.formatStr("1+2"));
        }

        @Test
        void deprecatedBinaryCalculationComputesResultOrFailsWithoutOperator() {
            BracketsCalculation calculator = BracketsCalculation2.getInstance("bracketsDeprecatedInstance");
            assertEquals(3.0, calculator.calculation2("1+2"));
            assertEquals(-3.0, calculator.calculation2("5-2"));
            assertThrows(AbnormalOperation.class, () -> calculator.calculation2("12"));
        }

        @Test
        void getInstanceReturnsCachedInstance() {
            BracketsCalculation2 first = BracketsCalculation2.getInstance("bracketsSameInstance");
            BracketsCalculation2 second = BracketsCalculation2.getInstance("bracketsSameInstance");
            assertSame(first, second);
        }

        @Test
        void calculationHandlesNestedParentheses() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCalcInstance");
            assertEquals(3.0, calculator.calculation("1+2").getResult());
            assertEquals(9.0, calculator.calculation("(1+2)*3").getResult());
            assertEquals(21.0, calculator.calculation("(1+2)*(3+4)").getResult());
            assertEquals(10.0, calculator.calculation("((1+2)*2)+4").getResult());
            assertEquals(3.0, calculator.calculation("((1+2))").getResult());
            assertEquals(6.0, calculator.calculation("(1+(2+3))").getResult());
        }

        @Test
        void calculationBigDecimalPathComputesHighPrecisionResults() {
            Mathematical_Expression.Options.setUseBigDecimal(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsBigInstance");
            CalculationNumberResults r = calculator.calculation("(1+2)*3");
            assertEquals(9.0, r.getResult());
            assertEquals(0, new BigDecimal("9").compareTo(r.getBigDecimalResult()));
        }

        @Test
        void compileWrapsInnerExpressionWithPackName() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCompileInstance");
            PackExpression pack = calculator.compile("1+2", true);
            assertEquals("1+2 -> 1+2+0 -> 3.0+0", pack.getExpressionStr());
            assertTrue(pack.getCalculationName().endsWith(":{PrefixExpressionOperation}"));
            NameExpression inner = pack.dismantling();
            assertEquals("PrefixExpressionOperation", inner.getCalculationName());
        }

        @Test
        void packCalculationSetsSourceAndCachesResults() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsPackInstance");
            PackExpression pack = calculator.compile("1+2", true);
            CalculationResults first = pack.calculationCache(false);
            assertEquals(3.0, (Double) first.getResult());
            assertEquals(pack.getCalculationName(), first.getCalculationSourceName());
            CalculationResults second = pack.calculationCache(false);
            assertSame(first, second);
            assertEquals(3.0, (Double) pack.calculation(true).getResult());
        }

        @Test
        void compileBigDecimalProducesBigDecimalCapablePack() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsBigCompileInstance");
            PackExpression pack = calculator.compileBigDecimal("1+2", true);
            assertTrue(pack.isBigDecimal());
            CalculationResults r = pack.calculationBigDecimalsCache(false);
            assertEquals(3.0, (Double) r.getResult());
            assertEquals(0, new BigDecimal("3").compareTo(r.getBigDecimalResult()));
            assertEquals(pack.getCalculationName(), r.getCalculationSourceName());
            assertFalse(pack.isAvailable());
            CalculationResults cachedAgain = pack.calculationBigDecimalsCache(false);
            assertSame(r, cachedAgain);
            assertTrue(pack.calculationBigDecimalsCache(true) != null);
        }

        @Test
        void bracketsExpressionCompileUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprCacheInstance");
            PackExpression first = BracketsExpression.compile("1+2", true, calculator);
            PackExpression second = BracketsExpression.compile("1+2", true, calculator);
            assertSame(first, second);
        }

        @Test
        void bracketsExpressionCompileBigDecimalUsesCacheWhenEnabled() {
            Mathematical_Expression.Options.setUseCache(true);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprBigCacheInstance");
            PackExpression first = BracketsExpression.compileBigDecimal("1+2", true, calculator);
            PackExpression second = BracketsExpression.compileBigDecimal("1+2", true, calculator);
            assertSame(first, second);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("1+2uB"));
        }

        @Test
        void explainBuildsNestedLogTree() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExplainInstance");
            LogResults simple = calculator.explain("1+2", true);
            assertEquals(3.0, simple.getResult());
            assertTrue(simple.getCalculationSourceName().startsWith("f_"));
            LogResults nested = calculator.explain("(1+2)*3", true);
            assertEquals(9.0, nested.getResult());
            LogResults deep = calculator.explain("(1+2)*(3+4)", true);
            assertEquals(21.0, deep.getResult());
        }

        @Test
        void explainWithoutFormattingSkipsSpacesInRawFormula() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsRawSpacesInstance");
            assertEquals(3.0, calculator.explain("1 + 2", false).getResult());
        }

        @Test
        void explainNestedFormulaBuildsObservableTree() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsNestedTreeInstance");
            LogResults nested = calculator.explain("(1+2)*3", true);
            assertNotEquals("", nested.getPrefix());
            assertTrue(nested.containsKey("f_" + "1+2".hashCode()));
            assertTrue(nested.containsKey("f_" + "3.0*3".hashCode()));
        }

        @Test
        void calculationCachesUnderDoubleKeyByDefault() {
            Mathematical_Expression.Options.setUseCache(true);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsCacheKeyInstance");
            calculator.calculation("(1+2)*3", true);
            assertNotNull(Mathematical_Expression.Options.getCacheCalculation("(1+2)*3"));
            assertNull(Mathematical_Expression.Options.getCacheCalculation("(1+2)*3uB"));
        }

        @Test
        void bracketsExpressionCompileHandlesNestedAndRawFormulas() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsExprNestedInstance");
            PackExpression nested = BracketsExpression.compile("((1+2))", true, calculator);
            assertEquals(3.0, (Double) nested.calculation(true).getResult());
            PackExpression raw = BracketsExpression.compile("1 + 2", false, calculator);
            assertEquals(3.0, (Double) raw.calculation(true).getResult());
        }

        @Test
        void packExpressionReflectsInnerPrecisionMode() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("bracketsPackModeInstance");
            PackExpression pack = calculator.compile("1+2", true);
            assertFalse(pack.isBigDecimal());
            assertTrue(pack.isUnBigDecimal());
            PackExpression big = calculator.compileBigDecimal("1+2", true);
            assertTrue(big.isBigDecimal());
            assertFalse(big.isUnBigDecimal());
            pack.convertToMultiPrecisionSupported();
            assertTrue(pack.isBigDecimal());
            assertTrue(pack.isUnBigDecimal());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: boolean comparison component
    // ------------------------------------------------------------------
    @Nested
    class BooleanCalculationTests {

        @Test
        void formatAndNameAccessors() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanAccessorInstance");
            assertEquals("1>2", calc.formatStr(" 1 > 2 "));
            assertEquals("booleanAccessorInstance", calc.getName());
        }

        @Test
        void calculationEvaluatesEveryComparisonOperator() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanCalcInstance");
            assertFalse(calc.calculation("1>2").getResult());
            assertTrue(calc.calculation("2>1").getResult());
            assertTrue(calc.calculation("1<2").getResult());
            assertFalse(calc.calculation("2<1").getResult());
            assertTrue(calc.calculation("1<=1").getResult());
            assertTrue(calc.calculation("1>=1").getResult());
            assertTrue(calc.calculation("1==1").getResult());
            assertFalse(calc.calculation("1==2").getResult());
            assertTrue(calc.calculation("1!=2").getResult());
            assertTrue(calc.calculation("1<>2").getResult());
            assertFalse(calc.calculation("1=2").getResult());
        }

        @Test
        void calculationStoresLeftRightAndLayerCount() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanDataInstance");
            CalculationBooleanResults r = calc.calculation("1 > 2");
            assertEquals(1.0, r.getLeft());
            assertEquals(2.0, r.getRight());
            assertEquals(2, r.getResultLayers());
            assertFalse(r.getResult());
            CalculationBooleanResults nested = calc.calculation("1+1>1");
            assertEquals(2, nested.getResultLayers());
            assertTrue(nested.getResult());
        }

        @Test
        void calculationWithoutFormattingKeepsRawFormula() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanRawInstance");
            assertFalse(calc.calculation("1>2", false).getResult());
            assertTrue(calc.calculation("2>1", false).getResult());
        }

        @Test
        void nullOperandBranchesFailBecauseOperatorIsLost() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanNullInstance");
            assertThrows(AbnormalOperation.class, () -> calc.calculation("null>5"));
            assertThrows(AbnormalOperation.class, () -> calc.calculation("null==null"));
            assertThrows(AbnormalOperation.class, () -> calc.calculation("5>null"));
        }

        @Test
        void setCalculationSwapsNumericEngine() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanSwappedEngine");
            NumberCalculation replacement = mock(NumberCalculation.class);
            when(replacement.calculation(anyString()))
                    .thenReturn(new CalculationNumberResults(0, 1.0, "mock"))
                    .thenReturn(new CalculationNumberResults(0, 2.0, "mock"));
            calc.setCalculation(replacement);
            CalculationBooleanResults r = calc.calculation("1>2");
            assertFalse(r.getResult());
            verify(replacement, times(2)).calculation(anyString());
        }

        @Test
        void defaultExplainIsUnsupported() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanExplainInstance");
            assertThrows(UnsupportedOperationException.class, () -> calc.explain("1>2", true));
        }

        @Test
        void getInstanceRejectsNonBooleanComponent() {
            PrefixExpressionOperation.getInstance("booleanWrongTypeInstance");
            assertThrows(ExtractException.class, () -> BooleanCalculation2.getInstance("booleanWrongTypeInstance"));
        }

        @Test
        void getInstanceReturnsRegisteredComponentOfSameType() {
            BooleanCalculation2 first = BooleanCalculation2.getInstance("booleanRepeatInstance");
            BooleanCalculation2 second = BooleanCalculation2.getInstance("booleanRepeatInstance");
            assertSame(first, second);
        }

        @Test
        void getInstanceNeverReturnsNull() {
            BooleanCalculation2 fresh = BooleanCalculation2.getInstance("booleanFreshNullInstance");
            assertNotNull(fresh);
            BooleanCalculation2 cached = BooleanCalculation2.getInstance("booleanFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void legacyBooleanCheckValidatesSidesAndReportsOperatorCount() {
            BooleanCalculation legacy = BooleanCalculation2.getInstance("booleanLegacyCheckInstance");
            assertThrows(WrongFormat.class, () -> legacy.check("1+>3"));
            assertThrows(WrongFormat.class, () -> legacy.check("1+2>3+"));
            WrongFormat e = assertThrows(WrongFormat.class, () -> legacy.check("1+2"));
            assertTrue(e.getMessage().contains("[0]"));
        }

        @Test
        void calculationFormatsSpacedComparisons() {
            BooleanCalculation2 calc = BooleanCalculation2.getInstance("booleanSpacedInstance");
            assertFalse(calc.calculation(" 1 > 2 ").getResult());
        }
    }

    // ------------------------------------------------------------------
    // Expression engine: cumulative (sigma) calculation component
    // ------------------------------------------------------------------
    @Nested
    class CumulativeCalculationTests {

        @Test
        void formatStrExpandsIntervalIntoSumOfTerms() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeFormatInstance");
            assertEquals("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", calc.formatStr("n[0,2,1](n*n)"));
            assertEquals("((3.0+1))", calc.formatStr("n[3,3,1](n+1)"));
            assertEquals("((1+0.0))", calc.formatStr("n[0,0,1](1+n)"));
        }

        @Test
        void formatStrFailsForDescendingIntervalWithoutIterations() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeEmptyInstance");
            assertThrows(StringIndexOutOfBoundsException.class, () -> calc.formatStr("n[0,-1,1](n)"));
        }

        @Test
        void calculationSumsTermsOfInterval() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCalcInstance");
            assertEquals(5.0, calc.calculation("n[0,2,1](n*n)").getResult());
            assertEquals(4.0, calc.calculation("n[3,3,1](n+1)").getResult());
        }

        @Test
        void calculationWithoutFormattingUsesRawFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeRawInstance");
            assertEquals(5.0, calc.calculation("(0.0*0.0)+(1.0*1.0)+(2.0*2.0)", false).getResult());
        }

        @Test
        void explainReturnsSumOfInterval() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeExplainInstance");
            assertEquals(5.0, calc.explain("n[0,2,1](n*n)", true).getResult());
        }

        @Test
        void compileAndCompileBigDecimalEvaluateIntervalSums() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeCompileInstance");
            assertEquals(5.0, (Double) calc.compile("n[0,2,1](n*n)", true).calculationCache(false).getResult());
            assertEquals(5.0, (Double) calc.compile("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false)
                    .calculationCache(false).getResult());
            CalculationResults r = calc.compileBigDecimal("n[0,2,1](n*n)", true).calculationBigDecimalsCache(false);
            assertEquals(5.0, (Double) r.getResult());
            assertEquals(0, new BigDecimal("5").compareTo(r.getBigDecimalResult()));
            assertEquals(5.0, (Double) calc.compileBigDecimal("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false)
                    .calculationBigDecimalsCache(false).getResult());
            assertEquals(5.0, calc.explain("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))", false).getResult());
        }

        @Test
        void getInstanceRejectsNonCumulativeComponent() {
            PrefixExpressionOperation.getInstance("cumulativeWrongTypeInstance");
            assertThrows(ExtractException.class, () -> CumulativeCalculation.getInstance("cumulativeWrongTypeInstance"));
        }

        @Test
        void getInstanceReturnsRegisteredComponentOfSameType() {
            CumulativeCalculation first = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            CumulativeCalculation second = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            assertSame(first, second);
        }

        @Test
        void getInstanceNeverReturnsNull() {
            CumulativeCalculation fresh = CumulativeCalculation.getInstance("cumulativeFreshNullInstance");
            assertNotNull(fresh);
            CumulativeCalculation cached = CumulativeCalculation.getInstance("cumulativeFreshNullInstance");
            assertNotNull(cached);
            assertSame(fresh, cached);
        }

        @Test
        void checkValidatesInnerFormula() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeInnerCheckInstance");
            assertThrows(WrongFormat.class, () -> calc.check("n[0,2,1](1a2)"));
        }
    }

    // ------------------------------------------------------------------
    // Grading domain: records and enums
    // ------------------------------------------------------------------
    @Nested
    class GradingDomainTests {

        @Test
        void answerResultExposesAllComponents() {
            AnswerResult r = new AnswerResult("q1", QuestionType.MULTIPLE_CHOICE, "S2",
                    new BigDecimal("8.5"), true, false, false, 4, 2, 1,
                    new BigDecimal("0.25"), new BigDecimal("0.5"), 60, 3, 5,
                    true, true, true, true);
            assertEquals("q1", r.questionId());
            assertEquals(QuestionType.MULTIPLE_CHOICE, r.type());
            assertEquals("S2", r.section());
            assertBigDecimalEquals("8.5", r.maximumPoints());
            assertTrue(r.mandatory());
            assertFalse(r.answered());
            assertFalse(r.exactMatch());
            assertEquals(4, r.correctOptionCount());
            assertEquals(2, r.selectedCorrectOptions());
            assertEquals(1, r.selectedIncorrectOptions());
            assertBigDecimalEquals("0.25", r.numericError());
            assertBigDecimalEquals("0.5", r.numericTolerance());
            assertEquals(60, r.rubricPercent());
            assertEquals(3, r.passedTests());
            assertEquals(5, r.totalTests());
            assertTrue(r.compilationSucceeded());
            assertTrue(r.plagiarismFlag());
            assertTrue(r.timedOut());
            assertTrue(r.manualReviewRequired());
        }

        @Test
        void questionTypeEnumeratesSevenKinds() {
            assertEquals(7, QuestionType.values().length);
            assertArrayEquals(new QuestionType[]{QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE,
                    QuestionType.TRUE_FALSE, QuestionType.FILL_BLANK, QuestionType.NUMERIC,
                    QuestionType.ESSAY, QuestionType.PROGRAMMING}, QuestionType.values());
        }

        @Test
        void examSubmissionDefendsAgainstNullCollections() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExamSubmission s = new ExamSubmission("e", "s", null, start, start.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, BigDecimal.ZERO, null, null);
            assertEquals(List.of(), s.answers());
            assertEquals(Map.of(), s.sectionPassPercentages());
            assertEquals("e", s.examId());
            assertEquals("s", s.studentId());
        }

        @Test
        void examSubmissionFreezesCopiedCollections() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            AnswerResult answer = new AnswerResult("q1", QuestionType.NUMERIC, "S", BigDecimal.TEN,
                    false, true, true, 0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            List<AnswerResult> src = new ArrayList<>(List.of(answer));
            Map<String, Integer> passMap = new HashMap<>();
            passMap.put("S", 50);
            ExamSubmission s = new ExamSubmission("e", "s", src, start, start.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, BigDecimal.ZERO, null, passMap);
            src.clear();
            passMap.clear();
            assertEquals(1, s.answers().size());
            assertEquals(50, s.sectionPassPercentages().get("S"));
            assertThrows(UnsupportedOperationException.class, () -> s.answers().add(answer));
            assertThrows(UnsupportedOperationException.class, () -> s.sectionPassPercentages().put("X", 1));
        }

        @Test
        void gradeReportDefendsAgainstNullCollectionsAndFreezesCopies() {
            GradeReport empty = new GradeReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 0,
                    "F", false, false, null, null, null);
            assertEquals(Map.of(), empty.sectionPercentages());
            assertEquals(List.of(), empty.failures());
            assertEquals(List.of(), empty.warnings());

            GradeReport r = new GradeReport(new BigDecimal("5.00"), new BigDecimal("5.00"), BigDecimal.TEN, 50,
                    "F", false, true, Map.of("S", 50), List.of("f1"), List.of("w1"));
            assertBigDecimalEquals("5.00", r.rawPoints());
            assertBigDecimalEquals("5.00", r.finalPoints());
            assertBigDecimalEquals("10", r.maximumPoints());
            assertEquals(50, r.percentage());
            assertEquals("F", r.letterGrade());
            assertFalse(r.passed());
            assertTrue(r.manualReview());
            assertEquals(50, r.sectionPercentages().get("S"));
            assertEquals(List.of("f1"), r.failures());
            assertEquals(List.of("w1"), r.warnings());
            assertThrows(UnsupportedOperationException.class, () -> r.failures().add("x"));
        }

        @Test
        void gradingPolicyExposesAllComponents() {
            GradingPolicy p = new GradingPolicy(60, true, true, new BigDecimal("0.5"), new BigDecimal("0.25"),
                    10, 50, 40, true, true, 3, true, true, true);
            assertEquals(60, p.passPercentage());
            assertTrue(p.partialCreditEnabled());
            assertTrue(p.negativeMarkingEnabled());
            assertBigDecimalEquals("0.5", p.wrongAnswerPenaltyRate());
            assertBigDecimalEquals("0.25", p.unansweredPenaltyRate());
            assertEquals(10, p.latePenaltyPercentPerHour());
            assertEquals(50, p.maximumLatePenaltyPercent());
            assertEquals(40, p.minimumSectionPercentage());
            assertTrue(p.allMandatoryQuestionsRequired());
            assertTrue(p.identityVerificationRequired());
            assertEquals(3, p.maximumAttempts());
            assertTrue(p.failOnIntegrityViolation());
            assertTrue(p.capScoreAtOneHundred());
            assertTrue(p.programmingCompilationRequired());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: question, submission, evaluation records
    // ------------------------------------------------------------------
    @Nested
    class ExpressionGradingTests {

        private static ExpressionQuestion question(String id, String correct, String maxPoints,
                                                   String tolerance, boolean mandatory) {
            return new ExpressionQuestion(id, "math", correct, new BigDecimal(maxPoints),
                    new BigDecimal(tolerance), mandatory);
        }

        private static AnswerResult numericAnswer(String id, boolean answered, boolean exact,
                                                   BigDecimal error, BigDecimal tolerance) {
            return new AnswerResult(id, QuestionType.NUMERIC, "math", BigDecimal.TEN, false, answered, exact,
                    0, 0, 0, error, tolerance, 0, 0, 0, false, false, false, false);
        }

        @Test
        void expressionQuestionValidatesEveryField() {
            ExpressionQuestion q = question("q1", "2+2", "10", "0.1", true);
            assertEquals("q1", q.questionId());
            assertEquals("math", q.section());
            assertEquals("2+2", q.correctExpression());
            assertBigDecimalEquals("10", q.maximumPoints());
            assertBigDecimalEquals("0.1", q.tolerance());
            assertTrue(q.mandatory());
            assertThrows(IllegalArgumentException.class, () -> question(null, "2+2", "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question(" ", "2+2", "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", null, "10", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "", "10", "0.1", true));
            assertThrows(NullPointerException.class, () -> question("q", "2+2", null, "0.1", true));
            assertThrows(NullPointerException.class, () -> question("q", "2+2", "10", null, true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "0", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "-5", "0.1", true));
            assertThrows(IllegalArgumentException.class, () -> question("q", "2+2", "10", "-0.1", true));
            assertDoesNotThrow(() -> question("q", "2+2", "10", "0", true));
        }

        @Test
        void expressionAnswerSubmissionRequiresQuestion() {
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerSubmission(null, "2+2"));
            ExpressionAnswerSubmission s = new ExpressionAnswerSubmission(question("q1", "2+2", "10", "0", true), "2+2");
            assertEquals("q1", s.question().questionId());
            assertEquals("2+2", s.submittedExpression());
        }

        @Test
        void expressionAnswerEvaluationRequiresAnswerResultAndStatus() {
            AnswerResult answer = numericAnswer("q1", true, true, null, BigDecimal.ZERO);
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerEvaluation(
                    null, ExpressionAnswerStatus.EXACT, null, null, null, "m"));
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerEvaluation(
                    answer, null, null, null, null, "m"));
            ExpressionAnswerEvaluation ev = new ExpressionAnswerEvaluation(answer, ExpressionAnswerStatus.EXACT,
                    new BigDecimal("4"), new BigDecimal("4"), BigDecimal.ZERO, "EXACT");
            assertSame(answer, ev.answerResult());
            assertEquals(ExpressionAnswerStatus.EXACT, ev.status());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertBigDecimalEquals("4", ev.actualValue());
            assertBigDecimalEquals("0", ev.absoluteError());
            assertEquals("EXACT", ev.message());
        }

        @Test
        void expressionAnswerStatusEnumeratesFiveStates() {
            assertEquals(5, ExpressionAnswerStatus.values().length);
        }

        @Test
        void expressionEvaluationExceptionCarriesMessageAndCause() {
            ExpressionEvaluationException e1 = new ExpressionEvaluationException("m");
            assertEquals("m", e1.getMessage());
            assertNull(e1.getCause());
            ExpressionEvaluationException e2 = new ExpressionEvaluationException("m", new IllegalStateException("c"));
            assertEquals("m", e2.getMessage());
            assertEquals("c", e2.getCause().getMessage());
        }

        @Test
        void expressionExamGradeRequiresReportAndFreezesEvaluations() {
            GradeReport report = new GradeReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 0, "F",
                    false, false, Map.of(), List.of(), List.of());
            assertThrows(NullPointerException.class, () -> new ExpressionExamGrade(null, List.of()));
            ExpressionExamGrade g = new ExpressionExamGrade(report, null);
            assertSame(report, g.report());
            assertEquals(List.of(), g.answerEvaluations());
        }

        @Test
        void expressionExamSubmissionFreezesCollectionsAndConvertsToExamSubmission() {
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            Instant submit = start.plusSeconds(300);
            Instant deadline = start.plusSeconds(3600);
            ExpressionAnswerSubmission a1 = new ExpressionAnswerSubmission(question("q1", "2+2", "10", "0", true), "2+2");
            ExpressionExamSubmission s = new ExpressionExamSubmission("exam", "stu", List.of(a1),
                    start, submit, deadline, 3600, 2, true, true, false, true, false, 1, 2,
                    new BigDecimal("1.5"), 90, Map.of("math", 60));
            assertEquals("exam", s.examId());
            assertEquals("stu", s.studentId());
            assertEquals(1, s.answers().size());

            ExpressionExamSubmission empty = new ExpressionExamSubmission("exam", "stu", null,
                    start, submit, deadline, 3600, 2, true, true, false, true, false, 1, 2,
                    new BigDecimal("1.5"), 90, null);
            assertEquals(List.of(), empty.answers());
            assertEquals(Map.of(), empty.sectionPassPercentages());

            AnswerResult evaluated = numericAnswer("q1", true, true, null, BigDecimal.ZERO);
            ExamSubmission converted = s.toExamSubmission(List.of(evaluated));
            assertEquals("exam", converted.examId());
            assertEquals("stu", converted.studentId());
            assertEquals(List.of(evaluated), converted.answers());
            assertEquals(start, converted.startedAt());
            assertEquals(submit, converted.submittedAt());
            assertEquals(deadline, converted.deadline());
            assertEquals(3600, converted.allowedDurationSeconds());
            assertEquals(2, converted.attemptNumber());
            assertTrue(converted.identityVerified());
            assertTrue(converted.termsAccepted());
            assertFalse(converted.suspendedAccount());
            assertTrue(converted.proctorDisconnected());
            assertFalse(converted.ipAddressChanged());
            assertEquals(1, converted.fullscreenExitCount());
            assertEquals(2, converted.copyPasteEventCount());
            assertBigDecimalEquals("1.5", converted.bonusPoints());
            assertEquals(90, converted.instructorOverridePercent());
            assertEquals(60, converted.sectionPassPercentages().get("math"));
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: ExpressionAnswerGradingService
    // ------------------------------------------------------------------
    @Nested
    class ExpressionAnswerGradingServiceTests {

        private static ExpressionAnswerGradingService gradingService() {
            return new ExpressionAnswerGradingService(expression -> {
                switch (expression) {
                    case "2+2":
                        return new BigDecimal("4");
                    case "2*3":
                        return new BigDecimal("6");
                    default:
                        throw new ExpressionEvaluationException("unexpected expression: " + expression);
                }
            });
        }

        private static ExpressionQuestion question(String id, String correct, String tolerance, boolean mandatory) {
            return new ExpressionQuestion(id, "math", correct, BigDecimal.TEN, new BigDecimal(tolerance), mandatory);
        }

        @Test
        void constructorRequiresEvaluator() {
            assertThrows(NullPointerException.class, () -> new ExpressionAnswerGradingService(null));
        }

        @Test
        void evaluateRequiresSubmission() {
            assertThrows(NullPointerException.class, () -> gradingService().evaluate(null));
        }

        @Test
        void evaluateBlankSubmissionIsUnanswered() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", true);
            for (String blank : new String[]{null, "", "   "}) {
                ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, blank));
                assertEquals(ExpressionAnswerStatus.UNANSWERED, ev.status());
                assertEquals("ANSWER_NOT_PROVIDED", ev.message());
                assertFalse(ev.answerResult().answered());
                assertFalse(ev.answerResult().exactMatch());
                assertNull(ev.answerResult().numericError());
                assertNull(ev.expectedValue());
                assertNull(ev.actualValue());
                assertNull(ev.absoluteError());
                assertEquals("q1", ev.answerResult().questionId());
                assertEquals(QuestionType.NUMERIC, ev.answerResult().type());
            }
        }

        @Test
        void evaluateWrapsInvalidCorrectExpressionInIllegalState() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "broken", "0.1", true);
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> service.evaluate(new ExpressionAnswerSubmission(q, "2+2")));
            assertEquals("invalid correct expression for question q1", e.getMessage());
            assertTrue(e.getCause() instanceof ExpressionEvaluationException);
        }

        @Test
        void evaluateFlagsInvalidStudentExpression() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", false);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "broken"));
            assertEquals(ExpressionAnswerStatus.INVALID_EXPRESSION, ev.status());
            assertEquals("INVALID_STUDENT_EXPRESSION", ev.message());
            assertTrue(ev.answerResult().answered());
            assertFalse(ev.answerResult().exactMatch());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertNull(ev.actualValue());
            assertNull(ev.absoluteError());
        }

        @Test
        void evaluateExactMatchPopulatesFullResult() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", true);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "2+2"));
            assertEquals(ExpressionAnswerStatus.EXACT, ev.status());
            assertEquals("EXACT", ev.message());
            assertTrue(ev.answerResult().exactMatch());
            assertBigDecimalEquals("4", ev.expectedValue());
            assertBigDecimalEquals("4", ev.actualValue());
            assertBigDecimalEquals("0", ev.absoluteError());
            AnswerResult a = ev.answerResult();
            assertEquals("q1", a.questionId());
            assertEquals(QuestionType.NUMERIC, a.type());
            assertEquals("math", a.section());
            assertBigDecimalEquals("10", a.maximumPoints());
            assertTrue(a.mandatory());
            assertBigDecimalEquals("0.1", a.numericTolerance());
            assertBigDecimalEquals("0", a.numericError());
        }

        @Test
        void evaluateWithinToleranceOnBothSides() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2*3", "0.1", false);
            // actual 4.05 via another evaluator
            ExpressionAnswerGradingService tolerant = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("6.05"));
            ExpressionAnswerEvaluation high = tolerant.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, high.status());
            assertEquals("WITHIN_TOLERANCE", high.message());
            assertFalse(high.answerResult().exactMatch());
            assertBigDecimalEquals("0.05", high.absoluteError());
            ExpressionAnswerGradingService low = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("5.95"));
            ExpressionAnswerEvaluation below = low.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, below.status());
            assertBigDecimalEquals("0.05", below.absoluteError());
        }

        @Test
        void evaluateBoundaryErrorExactlyAtTolerance() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2*3", "0.1", false);
            ExpressionAnswerGradingService edge = new ExpressionAnswerGradingService(expression ->
                    expression.equals("2*3") ? new BigDecimal("6") : new BigDecimal("6.1"));
            ExpressionAnswerEvaluation evaluation = edge.evaluate(new ExpressionAnswerSubmission(q, "other"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, evaluation.status());
            assertBigDecimalEquals("0.1", evaluation.absoluteError());
        }

        @Test
        void evaluateIncorrectBeyondTolerance() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0.1", false);
            ExpressionAnswerEvaluation ev = service.evaluate(new ExpressionAnswerSubmission(q, "2*3"));
            assertEquals(ExpressionAnswerStatus.INCORRECT, ev.status());
            assertEquals("INCORRECT", ev.message());
            assertFalse(ev.answerResult().exactMatch());
            assertBigDecimalEquals("2", ev.absoluteError());
            assertBigDecimalEquals("6", ev.actualValue());
        }

        @Test
        void evaluateUsesToleranceZeroForStrictComparison() {
            ExpressionAnswerGradingService service = gradingService();
            ExpressionQuestion q = question("q1", "2+2", "0", false);
            ExpressionAnswerEvaluation wrong = service.evaluate(new ExpressionAnswerSubmission(q, "2*3"));
            assertEquals(ExpressionAnswerStatus.INCORRECT, wrong.status());
            ExpressionAnswerEvaluation exact = service.evaluate(new ExpressionAnswerSubmission(q, "2+2"));
            assertEquals(ExpressionAnswerStatus.EXACT, exact.status());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: MathematicalExpressionEvaluator
    // ------------------------------------------------------------------
    @Nested
    class MathematicalExpressionEvaluatorTests {

        @Test
        void defaultEvaluatorUsesArithmeticPathForDecimalExpressions() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            assertBigDecimalEquals("3", evaluator.evaluate("1+2"));
            assertBigDecimalEquals("9", evaluator.evaluate("(1+2)*3"));
            assertBigDecimalEquals("3.75", evaluator.evaluate("1.5+2.25"));
            assertBigDecimalEquals("0.3", evaluator.evaluate("0.1+0.2"));
        }

        @Test
        void defaultEvaluatorFallsBackToEngineForUnsupportedOperators() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            assertBigDecimalEquals("8", evaluator.evaluate("2^3"));
            assertBigDecimalEquals("10", evaluator.evaluate("2+2^3"));
            assertBigDecimalEquals("6", evaluator.evaluate("3!"));
        }

        @Test
        void evaluatorRejectsBlankExpressions() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            for (String bad : new String[]{null, "", "   "}) {
                ExpressionEvaluationException e = assertThrows(ExpressionEvaluationException.class,
                        () -> evaluator.evaluate(bad));
                assertEquals("expression must not be blank", e.getMessage());
            }
        }

        @Test
        void evaluatorWrapsEngineFailures() {
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();
            ExpressionEvaluationException e = assertThrows(ExpressionEvaluationException.class,
                    () -> evaluator.evaluate("abc"));
            assertTrue(e.getMessage().contains("unable to evaluate expression"));
            assertThrows(ExpressionEvaluationException.class, () -> evaluator.evaluate("1+"));
        }

        @Test
        void evaluatorWithCustomCalculatorUsesEnginePathOnly() {
            BracketsCalculation2 calculator = BracketsCalculation2.getInstance("customEvaluatorCalculator");
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator(calculator);
            assertBigDecimalEquals("3", evaluator.evaluate("1+2"));
            assertBigDecimalEquals("8", evaluator.evaluate("2^3"));
            assertBigDecimalEquals("6", evaluator.evaluate("3!"));
            assertBigDecimalEquals("21", evaluator.evaluate("(1+2)*(3+4)"));
        }

        @Test
        void evaluatorConstructorRejectsNullCalculator() {
            assertThrows(NullPointerException.class,
                    () -> new MathematicalExpressionEvaluator((BracketsCalculation2) null));
        }

        @Test
        void evaluatorThrowsWhenResultHasNoNumericValue() {
            BracketsCalculation2 calculator = mock(BracketsCalculation2.class);
            PackExpression expression = mock(PackExpression.class);
            CalculationResults result = mock(CalculationResults.class);
            when(calculator.compileBigDecimal(anyString(), anyBoolean())).thenReturn(expression);
            when(expression.calculationBigDecimals(anyBoolean())).thenReturn(result);
            when(result.getBigDecimalResult()).thenReturn(null);
            MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator(calculator);
            ExpressionEvaluationException e = assertThrows(ExpressionEvaluationException.class,
                    () -> evaluator.evaluate("1+2"));
            assertEquals("expression produced no numeric result", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Grading service: ExamGradingService
    // ------------------------------------------------------------------
    @Nested
    class ExamGradingServiceTests {

        private static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
        private static final AtomicInteger IDS = new AtomicInteger();

        private static AnswerResult ans(QuestionType type, String section, String max, boolean mandatory,
                                        boolean answered, boolean exact, int correctOptions, int selectedCorrect,
                                        int selectedIncorrect, String numericError, String numericTolerance,
                                        int rubric, int passed, int total, boolean compiled, boolean plag,
                                        boolean timedOut, boolean manual) {
            return new AnswerResult("q" + IDS.incrementAndGet(), type, section, new BigDecimal(max), mandatory,
                    answered, exact, correctOptions, selectedCorrect, selectedIncorrect,
                    numericError == null ? null : new BigDecimal(numericError),
                    numericTolerance == null ? null : new BigDecimal(numericTolerance),
                    rubric, passed, total, compiled, plag, timedOut, manual);
        }

        private static AnswerResult ans(QuestionType type, String max, boolean answered, boolean exact) {
            return ans(type, "S1", max, false, answered, exact, 0, 0, 0, null, null, 0, 0, 0,
                    false, false, false, false);
        }

        private static AnswerResult raw(String id, QuestionType type, String section, String max,
                                        boolean mandatory, boolean answered, boolean exact, int correctOptions,
                                        int selectedCorrect, int selectedIncorrect, String numericError,
                                        String numericTolerance, int rubric, int passed, int total,
                                        boolean compiled, boolean plag, boolean timedOut, boolean manual) {
            return new AnswerResult(id, type, section, max == null ? null : new BigDecimal(max), mandatory,
                    answered, exact, correctOptions, selectedCorrect, selectedIncorrect,
                    numericError == null ? null : new BigDecimal(numericError),
                    numericTolerance == null ? null : new BigDecimal(numericTolerance),
                    rubric, passed, total, compiled, plag, timedOut, manual);
        }

        private static ExamSubmission sub(String examId, String studentId, List<AnswerResult> answers,
                                          Instant start, Instant submit, Instant deadline, int allowedSeconds,
                                          int attempt, boolean identityVerified, boolean termsAccepted,
                                          boolean suspended, boolean proctor, boolean ipChanged, int fullscreen,
                                          int copyPaste, String bonus, Integer override,
                                          Map<String, Integer> sectionPass) {
            return new ExamSubmission(examId, studentId, answers, start, submit, deadline, allowedSeconds, attempt,
                    identityVerified, termsAccepted, suspended, proctor, ipChanged, fullscreen, copyPaste,
                    new BigDecimal(bonus), override, sectionPass);
        }

        private static ExamSubmission sub(List<AnswerResult> answers, Instant start, Instant submit,
                                          Instant deadline, int allowedSeconds, int attempt,
                                          boolean identityVerified, boolean termsAccepted, boolean suspended,
                                          boolean proctor, boolean ipChanged, int fullscreen, int copyPaste,
                                          String bonus, Integer override, Map<String, Integer> sectionPass) {
            return sub("exam1", "stu1", answers, start, submit, deadline, allowedSeconds, attempt,
                    identityVerified, termsAccepted, suspended, proctor, ipChanged, fullscreen, copyPaste,
                    bonus, override, sectionPass);
        }

        private static ExamSubmission sub(List<AnswerResult> answers) {
            return sub(answers, T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of());
        }

        private static GradingPolicy pol(int pass, boolean partial, boolean negative, String wrongRate,
                                         String unansweredRate, int latePerHour, int maxLate, int minSection,
                                         boolean allMandatory, boolean idRequired, int maxAttempts,
                                         boolean failIntegrity, boolean cap, boolean compilationRequired) {
            return new GradingPolicy(pass, partial, negative, new BigDecimal(wrongRate),
                    new BigDecimal(unansweredRate), latePerHour, maxLate, minSection, allMandatory, idRequired,
                    maxAttempts, failIntegrity, cap, compilationRequired);
        }

        private static GradingPolicy pol() {
            return pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true);
        }

        private final ExamGradingService service = new ExamGradingService();

        @Test
        void singleChoiceCorrectAwardsFullCredit() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertBigDecimalEquals("10", report.maximumPoints());
            assertEquals(100, report.percentage());
            assertEquals("A+", report.letterGrade());
            assertTrue(report.passed());
            assertFalse(report.manualReview());
            assertEquals(100, report.sectionPercentages().get("S1"));
            assertTrue(report.failures().isEmpty());
            assertTrue(report.warnings().isEmpty());
        }

        @Test
        void trueFalseWrongWithoutNegativeMarkingGivesZero() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.TRUE_FALSE, "10", true, false))), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void negativeMarkingDeductsWrongAnswerPenalty() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", true, false))),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
            assertEquals(25, report.percentage());
        }

        @Test
        void unansweredQuestionAppliesPenaltyRateWhenConfigured() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", false, false))),
                    pol(60, true, false, "0.50", "0.25", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("7.50", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("UNANSWERED_PENALTY:")));
        }

        @Test
        void unansweredWithoutPenaltyGivesZero() {
            GradeReport report = service.grade(sub(List.of(
                    ans(QuestionType.SINGLE_CHOICE, "10", true, true),
                    ans(QuestionType.SINGLE_CHOICE, "10", false, false))), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("UNANSWERED_PENALTY:")));
        }

        @Test
        void multipleChoiceExactMatchGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, true,
                    3, 3, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void multipleChoiceExactFlagOverridesPartialCounts() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, true,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void multipleChoiceWithNoCorrectOptionsGivesZeroAndWarning() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:")));
        }

        @Test
        void multipleChoiceWithoutPartialCreditGivesZeroWhenWrong() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void multipleChoiceWithoutPartialCreditAndNegativeMarkingPenalizesIncorrectSelections() {
            AnswerResult wrong = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), wrong)),
                    pol(60, false, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());

            AnswerResult noIncorrect = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport zeroed = service.grade(sub(List.of(noIncorrect)),
                    pol(60, false, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", zeroed.rawPoints());
        }

        @Test
        void multipleChoicePartialCreditScoresSelectedOptionsAndDeductsIncorrect() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 2, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("5.83", report.rawPoints());
        }

        @Test
        void multipleChoicePartialCreditWithNegativeMarkingUsesPenaltyRate() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 2, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void multipleChoiceOnlyIncorrectSelectionWarnsAndScoresZeroAfterClamp() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 0, 1, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("ONLY_INCORRECT_OPTIONS_SELECTED:")));
        }

        @Test
        void multipleChoiceAllCorrectSelectedWithoutExactFlagStillFullCredit() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    2, 2, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void fillBlankExactMatchGivesFullCredit() {
            assertBigDecimalEquals("10.00", service.grade(
                    sub(List.of(ans(QuestionType.FILL_BLANK, "10", true, true))), pol()).rawPoints());
        }

        @Test
        void fillBlankPartialCreditUsesRubricPercent() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 40, 0, 0, false, false, false, false);
            assertBigDecimalEquals("4.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void fillBlankWithoutRubricAndNegativeMarkingPenalizes() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.FILL_BLANK, "10", true, true), wrong)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void fillBlankWithoutRubricAndNoNegativeGivesZero() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(wrong)), pol()).rawPoints());
        }

        @Test
        void numericExactMatchGivesFullCredit() {
            assertBigDecimalEquals("10.00", service.grade(
                    sub(List.of(ans(QuestionType.NUMERIC, "10", true, true))), pol()).rawPoints());
        }

        @Test
        void numericWithinToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.05", "0.1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericMissingToleranceDataWarnsAndGivesZero() {
            AnswerResult noError = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, null, "0.1", 0, 0, 0, false, false, false, false);
            AnswerResult noTolerance = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.05", null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(noError, noTolerance)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(2, report.warnings().stream().filter(w -> w.startsWith("NUMERIC_TOLERANCE_DATA_MISSING:")).count());
        }

        @Test
        void numericPartialCreditScalesByErrorRatio() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "3", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("7.78", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericPartialCreditWithNegativeError() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "-3", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("7.78", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericBeyondTenTimesToleranceGivesZero() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "10.5", "1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericZeroToleranceSkipsPartialCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.5", "0", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericNegativeMarkingPenalizesWrongAnswers() {
            AnswerResult wrong = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "11", "1", 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.NUMERIC, "10", true, true), wrong)),
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.rawPoints());
        }

        @Test
        void essayExactMatchGivesFullCredit() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayRubricScalesCredit() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            assertBigDecimalEquals("6.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayWithoutRubricGivesZero() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(essay)), pol()).rawPoints());
        }

        @Test
        void essayManualReviewAddsWarning() {
            AnswerResult essay = ans(QuestionType.ESSAY, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, true);
            GradeReport report = service.grade(sub(List.of(essay)), pol());
            assertBigDecimalEquals("6.00", report.rawPoints());
            assertTrue(report.warnings().contains("ESSAY_SCORE_IS_PROVISIONAL:q" + IDS.get()));
            assertTrue(report.warnings().contains("MANUAL_GRADING_REQUIRED:q" + IDS.get()));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void programWithoutCompilationFailsWhenRequired() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("PROGRAM_DID_NOT_COMPILE:")));
        }

        @Test
        void programWithoutCompilationGetsTenthCreditWhenNotRequired() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, false));
            assertBigDecimalEquals("1.00", report.rawPoints());
        }

        @Test
        void programCompiledWithoutTestsAndManualReviewGivesZero() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, true, false, false, true);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("PROGRAM_HAS_NO_AUTOMATED_TESTS:")));
        }

        @Test
        void programAllTestsPassedGivesFullCredit() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 4, 4, true, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programWithoutPartialCreditGivesZeroWhenSomeTestsFail() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void programPartialCreditScalesByPassRate() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, false, false);
            assertBigDecimalEquals("7.50", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programTimeoutAppliesBothReductions() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, false, true, false);
            assertBigDecimalEquals("4.50", service.grade(sub(List.of(program)), pol()).rawPoints());
        }

        @Test
        void programPlagiarismZeroesScore() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 3, 4, true, true, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("ANSWER_INTEGRITY_VIOLATION:")));
            assertTrue(report.manualReview());
        }

        @Test
        void timedOutAnswerLosesTwentyPercent() {
            AnswerResult answer = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, true, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("8.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("QUESTION_TIMED_OUT:")));
        }

        @Test
        void timedOutZeroScoreIsNotRescaled() {
            AnswerResult answer = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, true, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("QUESTION_TIMED_OUT:")));
        }

        @Test
        void mandatoryAnsweredCorrectlySkipsMandatoryFailureAndWarning() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(mandatory)), pol());
            assertBigDecimalEquals("10.00", report.rawPoints());
            assertTrue(report.failures().stream().noneMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("MANDATORY_QUESTION_NO_CREDIT:")));
            assertTrue(report.passed());
        }

        @Test
        void fillBlankPartialCreditRequiresPolicyEnabled() {
            AnswerResult wrong = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 40, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(wrong)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void numericPartialCreditRequiresPolicyEnabled() {
            AnswerResult wrong = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "3", "1", 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(wrong)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void bonusPointsAreAddedAndWarned() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "5", null, Map.of()),
                    pol());
            assertBigDecimalEquals("15.00", report.finalPoints());
            assertEquals(150, report.percentage());
            assertTrue(report.warnings().contains("BONUS_POINTS_APPLIED"));
        }

        @Test
        void capScoreAtOneHundredClampsPercentageAndAdjusted() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "5", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, true, true));
            assertEquals(100, report.percentage());
            assertBigDecimalEquals("10.00", report.finalPoints());
        }

        @Test
        void lateSubmissionDeductsPerHourPenalty() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), T0.plusSeconds(540), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("9.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:10"));
        }

        @Test
        void lateSubmissionUnderOneMinuteStillChargesOneHour() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(30), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("9.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:10"));
        }

        @Test
        void lateSubmissionChargesWholeHours() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(121 * 60), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("7.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:30"));
        }

        @Test
        void latePenaltyRespectsMaximumCap() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(2 * 3600), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 60, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("5.00", report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:50"));
        }

        @Test
        void latePenaltyAtHundredPercentFailsSubmission() {
            Instant submit = T0.plusSeconds(600);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.minusSeconds(3600), 3600, 1, true, true, false, false, false,
                    0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 100, 100, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("0.00", report.finalPoints());
            assertTrue(report.failures().contains("SUBMISSION_TOO_LATE"));
        }

        @Test
        void noLatePenaltyWithoutDeadlineOrWhenSubmittedInTime() {
            Instant submit = T0.plusSeconds(600);
            GradeReport noDeadline = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, null, 3600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol());
            assertBigDecimalEquals("10.00", noDeadline.finalPoints());
            GradeReport inTime = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, submit, submit.plusSeconds(60), 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of()), pol());
            assertBigDecimalEquals("10.00", inTime.finalPoints());
        }

        @Test
        void shortOverrunUsesGracePeriod() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(130), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
        }

        @Test
        void exactlySixtySecondsOverrunStillUsesGracePeriod() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(160), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
        }

        @Test
        void exactlyThreeHundredSecondsOverrunIsMinor() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(400), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("9.50", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));
            assertFalse(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        void durationExactlyAtAllowedTimeHasNoPenalty() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("EXAM_")));
        }

        @Test
        void minorOverrunReducesScoreByFivePercent() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(161), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("9.50", report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));
        }

        @Test
        void majorOverrunReducesByTwentyPercentAndFails() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(401), null, 100, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("8.00", report.finalPoints());
            assertTrue(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        void zeroAllowedDurationSkipsDurationChecks() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
        }

        @Test
        void attemptsAboveMaximumZeroTheScore() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 4, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("0.00", report.finalPoints());
            assertTrue(report.failures().contains("MAXIMUM_ATTEMPTS_EXCEEDED"));
        }

        @Test
        void finalAttemptAddsWarning() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 3, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertBigDecimalEquals("10.00", report.finalPoints());
            assertTrue(report.warnings().contains("FINAL_ALLOWED_ATTEMPT"));
        }

        @Test
        void sectionBelowMinimumFailsAndListsFailure() {
            AnswerResult a = ans(QuestionType.SINGLE_CHOICE, "S-A", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult b = ans(QuestionType.SINGLE_CHOICE, "S-B", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(a, b)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertEquals(100, report.sectionPercentages().get("S-A"));
            assertEquals(0, report.sectionPercentages().get("S-B"));
            assertTrue(report.failures().contains("SECTION_BELOW_PASS_MARK:S-B"));
            assertFalse(report.passed());
        }

        @Test
        void perSectionOverrideCanLowerTheBar() {
            AnswerResult a = ans(QuestionType.SINGLE_CHOICE, "S-A", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult b = ans(QuestionType.SINGLE_CHOICE, "S-B", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(a, b), T0, T0.plusSeconds(600), null, 3600, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of("S-B", 0)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertTrue(report.passed());
            assertTrue(report.failures().stream().noneMatch(f -> f.startsWith("SECTION_BELOW_PASS_MARK")));
        }

        @Test
        void requiredSectionMissingFromAnswersIsReported() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of("S-C", 80)), pol());
            assertEquals(0, report.sectionPercentages().get("S-C"));
            assertTrue(report.failures().contains("REQUIRED_SECTION_MISSING:S-C"));
            assertFalse(report.passed());
        }

        @Test
        void emptySubmissionScoresZeroPercent() {
            GradeReport report = service.grade(sub(List.of()), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void termsNotAcceptedAddsFailure() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, false, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.failures().contains("EXAM_TERMS_NOT_ACCEPTED"));
            assertTrue(report.passed());
        }

        @Test
        void suspendedAccountAddsFailureAndBlocksWhenConfigured() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, true, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("ACCOUNT_SUSPENDED"));
            assertFalse(report.passed());
        }

        @Test
        void identityVerificationRequiredAndMissingFails() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, false, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, true, 3, true, false, true));
            assertTrue(report.failures().contains("IDENTITY_NOT_VERIFIED"));
            assertFalse(report.passed());
        }

        @Test
        void identityNotVerifiedOnlyWarnsWhenNotRequired() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, false, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("IDENTITY_NOT_VERIFIED"));
            assertTrue(report.passed());
        }

        @Test
        void identityRequiredAndVerifiedStaysSilent() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, true, 3, false, false, true));
            assertTrue(report.failures().stream().noneMatch(f -> f.contains("IDENTITY")));
            assertTrue(report.warnings().stream().noneMatch(w -> w.contains("IDENTITY")));
            assertTrue(report.passed());
        }

        @Test
        void integrityScanIgnoresNonIntegrityFailures() {
            AnswerResult correct = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(correct, mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, false, false, 3, true, false, true));
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.passed());
        }

        @Test
        void proctorDisconnectionWarnsAndTriggersManualReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, true, false, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("PROCTOR_CONNECTION_LOST"));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void ipAddressChangeWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, true, 0, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("IP_ADDRESS_CHANGED"));
        }

        @Test
        void singleFullscreenExitWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 1, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("FULLSCREEN_EXIT_RECORDED"));
        }

        @Test
        void twoFullscreenExitsWarn() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 2, 0, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("MULTIPLE_FULLSCREEN_EXITS"));
        }

        @Test
        void excessiveFullscreenExitsFailAndTriggerReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 3, 0, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("EXCESSIVE_FULLSCREEN_EXITS"));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void fewCopyPasteEventsWarn() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 3, "0", null, Map.of()),
                    pol());
            assertTrue(report.warnings().contains("COPY_PASTE_EVENTS_RECORDED"));
        }

        @Test
        void excessiveCopyPasteFailsAndTriggersReview() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 5, "0", null, Map.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().contains("EXCESSIVE_COPY_PASTE_EVENTS"));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void mandatoryUnansweredQuestionFailsAndWarns() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(mandatory)), pol());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MANDATORY_QUESTION_NO_CREDIT:")));
            assertFalse(report.passed());
        }

        @Test
        void mandatoryRequirementCanBeRelaxedByPolicy() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, false, false, 3, false, false, true));
            assertTrue(report.passed());
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
        }

        @Test
        void answerIntegrityViolationFailsWhenConfigured() {
            AnswerResult plag = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, true, false, false);
            GradeReport report = service.grade(sub(List.of(plag)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.failures().stream().anyMatch(f -> f.startsWith("ANSWER_INTEGRITY_VIOLATION:")));
            assertTrue(report.manualReview());
            assertFalse(report.passed());
        }

        @Test
        void manualReviewRequiredAnswerWarnsAndFlagsReport() {
            AnswerResult manual = ans(QuestionType.SINGLE_CHOICE, "S1", "10", false, true, true,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, true);
            GradeReport report = service.grade(sub(List.of(manual)), pol());
            assertTrue(report.warnings().stream().anyMatch(w -> w.startsWith("MANUAL_GRADING_REQUIRED:")));
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
            assertTrue(report.manualReview());
        }

        @Test
        void instructorOverrideReplacesScoreAndWarns() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 50, Map.of()),
                    pol());
            assertEquals(50, report.percentage());
            assertBigDecimalEquals("5.00", report.finalPoints());
            assertTrue(report.warnings().contains("INSTRUCTOR_OVERRIDE_APPLIED"));
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void letterGradeFollowsScale() {
            Map<Integer, String> expected = Map.of(
                    95, "A+", 90, "A", 85, "A-", 80, "B+", 75, "B", 70, "C+", 65, "C", 60, "D");
            for (Map.Entry<Integer, String> entry : expected.entrySet()) {
                AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                        0, 0, 0, null, null, entry.getKey(), 0, 0, false, false, false, false);
                GradeReport report = service.grade(sub(List.of(answer)), pol());
                assertEquals(entry.getKey(), report.percentage());
                assertEquals(entry.getValue(), report.letterGrade(), "grade for " + entry.getKey() + "%");
            }
        }

        @Test
        void failedExamWithDecentScoreKeepsLetterGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 65, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(70, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals("C", report.letterGrade());
            assertFalse(report.passed());
        }

        @Test
        void failingScoreBelowSixtyAlwaysFailsWithFGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 55, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertEquals("F", report.letterGrade());
        }

        @Test
        void percentageExactlyAtPassMarkPasses() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertEquals(60, report.percentage());
            assertTrue(report.passed());
            assertEquals("D", report.letterGrade());
        }

        @Test
        void failedAtPassMarkDoesNotAddOverallFailure() {
            AnswerResult strong = ans(QuestionType.FILL_BLANK, "S-A", "100", false, true, false,
                    0, 0, 0, null, null, 100, 0, 0, false, false, false, false);
            AnswerResult weak = ans(QuestionType.FILL_BLANK, "S-B", "100", false, true, false,
                    0, 0, 0, null, null, 20, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(strong, weak)),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 50, true, false, 3, false, false, true));
            assertEquals(60, report.percentage());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("SECTION_BELOW_PASS_MARK:S-B"));
            assertFalse(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        void failedExamAtSixtyPercentKeepsLetterGradeD() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 60, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(70, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals("D", report.letterGrade());
            assertFalse(report.passed());
        }

        @Test
        void passingScoreBelowSixtyStillGetsFGrade() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "100", false, true, false,
                    0, 0, 0, null, null, 55, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)),
                    pol(50, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(report.passed());
            assertEquals("F", report.letterGrade());
        }

        @Test
        void cleanSubmissionPassesWithIntegrityPolicyEnabled() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, true, false, true));
            assertTrue(report.passed());
        }

        @Test
        void mandatoryUnansweredAloneFailsWhenOtherwisePassing() {
            AnswerResult mandatory = ans(QuestionType.SINGLE_CHOICE, "S1", "10", true, false, false,
                    0, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true), mandatory)),
                    pol(40, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertEquals(50, report.percentage());
            assertFalse(report.passed());
        }

        @Test
        void multipleChoiceWithZeroSelectionsHasNoOnlyIncorrectWarning() {
            AnswerResult answer = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 0, 0, null, null, 0, 0, 0, false, false, false, false);
            GradeReport report = service.grade(sub(List.of(answer)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
            assertTrue(report.warnings().stream().noneMatch(w -> w.startsWith("ONLY_INCORRECT_OPTIONS_SELECTED:")));
        }

        @Test
        void numericErrorExactlyAtToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0.1", "0.1", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void numericZeroErrorAndZeroToleranceGivesFullCredit() {
            AnswerResult answer = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "0", "0", 0, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void programFullPassWithoutPartialCreditStillFull() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 4, 4, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)),
                    pol(60, false, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertBigDecimalEquals("10.00", report.rawPoints());
        }

        @Test
        void instructorOverrideBoundaryValuesAreValid() {
            GradeReport zero = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 0, Map.of()),
                    pol());
            assertEquals(0, zero.percentage());
            GradeReport hundred = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", 100, Map.of()),
                    pol());
            assertEquals(100, hundred.percentage());
        }

        @Test
        void sectionPassPercentageOfHundredIsValid() {
            GradeReport report = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true)),
                    T0, T0.plusSeconds(600), null, 3600, 1, true, true, false, false, false, 0, 0, "0", null,
                    Map.of("S1", 100)), pol());
            assertTrue(report.passed());
        }

        @Test
        void rubricPercentOfHundredIsValid() {
            AnswerResult answer = ans(QuestionType.FILL_BLANK, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 100, 0, 0, false, false, false, false);
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(answer)), pol()).rawPoints());
        }

        @Test
        void policyBoundaryRatesAreValid() {
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0", "0.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "1.00", "0.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "1.00", 10, 50, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 0, 0, 0, true, false, 3, false, false, true)).rawPoints());
            assertBigDecimalEquals("10.00", service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 100, true, false, 3, false, false, true)).rawPoints());
        }

        @Test
        void passPercentageBoundaryValuesAreValid() {
            GradeReport zero = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(0, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(zero.passed());
            GradeReport hundred = service.grade(sub(List.of(ans(QuestionType.SINGLE_CHOICE, "10", true, true))),
                    pol(100, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true));
            assertTrue(hundred.passed());
        }

        // ----- validation of ExamGradingService -----

        @Test
        void gradeRequiresNonNullArguments() {
            assertThrows(NullPointerException.class, () -> service.grade(null, pol()));
            assertThrows(NullPointerException.class, () -> service.grade(sub(List.of()), null));
        }

        @Test
        void validateRejectsBlankIds() {
            ExamSubmission blankExam = sub("", "stu1", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission nullExam = sub(null, "stu1", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission blankStudent = sub("exam1", " ", List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(blankExam, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullExam, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(blankStudent, pol()));
        }

        @Test
        void validateRejectsMissingOrInvertedTimestamps() {
            ExamSubmission noStart = sub(List.of(), null, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission noSubmit = sub(List.of(), T0, null, null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            ExamSubmission inverted = sub(List.of(), T0.plusSeconds(1), T0, null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(noStart, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(noSubmit, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(inverted, pol()));
        }

        @Test
        void validateRejectsInvalidCountersAndBonuses() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, -1, 1, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 0, true, true, false, false, false, 0, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, -1, 0, "0", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, -1, "0", null, Map.of()), pol()));
            ExamSubmission nullBonus = new ExamSubmission("exam1", "stu1", List.of(), T0, T0.plusSeconds(1), null,
                    0, 1, true, true, false, false, false, 0, 0, null, null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullBonus, pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "-1", null, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", -1, Map.of()), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", 101, Map.of()), pol()));
        }

        @Test
        void validatePolicyRejectsOutOfRangeValues() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(-1, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(101, true, false, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            GradingPolicy nullWrong = new GradingPolicy(60, true, false, null, BigDecimal.ZERO, 10, 50, 0,
                    true, false, 3, false, false, true);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()), nullWrong));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "-0.1", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "1.5", "0.00", 10, 50, 0, true, false, 3, false, false, true)));
            GradingPolicy nullUnanswered = new GradingPolicy(60, true, false, new BigDecimal("0.50"), null,
                    10, 50, 0, true, false, 3, false, false, true);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()), nullUnanswered));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "-0.1", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "1.5", 10, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", -1, 50, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, -1, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 101, 0, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, -1, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 101, true, false, 3, false, false, true)));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of()),
                    pol(60, true, false, "0.50", "0.00", 10, 50, 0, true, false, 0, false, false, true)));
        }

        @Test
        void validateAnswerRejectsMalformedAnswers() {
            GradingPolicy policy = pol();
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", null, "S", "10", false, true, true, 0, 0, 0, null, null, 0, 0, 0,
                            false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw(" ", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", null, false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", "0", false, true, true, 0, 0, 0, null, null,
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, -1, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 1, -1, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 1, 0, -1,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.MULTIPLE_CHOICE, "S", "10", false, true, true, 2, 3, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0, null, "-0.5",
                            0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.FILL_BLANK, "S", "10", false, true, true, 0, 0, 0, null, null,
                            -1, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.FILL_BLANK, "S", "10", false, true, true, 0, 0, 0, null, null,
                            101, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, -1, 0, true, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 0, -1, true, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0, null, null,
                            0, 3, 2, true, false, false, false))), policy));
        }

        @Test
        void validateRejectsDuplicateQuestionIds() {
            AnswerResult first = raw("qdup", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, false, false, false, false);
            AnswerResult second = raw("qdup", QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, false, false, false, false);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.grade(sub(List.of(first, second)), pol()));
            assertEquals("questionId must be unique", e.getMessage());
        }

        @Test
        void validateRejectsBadSectionPassPercentages() {
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("", 50)), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("S", -1)), pol()));
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(), T0, T0.plusSeconds(1),
                    null, 0, 1, true, true, false, false, false, 0, 0, "0", null, Map.of("S", 101)), pol()));
        }

        @Test
        void validateRejectsNullStudentId() {
            ExamSubmission nullStudent = sub("exam1", null, List.of(), T0, T0.plusSeconds(1), null, 0, 1,
                    true, true, false, false, false, 0, 0, "0", null, Map.of());
            assertThrows(IllegalArgumentException.class, () -> service.grade(nullStudent, pol()));
        }

        @Test
        void validateRejectsNullQuestionIdAndSection() {
            GradingPolicy policy = pol();
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw(null, QuestionType.NUMERIC, "S", "10", false, true, true, 0, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
            assertThrows(IllegalArgumentException.class, () -> service.grade(
                    sub(List.of(raw("q1", QuestionType.NUMERIC, null, "10", false, true, true, 0, 0, 0,
                            null, null, 0, 0, 0, false, false, false, false))), policy));
        }

        @Test
        void unansweredProgrammingAnswerPassesValidation() {
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, false, false,
                    0, 0, 0, null, null, 0, 0, 0, true, false, false, false);
            GradeReport report = service.grade(sub(List.of(program)), pol());
            assertBigDecimalEquals("0.00", report.rawPoints());
        }

        @Test
        void reflectionExercisesPrivateScoringAndValidationBranches() throws Exception {
            // scoreProgramming: compiled with zero tests and no manual review → 20% credit
            Method scoreProgramming = ExamGradingService.class.getDeclaredMethod("scoreProgramming",
                    AnswerResult.class, BigDecimal.class, GradingPolicy.class, List.class);
            scoreProgramming.setAccessible(true);
            AnswerResult program = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 0, 0, true, false, false, false);
            BigDecimal programScore = (BigDecimal) scoreProgramming.invoke(service, program,
                    new BigDecimal("10"), pol(), new ArrayList<String>());
            assertBigDecimalEquals("2.00", programScore);

            // scoreAnswer: negative partial multiple-choice score is clamped to zero
            Method scoreAnswer = ExamGradingService.class.getDeclaredMethod("scoreAnswer",
                    AnswerResult.class, GradingPolicy.class, List.class);
            scoreAnswer.setAccessible(true);
            AnswerResult onlyIncorrect = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 0, 1, null, null, 0, 0, 0, false, false, false, false);
            BigDecimal clamped = (BigDecimal) scoreAnswer.invoke(service, onlyIncorrect,
                    pol(), new ArrayList<String>());
            assertBigDecimalEquals("0.00", clamped);

            // scoreMultipleChoice: negative marking without partial credit and no wrong selections → zero
            Method scoreMultipleChoice = ExamGradingService.class.getDeclaredMethod("scoreMultipleChoice",
                    AnswerResult.class, BigDecimal.class, GradingPolicy.class, List.class);
            scoreMultipleChoice.setAccessible(true);
            AnswerResult wrong = ans(QuestionType.MULTIPLE_CHOICE, "S1", "10", false, true, false,
                    3, 1, 0, null, null, 0, 0, 0, false, false, false, false);
            BigDecimal zeroed = (BigDecimal) scoreMultipleChoice.invoke(service, wrong, new BigDecimal("10"),
                    pol(60, false, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true),
                    new ArrayList<String>());
            assertBigDecimalEquals("0.00", zeroed);

            // calculateSections: a section whose accumulated maximum is zero yields zero percent
            Method calculateSections = ExamGradingService.class.getDeclaredMethod("calculateSections",
                    Map.class, Map.class, ExamSubmission.class, GradingPolicy.class, List.class);
            calculateSections.setAccessible(true);
            Map<String, Integer> percents = (Map<String, Integer>) calculateSections.invoke(service,
                    Map.of("S", BigDecimal.ZERO), Map.of("S", BigDecimal.ZERO),
                    sub(List.of(ans(QuestionType.SINGLE_CHOICE, "S", "10", false, true, true,
                            0, 0, 0, null, null, 0, 0, 0, false, false, false, false))), pol(),
                    new ArrayList<String>());
            assertEquals(0, percents.get("S"));

            // validate: a section-pass map carrying a null key is rejected
            Method validate = ExamGradingService.class.getDeclaredMethod("validate",
                    ExamSubmission.class, GradingPolicy.class);
            validate.setAccessible(true);
            ExamSubmission mockedSubmission = mock(ExamSubmission.class);
            when(mockedSubmission.examId()).thenReturn("exam1");
            when(mockedSubmission.studentId()).thenReturn("stu1");
            when(mockedSubmission.startedAt()).thenReturn(T0);
            when(mockedSubmission.submittedAt()).thenReturn(T0.plusSeconds(1));
            when(mockedSubmission.attemptNumber()).thenReturn(1);
            when(mockedSubmission.bonusPoints()).thenReturn(BigDecimal.ZERO);
            when(mockedSubmission.answers()).thenReturn(List.of());
            Map<String, Integer> nullKeyMap = new HashMap<>();
            nullKeyMap.put(null, 50);
            when(mockedSubmission.sectionPassPercentages()).thenReturn(nullKeyMap);
            try {
                validate.invoke(service, mockedSubmission, pol());
                fail("expected IllegalArgumentException");
            } catch (InvocationTargetException e) {
                assertTrue(e.getCause() instanceof IllegalArgumentException);
            }
        }

        @Test
        void validateAnswerRequiresTestsOrReviewForCompiledPrograms() {
            GradingPolicy policy = pol();
            AnswerResult invalid = raw("q1", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, true, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service.grade(sub(List.of(invalid)), policy));
            AnswerResult valid = raw("q2", QuestionType.PROGRAMMING, "S", "10", false, true, true, 0, 0, 0,
                    null, null, 0, 0, 0, true, false, false, true);
            assertBigDecimalEquals("0.00", service.grade(sub(List.of(valid)), policy).rawPoints());
        }
    }

    // ------------------------------------------------------------------
    // Grading expression: ExpressionExamGradingService orchestration
    // ------------------------------------------------------------------
    @Nested
    class ExpressionExamGradingServiceTests {

        @Test
        void constructorRequiresBothServices() {
            ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(expression -> BigDecimal.ONE);
            assertThrows(NullPointerException.class,
                    () -> new ExpressionExamGradingService(null, new ExamGradingService()));
            assertThrows(NullPointerException.class,
                    () -> new ExpressionExamGradingService(answerService, null));
        }

        @Test
        void gradeRequiresSubmissionAndPolicy() {
            ExpressionExamGradingService service = new ExpressionExamGradingService(
                    new ExpressionAnswerGradingService(expression -> BigDecimal.ONE), new ExamGradingService());
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu", List.of(),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            assertThrows(NullPointerException.class, () -> service.grade(null, ExamGradingServiceTests.pol()));
            assertThrows(NullPointerException.class, () -> service.grade(submission, null));
        }

        @Test
        void gradeEvaluatesEachAnswerAndBuildsExamGrade() {
            ExpressionAnswerGradingService answerService = new ExpressionAnswerGradingService(expression -> {
                switch (expression) {
                    case "2+2":
                        return new BigDecimal("4");
                    case "2*3":
                        return new BigDecimal("6");
                    case "4":
                        return new BigDecimal("4");
                    default:
                        throw new ExpressionEvaluationException("unexpected: " + expression);
                }
            });
            ExpressionExamGradingService service = new ExpressionExamGradingService(answerService,
                    new ExamGradingService());
            ExpressionQuestion q1 = new ExpressionQuestion("q1", "math", "2+2", BigDecimal.TEN,
                    BigDecimal.ZERO, false);
            ExpressionQuestion q2 = new ExpressionQuestion("q2", "math", "2*3", BigDecimal.TEN,
                    BigDecimal.ZERO, false);
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu",
                    List.of(new ExpressionAnswerSubmission(q1, "2+2"), new ExpressionAnswerSubmission(q2, "4")),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            ExpressionExamGrade grade = service.grade(submission, ExamGradingServiceTests.pol());
            assertEquals(2, grade.answerEvaluations().size());
            assertEquals(ExpressionAnswerStatus.EXACT, grade.answerEvaluations().get(0).status());
            assertEquals(ExpressionAnswerStatus.INCORRECT, grade.answerEvaluations().get(1).status());
            assertEquals("q1", grade.answerEvaluations().get(0).answerResult().questionId());
            assertEquals("q2", grade.answerEvaluations().get(1).answerResult().questionId());
            assertBigDecimalEquals("10.00", grade.report().rawPoints());
            assertEquals(50, grade.report().percentage());
        }

        @Test
        void gradeMarksBlankSubmissionUnanswered() {
            ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(expression -> new BigDecimal("4"));
            ExpressionExamGradingService service = new ExpressionExamGradingService(answerService,
                    new ExamGradingService());
            ExpressionQuestion q1 = new ExpressionQuestion("q1", "math", "2+2", BigDecimal.TEN,
                    BigDecimal.ZERO, true);
            Instant start = Instant.parse("2026-10-01T08:00:00Z");
            ExpressionExamSubmission submission = new ExpressionExamSubmission("exam", "stu",
                    List.of(new ExpressionAnswerSubmission(q1, "")),
                    start, start.plusSeconds(60), null, 3600, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, Map.of());
            ExpressionExamGrade grade = service.grade(submission, ExamGradingServiceTests.pol());
            assertEquals(ExpressionAnswerStatus.UNANSWERED, grade.answerEvaluations().get(0).status());
            assertBigDecimalEquals("0.00", grade.report().rawPoints());
            assertTrue(grade.report().failures().stream().anyMatch(f -> f.startsWith("MANDATORY_QUESTION_UNANSWERED:")));
        }
    }

    // ------------------------------------------------------------------
    // Progress domain records
    // ------------------------------------------------------------------
    @Nested
    class ProgressDomainTests {

        @Test
        void lessonProgressExposesAllComponents() {
            LessonProgress lesson = new LessonProgress("l1", LessonType.QUIZ, true, 2, 80, 90, 1, 3,
                    true, false, true, 45, 60, false);
            assertEquals("l1", lesson.lessonId());
            assertEquals(LessonType.QUIZ, lesson.type());
            assertTrue(lesson.mandatory());
            assertEquals(2, lesson.order());
            assertEquals(80, lesson.completionPercent());
            assertEquals(90, lesson.score());
            assertEquals(1, lesson.attempts());
            assertEquals(3, lesson.maximumAttempts());
            assertTrue(lesson.submitted());
            assertFalse(lesson.approved());
            assertTrue(lesson.plagiarismFlag());
            assertEquals(45, lesson.attendedMinutes());
            assertEquals(60, lesson.requiredMinutes());
            assertFalse(lesson.unlocked());
        }

        @Test
        void lessonTypeEnumeratesSixKinds() {
            assertEquals(6, LessonType.values().length);
        }

        @Test
        void courseProgressSnapshotDefendsAgainstNullLessons() {
            CourseProgressSnapshot snapshot = new CourseProgressSnapshot("s1", "c1", null, false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertEquals(List.of(), snapshot.lessons());
            assertEquals("s1", snapshot.studentId());
            assertEquals("c1", snapshot.courseId());
            assertTrue(snapshot.coursePublished());
        }

        @Test
        void progressEvaluationDefendsAgainstNullCollections() {
            ProgressEvaluation empty = new ProgressEvaluation(50, 1, 2, false, false, null, null, null);
            assertEquals(List.of(), empty.blockers());
            assertEquals(List.of(), empty.warnings());
            ProgressEvaluation full = new ProgressEvaluation(50, 1, 2, false, false, "next",
                    List.of("b1"), List.of("w1"));
            assertEquals(50, full.overallPercent());
            assertEquals(1, full.completedLessons());
            assertEquals(2, full.totalLessons());
            assertFalse(full.courseCompleted());
            assertFalse(full.certificateEligible());
            assertEquals("next", full.nextLessonId());
            assertEquals(List.of("b1"), full.blockers());
            assertEquals(List.of("w1"), full.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Progress service: LearningProgressService
    // ------------------------------------------------------------------
    @Nested
    class LearningProgressServiceTests {

        private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

        private static LearningProgressService service() {
            return new LearningProgressService(Clock.fixed(NOW, ZoneOffset.UTC));
        }

        private static LessonProgress lesson(String id, LessonType type, boolean mandatory, int order,
                                             int completion, int score, int attempts, int maxAttempts,
                                             boolean submitted, boolean approved, boolean plag,
                                             int attended, int required, boolean unlocked) {
            return new LessonProgress(id, type, mandatory, order, completion, score, attempts, maxAttempts,
                    submitted, approved, plag, attended, required, unlocked);
        }

        private static LessonProgress lesson(String id, LessonType type, boolean mandatory, int order,
                                             boolean complete, boolean unlocked) {
            int completion = complete ? 100 : 0;
            int score = complete ? 100 : 0;
            boolean submitted = complete;
            boolean approved = complete;
            int attended = complete ? 60 : 0;
            int required = type == LessonType.LIVE_SESSION ? 60 : 0;
            return lesson(id, type, mandatory, order, completion, score, 0, 0, submitted, approved,
                    false, attended, required, unlocked);
        }

        private static CourseProgressSnapshot snap(List<LessonProgress> lessons, int requiredOverall,
                                                   int requiredQuiz, boolean sequential, boolean identityVerified,
                                                   boolean codeOfConduct, boolean subscriptionActive,
                                                   boolean suspended, boolean published, Instant enrolledAt,
                                                   Instant expiresAt, boolean certificateEnabled,
                                                   String certificateName, boolean finalProjectRequired,
                                                   boolean outstandingBalance,
                                                   boolean instructorApprovalRequired) {
            return new CourseProgressSnapshot("s1", "c1", lessons, sequential, requiredOverall, requiredQuiz,
                    identityVerified, codeOfConduct, subscriptionActive, suspended, published, enrolledAt,
                    expiresAt, certificateEnabled, certificateName, finalProjectRequired, outstandingBalance,
                    instructorApprovalRequired);
        }

        private static CourseProgressSnapshot snap(List<LessonProgress> lessons) {
            return snap(lessons, 0, 0, false, true, true, true, false, true, null, null, false, null,
                    false, false, false);
        }

        @Test
        void completedCourseWithSingleVideo() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true))));
            assertEquals(100, evaluation.overallPercent());
            assertEquals(1, evaluation.completedLessons());
            assertEquals(1, evaluation.totalLessons());
            assertTrue(evaluation.courseCompleted());
            assertFalse(evaluation.certificateEligible());
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.blockers().isEmpty());
            assertEquals(List.of("CERTIFICATE_DISABLED"), evaluation.warnings());
        }

        @Test
        void nearCompletionVideoWarnsAndReportsNextLesson() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 75, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertEquals(73, evaluation.overallPercent());
            assertEquals(0, evaluation.completedLessons());
            assertTrue(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
            assertEquals("v1", evaluation.nextLessonId());
            assertTrue(evaluation.courseCompleted());
            assertTrue(evaluation.warnings().contains("COMPLETION_STATE_CONFLICT"));
        }

        @Test
        void articleEarnsWeightOnlyWhenComplete() {
            LessonProgress complete = lesson("a1", LessonType.ARTICLE, false, 0, true, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(complete)));
            assertEquals(100, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress partial = lesson("a2", LessonType.ARTICLE, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation half = service().evaluate(snap(List.of(partial)));
            assertEquals(50, half.overallPercent());
            assertEquals("a2", half.nextLessonId());
            assertFalse(half.warnings().contains("VIDEO_NEAR_COMPLETION:a2"));
        }

        @Test
        void quizRequiresSubmissionAndRequiredScore() {
            LessonProgress passed = lesson("q1", LessonType.QUIZ, false, 0, 100, 80, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(passed), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(done.courseCompleted());

            LessonProgress low = lesson("q2", LessonType.QUIZ, false, 0, 100, 50, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation retry = service().evaluate(snap(List.of(low), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(retry.warnings().contains("QUIZ_SCORE_TOO_LOW:q2"));
            assertEquals(50, retry.overallPercent());

            LessonProgress untouched = lesson("q3", LessonType.QUIZ, false, 0, 100, 80, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation none = service().evaluate(snap(List.of(untouched), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(0, none.overallPercent());
            assertEquals("q3", none.nextLessonId());
        }

        @Test
        void assignmentEarnsWeightPerState() {
            LessonProgress approved = lesson("a1", LessonType.ASSIGNMENT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved)));
            assertEquals(100, done.overallPercent());
            assertEquals(1, done.completedLessons());
            assertTrue(done.courseCompleted());

            LessonProgress pending = lesson("a2", LessonType.ASSIGNMENT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending)));
            assertEquals(48, awaiting.overallPercent());
            assertTrue(awaiting.warnings().contains("ASSIGNMENT_AWAITING_REVISION:a2"));

            LessonProgress untouched = lesson("a3", LessonType.ASSIGNMENT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation none = service().evaluate(snap(List.of(untouched)));
            assertEquals(0, none.overallPercent());
        }

        @Test
        void liveSessionRequiresAttendanceThreshold() {
            LessonProgress enough = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 80, 100, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(enough)));
            assertEquals(80, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress shortAttendance = lesson("l2", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 79, 100, true);
            ProgressEvaluation low = service().evaluate(snap(List.of(shortAttendance)));
            assertTrue(low.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l2"));
            assertEquals(75, low.overallPercent());

            LessonProgress zeroRequiredAttended = lesson("l3", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation absent = service().evaluate(snap(List.of(zeroRequiredAttended)));
            assertEquals(0, absent.overallPercent());
            assertEquals(0, absent.completedLessons());

            LessonProgress zeroRequiredPresent = lesson("l4", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 1, 0, true);
            ProgressEvaluation present = service().evaluate(snap(List.of(zeroRequiredPresent)));
            assertEquals(100, present.overallPercent());

            LessonProgress overAttended = lesson("l5", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 120, 100, true);
            ProgressEvaluation capped = service().evaluate(snap(List.of(overAttended)));
            assertEquals(100, capped.overallPercent());
        }

        @Test
        void projectRequiresSubmissionApprovalAndIntegrity() {
            LessonProgress approved = lesson("p1", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved)));
            assertEquals(100, done.overallPercent());
            assertTrue(done.courseCompleted());

            LessonProgress pending = lesson("p2", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending)));
            assertEquals(33, awaiting.overallPercent());

            LessonProgress plag = lesson("p3", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, true, 0, 0, true);
            ProgressEvaluation flagged = service().evaluate(snap(List.of(plag)));
            assertTrue(flagged.warnings().contains("PROJECT_HELD_FOR_INTEGRITY_REVIEW:p3"));
            assertTrue(flagged.blockers().contains("PLAGIARISM_REVIEW:p3"));
            assertFalse(flagged.courseCompleted());
        }

        @Test
        void mandatoryIncompleteLessonBlocksCompletion() {
            LessonProgress mandatory = lesson("v1", LessonType.VIDEO, true, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(mandatory)));
            assertTrue(evaluation.blockers().contains("MANDATORY_LESSON_INCOMPLETE:v1"));
            assertFalse(evaluation.courseCompleted());
            assertEquals("v1", evaluation.nextLessonId());
        }

        @Test
        void attemptLimitReachedBlocksCompletion() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 30, 3, 3,
                    true, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("ATTEMPT_LIMIT_REACHED:q1"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void attemptLimitOnlyBlocksWhenAtLimitAndIncomplete() {
            LessonProgress noCap = lesson("a1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress underCap = lesson("a2", LessonType.VIDEO, false, 1, 50, 0, 2, 5,
                    false, false, false, 0, 0, true);
            LessonProgress atCapButComplete = lesson("a3", LessonType.VIDEO, false, 2, 100, 0, 3, 3,
                    true, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(noCap, underCap, atCapButComplete)));
            assertTrue(evaluation.blockers().stream().noneMatch(b -> b.startsWith("ATTEMPT_LIMIT_REACHED")));
        }

        @Test
        void lockedLessonWithProgressWarnsAndIsNotNextLesson() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked)));
            assertTrue(evaluation.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:v1"));
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void sequentialLearningRequiresPreviousCompletion() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, true, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("MANDATORY_LESSON_INCOMPLETE:v1"));
            assertTrue(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));
            assertEquals("v1", evaluation.nextLessonId());
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void sequentialLearningAllowsHalfCompletedNonMandatoryLessons() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, false, 0, 60, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));

            LessonProgress low = lesson("v3", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress follow = lesson("v4", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation blocked = service().evaluate(snap(List.of(low, follow), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(blocked.warnings().contains("SEQUENCE_RULE_BYPASSED:v4"));
        }

        @Test
        void lessonsAreSortedByOrderIgnoringInputOrder() {
            LessonProgress later = lesson("late", LessonType.VIDEO, true, 1, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress earlier = lesson("early", LessonType.VIDEO, false, 0, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(later, earlier), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:early"));
            assertEquals("late", evaluation.nextLessonId());
        }

        @Test
        void sequentialLockedChainLeavesNoAccessibleNextLesson() {
            LessonProgress lockedFirst = lesson("l1", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, false);
            LessonProgress openSecond = lesson("l2", LessonType.VIDEO, false, 1, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lockedFirst, openSecond), 80, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void sequentialLockedLessonIsNotReportedAsBypassed() {
            LessonProgress first = lesson("l1", LessonType.VIDEO, false, 0, 40, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress locked = lesson("l2", LessonType.VIDEO, false, 1, 40, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, locked), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
            assertEquals("l1", evaluation.nextLessonId());
        }

        @Test
        void emptyCourseAddsBlockerAndIsNotCompleted() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of()));
            assertTrue(evaluation.blockers().contains("COURSE_HAS_NO_LESSONS"));
            assertEquals(0, evaluation.overallPercent());
            assertFalse(evaluation.courseCompleted());
            assertNull(evaluation.nextLessonId());
            assertFalse(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
        }

        @Test
        void overallBelowRequiredBlocksCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video), 80, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.blockers().contains("OVERALL_PROGRESS_BELOW_REQUIRED"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void noAccessibleNextLessonWarnsWhenIncompleteLockedCourse() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked), 80, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertTrue(evaluation.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        void finalProjectRequiredBlocksUntilProjectApproved() {
            ProgressEvaluation without = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 0, 0, false, true, true, true, false, true, null, null,
                    false, null, true, false, false));
            assertTrue(without.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
            assertFalse(without.courseCompleted());

            LessonProgress project = lesson("p1", LessonType.PROJECT, false, 1, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation with = service().evaluate(snap(List.of(project), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertFalse(with.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
            assertTrue(with.courseCompleted());
        }

        @Test
        void finalProjectGateTracksSubmissionAndApprovalStates() {
            LessonProgress untouched = lesson("p1", LessonType.PROJECT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation unsubmitted = service().evaluate(snap(List.of(untouched), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(unsubmitted.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress pending = lesson("p2", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation awaiting = service().evaluate(snap(List.of(pending), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(awaiting.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress approved = lesson("p3", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(approved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertFalse(done.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            LessonProgress plagiarized = lesson("p4", LessonType.PROJECT, false, 0, 100, 0, 1, 0,
                    true, true, true, 0, 0, true);
            ProgressEvaluation held = service().evaluate(snap(List.of(plagiarized), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, true, false, false));
            assertTrue(held.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
        }

        @Test
        void unsubmittedProjectEarnsNothing() {
            LessonProgress untouched = lesson("p1", LessonType.PROJECT, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(untouched)));
            assertEquals(0, evaluation.overallPercent());
            assertEquals("p1", evaluation.nextLessonId());
            assertTrue(evaluation.warnings().stream().noneMatch(w -> w.startsWith("PROJECT_HELD")));
        }

        @Test
        void enrollmentInFutureBlocksAccess() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, true, true);
            ProgressEvaluation future = service().evaluate(snap(List.of(video), 0, 0, false, true,
                    true, true, false, true, NOW.plusSeconds(3600), null, false, null, false, false, false));
            assertTrue(future.blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
            assertTrue(future.courseCompleted());
            ProgressEvaluation past = service().evaluate(snap(List.of(video), 0, 0, false, true,
                    true, true, false, true, NOW.minusSeconds(3600), null, false, null, false, false, false));
            assertFalse(past.blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
        }

        @Test
        void instructorApprovalRequiredChecksMandatoryAssignments() {
            LessonProgress approved = lesson("a1", LessonType.ASSIGNMENT, true, 0, 100, 0, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation ok = service().evaluate(snap(List.of(approved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertFalse(ok.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            LessonProgress unapproved = lesson("a2", LessonType.ASSIGNMENT, true, 0, 100, 0, 1, 0,
                    true, false, false, 0, 0, true);
            ProgressEvaluation blocked = service().evaluate(snap(List.of(unapproved), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertTrue(blocked.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
            assertFalse(blocked.courseCompleted());

            ProgressEvaluation vacuous = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 0, 0, false, true, true, true, false, true, null, null,
                    false, null, false, false, true));
            assertFalse(vacuous.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            LessonProgress mandatoryVideo = lesson("v2", LessonType.VIDEO, true, 0, true, true);
            ProgressEvaluation videoOk = service().evaluate(snap(List.of(mandatoryVideo), 0, 0, false, true,
                    true, true, false, true, null, null, false, null, false, false, true));
            assertFalse(videoOk.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
        }

        @Test
        void accountAccessBlockers() {
            CourseProgressSnapshot unpublished = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, false, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(unpublished).blockers().contains("COURSE_NOT_PUBLISHED"));

            CourseProgressSnapshot suspended = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, true, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(suspended).blockers().contains("ACCOUNT_SUSPENDED"));

            CourseProgressSnapshot inactive = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, false, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(inactive).blockers().contains("SUBSCRIPTION_INACTIVE"));

            CourseProgressSnapshot noConduct = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, false, true, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(noConduct).blockers().contains("CODE_OF_CONDUCT_NOT_ACCEPTED"));
        }

        @Test
        void enrollmentInFutureBlocks() {
            CourseProgressSnapshot future = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, NOW.plusSeconds(60), null, false, null,
                    false, false, false);
            assertTrue(service().evaluate(future).blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));
        }

        @Test
        void accessExpiryBlocksAndWarnsNearExpiry() {
            CourseProgressSnapshot expired = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.minusSeconds(1), false, null,
                    false, false, false);
            assertTrue(service().evaluate(expired).blockers().contains("COURSE_ACCESS_EXPIRED"));

            CourseProgressSnapshot soon = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.plusSeconds(7L * 86400), false,
                    null, false, false, false);
            ProgressEvaluation soonEval = service().evaluate(soon);
            assertTrue(soonEval.warnings().contains("COURSE_ACCESS_EXPIRES_SOON"));
            assertFalse(soonEval.blockers().contains("COURSE_ACCESS_EXPIRED"));

            CourseProgressSnapshot later = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, NOW.plusSeconds(8L * 86400), false,
                    null, false, false, false);
            ProgressEvaluation laterEval = service().evaluate(later);
            assertFalse(laterEval.warnings().contains("COURSE_ACCESS_EXPIRES_SOON"));
        }

        @Test
        void identityAndBalanceWarnings() {
            CourseProgressSnapshot noIdentity = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, false, true, true, false, true, null, null, false, null, false, false, false);
            assertTrue(service().evaluate(noIdentity).warnings().contains("IDENTITY_NOT_VERIFIED"));

            CourseProgressSnapshot balance = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, false, null, false, true, false);
            assertTrue(service().evaluate(balance).warnings().contains("OUTSTANDING_BALANCE"));
        }

        @Test
        void certificateDisabledWarnsAndBlocksEligibility() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true))));
            assertTrue(evaluation.warnings().contains("CERTIFICATE_DISABLED"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateRequiresCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video), 80, 0, false, true,
                    true, true, false, true, null, null, true, "AB", false, false, false));
            assertFalse(evaluation.certificateEligible());
            assertTrue(evaluation.blockers().stream().noneMatch(b -> b.startsWith("CERTIFICATE_")));
        }

        @Test
        void certificateRequiresIdentityVerification() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, false, true, true, false, true, null, null, true, "AB", false, false, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.blockers().contains("CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateBlockedByBalance() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "AB", false, true, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.blockers().contains("CERTIFICATE_BLOCKED_BY_BALANCE"));
            assertFalse(evaluation.certificateEligible());
        }

        @Test
        void certificateRequiresName() {
            CourseProgressSnapshot noName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, null, false, false, false);
            assertTrue(service().evaluate(noName).blockers().contains("CERTIFICATE_NAME_REQUIRED"));
            CourseProgressSnapshot blankName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "  ", false, false, false);
            assertTrue(service().evaluate(blankName).blockers().contains("CERTIFICATE_NAME_REQUIRED"));
        }

        @Test
        void certificateNameLengthMustBeWithinBounds() {
            CourseProgressSnapshot shortName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "A", false, false, false);
            assertTrue(service().evaluate(shortName).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));

            CourseProgressSnapshot longName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(81), false, false, false);
            assertTrue(service().evaluate(longName).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));
        }

        @Test
        void certificateEligibleHappyPath() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "AB", false, false, false);
            ProgressEvaluation evaluation = service().evaluate(snapshot);
            assertTrue(evaluation.certificateEligible());
            assertTrue(evaluation.blockers().isEmpty());
        }

        @Test
        void videoAtExactlyNinetyPercentCompletesWithoutWarning() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 90, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertEquals(1, evaluation.completedLessons());
            assertFalse(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
        }

        @Test
        void videoAtSeventyPercentWarnsAboutNearCompletion() {
            LessonProgress video = lesson("v1", LessonType.VIDEO, false, 0, 70, 0, 0, 0,
                    false, false, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(video)));
            assertTrue(evaluation.warnings().contains("VIDEO_NEAR_COMPLETION:v1"));
        }

        @Test
        void articleCompleteCountsAndHasNoNextLesson() {
            LessonProgress complete = lesson("a1", LessonType.ARTICLE, false, 0, true, true);
            ProgressEvaluation done = service().evaluate(snap(List.of(complete)));
            assertEquals(1, done.completedLessons());
            assertNull(done.nextLessonId());
        }

        @Test
        void quizScoreExactlyAtRequiredPasses() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 60, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 60, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(1, evaluation.completedLessons());
            assertFalse(evaluation.warnings().contains("QUIZ_SCORE_TOO_LOW:q1"));
        }

        @Test
        void requiredQuizScoreOfHundredIsValid() {
            LessonProgress quiz = lesson("q1", LessonType.QUIZ, false, 0, 100, 100, 1, 0,
                    true, true, false, 0, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(quiz), 0, 100, false, true,
                    true, true, false, true, null, null, false, null, false, false, false));
            assertEquals(1, evaluation.completedLessons());
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void liveSessionZeroRequiredWithAttendanceCompletes() {
            LessonProgress present = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 1, 0, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(present)));
            assertEquals(1, evaluation.completedLessons());
        }

        @Test
        void liveSessionAtThresholdCompletes() {
            LessonProgress enough = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 80, 100, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(enough)));
            assertEquals(1, evaluation.completedLessons());
        }

        @Test
        void liveSessionWithoutAttendanceHasNoWarning() {
            LessonProgress absent = lesson("l1", LessonType.LIVE_SESSION, false, 0, 100, 0, 0, 0,
                    true, true, false, 0, 100, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(absent)));
            assertFalse(evaluation.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l1"));
        }

        @Test
        void lockedLessonWithZeroProgressHasNoWarning() {
            LessonProgress locked = lesson("v1", LessonType.VIDEO, false, 0, 0, 0, 0, 0,
                    false, false, false, 0, 0, false);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(locked)));
            assertFalse(evaluation.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:v1"));
        }

        @Test
        void sequentialHalfCompletedNonMandatoryLessonAllowsNext() {
            LessonProgress first = lesson("v1", LessonType.VIDEO, false, 0, 50, 0, 0, 0,
                    false, false, false, 0, 0, true);
            LessonProgress second = lesson("v2", LessonType.VIDEO, false, 1, true, true);
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(first, second), 0, 0,
                    true, true, true, true, false, true, null, null, false, null, false, false, false));
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:v2"));
        }

        @Test
        void overallExactlyAtRequiredIsNotBlocked() {
            ProgressEvaluation evaluation = service().evaluate(snap(List.of(lesson("v1", LessonType.VIDEO,
                    false, 0, true, true)), 100, 0, false, true, true, true, false, true, null, null,
                    false, null, false, false, false));
            assertFalse(evaluation.blockers().contains("OVERALL_PROGRESS_BELOW_REQUIRED"));
            assertTrue(evaluation.courseCompleted());
        }

        @Test
        void certificateNameOfEightyCharsIsValid() {
            CourseProgressSnapshot snapshot = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(80), false, false, false);
            assertTrue(service().evaluate(snapshot).certificateEligible());
        }

        @Test
        void certificateBlockingBranchesReturnFalseEligibility() {
            CourseProgressSnapshot noName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, null, false, false, false);
            assertFalse(service().evaluate(noName).certificateEligible());
            CourseProgressSnapshot shortName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true, "A", false, false, false);
            assertFalse(service().evaluate(shortName).certificateEligible());
            CourseProgressSnapshot longName = snap(List.of(lesson("v1", LessonType.VIDEO, false, 0, true, true)),
                    0, 0, false, true, true, true, false, true, null, null, true,
                    "N".repeat(81), false, false, false);
            assertFalse(service().evaluate(longName).certificateEligible());
        }

        // ----- validation of LearningProgressService -----

        @Test
        void evaluateRequiresSnapshot() {
            assertThrows(NullPointerException.class, () -> service().evaluate(null));
        }

        @Test
        void validateRejectsNullStudentCourseAndLessonIds() {
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot(null, "c1", List.of(), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot("s1", null, List.of(), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(
                    new CourseProgressSnapshot("s1", "c1", List.of(
                            new LessonProgress(null, LessonType.VIDEO, false, 0, 0, 0, 0, 0,
                                    false, false, false, 0, 0, true)), false, 0, 0, true, true, true,
                            false, true, null, null, false, null, false, false, false)));
        }

        @Test
        void reflectionExercisesPrivateAcademicBlockerCheck() throws Exception {
            Method noAcademicBlocker = LearningProgressService.class.getDeclaredMethod("noAcademicBlocker", List.class);
            noAcademicBlocker.setAccessible(true);
            assertFalse((Boolean) noAcademicBlocker.invoke(service(), List.of("INSTRUCTOR_APPROVAL_REQUIRED")));
            assertTrue((Boolean) noAcademicBlocker.invoke(service(), List.of("OVERALL_PROGRESS_BELOW_REQUIRED")));
        }

        @Test
        void validateRejectsBlankStudentAndCourse() {
            CourseProgressSnapshot blankStudent = new CourseProgressSnapshot("", "c1", List.of(), false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(blankStudent));
            CourseProgressSnapshot blankCourse = new CourseProgressSnapshot("s1", " ", List.of(), false, 0, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(blankCourse));
        }

        @Test
        void validateRejectsOutOfRangeRequirements() {
            CourseProgressSnapshot lowOverall = new CourseProgressSnapshot("s1", "c1", List.of(), false, -1, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(lowOverall));
            CourseProgressSnapshot highOverall = new CourseProgressSnapshot("s1", "c1", List.of(), false, 101, 0,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(highOverall));
            CourseProgressSnapshot lowQuiz = new CourseProgressSnapshot("s1", "c1", List.of(), false, 0, -1,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(lowQuiz));
            CourseProgressSnapshot highQuiz = new CourseProgressSnapshot("s1", "c1", List.of(), false, 0, 101,
                    true, true, true, false, true, null, null, false, null, false, false, false);
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(highQuiz));
        }

        @Test
        void validateRejectsMalformedLessons() {
            List<LessonProgress> nullElement = new ArrayList<>();
            nullElement.add(null);
            assertThrows(NullPointerException.class, () -> new CourseProgressSnapshot("s1", "c1", nullElement,
                    false, 0, 0, true, true, true, false, true, null, null, false, null, false, false, false));

            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("", LessonType.VIDEO, false, 0, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, true, true),
                    lesson("v1", LessonType.VIDEO, false, 1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 1, true, true),
                    lesson("v2", LessonType.VIDEO, false, 1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", null, false, 0, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, -1, true, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, -1, 0, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 101, 0, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, -1, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 101, 0, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, -1, 0, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, -1, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 4, 3, false, false, false, 0, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0, false, false, false, -1, 0, true)))));
            assertThrows(IllegalArgumentException.class, () -> service().evaluate(snap(List.of(
                    lesson("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0, false, false, false, 0, -1, true)))));
        }
    }

    // ------------------------------------------------------------------
    // Progress service: ExpressionExamProgressService
    // ------------------------------------------------------------------
    @Nested
    class ExpressionExamProgressServiceTests {

        private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

        private static CourseProgressSnapshot progressSnapshot(List<LessonProgress> lessons) {
            return new CourseProgressSnapshot("s1", "c1", lessons, false, 0, 0, true, true, true, false, true,
                    null, null, false, null, false, false, false);
        }

        private static ExpressionExamGrade examGrade(boolean manualReview, int percentage, List<String> failures) {
            GradeReport report = new GradeReport(new BigDecimal("9.00"), new BigDecimal("9.00"),
                    new BigDecimal("10"), percentage, "A-", true, manualReview, Map.of(), failures, List.of());
            return new ExpressionExamGrade(report, List.of());
        }

        private static ExpressionExamProgressService service(LearningProgressService progressService) {
            return new ExpressionExamProgressService(progressService);
        }

        @Test
        void constructorRequiresProgressService() {
            assertThrows(NullPointerException.class, () -> new ExpressionExamProgressService(null));
        }

        @Test
        void evaluateRequiresNonNullArguments() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of());
            ExpressionExamGrade grade = examGrade(false, 50, List.of());
            assertThrows(NullPointerException.class, () -> service.evaluate(null, "q", grade));
            assertThrows(NullPointerException.class, () -> service.evaluate(snapshot, "q", null));
            assertThrows(IllegalArgumentException.class, () -> service.evaluate(snapshot, null, grade));
            assertThrows(IllegalArgumentException.class, () -> service.evaluate(snapshot, " ", grade));
        }

        @Test
        void evaluateRejectsManualReviewPendingGrades() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of());
            ExpressionExamGrade grade = examGrade(true, 50, List.of());
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot, "q", grade));
            assertEquals("exam grade is pending manual review", e.getMessage());
        }

        @Test
        void evaluateRejectsMissingQuizLesson() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of(
                    new LessonProgress("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0, true, true, false,
                            60, 0, true)));
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot, "missing", examGrade(false, 50, List.of())));
            assertEquals("quiz lesson not found: missing", e.getMessage());
        }

        @Test
        void evaluateRejectsNonQuizLessonType() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of(
                    new LessonProgress("a1", LessonType.ARTICLE, false, 0, 100, 0, 0, 0, true, true, false,
                            0, 0, true)));
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot, "a1", examGrade(false, 50, List.of())));
            assertEquals("lesson is not a quiz: a1", e.getMessage());
        }

        @Test
        void evaluateRejectsDuplicatedQuizIds() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            LessonProgress quiz = new LessonProgress("q", LessonType.QUIZ, false, 0, 30, 30, 1, 5,
                    false, false, false, 0, 0, true);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of(quiz, new LessonProgress("q",
                    LessonType.QUIZ, false, 1, 30, 30, 1, 5, false, false, false, 0, 0, true)));
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot, "q", examGrade(false, 50, List.of())));
            assertEquals("quizLessonId is duplicated: q", e.getMessage());
        }

        @Test
        void evaluateUpdatesQuizLessonAndDelegatesToProgressService() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            LessonProgress first = new LessonProgress("v1", LessonType.VIDEO, false, 0, 100, 0, 0, 0,
                    true, true, false, 60, 0, true);
            LessonProgress quiz = new LessonProgress("q", LessonType.QUIZ, false, 1, 30, 30, 1, 5,
                    false, false, false, 0, 0, true);
            LessonProgress last = new LessonProgress("v2", LessonType.VIDEO, false, 2, 100, 0, 0, 0,
                    true, true, false, 60, 0, true);
            CourseProgressSnapshot snapshot = progressSnapshot(List.of(first, quiz, last));
            ProgressEvaluation expected = new ProgressEvaluation(100, 3, 3, true, false, null,
                    List.of(), List.of("CERTIFICATE_DISABLED"));
            when(progress.evaluate(any())).thenReturn(expected);
            ProgressEvaluation actual = service.evaluate(snapshot, "q", examGrade(false, 90, List.of()));
            assertEquals(expected, actual);
            ArgumentCaptor<CourseProgressSnapshot> captor = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress).evaluate(captor.capture());
            CourseProgressSnapshot updated = captor.getValue();
            assertEquals(3, updated.lessons().size());
            assertEquals("v1", updated.lessons().get(0).lessonId());
            LessonProgress updatedQuiz = updated.lessons().get(1);
            assertEquals("q", updatedQuiz.lessonId());
            assertEquals(100, updatedQuiz.completionPercent());
            assertEquals(90, updatedQuiz.score());
            assertEquals(2, updatedQuiz.attempts());
            assertEquals(5, updatedQuiz.maximumAttempts());
            assertTrue(updatedQuiz.submitted());
            assertFalse(updatedQuiz.approved());
            assertFalse(updatedQuiz.plagiarismFlag());
            assertEquals("v2", updated.lessons().get(2).lessonId());
            assertEquals("s1", updated.studentId());
            assertEquals("c1", updated.courseId());
        }

        @Test
        void evaluateClampsPercentageToBounds() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            LessonProgress quiz = new LessonProgress("q", LessonType.QUIZ, false, 0, 30, 30, 1, 5,
                    false, false, false, 0, 0, true);
            when(progress.evaluate(any())).thenReturn(new ProgressEvaluation(0, 0, 0, false, false,
                    null, List.of(), List.of()));
            service.evaluate(progressSnapshot(List.of(quiz)), "q", examGrade(false, 150, List.of()));
            ArgumentCaptor<CourseProgressSnapshot> captor = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress, times(1)).evaluate(captor.capture());
            assertEquals(100, captor.getValue().lessons().get(0).score());

            service.evaluate(progressSnapshot(List.of(quiz)), "q", examGrade(false, -10, List.of()));
            ArgumentCaptor<CourseProgressSnapshot> second = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress, times(2)).evaluate(second.capture());
            assertEquals(0, second.getValue().lessons().get(0).score());
        }

        @Test
        void evaluateMergesIntegrityFailuresIntoPlagiarismFlag() {
            LearningProgressService progress = mock(LearningProgressService.class);
            ExpressionExamProgressService service = service(progress);
            LessonProgress quiz = new LessonProgress("q", LessonType.QUIZ, false, 0, 30, 30, 1, 5,
                    false, false, false, 0, 0, true);
            when(progress.evaluate(any())).thenReturn(new ProgressEvaluation(0, 0, 0, false, false,
                    null, List.of(), List.of()));

            service.evaluate(progressSnapshot(List.of(quiz)), "q",
                    examGrade(false, 60, List.of("ANSWER_INTEGRITY_VIOLATION:q")));
            ArgumentCaptor<CourseProgressSnapshot> captor = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress, times(1)).evaluate(captor.capture());
            assertTrue(captor.getValue().lessons().get(0).plagiarismFlag());

            service.evaluate(progressSnapshot(List.of(quiz)), "q", examGrade(false, 60, List.of("OTHER_FAILURE")));
            ArgumentCaptor<CourseProgressSnapshot> second = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress, times(2)).evaluate(second.capture());
            assertFalse(second.getValue().lessons().get(0).plagiarismFlag());

            LessonProgress alreadyFlagged = new LessonProgress("q", LessonType.QUIZ, false, 0, 30, 30, 1, 5,
                    false, false, true, 0, 0, true);
            service.evaluate(progressSnapshot(List.of(alreadyFlagged)), "q", examGrade(false, 60, List.of()));
            ArgumentCaptor<CourseProgressSnapshot> third = ArgumentCaptor.forClass(CourseProgressSnapshot.class);
            verify(progress, times(3)).evaluate(third.capture());
            assertTrue(third.getValue().lessons().get(0).plagiarismFlag());
        }
    }
}
