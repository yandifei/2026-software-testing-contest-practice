package net.mooctest.expression.engine;

import java.util.HashMap;
import java.util.Map;

/** Registry for the calculation components included in this examination scope. */
public final class CalculationManagement {
    public static final String PREFIX_EXPRESSION_OPERATION_NAME = "PrefixExpressionOperation";
    public static final String BRACKETS_CALCULATION_2_NAME = "BracketsCalculation2";
    private static final Map<String, Calculation> CALCULATIONS = new HashMap<>();

    private CalculationManagement() {
    }

    public static Calculation getCalculationByName(String name) {
        return CALCULATIONS.get(name);
    }

    public static boolean register(Calculation calculation, boolean checkDuplicate) {
        if (checkDuplicate && CALCULATIONS.containsKey(calculation.getName())) {
            return false;
        }
        CALCULATIONS.put(calculation.getName(), calculation);
        return true;
    }

    public static boolean isRegister(String name) {
        return CALCULATIONS.containsKey(name);
    }

}
