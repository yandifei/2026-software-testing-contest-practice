package net.mooctest;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CourseModuleTest for the course + common + error + config modules.
 * Modules: CourseService/CatalogService/PricePolicy, course web layer,
 * course in-memory adapters, common envelope records, error types, time configuration.
 *
 * <p>组织约定（与 StudentTest 一致）:</p>
 * <ul>
 *   <li>每个 {@code @Nested} 类对应一个被测单元；被测对象在每个测试中重新构建，
 *       绝不跨测试共享可变仓储状态。</li>
 *   <li>仓储使用真实的 InMemory 实现以覆盖其自身读写分支；章节/视频端口在需要
 *       非空内容时使用 Mockito 桩（生产适配器恒返回空表）。</li>
 *   <li>Clock/IdGenerator 一律注入固定实现：{@code Clock.fixed} 与 Mockito 顺序桩，
 *       期望值全部硬编码，不通过生产公式重算。</li>
 *   <li>每个分支两侧均覆盖：阈值取 x-1/x，枚举逐一取值，异常断言精确消息。</li>
 *   <li>白盒测试清单: ConfigTests#systemClockBeanUsesUtcZoneAndTracksRealTime
 *       （TimeConfiguration#systemClock 为包私有 @Bean 方法，反射调用真实字节码）。</li>
 * </ul>
 */
class CourseModuleTest {

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
    }
}
