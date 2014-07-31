//{ include("simulate_room_equipment.asl") }

/* Common code for the organization's agents */

// == standard measurement for duration == //
hour_length(10).

// == monitor request message types == //
request_message_type(meeting_room_manager, meeting_request).
request_message_type(teaching_room_manager, teaching_request).
request_message_type(brainstorm_room_manager, brainstorm_request).

room_type(meeting_room_manager, "meetingRoom").
room_type(teaching_room_manager, "teachingRoom").
room_type(brainstorm_room_manager, "brainstormRoom").

event_type(meeting_room_manager, "meetingEvent").
event_type(teaching_room_manager, "teachingEvent").
event_type(brainstorm_room_manager, "brainstormEvent").

subgroup_type(meeting_room_manager, meeting_group).
subgroup_type(teaching_room_manager, teaching_group).
subgroup_type(brainstorm_room_manager, brainstorm_group).



// == maintenance list of missions to commit to for each agent - the ones that don't get signaled by the artifact == //
commit_to(scheduler, [schedule_management], "building_management_scheme").
commit_to(teaching_room_manager, [teaching_room_management], "building_management_scheme").
commit_to(meeting_room_manager, [meeting_room_management], "building_management_scheme").
commit_to(brainstorm_room_manager, [brainstorm_room_management], "building_management_scheme").

achieve_maintenance(teaching_room_management, 
					[manage_teaching_events, monitor_teaching_equipment, monitor_teaching_energy],
					"building_management_scheme").

achieve_maintenance(meeting_room_management, 
					[manage_meeting_events, monitor_meeting_equipment, monitor_meeting_energy],
					"building_management_scheme").

achieve_maintenance(brainstorm_room_management, 
					[manage_brainstorm_events, monitor_brainstorm_equipment, monitor_brainstorm_energy],
					"building_management_scheme").
					
achieve_maintenance(schedule_management, 
					[manage_event_schedule_request, manage_event_delete_request, 
					manage_event_modification_request],
					"building_management_scheme").
					

// ===== keep focused on schemes my groups are responsible for ===== //
+schemes(L)[artifact_name(_, GrArtName)]
	:	.my_name(Me) & play(Me, _, GrArtName)
   	<- 	for ( .member(S,L) ) {
         	?workspace_data("OrgSpecWSP", OrgId);
   			//cartago.set_current_wsp(OrgId);
   			
   			lookupArtifact(S,ArtId)[wsp_id(OrgId)];
        	focus(ArtId)[wsp_id(OrgId)];
        	
      		!commit_to_maintenance_missions(S);
      	}.

-schemes(L)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<- .print("Error scheme addition -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).	

//@find_scheme_plan[atomic]
+!find_scheme_artifact(S)
	<-	true.	

-!find_scheme_artifact(S)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("Error finding scheme ", S, " -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

+!quit_mission(M,S)
   <- .print("leaving my mission ",M," on ",S,"....");
      leaveMission(M)[artifact_name(S)].

@commit_maintenance[atomic]
+!commit_to_maintenance_missions(Scheme)
	:	.my_name(MyName) & play(MyName, Role, _) & commit_to(Role, MissionList, Scheme) 
	<-	?workspace_data("OrgSpecWSP", OrgId);
   		//cartago.set_current_wsp(OrgId);
		
		for ( .member(Mission, MissionList) ) {
			// see if i have to commit to the mission
			if (commitment(MyName, Mission, Scheme)) {
				.print("I am already commited to maintenance mission ",Mission, " on ", Scheme);
			}
			else {
				.print("I am obliged to commit to maintenance mission ",Mission, " on ", Scheme);
				commitMission(Mission)[artifact_name(Scheme), wsp_id(OrgId)];
			}	
			
			?achieve_maintenance(Mission, GoalList, Scheme);
			for ( .member(Goal, GoalList) ) {
				!Goal[scheme(Scheme)];
				//goalAchieved(Goal)[artifact_name(Scheme)];
			}
		}.
		
-!commit_to_maintenance_missions(Scheme)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("Error commit maintenance missions -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

+!commit_to_maintenance_missions(Scheme)
	<-	true.
	
	
// =============== common plans to follow obligations ============== //
@po[atomic]
+obligation(Ag,Norm,committed(Ag,Mission,Scheme),Deadline)
	:	.my_name(Ag) & not commitment(Ag, Mission, Scheme)
   	<- 	//.print("I am obliged to commit to ",Mission," on ",Scheme);
      	?workspace_data("OrgSpecWSP", OrgId);
      	commitMission(Mission)[artifact_name(Scheme), wsp_id(OrgId)].

+obligation(Ag,Norm,committed(Ag,Mission,Scheme),Deadline)
	:	.my_name(Ag) & commitment(Ag, Mission, Scheme)
   	<- 	//.print("I am already committed to ",Mission," on ",Scheme).
   		true.


+obligation(Ag,Norm,achieved(Scheme,Goal,Ag),Deadline)
    : 	.my_name(Ag)
   	<- 	//.print("I have to achieve goal ",Goal);
      	!Goal[scheme(Scheme)].
      
+obligation(Ag,Norm,What,Deadline)  
	:	.my_name(Ag)
	<- 	.print("I am obliged to ",What,", but I don't know what to do!").
      
+?getWorkspace(WorkspaceName, WspId)
	:	workspace_data(WorkspaceName, WspId)
	<-	true.
	
+?getWorkspace(WorkspaceName, WspId)
	:	not workspace_data(WorkspaceName, _)
	<-	joinWorkspace(WorkspaceName, WspId);
		+workspace_data(WorkspaceName, WspId).
