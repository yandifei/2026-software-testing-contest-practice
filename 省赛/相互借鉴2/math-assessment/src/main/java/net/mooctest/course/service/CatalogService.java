package net.mooctest.course.service;

import net.mooctest.course.domain.Chapter;
import net.mooctest.course.domain.ChapterView;
import net.mooctest.course.domain.CourseCatalog;
import net.mooctest.course.domain.Video;
import net.mooctest.course.domain.VideoView;
import net.mooctest.course.port.ChapterRepository;
import net.mooctest.course.port.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Comparator<Chapter> CHAPTER_ORDER = Comparator
            .comparingInt(Chapter::sortOrder)
            .thenComparing(Chapter::title, String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<Video> VIDEO_ORDER = Comparator
            .comparingInt(Video::sortOrder)
            .thenComparing(Video::title, String.CASE_INSENSITIVE_ORDER);

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final VideoRepository videoRepository;

    public CatalogService(
            CourseService courseService,
            ChapterRepository chapterRepository,
            VideoRepository videoRepository
    ) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.videoRepository = videoRepository;
    }

    public CourseCatalog getCatalog(String courseId) {
        courseService.getCourse(courseId);

        List<Chapter> chapters = chapterRepository.findByCourseId(courseId)
                .stream()
                .sorted(CHAPTER_ORDER)
                .toList();
        if (chapters.isEmpty()) {
            return new CourseCatalog(courseId, List.of());
        }

        Set<String> chapterIds = chapters.stream()
                .map(Chapter::id)
                .collect(Collectors.toUnmodifiableSet());
        Map<String, List<Video>> videosByChapter = videoRepository.findByChapterIds(chapterIds)
                .stream()
                .sorted(VIDEO_ORDER)
                .collect(Collectors.groupingBy(Video::chapterId));

        List<ChapterView> views = chapters.stream()
                .map(chapter -> toView(chapter, videosByChapter))
                .toList();
        return new CourseCatalog(courseId, views);
    }

    private ChapterView toView(Chapter chapter, Map<String, List<Video>> videosByChapter) {
        List<VideoView> videos = videosByChapter.getOrDefault(chapter.id(), List.of())
                .stream()
                .map(video -> new VideoView(video.id(), video.title(), video.durationSeconds()))
                .toList();
        return new ChapterView(chapter.id(), chapter.title(), videos);
    }
}
