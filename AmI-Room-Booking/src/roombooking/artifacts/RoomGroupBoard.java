package roombooking.artifacts;

import java.util.HashMap;
import java.util.TreeMap;

import moise.common.MoiseException;
import moise.os.ss.Role;
import npl.parser.ParseException;
import ora4mas.nopl.GroupBoard;
import ora4mas.nopl.JasonTermWrapper;
import cartago.OPERATION;
import cartago.ObsProperty;

public class RoomGroupBoard extends GroupBoard {
	
	// particular to the GroupBoard artifact that is the instance of reorgGroup
    public static final String obsPropMonitorComm = "monitorCommRing";
    
    private HashMap<String, TreeMap<String, String>> orderedMonitorMap = new HashMap<String, TreeMap<String,String>>();
    //private TreeMap<String, String> orderedMonitorsList = new TreeMap<String, String>();
	
    
    public void init(final String osFile, final String grType, final boolean createMonitoring, final boolean hasGUI) throws ParseException, MoiseException{
        
    	super.init(osFile, grType, createMonitoring, hasGUI);
    	
        // initiate orderedMonitorMap
        for(Role r : getSpec().getRoles()) {
        	String roleName = r.getId();
        	orderedMonitorMap.put(roleName, new TreeMap<String, String>());
        }
    }
    
    @OPERATION @Override
    public void adoptRole(String role) {
    	adoptRole(getOpUserName(), role);
    	
    	// the room_manager_group will create the communication rings per role
        if (getSpec().getId().equals("room_manager_group")) {
        	commOrderInsertion(getOpUserName(), role);
        }
    }
    
    @OPERATION @Override public void leaveRole(String role)  {
    	leaveRole(getOpUserName(), role);
    	
    	// the room_manager_group will create the communication rings per role
        if (getSpec().getId().equals("room_manager_group")) {
        	commOrderRemoval(getOpUserName(), role);
        }
    }
    
	private void commOrderInsertion(String player, String role) {
    	TreeMap<String, String> orderedMonitorsList = orderedMonitorMap.get(role);
    	
		orderedMonitorsList.put(player, role);
		String leftN = orderedMonitorsList.lowerKey(player);
		String rightN = orderedMonitorsList.higherKey(player);
		
		if (leftN == null) leftN = orderedMonitorsList.lastKey();
		if (rightN == null) rightN = orderedMonitorsList.firstKey();
		
		defineObsProperty(obsPropMonitorComm, new JasonTermWrapper(player),
				new JasonTermWrapper("left"), new JasonTermWrapper(leftN));
		defineObsProperty(obsPropMonitorComm, new JasonTermWrapper(player),
				new JasonTermWrapper("right"), new JasonTermWrapper(rightN));
		
		if (orderedMonitorsList.size() > 1) {
			ObsProperty propLeftN = getObsPropertyByTemplate(obsPropMonitorComm,
					new JasonTermWrapper(leftN), new JasonTermWrapper("right"),
					new JasonTermWrapper(rightN));
	
			if (propLeftN != null) {
				propLeftN.updateValues(new JasonTermWrapper(leftN),
								new JasonTermWrapper("right"),
								new JasonTermWrapper(player));
			}
	
			ObsProperty propRightN = getObsPropertyByTemplate(obsPropMonitorComm,
					new JasonTermWrapper(rightN), new JasonTermWrapper("left"),
					new JasonTermWrapper(leftN));
	
			if (propRightN != null) {
				propRightN.updateValues(new JasonTermWrapper(rightN),
						new JasonTermWrapper("left"), new JasonTermWrapper(player));
			}
		}	
    }
    
    private void commOrderRemoval(String player, String role) {
    	TreeMap<String, String> orderedMonitorsList = orderedMonitorMap.get(role);
    	
    	String leftN = orderedMonitorsList.lowerKey(player);
		String rightN = orderedMonitorsList.higherKey(player);
    
		if (leftN == null) leftN = orderedMonitorsList.lastKey();
		if (rightN == null) rightN = orderedMonitorsList.firstKey();
		
		// remove currenPlayer
		orderedMonitorsList.remove(player);
		removeObsPropertyByTemplate(obsPropMonitorComm, new JasonTermWrapper(player), 
									new JasonTermWrapper("left"),
									new JasonTermWrapper(leftN));
		removeObsPropertyByTemplate(obsPropMonitorComm, new JasonTermWrapper(player), 
				new JasonTermWrapper("right"),
				new JasonTermWrapper(rightN));
		
		if (orderedMonitorsList.size() > 1) {
			ObsProperty propLeftN = getObsPropertyByTemplate(obsPropMonitorComm,
					new JasonTermWrapper(leftN), new JasonTermWrapper("right"),
					new JasonTermWrapper(player));
	
			if (propLeftN != null) {
				propLeftN.updateValues(new JasonTermWrapper(leftN),
								new JasonTermWrapper("right"),
								new JasonTermWrapper(rightN));
			}
	
			ObsProperty propRightN = getObsPropertyByTemplate(obsPropMonitorComm,
					new JasonTermWrapper(rightN), new JasonTermWrapper("left"),
					new JasonTermWrapper(player));
	
			if (propRightN != null) {
				propRightN.updateValues(new JasonTermWrapper(rightN),
						new JasonTermWrapper("left"), new JasonTermWrapper(leftN));
			}
		}
    }
	
}
