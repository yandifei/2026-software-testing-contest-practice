
    // ==================================================================
    // 引擎中“不依赖动态编译”的两个真实路径
    //
    // 背景：本副本缺少 io.github.beardedManZhao.mathematicalExpression 包，
    // DynamicFunctionCompiler 的 javac 必然失败，所有“编译成功”分支不可达。
    // 但下面两处逻辑在编译之前就已执行，属于真实可达：
    //   1) BooleanCalculation2 对 "null" 操作数的两个早退分支；
    //   2) JvmExpressionFunction 的签名解析（先解析、后编译）。
    // ==================================================================
    @Nested
    @DisplayName("引擎预编译路径（null 比较与函数签名解析）")
    class EnginePreCompilePaths {

        @Test
        @DisplayName("BooleanCalculation2：左侧为 null 时按 0 与右侧比较")
        void booleanCalculationWithNullOnLeft() {
            // Given：独立的布尔计算组件，避免与其它用例共享注册表状态。
            BooleanCalculation2 booleanCalculation = BooleanCalculation2.getInstance("boolNullLeft");

            // When：左操作数为 null、右操作数为 1；按实现约定 null 视为 0。
            CalculationBooleanResults result = booleanCalculation.calculation("null==1", true);

            // Then：0 == 1 为假，且结果层数固定为 1。
            assertFalse(result.getResult());
            assertEquals(1, result.getResultLayers());
        }

        @Test
        @DisplayName("BooleanCalculation2：右侧为 null 时按 1 与 0 比较")
        void booleanCalculationWithNullOnRight() {
            // Given：同一实现、独立名称。
            BooleanCalculation2 booleanCalculation = BooleanCalculation2.getInstance("boolNullRight");

            // When：左操作数 1、右操作数 null；该分支固定用 1 与 0 比较。
            CalculationBooleanResults result = booleanCalculation.calculation("1==null", true);

            // Then：1 == 0 为假。
            assertFalse(result.getResult());
        }

        @Test
        @DisplayName("BooleanCalculation2：两侧都是 null 时两边都按 0 处理")
        void booleanCalculationWithNullOnBothSides() {
            // Given：独立名称。
            BooleanCalculation2 booleanCalculation = BooleanCalculation2.getInstance("boolNullBoth");

            // When：两侧都为 null，aNull 取 0。
            CalculationBooleanResults result = booleanCalculation.calculation("null==null", true);

            // Then：0 == 0 为真。
            assertTrue(result.getResult());
        }

        @Test
        @DisplayName("JvmExpressionFunction：左括号在最前时签名解析不误判为缺少参数列表")
        void functionSignatureWithLeadingParenthesis() {
            // Given：函数定义体以 '(' 开头（openParen == 0），参数为空、函数名为空串。
            //        该输入用于区分 “openParen < 0” 与其边界变异 “openParen <= 0”。

            // When & Then：解析通过后才会进入动态编译并因缺包失败，
            //              因此必须能看到“动态函数生成失败”，而不是“需要包含参数列表”。
            assertMessageContains(assertThrows(RuntimeException.class,
                    () -> JvmExpressionFunction.parse("(x)=x+1")), "动态函数生成失败");
        }
    }
