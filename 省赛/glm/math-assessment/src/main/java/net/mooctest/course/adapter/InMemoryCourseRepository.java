package net.mooctest.course.adapter;

import net.mooctest.course.domain.Course;
import net.mooctest.course.port.CourseRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCourseRepository implements CourseRepository {

    private final Map<String, Course> courses = new ConcurrentHashMap<>();

    @Override
    public Course save(Course course) {
        courses.put(course.id(), course);
        return course;
    }

    @Override
    public Optional<Course> findById(String id) {
        return Optional.ofNullable(courses.get(id));
    }

    @Override
    public boolean existsByTitleIgnoreCase(String title) {
        return courses.values().stream()
                .anyMatch(course -> course.title().equalsIgnoreCase(title));
    }
}
