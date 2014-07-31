package roombooking.artifacts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import roombooking.core.Room;
import roombooking.core.equipment.Equipment;
import roombooking.core.equipment.EquipmentBookingSlot;
import roombooking.core.equipment.EquipmentState;
import roombooking.core.equipment.Equipment.EquipmentType;
import roombooking.core.event.Event;
import roombooking.core.event.EventRequirements;
import cartago.Artifact;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;


public class EquipmentStateArtifact extends Artifact {
	private List<Equipment> equipments;
	
	protected void init() {
		equipments = new ArrayList<Equipment>();
		testingInit();
	}
	
	@Override
	protected void dispose() {
		System.out.println("Disposing EquipmentStateArtifact");
		super.dispose();
	}
	
	private void testingInit() {
		// create a list of simulation equipments
		int numBlackboards = 7;
		int numProjectors = 6;
		int numTvs = 2;
		int numMicSets = 1;
		
		int eqId = 1;
		
		// set all the blackboards
		EquipmentType eqType = EquipmentType.blackboard; 
		for (int i = 0; i < numBlackboards; i++) {
			int eqEnergyConsumption = 0;
			Integer energyUnits = Constants.equipmentEnergyFactors.get(eqType);
			if (energyUnits != null) {
				eqEnergyConsumption = energyUnits;
			}
			
			Equipment eq = new Equipment(eqId, eqType, eqEnergyConsumption);
			eq.setEquipmentState(new EquipmentState(eq, false));
			
			equipments.add(eq);
			eqId++;
		}
		
		// set all the projectors
		eqType = EquipmentType.projector; 
		for (int i = 0; i < numProjectors; i++) {
			int eqEnergyConsumption = 0;
			Integer energyUnits = Constants.equipmentEnergyFactors.get(eqType);
			if (energyUnits != null) {
				eqEnergyConsumption = energyUnits;
			}
			
			Equipment eq = new Equipment(eqId, eqType, eqEnergyConsumption);
			
			if (i < 2 * numProjectors / 3) {
				eq.setEquipmentState(new EquipmentState(eq, true));
			}
			else {
				eq.setEquipmentState(new EquipmentState(eq, false));
			}
			
			equipments.add(eq);
			eqId++;
		}
		
		// set all the tvs
		eqType = EquipmentType.tv; 
		for (int i = 0; i < numTvs; i++) {
			int eqEnergyConsumption = 0;
			Integer energyUnits = Constants.equipmentEnergyFactors.get(eqType);
			if (energyUnits != null) {
				eqEnergyConsumption = energyUnits;
			}
			
			Equipment eq = new Equipment(eqId, eqType, eqEnergyConsumption);
			eq.setEquipmentState(new EquipmentState(eq, false));
			
			equipments.add(eq);
			eqId++;
		}
		
		// set all the mics
		eqType = EquipmentType.microphone; 
		for (int i = 0; i < numMicSets; i++) {
			int eqEnergyConsumption = 0;
			Integer energyUnits = Constants.equipmentEnergyFactors.get(eqType);
			if (energyUnits != null) {
				eqEnergyConsumption = energyUnits;
			}
			
			Equipment eq = new Equipment(eqId, eqType, eqEnergyConsumption);
			eq.setEquipmentState(new EquipmentState(eq, false));
			
			equipments.add(eq);
			eqId++;
		}
	}
	
	private List<Equipment> filterByType(EquipmentType eqType) {
		List<Equipment> filteredEquipments = new ArrayList<Equipment>();
		
		for(Equipment eq : equipments) {
			if (eq.getType() == eqType) {
				filteredEquipments.add(eq);
			}
		}
		
		return filteredEquipments;
	}
	
	@OPERATION
	protected void initializeEquipment(List<Equipment> initialEquipments) {
		equipments.addAll(initialEquipments);
	}
	
	@OPERATION
	protected void addEquipment(Equipment eq) {
		equipments.add(eq);
	}
	
	@OPERATION
	protected void removeEquipment(Equipment eq) {
		equipments.remove(eq);
	}
	
	@LINK @OPERATION
	protected void getEquipment(int eqId, OpFeedbackParam<Equipment> equip) {
		for (Equipment eq : equipments) {
			if (eq.getId() == eqId) {
				equip.set(eq);
				return;
			}
		}
		
		equip.set(null);
	}
	
