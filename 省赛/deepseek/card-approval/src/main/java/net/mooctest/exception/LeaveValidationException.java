package net.mooctest.exception;

/** Raised when a leave request violates a business rule. */
public class LeaveValidationException extends IllegalArgumentException {

    public LeaveValidationException(String message) {
        super(message);
    }
}
