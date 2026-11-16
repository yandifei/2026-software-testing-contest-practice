package net.mooctest.course.port;

import net.mooctest.course.domain.Video;

import java.util.List;
import java.util.Set;

public interface VideoRepository {

    List<Video> findByChapterIds(Set<String> chapterIds);
}
