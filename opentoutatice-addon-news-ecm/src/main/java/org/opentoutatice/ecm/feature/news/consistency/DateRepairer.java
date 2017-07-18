/**
 * 
 */
package org.opentoutatice.ecm.feature.news.consistency;

import java.util.Calendar;
import java.util.Date;

import org.opentoutatice.ecm.feature.news.scanner.DateUpdaterTools;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;


/**
 * @author david
 *
 */
public class DateRepairer {

    /**
     * Utility class.
     */
    private DateRepairer() {
        super();
    }

    /**
     * Checks, according to period, if given date must be repaired and repair it if necessary.
     * 
     * @param period
     * @param dateToCheck
     */
    public static Date checkDateNRepair(NewsPeriod period, Date dateToCheck, int boundary) {
        // Corrected date
        Date repairedDate = null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateToCheck);

        switch (period) {
            case weekly:
                // Date must be a Sunday around midnight
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

                if (!((day == Calendar.SUNDAY && minutes >= boundary) || (day == Calendar.MONDAY && minutes <= boundary))) {
                    repairedDate = DateUpdaterTools.getNextSunday(dateToCheck);
                }
                break;

            default:
                break;
        }

        return repairedDate;
    }

}
