package roombooking.core;



public class SystemAgentId {
	private static int counter = 0;
	private final int id = counter++;
	
	private String globalAgentId;
	private String agentName;
	
	public SystemAgentId(String agentName, String globalAgentId) {
		this.globalAgentId = globalAgentId;
		this.agentName = agentName;
	}
	
	public int getId() {
		return id;
	}
	
	public String getRequesterName() {
		return agentName;
	}
	
	
	public String getGlobalAgentId() {
		return globalAgentId;
	}
	
	
	@Override
	public int hashCode() {
		return id;
		//return globalAgentId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SystemAgentId)) {
			return false;
		}
		
		SystemAgentId idObj = (SystemAgentId)obj;
		
		if (!idObj.getRequesterName().equals(getRequesterName())) {
			return false;
		}
		
		if (idObj.getId() != id) return false;
		
		return true;
	}
}
