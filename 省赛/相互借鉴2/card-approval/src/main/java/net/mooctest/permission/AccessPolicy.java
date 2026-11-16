package net.mooctest.permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deny-overrides access policy with role, scope and contextual rules. */
public final class AccessPolicy {

    public AccessDecision decide(AccessContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<String> denials = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        boolean audit = false;
        boolean stepUp = false;

        if (!context.accountActive()) {
            denials.add("account is inactive");
        }
        if (context.accountSuspended()) {
            denials.add("account is suspended");
        }
        if (context.role() == UserRole.GUEST
                && context.resourceType() != ResourceType.PUBLIC_DOCUMENT) {
            denials.add("guest may only access public documents");
        }
        if (context.sensitivity() == Sensitivity.RESTRICTED
                && !context.multiFactorAuthenticated()) {
            stepUp = true;
            denials.add("restricted resource requires multi-factor authentication");
        }
        if (context.sensitivity() == Sensitivity.RESTRICTED
                && !context.trustedDevice()) {
            denials.add("restricted resource requires a trusted device");
        }
        if ((context.sensitivity() == Sensitivity.CONFIDENTIAL
                || context.sensitivity() == Sensitivity.RESTRICTED)
                && !context.securityTrainingCompleted()) {
            denials.add("security training is required");
        }
        if (!context.dataRegionMatches() && context.action() == ResourceAction.EXPORT) {
            denials.add("cross-region export is not allowed");
        }
        if (!context.trustedNetwork()
                && (context.action() == ResourceAction.EXPORT
                || context.action() == ResourceAction.SHARE)) {
            denials.add("export or sharing requires a trusted network");
        }
        if (!context.businessHours()
                && context.sensitivity() == Sensitivity.RESTRICTED
                && context.role() != UserRole.SYSTEM_ADMINISTRATOR) {
            denials.add("restricted resource is unavailable outside business hours");
        }
        if (context.delegated() && !context.delegationValid()) {
            denials.add("delegation has expired or is invalid");
        }
        if (context.delegated() && context.action() == ResourceAction.DELETE) {
            denials.add("delegated permission cannot delete resources");
        }
        if (context.delegated() && context.action() == ResourceAction.EXPORT) {
            denials.add("delegated permission cannot export resources");
        }
        if (context.legalHold()
                && (context.action() == ResourceAction.DELETE
                || context.action() == ResourceAction.ARCHIVE)) {
            denials.add("resource is under legal hold");
        }
        if (context.archived() && context.action() == ResourceAction.UPDATE) {
            denials.add("archived resource cannot be updated");
        }
        if (!context.archived() && context.action() == ResourceAction.RESTORE) {
            denials.add("only archived resources can be restored");
        }

        boolean roleAllowed = roleAllows(context, notes);
        if (!roleAllowed) {
            denials.add("role does not grant the requested action");
        }
        boolean scopeAllowed = scopeAllows(context, notes);
        if (!scopeAllowed) {
            denials.add("requested scope is outside the actor's authority");
        }
        validateResourceSpecificRules(context, denials, notes);

        if (context.action() == ResourceAction.EXPORT
                || context.action() == ResourceAction.DELETE
                || context.action() == ResourceAction.APPROVE) {
            audit = true;
        }
        if (context.sensitivity() == Sensitivity.RESTRICTED) {
            audit = true;
        }
        if (context.delegated()) {
            audit = true;
        }
        if (!context.businessHours()) {
            audit = true;
        }
        if (context.role() == UserRole.SYSTEM_ADMINISTRATOR) {
            audit = true;
        }
        if (context.resourceType() == ResourceType.AUDIT_LOG) {
            audit = true;
        }

        boolean allowed = denials.isEmpty();
        List<String> reasons = allowed ? notes : denials;
        if (allowed && reasons.isEmpty()) {
            reasons = List.of("access granted by role and scope policy");
        }
        return new AccessDecision(allowed, audit, !allowed && stepUp, reasons);
    }

    private static boolean roleAllows(AccessContext context, List<String> notes) {
        return switch (context.role()) {
            case EMPLOYEE -> employeeAllows(context, notes);
            case TEAM_LEAD -> teamLeadAllows(context, notes);
            case DEPARTMENT_MANAGER -> departmentManagerAllows(context, notes);
            case HUMAN_RESOURCES -> humanResourcesAllows(context, notes);
            case FINANCE -> financeAllows(context, notes);
            case AUDITOR -> auditorAllows(context, notes);
            case SYSTEM_ADMINISTRATOR -> administratorAllows(context, notes);
            case GUEST -> guestAllows(context, notes);
        };
    }

