{ include("common.asl") }
{ include("roomagent/room_management.asl") }
{ include("roomagent/room_reorg.asl") }
{ include("roomagent/room_join_org.asl") }


/*
	Initial beliefs and goals
*/
+init_complete
	<-	!join_work;
		!do_management_loop.

// =================================== Plans for the actual responsabilities ============================== //
// ============================================ Monitoring ================================================ //

+!monitor_request_trend_growing[scheme(Scheme)]
	<-	//goalAchieved(monitor_request_trend_growing)[artifact_name(Scheme)].
		// set monitor_request_trend_growing flag.
		+monitor_req_growing_flag.
		//.print("Monitoring phase :::: Growing request trend monitoring is not performed by me in the current system").


+!monitor_request_trend_dropping[scheme(Scheme)]
	<-	//goalAchieved(monitor_request_trend_dropping)[artifact_name(Scheme)].
		// +monitor_req_dropping_flag;
		//.print("Monitoring phase :::: Dropping request trend monitoring is not performed by me in the current system").
		true.


+!monitor_energy[scheme(Scheme)]
	<-	//goalAchieved(monitor_energy)[artifact_name(Scheme)].
		// +monitor_energy_flag;
		//.print("Monitoring phase :::: Energy monitoring is not performed by me in the current system");
		
		// announce init complete for room_agent
		joinWorkspace("RequestNotificationWSP", ReqWspId);
      	lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
      	markInitComplete[artifact_id(SimArtId)].
		
