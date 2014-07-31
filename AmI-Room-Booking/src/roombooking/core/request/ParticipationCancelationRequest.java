package roombooking.core.request;

import roombooking.core.SystemAgentId;
import roombooking.core.event.Event;
import cartago.AgentId;

public class ParticipationCancelationRequest extends Request {
	private Event event;
	
	public ParticipationCancelationRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, Event event) {
		super(requesterAgentId, callerAgentId, RequestType.participationCancelation);
		this.event = event;
	}

	public Event getEvent() {
		return event;
	}
}
