/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.security.auth.login.LoginContext;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.consistency.DateRepairer;
import org.opentoutatice.ecm.feature.news.model.SpaceMember;
import org.opentoutatice.ecm.feature.news.model.SpaceMemberConstants;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;
import org.opentoutatice.ecm.scanner.AbstractScanUpdater;
import org.richfaces.component.NumberUtils;

import fr.toutatice.ecm.platform.core.helper.ToutaticeQueryHelper;


/**
 * @author david
 *
 */
public class NewsUpdater extends AbstractScanUpdater {

    /** Logger. */
    private static final Log log = LogFactory.getLog(NewsUpdater.class);

    /** UserWorksapces Root query. */
    private static final String UWS_ROOT_QUERY = "select * from UserWorkspacesRoot where ecm:isProxy = 0 and ecm:isVersion = 0 and ecm:currentLifeCycleState <> 'deleted'";

    /** Space member model. */
    private SpaceMember member;

    /** Current date. */
    private Date currentDate;

    /** UserWorkspaces root. */
    private static DocumentModel userWorkspacesRoot = null;

    /**
     * @return the currentDate
     */
    public Date getCurrentDate() {
        return currentDate;
    }

    /**
     * @return the member
     */
    public SpaceMember getMember() {
        return member;
    }

    /**
     * @param member the member to set
     */
    public void setMember(SpaceMember member) {
        this.member = member;
    }


    /**
     * @param currentDate the currentDate to set
     */
    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    /**
     * Gets root of user's workspaces.
     * 
     * @return
     * @throws Exception
     */
    public static DocumentModel getUserWorkspacesRoot() throws Exception {
        if (userWorkspacesRoot == null) {
            LoginContext login = null;
            CoreSession sessionSystem = null;
            try {
                // Find it
                login = Framework.login();
                sessionSystem = CoreInstance.openCoreSessionSystem(null);

                DocumentModelList uWorspacesRoots = ToutaticeQueryHelper.queryUnrestricted(sessionSystem, UWS_ROOT_QUERY);
                // FIXME: takes the first for the moment if more than one
                userWorkspacesRoot = uWorspacesRoots.get(0);
            } finally {
                if (sessionSystem != null) {
                    sessionSystem.close();
                }
                if (login != null) {
                    login.logout();
                }
            }
        }
        return userWorkspacesRoot;
    }


    /**
     * Getter for test mode.
     * 
     * @return boolean
     */
    public static boolean isTestModeSet() {
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

                if (log.isTraceEnabled()) {
                    log.trace("[Period]: " + boundaryValue);
                }

                break;

            case weekly:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_WEEKLY_BOUNDARY);

                if (log.isTraceEnabled()) {
                    log.trace("[Period]: " + boundaryValue);
                }

                break;

