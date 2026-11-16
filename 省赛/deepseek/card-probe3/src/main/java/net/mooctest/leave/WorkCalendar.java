package net.mooctest.leave;

import java.time.LocalDate;

/** Calendar boundary; tests can replace it with a lambda or Mockito mock. */
@FunctionalInterface
public interface WorkCalendar {

    boolean isWorkingDay(LocalDate date);
}
