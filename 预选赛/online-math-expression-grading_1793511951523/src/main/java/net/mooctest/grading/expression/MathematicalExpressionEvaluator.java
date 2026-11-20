package net.mooctest.grading.expression;

import net.mooctest.expression.engine.BracketsCalculation2;
import net.mooctest.expression.engine.CalculationResults;
import net.mooctest.expression.engine.ArithmeticExpression;

import java.math.BigDecimal;
import java.util.Objects;

public class MathematicalExpressionEvaluator implements ExpressionEvaluator {

    private static final String DEFAULT_CALCULATOR_NAME = "online-math-expression-grading";

    private final BracketsCalculation2 calculator;
    private final boolean useArithmeticPath;

    public MathematicalExpressionEvaluator() {
        this(BracketsCalculation2.getInstance(DEFAULT_CALCULATOR_NAME), true);
    }

    public MathematicalExpressionEvaluator(BracketsCalculation2 calculator) {
        this(calculator, false);
    }

    private MathematicalExpressionEvaluator(BracketsCalculation2 calculator, boolean useArithmeticPath) {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.useArithmeticPath = useArithmeticPath;
    }

    @Override
    public BigDecimal evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ExpressionEvaluationException("expression must not be blank");
        }
        try {
            BigDecimal value;
            if (useArithmeticPath && ArithmeticExpression.supports(expression)) {
                value = ArithmeticExpression.evaluate(expression);
            } else {
                CalculationResults result = calculator
                        .compileBigDecimal(expression, true)
                        .calculationBigDecimals(false);
                value = result.getBigDecimalResult();
            }
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
