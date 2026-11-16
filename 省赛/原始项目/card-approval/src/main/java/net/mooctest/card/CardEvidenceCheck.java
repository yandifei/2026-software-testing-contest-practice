package net.mooctest.card;

import java.util.List;
import java.util.Objects;

/** Validation result excludes the card number itself. */
public record CardEvidenceCheck(String itemId, CreditCardType type, boolean valid, List<String> issues) {
    public CardEvidenceCheck {
        itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues must not be null"));
        if (valid != issues.isEmpty()) {
            throw new IllegalArgumentException("valid must agree with issues");
        }
    }
}
