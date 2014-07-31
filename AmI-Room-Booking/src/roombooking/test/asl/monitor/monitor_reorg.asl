// ================================== Org manager obligations ================================== //


// ======================================== SELECTION PHASE =========================================== // 
// selection goal - as selector I will have to judge among the different variants submitted by the agents
+!selection[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		goalAchieved(selection)[artifact_name(Scheme)].



// ===================================== IMPLEMENTATION PHASE ========================================= //
// implementation goal - actual submission of the reorganization design - hardest part
// ########################################################### //
+!focus_on_entire_org
	<-	.send(building_manager, askOne, system_group_artifacts(GrArtifacts), system_group_artifacts(GrArtifacts));
		.send(building_manager, askOne, system_scheme_artifacts(SchArtifacts), system_scheme_artifacts(SchArtifacts));
		.concat(GrArtifacts, SchArtifacts, AllArtifacts);
		
		?known_groups(KnownGroups);
		?known_schemes(KnownSchems);
		.concat(KnownGroups, KnownSchems, KnownAll);
		.difference(AllArtifacts, KnownAll, UnknownAll);
		
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		for ( .member(ArtName, UnknownAll) ) {
			lookupArtifact(ArtName, ArtId);
			focus(ArtId)
		}.
	
+!defocus_unnecessary_org
	<-	.send(building_manager, askOne, system_group_artifacts(GrArtifacts), system_group_artifacts(GrArtifacts));
		.send(building_manager, askOne, system_scheme_artifacts(SchArtifacts), system_scheme_artifacts(SchArtifacts));
		.concat(GrArtifacts, SchArtifacts, AllArtifacts);
		
		?known_groups(KnownGroups);
		?known_schemes(KnownSchems);
		.concat(KnownGroups, KnownSchems, KnownAll);
		.difference(AllArtifacts, KnownAll, UnknownAll);
		
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		for ( .member(ArtName, UnknownAll) ) {
			lookupArtifact(ArtName, ArtId);
			stopFocus(ArtId)
		}.

+!stop_current_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		// first focus on all artifacts that I need to monitor
		!focus_on_entire_org;
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
		focus(ArtIdReorg);
		startStageExecution("OEStop")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(stop_current_mas)[artifact_name(Scheme)].
		
-!stop_current_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Stop current MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).
		


// ########################################################### //
+!change_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
		focus(ArtIdReorg);
		startStageExecution("OSChange")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(change_mas)[artifact_name(Scheme)].
	
-!change_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Change current MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).



// ########################################################### //
+!instantiate_new_mas[scheme(Scheme)]
	<-	?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
		focus(ArtIdReorg);
		startStageExecution("OEStart")[artifact_id(ArtIdReorg)].
		
		//goalAchieved(instantiate_new_mas)[artifact_name(Scheme)].
	
-!instantiate_new_mas[error(ErrorId), error_msg(Msg), code(CodeBody), code_src(CodeSrc), code_line(CodeLine)]
	<-	.print("!!!! Instantiate new MAS process stopped because -- ",ErrorId," msg: ", Msg, " codeBody: ", CodeBody, " codeSrc: ", CodeSrc, " codeLine: ", CodeLine).

	
// ############ await reorganization change plans ############ //
+stage_finished("OEStop")
	<-	.print("I have achieved OEStop");
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		goalAchieved(stop_current_mas)[artifact_name("reorganization_scheme")].
	
+stage_finished("OSChange")
	<-	.print("I have achieved OSChange");
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		goalAchieved(change_mas)[artifact_name("reorganization_scheme")].
	
+stage_finished("OEStart")
	<-	.print("I have achieved OEStart");
		!defocus_unnecessary_org;
		
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		-performing_reorganization_flag;
		goalAchieved(instantiate_new_mas)[artifact_name("reorganization_scheme")].

+await_plan_changes(ReorgStage, SectionIndex, "[]")
	<-	.wait(5000);
		
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
		nextStageExecution(ReorgStage)[artifact_id(ArtIdReorg)].

+await_plan_changes(ReorgStage, SectionIndex, [])
	<-	.wait(5000);
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
		nextStageExecution(ReorgStage)[artifact_id(ArtIdReorg)].


+await_plan_changes(ReorgStage, SectionIndex, PlanAwaitedChangesStr)
	<-	.print("Received wait changes notification: ", PlanAwaitedChangesStr);
		.term2string(PlanAwaitedChanges, PlanAwaitedChangesStr);
		.print("Wait changes list: ", PlanAwaitedChanges);
		
		for ( .member(Change, PlanAwaitedChanges) ) {
			!await_change(Change, 500, 0, 10)
		};
		
		?workspace_data("OrgSpecWSP", WspId);
		cartago.set_current_wsp(WspId);
		
		lookupArtifact("reorganization_planner", ArtIdReorg);
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
+reorgSpecification(change(addGroup, _, Agent, GrArtName, GrType))
	<-	!focus_on_group(GrArtName, 500, 0, 10).

+!focus_on_group(GrArtName, Time, TrialCt, MaxTrials)
	:	TrialCt <= MaxTrials
	<-	?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact(GrArtName, ArtId)[wsp_id(OrgId)];
		focus(ArtId)[wsp_id(OrgId)].

-!focus_on_group(GrArtName, Time, TrialCt, MaxTrials)
	:	TrialCt < MaxTrials
	<-	.wait(Time);
		CtNew = TrialCt + 1;
		!focus_on_group(GrArtName, Time, TrialCt, MaxTrials).
