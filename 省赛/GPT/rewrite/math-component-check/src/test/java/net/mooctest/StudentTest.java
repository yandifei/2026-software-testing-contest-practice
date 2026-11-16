package net.mooctest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StudentTest {
    @Test
    void conditionalFunctionsChooseTheSpecifiedValue() {
        var branch = net.mooctest.expression.engine.FunctionPackage.BRANCH;
        String[] names = {"ifEq", "ifGt", "ifLt", "ifGe", "ifLe"};
        double[][] results = {{23, 11, 23}, {23, 23, 11}, {11, 23, 23}, {23, 11, 11}, {11, 11, 23}};
        for (int i = 0; i < names.length; i++) {
            var fn = (net.mooctest.expression.engine.ManyToOneNumberFunction) branch.getFunctionByNameNoCheck(names[i]);
            for (int x = 0; x <= 2; x++) {
                assertEquals(results[i][x], fn.run(x, 1, 11, 23), 1e-12);
            }
            assertThrows(UnsupportedOperationException.class, () -> fn.run(1, 1, 11));
        }
    }

    @Test
    void mathPackageUsesNumericArguments() {
        var math = net.mooctest.expression.engine.FunctionPackage.MATH;
        var examples = java.util.Map.of(
                "sum", new double[]{10, 7, -2, 5}, "pow", new double[]{81, 3, 4},
                "sqrt", new double[]{7, 49}, "factorial", new double[]{120, 5},
                "sin", new double[]{0.5, Math.PI / 6}, "cos", new double[]{0.5, Math.PI / 3},
                "tan", new double[]{1, Math.PI / 4}, "cot", new double[]{1, Math.PI / 4},
                "sec", new double[]{2, Math.PI / 3}, "csc", new double[]{2, Math.PI / 6});
        examples.forEach((name, sample) -> {
            var fn = (net.mooctest.expression.engine.ManyToOneNumberFunction) math.getFunctionByNameNoCheck(name);
            assertEquals(sample[0], fn.run(java.util.Arrays.copyOfRange(sample, 1, sample.length)), 1e-10, name);
        });
        assertNull(math.getFunctionByNameNoCheck("unknown"));
    }

    @Test
    void complexCalculatorSeparatesParenthesizedRealAndImaginaryParts() throws Exception {
        String label = "RewriteComplex";
        var calc = net.mooctest.expression.engine.ComplexCalculation.getInstance(label);
        try {
            calc.check("(2+3)+4i");
            var positive = calc.compile(" (2+3) + 4i ", true);
            assertEquals(5.0, positive.calculation(true).getResult());
            assertEquals(new net.mooctest.expression.engine.CalculationComplexResults(0, 5, 4, label).toString(),
                    positive.calculation(true).toString());
            assertTrue(positive.isUnBigDecimal());
            assertTrue(positive.isBigDecimal());
            assertNotNull(positive.getFunctionExpression1());
            assertNotNull(positive.getFunctionExpression2());
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("7+8"));
            assertThrows(RuntimeException.class, () -> calc.check("7"));
            assertThrows(UnsupportedOperationException.class, () -> calc.explain("7+8", true));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void compilerRejectsIncompleteSignaturesBeforeCalculating() {
        var malformed = java.util.List.of("f(a)", "f(a)=a=1", "f=a", "f2(a)=a");
        for (var signature : malformed) {
            assertThrows(IllegalArgumentException.class,
                    () -> net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource(signature));
        }
        assertEquals("public static double weighted(double a, double b) { return 3*a+b; }",
                net.mooctest.expression.engine.JvmExpressionFunction.convertToStaticMethodSource("weighted(a,b)=3*a+b"));
        assertThrows(IllegalArgumentException.class,
                () -> net.mooctest.expression.engine.EquationSolver.compile("x+2", "isolated"));
        assertThrows(NumberFormatException.class,
                () -> net.mooctest.expression.engine.EquationSolver.compile("x+2=abc", "isolated"));
    }

    @Test
    void pricePolicyEnforcesBoundsBeforeScaling() {
        var policy = new net.mooctest.course.service.PricePolicy();
        var invalid = new java.math.BigDecimal[]{null, new java.math.BigDecimal("-0.001"), new java.math.BigDecimal("99999.991")};
        for (var price : invalid) {
            var error = assertThrows(net.mooctest.error.BusinessException.class, () -> policy.normalizePrice(price));
            assertEquals(net.mooctest.error.ErrorCode.INVALID_PRICE, error.code());
        }
        assertEquals(new java.math.BigDecimal("99999.99"), policy.normalizePrice(new java.math.BigDecimal("99999.99")));
        assertEquals(new java.math.BigDecimal("0.00"), policy.normalizePrice(java.math.BigDecimal.ZERO));
        var price = new java.math.BigDecimal("25.35");
        assertEquals(new java.math.BigDecimal("5.07"), policy.discountedPrice(price, 80));
        assertEquals(price, policy.discountedPrice(price, 0));
        for (int invalidDiscount : new int[]{-1, 81}) {
            assertEquals(net.mooctest.error.ErrorCode.INVALID_DISCOUNT, assertThrows(net.mooctest.error.BusinessException.class,
                    () -> policy.discountedPrice(price, invalidDiscount)).code());
        }
    }

    @Test
    void restHandlerPreservesErrorCodeAndDetails() {
        var handler = new net.mooctest.course.web.RestExceptionHandler();
        var codes = net.mooctest.error.ErrorCode.class;
        var expected = java.util.Map.of(
                "COURSE_NOT_FOUND", 404, "DUPLICATE_COURSE_TITLE", 409, "COURSE_ALREADY_PUBLISHED", 409,
                "COURSE_HAS_NO_CHAPTERS", 422, "INVALID_COURSE_TITLE", 400, "INVALID_PRICE", 400, "INVALID_DISCOUNT", 400);
        expected.forEach((name, status) -> {
            var response = handler.handleBusinessException(new net.mooctest.error.BusinessException(Enum.valueOf(codes, name), "issue"));
            assertEquals(status.intValue(), response.getStatusCode().value());
            assertFalse(response.getBody().success());
            assertNull(response.getBody().data());
            assertEquals(name, response.getBody().error().code());
            assertEquals("issue", response.getBody().error().message());
        });
    }

    @Test
    void rangeArithmeticAgreesWithEnumeratedTerms() {
        String label = "RewriteRangeOracle";
        var calc = net.mooctest.expression.engine.BracketsCalculation2.getInstance(label);
        try {
            for (int start = 1; start <= 3; start++) {
                for (int end = start; end <= start + 4; end++) {
                    int consecutiveTotal = 0;
                    for (int x = start; x <= end; x++) consecutiveTotal += x;
                    assertEquals(consecutiveTotal, net.mooctest.expression.engine.NumberUtils.sumOfRange(start, end));
                    assertEquals(consecutiveTotal, net.mooctest.expression.engine.NumberUtils.sumOfRange((double) end, start), 1e-12);
                    for (int step = 1; step <= 3; step++) {
                        int total = 0;
                        int product = 1;
                        for (int x = start; x <= end; x += step) {
                            total += x;
                            product *= x;
                        }
                        assertEquals(total, net.mooctest.expression.engine.NumberUtils.sumOfRange(start, end, step), 1e-12);
                        assertEquals(product, net.mooctest.expression.engine.NumberUtils.MultiplyOfRange(start, end, step), 1e-12);
                        String sum = net.mooctest.expression.engine.NumberUtils.sumOfRangeString(start, end, step);
                        String multiply = net.mooctest.expression.engine.NumberUtils.MultiplyOfRangeString(start, end, step);
                        assertEquals(total, calc.calculation(sum).getResult(), 1e-12);
                        assertEquals(product, Double.parseDouble(multiply), 1e-12);
                    }
                }
            }
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void prefixCompilerProducesExactDecimalAndRejectsIncompleteSyntax() throws Exception {
        String label = "RewritePrefix";
        var calc = net.mooctest.expression.engine.PrefixExpressionOperation.getInstance(label);
        try {
            calc.check("0.1+0.2");
            var exact = (net.mooctest.expression.engine.CalculationNumberResults)
                    calc.compileBigDecimal("0.1+0.2", true).calculationBigDecimals(true);
            assertEquals(0, new java.math.BigDecimal("0.3").compareTo(exact.getBigDecimalResult()));
            var compiled = calc.compile("12/3+4*2", true);
            assertEquals(12.0, compiled.calculation(true).getResult());
            assertEquals(12.0, compiled.calculation(true).getResult());
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("3+"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calc.check("3@2"));
            assertEquals(11.0, (Double) calc.explain("3+4*2", true).getResult());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void basicFunctionCalculatorHandlesIntegralAndFractionalResults() throws Exception {
        String label = "RewriteBasicFunctions";
        var run = net.mooctest.expression.engine.FunctionFormulaCalculation.getInstance(label);
        var function = net.mooctest.expression.engine.ExpressionFunction.parse("rewritehalve(a)=a/2");
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(function));
        try {
            assertEquals("rewritehalve(6)+1", run.formatStr(" rewritehalve( 6 ) + 1 "));
            run.check("rewritehalve(123)");
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> run.check("rewritehalve(123"));
            assertEquals(4, run.calculation("rewritehalve((2+4))+1").getResult(), 1e-12);
            assertEquals(3.5, run.calculation("rewritehalve(5) + 1").getResult(), 1e-12);
            assertEquals(9, run.calculation("(2+1)*3").getResult(), 1e-12);
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF("rewritehalve");
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void factorialAndDecimalParsingRespectCacheAndPrecisionModes() {
        boolean prior = net.mooctest.expression.engine.Mathematical_Expression.Options.isUseCache();
        try {
            for (boolean useCache : new boolean[]{false, true}) {
                net.mooctest.expression.engine.Mathematical_Expression.Options.setUseCache(useCache);
                for (int i = 0; i < 2; i++) {
                    assertEquals(-73.0625, net.mooctest.expression.engine.StrUtils.stringToDouble("-73.0625"), 0);
                    assertEquals(720, net.mooctest.expression.engine.StrUtils.stringToDouble("6!"), 0);
                    assertEquals(new java.math.BigDecimal("-73.0625"), net.mooctest.expression.engine.StrUtils.stringToBigDecimal("-73.0625"));
                    assertEquals(0, new java.math.BigDecimal("720").compareTo(net.mooctest.expression.engine.StrUtils.stringToBigDecimal("6!")));
                }
                assertThrows(UnsupportedOperationException.class, () -> net.mooctest.expression.engine.StrUtils.stringToBigDecimal("-1!"));
                assertThrows(UnsupportedOperationException.class, () -> net.mooctest.expression.engine.StrUtils.stringToDouble("-1!"));
                assertThrows(NumberFormatException.class, () -> net.mooctest.expression.engine.StrUtils.stringToDouble("1..1"));
            }
        } finally {
            net.mooctest.expression.engine.Mathematical_Expression.Options.setUseCache(prior);
        }
    }

    @Test
    void parsedFunctionSurvivesSerializationRoundTrip() throws Exception {
        var function = net.mooctest.expression.engine.ExpressionFunction.parse("rewritegap(a,b)=a-b+a");
        var bytes = new java.io.ByteArrayOutputStream();
        try (var output = new java.io.ObjectOutputStream(bytes)) {
            function.saveTo(output);
        }
        try (var input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            var restored = net.mooctest.expression.engine.ExpressionFunction.readFrom(input);
            assertEquals("rewritegap", restored.getName());
            assertEquals("rewritegap(a,b)=a-b+a", restored.toString());
            assertEquals(13, restored.run(8, 3), 1e-12);
        }
    }

    @Test
    void primitiveOperatorsWithUnequalOperands() {
        var operators = new char[]{'+', '-', '*', '/', '%', '^'};
        var answers = new double[]{11, 5, 24, 8.0 / 3, 2, 512};
        for (int i = 0; i < operators.length; i++) {
            assertEquals(answers[i], net.mooctest.expression.engine.NumberUtils.calculation(operators[i], 8, 3), 1e-12);
        }
        assertThrows(net.mooctest.expression.engine.AbnormalOperation.class,
                () -> net.mooctest.expression.engine.NumberUtils.calculation('?', 8, 3));
    }

    @Test
    void comparisonSeparatesEqualityFromOrdering() {
        String[] operators = {"<", ">", "<=", ">=", "==", "=", "!=", "<>"};
        boolean[][] expected = {
            {true, false, true, false, false, false, true, true},
            {false, false, true, true, true, true, false, false},
            {false, true, false, true, false, false, true, true}
        };
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < operators.length; j++) {
                assertEquals(expected[i][j], net.mooctest.expression.engine.NumberUtils.ComparisonOperation(operators[j], i, 1), operators[j]);
            }
        }
    }

    @Test
    void groupedArithmeticAgreesWithWorkedExamples() {
        String name = "RewriteArithmetic";
        var calculator = net.mooctest.expression.engine.BracketsCalculation2.getInstance(name);
        try {
            assertEquals(19.0, calculator.calculation("1+(2+4)*3").getResult(), 1e-10);
            assertEquals(6.0, calculator.calculation("18/(1+2)").getResult(), 1e-10);
            assertEquals(2.0, calculator.calculation("7-2-3").getResult(), 1e-10);
            assertEquals(21.0, calculator.calculation("3*(4+(3))").getResult(), 1e-10);
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calculator.check("2*(3+4"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }

    @Test
    void engineSeparatesTopLevelDelimiters() {
        assertEquals(java.util.List.of("a", "(b,c)", "d"), net.mooctest.expression.engine.StrUtils.splitByCharWhereNoB("a,(b,c),d", ','));
        assertEquals(java.util.List.of("a", "(b", "c)", "d"), net.mooctest.expression.engine.StrUtils.splitByChar("a,(b,c),d", ','));
        assertTrue(net.mooctest.expression.engine.StrUtils.containsNumber("a2Z"));
        assertFalse(net.mooctest.expression.engine.StrUtils.containsNumber("az"));
        assertEquals('z', net.mooctest.expression.engine.StrUtils.containsEnChar("12z9"));
        assertNull(net.mooctest.expression.engine.StrUtils.containsEnChar("123"));
    }

    @Test
    void namedExpressionFunctionAcceptsNumericArgumentsAndEvaluatesBody() throws Exception {
        var fn = net.mooctest.expression.engine.ExpressionFunction.parse("delta(a,b)=a*a-b");
        assertEquals("delta", fn.getName());
        assertEquals(2, fn.getParamSize());
        assertEquals(21.0, fn.run(5, 4), 1e-10);
        assertEquals(12.0, fn.run(4, 4), 1e-10);
        assertThrows(UnsupportedOperationException.class, () -> fn.run(5));
        assertThrows(net.mooctest.expression.engine.WrongFormat.class,
                () -> net.mooctest.expression.engine.ExpressionFunction.parse("delta(a,b)=a+z"));
    }

    @Test
    void builtInTimeConversionsUseDistinctUnits() {
        var dates = net.mooctest.expression.engine.FunctionPackage.DATE;
        String[] units = {"S", "Min", "Hour", "Day", "Week", "Month", "Year"};
        long[] millis = {1000L, 60000L, 3600000L, 86400000L, 604800000L, 2592000000L, 31536000000L};
        for (int i = 0; i < units.length; i++) {
            String unit = units[i];
            var toUnit = (net.mooctest.expression.engine.ManyToOneNumberFunction) dates.getFunctionByNameNoCheck("msTo" + unit);
            String prefix = Character.toLowerCase(unit.charAt(0)) + unit.substring(1);
            var toMillis = (net.mooctest.expression.engine.ManyToOneNumberFunction) dates.getFunctionByNameNoCheck(prefix + "ToMs");
            assertEquals(1.5, toUnit.run(1.5 * millis[i]), 1e-12, unit);
            assertEquals(-2.0 * millis[i], toMillis.run(-2), 1e-12, unit);
        }
    }

    @Test
    void directCourseLifecyclePreservesIdentityAndStoredState() {
        var repo = new net.mooctest.course.adapter.InMemoryCourseRepository();
        var clock = java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC);
        net.mooctest.course.port.ChapterRepository chapters = id -> java.util.List.of(new net.mooctest.course.domain.Chapter("intro", id, "First", 1));
        var service = new net.mooctest.course.service.CourseService(repo, chapters, () -> "linear", new net.mooctest.course.service.PricePolicy(), clock);
        var first = service.createCourse("  Matrix    Theory  ", new java.math.BigDecimal("9.995"));
        assertEquals(new net.mooctest.course.domain.Course("linear", "Matrix Theory", new java.math.BigDecimal("10.00"),
                net.mooctest.course.domain.CourseStatus.DRAFT, java.time.Instant.EPOCH), first);
        var published = service.publishCourse("linear");
        assertEquals(net.mooctest.course.domain.CourseStatus.PUBLISHED, published.status());
        assertEquals(published, repo.findById("linear").orElseThrow());
        var duplicate = assertThrows(net.mooctest.error.BusinessException.class,
                () -> service.createCourse("matrix theory", java.math.BigDecimal.ZERO));
        assertEquals(net.mooctest.error.ErrorCode.DUPLICATE_COURSE_TITLE, duplicate.code());
        var catalog = new net.mooctest.course.service.CatalogService(service, chapters,
                ids -> java.util.List.of(new net.mooctest.course.domain.Video("formula", "intro", "Formula", 45, 1)));
        assertEquals(new net.mooctest.course.domain.CourseCatalog("linear", java.util.List.of(
            new net.mooctest.course.domain.ChapterView("intro", "First", java.util.List.of(
                new net.mooctest.course.domain.VideoView("formula", "Formula", 45))))), catalog.getCatalog("linear"));
    }
    @Test
    void intervalCalculatorsEvaluateEndpointsAndSteps() throws Exception {
        String sumName = "RewriteSums";
        String productName = "RewriteProducts";
        var sum = net.mooctest.expression.engine.FastSumOfIntervalsBrackets.getInstance(sumName);
        var product = net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(productName);
        try {
            sum.check("1+1,5");
            assertTrue(sum.isCache("1+1,5"));
            assertEquals(14, sum.calculation("1+1,5").getResult(), 1e-12);
            assertEquals(14, sum.calculation("1+1,5").getResult(), 1e-12);
            assertEquals(4, sum.calculation("4", "4").getResult(), 1e-12);
            assertEquals(18.0, sum.compile("3,6", true).calculation(true).getResult());
            assertEquals(48, product.calculation("2,6").getResult(), 1e-12);
            assertEquals(48.0, product.compile("2,6", true).calculation(true).getResult());
            sum.setStartSharedPool(false);
            assertFalse(sum.isStartSharedPool());
            assertEquals(7, sum.calculation("3,4").getResult(), 1e-12);
            sum.clearCache();
            assertFalse(sum.isCache("1+1,5"));
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> sum.check("3"));
            assertThrows(net.mooctest.expression.engine.ExtractException.class, () -> net.mooctest.expression.engine.FastMultiplyOfIntervalsBrackets.getInstance(sumName));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(sumName);
            net.mooctest.expression.engine.CalculationManagement.unregister(productName);
        }
    }

    @Test
    void functionCalculatorResolvesNestedParametersAndClearsItsCache() throws Exception {
        String label = "RewriteFunctions";
        String name = "rewritecube";
        var fn = net.mooctest.expression.engine.ExpressionFunction.parse(name + "(a)=a*a*a");
        var calculator = net.mooctest.expression.engine.FunctionFormulaCalculation2.getInstance(label);
        assertTrue(net.mooctest.expression.engine.CalculationManagement.register(fn));
        try {
            String expression = name + "(2)+1";
            calculator.check(expression);
            assertTrue(calculator.isCache(expression));
            calculator.check(expression);
            assertEquals(9, calculator.calculation(expression).getResult(), 1e-12);
            assertEquals(9, calculator.calculation(expression).getResult(), 1e-12);
            assertEquals(512, calculator.calculation(name + "(" + name + "(2))").getResult(), 1e-12);
            assertEquals(8, (Double) calculator.explain(name + "(2)", true).getResult(), 1e-12);
            assertEquals(6, calculator.calculation("6").getResult(), 1e-12);
            calculator.clearCache();
            assertFalse(calculator.isCache(expression));
            calculator.setStartSharedPool(false);
            assertFalse(calculator.isStartSharedPool());
            assertEquals(28, calculator.calculation(name + "(3)+1").getResult(), 1e-12);
            assertThrows(net.mooctest.expression.engine.WrongFormat.class, () -> calculator.check(name + "(2"));
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregisterF(name);
            net.mooctest.expression.engine.CalculationManagement.unregister(label);
        }
    }

    @Test
    void precisionAndComparisonCalculatorsKeepTheirResultTypes() {
        String name = "RewriteBoolean";
        var calculator = net.mooctest.expression.engine.BooleanCalculation2.getInstance(name);
        try {
            assertEquals(true, calculator.calculation("(2+3)<=5", true).getResult());
            assertEquals(false, calculator.calculation("2+3<5", true).getResult());
            assertEquals(true, calculator.calculation("4<>5", true).getResult());
            assertEquals(false, calculator.calculation("4!=4", true).getResult());
        } finally {
            net.mooctest.expression.engine.CalculationManagement.unregister(name);
        }
    }


}
