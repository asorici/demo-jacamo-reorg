package roombooking.core.request;

import roombooking.core.SystemAgentId;
import roombooking.core.event.EventRequirements;
import cartago.AgentId;


public class ScheduleEventRequest extends Request {
	private EventRequirements requirements;
	
	public ScheduleEventRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, EventRequirements requirements) {
		super(requesterAgentId, callerAgentId, RequestType.schedule);
		this.requirements = requirements;
	}
	
	public EventRequirements getRequirements() {
		return requirements;
	}
	
	public void setRequirements(EventRequirements requirements) {
		this.requirements = requirements;
	}
	
}
