package net.mooctest;

import java.io.*;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.ArrayList;
import java.util.function.DoubleUnaryOperator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import top.lingyuzhao.varFormatter.core.VarFormatter;

/**
 * 项目说明：
 * 构建环境：
 * - Maven 版本：3.9.6 及以上
 * - JDK 版本：21
 * - JUnit 版本：5.10.2
**/

class Mathematical_ExpressionTest {
    @TempDir Path temporaryDirectory;
    private Map<String, Function> originalFunctions;
    private Map<String, String> originalJvmFunctions;

    @BeforeEach
    void setUp() {
        originalFunctions = new HashMap<>();
        for (String name : CalculationManagement.getFunctionMap()) {
            originalFunctions.put(name, CalculationManagement.getFunctionByNameNoCheck(name));
        }
        originalJvmFunctions = new LinkedHashMap<>(JvmExpressionFunction.getRegisteredMethods());
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
    }

    @AfterEach
    void tearDown() {
        for (String name : new HashSet<>(CalculationManagement.getFunctionMap())) {
            CalculationManagement.unregisterF(name);
        }
        originalFunctions.values().forEach(CalculationManagement::register);
        JvmExpressionFunction.getRegisteredMethods().clear();
        JvmExpressionFunction.getRegisteredMethods().putAll(originalJvmFunctions);
        Mathematical_Expression.Options.setUseCache(false);
        Mathematical_Expression.Options.setUseBigDecimal(false);
        for (Mathematical_Expression type : Mathematical_Expression.values()) {
            CalculationManagement.unregister("facade" + type.name());
        }
        CalculationManagement.unregister("facadeRegistry");
    }

    @Test
    void getInstance() {
        Calculation instance = Mathematical_Expression.getInstance(Mathematical_Expression.bracketsCalculation2);

        assertNotNull(instance);
        assertInstanceOf(BracketsCalculation2.class, instance);
    }

    @Test
    void allFactoriesPreserveTypeNameAndRegisteredIdentity() {
        Class<?>[] types = {BracketsCalculation2.class, CumulativeCalculation.class,
                FastMultiplyOfIntervalsBrackets.class, FastSumOfIntervalsBrackets.class,
                FunctionFormulaCalculation.class, FunctionFormulaCalculation2.class,
                PrefixExpressionOperation.class, BooleanCalculation2.class, ComplexCalculation.class,
                JvmCalculation.class, SingletonEquationSolving.class, SingletonEquationSolvingTwo.class};
        Mathematical_Expression[] values = Mathematical_Expression.values();
        assertEquals(12, values.length);
        for (int i = 0; i < values.length; i++) {
            Mathematical_Expression type = values[i];
            String name = "facade" + type.name();
            Calculation named = Mathematical_Expression.getInstance(type, name);
            assertEquals(types[i], named.getClass());
            assertEquals(name, named.getName());
            assertSame(named, type.getInstance(name));
            assertSame(named, CalculationManagement.getCalculationByName(name));
            Calculation defaultInstance = Mathematical_Expression.getInstance(type);
            assertEquals(type.name(), defaultInstance.getName());
            assertSame(defaultInstance, Mathematical_Expression.getInstance(type));
            assertSame(type, Mathematical_Expression.valueOf(type.name()));
        }
    }

    @Test
    void factoriesRejectNamesOwnedByAnotherCalculatorType() {
        for (Mathematical_Expression type : Mathematical_Expression.values()) {
            String name = "facade" + type.name();
            Calculation conflict = type == Mathematical_Expression.prefixExpressionOperation
                    ? new BracketsCalculation2(name) : PrefixExpressionOperation.getInstance(name);
            assertTrue(CalculationManagement.register(conflict));
            assertThrows(ExtractException.class, () -> type.getInstance(name), type.name());
            assertSame(conflict, CalculationManagement.getCalculationByName(name));
        }
    }

    @Test
    void functionRegistrationRejectsDuplicatesAndSupportsBothUnregisterOverloads() {
        ManyToOneNumberFunction function = new SerializableFunction("facadeDouble", true);
        assertFalse(CalculationManagement.isFunctionExist("facadeDouble"));
        assertTrue(Mathematical_Expression.register_function(function));
        assertSame(function, Mathematical_Expression.getFunction("facadeDouble"));
        assertEquals(14, Mathematical_Expression.getFunction("facadeDouble").run(7));
        assertTrue(Mathematical_Expression.getFunctionMap().contains("facadeDouble"));
        assertFalse(Mathematical_Expression.register_function(new SerializableFunction("facadeDouble", true)));
        assertSame(function, CalculationManagement.getFunctionByNameNoCheck("facadeDouble"));
        assertTrue(Mathematical_Expression.unregister_function(function));
        assertFalse(Mathematical_Expression.unregister_function(function));
        assertNull(CalculationManagement.getFunctionByNameNoCheck("facadeDouble"));
        assertThrows(ExtractException.class, () -> Mathematical_Expression.getFunction("facadeDouble"));
        assertTrue(Mathematical_Expression.register_function(function));
        assertTrue(Mathematical_Expression.unregister_function("facadeDouble"));
        assertFalse(Mathematical_Expression.unregister_function("facadeDouble"));
        assertFalse(Mathematical_Expression.getFunctionMap().contains("facadeDouble"));
    }

    @Test
    void expressionAndAnnotatedRegistrationsExposeCallableFunctions() throws WrongFormat {
        assertTrue(Mathematical_Expression.register_function("facadeLinear(x)=3*x+2"));
        assertEquals(17, Mathematical_Expression.getFunction("facadeLinear").run(5));
        assertFalse(Mathematical_Expression.register_function("facadeLinear(x)=x+99"));
        assertEquals(17, Mathematical_Expression.getFunction("facadeLinear").run(5));
        assertTrue(Mathematical_Expression.register_function(AnnotatedFunctions.class));
        assertEquals(9, Mathematical_Expression.getFunction("facadeSquare").run(3));
        assertEquals(6, Mathematical_Expression.getFunction("facadeAdd").run(2, 4));
        assertTrue(Mathematical_Expression.register_function(EmptyFunctions.class));
        assertFalse(Mathematical_Expression.register_function(Object.class));
        assertThrows(UnsupportedOperationException.class,
                () -> Mathematical_Expression.register_function("not a function"));
    }

    @Test
    void jvmRegistrationStoresEveryMethod() {
        Mathematical_Expression.register_jvm_function("facadeJvm(x)=x*4");
        assertEquals("public static double facadeJvm(double x) { return x*4; }",
                JvmExpressionFunction.getRegisteredMethods().get("double facadeJvm"));
        assertTrue(Mathematical_Expression.register_jvm_function(AnnotatedFunctions.class));
        assertEquals("public static double facadeSquare(double x) { return x*x; }",
                JvmExpressionFunction.getRegisteredMethods().get("double facadeSquare"));
        assertEquals("public static double facadeAdd(double x, double y) { return x+y; }",
                JvmExpressionFunction.getRegisteredMethods().get("double facadeAdd"));
        assertTrue(Mathematical_Expression.register_jvm_function(EmptyFunctions.class));
        assertFalse(Mathematical_Expression.register_jvm_function(Object.class));
    }

    @Test
    void functionPackageRegistersActualFunctions() {
        Mathematical_Expression.register_function(FunctionPackage.MATH);
        assertEquals(3, Mathematical_Expression.getFunction("sqrt").run(9));
        assertSame(FunctionPackage.MATH.getFunctionByNameNoCheck("sqrt"),
                Mathematical_Expression.getFunction("sqrt"));
    }

    @Test
    void serializedFilesRoundTripAndRejectOtherObjects() throws Exception {
        File file = temporaryDirectory.resolve("function.bin").toFile();
        Mathematical_Expression.saveFunction(new SerializableFunction("facadeFile", true), file);
        assertTrue(Mathematical_Expression.register_function(file));
        assertEquals(12, Mathematical_Expression.getFunction("facadeFile").run(6));
        assertFalse(Mathematical_Expression.register_function(file));
        Files.write(file.toPath(), serialize("a string"));
        assertFalse(Mathematical_Expression.register_function(file));
        Files.write(file.toPath(), serialize((Object) null));
        assertFalse(Mathematical_Expression.register_function(file));
        assertThrows(IOException.class, () -> Mathematical_Expression.register_function(
                temporaryDirectory.resolve("missing.bin").toFile()));
    }

    @Test
    void streamRegistrationCountsSuccessDuplicateAndNonFunctions() throws Exception {
        SerializableFunction first = new SerializableFunction("facadeStreamFirst", true);
        SerializableFunction second = new SerializableFunction("facadeStreamSecond", true);
        TrackingInput input = new TrackingInput(serialize(first, first, "text", null, second));
        assertEquals(Map.entry(2, 3), Mathematical_Expression.register_function(input));
        assertTrue(input.closed);
        assertEquals(8, Mathematical_Expression.getFunction("facadeStreamFirst").run(4));
        assertEquals(10, Mathematical_Expression.getFunction("facadeStreamSecond").run(5));
        assertEquals(Map.entry(0, 0), Mathematical_Expression.register_function(
                new ByteArrayInputStream(serialize())));
    }

