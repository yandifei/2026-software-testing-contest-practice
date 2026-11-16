
    // ==================================================================
    // 表达式引擎补测：真实 API 可达、但原套件未断言的缺口
    //
    // 覆盖点：
    //   1) ComplexCalculation.compile / compileBigDecimal 在
    //      formatRequired=true 时先“去空白”再编译（两条独立的格式化语句）；
    //   2) FunctionExpression.subCompile 在表达式不含可替换函数时提前返回，
    //      且不修改传入的三个栈；
    //   3) FunctionPackage.MATH 的内置函数按数值参数求值（sum/pow/sqrt/…）；
    //   4) CalculationManagement 的注册、注销、查询与函数表簿记。
    // 全部使用公开 API 与固定输入，不 mock 被测类、不替换实现。
    // ==================================================================
    @Nested
    @DisplayName("表达式引擎补测")
    class EngineGapRegressionTests {

        @Test
        @DisplayName("复数编译：formatRequired=true 时先去空白再编译，实部虚部结果正确")
        void complexCompilationFormatsInputWhenRequired() {
            // 独立名称注册复数计算组件，避免与其它用例共享注册表状态。
            String calculationName = uniq("gapComplexFormat");
            ComplexCalculation complexCalculation = ComplexCalculation.getInstance(calculationName);
            try {
                // 入参带前导/尾随空格与内部空格：formatRequired=true 时必须先格式化。
                ComplexExpression formatted = complexCalculation.compile(" (2+3) + 4i ", true);
                // 格式化后实部与虚部之间只保留一个运算符，且无空白。
                assertEquals("(2+3)+4i", formatted.getExpressionStr());

                // compileBigDecimal 走的是另一条格式化语句，必须单独验证。
                ComplexExpression formattedBigDecimal =
                        complexCalculation.compileBigDecimal(" (2+3) + 4i ", true);
                assertEquals("(2+3)+4i", formattedBigDecimal.getExpressionStr());

                // 编译结果可计算：实部 (2+3)=5，虚部 +4i=4。
                CalculationComplexResults result = formattedBigDecimal.calculationBigDecimals(false);
                assertEquals(5.0d, result.getResult(), 1e-12);
                assertEquals(4.0d, result.toComplexNumber().getImaginary(), 1e-12);
            } finally {
                // 无论断言是否失败都释放注册表，保证用例之间互不影响。
                CalculationManagement.unregister(calculationName);
            }
        }

        @Test
        @DisplayName("子编译短路：无需替换函数时直接返回原表达式，且三个栈保持不变")
        void functionSubCompileReturnsEarlyWithoutTouchingStacks() {
            // 数值起始下标栈：只要表达式没有函数调用，内容就不会被消费。
            Stack<Integer> numericStartIndexes = new Stack<>();
            numericStartIndexes.push(1);
            // 数值结束下标栈：同上。
            Stack<Integer> numericEndIndexes = new Stack<>();
            numericEndIndexes.push(2);
            // 函数名栈：故意放一个未注册函数名，验证“未注册”时也不会触发替换。
            Stack<String> functionNames = new Stack<>();
            functionNames.push("unregisteredFunction");

            // 纯数字表达式没有可子编译的部分，应原样返回。
            assertEquals("123", FunctionExpression.subCompile(
                    "123", numericStartIndexes, numericEndIndexes, functionNames).toString());
            // 三个栈必须保持入参时的深度，说明短路分支没有消费任何下标。
            assertEquals(1, numericStartIndexes.size());
            assertEquals(1, numericEndIndexes.size());
            assertEquals(1, functionNames.size());

            // 普通四则表达式同样不含函数调用，也应原样返回。
            Stack<Integer> plainStartIndexes = new Stack<>();
            plainStartIndexes.push(1);
            Stack<Integer> plainEndIndexes = new Stack<>();
            plainEndIndexes.push(2);
            Stack<String> emptyFunctionNames = new Stack<>();
            assertEquals("1+2", FunctionExpression.subCompile(
                    "1+2", plainStartIndexes, plainEndIndexes, emptyFunctionNames).toString());
            assertEquals(1, plainStartIndexes.size());
        }

        @Test
        @DisplayName("函数包 MATH：内置函数按数值参数求值，未知函数返回 null")
        void mathPackageEvaluatesNumericArguments() {
            // 内置函数包 MATH 是单例，直接取用。
            FunctionPackage mathPackage = FunctionPackage.MATH;
            // 期望值 → 入参 的对照表：覆盖求和、幂、开方、阶乘与三角函数。
            Map<String, double[]> numericExamples = new LinkedHashMap<>();
            numericExamples.put("sum", new double[]{10, 7, -2, 5});
            numericExamples.put("pow", new double[]{81, 3, 4});
            numericExamples.put("sqrt", new double[]{7, 49});
            numericExamples.put("factorial", new double[]{120, 5});
            numericExamples.put("sin", new double[]{0.5, Math.PI / 6});
            numericExamples.put("cos", new double[]{0.5, Math.PI / 3});
            numericExamples.put("tan", new double[]{1, Math.PI / 4});
            numericExamples.put("cot", new double[]{1, Math.PI / 4});
            numericExamples.put("sec", new double[]{2, Math.PI / 3});
            numericExamples.put("csc", new double[]{2, Math.PI / 6});

            numericExamples.forEach((functionName, sample) -> {
                // 从函数包中取出内置函数；getFunctionByNameNoCheck 不抛异常。
                ManyToOneNumberFunction function =
                        (ManyToOneNumberFunction) mathPackage.getFunctionByNameNoCheck(functionName);
                // 第一个元素是期望值，其余是实参。
                double expectedValue = sample[0];
                double[] arguments = Arrays.copyOfRange(sample, 1, sample.length);
                // 三角函数存在浮点误差，统一用 1e-10 比较。
                assertEquals(expectedValue, function.run(arguments), 1e-10, functionName);
            });

            // 未注册的函数名必须返回 null，而不是抛异常。
            assertNull(mathPackage.getFunctionByNameNoCheck("unknown"));
        }

        @Test
        @DisplayName("计算注册中心：注册/注销/查询与函数表的簿记一致")
        void calculationRegistryKeepsBookkeepingConsistent() throws Exception {
            // 先取一个已知名称的组件并注册，随后用同名再注册应返回 false。
            BracketsCalculation2 registeredCalculation = BracketsCalculation2.getInstance("gapMgmtKnown");
            assertFalse(CalculationManagement.register(registeredCalculation, true));

            // 新建一个未注册名称的组件，先注销（不存在）再注册（成功）。
            BracketsCalculation2 freshCalculation = BracketsCalculation2.getInstance("gapMgmtFresh");
            assertTrue(CalculationManagement.unregister("gapMgmtFresh"));
            assertTrue(CalculationManagement.register(freshCalculation, true));
            // 原组件以“不覆盖”方式注册：已存在则返回 true。
            assertTrue(CalculationManagement.register(registeredCalculation, false));

            // 查询侧：已注册为 true，未注册为 false，取回的必须是同一个对象。
            assertTrue(CalculationManagement.isRegister("gapMgmtKnown"));
            assertSame(registeredCalculation, CalculationManagement.getCalculationByName("gapMgmtKnown"));
            assertFalse(CalculationManagement.isRegister("gapMgmtAbsent"));

            // 注销一次成功、再注销返回 false（幂等性）。
            assertTrue(CalculationManagement.unregister("gapMgmtKnown"));
            assertFalse(CalculationManagement.unregister("gapMgmtKnown"));

            // 函数表：重复注册同名函数第二次返回 false。
            ExpressionFunction parsedFunction = ExpressionFunction.parse("gapmgmtf(x) = x + 5");
            assertTrue(CalculationManagement.register(parsedFunction));
            assertFalse(CalculationManagement.register(parsedFunction));
            // 函数存在性查询与按名取对象（含 NoCheck 变体）必须一致。
            assertTrue(CalculationManagement.isFunctionExist("gapmgmtf"));
            assertFalse(CalculationManagement.isFunctionExist("gapmgmtMissing"));
            assertTrue(CalculationManagement.getFunctionMap().contains("gapmgmtf"));
            assertSame(parsedFunction, CalculationManagement.getFunctionByNameNoCheck("gapmgmtf"));
            assertSame(parsedFunction, CalculationManagement.getFunctionByName("gapmgmtf"));
            assertNull(CalculationManagement.getFunctionByNameNoCheck("gapmgmtMissing"));

            // 内置函数包注册后可用，注销函数后 unregisterF 返回 false。
            CalculationManagement.register(FunctionPackage.BRANCH);
            assertTrue(CalculationManagement.isFunctionExist("ifGt"));
            assertTrue(CalculationManagement.unregisterF("ifGt"));
            assertFalse(CalculationManagement.unregisterF("ifGt"));
        }
    }
