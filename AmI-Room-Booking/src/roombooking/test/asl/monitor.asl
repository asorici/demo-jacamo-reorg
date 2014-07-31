{ include("common.asl") }
{ include("monitor/monitor_join_org.asl") }
{ include("monitor/monitor_reorg.asl") }


/*
	Initial beliefs and goals
*/
known_groups(["building_group", "room_manager_group", "reorg_group"]).
known_schemes(["building_management_scheme", "reorganization_scheme"]).

+init_complete
	<-	!join_work.
		
// =================================== Plans for the actual responsabilities ============================== //

// ############### Monitoring ############### //

+!monitor_request_trend_growing[scheme(Scheme)]
	<-	//goalAchieved(monitor_request_trend_growing)[artifact_name(Scheme)].
		?goalState("reorganization_scheme", monitor_request_trend_growing, _, _, Status);
		if ( Status == feasible ) {
		//if ( Status == enabled ) {
			//.print("Monitor does not perform short term request trend growing for this test period.");
			+monitor_req_growing_flag;
		}.
	
+!monitor_request_trend_dropping[scheme(Scheme)]
	<-	//goalAchieved(monitor_request_trend_dropping)[artifact_name(Scheme)].
		?goalState("reorganization_scheme", monitor_request_trend_dropping, _, _, Status);
		if ( Status == feasible ) {
		//if ( Status == enabled ) {
			//.print("Monitor does not perform request trend dropping for this test period.");
			+monitor_req_dropping_flag;
		}.
	
+!monitor_energy[scheme(Scheme)]
	<-	//goalAchieved(monitor_energy)[artifact_name(Scheme)].
		?goalState("reorganization_scheme", monitor_energy, _, _, Status);
		if ( Status == feasible ) {
		//if ( Status == enabled ) {
			//.print("Monitor does not perform energy overview for this test period.");
			+monitor_energy_flag;
		};
		
		// announce init complete for monitor
      	//?sim_art_id(SimArtId);
      	?workspace_data("RequestNotificationWSP", ReqWspId);
      	lookupArtifact("simulation", SimArtId)[wsp_id(ReqWspId)];
      	markInitComplete[artifact_id(SimArtId)].