    private static boolean employeeAllows(AccessContext context, List<String> notes) {
        if (context.action() == ResourceAction.CREATE
                && (context.resourceType() == ResourceType.EXPENSE_CLAIM
                || context.resourceType() == ResourceType.LEAVE_REQUEST)) {
            notes.add("employee may create personal requests");
            return true;
        }
        if (context.ownsResource()
                && (context.action() == ResourceAction.VIEW
                || context.action() == ResourceAction.UPDATE)) {
            notes.add("employee may access owned resource");
            return context.resourceType() != ResourceType.PAYROLL || context.action() == ResourceAction.VIEW;
        }
        if (context.resourceType() == ResourceType.PUBLIC_DOCUMENT
                && context.action() == ResourceAction.VIEW) {
            return true;
        }
        return false;
    }

    private static boolean teamLeadAllows(AccessContext context, List<String> notes) {
        if (employeeAllows(context, notes)) {
            return true;
        }
        if (context.requestedScope() == AccessScope.TEAM
                && (context.action() == ResourceAction.VIEW
                || context.action() == ResourceAction.APPROVE)) {
            if (context.resourceType() == ResourceType.LEAVE_REQUEST
                    || context.resourceType() == ResourceType.ATTENDANCE_RECORD
                    || context.resourceType() == ResourceType.EXPENSE_CLAIM) {
                notes.add("team lead has operational team access");
                return true;
            }
        }
        return false;
    }

    private static boolean departmentManagerAllows(AccessContext context, List<String> notes) {
        if (teamLeadAllows(context, notes)) {
            return true;
        }
        if (!context.sameDepartment()) {
            return false;
        }
        if (context.requestedScope() != AccessScope.DEPARTMENT
                && context.requestedScope() != AccessScope.TEAM) {
            return false;
        }
        if (context.resourceType() == ResourceType.PAYROLL
                || context.resourceType() == ResourceType.SYSTEM_CONFIGURATION
                || context.resourceType() == ResourceType.AUDIT_LOG) {
            return false;
        }
        if (context.action() == ResourceAction.DELETE
                && context.sensitivity() != Sensitivity.PUBLIC) {
            return false;
        }
        notes.add("department manager has department-scoped access");
        return true;
    }

    private static boolean humanResourcesAllows(AccessContext context, List<String> notes) {
        if (context.resourceType() == ResourceType.EMPLOYEE_PROFILE
                || context.resourceType() == ResourceType.LEAVE_REQUEST
                || context.resourceType() == ResourceType.ATTENDANCE_RECORD) {
            if (context.action() == ResourceAction.DELETE) {
                return false;
            }
            if (context.action() == ResourceAction.EXPORT
                    && context.requestedScope() == AccessScope.ORGANIZATION
                    && !context.projectMember()) {
                return false;
            }
            notes.add("human resources role grants workforce-data access");
            return true;
        }
        if (context.resourceType() == ResourceType.PAYROLL
                && context.action() == ResourceAction.VIEW
                && context.requestedScope() != AccessScope.ORGANIZATION) {
            notes.add("human resources may view scoped payroll data");
            return true;
        }
        return context.resourceType() == ResourceType.PUBLIC_DOCUMENT
                && context.action() == ResourceAction.VIEW;
    }

    private static boolean financeAllows(AccessContext context, List<String> notes) {
        if (context.resourceType() == ResourceType.EXPENSE_CLAIM
                || context.resourceType() == ResourceType.PAYROLL
                || context.resourceType() == ResourceType.CONTRACT) {
            if (context.action() == ResourceAction.DELETE) {
                return false;
            }
            if (context.action() == ResourceAction.SHARE
                    && context.sensitivity() == Sensitivity.RESTRICTED) {
                return false;
            }
            notes.add("finance role grants financial-data access");
            return true;
        }
        return context.resourceType() == ResourceType.PUBLIC_DOCUMENT
                && context.action() == ResourceAction.VIEW;
    }

    private static boolean auditorAllows(AccessContext context, List<String> notes) {
        if (context.action() == ResourceAction.VIEW || context.action() == ResourceAction.EXPORT) {
            if (context.resourceType() != ResourceType.SYSTEM_CONFIGURATION) {
                notes.add("auditor receives read-only access");
                return true;
            }
        }
        return false;
    }

