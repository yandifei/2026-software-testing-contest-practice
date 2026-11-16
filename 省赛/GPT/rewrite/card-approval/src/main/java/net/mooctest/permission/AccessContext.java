package net.mooctest.permission;

import java.util.Objects;

public record AccessContext(
        String actorId,
        String ownerId,
        String actorDepartment,
        String resourceDepartment,
        UserRole role,
        ResourceAction action,
        ResourceType resourceType,
        Sensitivity sensitivity,
        AccessScope requestedScope,
        boolean accountActive,
        boolean accountSuspended,
        boolean multiFactorAuthenticated,
        boolean trustedDevice,
        boolean trustedNetwork,
        boolean businessHours,
        boolean delegated,
        boolean delegationValid,
        boolean legalHold,
        boolean archived,
        boolean projectMember,
        boolean dataRegionMatches,
        boolean securityTrainingCompleted) {

    public AccessContext {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        actorId = actorId.trim();
        if (ownerId != null) {
            ownerId = ownerId.trim();
        }
        if (actorDepartment != null) {
            actorDepartment = actorDepartment.trim();
        }
        if (resourceDepartment != null) {
            resourceDepartment = resourceDepartment.trim();
        }
        role = Objects.requireNonNull(role, "role must not be null");
        action = Objects.requireNonNull(action, "action must not be null");
        resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");
        sensitivity = Objects.requireNonNull(sensitivity, "sensitivity must not be null");
        requestedScope = Objects.requireNonNull(requestedScope, "requestedScope must not be null");
        if (accountSuspended && accountActive) {
            throw new IllegalArgumentException("suspended account cannot be active");
        }
        if (delegationValid && !delegated) {
            throw new IllegalArgumentException("valid delegation requires delegated access");
        }
    }

    public boolean ownsResource() {
        return ownerId != null && actorId.equals(ownerId);
    }

    public boolean sameDepartment() {
        return actorDepartment != null
                && resourceDepartment != null
                && actorDepartment.equals(resourceDepartment);
    }
}
