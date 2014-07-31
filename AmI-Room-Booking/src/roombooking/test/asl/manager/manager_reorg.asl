// ================================== Org manager obligations ================================== //
known_groups(["building_group", "room_manager_group", "reorg_group"]).
known_schemes(["building_management_scheme", "reorganization_scheme"]).

// ======================================== SELECTION PHASE =========================================== // 
// selection goal - as selector I will have to judge among the different variants submitted by the agents
+!selection[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		goalAchieved(selection)[artifact_name(Scheme), wsp_id(OrgId)].



// ===================================== IMPLEMENTATION PHASE ========================================= //
// implementation goal - actual submission of the reorganization design - hardest part
// ########################################################### //
+!focus_on_entire_org
	<-	//.send(building_manager, askOne, system_group_artifacts(GrArtifacts), system_group_artifacts(GrArtifacts));
		//.send(building_manager, askOne, system_scheme_artifacts(SchArtifacts), system_scheme_artifacts(SchArtifacts));
		?system_group_artifacts(GrArtifacts);
		?system_scheme_artifacts(SchArtifacts);
		.concat(GrArtifacts, SchArtifacts, AllArtifacts);
		
		?known_groups(KnownGroups);
		?known_schemes(KnownSchems);
		.concat(KnownGroups, KnownSchems, KnownAll);
		.difference(AllArtifacts, KnownAll, UnknownAll);
		
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		for ( .member(ArtName, UnknownAll) ) {
			lookupArtifact(ArtName, ArtId)[wsp_id(OrgId)];
			focus(ArtId)[wsp_id(OrgId)];
		}.
	
+!defocus_unnecessary_org
	<-	//.send(building_manager, askOne, system_group_artifacts(GrArtifacts), system_group_artifacts(GrArtifacts));
		//.send(building_manager, askOne, system_scheme_artifacts(SchArtifacts), system_scheme_artifacts(SchArtifacts));
		?system_group_artifacts(GrArtifacts);
		?system_scheme_artifacts(SchArtifacts);
		.concat(GrArtifacts, SchArtifacts, AllArtifacts);
		
		?known_groups(KnownGroups);
		?known_schemes(KnownSchems);
		.concat(KnownGroups, KnownSchems, KnownAll);
		.difference(AllArtifacts, KnownAll, UnknownAll);
		
		?workspace_data("OrgSpecWSP", OrgId);
		
		for ( .member(ArtName, UnknownAll) ) {
			lookupArtifact(ArtName, ArtId)[wsp_id(OrgId)];
			stopFocus(ArtId)[wsp_id(OrgId)];
		}.

+!stop_current_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		?sim_art_id(SimArtId);
		logKeyMessage("EXECUTING OEStop STAGE.")[artifact_id(SimArtId)];
		
		// first focus on all artifacts that I need to monitor
		//!focus_on_entire_org;
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		focus(ArtIdReorg)[wsp_id(OrgId)];
		startStageExecution("OEStop")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(stop_current_mas)[artifact_name(Scheme)].
		
-!stop_current_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Stop current MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
		


// ########################################################### //
+!change_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		?sim_art_id(SimArtId);
		logKeyMessage("EXECUTING OSChange STAGE.")[artifact_id(SimArtId)];
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		focus(ArtIdReorg)[wsp_id(OrgId)];
		startStageExecution("OSChange")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(change_mas)[artifact_name(Scheme)].
	
-!change_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Change current MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).



// ########################################################### //
+!instantiate_new_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", OrgId);
		?sim_art_id(SimArtId);
		logKeyMessage("EXECUTING OEStart STAGE.")[artifact_id(SimArtId)];
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		focus(ArtIdReorg)[wsp_id(OrgId)];
		startStageExecution("OEStart")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(instantiate_new_mas)[artifact_name(Scheme)].
	
-!instantiate_new_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Instantiate new MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

	
// ############ await reorganization change plans ############ //
+stage_finished("OEStop")
	<-	.print("I have achieved OEStop");
		?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(stop_current_mas)[artifact_name("reorganization_scheme"), wsp_id(OrgId)].
	
+stage_finished("OSChange")
	<-	.print("I have achieved OSChange");
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(WspId);
		goalAchieved(change_mas)[artifact_name("reorganization_scheme"), wsp_id(OrgId)].
	
+stage_finished("OEStart")
	<-	.print("I have achieved OEStart");
		//!defocus_unnecessary_org;
		
		?workspace_data("OrgSpecWSP", OrgId);
		goalAchieved(instantiate_new_mas)[artifact_name("reorganization_scheme"), wsp_id(OrgId)];
		
		// old reorganization_scheme has been completed successfully => remove old scheme artifact instance 
		// and make a new one
		lookupArtifact("reorganization_scheme", OldSchId)[wsp_id(OrgId)];
		destroy[artifact_id(OldSchId)];
		disposeArtifact(OldSchId)[wsp_id(OrgId)];
		
		// dispose reorganization_planner as well
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		disposeArtifact(ArtIdReorg)[wsp_id(OrgId)];
		
		// make new instance of reorganization scheme
		.my_name(Me);
		makeArtifact("reorganization_scheme", "ora4mas.nopl.SchemeBoard",["AmI-Room-Booking/src/reorg-os.xml", reorganization, false, true ], ReorgSchArtId)[wsp_id(OrgId)];
      	setOwner(Me)[artifact_id(ReorgSchArtId)];
      	focus(ReorgSchArtId)[wsp_id(OrgId)];
      	addScheme("reorganization_scheme")[artifact_name("reorg_group"), wsp_id(OrgId)];
      	
      	// resume simulation that has been previously stopped
      	-performing_reorganization_flag;
      	?sim_art_id(SimArtId);
      	logKeyMessage("REORGANIZATION COMPLETED.")[artifact_id(SimArtId)];
		simulationPaused(false)[artifact_id(SimArtId)].
		