	@LINK
	protected void requestEquipment(EquipmentType eqType, EventRequirements evReq, OpFeedbackParam<Equipment> resultEquipment) {
		List<Equipment> filteredEquipments = filterByType(eqType);
		
		// strategy - search for an available device otherwise queue the agent to the device
		// that has the least number of other rooms waiting for it.
		
		Equipment freeEq = null;
		for (Equipment eq : filteredEquipments) { 
			EquipmentState eqState = eq.getEquipmentState();
			if (eqState.isMovable()) {
				if (!eqState.isBooked()) {
					freeEq = eq;
					break;
				}
			}
		}
		
		if (freeEq != null) {
			// if a free device was found - change its state accordingly and return it as a result 
			freeEq.getEquipmentState().setBooked(true);
			
			EquipmentBookingSlot newSlot = new EquipmentBookingSlot(evReq.getStartDate(), evReq.getEndDate());
			//freeEq.getEquipmentState().setCurrentSlot(newSlot);
			freeEq.getEquipmentState().insertBookingSlot(newSlot);
			
			resultEquipment.set(freeEq);
		}
		else {
			// if no free device could be found try and see if we can find an opening 
			// in one of the devices that is already booked 
			// (if we can find a slot for our event)
			Equipment freeSlotEquipment = null;
			
			for (Equipment eq : filteredEquipments) {
				List<EquipmentBookingSlot> pretenderBookingSlots = eq.getEquipmentState().getEquipmentBookingSlots();
				
				synchronized(pretenderBookingSlots) {
					// since agents can both request and release equipment
					// first, the pretenderBooking slot list might be empty since the equipment is currently booked
					// but nobody else is waiting
					
					if (pretenderBookingSlots.isEmpty()) {
						EquipmentBookingSlot newSlot = new EquipmentBookingSlot(evReq.getStartDate(), evReq.getEndDate());
						eq.getEquipmentState().insertBookingSlot(newSlot);
						
						freeSlotEquipment = eq;
						break;
					}
					
					// if the list is not empty, check if the new event fits in
					int slotIndex = -1;
					
					for (int i = 0; i < pretenderBookingSlots.size() - 1; i++) {
						EquipmentBookingSlot bookingSlot = pretenderBookingSlots.get(i);
						EquipmentBookingSlot nextBookingSlot = pretenderBookingSlots.get(i + 1);
						
						if (evReq.getStartDate().compareTo(bookingSlot.getEnd()) >= 0 && 
								evReq.getEndDate().compareTo(nextBookingSlot.getStart()) <= 0) {
							
							// we have found a slot
							slotIndex = i + 1;
							break;
						}
					}
					
					if (slotIndex != -1) {
						// if we have found a slot in one of the waiting lists, return that device as a result
						// considering the ways in which agents book equipments, the requester agent will
						// be sure to have it ready at the needed time
						
						EquipmentBookingSlot newSlot = new EquipmentBookingSlot(evReq.getStartDate(), evReq.getEndDate());
						eq.getEquipmentState().insertBookingSlot(newSlot, slotIndex);
						freeSlotEquipment = eq;
						break;
					}
					
					// if we have gotten here it is either because there is no slot or all pretender
					// events finish earlier than the requesterEvent starts - so let's check this
					EquipmentBookingSlot lastSlot = pretenderBookingSlots.get(pretenderBookingSlots.size() - 1);
					if (evReq.getStartDate().compareTo(lastSlot.getEnd()) >= 0) {
						EquipmentBookingSlot newSlot = new EquipmentBookingSlot(evReq.getStartDate(), evReq.getEndDate());
						eq.getEquipmentState().insertBookingSlot(newSlot);
						
						freeSlotEquipment = eq;
						break;
					}
				}
			}
			
			// if any equipment had an opening in its pretenderList, the statement below will return
			// a value different from NULL
			resultEquipment.set(freeSlotEquipment);
		}
	}
	
	@LINK
	protected void bookEquipmentConfirm(Equipment equipment, Event ev) {
		// we can safely assume that the slot has been previously allotted via requestEquipment
		// the slot can be given up either by specifying the event or the start and end dates
		
		for (Equipment eq : equipments) {
			if (eq.equals(equipment)) {
				EquipmentBookingSlot slot = eq.getEquipmentState().matchBookingSlot(ev.getStart(), ev.getEnd());
				
				if (slot != null) {
					slot.setEvent(ev);
				}
				else {
					System.out.println("[EQ_STATE] WARNING! Event " + ev + " matched no slot for equipment " + equipment);
				}
			}
		}
	}
	
	@LINK
	protected void releaseEquipment(Equipment releasedEq, Event event) {
		EquipmentBookingSlot existingSlot = new EquipmentBookingSlot(event);
		releaseSlot(releasedEq, existingSlot);
	}
	
	@LINK
	protected void releaseEquipment(Equipment releasedEq, Calendar start, Calendar end) {
		EquipmentBookingSlot existingSlot = new EquipmentBookingSlot(start, end);
		releaseSlot(releasedEq, existingSlot);
	}
	
	private void releaseSlot(Equipment releasedEq, EquipmentBookingSlot existingSlot) {
		// identify Equipment from list
		for (Equipment eq : equipments) {
			if (eq.equals(releasedEq)) {
				EquipmentState eqState = eq.getEquipmentState();
				
				// the room agent that gives up the eq must host an event that is in the waiting list 
				// just remove the event from the list 
				eqState.removeBookingSlot(existingSlot);
				if (eqState.getEquipmentBookingSlots().isEmpty()) {
					// if no one is waiting in the list it means this event was the only one so
					// i will just mark the equipment as free
						
					eqState.setBooked(false);
				}
				
				// since equipments have unique IDs we can stop when we have found the required one
				break;
			}
		}
	}
	
	
	
	@OPERATION
	protected void updateEventRoom(int eventId, Room room) {
		for (Equipment eq : equipments) {
			for (EquipmentBookingSlot slot : eq.getEquipmentState().getEquipmentBookingSlots()) {
				if (slot.getEvent() != null && slot.getEvent().getId() == eventId) {
					slot.getEvent().setEventRoom(room);
					return;
				}
			}
		}
	}
}
