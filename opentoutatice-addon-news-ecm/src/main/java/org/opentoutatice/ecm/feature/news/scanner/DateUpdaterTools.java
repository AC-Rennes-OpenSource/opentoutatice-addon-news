/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.util.Date;
import java.util.Random;

import org.apache.commons.lang.time.DateFormatUtils;
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

    /** Randon generator. */
    private static final Random rand = new Random();

    /**
     * Utility class.
     */
    private DateUpdaterTools() {
        super();
    }

    /**
     * Gets an random integer in [-boundary, + boundary].
     * 
     * @param boundary
     * @return
     */
    public static int getRandomIntIn(int boundary) {
        // return rand.nextInt(2 * boundary) - boundary;
        int rInt = rand.nextInt(boundary);
        // Sign
        int sign = rInt % 2 == 0 ? 1 : -1;

        return sign * rInt;
    }

    /**
     * 
     * @param newsPeriod
     * @param boundary
     * @return
     */
    public static Date initializeNextDate(NewsPeriod newsPeriod, Date inputDate, int boundary) {
        // Next date
        Date nextDate = null;
        // For debug
        String inputStrDate = DateFormatUtils.format(inputDate, DATE_TIME_FORMAT);

        // Initialization
        switch (newsPeriod) {

            case daily:
             // + one day at 00:00 +/- random time in boundary
                Date addedDayDate = DateUtils.addDays(inputDate, 1);
                nextDate = getNextRandomDate(addedDayDate, boundary);
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Daily] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                }
                
                if(NewsUpdater.isTestModeSet()){
                 // Gap in minutes
                    int gap = Integer.valueOf(Framework.getProperty("ottc.news.scan.daily.test.gap", "10"));
                    nextDate = DateUtils.addMinutes(inputDate, gap);
                    
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [Daily] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                    }
                }
                
                break;

            case weekly:
             // + one week at 00:00 +/- random time in boundary
                Date addedWeekDate = DateUtils.addWeeks(inputDate, 1);
                nextDate = getNextRandomDate(addedWeekDate, boundary);
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Weekly] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                }
                

                if(NewsUpdater.isTestModeSet()){
                 // Gap in minutes
                    int gap = Integer.valueOf(Framework.getProperty("ottc.news.scan.weekly.test.gap", "10"));
                    nextDate = DateUtils.addMinutes(inputDate, gap);
                    
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [Weekly] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                    }
                }

                break;
                
            case none:
                if(NewsUpdater.isTestModeSet()){
                 // Gap in minutes
                    int gap = Integer.valueOf(Framework.getProperty("ottc.news.scan.none.test.gap", "10"));
                    nextDate = DateUtils.addMinutes(inputDate, gap);
                    
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [None] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                    }
                }

                break;

            case error:
             // + one hour +/- random time in boundary
                Date addedHourDate = DateUtils.addHours(inputDate, 1);
                nextDate = getNextRandomDate(addedHourDate, boundary);
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Error] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                }
                
                if(NewsUpdater.isTestModeSet()){
                    // Gap in minutes
                       int gap = Integer.valueOf(Framework.getProperty("ottc.news.scan.error.test.gap", "10"));
                       nextDate = DateUtils.addMinutes(inputDate, gap);
                       
                       if(log.isDebugEnabled()){
                           log.debug("[MODE TEST] [Error] NextDate = " + inputStrDate + " -> " + DateFormatUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
                       }
                   }

                break;

        }

        return nextDate;
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
        // ms to shift (can be negative)
        int msToShift = getRandomIntIn(boundary) * 60000;

        // Date computed from 1970-01-01 00:00:00
        long inputMs = inputDate.getTime();
        long time = inputMs + msToShift;

        Date nextDate = new Date(0);
        nextDate.setTime(time);

        return nextDate;
    }

    /**
     * @param boundary
     * @param midnightDate
     * @return
     */
    public static Date getRandomDateAroundMidnight(int boundary, Date inputDate) {
        // Set to midnight
        Date midnightDate = DateUtils.setHours(inputDate, 0);
        return getRandomDateAroundMidnight(boundary, midnightDate);
    }

    // For test ....
    public static Date initializeTestNextDate(NewsPeriod newsPeriod, Date inputDate, int boundary) {
        // Next date
        Date nextDate = null;

        // Initialization
        switch (newsPeriod) {
            case none:

                break;

            case daily:
                // + one day at 00:00 +/- random time in boundary
                Date addedDayDate = DateUtils.addDays(inputDate, 1);
                nextDate = getNextRandomDate(addedDayDate, boundary);

                break;

            case weekly:
                // + one week at 00:00 +/- random time in boundary
                Date addedWeekDate = DateUtils.addWeeks(inputDate, 1);
                nextDate = getNextRandomDate(addedWeekDate, boundary);

                break;

            case error:
                // + one hour +/- random time in boundary
                Date addedHourDate = DateUtils.addHours(inputDate, 1);
                nextDate = getNextRandomDate(addedHourDate, boundary);

                break;

        }

        return nextDate;
    }

}
