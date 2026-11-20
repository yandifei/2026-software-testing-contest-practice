package net.mooctest.grading.expression;

import java.math.BigDecimal;

@FunctionalInterface
public interface ExpressionEvaluator {

    BigDecimal evaluate(String expression);
}
