package nismod.transport.showcase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.decision.Intervention;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.Trip;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.BarVisualiser;

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
import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.JSeparator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dashboard for the congestion charging policy intervention.
 * @author Milan Lovric
 */
public class DashboardCongestionCharging extends JFrame {

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
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DashboardCongestionCharging frame = new DashboardCongestionCharging();
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
	public DashboardCongestionCharging() throws IOException {
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Dashboard");
		setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		setBounds(100, 100, 939, 352);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(null);
		 
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

		table_2 = new JTable();
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

		table_3 = new JTable();
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
		
//		barDataset.addValue(100.0, "No intervention", "Number of Trips");
//		barDataset.addValue(80.0, "Congestion charging", "Number of Trips");
//		barDataset.addValue(60.0, "No intervention", "Number of Trips Through the Zone");
//		barDataset.addValue(50.0, "Congestion charging", "Number of Trips Through the Zone");
//		barDataset.addValue(45.0, "No intervention", "Number of Trips Outside the Zone");
//		barDataset.addValue(40.0, "Congestion charging", "Number of Trips Outside the Zone");
		
		barDataset.addValue(100.0, "No intervention", "Total Trips");
		barDataset.addValue(90.0, "Congestion charging", "Total Trips");
		barDataset.addValue(55.0, "No intervention", "Through Zone");
		barDataset.addValue(40.0, "Congestion charging", "Through Zone");
		barDataset.addValue(45.0, "No intervention", "Outside Zone");
		barDataset.addValue(50.0, "Congestion charging", "Outside Zone");
		
		JFreeChart chart = ChartFactory.createBarChart("Impact of Congestion Charging on Demand", "", "", barDataset, PlotOrientation.VERTICAL, true, true, false);
	
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
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
	
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		RoadNetworkAssignment rnaBefore = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
		
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		//rsg.generateRouteSetForODMatrix(odm, 5);
		
		//rnaBefore.assignPassengerFlows(odm, rsg);
		rnaBefore.assignPassengerFlowsRouteChoice(odm, rsg, props);
		rnaBefore.updateLinkVolumeInPCU();
		rnaBefore.updateLinkVolumeInPCUPerTimeOfDay();
		
		String shapefilePathBefore = "./temp/networkWithCapacityUtilisationBefore.shp";
		String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";
		
		HashMap<Integer, Double> capacityBefore = rnaBefore.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame leftFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation Before Intervention", capacityBefore, "CapUtil", shapefilePathBefore);
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//leftFrame.setSize(screenSize.width / 2, (int)Math.round(screenSize.height * 0.65));
		//leftFrame.setLocation(0, 0);
		leftFrame.setVisible(false);
		((JMapFrameDemo)leftFrame).getMapPane().reset();
		
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
		
		JLabel lblANode = new JLabel("Peak-hour congestion charge:");
		lblANode.setBounds(1404, 813, 187, 14);
		contentPane.add(lblANode);
		
		
		JSlider slider_1 = new JSlider();
		slider_1.setValue(15);
		slider_1.setPaintTicks(true);
		slider_1.setPaintLabels(true);
		slider_1.setMinorTickSpacing(1);
		slider_1.setMaximum(50);
		slider_1.setMajorTickSpacing(10);
		slider_1.setBounds(1404, 841, 200, 45);
		contentPane.add(slider_1);
		
		JSlider slider = new JSlider();
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMinorTickSpacing(1);
		slider.setValue(2);
		slider.setMajorTickSpacing(10);
		slider.setMaximum(50);
		slider.setBounds(1404, 947, 200, 45);
		contentPane.add(slider);
		
		JLabel lblLanesToAdd = new JLabel("Off-peak congestion charge:");
		lblLanesToAdd.setBounds(1404, 921, 177, 14);
		contentPane.add(lblLanesToAdd);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBounds(10, 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_1.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_1);
		
		JPanel panel_2 = new JPanel();
		panel_2.setBounds((int)Math.round(screenSize.width * 0.5), 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_2.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_2);
		
		JButton btnNewButton = new JButton("RUN");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				final String congestionChargingFile = "./src/test/resources/testdata/interventions/congestionChargingSouthampton.properties";
				try {
					final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");
				} catch (MalformedURLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

				CongestionCharging cc = new CongestionCharging(congestionChargingFile);
				System.out.println("Congestion charging intervention: " + cc.toString());
				
				List<Intervention> interventions = new ArrayList<Intervention>();
				interventions.add(cc);
		
				DemandModel dm;
				try {
					dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);
										
					System.out.println("Base-year congestion charging: ");
					//System.out.println(dm.getCongestionCharges(2015));
					
					cc.install(dm);
					
					HashMap<String, MultiKeyMap> congestionCharges = dm.getCongestionCharges(2016);
					System.out.println("Policies: " + congestionCharges);
					String policyName = congestionCharges.keySet().iterator().next();
					MultiKeyMap specificCharges = (MultiKeyMap) congestionCharges.get(policyName);
					System.out.println("Southampton policy: " + specificCharges);
					
					double peakCharge = slider_1.getValue();
					double offPeakCharge = slider.getValue();
					
					for (Object mk: specificCharges.keySet()) {
						
						VehicleType vht = (VehicleType) ((MultiKey)mk).getKey(0);
						TimeOfDay hour = (TimeOfDay) ((MultiKey)mk).getKey(1);
						HashMap<Integer, Double> linkCharges = (HashMap<Integer, Double>) specificCharges.get(vht, hour);
						if (hour == TimeOfDay.SEVENAM || hour == TimeOfDay.EIGHTAM || hour == TimeOfDay.NINEAM || hour == TimeOfDay.TENAM ||
							hour == TimeOfDay.FOURPM || hour == TimeOfDay.FIVEPM || hour == TimeOfDay.SIXPM || hour == TimeOfDay.SEVENPM)
								for (int edgID: linkCharges.keySet()) linkCharges.put(edgID, peakCharge);
						else	for (int edgID: linkCharges.keySet()) linkCharges.put(edgID, offPeakCharge);
					}
					
					props.setProperty("TIME", "-1.5");
					props.setProperty("LENGTH", "-1.0");
					props.setProperty("COST", "-10.0"); //bump the cost sensitivity

					RoadNetworkAssignment rnaAfterCongestionCharging = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, congestionCharges, props);
					//rnaAfterCongestionCharging.assignPassengerFlows(odm, rsg);
					rnaAfterCongestionCharging.assignPassengerFlowsRouteChoice(odm, rsg, props);
					rnaAfterCongestionCharging.updateLinkVolumeInPCU();
					rnaAfterCongestionCharging.updateLinkVolumeInPCUPerTimeOfDay();

					HashMap<Integer, Double> capacityAfter = rnaAfterCongestionCharging.calculateDirectionAveragedPeakLinkCapacityUtilisation();
					
					System.out.println(capacityAfter.get(615));
					System.out.println(capacityAfter.get(616));

					String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";
					JFrame rightFrame;

					rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
					rightFrame.setVisible(false);
					//rightFrame.repaint();
					//panel_2.add(rightFrame.getContentPane());
					//panel_2.setLayout(null);
					
					//update bar chart
					barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Total Trips");
					barDataset.addValue(rnaAfterCongestionCharging.getTripList().size(), "Congestion charging", "Total Trips");
					
					double sumThroughBefore = 0.0, sumOutsideBefore = 0.0;
					for (Trip t: rnaBefore.getTripList())
						if (t.isTripGoingThroughCongestionChargingZone(policyName, congestionCharges))
							sumThroughBefore++;
						else
							sumOutsideBefore++;
						
					double sumThrough = 0.0, sumOutside = 0.0;
					for (Trip t: rnaAfterCongestionCharging.getTripList())
						if (t.isTripGoingThroughCongestionChargingZone(policyName, congestionCharges))
							sumThrough++;
						else
							sumOutside++;
					
					barDataset.addValue(sumThroughBefore, "No intervention", "Through Zone");
					barDataset.addValue(sumThrough, "Congestion charging", "Through Zone");
					barDataset.addValue(sumOutsideBefore, "No intervention", "Outside Zone");
					barDataset.addValue(sumOutside, "Congestion charging", "Outside Zone");

					cc.uninstall(dm);

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		btnNewButton.setFont(new Font("Calibri Light", Font.BOLD, 25));
		btnNewButton.setBounds(1686, 799, 200, 169);
		contentPane.add(btnNewButton);
		
		JLabel lblRoadExpansionPolicy = new JLabel("Congestion Charging Policy");
		lblRoadExpansionPolicy.setForeground(Color.DARK_GRAY);
		lblRoadExpansionPolicy.setFont(new Font("Calibri Light", Font.BOLD, 16));
		lblRoadExpansionPolicy.setBounds(1404, 755, 380, 30);
		contentPane.add(lblRoadExpansionPolicy);
		
		JLabel lblNewLabel = new JLabel("£");
		lblNewLabel.setBounds(1604, 870, 46, 14);
		contentPane.add(lblNewLabel);
		
		JLabel label_1 = new JLabel("£");
		label_1.setBounds(1604, 976, 46, 14);
		contentPane.add(label_1);
		
		final String congestionChargingFile = "./src/test/resources/testdata/interventions/congestionChargingSouthampton.properties";
		final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");

		CongestionCharging cc = new CongestionCharging(congestionChargingFile);
		System.out.println("Congestion charging intervention: " + cc.toString());
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		interventions.add(cc);
		
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);
		System.out.println("Base-year congestion charging: ");
		System.out.println(dm.getCongestionCharges(2015));
		
