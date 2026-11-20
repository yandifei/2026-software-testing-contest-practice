package net.mooctest.expression.engine;

import java.math.BigDecimal;

/** Evaluates decimal arithmetic with addition, subtraction, multiplication and parentheses. */
public final class ArithmeticExpression {
    private final String input;
    private int position;

    private ArithmeticExpression(String input) {
        this.input = input;
    }

    public static boolean supports(String expression) {
        return expression != null && expression.matches("[0-9.()+*\\-\\s]+");
    }

    public static BigDecimal evaluate(String expression) {
        if (!supports(expression)) {
            throw new IllegalArgumentException("unsupported arithmetic expression");
        }
        ArithmeticExpression parser = new ArithmeticExpression(expression);
        BigDecimal result = parser.sum();
        parser.skipSpaces();
        if (parser.position != expression.length()) {
            throw new IllegalArgumentException("unexpected token in expression");
        }
        return result;
    }

    private BigDecimal sum() {
        BigDecimal value = product();
        while (true) {
            skipSpaces();
            if (take('+')) {
                value = value.add(product());
            } else if (take('-')) {
                value = value.subtract(product());
            } else {
                return value;
            }
        }
    }

    private BigDecimal product() {
        BigDecimal value = unary();
        while (true) {
            skipSpaces();
            if (take('*')) {
                value = value.multiply(unary());
            } else {
                return value;
            }
        }
    }

    private BigDecimal unary() {
        skipSpaces();
        if (take('+')) {
            return unary();
        }
        if (take('-')) {
            return unary().negate();
        }
        return atom();
    }

    private BigDecimal atom() {
        skipSpaces();
        if (take('(')) {
            BigDecimal value = sum();
            skipSpaces();
            if (!take(')')) {
                throw new IllegalArgumentException("missing closing parenthesis");
            }
            return value;
        }
        int start = position;
        while (position < input.length()) {
            char current = input.charAt(position);
            if (Character.isDigit(current) || current == '.') {
                position++;
            } else {
                break;
            }
        }
        if (start == position) {
            throw new IllegalArgumentException("expected a number");
        }
        return new BigDecimal(input.substring(start, position));
    }

    private boolean take(char expected) {
        if (position < input.length() && input.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
    }
}
