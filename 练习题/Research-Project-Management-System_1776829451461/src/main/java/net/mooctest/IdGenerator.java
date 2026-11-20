package net.mooctest;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicLong COUNTER = new AtomicLong(System.nanoTime());

    private IdGenerator() {}

    public static long nextId() {
        long base = System.currentTimeMillis();
        long c = COUNTER.incrementAndGet();
        long id = (base << 20) ^ c;
        if (id < 0) id = -id;
        return id;
    }

    public static String nextIdStr() {
        return Long.toUnsignedString(nextId());
    }

    public static long fromString(String s) {
        if (s == null || s.isEmpty()) throw new DomainException("id string invalid");
        try {
            long v = Long.parseUnsignedLong(s);
            if (v == 0) throw new DomainException("id zero invalid");
            return v;
        } catch (NumberFormatException e) {
            throw new DomainException("id parse failed", e);
        }
    }
}
