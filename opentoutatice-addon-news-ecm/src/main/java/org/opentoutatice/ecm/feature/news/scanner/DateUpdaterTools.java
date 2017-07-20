/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;


/**
 * @author david
 *
 */
public class DateUpdaterTools {

    /** Logger. */
    private static final Log log = LogFactory.getLog(DateUpdaterTools.class);

    /** Date time format. */
    public static final String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm";

    /** Date time format for query. */
    public static final String DATE_TIME_QUERY_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** Next daily interval. */
    public static final String NEXT_DAILY_BOUNDARY = "nextDailyBoundary";
    /** Next weekly interval. */
    public static final String NEXT_WEEKLY_BOUNDARY = "nextWeeklyBoundary";
    /** Next error interval. */
    public static final String NEXT_ERROR_BOUNDARY = "nextErrorBoundary";

    /**
     * Utility class.
     */
    private DateUpdaterTools() {
        super();
    }

    /**
     * 
     * @param newsPeriod
     * @param boundary
     * @return
     */
    public static Date computeNextDate(NewsPeriod newsPeriod, Date inputDate, int boundary, boolean init) {
        // Result
        Date nextDate = null;

        // Initialization
        switch (newsPeriod) {

            case daily:
                // + one day at 00:00 +/- random time in boundary
                Date addedDayDate = setMidnight(inputDate, boundary);
                addedDayDate = DateUtils.addDays(addedDayDate, 1);

                nextDate = getNextRandomDate(addedDayDate, boundary);
                break;

            case weekly:
                // Next Sunday at 00:00 +/- random time in boundary
                Date addedWeekDate = setMidnight(inputDate, boundary);
                addedWeekDate = getNextMonday(addedWeekDate);

                nextDate = getNextRandomDate(addedWeekDate, boundary);
                break;

            case none:
                break;

            case error:
                // + one hour +/- random time in boundary
                nextDate = DateUtils.addHours(inputDate, 2);
                break;

        }

        return nextDate;
    }

    /**
     * Gets next Sunday from date.
     * 
     * @param inputDate
     * @return next Sunday
     */
    public static Date getNextMonday(Date inputDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(inputDate);

        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int shiftNextSunday = currentDay != Calendar.MONDAY ? 9 - currentDay : 7;

        return DateUtils.addDays(inputDate, shiftNextSunday);
    }

    /**
     * Used for dev.
     * 
     * @param inputDate
     * @return
     */
    public static Date getNextTestRandomDate(Date inputDate) {
        // Gap in minutes
        int gap = Integer.valueOf(Framework.getProperty("ottc.news.scan.dev.gap", "2"));
        return DateUtils.addMinutes(inputDate, gap);
    }

    /**
     * Get next randon Date at 00:00 +/- boundary.
     * 
     * @param inputDate
     * @param gap
     * @param boundary
     * @return Date
     */
    public static Date getNextRandomDate(Date inputDate, int boundary) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(inputDate);

        // Shift
        int shift = ThreadLocalRandom.current().nextInt((-1) * boundary, boundary);

        // Build date
        calendar.set(Calendar.MINUTE, shift);
        return calendar.getTime();
    }

    /**
     * Sets given date (which is in daily, weekly, ... boundaries) at midnight.
     * 
     * @param calendar
     */
    public static Date setMidnight(Date date, int boundary) {
        // Check if we must add or remove time to go to midnight
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int day = calendar.get(Calendar.DAY_OF_WEEK);

        int minutes = calendar.get(Calendar.MINUTE);
        calendar.set(Calendar.MINUTE, minutes + boundary);

        if (day != calendar.get(Calendar.DAY_OF_WEEK)) {
            calendar.setTime(DateUtils.addDays(date, 1));
        } else {
            calendar.set(Calendar.MINUTE, minutes);
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar.getTime();
    }

    /**
     * Set midnight for given day.
     * 
     * @param calendar
     * @param date
     * @return date
     */
    public static Date setMidnight(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar.getTime();
    }

}
