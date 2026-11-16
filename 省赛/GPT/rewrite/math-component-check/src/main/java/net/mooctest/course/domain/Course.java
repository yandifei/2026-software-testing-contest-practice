package net.mooctest.course.domain;

import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public record Course(
        String id,
        String title,
        BigDecimal price,
        CourseStatus status,
        Instant createdAt
) {

    public Course {
        id = requireText(id, "id");
        title = requireText(title, "title");
        price = Objects.requireNonNull(price, "price must not be null").setScale(2, RoundingMode.HALF_UP);
        status = Objects.requireNonNull(status, "status must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Course publish() {
        if (status != CourseStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.COURSE_ALREADY_PUBLISHED,
                    "Only a draft course can be published"
            );
        }
        return new Course(id, title, price, CourseStatus.PUBLISHED, createdAt);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
