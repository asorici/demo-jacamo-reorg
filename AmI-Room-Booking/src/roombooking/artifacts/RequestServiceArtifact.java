package roombooking.artifacts;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import roombooking.core.ParticipantList;
import roombooking.core.SystemAgentId;
import roombooking.core.UserRole.RoleType;
import roombooking.core.event.Event;
import roombooking.core.event.EventRequirements;
import roombooking.core.event.Event.EventType;
import roombooking.core.request.DeleteEventRequest;
import roombooking.core.request.ModifyEventRequest;
import roombooking.core.request.Request;
import roombooking.core.request.RequestPool;
import roombooking.core.request.ScheduleEventRequest;
import cartago.AgentId;
import cartago.Artifact;
import cartago.ArtifactId;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cartago.OperationException;


public class RequestServiceArtifact extends Artifact {
	private RequestPool requestPool;
	private HashMap<String, SystemAgentId> agentMap;
	private HashMap<Event, ParticipantList> eventsParticipants;
	
	
	protected void init() {
		requestPool = new RequestPool();
		defineObsProperty("requestPool", requestPool);
		
		agentMap = new HashMap<String, SystemAgentId>();
		eventsParticipants = new HashMap<Event, ParticipantList>();
	}
	
	@Override
	protected void dispose() {
		System.out.println("Disposing RequestServiceArtifact");
		super.dispose();
	}
	
	private SystemAgentId getRequesterId(String agName, String globalAgentId) {
		synchronized(agentMap) {
			if (agentMap.containsKey(agName)) {
				return agentMap.get(agName);
			}
			else {
				SystemAgentId reqId = new SystemAgentId(agName, globalAgentId);
				agentMap.put(agName, reqId);
				return reqId;
			}
		}
	}
	
	@OPERATION
	protected void answerToRequest(Request request, boolean answer, Object additional) {
		//System.out.println("[REQUEST_SERVICE] Answering to request: " + request + " with answer: " + answer 
		//		+ " additional: " + additional);
		
		AgentId callerAgentId = request.getCallerAgentId();
		signal(callerAgentId, "requestAnswer", request.getId(), request.getType().name(), answer, additional);
	}
	
	@OPERATION
	protected void confirmRequestAnswer(int requestId) {
		//System.out.println("[REQUEST_SERVICE] Confirming request answer to request id: " + requestId);
		signal("requestAnswerConfirmation", requestId);
	}
	
	@OPERATION
	protected void scheduleEvent(EventRequirements requirements, OpFeedbackParam<Integer> requestId) {
		AgentId agId = getOpUserId();
		SystemAgentId reqId = getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		Request schedReq = new ScheduleEventRequest(reqId, agId,requirements);
		requestPool.pushRequest(schedReq);
		
		System.out.println("Got a new request: " + schedReq.toString());
		
		getObsProperty("requestPool").updateValue(requestPool);
		//signal(Constants.REQUEST_SIGNAL, RequestType.Schedule.name());
		
		try {
			ArtifactId monitorArtId = lookupArtifact("monitor_service");
			execLinkedOp(monitorArtId, "logRequest", requirements.getType(), 1);
		} catch (OperationException e) {
			System.err.println("INTERNAL ERROR. COULD NOT LOG SCHEDULING REQUEST.");
			e.printStackTrace();
		}
		
		requestId.set(schedReq.getId());
	}
	
	@OPERATION
	protected void modifyEvent(Event event, EventRequirements requirements, OpFeedbackParam<Integer> requestId) {
		AgentId agId = getOpUserId();
		SystemAgentId reqId = getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		Request modifReq = new ModifyEventRequest(reqId, agId, event, requirements);
		requestPool.pushRequest(modifReq);
		
		getObsProperty("requestPool").updateValue(requestPool);
		//signal(Constants.REQUEST_SIGNAL, RequestType.Modify.name());
		
		requestId.set(modifReq.getId());
	}
	
	@OPERATION
	protected void cancelEvent(Event event, OpFeedbackParam<Integer> requestId) {
		AgentId agId = getOpUserId();
		SystemAgentId reqId = getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		Request delReq = new DeleteEventRequest(reqId, agId, event);
		requestPool.pushRequest(delReq);
		
		getObsProperty("requestPool").updateValue(requestPool);
		//signal(Constants.REQUEST_SIGNAL, RequestType.Delete.name());
		
		try {
			ArtifactId monitorArtId = lookupArtifact("monitor_service");
			execLinkedOp(monitorArtId, "logRequest", event.getType(), -1);
		} catch (OperationException e) {
			System.err.println("INTERNAL ERROR. COULD NOT LOG SCHEDULING REQUEST.");
			e.printStackTrace();
		}
		
		requestId.set(delReq.getId());
	}
	
