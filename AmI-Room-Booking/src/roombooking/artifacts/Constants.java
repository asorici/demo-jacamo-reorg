package roombooking.artifacts;

import java.util.HashMap;

import roombooking.core.Room.RoomFormat;
import roombooking.core.equipment.Equipment.EquipmentType;


public class Constants {
	public static final String REQUEST_SIGNAL 	= "new_request";
	public static final String EVENT_DELETED 	= "event_deleted";
	public static final String EVENT_MODIFIED 	= "event_modified";
	public static final String PARTICIPATION_ANOUNCED = "participation_anounced";
	public static final String PARTICIPATION_CANCELED = "participation_canceled";
	
	public static final String ERROR_EVENT_OVER 				= "error_event_over";
	public static final String ERROR_EVENT_PENDING_DELETION 	= "error_event_pending_deletion";
	public static final String ERROR_EVENT_UNMATCHED 			= "error_event_unmatched";
	
	public static final String ERROR_PARTICIPATION_ALREADY_CANCELED = "error_participation_already_canceled";
	
	public static HashMap<EquipmentType, Integer> equipmentEnergyFactors = new HashMap<EquipmentType, Integer>();
	static {
		equipmentEnergyFactors = new HashMap<EquipmentType, Integer>();
		equipmentEnergyFactors.put(EquipmentType.light, 30);		// Wh per light-bulb / neon
		equipmentEnergyFactors.put(EquipmentType.heating, 80);		// Wh per heating utility
		equipmentEnergyFactors.put(EquipmentType.projector, 50);	// Wh per projector
		equipmentEnergyFactors.put(EquipmentType.tv, 40);			// Wh per tv
		equipmentEnergyFactors.put(EquipmentType.microphone, 5);	// Wh per microphone
	}
	
	public static HashMap<RoomFormat, Integer> lightUtility2roomTypeMap = new HashMap<RoomFormat, Integer>();
	public static HashMap<RoomFormat, Integer> heatUtility2roomTypeMap = new HashMap<RoomFormat, Integer>();
	static {
		lightUtility2roomTypeMap.put(RoomFormat.amphi, 8);
		lightUtility2roomTypeMap.put(RoomFormat.common, 4);
		lightUtility2roomTypeMap.put(RoomFormat.office, 2);
		lightUtility2roomTypeMap.put(RoomFormat.lab, 6);
		
		heatUtility2roomTypeMap.put(RoomFormat.amphi, 4);
		heatUtility2roomTypeMap.put(RoomFormat.common, 2);
		heatUtility2roomTypeMap.put(RoomFormat.office, 2);
		heatUtility2roomTypeMap.put(RoomFormat.lab, 3);
	}
	
	public static final int HOUR_UNIT 		= 10;					// duration of an hour expressed in seconds
	public static final int DAY_START_HOUR 	= 8;
	public static final int DAY_END_HOUR 	= 20;
	
	public static boolean compatibleRoomFormat(RoomFormat f1, RoomFormat f2) {
		switch(f1) {
			case amphi:
				if (f2 == RoomFormat.amphi) return true;
				else return false;
			case common:
				if (f2 == RoomFormat.common || f2 == RoomFormat.lab || f2 == RoomFormat.office) return true;
				else return false;
			case office:
				if (f2 == RoomFormat.office || f2 == RoomFormat.common) return true;
				else return false;
			case lab:
				if (f2 == RoomFormat.lab || f2 == RoomFormat.common) return true;
				else return false;
			case any:
				return true;
			default:
				return false;
		}
	}
}
