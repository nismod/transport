package nismod.transport.visualisation;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
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
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.network.road.RoadNetwork;

/**
 * For visualising the road network.
 * @author Milan Lovric
  */
public class NetworkVisualiser {
	
	private final static Logger LOGGER = Logger.getLogger(NetworkVisualiser.class.getName());

	protected NetworkVisualiser() {

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
	public static void visualise(RoadNetwork roadNetwork, String mapTitle, Map<Integer, Double> linkData, String linkDataLabel, String shapefilePath) throws IOException {
		
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
	public static void visualise(RoadNetwork roadNetwork, String mapTitle, Map<Integer, Double> linkData, String linkDataLabel, String shapefilePath, URL congestionChargeZoneUrl) throws IOException {
		
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
	}
}
