package net.mooctest.card;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Checks card-number syntax, checksum, supported issuer and expiry for expense evidence. */
public final class CardEvidenceValidator {
    private final Clock clock;

    public CardEvidenceValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CardEvidenceCheck check(CardEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        List<String> issues = new ArrayList<>();
        String number = evidence.number();
        CreditCardType type = CreditCardType.OTHER;
        if (!number.matches("[0-9]{12,19}")) {
            issues.add("card number must contain 12 to 19 digits");
        } else {
            if (!new LuhnValidator().validate(number)) {
                issues.add("card number checksum is invalid");
            }
            type = TypeChecker.checkType(number);
            if (type == CreditCardType.OTHER) {
                issues.add("card network is not supported");
            }
        }
        if (!DateChecker.isValidExpiration(evidence.expires(), clock)) {
            issues.add("card has expired");
        }
        return new CardEvidenceCheck(evidence.itemId(), type, issues.isEmpty(), issues);
    }
}