	@OPERATION
	protected void inquireEvent(String eventTitle, EventType eventType, Date startDate, 
			OpFeedbackParam<Event[]> returnEvents) {
		
		AgentId agId = getOpUserId();
		//SystemAgentId reqId = getRequesterId(agId.getGlobalId());
		getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		//Request inqReq = new InquireEventsRequest(reqId, eventTitle, eventType, startDate);
		//requestPool.pushRequest(inqReq);
		
		//signal(Signals.REQUEST_SIGNAL, RequestType.Inquire.name());
		//requestId.set(inqReq.getId());
		
		try {
			ArtifactId schedArtId = lookupArtifact("schedule_service");
			execLinkedOp(schedArtId, "retrieveEvent", eventTitle, eventType, startDate, returnEvents);
		} catch (OperationException e) {
			e.printStackTrace();
			failed(Constants.ERROR_EVENT_UNMATCHED);
		}
	}
	
	@OPERATION
	protected void notifyParticipationIntention(Event event, String role) {
		RoleType userRoleType = RoleType.valueOf(role);
		
		AgentId agId = getOpUserId();
		SystemAgentId reqId = getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		//Request partReq = new ParticipationIntentionRequest(reqId, event, role);
		//requestPool.pushRequest(partReq);
		
		// get currentDateUnit from Simulation artifact
		OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
		
		try {
			ArtifactId artId = lookupArtifact("simulation");
			execLinkedOp(artId, "getDateUnit", dayUnit, hourUnit, minuteUnit);
		} catch (OperationException e) {
			e.printStackTrace();
			failed("INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
		}
		
		Calendar now = Calendar.getInstance();
		now.set(Calendar.DAY_OF_YEAR, dayUnit.get());
		now.set(Calendar.HOUR_OF_DAY, hourUnit.get());
		now.set(Calendar.MINUTE, minuteUnit.get());
		
		if (event.getEnd().before(now)) { 			// if the event is already over, deny the request
			failed(Constants.ERROR_EVENT_OVER);
		} else {
			ParticipantList list = eventsParticipants.get(event);
			if (list != null) {
				list.addParticipant(reqId, userRoleType);
			}

			//System.out.println("[REQUEST_SERVICE] Registering participation intention of user " 
			//				+ reqId.getRequesterName() + ", for event " + event + ", as " + role);
		}
		
		//signal(Signals.REQUEST_SIGNAL, RequestType.ParticipationIntention.name());
	}
	
	@OPERATION
	protected void cancelParticipationIntention(Event event) {
		AgentId agId = getOpUserId();
		SystemAgentId reqId = getRequesterId(agId.getAgentName(), agId.getGlobalId());
		
		//Request cancelReq = new ParticipationCancelationRequest(reqId, event);
		//requestPool.pushRequest(cancelReq);
		
		// get currentDateUnit from Simulation artifact
		OpFeedbackParam<Integer> dayUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> hourUnit = new OpFeedbackParam<Integer>();
		OpFeedbackParam<Integer> minuteUnit = new OpFeedbackParam<Integer>();
		
		try {
			ArtifactId artId = lookupArtifact("simulation");
			execLinkedOp(artId, "getDateUnit", dayUnit, hourUnit, minuteUnit);
		} catch (OperationException e) {
			e.printStackTrace();
			failed("INTERNAL ERROR. REQUEST DATE UNIT OPERATION FAILED.");
		}
		
		Calendar now = Calendar.getInstance();
		now.set(Calendar.DAY_OF_YEAR, dayUnit.get());
		now.set(Calendar.HOUR_OF_DAY, hourUnit.get());
		now.set(Calendar.MINUTE, minuteUnit.get());
		
		if (event.getEnd().before(now)) {
			failed(Constants.ERROR_PARTICIPATION_ALREADY_CANCELED);
		}
		else {
			ParticipantList list = eventsParticipants.get(event);
			if (list != null) {
				list.removeParticipant(reqId);
			}
		}
		
		//signal(Signals.REQUEST_SIGNAL, RequestType.ParticipationCancelation.name());
	}
	
	// =================================================================================== //
	// operations that other services from the RoomStateArtifact, ScheduleArtifact link to //
	// =================================================================================== //
	
	@LINK
	protected void getSystemAgentId(String agentName, OpFeedbackParam<SystemAgentId> requesterId) {
		// equals and hash code of an AgentId are based on its global name - a string - which will be the
		// same in all cases
		requesterId.set(agentMap.get(agentName));
	}
	
	@LINK
	protected void addSystemAgent(String agentName, String globalAgentId, OpFeedbackParam<SystemAgentId> newSysAgentId) {
		SystemAgentId sysAgId = getRequesterId(agentName, globalAgentId);
		newSysAgentId.set(sysAgId);
	}
	
	@LINK
	protected void notifyEventFinish(Event event) {
		eventsParticipants.remove(event);	// we delete the event and its participants from the map
		System.out.println("[REQUEST_SERVICE] Deregistering all participation intentions for finished event "
				+ event);
	}
}