    @Test
    void savingFiltersOptOutFunctionsAndClosesOutput() throws Exception {
        TrackingOutput output = new TrackingOutput();
        Mathematical_Expression.saveFunction(output,
                new SerializableFunction("facadeSavedFirst", true),
                new SerializableFunction("facadeSkipped", false),
                new SerializableFunction("facadeSavedSecond", true));
        assertTrue(output.closed);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ManyToOneNumberFunction first = (ManyToOneNumberFunction) in.readObject();
            ManyToOneNumberFunction second = (ManyToOneNumberFunction) in.readObject();
            assertEquals("facadeSavedFirst", first.getName());
            assertEquals(18, first.run(9));
            assertEquals("facadeSavedSecond", second.getName());
            assertThrows(EOFException.class, in::readObject);
        }
        TrackingOutput empty = new TrackingOutput();
        Mathematical_Expression.saveFunction(empty);
        assertTrue(empty.closed);
        assertEquals(Map.entry(0, 0), Mathematical_Expression.register_function(
                new ByteArrayInputStream(empty.toByteArray())));
    }

    @Test
    void unknownSerializedClassesProduceDocumentedExceptions() throws Exception {
        String encoded = new String(serialize(new Date(0)), StandardCharsets.ISO_8859_1);
        assertTrue(encoded.contains("java.util.Date"));
        byte[] bytes = encoded.replace("java.util.Date", "java.util.Fate")
                .getBytes(StandardCharsets.ISO_8859_1);
        TrackingInput input = new TrackingInput(bytes);
        WrongFormat error = assertThrows(WrongFormat.class,
                () -> Mathematical_Expression.register_function(input));
        assertTrue(error.getMessage().contains("unknown class"));
        assertTrue(input.closed);
        Path file = temporaryDirectory.resolve("unknown.bin");
        Files.write(file, bytes);
        assertThrows(ClassNotFoundException.class,
                () -> Mathematical_Expression.register_function(file.toFile()));
        assertThrows(StreamCorruptedException.class, () -> Mathematical_Expression.register_function(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4})));
    }

    @Test
    void flagsRetainCacheWhenEnabledAndClearBothCachesWhenDisabled() {
        new Mathematical_Expression.Options();
        assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
        Mathematical_Expression.Options.setUseBigDecimal(true);
        assertTrue(Mathematical_Expression.Options.isUseBigDecimal());
        Mathematical_Expression.Options.setUseBigDecimal(false);
        assertFalse(Mathematical_Expression.Options.isUseBigDecimal());
        assertFalse(Mathematical_Expression.Options.isUseCache());
        assertNull(Mathematical_Expression.Options.getCacheCalculation(789));
        assertNull(Mathematical_Expression.Options.getCacheCalculation("facadeFormula"));
        Number number = new BigDecimal("12.75");
        PackExpression expression = new PackExpression("cache",
                PrefixExpression.compile("2+3", "facadeCache"), "facadePack");
        Mathematical_Expression.Options.cacheCalculation(789, number);
        Mathematical_Expression.Options.cacheCalculation("facadeFormula", expression);
        Mathematical_Expression.Options.setUseCache(true);
        assertTrue(Mathematical_Expression.Options.isUseCache());
        assertSame(number, Mathematical_Expression.Options.getCacheCalculation(789));
        assertSame(expression, Mathematical_Expression.Options.getCacheCalculation("facadeFormula"));
        Mathematical_Expression.Options.setUseCache(false);
        assertFalse(Mathematical_Expression.Options.isUseCache());
        assertNull(Mathematical_Expression.Options.getCacheCalculation(789));
        assertNull(Mathematical_Expression.Options.getCacheCalculation("facadeFormula"));
    }

    @Test
    void calculationRegistryChecksIdentityAndSupportsReplacement() {
        new CalculationManagement();
        String name = "facadeRegistry";
        assertFalse(CalculationManagement.isRegister(name));
        assertNull(CalculationManagement.getCalculationByName(name));
        BracketsCalculation2 first = new BracketsCalculation2(name);
        BracketsCalculation2 second = new BracketsCalculation2(name);
        assertTrue(CalculationManagement.register(first, true));
        assertFalse(CalculationManagement.register(first, true));
        assertTrue(CalculationManagement.isRegister(name));
        assertSame(first, CalculationManagement.getCalculationByName(name));
        assertTrue(CalculationManagement.register(second, true));
        assertSame(second, CalculationManagement.getCalculationByName(name));
        assertTrue(CalculationManagement.register(first));
        assertSame(first, CalculationManagement.getCalculationByName(name));
        assertTrue(CalculationManagement.unregister(name));
        assertFalse(CalculationManagement.unregister(name));
        assertFalse(CalculationManagement.isRegister(name));
    }

    @Test
    void decimalOperationsPreserveExactResultsAndRejectUnknownOperators() {
        new CalculationOptimized();
        char[] operators = {'+', '-', '*', '/', '%', '^'};
        String[] expected = {"10", "6", "16", "4", "0", "64"};
        for (int i = 0; i < operators.length; i++) {
            assertEquals(new BigDecimal(expected[i]), CalculationOptimized.calculation(
                    operators[i], new BigDecimal("8"), new BigDecimal("2")));
        }
        assertEquals(new BigDecimal("0.3"), CalculationOptimized.calculation(
                '+', new BigDecimal("0.1"), new BigDecimal("0.2")));
        assertThrows(AbnormalOperation.class, () -> CalculationOptimized.calculation(
                '?', BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(ArithmeticException.class, () -> CalculationOptimized.calculation(
                '/', BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void functionNamesValidateRenameAndRoundTripThroughSerialization() throws Exception {
        NamedFunction function = new NamedFunction();
        assertEquals("f", function.getName());
        assertTrue(function.AllowSerialization());
        for (String invalid : new String[]{"", "bad1", "two names", "with_underscore"}) {
            assertThrows(ExtractException.class, () -> new NamedFunction(invalid));
        }
        assertEquals("Az", new NamedFunction("Az").getName());
        function.setName("facadeRenamed");
        assertEquals("facadeRenamed", function.getName());
        assertEquals("facadeRenamed", function.toString());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Mathematical_Expression.saveFunction(bytes, function);
        assertEquals(Map.entry(1, 0), Mathematical_Expression.register_function(
                new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(9, Mathematical_Expression.getFunction("facadeRenamed").run(8));
    }

    static class NamedFunction extends ManyToOneNumberFunction {
        private static final long serialVersionUID = 1L;
        NamedFunction() { super(); }
        NamedFunction(String name) { super(name); }
        @Override public double run(double... numbers) { return numbers[0] + 1; }
    }

    private static byte[] serialize(Object... values) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            for (Object value : values) output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    @Functions({"facadeSquare(x)=x*x", "facadeAdd(x,y)=x+y"})
    static class AnnotatedFunctions { }
    @Functions({})
    static class EmptyFunctions { }

    static class SerializableFunction extends ManyToOneNumberFunction {
        private static final long serialVersionUID = 1L;
        private final boolean serializable;
        SerializableFunction(String name, boolean serializable) {
            super(name);
            this.serializable = serializable;
        }
        @Override public double run(double... numbers) { return numbers[0] * 2; }
        @Override public boolean AllowSerialization() { return serializable; }
    }
    static class TrackingInput extends ByteArrayInputStream {
        boolean closed;
        TrackingInput(byte[] bytes) { super(bytes); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
    static class TrackingOutput extends ByteArrayOutputStream {
        boolean closed;
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    @Nested
    class CalculatorAndExpressionTest {
        @BeforeEach
        void calculatorOptionsReset() {
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            ComplexCalculation.FUNCTION_FORMULA_CALCULATION_2.clearCache();
            ComplexCalculation.FUNCTION_FORMULA_CALCULATION_2.setStartSharedPool(true);
            for (String name : new String[]{"calcfactoryprefix", "calcfactorybracket", "calcfactoryboolean", "calcfactorycumulative", "calcfactorysum", "calcfactorymultiply", "calcfactorycomplex", "calcmultiplyinterval", "calcsuminterval"})
                CalculationManagement.unregister(name);
        }
    
        @AfterEach
        void calculatorOptionsRestore() {
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
    
        private static void value(double expected, CalculationResults actual) {
            assertEquals(expected, ((Number) actual.getResult()).doubleValue(), 1e-10);
        }
    
        @Test
        void prefixArithmeticRespectsSignsAndReductionOrder() {
            PrefixExpressionOperation p = PrefixExpressionOperation.getInstance("calcprefixarithmetic");
            assertEquals("calcprefixarithmetic", p.getName());
            assertEquals("2-3+0", p.formatStr("2+-3"));
            String[] formulas = {"2+3*4", "20/4+3", "8-3-2", "2^3+1", "13%5+1", "-2*-3+4", "3!+2", "0.5+1.25", "7"};
            double[] expected = {14, 8, 3, 9, 4, 10, 8, 1.75, 7};
            for (int i = 0; i < formulas.length; i++) {
                value(expected[i], p.compile(formulas[i], true).calculation(true));
                value(expected[i], p.explain(formulas[i], true));
            }
            value(5, p.compile("2+3", false).calculation(false));
            value(5, p.compileBigDecimal("2+3", false).calculationBigDecimals(false));
            value(14, p.compileBigDecimal("2+3*4", true).calculationBigDecimals(true));
            value(14, p.calculation("2+3*4"));
            Mathematical_Expression.Options.setUseBigDecimal(true);
            value(14, p.calculation("2+3*4", true));
            value(7, p.calculation("7", false));
            assertEquals(0.30000000000000004, p.calculation("0.1+0.2", true).getResult());
            Mathematical_Expression.Options.setUseBigDecimal(false);
            assertEquals(0.3, p.calculation("0.1+0.2", true).getResult());
            StringBuilder number = new StringBuilder();
            for (char c : "a9.!-+".toCharArray()) PrefixExpression.isBackIsOpt(number, c);
            assertEquals("9.!-", number.toString());
        }
    
        @Test
        void prefixChecksRejectMalformedAndAcceptUnarySigns() {
            PrefixExpressionOperation p = PrefixExpressionOperation.getInstance("calcprefixvalidation");
            for (String s : new String[]{"0", "-2+3", "2*-3", "1.5 + 2  ", "2--3"}) assertDoesNotThrow(() -> p.check(s));
            for (String s : new String[]{"1+", "(1)+2", "(1+2", "1)+2", "1@2", "2**3", "2++3", "2!"})
                assertThrows(WrongFormat.class, () -> p.check(s), s);
            assertTrue(assertThrows(WrongFormat.class, () -> p.check("1)+2")).getMessage().contains("without parentheses"));
            assertThrows(IndexOutOfBoundsException.class, () -> p.check(""));
            assertThrows(IndexOutOfBoundsException.class, () -> p.check("  "));
        }
    
        @Test
        void prefixPrecisionConversionAndCopiesKeepOperandsIndependent() {
            PrefixExpression normal = PrefixExpression.compile("2+3", "normal");
            assertTrue(normal.isUnBigDecimal());
            assertFalse(normal.isBigDecimal());
            assertNull(normal.getCache());
            assertEquals("2+3 -> 2.0+0", normal.getExpressionStr());
            assertEquals("normal", normal.getCalculationName());
            assertSame(normal.getDoubleStack(false), normal.getDoubleStack(false));
            assertNotSame(normal.getDoubleStack(false), normal.getDoubleStack(true));
            normal.getDoubleStack(true).clear();
            normal.getCharacterStack(true).clear();
            assertEquals(2, normal.getDoubleStack(false).size());
            assertEquals(1, normal.getCharacterStack(false).size());
            assertSame(normal.getCharacterStack(false), normal.getCharacterStack(false));
            assertThrows(UnsupportedOperationException.class, () -> normal.calculationBigDecimals(true));
            Stack<Double> originalDoubleOperands = normal.getDoubleStack(false);
            normal.convertToMultiPrecisionSupported();
            assertSame(originalDoubleOperands, normal.getDoubleStack(false));
            assertTrue(normal.isBigDecimal());
            assertTrue(normal.isUnBigDecimal());
            assertSame(normal.getBigDecimalsR(false), normal.getBigDecimalsR(false));
            assertNotSame(normal.getBigDecimalsR(false), normal.getBigDecimalsR(true));
            normal.getBigDecimalsR(true).clear();
            assertEquals(2, normal.getBigDecimalsR(false).size());
            normal.convertToMultiPrecisionSupported();
            assertSame(originalDoubleOperands, normal.getDoubleStack(false));
            value(5, normal.calculationBigDecimals(true));
            value(5, normal.calculation(true));
            assertEquals(1, normal.calculation(true).getResultLayers());
            assertEquals("normal", normal.calculation(true).getCalculationSourceName());
    
            PrefixExpression decimal = PrefixExpression.compileBigDecimal("4+2", "decimal");
            assertTrue(decimal.isBigDecimal());
            assertFalse(decimal.isUnBigDecimal());
            assertThrows(UnsupportedOperationException.class, () -> decimal.calculation(false));
            decimal.convertToMultiPrecisionSupported();
            decimal.convertToMultiPrecisionSupported();
            assertTrue(decimal.isUnBigDecimal());
            value(6, decimal.calculation(false));
            value(6, decimal.calculationBigDecimals(false));
            Stack<BigDecimal> bothDecimal = new Stack<>(); bothDecimal.push(BigDecimal.valueOf(9));
            Stack<Double> bothDouble = new Stack<>(); bothDouble.push(9d);
            PrefixExpression both = new PrefixExpression(bothDecimal, bothDouble, new Stack<>(), "9", "both");
            both.convertToMultiPrecisionSupported();
            value(9, both.calculationBigDecimals(true));
        }
    
        @Test
        void precisionConversionPreservesPrecomputedOperandsForPublicSubclasses() {
            Stack<BigDecimal> decimals = new Stack<>();
            decimals.push(new BigDecimal("0.1000000000000000000000000001"));
            Stack<Double> doubles = new Stack<>();
            doubles.push(0.1);
            PrefixExpression expression = new PrefixExpression(decimals, doubles, new Stack<>(), "0.1", "customprecision") {
                @Override public boolean isBigDecimal() { return false; }
                @Override public boolean isUnBigDecimal() { return true; }
            };
            expression.convertToMultiPrecisionSupported();
            assertSame(decimals, expression.getBigDecimalsR(false));
            assertSame(doubles, expression.getDoubleStack(false));
            assertEquals(new BigDecimal("0.1000000000000000000000000001"), expression.getBigDecimalsR(false).firstElement());
            value(0.1, expression.calculation(true));
        }
    
        @Test
        void expressionCachePreservesResultAfterClosingReusableState() {
            PrefixExpression e = PrefixExpression.compile("4+3", "cachednormal");
            CalculationResults first = e.calculationCache(true);
            value(7, first);
            assertTrue(e.isAvailable());
            assertSame(first, e.getCache());
            assertSame(first, e.calculationCache(false));
            assertFalse(e.isAvailable());
            assertSame(first, e.calculationCache(true));
            assertThrows(UnsupportedOperationException.class, () -> e.getDoubleStack(false));
            assertThrows(UnsupportedOperationException.class, () -> e.getCharacterStack(true));
            assertThrows(UnsupportedOperationException.class, () -> e.getBigDecimalsR(false));
            PrefixExpression d = PrefixExpression.compileBigDecimal("8+1", "cacheddecimal");
            CalculationResults decimal = d.calculationBigDecimalsCache(true);
            value(9, decimal);
            assertTrue(d.isAvailable());
            assertSame(decimal, d.calculationBigDecimalsCache(false));
            assertFalse(d.isAvailable());
            assertSame(decimal, d.calculationBigDecimalsCache(true));
            assertThrows(UnsupportedOperationException.class, () -> d.getBigDecimalsR(true));
            e.setCache(new CalculationNumberResults(0, 42, "replacement"));
            value(42, e.calculationCache(true));
        }
    
        @Test
        void compilationCacheKeepsNormalAndDecimalExpressionsSeparate() {
            Mathematical_Expression.Options.setUseCache(true);
            PrefixExpression p = PrefixExpression.compile("431+12", "cacheprefix");
            assertSame(p, PrefixExpression.compile("431+12", "other"));
            PrefixExpression d = PrefixExpression.compileBigDecimal("431+12", "cachedecimal");
            assertSame(d, PrefixExpression.compileBigDecimal("431+12", "other"));
            assertNotSame(p, d);
            value(443, p.calculationCache(false));
            value(443, d.calculationBigDecimalsCache(false));
            BracketsCalculation2 b = BracketsCalculation2.getInstance("calcbracketcache");
            PackExpression e = b.compile("(43+2)*3", true);
            assertSame(e, b.compile("(43+2)*3", true));
            value(135, e.calculationCache(false));
            PackExpression eb = b.compileBigDecimal("(43+2)*3", true);
            assertSame(eb, b.compileBigDecimal("(43+2)*3", true));
            value(135, eb.calculationBigDecimalsCache(false));
        }
    
        @Test
        void wrapperPreservesExpressionAndChangesResultSource() {
            PrefixExpression n = PrefixExpression.compile("3+8", "inner");
            PackExpression p = new PackExpression("wrapper", n, "outer");
            assertEquals("wrapper -> 3+8 -> 3.0+0", p.getExpressionStr());
            assertEquals("outer:{inner}", p.getCalculationName());
            assertSame(n, p.dismantling());
            assertTrue(p.isUnBigDecimal());
            assertFalse(p.isBigDecimal());
            p.convertToMultiPrecisionSupported();
            assertTrue(p.isBigDecimal());
            value(11, p.calculation(true));
            assertEquals("outer:{inner}", p.calculation(true).getCalculationSourceName());
            PackExpression decimal = new PackExpression(null, PrefixExpression.compileBigDecimal("1+6", "innerb"), "outerb");
            assertFalse(decimal.isUnBigDecimal());
            assertEquals("1+6 -> 1+0", decimal.getExpressionStr());
            value(7, decimal.calculationBigDecimals(true));
            assertEquals("outerb:{innerb}", decimal.calculationBigDecimals(true).getCalculationSourceName());
        }
    
        @Test
        void bracketParsingComputesNestedTermsAndSupportsBothPrecisions() {
            BracketsCalculation2 b = BracketsCalculation2.getInstance("calcbracketarithmetic");
            assertEquals("(1+2)*3", b.formatStr(" \t(1 + 2) *\n3 "));
            value(45, b.calculation(" (2 + (3*4)) * 3 + 3 ", true));
            value(20, b.calculation("(2+3)*4", false));
            value(11, b.compile("3+(2*4)", true).calculation(true));
            value(11, b.compileBigDecimal("3+(2*4)", false).calculationBigDecimals(true));
            Mathematical_Expression.Options.setUseBigDecimal(true);
            value(0.3, b.calculation("0.1+0.2", true));
            value(9, b.explain(" (2+(1*3)) +4 ", true));
            value(5, b.explain("2+3", false));
            assertEquals(-5, b.calculation2("8-3"));
            assertEquals(6, b.calculation2("2*3"));
            assertThrows(AbnormalOperation.class, () -> b.calculation2("123"));
        }
    
        @Test
        void numericValidationRejectsUnbalancedUnknownAndIncompleteExpressions() {
            BracketsCalculation2 b = BracketsCalculation2.getInstance("calcnumbervalidation");
            for (String s : new String[]{"0", "2!", "(2+3)", "1.5 + 2!  ", "2*-3", "2*+3", "((2)+3)", "2!!+3"})
                assertDoesNotThrow(() -> b.check(s), s);
            for (String s : new String[]{"", "1+", "(1+", "1@2", "2**3", "(2+3", "2+3)", "((2)"})
                assertThrows(WrongFormat.class, () -> b.check(s), s);
            assertTrue(assertThrows(WrongFormat.class, () -> b.check("((2)")).getMessage().contains("[1]"));
            assertThrows(IndexOutOfBoundsException.class, () -> b.check("  "));
        }
    
        @Test
        void explanationContainsResultsOnlyWhenRequested() {
            PrefixExpressionOperation p = PrefixExpressionOperation.getInstance("calcexplanation");
            LogResults with = p.explain("2+3*4+5", true, true);
            LogResults without = p.explain("2+3*4+5", false, false);
            value(19, with);
            value(19, without);
            assertFalse(with.isNameJoin());
            assertTrue(with.getPrefix().contains("2+3*4+5+0"));
            assertTrue(containsResult(with));
            assertFalse(containsResult(without));
            assertTrue(allCalculationNodesHavePrefix(with));
            BracketsCalculation2 brackets = BracketsCalculation2.getInstance("calclogstructure");
            LogResults tree = brackets.explain("2+(3*(4+1))", true);
            assertEquals(2, tree.size());
            assertTrue(tree.getPrefix().contains("2+(3*(4+1))"));
            LogResults inner = (LogResults) tree.values().iterator().next();
            assertEquals(2, inner.size());
            assertTrue(inner.getPrefix().contains("3*(4+1)"));
            value(15, inner);
            value(7, p.explain("7", false));
        }
    
        private static boolean containsResult(Map<?, ?> map) {
            if (map.containsKey("result")) return true;
            for (Object x : map.values()) if (x instanceof Map<?, ?> child && containsResult(child)) return true;
            return false;
        }
    
        private static boolean allCalculationNodesHavePrefix(Map<?, ?> map) {
            for (Object x : map.values()) {
                if (x instanceof top.lingyuzhao.varFormatter.utils.DataObj node) {
                    if (node.getName().endsWith("_计算") && (node.getPrefix() == null || !node.getPrefix().contains("(\""))) return false;
                    if (!allCalculationNodesHavePrefix(node)) return false;
                }
            }
            return true;
        }
    
        @Test
        void booleanComparisonUsesIndependentSidesAndSelectedCalculator() {
            BooleanCalculation2 b = BooleanCalculation2.getInstance("calcboolean");
            assertEquals("calcboolean", b.getName());
            assertEquals("2>1", b.formatStr(" 2 >\n1 "));
            CalculationBooleanResults r = b.calculation(" (2+3) * 2 >= 7+2 ");
            assertTrue(r.getResult());
            assertEquals(10, r.getLeft()); assertEquals(9, r.getRight());
            assertEquals("calcboolean", r.getCalculationSourceName());
            assertTrue(r.getResultLayers() > 0);
            assertFalse(b.calculation("2>3", false).getResult());
            b.setCalculation(PrefixExpressionOperation.getInstance("calcbooleanprefix"));
            assertTrue(b.calculation("2+3=5").getResult());
            b.setCalculation(Mathematical_Expression.bracketsCalculation2);
            assertInstanceOf(BracketsCalculation2.class, b.numberCalculation);
            assertTrue(b.calculation("(2+3)==5").getResult());
            for (String s : new String[]{"null==null", "null==2", "2==null", "1+2<4"}) assertDoesNotThrow(() -> b.check(s));
            for (String s : new String[]{"2", "1<2<3", "x<2", "2<x"}) assertThrows(WrongFormat.class, () -> b.check(s));
            assertTrue(assertThrows(WrongFormat.class, () -> b.check("1<2<3")).getMessage().contains("[2]"));
            for (String s : new String[]{"null==null", "NULL!=2", "2==NULL"}) {
                AbnormalOperation badNull = assertThrows(AbnormalOperation.class, () -> b.calculation(s));
                assertTrue(badNull.getMessage().contains("Bad comparison operator => " + s));
            }
            assertTrue(assertThrows(AbnormalOperation.class, () -> b.calculation(" NULL != 2 ")).getMessage().contains("Bad comparison operator => NULL!=2"));
            assertThrows(UnsupportedOperationException.class, () -> b.explain("1<2", true));
        }
    
        @Test
        void cumulativeExpandsInclusiveSequenceAndHandlesFormattedInputs() {
            CumulativeCalculation c = CumulativeCalculation.getInstance("calccumulative");
            assertDoesNotThrow(() -> c.check("n[1,3,1](n*2)"));
            assertThrows(WrongFormat.class, () -> c.check("n[1,3](n*2)"));
            assertThrows(WrongFormat.class, () -> c.check("n*2"));
            assertThrows(WrongFormat.class, () -> c.check("n[1,3,1](n@2)"));
            assertEquals("((1.0*2))+((2.0*2))+((3.0*2))", c.formatStr("n[1,3,1](n*2)"));
            assertEquals("(2.0)", c.formatStr("n[2,2,1]n"));
            assertEquals("(1.073741824E9)", c.formatStr("n[1073741824,1073741824,1]n"));
            value(12, c.calculation("n[1,3,1](n*2)", true));
            value(9, c.calculation("(1)+(3)+(5)", false));
            value(6, c.compile("n[1,3,1]n", true).calculation(true));
            value(6, c.compile("1+2+3", false).calculation(true));
            value(6, c.compileBigDecimal("n[1,3,1]n", true).calculationBigDecimals(true));
            value(6, c.compileBigDecimal("1+2+3", false).calculationBigDecimals(true));
            value(6, c.explain("n[1,3,1]n", true));
            value(6, c.explain("1+2+3", false));
            assertThrows(IndexOutOfBoundsException.class, () -> c.formatStr("n[3,1,1]n"));
        }
    
        @Test
        void intervalSumSharesOnlyCheckedExpressionAndSupportsSteps() throws WrongFormat {
            FastSumOfIntervalsBrackets s = FastSumOfIntervalsBrackets.getInstance("calcsuminterval");
            s.clearCache(); s.setStartSharedPool(true); s.step = 1;
            assertTrue(s.isStartSharedPool());
            assertFalse(s.isCache("1,4"));
            assertEquals("1,4", s.formatStr("  1,4  "));
            s.check("1,4"); s.check("1,4");
            assertTrue(s.isCache("1,4"));
            CalculationNumberResults first = s.calculation("1,4");
            value(10, first);
            assertSame(first, s.calculation("1,4", false));
            assertEquals("calcsuminterval", first.getCalculationSourceName());
            s.clearCache();
            assertFalse(s.isCache("1,4"));
            value(14, s.calculation("2,5"));
            s.setStartSharedPool(false);
            assertFalse(s.isStartSharedPool());
            s.check("3,3");
            assertFalse(s.isCache("3,3"));
            value(3, s.calculation("3,3"));
            s.step = 2;
            value(9, s.calculation("1", "5"));
            CalculationNumberResults combined = s.calculation(new CalculationNumberResults(2, 1, "a"), new CalculationNumberResults(3, 5, "b"));
            value(9, combined); assertEquals(5, combined.getResultLayers());
            value(9, s.compile("1,5", true).calculation(true));
            value(7.5, s.compileBigDecimal("1,5", false).calculationBigDecimals(true));
            for (String bad : new String[]{"1", "1,2,3", "1,2+", "x,2"}) assertThrows(WrongFormat.class, () -> s.check(bad));
        }
    
        @Test
        void intervalMultiplicationUsesDefaultAndCustomStep() {
            FastMultiplyOfIntervalsBrackets m = FastMultiplyOfIntervalsBrackets.getInstance("calcmultiplyinterval");
            assertEquals(2, m.step);
            value(15, m.calculation("1,5"));
            value(15, m.compile("1,5", true).calculation(true));
            value(15, m.compileBigDecimal("1,5", false).calculationBigDecimals(true));
            m.step = 1;
            CalculationNumberResults r = m.calculation(new CalculationNumberResults(3, 2, "a"), new CalculationNumberResults(4, 4, "b"));
            value(24, r); assertEquals(7, r.getResultLayers()); assertEquals("calcmultiplyinterval", r.getCalculationSourceName());
        }
    
        @Test
        void factoriesReuseRightTypesAndRejectNameCollisions() {
            PrefixExpressionOperation p = PrefixExpressionOperation.getInstance("calcfactoryprefix");
            assertNotNull(p);
            assertSame(p, PrefixExpressionOperation.getInstance("calcfactoryprefix"));
            BracketsCalculation2 b = BracketsCalculation2.getInstance("calcfactorybracket");
            assertNotNull(b);
            assertSame(b, BracketsCalculation2.getInstance("calcfactorybracket"));
            BooleanCalculation2 bool = BooleanCalculation2.getInstance("calcfactoryboolean");
            assertNotNull(bool);
            assertSame(bool, BooleanCalculation2.getInstance("calcfactoryboolean"));
            CumulativeCalculation c = CumulativeCalculation.getInstance("calcfactorycumulative");
            assertNotNull(c);
            assertSame(c, CumulativeCalculation.getInstance("calcfactorycumulative"));
            FastSumOfIntervalsBrackets sum = FastSumOfIntervalsBrackets.getInstance("calcfactorysum");
            assertNotNull(sum);
            assertSame(sum, FastSumOfIntervalsBrackets.getInstance("calcfactorysum"));
            FastMultiplyOfIntervalsBrackets multiply = FastMultiplyOfIntervalsBrackets.getInstance("calcfactorymultiply");
            assertNotNull(multiply);
            assertSame(multiply, FastMultiplyOfIntervalsBrackets.getInstance("calcfactorymultiply"));
            ComplexCalculation complex = ComplexCalculation.getInstance("calcfactorycomplex");
            assertNotNull(complex);
            assertSame(complex, ComplexCalculation.getInstance("calcfactorycomplex"));
            assertThrows(ExtractException.class, () -> PrefixExpressionOperation.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> BracketsCalculation2.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> CumulativeCalculation.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> FastSumOfIntervalsBrackets.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> FastMultiplyOfIntervalsBrackets.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> ComplexCalculation.getInstance("calcfactoryboolean"));
            assertThrows(ExtractException.class, () -> BooleanCalculation2.getInstance("calcfactoryprefix"));
        }
    
        @Test
        void complexExpressionsCompileBothPartsAndPreserveObservableSignBehavior() {
            ComplexCalculation c = ComplexCalculation.getInstance("calccomplex");
            assertEquals("(2+3)+4i", c.formatStr(" (2 + 3) + 4i "));
            assertDoesNotThrow(() -> c.check("(2+3)+(4-1)i"));
            assertThrows(WrongFormat.class, () -> c.check("2+3"));
            assertThrows(WrongFormat.class, () -> c.check("(2@3)+4i"));
            assertThrows(WrongFormat.class, () -> c.check("2+(3@4)i"));
            assertThrows(RuntimeException.class, () -> c.check("23i"));
            assertThrows(RuntimeException.class, () -> ComplexExpression.compile("23i", "invalid"));
            assertThrows(UnsupportedOperationException.class, () -> c.explain("2+3", true));
            assertThrows(RuntimeException.class, () -> c.explain("23i", true));
            ComplexExpression e = c.compile(" (2+3) + (4-1)i ", true);
            assertTrue(e.isBigDecimal()); assertTrue(e.isUnBigDecimal());
            assertEquals("(2+3)", e.getFunctionExpression1().getExpressionStr());
            assertEquals("+(4-1)", e.getFunctionExpression2().getExpressionStr());
            e.convertToMultiPrecisionSupported();
            complexValue(5, 3, e.calculation(true));
            complexValue(5, 3, e.calculationBigDecimals(true));
            assertEquals(2, e.calculation(true).getResultLayers());
            assertEquals(2, e.calculationBigDecimals(true).getResultLayers());
            complexValue(5, 3, e.calculationCache(true));
            complexValue(5, 3, e.calculationBigDecimalsCache(true));
            complexValue(5, 3, c.compileBigDecimal(" (2+3)+(4-1)i ", true).calculationBigDecimals(true));
            complexValue(5, 3, c.compileBigDecimal("(2+3)+(4-1)i", false).calculation(true));
            complexValue(2, 3, c.compile("2-3i", false).calculation(true));
            complexValue(2, 3, c.compile("2-3i", false).calculationBigDecimals(true));
            complexValue(2, 3, (CalculationComplexResults) c.calculation("2+3i", false));
            Mathematical_Expression.Options.setUseBigDecimal(true);
            complexValue(2, 3, (CalculationComplexResults) c.calculation("2+3i", true));
            LogResults logs = c.explain("(2+3)+(4-1)i", true);
            complexValue(5, 3, (CalculationComplexResults) logs.getResult());
            assertFalse(logs.isNameJoin()); assertTrue(logs.containsKey("result"));
            assertTrue(logs.getPrefix().contains("(2+3)+(4-1)i"));
            complexValue(2, -3, (CalculationComplexResults) c.explain("2-3i", false).getResult());
        }
    
        private static void complexValue(double real, double imaginary, CalculationComplexResults result) {
            assertEquals(real, result.toComplexNumber().getReal(), 1e-10);
            assertEquals(imaginary, result.toComplexNumber().getImaginary(), 1e-10);
        }
    
        @Test
        void complexWrapperForwardsPrecisionCapabilitiesOfSuppliedExpressions() {
            class PrecisionFunction extends FunctionExpression {
                final PrefixExpression delegate;
                PrecisionFunction(PrefixExpression delegate) {
                    super("2", "precisionfunction", new Stack<>(), new Stack<>(), new Stack<>());
                    this.delegate = delegate;
                }
                @Override public boolean isBigDecimal() { return delegate.isBigDecimal(); }
                @Override public boolean isUnBigDecimal() { return delegate.isUnBigDecimal(); }
                @Override public void convertToMultiPrecisionSupported() { delegate.convertToMultiPrecisionSupported(); }
            }
            PrecisionFunction left = new PrecisionFunction(PrefixExpression.compileBigDecimal("2", "decimalleft"));
            PrecisionFunction right = new PrecisionFunction(PrefixExpression.compile("3", "doubleright"));
            ComplexExpression e = new ComplexExpression("2+3i", "manualcomplex", left, right, '+');
            assertTrue(e.isBigDecimal());
            assertFalse(e.isUnBigDecimal());
            assertFalse(new ComplexExpression("3+2i", "reversedcomplex", right, left, '+').isBigDecimal());
            e.convertToMultiPrecisionSupported();
            assertTrue(left.isUnBigDecimal());
            assertTrue(right.isBigDecimal());
            assertTrue(e.isUnBigDecimal());
        }
    
        @Test
        void subCompilationDelegatesOnlyWhenRequiredAndHonorsCopyFlag() {
            class Nested extends SubCompileExpression {
                private final boolean simple;
                boolean seenCopy;
                int normalCalls;
                int decimalCalls;
                Nested(boolean simple) { super("2+3", "nested"); this.simple = simple; }
                public NameExpression subCompile(boolean copy) { normalCalls++; seenCopy = copy; return PrefixExpression.compile("4+6", "deep"); }
                public NameExpression subCompileBigDecimals(boolean copy) { decimalCalls++; seenCopy = copy; return PrefixExpression.compileBigDecimal("8+3", "deep"); }
                public boolean notNeedSubCompile() { return simple; }
                public boolean isBigDecimal() { return true; }
                public boolean isUnBigDecimal() { return true; }
                public void convertToMultiPrecisionSupported() { }
                void replace(String s) { getExpressionStrBuilder().setLength(0); getExpressionStrBuilder().append(s); }
            }
            Nested plain = new Nested(true);
            value(5, plain.calculation(true)); value(5, plain.calculationBigDecimals(false));
            assertEquals(0, plain.normalCalls); assertEquals(0, plain.decimalCalls);
            plain.replace("7+2"); assertEquals("7+2", plain.getExpressionStr()); value(9, plain.calculation(false));
            Nested deep = new Nested(false);
            value(10, deep.calculation(true)); assertTrue(deep.seenCopy); assertEquals(1, deep.normalCalls);
            value(11, deep.calculationBigDecimals(false)); assertFalse(deep.seenCopy); assertEquals(1, deep.decimalCalls);
        }
    }

    @Nested
    class FunctionAndJvmTest {
        private final Map<String, Function> previousFunctions = new HashMap<>();
        private final Map<String, String> previousJvmMethods = new LinkedHashMap<>();
    
        @BeforeEach
        void prepareFunctions() {
            for (String name : new ArrayList<>(CalculationManagement.getFunctionMap())) {
                previousFunctions.put(name, CalculationManagement.getFunctionByName(name));
            }
            CalculationManagement.getFunctionMap().clear();
            previousJvmMethods.putAll(JvmExpressionFunction.getRegisteredMethods());
            JvmExpressionFunction.getRegisteredMethods().clear();
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            resetSharedFunctionCaches();
            CalculationManagement.register(FunctionPackage.MATH);
        }
    
        @AfterEach
        void restoreFunctions() {
            CalculationManagement.getFunctionMap().clear();
            previousFunctions.values().forEach(CalculationManagement::register);
            JvmExpressionFunction.getRegisteredMethods().clear();
            JvmExpressionFunction.getRegisteredMethods().putAll(previousJvmMethods);
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
            resetSharedFunctionCaches();
            for (String name : List.of("agentFunctionFactory", "agentFunctionSecond", "agentJvmFactory", "agentWrongType")) {
                CalculationManagement.unregister(name);
            }
        }
    
        private static void resetSharedFunctionCaches() {
            ComplexCalculation.FUNCTION_FORMULA_CALCULATION_2.clearCache();
            ComplexCalculation.FUNCTION_FORMULA_CALCULATION_2.setStartSharedPool(true);
            for (String name : List.of(CalculationManagement.FUNCTION_FORMULA_CALCULATION_2_NAME,
                    Mathematical_Expression.functionFormulaCalculation2.name())) {
                Calculation calculation = CalculationManagement.getCalculationByName(name);
                if (calculation instanceof FunctionFormulaCalculation2 functions) {
                    functions.clearCache();
                    functions.setStartSharedPool(true);
                }
            }
        }
    
        private static ManyToOneNumberFunction builtin(FunctionPackage pack, String name) {
            return (ManyToOneNumberFunction) pack.getFunctionByNameNoCheck(name);
        }
    
        private static double number(CalculationResults result) {
            return ((Number) result.getResult()).doubleValue();
        }
    
        @Test
        void mathPackageFunctionsProduceDistinctResultsAndExposeMetadata() {
            assertEquals("Math", FunctionPackage.MATH.getName());
            assertEquals("Date", FunctionPackage.DATE.getName());
            assertEquals("Branch", FunctionPackage.BRANCH.getName());
            assertSame(FunctionPackage.MATH, FunctionPackage.valueOf("MATH"));
            assertEquals(3, FunctionPackage.values().length);
            assertEquals(12, FunctionPackage.MATH.getFunctionMap().size());
            assertNull(FunctionPackage.MATH.getFunctionByNameNoCheck("absent"));
            assertSame(builtin(FunctionPackage.MATH, "sum"), FunctionPackage.MATH.getFunctionMap().get("sum"));
            assertEquals(0, builtin(FunctionPackage.MATH, "sum").run());
            assertEquals(7.5, builtin(FunctionPackage.MATH, "sum").run(2, -1, 6.5));
            assertEquals(32, builtin(FunctionPackage.MATH, "pow").run(2, 5));
            assertEquals(3, builtin(FunctionPackage.MATH, "sqrt").run(9));
            assertEquals(120, builtin(FunctionPackage.MATH, "factorial").run(5));
            assertEquals(4, builtin(FunctionPackage.MATH, "avg").run(2, 4, 6));
            assertTrue(Double.isNaN(builtin(FunctionPackage.MATH, "avg").run()));
            double angle = .7;
            assertEquals(Math.sin(angle), builtin(FunctionPackage.MATH, "sin").run(angle));
            assertEquals(Math.cos(angle), builtin(FunctionPackage.MATH, "cos").run(angle));
            assertEquals(Math.tan(angle), builtin(FunctionPackage.MATH, "tan").run(angle));
            assertEquals(1 / Math.tan(angle), builtin(FunctionPackage.MATH, "cot").run(angle));
            assertEquals(1 / Math.cos(angle), builtin(FunctionPackage.MATH, "sec").run(angle));
            assertEquals(1 / Math.sin(angle), builtin(FunctionPackage.MATH, "csc").run(angle));
        }
    
        @Test
        void mathPackageReportsMissingDependenciesAndUsesReplacementFunctions() {
            ManyToOneNumberFunction avg = builtin(FunctionPackage.MATH, "avg");
            ManyToOneNumberFunction module = builtin(FunctionPackage.MATH, "moduleLength");
            CalculationManagement.unregisterF("sum");
            UnsupportedOperationException sum = assertThrows(UnsupportedOperationException.class, () -> avg.run(1, 2));
            assertTrue(sum.getMessage().contains("sum"));
            assertTrue(sum.getMessage().contains("函数位于 Math 函数包中"));
            CalculationManagement.unregisterF("pow");
            assertTrue(assertThrows(UnsupportedOperationException.class, () -> module.run(3, 4)).getMessage().contains("pow"));
            CalculationManagement.register(builtin(FunctionPackage.MATH, "pow"));
            CalculationManagement.unregisterF("sqrt");
            assertTrue(assertThrows(UnsupportedOperationException.class, () -> module.run(3, 4)).getMessage().contains("sqrt"));
            CalculationManagement.register(builtin(FunctionPackage.MATH, "sqrt"));
            assertEquals(0, module.run());
            // Current module implementation supplies one argument to the two-argument pow function.
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> module.run(3, 4));
            CalculationManagement.unregisterF("pow");
            CalculationManagement.register(new ManyToOneNumberFunction("pow") {
                public double run(double... values) { return values[0] * values[0]; }
            });
            assertEquals(5, module.run(3, 4));
        }
    
        @Test
        void datePackageConvertsBothDirections() {
            String[] names = {"S", "Min", "Hour", "Day", "Week", "Month", "Year"};
            String[] reverse = {"s", "min", "hour", "day", "week", "month", "year"};
            double[] scales = {1000, 60000, 3600000, 86400000, 604800000, 2592000000L, 31536000000L};
            assertEquals(14, FunctionPackage.DATE.getFunctionMap().size());
            for (int i = 0; i < names.length; i++) {
                assertEquals(2.5, builtin(FunctionPackage.DATE, "msTo" + names[i]).run(scales[i] * 2.5));
                assertEquals(scales[i] * 2.5, builtin(FunctionPackage.DATE, reverse[i] + "ToMs").run(2.5));
            }
        }
    
        @Test
        void branchFunctionsCheckArityAndBothOutcomesAtEqualityBoundary() {
            String[] names = {"ifEq", "ifGt", "ifLt", "ifGe", "ifLe"};
            boolean[][] outcomes = {{false, true, false}, {false, false, true}, {true, false, false}, {false, true, true}, {true, true, false}};
            for (int i = 0; i < names.length; i++) {
                ManyToOneNumberFunction function = builtin(FunctionPackage.BRANCH, names[i]);
                assertThrows(UnsupportedOperationException.class, () -> function.run(1, 2, 3));
                assertThrows(UnsupportedOperationException.class, () -> function.run(1, 2, 3, 4, 5));
                for (int comparison = 0; comparison < 3; comparison++) {
                    assertEquals(outcomes[i][comparison] ? 17 : -9, function.run(comparison, 1, 17, -9));
                }
            }
        }
    
        @Test
        void expressionFunctionsSubstituteRepeatedReorderedAndUnusedParameters() throws Exception {
            ExpressionFunction function = ExpressionFunction.parse("agentLinear(x,y)=y+x*2+x");
            assertEquals("agentLinear", function.getName());
            assertEquals(2, function.getParamSize());
            assertEquals("agentLinear(x,y)=y+x*2+x", function.toString());
            assertEquals("4.0+3.0*2+3.0", function.explain(3, 4));
            assertEquals(13, function.run(3, 4));
            assertThrows(UnsupportedOperationException.class, () -> function.explain(3));
            assertThrows(UnsupportedOperationException.class, () -> function.explain(3, 4, 5));
            ExpressionFunction constant = ExpressionFunction.parse("agentConstant(x)=7");
            assertEquals("7", constant.explain(123));
            assertEquals(7, constant.run(123));
            ExpressionFunction nested = ExpressionFunction.parse("agentNested(x)=sum(x,2)");
            assertEquals(5, nested.run(3));
        }
    
        @Test
        void expressionFunctionsRejectMalformedSignaturesAndUnknownParameters() {
            assertThrows(UnsupportedOperationException.class, () -> ExpressionFunction.parse("agentMissing(x)"));
            assertThrows(UnsupportedOperationException.class, () -> ExpressionFunction.parse("agentTooMany(x)=x=2"));
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("broken)=2"));
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("broken=2"));
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("agentUnknown(x)=y"));
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("agentUnknown(x)=y+2"));
            assertThrows(ExtractException.class, () -> ExpressionFunction.parse("bad1(x)=x"));
            assertThrows(WrongFormat.class, () -> ExpressionFunction.parse("agentMissingClose(x=x"));
        }
    
        @Test
        void expressionFunctionSerializationRoundTripPreservesCalculation() throws Exception {
            ExpressionFunction function = ExpressionFunction.parse("agentSerialized(x)=x*3+2");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { function.saveTo(output); }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                ManyToOneNumberFunction restored = ExpressionFunction.readFrom(input);
                assertNotSame(function, restored);
                assertEquals(function.toString(), restored.toString());
                assertEquals(17, restored.run(5));
            }
        }
    
        @Test
        void functionFactoriesReuseInstancesAndRejectCollisions() {
            FunctionFormulaCalculation first = FunctionFormulaCalculation.getInstance("agentFunctionFactory");
            assertSame(first, FunctionFormulaCalculation.getInstance("agentFunctionFactory"));
            assertEquals("agentFunctionFactory", first.getName());
            FunctionFormulaCalculation2 second = FunctionFormulaCalculation2.getInstance("agentFunctionSecond");
            assertSame(second, FunctionFormulaCalculation2.getInstance("agentFunctionSecond"));
            assertEquals("agentFunctionSecond", second.getName());
            JvmCalculation jvm = JvmCalculation.getInstance("agentJvmFactory");
            assertSame(jvm, JvmCalculation.getInstance("agentJvmFactory"));
            assertEquals("agentJvmFactory", jvm.getName());
            PrefixExpressionOperation.getInstance("agentWrongType");
            assertThrows(ExtractException.class, () -> FunctionFormulaCalculation.getInstance("agentWrongType"));
            assertThrows(ExtractException.class, () -> FunctionFormulaCalculation2.getInstance("agentWrongType"));
            assertThrows(ExtractException.class, () -> JvmCalculation.getInstance("agentWrongType"));
            assertEquals("functionFormulaCalculation_temp", new FunctionFormulaCalculation().getName());
            assertEquals("functionFormulaCalculation2_temp", new FunctionFormulaCalculation2().getName());
        }
    
        @Test
        void legacyFunctionCalculatorEvaluatesIntegerFractionalAndNestedArguments() throws Exception {
            FunctionFormulaCalculation calculation = new FunctionFormulaCalculation("agentLegacy");
            assertEquals("sqrt(9)+2", calculation.formatStr(" sqrt ( 9 ) \n+ 2\t"));
            assertEquals(5, number(calculation.calculation("sqrt(9)+2")));
            assertEquals(3.5, number(calculation.calculation("sqrt(2.25) + 2", false)));
            assertEquals(5, number(calculation.calculation("sqrt((4+5))+2", true)));
            assertEquals(10, number(calculation.calculation("(2+3)*2", true)));
            assertDoesNotThrow(() -> calculation.check("sqrt(12) + 3"));
            assertDoesNotThrow(() -> calculation.check("3 + 4"));
            assertThrows(WrongFormat.class, () -> calculation.check("sqrt(12"));
            assertThrows(WrongFormat.class, () -> calculation.check("sqrt((12)"));
            assertThrows(ExtractException.class, () -> calculation.calculation("agentAbsent(12)"));
        }
    
        @Test
        void parameterExtractionPreservesArgumentRangesAndOuterFormula() {
            String expression = " 2+Az(3,(4+5))+zA(6)+7 ";
            Stack<Integer> start = new Stack<>(), end = new Stack<>();
            Stack<String> names = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction(expression, start, end, names);
            assertEquals(List.of("Az", "zA"), names);
            assertEquals(2, start.size());
            assertEquals("3,(4+5)", expression.substring(start.get(0), end.get(0)));
            assertEquals("6", expression.substring(start.get(1), end.get(1)));
            Stack<Integer> startCopy = new Stack<>(), endCopy = new Stack<>();
            Stack<String> nameCopy = new Stack<>();
            StringBuilder outer = new StringBuilder();
            new FunctionFormulaCalculation2("agentExtract").FunctionParameterExtraction(expression, startCopy, endCopy, nameCopy, outer);
            assertEquals(start, startCopy);
            assertEquals(end, endCopy);
            assertEquals(names, nameCopy);
            assertEquals("2+0+0+7", outer.toString());
        }
    
        @Test
        void secondCalculatorCachesCheckedExpressionsAndResultsAndClearsBoth() throws Exception {
            FunctionFormulaCalculation2 calculation = new FunctionFormulaCalculation2("agentCached");
            String expression = "sum(2,3)+4";
            assertTrue(calculation.isStartSharedPool());
            assertFalse(calculation.isCache(expression));
            calculation.check(expression);
            assertTrue(calculation.isCache(expression));
            calculation.check(expression);
            CalculationNumberResults first = calculation.calculation(expression, true);
            assertEquals(9, first.getResult());
            assertSame(first, calculation.calculation(expression, false));
            calculation.clearCache();
            assertFalse(calculation.isCache(expression));
            CalculationNumberResults fresh = calculation.calculation(expression, false);
            assertNotSame(first, fresh);
            assertEquals(9, fresh.getResult());
            calculation.setStartSharedPool(false);
            assertFalse(calculation.isStartSharedPool());
            calculation.check("sum(5,6)+7");
            assertFalse(calculation.isCache("sum(5,6)+7"));
            assertEquals(18, calculation.calculation("sum(5,6)+7", true).getResult());
            assertEquals(42.5, calculation.calculation("42.5", false).getResult());
            Mathematical_Expression.Options.setUseBigDecimal(true);
            assertEquals(.3, calculation.calculation("sum(0.1,0.2)", true).getResult(), 1e-12);
            assertEquals(5, number(calculation.compileBigDecimal("sum(2,3)", true).calculationBigDecimalsCache(true)));
            assertThrows(WrongFormat.class, () -> calculation.check("sum(2,3"));
            assertThrows(WrongFormat.class, () -> calculation.check("sum(2+,3)"));
        }
    
        @Test
        void secondCalculatorExplainsNestedRepeatedAndPlainExpressions() {
            FunctionFormulaCalculation2 calculation = new FunctionFormulaCalculation2("agentExplain");
            LogResults explanation = calculation.explain("sum(1+2,3)+sum(4,5)", true);
            assertEquals(15, ((Double) explanation.getResult()).doubleValue());
            assertFalse(explanation.isEmpty());
            assertEquals(7, ((Double) calculation.explain("3+4", false, false, "agentTrace").getResult()).doubleValue());
            assertEquals(8, ((Double) calculation.explain("sum(sum(1,2),5)", true).getResult()).doubleValue());
        }
    
        @Test
        void compiledFunctionsExposeCopiesAndPermitSameLengthFunctionReplacement() {
            FunctionExpression expression = FunctionExpression.compile("sum(2,3)+4", "agentCompiled");
            assertTrue(expression.isBigDecimal());
            assertTrue(expression.isUnBigDecimal());
            expression.convertToMultiPrecisionSupported();
            assertFalse(expression.notNeedSubCompile());
            assertEquals(List.of("sum"), expression.getFunNames());
            expression.getFunNames().clear();
            assertEquals(List.of("sum"), expression.getFunNames());
            assertNotSame(expression.getStart(false), expression.getStart(true));
            assertSame(expression.getStart(false), expression.getStart(false));
            assertNotSame(expression.getEnd(false), expression.getEnd(true));
            assertSame(expression.getEnd(false), expression.getEnd(false));
            assertSame(expression.getFunNames(false), expression.getFunNames(false));
            expression.setFunName(0, "pow");
            assertEquals("pow(2,3)+4", expression.getExpressionStr());
            assertEquals(List.of("pow"), expression.getFunNames());
            assertEquals(12, number(expression.subCompile(true).calculation(true)));
            CalculationNumberResults result = expression.calculationCache(true);
            assertEquals(12, result.getResult());
            assertEquals("agentCompiled", result.getCalculationSourceName());
            assertSame(result, expression.calculationCache(true));
        }
    
        @Test
        void compiledFunctionsSupportBigDecimalAndConsumeTheirStacks() {
            FunctionExpression expression = FunctionExpression.compile("sum(2,3)", "agentBigFunctions");
            assertEquals(5, number(expression.subCompileBigDecimals(true).calculationBigDecimalsCache(true)));
            CalculationNumberResults result = expression.calculationBigDecimalsCache(false);
            assertEquals(5, result.getResult());
            assertEquals("agentBigFunctions", result.getCalculationSourceName());
            assertFalse(expression.isAvailable());
            assertTrue(expression.notNeedSubCompile());
            assertThrows(UnsupportedOperationException.class, () -> expression.getStart(true));
            assertThrows(UnsupportedOperationException.class, () -> expression.getEnd(false));
            assertThrows(UnsupportedOperationException.class, expression::getFunNames);
            FunctionExpression consumed = FunctionExpression.compile("sum(7,8)", "agentConsumed");
            assertEquals(15, consumed.calculationCache(false).getResult());
            assertFalse(consumed.isAvailable());
        }
    
        @Test
        void noFunctionExpressionsSkipSubcompilation() {
            FunctionExpression plain = FunctionExpression.compile("2+3", "agentPlain");
            assertTrue(plain.notNeedSubCompile());
            assertEquals(5, number(plain.calculation(true)));
            assertEquals(5, number(plain.calculationBigDecimals(true)));
            assertThrows(UnsupportedOperationException.class, () -> plain.subCompile(true));
            assertThrows(UnsupportedOperationException.class, () -> plain.subCompileBigDecimals(true));
            assertEquals("42", FunctionExpression.subCompile("42", new Stack<>(), new Stack<>(), new Stack<>()).toString());
            assertEquals("2+3", FunctionExpression.subCompile("2+3", new Stack<>(), new Stack<>(), new Stack<>()).toString());
        }
    
        @Test
        void jvmRegistrationBuildsExactSourcesAndValidatesSignatures() {
            assertEquals("public static double agentJvm(double x, double y) { return x+y; }", JvmExpressionFunction.convertToStaticMethodSource("agentJvm(x, y)=x+y"));
            assertEquals("public static double agentConst() { return 7; }", JvmExpressionFunction.convertToStaticMethodSource("agentConst()=7"));
            JvmExpressionFunction.registerFunction("agentJvm(x)=x+3");
            assertEquals("public static double agentJvm(double x) { return x+3; }", JvmExpressionFunction.getRegisteredMethods().get("double agentJvm"));
            JvmExpressionFunction.registerJvmFunction("  public static double agentRaw(double x) { return x*2; }  ");
            assertEquals("  public static double agentRaw(double x) { return x*2; }  ", JvmExpressionFunction.getRegisteredMethods().get("double agentRaw"));
            assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.registerJvmFunction("broken"));
            assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.registerJvmFunction("private double bad() { return 1; }"));
            for (String expression : List.of("broken(x)", "broken(x)=x=2", "broken=x", "broken(x=x", "broken)x=x", "broken1(x)=x")) {
                assertThrows(IllegalArgumentException.class, () -> JvmExpressionFunction.convertToStaticMethodSource(expression), expression);
            }
        }
    
        private static JvmExpressionFunction jvmFunction(String name, String[] params, String expression, ManyToOneNumberFunction delegate) throws Exception {
            Constructor<JvmExpressionFunction> constructor = JvmExpressionFunction.class.getDeclaredConstructor(ParsedSignature.class, ManyToOneNumberFunction.class);
            constructor.setAccessible(true);
            return constructor.newInstance(new ParsedSignature(name, params, expression), delegate);
        }
    
        private static ManyToOneNumberFunction sumDelegate() {
            return new ManyToOneNumberFunction("agentDelegate") {
                public double run(double... values) { return Arrays.stream(values).sum(); }
            };
        }
    
        @Test
        void jvmRuntimeChecksArityExplainsBothOverloadsAndResetsMatcher() throws Exception {
            JvmExpressionFunction function = jvmFunction("agentRuntime", new String[]{"x", "y"}, "x+y+x", sumDelegate());
            assertEquals("agentRuntime(x,y)=x+y+x", function.toString());
            assertEquals(2, function.getParamSize());
            assertEquals(7, function.run(3, 4));
            assertEquals("3.0+4.0+3.0", function.explain(new double[]{3, 4}));
            assertEquals("5.0+6.0+5.0", function.explain(new double[]{5, 6}));
            assertEquals("left+right+left", function.explain(new Object[]{"left", "right"}));
            assertEquals("null+3+null", function.explain(new Object[]{null, 3}));
            assertThrows(IllegalArgumentException.class, () -> function.run(1));
            assertThrows(IllegalArgumentException.class, () -> function.explain(new double[]{1}));
            assertThrows(IllegalArgumentException.class, () -> function.explain(new Object[]{1}));
            JvmExpressionFunction noParams = jvmFunction("agentZero", new String[0], "7", sumDelegate());
            assertEquals("7", noParams.explain(new double[0]));
            assertEquals("7", noParams.explain(new Object[0]));
            assertEquals(0, noParams.run());
        }
    
        @Test
        void parsedSignatureExposesInputsAndStableExpression() {
            String[] names = {"x", "y"};
            ParsedSignature signature = new ParsedSignature("agentSignature", names, "x-y");
            assertEquals("agentSignature", signature.getName());
            assertSame(names, signature.getParamNames());
            assertEquals("x-y", signature.getExpression());
            assertEquals("agentSignature(x,y)=x-y", signature.toString());
        }
    
        @Test
        void jvmExpressionExposesAndClonesOperandsAndComputesWithDelegate() throws Exception {
            JvmExpressionFunction function = jvmFunction("agentOperands", new String[]{"x", "y"}, "x+y", sumDelegate());
            JvmExpression expression = new JvmExpression("agentJvmExpression", new double[]{2, 3}, function, "x+y");
            assertEquals(2, expression.getLength());
            assertEquals(2, expression.getParamNumber(0));
            assertArrayEquals(new double[]{2, 3}, expression.iterator().toArray());
            assertSame(function, expression.getJvmExpressionFunction());
            assertFalse(expression.isBigDecimal());
            assertTrue(expression.isUnBigDecimal());
            assertEquals("2.0+3.0", expression.getExpressionStrAndNumber());
            CalculationNumberResults result = expression.calculation(true);
            assertEquals(5, result.getResult());
            assertEquals(1, result.getResultLayers());
            assertEquals("x+y", result.getCalculationSourceName());
            JvmExpression copy = expression.cloneExpression();
            assertEquals("agentJvmExpression_clone", copy.getCalculationName());
            assertSame(function, copy.getJvmExpressionFunction());
            assertEquals("x+y", copy.getExpressionStr());
            expression.setParamNumber(0, 7);
            assertEquals(7, expression.getParamNumber(0));
            assertEquals(2, copy.getParamNumber(0));
            assertEquals(10, expression.calculation(false).getResult());
            assertEquals(5, copy.calculation(false).getResult());
            assertThrows(UnsupportedOperationException.class, expression::convertToMultiPrecisionSupported);
            assertThrows(UnsupportedOperationException.class, () -> expression.calculationBigDecimals(true));
        }
    
        @Test
        void dynamicCompilerReportsOriginalPackageMismatchAndRejectsMalformedJava() throws Exception {
            JvmExpressionFunction.registerFunction("agentAvailable(x)=x+1");
            RuntimeException failure = assertThrows(RuntimeException.class, () -> DynamicFunctionCompiler.compile("agentCompile", "x+1", new String[]{"x"}));
            assertEquals("动态函数生成失败", failure.getMessage());
            assertNotNull(failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("ManyToOneNumberFunction"));
            assertThrows(RuntimeException.class, () -> DynamicFunctionCompiler.compile("agentInvalid", "! ! !", new String[0]));
            assertThrows(RuntimeException.class, () -> JvmExpressionFunction.parse("agentParse(x)=x+1"));
            assertThrows(RuntimeException.class, () -> JvmExpressionFunction.parse(ExpressionFunction.parse("agentParseExpr(x)=x+1")));
            assertThrows(RuntimeException.class, () -> JvmExpression.compile("2+3", "agentCompilation"));
            assertThrows(RuntimeException.class, () -> JvmExpression.compile("7*8"));
            assertThrows(IllegalArgumentException.class, () -> JvmExpression.compile("Math.PI"));
        }
    
        @Test
        void jvmCalculatorRejectsBigDecimalAndPreservesFormatting() {
            JvmCalculation calculation = new JvmCalculation("agentJvmCalc");
            assertEquals(" 2 + 3 ", calculation.formatStr(" 2 + 3 "));
            assertDoesNotThrow(() -> calculation.check("any expression"));
            assertThrows(UnsupportedOperationException.class, () -> calculation.compileBigDecimal("2+3", true));
            assertThrows(RuntimeException.class, () -> calculation.compile("2+3", false));
            assertThrows(RuntimeException.class, () -> calculation.calculation("2+3", true));
        }
    
        @Test
        void inMemoryClassFilesStoreBytecodeAndLoaderRejectsMissingOrInvalidClasses() throws Exception {
            InMemoryJavaFileObject object = new InMemoryJavaFileObject("agent.Example", JavaFileObject.Kind.CLASS);
            assertEquals("mem://agent/Example.class", object.toUri().toString());
            assertArrayEquals(new byte[0], object.getBytes());
            OutputStream stream = object.openOutputStream();
            stream.write(new byte[]{1, 2, 3, 4});
            assertArrayEquals(new byte[]{1, 2, 3, 4}, object.getBytes());
            assertSame(stream, object.openOutputStream());
            byte[] first = object.getBytes();
            first[0] = 99;
            assertEquals(1, object.getBytes()[0]);
            DynamicClassLoader loader = new DynamicClassLoader();
            assertThrows(ClassNotFoundException.class, () -> loader.findClass("agent.Missing"));
            loader.addClass("agent.Example", object);
            assertThrows(ClassFormatError.class, () -> loader.findClass("agent.Example"));
        }
    
        @Test
        void dynamicClassLoaderDefinesValidIndependentClassBytes() throws Exception {
            InMemoryJavaFileObject object = new InMemoryJavaFileObject(ParsedSignature.class.getName(), JavaFileObject.Kind.CLASS);
            try (InputStream input = ParsedSignature.class.getResourceAsStream("ParsedSignature.class")) {
                assertNotNull(input);
                object.openOutputStream().write(input.readAllBytes());
            }
            DynamicClassLoader loader = new DynamicClassLoader();
            loader.addClass(ParsedSignature.class.getName(), object);
            Class<?> loaded = loader.findClass(ParsedSignature.class.getName());
            assertEquals(ParsedSignature.class.getName(), loaded.getName());
            assertSame(loader, loaded.getClassLoader());
            assertNotSame(ParsedSignature.class, loaded);
        }
    
        @Test
        void functionNameBoundariesAndInvalidOuterSymbolsAreDistinguished() throws Exception {
            for (String name : List.of("A", "Z", "a", "z")) {
                CalculationManagement.register(new ManyToOneNumberFunction(name) {
                    public double run(double... values) { return values[0]; }
                });
            }
            FunctionFormulaCalculation legacy = new FunctionFormulaCalculation("agentBoundary");
            String valid = "A(12)+Z(13)+a(14)+z(15)";
            assertEquals(54, legacy.calculation(valid).getResult());
            assertDoesNotThrow(() -> legacy.check(valid));
            for (String symbol : List.of("@", "[", "`", "{")) {
                assertThrows(WrongFormat.class, () -> legacy.check("A(12)+" + symbol + "2"));
                assertEquals(14, legacy.calculation("A(12)+" + symbol + "2").getResult());
            }
            assertThrows(WrongFormat.class, () -> legacy.check("A(1@)"));
            assertThrows(WrongFormat.class, () -> legacy.check("1+@"));
            assertTrue(assertThrows(WrongFormat.class, () -> legacy.check("A(12)+Z(13")).getMessage().endsWith("1"));
            FunctionFormulaCalculation2 current = new FunctionFormulaCalculation2("agentSecondBoundary");
            assertTrue(assertThrows(WrongFormat.class, () -> current.check("sum(12)+sum(13")).getMessage().endsWith("1"));
            String allCharacters = "A(1)+Z(2)+a(3)+z(4)+@[`{";
            Stack<Integer> start = new Stack<>(), end = new Stack<>();
            Stack<String> names = new Stack<>();
            FunctionFormulaCalculation2.FunctionParameterExtraction(allCharacters, start, end, names);
            assertEquals(List.of("A", "Z", "a", "z"), names);
            assertEquals(List.of(2, 7, 12, 17), start);
            assertEquals(List.of(3, 8, 13, 18), end);
            start.clear(); end.clear(); names.clear();
            StringBuilder outer = new StringBuilder();
            current.FunctionParameterExtraction(allCharacters, start, end, names, outer);
            assertEquals(List.of("A", "Z", "a", "z"), names);
            assertEquals("0+0+0+0+@[`{", outer.toString());
        }
    
        @Test
        void checkingCachedFormulaAvoidsReextractingAndNumericResultsKeepSource() throws Exception {
            int[] extractions = {0};
            FunctionFormulaCalculation2 calculation = new FunctionFormulaCalculation2("agentCheckCount") {
                @Override public void FunctionParameterExtraction(String value, Stack<Integer> starts, Stack<Integer> ends, Stack<String> names, StringBuilder outer) {
                    extractions[0]++;
                    super.FunctionParameterExtraction(value, starts, ends, names, outer);
                }
            };
            calculation.check("sum(2,3)");
            calculation.check("sum(2,3)");
            assertEquals(1, extractions[0]);
            calculation.setStartSharedPool(false);
            calculation.check("sum(2,3)");
            assertEquals(2, extractions[0]);
            CalculationNumberResults numeric = calculation.calculation("42", true);
            assertEquals("42", numeric.getCalculationSourceName());
            assertEquals(0, numeric.getResultLayers());
        }
    
        @Test
        void explainPreservesFunctionArgumentNodesAndPrefixes() {
            FunctionFormulaCalculation2 calculation = new FunctionFormulaCalculation2("agentExplainNodes");
            String formula = "sum(1+2,3)+sum(4,5)";
            LogResults result = calculation.explain(formula, false, true, "trace");
            assertFalse(result.isNameJoin());
            assertEquals("trace(\"" + formula + "\")", result.getPrefix());
            assertEquals(2, result.size());
            String functionKey = "trace" + "sum".hashCode() + "(\"sum\")";
            top.lingyuzhao.varFormatter.utils.DataObj function = (top.lingyuzhao.varFormatter.utils.DataObj) result.get(functionKey);
            assertNotNull(function);
            assertEquals("" + "1+2,3".hashCode() + ":layer(\"1+2,3\")", function.getPrefix());
            assertEquals(3, function.size());
            top.lingyuzhao.varFormatter.utils.DataObj invocation = (top.lingyuzhao.varFormatter.utils.DataObj) function.get("tracesum4_9");
            assertNotNull(invocation);
            assertEquals("6.0", invocation.get("tracesum4_9_r"));
            assertTrue(invocation.getPrefix().contains("sum(1+2,3)"));
            assertTrue(invocation.getPrefix().contains("_r("));
            LogResults inner = calculation.explain("3+4", false, false, "inner");
            assertTrue(inner.isEmpty());
            assertEquals("inner(\"3+4\")", inner.getPrefix());
        }
    
        @Test
        void jvmSignatureConversionDistinguishesParenthesisAtIndexZero() {
            assertEquals("public static double (double x) { return x; }", JvmExpressionFunction.convertToStaticMethodSource("(x)=x"));
            assertThrows(StringIndexOutOfBoundsException.class, () -> JvmExpressionFunction.convertToStaticMethodSource(")x(=x"));
        }
    }

    @Nested
    class UtilitiesAndEquationTest {
        @BeforeEach void resetUtilityOptions() {
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
        @AfterEach void restoreUtilityOptions() {
            Mathematical_Expression.Options.setUseCache(false);
            Mathematical_Expression.Options.setUseBigDecimal(false);
        }
        @Test void elementaryArithmeticPreservesSignsAndOperatorMeaning() {
            assertEquals(4, NumberUtils.divideByTen(49));
            assertEquals(-1, NumberUtils.divideByTen(-11));
            assertEquals(0.0, NumberUtils.factorial(0));
            assertEquals(1.0, NumberUtils.factorial(1));
            assertEquals(-2.0, NumberUtils.factorial(-2));
            assertEquals(0.75, NumberUtils.factorial(1.5));
            assertEquals(120.0, NumberUtils.factorial(5));
            char[] operators = {'+', '-', '*', '/', '%', '^'};
            double[] expected = {10, 6, 16, 4, 0, 64};
            for (int i = 0; i < operators.length; i++) assertEquals(expected[i], NumberUtils.calculation(operators[i], 8, 2));
            assertEquals(2.0, NumberUtils.calculation('%', 8, 3));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.calculation('?', 1, 2));
            for (char low : new char[]{'+', '-'}) {
                for (char high : new char[]{'*', '/', '%', '^'}) assertTrue(NumberUtils.PriorityComparison(low, high));
                assertFalse(NumberUtils.PriorityComparison(low, '+'));
            }
            assertFalse(NumberUtils.PriorityComparison('*', '^'));
            assertFalse(NumberUtils.PriorityComparison('?', '*'));
        }
        @Test void numericComparisonsDistinguishEqualityAndEveryOrdering() {
            String[] operators = {"<", ">", "<=", ">=", "=", "==", "!=", "<>"};
            boolean[][] expected = {
                {true, false, true, false, false, false, true, true},
                {false, false, true, true, true, true, false, false},
                {false, true, false, true, false, false, true, true}
            };
            for (int row = 0; row < 3; row++) for (int column = 0; column < operators.length; column++)
                assertEquals(expected[row][column], NumberUtils.ComparisonOperation(operators[column], row, 1));
            assertThrows(AbnormalOperation.class, () -> NumberUtils.ComparisonOperation("?", 1, 2));
        }
        @Test void rangeSumsHandleDirectionStepsSingleValuesAndOverflowBoundary() {
            assertEquals(18, NumberUtils.sumOfRange(3, 6));
            assertEquals(7, NumberUtils.sumOfRange(7, 7));
            assertEquals(Integer.MAX_VALUE, NumberUtils.sumOfRange(Integer.MAX_VALUE, Integer.MAX_VALUE));
            assertEquals(18.0, NumberUtils.sumOfRange(3.0, 6.0));
            assertEquals(18.0, NumberUtils.sumOfRange(6.0, 3.0));
            assertEquals(Double.MAX_VALUE, NumberUtils.sumOfRange(Double.MAX_VALUE, Double.MAX_VALUE));
            assertEquals(18.0, NumberUtils.sumOfRange(3.0, 6.0, 1));
            assertEquals(7.0, NumberUtils.sumOfRange(7, 7, 0));
            assertEquals(18.0, NumberUtils.sumOfRange(3, 10, 3));
            assertEquals(26.0, NumberUtils.sumOfRange(2, 11, 3));
            assertEquals(8.0, NumberUtils.sumOfRange(3, 4, 0.75));
            assertEquals("7.0", NumberUtils.sumOfRangeString(7, 7));
            assertEquals("(3.0+6.0)*4.0/2", NumberUtils.sumOfRangeString(3, 6));
            assertEquals("(6.0+3.0)*4.0/2", NumberUtils.sumOfRangeString(6, 3));
            assertEquals("(3.0+6.0)*4.0/2", NumberUtils.sumOfRangeString(3, 6, 1));
            assertEquals("7.0", NumberUtils.sumOfRangeString(7, 7, 0));
            assertEquals("3.0*3.0+3.0*(3.0-1)*3.0/2", NumberUtils.sumOfRangeString(3, 10, 3));
            assertEquals("4.0*2.0+4.0*(4.0-1)*3.0/2", NumberUtils.sumOfRangeString(2, 11, 3));
            assertEquals("2.0*3.0+2.0*(2.0-1)*2.0/2", NumberUtils.sumOfRangeString(3, 4, 0.75));
        }
        @Test void rangeProductsIncludeTheLastStepAndReturnSingleValues() {
            assertEquals(105.0, NumberUtils.MultiplyOfRange(3, 7, 2));
            assertEquals(105.0, NumberUtils.MultiplyOfRange(3, 8, 2));
            assertEquals(80.0, NumberUtils.MultiplyOfRange(2, 9, 3));
            assertEquals(1.0, NumberUtils.MultiplyOfRange(7, 3, 2));
            assertEquals(7.0, NumberUtils.MultiplyOfRange(7, 7, 0));
            assertEquals("105.0", NumberUtils.MultiplyOfRangeString(3, 7, 2));
            assertEquals("105.0", NumberUtils.MultiplyOfRangeString(3, 8, 2));
            assertEquals("1.0", NumberUtils.MultiplyOfRangeString(7, 3, 2));
            assertEquals("7.0", NumberUtils.MultiplyOfRangeString(7, 7, 0));
        }
        @Test void characterPredicatesRespectAsciiBoundariesAndEmptyInput() {
            assertEquals("a\tb", StrUtils.removeEmpty(" a \t b "));
            assertEquals("", StrUtils.removeEmpty(""));
            for (char c = '0'; c <= '9'; c++) assertTrue(StrUtils.IsANumber(c));
            for (char c : new char[]{'/', ':', 'a', '\u0661'}) assertFalse(StrUtils.IsANumber(c));
            assertTrue(StrUtils.IsANumber(""));
            assertTrue(StrUtils.IsANumber("01239"));
            assertFalse(StrUtils.IsANumber("12a"));
            assertFalse(StrUtils.IsANumber("3!"));
            for (char c : new char[]{'+', '-', '*', '/', '%', '^'}) assertTrue(StrUtils.IsAnOperator(c));
            assertFalse(StrUtils.IsAnOperator('!'));
            assertFalse(StrUtils.containsNumber("abc"));
            assertFalse(StrUtils.containsNumber(""));
            assertTrue(StrUtils.containsNumber("a7z"));
            for (char c : new char[]{'A', 'Z', 'a', 'z'}) assertEquals(c, StrUtils.containsEnChar("12" + c));
            for (char c : new char[]{'@', '[', '`', '{'}) assertNull(StrUtils.containsEnChar("12" + c));
            assertNull(StrUtils.containsEnChar(""));
        }
        @Test void numericParsingCoversFactorialsCachingAndMalformedNumbers() {
            for (boolean cache : new boolean[]{false, true}) {
                Mathematical_Expression.Options.setUseCache(cache);
                assertEquals(-12.5, StrUtils.stringToDouble("-12.5"));
                assertEquals(120.0, StrUtils.stringToDouble("5!"));
                assertEquals(0.0, StrUtils.stringToDouble("0!"));
                if (cache) {
                    assertEquals(-12.5, Mathematical_Expression.Options.getCacheCalculation("-12.5".hashCode()));
                    assertEquals(120.0, Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
                    assertEquals(0.0, Mathematical_Expression.Options.getCacheCalculation("0!".hashCode()));
                }
                assertEquals(new BigDecimal("-12.5"), StrUtils.stringToBigDecimal("-12.5"));
                assertEquals(new BigDecimal("120.0"), StrUtils.stringToBigDecimal("5!"));
                assertEquals(new BigDecimal("0.0"), StrUtils.stringToBigDecimal("0!"));
                assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToDouble("-1!"));
                assertThrows(UnsupportedOperationException.class, () -> StrUtils.stringToBigDecimal("-1!"));
                assertThrows(NumberFormatException.class, () -> StrUtils.stringToDouble("abc"));
                assertThrows(NumberFormatException.class, () -> StrUtils.stringToBigDecimal("abc"));
                if (cache) {
                    assertEquals(new BigDecimal("120.0"), Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()));
                    assertSame(Mathematical_Expression.Options.getCacheCalculation("5!".hashCode()), StrUtils.stringToBigDecimal("5!"));
                    assertEquals(120.0, StrUtils.stringToDouble("5!"));
                    Mathematical_Expression.Options.cacheCalculation("6.25".hashCode(), new BigDecimal("9.75"));
                    assertEquals(9.75, StrUtils.stringToDouble("6.25"));
                    assertEquals(new BigDecimal("9.75"), StrUtils.stringToBigDecimal("6.25"));
                    Mathematical_Expression.Options.cacheCalculation("4.50".hashCode(), 88.0);
                    assertEquals(new BigDecimal("4.50"), StrUtils.stringToBigDecimal("4.50"));
                    assertEquals(new BigDecimal("4.50"), Mathematical_Expression.Options.getCacheCalculation("4.50".hashCode()));
                }
            }
        }
        @Test void splittingRetainsNestedArgumentsAndEmptyFields() {
            assertEquals(List.of("a", "", "b", ""), StrUtils.splitByChar(" a ,, b ,", ','));
            assertEquals(List.of(""), StrUtils.splitByChar("", ','));
            assertEquals(List.of("a", "f(1,g(2,3))", "", "b", ""), StrUtils.splitByCharWhereNoB(" a , f(1,g(2,3)),, b ,", ','));
            assertEquals(List.of(""), StrUtils.splitByCharWhereNoB("", ','));
            assertEquals("", StrUtils.randomString(0));
            String random = StrUtils.randomString(64);
            assertEquals(64, random.length());
            assertTrue(random.matches("[a-z]{64}"));
            assertThrows(NegativeArraySizeException.class, () -> StrUtils.randomString(-1));
        }
        @Test void numberResultsPreservePrecisionSourceAndOrdering() {
            CalculationNumberResults result = new CalculationNumberResults(3, 8.25, "arithmetic");
            assertEquals(3, result.getResultLayers());
            assertEquals(8.25, result.getResult());
            assertEquals(new BigDecimal("8.25"), result.getBigDecimalResult());
            assertEquals("arithmetic", result.getCalculationSourceName());
            result.setSource("updated");
            assertEquals("updated", result.getCalculationSourceName());
            assertEquals("CalculationNumberResults{result=8.25, source='updated'}", result.toString());
            assertEquals(1, result.compareTo(null));
            assertEquals(2, result.compareTo(new CalculationNumberResults(1, 7, "other")));
            assertEquals(-2, result.compareTo(new CalculationNumberResults(1, 11, "other")));
            assertEquals(0, result.compare(result, result));
            assertEquals(2, result.compare(result, new CalculationNumberResults(1, 7, "other")));
            assertThrows(UnsupportedOperationException.class, () -> result.explain(VarFormatter.JSON));
            BigDecimal precise = new BigDecimal("12345678901234567890.123456789");
            CalculationBigDecimalResults decimal = new CalculationBigDecimalResults(7, precise, "decimal");
            assertSame(precise, decimal.getBigDecimalResult());
            assertEquals(7, decimal.getResultLayers());
            assertEquals("decimal", decimal.getCalculationSourceName());
            CalculationComplexResults complex = new CalculationComplexResults(4, 3, -2, "complex");
            assertEquals(3.0, complex.toComplexNumber().getReal());
            assertEquals(-2.0, complex.toComplexNumber().getImaginary());
            assertEquals("3.0 - 2.0i", complex.toString());
        }
        @Test void booleanResultsSortFalseBeforeTrueAndDescribeOperands() {
            CalculationBooleanResults yes = new CalculationBooleanResults(true, "comparison", 4, 5, 2);
            CalculationBooleanResults no = new CalculationBooleanResults(false, "other", 2, 1, 3);
            assertTrue(yes.getResult());
            assertFalse(no.getResult());
            assertEquals(4, yes.getResultLayers());
            assertEquals(5.0, yes.getLeft());
            assertEquals(2.0, yes.getRight());
            assertEquals("comparison", yes.getCalculationSourceName());
            yes.setSource("changed");
            assertEquals("changed", yes.getCalculationSourceName());
            assertEquals(1, yes.compareTo(null));
            assertEquals(1, no.compareTo(null));
            assertEquals(1, yes.compareTo(no));
            assertEquals(-1, no.compareTo(yes));
            assertEquals(0, yes.compareTo(yes));
            assertEquals(0, no.compareTo(no));
            assertEquals(-1, yes.compare(no, yes));
            assertEquals("CalculationBooleanResults{result=true, source='changed', Layers=4, left=5.0, right=2.0}", yes.toString());
            assertThrows(UnsupportedOperationException.class, yes::getBigDecimalResult);
        }
        @Test void logResultsExposeResultAndFormatRecordedData() {
            LogResults log = new LogResults("evaluation");
            assertNull(log.getResult());
            log.setResult(17.5);
            assertEquals(17.5, log.getResult());
            assertEquals("evaluation", log.getCalculationSourceName());
            log.put("answer", 17.5);
            String json = log.explain(VarFormatter.JSON);
            assertTrue(json.contains("answer"));
            assertTrue(json.contains("17.5"));
            assertThrows(UnsupportedOperationException.class, log::getResultLayers);
            assertThrows(UnsupportedOperationException.class, () -> log.setSource("other"));
        }
        @Test void exceptionConstructorsRetainMessagesAndCauses() {
            Exception cause = new Exception("reason");
            assertNull(new WrongFormat().getMessage());
            assertEquals("bad", new WrongFormat("bad").getMessage());
            assertSame(cause, new WrongFormat("bad", cause).getCause());
            assertSame(cause, new WrongFormat(cause).getCause());
            assertNull(new AbnormalOperation().getMessage());
            assertEquals("bad", new AbnormalOperation("bad").getMessage());
            assertSame(cause, new AbnormalOperation("bad", cause).getCause());
            assertSame(cause, new AbnormalOperation(cause).getCause());
            assertNull(new ExtractException().getMessage());
            assertEquals("bad", new ExtractException("bad").getMessage());
            assertSame(cause, new ExtractException("bad", cause).getCause());
            assertSame(cause, new ExtractException(cause).getCause());
            assertEquals("absent", new EquationSolver.NoRootFoundException("absent").getMessage());
        }
        private static EquationSolver solver(DoubleUnaryOperator function, double target) {
            return new EquationSolver("expression", target, "equation", new ManyToOneNumberFunction("equation") {
                @Override public double run(double... arguments) { return function.applyAsDouble(arguments[0]); }
            });
        }
        @Test void newtonMethodFindsNontrivialRootAndHonorsConfiguration() {
            EquationSolver square = solver(x -> x * x, 9);
            assertEquals(9.0, square.getResult());
            assertEquals(2.0, square.getNewtonInitialX());
            assertEquals(1.0, square.getBisectionLeft());
            assertEquals(3.0, square.getBisectionRight());
            assertEquals(1e-12, square.getTol());
            assertEquals(100, square.getMaxIter());
            assertTrue(square.isUseNewton());
            square.setNewtonInitialX(4);
            square.setTol(1e-9);
            square.setMaxIter(20);
            assertEquals(4.0, square.getNewtonInitialX());
            assertEquals(1e-9, square.getTol());
            assertEquals(20, square.getMaxIter());
            double[] solution = square.solveNewton(1e-9, 20);
            assertEquals(3.0, solution[0], 1e-8);
            assertTrue(solution[1] > 1);
            CalculationNumberResults result = square.calculation(false);
            assertEquals(3.0, result.getResult(), 1e-8);
            assertEquals("equation", result.getCalculationSourceName());
            assertEquals((int) solution[1], result.getResultLayers());
            square.setNewtonInitialX(3);
            assertArrayEquals(new double[]{3, 1}, square.solveNewton(1e-9, 1));
            assertFalse(square.isBigDecimal());
            assertTrue(square.isUnBigDecimal());
            assertThrows(UnsupportedOperationException.class, square::convertToMultiPrecisionSupported);
            assertThrows(UnsupportedOperationException.class, () -> square.calculationBigDecimals(false));
        }
        @Test void newtonMethodRejectsZeroDerivativeAndIterationExhaustion() {
            EquationSolver constant = solver(x -> 5, 3);
            assertTrue(assertThrows(ArithmeticException.class, () -> constant.solveNewton(1e-8, 4)).getMessage().contains("导数过小"));
            assertTrue(assertThrows(ArithmeticException.class, () -> constant.solveNewton(1e-8, 0)).getMessage().contains("未收敛"));
            EquationSolver square = solver(x -> x * x, 9);
            assertThrows(ArithmeticException.class, () -> square.solveNewton(1e-15, 1));
            EquationSolver boundary = solver(x -> x, 0);
            boundary.setNewtonInitialX(1);
            assertThrows(ArithmeticException.class, () -> boundary.solveNewton(1, 1));
            EquationSolver derivativeBoundary = solver(x -> x * 1e-12, 0);
            derivativeBoundary.setNewtonInitialX(0);
            assertTrue(assertThrows(ArithmeticException.class, () -> derivativeBoundary.solveNewton(0, 1)).getMessage().contains("未收敛"));
        }
        @Test void bisectionMethodTracksBothHalvesAndToleranceLimits() {
            EquationSolver linear = solver(x -> x, 2.75);
            linear.setBisectionLeft(1);
            linear.setBisectionRight(4);
            assertEquals(1.0, linear.getBisectionLeft());
            assertEquals(4.0, linear.getBisectionRight());
            double[] root = linear.solveBisection(1e-10, 100);
            assertEquals(2.75, root[0], 1e-8);
            assertTrue(root[1] > 1);
            linear.setUseNewton(false);
            assertFalse(linear.isUseNewton());
            linear.setTol(1e-10);
            assertEquals(2.75, linear.calculation(true).getResult(), 1e-8);
            assertArrayEquals(new double[]{1, 0}, linear.solveBisection(1e-6, 0));
            assertArrayEquals(new double[]{1, 9}, linear.solveBisection(1.5, 9));
            assertArrayEquals(new double[]{2.5, 1}, linear.solveBisection(0, 1));
            EquationSolver exact = solver(x -> x, 2);
            assertArrayEquals(new double[]{2, 1}, exact.solveBisection(1e-9, 1));
            assertArrayEquals(new double[]{2, 1}, exact.solveBisection(1e-9, 10));
            EquationSolver residual = solver(x -> x, 2.5);
            assertArrayEquals(new double[]{2, 2}, residual.solveBisection(0.5, 2));
            EquationSolver endpoint = solver(x -> x, 1);
            assertArrayEquals(new double[]{2, 1}, endpoint.solveBisection(0, 1));
            EquationSolver zeroMid = solver(x -> x, 2);
            assertArrayEquals(new double[]{1.5, 2}, zeroMid.solveBisection(0, 2));
            EquationSolver decreasing = solver(x -> 5 - x, 2);
            assertArrayEquals(new double[]{2, 1}, decreasing.solveBisection(0, 1));
            EquationSolver decreasingMid = solver(x -> 5 - x, 3);
            assertArrayEquals(new double[]{1.5, 2}, decreasingMid.solveBisection(0, 2));
            assertThrows(IllegalArgumentException.class, () -> solver(x -> x, 10).solveBisection(1e-8, 100));
            assertThrows(IllegalArgumentException.class, () -> solver(x -> x * x, -1).solveBisection(1e-8, 100));
        }
        @Test void intervalDetectionRefinesPositiveRootsAndReportsNegativeIntervalFailure() {
            EquationSolver detected = solver(x -> 6.5 - x, 0);
            detected.autoDetectInterval();
            assertEquals(6.0, detected.getBisectionLeft());
            assertEquals(7.0, detected.getBisectionRight());
            assertEquals(6.5, detected.getNewtonInitialX());
            assertEquals(6.5, detected.calculation(false).getResult());
            EquationSolver shifted = solver(x -> 8 - x, 2.5);
            shifted.autoDetectInterval();
            assertEquals(5.0, shifted.getBisectionLeft());
            assertEquals(6.0, shifted.getBisectionRight());
            assertEquals(5.5, shifted.getNewtonInitialX());
            EquationSolver endpoint = solver(x -> 1 - x, 0);
            endpoint.autoDetectInterval();
            assertEquals(0.0, endpoint.getBisectionLeft());
            assertEquals(1.0, endpoint.getBisectionRight());
            assertEquals(0.5, endpoint.getNewtonInitialX());
            EquationSolver atZero = solver(x -> x, 0);
            atZero.autoDetectInterval();
            assertEquals(0.0, atZero.getBisectionLeft());
            assertEquals(1.0, atZero.getBisectionRight());
            EquationSolver negative = solver(x -> -x - 1, 0);
            assertThrows(EquationSolver.NoRootFoundException.class, negative::autoDetectInterval);
            EquationSolver negativeShifted = solver(x -> 1 - x, 2);
            assertThrows(EquationSolver.NoRootFoundException.class, negativeShifted::autoDetectInterval);
            ArrayList<Double> visited = new ArrayList<>();
            EquationSolver absent = solver(x -> { visited.add(x); return 2; }, 0);
            absent.autoDetectInterval();
            assertEquals(List.of(0.0, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0), visited);
            assertEquals(1.0, absent.getBisectionLeft());
            assertEquals(3.0, absent.getBisectionRight());
        }
        private static EquationSolverExpression parameterEquation() throws Exception {
            ParsedSignature parsed = new ParsedSignature("equation", new String[]{"a", "b"}, "a-b");
            ManyToOneNumberFunction delegate = new ManyToOneNumberFunction("equation") {
                @Override public double run(double... arguments) { return arguments[0] - arguments[1]; }
            };
            java.lang.reflect.Constructor<JvmExpressionFunction> constructor = JvmExpressionFunction.class
                .getDeclaredConstructor(ParsedSignature.class, ManyToOneNumberFunction.class);
            constructor.setAccessible(true);
            JvmExpression expression = new JvmExpression("parameters", new double[]{5, 0},
                constructor.newInstance(parsed, delegate), "a-b");
            return new EquationSolverExpression("5-x", 0, "parameterEquation", new HashSet<>(List.of(1)), expression, new Object[]{5.0, "x"});
        }
        @Test void parameterEquationsUpdateKnownValuesAndPreserveUnknownPositions() throws Exception {
            EquationSolverExpression equation = parameterEquation();
            assertEquals(4.0, equation.getBisectionLeft());
            assertEquals(5.0, equation.getBisectionRight());
            assertEquals(4.5, equation.getNewtonInitialX());
            assertFalse(equation.indexIsUnKnownNumber(0));
            assertTrue(equation.indexIsUnKnownNumber(1));
            assertEquals(3.0, equation.eval(2));
            assertEquals(5.0, equation.getJvmExpression().getParamNumber(0));
            assertEquals(2.0, equation.getJvmExpression().getParamNumber(1));
            assertEquals("5.0-x = 0.0", equation.explain());
            equation.setKnownNumber(0, 8);
            assertEquals(6.0, equation.eval(2));
            assertEquals("8.0-x = 0.0", equation.explain());
            assertThrows(IllegalArgumentException.class, () -> equation.setKnownNumber(1, 3));
            assertThrows(IllegalArgumentException.class, () -> equation.setUnKnownNumber(0, 3));
            equation.setUnKnownNumber(1, 4);
            assertFalse(equation.indexIsUnKnownNumber(1));
            assertEquals(4.0, equation.eval(99));
            assertEquals("8.0-4.0 = 0.0", equation.explain());
            assertThrows(IllegalArgumentException.class, () -> equation.setUnKnownNumber(1, 2));
        }
        @Test void clonedParameterEquationsKeepIndependentNumericArrays() throws Exception {
            EquationSolverExpression original = parameterEquation();
            EquationSolverExpression clone = original.cloneExpression();
            assertNotSame(original, clone);
            assertNotSame(original.getJvmExpression(), clone.getJvmExpression());
            assertEquals("parameterEquation_clone", clone.getCalculationName());
            assertEquals("5-x", clone.getExpressionStr());
            assertEquals("5.0-x = 0.0", clone.explain());
            clone.setKnownNumber(0, 8);
            assertEquals(6.0, clone.eval(2));
            assertEquals(3.0, original.eval(2));
            assertEquals("5.0-x = 0.0", original.explain());
        }
        @Test void solverFactoriesReuseNamesRejectConflictsAndExposeUnsupportedPrecision() {
            String firstName = "utility-equation-single";
            String secondName = "utility-equation-two";
            CalculationManagement.unregister(firstName);
            CalculationManagement.unregister(secondName);
            try {
                SingletonEquationSolving first = SingletonEquationSolving.getInstance(firstName);
                SingletonEquationSolvingTwo second = SingletonEquationSolvingTwo.getInstance(secondName);
                assertEquals(firstName, first.getName());
                assertEquals(secondName, second.getName());
                assertSame(first, SingletonEquationSolving.getInstance(firstName));
                assertSame(second, SingletonEquationSolvingTwo.getInstance(secondName));
                assertThrows(ExtractException.class, () -> SingletonEquationSolving.getInstance(secondName));
                assertThrows(ExtractException.class, () -> SingletonEquationSolvingTwo.getInstance(firstName));
                assertEquals(" 5-x = 0 ", first.formatStr(" 5-x = 0 "));
                assertEquals(" 5-x = 0 ", second.formatStr(" 5-x = 0 "));
                assertThrows(UnsupportedOperationException.class, () -> first.compileBigDecimal("x=1", true));
                assertThrows(UnsupportedOperationException.class, () -> second.compileBigDecimal("x=1", false));
                assertThrows(IllegalArgumentException.class, () -> first.compile("x", true));
                assertThrows(IllegalArgumentException.class, () -> second.compile("x=1=2", false));
                assertThrows(IllegalArgumentException.class, () -> first.calculation("x", true));
                assertThrows(IllegalArgumentException.class, () -> second.calculation("x", false));
            } finally {
                CalculationManagement.unregister(firstName);
                CalculationManagement.unregister(secondName);
            }
        }
        @Test void equationParsingRejectsBadSeparatorsAndNonNumericRightSide() {
            assertThrows(IllegalArgumentException.class, () -> EquationSolver.compile("x", "invalid"));
            assertThrows(IllegalArgumentException.class, () -> EquationSolver.compile("x=1=2", "invalid"));
            assertThrows(NumberFormatException.class, () -> EquationSolver.compile("x=abc", "invalid"));
            assertThrows(IllegalArgumentException.class, () -> EquationSolverExpression.compile("x", "invalid"));
            assertThrows(IllegalArgumentException.class, () -> EquationSolverExpression.compile("x=1=2", "invalid"));
            // Runtime generation imports an unavailable original-package superclass in this distribution.
            RuntimeException generated = assertThrows(RuntimeException.class, () -> EquationSolver.compile("x=1", "unavailable"));
            assertNotNull(generated.getCause());
            assertTrue(generated.getCause().getMessage().contains("编译错误"));
            RuntimeException expression = assertThrows(RuntimeException.class, () -> EquationSolverExpression.compile("5-x=1", "unavailable"));
            assertNotNull(expression.getCause());
            assertTrue(expression.getCause().getMessage().contains("编译错误"));
        }
        @Test void solverComponentsReturnTheResultOfTheirCompiledEquations() throws Exception {
            SingletonEquationSolving component = new SingletonEquationSolving("numeric-equation-component") {
                @Override public EquationSolver compile(String formula, boolean formatted) {
                    assertEquals("x=2", formula);
                    assertTrue(formatted);
                    return solver(x -> x, 2);
                }
            };
            CalculationNumberResults result = component.calculation("x=2", true);
            assertEquals(2.0, result.getResult());
            assertEquals(1, result.getResultLayers());
            EquationSolverExpression parameter = parameterEquation();
            SingletonEquationSolvingTwo componentTwo = new SingletonEquationSolvingTwo("parameter-equation-component") {
                @Override public EquationSolverExpression compile(String formula, boolean formatted) {
                    assertEquals("5-x=0", formula);
                    assertFalse(formatted);
                    return parameter;
                }
            };
            assertEquals(5.0, componentTwo.calculation("5-x=0", false).getResult(), 1e-9);
        }
    }
}
