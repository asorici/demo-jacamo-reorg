package roombooking.util;

import java.util.Comparator;

import roombooking.core.event.Event;


public class EventComparator implements Comparator<Event> {
	@Override
	public int compare(Event e1, Event e2) {
		return e1.getStart().compareTo(e2.getStart());
	}
}
