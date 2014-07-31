// ############## Reorganization Signals ############## //
//+reorgSpecification(Change)
//	<-	.print("!!! Reorg specification: ", Change).

+reorgSpecification(change(leaveRole, Modality, Agent, Role, GrArtName))
	:	.my_name(Agent) & play(Agent, Role, GrArtName) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(leaveRole, obligation, Agent, Role, GrArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		//?sim_art_id(SimArtId);
		//logKeyMessage("LEAVING ROLE: ", Role)[artifact_id(SimArtId)];
		leaveRole(Role)[artifact_name(GrArtName), wsp_id(OrgId)].
		//lookupArtifact(GrArtName, Id);
		//stopFocus(Id).
	
-play(Agent, Role, GrArtName)
	:	.my_name(Agent)
	<-	?sim_art_id(SimArtId);
		logKeyMessage("LEAVING ROLE: ", Role, " IN GROUP: ", GrArtName)[artifact_id(SimArtId)].
	
+reorgSpecification(change(leaveMission, Modality, Agent, Mission, SchArtName))
	:	.my_name(Agent) & commitment(Agent, Mission, SchArtName) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(leaveMission, obligation, Agent, Mission, SchArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		leaveMission(Mission)[artifact_name(SchArtName), wsp_id(OrgId)].
		//lookupArtifact(SchArtName, Id);
		//stopFocus(Id).
	
+reorgSpecification(change(playRole, Modality, Agent, Role, GrArtName))
	:	.my_name(Agent)  & Modality == obligation & not play(Agent, Role, GrArtName)
	<-	.print(":::: Received reorg specification : ", change(playRole, obligation, Agent, Role, GrArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		?sim_art_id(SimArtId);
		//logKeyMessage("ADOPTING NEW ROLE: ", Role)[artifact_id(SimArtId)];
		
		lookupArtifact(GrArtName, GrArtId)[wsp_id(OrgId)];
		focus(GrArtId)[wsp_id(OrgId)];
		adoptRole(Role)[artifact_id(GrArtId)].
		
+play(Agent, Role, GrArtName)
	:	.my_name(Agent)
	<-	?sim_art_id(SimArtId);
		if (SimArtId \== none) {
			logKeyMessage("ADOPTING ROLE: ", Role, " IN GROUP: ", GrArtName)[artifact_id(SimArtId)];
		}.
		
+reorgSpecification(change(commitMission, Modality, Agent, Mission, SchArtName))
	:	.my_name(Agent) & Modality == obligation & not commitment(Agent, Mission, SchArtName)
	<-	.print(":::: Received reorg specification : ", change(commitMission, obligation, Agent, Mission, SchArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		
		lookupArtifact(SchArtName, SchArtId)[wsp_id(OrgId)];
		focus(SchArtId)[wsp_id(OrgId)];
		commitMission(Role)[artifact_id(SchArtId)].

+reorgSpecification(change(removeGroup, Modality, Agent, GrArtName))
	:	.my_name(Agent) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(removeGroup, obligation, Agent, GrArtName));
		.abolish(_[artifact_name(_, GrArtName)]).
		
// ############################################### ADDITIONAL ############################################### //
+?sim_art_id(SimArtId)
	<-	if ( sim_art_id(Id) ) {
			SimArtId = Id;
		}  
		else {
			SimArtId = none;
		}.