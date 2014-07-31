package roombooking.core.equipment;

import java.util.Calendar;

import roombooking.core.event.Event;

public class EquipmentBookingSlot {
	private Calendar start;
	private Calendar end;
	private Event event;
	
	public EquipmentBookingSlot(Calendar start, Calendar end) {
		this.start = start;
		this.end = end;
	}

	public EquipmentBookingSlot(Event event) {
		this(event.getStart(), event.getEnd());
		this.event = event;
	}
	
	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Calendar getStart() {
		return start;
	}

	public Calendar getEnd() {
		return end;
	}
	
	@Override
	public int hashCode() {
		return start.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		
		if (!(obj instanceof EquipmentBookingSlot)) return false;
		
		EquipmentBookingSlot otherSlot = (EquipmentBookingSlot)obj;
		if (!start.equals(otherSlot.getStart()) || !end.equals(otherSlot.getEnd())) return false;
		
		if (event != null && otherSlot.getEvent() == null) return false;
		if (event == null && otherSlot.getEvent() != null) return false;
		
		if (event != null && otherSlot.getEvent() != null && !event.equals(otherSlot.getEvent())) return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		String info = "start: " + start.get(Calendar.DAY_OF_YEAR) + "-" + start.get(Calendar.HOUR_OF_DAY) 
		+ " end: " + end.get(Calendar.DAY_OF_YEAR) + "-" + end.get(Calendar.HOUR_OF_DAY);
		
		if (event != null) {
			info += " room: " + event.getEventRoom();
		}
		
		return info;
	}
}
