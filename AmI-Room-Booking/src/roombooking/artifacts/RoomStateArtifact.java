package roombooking.artifacts;

import jason.asSyntax.parser.ParseException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import roombooking.core.Room;
import roombooking.core.Room.RoomType;
import roombooking.core.UserRole.RoleType;
import roombooking.core.equipment.Equipment;
import roombooking.core.equipment.Equipment.EquipmentType;
import roombooking.core.event.Event;
import roombooking.core.event.EventRequirements;
import roombooking.core.event.Event.EventType;
import roombooking.util.DateDifference;
import roombooking.util.EventComparator;
import cartago.ARTIFACT_INFO;
import cartago.AgentId;
import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;
import cartago.OUTPORT;
import cartago.OpFeedbackParam;
import cartago.OperationException;


@ARTIFACT_INFO(
	outports = {
	    @OUTPORT(name = "equipment-management"),
	    @OUTPORT(name = "request-management"),
	    @OUTPORT(name = "schedule-management"),
	    @OUTPORT(name = "monitor-management"),
	    @OUTPORT(name = "simulation-management")
	}
)

public class RoomStateArtifact extends Artifact {
	public static int EVENT_BUFFER_WINDOW 	= 	Constants.HOUR_UNIT / 4;
	public static int EVENT_PAUSE_WINDOW 	= 	Constants.HOUR_UNIT / 10;
	public static int EVENT_UNIT_DURATION 	= 	Constants.HOUR_UNIT;
	
	private Room myRoom;
	private boolean active = true;
	
	private Event currentEvent;
	private List<Event> roomEventList;
	private List<Equipment> roomEquipment;
	
	private HashMap<EventRequirements, List<Equipment>> pendingEquipmentBooking;
	private HashMap<Event, List<Equipment>> bookedEquipmentMap;
	
	private List<EventCount> eventCounterHistory;
	private int requestCounter = 0;
	private int unscheduledRequests = 0;
	
	//private HashMap<SystemAgentId, RoleType> registeredUserAgents;
	private HashMap<String, RoleType> registeredUserAgents;
	private double energyUsage = 0;
	
	private EventComparator eventComparator;
	private HashMap<EventType, RoomType> event2roomTypeMap;
	
	protected void init(Room room) {
		myRoom = room;
		
		eventComparator = new EventComparator();
		event2roomTypeMap = new HashMap<EventType, RoomType>();
		event2roomTypeMap.put(EventType.teachingEvent, RoomType.teachingRoom);
		event2roomTypeMap.put(EventType.meetingEvent, RoomType.meetingRoom);
		event2roomTypeMap.put(EventType.brainstormEvent, RoomType.brainstormRoom);
		
		roomEventList = new ArrayList<Event>();
		roomEquipment = new ArrayList<Equipment>();
		
		pendingEquipmentBooking = new HashMap<EventRequirements, List<Equipment>>();
		bookedEquipmentMap = new HashMap<Event, List<Equipment>>();
		
		//registeredUserAgents = new HashMap<SystemAgentId, RoleType>();
		registeredUserAgents = new HashMap<String, RoleType>();
		eventCounterHistory = new ArrayList<EventCount>();
		
		defineObservableProperties();
		
		// start internal actions
		//execInternalOp("eventCounterUpdate");
	}
	
	@Override
	protected void dispose() {
		active = false;
		System.out.println("Room " + myRoom.getId() + " offline.");
		super.dispose();
	}
	
	private void defineObservableProperties() {
		// property for myRoom
		defineObsProperty("myRoom", myRoom);
		
		// property for currentEvent
		defineObsProperty("currentEvent", currentEvent);
		
		// property for roomEventList
		//defineObsProperty("roomEventList", roomEventList);
		
		// property for event counter
		defineObsProperty("eventCounter", requestCounter);
		
		// property for roomEquipment
		//defineObsProperty("roomEquipment", roomEquipment);
		
		// property for registeredUserAgents
		// defineObsProperty("registeredUserAgents", registeredUserAgents);
		
		// property for energyUsage
		defineObsProperty("energyUsage", energyUsage);
	}
	
