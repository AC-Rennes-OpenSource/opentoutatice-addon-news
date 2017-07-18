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

    /** Formatter. */
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    /** Print dates or not. */
    private static final boolean PRINT = false;
    /** Temprary: to avoid skip test in mvn install. */
    private static final boolean TEST = false;

    @Test
    public void testNextDate() throws ParseException {
        if (TEST) {

            Calendar calendar = Calendar.getInstance();
            // Date inputDate = calendar.getTime();
            Date inputDate = getRandomDate(calendar);

            if (PRINT) {
                System.out.println("[INPUT]: " + formatter.format(inputDate));
            }

            for (int index = 0; index < 10; index++) {
                // Check daily init
                Date initDate = checkNextDate(calendar, inputDate, NewsPeriod.daily, 1, DAILY_BOUNDARY, true);
                // Check daily update
                for (int indexDu = 0; indexDu < 100; indexDu++) {
                    Date nextDailyDate = checkNextDate(calendar, initDate, NewsPeriod.daily, 1, DAILY_BOUNDARY, false);
                    initDate = nextDailyDate;
                }

                if (PRINT) {
                    System.out.println(" ------------------------ ");
                }

                // Check weekly init
                Date initWeeklyDate = checkNextDate(calendar, inputDate, NewsPeriod.weekly, 7, WEEKLY_BOUNDARY, true);
                // Must be a Sunday
                calendar.setTime(initWeeklyDate);
                Assert.assertTrue(calendar.get(Calendar.DAY_OF_WEEK) == 1 || calendar.get(Calendar.DAY_OF_WEEK) == 2);

                // Check weekly update
                for (int indexWu = 0; indexWu < 100; indexWu++) {
                    Date nextWeeklyDate = checkNextDate(calendar, initWeeklyDate, NewsPeriod.weekly, 7, WEEKLY_BOUNDARY, false);
                    // Must be a Sunday before 00 or Monday after 00
                    calendar.setTime(nextWeeklyDate);
                    Assert.assertTrue(calendar.get(Calendar.DAY_OF_WEEK) == 1 || calendar.get(Calendar.DAY_OF_WEEK) == 2);

                    initWeeklyDate = nextWeeklyDate;
                }

                if (PRINT) {
                    System.out.println(" ===================== ");
                }
            }

        }
    }


    /**
     * @param calendar
     * @param inputDate
     */
    protected Date checkNextDate(Calendar calendar, Date inputDate, NewsPeriod newsPeriod, int addedDays, int boundary, boolean init) {
        // Given date
        calendar.setTime(inputDate);

        // Min Date
        Date minDate = null;
        if (init) {
            if (NewsPeriod.daily.equals(newsPeriod)) {
                minDate = shiftDate(DateUpdaterTools.setMidnight(inputDate), boundary);
                minDate = DateUtils.addDays(minDate, 1);
            } else if (NewsPeriod.weekly.equals(newsPeriod)) {
                Date nextSunday = DateUpdaterTools.setMidnight(DateUpdaterTools.getNextSunday(inputDate));
                nextSunday = DateUtils.addDays(nextSunday, 1);
                minDate = shiftDate(nextSunday, boundary);
            }
        } else {
            minDate = DateUtils.addMinutes(DateUpdaterTools.setMidnight(inputDate, boundary), -boundary);
            minDate = DateUtils.addDays(minDate, addedDays);
        }

        // Max Date
        Date maxDate = DateUtils.addMinutes(minDate, 2 * boundary);

        // Test
        Date nextDate = null;
        try {
            nextDate = (Date) DateUpdaterTools.computeNextDate(newsPeriod, inputDate, boundary, init);

            // Day
            if (PRINT) {
                System.out.println(formatter.format(minDate) + " < " + formatter.format(nextDate) + " < " + formatter.format(maxDate));
            }

            Assert.assertTrue(nextDate.compareTo(minDate) >= 0);
            Assert.assertTrue(nextDate.compareTo(maxDate) <= 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nextDate;
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

}