    private static boolean administratorAllows(AccessContext context, List<String> notes) {
        if (context.resourceType() == ResourceType.PAYROLL
                || context.resourceType() == ResourceType.EMPLOYEE_PROFILE) {
            if (context.action() == ResourceAction.VIEW
                    || context.action() == ResourceAction.EXPORT
                    || context.action() == ResourceAction.APPROVE) {
                return false;
            }
        }
        if (context.legalHold() && context.action() == ResourceAction.DELETE) {
            return false;
        }
        notes.add("system administrator has technical administration access");
        return true;
    }

    private static boolean guestAllows(AccessContext context, List<String> notes) {
        if (context.resourceType() != ResourceType.PUBLIC_DOCUMENT) {
            return false;
        }
        if (context.action() != ResourceAction.VIEW) {
            return false;
        }
        if (context.sensitivity() != Sensitivity.PUBLIC) {
            return false;
        }
        notes.add("guest may view public documents");
        return true;
    }

    private static boolean scopeAllows(AccessContext context, List<String> notes) {
        if (context.requestedScope() == AccessScope.OWN) {
            return context.ownsResource()
                    || context.action() == ResourceAction.CREATE
                    || context.role() == UserRole.SYSTEM_ADMINISTRATOR;
        }
        if (context.requestedScope() == AccessScope.TEAM) {
            return context.role() == UserRole.TEAM_LEAD
                    || context.role() == UserRole.DEPARTMENT_MANAGER
                    || context.role() == UserRole.HUMAN_RESOURCES
                    || context.role() == UserRole.FINANCE
                    || context.role() == UserRole.AUDITOR
                    || context.role() == UserRole.SYSTEM_ADMINISTRATOR;
        }
        if (context.requestedScope() == AccessScope.DEPARTMENT) {
            if (!context.sameDepartment()
                    && context.role() != UserRole.HUMAN_RESOURCES
                    && context.role() != UserRole.FINANCE
                    && context.role() != UserRole.AUDITOR
                    && context.role() != UserRole.SYSTEM_ADMINISTRATOR) {
                return false;
            }
            return context.role() == UserRole.DEPARTMENT_MANAGER
                    || context.role() == UserRole.HUMAN_RESOURCES
                    || context.role() == UserRole.FINANCE
                    || context.role() == UserRole.AUDITOR
                    || context.role() == UserRole.SYSTEM_ADMINISTRATOR;
        }
        boolean organizationRole = context.role() == UserRole.HUMAN_RESOURCES
                || context.role() == UserRole.FINANCE
                || context.role() == UserRole.AUDITOR
                || context.role() == UserRole.SYSTEM_ADMINISTRATOR;
        if (organizationRole) {
            notes.add("organization scope granted to specialist role");
        }
        return organizationRole;
    }

    private static void validateResourceSpecificRules(
            AccessContext context,
            List<String> denials,
            List<String> notes) {
        if (context.resourceType() == ResourceType.AUDIT_LOG
                && context.action() != ResourceAction.VIEW
                && context.action() != ResourceAction.EXPORT) {
            denials.add("audit logs are immutable");
        }
        if (context.resourceType() == ResourceType.SYSTEM_CONFIGURATION
                && context.role() != UserRole.SYSTEM_ADMINISTRATOR) {
            denials.add("system configuration requires administrator role");
        }
        if (context.resourceType() == ResourceType.PAYROLL
                && context.sensitivity() == Sensitivity.PUBLIC) {
            denials.add("payroll cannot be classified as public");
        }
        if (context.resourceType() == ResourceType.EMPLOYEE_PROFILE
                && context.action() == ResourceAction.SHARE
                && !context.projectMember()) {
            denials.add("profile sharing requires project membership");
        }
        if (context.resourceType() == ResourceType.CONTRACT
                && context.action() == ResourceAction.APPROVE
                && context.role() != UserRole.FINANCE
                && context.role() != UserRole.DEPARTMENT_MANAGER) {
            denials.add("contract approval requires finance or department manager");
        }
        if (context.resourceType() == ResourceType.PUBLIC_DOCUMENT
                && context.sensitivity() == Sensitivity.PUBLIC) {
            notes.add("resource is publicly classified");
        }
        if (context.ownsResource()) {
            notes.add("actor owns the resource");
        }
        if (context.sameDepartment()) {
            notes.add("actor and resource belong to the same department");
        }
    }
}
