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
import javax.swing.border.Border;
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
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.RoadExpansion;
import nismod.transport.demand.DemandModel;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.demand.DemandModel.ElasticityTypes;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.visualisation.BarVisualiser;
import nismod.transport.zone.Zoning;

import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;

import javax.swing.JInternalFrame;
import javax.swing.JDesktopPane;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;

import java.awt.event.MouseWheelEvent;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.AbstractListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.JSeparator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Dashboard for the road expansion policy intervention.
 * @author Milan Lovric
 */
public class DashboardRoadExpansion extends JFrame {

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
					DashboardRoadExpansion frame = new DashboardRoadExpansion();
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
	public DashboardRoadExpansion() throws IOException {
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Dashboard");
		setIconImage(Toolkit.getDefaultToolkit().getImage("./src/test/resources/images/NISMOD-LP.jpg"));
		setBounds(100, 100, 939, 352);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(null);

		setAlwaysOnTop(true);


		contentPane.setBackground(GUI.DASHBOARD); //mistral green

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
		scrollPane_2.setToolTipText("This shows the impact on demand.");
		scrollPane_2.setBounds(470, 796, 416, 90);
		contentPane.add(scrollPane_2);

		//UIManager.put("ToolTip.background", new ColorUIResource(255, 247, 200)); //#fff7c8
		UIManager.put("ToolTip.background", new ColorUIResource(255, 255, 255)); //#fff7c8
		Border border = BorderFactory.createLineBorder(new Color(76,79,83));    //#4c4f53
		UIManager.put("ToolTip.border", border);
		UIManager.put("ToolTip.font", new Font("Calibri Light", Font.BOLD, 16));

		ToolTipManager.sharedInstance().setDismissDelay(15000); // 15 second delay  

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
		barDataset.addValue(70100.0, "Road expansion", "Number of Trips");
		//barDataset.addValue(70150.0, "Road development", "Number of Trips");

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

		JPanel panel_1 = new JPanel();
		panel_1.setBounds(10, 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_1.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_1);

		JPanel panel_2 = new JPanel();
		panel_2.setBounds((int)Math.round(screenSize.width * 0.5), 10, (int)Math.round(screenSize.width * 0.5) - 12, (int)Math.round(screenSize.height * 0.65));
		//panel_2.setSize((int)Math.round(screenSize.width * 0.5) - 5, (int)Math.round(screenSize.height * 0.6));
		contentPane.add(panel_2);

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

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);

		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);

		rnaBefore.assignPassengerFlowsRouting(odm, rsg);
		rnaBefore.updateLinkTravelTimes(0.9);

		rnaBefore.resetLinkVolumes();
		rnaBefore.resetTripStorages();
		rnaBefore.assignPassengerFlowsRouting(odm, rsg);
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
		panel_1.add(leftFrame.getContentPane());
		panel_1.setLayout(null);
		JMapPane pane = ((JMapFrameDemo)leftFrame).getMapPane();

		//JFrame rightFrame;
		//panel_2.add(rightFrame.getContentPane());
		//panel_2.setLayout(null);


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

		JCheckBox chckbxNewCheckBox = new JCheckBox("Both directions?");
		chckbxNewCheckBox.setBackground(GUI.DASHBOARD);
		chckbxNewCheckBox.setSelected(true);
		chckbxNewCheckBox.setBounds(1558, 945, 122, 23);
		contentPane.add(chckbxNewCheckBox);

		//look and feel of the comboBox
