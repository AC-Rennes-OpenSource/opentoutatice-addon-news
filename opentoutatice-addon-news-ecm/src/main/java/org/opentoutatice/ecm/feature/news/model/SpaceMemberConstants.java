/**
 * 
 */
package org.opentoutatice.ecm.feature.news.model;


/**
 * @author david
 *
 */
public interface SpaceMemberConstants {
    
    /** Subscription. */
    String NEWS_SUBSCRIPTION = "ttc_userprofile:newsSubscription";
    
    /** Space id. */
    String SPACE_ID = "ecm:uuid";
    /** Space title. */
    String SPACE_TITLE = "dc:title";
    
    /** Login. */
    String LOGIN_DATA = "ttcs:spaceMembers/*1/login";
    /** Joined date. */
    String JOINED_DATE_DATA = "ttcs:spaceMembers/*1/joinedDate";
    /** News period. */
    String NEWS_PERIOD_DATA = "ttcs:spaceMembers/*1/newsPeriod";
    /** Last news date. */
    String LAST_NEWS_DATE_DATA = "ttcs:spaceMembers/*1/lastNewsDate";
    /** Next news date. */
    String NEXT_NEWS_DATE_DATA = "ttcs:spaceMembers/*1/nextNewsDate";
    
}
