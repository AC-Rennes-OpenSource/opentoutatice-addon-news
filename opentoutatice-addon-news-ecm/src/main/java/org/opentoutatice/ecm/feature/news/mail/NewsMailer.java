/**
 * 
 */
package org.opentoutatice.ecm.feature.news.mail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.DateUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.runtime.api.Framework;
import org.opentoutatice.ecm.feature.news.model.SpaceMember;
import org.opentoutatice.ecm.feature.news.scanner.DateUpdaterTools;
import org.opentoutatice.ecm.reporter.AbstractMailer;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author david
 *
 */
public class NewsMailer extends AbstractMailer {

    /** Logger. */
    private static final Log log = LogFactory.getLog(NewsMailer.class);

    /** Modified documents query. */
    private static final String MODIFIED_DOCS_QUERY = "select * from Note, File, Picture, ContextualLink, ToutaticePad where ecm:ancestorId = '%s' "
            + " and dc:modified > TIMESTAMP '%s'" + " and ecm:isVersion = 0 and ecm:currentLifeCycleState <> 'deleted'" + " order by dc:modified desc";

    /** New members query. */
    // FIXME: can do a count() in select
    private static final String NEW_MEMBERS_QUERY = "select distinct ttcs:spaceMembers/*1/login from Workspace "
            + " where ttcs:spaceMembers/*1/joinedDate > TIMESTAMP '%s'" + " and ecm:isVersion = 0 and ecm:currentLifeCycleState <> 'deleted'";

    /** News documents query. */
    private static final String NEWS_DOCS_QUERY = "select * from Annonce, VEVENT where ecm:ancestorId = '%s' " + " and dc:modified > TIMESTAMP '%s'"
            + " and ecm:isVersion = 0 and ecm:currentLifeCycleState <> 'deleted'" + " order by dc:modified desc";

    /** Max displayed news and activities. */
    private final static int MAX_DISPLAYED = 10;
    
    /** Portal url. */
    private final static String PORTAL_URL = Framework.getProperty("ottc.news.portal.url");
    
    /** Event date format. */
    private static final String EVENT_DATE_FORMAT = "dd/MM/yyyy";
    /** Event date format. */
    private static final String EVENT_TIME_FORMAT = "HH:mm ";

    /** Mail's header. */
    private Map<String, Object> header;

    /** News. */
    private Map<String, Object> news;

    /** Activities. */
    private Map<String, Object> activities;

    /**
     * Default constructor.
     */
    public NewsMailer() {
        super();
    }

    // @Override
    // public <C, S> C write(S scannedObj) {
    // Map<String, Object> mailData = new HashMap<String, Object>(1);
    // mailData.put("mail.to", "dchevrier@osivia.com");
    // mailData.put(NotificationConstants.SUBJECT_KEY, "Ta première Notif!!!");
    // mailData.put(NotificationConstants.TEMPLATE_KEY, "ottcNews");
    //
    // ScannedObject scannedObject = (ScannedObject) scannedObj;
    // if(scannedObject != null){
    // Map<String, Object> docs = new HashMap<String, Object>();
    //
    // int index = 0;
    // for(Entry<String,Map<String,Serializable>> entry : scannedObject.entrySet()){
    // docs.put("doc" + String.valueOf(index), entry.getValue());
    // index++;
    // }
    //
    // mailData.put("docs", docs);
    // }
    //
    //
    // return (C) mailData;
    // }

    /**
     * Input data is of SpaceMember type.
     * Mail data is of Map<String, Object> type.
     */
    @Override
    public Object adapt(Object inputData) throws Exception {
        return inputData;
    }

