// =========================================== Room management plans =========================================== //
// ############# teaching ############## //
+!manage_teaching_events[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_teaching_events)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_teaching_equipment[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_teaching_equipment)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_teaching_energy[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_teaching_energy)[artifact_name(Scheme), wsp_id(OrgId)].
	

// ############# meeting ############# //
+!manage_meeting_events[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_meeting_events)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_meeting_equipment[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_meeting_equipment)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_meeting_energy[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_meeting_energy)[artifact_name(Scheme), wsp_id(OrgId)].

	
// ############# brainstorm ############# //
+!manage_brainstorm_events[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(manage_brainstorm_events)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_brainstorm_equipment[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_brainstorm_equipment)[artifact_name(Scheme), wsp_id(OrgId)].
	
+!monitor_brainstorm_energy[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(monitor_brainstorm_energy)[artifact_name(Scheme), wsp_id(OrgId)].
	

// ========================================= Room Management main loop ========================================= //
+!do_management_loop
	<-	// normal loop only runs if no reorganization is in place -> check for that first
		.send(building_manager, askOne, performingReorganization(Flag), performingReorganization(Flag), 5000);
		if (Flag == true) {
			// wait for a while and resume loop
			PauseLength = (10 * 1000) div 30;
			.wait(PauseLength);
			!!do_management_loop;
		} 
		else {
			// check for new event if one not already running 
			// if new event also 
			?check_for_event(Exists);
			
			if (Exists) {
				// 1) check event start - announce energy usage and turn on utilities
				!do_event_start;
				
				// 2) handle "during event" situations e.g. no users present -> turn off utilities
				!do_event_running;
			
				// 3) check if current event needs to end - in this case call nextEvent
				!do_event_ending;
			}
			
			// pause for 1/30 of HOUR_UNIT(10 seconds) and re-initiate loop
			PauseLength = (10 * 1000) div 30;
			.wait(PauseLength);
			!!do_management_loop;
		}.

//@event_beginning_plan[atomic]
+?check_for_event(Exists)
	<-	?room_artifact_id(RoomArtId);
	
		?currentEvent(CurrentEv);
		if ( not .ground(CurrentEv) ) {		// if no current event call next event to see if we have one
			// switch off utilities - if there will be a next event they will be turned on when doing the loop again
			?utilities_artifact_id(ArdUtilId);
			if ( onstate(true) ) {
				setUtilitiesOn(false)[artifact_id(ArdUtilId)];
			};
			
			?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
			//.print("NO EVENT AT ", DayUnit, " - ", HourUnit, " : ", MinuteUnit);
			
			nextEvent[artifact_id(RoomArtId)];
			Exists = false;
		}
		else {
			// new event has just been detected 
			Exists = true;
		}.

//@event_start_plan[atomic]
+!do_event_start
	<- 	?room_artifact_id(RoomArtId);
		?utilities_artifact_id(ArdUtilId);
		?sim_art_id(SimArtId);
		
		?currentEvent(CurrentEv);
		?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
		
		cartago.invoke_obj(CurrentEv, getStart, EventStart);
		cartago.invoke_obj(EventStart, get(11), StartHour);
		cartago.invoke_obj(EventStart, get(12), StartMinute);
		
		timeDiffAsMinutes(StartHour, StartMinute, HourUnit, MinuteUnit, MinDiff)[artifact_id(SimArtId)];
		
		if ( MinDiff >= -3 & MinDiff <= 3 ) {
			// turn on utilities
			if (onstate(false)) {
				setUtilitiesOn(true)[artifact_id(ArdUtilId)];
			};
			
			cartago.invoke_obj(CurrentEv, getAsProlog, EvStr);
			.print("[EVENT_START] Event ", EvStr, " is starting.");
			//.print("[EVENT_START] Turning on utilities - if not already on.");
		
			// announce energy usage
			//.print("[EVENT_START] Announcing energy estimate.");
			announceEnergyEstimate[artifact_id(RoomArtId)];
			
			logMessage("Event starting: ", EvStr)[artifact_id(SimArtId)];
		}.

//@event_running_plan[atomic]
+!do_event_running
	<-	?room_artifact_id(RoomArtId);
		?utilities_artifact_id(ArdUtilId);
		?sim_art_id(SimArtId);
		
		?currentEvent(CurrentEv);
		?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
		
		cartago.invoke_obj(CurrentEv, getStart, EventStart);
		cartago.invoke_obj(EventStart, get(11), StartHour);
		cartago.invoke_obj(EventStart, get(12), StartMinute);
		
		cartago.invoke_obj(CurrentEv, getEnd, EventEnd);
		cartago.invoke_obj(EventEnd, get(11), EndHour);
		cartago.invoke_obj(EventEnd, get(12), EndMinute);
		
		timeDiffAsMinutes(StartHour, StartMinute, HourUnit, MinuteUnit, MinDiffStart)[artifact_id(SimArtId)];
		timeDiffAsMinutes(EndHour, EndMinute, HourUnit, MinuteUnit, MinDiffEnd)[artifact_id(SimArtId)];
		
		// if during event
		if ( MinDiffStart >= 3 & MinDiffEnd < -6 ) {
			// get count of registered users
			registeredUserCount(RegUserCount)[artifact_id(RoomArtId)];
			cartago.invoke_obj(CurrentEv, getAsProlog, EvStr);
			
			
			if (RegUserCount > 0) {
				// check that utilities are on - setUtilitiesOn method does this
				if ( onstate(false) ) {
					setUtilitiesOn(true)[artifact_id(ArdUtilId)];
				};
					
				//.print("[EVENT_RUNNING] I have ", RegUserCount, " registered users for event ", EvStr);
				//.print("[EVENT_RUNNING] Performing utilities on check.");
			}
			else {
				//.print("[EVENT_RUNNING] I have ", RegUserCount, " registered users for event ", EvStr);
				//.print("[EVENT_RUNNING] NO USERS - Turning off utilities.");
				if ( onstate(true) ) {
					setUtilitiesOn(false)[artifact_id(ArdUtilId)];
				};
			};
			
		}.

//@event_end_plan[atomic]
+!do_event_ending
	<-	?room_artifact_id(RoomArtId);
		?utilities_artifact_id(ArdUtilId);
		?sim_art_id(SimArtId);
		
		?currentEvent(CurrentEv);
		?currentDateUnit(DayUnit, HourUnit, MinuteUnit);
		
		cartago.invoke_obj(CurrentEv, getEnd, EventEnd);
		cartago.invoke_obj(EventEnd, get(11), EndHour);
		cartago.invoke_obj(EventEnd, get(12), EndMinute);
		
		timeDiffAsMinutes(EndHour, EndMinute, HourUnit, MinuteUnit, MinDiffEnd)[artifact_id(SimArtId)];
		
		// if at event end
		if ( MinDiffEnd >= -6 & MinDiffEnd < -3 ) {
			// get count of registered users
			registeredUserCount(RegUserCount)[artifact_id(RoomArtId)];
			cartago.invoke_obj(CurrentEv, getAsProlog, EvStr);
			
			if (RegUserCount > 0) {
				// check that utilities are on - turnOnUtilities method does this
				if (onstate(false)) {
					setUtilitiesOn(true)[artifact_id(ArdUtilId)];
				};
				
				.print("[EVENT_ENDING] I still have ", RegUserCount, " registered users for event ", EvStr);
				.print("[EVENT_ENDING] Signaling event finish to users.");
			}
			else {
				.print("[EVENT_ENDING] I have ", RegUserCount, " registered users for event ", EvStr);
				.print("[EVENT_ENDING] NO USERS - event considered as finished.");
				
				.print("[EVENT_ENDING] Turning off utilities.");
				setUtilitiesOn(false)[artifact_id(ArdUtilId)];
				
				// announce event finish
				announceEventFinish(CurrentEv)[artifact_id(RoomArtId)];
				
				// free booked equipment
				releaseEquipment(CurrentEv, 0)[artifact_id(RoomArtId)];
				
				// remove event from event list
				removeEvent(CurrentEv)[artifact_id(RoomArtId)];
				
				// call nextEvent
				.print("[EVENT_ENDING] Calling next event.");
				nextEvent[artifact_id(RoomArtId)];
			};
		}
		else {
			//if ( MinDiffEnd >= -3  & MinDiffEnd < 0 ) {
			if ( MinDiffEnd >= -3 ) {
				cartago.invoke_obj(CurrentEv, getAsProlog, EvStr);
				
				// force event end
				.print("[EVENT_ENDING] Event ", EvStr, " has reached end. Turning off utilities.");
				setUtilitiesOn(false)[artifact_id(ArdUtilId)];
				
				// announce event finish
				announceEventFinish(CurrentEv)[artifact_id(RoomArtId)];
				
				// free booked equipment
				//.print("Releasing equipment for current event ", EvStr);
				releaseEquipment(CurrentEv, 0)[artifact_id(RoomArtId)];
				
				// remove event from event list
				.print("Removing current event ", EvStr);
				removeEvent(CurrentEv)[artifact_id(RoomArtId)];
				
				// call nextEvent
				.print("[EVENT_ENDING] Calling next event.");
				nextEvent[artifact_id(RoomArtId)];
			}
		}.


-!do_event_ending[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Error in room ending block -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

-!do_management_loop[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	// print error
		.print("!!!! Error in room management loop -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine);
		
		// resume loop
		!!do_management_loop.