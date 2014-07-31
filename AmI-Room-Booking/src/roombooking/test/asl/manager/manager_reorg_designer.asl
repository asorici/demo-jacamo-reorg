current_fault(none).

disposed_group_artifacts([], []).
disposed_group_artifacts([ plays(A, R, G) | T ], List) :- G \== "room_manager_group" & 
															disposed_group_artifacts(T, L) & .concat([G], L, List).
disposed_group_artifacts([ plays(A, R, G) | T ], List) :- G == "room_manager_group" & disposed_group_artifacts(T, List).



// ======================================== INVITATION PHASE =========================================== //
// invitation goal - invite designers to the reorganization group - these are already there as our org_participants
// are also designers
+!invitation[scheme(Scheme)]
	<-	!wait_for_fault(Scheme).

+!wait_for_fault(Scheme)
	:	current_fault(none)
	<-	.print("Waiting for fault");
		.wait({+monitor_msg(Sender, Fault)}, 5000);
		
		?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		goalAchieved(invitation)[artifact_name(Scheme), wsp_id(WspId)].
		
+!wait_for_fault(Scheme)
	:	current_fault(Fault) & Fault \== none
	<-	.print("Already have received fault: ", Fault);
		?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		goalAchieved(invitation)[artifact_name(Scheme), wsp_id(WspId)].

-!wait_for_fault(Scheme)
	<-	.print("INVITATION PHASE :::: No fault detected for 5 seconds - stopping reorganization").

