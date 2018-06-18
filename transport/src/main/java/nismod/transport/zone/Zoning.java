package nismod.transport.zone;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * For mapping Tempro zones to the nodes of the road network.
 * @author Milan Lovric
  */
public class Zoning {
	
	private final static Logger LOGGER = LogManager.getLogger(Zoning.class);
	
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore nodesShapefile;
	private RoadNetwork rn;
	
	private HashMap<String, Integer> zoneToNearestNodeID;
	private HashMap<String, Double> zoneToNearestNodeDistance;
	
	private HashMap<String, Point> zoneToCentroid;
	
	private HashMap<String, List<Pair<Integer, Double>>> zoneToSortedListOfNodeAndDistancePairs;
	
	private HashMap<Integer, String> nodeToZoneInWhichLocated; //maps node to Tempro zone in which it is located
	private HashMap<String, List<Integer>> zoneToListOfContainedNodes; //maps Tempro zone to a list of nodes within that zone (if they exist)
	
	private HashMap<String, String> zoneToLAD; //maps Tempro zone to LAD zone
	private HashMap<String, List<String>> LADToListOfContainedZones; //maps LAD zones to a list of contained Temrpo zones
	
	private HashMap<String, String> LADToName; //maps LAD code to LAD name
	
	private HashMap<String, Integer> temproCodeToID;
	private HashMap<Integer, String> temproIDToCode;
	
	private HashMap<String, NodeMatrix> zoneToNodeMatrix; //mapsTempro zones to a matrix of join node probabilities (for tempro zones that have multiple contained nodes)
		
	/**
	 * Constructor for the zoning system.
	 * @param zonesUrl Url for the zones shapefile.
	 * @param nodesUrl Url for the nodes shapefile.
	 * @param rn Road network.
	 * @throws IOException if any.
	 */
	public Zoning(URL zonesUrl, URL nodesUrl, RoadNetwork rn) throws IOException {
		
		LOGGER.info("Creating the Tempro zoning system.");
	
		this.zonesShapefile = new ShapefileDataStore(zonesUrl);
		this.nodesShapefile = new ShapefileDataStore(nodesUrl);
		this.rn = rn;
		
		CachingFeatureSource cache3 = new CachingFeatureSource(nodesShapefile.getFeatureSource());
		SimpleFeatureCollection nodesFeatureCollection = cache3.getFeatures();
		CachingFeatureSource cache4 = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		SimpleFeatureCollection zonesFeatureCollection = cache4.getFeatures();
		
		//map codes and IDs
		LOGGER.debug("Mapping Tempro zones IDs to their ONS codes...");
		mapCodesAndIDs(zonesFeatureCollection);
		
		//map zones to nearest nodes
		LOGGER.debug("Mapping Tempro zones to the nearest node...");
		mapZonesToNodes(zonesFeatureCollection);
		
		//map nodes to zones
		LOGGER.debug("Mapping nodes to Tempro zones in which they are located...");
		mapNodesToZones(zonesFeatureCollection);
		
		//map zones to all the nodes, including the distance
		LOGGER.debug("Mapping Tempro zones to all the nodes, including distance...");
		mapZonesToNodesAndDistances(zonesFeatureCollection);
		
		//map zones to nodes contained within zone
		LOGGER.debug("Mapping Tempro zones to a list of nodes contained within that zone...");
		mapZonesToContainedNodes();
		
		//map LADs to contained Tempro zones
		LOGGER.debug("Mapping LADs to a list of contained Tempro zones...");
		mapLADsToContainedZones();
		
		//mapZoneToNodeMatrices();
		
		LOGGER.debug("Done creating the zoning system.");
	}
	
