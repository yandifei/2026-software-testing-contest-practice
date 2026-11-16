package net.mooctest.course.port;

import net.mooctest.course.domain.Course;

import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(String id);

    boolean existsByTitleIgnoreCase(String title);
}
