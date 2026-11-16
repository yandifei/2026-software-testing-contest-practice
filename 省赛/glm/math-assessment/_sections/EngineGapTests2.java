package net.mooctest;

import net.mooctest.expression.engine.ComplexCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation;
import net.mooctest.expression.engine.FunctionFormulaCalculation2;
import net.mooctest.expression.engine.Mathematical_Expression;
import net.mooctest.expression.engine.ManyToOneNumberFunction;
import net.mooctest.expression.engine.WrongFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Third engine pass: character-class boundaries of the letter-detection
 * conditions (uppercase range, lowercase range, and the punctuation gap
 * between them) inside extraction, check and calculation loops.
 */
class EngineGapTests2 {

    @BeforeEach
    void resetFunctionRegistries() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
        try {
            Field calculations = net.mooctest.expression.engine.CalculationManagement.class
                    .getDeclaredField("STRING_CALCULATION_HASH_MAP");
            calculations.setAccessible(true);
            ((java.util.Map<?, ?>) calculations.get(null)).clear();
            Field functions = net.mooctest.expression.engine.CalculationManagement.class
                    .getDeclaredField("STRING_FUNCTION_HASH_MAP");
            functions.setAccessible(true);
            ((java.util.Map<?, ?>) functions.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot reset component registries", e);
        }
    }

    @AfterEach
    void restoreEngineFlags() {
        Mathematical_Expression.Options.setUseBigDecimal(false);
        Mathematical_Expression.Options.setUseCache(false);
    }

    @Nested
    class CharacterClassGapTests {

        @Test
        void extractionTreatsPunctuationGapAsNonLetter() {
            for (String expression : new String[]{"s_m(1,2)", "sum|(1,2)", "s`m(1,2)", "[x]"}) {
                Stack<Integer> start = new Stack<>();
                Stack<Integer> end = new Stack<>();
                Stack<String> names = new Stack<>();
                FunctionFormulaCalculation2.FunctionParameterExtraction(expression, start, end, names);
                assertEquals(1, names.size(), expression);
            }
        }

        @Test
        void builderExtractionTreatsPunctuationGapAsNonLetter() {
            FunctionFormulaCalculation2 c = FunctionFormulaCalculation2.getInstance("gap2Builder");
            for (String expression : new String[]{"S_M(1,2)", "SUM|(1,2)"}) {
                StringBuilder builder = new StringBuilder();
                Stack<Integer> start = new Stack<>();
                Stack<Integer> end = new Stack<>();
                Stack<String> names = new Stack<>();
                c.FunctionParameterExtraction(expression, start, end, names, builder);
                assertEquals(1, names.size(), expression);
            }
        }

        @Test
        void singleParameterCheckScansUppercaseAndPunctuationNames() {
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gap2Ffc1Check");
            assertThrows(WrongFormat.class, () -> c.check("SIN(1"));
            assertThrows(WrongFormat.class, () -> c.check("s_n(1"));
            assertThrows(WrongFormat.class, () -> c.check("sin(cos(1"));
        }

        @Test
        void singleParameterCalculationHandlesNestedBracketsAndGarbageNames() {
            Mathematical_Expression.register_function(FunctionPackage.MATH);
            FunctionFormulaCalculation c = FunctionFormulaCalculation.getInstance("gap2Ffc1Calc");
            assertEquals(0, new java.math.BigDecimal("2").compareTo(
                    new java.math.BigDecimal(String.valueOf(c.calculation("1+cos((0))", true).getResult()))));
            assertThrows(net.mooctest.expression.engine.ExtractException.class,
                    () -> c.calculation("1+C|S(0)", true));
        }
    }
}