//	    UIManager.put("ComboBox.control", new ColorUIResource(255, 255, 255)); 
//	    UIManager.put("ComboBox.controlForeground", new ColorUIResource(50, 15, 135));
//	    UIManager.put("ComboBox.buttonShadow", new ColorUIResource(50, 15, 135));
//	    UIManager.put("ComboBox.background", new ColorUIResource(255, 255, 255));
//	    UIManager.put("ComboBox.selectionBackground", new ColorUIResource(238, 238, 238));
//	    //UIManager.put("ComboBox.selectionForeground", new ColorUIResource(238, 238, 238)); //do not set
//	    UIManager.put("ComboBox.foreground", new ColorUIResource(50, 15, 135));
//		Border comboBoxBorder = BorderFactory.createLineBorder(new Color(76,79,83));    //#4c4f53
//	    UIManager.put("ComboBox.boder", comboBoxBorder);
//	    UIManager.put("ComboBox.buttonBackground", new ColorUIResource(50, 15, 135));
//	    UIManager.put("ComboBox.buttonDarkShadow", new ColorUIResource(10, 10, 10));
//	    
//	    final Color COLOR_BUTTON_BACKGROUND = Color.decode("#d3dedb");
//	    UIManager.put("ComboBox.buttonBackground", new ColorUIResource(COLOR_BUTTON_BACKGROUND));
//	    UIManager.put("ComboBox.buttonShadow", new ColorUIResource(COLOR_BUTTON_BACKGROUND));
//	    UIManager.put("ComboBox.buttonDarkShadow", new ColorUIResource(COLOR_BUTTON_BACKGROUND));
//	    UIManager.put("ComboBox.buttonHighlight", new ColorUIResource(COLOR_BUTTON_BACKGROUND));
//	    SwingUtilities.updateComponentTreeUI(this);
	    
//	    try {
//			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//		} catch (ClassNotFoundException e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		} catch (InstantiationException e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		} catch (IllegalAccessException e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		} catch (UnsupportedLookAndFeelException e3) {
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		}
//	    System.out.println(   UIManager.getInstalledLookAndFeels().toString() );
//	    SwingUtilities.updateComponentTreeUI(this);
	    