	/* ============================= Event Requirements Operations ============================== */
	@OPERATION
	protected void canMeetRequirements(String evReqStr, OpFeedbackParam<Boolean> okMessage) throws ParseException {
		//Literal evReq = ASSyntax.parseLiteral(evReqStr);
		EventRequirements evReq = EventRequirements.parseProlog(evReqStr);
		
		//System.out.println("[ROOM_STATE] Investigating requirements for evReq: " + evReqStr);
		
		//if (evReq.getRoomFormat() != RoomFormat.any && evReq.getRoomFormat() != myRoom.getFormat()) {
		if (!Constants.compatibleRoomFormat(evReq.getRoomFormat(), myRoom.getFormat())) {
			okMessage.set(false);
			//System.out.println("[ROOM_STATE] Investigation failed on room_format: " 
			//		+ evReq.getRoomFormat() + " <> " + myRoom.getFormat());
			return;
		}
		
		
		if (evReq.getNumSeats() > myRoom.getNumSeats()) {
			okMessage.set(false);
			//System.out.println("[ROOM_STATE] Investigation failed on num_seats.");
			return;
		}
		
		// check equipment that we know is not movable
		if (evReq.hasBlackboard()) {
			boolean hasBlackboard = existsEquipment(EquipmentType.blackboard);
			if (!hasBlackboard) {
				okMessage.set(false);
				//System.out.println("[ROOM_STATE] Investigation failed on hasBlackboard.");
				return;
			}
		}
		
		if (evReq.hasTv()) {
			boolean hasTv = existsEquipment(EquipmentType.tv);
			if (!hasTv) {
				okMessage.set(false);
				//System.out.println("[ROOM_STATE] Investigation failed on hasTv.");
				return;
			}
		}
		
		if (evReq.hasMic()) {
			boolean hasMic = existsEquipment(EquipmentType.microphone);
			if (!hasMic) {
				okMessage.set(false);
				//System.out.println("[ROOM_STATE] Investigation failed on hasMic.");
				return;
			}
		}
		
		List<Equipment> bookedEquipment = new ArrayList<Equipment>();
		
		// check equipment that we know is movable
		if (evReq.hasProjector()) {
			boolean hasProjector = existsEquipment(EquipmentType.projector);
			
			if (!hasProjector) {		// i don't have a fixed projector but I will try to book one
				OpFeedbackParam<Equipment> obtainedEq = new OpFeedbackParam<Equipment>();
				try {
					execLinkedOp("equipment-management", "requestEquipment", 
							EquipmentType.projector, evReq, obtainedEq);
					
					Equipment bookedEq = obtainedEq.get();
					if (bookedEq != null) {
						bookedEquipment.add(bookedEq);
						System.out.println("[ROOM_STATE + " + myRoom.getManagingAgentName() + "] Have booked equipment: " + bookedEq);
					}
					else {
						System.out.println("[ROOM_STATE + " + myRoom.getManagingAgentName() + "] Investigation failed on hasProjector.");
						okMessage.set(false);
					}
				} catch (OperationException e) {
					e.printStackTrace();
					System.out.println("[ROOM_STATE] INTERNAL WARNING! Could not request equipment of type " 
							+ EquipmentType.projector + " FOR EVENR REQ " + evReq);
					
					//System.out.println("[ROOM_STATE] Investigation failed on hasProjector.");
					okMessage.set(false);
				}
			}
		}
		
		if (!bookedEquipment.isEmpty()) {
			pendingEquipmentBooking.put(evReq, bookedEquipment);
		}
		
		okMessage.set(true);
	}
	
	private boolean existsEquipment(EquipmentType eqType) {
		for (Equipment eq : roomEquipment) {
			if (eq.getType() == eqType) {
				return true;
			}
		}
		
		return false;
	}
	
	/* ==================================== Event Operations ==================================== */
	
