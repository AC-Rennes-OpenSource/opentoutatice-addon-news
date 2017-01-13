/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.io.Serializable;
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
    protected String getParamBoundary(NewsPeriod newsPeriod) throws Exception {
        // Boundary
        String boundary = null;

        switch (newsPeriod) {
            case daily:
                boundary = (String) getParams().get(DateUpdaterTools.NEXT_DAILY_BOUNDARY);
                break;

            case weekly:
                boundary = (String) getParams().get(DateUpdaterTools.NEXT_WEEKLY_BOUNDARY);
                break;

            case error:
                boundary = (String) getParams().get(DateUpdaterTools.NEXT_ERROR_BOUNDARY);
                break;

            case none:
                boundary = NewsPeriod.none.name();
                break;
                
            case dev:
                boundary = "dev";
                break;
        }

        return boundary;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean filter(Object scannedObject) throws Exception {
        // Member
        this.member = (SpaceMember) this.toModel(scannedObject);

//        // Member must have subscribed
//        boolean hasSubscribed = this.member.hasSubscribed();
//        
//        // Period subscription
//        boolean noPeriod = NewsPeriod.none.equals(this.member.getNewsPeriod());
//        
//        // Date condition
//        Date nextNewsDate = this.member.getNextNewsDate();
//        boolean mustNotify = nextNewsDate.getTime() > System.currentTimeMillis();
//
//        return hasSubscribed && !noPeriod && mustNotify;
        
        return false;
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
            nextNewsDate = getNextNewsDate(new Date(), getParamBoundary(this.member.getNewsPeriod()));
            this.member.setNextNewsDate(index, nextNewsDate);
            
            // Set lastNewsDate to
            this.member.setLastNewsDate(index, new Date());
        }
        
        // FIXME: Due to bug?? -> yes: updateOnError
        Date lastNewsDate = this.member.getLastNewsDate();
        if(lastNewsDate == null){
            this.member.setLastNewsDate(index, new Date());
        }

        return this.member;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object update(int index, Object scannedObject) throws Exception {
        // Next news date
        Date nextNewsDate = this.member.getNextNewsDate();

        Date newsDate = getNextNewsDate(nextNewsDate, getParamBoundary(this.member.getNewsPeriod()));
        this.member.setNextNewsDate(index, newsDate);
        
        // Update lastNewsDate
        this.member.setLastNewsDate(index, new Date());
        
        return this.member;
    }

    /**
     * Gets next news Date.
     * 
     * @param nextBaseDate
     * @return Date
     * @throws Exception
     */
    public Date getNextNewsDate(Date nextBaseDate, String boundary) throws Exception {
        // Parameter
        String boundaryValue = (String) getParams().get(boundary);
        
        // For dev
        if(Framework.isDevModeSet()){
            boundaryValue = "2";
        }
        
        int nextInterval = Integer.valueOf(boundaryValue).intValue();
        NewsPeriod newsPeriod = this.member.getNewsPeriod();
        
        return DateUpdaterTools.initializeNextDate(newsPeriod, nextBaseDate, nextInterval);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object updateOnError(int index, Object scannedObject) throws Exception {
        // Next news date
        Date nextNewsDate = this.member.getNextNewsDate();

        Date newsDate = getNextNewsDate(nextNewsDate, getParamBoundary(NewsPeriod.error));
        this.member.setNextNewsDate(index, newsDate);
        
        // Set lastNewsDate to
        this.member.setLastNewsDate(index, newsDate);

        return this.member;
    }

}
