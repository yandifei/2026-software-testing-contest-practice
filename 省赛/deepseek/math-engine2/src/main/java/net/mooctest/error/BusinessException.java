package net.mooctest.error;

import java.util.Objects;

public final class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public ErrorCode code() {
        return code;
    }
}
