/**
 * 
 */
package org.opentoutatice.ecm.feature.news.scanner;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.consistency.DateRepairer;
import org.opentoutatice.ecm.feature.news.model.SpaceMember;
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
                    log.trace("[NO MODE TEST] [Period]: " + boundaryValue);
                }

                if (isTestModeSet()) {
                    boundaryValue = Framework.getProperty("ottc.news.scan.daily.test.boundary");
                    if (log.isDebugEnabled()) {
                        log.trace("[MODE TEST] [Period]: " + boundaryValue);
                    }
                }

                break;

            case weekly:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_WEEKLY_BOUNDARY);

                if (log.isTraceEnabled()) {
                    log.trace("[NO MODE TEST] [Period]: " + boundaryValue);
                }

                if (isTestModeSet()) {
                    boundaryValue = Framework.getProperty("ottc.news.scan.weekly.test.boundary");
                    if (log.isTraceEnabled()) {
                        log.trace("[MODE TEST] [Period]: " + boundaryValue);
                    }
                }

                break;

            case error:
                boundaryValue = (String) getParams().get(DateUpdaterTools.NEXT_ERROR_BOUNDARY);

                if (log.isTraceEnabled()) {
                    log.trace("[NO MODE TEST] [Period]: " + boundaryValue);
                }

                if (isTestModeSet()) {
                    boundaryValue = Framework.getProperty("ottc.news.scan.error.test.boundary");
                    if (log.isTraceEnabled()) {
                        log.trace("[MODE TEST] [Period]: " + boundaryValue);
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
        if (this.member.isUsable()) {

            // Member must have subscribed
            boolean hasSubscribed = this.member.hasSubscribed();
            if (!hasSubscribed) {
                // Could have subscribed before: reset
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
                log.debug("[NO TEST MODE] [accepts]: " + accepts + ": " + "(hasSubscribed=" + hasSubscribed + " / noPeriod=" + noPeriod + " / mustNotify="
                        + mustNotify);
            }

            if (isTestModeSet()) {
                // Test mode: accept conditions
                hasSubscribed = hasSubscribed ? hasSubscribed : Boolean.valueOf(Framework.getProperty("ottc.news.scan.accept.subscr.test", "false"));
                if (noPeriod && Boolean.valueOf(Framework.getProperty("ottc.news.scan.accept.none.period.test", "false"))) {
                    noPeriod = false;
                }
                mustNotify = nextNewsDate == null || (nextNewsDate != null && nextNewsDate.getTime() < this.currentDate.getTime());

                accepts = hasSubscribed && !noPeriod && mustNotify;

                // Debug
                if (log.isDebugEnabled()) {
                    log.debug("[TEST MODE] [accepts]: " + accepts + ": " + "(hasSubscribed=" + hasSubscribed + " / noPeriod=" + noPeriod + " / mustNotify="
                            + mustNotify);
                }

            }

            if (accepts) {
                if (this.member != null) {
                    if (log.isInfoEnabled()) {
                        log.info("[Treating] " + this.member.getLogin() + " | " + this.member.getSpaceTitle());
                    }
                }
            }

        } else {
            accepts = false;
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
            nextNewsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()), true);
            this.member.setNextNewsDate(index, nextNewsDate);

            // Set lastNewsDate too
            this.member.setLastNewsDate(index, this.currentDate);
        } else {
            // Consistency
            NewsPeriod period = this.member.getNewsPeriod();
            int boundary = NumberUtils.getNumber(getBoundaryValue(period)).intValue();

            nextNewsDate = DateRepairer.checkDateNRepair(period, nextNewsDate, boundary);
            if (nextNewsDate != null) {
                this.member.setNextNewsDate(index, nextNewsDate);
            }
        }

        return this.member;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object update(int index, Object scannedObject) throws Exception {
        // LastNewsDate = current Date
        this.member.setLastNewsDate(index, this.currentDate);

        // Update nextNewsDate
        Date newsDate = getNextNewsDate(this.currentDate, getBoundaryValue(this.member.getNewsPeriod()), false);
        this.member.setNextNewsDate(index, newsDate);

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
        // NextNewsDate
        Date newsDate = getNextNewsDate(this.currentDate, getBoundaryValue(NewsPeriod.error), false);
        this.member.setNextNewsDate(index, newsDate);

        return this.member;
    }

}
