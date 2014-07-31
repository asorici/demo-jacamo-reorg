package roombooking.core.equipment;

public class Equipment {
	public enum EquipmentType {
		light, heating, projector, whiteboard, blackboard, tv, microphone
	}
	
	private int id;
	private EquipmentType type;
	private EquipmentState equipmentState;
	
	private int estimatedEnergyConsumption;		// measured in WH
	
	public Equipment(int id, EquipmentType type, int estimatedEnergyConsumption) {
		this.id = id;
		this.type = type;
		this.estimatedEnergyConsumption = estimatedEnergyConsumption;
	}
	
	public EquipmentState getEquipmentState() {
		return equipmentState;
	}
	
	public void setEquipmentState(EquipmentState equipmentState) {
		this.equipmentState = equipmentState;
	}
	
	public int getId() {
		return id;
	}
	
	public EquipmentType getType() {
		return type;
	}

	public int getEstimatedEnergyConsumption() {
		return estimatedEnergyConsumption;
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
		
		if (!(obj instanceof Equipment))
			return false;
		
		Equipment other = (Equipment) obj;
		if (id != other.id)
			return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		String info = "( Equipment: " + id + " type:" + type.name() + " movable:" + equipmentState.isMovable();
		if (equipmentState.isMovable()) {
			info += " booked:" + equipmentState.isBooked();
			
			if (equipmentState.isBooked() && !equipmentState.getEquipmentBookingSlots().isEmpty()) {
				info += " next slot:" + equipmentState.getEquipmentBookingSlots().get(0);
			}
		}
		
		info += " )";
		return info;
	}
}
