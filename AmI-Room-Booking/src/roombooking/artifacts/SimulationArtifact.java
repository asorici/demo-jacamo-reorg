package roombooking.artifacts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import ora4mas.nopl.JasonTermWrapper;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import roombooking.core.Room.RoomFormat;
import roombooking.core.Room.RoomType;
import roombooking.core.event.Event;
import roombooking.core.event.Event.EventType;
import roombooking.core.event.EventRequirements;
import roombooking.gui.SimulationGui;
import roombooking.util.DateDifference;
import roombooking.util.RoomData;
import roombooking.util.SimulationParser;
import roombooking.util.SimulationParser.RequestSpreadType;
import cartago.Artifact;
import cartago.GUARD;
import cartago.INTERNAL_OPERATION;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class SimulationArtifact extends Artifact {
	private int currentDayUnit = 1;
	private int currentHourUnit = Constants.DAY_START_HOUR;
	private int currentMinuteUnit = 0;
	
	private int dayCount = 0; 
	private boolean simRunning = true;
	private boolean simPaused = false;
	private List<String> initCompleteMarkers = new ArrayList<String>();
	
	private Random myRand = new Random();
	private int generatedEventCounter = 1;
	
	//private int participationAgentIndex = 0;
	private int managementStructureCount = 1;
	
	//private HashMap<Calendar, List<EventRequirements>> dateRequestMap = new HashMap<Calendar, List<EventRequirements>>();
	//private HashMap<String, List<EventRequirements>> agentRequestMap = new HashMap<String, List<EventRequirements>>();
	private List<HashMap<Calendar, List<EventRequirements>>> dateRequestMapList = new ArrayList<HashMap<Calendar,List<EventRequirements>>>();
	private List<HashMap<String, List<EventRequirements>>> agentRequestMapList = new ArrayList<HashMap<String,List<EventRequirements>>>();
	
	private HashMap<EventType, List<Integer>> interDaySpreadMap = new HashMap<EventType, List<Integer>>();
	
	private List<HashMap<EventType, List<Integer>>> actionSpreadMapList = new ArrayList<HashMap<EventType,List<Integer>>>();
	private List<Event> scheduledEventsList = new ArrayList<Event>();
	
	private SimulationGui simChartGui;
	private HashMap<EventType, Integer> allRequestData = new HashMap<EventType, Integer>();
	private HashMap<EventType, Integer> deniedRequestData = new HashMap<EventType, Integer>();
	private HashMap<RoomType, Integer> roomTypeData = new HashMap<RoomType, Integer>();
	
	// logging
	private static Logger logger = Logger.getLogger(SimulationArtifact.class);
	
	protected void init(String simConfigurationFile) {
		SimulationParser.parse(simConfigurationFile);
		
		for (EventType evType : EventType.values()) {
			managementStructureCount += SimulationParser.managementStructure.get(evType);
		}
		
		// set inter-day request spread map
		setInterDaySpreadMap();
		
		// construct simulation actions
		constructSimulationEvents();
		constructSimulationActions();
		printSimActions();
		
		// define observable properties
		defineObsProperties();
		
		// configure logger
		PropertyConfigurator.configure("AmI-Room-Booking/logging.properties");
		
		// start looping internal operations
		execInternalOp("simulationStart");
	}
	
	private void defineObsProperties() {
		// define time unit
		defineObsProperty("currentDateUnit", currentDayUnit, currentHourUnit, currentMinuteUnit);
		
		// define sim paused
		defineObsProperty("simPaused", simPaused);
		
		// define room data
		for (RoomFormat roomFormat : RoomFormat.values()) {
			defineObsProperty("room_format", new JasonTermWrapper(roomFormat.name()));
		}
		
		
		for (Integer roomId : SimulationParser.roomData.keySet()) {
			RoomData rd = SimulationParser.roomData.get(roomId);
			defineObsProperty("room_spec", 
					rd.getId(), new JasonTermWrapper(rd.getFormat().name()), rd.getNumSeats(), rd.getEquipmentIds().toArray());
		}
		
		// define initial assignment
		for (String agName : SimulationParser.initialAssignments.keySet()) {
			String specializedGroup = (String)SimulationParser.initialAssignments.get(agName).get("group");
			int roomId = (Integer)SimulationParser.initialAssignments.get(agName).get("roomId");
			
			defineObsProperty("initial_assignment", 
					new JasonTermWrapper(agName), new JasonTermWrapper(specializedGroup), roomId);
		}
	}
	
	// ====================================================================================================== //
	// =================================== Simulation parameter settings ==================================== //
	// ====================================================================================================== //
	
	private void printSimActions() {
		System.out.println("#### Requests by date ####");
		for ( int i = 0; i < dateRequestMapList.size(); i++) {
			HashMap<Calendar, List<EventRequirements>> dateRequestMap = dateRequestMapList.get(i);
			System.out.println("## Day: " + (i + 1));
			
			for (Calendar c : dateRequestMap.keySet()) {
				System.out.print(c.get(Calendar.HOUR_OF_DAY) + ": ");
				
				for (EventRequirements evReq : dateRequestMap.get(c)) {
					System.out.print(evReq.getTitle() + " ");
				}
				
				System.out.println();
			}
		
		}
		
		System.out.println("#### Requests by agent ####");
		for ( int i = 0; i < agentRequestMapList.size(); i++) {
			HashMap<String, List<EventRequirements>> agentRequestMap = agentRequestMapList.get(i);
			System.out.println("## Day: " + (i + 1));
			
			for (String proposerName : agentRequestMap.keySet()) {
				System.out.print(proposerName + ": ");
				
				for (EventRequirements evReq : agentRequestMap.get(proposerName)) {
					System.out.print(evReq.getTitle() + "[" + evReq.getStartDate().get(Calendar.HOUR_OF_DAY) + "] ");
				}
				
				System.out.println();
			}
		}
	}

	private void setInterDaySpreadMap() {
		for (EventType evType: EventType.values()) {
			int totalEvents = SimulationParser.requestStructure.get(evType);
			int days = SimulationParser.simDayDuration;
			
			RequestSpreadType spreadType = SimulationParser.requestInterDaySpread.get(evType);
			List<Integer> spread = new ArrayList<Integer>();
			
			switch(spreadType) {
				case uniform:
				{
					int split = totalEvents / days;
					int s = 0;
					
					for (int i = 0; i < days; i++) {
						if (i != days - 1) {
							s += split;
							spread.add(split);
						}
						else {
							spread.add(totalEvents - s);
						}
					}
					
					break;
				}	
				case arithmeticIncrease:
				case arithmeticDecrease:
				{
					int step = totalEvents / (days * (days + 1) / 2);
					int s = 0;
					for (int i = 0; i < days; i++) {
						if (i != days - 1) {
							int part = (i + 1) * step; 
							s += part;
							spread.add(part);
						}
						else {
							int part = totalEvents - s;
							spread.add(part);
						}
					}
					
					if (spreadType == RequestSpreadType.arithmeticDecrease) {
						Collections.reverse(spread);
					}
					
					break;
				}	
				
				default:
					break;
			}
			
			interDaySpreadMap.put(evType, spread);
		}
	}
	
	private void constructSimulationActions() {
		for (int day = 0; day < SimulationParser.simDayDuration; day++) {
			HashMap<EventType, List<Integer>> intraDaySpreadMap = new HashMap<EventType, List<Integer>>();
			
			for (EventType evType : EventType.values()) {
				List<Integer> interDaySpread = interDaySpreadMap.get(evType);
				int daySpread = interDaySpread.get(day);
				
				List<Integer> spreadList = new ArrayList<Integer>();
				
				RequestSpreadType spreadType = SimulationParser.requestIntraDaySpread.get(evType);
				switch(spreadType) {
					case uniform:
					{
						int quota = daySpread / 6;
						int s = 0;
						
						for (int k = 0; k < 6; k++) {
							if (k != 5) {
								s += quota;
								spreadList.add(quota);
							}
							else {
								spreadList.add(daySpread - s);
							}
						}
						
						break;
					}
					
					case arithmeticIncrease:
					case arithmeticDecrease:
					{	
						int step = (int) Math.ceil( (double)daySpread / 21);
						int quota = step, s = 0;
						
						for (int k = 0; k < 6; k++) {
							if (k != 5) {
								if (s + quota < daySpread) {
									s += quota;
									spreadList.add(quota);
									quota += step;
								}
								else {
									int remaining = daySpread - s;
									quota = remaining / (6 - k);
									step = 0;
									k--;
								}
							}
							else {
								spreadList.add(daySpread - s);
							}
						}
						
						if (spreadType == RequestSpreadType.arithmeticDecrease) {
							Collections.reverse(spreadList);
						}
						
						break;
					}
					
					default:
						break;
				}
				
				intraDaySpreadMap.put(evType, spreadList);
			}
			
			// just printing
			System.out.println("[SIMULATION] Spreads for day: " + (day + 1));
			for (EventType evType : EventType.values()) {
				System.out.print(evType + ": ");
				List<Integer> spread = intraDaySpreadMap.get(evType);
				System.out.println(spread);
			}
			
			actionSpreadMapList.add(intraDaySpreadMap);
		}
	}
	
	private void constructSimulationEvents() {
		
		// first construct mapping of dates to requests
		for (int day = 0; day < SimulationParser.simDayDuration; day++) {
		
			HashMap<Calendar, List<EventRequirements>> dateRequestMap = new HashMap<Calendar, List<EventRequirements>>();
			
			for (EventType evType : EventType.values()) {
				//int nrEvents = SimulationParser.requestStructure.get(evType);
				int nrEvents = interDaySpreadMap.get(evType).get(day);
				
				// fill available hour slot array
				List<Integer> availableHourSlots = new ArrayList<Integer>();
				for (int hour = Constants.DAY_START_HOUR + 1; hour < Constants.DAY_END_HOUR; hour++) {
					availableHourSlots.add(hour);
				}
				
				for (int k = 0; k < nrEvents; k++) {
					// first check if any the dataRequestMap entries has reached the "limit" for the
					// number of concurrent events (by type)
					//determineRemainingHourSlots(dateRequestMap, evType, availableHourSlots);
					
					// reset availableHourSlots if empty
					if (availableHourSlots.isEmpty()) {
						for (int hour = Constants.DAY_START_HOUR + 1; hour < Constants.DAY_END_HOUR; hour++) {
							availableHourSlots.add(hour);
						}
					}
					
					// choose an hour from uniform distribution
					int index = myRand.nextInt(availableHourSlots.size());
					int selectedHourSlot = availableHourSlots.remove(index);
					
					// build a new event requirement entry
					EventRequirements evReq = buildEventRequirement(evType, day + 1, selectedHourSlot);
					List<EventRequirements> evReqList = dateRequestMap.get(evReq.getStartDate());
					
					if (evReqList != null) {
						evReqList.add(evReq);
					}
					else {
						evReqList = new ArrayList<EventRequirements>();
						evReqList.add(evReq);
						dateRequestMap.put(evReq.getStartDate(), evReqList);
					}
				}
			}
			
			
			// then use it to map each request to an agent
			// since we are ensured that there are more proposer agents than total possible concurrent events
			// we will just loop through the agents and assign the events
			
			HashMap<String, List<EventRequirements>> agentRequestMap = new HashMap<String, List<EventRequirements>>();
			
			int agentIndex = 0;
			for (List<EventRequirements> evReqList : dateRequestMap.values()) {
				for (EventRequirements evReq : evReqList) {
					String proposerName = SimulationParser.proposerAgentNames.get(agentIndex);
					List<EventRequirements> proposeList = agentRequestMap.get(proposerName);
					
					if (proposeList != null) {
						proposeList.add(evReq);
					}
					else {
						proposeList = new ArrayList<EventRequirements>();
						proposeList.add(evReq);
						agentRequestMap.put(proposerName, proposeList);
					}
				
					agentIndex = (agentIndex + 1) % SimulationParser.proposerAgentNames.size();
				}
			}
		
			// at the end add the date- and name-based hashmaps to the list containing the mappings by day
			dateRequestMapList.add(dateRequestMap);
			agentRequestMapList.add(agentRequestMap);
		}
	}
	
	
	private void determineRemainingHourSlots(HashMap<Calendar, List<EventRequirements>> dateRequestMap, EventType evType, List<Integer> availableHourSlots) {
		for (Calendar c : dateRequestMap.keySet()) {
			int hour = c.get(Calendar.HOUR_OF_DAY);
			
			int nrEvOfType = 0;
			for (EventRequirements evReq : dateRequestMap.get(c)) {
				if (evReq.getType() == evType) {
					nrEvOfType ++;
				}
			}
			
			// get number of initial management agents for event type evType
			int nrManagers = SimulationParser.managementStructure.get(evType);
			if (nrManagers > 1 && nrEvOfType == nrManagers - 1) {
				availableHourSlots.remove(new Integer(hour));
			}
			//else if (nrManagers == 1 && nrEvOfType == 2) {
			//	availableHourSlots.remove(new Integer(hour));
			//}
		}
	}
	
	
	private EventRequirements buildEventRequirement(EventType evType, int yearDay, int selectedHourSlot) {
		// set startDate
		Calendar startDate = Calendar.getInstance();
		startDate.set(Calendar.DAY_OF_YEAR, yearDay);
		startDate.set(Calendar.HOUR_OF_DAY, selectedHourSlot);
		startDate.set(Calendar.MINUTE, 0);
		startDate.set(Calendar.SECOND, 0);
		startDate.set(Calendar.MILLISECOND, 0);
		
		// set endDate
		Calendar endDate = Calendar.getInstance();
		endDate.set(Calendar.DAY_OF_YEAR, yearDay);
		endDate.set(Calendar.HOUR_OF_DAY, selectedHourSlot + 1);	// in this version all events last one hour
		endDate.set(Calendar.MINUTE, 0);
		endDate.set(Calendar.SECOND, 0);
		endDate.set(Calendar.MILLISECOND, 0);
		
		switch(evType) {
			case teachingEvent:
			{
				// set title
				String eventTitle = "TeachingEvent" + generatedEventCounter;
				generatedEventCounter++;
				
				// set number of seats
				int numSeats = 12;
				EventRequirements evReq = new EventRequirements(eventTitle, evType.name(), numSeats, startDate, endDate);
				
				// choose between amphi and lab
				boolean isLab = myRand.nextBoolean();
				if (isLab) {
					evReq.setRoomFormat(RoomFormat.lab);
				}
				else {
					evReq.setRoomFormat(RoomFormat.amphi);
					evReq.setNumSeats(60);
				}
				
				return evReq;
			}
			
			case meetingEvent:
			{
				// set title
				String eventTitle = "MeetingEvent" + generatedEventCounter;
				generatedEventCounter++;
				
				// set number of seats
				int numSeats = 10;
				EventRequirements evReq = new EventRequirements(eventTitle, evType.name(), numSeats, startDate, endDate);
				evReq.setRoomFormat(RoomFormat.any);
				evReq.setProjector(true);
								
				return evReq;
			}
			
			case brainstormEvent:
			{
				// set title
				String eventTitle = "BrainstormEvent" + generatedEventCounter;
				generatedEventCounter++;
				
				// set number of seats
				int numSeats = 6;
				EventRequirements evReq = new EventRequirements(eventTitle, evType.name(), numSeats, startDate, endDate);
				evReq.setRoomFormat(RoomFormat.office);
								
				return evReq;
			}
			
			default:
				return null;	// it should (and will) never get here
		}
	}
	
	
	// ====================================================================================================== //
	// ======================================= simulation management ======================================== //
	// ====================================================================================================== //
	
	
	private void startSim() {
		// display chart GUI - possibly control execution from there, otherwise run actions below
		simChartGui = new SimulationGui();
		
		// init chart data
		for (EventType evType : EventType.values()) {
			allRequestData.put(evType, 0);
			deniedRequestData.put(evType, 0);
		}
		
		roomTypeData.put(RoomType.teachingRoom, SimulationParser.managementStructure.get(EventType.teachingEvent));
		roomTypeData.put(RoomType.meetingRoom, SimulationParser.managementStructure.get(EventType.meetingEvent));
		roomTypeData.put(RoomType.brainstormRoom, SimulationParser.managementStructure.get(EventType.brainstormEvent));
		
		simChartGui.updateAllRequests(currentDayUnit, currentHourUnit, allRequestData);
		simChartGui.updateDeniedRequests(currentDayUnit, currentHourUnit, deniedRequestData);
		simChartGui.updateRoomTypes(currentDayUnit, currentHourUnit, roomTypeData);
		
		// launch internal op to signal all user agents what to do; start time keeper
		execInternalOp("runSim");
	}
		
	@OPERATION
	protected void markInitComplete() {
		String agentName = getOpUserName();
		initCompleteMarkers.add(agentName);
	}
	
	@GUARD
	protected boolean orgInitComplete() {
		System.out.println("[SIMULATION] orgAgentName: " + SimulationParser.orgAgentNames);
		System.out.println("[SIMULATION] complete markers: " + initCompleteMarkers);
		
		for (String agentName : SimulationParser.orgAgentNames) {
			if (!initCompleteMarkers.contains(agentName)) {
				return false; 
			}
		}
		
		return true;
		
	}
	
	@INTERNAL_OPERATION
	protected void simulationStart() {
		await("orgInitComplete");
		
		System.out.println("[SIMULATION] Sending sim_init");
		//signal("sim_init");
		defineObsProperty("sim_init");
		startSim();
	}
	
	@LINK
	protected void getDateUnit(OpFeedbackParam<Integer> dayUnit, OpFeedbackParam<Integer> hourUnit, 
			OpFeedbackParam<Integer> minuteUnit) {
		dayUnit.set(currentDayUnit);
		hourUnit.set(currentHourUnit);
		minuteUnit.set(currentMinuteUnit);
	}
	
	@OPERATION
	protected void timeDiffAsMinutes(int startHour, int startMinute, int endHour, int endMinute, OpFeedbackParam<Integer> minuteDiff) {
		Calendar start = Calendar.getInstance();
		start.set(Calendar.HOUR_OF_DAY, startHour);
		start.set(Calendar.MINUTE, startMinute);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		
		Calendar end = Calendar.getInstance();
		end.set(Calendar.HOUR_OF_DAY, endHour);
		end.set(Calendar.MINUTE, endMinute);
		end.set(Calendar.SECOND, 0);
		end.set(Calendar.MILLISECOND, 0);
		
		int diff = (int)DateDifference.getAsMinutes(start, end);
		minuteDiff.set(diff);
	}
	
	@OPERATION
	protected void addScheduledEvent(Event event) {
		scheduledEventsList.add(event);
	}
	
	@OPERATION
	protected void simulationPaused(boolean paused) {
		simPaused = paused;
		getObsProperty("simPaused").updateValue(simPaused);
		
		if (simPaused) {
			System.out.println("[SIMULATION] =========== Simulation has been paused ========== ");
		}
		else {
			System.out.println("[SIMULATION] =========== Simulation has resumed ========== ");
		}
	}
	
	@OPERATION
	protected void updateAllRequestData(String eventType, int data) {
		EventType evType = EventType.valueOf(eventType);
		Integer reqCount = allRequestData.get(evType);
		
		reqCount += data;
		allRequestData.put(evType, reqCount);
		
		simChartGui.updateAllRequests(currentDayUnit, currentHourUnit, allRequestData);
	}
	
	@OPERATION
	protected void updateDeniedRequestData(String eventType, int data) {
		EventType evType = EventType.valueOf(eventType);
		Integer reqCount = deniedRequestData.get(evType);
		
		reqCount += data;
		deniedRequestData.put(evType, reqCount);
		simChartGui.updateDeniedRequests(currentDayUnit, currentHourUnit, deniedRequestData);
	}
	
	@OPERATION
	protected void updateRoomTypeData(String roomType, int data) {
		RoomType rType = RoomType.valueOf(roomType);
		Integer roomCount = roomTypeData.get(rType);
		
		roomCount += data;
		roomTypeData.put(rType, roomCount);
	}
	
	@OPERATION
	protected void logMessage(Object ... messages) {
		
		String agentName = getOpUserName();
		String msg = "[ " + currentDayUnit + "-" + currentHourUnit + "-"+ currentMinuteUnit + "][" + agentName + "] ";
		for (int i = 0; i < messages.length; i++) {
			msg += messages[i];
		}
			
		// log message to gui
		simChartGui.logMessage(msg);
			
		// log message to output log
		logger.info(msg);
		
	}
	
	@OPERATION
	protected void logKeyMessage(Object ... messages) {
		
		String agentName = getOpUserName();
		String msg = "[ " + currentDayUnit + "-" + currentHourUnit + "-"+ currentMinuteUnit + "][" + agentName + "] ";
		for (int i = 0; i < messages.length; i++) {
			msg += messages[i];
		}
			
		// log message to gui
		simChartGui.logKeyMessage(msg);
			
		// log message to output log
		logger.info(msg);
	}
	
	// ====================================================================================================== //
	// ====================================== Running the simulation ======================================== //
	// ====================================================================================================== //
	
	@GUARD
	protected boolean simulationUnpaused() {
		return !simPaused;
	}
	
	@INTERNAL_OPERATION
	protected void runSim() {
		while (simRunning) {
			
			if (!simPaused) {			
				// ================ do simulation actions ============== //
				// event requests will be sent out during the first 6 hours of the morning - 8, 9, 10, 11, 12, 13
				if (currentMinuteUnit == 0) {
					int hoursSinceStart = currentHourUnit - Constants.DAY_START_HOUR;
					if (hoursSinceStart < 6) {
						sendoutEventRequests(currentDayUnit - 1, hoursSinceStart);
					}
					
					// update graphics every hour
					simChartGui.updateAllRequests(currentDayUnit, currentHourUnit, allRequestData);
					simChartGui.updateDeniedRequests(currentDayUnit, currentHourUnit, deniedRequestData);
					simChartGui.updateRoomTypes(currentDayUnit, currentHourUnit, roomTypeData);
					simChartGui.refreshGui();
					
				}
				
				// send participant agents to events that are about to begin
				sendoutParticipationDemand();
				
				// =============== update simulation time ============== //
				// minute increase
				currentMinuteUnit++;
				
				// increase hour and reset minutes if necessary
				if (currentMinuteUnit == 60) {
					currentHourUnit++;
					currentMinuteUnit = 0;
					System.out.println("[SIMULATION] ========================================= DAY: " + currentDayUnit + ", HOUR : " 
							+ currentHourUnit +
							" ========================================= ");
				}
				
				// increase day and reset hour if necessary
				if (currentHourUnit == Constants.DAY_END_HOUR) {
					currentDayUnit++;
					dayCount++;
					currentHourUnit = Constants.DAY_START_HOUR;
					
					// reset scheduled events list
					scheduledEventsList.clear();
					
					// reset chart gui
					clearRequestData();
					
				}
				
				if (currentDayUnit > Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_YEAR)) {
					// reset day count
					currentDayUnit = 1;
				}
				
				getObsProperty("currentDateUnit").updateValues(currentDayUnit, currentHourUnit, currentMinuteUnit);
				
				// ================= check sim ending ================== //
				if (dayCount == SimulationParser.simDayDuration) {
					simRunning = false;
					break;
				}
			
			
				await_time(Constants.HOUR_UNIT * 1000 / 60);
			}
			else {
				await("simulationUnpaused");
			}
		}
	}
	
	
	private void clearRequestData() {
		allRequestData.clear();
		deniedRequestData.clear();
		
		for (EventType evType : EventType.values()) {
			allRequestData.put(evType, 0);
			deniedRequestData.put(evType, 0);
		}
		
		simChartGui.clearRequestCharts();
	}

	private void sendoutEventRequests(int dayIndex, int intervalIndex) {
		HashMap<Calendar, List<EventRequirements>> dateRequestMap = dateRequestMapList.get(dayIndex);
		
		List<Calendar> emptyRequestDates = new ArrayList<Calendar>();
		List<EventRequirements> evTypeReqList = new ArrayList<EventRequirements>();
		
		for (EventType evType : EventType.values()) {
			int nrToSend = actionSpreadMapList.get(dayIndex).get(evType).get(intervalIndex);
			int ct = 0;
			
			// first add all requests that are scheduled to begin next hour
			int nextHour = currentHourUnit + 1;
			
			for (Calendar c : dateRequestMap.keySet()) {
				if (c.get(Calendar.HOUR_OF_DAY) == nextHour) {
					List<EventRequirements> evReqList = dateRequestMap.get(c);
					
					for (int i = 0; i < evReqList.size(); i++) {
						EventRequirements requirement = evReqList.get(i);
						if (requirement.getType() == evType) {
							evTypeReqList.add(requirement);
							ct++;
							
							evReqList.remove(i);
							i--;
						}
					}
					
					if (evReqList.isEmpty()) {
						emptyRequestDates.add(c);
					}
				}
			}
			
			// now get the list of all the hours past the nextHour that have an event of type evType
			List<Calendar> possibleDates = new ArrayList<Calendar>();
			for (Calendar c : dateRequestMap.keySet()) {
				if (c.get(Calendar.HOUR_OF_DAY) > nextHour) {
					List<EventRequirements> evReqList = dateRequestMap.get(c);
					
					for (int i = 0; i < evReqList.size(); i++) {
						EventRequirements requirement = evReqList.get(i);
						if (requirement.getType() == evType) {
							possibleDates.add(c);
							break;
						}
					}
				}
			}
			
			
			// now choose randomly an event from the possible time slots
			for (int k = ct; k < nrToSend; k++) {
				if (!possibleDates.isEmpty()) {
					int randIndex = myRand.nextInt(possibleDates.size());
					Calendar c = possibleDates.remove(randIndex);
					
					List<EventRequirements> evReqList = dateRequestMap.get(c);
					
					for (int i = 0; i < evReqList.size(); i++) {
						EventRequirements requirement = evReqList.get(i);
						if (requirement.getType() == evType) {
							evTypeReqList.add(requirement);
							evReqList.remove(i);
							break;
						}
					}
					
					if (evReqList.isEmpty()) {
						emptyRequestDates.add(c);
					}
				}
			}
		}
		
		for (Calendar c : emptyRequestDates) {
			dateRequestMap.remove(c);
		}
		
		execInternalOp("sendRequests", dayIndex, evTypeReqList);
	}
	
	@INTERNAL_OPERATION
	protected void sendRequests(int dayIndex, List<EventRequirements> evReqList) {
		HashMap<String, List<EventRequirements>> agentRequestMap = agentRequestMapList.get(dayIndex);
			
		for (int i = 0; i < evReqList.size(); i++) {
			EventRequirements evReq = evReqList.get(i);
			
			// search evReq in proposer agent map
			for (String agName : agentRequestMap.keySet()) {
				if (agentRequestMap.get(agName).contains(evReq)) {
					// send out signal to that agent
					// System.out.println("[SIMULATION] Sending out request demand for evReq: "
					// + evReq + " to agent " + agName);
					
					if (simPaused) {
						await("simulationUnpaused");
					}
					
					signal("sendRequest", agName, evReq);
					break;
				}
			}
			
			await_time(Constants.HOUR_UNIT * 10000 / 60);
		}
	}
	
	
	private void sendoutParticipationDemand() {
		int numParticipants = SimulationParser.participantAgentNames.size();
		// int participantsLot = numParticipants / managementStructureCount;
		
		for (int i = 0; i < scheduledEventsList.size(); i++) {
			Event ev = scheduledEventsList.get(i);
			
			Calendar now = DateDifference.calendarFromTimeUnits(currentDayUnit, currentHourUnit, currentMinuteUnit);
			int minuteDiff = (int)DateDifference.getAsMinutes(ev.getStart(), now); 
			
			if (minuteDiff > 1 && minuteDiff < 4) {
				//System.out.println("[SIMULATION] Sending out participation demands.");
				
				// signal all required participant agents to register for attend event
				//for (int k = 0; k < participantsLot; k++) {
				for (int k = 0; k < numParticipants; k++) {
					//String participantAgName = SimulationParser.participantAgentNames.get(participationAgentIndex);
					String participantAgName = SimulationParser.participantAgentNames.get(k);
					signal("joinEvent", participantAgName, ev, ev.getEventRoom().getManagingAgentName());
					
					//participationAgentIndex = (participationAgentIndex + 1) % numParticipants;
				}
				
				// signal the event owner to do the same
				signal("joinEvent", ev.getOwnerUserId().getRequesterName(), ev, ev.getEventRoom().getManagingAgentName());
				
				// remove event from scheduled events list
				scheduledEventsList.remove(i);
				i--;
			}
		}
		
	}
}
