package roombooking.core.request;

import roombooking.core.SystemAgentId;
import roombooking.core.event.Event;
import cartago.AgentId;

public class DeleteEventRequest extends Request {
	private Event event;
	
	public DeleteEventRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, Event event) {
		super(requesterAgentId, callerAgentId, RequestType.delete);
		this.event = event;
	}
	
	public Event getEvent() {
		return event;
	}
}
