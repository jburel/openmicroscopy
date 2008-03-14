
/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 *	author Will Moore will@lifesci.dundee.ac.uk
 */

package calendar;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import omeroCal.CalendarEvent;
import omeroCal.CalendarObject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tree.DataFieldConstants;
import ui.components.FileChooserReturnFile;
import ui.fieldEditors.FieldEditorDate;
import ui.fieldEditors.FieldEditorTime;
import util.XMLMethods;

public class CalendarFile extends CalendarObject {
	
	

	/**
	 * A list of the date-time fields in the file
	 */
	ArrayList<CalendarEvent> scheduledDates;
	
	
	public CalendarFile(File xmlExperimentFile) {
		
		setInfo(xmlExperimentFile.getAbsolutePath());
		
		scheduledDates = new ArrayList<CalendarEvent>();
		
		buildScheduledDates(xmlExperimentFile);
		
	}
	
	public CalendarFile(String filePath, String fileName) {
		super(fileName, filePath);
		
		scheduledDates = new ArrayList<CalendarEvent>();
	}
	
	
	public void addEvent(String eventName, Calendar eventDate) {
		scheduledDates.add(new CalendarEvent(eventName, eventDate));
	}
	
	public void addEvent(CalendarEvent event) {
		scheduledDates.add(event);
	}
	
	public void buildScheduledDates(File xmlExperimentFile) {
		
		try {
			Document domDoc = XMLMethods.readXMLtoDOM(xmlExperimentFile);
			NodeList dateTimeNodes = domDoc.getElementsByTagName("*");
			
			Element titleElement = domDoc.getDocumentElement();
			setName(titleElement.getAttribute(DataFieldConstants.ELEMENT_NAME));
			
			
			boolean fileContainsDate = false;
			
			GregorianCalendar gc = new GregorianCalendar();
			String eventName;
			
			for (int i=0; i<dateTimeNodes.getLength(); i++) {
				
				System.out.println("NodeName = " + dateTimeNodes.item(i).getNodeName());
				
				// Check whether a field is a DateTimeField
				if (dateTimeNodes.item(i).getNodeName().equals(DataFieldConstants.DATE_TIME_FIELD)) {
					fileContainsDate = true;
					Element dateTimeElement = (Element)dateTimeNodes.item(i);
					String millisecs = dateTimeElement.getAttribute(DataFieldConstants.UTC_MILLISECS);
					System.out.println("CalendarFile millisecs = " + millisecs);
					// create a new calendar (don't want to change time of calendars added to list).
					gc = new GregorianCalendar();
					gc.setTimeInMillis(new Long(millisecs));
			
					eventName = dateTimeElement.getAttribute(DataFieldConstants.ELEMENT_NAME);
					//System.out.println("CalendarFile " + eventName + ": " + dateFormat.format(gc.getTime()));
					
					scheduledDates.add(new CalendarEvent(eventName, gc));
					
					// just to make sure that fileName gets assigned to something (ie if it didn't get name from root)
					if (getName() == null)
						setName(eventName);
				} else
				
					// Check for the deprecated DateField, for older files. 
				if (dateTimeNodes.item(i).getNodeName().equals(DataFieldConstants.DATE)) {
					
					Element dateTimeElement = (Element)dateTimeNodes.item(i);
					
					String formattedDate = dateTimeElement.getAttribute(DataFieldConstants.VALUE);
					System.out.println("formattedDate = " + formattedDate);
					Date date = FieldEditorDate.getDateFromString(formattedDate);
					
					// create a new calendar (don't want to change time of calendars added to list).
					gc = new GregorianCalendar();
					if (date != null)
						gc.setTime(date);
					else {
						System.out.println("DATE == null.  formattedDate = " + formattedDate);
						// if no date was set, ignore this DateField
						continue;
					}
			
					fileContainsDate = true;
					eventName = dateTimeElement.getAttribute(DataFieldConstants.ELEMENT_NAME);
					//System.out.println("CalendarFile " + eventName + ": " + dateFormat.format(gc.getTime()));
					
					scheduledDates.add(new CalendarEvent(eventName, gc));
					
					// just to make sure that fileName gets assigned to something (ie if it didn't get name from root)
					if (getName() == null)
						setName(eventName);
				} else 
				
				// if you know that dates exist, look for times that follow
				if (fileContainsDate && (dateTimeNodes.item(i).getNodeName().equals(DataFieldConstants.TIME_FIELD))) {
					Element timeElement = (Element)dateTimeNodes.item(i);
					// look for time-value in new "seconds" attribute.
					String timeValue = timeElement.getAttribute(DataFieldConstants.SECONDS);
					
					// If it is null, time may be stored under the older "value" attribute.
					if ((timeValue == null) || (timeValue.length() == 0))
						timeValue = timeElement.getAttribute(DataFieldConstants.VALUE);
					
					int timeInSecs = FieldEditorTime.getSecondsFromTimeValue(timeValue);
					
					// take the last date for gc (updated for the last TIME_DATE_FIELD or TIME_FIELD),
					// increment the timeInSecs, 
					// and add it as a new scheduledDate.
					Date previousDate = gc.getTime();
					gc = new GregorianCalendar();	// don't want to change existing date in list
					gc.setTime(previousDate);
					gc.add(Calendar.SECOND, timeInSecs);
					eventName = timeElement.getAttribute(DataFieldConstants.ELEMENT_NAME);
					scheduledDates.add(new CalendarEvent(eventName, gc));
				}
				
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printDatesList() {
		
		for (CalendarEvent event: scheduledDates) {
			//Date eventTime = event.getTime();
			
			// String time = timeFormat.format(eventTime);
			// String date = dateFormat.format(eventTime);
			String name = event.getName();
			
			// System.out.println(name + ": " + date + ", " + time);
		}
	}
	
	/**
	 * For testing.
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Create a file chooser, get a file from user
		FileChooserReturnFile fc = new FileChooserReturnFile(new String[] {"xml"}, null);
		File file = fc.getFileFromUser();
		
		new CalendarFile(file).printDatesList();
	}
	
	public List<CalendarEvent> getEvents() {
		return scheduledDates;
	}
}
