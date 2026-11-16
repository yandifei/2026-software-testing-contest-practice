package net.mooctest.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable sequential workflow engine with configurable voting per step. */
public final class ApprovalWorkflowEngine {

    public WorkflowSnapshot start(
            String workflowId,
            String requesterId,
            List<ApprovalStep> configuredSteps,
            Instant now,
            boolean urgent,
            boolean withdrawalAllowed) {
        Objects.requireNonNull(configuredSteps, "configuredSteps must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (configuredSteps.isEmpty()) {
            throw new IllegalArgumentException("workflow requires at least one step");
        }

        String requester = requireActor(requesterId, "requesterId");
        Set<String> ids = new HashSet<>();
        List<StepProgress> steps = new ArrayList<>();
        for (ApprovalStep step : configuredSteps) {
            if (!ids.add(step.stepId())) {
                throw new IllegalArgumentException("stepId must be unique: " + step.stepId());
            }
            if (step.deadline() != null && !step.deadline().isAfter(now)) {
                throw new IllegalArgumentException("step deadline must be after workflow creation");
            }
            steps.add(StepProgress.empty(step));
        }

        int first = nextApplicableStep(steps, 0, requester);
        if (first >= steps.size()) {
            return new WorkflowSnapshot(
                    workflowId, requester, WorkflowState.APPROVED, steps, steps.size(),
                    now, now, 1, urgent, withdrawalAllowed);
        }
        return new WorkflowSnapshot(
                workflowId, requester, WorkflowState.PENDING, steps, first,
                now, now, 1, urgent, withdrawalAllowed);
    }

    public WorkflowSnapshot act(WorkflowSnapshot snapshot, WorkflowAction action) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(action, "action must not be null");
        ensureActive(snapshot);
        if (action.actionTime().isBefore(snapshot.updatedAt())) {
            throw new IllegalArgumentException("action time cannot precede the previous update");
        }

        StepProgress current = snapshot.currentStep();
        ApprovalStep step = current.step();
        if (step.deadline() != null && action.actionTime().isAfter(step.deadline())) {
            return withState(snapshot, WorkflowState.EXPIRED, snapshot.currentStepIndex(), action.actionTime());
        }
        if (!step.canApprove(action.actorId())) {
            throw new IllegalArgumentException("actor is not authorized for the current step");
        }
        if (step.skipRequester() && snapshot.requesterId().equals(action.actorId())) {
            throw new IllegalArgumentException("requester cannot approve this step");
        }

        StepProgress decided = current.decide(action.actorId(), action.decision());
        List<StepProgress> updatedSteps = replace(snapshot.steps(), snapshot.currentStepIndex(), decided);

        if (action.decision() == ApprovalDecision.REJECT) {
            return update(snapshot, WorkflowState.REJECTED, updatedSteps,
                    snapshot.currentStepIndex(), action.actionTime());
        }
        if (action.decision() == ApprovalDecision.RETURN) {
            return update(snapshot, WorkflowState.RETURNED, updatedSteps,
                    snapshot.currentStepIndex(), action.actionTime());
        }
        if (!decided.canStillSucceed()) {
            return update(snapshot, WorkflowState.REJECTED, updatedSteps,
                    snapshot.currentStepIndex(), action.actionTime());
        }
        if (!decided.satisfied()) {
            return update(snapshot, WorkflowState.IN_PROGRESS, updatedSteps,
                    snapshot.currentStepIndex(), action.actionTime());
        }

