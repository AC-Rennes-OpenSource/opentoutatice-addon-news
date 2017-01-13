/**
 * 
 */
package org.opentoutatice.ecm.feature.news.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author david
 *
 */
public class SpaceMember {

    /** User profile. */
    private DocumentModel userProfile;

    /** Data. */
    private Map<String, Serializable> data;

    /** Space. */
    private DocumentModel space;

    /** Session. */
    private CoreSession session;

    /** UserManager. */
    private static UserManager usrManager;


    public SpaceMember(Map<String, Serializable> data) throws LoginException {
        super();
        initialize(data);
    }


    /**
     * @return the session
     */
    public CoreSession getSession() {
        return session;
    }

    /**
     * Getter for user manager.
     * 
     * @return UserManager
     */
    protected static UserManager getUsermanager() {
        if (usrManager == null) {
            usrManager = (UserManager) Framework.getService(UserManager.class);
        }
        return usrManager;
    }

    /**
     * Initialize model.
     * 
     * @param login
     * @throws LoginException
     */
    private void initialize(Map<String, Serializable> data) throws LoginException {
        // Data
        this.data = data;

        // Login
        String login = (String) data.get(SpaceMemberConstants.LOGIN_DATA);

        // Initialize session
        Framework.loginAsUser(login);
        this.session = CoreInstance.openCoreSession(null, login);

        // User profile
        UserProfileService usrProfileSrv = (UserProfileService) Framework.getService(UserProfileService.class);
        this.userProfile = usrProfileSrv.getUserProfileDocument(login, session);

        // Space
        String spaceId = (String) this.data.get(SpaceMemberConstants.SPACE_ID);
        this.space = ToutaticeDocumentHelper.getUnrestrictedDocument(this.session, spaceId);
    }

    /**
     * Checks if space's member wants to have notifications.
     * 
     * @return
     */
    public boolean hasSubscribed() {
        return (Boolean) this.userProfile.getPropertyValue(SpaceMemberConstants.NEWS_SUBSCRIPTION);
    }

    /**
     * Space id.
     * 
     * @return String
     */
    public String getSpaceId() {
        return (String) this.data.get(SpaceMemberConstants.SPACE_ID);
    }

    /**
     * Gets space's title.
     * 
     * @return title
     */
    public String getSpaceTitle() {
        return (String) this.data.get(SpaceMemberConstants.SPACE_TITLE);
    }

    /**
     * Gets member's email.
     * 
     * @return String
     */
    public String getEmail() throws Exception {
        String email = null;

        // Get Principal
        String userName = (String) this.data.get(SpaceMemberConstants.LOGIN_DATA);
        NuxeoPrincipal principal = getUsermanager().getPrincipal(userName);

        if (principal != null) {
            // Get email
            email = principal.getEmail();
        } else {
            throw new Exception(userName + " doesn't exist.");
        }

        return email;
    }

    /**
     * Getter for news periodicity.
     * 
     * @return NewsPeriod
     */
    public NewsPeriod getNewsPeriod() {
        NewsPeriod newsPeriod = null;

        String newsPeriodProp = (String) this.data.get(SpaceMemberConstants.NEWS_PERIOD_DATA);

        if (newsPeriodProp == null) {
            // For coherence
            newsPeriod = NewsPeriod.none;
//            if (Framework.isDevModeSet()) {
//                // None period filter
//                Boolean filterNonePeriod = Boolean.valueOf(Framework.getProperty("ottc.news.scan.dev.filter.none.period"));
//                if(!filterNonePeriod){
//                    newsPeriod = NewsPeriod.none;
//                }
//            }
        } else {
            newsPeriod = NewsPeriod.valueOf(newsPeriodProp);
        }

        return newsPeriod;
    }

    /**
     * Getter for nextNewsDate.
     * 
     * @return Date
     */
    public Date getNextNewsDate() {
        Date nextDate = null;

        Calendar calendar = (GregorianCalendar) this.data.get(SpaceMemberConstants.NEXT_NEWS_DATE_DATA);
        if (calendar != null) {
            nextDate = calendar.getTime();
        }
        
//        if(Framework.isDevModeSet()){
//            nextDate = new Date();
//        }

        return nextDate;
    }

    /**
     * Model and persistent store of next news date.
     * 
     * @param nextNewsDate
     */
    public void setNextNewsDate(int index, Date nextNewsDate) {
        // Model update
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(nextNewsDate);
        this.data.put(SpaceMemberConstants.NEXT_NEWS_DATE_DATA, calendar);

        // Update
        this.space.setPropertyValue("ttcs:spaceMembers/[" + index + "]/nextNewsDate", nextNewsDate);
        // Save
        ToutaticeDocumentHelper.saveDocumentSilently(this.session, this.space, true);
    }

    /**
     * Getter for lastNewsDate.
     * 
     * @return Date
     */
    public Date getLastNewsDate() {
        Date lastDate = null;

        Calendar calendar = (GregorianCalendar) this.data.get(SpaceMemberConstants.LAST_NEWS_DATE_DATA);
        if (calendar != null) {
            lastDate = calendar.getTime();
        }

        return lastDate;
    }

    /**
     * Model and persistent store of next news date.
     * 
     * @param lastNewsDate
     */
    public void setLastNewsDate(int index, Date lastNewsDate) {
        // Update model
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(lastNewsDate);
        this.data.put(SpaceMemberConstants.LAST_NEWS_DATE_DATA, calendar);

        // Update
        this.space.setPropertyValue("ttcs:spaceMembers/[" + index + "]/lastNewsDate", lastNewsDate);
        // Save
        ToutaticeDocumentHelper.saveDocumentSilently(this.session, this.space, true);
    }

    /**
     * Converts model (~ query result entry) to DocumentModel property.
     * 
     * @param member
     * @return
     */
    private Map<String, Serializable> toProp(SpaceMember member) {
        Map<String, Serializable> prop = new HashMap<>(1);

        // Fill
        prop.put(SpaceConstants.LOGIN, this.data.get(SpaceMemberConstants.LOGIN_DATA));
        prop.put(SpaceConstants.JOINED_DATE, this.data.get(SpaceMemberConstants.JOINED_DATE_DATA));
        prop.put(SpaceConstants.NEWS_PERIOD, this.data.get(SpaceMemberConstants.NEWS_PERIOD_DATA));
        prop.put(SpaceConstants.LAST_NEWS_DATE, this.data.get(SpaceMemberConstants.LAST_NEWS_DATE_DATA));
        prop.put(SpaceConstants.NEXT_NEWS_DATE, this.data.get(SpaceMemberConstants.NEXT_NEWS_DATE_DATA));

        return prop;
    }

}
