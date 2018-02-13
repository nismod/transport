package nismod.transport.zone;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.CachingFeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import nismod.transport.network.road.RoadNetwork;

/**
 * For mapping zones (e.g. TEMPRO) to the nodes of the road network.
 * @author Milan Lovric
  */
public class Zoning {
	
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore nodesShapefile;
	private RoadNetwork rn;
	
	private HashMap<String, Integer> zoneToNearestNodeID;
	private HashMap<String, Double> zoneToNearestNodeDistance;
	

	/**
	 * @param zonesUrl
	 * @param nodesUrl
	 * @param rn
	 * @throws IOException
	 */
	public Zoning(URL zonesUrl, URL nodesUrl, RoadNetwork rn) throws IOException {
	
		this.zonesShapefile = new ShapefileDataStore(zonesUrl);
		this.nodesShapefile = new ShapefileDataStore(nodesUrl);
		this.rn = rn;
		
		CachingFeatureSource cache3 = new CachingFeatureSource(nodesShapefile.getFeatureSource());
		SimpleFeatureCollection nodesFeatureCollection = cache3.getFeatures();
		CachingFeatureSource cache4 = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		SimpleFeatureCollection zonesFeatureCollection = cache4.getFeatures();
		
		//map the nodes to zones
		mapNodesToZones(zonesFeatureCollection);
	}

	/**
	 * Maps the nodes of the graph to the zone codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapNodesToZones(SimpleFeatureCollection zonesFeatureCollection) {

		this.zoneToNearestNodeID = new HashMap<String, Integer>();
		this.zoneToNearestNodeDistance = new HashMap<String, Double>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");

				Point centroid = polygon.getCentroid();
				double minDistance = Double.MAX_VALUE;
				Integer nearestNodeID = null;

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();

					double distanceToNode = centroid.distance(point);
					if (distanceToNode < minDistance) {
						minDistance = distanceToNode;
						nearestNodeID = node.getID();
					}
				}

				this.zoneToNearestNodeID.put(zoneID, nearestNodeID);
				this.zoneToNearestNodeDistance.put(zoneID, minDistance);

			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
	
	/**
	 * Getter for zone centroid to nearest node ID mapping.
	 * @return Zone to node map.
	 */
	public HashMap<String, Integer> getZoneToNearestNodeIDMap() {
		
		return this.zoneToNearestNodeID;
		
	}
	
	/**
	 * Getter for zone centroid to nearest node distance mapping (in meters).
	 * @return Zone to distance map.
	 */
	public HashMap<String, Double> getZoneToNearestNodeDistanceMap() {
		
		return this.zoneToNearestNodeDistance;
		
	}
}
