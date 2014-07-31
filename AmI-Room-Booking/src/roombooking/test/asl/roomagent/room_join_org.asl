// ================ Plans for joining organization specification workspace and role assuming =============== //

+!join_work
	<-	!join_workspaces.

+!join_workspaces
	<-	.my_name(Me);
		.concat("", Me, "_wsp", RoomWspName);
		createWorkspace(RoomWspName);
		joinWorkspace(RoomWspName, WspId);
		+workspace_data(RoomWspName, WspId);
		
		joinWorkspace("RequestNotificationWSP", ReqWspId);
		+workspace_data("RequestNotificationWSP", ReqWspId);
		
		!join_org_workspace;
		
		!create_room_artifact.
		

//@create_room_artifact_plan[atomic]
+!create_room_artifact
	:	.my_name(Me) & play(Me, MyRole, "room_manager_group") & room_type(MyRole, Type)
	<-	
		?workspace_data("RequestNotificationWSP", ReqWspId);
		lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
		+sim_art_id(SimArtId);
		cartago.new_array("java.lang.String[]",["sim_init", "currentDateUnit"],Array);
     	cartago.new_obj("cartago.events.ObsPropertyFilter",[Array],Filter);
		focus(SimArtId, Filter)[wsp_id(ReqWspId)];
		
		?initial_assignment(Me, _, RoomId);
		?room_spec(RoomId, Format, NumSeats, EqList);
		
		.concat("", Me, "_wsp", RoomWspName);
		?workspace_data(RoomWspName, RoomWspId);
		
		cartago.new_obj("roombooking.core.Room", [RoomId, Type, Format, NumSeats, Me], Room);
		makeArtifact("room_state", "roombooking.artifacts.RoomStateArtifact", [Room], RArtId)[wsp_id(RoomWspId)];
		+room_artifact_id(RArtId);
		focus(RArtId)[wsp_id(RoomWspId)];
		
		makeArtifact("utilities_state", "roombooking.artifacts.ArduinoUtilities", [Room, "AmI-Room-Booking/arduino-utilities-config.xml"], ArdUtilId)[wsp_id(RoomWspId)];
		+utilities_artifact_id(ArdUtilId);
		focus(ArdUtilId)[wsp_id(RoomWspId)];
		
		
		// build room inventory 
		!build_room_inventory(RArtId, EqList);
		
		lookupArtifact("room_state", RArtId1)[wsp_id(RoomWspId)];
		lookupArtifact("schedule_service", SchedServId)[wsp_id(ReqWspId)];
		linkArtifacts(RArtId1, "schedule-management", SchedServId)[wsp_id(RoomWspId)];
		+sched_artifact_id(SchedServId);
		
		lookupArtifact("room_state", RArtId2)[wsp_id(RoomWspId)];
		lookupArtifact("equipment_service", EqServId)[wsp_id(ReqWspId)];
		linkArtifacts(RArtId2, "equipment-management", EqServId)[wsp_id(RoomWspId)];
		+eq_artifact_id(EqServId);
		
		lookupArtifact("room_state", RArtId3)[wsp_id(RoomWspId)];
		lookupArtifact("monitor_service", MonServId)[wsp_id(ReqWspId)];
		linkArtifacts(RArtId3, "monitor-management", MonServId)[wsp_id(RoomWspId)];
		+monitor_artifact_id(MonServId);
		
		lookupArtifact("room_state", RArtId4)[wsp_id(RoomWspId)];
		lookupArtifact("request_service", ReqServId)[wsp_id(ReqWspId)];
		linkArtifacts(RArtId4, "request-management", ReqServId)[wsp_id(RoomWspId)];
		+req_artifact_id(ReqServId);
		
		?sched_artifact_id(SchedServId);
		initRoomSchedule(Room)[artifact_id(SchedServId)].

+!lookup_system_artifacts
	<-	// link my room to all the other needed system artifacts and the simulation artifact
		?workspace_data("RequestNotificationWSP", ReqWspId);
		
		lookupArtifact("schedule_service", SchedServId)[wsp_id(ReqWspId)];
		+sched_artifact_id(SchedServId);
		
		lookupArtifact("equipment_service", EqServId)[wsp_id(ReqWspId)];
		+eq_artifact_id(EqServId);
		
		lookupArtifact("monitor_service", MonServId)[wsp_id(ReqWspId)];
		+monitor_artifact_id(MonServId);
		
		lookupArtifact("request_service", ReqServId)[wsp_id(ReqWspId)];
		+req_artifact_id(ReqServId);
		
		lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
		+sim_art_id(SimArtId);
		cartago.new_array("java.lang.String[]",["sim_init", "currentDateUnit"],Array);
     	cartago.new_obj("cartago.events.ObsPropertyFilter",[Array],Filter);
		focus(SimArtId, Filter).
		
