package nismod.transport.showcase;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.CachingFeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.function.Classifier;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Font;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.MyStyleGenerator;
import nismod.transport.visualisation.NetworkVisualiser;

/**
 * For visualising the road network.
 * @author Milan Lovric
  */
public class NetworkVisualiserDemo {
	
	private final static Logger LOGGER = Logger.getLogger(NetworkVisualiserDemo.class.getName());
	
	public static void main( String[] args ) throws IOException	{

		final String configFile = "./src/test/config/testConfig.properties";
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

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, null, props);
		ODMatrix odm = new ODMatrix("./src/test/resources/testdata/csvfiles/passengerODM.csv");
		rna.assignPassengerFlowsRouting(odm, null);
		
		final URL congestionChargeZoneUrl = new URL("file://src/test/resources/testdata/shapefiles/congestionChargingZone.shp");
		String shapefilePath = "./temp/networkWithDailyVolume.shp";
		String shapefilePath2 = "./temp/networkWithCountComparison.shp";
		String shapefilePath3 = "./temp/networkWithCountComparison.shp";
		
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumePerVehicleType();
		Map<Integer, Double> dailyVolume = rna.getLinkVolumeInPCU();
		//NetworkVisualiser.visualise(roadNetwork, "Network from shapefiles");
		NetworkVisualiserDemo.visualise(roadNetwork, "Network with traffic volume", dailyVolume, "DayVolume", shapefilePath);
		//NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "AbsDiffCounts", shapefilePath2);
		//NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "AbsDiffCounts", shapefilePath3, congestionChargeZoneUrl);
		
	}

	protected NetworkVisualiserDemo() {

	}

	/**
	 * Visualises the road network as loaded from the shapefiles.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 */
	public static void visualise(RoadNetwork roadNetwork, String mapTitle) throws IOException {
		
		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		
		//create fonts
		Font font1 = styleBuilder.createFont("Lucida Sans", false, false, 10);
		Font font2 = styleBuilder.createFont("Arial", false, false, 10);
		Font zonesFont = styleBuilder.createFont("Arial", false, true, 14);
				
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);
		
		TextSymbolizer textSymbolizer = styleBuilder.createTextSymbolizer(Color.DARK_GRAY, zonesFont, "NAME");
		Symbolizer[] syms = new Symbolizer[2];
		syms[0] = symbolizer; syms[1] = textSymbolizer;
	
		Style zonesStyle4 = styleBuilder.createStyle(textSymbolizer);		
		Style zonesStyle2 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f);
		Style zonesStyle3 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f, "NAME", zonesFont);
		
		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(roadNetwork.getZonesShapefile().getFeatureSource(), (Style) zonesStyle3);
		map.addLayer(zonesLayer);

		//create style for road network
		Style networkStyle = SLD.createLineStyle(Color.GREEN, 4.0f, "RoadNumber", font2);

		//add network layer to the map
		FeatureLayer networkLayer;
		if (roadNetwork.getNewNetworkShapefile() != null)
			networkLayer = new FeatureLayer(roadNetwork.getNewNetworkShapefile().getFeatureSource(), networkStyle);
		else
			networkLayer = new FeatureLayer(roadNetwork.getNetworkShapefile().getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);

		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.BLUE, 1, 5, "nodeID", font2);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(roadNetwork.getNodesShapefile().getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, null, font2);

		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(roadNetwork.getAADFShapefile().getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map in JMapFrame
		JMapFrame show = new JMapFrame(map);
		
	  	//list layers and set them as visible + selected
		show.enableLayerTable(true);
		//zoom in, zoom out, pan, show all
		show.enableToolBar(true);
		//location of cursor and bounds of current
		show.enableStatusBar(true);
		//display
		show.setVisible(true);
		//maximise window
		show.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		
		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		show.setIconImage(icon.getImage());
		
		show.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		
		//improve rendering
		GTRenderer renderer = show.getMapPane().getRenderer();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
	    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	    renderer.setJava2DHints(hints);
	    show.getMapPane().setRenderer(renderer);
	    
        // Create a JMapFrame with a menu to choose the display style for the
		JMapFrame frame = new JMapFrame(map);
        frame.setSize(800, 600);
        frame.enableStatusBar(true);
        //frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
        frame.enableToolBar(true);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        JMenu menu = new JMenu("Raster");
        menuBar.add(menu);

        menu.add( new SafeAction("Grayscale display") {
            public void action(ActionEvent e) throws Throwable {
  
            }
        });

        menu.add( new SafeAction("RGB display") {
            public void action(ActionEvent e) throws Throwable {

           }
        });
        
        // Finally display the map frame.
        // When it is closed the app will exit.
        frame.setVisible(true);
	 }
	
	
	
	/**
	 * Visualises the road network with dailyVolume.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 * @param linkData Data used to classify and colour road links.
	 * @param linkDataLabel Label describing the link data used.
	 * @param shapefilePath The path to the shapefile into which data will be stored.
	 */
	public static JFrame visualise(URL zonesUrl, URL networkUrl, URL nodesUrl, URL AADFurl, String mapTitle, String linkDataLabel) throws IOException {
		
		String linkDataLabelTruncated = linkDataLabel;
		if (linkDataLabelTruncated.length() > 10) linkDataLabelTruncated = linkDataLabel.substring(0, 10);
		
		ShapefileDataStore zonesShapefile = new ShapefileDataStore(zonesUrl);
		ShapefileDataStore networkShapefile = new ShapefileDataStore(networkUrl);
		ShapefileDataStore nodesShapefile = new ShapefileDataStore(nodesUrl);
		ShapefileDataStore AADFshapefile = new ShapefileDataStore(AADFurl);
		
		
		//CachingFeatureSource cache = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		//SimpleFeatureCollection zonesFeatureCollection = cache.getFeatures();
	
	    //classify road links based on the traffic volume
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    PropertyName propertyExpression = ff.property(linkDataLabelTruncated);
	    
	    //FeatureCollection featureCollection = this.newNetworkShapefile.getFeatureSource().getFeatures();
	    FeatureCollection featureCollection = networkShapefile.getFeatureSource().getFeatures();
		    
	    // classify into five categories
	    Function classify = ff.function("Quantile", propertyExpression, ff.literal(5));
	    Classifier groups = (Classifier) classify.evaluate(featureCollection);
	    
	    //use a predefined colour palette from the color brewer
	    ColorBrewer brewer = ColorBrewer.instance();
	    //System.out.println(Arrays.toString(brewer.getPaletteNames()));
	    String paletteName = "RdYlGn";
	    Color[] colours = brewer.getPalette(paletteName).getColors(5);
	    Color[] coloursReversed = new Color[5];
	    for (int i = 0; i < 5; i++) coloursReversed[i] = colours[4-i]; //reverse colours
	    
	    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
	    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

	    // Use MyStyleGenerator to make a set of rules for the Classifier
	    // assigning features the correct colour based on traffic volume
	    FeatureTypeStyle style = MyStyleGenerator.createFeatureTypeStyle(
	            groups,
	            propertyExpression,
	            coloursReversed,
	            "Generated FeatureTypeStyle for RdYlGn",
	            featureCollection.getSchema().getGeometryDescriptor(),
	            MyStyleGenerator.ELSEMODE_IGNORE,
	            0.95f,
	            4.0f,
	            null);
	            //stroke);
	    
		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		
		//create fonts
		Font font1 = styleBuilder.createFont("Lucida Sans", false, false, 10);
		Font font2 = styleBuilder.createFont("Arial", false, false, 10);
		Font zonesFont = styleBuilder.createFont("Arial", false, true, 14);
				
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);
		
		TextSymbolizer textSymbolizer = styleBuilder.createTextSymbolizer(Color.DARK_GRAY, zonesFont, "NAME");
		Symbolizer[] syms = new Symbolizer[2];
		syms[0] = symbolizer; syms[1] = textSymbolizer;
	
		Style zonesStyle4 = styleBuilder.createStyle(textSymbolizer);		
		Style zonesStyle2 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f);
		Style zonesStyle3 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f, "NAME", zonesFont);
		
		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(zonesShapefile.getFeatureSource(), (Style) zonesStyle3);
		map.addLayer(zonesLayer);

		//create style for road network
		Style networkStyle = styleFactory.createStyle(); // = SLD.createLineStyle(Color.GREEN, 4.0f, "RoadNumber", font2);
		
		//networkStyle.featureTypeStyles().add(fts);
		networkStyle.featureTypeStyles().add(style);
	
		//add network layer to the map
		FeatureLayer networkLayer = new FeatureLayer(networkShapefile.getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);
		
		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.BLUE, 1, 5, "nodeID", font2);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(nodesShapefile.getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, null, font2);
		
		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(AADFshapefile.getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map in JMapFrame
		JMapFrameDemo show = new JMapFrameDemo(map);
		//JMapFrameDemo show2 = new JMapFrameDemo(map);
		//show2.setVisible(true);
		
	  	//list layers and set them as visible + selected
		show.enableLayerTable(false);
		//zoom in, zoom out, pan, show all
		show.enableToolBar(true);
		//location of cursor and bounds of current
		show.enableStatusBar(true);
		//display
		show.setVisible(true);
		
		show.pack();
		
		//maximise window
		show.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
				
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//show.setSize(screenSize.width / 2, screenSize.height);
		//show.setLocation(screenSize.width / 2, 0);
		
		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		show.setIconImage(icon.getImage());
		
		show.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		
		//improve rendering
		GTRenderer renderer = show.getMapPane().getRenderer();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
	    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	    renderer.setJava2DHints(hints);
	    show.getMapPane().setRenderer(renderer);
	    
	    
//        JPanel panel1 = new JPanel();
//        JPanel panel2 = new JPanel();
//        
//       // panel1.add(show.getComponent(0));
//       // panel2.add(show2.getComponent(0));
//       
//        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);
//        
//        show.getContentPane().add(splitPane);
        
       return show;
	}
	

	/**
	 * Visualises the road network with dailyVolume.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 * @param linkData Data used to classify and colour road links.
	 * @param linkDataLabel Label describing the link data used.
	 * @param shapefilePath The path to the shapefile into which data will be stored.
	 */
	public static JFrame getMap(URL zonesUrl, URL networkUrl, URL nodesUrl, URL AADFurl, String mapTitle, String linkDataLabel) throws IOException {
		
		String linkDataLabelTruncated = linkDataLabel;
		if (linkDataLabelTruncated.length() > 10) linkDataLabelTruncated = linkDataLabel.substring(0, 10);
		
		ShapefileDataStore zonesShapefile = new ShapefileDataStore(zonesUrl);
		ShapefileDataStore networkShapefile = new ShapefileDataStore(networkUrl);
		ShapefileDataStore nodesShapefile = new ShapefileDataStore(nodesUrl);
		ShapefileDataStore AADFshapefile = new ShapefileDataStore(AADFurl);
		
		
		//CachingFeatureSource cache = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		//SimpleFeatureCollection zonesFeatureCollection = cache.getFeatures();
	
	    //classify road links based on the traffic volume
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    PropertyName propertyExpression = ff.property(linkDataLabelTruncated);
	    
	    //FeatureCollection featureCollection = this.newNetworkShapefile.getFeatureSource().getFeatures();
	    FeatureCollection featureCollection = networkShapefile.getFeatureSource().getFeatures();
		    
	    // classify into five categories
	    Function classify = ff.function("Quantile", propertyExpression, ff.literal(5));
	    Classifier groups = (Classifier) classify.evaluate(featureCollection);
	    
	    //use a predefined colour palette from the color brewer
	    ColorBrewer brewer = ColorBrewer.instance();
	    //System.out.println(Arrays.toString(brewer.getPaletteNames()));
	    String paletteName = "RdYlGn";
	    Color[] colours = brewer.getPalette(paletteName).getColors(5);
	    Color[] coloursReversed = new Color[5];
	    for (int i = 0; i < 5; i++) coloursReversed[i] = colours[4-i]; //reverse colours
	    
	    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
	    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

	    // Use MyStyleGenerator to make a set of rules for the Classifier
	    // assigning features the correct colour based on traffic volume
	    FeatureTypeStyle style = MyStyleGenerator.createFeatureTypeStyle(
	            groups,
	            propertyExpression,
	            coloursReversed,
	            "Generated FeatureTypeStyle for RdYlGn",
	            featureCollection.getSchema().getGeometryDescriptor(),
	            MyStyleGenerator.ELSEMODE_IGNORE,
	            0.95f,
	            4.0f,
	            null);
	            //stroke);
	    
		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		
		//create fonts
		Font font1 = styleBuilder.createFont("Lucida Sans", false, false, 10);
		Font font2 = styleBuilder.createFont("Arial", false, false, 10);
		Font zonesFont = styleBuilder.createFont("Arial", false, true, 14);
				
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);
		
		TextSymbolizer textSymbolizer = styleBuilder.createTextSymbolizer(Color.DARK_GRAY, zonesFont, "NAME");
		Symbolizer[] syms = new Symbolizer[2];
		syms[0] = symbolizer; syms[1] = textSymbolizer;
	
		Style zonesStyle4 = styleBuilder.createStyle(textSymbolizer);		
		Style zonesStyle2 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f);
		Style zonesStyle3 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f, "NAME", zonesFont);
		
		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(zonesShapefile.getFeatureSource(), (Style) zonesStyle3);
		map.addLayer(zonesLayer);

		//create style for road network
		Style networkStyle = styleFactory.createStyle(); // = SLD.createLineStyle(Color.GREEN, 4.0f, "RoadNumber", font2);
		
		//networkStyle.featureTypeStyles().add(fts);
		networkStyle.featureTypeStyles().add(style);
	
		//add network layer to the map
		FeatureLayer networkLayer = new FeatureLayer(networkShapefile.getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);
		
		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.BLUE, 1, 5, "nodeID", font2);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(nodesShapefile.getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, null, font2);
		
		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(AADFshapefile.getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map in JMapFrame
		JMapFrameDemo show = new JMapFrameDemo(map);
		//JMapFrameDemo show2 = new JMapFrameDemo(map);
		//show2.setVisible(true);
		
	  	//list layers and set them as visible + selected
		show.enableLayerTable(false);
		//zoom in, zoom out, pan, show all
		show.enableToolBar(true);
		//location of cursor and bounds of current
		show.enableStatusBar(true);
		//display
		//show.setVisible(true);
		
		//show.pack();
		
		//maximise window
		//show.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
				
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//show.setSize(screenSize.width / 2, screenSize.height);
		//show.setLocation(screenSize.width / 2, 0);
		
		//ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		//show.setIconImage(icon.getImage());
		
		//show.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		
		//improve rendering
		GTRenderer renderer = show.getMapPane().getRenderer();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
	    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	    renderer.setJava2DHints(hints);
	    show.getMapPane().setRenderer(renderer);
	    
	    
//        JPanel panel1 = new JPanel();
//        JPanel panel2 = new JPanel();
//        
//       // panel1.add(show.getComponent(0));
//       // panel2.add(show2.getComponent(0));
//       
//        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);
//        
//        show.getContentPane().add(splitPane);
        
       return show;
	}
	
	
	
	/**
	 * Visualises the road network with dailyVolume.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 * @param linkData Data used to classify and colour road links.
	 * @param linkDataLabel Label describing the link data used.
	 * @param shapefilePath The path to the shapefile into which data will be stored.
	 */
	public static JFrame visualise(RoadNetwork roadNetwork, String mapTitle, Map<Integer, Double> linkData, String linkDataLabel, String shapefilePath) throws IOException {
		
		String linkDataLabelTruncated = linkDataLabel;
		if (linkDataLabelTruncated.length() > 10) linkDataLabelTruncated = linkDataLabel.substring(0, 10);
		
		SimpleFeatureCollection networkFeatures = roadNetwork.createNetworkFeatureCollection(linkData, linkDataLabelTruncated, shapefilePath);
		networkFeatures = roadNetwork.getNewNetworkShapefile().getFeatureSource().getFeatures();
	
	    //classify road links based on the traffic volume
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    PropertyName propertyExpression = ff.property(linkDataLabelTruncated);
	    
	    //FeatureCollection featureCollection = this.newNetworkShapefile.getFeatureSource().getFeatures();
	    FeatureCollection featureCollection = networkFeatures;
		    
	    // classify into five categories
	    Function classify = ff.function("Quantile", propertyExpression, ff.literal(5));
	    Classifier groups = (Classifier) classify.evaluate(featureCollection);
	    
	    //use a predefined colour palette from the color brewer
	    ColorBrewer brewer = ColorBrewer.instance();
	    //System.out.println(Arrays.toString(brewer.getPaletteNames()));
	    String paletteName = "RdYlGn";
	    Color[] colours = brewer.getPalette(paletteName).getColors(5);
	    Color[] coloursReversed = new Color[5];
	    for (int i = 0; i < 5; i++) coloursReversed[i] = colours[4-i]; //reverse colours
	    
	    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
	    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

	    // Use MyStyleGenerator to make a set of rules for the Classifier
	    // assigning features the correct colour based on traffic volume
	    FeatureTypeStyle style = MyStyleGenerator.createFeatureTypeStyle(
	            groups,
	            propertyExpression,
	            coloursReversed,
	            "Generated FeatureTypeStyle for RdYlGn",
	            featureCollection.getSchema().getGeometryDescriptor(),
	            MyStyleGenerator.ELSEMODE_IGNORE,
	            0.95f,
	            4.0f,
	            null);
	            //stroke);
	    
		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		
		//create fonts
		Font font1 = styleBuilder.createFont("Lucida Sans", false, false, 10);
		Font font2 = styleBuilder.createFont("Arial", false, false, 10);
		Font zonesFont = styleBuilder.createFont("Arial", false, true, 14);
				
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);
		
		TextSymbolizer textSymbolizer = styleBuilder.createTextSymbolizer(Color.DARK_GRAY, zonesFont, "NAME");
		Symbolizer[] syms = new Symbolizer[2];
		syms[0] = symbolizer; syms[1] = textSymbolizer;
	
		Style zonesStyle4 = styleBuilder.createStyle(textSymbolizer);		
		Style zonesStyle2 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f);
		Style zonesStyle3 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f, "NAME", zonesFont);
				
		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(roadNetwork.getZonesShapefile().getFeatureSource(), (Style) zonesStyle3);
		map.addLayer(zonesLayer);

		//create style for road network
		Style networkStyle = styleFactory.createStyle(); // = SLD.createLineStyle(Color.GREEN, 4.0f, "RoadNumber", font2);
		
		//networkStyle.featureTypeStyles().add(fts);
		networkStyle.featureTypeStyles().add(style);
	
		//add network layer to the map
		FeatureLayer networkLayer = new FeatureLayer(roadNetwork.getNewNetworkShapefile().getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);
		
		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.BLUE, 1, 5, "nodeID", font2);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(roadNetwork.getNodesShapefile().getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, null, font2);
		
		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(roadNetwork.getAADFShapefile().getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map in JMapFrame
		JMapFrameDemo show = new JMapFrameDemo(map);
		//JMapFrameDemo show2 = new JMapFrameDemo(map);
		//show2.setVisible(true);
		
	  	//list layers and set them as visible + selected
		show.enableLayerTable(false);
		//zoom in, zoom out, pan, show all
		show.enableToolBar(true);
		//location of cursor and bounds of current
		show.enableStatusBar(true);
		//display
//		show.setVisible(true);
		
		show.pack();
		
		//maximise window
		show.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
				
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//show.setSize(screenSize.width / 2, screenSize.height);
		//show.setLocation(screenSize.width / 2, 0);
		
		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		show.setIconImage(icon.getImage());
		
		show.setDefaultCloseOperation(JMapFrame.HIDE_ON_CLOSE);
		
		//improve rendering
		GTRenderer renderer = show.getMapPane().getRenderer();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
	    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	    renderer.setJava2DHints(hints);
	    show.getMapPane().setRenderer(renderer);
	    
	    
//        JPanel panel1 = new JPanel();
//        JPanel panel2 = new JPanel();
//        
//       // panel1.add(show.getComponent(0));
//       // panel2.add(show2.getComponent(0));
//       
//        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);
//        
//        show.getContentPane().add(splitPane);
        
       return show;
	}
	
	/**
	 * Visualises the road network with dailyVolume.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 * @param linkData Data used to classify and colour road links.
	 * @param linkDataLabel Label describing the link data used.
	 * @param shapefilePath The path to the shapefile into which data will be stored.
	 * @param congestionChargeZoneUrl The path to the shapefile with the congestion charge zone boundary.
	 */
	public static JFrame visualise(RoadNetwork roadNetwork, String mapTitle, Map<Integer, Double> linkData, String linkDataLabel, String shapefilePath, URL congestionChargeZoneUrl) throws IOException {
		
		String linkDataLabelTruncated = linkDataLabel;
		if (linkDataLabelTruncated.length() > 10) linkDataLabelTruncated = linkDataLabel.substring(0, 10);
		
		SimpleFeatureCollection networkFeatures = roadNetwork.createNetworkFeatureCollection(linkData, linkDataLabelTruncated, shapefilePath);
		networkFeatures = roadNetwork.getNewNetworkShapefile().getFeatureSource().getFeatures();
	
	    //classify road links based on the traffic volume
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    PropertyName propertyExpression = ff.property(linkDataLabelTruncated);
	    
	    //FeatureCollection featureCollection = this.newNetworkShapefile.getFeatureSource().getFeatures();
	    FeatureCollection featureCollection = networkFeatures;
		    
	    // classify into five categories
	    Function classify = ff.function("Quantile", propertyExpression, ff.literal(5));
	    Classifier groups = (Classifier) classify.evaluate(featureCollection);
	    
	    //use a predefined colour palette from the color brewer
	    ColorBrewer brewer = ColorBrewer.instance();
	    //System.out.println(Arrays.toString(brewer.getPaletteNames()));
	    String paletteName = "RdYlGn";
	    Color[] colours = brewer.getPalette(paletteName).getColors(5);
	    Color[] coloursReversed = new Color[5];
	    for (int i = 0; i < 5; i++) coloursReversed[i] = colours[4-i]; //reverse colours
	    
	    StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
	    FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

	    // Use MyStyleGenerator to make a set of rules for the Classifier
	    // assigning features the correct colour based on traffic volume
	    FeatureTypeStyle style = MyStyleGenerator.createFeatureTypeStyle(
	            groups,
	            propertyExpression,
	            coloursReversed,
	            "Generated FeatureTypeStyle for RdYlGn",
	            featureCollection.getSchema().getGeometryDescriptor(),
	            MyStyleGenerator.ELSEMODE_IGNORE,
	            0.95f,
	            4.0f,
	            null);
	            //stroke);
	    
		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		
		//create fonts
		Font font1 = styleBuilder.createFont("Lucida Sans", false, false, 10);
		Font font2 = styleBuilder.createFont("Arial", false, false, 10);
		Font zonesFont = styleBuilder.createFont("Arial", false, true, 14);
				
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);
		
		TextSymbolizer textSymbolizer = styleBuilder.createTextSymbolizer(Color.DARK_GRAY, zonesFont, "NAME");
		Symbolizer[] syms = new Symbolizer[2];
		syms[0] = symbolizer; syms[1] = textSymbolizer;
	
		Style zonesStyle4 = styleBuilder.createStyle(textSymbolizer);		
		Style zonesStyle2 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f);
		Style zonesStyle3 = SLD.createPolygonStyle(Color.DARK_GRAY, Color.DARK_GRAY, 0.5f, "NAME", zonesFont);
		
		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(roadNetwork.getZonesShapefile().getFeatureSource(), (Style) zonesStyle3);
		map.addLayer(zonesLayer);
		
		//add congestion charge zone
		ShapefileDataStore zonesShapefile = new ShapefileDataStore(congestionChargeZoneUrl);
		Style congestionZoneStyle = SLD.createPolygonStyle(Color.DARK_GRAY, Color.RED, 0.5f);
		//add zones layer to the map     
		FeatureLayer congestionChargeLayer = new FeatureLayer(zonesShapefile.getFeatureSource(), (Style) congestionZoneStyle);
		map.addLayer(congestionChargeLayer);

		//create style for road network
		Style networkStyle = styleFactory.createStyle(); // = SLD.createLineStyle(Color.GREEN, 4.0f, "RoadNumber", font2);
		
		//networkStyle.featureTypeStyles().add(fts);
		networkStyle.featureTypeStyles().add(style);
		
		//System.out.println(style.getFeatureInstanceIDs());
		//System.out.println(style);
		
		//add network layer to the map
		FeatureLayer networkLayer = new FeatureLayer(roadNetwork.getNewNetworkShapefile().getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);
		
		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.BLUE, 1, 5, "nodeID", font2);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(roadNetwork.getNodesShapefile().getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, null, font2);
		
		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(roadNetwork.getAADFShapefile().getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map in JMapFrame
		JMapFrameDemo show = new JMapFrameDemo(map);
		
		//show.getContentPane()
		
	  	//list layers and set them as visible + selected
		show.enableLayerTable(false);
		//zoom in, zoom out, pan, show all
		show.enableToolBar(true);
		//location of cursor and bounds of current
		show.enableStatusBar(false);
		//display
		show.setVisible(true);
		//
		show.pack();
		//maximise window
		show.setExtendedState(JMapFrame.MAXIMIZED_BOTH);

		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		show.setIconImage(icon.getImage());
		
		show.setDefaultCloseOperation(JMapFrame.EXIT_ON_CLOSE);
		
		//improve rendering
		GTRenderer renderer = show.getMapPane().getRenderer();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
	    hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
	    hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	    renderer.setJava2DHints(hints);
	    show.getMapPane().setRenderer(renderer);
	    
	    return show;
	}
}
