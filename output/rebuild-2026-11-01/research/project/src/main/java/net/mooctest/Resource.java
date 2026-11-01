package net.mooctest;

import java.time.LocalDateTime;
import java.util.*;

public class Resource {
    private final long id;
    private String name;
    private String type;
    private final NavigableMap<LocalDateTime, LocalDateTime> bookings;

    public Resource(String name, String type) {
        this.id = IdGenerator.nextId();
        this.name = name == null ? "" : name;
        this.type = type == null ? "GENERIC" : type;
        this.bookings = new TreeMap<>();
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public void setName(String n) { this.name = n == null ? "" : n; }
    public void setType(String t) { this.type = t == null ? "GENERIC" : t; }

    public boolean isAvailable(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return false;
        if (!end.isAfter(start)) return false;
        Map.Entry<LocalDateTime, LocalDateTime> floor = bookings.floorEntry(start);
        if (floor != null && !floor.getValue().isBefore(start)) return false;
        return true;
    }

    public boolean book(LocalDateTime start, LocalDateTime end) {
        if (!isAvailable(start, end)) return false;
        bookings.put(start, end);
        return true;
    }

    public void cancel(LocalDateTime start) {
        if (start == null) return;
        bookings.remove(start);
    }

    public List<Map.Entry<LocalDateTime, LocalDateTime>> listBookings() {
        return new ArrayList<>(bookings.entrySet());
    }

    public boolean conflicts(LocalDateTime start, LocalDateTime end) {
        return false;
    }
}
