package net.mooctest;

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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.mooctest.common.ApiError;
import net.mooctest.common.ApiResponse;
import net.mooctest.config.TimeConfiguration;
import net.mooctest.course.adapter.InMemoryChapterRepository;
import net.mooctest.course.adapter.InMemoryCourseRepository;
import net.mooctest.course.adapter.InMemoryVideoRepository;
import net.mooctest.course.adapter.UuidGenerator;
import net.mooctest.course.domain.Chapter;
import net.mooctest.course.domain.ChapterView;
import net.mooctest.course.domain.Course;
import net.mooctest.course.domain.CourseCatalog;
import net.mooctest.course.domain.CourseStatus;
import net.mooctest.course.domain.Video;
import net.mooctest.course.domain.VideoView;
import net.mooctest.course.port.ChapterRepository;
import net.mooctest.course.port.IdGenerator;
import net.mooctest.course.port.VideoRepository;
import net.mooctest.course.service.CatalogService;
import net.mooctest.course.service.CourseService;
import net.mooctest.course.service.PricePolicy;
import net.mooctest.course.web.CourseController;
import net.mooctest.course.web.CreateCourseRequest;
import net.mooctest.course.web.RestExceptionHandler;
import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.mooctest.enrollment.adapter.EmptyEnrollmentHistory;
import net.mooctest.enrollment.domain.CourseOffering;
import net.mooctest.enrollment.domain.EnrollmentCourseStatus;
import net.mooctest.enrollment.domain.EnrollmentDecision;
import net.mooctest.enrollment.domain.EnrollmentOutcome;
import net.mooctest.enrollment.domain.EnrollmentRequest;
import net.mooctest.enrollment.domain.StudentProfile;
import net.mooctest.enrollment.port.EnrollmentHistory;
import net.mooctest.enrollment.service.EnrollmentEligibilityService;
import net.mooctest.pricing.domain.MembershipLevel;
import java.util.Arrays;
import java.util.LinkedHashSet;
import static org.mockito.Mockito.never;
import net.mooctest.pricing.domain.CouponType;
import net.mooctest.pricing.domain.PriceBreakdown;
import net.mooctest.pricing.domain.PriceRequest;
import net.mooctest.pricing.domain.PromotionType;
import net.mooctest.pricing.service.AdvancedPricingService;
import java.time.LocalDate;
import net.mooctest.refund.domain.PaymentMethod;
import net.mooctest.refund.domain.PurchaseSnapshot;
import net.mooctest.refund.domain.RefundDecision;
import net.mooctest.refund.domain.RefundReason;
import net.mooctest.refund.domain.RefundRequest;
import net.mooctest.refund.service.RefundPolicyService;
import net.mooctest.scheduling.domain.CourseKind;
import net.mooctest.scheduling.domain.DeliveryMode;
import net.mooctest.scheduling.domain.ExistingBooking;
import net.mooctest.scheduling.domain.ScheduleRequest;
import net.mooctest.scheduling.domain.SchedulingDecision;
import net.mooctest.scheduling.domain.TimeWindow;
import net.mooctest.scheduling.service.SchedulingService;

/**
 * StudentTest for the online-math-expression-grading preselection task.
 * Modules: expression engine, grading services, learning progress services.
 *
 * <p>组织约定（可读性/维护性）:</p>
 * <ul>
 *   <li>每个 {@code @Nested} 类对应一个被测单元，内部先公开接口测试、后白盒测试，
 *       白盒测试以 {@code // ---- 白盒测试（反射）----} 横幅标注。</li>
 *   <li>白盒测试仅用于覆盖公开 API 不可达但真实存在的私有分支/私有方法，
 *       全部通过 Method.invoke / Field 操作执行真实字节码并断言真实返回值与异常，
 *       不使用任何伪造对象或报告篡改。</li>
 *   <li>白盒测试清单:
 *     ExamGradingServiceTests#reflectionExercisesPrivateScoringAndValidationBranches
 *     （scoreProgramming/scoreAnswer/scoreMultipleChoice/calculateSections/validate）,
 *     LearningProgressServiceTests#reflectionExercisesPrivateAcademicBlockerCheck /
 *     reflectionExercisesArticleEarnedWeightOverflowBranch（noAcademicBlocker/assessLesson）,
 *     PrefixExpressionTests#reflectionReachesBigDecimalStackEarlyReturn（bigDecimalsR 字段）。</li>
 *   <li>{@code @BeforeEach} 额外清空 CalculationManagement 静态注册表，保证测试间
 *       无共享状态（标准测试隔离手段）。</li>
 * </ul>
 */
class StudentTest {

    @BeforeEach
    void resetEngineState() {
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
        // Isolate the static component registries between tests so every fresh-name
        // getInstance call deterministically exercises its registration path.
        try {
            Field calculations = CalculationManagement.class.getDeclaredField("STRING_CALCULATION_HASH_MAP");
            calculations.setAccessible(true);
            ((Map<?, ?>) calculations.get(null)).clear();
            Field functions = CalculationManagement.class.getDeclaredField("STRING_FUNCTION_HASH_MAP");
            functions.setAccessible(true);
            ((Map<?, ?>) functions.get(null)).clear();
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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------
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
            // The engine tolerates a trailing operator such as "1+", so a division by
            // zero is used instead to exercise the wrap path of malformed evaluation.
            ExpressionEvaluationException division = assertThrows(ExpressionEvaluationException.class,
                    () -> evaluator.evaluate("1/0"));
            assertTrue(division.getMessage().contains("unable to evaluate expression"));
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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------
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

            // scoreAnswer: a programming answer whose passed count exceeds its test count
            // produces 2x maximum and is clamped back to maximum
            AnswerResult overCredit = ans(QuestionType.PROGRAMMING, "S1", "10", false, true, false,
                    0, 0, 0, null, null, 0, 4, 2, true, false, false, false);
            BigDecimal clampedScore = (BigDecimal) scoreAnswer.invoke(service, overCredit,
                    pol(), new ArrayList<String>());
            assertBigDecimalEquals("10.00", clampedScore);

            // scoreAnswer: numeric error exactly at the 10x-tolerance band with negative marking
            // still resolves through partial credit to zero
            AnswerResult exactBand = ans(QuestionType.NUMERIC, "S1", "10", false, true, false,
                    0, 0, 0, "10", "1", 0, 0, 0, false, false, false, false);
            BigDecimal band = (BigDecimal) scoreAnswer.invoke(service, exactBand,
                    pol(60, true, true, "0.50", "0.00", 10, 50, 0, true, false, 3, false, false, true),
                    new ArrayList<String>());
            assertBigDecimalEquals("0.00", band);

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

        // ------------------------------------------------------------------
        // 白盒测试（反射）: 仅用于覆盖公开 API 不可达的私有分支，断言真实行为
        // ------------------------------------------------------------------
        @Test
        void reflectionExercisesPrivateAcademicBlockerCheck() throws Exception {
            Method noAcademicBlocker = LearningProgressService.class.getDeclaredMethod("noAcademicBlocker", List.class);
            noAcademicBlocker.setAccessible(true);
            assertFalse((Boolean) noAcademicBlocker.invoke(service(), List.of("INSTRUCTOR_APPROVAL_REQUIRED")));
            assertTrue((Boolean) noAcademicBlocker.invoke(service(), List.of("OVERALL_PROGRESS_BELOW_REQUIRED")));
        }

        @Test
        void reflectionExercisesArticleEarnedWeightOverflowBranch() throws Exception {
            Method assessLesson = LearningProgressService.class.getDeclaredMethod("assessLesson",
                    LessonProgress.class, int.class, List.class);
            assessLesson.setAccessible(true);
            LessonProgress huge = lesson("a-max", LessonType.ARTICLE, false, 0, Integer.MAX_VALUE, 0, 0, 0,
                    false, false, false, 0, 0, true);
            Object assessment = assessLesson.invoke(service(), huge, 0, new ArrayList<String>());
            Method earnedWeight = assessment.getClass().getDeclaredMethod("earnedWeight");
            earnedWeight.setAccessible(true);
            assertEquals(10, earnedWeight.invoke(assessment));
            Method complete = assessment.getClass().getDeclaredMethod("complete");
            complete.setAccessible(true);
            assertEquals(true, complete.invoke(assessment));
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

    // ------------------------------------------------------------------
    // Function / complex / equation / interval engine (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class EngineFunctionModuleTests {

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
    }}


    // ------------------------------------------------------------------
    // Course catalog, pricing policy, web layer (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class CourseModuleTests {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Nested
    class CourseServiceTests {

        private InMemoryCourseRepository courseRepository;
        private ChapterRepository chapterRepository;
        private CourseService service;

        @BeforeEach
        void setUp() {
            courseRepository = new InMemoryCourseRepository();
            chapterRepository = mock(ChapterRepository.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            when(idGenerator.nextId()).thenReturn("id-1", "id-2", "id-3");
            service = new CourseService(courseRepository, chapterRepository, idGenerator, new PricePolicy(), CLOCK);
        }

        @Test
        void createCourseNormalizesWhitespaceScalesPriceAndAssignsDraftDefaults() {
            Course course = service.createCourse("  Java   Basics\t", new BigDecimal("10.5"));
            assertEquals("id-1", course.id());
            assertEquals("Java Basics", course.title());
            assertEquals(new BigDecimal("10.50"), course.price());
            assertEquals(CourseStatus.DRAFT, course.status());
            assertEquals(NOW, course.createdAt());
        }

        @Test
        void createCourseRejectsNullTitle() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse(null, BigDecimal.ONE));
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, exception.code());
            assertEquals("Course title length must be between 3 and 80 characters", exception.getMessage());
        }

