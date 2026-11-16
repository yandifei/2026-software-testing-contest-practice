package net.mooctest.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

public record ExpenseItem(
        String itemId,
        ExpenseCategory category,
        BigDecimal amount,
        Currency currency,
        LocalDate incurredDate,
        String merchant,
        String businessPurpose,
        ReceiptType receiptType,
        PaymentMethod paymentMethod,
        int attendeeCount,
        boolean alcoholIncluded,
        boolean personalComponent,
        boolean suspectedDuplicate) {

    public ExpenseItem {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        itemId = itemId.trim();
        category = Objects.requireNonNull(category, "category must not be null");
        amount = Objects.requireNonNull(amount, "amount must not be null");
        currency = Objects.requireNonNull(currency, "currency must not be null");
        incurredDate = Objects.requireNonNull(incurredDate, "incurredDate must not be null");
        receiptType = Objects.requireNonNull(receiptType, "receiptType must not be null");
        paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (amount.scale() > 2) {
            amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if (attendeeCount < 0 || attendeeCount > 1000) {
            throw new IllegalArgumentException("attendeeCount is outside the supported range");
        }
        if (category != ExpenseCategory.CLIENT_ENTERTAINMENT && attendeeCount > 0) {
            throw new IllegalArgumentException("attendeeCount is only valid for client entertainment");
        }
    }
}
