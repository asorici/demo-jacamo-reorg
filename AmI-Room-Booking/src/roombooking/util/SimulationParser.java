package roombooking.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import roombooking.core.Room.RoomFormat;
import roombooking.core.event.Event.EventType;
import xml.ParseErrorHandler;

public class SimulationParser {
	public static String simConfigFileName = "src/ami-room-booking-sim.xml";
	
	public static List<String> orgAgentNames = new ArrayList<String>();
	public static List<String> proposerAgentNames = new ArrayList<String>();
	public static List<String> participantAgentNames = new ArrayList<String>();
	
	public static HashMap<EventType, Integer> managementStructure = new HashMap<EventType, Integer>();
	public static HashMap<Integer, RoomData> roomData = new HashMap<Integer, RoomData>();
	public static HashMap<String, HashMap<String, Object>> initialAssignments = new HashMap<String, HashMap<String,Object>>();
	
	public static HashMap<EventType, Integer> requestStructure = new HashMap<EventType, Integer>();
	public static HashMap<EventType, RequestSpreadType> requestIntraDaySpread = new HashMap<EventType, RequestSpreadType>();
	public static HashMap<EventType, RequestSpreadType> requestInterDaySpread = new HashMap<EventType, RequestSpreadType>();
	
	public static enum RequestSpreadType {
		uniform, arithmeticIncrease, arithmeticDecrease
	}
	public static int simDayDuration = 1;
	
	
	public static void parse(String configurationFile) {
		if (configurationFile != null && !configurationFile.equals("")) {
			simConfigFileName = configurationFile;
			//System.out.println("Using configuration file: " + simConfigFileName);
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new ParseErrorHandler());
			Document document = builder.parse(new InputSource(simConfigFileName));
			
			parseOrgAgentList(document.getDocumentElement());
			parseUserAgentList(document.getDocumentElement());
			
			parseSimulationConfig(document.getDocumentElement());
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Element getElementByTagName(Element root, String tag) {
		NodeList l = root.getElementsByTagName(tag);
		if (l != null) {
			return (Element)l.item(0);
		}
		
		return null;
	}
	
	private static void parseOrgAgentList(Element documentElement) {
		Element orgAgentsElem = getElementByTagName(documentElement, "org-agents");
		NodeList agentList = orgAgentsElem.getElementsByTagName("agent");
		
		for (int i = 0; i < agentList.getLength(); i++) {
			Element agentElem = (Element)agentList.item(i);
			
			String agName = agentElem.getAttribute("name"); 
			int nrAgents = Integer.parseInt(agentElem.getAttribute("number"));
			
			if (nrAgents == 1) {
				orgAgentNames.add(agName);
			}
			else {
				for (int k = 1; k <= nrAgents; k++) {
					String numberedName = agName + "" + k;
					orgAgentNames.add(numberedName);
				}
			}
		}
	}
	
	private static void parseUserAgentList(Element documentElement) {
		Element userAgentElem = getElementByTagName(documentElement, "user-agents");
		Element proposerAgentElem = getElementByTagName(userAgentElem, "proposers");
		Element participantAgentElem = getElementByTagName(userAgentElem, "participants");
		
		NodeList proposerUserAgentList = proposerAgentElem.getElementsByTagName("agent");
		NodeList participantUserAgentList = participantAgentElem.getElementsByTagName("agent");
		
		// add all proposer agents
		for (int i = 0; i < proposerUserAgentList.getLength(); i++) {
			Element agentElem = (Element)proposerUserAgentList.item(i);
			
			String agName = agentElem.getAttribute("name"); 
			int nrAgents = Integer.parseInt(agentElem.getAttribute("number"));
			
			if (nrAgents == 1) {
				proposerAgentNames.add(agName);
			}
			else {
				for (int k = 1; k <= nrAgents; k++) {
					String numberedName = agName + "" + k;
					proposerAgentNames.add(numberedName);
				}
			}
		}
		
		// add all participant agents
		for (int i = 0; i < participantUserAgentList.getLength(); i++) {
			Element agentElem = (Element)participantUserAgentList.item(i);
			
			String agName = agentElem.getAttribute("name"); 
			int nrAgents = Integer.parseInt(agentElem.getAttribute("number"));
			
			if (nrAgents == 1) {
				participantAgentNames.add(agName);
			}
			else {
				for (int k = 1; k <= nrAgents; k++) {
					String numberedName = agName + "" + k;
					participantAgentNames.add(numberedName);
				}
			}
		}
	}
	
	private static void parseSimulationConfig(Element documentElement) {
		Element simConfigElem = getElementByTagName(documentElement, "sim-config");
		
		// parse simulation duration 
		Element dayDurationElem = getElementByTagName(simConfigElem, "day-duration");
		simDayDuration = Integer.parseInt(dayDurationElem.getTextContent());
		
		// parse room-management and request structures
		Element roomManagementStructElem = getElementByTagName(simConfigElem, "room-management");
		Element requestStructElem = getElementByTagName(simConfigElem, "request-management");
		
		Element roomDataElem = getElementByTagName(roomManagementStructElem, "room-data");
		NodeList roomList = roomDataElem.getElementsByTagName("room");
		
		for (int i = 0; i < roomList.getLength(); i++) {
			Element roomElem = (Element)roomList.item(i);
			
			int roomId = Integer.parseInt(roomElem.getAttribute("id"));
			int numSeats = Integer.parseInt(roomElem.getAttribute("seats"));
			RoomFormat roomFormat = RoomFormat.valueOf(roomElem.getAttribute("format"));
			String equipmentListString = roomElem.getAttribute("equipment");
			
			String[] equipmentIdStrings = equipmentListString.split(", ");
			List<Integer> equipmentList = new ArrayList<Integer>();
			for (int k = 0; k < equipmentIdStrings.length; k++) {
				if (!equipmentIdStrings[k].isEmpty()) {
					equipmentList.add(Integer.parseInt(equipmentIdStrings[k]));
				}
			}
			
			RoomData rd = new RoomData(roomId, roomFormat, numSeats, equipmentList);
			roomData.put(roomId, rd);
		}
		
		NodeList assignmentList = roomManagementStructElem.getElementsByTagName("initial-assignment");
		int teachingEventHandlers = 0;
		int meetingEventHandlers = 0;
		int brainstormEventHandlers = 0;
		
		for (int i = 0; i < assignmentList.getLength(); i++) {
			Element assignmentElem = (Element)assignmentList.item(i);
			HashMap<String, Object> assignmentData = new HashMap<String, Object>();
			
			String specializedGroup = assignmentElem.getAttribute("group");
			assignmentData.put("group", specializedGroup);
			
			int roomId = Integer.parseInt(assignmentElem.getAttribute("room_id"));
			assignmentData.put("roomId", roomId);
			
			if (specializedGroup.equals("teaching_group")) {
				teachingEventHandlers ++;
			}
			else if (specializedGroup.equals("meeting_group")) {
				meetingEventHandlers ++;
			}
			else if (specializedGroup.equals("brainstorm_group")) {
				brainstormEventHandlers ++;
			}
			
			initialAssignments.put(assignmentElem.getAttribute("agent"), assignmentData);
		}
		
		managementStructure.put(EventType.teachingEvent, teachingEventHandlers);
		managementStructure.put(EventType.meetingEvent, meetingEventHandlers);
		managementStructure.put(EventType.brainstormEvent, brainstormEventHandlers);
		
		for (EventType eventType : EventType.values()) {
			//Element roomMgElem = getElementByTagName(roomManagementStructElem, eventType.name());
			//managementStructure.put(eventType, Integer.parseInt(roomMgElem.getTextContent()));
			
			Element requestMgElem = getElementByTagName(requestStructElem, eventType.name());
			requestStructure.put(eventType, Integer.parseInt(requestMgElem.getAttribute("number")));
			requestIntraDaySpread.put(eventType, RequestSpreadType.valueOf(requestMgElem.getAttribute("distIntraDay")));
			requestInterDaySpread.put(eventType, RequestSpreadType.valueOf(requestMgElem.getAttribute("distInterDay")));
		}
	}
}
