/**
 * 
 */
package org.opentoutatice.ecm.feature.news.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import junit.framework.Assert;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.opentoutatice.ecm.feature.news.consistency.DateRepairer;
import org.opentoutatice.ecm.feature.news.scanner.DateUpdaterTools;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;


/**
 * @author david
 *
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class DateToolsTest {

    /** Daily constants. */
    private static final int DAILY_BOUNDARY = 240;

    /** Weekly constants. */
    private static final int WEEKLY_BOUNDARY = 720;

    /** Formatters. */
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final SimpleDateFormat formatterWithDayName = new SimpleDateFormat("EEEEE dd-MM-yyyy HH:mm:ss");

    /** Print dates or not. */
    private static final boolean PRINT = false;
    /** Temprary: to avoid skip test in mvn install. */
    private static final boolean TEST = false;

    /**
     * {@link DateUpdaterTools#computeNextDate(NewsPeriod, Date, int, boolean)} takes current Date in entry.
     * We test here:
     * <ul>
     * <li>initialization of NextDate from current Date if notification is daily or weekly</li>
     * <li>update of NextDate from current Date and current Date + n days when daily and current Date + n weeks when weekly</li>
     * </ul>
     * 
     * @throws ParseException
     */
    @Test
    public void testNextDate() throws ParseException {
        if (TEST) {

            Calendar calendar = Calendar.getInstance();
            Date currentCronDate = getDateBeetweenHours(calendar, 20, 28);

            if (PRINT) {
                System.out.println("[INPUT]: " + formatter.format(currentCronDate));
            }

            for (int index = 0; index < 10; index++) {
                // Check daily init
                Date initDate = checkNextDate(calendar, currentCronDate, NewsPeriod.daily, 1, DAILY_BOUNDARY, true);
                // Check daily update
                Date nextCurrentDate = initDate;
                for (int indexDu = 0; indexDu < 100; indexDu++) {
                    checkNextDate(calendar, nextCurrentDate, NewsPeriod.daily, 1, DAILY_BOUNDARY, false);
                    nextCurrentDate = DateUtils.addDays(nextCurrentDate, 1);
                }

                if (PRINT) {
                    System.out.println(" ------------------------ ");
                }

                // Check weekly init
                Date initWeeklyDate = checkNextDate(calendar, currentCronDate, NewsPeriod.weekly, 7, WEEKLY_BOUNDARY, true);
                // Must be a Sunday
                calendar.setTime(initWeeklyDate);
                Assert.assertTrue(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY);

                // Check weekly update
                nextCurrentDate = initWeeklyDate;
                for (int indexWu = 0; indexWu < 100; indexWu++) {
                    Date nextWeeklyDate = checkNextDate(calendar, nextCurrentDate, NewsPeriod.weekly, 7, WEEKLY_BOUNDARY, false);
                    // Must be a Sunday before 00 or Monday after 00
                    calendar.setTime(nextWeeklyDate);
                    Assert.assertTrue(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY);

                    nextCurrentDate = DateUtils.addDays(nextCurrentDate, 7);
                }

                if (PRINT) {
                    System.out.println(" ===================== ");
                }
            }

        }
    }

    @Test
    public void testCheckNRepair() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        // Get date not beetween a Sunday at 12:00 and Sunday at 12:00
        if (PRINT) {
            System.out.println("Around Sunday at 12:00 and Monday at 12:00? ");
        }

        for (int nbTests = 0; nbTests < 100; nbTests++) {
            Date randomDate = getRandomDate(calendar);
            // Date randomDate = formatter.parse("19-12-2021 10:09:00");
            Date aroundSundayDate = DateRepairer.checkDateNRepair(NewsPeriod.weekly, randomDate, WEEKLY_BOUNDARY);

            if (PRINT) {
                if (aroundSundayDate != null) {
                    System.out.println(formatterWithDayName.format(randomDate) + " -> " + formatterWithDayName.format(aroundSundayDate));
                } else {
                    System.out.println(formatterWithDayName.format(randomDate) + " -> No need");
                }
            }

            if (aroundSundayDate != null) {
                assertAroundMonday(calendar, aroundSundayDate);
            } else {
                assertAroundMonday(calendar, randomDate);
            }

        }
    }

    /**
     * @param calendar
     * @param aroundSundayDate
     */
    protected void assertAroundMonday(Calendar calendar, Date aroundSundayDate) {
        calendar.setTime(aroundSundayDate);

        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        Assert.assertTrue((day == Calendar.SUNDAY && minutes >= WEEKLY_BOUNDARY) || (day == Calendar.MONDAY && minutes <= WEEKLY_BOUNDARY));
    }

    /**
     * @param calendar
     * @param inputDate
     */
    protected Date checkNextDate(Calendar calendar, Date inputDate, NewsPeriod newsPeriod, int addedDays, int boundary, boolean init) {
        // Given date
        calendar.setTime(inputDate);

        // Min Date
        Date minDate = getMinDate(inputDate, newsPeriod, addedDays, boundary, init);

        // Max Date
        Date maxDate = DateUtils.addMinutes(minDate, 2 * boundary);

        // Test
        Date nextDate = null;
        try {
            nextDate = (Date) DateUpdaterTools.computeNextDate(newsPeriod, inputDate, boundary, init);

            // Day
            if (PRINT) {
                String uc = init ? "[Init]:   " : "[Update]: ";
                System.out.println(uc + formatter.format(minDate) + " < " + formatter.format(nextDate) + " < " + formatter.format(maxDate));
            }

            Assert.assertTrue(nextDate.compareTo(minDate) >= 0);
            Assert.assertTrue(nextDate.compareTo(maxDate) <= 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nextDate;
    }

    /**
     * @param inputDate
     * @param newsPeriod
     * @param addedDays
     * @param boundary
     * @param init
     * @param minDate
     * @return
     */
    protected Date getMinDate(Date inputDate, NewsPeriod newsPeriod, int addedDays, int boundary, boolean init) {
        Date minDate = null;
        if (init) {
            if (NewsPeriod.daily.equals(newsPeriod)) {
                minDate = shiftDate(DateUpdaterTools.setMidnight(inputDate), boundary);
                minDate = DateUtils.addDays(minDate, 1);
            } else if (NewsPeriod.weekly.equals(newsPeriod)) {
                inputDate = DateUpdaterTools.setMidnight(inputDate);
                Date nextMonday = DateUpdaterTools.getNextMonday(inputDate);
                // nextMonday = DateUtils.addDays(nextMonday, 1);
                minDate = shiftDate(nextMonday, boundary);
            }
        } else {
            minDate = DateUtils.addMinutes(DateUpdaterTools.setMidnight(inputDate, boundary), -boundary);
            minDate = DateUtils.addDays(minDate, addedDays);
        }
        return minDate;
    }

    private static final int MINUTES_IN_YEAR = 365 * 24 * 60;

    protected Date getRandomDate(Calendar calendar) {
        int minutesInYear = ThreadLocalRandom.current().nextInt(0, MINUTES_IN_YEAR + 1);
        return DateUtils.addMinutes(calendar.getTime(), minutesInYear);
    }

    /**
     * @param calendar
     * @return
     */
    protected Date getDateBeetweenHours(Calendar calendar, int minHour, int maxHour) {
        int hours = ThreadLocalRandom.current().nextInt(minHour, maxHour);
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        int minutes = ThreadLocalRandom.current().nextInt(0, 60);
        calendar.set(Calendar.MINUTE, minutes);

        // Result
        return calendar.getTime();
    }

    private Date shiftDate(Date date, int boundary) {
        return DateUtils.addMinutes(date, -boundary);
    }

    @Test
    public void testComputeNextDate() throws ParseException {

        Date currentDate = formatter.parse("20-07-2017 20:15:00");
        // Date nextDate = formatter.parse("7-07-2017 22:15");

        System.out.println("INIT ===");

        Date nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.daily, currentDate, DAILY_BOUNDARY, true);
        System.out.println("Next Daily: " + formatter.format(nextDate));

        nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.weekly, currentDate, WEEKLY_BOUNDARY, true);
        System.out.println("Next Weekly repaired: " + formatter.format(nextDate));

        currentDate = formatter.parse("24-07-2017 09:25:14");
        nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.weekly, currentDate, WEEKLY_BOUNDARY, true);
        System.out.println("Next Weekly: " + formatter.format(nextDate));

        System.out.println("UPDATE ===");

        currentDate = formatter.parse("20-07-2017 20:15:00");

        nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.daily, currentDate, DAILY_BOUNDARY, false);
        System.out.println("Next Daily: " + formatter.format(nextDate));

        nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.weekly, currentDate, WEEKLY_BOUNDARY, false);
        System.out.println("Next Weekly repaired: " + formatter.format(nextDate));

        currentDate = formatter.parse("24-07-2017 09:25:14");
        nextDate = DateUpdaterTools.computeNextDate(NewsPeriod.weekly, currentDate, WEEKLY_BOUNDARY, false);
        System.out.println("Next Weekly: " + formatter.format(nextDate));

    }

}
