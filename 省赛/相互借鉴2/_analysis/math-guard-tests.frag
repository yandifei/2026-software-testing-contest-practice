
    // ==================================================================
    // 区间运算的“必须终止”守卫
    //
    // 目的：NumberUtils 的两个区间运算各自有一个累加语句
    // （MultiplyOfRange L218、MultiplyOfRangeString L238 的 start += step）。
    // 该语句若被改成 start -= step，循环条件 start <= end 永远成立，
    // 变异体会死循环；原始实现则会立即返回。
    //
    // PIT 对死循环只能记 TIMED_OUT（按官方口径不计入 KILLED）。
    // 这里把调用放进守护线程，最多等 1 秒（< PIT 的 3 秒上限）：
    //   - 原始实现：线程已结束，断言通过；
    //   - 死循环变异：线程仍存活，断言失败 => 记 KILLED，而不是超时。
    // 不修改被测实现，不依赖 PIT 变异体身份，只是给调用加了时间上界。
    // ==================================================================
    @Nested
    @DisplayName("区间运算终止性守卫")
    class RangeComputationTerminationGuards {

        /** 守护线程等待上限（毫秒），必须明显小于 PIT 的单测 3000ms 超时。 */
        private static final long TERMINATION_GUARD_MILLIS = 1000L;

        @Test
        @DisplayName("MultiplyOfRange(1,5,1) 必须在 1 秒内返回 120")
        void multiplyOfRangeReturnsWithinGuard() throws InterruptedException {
            // Given：1..5、步长 1，期望累乘 1*2*3*4*5=120；用长度 1 的数组回传结果。
            double[] productHolder = new double[1];

            // When：在守护线程里调用；守护线程不会阻止 JVM 退出。
            Thread calculationThread = new Thread(
                    () -> productHolder[0] = NumberUtils.MultiplyOfRange(1.0d, 5.0d, 1.0d),
                    "multiply-of-range-guard");
            calculationThread.setDaemon(true);
            calculationThread.start();
            calculationThread.join(TERMINATION_GUARD_MILLIS);

            // Then：线程必须已结束（否则说明发生了死循环），且结果正确。
            assertFalse(calculationThread.isAlive(),
                    "MultiplyOfRange(1,5,1) 未在 " + TERMINATION_GUARD_MILLIS + "ms 内返回，疑似死循环");
            assertEquals(120.0d, productHolder[0], 1e-9d);
        }

        @Test
        @DisplayName("MultiplyOfRangeString(1,5,1) 必须在 1 秒内返回 \"120.0\"")
        void multiplyOfRangeStringReturnsWithinGuard() throws InterruptedException {
            // Given：同一区间，字符串版本无步长归一化，直接按 start<=end 累乘。
            String[] productHolder = new String[1];

            // When：同样放进守护线程并设 1 秒上限。
            Thread calculationThread = new Thread(
                    () -> productHolder[0] = NumberUtils.MultiplyOfRangeString(1.0d, 5.0d, 1.0d),
                    "multiply-of-range-string-guard");
            calculationThread.setDaemon(true);
            calculationThread.start();
            calculationThread.join(TERMINATION_GUARD_MILLIS);

            // Then：线程必须结束，且返回字符串形式的累乘结果。
            assertFalse(calculationThread.isAlive(),
                    "MultiplyOfRangeString(1,5,1) 未在 " + TERMINATION_GUARD_MILLIS + "ms 内返回，疑似死循环");
            assertEquals("120.0", productHolder[0]);
        }
    }
