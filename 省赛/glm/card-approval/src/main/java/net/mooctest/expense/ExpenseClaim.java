package net.mooctest.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ExpenseClaim(
        String claimId,
        String employeeId,
        EmployeeGrade employeeGrade,
        LocalDate submittedDate,
        String costCenter,
        List<ExpenseItem> items,
        boolean managerPreApproved,
        boolean internationalTrip,
        boolean clientBillable,
        boolean projectActive,
        int previousClaimsThisMonth) {

    public ExpenseClaim {
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("claimId must not be blank");
        }
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        claimId = claimId.trim();
        employeeId = employeeId.trim();
        employeeGrade = Objects.requireNonNull(employeeGrade, "employeeGrade must not be null");
        submittedDate = Objects.requireNonNull(submittedDate, "submittedDate must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("claim must contain at least one item");
        }
        if (items.size() > 100) {
            throw new IllegalArgumentException("claim cannot contain more than 100 items");
        }
        if (previousClaimsThisMonth < 0) {
            throw new IllegalArgumentException("previousClaimsThisMonth must not be negative");
        }
        if (clientBillable && (costCenter == null || costCenter.isBlank())) {
            throw new IllegalArgumentException("client-billable claim requires a cost center");
        }
    }
}
