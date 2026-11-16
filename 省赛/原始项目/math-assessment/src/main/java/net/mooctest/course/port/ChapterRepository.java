package net.mooctest.course.port;

import net.mooctest.course.domain.Chapter;

import java.util.List;

public interface ChapterRepository {

    List<Chapter> findByCourseId(String courseId);
}
