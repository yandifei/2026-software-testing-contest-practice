package net.mooctest.course.domain;

import java.util.List;

public record ChapterView(String id, String title, List<VideoView> videos) {

    public ChapterView {
        videos = List.copyOf(videos);
    }
}
