package nismod.transport.showcase;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.swing.JMapFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.decision.RoadDevelopment;
import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Dashboard for the road development policy intervention.
 * @author Milan Lovric
 */
public class RoadDevelopmentDashboard extends JFrame {

	private JPanel contentPane;
	private JPanel panel_1;
	private JPanel panel_2;
	private JTable table;
	private JTable table_1;
	private JTable table_2;
	private JTable table_3;
	private JScrollPane scrollPane;
	private JScrollPane scrollPane_1;
	private JScrollPane scrollPane_2;
	private JScrollPane scrollPane_3;
	private JLabel lblBeforeIntervention;
	private JLabel label;
	private JLabel labelPanel2;
	private JTextField totalDemandAfter;
	private JTextField totalDemandBefore;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField textField;
	
	private static final String configFile = "./src/test/config/testConfig.properties";
	private static RoadNetwork roadNetwork;
	private static Properties props;
	private static ODMatrix odm;
	private static SkimMatrix tsmBefore;
	private static SkimMatrix csmBefore;
	private static RouteSetGenerator rsg;
	private static Zoning zoning;
	
	private static HashMap<VehicleType, Double> vehicleTypeToPCU;
	private static HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> baseFuelConsumptionRates;
	private static HashMap<Integer, HashMap<Pair<VehicleType, EngineType>, Double>> relativeFuelEfficiency;
	private static Map<Integer, Map<TimeOfDay, Double>> timeOfDayDistribution;
	private static Map<Integer, Map<TimeOfDay, Double>> timeOfDayDistributionFreight;
	private static HashMap<Integer, HashMap<EnergyType, Double>> yearToEnergyUnitCosts;
	private static HashMap<Integer, HashMap<EnergyType, Double>> yearToUnitCO2Emissions;
	private static HashMap<Integer, HashMap<VehicleType, HashMap<EngineType, Double>>> yearToEngineTypeFractions;
	private static HashMap<Integer, HashMap<VehicleType, Double>> yearToAVFractions;
	
	public static final int MAP_WIDTH = 750;
	public static final int MAP_HEIGHT = 700;
	public static final int BETWEEN_MAP_SPACE = 0; 
	public static final int BEFORE_MAP_X = LandingGUI.SCREEN_WIDTH - MAP_WIDTH * 2 - BETWEEN_MAP_SPACE;
	public static final int BEFORE_MAP_Y = 0;
	public static final int AFTER_MAP_X = BEFORE_MAP_X + MAP_WIDTH + BETWEEN_MAP_SPACE;
	public static final int AFTER_MAP_Y = BEFORE_MAP_Y;
	public static final int TABLE_LABEL_WIDTH = 100; //the width of the table label (with car icon and clock icon)
	public static final int TABLE_ROW_HEIGHT = 18;
	public static final int LEFT_MARGIN = 36; //x position of the first control (policy design area)
	public static final int SECOND_MARGIN = 233; //x position of the second control (policy design area)

	public static final Font TABLE_FONT = new Font("Lato", Font.BOLD, 12);
	public static final Border TABLE_BORDER = BorderFactory.createLineBorder(LandingGUI.DARK_GRAY, 1);
	public static final Border COMBOBOX_BORDER = BorderFactory.createLineBorder(LandingGUI.DARK_GRAY, 2);
	public static final Border TOTAL_DEMAND_BORDER = BorderFactory.createLineBorder(LandingGUI.DARK_GRAY, 3);
	public static final Border RUN_BUTTON_BORDER = BorderFactory.createLineBorder(LandingGUI.DARK_GRAY, 5);
	public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
	
	public static final double MATRIX_SCALING_FACTOR = 3.0; //to multiply OD matrix
	public static final double OPACITY_FACTOR = 3.0; //to multiply opacity for table cells (to emphasise the change)


	/**
	 * Launch the application.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RoadDevelopmentDashboard frame = new RoadDevelopmentDashboard();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @throws IOException if any.
	 * @throws AWTException if any.
	 */
	public RoadDevelopmentDashboard() throws IOException, AWTException {
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("NISMOD v2 Showcase Demo");
		setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		setBounds(100, 100, 939, 352);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(null);
		//this.setSize(1920, 876);
		
		//this.setLocation(0,  (int)Math.round(screenSize.height * 0.65));
		this.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		this.setLocation(0, 0);

		Robot robot = new Robot();
		
		setAlwaysOnTop(true);
		contentPane.setBackground(LandingGUI.LIGHT_GRAY);
	
		//UIManager.put("ToolTip.background", new ColorUIResource(255, 247, 200)); //#fff7c8
		UIManager.put("ToolTip.background", new ColorUIResource(255, 255, 255)); //#fff7c8
		Border border = BorderFactory.createLineBorder(new Color(76,79,83));    //#4c4f53
		UIManager.put("ToolTip.border", border);
		UIManager.put("ToolTip.font", new Font("Calibri Light", Font.BOLD, 16));
		ToolTipManager.sharedInstance().setDismissDelay(15000); // 15 second delay  

		
		createDashboardExplanation();
		
		createBeforeTables();
		
		createAfterTables();
		
		createLabelsLeftOfTables();
		
		//create map panels and legend
		
		panel_1 = new JPanel();
		panel_1.setBounds(BEFORE_MAP_X, BEFORE_MAP_Y, MAP_WIDTH, MAP_HEIGHT);
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//panel_1.setBounds(10, 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_1.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_1);

		panel_2 = new JPanel();
		panel_2.setBounds(AFTER_MAP_X, AFTER_MAP_Y, MAP_WIDTH, MAP_HEIGHT);
		//panel_2.setBounds((int)Math.round(screenSize.width * 0.5), 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_2.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_2);

		JPanel legend = new CapacityUtilisationLegend();
		legend.setBounds(BEFORE_MAP_X, BEFORE_MAP_Y + MAP_HEIGHT, MAP_WIDTH * 2 + BETWEEN_MAP_SPACE, 28);
		contentPane.add(legend);
	

		runModelBeforeIntervention();
		
		//policy design area
		
		JLabel lblRoadExpansionPolicy = new JLabel("Try it yourself!");
		lblRoadExpansionPolicy.setForeground(Color.DARK_GRAY);
		lblRoadExpansionPolicy.setFont(new Font("Lato", Font.BOLD, 26));
		lblRoadExpansionPolicy.setBounds(LEFT_MARGIN, 569, 380, 30);
		contentPane.add(lblRoadExpansionPolicy);

		JComboBox comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(roadNetwork.getNodeIDtoNode().keySet().toArray()));
		//comboBox.setModel(new DefaultComboBoxModel(new String[] {"5", "6", "27", "23", "25"}));
		comboBox.setBounds(LEFT_MARGIN, 728, 149, 22);
		comboBox.setBorder(COMBOBOX_BORDER);
		comboBox.setFont(new Font("Lato", Font.BOLD, 14));
		//comboBox.setBorder(comboBoxBorder);
		//comboBox.setBackground(GUI.DASHBOARD);
		contentPane.add(comboBox);

