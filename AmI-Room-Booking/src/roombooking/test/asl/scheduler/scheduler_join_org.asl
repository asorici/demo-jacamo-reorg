// ================ Plans for joining organization specification workspace and role assuming =============== //

+!join_work
	<-	!join_workspaces.

+!join_workspaces
	<-	!join_main_workspace;
		!join_org_workspace.


+!join_main_workspace
	<-	joinWorkspace("RequestNotificationWSP", ReqWspId);
		+workspace_data("RequestNotificationWSP", ReqWspId);
		
		lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
		+sim_art_id(SimArtId);
		cartago.new_array("java.lang.String[]",["sim_init", "currentDateUnit"],Array);
     	cartago.new_obj("cartago.events.ObsPropertyFilter",[Array],Filter);
		focus(SimArtId, Filter);
		
		lookupArtifact("schedule_service", SchedArtId)[wsp_id(ReqWspId)];
		focus(SchedArtId)[wsp_id(ReqWspId)];
		
		lookupArtifact("request_service", ReqArtId)[wsp_id(ReqWspId)];
		focus(ReqArtId)[wsp_id(ReqWspId)].
		
-!join_main_workspace
	<-	.wait(100);
		!join_main_workspace.

+!join_org_workspace
	<-	joinWorkspace("OrgSpecWSP", OrgWspId);
		+workspace_data("OrgSpecWSP", OrgWspId);
		
		!join_building_management;
		!follow_room_manager_group.

-!join_org_workspace
	<-	.wait(100);
		!join_org_workspace.


+!join_building_management
	<-	!join_building_group(scheduler).

-!join_building_management
	<-	.wait(100);
		!join_building_management.

//@join_building_group_plan[atomic]
+!join_building_group(RoleName)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("building_group", GrId)[wsp_id(OrgId)]; 
		adoptRole(RoleName)[artifact_id(GrId)];
		focus(GrId)[wsp_id(OrgId)].
		
-!join_building_group(RoleName)
	<-	.wait(100);
		!join_building_group(RoleName).

//@follow_room_manager_group_plan[atomic]
+!follow_room_manager_group
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("room_manager_group", RoomGrId)[wsp_id(OrgId)];
		focus(RoomGrId)[wsp_id(OrgId)].

-!follow_room_manager_group
	<-	.wait(100);
		!follow_room_manager_group.
