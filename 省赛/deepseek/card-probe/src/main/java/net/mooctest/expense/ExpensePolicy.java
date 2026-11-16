package net.mooctest.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Expense decision rules with no persistence or network implementation. */
public final class ExpensePolicy {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal LARGE_ITEM = new BigDecimal("5000.00");

    public ExpenseAssessment assess(
            ExpenseClaim claim,
            ExchangeRateProvider exchangeRates,
            LocalDate today) {
        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(exchangeRates, "exchangeRates must not be null");
        Objects.requireNonNull(today, "today must not be null");

        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> seenFingerprints = new HashSet<>();
        BigDecimal claimed = ZERO;
        BigDecimal reimbursable = ZERO;
        boolean auditRequired = false;

        validateClaimLevel(claim, today, violations, warnings);
        for (ExpenseItem item : claim.items()) {
            BigDecimal baseAmount = convert(item, exchangeRates, violations);
            claimed = claimed.add(baseAmount);

            validateCommonItemRules(item, claim, today, baseAmount, violations, warnings);
            validateDuplicate(item, seenFingerprints, violations);

            BigDecimal allowed = allowedAmount(item, claim, baseAmount, violations, warnings);
            reimbursable = reimbursable.add(allowed);

            if (item.suspectedDuplicate()) {
                auditRequired = true;
            }
            if (item.personalComponent()) {
                auditRequired = true;
            }
            if (item.amount().compareTo(LARGE_ITEM) > 0) {
                auditRequired = true;
            }
            if (item.paymentMethod() == PaymentMethod.CASH
                    && baseAmount.compareTo(new BigDecimal("1000")) > 0) {
                auditRequired = true;
            }
            if (item.receiptType() == ReceiptType.BANK_STATEMENT) {
                auditRequired = true;
            }
        }

        claimed = money(claimed);
        reimbursable = money(reimbursable.max(ZERO));
        if (reimbursable.compareTo(claimed) < 0) {
            warnings.add("one or more items were capped or excluded");
        }
        if (claim.previousClaimsThisMonth() >= 10) {
            auditRequired = true;
            warnings.add("employee has a high monthly claim frequency");
        }
        if (claim.items().size() >= 20) {
            auditRequired = true;
        }
        if (violations.size() >= 3) {
            auditRequired = true;
        }

        boolean rejected = containsFatalViolation(violations) || reimbursable.signum() == 0;
        ExpenseApprovalTier tier = resolveApprovalTier(claim, claimed, auditRequired, rejected);
        return new ExpenseAssessment(
                claimed,
                rejected ? ZERO : reimbursable,
                tier,
                rejected,
                auditRequired,
                violations,
                warnings);
    }

    private static void validateClaimLevel(
            ExpenseClaim claim,
            LocalDate today,
            List<String> violations,
            List<String> warnings) {
        if (claim.submittedDate().isAfter(today)) {
            violations.add("FATAL: submission date cannot be in the future");
        }
        if (ChronoUnit.DAYS.between(claim.submittedDate(), today) > 30) {
            warnings.add("claim review is occurring more than thirty days after submission");
        }
        if (!claim.projectActive() && claim.clientBillable()) {
            violations.add("FATAL: inactive project cannot receive client-billable expenses");
        }
        if (claim.costCenter() == null || claim.costCenter().isBlank()) {
            if (claim.clientBillable()) {
                violations.add("FATAL: client-billable claim requires cost center");
            } else {
                warnings.add("cost center is missing");
            }
        }
        if (claim.employeeGrade() == EmployeeGrade.INTERN && claim.internationalTrip()) {
            violations.add("FATAL: intern international travel requires a separate process");
        }
        if (claim.items().size() > 50) {
            warnings.add("large claim should be split by trip or purpose");
        }
        if (claim.previousClaimsThisMonth() > 20) {
            warnings.add("unusually high claim frequency");
        }
        if (claim.managerPreApproved() && claim.employeeGrade() == EmployeeGrade.EXECUTIVE) {
            warnings.add("executive pre-approval still requires finance review");
        }
    }

    private static BigDecimal convert(
            ExpenseItem item,
            ExchangeRateProvider exchangeRates,
            List<String> violations) {
        BigDecimal rate = exchangeRates.rateToBase(item.currency(), item.incurredDate());
        if (rate == null) {
            violations.add("FATAL: missing exchange rate for item " + item.itemId());
            return ZERO;
        }
        if (rate.signum() <= 0) {
            violations.add("FATAL: invalid exchange rate for item " + item.itemId());
            return ZERO;
        }
        if (rate.compareTo(new BigDecimal("1000")) > 0) {
            violations.add("FATAL: implausible exchange rate for item " + item.itemId());
            return ZERO;
        }
        return money(item.amount().multiply(rate));
    }

