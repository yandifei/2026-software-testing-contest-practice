package net.mooctest.card;

import java.util.Calendar;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

import static net.mooctest.card.DateFormatter.*;

/*
 * Date checker used to validate expiration date against
 * current year and month
 */

public class DateChecker {
    /*
     * Set the current year and month for comparison values
     */
    public static final int CURRENT_YEAR = getCurrentYear();
    public static final int CURRENT_MONTH = getCurrentMonth();

    /*
     * @param accept expiration month/year, compare against
     * current month/year
     * @return boolean representing whether or not the card
     * s expired
     */
    public static boolean compareDates(int date, int currentDate) {
        boolean isValid = false;

        if (date > currentDate) {
            isValid = true;
        }

        return isValid;
    }

    /** A card remains valid through the last day of its expiration month. */
    public static boolean isValidExpiration(YearMonth expiration, Clock clock) {
        Objects.requireNonNull(expiration, "expiration must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        return !expiration.isBefore(YearMonth.now(clock));
    }

    /** Two-digit card years are interpreted as 2000 through 2099. */
    public static boolean isValidExpiration(int month, int year, Clock clock) {
        if (year < 0 || year > 99) {
            throw new IllegalArgumentException("year must contain two digits");
        }
        return isValidExpiration(YearMonth.of(2000 + year, month), clock);
    }

    /*
     * @param String representation of the expirationDate
     * @return expirationDate as a StringBuilder
     */
    public static StringBuilder convertDate(String expirationDate) {
        StringBuilder date = new StringBuilder(expirationDate);

        return date;
    }

    /*
     * @return the current year
     */
    public static int getCurrentYear() {
        int currentYear = Integer.parseInt(yearFormat().format(Calendar.getInstance().getTime()));

        return currentYear;
    }

    /*
     * @return the current month
     */
    public static int getCurrentMonth() {
        int currentMonth = Integer.parseInt(monthFormat().format(Calendar.getInstance().getTime()));

        return currentMonth;
    }
}
