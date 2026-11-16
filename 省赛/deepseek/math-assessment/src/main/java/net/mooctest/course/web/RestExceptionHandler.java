package net.mooctest.course.web;

import net.mooctest.common.ApiResponse;
import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.code()) {
            case COURSE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_COURSE_TITLE, COURSE_ALREADY_PUBLISHED -> HttpStatus.CONFLICT;
            case COURSE_HAS_NO_CHAPTERS -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INVALID_COURSE_TITLE, INVALID_PRICE, INVALID_DISCOUNT -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = firstError == null
                ? "Request validation failed"
                : firstError.getField() + ": " + firstError.getDefaultMessage();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure("VALIDATION_ERROR", message));
    }
}
