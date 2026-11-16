
    // ===== merged from CardProbeTests3 probe =====
    @org.junit.jupiter.api.Nested
    class CardProbeTests3 {


    private static final LocalDate PROBE3_TODAY = LocalDate.of(2026, 5, 10);

    @Test
    @DisplayName("roundHalfDay: negative zero must still be clamped to positive zero")
    void probe3RoundHalfDayNegativeZeroBoundary() {
        Method round = probe3Method(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

        double negativeZero = (Double) probe3Invoke(round, -0.0d);
        assertEquals(0L, Double.doubleToRawLongBits(negativeZero),
                "roundHalfDay(-0.0) must return +0.0, not -0.0");

        assertEquals(0.0d, (Double) probe3Invoke(round, 0.0d), 0.0d);
        assertEquals(0.0d, (Double) probe3Invoke(round, -0.4d), 0.0d);
    }

    @Test
    @DisplayName("roundHalfDay: negative hour requests are clamped to zero days")
    void probe3RoundHalfDayClampsNegativeHours() {
        Method round = probe3Method(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

        assertEquals(0.0d, (Double) probe3Invoke(round, -1.0d), 0.0d);
        assertEquals(0.0d, (Double) probe3Invoke(round, -8.0d), 0.0d);
        assertEquals(0.5d, (Double) probe3Invoke(round, 0.1d), 0.0d);
        assertEquals(0.5d, (Double) probe3Invoke(round, 0.5d), 0.0d);
        assertEquals(1.0d, (Double) probe3Invoke(round, 0.6d), 0.0d);
        assertEquals(8.0d, (Double) probe3Invoke(round, 8.0d), 0.0d);
    }

    @Test
    @DisplayName("validateUnit: a sub-half-day charge is reported, exactly half a day is allowed")
    void probe3ValidateUnitMinimumCharge() {
        Method validateUnit = probe3Method(AdvancedLeavePolicy.class, "validateUnit",
                AdvancedLeaveRequest.class, double.class, List.class);
        AdvancedLeaveRequest request = probe3FullDayRequest();

        List<String> quarterDay = new ArrayList<>();
        probe3Invoke(validateUnit, request, 0.25d, quarterDay);
        assertEquals(List.of("minimum charge is half a day"), quarterDay);

        List<String> exactlyHalfDay = new ArrayList<>();
        probe3Invoke(validateUnit, request, 0.5d, exactlyHalfDay);
        assertEquals(List.of(), exactlyHalfDay);

        List<String> noCharge = new ArrayList<>();
        probe3Invoke(validateUnit, request, 0.0d, noCharge);
        assertEquals(List.of(), noCharge);
    }

    @Test
    @DisplayName("validateClaimLevel: missing cost center is reported as a warning")
    void probe3ClaimWithoutCostCenterIsReported() {
        ExpenseClaim claim = new ExpenseClaim("CLM-3", "EMP-3", EmployeeGrade.STAFF,
                LocalDate.of(2026, 5, 1), null, List.of(probe3MealItem(new BigDecimal("40.00"))),
                false, false, false, true, 0);

        ExpenseAssessment assessment = new ExpensePolicy()
                .assess(claim, (currency, date) -> BigDecimal.ONE, PROBE3_TODAY);

        assertFalse(assessment.rejected());
        assertEquals(List.of(), assessment.violations());
        assertTrue(assessment.warnings().contains("cost center is missing"),
                "warnings: " + assessment.warnings());
    }

    @Test
    @DisplayName("validateClaimLevel: billable claim with a blank cost center is a fatal violation")
    void probe3BillableClaimWithoutCostCenterIsFatal() {
        ExpenseClaim claim = mock(ExpenseClaim.class);
        when(claim.submittedDate()).thenReturn(LocalDate.of(2026, 5, 1));
        when(claim.costCenter()).thenReturn("   ");
        when(claim.clientBillable()).thenReturn(true);
        when(claim.projectActive()).thenReturn(true);
        when(claim.employeeGrade()).thenReturn(EmployeeGrade.STAFF);
        when(claim.items()).thenReturn(List.of(probe3MealItem(new BigDecimal("40.00"))));
        when(claim.previousClaimsThisMonth()).thenReturn(0);
        when(claim.managerPreApproved()).thenReturn(false);

        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Method claimLevel = probe3Method(ExpensePolicy.class, "validateClaimLevel",
                ExpenseClaim.class, LocalDate.class, List.class, List.class);
        probe3Invoke(claimLevel, claim, PROBE3_TODAY, violations, warnings);

        assertTrue(violations.contains("FATAL: client-billable claim requires cost center"),
                "violations: " + violations);
        assertFalse(warnings.contains("cost center is missing"), "warnings: " + warnings);
    }

    @Test
    @DisplayName("validateCategoryRules: meal lines carrying an attendee count are flagged")
    void probe3MealWithAttendeeCountIsFlagged() {
        ExpenseItem item = mock(ExpenseItem.class);
        when(item.category()).thenReturn(ExpenseCategory.MEAL);
        when(item.itemId()).thenReturn("ITEM-MEAL");
        when(item.alcoholIncluded()).thenReturn(false);
        when(item.attendeeCount()).thenReturn(1);

        Method rules = probe3Method(ExpensePolicy.class, "validateCategoryRules",
                ExpenseItem.class, ExpenseClaim.class, BigDecimal.class, List.class, List.class);

        List<String> violations = new ArrayList<>();
        probe3Invoke(rules, item, probe3BillableClaim(), new BigDecimal("40.00"),
                violations, new ArrayList<>());
        assertTrue(violations.contains("meal attendee count belongs under entertainment"),
                "violations: " + violations);

        when(item.attendeeCount()).thenReturn(0);
        List<String> clean = new ArrayList<>();
        probe3Invoke(rules, item, probe3BillableClaim(), new BigDecimal("40.00"),
                clean, new ArrayList<>());
        assertFalse(clean.contains("meal attendee count belongs under entertainment"),
                "violations: " + clean);
    }

    @Test
    @DisplayName("ensureActive: a non-terminal snapshot that is neither pending nor in progress is rejected")
    void probe3EnsureActiveRejectsUnknownActiveState() {
        Method ensureActive = probe3Method(ApprovalWorkflowEngine.class, "ensureActive",
                WorkflowSnapshot.class);

        WorkflowSnapshot stuck = mock(WorkflowSnapshot.class);
        when(stuck.terminal()).thenReturn(false);
        when(stuck.state()).thenReturn(WorkflowState.WITHDRAWN);

        Throwable thrown = probe3InvokeExpectingThrow(ensureActive, stuck);
        assertNotNull(thrown, "ensureActive must reject a non-terminal snapshot that is not active");
        assertEquals(IllegalStateException.class, thrown.getClass());
        assertEquals("workflow is not active", thrown.getMessage());

        when(stuck.state()).thenReturn(WorkflowState.PENDING);
        assertNull(probe3InvokeExpectingThrow(ensureActive, stuck));
    }

    @Test
    @DisplayName("roundDownToHalfHour: negative minutes are clamped to zero")
    void probe3RoundDownToHalfHourClampsNegativeMinutes() {
        Method round = probe3Method(OvertimePolicy.class, "roundDownToHalfHour", int.class);

        assertEquals(0, (int) (Integer) probe3Invoke(round, -40));
        assertEquals(0, (int) (Integer) probe3Invoke(round, -30));
        assertEquals(0, (int) (Integer) probe3Invoke(round, -1));
        assertEquals(0, (int) (Integer) probe3Invoke(round, 29));
        assertEquals(30, (int) (Integer) probe3Invoke(round, 30));
        assertEquals(60, (int) (Integer) probe3Invoke(round, 89));
    }

    @Test
    @DisplayName("positiveMinutes: a reversed span is clamped to zero")
    void probe3PositiveMinutesClampsReversedSpan() {
        Method positiveMinutes = probe3Method(AttendancePolicy.class, "positiveMinutes",
                LocalDateTime.class, LocalDateTime.class);
        LocalDateTime nine = LocalDateTime.of(2026, 5, 5, 9, 0);
        LocalDateTime eighteen = LocalDateTime.of(2026, 5, 5, 18, 0);

        assertEquals(0, (int) (Integer) probe3Invoke(positiveMinutes, eighteen, nine));
        assertEquals(540, (int) (Integer) probe3Invoke(positiveMinutes, nine, eighteen));
        assertEquals(0, (int) (Integer) probe3Invoke(positiveMinutes, nine, nine));
        assertEquals(0, (int) (Integer) probe3Invoke(positiveMinutes, nine, nine.plusSeconds(30)));
    }

    @Test
    @DisplayName("categoryCap is always strictly positive, so the zero-cap guard cannot be entered")
    void probe3CategoryCapIsAlwaysPositive() {
        Method cap = probe3Method(ExpensePolicy.class, "categoryCap", ExpenseItem.class, ExpenseClaim.class);
        BigDecimal floor = new BigDecimal("80");

        for (ExpenseCategory category : ExpenseCategory.values()) {
            for (EmployeeGrade grade : EmployeeGrade.values()) {
                ExpenseItem item = new ExpenseItem("ITEM-" + category, category, new BigDecimal("10.00"),
                        Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Shop",
                        "probe purpose text", ReceiptType.TAX_INVOICE, PaymentMethod.PERSONAL_CARD,
                        0, false, false, false);
                ExpenseClaim claim = new ExpenseClaim("CLM-CAP", "EMP-CAP", grade,
                        LocalDate.of(2026, 5, 1), "CC-1", List.of(item), false, false, false, true, 0);

                BigDecimal capAmount = (BigDecimal) probe3Invoke(cap, item, claim);
                assertTrue(capAmount.signum() > 0, "zero cap for " + category + "/" + grade);
                assertTrue(capAmount.compareTo(floor) >= 0,
                        "cap below floor for " + category + "/" + grade + ": " + capAmount);
            }
        }
    }

    @Test
    @DisplayName("overtime already flagged by the four-hour cap, so raw>360 adds no visible change")
    void probe3OvertimeReviewAlreadyTrueAboveFourHourCap() {
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                0, 60, 480);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(2026, 5, 5, 9, 0),
                LocalDateTime.of(2026, 5, 5, 21, 0), 60, false, true, false, false);
        AttendanceInput input = new AttendanceInput("EMP-OT", LocalDate.of(2026, 5, 5), shift,
                punch, EmploymentMode.OFFICE, true, false, false, false, false, true, true, 0, 0);

        OvertimeResult capped = new OvertimePolicy().evaluate(input, 660, 361);
        assertEquals(240, capped.payableMinutes());
        assertEquals("daily overtime capped at four hours", capped.reason());
        assertTrue(capped.managerReview());

        OvertimeResult boundary = new OvertimePolicy().evaluate(input, 660, 360);
        assertEquals(240, boundary.payableMinutes());
        assertEquals("daily overtime capped at four hours", boundary.reason());
        assertTrue(boundary.managerReview());
    }

    private static AdvancedLeaveRequest probe3FullDayRequest() {
        return new AdvancedLeaveRequest("EMP-3", EmployeeCategory.REGULAR, LeaveType.ANNUAL,
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5), LeaveUnit.FULL_DAY, 0.0d,
                "annual leave request", false, false, false, 24, 5, 10.0d, 0.0d);
    }

    private static ExpenseItem probe3MealItem(BigDecimal amount) {
        return new ExpenseItem("ITEM-1", ExpenseCategory.MEAL, amount,
                Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Cafe Uno",
                "team lunch with client", ReceiptType.NONE, PaymentMethod.PERSONAL_CARD,
                0, false, false, false);
    }

    private static ExpenseClaim probe3BillableClaim() {
        return new ExpenseClaim("CLM-1", "EMP-1", EmployeeGrade.STAFF, LocalDate.of(2026, 5, 1),
                "CC-100", List.of(probe3MealItem(new BigDecimal("40.00"))),
                true, false, true, true, 0);
    }

    private static Method probe3Method(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            Method method = owner.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("missing method " + owner.getName() + "#" + name, e);
        }
    }

    private static Object probe3Invoke(Method method, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            throw new AssertionError("target threw " + e.getCause(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Throwable probe3InvokeExpectingThrow(Method method, Object... arguments) {
        try {
            method.invoke(null, arguments);
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    }
}
