
    // ==================================================================
    // 私有入口边界探针（反射直调，不替换任何被测实现）
    //
    // 为什么这些边界只能从私有入口进入：
    //   1) AdvancedLeavePolicy.roundHalfDay 在公开链路上的入参恒为正数，
    //      负值与 IEEE-754 负零永远到不了这里；
    //   2) OvertimePolicy.roundDownToHalfHour 的负数已被上游 Math.max(0, …) 吸收；
    //   3) AttendancePolicy.positiveMinutes 的负跨度在 3 个调用点分别被
    //      Math.max(0, …)、else 归零与「签退不早于签到」吸收。
    // 这些用例只“读”真实实现对边界的处理，不改字节码、不替换方法实现，
    // 属于赛题允许的探针手段；断言失败信息写明入参，便于定位。
    // ==================================================================
    @Nested
    @DisplayName("私有入口边界探针")
    class PrivateEntryBoundaryProbes {

        /** 反射取得私有静态方法，parameterTypes 用于区分同名重载。 */
        private java.lang.reflect.Method lookupPrivateStatic(Class<?> owner, String methodName, Class<?>... parameterTypes) {
            try {
                // 只取当前类自己声明的方法，避免误拿到父类同名方法。
                java.lang.reflect.Method method = owner.getDeclaredMethod(methodName, parameterTypes);
                // 私有方法必须放开访问检查，否则 invoke 会抛 IllegalAccessException。
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException missingMethod) {
                // 方法不存在说明被测结构与预期不符，直接失败而不是静默跳过。
                throw new AssertionError("被测私有方法不存在：" + owner.getName() + "#" + methodName, missingMethod);
            }
        }

        /** 反射调用私有静态方法；被测方法内部抛出的异常原样上抛，避免被反射包装掩盖。 */
        private Object invokePrivateStatic(java.lang.reflect.Method method, Object... arguments) {
            try {
                // 私有方法为 static，第一个参数（实例）传 null。
                return method.invoke(null, arguments);
            } catch (java.lang.reflect.InvocationTargetException wrapped) {
                // 拆掉反射包装，让被测实现自己的异常直接暴露在失败栈里。
                throw new AssertionError("被测方法内部抛出异常：" + wrapped.getCause(), wrapped.getCause());
            } catch (ReflectiveOperationException reflectionFailure) {
                // 访问失败属于测试脚手架问题，单独报告方法名。
                throw new AssertionError("反射调用失败：" + method.getName(), reflectionFailure);
            }
        }

        @Test
        @DisplayName("roundHalfDay(-0.0) 必须归一化成 +0.0，不能把负零泄漏给调用方")
        void roundHalfDayNormalizesNegativeZeroToPositiveZero() {
            // roundHalfDay 只接收一个 double，无重载。
            java.lang.reflect.Method roundHalfDay = lookupPrivateStatic(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

            // -0.0 是 IEEE-754 中唯一「数值等于 0、符号位为 1」的取值；
            // 判据从 value<=0 改成 value<0 时 Math.ceil(-0.0)/2 会保留负零，
            // 因此必须用原始位比较，assertEquals(0.0, …) 无法区分正负零。
            double negativeZeroResult = (Double) invokePrivateStatic(roundHalfDay, -0.0d);
            assertEquals(0L, Double.doubleToRawLongBits(negativeZeroResult),
                    "roundHalfDay(-0.0) 应返回符号位为 0 的正零");

            // 普通正零与小幅负值同样应落到 0（避免 -0.4 被向上取整成 0.5）。
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, 0.0d), 0.0d);
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -0.4d), 0.0d);
        }

        @Test
        @DisplayName("roundHalfDay 把负小时数归零，把正小时数按半天向上取整")
        void roundHalfDayClampsNegativeHoursAndRoundsPositiveHoursUp() {
            java.lang.reflect.Method roundHalfDay = lookupPrivateStatic(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

            // 负值只能从反射入口进入；真实请假请求的 requestedHours 恒大于 0。
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -1.0d), 0.0d);
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -8.0d), 0.0d);
            // 0.1 天不足半天，向上取整为 0.5 天。
            assertEquals(0.5d, (Double) invokePrivateStatic(roundHalfDay, 0.1d), 0.0d);
            // 恰好半天保持 0.5，不额外进位。
            assertEquals(0.5d, (Double) invokePrivateStatic(roundHalfDay, 0.5d), 0.0d);
            // 0.6 天超过半天，向上取整为整天。
            assertEquals(1.0d, (Double) invokePrivateStatic(roundHalfDay, 0.6d), 0.0d);
            // 整天数原样返回。
            assertEquals(8.0d, (Double) invokePrivateStatic(roundHalfDay, 8.0d), 0.0d);
        }

        @Test
        @DisplayName("validateUnit：不足半天按半天计费，恰好半天与零计费都不告警")
        void validateUnitReportsMinimumChargeOnlyBelowHalfDay() {
            // validateUnit(请求, 计费天数, 违规列表) 为私有静态方法。
            java.lang.reflect.Method validateUnit = lookupPrivateStatic(AdvancedLeavePolicy.class, "validateUnit",
                    AdvancedLeaveRequest.class, double.class, List.class);

            // 全日制请假请求作为固定上下文，保证只有「计费天数」一个变量。
            AdvancedLeaveRequest fullDayRequest = new AdvancedLeaveRequest("EMP-3", EmployeeCategory.REGULAR,
                    LeaveType.ANNUAL, LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5),
                    LeaveUnit.FULL_DAY, 0.0d, "annual leave request", false, false, false, 24, 5, 10.0d, 0.0d);

            // 0.25 天低于半天门槛，必须给出「minimum charge is half a day」。
            List<String> quarterDayMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.25d, quarterDayMessages);
            assertEquals(List.of("minimum charge is half a day"), quarterDayMessages);

            // 恰好 0.5 天是等值点，不得告警。
            List<String> halfDayMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.5d, halfDayMessages);
            assertEquals(List.of(), halfDayMessages);

            // 0 天表示不产生计费，也不得告警（防止 < 写成 <= 时误报）。
            List<String> zeroMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.0d, zeroMessages);
            assertEquals(List.of(), zeroMessages);
        }

        @Test
        @DisplayName("roundDownToHalfHour 把负分钟数归零，并按 30 分钟向下取整")
        void roundDownToHalfHourClampsNegativeMinutesToZero() {
            java.lang.reflect.Method roundDownToHalfHour = lookupPrivateStatic(OvertimePolicy.class, "roundDownToHalfHour", int.class);

            // 负分钟数在公开链路已被 Math.max(0, …) 截断，只能直调观察。
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -40));
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -30));
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -1));
            // 29 分钟不足半小时，向下取整为 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 29));
            // 恰好 30 分钟保留 30，是 `minutes < 30` 的等值点。
            assertEquals(30, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 30));
            // 89 分钟向下取整为 60，验证按 30 分钟粒度取整。
            assertEquals(60, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 89));
        }

        @Test
        @DisplayName("positiveMinutes 把倒序时间段（签退早于签到）夹到 0")
        void positiveMinutesClampsReversedTimeSpanToZero() {
            java.lang.reflect.Method positiveMinutes = lookupPrivateStatic(AttendancePolicy.class, "positiveMinutes",
                    LocalDateTime.class, LocalDateTime.class);
            // 固定日期，避免依赖机器时区与当前时间。
            LocalDateTime nineOClock = LocalDateTime.of(2026, 5, 5, 9, 0);
            LocalDateTime sixOClock = LocalDateTime.of(2026, 5, 5, 18, 0);

            // 起点晚于终点（倒序跨度）应得到 0，而不是负分钟数。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, sixOClock, nineOClock));
            // 正序跨度 9 小时 = 540 分钟。
            assertEquals(540, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, sixOClock));
            // 零跨度得到 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, nineOClock));
            // 不足 1 分钟的跨度按 toMinutes() 截断后仍为 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, nineOClock.plusSeconds(30)));
        }

        @Test
        @DisplayName("categoryCap 对全部「费用类别 × 员工职级」组合恒为正，零额度保护分支不可达")
        void categoryCapIsAlwaysPositiveForEveryCategoryAndGrade() {
            // categoryCap(费用项, 报销单) 为私有静态方法，返回该类别该职级的额度上限。
            java.lang.reflect.Method categoryCap = lookupPrivateStatic(ExpensePolicy.class, "categoryCap",
                    ExpenseItem.class, ExpenseClaim.class);
            // 额度下限：最低档类别的基础额度乘以最低职级系数后仍应 ≥ 80。
            BigDecimal minimumExpectedCap = new BigDecimal("80");

            // 遍历 11 个费用类别 × 3 个职级，证明 allowedAmount 里的 cap.signum()==0 不可达。
            for (ExpenseCategory category : ExpenseCategory.values()) {
                for (EmployeeGrade grade : EmployeeGrade.values()) {
                    // 固定 10 元、CNY、当天的小额消费，避免金额本身影响类别额度。
                    ExpenseItem probeItem = new ExpenseItem("ITEM-" + category, category, new BigDecimal("10.00"),
                            Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Shop",
                            "probe purpose text", ReceiptType.TAX_INVOICE, PaymentMethod.PERSONAL_CARD,
                            0, false, false, false);
                    // 单条明细的报销单，职级来自外层循环。
                    ExpenseClaim probeClaim = new ExpenseClaim("CLM-CAP", "EMP-CAP", grade,
                            LocalDate.of(2026, 5, 1), "CC-1", List.of(probeItem), false, false, false, true, 0);

                    BigDecimal capAmount = (BigDecimal) invokePrivateStatic(categoryCap, probeItem, probeClaim);
                    // 额度必须为正数：为 0 意味着 allowedAmount 会走不可达的早退分支。
                    assertTrue(capAmount.signum() > 0, "类别/职级额度为 0：" + category + "/" + grade);
                    // 额度还必须不低于最低地板价，防止切换表达式后静默变小。
                    assertTrue(capAmount.compareTo(minimumExpectedCap) >= 0,
                            "类别/职级额度低于地板价：" + category + "/" + grade + " => " + capAmount);
                }
            }
        }

        @Test
        @DisplayName("超过四小时的加班告警已由应付封顶置位，raw>360 的额外置位不可观察")
        void fourHourCapAlreadyRequiresManagerReview() {
            // 标准班次 09:00-18:00，含 60 分钟午休，排班 480 分钟。
            Shift standardShift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    0, 60, 480);
            // 09:00 签到、21:00 签退，实际工作 660 分钟，产生 180 分钟以上加班。
            PunchRecord longPunch = new PunchRecord(LocalDateTime.of(2026, 5, 5, 9, 0),
                    LocalDateTime.of(2026, 5, 5, 21, 0), 60, false, true, false, false);
            // 办公模式、非节假日、非远程，排除其它置位 review 的分支。
            AttendanceInput attendanceInput = new AttendanceInput("EMP-OT", LocalDate.of(2026, 5, 5), standardShift,
                    longPunch, EmploymentMode.OFFICE, true, false, false, false, false, true, true, 0, 0);

            // raw=361 时 payable 被封顶到 240，review 已为 true。
            OvertimeResult aboveThreshold = new OvertimePolicy().evaluate(attendanceInput, 660, 361);
            assertEquals(240, aboveThreshold.payableMinutes());
            assertEquals("daily overtime capped at four hours", aboveThreshold.reason());
            assertTrue(aboveThreshold.managerReview());

            // raw=360（等值点）同样被封顶，两个输入的可观察结果完全一致，
            // 因此 `rawOvertimeMinutes > 360` 的边界与跳转变异不可区分。
            OvertimeResult atThreshold = new OvertimePolicy().evaluate(attendanceInput, 660, 360);
            assertEquals(240, atThreshold.payableMinutes());
            assertEquals("daily overtime capped at four hours", atThreshold.reason());
            assertTrue(atThreshold.managerReview());
        }

        @Test
        @DisplayName("非对公报销单缺少成本中心只产生告警，不会升级为致命违规")
        void missingCostCenterIsOnlyAWarningWhenNotBillable() {
            // 40 元工作餐明细，抬头为个人卡。
            ExpenseItem mealItem = new ExpenseItem("ITEM-1", ExpenseCategory.MEAL, new BigDecimal("40.00"),
                    Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Cafe Uno",
                    "team lunch with client", ReceiptType.NONE, PaymentMethod.PERSONAL_CARD,
                    0, false, false, false);
            // costCenter 传 null，clientBillable=false：这是公开构造器允许的唯一「缺成本中心」组合。
            ExpenseClaim nonBillableClaim = new ExpenseClaim("CLM-3", "EMP-3", EmployeeGrade.STAFF,
                    LocalDate.of(2026, 5, 1), null, List.of(mealItem), false, false, false, true, 0);

            // 汇率恒为 1，避免金额换算干扰断言。
            ExpenseAssessment assessment = new ExpensePolicy()
                    .assess(nonBillableClaim, (currency, date) -> BigDecimal.ONE, LocalDate.of(2026, 5, 10));

            // 不拒单、无违规，只在告警里提示成本中心缺失。
            assertFalse(assessment.rejected());
            assertEquals(List.of(), assessment.violations());
            assertTrue(assessment.warnings().contains("cost center is missing"),
                    "warnings：" + assessment.warnings());
        }
    }
