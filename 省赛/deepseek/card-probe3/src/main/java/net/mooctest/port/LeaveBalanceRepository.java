package net.mooctest.port;

/** Annual-leave balance boundary. */
public interface LeaveBalanceRepository {

    int remainingAnnualDays(String employeeId);

    void deductAnnualDays(String employeeId, int days);
}