		int fromNode = (int)comboBox.getSelectedItem();
		DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);
		Set<Integer> listOfNodes = new HashSet<Integer>();
		for (Integer nodeID: roadNetwork.getNodeIDtoNode().keySet()) //first copy all of the nodes
			listOfNodes.add(nodeID);
		//remove fromNode
		listOfNodes.remove(fromNode);
		//remove all related/neighbouring nodes (regardless of direction).
		List edges = nodeA.getEdges();
		for (Object o: edges) {
			Edge e = (Edge) o;
			Node other = e.getOtherNode(nodeA);
			listOfNodes.remove(other.getID());
		}
		Integer[] arrayOfNodes = listOfNodes.toArray(new Integer[0]);
		Arrays.sort(arrayOfNodes);
		JComboBox comboBox_1 = new JComboBox();
		comboBox_1.setModel(new DefaultComboBoxModel(roadNetwork.getNodeIDtoNode().keySet().toArray()));
		comboBox_1.setBounds(SECOND_MARGIN, 728, 149, 22);
		comboBox_1.setFont(new Font("Lato", Font.BOLD, 14));
		comboBox_1.setBorder(COMBOBOX_BORDER);
		contentPane.add(comboBox_1);

		comboBox.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {

				int fromNode = (int)comboBox.getSelectedItem();
				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);
				Set<Integer> listOfNodes = new HashSet<Integer>();
				for (Integer nodeID: roadNetwork.getNodeIDtoNode().keySet()) //first copy all of the nodes
					listOfNodes.add(nodeID);
				//remove fromNode
				listOfNodes.remove(fromNode);
				//remove all related/neighbouring nodes (regardless of direction).
				List edges = nodeA.getEdges();
				for (Object o: edges) {
					Edge e2 = (Edge) o;
					Node other = e2.getOtherNode(nodeA);
					listOfNodes.remove(other.getID());
				}
				Integer[] arrayOfNodes = listOfNodes.toArray(new Integer[0]);
				Arrays.sort(arrayOfNodes);
				comboBox_1.setModel(new DefaultComboBoxModel(arrayOfNodes));
			}
		});
		
		comboBox_1.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {

				int fromNode = (int) comboBox.getSelectedItem();
				int toNode = (int) comboBox_1.getSelectedItem();
	
				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);
				DirectedNode nodeB = (DirectedNode) roadNetwork.getNodeIDtoNode().get(toNode);

				//calculate straight line distance between nodes
				SimpleFeature sf1 = (SimpleFeature)nodeA.getObject();
				Point point = (Point)sf1.getDefaultGeometry();
				SimpleFeature sf2 = (SimpleFeature)nodeB.getObject();
				Point point2 = (Point)sf2.getDefaultGeometry();
				double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!

				//update textbox with new value
				textField.setText(String.format("%.1f ", distance));
			}
		});


		JLabel lblANode = new JLabel("Node A");
		lblANode.setFont(new Font("Lato", Font.PLAIN, 20));
		lblANode.setLabelFor(comboBox);
		lblANode.setBounds(LEFT_MARGIN, 700, 79, 23);
		contentPane.add(lblANode);

		JLabel lblBNode = new JLabel("Node B");
		lblBNode.setFont(new Font("Lato", Font.PLAIN, 20));
		lblBNode.setLabelFor(comboBox_1);
		lblBNode.setBounds(SECOND_MARGIN, 700, 79, 23);
		contentPane.add(lblBNode);

		JSlider slider = new JSlider();
		slider.setFont(new Font("Lato", Font.BOLD, 11));
		slider.setSnapToTicks(true);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMinorTickSpacing(1);
		slider.setValue(1);
		slider.setMajorTickSpacing(1);
		slider.setMaximum(5);
		slider.setMinimum(1);
		slider.setBounds(SECOND_MARGIN, 807, 138, 45);
		slider.setBackground(LandingGUI.LIGHT_GRAY);
		slider.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(slider);

		JLabel lblLanesToAdd = new JLabel("How many lanes?");
		lblLanesToAdd.setFont(new Font("Lato", Font.PLAIN, 16));
		lblLanesToAdd.setBounds(SECOND_MARGIN, 772, 165, 30);
		contentPane.add(lblLanesToAdd);
		
		JRadioButton rdbtnNewRadioButton = new JRadioButton("A-road");
		rdbtnNewRadioButton.setFont(new Font("Lato", Font.PLAIN, 12));
		buttonGroup.add(rdbtnNewRadioButton);
		rdbtnNewRadioButton.setBounds(LEFT_MARGIN, 809, 70, 23);
		contentPane.add(rdbtnNewRadioButton);

		rdbtnNewRadioButton.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnNewRadioButton.isSelected()) slider.setValue(1); //default number of lanes per direction for A roads
			}
		});

		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("Motorway");
		rdbtnNewRadioButton_1.setFont(new Font("Lato", Font.PLAIN, 12));
		buttonGroup.add(rdbtnNewRadioButton_1);
		rdbtnNewRadioButton_1.setBounds(108, 809, 104, 23);
		contentPane.add(rdbtnNewRadioButton_1);

		rdbtnNewRadioButton_1.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnNewRadioButton_1.isSelected()) slider.setValue(3); //default number of lanes per direction for motorways
			}
		});

		JButton btnNewButton = new JButton("RUN");
		btnNewButton.setFont(new Font("Lato", Font.BOLD, 25));
		btnNewButton.setBounds(134, 920, 149, 70);
		btnNewButton.setBorder(RUN_BUTTON_BORDER);
		btnNewButton.setBackground(LandingGUI.LIGHT_GRAY);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				int fromNode = (int) comboBox.getSelectedItem();
				int toNode = (int) comboBox_1.getSelectedItem();
				int lanes = slider.getValue();

				System.out.println("fromNode = " + fromNode);
				System.out.println("toNode = " + toNode);
				System.out.println("lanes = " + lanes);

				String roadClass;
				if (rdbtnNewRadioButton.isSelected()) roadClass = "A";
				else roadClass = "M";

				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);
				DirectedNode nodeB = (DirectedNode) roadNetwork.getNodeIDtoNode().get(toNode);

				//calculate straight line distance between nodes
				SimpleFeature sf1 = (SimpleFeature)nodeA.getObject();
				Point point = (Point)sf1.getDefaultGeometry();
				SimpleFeature sf2 = (SimpleFeature)nodeB.getObject();
				Point point2 = (Point)sf2.getDefaultGeometry();
				double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!

				double length = distance;
				try {
					length = Double.parseDouble(textField.getText());
				} catch (NumberFormatException e) {
					System.err.println("The text box with road distnace has a wrong number format!");
					length = distance;
				}

				if (length < distance) //length should not be smaller than the straight line distance, if it is use straight line distance.
					length = distance;

				//update textbox with new value
				textField.setText(String.format("%.1f ", length));
				
				length = Math.ceil(length * 10) / 10;

				Properties props2 = new Properties();
				props2.setProperty("startYear", "2016");
				props2.setProperty("endYear", "2025");
				props2.setProperty("fromNode", Integer.toString(nodeA.getID()));
				props2.setProperty("toNode", Integer.toString(nodeB.getID()));
				props2.setProperty("biDirectional", "true");
				props2.setProperty("lanesPerDirection", Integer.toString(lanes));
				props2.setProperty("length", Double.toString(length));
				props2.setProperty("roadClass", roadClass);
				RoadDevelopment rd = new RoadDevelopment(props2);

				System.out.println("Road development intervention: " + rd.toString());
				rd.install(roadNetwork);

				final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
				
				//create a road network assignment
				RoadNetworkAssignment rnaAfterDevelopment = new RoadNetworkAssignment(roadNetwork,
						yearToUnitCO2Emissions.get(BASE_YEAR),
						yearToEnergyUnitCosts.get(BASE_YEAR),
						yearToEngineTypeFractions.get(BASE_YEAR),
						yearToAVFractions.get(BASE_YEAR),
						vehicleTypeToPCU,
						baseFuelConsumptionRates,
						relativeFuelEfficiency.get(BASE_YEAR),
						timeOfDayDistribution.get(BASE_YEAR),
						timeOfDayDistributionFreight.get(BASE_YEAR),
						null,
						null,
						null,
						null,
						props);

				//rsg.generateRouteSetForODMatrix(odm);
				rsg.clearRoutes();
				//rnaAfterDevelopment.assignPassengerFlowsRouteChoice(odm, rsg, props);
				rnaAfterDevelopment.assignPassengerFlowsRouting(odm, rsg, props);
				rnaAfterDevelopment.updateLinkVolumeInPCU();
				rnaAfterDevelopment.updateLinkVolumeInPCUPerTimeOfDay();

				//predict change in demand
				SkimMatrix tsm = rnaAfterDevelopment.calculateTimeSkimMatrix();
				SkimMatrix csm = rnaAfterDevelopment.calculateCostSkimMatrix();

				//predicted demand	
				ODMatrix predictedODM = new ODMatrix();

				final String elasticitiesFile = props.getProperty("elasticitiesFile");
				HashMap<ElasticityTypes, Double> elasticities = InputFileReader.readElasticitiesFile(elasticitiesFile);

				tsmBefore.printMatrixFormatted();
				tsm.printMatrixFormatted();

				//for each OD pair predict the change in passenger vehicle flow from the change in skim matrices
				for (MultiKey mk: odm.getKeySet()) {
					String originZone = (String) mk.getKey(0);
					String destinationZone = (String) mk.getKey(1);

					double oldFlow = odm.getFlow(originZone, destinationZone);

					double oldODTravelTime = tsmBefore.getCost(originZone, destinationZone);
					double newODTravelTime = tsm.getCost(originZone, destinationZone);
					double oldODTravelCost = csmBefore.getCost(originZone, destinationZone);
					double newODTravelCost = csm.getCost(originZone, destinationZone);

					double predictedflow = oldFlow * Math.pow(newODTravelTime / oldODTravelTime, elasticities.get(ElasticityTypes.TIME)) *
							Math.pow(newODTravelCost / oldODTravelCost, elasticities.get(ElasticityTypes.COST));

					predictedODM.setFlow(originZone, destinationZone, (int) Math.round(predictedflow));
				}

				rnaAfterDevelopment.resetLinkVolumes();
				rnaAfterDevelopment.resetTripStorages();

				rsg.clearRoutes();
				rnaAfterDevelopment.assignPassengerFlowsRouting(predictedODM, rsg, props);
				rnaAfterDevelopment.updateLinkVolumeInPCU();
				rnaAfterDevelopment.updateLinkVolumeInPCUPerTimeOfDay();
				//SkimMatrix sm = rnaAfterDevelopment.calculateTimeSkimMatrix();

				//update bar chart
				//barDataset.addValue(rnaAfterDevelopment.getTripList().size(), "Road development", "Number of Trips");
				
				HashMap<Integer, Double> capacityAfter = rnaAfterDevelopment.calculateDirectionAveragedPeakLinkCapacityUtilisation();
				String shapefilePathAfter = "./temp/after" +  LandingGUI.counter++ + ".shp";
				JFrame rightFrame;
				try {
					rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
					rightFrame.setVisible(true);
					//rightFrame.repaint();
					//	panel_2.add(rightFrame.getContentPane());
					//panel_2.removeAll();
					panel_2.add(rightFrame.getContentPane(), 0);
					panel_2.setLayout(null);
					//panel_2.setComponentZOrder(labelPanel2, 0);
					//panel_2.doLayout();
					//panel_2.repaint();
					
					//find JPanel
					JPanel jp = null;
					for (Component comp: rightFrame.getContentPane().getComponents())
						if (comp instanceof JPanel) {
							jp = (JPanel) comp;
							break;
						}
					//find toolbar
					JToolBar tb = null;
					for (Component comp: jp.getComponents())
						if (comp instanceof JToolBar) {
							tb = (JToolBar) comp;
							break;
						}
					tb.setBackground(LandingGUI.LIGHT_GRAY); //to set toolbar background
					tb.setBorder(BorderFactory.createLineBorder(LandingGUI.LIGHT_GRAY, 1));
	
					rightFrame.setVisible(true);
					rightFrame.setVisible(false);
					
					labelPanel2 = new JLabel("After Policy Intervention");
					labelPanel2.setBounds(301, 11, 331, 20);
					panel_2.add(labelPanel2);
					panel_2.setComponentZOrder(labelPanel2, 0);
					labelPanel2.setForeground(LandingGUI.DARK_GRAY);
					labelPanel2.setFont(new Font("Lato", Font.BOLD, 16));
							
					robot.mouseMove(1300, 600);
								
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//update after tables
				int rows = predictedODM.getSortedOrigins().size();
				int columns = predictedODM.getSortedDestinations().size();
				Object[][] data = new Object[rows][columns + 1];
				for (int i = 0; i < rows; i++) {
					data[i][0] = zoning.getLADToName().get(predictedODM.getSortedOrigins().get(i));
					for (int j = 0; j < columns; j++) {
						data[i][j+1] = predictedODM.getFlow(predictedODM.getSortedOrigins().get(i), predictedODM.getSortedDestinations().get(j));
					}
				}
				String[] labels = new String[columns + 1];
				labels[0] = "ORIG \\ DEST";
				for (int j = 0; j < columns; j++) labels[j+1] = zoning.getLADToName().get(predictedODM.getSortedDestinations().get(j));
				table_2.setModel(new DefaultTableModel(data, labels));


				SkimMatrix sm = rnaAfterDevelopment.calculateTimeSkimMatrix();
				rows = sm.getOrigins().size();
				columns = sm.getDestinations().size();
				Object[][] data2 = new Object[rows][columns + 1];
				for (int i = 0; i < rows; i++) {
					data2[i][0] = zoning.getLADToName().get(sm.getOrigins().get(i));
					for (int j = 0; j < columns; j++) {
						data2[i][j+1] = String.format("%.2f", sm.getCost(sm.getOrigins().get(i), sm.getDestinations().get(j)));
					}
				}
				String[] labels2 = new String[columns + 1];
				labels2[0] = "ORIG \\ DEST";
				for (int j = 0; j < columns; j++) labels2[j+1] = zoning.getLADToName().get(sm.getDestinations().get(j));
				table_3.setModel(new DefaultTableModel(data2, labels2));

				/*
				//update bar chart
				barDataset.addValue(rnaAfterDevelopment.getTripList().size(), "Road development", "Number of Trips");
				//barDataset.addValue(predictedODM.getTotalFlow(), "Road expansion", "Number of Trips");
				*/
				
				//update text cell with total demand
				totalDemandAfter.setText(String.valueOf(rnaAfterDevelopment.getTripList().size()));
				
				rd.uninstall(roadNetwork);

				//pack();
			}
		});
		contentPane.add(btnNewButton);

		JLabel label_1 = new JLabel("<html>Length [<i>km</i>]:</html>");
		label_1.setFont(new Font("Lato", Font.PLAIN, 16));
		label_1.setBounds(LEFT_MARGIN, 853, 101, 30);
		contentPane.add(label_1);
		
		textField = new JTextField();
		textField.setHorizontalAlignment(SwingConstants.RIGHT);
		textField.setText("10.0");
		textField.setFont(new Font("Lato", Font.BOLD, 12));
		textField.setBorder(COMBOBOX_BORDER);
		textField.setColumns(10);
		textField.setBounds(135, 855, 50, 30);
		contentPane.add(textField);
		
