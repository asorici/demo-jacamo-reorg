package roombooking.artifacts;

import java.util.HashMap;

import roombooking.core.Room;
import roombooking.core.Room.RoomType;
import roombooking.core.event.Event.EventType;
import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.LINK;
import cartago.OPERATION;

public class MonitorArtifact extends Artifact {
	private HashMap<EventType, Integer> requestCounter;
	private HashMap<RoomType, Double> energyConsumption;
	private HashMap<Room, Double> energyConsumptionByRoom;
	
	private double totalEnergyConsumption = 0;
	
	private long anouncementPeriod = 7 * 12 * Constants.HOUR_UNIT;		// every week (working hours only) 
																		// try to model global picture
	protected void init() {
		requestCounter = new HashMap<EventType, Integer>();
		energyConsumption = new HashMap<RoomType, Double>();
		energyConsumptionByRoom = new HashMap<Room, Double>();
		
		defineObsProperties();
		
		execInternalOp("issueMonitoringUpdates");
	}
	
	private void defineObsProperties() {
		energyConsumption.put(RoomType.teachingRoom, 0.0);
		energyConsumption.put(RoomType.brainstormRoom, 0.0);
		energyConsumption.put(RoomType.meetingRoom, 0.0);
		
		requestCounter.put(EventType.teachingEvent, 0);
		requestCounter.put(EventType.brainstormEvent, 0);
		requestCounter.put(EventType.meetingEvent, 0);
		
		defineObsProperty("requestCounter", requestCounter);
		defineObsProperty("energyConsumptionByRoom", energyConsumptionByRoom);
		defineObsProperty("energyConsumption", energyConsumption);
		defineObsProperty("totalEnergyConsumption", totalEnergyConsumption);
	}

	@INTERNAL_OPERATION
	protected void issueMonitoringUpdates() {
		while(true) {
			// gather usage from all rooms into room types and total amount
			
			totalEnergyConsumption = 0;
			for (Room r : energyConsumptionByRoom.keySet()) {
				Double estimatedRoomConsumption = energyConsumptionByRoom.get(r);
				
				Double amount = energyConsumption.get(r.getType());
				if (amount != null) {
					energyConsumption.put(r.getType(), amount + estimatedRoomConsumption);
				}
				else {
					energyConsumption.put(r.getType(), estimatedRoomConsumption);
				}
				
				totalEnergyConsumption += estimatedRoomConsumption;
			}
			
			getObsProperty("requestCounter").updateValue(requestCounter);
			getObsProperty("energyConsumption").updateValue(energyConsumption);
			getObsProperty("totalEnergyConsumption").updateValue(totalEnergyConsumption);
			
			await_time(anouncementPeriod);
		}
	}
	
	@OPERATION @LINK
	protected void logRequest(EventType eventType, int amount) {
		Integer reqTypeQuantum = requestCounter.get(eventType);
		if (reqTypeQuantum == null) {
			requestCounter.put(eventType, amount);
		}
		else {
			reqTypeQuantum += amount;
			requestCounter.put(eventType, reqTypeQuantum);
		}
	}
	
	@OPERATION @LINK
	protected void logEnergyUsage(Room room, double amount) {
		energyConsumptionByRoom.put(room, amount);
		//getObsProperty("energyConsumptionByRoom").updateValue(energyConsumptionByRoom);
	}
}
