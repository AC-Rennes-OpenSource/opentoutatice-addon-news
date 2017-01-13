/**
 * 
 */
package org.opentoutatice.ecm.feature.news.test;

import java.util.Date;

import org.opentoutatice.ecm.feature.news.scanner.DateUpdaterTools;
import org.opentoutatice.ecm.feature.news.scanner.io.NewsPeriod;
import org.restlet.util.DateUtils;


/**
 * @author david
 *
 */
public class DateToolsTest {

  
  public static void main(String[] args){
      Date inputDate = new Date();
      System.out.println("[Input   ]: " + DateUtils.format(inputDate, DateUpdaterTools.DATE_TIME_FORMAT));
      
      // In minutes:
      Date nextDDate = DateUpdaterTools.initializeTestNextDate(NewsPeriod.daily, inputDate, 240);
      System.out.println("[Next+1d ]: " + DateUtils.format(nextDDate, DateUpdaterTools.DATE_TIME_FORMAT));
      
      Date nextWDate = DateUpdaterTools.initializeTestNextDate(NewsPeriod.weekly, inputDate, 720);
      System.out.println("[Next+1w ]: " + DateUtils.format(nextWDate, DateUpdaterTools.DATE_TIME_FORMAT));
      
      Date nextDate = DateUpdaterTools.initializeTestNextDate(NewsPeriod.none, inputDate, 720);
      System.out.println("[Next+non]: " + DateUtils.format(inputDate, DateUpdaterTools.DATE_TIME_FORMAT));
      
      
      
//      DateUpdaterTools.getNextDevRandomDate(inputDate);
//      System.out.println("[Next DEV]: " + DateUtils.format(nextDate, DateUpdaterTools.DATE_TIME_FORMAT));
      
  }
  
  

}
