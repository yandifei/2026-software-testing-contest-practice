package net.mooctest;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ResourceBehaviorTest {
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Test
    void metadataDefaultsAndSetters() {
        Resource resource = new Resource(null, null);
        assertTrue(resource.getId() > 0);
        assertEquals("", resource.getName());
        assertEquals("GENERIC", resource.getType());
        resource.setName("Microscope");
        resource.setType("LAB");
        assertEquals("Microscope", resource.getName());
        assertEquals("LAB", resource.getType());
        resource.setName(null);
        resource.setType(null);
        assertEquals("", resource.getName());
        assertEquals("GENERIC", resource.getType());
        Resource named = new Resource("Room", "SPACE");
        assertEquals("Room", named.getName());
        assertEquals("SPACE", named.getType());
    }

    @Test
    void invalidIntervalsDoNotCreateBookings() {
        Resource resource = new Resource("r", "t");
        assertFalse(resource.isAvailable(null, BASE));
        assertFalse(resource.isAvailable(BASE, null));
        assertFalse(resource.isAvailable(BASE, BASE));
        assertFalse(resource.isAvailable(BASE, BASE.minusMinutes(1)));
        assertFalse(resource.book(BASE, BASE));
        assertTrue(resource.listBookings().isEmpty());
        resource.cancel(null);
        resource.cancel(BASE);
        assertTrue(resource.listBookings().isEmpty());
    }

    @Test
    void bookingsCheckPrecedingEndAndCanBeCancelled() {
        Resource resource = new Resource("r", "t");
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(1)));
        assertTrue(resource.book(BASE, BASE.plusHours(1)));
        assertFalse(resource.book(BASE, BASE.plusHours(2)));
        assertFalse(resource.isAvailable(BASE.plusMinutes(30), BASE.plusHours(2)));
        assertFalse(resource.isAvailable(BASE.plusHours(1), BASE.plusHours(2)));
        assertTrue(resource.isAvailable(BASE.plusHours(1).plusSeconds(1), BASE.plusHours(2)));
        assertTrue(resource.book(BASE.plusHours(2), BASE.plusHours(3)));
        assertEquals(2, resource.listBookings().size());
        assertEquals(BASE, resource.listBookings().get(0).getKey());
        assertEquals(BASE.plusHours(1), resource.listBookings().get(0).getValue());
        resource.listBookings().clear();
        assertEquals(2, resource.listBookings().size());
        resource.cancel(BASE);
        assertEquals(1, resource.listBookings().size());
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(1)));
    }

    @Test
    void characterizesIgnoredFutureOverlapAndConflictStub() {
        Resource resource = new Resource("r", "t");
        assertTrue(resource.book(BASE.plusHours(1), BASE.plusHours(2)));
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(3)),
                "Known defect: later bookings are ignored");
        assertFalse(resource.conflicts(BASE.plusHours(1), BASE.plusHours(2)),
                "Known defect: conflicts always returns false");
        assertFalse(resource.conflicts(null, null));
    }
}