//		final String roadDevelopmentFileName = props.getProperty("roadDevelopmentFile");
//		RoadDevelopment rd = new RoadDevelopment(roadDevelopmentFileName);
//		System.out.println("Road development intervention: " + rd.toString());
//		rd.install(roadNetwork);

		//set controls to represent the intervention
		ActionListener al = comboBox.getActionListeners()[0];
		comboBox.removeActionListener(al); //temporarilly
		comboBox.setSelectedItem(new Integer(63));
		comboBox_1.setSelectedItem(new Integer(23));
		comboBox.addActionListener(al);
		rdbtnNewRadioButton.setSelected(true);
		slider.setValue(2);
		textField.setText("10.2");
		
		//run the default intervention
		btnNewButton.doClick();

		pack();
		//this.setExtendedState(this.getExtendedState()|JFrame.MAXIMIZED_BOTH );
	}
	
	
	private void createDashboardExplanation( ) {
		
	
		
		//coloured panel with icon
		JPanel panel_4 = new JPanel();
		panel_4.setBackground(LandingGUI.PASTEL_YELLOW);
		panel_4.setBounds(LEFT_MARGIN, 34, 346, 123);
		contentPane.add(panel_4);
		panel_4.setLayout(null);
		
		JLabel lblNewLabel_2 = new JLabel("<html><left>Intervention 2:<br>Road Development</html>");
		lblNewLabel_2.setBounds(116, 5, 220, 115);
		lblNewLabel_2.setFont(new Font("Lato", Font.BOLD, 26));
		panel_4.add(lblNewLabel_2);
		
		File imgRoad = new File("./src/test/resources/images/roadworksIcon.png");
		BufferedImage bufferedImageRoad = null;
		try {
			bufferedImageRoad = Sanselan.getBufferedImage(imgRoad);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//BufferedImage subImage = bufferedImage.getSubimage(0, 10, bufferedImage.getWidth(), bufferedImage.getHeight() - 20); //trimming
		Image newimgRoad = bufferedImageRoad.getScaledInstance(80, 80, java.awt.Image.SCALE_SMOOTH); //scaling  
		ImageIcon iconRoad = new ImageIcon(newimgRoad);
		
		JLabel lblNewLabel_3 = new JLabel(iconRoad);
		lblNewLabel_3.setBounds(17, 20, 80, 80);
		panel_4.add(lblNewLabel_3);
				
		StringBuilder html = new StringBuilder();
		html.append("<html><left>");
		html.append("<font size=+1><b>What we asked:</b></font><br>");
		html.append("<font size=+1>What happens when we build completely new roads?</font><br><br>");
		html.append("<font size=+1><b>What we found:</b></font><br>");
		html.append("<ul>");
		html.append("<li><font size=+1>Lower road capacity utilisation in a wider area compared to road expansion.</font>");
		html.append("<li><font size=+1>Increase in vehicle ﬂows.</font>");
		html.append("<li><font size=+1>Decrease in travel times.</font>");
		html.append("</ul></html>");
		
		JLabel lblNewLabel_4 = new JLabel(html.toString());
		lblNewLabel_4.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel_4.setFont(new Font("Lato", Font.PLAIN, 20));
		lblNewLabel_4.setBounds(LEFT_MARGIN, 200, 346, 346);
		contentPane.add(lblNewLabel_4);
		
		JSeparator separator = new JSeparator();
		separator.setForeground(Color.GRAY);
		separator.setBounds(LEFT_MARGIN, LandingGUI.SCREEN_HEIGHT / 2, 346, 2);
		contentPane.add(separator);
		
		JLabel lblNewLabel_5 = new JLabel("<html><left>Create a new road by selecting two nodes indicated on the \"before\" map:</html>");
		lblNewLabel_5.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_5.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel_5.setFont(new Font("Lato", Font.PLAIN, 20));
		lblNewLabel_5.setBounds(LEFT_MARGIN, 631, 346, 100);
		contentPane.add(lblNewLabel_5);
		
		JLabel lblHowManyDirections = new JLabel("What road class?");
		lblHowManyDirections.setFont(new Font("Lato", Font.PLAIN, 16));
		lblHowManyDirections.setBounds(LEFT_MARGIN, 772, 178, 30);
		contentPane.add(lblHowManyDirections);
		
		JLabel lblNewLabel_6 = new JLabel("Observe the change in capacity utilisation in the \"after\" map");
		lblNewLabel_6.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_6.setFont(new Font("Lato", Font.PLAIN, 12));
		lblNewLabel_6.setBounds(LEFT_MARGIN, 1006, 342, 14);
		contentPane.add(lblNewLabel_6);
				
	}
	
	private void createBeforeTables() {
		
		JLabel lblBeforePolicyIntervention = new JLabel("Before Policy Intervention");
		lblBeforePolicyIntervention.setLabelFor(table);
		lblBeforePolicyIntervention.setForeground(LandingGUI.DARK_GRAY);
		lblBeforePolicyIntervention.setBounds(BEFORE_MAP_X, MAP_HEIGHT + 55, 392, 30);
		contentPane.add(lblBeforePolicyIntervention);
		lblBeforePolicyIntervention.setFont(new Font("Lato", Font.BOLD, 18));
		
		scrollPane = new JScrollPane();
		scrollPane.setBounds(BEFORE_MAP_X + TABLE_LABEL_WIDTH, MAP_HEIGHT + 100, 416, 90);
		contentPane.add(scrollPane);
		table = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
				component.setOpaque(true);
				if (columnIndex == 0)  { 
					component.setBackground(LandingGUI.MID_GRAY);
				} else {
					component.setBackground(Color.WHITE);
				}

				return component;
			}
		};
		scrollPane.setViewportView(table);
		table.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "2100", "1343", "4321", "1234"},
					{"New Forest", "4252", "623", "1425", "653"},
					{"Eeastleigh", "6534", "2345", "541", "6327"},
					{"Isle of Wight", "2345", "235", "52", "435"},
				},
				new String[] {
						"ORIG-DEST", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));
		final TableCellRenderer tcr = table.getTableHeader().getDefaultRenderer();
		table.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, 
					Object value, boolean isSelected, boolean hasFocus, 
					int row, int column) {
				JLabel lbl = (JLabel) tcr.getTableCellRendererComponent(table, 
						value, isSelected, hasFocus, row, column);

				lbl.setBackground(LandingGUI.MID_GRAY);
				lbl.setBorder(TABLE_BORDER);
				//lbl.setBorder(BorderFactory.createCompoundBorder(TABLE_BORDER, BorderFactory.createEmptyBorder(0, 0, 0, 0)));
				//	                lbl.setHorizontalAlignment(SwingConstants.LEFT);
				//	                if (isSelected) {
				//	                    lbl.setForeground(Color.red);
				//	                    lbl.setBackground(Color.lightGray);
				//	                } else {
				//	                    lbl.setForeground(Color.blue);
				//	                    lbl.setBackground(Color.black);
				//	                }
				return lbl;
			}
		});
		table.setBackground(Color.WHITE);
		table.setGridColor(LandingGUI.DARK_GRAY);
		table.setFont(TABLE_FONT);
		table.getTableHeader().setFont(TABLE_FONT);
		table.getTableHeader().setBackground(LandingGUI.MID_GRAY);
		table.getTableHeader().setPreferredSize(new Dimension(scrollPane.getWidth(), TABLE_ROW_HEIGHT));
		table.setRowHeight(TABLE_ROW_HEIGHT);
		table.setBorder(TABLE_BORDER);
		table.getTableHeader().setBorder(TABLE_BORDER);
		scrollPane.setBorder(EMPTY_BORDER);

		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(BEFORE_MAP_X + TABLE_LABEL_WIDTH, scrollPane.getY() + scrollPane.getHeight() + TABLE_ROW_HEIGHT, 416, 90);
		contentPane.add(scrollPane_1);
		table_1 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
				component.setOpaque(true);

				if (columnIndex == 0)  { 
					component.setBackground(LandingGUI.MID_GRAY);
				} else {
					component.setBackground(Color.WHITE);
				}
				return component;
			}
		};
		scrollPane_1.setViewportView(table_1);
		table_1.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "10.0", "13.4", "43.1", "12.4"},
					{"New Forest", "3.5", "6.2", "14.2", "6.5"},
					{"Eeastleigh", "15.3", "23.4", "5.4", "6.3"},
					{"Isle of Wight", "23.5", "35.7", "25.2", "14.6"},
				},
				new String[] {
						"ORIG DEST", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));
		final TableCellRenderer tcr_1 = table_1.getTableHeader().getDefaultRenderer();
		table_1.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, 
					Object value, boolean isSelected, boolean hasFocus, 
					int row, int column) {
				JLabel lbl = (JLabel) tcr_1.getTableCellRendererComponent(table, 
						value, isSelected, hasFocus, row, column);

				lbl.setBackground(LandingGUI.MID_GRAY);
				lbl.setBorder(TABLE_BORDER);
				return lbl;
			}
		});
		table_1.setBackground(Color.WHITE);
		table_1.setGridColor(LandingGUI.DARK_GRAY);
		table_1.setFont(TABLE_FONT);
		table_1.getTableHeader().setFont(TABLE_FONT);
		table_1.getTableHeader().setBackground(LandingGUI.MID_GRAY);
		table_1.getTableHeader().setPreferredSize(new Dimension(scrollPane_1.getWidth(), TABLE_ROW_HEIGHT));
		table_1.setBorder(TABLE_BORDER);
		table_1.getTableHeader().setBorder(TABLE_BORDER);
		table_1.setRowHeight(TABLE_ROW_HEIGHT);
		scrollPane_1.setBorder(EMPTY_BORDER);
		//scrollPane_1.setViewportBorder(tableBorder);

		scrollPane_2 = new JScrollPane();
		scrollPane_2.setToolTipText("This shows the impact on demand.");
		scrollPane_2.setBounds(AFTER_MAP_X + TABLE_LABEL_WIDTH, MAP_HEIGHT + 100, 416, 90);
		contentPane.add(scrollPane_2);
		
		//total demand panel (before)
		JPanel panel_3 = new JPanel();
		panel_3.setLayout(null);
		panel_3.setBorder(TABLE_BORDER);
		panel_3.setBackground(Color.WHITE);
		panel_3.setBounds(scrollPane.getX() + scrollPane.getWidth() - 1, scrollPane.getY(), 222, scrollPane.getHeight());
		contentPane.add(panel_3);
		
		totalDemandBefore = new JTextField();
		totalDemandBefore.setHorizontalAlignment(SwingConstants.RIGHT);
		totalDemandBefore.setFont(new Font("Lato", Font.BOLD, 24));
		totalDemandBefore.setColumns(5);
		totalDemandBefore.setBorder(TOTAL_DEMAND_BORDER);
		totalDemandBefore.setBounds(86, 37, 114, 31);
		panel_3.add(totalDemandBefore);
		
		JLabel label_1 = new JLabel("(Total Number of Trips)");
		label_1.setFont(new Font("Lato", Font.BOLD, 11));
		label_1.setBounds(86, 11, 126, 14);
		panel_3.add(label_1);
		
		JLabel label_2 = new JLabel("Demand");
		label_2.setFont(new Font("Lato", Font.BOLD, 14));
		label_2.setBounds(15, 9, 66, 14);
		panel_3.add(label_2);
		
		File img = new File("./src/test/resources/images/car.png");
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = Sanselan.getBufferedImage(img);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage subImage = bufferedImage.getSubimage(10, 20, bufferedImage.getWidth() - 10, bufferedImage.getHeight() - 20); //trimming
		Image newimg = subImage.getScaledInstance(70, 70, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon icon = new ImageIcon(newimg);
		
		JLabel label_3 = new JLabel(icon);
		label_3.setBounds(10, 30, 70, 45);
		panel_3.add(label_3);
	
	}
	
	
	private void createAfterTables() {
		
		JLabel lblAfterPolicyIntervention = new JLabel("After Policy Intervention");
		lblAfterPolicyIntervention.setLabelFor(table_2);
		lblAfterPolicyIntervention.setForeground(LandingGUI.DARK_GRAY);
		lblAfterPolicyIntervention.setFont(new Font("Lato", Font.BOLD, 18));
		lblAfterPolicyIntervention.setBounds(AFTER_MAP_X, MAP_HEIGHT + 55, 392, 30);
		contentPane.add(lblAfterPolicyIntervention);
		
		table_2 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
				component.setOpaque(true);
				
				if (columnIndex == 0)  { 
					component.setBackground(LandingGUI.MID_GRAY);
				} else {
					int newValue = Integer.parseInt(getValueAt(rowIndex, columnIndex).toString());
					int oldValue = Integer.parseInt(table.getValueAt(rowIndex, columnIndex).toString());

									double absolutePercentChange = Math.abs((1.0 * newValue / oldValue - 1.0) * 100);
					int opacity = (int) Math.round(absolutePercentChange * OPACITY_FACTOR); //amplify the change 
					if (opacity > 255) opacity = 255;
					
					Color inc = LandingGUI.PASTEL_BLUE;
					Color dec = LandingGUI.PASTEL_YELLOW;
					Color increase = new Color (inc.getRed(), inc.getGreen(), inc.getBlue(), opacity);
					Color decrease = new Color (dec.getRed(), dec.getGreen(), dec.getBlue(), opacity);
					
					if (newValue > oldValue) component.setBackground(increase);
					else if (newValue < oldValue) component.setBackground(decrease);
					else component.setBackground(Color.WHITE);
				}
				return component;
			}
		};
		final TableCellRenderer tcr_2 = table_2.getTableHeader().getDefaultRenderer();
		table_2.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, 
					Object value, boolean isSelected, boolean hasFocus, 
					int row, int column) {
				JLabel lbl = (JLabel) tcr_2.getTableCellRendererComponent(table, 
						value, isSelected, hasFocus, row, column);

				lbl.setBackground(LandingGUI.MID_GRAY);
				lbl.setBorder(TABLE_BORDER);
				return lbl;
			}
		});
		table_2.setBackground(Color.WHITE);
		table_2.setGridColor(LandingGUI.DARK_GRAY);
		table_2.setFont(TABLE_FONT);
		table_2.getTableHeader().setFont(TABLE_FONT);
		table_2.setRowHeight(TABLE_ROW_HEIGHT);
		table_2.getTableHeader().setBackground(LandingGUI.MID_GRAY);
		table_2.getTableHeader().setPreferredSize(new Dimension(scrollPane_2.getWidth(), TABLE_ROW_HEIGHT));
		table_2.setBorder(TABLE_BORDER);
		table_2.getTableHeader().setBorder(TABLE_BORDER);
		scrollPane_2.setBorder(EMPTY_BORDER);
		//table_2.setOpaque(false);
		//((JComponent)table_2.getDefaultRenderer(Object.class)).setOpaque(false);
		//scrollPane_2.setOpaque(false);
		//scrollPane_2.getViewport().setOpaque(false);
		scrollPane_2.setViewportView(table_2);
		table_2.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "2100", "1343", "4321", "1234"},
					{"New Forest", "4253", "623", "1420", "653"},
					{"Eeastleigh", "6534", "2346", "541", "6327"},
					{"Isle of Wight", "2345", "234", "52", "435"},
				},
				new String[] {
						"ORIG DEST", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));


		scrollPane_3 = new JScrollPane();
		scrollPane_3.setBounds(AFTER_MAP_X + TABLE_LABEL_WIDTH, scrollPane_2.getY() + scrollPane_2.getHeight() + TABLE_ROW_HEIGHT, 416, 90);
		contentPane.add(scrollPane_3);
		table_3 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
			
				if (columnIndex == 0)  { 
					//component.setBackground(new Color(0, 0, 0, 20));
					component.setBackground(LandingGUI.MID_GRAY);
				} else {
					double newValue = Double.parseDouble(getValueAt(rowIndex, columnIndex).toString());
					double oldValue = Double.parseDouble(table_1.getValueAt(rowIndex, columnIndex).toString());
					
					double absolutePercentChange = Math.abs((1.0 * newValue / oldValue - 1.0) * 100);
					int opacity = (int) Math.round(absolutePercentChange * OPACITY_FACTOR); // //amplify the change
					if (opacity > 255) opacity = 255;
					
					Color inc = LandingGUI.PASTEL_BLUE;
					Color dec = LandingGUI.PASTEL_YELLOW;
					Color increase = new Color (inc.getRed(), inc.getGreen(), inc.getBlue(), opacity);
					Color decrease = new Color (dec.getRed(), dec.getGreen(), dec.getBlue(), opacity);
					
					if (newValue > oldValue) component.setBackground(increase);
					else if (newValue < oldValue) component.setBackground(decrease);
					else component.setBackground(Color.WHITE);
				}
				return component;
			}
		};

		table_3.setBackground(Color.WHITE);
		table_3.setBackground(Color.WHITE);
		table_3.setGridColor(LandingGUI.DARK_GRAY);
		table_3.setFont(TABLE_FONT);
		table_3.getTableHeader().setFont(TABLE_FONT);
		table_3.setRowHeight(TABLE_ROW_HEIGHT);
		table_3.getTableHeader().setBackground(LandingGUI.MID_GRAY);
		table_3.getTableHeader().setPreferredSize(new Dimension(scrollPane_3.getWidth(), TABLE_ROW_HEIGHT));
		table_3.setBorder(TABLE_BORDER);
		table_3.getTableHeader().setBorder(TABLE_BORDER);
		scrollPane_3.setBorder(EMPTY_BORDER);

		scrollPane_3.setViewportView(table_3);
		table_3.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "10.0", "13.4", "43.1", "12.4"},
					{"New Forest", "3.5", "6.2", "14.2", "6.5"},
					{"Eeastleigh", "15.3", "23.4", "5.4", "6.3"},
					{"Isle of Wight", "23.5", "35.7", "25.2", "14.6"},
				},
				new String[] {
						"ORIG DEST", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));
		final TableCellRenderer tcr_3 = table_3.getTableHeader().getDefaultRenderer();
		table_3.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, 
					Object value, boolean isSelected, boolean hasFocus, 
					int row, int column) {
				JLabel lbl = (JLabel) tcr_3.getTableCellRendererComponent(table, 
						value, isSelected, hasFocus, row, column);

				lbl.setBackground(LandingGUI.MID_GRAY);
				lbl.setBorder(TABLE_BORDER);
				return lbl;
			}
		});
		
		//total demand panel (after)
		JPanel panel = new JPanel();
		panel.setBorder(TABLE_BORDER);
		panel.setBackground(Color.WHITE);
		panel.setBounds(scrollPane_2.getX() + scrollPane_2.getWidth() - 1, scrollPane_2.getY(), 222, scrollPane_2.getHeight());
		contentPane.add(panel);
		panel.setLayout(null);
		
		totalDemandAfter = new JTextField();
		totalDemandAfter.setBounds(86, 37, 114, 31);
		totalDemandAfter.setFont(new Font("Lato", Font.BOLD, 24));
		totalDemandAfter.setHorizontalAlignment(SwingConstants.RIGHT);
		totalDemandAfter.setText("70000");
		panel.add(totalDemandAfter);
		totalDemandAfter.setColumns(5);
		totalDemandAfter.setBorder(TOTAL_DEMAND_BORDER);
		
		JLabel lblNewLabel = new JLabel("(Total Number of Trips)");
		lblNewLabel.setFont(new Font("Lato", Font.BOLD, 11));
		lblNewLabel.setBounds(86, 11, 126, 14);
		panel.add(lblNewLabel);
		
		JLabel lblDemand = new JLabel("Demand");
		lblDemand.setFont(new Font("Lato", Font.BOLD, 14));
		lblDemand.setBounds(15, 11, 66, 14);
		panel.add(lblDemand);
		
		File img = new File("./src/test/resources/images/car.png");
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = Sanselan.getBufferedImage(img);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage subImage = bufferedImage.getSubimage(10, 20, bufferedImage.getWidth() - 10, bufferedImage.getHeight() - 20); //trimming
		Image newimg = subImage.getScaledInstance(70, 70, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon icon = new ImageIcon(newimg);
		
		JLabel lblNewLabel_1 = new JLabel(icon);
		lblNewLabel_1.setBounds(10, 30, 70, 45);
		panel.add(lblNewLabel_1);
		
		JPanel tableChangeLegend = new TableChangeLegend();
		tableChangeLegend.setBounds(scrollPane_3.getX() + scrollPane_3.getWidth() + 7, scrollPane_3.getY() + 10, 165, 77);
		contentPane.add(tableChangeLegend);
	}
	
	
	private void createLabelsLeftOfTables () {
		
		JLabel lblTrips = new JLabel("<html><center>OD Matrix<br>[<i>trips</i>]</html>");
		lblTrips.setVerticalAlignment(SwingConstants.TOP);
		lblTrips.setHorizontalAlignment(SwingConstants.CENTER);
		lblTrips.setFont(new Font("Lato", Font.BOLD, 13));
		lblTrips.setForeground(LandingGUI.DARK_GRAY);
		lblTrips.setBounds(BEFORE_MAP_X, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 40);
		contentPane.add(lblTrips);
		
		JLabel lblTrips_1 = new JLabel("<html><center>OD Matrix<br>[<i>trips</i>]</html>");
		lblTrips_1.setVerticalAlignment(SwingConstants.TOP);
		lblTrips_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblTrips_1.setFont(new Font("Lato", Font.BOLD, 13));
		lblTrips_1.setForeground(LandingGUI.DARK_GRAY);
		lblTrips_1.setBounds(AFTER_MAP_X, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 40);
		contentPane.add(lblTrips_1);
		
		File imgCars = new File("./src/test/resources/images/cars.png");
		BufferedImage bufferedImageCars = null;
		try {
			bufferedImageCars = Sanselan.getBufferedImage(imgCars);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage subImageCars = bufferedImageCars.getSubimage(0, 20, bufferedImageCars.getWidth(), bufferedImageCars.getHeight() - 20); //trimming
		Image newimgCars = subImageCars.getScaledInstance(100, 100, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon iconCars = new ImageIcon(newimgCars);
		
		JLabel lblCars = new JLabel(iconCars);
		lblCars.setVerticalAlignment(SwingConstants.TOP);
		lblCars.setHorizontalAlignment(SwingConstants.CENTER);
		lblCars.setBounds(BEFORE_MAP_X, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 100);
		contentPane.add(lblCars);
		
		JLabel lblCars_1 = new JLabel(iconCars);
		lblCars_1.setVerticalAlignment(SwingConstants.TOP);
		lblCars_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblCars_1.setBounds(AFTER_MAP_X, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 100);
		contentPane.add(lblCars_1);
		
		JLabel lblTime = new JLabel("<html><center>Travel Time<br>[<i>min</i>]</html>");
		lblTime.setVerticalAlignment(SwingConstants.TOP);
		lblTime.setHorizontalAlignment(SwingConstants.CENTER);
		lblTime.setFont(new Font("Lato", Font.BOLD, 13));
		lblTime.setBounds(BEFORE_MAP_X, scrollPane_1.getY(), TABLE_LABEL_WIDTH, 40);
		contentPane.add(lblTime);
		
		JLabel lblTime_1 = new JLabel("<html><center>Travel Time<br>[<i>min</i>]</html>");
		lblTime_1.setVerticalAlignment(SwingConstants.TOP);
		lblTime_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblTime_1.setFont(new Font("Lato", Font.BOLD, 13));
		lblTime_1.setBounds(AFTER_MAP_X, scrollPane_3.getY(), TABLE_LABEL_WIDTH, 40);
		contentPane.add(lblTime_1);
		
		File imgClock = new File("./src/test/resources/images/clock.png");
		BufferedImage bufferedImageClock = null;
		try {
			bufferedImageClock = Sanselan.getBufferedImage(imgClock);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedImage subImageClock = bufferedImageClock.getSubimage(0, 20, bufferedImageCars.getWidth(), bufferedImageCars.getHeight() - 20); //trimming
		Image newimgClock = subImageClock.getScaledInstance(75, 75, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon iconClock = new ImageIcon(newimgClock);
		
		JLabel lblClock = new JLabel(iconClock);
		lblClock.setVerticalAlignment(SwingConstants.TOP);
		lblClock.setHorizontalAlignment(SwingConstants.CENTER);
		lblClock.setBounds(BEFORE_MAP_X, scrollPane_1.getY() + 20, TABLE_LABEL_WIDTH, 100);
		contentPane.add(lblClock);
		
		JLabel lblClock_1 = new JLabel(iconClock);
		lblClock_1.setVerticalAlignment(SwingConstants.TOP);
		lblClock_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblClock_1.setBounds(AFTER_MAP_X, scrollPane_3.getY() + 20, TABLE_LABEL_WIDTH, 100);
		contentPane.add(lblClock_1);
		
	}
	
	private void runModelBeforeIntervention() throws IOException {
		
		props = ConfigReader.getProperties(configFile);

		final String areaCodeFileName = props.getProperty("areaCodeFileName");
		final String areaCodeNearestNodeFile = props.getProperty("areaCodeNearestNodeFile");
		final String workplaceZoneFileName = props.getProperty("workplaceZoneFileName");
		final String workplaceZoneNearestNodeFile = props.getProperty("workplaceZoneNearestNodeFile");
		final String freightZoneToLADfile = props.getProperty("freightZoneToLADfile");
		final String freightZoneNearestNodeFile = props.getProperty("freightZoneNearestNodeFile");

		final URL zonesUrl = new URL(props.getProperty("zonesUrl"));
		final URL networkUrl = new URL(props.getProperty("networkUrl"));
		final URL networkUrlFixedEdgeIDs = new URL(props.getProperty("networkUrlFixedEdgeIDs"));
		final URL nodesUrl = new URL(props.getProperty("nodesUrl"));
		final URL AADFurl = new URL(props.getProperty("AADFurl"));

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");

		roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String timeOfDayDistributionFreightFile = props.getProperty("timeOfDayDistributionFreightFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
		
		vehicleTypeToPCU = InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile);
		baseFuelConsumptionRates = InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile);
		relativeFuelEfficiency = InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile);
		timeOfDayDistribution = InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile);
		timeOfDayDistributionFreight = InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFreightFile);
		yearToEnergyUnitCosts = InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile);
		yearToUnitCO2Emissions = InputFileReader.readUnitCO2EmissionFile(unitCO2EmissionsFile);
		yearToEngineTypeFractions = InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile);
		yearToAVFractions = InputFileReader.readAVFractionsFile(AVFractionsFile);
	
		//create a road network assignment
		RoadNetworkAssignment rnaBefore = new RoadNetworkAssignment(roadNetwork, 
															yearToEnergyUnitCosts.get(BASE_YEAR),
															yearToUnitCO2Emissions.get(BASE_YEAR),
															yearToEngineTypeFractions.get(BASE_YEAR),
															yearToAVFractions.get(BASE_YEAR),
															vehicleTypeToPCU,
															baseFuelConsumptionRates,
															relativeFuelEfficiency.get(BASE_YEAR),
															timeOfDayDistribution.get(BASE_YEAR),
															timeOfDayDistributionFreight.get(BASE_YEAR),
															null,
															null,
															null,
															null,
															props);

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);

		odm = new ODMatrix(baseYearODMatrixFile); 
		odm.scaleMatrixValue(MATRIX_SCALING_FACTOR);
		rsg = new RouteSetGenerator(roadNetwork, props);

		rnaBefore.assignPassengerFlowsRouting(odm, rsg, props);
		rnaBefore.updateLinkTravelTimes(0.9);

		rnaBefore.resetLinkVolumes();
		rnaBefore.resetTripStorages();
		rnaBefore.assignPassengerFlowsRouting(odm, rsg, props);
		rnaBefore.updateLinkTravelTimes(0.9);

		rnaBefore.updateLinkVolumeInPCU();
		rnaBefore.updateLinkVolumeInPCUPerTimeOfDay();

		/*
		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Number of Trips");
		*/
		
		tsmBefore = rnaBefore.calculateTimeSkimMatrix();
		csmBefore = rnaBefore.calculateCostSkimMatrix();
		
		File directory = new File("temp");
		if (!directory.exists()) directory.mkdir();

		String shapefilePathBefore = "./temp/networkWithCapacityUtilisationBefore.shp";
		//String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";

		HashMap<Integer, Double> capacityBefore = rnaBefore.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame leftFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation Before Intervention", capacityBefore, "CapUtil", shapefilePathBefore);
	
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//leftFrame.setSize(screenSize.width / 2, (int)Math.round(screenSize.height * 0.65));
		//leftFrame.setLocation(0, 0);
		leftFrame.setVisible(false);
		panel_1.add(leftFrame.getContentPane());
		panel_1.setLayout(null);
		
		leftFrame.setVisible(true);
		leftFrame.setVisible(false);

		//find JPanel in leftFrame
		JPanel jp = null;
		for (Component comp: leftFrame.getContentPane().getComponents())
			if (comp instanceof JPanel) {
				jp = (JPanel) comp;
				break;
			}
		//find toolbar
		JToolBar tb = null;
		for (Component comp: jp.getComponents())
			if (comp instanceof JToolBar) {
				tb = (JToolBar) comp;
				break;
			}
		tb.setBackground(LandingGUI.LIGHT_GRAY); //to set toolbar background
		tb.setBorder(BorderFactory.createLineBorder(LandingGUI.LIGHT_GRAY, 1));
				
		JLabel labelPanel1 = new JLabel("Before Policy Intervention");
		labelPanel1.setBounds(301, 11, 331, 20);
		panel_1.add(labelPanel1);
		panel_1.setComponentZOrder(labelPanel1, 0);
		labelPanel1.setForeground(LandingGUI.DARK_GRAY);
		labelPanel1.setFont(new Font("Lato", Font.BOLD, 16));
		
		labelPanel2 = new JLabel("After Policy Intervention");
		labelPanel2.setBounds(301, 11, 331, 20);
		panel_2.add(labelPanel2);
		panel_2.setComponentZOrder(labelPanel2, 0);
		labelPanel2.setForeground(LandingGUI.DARK_GRAY);
		labelPanel2.setFont(new Font("Lato", Font.BOLD, 16));
	
		//JFrame rightFrame;
		//panel_2.add(rightFrame.getContentPane());
		//panel_2.setLayout(null);
		
		//update tables

		int rows = odm.getSortedOrigins().size();
		int columns = odm.getSortedDestinations().size();
		Object[][] data = new Object[rows][columns + 1];
		for (int i = 0; i < rows; i++) {
			data[i][0] = zoning.getLADToName().get(odm.getSortedOrigins().get(i));
			for (int j = 0; j < columns; j++) {
				data[i][j+1] = odm.getFlow(odm.getSortedOrigins().get(i), odm.getSortedDestinations().get(j));
			}
		}
		String[] labels = new String[columns + 1];
		labels[0] = "ORIG\\DEST";
		for (int j = 0; j < columns; j++) labels[j+1] = zoning.getLADToName().get(odm.getSortedDestinations().get(j));
		table.setModel(new DefaultTableModel(data, labels));

		SkimMatrix sm = rnaBefore.calculateTimeSkimMatrix();
		rows = sm.getOrigins().size();
		columns = sm.getDestinations().size();
		Object[][] data2 = new Object[rows][columns + 1];
		for (int i = 0; i < rows; i++) {
			data2[i][0] = zoning.getLADToName().get(sm.getOrigins().get(i));
			for (int j = 0; j < columns; j++) {
				data2[i][j+1] = String.format("%.2f", sm.getCost(sm.getOrigins().get(i), sm.getDestinations().get(j)));
			}
		}
		String[] labels2 = new String[columns + 1];
		labels2[0] = "ORIG \\ DEST";
		for (int j = 0; j < columns; j++) labels2[j+1] = zoning.getLADToName().get(sm.getDestinations().get(j));

		table_1.setModel(new DefaultTableModel(data2, labels2));	
		
		//update text cell with total demand
		totalDemandBefore.setText(String.valueOf(rnaBefore.getTripList().size()));
	
	}
	
	
	private void createBarChart() {
		//BAR CHART
		DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
		barDataset.addValue(70050.0, "No intervention", "Number of Trips");
		barDataset.addValue(70150.0, "Road development", "Number of Trips");
		JFreeChart chart = ChartFactory.createBarChart("Impact of New Infrastructure on Demand", "", "", barDataset, PlotOrientation.VERTICAL, true, true, false);
		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);
		chart.setTextAntiAlias(true);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.WHITE);
		//chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 367 ) );
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		//plot.setBackgroundPaint(Color.black);
		//plot.setBackgroundAlpha(0.1f);
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.DARK_GRAY);
		chart.getTitle().setPaint(Color.DARK_GRAY);
		Font titleFont = new Font("Calibri Light", Font.BOLD, 16); 
		chart.getTitle().setFont(titleFont);
		BarRenderer barRenderer = (BarRenderer)plot.getRenderer();
		barRenderer.setBarPainter(new StandardBarPainter()); //to remove gradient bar painter
		barRenderer.setDrawBarOutline(false);
		barRenderer.setShadowVisible(false);
		//barRenderer.setMaximumBarWidth(0.30);
		//get brewer palette
		ColorBrewer brewer = ColorBrewer.instance();
		//String paletteName = "BrBG";
		//String paletteName = "Set3";
		String paletteName = "Set2";
		int numberOfRowKeys = barDataset.getRowKeys().size();
		BrewerPalette palette = brewer.getPalette(paletteName);
		if (palette == null) {
			System.err.println("Invalid brewer palette name. Use one of the following:");
			System.err.println(Arrays.toString(brewer.getPaletteNames()));
		}
		Color[] colors = palette.getColors(numberOfRowKeys);
		for (int i = 0; i < numberOfRowKeys; i++) {
			barRenderer.setSeriesPaint(i, colors[i]);
			//barRenderer.setSeriesFillPaint(i, colors[i]);
		}
		chartPanel.setPreferredSize(new Dimension(421, 262)); //size according to my window
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setMouseZoomable(true);
		JPanel panel = new JPanel();
		panel.setBounds(831, 772, 421, 262);
		panel.add(chartPanel);
		contentPane.add(panel);
	}
}