-!monitor_energy[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure monitoring energy -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

// ============================================ Practical Design =============================================== //
+!practice_des[scheme(Scheme)]
	<- 	.print("Design phase :::: Room agent practical design is not yet implemented in the system.").


		
// ======================== RoomArtifact notifications for request type change assesment ======================== //

+event_counter_update(CurrentHourUnit, EventCt, LastHourDiff, NextHourExp, RemUnitSlots)
	:	monitor_req_growing_flag
	<-	.send(building_manager, askOne, performingReorganization(Flag), performingReorganization(Flag), 5000);
		if (Flag == false) {
			?goalState("reorganization_scheme", monitor_request_trend_growing, _, _, State);
			if ( State == feasible ) {
			//if ( State == enabled ) {
				if (CurrentHourUnit <= 14 & NextHourExp > RemUnitSlots) {
					.print("ANALYZING EVENT COUNTER UPDATE AT HOUR: ", CurrentHourUnit);
				
					.my_name(Me);
					?play(Me, Role, "room_manager_group");
					?request_message_type(Role, MsgType);
					?monitorCommRing(Me, right, RightN);
					.print("========== Have noticed a fault ", 
					request_trend_growing(Me, MsgType, CurrentHourUnit, EventCt, LastHourDiff, NextHourExp, RemUnitSlots, 1, 1),
					". Looking for confirmation. ==========");
					.send(RightN, tell, monitor_msg(Me, request_trend_growing(Me, MsgType, CurrentHourUnit, 
											EventCt, LastHourDiff, NextHourExp, RemUnitSlots, 1, 1)));
				};
			}
		}.

+monitor_msg(Sender, request_trend_growing(Initiator, ReqType, Hour, EvCt, LastHourDiff, NextHourExp, 
											RemUnitSlots, NumAgree, NumObs))
	<-	.print("Monitoring phase :::: Handling request_trend_growing message from ", Sender, 
				": [", ReqType, ",", NumAgree, "," , NumObs, "]");
		?workspace_data("OrgSpecWSP", OrgId);
		
		.my_name(Me);
		if ( monitorCommRing(Me, left, Sender) ) {
			if (Initiator == Me) {
				//Threshold = (2 * NumObs) div 3;
				
				//if (NextHourExp > RemUnitSlots | NumAgree > Threshold) {
				if (NextHourExp > RemUnitSlots) {
					.print("Monitoring phase :::: growing trend observed for request >> ", ReqType);
					?sim_art_id(SimArtId);
					logMessage("Monitoring phase :: have noticed fault ", request_trend_growing(Me, ReqType, Hour, 
									NextHourExp, RemUnitSlots, NumAgree, NumObs))[artifact_id(SimArtId)];
					
					?play(Agent, org_manager, "reorg_group");
					
					// send out monitoring notification only if goal is still feasible
					// this also ensures that no monitoring notification is sent during reorganization
					// because the goal has already been satisfied
					?goalState("reorganization_scheme", monitor_request_trend_growing, _, _, State);
					if ( State == feasible ) {
					//if ( State == enabled ) {
						.send(Agent, tell, monitor_fault(request_trend_growing(Me, ReqType, Hour, 
												NextHourExp, RemUnitSlots, NumAgree, NumObs)));
						goalAchieved(monitor_request_trend_growing)[artifact_name("reorganization_scheme"), wsp_id(OrgId)];
					}
				}
				else {
					.print("Monitoring phase :::: There is no growing trend observed anywhere else.");
				}
			}
			else {
				.concat("", Me, "_wsp", RoomWspName);
				?workspace_data(RoomWspName, WspId);
				//cartago.set_current_wsp(WspId);
				
				getEventCountInfo(InfoStr)[artifact_name("room_state"), wsp_id(WspId)];
				.term2string(InfoStrExpr, InfoStr);
				InfoStrExpr = event_counter_update(MyHour, MyEvCt, MyDiff, MyExp, MyRemSlots);
				
				.print("Monitoring phase :::: My status: ", event_counter_update(MyHour, MyEvCt, MyDiff, MyExp, MyRemSlots));
				
				NewEvCt = EvCt + MyEvCt;
				NewDiff = LastHourDiff + MyDiff;
				NewExp = NextHourExp + MyExp;
				NewRemSlots = RemUnitSlots + MyRemSlots;
				NewNumObs = NumObs + 1;
				
				?monitorCommRing(Me, right, RightN);
				
				if (Hour <= 14 & MyExp > MyRemSlots) {
					NewNumAgree = NumAgree + 1;
					.send(RightN, tell, monitor_msg(Me, request_trend_growing(Initiator, ReqType, Hour, NewEvCt, 
										NewDiff, NewExp, NewRemSlots, NewNumAgree, NewNumObs)));
				}
				else {
					NewNumAgree = NumAgree;
					.send(RightN, tell, monitor_msg(Me, request_trend_growing(Initiator, ReqType, Hour, NewEvCt, 
										NewDiff, NewExp, NewRemSlots, NewNumAgree, NewNumObs)));
				}
			}
		}.

-monitor_msg(Sender, request_trend_growing(Initiator, ReqType, Hour, EvCt, LastHourDiff, NextHourExp, 
			RemUnitSlots, NumAgree, NumObs))[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure in monitor message -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
	
// ============================== Messages for new event requirements satisfaction ============================== //
+?isRequirementsOk(EvReqStr, OkMessage)
	<-	// check event requirements string
		?room_artifact_id(RoomArtId);
		//.print("Searching to match requirements: ", EvReqStr);
		canMeetRequirements(EvReqStr, OkMessage)[artifact_id(RoomArtId)].
	
-?isRequirementsOk(EvReqStr, OkMessage)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure answering requirementsOk inquiry -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine);
		OkMessage = false.

+!releasePendingEquipment(EvReqStr)
	<-	?room_artifact_id(RoomArtId);
		//.print("Releasing pending equipment for event requirements: ", EvReqStr);
		releaseEquipment(EvReqStr)[artifact_id(RoomArtId)].
		
-!releasePendingEquipment(EvReqStr)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure releasing pending equipment for event req: ", EvReqStr, " -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
	

// ========================================= Simulation control messages ======================================== //
+sim_init
	<-	.print("Received sim init message ... starting monitoring of event requests.");
		.my_name(Me);
		.concat("", Me, "_wsp", RoomWspName);
		
		?room_artifact_id(RArtId);
		?sim_art_id(SimArtId);
		?workspace_data(RoomWspName, RoomWspId);
		
		linkArtifacts(RArtId, "simulation-management", SimArtId)[wsp_id(RoomWspId)];
		startEventCounting[artifact_id(RArtId)].
