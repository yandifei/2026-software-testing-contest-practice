package net.mooctest.course.domain;

public record Video(String id, String chapterId, String title, int durationSeconds, int sortOrder) {
}
