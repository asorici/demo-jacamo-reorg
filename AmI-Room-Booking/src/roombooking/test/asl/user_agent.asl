// ================================ TEST ACTIONS ============================= //
hour_length(10).

!start.

+!start
	<-	!join_workspace;
		!lookup_sim_controller;
		
		.wait({+sim_init});
		
		+do_sim_actions.
		//!make_request("First Meeting", "meetingEvent", 10, 10, 11).
		
+!join_workspace
	<-	.wait(1000);
		joinWorkspace("RequestNotificationWSP", RNWspId);
		+workspace_data("RequestNotificationWSP", RNWspId);
		lookupArtifact("request_service", ReqArtId);
		+req_service_art_id(ReqArtId);
		cartago.new_array("java.lang.String[]", ["requestAnswer"], Array);
     	cartago.new_obj("cartago.events.SignalFilter", [Array], Filter);
		focus(ReqArtId, Filter).
		

-!join_workspace
	<-	.wait(1000);
		!join_workspace.


+!lookup_sim_controller
	<-	?workspace_data("RequestNotificationWSP", RNWspId);
		lookupArtifact("simulation", SimArtId)[wsp_id(RNWspId)];
		+sim_art_id(SimArtId);
		focus(SimArtId).
		
-!lookup_sim_controller
	<-	.wait(1000);
		!lookup_sim_controller.

+sendRequest(AgNameStr, EvReq)
	:	.term2string(AgName, AgNameStr) & .my_name(AgName)
	<-	//if ( simPaused(false) ) {
	 		!send_request(EvReq).
	 	//}
	 	//else {
	 	//	.wait({+simPaused(false)}, 5000, TimeTaken);
	 	//	if ( TimeTaken < 5000 ) {
	 	//		!send_request(EvReq);
	 	//	};
	 	//	.wait({+simPaused(false)});
		//	!send_request(EvReq);
	 	//}.
	
+!send_request(EvReq)
	<-	cartago.invoke_obj(EvReq, getAsProlog, EvReqStr);
		.print("Making request: ", EvReqStr);
		
		//joinWorkspace("RequestNotificationWSP", RNWspId);
		?req_service_art_id(ReqArtId);
		
		roombooking.test.timemillis(SchedReqStart);
		scheduleEvent(EvReq, RequestId)[artifact_id(ReqArtId)];
		roombooking.test.timemillis(SchedReqEnd);
		SchedReqDiff = SchedReqEnd - SchedReqStart;
		//.print("------------------ It took ", SchedReqDiff, " ms to make schedule request id ", RequestId, " ----------------");
		
		// wait for response to request
		.wait({+requestAnswer(RequestId, "schedule", Answer, Event)}, 5000, TimeTaken);
		//.print("---- Time taken value: ", TimeTaken, " for answer to requestId ", RequestId, " for event req ", EvReqStr, " ----");
		
		if ( TimeTaken < 5000 ) {
			if (Answer == true) {
				// confirm request answer received
				//.wait(100);
				confirmRequestAnswer(RequestId)[artifact_id(ReqArtId)];
				
				cartago.invoke_obj(Event, toString, EventString);
				.print("::::::::::::::: I have booked event: ", EventString);
				
				// add event to scheduled events in simulation
				?sim_art_id(SimArtId);
				addScheduledEvent(Event)[artifact_id(SimArtId)];
				logMessage("I have booked event: ", EventString)[artifact_id(SimArtId)];
			}
			else {
			//	.print("==== Booking action failed for evReq: ", EvReqStr , ". ====");
				?sim_art_id(SimArtId);
				logMessage("Booking failed for event: ", EvReqStr)[artifact_id(SimArtId)];
			}
		}
		else {
			.print(" -------------- Request timed out for requestId ", RequestId);
		}.
		

-!send_request(EvReq)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure sending request -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).



+joinEvent(AgNameStr, Event, RoomAgent)
	:	do_sim_actions & .term2string(AgName, AgNameStr) & .my_name(AgName)
	<-	!register_for_event(Event, RoomAgent).
		
+!register_for_event(Event, RoomAgent)
	<-	cartago.invoke_obj(Event, toString, EvStr);
		.print("Registering for event ", EvStr);
		
		// first announce intention in request service artifact
		//?workspace_data("RequestNotificationWSP", ReqWspId);
		//notifyParticipationIntention(Event, "presenter")[artifact_name("request_service"), wsp_id(ReqWspId)];
		
		// then register in room state artifact
		.concat("", RoomAgent, "_wsp", RoomWspName);
		//joinWorkspace(RoomWspName, RoomWspId);
		?getWorkspace(RoomWspName, RoomWspId);
		
		registerAs("presenter", Event)[artifact_name("room_state"), wsp_id(RoomWspId)];
		
		!deregister_when_finished(Event, RoomAgent).

-!register_for_event(Event, RoomAgent)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	cartago.invoke_obj(Event, toString, EvStr);
		.print("!!!! Failure registering for event ", EvStr, " -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).


+!deregister_when_finished(Event, RoomAgent)
	<-	?sim_art_id(SimArtId);	
	
		cartago.invoke_obj(Event, getEnd, EventEnd);
		cartago.invoke_obj(EventEnd, get(11), EndHour);
		cartago.invoke_obj(EventEnd, get(12), EndMinute);
		
		?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
		timeDiffAsMinutes(EndHour, EndMinute, HourUnit, MinuteUnit, MinDiffEnd)[artifact_id(SimArtId)];
		
		if (MinDiffEnd >= -3 & MinDiffEnd <= 0) {
			cartago.invoke_obj(Event, toString, EvStr);
			.print("Deregistering from event ", EvStr);
			
			.concat("", RoomAgent, "_wsp", RoomWspName);
			//joinWorkspace(RoomWspName, RoomWspId);
			?getWorkspace(RoomWspName, RoomWspId);
			
			deregister[artifact_name("room_state"), wsp_id(RoomWspId)];
		}
		else {
			?hour_length(HourLength);
			WaitTime = (HourLength * 1000) div 60;
			.wait(WaitTime);
			!!deregister_when_finished(Event, RoomAgent);
		}.
		
-!deregister_when_finished(Event, RoomAgent)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	cartago.invoke_obj(Event, toString, EvStr);
		.print("!!!! Failure to deregister from event ", EvStr, " -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
		
		
+?getWorkspace(WorkspaceName, WspId)
	:	workspace_data(WorkspaceName, WspId)
	<-	true.
	
+?getWorkspace(WorkspaceName, WspId)
	:	not workspace_data(WorkspaceName, _)
	<-	joinWorkspace(WorkspaceName, WspId);
		+workspace_data(WorkspaceName, WspId).
		