        int next = nextApplicableStep(updatedSteps, snapshot.currentStepIndex() + 1, snapshot.requesterId());
        if (next >= updatedSteps.size()) {
            return update(snapshot, WorkflowState.APPROVED, updatedSteps,
                    updatedSteps.size(), action.actionTime());
        }
        return update(snapshot, WorkflowState.IN_PROGRESS, updatedSteps, next, action.actionTime());
    }

    public WorkflowSnapshot delegate(
            WorkflowSnapshot snapshot,
            String fromActor,
            String toActor,
            Instant now) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(now, "now must not be null");
        ensureActive(snapshot);
        if (now.isBefore(snapshot.updatedAt())) {
            throw new IllegalArgumentException("delegation time cannot precede the previous update");
        }
        String from = requireActor(fromActor, "fromActor");
        String to = requireActor(toActor, "toActor");
        if (snapshot.requesterId().equals(to) && snapshot.currentStep().step().skipRequester()) {
            throw new IllegalArgumentException("cannot delegate approval to the requester");
        }
        StepProgress current = snapshot.currentStep();
        ApprovalStep delegatedStep = current.step().delegate(from, to);
        StepProgress delegatedProgress = current.withDelegatedStep(delegatedStep, from);
        List<StepProgress> updated = replace(snapshot.steps(), snapshot.currentStepIndex(), delegatedProgress);
        return update(snapshot, WorkflowState.IN_PROGRESS, updated,
                snapshot.currentStepIndex(), now);
    }

    public WorkflowSnapshot withdraw(WorkflowSnapshot snapshot, String actorId, Instant now) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(now, "now must not be null");
        ensureActive(snapshot);
        if (!snapshot.withdrawalAllowed()) {
            throw new IllegalStateException("withdrawal is disabled for this workflow");
        }
        if (!snapshot.requesterId().equals(requireActor(actorId, "actorId"))) {
            throw new IllegalArgumentException("only the requester may withdraw the workflow");
        }
        if (snapshot.currentStepIndex() > 0 && snapshot.urgent()) {
            throw new IllegalStateException("urgent workflow cannot be withdrawn after first approval");
        }
        if (now.isBefore(snapshot.updatedAt())) {
            throw new IllegalArgumentException("withdrawal time cannot precede the previous update");
        }
        return withState(snapshot, WorkflowState.WITHDRAWN, snapshot.currentStepIndex(), now);
    }

    public WorkflowSnapshot cancel(
            WorkflowSnapshot snapshot,
            boolean actorIsAdministrator,
            String reason,
            Instant now) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (snapshot.terminal()) {
            throw new IllegalStateException("terminal workflow cannot be cancelled");
        }
        if (!actorIsAdministrator) {
            throw new IllegalArgumentException("workflow cancellation requires administrator role");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException("cancellation reason must contain at least five characters");
        }
        if (now.isBefore(snapshot.updatedAt())) {
            throw new IllegalArgumentException("cancellation time cannot precede the previous update");
        }
        return withState(snapshot, WorkflowState.CANCELLED, snapshot.currentStepIndex(), now);
    }

    public WorkflowSnapshot expireIfNecessary(WorkflowSnapshot snapshot, Instant now) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (snapshot.terminal()) {
            return snapshot;
        }
        Instant deadline = snapshot.currentStep().step().deadline();
        if (deadline == null || !now.isAfter(deadline)) {
            return snapshot;
        }
        return withState(snapshot, WorkflowState.EXPIRED, snapshot.currentStepIndex(), now);
    }

    private static void ensureActive(WorkflowSnapshot snapshot) {
        if (snapshot.terminal()) {
            throw new IllegalStateException("terminal workflow cannot accept actions");
        }
        if (snapshot.state() != WorkflowState.PENDING
                && snapshot.state() != WorkflowState.IN_PROGRESS) {
            throw new IllegalStateException("workflow is not active");
        }
    }

    private static int nextApplicableStep(
            List<StepProgress> steps,
            int startIndex,
            String requesterId) {
        int index = startIndex;
        while (index < steps.size()) {
            ApprovalStep step = steps.get(index).step();
            if (!(step.skipRequester()
                    && step.approverIds().size() == 1
                    && step.approverIds().contains(requesterId))) {
                return index;
            }
            index++;
        }
        return index;
    }

    private static List<StepProgress> replace(
            List<StepProgress> source,
            int index,
            StepProgress replacement) {
        List<StepProgress> result = new ArrayList<>(source);
        result.set(index, replacement);
        return List.copyOf(result);
    }

    private static WorkflowSnapshot withState(
            WorkflowSnapshot snapshot,
            WorkflowState state,
            int currentIndex,
            Instant now) {
        return update(snapshot, state, snapshot.steps(), currentIndex, now);
    }

    private static WorkflowSnapshot update(
            WorkflowSnapshot snapshot,
            WorkflowState state,
            List<StepProgress> steps,
            int currentIndex,
            Instant now) {
        return new WorkflowSnapshot(
                snapshot.workflowId(), snapshot.requesterId(), state, steps, currentIndex,
                snapshot.createdAt(), now, snapshot.version() + 1,
                snapshot.urgent(), snapshot.withdrawalAllowed());
    }

    private static String requireActor(String actor, String field) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return actor.trim();
    }
}
