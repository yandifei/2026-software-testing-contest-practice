package net.mooctest.scheduling.domain;

import java.time.Instant;

public record TimeWindow(Instant start, Instant end) {
}