	@OPERATION
	protected void setCurrentEvent(Event event) {
		currentEvent = event;
		
		// update observable property
		getObsProperty("currentEvent").updateValue(currentEvent);
	}
	
	@OPERATION
	protected void nextEvent() {
		synchronized(roomEventList) {
			if (!roomEventList.isEmpty()) {
				Event nextInLine = roomEventList.get(0);
				
				// get currentDateUnit from Simulation artifact
				OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
				OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
				OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
				
				try {
					execLinkedOp("simulation-management", "getDateUnit", dayUnit, hourUnit, minuteUnit);
					Calendar now = DateDifference.calendarFromTimeUnits(dayUnit.get(), hourUnit.get(), minuteUnit.get());
					
					if (DateDifference.getAsMinutes(now, nextInLine.getStart()) > EVENT_PAUSE_WINDOW) {
						// this will ensure that resources (light, heat, equipments) can be released
						
						if (currentEvent != null) {
							System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Next event is null.");
						}
						setCurrentEvent(null);
						
					}
					else {
						// next event is close enough => don't shut things off
						setCurrentEvent(nextInLine);
						//roomEventList.remove(0);		// remove the next event from the waiting list
						
						// update observable property
						//getObsProperty("roomEventList").updateValue(roomEventList);
						System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Next event is: " + nextInLine);
					}
				
				} catch (OperationException e) {
					e.printStackTrace();
					System.out.println("[ROOM_STATE] INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED." +
							"CURRENT EVENT SET TO \"NULL\"");
					setCurrentEvent(null);
				}
			}
			else {
				if (currentEvent != null) {
					System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] No more events in list. Next event is null.");
				}
				setCurrentEvent(null);
			}
		}
	}
	
	@OPERATION
	protected void addEvent(Event event) {
		synchronized(roomEventList) {
			EventType evType = event.getType();
			RoomType newRoomType = event2roomTypeMap.get(evType);
			
			if (newRoomType != myRoom.getType()) {	// this can only happen if the room agent has changed roles
				myRoom.setType(newRoomType);		// and the scheduler sets a new event in this room. The 
			}										// scheduler alg. ensures that this is the only case where it can happen 
			
			event.setEventRoom(myRoom);
			
			//System.out.println("Room: " + myRoom + " -- adding a new event: " + event);
			
			// Search for the non-existent item
			int index = Collections.binarySearch(roomEventList, event, eventComparator);
			
			// if the event is not already in the list index will be < 0
			if (index < 0) {
				roomEventList.add(-index - 1, event);
			}
			
			// look into the pendingEquipment map to see if I need to confirm booking of any equipment
			// now that I have been assigned the room
			// using the EventRequirements equals method
			List<Equipment> bookedEquipment = pendingEquipmentBooking.remove(event.getEventRequirements());
			if (bookedEquipment != null) {
				// confirm booking
				for (Equipment eq : bookedEquipment) {
					try {
						System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Confirming equipment booking of eq: " + eq + " for event " + event);
						execLinkedOp("equipment-management", "bookEquipmentConfirm", eq, event);
					} catch (OperationException e) {
						e.printStackTrace();
						System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] INTERNAL WARNING. CONFIRM BOOK EQUIPMENT FOR EQUIPMENT" + eq + 
								" FOR EVENT " + event + " OPERATION FAILED.");
					}
				}
				
				// add to bookedEquipmentMap
				bookedEquipmentMap.put(event, bookedEquipment);
			}
			else {
				System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] No equipment had to be booked for event: " + event);
			}
			
			// update observable property
			//getObsProperty("roomEventList").updateValue(roomEventList);
		}
	}
	
	@OPERATION
	protected void removeEvent(Event event) {
		synchronized(roomEventList) {
			
			// remove from event list
			roomEventList.remove(event);
			
			// remove any booked equipment - release equipment will have been called before
			bookedEquipmentMap.remove(event);
			
			/*
			// get currentDateUnit from Simulation artifact
			OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
			OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
			OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
			
			try {
				execLinkedOp("simulation-management", "getDateUnit", dayUnit, hourUnit, minuteUnit);
				Calendar now = DateDifference.calendarFromTimeUnits(dayUnit.get(), hourUnit.get(), minuteUnit.get());
				
				// removing an event that got canceled => do not count it for next eventCounter update
				if (event.getStart().after(now)) {
					requestCounter --;
				}
				
			} catch (OperationException e) {
				e.printStackTrace();
				System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
			}
			*/
			
			// update observable property
			//getObsProperty("roomEventList").updateValue(roomEventList);
			
			System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Room removed event: " + event);
		}
	}
	
	@OPERATION
	protected void removeEventsPast(int hour) {
		synchronized(roomEventList) {
			for (Event ev : roomEventList) {
				if (ev.getStart().get(Calendar.HOUR_OF_DAY) >= hour) {
					roomEventList.remove(ev);
				}
			}
			
			// update observable property
			//getObsProperty("roomEventList").updateValue(roomEventList);
		}
	}
	
	@OPERATION
	protected void announceEventFinish(Event event) {
		if (event.equals(currentEvent)) {
			try {
				System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Announcing finish of event: " + event);
				
				// clear registered users list
				registeredUserAgents.clear();
				
				// remove from schedule
				execLinkedOp("schedule-management", "removeScheduledEvent", event);
				
				// clear all user participation intents
				execLinkedOp("request-management", "notifyEventFinish", event);
			}
			catch(OperationException ex) {
				ex.printStackTrace();
			}
		}
		else {
			failed("ERROR. Can only announce finish of the current event. " +
					"Current event: " + currentEvent.toString() + ", given event: " + event.toString());
		}
	}
	
	@OPERATION
	protected void startEventCounting() {
		execInternalOp("eventCounterUpdate");
	}
	
	@OPERATION
	protected void getEventCountInfo(OpFeedbackParam<String> eventCountInfo) {
		// get currentDateUnit from Simulation artifact
		OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
		
		try {
			execLinkedOp("simulation-management", "getDateUnit", dayUnit, hourUnit, minuteUnit);
		} catch (OperationException e) {
			e.printStackTrace();
			failed("INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
		}
		
		if (!eventCounterHistory.isEmpty()) {
			int previousCount = eventCounterHistory.get(eventCounterHistory.size() - 1).getCount();
			for (int index = eventCounterHistory.size() - 1; index >= 0; index --) {
				if (eventCounterHistory.get(index).getDate().get(Calendar.HOUR_OF_DAY) == hourUnit.get() - 1){
					previousCount = eventCounterHistory.get(index).getCount();
					break;
				}
			}
			
			int diff = requestCounter + unscheduledRequests - previousCount;
			int expectedCount = diff;
			
			int remainingUnitSlots = Constants.DAY_END_HOUR - hourUnit.get() - 1;
			for (Event ev : roomEventList) {
				if (ev.getStart().get(Calendar.HOUR_OF_DAY) > hourUnit.get()) {
					if (DateDifference.getAsHours(ev.getStart(), ev.getEnd()) > 1) {
						remainingUnitSlots -= 2;
					}
					else {
						remainingUnitSlots --;
					}
				}
			}
			
			String info = "event_counter_update(" + hourUnit.get() + "," + (requestCounter + unscheduledRequests) + "," +
							diff + "," + expectedCount + "," + remainingUnitSlots + ")";
			System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] requestCounter = " + requestCounter + ", unscheduledRequests = " + unscheduledRequests + "\n");
			eventCountInfo.set(info);
		}
		else {
			int expectedCount = (requestCounter + unscheduledRequests) * 2;
			int remainingUnitSlots = Constants.DAY_END_HOUR - hourUnit.get() - 1;
			
			String info = "event_counter_update(" + hourUnit.get() + "," + (requestCounter + unscheduledRequests) + "," +
							(requestCounter + unscheduledRequests) + "," + expectedCount + "," + remainingUnitSlots + ")";
			
			eventCountInfo.set(info);
		}
	}
	
	@INTERNAL_OPERATION
	protected void eventCounterUpdate() {
		await_time(Constants.HOUR_UNIT * 3000 / 60);
		
		while(active) {
			// get currentDateUnit from Simulation artifact
			OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
			OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
			OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
			
			try {
				execLinkedOp("simulation-management", "getDateUnit", dayUnit, hourUnit, minuteUnit);
			} catch (OperationException e) {
				e.printStackTrace();
				failed("INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
			}
			
			if (hourUnit.get() == Constants.DAY_END_HOUR - 1) {		// counter and history are only kept for one
																// day at room agent level
				requestCounter = 0;								// reset counter on day change
				unscheduledRequests = 0;
				eventCounterHistory.clear();					// clear history on day change
			}
			
			if (!eventCounterHistory.isEmpty()) {
				int previousCount = eventCounterHistory.get(eventCounterHistory.size() - 1).getCount();
				int diff = requestCounter + unscheduledRequests - previousCount;
				//int expectedCount = eventCounter + unscheduledEvents + diff;
				//int expectedCount = 2 * diff;
				int expectedCount = diff;
				
				int remainingUnitSlots = Constants.DAY_END_HOUR - hourUnit.get() - 1;
				for (Event ev : roomEventList) {
					if (ev.getStart().get(Calendar.HOUR_OF_DAY) > hourUnit.get()) {
						if (DateDifference.getAsHours(ev.getStart(), ev.getEnd()) > 1) {
							remainingUnitSlots -= 2;
						}
						else {
							remainingUnitSlots --;
						}
					}
				}
				
				System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] requestCounter = " + requestCounter + ", unscheduledRequests = " + unscheduledRequests + "\n");
				// signal event counter update
				signal("event_counter_update", hourUnit.get(), requestCounter + unscheduledRequests, diff, expectedCount, remainingUnitSlots);
			}
			else {
				int expectedCount = (requestCounter + unscheduledRequests) * 2;
				int remainingUnitSlots = Constants.DAY_END_HOUR - hourUnit.get() - 1;
				
				// signal event counter update
				signal("event_counter_update", hourUnit.get(), requestCounter + unscheduledRequests, 
						requestCounter + unscheduledRequests, expectedCount, remainingUnitSlots);
			}
			
			// mark the new number of registered events
			Calendar now = DateDifference.calendarFromTimeUnits(dayUnit.get(), hourUnit.get(), minuteUnit.get());
			
			EventCount evCount = new EventCount(now, requestCounter + unscheduledRequests);
			if (!eventCounterHistory.contains(evCount)) {
				eventCounterHistory.add(evCount);
			}
			
			
			// pass the hour unit
			await_time(Constants.HOUR_UNIT * 1000);
		}
	}
	
	@OPERATION
	protected void notifyUnscheduledEvent() {
		unscheduledRequests ++;
	}
	
	@OPERATION
	protected void notifyScheduleRequest(Event event) {
		// get currentDateUnit from Simulation artifact
		OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
		
		try {
			execLinkedOp("simulation-management", "getDateUnit", dayUnit, hourUnit, minuteUnit);
			
			if (event.getStart().get(Calendar.DAY_OF_YEAR) == dayUnit.get()) {
				double duration = DateDifference.getAsHours(event.getStart(), event.getEnd());
				if (duration > 1)	{
					requestCounter += (int)duration;
				}
				else {
					requestCounter ++;
				}
				
				getObsProperty("eventCounter").updateValue(requestCounter);
			}
			
		} catch (OperationException e) {
			e.printStackTrace();
			System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
		}
	}
	
	/* ==================================== Equipment Operations ==================================== */
	
	@OPERATION
	protected void addEquipment(Equipment equipment) {
		roomEquipment.add(equipment);
		
		// update observable property
		//getObsProperty("roomEquipment").updateValue(roomEquipment);
	}
	
	@OPERATION void removeEquipment(Equipment equipment) {
		roomEquipment.remove(equipment);
		
		// update observable property
		//getObsProperty("roomEquipment").updateValue(roomEquipment);
	}
	
	@OPERATION
	protected void requestEquipment(EquipmentType eqType, EventRequirements req, OpFeedbackParam<Equipment> obtainedEq) {
		try {
			execLinkedOp("equipment-management", "requestEquipment", eqType, req, obtainedEq);
		} catch (OperationException e) {
			e.printStackTrace();
			failed("INTERNAL ERROR. REQUEST EQUIPMENT OF TYPE" + eqType.name() + " FOR EVENT REQ " + req + " OPERATION FAILED.");
		}
	}
	
	@OPERATION
	protected void bookEquipmentConfirm(Equipment eq, Event event) {
		try {
			execLinkedOp("equipment-management", "bookEquipmentConfirm", eq, event);
		} catch (OperationException e) {
			e.printStackTrace();
			failed("INTERNAL ERROR. CONFIRM BOOK EQUIPMENT FOR EQUIPMENT" + eq + " FOR EVENT " + event + " OPERATION FAILED.");
		}
	}
	
	@OPERATION 
	protected void releaseEquipment(Event event, int dummy) {
		//System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] !!!! Releasing booked equipment for event: " + event);
		
		List<Equipment> bookedEquipment = bookedEquipmentMap.remove(event);
		if (bookedEquipment != null) {
			for (Equipment eq : bookedEquipment) {
				try {
					System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Releasing equipment " + eq + " for event: " + event);
					execLinkedOp("equipment-management", "releaseEquipment", eq, event);
				} catch (OperationException e) {
					e.printStackTrace();
					failed("INTERNAL ERROR. RELEASING EQUIPMENT " + eq.toString() + " FOR FINISHED EVENT " + event.toString() + " OPERATION FAILED.");
				}
			}
		}
	}
	
	@OPERATION 
	protected void releaseEquipment(String evReqStr) throws ParseException {
		System.out.println("[ROOM_STATE " + myRoom.getManagingAgentName() + "] Releasing equipment for evReq: " + evReqStr);
		EventRequirements evReq = EventRequirements.parseProlog(evReqStr);
		List<Equipment> bookedEquipment = pendingEquipmentBooking.remove(evReq);
		if (bookedEquipment != null) {
			for (Equipment eq : bookedEquipment) {
				try {
					execLinkedOp("equipment-management", "releaseEquipment", eq, evReq.getStartDate(), evReq.getEndDate());
				} catch (OperationException e) {
					e.printStackTrace();
					failed("INTERNAL ERROR. RELEASING EQUIPMENT " + eq.toString() + " FOR BOOKED PERIOD " 
							+ evReq.getStartDate() + " - " + evReq.getEndDate() + " OPERATION FAILED.");
				}
			}
		}
	}
	
	/*
		The following two operations will be used by the reorg. designer when moving events from a target room  
		to an adopting room 
	*/
	@OPERATION
	protected void getBookedEquipment(Event event, OpFeedbackParam<int[]> bookedIds) {
		List<Equipment> bookedEquipment = bookedEquipmentMap.get(event);
		if (bookedEquipment != null) {
			int[] booked = new int[bookedEquipment.size()];
			
			for (int i = 0; i < bookedEquipment.size(); i++) {
				booked[i] = bookedEquipment.get(i).getId();
			}
			
			bookedIds.set(booked);
		}
		else {
			int[] booked = new int[0];
			bookedIds.set(booked);
		}
	}
	
	@OPERATION
	protected void setBookedEquipment(Event event, int[] bookedIds) {
		List<Equipment> bookedEquipment = new ArrayList<Equipment>();
		
		for (int i = 0; i < bookedIds.length; i++) {
			try {
				OpFeedbackParam<Equipment> equip = new OpFeedbackParam<Equipment>(); 
				execLinkedOp("equipment-management", "getEquipment", bookedIds[i], equip);
				bookedEquipment.add(equip.get());
			}
			catch(OperationException ex) {
				ex.printStackTrace();
				System.out.println("[ROOM_STATE] INTERNAL ERROR. Set booked equipment action failed. ");
			}
		}
		
		bookedEquipmentMap.put(event, bookedEquipment);
	}
	
	/* ==================================== User agent Operations ==================================== */
	
	@OPERATION
	protected void registeredUserCount(OpFeedbackParam<Integer> userCount) {
		userCount.set(registeredUserAgents.size());
	}
	
	@OPERATION
	protected void registerAs(String userRole, Event event) {
		
		RoleType userRoleType = RoleType.valueOf(userRole);
		
		if (currentEvent != null && event.equals(currentEvent)) {
			AgentId registeringAgId = getOpUserId();
			String agName = registeringAgId.getAgentName();
			
			if (userRoleType == RoleType.presenter && !event.getOwnerUserId().getRequesterName().equals(agName)) {
				failed("ERROR-2. Trying to register as presenter for event you do not own. Current event: " + 
						currentEvent.toString() + " you: " +registeringAgId.getAgentName() + " owner: " + 
						event.getOwnerUserId().getRequesterName());
			}
			else {
				registeredUserAgents.put(agName, userRoleType);
				
				// getObsProperty("registeredUserAgents").updateValue(registeredUserAgents);
				// System.out.println("[ROOM_STATE] User " + agName + " has registered for event: " + currentEvent);
			}
			
			/*
			OpFeedbackParam<SystemAgentId> existingSysAgentData = new OpFeedbackParam<SystemAgentId>();
			try {
				execLinkedOp("request-management", "getSystemAgentId", registeringAgId.getAgentName(), existingSysAgentData);
				if (existingSysAgentData.get() == null) {
					OpFeedbackParam<SystemAgentId> newSysAgentId = new OpFeedbackParam<SystemAgentId>();
					execLinkedOp("request-management", "addSystemAgent", 
							registeringAgId.getAgentName(), 
							registeringAgId.getGlobalId(),
							newSysAgentId);
					
					if (userRoleType == RoleType.presenter) {
						// fail the action because the user that registered the event must be known to the system
						failed("ERROR-1. Trying to register as presenter for event you do not own. Current event: " + 
								currentEvent.toString() + " you: " +registeringAgId.getAgentName() + " owner: " + 
								event.getOwnerUserId().getRequesterName());
					}
					else {
						registeredUserAgents.put(newSysAgentId.get(), userRoleType);
						getObsProperty("registeredUserAgents").updateValue(registeredUserAgents);
						
						System.out.println("[ROOM_STATE] User " + newSysAgentId.get().getRequesterName() 
								+ " has registered for event: " + currentEvent);
					}
				}
				else {
					SystemAgentId existingAgentId = existingSysAgentData.get();
					if (userRoleType == RoleType.presenter && !event.getOwnerUserId().equals(existingAgentId)) {
						failed("ERROR-2. Trying to register as presenter for event you do not own. Current event: " + 
								currentEvent.toString() + " you: " +registeringAgId.getAgentName() + " owner: " + 
								event.getOwnerUserId().getRequesterName());
					}
					else {
						registeredUserAgents.put(existingSysAgentData.get(), userRoleType);
						getObsProperty("registeredUserAgents").updateValue(registeredUserAgents);
						
						System.out.println("[ROOM_STATE] User " + existingAgentId.getRequesterName() 
								+ " has registered for event: " + currentEvent);
					}
				}
			} catch (OperationException e) {
				e.printStackTrace();
			}
			*/
			
			
		}
		else {
			if (currentEvent != null) {
				failed("ERROR. Trying to register for the wrong event. Current event: " + currentEvent.toString()
						+ "<> requested event: " + event.toString());
			}
			else {
				failed("ERROR. Trying to register when no current event exists.");
			}
		}
	}
	
	@OPERATION
	protected void deregister() {
		
		AgentId deregisteringAgId = getOpUserId();
		/*
		OpFeedbackParam<SystemAgentId> existingSysAgentData = new OpFeedbackParam<SystemAgentId>();
		
		try {
			execLinkedOp("request-management", "getSystemAgentId", deregisteringAgId.getGlobalId(), existingSysAgentData);
			if (existingSysAgentData.get() != null) {
				registeredUserAgents.remove(existingSysAgentData.get());
				getObsProperty("registeredUserAgents").updateValue(registeredUserAgents);
			}
		} catch (OperationException e) {
			e.printStackTrace();
			failed("Agent with global id name: " + deregisteringAgId.getGlobalId() + " was never registered in this room.");
		}
		*/
		
		registeredUserAgents.remove(deregisteringAgId.getAgentName());
		//getObsProperty("registeredUserAgents").updateValue(registeredUserAgents);
	}
	
	/* ==================================== Energy Operations ==================================== */
	
	@OPERATION
	protected void announceEnergyEstimate() {
		
		if (currentEvent != null) {			// if I have a current running event
			double amount = 0;
			double timeEstimate = DateDifference.getAsHours(Calendar.getInstance(), currentEvent.getEnd());
			
			// add equipment values
			for (Equipment eq : roomEquipment) {
				Integer eqEnergyConsumption = Constants.equipmentEnergyFactors.get(eq.getType());
				if (eqEnergyConsumption != null) {
					amount += eqEnergyConsumption * timeEstimate;
				}
			}
			
			// add utilities values
			amount += Constants.lightUtility2roomTypeMap.get(myRoom.getFormat()) * 
						Constants.equipmentEnergyFactors.get(EquipmentType.light) * timeEstimate;
			
			amount += Constants.heatUtility2roomTypeMap.get(myRoom.getFormat()) * 
				Constants.equipmentEnergyFactors.get(EquipmentType.heating) * timeEstimate;
			
			try {
				execLinkedOp("monitor-management", "logEnergyUsage", myRoom, amount);
			}
			catch(OperationException ex) {
				failed("INTERNAL ERROR. Could not announce energy estimate to monitoring artifact.");
				ex.printStackTrace();
			}
		}
	}
	
	@OPERATION
	protected void announceEnergyEstimate(HashMap<EquipmentType, Integer> usedEquipment) {
		
		if (currentEvent != null) {			// if I have a current running event
			double amount = 0;
			double timeEstimate = DateDifference.getAsHours(Calendar.getInstance(), currentEvent.getEnd());
			
			for (EquipmentType eqType : usedEquipment.keySet()) {
				amount += usedEquipment.get(eqType) * Constants.equipmentEnergyFactors.get(eqType) * timeEstimate;
			}
			
			try {
				execLinkedOp("monitor-management", "logEnergyUsage", myRoom, amount);
			}
			catch(OperationException ex) {
				failed("INTERNAL ERROR. Could not announce energy estimate to monitoring artifact.");
				ex.printStackTrace();
			}
		}
	}
	
	/* ==================================== Auxiliary Operations ==================================== */
	
	@OPERATION
	protected void getRoom(OpFeedbackParam<Room> room) {
		room.set(myRoom);
	}
	
	
	private class EventCount {
		private Calendar date;
		private int count;
		
		public EventCount(Calendar date, int count) {
			this.date = date;
			this.count = count;
		}

		public Calendar getDate() {
			return date;
		}

		public int getCount() {
			return count;
		}
		
		@Override
		public int hashCode() {
			return date.get(Calendar.MINUTE) + date.get(Calendar.HOUR_OF_DAY) * 60 + 
					date.get(Calendar.DAY_OF_YEAR) * 24 * 60;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			
			if ( !(obj instanceof EventCount) ) {
				return false;
			}
			
			EventCount other = (EventCount)obj;
			
			if (date.get(Calendar.MINUTE) != other.getDate().get(Calendar.MINUTE)) return false;
			if (date.get(Calendar.HOUR_OF_DAY) != other.getDate().get(Calendar.HOUR_OF_DAY)) return false;
			if (date.get(Calendar.DAY_OF_YEAR) != other.getDate().get(Calendar.DAY_OF_YEAR)) return false;
			
			return true;
		}
	}
}
