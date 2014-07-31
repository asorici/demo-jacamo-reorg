{ include("common.asl") }
{ include("manager/manager_reorg.asl") }
{ include("manager/manager_reorg_designer.asl") }

/*
	================= Initial beliefs ================= 
*/
init_phase_building_group.
init_phase_room_manager_group.
init_phase_reorg_group.

/*
initial_assignment(room_agent1, teaching_group).
initial_assignment(room_agent2, teaching_group).
initial_assignment(room_agent3, teaching_group).
initial_assignment(room_agent4, teaching_group).
initial_assignment(room_agent5, meeting_group).
initial_assignment(room_agent6, meeting_group).
initial_assignment(room_agent7, meeting_group).
initial_assignment(room_agent8, meeting_group).
initial_assignment(room_agent9, brainstorm_group).
*/

room_manager_agents([room_agent1, room_agent2, room_agent3, room_agent4, 
			   room_agent5, room_agent6, room_agent7, room_agent8, room_agent9]).
			   
system_agents([scheduler, monitor, room_agent1, room_agent2, room_agent3, room_agent4, 
			   room_agent5, room_agent6, room_agent7, room_agent8, room_agent9]).

// common repository for organization artifacts
system_group_artifacts(["building_group", "room_manager_group"]).

system_scheme_artifacts(["building_management_scheme"]).


/*
	================= Rules =================
*/

// rules for determining sub-group specifications
sub_group_cardinality(Group, teaching_group, Min, Max) :- 
	specification( group_specification(Group, _, SubGroupList, _) ) &  
	.member(teaching_group(Min, Max), SubGroupList).
	//.concat("", SubGroup, "(Min, Max\)", StrTerm) & .term2string(Term, StrTerm) & .member(Term, SubGroupList).  

sub_group_cardinality(Group, meeting_group, Min, Max) :- 
	specification( group_specification(Group, _, SubGroupList, _) ) &  
		.member(meeting_group(Min, Max), SubGroupList).

sub_group_cardinality(Group, brainstorm_group, Min, Max) :- 
	specification( group_specification(Group, _, SubGroupList, _) ) &  
		.member(brainstorm_group(Min, Max), SubGroupList).

role_cardinality(Group, Role, Min, Max) :-	
	specification( group_specification(Group, RoleList, _, _) ) &
		.member( role(Role, Min, Max, _, _), RoleList ).


/*
	================= Goals =================
*/
!create_workspaces.


/*
	================= Plans =================
*/

+!create_workspaces
	<-	createWorkspace("RequestNotificationWSP");
		joinWorkspace("RequestNotificationWSP", RNWspId);
		+workspace_data("RequestNotificationWSP", RNWspId);
		!create_main_artifacts;
		
		createWorkspace("OrgSpecWSP");
		joinWorkspace("OrgSpecWSP", OrgId);
		+workspace_data("OrgSpecWSP", OrgId);
		!create_org_artifacts;
		
		!announce_initiation_complete.
		

+!create_main_artifacts
	<-	// build simultation artifact
		makeArtifact("simulation", "roombooking.artifacts.SimulationArtifact", ["AmI-Room-Booking/src/ami-room-booking-sim.xml"], SimArtId);
		+sim_art_id(SimArtId);
		cartago.new_array("java.lang.String[]",["sim_init", "currentDateUnit"],Array);
     	cartago.new_obj("cartago.events.ObsPropertyFilter",[Array],Filter);
		focus(SimArtId, Filter);
		
		// build request service and user management
		makeArtifact("request_service", "roombooking.artifacts.RequestServiceArtifact", [], ReqArtId);
		focus(ReqArtId);
		
		// build schedule service
		makeArtifact("schedule_service", "roombooking.artifacts.ScheduleStateArtifact", [], SchedArtId);
		
		// build monitoring and equipment state services
		makeArtifact("monitor_service", "roombooking.artifacts.MonitorArtifact", [], MArtId);
		makeArtifact("equipment_service", "roombooking.artifacts.EquipmentStateArtifact", [], EArtId).