		cc.install(dm);
		
		props.setProperty("TIME", "-1.5");
		props.setProperty("LENGTH", "-1.0");
		props.setProperty("COST", "-10.0"); //bump the cost sensitivity
		
		RoadNetworkAssignment rnaAfterCongestionCharging = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, dm.getCongestionCharges(2016), props);
		//rnaAfterCongestionCharging.assignPassengerFlows(odm, rsg);
		rnaAfterCongestionCharging.assignPassengerFlowsRouteChoice(odm, rsg, props);
		rnaAfterCongestionCharging.updateLinkVolumeInPCU();
		rnaAfterCongestionCharging.updateLinkVolumeInPCUPerTimeOfDay();
			
		HashMap<Integer, Double> capacityAfter = rnaAfterCongestionCharging.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter, congestionChargeZoneUrl);
		rightFrame.setVisible(false);
	
		panel_1.add(leftFrame.getContentPane());
		panel_1.setLayout(null);
				
		panel_2.add(rightFrame.getContentPane());
		panel_2.setLayout(null);
		
		//update bar chart
		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Total Trips");
		barDataset.addValue(rnaAfterCongestionCharging.getTripList().size(), "Congestion charging", "Total Trips");
		
		double sumThroughBefore = 0.0, sumOutsideBefore = 0.0;
		HashMap<String, MultiKeyMap> congestionCharges = dm.getCongestionCharges(2016);
		String policyName = congestionCharges.keySet().iterator().next();
		for (Trip t: rnaBefore.getTripList())
			if (t.isTripGoingThroughCongestionChargingZone(policyName, congestionCharges))
				sumThroughBefore++;
			else
				sumOutsideBefore++;
			
		double sumThrough = 0.0, sumOutside = 0.0;
		for (Trip t: rnaAfterCongestionCharging.getTripList())
			if (t.isTripGoingThroughCongestionChargingZone(policyName, congestionCharges))
				sumThrough++;
			else
				sumOutside++;
		
		barDataset.addValue(sumThroughBefore, "No intervention", "Through Zone");
		barDataset.addValue(sumThrough, "Congestion charging", "Through Zone");
		barDataset.addValue(sumOutsideBefore, "No intervention", "Outside Zone");
		barDataset.addValue(sumOutside, "Congestion charging", "Outside Zone");
		
		
		pack();
		
		cc.uninstall(dm);
	}
}