    private static void validateCommonItemRules(
            ExpenseItem item,
            ExpenseClaim claim,
            LocalDate today,
            BigDecimal baseAmount,
            List<String> violations,
            List<String> warnings) {
        if (item.incurredDate().isAfter(today)) {
            violations.add("FATAL: future expense date for item " + item.itemId());
        }
        long age = ChronoUnit.DAYS.between(item.incurredDate(), claim.submittedDate());
        if (age > 90) {
            violations.add("FATAL: expense older than ninety days for item " + item.itemId());
        } else if (age > 30) {
            warnings.add("late submission for item " + item.itemId());
        }
        if (item.merchant() == null || item.merchant().isBlank()) {
            violations.add("merchant is required for item " + item.itemId());
        } else if (item.merchant().trim().length() < 2) {
            warnings.add("merchant name is unusually short for item " + item.itemId());
        }
        if (item.businessPurpose() == null || item.businessPurpose().isBlank()) {
            violations.add("business purpose is required for item " + item.itemId());
        } else if (item.businessPurpose().trim().length() < 8) {
            warnings.add("business purpose is too brief for item " + item.itemId());
        }
        if (item.personalComponent()) {
            violations.add("personal component is not reimbursable for item " + item.itemId());
        }
        if (item.alcoholIncluded() && item.category() != ExpenseCategory.CLIENT_ENTERTAINMENT) {
            violations.add("alcohol is only allowed for client entertainment");
        }
        if (item.paymentMethod() == PaymentMethod.CORPORATE_CARD
                && item.category() == ExpenseCategory.MEDICAL) {
            violations.add("corporate card cannot be used for medical expenses");
        }
        if (item.receiptType() == ReceiptType.NONE && receiptRequired(item.category(), baseAmount)) {
            violations.add("receipt is required for item " + item.itemId());
        }
        if (baseAmount.compareTo(new BigDecimal("50000")) > 0) {
            violations.add("FATAL: single item exceeds the maximum supported amount");
        }
        DayOfWeek day = item.incurredDate().getDayOfWeek();
        if ((day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                && item.category() == ExpenseCategory.OFFICE_SUPPLIES) {
            warnings.add("weekend office-supply purchase needs explanation");
        }
    }

    private static void validateDuplicate(
            ExpenseItem item,
            Set<String> seenFingerprints,
            List<String> violations) {
        String merchant = item.merchant() == null ? "" : item.merchant().trim().toLowerCase();
        String fingerprint = item.incurredDate() + "|" + merchant + "|" + item.amount() + "|" + item.currency();
        if (!seenFingerprints.add(fingerprint)) {
            violations.add("possible duplicate item: " + item.itemId());
        }
        if (item.suspectedDuplicate()) {
            violations.add("item is marked as a suspected duplicate: " + item.itemId());
        }
    }

    private static BigDecimal allowedAmount(
            ExpenseItem item,
            ExpenseClaim claim,
            BigDecimal amount,
            List<String> violations,
            List<String> warnings) {
        if (item.personalComponent() || item.suspectedDuplicate()) {
            return ZERO;
        }
        BigDecimal cap = categoryCap(item, claim);
        validateCategoryRules(item, claim, amount, violations, warnings);
        if (cap.signum() == 0) {
            return ZERO;
        }
        if (amount.compareTo(cap) > 0) {
            warnings.add("category cap applied to item " + item.itemId());
            return cap;
        }
        return amount;
    }

    private static BigDecimal categoryCap(ExpenseItem item, ExpenseClaim claim) {
        int gradeMultiplier = switch (claim.employeeGrade()) {
            case INTERN -> 1;
            case STAFF -> 2;
            case SENIOR -> 3;
            case MANAGER -> 4;
            case DIRECTOR -> 6;
            case EXECUTIVE -> 10;
        };
        BigDecimal base = switch (item.category()) {
            case AIRFARE -> new BigDecimal("1500");
            case HOTEL -> new BigDecimal("500");
            case MEAL -> new BigDecimal("80");
            case LOCAL_TRANSPORT -> new BigDecimal("200");
            case OFFICE_SUPPLIES -> new BigDecimal("300");
            case CLIENT_ENTERTAINMENT -> new BigDecimal("500");
            case TRAINING -> new BigDecimal("1000");
            case SOFTWARE -> new BigDecimal("800");
            case COMMUNICATION -> new BigDecimal("150");
            case MEDICAL -> new BigDecimal("300");
            case OTHER -> new BigDecimal("100");
        };
        BigDecimal cap = base.multiply(BigDecimal.valueOf(gradeMultiplier));
        if (claim.internationalTrip()
                && (item.category() == ExpenseCategory.HOTEL
                || item.category() == ExpenseCategory.MEAL
                || item.category() == ExpenseCategory.LOCAL_TRANSPORT)) {
            cap = cap.multiply(new BigDecimal("1.5"));
        }
        if (claim.clientBillable() && item.category() == ExpenseCategory.CLIENT_ENTERTAINMENT) {
            cap = cap.multiply(new BigDecimal("1.25"));
        }
        return money(cap);
    }

    private static void validateCategoryRules(
            ExpenseItem item,
            ExpenseClaim claim,
            BigDecimal amount,
            List<String> violations,
            List<String> warnings) {
        switch (item.category()) {
            case AIRFARE -> {
                if (!claim.managerPreApproved() && amount.compareTo(new BigDecimal("1000")) > 0) {
                    violations.add("airfare over 1000 requires pre-approval");
                }
                if (item.paymentMethod() == PaymentMethod.CASH) {
                    warnings.add("cash airfare purchase requires verification");
                }
            }
            case HOTEL -> {
                if (item.receiptType() == ReceiptType.BANK_STATEMENT) {
                    violations.add("hotel requires an invoice or receipt");
                }
                if (!claim.internationalTrip() && amount.compareTo(new BigDecimal("2000")) > 0) {
                    warnings.add("domestic hotel amount is unusually high");
                }
            }
            case MEAL -> {
                if (item.alcoholIncluded()) {
                    violations.add("ordinary meal cannot include alcohol");
                }
                if (item.attendeeCount() > 0) {
                    violations.add("meal attendee count belongs under entertainment");
                }
            }
            case LOCAL_TRANSPORT -> {
                if (item.paymentMethod() == PaymentMethod.CASH && item.receiptType() == ReceiptType.NONE) {
                    warnings.add("cash transport without receipt will be capped");
                }
                if (amount.compareTo(new BigDecimal("1000")) > 0) {
                    warnings.add("local transport amount needs route details");
                }
            }
            case OFFICE_SUPPLIES -> {
                if (claim.employeeGrade() == EmployeeGrade.INTERN && amount.compareTo(new BigDecimal("100")) > 0) {
                    violations.add("intern office-supply purchase exceeds delegated limit");
                }
                if (item.paymentMethod() == PaymentMethod.PERSONAL_CARD && amount.compareTo(new BigDecimal("500")) > 0) {
                    warnings.add("large office purchase should use corporate procurement");
                }
            }
            case CLIENT_ENTERTAINMENT -> {
                if (item.attendeeCount() == 0) {
                    violations.add("client entertainment requires attendee count");
                }
                if (!claim.managerPreApproved()) {
                    warnings.add("client entertainment lacks pre-approval");
                }
                if (item.alcoholIncluded() && item.attendeeCount() < 2) {
                    violations.add("alcohol expense requires at least two attendees");
                }
            }
            case TRAINING -> {
                if (!claim.managerPreApproved()) {
                    violations.add("training requires pre-approval");
                }
                if (claim.employeeGrade() == EmployeeGrade.INTERN) {
                    warnings.add("intern training requires department confirmation");
                }
            }
            case SOFTWARE -> {
                if (item.paymentMethod() == PaymentMethod.CASH) {
                    violations.add("software cannot be purchased with cash");
                }
                if (amount.compareTo(new BigDecimal("3000")) > 0) {
                    warnings.add("software purchase should use procurement review");
                }
            }
            case COMMUNICATION -> {
                if (amount.compareTo(new BigDecimal("500")) > 0) {
                    warnings.add("communication expense exceeds ordinary monthly level");
                }
            }
            case MEDICAL -> {
                if (item.receiptType() != ReceiptType.TAX_INVOICE
                        && item.receiptType() != ReceiptType.ELECTRONIC_RECEIPT) {
                    violations.add("medical expense requires a recognized receipt");
                }
            }
            case OTHER -> {
                if (!claim.managerPreApproved()) {
                    warnings.add("other-category expense needs manager explanation");
                }
                if (amount.compareTo(new BigDecimal("500")) > 0) {
                    violations.add("other-category expense exceeds allowed limit");
                }
            }
        }
    }

    private static boolean receiptRequired(ExpenseCategory category, BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("50")) <= 0
                && (category == ExpenseCategory.MEAL || category == ExpenseCategory.LOCAL_TRANSPORT)) {
            return false;
        }
        if (category == ExpenseCategory.COMMUNICATION && amount.compareTo(new BigDecimal("20")) <= 0) {
            return false;
        }
        return category != ExpenseCategory.OTHER || amount.compareTo(new BigDecimal("10")) > 0;
    }

    private static boolean containsFatalViolation(List<String> violations) {
        for (String violation : violations) {
            if (violation.startsWith("FATAL:")) {
                return true;
            }
        }
        return false;
    }

    private static ExpenseApprovalTier resolveApprovalTier(
            ExpenseClaim claim,
            BigDecimal total,
            boolean auditRequired,
            boolean rejected) {
        if (rejected) {
            return ExpenseApprovalTier.FINANCE;
        }
        if (total.compareTo(new BigDecimal("50000")) > 0) {
            return ExpenseApprovalTier.CHIEF_FINANCIAL_OFFICER;
        }
        if (total.compareTo(new BigDecimal("20000")) > 0 || claim.employeeGrade() == EmployeeGrade.EXECUTIVE) {
            return ExpenseApprovalTier.FINANCE_DIRECTOR;
        }
        if (auditRequired || total.compareTo(new BigDecimal("5000")) > 0) {
            return ExpenseApprovalTier.FINANCE;
        }
        if (total.compareTo(new BigDecimal("2000")) > 0 || claim.clientBillable()) {
            return ExpenseApprovalTier.DEPARTMENT_HEAD;
        }
        if (total.compareTo(new BigDecimal("200")) > 0 || !claim.managerPreApproved()) {
            return ExpenseApprovalTier.LINE_MANAGER;
        }
        return ExpenseApprovalTier.AUTO_APPROVED;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