	/**
	 * Maps tempro zone IDs to codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapCodesAndIDs(SimpleFeatureCollection zonesFeatureCollection) {

		this.temproCodeToID = new HashMap<String, Integer>();
		this.temproIDToCode = new HashMap<Integer, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				Integer zoneID = ((Long) sf.getAttribute("Zone_ID")).intValue();
				String zoneCode = (String) sf.getAttribute("Zone_Code");
				
				this.temproCodeToID.put(zoneCode, zoneID);
				this.temproIDToCode.put(zoneID, zoneCode);
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
		
	/**
	 * Maps zones to nearest nodes of the network.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapZonesToNodes(SimpleFeatureCollection zonesFeatureCollection) {

		this.zoneToNearestNodeID = new HashMap<String, Integer>();
		this.zoneToNearestNodeDistance = new HashMap<String, Double>();
		
		this.zoneToLAD = new HashMap<String, String>();
		this.LADToName = new HashMap<String, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");
				
				String ladID = (String) sf.getAttribute("LAD_Code");
				this.zoneToLAD.put(zoneID, ladID);
				
				String ladName = (String) sf.getAttribute("Local_Auth");
				this.LADToName.put(ladID, ladName);
				
				Point centroid = polygon.getCentroid();
				double minDistance = Double.MAX_VALUE;
				Integer nearestNodeID = null;

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if node is blacklisted as either start or end node, do not consider that node
					if (rn.isBlacklistedAsStartNode(node.getID()) || rn.isBlacklistedAsEndNode(node.getID())) continue;
					
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
	 * Maps zones to all the nodes of the network and distances, sorted by distance.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapZonesToNodesAndDistances(SimpleFeatureCollection zonesFeatureCollection) {
		
		this.zoneToCentroid = new HashMap<String, Point>();

		this.zoneToSortedListOfNodeAndDistancePairs = new HashMap<String, List<Pair<Integer, Double>>>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");
			
				List<Pair<Integer, Double>> list = new ArrayList<Pair<Integer, Double>>();
				this.zoneToSortedListOfNodeAndDistancePairs.put(zoneID, list);
								
				Point centroid = polygon.getCentroid();
				this.zoneToCentroid.put(zoneID, centroid);

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if node is blacklisted as either start or end node, do not consider that node
					if (rn.isBlacklistedAsStartNode(node.getID()) || rn.isBlacklistedAsEndNode(node.getID())) continue;
					
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();
					double distanceToNode = centroid.distance(point);

					Pair<Integer, Double> pair = Pair.of(node.getID(), distanceToNode);
					list.add(pair);
				}
				
				//sort the list of nodes based on the distance
				Comparator<Pair<Integer, Double>> c = new Comparator<Pair<Integer, Double>>() {
				    public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
				    	Double distance1 = p1.getValue();
				    	Double distance2 = p2.getValue();
				    	return distance1.compareTo(distance2); //ascending
				    }
				};
				
				Collections.sort(list, c);
					
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
	
	private void mapZonesToContainedNodes () {
		
		this.zoneToListOfContainedNodes = new HashMap<String, List<Integer>>();
		
		for (Integer nodeID: this.nodeToZoneInWhichLocated.keySet()) {
			
			String zoneCode = this.nodeToZoneInWhichLocated.get(nodeID);
			List<Integer> listOfNodes = this.zoneToListOfContainedNodes.get(zoneCode);
			if (listOfNodes == null) {
				listOfNodes = new ArrayList<Integer>();
				this.zoneToListOfContainedNodes.put(zoneCode, listOfNodes);
			}
			listOfNodes.add(nodeID);
		}
	}
	
	private void mapZoneToNodeMatrices() {
		
		this.zoneToNodeMatrix = new HashMap<String, NodeMatrix>();
		
		for (String zone: this.zoneToListOfContainedNodes.keySet()) {
			if (this.zoneToListOfContainedNodes.get(zone).size() >= 2) {
				NodeMatrix nm = NodeMatrix.createUnitMatrix(this.zoneToListOfContainedNodes.get(zone));
				nm.normaliseWithZeroDiagonal();
				//nm.normalise();
				this.zoneToNodeMatrix.put(zone, nm);
			}
		}
	}
	
	private void mapLADsToContainedZones() {
		
		this.LADToListOfContainedZones = new HashMap<String, List<String>>();
		
		for (String zone: this.zoneToLAD.keySet()) {
			String lad = this.zoneToLAD.get(zone);
			List<String> list = this.LADToListOfContainedZones.get(lad);
			if (list == null) {
				list = new ArrayList<String>();
				this.LADToListOfContainedZones.put(lad, list);
			}
			list.add(zone);
		}
	}
	
	/**
	 * Maps the nodes of the graph to the zone codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapNodesToZones(SimpleFeatureCollection zonesFeatureCollection) {

		this.nodeToZoneInWhichLocated = new HashMap<Integer, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if nodes already assigned to a zone, skip it
					if (this.nodeToZoneInWhichLocated.containsKey(node.getID())) continue;
					
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();

					//if polygon of the zone contains the node, add it to the map
					if (polygon.contains(point)) this.nodeToZoneInWhichLocated.put(node.getID(), zoneID);
				}
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
		
	/**
	 * Getter for tempro zone code to ID.
	 * @return Tempro zone code to tempro zone ID map.
	 */
	public HashMap<String, Integer> getZoneCodeToIDMap() {
		
		return this.temproCodeToID;
	}
	
	/**
	 * Getter for tempro zone ID to code.
	 * @return Tempro zone ID to tempro zone code.
	 */
	public HashMap<Integer, String> getZoneIDToCodeMap() {
		
		return this.temproIDToCode;
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
	
	/**
	 * Getter for node to zone mapping (for each node gives the zone in which it is located).
	 * @return Node to zone map.
	 */
	public HashMap<Integer, String> getNodeToZoneMap() {
		
		return this.nodeToZoneInWhichLocated;
	}
	
	
	/**
	 * Getter for tempro zone to LAD zone mapping.
	 * @return Tempro zone to LAD zone map.
	 */
	public HashMap<String, String> getZoneToLADMap() {
		
		return this.zoneToLAD;
	}
	
	/**
	 * Getter for tempro zone to sorted node distances mapping (distances to ALL nodes in the network).
	 * @return Zone to sorted list of nodes and distances.
	 */
	public HashMap<String, List<Pair<Integer, Double>>> getZoneToSortedListOfNodeAndDistancePairs() {

		return this.zoneToSortedListOfNodeAndDistancePairs;
	}
	
	/**
	 * Getter for tempro zone to list of contained nodes mapping.
	 * @return Zone to list of contained nodes.
	 */
	public HashMap<String, List<Integer>> getZoneToListOfContaintedNodes() {
		
		return this.zoneToListOfContainedNodes;
	}
	
//	/**
//	 * Getter for tempro zone to node matrix of contained nodes.
//	 * @return Node matrix of contained nodes (with joint probabilities).
//	 */
//	public HashMap<String, NodeMatrix> getZoneToNodeMatrix() {
//		
//		return this.zoneToNodeMatrix;
//	}
//	
	/**
	 * Getter for LAD to list of contained tempro zones mapping.
	 * @return LAD to list of contained zones.
	 */
	public HashMap<String, List<String>> getLADToListOfContainedZones() {
		
		return this.LADToListOfContainedZones;
	}
	
	/**
	 * Getter for LAD code to LAD name mapping.
	 * @return LAD code to LAD name mapping.
	 */
	public HashMap<String, String> getLADToName() {
		
		return this.LADToName;
	}
	
	/**
	 * Getter for tempro zone to its centroid mapping.
	 * @return Tempro zone to centroid mapping.
	 */
	public HashMap<String, Point> getZoneToCentroid() {
		
		return this.zoneToCentroid;
	}
}