-!create_main_artifacts[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure building main artifacts -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

+!create_org_artifacts
	<-	!create_building_organization;
		!create_reorganization_group.
		
		//!create_management_scheme;
		//!create_reorganization_scheme.

// ======================================= Building Organization ========================================= //

+!create_building_organization
	<-	.my_name(Me);
	
		makeArtifact("building_group", "ora4mas.nopl.GroupBoard", 
					 ["AmI-Room-Booking/src/ami-room-booking-os.xml", building_group, false, true ], BGrArtId);
		setOwner(Me)[artifact_name("building_group")];
		focus(BGrArtId);
		.print("1. building group created");
		
		// build the subgroups - room_manager_group
		makeArtifact("room_manager_group", "roombooking.artifacts.RoomGroupBoard", 
						["AmI-Room-Booking/src/ami-room-booking-os.xml", room_manager_group, false, true ], RGrArtId);
		setOwner(Me)[artifact_name("room_manager_group")];
		focus(RGrArtId);
		//setParentGroup(building_group)[artifact_id(RGrArtId)];
		.print("1.1 room_manager_group created");
		
		// build the subgroups - teaching_group
		!build_specialized_subgroups(teaching_group);
		
		// build the subgroups - meeting_group
		!build_specialized_subgroups(meeting_group);
		
		// build the subgroups - brainstorm_group
		!build_specialized_subgroups(brainstorm_group);
		
		// build building management scheme
		makeArtifact("building_management_scheme", "ora4mas.nopl.SchemeBoard",["AmI-Room-Booking/src/ami-room-booking-os.xml", manage_building_sch, false, true ],SchArtId);
      	setOwner(Me)[artifact_name("building_management_scheme")];
      	focus(SchArtId);
      	
      	// store role and subgroup cardinalities for easy manipulation during reorganization
      	!store_cardinality_specifications;
      	
		// wait for agents to adopt their roles
		//.wait(1000);
		adoptRole(building_manager)[artifact_id(BGrArtId)].

-!create_building_organization[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure building organization -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).


+!build_specialized_subgroups(SubGrType)
	<-	.my_name(Me);
		.findall( Ag, initial_assignment(Ag, SubGrType, _), AgNames);
		for ( .member(AgName, AgNames) ) {
			.concat("", SubGrType, "_", AgName, SubGrArtName);
			makeArtifact(SubGrArtName, "ora4mas.nopl.GroupBoard", 
						["AmI-Room-Booking/src/ami-room-booking-os.xml", SubGrType, false, true ], GrArtId);
			setOwner(Me)[artifact_name(SubGrArtName)];
			focus(GrArtId);
			//setParentGroup(building_group)[artifact_id(GrArtId)];
			
			?system_group_artifacts(SystemGroupList);
			.concat(SystemGroupList, [SubGrArtName], NewSystemGroupList);
			-+system_group_artifacts(NewSystemGroupList);
			
			.print("1.2.X ", SubGrType, " created: ", SubGrArtName);
		}.


+!store_cardinality_specifications
	<-	// first store all room_manager_group role cardinalities
		?specification( group_specification(room_manager_group, RoleList, _, _) );
		for ( .member(role(R, Min, Max, _, _), RoleList) ) {
			+role_card(room_manager_group, R, Min, Max);
		};
		
		// store all specialized sub group cardinalities
		
		MainGroups = [building_group, room_manager_group, reorg_group];
		.findall(G, 
				 specification( group_specification(G, _, _, _)) & not .member(G, MainGroups),
				 SubGroups
		);
		
		for ( .member(SubGr, SubGroups) ) {
			?sub_group_cardinality(building_group, SubGr, Min, Max);
			+sub_group_card(building_group, SubGr, Min, Max);
		}.


// ======================================= Reorganization Organization ========================================= //
     			
+!create_reorganization_group
	<-	.my_name(Me);
		?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		makeArtifact("reorg_group", "ora4mas.nopl.GroupBoard",
						["AmI-Room-Booking/src/reorg-os.xml", reorg_group, false, true ], ReorgGrArtId)[wsp_id(WspId)];
		setOwner(Me)[artifact_id(ReorgGrArtId), wsp_id(WspId)];
		focus(ReorgGrArtId)[wsp_id(WspId)];
		adoptRole(org_manager)[artifact_id(ReorgGrArtId)];
		adoptRole(historian)[artifact_id(ReorgGrArtId)];
		adoptRole(selector)[artifact_id(ReorgGrArtId)];
		
		
		// build first reorganization scheme 
      	makeArtifact("reorganization_scheme", "ora4mas.nopl.SchemeBoard",["AmI-Room-Booking/src/reorg-os.xml", reorganization, false, true ], ReorgSchArtId)[wsp_id(WspId)];
      	setOwner(Me)[artifact_id(ReorgSchArtId)];
      	focus(ReorgSchArtId)[wsp_id(WspId)];
		
		.print("1.3 reorg group created").


// ---------- when all working artifacts have been created signal initiation complete ----------- //

+!announce_initiation_complete
	<-	?system_agents(SystemAgents);
		.print("Sending system agents: ", SystemAgents, " the init_complete signal.");
		.send(SystemAgents, tell, init_complete).

// ------------------ when all groups are well formed add the schemes to them ------------------- //
//@plan_formation_status1[atomic]
+formationStatus(ok)[artifact_name(_, "building_group")] 
	:	init_phase_building_group & schemes(SchemeList)[artifact_name(_, "building_group")] 
				& not .member("building_management_scheme", SchemeList)
	<-	.print("Add building management scheme to building group");
		-init_phase_building_group;
		
		?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		
      	addScheme("building_management_scheme")[artifact_name("building_group"), wsp_id(WspId)];
      	.print("Scheme building management is linked to building group").
     	//commitMission(building_management)[artifact_id(SchArtId)].

//@plan_formation_status2[atomic]
+formationStatus(ok)[artifact_name(_, "room_manager_group")] 
	:	init_phase_room_manager_group & schemes(SchemeList)[artifact_name(_, "room_manager_group")] 
				& not .member("building_management_scheme", SchemeList)
	<-	.print("Add building management scheme to room_manager_group");
		-init_phase_room_manager_group;
		
		?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId);
		
      	addScheme("building_management_scheme")[artifact_name("room_manager_group"), wsp_id(WspId)];
      	.print("Scheme building management is linked to room_manager_group").

