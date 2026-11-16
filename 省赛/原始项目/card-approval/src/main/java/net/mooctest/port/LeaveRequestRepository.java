package net.mooctest.port;

import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveRequest;

import java.util.Optional;

/** Persistence boundary. Implementations are replaced with mocks in unit tests. */
public interface LeaveRequestRepository {

    Optional<LeaveRequest> findById(String requestId);

    boolean existsActiveOverlap(String employeeId, DateRange period);

    LeaveRequest save(LeaveRequest request);
}
