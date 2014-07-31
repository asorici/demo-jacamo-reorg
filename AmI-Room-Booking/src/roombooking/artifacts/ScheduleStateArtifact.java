package roombooking.artifacts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import roombooking.core.EventList;
import roombooking.core.Room;
import roombooking.core.Room.RoomType;
import roombooking.core.event.Event;
import roombooking.core.event.Event.EventType;
import roombooking.core.event.EventRequirements;
import roombooking.core.request.Request;
import cartago.Artifact;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class ScheduleStateArtifact extends Artifact {
	private static int eventIdCounter = 0;
	
	private HashMap<Room, EventList> eventsByRoom;
	
	protected void init() {
		eventsByRoom = new HashMap<Room, EventList>();
	}
	
	@Override
	protected void dispose() {
		System.out.println("Disposing ScheduleStateArtifact");
		super.dispose();
	}
	
	@OPERATION
	protected void initRoomSchedule(Room room) {
		eventsByRoom.put(room, new EventList());
	}
	
	
	@OPERATION
	protected void createEvent(Request request, EventRequirements eventRequirements, 
			Room eventRoom, OpFeedbackParam<Object> createdEvent) {
		int eventId = eventIdCounter++;
		
		Event newEvent = new Event(request.getRequesterAgentId(), eventId, eventRoom, eventRequirements);
		EventList list = eventsByRoom.get(eventRoom);
		
		//System.out.println("Created event: " + newEvent.toString());
		
		if (list != null) {
			list.addEvent(newEvent);
		}
		else {
			list = new EventList();
			list.addEvent(newEvent);
			eventsByRoom.put(eventRoom, list);
		}
		
		createdEvent.set(newEvent);
	}
	
	@OPERATION @LINK
	protected void setScheduledEvent(Event event) {
		Room eventRoom = event.getEventRoom();
		EventList list = eventsByRoom.get(eventRoom);
		
		if (list != null) {
			list.addEvent(event);
		}
		else {
			list = new EventList();
			list.addEvent(event);
			eventsByRoom.put(eventRoom, list);
		}
	}
	
	@OPERATION	@LINK
	protected void removeScheduledEvent(Event event) {
		Room eventRoom = event.getEventRoom();
		EventList list = eventsByRoom.get(eventRoom);
		
		if (list != null) {
			list.removeEvent(event);
			System.out.println("[SCHEDULE_STATE] Removed event " + event);
		}
		else {
			failed("room_has_no_event_list");
		}
	}
	
	@OPERATION
	protected void readSchedule(OpFeedbackParam<HashMap<Room, EventList>> sched) {
		if (eventsByRoom != null) {
			sched.set(eventsByRoom);
		}
		else {
			failed("no_schedule");
		}
	}
	
	@OPERATION
	protected void isSchedulePossible(Room room, EventRequirements evReq, OpFeedbackParam<Boolean> possible) {
		boolean schedPossible = true;
		
		EventList evList = eventsByRoom.get(room);
		if (evList != null) {
			Calendar start = evReq.getStartDate();
			Calendar end = evReq.getEndDate();
			
			if (!evList.canHost(start, end)) {
				schedPossible = false;
			}
		}
		
		possible.set(schedPossible);
	}
	
	@LINK
	protected void retrieveEvent(String eventTitle, EventType eventType, Date startDate, 
			OpFeedbackParam<Event[]> returnEvents) {
		
		List<Event> matchingEvents = new ArrayList<Event>();
		
		for (EventList eventList : eventsByRoom.values()) {
			Event matchingEvent = eventList.matchEvent(eventTitle, eventType, startDate);
			if (matchingEvent != null) {
				matchingEvents.add(matchingEvent);
			}
		}
		
		Event[] matchingEventsArray = new Event[matchingEvents.size()];
		matchingEvents.toArray(matchingEventsArray);
		
		returnEvents.set(matchingEventsArray);
	}
	
	@OPERATION
	protected void getEventById(int id, OpFeedbackParam<Event> matchingEvent) {
		Collection<EventList> allEventLists = eventsByRoom.values();
		
		for (EventList evList : allEventLists) {
			Event matchEv = evList.matchEvent(id);
			
			if (matchEv != null) {
				matchingEvent.set(matchEv);
				return;
			}
		}
	}
	
	
	
	@OPERATION
	protected void searchPlaceHolderCandidates(Object[] candidateRoomTypes, 
			String constrainedRoomType, int day, int hour, int nrNeeded, 
			OpFeedbackParam<String[]> suitableAgents, OpFeedbackParam<String> eventMovements) {
		
		int nrMatching = 0;
		List<String> matchingAgents = new ArrayList<String>();
		
		
		// set place holder room type list
		List<String> placeHolderRoomTypes = new ArrayList<String>();
		for (int i = 0; i < candidateRoomTypes.length; i++) {
			placeHolderRoomTypes.add((String)candidateRoomTypes[i]);
		}
		
		// perform seep like process - first step: rooms with no events past "hour" 
		for (Room r : eventsByRoom.keySet()) {
			if (nrMatching >= nrNeeded) break;
			
			if ( r.compatible(constrainedRoomType) && placeHolderRoomTypes.contains(r.getType().name())) {
				EventList evList = eventsByRoom.get(r);
				if (!evList.hasEventAfter(day, hour)) {
					matchingAgents.add(r.getManagingAgentName());
					nrMatching++;
					
					// set new room type here - TODO - see if this can be done in RoomStateArtifact
					r.setType(RoomType.valueOf(constrainedRoomType));
				}
			}
		}
		
		// search for possible re-programming as long as needed
		HashMap<Room, HashMap<Event, Room>> reprogrammings = new HashMap<Room, HashMap<Event,Room>>();
		
		
		// perform second step of seep: rooms that can have the event past "hour" delegated
		if (nrMatching < nrNeeded) {		// perform this step only if still needed
			
			// first sort candidate room types according to event list length
			List<Entry<Room, EventList>> sortedRooms = new ArrayList<Entry<Room,EventList>>();
			Set<Entry<Room, EventList>> eventsByRoomSet = eventsByRoom.entrySet();
			
			for (Entry<Room, EventList> entry : eventsByRoomSet) {
				if (placeHolderRoomTypes.contains(entry.getKey().getType().name())) {
					
					int index = Collections.binarySearch(sortedRooms, entry, new Comparator<Entry<Room, EventList>>() {
						@Override
						public int compare(Entry<Room, EventList> o1, Entry<Room, EventList> o2) {
							return o1.getValue().getSize() - o2.getValue().getSize();
						}
					});
					
					// if the event is not already in the list index will be < 0
					if (index < 0) {
						sortedRooms.add(-index - 1, entry);
					}
					
				}
			}
			
			for (int i = 0; i < sortedRooms.size(); i++) {
				if (nrMatching >= nrNeeded) {
					break;
				}
				
				Entry<Room, EventList> entry = sortedRooms.get(i);
				
				// Room r = entry.getKey();
				// EventList evList = entry.getValue();
				
				if (reprogrammableEvents(entry, sortedRooms, day, hour, reprogrammings)) {
					nrMatching++;
				}
			}
			
			for (Room tr : reprogrammings.keySet()) {
				// move events from target room to adopter room
				
				HashMap<Event, Room> movedEvents = reprogrammings.get(tr); 
				for (Event trEv : movedEvents.keySet()) {
					Room adopterRoom = movedEvents.get(trEv);
					
					// remove target event from target room
					eventsByRoom.get(tr).removeEvent(trEv);
					
					// change event room
					trEv.setEventRoom(adopterRoom);
					
					// insert it into adopter room
					eventsByRoom.get(adopterRoom).addEvent(trEv);
				}
				
				tr.setType(RoomType.valueOf(constrainedRoomType));
				
				// add target room agent to list of matching agents
				matchingAgents.add(tr.getManagingAgentName());
			}
		}
		
		String[] matchingAgentsList = new String[matchingAgents.size()];
		matchingAgents.toArray(matchingAgentsList);
		
		//ToProlog[] movedEventChanges = getMovedEventChanges(reprogrammings);
		String movedEventChanges = getMovedEventChanges(reprogrammings);
		eventMovements.set(movedEventChanges);
		
		suitableAgents.set(matchingAgentsList);
	}

	
	//private ToProlog[] getMovedEventChanges(HashMap<Room, HashMap<Event, Room>> reprogrammings) {
	private String getMovedEventChanges(HashMap<Room, HashMap<Event, Room>> reprogrammings) {
		String info = "[";
		for (Room r : reprogrammings.keySet()) {
			HashMap<Event, Room> eventMovings = reprogrammings.get(r);
			for (Event ev : eventMovings.keySet()) {
				String adopterAgent = eventMovings.get(ev).getManagingAgentName();
				info += "moveEvent(" + ev.getAsProlog() + "," + r.getManagingAgentName() + "," + adopterAgent + ")";
				info += ",";
			}
		}
		
		//if (!reprogrammings.isEmpty()) {
		//	info = info.substring(0, info.length() - 1);
		//}
		if (info.length() > 1) {
			info = info.substring(0, info.length() - 1);
		}
		
		info += "]";
		
		return info;
		/*
		final String finalInfo = info;
		return new ToProlog() {
			@Override
			public String getAsPrologStr() {
				return finalInfo;
			}
		};
		*/
	}

	private boolean reprogrammableEvents(Entry<Room, EventList> targetEntry, List<Entry<Room, EventList>> sortedRooms,
			int day, int hour, HashMap<Room, HashMap<Event, Room>> reprogrammings) {
		
		List<Event> targetEvList = targetEntry.getValue().getAllEvents();
		Room targetRoom = targetEntry.getKey();
		HashMap<Event, Room> targetEventMovements = new HashMap<Event, Room>();
		
		// first look to see if targetEntry has no event that is currently running
		for (int k = 0; k < targetEvList.size(); k++) {
			Event targetEvent = targetEvList.get(k);
			if (targetEvent.getStart().get(Calendar.DAY_OF_YEAR) == day && 
				targetEvent.getStart().get(Calendar.HOUR_OF_DAY) < hour &&
				targetEvent.getEnd().get(Calendar.HOUR_OF_DAY) > hour) {
					return false;
			}
		}
		
		// if it doesn't see if all it's events are movable
		for (int k = 0; k < targetEvList.size(); k++) {
			Event targetEvent = targetEvList.get(k);
			
			if (targetEvent.getStart().get(Calendar.DAY_OF_YEAR) != day && targetEvent.getStart().get(Calendar.HOUR_OF_DAY) < hour) {
				// we are only interested in the events past the current hour in the current day
				continue;
			}
			
			boolean targetEventMovable = false;
			
			for (int i = 0; i < sortedRooms.size(); i++) {
				Entry<Room, EventList> entry = sortedRooms.get(i);
				Room adopterRoom = entry.getKey();
				
				// determine if this room is not part of a previous reprogramming at the time of this targetEvent
				boolean roomUsedForOverlappingEvent = false;
				
				for (HashMap<Event, Room> otherMovedEvents : reprogrammings.values()) {
					// search for events that are to be held in the same room as adopterRoom
					for (Event movedEv : otherMovedEvents.keySet()) {
						if (otherMovedEvents.get(movedEv).equals(adopterRoom) && targetEvent.overlaps(movedEv)) {
							// if the events overlap then don't use this room
							roomUsedForOverlappingEvent = true;
							break;
						}
					}
					
					if (roomUsedForOverlappingEvent) {
						break;
					}
				}
				
				// if this room is not part of a previous reprogramming at the time of this targetEvent
				if ( !roomUsedForOverlappingEvent ) {
					EventList adopterEvList = entry.getValue();
					
					if (adopterEvList.canHost(targetEvent)) {
						targetEventMovements.put(targetEvent, adopterRoom);
						targetEventMovable = true;
						break;
					}
					
				}
			}
			
			if (!targetEventMovable) {
				return false;
			}
		}
		
		// if we have come this far it means all target events past the current hour are movable
		// fill the reprogramming structure
		reprogrammings.put(targetRoom, targetEventMovements);
		
		return true;
	}
	
	private void delegateEvents(Room tr, Room adopterRoom, int hour) {
		EventList targetEvList = eventsByRoom.get(tr);
		EventList adopterEvList = eventsByRoom.get(adopterRoom);
		
		for (Event targetEv : targetEvList.getAllEvents()) {
			if (targetEv.getStart().get(Calendar.HOUR_OF_DAY) >= hour) {
				
				// remove target event from target room
				targetEvList.removeEvent(targetEv);
				
				
				// insert it into adopter room
				adopterEvList.addEvent(targetEv);
			}
		}
	}

}