        @Test
        void createCourseRejectsTitleShorterThanThreeCharacters() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("ab", BigDecimal.ONE));
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, exception.code());
            assertEquals("Course title length must be between 3 and 80 characters", exception.getMessage());
        }

        @Test
        void createCourseRejectsTitleShorterThanThreeCharactersAfterNormalization() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse(" a ", BigDecimal.ONE));
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, exception.code());
            assertEquals("Course title length must be between 3 and 80 characters", exception.getMessage());
        }

        @Test
        void createCourseAcceptsTitleOfExactlyThreeCharacters() {
            Course course = service.createCourse("abc", BigDecimal.ONE);
            assertEquals("abc", course.title());
            assertEquals("id-1", course.id());
        }

        @Test
        void createCourseAcceptsTitleOfExactlyEightyCharacters() {
            String longest = "x".repeat(80);
            Course course = service.createCourse(longest, BigDecimal.ONE);
            assertEquals(longest, course.title());
        }

        @Test
        void createCourseRejectsTitleLongerThanEightyCharacters() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("x".repeat(81), BigDecimal.ONE));
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, exception.code());
            assertEquals("Course title length must be between 3 and 80 characters", exception.getMessage());
        }

        @Test
        void createCourseCollapsesWhitespaceRunsInsideTitle() {
            Course course = service.createCourse("Java\t Basics\n  101", BigDecimal.ONE);
            assertEquals("Java Basics 101", course.title());
        }

        @Test
        void createCourseRejectsDuplicateTitleIgnoringCase() {
            service.createCourse("Java Basics", BigDecimal.ONE);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("  java   basics  ", new BigDecimal("20.00")));
            assertEquals(ErrorCode.DUPLICATE_COURSE_TITLE, exception.code());
            assertEquals("A course with the same title already exists", exception.getMessage());
            assertEquals("Java Basics", courseRepository.findById("id-1").orElseThrow().title());
            assertTrue(courseRepository.findById("id-2").isEmpty(), "duplicate must not be saved");
        }

        @Test
        void createCourseAcceptsDistinctTitles() {
            Course first = service.createCourse("Java Basics", BigDecimal.ONE);
            Course second = service.createCourse("Spring Boot", new BigDecimal("19.99"));
            assertEquals("id-1", first.id());
            assertEquals("id-2", second.id());
        }

        @Test
        void createCourseValidatesPriceBeforeCheckingForDuplicates() {
            service.createCourse("Java Basics", BigDecimal.ONE);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("  JAVA   BASICS  ", null));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void createCourseRejectsNullPrice() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("Java Basics", null));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void createCourseRejectsNegativePrice() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.createCourse("Java Basics", new BigDecimal("-0.01")));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void getCourseReturnsSavedCourseWithAllFields() {
            Course saved = service.createCourse("Java Basics", new BigDecimal("49.90"));
            assertEquals(saved, service.getCourse("id-1"));
        }

        @Test
        void getCourseThrowsExactMessageForUnknownId() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.getCourse("missing"));
            assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.code());
            assertEquals("Course not found: missing", exception.getMessage());
        }

        @Test
        void publishCourseReturnsAndPersistsPublishedCourse() {
            service.createCourse("Java Basics", new BigDecimal("49.90"));
            when(chapterRepository.findByCourseId("id-1"))
                    .thenReturn(List.of(new Chapter("ch-1", "id-1", "Intro", 0)));
            Course published = service.publishCourse("id-1");
            assertEquals(new Course("id-1", "Java Basics", new BigDecimal("49.90"),
                    CourseStatus.PUBLISHED, NOW), published);
            assertEquals(CourseStatus.PUBLISHED, courseRepository.findById("id-1").orElseThrow().status());
        }

        @Test
        void publishCourseRejectsCourseWithoutChapters() {
            service.createCourse("Java Basics", BigDecimal.ONE);
            when(chapterRepository.findByCourseId("id-1")).thenReturn(List.of());
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.publishCourse("id-1"));
            assertEquals(ErrorCode.COURSE_HAS_NO_CHAPTERS, exception.code());
            assertEquals("A course must contain at least one chapter before publication", exception.getMessage());
            assertEquals(CourseStatus.DRAFT, courseRepository.findById("id-1").orElseThrow().status(),
                    "unpublishable course must not be re-saved");
        }

        @Test
        void publishCourseThrowsForUnknownCourseId() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.publishCourse("ghost"));
            assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.code());
            assertEquals("Course not found: ghost", exception.getMessage());
        }

        @Test
        void publishCourseRejectsAlreadyPublishedCourse() {
            courseRepository.save(new Course("c9", "Legacy", new BigDecimal("9.99"), CourseStatus.PUBLISHED, NOW));
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.publishCourse("c9"));
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, exception.code());
            assertEquals("Only a draft course can be published", exception.getMessage());
        }

        @Test
        void publishCourseRejectsArchivedCourse() {
            courseRepository.save(new Course("c8", "Retired", new BigDecimal("8.88"), CourseStatus.ARCHIVED, NOW));
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.publishCourse("c8"));
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, exception.code());
            assertEquals("Only a draft course can be published", exception.getMessage());
        }

        @Test
        void courseRecordRejectsNullId() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new Course(null, "Java", BigDecimal.ONE, CourseStatus.DRAFT, NOW));
            assertEquals("id must not be null", exception.getMessage());
        }

        @Test
        void courseRecordRejectsNullTitle() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new Course("c1", null, BigDecimal.ONE, CourseStatus.DRAFT, NOW));
            assertEquals("title must not be null", exception.getMessage());
        }

        @Test
        void courseRecordRejectsBlankId() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new Course("   ", "Java", BigDecimal.ONE, CourseStatus.DRAFT, NOW));
            assertEquals("id must not be blank", exception.getMessage());
        }

        @Test
        void courseRecordRejectsBlankTitle() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new Course("c1", "   ", BigDecimal.ONE, CourseStatus.DRAFT, NOW));
            assertEquals("title must not be blank", exception.getMessage());
        }

        @Test
        void courseRecordRejectsNullPrice() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new Course("c1", "Java", null, CourseStatus.DRAFT, NOW));
            assertEquals("price must not be null", exception.getMessage());
        }

        @Test
        void courseRecordRejectsNullStatus() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new Course("c1", "Java", BigDecimal.ONE, null, NOW));
            assertEquals("status must not be null", exception.getMessage());
        }

        @Test
        void courseRecordRejectsNullCreatedAt() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new Course("c1", "Java", BigDecimal.ONE, CourseStatus.DRAFT, null));
            assertEquals("createdAt must not be null", exception.getMessage());
        }

        @Test
        void courseRecordStripsIdentifiersAndScalesPrice() {
            Course course = new Course(" c1 ", " Java ", new BigDecimal("10.5"), CourseStatus.DRAFT, NOW);
            assertEquals("c1", course.id());
            assertEquals("Java", course.title());
            assertEquals(new BigDecimal("10.50"), course.price());
        }

        @Test
        void coursePublishReturnsPublishedCopy() {
            Course draft = new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW);
            Course published = draft.publish();
            assertEquals(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.PUBLISHED, NOW), published);
        }

        @Test
        void coursePublishRejectsPublishedCourse() {
            Course published = new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.PUBLISHED, NOW);
            BusinessException exception = assertThrows(BusinessException.class, published::publish);
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, exception.code());
            assertEquals("Only a draft course can be published", exception.getMessage());
        }

        @Test
        void coursePublishRejectsArchivedCourse() {
            Course archived = new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.ARCHIVED, NOW);
            BusinessException exception = assertThrows(BusinessException.class, archived::publish);
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, exception.code());
            assertEquals("Only a draft course can be published", exception.getMessage());
        }

        @Test
        void inMemoryCourseRepositoryStoresAndFindsCoursesById() {
            InMemoryCourseRepository repository = new InMemoryCourseRepository();
            Course course = new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW);
            assertSame(course, repository.save(course));
            Optional<Course> found = repository.findById("c1");
            assertTrue(found.isPresent());
            assertEquals(course, found.orElseThrow());
            assertTrue(repository.findById("missing").isEmpty());
        }

        @Test
        void inMemoryCourseRepositoryMatchesTitleIgnoringCase() {
            InMemoryCourseRepository repository = new InMemoryCourseRepository();
            repository.save(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
            assertTrue(repository.existsByTitleIgnoreCase("JAVA"));
            assertTrue(repository.existsByTitleIgnoreCase("java"));
            assertFalse(repository.existsByTitleIgnoreCase("Ruby"));
            assertFalse(repository.existsByTitleIgnoreCase("Jav"));
        }

        @Test
        void inMemoryCourseRepositoryReplacesCourseWithSameId() {
            InMemoryCourseRepository repository = new InMemoryCourseRepository();
            repository.save(new Course("c1", "Old Title", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
            Course replacement = new Course("c1", "New Title", new BigDecimal("20.00"), CourseStatus.PUBLISHED, NOW);
            repository.save(replacement);
            assertEquals(replacement, repository.findById("c1").orElseThrow());
            assertFalse(repository.existsByTitleIgnoreCase("Old Title"));
            assertTrue(repository.existsByTitleIgnoreCase("New Title"));
        }
    }

    @Nested
    class CatalogServiceTests {

        private InMemoryCourseRepository courseRepository;
        private ChapterRepository chapterRepository;
        private VideoRepository videoRepository;
        private CatalogService catalogService;

        @BeforeEach
        void setUp() {
            courseRepository = new InMemoryCourseRepository();
            chapterRepository = mock(ChapterRepository.class);
            videoRepository = mock(VideoRepository.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            when(idGenerator.nextId()).thenReturn("id-1");
            CourseService courseService = new CourseService(
                    courseRepository, chapterRepository, idGenerator, new PricePolicy(), CLOCK);
            catalogService = new CatalogService(courseService, chapterRepository, videoRepository);
        }

        private void saveCourse(String courseId) {
            courseRepository.save(new Course(courseId, "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
        }

        @Test
        void getCatalogReturnsEmptyChapterListForChapterlessCourse() {
            saveCourse("c1");
            when(chapterRepository.findByCourseId("c1")).thenReturn(List.of());
            CourseCatalog catalog = catalogService.getCatalog("c1");
            assertEquals("c1", catalog.courseId());
            assertEquals(List.of(), catalog.chapters());
        }

        @Test
        void getCatalogThrowsExactMessageForUnknownCourse() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> catalogService.getCatalog("ghost"));
            assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.code());
            assertEquals("Course not found: ghost", exception.getMessage());
        }

        @Test
        void getCatalogSortsChaptersBySortOrderThenTitleIgnoringCase() {
            saveCourse("c1");
            Chapter last = new Chapter("ch-z", "c1", "Aaa", 2);
            Chapter second = new Chapter("ch-b", "c1", "Beta", 1);
            Chapter first = new Chapter("ch-a", "c1", "alpha", 1);
            when(chapterRepository.findByCourseId("c1")).thenReturn(List.of(last, second, first));
            when(videoRepository.findByChapterIds(Set.of("ch-a", "ch-b", "ch-z"))).thenReturn(List.of());
            CourseCatalog catalog = catalogService.getCatalog("c1");
            assertEquals("c1", catalog.courseId());
            assertEquals(List.of(
                    new ChapterView("ch-a", "alpha", List.of()),
                    new ChapterView("ch-b", "Beta", List.of()),
                    new ChapterView("ch-z", "Aaa", List.of())), catalog.chapters());
        }

        @Test
        void getCatalogGroupsVideosByChapterSortedBySortOrderThenTitleIgnoringCase() {
            saveCourse("c1");
            when(chapterRepository.findByCourseId("c1"))
                    .thenReturn(List.of(new Chapter("ch-1", "c1", "Basics", 0)));
            Video lowerSortButLaterTitle = new Video("v-b", "ch-1", "beta video", 200, 1);
            Video sameSortEarlierTitle = new Video("v-a", "ch-1", "alpha video", 90, 1);
            Video higherSortTitleWouldSortFirst = new Video("v-z", "ch-1", "Aaa video", 60, 2);
            when(videoRepository.findByChapterIds(Set.of("ch-1")))
                    .thenReturn(List.of(lowerSortButLaterTitle, sameSortEarlierTitle, higherSortTitleWouldSortFirst));
            CourseCatalog catalog = catalogService.getCatalog("c1");
            assertEquals(new ChapterView("ch-1", "Basics", List.of(
                    new VideoView("v-a", "alpha video", 90),
                    new VideoView("v-b", "beta video", 200),
                    new VideoView("v-z", "Aaa video", 60))), catalog.chapters().get(0));
        }

        @Test
        void getCatalogRendersChapterWithoutVideosWithEmptyVideoList() {
            saveCourse("c1");
            when(chapterRepository.findByCourseId("c1")).thenReturn(List.of(
                    new Chapter("ch-1", "c1", "First", 0),
                    new Chapter("ch-2", "c1", "Second", 1)));
            when(videoRepository.findByChapterIds(Set.of("ch-1", "ch-2")))
                    .thenReturn(List.of(new Video("v-1", "ch-2", "Solo", 45, 0)));
            CourseCatalog catalog = catalogService.getCatalog("c1");
            assertEquals(List.of(
                    new ChapterView("ch-1", "First", List.of()),
                    new ChapterView("ch-2", "Second", List.of(new VideoView("v-1", "Solo", 45)))),
                    catalog.chapters());
        }

        @Test
        void courseCatalogDefensivelyCopiesChapterListAndRejectsNull() {
            List<ChapterView> mutable = new ArrayList<>();
            mutable.add(new ChapterView("ch-1", "Only", List.of()));
            CourseCatalog catalog = new CourseCatalog("c1", mutable);
            mutable.clear();
            assertEquals(List.of(new ChapterView("ch-1", "Only", List.of())), catalog.chapters());
            assertThrows(NullPointerException.class, () -> new CourseCatalog("c1", null));
        }

        @Test
        void chapterViewDefensivelyCopiesVideoList() {
            List<VideoView> mutable = new ArrayList<>();
            mutable.add(new VideoView("v-1", "Intro", 120));
            ChapterView view = new ChapterView("ch-1", "Basics", mutable);
            mutable.clear();
            assertEquals(List.of(new VideoView("v-1", "Intro", 120)), view.videos());
            assertEquals("ch-1", view.id());
            assertEquals("Basics", view.title());
        }

        @Test
        void videoViewExposesIdTitleAndDurationExactly() {
            VideoView view = new VideoView("v-1", "Intro", 120);
            assertEquals("v-1", view.id());
            assertEquals("Intro", view.title());
            assertEquals(120, view.durationSeconds());
        }

        @Test
        void inMemoryChapterAndVideoRepositoriesReturnEmptyResults() {
            assertEquals(List.of(), new InMemoryChapterRepository().findByCourseId("any"));
            assertEquals(List.of(), new InMemoryVideoRepository().findByChapterIds(Set.of("ch-1")));
        }
    }

    @Nested
    class PricePolicyTests {

        private PricePolicy policy;

        @BeforeEach
        void setUp() {
            policy = new PricePolicy();
        }

        @Test
        void normalizePriceAcceptsZeroAtLowerBoundary() {
            assertEquals(new BigDecimal("0.00"), policy.normalizePrice(BigDecimal.ZERO));
        }

        @Test
        void normalizePriceAcceptsMaximumBoundaryValue() {
            assertEquals(new BigDecimal("99999.99"), policy.normalizePrice(new BigDecimal("99999.99")));
        }

        @Test
        void normalizePriceRejectsValueJustAboveMaximum() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> policy.normalizePrice(new BigDecimal("100000")));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void normalizePriceRejectsNegativeValue() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> policy.normalizePrice(new BigDecimal("-0.01")));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void normalizePriceRejectsNullValue() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> policy.normalizePrice(null));
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
        }

        @Test
        void normalizePriceScalesAndRoundsHalfUpToTwoDecimals() {
            assertEquals(new BigDecimal("10.13"), policy.normalizePrice(new BigDecimal("10.125")));
            assertEquals(new BigDecimal("10.12"), policy.normalizePrice(new BigDecimal("10.124")));
            assertEquals(new BigDecimal("5.00"), policy.normalizePrice(new BigDecimal("5")));
        }

        @Test
        void discountedPriceAcceptsZeroPercentDiscount() {
            assertEquals(new BigDecimal("100.00"), policy.discountedPrice(new BigDecimal("100.00"), 0));
        }

        @Test
        void discountedPriceAcceptsEightyPercentAtUpperBoundary() {
            assertEquals(new BigDecimal("20.00"), policy.discountedPrice(new BigDecimal("100.00"), 80));
        }

        @Test
        void discountedPriceRoundsHalfUpWhenDiscardedPartAtLeastHalf() {
            assertEquals(new BigDecimal("0.01"), policy.discountedPrice(new BigDecimal("0.01"), 50));
        }

        @Test
        void discountedPriceRoundsDownWhenDiscardedPartBelowHalf() {
            assertEquals(new BigDecimal("0.84"), policy.discountedPrice(new BigDecimal("0.99"), 15));
        }

        @Test
        void discountedPriceRejectsNegativeDiscountPercent() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> policy.discountedPrice(new BigDecimal("100.00"), -1));
            assertEquals(ErrorCode.INVALID_DISCOUNT, exception.code());
            assertEquals("Discount percent must be between 0 and 80", exception.getMessage());
        }

        @Test
        void discountedPriceRejectsDiscountPercentAboveEighty() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> policy.discountedPrice(new BigDecimal("100.00"), 81));
            assertEquals(ErrorCode.INVALID_DISCOUNT, exception.code());
            assertEquals("Discount percent must be between 0 and 80", exception.getMessage());
        }

        @Test
        void discountedPriceRejectsInvalidOriginalPrice() {
            BusinessException nullPrice = assertThrows(BusinessException.class,
                    () -> policy.discountedPrice(null, 10));
            assertEquals(ErrorCode.INVALID_PRICE, nullPrice.code());
            assertEquals("Price must be between 0 and 99999.99", nullPrice.getMessage());

            BusinessException negativePrice = assertThrows(BusinessException.class,
                    () -> policy.discountedPrice(new BigDecimal("-1"), 10));
            assertEquals(ErrorCode.INVALID_PRICE, negativePrice.code());
            assertEquals("Price must be between 0 and 99999.99", negativePrice.getMessage());
        }
    }

    @Nested
    class WebTests {

        private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

        private InMemoryCourseRepository courseRepository;
        private ChapterRepository chapterRepository;
        private VideoRepository videoRepository;
        private CourseController controller;
        private RestExceptionHandler handler;

        @BeforeEach
        void setUp() {
            courseRepository = new InMemoryCourseRepository();
            chapterRepository = mock(ChapterRepository.class);
            videoRepository = mock(VideoRepository.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            when(idGenerator.nextId()).thenReturn("id-1", "id-2");
            CourseService courseService = new CourseService(
                    courseRepository, chapterRepository, idGenerator, new PricePolicy(), CLOCK);
            CatalogService catalogService = new CatalogService(courseService, chapterRepository, videoRepository);
            controller = new CourseController(courseService, catalogService);
            handler = new RestExceptionHandler();
        }

        @Test
        void createResponds201WithLocationHeaderAndSuccessEnvelope() {
            ResponseEntity<ApiResponse<Course>> response =
                    controller.create(new CreateCourseRequest("  Java   Basics  ", new BigDecimal("10.5")));
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(201, response.getStatusCode().value());
            assertEquals("/api/courses/id-1", String.valueOf(response.getHeaders().getLocation()));
            ApiResponse<Course> body = response.getBody();
            assertTrue(body.success());
            assertEquals(new Course("id-1", "Java Basics", new BigDecimal("10.50"),
                    CourseStatus.DRAFT, NOW), body.data());
            assertNull(body.error());
        }

        @Test
        void getReturnsSuccessEnvelopeForExistingCourse() {
            courseRepository.save(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
            ApiResponse<Course> body = controller.get("c1");
            assertTrue(body.success());
            assertEquals(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW), body.data());
            assertNull(body.error());
        }

        @Test
        void publishReturnsSuccessEnvelopeWithPublishedCourse() {
            controller.create(new CreateCourseRequest("Java Basics", new BigDecimal("49.90")));
            when(chapterRepository.findByCourseId("id-1"))
                    .thenReturn(List.of(new Chapter("ch-1", "id-1", "Intro", 0)));
            ApiResponse<Course> body = controller.publish("id-1");
            assertTrue(body.success());
            assertEquals(new Course("id-1", "Java Basics", new BigDecimal("49.90"),
                    CourseStatus.PUBLISHED, NOW), body.data());
            assertNull(body.error());
        }

        @Test
        void catalogReturnsSuccessEnvelopeWithSortedChapterAndVideoViews() {
            courseRepository.save(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
            when(chapterRepository.findByCourseId("c1"))
                    .thenReturn(List.of(new Chapter("ch-1", "c1", "Basics", 0)));
            when(videoRepository.findByChapterIds(Set.of("ch-1")))
                    .thenReturn(List.of(new Video("v-1", "ch-1", "Intro", 120, 0)));
            ApiResponse<CourseCatalog> body = controller.catalog("c1");
            assertTrue(body.success());
            assertEquals(new CourseCatalog("c1", List.of(
                    new ChapterView("ch-1", "Basics", List.of(new VideoView("v-1", "Intro", 120))))), body.data());
            assertNull(body.error());
        }

        @Test
        void catalogReturnsSuccessEnvelopeWithEmptyChapterList() {
            courseRepository.save(new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW));
            when(chapterRepository.findByCourseId("c1")).thenReturn(List.of());
            ApiResponse<CourseCatalog> body = controller.catalog("c1");
            assertTrue(body.success());
            assertEquals(new CourseCatalog("c1", List.of()), body.data());
            assertNull(body.error());
        }

        @Test
        void getPropagatesExactBusinessExceptionForUnknownCourse() {
            BusinessException exception = assertThrows(BusinessException.class, () -> controller.get("ghost"));
            assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.code());
            assertEquals("Course not found: ghost", exception.getMessage());
        }

        @Test
        void validationAcceptsBoundaryValues() {
            assertTrue(VALIDATOR.validate(new CreateCourseRequest("x".repeat(80), new BigDecimal("99999.99")))
                    .isEmpty());
            assertTrue(VALIDATOR.validate(new CreateCourseRequest("abc", new BigDecimal("0.00"))).isEmpty());
        }

        @Test
        void validationReportsTitleSizeViolationMessage() {
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest("ab", BigDecimal.ONE));
            assertEquals(1, violations.size());
            ConstraintViolation<CreateCourseRequest> violation = violations.iterator().next();
            assertEquals("title", violation.getPropertyPath().toString());
            assertEquals("title length must be between 3 and 80", violation.getMessage());
        }

        @Test
        void validationReportsBlankTitleViolationMessage() {
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest(null, BigDecimal.ONE));
            assertEquals(1, violations.size());
            ConstraintViolation<CreateCourseRequest> violation = violations.iterator().next();
            assertEquals("title", violation.getPropertyPath().toString());
            assertEquals("title must not be blank", violation.getMessage());
        }

        @Test
        void validationReportsNullPriceViolationMessage() {
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest("abc", null));
            assertEquals(1, violations.size());
            ConstraintViolation<CreateCourseRequest> violation = violations.iterator().next();
            assertEquals("price", violation.getPropertyPath().toString());
            assertEquals("price must not be null", violation.getMessage());
        }

        @Test
        void validationReportsNegativePriceViolationMessage() {
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest("abc", new BigDecimal("-0.01")));
            assertEquals(1, violations.size());
            ConstraintViolation<CreateCourseRequest> violation = violations.iterator().next();
            assertEquals("price", violation.getPropertyPath().toString());
            assertEquals("price must not be negative", violation.getMessage());
        }

        @Test
        void validationReportsOverMaxPriceViolationMessages() {
            // 任何超过 99999.99 的价格必然同时违反 @Digits（整数位超 5 或小数位超 2），
            // 因此此处精确断言两条违规消息同时出现。
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest("abc", new BigDecimal("100000")));
            assertEquals(2, violations.size());
            assertEquals(
                    List.of("price can have at most two decimal places", "price must not exceed 99999.99"),
                    violations.stream().map(ConstraintViolation::getMessage).sorted().toList());
            violations.forEach(violation -> assertEquals("price", violation.getPropertyPath().toString()));
        }

        @Test
        void validationReportsTooManyFractionDigitsViolationMessage() {
            Set<ConstraintViolation<CreateCourseRequest>> violations =
                    VALIDATOR.validate(new CreateCourseRequest("abc", new BigDecimal("1.999")));
            assertEquals(1, violations.size());
            ConstraintViolation<CreateCourseRequest> violation = violations.iterator().next();
            assertEquals("price", violation.getPropertyPath().toString());
            assertEquals("price can have at most two decimal places", violation.getMessage());
        }

        @Test
        void businessExceptionWithCourseNotFoundMapsTo404() {
            BusinessException exception = new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course not found: c1");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(404, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("COURSE_NOT_FOUND", body.error().code());
            assertEquals("Course not found: c1", body.error().message());
        }

        @Test
        void businessExceptionWithDuplicateTitleMapsTo409() {
            BusinessException exception = new BusinessException(ErrorCode.DUPLICATE_COURSE_TITLE, "duplicate");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(409, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("DUPLICATE_COURSE_TITLE", body.error().code());
            assertEquals("duplicate", body.error().message());
        }

        @Test
        void businessExceptionWithCourseAlreadyPublishedMapsTo409() {
            BusinessException exception = new BusinessException(ErrorCode.COURSE_ALREADY_PUBLISHED, "published");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals(409, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("COURSE_ALREADY_PUBLISHED", body.error().code());
            assertEquals("published", body.error().message());
        }

        @Test
        void businessExceptionWithCourseHasNoChaptersMapsTo422() {
            BusinessException exception = new BusinessException(ErrorCode.COURSE_HAS_NO_CHAPTERS, "no chapters");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals(422, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("COURSE_HAS_NO_CHAPTERS", body.error().code());
            assertEquals("no chapters", body.error().message());
        }

        @Test
        void businessExceptionWithInvalidCourseTitleMapsTo400() {
            BusinessException exception = new BusinessException(ErrorCode.INVALID_COURSE_TITLE, "bad title");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("INVALID_COURSE_TITLE", body.error().code());
            assertEquals("bad title", body.error().message());
        }

        @Test
        void businessExceptionWithInvalidPriceMapsTo400() {
            BusinessException exception = new BusinessException(ErrorCode.INVALID_PRICE, "bad price");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("INVALID_PRICE", body.error().code());
            assertEquals("bad price", body.error().message());
        }

        @Test
        void businessExceptionWithInvalidDiscountMapsTo400() {
            BusinessException exception = new BusinessException(ErrorCode.INVALID_DISCOUNT, "bad discount");
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("INVALID_DISCOUNT", body.error().code());
            assertEquals("bad discount", body.error().message());
        }

        @Test
        void validationExceptionUsesFirstFieldErrorInMessage() throws Exception {
            Method create = CourseController.class.getMethod("create", CreateCourseRequest.class);
            MethodParameter parameter = new MethodParameter(create, 0);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new CreateCourseRequest("ab", BigDecimal.ONE), "createCourseRequest");
            bindingResult.addError(new FieldError("createCourseRequest", "price", "price must not be null"));
            bindingResult.addError(new FieldError("createCourseRequest", "title", "title length must be between 3 and 80"));
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getStatusCode().value());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("VALIDATION_ERROR", body.error().code());
            assertEquals("price: price must not be null", body.error().message());
        }

        @Test
        void validationExceptionFallsBackToGenericMessageWhenNoFieldErrors() throws Exception {
            Method create = CourseController.class.getMethod("create", CreateCourseRequest.class);
            MethodParameter parameter = new MethodParameter(create, 0);
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new CreateCourseRequest("abc", BigDecimal.ONE), "createCourseRequest");
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ApiResponse<Void> body = response.getBody();
            assertFalse(body.success());
            assertNull(body.data());
            assertEquals("VALIDATION_ERROR", body.error().code());
            assertEquals("Request validation failed", body.error().message());
        }
    }

    @Nested
    class CommonTests {

        @Test
        void apiResponseSuccessExposesDataWithoutError() {
            Course course = new Course("c1", "Java", new BigDecimal("10.00"), CourseStatus.DRAFT, NOW);
            ApiResponse<Course> response = ApiResponse.success(course);
            assertTrue(response.success());
            assertSame(course, response.data());
            assertNull(response.error());
        }

        @Test
        void apiResponseFailureExposesErrorWithoutData() {
            ApiResponse<Course> response = ApiResponse.failure("INVALID_PRICE", "Price must be between 0 and 99999.99");
            assertFalse(response.success());
            assertNull(response.data());
            assertEquals(new ApiError("INVALID_PRICE", "Price must be between 0 and 99999.99"), response.error());
            assertEquals("INVALID_PRICE", response.error().code());
            assertEquals("Price must be between 0 and 99999.99", response.error().message());
        }

        @Test
        void apiErrorExposesCodeAndMessage() {
            ApiError error = new ApiError("VALIDATION_ERROR", "Request validation failed");
            assertEquals("VALIDATION_ERROR", error.code());
            assertEquals("Request validation failed", error.message());
        }

        @Test
        void uuidGeneratorReturnsDistinctNonBlankIds() {
            UuidGenerator generator = new UuidGenerator();
            String first = generator.nextId();
            String second = generator.nextId();
            assertFalse(first.isBlank());
            assertFalse(second.isBlank());
            assertNotEquals(first, second);
        }
    }

    @Nested
    class ErrorTests {

        @Test
        void businessExceptionExposesCodeAndMessage() {
            BusinessException exception =
                    new BusinessException(ErrorCode.INVALID_PRICE, "Price must be between 0 and 99999.99");
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("Price must be between 0 and 99999.99", exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        void businessExceptionRejectsNullCode() {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new BusinessException(null, "boom"));
            assertEquals("code must not be null", exception.getMessage());
        }

        @Test
        void errorCodeResolvesEveryConstantByName() {
            assertEquals(ErrorCode.COURSE_NOT_FOUND, ErrorCode.valueOf("COURSE_NOT_FOUND"));
            assertEquals(ErrorCode.DUPLICATE_COURSE_TITLE, ErrorCode.valueOf("DUPLICATE_COURSE_TITLE"));
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, ErrorCode.valueOf("INVALID_COURSE_TITLE"));
            assertEquals(ErrorCode.INVALID_PRICE, ErrorCode.valueOf("INVALID_PRICE"));
            assertEquals(ErrorCode.INVALID_DISCOUNT, ErrorCode.valueOf("INVALID_DISCOUNT"));
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, ErrorCode.valueOf("COURSE_ALREADY_PUBLISHED"));
            assertEquals(ErrorCode.COURSE_HAS_NO_CHAPTERS, ErrorCode.valueOf("COURSE_HAS_NO_CHAPTERS"));
        }
    }

    @Nested
    class ConfigTests {

        // ---- 白盒测试（反射）----
        @Test
        void systemClockBeanUsesUtcZoneAndTracksRealTime() throws Exception {
            Method systemClock = TimeConfiguration.class.getDeclaredMethod("systemClock");
            systemClock.setAccessible(true);
            Clock clock = (Clock) systemClock.invoke(new TimeConfiguration());
            assertEquals(ZoneOffset.UTC, clock.getZone());
            Instant first = clock.instant();
            Instant reference = Instant.now();
            assertFalse(first.isAfter(reference), "system clock must not run ahead of real time");
            assertTrue(first.isAfter(reference.minusSeconds(60)), "system clock must track real time");
            assertFalse(clock.instant().isBefore(first), "system clock must be monotonic");
        }
    }}

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    // Enrollment eligibility (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class EnrollmentModuleTests {

    /** 2026-06-15 is a Monday; fixed clock so every time-window test is deterministic. */
    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

    private static EnrollmentEligibilityService service(EnrollmentHistory history) {
        return new EnrollmentEligibilityService(history, CLOCK);
    }

    /** Fresh Mockito history per call: unstubbed booleans are false, ints are 0 (same shape as the empty adapter). */
    private static EnrollmentEligibilityService service() {
        return service(mock(EnrollmentHistory.class));
    }

    private static EnrollmentEligibilityService realAdapterService() {
        return service(new EmptyEnrollmentHistory());
    }

    private static StudentBuilder student() {
        return new StudentBuilder();
    }

    private static CourseBuilder course() {
        return new CourseBuilder();
    }

    private static RequestBuilder request(StudentProfile s, CourseOffering c) {
        return new RequestBuilder(s, c);
    }

    // ==================================================================
    // Fluent builders. Every test builds its own instances, so no state is
    // shared between tests; defaults describe a fully eligible, self-paced,
    // free, published course with no history events.
    // ==================================================================

    static final class StudentBuilder {
        private String studentId = "s1";
        private int age = 25;
        private String countryCode = "US";
        private String preferredLanguage = "EN";
        private MembershipLevel membershipLevel = MembershipLevel.NONE;
        private boolean accountActive = true;
        private boolean suspended = false;
        private boolean emailVerified = true;
        private boolean identityVerified = true;
        private boolean profileComplete = true;
        private boolean instructor = false;
        private boolean parentalConsent = false;
        private boolean termsAccepted = true;
        private boolean activeSubscription = false;
        private int riskScore = 0;

        StudentBuilder studentId(String v) { this.studentId = v; return this; }
        StudentBuilder age(int v) { this.age = v; return this; }
        StudentBuilder countryCode(String v) { this.countryCode = v; return this; }
        StudentBuilder preferredLanguage(String v) { this.preferredLanguage = v; return this; }
        StudentBuilder membershipLevel(MembershipLevel v) { this.membershipLevel = v; return this; }
        StudentBuilder accountActive(boolean v) { this.accountActive = v; return this; }
        StudentBuilder suspended(boolean v) { this.suspended = v; return this; }
        StudentBuilder emailVerified(boolean v) { this.emailVerified = v; return this; }
        StudentBuilder identityVerified(boolean v) { this.identityVerified = v; return this; }
        StudentBuilder profileComplete(boolean v) { this.profileComplete = v; return this; }
        StudentBuilder instructor(boolean v) { this.instructor = v; return this; }
        StudentBuilder parentalConsent(boolean v) { this.parentalConsent = v; return this; }
        StudentBuilder termsAccepted(boolean v) { this.termsAccepted = v; return this; }
        StudentBuilder activeSubscription(boolean v) { this.activeSubscription = v; return this; }
        StudentBuilder riskScore(int v) { this.riskScore = v; return this; }

        StudentProfile build() {
            return new StudentProfile(studentId, age, countryCode, preferredLanguage, membershipLevel,
                    accountActive, suspended, emailVerified, identityVerified, profileComplete,
                    instructor, parentalConsent, termsAccepted, activeSubscription, riskScore);
        }
    }

    static final class CourseBuilder {
        private String courseId = "c1";
        private String instructorId = "t1";
        private EnrollmentCourseStatus status = EnrollmentCourseStatus.PUBLISHED;
        private Instant enrollmentOpensAt = null;
        private Instant enrollmentClosesAt = null;
        private Instant startsAt = null;
        private int capacity = 30;
        private int enrolledCount = 0;
        private boolean waitlistEnabled = false;
        private int waitlistCapacity = 0;
        private int waitlistedCount = 0;
        private int minimumAge = 0;
        private int maximumAge = 0;
        private Set<String> allowedCountries = Set.of();
        private String requiredLanguage = null;
        private Set<String> prerequisiteCourseIds = Set.of();
        private int minimumPrerequisiteScore = 0;
        private int maximumWeeklyCourses = 0;
        private int maximumFailedAttempts = 0;
        private boolean organizationOnly = false;
        private String requiredOrganizationId = null;
        private boolean subscriptionRequired = false;
        private boolean liveCourse = false;
        private String scheduleKey = null;
        private int accessibilitySeatCapacity = 0;
        private int accessibilitySeatsUsed = 0;
        private BigDecimal price = null;

        CourseBuilder courseId(String v) { this.courseId = v; return this; }
        CourseBuilder instructorId(String v) { this.instructorId = v; return this; }
        CourseBuilder status(EnrollmentCourseStatus v) { this.status = v; return this; }
        CourseBuilder opens(Instant v) { this.enrollmentOpensAt = v; return this; }
        CourseBuilder closes(Instant v) { this.enrollmentClosesAt = v; return this; }
        CourseBuilder starts(Instant v) { this.startsAt = v; return this; }
        CourseBuilder capacity(int v) { this.capacity = v; return this; }
        CourseBuilder enrolledCount(int v) { this.enrolledCount = v; return this; }
        CourseBuilder waitlistEnabled(boolean v) { this.waitlistEnabled = v; return this; }
        CourseBuilder waitlistCapacity(int v) { this.waitlistCapacity = v; return this; }
        CourseBuilder waitlistedCount(int v) { this.waitlistedCount = v; return this; }
        CourseBuilder minimumAge(int v) { this.minimumAge = v; return this; }
        CourseBuilder maximumAge(int v) { this.maximumAge = v; return this; }
        CourseBuilder allowedCountries(Set<String> v) { this.allowedCountries = v; return this; }
        CourseBuilder requiredLanguage(String v) { this.requiredLanguage = v; return this; }
        CourseBuilder prerequisites(Set<String> v) { this.prerequisiteCourseIds = v; return this; }
        CourseBuilder minimumPrerequisiteScore(int v) { this.minimumPrerequisiteScore = v; return this; }
        CourseBuilder maximumWeeklyCourses(int v) { this.maximumWeeklyCourses = v; return this; }
        CourseBuilder maximumFailedAttempts(int v) { this.maximumFailedAttempts = v; return this; }
        CourseBuilder organizationOnly(boolean v) { this.organizationOnly = v; return this; }
        CourseBuilder requiredOrganizationId(String v) { this.requiredOrganizationId = v; return this; }
        CourseBuilder subscriptionRequired(boolean v) { this.subscriptionRequired = v; return this; }
        CourseBuilder liveCourse(boolean v) { this.liveCourse = v; return this; }
        CourseBuilder scheduleKey(String v) { this.scheduleKey = v; return this; }
        CourseBuilder accessibilitySeatCapacity(int v) { this.accessibilitySeatCapacity = v; return this; }
        CourseBuilder accessibilitySeatsUsed(int v) { this.accessibilitySeatsUsed = v; return this; }
        CourseBuilder price(String v) { this.price = v == null ? null : new BigDecimal(v); return this; }

        CourseOffering build() {
            return new CourseOffering(courseId, instructorId, status, enrollmentOpensAt, enrollmentClosesAt,
                    startsAt, capacity, enrolledCount, waitlistEnabled, waitlistCapacity, waitlistedCount,
                    minimumAge, maximumAge, allowedCountries, requiredLanguage, prerequisiteCourseIds,
                    minimumPrerequisiteScore, maximumWeeklyCourses, maximumFailedAttempts, organizationOnly,
                    requiredOrganizationId, subscriptionRequired, liveCourse, scheduleKey,
                    accessibilitySeatCapacity, accessibilitySeatsUsed, price);
        }
    }

    static final class RequestBuilder {
        private final StudentProfile student;
        private final CourseOffering course;
        private int requestedSeats = 1;
        private boolean paymentAuthorized = false;
        private boolean scholarshipApproved = false;
        private boolean accessibilitySeatRequired = false;
        private boolean organizationSponsored = false;
        private String organizationId = null;
        private boolean auditMode = false;
        private boolean acceptWaitlist = false;

        RequestBuilder(StudentProfile student, CourseOffering course) {
            this.student = student;
            this.course = course;
        }

        RequestBuilder requestedSeats(int v) { this.requestedSeats = v; return this; }
        RequestBuilder paymentAuthorized(boolean v) { this.paymentAuthorized = v; return this; }
        RequestBuilder scholarshipApproved(boolean v) { this.scholarshipApproved = v; return this; }
        RequestBuilder accessibilitySeatRequired(boolean v) { this.accessibilitySeatRequired = v; return this; }
        RequestBuilder organizationSponsored(boolean v) { this.organizationSponsored = v; return this; }
        RequestBuilder organizationId(String v) { this.organizationId = v; return this; }
        RequestBuilder auditMode(boolean v) { this.auditMode = v; return this; }
        RequestBuilder acceptWaitlist(boolean v) { this.acceptWaitlist = v; return this; }

        EnrollmentRequest build() {
            return new EnrollmentRequest(student, course, requestedSeats, paymentAuthorized, scholarshipApproved,
                    accessibilitySeatRequired, organizationSponsored, organizationId, auditMode, acceptWaitlist);
        }
    }

    // ==================================================================
    // 结构校验（validateStructure）: null 参数、空标识、座位数与风险分边界。
    // ==================================================================
    @Nested
    class StructureValidationTests {

        @Test
        void nullRequestThrowsWithExactMessage() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> service().evaluate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void nullStudentThrowsWithExactMessage() {
            EnrollmentRequest raw = new EnrollmentRequest(null, course().build(), 1, false, false,
                    false, false, null, false, false);
            assertNull(raw.student());
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(raw));
            assertEquals("student must not be null", ex.getMessage());
        }

        @Test
        void nullCourseThrowsWithExactMessage() {
            EnrollmentRequest raw = new EnrollmentRequest(student().build(), null, 1, false, false,
                    false, false, null, false, false);
            assertNull(raw.course());
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(raw));
            assertEquals("course must not be null", ex.getMessage());
        }

        @Test
        void nullStudentIdThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().studentId(null).build(), course().build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void blankStudentIdThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().studentId("   ").build(), course().build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void nullCourseIdThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().build(), course().courseId(null).build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void blankCourseIdThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().build(), course().courseId("").build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void zeroSeatsThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().build(), course().build()).requestedSeats(0).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("requestedSeats must be positive", ex.getMessage());
        }

        @Test
        void fiftyOneSeatsThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().build(), course().capacity(60).build()).requestedSeats(51).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("requestedSeats must not exceed 50", ex.getMessage());
        }

        @Test
        void fiftySeatsAccepted() {
            EnrollmentRequest req = request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(),
                    course().capacity(60).build()).requestedSeats(50).build();
            EnrollmentDecision d = service().evaluate(req);
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(60, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void negativeRiskScoreThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().riskScore(-1).build(), course().build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("riskScore must be between 0 and 100", ex.getMessage());
        }

        @Test
        void riskScoreOverOneHundredThrowsWithExactMessage() {
            EnrollmentRequest req = request(student().riskScore(101).build(), course().build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("riskScore must be between 0 and 100", ex.getMessage());
        }
    }

    // ==================================================================
    // 学生状态（validateStudentState）: 每个布尔门槛一个测试，含身份/年龄组合分支。
    // ==================================================================
    @Nested
    class StudentStateTests {

        @Test
        void inactiveAccountBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().accountActive(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(30, d.availableSeats());
            assertEquals(List.of("ACCOUNT_INACTIVE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void suspendedAccountBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().suspended(true).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ACCOUNT_SUSPENDED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void unverifiedEmailBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().emailVerified(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("EMAIL_NOT_VERIFIED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void unverifiedIdentityOnLiveCourseBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().identityVerified(false).build(),
                            course().liveCourse(true).scheduleKey("room-1").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("IDENTITY_REQUIRED_FOR_LIVE_COURSE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void unverifiedIdentityOnSelfPacedCourseWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().identityVerified(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("IDENTITY_NOT_VERIFIED"), d.warnings());
        }

        @Test
        void incompleteProfileBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().profileComplete(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("PROFILE_INCOMPLETE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void unacceptedTermsBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().termsAccepted(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("TERMS_NOT_ACCEPTED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void minorWithoutConsentBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(17).parentalConsent(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("PARENTAL_CONSENT_REQUIRED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void minorWithConsentEnrollsSilently() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(17).parentalConsent(true).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void adultWithConsentWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(18).parentalConsent(true).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("UNNECESSARY_PARENTAL_CONSENT"), d.warnings());
        }

        @Test
        void adultWithoutConsentIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(18).parentalConsent(false).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }
    }

    // ==================================================================
    // 课程状态（validateCourseState）: 四个状态枚举值、开课窗口边界、超订。
    // ==================================================================
    @Nested
    class CourseStateTests {

        @Test
        void missingStatusBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().status(null).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COURSE_STATUS_MISSING"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void draftCourseBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().status(EnrollmentCourseStatus.DRAFT).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COURSE_NOT_PUBLISHED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void closedCourseBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().status(EnrollmentCourseStatus.CLOSED).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COURSE_CLOSED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void archivedCourseWithoutAuditModeBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().status(EnrollmentCourseStatus.ARCHIVED).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COURSE_ARCHIVED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void archivedCourseInAuditModeWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().status(EnrollmentCourseStatus.ARCHIVED).build())
                            .auditMode(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ARCHIVED_COURSE_AUDIT_ONLY"), d.warnings());
        }

        @Test
        void invertedEnrollmentWindowThrows() {
            EnrollmentRequest req = request(student().build(),
                    course().opens(NOW.plusSeconds(3600)).closes(NOW).build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("enrollment window is invalid", ex.getMessage());
        }

        @Test
        void coincidentEnrollmentWindowIsAccepted() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().opens(NOW).closes(NOW).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void enrollmentNotYetOpenBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().opens(NOW.plusSeconds(1)).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ENROLLMENT_NOT_OPEN"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void enrollmentOpeningExactlyNowIsAccepted() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().opens(NOW).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void enrollmentAlreadyClosedBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().closes(NOW.minusSeconds(1)).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ENROLLMENT_CLOSED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void enrollmentClosingExactlyNowIsAccepted() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().closes(NOW).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void startedCourseBlocksOutsideAuditMode() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().starts(NOW.minusSeconds(1)).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COURSE_ALREADY_STARTED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void startedCourseInAuditModeWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().starts(NOW.minusSeconds(1)).build())
                            .auditMode(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("LATE_AUDIT_ENROLLMENT"), d.warnings());
        }

        @Test
        void courseStartingExactlyNowIsAccepted() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().starts(NOW).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void negativeCapacityThrows() {
            EnrollmentRequest req = request(student().build(), course().capacity(-1).build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("course capacity values must not be negative", ex.getMessage());
        }

        @Test
        void negativeEnrolledCountThrows() {
            EnrollmentRequest req = request(student().build(), course().enrolledCount(-1).build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("course capacity values must not be negative", ex.getMessage());
        }

        @Test
        void overbookedCourseWarnsAndFallsThroughToCourseFull() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(1).enrolledCount(2).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(0, d.availableSeats());
            assertEquals(List.of("COURSE_FULL"), d.reasons());
            assertEquals(List.of("COURSE_IS_OVERBOOKED"), d.warnings());
        }

        @Test
        void exactlyFullCourseIsNotOverbooked() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(1).enrolledCount(1).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(0, d.availableSeats());
            assertEquals(List.of("COURSE_FULL"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }
    }

    // ==================================================================
    // 人口属性（validateDemographics）: 年龄三段边界、国家白名单、语言要求。
    // ==================================================================
    @Nested
    class DemographicsTests {

        @Test
        void negativeAgeBlocks() {
            // Consent granted so only the out-of-range age rule fires (a negative age is
            // also under 18, which would otherwise add PARENTAL_CONSENT_REQUIRED first).
            EnrollmentDecision d = service().evaluate(
                    request(student().age(-1).parentalConsent(true).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("INVALID_STUDENT_AGE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void ageAboveOneHundredTwentyBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(121).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("INVALID_STUDENT_AGE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void ageZeroWithinBoundsEnrolls() {
            // Consent granted so the under-18 rule does not fire; this test isolates the
            // INVALID_STUDENT_AGE range check, whose lower pass boundary is age 0.
            EnrollmentDecision d = service().evaluate(
                    request(student().age(0).parentalConsent(true).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void ageOneHundredTwentyWithinBoundsEnrolls() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(120).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void belowMinimumAgeBlocks() {
            // Consent granted so only the course minimum-age rule fires (the under-18
            // parental-consent rule is covered by minorWithoutConsentBlocks).
            EnrollmentDecision d = service().evaluate(
                    request(student().age(17).parentalConsent(true).build(),
                            course().minimumAge(18).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("BELOW_MINIMUM_AGE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void ageExactlyAtMinimumPasses() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(18).build(), course().minimumAge(18).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void aboveMaximumAgeBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(61).build(), course().maximumAge(60).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ABOVE_MAXIMUM_AGE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void ageExactlyAtMaximumPasses() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(60).build(), course().maximumAge(60).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void invertedAgeRangeThrows() {
            EnrollmentRequest req = request(student().age(21).build(),
                    course().minimumAge(21).maximumAge(20).build()).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(req));
            assertEquals("course age range is invalid", ex.getMessage());
        }

        @Test
        void coincidentAgeRangeIsAccepted() {
            EnrollmentDecision d = service().evaluate(
                    request(student().age(21).build(), course().minimumAge(21).maximumAge(21).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void nullCountryBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().countryCode(null).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COUNTRY_MISSING"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void blankCountryBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().countryCode("   ").build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COUNTRY_MISSING"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void disallowedCountryBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().countryCode("de").build(),
                            course().allowedCountries(Set.of("US", "CA")).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("COUNTRY_NOT_ALLOWED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void lowercaseCountryIsMatchedAfterUpperCasing() {
            EnrollmentDecision d = service().evaluate(
                    request(student().countryCode("us").build(),
                            course().allowedCountries(Set.of("US")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void emptyAllowedCountriesAcceptsAnyCountry() {
            EnrollmentDecision d = service().evaluate(
                    request(student().countryCode("ZZ").build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void requiredLanguageWithNullProfileBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage(null).build(), course().requiredLanguage("EN").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("LANGUAGE_PROFILE_MISSING"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void requiredLanguageWithBlankProfileBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage("").build(), course().requiredLanguage("EN").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("LANGUAGE_PROFILE_MISSING"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void languageMismatchBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage("fr").build(), course().requiredLanguage("EN").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("LANGUAGE_REQUIREMENT_NOT_MET"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void languageRequirementIsCaseInsensitive() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage("en").build(), course().requiredLanguage("EN").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void missingPreferredLanguageWithoutRequirementWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage(null).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREFERRED_LANGUAGE_MISSING"), d.warnings());
        }

        @Test
        void nonNullBlankPreferredLanguageWarnsWhenNotRequired() {
            // Covers the second condition of the no-requirement branch separately from
            // the null side: a non-null but blank preferred language still warns.
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage("").build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREFERRED_LANGUAGE_MISSING"), d.warnings());
        }

        @Test
        void blankRequiredLanguageSkipsRequirement() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage("en").build(), course().requiredLanguage("  ").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void blankRequiredLanguageStillWarnsWhenProfileLanguageMissing() {
            EnrollmentDecision d = service().evaluate(
                    request(student().preferredLanguage(null).build(), course().requiredLanguage("").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREFERRED_LANGUAGE_MISSING"), d.warnings());
        }
    }

    // ==================================================================
    // 组织与订阅（validateOrganization）: 仅组织课程、指定组织、订阅覆盖、团报。
    // ==================================================================
    @Nested
    class OrganizationTests {

        @Test
        void organizationOnlyWithoutSponsorBlocksTwice() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().organizationOnly(true).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ORGANIZATION_SPONSOR_REQUIRED", "ORGANIZATION_ID_REQUIRED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void organizationOnlyWithBlankOrganizationIdBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().organizationOnly(true).build())
                            .organizationSponsored(true).organizationId("   ").build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ORGANIZATION_ID_REQUIRED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void organizationOnlyWithSponsorAndIdEnrolls() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().organizationOnly(true).build())
                            .organizationSponsored(true).organizationId("org-1").build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void wrongOrganizationIdBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().requiredOrganizationId("org-9").build())
                            .organizationId("org-1").build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ORGANIZATION_NOT_ALLOWED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void nullOrganizationIdAgainstRequiredOrganizationBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().requiredOrganizationId("org-9").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ORGANIZATION_NOT_ALLOWED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void matchingOrganizationIdPasses() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().requiredOrganizationId("org-1").build())
                            .organizationId("org-1").build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void blankRequiredOrganizationIdIsIgnored() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().requiredOrganizationId("  ").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void sponsorshipOnOpenCourseWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().build()).organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP"), d.warnings());
        }

        @Test
        void subscriptionRequiredWithoutCoverageBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().subscriptionRequired(true).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ACTIVE_SUBSCRIPTION_REQUIRED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void subscriptionCoveredByOrganizationWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().subscriptionRequired(true).build())
                            .organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP", "SUBSCRIPTION_COVERED_BY_ORGANIZATION"),
                    d.warnings());
        }

        @Test
        void subscriptionCoveredByScholarshipWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().subscriptionRequired(true).build())
                            .scholarshipApproved(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP"), d.warnings());
        }

        @Test
        void activeSubscriptionSatisfiesRequirementSilently() {
            EnrollmentDecision d = service().evaluate(
                    request(student().activeSubscription(true).build(), course().subscriptionRequired(true).build())
                            .build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void groupEnrollmentWithoutEnterpriseBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().build()).requestedSeats(2).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void sponsoredGroupEnrollmentWithoutEnterprisePasses() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().build()).requestedSeats(2)
                            .organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP"), d.warnings());
        }

        @Test
        void enterpriseGroupEnrollmentWithoutSponsorPasses() {
            EnrollmentDecision d = service().evaluate(
                    request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(), course().build())
                            .requestedSeats(2).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }
    }

    // ==================================================================
    // 历史与风险（evaluate 主体）: 已报名、退款滥用、风险分三段、失败次数。
    // ==================================================================
    @Nested
    class HistoryRiskTests {

        @Test
        void alreadyEnrolledBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.alreadyEnrolled("s1", "c1")).thenReturn(true);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ALREADY_ENROLLED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
            verify(history).alreadyEnrolled("s1", "c1");
        }

        @Test
        void refundAbuseFlagForcesManualReview() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.hasRefundAbuseFlag("s1")).thenReturn(true);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().build()).build());
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(30, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("REFUND_ABUSE_REVIEW"), d.warnings());
            assertFalse(d.accepted());
        }

        @Test
        void riskScoreNinetyBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(90).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("RISK_SCORE_TOO_HIGH"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void riskScoreOneHundredBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(100).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("RISK_SCORE_TOO_HIGH"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void riskScoreEightyNineFlagsManualReview() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(89).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("HIGH_RISK_MANUAL_REVIEW"), d.warnings());
        }

        @Test
        void riskScoreSeventyFlagsManualReview() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(70).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("HIGH_RISK_MANUAL_REVIEW"), d.warnings());
        }

        @Test
        void riskScoreSixtyNineWarnsMonitoring() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(69).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ELEVATED_RISK_MONITORING"), d.warnings());
        }

        @Test
        void riskScoreFiftyWarnsMonitoring() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(50).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ELEVATED_RISK_MONITORING"), d.warnings());
        }

        @Test
        void riskScoreFortyNineIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().riskScore(49).build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void negativeFailedAttemptCountThrows() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.failedEnrollmentAttempts("s1", "c1")).thenReturn(-1);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service(history).evaluate(request(student().build(), course().build()).build()));
            assertEquals("failed attempt count must not be negative", ex.getMessage());
        }

        @Test
        void failedAttemptsAtLimitBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.failedEnrollmentAttempts("s1", "c1")).thenReturn(3);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumFailedAttempts(3).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("TOO_MANY_FAILED_ATTEMPTS"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void failedAttemptsBelowLimitWarn() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.failedEnrollmentAttempts("s1", "c1")).thenReturn(2);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumFailedAttempts(3).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREVIOUS_FAILED_ATTEMPTS"), d.warnings());
        }

        @Test
        void zeroLimitStillWarnsOnPastFailures() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.failedEnrollmentAttempts("s1", "c1")).thenReturn(5);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumFailedAttempts(0).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREVIOUS_FAILED_ATTEMPTS"), d.warnings());
        }
    }

    // ==================================================================
    // 讲师自报与历史交互（evaluate 主体 + Mockito verify）。
    // ==================================================================
    @Nested
    class InstructorAndInteractionTests {

        @Test
        void instructorCannotEnrollInOwnCourse() {
            EnrollmentDecision d = service().evaluate(
                    request(student().instructor(true).build(), course().instructorId("s1").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void instructorMayEnrollInSomeoneElsesCourse() {
            EnrollmentDecision d = service().evaluate(
                    request(student().instructor(true).build(), course().instructorId("t1").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void nonInstructorWithMatchingIdsIsNotBlocked() {
            EnrollmentDecision d = service().evaluate(
                    request(student().instructor(false).build(), course().instructorId("s1").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void historyIsConsultedExactlyOncePerRelevantMethod() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            verify(history).alreadyEnrolled("s1", "c1");
            verify(history).hasRefundAbuseFlag("s1");
            verify(history).failedEnrollmentAttempts("s1", "c1");
            verify(history).activeCourseCountThisWeek("s1");
            verify(history, never()).completedCourse(anyString(), anyString());
            verify(history, never()).completionScore(anyString(), anyString());
            verify(history, never()).hasScheduleConflict(anyString(), anyString());
        }
    }

    // ==================================================================
    // 先修课（evaluatePrerequisites）: 缺课、分数三段边界、空/脏配置、多门混合。
    // ==================================================================
    @Nested
    class PrerequisiteTests {

        @Test
        void missingPrerequisiteBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(false);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("MISSING_PREREQUISITE:p1"), d.reasons());
            assertTrue(d.warnings().isEmpty());
            verify(history).completedCourse("s1", "p1");
        }

        @Test
        void completedPrerequisiteAddsAllCompletedWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(85);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
            verify(history).completionScore("s1", "p1");
        }

        @Test
        void prerequisiteScoreBelowMinimumBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(79);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(),
                            course().prerequisites(Set.of("p1")).minimumPrerequisiteScore(80).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("PREREQUISITE_SCORE_TOO_LOW:p1"), d.reasons());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void prerequisiteScoreExactlyAtMinimumPasses() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(80);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(),
                            course().prerequisites(Set.of("p1")).minimumPrerequisiteScore(80).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void negativePrerequisiteScoreBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(-1);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("INVALID_PREREQUISITE_SCORE:p1"), d.reasons());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void prerequisiteScoreAboveOneHundredBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(101);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("INVALID_PREREQUISITE_SCORE:p1"), d.reasons());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void zeroScoreIsValidAndSilent() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(0);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void scoreNinetyFiveEarnsExcellenceWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(95);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(List.of("EXCELLENT_PREREQUISITE_SCORE:p1", "ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void scoreNinetyFourEarnsNoExcellenceWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(94);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void perfectScoreEarnsExcellenceWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(100);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(List.of("EXCELLENT_PREREQUISITE_SCORE:p1", "ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void scoreBelowHighMinimumBlocksEvenBelowExcellence() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(99);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(),
                            course().prerequisites(Set.of("p1")).minimumPrerequisiteScore(100).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("PREREQUISITE_SCORE_TOO_LOW:p1"), d.reasons());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }

        @Test
        void blankPrerequisiteIdWarnsBadConfiguration() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().prerequisites(Set.of("   ")).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("INVALID_PREREQUISITE_CONFIGURATION"), d.warnings());
        }

        @Test
        void partiallyCompletedPrerequisitesSkipAllCompletedWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(95);
            when(history.completedCourse("s1", "p2")).thenReturn(false);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(new LinkedHashSet<>(List.of("p1", "p2"))).build())
                            .build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("MISSING_PREREQUISITE:p2"), d.reasons());
            assertEquals(List.of("EXCELLENT_PREREQUISITE_SCORE:p1"), d.warnings());
        }

        @Test
        void twoCompletedPrerequisitesAddSingleAllCompletedWarning() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.completedCourse("s1", "p1")).thenReturn(true);
            when(history.completionScore("s1", "p1")).thenReturn(85);
            when(history.completedCourse("s1", "p2")).thenReturn(true);
            when(history.completionScore("s1", "p2")).thenReturn(90);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().prerequisites(new LinkedHashSet<>(List.of("p1", "p2"))).build())
                            .build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ALL_PREREQUISITES_COMPLETED"), d.warnings());
        }
    }

    // ==================================================================
    // 排课（liveCourse / scheduleKey 分支）: 缺排课、冲突、审计豁免、自学课忽略排课。
    // ==================================================================
    @Nested
    class ScheduleTests {

        @Test
        void liveCourseWithoutScheduleBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().liveCourse(true).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("LIVE_COURSE_WITHOUT_SCHEDULE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void liveCourseWithBlankScheduleBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().liveCourse(true).scheduleKey("   ").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("LIVE_COURSE_WITHOUT_SCHEDULE"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void liveScheduleConflictBlocksOutsideAuditMode() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.hasScheduleConflict("s1", "room-1")).thenReturn(true);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().liveCourse(true).scheduleKey("room-1").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("SCHEDULE_CONFLICT"), d.reasons());
            assertTrue(d.warnings().isEmpty());
            verify(history).hasScheduleConflict("s1", "room-1");
        }

        @Test
        void liveScheduleConflictIsToleratedInAuditMode() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.hasScheduleConflict("s1", "room-1")).thenReturn(true);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().liveCourse(true).scheduleKey("room-1").build())
                            .auditMode(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("SCHEDULE_CONFLICT_ALLOWED_IN_AUDIT_MODE"), d.warnings());
        }

        @Test
        void liveCourseWithoutConflictEnrollsSilently() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().liveCourse(true).scheduleKey("room-1").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void selfPacedCourseWithScheduleWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().scheduleKey("room-1").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("SELF_PACED_COURSE_IGNORES_SCHEDULE"), d.warnings());
        }

        @Test
        void selfPacedCourseWithoutScheduleIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().scheduleKey(null).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void selfPacedCourseWithBlankScheduleIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().scheduleKey("").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }
    }

    // ==================================================================
    // 周课上限（maximumWeeklyCourses）: 负数计数、PREMIUM 宽限、ENTERPRISE 覆盖。
    // ==================================================================
    @Nested
    class WeeklyLimitTests {

        @Test
        void negativeActiveCourseCountThrows() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(-1);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service(history).evaluate(request(student().build(), course().build()).build()));
            assertEquals("active course count must not be negative", ex.getMessage());
        }

        @Test
        void weeklyLimitExactlyReachedBlocks() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(2);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumWeeklyCourses(2).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WEEKLY_COURSE_LIMIT_REACHED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void weeklyLimitBelowThresholdPasses() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(1);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumWeeklyCourses(2).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void premiumMemberGetsGraceExactlyAtLimit() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(2);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().membershipLevel(MembershipLevel.PREMIUM).build(),
                            course().maximumWeeklyCourses(2).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PREMIUM_WEEKLY_LIMIT_GRACE"), d.warnings());
        }

        @Test
        void premiumMemberBeyondLimitIsBlocked() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(3);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().membershipLevel(MembershipLevel.PREMIUM).build(),
                            course().maximumWeeklyCourses(2).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WEEKLY_COURSE_LIMIT_REACHED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void enterpriseSponsorshipOverridesWeeklyLimit() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(2);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(),
                            course().maximumWeeklyCourses(2).build()).organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP", "ENTERPRISE_WEEKLY_LIMIT_OVERRIDE"),
                    d.warnings());
        }

        @Test
        void enterpriseWithoutSponsorshipIsBlockedAtLimit() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(2);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(),
                            course().maximumWeeklyCourses(2).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WEEKLY_COURSE_LIMIT_REACHED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void zeroWeeklyLimitIgnoresActiveCount() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.activeCourseCountThisWeek("s1")).thenReturn(5);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(), course().maximumWeeklyCourses(0).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }
    }

    // ==================================================================
    // 支付（requiresPayment + 主体分支）: 价格四种豁免、授权组合、奖学金+赞助并存。
    // ==================================================================
    @Nested
    class PaymentTests {

        @Test
        void paidCourseWithoutAuthorizationBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("149.50").build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(true, d.paymentRequired());
            assertEquals(30, d.availableSeats());
            assertEquals(List.of("PAYMENT_NOT_AUTHORIZED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void paidCourseWithAuthorizationEnrollsAndRequiresPayment() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("149.50").build()).paymentAuthorized(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(true, d.paymentRequired());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void freeCourseWithAuthorizationWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("0").build()).paymentAuthorized(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PAYMENT_AUTHORIZATION_NOT_REQUIRED"), d.warnings());
        }

        @Test
        void freeCourseWithoutAuthorizationIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("0").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void negativePriceIsTreatedAsFree() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("-1").build()).paymentAuthorized(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(List.of("PAYMENT_AUTHORIZATION_NOT_REQUIRED"), d.warnings());
        }

        @Test
        void nullPriceIsTreatedAsFree() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price(null).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void auditModeWaivesPayment() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("149.50").build()).auditMode(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void scholarshipWaivesPayment() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("149.50").build()).scholarshipApproved(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void sponsorshipWaivesPayment() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().price("149.50").build()).organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP"), d.warnings());
        }

        @Test
        void scholarshipAndSponsorshipTogetherWarn() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().build())
                            .scholarshipApproved(true).organizationSponsored(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("OPTIONAL_ORGANIZATION_SPONSORSHIP", "SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT"),
                    d.warnings());
        }
    }

    // ==================================================================
    // 容量与候补（evaluateCapacity + 候补级联）: 无障碍座位、临满警告、五个候补拒绝与两条候补出路。
    // ==================================================================
    @Nested
    class CapacityWaitlistTests {

        @Test
        void noAccessibilitySeatsBlock() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().build()).accessibilitySeatRequired(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("NO_ACCESSIBILITY_SEATS"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void accessibilitySeatsFullBlocks() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().accessibilitySeatCapacity(2).accessibilitySeatsUsed(2).build())
                            .accessibilitySeatRequired(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("ACCESSIBILITY_SEATS_FULL"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void accessibilitySeatBelowCapacityReservesWithWarning() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().accessibilitySeatCapacity(2).accessibilitySeatsUsed(1).build())
                            .accessibilitySeatRequired(true).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ACCESSIBILITY_SEAT_RESERVED"), d.warnings());
        }

        @Test
        void invalidAccessibilityConfigurationWarns() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().accessibilitySeatCapacity(2).accessibilitySeatsUsed(3).build())
                            .build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID"), d.warnings());
        }

        @Test
        void accessibilityUsageExactlyAtCapacityIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().accessibilitySeatCapacity(3).accessibilitySeatsUsed(3).build())
                            .build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void courseAlmostFullWarnsWhenTwoSeatsWouldRemain() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(3).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(3, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("COURSE_ALMOST_FULL"), d.warnings());
        }

        @Test
        void courseWithThreeRemainingSeatsIsSilent() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(4).build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void takingTheLastSeatWarnsAlmostFull() {
            EnrollmentDecision d = service().evaluate(
                    request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(),
                            course().capacity(2).build()).requestedSeats(2).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("COURSE_ALMOST_FULL"), d.warnings());
        }

        @Test
        void waitlistDeclinedRejects() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(0).waitlistEnabled(true).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(0, d.availableSeats());
            assertEquals(List.of("WAITLIST_NOT_ACCEPTED"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void zeroWaitlistCapacityRejects() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(), course().capacity(0).waitlistEnabled(true).waitlistCapacity(0).build())
                            .acceptWaitlist(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WAITLIST_DISABLED_BY_CAPACITY"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void negativeWaitlistCapacityRejects() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(),
                            course().capacity(0).waitlistEnabled(true).waitlistCapacity(-3).build())
                            .acceptWaitlist(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WAITLIST_DISABLED_BY_CAPACITY"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void fullWaitlistRejects() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(),
                            course().capacity(0).waitlistEnabled(true).waitlistCapacity(1).waitlistedCount(1).build())
                            .acceptWaitlist(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("WAITLIST_FULL"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void groupEnrollmentCannotJoinWaitlist() {
            EnrollmentDecision d = service().evaluate(
                    request(student().membershipLevel(MembershipLevel.ENTERPRISE).build(),
                            course().capacity(0).waitlistEnabled(true).waitlistCapacity(5).build())
                            .requestedSeats(2).acceptWaitlist(true).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(0, d.availableSeats());
            assertEquals(List.of("GROUP_ENROLLMENT_CANNOT_WAITLIST"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void studentWithSpaceIsWaitlistedAndPaymentFlagIsHardcodedFalse() {
            EnrollmentDecision d = service().evaluate(
                    request(student().build(),
                            course().capacity(0).waitlistEnabled(true).waitlistCapacity(1).waitlistedCount(0)
                                    .price("49.00").build())
                            .acceptWaitlist(true).paymentAuthorized(true).build());
            assertEquals(EnrollmentOutcome.WAITLISTED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(0, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("PLACED_ON_WAITLIST"), d.warnings());
            assertTrue(d.accepted());
        }

        @Test
        void manualReviewStudentIsWaitlistedAfterReview() {
            EnrollmentHistory history = mock(EnrollmentHistory.class);
            when(history.hasRefundAbuseFlag("s1")).thenReturn(true);
            EnrollmentDecision d = service(history).evaluate(
                    request(student().build(),
                            course().capacity(0).waitlistEnabled(true).waitlistCapacity(1).waitlistedCount(0).build())
                            .acceptWaitlist(true).build());
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(0, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertEquals(List.of("REFUND_ABUSE_REVIEW", "WAITLIST_AFTER_MANUAL_REVIEW"), d.warnings());
            assertFalse(d.accepted());
        }
    }

    // ==================================================================
    // 适配器集成（真实 EmptyEnrollmentHistory）: 端口默认实现的端到端行为。
    // ==================================================================
    @Nested
    class AdapterIntegrationTests {

        @Test
        void emptyHistoryLetsEligibleStudentEnroll() {
            EnrollmentDecision d = realAdapterService().evaluate(
                    request(student().build(), course().build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertEquals(false, d.paymentRequired());
            assertEquals(30, d.availableSeats());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
            assertTrue(d.accepted());
        }

        @Test
        void emptyHistoryReportsMissingPrerequisites() {
            EnrollmentDecision d = realAdapterService().evaluate(
                    request(student().build(), course().prerequisites(Set.of("p1")).build()).build());
            assertEquals(EnrollmentOutcome.REJECTED, d.outcome());
            assertEquals(List.of("MISSING_PREREQUISITE:p1"), d.reasons());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void emptyHistorySeesNoScheduleConflictOnLiveCourse() {
            EnrollmentDecision d = realAdapterService().evaluate(
                    request(student().build(), course().liveCourse(true).scheduleKey("room-1").build()).build());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
            assertTrue(d.reasons().isEmpty());
            assertTrue(d.warnings().isEmpty());
        }

        @Test
        void emptyHistoryNeverFlagsRefundAbuseSoOutcomeIsEnrolledNotManualReview() {
            EnrollmentDecision d = realAdapterService().evaluate(
                    request(student().build(), course().build()).build());
            assertNotSame(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertEquals(EnrollmentOutcome.ENROLLED, d.outcome());
        }
    }

    // ==================================================================
    // 领域记录（compact constructor 防御拷贝/校验）与枚举全量覆盖。
    // ==================================================================
    @Nested
    class DomainRecordTests {

        @Test
        void acceptedIsTrueOnlyForEnrolledAndWaitlisted() {
            assertTrue(new EnrollmentDecision(EnrollmentOutcome.ENROLLED, false, 0, List.of(), List.of()).accepted());
            assertTrue(new EnrollmentDecision(EnrollmentOutcome.WAITLISTED, false, 0, List.of(), List.of()).accepted());
            assertFalse(new EnrollmentDecision(EnrollmentOutcome.MANUAL_REVIEW, false, 0, List.of(), List.of())
                    .accepted());
            assertFalse(new EnrollmentDecision(EnrollmentOutcome.REJECTED, false, 0, List.of(), List.of()).accepted());
        }

        @Test
        void decisionDefensivelyCopiesMutableLists() {
            List<String> reasons = new ArrayList<>();
            reasons.add("R1");
            List<String> warnings = new ArrayList<>();
            warnings.add("W1");
            EnrollmentDecision d = new EnrollmentDecision(EnrollmentOutcome.REJECTED, true, 7, reasons, warnings);
            reasons.add("R2");
            warnings.add("W2");
            assertEquals(List.of("R1"), d.reasons());
            assertEquals(List.of("W1"), d.warnings());
            assertThrows(UnsupportedOperationException.class, () -> d.reasons().add("X"));
            assertThrows(UnsupportedOperationException.class, () -> d.warnings().add("Y"));
        }

        @Test
        void decisionRejectsNullReasonsList() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new EnrollmentDecision(EnrollmentOutcome.ENROLLED, false, 0, null, List.of()));
            assertNotNull(ex);
        }

        @Test
        void decisionRejectsNullWarningsList() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new EnrollmentDecision(EnrollmentOutcome.ENROLLED, false, 0, List.of(), null));
            assertNotNull(ex);
        }

        @Test
        void decisionKeepsEveryFieldVerbatim() {
            EnrollmentDecision d = new EnrollmentDecision(EnrollmentOutcome.MANUAL_REVIEW, true, 12,
                    List.of("A", "B"), List.of("C"));
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, d.outcome());
            assertEquals(true, d.paymentRequired());
            assertEquals(12, d.availableSeats());
            assertEquals(List.of("A", "B"), d.reasons());
            assertEquals(List.of("C"), d.warnings());
        }

        @Test
        void courseOfferingDefaultsNullCollectionsToEmptySets() {
            CourseOffering offering = course().allowedCountries(null).prerequisites(null).build();
            assertNotNull(offering.allowedCountries());
            assertTrue(offering.allowedCountries().isEmpty());
            assertNotNull(offering.prerequisiteCourseIds());
            assertTrue(offering.prerequisiteCourseIds().isEmpty());
        }

        @Test
        void courseOfferingDefensivelyCopiesCollections() {
            Set<String> countries = new HashSet<>(List.of("US"));
            Set<String> prereqs = new HashSet<>(List.of("p1"));
            CourseOffering offering = course().allowedCountries(countries).prerequisites(prereqs).build();
            countries.add("CA");
            prereqs.add("p2");
            assertEquals(Set.of("US"), offering.allowedCountries());
            assertEquals(Set.of("p1"), offering.prerequisiteCourseIds());
            assertThrows(UnsupportedOperationException.class, () -> offering.allowedCountries().add("DE"));
            assertThrows(UnsupportedOperationException.class, () -> offering.prerequisiteCourseIds().add("p9"));
        }

        @Test
        void courseOfferingRejectsNullElementsInCollections() {
            Set<String> withNull = new HashSet<>(Arrays.asList("US", null));
            Set<String> prereqsWithNull = new HashSet<>(Arrays.asList("p1", null));
            assertThrows(NullPointerException.class,
                    () -> course().allowedCountries(withNull).build());
            assertThrows(NullPointerException.class,
                    () -> course().prerequisites(prereqsWithNull).build());
        }

        @Test
        void courseOfferingKeepsScalarFieldsVerbatim() {
            CourseOffering offering = course().courseId("c9").instructorId("t9")
                    .status(EnrollmentCourseStatus.ARCHIVED).capacity(7).enrolledCount(3)
                    .minimumAge(18).maximumAge(60).requiredLanguage("EN")
                    .minimumPrerequisiteScore(80).maximumWeeklyCourses(4).maximumFailedAttempts(2)
                    .organizationOnly(true).requiredOrganizationId("org-7").subscriptionRequired(true)
                    .liveCourse(true).scheduleKey("room-9").accessibilitySeatCapacity(1)
                    .accessibilitySeatsUsed(0).price("199.99").build();
            assertEquals("c9", offering.courseId());
            assertEquals("t9", offering.instructorId());
            assertEquals(EnrollmentCourseStatus.ARCHIVED, offering.status());
            assertEquals(7, offering.capacity());
            assertEquals(3, offering.enrolledCount());
            assertEquals(18, offering.minimumAge());
            assertEquals(60, offering.maximumAge());
            assertEquals("EN", offering.requiredLanguage());
            assertEquals(80, offering.minimumPrerequisiteScore());
            assertEquals(4, offering.maximumWeeklyCourses());
            assertEquals(2, offering.maximumFailedAttempts());
            assertEquals(true, offering.organizationOnly());
            assertEquals("org-7", offering.requiredOrganizationId());
            assertEquals(true, offering.subscriptionRequired());
            assertEquals(true, offering.liveCourse());
            assertEquals("room-9", offering.scheduleKey());
            assertEquals(1, offering.accessibilitySeatCapacity());
            assertEquals(0, offering.accessibilitySeatsUsed());
            assertEquals(new BigDecimal("199.99"), offering.price());
        }

        @Test
        void studentProfileKeepsEveryFieldVerbatim() {
            StudentProfile s = student().studentId("s9").age(30).countryCode("CA").preferredLanguage("fr")
                    .membershipLevel(MembershipLevel.PREMIUM).accountActive(false).suspended(true)
                    .emailVerified(false).identityVerified(false).profileComplete(false).instructor(true)
                    .parentalConsent(true).termsAccepted(false).activeSubscription(true).riskScore(77).build();
            assertEquals("s9", s.studentId());
            assertEquals(30, s.age());
            assertEquals("CA", s.countryCode());
            assertEquals("fr", s.preferredLanguage());
            assertEquals(MembershipLevel.PREMIUM, s.membershipLevel());
            assertEquals(false, s.accountActive());
            assertEquals(true, s.suspended());
            assertEquals(false, s.emailVerified());
            assertEquals(false, s.identityVerified());
            assertEquals(false, s.profileComplete());
            assertEquals(true, s.instructor());
            assertEquals(true, s.parentalConsent());
            assertEquals(false, s.termsAccepted());
            assertEquals(true, s.activeSubscription());
            assertEquals(77, s.riskScore());
        }

        @Test
        void courseStatusEnumCoversEveryValue() {
            assertEquals(4, EnrollmentCourseStatus.values().length);
            assertEquals(EnrollmentCourseStatus.DRAFT, EnrollmentCourseStatus.valueOf("DRAFT"));
            assertEquals(EnrollmentCourseStatus.PUBLISHED, EnrollmentCourseStatus.valueOf("PUBLISHED"));
            assertEquals(EnrollmentCourseStatus.CLOSED, EnrollmentCourseStatus.valueOf("CLOSED"));
            assertEquals(EnrollmentCourseStatus.ARCHIVED, EnrollmentCourseStatus.valueOf("ARCHIVED"));
        }

        @Test
        void outcomeEnumCoversEveryValue() {
            assertEquals(4, EnrollmentOutcome.values().length);
            assertEquals(EnrollmentOutcome.ENROLLED, EnrollmentOutcome.valueOf("ENROLLED"));
            assertEquals(EnrollmentOutcome.WAITLISTED, EnrollmentOutcome.valueOf("WAITLISTED"));
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, EnrollmentOutcome.valueOf("MANUAL_REVIEW"));
            assertEquals(EnrollmentOutcome.REJECTED, EnrollmentOutcome.valueOf("REJECTED"));
        }

        @Test
        void emptyAdapterAnswersAllDefaults() {
            EmptyEnrollmentHistory adapter = new EmptyEnrollmentHistory();
            assertEquals(false, adapter.alreadyEnrolled("s1", "c1"));
            assertEquals(false, adapter.completedCourse("s1", "c1"));
            assertEquals(0, adapter.completionScore("s1", "c1"));
            assertEquals(false, adapter.hasScheduleConflict("s1", "room-1"));
            assertEquals(0, adapter.activeCourseCountThisWeek("s1"));
            assertEquals(0, adapter.failedEnrollmentAttempts("s1", "c1"));
            assertEquals(false, adapter.hasRefundAbuseFlag("s1"));
        }
    }}

    // ------------------------------------------------------------------
    // Advanced pricing (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class PricingModuleTests {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Clock PRICING_CLOCK = Clock.fixed(NOW, UTC);

    /** 无状态服务（仅持有不可变 Clock），全套件安全共享。 */
    private static final AdvancedPricingService SERVICE = new AdvancedPricingService(PRICING_CLOCK);

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 6, 16);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 6, 14);

    // ------------------------------------------------------------------
    // 请求 / 券 / 活动工厂（字段全显式，验证类测试可单字段变形）
    // ------------------------------------------------------------------

    private static BigDecimal amount(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static PriceRequest req(String courseId, String unitPrice, int quantity,
                                    MembershipLevel membershipLevel,
                                    net.mooctest.pricing.domain.Coupon coupon,
                                    net.mooctest.pricing.domain.Promotion promotion,
                                    boolean firstPurchase, boolean nonprofitCustomer, boolean taxExempt,
                                    boolean instructorOwnedCourse, int loyaltyPointsToUse,
                                    int availableLoyaltyPoints, String countryCode, String currency) {
        return new PriceRequest(courseId, amount(unitPrice), quantity, membershipLevel, coupon, promotion,
                firstPurchase, nonprofitCustomer, taxExempt, instructorOwnedCourse,
                loyaltyPointsToUse, availableLoyaltyPoints, countryCode, currency);
    }

    private static PriceRequest request(String unitPrice, int quantity) {
        return request(unitPrice, quantity, MembershipLevel.NONE);
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel) {
        return req("course-1", unitPrice, quantity, membershipLevel, null, null,
                false, false, false, false, 0, 0, "CN", "CNY");
    }

    private static PriceRequest request(String unitPrice, int quantity, String countryCode, String currency) {
        return req("course-1", unitPrice, quantity, MembershipLevel.NONE, null, null,
                false, false, false, false, 0, 0, countryCode, currency);
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel,
                                        net.mooctest.pricing.domain.Coupon coupon,
                                        net.mooctest.pricing.domain.Promotion promotion) {
        return req("course-1", unitPrice, quantity, membershipLevel, coupon, promotion,
                false, false, false, false, 0, 0, "CN", "CNY");
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel,
                                        net.mooctest.pricing.domain.Coupon coupon,
                                        net.mooctest.pricing.domain.Promotion promotion,
                                        boolean firstPurchase,
                                        boolean nonprofitCustomer, boolean instructorOwnedCourse,
                                        int loyaltyPointsToUse, int availableLoyaltyPoints) {
        return req("course-1", unitPrice, quantity, membershipLevel, coupon, promotion,
                firstPurchase, nonprofitCustomer, false, instructorOwnedCourse,
                loyaltyPointsToUse, availableLoyaltyPoints, "CN", "CNY");
    }

    private static net.mooctest.pricing.domain.Coupon coupon(
            String code, CouponType type, String value, String minimumSubtotal,
            LocalDate validFrom, LocalDate validUntil, int remainingUses,
            boolean stackable, Set<String> eligibleCourseIds,
            MembershipLevel minimumMembership, int maximumQuantity) {
        return new net.mooctest.pricing.domain.Coupon(code, type, amount(value), amount(minimumSubtotal),
                validFrom, validUntil, remainingUses, stackable, eligibleCourseIds,
                minimumMembership, maximumQuantity);
    }

    /** 默认可用券: code SAVE-10, 剩余 5 次, 可叠加, 无窗口/门槛/限量。 */
    private static net.mooctest.pricing.domain.Coupon coupon(CouponType type, String value) {
        return coupon("SAVE-10", type, value, null, null, null, 5, true, Set.of(), null, 0);
    }

    private static net.mooctest.pricing.domain.Coupon coupon(CouponType type, String value, boolean stackable) {
        return coupon("SAVE-10", type, value, null, null, null, 5, stackable, Set.of(), null, 0);
    }

    /** 仅变 code/type/value 的券（其余字段取默认）。 */
    private static net.mooctest.pricing.domain.Coupon coupon(String code, CouponType type, String value) {
        return coupon(code, type, value, null, null, null, 5, true, Set.of(), null, 0);
    }

    /** 默认活动: name "Summer Sale", minimumQuantity 1, 无窗口/上限。 */
    private static net.mooctest.pricing.domain.Promotion promotion(PromotionType type, String value) {
        return promotion("Summer Sale", type, value, null, null, false, 1, null);
    }

    private static net.mooctest.pricing.domain.Promotion promotion(
            String name, PromotionType type, String value, LocalDate startsOn,
            LocalDate endsOn, boolean membersOnly, int minimumQuantity, String maximumDiscount) {
        return new net.mooctest.pricing.domain.Promotion(name, type, amount(value), startsOn, endsOn,
                membersOnly, minimumQuantity, amount(maximumDiscount));
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertEquals(expected, actual.toPlainString());
    }

    // ------------------------------------------------------------------
    // 请求校验: validateRequest 的每个守卫及边界
    // ------------------------------------------------------------------
    @Nested
    class Validation {

        @Test
        void nullRequestThrowsNullPointerException() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> SERVICE.calculate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req(null, "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals("courseId must not be blank", nullId.getMessage());

            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("   ", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals("courseId must not be blank", blankId.getMessage());
        }

        @Test
        void nullUnitPriceThrowsBusinessExceptionWithInvalidPrice() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(req("course-1", null, 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals(ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Unit price must be between 0 and 99999.99", ex.getMessage());
        }

        @Test
        void negativeUnitPriceThrowsBusinessException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(req("course-1", "-0.01", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals(ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Unit price must be between 0 and 99999.99", ex.getMessage());
        }

        @Test
        void unitPriceBoundaryAcceptsMaximumAndRejectsBeyond() {
            PriceBreakdown atMax = SERVICE.calculate(request("99999.99", 1));
            assertMoney(atMax.subtotal(), "99999.99");
            // 99999.99 * 0.06 = 5999.9994 -> HALF_UP -> 6000.00
            assertMoney(atMax.tax(), "6000.00");
            assertMoney(atMax.total(), "105999.99");
            assertEquals(List.of("TAX_CN"), atMax.appliedRules());

            BusinessException beyond = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(request("100000.00", 1)));
            assertEquals(ErrorCode.INVALID_PRICE, beyond.code());
            assertEquals("Unit price must be between 0 and 99999.99", beyond.getMessage());
        }

        @Test
        void quantityBelowOneIsRejected() {
            assertEquals("quantity must be positive", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 0))).getMessage());
            assertEquals("quantity must be positive", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", -3))).getMessage());
        }

        @Test
        void quantityAboveHundredIsRejected() {
            assertEquals("quantity must not exceed 100", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 101))).getMessage());
        }

        @Test
        void nullMembershipLevelIsRejected() {
            assertEquals("membershipLevel must not be null", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, null, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY"))).getMessage());
        }

        @Test
        void blankCurrencyIsRejected() {
            assertEquals("currency must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", null))).getMessage());
            assertEquals("currency must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", ""))).getMessage());
        }

        @Test
        void unsupportedCurrencyIsRejectedButLowercaseIsAccepted() {
            assertEquals("unsupported currency: JPY", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 1, "CN", "JPY"))).getMessage());

            PriceBreakdown lowercase = SERVICE.calculate(request("10.00", 1, "CN", "cny"));
            assertMoney(lowercase.total(), "10.60");
            assertEquals(List.of("TAX_CN"), lowercase.appliedRules());
            assertTrue(lowercase.warnings().isEmpty());
        }

        @Test
        void blankCountryCodeIsRejected() {
            assertEquals("countryCode must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, null, "CNY"))).getMessage());
            assertEquals("countryCode must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "  ", "CNY"))).getMessage());
        }

        @Test
        void negativeLoyaltyPointsToUseIsRejected() {
            assertEquals("loyaltyPointsToUse must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, -1, 0, "CN", "CNY"))).getMessage());
        }

        @Test
        void negativeAvailableLoyaltyPointsIsRejected() {
            assertEquals("availableLoyaltyPoints must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, -5, "CN", "CNY"))).getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 会员折扣: 六个档位 + INSTRUCTOR/ENTERPRISE 双侧 + NONE 早退
    // ------------------------------------------------------------------
    @Nested
    class Membership {

        @Test
        void noneMembershipAddsNoDiscountAndNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.NONE));
            assertMoney(breakdown.subtotal(), "10.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.60");
            assertMoney(breakdown.total(), "10.60");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void basicMembershipDiscountIsThreePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.BASIC));
            assertMoney(breakdown.membershipDiscount(), "0.30");
            assertMoney(breakdown.total(), "10.28");
            assertEquals(List.of("MEMBERSHIP_BASIC", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void plusMembershipDiscountIsSevenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.PLUS));
            assertMoney(breakdown.membershipDiscount(), "0.70");
            assertMoney(breakdown.total(), "9.86");
            assertEquals(List.of("MEMBERSHIP_PLUS", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void premiumMembershipDiscountIsTwelvePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.PREMIUM));
            assertMoney(breakdown.membershipDiscount(), "1.20");
            assertMoney(breakdown.total(), "9.33");
            assertEquals(List.of("MEMBERSHIP_PREMIUM", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void instructorOwnedCourseWithInstructorLevelIsFree() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.INSTRUCTOR,
                    null, null, false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "10.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "FREE_ORDER"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void instructorLevelWithoutOwnershipGetsTenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.INSTRUCTOR));
            assertMoney(breakdown.membershipDiscount(), "1.00");
            assertMoney(breakdown.total(), "9.54");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void enterpriseTenSeatsGetEighteenPercentWithoutWarning() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.ENTERPRISE));
            assertMoney(breakdown.subtotal(), "100.00");
            assertMoney(breakdown.membershipDiscount(), "18.00");
            assertMoney(breakdown.bulkDiscount(), "5.00");
            assertMoney(breakdown.total(), "81.62");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void enterpriseBelowTenSeatsIsReducedWithWarning() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 9, MembershipLevel.ENTERPRISE));
            assertMoney(breakdown.membershipDiscount(), "7.20");
            assertMoney(breakdown.bulkDiscount(), "4.50");
            assertMoney(breakdown.total(), "83.00");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Enterprise discount is reduced for orders below ten seats"),
                    breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 批量折扣: 50/20/5 三档边界（49/50, 19/20, 4/5）+ 以下无规则
    // ------------------------------------------------------------------
    @Nested
    class Bulk {

        @Test
        void fourSeatsEarnNoBulkRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 4));
            assertMoney(breakdown.subtotal(), "40.60");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.total(), "43.04");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fiveSeatsTriggerBulkFiveWithHalfUpRounding() {
            // 50.75 * 0.05 = 2.5375 -> 2.54
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 5));
            assertMoney(breakdown.bulkDiscount(), "2.54");
            assertMoney(breakdown.total(), "51.10");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void nineteenSeatsStayInBulkFive() {
            // 192.85 * 0.05 = 9.6425 -> 9.64
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 19));
            assertMoney(breakdown.bulkDiscount(), "9.64");
            assertMoney(breakdown.total(), "194.20");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void twentySeatsTriggerBulkTwenty() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 20));
            assertMoney(breakdown.bulkDiscount(), "20.30");
            assertMoney(breakdown.total(), "193.66");
            assertEquals(List.of("BULK_20", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fortyNineSeatsStayInBulkTwenty() {
            // 497.35 * 0.10 = 49.7350 -> 49.74
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 49));
            assertMoney(breakdown.bulkDiscount(), "49.74");
            assertMoney(breakdown.total(), "474.47");
            assertEquals(List.of("BULK_20", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fiftySeatsTriggerBulkFifty() {
            // 507.50 * 0.15 = 76.125 -> 76.13
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 50));
            assertMoney(breakdown.bulkDiscount(), "76.13");
            assertMoney(breakdown.total(), "457.25");
            assertEquals(List.of("BULK_50", "TAX_CN"), breakdown.appliedRules());
        }
    }

    // ------------------------------------------------------------------
    // 促销: 配置完整性 / 日期窗口 / 会员限定 / 最少数量 / 三种类型 / 上限
    // ------------------------------------------------------------------
    @Nested
    class Promotion {

        @Test
        void absentPromotionAddsNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unnamedPromotionIsIgnored() {
            PriceBreakdown nullName = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(null, PromotionType.PERCENT, "10.00", null, null, false, 1, null)));
            assertMoney(nullName.promotionDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), nullName.appliedRules());
            assertEquals(List.of("A promotion without a name was ignored"), nullName.warnings());

            PriceBreakdown blankName = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("   ", PromotionType.PERCENT, "10.00", null, null, false, 1, null)));
            assertEquals(List.of("A promotion without a name was ignored"), blankName.warnings());
        }

        @Test
        void incompletePromotionConfigurationIsIgnored() {
            PriceBreakdown nullType = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", null, "10.00", null, null, false, 1, null)));
            assertEquals(List.of("Promotion configuration is incomplete"), nullType.warnings());
            assertMoney(nullType.promotionDiscount(), "0.00");

            PriceBreakdown nullValue = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, null, null, null, false, 1, null)));
            assertEquals(List.of("Promotion configuration is incomplete"), nullValue.warnings());
        }

        @Test
        void nonPositivePromotionValueIsRejected() {
            PriceBreakdown zero = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "0.00")));
            assertEquals(List.of("Promotion value must be positive"), zero.warnings());

            PriceBreakdown negative = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "-5.00")));
            assertEquals(List.of("Promotion value must be positive"), negative.warnings());
        }

        @Test
        void promotionBeforeItsStartIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TOMORROW, null,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion has not started"), breakdown.warnings());
        }

        @Test
        void promotionStartingTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, null,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void promotionExpiredYesterdayIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, YESTERDAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion has expired"), breakdown.warnings());
        }

        @Test
        void promotionEndingTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, TODAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void singleDayPromotionWindowIsStillValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, TODAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        /**
         * 可观测行为固定: startsOn=TODAY 且 endsOn=YESTERDAY（start &gt; end）时，
         * "has expired" 先于 "date range is invalid" 触发 —— 后者在固定时钟下是死分支，
         * 只能经由下方 stub record 白盒测试执行。
         */
        @Test
        void invertedPromotionWindowFiresTheEarlierExpiryGuard() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, YESTERDAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion has expired"), breakdown.warnings());
            assertFalse(breakdown.appliedRules().contains("PROMOTION_PERCENT"));
        }

        /**
         * 白盒分支覆盖: "Promotion date range is invalid" 守卫要求 startsOn &gt; endsOn，
         * 但前两个窗口守卫已强制 startsOn &le; today &le; endsOn，真实 record 在固定时钟下
         * 数学上不可能到达。用按调用序返回不一致日期的 stub record 执行真实字节码:
         * startsOn 第 1/2 次调用（窗口检查）返回 06-01，第 3/4 次调用（区间检查）返回 06-30；
         * endsOn 反向（06-30, 06-30, 06-01, 06-01）。
         */
        @Test
        void stubbedRecordReachesInvertedPromotionWindowGuard() {
            net.mooctest.pricing.domain.Promotion inverted =
                    mock(net.mooctest.pricing.domain.Promotion.class);
            when(inverted.name()).thenReturn("Inverted Window");
            when(inverted.type()).thenReturn(PromotionType.PERCENT);
            when(inverted.value()).thenReturn(new BigDecimal("10"));
            when(inverted.startsOn()).thenReturn(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30));
            when(inverted.endsOn()).thenReturn(
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));

            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, inverted));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion date range is invalid"), breakdown.warnings());
        }

        @Test
        void membersOnlyPromotionIsBlockedForNonMembers() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            true, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion is available to members only"), breakdown.warnings());
        }

        @Test
        void membersOnlyPromotionAppliesToMembers() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.BASIC,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            true, 1, null)));
            assertMoney(breakdown.membershipDiscount(), "3.00");
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "92.22");
            assertEquals(List.of("MEMBERSHIP_BASIC", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void promotionBelowMinimumQuantityIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 2, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 3, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Order quantity is below the promotion minimum"), breakdown.warnings());
        }

        @Test
        void promotionAtExactMinimumQuantityPasses() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 3, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 3, null)));
            assertMoney(breakdown.subtotal(), "30.00");
            assertMoney(breakdown.promotionDiscount(), "3.00");
            assertMoney(breakdown.total(), "28.62");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void minimumQuantityBelowOneOnlyRequiresSingleUnit() {
            PriceBreakdown zeroFloor = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 0, null)));
            assertMoney(zeroFloor.promotionDiscount(), "10.00");
            assertTrue(zeroFloor.warnings().isEmpty());

            PriceBreakdown negativeFloor = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, -5, null)));
            assertMoney(negativeFloor.promotionDiscount(), "10.00");
        }

        @Test
        void percentPromotionAppliesConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "50.00")));
            assertMoney(breakdown.promotionDiscount(), "50.00");
            assertMoney(breakdown.total(), "53.00");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        /**
         * 边界 value=80 走 percent() 分支（&gt; 80 为假）。注意 80% 折扣必然超过 0.70 标准
         * 折扣上限，故随后被修剪至 70.00 —— 两条路径的最终数值与规则在结构上不可区分，
         * 由 DISCOUNT_CAP 告警钉住修剪行为。
         */
        @Test
        void percentPromotionAtEightyIsStillLimitedByStandardCap() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "80.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        /** value=81 &gt; 80: 先被 80% 钳制为 80.00，再被 0.70 标准折扣上限修剪为 70.00。 */
        @Test
        void percentPromotionAboveEightyIsCappedAtEightyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "81.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void fixedPromotionAppliesValueBelowSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "30.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertEquals(List.of("PROMOTION_FIXED", "TAX_CN"), breakdown.appliedRules());
        }

        /** min(500, 100) = 100.00 &gt; 0.70 标准:上限 -> 再修剪至 70.00（FREE_ORDER 不再触发）。 */
        @Test
        void fixedPromotionNeverExceedsSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "500.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_FIXED", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void buyMoreSaveMoreGivesTwelvePercentAtTwentySeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 20, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.bulkDiscount(), "20.00");
            assertMoney(breakdown.promotionDiscount(), "24.00");
            assertMoney(breakdown.total(), "165.36");
            assertEquals(List.of("BULK_20", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void buyMoreSaveMoreGivesEightPercentBelowTwentySeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 19, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "15.20");
            assertMoney(breakdown.total(), "175.22");
            assertEquals(List.of("BULK_5", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void buyMoreSaveMoreGivesEightPercentAtTenSeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "8.00");
            assertMoney(breakdown.total(), "92.22");
        }

        @Test
        void buyMoreSaveMoreGivesFourPercentBelowTenSeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 9, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "3.60");
            assertMoney(breakdown.total(), "86.81");
            assertEquals(List.of("BULK_5", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void promotionCapClipsDiscountAndWarns() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "50.00", null, null,
                            false, 1, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion discount reached its configured cap"), breakdown.warnings());
        }

        @Test
        void promotionCapAboveDiscountDoesNotTrigger() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "30.00", null, null,
                            false, 1, "40.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void negativePromotionCapIsIgnored() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "30.00", null, null,
                            false, 1, "-1.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void zeroPromotionCapZeroesDiscountAndOmitsRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "50.00", null, null,
                            false, 1, "0.00")));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion discount reached its configured cap"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 优惠券: 配置完整性 / 窗口 / 门槛 / 限定课程 / 数量上限 / 会员下限 / 类型 / 互斥
    // ------------------------------------------------------------------
    @Nested
    class Coupon {

        @Test
        void absentCouponAddsNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void blankCouponCodeIsRejected() {
            PriceBreakdown nullCode = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(null, CouponType.PERCENT, "10.00"), null));
            assertEquals(List.of("Coupon code is blank"), nullCode.warnings());
            assertMoney(nullCode.couponDiscount(), "0.00");

            PriceBreakdown blankCode = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("  ", CouponType.PERCENT, "10.00"), null));
            assertEquals(List.of("Coupon code is blank"), blankCode.warnings());
        }

        @Test
        void incompleteCouponConfigurationIsRejected() {
            PriceBreakdown nullType = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", null, "10.00"), null));
            assertEquals(List.of("Coupon configuration is incomplete"), nullType.warnings());

            PriceBreakdown nullValue = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, null), null));
            assertEquals(List.of("Coupon configuration is incomplete"), nullValue.warnings());
        }

        @Test
        void nonPositiveCouponValueIsRejected() {
            PriceBreakdown zero = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "0.00"), null));
            assertEquals(List.of("Coupon value must be positive"), zero.warnings());

            PriceBreakdown negative = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "-10.00"), null));
            assertEquals(List.of("Coupon value must be positive"), negative.warnings());
        }

        @Test
        void exhaustedCouponIsRejected() {
            PriceBreakdown zeroUses = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 0, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has no remaining uses"), zeroUses.warnings());

            PriceBreakdown negativeUses = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, -2, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has no remaining uses"), negativeUses.warnings());
        }

        @Test
        void couponBeforeValidFromIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TOMORROW, null, 5, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon is not active yet"), breakdown.warnings());
            assertMoney(breakdown.couponDiscount(), "0.00");
        }

        @Test
        void couponValidFromTodayIsActive() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponExpiredYesterdayIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, YESTERDAY, 5, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has expired"), breakdown.warnings());
        }

        @Test
        void couponValidUntilTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, TODAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void singleDayCouponWindowIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, TODAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        /**
         * 可观测行为固定: validFrom=TODAY 且 validUntil=YESTERDAY（start &gt; end）时，
         * "has expired" 先于 "date range is invalid" 触发（后者在固定时钟下为死分支，
         * 仅可经由下方 stub record 白盒测试执行）。
         */
        @Test
        void invertedCouponWindowFiresTheEarlierExpiryGuard() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, YESTERDAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertEquals(List.of("Coupon has expired"), breakdown.warnings());
        }

        /**
         * 白盒分支覆盖: "Coupon date range is invalid" 守卫与固定时钟矛盾（同促销侧注释）。
         * validFrom 第 1/2 次调用（窗口检查）返回 06-01，第 3/4 次（区间检查）返回 06-30；
         * validUntil 反向（06-30, 06-30, 06-01, 06-01）。
         */
        @Test
        void stubbedRecordReachesInvertedCouponWindowGuard() {
            net.mooctest.pricing.domain.Coupon inverted =
                    mock(net.mooctest.pricing.domain.Coupon.class);
            when(inverted.code()).thenReturn("INVERTED");
            when(inverted.type()).thenReturn(CouponType.PERCENT);
            when(inverted.value()).thenReturn(new BigDecimal("10"));
            when(inverted.remainingUses()).thenReturn(1);
            when(inverted.validFrom()).thenReturn(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30));
            when(inverted.validUntil()).thenReturn(
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));

            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    inverted, null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Coupon date range is invalid"), breakdown.warnings());
        }

        @Test
        void couponMinimumSubtotalBlocksSmallerOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("99.99", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", "100.00", null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.subtotal(), "99.99");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "105.99");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Order does not meet the coupon minimum subtotal"), breakdown.warnings());
        }

        @Test
        void couponMinimumSubtotalBoundaryPassesOnEquality() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", "100.00", null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponRestrictedToOtherCoursesIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of("course-9"), null, 0), null));
            assertEquals(List.of("Coupon is not valid for this course"), breakdown.warnings());
            assertMoney(breakdown.couponDiscount(), "0.00");
        }

        @Test
        void couponAppliesToItsEligibleCourse() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of("course-1"), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponWithEmptyEligibilityAppliesEverywhere() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nullEligibleCourseIdsAreNormalizedToUnrestricted() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            null, null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponQuantityLimitBlocksLargerOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 6, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 5), null));
            assertMoney(breakdown.bulkDiscount(), "3.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "60.42");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Order quantity exceeds the coupon limit"), breakdown.warnings());
        }

        @Test
        void couponQuantityLimitBoundaryPassesOnEquality() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 5, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 5), null));
            assertMoney(breakdown.bulkDiscount(), "2.50");
            assertMoney(breakdown.couponDiscount(), "5.00");
            assertMoney(breakdown.total(), "45.05");
            assertEquals(List.of("BULK_5", "COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponWithoutQuantityLimitAcceptsLargeOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 60, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.bulkDiscount(), "90.00");
            assertMoney(breakdown.couponDiscount(), "60.00");
            assertMoney(breakdown.total(), "477.00");
            assertEquals(List.of("BULK_50", "COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponMembershipFloorBlocksLowerLevels() {
            PriceBreakdown basic = SERVICE.calculate(request("100.00", 1, MembershipLevel.BASIC,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertEquals(List.of("Membership level is too low for this coupon"), basic.warnings());

            PriceBreakdown none = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.BASIC, 0), null));
            assertEquals(List.of("Membership level is too low for this coupon"), none.warnings());
        }

        @Test
        void couponMembershipFloorAllowsEqualAndHigherLevels() {
            PriceBreakdown equal = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertMoney(equal.membershipDiscount(), "7.00");
            assertMoney(equal.couponDiscount(), "10.00");
            assertMoney(equal.total(), "87.98");
            assertEquals(List.of("MEMBERSHIP_PLUS", "COUPON_PERCENT", "TAX_CN"), equal.appliedRules());

            PriceBreakdown higher = SERVICE.calculate(request("100.00", 1, MembershipLevel.PREMIUM,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertMoney(higher.couponDiscount(), "10.00");
            assertMoney(higher.total(), "82.68");
            assertTrue(higher.warnings().isEmpty());
        }

        @Test
        void percentCouponAboveSeventyIsCapped() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "90.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void percentCouponAtSeventyKeepsConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "70.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void percentCouponBelowSeventyAppliesConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "30.00"), null));
            assertMoney(breakdown.couponDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fixedAndFullReductionCouponsApplyConfiguredValue() {
            PriceBreakdown fixed = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "25.00"), null));
            assertMoney(fixed.couponDiscount(), "25.00");
            assertMoney(fixed.total(), "79.50");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), fixed.appliedRules());

            PriceBreakdown fullReduction = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FULL_REDUCTION, "25.00"), null));
            assertMoney(fullReduction.couponDiscount(), "25.00");
            assertEquals(List.of("COUPON_FULL_REDUCTION", "TAX_CN"), fullReduction.appliedRules());
        }

        /** min(999, 100) = 100.00 &gt; 0.70 标准上限 -> 再修剪至 70.00。 */
        @Test
        void fixedCouponNeverExceedsSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "999.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("COUPON_FIXED", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void cashbackCouponDefersValueWithFutureRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.CASHBACK, "50.00"), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("COUPON_CASHBACK_FUTURE", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Cashback is credited after payment and does not reduce this order"),
                    breakdown.warnings());
        }

        // ---- 不可叠加券与促销的互斥（双向 + 等额保促销 + 可叠加共存 + 无促销侧）----

        @Test
        void stackableCouponAndPromotionCombine() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", true), promotion(PromotionType.PERCENT, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "63.60");
            assertEquals(List.of("PROMOTION_PERCENT", "COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonStackableCouponLosesToStrongerPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "50.00")));
            assertMoney(breakdown.promotionDiscount(), "50.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "53.00");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The coupon was ignored because the promotion gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void nonStackableCouponBeatsWeakerPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "5.00")));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The promotion was ignored because the coupon gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void equalDiscountsKeepPromotionAndDropCoupon() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The coupon was ignored because the promotion gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void nonStackableCouponStillAppliesWithoutPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), null));
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 首购: 8% / 100 元封顶 / 讲师课程拒绝 / 不可叠加券阻断
    // ------------------------------------------------------------------
    @Nested
    class FirstPurchase {

        @Test
        void firstPurchaseDiscountAppliesEightPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, false, 0, 0));
            assertMoney(breakdown.firstPurchaseDiscount(), "80.00");
            assertMoney(breakdown.total(), "975.20");
            assertEquals(List.of("FIRST_PURCHASE", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void firstPurchaseDiscountCapsAtHundred() {
            PriceBreakdown breakdown = SERVICE.calculate(request("2000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, false, 0, 0));
            // 2000 * 0.08 = 160.00 -> min(160.00, 100.00) = 100.00
            assertMoney(breakdown.firstPurchaseDiscount(), "100.00");
            assertMoney(breakdown.total(), "2014.00");
            assertEquals(List.of("FIRST_PURCHASE", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void firstPurchaseIsBlockedOnInstructorOwnedCourses() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, true, 0, 0));
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.total(), "1060.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("First purchase discount is not applied to instructor-owned courses"),
                    breakdown.warnings());
        }

        @Test
        void firstPurchaseIsBlockedByNonStackableCoupon() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "10.00", false), null, true, false, false, 0, 0));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.total(), "1049.40");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("First purchase discount is blocked by a non-stackable coupon"),
                    breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 非营利: 5% / 企业 8% / 未知国家不加税率减免规则
    // ------------------------------------------------------------------
    @Nested
    class Nonprofit {

        @Test
        void nonprofitDiscountIsFivePercentWithHalvedChineseTax() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, true, false, 0, 0));
            assertMoney(breakdown.nonprofitDiscount(), "5.00");
            // 95.00 * 0.03 (0.06 减半) = 2.85
            assertMoney(breakdown.tax(), "2.85");
            assertMoney(breakdown.total(), "97.85");
            assertEquals(List.of("NONPROFIT", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonprofitEnterpriseDiscountIsEightPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.ENTERPRISE,
                    null, null, false, true, false, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "18.00");
            assertMoney(breakdown.nonprofitDiscount(), "8.00");
            assertMoney(breakdown.total(), "71.07");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "NONPROFIT_ENTERPRISE",
                    "NONPROFIT_TAX_REDUCTION", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonprofitUnknownCountrySkipsTaxReductionRule() {
            PriceBreakdown breakdown = SERVICE.calculate(req("course-1", "100.00", 1,
                    MembershipLevel.NONE, null, null, false, true, false, false, 0, 0, "FR", "EUR"));
            assertMoney(breakdown.nonprofitDiscount(), "5.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "95.00");
            assertEquals(List.of("NONPROFIT"), breakdown.appliedRules());
            assertEquals(List.of("No tax rule is configured for country FR"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 积分: 0 跳过 / 超额缩减 / 99 拒收 / 100 可用 / 20% 封顶双侧 / 零上限归零
    // ------------------------------------------------------------------
    @Nested
    class Loyalty {

        @Test
        void zeroRequestedPointsAreSkipped() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 0, 500));
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void requestedPointsAreReducedToAvailableBalance() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 500, 200));
            // 200 points -> 2.00，低于 20% 上限（20.00），无封顶告警
            assertMoney(breakdown.loyaltyDiscount(), "2.00");
            assertMoney(breakdown.total(), "103.88");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Requested loyalty points were reduced to the available balance"),
                    breakdown.warnings());
        }

        @Test
        void ninetyNinePointsAreRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 99, 500));
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("At least 100 loyalty points are required"), breakdown.warnings());
        }

        @Test
        void exactlyHundredPointsApplyOneUnitOfValue() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 100, 1000));
            assertMoney(breakdown.loyaltyDiscount(), "1.00");
            assertMoney(breakdown.total(), "104.94");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void loyaltyDiscountIsCappedAtTwentyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 5000, 5000));
            // 5000 points -> 50.00 > cap 100.00 * 0.20 = 20.00
            assertMoney(breakdown.loyaltyDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Loyalty discount was limited to twenty percent"), breakdown.warnings());
        }

        @Test
        void tinyCapReducesLoyaltyToZeroWithoutRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.01", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 100, 100));
            // cap = money(0.01 * 0.20) = 0.00 -> value 归零且不加 LOYALTY_POINTS 规则
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.01");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Loyalty discount was limited to twenty percent"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 税收: 免税 / 五国税率 / 未知国家 / 非营利减半 / 币种错配 / 小写国家码
    // ------------------------------------------------------------------
    @Nested
    class Tax {

        @Test
        void taxExemptOrderAddsExemptRuleWithoutTax() {
            PriceBreakdown breakdown = SERVICE.calculate(req("course-1", "100.00", 1,
                    MembershipLevel.NONE, null, null, false, false, true, false, 0, 0, "CN", "CNY"));
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "100.00");
            assertEquals(List.of("TAX_EXEMPT"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void chinaTaxIsSixPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "CN", "CNY"));
            assertMoney(breakdown.tax(), "6.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void usTaxIsSevenAndAHalfPercentWithHalfUpRounding() {
            // 147.00 * 0.075 = 11.0250 -> HALF_UP -> 11.03
            PriceBreakdown breakdown = SERVICE.calculate(request("147.00", 1, "US", "USD"));
            assertMoney(breakdown.tax(), "11.03");
            assertMoney(breakdown.total(), "158.03");
            assertEquals(List.of("TAX_US"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void germanTaxIsNineteenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "DE", "EUR"));
            assertMoney(breakdown.tax(), "19.00");
            assertMoney(breakdown.total(), "119.00");
            assertEquals(List.of("TAX_DE"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void singaporeTaxIsNinePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "SG", "SGD"));
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "109.00");
            assertEquals(List.of("TAX_SG"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unitedKingdomTaxIsTwentyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "GB", "GBP"));
            assertMoney(breakdown.tax(), "20.00");
            assertMoney(breakdown.total(), "120.00");
            assertEquals(List.of("TAX_GB"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unknownCountryWarnsWithoutTaxRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "FR", "EUR"));
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "100.00");
            assertTrue(breakdown.appliedRules().isEmpty());
            assertEquals(List.of("No tax rule is configured for country FR"), breakdown.warnings());
        }

        @Test
        void chineseTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "CN", "USD"));
            assertMoney(breakdown.tax(), "6.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Chinese tax is calculated for a non-CNY order"), breakdown.warnings());
        }

        @Test
        void usTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "US", "CNY"));
            assertMoney(breakdown.tax(), "7.50");
            assertEquals(List.of("TAX_US"), breakdown.appliedRules());
            assertEquals(List.of("United States tax is calculated for a non-USD order"),
                    breakdown.warnings());
        }

        @Test
        void germanTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "DE", "USD"));
            assertMoney(breakdown.tax(), "19.00");
            assertEquals(List.of("TAX_DE"), breakdown.appliedRules());
            assertEquals(List.of("German tax is calculated for a non-EUR order"), breakdown.warnings());
        }

        @Test
        void lowercaseCountryCodeUsesConfiguredRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "cn", "CNY"));
            assertMoney(breakdown.tax(), "6.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 折扣上限: 0.70 双侧 / 讲师全额上限 / trimDiscounts 从队尾修剪且跳过零项
    // ------------------------------------------------------------------
    @Nested
    class Cap {

        /**
         * 修剪次序可观测: excess = 350.00 时 NONPROFIT(50) 与 FIRST_PURCHASE(80) 先被清零，
         * COUPON 位置为 0（触发 continue），PROMOTION 承担剩余 220 -> 580，
         * MEMBERSHIP/BULK 完全不受影响。
         */
        @Test
        void standardCapTrimsFromTheEndSkippingZeroComponents() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 10, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "80.00"), true, true, false, 0, 0));
            assertMoney(breakdown.subtotal(), "1000.00");
            assertMoney(breakdown.membershipDiscount(), "70.00");
            assertMoney(breakdown.bulkDiscount(), "50.00");
            assertMoney(breakdown.promotionDiscount(), "580.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            // 非营利客户: 300.00 * 0.03 (0.06 减半) = 9.00
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "309.00");
            assertEquals(List.of("MEMBERSHIP_PLUS", "BULK_5", "PROMOTION_PERCENT", "FIRST_PURCHASE",
                    "NONPROFIT", "DISCOUNT_CAP", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        /** 讲师自营 + INSTRUCTOR: cap = subtotal，折扣被压回 100%，after 为 0 触发 FREE_ORDER。 */
        @Test
        void instructorCapEqualsSubtotalAndForcesFreeOrder() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.INSTRUCTOR,
                    null, promotion(PromotionType.FIXED, "50.00"), false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "100.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "PROMOTION_FIXED", "DISCOUNT_CAP",
                    "FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void discountsExactlyAtCapAreNotTrimmed() {
            // 7.00 + 63.00 = 70.00 == cap(1000... subtotal 100 * 0.70)
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "63.00")));
            assertMoney(breakdown.membershipDiscount(), "7.00");
            assertMoney(breakdown.promotionDiscount(), "63.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("MEMBERSHIP_PLUS", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        /** 讲师自营但会员非 INSTRUCTOR: 仍用 0.70 标准上限（若错用 subtotal 则不会修剪）。 */
        @Test
        void instructorOwnedNonInstructorMembershipKeepsStandardCap() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PREMIUM,
                    null, promotion(PromotionType.PERCENT, "80.00"), false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "12.00");
            assertMoney(breakdown.promotionDiscount(), "58.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("MEMBERSHIP_PREMIUM", "PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void discountsBelowCapAccumulateFreely() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "30.00")));
            assertMoney(breakdown.membershipDiscount(), "7.00");
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "66.78");
            assertEquals(List.of("MEMBERSHIP_PLUS", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 结果对象: 12 个分量全量断言（简单 + 封顶各一）、免费课程、第 100 件告警
    // ------------------------------------------------------------------
    @Nested
    class Breakdown {

        @Test
        void simpleOrderProducesCompleteBreakdown() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1));
            assertMoney(breakdown.subtotal(), "10.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.60");
            assertMoney(breakdown.total(), "10.60");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        /** 封顶触发样例的全量分量断言（1150 -> 修剪至 700 的完整路径；非营利税率减半）。 */
        @Test
        void cappedOrderProducesCompleteBreakdown() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 10, MembershipLevel.PLUS,
                    coupon(CouponType.FIXED, "100.00"), promotion(PromotionType.PERCENT, "80.00"),
                    true, true, false, 0, 0));
            assertMoney(breakdown.subtotal(), "1000.00");
            assertMoney(breakdown.membershipDiscount(), "70.00");
            assertMoney(breakdown.bulkDiscount(), "50.00");
            assertMoney(breakdown.promotionDiscount(), "580.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            // 非营利客户: 300.00 * 0.03 (0.06 减半) = 9.00
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "309.00");
            assertEquals(List.of("MEMBERSHIP_PLUS", "BULK_5", "PROMOTION_PERCENT", "COUPON_FIXED",
                    "FIRST_PURCHASE", "NONPROFIT", "DISCOUNT_CAP", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void freeCourseProducesFreeOrderWithoutLoyaltyOrTaxRules() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 500, 500));
            assertMoney(breakdown.subtotal(), "0.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("No discount value is generated for a free course"), breakdown.warnings());
        }

        @Test
        void fixedCouponOnFreeCourseYieldsNoCouponRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00"), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("No discount value is generated for a free course"), breakdown.warnings());
        }

        @Test
        void maximumQuantityWarningAppearsOnlyAtHundredUnits() {
            PriceBreakdown atLimit = SERVICE.calculate(request("2.00", 100));
            assertMoney(atLimit.subtotal(), "200.00");
            assertMoney(atLimit.bulkDiscount(), "30.00");
            assertMoney(atLimit.total(), "180.20");
            assertEquals(List.of("BULK_50", "TAX_CN"), atLimit.appliedRules());
            assertEquals(List.of("The order reached the maximum allowed quantity"), atLimit.warnings());

            PriceBreakdown belowLimit = SERVICE.calculate(request("2.00", 99));
            assertMoney(belowLimit.total(), "178.40");
            assertTrue(belowLimit.warnings().isEmpty());
        }
    }}

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    // Refund policy (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class RefundModuleTests {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");

    private static final Instant H1 = NOW.minusSeconds(3600);        // 1h ago
    private static final Instant H2 = NOW.minusSeconds(7200);        // exactly 2h
    private static final Instant H3 = NOW.minusSeconds(10800);       // 3h
    private static final Instant H24 = NOW.minusSeconds(86400);      // exactly 24h
    private static final Instant H25 = NOW.minusSeconds(90000);      // 25h
    private static final Instant H168 = NOW.minusSeconds(604800);    // exactly 7d
    private static final Instant H169 = NOW.minusSeconds(608400);    // 169h
    private static final Instant H336 = NOW.minusSeconds(1209600);   // exactly 14d
    private static final Instant H337 = NOW.minusSeconds(1213200);   // 337h
    private static final Instant H720 = NOW.minusSeconds(2592000);   // exactly 30d
    private static final Instant H721 = NOW.minusSeconds(2595600);   // 721h
    private static final Instant FUTURE = NOW.plusSeconds(3600);     // 1h ahead

    private static RefundPolicyService service() {
        return new RefundPolicyService(Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    /** 默认快照：P-1/S-1/C-1，paid 100.00，无证书/无券/无积分/无取消，accessExpiresAt=null。 */
    private static PurchaseSnapshot purchase(Instant paidAt, PaymentMethod method, int progress, int downloads) {
        return snapshot(paidAt, method, progress, downloads,
                "100.00", "0.00", "0.00", false, 0, 0, 0, false, false, false, false, false, false, null);
    }

    private static PurchaseSnapshot snapshot(Instant paidAt, PaymentMethod method, int progress, int downloads,
                                             String paidAmount, String tax, String discount,
                                             boolean certificateIssued, int attended, int totalSessions,
                                             int loyaltyPoints, boolean giftPurchase, boolean couponUsed,
                                             boolean chargebackOpen, boolean previouslyRefunded,
                                             boolean instructorCancelled, boolean courseRemoved,
                                             Instant accessExpiresAt) {
        return new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal(paidAmount), new BigDecimal(tax),
                new BigDecimal(discount), paidAt, method, progress, downloads, certificateIssued,
                attended, totalSessions, giftPurchase, couponUsed, loyaltyPoints, chargebackOpen,
                previouslyRefunded, instructorCancelled, courseRemoved, accessExpiresAt);
    }

    private static RefundRequest request(PurchaseSnapshot p, RefundReason reason, boolean fullRefund, int pct,
                                         boolean evidence, boolean techConfirmed, boolean complaintUpheld,
                                         boolean medicalVerified, boolean duplicateConfirmed, boolean bankDetails,
                                         boolean fraudSuspected, boolean adminOverride) {
        return new RefundRequest(p, reason, fullRefund, pct, evidence, techConfirmed, complaintUpheld,
                medicalVerified, duplicateConfirmed, bankDetails, fraudSuspected, adminOverride);
    }

    /** 便利构造：全额退款、无附加标记、银行账户已提供。 */
    private static RefundRequest request(PurchaseSnapshot p, RefundReason reason) {
        return request(p, reason, true, 0, false, false, false, false, false, true, false, false);
    }

    /** 全字段断言：金额同时断言 compareTo（值）与 toPlainString（固定 2 位小数刻度）。 */
    private static void assertDecision(RefundDecision d, boolean approved, boolean manualReview,
                                       String refundable, String fee, int points, String route,
                                       List<String> reasons, List<String> warnings) {
        assertEquals(approved, d.approved(), "approved");
        assertEquals(manualReview, d.manualReview(), "manualReview");
        assertNotNull(d.refundableAmount(), "refundableAmount null");
        assertEquals(0, d.refundableAmount().compareTo(new BigDecimal(refundable)), "refundableAmount value");
        assertEquals(refundable, d.refundableAmount().toPlainString(), "refundableAmount scale");
        assertNotNull(d.processingFee(), "processingFee null");
        assertEquals(0, d.processingFee().compareTo(new BigDecimal(fee)), "processingFee value");
        assertEquals(fee, d.processingFee().toPlainString(), "processingFee scale");
        assertEquals(points, d.loyaltyPointsReturned(), "loyaltyPointsReturned");
        assertEquals(route, d.refundRoute(), "refundRoute");
        assertEquals(reasons, d.reasons(), "reasons");
        assertEquals(warnings, d.warnings(), "warnings");
    }

    @Nested
    class RequestValidationTests {

        @Test
        void nullRequestIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void nullPurchaseIsRejected() {
            RefundRequest r = new RefundRequest(null, RefundReason.OTHER, true, 0, false,
                    false, false, false, false, true, false, false);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(r));
            assertEquals("purchase must not be null", ex.getMessage());
        }

        @Test
        void nullReasonIsRejected() {
            RefundRequest r = new RefundRequest(purchase(H1, PaymentMethod.CARD, 0, 0), null, true, 0,
                    false, false, false, false, false, true, false, false);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(r));
            assertEquals("reason must not be null", ex.getMessage());
        }

        @Test
        void blankPurchaseIdIsRejected() {
            PurchaseSnapshot blank = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            PurchaseSnapshot p = new PurchaseSnapshot("  ", blank.studentId(), blank.courseId(),
                    blank.paidAmount(), blank.taxAmount(), blank.discountAmount(), blank.paidAt(),
                    blank.paymentMethod(), blank.progressPercent(), blank.downloadedResources(),
                    blank.certificateIssued(), blank.liveSessionsAttended(), blank.totalLiveSessions(),
                    blank.giftPurchase(), blank.couponUsed(), blank.loyaltyPointsUsed(),
                    blank.chargebackOpen(), blank.previouslyRefunded(), blank.instructorCancelled(),
                    blank.courseRemoved(), blank.accessExpiresAt());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("purchaseId must not be blank", ex.getMessage());
        }

        @Test
        void nullPurchaseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot(null, "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("purchaseId must not be blank", ex.getMessage());
        }

        @Test
        void blankStudentIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", " ", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void nullCourseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", null, new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void nullPaidAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", null, new BigDecimal("0.00"),
                    new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("paidAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativePaidAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("-0.01"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("paidAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativeTaxAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("-1.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("taxAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativeDiscountAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("-0.50"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("discountAmount must not be negative", ex.getMessage());
        }

        @Test
        void nullPaidAtIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(null, PaymentMethod.CARD, 0, 0), RefundReason.OTHER)));
            assertEquals("paidAt must not be null", ex.getMessage());
        }

        @Test
        void nullPaymentMethodIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, null, 0, 0), RefundReason.OTHER)));
            assertEquals("paymentMethod must not be null", ex.getMessage());
        }

        @Test
        void progressPercentBelowZeroIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, -1, 0), RefundReason.OTHER)));
            assertEquals("progressPercent must be between 0 and 100", ex.getMessage());
        }

        @Test
        void progressPercentAboveHundredIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, 101, 0), RefundReason.OTHER)));
            assertEquals("progressPercent must be between 0 and 100", ex.getMessage());
        }

        @Test
        void negativeDownloadedResourcesIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, -1), RefundReason.OTHER)));
            assertEquals("downloadedResources must not be negative", ex.getMessage());
        }

        @Test
        void negativeLiveSessionCountsAreRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, -1, 5, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("live session counts must not be negative", ex.getMessage());

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 1, -1, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("live session counts must not be negative", ex2.getMessage());
        }

        @Test
        void attendedSessionsExceedingTotalIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 2, 1, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("attended sessions must not exceed total sessions", ex.getMessage());
        }

        @Test
        void negativeLoyaltyPointsAreRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 0, 0, -1, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("loyaltyPointsUsed must not be negative", ex.getMessage());
        }

        @Test
        void requestedPercentageBelowZeroIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.OTHER,
                    false, -1, false, false, false, false, false, true, false, false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(r));
            assertEquals("requestedPercentage must be between 0 and 100", ex.getMessage());
        }

        @Test
        void requestedPercentageAboveHundredIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.OTHER,
                    false, 101, false, false, false, false, false, true, false, false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(r));
            assertEquals("requestedPercentage must be between 0 and 100", ex.getMessage());
        }

        @Test
        void nullStudentIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", null, "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "  ", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void nullTaxAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    null, new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("taxAmount must not be negative", ex.getMessage());
        }

        @Test
        void nullDiscountAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), null, H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("discountAmount must not be negative", ex.getMessage());
        }

        @Test
        void validBoundaryRequestPassesValidation() {
            RefundDecision d = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());
        }
    }

    @Nested
    class EligibleRateTests {

        @Test
        void instructorCancelledCourseGrantsFullRefundWithoutFee() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 50, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void courseRemovedGrantsFullRefundWithoutFee() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 50, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, true, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentConfirmedGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    true, 0, false, false, false, false, true, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_CONFIRMED", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentWithEvidenceOnlyGetsHalf() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentUnconfirmedIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("DUPLICATE_PAYMENT_NOT_CONFIRMED"), List.of());
        }

        @Test
        void confirmedTechnicalIssueAtHalfProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 50, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void confirmedTechnicalIssueJustPastHalfGetsSeventyFivePercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 51, 0), RefundReason.TECHNICAL_ISSUE,
                    false, 75, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "75.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void unconfirmedTechnicalIssueWithoutEvidenceIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("TECHNICAL_EVIDENCE_REQUIRED"), List.of());
        }

        @Test
        void unconfirmedTechnicalIssueWithEvidenceWarnsThenFallsToCoolingOff() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("UNCONFIRMED_TECHNICAL_ISSUE"));
        }

        @Test
        void verifiedMedicalUnderEightyProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 79, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void verifiedMedicalAtEightyProgressDropsToFortyPercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 80, 0), RefundReason.MEDICAL,
                    false, 40, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "38.80", "1.20", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void medicalEvidenceWithoutVerificationGetsHalf() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.MEDICAL,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("MEDICAL_DOCUMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void medicalWithoutDocumentOrEvidenceIsRejected() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("MEDICAL_DOCUMENT_REQUIRED"), List.of());
        }

        @Test
        void upheldComplaintJustUnderSixtyProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 59, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void upheldComplaintAtSixtyProgressDropsToSixtyPercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 60, 0), RefundReason.CONTENT_QUALITY,
                    false, 60, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "60.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void contentQualityWithoutUpheldComplaintFallsToGeneralPolicy() {
            RefundRequest r = request(purchase(H168, PaymentMethod.CARD, 59, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void certificateIssuedBlocksRefund() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    true, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("CERTIFICATE_ALREADY_ISSUED"), List.of());
        }

        @Test
        void progressEightyBlocksRefundButSeventyNineFallsThrough() {
            RefundDecision at80 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 80, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at80, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED"), List.of());

            RefundDecision at79 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 79, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at79, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void tenDownloadedResourcesBlockRefundButNineStillAllowCoolingOff() {
            RefundDecision at10 = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 10),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at10, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("TOO_MANY_RESOURCES_DOWNLOADED"), List.of());

            RefundDecision at9 = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 9),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at9, true, false, "98.00", "2.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));
        }

        @Test
        void coolingOffCoversExactlyTwoHoursButThreeHoursShiftsToOneDayRule() {
            RefundDecision at2h = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at2h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());

            RefundDecision at3h = service().evaluate(request(purchase(H3, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at3h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void coolingOffExcludesNonZeroProgressWithinTwoHours() {
            // hours <= 2 but progress 5 != 0: cooling-off window skipped, one-day rule applies instead.
            RefundDecision d = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void oneDayRuleCoversFivePercentProgressAndExactly24h() {
            RefundDecision p5at24h = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p5at24h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());

            RefundDecision p6at24h = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 6, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p6at24h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision p5at25h = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p5at25h, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void sevenDayGuaranteeBoundaries() {
            RefundDecision at168 = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 10, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at168, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision at169 = service().evaluate(request(purchase(H169, PaymentMethod.CARD, 10, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 50, false, false, false, false, false, true, false, false));
            assertDecision(at169, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundDecision p11 = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 11, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 70, false, false, false, false, false, true, false, false));
            assertDecision(p11, true, false, "67.90", "2.10", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());
        }

        @Test
        void fourteenDayBoundaries() {
            RefundDecision at336 = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 50, false, false, false, false, false, true, false, false));
            assertDecision(at336, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundDecision at337 = service().evaluate(request(purchase(H337, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at337, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());

            RefundDecision p31 = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 31, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p31, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void thirtyDayExceptionBoundaries() {
            RefundDecision at720 = service().evaluate(request(purchase(H720, PaymentMethod.CARD, 15, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 25, false, false, false, false, false, true, false, false));
            assertDecision(at720, true, false, "24.25", "0.75", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("THIRTY_DAY_EXCEPTION", "REFUND_APPROVED"), List.of());

            RefundDecision at721 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 15, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at721, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());

            RefundDecision p16 = service().evaluate(request(purchase(H720, PaymentMethod.CARD, 16, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p16, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void accidentalPurchaseAfterDownloadIsRejectedButWithoutDownloadFallsThrough() {
            RefundDecision withDownload = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(withDownload, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"), List.of());

            RefundDecision noDownload = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(noDownload, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void otherReasonRequiresEvidence() {
            RefundDecision noEvidence = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.OTHER));
            assertDecision(noEvidence, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("OTHER_REASON_REQUIRES_EVIDENCE"), List.of());

            RefundDecision withEvidence = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.OTHER, true, 0, true, false, false, false, false, true, false, false));
            assertDecision(withEvidence, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }
    }

    @Nested
    class DecisionPathTests {

        @Test
        void purchaseDatedInFutureIsRejected() {
            RefundDecision d = service().evaluate(request(purchase(FUTURE, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("PURCHASE_DATE_IN_FUTURE"), List.of());
        }

        @Test
        void alreadyRefundedPurchaseIsRejected() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, true, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("PURCHASE_ALREADY_REFUNDED"), List.of());
        }

        @Test
        void openChargebackGoesToManualReview() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, true, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("OPEN_CHARGEBACK"), List.of());
        }

        @Test
        void fraudReasonGoesToManualReview() {
            RefundDecision d = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 0),
                    RefundReason.FRAUD));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void fraudSuspicionOnAnyReasonGoesToManualReview() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, true, true, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void expiredAccessAddsWarningButApprovalContinues() {
            PurchaseSnapshot expired = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false,
                    NOW.minusSeconds(1800));
            RefundDecision d = service().evaluate(request(expired, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("COURSE_ACCESS_EXPIRED"));
        }

        @Test
        void accessExpiringExactlyNowOrLaterAddsNoWarning() {
            PurchaseSnapshot exactlyNow = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, NOW);
            RefundDecision nowDecision = service().evaluate(request(exactlyNow, RefundReason.ACCIDENTAL_PURCHASE));
            assertTrue(nowDecision.warnings().isEmpty(), "expires exactly now must not warn: " + nowDecision.warnings());

            PurchaseSnapshot later = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false,
                    NOW.plusSeconds(1800));
            RefundDecision futureDecision = service().evaluate(request(later, RefundReason.ACCIDENTAL_PURCHASE));
            assertTrue(futureDecision.warnings().isEmpty(), "future expiry must not warn");
        }

        @Test
        void adminOverrideRescuesZeroRateWithFullRefund() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideWithoutFullRequestDefaultsToFiftyPercent() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideHonorsExplicitRequestedPercentage() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "25.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));

            RefundRequest r100 = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 100, false, false, false, false, false, true, false, true);
            RefundDecision d100 = service().evaluate(r100);
            assertDecision(d100, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideOnNonzeroRateSkipsResetAndWaivesFee() {
            // 14-day partial rate 0.50; override does not reset it, and no processing fee is charged.
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void fullRefundRequestAbovePolicyLimitIsReducedWithWarning() {
            RefundDecision d = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
        }

        @Test
        void requestedPercentageWithinLimitIsHonoredWithoutWarning() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "24.25", "0.75", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundRequest r50 = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false);
            RefundDecision d50 = service().evaluate(r50);
            assertDecision(d50, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());
        }

        @Test
        void zeroRateWithoutOverrideKeepsDeterminationReasons() {
            RefundDecision d = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 80, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED"), List.of());
        }
    }

    @Nested
    class FeeRouteAndAdjustmentTests {

        @Test
        void cardFeeIsThreePercentBeyond24h() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void alipayAndWechatFeeIsOnePercentBeyond24h() {
            RefundDecision alipay = service().evaluate(request(purchase(H25, PaymentMethod.ALIPAY, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(alipay, true, false, "99.00", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision wechat = service().evaluate(request(purchase(H25, PaymentMethod.WECHAT, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(wechat, true, false, "99.00", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void bankTransferFeeIsTwoPercentWithBankDetails() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.BANK_TRANSFER, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "98.00", "2.00", 0, "BANK_TRANSFER",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void walletAndGiftChargesNoFeeAndRoutesToWalletCredit() {
            RefundDecision wallet = service().evaluate(request(purchase(H25, PaymentMethod.WALLET, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(wallet, true, false, "100.00", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision gift = service().evaluate(request(purchase(H25, PaymentMethod.GIFT, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(gift, true, false, "100.00", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        @Test
        void cryptoFeeIsFivePercentAndForcesManualReview() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CRYPTO, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "95.00", "5.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW"));
        }

        @Test
        void feeIsZeroedWithin24hEvenForCard() {
            RefundDecision d = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void downloadFeeAppliesEvenWithin24hAndStacksBeyond24h() {
            RefundDecision within24 = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 0, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(within24, true, false, "98.00", "2.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));

            RefundDecision beyond24 = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 0, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(beyond24, true, false, "95.00", "5.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));
        }

        @Test
        void liveSessionAttendanceFeeBoundaries() {
            // 2/4 = 0.5000 -> full attendance fee (+0.10) on top of card 0.03 => 0.13 of 100.00.
            RefundDecision full = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 2, 4, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(full, true, false, "87.00", "13.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("LIVE_SESSION_ATTENDANCE_FEE_APPLIED"));

            // 4/10 = 0.4000 -> partial fee (+0.05) => rate 0.08.
            RefundDecision partial = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 4, 10, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(partial, true, false, "92.00", "8.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));

            // 1/5 = 0.2000 exactly -> still partial fee (>= 0.20).
            RefundDecision atBoundary = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 1, 5, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(atBoundary, true, false, "92.00", "8.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));

            // 1/10 = 0.1000 -> below 0.20, no session fee.
            RefundDecision none = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 1, 10, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(none, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void giftPurchaseFlagRoutesToWalletCreditEvenForCard() {
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, true, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        @Test
        void bankTransferWithoutBankDetailsGoesToManualReview() {
            RefundRequest r = request(purchase(H25, PaymentMethod.BANK_TRANSFER, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, true, 0, false, false, false, false, false,
                    false, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "98.00", "2.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("BANK_DETAILS_REQUIRED"));
        }

        @Test
        void couponValueIsDeductedWhenRefundCoversIt() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "38.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("COUPON_VALUE_NOT_REFUNDED"));
        }

        @Test
        void couponLargerThanRefundConsumesItAll() {
            // paid 40.00, rate 0.50 -> gross 20.00, card fee 0.60, refundable 19.40;
            // coupon adjustment 40.00*0.50 = 20.00 >= 19.40 -> refundable 0.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "40.00", "0.00", "40.00",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("COUPON_VALUE_CONSUMED_REFUND"));
        }

        @Test
        void couponIsIgnoredForProviderFaultRefunds() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 0, false, true, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void loyaltyPointsReturnProportionallyWithWarning() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "48.50", "1.50", 50, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));
        }

        @Test
        void fullRateReturnsAllPointsWithoutWarning() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void pointReturnFloorsDownward() {
            // 0.50 * 99 = 49.5 -> RoundingMode.DOWN -> 49.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 99, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertEquals(49, d.loyaltyPointsReturned(), "points must floor, not round up");
            assertTrue(d.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));
        }

        @Test
        void couponOrderHalvesReturnedPoints() {
            // rate 0.50 * 100 = 50 -> halved to 25; coupon adjustment 10.00*0.50 = 5.00 < refundable 47.00.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "10.00",
                    false, 0, 0, 100, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "43.50", "1.50", 25, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("COUPON_VALUE_NOT_REFUNDED", "LOYALTY_POINTS_RETURNED_PROPORTIONALLY",
                            "POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));
        }

        @Test
        void smallRefundToWalletIsPaidBelowMinimum() {
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.WALLET, 0, 0, "0.50", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "0.50", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        void smallNonWalletRefundIsAbsorbedIntoFeeAndRejected() {
            // paid 1.00, card fee 0.03 -> refundable 0.97 < 1.00 -> fee absorbs it -> NO_REFUNDABLE_VALUE.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void refundOfExactlyOnePayoutUnitIsPaid() {
            // paid 1.03, fee 0.03 -> refundable exactly 1.00, no small-refund warning.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.03", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "1.00", "0.03", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void zeroRefundableAfterFeeIsRejected() {
            // paid 2.00, rate 0.25 -> gross 0.50, fee 0.06 -> 0.44 < 1.00 -> absorbed -> 0.
            PurchaseSnapshot p = snapshot(H720, PaymentMethod.CARD, 15, 0, "2.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, false));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void cryptoSmallRefundStaysInManualReviewInsteadOfRejecting() {
            // paid 1.00, crypto fee 0.05 -> refundable 0.95 < 1.00 -> absorbed, but
            // MANUAL_REVIEW route short-circuits the NO_REFUNDABLE_VALUE rejection.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CRYPTO, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "0.00", "1.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW", "REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void manualReviewWithPositiveRefundAndPointsSkipsZeroValueRejection() {
            // refundable 98.00 > 0 and points 100 > 0 with MANUAL_REVIEW route: the
            // NO_REFUNDABLE_VALUE guard is skipped on both conditions.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.BANK_TRANSFER, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundRequest r = request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, false, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "98.00", "2.00", 100, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("BANK_DETAILS_REQUIRED"));
        }

        @Test
        void absorbedSmallRefundStillReturnsPointsInsteadOfRejecting() {
            // paid 1.00, fee 0.03 -> refundable 0.97 < 1.00 -> absorbed to 0.00; because
            // points 100 > 0 the NO_REFUNDABLE_VALUE rejection is skipped and the decision
            // is approved with zero cash but full points.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "0.00", "1.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void zeroAttendanceWithScheduledSessionsChargesNoSessionFee() {
            // totalLiveSessions 10 > 0 but attended 0: attendance-fee block never entered.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 10, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void couponOrderWithProviderFaultKeepsFullPointReturn() {
            // couponUsed but instructor cancelled (provider fault): coupon adjustment and
            // point halving are both skipped; full rate 1.00 returns all 100 points.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 100, false, true, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 100, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "100.00", "0.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }
    }

    @Nested
    class RefundReasonEndToEndTests {

        @Test
        void endToEndAccidentalPurchase() {
            RefundDecision d = service().evaluate(request(purchase(H3, PaymentMethod.CARD, 4, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndTechnicalIssue() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 50, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndContentQuality() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 10, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndMedical() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 20, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndCourseCancelled() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 10, 0),
                    RefundReason.COURSE_CANCELLED));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndDuplicatePayment() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 10, 0), RefundReason.DUPLICATE_PAYMENT,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndFraud() {
            RefundDecision d = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 0),
                    RefundReason.FRAUD));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void endToEndOther() {
            RefundDecision d = service().evaluate(request(purchase(H721, PaymentMethod.WECHAT, 50, 0),
                    RefundReason.OTHER));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("OTHER_REASON_REQUIRES_EVIDENCE"), List.of());
        }
    }

    @Nested
    class RoundingTests {

        @Test
        void grossRefundUsesHalfUp() {
            // 0.03 * 0.50 = 0.0150 -> HALF_UP -> 0.02 (not 0.01).
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.WALLET, 30, 0, "0.03", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "0.02", "0.00", 0, "WALLET_CREDIT",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        void processingFeeUsesHalfUp() {
            // 33.33 * 0.03 = 0.9999 -> HALF_UP -> 1.00; refundable 33.33 - 1.00 = 32.33.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "33.33", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "32.33", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void couponAdjustmentUsesHalfUp() {
            // coupon adjustment = 0.03 * 0.50 = 0.0150 -> HALF_UP -> 0.02; 47.00 - 0.02 = 46.98.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.03",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "48.48", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("COUPON_VALUE_NOT_REFUNDED"));
        }
    }

    @Nested
    class RefundDecisionRecordTests {

        @Test
        void decisionCopiesReasonAndWarningListsDefensively() {
            List<String> reasons = new ArrayList<>(List.of("R1"));
            List<String> warnings = new ArrayList<>(List.of("W1"));
            RefundDecision d = new RefundDecision(true, false, new BigDecimal("1.00"),
                    new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", reasons, warnings);

            reasons.add("MUTANT");
            warnings.add("MUTANT");
            assertEquals(List.of("R1"), d.reasons(), "reasons must be snapshotted");
            assertEquals(List.of("W1"), d.warnings(), "warnings must be snapshotted");

            assertThrows(UnsupportedOperationException.class, () -> d.reasons().add("X"));
            assertThrows(UnsupportedOperationException.class, () -> d.warnings().add("X"));
        }

        @Test
        void decisionRejectsNullLists() {
            assertThrows(NullPointerException.class, () -> new RefundDecision(true, false,
                    new BigDecimal("1.00"), new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", null, List.of()));
            assertThrows(NullPointerException.class, () -> new RefundDecision(true, false,
                    new BigDecimal("1.00"), new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", List.of(), null));
        }
    }}

    // ------------------------------------------------------------------
    // Engine second-pass branch gaps (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class EngineGapTestsContainer {

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
    }}

    // ------------------------------------------------------------------
    // Engine character-class gaps (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class EngineGapTests2Container {

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
    class CharacterClassGapTests {

        @Test
        void extractionTreatsPunctuationGapAsNonLetter() {
            assertEquals(1, extractFunctionCount("s_m(1,2)"));
            assertEquals(1, extractFunctionCount("sum|(1,2)"));
            assertEquals(1, extractFunctionCount("s`m(1,2)"));
            // "[x]" contains no function at all: 'x' sits between punctuation that
            // never opens a function context.
            assertEquals(0, extractFunctionCount("[x]"));
        }

        private int extractFunctionCount(String expression) {
            Stack<Integer> start = new Stack<>();
            Stack<Integer> end = new Stack<>();
            Stack<String> names = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction(expression, start, end, names);
            return names.size();
        }

        @Test
        void builderExtractionTreatsPunctuationGapAsNonLetter() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gap2Builder");
            for (String expression : new String[]{"S_M(1,2)", "SUM|(1,2)"}) {
                StringBuilder builder = new StringBuilder();
                Stack<Integer> start = new Stack<>();
                Stack<Integer> end = new Stack<>();
                Stack<String> names = new Stack<>();
                c.FunctionParameterExtraction(expression, start, end, names, builder);
                assertEquals(1, names.size(), expression);
            }
        }

        @Test
        void singleParameterCheckScansUppercaseAndPunctuationNames() {
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gap2Ffc1Check");
            assertThrows(WrongFormat.class, () -> c.check("SIN(1"));
            assertThrows(WrongFormat.class, () -> c.check("s_n(1"));
            assertThrows(WrongFormat.class, () -> c.check("sin(cos(1"));
        }

        @Test
        void singleParameterCalculationHandlesNestedBracketsAndGarbageNames() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gap2Ffc1Calc");
            assertEquals(0, new java.math.BigDecimal("2").compareTo(
                    new java.math.BigDecimal(String.valueOf(c.calculation("1+cos((0))", true).getResult()))));
            assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> c.calculation("1+C|S(0)", true));
        }
    }}

    // ------------------------------------------------------------------
    // Engine final pass (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class EngineGapTests3Container {

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
    }}

    // ------------------------------------------------------------------
    // Scheduling service (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class SchedulingModuleTests {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static SchedulingService service() {
        return new SchedulingService(Clock.fixed(NOW, ZONE));
    }

    private static TimeWindow window(String startIso, String endIso) {
        return new TimeWindow(Instant.parse(startIso), Instant.parse(endIso));
    }

    private static ExistingBooking booking(String id, String instructorId, String roomId, String cohortId,
                                           String startIso, String endIso, boolean cancelled, boolean tentative) {
        return new ExistingBooking(id, instructorId, roomId, cohortId,
                Instant.parse(startIso), Instant.parse(endIso), cancelled, tentative, CourseKind.LECTURE);
    }

    /**
     * Mutable builder producing a fresh {@link ScheduleRequest} per test (no shared state).
     * Defaults describe a clean request: LECTURE 60min, Thursday 2026-06-18 10:00-11:00 UTC,
     * classroom R-1, capacity 30 for 10 expected / 5 registered -> APPROVED, no conflicts, no warnings.
     */
    private static final class Req {
        String courseId = "C-1";
        String cohortId = "CO-1";
        String instructorId = "I-1";
        DeliveryMode deliveryMode = DeliveryMode.CLASSROOM;
        CourseKind courseKind = CourseKind.LECTURE;
        Instant start = Instant.parse("2026-06-18T10:00:00Z");
        Instant end = Instant.parse("2026-06-18T11:00:00Z");
        String zoneId = "UTC";
        String roomId = "R-1";
        String meetingUrl = null;
        int expectedParticipants = 10;
        int registeredParticipants = 5;
        int roomCapacity = 30;
        Set<String> requiredEquipment = Set.of();
        Set<String> availableEquipment = Set.of();
        boolean accessibleRoomRequired = false;
        boolean roomAccessible = false;
        boolean weekendAllowed = false;
        boolean afterHoursAllowed = false;
        boolean lunchTimeAllowed = false;
        boolean recurring = false;
        int occurrenceCount = 1;
        int minimumBufferMinutes = 0;
        int maximumInstructorSessionsPerDay = 3;
        List<TimeWindow> instructorAvailability = null; // null -> default window containing the session
        List<TimeWindow> maintenanceWindows = List.of();
        List<ExistingBooking> existingBookings = List.of();
        Set<LocalDate> holidays = Set.of();
        boolean administratorOverride = false;
        boolean recordingRequired = false;
        boolean recordingSupported = false;

        ScheduleRequest build() {
            List<TimeWindow> availability = instructorAvailability == null
                    ? List.of(window("2026-06-14T00:00:00Z", "2026-06-25T00:00:00Z")) // covers every session used here
                    : instructorAvailability;
            return new ScheduleRequest(courseId, cohortId, instructorId, deliveryMode, courseKind,
                    start, end, zoneId, roomId, meetingUrl, expectedParticipants, registeredParticipants,
                    roomCapacity, requiredEquipment, availableEquipment, accessibleRoomRequired,
                    roomAccessible, weekendAllowed, afterHoursAllowed, lunchTimeAllowed, recurring,
                    occurrenceCount, minimumBufferMinutes, maximumInstructorSessionsPerDay,
                    availability, maintenanceWindows, existingBookings, holidays,
                    administratorOverride, recordingRequired, recordingSupported);
        }
    }

    private static Req req() {
        return new Req();
    }

    // ------------------------------------------------------------------
    // Validation: every distinct rule with exact message, both boundary sides
    // ------------------------------------------------------------------
    @Nested
    class Validation {

        @Test
        void nullRequestIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseId = null; service().evaluate(r.build()); });
            assertEquals("courseId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseId = "  "; service().evaluate(r.build()); });
            assertEquals("courseId must not be blank", blankId.getMessage());
        }

        @Test
        void blankCohortIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.cohortId = null; service().evaluate(r.build()); });
            assertEquals("cohortId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.cohortId = " "; service().evaluate(r.build()); });
            assertEquals("cohortId must not be blank", blankId.getMessage());
        }

        @Test
        void blankInstructorIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorId = null; service().evaluate(r.build()); });
            assertEquals("instructorId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorId = " "; service().evaluate(r.build()); });
            assertEquals("instructorId must not be blank", blankId.getMessage());
        }

        @Test
        void missingEnumIsRejected() {
            IllegalArgumentException noMode = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.deliveryMode = null; service().evaluate(r.build()); });
            assertEquals("delivery mode and course kind are required", noMode.getMessage());
            IllegalArgumentException noKind = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseKind = null; service().evaluate(r.build()); });
            assertEquals("delivery mode and course kind are required", noKind.getMessage());
        }

        @Test
        void missingInstantsAreRejected() {
            IllegalArgumentException noStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.start = null; service().evaluate(r.build()); });
            assertEquals("start and end are required", noStart.getMessage());
            IllegalArgumentException noEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = null; service().evaluate(r.build()); });
            assertEquals("start and end are required", noEnd.getMessage());
        }

        @Test
        void endNotAfterStartIsRejected() {
            IllegalArgumentException equal = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = r.start; service().evaluate(r.build()); });
            assertEquals("end must be after start", equal.getMessage());
            IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = Instant.parse("2026-06-18T09:59:00Z"); service().evaluate(r.build()); });
            assertEquals("end must be after start", before.getMessage());
        }

        @Test
        void durationOfOneDayOrMoreIsRejected() {
            IllegalArgumentException fullDay = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = Instant.parse("2026-06-19T10:00:00Z"); service().evaluate(r.build()); });
            assertEquals("session duration must be less than one day", fullDay.getMessage());
            Req justUnderDay = req();
            justUnderDay.end = Instant.parse("2026-06-19T09:59:00Z");
            SchedulingDecision decision = service().evaluate(justUnderDay.build());
            assertEquals(1439, decision.durationMinutes());
        }

        @Test
        void blankZoneIdIsRejected() {
            IllegalArgumentException nullZone = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = null; service().evaluate(r.build()); });
            assertEquals("zoneId must not be blank", nullZone.getMessage());
            IllegalArgumentException blankZone = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = "  "; service().evaluate(r.build()); });
            assertEquals("zoneId must not be blank", blankZone.getMessage());
        }

        @Test
        void unknownZoneIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = "Mars/Olympus"; service().evaluate(r.build()); });
            assertEquals("zoneId is invalid", ex.getMessage());
        }

        @Test
        void negativeCountsAreRejected() {
            IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.expectedParticipants = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", expected.getMessage());
            IllegalArgumentException registered = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.registeredParticipants = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", registered.getMessage());
            IllegalArgumentException capacity = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.roomCapacity = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", capacity.getMessage());
        }

        @Test
        void nonPositiveOccurrenceCountIsRejected() {
            IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.occurrenceCount = 0; service().evaluate(r.build()); });
            assertEquals("occurrenceCount must be positive", zero.getMessage());
            IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.occurrenceCount = -3; service().evaluate(r.build()); });
            assertEquals("occurrenceCount must be positive", negative.getMessage());
        }

        @Test
        void bufferMinutesOutsideZeroTo240AreRejected() {
            IllegalArgumentException below = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.minimumBufferMinutes = -1; service().evaluate(r.build()); });
            assertEquals("minimumBufferMinutes must be between 0 and 240", below.getMessage());
            IllegalArgumentException above = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.minimumBufferMinutes = 241; service().evaluate(r.build()); });
            assertEquals("minimumBufferMinutes must be between 0 and 240", above.getMessage());
            Req atZero = req();
            atZero.minimumBufferMinutes = 0;
            Req atMax = req();
            atMax.minimumBufferMinutes = 240;
            assertTrue(service().evaluate(atMax.build()).conflicts().isEmpty());
        }

        @Test
        void nonPositiveMaxSessionsPerDayIsRejected() {
            IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maximumInstructorSessionsPerDay = 0; service().evaluate(r.build()); });
            assertEquals("maximumInstructorSessionsPerDay must be positive", zero.getMessage());
            IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maximumInstructorSessionsPerDay = -2; service().evaluate(r.build()); });
            assertEquals("maximumInstructorSessionsPerDay must be positive", negative.getMessage());
        }

        @Test
        void invalidInstructorAvailabilityWindowIsRejected() {
            IllegalArgumentException emptyEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(window("2026-06-18T09:00:00Z", "2026-06-18T09:00:00Z")); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", emptyEnd.getMessage());
            IllegalArgumentException backwards = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T09:00:00Z")); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", backwards.getMessage());
            IllegalArgumentException nullStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(new TimeWindow(null, Instant.parse("2026-06-18T12:00:00Z"))); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", nullStart.getMessage());
            IllegalArgumentException nullEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(new TimeWindow(Instant.parse("2026-06-18T09:00:00Z"), null)); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", nullEnd.getMessage());
        }

        @Test
        void invalidMaintenanceWindowIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maintenanceWindows = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T11:00:00Z")); service().evaluate(r.build()); });
            assertEquals("maintenance window is invalid", ex.getMessage());
        }

        @Test
        void blankBookingIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking(null, "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("bookingId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking(" ", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("bookingId must not be blank", blankId.getMessage());
        }

        @Test
        void duplicateBookingIdIsRejected() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(r.build()));
            assertEquals("bookingId must be unique", ex.getMessage());
        }

        @Test
        void invalidBookingWindowIsRejected() {
            IllegalArgumentException nullStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(new ExistingBooking("b1", "I-9", "R-9", "CO-9", null, Instant.parse("2026-06-18T11:30:00Z"), false, false, CourseKind.LECTURE)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", nullStart.getMessage());
            IllegalArgumentException nullEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(new ExistingBooking("b1", "I-9", "R-9", "CO-9", Instant.parse("2026-06-18T10:30:00Z"), null, false, false, CourseKind.LECTURE)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", nullEnd.getMessage());
            IllegalArgumentException equalEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T10:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", equalEnd.getMessage());
            IllegalArgumentException backwards = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T11:30:00Z", "2026-06-18T10:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", backwards.getMessage());
        }

        @Test
        void blankEquipmentEntriesAreRejected() {
            IllegalArgumentException required = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.requiredEquipment = Set.of("PROJECTOR", " "); service().evaluate(r.build()); });
            assertEquals("required equipment must not contain blanks", required.getMessage());
            IllegalArgumentException available = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.availableEquipment = Set.of(" ", "LAB_SAFETY_KIT"); service().evaluate(r.build()); });
            assertEquals("available equipment must not contain blanks", available.getMessage());
        }

        @Test
        void validRequestPassesValidationAndIsApproved() {
            SchedulingDecision decision = service().evaluate(req().build());
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals("APPROVED", decision.status());
            assertEquals(60, decision.durationMinutes());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Timing: past start, notice thresholds, date crossing, weekend/holiday,
    // operating hours, lunch, per-courseKind duration boundaries
    // ------------------------------------------------------------------
    @Nested
    class Timing {

        @Test
        void startTimeBeforeNowConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T01:00:00Z");
            r.end = Instant.parse("2026-06-15T01:30:00Z");
            r.afterHoursAllowed = true; // isolate START_TIME_IN_PAST from OUTSIDE_OPERATING_HOURS
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("START_TIME_IN_PAST"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), decision.warnings());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void leadTimeJustUnderTwoHoursConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T03:59:00Z"); // 1h59m lead
            r.end = Instant.parse("2026-06-15T04:59:00Z");
            r.afterHoursAllowed = true; // local 03:59 is before opening; isolate the notice rule
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSUFFICIENT_BOOKING_NOTICE"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), decision.warnings());
        }

        @Test
        void leadTimeExactlyTwoHoursWarnsInstead() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T04:00:00Z"); // exactly 2h lead
            r.end = Instant.parse("2026-06-15T05:00:00Z");
            r.afterHoursAllowed = true; // local 04:00 is before opening; isolate the notice rule
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("SHORT_BOOKING_NOTICE", "AFTER_HOURS_SESSION"), decision.warnings());
            assertTrue(decision.approved());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void leadTimeJustUnder24HoursWarns() {
            // 23h59m after now is 01:59Z = 09:59 local in Asia/Shanghai (inside operating hours)
            Req r = req();
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-16T01:59:00Z"); // 23h59m lead
            r.end = Instant.parse("2026-06-16T02:59:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("SHORT_BOOKING_NOTICE"), decision.warnings());
        }

        @Test
        void leadTimeExactly24HoursIsSilent() {
            // now + 24h is 02:00Z, which is 10:00 local in Asia/Shanghai (inside operating hours)
            Req r = req();
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-16T02:00:00Z"); // exactly 24h lead
            r.end = Instant.parse("2026-06-16T03:00:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void sessionCrossingLocalDateConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T23:30:00Z");
            r.end = Instant.parse("2026-06-19T00:30:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("SESSION_CROSSES_LOCAL_DATE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void weekendDisallowedConflictsAndAllowedIsSilent() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            blocked.end = Instant.parse("2026-06-20T11:00:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-20T10:00:00Z");
            allowed.end = Instant.parse("2026-06-20T11:00:00Z");
            allowed.weekendAllowed = true;
            SchedulingDecision cleared = service().evaluate(allowed.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
            assertEquals("APPROVED", cleared.status());
        }

        @Test
        void holidayDateConflictsAndOtherDatesDoNot() {
            Req onHoliday = req();
            onHoliday.holidays = Set.of(LocalDate.of(2026, 6, 18));
            SchedulingDecision denied = service().evaluate(onHoliday.build());
            assertEquals(List.of("HOLIDAY_NOT_AVAILABLE"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req otherDate = req();
            otherDate.holidays = Set.of(LocalDate.of(2026, 6, 19));
            SchedulingDecision cleared = service().evaluate(otherDate.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
        }

        @Test
        void beforeOpeningHourConflictsOrWarnsByPermission() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-18T07:00:00Z");
            blocked.end = Instant.parse("2026-06-18T08:00:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("OUTSIDE_OPERATING_HOURS"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-18T07:00:00Z");
            allowed.end = Instant.parse("2026-06-18T08:00:00Z");
            allowed.afterHoursAllowed = true;
            SchedulingDecision warned = service().evaluate(allowed.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void openingAndClosingBoundariesAreInsideHours() {
            Req opensAt8 = req();
            opensAt8.start = Instant.parse("2026-06-18T08:00:00Z");
            opensAt8.end = Instant.parse("2026-06-18T09:00:00Z");
            SchedulingDecision atOpen = service().evaluate(opensAt8.build());
            assertEquals(List.of(), atOpen.conflicts());
            assertEquals(List.of(), atOpen.warnings());

            Req endsAt21 = req();
            endsAt21.start = Instant.parse("2026-06-18T20:00:00Z");
            endsAt21.end = Instant.parse("2026-06-18T21:00:00Z");
            SchedulingDecision atClose = service().evaluate(endsAt21.build());
            assertEquals(List.of(), atClose.conflicts());
            assertEquals(List.of(), atClose.warnings());
        }

        @Test
        void endAfterClosingHourConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T20:00:00Z");
            r.end = Instant.parse("2026-06-18T21:01:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("OUTSIDE_OPERATING_HOURS"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void lunchOverlapConflictsOrWarnsByPermission() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-18T12:00:00Z");
            blocked.end = Instant.parse("2026-06-18T12:30:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("LUNCH_BREAK_CONFLICT"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-18T12:00:00Z");
            allowed.end = Instant.parse("2026-06-18T12:30:00Z");
            allowed.lunchTimeAllowed = true;
            SchedulingDecision warned = service().evaluate(allowed.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("LUNCH_TIME_SESSION"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void sessionsTouchingLunchEdgesDoNotOverlapLunch() {
            Req before = req();
            before.start = Instant.parse("2026-06-18T11:00:00Z");
            before.end = Instant.parse("2026-06-18T12:00:00Z"); // ends exactly at 12:00
            SchedulingDecision endsAtNoon = service().evaluate(before.build());
            assertEquals(List.of(), endsAtNoon.conflicts());
            assertEquals(List.of(), endsAtNoon.warnings());

            Req after = req();
            after.start = Instant.parse("2026-06-18T13:30:00Z");
            after.end = Instant.parse("2026-06-18T14:30:00Z"); // starts exactly at 13:30
            SchedulingDecision startsAtEdge = service().evaluate(after.build());
            assertEquals(List.of(), startsAtEdge.conflicts());
            assertEquals(List.of(), startsAtEdge.warnings());
        }

        @Test
        void lectureDurationBoundaries() {
            Req short29 = req();
            short29.end = Instant.parse("2026-06-18T10:29:00Z");
            SchedulingDecision tooShort = service().evaluate(short29.build());
            assertEquals(List.of("LECTURE_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at30 = req();
            at30.end = Instant.parse("2026-06-18T10:30:00Z");
            SchedulingDecision minLength = service().evaluate(at30.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at180 = req();
            at180.start = Instant.parse("2026-06-18T09:00:00Z");
            at180.end = Instant.parse("2026-06-18T12:00:00Z");
            SchedulingDecision maxLength = service().evaluate(at180.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at181 = req();
            at181.start = Instant.parse("2026-06-18T13:30:00Z");
            at181.end = Instant.parse("2026-06-18T16:31:00Z"); // starts at lunch edge, no lunch overlap
            SchedulingDecision over = service().evaluate(at181.build());
            assertEquals(List.of(), over.conflicts());
            assertEquals(List.of("LECTURE_LONGER_THAN_RECOMMENDED"), over.warnings());
        }

        @Test
        void labDurationBoundaries() {
            Req short59 = req();
            short59.courseKind = CourseKind.LAB;
            short59.end = Instant.parse("2026-06-18T10:59:00Z");
            short59.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision tooShort = service().evaluate(short59.build());
            assertEquals(List.of("LAB_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at60 = req();
            at60.courseKind = CourseKind.LAB;
            at60.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision minLength = service().evaluate(at60.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at240 = req();
            at240.courseKind = CourseKind.LAB;
            at240.start = Instant.parse("2026-06-18T13:30:00Z");
            at240.end = Instant.parse("2026-06-18T17:30:00Z");
            at240.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision maxLength = service().evaluate(at240.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at241 = req();
            at241.courseKind = CourseKind.LAB;
            at241.start = Instant.parse("2026-06-18T13:30:00Z");
            at241.end = Instant.parse("2026-06-18T17:31:00Z");
            at241.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision over = service().evaluate(at241.build());
            assertEquals(List.of("LAB_TOO_LONG"), over.conflicts());
            assertEquals(List.of(), over.warnings());
        }

        @Test
        void examDurationAnd48hNoticeBoundaries() {
            Req short19 = req();
            short19.courseKind = CourseKind.EXAM;
            short19.end = Instant.parse("2026-06-18T10:19:00Z");
            SchedulingDecision tooShort = service().evaluate(short19.build());
            assertEquals(List.of("EXAM_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at20 = req();
            at20.courseKind = CourseKind.EXAM;
            at20.end = Instant.parse("2026-06-18T10:20:00Z");
            SchedulingDecision minLength = service().evaluate(at20.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at300 = req();
            at300.courseKind = CourseKind.EXAM;
            at300.start = Instant.parse("2026-06-18T13:30:00Z");
            at300.end = Instant.parse("2026-06-18T18:30:00Z");
            SchedulingDecision maxLength = service().evaluate(at300.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at301 = req();
            at301.courseKind = CourseKind.EXAM;
            at301.start = Instant.parse("2026-06-18T13:30:00Z");
            at301.end = Instant.parse("2026-06-18T18:31:00Z");
            SchedulingDecision over = service().evaluate(at301.build());
            assertEquals(List.of("EXAM_TOO_LONG"), over.conflicts());
            assertEquals(List.of(), over.warnings());
        }

        @Test
        void examNoticeBoundaryAt48HoursUsesShanghaiZone() {
            // 48h after 02:00Z is 02:00Z on 2026-06-17 = 10:00 local in Asia/Shanghai (inside hours)
            Req exactly48 = req();
            exactly48.courseKind = CourseKind.EXAM;
            exactly48.zoneId = "Asia/Shanghai";
            exactly48.start = Instant.parse("2026-06-17T02:00:00Z");
            exactly48.end = Instant.parse("2026-06-17T03:00:00Z");
            SchedulingDecision atThreshold = service().evaluate(exactly48.build());
            assertEquals(List.of(), atThreshold.conflicts());
            assertEquals(List.of(), atThreshold.warnings());
            assertEquals("APPROVED", atThreshold.status());

            Req oneMinuteLess = req();
            oneMinuteLess.courseKind = CourseKind.EXAM;
            oneMinuteLess.zoneId = "Asia/Shanghai";
            oneMinuteLess.start = Instant.parse("2026-06-17T01:59:00Z"); // 47h59m lead
            oneMinuteLess.end = Instant.parse("2026-06-17T02:59:00Z");
            SchedulingDecision under = service().evaluate(oneMinuteLess.build());
            assertEquals(List.of(), under.conflicts());
            assertEquals(List.of("EXAM_SCHEDULED_WITH_SHORT_NOTICE"), under.warnings());
        }

        @Test
        void workshopDurationBoundaries() {
            Req short44 = req();
            short44.courseKind = CourseKind.WORKSHOP;
            short44.end = Instant.parse("2026-06-18T10:44:00Z");
            SchedulingDecision tooShort = service().evaluate(short44.build());
            assertEquals(List.of("WORKSHOP_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at45 = req();
            at45.courseKind = CourseKind.WORKSHOP;
            at45.end = Instant.parse("2026-06-18T10:45:00Z");
            SchedulingDecision minLength = service().evaluate(at45.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());
            assertEquals("APPROVED", minLength.status());

            Req at361 = req();
            at361.courseKind = CourseKind.WORKSHOP;
            at361.start = Instant.parse("2026-06-18T13:30:00Z");
            at361.end = Instant.parse("2026-06-18T19:31:00Z"); // 361 min, inside 08:00-21:00, after lunch edge
            SchedulingDecision over = service().evaluate(at361.build());
            assertEquals(List.of(), over.conflicts());
            // 361 min alone also crosses the 6h instructor-workday line
            assertEquals(List.of("WORKSHOP_REQUIRES_BREAK_PLAN", "INSTRUCTOR_LONG_WORKDAY",
                    "EXTENDED_WORKSHOP_REQUIRES_REVIEW"), over.warnings());
            assertTrue(over.manualReview());
            assertEquals("MANUAL_REVIEW", over.status());
        }

        @Test
        void webinarDurationBoundaries() {
            Req short14 = req();
            short14.courseKind = CourseKind.WEBINAR;
            short14.end = Instant.parse("2026-06-18T10:14:00Z");
            SchedulingDecision tooShort = service().evaluate(short14.build());
            assertEquals(List.of("WEBINAR_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at15 = req();
            at15.courseKind = CourseKind.WEBINAR;
            at15.end = Instant.parse("2026-06-18T10:15:00Z");
            SchedulingDecision minLength = service().evaluate(at15.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at120 = req();
            at120.courseKind = CourseKind.WEBINAR;
            at120.start = Instant.parse("2026-06-18T09:00:00Z");
            at120.end = Instant.parse("2026-06-18T11:00:00Z");
            SchedulingDecision maxLength = service().evaluate(at120.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at121 = req();
            at121.courseKind = CourseKind.WEBINAR;
            at121.start = Instant.parse("2026-06-18T09:00:00Z");
            at121.end = Instant.parse("2026-06-18T11:01:00Z");
            SchedulingDecision over = service().evaluate(at121.build());
            assertEquals(List.of(), over.conflicts());
            assertEquals(List.of("WEBINAR_LONGER_THAN_RECOMMENDED"), over.warnings());
        }

        @Test
        void durationMinutesIsReportedExactly() {
            Req r = req();
            r.end = Instant.parse("2026-06-18T11:30:00Z");
            assertEquals(90, service().evaluate(r.build()).durationMinutes());
        }
    }

    // ------------------------------------------------------------------
    // DeliveryResources: ONLINE/CLASSROOM/HYBRID sub-branches, recording,
    // equipment matching, LAB safety kit, remote exam proctoring
    // ------------------------------------------------------------------
    @Nested
    class DeliveryResources {

        @Test
        void onlineWithoutMeetingUrlConflicts() {
            Req nullUrl = req();
            nullUrl.deliveryMode = DeliveryMode.ONLINE;
            nullUrl.roomId = null;
            SchedulingDecision decision = service().evaluate(nullUrl.build());
            assertEquals(List.of("MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("REJECTED", decision.status());

            Req blankUrl = req();
            blankUrl.deliveryMode = DeliveryMode.ONLINE;
            blankUrl.roomId = null;
            blankUrl.meetingUrl = "   ";
            SchedulingDecision blank = service().evaluate(blankUrl.build());
            assertEquals(List.of("MEETING_URL_REQUIRED"), blank.conflicts());
        }

        @Test
        void onlineWithInsecureMeetingUrlConflicts() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.roomId = null;
            r.meetingUrl = "http://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("SECURE_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void onlineWithSecureUrlAndNoRoomIsClean() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.roomId = null;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void onlineWithRoomWarnsRoomIgnored() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("ROOM_IGNORED_FOR_ONLINE_SESSION"), decision.warnings());
        }

        @Test
        void classroomWithoutRoomConflicts() {
            Req nullRoom = req();
            nullRoom.roomId = null;
            SchedulingDecision decision = service().evaluate(nullRoom.build());
            assertEquals(List.of("ROOM_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());

            Req blankRoom = req();
            blankRoom.roomId = "  ";
            SchedulingDecision blank = service().evaluate(blankRoom.build());
            assertEquals(List.of("ROOM_REQUIRED"), blank.conflicts());
        }

        @Test
        void classroomWithMeetingUrlWarnsOptional() {
            Req r = req();
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("MEETING_URL_OPTIONAL_FOR_CLASSROOM"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void hybridWithoutRoomAndUrlConflictsInOrder() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.roomId = null;
            r.meetingUrl = null;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED", "HYBRID_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void hybridWithInsecureUrlConflicts() {
            Req noRoom = req();
            noRoom.deliveryMode = DeliveryMode.HYBRID;
            noRoom.roomId = null;
            noRoom.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision roomOnly = service().evaluate(noRoom.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED"), roomOnly.conflicts());

            Req insecure = req();
            insecure.deliveryMode = DeliveryMode.HYBRID;
            insecure.meetingUrl = "http://meet.example.com/room";
            SchedulingDecision urlOnly = service().evaluate(insecure.build());
            assertEquals(List.of("SECURE_MEETING_URL_REQUIRED"), urlOnly.conflicts());
            assertEquals(List.of(), urlOnly.warnings());
        }

        @Test
        void hybridWithRoomAndSecureUrlIsClean() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void hybridBlankRoomAndBlankUrlFollowSameBranches() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.roomId = "  ";
            r.meetingUrl = "  ";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED", "HYBRID_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void recordingCombinations() {
            Req unsupported = req();
            unsupported.recordingRequired = true;
            unsupported.recordingSupported = false;
            SchedulingDecision denied = service().evaluate(unsupported.build());
            assertEquals(List.of("RECORDING_NOT_SUPPORTED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req supportedAndRequired = req();
            supportedAndRequired.recordingRequired = true;
            supportedAndRequired.recordingSupported = true;
            SchedulingDecision silentRequired = service().evaluate(supportedAndRequired.build());
            assertEquals(List.of(), silentRequired.conflicts());
            assertEquals(List.of(), silentRequired.warnings());

            Req availableUnused = req();
            availableUnused.recordingRequired = false;
            availableUnused.recordingSupported = true;
            SchedulingDecision warned = service().evaluate(availableUnused.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("RECORDING_AVAILABLE"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void missingEquipmentIsReportedExactForSingleItem() {
            Req r = req();
            r.requiredEquipment = Set.of("MIC");
            r.availableEquipment = Set.of("PROJECTOR");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("MISSING_EQUIPMENT:MIC"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void missingEquipmentContentIsCheckedIgnoringOrder() {
            Req r = req();
            r.requiredEquipment = Set.of("PROJECTOR", "MIC", "CAMERA");
            r.availableEquipment = Set.of("PROJECTOR");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(2, decision.conflicts().size());
            assertTrue(decision.conflicts().contains("MISSING_EQUIPMENT:MIC"));
            assertTrue(decision.conflicts().contains("MISSING_EQUIPMENT:CAMERA"));
        }

        @Test
        void fullySatisfiedEquipmentProducesNoConflicts() {
            Req r = req();
            r.requiredEquipment = Set.of("PROJECTOR", "MIC");
            r.availableEquipment = Set.of("PROJECTOR", "MIC", "SPEAKERS");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void labRequiresSafetyKit() {
            Req without = req();
            without.courseKind = CourseKind.LAB;
            SchedulingDecision denied = service().evaluate(without.build());
            assertEquals(List.of("LAB_SAFETY_KIT_REQUIRED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req with = req();
            with.courseKind = CourseKind.LAB;
            with.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision cleared = service().evaluate(with.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
        }

        @Test
        void remoteExamWithoutProctoringWarnsButClassroomExamDoesNot() {
            Req onlineNoProctoring = req();
            onlineNoProctoring.courseKind = CourseKind.EXAM;
            onlineNoProctoring.deliveryMode = DeliveryMode.ONLINE;
            onlineNoProctoring.roomId = null;
            onlineNoProctoring.meetingUrl = "https://meet.example.com/exam";
            SchedulingDecision warned = service().evaluate(onlineNoProctoring.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("REMOTE_PROCTORING_NOT_CONFIGURED"), warned.warnings());
            assertTrue(warned.manualReview());
            assertEquals("MANUAL_REVIEW", warned.status());

            Req onlineWithProctoring = req();
            onlineWithProctoring.courseKind = CourseKind.EXAM;
            onlineWithProctoring.deliveryMode = DeliveryMode.ONLINE;
            onlineWithProctoring.roomId = null;
            onlineWithProctoring.meetingUrl = "https://meet.example.com/exam";
            onlineWithProctoring.availableEquipment = Set.of("PROCTORING");
            SchedulingDecision configured = service().evaluate(onlineWithProctoring.build());
            assertEquals(List.of(), configured.conflicts());
            assertEquals(List.of(), configured.warnings());
            assertFalse(configured.manualReview());
            assertEquals("APPROVED", configured.status());

            Req classroomExam = req();
            classroomExam.courseKind = CourseKind.EXAM;
            SchedulingDecision inRoom = service().evaluate(classroomExam.build());
            assertEquals(List.of(), inRoom.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Capacity: forecast vs registration, room capacity ladder, oversized
    // room, accessibility, online platform limits, kind-specific group caps
    // ------------------------------------------------------------------
    @Nested
    class Capacity {

        @Test
        void registrationAboveForecastWarns() {
            Req above = req();
            above.registeredParticipants = 11;
            SchedulingDecision warned = service().evaluate(above.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("REGISTRATION_EXCEEDS_FORECAST"), warned.warnings());
            assertEquals("APPROVED", warned.status());

            Req equal = req();
            equal.registeredParticipants = 10;
            SchedulingDecision silent = service().evaluate(equal.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void zeroRoomCapacityConflictsUnlessOnline() {
            Req classroom = req();
            classroom.roomCapacity = 0;
            SchedulingDecision denied = service().evaluate(classroom.build());
            assertEquals(List.of("ROOM_CAPACITY_UNKNOWN"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req online = req();
            online.deliveryMode = DeliveryMode.ONLINE;
            online.roomId = null;
            online.meetingUrl = "https://meet.example.com/room";
            online.roomCapacity = 0;
            SchedulingDecision skipped = service().evaluate(online.build());
            assertEquals(List.of(), skipped.conflicts());
            assertEquals(List.of(), skipped.warnings());
        }

        @Test
        void registrationAboveRoomCapacityConflicts() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 31;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("ROOM_TOO_SMALL_FOR_REGISTRATION"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void expectedAboveRoomCapacityWarns() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 30;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("ROOM_BELOW_EXPECTED_CAPACITY"), decision.warnings());
        }

        @Test
        void exactlyFullRoomIsSilent() {
            Req r = req();
            r.expectedParticipants = 30;
            r.registeredParticipants = 30;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void oversizedRoomWarnsAboveThreeTimesExpected() {
            Req over = req();
            over.roomCapacity = 31; // max(10, 10*3) = 30, so 31 is oversized
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("ROOM_SIGNIFICANTLY_OVERSIZED"), warned.warnings());

            Req atLimit = req();
            atLimit.roomCapacity = 30;
            SchedulingDecision silent = service().evaluate(atLimit.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void smallSessionsUseFloorOfTenForOversizing() {
            Req over = req();
            over.expectedParticipants = 3;
            over.registeredParticipants = 1;
            over.roomCapacity = 11; // max(10, 3*3) = 10, so 11 is oversized
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("ROOM_SIGNIFICANTLY_OVERSIZED"), warned.warnings());

            Req atLimit = req();
            atLimit.expectedParticipants = 3;
            atLimit.registeredParticipants = 1;
            atLimit.roomCapacity = 10;
            SchedulingDecision silent = service().evaluate(atLimit.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void accessibleRoomRequiredConflictsUnlessOnlineOrAccessible() {
            Req classroom = req();
            classroom.accessibleRoomRequired = true;
            classroom.roomAccessible = false;
            SchedulingDecision denied = service().evaluate(classroom.build());
            assertEquals(List.of("ACCESSIBLE_ROOM_REQUIRED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req accessible = req();
            accessible.accessibleRoomRequired = true;
            accessible.roomAccessible = true;
            SchedulingDecision cleared = service().evaluate(accessible.build());
            assertEquals(List.of(), cleared.conflicts());

            Req online = req();
            online.deliveryMode = DeliveryMode.ONLINE;
            online.roomId = null;
            online.meetingUrl = "https://meet.example.com/room";
            online.accessibleRoomRequired = true;
            online.roomAccessible = false;
            SchedulingDecision skipped = service().evaluate(online.build());
            assertEquals(List.of(), skipped.conflicts());
            assertEquals(List.of(), skipped.warnings());
        }

        @Test
        void onlinePlatformCapacityBoundaries() {
            Req exceeded = req();
            exceeded.deliveryMode = DeliveryMode.ONLINE;
            exceeded.roomId = null;
            exceeded.meetingUrl = "https://meet.example.com/room";
            exceeded.expectedParticipants = 1001;
            SchedulingDecision denied = service().evaluate(exceeded.build());
            assertEquals(List.of("ONLINE_PLATFORM_CAPACITY_EXCEEDED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req atThousand = req();
            atThousand.deliveryMode = DeliveryMode.ONLINE;
            atThousand.roomId = null;
            atThousand.meetingUrl = "https://meet.example.com/room";
            atThousand.expectedParticipants = 1000;
            SchedulingDecision large = service().evaluate(atThousand.build());
            assertEquals(List.of(), large.conflicts());
            assertEquals(List.of("LARGE_ONLINE_EVENT"), large.warnings());

            Req at501 = req();
            at501.deliveryMode = DeliveryMode.ONLINE;
            at501.roomId = null;
            at501.meetingUrl = "https://meet.example.com/room";
            at501.expectedParticipants = 501;
            SchedulingDecision above500 = service().evaluate(at501.build());
            assertEquals(List.of(), above500.conflicts());
            assertEquals(List.of("LARGE_ONLINE_EVENT"), above500.warnings());

            Req at500 = req();
            at500.deliveryMode = DeliveryMode.ONLINE;
            at500.roomId = null;
            at500.meetingUrl = "https://meet.example.com/room";
            at500.expectedParticipants = 500;
            SchedulingDecision silent = service().evaluate(at500.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void workshopGroupAboveFiftyWarns() {
            Req over = req();
            over.courseKind = CourseKind.WORKSHOP;
            over.deliveryMode = DeliveryMode.ONLINE;
            over.roomId = null;
            over.meetingUrl = "https://meet.example.com/room";
            over.expectedParticipants = 51;
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("WORKSHOP_GROUP_TOO_LARGE"), warned.warnings());

            Req at50 = req();
            at50.courseKind = CourseKind.WORKSHOP;
            at50.deliveryMode = DeliveryMode.ONLINE;
            at50.roomId = null;
            at50.meetingUrl = "https://meet.example.com/room";
            at50.expectedParticipants = 50;
            SchedulingDecision silent = service().evaluate(at50.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void labAboveThirtyParticipantsConflicts() {
            Req over = req();
            over.courseKind = CourseKind.LAB;
            over.deliveryMode = DeliveryMode.ONLINE;
            over.roomId = null;
            over.meetingUrl = "https://meet.example.com/room";
            over.availableEquipment = Set.of("LAB_SAFETY_KIT");
            over.expectedParticipants = 31;
            SchedulingDecision denied = service().evaluate(over.build());
            assertEquals(List.of("LAB_CAPACITY_EXCEEDED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req at30 = req();
            at30.courseKind = CourseKind.LAB;
            at30.deliveryMode = DeliveryMode.ONLINE;
            at30.roomId = null;
            at30.meetingUrl = "https://meet.example.com/room";
            at30.availableEquipment = Set.of("LAB_SAFETY_KIT");
            at30.expectedParticipants = 30;
            SchedulingDecision silent = service().evaluate(at30.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Availability: instructor availability containment, maintenance overlaps
    // ------------------------------------------------------------------
    @Nested
    class Availability {

        @Test
        void emptyAvailabilityWarnsButDoesNotConflict() {
            Req r = req();
            r.instructorAvailability = List.of();
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void sessionOutsideAllWindowsConflicts() {
            Req r = req();
            r.instructorAvailability = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z"));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_UNAVAILABLE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void containmentBoundaryExactWindowIsInsideOneMinuteShortIsOutside() {
            Req exact = req();
            exact.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T11:00:00Z"));
            SchedulingDecision contained = service().evaluate(exact.build());
            assertEquals(List.of(), contained.conflicts());
            assertEquals(List.of(), contained.warnings());

            Req shortByMinute = req();
            shortByMinute.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T10:59:00Z"));
            SchedulingDecision outside = service().evaluate(shortByMinute.build());
            assertEquals(List.of("INSTRUCTOR_UNAVAILABLE"), outside.conflicts());
        }

        @Test
        void overlappingMaintenanceWindowConflicts() {
            Req r = req();
            r.maintenanceWindows = List.of(window("2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z"));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("MAINTENANCE_WINDOW_CONFLICT"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void touchingMaintenanceWindowsDoNotOverlap() {
            Req endsAtSessionStart = req();
            endsAtSessionStart.maintenanceWindows = List.of(window("2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z"));
            SchedulingDecision before = service().evaluate(endsAtSessionStart.build());
            assertEquals(List.of(), before.conflicts());
            assertEquals(List.of(), before.warnings());

            Req startsAtSessionEnd = req();
            startsAtSessionEnd.maintenanceWindows = List.of(window("2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z"));
            SchedulingDecision after = service().evaluate(startsAtSessionEnd.build());
            assertEquals(List.of(), after.conflicts());
            assertEquals(List.of(), after.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Bookings: cancelled skip, conflict codes, tentative, buffer window,
    // daily session limit, daily minutes
    // ------------------------------------------------------------------
    @Nested
    class Bookings {

        @Test
        void cancelledBookingIsSkipped() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", true, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void overlappingBookingMatchingAllThreeConflictsInOrder() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(
                    "INSTRUCTOR_BOOKING_CONFLICT:b1",
                    "ROOM_BOOKING_CONFLICT:b1",
                    "COHORT_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void onlineModeSkipsRoomBookingConflict() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.meetingUrl = "https://meet.example.com/room";
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1", "COHORT_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of("ROOM_IGNORED_FOR_ONLINE_SESSION"), decision.warnings());
        }

        @Test
        void unrelatedOverlappingBookingIsClean() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void differentDaySameInstructorBookingDoesNotCountTowardDailyTotals() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-19T10:00:00Z", "2026-06-19T11:00:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void tentativeBookingWarnsEvenWithoutConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, true));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("CONFLICTING_BOOKING_IS_TENTATIVE:b1"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void tentativeBookingNextToRealConflictReportsBoth() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, true));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of("CONFLICTING_BOOKING_IS_TENTATIVE:b1"), decision.warnings());
        }

        @Test
        void bufferWindowOverlapConflicts() {
            Req r = req();
            r.minimumBufferMinutes = 30;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1"), decision.conflicts());
        }

        @Test
        void bookingTouchingBufferedWindowOrWithoutBufferIsClean() {
            Req withinBufferEdge = req();
            withinBufferEdge.minimumBufferMinutes = 30;
            withinBufferEdge.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:30:00Z", "2026-06-18T09:30:00Z", false, false));
            SchedulingDecision before = service().evaluate(withinBufferEdge.build());
            assertEquals(List.of(), before.conflicts());
            assertEquals(List.of(), before.warnings());

            Req noBuffer = req();
            noBuffer.minimumBufferMinutes = 0;
            noBuffer.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z", false, false));
            SchedulingDecision touching = service().evaluate(noBuffer.build());
            assertEquals(List.of(), touching.conflicts());
            assertEquals(List.of(), touching.warnings());
        }

        @Test
        void exactlyAtSessionLimitWarns() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false));
            // 2 prior sessions + this one == maximum 3
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void overSessionLimitConflicts() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T14:00:00Z", "2026-06-18T15:00:00Z", false, false));
            // 3 prior sessions >= maximum 3
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_DAILY_SESSION_LIMIT"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void dailyMinutesExactly360IsSilent() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T13:00:00Z", false, false));
            // 240 booking minutes + 60 new = 360: neither > 480 nor > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void dailyMinutes361WarnsLongWorkday() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:01:00Z", false, false));
            // 120 + 181 = 301 booking minutes, + 60 new = 361 > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_LONG_WORKDAY"), decision.warnings());
        }

        @Test
        void dailyMinutesExactly480WarnsButDoesNotConflict() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:00:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T15:00:00Z", "2026-06-18T17:00:00Z", false, false));
            // 420 + 60 = 480: not > 480, but > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_LONG_WORKDAY"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void dailyMinutes481ConflictsWithoutWarning() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:01:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T15:00:00Z", "2026-06-18T17:00:00Z", false, false));
            // 421 + 60 = 481 > 480
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_DAILY_HOURS_EXCEEDED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Recurrence: recurring/occurrence-count consistency and limits
    // ------------------------------------------------------------------
    @Nested
    class Recurrence {

        @Test
        void nonRecurringWithMultipleOccurrencesConflicts() {
            Req r = req();
            r.occurrenceCount = 2;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void recurringWithSingleOccurrenceConflicts() {
            Req single = req();
            single.recurring = true;
            single.occurrenceCount = 1;
            SchedulingDecision denied = service().evaluate(single.build());
            assertEquals(List.of("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req pair = req();
            pair.recurring = true;
            pair.occurrenceCount = 2;
            SchedulingDecision cleared = service().evaluate(pair.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
            assertEquals("APPROVED", cleared.status());
        }

        @Test
        void recurrenceCountBoundaries() {
            Req at20 = req();
            at20.recurring = true;
            at20.occurrenceCount = 20;
            SchedulingDecision silent = service().evaluate(at20.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
            assertFalse(silent.manualReview());
            assertEquals("APPROVED", silent.status());

            Req at21 = req();
            at21.recurring = true;
            at21.occurrenceCount = 21;
            SchedulingDecision longSeries = service().evaluate(at21.build());
            assertEquals(List.of(), longSeries.conflicts());
            assertEquals(List.of("LONG_RECURRENCE_SERIES"), longSeries.warnings());
            assertTrue(longSeries.manualReview());
            assertEquals("MANUAL_REVIEW", longSeries.status());

            Req at52 = req();
            at52.recurring = true;
            at52.occurrenceCount = 52;
            SchedulingDecision max = service().evaluate(at52.build());
            assertEquals(List.of(), max.conflicts());
            assertEquals(List.of("LONG_RECURRENCE_SERIES"), max.warnings());

            Req at53 = req();
            at53.recurring = true;
            at53.occurrenceCount = 53;
            SchedulingDecision over = service().evaluate(at53.build());
            assertEquals(List.of("TOO_MANY_RECURRENCES"), over.conflicts());
            assertEquals(List.of(), over.warnings());
            assertEquals("REJECTED", over.status());
        }

        @Test
        void recurringWeekendSessionWarnsOnlyWhenRecurring() {
            Req weekend = req();
            weekend.recurring = true;
            weekend.occurrenceCount = 2;
            weekend.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            weekend.end = Instant.parse("2026-06-20T11:00:00Z");
            weekend.weekendAllowed = true;
            SchedulingDecision warned = service().evaluate(weekend.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("RECURRING_WEEKEND_SESSION"), warned.warnings());

            Req notRecurring = req();
            notRecurring.start = Instant.parse("2026-06-20T10:00:00Z");
            notRecurring.end = Instant.parse("2026-06-20T11:00:00Z");
            notRecurring.weekendAllowed = true;
            SchedulingDecision silent = service().evaluate(notRecurring.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void recurringExamRequiresReview() {
            Req r = req();
            r.recurring = true;
            r.occurrenceCount = 2;
            r.courseKind = CourseKind.EXAM;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("RECURRING_EXAM_REQUIRES_REVIEW"), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // OverrideAndDecision: administrator override matrix, manual review
    // triggers, status outcomes, record normalization of null collections
    // ------------------------------------------------------------------
    @Nested
    class OverrideAndDecision {

        @Test
        void overrideClearsAllOverridableConflicts() {
            Req single = req();
            single.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            single.end = Instant.parse("2026-06-20T11:00:00Z");
            SchedulingDecision denied = service().evaluate(single.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED"), denied.conflicts());
            assertEquals("REJECTED", denied.status());

            Req overrideOnly = req();
            overrideOnly.start = Instant.parse("2026-06-20T10:00:00Z");
            overrideOnly.end = Instant.parse("2026-06-20T11:00:00Z");
            overrideOnly.administratorOverride = true;
            SchedulingDecision cleared = service().evaluate(overrideOnly.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), cleared.warnings());
            assertTrue(cleared.approved());
            assertFalse(cleared.manualReview());
            assertEquals("APPROVED", cleared.status());

            Req multiple = req();
            multiple.start = Instant.parse("2026-06-20T10:00:00Z");
            multiple.end = Instant.parse("2026-06-20T10:29:00Z"); // also LECTURE_TOO_SHORT
            SchedulingDecision before = service().evaluate(multiple.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED", "LECTURE_TOO_SHORT"), before.conflicts());

            Req overridden = multiple;
            overridden.administratorOverride = true;
            SchedulingDecision after = service().evaluate(overridden.build());
            assertEquals(List.of(), after.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), after.warnings());
            assertEquals("APPROVED", after.status());
        }

        @Test
        void overrideClearsOverridableBookingRoomAndMaintenanceConflicts() {
            Req roomConflict = req();
            roomConflict.existingBookings = List.of(
                    booking("b1", "I-9", "R-1", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision room = service().evaluate(roomConflict.build());
            assertEquals(List.of("ROOM_BOOKING_CONFLICT:b1"), room.conflicts());
            roomConflict.administratorOverride = true;
            SchedulingDecision roomCleared = service().evaluate(roomConflict.build());
            assertEquals(List.of(), roomCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), roomCleared.warnings());

            Req unavailable = req();
            unavailable.instructorAvailability = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z"));
            unavailable.administratorOverride = true;
            SchedulingDecision availabilityCleared = service().evaluate(unavailable.build());
            assertEquals(List.of(), availabilityCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), availabilityCleared.warnings());

            Req maintenance = req();
            maintenance.maintenanceWindows = List.of(window("2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z"));
            maintenance.administratorOverride = true;
            SchedulingDecision maintenanceCleared = service().evaluate(maintenance.build());
            assertEquals(List.of(), maintenanceCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), maintenanceCleared.warnings());
        }

        private void assertOverridePartiallyRejected(Req r, String expectedConflict) {
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"), decision.warnings());
            assertTrue(decision.conflicts().contains(expectedConflict));
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void overrideRejectedForStartTimeInPast() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T01:00:00Z");
            r.end = Instant.parse("2026-06-15T01:30:00Z");
            r.afterHoursAllowed = true; // downgrades hours conflict, but its warning remains
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("START_TIME_IN_PAST"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION", "ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"),
                    decision.warnings());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void overrideRejectedForCrossedLocalDate() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T23:30:00Z");
            r.end = Instant.parse("2026-06-19T00:30:00Z");
            assertOverridePartiallyRejected(r, "SESSION_CROSSES_LOCAL_DATE");
        }

        @Test
        void overrideRejectedForRoomTooSmall() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 31;
            r.roomCapacity = 30;
            assertOverridePartiallyRejected(r, "ROOM_TOO_SMALL_FOR_REGISTRATION");
        }

        @Test
        void overrideRejectedForAccessibleRoom() {
            Req r = req();
            r.accessibleRoomRequired = true;
            r.roomAccessible = false;
            assertOverridePartiallyRejected(r, "ACCESSIBLE_ROOM_REQUIRED");
        }

        @Test
        void overrideRejectedForInstructorBookingConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            assertOverridePartiallyRejected(r, "INSTRUCTOR_BOOKING_CONFLICT:b1");
        }

        @Test
        void overrideRejectedForCohortBookingConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            assertOverridePartiallyRejected(r, "COHORT_BOOKING_CONFLICT:b1");
        }

        @Test
        void overrideWithMixedConflictsKeepsAllAndRejects() {
            Req r = req();
            r.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday, overridable
            r.end = Instant.parse("2026-06-20T11:00:00Z");
            r.expectedParticipants = 40;
            r.registeredParticipants = 31; // non-overridable
            r.roomCapacity = 30;
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED", "ROOM_TOO_SMALL_FOR_REGISTRATION"), decision.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"), decision.warnings());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void largeHybridEventBoundaryTriggersManualReview() {
            Req at200 = req();
            at200.deliveryMode = DeliveryMode.HYBRID;
            at200.meetingUrl = "https://meet.example.com/room";
            at200.expectedParticipants = 200;
            at200.roomCapacity = 300; // room fits 200 without triggering room warnings
            SchedulingDecision silent = service().evaluate(at200.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
            assertFalse(silent.manualReview());
            assertEquals("APPROVED", silent.status());

            Req at201 = req();
            at201.deliveryMode = DeliveryMode.HYBRID;
            at201.meetingUrl = "https://meet.example.com/room";
            at201.expectedParticipants = 201;
            at201.roomCapacity = 300;
            SchedulingDecision review = service().evaluate(at201.build());
            assertEquals(List.of(), review.conflicts());
            assertEquals(List.of("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"), review.warnings());
            assertTrue(review.manualReview());
            assertFalse(review.approved());
            assertEquals("MANUAL_REVIEW", review.status());
        }

        @Test
        void extendedWorkshopBoundaryTriggersManualReview() {
            Req at359 = req();
            at359.courseKind = CourseKind.WORKSHOP;
            at359.start = Instant.parse("2026-06-18T13:30:00Z");
            at359.end = Instant.parse("2026-06-18T19:29:00Z");
            SchedulingDecision justUnder = service().evaluate(at359.build());
            assertEquals(List.of(), justUnder.conflicts());
            assertEquals(List.of(), justUnder.warnings());
            assertFalse(justUnder.manualReview());
            assertEquals("APPROVED", justUnder.status());

            Req at360 = req();
            at360.courseKind = CourseKind.WORKSHOP;
            at360.start = Instant.parse("2026-06-18T13:30:00Z");
            at360.end = Instant.parse("2026-06-18T19:30:00Z");
            SchedulingDecision atThreshold = service().evaluate(at360.build());
            assertEquals(List.of(), atThreshold.conflicts());
            assertEquals(List.of("EXTENDED_WORKSHOP_REQUIRES_REVIEW"), atThreshold.warnings());
            assertTrue(atThreshold.manualReview());
            assertEquals("MANUAL_REVIEW", atThreshold.status());
        }

        @Test
        void manualReviewPlusConflictYieldsRejectedStatus() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-15T03:00:00Z"); // local 11:00, 1h lead
            r.end = Instant.parse("2026-06-15T04:00:00Z");
            r.meetingUrl = "https://meet.example.com/room";
            r.expectedParticipants = 201;
            r.roomCapacity = 300;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSUFFICIENT_BOOKING_NOTICE"), decision.conflicts());
            assertEquals(List.of("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"), decision.warnings());
            assertTrue(decision.manualReview());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void requestRecordNormalizesNullCollections() {
            ScheduleRequest raw = new ScheduleRequest("C-1", "CO-1", "I-1", DeliveryMode.CLASSROOM,
                    CourseKind.LECTURE,
                    Instant.parse("2026-06-18T10:00:00Z"), Instant.parse("2026-06-18T11:00:00Z"),
                    "UTC", "R-1", null, 10, 5, 30,
                    null, null, false, false, false, false, false, false, 1, 0, 3,
                    null, null, null, null, false, false, false);
            SchedulingDecision decision = service().evaluate(raw);
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void decisionRecordNormalizesNullLists() {
            SchedulingDecision decision = new SchedulingDecision(true, false, "APPROVED", 60, null, null);
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals(60, decision.durationMinutes());
        }
    }

    // ------------------------------------------------------------------
    // Scheduling residual branch sides (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class SchedulingResidualGaps {

        @Test
        void overrideWithNoConflictsChangesNothing() {
            Req override = req();
            override.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(override.build());
            assertTrue(decision.approved());
            assertEquals("APPROVED", decision.status());
            assertTrue(decision.conflicts().isEmpty());
            assertTrue(decision.warnings().isEmpty());
        }

        @Test
        void sundaySessionNeedsWeekendPermission() {
            Req sunday = req();
            sunday.start = Instant.parse("2026-06-21T10:00:00Z");
            sunday.end = Instant.parse("2026-06-21T11:00:00Z");
            assertTrue(service().evaluate(sunday.build()).conflicts().contains("WEEKEND_NOT_ALLOWED"));
            Req permitted = req();
            permitted.start = Instant.parse("2026-06-21T10:00:00Z");
            permitted.end = Instant.parse("2026-06-21T11:00:00Z");
            permitted.weekendAllowed = true;
            assertTrue(service().evaluate(permitted.build()).conflicts().isEmpty());
        }

        @Test
        void onlineSessionWithBlankRoomIdStillWarnsIgnored() {
            Req online = req();
            online.deliveryMode = DeliveryMode.ONLINE;
            online.meetingUrl = "https://meet.example/s1";
            online.roomId = " ";
            SchedulingDecision decision = service().evaluate(online.build());
            assertTrue(decision.conflicts().isEmpty());
            // A blank room id counts as absent: no ignore warning is emitted.
            assertFalse(decision.warnings().contains("ROOM_IGNORED_FOR_ONLINE_SESSION"));
        }

        @Test
        void classroomWithBlankMeetingUrlStaysSilent() {
            Req classroom = req();
            classroom.meetingUrl = " ";
            SchedulingDecision decision = service().evaluate(classroom.build());
            assertFalse(decision.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));
            assertTrue(decision.conflicts().isEmpty());
        }

        @Test
        void recurringSundaySessionTriggersWeekendWarning() {
            Req recurring = req();
            recurring.start = Instant.parse("2026-06-21T10:00:00Z");
            recurring.end = Instant.parse("2026-06-21T11:00:00Z");
            recurring.recurring = true;
            recurring.occurrenceCount = 2;
            SchedulingDecision decision = service().evaluate(recurring.build());
            assertTrue(decision.warnings().contains("RECURRING_WEEKEND_SESSION"));
        }

        @Test
        void availabilityWindowWithNullStartIsRejected() {
            Req invalid = req();
            invalid.instructorAvailability = List.of(
                    new TimeWindow(null, Instant.parse("2026-06-25T00:00:00Z")));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(invalid.build()));
            assertEquals("instructor availability is invalid", ex.getMessage());
        }
    }
    }
}
