/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.model.SpaceMember;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;
import org.opentoutatice.ecm.scanner.AbstractScanUpdater;


/**
 * @author david
 *
 */
public class NewsUpdater extends AbstractScanUpdater {

    /** Space member model. */
    private SpaceMember member;

    /** Current date. */
    private Date currentDate;


    /**
     * {@inheritDoc}
     */
    @Override
    public Object toModel(Object scannedObject) throws Exception {
        return new SpaceMember((Map<String, Serializable>) scannedObject);
    }

    /**
     * Gets boundary according to news periodicity.
     * 
     * @param newsPeriod
     * @return String
     * @throws Exception
     */
    protected String getBoundaryValue(NewsPeriod newsPeriod) throws Exception {
        // Boundary
        String boundaryValue = null;

        switch (newsPeriod) {
            case daily:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_DAILY_BOUNDARY);
                break;

            case weekly:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_WEEKLY_BOUNDARY);
                break;

            case error:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_ERROR_BOUNDARY);
                break;

            case none:
                boundaryValue = "0";
                break;

            default:
                break;
        }

        return boundaryValue;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean filter(int index, Object scannedObject) throws Exception {
        // Member
        this.member = (SpaceMember) this.toModel(scannedObject);

        if (!Framework.isDevModeSet()) {

            // Member must have subscribed
            boolean hasSubscribed = this.member.hasSubscribed();
            if (!hasSubscribed) {
                // Could have subscribed before
                this.member.setNextNewsDate(index, null);
            }

            // Period subscription
            boolean noPeriod = NewsPeriod.none.equals(this.member.getNewsPeriod());

            // Date condition
            Date nextNewsDate = this.member.getNextNewsDate();
            boolean mustNotify = nextNewsDate != null && nextNewsDate.getTime() < System.currentTimeMillis();

            return hasSubscribed && !noPeriod && mustNotify;

        } else {
            // Dev mode: filter's conditions
            boolean filters = false;

            boolean filterSubscription = Boolean.valueOf(Framework.getProperty("ottc.news.scan.dev.filter.subscr", "false"));

            if (filterSubscription) {
                boolean filterNonePeriod = Boolean.valueOf(Framework.getProperty("ottc.news.scan.dev.filter.none.period", "false"));
                if (filterNonePeriod) {
                    Date nextNewsDate = this.member.getNextNewsDate();
                    filters = nextNewsDate.getTime() < System.currentTimeMillis();
                }
            }

            return filters;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object initialize(int index, Object scannedObject) throws Exception {
        // Current Date initialized
        this.currentDate = new Date();

        // Next news date
        Date nextNewsDate = this.member.getNextNewsDate();

        if (nextNewsDate == null) {
            // Member not yet notified: initialize
            nextNewsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()));
            this.member.setNextNewsDate(index, nextNewsDate);

            // Set lastNewsDate too
            this.member.setLastNewsDate(index, this.currentDate);
        }

        return this.member;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object update(int index, Object scannedObject) throws Exception {
        // LastNewsDate = previous nextNewsDate
        Date storedNextNewsDate = ((SpaceMember) scannedObject).getNextNewsDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(storedNextNewsDate);
        this.member.setLastNewsDate(index, calendar.getTime());
        
        // Update nextNewsDate
        Date newsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()));
        this.member.setNextNewsDate(index, newsDate);

        return this.member;
    }

    /**
     * Gets next news Date.
     * 
     * @param nextBaseDate
     * @return Date
     * @throws Exception
     */
    public Date getNextNewsDate(Date nextBaseDate, String boundaryValue) throws Exception {
        int nextInterval = Integer.valueOf(boundaryValue).intValue();
        NewsPeriod newsPeriod = this.member.getNewsPeriod();

        return DateUpdaterTools.initializeNextDate(newsPeriod, nextBaseDate, nextInterval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object updateOnError(int index, Object scannedObject) throws Exception {
        // LastNewsDate = previous nextNewsDate
        Date storedNextNewsDate = ((SpaceMember) scannedObject).getNextNewsDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(storedNextNewsDate);
        this.member.setLastNewsDate(index, calendar.getTime());
        
        // NextNewsDate
        Date newsDate = getNextNewsDate(this.currentDate, getBoundaryValue(NewsPeriod.error));
        this.member.setNextNewsDate(index, newsDate);

        return this.member;
    }

}
