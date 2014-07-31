package roombooking.core.request;

import roombooking.core.SystemAgentId;

public class UserRequest {
	private SystemAgentId requesterId;
	private Request request;
	
	public UserRequest(SystemAgentId requesterId, Request request) {
		this.requesterId = requesterId;
		this.request = request;
	}

	public SystemAgentId getRequesterId() {
		return requesterId;
	}

	public Request getRequest() {
		return request;
	}
}
