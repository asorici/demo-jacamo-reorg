package roombooking.core;

import roombooking.artifacts.Constants;

public class Room {
	public enum RoomType {
		teachingRoom, brainstormRoom, meetingRoom
	}
	
	public enum RoomFormat {
		common, amphi, lab, office, any
	}
	
	private int id;
	private String managingAgentName;
	private RoomType type;
	private RoomFormat format;
	private int numSeats;
	
	public Room(int id, String typeStr, String formatStr, int numSeats, String managingAgentName) {
		RoomType type = RoomType.valueOf(typeStr);
		RoomFormat format = RoomFormat.valueOf(formatStr);
		
		this.id = id;
		this.type = type;
		this.format = format;
		this.numSeats = numSeats;
		this.managingAgentName = managingAgentName;
	}

	public RoomType getType() {
		return type;
	}

	public void setType(RoomType type) {
		this.type = type;
	}

	public int getNumSeats() {
		return numSeats;
	}

	public void setNumSeats(int numSeats) {
		this.numSeats = numSeats;
	}

	public int getId() {
		return id;
	}

	public RoomFormat getFormat() {
		return format;
	}

	public String getManagingAgentName() {
		return managingAgentName;
	}

	public void setManagingAgentName(String managingAgentName) {
		this.managingAgentName = managingAgentName;
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null) 
			return false;
		
		if (!(obj instanceof Room)) 
			return false;
		
		Room other = (Room) obj;
		/*
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} 
		else if (!type.equals(other.type)) {
			return false;
		}
		*/
		
		if (id != other.id) 
			return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		String info = "( Room:" + id + " managingAgent:" + managingAgentName 
						+ " type:" + type.name() + " format:" + format.name() + " )";
		return info;
	}

	public boolean compatible(String constrainedRoomType) {
		// check just the room format for now
		RoomType rType = RoomType.valueOf(constrainedRoomType);
		RoomFormat preferredFormat = RoomFormat.common;
		
		switch(rType) {
			case teachingRoom:
				preferredFormat = RoomFormat.lab;
			case brainstormRoom:
				preferredFormat = RoomFormat.office;
		}
		
		return Constants.compatibleRoomFormat(preferredFormat, getFormat());
	}

}