    /**
     * Input data is of SpaceMember type.
     * Mail data is of Map<String, Object> type.
     */
    @Override
    public Object build(Object data) throws Exception {
        // Input
        SpaceMember member = (SpaceMember) data;
        // Output
        setData(new HashMap<String, Object>(1));

        try {
            CoreSession session = member.getSession();
            String spaceId = member.getSpaceId();
            Date lastNewsDate = member.getLastNewsDate();

            // Mofified documents
            DocumentModelList modifiedDocs = getModifiedDocs(member.getSession(), spaceId, lastNewsDate);
            setActivities(modifiedDocs);
            
            if (log.isDebugEnabled() && modifiedDocs != null) {
                DocumentModel space = ToutaticeDocumentHelper.getUnrestrictedDocument(session, member.getSpaceId());
                log.debug("[" + space.getTitle() + " : " +session.getPrincipal().getName() + "] "
                        + "[Last Notif: " + DateFormatUtils.format(lastNewsDate, DateUpdaterTools.DATE_TIME_FORMAT) + "]");
                log.debug("[" + modifiedDocs.size() + "] MODIFIED DOCS");
                for (DocumentModel doc : modifiedDocs) {
                    log.debug(doc.getTitle() + " ; ");
                }
            }

            // News
            IterableQueryResult newMembers = null;
            try {
                // Members
                newMembers = getNewMembers(session, spaceId, lastNewsDate);
                // News
                DocumentModelList newsDocs = getNewsDocs(session, spaceId, lastNewsDate);
                setNews(newMembers.size(), newsDocs);
                
                if (log.isDebugEnabled() && newsDocs != null) {
                    DocumentModel space = ToutaticeDocumentHelper.getUnrestrictedDocument(session, member.getSpaceId());
                    log.debug("[" + space.getTitle() + " : " +session.getPrincipal().getName() + "] "
                            + "[Last Notif: " + DateFormatUtils.format(lastNewsDate, DateUpdaterTools.DATE_TIME_FORMAT) + "]");
                    log.debug("[" + newsDocs.size() + "] NEWS");
                    for (DocumentModel doc : newsDocs) {
                        log.debug(doc.getTitle() + " ; ");
                    }
                }

                if (log.isDebugEnabled() && newMembers != null) {
                    log.debug("[" + newMembers.size() + "]" + " new members");
                }
            } finally {
                if (newMembers != null) {
                    newMembers.close();
                }
            }

            // Mail
            setMailHeader(member);

        } finally {
            // Session
            CoreSession session = member.getSession();
            if (session != null) {
                // To clear caches ??
                session.save();
                session.close();
            }

        }
        return getData();
    }

    public void setMailHeader(SpaceMember member) throws Exception {
        this.header = new HashMap<>();

        // "Header"
        this.header.put("mail.to", member.getEmail());

        this.header.put(NotificationConstants.SUBJECT_TEMPLATE_KEY, "ottcNewsSubject");
        this.header.put(NotificationConstants.TEMPLATE_KEY, "ottcNews");

        this.header.put("spaceTitle", member.getSpaceTitle());
        this.header.put("sendDate", DateUtils.format(new Date(), AbstractMailer.DATE_FORMAT));
        this.header.put("lastSendDate", DateUtils.format(member.getLastNewsDate(), AbstractMailer.DATE_FORMAT));

        // Global data
        getData().putAll(this.header);
    }

    public void setNews(long newMembersCount, DocumentModelList docs) {
        this.news = new HashMap<>();
        // Display news in mail indicator
        boolean display = false;

        // New members
        this.news.put("newMembersCount", newMembersCount);
        if(newMembersCount > 0){
            display = true;
        }

        // Number of documents to display
        int maxLoops = docs.size() > 0 && docs.size() > MAX_DISPLAYED ? MAX_DISPLAYED : docs.size();
        if(maxLoops > 0){
            display = true;
        }

        // Number of documents not displayed
        int otherDocsCount = 0 < maxLoops && docs.size() < maxLoops ? 0 : docs.size() - maxLoops;
        this.news.put("otherDocsCount", otherDocsCount);

        // Documents properties
        List<Map<String, Object>> newsList = new ArrayList<>(maxLoops);
        for (int index = 0; index < maxLoops; index++) {
            // Document
            Map<String, Object> news = new HashMap<>();
            DocumentModel doc = docs.get(index);
            news.put("doc", doc);
            // We put title cause getTitle is not a bean getter (cf DocumentModelImpl)
            news.put("title", doc.getTitle());
            
            // Portal link
            news.put("link", getPortalLink(doc));
            // Date modification
            GregorianCalendar calendar = (GregorianCalendar) doc.getPropertyValue("dc:modified");
            news.put("modified", DateUtils.format(calendar.getTime(), AbstractMailer.DATE_FORMAT));
            // Last contributor
            news.put("lastContributor", doc.getPropertyValue("dc:lastContributor"));
            
            // Event case
            if("VEVENT".equals(doc.getType())){
                Calendar evtCal = (GregorianCalendar) doc.getPropertyValue("vevent:dtstart");
                news.put("evtBegin", "le " + DateFormatUtils.format(evtCal.getTime(), EVENT_DATE_FORMAT)
                        + " à " + DateFormatUtils.format(evtCal.getTime(), EVENT_TIME_FORMAT));
            }

            // Add
            newsList.add(news);
        }

        this.news.put("docs", newsList);
        
        // Indicator
        this.news.put("display", display);

        // Global data
        getData().put("news", this.news);
    }

