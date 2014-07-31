package roombooking.core.equipment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class EquipmentState {
	private boolean booked = false;
	private boolean movable = false;
	
	private Equipment equipment;
	//private EquipmentBookingSlot currentSlot;
	// private DateDifference bookingTime;
	
	// sorted queue of pretender events for this equipment - to be used as an auxiliary structure
	private List<EquipmentBookingSlot> equipmentBookingSlots;
	
	public EquipmentState(Equipment equipment, boolean movable) {
		this.equipment = equipment;
		this.movable = movable;
		equipmentBookingSlots = new ArrayList<EquipmentBookingSlot>();
	}

	public synchronized boolean isBooked() {
		return booked;
	}

	public synchronized void setBooked(boolean booked) {
		this.booked = booked;
	}

	/*
	public EquipmentBookingSlot getCurrentSlot() {
		return currentSlot;
	}

	public synchronized void setCurrentSlot(EquipmentBookingSlot currentSlot) {
		this.currentSlot = currentSlot;
	}
	*/
	
	/*
	public DateDifference getBookingTime() {
		return bookingTime;
	}

	public void setBookingTime(DateDifference bookingTime) {
		this.bookingTime = bookingTime;
	}
	*/
	
	public boolean isMovable() {
		return movable;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public List<EquipmentBookingSlot> getEquipmentBookingSlots() {
		return equipmentBookingSlots;
	}
	
	public void insertBookingSlot(EquipmentBookingSlot bookingSlot) {
		synchronized(equipmentBookingSlots) {
			equipmentBookingSlots.add(bookingSlot);
		}
	}
	
	public void insertBookingSlot(EquipmentBookingSlot bookingSlot, int index) {
		synchronized(equipmentBookingSlots) {
			equipmentBookingSlots.add(index, bookingSlot);
		}
	}
	
	public void removeBookingSlot(EquipmentBookingSlot bookingSlot) {
		synchronized(equipmentBookingSlots) {
			equipmentBookingSlots.remove(bookingSlot);
		}
	}
	
	public EquipmentBookingSlot getNextSlot() {
		synchronized(equipmentBookingSlots) {
			if (equipmentBookingSlots.isEmpty()) {
				return null;
			}
			else {
				return equipmentBookingSlots.remove(0);
			}
		}
	}

	public EquipmentBookingSlot matchBookingSlot(Calendar start, Calendar end) {
		for (EquipmentBookingSlot slot : equipmentBookingSlots) {
			if (slot.getStart().equals(start) && slot.getEnd().equals(end)) { 
				return slot;
			}
		}
		
		return null;
	}
}
