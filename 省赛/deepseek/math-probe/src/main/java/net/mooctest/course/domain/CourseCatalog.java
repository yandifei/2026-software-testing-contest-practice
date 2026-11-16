package net.mooctest.course.domain;

import java.util.List;

public record CourseCatalog(String courseId, List<ChapterView> chapters) {

    public CourseCatalog {
        chapters = List.copyOf(chapters);
    }
}
