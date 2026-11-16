package net.mooctest.grading.expression;

import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.CalculationResults;

import java.math.BigDecimal;
import java.util.Objects;

public class MathematicalExpressionEvaluator implements ExpressionEvaluator {

    private static final String DEFAULT_CALCULATOR_NAME = "math-assessment";

    private final BracketsCalculation2 calculator;

    public MathematicalExpressionEvaluator() {
        this(BracketsCalculation2.getInstance(DEFAULT_CALCULATOR_NAME));
    }

    public MathematicalExpressionEvaluator(BracketsCalculation2 calculator) {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public BigDecimal evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ExpressionEvaluationException("expression must not be blank");
        }
        try {
            CalculationResults result = calculator
                    .compileBigDecimal(expression, true)
                    .calculationBigDecimals(false);
            BigDecimal value = result.getBigDecimalResult();
            if (value == null) {
                throw new ExpressionEvaluationException("expression produced no numeric result");
            }
            return value;
        } catch (ExpressionEvaluationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExpressionEvaluationException(
                    "unable to evaluate expression: " + expression,
                    exception
            );
        }
    }
}
