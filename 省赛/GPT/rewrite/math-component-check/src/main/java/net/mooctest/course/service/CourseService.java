package net.mooctest.course.service;

import net.mooctest.course.domain.Chapter;
import net.mooctest.course.domain.Course;
import net.mooctest.course.domain.CourseStatus;
import net.mooctest.course.port.ChapterRepository;
import net.mooctest.course.port.CourseRepository;
import net.mooctest.course.port.IdGenerator;
import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final IdGenerator idGenerator;
    private final PricePolicy pricePolicy;
    private final Clock clock;

    public CourseService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            IdGenerator idGenerator,
            PricePolicy pricePolicy,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.idGenerator = idGenerator;
        this.pricePolicy = pricePolicy;
        this.clock = clock;
    }

    public Course createCourse(String title, BigDecimal price) {
        String normalizedTitle = normalizeTitle(title);
        BigDecimal normalizedPrice = pricePolicy.normalizePrice(price);

        if (courseRepository.existsByTitleIgnoreCase(normalizedTitle)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_COURSE_TITLE,
                    "A course with the same title already exists"
            );
        }

        Course course = new Course(
                idGenerator.nextId(),
                normalizedTitle,
                normalizedPrice,
                CourseStatus.DRAFT,
                Instant.now(clock)
        );
        return courseRepository.save(course);
    }

    public Course getCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.COURSE_NOT_FOUND,
                        "Course not found: " + courseId
                ));
    }

    public Course publishCourse(String courseId) {
        Course course = getCourse(courseId);
        Course publishedCourse = course.publish();
        List<Chapter> chapters = chapterRepository.findByCourseId(courseId);
        if (chapters.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.COURSE_HAS_NO_CHAPTERS,
                    "A course must contain at least one chapter before publication"
            );
        }
        return courseRepository.save(publishedCourse);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            throw invalidTitle();
        }
        String normalized = title.strip().replaceAll("\\s+", " ");
        if (normalized.length() < 3 || normalized.length() > 80) {
            throw invalidTitle();
        }
        return normalized;
    }

    private BusinessException invalidTitle() {
        return new BusinessException(
                ErrorCode.INVALID_COURSE_TITLE,
                "Course title length must be between 3 and 80 characters"
        );
    }
}
