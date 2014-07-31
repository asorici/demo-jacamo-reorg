package roombooking.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import roombooking.core.event.Event;
import roombooking.core.event.Event.EventType;
import roombooking.util.EventComparator;


public class EventList {
	private List<Event> events;
	private EventComparator eventComparator;
	
	public EventList() {
		events = new ArrayList<Event>();
		eventComparator = new EventComparator();
	}
	
	public void addEvent(Event event) {
		// Search for the non-existent item
		int index = Collections.binarySearch(events, event, eventComparator);
		
		// if the event is not already in the list index will be < 0
		if (index < 0) {	
			events.add(-index - 1, event);
		}
	}
	
	public void removeEvent(Event event) {
		events.remove(event);
	}
	
	public List<Event> getAllEvents() {
		return events;
	}
	
	public int getSize() {
		return events.size();
	}
	
	public Event matchEvent(String eventTitle, EventType eventType, Date startDate) {
		for (int i = 0; i < events.size(); i++) {
			Event ev = events.get(i);
			boolean matching = true;
			
			if (eventTitle != null && !ev.getTitle().equals(eventTitle)) 
				matching = false;
			
			if (eventType != null && ev.getType() != eventType)
				matching = false;
			
			if (startDate != null && !ev.getStart().equals(startDate))
				matching = false;
			
			if (matching) 
				return ev;
		}
		
		return null;
	}

	public Event matchEvent(int id) {
		for (int i = 0; i < events.size(); i++) {
			Event ev = events.get(i);
			
			if (ev.getId() == id) {
				return ev;
			}
		}
		
		return null;
	}
	
	public boolean hasEventAfter(int day, int hour) {
		for (int i = 0; i < events.size(); i++) {
			Event event = events.get(i);
			
			if (event.getStart().get(Calendar.DAY_OF_YEAR) == day) {
				if ( (event.getStart().get(Calendar.HOUR_OF_DAY) >= hour) || 
					 (event.getStart().get(Calendar.HOUR_OF_DAY) < hour && event.getEnd().get(Calendar.HOUR) > hour)
				) return true;
			}
		}
		
		return false;
	}

	public boolean canHostAll(EventList targetEvList, int hour) {
		List<Event> allTargetEvents = targetEvList.getAllEvents();
		
		for (int i = 0; i < allTargetEvents.size(); i++) {
			Event ev = allTargetEvents.get(i);
			
			if (ev.getStart().get(Calendar.HOUR_OF_DAY) >= hour) {
				for (int k = 0; k < events.size(); k++) {
					Event myEv = events.get(k);
					
					if ( myEv.overlaps(ev) ) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	public boolean canHost(Event targetEvent) {
		for (int k = 0; k < events.size(); k++) {
			Event myEv = events.get(k);
			if (myEv.overlaps(targetEvent)) {
				return false;
			}
		}
		
		return true;
	}

	public boolean canHost(Calendar start, Calendar end) {
		for (int k = 0; k < events.size(); k++) {
			Event myEv = events.get(k);
			if (myEv.overlaps(start, end)) {
				return false;
			}
		}
		
		return true;
	}
}
