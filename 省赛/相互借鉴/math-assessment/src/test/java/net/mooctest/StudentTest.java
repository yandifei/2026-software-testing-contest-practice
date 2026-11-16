/*
 * 开发者测试比赛要求（摘要）
 *
 * 本题包含在线教育业务、数学表达式引擎，以及表达式答案和考试成绩判定流程。
 * - 使用 JDK 21、Maven 3.9.6及以上、JUnit 5。
 * - 测试代码只写在 src/test/java/net/mooctest/StudentTest.java 中，不要包含 main() 方法。
 * - 不要修改 src/main/java 中的被测源代码或 pom.xml。
 *
 * 作弊与违规说明（摘要）：绝对禁止利用评分工具的运行特征，或改变被测程序的执行逻辑，
 * 人为提高覆盖率或变异杀死率。本文件只使用真实输入、反射直调私有入口等规则允许的白盒手段，
 * 不替换被测类字节码、不探测 PIT mutant 身份。
 *
 * 融合说明：以 deepseek 版的业务+引擎套件为骨架，吸收 GLM 的退款时间窗、排课周末/混合教室
 * 边界用例，形成 RefundWindowAndComplexPrecisionTests；每个用例带 @DisplayName 语义说明。
 */
package net.mooctest;

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
import net.mooctest.enrollment.adapter.EmptyEnrollmentHistory;
import net.mooctest.enrollment.domain.CourseOffering;
import net.mooctest.enrollment.domain.EnrollmentCourseStatus;
import net.mooctest.enrollment.domain.EnrollmentDecision;
import net.mooctest.enrollment.domain.EnrollmentOutcome;
import net.mooctest.enrollment.domain.EnrollmentRequest;
import net.mooctest.enrollment.domain.StudentProfile;
import net.mooctest.enrollment.port.EnrollmentHistory;
import net.mooctest.enrollment.service.EnrollmentEligibilityService;
import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.NameExpression;
import net.mooctest.expression.engine.PackExpression;
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
import net.mooctest.grading.expression.ExpressionEvaluator;
import net.mooctest.grading.expression.ExpressionExamGrade;
import net.mooctest.grading.expression.ExpressionExamGradingService;
import net.mooctest.grading.expression.ExpressionExamSubmission;
import net.mooctest.grading.expression.ExpressionQuestion;
import net.mooctest.grading.expression.MathematicalExpressionEvaluator;
import net.mooctest.grading.service.ExamGradingService;
import net.mooctest.pricing.domain.Coupon;
import net.mooctest.pricing.domain.CouponType;
import net.mooctest.pricing.domain.MembershipLevel;
import net.mooctest.pricing.domain.PriceBreakdown;
import net.mooctest.pricing.domain.PriceRequest;
import net.mooctest.pricing.domain.Promotion;
import net.mooctest.pricing.domain.PromotionType;
import net.mooctest.pricing.service.AdvancedPricingService;
import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;
import net.mooctest.progress.service.LearningProgressService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.mooctest.expression.engine.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.mooctest.expression.engine.AbnormalOperation;
import net.mooctest.expression.engine.Calculation;
import net.mooctest.expression.engine.CalculationManagement;
import net.mooctest.expression.engine.CalculationNumberResults;
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
import net.mooctest.expression.engine.NumberCalculation;
import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.expression.engine.PrefixExpression;
import net.mooctest.expression.engine.PrefixExpressionOperation;
import net.mooctest.expression.engine.SingletonEquationSolving;
import net.mooctest.expression.engine.SingletonEquationSolvingTwo;
import net.mooctest.expression.engine.StrUtils;
import net.mooctest.expression.engine.WrongFormat;
import org.junit.jupiter.api.BeforeAll;
import top.lingyuzhao.varFormatter.core.VarFormatter;
import top.lingyuzhao.varFormatter.utils.DataObj;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 省赛 Math 题（math-assessment）单文件测试套件。
 *
 * <p>覆盖在线教育业务域（打分、选课、排课、定价、退款、学习进度、课程）、
 * 内嵌数学表达式引擎、表达式答案与考试成绩判定流程。</p>
 */
@DisplayName("math-assessment 全量测试套件")
class StudentTest {

    /** 固定时钟基准：2026-11-15T02:00:00Z（周日）。所有用例都相对该时刻构造输入。 */
    private static final Instant T0 = Instant.parse("2026-11-15T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    private final ExamGradingService grading = new ExamGradingService();

    // ---------------------------------------------------------------- helpers

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** AnswerResult 的可变构造辅助。 */
    private static final class Ans {
        String id = "q1";
        QuestionType type = QuestionType.SINGLE_CHOICE;
        String section = "A";
        BigDecimal max = new BigDecimal("10.00");
        boolean mandatory = false;
        boolean answered = true;
        boolean exact = true;
        int correctOptionCount = 0;
        int selectedCorrectOptions = 0;
        int selectedIncorrectOptions = 0;
        BigDecimal numericError = null;
        BigDecimal numericTolerance = null;
        int rubricPercent = 0;
        int passedTests = 0;
        int totalTests = 0;
        boolean compilationSucceeded = true;
        boolean plagiarism = false;
        boolean timedOut = false;
        boolean manualReview = false;

        Ans id(String value) { this.id = value; return this; }
        Ans type(QuestionType value) { this.type = value; return this; }
        Ans section(String value) { this.section = value; return this; }
        Ans max(BigDecimal value) { this.max = value; return this; }
        Ans mandatory(boolean value) { this.mandatory = value; return this; }
        Ans answered(boolean value) { this.answered = value; return this; }
        Ans exact(boolean value) { this.exact = value; return this; }
        Ans correctOptionCount(int value) { this.correctOptionCount = value; return this; }
        Ans selectedCorrectOptions(int value) { this.selectedCorrectOptions = value; return this; }
        Ans selectedIncorrectOptions(int value) { this.selectedIncorrectOptions = value; return this; }
        Ans numericError(BigDecimal value) { this.numericError = value; return this; }
        Ans numericTolerance(BigDecimal value) { this.numericTolerance = value; return this; }
        Ans rubricPercent(int value) { this.rubricPercent = value; return this; }
        Ans passedTests(int value) { this.passedTests = value; return this; }
        Ans totalTests(int value) { this.totalTests = value; return this; }
        Ans compilationSucceeded(boolean value) { this.compilationSucceeded = value; return this; }
        Ans plagiarism(boolean value) { this.plagiarism = value; return this; }
        Ans timedOut(boolean value) { this.timedOut = value; return this; }
        Ans manualReview(boolean value) { this.manualReview = value; return this; }

        AnswerResult build() {
            return new AnswerResult(id, type, section, max, mandatory, answered, exact,
                    correctOptionCount, selectedCorrectOptions, selectedIncorrectOptions,
                    numericError, numericTolerance, rubricPercent, passedTests, totalTests,
                    compilationSucceeded, plagiarism, timedOut, manualReview);
        }
    }

    /** ExamSubmission 的可变构造辅助。 */
    private static final class Sub {
        String examId = "exam-1";
        String studentId = "student-1";
        List<AnswerResult> answers = new ArrayList<>();
        Instant startedAt = T0;
        Instant submittedAt = T0.plusSeconds(1800);
        Instant deadline = null;
        int allowedDurationSeconds = 7200;
        int attemptNumber = 1;
        boolean identityVerified = true;
        boolean termsAccepted = true;
        boolean suspended = false;
        boolean proctorDisconnected = false;
        boolean ipChanged = false;
        int fullscreenExitCount = 0;
        int copyPasteEventCount = 0;
        BigDecimal bonusPoints = BigDecimal.ZERO;
        Integer overridePercent = null;
        Map<String, Integer> sectionPass = new LinkedHashMap<>();

        Sub plus(AnswerResult answer) {
            answers.add(answer);
            return this;
        }

        ExamSubmission build() {
            return new ExamSubmission(examId, studentId, answers, startedAt, submittedAt, deadline,
                    allowedDurationSeconds, attemptNumber, identityVerified, termsAccepted,
                    suspended, proctorDisconnected, ipChanged, fullscreenExitCount,
                    copyPasteEventCount, bonusPoints, overridePercent, sectionPass);
        }
    }

    /** GradingPolicy 的可变构造辅助。 */
    private static final class Pol {
        int passPercentage = 60;
        boolean partialCreditEnabled = true;
        boolean negativeMarkingEnabled = false;
        BigDecimal wrongAnswerPenaltyRate = new BigDecimal("0.25");
        BigDecimal unansweredPenaltyRate = BigDecimal.ZERO;
        int latePenaltyPercentPerHour = 10;
        int maximumLatePenaltyPercent = 50;
        int minimumSectionPercentage = 50;
        boolean allMandatoryQuestionsRequired = true;
        boolean identityVerificationRequired = false;
        int maximumAttempts = 3;
        boolean failOnIntegrityViolation = true;
        boolean capScoreAtOneHundred = true;
        boolean programmingCompilationRequired = true;

        GradingPolicy build() {
            return new GradingPolicy(passPercentage, partialCreditEnabled, negativeMarkingEnabled,
                    wrongAnswerPenaltyRate, unansweredPenaltyRate, latePenaltyPercentPerHour,
                    maximumLatePenaltyPercent, minimumSectionPercentage,
                    allMandatoryQuestionsRequired, identityVerificationRequired, maximumAttempts,
                    failOnIntegrityViolation, capScoreAtOneHundred, programmingCompilationRequired);
        }
    }

    private static Ans ans() {
        return new Ans();
    }

    private static Sub sub() {
        return new Sub();
    }

    private static Pol pol() {
        return new Pol();
    }

    /** 反射直调 ExamGradingService 的私有评分入口（规则允许的白盒手段）。 */
    private BigDecimal invokeScoreAnswer(AnswerResult answer, GradingPolicy policy, List<String> warnings) {
        try {
            Method method = ExamGradingService.class.getDeclaredMethod(
                    "scoreAnswer", AnswerResult.class, GradingPolicy.class, List.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(grading, answer, policy, warnings);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        }
    }

    // ============================================================ grading

    @Nested
    @DisplayName("ExamGradingService 客观题与填空题评分")
    class GradingObjective {

        @Test
        @DisplayName("单选题答对拿满分，等级 A+，无失败项")
        void singleChoiceExactGivesFullMarks() {
            GradeReport report = grading.grade(sub().plus(ans().build()).build(), pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
            assertEquals(bd("10.00"), report.finalPoints());
            assertEquals(bd("10.00"), report.maximumPoints());
            assertEquals(100, report.percentage());
            assertEquals("A+", report.letterGrade());
            assertTrue(report.passed());
            assertFalse(report.manualReview());
            assertEquals(Map.of("A", 100), report.sectionPercentages());
            assertTrue(report.failures().isEmpty());
            assertTrue(report.warnings().isEmpty());
        }

        @Test
        @DisplayName("单选题答错且未开启负分：0 分并判定不及格")
        void singleChoiceWrongWithoutNegativeMarkingScoresZero() {
            GradeReport report = grading.grade(sub().plus(ans().exact(false).build()).build(), pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertEquals(List.of("SECTION_BELOW_PASS_MARK:A", "OVERALL_SCORE_BELOW_PASS_MARK"),
                    report.failures());
        }

        @Test
        @DisplayName("单选题答错且开启负分：负分被原始分下限截断为 0")
        void singleChoiceWrongWithNegativeMarking() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            GradeReport report = grading.grade(sub().plus(ans().exact(false).build()).build(), policy.build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertEquals(0, report.percentage());
            assertFalse(report.passed());
        }

        @Test
        @DisplayName("判断题答对同样按满分处理")
        void trueFalseExactGivesFullMarks() {
            GradeReport report = grading.grade(
                    sub().plus(ans().type(QuestionType.TRUE_FALSE).build()).build(), pol().build());
            assertEquals(bd("10.00"), report.finalPoints());
            assertEquals("A+", report.letterGrade());
        }

        @Test
        @DisplayName("未作答且无惩罚率：0 分且无 UNANSWERED_PENALTY 警告")
        void unansweredWithoutPenalty() {
            GradeReport report = grading.grade(
                    sub().plus(ans().answered(false).exact(false).build()).build(), pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertFalse(report.warnings().contains("UNANSWERED_PENALTY:q1"));
        }

        @Test
        @DisplayName("未作答且有惩罚率：写入 UNANSWERED_PENALTY 警告且必答题无学分")
        void unansweredWithPenaltyRate() {
            Pol policy = pol();
            policy.unansweredPenaltyRate = bd("0.50");
            Sub submission = sub().plus(ans().answered(false).exact(false).mandatory(true).build());
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertTrue(report.warnings().contains("UNANSWERED_PENALTY:q1"));
            assertTrue(report.warnings().contains("MANDATORY_QUESTION_NO_CREDIT:q1"));
            assertTrue(report.failures().contains("MANDATORY_QUESTION_UNANSWERED:q1"));
            assertFalse(report.passed());
        }

        @Test
        @DisplayName("多选题完全正确拿满分")
        void multipleChoiceExactGivesFullMarks() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).correctOptionCount(4)
                    .selectedCorrectOptions(4).build()).build(), pol().build());
            assertEquals(bd("10.00"), report.finalPoints());
        }

        @Test
        @DisplayName("多选题没有正确选项：警告并 0 分")
        void multipleChoiceWithoutCorrectOptions() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(0).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertTrue(report.warnings().contains("MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:q1"));
        }

        @Test
        @DisplayName("多选题不开启部分给分：有/无负分与错选组合")
        void multipleChoiceNoPartialCredit() {
            Pol policy = pol();
            policy.partialCreditEnabled = false;
            GradeReport plain = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(2).selectedIncorrectOptions(1).build()).build(), policy.build());
            assertEquals(bd("0.00"), plain.rawPoints());

