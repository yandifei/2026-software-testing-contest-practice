package net.mooctest.course.adapter;

import net.mooctest.course.domain.Chapter;
import net.mooctest.course.port.ChapterRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryChapterRepository implements ChapterRepository {

    @Override
    public List<Chapter> findByCourseId(String courseId) {
        return List.of();
    }
}
