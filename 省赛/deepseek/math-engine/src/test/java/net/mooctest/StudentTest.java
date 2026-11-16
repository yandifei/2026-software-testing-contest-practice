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
import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.domain.QuestionType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // __PART_F__
}
