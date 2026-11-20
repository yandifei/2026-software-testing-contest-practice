package net.mooctest.expression.engine;

import java.util.HashMap;
import java.util.Map;

/** Shared evaluation options for the selected expression implementation. */
public final class Mathematical_Expression {
    private Mathematical_Expression() {
    }

    public static final class Options {
        private static final Map<Integer, Number> NUMBER_CACHE = new HashMap<>();
        private static final Map<String, PackExpression> EXPRESSION_CACHE = new HashMap<>();
        private static boolean useBigDecimal;
        private static boolean useCache;

        private Options() {
        }

        public static void cacheCalculation(int key, Number number) {
            NUMBER_CACHE.put(key, number);
        }

        public static Number getCacheCalculation(int key) {
            return NUMBER_CACHE.get(key);
        }

        public static void cacheCalculation(String formula, PackExpression expression) {
            EXPRESSION_CACHE.put(formula, expression);
        }

        public static PackExpression getCacheCalculation(String formula) {
            return EXPRESSION_CACHE.get(formula);
        }

        public static boolean isUseBigDecimal() {
            return useBigDecimal;
        }

        public static void setUseBigDecimal(boolean enabled) {
            useBigDecimal = enabled;
        }

        public static Boolean isUseCache() {
            return useCache;
        }

        public static void setUseCache(boolean enabled) {
            if (!enabled) {
                NUMBER_CACHE.clear();
                EXPRESSION_CACHE.clear();
            }
            useCache = enabled;
        }
    }
}
