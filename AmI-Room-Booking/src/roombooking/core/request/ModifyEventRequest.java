package roombooking.core.request;

import roombooking.core.SystemAgentId;
import roombooking.core.event.Event;
import roombooking.core.event.EventRequirements;
import cartago.AgentId;

public class ModifyEventRequest extends Request {
	private Event event;
	private EventRequirements requirements;
	
	public ModifyEventRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, Event event, EventRequirements requirements) {
		super(requesterAgentId, callerAgentId, RequestType.modify);
		this.event = event;
		this.requirements = requirements;
	}

	public Event getEvent() {
		return event;
	}

	public EventRequirements getRequirements() {
		return requirements;
	}
}
