package roombooking.core;

public class UserRole {
	public enum RoleType {
		presenter, participant 
	}
	
	private RoleType type;
	
	public UserRole(RoleType type) {
		this.type = type;
	}
	
	public RoleType getType() {
		return type;
	}
}
