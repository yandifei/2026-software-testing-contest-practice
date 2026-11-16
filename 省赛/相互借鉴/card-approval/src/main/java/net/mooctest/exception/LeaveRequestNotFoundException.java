package net.mooctest.exception;

/** Raised when a requested leave request does not exist. */
public class LeaveRequestNotFoundException extends RuntimeException {

    public LeaveRequestNotFoundException(String requestId) {
        super("leave request not found: " + requestId);
    }
}