+await_plan_changes(ReorgStage, SectionIndex, "[]")
	<-	.wait(200);
		
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		nextStageExecution(ReorgStage)[artifact_id(ArtIdReorg)].

+await_plan_changes(ReorgStage, SectionIndex, [])
	<-	.wait(200);
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		nextStageExecution(ReorgStage)[artifact_id(ArtIdReorg)].


+await_plan_changes(ReorgStage, SectionIndex, PlanAwaitedChangesStr)
	<-	.print("Received wait changes notification: ", PlanAwaitedChangesStr);
		.term2string(PlanAwaitedChanges, PlanAwaitedChangesStr);
		.print("Wait changes list: ", PlanAwaitedChanges);
		
		for ( .member(Change, PlanAwaitedChanges) ) {
			!await_change(Change, 500, 0, 10)
		};
		
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg)[wsp_id(OrgId)];
		nextStageExecution(ReorgStage)[artifact_id(ArtIdReorg)].


-!await_plan_changes
	<-	.print("=========== Reorganization attempt failed! Agents could not perform required OE changes ===========").

+!await_change(no(play(Agent, Role, GrArtId)), Time, TrialCt, MaxTrials)
	:	not play(Agent, Role, GrArtId)[artifact_name(_, GrArtId)] & TrialCt <= MaxTrials
	<-	true.

+!await_change(no(commitment(Agent, Mission, SchArtId)), Time, TrialCt, MaxTrials)
	:	not commitment(Agent, Mission, SchArtId)[artifact_name(_, SchArtId)] & TrialCt <= MaxTrials
	<-	true.

+!await_change(no(schemes(SchArtId, GrArtId)), Time, TrialCt, MaxTrials)
	:	schemes(SchemeList)[artifact_name(_, GrArtId)] & not .member(SchArtId, SchemeList) & TrialCt <= MaxTrials
	<-	
		//.print("No more sheme " , SchArtId, " for group ", GrArtId, " in scheme list ", SchemeList);
		true.

+!await_change(play(Agent, Role, GrArtId), Time, TrialCt, MaxTrials)
	:	play(Agent, Role, GrArtId)[artifact_name(_, GrArtId)] & TrialCt <= MaxTrials
	<-	true.

+!await_change(commitment(Agent, Mission, SchArtId), Time, TrialCt, MaxTrials)
	:	commitment(Agent, Mission, SchArtId)[artifact_name(_, SchArtId)] & TrialCt <= MaxTrials
	<-	true.

+!await_change(Change, Time, TrialCt, MaxTrials)
	:	TrialCt > MaxTrials
	<-	.print("==== Change requirement << ", Change, " >> could not be achieved. OE REORG FAILED. ====" );
		.fail.

//@p_not_await_change[atomic]
-!await_change(Change, Time, TrialCt, MaxTrials)
	:	TrialCt < MaxTrials
	<-	.wait(Time);
		CtNew = TrialCt + 1;
		!await_change(Change, Time, CtNew, MaxTrials).
		


// ======================================= Reorganization Signals ======================================= //	

+reorgSpecification(change(removeScheme, Modality, Agent, SchArtName, GrArtName))
	:	.my_name(Agent) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(removeScheme, obligation, Agent, SchArtName, GrArtName));
		!do_remove_scheme_obligation(SchArtName, GrArtName).

+!do_remove_scheme_obligation(SchArtName, GrArtName)
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		removeScheme(SchArtName)[artifact_name(GrArtName), wsp_id(OrgId)].

-!do_remove_scheme_obligation(SchArtName, GrArtName)[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Error remove scheme obligation because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

+reorgSpecification(change(addScheme, Modality, Agent, SchArtName, GrArtName))
	:	.my_name(Agent) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(addScheme, obligation, Agent, SchArtName, GrArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		addScheme(SchArtName)[artifact_name(GrArtName), wsp_id(OrgId)].

+reorgSpecification(change(removeGroup, Modality, Agent, GrArtName))
	:	.my_name(Agent) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(removeGroup, obligation, Agent, GrArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		
		lookupArtifact(GrArtName, GrArtId)[wsp_id(OrgId)];
		stopFocus(GrArtId)[wsp_id(OrgId)];
		destroy[artifact_name(GrArtName), wsp_id(OrgId)];
		disposeArtifact(GrArtId)[wsp_id(OrgId)];
		
		// remove from common known org. artifact repository as well
		?system_group_artifacts(GroupArtifacts);
		.delete(GrArtName, GroupArtifacts, NewGroupArtifacts);
		-+system_group_artifacts(NewGroupArtifacts).

+reorgSpecification(change(addGroup, Modality, Agent, GrArtName, GrType))
	:	.my_name(Agent) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", changeObligation(addGroup, obligation, Agent, GrArtName, GrType));
		?workspace_data("OrgSpecWSP", OrgId);
		
		makeArtifact(GrArtName, "ora4mas.nopl.GroupBoard", 
						["AmI-Room-Booking/src/ami-room-booking-os.xml", GrType, false, true ], GrArtId)[wsp_id(OrgId)];
		setOwner(Agent)[artifact_name(GrArtName), wsp_id(OrgId)];
		focus(GrArtId)[wsp_id(OrgId)];
		
		// add to common known org. artifact repository as well
		?system_group_artifacts(GroupArtifacts);
		.concat([GrArtName], GroupArtifacts, NewGroupArtifacts);
		-+system_group_artifacts(NewGroupArtifacts).
		
		
-reorgSpecification(Change)[error(I),error_msg(M)]
	<-	.print(":::: failure at Change requirement -- ", Change, ": ", I,": ",M).