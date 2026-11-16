package net.mooctest.permission;

import java.util.List;
import java.util.Objects;

public record AccessDecision(
        boolean allowed,
        boolean auditRequired,
        boolean stepUpAuthenticationRequired,
        List<String> reasons) {

    public AccessDecision {
        reasons = List.copyOf(Objects.requireNonNull(reasons));
        if (!allowed && reasons.isEmpty()) {
            throw new IllegalArgumentException("denied access must contain a reason");
        }
        if (allowed && stepUpAuthenticationRequired) {
            throw new IllegalArgumentException("step-up authentication must be completed before access is allowed");
        }
    }
}
