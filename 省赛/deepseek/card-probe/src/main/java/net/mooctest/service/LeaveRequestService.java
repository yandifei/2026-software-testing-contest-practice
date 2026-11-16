package net.mooctest.service;

import net.mooctest.domain.LeaveRequest;
import net.mooctest.domain.LeaveType;
import net.mooctest.exception.LeaveRequestNotFoundException;
import net.mooctest.exception.LeaveValidationException;
import net.mooctest.port.LeaveBalanceRepository;
import net.mooctest.port.LeaveRequestRepository;
import net.mooctest.port.NotificationGateway;
import net.mooctest.port.RequestIdGenerator;

import java.time.Clock;
import java.util.Objects;

/** Application service designed for interaction-based unit tests. */
public final class LeaveRequestService {

    private final LeaveRequestRepository requestRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final NotificationGateway notificationGateway;
    private final RequestIdGenerator idGenerator;
    private final LeavePolicy policy;
    private final Clock clock;

    public LeaveRequestService(
            LeaveRequestRepository requestRepository,
            LeaveBalanceRepository balanceRepository,
            NotificationGateway notificationGateway,
            RequestIdGenerator idGenerator,
            LeavePolicy policy,
            Clock clock) {
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.balanceRepository = Objects.requireNonNull(balanceRepository);
        this.notificationGateway = Objects.requireNonNull(notificationGateway);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.policy = Objects.requireNonNull(policy);
        this.clock = Objects.requireNonNull(clock);
    }

    public LeaveRequest submit(SubmitLeaveCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        int remainingDays = command.type() == LeaveType.ANNUAL
                ? balanceRepository.remainingAnnualDays(command.employeeId())
                : Integer.MAX_VALUE;
        policy.validate(command.type(), command.period(), command.reason(), remainingDays);

        if (requestRepository.existsActiveOverlap(command.employeeId(), command.period())) {
            throw new LeaveValidationException("leave period overlaps an active request");
        }

        LeaveRequest request = LeaveRequest.pending(
                idGenerator.nextId(),
                command.employeeId(),
                command.type(),
                command.period(),
                command.reason(),
                clock.instant());
        LeaveRequest saved = requestRepository.save(request);
        notificationGateway.requestSubmitted(saved);
        return saved;
    }

    public LeaveRequest approve(String requestId, String approverId) {
        LeaveRequest current = findRequest(requestId);
        String approver = requireActor(approverId);
        if (current.employeeId().equals(approver)) {
            throw new LeaveValidationException("employee cannot approve their own request");
        }

        LeaveRequest approved = current.approve(approver, clock.instant());
        if (approved.type() == LeaveType.ANNUAL) {
            balanceRepository.deductAnnualDays(
                    approved.employeeId(), approved.period().businessDays());
        }
        LeaveRequest saved = requestRepository.save(approved);
        notificationGateway.requestApproved(saved);
        return saved;
    }

    public LeaveRequest reject(String requestId, String approverId, String comment) {
        LeaveRequest current = findRequest(requestId);
        String approver = requireActor(approverId);
        if (current.employeeId().equals(approver)) {
            throw new LeaveValidationException("employee cannot reject their own request");
        }

        LeaveRequest rejected = current.reject(approver, clock.instant(), comment);
        LeaveRequest saved = requestRepository.save(rejected);
        notificationGateway.requestRejected(saved);
        return saved;
    }

    private LeaveRequest findRequest(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        return requestRepository.findById(requestId.trim())
                .orElseThrow(() -> new LeaveRequestNotFoundException(requestId.trim()));
    }

    private static String requireActor(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("approverId must not be blank");
        }
        return actorId.trim();
    }
}
