{ include("common.asl") }
{ include("scheduler/scheduler_join_org.asl") }
{ include("scheduler/scheduler_reorg.asl") }

manage_event_schedule(false).
manage_event_delete(false).
manage_event_modification(false).

/*
	Initial beliefs and goals
*/

// message will be received from building_manager once all workspaces are constructed
+init_complete
	<-	!join_work.

// =================================== Plans for the actual responsabilities ============================== //
+!manage_event_schedule_request[scheme(Scheme)]
	<-	-+manage_event_schedule(true);
		?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_event_schedule_request)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!manage_event_delete_request[scheme(Scheme)]
	<-	-+manage_event_delete(true);
		?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_event_delete_request)[artifact_name(Scheme), wsp_id(OrgId)].
		
	
+!manage_event_modification_request[scheme(Scheme)]
	<-	-+manage_event_modification(true);
		?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_event_modification_request)[artifact_name(Scheme), wsp_id(OrgId)];
		
		?workspace_data("RequestNotificationWSP", ReqWspId);
      	lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
      	markInitComplete[artifact_id(SimArtId)].
	
      	
		
		
// ------------------------------------- Signals for request handling ------------------------------------- //
//@new_request_plan[atomic]
+requestPool(RequestPool)
	:	manage_event_schedule(true) & manage_event_delete(true) & 
		manage_event_modification(true)
	<-	roombooking.test.timemillis(BeginMillis);
	
		cartago.invoke_obj(RequestPool, popRequest, Request);
		// check if a reorganization process is still going on
		.send(building_manager, askOne, performingReorganization(Flag), performingReorganization(Flag), 5000);
		
		if (Flag == false) {		// if under normal run => handle request
			//cartago.invoke_obj(Request, toString, RequestString);
			//.print("Handling request: ", RequestString);
			
			cartago.invoke_obj(Request, getTypeString, ReqType);
			!handle_request(ReqType, Request, BeginMillis);
		}
		else {						// otherwise deny request
			.print("DURING REORGANIZATION PROCESS - REQUEST HANDLING PAUSED");
			?workspace_data("RequestNotificationWSP", ReqWspId);
			answerToRequest(Request, false, reorganization)[artifact_name("request_service"), wsp_id(ReqWspId)];
		}.

// --------- handle schedule requests --------- //
+!handle_request(ReqType, Request, BeginMillis)
	: 	ReqType == "schedule"
	<-	
		//cartago.invoke_obj(Request, toString, RequestString);
		//.print("Handling scheduling request: ", RequestString);
		
		cartago.invoke_obj(Request, getRequirements, EvRequirements);
		cartago.invoke_obj(EvRequirements, getTypeString, EvType);
		
		// update sim gui with all request data
		?sim_art_id(SimArtId);
		updateAllRequestData(EvType, 1)[artifact_id(SimArtId)];
		
		!schedule_event_type(Request, EvType, EvRequirements, BeginMillis).

@schedule_event_type_plan[atomic]
+!schedule_event_type(Request, EvType, EvRequirements, BeginMillis)
	<-	
		cartago.invoke_obj(EvRequirements, getAsProlog, EvReqStr);
		roombooking.test.timemillis(StartHandle);
		DiffHandle = StartHandle - BeginMillis;
		//.print("------------------ It took ", DiffHandle, " ms to start handling ev req ", EvReqStr, " ----------------");
		
		?sim_art_id(SimArtId);
		logMessage("Handling schedule request with event requirements: ", EvReqStr)[artifact_id(SimArtId)];
		
		?event_type(EvRole, EvType);
		
		//.print("Searching for agents of role type: ", EvRole);
		.findall(Agent, play(Agent, EvRole, "room_manager_group"), EvRoleAgents);
		for ( .member(Ag, EvRoleAgents) ) {
			.concat("", Ag, "_wsp", RoomWspName);
			?getWorkspace(RoomWspName, RoomWspId);
			lookupArtifact("room_state", RoomArtId)[wsp_id(RoomWspId)];
			//getRoom(AgentRoom)[artifact_name("room_state"), wsp_id(RoomWspId)];
			getRoom(AgentRoom)[artifact_id(RoomArtId)];
			
			?workspace_data("RequestNotificationWSP", ReqWspId);
			isSchedulePossible(AgentRoom, EvRequirements, Possible)[artifact_name("schedule_service"), wsp_id(ReqWspId)];
			
			if (Possible == true) {
				// ask if room agent can meet format, seatnumber and can book required equipment
				//cartago.invoke_obj(EvRequirements, getAsProlog, EvReqStr);
				
				roombooking.test.timemillis(TestReqOkStart);
				.send(Ag, askOne, isRequirementsOk(EvReqStr, OkMessage), isRequirementsOk(EvReqStr, OkMessage), 5000);
				roombooking.test.timemillis(TestReqOkEnd);
				TestReqOkDiff = TestReqOkEnd - TestReqOkStart;
				//.print("------------------ It took ", TestReqOkDiff, " ms to test requirments ", EvReqStr, " matching ----------------");
				
				if (OkMessage == true) {
					// make event
					createEvent(Request, EvRequirements, AgentRoom, CreatedEvent)[artifact_name("schedule_service"), wsp_id(ReqWspId)];
					
					roombooking.test.timemillis(EndMillis);
					DiffMillis = EndMillis - BeginMillis;
					//.print("::::::: It took ", DiffMillis, " ms to find event slot for event ", EvReqStr, " :::::::");
					
					!!awaitAnswerConfirmation(Request, Ag, RoomWspName, CreatedEvent, EvReqStr);
					.succeed_goal(schedule_event_type(Request, EvType, EvRequirements, BeginMillis));
				}
			}
		};
		
		// if we got till over here it means that no room has the possibility to host the event
		?workspace_data("RequestNotificationWSP", ReqWspId);
		answerToRequest(Request, false, false)[artifact_name("request_service"), wsp_id(ReqWspId)];
		
		//cartago.invoke_obj(EvRequirements, getAsProlog, EvReqStr);
		.print("NO SLOT OPEN TO SCHEDULE EVENT ", EvReqStr);
		
		// update sim gui with denied request
		//?sim_art_id(SimArtId);
		updateDeniedRequestData(EvType, 1)[artifact_id(SimArtId)];
		logMessage("No slot open to schedule event ", EvReqStr)[artifact_id(SimArtId)];
		
		// notify all concerned room agents that event could not be scheduled
		for ( .member(Ag, EvRoleAgents) ) {
			.concat("", Ag, "_wsp", RoomWspName);
			//joinWorkspace(RoomWspName, RoomWspId);
			?getWorkspace(RoomWspName, RoomWspId);
			
			notifyUnscheduledEvent[artifact_name("room_state"), wsp_id(RoomWspId)];
			//quitWorkspace;
		}.