            case error:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_ERROR_BOUNDARY);

                if (log.isTraceEnabled()) {
                    log.trace("[Period]: " + boundaryValue);
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
    	
    	// LBI #1847 - Test period first
    	boolean periodAvaliable = testNewsPeriodAvaliable(scannedObject);
    	
        // Accepts
        boolean accepts = false;
        
        if(periodAvaliable) {

	        // Current Date initialized
	        this.currentDate = new Date();
	
	        // Member
	        this.member = (SpaceMember) this.toModel(scannedObject);
	        if (this.member.hasUserProfile()) {
	            // Next news Date
	            Date nextNewsDate = this.member.getNextNewsDate();
	
	            // Member must have subscribed
	            boolean hasSubscribed = this.member.hasSubscribed();
	            if (!hasSubscribed && nextNewsDate != null) {
	                // Could have subscribed before: reset to be consistent
	                this.member.setNextNewsDate(index, null);
	            }
	
	            if (hasSubscribed) {
	                // Period subscription
	                //boolean noPeriod = NewsPeriod.none.equals(this.member.getNewsPeriod());
	
	                // Date condition:
	                // nextNewsDate is null if just subscribed
	                boolean mustNotify = nextNewsDate == null;
	
	                // Yet subscribed
	                if (nextNewsDate != null) {
	                    mustNotify = nextNewsDate.getTime() < this.currentDate.getTime();
	
	                    if (isTestModeSet()) {
	                        mustNotify = true;
	                    }
	                }
	
	                accepts = mustNotify;
	
	                // Debug
	                if (log.isDebugEnabled()) {
	                    log.debug("[accepts]: " + accepts + ": " + "(hasSubscribed=" + hasSubscribed + " / mustNotify=" + mustNotify);
	                }
	            }
	
	            if (accepts) {
	                if (this.member != null) {
	                    if (log.isInfoEnabled()) {
	                        log.info("[Treating] " + this.member.getLogin() + " | " + this.member.getSpaceTitle());
	                    }
	                }
	            }
	        }
        }
        else {
        	// Debug
            if (log.isDebugEnabled()) {
            	Map<String, Serializable> map =  (Map<String, Serializable>) scannedObject;
            	String login = (String) map.get(SpaceMemberConstants.LOGIN_DATA);
            	String spaceTitle = (String) map.get(SpaceMemberConstants.SPACE_TITLE);
            	log.debug("[Skipping] " + login + " | " + spaceTitle);
            }
        }

        return accepts;

    }

    private boolean testNewsPeriodAvaliable(Object scannedObject) {
    	Map<String, Serializable> map =  (Map<String, Serializable>) scannedObject;
    	String period = (String) map.get(SpaceMemberConstants.NEWS_PERIOD_DATA);
    	
    	if(period == null || NewsPeriod.none.equals(NewsPeriod.valueOf(period)))
    		return false;
    	else return true;
    	
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public Object initialize(int index, Object scannedObject) throws Exception {
        // Next news date
        Date nextNewsDate = this.member.getNextNewsDate();
        // Possible nextNewsDate repaired
        Date repairedNextNewsDate = null;

        // For debug logs
        Date formerNextNewsDate = nextNewsDate;

        // Member not yet notified: initialize
        if (nextNewsDate == null) {
            nextNewsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()), true);
            this.member.setNextNewsDate(index, nextNewsDate);

            // Set lastNewsDate too
            this.member.setLastNewsDate(index, this.currentDate);
        } else {
            // Consistency
            NewsPeriod period = this.member.getNewsPeriod();
            int boundary = NumberUtils.getNumber(getBoundaryValue(period)).intValue();

            repairedNextNewsDate = DateRepairer.checkDateNRepair(period, nextNewsDate, boundary);
            if (nextNewsDate != null) {
                this.member.setNextNewsDate(index, repairedNextNewsDate);
            }
        }

        if (log.isDebugEnabled()) {
            String period = this.member.getNewsPeriod().name();
            String repairedStatus = repairedNextNewsDate != null ? "(repaired)" : "(not repaired)";

            String inputDate = formerNextNewsDate != null ? DateFormatUtils.format(formerNextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT) : "Undefined";
            String outputDate = repairedNextNewsDate != null ? DateFormatUtils.format(repairedNextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT)
                    : DateFormatUtils.format(nextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT);

            log.debug("INIT [" + period + "] NextDate " + repairedStatus + " = " + inputDate + " -> " + outputDate);
        }

        return this.member;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object update(int index, Object scannedObject) throws Exception {
        // For debug logs
        Date formerNextNewsDate = this.member.getNextNewsDate();

        // LastNewsDate = current Date
        this.member.setLastNewsDate(index, this.currentDate);

        // Update nextNewsDate
        Date nextNewsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()), false);
        this.member.setNextNewsDate(index, nextNewsDate);

        if (log.isDebugEnabled()) {
            String period = this.member.getNewsPeriod().name();

            String inputDate = formerNextNewsDate != null ? DateFormatUtils.format(formerNextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT) : "Undefined";
            String outputDate = DateFormatUtils.format(nextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT);

            log.debug("UPDATE [" + period + "] NextDate = " + inputDate + " -> " + outputDate);
        }

        return this.member;
    }

    /**
     * Gets next news Date.
     * 
     * @param previousBaseDate
     * @return Date
     * @throws Exception
     */
    public Date getNextNewsDate(Date previousBaseDate, String boundaryValue, boolean init) throws Exception {
        int interval = Integer.valueOf(boundaryValue).intValue();
        NewsPeriod newsPeriod = this.member.getNewsPeriod();

        return DateUpdaterTools.computeNextDate(newsPeriod, previousBaseDate, interval, init);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object updateOnError(int index, Object scannedObject) throws Exception {
        // For debug logs
        Date formernextNewsDate = this.member.getNextNewsDate();

        // NextNewsDate
        Date nextNewsDate = getNextNewsDate(this.currentDate, getBoundaryValue(NewsPeriod.error), false);
        this.member.setNextNewsDate(index, nextNewsDate);

        if (log.isDebugEnabled()) {
            String period = this.member.getNewsPeriod().name();

            log.debug("UPDATE [" + period + "] NextDate = " + DateFormatUtils.format(formernextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT) + " -> "
                    + DateFormatUtils.format(nextNewsDate, DateUpdaterTools.DATE_TIME_FORMAT));
        }

        return this.member;
    }

}
