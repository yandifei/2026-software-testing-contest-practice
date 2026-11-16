package net.mooctest;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Branch-coverage tests for the enrollment module: {@link EnrollmentEligibilityService},
 * its domain records, the {@link EnrollmentHistory} port and the {@link EmptyEnrollmentHistory} adapter.
 *
 * <p>组织约定（与 StudentTest 一致）:</p>
 * <ul>
 *   <li>每个 {@code @Nested} 类对应一个被测片段（结构校验 / 学生状态 / 课程状态 / 人口属性 /
 *       组织与订阅 / 历史与风险 / 先修课 / 排课 / 周上限 / 支付 / 容量与候补 / 适配器集成 / 领域记录）。</li>
 *   <li>每个测试独立构造 student/course/request 与 service，不共享可变状态；
 *       期望值全部硬编码，绝不复用生产代码公式。</li>
 *   <li>每个数值规则做边界矩阵（&lt; / == / &gt;），每个枚举分支覆盖全部取值，
 *       每条校验规则单独一个测试并断言精确异常消息、完整 blockers/warnings 列表。</li>
 * </ul>
 *
 * // DEAD: L367 (evaluatePrerequisites) `prerequisite == null` 的 true 侧不可达——
 * // CourseOffering 的 compact constructor 执行 Set.copyOf(prerequisiteCourseIds)，
 * // 而 Set.copyOf 对任何 null 元素抛 NPE（courseOfferingRejectsNullElementsInCollections
 * // 已验证），故经公共 API 构造的 CourseOffering 不可能携带 null 先修课进入该判断；
 * // 同条件的 isBlank 侧已由 blankPrerequisiteIdWarnsBadConfiguration 覆盖。
 */
class EnrollmentModuleTest {

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
    }
}
