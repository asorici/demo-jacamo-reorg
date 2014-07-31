package roombooking.core.request;

import java.util.Calendar;

import roombooking.core.SystemAgentId;
import cartago.AgentId;


public abstract class Request {
	public enum RequestType {
		schedule, delete, modify, inquire, participationIntention, participationCancelation,
		equipmentRequest
	}

	private static int idCounter = 0;
	private final int requestId = idCounter++;
	
	private SystemAgentId requesterAgentId;
	private AgentId callerAgentId;
	private RequestType type;
	private Calendar submissionDate;
	
	public Request(SystemAgentId requesterAgentId, AgentId callerAgentId, RequestType type) {
		this.callerAgentId = callerAgentId;
		this.requesterAgentId = requesterAgentId;
		this.type = type;
		submissionDate = Calendar.getInstance();
	}

	public int getId() {
		return requestId;
	}

	public RequestType getType() {
		return type;
	}
	
	public String getTypeString() {
		return type.name();
	}

	public Calendar getSubmissionDate() {
		return submissionDate;
	}

	public SystemAgentId getRequesterAgentId() {
		return requesterAgentId;
	}
	
	public AgentId getCallerAgentId() {
		return callerAgentId;
	}
	
	@Override
	public int hashCode() {
		return getId();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Request)) return false;
		
		Request reqObj = (Request)obj;
		if (reqObj.getId() != requestId) return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		String info = "request( " + type.name() + ":" + requestId + ":" + requesterAgentId.getRequesterName() + " )";
		return info;
	}
}