//@plan_formation_status3[atomic]
+formationStatus(ok)[artifact_name(_, "reorg_group")]
	:	init_phase_reorg_group & schemes(SchemeList)[artifact_name(_, "reorg_group")] 
				& not .member("reorganization_scheme", SchemeList)
	<-	.print("Add reorganization scheme to reorg_group");
		-init_phase_reorg_group;
		
		//if ( not formationStatus(ok)[artifact_name(_, "building_group")] ) {
		//	.wait({+formationStatus(ok)[artifact_name(_, "building_group")]});
		//};
		
		?check_goal_state("building_management_scheme",manage_building,satisfied);
		?workspace_data("OrgSpecWSP", WspId);
		
		//.wait(1000);
		
      	addScheme("reorganization_scheme")[artifact_name("reorg_group"), wsp_id(WspId)];
      	.print("Scheme reorganization_scheme is linked to reorg_group");
      	
      	// announce init complete for building_manager
      	//?sim_art_id(SimArtId);
      	?workspace_data("RequestNotificationWSP", ReqWspId);
      	lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
      	markInitComplete[artifact_id(SimArtId)].
      	
	
+?check_goal_state(SchemeName, GoalName, State)
	:	goalState(SchemeName, GoalName, _, _, State)
	<-	true.

+?check_goal_state(SchemeName, GoalName, State)
	:	not goalState(SchemeName, GoalName, _, _, State)
	<-	.wait(1000);
		?check_goal_state(SchemeName, GoalName, State).
	