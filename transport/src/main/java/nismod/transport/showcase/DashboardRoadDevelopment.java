package nismod.transport.showcase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadDevelopment;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.BarVisualiser;
import nismod.transport.zone.Zoning;

import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JInternalFrame;
import javax.swing.JDesktopPane;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import java.awt.event.MouseWheelEvent;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dashboard for the road development policy intervention.
 * @author Milan Lovric
 */
public class DashboardRoadDevelopment extends JFrame {

	private JPanel contentPane;
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
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField textField;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DashboardRoadDevelopment frame = new DashboardRoadDevelopment();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public DashboardRoadDevelopment() throws IOException {
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Dashboard");
		setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		setBounds(100, 100, 939, 352);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(null);

		setAlwaysOnTop(true);

		JLabel labelPanel1 = new JLabel("Before Policy Intervention");
		labelPanel1.setBounds(300, 20, 331, 20);
		contentPane.add(labelPanel1);
		labelPanel1.setForeground(Color.DARK_GRAY);
		labelPanel1.setFont(new Font("Calibri Light", Font.BOLD, 16));

		JLabel labelPanel2 = new JLabel("After Policy Intervention");
		labelPanel2.setBounds(1300, 20, 331, 20);
		contentPane.add(labelPanel2);
		labelPanel2.setForeground(Color.DARK_GRAY);
		labelPanel2.setFont(new Font("Calibri Light", Font.BOLD, 16));

		scrollPane = new JScrollPane();
		scrollPane.setBounds(21, 796, 416, 90);
		contentPane.add(scrollPane);

		table = new JTable();
		scrollPane.setViewportView(table);
		table.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "2100", "1343", "4321", "1234"},
					{"New Forest", "4252", "623", "1425", "653"},
					{"Eeastleigh", "6534", "2345", "541", "6327"},
					{"Isle of Wight", "2345", "235", "52", "435"},
				},
				new String[] {
						"TRIPS", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));

		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(21, 916, 416, 90);
		contentPane.add(scrollPane_1);


		table_1 = new JTable();
		scrollPane_1.setViewportView(table_1);
		table_1.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "10.0", "13.4", "43.1", "12.4"},
					{"New Forest", "3.5", "6.2", "14.2", "6.5"},
					{"Eeastleigh", "15.3", "23.4", "5.4", "6.3"},
					{"Isle of Wight", "23.5", "35.7", "25.2", "14.6"},
				},
				new String[] {
						"TRAVEL TIME", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));

		scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(470, 796, 416, 90);
		contentPane.add(scrollPane_2);

		table_2 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
				component.setOpaque(true);

				if (columnIndex == 0)  { 
					component.setBackground(new Color(0, 0, 0, 20));
				} else {
					int newValue = Integer.parseInt(getValueAt(rowIndex, columnIndex).toString());
					int oldValue = Integer.parseInt(table.getValueAt(rowIndex, columnIndex).toString());

					if (newValue > oldValue) component.setBackground(new Color(255, 0, 0, 50));
					else if (newValue < oldValue) component.setBackground(new Color(0, 255, 0, 50));
					else component.setBackground(Color.WHITE);

					/*
		            	double percentChange = 0.01;
		               	if (1.0 * newValue / oldValue > (1 + percentChange)) component.setBackground(new Color(255, 0, 0, 50));
		            	else if (1.0 * newValue / oldValue < (1 - percentChange)) component.setBackground(new Color(0, 255, 0, 50));
		            	else component.setBackground(Color.WHITE);
					 */

				}
				return component;
			}
		};

		table_2.setBackground(Color.WHITE);
		scrollPane_2.setViewportView(table_2);
		table_2.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "2100", "1343", "4321", "1234"},
					{"New Forest", "4252", "623", "1425", "653"},
					{"Eeastleigh", "6534", "2345", "541", "6327"},
					{"Isle of Wight", "2345", "235", "52", "435"},
				},
				new String[] {
						"TRIPS", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));

		scrollPane_3 = new JScrollPane();
		scrollPane_3.setBounds(470, 916, 416, 90);
		contentPane.add(scrollPane_3);

		table_3 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  

				if (columnIndex == 0)  { 
					component.setBackground(new Color(0, 0, 0, 20));
				} else {
					double newValue = Double.parseDouble(getValueAt(rowIndex, columnIndex).toString());
					double oldValue = Double.parseDouble(table_1.getValueAt(rowIndex, columnIndex).toString());

					if (newValue > oldValue) component.setBackground(new Color(255, 0, 0, 50));
					else if (newValue < oldValue) component.setBackground(new Color(0, 255, 0, 50));
					else component.setBackground(Color.WHITE);
				}
				return component;
			}

		};

		table_3.setBackground(Color.WHITE);
		scrollPane_3.setViewportView(table_3);
		table_3.setModel(new DefaultTableModel(
				new Object[][] {
					{"Southampton", "10.0", "13.4", "43.1", "12.4"},
					{"New Forest", "3.5", "6.2", "14.2", "6.5"},
					{"Eeastleigh", "15.3", "23.4", "5.4", "6.3"},
					{"Isle of Wight", "23.5", "35.7", "25.2", "14.6"},
				},
				new String[] {
						"TRAVEL TIME", "Southampton", "New Forest", "Eastleigh", "Isle of Wight"
				}
				));

		DefaultCategoryDataset barDataset = new DefaultCategoryDataset();

		barDataset.addValue(70050.0, "No intervention", "Number of Trips");
		barDataset.addValue(70150.0, "Road development", "Number of Trips");
		//barDataset.addValue(70150.0, "Road development", "Number of Trips");

		JFreeChart chart = ChartFactory.createBarChart("Impact of New Infrastructure on Demand", "", "", barDataset, PlotOrientation.VERTICAL, true, true, false);

		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);

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
		}


		chartPanel.setPreferredSize(new Dimension(421, 262)); //size according to my window
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setMouseZoomable(true);

		JPanel panel = new JPanel();
		panel.setBounds(920, 744, 421, 262);
		panel.add(chartPanel);
		contentPane.add(panel);

		//		lblBeforeIntervention = new JLabel("Before Intervention");
		//		lblBeforeIntervention.setFont(new Font("Calibri Light", Font.BOLD, 16));
		//		lblBeforeIntervention.setBounds(21, 771, 180, 14);
		//		contentPane.add(lblBeforeIntervention);
		//		
		//		label = new JLabel("After Intervention");
		//		label.setFont(new Font("Calibri Light", Font.BOLD, 16));
		//		label.setBounds(470, 771, 180, 14);
		//		contentPane.add(label);
		//		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		//this.setSize(1920, 876);
		//this.setLocation(0,  (int)Math.round(screenSize.height * 0.65));
		this.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		this.setLocation(0, 0);



		final String configFile = "./src/test/config/testConfig.properties";
		//final String configFile = "./src/main/config/config.properties";
		Properties props = ConfigReader.getProperties(configFile);

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

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		RoadNetworkAssignment rnaBefore = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);

		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);

		rnaBefore.assignPassengerFlows(odm, rsg);
		rnaBefore.updateLinkTravelTimes(0.9);

		rnaBefore.resetLinkVolumes();
		rnaBefore.resetTripStorages();
		rnaBefore.assignPassengerFlows(odm, rsg);
		rnaBefore.updateLinkTravelTimes(0.9);

		rnaBefore.updateLinkVolumeInPCU();
		rnaBefore.updateLinkVolumeInPCUPerTimeOfDay();

		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Number of Trips");

		SkimMatrix tsmBefore = rnaBefore.calculateTimeSkimMatrix();
		SkimMatrix csmBefore = rnaBefore.calculateCostSkimMatrix();

		String shapefilePathBefore = "./temp/networkWithCapacityUtilisationBefore.shp";
		String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";

		HashMap<Integer, Double> capacityBefore = rnaBefore.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame leftFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation Before Intervention", capacityBefore, "CapUtil", shapefilePathBefore);
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//leftFrame.setSize(screenSize.width / 2, (int)Math.round(screenSize.height * 0.65));
		//leftFrame.setLocation(0, 0);
		leftFrame.setVisible(false);
		//((JMapFrameDemo)leftFrame).getMapPane()

		//update before tables
		int rows = odm.getOrigins().size();
		int columns = odm.getDestinations().size();
		Object[][] data = new Object[rows][columns + 1];
		for (int i = 0; i < rows; i++) {
			data[i][0] = zoning.getLADToName().get(odm.getOrigins().get(i));
			for (int j = 0; j < columns; j++) {
				data[i][j+1] = odm.getFlow(odm.getOrigins().get(i), odm.getDestinations().get(j));
			}
		}
		String[] labels = new String[columns + 1];
		labels[0] = "TRIPS";
		for (int j = 0; j < columns; j++) labels[j+1] = zoning.getLADToName().get(odm.getDestinations().get(j));
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
		labels2[0] = "TRAVEL TIME";
		for (int j = 0; j < columns; j++) labels2[j+1] = zoning.getLADToName().get(sm.getDestinations().get(j));

		table_1.setModel(new DefaultTableModel(data2, labels2));

		JLabel lblBeforePolicyIntervention = new JLabel("Before Policy Intervention");
		lblBeforePolicyIntervention.setLabelFor(table);
		lblBeforePolicyIntervention.setForeground(Color.DARK_GRAY);
		lblBeforePolicyIntervention.setBounds(21, 755, 392, 30);
		contentPane.add(lblBeforePolicyIntervention);
		lblBeforePolicyIntervention.setFont(new Font("Calibri Light", Font.BOLD, 16));

		JLabel lblAfterPolicyIntervention = new JLabel("After Policy Intervention");
		lblAfterPolicyIntervention.setLabelFor(table_2);
		lblAfterPolicyIntervention.setForeground(Color.DARK_GRAY);
		lblAfterPolicyIntervention.setFont(new Font("Calibri Light", Font.BOLD, 16));
		lblAfterPolicyIntervention.setBounds(470, 755, 392, 30);
		contentPane.add(lblAfterPolicyIntervention);

		JComboBox comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(roadNetwork.getNodeIDtoNode().keySet().toArray()));
		comboBox.setBounds(1379, 832, 149, 20);
		contentPane.add(comboBox);

		JComboBox comboBox_1 = new JComboBox();
		int fromNode = (int)comboBox.getSelectedItem();

		System.out.println("fromNode = " + fromNode);

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
		comboBox_1.setModel(new DefaultComboBoxModel(roadNetwork.getNodeIDtoNode().keySet().toArray()));
		comboBox_1.setBounds(1379, 886, 149, 20);
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

		JLabel lblANode = new JLabel("A node");
		lblANode.setLabelFor(comboBox);
		lblANode.setBounds(1379, 813, 46, 14);
		contentPane.add(lblANode);

		JLabel lblBNode = new JLabel("B node");
		lblBNode.setLabelFor(comboBox_1);
		lblBNode.setBounds(1379, 861, 46, 14);
		contentPane.add(lblBNode);

		JSlider slider = new JSlider();
		slider.setSnapToTicks(true);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMinorTickSpacing(1);
		slider.setValue(1);
		slider.setMajorTickSpacing(1);
		slider.setMaximum(5);
		slider.setMinimum(1);
		slider.setBounds(1379, 947, 149, 45);
		contentPane.add(slider);

		JLabel lblLanesToAdd = new JLabel("Lanes in each direction:");
		lblLanesToAdd.setBounds(1379, 921, 177, 14);
		contentPane.add(lblLanesToAdd);

		JRadioButton rdbtnNewRadioButton = new JRadioButton("A-road");
		buttonGroup.add(rdbtnNewRadioButton);
		rdbtnNewRadioButton.setBounds(1572, 831, 70, 23);
		contentPane.add(rdbtnNewRadioButton);

		rdbtnNewRadioButton.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnNewRadioButton.isSelected()) slider.setValue(1); //default number of lanes per direction for A roads
			}
		});

		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton("Motorway");
		buttonGroup.add(rdbtnNewRadioButton_1);
		rdbtnNewRadioButton_1.setBounds(1572, 861, 104, 23);
		contentPane.add(rdbtnNewRadioButton_1);

		rdbtnNewRadioButton_1.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnNewRadioButton_1.isSelected()) slider.setValue(3); //default number of lanes per direction for motorways
			}
		});

		textField = new JTextField();
		textField.setText("10.0");
		textField.setBounds(1575, 945, 56, 36);
		contentPane.add(textField);
		textField.setColumns(10);

		JLabel lblRoadLengthkm = new JLabel("Length in km:");
		lblRoadLengthkm.setBounds(1575, 923, 101, 14);
		contentPane.add(lblRoadLengthkm);

		JButton btnNewButton = new JButton("RUN");
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
				textField.setText(String.format("%.1f", length));

				Properties props2 = new Properties();
				props2.setProperty("startYear", "2016");
				props2.setProperty("endYear", "2025");
				props2.setProperty("fromNode", Integer.toString(nodeA.getID()));
				props2.setProperty("toNode", Integer.toString(nodeB.getID()));
				props2.setProperty("biDirectional", "true");
				props2.setProperty("lanesPerDirection", Integer.toString(lanes));
				props2.setProperty("length", "10.23");
				props2.setProperty("roadClass", roadClass);
				RoadDevelopment rd = new RoadDevelopment(props2);

				System.out.println("Road development intervention: " + rd.toString());
				rd.install(roadNetwork);

				RoadNetworkAssignment rnaAfterDevelopment = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);

				rsg.generateRouteSetForODMatrix(odm);
				//rnaAfterDevelopment.assignPassengerFlowsRouteChoice(odm, rsg, props);
				rnaAfterDevelopment.assignPassengerFlows(odm, rsg);
				rnaAfterDevelopment.updateLinkVolumeInPCU();
				rnaAfterDevelopment.updateLinkVolumeInPCUPerTimeOfDay();

				//predict change in demand
				SkimMatrix tsm = rnaAfterDevelopment.calculateTimeSkimMatrix();
				SkimMatrix csm = rnaAfterDevelopment.calculateCostSkimMatrix();

				//predicted demand	
				ODMatrix predictedODM = new ODMatrix();

				final String elasticitiesFile = props.getProperty("elasticitiesFile");
				HashMap<ElasticityTypes, Double> elasticities = null;
				try {
					elasticities = DemandModel.readElasticitiesFile(elasticitiesFile);
				} catch (FileNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}


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

				rnaAfterDevelopment.assignPassengerFlows(predictedODM, rsg);
				rnaAfterDevelopment.updateLinkVolumeInPCU();
				rnaAfterDevelopment.updateLinkVolumeInPCUPerTimeOfDay();
				//SkimMatrix sm = rnaAfterDevelopment.calculateTimeSkimMatrix();


				//update bar chart
				barDataset.addValue(rnaAfterDevelopment.getTripList().size(), "Road development", "Number of Trips");

				HashMap<Integer, Double> capacityAfter = rnaAfterDevelopment.calculateDirectionAveragedPeakLinkCapacityUtilisation();
				String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";
				JFrame rightFrame;
				try {
					rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
					rightFrame.setVisible(false);
					rightFrame.repaint();
					//	panel_2.add(rightFrame.getContentPane());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				rd.uninstall(roadNetwork);

				//update after tables
				int rows = predictedODM.getOrigins().size();
				int columns = predictedODM.getDestinations().size();
				Object[][] data = new Object[rows][columns + 1];
				for (int i = 0; i < rows; i++) {
					data[i][0] = zoning.getLADToName().get(predictedODM.getOrigins().get(i));
					for (int j = 0; j < columns; j++) {
						data[i][j+1] = predictedODM.getFlow(predictedODM.getOrigins().get(i), predictedODM.getDestinations().get(j));
					}
				}
				String[] labels = new String[columns + 1];
				labels[0] = "TRIPS";
				for (int j = 0; j < columns; j++) labels[j+1] = zoning.getLADToName().get(predictedODM.getDestinations().get(j));
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
				labels2[0] = "TRAVEL TIME";
				for (int j = 0; j < columns; j++) labels2[j+1] = zoning.getLADToName().get(sm.getDestinations().get(j));
				table_3.setModel(new DefaultTableModel(data2, labels2));
			}
		});

		btnNewButton.setFont(new Font("Calibri Light", Font.BOLD, 25));
		btnNewButton.setBounds(1686, 799, 200, 169);
		contentPane.add(btnNewButton);

		JLabel lblRoadDevelopmentPolicy = new JLabel("Road Development Policy");
		lblRoadDevelopmentPolicy.setForeground(Color.DARK_GRAY);
		lblRoadDevelopmentPolicy.setFont(new Font("Calibri Light", Font.BOLD, 16));
		lblRoadDevelopmentPolicy.setBounds(1379, 755, 380, 30);
		contentPane.add(lblRoadDevelopmentPolicy);

		JLabel lblRoadClass = new JLabel("Road class:");
		lblRoadClass.setBounds(1572, 813, 79, 14);
		contentPane.add(lblRoadClass);

		final String roadDevelopmentFileName = props.getProperty("roadDevelopmentFile");
		RoadDevelopment rd = new RoadDevelopment(roadDevelopmentFileName);
		System.out.println("Road development intervention: " + rd.toString());
		rd.install(roadNetwork);

		//set controls to represent the intervention
		ActionListener al = comboBox.getActionListeners()[0];
		comboBox.removeActionListener(al); //temporarilly
		comboBox.setSelectedItem(new Integer(63));
		comboBox_1.setSelectedItem(new Integer(23));
		comboBox.addActionListener(al);
		rdbtnNewRadioButton.setSelected(true);
		slider.setValue(2);
		textField.setText("10.23");

		RoadNetworkAssignment rnaAfterDevelopment = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
		rnaAfterDevelopment.assignPassengerFlows(odm, rsg);
		rnaAfterDevelopment.updateLinkVolumeInPCU();
		rnaAfterDevelopment.updateLinkVolumeInPCUPerTimeOfDay();

		HashMap<Integer, Double> capacityAfter = rnaAfterDevelopment.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation Before Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
		rightFrame.setVisible(false);

		rd.uninstall(roadNetwork);

		JPanel panel_1 = new JPanel();
		panel_1.setBounds(10, 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_1.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_1);
		panel_1.add(leftFrame.getContentPane());
		panel_1.setLayout(null);

		JPanel panel_2 = new JPanel();
		panel_2.setBounds((int)Math.round(screenSize.width * 0.5), 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_2.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_2);
		panel_2.add(rightFrame.getContentPane());
		panel_2.setLayout(null);

		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Number of Trips");
		barDataset.addValue(rnaAfterDevelopment.getTripList().size(), "Road development", "Number of Trips");

		pack();
	}
}
