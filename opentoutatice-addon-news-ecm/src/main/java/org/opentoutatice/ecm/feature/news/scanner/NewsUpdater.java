/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.model.SpaceMember;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;
import org.opentoutatice.ecm.scanner.AbstractScanUpdater;


/**
 * @author david
 *
 */
public class NewsUpdater extends AbstractScanUpdater {
    
    /** Logger. */
    private static final Log log = LogFactory.getLog(NewsUpdater.class);

    /** Space member model. */
    private SpaceMember member;

    /** Current date. */
    private Date currentDate;
    
    /**
     * Getter for test mode.
     * 
     * @return boolean
     */
    public static boolean isTestModeSet(){
        return Boolean.valueOf(Framework.getProperty("ottc.news.mode.test"));
    }
    
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
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Period]: " + boundaryValue);
                }
                
                if(isTestModeSet()){
                    boundaryValue = Framework.getProperty("ottc.news.scan.daily.test.boundary");
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [Period]: " + boundaryValue);
                    }
                }
                
                break;

            case weekly:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_WEEKLY_BOUNDARY);
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Period]: " + boundaryValue);
                }
                
                if(isTestModeSet()){
                    boundaryValue = Framework.getProperty("ottc.news.scan.weekly.test.boundary");
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [Period]: " + boundaryValue);
                    }
                }
                
                break;

            case error:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_ERROR_BOUNDARY);
                
                if(log.isDebugEnabled()){
                    log.debug("[NO MODE TEST] [Period]: " + boundaryValue);
                }
                
                if(isTestModeSet()){
                    boundaryValue = Framework.getProperty("ottc.news.scan.error.test.boundary");
                    if(log.isDebugEnabled()){
                        log.debug("[MODE TEST] [Period]: " + boundaryValue);
                    }
                }
                
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
    public boolean accept(int index, Object scannedObject) throws Exception {
        // Accepts
        boolean accepts = true;

        // Current Date initialized
        this.currentDate = new Date();

        // Member
        this.member = (SpaceMember) this.toModel(scannedObject);

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
        // nextNewsDat is null if just subscribed
        boolean mustNotify = nextNewsDate == null || (nextNewsDate != null && nextNewsDate.getTime() < this.currentDate.getTime());

        accepts = hasSubscribed && !noPeriod && mustNotify;

        // Debug
        if (log.isDebugEnabled()) {
            log.debug("[NO MODE SET] [accepts]: " + accepts + ": " + "(hasSubscribed=" + hasSubscribed + " / noPeriod=" + noPeriod + " / mustNotify="
                    + mustNotify);
        }

        if (isTestModeSet()) {
            // Test mode: accept conditions
            hasSubscribed = Boolean.valueOf(Framework.getProperty("ottc.news.scan.accept.subscr.test", "false"));
            if (noPeriod && Boolean.valueOf(Framework.getProperty("ottc.news.scan.accept.none.period.test", "false"))) {
                noPeriod = false;
            }
            mustNotify = nextNewsDate == null || (nextNewsDate != null && nextNewsDate.getTime() < this.currentDate.getTime());

            accepts = hasSubscribed && !noPeriod && mustNotify;
            
            // Debug
            if (log.isDebugEnabled()) {
                log.debug("[MODE SET] [accepts]: " + accepts + ": " + "(hasSubscribed=" + hasSubscribed + " / noPeriod=" + noPeriod + " / mustNotify="
                        + mustNotify);
            }

        }

        return accepts;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object initialize(int index, Object scannedObject) throws Exception {
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