//	    ComboBox.actionMap ActionMap 
//	    ComboBox.ancestorInputMap InputMap 
//	    ComboBox.background Color 
//	    ComboBox.border Border 
//	    ComboBox.buttonBackground Color 
//	    ComboBox.buttonDarkShadow Color 
//	    ComboBox.buttonHighlight Color 
//	    ComboBox.buttonShadow Color 
//	    ComboBox.control Color 
//	    ComboBox.controlForeground Color 
//	    ComboBox.disabledBackground Color 
//	    ComboBox.disabledForeground Color 
//	    ComboBox.font Font 
//	    ComboBox.foreground Color 
//	    ComboBox.rendererUseListColors Boolean 
//	    ComboBox.selectionBackground Color 
//	    ComboBox.selectionForeground Color 
//	    ComboBox.showPopupOnNavigation Boolean 
//	    ComboBox.timeFactor Long 
//	    ComboBox.togglePopupText String 
//	    ComboBoxUI String 
			
		JComboBox comboBox = new JComboBox();
		comboBox.setModel(new DefaultComboBoxModel(roadNetwork.getNodeIDtoNode().keySet().toArray()));
		//comboBox.setModel(new DefaultComboBoxModel(new String[] {"5", "6", "27", "23", "25"}));
		comboBox.setBounds(1404, 832, 149, 20);
		//comboBox.setBorder(comboBoxBorder);
		//comboBox.setBackground(GUI.DASHBOARD);
		contentPane.add(comboBox);

		int fromNode = (int)comboBox.getSelectedItem();
		DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);

		Set<Integer> listOfNodes = new HashSet<Integer>();
		List edges = nodeA.getOutEdges();
		for (Object o: edges) {
			DirectedEdge e = (DirectedEdge) o;
			DirectedNode other = e.getOutNode();
			if (roadNetwork.getNumberOfLanes().get(e.getID()) != null)	listOfNodes.add(other.getID()); //if there is no lane number information (e.g. ferry) skip edge
		}
		Integer[] arrayOfNodes = listOfNodes.toArray(new Integer[0]);
		Arrays.sort(arrayOfNodes);

		JComboBox comboBox_1 = new JComboBox();
		comboBox_1.setModel(new DefaultComboBoxModel(arrayOfNodes));
		//comboBox_1.setModel(new DefaultComboBoxModel(new String[] {"24", "25", "1", "22", "34", "16"}));
		comboBox_1.setBounds(1404, 886, 149, 20);
		contentPane.add(comboBox_1);

		comboBox.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {

				int fromNode = (int)comboBox.getSelectedItem();
				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);

				Set<Integer> listOfNodes = new HashSet<Integer>();
				List edges = nodeA.getOutEdges();
				for (Object o: edges) {
					DirectedEdge e2 = (DirectedEdge) o;
					DirectedNode other = e2.getOutNode();//e2.getOtherNode(nodeA);
					if (roadNetwork.getNumberOfLanes().get(e2.getID()) != null)	listOfNodes.add(other.getID()); //if there is no lane number information (e.g. ferry) skip edge
				}
				Integer[] arrayOfNodes = listOfNodes.toArray(new Integer[0]);
				Arrays.sort(arrayOfNodes);
				comboBox_1.setModel(new DefaultComboBoxModel(arrayOfNodes));

			}
		});

		comboBox_1.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {

				int fromNode = (int)comboBox.getSelectedItem();
				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);

				int toNode = (int)comboBox_1.getSelectedItem();
				DirectedNode nodeB = (DirectedNode) roadNetwork.getNodeIDtoNode().get(toNode);

				DirectedEdge edge = (DirectedEdge) nodeA.getOutEdge(nodeB);
				if (roadNetwork.getEdgeIDtoOtherDirectionEdgeID().get(edge.getID()) == null) //if there is no other direction edge, disable checkbox
					chckbxNewCheckBox.setEnabled(false);
				else
					chckbxNewCheckBox.setEnabled(true);

			}
		});

		JLabel lblANode = new JLabel("A node");
		lblANode.setLabelFor(comboBox);
		lblANode.setBounds(1404, 813, 46, 14);
		contentPane.add(lblANode);

		JLabel lblBNode = new JLabel("B node");
		lblBNode.setLabelFor(comboBox_1);
		lblBNode.setBounds(1404, 861, 46, 14);
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
		slider.setBounds(1404, 947, 138, 45);
		slider.setBackground(GUI.DASHBOARD);
		contentPane.add(slider);

		JLabel lblLanesToAdd = new JLabel("Lanes to add:");
		lblLanesToAdd.setBounds(1404, 921, 177, 14);
		contentPane.add(lblLanesToAdd);

		JButton btnNewButton = new JButton("RUN");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				//RandomSingleton.getInstance().setSeed(1234);

				int fromNode = (int) comboBox.getSelectedItem();
				int toNode = (int) comboBox_1.getSelectedItem();
				int lanes = slider.getValue();

				System.out.println("fromNode = " + fromNode);
				System.out.println("toNode = " + toNode);

				DirectedNode nodeA = (DirectedNode) roadNetwork.getNodeIDtoNode().get(fromNode);
				DirectedNode nodeB = (DirectedNode) roadNetwork.getNodeIDtoNode().get(toNode);
				DirectedEdge edge = (DirectedEdge) nodeA.getOutEdge(nodeB);
				SimpleFeature sf = (SimpleFeature)edge.getObject();
				Long countPoint = (long) sf.getAttribute("CP");

				Properties props2 = new Properties();
				props2.setProperty("startYear", "2016");
				props2.setProperty("endYear", "2025");
				props2.setProperty("fromNode", Integer.toString(nodeA.getID()));
				props2.setProperty("toNode", Integer.toString(nodeB.getID()));
				props2.setProperty("CP", Long.toString(countPoint));
				props2.setProperty("number", Integer.toString(lanes));
				RoadExpansion re = new RoadExpansion(props2);

				System.out.println("Road expansion intervention: " + re.toString());
				//interventions.add(re);
				re.install(roadNetwork);

				RoadExpansion re2 = null;
				if (chckbxNewCheckBox.isSelected()) { //if both directions

					edge = (DirectedEdge) nodeB.getEdge(nodeA);
					sf = (SimpleFeature)edge.getObject();
					countPoint = (long) sf.getAttribute("CP");

					props2.setProperty("fromNode", Integer.toString(nodeB.getID()));
					props2.setProperty("toNode", Integer.toString(nodeA.getID()));
					props2.setProperty("CP", Long.toString(countPoint));
					re2 = new RoadExpansion(props2);

					System.out.println("Road expansion intervention: " + re2.toString());
					//interventions.add(re);
					re2.install(roadNetwork);
				}

				RoadNetworkAssignment rnaAfterExpansion = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
				rnaAfterExpansion.assignPassengerFlowsRouting(odm, rsg);
				rnaAfterExpansion.updateLinkVolumeInPCU();
				rnaAfterExpansion.updateLinkVolumeInPCUPerTimeOfDay();

				//predict change in demand
				SkimMatrix tsm = rnaAfterExpansion.calculateTimeSkimMatrix();
				SkimMatrix csm = rnaAfterExpansion.calculateCostSkimMatrix();

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

				rnaAfterExpansion.resetLinkVolumes();
				rnaAfterExpansion.resetTripStorages();

				rnaAfterExpansion.assignPassengerFlowsRouting(predictedODM, rsg);
				rnaAfterExpansion.updateLinkVolumeInPCU();
				rnaAfterExpansion.updateLinkVolumeInPCUPerTimeOfDay();
				//SkimMatrix sm = rnaAfterExpansion.calculateTimeSkimMatrix();

				HashMap<Integer, Double> capacityAfter = rnaAfterExpansion.calculateDirectionAveragedPeakLinkCapacityUtilisation();
				//String shapefilePathAfter = "./temp/networkWithCapacityUtilisationAfter.shp";
				String shapefilePathAfter = "./temp/after" +  GUI.counter++ + ".shp";
				JFrame rightFrame;
				JButton reset = null;
				try {
					rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
					rightFrame.setVisible(false);
					rightFrame.repaint();

					JMapPane pane = ((JMapFrameDemo)rightFrame).getMapPane();
					//((JMapFrameDemo)rightFrame).getToolBar().setBackground(GUI.TOOLBAR); //to set toolbar background

					System.out.println("component: " + ((JMapFrameDemo)rightFrame).getToolBar().getComponent(8).toString());
					reset = (JButton) ((JMapFrameDemo)rightFrame).getToolBar().getComponent(8);
					//reset.setBackground(Color.BLUE); //set icon background
					//reset.setBorderPainted(false); //remove border
					JButton minus = (JButton) ((JMapFrameDemo)rightFrame).getToolBar().getComponent(2);
					//minus.setBackground(Color.GREEN); //set icon background

					//panel_2.removeAll();
					panel_2.add(rightFrame.getContentPane(), 0);
					panel_2.setLayout(null);
					//panel_2.doLayout();
					//panel_2.repaint();

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//reset.doClick(4000);

				//update tables
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


				SkimMatrix sm = rnaAfterExpansion.calculateTimeSkimMatrix();
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

				//update bar chart
				barDataset.addValue(rnaAfterExpansion.getTripList().size(), "Road expansion", "Number of Trips");
				//barDataset.addValue(predictedODM.getTotalFlow(), "Road expansion", "Number of Trips");

				re.uninstall(roadNetwork);
				if (re2 != null) re2.uninstall(roadNetwork);

				//pack();
			}
		});

		btnNewButton.setFont(new Font("Calibri Light", Font.BOLD, 25));
		btnNewButton.setBounds(1686, 799, 200, 169);
		contentPane.add(btnNewButton);

		JLabel lblRoadExpansionPolicy = new JLabel("Road Expansion Policy");
		lblRoadExpansionPolicy.setForeground(Color.DARK_GRAY);
		lblRoadExpansionPolicy.setFont(new Font("Calibri Light", Font.BOLD, 16));
		lblRoadExpansionPolicy.setBounds(1404, 755, 380, 30);
		contentPane.add(lblRoadExpansionPolicy);

		//final String roadExpansionFileName = props.getProperty("roadExpansionFile");
		//List<Intervention> interventions = new ArrayList<Intervention>();
		//RoadExpansion re = new RoadExpansion(roadExpansionFileName);
		//Properties props2 = new Properties();
		//props2.setProperty("startYear", "2016");
		//props2.setProperty("endYear", "2025");
		//props2.setProperty("fromNode", "22");
		//props2.setProperty("toNode", "23");
		//props2.setProperty("CP", "6935");
		//props2.setProperty("number", "2");
		//RoadExpansion re = new RoadExpansion(props2);

		//set controls to represent the intervention
		comboBox.setSelectedItem(new Integer(22));
		comboBox_1.setSelectedItem(new Integer(23));
		slider.setValue(2);
		chckbxNewCheckBox.setSelected(false);

		//run the default intervention
		btnNewButton.doClick();

		/*
		System.out.println("Road expansion intervention: " + re.toString());
		//interventions.add(re);
		re.install(roadNetwork);

		RoadNetworkAssignment rnaAfterExpansion = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, props);
		rnaAfterExpansion.assignPassengerFlows(odm, rsg);
		rnaAfterExpansion.updateLinkVolumeInPCU();
		rnaAfterExpansion.updateLinkVolumeInPCUPerTimeOfDay();
		HashMap<Integer, Double> capacityAfter = rnaAfterExpansion.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		JFrame rightFrame = NetworkVisualiserDemo.visualise(roadNetwork, "Capacity Utilisation After Intervention", capacityAfter, "CapUtil", shapefilePathAfter);
		rightFrame.setVisible(false);

		re.uninstall(roadNetwork);

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

		//update tables
		rows = odm.getOrigins().size();
		columns = odm.getDestinations().size();
		Object[][] data3 = new Object[rows][columns + 1];
		for (int i = 0; i < rows; i++) {
			data3[i][0] = zoning.getLADToName().get(odm.getOrigins().get(i));
			for (int j = 0; j < columns; j++) {
				data3[i][j+1] = odm.getFlow(odm.getOrigins().get(i), odm.getDestinations().get(j));
			}
		}
		String[] labels3 = new String[columns + 1];
		labels3[0] = "TRIPS";
		for (int j = 0; j < columns; j++) labels3[j+1] = zoning.getLADToName().get(odm.getDestinations().get(j));
		table_2.setModel(new DefaultTableModel(data3, labels3));


		SkimMatrix sm4 = rnaAfterExpansion.calculateTimeSkimMatrix();
		rows = sm4.getOrigins().size();
		columns = sm4.getDestinations().size();
		Object[][] data4 = new Object[rows][columns + 1];
		for (int i = 0; i < rows; i++) {
			data4[i][0] = zoning.getLADToName().get(sm4.getOrigins().get(i));
			for (int j = 0; j < columns; j++) {
				data4[i][j+1] = String.format("%.2f", sm4.getCost(sm4.getOrigins().get(i), sm4.getDestinations().get(j)));
			}
		}
		String[] labels4 = new String[columns + 1];
		labels4[0] = "TRAVEL TIME";
		for (int j = 0; j < columns; j++) labels4[j+1] = zoning.getLADToName().get(sm4.getDestinations().get(j));
		table_3.setModel(new DefaultTableModel(data4, labels4));

		//update bar chart
		barDataset.addValue(rnaBefore.getTripList().size(), "No intervention", "Number of Trips");
		barDataset.addValue(rnaAfterExpansion.getTripList().size(), "Road expansion", "Number of Trips");

		 */

		pack();
		//this.setExtendedState(this.getExtendedState()|JFrame.MAXIMIZED_BOTH );


	}
}
