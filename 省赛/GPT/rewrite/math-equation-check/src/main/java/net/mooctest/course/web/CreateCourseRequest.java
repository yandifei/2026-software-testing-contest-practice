package net.mooctest.course.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCourseRequest(
        @NotBlank(message = "title must not be blank")
        @Size(min = 3, max = 80, message = "title length must be between 3 and 80")
        String title,

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0.00", message = "price must not be negative")
        @DecimalMax(value = "99999.99", message = "price must not exceed 99999.99")
        @Digits(integer = 5, fraction = 2, message = "price can have at most two decimal places")
        BigDecimal price
) {
}
