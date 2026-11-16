package net.mooctest.card;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/*
 * Date formatter used by the DateChecker to parse month && year
 * out of the expiration date
 */

public class DateFormatter {

    /*
     * @return a year SimpleDateFormat
     */
    public static DateFormat yearFormat() {
        DateFormat yearFormat = new SimpleDateFormat("yy");

        return yearFormat;
    }

    /*
     * @return a month SimpleDateFormat
     */
    public static DateFormat monthFormat() {
        DateFormat monthFormat = new SimpleDateFormat("MM");

        return monthFormat;
    }
}