+!build_link_to_system_artifacts
	<-	//.wait(5000);
		.my_name(Me);
		.concat("", Me, "_wsp", RoomWspName);
		
		?workspace_data(RoomWspName, RoomWspId);
		?room_artifact_id(RArtId);
		
		?sched_artifact_id(SchedServId);
		linkArtifacts(RArtId, "schedule-management", SchedServId)[wsp_id(RoomWspId)];
		
		?eq_artifact_id(EqServId);
		linkArtifacts(RArtId, "equipment-management", EqServId)[wsp_id(RoomWspId)];
		
		?monitor_artifact_id(MonServId);
		linkArtifacts(RArtId, "monitor-management", MonServId)[wsp_id(RoomWspId)];
		
		?req_artifact_id(ReqServId);
		linkArtifacts(RArtId, "request-management", ReqServId)[wsp_id(RoomWspId)].
		
		// start event counting in room artifact
		//startEventCounting[artifact_id(RArtId)].
		
		
-!create_room_artifact[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Failure building room artifact -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
		
// build inventory based on the simulate_room_equipment descriptions which could be automatically generated.

+!build_room_inventory(RArtId, EqList)
	<-	.my_name(Me);
		?workspace_data("RequestNotificationWSP", ReqWspId);
		lookupArtifact("equipment_service", EqServId)[wsp_id(ReqWspId)];
		
		for ( .member(EqId, EqList) ) {
			getEquipment(EqId, Equipment)[artifact_id(EqServId)];
			addEquipment(Equipment)[artifact_id(RArtId)];
		}.


//@join_main_wsp[atomic]
+!join_main_workspace
	<-	joinWorkspace("RequestNotificationWSP", WspId);
		+workspace_data("RequestNotificationWSP", WspId).
		
-!join_main_workspace
	<-	.wait(1000);
		!join_main_workspace.


//@join_org_wsp[atomic]
+!join_org_workspace
	<-	joinWorkspace("OrgSpecWSP", WspId);
		+workspace_data("OrgSpecWSP", WspId);
		
		!join_building_management;
		!join_reorganization_management.
		
-!join_org_workspace
	<-	.wait(100);
		!join_org_workspace.


+!join_building_management
	<-	
		//!join_building_group;
		!join_room_manager_group;
		!join_specialized_group.

-!join_building_management
	<-	.wait(100);
		!join_building_management.

+!join_building_group
	<-	?workspace_data("OrgSpecWSP", OrgId);
		
		lookupArtifact("building_group", BGrId)[wsp_id(OrgId)];
		focus(BGrId)[wsp_id(OrgId)].

-!join_building_group
	<-	.wait(100);
		!join_building_group.

+!join_room_manager_group
	<-	.my_name(Me);
		.send(building_manager, askOne, initial_assignment(Me, SubGrType, _), initial_assignment(Me, SubGrType, _));
		?workspace_data("OrgSpecWSP", OrgId);
		?subgroup_type(RoleType, SubGrType);
		
		lookupArtifact("room_manager_group", RoomGrId)[wsp_id(OrgId)];
		adoptRole(RoleType)[artifact_id(RoomGrId)];
		focus(RoomGrId)[wsp_id(OrgId)].

-!join_room_manager_group
	<-	.wait(100);
		!join_room_manager_group.



+!join_specialized_group
	<-	.my_name(Me);
		.send(building_manager, askOne, initial_assignment(Me, SubGrType, _), initial_assignment(Me, SubGrType, _));
		?workspace_data("OrgSpecWSP", OrgId);
		?subgroup_type(RoleType, SubGrType);
		
		.concat("", SubGrType, "_", Me, SubGrArtName);
		lookupArtifact(SubGrArtName, ArtId)[wsp_id(OrgId)];
		adoptRole(RoleType)[artifact_id(ArtId)];
		focus(ArtId)[wsp_id(OrgId)].
				
-!join_specialized_group
	<-	.wait(100);
		!join_specialized_group.



+!join_reorganization_management
	<-	//?workspace_data("OrgSpecWSP", WspId);
		//cartago.set_current_wsp(WspId)		
		!join_reorganization_group("reorg_group", org_participant).

-!join_reorganization_management
	<-	.wait(100);
		!join_reorganization_management.

//@join_reorganization_group_plan[atomic]
+!join_reorganization_group(GroupName, RoleName)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		
		lookupArtifact(GroupName, GrId)[wsp_id(OrgId)]; 
		adoptRole(RoleName)[artifact_id(GrId)];
		focus(GrId)[wsp_id(OrgId)].
		
-!join_reorganization_group(GroupName, RoleName)
	<-	.wait(100);
		!join_reorganization_group(GroupName, RoleName).		
