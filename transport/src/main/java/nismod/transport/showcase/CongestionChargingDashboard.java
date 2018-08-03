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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
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
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.decision.Intervention;
import nismod.transport.demand.DemandModel;
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
import nismod.transport.network.road.Trip;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.visualisation.NetworkVisualiser;
import nismod.transport.zone.Zoning;

/**
 * Dashboard for the road expansion policy intervention.
 * @author Milan Lovric
 */
public class CongestionChargingDashboard extends JFrame {

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
	private DefaultCategoryDataset barDataset;

	private static final String configFile = "./src/test/config/testConfig.properties";
	private static RoadNetwork roadNetwork;
	private static Properties props;
	private static ODMatrix odm;
	private static SkimMatrix tsmBefore;
	private static SkimMatrix csmBefore;
	private static RouteSetGenerator rsg;
	private static Zoning zoning;
	private static RoadNetworkAssignment rnaBefore;
	
	private static HashMap<VehicleType, Double> vehicleTypeToPCU;
	private static HashMap<Pair<VehicleType, EngineType>, HashMap<String, Double>> baseFuelConsumptionRates;
	private static HashMap<Integer, HashMap<Pair<VehicleType, EngineType>, Double>> relativeFuelEfficiency;
	private static HashMap<Integer, HashMap<TimeOfDay, Double>> timeOfDayDistribution;
	private static HashMap<Integer, HashMap<TimeOfDay, Double>> timeOfDayDistributionFreight;
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
	public static final int AFTER_TABLE_SHIFT = 200; //we have to shift after tables to fit the barchart in between

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
					CongestionChargingDashboard frame = new CongestionChargingDashboard();
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
	public CongestionChargingDashboard() throws IOException, AWTException {
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
		UIManager.put("ToolTip.font", new Font("Lato", Font.PLAIN, 14));
		ToolTipManager.sharedInstance().setDismissDelay(15000); // 15 second delay  


		createDashboardExplanation();

		createBeforeTables();

		createAfterTables();

		createLabelsLeftOfTables();

		createBarChart();

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

		JSlider slider = new JSlider();
		slider.setValue(15);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setMinorTickSpacing(1);
		slider.setMaximum(50);
		slider.setMajorTickSpacing(10);
		slider.setBounds(146, 728, 200, 45);
		slider.setFont(new Font("Lato", Font.BOLD, 14));
		slider.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(slider);


		//		Icon icon = new ImageIcon("./src/test/resources/images/thumb.gif");
		//		UIDefaults defaults = UIManager.getDefaults();
		//	//	defaults.put("Slider.horizontalThumbIcon", icon);
		//		defaults.put("Slider.thumb", new ColorUIResource(LandingGUI.DARK_GRAY));
		//		defaults.put("Slider.focus", new ColorUIResource(LandingGUI.DARK_GRAY));
		//		defaults.put("Slider.altTrackColor", new ColorUIResource(LandingGUI.DARK_GRAY));
		//		defaults.put("Slider.shadow", new ColorUIResource(LandingGUI.DARK_GRAY));
		//		defaults.put("Slider.highlight", new ColorUIResource(LandingGUI.DARK_GRAY));
		//		defaults.put("Slider.thumbHeight", 20);
		//		defaults.put("Slider.thumbWidth", 20);


		JLabel label_1 = new JLabel("£");
		label_1.setFont(new Font("Lato", Font.BOLD, 14));
		label_1.setBounds(346, 756, 46, 14);
		contentPane.add(label_1);

		JSlider slider_1 = new JSlider();
		slider_1.setValue(2);
		slider_1.setPaintTicks(true);
		slider_1.setPaintLabels(true);
		slider_1.setMinorTickSpacing(1);
		slider_1.setMaximum(50);
		slider_1.setMajorTickSpacing(10);
		slider_1.setBounds(146, 830, 200, 45);
		slider_1.setFont(new Font("Lato", Font.BOLD, 14));
		slider_1.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(slider_1);

		JLabel label_2 = new JLabel("£");
		label_2.setFont(new Font("Lato", Font.BOLD, 14));
		label_2.setBounds(346, 858, 46, 14);
		contentPane.add(label_2);

		JLabel label_3 = new JLabel("<html><left>What's the <b>peak-hour</b> charge?</html>?");
		label_3.setBounds(LEFT_MARGIN, 711, 100, 70);
		label_3.setFont(new Font("Lato", Font.PLAIN, 16));
		label_3.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(label_3);

		JLabel label_4 = new JLabel("<html><left>What's the <b>off-peak</b> charge?</html>?");
		label_4.setBounds(LEFT_MARGIN, 810, 100, 70);
		label_4.setFont(new Font("Lato", Font.PLAIN, 16));
		label_4.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(label_4);


		JButton btnNewButton = new JButton("RUN");
		btnNewButton.setFont(new Font("Lato", Font.BOLD, 25));
		btnNewButton.setBounds(134, 920, 149, 70);
		btnNewButton.setBorder(RUN_BUTTON_BORDER);
		btnNewButton.setBackground(LandingGUI.LIGHT_GRAY);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
				final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
				final String populationFile = props.getProperty("populationFile");
				final String GVAFile = props.getProperty("GVAFile");
				final String elasticitiesFile = props.getProperty("elasticitiesFile");
				final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");
				final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
				final String freightRoutesFile = props.getProperty("freightRoutesFile");

				final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
				final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
				final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
				final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

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
					dm = new DemandModel(roadNetwork, baseYearODMatrixFile, baseYearFreightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions, rsg, props);

					System.out.println("Base-year congestion charging: ");
					//System.out.println(dm.getCongestionCharges(2015));

					cc.install(dm);

					HashMap<String, MultiKeyMap> congestionCharges = dm.getCongestionCharges(2016);
					System.out.println("Policies: " + congestionCharges);
					String policyName = congestionCharges.keySet().iterator().next();
					MultiKeyMap specificCharges = (MultiKeyMap) congestionCharges.get(policyName);
					System.out.println("Southampton policy: " + specificCharges);

					double peakCharge = slider.getValue();
					double offPeakCharge = slider_1.getValue();

					for (Object mk: specificCharges.keySet()) {

						VehicleType vht = (VehicleType) ((MultiKey)mk).getKey(0);
						TimeOfDay hour = (TimeOfDay) ((MultiKey)mk).getKey(1);
						HashMap<Integer, Double> linkCharges = (HashMap<Integer, Double>) specificCharges.get(vht, hour);
						if (hour == TimeOfDay.SEVENAM || hour == TimeOfDay.EIGHTAM || hour == TimeOfDay.NINEAM || hour == TimeOfDay.TENAM ||
								hour == TimeOfDay.FOURPM || hour == TimeOfDay.FIVEPM || hour == TimeOfDay.SIXPM || hour == TimeOfDay.SEVENPM)
							for (int edgID: linkCharges.keySet()) linkCharges.put(edgID, peakCharge);
						else	for (int edgID: linkCharges.keySet()) linkCharges.put(edgID, offPeakCharge);
					}

					System.out.println("Congestion charges from sliders: " + specificCharges);

					props.setProperty("TIME", "-1.5");
					props.setProperty("LENGTH", "-1.0");
					props.setProperty("COST", "-3.6"); //based on VOT

					final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
					
					//create a road network assignment
					RoadNetworkAssignment rnaAfterCongestionCharging = new RoadNetworkAssignment(roadNetwork,
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
							congestionCharges,
							props);
					
					//rnaAfterCongestionCharging.assignPassengerFlows(odm, rsg);

					RandomSingleton.getInstance().setSeed(1234);

					rnaAfterCongestionCharging.assignPassengerFlowsRouteChoice(odm, rsg, props);
					rnaAfterCongestionCharging.updateLinkVolumeInPCU();
					rnaAfterCongestionCharging.updateLinkVolumeInPCUPerTimeOfDay();

					//predict change in demand
					SkimMatrix tsm = rnaAfterCongestionCharging.calculateTimeSkimMatrix();
					SkimMatrix csm = rnaAfterCongestionCharging.calculateCostSkimMatrix();

					//predicted demand	
					ODMatrix predictedODM = new ODMatrix();

					//final String elasticitiesFile = props.getProperty("elasticitiesFile");
					HashMap<ElasticityTypes, Double> elasticities = InputFileReader.readElasticitiesFile(elasticitiesFile);

					System.out.println("Time Skim Matrix Before:");
					tsmBefore.printMatrixFormatted();
					System.out.println("Time Skim Matrix After:");
					tsm.printMatrixFormatted();
					System.out.println("Cost Skim Matrix Before:");
					csmBefore.printMatrixFormatted();
					System.out.println("Cost Skim Matrix After:");
					csm.printMatrixFormatted();


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


					rnaAfterCongestionCharging.resetLinkVolumes();
					rnaAfterCongestionCharging.resetTripStorages();

					RandomSingleton.getInstance().setSeed(1234);

					rnaAfterCongestionCharging.assignPassengerFlowsRouting(predictedODM, rsg);
					rnaAfterCongestionCharging.updateLinkVolumeInPCU();
					rnaAfterCongestionCharging.updateLinkVolumeInPCUPerTimeOfDay();
					//SkimMatrix sm = rnaAfterCongestionCharging.calculateTimeSkimMatrix();


					HashMap<Integer, Double> capacityAfter = rnaAfterCongestionCharging.calculateDirectionAveragedPeakLinkCapacityUtilisation();

					//String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";
					final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");
					String shapefilePathAfter = "./temp/after" +  LandingGUI.counter++ + ".shp";
					JFrame rightFrame;
					JButton reset = null;
					try {
						rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter, congestionChargeZoneUrl);
						rightFrame.setVisible(true);
						//rightFrame.repaint();

						//panel_2.removeAll();
						panel_2.add(rightFrame.getContentPane(), 0);
						panel_2.setLayout(null);
						panel_2.setComponentZOrder(labelPanel2, 0);
						//				contentPane.setComponentZOrder(labelAfter, 0);
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


					SkimMatrix sm = rnaAfterCongestionCharging.calculateTimeSkimMatrix();

					System.out.println("Time Skim Matrix Final:");
					sm.printMatrixFormatted();

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


					//update bar chart
		//			barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Total Trips");
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

					//uninstall intervention
					cc.uninstall(dm);

					//pack();

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});
		contentPane.add(btnNewButton);

		//set controls to represent the intervention
		slider.setValue(15);
		slider_1.setValue(2);

		//run the default intervention
		btnNewButton.doClick();

		pack();
		//this.setExtendedState(this.getExtendedState()|JFrame.MAXIMIZED_BOTH );
	}


	private void createDashboardExplanation( ) {



		//coloured panel with icon
		JPanel panel_4 = new JPanel();
		panel_4.setBackground(LandingGUI.PASTEL_BLUE);
		panel_4.setBounds(LEFT_MARGIN, 34, 346, 123);
		contentPane.add(panel_4);
		panel_4.setLayout(null);

		JLabel lblNewLabel_2 = new JLabel("<html><left>Intervention 3:<br>Congestion Charging</html>");
		lblNewLabel_2.setBounds(116, 5, 220, 115);
		lblNewLabel_2.setFont(new Font("Lato", Font.BOLD, 26));
		panel_4.add(lblNewLabel_2);

		File imgRoad = new File("./src/test/resources/images/tollGateIcon.png");
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
		html.append("<font size=+1>What happens when we introduce a congestion charging zone?</font><br><br>");
		html.append("<font size=+1><b>What we found:</b></font><br>");
		html.append("<ul>");
		html.append("<li><font size=+1>Lower road capacity utilisation within the policy area.</font>");
		html.append("<li><font size=+1>Decrease in vehicle flows due to increased travel costs.</font>");
		html.append("<li><font size=+1>Decrease in travel times due to lower total demand.</font>");
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

		JLabel lblNewLabel_5 = new JLabel("<html><left>Change the congestion charge during peak and off-peak hours:</html>");
		lblNewLabel_5.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_5.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel_5.setFont(new Font("Lato", Font.PLAIN, 20));
		lblNewLabel_5.setForeground(LandingGUI.DARK_GRAY);
		lblNewLabel_5.setBounds(LEFT_MARGIN, 628, 346, 100);
		contentPane.add(lblNewLabel_5);

		JLabel lblNewLabel_6 = new JLabel("<html>(Peak hours: 7AM-11AM & 4PM-8PM)</html>");
		lblNewLabel_6.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_6.setFont(new Font("Lato", Font.PLAIN, 12));
		lblNewLabel_6.setBounds(152, 784, 226, 14);
		lblNewLabel_6.setForeground(LandingGUI.DARK_GRAY);
		contentPane.add(lblNewLabel_6);

		JLabel lblNewLabel_7 = new JLabel("Observe the change in capacity utilisation in the \"after\" map");
		lblNewLabel_7.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_7.setFont(new Font("Lato", Font.PLAIN, 12));
		lblNewLabel_7.setForeground(LandingGUI.DARK_GRAY);
		lblNewLabel_7.setBounds(LEFT_MARGIN, 1006, 342, 14);
		contentPane.add(lblNewLabel_7);

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
		scrollPane_2.setBounds(AFTER_MAP_X + TABLE_LABEL_WIDTH + AFTER_TABLE_SHIFT, MAP_HEIGHT + 100, 416, 90);
		contentPane.add(scrollPane_2);
	}


	private void createAfterTables() {

		JLabel lblAfterPolicyIntervention = new JLabel("After Policy Intervention");
		lblAfterPolicyIntervention.setLabelFor(table_2);
		lblAfterPolicyIntervention.setForeground(LandingGUI.DARK_GRAY);
		lblAfterPolicyIntervention.setFont(new Font("Lato", Font.BOLD, 18));
		lblAfterPolicyIntervention.setBounds(AFTER_MAP_X + AFTER_TABLE_SHIFT, MAP_HEIGHT + 55, 392, 30);
		contentPane.add(lblAfterPolicyIntervention);

		table_2 = new JTable() {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int columnIndex) {
				JComponent component = (JComponent) super.prepareRenderer(renderer, rowIndex, columnIndex);  
				component.setOpaque(true);

				if (columnIndex == 0)  { 
					//component.setBackground(new Color(0, 0, 0, 20));
					component.setBackground(LandingGUI.MID_GRAY);
				} else {
					int newValue = Integer.parseInt(getValueAt(rowIndex, columnIndex).toString());
					int oldValue = Integer.parseInt(table.getValueAt(rowIndex, columnIndex).toString());

					/*
					if (newValue > oldValue) component.setBackground(increase);
					else if (newValue < oldValue) component.setBackground(decrease);
					else component.setBackground(Color.WHITE);
					 */
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
		scrollPane_3.setBounds(AFTER_MAP_X + TABLE_LABEL_WIDTH + AFTER_TABLE_SHIFT, scrollPane_2.getY() + scrollPane_2.getHeight() + TABLE_ROW_HEIGHT, 416, 90);
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

		JPanel tableChangeLegendHorizontal = new TableChangeLegendHorizontal();
		tableChangeLegendHorizontal.setBounds(scrollPane_3.getX() - 19, scrollPane_3.getY() + scrollPane_3.getHeight() + TABLE_ROW_HEIGHT, 450, 29);
		contentPane.add(tableChangeLegendHorizontal);
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
		lblTrips_1.setBounds(AFTER_MAP_X + AFTER_TABLE_SHIFT, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 40);
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
		lblCars_1.setBounds(AFTER_MAP_X + AFTER_TABLE_SHIFT, MAP_HEIGHT + 100, TABLE_LABEL_WIDTH, 100);
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
		lblTime_1.setBounds(AFTER_MAP_X + AFTER_TABLE_SHIFT, scrollPane_3.getY(), TABLE_LABEL_WIDTH, 40);
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
		lblClock_1.setBounds(AFTER_MAP_X + AFTER_TABLE_SHIFT, scrollPane_3.getY() + 20, TABLE_LABEL_WIDTH, 100);
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
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");

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
		yearToUnitCO2Emissions = InputFileReader.readEnergyUnitCostsFile(unitCO2EmissionsFile);
		yearToEngineTypeFractions = InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile);
		yearToAVFractions = InputFileReader.readAVFractionsFile(AVFractionsFile);
	
		//create a road network assignment
		rnaBefore = new RoadNetworkAssignment(roadNetwork, 
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
		rsg = new RouteSetGenerator(roadNetwork);

		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		//rsg.generateRouteSetForODMatrix(odm, 5);

		RandomSingleton.getInstance().setSeed(1234);

		rnaBefore.assignPassengerFlowsRouteChoice(odm, rsg, props);
		//		rnaBefore.assignPassengerFlows(odm, rsg);
		//		rnaBefore.updateLinkTravelTimes(1.0);
		//		rnaBefore.resetLinkVolumes();
		//		rnaBefore.resetTripStorages();
		//		rnaBefore.assignPassengerFlowsRouteChoice(odm, rsg, props);
		rnaBefore.updateLinkTravelTimes(1.0);
		rnaBefore.updateLinkVolumeInPCU();
		rnaBefore.updateLinkVolumeInPCUPerTimeOfDay();

		//update bar chart
		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Total Trips");
		
		
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
	}


	private void createBarChart() {
		//BAR CHART
		barDataset = new DefaultCategoryDataset();

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

		//JFreeChart chart = ChartFactory.createBarChart("Impact of Congestion Charging on Demand", "", "", barDataset, PlotOrientation.VERTICAL, true, true, false);
		JFreeChart chart = ChartFactory.createBarChart("", "", "", barDataset, PlotOrientation.VERTICAL, true, true, false);

		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);

		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		//chartPanel.setBackground(Color.WHITE);
		chartPanel.setBackground(LandingGUI.LIGHT_GRAY);

		//chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 367 ) );

		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		//plot.setBackgroundPaint(Color.black);
		//plot.setBackgroundAlpha(0.1f);
		plot.setBackgroundPaint(LandingGUI.LIGHT_GRAY);
		plot.setOutlinePaint(LandingGUI.LIGHT_GRAY);
		plot.setRangeGridlinePaint(LandingGUI.DARK_GRAY);

		chart.getTitle().setPaint(LandingGUI.DARK_GRAY);
		Font titleFont = new Font("Lato", Font.BOLD, 16); 
		chart.getTitle().setFont(titleFont);

		chartPanel.setFont(titleFont);

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
			barRenderer.setSeriesItemLabelFont(i, TABLE_FONT);
			barRenderer.setLegendTextFont(i, TABLE_FONT);
		}

		barRenderer.setDefaultItemLabelFont(TABLE_FONT);
		barRenderer.setDefaultLegendTextFont(TABLE_FONT);

		chartPanel.setPreferredSize(new Dimension(400, 175)); //size according to my window
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setMouseZoomable(true);

		JPanel panel = new JPanel();
		panel.setBounds(scrollPane.getX() + scrollPane.getWidth() + 20, scrollPane.getY() + 20, 400, 180);
		panel.add(chartPanel);
		contentPane.add(panel);

		//Demand label

		JPanel panelDemand = new JPanel();
		//panelDemand.setBorder(TABLE_BORDER);
		panelDemand.setBackground(LandingGUI.LIGHT_GRAY);
		panelDemand.setBounds(scrollPane.getX() + scrollPane.getWidth() + 195, scrollPane.getY() - 50, 100, 130);
		contentPane.add(panelDemand);
		panelDemand.setLayout(null);

		JLabel lblDemand = new JLabel("Demand");
		lblDemand.setFont(new Font("Lato", Font.BOLD, 14));
		lblDemand.setBounds(15, 11, 66, 14);
		panelDemand.add(lblDemand);

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
		Image newimg = subImage.getScaledInstance(65, 65, java.awt.Image.SCALE_SMOOTH); //scaling
		ImageIcon icon = new ImageIcon(newimg);

		JLabel lblNewLabel_1 = new JLabel(icon);
		lblNewLabel_1.setBounds(7, 15, 65, 65);
		panelDemand.add(lblNewLabel_1);

	}
}
