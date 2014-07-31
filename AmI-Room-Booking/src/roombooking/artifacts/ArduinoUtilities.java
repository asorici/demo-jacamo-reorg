package roombooking.artifacts;

import jacaarduino.ArduinoArtifact;
import jacaarduino.ArduinoException;
import jacaarduino.ArduinoOperationDescription;
import jacaarduino.ArduinoOperationDescription.OperationType;
import roombooking.core.Room;
import xml.ArduinoConfigParser;
import cartago.OPERATION;
import cartago.ObsProperty;

public class ArduinoUtilities extends ArduinoArtifact {
	private Room myRoom = null;
	private boolean utilitiesOn = false;
	
	protected void init(Room myRoom, String configurationFile) throws Exception {
		this.myRoom = myRoom;
		super.init(configurationFile);
	}
	
	@Override
	protected void establishArduinoConnection() throws ArduinoException {
		System.out.println("[ArduinoUtlities " + myRoom.getManagingAgentName() + "] Arduino Utilties connection established.");
	}
	
	@Override
	protected Object[] executeGet(ArduinoOperationDescription opDesc) throws ArduinoException {
		
		// check for appropriate operation type
		if (opDesc.getOperationType() == OperationType.SET && opDesc.getOperationType() != OperationType.ALL) {
			throw new ArduinoException("Illegal GET action for this operation " + opDesc.toString());
		}
		
		Object[] answer = new Object[1];
		answer[0] = utilitiesOn;
		
		return answer;
	}

	@Override
	protected void executeSet(ArduinoOperationDescription opDesc, Object[] values) throws ArduinoException { 
		
		// check for appropriate operation type
		if (opDesc.getOperationType() != OperationType.SET && opDesc.getOperationType() != OperationType.ALL) {
			throw new ArduinoException("Illegal SET action for the operation " + opDesc.toString());
		}
		
		if (values != null && values[0] != null) {
			utilitiesOn = (Boolean)values[0];
			System.out.println("[ArduinoUtlities " + myRoom.getManagingAgentName() + "] UTILITIES ONSTATE: " + utilitiesOn + ".");
		}
	}
	
	@OPERATION
	protected void doValueQuery(ArduinoOperationDescription opDesc) {
		while(working) {
			try {
				Object[] values = executeGet(opDesc);
				if (values != null) {
					 // if values were successfully retrieved update the observable property
					 ObsProperty prop = getObsProperty(opDesc.getSensorValueName());
					 prop.updateValues(values);
				}
				else {
					System.out.println("GET acton for operation " + opDesc.toString() + " failed.");
				}
				
				await_time(ArduinoConfigParser.queryFrequencyMap.get(opDesc.getSensorName()));
			} catch (ArduinoException e) {
				e.printStackTrace();
			}
		}
	}
}