-!schedule_event_type(Request, EvType, EvRequirements, BeginMillis)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure scheduling event type ", EvType, " -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine);
		?workspace_data("RequestNotificationWSP", ReqWspId);
		answerToRequest(Request, false, false)[artifact_name("request_service"), wsp_id(ReqWspId)].	


+!awaitAnswerConfirmation(Request, Ag, RoomWspName, CreatedEvent, EvReqStr)
	<-	?workspace_data("RequestNotificationWSP", ReqWspId);
		?getWorkspace(RoomWspName, WSPID);
		
		lookupArtifact("room_state", RoomArtId)[wsp_id(WSPID)];
		notifyScheduleRequest(CreatedEvent)[artifact_id(RoomArtId)];
		
		cartago.invoke_obj(Request, getId, RequestId);
		answerToRequest(Request, true, CreatedEvent)[artifact_name("request_service"), wsp_id(ReqWspId)];
					
		.wait({+requestAnswerConfirmation(RequestId)}, 5000, TimeTaken);
		//?is_answer_confirmed(RequestId, 0, Confirmed);
		
		if ( TimeTaken >= 5000 ) {		// no confirmation received => cancel event
		//if ( not Confirmed ) {		// no confirmation received => cancel event
			.print("+++++++++++++++++++++ No answer confirmation received for request id ", RequestId, " for ", TimeTaken, " ms ++++++++++++++++++s");
			removeScheduledEvent(CreatedEvent)[artifact_name("schedule_service"), wsp_id(ReqWspId)];
			.send(Ag, achieve, releasePendingEquipment(EvReqStr));
		}
		else {
			//.print("Received answer confirmation for request id ", RequestId, " within ", TimeTaken, " ms");
			// add new event to the appropriate room
			//joinWorkspace(RoomWspName, WSPID);
			
			//addEvent(CreatedEvent)[artifact_name("room_state"), wsp_id(WSPID)];
			addEvent(CreatedEvent)[artifact_id(RoomArtId)];
			-request_answer_confirmation(RequestId);
		}.

-!awaitAnswerConfirmation(Request, Ag, RoomWspName, CreatedEvent, EvReqStr)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure awaiting answerConfirmation for event ", EvReqStr, " -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

+?is_answer_confirmed(RequestId, Ct, Confirmed)
	:	request_answer_confirmation(RequestId) 
	<-	-request_answer_confirmation(RequestId);
		Confirmed = true.
	
+?is_answer_confirmed(RequestId, Ct, Confirmed)
	:	not request_answer_confirmation(RequestId) 
	<-	if (Ct < 100) {
			NewCt = Ct + 1;
			.wait(50);
			?is_answer_confirmed(RequestId, NewCt, Confirmed);
		}
		else {
			Confirmed = false;
		}.

+requestAnswerConfirmation(RequestId)
	<-	+request_answer_confirmation(RequestId).
		 
// --------- handle delete requests --------- //	
+!handle_request(ReqType, Request, BeginMillis)
	: 	ReqType == "delete"
	<-	true.

// --------- handle modify requests --------- //
+!handle_request(ReqType, Request, BeginMillis)
	: 	ReqType == "modify"
	<-	true.