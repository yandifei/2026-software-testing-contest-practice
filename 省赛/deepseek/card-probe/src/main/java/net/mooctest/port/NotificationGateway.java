package net.mooctest.port;

import net.mooctest.domain.LeaveRequest;

/** External notification boundary. */
public interface NotificationGateway {

    void requestSubmitted(LeaveRequest request);

    void requestApproved(LeaveRequest request);

    void requestRejected(LeaveRequest request);
}
