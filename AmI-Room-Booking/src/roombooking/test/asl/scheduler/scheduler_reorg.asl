// ############## Reorganization Signals ############## //

@reorg_spec_plan1[atomic]
+reorgSpecification(change(leaveMission, Modality, Agent, Mission, SchArtName))
	:	.my_name(Agent) & commitment(Agent, Mission, SchArtName) & Modality == obligation
	<-	.print(":::: Received reorg specification : ", change(leaveMission, Modality, Agent, Mission, SchArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		
		-+manage_event_schedule(false);
		-+manage_event_delete(false);
		-+manage_event_modification(false);
		-+manage_event_participation(false);
		
		//cartago.set_current_wsp(OrgId);
		leaveMission(Mission)[artifact_name(SchArtName), wsp_id(OrgId)].
		
		//lookupArtifact(SchArtName, Id);
		//stopFocus(Id).

@reorg_spec_plan2[atomic]		
+reorgSpecification(change(commitMission, Modality, Agent, Mission, SchArtName))
	:	.my_name(Agent) & Modality == obligation & not commitment(Agent, Mission, SchArtName)
	<-	.print(":::: Received reorg specification : ", change(commitMission, Modality, Agent, Mission, SchArtName));
		?workspace_data("OrgSpecWSP", OrgId);
		//cartago.set_current_wsp(OrgId);
		
		lookupArtifact(SchArtName, SchArtId)[wsp_id(OrgId)];
		focus(SchArtId)[wsp_id(OrgId)];
		commitMission(Role)[artifact_id(SchArtId)].