    public void setActivities(DocumentModelList docs) {
        this.activities = new HashMap<>();

        // Number of documents to display
        int maxLoops = docs.size() > 0 && docs.size() > MAX_DISPLAYED ? MAX_DISPLAYED : docs.size();
        // Display activities in mail indicator
        boolean display = maxLoops > 0;

        // Number of documents not displayed
        int otherDocsCount = 0 < maxLoops && docs.size() < maxLoops ? 0 : docs.size() - maxLoops;
        this.activities.put("otherDocsCount", otherDocsCount);

        // Documents properties
        List<Map<String, Object>> activities = new ArrayList<>(maxLoops);
        for (int index = 0; index < maxLoops; index++) {
            // Document
            Map<String, Object> activity = new HashMap<>();
            DocumentModel doc = docs.get(index);
            activity.put("doc", doc);
            // We put title cause getTitle is not a bean getter (cf DocumentModelImpl)
            activity.put("title", doc.getTitle());
            
            // Portal link
            activity.put("link", getPortalLink(doc));
            // Date modification
            Calendar calendar = (GregorianCalendar) doc.getPropertyValue("dc:modified");
            activity.put("modified", DateUtils.format(calendar.getTime(), AbstractMailer.DATE_FORMAT));
            // Last contributor
            activity.put("lastContributor", doc.getPropertyValue("dc:lastContributor"));
            
            // Add
            activities.add(activity);
        }

        this.activities.put("docs", activities);
        
        // Indicator
        this.activities.put("display", display);

        // Global data
        getData().put("activities", this.activities);
    }

    /**
     * Gets modified documents by member.
     * 
     * @param session
     * @param wsId
     * @param lastNewsDate
     * @return DocumentModelList
     */
    protected DocumentModelList getModifiedDocs(CoreSession session, String wsId, Date lastNewsDate) {
        String formatedDate = DateFormatUtils.format(lastNewsDate, DateUpdaterTools.DATE_TIME_QUERY_FORMAT);
        String query = String.format(MODIFIED_DOCS_QUERY, wsId, formatedDate);
        if (log.isDebugEnabled()) {
            log.debug("Modifieds docs query: " + query);
        }
        return session.query(query);
    }

    /**
     * Gets new members of member's space.
     * 
     * @param session
     * @param wsId
     * @param lastNewsDate
     * @return IterableQueryResult
     */
    protected IterableQueryResult getNewMembers(CoreSession session, String wsId, Date lastNewsDate) {
        String formatedDate = DateFormatUtils.format(lastNewsDate, DateUpdaterTools.DATE_TIME_QUERY_FORMAT);
        String query = String.format(NEW_MEMBERS_QUERY, formatedDate);
        return session.queryAndFetch(query, NXQL.NXQL, new Object[0]);
    }

    protected DocumentModelList getNewsDocs(CoreSession session, String wsId, Date lastNewsDate) {
        String formatedDate = DateFormatUtils.format(lastNewsDate, DateUpdaterTools.DATE_TIME_QUERY_FORMAT);
        String query = String.format(NEWS_DOCS_QUERY, wsId, formatedDate);
        if (log.isDebugEnabled()) {
            log.debug("News docs query: " + query);
        }
        return session.query(query);
    }
    
    /**
     * Gets portal document's link with webId. 
     * 
     * @param doc
     * @return
     */
    protected String getPortalLink(DocumentModel doc){
        String id = (String) doc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
        return PORTAL_URL + id;
    }


}
