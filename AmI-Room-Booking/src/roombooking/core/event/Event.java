package roombooking.core.event;

import java.util.Calendar;

import moise.prolog.ToProlog;
import roombooking.core.Room;
import roombooking.core.SystemAgentId;


public class Event { //implements ToProlog {
	//private static long counter = 0; 
	
	public enum EventType {
		teachingEvent, brainstormEvent, meetingEvent
	}
	
	private int id;
	private SystemAgentId ownerUserId;
	private EventRequirements eventRequirements;

	private Room eventRoom;
	
	private int hourPeriod = 0;
	private int dayPeriod = 0;
	private int numPeriods = 0;
	
	public Event(SystemAgentId ownerUserId, int id, Room eventRoom, EventRequirements eventRequirements, int hourPeriod, int dayPeriod, int numPeriods) {
		this.id = id;
		this.ownerUserId = ownerUserId;
		this.setEventRoom(eventRoom);
		this.eventRequirements = eventRequirements;
		this.hourPeriod = hourPeriod;
		this.dayPeriod = dayPeriod;
		this.numPeriods = numPeriods;
	}
	
	public Event(SystemAgentId ownerUserId, int id, Room eventRoom, EventRequirements eventRequirements) {
		this(ownerUserId, id, eventRoom, eventRequirements, 0, 0, 0);
	}

	public Calendar getStart() {
		return eventRequirements.getStartDate();
	}

	public void setStart(Calendar start) {
		eventRequirements.setStartDate(start);
	}

	public Calendar getEnd() {
		return eventRequirements.getEndDate();
	}

	public void setEnd(Calendar end) {
		eventRequirements.setEndDate(end);
	}

	public int getHourPeriod() {
		return hourPeriod;
	}

	public void setHourPeriod(int hourPeriod) {
		this.hourPeriod = hourPeriod;
	}

	public int getDayPeriod() {
		return dayPeriod;
	}

	public void setDayPeriod(int dayPeriod) {
		this.dayPeriod = dayPeriod;
	}

	public int getNumPeriods() {
		return numPeriods;
	}

	public void setNumPeriods(int numPeriods) {
		this.numPeriods = numPeriods;
	}

	public int getId() {
		return id;
	}

	public void setEventRoom(Room eventRoom) {
		this.eventRoom = eventRoom;
	}

	public Room getEventRoom() {
		return eventRoom;
	}
	
	public EventType getType() {
		return eventRequirements.getType();
	}
	
	public String getTitle() {
		return eventRequirements.getTitle();
	}

	public SystemAgentId getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(SystemAgentId ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
	
	public boolean overlaps(Event ev) {
		if (this.getStart().compareTo(ev.getEnd()) >= 0 || this.getEnd().compareTo(ev.getStart()) <= 0) return false;
		
		return true;
	}
	
	public boolean overlaps(Calendar start, Calendar end) {
		if (this.getStart().compareTo(end) >= 0 || this.getEnd().compareTo(start) <= 0) return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (int)getId();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Event)) return false;
		
		Event eventObj = (Event)obj;
		//if (eventObj.getType() != getType()) return false;
		
		if (eventObj.getId() != getId()) return false;		
		
		return true;
	}
	
	@Override
	public String toString() {
		String info = "(Event:" + id + " type:" + eventRequirements.getType().name() + 
		" title:" + eventRequirements.getTitle() + " room:" + eventRoom.toString() + ")";
		
		return info;
	}
	
	//@Override
	public String getAsProlog() {
		String info = "event(" + id + "," + "\"" + eventRequirements.getTitle() + "\"," + eventRequirements.getType().name() 
							   	+ "," + eventRequirements.getStartDate().get(Calendar.HOUR_OF_DAY) + "," 
							   	+ eventRequirements.getEndDate().get(Calendar.HOUR_OF_DAY) + ")";
		
		return info;
	}

	public EventRequirements getEventRequirements() {
		return eventRequirements;
	}
	
}
