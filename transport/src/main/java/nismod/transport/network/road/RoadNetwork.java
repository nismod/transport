/**
 * 
 */
package nismod.transport.network.road;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.swing.JMapFrame;

/**
 * A routable road network built from the shapefiles
 * @author Milan Lovric
 *
 */
public class RoadNetwork {

	private DirectedGraph network;
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore networkShapefile;
	private ShapefileDataStore nodesShapefile;
	private ShapefileDataStore AADFshapefile;

	/**
	 * @param zonesUrl Url for the shapefile with zone polygons
	 * @param networkUrl Url for the shapefile with road network
	 * @param nodesUrl Url for the shapefile with nodes
	 * @param AADFurl Url for the shapefile with AADF counts
	 */
	public RoadNetwork(URL zonesUrl, URL networkUrl, URL nodesUrl, URL AADFurl) {

		zonesShapefile = new ShapefileDataStore(zonesUrl);
		networkShapefile = new ShapefileDataStore(networkUrl);
		nodesShapefile = new ShapefileDataStore(nodesUrl);
		AADFshapefile = new ShapefileDataStore(AADFurl);
	}

	/**
	 * Visualises the road network as loaded from shapefiles
	 * @throws IOException
	 */
	public void visualise() throws IOException {

		//create a map
		MapContent map = new MapContent();

		//set windows title
		map.setTitle("Test Area");

		//create style for zones
		StyleBuilder styleBuilder = new StyleBuilder();
		PolygonSymbolizer symbolizer = styleBuilder.createPolygonSymbolizer(Color.DARK_GRAY, Color.BLACK, 1);
		symbolizer.getFill().setOpacity(styleBuilder.literalExpression(0.5));
		org.geotools.styling.Style zonesStyle = styleBuilder.createStyle(symbolizer);

		//add zones layer to the map     
		FeatureLayer zonesLayer = new FeatureLayer(zonesShapefile.getFeatureSource(), zonesStyle);
		map.addLayer(zonesLayer);

		//create style for road network
		Style networkStyle = SLD.createLineStyle(Color.GREEN, 2.0f, "CP_Number", null);
		
		//add network layer to the map
		FeatureLayer networkLayer = new FeatureLayer(networkShapefile.getFeatureSource(), networkStyle);
		map.addLayer(networkLayer);

		//create style for nodes
		Style nodesStyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.RED, 1, 4, "nodeID", null);

		//add nodes layer to the map     
		FeatureLayer nodesLayer = new FeatureLayer(nodesShapefile.getFeatureSource(), nodesStyle);
		map.addLayer(nodesLayer);					

		//create style for AADF counts
		Style AADFstyle = SLD.createPointStyle("Circle", Color.DARK_GRAY, Color.YELLOW, 1, 4, "CP", null);
		
		//add counts layer to the map     
		FeatureLayer AADFlayer = new FeatureLayer(AADFshapefile.getFeatureSource(), AADFstyle);
		map.addLayer(AADFlayer);

		//show the map 
		JMapFrame.showMap(map);
	}
}
