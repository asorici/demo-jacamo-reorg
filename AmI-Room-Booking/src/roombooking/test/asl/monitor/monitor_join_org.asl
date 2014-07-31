// ================ Plans for joining organization specification workspace and role assuming =============== //

+!join_work
	<-	!join_workspaces.
			

+!join_workspaces
	<-	!join_main_workspace;
		!join_org_workspace.


+!join_main_workspace
	<-	joinWorkspace("RequestNotificationWSP", WspId);
		+workspace_data("RequestNotificationWSP", WspId);
		
		lookupArtifact("simulation", SimArtId);
		+sim_art_id(SimArtId);
		cartago.new_array("java.lang.String[]",["sim_init", "currentDateUnit"],Array);
     	cartago.new_obj("cartago.events.ObsPropertyFilter",[Array],Filter);
		focus(SimArtId, Filter);
		
		lookupArtifact("monitor_service", MArtId);
		focus(MArtId);
		
		lookupArtifact("equipment_service", EArtId);
		focus(EArtId).
		
-!join_main_workspace
	<-	.wait(100);
		!join_main_workspace.


+!join_org_workspace
	<-	joinWorkspace("OrgSpecWSP", WspId);
		+workspace_data("OrgSpecWSP", WspId);
		
		!join_building_management;
		!join_reorganization_management;
		!follow_room_manager_group.

-!join_org_workspace
	<-	.wait(100);
		!join_org_workspace.
		

+!join_building_management
	<-	!join_building_group(monitor).

-!join_building_management
	<-	.wait(100);
		!join_building_management.
		

+!join_building_group(RoleName)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("building_group", GrId)[wsp_id(OrgId)]; 
		adoptRole(RoleName)[artifact_id(GrId)];
		focus(GrId)[wsp_id(OrgId)].
		
-!join_building_group(RoleName)
	<-	.wait(100);
		!join_building_group(RoleName).
		

+!join_reorganization_management
	<-	//?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
	
		//!join_reorganization_group("reorg_group", org_manager);
		//!join_reorganization_group("reorg_group", historian);
		//!join_reorganization_group("reorg_group", selector);
		!join_reorganization_group("reorg_group", monitor).

-!join_reorganization_management
	<-	.wait(100);
		!join_reorganization_management.
		
//@join_reorg_group_plan[atomic]
+!join_reorganization_group(GroupName, RoleName)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		
		lookupArtifact(GroupName, GrId)[wsp_id(OrgId)]; 
		adoptRole(RoleName)[artifact_id(GrId)];
		focus(GrId)[wsp_id(OrgId)].
		
-!join_reorganization_group(GroupName, RoleName)
	<-	.wait(100);
		!join_reorganization_group(GroupName, RoleName).

//@follow_room_mgr_plan[atomic]
+!follow_room_manager_group
	<-	?workspace_data("OrgSpecWSP", OrgId);
		lookupArtifact("room_manager_group", RoomGrId)[wsp_id(OrgId)];
		focus(RoomGrId)[wsp_id(OrgId)].

-!follow_room_manager_group
	<-	.wait(100);
		!follow_room_manager_group.		
