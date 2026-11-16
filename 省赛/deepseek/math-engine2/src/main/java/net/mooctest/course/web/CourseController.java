package net.mooctest.course.web;

import net.mooctest.common.ApiResponse;
import net.mooctest.course.domain.Course;
import net.mooctest.course.domain.CourseCatalog;
import net.mooctest.course.service.CatalogService;
import net.mooctest.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final CatalogService catalogService;

    public CourseController(CourseService courseService, CatalogService catalogService) {
        this.courseService = courseService;
        this.catalogService = catalogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Course>> create(@Valid @RequestBody CreateCourseRequest request) {
        Course course = courseService.createCourse(request.title(), request.price());
        return ResponseEntity
                .created(URI.create("/api/courses/" + course.id()))
                .body(ApiResponse.success(course));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<Course> get(@PathVariable String courseId) {
        return ApiResponse.success(courseService.getCourse(courseId));
    }

    @PostMapping("/{courseId}/publish")
    public ApiResponse<Course> publish(@PathVariable String courseId) {
        return ApiResponse.success(courseService.publishCourse(courseId));
    }

    @GetMapping("/{courseId}/catalog")
    public ApiResponse<CourseCatalog> catalog(@PathVariable String courseId) {
        return ApiResponse.success(catalogService.getCatalog(courseId));
    }
}
