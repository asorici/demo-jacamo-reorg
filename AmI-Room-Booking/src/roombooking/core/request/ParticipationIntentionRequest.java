package roombooking.core.request;

import roombooking.core.SystemAgentId;
import roombooking.core.UserRole;
import roombooking.core.event.Event;
import cartago.AgentId;

public class ParticipationIntentionRequest extends Request {
	private UserRole role;
	private Event event;
	
	public ParticipationIntentionRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, Event event, UserRole role) {
		super(requesterAgentId, callerAgentId, RequestType.participationIntention);
		this.event = event;
		this.role = role;
	}

	public UserRole getRole() {
		return role;
	}

	public Event getEvent() {
		return event;
	}
}
