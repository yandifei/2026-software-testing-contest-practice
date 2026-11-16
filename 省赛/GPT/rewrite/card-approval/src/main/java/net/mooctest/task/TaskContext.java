package net.mooctest.task;

import java.time.Instant;
import java.util.Objects;

public record TaskContext(
        String actorId,
        Instant now,
        boolean actorIsManager,
        boolean actorIsAdministrator,
        boolean dependenciesComplete,
        boolean assigneeActive,
        boolean projectActive,
        boolean withinWorkingHours,
        boolean notificationsEnabled,
        int previousReopens,
        int assigneeOpenTaskCount,
        int overdueTaskCount) {

    public TaskContext {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        actorId = actorId.trim();
        now = Objects.requireNonNull(now, "now must not be null");
        if (previousReopens < 0 || assigneeOpenTaskCount < 0 || overdueTaskCount < 0) {
            throw new IllegalArgumentException("task counters must not be negative");
        }
    }
}
