package roombooking.core.request;

import java.util.Date;

import roombooking.core.SystemAgentId;
import roombooking.core.event.Event.EventType;
import cartago.AgentId;


public class InquireEventsRequest extends Request {
	private String eventTitle;
	private EventType eventType;
	private Date startDate;
	
	public InquireEventsRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, String eventTitle, EventType eventType, Date startDate) {
		super(requesterAgentId, callerAgentId, RequestType.inquire);
		this.eventTitle = eventTitle;
		this.eventType = eventType;
		this.startDate = startDate;
	}

	public String getEventTitle() {
		return eventTitle;
	}

	public EventType getEventType() {
		return eventType;
	}
	
	public Date getStartDate() {
		return startDate;
	}
}
