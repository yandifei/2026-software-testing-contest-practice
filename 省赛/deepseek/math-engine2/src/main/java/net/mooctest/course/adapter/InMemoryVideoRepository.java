package net.mooctest.course.adapter;

import net.mooctest.course.domain.Video;
import net.mooctest.course.port.VideoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class InMemoryVideoRepository implements VideoRepository {

    @Override
    public List<Video> findByChapterIds(Set<String> chapterIds) {
        return List.of();
    }
}
