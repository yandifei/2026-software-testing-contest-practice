package net.mooctest;
import net.mooctest.pricing.domain.*;
import net.mooctest.pricing.service.*;
import net.mooctest.refund.domain.*;
import net.mooctest.refund.service.*;
import net.mooctest.enrollment.domain.*;
import net.mooctest.enrollment.service.*;
import net.mooctest.enrollment.port.*;
import net.mooctest.enrollment.adapter.*;
import net.mooctest.scheduling.domain.*;
import net.mooctest.scheduling.service.*;
import java.time.LocalDate;
import java.util.Set;


import net.mooctest.expression.engine.AbnormalOperation;
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
import net.mooctest.progress.service.LearningProgressService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import top.lingyuzhao.varFormatter.core.VarFormatter;
import top.lingyuzhao.varFormatter.utils.DataObj;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.fail;

/** Regression tests for mathematical assessment and education workflows. */
class StudentTest {

    @BeforeEach
    void resetEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
        // Isolate the static component registry between tests so every fresh-name
        // getInstance call deterministically exercises its registration path.
        try {
            Field registry = CalculationManagement.class.getDeclaredField("STRING_CALCULATION_HASH_MAP");
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
        void sumOfRangeStringAdjustsEndWhenStepDoesNotDivideRange() {
            assertEquals("3.0*2.0+3.0*(3.0-1)*3.0/2", NumberUtils.sumOfRangeString(2, 8, 3));
        }

        @Test
        void multiplyOfRangeWithEqualBoundsReturnsStartImmediately() throws Throwable {
            // Bounded loops run in a daemon thread: a mutated zero/negative step must
            // surface as a deterministic assertion failure, never as a hung fork.
            FutureTask<Void> task = new FutureTask<>(() -> {
                assertEquals(3.0, NumberUtils.MultiplyOfRange(3, 3, 0));
                assertEquals("3.0", NumberUtils.MultiplyOfRangeString(3, 3, 0));
                assertEquals(280.0, NumberUtils.MultiplyOfRange(1, 10, 3));
                assertEquals("280.0", NumberUtils.MultiplyOfRangeString(1, 10, 3));
                assertEquals("((0.0*0.0))+((1.0*1.0))+((2.0*2.0))",
                        CumulativeCalculation.getInstance("cumulativeTimeoutInstance").formatStr("n[0,2,1](n*n)"));
            }, null);
            Thread thread = new Thread(task, "range-product-bounds");
            thread.setDaemon(true);
            thread.start();
            try {
                task.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                throw new AssertionError("finite range product did not return within one second", timeout);
            } catch (ExecutionException failure) {
                throw failure.getCause();
            } finally {
                task.cancel(true);
            }
        }

        @Test
        void multiplyOfRangeReachesTheFinalTermOnTheTestThread() {
            assertEquals(280.0, NumberUtils.MultiplyOfRange(1, 10, 3));
            assertEquals("280.0", NumberUtils.MultiplyOfRangeString(1, 10, 3));
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
    class ManagementAndResultsTests {



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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------


        @Test
        void convertToMultiPrecisionAfterCloseStillShortCircuits() {
            PrefixExpression p = PrefixExpression.compile("1+2", "closeConvertName");
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isBigDecimal());
            assertEquals(3.0, (Double) p.calculationCache(false).getResult());
            assertFalse(p.isAvailable());
            assertDoesNotThrow(p::convertToMultiPrecisionSupported);
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
        void getInstanceRegistersCachesAndNeverReturnsNull() {
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
        void getInstanceRegistersCachesAndNeverReturnsNull() {
            BooleanCalculation2 first = BooleanCalculation2.getInstance("booleanRepeatInstance");
            BooleanCalculation2 second = BooleanCalculation2.getInstance("booleanRepeatInstance");
            assertNotNull(first);
            assertNotNull(second);
            assertSame(first, second);

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
        void getInstanceRegistersCachesAndNeverReturnsNull() {
            CumulativeCalculation first = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            CumulativeCalculation second = CumulativeCalculation.getInstance("cumulativeRepeatInstance");
            assertNotNull(first);
            assertNotNull(second);
            assertSame(first, second);

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

        @Test
        void formatStrRejectsAbsurdIntervalsBeforeIterating() {
            CumulativeCalculation calc = CumulativeCalculation.getInstance("cumulativeOverflowInstance");
            // The capacity term format.length() * |end - start| overflows int arithmetic;
            // the descending endpoints would otherwise start iterating from 1e300.
            assertThrows(NegativeArraySizeException.class, () -> calc.formatStr("n[1e300,-1e300,1](n)"));
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
            assertThrows(ExpressionEvaluationException.class, () -> evaluator.evaluate("1/0"));
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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------






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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------





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


    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static BigDecimal bd(String v) { return new BigDecimal(v); }
    @SuppressWarnings("unchecked")
    private static <T> T with(T source, Object... changes) {
        try {
            var components = source.getClass().getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            for (int i=0;i<components.length;i++) {
                types[i]=components[i].getType(); values[i]=components[i].getAccessor().invoke(source);
                for(int j=0;j<changes.length;j+=2) if(components[i].getName().equals(changes[j])) values[i]=changes[j+1];
            }
            return (T) source.getClass().getConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static PriceRequest price() { return new PriceRequest("c",bd("100"),1,MembershipLevel.NONE,null,null,false,false,false,false,0,0,"CN","CNY"); }
    private static Promotion promotion() { return new Promotion("Sale",PromotionType.PERCENT,bd("10"),null,null,false,1,null); }
    private static Coupon coupon() { return new Coupon("SAVE",CouponType.PERCENT,bd("10"),null,null,null,1,true,null,null,0); }
    private static PriceBreakdown priced(PriceRequest r) { return new AdvancedPricingService(CLOCK).calculate(r); }
    @Test void priceMembershipAndBulkBoundaries() {
        MembershipLevel[] levels=MembershipLevel.values(); String[] amounts={"0","3","7","12","10","8"};
        for(int i=0;i<levels.length;i++) { var r=priced(with(price(),"membershipLevel",levels[i])); assertBigDecimalEquals(amounts[i],r.membershipDiscount()); assertBigDecimalEquals("100",r.subtotal()); }
        int[] quantities={1,4,5,19,20,49,50,100}; String[] discounts={"0","0","25","95","200","490","750","1500"};
        for(int i=0;i<quantities.length;i++) assertBigDecimalEquals(discounts[i],priced(with(price(),"quantity",quantities[i])).bulkDiscount());
        assertBigDecimalEquals("180",priced(with(price(),"quantity",10,"membershipLevel",MembershipLevel.ENTERPRISE)).membershipDiscount());
        var free=priced(with(price(),"membershipLevel",MembershipLevel.INSTRUCTOR,"instructorOwnedCourse",true)); assertBigDecimalEquals("0",free.total()); assertTrue(free.appliedRules().contains("FREE_ORDER"));
    }
    @Test void priceTaxesAndOtherDiscounts() {
        String[] countries={"CN","US","DE","SG","GB","FR"}; String[] taxes={"6","7.50","19","9","20","0"};
        for(int i=0;i<countries.length;i++) { var r=priced(with(price(),"countryCode",countries[i])); assertBigDecimalEquals(taxes[i],r.tax()); }
        assertBigDecimalEquals("0",priced(with(price(),"taxExempt",true)).tax());
        assertBigDecimalEquals("2.85",priced(with(price(),"nonprofitCustomer",true)).tax());
        assertBigDecimalEquals("5",priced(with(price(),"nonprofitCustomer",true)).nonprofitDiscount());
        assertBigDecimalEquals("8",priced(with(price(),"nonprofitCustomer",true,"membershipLevel",MembershipLevel.ENTERPRISE)).nonprofitDiscount());
        assertBigDecimalEquals("8",priced(with(price(),"firstPurchase",true)).firstPurchaseDiscount());
        assertBigDecimalEquals("100",priced(with(price(),"firstPurchase",true,"unitPrice",bd("2000"))).firstPurchaseDiscount());
        assertBigDecimalEquals("0",priced(with(price(),"firstPurchase",true,"instructorOwnedCourse",true)).firstPurchaseDiscount());
        assertBigDecimalEquals("0",priced(with(price(),"firstPurchase",true,"coupon",with(coupon(),"stackable",false))).firstPurchaseDiscount());
        for(int points:new int[]{0,99,100,1000,2000,2001}) { String expected=points<100?"0":points==100?"1":points==1000?"10":"20"; assertBigDecimalEquals(expected,priced(with(price(),"loyaltyPointsToUse",points,"availableLoyaltyPoints",10000)).loyaltyDiscount()); }
        assertBigDecimalEquals("1",priced(with(price(),"loyaltyPointsToUse",1000,"availableLoyaltyPoints",100)).loyaltyDiscount());
        var zero=priced(with(price(),"unitPrice",BigDecimal.ZERO,"loyaltyPointsToUse",100,"availableLoyaltyPoints",100)); assertBigDecimalEquals("0",zero.total()); assertEquals(1,zero.warnings().size());
    }
    @Test void pricePromotionsCouponsCapsAndStacking() {
        assertBigDecimalEquals("10",priced(with(price(),"promotion",promotion())).promotionDiscount());
        assertBigDecimalEquals("20",priced(with(price(),"promotion",with(promotion(),"type",PromotionType.FIXED,"value",bd("20")))).promotionDiscount());
        for(int q:new int[]{1,10,20}) assertBigDecimalEquals(q==1?"4":q==10?"80":"240",priced(with(price(),"quantity",q,"promotion",with(promotion(),"type",PromotionType.BUY_MORE_SAVE_MORE))).promotionDiscount());
        assertBigDecimalEquals("5",priced(with(price(),"promotion",with(promotion(),"maximumDiscount",bd("5")))).promotionDiscount());
        for(CouponType t:CouponType.values()) assertBigDecimalEquals(t==CouponType.CASHBACK?"0":"10",priced(with(price(),"coupon",with(coupon(),"type",t))).couponDiscount());
        var cap=priced(with(price(),"promotion",with(promotion(),"value",bd("100")),"coupon",with(coupon(),"value",bd("100")),"firstPurchase",true,"nonprofitCustomer",true));
        assertBigDecimalEquals("70",cap.promotionDiscount()); assertBigDecimalEquals("0",cap.couponDiscount()); assertTrue(cap.appliedRules().contains("DISCOUNT_CAP"));
        var a=priced(with(price(),"promotion",promotion(),"coupon",with(coupon(),"stackable",false))); assertBigDecimalEquals("10",a.promotionDiscount()); assertBigDecimalEquals("0",a.couponDiscount()); assertFalse(a.appliedRules().contains("COUPON_PERCENT"));
        var b=priced(with(price(),"promotion",promotion(),"coupon",with(coupon(),"value",bd("20"),"stackable",false))); assertBigDecimalEquals("0",b.promotionDiscount()); assertBigDecimalEquals("20",b.couponDiscount()); assertFalse(b.appliedRules().contains("PROMOTION_PERCENT"));
    }
    @Test void invalidDiscountConfigurationIsIgnoredWithReason() {
        Object[][] promotions={{"name",null},{"name"," "},{"type",null},{"value",null},{"value",BigDecimal.ZERO},{"value",bd("-1")},{"startsOn",LocalDate.of(2026,11,17)},{"endsOn",LocalDate.of(2026,11,15)},{"membersOnly",true},{"minimumQuantity",2}};
        for(Object[] change:promotions) { var r=priced(with(price(),"promotion",with(promotion(),change))); assertBigDecimalEquals("0",r.promotionDiscount()); assertFalse(r.warnings().isEmpty()); }
        Object[][] coupons={{"code",null},{"code"," "},{"type",null},{"value",null},{"value",BigDecimal.ZERO},{"value",bd("-1")},{"remainingUses",0},{"remainingUses",-1},{"validFrom",LocalDate.of(2026,11,17)},{"validUntil",LocalDate.of(2026,11,15)},{"minimumSubtotal",bd("101")},{"eligibleCourseIds",Set.of("other")},{"maximumQuantity",1},{"minimumMembership",MembershipLevel.BASIC}};
        for(Object[] change:coupons) { var r=priced(with(price(),"quantity",2,"coupon",with(coupon(),change[0].equals("minimumSubtotal")?new Object[]{"minimumSubtotal",bd("201")}:change))); assertBigDecimalEquals("0",r.couponDiscount()); assertFalse(r.warnings().isEmpty()); }
        var today=LocalDate.of(2026,11,16); assertBigDecimalEquals("10",priced(with(price(),"coupon",with(coupon(),"validFrom",today,"validUntil",today,"minimumSubtotal",bd("100"),"maximumQuantity",1,"minimumMembership",MembershipLevel.NONE,"eligibleCourseIds",Set.of("c")))).couponDiscount());
    }
    @Test void priceRejectsMalformedRequests() {
        assertThrows(NullPointerException.class,()->priced(null));
        Object[][] bad={{"courseId",null},{"courseId"," "},{"unitPrice",null},{"unitPrice",bd("-0.01")},{"unitPrice",bd("100000")},{"quantity",0},{"quantity",101},{"membershipLevel",null},{"currency",null},{"currency"," "},{"currency","JPY"},{"countryCode",null},{"countryCode"," "},{"loyaltyPointsToUse",-1},{"availableLoyaltyPoints",-1}};
        for(Object[] c:bad) assertThrows(RuntimeException.class,()->priced(with(price(),c)));
        assertBigDecimalEquals("99999.99",priced(with(price(),"unitPrice",bd("99999.99"))).subtotal());
    }


    private static PurchaseSnapshot purchase() { return new PurchaseSnapshot("p","s","c",bd("100"),bd("6"),BigDecimal.ZERO,NOW.minusSeconds(3600),PaymentMethod.CARD,0,0,false,0,0,false,false,0,false,false,false,false,null); }
    private static RefundRequest refund() { return new RefundRequest(purchase(),RefundReason.ACCIDENTAL_PURCHASE,true,0,false,false,false,false,false,false,false,false); }
    private static RefundDecision refunded(RefundRequest r) { return new RefundPolicyService(CLOCK).evaluate(r); }
    @Test void refundTimeAndUsageBoundaries() {
        int[][] cases={{2,0,100},{3,0,100},{24,5,100},{25,5,100},{168,10,100},{169,10,50},{168,11,70},{168,30,70},{169,30,50},{336,30,50},{337,15,25},{720,15,25},{721,15,0},{720,16,0},{100,80,0},{100,79,0}};
        for(int[] c:cases) { var result=refunded(with(refund(),"purchase",with(purchase(),"paidAt",NOW.minusSeconds(c[0]*3600L),"progressPercent",c[1],"paymentMethod",PaymentMethod.WALLET))); assertBigDecimalEquals(String.valueOf(c[2]),result.refundableAmount()); assertEquals(c[2]>0,result.approved()); assertFalse(result.manualReview()); }
        var reduced=refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",25)); assertBigDecimalEquals("25",reduced.refundableAmount());
        assertBigDecimalEquals("100",refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",0)).refundableAmount());
    }
    @Test void refundSpecialReasonsAndEarlyExits() {
        Object[][] states={{"previouslyRefunded",true,"PURCHASE_ALREADY_REFUNDED"},{"paidAt",NOW.plusSeconds(3600),"PURCHASE_DATE_IN_FUTURE"}};
        for(Object[] c:states) { var r=refunded(with(refund(),"purchase",with(purchase(),c[0],c[1]))); assertFalse(r.approved()); assertEquals(List.of(c[2]),r.reasons()); assertEquals("NONE",r.refundRoute()); }
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"chargebackOpen",true))).manualReview());
        assertTrue(refunded(with(refund(),"fraudSuspected",true)).manualReview());
        assertTrue(refunded(with(refund(),"reason",RefundReason.FRAUD)).manualReview());
        for(String flag:new String[]{"instructorCancelled","courseRemoved"}) assertBigDecimalEquals("100",refunded(with(refund(),"purchase",with(purchase(),flag,true,"certificateIssued",true))).refundableAmount());
        for(RefundReason reason:new RefundReason[]{RefundReason.DUPLICATE_PAYMENT,RefundReason.TECHNICAL_ISSUE,RefundReason.MEDICAL}) assertFalse(refunded(with(refund(),"reason",reason)).approved());
        for(RefundReason reason:new RefundReason[]{RefundReason.DUPLICATE_PAYMENT,RefundReason.MEDICAL}) assertBigDecimalEquals("50",refunded(with(refund(),"reason",reason,"evidenceProvided",true)).refundableAmount());
        assertBigDecimalEquals("100",refunded(with(refund(),"reason",RefundReason.TECHNICAL_ISSUE,"evidenceProvided",true)).refundableAmount());
        Object[][] exceptional={{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",50,"100"},{RefundReason.TECHNICAL_ISSUE,"technicalIssueConfirmed",51,"75"},{RefundReason.MEDICAL,"medicalDocumentVerified",79,"100"},{RefundReason.MEDICAL,"medicalDocumentVerified",80,"40"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",59,"100"},{RefundReason.CONTENT_QUALITY,"contentComplaintUpheld",60,"60"},{RefundReason.DUPLICATE_PAYMENT,"duplicatePaymentConfirmed",100,"100"}};
        for(Object[] c:exceptional) assertBigDecimalEquals((String)c[3],refunded(with(refund(),"reason",c[0],c[1],true,"purchase",with(purchase(),"progressPercent",c[2]))).refundableAmount());
        for(Object[] c:new Object[][]{{"certificateIssued",true},{"downloadedResources",10},{"progressPercent",80}}) assertFalse(refunded(with(refund(),"purchase",with(purchase(),c))).approved());
        var override=refunded(with(refund(),"adminOverride",true,"fullRefundRequested",false,"purchase",with(purchase(),"certificateIssued",true))); assertBigDecimalEquals("50",override.refundableAmount()); assertTrue(override.warnings().contains("ADMIN_OVERRIDE_APPLIED"));
        assertBigDecimalEquals("100",refunded(with(refund(),"adminOverride",true,"purchase",with(purchase(),"certificateIssued",true))).refundableAmount());
    }
    @Test void refundFeesRoutesCouponsAndPoints() {
        PaymentMethod[] methods=PaymentMethod.values(); String[] fees={"3","1","1","2","0","0","5"}; String[] routes={"ORIGINAL_PAYMENT_METHOD","ORIGINAL_PAYMENT_METHOD","ORIGINAL_PAYMENT_METHOD","BANK_TRANSFER","WALLET_CREDIT","WALLET_CREDIT","MANUAL_REVIEW"};
        for(int i=0;i<methods.length;i++) { var r=refunded(with(refund(),"bankDetailsPresent",true,"purchase",with(purchase(),"paymentMethod",methods[i],"paidAt",NOW.minusSeconds(25*3600)))); assertBigDecimalEquals(fees[i],r.processingFee()); assertEquals(routes[i],r.refundRoute()); assertEquals(methods[i]==PaymentMethod.CRYPTO,r.manualReview()); }
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"paymentMethod",PaymentMethod.BANK_TRANSFER))).manualReview());
        for(int attended:new int[]{0,1,2,4,5,10}) { var r=refunded(with(refund(),"purchase",with(purchase(),"paidAt",NOW.minusSeconds(25*3600),"liveSessionsAttended",attended,"totalLiveSessions",10,"downloadedResources",1))); assertBigDecimalEquals(attended<2?"5":attended<5?"10":"15",r.processingFee()); }
        var couponResult=refunded(with(refund(),"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("10"),"loyaltyPointsUsed",101))); assertBigDecimalEquals("90",couponResult.refundableAmount()); assertEquals(50,couponResult.loyaltyPointsReturned());
        var provider=refunded(with(refund(),"reason",RefundReason.COURSE_CANCELLED,"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("200"),"loyaltyPointsUsed",101))); assertBigDecimalEquals("100",provider.refundableAmount()); assertEquals(101,provider.loyaltyPointsReturned());
        assertFalse(refunded(with(refund(),"purchase",with(purchase(),"couponUsed",true,"discountAmount",bd("100")))).approved());
        var proportional=refunded(with(refund(),"fullRefundRequested",false,"requestedPercentage",33,"purchase",with(purchase(),"loyaltyPointsUsed",101))); assertEquals(33,proportional.loyaltyPointsReturned());
        assertFalse(refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("0.99")))).approved());
        assertBigDecimalEquals("0.99",refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("0.99"),"paymentMethod",PaymentMethod.WALLET))).refundableAmount());
        assertBigDecimalEquals("1",refunded(with(refund(),"purchase",with(purchase(),"paidAmount",bd("1")))).refundableAmount());
        assertTrue(refunded(with(refund(),"purchase",with(purchase(),"accessExpiresAt",NOW.minusSeconds(1)))).warnings().contains("COURSE_ACCESS_EXPIRED"));
    }
    @Test void refundValidationMatrix() {
        assertThrows(NullPointerException.class,()->refunded(null)); assertThrows(NullPointerException.class,()->refunded(with(refund(),"purchase",null))); assertThrows(NullPointerException.class,()->refunded(with(refund(),"reason",null)));
        Object[][] bad={{"purchaseId",null},{"purchaseId"," "},{"studentId",null},{"studentId"," "},{"courseId",null},{"courseId"," "},{"paidAmount",null},{"paidAmount",bd("-1")},{"taxAmount",null},{"taxAmount",bd("-1")},{"discountAmount",null},{"discountAmount",bd("-1")},{"paidAt",null},{"paymentMethod",null},{"progressPercent",-1},{"progressPercent",101},{"downloadedResources",-1},{"liveSessionsAttended",-1},{"totalLiveSessions",-1},{"liveSessionsAttended",1},{"loyaltyPointsUsed",-1}};
        for(Object[] c:bad) assertThrows(RuntimeException.class,()->refunded(with(refund(),"purchase",with(purchase(),c))));
        for(int n:new int[]{-1,101}) assertThrows(IllegalArgumentException.class,()->refunded(with(refund(),"requestedPercentage",n)));
    }
    private static StudentProfile student() { return new StudentProfile("s",18,"CN","zh",MembershipLevel.NONE,true,false,true,true,true,false,false,true,true,0); }
    private static CourseOffering offering() { return new CourseOffering("c","i",EnrollmentCourseStatus.PUBLISHED,null,null,null,10,0,false,0,0,0,0,null,null,null,0,0,0,false,null,false,false,null,0,0,bd("100")); }
    private static EnrollmentRequest enrollment() { return new EnrollmentRequest(student(),offering(),1,true,false,false,false,null,false,false); }
    private static EnrollmentDecision enrolled(EnrollmentRequest r) { return new EnrollmentEligibilityService(new EmptyEnrollmentHistory(),CLOCK).evaluate(r); }
    @Test void enrollmentBaselinePaymentsAndRisks() {
        var normal=enrolled(enrollment()); assertTrue(normal.accepted()); assertEquals(EnrollmentOutcome.ENROLLED,normal.outcome()); assertEquals(10,normal.availableSeats()); assertTrue(normal.paymentRequired()); assertEquals(List.of(),normal.reasons()); assertEquals(List.of(),normal.warnings());
        assertEquals(List.of("PAYMENT_NOT_AUTHORIZED"),enrolled(with(enrollment(),"paymentAuthorized",false)).reasons());
        for(String flag:new String[]{"auditMode","scholarshipApproved","organizationSponsored"}) assertFalse(enrolled(with(enrollment(),flag,true)).paymentRequired());
        for(BigDecimal amount:new BigDecimal[]{null,BigDecimal.ZERO,bd("-1")}) assertFalse(enrolled(with(enrollment(),"course",with(offering(),"price",amount))).paymentRequired());
        int[] risks={0,49,50,69,70,89,90,100};
        for(int risk:risks) { var r=enrolled(with(enrollment(),"student",with(student(),"riskScore",risk))); assertEquals(risk>=90?EnrollmentOutcome.REJECTED:risk>=70?EnrollmentOutcome.MANUAL_REVIEW:EnrollmentOutcome.ENROLLED,r.outcome()); if(risk>=50&&risk<70) assertTrue(r.warnings().contains("ELEVATED_RISK_MONITORING")); }
    }
    @Test void enrollmentStudentAndCourseBlockers() {
        Object[][] cases={{"accountActive",false,"ACCOUNT_INACTIVE"},{"suspended",true,"ACCOUNT_SUSPENDED"},{"emailVerified",false,"EMAIL_NOT_VERIFIED"},{"profileComplete",false,"PROFILE_INCOMPLETE"},{"termsAccepted",false,"TERMS_NOT_ACCEPTED"},{"age",17,"PARENTAL_CONSENT_REQUIRED"},{"age",-1,"INVALID_STUDENT_AGE"},{"age",121,"INVALID_STUDENT_AGE"},{"countryCode",null,"COUNTRY_MISSING"},{"countryCode"," ","COUNTRY_MISSING"}};
        for(Object[] c:cases) { var r=enrolled(with(enrollment(),"student",with(student(),c[0],c[1]))); assertEquals(EnrollmentOutcome.REJECTED,r.outcome()); assertTrue(r.reasons().contains(c[2])); }
        Object[][] courses={{"status",null,"COURSE_STATUS_MISSING"},{"status",EnrollmentCourseStatus.DRAFT,"COURSE_NOT_PUBLISHED"},{"status",EnrollmentCourseStatus.CLOSED,"COURSE_CLOSED"},{"status",EnrollmentCourseStatus.ARCHIVED,"COURSE_ARCHIVED"},{"enrollmentOpensAt",NOW.plusSeconds(1),"ENROLLMENT_NOT_OPEN"},{"enrollmentClosesAt",NOW.minusSeconds(1),"ENROLLMENT_CLOSED"},{"startsAt",NOW.minusSeconds(1),"COURSE_ALREADY_STARTED"},{"minimumAge",19,"BELOW_MINIMUM_AGE"},{"maximumAge",17,"ABOVE_MAXIMUM_AGE"},{"allowedCountries",Set.of("US"),"COUNTRY_NOT_ALLOWED"},{"requiredLanguage","en","LANGUAGE_REQUIREMENT_NOT_MET"},{"requiredOrganizationId","org","ORGANIZATION_NOT_ALLOWED"},{"liveCourse",true,"LIVE_COURSE_WITHOUT_SCHEDULE"}};
        for(Object[] c:courses) assertTrue(enrolled(with(enrollment(),"course",with(offering(),c[0],c[1]))).reasons().contains(c[2]));
        assertEquals(EnrollmentOutcome.ENROLLED,enrolled(with(enrollment(),"course",with(offering(),"enrollmentOpensAt",NOW,"enrollmentClosesAt",NOW,"startsAt",NOW,"minimumAge",18,"maximumAge",18,"allowedCountries",Set.of("CN"),"requiredLanguage","ZH"))).outcome());
        assertTrue(enrolled(with(enrollment(),"auditMode",true,"course",with(offering(),"status",EnrollmentCourseStatus.ARCHIVED,"startsAt",NOW.minusSeconds(1)))).warnings().containsAll(List.of("ARCHIVED_COURSE_AUDIT_ONLY","LATE_AUDIT_ENROLLMENT")));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"identityVerified",false))).warnings().contains("IDENTITY_NOT_VERIFIED"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"identityVerified",false),"course",with(offering(),"liveCourse",true))).reasons().contains("IDENTITY_REQUIRED_FOR_LIVE_COURSE"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"instructor",true),"course",with(offering(),"instructorId","s"))).reasons().contains("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"));
    }
    @Test void enrollmentCapacityWaitlistAndAccessibility() {
        var full=with(offering(),"enrolledCount",10); assertEquals(List.of("COURSE_FULL"),enrolled(with(enrollment(),"course",full)).reasons());
        var wait=with(full,"waitlistEnabled",true,"waitlistCapacity",2);
        assertEquals(List.of("WAITLIST_NOT_ACCEPTED"),enrolled(with(enrollment(),"course",wait)).reasons());
        var req=with(enrollment(),"course",wait,"acceptWaitlist",true); var r=enrolled(req); assertEquals(EnrollmentOutcome.WAITLISTED,r.outcome()); assertTrue(r.accepted()); assertFalse(r.paymentRequired());
        assertEquals(List.of("WAITLIST_DISABLED_BY_CAPACITY"),enrolled(with(req,"course",with(wait,"waitlistCapacity",0))).reasons());
        assertEquals(List.of("WAITLIST_FULL"),enrolled(with(req,"course",with(wait,"waitlistedCount",2))).reasons());
        assertEquals(List.of("GROUP_ENROLLMENT_CANNOT_WAITLIST"),enrolled(with(req,"requestedSeats",2,"organizationSponsored",true)).reasons());
        assertEquals(EnrollmentOutcome.MANUAL_REVIEW,enrolled(with(req,"student",with(student(),"riskScore",70))).outcome());
        assertTrue(enrolled(with(enrollment(),"requestedSeats",2)).reasons().contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true)).reasons().contains("NO_ACCESSIBILITY_SEATS"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true,"course",with(offering(),"accessibilitySeatCapacity",1,"accessibilitySeatsUsed",1))).reasons().contains("ACCESSIBILITY_SEATS_FULL"));
        assertTrue(enrolled(with(enrollment(),"accessibilitySeatRequired",true,"course",with(offering(),"accessibilitySeatCapacity",1))).warnings().contains("ACCESSIBILITY_SEAT_RESERVED"));
        assertTrue(enrolled(with(enrollment(),"course",with(offering(),"accessibilitySeatsUsed",1))).warnings().contains("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID"));
        for(int seats:new int[]{2,3,4}) assertEquals(seats<=3,enrolled(with(enrollment(),"course",with(offering(),"capacity",seats))).warnings().contains("COURSE_ALMOST_FULL"));
    }
    @Test void enrollmentExternalHistoryAndPrerequisiteScores() {
        EnrollmentHistory history=mock(EnrollmentHistory.class);
        var service=new EnrollmentEligibilityService(history,CLOCK);
        when(history.alreadyEnrolled("s","c")).thenReturn(true); assertTrue(service.evaluate(enrollment()).reasons().contains("ALREADY_ENROLLED")); when(history.alreadyEnrolled("s","c")).thenReturn(false);
        when(history.hasRefundAbuseFlag("s")).thenReturn(true); assertEquals(EnrollmentOutcome.MANUAL_REVIEW,service.evaluate(enrollment()).outcome()); when(history.hasRefundAbuseFlag("s")).thenReturn(false);
        var prereq=with(enrollment(),"course",with(offering(),"prerequisiteCourseIds",Set.of("pre"),"minimumPrerequisiteScore",60));
        assertTrue(service.evaluate(prereq).reasons().contains("MISSING_PREREQUISITE:pre")); when(history.completedCourse("s","pre")).thenReturn(true);
        for(int score:new int[]{-1,0,59,60,94,95,100,101}) { when(history.completionScore("s","pre")).thenReturn(score); var r=service.evaluate(prereq); assertEquals(score>=60&&score<=100,r.accepted()); assertEquals(score>=95&&score<=100,r.warnings().contains("EXCELLENT_PREREQUISITE_SCORE:pre")); }
        for(int attempts:new int[]{-1,0,1,2}) { when(history.failedEnrollmentAttempts("s","c")).thenReturn(attempts); var req=with(enrollment(),"course",with(offering(),"maximumFailedAttempts",2)); if(attempts<0) assertThrows(IllegalStateException.class,()->service.evaluate(req)); else assertEquals(attempts<2,service.evaluate(req).accepted()); } when(history.failedEnrollmentAttempts("s","c")).thenReturn(0);
        for(int active:new int[]{-1,0,1,2,3}) { when(history.activeCourseCountThisWeek("s")).thenReturn(active); var req=with(enrollment(),"course",with(offering(),"maximumWeeklyCourses",2)); if(active<0) assertThrows(IllegalStateException.class,()->service.evaluate(req)); else { assertEquals(active<2,service.evaluate(req).accepted()); assertEquals(active<=2,service.evaluate(with(req,"student",with(student(),"membershipLevel",MembershipLevel.PREMIUM))).accepted()); assertTrue(service.evaluate(with(req,"student",with(student(),"membershipLevel",MembershipLevel.ENTERPRISE),"organizationSponsored",true)).accepted()); } }
        when(history.activeCourseCountThisWeek("s")).thenReturn(0); when(history.hasScheduleConflict("s","slot")).thenReturn(true);
        var live=with(enrollment(),"course",with(offering(),"liveCourse",true,"scheduleKey","slot")); assertTrue(service.evaluate(live).reasons().contains("SCHEDULE_CONFLICT")); assertTrue(service.evaluate(with(live,"auditMode",true)).accepted());
    }
    @Test void enrollmentOrganizationsAndValidation() {
        var req=with(enrollment(),"course",with(offering(),"organizationOnly",true,"subscriptionRequired",true),"student",with(student(),"activeSubscription",false));
        assertTrue(enrolled(req).reasons().containsAll(List.of("ORGANIZATION_SPONSOR_REQUIRED","ORGANIZATION_ID_REQUIRED","ACTIVE_SUBSCRIPTION_REQUIRED")));
        assertTrue(enrolled(with(req,"organizationSponsored",true,"organizationId","org")).accepted());
        assertTrue(enrolled(with(enrollment(),"scholarshipApproved",true,"organizationSponsored",true)).warnings().contains("SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT"));
        assertTrue(enrolled(with(enrollment(),"student",with(student(),"activeSubscription",false),"course",with(offering(),"subscriptionRequired",true),"scholarshipApproved",true)).warnings().contains("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP"));
        for(Object[] c:new Object[][]{{"studentId",null},{"studentId"," "},{"riskScore",-1},{"riskScore",101}}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"student",with(student(),c))));
        for(Object[] c:new Object[][]{{"courseId",null},{"courseId"," "},{"capacity",-1},{"enrolledCount",-1},{"enrollmentOpensAt",NOW.plusSeconds(1),"enrollmentClosesAt",NOW},{"minimumAge",20,"maximumAge",10}}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"course",with(offering(),c))));
        for(int seats:new int[]{0,51}) assertThrows(IllegalArgumentException.class,()->enrolled(with(enrollment(),"requestedSeats",seats)));
        assertThrows(NullPointerException.class,()->enrolled(null)); assertThrows(NullPointerException.class,()->enrolled(with(enrollment(),"student",null))); assertThrows(NullPointerException.class,()->enrolled(with(enrollment(),"course",null)));
    }

    @Test
    void coursePriceBoundariesAndDiscountRounding() {
        var policy = new net.mooctest.course.service.PricePolicy();
        for (BigDecimal invalid : new BigDecimal[]{null, new BigDecimal("-0.01"), new BigDecimal("100000"), new BigDecimal("99999.991")}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.normalizePrice(invalid));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Price must be between 0 and 99999.99", ex.getMessage());
        }
        assertEquals(new BigDecimal("0.00"), policy.normalizePrice(BigDecimal.ZERO));
        assertEquals(new BigDecimal("99999.99"), policy.normalizePrice(new BigDecimal("99999.99")));
        assertEquals(new BigDecimal("1.24"), policy.normalizePrice(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("12.34"), policy.discountedPrice(new BigDecimal("12.34"), 0));
        assertEquals(new BigDecimal("2.47"), policy.discountedPrice(new BigDecimal("12.34"), 80));
        assertEquals(new BigDecimal("9.26"), policy.discountedPrice(new BigDecimal("12.34"), 25));
        for (int invalid : new int[]{-1, 81}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.discountedPrice(BigDecimal.ONE, invalid));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_DISCOUNT, ex.code());
            assertEquals("Discount percent must be between 0 and 80", ex.getMessage());
        }
    }

    @Test
    void courseRecordNormalizesAndPublishesOnlyDraft() {
        var time = Instant.parse("2026-01-01T00:00:00Z");
        var course = new net.mooctest.course.domain.Course(" id ", " Title ", new BigDecimal("1.235"), net.mooctest.course.domain.CourseStatus.DRAFT, time);
        assertEquals("id", course.id());
        assertEquals("Title", course.title());
        assertEquals(new BigDecimal("1.24"), course.price());
        assertEquals(time, course.createdAt());
        var published = course.publish();
        assertEquals(net.mooctest.course.domain.CourseStatus.DRAFT, course.status());
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.status());
        assertEquals(course.id(), published.id());
        assertEquals(course.title(), published.title());
        assertEquals(course.price(), published.price());
        assertEquals(time, published.createdAt());
        for (var status : new net.mooctest.course.domain.CourseStatus[]{net.mooctest.course.domain.CourseStatus.PUBLISHED, net.mooctest.course.domain.CourseStatus.ARCHIVED}) {
            var other = new net.mooctest.course.domain.Course("a", "title", BigDecimal.ZERO, status, time);
            var ex = assertThrows(net.mooctest.error.BusinessException.class, other::publish);
            assertEquals(net.mooctest.error.ErrorCode.COURSE_ALREADY_PUBLISHED, ex.code());
            assertEquals("Only a draft course can be published", ex.getMessage());
        }
        for (String invalid : new String[]{null, "", " \t "}) {
            Class<? extends Throwable> expected = invalid == null ? NullPointerException.class : IllegalArgumentException.class;
            assertThrows(expected, () -> new net.mooctest.course.domain.Course(invalid, "title", BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, time));
            assertThrows(expected, () -> new net.mooctest.course.domain.Course("id", invalid, BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, time));
        }
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", null, net.mooctest.course.domain.CourseStatus.DRAFT, time));
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", BigDecimal.ZERO, null, time));
        assertThrows(NullPointerException.class, () -> new net.mooctest.course.domain.Course("id", "title", BigDecimal.ZERO, net.mooctest.course.domain.CourseStatus.DRAFT, null));
    }

    @Test
    void courseCreationRejectsDuplicateAndInvalidTitles() {
        var repository = new net.mooctest.course.adapter.InMemoryCourseRepository();
        var clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        var service = new net.mooctest.course.service.CourseService(repository, id -> List.of(), () -> "course-1", new net.mooctest.course.service.PricePolicy(), clock);
        assertFalse(repository.existsByTitleIgnoreCase("absent"));
        var course = service.createCourse("  Linear   Algebra  ", new BigDecimal("25.125"));
        assertEquals("Linear Algebra", course.title());
        assertEquals("course-1", course.id());
        assertEquals(new BigDecimal("25.13"), course.price());
        assertEquals(clock.instant(), course.createdAt());
        assertSame(course, service.getCourse("course-1"));
        assertTrue(repository.existsByTitleIgnoreCase("LINEAR ALGEBRA"));
        assertFalse(repository.existsByTitleIgnoreCase("geometry"));
        assertEquals(net.mooctest.error.ErrorCode.DUPLICATE_COURSE_TITLE, assertThrows(net.mooctest.error.BusinessException.class, () -> service.createCourse("linear algebra", BigDecimal.ONE)).code());
        var missing = assertThrows(net.mooctest.error.BusinessException.class, () -> service.getCourse("absent"));
        assertEquals(net.mooctest.error.ErrorCode.COURSE_NOT_FOUND, missing.code());
        assertEquals("Course not found: absent", missing.getMessage());
        assertEquals(net.mooctest.error.ErrorCode.COURSE_HAS_NO_CHAPTERS, assertThrows(net.mooctest.error.BusinessException.class, () -> service.publishCourse("course-1")).code());
        assertEquals(net.mooctest.course.domain.CourseStatus.DRAFT, service.getCourse("course-1").status());
        for (String invalid : new String[]{null, "", "  ", "ab", "a".repeat(81)}) {
            var ex = assertThrows(net.mooctest.error.BusinessException.class, () -> service.createCourse(invalid, BigDecimal.ONE));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_COURSE_TITLE, ex.code());
            assertEquals("Course title length must be between 3 and 80 characters", ex.getMessage());
        }
        assertEquals("abc", service.createCourse("abc", BigDecimal.ZERO).title());
        assertEquals("a".repeat(80), service.createCourse("a".repeat(80), BigDecimal.ZERO).title());
    }

    @Test
    void courseControllerAndCatalogPreserveSortedContent() {
        var repository = new net.mooctest.course.adapter.InMemoryCourseRepository();
        net.mooctest.course.port.ChapterRepository chapters = id -> List.of(
                new net.mooctest.course.domain.Chapter("c3", id, "zeta", 1),
                new net.mooctest.course.domain.Chapter("c2", id, "Alpha", 1),
                new net.mooctest.course.domain.Chapter("c1", id, "first", 0));
        net.mooctest.course.port.VideoRepository videos = ids -> {
            assertEquals(java.util.Set.of("c1", "c2", "c3"), ids);
            return List.of(new net.mooctest.course.domain.Video("v3", "c1", "zeta", 30, 1),
                    new net.mooctest.course.domain.Video("v2", "c1", "Alpha", 20, 1),
                    new net.mooctest.course.domain.Video("v1", "c1", "first", 10, 0));
        };
        var service = new net.mooctest.course.service.CourseService(repository, chapters, () -> "course", new net.mooctest.course.service.PricePolicy(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        var catalog = new net.mooctest.course.service.CatalogService(service, chapters, videos);
        var controller = new net.mooctest.course.web.CourseController(service, catalog);
        var response = controller.create(new net.mooctest.course.web.CreateCourseRequest("Algebra", new BigDecimal("10")));
        assertEquals(201, response.getStatusCode().value());
        assertEquals(java.net.URI.create("/api/courses/course"), response.getHeaders().getLocation());
        assertTrue(response.getBody().success());
        assertNull(response.getBody().error());
        assertEquals("Algebra", response.getBody().data().title());
        assertEquals(response.getBody(), controller.get("course"));
        var published = controller.publish("course");
        assertTrue(published.success());
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.data().status());
        assertEquals(published.data(), service.getCourse("course"));
        var result = controller.catalog("course");
        assertTrue(result.success());
        assertNull(result.error());
        assertEquals("course", result.data().courseId());
        assertEquals(List.of("c1", "c2", "c3"), result.data().chapters().stream().map(net.mooctest.course.domain.ChapterView::id).toList());
        assertEquals("first", result.data().chapters().get(0).title());
        assertEquals(List.of(new net.mooctest.course.domain.VideoView("v1", "first", 10), new net.mooctest.course.domain.VideoView("v2", "Alpha", 20), new net.mooctest.course.domain.VideoView("v3", "zeta", 30)), result.data().chapters().get(0).videos());
        assertEquals(List.of(), result.data().chapters().get(1).videos());
        assertThrows(UnsupportedOperationException.class, () -> result.data().chapters().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.data().chapters().get(0).videos().clear());
        var emptyCatalog = new net.mooctest.course.service.CatalogService(service, new net.mooctest.course.adapter.InMemoryChapterRepository(), new net.mooctest.course.adapter.InMemoryVideoRepository());
        assertEquals(List.of(), emptyCatalog.getCatalog("course").chapters());
        assertEquals(List.of(), new net.mooctest.course.adapter.InMemoryVideoRepository().findByChapterIds(java.util.Set.of("c")));
        var ids = new net.mooctest.course.adapter.UuidGenerator();
        assertEquals(4, java.util.UUID.fromString(ids.nextId()).version());
    }

    @Test
    void restErrorsExposeStatusCodeAndMessage() throws Exception {
        var handler = new net.mooctest.course.web.RestExceptionHandler();
        var codes = net.mooctest.error.ErrorCode.values();
        int[] statuses = {404, 409, 400, 400, 400, 409, 422};
        for (int i = 0; i < codes.length; i++) {
            var response = handler.handleBusinessException(new net.mooctest.error.BusinessException(codes[i], "detail"));
            assertEquals(statuses[i], response.getStatusCode().value());
            assertFalse(response.getBody().success());
            assertNull(response.getBody().data());
            assertEquals(codes[i].name(), response.getBody().error().code());
            assertEquals("detail", response.getBody().error().message());
        }
        assertThrows(NullPointerException.class, () -> new net.mooctest.error.BusinessException(null, "detail"));
        var binding = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request");
        var parameter = new org.springframework.core.MethodParameter(net.mooctest.course.web.CourseController.class.getMethod("create", net.mooctest.course.web.CreateCourseRequest.class), 0);
        var exception = new org.springframework.web.bind.MethodArgumentNotValidException(parameter, binding);
        var empty = handler.handleValidation(exception);
        assertEquals(400, empty.getStatusCode().value());
        assertEquals("Request validation failed", empty.getBody().error().message());
        binding.addError(new org.springframework.validation.FieldError("request", "title", "required"));
        binding.addError(new org.springframework.validation.FieldError("request", "price", "negative"));
        var error = handler.handleValidation(exception);
        assertEquals(400, error.getStatusCode().value());
        assertFalse(error.getBody().success());
        assertNull(error.getBody().data());
        assertEquals("VALIDATION_ERROR", error.getBody().error().code());
        assertEquals("title: required", error.getBody().error().message());
        var method = net.mooctest.config.TimeConfiguration.class.getDeclaredMethod("systemClock");
        method.setAccessible(true);
        var clock = (Clock) method.invoke(new net.mooctest.config.TimeConfiguration());
        assertEquals(ZoneOffset.UTC, clock.getZone());
    }


    private static ScheduleRequest schedule() { return new ScheduleRequest("c","co","i",DeliveryMode.ONLINE,CourseKind.LECTURE,NOW.plusSeconds(34*3600),NOW.plusSeconds(35*3600),"UTC",null,"https://meet",20,20,30,null,null,false,true,false,false,false,false,1,0,5,null,null,null,null,false,false,false); }
    private static SchedulingDecision scheduled(ScheduleRequest r) { return new SchedulingService(CLOCK).evaluate(r); }
    @Test void scheduleBaselineDurationAndCourseKindBoundaries() {
        var r=scheduled(schedule()); assertTrue(r.approved()); assertFalse(r.manualReview()); assertEquals("APPROVED",r.status()); assertEquals(60,r.durationMinutes()); assertEquals(List.of(),r.conflicts()); assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"),r.warnings());
        CourseKind[] kinds=CourseKind.values(); int[] minimum={30,60,20,45,15}; int[] maximum={180,240,300,360,120}; String[] shortCodes={"LECTURE_TOO_SHORT","LAB_TOO_SHORT","EXAM_TOO_SHORT","WORKSHOP_TOO_SHORT","WEBINAR_TOO_SHORT"}; String[] longCodes={"LECTURE_LONGER_THAN_RECOMMENDED","LAB_TOO_LONG","EXAM_TOO_LONG","WORKSHOP_REQUIRES_BREAK_PLAN","WEBINAR_LONGER_THAN_RECOMMENDED"};
        for(int i=0;i<kinds.length;i++) for(int minutes:new int[]{minimum[i]-1,minimum[i],maximum[i],maximum[i]+1}) { var req=with(schedule(),"courseKind",kinds[i],"end",schedule().start().plusSeconds(minutes*60L),"lunchTimeAllowed",true,"availableEquipment",Set.of("LAB_SAFETY_KIT","PROCTORING")); var decision=scheduled(req); assertEquals(minutes<minimum[i],decision.conflicts().contains(shortCodes[i])); assertEquals(minutes>maximum[i],(i==1||i==2?decision.conflicts():decision.warnings()).contains(longCodes[i])); assertEquals(minutes,decision.durationMinutes()); }
    }
    @Test void schedulingTimingBoundaryMatrix() {
        for(int lead:new int[]{-1,0,1,2,23,24}) { var start=NOW.plusSeconds(lead*3600L); var r=scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600),"weekendAllowed",true,"afterHoursAllowed",true,"lunchTimeAllowed",true)); assertEquals(lead<0,r.conflicts().contains("START_TIME_IN_PAST")); assertEquals(lead>=0&&lead<2,r.conflicts().contains("INSUFFICIENT_BOOKING_NOTICE")); assertEquals(lead>=2&&lead<24,r.warnings().contains("SHORT_BOOKING_NOTICE")); }
        for(int day:new int[]{5,6}) { var start=NOW.plusSeconds(day*86400L+10*3600); assertTrue(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600))).conflicts().contains("WEEKEND_NOT_ALLOWED")); assertFalse(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600),"weekendAllowed",true)).conflicts().contains("WEEKEND_NOT_ALLOWED")); }
        assertTrue(scheduled(with(schedule(),"holidays",Set.of(LocalDate.of(2026,11,17)))).conflicts().contains("HOLIDAY_NOT_AVAILABLE"));
        for(int hour:new int[]{7,8,20,21}) { var start=NOW.plusSeconds(86400+hour*3600L); var req=with(schedule(),"start",start,"end",start.plusSeconds(3600)); assertEquals(hour==7||hour==21,scheduled(req).conflicts().contains("OUTSIDE_OPERATING_HOURS")); assertEquals(hour==7||hour==21,scheduled(with(req,"afterHoursAllowed",true)).warnings().contains("AFTER_HOURS_SESSION")); }
        for(int minute:new int[]{660,661,750,810}) { var start=NOW.plusSeconds(86400+minute*60L); var req=with(schedule(),"start",start,"end",start.plusSeconds(3600)); boolean overlap=minute>660&&minute<810; assertEquals(overlap,scheduled(req).conflicts().contains("LUNCH_BREAK_CONFLICT")); assertEquals(overlap,scheduled(with(req,"lunchTimeAllowed",true)).warnings().contains("LUNCH_TIME_SESSION")); }
        var start=NOW.plusSeconds(47*3600); assertTrue(scheduled(with(schedule(),"start",start,"end",start.plusSeconds(3600))).conflicts().contains("SESSION_CROSSES_LOCAL_DATE"));
    }
    @Test void schedulingDeliveryAndEquipment() {
        for(DeliveryMode mode:DeliveryMode.values()) for(String room:new String[]{null," ","room"}) for(String url:new String[]{null," ","http://meet","https://meet"}) { var r=scheduled(with(schedule(),"deliveryMode",mode,"roomId",room,"meetingUrl",url)); boolean noRoom=room==null||room.isBlank(), noUrl=url==null||url.isBlank(); if(mode==DeliveryMode.CLASSROOM) assertEquals(noRoom,r.conflicts().contains("ROOM_REQUIRED")); else { assertEquals(noUrl,r.conflicts().contains(mode==DeliveryMode.HYBRID?"HYBRID_MEETING_URL_REQUIRED":"MEETING_URL_REQUIRED")); assertEquals(!noUrl&&!url.startsWith("https://"),r.conflicts().contains("SECURE_MEETING_URL_REQUIRED")); } if(mode==DeliveryMode.HYBRID) assertEquals(noRoom,r.conflicts().contains("HYBRID_ROOM_REQUIRED")); }
        assertTrue(scheduled(with(schedule(),"recordingRequired",true)).conflicts().contains("RECORDING_NOT_SUPPORTED"));
        assertTrue(scheduled(with(schedule(),"recordingSupported",true)).warnings().contains("RECORDING_AVAILABLE"));
        assertTrue(scheduled(with(schedule(),"requiredEquipment",Set.of("PROJECTOR"))).conflicts().contains("MISSING_EQUIPMENT:PROJECTOR"));
        assertFalse(scheduled(with(schedule(),"requiredEquipment",Set.of("PROJECTOR"),"availableEquipment",Set.of("PROJECTOR"))).conflicts().contains("MISSING_EQUIPMENT:PROJECTOR"));
        assertTrue(scheduled(with(schedule(),"courseKind",CourseKind.LAB)).conflicts().contains("LAB_SAFETY_KIT_REQUIRED"));
        assertEquals("MANUAL_REVIEW",scheduled(with(schedule(),"courseKind",CourseKind.EXAM)).status());
    }
    @Test void schedulingCapacityAndManualReview() {
        for(int expected:new int[]{500,501,1000,1001}) {var r=scheduled(with(schedule(),"expectedParticipants",expected)); assertEquals(expected>1000,r.conflicts().contains("ONLINE_PLATFORM_CAPACITY_EXCEEDED")); assertEquals(expected>500&&expected<=1000,r.warnings().contains("LARGE_ONLINE_EVENT")); }
        for(int capacity:new int[]{0,19,20,59,60,61}) {var r=scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","roomCapacity",capacity)); assertEquals(capacity==0,r.conflicts().contains("ROOM_CAPACITY_UNKNOWN")); assertEquals(capacity>0&&capacity<20,r.conflicts().contains("ROOM_TOO_SMALL_FOR_REGISTRATION")); assertEquals(capacity>60,r.warnings().contains("ROOM_SIGNIFICANTLY_OVERSIZED")); }
        assertTrue(scheduled(with(schedule(),"deliveryMode",DeliveryMode.HYBRID,"roomId","room","expectedParticipants",31)).warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY"));
        assertTrue(scheduled(with(schedule(),"expectedParticipants",19)).warnings().contains("REGISTRATION_EXCEEDS_FORECAST"));
        assertTrue(scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","accessibleRoomRequired",true,"roomAccessible",false)).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));
        for(int size:new int[]{30,31}) assertEquals(size==31,scheduled(with(schedule(),"courseKind",CourseKind.LAB,"expectedParticipants",size)).conflicts().contains("LAB_CAPACITY_EXCEEDED"));
        for(int size:new int[]{50,51}) assertEquals(size==51,scheduled(with(schedule(),"courseKind",CourseKind.WORKSHOP,"expectedParticipants",size)).warnings().contains("WORKSHOP_GROUP_TOO_LARGE"));
        for(int size:new int[]{200,201}) assertEquals(size==201,scheduled(with(schedule(),"deliveryMode",DeliveryMode.HYBRID,"roomId","room","expectedParticipants",size,"roomCapacity",size)).manualReview());
        for(int occurrences:new int[]{1,2,20,21,52,53}) {var r=scheduled(with(schedule(),"recurring",true,"occurrenceCount",occurrences)); assertEquals(occurrences==1,r.conflicts().contains("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES")); assertEquals(occurrences>52,r.conflicts().contains("TOO_MANY_RECURRENCES")); assertEquals(occurrences>20,r.manualReview());}
        assertTrue(scheduled(with(schedule(),"occurrenceCount",2)).conflicts().contains("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"));
    }
    @Test void schedulingIntervalsBookingsAndOverride() {
        Instant start=schedule().start(),end=schedule().end();
        for(TimeWindow window:List.of(new TimeWindow(start,end),new TimeWindow(start.minusSeconds(1),end.plusSeconds(1)))) assertFalse(scheduled(with(schedule(),"instructorAvailability",List.of(window))).conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        for(TimeWindow window:List.of(new TimeWindow(start.plusSeconds(1),end),new TimeWindow(start,end.minusSeconds(1)))) assertTrue(scheduled(with(schedule(),"instructorAvailability",List.of(window))).conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        for(TimeWindow window:List.of(new TimeWindow(start.minusSeconds(3600),start),new TimeWindow(end,end.plusSeconds(3600)))) assertFalse(scheduled(with(schedule(),"maintenanceWindows",List.of(window))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        assertTrue(scheduled(with(schedule(),"maintenanceWindows",List.of(new TimeWindow(start,end)))).conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
        var booking=new ExistingBooking("b","i","room","co",start,end,false,true,CourseKind.LECTURE);
        var r=scheduled(with(schedule(),"deliveryMode",DeliveryMode.CLASSROOM,"roomId","room","existingBookings",List.of(booking))); assertTrue(r.conflicts().containsAll(List.of("INSTRUCTOR_BOOKING_CONFLICT:b","ROOM_BOOKING_CONFLICT:b","COHORT_BOOKING_CONFLICT:b"))); assertTrue(r.warnings().contains("CONFLICTING_BOOKING_IS_TENTATIVE:b"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(with(booking,"cancelled",true)))).approved());
        var prior=with(booking,"start",start.minusSeconds(3600),"end",start,"tentative",false);
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior))).approved());
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"minimumBufferMinutes",1)).conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:b"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"maximumInstructorSessionsPerDay",1)).conflicts().contains("INSTRUCTOR_DAILY_SESSION_LIMIT"));
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(prior),"maximumInstructorSessionsPerDay",2)).warnings().contains("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"));
        assertTrue(scheduled(with(schedule(),"meetingUrl",null,"administratorOverride",true)).approved());
        assertFalse(scheduled(with(schedule(),"existingBookings",List.of(booking),"administratorOverride",true)).approved());
        assertTrue(scheduled(with(schedule(),"existingBookings",List.of(booking),"administratorOverride",true)).warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));
    }
    @Test void schedulingRejectsMalformedRequests() {
        assertThrows(NullPointerException.class,()->scheduled(null));
        Object[][] bad={{"courseId",null},{"courseId"," "},{"cohortId",null},{"cohortId"," "},{"instructorId",null},{"instructorId"," "},{"deliveryMode",null},{"courseKind",null},{"start",null},{"end",null},{"end",schedule().start()},{"end",schedule().start().plusSeconds(86400)},{"zoneId",null},{"zoneId"," "},{"zoneId","Invalid/Zone"},{"expectedParticipants",-1},{"registeredParticipants",-1},{"roomCapacity",-1},{"occurrenceCount",0},{"minimumBufferMinutes",-1},{"minimumBufferMinutes",241},{"maximumInstructorSessionsPerDay",0},{"requiredEquipment",Set.of(" ")},{"availableEquipment",Set.of(" ")}};
        for(Object[] c:bad) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),c)),String.valueOf(c[0]));
        for(TimeWindow w:List.of(new TimeWindow(null,NOW),new TimeWindow(NOW,null),new TimeWindow(NOW,NOW))) for(String field:List.of("instructorAvailability","maintenanceWindows")) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),field,List.of(w))));
        var b=new ExistingBooking("b","i","r","co",NOW,NOW.plusSeconds(60),false,false,CourseKind.EXAM);
        for(Object[] c:new Object[][]{{"bookingId",null},{"bookingId"," "},{"start",null},{"end",null},{"end",NOW}}) assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),"existingBookings",List.of(with(b,c)))));
        assertThrows(IllegalArgumentException.class,()->scheduled(with(schedule(),"existingBookings",List.of(b,b))));
    }

    private net.mooctest.expression.engine.EquationSolver equationFor(java.util.function.DoubleUnaryOperator function, double result) throws Exception {
        var constructor = net.mooctest.expression.engine.EquationSolver.class.getDeclaredConstructor(String.class, double.class, String.class, net.mooctest.expression.engine.ManyToOneNumberFunction.class);
        constructor.setAccessible(true);
        var input = new net.mooctest.expression.engine.ManyToOneNumberFunction("testfunction") {
            @Override public double run(double... values) { return function.applyAsDouble(values[0]); }
        };
        return constructor.newInstance("f(x)", result, "equation", input);
    }

    @Test
    void equationNewtonSolvesKnownRootsAndRejectsNonconvergence() throws Exception {
        var solver = equationFor(x -> x * x, 9);
        assertEquals(9, solver.getResult());
        assertFalse(solver.isBigDecimal());
        assertTrue(solver.isUnBigDecimal());
        assertTrue(solver.isUseNewton());
        assertEquals(100, solver.getMaxIter());
        assertEquals(1e-12, solver.getTol());
        assertEquals(2, solver.getNewtonInitialX());
        assertEquals(1, solver.getBisectionLeft());
        assertEquals(3, solver.getBisectionRight());
        assertEquals(3, solver.solveNewton(1e-10, 100)[0], 1e-8);
        solver.setNewtonInitialX(3);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{3, 1}, solver.solveNewton(1e-10, 1));
        solver.setNewtonInitialX(-2);
        assertEquals(-2, solver.getNewtonInitialX());
        assertEquals(-3, solver.solveNewton(1e-10, 100)[0], 1e-8);
        assertThrows(ArithmeticException.class, () -> solver.solveNewton(1e-12, 0));
        var constant = equationFor(x -> 4, 3);
        assertTrue(assertThrows(ArithmeticException.class, () -> constant.solveNewton(1e-12, 10)).getMessage().contains("导数过小"));
        var linear = equationFor(x -> 2 * x + 1, 9);
        assertEquals(4, linear.calculation(false).getResult().doubleValue(), 1e-8);
        assertThrows(UnsupportedOperationException.class, linear::convertToMultiPrecisionSupported);
        assertThrows(UnsupportedOperationException.class, () -> linear.calculationBigDecimals(false));
    }

    @Test
    void equationBisectionRespectsBracketsAndSettings() throws Exception {
        var solver = equationFor(x -> x * x, 9);
        solver.setBisectionLeft(0);
        solver.setBisectionRight(8);
        solver.setUseNewton(false);
        solver.setTol(1e-10);
        solver.setMaxIter(80);
        assertEquals(0, solver.getBisectionLeft());
        assertEquals(8, solver.getBisectionRight());
        assertFalse(solver.isUseNewton());
        assertEquals(1e-10, solver.getTol());
        assertEquals(80, solver.getMaxIter());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{3, 3}, solver.solveBisection(1e-10, 80));
        assertEquals(3, solver.calculation(true).getResult().doubleValue(), 1e-8);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{0, 0}, solver.solveBisection(1e-10, 0));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[]{0, 7}, solver.solveBisection(4, 7));
        solver.setBisectionLeft(4);
        assertThrows(IllegalArgumentException.class, () -> solver.solveBisection(1e-10, 80));
        solver.setBisectionLeft(3);
        solver.setBisectionRight(5);
        assertEquals(3, solver.solveBisection(1e-9, 80)[0], 1e-7);
    }

    @Test
    void equationCompilerRejectsMalformedEquationsAndReportsUnavailableDependency() {
        for (String expression : new String[]{"x", "x=2=3"}) {
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.EquationSolver.compile(expression, "equation"));
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.EquationSolverExpression.compile(expression, "equation"));
        }
        assertThrows(NumberFormatException.class, () -> net.mooctest.expression.engine.EquationSolver.compile("x=abc", "equation"));
        // The bundled compiler generates an import of the original upstream package.
        var failure = assertThrows(RuntimeException.class, () -> net.mooctest.expression.engine.DynamicFunctionCompiler.compile("linear", "x + 1", new String[]{"x"}));
        assertEquals("动态函数生成失败", failure.getMessage());
        assertNotNull(failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("编译错误"));
        new net.mooctest.expression.engine.DynamicFunctionCompiler();
        assertEquals("missing", new net.mooctest.expression.engine.EquationSolver.NoRootFoundException("missing").getMessage());
    }

    @Test
    void equationCalculatorRegistrationChecksTypeAndPrecision() {
        String firstName = "equationRegistryOne";
        String secondName = "equationRegistryTwo";
        net.mooctest.expression.engine.CalculationManagement.unregister(firstName);
        net.mooctest.expression.engine.CalculationManagement.unregister(secondName);
        try {
            var first = net.mooctest.expression.engine.SingletonEquationSolving.getInstance(firstName);
            var second = net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(secondName);
            assertSame(first, net.mooctest.expression.engine.SingletonEquationSolving.getInstance(firstName));
            assertSame(second, net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(secondName));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.SingletonEquationSolving.getInstance(secondName));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.SingletonEquationSolvingTwo.getInstance(firstName));
            assertEquals(" x = 3 ", first.formatStr(" x = 3 "));
            assertEquals(" x = 3 ", second.formatStr(" x = 3 "));
            assertThrows(UnsupportedOperationException.class, () -> first.compileBigDecimal("x=3", true));
            assertThrows(UnsupportedOperationException.class, () -> second.compileBigDecimal("x=3", true));
            assertThrows(IllegalArgumentException.class, () -> first.compile("bad", true));
            assertThrows(IllegalArgumentException.class, () -> second.compile("bad", true));
            assertThrows(IllegalArgumentException.class, () -> first.calculation("bad", false));
            assertThrows(IllegalArgumentException.class, () -> second.calculation("bad", false));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(firstName);
            net.mooctest.expression.engine.CalculationManagement.unregister(secondName);
        }
    }

    @Test
    void jvmFunctionSignatureValidationAndRegistration() {
        var methods = net.mooctest.expression.engine.JvmExpressionFunction.getRegisteredMethods();
        var saved = new java.util.LinkedHashMap<>(methods);
        try {
            methods.clear();
            assertEquals("public static double f() { return 2; }", net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("f()=2"));
            assertEquals("public static double sum(double x, double y) { return x+y; }", net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("sum(x, y)=x+y"));
            for (String invalid : new String[]{"f(x)", "f(x)=x=2", "f=x", "f(x=x", "f1(x)=x"}) {
                assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource(invalid));
            }
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.registerJvmFunction("bad"));
            assertThrows(IllegalArgumentException.class, () -> net.mooctest.expression.engine.JvmExpressionFunction.registerJvmFunction("private double f() {}"));
            net.mooctest.expression.engine.JvmExpressionFunction.registerFunction("square(x)=x*x");
            assertEquals("public static double square(double x) { return x*x; }", methods.get("double square"));
            assertEquals(1, methods.size());
        } finally {
            methods.clear();
            methods.putAll(saved);
        }
    }

}