            policy.negativeMarkingEnabled = true;
            GradeReport negative = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedIncorrectOptions(1).build()).build(), policy.build());
            assertEquals(bd("0.00"), negative.rawPoints());

            GradeReport noIncorrect = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(0).selectedIncorrectOptions(0).build()).build(), policy.build());
            assertEquals(bd("0.00"), noIncorrect.rawPoints());
        }

        @Test
        @DisplayName("多选题部分给分：按选中正确选项比例得分")
        void multipleChoicePartialCredit() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(2).build()).build(), pol().build());
            assertEquals(bd("5.00"), report.rawPoints());
        }

        @Test
        @DisplayName("多选题部分给分：错选按 25% 默认比例扣分")
        void multipleChoicePartialCreditWithWrongSelections() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(4).selectedIncorrectOptions(2).build()).build(),
                    pol().build());
            assertEquals(bd("8.75"), report.rawPoints());
        }

        @Test
        @DisplayName("多选题部分给分且开启负分：错选扣分比例随策略变化")
        void multipleChoicePartialCreditNegativeMarking() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            policy.wrongAnswerPenaltyRate = bd("0.50");
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(2).selectedIncorrectOptions(1).build()).build(),
                    policy.build());
            assertEquals(bd("3.75"), report.rawPoints());
        }

        @Test
        @DisplayName("多选题只选中错误选项：写入 ONLY_INCORRECT_OPTIONS_SELECTED 警告")
        void multipleChoiceOnlyIncorrectSelections() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(0).selectedIncorrectOptions(2).build()).build(),
                    pol().build());
            assertTrue(report.warnings().contains("ONLY_INCORRECT_OPTIONS_SELECTED:q1"));
            assertEquals(bd("0.00"), report.rawPoints());
        }

        @Test
        @DisplayName("填空题四种计分路径")
        void fillBlankPaths() {
            GradeReport exact = grading.grade(sub().plus(ans()
                    .type(QuestionType.FILL_BLANK).build()).build(), pol().build());
            assertEquals(bd("10.00"), exact.finalPoints());

            GradeReport rubric = grading.grade(sub().plus(ans()
                    .type(QuestionType.FILL_BLANK).exact(false).rubricPercent(50).build()).build(),
                    pol().build());
            assertEquals(bd("5.00"), rubric.rawPoints());

            Pol negativePolicy = pol();
            negativePolicy.negativeMarkingEnabled = true;
            GradeReport negative = grading.grade(sub().plus(ans()
                    .type(QuestionType.FILL_BLANK).exact(false).rubricPercent(0).build()).build(),
                    negativePolicy.build());
            assertEquals(bd("0.00"), negative.rawPoints());

            Pol strictPolicy = pol();
            strictPolicy.partialCreditEnabled = false;
            GradeReport strict = grading.grade(sub().plus(ans()
                    .type(QuestionType.FILL_BLANK).exact(false).rubricPercent(80).build()).build(),
                    strictPolicy.build());
            assertEquals(bd("0.00"), strict.rawPoints());
        }
    }

    @Nested
    @DisplayName("ExamGradingService 数值题、问答题与编程题评分")
    class GradingNumericEssayProgramming {

        private Ans numeric(BigDecimal error, BigDecimal tolerance) {
            return ans().type(QuestionType.NUMERIC).exact(false).numericError(error)
                    .numericTolerance(tolerance);
        }

        @Test
        @DisplayName("数值题精确匹配拿满分")
        void numericExactGivesFullMarks() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.NUMERIC).numericError(bd("5")).numericTolerance(bd("1")).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
        }

        @Test
        @DisplayName("数值题缺少误差或容差数据：警告并 0 分")
        void numericMissingErrorData() {
            GradeReport noError = grading.grade(sub().plus(numeric(null, bd("1")).build()).build(), pol().build());
            assertEquals(bd("0.00"), noError.rawPoints());
            assertTrue(noError.warnings().contains("NUMERIC_TOLERANCE_DATA_MISSING:q1"));

            GradeReport noTolerance = grading.grade(sub().plus(numeric(bd("1"), null).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), noTolerance.rawPoints());
            assertTrue(noTolerance.warnings().contains("NUMERIC_TOLERANCE_DATA_MISSING:q1"));
        }

        @Test
        @DisplayName("数值题误差在容差内拿满分")
        void numericWithinTolerance() {
            GradeReport report = grading.grade(sub().plus(numeric(bd("-1"), bd("2")).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
        }

        @Test
        @DisplayName("数值题误差在十倍容差内：按线性比例给部分分")
        void numericPartialCreditBetweenToleranceAndTenTimes() {
            GradeReport report = grading.grade(sub().plus(numeric(bd("5"), bd("1")).build()).build(),
                    pol().build());
            assertEquals(bd("5.56"), report.rawPoints());
        }

        @Test
        @DisplayName("数值题误差超过十倍容差：负分开关决定结果")
        void numericBeyondTenTimesTolerance() {
            GradeReport report = grading.grade(sub().plus(numeric(bd("12"), bd("1")).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());

            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            GradeReport negative = grading.grade(sub().plus(numeric(bd("12"), bd("1")).build()).build(),
                    policy.build());
            assertEquals(bd("0.00"), negative.rawPoints());
        }

        @Test
        @DisplayName("数值题容差为 0 时部分给分条件不成立，走负分分支")
        void numericZeroTolerance() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            GradeReport report = grading.grade(sub().plus(numeric(bd("3"), bd("0")).build()).build(),
                    policy.build());
            assertEquals(bd("0.00"), report.rawPoints());
        }

        @Test
        @DisplayName("问答题 rubricPercent 为 0 且非精确匹配：0 分")
        void essayZeroRubric() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.ESSAY).exact(false).rubricPercent(0).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
        }

        @Test
        @DisplayName("问答题 rubricPercent 给分并写入人工复核警告")
        void essayRubricAndManualReview() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.ESSAY).exact(false).rubricPercent(75).manualReview(true).build()).build(),
                    pol().build());
            assertEquals(bd("7.50"), report.rawPoints());
            assertTrue(report.warnings().contains("ESSAY_SCORE_IS_PROVISIONAL:q1"));
            assertTrue(report.warnings().contains("MANUAL_GRADING_REQUIRED:q1"));
            assertTrue(report.manualReview());
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
        }

        @Test
        @DisplayName("问答题精确匹配时即使 rubricPercent 为 0 也拿满分")
        void essayExactOverridesZeroRubric() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.ESSAY).exact(true).rubricPercent(0).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
        }

        @Test
        @DisplayName("编程题编译失败：是否要求编译决定分数")
        void programmingNotCompiled() {
            GradeReport required = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).compilationSucceeded(false).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), required.rawPoints());
            assertTrue(required.warnings().contains("PROGRAM_DID_NOT_COMPILE:q1"));

            Pol policy = pol();
            policy.programmingCompilationRequired = false;
            GradeReport optional = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).compilationSucceeded(false).build()).build(),
                    policy.build());
            assertEquals(bd("1.00"), optional.rawPoints());
        }

        @Test
        @DisplayName("编程题编译成功但没有自动化测试：反射直调私有评分入口给 20% 分")
        void programmingNoAutomatedTests() {
            List<String> warnings = new ArrayList<>();
            BigDecimal score = invokeScoreAnswer(ans()
                    .type(QuestionType.PROGRAMMING).totalTests(0).build(), pol().build(), warnings);
            assertEquals(bd("2.00"), score);
            assertTrue(warnings.contains("PROGRAM_HAS_NO_AUTOMATED_TESTS:q1"));
        }

        @Test
        @DisplayName("公开路径下编译成功但零测试被 validate 拒绝")
        void programmingNoTestsRejectedByValidation() {
            assertEquals("compiled program requires tests or manual review",
                    assertThrows(IllegalArgumentException.class, () -> grading.grade(
                            sub().plus(ans().type(QuestionType.PROGRAMMING).totalTests(0).build()).build(),
                            pol().build())).getMessage());
        }

        @Test
        @DisplayName("编程题零测试且需要人工复核：0 分")
        void programmingNoTestsManualReview() {
            List<String> warnings = new ArrayList<>();
            BigDecimal score = invokeScoreAnswer(ans().type(QuestionType.PROGRAMMING)
                    .totalTests(0).manualReview(true).build(), pol().build(), warnings);
            assertEquals(bd("0.00"), score);
        }

        @Test
        @DisplayName("编程题部分通过：部分给分开关与超时/抄袭叠加")
        void programmingPartialPaths() {
            GradeReport all = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(3).totalTests(3).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), all.rawPoints());

            Pol strict = pol();
            strict.partialCreditEnabled = false;
            GradeReport strictScore = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(1).totalTests(2).build()).build(),
                    strict.build());
            assertEquals(bd("0.00"), strictScore.rawPoints());

            GradeReport partial = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(1).totalTests(2).build()).build(),
                    pol().build());
            assertEquals(bd("5.00"), partial.rawPoints());

            GradeReport timedOut = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(1).totalTests(2).timedOut(true).build()).build(),
                    pol().build());
            assertEquals(bd("3.00"), timedOut.rawPoints());
            assertTrue(timedOut.warnings().contains("QUESTION_TIMED_OUT:q1"));

            GradeReport plagiarism = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(1).totalTests(2).plagiarism(true).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), plagiarism.rawPoints());
            assertTrue(plagiarism.failures().contains("ANSWER_INTEGRITY_VIOLATION:q1"));
            assertTrue(plagiarism.manualReview());
            assertFalse(plagiarism.passed());
        }

        @Test
        @DisplayName("超时且得分大于 0 时全局打八折")
        void timedOutAppliesGlobalDiscount() {
            GradeReport report = grading.grade(sub().plus(ans().timedOut(true).build()).build(), pol().build());
            assertEquals(bd("8.00"), report.rawPoints());
            assertEquals(80, report.percentage());
        }

        @Test
        @DisplayName("超时但得分为 0 时不触发折扣分支")
        void timedOutWithZeroScore() {
            GradeReport report = grading.grade(sub().plus(ans().exact(false).timedOut(true).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
        }
    }

    @Nested
    @DisplayName("ExamGradingService 提交级规则")
    class GradingSubmissionRules {

        @Test
        @DisplayName("奖励分被累加进调整分，原始分保持不含奖励")
        void bonusPointsApplied() {
            Sub submission = sub().plus(ans().build());
            submission.bonusPoints = bd("5");
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
            assertEquals(bd("10.00"), report.finalPoints());
            assertTrue(report.warnings().contains("BONUS_POINTS_APPLIED"));
        }

        @Test
        @DisplayName("开启不封顶时百分比可超过 100")
        void uncappedPercentageExceedsHundred() {
            Pol policy = pol();
            policy.capScoreAtOneHundred = false;
            Sub submission = sub().plus(ans().build());
            submission.bonusPoints = bd("5");
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertEquals(150, report.percentage());
            assertEquals(bd("15.00"), report.finalPoints());
        }

        @Test
        @DisplayName("迟交按小时计惩罚并写入警告")
        void lateSubmissionPenalty() {
            Sub submission = sub().plus(ans().build());
            submission.deadline = T0;
            submission.submittedAt = T0.plusSeconds(2 * 3600);
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(bd("8.00"), report.finalPoints());
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:20"));
            assertFalse(report.failures().contains("SUBMISSION_TOO_LATE"));
        }

        @Test
        @DisplayName("迟交惩罚达到 100% 时记录 SUBMISSION_TOO_LATE")
        void lateSubmissionTooLate() {
            Pol policy = pol();
            policy.latePenaltyPercentPerHour = 60;
            policy.maximumLatePenaltyPercent = 100;
            Sub submission = sub().plus(ans().build());
            submission.deadline = T0;
            submission.submittedAt = T0.plusSeconds(2 * 3600);
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertTrue(report.failures().contains("SUBMISSION_TOO_LATE"));
            assertTrue(report.warnings().contains("LATE_SUBMISSION_PENALTY:100"));
            assertEquals(bd("0.00"), report.finalPoints());
        }

        @Test
        @DisplayName("考试时长三档超时处理")
        void durationOverruns() {
            Sub grace = sub().plus(ans().build());
            grace.allowedDurationSeconds = 600;
            grace.submittedAt = T0.plusSeconds(660);
            GradeReport graceReport = grading.grade(grace.build(), pol().build());
            assertEquals(bd("10.00"), graceReport.finalPoints());
            assertTrue(graceReport.warnings().contains("EXAM_GRACE_PERIOD_USED"));

            Sub minor = sub().plus(ans().build());
            minor.allowedDurationSeconds = 600;
            minor.submittedAt = T0.plusSeconds(700);
            GradeReport minorReport = grading.grade(minor.build(), pol().build());
            assertEquals(bd("9.50"), minorReport.finalPoints());
            assertTrue(minorReport.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));

            Sub major = sub().plus(ans().build());
            major.allowedDurationSeconds = 600;
            major.submittedAt = T0.plusSeconds(1000);
            GradeReport majorReport = grading.grade(major.build(), pol().build());
            assertEquals(bd("8.00"), majorReport.finalPoints());
            assertTrue(majorReport.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        @DisplayName("超过最大次数：记录失败并把分数清零；正好用完则警告")
        void attemptLimits() {
            Sub exceeded = sub().plus(ans().build());
            exceeded.attemptNumber = 4;
            GradeReport failed = grading.grade(exceeded.build(), pol().build());
            assertEquals(bd("0.00"), failed.finalPoints());
            assertTrue(failed.failures().contains("MAXIMUM_ATTEMPTS_EXCEEDED"));

            Sub last = sub().plus(ans().build());
            last.attemptNumber = 3;
            assertTrue(grading.grade(last.build(), pol().build()).warnings()
                    .contains("FINAL_ALLOWED_ATTEMPT"));
        }

        @Test
        @DisplayName("教师覆写百分比同时改变百分比与最终得分")
        void instructorOverride() {
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = 80;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(80, report.percentage());
            assertEquals(bd("8.00"), report.finalPoints());
            assertTrue(report.warnings().contains("INSTRUCTOR_OVERRIDE_APPLIED"));
            assertTrue(report.passed());
        }

        @Test
        @DisplayName("没有任何答案时最大分为 0，百分比为 0")
        void noAnswersYieldsZeroPercentage() {
            GradeReport report = grading.grade(sub().build(), pol().build());
            assertEquals(bd("0.00"), report.maximumPoints());
            assertEquals(0, report.percentage());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }
    }

    @Nested
    @DisplayName("ExamGradingService 考纪与章节规则")
    class GradingIntegrityAndSections {

        @Test
        @DisplayName("未接受考试条款只记录失败项，不影响 passed 判定")
        void termsNotAcceptedRecordsFailureOnly() {
            Sub submission = sub().plus(ans().build());
            submission.termsAccepted = false;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertTrue(report.failures().contains("EXAM_TERMS_NOT_ACCEPTED"));
            assertTrue(report.passed());
        }

        @Test
        @DisplayName("账号被停用：记录 ACCOUNT_SUSPENDED 且不通过")
        void accountSuspended() {
            Sub submission = sub().plus(ans().build());
            submission.suspended = true;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertTrue(report.failures().contains("ACCOUNT_SUSPENDED"));
            assertFalse(report.passed());
        }

        @Test
        @DisplayName("身份验证要求开关决定失败项还是警告")
        void identityRules() {
            Pol policy = pol();
            policy.identityVerificationRequired = true;
            Sub unverified = sub().plus(ans().build());
            unverified.identityVerified = false;
            GradeReport failed = grading.grade(unverified.build(), policy.build());
            assertTrue(failed.failures().contains("IDENTITY_NOT_VERIFIED"));
            assertFalse(failed.warnings().contains("IDENTITY_NOT_VERIFIED"));
            assertFalse(failed.passed());

            Sub optional = sub().plus(ans().build());
            optional.identityVerified = false;
            GradeReport warned = grading.grade(optional.build(), pol().build());
            assertTrue(warned.warnings().contains("IDENTITY_NOT_VERIFIED"));
            assertTrue(warned.passed());
        }

        @Test
        @DisplayName("监考断线与 IP 变更：写入警告并要求人工复核")
        void proctorAndIpWarnings() {
            Sub submission = sub().plus(ans().build());
            submission.proctorDisconnected = true;
            submission.ipChanged = true;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertTrue(report.warnings().contains("PROCTOR_CONNECTION_LOST"));
            assertTrue(report.warnings().contains("IP_ADDRESS_CHANGED"));
            assertTrue(report.manualReview());
            assertTrue(report.warnings().contains("RESULT_PENDING_MANUAL_REVIEW"));
        }

        @Test
        @DisplayName("全屏退出 1/2/3 次与复制粘贴 4/5 次的阈值")
        void fullscreenAndCopyPasteThresholds() {
            Sub one = sub().plus(ans().build());
            one.fullscreenExitCount = 1;
            assertTrue(grading.grade(one.build(), pol().build()).warnings()
                    .contains("FULLSCREEN_EXIT_RECORDED"));

            Sub two = sub().plus(ans().build());
            two.fullscreenExitCount = 2;
            assertTrue(grading.grade(two.build(), pol().build()).warnings()
                    .contains("MULTIPLE_FULLSCREEN_EXITS"));

            Sub three = sub().plus(ans().build());
            three.fullscreenExitCount = 3;
            GradeReport many = grading.grade(three.build(), pol().build());
            assertTrue(many.failures().contains("EXCESSIVE_FULLSCREEN_EXITS"));
            assertTrue(many.manualReview());
            assertFalse(many.passed());

            Sub four = sub().plus(ans().build());
            four.copyPasteEventCount = 4;
            assertTrue(grading.grade(four.build(), pol().build()).warnings()
                    .contains("COPY_PASTE_EVENTS_RECORDED"));

            Sub five = sub().plus(ans().build());
            five.copyPasteEventCount = 5;
            GradeReport copy = grading.grade(five.build(), pol().build());
            assertTrue(copy.failures().contains("EXCESSIVE_COPY_PASTE_EVENTS"));
            assertFalse(copy.passed());
        }

        @Test
        @DisplayName("failOnIntegrityViolation 关闭时完整性失败不影响通过")
        void integrityFailureIgnoredWhenPolicyAllows() {
            Pol policy = pol();
            policy.failOnIntegrityViolation = false;
            Sub submission = sub().plus(ans().build());
            submission.suspended = true;
            assertTrue(grading.grade(submission.build(), policy.build()).passed());
        }

        @Test
        @DisplayName("章节通过率与缺失章节")
        void sectionRules() {
            Pol policy = pol();
            policy.minimumSectionPercentage = 80;
            Sub failing = sub().plus(ans().exact(false).build());
            GradeReport below = grading.grade(failing.build(), policy.build());
            assertEquals(Map.of("A", 0), below.sectionPercentages());
            assertTrue(below.failures().contains("SECTION_BELOW_PASS_MARK:A"));
            assertFalse(below.passed());

            Sub missing = sub().plus(ans().build());
            missing.sectionPass.put("B", 60);
            GradeReport absent = grading.grade(missing.build(), pol().build());
            assertEquals(0, absent.sectionPercentages().get("B"));
            assertTrue(absent.failures().contains("REQUIRED_SECTION_MISSING:B"));
            assertFalse(absent.passed());
        }

        @Test
        @DisplayName("章节通过率按章节独立计算")
        void sectionPercentagesAreIndependent() {
            Sub submission = sub()
                    .plus(ans().id("q1").section("A").build())
                    .plus(ans().id("q2").section("B").exact(false).build());
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(Map.of("A", 100, "B", 0), report.sectionPercentages());
            assertEquals(bd("20.00"), report.maximumPoints());
            assertEquals(50, report.percentage());
            assertFalse(report.passed());
        }

        @Test
        @DisplayName("必答题未完成且策略不强制时仍不通过（分数不足）")
        void mandatoryRequirementDisabled() {
            Pol policy = pol();
            policy.allMandatoryQuestionsRequired = false;
            Sub submission = sub().plus(ans().mandatory(true).answered(false).exact(false).build());
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertFalse(report.passed());
            assertTrue(report.failures().contains("MANDATORY_QUESTION_UNANSWERED:q1"));
        }

        @Test
        @DisplayName("等级边界：95/94/90/89/85/84/80/79/75/74/70/69/65/64/60")
        void letterGradeBoundaries() {
            assertEquals("A+", gradeWithOverride(95).letterGrade());
            assertEquals("A", gradeWithOverride(94).letterGrade());
            assertEquals("A", gradeWithOverride(90).letterGrade());
            assertEquals("A-", gradeWithOverride(89).letterGrade());
            assertEquals("A-", gradeWithOverride(85).letterGrade());
            assertEquals("B+", gradeWithOverride(84).letterGrade());
            assertEquals("B+", gradeWithOverride(80).letterGrade());
            assertEquals("B", gradeWithOverride(79).letterGrade());
            assertEquals("B", gradeWithOverride(75).letterGrade());
            assertEquals("C+", gradeWithOverride(74).letterGrade());
            assertEquals("C+", gradeWithOverride(70).letterGrade());
            assertEquals("C", gradeWithOverride(69).letterGrade());
            assertEquals("C", gradeWithOverride(65).letterGrade());
            assertEquals("D", gradeWithOverride(64).letterGrade());
            assertEquals("D", gradeWithOverride(60).letterGrade());
        }

        @Test
        @DisplayName("不及格且低于 60 分时等级为 F")
        void failingGradeBelowSixty() {
            Pol policy = pol();
            policy.passPercentage = 80;
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = 50;
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertEquals("F", report.letterGrade());
            assertFalse(report.passed());
        }

        private GradeReport gradeWithOverride(int percent) {
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = percent;
            return grading.grade(submission.build(), pol().build());
        }
    }

    @Nested
    @DisplayName("ExamGradingService 参数校验")
    class GradingValidation {

        @Test
        @DisplayName("submission 与 policy 为 null 时抛 NPE")
        void nullArguments() {
            assertThrows(NullPointerException.class, () -> grading.grade(null, pol().build()));
            assertThrows(NullPointerException.class,
                    () -> grading.grade(sub().plus(ans().build()).build(), null));
        }

        @Test
        @DisplayName("提交头字段校验消息")
        void submissionHeaderValidation() {
            assertEquals("examId must not be blank", illegalSubmission(s -> s.examId = "   "));
            assertEquals("studentId must not be blank", illegalSubmission(s -> s.studentId = null));
            assertEquals("exam timestamps must not be null", illegalSubmission(s -> s.startedAt = null));
            assertEquals("exam timestamps must not be null", illegalSubmission(s -> s.submittedAt = null));
            assertEquals("submittedAt must not be before startedAt",
                    illegalSubmission(s -> s.submittedAt = T0.minusSeconds(1)));
            assertEquals("allowedDurationSeconds must not be negative",
                    illegalSubmission(s -> s.allowedDurationSeconds = -1));
            assertEquals("attemptNumber must be positive", illegalSubmission(s -> s.attemptNumber = 0));
            assertEquals("integrity event counts must not be negative",
                    illegalSubmission(s -> s.fullscreenExitCount = -1));
            assertEquals("integrity event counts must not be negative",
                    illegalSubmission(s -> s.copyPasteEventCount = -1));
            assertEquals("bonusPoints must not be negative", illegalSubmission(s -> s.bonusPoints = bd("-0.01")));
            assertEquals("bonusPoints must not be negative", illegalSubmission(s -> s.bonusPoints = null));
            assertEquals("override percentage must be between 0 and 100",
                    illegalSubmission(s -> s.overridePercent = -1));
            assertEquals("override percentage must be between 0 and 100",
                    illegalSubmission(s -> s.overridePercent = 101));
        }

        @Test
        @DisplayName("重复 questionId 与章节通过率校验")
        void duplicateAndSectionValidation() {
            Sub duplicate = sub().plus(ans().id("dup").build()).plus(ans().id("dup").exact(false).build());
            assertEquals("questionId must be unique",
                    assertThrows(IllegalArgumentException.class,
                            () -> grading.grade(duplicate.build(), pol().build())).getMessage());

            assertEquals("section name must not be blank", illegalSection(Map.of("  ", 50)));
            assertEquals("section percentage must be between 0 and 100", illegalSection(Map.of("A", -1)));
            assertEquals("section percentage must be between 0 and 100", illegalSection(Map.of("A", 101)));
        }

        @Test
        @DisplayName("策略字段越界")
        void invalidPolicyFields() {
            assertEquals("passPercentage must be between 0 and 100", illegalPolicy(p -> p.passPercentage = -1));
            assertEquals("passPercentage must be between 0 and 100", illegalPolicy(p -> p.passPercentage = 101));
            assertEquals("wrongAnswerPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.wrongAnswerPenaltyRate = null));
            assertEquals("wrongAnswerPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.wrongAnswerPenaltyRate = bd("-0.1")));
            assertEquals("wrongAnswerPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.wrongAnswerPenaltyRate = bd("1.1")));
            assertEquals("unansweredPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.unansweredPenaltyRate = null));
            assertEquals("unansweredPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.unansweredPenaltyRate = bd("-0.1")));
            assertEquals("unansweredPenaltyRate must be between 0 and 1",
                    illegalPolicy(p -> p.unansweredPenaltyRate = bd("1.1")));
            assertEquals("late penalties must be between 0 and 100",
                    illegalPolicy(p -> p.latePenaltyPercentPerHour = -1));
            assertEquals("late penalties must be between 0 and 100",
                    illegalPolicy(p -> p.maximumLatePenaltyPercent = -1));
            assertEquals("late penalties must be between 0 and 100",
                    illegalPolicy(p -> p.maximumLatePenaltyPercent = 101));
            assertEquals("minimumSectionPercentage must be between 0 and 100",
                    illegalPolicy(p -> p.minimumSectionPercentage = -1));
            assertEquals("minimumSectionPercentage must be between 0 and 100",
                    illegalPolicy(p -> p.minimumSectionPercentage = 101));
            assertEquals("maximumAttempts must be positive", illegalPolicy(p -> p.maximumAttempts = 0));
        }

        @Test
        @DisplayName("答案字段非法：逐项校验消息")
        void invalidAnswerFields() {
            assertEquals("questionId must not be blank", illegalAnswer(a -> a.id = " "));
            assertEquals("question type must not be null", illegalAnswer(a -> a.type = null));
            assertEquals("section must not be blank", illegalAnswer(a -> a.section = null));
            assertEquals("section must not be blank", illegalAnswer(a -> a.section = " "));
            assertEquals("maximumPoints must be positive", illegalAnswer(a -> a.max = null));
            assertEquals("maximumPoints must be positive", illegalAnswer(a -> a.max = BigDecimal.ZERO));
            assertEquals("option counts must not be negative", illegalAnswer(a -> a.correctOptionCount = -1));
            assertEquals("option counts must not be negative", illegalAnswer(a -> a.selectedCorrectOptions = -1));
            assertEquals("option counts must not be negative", illegalAnswer(a -> a.selectedIncorrectOptions = -1));
            assertEquals("selected correct options exceed available options", illegalAnswer(a -> {
                a.correctOptionCount = 1;
                a.selectedCorrectOptions = 2;
            }));
            assertEquals("numericTolerance must not be negative", illegalAnswer(a -> {
                a.type = QuestionType.NUMERIC;
                a.numericTolerance = bd("-1");
            }));
            assertEquals("rubricPercent must be between 0 and 100", illegalAnswer(a -> a.rubricPercent = -1));
            assertEquals("rubricPercent must be between 0 and 100", illegalAnswer(a -> a.rubricPercent = 101));
            assertEquals("program test counts are invalid", illegalAnswer(a -> {
                a.type = QuestionType.PROGRAMMING;
                a.passedTests = -1;
            }));
            assertEquals("program test counts are invalid", illegalAnswer(a -> {
                a.type = QuestionType.PROGRAMMING;
                a.totalTests = -1;
            }));
            assertEquals("program test counts are invalid", illegalAnswer(a -> {
                a.type = QuestionType.PROGRAMMING;
                a.passedTests = 2;
                a.totalTests = 1;
            }));
            assertEquals("compiled program requires tests or manual review", illegalAnswer(a -> {
                a.type = QuestionType.PROGRAMMING;
                a.compilationSucceeded = true;
                a.totalTests = 0;
                a.manualReview = false;
            }));
        }

        @Test
        @DisplayName("反射直调 validateAnswer 覆盖 null 答案分支")
        void validateAnswerRejectsNull() throws Exception {
            Method method = ExamGradingService.class.getDeclaredMethod("validateAnswer", AnswerResult.class);
            method.setAccessible(true);
            InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                    () -> method.invoke(grading, (AnswerResult) null));
            assertTrue(exception.getCause() instanceof NullPointerException);
        }

        private String illegalSubmission(java.util.function.Consumer<Sub> mutation) {
            Sub submission = sub().plus(ans().build());
            mutation.accept(submission);
            return assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(submission.build(), pol().build())).getMessage();
        }

        private String illegalSection(Map<String, Integer> sectionPass) {
            Sub submission = sub().plus(ans().build());
            submission.sectionPass = new LinkedHashMap<>(sectionPass);
            return assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(submission.build(), pol().build())).getMessage();
        }

        private String illegalPolicy(java.util.function.Consumer<Pol> mutation) {
            Pol policy = pol();
            mutation.accept(policy);
            return assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(sub().plus(ans().build()).build(), policy.build())).getMessage();
        }

        private String illegalAnswer(java.util.function.Consumer<Ans> mutation) {
            Ans answer = ans();
            mutation.accept(answer);
            return assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(sub().plus(answer.build()).build(), pol().build())).getMessage();
        }
    }

    // ============================================================ enrollment

    /** EnrollmentHistory 的可配置测试替身。 */
    private static final class History implements EnrollmentHistory {
        boolean alreadyEnrolled;
        boolean refundAbuse;
        boolean scheduleConflict;
        Set<String> completed = new java.util.HashSet<>();
        Map<String, Integer> scores = new java.util.HashMap<>();
        int activeCourses;
        int failedAttempts;

        @Override
        public boolean alreadyEnrolled(String studentId, String courseId) {
            return alreadyEnrolled;
        }

        @Override
        public boolean completedCourse(String studentId, String courseId) {
            return completed.contains(courseId);
        }

        @Override
        public int completionScore(String studentId, String courseId) {
            return scores.getOrDefault(courseId, 0);
        }

        @Override
        public boolean hasScheduleConflict(String studentId, String scheduleKey) {
            return scheduleConflict;
        }

        @Override
        public int activeCourseCountThisWeek(String studentId) {
            return activeCourses;
        }

        @Override
        public int failedEnrollmentAttempts(String studentId, String courseId) {
            return failedAttempts;
        }

        @Override
        public boolean hasRefundAbuseFlag(String studentId) {
            return refundAbuse;
        }
    }

    /** StudentProfile 构造辅助。 */
    private static final class Stu {
        String id = "s1";
        int age = 20;
        String country = "CN";
        String language = "en";
        MembershipLevel level = MembershipLevel.BASIC;
        boolean active = true;
        boolean suspended = false;
        boolean emailVerified = true;
        boolean identityVerified = true;
        boolean profileComplete = true;
        boolean instructor = false;
        boolean parentalConsent = false;
        boolean termsAccepted = true;
        boolean subscription = true;
        int risk = 10;

        StudentProfile build() {
            return new StudentProfile(id, age, country, language, level, active, suspended,
                    emailVerified, identityVerified, profileComplete, instructor, parentalConsent,
                    termsAccepted, subscription, risk);
        }
    }

    /** CourseOffering 构造辅助。 */
    private static final class Off {
        String courseId = "c1";
        String instructorId = "i1";
        EnrollmentCourseStatus status = EnrollmentCourseStatus.PUBLISHED;
        Instant opens = null;
        Instant closes = null;
        Instant starts = null;
        int capacity = 30;
        int enrolled = 10;
        boolean waitlistEnabled = true;
        int waitlistCapacity = 5;
        int waitlisted = 0;
        int minAge = 0;
        int maxAge = 0;
        Set<String> countries = Set.of("CN");
        String requiredLanguage = null;
        Set<String> prereqs = Set.of();
        int minPrereqScore = 0;
        int maxWeekly = 0;
        int maxFailed = 0;
        boolean organizationOnly = false;
        String requiredOrg = null;
        boolean subscriptionRequired = false;
        boolean liveCourse = false;
        String scheduleKey = null;
        int accessCapacity = 0;
        int accessUsed = 0;
        BigDecimal price = BigDecimal.ZERO;

        CourseOffering build() {
            return new CourseOffering(courseId, instructorId, status, opens, closes, starts,
                    capacity, enrolled, waitlistEnabled, waitlistCapacity, waitlisted, minAge,
                    maxAge, countries, requiredLanguage, prereqs, minPrereqScore, maxWeekly,
                    maxFailed, organizationOnly, requiredOrg, subscriptionRequired, liveCourse,
                    scheduleKey, accessCapacity, accessUsed, price);
        }
    }

    /** EnrollmentRequest 构造辅助。 */
    private static final class Req {
        StudentProfile student = new Stu().build();
        CourseOffering course = new Off().build();
        int seats = 1;
        boolean paymentAuthorized = false;
        boolean scholarship = false;
        boolean accessibility = false;
        boolean sponsored = false;
        String orgId = null;
        boolean audit = false;
        boolean acceptWaitlist = false;

        EnrollmentRequest build() {
            return new EnrollmentRequest(student, course, seats, paymentAuthorized, scholarship,
                    accessibility, sponsored, orgId, audit, acceptWaitlist);
        }
    }

    @Nested
    @DisplayName("EnrollmentEligibilityService 选课资格")
    class EnrollmentEligibility {

        private final History history = new History();
        private final EnrollmentEligibilityService service =
                new EnrollmentEligibilityService(history, CLOCK);

        private EnrollmentDecision eval(Stu student, Off course, java.util.function.Consumer<Req> mutation) {
            Req request = new Req();
            request.student = student.build();
            request.course = course.build();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        private EnrollmentDecision eval(Stu student, Off course) {
            return eval(student, course, null);
        }

        @Test
        @DisplayName("满足全部条件时录取，剩余名额与付款标记正确")
        void basicEnrollmentSucceeds() {
            EnrollmentDecision decision = eval(new Stu(), new Off());
            assertEquals(EnrollmentOutcome.ENROLLED, decision.outcome());
            assertTrue(decision.accepted());
            assertFalse(decision.paymentRequired());
            assertEquals(20, decision.availableSeats());
            assertTrue(decision.reasons().isEmpty());
            assertTrue(decision.warnings().isEmpty());
        }

        @Test
        @DisplayName("账号状态类阻塞项")
        void accountStateBlockers() {
            Stu inactive = new Stu();
            inactive.active = false;
            assertTrue(eval(inactive, new Off()).reasons().contains("ACCOUNT_INACTIVE"));

            Stu suspended = new Stu();
            suspended.suspended = true;
            assertTrue(eval(suspended, new Off()).reasons().contains("ACCOUNT_SUSPENDED"));

            Stu mail = new Stu();
            mail.emailVerified = false;
            assertTrue(eval(mail, new Off()).reasons().contains("EMAIL_NOT_VERIFIED"));

            Stu profile = new Stu();
            profile.profileComplete = false;
            assertTrue(eval(profile, new Off()).reasons().contains("PROFILE_INCOMPLETE"));

            Stu terms = new Stu();
            terms.termsAccepted = false;
            assertTrue(eval(terms, new Off()).reasons().contains("TERMS_NOT_ACCEPTED"));
        }

        @Test
        @DisplayName("直播课要求身份验证，非直播课只给警告")
        void identityRules() {
            Stu unverified = new Stu();
            unverified.identityVerified = false;
            Off live = new Off();
            live.liveCourse = true;
            live.scheduleKey = "MON-10";
            assertTrue(eval(unverified, live).reasons().contains("IDENTITY_REQUIRED_FOR_LIVE_COURSE"));
            assertTrue(eval(unverified, new Off()).warnings().contains("IDENTITY_NOT_VERIFIED"));
        }

        @Test
        @DisplayName("未成年人需要监护人同意，成年人多余同意给警告")
        void parentalConsentRules() {
            Stu minor = new Stu();
            minor.age = 16;
            assertTrue(eval(minor, new Off()).reasons().contains("PARENTAL_CONSENT_REQUIRED"));

            Stu minorWithConsent = new Stu();
            minorWithConsent.age = 16;
            minorWithConsent.parentalConsent = true;
            assertTrue(eval(minorWithConsent, new Off()).warnings().isEmpty());

            Stu adultWithConsent = new Stu();
            adultWithConsent.parentalConsent = true;
            assertTrue(eval(adultWithConsent, new Off()).warnings()
                    .contains("UNNECESSARY_PARENTAL_CONSENT"));
        }

        @Test
        @DisplayName("课程状态类阻塞项")
        void courseStatusBlockers() {
            Off draft = new Off();
            draft.status = EnrollmentCourseStatus.DRAFT;
            assertTrue(eval(new Stu(), draft).reasons().contains("COURSE_NOT_PUBLISHED"));

            Off closed = new Off();
            closed.status = EnrollmentCourseStatus.CLOSED;
            assertTrue(eval(new Stu(), closed).reasons().contains("COURSE_CLOSED"));

            Off archived = new Off();
            archived.status = EnrollmentCourseStatus.ARCHIVED;
            assertTrue(eval(new Stu(), archived).reasons().contains("COURSE_ARCHIVED"));
            assertTrue(eval(new Stu(), archived, r -> r.audit = true).warnings()
                    .contains("ARCHIVED_COURSE_AUDIT_ONLY"));

            Off missing = new Off();
            missing.status = null;
            assertTrue(eval(new Stu(), missing).reasons().contains("COURSE_STATUS_MISSING"));
        }

        @Test
        @DisplayName("报名时间窗与非法的窗口配置")
        void enrollmentWindowRules() {
            Off invalid = new Off();
            invalid.opens = T0.plusSeconds(7200);
            invalid.closes = T0;
            assertEquals("enrollment window is invalid",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(new Stu(), invalid)).getMessage());

            Off future = new Off();
            future.opens = T0.plusSeconds(3600);
            assertTrue(eval(new Stu(), future).reasons().contains("ENROLLMENT_NOT_OPEN"));

            Off expired = new Off();
            expired.opens = T0.minusSeconds(7200);
            expired.closes = T0.minusSeconds(3600);
            assertTrue(eval(new Stu(), expired).reasons().contains("ENROLLMENT_CLOSED"));

            Off started = new Off();
            started.starts = T0.minusSeconds(60);
            assertTrue(eval(new Stu(), started).reasons().contains("COURSE_ALREADY_STARTED"));
            assertTrue(eval(new Stu(), started, r -> r.audit = true).warnings()
                    .contains("LATE_AUDIT_ENROLLMENT"));
        }

        @Test
        @DisplayName("容量配置非法与超员警告")
        void capacityConfiguration() {
            Off negative = new Off();
            negative.capacity = -1;
            assertEquals("course capacity values must not be negative",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(new Stu(), negative)).getMessage());

            Off overbooked = new Off();
            overbooked.enrolled = 40;
            assertTrue(eval(new Stu(), overbooked).warnings().contains("COURSE_IS_OVERBOOKED"));
        }

        @Test
        @DisplayName("年龄与国家限制")
        void demographicsRules() {
            Stu invalidAge = new Stu();
            invalidAge.age = 130;
            assertTrue(eval(invalidAge, new Off()).reasons().contains("INVALID_STUDENT_AGE"));

            Off minimum = new Off();
            minimum.minAge = 21;
            assertTrue(eval(new Stu(), minimum).reasons().contains("BELOW_MINIMUM_AGE"));

            Off maximum = new Off();
            maximum.maxAge = 19;
            assertTrue(eval(new Stu(), maximum).reasons().contains("ABOVE_MAXIMUM_AGE"));

            Off invalidRange = new Off();
            invalidRange.minAge = 30;
            invalidRange.maxAge = 20;
            assertEquals("course age range is invalid",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(new Stu(), invalidRange)).getMessage());

            Stu noCountry = new Stu();
            noCountry.country = " ";
            assertTrue(eval(noCountry, new Off()).reasons().contains("COUNTRY_MISSING"));

            Stu wrongCountry = new Stu();
            wrongCountry.country = "US";
            assertTrue(eval(wrongCountry, new Off()).reasons().contains("COUNTRY_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("语言要求与缺少语言画像")
        void languageRules() {
            Off required = new Off();
            required.requiredLanguage = "fr";
            Stu noLanguage = new Stu();
            noLanguage.language = null;
            assertTrue(eval(noLanguage, required).reasons().contains("LANGUAGE_PROFILE_MISSING"));
            assertTrue(eval(new Stu(), required).reasons().contains("LANGUAGE_REQUIREMENT_NOT_MET"));

            Stu french = new Stu();
            french.language = "FR";
            assertTrue(eval(french, required).reasons().isEmpty());

            Stu blankLanguage = new Stu();
            blankLanguage.language = "";
            assertTrue(eval(blankLanguage, new Off()).warnings()
                    .contains("PREFERRED_LANGUAGE_MISSING"));
        }

        @Test
        @DisplayName("组织与订阅限制")
        void organizationRules() {
            Off organization = new Off();
            organization.organizationOnly = true;
            assertTrue(eval(new Stu(), organization).reasons()
                    .contains("ORGANIZATION_SPONSOR_REQUIRED"));
            assertTrue(eval(new Stu(), organization, r -> r.sponsored = true).reasons()
                    .contains("ORGANIZATION_ID_REQUIRED"));

            Off requiredOrg = new Off();
            requiredOrg.requiredOrg = "org-1";
            assertTrue(eval(new Stu(), requiredOrg, r -> r.orgId = "org-2").reasons()
                    .contains("ORGANIZATION_NOT_ALLOWED"));
            assertTrue(eval(new Stu(), requiredOrg, r -> r.orgId = "org-1").reasons().isEmpty());

            assertTrue(eval(new Stu(), new Off(), r -> r.sponsored = true).warnings()
                    .contains("OPTIONAL_ORGANIZATION_SPONSORSHIP"));

            Off subscription = new Off();
            subscription.subscriptionRequired = true;
            Stu noSubscription = new Stu();
            noSubscription.subscription = false;
            assertTrue(eval(noSubscription, subscription).reasons()
                    .contains("ACTIVE_SUBSCRIPTION_REQUIRED"));
            assertTrue(eval(noSubscription, subscription, r -> r.sponsored = true).warnings()
                    .contains("SUBSCRIPTION_COVERED_BY_ORGANIZATION"));
            assertTrue(eval(noSubscription, subscription, r -> r.scholarship = true).warnings()
                    .contains("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP"));

            assertTrue(eval(new Stu(), new Off(), r -> r.seats = 2).reasons()
                    .contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"));
        }

        @Test
        @DisplayName("教师选自己的课、已选课与退款滥用标记")
        void historyAndIdentityRules() {
            Stu instructor = new Stu();
            instructor.instructor = true;
            instructor.id = "i1";
            assertTrue(eval(instructor, new Off()).reasons()
                    .contains("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"));

            history.alreadyEnrolled = true;
            assertTrue(eval(new Stu(), new Off()).reasons().contains("ALREADY_ENROLLED"));
            history.alreadyEnrolled = false;

            history.refundAbuse = true;
            EnrollmentDecision abuser = eval(new Stu(), new Off());
            assertTrue(abuser.warnings().contains("REFUND_ABUSE_REVIEW"));
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, abuser.outcome());
            history.refundAbuse = false;
        }

        @Test
        @DisplayName("风险分三档")
        void riskScoreBuckets() {
            Stu high = new Stu();
            high.risk = 95;
            assertTrue(eval(high, new Off()).reasons().contains("RISK_SCORE_TOO_HIGH"));

            Stu elevated = new Stu();
            elevated.risk = 75;
            EnrollmentDecision review = eval(elevated, new Off());
            assertTrue(review.warnings().contains("HIGH_RISK_MANUAL_REVIEW"));
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, review.outcome());

            Stu monitor = new Stu();
            monitor.risk = 55;
            assertTrue(eval(monitor, new Off()).warnings().contains("ELEVATED_RISK_MONITORING"));
        }

        @Test
        @DisplayName("历史失败次数")
        void failedAttemptRules() {
            history.failedAttempts = -1;
            assertEquals("failed attempt count must not be negative",
                    assertThrows(IllegalStateException.class,
                            () -> eval(new Stu(), new Off())).getMessage());
            history.failedAttempts = 0;

            Off limited = new Off();
            limited.maxFailed = 2;
            history.failedAttempts = 2;
            assertTrue(eval(new Stu(), limited).reasons().contains("TOO_MANY_FAILED_ATTEMPTS"));
            history.failedAttempts = 1;
            assertTrue(eval(new Stu(), limited).warnings().contains("PREVIOUS_FAILED_ATTEMPTS"));
            history.failedAttempts = 0;
        }

        @Test
        @DisplayName("直播排课冲突与自学课排课键")
        void scheduleRules() {
            Off live = new Off();
            live.liveCourse = true;
            assertTrue(eval(new Stu(), live).reasons().contains("LIVE_COURSE_WITHOUT_SCHEDULE"));

            live.scheduleKey = "MON-10";
            history.scheduleConflict = true;
            assertTrue(eval(new Stu(), live).reasons().contains("SCHEDULE_CONFLICT"));
            assertTrue(eval(new Stu(), live, r -> r.audit = true).warnings()
                    .contains("SCHEDULE_CONFLICT_ALLOWED_IN_AUDIT_MODE"));
            history.scheduleConflict = false;

            Off selfPaced = new Off();
            selfPaced.scheduleKey = "MON-10";
            assertTrue(eval(new Stu(), selfPaced).warnings()
                    .contains("SELF_PACED_COURSE_IGNORES_SCHEDULE"));
        }

        @Test
        @DisplayName("每周课程上限的三个分支")
        void weeklyLimitRules() {
            history.activeCourses = -1;
            assertEquals("active course count must not be negative",
                    assertThrows(IllegalStateException.class,
                            () -> eval(new Stu(), new Off())).getMessage());
            history.activeCourses = 0;

            Off limited = new Off();
            limited.maxWeekly = 2;
            history.activeCourses = 2;
            assertTrue(eval(new Stu(), limited).reasons().contains("WEEKLY_COURSE_LIMIT_REACHED"));

            Stu premium = new Stu();
            premium.level = MembershipLevel.PREMIUM;
            assertTrue(eval(premium, limited).warnings().contains("PREMIUM_WEEKLY_LIMIT_GRACE"));

            Stu enterprise = new Stu();
            enterprise.level = MembershipLevel.ENTERPRISE;
            assertTrue(eval(enterprise, limited, r -> r.sponsored = true).warnings()
                    .contains("ENTERPRISE_WEEKLY_LIMIT_OVERRIDE"));
            history.activeCourses = 0;
        }

        @Test
        @DisplayName("付款需求判定")
        void paymentRules() {
            Off priced = new Off();
            priced.price = bd("100.00");
            assertTrue(eval(new Stu(), priced).reasons().contains("PAYMENT_NOT_AUTHORIZED"));
            assertTrue(eval(new Stu(), priced, r -> r.paymentAuthorized = true).reasons().isEmpty());
            assertEquals(EnrollmentOutcome.ENROLLED,
                    eval(new Stu(), priced, r -> r.paymentAuthorized = true).outcome());
            assertTrue(eval(new Stu(), priced, r -> r.audit = true).reasons().isEmpty());
            assertTrue(eval(new Stu(), priced, r -> r.scholarship = true).reasons().isEmpty());
            assertTrue(eval(new Stu(), priced, r -> r.sponsored = true).reasons().isEmpty());

            assertTrue(eval(new Stu(), new Off(), r -> r.paymentAuthorized = true).warnings()
                    .contains("PAYMENT_AUTHORIZATION_NOT_REQUIRED"));

            EnrollmentDecision both = eval(new Stu(), priced, r -> {
                r.scholarship = true;
                r.sponsored = true;
            });
            assertTrue(both.warnings().contains("SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT"));
        }

        @Test
        @DisplayName("名额不足时的候补分支")
        void waitlistBranches() {
            Off full = new Off();
            full.capacity = 10;
            full.enrolled = 10;
            full.waitlistEnabled = false;
            assertEquals(List.of("COURSE_FULL"), eval(new Stu(), full).reasons());

            Off waitlist = new Off();
            waitlist.capacity = 10;
            waitlist.enrolled = 10;
            assertTrue(eval(new Stu(), waitlist).reasons().contains("WAITLIST_NOT_ACCEPTED"));

            Off noCapacity = new Off();
            noCapacity.capacity = 10;
            noCapacity.enrolled = 10;
            noCapacity.waitlistCapacity = 0;
            assertTrue(eval(new Stu(), noCapacity, r -> r.acceptWaitlist = true).reasons()
                    .contains("WAITLIST_DISABLED_BY_CAPACITY"));

            Off waitlistFull = new Off();
            waitlistFull.capacity = 10;
            waitlistFull.enrolled = 10;
            waitlistFull.waitlistCapacity = 2;
            waitlistFull.waitlisted = 2;
            assertTrue(eval(new Stu(), waitlistFull, r -> r.acceptWaitlist = true).reasons()
                    .contains("WAITLIST_FULL"));

            Off group = new Off();
            group.capacity = 10;
            group.enrolled = 10;
            Stu enterprise = new Stu();
            enterprise.level = MembershipLevel.ENTERPRISE;
            assertTrue(eval(enterprise, group, r -> {
                r.acceptWaitlist = true;
                r.seats = 2;
            }).reasons().contains("GROUP_ENROLLMENT_CANNOT_WAITLIST"));
        }

        @Test
        @DisplayName("候补成功与人工复核候补")
        void waitlistSuccess() {
            Off waitlist = new Off();
            waitlist.capacity = 10;
            waitlist.enrolled = 10;
            EnrollmentDecision decision = eval(new Stu(), waitlist, r -> r.acceptWaitlist = true);
            assertEquals(EnrollmentOutcome.WAITLISTED, decision.outcome());
            assertFalse(decision.paymentRequired());
            assertTrue(decision.warnings().contains("PLACED_ON_WAITLIST"));

            history.refundAbuse = true;
            EnrollmentDecision review = eval(new Stu(), waitlist, r -> r.acceptWaitlist = true);
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, review.outcome());
            assertTrue(review.warnings().contains("WAITLIST_AFTER_MANUAL_REVIEW"));
            history.refundAbuse = false;
        }

        @Test
        @DisplayName("名额接近用尽的时间点会写 COURSE_ALMOST_FULL")
        void almostFullWarning() {
            Off almost = new Off();
            almost.capacity = 3;
            almost.enrolled = 0;
            assertTrue(eval(new Stu(), almost, r -> r.seats = 1).warnings()
                    .contains("COURSE_ALMOST_FULL"));
        }

        @Test
        @DisplayName("无障碍席位规则")
        void accessibilityRules() {
            Off none = new Off();
            assertTrue(eval(new Stu(), none, r -> r.accessibility = true).reasons()
                    .contains("NO_ACCESSIBILITY_SEATS"));

            Off fullSeats = new Off();
            fullSeats.accessCapacity = 2;
            fullSeats.accessUsed = 2;
            assertTrue(eval(new Stu(), fullSeats, r -> r.accessibility = true).reasons()
                    .contains("ACCESSIBILITY_SEATS_FULL"));

            Off available = new Off();
            available.accessCapacity = 2;
            available.accessUsed = 1;
            assertTrue(eval(new Stu(), available, r -> r.accessibility = true).warnings()
                    .contains("ACCESSIBILITY_SEAT_RESERVED"));

            Off invalidConfig = new Off();
            invalidConfig.accessCapacity = 1;
            invalidConfig.accessUsed = 2;
            assertTrue(eval(new Stu(), invalidConfig).warnings()
                    .contains("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID"));
        }

        @Test
        @DisplayName("先修课规则")
        void prerequisiteRules() {
            Off blank = new Off();
            blank.prereqs = new java.util.LinkedHashSet<>(java.util.Arrays.asList("  "));
            assertTrue(eval(new Stu(), blank).warnings()
                    .contains("INVALID_PREREQUISITE_CONFIGURATION"));

            Off missing = new Off();
            missing.prereqs = Set.of("pre-1");
            assertTrue(eval(new Stu(), missing).reasons().contains("MISSING_PREREQUISITE:pre-1"));

            Off completed = new Off();
            completed.prereqs = Set.of("pre-1");
            history.completed.add("pre-1");
            history.scores.put("pre-1", 90);
            assertTrue(eval(new Stu(), completed).warnings()
                    .contains("ALL_PREREQUISITES_COMPLETED"));

            history.scores.put("pre-1", 120);
            assertTrue(eval(new Stu(), completed).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));

            Off minimum = new Off();
            minimum.prereqs = Set.of("pre-1");
            minimum.minPrereqScore = 95;
            history.scores.put("pre-1", 80);
            assertTrue(eval(new Stu(), minimum).reasons()
                    .contains("PREREQUISITE_SCORE_TOO_LOW:pre-1"));

            history.scores.put("pre-1", 96);
            assertTrue(eval(new Stu(), minimum).warnings()
                    .contains("EXCELLENT_PREREQUISITE_SCORE:pre-1"));
            history.completed.clear();
            history.scores.clear();
        }

        @Test
        @DisplayName("结构校验")
        void structuralValidation() {
            Req empty = new Req();
            empty.student = null;
            assertEquals("student must not be null",
                    assertThrows(NullPointerException.class,
                            () -> service.evaluate(empty.build())).getMessage());

            Req noCourse = new Req();
            noCourse.course = null;
            assertEquals("course must not be null",
                    assertThrows(NullPointerException.class,
                            () -> service.evaluate(noCourse.build())).getMessage());

            assertEquals("request must not be null",
                    assertThrows(NullPointerException.class, () -> service.evaluate(null)).getMessage());

            assertEquals("studentId must not be blank", structural(r -> {
                Stu student = new Stu();
                student.id = " ";
                r.student = student.build();
            }));
            assertEquals("courseId must not be blank", structural(r -> {
                Off course = new Off();
                course.courseId = null;
                r.course = course.build();
            }));
            assertEquals("requestedSeats must be positive", structural(r -> r.seats = 0));
            assertEquals("requestedSeats must not exceed 50", structural(r -> r.seats = 51));
            assertEquals("riskScore must be between 0 and 100", structural(r -> {
                Stu student = new Stu();
                student.risk = -1;
                r.student = student.build();
            }));
            assertEquals("riskScore must be between 0 and 100", structural(r -> {
                Stu student = new Stu();
                student.risk = 101;
                r.student = student.build();
            }));
        }

        private String structural(java.util.function.Consumer<Req> mutation) {
            Req request = new Req();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(request.build())).getMessage();
        }
    }

    // ============================================================ course

    @Nested
    @DisplayName("Course 领域与 CourseService/CatalogService")
    class CourseServices {

        private final InMemoryCourseRepository courses = new InMemoryCourseRepository();
        private final PricePolicy pricePolicy = new PricePolicy();
        private final IdGenerator fixedIds = () -> "course-1";
        private final ChapterRepository emptyChapters = new InMemoryChapterRepository();

        private CourseService service(ChapterRepository chapters) {
            return new CourseService(courses, chapters, fixedIds, pricePolicy, CLOCK);
        }

        private CourseService service() {
            return service(emptyChapters);
        }

        @Test
        @DisplayName("创建课程会归一化标题与价格")
        void createCourseNormalizes() {
            Course course = service().createCourse("  Intro   To   Math  ", bd("9.999"));
            assertEquals("course-1", course.id());
            assertEquals("Intro To Math", course.title());
            assertEquals(bd("10.00"), course.price());
            assertEquals(CourseStatus.DRAFT, course.status());
            assertEquals(T0, course.createdAt());
            assertEquals(course, courses.findById("course-1").orElseThrow());
        }

        @Test
        @DisplayName("重复标题（忽略大小写）被拒绝")
        void duplicateTitle() {
            service().createCourse("Intro To Math", bd("10"));
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service().createCourse("intro to math", bd("10")));
            assertEquals(ErrorCode.DUPLICATE_COURSE_TITLE, exception.code());
        }

        @Test
        @DisplayName("标题长度与空值校验")
        void invalidTitles() {
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, assertThrows(BusinessException.class,
                    () -> service().createCourse(null, bd("10"))).code());
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, assertThrows(BusinessException.class,
                    () -> service().createCourse("ab", bd("10"))).code());
            assertEquals(ErrorCode.INVALID_COURSE_TITLE, assertThrows(BusinessException.class,
                    () -> service().createCourse("x".repeat(81), bd("10"))).code());
            assertEquals("Course title length must be between 3 and 80 characters",
                    assertThrows(BusinessException.class,
                            () -> service().createCourse("ab", bd("10"))).getMessage());
        }

        @Test
        @DisplayName("价格校验")
        void invalidPrices() {
            assertEquals(ErrorCode.INVALID_PRICE, assertThrows(BusinessException.class,
                    () -> service().createCourse("Valid Title", null)).code());
            assertEquals(ErrorCode.INVALID_PRICE, assertThrows(BusinessException.class,
                    () -> service().createCourse("Valid Title", bd("-1"))).code());
            assertEquals(ErrorCode.INVALID_PRICE, assertThrows(BusinessException.class,
                    () -> service().createCourse("Valid Title", bd("100000.00"))).code());
            assertEquals("Price must be between 0 and 99999.99",
                    assertThrows(BusinessException.class,
                            () -> service().createCourse("Valid Title", bd("-1"))).getMessage());
        }

        @Test
        @DisplayName("查询不存在的课程")
        void courseNotFound() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service().getCourse("missing"));
            assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.code());
            assertEquals("Course not found: missing", exception.getMessage());
        }

        @Test
        @DisplayName("发布课程需要至少一个章节")
        void publishWithoutChapters() {
            service().createCourse("Intro To Math", bd("10"));
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service().publishCourse("course-1"));
            assertEquals(ErrorCode.COURSE_HAS_NO_CHAPTERS, exception.code());
            assertEquals(CourseStatus.DRAFT, courses.findById("course-1").orElseThrow().status());
        }

        @Test
        @DisplayName("发布成功后再次发布报错")
        void publishTwice() {
            ChapterRepository chapters = courseId -> List.of(new Chapter("ch1", courseId, "Intro", 1));
            CourseService service = service(chapters);
            service.createCourse("Intro To Math", bd("10"));
            assertEquals(CourseStatus.PUBLISHED, service.publishCourse("course-1").status());
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.publishCourse("course-1"));
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED, exception.code());
        }

        @Test
        @DisplayName("PricePolicy 折扣价与边界")
        void pricePolicyDiscounts() {
            assertEquals(bd("90.00"), pricePolicy.discountedPrice(bd("100.00"), 10));
            assertEquals(bd("100.00"), pricePolicy.discountedPrice(bd("100.00"), 0));
            assertEquals(bd("20.00"), pricePolicy.discountedPrice(bd("100.00"), 80));
            assertEquals(ErrorCode.INVALID_DISCOUNT, assertThrows(BusinessException.class,
                    () -> pricePolicy.discountedPrice(bd("100.00"), -1)).code());
            assertEquals("Discount percent must be between 0 and 80",
                    assertThrows(BusinessException.class,
                            () -> pricePolicy.discountedPrice(bd("100.00"), 81)).getMessage());
            assertEquals(ErrorCode.INVALID_PRICE, assertThrows(BusinessException.class,
                    () -> pricePolicy.discountedPrice(bd("-1"), 10)).code());
        }

        @Test
        @DisplayName("Course 记录构造校验与 publish 状态机")
        void courseRecordValidation() {
            assertEquals("id must not be null", assertThrows(NullPointerException.class,
                    () -> new Course(null, "Title", bd("1"), CourseStatus.DRAFT, T0)).getMessage());
            assertEquals("title must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> new Course("id", "   ", bd("1"), CourseStatus.DRAFT, T0)).getMessage());
            assertEquals("price must not be null", assertThrows(NullPointerException.class,
                    () -> new Course("id", "Title", null, CourseStatus.DRAFT, T0)).getMessage());
            assertEquals("status must not be null", assertThrows(NullPointerException.class,
                    () -> new Course("id", "Title", bd("1"), null, T0)).getMessage());
            assertEquals("createdAt must not be null", assertThrows(NullPointerException.class,
                    () -> new Course("id", "Title", bd("1"), CourseStatus.DRAFT, null)).getMessage());

            Course draft = new Course("id", "Title", bd("1.005"), CourseStatus.DRAFT, T0);
            assertEquals(bd("1.01"), draft.price());
            Course published = draft.publish();
            assertEquals(CourseStatus.PUBLISHED, published.status());
            assertEquals(ErrorCode.COURSE_ALREADY_PUBLISHED,
                    assertThrows(BusinessException.class, published::publish).code());
            Course archived = new Course("id", "Title", bd("1"), CourseStatus.ARCHIVED, T0);
            assertEquals("Only a draft course can be published",
                    assertThrows(BusinessException.class, archived::publish).getMessage());
        }

        @Test
        @DisplayName("目录为空时返回空章节列表")
        void emptyCatalog() {
            CourseService service = service();
            service.createCourse("Intro To Math", bd("10"));
            CatalogService catalogService = new CatalogService(service, emptyChapters,
                    new InMemoryVideoRepository());
            CourseCatalog catalog = catalogService.getCatalog("course-1");
            assertEquals("course-1", catalog.courseId());
            assertTrue(catalog.chapters().isEmpty());
        }

        @Test
        @DisplayName("目录按章节与视频排序并分组")
        void catalogOrdering() {
            CourseService service = service();
            service.createCourse("Intro To Math", bd("10"));
            ChapterRepository chapters = courseId -> List.of(
                    new Chapter("ch2", courseId, "Second", 2),
                    new Chapter("ch1", courseId, "First", 1));
            VideoRepository videos = chapterIds -> List.of(
                    new Video("v2", "ch1", "Beta", 90, 2),
                    new Video("v1", "ch1", "alpha", 60, 1),
                    new Video("v3", "ch2", "Gamma", 30, 1),
                    new Video("v4", "ch9", "Orphan", 10, 1));
            CatalogService catalogService = new CatalogService(service, chapters, videos);
            CourseCatalog catalog = catalogService.getCatalog("course-1");
            assertEquals(2, catalog.chapters().size());
            assertEquals("ch1", catalog.chapters().get(0).id());
            assertEquals("ch2", catalog.chapters().get(1).id());
            List<VideoView> first = catalog.chapters().get(0).videos();
            assertEquals(List.of("v1", "v2"), List.of(first.get(0).id(), first.get(1).id()));
            assertEquals(1, catalog.chapters().get(1).videos().size());
            ChapterView chapter = catalog.chapters().get(0);
            assertEquals("First", chapter.title());
            assertEquals(60, first.get(0).durationSeconds());
        }

        @Test
        @DisplayName("目录查询透传课程不存在异常")
        void catalogMissingCourse() {
            CatalogService catalogService = new CatalogService(service(), emptyChapters,
                    new InMemoryVideoRepository());
            assertEquals(ErrorCode.COURSE_NOT_FOUND, assertThrows(BusinessException.class,
                    () -> catalogService.getCatalog("nope")).code());
        }

        @Test
        @DisplayName("内存仓库与生成器适配器")
        void adapters() {
            Course course = new Course("c9", "Some Course", bd("5"), CourseStatus.DRAFT, T0);
            assertEquals(course, courses.save(course));
            assertEquals(Optional.of(course), courses.findById("c9"));
            assertEquals(Optional.empty(), courses.findById("unknown"));
            assertTrue(courses.existsByTitleIgnoreCase("some course"));
            assertFalse(courses.existsByTitleIgnoreCase("other"));

            assertTrue(new InMemoryChapterRepository().findByCourseId("c9").isEmpty());
            assertTrue(new InMemoryVideoRepository().findByChapterIds(Set.of("ch1")).isEmpty());

            UuidGenerator generator = new UuidGenerator();
            String first = generator.nextId();
            assertNotNull(first);
            assertTrue(first.length() > 10);
            assertNotEquals(first, generator.nextId());

            EmptyEnrollmentHistory empty = new EmptyEnrollmentHistory();
            assertFalse(empty.alreadyEnrolled("s", "c"));
            assertFalse(empty.completedCourse("s", "c"));
            assertEquals(0, empty.completionScore("s", "c"));
            assertFalse(empty.hasScheduleConflict("s", "k"));
            assertEquals(0, empty.activeCourseCountThisWeek("s"));
            assertEquals(0, empty.failedEnrollmentAttempts("s", "c"));
            assertFalse(empty.hasRefundAbuseFlag("s"));
        }

        @Test
        @DisplayName("ApiResponse 成功与失败包装")
        void apiResponseWrappers() {
            ApiResponse<String> success = ApiResponse.success("data");
            assertTrue(success.success());
            assertEquals("data", success.data());
            assertNull(success.error());

            ApiResponse<String> failure = ApiResponse.failure("CODE", "message");
            assertFalse(failure.success());
            assertNull(failure.data());
            assertEquals("CODE", failure.error().code());
            assertEquals("message", failure.error().message());
        }

        @Test
        @DisplayName("BusinessException 携带错误码")
        void businessExceptionCarriesCode() {
            BusinessException exception = new BusinessException(ErrorCode.INVALID_PRICE, "boom");
            assertEquals(ErrorCode.INVALID_PRICE, exception.code());
            assertEquals("boom", exception.getMessage());
            assertEquals("code must not be null", assertThrows(NullPointerException.class,
                    () -> new BusinessException(null, "boom")).getMessage());
        }

        @Test
        @DisplayName("REST 异常处理器映射全部错误码")
        void restExceptionHandlerMappings() {
            RestExceptionHandler handler = new RestExceptionHandler();
            assertEquals(HttpStatus.NOT_FOUND, statusOf(handler, ErrorCode.COURSE_NOT_FOUND));
            assertEquals(HttpStatus.CONFLICT, statusOf(handler, ErrorCode.DUPLICATE_COURSE_TITLE));
            assertEquals(HttpStatus.CONFLICT, statusOf(handler, ErrorCode.COURSE_ALREADY_PUBLISHED));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                    statusOf(handler, ErrorCode.COURSE_HAS_NO_CHAPTERS));
            assertEquals(HttpStatus.BAD_REQUEST, statusOf(handler, ErrorCode.INVALID_COURSE_TITLE));
            assertEquals(HttpStatus.BAD_REQUEST, statusOf(handler, ErrorCode.INVALID_PRICE));
            assertEquals(HttpStatus.BAD_REQUEST, statusOf(handler, ErrorCode.INVALID_DISCOUNT));

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                    new BusinessException(ErrorCode.COURSE_NOT_FOUND, "missing"));
            assertEquals("COURSE_NOT_FOUND", response.getBody().error().code());
            assertEquals("missing", response.getBody().error().message());
        }

        @Test
        @DisplayName("REST 校验异常处理器输出字段消息与兜底消息")
        void restValidationHandler() throws Exception {
            RestExceptionHandler handler = new RestExceptionHandler();
            Method method = CourseController.class.getMethod("create", CreateCourseRequest.class);
            MethodParameter parameter = new MethodParameter(method, 0);
            BeanPropertyBindingResult binding = new BeanPropertyBindingResult(
                    new CreateCourseRequest("ab", bd("1")), "createCourseRequest");
            binding.addError(new FieldError("createCourseRequest", "title",
                    "title length must be between 3 and 80"));
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleValidation(new MethodArgumentNotValidException(parameter, binding));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("VALIDATION_ERROR", response.getBody().error().code());
            assertEquals("title: title length must be between 3 and 80",
                    response.getBody().error().message());

            BeanPropertyBindingResult emptyBinding = new BeanPropertyBindingResult(
                    new CreateCourseRequest("ab", bd("1")), "createCourseRequest");
            ResponseEntity<ApiResponse<Void>> fallback = handler.handleValidation(
                    new MethodArgumentNotValidException(parameter, emptyBinding));
            assertEquals("Request validation failed", fallback.getBody().error().message());
        }

        @Test
        @DisplayName("CourseController 四个端点的响应")
        void courseControllerEndpoints() {
            ChapterRepository chapters = courseId -> List.of(new Chapter("ch1", courseId, "Intro", 1));
            CourseService service = service(chapters);
            CatalogService catalogService = new CatalogService(service, chapters,
                    new InMemoryVideoRepository());
            CourseController controller = new CourseController(service, catalogService);

            ResponseEntity<ApiResponse<Course>> created = controller.create(
                    new CreateCourseRequest("Intro To Math", bd("10.00")));
            assertEquals(HttpStatus.CREATED, created.getStatusCode());
            assertEquals("/api/courses/course-1", created.getHeaders().getLocation().toString());
            assertEquals("Intro To Math", created.getBody().data().title());

            assertEquals("course-1", controller.get("course-1").data().id());
            assertEquals(CourseStatus.PUBLISHED, controller.publish("course-1").data().status());
            assertEquals("course-1", controller.catalog("course-1").data().courseId());
        }

        @Test
        @DisplayName("时间配置 Bean 提供 UTC 时钟")
        void timeConfigurationBean() throws Exception {
            Method method = TimeConfiguration.class.getDeclaredMethod("systemClock");
            method.setAccessible(true);
            Clock clock = (Clock) method.invoke(new TimeConfiguration());
            assertNotNull(clock);
            assertEquals(ZoneOffset.UTC, clock.getZone());
        }

        private HttpStatus statusOf(RestExceptionHandler handler, ErrorCode code) {
            return (HttpStatus) handler.handleBusinessException(
                    new BusinessException(code, "x")).getStatusCode();
        }
    }

    // ============================================================ pricing

    /** PriceRequest 构造辅助。 */
    private static final class Price {
        String courseId = "c1";
        BigDecimal unitPrice = bd("100.00");
        int quantity = 1;
        MembershipLevel level = MembershipLevel.NONE;
        Coupon coupon = null;
        Promotion promotion = null;
        boolean firstPurchase = false;
        boolean nonprofit = false;
        boolean taxExempt = false;
        boolean instructorOwned = false;
        int loyaltyPoints = 0;
        int availablePoints = 0;
        String country = "CN";
        String currency = "CNY";

        PriceRequest build() {
            return new PriceRequest(courseId, unitPrice, quantity, level, coupon, promotion,
                    firstPurchase, nonprofit, taxExempt, instructorOwned, loyaltyPoints,
                    availablePoints, country, currency);
        }
    }

    private static Coupon coupon(CouponType type, String value) {
        return new Coupon("SAVE10", type, bd(value), null, null, null, 5, true, Set.of(),
                MembershipLevel.NONE, 0);
    }

    private static Promotion promotion(PromotionType type, String value) {
        return new Promotion("promo", type, bd(value), null, null, false, 1, null);
    }

    @Nested
    @DisplayName("AdvancedPricingService 定价")
    class Pricing {

        private final AdvancedPricingService service = new AdvancedPricingService(CLOCK);

        private PriceBreakdown price(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.calculate(request.build());
        }

        @Test
        @DisplayName("基础订单只加税，无折扣")
        void baseOrder() {
            PriceBreakdown breakdown = price(null);
            assertEquals(bd("100.00"), breakdown.subtotal());
            assertEquals(bd("0.00"), breakdown.membershipDiscount());
            assertEquals(bd("0.00"), breakdown.bulkDiscount());
            assertEquals(bd("0.00"), breakdown.promotionDiscount());
            assertEquals(bd("0.00"), breakdown.couponDiscount());
            assertEquals(bd("0.00"), breakdown.firstPurchaseDiscount());
            assertEquals(bd("0.00"), breakdown.nonprofitDiscount());
            assertEquals(bd("0.00"), breakdown.loyaltyDiscount());
            assertEquals(bd("6.00"), breakdown.tax());
            assertEquals(bd("106.00"), breakdown.total());
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        @DisplayName("会员等级折扣率")
        void membershipDiscounts() {
            assertEquals(bd("3.00"), price(p -> p.level = MembershipLevel.BASIC).membershipDiscount());
            assertEquals(bd("7.00"), price(p -> p.level = MembershipLevel.PLUS).membershipDiscount());
            assertEquals(bd("12.00"), price(p -> p.level = MembershipLevel.PREMIUM).membershipDiscount());
            assertEquals(bd("10.00"), price(p -> p.level = MembershipLevel.INSTRUCTOR).membershipDiscount());
            assertEquals(bd("100.00"), price(p -> {
                p.level = MembershipLevel.INSTRUCTOR;
                p.instructorOwned = true;
            }).membershipDiscount());
            assertEquals(bd("0.00"), price(p -> p.level = MembershipLevel.NONE).membershipDiscount());

            PriceBreakdown enterprise = price(p -> {
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 10;
                p.unitPrice = bd("10.00");
            });
            assertEquals(bd("18.00"), enterprise.membershipDiscount());
            PriceBreakdown small = price(p -> {
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 2;
                p.unitPrice = bd("50.00");
            });
            assertEquals(bd("8.00"), small.membershipDiscount());
            assertTrue(small.warnings()
                    .contains("Enterprise discount is reduced for orders below ten seats"));
            assertTrue(price(p -> p.level = MembershipLevel.PLUS).appliedRules()
                    .contains("MEMBERSHIP_PLUS"));
        }

        @Test
        @DisplayName("批量折扣档位")
        void bulkDiscountTiers() {
            assertEquals(bd("5.00"), price(p -> {
                p.quantity = 5;
                p.unitPrice = bd("20.00");
            }).bulkDiscount());
            assertEquals(bd("10.00"), price(p -> {
                p.quantity = 20;
                p.unitPrice = bd("5.00");
            }).bulkDiscount());
            assertEquals(bd("15.00"), price(p -> {
                p.quantity = 50;
                p.unitPrice = bd("2.00");
            }).bulkDiscount());
            assertEquals(bd("0.00"), price(p -> {
                p.quantity = 4;
                p.unitPrice = bd("25.00");
            }).bulkDiscount());
            assertTrue(price(p -> {
                p.quantity = 5;
                p.unitPrice = bd("20.00");
            }).appliedRules().contains("BULK_5"));
            assertTrue(price(p -> {
                p.quantity = 20;
                p.unitPrice = bd("5.00");
            }).appliedRules().contains("BULK_20"));
            assertTrue(price(p -> {
                p.quantity = 50;
                p.unitPrice = bd("2.00");
            }).appliedRules().contains("BULK_50"));
        }

        @Test
        @DisplayName("促销的合法性与三种类型（超大额度被 70% 上限回退）")
        void promotionRules() {
            PriceBreakdown percent = price(p -> p.promotion = promotion(PromotionType.PERCENT, "50"));
            assertEquals(bd("50.00"), percent.promotionDiscount());
            assertTrue(percent.appliedRules().contains("PROMOTION_PERCENT"));

            assertEquals(bd("70.00"),
                    price(p -> p.promotion = promotion(PromotionType.PERCENT, "90"))
                            .promotionDiscount());
            assertEquals(bd("30.00"),
                    price(p -> p.promotion = promotion(PromotionType.FIXED, "30"))
                            .promotionDiscount());
            assertEquals(bd("70.00"),
                    price(p -> p.promotion = promotion(PromotionType.FIXED, "300"))
                            .promotionDiscount());
            assertEquals(bd("12.00"), price(p -> {
                p.promotion = promotion(PromotionType.BUY_MORE_SAVE_MORE, "1");
                p.quantity = 25;
                p.unitPrice = bd("4.00");
            }).promotionDiscount());
            assertEquals(bd("8.00"), price(p -> {
                p.promotion = promotion(PromotionType.BUY_MORE_SAVE_MORE, "1");
                p.quantity = 10;
                p.unitPrice = bd("10.00");
            }).promotionDiscount());
            assertEquals(bd("4.00"), price(p -> {
                p.promotion = promotion(PromotionType.BUY_MORE_SAVE_MORE, "1");
                p.quantity = 5;
                p.unitPrice = bd("20.00");
            }).promotionDiscount());

            PriceBreakdown capped = price(p -> p.promotion = new Promotion("promo",
                    PromotionType.PERCENT, bd("50"), null, null, false, 1, bd("20.00")));
            assertEquals(bd("20.00"), capped.promotionDiscount());
            assertTrue(capped.warnings().contains("Promotion discount reached its configured cap"));
        }

        @Test
        @DisplayName("促销的非法配置只写警告不给折扣")
        void invalidPromotions() {
            assertEquals(bd("0.00"), price(p -> p.promotion = new Promotion(" ", PromotionType.PERCENT,
                    bd("50"), null, null, false, 1, null)).promotionDiscount());
            assertTrue(price(p -> p.promotion = new Promotion("promo", null, bd("50"), null, null,
                    false, 1, null)).warnings().contains("Promotion configuration is incomplete"));
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    null, null, null, false, 1, null)).warnings()
                    .contains("Promotion configuration is incomplete"));
            assertTrue(price(p -> p.promotion = promotion(PromotionType.PERCENT, "0"))
                    .warnings().contains("Promotion value must be positive"));
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    bd("50"), LocalDate.now(CLOCK).plusDays(1), null, false, 1, null))
                    .warnings().contains("Promotion has not started"));
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    bd("50"), null, LocalDate.now(CLOCK).minusDays(1), false, 1, null))
                    .warnings().contains("Promotion has expired"));
            // 促销区间倒挂时 today 必然 isAfter(endsOn)，先被 "Promotion has expired" 命中，
            // 因此 "Promotion date range is invalid" 分支不可达（见等价/不可达清单）。
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    bd("50"), LocalDate.now(CLOCK), LocalDate.now(CLOCK).minusDays(2), false, 1, null))
                    .warnings().contains("Promotion has expired"));
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    bd("50"), null, null, true, 1, null))
                    .warnings().contains("Promotion is available to members only"));
            assertTrue(price(p -> p.promotion = new Promotion("promo", PromotionType.PERCENT,
                    bd("50"), null, null, false, 3, null))
                    .warnings().contains("Order quantity is below the promotion minimum"));
        }

        @Test
        @DisplayName("优惠券的合法性、类型与现金返还")
        void couponRules() {
            assertEquals(bd("50.00"),
                    price(p -> p.coupon = coupon(CouponType.PERCENT, "50")).couponDiscount());
            assertEquals(bd("70.00"),
                    price(p -> p.coupon = coupon(CouponType.PERCENT, "90")).couponDiscount());
            assertEquals(bd("30.00"),
                    price(p -> p.coupon = coupon(CouponType.FIXED, "30")).couponDiscount());
            assertEquals(bd("30.00"),
                    price(p -> p.coupon = coupon(CouponType.FULL_REDUCTION, "30")).couponDiscount());
            PriceBreakdown cashback = price(p -> p.coupon = coupon(CouponType.CASHBACK, "10"));
            assertEquals(bd("0.00"), cashback.couponDiscount());
            assertTrue(cashback.appliedRules().contains("COUPON_CASHBACK_FUTURE"));
            assertTrue(cashback.warnings()
                    .contains("Cashback is credited after payment and does not reduce this order"));

            assertTrue(price(p -> p.coupon = new Coupon(" ", CouponType.PERCENT, bd("50"), null,
                    null, null, 5, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .contains("Coupon code is blank"));
            assertTrue(price(p -> p.coupon = new Coupon("c", null, bd("50"), null, null, null,
                    5, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .contains("Coupon configuration is incomplete"));
            assertTrue(price(p -> p.coupon = coupon(CouponType.PERCENT, "0")).warnings()
                    .contains("Coupon value must be positive"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    null, null, 0, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .contains("Coupon has no remaining uses"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    LocalDate.now(CLOCK).plusDays(1), null, 5, true, Set.of(),
                    MembershipLevel.NONE, 0)).warnings().contains("Coupon is not active yet"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    null, LocalDate.now(CLOCK).minusDays(1), 5, true, Set.of(),
                    MembershipLevel.NONE, 0)).warnings().contains("Coupon has expired"));
            // 同上：倒挂区间先被 expired 命中，"Coupon date range is invalid" 不可达。
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    LocalDate.now(CLOCK), LocalDate.now(CLOCK).minusDays(2), 5, true, Set.of(),
                    MembershipLevel.NONE, 0)).warnings().contains("Coupon has expired"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"),
                    bd("200"), null, null, 5, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .contains("Order does not meet the coupon minimum subtotal"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    null, null, 5, true, Set.of("other"), MembershipLevel.NONE, 0)).warnings()
                    .contains("Coupon is not valid for this course"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    null, null, 5, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .isEmpty());
            assertTrue(price(p -> {
                p.quantity = 5;
                p.unitPrice = bd("20.00");
                p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null, null, null, 5,
                        true, Set.of(), MembershipLevel.NONE, 2);
            }).warnings().contains("Order quantity exceeds the coupon limit"));
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null,
                    null, null, 5, true, Set.of(), MembershipLevel.PLUS, 0)).warnings()
                    .contains("Membership level is too low for this coupon"));
        }

        @Test
        @DisplayName("促销与不可叠加优惠券二选一")
        void nonStackableCouponResolution() {
            PriceBreakdown promotionWins = price(p -> {
                p.promotion = promotion(PromotionType.FIXED, "40");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("40.00"), promotionWins.promotionDiscount());
            assertEquals(bd("0.00"), promotionWins.couponDiscount());
            assertTrue(promotionWins.warnings()
                    .contains("The coupon was ignored because the promotion gives a greater discount"));

            PriceBreakdown couponWins = price(p -> {
                p.promotion = promotion(PromotionType.FIXED, "10");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("40"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("40.00"), couponWins.couponDiscount());
            assertEquals(bd("0.00"), couponWins.promotionDiscount());
            assertTrue(couponWins.warnings()
                    .contains("The promotion was ignored because the coupon gives a greater discount"));
        }

        @Test
        @DisplayName("折扣总和受 70% 上限约束并按顺序回退")
        void discountCapTrimming() {
            PriceBreakdown breakdown = price(p -> {
                p.level = MembershipLevel.PREMIUM;
                p.quantity = 50;
                p.unitPrice = bd("2.00");
                p.promotion = promotion(PromotionType.PERCENT, "80");
                p.nonprofit = true;
            });
            assertTrue(breakdown.warnings()
                    .contains("Combined discounts were limited by the maximum discount policy"));
            assertTrue(breakdown.appliedRules().contains("DISCOUNT_CAP"));
            BigDecimal sum = breakdown.membershipDiscount()
                    .add(breakdown.bulkDiscount())
                    .add(breakdown.promotionDiscount())
                    .add(breakdown.couponDiscount())
                    .add(breakdown.firstPurchaseDiscount())
                    .add(breakdown.nonprofitDiscount());
            assertEquals(0, sum.compareTo(bd("70.00")));
        }

        @Test
        @DisplayName("教师自有课程的折扣上限放宽到全额")
        void instructorOwnedCourseCapIsSubtotal() {
            PriceBreakdown relaxed = price(p -> {
                p.level = MembershipLevel.INSTRUCTOR;
                p.instructorOwned = true;
            });
            assertEquals(bd("100.00"), relaxed.membershipDiscount());
            assertFalse(relaxed.appliedRules().contains("DISCOUNT_CAP"));
            assertEquals(bd("10.00"), price(p -> p.level = MembershipLevel.INSTRUCTOR)
                    .membershipDiscount());
        }

        @Test
        @DisplayName("首次购买折扣的四个分支")
        void firstPurchaseRules() {
            assertEquals(bd("8.00"), price(p -> p.firstPurchase = true).firstPurchaseDiscount());
            assertEquals(0, price(p -> {
                p.firstPurchase = true;
                p.unitPrice = bd("2000.00");
            }).firstPurchaseDiscount().compareTo(bd("100.00")));
            assertTrue(price(p -> {
                p.firstPurchase = true;
                p.instructorOwned = true;
            }).warnings().contains("First purchase discount is not applied to instructor-owned courses"));
            assertTrue(price(p -> {
                p.firstPurchase = true;
                p.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            }).warnings().contains("First purchase discount is blocked by a non-stackable coupon"));
            assertTrue(price(p -> p.firstPurchase = true).appliedRules().contains("FIRST_PURCHASE"));
        }

        @Test
        @DisplayName("公益折扣按企业/普通两档")
        void nonprofitRules() {
            PriceBreakdown normal = price(p -> p.nonprofit = true);
            assertEquals(bd("5.00"), normal.nonprofitDiscount());
            assertTrue(normal.appliedRules().contains("NONPROFIT"));
            PriceBreakdown enterprise = price(p -> {
                p.nonprofit = true;
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 10;
                p.unitPrice = bd("10.00");
            });
            assertEquals(bd("8.00"), enterprise.nonprofitDiscount());
            assertTrue(enterprise.appliedRules().contains("NONPROFIT_ENTERPRISE"));
        }

        @Test
        @DisplayName("积分抵扣的边界")
        void loyaltyRules() {
            assertEquals(bd("0.00"), price(p -> p.loyaltyPoints = 0).loyaltyDiscount());
            assertEquals(bd("0.00"), price(p -> {
                p.loyaltyPoints = 500;
                p.availablePoints = 500;
                p.unitPrice = BigDecimal.ZERO;
            }).loyaltyDiscount());
            assertTrue(price(p -> {
                p.loyaltyPoints = 50;
                p.availablePoints = 50;
            }).warnings().contains("At least 100 loyalty points are required"));
            assertTrue(price(p -> {
                p.loyaltyPoints = 500;
                p.availablePoints = 200;
            }).warnings().contains("Requested loyalty points were reduced to the available balance"));
            PriceBreakdown capped = price(p -> {
                p.loyaltyPoints = 5000;
                p.availablePoints = 5000;
            });
            assertEquals(bd("20.00"), capped.loyaltyDiscount());
            assertTrue(capped.warnings().contains("Loyalty discount was limited to twenty percent"));
            PriceBreakdown used = price(p -> {
                p.loyaltyPoints = 500;
                p.availablePoints = 500;
            });
            assertEquals(bd("5.00"), used.loyaltyDiscount());
            assertTrue(used.appliedRules().contains("LOYALTY_POINTS"));
        }

        @Test
        @DisplayName("税率为国家/地区与免税开关")
        void taxRules() {
            assertEquals(bd("0.00"), price(p -> p.taxExempt = true).tax());
            assertTrue(price(p -> p.taxExempt = true).appliedRules().contains("TAX_EXEMPT"));
            assertTrue(price(p -> p.unitPrice = BigDecimal.ZERO).appliedRules()
                    .contains("FREE_ORDER"));
            assertTrue(price(p -> p.unitPrice = BigDecimal.ZERO).warnings()
                    .contains("No discount value is generated for a free course"));
            assertEquals(bd("7.50"), price(p -> {
                p.country = "us";
                p.currency = "USD";
            }).tax());
            assertEquals(bd("19.00"), price(p -> {
                p.country = "DE";
                p.currency = "EUR";
            }).tax());
            assertEquals(bd("9.00"), price(p -> {
                p.country = "SG";
                p.currency = "SGD";
            }).tax());
            assertEquals(bd("20.00"), price(p -> {
                p.country = "GB";
                p.currency = "GBP";
            }).tax());
            PriceBreakdown unknown = price(p -> p.country = "XX");
            assertEquals(bd("0.00"), unknown.tax());
            assertTrue(unknown.warnings().contains("No tax rule is configured for country XX"));
            PriceBreakdown nonprofit = price(p -> p.nonprofit = true);
            assertEquals(bd("2.85"), nonprofit.tax());
            assertTrue(nonprofit.appliedRules().contains("NONPROFIT_TAX_REDUCTION"));
            assertTrue(price(p -> p.currency = "USD").warnings()
                    .contains("Chinese tax is calculated for a non-CNY order"));
            assertTrue(price(p -> {
                p.country = "US";
                p.currency = "CNY";
            }).warnings().contains("United States tax is calculated for a non-USD order"));
            assertTrue(price(p -> {
                p.country = "DE";
                p.currency = "CNY";
            }).warnings().contains("German tax is calculated for a non-EUR order"));
        }

        @Test
        @DisplayName("数量上限与总价为零的规则")
        void quantityAndZeroTotalRules() {
            PriceBreakdown bulk = price(p -> {
                p.quantity = 100;
                p.unitPrice = bd("1.00");
            });
            assertTrue(bulk.warnings().contains("The order reached the maximum allowed quantity"));
            assertEquals(bd("100.00"), bulk.subtotal());
        }

        @Test
        @DisplayName("请求校验")
        void validateRequest() {
            assertEquals("request must not be null", assertThrows(NullPointerException.class,
                    () -> service.calculate(null)).getMessage());
            assertEquals("courseId must not be blank", invalid(p -> p.courseId = " "));
            assertEquals(ErrorCode.INVALID_PRICE, businessInvalid(p -> p.unitPrice = null));
            assertEquals(ErrorCode.INVALID_PRICE, businessInvalid(p -> p.unitPrice = bd("-1")));
            assertEquals(ErrorCode.INVALID_PRICE, businessInvalid(p -> p.unitPrice = bd("100000")));
            Price negativePrice = new Price();
            negativePrice.unitPrice = bd("-1");
            assertEquals("Unit price must be between 0 and 99999.99",
                    assertThrows(BusinessException.class,
                            () -> service.calculate(negativePrice.build())).getMessage());
            assertEquals("quantity must be positive", invalid(p -> p.quantity = 0));
            assertEquals("quantity must not exceed 100", invalid(p -> p.quantity = 101));
            assertEquals("membershipLevel must not be null", invalid(p -> p.level = null));
            assertEquals("currency must not be blank", invalid(p -> p.currency = " "));
            assertEquals("unsupported currency: XYZ", invalid(p -> p.currency = "XYZ"));
            assertEquals("countryCode must not be blank", invalid(p -> p.country = " "));
            assertEquals("loyaltyPointsToUse must not be negative", invalid(p -> p.loyaltyPoints = -1));
            assertEquals("availableLoyaltyPoints must not be negative",
                    invalid(p -> p.availablePoints = -1));
        }

        private ErrorCode businessInvalid(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            mutation.accept(request);
            return assertThrows(BusinessException.class,
                    () -> service.calculate(request.build())).code();
        }

        private String invalid(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.calculate(request.build())).getMessage();
        }
    }

    // ============================================================ refund

    /** PurchaseSnapshot 构造辅助。 */
    private static final class Pur {
        String purchaseId = "p1";
        String studentId = "s1";
        String courseId = "c1";
        BigDecimal paidAmount = bd("100.00");
        BigDecimal taxAmount = bd("10.00");
        BigDecimal discountAmount = bd("5.00");
        Instant paidAt = T0.minusSeconds(3600);
        PaymentMethod method = PaymentMethod.CARD;
        int progress = 0;
        int downloads = 0;
        boolean certificate = false;
        int attendedSessions = 0;
        int totalSessions = 0;
        boolean gift = false;
        boolean couponUsed = false;
        int loyaltyUsed = 0;
        boolean chargeback = false;
        boolean refunded = false;
        boolean instructorCancelled = false;
        boolean courseRemoved = false;
        Instant accessExpiresAt = null;

        PurchaseSnapshot build() {
            return new PurchaseSnapshot(purchaseId, studentId, courseId, paidAmount, taxAmount,
                    discountAmount, paidAt, method, progress, downloads, certificate,
                    attendedSessions, totalSessions, gift, couponUsed, loyaltyUsed, chargeback,
                    refunded, instructorCancelled, courseRemoved, accessExpiresAt);
        }
    }

    /** RefundRequest 构造辅助。 */
    private static final class Ref {
        PurchaseSnapshot purchase = new Pur().build();
        RefundReason reason = RefundReason.ACCIDENTAL_PURCHASE;
        boolean fullRefund = true;
        int requestedPercentage = 0;
        boolean evidence = false;
        boolean technicalConfirmed = false;
        boolean contentUpheld = false;
        boolean medicalVerified = false;
        boolean duplicateConfirmed = false;
        boolean bankDetails = false;
        boolean fraud = false;
        boolean adminOverride = false;

        Ref with(Pur purchase) { this.purchase = purchase.build(); return this; }

        RefundRequest build() {
            return new RefundRequest(purchase, reason, fullRefund, requestedPercentage, evidence,
                    technicalConfirmed, contentUpheld, medicalVerified, duplicateConfirmed,
                    bankDetails, fraud, adminOverride);
        }
    }

    @Nested
    @DisplayName("RefundPolicyService 退款")
    class Refund {

        private final RefundPolicyService service = new RefundPolicyService(CLOCK);

        private RefundDecision refund(java.util.function.Consumer<Pur> purchaseMutation,
                                      java.util.function.Consumer<Ref> requestMutation) {
            Pur purchase = new Pur();
            if (purchaseMutation != null) {
                purchaseMutation.accept(purchase);
            }
            Ref request = new Ref();
            request.purchase = purchase.build();
            if (requestMutation != null) {
                requestMutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        private RefundDecision refund(java.util.function.Consumer<Pur> purchaseMutation) {
            return refund(purchaseMutation, null);
        }

        @Test
        @DisplayName("两小时内的冷静期全额退款且免手续费")
        void coolingOffFullRefund() {
            RefundDecision decision = refund(null);
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals(bd("100.00"), decision.refundableAmount());
            assertEquals(bd("0.00"), decision.processingFee());
            assertEquals(0, decision.loyaltyPointsReturned());
            assertEquals("ORIGINAL_PAYMENT_METHOD", decision.refundRoute());
            assertEquals(List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), decision.reasons());
            assertTrue(decision.warnings().isEmpty());
        }

        @Test
        @DisplayName("未来购买时间、已退款、拒付与欺诈")
        void rejectedAndManualPaths() {
            RefundDecision future = refund(p -> p.paidAt = T0.plusSeconds(3600));
            assertFalse(future.approved());
            assertEquals("NONE", future.refundRoute());
            assertEquals(List.of("PURCHASE_DATE_IN_FUTURE"), future.reasons());

            assertEquals(List.of("PURCHASE_ALREADY_REFUNDED"), refund(p -> p.refunded = true).reasons());

            RefundDecision chargeback = refund(p -> p.chargeback = true);
            assertTrue(chargeback.manualReview());
            assertFalse(chargeback.approved());
            assertEquals("MANUAL_REVIEW", chargeback.refundRoute());
            assertEquals(List.of("OPEN_CHARGEBACK"), chargeback.reasons());

            assertEquals(List.of("FRAUD_REVIEW_REQUIRED"), refund(null, r -> r.fraud = true).reasons());
            assertEquals(List.of("FRAUD_REVIEW_REQUIRED"),
                    refund(null, r -> r.reason = RefundReason.FRAUD).reasons());
        }

        @Test
        @DisplayName("访问过期只写警告")
        void expiredAccessWarning() {
            RefundDecision decision = refund(p -> p.accessExpiresAt = T0.minusSeconds(60));
            assertTrue(decision.warnings().contains("COURSE_ACCESS_EXPIRED"));
            assertTrue(decision.approved());
        }

        @Test
        @DisplayName("提供方取消课程：全额且免手续费")
        void providerCancelled() {
            RefundDecision decision = refund(p -> p.instructorCancelled = true);
            assertTrue(decision.reasons().contains("PROVIDER_CANCELLED_COURSE"));
            assertEquals(bd("100.00"), decision.refundableAmount());
            assertEquals(bd("0.00"), decision.processingFee());
            assertTrue(refund(p -> p.courseRemoved = true).reasons()
                    .contains("PROVIDER_CANCELLED_COURSE"));
            assertTrue(refund(null, r -> r.reason = RefundReason.COURSE_CANCELLED).reasons()
                    .contains("PROVIDER_CANCELLED_COURSE"));
        }

        @Test
        @DisplayName("重复支付确认与凭证")
        void duplicatePayment() {
            RefundDecision confirmed = refund(null, r -> {
                r.reason = RefundReason.DUPLICATE_PAYMENT;
                r.duplicateConfirmed = true;
            });
            assertEquals(bd("100.00"), confirmed.refundableAmount());
            assertTrue(confirmed.reasons().contains("DUPLICATE_PAYMENT_CONFIRMED"));

            RefundDecision evidence = refund(null, r -> {
                r.reason = RefundReason.DUPLICATE_PAYMENT;
                r.evidence = true;
            });
            assertEquals(bd("50.00"), evidence.refundableAmount());
            assertTrue(evidence.reasons().contains("DUPLICATE_PAYMENT_REQUIRES_REVIEW"));

            RefundDecision none = refund(null, r -> r.reason = RefundReason.DUPLICATE_PAYMENT);
            assertEquals(List.of("DUPLICATE_PAYMENT_NOT_CONFIRMED"), none.reasons());
        }

        @Test
        @DisplayName("技术问题：确认后按进度分档")
        void technicalIssue() {
            RefundDecision confirmed = refund(null, r -> {
                r.reason = RefundReason.TECHNICAL_ISSUE;
                r.technicalConfirmed = true;
            });
            assertEquals(bd("100.00"), confirmed.refundableAmount());
            assertTrue(confirmed.reasons().contains("CONFIRMED_TECHNICAL_ISSUE"));

            RefundDecision late = refund(p -> p.progress = 80, r -> {
                r.reason = RefundReason.TECHNICAL_ISSUE;
                r.technicalConfirmed = true;
            });
            assertEquals(bd("75.00"), late.refundableAmount());

            RefundDecision noEvidence = refund(null, r -> r.reason = RefundReason.TECHNICAL_ISSUE);
            assertTrue(noEvidence.reasons().contains("TECHNICAL_EVIDENCE_REQUIRED"));
            assertFalse(noEvidence.approved());

            RefundDecision evidence = refund(null, r -> {
                r.reason = RefundReason.TECHNICAL_ISSUE;
                r.evidence = true;
            });
            assertTrue(evidence.warnings().contains("UNCONFIRMED_TECHNICAL_ISSUE"));
            // 未确认的技术问题不会立即拒绝，会继续落入冷静期规则。
            assertTrue(evidence.approved());
            assertTrue(evidence.reasons().contains("COOLING_OFF_PERIOD"));
        }

        @Test
        @DisplayName("医疗原因四分支")
        void medicalReason() {
            RefundDecision verified = refund(null, r -> {
                r.reason = RefundReason.MEDICAL;
                r.medicalVerified = true;
            });
            assertEquals(bd("100.00"), verified.refundableAmount());

            RefundDecision highProgress = refund(p -> p.progress = 90, r -> {
                r.reason = RefundReason.MEDICAL;
                r.medicalVerified = true;
            });
            assertEquals(bd("40.00"), highProgress.refundableAmount());

            RefundDecision evidence = refund(null, r -> {
                r.reason = RefundReason.MEDICAL;
                r.evidence = true;
            });
            assertEquals(bd("50.00"), evidence.refundableAmount());
            assertTrue(evidence.reasons().contains("MEDICAL_DOCUMENT_REQUIRES_REVIEW"));

            assertTrue(refund(null, r -> r.reason = RefundReason.MEDICAL).reasons()
                    .contains("MEDICAL_DOCUMENT_REQUIRED"));
        }

        @Test
        @DisplayName("内容质量投诉按进度分档")
        void contentQuality() {
            RefundDecision early = refund(null, r -> {
                r.reason = RefundReason.CONTENT_QUALITY;
                r.contentUpheld = true;
            });
            assertEquals(bd("100.00"), early.refundableAmount());

            RefundDecision late = refund(p -> p.progress = 70, r -> {
                r.reason = RefundReason.CONTENT_QUALITY;
                r.contentUpheld = true;
            });
            assertEquals(bd("60.00"), late.refundableAmount());
        }

        @Test
        @DisplayName("证书已发、进度过高、资源下载过多都会拒绝")
        void blockingConditions() {
            assertEquals(List.of("CERTIFICATE_ALREADY_ISSUED"),
                    refund(p -> p.certificate = true).reasons());
            assertEquals(List.of("COURSE_SUBSTANTIALLY_COMPLETED"),
                    refund(p -> p.progress = 80).reasons());
            assertEquals(List.of("TOO_MANY_RESOURCES_DOWNLOADED"),
                    refund(p -> p.downloads = 10).reasons());
        }

        @Test
        @DisplayName("阶梯式时间窗退款比例")
        void graduatedWindows() {
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(3600), null).reasons()
                    .contains("COOLING_OFF_PERIOD"));
            assertTrue(refund(p -> {
                p.paidAt = T0.minusSeconds(20 * 3600);
                p.progress = 5;
            }).reasons().contains("ONE_DAY_LOW_USAGE"));
            assertTrue(refund(p -> {
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.progress = 10;
            }).reasons().contains("SEVEN_DAY_GUARANTEE"));
            RefundDecision sevenDayPartial = refund(p -> {
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.progress = 20;
            });
            assertEquals(bd("67.90"), sevenDayPartial.refundableAmount());
            assertTrue(sevenDayPartial.reasons().contains("SEVEN_DAY_PARTIAL_REFUND"));
            RefundDecision fourteenDay = refund(p -> {
                p.paidAt = T0.minusSeconds(200 * 3600);
                p.progress = 20;
            });
            assertEquals(bd("48.50"), fourteenDay.refundableAmount());
            assertTrue(fourteenDay.reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));
            RefundDecision thirtyDay = refund(p -> {
                p.paidAt = T0.minusSeconds(400 * 3600);
                p.progress = 10;
            });
            assertEquals(bd("24.25"), thirtyDay.refundableAmount());
            assertTrue(thirtyDay.reasons().contains("THIRTY_DAY_EXCEPTION"));
            assertTrue(refund(p -> {
                p.paidAt = T0.minusSeconds(800 * 3600);
                p.downloads = 1;
            }).reasons().contains("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"));
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(800 * 3600), r -> {
                r.reason = RefundReason.OTHER;
                r.evidence = false;
            }).reasons().contains("OTHER_REASON_REQUIRES_EVIDENCE"));
            assertEquals(List.of("REFUND_POLICY_NOT_SATISFIED"),
                    refund(p -> p.paidAt = T0.minusSeconds(800 * 3600), r -> {
                        r.reason = RefundReason.OTHER;
                        r.evidence = true;
                    }).reasons());
        }

        @Test
        @DisplayName("管理员强制退款与百分比换算")
        void adminOverrideRules() {
            RefundDecision full = refund(p -> p.paidAt = T0.minusSeconds(800 * 3600), r -> {
                r.reason = RefundReason.OTHER;
                r.adminOverride = true;
            });
            assertTrue(full.warnings().contains("ADMIN_OVERRIDE_APPLIED"));
            assertEquals(bd("100.00"), full.refundableAmount());
            assertEquals(bd("0.00"), full.processingFee());

            RefundDecision half = refund(p -> p.progress = 100, r -> {
                r.reason = RefundReason.OTHER;
                r.evidence = true;
                r.adminOverride = true;
                r.fullRefund = false;
                r.requestedPercentage = 0;
            });
            assertTrue(half.warnings().contains("ADMIN_OVERRIDE_APPLIED"));
            assertEquals(bd("50.00"), half.refundableAmount());

            // requestedPercentage=100 时会被强制退款分支抬到 100%，不再触发“降级到策略上限”。
            RefundDecision cappedPercentage = refund(p -> p.progress = 100, r -> {
                r.reason = RefundReason.OTHER;
                r.evidence = true;
                r.adminOverride = true;
                r.fullRefund = false;
                r.requestedPercentage = 100;
            });
            assertEquals(bd("100.00"), cappedPercentage.refundableAmount());
            assertFalse(cappedPercentage.warnings()
                    .contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));

            RefundDecision reduced = refund(p -> {
                p.progress = 20;
                p.paidAt = T0.minusSeconds(100 * 3600);
            }, r -> r.fullRefund = true);
            assertTrue(reduced.warnings()
                    .contains("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
            assertEquals(bd("67.90"), reduced.refundableAmount());
        }

        @Test
        @DisplayName("手续费按支付方式与下载/直播行为叠加")
        void processingFees() {
            assertEquals(bd("1.00"), refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
            }).processingFee());

            RefundDecision withDownloads = refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.downloads = 1;
            });
            assertEquals(bd("3.00"), withDownloads.processingFee());
            assertTrue(withDownloads.warnings().contains("RESOURCE_DOWNLOAD_FEE_APPLIED"));

            RefundDecision liveHalf = refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.attendedSessions = 1;
                p.totalSessions = 2;
            });
            assertEquals(bd("11.00"), liveHalf.processingFee());
            assertTrue(liveHalf.warnings().contains("LIVE_SESSION_ATTENDANCE_FEE_APPLIED"));

            RefundDecision livePartial = refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.attendedSessions = 1;
                p.totalSessions = 4;
            });
            assertEquals(bd("6.00"), livePartial.processingFee());
            assertTrue(livePartial.warnings().contains("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));

            assertEquals(bd("0.00"), refund(p -> {
                p.method = PaymentMethod.WALLET;
                p.paidAt = T0.minusSeconds(100 * 3600);
            }).processingFee());
            assertEquals(bd("2.00"), refund(p -> {
                p.method = PaymentMethod.BANK_TRANSFER;
                p.paidAt = T0.minusSeconds(100 * 3600);
            }).processingFee());
        }

        @Test
        @DisplayName("优惠券金额不退还")
        void couponAdjustment() {
            RefundDecision adjusted = refund(p -> p.couponUsed = true);
            assertEquals(bd("95.00"), adjusted.refundableAmount());
            assertTrue(adjusted.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));

            RefundDecision consumed = refund(p -> {
                p.couponUsed = true;
                p.discountAmount = bd("200.00");
            });
            assertTrue(consumed.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
            assertFalse(consumed.approved());
            assertEquals(List.of("NO_REFUNDABLE_VALUE"), consumed.reasons());

            RefundDecision providerFault = refund(p -> {
                p.couponUsed = true;
                p.courseRemoved = true;
            });
            assertEquals(bd("100.00"), providerFault.refundableAmount());
        }

        @Test
        @DisplayName("积分按比例退回并受优惠券折半")
        void loyaltyPointsReturned() {
            RefundDecision full = refund(p -> p.loyaltyUsed = 1000);
            assertEquals(1000, full.loyaltyPointsReturned());
            assertTrue(full.warnings().isEmpty());

            RefundDecision partial = refund(p -> {
                p.loyaltyUsed = 1000;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.progress = 20;
            });
            assertEquals(700, partial.loyaltyPointsReturned());
            assertTrue(partial.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));

            RefundDecision coupon = refund(p -> {
                p.loyaltyUsed = 1000;
                p.couponUsed = true;
            });
            assertEquals(500, coupon.loyaltyPointsReturned());
            assertTrue(coupon.warnings().contains("POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));

            assertEquals(0, refund(p -> p.loyaltyUsed = 1000, r -> r.fraud = true)
                    .loyaltyPointsReturned());
        }

        @Test
        @DisplayName("小额退款转入钱包或补进手续费")
        void minimumPayout() {
            RefundDecision wallet = refund(p -> {
                p.paidAmount = bd("0.50");
                p.method = PaymentMethod.WALLET;
            });
            assertTrue(wallet.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
            assertEquals(bd("0.50"), wallet.refundableAmount());

            RefundDecision card = refund(p -> p.paidAmount = bd("0.50"));
            assertTrue(card.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));
            assertEquals(bd("0.00"), card.processingFee());
            assertFalse(card.approved());
            assertEquals(List.of("NO_REFUNDABLE_VALUE"), card.reasons());
        }

        @Test
        @DisplayName("退款渠道判定")
        void refundRoutes() {
            assertEquals("WALLET_CREDIT", refund(p -> p.gift = true).refundRoute());
            assertTrue(refund(p -> p.gift = true).warnings()
                    .contains("GIFT_REFUND_ISSUED_AS_CREDIT"));
            assertEquals("WALLET_CREDIT", refund(p -> p.method = PaymentMethod.GIFT).refundRoute());
            assertEquals("WALLET_CREDIT", refund(p -> p.method = PaymentMethod.WALLET).refundRoute());
            assertEquals("ORIGINAL_PAYMENT_METHOD",
                    refund(p -> p.method = PaymentMethod.WECHAT).refundRoute());

            RefundDecision transfer = refund(p -> {
                p.method = PaymentMethod.BANK_TRANSFER;
                p.paidAt = T0.minusSeconds(100 * 3600);
            }, r -> r.bankDetails = true);
            assertEquals("BANK_TRANSFER", transfer.refundRoute());
            assertTrue(transfer.approved());

            RefundDecision noDetails = refund(p -> {
                p.method = PaymentMethod.BANK_TRANSFER;
                p.paidAt = T0.minusSeconds(100 * 3600);
            });
            assertEquals("MANUAL_REVIEW", noDetails.refundRoute());
            assertTrue(noDetails.warnings().contains("BANK_DETAILS_REQUIRED"));
            assertFalse(noDetails.approved());
            assertTrue(noDetails.manualReview());

            RefundDecision crypto = refund(p -> {
                p.method = PaymentMethod.CRYPTO;
                p.paidAt = T0.minusSeconds(100 * 3600);
            });
            assertEquals("MANUAL_REVIEW", crypto.refundRoute());
            assertTrue(crypto.warnings().contains("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW"));
        }

        @Test
        @DisplayName("请求校验")
        void validation() {
            assertEquals("request must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(null)).getMessage());

            Ref noPurchase = new Ref();
            noPurchase.purchase = null;
            assertEquals("purchase must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(noPurchase.build())).getMessage());

            Ref noReason = new Ref();
            noReason.reason = null;
            assertEquals("reason must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(noReason.build())).getMessage());

            assertEquals("purchaseId must not be blank", invalid(p -> p.purchaseId = " "));
            assertEquals("studentId must not be blank", invalid(p -> p.studentId = null));
            assertEquals("courseId must not be blank", invalid(p -> p.courseId = " "));
            assertEquals("paidAmount must not be negative", invalid(p -> p.paidAmount = bd("-1")));
            assertEquals("paidAmount must not be negative", invalid(p -> p.paidAmount = null));
            assertEquals("taxAmount must not be negative", invalid(p -> p.taxAmount = bd("-1")));
            assertEquals("taxAmount must not be negative", invalid(p -> p.taxAmount = null));
            assertEquals("discountAmount must not be negative", invalid(p -> p.discountAmount = bd("-1")));
            assertEquals("discountAmount must not be negative", invalid(p -> p.discountAmount = null));
            assertEquals("paidAt must not be null", invalid(p -> p.paidAt = null));
            assertEquals("paymentMethod must not be null", invalid(p -> p.method = null));
            assertEquals("progressPercent must be between 0 and 100", invalid(p -> p.progress = -1));
            assertEquals("progressPercent must be between 0 and 100", invalid(p -> p.progress = 101));
            assertEquals("downloadedResources must not be negative", invalid(p -> p.downloads = -1));
            assertEquals("live session counts must not be negative",
                    invalid(p -> p.attendedSessions = -1));
            assertEquals("live session counts must not be negative", invalid(p -> p.totalSessions = -1));
            assertEquals("attended sessions must not exceed total sessions", invalid(p -> {
                p.attendedSessions = 2;
                p.totalSessions = 1;
            }));
            assertEquals("loyaltyPointsUsed must not be negative", invalid(p -> p.loyaltyUsed = -1));
            assertEquals("requestedPercentage must be between 0 and 100",
                    invalidRequest(r -> r.requestedPercentage = -1));
            assertEquals("requestedPercentage must be between 0 and 100",
                    invalidRequest(r -> r.requestedPercentage = 101));
        }

        private String invalid(java.util.function.Consumer<Pur> mutation) {
            Pur purchase = new Pur();
            mutation.accept(purchase);
            Ref request = new Ref();
            request.purchase = purchase.build();
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(request.build())).getMessage();
        }

        private String invalidRequest(java.util.function.Consumer<Ref> mutation) {
            Ref request = new Ref();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(request.build())).getMessage();
        }
    }

    // ============================================================ progress

    /** LessonProgress 构造辅助。 */
    private static final class Les {
        String id = "l1";
        LessonType type = LessonType.VIDEO;
        boolean mandatory = false;
        int order = 1;
        int completion = 100;
        int score = 100;
        int attempts = 0;
        int maximumAttempts = 0;
        boolean submitted = true;
        boolean approved = true;
        boolean plagiarism = false;
        int attended = 0;
        int required = 0;
        boolean unlocked = true;

        Les id(String value) { this.id = value; return this; }
        Les type(LessonType value) { this.type = value; return this; }
        Les mandatory(boolean value) { this.mandatory = value; return this; }
        Les order(int value) { this.order = value; return this; }
        Les completion(int value) { this.completion = value; return this; }
        Les score(int value) { this.score = value; return this; }
        Les attempts(int value) { this.attempts = value; return this; }
        Les maximumAttempts(int value) { this.maximumAttempts = value; return this; }
        Les submitted(boolean value) { this.submitted = value; return this; }
        Les approved(boolean value) { this.approved = value; return this; }
        Les plagiarism(boolean value) { this.plagiarism = value; return this; }
        Les attended(int value) { this.attended = value; return this; }
        Les required(int value) { this.required = value; return this; }
        Les unlocked(boolean value) { this.unlocked = value; return this; }

        LessonProgress build() {
            return new LessonProgress(id, type, mandatory, order, completion, score, attempts,
                    maximumAttempts, submitted, approved, plagiarism, attended, required, unlocked);
        }
    }

    /** CourseProgressSnapshot 构造辅助。 */
    private static final class Snap {
        String studentId = "s1";
        String courseId = "c1";
        List<LessonProgress> lessons = new ArrayList<>();
        boolean sequential = false;
        int requiredOverallPercent = 100;
        int requiredQuizScore = 60;
        boolean identityVerified = true;
        boolean conductAccepted = true;
        boolean subscription = true;
        boolean suspended = false;
        boolean published = true;
        Instant enrolledAt = T0.minusSeconds(86400);
        Instant accessExpiresAt = null;
        boolean certificateEnabled = false;
        String certificateName = "Certificate";
        boolean finalProjectRequired = false;
        boolean outstandingBalance = false;
        boolean instructorApprovalRequired = false;

        Snap lesson(LessonProgress lesson) {
            lessons.add(lesson);
            return this;
        }

        CourseProgressSnapshot build() {
            return new CourseProgressSnapshot(studentId, courseId, lessons, sequential,
                    requiredOverallPercent, requiredQuizScore, identityVerified, conductAccepted,
                    subscription, suspended, published, enrolledAt, accessExpiresAt,
                    certificateEnabled, certificateName, finalProjectRequired,
                    outstandingBalance, instructorApprovalRequired);
        }
    }

    @Nested
    @DisplayName("LearningProgressService 学习进度")
    class Progress {

        private final LearningProgressService service = new LearningProgressService(CLOCK);

        private ProgressEvaluation evaluate(java.util.function.Consumer<Snap> mutation) {
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().build());
            if (mutation != null) {
                mutation.accept(snapshot);
            }
            return service.evaluate(snapshot.build());
        }

        @Test
        @DisplayName("全部完成的课程判定为完成，证书未启用")
        void completedCourse() {
            ProgressEvaluation evaluation = evaluate(null);
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
        @DisplayName("视频课 90% 算完成，70-89% 给接近完成警告")
        void videoThresholds() {
            ProgressEvaluation partial = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(50).build())));
            assertEquals(46, partial.overallPercent());
            assertFalse(partial.courseCompleted());
            assertEquals("l1", partial.nextLessonId());

            ProgressEvaluation near = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(80).build())));
            assertTrue(near.warnings().contains("VIDEO_NEAR_COMPLETION:l1"));
            assertFalse(near.courseCompleted());

            ProgressEvaluation nearDone = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(90).build())));
            assertEquals(1, nearDone.completedLessons());
            assertFalse(nearDone.courseCompleted());

            ProgressEvaluation done = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(100).build())));
            assertTrue(done.courseCompleted());
        }

        @Test
        @DisplayName("文章课需要 100% 完成")
        void articleThreshold() {
            ProgressEvaluation half = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ARTICLE).completion(50).build())));
            assertEquals(50, half.overallPercent());
            assertFalse(half.courseCompleted());
            ProgressEvaluation full = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ARTICLE).completion(100).build())));
            assertTrue(full.courseCompleted());
        }

        @Test
        @DisplayName("测验课按得分阈值判定")
        void quizThreshold() {
            ProgressEvaluation low = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.QUIZ).score(40).build())));
            assertTrue(low.warnings().contains("QUIZ_SCORE_TOO_LOW:l1"));
            assertFalse(low.courseCompleted());
            assertEquals(40, low.overallPercent());

            ProgressEvaluation notSubmitted = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.QUIZ).submitted(false).score(0).build())));
            assertFalse(notSubmitted.courseCompleted());
            assertEquals(0, notSubmitted.overallPercent());

            ProgressEvaluation passed = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.QUIZ).score(100).build())));
            assertTrue(passed.courseCompleted());
        }

        @Test
        @DisplayName("作业课需要提交且通过")
        void assignmentThreshold() {
            ProgressEvaluation awaiting = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ASSIGNMENT).approved(false).build())));
            assertTrue(awaiting.warnings().contains("ASSIGNMENT_AWAITING_REVISION:l1"));
            assertFalse(awaiting.courseCompleted());
            assertEquals(48, awaiting.overallPercent());

            ProgressEvaluation notSubmitted = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ASSIGNMENT).submitted(false).build())));
            assertEquals(0, notSubmitted.overallPercent());

            ProgressEvaluation approved = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ASSIGNMENT).build())));
            assertTrue(approved.courseCompleted());
        }

        @Test
        @DisplayName("直播课按出勤分钟数判定")
        void liveSessionThreshold() {
            assertTrue(evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(0).attended(1).build())))
                    .courseCompleted());
            assertFalse(evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(0).attended(0).build())))
                    .courseCompleted());

            ProgressEvaluation enough = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(60).attended(48).build())));
            assertEquals(1, enough.completedLessons());
            assertEquals(80, enough.overallPercent());
            assertTrue(evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(60).attended(60).build())))
                    .courseCompleted());

            ProgressEvaluation shortAttendance = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(60).attended(30).build())));
            assertFalse(shortAttendance.courseCompleted());
            assertTrue(shortAttendance.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l1"));
            assertEquals(50, shortAttendance.overallPercent());
        }

        @Test
        @DisplayName("项目课需要提交通过且无抄袭")
        void projectThreshold() {
            assertTrue(evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.PROJECT).build()))).courseCompleted());

            ProgressEvaluation plagiarism = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.PROJECT).plagiarism(true).build())));
            assertTrue(plagiarism.warnings().contains("PROJECT_HELD_FOR_INTEGRITY_REVIEW:l1"));
            assertTrue(plagiarism.blockers().contains("PLAGIARISM_REVIEW:l1"));
            assertFalse(plagiarism.courseCompleted());
            assertEquals(33, plagiarism.overallPercent());
        }

        @Test
        @DisplayName("必做课未完成会阻塞并写 blocker")
        void mandatoryLessonBlocks() {
            ProgressEvaluation evaluation = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().mandatory(true).completion(10).build())));
            assertTrue(evaluation.blockers().contains("MANDATORY_LESSON_INCOMPLETE:l1"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        @DisplayName("尝试次数用尽会阻塞")
        void attemptLimitBlocks() {
            ProgressEvaluation evaluation = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(10).attempts(2).maximumAttempts(2).build())));
            assertTrue(evaluation.blockers().contains("ATTEMPT_LIMIT_REACHED:l1"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        @DisplayName("锁定但有进度、顺序学习被绕过都给警告")
        void lockedAndSequentialWarnings() {
            ProgressEvaluation locked = evaluate(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().unlocked(false).completion(50).build())));
            assertTrue(locked.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:l1"));
            assertTrue(locked.warnings().contains("NO_ACCESSIBLE_NEXT_LESSON"));

            Snap sequential = new Snap();
            sequential.sequential = true;
            sequential.lesson(new Les().id("l1").order(1).completion(10).mandatory(true).build());
            sequential.lesson(new Les().id("l2").order(2).completion(100).build());
            ProgressEvaluation evaluation = service.evaluate(sequential.build());
            assertTrue(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
            assertEquals("l1", evaluation.nextLessonId());
        }

        @Test
        @DisplayName("顺序学习下只有前一课完成才能解锁下一课")
        void sequentialNextLesson() {
            Snap sequential = new Snap();
            sequential.sequential = true;
            sequential.lesson(new Les().id("l1").order(1).completion(100).mandatory(true).build());
            sequential.lesson(new Les().id("l2").order(2).completion(50).build());
            ProgressEvaluation evaluation = service.evaluate(sequential.build());
            assertEquals("l2", evaluation.nextLessonId());
            assertEquals(73, evaluation.overallPercent());
        }

        @Test
        @DisplayName("完成状态与仍有下一课冲突时给警告")
        void completionStateConflict() {
            Snap snapshot = new Snap();
            snapshot.requiredOverallPercent = 70;
            snapshot.lesson(new Les().id("l1").order(1).completion(100).build());
            snapshot.lesson(new Les().id("l2").order(2).type(LessonType.ARTICLE)
                    .completion(50).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertTrue(evaluation.courseCompleted());
            assertEquals("l2", evaluation.nextLessonId());
            assertTrue(evaluation.warnings().contains("COMPLETION_STATE_CONFLICT"));
        }

        @Test
        @DisplayName("没有任何课时")
        void noLessons() {
            ProgressEvaluation evaluation = evaluate(s -> s.lessons = new ArrayList<>());
            assertEquals(0, evaluation.overallPercent());
            assertEquals(0, evaluation.totalLessons());
            assertFalse(evaluation.courseCompleted());
            assertTrue(evaluation.blockers().contains("COURSE_HAS_NO_LESSONS"));
        }

        @Test
        @DisplayName("总进度低于要求")
        void overallBelowRequirement() {
            ProgressEvaluation evaluation = evaluate(s -> {
                s.requiredOverallPercent = 80;
                s.lessons = new ArrayList<>(List.of(new Les().completion(50).build()));
            });
            assertTrue(evaluation.blockers().contains("OVERALL_PROGRESS_BELOW_REQUIRED"));
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        @DisplayName("毕业项目与教师审批要求")
        void finalProjectAndInstructorApproval() {
            ProgressEvaluation project = evaluate(s -> s.finalProjectRequired = true);
            assertTrue(project.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
            assertFalse(project.courseCompleted());

            ProgressEvaluation approval = evaluate(s -> {
                s.instructorApprovalRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.ASSIGNMENT)
                        .mandatory(true).approved(false).build()));
            });
            assertTrue(approval.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
            assertFalse(approval.courseCompleted());
        }

        @Test
        @DisplayName("账号与访问权限阻塞项")
        void accountAndAccessBlockers() {
            assertTrue(evaluate(s -> s.published = false).blockers().contains("COURSE_NOT_PUBLISHED"));
            assertTrue(evaluate(s -> s.suspended = true).blockers().contains("ACCOUNT_SUSPENDED"));
            assertTrue(evaluate(s -> s.subscription = false).blockers()
                    .contains("SUBSCRIPTION_INACTIVE"));
            assertTrue(evaluate(s -> s.conductAccepted = false).blockers()
                    .contains("CODE_OF_CONDUCT_NOT_ACCEPTED"));
            assertTrue(evaluate(s -> s.enrolledAt = T0.plusSeconds(60)).blockers()
                    .contains("ENROLLMENT_DATE_IN_FUTURE"));
            assertTrue(evaluate(s -> s.accessExpiresAt = T0.minusSeconds(60)).blockers()
                    .contains("COURSE_ACCESS_EXPIRED"));
            assertTrue(evaluate(s -> s.accessExpiresAt = T0.plusSeconds(3 * 86400)).warnings()
                    .contains("COURSE_ACCESS_EXPIRES_SOON"));
            assertTrue(evaluate(s -> s.identityVerified = false).warnings()
                    .contains("IDENTITY_NOT_VERIFIED"));
            assertTrue(evaluate(s -> s.outstandingBalance = true).warnings()
                    .contains("OUTSTANDING_BALANCE"));
        }

        @Test
        @DisplayName("证书资格的六个分支")
        void certificateEligibility() {
            ProgressEvaluation eligible = evaluate(s -> s.certificateEnabled = true);
            assertTrue(eligible.certificateEligible());
            assertTrue(eligible.blockers().isEmpty());

            assertTrue(evaluate(s -> {
                s.certificateEnabled = true;
                s.identityVerified = false;
            }).blockers().contains("CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION"));

            assertTrue(evaluate(s -> {
                s.certificateEnabled = true;
                s.outstandingBalance = true;
            }).blockers().contains("CERTIFICATE_BLOCKED_BY_BALANCE"));

            assertTrue(evaluate(s -> {
                s.certificateEnabled = true;
                s.certificateName = null;
            }).blockers().contains("CERTIFICATE_NAME_REQUIRED"));

            assertTrue(evaluate(s -> {
                s.certificateEnabled = true;
                s.certificateName = "x";
            }).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));

            assertTrue(evaluate(s -> {
                s.certificateEnabled = true;
                s.certificateName = "x".repeat(81);
            }).blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));

            assertFalse(evaluate(s -> {
                s.certificateEnabled = true;
                s.lessons = new ArrayList<>(List.of(new Les().completion(10).build()));
            }).certificateEligible());
        }

        @Test
        @DisplayName("快照与课时的校验")
        void validation() {
            assertEquals("snapshot must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(null)).getMessage());
            assertEquals("studentId must not be blank", invalidSnapshot(s -> s.studentId = " "));
            assertEquals("courseId must not be blank", invalidSnapshot(s -> s.courseId = null));
            assertEquals("requiredOverallPercent must be between 0 and 100",
                    invalidSnapshot(s -> s.requiredOverallPercent = 101));
            assertEquals("requiredOverallPercent must be between 0 and 100",
                    invalidSnapshot(s -> s.requiredOverallPercent = -1));
            assertEquals("requiredQuizScore must be between 0 and 100",
                    invalidSnapshot(s -> s.requiredQuizScore = 101));
            assertEquals("requiredQuizScore must be between 0 and 100",
                    invalidSnapshot(s -> s.requiredQuizScore = -1));
            assertThrows(NullPointerException.class, () -> {
                Snap snapshot = new Snap();
                snapshot.lessons = new ArrayList<>(java.util.Collections.singletonList(null));
                service.evaluate(snapshot.build());
            });
            assertEquals("lessonId must not be blank", invalidLesson(l -> l.id = " "));
            assertEquals("lesson type must not be null", invalidLesson(l -> l.type = null));
            assertEquals("lesson order must not be negative", invalidLesson(l -> l.order = -1));
            assertEquals("completionPercent must be between 0 and 100",
                    invalidLesson(l -> l.completion = 101));
            assertEquals("completionPercent must be between 0 and 100",
                    invalidLesson(l -> l.completion = -1));
            assertEquals("score must be between 0 and 100", invalidLesson(l -> l.score = 101));
            assertEquals("score must be between 0 and 100", invalidLesson(l -> l.score = -1));
            assertEquals("attempt counts must not be negative", invalidLesson(l -> l.attempts = -1));
            assertEquals("attempt counts must not be negative",
                    invalidLesson(l -> l.maximumAttempts = -1));
            assertEquals("attempts must not exceed maximumAttempts", invalidLesson(l -> {
                l.attempts = 3;
                l.maximumAttempts = 2;
            }));
            assertEquals("attendance minutes must not be negative", invalidLesson(l -> l.attended = -1));
            assertEquals("attendance minutes must not be negative", invalidLesson(l -> l.required = -1));

            Snap duplicateId = new Snap();
            duplicateId.lesson(new Les().id("dup").order(1).build());
            duplicateId.lesson(new Les().id("dup").order(2).build());
            assertEquals("lessonId must be unique", assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(duplicateId.build())).getMessage());

            Snap duplicateOrder = new Snap();
            duplicateOrder.lesson(new Les().id("a").order(1).build());
            duplicateOrder.lesson(new Les().id("b").order(1).build());
            assertEquals("lesson order must be unique", assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(duplicateOrder.build())).getMessage());
        }

        private String invalidSnapshot(java.util.function.Consumer<Snap> mutation) {
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().build());
            mutation.accept(snapshot);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot.build())).getMessage();
        }

        private String invalidLesson(java.util.function.Consumer<Les> mutation) {
            Les lesson = new Les();
            mutation.accept(lesson);
            Snap snapshot = new Snap();
            snapshot.lesson(lesson.build());
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(snapshot.build())).getMessage();
        }
    }

    // ============================================================ scheduling

    /** 周一 10:00（Asia/Shanghai）。 */
    private static final Instant MON_10 = Instant.parse("2026-11-16T02:00:00Z");

    /** ScheduleRequest 构造辅助。 */
    private static final class Sch {
        String courseId = "c1";
        String cohortId = "h1";
        String instructorId = "i1";
        DeliveryMode delivery = DeliveryMode.ONLINE;
        CourseKind kind = CourseKind.LECTURE;
        Instant start = MON_10;
        Instant end = MON_10.plusSeconds(3600);
        String zone = "Asia/Shanghai";
        String roomId = null;
        String meetingUrl = "https://meet.example/1";
        int expected = 20;
        int registered = 20;
        int roomCapacity = 0;
        Set<String> requiredEquipment = Set.of();
        Set<String> availableEquipment = Set.of();
        boolean accessibleRequired = false;
        boolean roomAccessible = false;
        boolean weekendAllowed = false;
        boolean afterHoursAllowed = false;
        boolean lunchAllowed = false;
        boolean recurring = false;
        int occurrences = 1;
        int buffer = 0;
        int maxSessions = 3;
        List<net.mooctest.scheduling.domain.TimeWindow> availability = List.of();
        List<net.mooctest.scheduling.domain.TimeWindow> maintenance = List.of();
        List<ExistingBooking> bookings = List.of();
        Set<LocalDate> holidays = Set.of();
        boolean adminOverride = false;
        boolean recordingRequired = false;
        boolean recordingSupported = false;

        Sch start(String isoInstant, int minutes) {
            this.start = Instant.parse(isoInstant);
            this.end = this.start.plusSeconds(minutes * 60L);
            return this;
        }

        Sch window(Instant from, Instant to) {
            this.start = from;
            this.end = to;
            return this;
        }

        ScheduleRequest build() {
            return new ScheduleRequest(courseId, cohortId, instructorId, delivery, kind, start, end,
                    zone, roomId, meetingUrl, expected, registered, roomCapacity, requiredEquipment,
                    availableEquipment, accessibleRequired, roomAccessible, weekendAllowed,
                    afterHoursAllowed, lunchAllowed, recurring, occurrences, buffer, maxSessions,
                    availability, maintenance, bookings, holidays, adminOverride, recordingRequired,
                    recordingSupported);
        }
    }

    private static net.mooctest.scheduling.domain.TimeWindow window(String from, String to) {
        return new net.mooctest.scheduling.domain.TimeWindow(Instant.parse(from), Instant.parse(to));
    }

    private static ExistingBooking booking(String id, String instructor, String room, String cohort,
                                           Instant start, Instant end) {
        return new ExistingBooking(id, instructor, room, cohort, start, end, false, false,
                CourseKind.LECTURE);
    }

    @Nested
    @DisplayName("SchedulingService 排课")
    class Scheduling {

        private final SchedulingService service = new SchedulingService(CLOCK);

        private SchedulingDecision evaluate(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        @Test
        @DisplayName("基础线上讲座排课通过，只提示未提供教师可用时段")
        void approvedBaseSchedule() {
            SchedulingDecision decision = evaluate(null);
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals("APPROVED", decision.status());
            assertEquals(60, decision.durationMinutes());
            assertTrue(decision.conflicts().isEmpty());
            assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), decision.warnings());
        }

        @Test
        @DisplayName("过去时间、临期与短期通知")
        void leadTimeRules() {
            assertTrue(evaluate(s -> s.start(T0.minusSeconds(3600).toString(), 60)).conflicts()
                    .contains("START_TIME_IN_PAST"));
            assertTrue(evaluate(s -> s.start(T0.plusSeconds(3600).toString(), 60)).conflicts()
                    .contains("INSUFFICIENT_BOOKING_NOTICE"));
            assertTrue(evaluate(s -> s.start(T0.plusSeconds(5 * 3600).toString(), 60)).warnings()
                    .contains("SHORT_BOOKING_NOTICE"));
        }

        @Test
        @DisplayName("跨本地日期的课程被拒绝")
        void crossDateSession() {
            SchedulingDecision decision = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T23:00:00Z", 120);
                s.afterHoursAllowed = true;
            });
            assertTrue(decision.conflicts().contains("SESSION_CROSSES_LOCAL_DATE"));
            assertFalse(decision.approved());
        }

        @Test
        @DisplayName("周末与节假日限制")
        void weekendAndHoliday() {
            assertTrue(evaluate(s -> s.start("2026-11-21T02:00:00Z", 60)).conflicts()
                    .contains("WEEKEND_NOT_ALLOWED"));
            assertFalse(evaluate(s -> {
                s.start("2026-11-21T02:00:00Z", 60);
                s.weekendAllowed = true;
            }).conflicts().contains("WEEKEND_NOT_ALLOWED"));
            assertTrue(evaluate(s -> s.start("2026-11-22T02:00:00Z", 60)).conflicts()
                    .contains("WEEKEND_NOT_ALLOWED"));
            assertTrue(evaluate(s -> s.holidays = Set.of(LocalDate.of(2026, 11, 16))).conflicts()
                    .contains("HOLIDAY_NOT_AVAILABLE"));
        }

        @Test
        @DisplayName("营业时间外与午休时段")
        void operatingHoursAndLunch() {
            SchedulingDecision outside = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T07:00:00Z", 60);
            });
            assertTrue(outside.conflicts().contains("OUTSIDE_OPERATING_HOURS"));

            SchedulingDecision afterHours = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T07:00:00Z", 60);
                s.afterHoursAllowed = true;
            });
            assertTrue(afterHours.warnings().contains("AFTER_HOURS_SESSION"));
            assertFalse(afterHours.conflicts().contains("OUTSIDE_OPERATING_HOURS"));

            SchedulingDecision lunch = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T12:00:00Z", 60);
            });
            assertTrue(lunch.conflicts().contains("LUNCH_BREAK_CONFLICT"));

            SchedulingDecision lunchAllowed = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T12:00:00Z", 60);
                s.lunchAllowed = true;
            });
            assertTrue(lunchAllowed.warnings().contains("LUNCH_TIME_SESSION"));
            assertFalse(lunchAllowed.conflicts().contains("LUNCH_BREAK_CONFLICT"));
        }

        @Test
        @DisplayName("讲座时长上下限")
        void lectureDuration() {
            assertTrue(evaluate(s -> s.start(MON_10.toString(), 20)).conflicts()
                    .contains("LECTURE_TOO_SHORT"));
            SchedulingDecision longLecture = evaluate(s -> {
                s.start(MON_10.toString(), 190);
                s.lunchAllowed = true;
            });
            assertTrue(longLecture.warnings().contains("LECTURE_LONGER_THAN_RECOMMENDED"));
        }

        @Test
        @DisplayName("实验课的时长、安全包与人数限制")
        void labRules() {
            assertTrue(evaluate(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 30);
            }).conflicts().contains("LAB_TOO_SHORT"));

            SchedulingDecision tooLong = evaluate(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 300);
                s.lunchAllowed = true;
                s.availableEquipment = Set.of("LAB_SAFETY_KIT");
            });
            assertTrue(tooLong.conflicts().contains("LAB_TOO_LONG"));

            SchedulingDecision noKit = evaluate(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 60);
            });
            assertTrue(noKit.conflicts().contains("LAB_SAFETY_KIT_REQUIRED"));

            SchedulingDecision ready = evaluate(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 60);
                s.availableEquipment = Set.of("LAB_SAFETY_KIT");
            });
            assertTrue(ready.conflicts().isEmpty());
            assertTrue(ready.approved());

            SchedulingDecision crowded = evaluate(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 60);
                s.availableEquipment = Set.of("LAB_SAFETY_KIT");
                s.expected = 40;
            });
            assertTrue(crowded.conflicts().contains("LAB_CAPACITY_EXCEEDED"));
        }

        @Test
        @DisplayName("考试的时长、短通知与远程监考")
        void examRules() {
            assertTrue(evaluate(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 10);
            }).conflicts().contains("EXAM_TOO_SHORT"));

            SchedulingDecision tooLong = evaluate(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 400);
                s.lunchAllowed = true;
            });
            assertTrue(tooLong.conflicts().contains("EXAM_TOO_LONG"));

            SchedulingDecision remote = evaluate(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 60);
            });
            assertTrue(remote.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
            assertTrue(remote.manualReview());
            assertEquals("MANUAL_REVIEW", remote.status());
            assertTrue(remote.warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));

            SchedulingDecision proctored = evaluate(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 60);
                s.availableEquipment = Set.of("PROCTORING");
            });
            assertFalse(proctored.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
            assertTrue(proctored.approved());
        }

        @Test
        @DisplayName("工作坊与网络研讨会的时长限制")
        void workshopAndWebinar() {
            assertTrue(evaluate(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 30);
            }).conflicts().contains("WORKSHOP_TOO_SHORT"));

            SchedulingDecision longWorkshop = evaluate(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 400);
                s.lunchAllowed = true;
            });
            assertTrue(longWorkshop.warnings().contains("WORKSHOP_REQUIRES_BREAK_PLAN"));
            assertTrue(longWorkshop.warnings().contains("EXTENDED_WORKSHOP_REQUIRES_REVIEW"));

            assertTrue(evaluate(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 60);
                s.expected = 60;
            }).warnings().contains("WORKSHOP_GROUP_TOO_LARGE"));

            assertTrue(evaluate(s -> {
                s.kind = CourseKind.WEBINAR;
                s.start(MON_10.toString(), 10);
            }).conflicts().contains("WEBINAR_TOO_SHORT"));

            assertTrue(evaluate(s -> {
                s.kind = CourseKind.WEBINAR;
                s.start(MON_10.toString(), 150);
                s.lunchAllowed = true;
            }).warnings().contains("WEBINAR_LONGER_THAN_RECOMMENDED"));
        }

        @Test
        @DisplayName("三种授课模式的资源要求")
        void deliveryModes() {
            assertTrue(evaluate(s -> s.meetingUrl = null).conflicts().contains("MEETING_URL_REQUIRED"));
            assertTrue(evaluate(s -> s.meetingUrl = " ").conflicts().contains("MEETING_URL_REQUIRED"));
            assertTrue(evaluate(s -> s.meetingUrl = "http://insecure").conflicts()
                    .contains("SECURE_MEETING_URL_REQUIRED"));
            assertTrue(evaluate(s -> s.roomId = "R1").warnings()
                    .contains("ROOM_IGNORED_FOR_ONLINE_SESSION"));

            SchedulingDecision classroomNoRoom = evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomCapacity = 50;
            });
            assertTrue(classroomNoRoom.conflicts().contains("ROOM_REQUIRED"));

            SchedulingDecision classroom = evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomCapacity = 50;
                s.roomId = "R1";
            });
            assertTrue(classroom.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));
            assertTrue(classroom.approved());

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.HYBRID;
                s.roomCapacity = 50;
                s.meetingUrl = null;
            }).conflicts().contains("HYBRID_ROOM_REQUIRED"));
            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.HYBRID;
                s.roomCapacity = 50;
                s.roomId = "R1";
                s.meetingUrl = null;
            }).conflicts().contains("HYBRID_MEETING_URL_REQUIRED"));
            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.HYBRID;
                s.roomCapacity = 50;
                s.roomId = "R1";
                s.meetingUrl = "http://insecure";
            }).conflicts().contains("SECURE_MEETING_URL_REQUIRED"));
        }

        @Test
        @DisplayName("录播与设备要求")
        void recordingAndEquipment() {
            assertTrue(evaluate(s -> s.recordingRequired = true).conflicts()
                    .contains("RECORDING_NOT_SUPPORTED"));
            assertTrue(evaluate(s -> s.recordingSupported = true).warnings()
                    .contains("RECORDING_AVAILABLE"));
            assertTrue(evaluate(s -> s.requiredEquipment = Set.of("projector")).conflicts()
                    .contains("MISSING_EQUIPMENT:projector"));
            assertTrue(evaluate(s -> {
                s.requiredEquipment = Set.of("projector");
                s.availableEquipment = Set.of("projector");
            }).conflicts().isEmpty());
        }

        @Test
        @DisplayName("容量与无障碍席位")
        void capacityRules() {
            assertTrue(evaluate(s -> s.registered = 30).warnings()
                    .contains("REGISTRATION_EXCEEDS_FORECAST"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 0;
            }).conflicts().contains("ROOM_CAPACITY_UNKNOWN"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 25;
                s.registered = 30;
                s.expected = 30;
            }).conflicts().contains("ROOM_TOO_SMALL_FOR_REGISTRATION"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 35;
                s.registered = 20;
                s.expected = 40;
            }).warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 1000;
                s.registered = 10;
                s.expected = 10;
            }).warnings().contains("ROOM_SIGNIFICANTLY_OVERSIZED"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.accessibleRequired = true;
                s.roomAccessible = false;
            }).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));

            assertTrue(evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.accessibleRequired = true;
                s.roomAccessible = true;
            }).conflicts().isEmpty());

            assertTrue(evaluate(s -> s.expected = 1500).conflicts()
                    .contains("ONLINE_PLATFORM_CAPACITY_EXCEEDED"));
            assertTrue(evaluate(s -> s.expected = 700).warnings().contains("LARGE_ONLINE_EVENT"));
        }

        @Test
        @DisplayName("教师可用时段与维护窗口")
        void availabilityAndMaintenance() {
            SchedulingDecision unavailable = evaluate(s -> s.availability = List.of(
                    window("2026-11-16T05:00:00Z", "2026-11-16T06:00:00Z")));
            assertTrue(unavailable.conflicts().contains("INSTRUCTOR_UNAVAILABLE"));

            SchedulingDecision contained = evaluate(s -> s.availability = List.of(
                    window("2026-11-16T01:00:00Z", "2026-11-16T04:00:00Z")));
            assertTrue(contained.conflicts().isEmpty());
            assertTrue(contained.warnings().isEmpty());
            assertTrue(contained.approved());

            assertTrue(evaluate(s -> s.maintenance = List.of(
                    window("2026-11-16T02:30:00Z", "2026-11-16T03:30:00Z"))).conflicts()
                    .contains("MAINTENANCE_WINDOW_CONFLICT"));
        }

        @Test
        @DisplayName("已有预约的冲突与会话数/工时上限")
        void existingBookings() {
            SchedulingDecision instructor = evaluate(s -> s.bookings = List.of(
                    booking("b1", "i1", "R9", "h9", MON_10, MON_10.plusSeconds(1800))));
            assertTrue(instructor.conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:b1"));
            assertFalse(instructor.approved());

            SchedulingDecision room = evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.bookings = List.of(booking("b2", "i2", "R1", "h9", MON_10,
                        MON_10.plusSeconds(1800)));
            });
            assertTrue(room.conflicts().contains("ROOM_BOOKING_CONFLICT:b2"));

            SchedulingDecision cohort = evaluate(s -> s.bookings = List.of(
                    booking("b3", "i2", "R9", "h1", MON_10, MON_10.plusSeconds(1800))));
            assertTrue(cohort.conflicts().contains("COHORT_BOOKING_CONFLICT:b3"));

            SchedulingDecision tentative = evaluate(s -> s.bookings = List.of(
                    new ExistingBooking("b4", "i2", "R9", "h9", MON_10, MON_10.plusSeconds(1800),
                            false, true, CourseKind.LECTURE)));
            assertTrue(tentative.warnings().contains("CONFLICTING_BOOKING_IS_TENTATIVE:b4"));

            SchedulingDecision cancelled = evaluate(s -> s.bookings = List.of(
                    new ExistingBooking("b5", "i1", "R1", "h1", MON_10, MON_10.plusSeconds(1800),
                            true, false, CourseKind.LECTURE)));
            assertTrue(cancelled.conflicts().isEmpty());

            List<ExistingBooking> threeSessions = List.of(
                    booking("s1", "i1", "R9", "h9", Instant.parse("2026-11-16T05:00:00Z"),
                            Instant.parse("2026-11-16T06:00:00Z")),
                    booking("s2", "i1", "R9", "h9", Instant.parse("2026-11-16T06:00:00Z"),
                            Instant.parse("2026-11-16T07:00:00Z")),
                    booking("s3", "i1", "R9", "h9", Instant.parse("2026-11-16T07:00:00Z"),
                            Instant.parse("2026-11-16T08:00:00Z")));
            SchedulingDecision limit = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.bookings = threeSessions;
            });
            assertTrue(limit.conflicts().contains("INSTRUCTOR_DAILY_SESSION_LIMIT"));

            SchedulingDecision nearLimit = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.bookings = threeSessions.subList(0, 2);
            });
            assertTrue(nearLimit.warnings().contains("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"));

            List<ExistingBooking> longDay = List.of(
                    booking("l1", "i1", "R9", "h9", Instant.parse("2026-11-16T00:00:00Z"),
                            Instant.parse("2026-11-16T02:00:00Z")),
                    booking("l2", "i1", "R9", "h9", Instant.parse("2026-11-16T02:00:00Z"),
                            Instant.parse("2026-11-16T04:00:00Z")),
                    booking("l3", "i1", "R9", "h9", Instant.parse("2026-11-16T04:00:00Z"),
                            Instant.parse("2026-11-16T06:00:00Z")),
                    booking("l4", "i1", "R9", "h9", Instant.parse("2026-11-16T06:00:00Z"),
                            Instant.parse("2026-11-16T08:00:00Z")),
                    booking("l5", "i1", "R9", "h9", Instant.parse("2026-11-16T08:00:00Z"),
                            Instant.parse("2026-11-16T10:00:00Z")));
            SchedulingDecision overworked = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.maxSessions = 10;
                s.bookings = longDay;
            });
            assertTrue(overworked.conflicts().contains("INSTRUCTOR_DAILY_HOURS_EXCEEDED"));

            SchedulingDecision longWorkday = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.maxSessions = 10;
                s.bookings = longDay.subList(0, 3);
            });
            assertTrue(longWorkday.warnings().contains("INSTRUCTOR_LONG_WORKDAY"));
        }

        @Test
        @DisplayName("重复课程规则")
        void recurrenceRules() {
            assertTrue(evaluate(s -> s.occurrences = 2).conflicts()
                    .contains("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"));
            assertTrue(evaluate(s -> {
                s.recurring = true;
                s.occurrences = 1;
            }).conflicts().contains("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES"));
            assertTrue(evaluate(s -> {
                s.recurring = true;
                s.occurrences = 60;
            }).conflicts().contains("TOO_MANY_RECURRENCES"));

            SchedulingDecision longSeries = evaluate(s -> {
                s.recurring = true;
                s.occurrences = 30;
            });
            assertTrue(longSeries.warnings().contains("LONG_RECURRENCE_SERIES"));

            SchedulingDecision weekendSeries = evaluate(s -> {
                s.recurring = true;
                s.occurrences = 5;
                s.weekendAllowed = true;
                s.start("2026-11-21T02:00:00Z", 60);
            });
            assertTrue(weekendSeries.warnings().contains("RECURRING_WEEKEND_SESSION"));

            SchedulingDecision examSeries = evaluate(s -> {
                s.recurring = true;
                s.occurrences = 5;
                s.kind = CourseKind.EXAM;
                s.availableEquipment = Set.of("PROCTORING");
            });
            assertTrue(examSeries.warnings().contains("RECURRING_EXAM_REQUIRES_REVIEW"));
        }

        @Test
        @DisplayName("管理员覆写：可覆盖冲突被清除，不可覆盖冲突保留")
        void administratorOverride() {
            SchedulingDecision cleared = evaluate(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T07:00:00Z", 60);
                s.adminOverride = true;
            });
            assertTrue(cleared.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
            assertTrue(cleared.conflicts().isEmpty());
            assertTrue(cleared.approved());

            SchedulingDecision partially = evaluate(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 5;
                s.registered = 20;
                s.expected = 20;
                s.adminOverride = true;
            });
            assertTrue(partially.warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));
            assertTrue(partially.conflicts().contains("ROOM_TOO_SMALL_FOR_REGISTRATION"));
            assertFalse(partially.approved());
        }

        @Test
        @DisplayName("大型混合课程进入人工复核")
        void largeHybridManualReview() {
            SchedulingDecision decision = evaluate(s -> {
                s.delivery = DeliveryMode.HYBRID;
                s.roomId = "R1";
                s.roomCapacity = 500;
                s.expected = 300;
                s.registered = 300;
            });
            assertTrue(decision.manualReview());
            assertTrue(decision.warnings().contains("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"));
            assertFalse(decision.approved());
            assertEquals("MANUAL_REVIEW", decision.status());
        }

        @Test
        @DisplayName("请求校验")
        void validation() {
            assertEquals("request must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(null)).getMessage());
            assertEquals("courseId must not be blank", invalid(s -> s.courseId = " "));
            assertEquals("cohortId must not be blank", invalid(s -> s.cohortId = null));
            assertEquals("instructorId must not be blank", invalid(s -> s.instructorId = " "));
            assertEquals("delivery mode and course kind are required", invalid(s -> s.delivery = null));
            assertEquals("delivery mode and course kind are required", invalid(s -> s.kind = null));
            assertEquals("start and end are required", assertInvalidSchedule(s -> s.start = null));
            assertEquals("start and end are required", assertInvalidSchedule(s -> s.end = null));
            assertEquals("end must be after start", assertInvalidSchedule(s -> s.end = s.start));
            assertEquals("session duration must be less than one day",
                    invalid(s -> s.end = s.start.plusSeconds(24 * 3600)));
            assertEquals("zoneId must not be blank", invalid(s -> s.zone = " "));
            assertEquals("zoneId is invalid", invalid(s -> s.zone = "Not/AZone"));
            assertEquals("participant and capacity values must not be negative",
                    invalid(s -> s.expected = -1));
            assertEquals("participant and capacity values must not be negative",
                    invalid(s -> s.registered = -1));
            assertEquals("participant and capacity values must not be negative",
                    invalid(s -> s.roomCapacity = -1));
            assertEquals("occurrenceCount must be positive", invalid(s -> s.occurrences = 0));
            assertEquals("minimumBufferMinutes must be between 0 and 240", invalid(s -> s.buffer = -1));
            assertEquals("minimumBufferMinutes must be between 0 and 240", invalid(s -> s.buffer = 241));
            assertEquals("maximumInstructorSessionsPerDay must be positive",
                    invalid(s -> s.maxSessions = 0));
            assertEquals("instructor availability is invalid", invalid(s -> s.availability = List.of(
                    window("2026-11-16T04:00:00Z", "2026-11-16T03:00:00Z"))));
            assertEquals("maintenance window is invalid", invalid(s -> s.maintenance = List.of(
                    window("2026-11-16T03:00:00Z", "2026-11-16T03:00:00Z"))));
            assertEquals("bookingId must not be blank", invalid(s -> s.bookings = List.of(
                    new ExistingBooking(" ", "i2", "R9", "h9", MON_10, MON_10.plusSeconds(600),
                            false, false, CourseKind.LECTURE))));
            assertEquals("bookingId must be unique", invalid(s -> s.bookings = List.of(
                    booking("dup", "i2", "R9", "h9", MON_10, MON_10.plusSeconds(600)),
                    booking("dup", "i3", "R8", "h8", MON_10, MON_10.plusSeconds(600)))));
            assertEquals("booking window is invalid", invalid(s -> s.bookings = List.of(
                    new ExistingBooking("b1", "i2", "R9", "h9", MON_10, MON_10,
                            false, false, CourseKind.LECTURE))));
            assertEquals("required equipment must not contain blanks",
                    invalid(s -> s.requiredEquipment = Set.of(" ")));
            assertEquals("available equipment must not contain blanks",
                    invalid(s -> s.availableEquipment = Set.of("")));
        }

        private String invalid(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(request.build())).getMessage();
        }

        private String assertInvalidSchedule(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(request.build())).getMessage();
        }
    }

    // ============================================================ grading.expression

    /** 只返回固定数值的表达式求值器替身。 */
    private static ExpressionEvaluator fixedEvaluator(BigDecimal value) {
        return expression -> value;
    }

    /** 对指定表达式抛异常、其它返回固定值的求值器替身。 */
    private static ExpressionEvaluator evaluatorReturning(BigDecimal value) {
        return expression -> {
            if ("CAUSE".equals(expression)) {
                throw new ExpressionEvaluationException("bad correct expression");
            }
            return value;
        };
    }

    /** 返回 null 数值结果的引擎替身（覆盖 evaluator 的空结果分支）。 */
    private static final class NullResultCalculator extends BracketsCalculation2 {
        NullResultCalculator(String name) {
            super(name);
        }

        @Override
        public PackExpression compileBigDecimal(String formula, boolean formatRequired) {
            NameExpression inner = BracketsCalculation2.getInstance("null-result-inner")
                    .compileBigDecimal("1", false);
            return new PackExpression("stub", inner, "stub") {
                @Override
                public CalculationResults calculationBigDecimals(boolean isCopy) {
                    return new CalculationResults() {
                        @Override
                        public int getResultLayers() {
                            return 0;
                        }

                        @Override
                        public String getCalculationSourceName() {
                            return "stub";
                        }

                        @Override
                        public Object getResult() {
                            return null;
                        }

                        @Override
                        public void setSource(String source) {
                        }

                        @Override
                        public BigDecimal getBigDecimalResult() {
                            return null;
                        }
                    };
                }
            };
        }
    }

    /** 编译即抛运行期异常的引擎替身（覆盖 evaluator 的包装分支）。 */
    private static final class ExplodingCalculator extends BracketsCalculation2 {
        ExplodingCalculator(String name) {
            super(name);
        }

        @Override
        public PackExpression compileBigDecimal(String formula, boolean formatRequired) {
            throw new IllegalStateException("boom");
        }
    }

    @Nested
    @DisplayName("MathematicalExpressionEvaluator 与表达式答案判分")
    class ExpressionAnswerGrading {

        private final MathematicalExpressionEvaluator evaluator = new MathematicalExpressionEvaluator();

        private ExpressionQuestion question(String correct, String tolerance) {
            return new ExpressionQuestion("q1", "math", correct, bd("10.00"), bd(tolerance), true);
        }

        @Test
        @DisplayName("空表达式与 null 表达式被拒绝")
        void evaluatorRejectsBlank() {
            assertEquals("expression must not be blank",
                    assertThrows(ExpressionEvaluationException.class,
                            () -> evaluator.evaluate(null)).getMessage());
            assertEquals("expression must not be blank",
                    assertThrows(ExpressionEvaluationException.class,
                            () -> evaluator.evaluate("   ")).getMessage());
        }

        @Test
        @DisplayName("真实表达式求值结果正确")
        void evaluatorComputesRealExpressions() {
            assertEquals(0, evaluator.evaluate("(2+3)*4").compareTo(bd("20")));
            assertEquals(0, evaluator.evaluate("10/4").compareTo(bd("2.5")));
            assertEquals(0, evaluator.evaluate("2^10").compareTo(bd("1024")));
        }

        @Test
        @DisplayName("非法表达式被包装成 ExpressionEvaluationException")
        void evaluatorWrapsRuntimeFailures() {
            ExpressionEvaluationException exception = assertThrows(ExpressionEvaluationException.class,
                    () -> evaluator.evaluate("abc"));
            assertTrue(exception.getMessage().startsWith("unable to evaluate expression: abc"));
            assertNotNull(exception.getCause());
        }

        @Test
        @DisplayName("构造器拒绝 null 计算组件，替身可覆盖空结果与异常分支")
        void evaluatorCollaboratorBranches() {
            assertEquals("calculator must not be null", assertThrows(NullPointerException.class,
                    () -> new MathematicalExpressionEvaluator(null)).getMessage());

            MathematicalExpressionEvaluator nullResult =
                    new MathematicalExpressionEvaluator(new NullResultCalculator("null-result"));
            assertEquals("expression produced no numeric result",
                    assertThrows(ExpressionEvaluationException.class,
                            () -> nullResult.evaluate("1")).getMessage());

            MathematicalExpressionEvaluator exploding =
                    new MathematicalExpressionEvaluator(new ExplodingCalculator("exploding"));
            ExpressionEvaluationException exception = assertThrows(ExpressionEvaluationException.class,
                    () -> exploding.evaluate("1+1"));
            assertTrue(exception.getMessage().contains("unable to evaluate expression: 1+1"));
            assertEquals("boom", exception.getCause().getMessage());
        }

        @Test
        @DisplayName("未作答：状态 UNANSWERED 且不再求值")
        void unansweredAnswer() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(
                    expression -> {
                        throw new AssertionError("evaluator should not be called");
                    });
            for (String blank : new String[]{null, "", "   "}) {
                ExpressionAnswerEvaluation evaluation =
                        service.evaluate(new ExpressionAnswerSubmission(question("10", "0"), blank));
                assertEquals(ExpressionAnswerStatus.UNANSWERED, evaluation.status());
                assertEquals("ANSWER_NOT_PROVIDED", evaluation.message());
                assertNull(evaluation.expectedValue());
                assertNull(evaluation.actualValue());
                assertNull(evaluation.absoluteError());
                AnswerResult result = evaluation.answerResult();
                assertFalse(result.answered());
                assertFalse(result.exactMatch());
                assertNull(result.numericError());
                assertEquals(QuestionType.NUMERIC, result.type());
                assertEquals("q1", result.questionId());
                assertEquals("math", result.section());
                assertEquals(bd("10.00"), result.maximumPoints());
                assertEquals(bd("0"), result.numericTolerance());
                assertTrue(result.mandatory());
            }
        }

        @Test
        @DisplayName("标准答案表达式非法时抛 IllegalStateException")
        void invalidCorrectExpression() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(
                    evaluatorReturning(bd("10")));
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.evaluate(new ExpressionAnswerSubmission(
                            question("CAUSE", "0"), "10")));
            assertEquals("invalid correct expression for question q1", exception.getMessage());
            assertTrue(exception.getCause() instanceof ExpressionEvaluationException);
        }

        @Test
        @DisplayName("学生表达式非法：状态 INVALID_EXPRESSION")
        void invalidStudentExpression() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(
                    expression -> {
                        if ("10".equals(expression)) {
                            return bd("10");
                        }
                        throw new ExpressionEvaluationException("bad");
                    });
            ExpressionAnswerEvaluation evaluation = service.evaluate(
                    new ExpressionAnswerSubmission(question("10", "0"), "1+"));
            assertEquals(ExpressionAnswerStatus.INVALID_EXPRESSION, evaluation.status());
            assertEquals("INVALID_STUDENT_EXPRESSION", evaluation.message());
            assertEquals(0, evaluation.expectedValue().compareTo(bd("10")));
            assertNull(evaluation.actualValue());
            assertNull(evaluation.absoluteError());
            assertTrue(evaluation.answerResult().answered());
            assertFalse(evaluation.answerResult().exactMatch());
        }

        @Test
        @DisplayName("三种判定状态：精确、容差内、错误")
        void threeStatuses() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(
                    expression -> {
                        switch (expression) {
                            case "10":
                                return bd("10");
                            case "10.5":
                                return bd("10.5");
                            case "13":
                                return bd("13");
                            default:
                                return BigDecimal.ZERO;
                        }
                    });
            ExpressionQuestion question = question("10", "1");

            ExpressionAnswerEvaluation exact = service.evaluate(
                    new ExpressionAnswerSubmission(question, "10"));
            assertEquals(ExpressionAnswerStatus.EXACT, exact.status());
            assertEquals("EXACT", exact.message());
            assertEquals(0, exact.absoluteError().compareTo(BigDecimal.ZERO));
            assertTrue(exact.answerResult().exactMatch());
            assertEquals(0, exact.answerResult().numericError().compareTo(BigDecimal.ZERO));

            ExpressionAnswerEvaluation tolerant = service.evaluate(
                    new ExpressionAnswerSubmission(question, "10.5"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, tolerant.status());
            assertEquals("WITHIN_TOLERANCE", tolerant.message());
            assertEquals(0, tolerant.absoluteError().compareTo(bd("0.5")));
            assertFalse(tolerant.answerResult().exactMatch());
            assertEquals(0, tolerant.actualValue().compareTo(bd("10.5")));

            ExpressionAnswerEvaluation wrong = service.evaluate(
                    new ExpressionAnswerSubmission(question, "13"));
            assertEquals(ExpressionAnswerStatus.INCORRECT, wrong.status());
            assertEquals("INCORRECT", wrong.message());
            assertEquals(0, wrong.absoluteError().compareTo(bd("3")));
        }

        @Test
        @DisplayName("真实引擎驱动的端到端判分")
        void endToEndWithRealEngine() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(evaluator);
            ExpressionAnswerEvaluation evaluation = service.evaluate(
                    new ExpressionAnswerSubmission(question("(2+3)*4", "0.01"), "10+10"));
            assertEquals(ExpressionAnswerStatus.EXACT, evaluation.status());
            assertEquals(0, evaluation.expectedValue().compareTo(bd("20")));

            ExpressionAnswerEvaluation tolerant = service.evaluate(
                    new ExpressionAnswerSubmission(question("(2+3)*4", "1"), "20.5"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, tolerant.status());
        }

        @Test
        @DisplayName("提交与求值结果对象的 null 校验")
        void nullChecks() {
            assertEquals("evaluator must not be null", assertThrows(NullPointerException.class,
                    () -> new ExpressionAnswerGradingService(null)).getMessage());

            ExpressionAnswerGradingService service =
                    new ExpressionAnswerGradingService(fixedEvaluator(bd("1")));
            assertEquals("submission must not be null", assertThrows(NullPointerException.class,
                    () -> service.evaluate(null)).getMessage());

            assertEquals("question must not be null", assertThrows(NullPointerException.class,
                    () -> new ExpressionAnswerSubmission(null, "1")).getMessage());

            assertEquals("answerResult must not be null", assertThrows(NullPointerException.class,
                    () -> new ExpressionAnswerEvaluation(null, ExpressionAnswerStatus.EXACT,
                            null, null, null, null)).getMessage());
            assertEquals("status must not be null", assertThrows(NullPointerException.class,
                    () -> new ExpressionAnswerEvaluation(
                            new Ans().build(), null, null, null, null, null)).getMessage());
        }

        @Test
        @DisplayName("ExpressionQuestion 的参数校验")
        void questionValidation() {
            assertEquals("questionId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> new ExpressionQuestion(" ", "math", "1", bd("10"), BigDecimal.ZERO,
                            false)).getMessage());
            assertEquals("questionId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> new ExpressionQuestion(null, "math", "1", bd("10"), BigDecimal.ZERO,
                            false)).getMessage());
            assertEquals("section must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> new ExpressionQuestion("q", " ", "1", bd("10"), BigDecimal.ZERO,
                            false)).getMessage());
            assertEquals("correctExpression must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> new ExpressionQuestion("q", "math", " ", bd("10"),
                                    BigDecimal.ZERO, false)).getMessage());
            assertEquals("maximumPoints must not be null",
                    assertThrows(NullPointerException.class,
                            () -> new ExpressionQuestion("q", "math", "1", null,
                                    BigDecimal.ZERO, false)).getMessage());
            assertEquals("tolerance must not be null",
                    assertThrows(NullPointerException.class,
                            () -> new ExpressionQuestion("q", "math", "1", bd("10"), null,
                                    false)).getMessage());
            assertEquals("maximumPoints must be positive",
                    assertThrows(IllegalArgumentException.class,
                            () -> new ExpressionQuestion("q", "math", "1", BigDecimal.ZERO,
                                    BigDecimal.ZERO, false)).getMessage());
            assertEquals("tolerance must not be negative",
                    assertThrows(IllegalArgumentException.class,
                            () -> new ExpressionQuestion("q", "math", "1", bd("10"), bd("-0.01"),
                                    false)).getMessage());

            ExpressionQuestion question = question("1+1", "0");
            assertEquals("q1", question.questionId());
            assertEquals("math", question.section());
            assertEquals("1+1", question.correctExpression());
            assertTrue(question.mandatory());
        }

        @Test
        @DisplayName("ExpressionExamSubmission 与 ExamSubmission 的字段映射")
        void examSubmissionMapping() {
            ExpressionExamSubmission submission = new ExpressionExamSubmission("e1", "s1",
                    List.of(new ExpressionAnswerSubmission(question("10", "0"), "10")),
                    T0, T0.plusSeconds(600), T0.plusSeconds(3600), 1800, 2, true, true, false,
                    false, true, 2, 4, bd("3"), 70, Map.of("math", 60));
            assertEquals(1, submission.answers().size());
            ExamSubmission exam = submission.toExamSubmission(List.of(new Ans().build()));
            assertEquals("e1", exam.examId());
            assertEquals("s1", exam.studentId());
            assertEquals(T0, exam.startedAt());
            assertEquals(T0.plusSeconds(600), exam.submittedAt());
            assertEquals(T0.plusSeconds(3600), exam.deadline());
            assertEquals(1800, exam.allowedDurationSeconds());
            assertEquals(2, exam.attemptNumber());
            assertTrue(exam.identityVerified());
            assertTrue(exam.termsAccepted());
            assertFalse(exam.suspendedAccount());
            assertFalse(exam.proctorDisconnected());
            assertTrue(exam.ipAddressChanged());
            assertEquals(2, exam.fullscreenExitCount());
            assertEquals(4, exam.copyPasteEventCount());
            assertEquals(bd("3"), exam.bonusPoints());
            assertEquals(70, exam.instructorOverridePercent());
            assertEquals(Map.of("math", 60), exam.sectionPassPercentages());
            assertEquals(1, exam.answers().size());

            ExpressionExamSubmission empty = new ExpressionExamSubmission("e1", "s1", null,
                    T0, T0, null, 0, 1, true, true, false, false, false, 0, 0, BigDecimal.ZERO,
                    null, null);
            assertTrue(empty.answers().isEmpty());
            assertTrue(empty.sectionPassPercentages().isEmpty());
        }

        @Test
        @DisplayName("ExpressionExamGrade 的聚合")
        void examGradeAggregation() {
            GradeReport report = grading.grade(sub().plus(ans().build()).build(), pol().build());
            ExpressionExamGrade grade = new ExpressionExamGrade(report, null);
            assertEquals(report, grade.report());
            assertTrue(grade.answerEvaluations().isEmpty());
            assertEquals("report must not be null", assertThrows(NullPointerException.class,
                    () -> new ExpressionExamGrade(null, List.of())).getMessage());
        }

        @Test
        @DisplayName("ExpressionExamGradingService 端到端判分与构造校验")
        void examGradingService() {
            ExpressionAnswerGradingService answerService =
                    new ExpressionAnswerGradingService(evaluator);
            ExpressionExamGradingService service =
                    new ExpressionExamGradingService(answerService, grading);

            ExpressionExamSubmission submission = new ExpressionExamSubmission("e1", "s1",
                    List.of(new ExpressionAnswerSubmission(question("20", "0"), "20")),
                    T0, T0.plusSeconds(600), null, 1800, 1, true, true, false, false, false,
                    0, 0, BigDecimal.ZERO, null, Map.of());
            ExpressionExamGrade grade = service.grade(submission, pol().build());
            assertEquals(1, grade.answerEvaluations().size());
            assertEquals(ExpressionAnswerStatus.EXACT, grade.answerEvaluations().get(0).status());
            assertEquals(100, grade.report().percentage());
            assertEquals(bd("10.00"), grade.report().finalPoints());
            assertTrue(grade.report().passed());

            assertEquals("submission must not be null", assertThrows(NullPointerException.class,
                    () -> service.grade(null, pol().build())).getMessage());
            assertEquals("policy must not be null", assertThrows(NullPointerException.class,
                    () -> service.grade(submission, null)).getMessage());
            assertEquals("answerGradingService must not be null",
                    assertThrows(NullPointerException.class,
                            () -> new ExpressionExamGradingService(null, grading)).getMessage());
            assertEquals("examGradingService must not be null",
                    assertThrows(NullPointerException.class,
                            () -> new ExpressionExamGradingService(answerService, null)).getMessage());
        }

        @Test
        @DisplayName("ExpressionAnswerStatus 枚举值")
        void statusEnum() {
            assertEquals(5, ExpressionAnswerStatus.values().length);
            assertEquals(ExpressionAnswerStatus.EXACT, ExpressionAnswerStatus.valueOf("EXACT"));
            assertEquals("INVALID_EXPRESSION",
                    ExpressionAnswerStatus.valueOf("INVALID_EXPRESSION").name());
        }
    }

    // ============================================================ boundary attacks

    @Nested
    @DisplayName("ExamGradingService 边界值攻击")
    class GradingBoundaries {

        @Test
        @DisplayName("必答题得 0 分也写 NO_CREDIT 警告")
        void mandatoryZeroCreditWarning() {
            GradeReport report = grading.grade(sub().plus(ans().mandatory(true).exact(false).build()).build(),
                    pol().build());
            assertTrue(report.warnings().contains("MANDATORY_QUESTION_NO_CREDIT:q1"));
        }

        @Test
        @DisplayName("复制粘贴 5 次/全屏 3 次触发人工复核，4 次/2 次不触发")
        void manualReviewThresholds() {
            Sub five = sub().plus(ans().build());
            five.copyPasteEventCount = 5;
            assertTrue(grading.grade(five.build(), pol().build()).manualReview());

            Sub four = sub().plus(ans().build());
            four.copyPasteEventCount = 4;
            assertFalse(grading.grade(four.build(), pol().build()).manualReview());

            Sub three = sub().plus(ans().build());
            three.fullscreenExitCount = 3;
            assertTrue(grading.grade(three.build(), pol().build()).manualReview());

            Sub two = sub().plus(ans().build());
            two.fullscreenExitCount = 2;
            assertFalse(grading.grade(two.build(), pol().build()).manualReview());
        }

        @Test
        @DisplayName("百分比正好等于及格线：passed 为 true，等级 D")
        void passedAtExactPassMark() {
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = 60;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(60, report.percentage());
            assertTrue(report.passed());
            assertEquals("D", report.letterGrade());
        }

        @Test
        @DisplayName("百分比等于及格线但必答题未完成：不通过且不写 OVERALL_SCORE_BELOW_PASS_MARK")
        void notPassedAtExactPassMark() {
            Sub submission = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").mandatory(true).answered(false).exact(false).build());
            submission.overridePercent = 60;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertFalse(report.passed());
            assertFalse(report.failures().contains("OVERALL_SCORE_BELOW_PASS_MARK"));
        }

        @Test
        @DisplayName("章节通过率正好等于要求：不写 SECTION_BELOW_PASS_MARK")
        void sectionExactlyAtRequirement() {
            Sub submission = sub()
                    .plus(ans().id("q1").section("A").build())
                    .plus(ans().id("q2").section("A").exact(false).build());
            submission.sectionPass.put("A", 50);
            submission.overridePercent = 100;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(Map.of("A", 50), report.sectionPercentages());
            assertFalse(report.failures().contains("SECTION_BELOW_PASS_MARK:A"));
            assertTrue(report.passed());
        }

        @Test
        @DisplayName("章节要求 100% 且章节满分：不写失败项")
        void sectionExactlyHundred() {
            Sub submission = sub().plus(ans().section("A").build());
            submission.sectionPass.put("A", 100);
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertFalse(report.failures().contains("SECTION_BELOW_PASS_MARK:A"));
            assertTrue(report.passed());
        }

        @Test
        @DisplayName("关闭必答强制时，缺答仍可通过；开启时必须失败")
        void mandatoryPolicySwitch() {
            Sub submission = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").mandatory(true).answered(false).exact(false).build());
            submission.overridePercent = 100;

            Pol relaxed = pol();
            relaxed.allMandatoryQuestionsRequired = false;
            assertTrue(grading.grade(submission.build(), relaxed.build()).passed());

            assertFalse(grading.grade(submission.build(), pol().build()).passed());
        }

        @Test
        @DisplayName("负分与正分混合时原始总分下降（负分不会被逐题归零）")
        void negativeScoresLowerRawTotal() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;

            Sub binary = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").exact(false).build());
            assertEquals(bd("7.50"), grading.grade(binary.build(), policy.build()).rawPoints());

            Pol strictNegative = pol();
            strictNegative.partialCreditEnabled = false;
            strictNegative.negativeMarkingEnabled = true;
            Sub multiple = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").type(QuestionType.MULTIPLE_CHOICE).exact(false)
                            .correctOptionCount(4).selectedIncorrectOptions(1).build());
            assertEquals(bd("7.50"), grading.grade(multiple.build(), strictNegative.build()).rawPoints());

            Sub fill = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").type(QuestionType.FILL_BLANK).exact(false)
                            .rubricPercent(0).build());
            assertEquals(bd("7.50"), grading.grade(fill.build(), policy.build()).rawPoints());

            Sub numeric = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").type(QuestionType.NUMERIC).exact(false)
                            .numericError(bd("12")).numericTolerance(bd("1")).build());
            assertEquals(bd("7.50"), grading.grade(numeric.build(), policy.build()).rawPoints());
        }

        @Test
        @DisplayName("多选题标记精确匹配但未选中任何选项：仍拿满分")
        void multipleChoiceExactWithNoSelections() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(true).correctOptionCount(4)
                    .selectedCorrectOptions(0).selectedIncorrectOptions(0).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), report.rawPoints());

            Pol strict = pol();
            strict.partialCreditEnabled = false;
            GradeReport strictReport = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(true).correctOptionCount(4).build()).build(),
                    strict.build());
            assertEquals(bd("10.00"), strictReport.rawPoints());
        }

        @Test
        @DisplayName("部分给分但没有错选时不应写 ONLY_INCORRECT_OPTIONS_SELECTED")
        void partialCreditWithoutIncorrectOptions() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(2).build()).build(), pol().build());
            assertTrue(report.warnings().isEmpty());
        }

        @Test
        @DisplayName("数值题误差正好等于容差、容差为 0 都拿满分")
        void numericToleranceBoundaries() {
            GradeReport equal = grading.grade(sub().plus(ans()
                    .type(QuestionType.NUMERIC).exact(false).numericError(bd("2"))
                    .numericTolerance(bd("2")).build()).build(), pol().build());
            assertEquals(bd("10.00"), equal.rawPoints());

            GradeReport zero = grading.grade(sub().plus(ans()
                    .type(QuestionType.NUMERIC).exact(false).numericError(bd("0"))
                    .numericTolerance(bd("0")).build()).build(), pol().build());
            assertEquals(bd("10.00"), zero.rawPoints());
        }

        @Test
        @DisplayName("数值题误差正好等于十倍容差：部分给分归零")
        void numericExactlyTenTimesTolerance() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            Sub submission = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").type(QuestionType.NUMERIC).exact(false)
                            .numericError(bd("10")).numericTolerance(bd("1")).build());
            assertEquals(bd("10.00"), grading.grade(submission.build(), policy.build()).rawPoints());
        }

        @Test
        @DisplayName("编程题全部通过且不开启部分给分：仍拿满分")
        void programmingAllPassedWithoutPartialCredit() {
            Pol policy = pol();
            policy.partialCreditEnabled = false;
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).passedTests(2).totalTests(2).build()).build(),
                    policy.build());
            assertEquals(bd("10.00"), report.rawPoints());
        }

        @Test
        @DisplayName("考试时长正好等于上限：不写宽限警告")
        void durationExactlyAllowed() {
            Sub submission = sub().plus(ans().build());
            submission.allowedDurationSeconds = 1800;
            submission.submittedAt = T0.plusSeconds(1800);
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertFalse(report.warnings().contains("EXAM_GRACE_PERIOD_USED"));
            assertEquals(bd("10.00"), report.finalPoints());
        }

        @Test
        @DisplayName("超时正好 300 秒：扣 5% 而不是 20%")
        void overtimeExactlyThreeHundred() {
            Sub submission = sub().plus(ans().build());
            submission.allowedDurationSeconds = 600;
            submission.submittedAt = T0.plusSeconds(900);
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(bd("9.50"), report.finalPoints());
            assertTrue(report.warnings().contains("EXAM_DURATION_MINOR_OVERRUN"));
            assertFalse(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        @DisplayName("百分比 60 但不及格：等级为 D 而不是 F")
        void failingAtSixtyIsGradeD() {
            Pol policy = pol();
            policy.passPercentage = 80;
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = 60;
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertEquals("D", report.letterGrade());
            assertFalse(report.passed());
        }

        @Test
        @DisplayName("合法边界：允许时长 0、覆写 0/100、章节通过率 0/100、策略极值")
        void legalBoundariesAreAccepted() {
            Sub zeroDuration = sub().plus(ans().build());
            zeroDuration.allowedDurationSeconds = 0;
            assertDoesNotThrow(() -> grading.grade(zeroDuration.build(), pol().build()));

            Sub zeroOverride = sub().plus(ans().build());
            zeroOverride.overridePercent = 0;
            assertEquals(0, grading.grade(zeroOverride.build(), pol().build()).percentage());

            Sub hundredOverride = sub().plus(ans().build());
            hundredOverride.overridePercent = 100;
            assertEquals(100, grading.grade(hundredOverride.build(), pol().build()).percentage());

            Sub zeroSection = sub().plus(ans().build());
            zeroSection.sectionPass.put("A", 0);
            assertDoesNotThrow(() -> grading.grade(zeroSection.build(), pol().build()));

            Sub hundredSection = sub().plus(ans().build());
            hundredSection.sectionPass.put("A", 100);
            assertDoesNotThrow(() -> grading.grade(hundredSection.build(), pol().build()));

            Pol extremes = pol();
            extremes.passPercentage = 0;
            extremes.wrongAnswerPenaltyRate = BigDecimal.ZERO;
            extremes.unansweredPenaltyRate = BigDecimal.ONE;
            extremes.wrongAnswerPenaltyRate = BigDecimal.ONE;
            extremes.minimumSectionPercentage = 0;
            extremes.maximumAttempts = 1;
            extremes.latePenaltyPercentPerHour = 0;
            extremes.maximumLatePenaltyPercent = 0;
            assertDoesNotThrow(() -> grading.grade(sub().plus(ans().build()).build(), extremes.build()));

            Pol top = pol();
            top.passPercentage = 100;
            top.minimumSectionPercentage = 100;
            assertDoesNotThrow(() -> grading.grade(sub().plus(ans().build()).build(), top.build()));
        }

        @Test
        @DisplayName("rubricPercent 0 与 100 都是合法值")
        void rubricPercentBoundaries() {
            GradeReport zero = grading.grade(sub().plus(ans()
                    .type(QuestionType.ESSAY).exact(false).rubricPercent(0).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), zero.rawPoints());

            GradeReport full = grading.grade(sub().plus(ans()
                    .type(QuestionType.ESSAY).exact(false).rubricPercent(100).build()).build(),
                    pol().build());
            assertEquals(bd("10.00"), full.rawPoints());
        }
    }

    @Nested
    @DisplayName("Enrollment/Course 边界值攻击")
    class EnrollmentBoundaries {

        private final History history = new History();
        private final EnrollmentEligibilityService service =
                new EnrollmentEligibilityService(history, CLOCK);

        private EnrollmentDecision eval(Stu student, Off course, java.util.function.Consumer<Req> mutation) {
            Req request = new Req();
            request.student = student.build();
            request.course = course.build();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        @Test
        @DisplayName("风险分 90/70/50 三个边界")
        void riskScoreBoundaries() {
            Stu ninety = new Stu();
            ninety.risk = 90;
            assertTrue(eval(ninety, new Off(), null).reasons().contains("RISK_SCORE_TOO_HIGH"));

            Stu seventy = new Stu();
            seventy.risk = 70;
            EnrollmentDecision review = eval(seventy, new Off(), null);
            assertTrue(review.warnings().contains("HIGH_RISK_MANUAL_REVIEW"));
            assertEquals(EnrollmentOutcome.MANUAL_REVIEW, review.outcome());

            Stu fifty = new Stu();
            fifty.risk = 50;
            assertTrue(eval(fifty, new Off(), null).warnings().contains("ELEVATED_RISK_MONITORING"));

            Stu zero = new Stu();
            zero.risk = 0;
            assertDoesNotThrow(() -> eval(zero, new Off(), null));
        }

        @Test
        @DisplayName("直播课空白 scheduleKey 与空白 courseId")
        void blankStrings() {
            Off live = new Off();
            live.liveCourse = true;
            live.scheduleKey = "  ";
            assertTrue(eval(new Stu(), live, null).reasons().contains("LIVE_COURSE_WITHOUT_SCHEDULE"));

            Off blankCourse = new Off();
            blankCourse.courseId = " ";
            assertEquals("courseId must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(new Stu(), blankCourse, null)).getMessage());
        }

        @Test
        @DisplayName("requestedSeats 50 合法、51 非法")
        void seatsBoundary() {
            Stu enterprise = new Stu();
            enterprise.level = MembershipLevel.ENTERPRISE;
            Off big = new Off();
            big.capacity = 100;
            big.enrolled = 0;
            assertDoesNotThrow(() -> eval(enterprise, big, r -> r.seats = 50));
            assertEquals("requestedSeats must not exceed 50",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(enterprise, big, r -> r.seats = 51)).getMessage());
        }

        @Test
        @DisplayName("年龄 17/18 的监护人规则与 0/120 的合法性")
        void ageBoundaries() {
            Stu seventeen = new Stu();
            seventeen.age = 17;
            assertTrue(eval(seventeen, new Off(), null).reasons()
                    .contains("PARENTAL_CONSENT_REQUIRED"));

            Stu eighteen = new Stu();
            eighteen.age = 18;
            assertFalse(eval(eighteen, new Off(), null).reasons()
                    .contains("PARENTAL_CONSENT_REQUIRED"));

            Stu eighteenConsent = new Stu();
            eighteenConsent.age = 18;
            eighteenConsent.parentalConsent = true;
            assertTrue(eval(eighteenConsent, new Off(), null).warnings()
                    .contains("UNNECESSARY_PARENTAL_CONSENT"));

            Stu zero = new Stu();
            zero.age = 0;
            zero.parentalConsent = true;
            assertDoesNotThrow(() -> eval(zero, new Off(), null));

            Stu max = new Stu();
            max.age = 120;
            assertFalse(eval(max, new Off(), null).reasons().contains("INVALID_STUDENT_AGE"));
        }

        @Test
        @DisplayName("年龄限制正好等于上下界时不阻塞")
        void courseAgeBoundaries() {
            Off minimum = new Off();
            minimum.minAge = 20;
            assertFalse(eval(new Stu(), minimum, null).reasons().contains("BELOW_MINIMUM_AGE"));

            Off maximum = new Off();
            maximum.maxAge = 20;
            assertFalse(eval(new Stu(), maximum, null).reasons().contains("ABOVE_MAXIMUM_AGE"));

            Off equal = new Off();
            equal.minAge = 20;
            equal.maxAge = 20;
            assertDoesNotThrow(() -> eval(new Stu(), equal, null));
        }

        @Test
        @DisplayName("容量 0/0 与满员相等的边界")
        void capacityBoundaries() {
            Off empty = new Off();
            empty.capacity = 0;
            empty.enrolled = 0;
            EnrollmentDecision decision = eval(new Stu(), empty, r -> r.acceptWaitlist = true);
            assertEquals(EnrollmentOutcome.WAITLISTED, decision.outcome());
            assertEquals(0, decision.availableSeats());

            Off exact = new Off();
            exact.capacity = 10;
            exact.enrolled = 10;
            assertFalse(eval(new Stu(), exact, r -> r.acceptWaitlist = true).warnings()
                    .contains("COURSE_IS_OVERBOOKED"));

            Off almost = new Off();
            almost.capacity = 11;
            almost.enrolled = 10;
            EnrollmentDecision last = eval(new Stu(), almost, null);
            assertEquals(EnrollmentOutcome.ENROLLED, last.outcome());
            assertTrue(last.warnings().contains("COURSE_ALMOST_FULL"));
        }

        @Test
        @DisplayName("语言与组织的空白边界")
        void languageAndOrganizationBoundaries() {
            Off required = new Off();
            required.requiredLanguage = "fr";
            Stu blankLanguage = new Stu();
            blankLanguage.language = "  ";
            assertTrue(eval(blankLanguage, required, null).reasons()
                    .contains("LANGUAGE_PROFILE_MISSING"));

            Off organization = new Off();
            organization.organizationOnly = true;
            assertTrue(eval(new Stu(), organization, r -> {
                r.sponsored = true;
                r.orgId = "  ";
            }).reasons().contains("ORGANIZATION_ID_REQUIRED"));
        }

        @Test
        @DisplayName("先修课得分 0/100/95 的边界")
        void prerequisiteScoreBoundaries() {
            Off course = new Off();
            course.prereqs = Set.of("pre-1");
            history.completed.add("pre-1");

            history.scores.put("pre-1", 0);
            assertFalse(eval(new Stu(), course, null).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));

            history.scores.put("pre-1", 100);
            assertFalse(eval(new Stu(), course, null).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));

            history.scores.put("pre-1", 95);
            assertTrue(eval(new Stu(), course, null).warnings()
                    .contains("EXCELLENT_PREREQUISITE_SCORE:pre-1"));

            Off minimum = new Off();
            minimum.prereqs = Set.of("pre-1");
            minimum.minPrereqScore = 95;
            history.scores.put("pre-1", 95);
            assertFalse(eval(new Stu(), minimum, null).reasons()
                    .contains("PREREQUISITE_SCORE_TOO_LOW:pre-1"));
            history.completed.clear();
            history.scores.clear();
        }

        @Test
        @DisplayName("候补决策的 accepted()：WAITLISTED 也算接受")
        void waitlistedAccepted() {
            Off full = new Off();
            full.capacity = 10;
            full.enrolled = 10;
            EnrollmentDecision decision = eval(new Stu(), full, r -> r.acceptWaitlist = true);
            assertEquals(EnrollmentOutcome.WAITLISTED, decision.outcome());
            assertTrue(decision.accepted());
            assertFalse(decision.paymentRequired());

            EnrollmentDecision rejected = eval(new Stu(), full, null);
            assertEquals(EnrollmentOutcome.REJECTED, rejected.outcome());
            assertFalse(rejected.accepted());
        }

        @Test
        @DisplayName("CourseService 标题长度 3/80 合法，价格 0/99999.99 合法")
        void courseBoundaries() {
            InMemoryCourseRepository courses = new InMemoryCourseRepository();
            PricePolicy pricePolicy = new PricePolicy();
            IdGenerator ids = () -> "course-1";
            CourseService service = new CourseService(courses, courseId -> List.of(
                    new Chapter("ch1", courseId, "Intro", 1)), ids, pricePolicy, CLOCK);

            assertEquals("abc", service.createCourse("abc", bd("0")).title());
            assertEquals(bd("0.00"), pricePolicy.normalizePrice(bd("0")));

            String eighty = "x".repeat(80);
            assertEquals(80, service.createCourse(eighty, bd("99999.99")).title().length());
            assertEquals(0, pricePolicy.normalizePrice(bd("99999.99")).compareTo(bd("99999.99")));
            assertEquals(bd("0.00"), pricePolicy.discountedPrice(bd("0"), 50));
        }

        @Test
        @DisplayName("CatalogService 空章节与单章节的行为")
        void catalogBoundaries() {
            InMemoryCourseRepository courses = new InMemoryCourseRepository();
            CourseService service = new CourseService(courses, new InMemoryChapterRepository(),
                    () -> "course-1", new PricePolicy(), CLOCK);
            service.createCourse("Intro To Math", bd("10"));
            CatalogService catalogService = new CatalogService(service, new InMemoryChapterRepository(),
                    new InMemoryVideoRepository());
            assertTrue(catalogService.getCatalog("course-1").chapters().isEmpty());
        }
    }

    @Nested
    @DisplayName("AdvancedPricingService 边界值攻击")
    class PricingBoundaries {

        private final AdvancedPricingService service = new AdvancedPricingService(CLOCK);

        private PriceBreakdown price(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.calculate(request.build());
        }

        @Test
        @DisplayName("不可叠加判断：一方为 0 时不触发放弃分支")
        void nonStackableOnlyWhenBothPositive() {
            PriceBreakdown zeroCoupon = price(p -> {
                p.promotion = promotion(PromotionType.FIXED, "40");
                p.coupon = new Coupon("c", CouponType.CASHBACK, bd("10"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("40.00"), zeroCoupon.promotionDiscount());
            assertFalse(zeroCoupon.warnings()
                    .contains("The coupon was ignored because the promotion gives a greater discount"));
        }

        @Test
        @DisplayName("促销与优惠券等额时按 >= 规则丢弃优惠券，并保留其它规则")
        void equalDiscountsKeepPromotion() {
            PriceBreakdown breakdown = price(p -> {
                p.promotion = promotion(PromotionType.FIXED, "20");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("20"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("20.00"), breakdown.promotionDiscount());
            assertEquals(bd("0.00"), breakdown.couponDiscount());
            assertTrue(breakdown.warnings()
                    .contains("The coupon was ignored because the promotion gives a greater discount"));
            assertTrue(breakdown.appliedRules().contains("TAX_CN"));
            assertTrue(breakdown.appliedRules().contains("PROMOTION_FIXED"));
            assertFalse(breakdown.appliedRules().contains("COUPON_FIXED"));

            PriceBreakdown reverse = price(p -> {
                p.promotion = promotion(PromotionType.FIXED, "10");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("40"), null, null, null, 5,
                        false, Set.of(), MembershipLevel.NONE, 0);
            });
            assertTrue(reverse.appliedRules().contains("COUPON_FIXED"));
            assertFalse(reverse.appliedRules().contains("PROMOTION_FIXED"));
            assertTrue(reverse.appliedRules().contains("TAX_CN"));
        }

        @Test
        @DisplayName("单价上限 99999.99 合法")
        void unitPriceUpperBound() {
            PriceBreakdown breakdown = price(p -> p.unitPrice = bd("99999.99"));
            assertEquals(0, breakdown.subtotal().compareTo(bd("99999.99")));
        }

        @Test
        @DisplayName("企业会员 9/10 座的折扣与警告边界")
        void enterpriseQuantityBoundary() {
            PriceBreakdown nine = price(p -> {
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 9;
                p.unitPrice = bd("11.12");
            });
            assertTrue(nine.warnings()
                    .contains("Enterprise discount is reduced for orders below ten seats"));

            PriceBreakdown ten = price(p -> {
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 10;
                p.unitPrice = bd("10.00");
            });
            assertFalse(ten.warnings()
                    .contains("Enterprise discount is reduced for orders below ten seats"));
            assertEquals(bd("18.00"), ten.membershipDiscount());
        }

        @Test
        @DisplayName("BUY_MORE_SAVE_MORE 在 20 座边界为 12%")
        void buyMoreBoundary() {
            assertEquals(bd("12.00"), price(p -> {
                p.promotion = promotion(PromotionType.BUY_MORE_SAVE_MORE, "1");
                p.quantity = 20;
                p.unitPrice = bd("5.00");
            }).promotionDiscount());
        }

        @Test
        @DisplayName("促销上限为 0 时折扣被清零，等额上限不触发警告")
        void promotionCapBoundaries() {
            PriceBreakdown zeroCap = price(p -> p.promotion = new Promotion("promo",
                    PromotionType.PERCENT, bd("50"), null, null, false, 1, BigDecimal.ZERO));
            assertEquals(bd("0.00"), zeroCap.promotionDiscount());
            assertTrue(zeroCap.warnings()
                    .contains("Promotion discount reached its configured cap"));

            PriceBreakdown equalCap = price(p -> p.promotion = new Promotion("promo",
                    PromotionType.FIXED, bd("30"), null, null, false, 1, bd("30.00")));
            assertEquals(bd("30.00"), equalCap.promotionDiscount());
            assertFalse(equalCap.warnings()
                    .contains("Promotion discount reached its configured cap"));

            PriceBreakdown zeroDiscount = price(p -> p.promotion = new Promotion("promo",
                    PromotionType.FIXED, bd("0.004"), null, null, false, 1, null));
            assertEquals(bd("0.00"), zeroDiscount.promotionDiscount());
            assertFalse(zeroDiscount.appliedRules().contains("PROMOTION_FIXED"));
        }

        @Test
        @DisplayName("优惠券 value 为 null、最低消费等额、数量等额、现金返还规则")
        void couponBoundaries() {
            assertTrue(price(p -> p.coupon = new Coupon("c", CouponType.PERCENT, null, null, null,
                    null, 5, true, Set.of(), MembershipLevel.NONE, 0)).warnings()
                    .contains("Coupon configuration is incomplete"));

            assertEquals(bd("50.00"), price(p -> p.coupon = new Coupon("c", CouponType.PERCENT,
                    bd("50"), bd("100"), null, null, 5, true, Set.of(), MembershipLevel.NONE, 0))
                    .couponDiscount());

            assertEquals(bd("50.00"), price(p -> {
                p.quantity = 5;
                p.unitPrice = bd("20.00");
                p.coupon = new Coupon("c", CouponType.PERCENT, bd("50"), null, null, null, 5,
                        true, Set.of(), MembershipLevel.NONE, 5);
            }).couponDiscount());

            PriceBreakdown cashback = price(p -> p.coupon = coupon(CouponType.CASHBACK, "10"));
            assertFalse(cashback.appliedRules().contains("COUPON_CASHBACK"));
            assertTrue(cashback.appliedRules().contains("COUPON_CASHBACK_FUTURE"));
        }

        @Test
        @DisplayName("积分抵扣边界：0 积分无警告、可用额度等额、100 分起、20% 上限等额")
        void loyaltyBoundaries() {
            PriceBreakdown zero = price(p -> p.loyaltyPoints = 0);
            assertTrue(zero.warnings().isEmpty());
            assertEquals(bd("0.00"), zero.loyaltyDiscount());

            PriceBreakdown equalBalance = price(p -> {
                p.loyaltyPoints = 500;
                p.availablePoints = 500;
            });
            assertFalse(equalBalance.warnings()
                    .contains("Requested loyalty points were reduced to the available balance"));

            PriceBreakdown hundred = price(p -> {
                p.loyaltyPoints = 100;
                p.availablePoints = 100;
            });
            assertEquals(bd("1.00"), hundred.loyaltyDiscount());
            assertFalse(hundred.warnings().contains("At least 100 loyalty points are required"));

            PriceBreakdown capEqual = price(p -> {
                p.loyaltyPoints = 2000;
                p.availablePoints = 2000;
            });
            assertEquals(bd("20.00"), capEqual.loyaltyDiscount());
            assertFalse(capEqual.warnings()
                    .contains("Loyalty discount was limited to twenty percent"));
        }

        @Test
        @DisplayName("公益免税国家对无税率国家不写减税规则")
        void nonprofitWithoutTaxRate() {
            PriceBreakdown breakdown = price(p -> {
                p.nonprofit = true;
                p.country = "XX";
            });
            assertFalse(breakdown.appliedRules().contains("NONPROFIT_TAX_REDUCTION"));
            assertTrue(breakdown.warnings().contains("No tax rule is configured for country XX"));
        }
    }

    @Nested
    @DisplayName("剩余存活变异专项攻击（第二轮）")
    class ResidualAttacks {

        @Test
        @DisplayName("反射直调 scoreAnswer：rubricPercent=125 触发 clamp 上界")
        void clampUpperBoundViaReflection() throws Exception {
            Method method = ExamGradingService.class.getDeclaredMethod(
                    "scoreAnswer", AnswerResult.class, GradingPolicy.class, List.class);
            method.setAccessible(true);
            List<String> warnings = new ArrayList<>();
            Object score = method.invoke(grading, ans().type(QuestionType.ESSAY).exact(false)
                    .rubricPercent(125).build(), pol().build(), warnings);
            assertEquals(bd("10.00"), score);
        }

        @Test
        @DisplayName("多选题部分给分且全未选：0 分且不写 ONLY_INCORRECT_OPTIONS_SELECTED")
        void multipleChoiceNoSelectionsNoWarning() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.MULTIPLE_CHOICE).exact(false).correctOptionCount(4)
                    .selectedCorrectOptions(0).selectedIncorrectOptions(0).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertTrue(report.warnings().isEmpty());
        }

        @Test
        @DisplayName("多选题错选数为 0 时负分分支不应生效")
        void multipleChoiceZeroIncorrectNoPenalty() {
            Pol policy = pol();
            policy.partialCreditEnabled = false;
            policy.negativeMarkingEnabled = true;
            Sub submission = sub()
                    .plus(ans().id("q1").build())
                    .plus(ans().id("q2").type(QuestionType.MULTIPLE_CHOICE).exact(false)
                            .correctOptionCount(4).selectedIncorrectOptions(0).build());
            assertEquals(bd("10.00"), grading.grade(submission.build(), policy.build()).rawPoints());
        }

        @Test
        @DisplayName("允许时长为 0 时不进入超时惩罚分支")
        void zeroAllowedDurationSkipsPenalty() {
            Sub submission = sub().plus(ans().build());
            submission.allowedDurationSeconds = 0;
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(bd("10.00"), report.finalPoints());
            assertFalse(report.failures().contains("EXAM_DURATION_EXCEEDED"));
        }

        @Test
        @DisplayName("及格线为 0 且百分比 55：passed 为 true 但等级仍是 F")
        void lowestLetterGradeF() {
            Pol policy = pol();
            policy.passPercentage = 0;
            Sub submission = sub().plus(ans().build());
            submission.overridePercent = 55;
            GradeReport report = grading.grade(submission.build(), policy.build());
            assertTrue(report.passed());
            assertEquals("F", report.letterGrade());
        }

        @Test
        @DisplayName("罚分率取 0 合法（边界 <=0 会误判）")
        void zeroPenaltyRateIsLegal() {
            Pol policy = pol();
            policy.negativeMarkingEnabled = true;
            policy.wrongAnswerPenaltyRate = BigDecimal.ZERO;
            assertDoesNotThrow(() -> grading.grade(
                    sub().plus(ans().exact(false).build()).build(), policy.build()));
        }

        @Test
        @DisplayName("studentId 为空字符串（非 null）同样被拒绝")
        void blankStudentIdString() {
            Sub submission = sub().plus(ans().build());
            submission.studentId = "";
            assertEquals("studentId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(submission.build(), pol().build())).getMessage());
            Sub exam = sub().plus(ans().build());
            exam.examId = "";
            assertEquals("examId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(exam.build(), pol().build())).getMessage());
        }

        @Test
        @DisplayName("促销为 0 折扣时不可叠加分支不触发")
        void zeroPromotionDoesNotTriggerNonStackable() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.promotion = new Promotion("promo", PromotionType.FIXED, bd("0.004"), null,
                    null, false, 1, null);
            request.coupon = new Coupon("c", CouponType.FIXED, bd("40"), null, null, null, 5,
                    false, Set.of(), MembershipLevel.NONE, 0);
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("40.00"), breakdown.couponDiscount());
            assertFalse(breakdown.warnings()
                    .contains("The promotion was ignored because the coupon gives a greater discount"));
        }

        @Test
        @DisplayName("排课缓冲：结束后的缓冲仍然生效")
        void bufferAfterEndStillConflicts() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch request = new Sch();
            request.buffer = 60;
            request.bookings = List.of(booking("b1", "i1", "R9", "h9",
                    Instant.parse("2026-11-16T03:30:00Z"),
                    Instant.parse("2026-11-16T03:40:00Z")));
            assertTrue(service.evaluate(request.build()).conflicts()
                    .contains("INSTRUCTOR_BOOKING_CONFLICT:b1"));
        }

        @Test
        @DisplayName("排课每日课时：8 小时加法边界")
        void dailyHoursAdditionBoundary() {
            SchedulingService service = new SchedulingService(CLOCK);
            List<ExistingBooking> four = List.of(
                    booking("h1", "i1", "R9", "h9", Instant.parse("2026-11-16T00:00:00Z"),
                            Instant.parse("2026-11-16T02:00:00Z")),
                    booking("h2", "i1", "R9", "h9", Instant.parse("2026-11-16T02:00:00Z"),
                            Instant.parse("2026-11-16T04:00:00Z")),
                    booking("h3", "i1", "R9", "h9", Instant.parse("2026-11-16T04:00:00Z"),
                            Instant.parse("2026-11-16T06:00:00Z")),
                    booking("h4", "i1", "R9", "h9", Instant.parse("2026-11-16T06:00:00Z"),
                            Instant.parse("2026-11-16T08:00:00Z")));
            Sch request = new Sch();
            request.zone = "UTC";
            request.start("2026-11-16T10:00:00Z", 60);
            request.maxSessions = 10;
            request.bookings = four;
            assertTrue(service.evaluate(request.build()).conflicts()
                    .contains("INSTRUCTOR_DAILY_HOURS_EXCEEDED"));
        }

        @Test
        @DisplayName("空白 roomId 与 meetingUrl 的判定")
        void blankResources() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch classroom = new Sch();
            classroom.delivery = DeliveryMode.CLASSROOM;
            classroom.roomCapacity = 50;
            classroom.roomId = "  ";
            assertTrue(service.evaluate(classroom.build()).conflicts().contains("ROOM_REQUIRED"));

            Sch hybrid = new Sch();
            hybrid.delivery = DeliveryMode.HYBRID;
            hybrid.roomCapacity = 50;
            hybrid.roomId = "R1";
            hybrid.meetingUrl = "  ";
            assertTrue(service.evaluate(hybrid.build()).conflicts()
                    .contains("HYBRID_MEETING_URL_REQUIRED"));
        }

        @Test
        @DisplayName("重复 52 次系列进入人工复核、20 次不进入")
        void recurrenceReviewBoundaries() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch fiftyTwo = new Sch();
            fiftyTwo.recurring = true;
            fiftyTwo.occurrences = 52;
            SchedulingDecision decision = service.evaluate(fiftyTwo.build());
            assertTrue(decision.manualReview());
            assertFalse(decision.approved());

            Sch twenty = new Sch();
            twenty.recurring = true;
            twenty.occurrences = 20;
            assertFalse(service.evaluate(twenty.build()).manualReview());
        }

        @Test
        @DisplayName("不重叠的预约不产生冲突")
        void nonOverlappingBookingsDoNotConflict() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch request = new Sch();
            request.bookings = List.of(booking("b1", "i1", "R9", "h9",
                    Instant.parse("2026-11-16T05:00:00Z"),
                    Instant.parse("2026-11-16T06:00:00Z")));
            assertTrue(service.evaluate(request.build()).conflicts().isEmpty());
        }

        @Test
        @DisplayName("退款时间窗 336/720 小时精确边界")
        void refundWindowExactBoundaries() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur fourteen = new Pur();
            fourteen.paidAt = T0.minusSeconds(336 * 3600);
            fourteen.progress = 20;
            Ref request = new Ref();
            request.purchase = fourteen.build();
            RefundDecision fourteenDecision = service.evaluate(request.build());
            assertTrue(fourteenDecision.reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));

            Pur thirty = new Pur();
            thirty.paidAt = T0.minusSeconds(720 * 3600);
            thirty.progress = 10;
            Ref thirtyRequest = new Ref();
            thirtyRequest.purchase = thirty.build();
            RefundDecision thirtyDecision = service.evaluate(thirtyRequest.build());
            assertTrue(thirtyDecision.reasons().contains("THIRTY_DAY_EXCEPTION"));
        }

        @Test
        @DisplayName("零下载时误购不再被下载拒绝")
        void accidentalPurchaseWithoutDownload() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.paidAt = T0.minusSeconds(800 * 3600);
            purchase.downloads = 0;
            Ref request = new Ref();
            request.purchase = purchase.build();
            RefundDecision decision = service.evaluate(request.build());
            assertFalse(decision.reasons().contains("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"));
        }

        @Test
        @DisplayName("顺序学习中未完成前一课时 nextLessonId 仍指向它")
        void sequentialNextLessonPointer() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 50;
            snapshot.lesson(new Les().id("l1").order(1).completion(49).build());
            snapshot.lesson(new Les().id("l2").order(2).completion(10).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertEquals("l1", evaluation.nextLessonId());
            assertFalse(evaluation.courseCompleted());
        }

        @Test
        @DisplayName("毕业项目与教师审批阻塞项在总进度达标时仍阻止完成")
        void academicBlockersFinalProjectAndApproval() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap project = new Snap();
            project.requiredOverallPercent = 0;
            project.finalProjectRequired = true;
            project.lesson(new Les().build());
            assertFalse(service.evaluate(project.build()).courseCompleted());

            Snap approval = new Snap();
            approval.requiredOverallPercent = 0;
            approval.instructorApprovalRequired = true;
            approval.lesson(new Les().type(LessonType.ASSIGNMENT).mandatory(true)
                    .approved(false).build());
            assertFalse(service.evaluate(approval.build()).courseCompleted());
        }

        @Test
        @DisplayName("attempts 1 且 maximumAttempts 0 属于合法配置")
        void attemptsWithoutLimit() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().attempts(1).maximumAttempts(0).build());
            assertDoesNotThrow(() -> service.evaluate(snapshot.build()));
        }

        @Test
        @DisplayName("选课：风险分 100、enrolled 负数、年龄 0/120 的边界")
        void enrollmentNumericBoundaries() {
            History history = new History();
            EnrollmentEligibilityService service = new EnrollmentEligibilityService(history, CLOCK);

            Stu hundred = new Stu();
            hundred.risk = 100;
            Req hundredRequest = new Req();
            hundredRequest.student = hundred.build();
            assertDoesNotThrow(() -> service.evaluate(hundredRequest.build()));

            Off negativeEnrolled = new Off();
            negativeEnrolled.enrolled = -1;
            Req negativeRequest = new Req();
            negativeRequest.course = negativeEnrolled.build();
            assertEquals("course capacity values must not be negative",
                    assertThrows(IllegalArgumentException.class,
                            () -> service.evaluate(negativeRequest.build())).getMessage());

            Stu zeroAge = new Stu();
            zeroAge.age = 0;
            Req zeroRequest = new Req();
            zeroRequest.student = zeroAge.build();
            assertFalse(service.evaluate(zeroRequest.build()).reasons()
                    .contains("INVALID_STUDENT_AGE"));

            Stu maxAge = new Stu();
            maxAge.age = 120;
            Req maxRequest = new Req();
            maxRequest.student = maxAge.build();
            assertFalse(service.evaluate(maxRequest.build()).reasons()
                    .contains("INVALID_STUDENT_AGE"));
        }

        @Test
        @DisplayName("选课：年龄上下限与先修课得分下限的等值边界")
        void enrollmentEqualityBoundaries() {
            History history = new History();
            EnrollmentEligibilityService service = new EnrollmentEligibilityService(history, CLOCK);

            Off minimum = new Off();
            minimum.minAge = 20;
            Req minimumRequest = new Req();
            minimumRequest.course = minimum.build();
            assertFalse(service.evaluate(minimumRequest.build()).reasons()
                    .contains("BELOW_MINIMUM_AGE"));

            Off maximum = new Off();
            maximum.maxAge = 20;
            Req maximumRequest = new Req();
            maximumRequest.course = maximum.build();
            assertFalse(service.evaluate(maximumRequest.build()).reasons()
                    .contains("ABOVE_MAXIMUM_AGE"));

            Off prerequisite = new Off();
            prerequisite.prereqs = Set.of("pre-1");
            prerequisite.minPrereqScore = 95;
            history.completed.add("pre-1");
            history.scores.put("pre-1", 95);
            Req prerequisiteRequest = new Req();
            prerequisiteRequest.course = prerequisite.build();
            assertFalse(service.evaluate(prerequisiteRequest.build()).reasons()
                    .contains("PREREQUISITE_SCORE_TOO_LOW:pre-1"));
            history.completed.clear();
            history.scores.clear();
        }
    }

    @Nested
    @DisplayName("第三轮碎片补齐")
    class FinalFragments {

        @Test
        @DisplayName("优惠券折扣为 0 时不写 COUPON_ 规则")
        void zeroCouponDiscountRule() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.coupon = new Coupon("c", CouponType.FIXED, bd("0.004"), null, null, null, 5,
                    true, Set.of(), MembershipLevel.NONE, 0);
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("0.00"), breakdown.couponDiscount());
            assertFalse(breakdown.appliedRules().contains("COUPON_FIXED"));
        }

        @Test
        @DisplayName("订单金额为 0 时积分抵扣直接返回 0，不产生 20% 上限警告")
        void zeroAmountSkipsLoyalty() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.unitPrice = BigDecimal.ZERO;
            request.loyaltyPoints = 500;
            request.availablePoints = 500;
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("0.00"), breakdown.loyaltyDiscount());
            assertFalse(breakdown.warnings()
                    .contains("Loyalty discount was limited to twenty percent"));
            assertFalse(breakdown.warnings().contains("At least 100 loyalty points are required"));
        }

        @Test
        @DisplayName("表达式误差正好等于容差：判定为 WITHIN_TOLERANCE")
        void expressionToleranceEquality() {
            ExpressionAnswerGradingService service = new ExpressionAnswerGradingService(
                    expression -> {
                        if ("10".equals(expression)) {
                            return bd("10");
                        }
                        return bd("11");
                    });
            ExpressionAnswerEvaluation evaluation = service.evaluate(new ExpressionAnswerSubmission(
                    new ExpressionQuestion("q1", "math", "10", bd("10.00"), bd("1"), true), "11"));
            assertEquals(ExpressionAnswerStatus.WITHIN_TOLERANCE, evaluation.status());
            assertEquals(0, evaluation.absoluteError().compareTo(bd("1")));
        }

        @Test
        @DisplayName("提供方责任在 24 小时之后同样免手续费（避免冷静期掩盖）")
        void providerFaultFeeBeyondOneDay() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur removed = new Pur();
            removed.paidAt = T0.minusSeconds(100 * 3600);
            removed.courseRemoved = true;
            Ref removedRequest = new Ref();
            removedRequest.purchase = removed.build();
            assertEquals(bd("0.00"), service.evaluate(removedRequest.build()).processingFee());

            Pur duplicate = new Pur();
            duplicate.paidAt = T0.minusSeconds(100 * 3600);
            Ref refundRequest = new Ref();
            refundRequest.purchase = duplicate.build();
            refundRequest.reason = RefundReason.DUPLICATE_PAYMENT;
            refundRequest.duplicateConfirmed = true;
            assertEquals(bd("0.00"), service.evaluate(refundRequest.build()).processingFee());

            Pur technical = new Pur();
            technical.paidAt = T0.minusSeconds(100 * 3600);
            technical.method = PaymentMethod.CRYPTO;
            Ref technicalRequest = new Ref();
            technicalRequest.purchase = technical.build();
            technicalRequest.reason = RefundReason.TECHNICAL_ISSUE;
            technicalRequest.technicalConfirmed = true;
            assertEquals(bd("0.00"), service.evaluate(technicalRequest.build()).processingFee());
        }

        @Test
        @DisplayName("礼物购买渠道只要 giftPurchase 为真即走钱包额度（不看支付方式）")
        void giftPurchaseRouteIgnoresPaymentMethod() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.gift = true;
            purchase.method = PaymentMethod.WECHAT;
            Ref request = new Ref();
            request.purchase = purchase.build();
            RefundDecision decision = service.evaluate(request.build());
            assertEquals("WALLET_CREDIT", decision.refundRoute());
            assertTrue(decision.warnings().contains("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        @Test
        @DisplayName("退款：优惠券折抵等于可退金额时不会超额扣减")
        void couponAdjustmentEqualToRefundable() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.couponUsed = true;
            purchase.discountAmount = bd("100.00");
            Ref request = new Ref();
            request.purchase = purchase.build();
            RefundDecision decision = service.evaluate(request.build());
            assertTrue(decision.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
            assertEquals(bd("0.00"), decision.refundableAmount());
        }

        @Test
        @DisplayName("排课：非覆盖冲突在无冲突时保持空列表")
        void schedulingNoFalsePositiveFromOverlaps() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch request = new Sch();
            request.bookings = List.of(booking("b1", "i2", "R2", "h2",
                    Instant.parse("2026-11-16T00:30:00Z"),
                    Instant.parse("2026-11-16T01:30:00Z")));
            SchedulingDecision decision = service.evaluate(request.build());
            assertTrue(decision.conflicts().isEmpty());
            assertTrue(decision.approved());
        }
    }

    @Nested
    @DisplayName("LearningProgressService 边界值攻击")
    class ProgressBoundaries {

        private final LearningProgressService service = new LearningProgressService(CLOCK);

        private ProgressEvaluation eval(java.util.function.Consumer<Snap> mutation) {
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().build());
            if (mutation != null) {
                mutation.accept(snapshot);
            }
            return service.evaluate(snapshot.build());
        }

        @Test
        @DisplayName("锁定但 0 进度不写 LOCKED_LESSON_HAS_PROGRESS")
        void lockedWithoutProgress() {
            ProgressEvaluation evaluation = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().unlocked(false).completion(0).build())));
            assertFalse(evaluation.warnings().contains("LOCKED_LESSON_HAS_PROGRESS:l1"));
        }

        @Test
        @DisplayName("视频课 70/90 的接近完成边界")
        void videoNearCompletionBoundaries() {
            assertTrue(eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(70).build()))).warnings()
                    .contains("VIDEO_NEAR_COMPLETION:l1"));
            assertFalse(eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(90).build()))).warnings()
                    .contains("VIDEO_NEAR_COMPLETION:l1"));
            assertFalse(eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(69).build()))).warnings()
                    .contains("VIDEO_NEAR_COMPLETION:l1"));
        }

        @Test
        @DisplayName("文章课 99 未完成、100 完成")
        void articleBoundary() {
            ProgressEvaluation ninetyNine = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ARTICLE).completion(99).build())));
            assertEquals(0, ninetyNine.completedLessons());
            ProgressEvaluation hundred = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.ARTICLE).completion(100).build())));
            assertEquals(1, hundred.completedLessons());
        }

        @Test
        @DisplayName("测验 60 分通过、59 分不通过")
        void quizScoreBoundary() {
            ProgressEvaluation sixty = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.QUIZ).score(60).build())));
            assertEquals(1, sixty.completedLessons());
            assertFalse(sixty.warnings().contains("QUIZ_SCORE_TOO_LOW:l1"));

            ProgressEvaluation fiftyNine = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.QUIZ).score(59).build())));
            assertEquals(0, fiftyNine.completedLessons());
            assertTrue(fiftyNine.warnings().contains("QUIZ_SCORE_TOO_LOW:l1"));
        }

        @Test
        @DisplayName("直播课出勤 0 与 1 分钟的边界")
        void liveAttendanceBoundary() {
            ProgressEvaluation zero = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(0).attended(0).build())));
            assertEquals(0, zero.completedLessons());
            assertFalse(zero.warnings().contains("LIVE_ATTENDANCE_TOO_SHORT:l1"));

            ProgressEvaluation one = eval(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.LIVE_SESSION).required(0).attended(1).build())));
            assertEquals(1, one.completedLessons());
        }

        @Test
        @DisplayName("顺序学习：必修完成后仍要继承前一课状态")
        void sequentialMandatoryChain() {
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 0;
            snapshot.lesson(new Les().id("l1").order(1).completion(10).build());
            snapshot.lesson(new Les().id("l2").order(2).mandatory(true).completion(100).build());
            snapshot.lesson(new Les().id("l3").order(3).completion(100).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l3"));

            Snap boundary = new Snap();
            boundary.sequential = true;
            boundary.requiredOverallPercent = 0;
            boundary.lesson(new Les().id("l1").order(1).completion(49).build());
            boundary.lesson(new Les().id("l2").order(2).completion(100).build());
            assertTrue(service.evaluate(boundary.build()).warnings()
                    .contains("SEQUENCE_RULE_BYPASSED:l2"));

            Snap exact = new Snap();
            exact.sequential = true;
            exact.requiredOverallPercent = 0;
            exact.lesson(new Les().id("l1").order(1).completion(50).build());
            exact.lesson(new Les().id("l2").order(2).completion(100).build());
            assertFalse(service.evaluate(exact.build()).warnings()
                    .contains("SEQUENCE_RULE_BYPASSED:l2"));
        }

        @Test
        @DisplayName("访问到期正好 7 天仍提示，6 天提示")
        void accessExpiryBoundary() {
            assertTrue(eval(s -> s.accessExpiresAt = T0.plusSeconds(7 * 86400L)).warnings()
                    .contains("COURSE_ACCESS_EXPIRES_SOON"));
            assertTrue(eval(s -> s.accessExpiresAt = T0.plusSeconds(6 * 86400L)).warnings()
                    .contains("COURSE_ACCESS_EXPIRES_SOON"));
            assertFalse(eval(s -> s.accessExpiresAt = T0.plusSeconds(8 * 86400L)).warnings()
                    .contains("COURSE_ACCESS_EXPIRES_SOON"));
        }

        @Test
        @DisplayName("证书资格各失败分支都返回 false")
        void certificateFailuresAreNotEligible() {
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.lessons = new ArrayList<>(List.of(new Les().completion(10).build()));
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.identityVerified = false;
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.outstandingBalance = true;
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.certificateName = null;
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.certificateName = " ";
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.certificateName = "x";
            }).certificateEligible());
            assertFalse(eval(s -> {
                s.certificateEnabled = true;
                s.lesson(new Les().build());
                s.lessons = new ArrayList<>(List.of(new Les().completion(10).build()));
            }).certificateEligible());
        }

        @Test
        @DisplayName("证书名长度 2/80 合法")
        void certificateNameBoundaries() {
            assertTrue(eval(s -> {
                s.certificateEnabled = true;
                s.certificateName = "ab";
            }).certificateEligible());
            assertTrue(eval(s -> {
                s.certificateEnabled = true;
                s.certificateName = "x".repeat(80);
            }).certificateEligible());
        }

        @Test
        @DisplayName("毕业项目已通过、必修作业已通过时不再阻塞")
        void approvedProjectAndAssignment() {
            ProgressEvaluation project = eval(s -> {
                s.finalProjectRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.PROJECT).build()));
            });
            assertFalse(project.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            ProgressEvaluation assignment = eval(s -> {
                s.instructorApprovalRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.ASSIGNMENT)
                        .mandatory(true).approved(true).build()));
            });
            assertFalse(assignment.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));
        }

        @Test
        @DisplayName("学术类阻塞项在总进度达标时仍阻止完成")
        void academicBlockersWhenProgressSufficient() {
            ProgressEvaluation plagiarism = eval(s -> {
                s.requiredOverallPercent = 0;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.PROJECT)
                        .plagiarism(true).build()));
            });
            assertFalse(plagiarism.courseCompleted());

            ProgressEvaluation attempts = eval(s -> {
                s.requiredOverallPercent = 0;
                s.lessons = new ArrayList<>(List.of(new Les().completion(10).attempts(1)
                        .maximumAttempts(1).build()));
            });
            assertFalse(attempts.courseCompleted());
        }

        @Test
        @DisplayName("校验边界：空白 courseId、0/100 的百分比、order 0、attempts 等额")
        void validationBoundaries() {
            Snap blankCourse = new Snap();
            blankCourse.lesson(new Les().build());
            blankCourse.courseId = " ";
            assertEquals("courseId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> service.evaluate(blankCourse.build())).getMessage());

            Snap zero = new Snap();
            zero.lesson(new Les().order(0).build());
            zero.requiredOverallPercent = 0;
            zero.requiredQuizScore = 0;
            assertDoesNotThrow(() -> service.evaluate(zero.build()));

            Snap hundred = new Snap();
            hundred.lesson(new Les().build());
            hundred.requiredOverallPercent = 100;
            hundred.requiredQuizScore = 100;
            assertDoesNotThrow(() -> service.evaluate(hundred.build()));

            Snap attempts = new Snap();
            attempts.lesson(new Les().attempts(2).maximumAttempts(2).build());
            assertDoesNotThrow(() -> service.evaluate(attempts.build()));
        }
    }

    @Nested
    @DisplayName("RefundPolicyService 边界值攻击")
    class RefundBoundaries {

        private final RefundPolicyService service = new RefundPolicyService(CLOCK);

        private RefundDecision refund(java.util.function.Consumer<Pur> purchaseMutation,
                                      java.util.function.Consumer<Ref> requestMutation) {
            Pur purchase = new Pur();
            if (purchaseMutation != null) {
                purchaseMutation.accept(purchase);
            }
            Ref request = new Ref();
            request.purchase = purchase.build();
            if (requestMutation != null) {
                requestMutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        private RefundDecision refund(java.util.function.Consumer<Pur> purchaseMutation) {
            return refund(purchaseMutation, null);
        }

        @Test
        @DisplayName("购买时间正好是现在：仍按冷静期全额退款")
        void purchaseExactlyNow() {
            RefundDecision decision = refund(p -> p.paidAt = T0, null);
            assertTrue(decision.approved());
            assertTrue(decision.reasons().contains("COOLING_OFF_PERIOD"));
        }

        @Test
        @DisplayName("退款金额正好 1.00：不落入小额退款分支")
        void refundableExactlyOne() {
            RefundDecision decision = refund(p -> p.paidAmount = bd("1.00"), null);
            assertEquals(bd("1.00"), decision.refundableAmount());
            assertTrue(decision.approved());
            assertFalse(decision.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));
            assertFalse(decision.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        @DisplayName("优惠券折抵金额正好等于可退金额：全额被吞掉")
        void couponAdjustmentExact() {
            RefundDecision decision = refund(p -> {
                p.couponUsed = true;
                p.discountAmount = bd("100.00");
            }, r -> r.reason = RefundReason.OTHER);
            assertTrue(decision.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
            assertEquals(bd("0.00"), decision.refundableAmount());
        }

        @Test
        @DisplayName("技术问题进度 50 与医疗进度 80、内容质量进度 60 的边界")
        void reasonProgressBoundaries() {
            assertEquals(bd("100.00"), refund(p -> p.progress = 50, r -> {
                r.reason = RefundReason.TECHNICAL_ISSUE;
                r.technicalConfirmed = true;
            }).refundableAmount());

            assertEquals(bd("40.00"), refund(p -> p.progress = 80, r -> {
                r.reason = RefundReason.MEDICAL;
                r.medicalVerified = true;
            }).refundableAmount());
            assertEquals(bd("100.00"), refund(p -> p.progress = 79, r -> {
                r.reason = RefundReason.MEDICAL;
                r.medicalVerified = true;
            }).refundableAmount());

            assertEquals(bd("60.00"), refund(p -> p.progress = 60, r -> {
                r.reason = RefundReason.CONTENT_QUALITY;
                r.contentUpheld = true;
            }).refundableAmount());
            assertEquals(bd("100.00"), refund(p -> p.progress = 59, r -> {
                r.reason = RefundReason.CONTENT_QUALITY;
                r.contentUpheld = true;
            }).refundableAmount());
        }

        @Test
        @DisplayName("时间窗精确边界：2h/24h/168h/336h/720h")
        void timeWindowBoundaries() {
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(2 * 3600), null).reasons()
                    .contains("COOLING_OFF_PERIOD"));
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(3 * 3600), null).reasons()
                    .contains("ONE_DAY_LOW_USAGE"));
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(24 * 3600), null).reasons()
                    .contains("ONE_DAY_LOW_USAGE"));
            assertTrue(refund(p -> p.paidAt = T0.minusSeconds(168 * 3600), null).reasons()
                    .contains("SEVEN_DAY_GUARANTEE"));
            assertTrue(refund(p -> {
                p.paidAt = T0.minusSeconds(169 * 3600);
                p.progress = 20;
            }).reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));
            RefundDecision fourteen = refund(p -> {
                p.paidAt = T0.minusSeconds(400 * 3600);
                p.progress = 10;
            });
            assertTrue(fourteen.reasons().contains("THIRTY_DAY_EXCEPTION"));
            RefundDecision sevenDayThirty = refund(p -> {
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.progress = 30;
            });
            assertEquals(bd("67.90"), sevenDayThirty.refundableAmount());
        }

        @Test
        @DisplayName("24 小时内免手续费，超过后按支付方式收费")
        void feeTimeBoundary() {
            assertEquals(bd("0.00"), refund(p -> {
                p.method = PaymentMethod.CARD;
                p.paidAt = T0.minusSeconds(24 * 3600);
            }, null).processingFee());
            assertEquals(bd("3.00"), refund(p -> {
                p.method = PaymentMethod.CARD;
                p.paidAt = T0.minusSeconds(25 * 3600);
                p.progress = 10;
            }, null).processingFee());
        }

        @Test
        @DisplayName("直播出勤 0 与 20% 的边界")
        void liveAttendanceFeeBoundary() {
            assertEquals(bd("1.00"), refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.attendedSessions = 0;
                p.totalSessions = 4;
            }, null).processingFee());

            RefundDecision five = refund(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.attendedSessions = 1;
                p.totalSessions = 5;
            });
            assertEquals(bd("6.00"), five.processingFee());
            assertTrue(five.warnings().contains("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));
        }

        @Test
        @DisplayName("重复支付确认属于提供方责任，手续费为 0")
        void duplicatePaymentIsProviderFault() {
            RefundDecision decision = refund(null, r -> {
                r.reason = RefundReason.DUPLICATE_PAYMENT;
                r.duplicateConfirmed = true;
                r.fullRefund = true;
            });
            assertEquals(bd("0.00"), decision.processingFee());
        }

        @Test
        @DisplayName("校验边界：金额 0 合法，空白字段与负金额非法")
        void validationBoundaries() {
            assertDoesNotThrow(() -> refund(p -> {
                p.paidAmount = BigDecimal.ZERO;
                p.taxAmount = BigDecimal.ZERO;
                p.discountAmount = BigDecimal.ZERO;
            }, null));

            assertEquals("studentId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> refund(p -> p.studentId = " ", null)).getMessage());
            assertEquals("paidAmount must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> refund(p -> p.paidAmount = bd("-0.01"), null)).getMessage());
            assertEquals("taxAmount must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> refund(p -> p.taxAmount = bd("-0.01"), null)).getMessage());
            assertEquals("discountAmount must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> refund(p -> p.discountAmount = bd("-0.01"), null)).getMessage());
        }
    }

    @Nested
    @DisplayName("SchedulingService 边界值攻击")
    class SchedulingBoundaries {

        private final SchedulingService service = new SchedulingService(CLOCK);

        private SchedulingDecision eval(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            if (mutation != null) {
                mutation.accept(request);
            }
            return service.evaluate(request.build());
        }

        @Test
        @DisplayName("提前 2 小时不再报 INSUFFICIENT_BOOKING_NOTICE")
        void leadTimeBoundary() {
            SchedulingDecision twoHours = eval(s -> s.start(T0.plusSeconds(2 * 3600).toString(), 60));
            assertFalse(twoHours.conflicts().contains("INSUFFICIENT_BOOKING_NOTICE"));
            assertTrue(twoHours.warnings().contains("SHORT_BOOKING_NOTICE"));

            SchedulingDecision exactlyDay = eval(s -> s.start(T0.plusSeconds(24 * 3600).toString(), 60));
            assertFalse(exactlyDay.warnings().contains("SHORT_BOOKING_NOTICE"));
        }

        @Test
        @DisplayName("营业结束正好 21:00 不算超出营业时间")
        void closingTimeBoundary() {
            SchedulingDecision decision = eval(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T20:00:00Z", 60);
            });
            assertFalse(decision.conflicts().contains("OUTSIDE_OPERATING_HOURS"));
        }

        @Test
        @DisplayName("讲座时长 30/180 边界")
        void lectureDurationBoundaries() {
            assertFalse(eval(s -> s.start(MON_10.toString(), 30)).conflicts()
                    .contains("LECTURE_TOO_SHORT"));
            SchedulingDecision long180 = eval(s -> {
                s.start(MON_10.toString(), 180);
                s.lunchAllowed = true;
            });
            assertFalse(long180.warnings().contains("LECTURE_LONGER_THAN_RECOMMENDED"));
        }

        @Test
        @DisplayName("实验课 240、考试 20/300、工作坊 45/360、网络研讨会 15/120 边界")
        void kindDurationBoundaries() {
            SchedulingDecision lab = eval(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 240);
                s.lunchAllowed = true;
                s.availableEquipment = Set.of("LAB_SAFETY_KIT");
            });
            assertFalse(lab.conflicts().contains("LAB_TOO_LONG"));

            SchedulingDecision exam = eval(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 20);
                s.availableEquipment = Set.of("PROCTORING");
            });
            assertFalse(exam.conflicts().contains("EXAM_TOO_SHORT"));

            SchedulingDecision examLong = eval(s -> {
                s.kind = CourseKind.EXAM;
                s.start(MON_10.toString(), 300);
                s.lunchAllowed = true;
                s.availableEquipment = Set.of("PROCTORING");
            });
            assertFalse(examLong.conflicts().contains("EXAM_TOO_LONG"));

            SchedulingDecision workshop = eval(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 45);
            });
            assertFalse(workshop.conflicts().contains("WORKSHOP_TOO_SHORT"));

            SchedulingDecision workshopLong = eval(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 360);
                s.lunchAllowed = true;
            });
            assertFalse(workshopLong.warnings().contains("WORKSHOP_REQUIRES_BREAK_PLAN"));
            assertTrue(workshopLong.manualReview());

            SchedulingDecision webinar = eval(s -> {
                s.kind = CourseKind.WEBINAR;
                s.start(MON_10.toString(), 15);
            });
            assertFalse(webinar.conflicts().contains("WEBINAR_TOO_SHORT"));

            SchedulingDecision webinarLong = eval(s -> {
                s.kind = CourseKind.WEBINAR;
                s.start(MON_10.toString(), 120);
                s.lunchAllowed = true;
            });
            assertFalse(webinarLong.warnings().contains("WEBINAR_LONGER_THAN_RECOMMENDED"));
        }

        @Test
        @DisplayName("考试提前 48 小时不再提示短通知")
        void examNoticeBoundary() {
            SchedulingDecision decision = eval(s -> {
                s.kind = CourseKind.EXAM;
                s.start(T0.plusSeconds(48 * 3600).toString(), 60);
                s.availableEquipment = Set.of("PROCTORING");
            });
            assertFalse(decision.warnings().contains("EXAM_SCHEDULED_WITH_SHORT_NOTICE"));
        }

        @Test
        @DisplayName("容量边界：报名等于场地容量、预期等于容量、容量刚好三倍")
        void roomCapacityBoundaries() {
            SchedulingDecision registeredEqual = eval(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 30;
                s.registered = 30;
                s.expected = 20;
            });
            assertTrue(registeredEqual.conflicts().isEmpty());
            assertFalse(registeredEqual.warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY"));

            SchedulingDecision expectedEqual = eval(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 30;
                s.registered = 20;
                s.expected = 30;
            });
            assertFalse(expectedEqual.warnings().contains("ROOM_BELOW_EXPECTED_CAPACITY"));

            SchedulingDecision exactlyTriple = eval(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 30;
                s.registered = 10;
                s.expected = 10;
            });
            assertFalse(exactlyTriple.warnings().contains("ROOM_SIGNIFICANTLY_OVERSIZED"));
        }

        @Test
        @DisplayName("线上人数 500/1000、工作坊 50、实验 30 的边界")
        void participantBoundaries() {
            SchedulingDecision thousand = eval(s -> s.expected = 1000);
            assertFalse(thousand.conflicts().contains("ONLINE_PLATFORM_CAPACITY_EXCEEDED"));
            assertTrue(thousand.warnings().contains("LARGE_ONLINE_EVENT"));

            SchedulingDecision fiveHundred = eval(s -> s.expected = 500);
            assertFalse(fiveHundred.warnings().contains("LARGE_ONLINE_EVENT"));

            SchedulingDecision workshop = eval(s -> {
                s.kind = CourseKind.WORKSHOP;
                s.start(MON_10.toString(), 60);
                s.expected = 50;
            });
            assertFalse(workshop.warnings().contains("WORKSHOP_GROUP_TOO_LARGE"));

            SchedulingDecision lab = eval(s -> {
                s.kind = CourseKind.LAB;
                s.start(MON_10.toString(), 60);
                s.availableEquipment = Set.of("LAB_SAFETY_KIT");
                s.expected = 30;
            });
            assertFalse(lab.conflicts().contains("LAB_CAPACITY_EXCEEDED"));
        }

        @Test
        @DisplayName("缓冲时间参与冲突判定（60 分钟缓冲）")
        void bufferAffectsConflicts() {
            SchedulingDecision decision = eval(s -> {
                s.buffer = 60;
                s.bookings = List.of(booking("b1", "i1", "R9", "h9",
                        Instant.parse("2026-11-16T01:10:00Z"),
                        Instant.parse("2026-11-16T01:20:00Z")));
            });
            assertTrue(decision.conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:b1"));
        }

        @Test
        @DisplayName("每日课时 8 小时与 6 小时边界")
        void dailyHoursBoundaries() {
            List<ExistingBooking> sevenHours = List.of(
                    booking("a1", "i1", "R9", "h9", Instant.parse("2026-11-16T00:00:00Z"),
                            Instant.parse("2026-11-16T02:20:00Z")),
                    booking("a2", "i1", "R9", "h9", Instant.parse("2026-11-16T02:20:00Z"),
                            Instant.parse("2026-11-16T04:40:00Z")),
                    booking("a3", "i1", "R9", "h9", Instant.parse("2026-11-16T04:40:00Z"),
                            Instant.parse("2026-11-16T07:00:00Z")));
            SchedulingDecision exactlyEight = eval(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.maxSessions = 10;
                s.bookings = sevenHours;
            });
            assertFalse(exactlyEight.conflicts().contains("INSTRUCTOR_DAILY_HOURS_EXCEEDED"));
            assertTrue(exactlyEight.warnings().contains("INSTRUCTOR_LONG_WORKDAY"));

            List<ExistingBooking> sixHours = List.of(
                    booking("b1", "i1", "R9", "h9", Instant.parse("2026-11-16T00:00:00Z"),
                            Instant.parse("2026-11-16T05:00:00Z")));
            SchedulingDecision exactlySix = eval(s -> {
                s.zone = "UTC";
                s.start("2026-11-16T10:00:00Z", 60);
                s.maxSessions = 10;
                s.bookings = sixHours;
            });
            assertFalse(exactlySix.warnings().contains("INSTRUCTOR_LONG_WORKDAY"));
        }

        @Test
        @DisplayName("重复次数 2/20/52 的边界与大型混合课程 200 人边界")
        void recurrenceBoundaries() {
            SchedulingDecision two = eval(s -> {
                s.recurring = true;
                s.occurrences = 2;
            });
            assertTrue(two.conflicts().isEmpty());

            SchedulingDecision twenty = eval(s -> {
                s.recurring = true;
                s.occurrences = 20;
            });
            assertFalse(twenty.warnings().contains("LONG_RECURRENCE_SERIES"));
            assertFalse(twenty.manualReview());

            SchedulingDecision fiftyTwo = eval(s -> {
                s.recurring = true;
                s.occurrences = 52;
            });
            assertFalse(fiftyTwo.conflicts().contains("TOO_MANY_RECURRENCES"));
            assertTrue(fiftyTwo.warnings().contains("LONG_RECURRENCE_SERIES"));

            SchedulingDecision hybrid = eval(s -> {
                s.delivery = DeliveryMode.HYBRID;
                s.roomId = "R1";
                s.roomCapacity = 400;
                s.expected = 200;
                s.registered = 200;
            });
            assertFalse(hybrid.manualReview());
        }

        @Test
        @DisplayName("周日重复课程与空白 cohortId")
        void sundayAndBlankCohort() {
            SchedulingDecision sunday = eval(s -> {
                s.recurring = true;
                s.occurrences = 5;
                s.weekendAllowed = true;
                s.start("2026-11-22T02:00:00Z", 60);
            });
            assertTrue(sunday.warnings().contains("RECURRING_WEEKEND_SESSION"));

            assertEquals("cohortId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> eval(s -> s.cohortId = " ")).getMessage());
        }

        @Test
        @DisplayName("参与人数 0 与缓冲 240 的合法边界")
        void zeroParticipantsAndBufferMax() {
            assertDoesNotThrow(() -> eval(s -> {
                s.expected = 0;
                s.registered = 0;
                s.roomCapacity = 0;
            }));
            assertDoesNotThrow(() -> eval(s -> s.buffer = 240));
            assertEquals("minimumBufferMinutes must be between 0 and 240",
                    assertThrows(IllegalArgumentException.class,
                            () -> eval(s -> s.buffer = 241)).getMessage());
        }
    }

    @Nested
    @DisplayName("定向补杀（7 条已定位可区分输入）")
    class TargetedKills {

        @Test
        @DisplayName("空章节目录不应查询视频（协作交互可观察）")
        void emptyCatalogMustNotQueryVideos() {
            InMemoryCourseRepository courses = new InMemoryCourseRepository();
            CourseService service = new CourseService(courses, new InMemoryChapterRepository(),
                    () -> "course-1", new PricePolicy(), CLOCK);
            service.createCourse("Intro To Math", bd("10"));
            VideoRepository noQueryAllowed = chapterIds -> {
                throw new AssertionError("videos must not be queried for a chapter-less catalog");
            };
            CatalogService catalogService =
                    new CatalogService(service, new InMemoryChapterRepository(), noQueryAllowed);
            assertTrue(catalogService.getCatalog("course-1").chapters().isEmpty());
        }

        @Test
        @DisplayName("minAge=0 且 age=-1 不应追加 BELOW_MINIMUM_AGE")
        void zeroMinimumAgeDoesNotBlockNegativeAge() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Stu negativeAge = new Stu();
            negativeAge.age = -1;
            Off zeroMinimum = new Off();
            zeroMinimum.minAge = 0;
            Req request = new Req();
            request.student = negativeAge.build();
            request.course = zeroMinimum.build();
            EnrollmentDecision decision = service.evaluate(request.build());
            assertTrue(decision.reasons().contains("INVALID_STUDENT_AGE"));
            assertFalse(decision.reasons().contains("BELOW_MINIMUM_AGE"));
        }

        @Test
        @DisplayName("maxAge=0（未设置上限）但 minAge=5 时不应判定区间非法")
        void zeroMaximumAgeIsNotAnInvalidRange() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Off course = new Off();
            course.minAge = 5;
            course.maxAge = 0;
            Req request = new Req();
            request.course = course.build();
            EnrollmentDecision decision = assertDoesNotThrow(() -> service.evaluate(request.build()));
            assertFalse(decision.reasons().contains("ABOVE_MAXIMUM_AGE"));
        }

        @Test
        @DisplayName("积分价值被 20% 上限压到 0 时不写 LOYALTY_POINTS 规则")
        void cappedLoyaltyValueAddsNoRule() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.unitPrice = bd("0.01");
            request.loyaltyPoints = 100;
            request.availablePoints = 100;
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("0.00"), breakdown.loyaltyDiscount());
            assertTrue(breakdown.warnings()
                    .contains("Loyalty discount was limited to twenty percent"));
            assertFalse(breakdown.appliedRules().contains("LOYALTY_POINTS"));
        }

        @Test
        @DisplayName("应税金额为 0 时不登记任何国家税率规则")
        void zeroTaxableAmountAddsNoTaxRule() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.unitPrice = BigDecimal.ZERO;
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("0.00"), breakdown.tax());
            assertTrue(breakdown.appliedRules().contains("FREE_ORDER"));
            assertFalse(breakdown.appliedRules().contains("TAX_CN"));
        }

        @Test
        @DisplayName("可退金额为 0 时不写 REFUND_BELOW_MINIMUM_PAYOUT")
        void zeroRefundableDoesNotTriggerMinimumPayoutWarning() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.couponUsed = true;
            purchase.discountAmount = bd("200.00");
            Ref request = new Ref();
            request.purchase = purchase.build();
            RefundDecision decision = service.evaluate(request.build());
            assertEquals(bd("0.00"), decision.refundableAmount());
            assertTrue(decision.warnings().contains("COUPON_VALUE_CONSUMED_REFUND"));
            assertFalse(decision.warnings().contains("REFUND_BELOW_MINIMUM_PAYOUT"));
            assertFalse(decision.warnings().contains("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        @DisplayName("证书名为空白时命中 CERTIFICATE_NAME_REQUIRED 而不是长度非法")
        void blankCertificateNameHitsRequiredBlocker() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.certificateEnabled = true;
            snapshot.certificateName = " ";
            snapshot.lesson(new Les().build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertTrue(evaluation.blockers().contains("CERTIFICATE_NAME_REQUIRED"));
            assertFalse(evaluation.blockers().contains("CERTIFICATE_NAME_LENGTH_INVALID"));
            assertFalse(evaluation.certificateEligible());
        }
    }

    // ===== merged from EngineProbeTest (expression.engine) =====
    @org.junit.jupiter.api.Nested
    class ExpressionEngineTests {


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

    // ===== merged from ExpressionEngineTestsB probe =====
    @org.junit.jupiter.api.Nested
    class ExpressionEngineTestsB {


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

    @Nested
    @DisplayName("合并后定向补杀（私有入口 / 归一化前可观察面）")
    class SurvivorKills {

        // ---------------- ExamGradingService ----------------

        @Test
        @DisplayName("反射直调 calculateSections：章节最大分为 0 时不得除零")
        @SuppressWarnings("unchecked")
        void zeroSectionMaximumViaReflection() throws Exception {
            Method method = ExamGradingService.class.getDeclaredMethod("calculateSections",
                    Map.class, Map.class, ExamSubmission.class, GradingPolicy.class, List.class);
            method.setAccessible(true);
            Map<String, BigDecimal> maximum = new LinkedHashMap<>();
            maximum.put("A", BigDecimal.ZERO);
            Map<String, BigDecimal> earned = new LinkedHashMap<>();
            earned.put("A", BigDecimal.ZERO);
            List<String> failures = new ArrayList<>();
            Map<String, Integer> percentages = (Map<String, Integer>) method.invoke(grading,
                    earned, maximum, sub().build(), pol().build(), failures);
            assertEquals(0, percentages.get("A"));
            assertTrue(failures.contains("SECTION_BELOW_PASS_MARK:A"));
        }

        @Test
        @DisplayName("多选题部分给分可为负：未开启负分制时分数被归零")
        void multipleChoicePartialNegativeScoreIsZeroed() {
            Ans negative = ans().id("q2").type(QuestionType.MULTIPLE_CHOICE).exact(false)
                    .correctOptionCount(1).selectedCorrectOptions(0).selectedIncorrectOptions(1);
            Sub submission = sub().plus(ans().id("q1").build()).plus(negative.build());
            GradeReport report = grading.grade(submission.build(), pol().build());
            assertEquals(bd("10.00"), report.rawPoints());
        }

        @Test
        @DisplayName("反射直调 scoreAnswer：负分在未开启负分制时返回 0.00")
        void negativeScoreZeroedViaReflection() {
            Ans negative = ans().type(QuestionType.MULTIPLE_CHOICE).exact(false)
                    .correctOptionCount(1).selectedCorrectOptions(0).selectedIncorrectOptions(1);
            List<String> warnings = new ArrayList<>();
            BigDecimal score = invokeScoreAnswer(negative.build(), pol().build(), warnings);
            assertEquals(bd("0.00"), score);
            assertEquals(2, score.scale());
        }

        // ---------------- AdvancedPricingService ----------------

        @Test
        @DisplayName("优惠券 71%：按 70% 上限裁剪而不是触发折扣总上限")
        void couponAboveSeventyPercentUsesPercentCap() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.coupon = coupon(CouponType.PERCENT, "71");
            PriceBreakdown breakdown = service.calculate(request.build());
            assertEquals(bd("70.00"), breakdown.couponDiscount());
            assertFalse(breakdown.warnings()
                    .contains("Combined discounts were limited by the maximum discount policy"));
            assertFalse(breakdown.appliedRules().contains("DISCOUNT_CAP"));
        }

        // ---------------- RefundPolicyService ----------------

        @Test
        @DisplayName("积分为 0 且用券时不得写 POINT_RETURN_REDUCED 警告")
        void zeroLoyaltyPointsSkipsCouponPointWarning() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.couponUsed = true;
            purchase.loyaltyUsed = 0;
            RefundDecision decision = service.evaluate(new Ref().with(purchase).build());
            assertFalse(decision.warnings().contains("POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));
            assertEquals(0, decision.loyaltyPointsReturned());
        }

        @Test
        @DisplayName("反射直调 calculateReturnedPoints：fraudSuspected 时不退积分")
        void fraudSuspectedReturnsNoPointsViaReflection() throws Exception {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Method method = RefundPolicyService.class.getDeclaredMethod("calculateReturnedPoints",
                    PurchaseSnapshot.class, RefundRequest.class, BigDecimal.class, List.class);
            method.setAccessible(true);
            Pur purchase = new Pur();
            purchase.loyaltyUsed = 1000;
            Ref request = new Ref().with(purchase);
            request.fraud = true;
            List<String> warnings = new ArrayList<>();
            int returned = (int) method.invoke(service, purchase.build(), request.build(),
                    BigDecimal.ONE, warnings);
            assertEquals(0, returned);
        }

        @Test
        @DisplayName("反射直调 percentageOrDefault：100 保留两位 scale，150 返回 1")
        void percentageOrDefaultBoundariesViaReflection() throws Exception {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Method method = RefundPolicyService.class.getDeclaredMethod("percentageOrDefault",
                    int.class, BigDecimal.class);
            method.setAccessible(true);
            assertEquals(bd("1.00"), method.invoke(service, 100, bd("0.50")));
            assertEquals(bd("1"), method.invoke(service, 150, bd("0.50")));
            assertEquals(bd("0.50"), method.invoke(service, 0, bd("0.50")));
            assertEquals(bd("0.25"), method.invoke(service, 25, bd("0.50")));
        }

        @Test
        @DisplayName("GIFT 支付方式也要写 GIFT_REFUND_ISSUED_AS_CREDIT")
        void giftPaymentMethodWarningPresent() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.method = PaymentMethod.GIFT;
            purchase.gift = false;
            RefundDecision decision = service.evaluate(new Ref().with(purchase).build());
            assertEquals("WALLET_CREDIT", decision.refundRoute());
            assertTrue(decision.warnings().contains("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        // ---------------- LearningProgressService ----------------

        @Test
        @DisplayName("作业提交且通过时 completedLessons 计为 1")
        void assignmentCompletionCounted() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().type(LessonType.ASSIGNMENT).submitted(true).approved(true).build());
            assertEquals(1, service.evaluate(snapshot.build()).completedLessons());

            Snap notSubmitted = new Snap();
            notSubmitted.lesson(new Les().type(LessonType.ASSIGNMENT).submitted(false)
                    .approved(true).build());
            assertEquals(0, service.evaluate(notSubmitted.build()).completedLessons());

            Snap notApproved = new Snap();
            notApproved.lesson(new Les().type(LessonType.ASSIGNMENT).submitted(true)
                    .approved(false).build());
            assertEquals(0, service.evaluate(notApproved.build()).completedLessons());
        }

        @Test
        @DisplayName("非学术类阻塞项不应阻止课程完成")
        void nonAcademicBlockerDoesNotBlockCompletion() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.published = false;
            snapshot.lesson(new Les().build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertTrue(evaluation.blockers().contains("COURSE_NOT_PUBLISHED"));
            assertTrue(evaluation.courseCompleted());

            Snap warning = new Snap();
            warning.outstandingBalance = true;
            warning.lesson(new Les().build());
            assertTrue(service.evaluate(warning.build()).courseCompleted());
        }

        // ---------------- SchedulingService ----------------

        @Test
        @DisplayName("结束时间超过 21:00 才算超出营业时间")
        void endTimeAfterClosingIsOutsideHours() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch after = new Sch();
            after.zone = "UTC";
            after.start("2026-11-16T20:00:00Z", 90);
            assertTrue(service.evaluate(after.build()).conflicts()
                    .contains("OUTSIDE_OPERATING_HOURS"));

            Sch before = new Sch();
            before.zone = "UTC";
            before.start("2026-11-16T19:30:00Z", 90);
            assertFalse(service.evaluate(before.build()).conflicts()
                    .contains("OUTSIDE_OPERATING_HOURS"));
        }

        @Test
        @DisplayName("教室模式：空白 roomId 报 ROOM_REQUIRED，正常 roomId 不报")
        void classroomBlankRoomId() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch blank = new Sch();
            blank.delivery = DeliveryMode.CLASSROOM;
            blank.roomCapacity = 50;
            blank.registered = 10;
            blank.expected = 10;
            blank.roomId = "   ";
            assertTrue(service.evaluate(blank.build()).conflicts().contains("ROOM_REQUIRED"));

            Sch ok = new Sch();
            ok.delivery = DeliveryMode.CLASSROOM;
            ok.roomCapacity = 50;
            ok.registered = 10;
            ok.expected = 10;
            ok.roomId = "R1";
            assertFalse(service.evaluate(ok.build()).conflicts().contains("ROOM_REQUIRED"));
        }

        @Test
        @DisplayName("重复课程：周六与周日都应写 RECURRING_WEEKEND_SESSION")
        void recurringWeekendBothDays() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch saturday = new Sch();
            saturday.recurring = true;
            saturday.occurrences = 4;
            saturday.weekendAllowed = true;
            saturday.start("2026-11-21T02:00:00Z", 60);
            assertTrue(service.evaluate(saturday.build()).warnings()
                    .contains("RECURRING_WEEKEND_SESSION"));

            Sch sunday = new Sch();
            sunday.recurring = true;
            sunday.occurrences = 4;
            sunday.weekendAllowed = true;
            sunday.start("2026-11-22T02:00:00Z", 60);
            assertTrue(service.evaluate(sunday.build()).warnings()
                    .contains("RECURRING_WEEKEND_SESSION"));
        }

        @Test
        @DisplayName("同批次(cohort)冲突属于不可覆写项：管理员覆写后仍保留")
        void cohortConflictIsNotOverridable() {
            SchedulingService service = new SchedulingService(CLOCK);
            Sch request = new Sch();
            request.adminOverride = true;
            request.bookings = List.of(booking("cb1", "i2", "R9", "h1", MON_10,
                    MON_10.plusSeconds(1800)));
            SchedulingDecision decision = service.evaluate(request.build());
            assertTrue(decision.conflicts().contains("COHORT_BOOKING_CONFLICT:cb1"));
            assertTrue(decision.warnings().contains("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"));
            assertFalse(decision.approved());

            Sch overridable = new Sch();
            overridable.zone = "UTC";
            overridable.start("2026-11-16T07:00:00Z", 60);
            overridable.adminOverride = true;
            SchedulingDecision cleared = service.evaluate(overridable.build());
            assertTrue(cleared.conflicts().isEmpty());
            assertTrue(cleared.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
        }

        // ---------------- EnrollmentEligibilityService ----------------

        @Test
        @DisplayName("组织专属课：缺少组织 ID 时 reasons 精确为 ORGANIZATION_ID_REQUIRED")
        void organizationWithoutIdExactReasons() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Off course = new Off();
            course.organizationOnly = true;
            Req request = new Req();
            request.course = course.build();
            request.sponsored = true;
            request.orgId = null;
            assertEquals(List.of("ORGANIZATION_ID_REQUIRED"), service.evaluate(request.build()).reasons());

            Req blankId = new Req();
            blankId.course = course.build();
            blankId.sponsored = true;
            blankId.orgId = "";
            assertEquals(List.of("ORGANIZATION_ID_REQUIRED"),
                    service.evaluate(blankId.build()).reasons());
        }
    }

    @Nested
    @DisplayName("分支覆盖补测（短路两侧 / else 侧 / null 变体）")
    class BranchCoverage {

        private final SchedulingService scheduler = new SchedulingService(CLOCK);

        private SchedulingDecision sched(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            if (mutation != null) {
                mutation.accept(request);
            }
            return scheduler.evaluate(request.build());
        }

        private String schedInvalid(java.util.function.Consumer<Sch> mutation) {
            Sch request = new Sch();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> scheduler.evaluate(request.build())).getMessage();
        }

        @Test
        @DisplayName("管理员覆写但没有冲突：直接通过且不写覆写警告")
        void overrideWithoutConflict() {
            SchedulingDecision decision = sched(s -> s.adminOverride = true);
            assertTrue(decision.approved());
            assertTrue(decision.conflicts().isEmpty());
            assertFalse(decision.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
        }

        @Test
        @DisplayName("线上课空白 roomId 不写 ROOM_IGNORED；教室模式 meetingUrl 为 null/空白不写可选警告")
        void optionalResourceCombinations() {
            SchedulingDecision blankRoom = sched(s -> s.roomId = "   ");
            assertFalse(blankRoom.warnings().contains("ROOM_IGNORED_FOR_ONLINE_SESSION"));

            SchedulingDecision noUrl = sched(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.meetingUrl = null;
            });
            assertFalse(noUrl.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));

            SchedulingDecision blankUrl = sched(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.meetingUrl = "   ";
            });
            assertFalse(blankUrl.warnings().contains("MEETING_URL_OPTIONAL_FOR_CLASSROOM"));
        }

        @Test
        @DisplayName("录播标记组合：需要且支持不报错，不需要且不支持也无提示")
        void recordingCombinations() {
            SchedulingDecision supported = sched(s -> {
                s.recordingRequired = true;
                s.recordingSupported = true;
            });
            assertFalse(supported.conflicts().contains("RECORDING_NOT_SUPPORTED"));
            assertTrue(supported.approved());

            SchedulingDecision plain = sched(s -> {
                s.recordingRequired = false;
                s.recordingSupported = false;
            });
            assertFalse(plain.warnings().contains("RECORDING_AVAILABLE"));
        }

        @Test
        @DisplayName("教室里的考试：不写远程监考提示，也不进入人工复核")
        void classroomExamSkipsRemoteProctoring() {
            SchedulingDecision decision = sched(s -> {
                s.kind = CourseKind.EXAM;
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.meetingUrl = null;
                s.start(MON_10.toString(), 60);
            });
            assertFalse(decision.warnings().contains("REMOTE_PROCTORING_NOT_CONFIGURED"));
            assertFalse(decision.manualReview());
        }

        @Test
        @DisplayName("线上课要求无障碍但房间不可用：不报 ACCESSIBLE_ROOM_REQUIRED")
        void onlineAccessibleSeatIsIgnored() {
            SchedulingDecision decision = sched(s -> {
                s.accessibleRequired = true;
                s.roomAccessible = false;
            });
            assertFalse(decision.conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));
        }

        @Test
        @DisplayName("教师可用时段只包住开始不包住结束：判为不可用")
        void availabilityCoversStartOnly() {
            SchedulingDecision decision = sched(s -> s.availability = List.of(
                    window("2026-11-16T01:30:00Z", "2026-11-16T02:30:00Z")));
            assertTrue(decision.conflicts().contains("INSTRUCTOR_UNAVAILABLE"));
        }

        @Test
        @DisplayName("维护窗口与课程不重叠：不报冲突")
        void maintenanceWithoutOverlap() {
            SchedulingDecision decision = sched(s -> s.maintenance = List.of(
                    window("2026-11-16T05:00:00Z", "2026-11-16T06:00:00Z")));
            assertFalse(decision.conflicts().contains("MAINTENANCE_WINDOW_CONFLICT"));
            assertTrue(decision.approved());
        }

        @Test
        @DisplayName("同日不同讲师的预约：只参与课时统计，不产生冲突")
        void sameDayOtherInstructor() {
            SchedulingDecision decision = sched(s -> s.bookings = List.of(
                    booking("b1", "i2", "R9", "h9", Instant.parse("2026-11-16T05:00:00Z"),
                            Instant.parse("2026-11-16T06:00:00Z"))));
            assertTrue(decision.conflicts().isEmpty());
        }

        @Test
        @DisplayName("教室模式不同房间的重叠预约：不产生房间冲突")
        void overlappingBookingWithDifferentRoom() {
            SchedulingDecision decision = sched(s -> {
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.bookings = List.of(booking("b1", "i2", "R2", "h9", MON_10,
                        MON_10.plusSeconds(1800)));
            });
            assertFalse(decision.conflicts().contains("ROOM_BOOKING_CONFLICT:b1"));
            assertTrue(decision.conflicts().isEmpty());
        }

        @Test
        @DisplayName("不可覆写冲突逐类型保留：过去时间/跨日/无障碍/讲师预约")
        void nonOverridableConflictTypes() {
            assertTrue(sched(s -> {
                s.adminOverride = true;
                s.start(T0.minusSeconds(3600).toString(), 60);
            }).conflicts().contains("START_TIME_IN_PAST"));

            assertTrue(sched(s -> {
                s.adminOverride = true;
                s.zone = "UTC";
                s.start("2026-11-16T23:00:00Z", 120);
                s.afterHoursAllowed = true;
            }).conflicts().contains("SESSION_CROSSES_LOCAL_DATE"));

            assertTrue(sched(s -> {
                s.adminOverride = true;
                s.delivery = DeliveryMode.CLASSROOM;
                s.roomId = "R1";
                s.roomCapacity = 50;
                s.accessibleRequired = true;
                s.roomAccessible = false;
            }).conflicts().contains("ACCESSIBLE_ROOM_REQUIRED"));

            assertTrue(sched(s -> {
                s.adminOverride = true;
                s.bookings = List.of(booking("ib1", "i1", "R9", "h9", MON_10,
                        MON_10.plusSeconds(1800)));
            }).conflicts().contains("INSTRUCTOR_BOOKING_CONFLICT:ib1"));

            SchedulingDecision overridable = sched(s -> {
                s.adminOverride = true;
                s.start("2026-11-21T02:00:00Z", 60);
            });
            assertTrue(overridable.conflicts().isEmpty());
            assertTrue(overridable.warnings().contains("ADMINISTRATOR_OVERRIDE_APPLIED"));
        }

        @Test
        @DisplayName("null 形式的必填字段各自被拒绝")
        void nullRequiredFields() {
            assertEquals("courseId must not be blank", schedInvalid(s -> s.courseId = null));
            assertEquals("instructorId must not be blank", schedInvalid(s -> s.instructorId = null));
            assertEquals("zoneId must not be blank", schedInvalid(s -> s.zone = null));
            assertEquals("bookingId must not be blank", schedInvalid(s -> s.bookings = List.of(
                    new ExistingBooking(null, "i2", "R9", "h9", MON_10, MON_10.plusSeconds(600),
                            false, false, CourseKind.LECTURE))));
            assertEquals("booking window is invalid", schedInvalid(s -> s.bookings = List.of(
                    new ExistingBooking("b1", "i2", "R9", "h9", null, MON_10.plusSeconds(600),
                            false, false, CourseKind.LECTURE))));
            assertEquals("booking window is invalid", schedInvalid(s -> s.bookings = List.of(
                    new ExistingBooking("b1", "i2", "R9", "h9", MON_10, null,
                            false, false, CourseKind.LECTURE))));
            assertEquals("instructor availability is invalid",
                    schedInvalid(s -> s.availability = List.of(new TimeWindow(null, MON_10))));
            assertEquals("instructor availability is invalid",
                    schedInvalid(s -> s.availability = List.of(new TimeWindow(MON_10, null))));
            assertEquals("maintenance window is invalid",
                    schedInvalid(s -> s.maintenance = List.of(new TimeWindow(null, MON_10))));
        }

        private final AdvancedPricingService pricingService = new AdvancedPricingService(CLOCK);

        private PriceBreakdown priced(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            if (mutation != null) {
                mutation.accept(request);
            }
            return pricingService.calculate(request.build());
        }

        private String pricedInvalid(java.util.function.Consumer<Price> mutation) {
            Price request = new Price();
            mutation.accept(request);
            return assertThrows(IllegalArgumentException.class,
                    () -> pricingService.calculate(request.build())).getMessage();
        }

        @Test
        @DisplayName("null 形式的必填字段各自被拒绝")
        void pricingNullFields() {
            assertEquals("courseId must not be blank", pricedInvalid(p -> p.courseId = null));
            assertEquals("currency must not be blank", pricedInvalid(p -> p.currency = null));
            assertEquals("countryCode must not be blank", pricedInvalid(p -> p.country = null));
        }

        @Test
        @DisplayName("可叠加优惠券与促销同时存在：不触发二选一")
        void stackableCouponWithPromotion() {
            PriceBreakdown breakdown = priced(p -> {
                p.promotion = promotion(PromotionType.FIXED, "10");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                        true, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("10.00"), breakdown.promotionDiscount());
            assertEquals(bd("10.00"), breakdown.couponDiscount());
        }

        @Test
        @DisplayName("无名称促销与未来结束日期、过去开始日期")
        void promotionDateCombinations() {
            assertEquals(bd("0.00"), priced(p -> p.promotion = new Promotion(null,
                    PromotionType.PERCENT, bd("50"), null, null, false, 1, null))
                    .promotionDiscount());

            PriceBreakdown future = priced(p -> p.promotion = new Promotion("promo",
                    PromotionType.FIXED, bd("10"), null,
                    LocalDate.now(CLOCK).plusDays(3), false, 1, null));
            assertEquals(bd("10.00"), future.promotionDiscount());

            PriceBreakdown alreadyStarted = priced(p -> p.promotion = new Promotion("promo",
                    PromotionType.FIXED, bd("10"), LocalDate.now(CLOCK).minusDays(2), null,
                    false, 1, null));
            assertEquals(bd("10.00"), alreadyStarted.promotionDiscount());
        }

        @Test
        @DisplayName("会员专属促销对会员生效；负的上限配置不参与裁剪")
        void membersOnlyAndNegativeCap() {
            PriceBreakdown member = priced(p -> {
                p.level = MembershipLevel.BASIC;
                p.promotion = new Promotion("promo", PromotionType.FIXED, bd("10"), null, null,
                        true, 1, null);
            });
            assertEquals(bd("10.00"), member.promotionDiscount());

            PriceBreakdown negativeCap = priced(p -> p.promotion = new Promotion("promo",
                    PromotionType.FIXED, bd("30"), null, null, false, 1, bd("-1")));
            assertEquals(bd("30.00"), negativeCap.promotionDiscount());
        }

        @Test
        @DisplayName("优惠券：null code、未来截止、适用课程命中、会员等级足够")
        void couponCombinations() {
            assertEquals(bd("0.00"), priced(p -> p.coupon = new Coupon(null, CouponType.PERCENT,
                    bd("50"), null, null, null, 5, true, Set.of(), MembershipLevel.NONE, 0))
                    .couponDiscount());

            PriceBreakdown future = priced(p -> p.coupon = new Coupon("c", CouponType.FIXED,
                    bd("10"), null, null, LocalDate.now(CLOCK).plusDays(2), 5, true, Set.of(),
                    MembershipLevel.NONE, 0));
            assertEquals(bd("10.00"), future.couponDiscount());

            PriceBreakdown eligible = priced(p -> p.coupon = new Coupon("c", CouponType.FIXED,
                    bd("10"), null, null, null, 5, true, Set.of("c1"), MembershipLevel.NONE, 0));
            assertEquals(bd("10.00"), eligible.couponDiscount());

            PriceBreakdown highMember = priced(p -> {
                p.level = MembershipLevel.ENTERPRISE;
                p.quantity = 10;
                p.unitPrice = bd("10.00");
                p.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                        true, Set.of(), MembershipLevel.BASIC, 0);
            });
            assertEquals(bd("10.00"), highMember.couponDiscount());
        }

        @Test
        @DisplayName("首购 + 可叠加优惠券：首购折扣仍然生效")
        void firstPurchaseWithStackableCoupon() {
            PriceBreakdown breakdown = priced(p -> {
                p.firstPurchase = true;
                p.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                        true, Set.of(), MembershipLevel.NONE, 0);
            });
            assertEquals(bd("8.00"), breakdown.firstPurchaseDiscount());
            assertTrue(breakdown.appliedRules().contains("FIRST_PURCHASE"));
        }

        private final EnrollmentEligibilityService eligibility =
                new EnrollmentEligibilityService(new History(), CLOCK);

        private EnrollmentDecision enroll(Stu student, Off course,
                                          java.util.function.Consumer<Req> mutation) {
            Req request = new Req();
            request.student = student.build();
            request.course = course.build();
            if (mutation != null) {
                mutation.accept(request);
            }
            return eligibility.evaluate(request.build());
        }

        @Test
        @DisplayName("教师但非本课讲师；自学课空白排课键不写提示")
        void instructorMismatchAndBlankSchedule() {
            Stu instructor = new Stu();
            instructor.instructor = true;
            assertFalse(enroll(instructor, new Off(), null).reasons()
                    .contains("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE"));

            Off blankSchedule = new Off();
            blankSchedule.scheduleKey = "   ";
            assertFalse(enroll(new Stu(), blankSchedule, null).warnings()
                    .contains("SELF_PACED_COURSE_IGNORES_SCHEDULE"));
        }

        @Test
        @DisplayName("每周上限未达、PREMIUM 未达上限、ENTERPRISE 未赞助")
        void weeklyLimitSides() {
            History history = new History();
            EnrollmentEligibilityService service = new EnrollmentEligibilityService(history, CLOCK);
            Off limited = new Off();
            limited.maxWeekly = 3;
            history.activeCourses = 1;
            Req request = new Req();
            request.course = limited.build();
            assertTrue(service.evaluate(request.build()).reasons().isEmpty());

            history.activeCourses = 4;
            Stu premium = new Stu();
            premium.level = MembershipLevel.PREMIUM;
            Req notAtLimit = new Req();
            notAtLimit.course = limited.build();
            notAtLimit.student = premium.build();
            assertTrue(service.evaluate(notAtLimit.build()).reasons()
                    .contains("WEEKLY_COURSE_LIMIT_REACHED"));

            history.activeCourses = 3;
            Stu premiumAtLimit = new Stu();
            premiumAtLimit.level = MembershipLevel.PREMIUM;
            Req atLimit = new Req();
            atLimit.course = limited.build();
            atLimit.student = premiumAtLimit.build();
            assertTrue(service.evaluate(atLimit.build()).warnings()
                    .contains("PREMIUM_WEEKLY_LIMIT_GRACE"));

            Stu enterprise = new Stu();
            enterprise.level = MembershipLevel.ENTERPRISE;
            Req notSponsored = new Req();
            notSponsored.course = limited.build();
            notSponsored.student = enterprise.build();
            assertTrue(service.evaluate(notSponsored.build()).reasons()
                    .contains("WEEKLY_COURSE_LIMIT_REACHED"));
            history.activeCourses = 0;
        }

        @Test
        @DisplayName("studentId 为 null；报名关闭/开课时间在未来；国家为 null")
        void nullAndFutureFields() {
            Req nullStudent = new Req();
            Stu student = new Stu();
            student.id = null;
            nullStudent.student = student.build();
            assertEquals("studentId must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> eligibility.evaluate(nullStudent.build())).getMessage());

            Off futureClose = new Off();
            futureClose.opens = T0.minusSeconds(7200);
            futureClose.closes = T0.plusSeconds(7200);
            assertFalse(enroll(new Stu(), futureClose, null).reasons()
                    .contains("ENROLLMENT_CLOSED"));

            Off futureStart = new Off();
            futureStart.starts = T0.plusSeconds(7200);
            assertFalse(enroll(new Stu(), futureStart, null).reasons()
                    .contains("COURSE_ALREADY_STARTED"));

            Stu nullCountry = new Stu();
            nullCountry.country = null;
            assertTrue(enroll(nullCountry, new Off(), null).reasons().contains("COUNTRY_MISSING"));
        }

        @Test
        @DisplayName("国家白名单为空与命中；语言要求为空白")
        void countryAndLanguageSides() {
            Off openCountries = new Off();
            openCountries.countries = Set.of();
            assertFalse(enroll(new Stu(), openCountries, null).reasons()
                    .contains("COUNTRY_NOT_ALLOWED"));

            Off blankLanguage = new Off();
            blankLanguage.requiredLanguage = "   ";
            Stu noLanguage = new Stu();
            noLanguage.language = null;
            assertTrue(enroll(noLanguage, blankLanguage, null).warnings()
                    .contains("PREFERRED_LANGUAGE_MISSING"));
        }

        @Test
        @DisplayName("组织校验的空白与命中组合")
        void organizationSides() {
            Off requiredOrg = new Off();
            requiredOrg.requiredOrg = "   ";
            assertFalse(enroll(new Stu(), requiredOrg, null).reasons()
                    .contains("ORGANIZATION_NOT_ALLOWED"));

            Off subscription = new Off();
            subscription.subscriptionRequired = true;
            assertFalse(enroll(new Stu(), subscription, null).reasons()
                    .contains("ACTIVE_SUBSCRIPTION_REQUIRED"));

            Stu enterprise = new Stu();
            enterprise.level = MembershipLevel.ENTERPRISE;
            assertFalse(enroll(enterprise, new Off(), r -> r.seats = 2).reasons()
                    .contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"));
        }

        @Test
        @DisplayName("先修课为 null、得分 0 与 100、免费课程")
        void prerequisiteNullAndBounds() {
            History history = new History();
            EnrollmentEligibilityService service = new EnrollmentEligibilityService(history, CLOCK);
            Off blankPrereq = new Off();
            blankPrereq.prereqs = Set.of("   ");
            Req blankRequest = new Req();
            blankRequest.course = blankPrereq.build();
            assertTrue(service.evaluate(blankRequest.build()).warnings()
                    .contains("INVALID_PREREQUISITE_CONFIGURATION"));

            Off scored = new Off();
            scored.prereqs = Set.of("pre-1");
            history.completed.add("pre-1");
            Req zero = new Req();
            zero.course = scored.build();
            history.scores.put("pre-1", 0);
            assertFalse(service.evaluate(zero.build()).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));

            history.scores.put("pre-1", 100);
            assertFalse(service.evaluate(zero.build()).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));

            Off free = new Off();
            free.price = null;
            Req freeRequest = new Req();
            freeRequest.course = free.build();
            assertFalse(service.evaluate(freeRequest.build()).paymentRequired());
            history.completed.clear();
            history.scores.clear();
        }

        private final RefundPolicyService refundService = new RefundPolicyService(CLOCK);

        private RefundDecision refunded(java.util.function.Consumer<Pur> purchaseMutation,
                                        java.util.function.Consumer<Ref> requestMutation) {
            Pur purchase = new Pur();
            if (purchaseMutation != null) {
                purchaseMutation.accept(purchase);
            }
            Ref request = new Ref();
            request.purchase = purchase.build();
            if (requestMutation != null) {
                requestMutation.accept(request);
            }
            return refundService.evaluate(request.build());
        }

        @Test
        @DisplayName("访问到期在未来；可退金额 0 但有积分退回")
        void futureAccessAndZeroRefundableWithPoints() {
            assertFalse(refunded(p -> p.accessExpiresAt = T0.plusSeconds(3600), null).warnings()
                    .contains("COURSE_ACCESS_EXPIRED"));

            RefundDecision withPoints = refunded(p -> {
                p.couponUsed = true;
                p.discountAmount = bd("200.00");
                p.loyaltyUsed = 1000;
            }, null);
            assertEquals(bd("0.00"), withPoints.refundableAmount());
            assertTrue(withPoints.loyaltyPointsReturned() > 0);
            assertTrue(withPoints.approved());
        }

        @Test
        @DisplayName("时间窗右侧不满足：2h 有进度、24h 进度高、7/14/30 天进度高")
        void timeWindowRightSides() {
            assertFalse(refunded(p -> {
                p.paidAt = T0.minusSeconds(3600);
                p.progress = 10;
            }, null).reasons().contains("COOLING_OFF_PERIOD"));

            RefundDecision progressTen = refunded(p -> {
                p.paidAt = T0.minusSeconds(20 * 3600);
                p.progress = 10;
            }, null);
            assertFalse(progressTen.reasons().contains("ONE_DAY_LOW_USAGE"));
            assertTrue(progressTen.reasons().contains("SEVEN_DAY_GUARANTEE"));

            assertFalse(refunded(p -> {
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.progress = 40;
            }, null).reasons().contains("SEVEN_DAY_PARTIAL_REFUND"));

            assertFalse(refunded(p -> {
                p.paidAt = T0.minusSeconds(200 * 3600);
                p.progress = 40;
            }, null).reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));

            assertFalse(refunded(p -> {
                p.paidAt = T0.minusSeconds(400 * 3600);
                p.progress = 40;
            }, null).reasons().contains("THIRTY_DAY_EXCEPTION"));
        }

        @Test
        @DisplayName("内容质量未采信、直播出勤率低于 20%")
        void contentNotUpheldAndLowAttendance() {
            RefundDecision content = refunded(p -> p.progress = 10, r -> {
                r.reason = RefundReason.CONTENT_QUALITY;
                r.contentUpheld = false;
            });
            assertFalse(content.reasons().contains("CONTENT_COMPLAINT_UPHELD"));

            RefundDecision lowAttendance = refunded(p -> {
                p.method = PaymentMethod.ALIPAY;
                p.paidAt = T0.minusSeconds(100 * 3600);
                p.attendedSessions = 1;
                p.totalSessions = 6;
            }, null);
            assertEquals(bd("1.00"), lowAttendance.processingFee());
        }

        @Test
        @DisplayName("优惠券未使用、提供方责任时不做券折抵")
        void couponAndProviderSides() {
            RefundDecision noCoupon = refunded(null, null);
            assertFalse(noCoupon.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));

            RefundDecision provider = refunded(p -> {
                p.couponUsed = true;
                p.courseRemoved = true;
            }, null);
            assertFalse(provider.warnings().contains("COUPON_VALUE_NOT_REFUNDED"));
            assertEquals(bd("100.00"), provider.refundableAmount());
        }

        @Test
        @DisplayName("purchaseId 与 courseId 为 null 被拒绝")
        void nullIds() {
            assertEquals("purchaseId must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> refunded(p -> p.purchaseId = null, null)).getMessage());
            assertEquals("courseId must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> refunded(p -> p.courseId = null, null)).getMessage());
        }

        private final LearningProgressService progressService =
                new LearningProgressService(CLOCK);

        private ProgressEvaluation progressOf(java.util.function.Consumer<Snap> mutation) {
            Snap snapshot = new Snap();
            snapshot.lesson(new Les().build());
            if (mutation != null) {
                mutation.accept(snapshot);
            }
            return progressService.evaluate(snapshot.build());
        }

        @Test
        @DisplayName("尝试次数未达上限；顺序学习中前一课已完成")
        void attemptBelowLimitAndSequentialComplete() {
            ProgressEvaluation belowLimit = progressOf(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().completion(10).attempts(1).maximumAttempts(2).build())));
            assertFalse(belowLimit.blockers().contains("ATTEMPT_LIMIT_REACHED:l1"));

            Snap sequential = new Snap();
            sequential.sequential = true;
            sequential.requiredOverallPercent = 0;
            sequential.lesson(new Les().id("l1").order(1).completion(100).mandatory(true).build());
            sequential.lesson(new Les().id("l2").order(2).completion(100).build());
            ProgressEvaluation evaluation = progressService.evaluate(sequential.build());
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
        }

        @Test
        @DisplayName("项目课未提交 / 未通过 / 无抄袭 三种 earned 分支")
        void projectEarnedBranches() {
            ProgressEvaluation notSubmitted = progressOf(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.PROJECT).submitted(false).build())));
            assertEquals(0, notSubmitted.overallPercent());

            ProgressEvaluation submitted = progressOf(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.PROJECT).submitted(true).approved(false)
                            .build())));
            assertEquals(33, submitted.overallPercent());

            ProgressEvaluation clean = progressOf(s -> s.lessons = new ArrayList<>(
                    List.of(new Les().type(LessonType.PROJECT).submitted(true).approved(true)
                            .plagiarism(false).build())));
            assertFalse(clean.warnings().contains("PROJECT_HELD_FOR_INTEGRITY_REVIEW:l1"));
            assertEquals(100, clean.overallPercent());
        }

        @Test
        @DisplayName("enrolledAt 为 null；毕业项目候选的三段短路")
        void enrolledAtNullAndProjectCandidateSides() {
            ProgressEvaluation noEnrollment = progressOf(s -> s.enrolledAt = null);
            assertFalse(noEnrollment.blockers().contains("ENROLLMENT_DATE_IN_FUTURE"));

            ProgressEvaluation notProject = progressOf(s -> s.finalProjectRequired = true);
            assertTrue(notProject.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            ProgressEvaluation notSubmitted = progressOf(s -> {
                s.finalProjectRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.PROJECT)
                        .submitted(false).build()));
            });
            assertTrue(notSubmitted.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            ProgressEvaluation notApproved = progressOf(s -> {
                s.finalProjectRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.PROJECT)
                        .submitted(true).approved(false).build()));
            });
            assertTrue(notApproved.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));

            ProgressEvaluation plagiarised = progressOf(s -> {
                s.finalProjectRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.PROJECT)
                        .submitted(true).approved(true).plagiarism(true).build()));
            });
            assertTrue(plagiarised.blockers().contains("FINAL_PROJECT_NOT_APPROVED"));
        }

        @Test
        @DisplayName("必修作业通过时不再需要教师审批；studentId 与 lessonId 为 null")
        void instructorApprovalAndNullIds() {
            ProgressEvaluation approved = progressOf(s -> {
                s.instructorApprovalRequired = true;
                s.lessons = new ArrayList<>(List.of(new Les().type(LessonType.ASSIGNMENT)
                        .mandatory(true).approved(true).build()));
            });
            assertFalse(approved.blockers().contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            Snap nullStudent = new Snap();
            nullStudent.studentId = null;
            assertEquals("studentId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> progressService.evaluate(nullStudent.build())).getMessage());

            Snap nullLesson = new Snap();
            nullLesson.lesson(new Les().id(null).build());
            assertEquals("lessonId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> progressService.evaluate(nullLesson.build())).getMessage());
        }

        @Test
        @DisplayName("数值题误差超过十倍容差、截止时间为未来、已做身份验证")
        void numericBeyondTenTimesAndDeadlineSides() {
            GradeReport beyond = grading.grade(sub().plus(ans()
                    .type(QuestionType.NUMERIC).exact(false).numericError(bd("20"))
                    .numericTolerance(bd("1")).build()).build(), pol().build());
            assertEquals(bd("0.00"), beyond.rawPoints());

            Sub futureDeadline = sub().plus(ans().build());
            futureDeadline.deadline = T0.plusSeconds(3600);
            GradeReport noPenalty = grading.grade(futureDeadline.build(), pol().build());
            assertEquals(bd("10.00"), noPenalty.finalPoints());

            Pol requiresIdentity = pol();
            requiresIdentity.identityVerificationRequired = true;
            Sub verified = sub().plus(ans().build());
            GradeReport identityOk = grading.grade(verified.build(), requiresIdentity.build());
            assertFalse(identityOk.failures().contains("IDENTITY_NOT_VERIFIED"));
            assertTrue(identityOk.passed());
        }

        @Test
        @DisplayName("完整性失败的不同来源都会阻断通过")
        void integrityFailureSources() {
            Sub identity = sub().plus(ans().build());
            identity.identityVerified = false;
            Pol requiresIdentity = pol();
            requiresIdentity.identityVerificationRequired = true;
            assertFalse(grading.grade(identity.build(), requiresIdentity.build()).passed());

            Sub fullscreen = sub().plus(ans().build());
            fullscreen.fullscreenExitCount = 3;
            assertFalse(grading.grade(fullscreen.build(), pol().build()).passed());

            Sub copyPaste = sub().plus(ans().build());
            copyPaste.copyPasteEventCount = 5;
            assertFalse(grading.grade(copyPaste.build(), pol().build()).passed());

            Sub suspended = sub().plus(ans().build());
            suspended.suspended = true;
            assertFalse(grading.grade(suspended.build(), pol().build()).passed());
        }

        @Test
        @DisplayName("examId 与 questionId 为 null 被拒绝")
        void gradingNullIds() {
            Sub noExam = sub().plus(ans().build());
            noExam.examId = null;
            assertEquals("examId must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> grading.grade(noExam.build(), pol().build())).getMessage());

            Ans noQuestion = ans();
            noQuestion.id = null;
            assertEquals("questionId must not be blank",
                    assertThrows(IllegalArgumentException.class,
                            () -> grading.grade(sub().plus(noQuestion.build()).build(),
                                    pol().build())).getMessage());
        }

        @Test
        @DisplayName("编程题未作答 / 编译失败 / 有测试 时绕过零测试校验")
        void programmingValidationSides() {
            assertDoesNotThrow(() -> grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).answered(false).exact(false).build()).build(),
                    pol().build()));
            assertDoesNotThrow(() -> grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).compilationSucceeded(false).build()).build(),
                    pol().build()));
            assertDoesNotThrow(() -> grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).totalTests(2).passedTests(1).build()).build(),
                    pol().build()));
        }
    }

    @Nested
    @DisplayName("分支覆盖补测第二轮（record 空集合 / 剩余短路侧）")
    class BranchCoverage2 {

        @Test
        @DisplayName("ScheduleRequest 六个集合字段为 null 时归一化为空集合")
        void scheduleRequestNullCollections() {
            ScheduleRequest request = new ScheduleRequest("c1", "h1", "i1", DeliveryMode.ONLINE,
                    CourseKind.LECTURE, MON_10, MON_10.plusSeconds(3600), "Asia/Shanghai", null,
                    "https://meet.example/1", 10, 10, 0, null, null, false, false, false, false,
                    false, false, 1, 0, 3, null, null, null, null, false, false, false);
            assertTrue(request.requiredEquipment().isEmpty());
            assertTrue(request.availableEquipment().isEmpty());
            assertTrue(request.instructorAvailability().isEmpty());
            assertTrue(request.maintenanceWindows().isEmpty());
            assertTrue(request.existingBookings().isEmpty());
            assertTrue(request.holidays().isEmpty());
            SchedulingDecision decision = new SchedulingService(CLOCK).evaluate(request);
            assertTrue(decision.approved());
        }

        @Test
        @DisplayName("GradeReport 三个集合字段为 null 时归一化，ExamSubmission 两个集合字段同理")
        void reportAndSubmissionNullCollections() {
            GradeReport report = new GradeReport(null, null, null, 0, "F", false, false, null,
                    null, null);
            assertTrue(report.sectionPercentages().isEmpty());
            assertTrue(report.failures().isEmpty());
            assertTrue(report.warnings().isEmpty());

            ExamSubmission submission = new ExamSubmission("e", "s", null, MON_10,
                    MON_10.plusSeconds(60), null, 0, 1, true, true, false, false, false, 0, 0,
                    BigDecimal.ZERO, null, null);
            assertTrue(submission.answers().isEmpty());
            assertTrue(submission.sectionPassPercentages().isEmpty());

            ProgressEvaluation evaluation = new ProgressEvaluation(0, 0, 0, false, false, null,
                    null, null);
            assertTrue(evaluation.blockers().isEmpty());
            assertTrue(evaluation.warnings().isEmpty());

            SchedulingDecision decision = new SchedulingDecision(true, false, "APPROVED", 0, null,
                    null);
            assertTrue(decision.conflicts().isEmpty());
            assertTrue(decision.warnings().isEmpty());

            EnrollmentDecision enrollment = new EnrollmentDecision(EnrollmentOutcome.ENROLLED,
                    false, 0, List.of(), List.of());
            assertTrue(enrollment.reasons().isEmpty());
        }

        @Test
        @DisplayName("优惠券最低会员等级为 null 时不限制")
        void couponWithoutMinimumMembership() {
            AdvancedPricingService service = new AdvancedPricingService(CLOCK);
            Price request = new Price();
            request.coupon = new Coupon("c", CouponType.FIXED, bd("10"), null, null, null, 5,
                    true, Set.of(), null, 0);
            assertEquals(bd("10.00"), service.calculate(request.build()).couponDiscount());
        }

        @Test
        @DisplayName("管理员覆写且策略本已满足：不再套用默认比例")
        void adminOverrideWithEligibleRate() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.paidAt = T0.minusSeconds(100 * 3600);
            purchase.progress = 20;
            Ref request = new Ref();
            request.purchase = purchase.build();
            request.adminOverride = true;
            RefundDecision decision = service.evaluate(request.build());
            assertTrue(decision.warnings().contains("ADMIN_OVERRIDE_APPLIED"));
            assertEquals(bd("70.00"), decision.refundableAmount());
        }

        @Test
        @DisplayName("可退金额 0、积分为 0 但进入人工复核：不写 NO_REFUNDABLE_VALUE")
        void manualReviewWithZeroRefundable() {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Pur purchase = new Pur();
            purchase.couponUsed = true;
            purchase.discountAmount = bd("200.00");
            purchase.method = PaymentMethod.CRYPTO;
            purchase.paidAt = T0.minusSeconds(100 * 3600);
            Ref request = new Ref();
            request.purchase = purchase.build();
            RefundDecision decision = service.evaluate(request.build());
            assertTrue(decision.manualReview());
            assertFalse(decision.reasons().contains("NO_REFUNDABLE_VALUE"));
        }

        @Test
        @DisplayName("反射直调 calculateReturnedPoints：fraudSuspected 但原因不是 FRAUD")
        void fraudSuspectedFlagOnlyViaReflection() throws Exception {
            RefundPolicyService service = new RefundPolicyService(CLOCK);
            Method method = RefundPolicyService.class.getDeclaredMethod("calculateReturnedPoints",
                    PurchaseSnapshot.class, RefundRequest.class, BigDecimal.class, List.class);
            method.setAccessible(true);
            Pur purchase = new Pur();
            purchase.loyaltyUsed = 1000;
            Ref request = new Ref();
            request.purchase = purchase.build();
            request.reason = RefundReason.OTHER;
            request.fraud = true;
            List<String> warnings = new ArrayList<>();
            assertEquals(0, (int) method.invoke(service, purchase.build(), request.build(),
                    BigDecimal.ONE, warnings));
        }

        @Test
        @DisplayName("必修作业审批状态的两侧（通过/未通过）")
        void mandatoryAssignmentApprovalSides() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap approved = new Snap();
            approved.instructorApprovalRequired = true;
            approved.lesson(new Les().type(LessonType.ASSIGNMENT).mandatory(true)
                    .approved(true).build());
            assertFalse(service.evaluate(approved.build()).blockers()
                    .contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            Snap rejected = new Snap();
            rejected.instructorApprovalRequired = true;
            rejected.lesson(new Les().type(LessonType.ASSIGNMENT).mandatory(true)
                    .approved(false).build());
            assertTrue(service.evaluate(rejected.build()).blockers()
                    .contains("INSTRUCTOR_APPROVAL_REQUIRED"));

            Snap optional = new Snap();
            optional.instructorApprovalRequired = true;
            optional.lesson(new Les().type(LessonType.ASSIGNMENT).mandatory(false)
                    .approved(false).build());
            assertFalse(service.evaluate(optional.build()).blockers()
                    .contains("INSTRUCTOR_APPROVAL_REQUIRED"));
        }

        @Test
        @DisplayName("顺序学习：前一课已完成时后续课不再触发绕过警告")
        void sequentialPreviousCompleteTrue() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 0;
            snapshot.lesson(new Les().id("l1").order(1).mandatory(true).completion(100).build());
            snapshot.lesson(new Les().id("l2").order(2).completion(50).build());
            assertFalse(service.evaluate(snapshot.build()).warnings()
                    .contains("SEQUENCE_RULE_BYPASSED:l2"));
        }

        @Test
        @DisplayName("编程题零测试但需要人工复核：公开路径可构造")
        void programmingZeroTestsManualReviewPublic() {
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.PROGRAMMING).totalTests(0).manualReview(true).build()).build(),
                    pol().build());
            assertEquals(bd("0.00"), report.rawPoints());
            assertTrue(report.warnings().contains("PROGRAM_HAS_NO_AUTOMATED_TESTS:q1"));
            assertTrue(report.manualReview());
        }

        @Test
        @DisplayName("数值题部分给分开关关闭时容忍度分支不再求值")
        void numericPartialCreditDisabled() {
            Pol strict = pol();
            strict.partialCreditEnabled = false;
            GradeReport report = grading.grade(sub().plus(ans()
                    .type(QuestionType.NUMERIC).exact(false).numericError(bd("5"))
                    .numericTolerance(bd("1")).build()).build(), strict.build());
            assertEquals(bd("0.00"), report.rawPoints());
        }
    }

    @Nested
    @DisplayName("分支覆盖补测第三轮（可达侧收尾）")
    class BranchCoverage3 {

        @Test
        @DisplayName("组织校验：有赞助且提供组织 ID 时无阻塞；赞助缺失时只报赞助")
        void organizationFilledSides() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Off organization = new Off();
            organization.organizationOnly = true;

            Req filled = new Req();
            filled.course = organization.build();
            filled.sponsored = true;
            filled.orgId = "org-1";
            assertTrue(service.evaluate(filled.build()).reasons().isEmpty());

            Req noSponsor = new Req();
            noSponsor.course = organization.build();
            noSponsor.orgId = "org-1";
            assertEquals(List.of("ORGANIZATION_SPONSOR_REQUIRED"),
                    service.evaluate(noSponsor.build()).reasons());
        }

        @Test
        @DisplayName("团体报名：非企业但由组织赞助时不报 ENTERPRISE 限制")
        void groupEnrollmentSponsored() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Off course = new Off();
            course.capacity = 100;
            course.enrolled = 0;
            Req request = new Req();
            request.course = course.build();
            request.seats = 3;
            request.sponsored = true;
            assertFalse(service.evaluate(request.build()).reasons()
                    .contains("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE"));
        }

        @Test
        @DisplayName("先修课得分为负时判为非法")
        void negativePrerequisiteScore() {
            History history = new History();
            EnrollmentEligibilityService service = new EnrollmentEligibilityService(history, CLOCK);
            Off course = new Off();
            course.prereqs = Set.of("pre-1");
            history.completed.add("pre-1");
            history.scores.put("pre-1", -1);
            Req request = new Req();
            request.course = course.build();
            assertTrue(service.evaluate(request.build()).reasons()
                    .contains("INVALID_PREREQUISITE_SCORE:pre-1"));
            history.completed.clear();
            history.scores.clear();
        }

        @Test
        @DisplayName("记录类空集合归一化：CourseOffering / Coupon / CourseProgressSnapshot")
        void domainNullCollections() {
            CourseOffering offering = new CourseOffering("c1", "i1",
                    EnrollmentCourseStatus.PUBLISHED, null, null, null, 10, 0, false, 0, 0, 0,
                    0, null, null, null, 0, 0, 0, false, null, false, false, null, 0, 0, null);
            assertTrue(offering.allowedCountries().isEmpty());
            assertTrue(offering.prerequisiteCourseIds().isEmpty());

            Coupon coupon = new Coupon("c", CouponType.PERCENT, BigDecimal.TEN, null, null, null,
                    1, true, null, MembershipLevel.NONE, 0);
            assertTrue(coupon.eligibleCourseIds().isEmpty());

            CourseProgressSnapshot snapshot = new CourseProgressSnapshot("s", "c", null, false, 0,
                    0, true, true, true, false, true, null, null, false, null, false, false, false);
            assertTrue(snapshot.lessons().isEmpty());
            assertTrue(new LearningProgressService(CLOCK).evaluate(snapshot).blockers()
                    .contains("COURSE_HAS_NO_LESSONS"));
        }

        @Test
        @DisplayName("顺序学习：目标课未解锁时既不选为下一课也不写绕过警告")
        void sequentialLockedLessonSides() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 0;
            snapshot.lesson(new Les().id("l1").order(1).completion(10).unlocked(false).build());
            snapshot.lesson(new Les().id("l2").order(2).completion(0).unlocked(false).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertFalse(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
            assertNull(evaluation.nextLessonId());
        }

        @Test
        @DisplayName("顺序学习：前课未完成时后续解锁课不被选为下一课")
        void sequentialNextLessonGuard() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 0;
            snapshot.lesson(new Les().id("l1").order(1).completion(10).unlocked(false).build());
            snapshot.lesson(new Les().id("l2").order(2).completion(0).unlocked(true).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertNull(evaluation.nextLessonId());
            assertTrue(evaluation.warnings().contains("SEQUENCE_RULE_BYPASSED:l2"));
        }

        @Test
        @DisplayName("顺序学习：前一课未完成时后续解锁课不成为下一课")
        void sequentialPreviousIncompleteBlocksNext() {
            LearningProgressService service = new LearningProgressService(CLOCK);
            Snap snapshot = new Snap();
            snapshot.sequential = true;
            snapshot.requiredOverallPercent = 0;
            snapshot.lesson(new Les().id("l1").order(1).mandatory(true).completion(10).build());
            snapshot.lesson(new Les().id("l2").order(2).completion(0).unlocked(true).build());
            ProgressEvaluation evaluation = service.evaluate(snapshot.build());
            assertEquals("l1", evaluation.nextLessonId());
        }

        @Test
        @DisplayName("审计模式与归档课程的组合")
        void auditArchivedCombination() {
            EnrollmentEligibilityService service =
                    new EnrollmentEligibilityService(new History(), CLOCK);
            Off archived = new Off();
            archived.status = EnrollmentCourseStatus.ARCHIVED;
            Req audit = new Req();
            audit.course = archived.build();
            audit.audit = true;
            EnrollmentDecision decision = service.evaluate(audit.build());
            assertFalse(decision.reasons().contains("COURSE_ARCHIVED"));
            assertTrue(decision.warnings().contains("ARCHIVED_COURSE_AUDIT_ONLY"));
        }
    }

    // ==================================================================
    // 退款时间窗“等值点”与复数表达式精度标志（补充边界回归）
    //
    // 目标：覆盖 RefundPolicyService 三个时间窗判断在“恰好等于阈值”
    // 时的行为，以及 ComplexExpression 在 double 模式下的精度标志。
    // 全部使用公开 API 与固定时钟 CLOCK，结果可重复。
    // ==================================================================
    @Nested
    @DisplayName("退款时间窗等值点与复数表达式精度标志")
    class RefundWindowAndComplexPrecisionTests {

        /**
         * 构造一笔退款申请并返回评估结果。
         *
         * @param hoursSincePurchase 从支付到当前时刻的小时数，用于命中不同时间窗
         * @param progressPercent    课程学习进度百分比
         * @return 退款策略服务给出的决策
         */
        private RefundDecision evaluateRefund(long hoursSincePurchase, int progressPercent) {
            // 构造购买快照：支付时间由固定时钟 CLOCK 反推，保证结果确定。
            Pur purchase = new Pur();
            purchase.paidAt = T0.minusSeconds(hoursSincePurchase * 3600L);
            purchase.progress = progressPercent;

            // 构造退款请求：默认“全额退款”与“意外购买”原因，便于命中比例条款。
            Ref request = new Ref();
            request.purchase = purchase.build();

            // 调用被测服务；Clock 固定后，小时差恰好等于传入值。
            return new RefundPolicyService(CLOCK).evaluate(request.build());
        }

        @Test
        @DisplayName("七天部分退款：恰好 168 小时仍按 70% 比例")
        void sevenDayWindowIncludesBoundaryHour() {
            // 168 小时是 `hoursSincePurchase <= 7 * 24` 的等值点；若边界被收窄为 `<`，
            // 就会错误落入十四天条款，退款比例随之不同。
            RefundDecision decision = evaluateRefund(7 * 24, 20);

            // 断言命中七天部分退款条款，并给出 70% 的退款金额。
            assertTrue(decision.reasons().contains("SEVEN_DAY_PARTIAL_REFUND"),
                    decision.reasons()::toString);
            assertEquals(bd("67.90"), decision.refundableAmount());
        }

        @Test
        @DisplayName("十四天部分退款：恰好 336 小时仍按 50% 比例")
        void fourteenDayWindowIncludesBoundaryHour() {
            // 336 小时是 `hoursSincePurchase <= 14 * 24` 的等值点。
            RefundDecision decision = evaluateRefund(14 * 24, 20);

            // 断言命中十四天部分退款条款。
            assertTrue(decision.reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"),
                    decision.reasons()::toString);
        }

        @Test
        @DisplayName("三十天例外：恰好 720 小时仍按 25% 比例")
        void thirtyDayWindowIncludesBoundaryHour() {
            // 720 小时是 `hoursSincePurchase <= 30 * 24` 的等值点。
            RefundDecision decision = evaluateRefund(30 * 24, 10);

            // 断言命中三十天例外条款。
            assertTrue(decision.reasons().contains("THIRTY_DAY_EXCEPTION"),
                    decision.reasons()::toString);
        }

        @Test
        @DisplayName("混合授课教室编号为 null 时仍报告 HYBRID_ROOM_REQUIRED")
        void hybridSessionWithNullRoomReportsConflict() {
            // 构造混合授课请求：教室编号显式置空，会议链接合法。
            Sch request = new Sch();
            request.delivery = DeliveryMode.HYBRID;
            request.roomCapacity = 50;
            request.roomId = null;
            request.meetingUrl = "https://meet.example/hybrid";

            // 调用被测服务并断言冲突原因。
            SchedulingDecision decision = new SchedulingService(CLOCK).evaluate(request.build());
            assertTrue(decision.conflicts().contains("HYBRID_ROOM_REQUIRED"),
                    decision.conflicts()::toString);
        }

        @Test
        @DisplayName("周日的重复课程给出周末提醒")
        void recurringSundaySessionWarnsAboutWeekend() {
            // 2026-11-22 是周日（亚洲/上海时区当地时间为上午 10 点）。
            Sch request = new Sch();
            request.recurring = true;
            request.occurrences = 3;
            request.weekendAllowed = true;
            request.start("2026-11-22T02:00:00Z", 60);

            // 断言周末重复课程提醒出现，覆盖“周日”这一操作数。
            SchedulingDecision decision = new SchedulingService(CLOCK).evaluate(request.build());
            assertTrue(decision.warnings().contains("RECURRING_WEEKEND_SESSION"),
                    decision.warnings()::toString);
        }
    }
}
