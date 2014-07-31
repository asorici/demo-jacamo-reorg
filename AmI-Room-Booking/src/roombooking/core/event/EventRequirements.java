package roombooking.core.event;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.parser.ParseException;

import java.util.Calendar;

import moise.prolog.ToProlog;
import roombooking.core.Room.RoomFormat;
import roombooking.core.event.Event.EventType;
import roombooking.util.DateDifference;


public class EventRequirements implements ToProlog {
	
	private String title = "unknown";
	private EventType type;
	private int numSeats;
	private Calendar startDate;
	private Calendar endDate;
	
	private RoomFormat roomFormat = RoomFormat.common;
	private boolean hasBlackboard = false;
	private boolean hasProjector = false;
	private boolean hasTv = false;
	private boolean hasMic = false;
	
	public EventRequirements(String title, String type, int numSeats, Calendar startDate, Calendar endDate) {
		this.type = EventType.valueOf(type);
		this.title = title;
		this.numSeats = numSeats;
		this.startDate = startDate;
		this.endDate = endDate;
		//endDate.set(Calendar.HOUR_OF_DAY, 12);
		
	}
	
	public boolean hasBlackboard() {
		return hasBlackboard;
	}
	
	public void setBlackboard(boolean hasBlackboard) {
		this.hasBlackboard = hasBlackboard;
	}
	
	public boolean hasProjector() {
		return hasProjector;
	}
	
	public void setProjector(boolean hasProjector) {
		this.hasProjector = hasProjector;
	}
	
	public boolean hasTv() {
		return hasTv;
	}
	
	public void setTv(boolean hasTv) {
		this.hasTv = hasTv;
	}

	public boolean hasMic() {
		return hasMic;
	}

	public void setMic(boolean hasMic) {
		this.hasMic = hasMic;
	}

	public String getTitle() {
		return title;
	}
	
	public EventType getType() {
		return type;
	}
	
	public String getTypeString() {
		return type.name();
	}
	
	public int getNumSeats() {
		return numSeats;
	}
	
	public void setNumSeats(int numSeats) {
		this.numSeats = numSeats;
	}
	
	public Calendar getStartDate() {
		return startDate;
	}
	
	public void setStartDate(Calendar startDate) {
		this.startDate = startDate;
	}

	public Calendar getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Calendar endDate) {
		this.endDate = endDate;
	}

	public RoomFormat getRoomFormat() {
		return roomFormat;
	}

	public void setRoomFormat(RoomFormat roomFormat) {
		this.roomFormat = roomFormat;
	}
	
	@Override
	public String toString() {
		String info = "eventReq(" + "\"" + title + "\"," + type.name() + "," 
		+ "\"" + DateDifference.calendar2string(startDate) + "\","
		+ "\"" + DateDifference.calendar2string(endDate) + "\","
		+ roomFormat.name() + "," + numSeats + "," 
		+ "hasBlackboard(" + hasBlackboard + ")," + "hasProjector("+ hasProjector +"),"
		+ "hasTv(" + hasTv + ")," + "hasMic(" + hasMic + "))";
		
		return info;
	}
	
	public int hashCode() {
		//return startDate.get(Calendar.DAY_OF_YEAR) * 24 * 60 + startDate.get(Calendar.HOUR_OF_DAY) * 60 +
		//		startDate.get(Calendar.MINUTE);
		return startDate.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (! (obj instanceof EventRequirements) ) return false;
		
		EventRequirements otherReq = (EventRequirements)obj;
		if (!otherReq.getStartDate().equals(startDate) || !otherReq.getEndDate().equals(endDate)) return false;
		
		if (type != otherReq.getType() || !title.equals(otherReq.getTitle())) return false;
		if (roomFormat != otherReq.getRoomFormat()) return false;
		if (numSeats != otherReq.getNumSeats()) return false;
		
		if (hasBlackboard && !otherReq.hasBlackboard()) return false;
		if (hasTv && !otherReq.hasTv()) return false;
		if (hasMic && !otherReq.hasMic()) return false;
		if (hasProjector && !otherReq.hasProjector()) return false;
		
		return true;
	}

	@Override
	public String getAsProlog() {
		String info = "eventReq(" + "\"" + title + "\"," + type.name() + "," 
		+ "\"" + DateDifference.calendar2string(startDate) + "\","
		+ "\"" + DateDifference.calendar2string(endDate) + "\","
		+ roomFormat.name() + "," + numSeats + "," 
		+ "hasBlackboard(" + hasBlackboard + ")," + "hasProjector("+ hasProjector +"),"
		+ "hasTv(" + hasTv + ")," + "hasMic(" + hasMic + "))";

		return info;
	}
	
	public static EventRequirements parseProlog(String evReqStr) throws ParseException {
		Literal evReq = ASSyntax.parseLiteral(evReqStr);
		
		String title = evReq.getTerm(0).toString();
		title = title.substring(1, title.length() - 1);
		String type = evReq.getTerm(1).toString();
		
		String startStr = evReq.getTerm(2).toString();	// remember to cut the quotes
		Calendar startDate = DateDifference.string2calendar(startStr.substring(1, startStr.length() - 1));
		
		String endStr = evReq.getTerm(3).toString();	// remember to cut the quotes
		Calendar endDate = DateDifference.string2calendar(endStr.substring(1, endStr.length() - 1));
		
		RoomFormat format = RoomFormat.valueOf(evReq.getTerm(4).toString());
		int numSeats = Integer.parseInt(evReq.getTerm(5).toString());
		
		Literal blackboard = ASSyntax.parseLiteral(evReq.getTerm(6).toString());
		boolean blackboardRequired = Boolean.parseBoolean(blackboard.getTerm(0).toString()); 
		
		Literal projector = ASSyntax.parseLiteral(evReq.getTerm(7).toString());
		boolean projectorRequired = Boolean.parseBoolean(projector.getTerm(0).toString()); 
		
		Literal tv = ASSyntax.parseLiteral(evReq.getTerm(8).toString());
		boolean tvRequired = Boolean.parseBoolean(tv.getTerm(0).toString());
		
		Literal mic = ASSyntax.parseLiteral(evReq.getTerm(9).toString());
		boolean micRequired = Boolean.parseBoolean(mic.getTerm(0).toString());
		
		EventRequirements eventRequirements = new EventRequirements(title, type, numSeats, startDate, endDate);
		eventRequirements.setRoomFormat(format);
		eventRequirements.setBlackboard(blackboardRequired);
		eventRequirements.setProjector(projectorRequired);
		eventRequirements.setTv(tvRequired);
		eventRequirements.setMic(micRequired);
		
		return eventRequirements;
	}
}
