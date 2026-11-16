package net.mooctest.card;

import java.time.YearMonth;
import java.util.Objects;

/** Card details supplied temporarily for a corporate-card expense item. */
public record CardEvidence(String itemId, String number, YearMonth expires) {
    public CardEvidence {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        itemId = itemId.trim();
        number = Objects.requireNonNull(number, "number must not be null");
        expires = Objects.requireNonNull(expires, "expires must not be null");
    }
}
