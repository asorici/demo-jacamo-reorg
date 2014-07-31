package roombooking.core;

import java.util.HashMap;

import roombooking.core.UserRole.RoleType;
import roombooking.core.event.Event;


public class ParticipantList {
	private Event event;
	private HashMap<SystemAgentId, RoleType> participants;
	
	public ParticipantList(Event event) {
		this.event = event;
		participants = new HashMap<SystemAgentId, RoleType>();
	}

	public Event getEvent() {
		return event;
	}
	
	public HashMap<SystemAgentId, RoleType> getParticipants() {
		return participants;
	}
	
	public void addParticipant(SystemAgentId reqId, RoleType userRoleType) {
		participants.put(reqId, userRoleType);
	}
	
	public void removeParticipant(SystemAgentId reqId) {
		participants.remove(reqId);
	}
}
