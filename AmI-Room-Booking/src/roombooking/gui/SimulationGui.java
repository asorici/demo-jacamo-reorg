package roombooking.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import roombooking.core.Room.RoomType;
import roombooking.core.event.Event.EventType;

public class SimulationGui {
	private DefaultCategoryDataset allRequestDynamics;
	private DefaultCategoryDataset deniedRequestDynamics;
	private DefaultCategoryDataset roleTypeDynamics;
	
	private JFreeChart allRequestChart;
	private JFreeChart deniedRequestChart;
	private JFreeChart roleTypeChart;
	
	//private JTextArea agentLogArea;
	private JTextPane agentLogArea;
	private SimpleAttributeSet keyPhrase = new SimpleAttributeSet(); 
	
	private JFrame chartFrame;
	
	public SimulationGui() {
		allRequestDynamics = new DefaultCategoryDataset();
		deniedRequestDynamics = new DefaultCategoryDataset();
		roleTypeDynamics = new DefaultCategoryDataset();
		
		initGui();
	}

	private void initGui() {
		chartFrame = new JFrame("Monitoring View");
		chartFrame.setLayout(new BorderLayout(10, 10));
		chartFrame.setMinimumSize(new Dimension(1000, 700));
		
		//agentLogArea = new JTextArea();
		agentLogArea = new JTextPane();
		agentLogArea.setEditable(false);
		DefaultCaret caret = (DefaultCaret)agentLogArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		StyleConstants.setForeground(keyPhrase, Color.RED);
		StyleConstants.setBackground(keyPhrase, Color.YELLOW);
		StyleConstants.setBold(keyPhrase, true);
		
		JPanel requestDynamicsPanel = new JPanel(new GridLayout(2, 1));
		JPanel agentLogPanel = new JPanel(new GridLayout(2, 1));
		
		allRequestChart = ChartFactory.createLineChart("All request dynamics", "date(day-hour)", "num. requests", 
				allRequestDynamics, PlotOrientation.VERTICAL, true, true, false);
		allRequestChart.setBackgroundPaint(Color.white);
		final CategoryPlot allRequestPlot = allRequestChart.getCategoryPlot();
		allRequestPlot.setBackgroundPaint(Color.white);
		allRequestPlot.setDomainGridlinePaint(Color.gray);
		allRequestPlot.setRangeGridlinePaint(Color.black);
		LineAndShapeRenderer allRequestrenderer = (LineAndShapeRenderer)allRequestPlot.getRenderer();
		allRequestrenderer.setShapesVisible(true);
		allRequestrenderer.setUseFillPaint(true);
		allRequestrenderer.setFillPaint(Color.yellow);
		
		
		deniedRequestChart = ChartFactory.createLineChart("Denied request dynamics", "date(day-hour)", "num. requests", 
				deniedRequestDynamics, PlotOrientation.VERTICAL, true, true, false);
		deniedRequestChart.setBackgroundPaint(Color.white);
		final CategoryPlot deniedRequestPlot = deniedRequestChart.getCategoryPlot();
		deniedRequestPlot.setBackgroundPaint(Color.white);
		deniedRequestPlot.setDomainGridlinePaint(Color.gray);
		deniedRequestPlot.setRangeGridlinePaint(Color.black);
		LineAndShapeRenderer deniedRequestrenderer = (LineAndShapeRenderer)deniedRequestPlot.getRenderer();
		deniedRequestrenderer.setShapesVisible(true);
		deniedRequestrenderer.setUseFillPaint(true);
		deniedRequestrenderer.setFillPaint(Color.yellow);
		
		roleTypeChart = ChartFactory.createBarChart("Role allocation dynamics", "role type", "num. agents", 
				roleTypeDynamics, PlotOrientation.VERTICAL, true, true, false);
		roleTypeChart.setBackgroundPaint(Color.white);
		final CategoryPlot roleTypePlot = roleTypeChart.getCategoryPlot();
		roleTypePlot.setBackgroundPaint(Color.white);
		roleTypePlot.setDomainGridlinePaint(Color.gray);
		roleTypePlot.setRangeGridlinePaint(Color.black);
		
		JPanel allRequestChartPanel = new ChartPanel(allRequestChart);
		allRequestChartPanel.setPreferredSize(new Dimension(650, 345));
		
		JPanel deniedRequestChartPanel = new ChartPanel(deniedRequestChart);
		deniedRequestChartPanel.setPreferredSize(new Dimension(650, 345));
		
		JPanel roomTypeChartPanel = new ChartPanel(roleTypeChart);
		roomTypeChartPanel.setPreferredSize(new Dimension(345, 345));
		
		requestDynamicsPanel.add(allRequestChartPanel);
		requestDynamicsPanel.add(deniedRequestChartPanel);
		
		agentLogPanel.add(roomTypeChartPanel);
		
		JScrollPane agentLogScroll = new JScrollPane(agentLogArea);
		agentLogScroll.setPreferredSize(new Dimension(345, 345));
		agentLogScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		agentLogScroll.setBorder(BorderFactory.createLineBorder(Color.BLUE));
		
		agentLogPanel.add(agentLogScroll);
		
		chartFrame.add(requestDynamicsPanel, BorderLayout.CENTER);
		chartFrame.add(agentLogPanel, BorderLayout.EAST);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				chartFrame.setVisible(true);
				//chartFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			}
		});
	}
	
	public void updateAllRequests(int day, int hour, HashMap<EventType, Integer> requestCount) {
		String dateString = day + "-" + hour;
		
		for (EventType evType : EventType.values()) {
			Integer count = requestCount.get(evType);
			if (count != null) {
				allRequestDynamics.addValue(count, evType.name(), dateString);
			}
		}
		
		/*
		for (EventType evType : requestCount.keySet()) {
			Integer count = requestCount.get(evType);
			allRequestDynamics.addValue(count, evType.name(), dateString);
		}
		*/
	}
	
	public void updateDeniedRequests(int day, int hour, HashMap<EventType, Integer> requestCount) {
		String dateString = day + "-" + hour;
		
		for (EventType evType : EventType.values()) {
			Integer count = requestCount.get(evType);
			if (count != null) {
				deniedRequestDynamics.addValue(count, evType.name(), dateString);
			}
		}
		
		/*
		for (EventType evType : requestCount.keySet()) {
			Integer count = requestCount.get(evType);
			deniedRequestDynamics.addValue(count, evType.name(), dateString);
		}
		*/
	}
	
	public void updateRoomTypes(int day, int hour, HashMap<RoomType, Integer> requestCount) {
		//String dateString = day + "-" + hour;
		
		for (EventType evType : EventType.values()) {
			switch(evType) {
				case teachingEvent:
				{
					Integer count = requestCount.get(RoomType.teachingRoom);
					if (count != null) {
						roleTypeDynamics.addValue(count, RoomType.teachingRoom.name(), "room types");
					}
					break;
				}
				
				case meetingEvent:
				{
					Integer count = requestCount.get(RoomType.meetingRoom);
					if (count != null) {
						roleTypeDynamics.addValue(count, RoomType.meetingRoom.name(), "room types");
					}
					break;
				}
				
				case brainstormEvent:
				{
					Integer count = requestCount.get(RoomType.brainstormRoom);
					if (count != null) {
						roleTypeDynamics.addValue(count, RoomType.brainstormRoom.name(), "room types");
					}
					break;
				}
			}
		}
		
		/*
		for (RoomType roomType : requestCount.keySet()) {
			Integer count = requestCount.get(roomType);
			roleTypeDynamics.addValue(count, roomType.name(), "room types");
		}
		*/
	}
	
	public void logMessage(String message) {
		try {
			Document doc = agentLogArea.getStyledDocument();
			doc.insertString(doc.getLength(), message + "\n\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	public void logKeyMessage(String message) {
		try {
			Document doc = agentLogArea.getStyledDocument();
			doc.insertString(doc.getLength(), message + "\n\n", keyPhrase);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	public void clearRequestCharts() {
		allRequestDynamics.clear();
		deniedRequestDynamics.clear();
		
		refreshGui();
	}
	
	public void refreshGui() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				chartFrame.repaint();
			}
		});
	}
}
