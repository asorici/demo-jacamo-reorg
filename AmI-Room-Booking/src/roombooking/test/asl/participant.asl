hour_length(10).

!start.

+!start
	<-	!join_workspace;
		!lookup_sim_controller;
		
		.wait({+sim_init});
		
		+do_sim_actions.
		//!make_request("First Meeting", "meetingEvent", 10, 10, 11).
		
+!join_workspace
	<-	joinWorkspace("RequestNotificationWSP", RNWspId);
		+workspace_data("RequestNotificationWSP", RNWspId).
		

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

+joinEvent(AgNameStr, Event, RoomAgent)
	:	do_sim_actions & .term2string(AgName, AgNameStr) & .my_name(AgName)
	<-	!register_for_event(Event, RoomAgent, 1).

		
+!register_for_event(Event, RoomAgent, Trial)
	<-	cartago.invoke_obj(Event, toString, EvStr);
		//.print("Registering for event ", EvStr);
		
		// first announce intention in request service artifact
		//?workspace_data("RequestNotificationWSP", ReqWspId);
		//notifyParticipationIntention(Event, "participant")[artifact_name("request_service"), wsp_id(ReqWspId)];
		
		// then register in room state artifact
		.concat("", RoomAgent, "_wsp", RoomWspName);
		//joinWorkspace(RoomWspName, RoomWspId);
		?getWorkspace(RoomWspName, RoomWspId);
		registerAs("participant", Event)[artifact_name("room_state"), wsp_id(RoomWspId)];
		
		!deregister_when_finished(Event, RoomAgent).


-!register_for_event(Event, RoomAgent, Trial)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	cartago.invoke_obj(Event, toString, EvStr);
		if (Trial == 2) {
			.print("!!!! Failure registering for event ", EvStr, " -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine);	
		}
		else {
			NewTrial = Trial + 1;
			?hour_length(HourLength);
			WaitTime = (HourLength * 2000) div 60;
			.wait(WaitTime);
			!!register_for_event(Event, RoomAgent, NewTrial);
		}.

	
+!deregister_when_finished(Event, RoomAgent)
	<-	?sim_art_id(SimArtId);	
		
		cartago.invoke_obj(Event, getEnd, EventEnd);
		cartago.invoke_obj(EventEnd, get(11), EndHour);
		cartago.invoke_obj(EventEnd, get(12), EndMinute);
		
		?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
		timeDiffAsMinutes(EndHour, EndMinute, HourUnit, MinuteUnit, MinDiffEnd)[artifact_id(SimArtId)];
		
		if ( MinDiffEnd >= -3 & MinDiffEnd <= 0 ) {
			cartago.invoke_obj(Event, toString, EvStr);
			//.print("Deregistering from event ", EvStr);
			
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
		