+!wait_des[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		goalAchieved(wait_des)[artifact_name(Scheme), wsp_id(WspId)].



// ========================================== DESIGN PHASE ============================================= //
// design goal - as org_manager this agent needs to take assure the design part has been achieved

+monitor_fault(Fault)
	:	not performing_reorganization_flag
	<-	+performing_reorganization_flag;
		
		?sim_art_id(SimArtId);
		simulationPaused(true)[artifact_id(SimArtId)];
		logKeyMessage("RECEIVED NEW FAULT: ", Fault, ". STARTING REORG.")[artifact_id(SimArtId)];
		
		.print("RECEIVED NEW FAULT: ", Fault);
		-+current_fault(Fault).

+?performingReorganization(Flag)
	:	performing_reorganization_flag
	<-	Flag = true.
	
+?performingReorganization(Flag)
	<-	Flag = false.

/*
+!practice_des[scheme(Scheme)]
	<-	!construct_reorganization_plan(Scheme).
*/

+!practice_des[scheme(Scheme)]
	:	current_fault(request_trend_growing(Initiator, ReqType, Hour, 
							NextExp, RemUnitSlots, NumAgree, NumObs))
	<-	!construct_reorg_plan_req_growing(Scheme, ReqType, Hour, NextExp, RemUnitSlots, NumAgree, NumObs).
	

+!practice_des[scheme(Scheme)]
	:	current_fault(request_trend_dropping(Initiator, ReqType, Hour,
							NextExp, RemUnitSlots, NumAgree, NumObs))
	<-	!construct_reorg_plan_req_dropping(Scheme, ReqType, Hour, NextExp, RemUnitSlots, NumAgree, NumObs).
	

+!practice_des[scheme(Scheme)]
	:	current_fault(energy_level(Initiator, ReqType, Hour,
							Level, Threshold, NumAgree, NumObs))
	<-	!construct_reorg_plan_energy(Scheme, ReqType, Hour, Level, Threshold, NumAgree, NumObs).
				

+!construct_reorg_plan_req_growing(Scheme, ReqType, Hour, NextExp, RemUnitSlots, NumAgree, NumObs)
	<-	Surplus = NextExp - RemUnitSlots;
		NumHours = 20 - Hour;
		roombooking.test.ceil(Surplus, NumHours, NumRoomsNeeded);
		
		.print("!!! Rooms needed to acustom surplus: ", NumRoomsNeeded);
		
		?request_message_type(ConstrainedRoleType, ReqType);
		?room_type(ConstrainedRoleType, ConstrainedRoomType);
		
		//.print("Constrained role type: ", ConstrainedRoleType);
		
		//.findall(RoleType, request_message_type(RoleType, MsgType) & MsgType \== ReqType, CandidateRoles);
		.findall(RoomType, 
				request_message_type(RoleType, MsgType) & MsgType \== ReqType & room_type(RoleType, RoomType), 
				CandidateRoomTypes);
		
		?workspace_data("OrgSpecWSP", OrgId);
		?workspace_data("RequestNotificationWSP", WspId);
		?currentDateUnit(DayUnit, _, _);
		
		//searchPlaceHolderCandidates(CandidateRoles, ConstrainedRoleType, NumRoomsNeeded, Hour, PlaceHolderAgents)[artifact_name("schedule_service")];
		//searchPlaceHolderCandidates(CandidateRoomTypes, ConstrainedRoomType, DayUnit, Hour, NumRoomsNeeded, 
		//							PlaceHolderAgents, EventMovements)[artifact_name("schedule_service"), wsp_id(WspId)];
		
		searchPlaceHolderCandidates(CandidateRoomTypes, ConstrainedRoomType, DayUnit, Hour, NumRoomsNeeded, 
									PlaceHolderAgents, EventMovementsString)[artifact_name("schedule_service"), wsp_id(WspId)];
		
		.term2string(EventMovements, EventMovementsString);
		.print("++++++++ Place holder list: ", PlaceHolderAgents);
		.print("++++++++ EventMovements list: ", EventMovements);
		
		//if ( .length(PlaceHolderAgents, Ct) & Ct \== NumRoomsNeeded ) {
		if ( .length(PlaceHolderAgents, 0) ) {
			.print("Cannot find any suitable candidates to play the role of ", ConstrainedRoleType);
			!cancel_reorg(Scheme);
		}
		else {
			// it is possible that some room agents had to clear their schedule to change their role
			// their schedule has been cleared by searchPlaceHolderCandidates in the ScheduleState artifact
			// but we need to do the changes at room agent level too, so we will do actions on the RoomStateArtifacts
			
			for ( .member(moveEvent(event(EvId, _, _, _, _), FromAgent, ToAgent),  EventMovements) ) {
				getEventById(EvId, EventObj)[artifact_name("schedule_service"), wsp_id(WspId)];
				
				if ( not .ground(EventObj) ) {
					.print("Error. There is no event with id: ", EvId);
				}
				else {
					// remove Event from FromAgent
					.concat("", FromAgent, "_wsp", FromRoomWspName);
					//joinWorkspace(FromRoomWspName, FromRoomWspId);
					?getWorkspace(FromRoomWspName, FromRoomWspId);
					
					lookupArtifact("room_state", FromRoomArtId)[wsp_id(FromRoomWspId)];
					removeEvent(EventObj)[artifact_id(FromRoomArtId)];
					getBookedEquipment(EventObj, BookedEqList)[artifact_id(FromRoomArtId)];
					
					// add event to ToAgent
					.concat("", ToAgent, "_wsp", ToRoomWspName);
					?getWorkspace(ToRoomWspName, ToRoomWspId);
					
					lookupArtifact("room_state", ToRoomArtId)[wsp_id(ToRoomWspId)];
					addEvent(EventObj)[artifact_id(ToRoomArtId)];
					if (not .length(BookedEqList, 0)) {
						setBookedEquipment(EventObj, BookedEqList)[artifact_id(ToRoomArtId)];
					};
					
					// mirror current event room change in equipment state artifact as well
					// just move the booked equipment from one room to another 
					cartago.invoke_obj(EventObj, getEventRoom, EvRoom);
					updateEventRoom(EvId, EvRoom)[artifact_name("equipment_service"), wsp_id(WspId)];
				}
			};
			
			.my_name(AgentName);
			//?workspace_data("OrgSpecWSP", OrgId);
			
			makeArtifact("reorganization_planner", "ora4mas.nopl.reorg.ReorgBoard", [AgentName], ArtIdReorg)[wsp_id(OrgId)];
			
			.print("START REORG CONSTRUCT");
			
			// ----------------------------- pre OE changes ----------------------------- //
			// section for removing missions
			.findall( doing(Agent, Mission), commitment(Agent, Mission, "building_management_scheme"), BuildingManagementAgents);
			newSection("OEStop", RemMisSectionId)[artifact_id(ArtIdReorg)];
			for ( .member( doing(MgrAg, MgrMission), BuildingManagementAgents ) ) {
				leaveMission("OEStop", RemMisSectionId, "building_management_scheme", "regimentation", MgrAg, MgrMission, "building_management_scheme")[artifact_id(ArtIdReorg)];	
			};
		
			// --------------- //
			newSection("OEStop", RemSchSectionId)[artifact_id(ArtIdReorg)];
			removeScheme("OEStop", RemSchSectionId, "building_group", "obligation", building_manager, "building_management_scheme", "room_manager_group")[artifact_id(ArtIdReorg)];
		
			// --------------- //
			newSection("OEStop", LeaveRoleSectionId)[artifact_id(ArtIdReorg)];
			
			.findall( plays(Ag, Role, GrArt), 
					  play(Ag, Role, GrArt) & GrArt \== "reorg_group" & .member(AgName, PlaceHolderAgents) & 
					  .term2string(Ag, AgName), 
					  PlaceHolderRoles);
			
			.print("Place holder roles list is: ", PlaceHolderRoles);					  
			for ( .member( plays(AgName, RoleName, GrName), PlaceHolderRoles )) {
				.term2string(GrTerm, GrName);
				leaveRole("OEStop", LeaveRoleSectionId, GrName, "regimentation", AgName, RoleName, GrTerm)[artifact_id(ArtIdReorg)];
			};
			
			
			// --------------- //
			// remove the specialized groups that will no longer be used
			newSection("OEStop", RemGrpSectionId)[artifact_id(ArtIdReorg)];
			for ( .member( plays(A, R, G), PlaceHolderRoles )) {
				if (G \== "room_manager_group") {
					.print("::::::::: Adding action to delete group: ", G);
					removeGroup("OEStop", RemGrpSectionId, "building_group", "obligation", building_manager, G)[artifact_id(ArtIdReorg)];
				}
			};
			
			
			// ----------------------------- OS changes ----------------------------- //
			newSection("OSChange", OSChangeSectionId)[artifact_id(ArtIdReorg)];
			?system_group_artifacts(GrArtifacts);
			?system_scheme_artifacts(SchArtifacts);
			
			.concat(GrArtifacts, SchArtifacts, AllArtifacts);
			
			?disposed_group_artifacts(PlaceHolderRoles, DisposedGrArts);			
			.difference(AllArtifacts, DisposedGrArts, AllArtifactsClean);
			
			// first compute the new cardinalities at BB level
			for ( .member( plays(A, R, G), PlaceHolderRoles )) {
				if (G == "room_manager_group") {
					?specification( group_specification(Group, _, _, _))[artifact_name(_, G)];
					?role_card(Group, R, Min, Max);
					?role_card(Group, ConstrainedRoleType, ConstrMin, ConstrMax);
					
					NewMin = Min - 1;
					NewConstrMin = ConstrMin + 1;
					
					-role_card(Group, R, Min, Max);
					+role_card(Group, R, NewMin, Max);
					
					-role_card(Group, ConstrainedRoleType, ConstrMin, ConstrMax);
					+role_card(Group, ConstrainedRoleType, NewConstrMin, ConstrMax);
					
					// also do update on gui for room type
					?sim_art_id(SimArtId);
					?room_type(R, RoomType);
					
					updateRoomTypeData(RoomType, -1)[artifact_id(SimArtId)];
					updateRoomTypeData(ConstrainedRoomType, 1)[artifact_id(SimArtId)];
				}
				else {
					?subgroup_type(R, SubGroup);
					?subgroup_type(ConstrainedRoleType, ConstrainedSubGroup);
					
					?sub_group_card(building_group, SubGroup, Min, Max);
					?sub_group_card(building_group, ConstrainedSubGroup, ConstrMin, ConstrMax);
					
					NewMin = Min - 1;
					NewConstrMin = ConstrMin + 1;
					
					-sub_group_card(building_group, SubGroup, Min, Max);
					+sub_group_card(building_group, SubGroup, NewMin, Max);
					
					-sub_group_card(building_group, ConstrainedSubGroup, ConstrMin, ConstrMax);
					+sub_group_card(building_group, ConstrainedSubGroup, NewConstrMin, ConstrMax);
				}
			};
				
			// then send the actual OS commands
			for ( .member(ArtName, AllArtifactsClean) ) {
				for ( .member( plays(A, R, G), PlaceHolderRoles )) {
					if (G == "room_manager_group") {
						?specification( group_specification(Group, _, _, _))[artifact_name(_, G)];
						?role_card(Group, R, Min, Max);
						?role_card(Group, ConstrainedRoleType, ConstrMin, ConstrMax);
						
						changeRoleCardinality("OSChange", OSChangeSectionId, ArtName, room_manager_group, R, Min, Max)[artifact_id(ArtIdReorg)];
						changeRoleCardinality("OSChange", OSChangeSectionId, ArtName, room_manager_group, ConstrainedRoleType, ConstrMin, ConstrMax)[artifact_id(ArtIdReorg)];
					}
					else {
						?subgroup_type(R, SubGroup);
						?subgroup_type(ConstrainedRoleType, ConstrainedSubGroup);
						
						?sub_group_card(building_group, SubGroup, Min, Max);
						?sub_group_card(building_group, ConstrainedSubGroup, ConstrMin, ConstrMax);
						
						changeSubgroupCardinality("OSChange", OSChangeSectionId, ArtName, building_group, SubGroup, Min, Max)[artifact_id(ArtIdReorg)];
						changeSubgroupCardinality("OSChange", OSChangeSectionId, ArtName, building_group, ConstrainedSubGroup, ConstrMin, ConstrMax)[artifact_id(ArtIdReorg)];		
					}
				};
				
			};
			
			// ----------------------------- post OE changes ----------------------------- //
			?subgroup_type(ConstrainedRoleType, ConstrSubGroup);
			
			newSection("OEStart", AddGrpSectionId)[artifact_id(ArtIdReorg)];
			for ( .member(Ag, PlaceHolderAgents) ) {
				.concat("", ConstrSubGroup, "_", Ag, GrArtName);
				addGroup("OEStart", AddGrpSectionId, "building_group", "obligation", building_manager, GrArtName, ConstrSubGroup)[artifact_id(ArtIdReorg)];	
			}
		
			// --------------- //
			newSection("OEStart", PlayRoleSectionId)[artifact_id(ArtIdReorg)];
			for ( .member(Ag, PlaceHolderAgents) ) {
				.concat("", ConstrSubGroup, "_", Ag, GroupArtName);
				
				playRole("OEStart", PlayRoleSectionId, "room_manager_group", "regimentation", Ag, ConstrainedRoleType, room_manager_group)[artifact_id(ArtIdReorg)];
				playRole("OEStart", PlayRoleSectionId, GroupArtName, "regimentation", Ag, ConstrainedRoleType, GroupArtName)[artifact_id(ArtIdReorg)];
			}
			
			// --------------- //
			newSection("OEStart", AddSchSectionId)[artifact_id(ArtIdReorg)];
			addScheme("OEStart", AddSchSectionId, "building_group", "obligation", building_manager, "building_management_scheme", "room_manager_group")[artifact_id(ArtIdReorg)];
		
			goalAchieved(practice_des)[artifact_name(Scheme), wsp_id(OrgId)];
		}.
		//goalAchieved(practice_des)[artifact_name(Scheme), wsp_id(OrgId)].
		
-!construct_reorg_plan_req_growing(Scheme, ReqType, Hour, NextExp, RemUnitSlots, NumAgree, NumObs)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure constructing reorg plan req growing: -- ", ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
	
@cancel_reorg_plan[atomic]
+!cancel_reorg(Scheme)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(practice_des)[artifact_name(Scheme), wsp_id(OrgId)];
		goalAchieved(selection)[artifact_name(Scheme), wsp_id(OrgId)];
		goalAchieved(stop_current_mas)[artifact_name(Scheme), wsp_id(OrgId)];
		goalAchieved(change_mas)[artifact_name(Scheme), wsp_id(OrgId)];
		goalAchieved(instantiate_new_mas)[artifact_name(Scheme), wsp_id(OrgId)].