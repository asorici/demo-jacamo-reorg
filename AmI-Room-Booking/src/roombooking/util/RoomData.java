package roombooking.util;

import java.util.ArrayList;
import java.util.List;

import roombooking.core.Room.RoomFormat;

public class RoomData {
	private int id;
	private RoomFormat format;
	private int numSeats;
	private List<Integer> equipmentIds;

	public RoomData(int id, RoomFormat format, int numSeats, List<Integer> equipmentIds) {
		super();
		this.id = id;
		this.format = format;
		this.numSeats = numSeats;
		this.equipmentIds = equipmentIds;
	}

	public int getId() {
		return id;
	}

	public RoomFormat getFormat() {
		return format;
	}

	public int getNumSeats() {
		return numSeats;
	}

	public List<Integer> getEquipmentIds() {
		return equipmentIds;
	}
}
