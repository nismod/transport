package nismod.transport.visualisation;

import java.awt.Color;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Map;

import javax.swing.ImageIcon;

import org.geotools.brewer.color.ColorBrewer;
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
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;

import nismod.transport.network.road.RoadNetwork;

/**
 * For visualising the road network.
 * @author Milan Lovric
  */
public class NetworkVisualiser {

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
	 }
	
	/**
	 * Visualises the road network with dailyVolume.
	 * @param roadNetwork Road network.
	 * @param mapTitle Map title for the window.
	 * @param dailyVolume Traffic volume to classify and colour road links.
	 */
	public static void visualise(RoadNetwork roadNetwork, String mapTitle, Map<Integer, Double> dailyVolume) throws IOException {
		
		
		SimpleFeatureCollection networkFeatures = roadNetwork.createNetworkFeatureCollection(dailyVolume);
		networkFeatures = roadNetwork.getNewNetworkShapefile().getFeatureSource().getFeatures();
	
	    //classify road links based on the traffic volume
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	    PropertyName propertyExpression = ff.property("DayVolume");
	    
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
}
