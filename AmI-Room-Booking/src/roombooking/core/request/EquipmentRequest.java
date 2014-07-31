package roombooking.core.request;

import java.util.Date;

import roombooking.core.Room;
import roombooking.core.SystemAgentId;
import roombooking.core.equipment.Equipment.EquipmentType;
import cartago.AgentId;


public class EquipmentRequest extends Request {
	private EquipmentType eqType;
	private Room requesterRoom;
	private Date startDate;
	private Date endDate;
	
	public EquipmentRequest(SystemAgentId requesterAgentId, AgentId callerAgentId, EquipmentType eqType, Room requesterRoom, Date startDate, Date endDate) {
		super(requesterAgentId, callerAgentId, RequestType.equipmentRequest);
		this.eqType = eqType;
		this.requesterRoom = requesterRoom;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public EquipmentType getRequestedEquipment() {
		return eqType;
	}

	public Room getRequesterRoom() {
		return requesterRoom;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
}
