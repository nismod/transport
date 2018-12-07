/**
 * 
 */
package nismod.transport.network.road;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.data.CachingFeatureSource;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.BasicDirectedLineGraphBuilder;
import org.geotools.graph.build.line.DirectedLineStringGraphGenerator;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.geotools.referencing.CRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

/**
 * Routable road network built from the shapefiles.
 * @author Milan Lovric
 *
 */
public class RoadNetwork {
	
	private final static Logger LOGGER = LogManager.getLogger(RoadNetwork.class);

	private DirectedGraph network;
	//BasicDirectedLineGraphBuilder graphBuilder;
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore networkShapefile;
	private ShapefileDataStore nodesShapefile;
	private ShapefileDataStore AADFshapefile;
	private ShapefileDataStore newNetworkShapefile;
	private DijkstraIterator.EdgeWeighter dijkstraWeighter;
	private DijkstraIterator.EdgeWeighter dijkstraTimeWeighter;
	private HashMap<Integer, String> nodeToZone;
	private HashMap<Integer, String> edgeToZone;
	private HashMap<String, List<Integer>> zoneToNodes;
	private HashMap<String, List<String>> zoneToAreaCodes;
	private HashMap<String, Integer> areaCodeToNearestNodeID;
	private HashMap<String, Double> areaCodeToNearestNodeDistance; //[m]
	private HashMap<String, Integer> areaCodeToPopulation;
	private HashMap<Integer, Integer> freightZoneToNearestNode;
	private HashMap<Integer, Double> freightZoneToNearestNodeDistance; //[m]
	private HashMap<Integer, String> freightZoneToLAD;
	private HashMap<String, Integer> workplaceZoneToNearestNodeID;
	private HashMap<String, Double> workplaceZoneToNearestNodeDistance; //[m]
	private HashMap<String, Integer> workplaceZoneToPopulation;
	private HashMap<String, List<String>> zoneToWorkplaceCodes;
	
	private int[] nodeToGravitatingPopulation;
	private int[] nodeToGravitatingWorkplacePopulation;
	private double[] nodeToAverageAccessEgressDistance; //[m]
	private double[] nodeToAverageAccessEgressDistanceFreight; //[m]

	private Node[] nodeIDtoNode; //for direct access
	private Edge[] edgeIDtoEdge; //for direct access
	private Integer[] edgeIDtoOtherDirectionEdgeID; //for direct access
	
	private Boolean[] isEdgeUrban; //true = urban, false = rural, null = unknown/ferry
	private EdgeType[] edgesType; //A-road, Motorway, Ferry
	
	private boolean[] startNodeBlacklist;
	private boolean[] endNodeBlacklist;
	
	private double[] freeFlowTravelTime;
	private double[] edgeLengths;
	private int[] numberOfLanes;
	
	public double freeFlowSpeedMRoad; //kph
	public double freeFlowSpeedARoad; //kph
	public double averageSpeedFerry; //kph
	public int numberOfLanesMRoad; //for one direction
	public int numberOfLanesARoad; //for one direction
	public int maximumEdgeID; //for the entire network (include roads developed in the future).
	public int maximumNodeID; //for the entire network
	private double accessEgressFactor;
	
	public static enum EdgeType {
		AROAD, MOTORWAY, FERRY
	}
		
	/**
	 * @param zonesUrl Url for the shapefile with zone polygons.
	 * @param networkUrl Url for the shapefile with road network.
	 * @param nodesUrl Url for the shapefile with nodes.
	 * @param AADFurl Url for the shapefile with AADF counts.
	 * @param areaCodeFileName Path to the file with census output areas.
	 * @param areaCodeNearestNodeFile Path to the file with nearest nodes to output area centroids.
	 * @param workplaceZoneFileName Path to the file with workplace zones.
	 * @param workplaceZoneNearestNodeFile Path to the file with nearest nodes to workplace zone centroids.
	 * @param freightZoneToLADfile Path to the file with freight zone to LAD mapping.
	 * @param freightZoneNearestNodeFile Path to the file with nearest nodes to freight zones that are points.
	 * @param params Properties with parameters from the config file.
	 * @throws IOException if any.
	 */
	public RoadNetwork(URL zonesUrl, URL networkUrl, URL nodesUrl, URL AADFurl, String areaCodeFileName, String areaCodeNearestNodeFile, String workplaceZoneFileName, String workplaceZoneNearestNodeFile, String freightZoneToLADfile, String freightZoneNearestNodeFile, Properties params) throws IOException {

		this.zonesShapefile = new ShapefileDataStore(zonesUrl);
		this.networkShapefile = new ShapefileDataStore(networkUrl);
		this.nodesShapefile = new ShapefileDataStore(nodesUrl);
		this.AADFshapefile = new ShapefileDataStore(AADFurl);

		//read the parameters
		this.freeFlowSpeedMRoad = Double.parseDouble(params.getProperty("FREE_FLOW_SPEED_M_ROAD"));
		this.freeFlowSpeedARoad = Double.parseDouble(params.getProperty("FREE_FLOW_SPEED_A_ROAD"));
		this.averageSpeedFerry = Double.parseDouble(params.getProperty("AVERAGE_SPEED_FERRY"));
		this.numberOfLanesMRoad = Integer.parseInt(params.getProperty("NUMBER_OF_LANES_M_ROAD"));
		this.numberOfLanesARoad = Integer.parseInt(params.getProperty("NUMBER_OF_LANES_A_ROAD"));
		this.maximumEdgeID = Integer.parseInt(params.getProperty("MAXIMUM_EDGE_ID"));
		this.maximumNodeID = Integer.parseInt(params.getProperty("MAXIMUM_NODE_ID"));
		this.accessEgressFactor = Double.parseDouble(params.getProperty("ACCESS_EGRESS_LAD_DISTANCE_SCALING_FACTOR"));
		
		//build the graph
		this.build();

		//weight the edges of the graph using the physical distance of each road segment		
		this.dijkstraWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {
				SimpleFeature sf = (SimpleFeature) edge.getObject(); 
				double length = (double) sf.getAttribute("LenNet");
				return length;
			}
		};
		
		//weight the edges of the graph using the free-flow travel time		
		this.dijkstraTimeWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {
				SimpleFeature feature = (SimpleFeature) edge.getObject(); 
				double length = (double) feature.getAttribute("LenNet");
				double cost;
				String roadNumber = (String) feature.getAttribute("RoadNumber");
					if (roadNumber.charAt(0) == 'M') //motorway
						cost = length / freeFlowSpeedMRoad * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'A') //A road
						cost = length / freeFlowSpeedARoad * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'F')//ferry
						cost = length / averageSpeedFerry * 60;  //travel time in minutes
					else {
						LOGGER.warn("Unknown road type for link {}", edge.getID());
						cost = Double.NaN;
					}
				return cost;
			}
		};
		

		this.calculateFreeFlowTravelTime(); //requries edge lenghts and edge types
		
		this.loadAreaCodePopulationData(areaCodeFileName);
		this.loadAreaCodeNearestNodeAndDistance(areaCodeNearestNodeFile);
		this.loadWorkplaceZonePopulationData(workplaceZoneFileName);
		this.loadWorkplaceZoneNearestNodeAndDistance(workplaceZoneNearestNodeFile);
		
		this.calculateNodeGravitatingPopulation();
		this.calculateNodeAccessEgressDistance();
		this.sortGravityNodes();
		
		this.calculateNodeGravitatingWorkplacePopulation();
		this.calculateNodeAccessEgressDistanceFreight();
		//this.sortGravityNodesFreight(); //call this before freight assignment
		
		this.loadFreightZoneNearestNodeAndDistance(freightZoneNearestNodeFile);
		this.loadFreightZoneToLAD(freightZoneToLADfile);
		//System.out.println(this.zoneToAreaCodes);
		//System.out.println(this.areaCodeToPopulation);
		//System.out.println(this.areaCodeToNearestNode);
	}
	
	private void calculateFreeFlowTravelTime() {
		//calculate free-flow travel time
		this.freeFlowTravelTime = new double[this.maximumEdgeID];
		for (Object edge: this.network.getEdges()) {
			int edgeID = ((Edge)edge).getID();
			double length = this.getEdgeLength(edgeID);
			double time;
			EdgeType edgeType = this.edgesType[edgeID];
			if (edgeType == EdgeType.MOTORWAY) //motorway
				time = length / freeFlowSpeedMRoad * 60;  //travel time in minutes
			else if (edgeType == EdgeType.AROAD) //A road
				time = length / freeFlowSpeedARoad * 60;  //travel time in minutes
			else if (edgeType == EdgeType.FERRY)//ferry
				time = length / averageSpeedFerry * 60;  //travel time in minutes
			else {
				LOGGER.warn("Unknown road type for link {}", edgeID);
				time = Double.NaN;
			}
			this.freeFlowTravelTime[edgeID] = time;
		}
	}

	/**
	 * Replaces edge IDs in the road network object with fixed edge IDs provided in a shapefile.
	 * @param networkShapeFile Path to the shapefile with the network with edge IDs.
	 * @throws IOException if any.
	 */
	public void replaceNetworkEdgeIDs(URL networkShapeFile) throws IOException {
		
		LOGGER.info("Replacing network edges IDs with persistent ones...");
		
		ShapefileDataStore networkShapefile = new ShapefileDataStore(networkShapeFile);
		CachingFeatureSource cache2 = new CachingFeatureSource(networkShapefile.getFeatureSource());
		SimpleFeatureCollection networkFeatureCollection = cache2.getFeatures();
		SimpleFeatureIterator iter = networkFeatureCollection.features();
		try {
			while(iter.hasNext()) {
				SimpleFeature feature = iter.next();
				int edgeID = (int) feature.getAttribute("EdgeID");
				int Anode = (int) feature.getAttribute("Anode");
				int Bnode = (int) feature.getAttribute("Bnode");
				int CP = (int) feature.getAttribute("CP");
				
				DirectedNode nodeA = (DirectedNode) this.nodeIDtoNode[Anode];
				DirectedNode nodeB = (DirectedNode) this.nodeIDtoNode[Bnode];
				
				List edges = nodeA.getOutEdges(nodeB);
				for (Object o: edges) {
					DirectedEdge e = (DirectedEdge) o;
					SimpleFeature sf = (SimpleFeature) e.getObject();
					long CP2 = 0;
					Object countPoint = sf.getAttribute("CP");
					if (Long.class.isInstance(countPoint)) CP2 = (long) countPoint;
					if (Double.class.isInstance(countPoint)) CP2 = (long) Math.round((double) countPoint);
					if (CP == CP2) { //if there is a match, override edgeID
						e.setID(edgeID);
					}
				}
			}
			//force recreation of direct access edge maps
			this.createDirectAccessEdgeMap();
			this.createEdgeToOtherDirectionEdgeMap();
			//update information on edge type
			this.determineEdgesType();
			//updates number of lanes with new edge ids
			this.addNumberOfLanes();
			//store edge lengths to the instance map
			this.storeEdgesLengths();
			//re-calculate free flow travel time (requires edge lengths and edge types!)
			this.calculateFreeFlowTravelTime();
			//update information on urban/rural
			this.determineEdgesUrbanRural();
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
		
		//edges must be remapped
		CachingFeatureSource cache = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		SimpleFeatureCollection zonesFeatureCollection = cache.getFeatures();
		this.mapEdgesToZones(zonesFeatureCollection);
	}
	
	/**
	 * Overrides actual edge lengths with straight line distances, when they are smaller than straight line distances.
	 */
	public void makeEdgesAdmissible() {
	
		LOGGER.info("Checking the admissibility of edge lenghts by comparing them to a straight line distance.");
		
		int counter = 0;
		for (Object o: this.network.getEdges()) {
			DirectedEdge edge = (DirectedEdge) o;
			DirectedNode fromNode = edge.getInNode();
			DirectedNode toNode = edge.getOutNode();
			
			SimpleFeature feature = (SimpleFeature)edge.getObject();
			double length = (double) feature.getAttribute("LenNet");  //use actual physical length
			
			//calculate straight line distance between nodes
			SimpleFeature sf1 = (SimpleFeature)fromNode.getObject();
			Point point = (Point)sf1.getDefaultGeometry();
			SimpleFeature sf2 = (SimpleFeature)toNode.getObject();
			Point point2 = (Point)sf2.getDefaultGeometry();
			double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!
			if (length < distance) {
				LOGGER.printf(Level.TRACE, "The length of the edge (%.2f) is smaller than the straight line distance (%.2f)!", length, distance);
				LOGGER.trace("I am overriding actual distance for edge ({})-{}->({})", fromNode.getID(), edge.getID(), toNode.getID());
				feature.setAttribute("LenNet", distance); //write into sf object
				//this.edgeLengths.put(edge.getID(), distance); //write into instance map
				this.edgeLengths[edge.getID()] = distance; //write into instance map
				counter++;
			}
		}
		if (counter > 0)
			LOGGER.debug("{} edge lengths were corrected with a calculated straight line distance.", counter);
	}
		
	/**
	* Exports a directed multigraph representation of the network as a shapefile.
	* @param fileName The name of the output shapefile.
	* @throws IOException if any.
	*/
	public void exportToShapefile(String fileName) throws IOException {

		if (network == null) {
			LOGGER.warn("Cannot export empty network to a shapefile!");
			return;
		}

		//get an output file name and create the new shapefile
		//File newFile = getNewShapeFile(fileName);
		File newFile = new File(fileName);
		
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		this.newNetworkShapefile = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

		//dynamically creates a feature type to describe the shapefile contents
		SimpleFeatureType type = createFeatureType();
		
		//List<SimpleFeatureType> features = new ArrayList<>();
		List features = new ArrayList<>();

		//GeometryFactory will be used to create the geometry attribute of each feature,
		//using a LineString object for the road link
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

		Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) network.getEdges().iterator();
		while (iter.hasNext()) {

			featureBuilder.reset();

			//create LineString geometry
			DirectedEdge edge = (DirectedEdge) iter.next();
			SimpleFeature featA = (SimpleFeature) edge.getNodeA().getObject();
			SimpleFeature featB = (SimpleFeature) edge.getNodeB().getObject();
			Point pointA = (Point) featA.getDefaultGeometry();
			Point pointB = (Point) featB.getDefaultGeometry();
			Coordinate coordA = pointA.getCoordinate(); 
			Coordinate coordB = pointB.getCoordinate();
			Coordinate[] coordinates = {coordA, coordB};
			LineString roadLink = geometryFactory.createLineString(coordinates);

			//build feature
			List<Object> objList = new ArrayList();
			objList.add(roadLink);
			objList.add(edge.getID());
			objList.add(edge.getNodeA().getID());
			objList.add(edge.getNodeB().getID());
			SimpleFeature feat = (SimpleFeature) edge.getObject();
			if (feat != null) { //has an object (e.g. count point)
				objList.add(feat.getAttribute("CP"));
				objList.add(feat.getAttribute("RoadNumber"));
				objList.add(feat.getAttribute("iDir"));
				objList.add(feat.getAttribute("S Ref E"));
				objList.add(feat.getAttribute("S Ref N"));
				objList.add(feat.getAttribute("LenNet"));
				String roadNumber = (String) feat.getAttribute("RoadNumber");
				if (roadNumber.charAt(0) == 'M') {//motorway
					objList.add(freeFlowSpeedMRoad);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / freeFlowSpeedMRoad * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(false); //not a ferry
				} else if (roadNumber.charAt(0) == 'A') {//A road
					objList.add(freeFlowSpeedARoad);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / freeFlowSpeedARoad * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(false); //not a ferry
				} else if (roadNumber.charAt(0) == 'F') {//ferry
					objList.add(averageSpeedFerry);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / averageSpeedFerry * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(true); //ferry
				} else {
					LOGGER.warn("Unknown road category for edge {}", edge.getID());
					objList.add(null);
					objList.add(null);
					objList.add(null);
				}
			} else {
				LOGGER.warn("No object assigned to the edge {}", edge.getID());
			}
			SimpleFeature feature = featureBuilder.build(type, objList, Integer.toString(edge.getID()));
			//System.out.println(feature.toString());
			features.add(feature);
		}

		this.newNetworkShapefile.createSchema(type);

		//Write the features to the shapefile using a Transaction
		Transaction transaction = new DefaultTransaction("create");

		String typeName = this.newNetworkShapefile.getTypeNames()[0];
		SimpleFeatureSource featureSource = this.newNetworkShapefile.getFeatureSource(typeName);
		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		/*
		 * The Shapefile format has a couple limitations:
		 * - "the_geom" is always first, and used for the geometry attribute name
		 * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
		 * - Attribute names are limited in length 
		 * - Not all data types are supported (example Timestamp represented as Date)
		 * 
		 * Each data store has different limitations so check the resulting SimpleFeatureType.
		 */
		//LOGGER.debug("SHAPE: {}", SHAPE_TYPE);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so the ListFeatureCollection
			 * class is used to wrap the list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
			} catch (Exception e) {
				LOGGER.error(e);
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			LOGGER.warn("{} does not support read/write access", typeName);
		}
	}
	
	/**
	 * Creates a custom feature collection for the network.
	 * @param linkData Data assigned to network links.
	 * @param linkDataLabel The label of the link data.
	 * @param shapefilePath The path to the shapefile into which data will be stored.
	 * @return Feature collection.
	 * @throws IOException if any.
	 */
	public SimpleFeatureCollection createNetworkFeatureCollection(Map<Integer, Double> linkData, String linkDataLabel, String shapefilePath) throws IOException {

		if (network == null) {
			LOGGER.warn("Cannot create network feature collection as the network is empty!");
			return null;
		}
		
		File newFile = null;
		if (shapefilePath == null)
			//get an output file name and create the new shapefile
			newFile = getNewShapeFile("networkWithLinkData");
		else 
			newFile = new File(shapefilePath);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		this.newNetworkShapefile = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

		//dynamically creates a feature type to describe the shapefile contents
		SimpleFeatureType type = createCustomFeatureType(linkDataLabel);
		
		//List<SimpleFeatureType> features = new ArrayList<>();
		//List features = new ArrayList<>();
		List<SimpleFeature> features = new ArrayList<>();

		//GeometryFactory will be used to create the geometry attribute of each feature,
		//using a LineString object for the road link
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

		Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) network.getEdges().iterator();
		while (iter.hasNext()) {

			featureBuilder.reset();

			//create LineString geometry
			DirectedEdge edge = (DirectedEdge) iter.next();
			SimpleFeature featA = (SimpleFeature) edge.getNodeA().getObject();
			SimpleFeature featB = (SimpleFeature) edge.getNodeB().getObject();
			Point pointA = (Point) featA.getDefaultGeometry();
			Point pointB = (Point) featB.getDefaultGeometry();
			Coordinate coordA = pointA.getCoordinate(); 
			Coordinate coordB = pointB.getCoordinate();
			Coordinate[] coordinates = {coordA, coordB};
			LineString roadLink = geometryFactory.createLineString(coordinates);

			//build feature
			List<Object> objList = new ArrayList();
			objList.add(roadLink);
			objList.add(edge.getID());
			objList.add(edge.getNodeA().getID());
			objList.add(edge.getNodeB().getID());
			SimpleFeature feat = (SimpleFeature) edge.getObject();
			if (feat != null) { //has an object (e.g. count point)
				objList.add(feat.getAttribute("CP"));
				objList.add(feat.getAttribute("RoadNumber"));
				objList.add(feat.getAttribute("iDir"));
				objList.add(feat.getAttribute("S Ref E"));
				objList.add(feat.getAttribute("S Ref N"));
				objList.add(feat.getAttribute("LenNet"));
				String roadNumber = (String) feat.getAttribute("RoadNumber");
				if (roadNumber.charAt(0) == 'M') {//motorway
					objList.add(freeFlowSpeedMRoad);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / freeFlowSpeedMRoad * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(false); //not a ferry
				} else if (roadNumber.charAt(0) == 'A') {//A road
					objList.add(freeFlowSpeedARoad);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / freeFlowSpeedARoad * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(false); //not a ferry
				} else if (roadNumber.charAt(0) == 'F') {//ferry
					objList.add(averageSpeedFerry);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / averageSpeedFerry * 60; //in minutes
					objList.add(freeFlowTime);
					objList.add(true); //ferry
				} else {
					LOGGER.warn("Unknown road category for edge {}", edge.getID());
					objList.add(null);
					objList.add(null);
					objList.add(null);
				}
				int lanes = this.numberOfLanes[edge.getID()];
				objList.add(lanes);
				Double volume = linkData.get(edge.getID());
				if (volume == null) volume = 0.0;
				objList.add(volume);
			} else {
				LOGGER.warn("No object assigned to the edge {}", edge.getID());
			}
			//SimpleFeature feature = featureBuilder.build(type, objList, Integer.toString(edge.getID()));
			SimpleFeature feature = SimpleFeatureBuilder.build(type,  objList, Integer.toString(edge.getID()));
			//System.out.println(feature.toString());
			features.add(feature);
		}

//		SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
//		
//		return collection;
		
		
		this.newNetworkShapefile.createSchema(type);

		//Write the features to the shapefile using a Transaction
		Transaction transaction = new DefaultTransaction("create");

		String typeName = this.newNetworkShapefile.getTypeNames()[0];
		SimpleFeatureSource featureSource = this.newNetworkShapefile.getFeatureSource(typeName);
		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		/*
		 * The Shapefile format has a couple limitations:
		 * - "the_geom" is always first, and used for the geometry attribute name
		 * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
		 * - Attribute names are limited in length 
		 * - Not all data types are supported (example Timestamp represented as Date)
		 * 
		 * Each data store has different limitations so check the resulting SimpleFeatureType.
		 */
		//System.out.println("SHAPE:"+SHAPE_TYPE);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			/*
			 * SimpleFeatureStore has a method to add features from a
			 * SimpleFeatureCollection object, so the ListFeatureCollection
			 * class is used to wrap the list of features.
			 */
			SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();
			} catch (Exception e) {
				LOGGER.error(e);
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			LOGGER.warn("{} does not support read/write access.", typeName);
		}
		
		return featureSource.getFeatures();
		
	}
	
//	/**
//	 * Creates a custom feature collection for the zones.
//	*/
//	public SimpleFeatureCollection createZonalFeatureCollection(Map<String, Double> values) throws IOException {
//
//		//get an output file name and create the new shapefile
//		File newFile = getNewShapeFile("zonesWithValues");
//
//		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
//		Map<String, Serializable> params = new HashMap<>();
//		params.put("url", newFile.toURI().toURL());
//		params.put("create spatial index", Boolean.TRUE);
//		
//		//this.newNetworkShapefile = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//		ShapefileDataStore newZonesShapefile = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//		
//		//dynamically creates a feature type to describe the shapefile contents
//		//SimpleFeatureType type = createCustomFeatureType();
//		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
//		CoordinateReferenceSystem crs;
//		try {
//			//British National Grid
//			crs = CRS.decode("EPSG:27700", false);
//			//builder.setCRS(DefaultGeographicCRS.WGS84);
//			builder.setCRS(crs);
//		} catch (NoSuchAuthorityCodeException e) {
//			// TODO Auto-generated catch block
//			LOGGER.error(e);
//		} catch (FactoryException e) {
//			// TODO Auto-generated catch block
//			LOGGER.error(e);
//		}
//		builder.setName("Location");
//		//add attributes in order
//		builder.add("the_geom", Polygon.class);
//		builder.add("zone", String.class);
//		builder.add("vehicleKm", Double.class);
//
//		//build the type
//		final SimpleFeatureType type = builder.buildFeatureType();
//		
//		List<SimpleFeature> features = new ArrayList<>();
//		//GeometryFactory will be used to create the geometry attribute of each feature,
//		//using a LineString object for the road link
//		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
//		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
//
//		Iterator<DirectedEdge> iter = (Iterator<DirectedEdge>) network.getEdges().iterator();
//	
//		
//		geometryFactory.createPolygon(coordinates)
//		
//		while (iter.hasNext()) {
//
//			featureBuilder.reset();
//
//			//create LineString geometry
//			DirectedEdge edge = (DirectedEdge) iter.next();
//			SimpleFeature featA = (SimpleFeature) edge.getNodeA().getObject();
//			SimpleFeature featB = (SimpleFeature) edge.getNodeB().getObject();
//			Point pointA = (Point) featA.getDefaultGeometry();
//			Point pointB = (Point) featB.getDefaultGeometry();
//			Coordinate coordA = pointA.getCoordinate(); 
//			Coordinate coordB = pointB.getCoordinate();
//			Coordinate[] coordinates = {coordA, coordB};
//			LineString roadLink = geometryFactory.createLineString(coordinates);
//
//			//build feature
//			List<Object> objList = new ArrayList();
//			objList.add(roadLink);
//			objList.add(edge.getID());
//			objList.add(edge.getNodeA().getID());
//			objList.add(edge.getNodeB().getID());
//			SimpleFeature feat = (SimpleFeature) edge.getObject();
//			if (feat != null) { //has an object (e.g. count point)
//				objList.add(feat.getAttribute("CP"));
//				objList.add(feat.getAttribute("RoadNumber"));
//				objList.add(feat.getAttribute("iDir"));
//				objList.add(feat.getAttribute("S Ref E"));
//				objList.add(feat.getAttribute("S Ref N"));
//				objList.add(feat.getAttribute("LenNet"));
//				String roadNumber = (String) feat.getAttribute("RoadNumber");
//				if (roadNumber.charAt(0) == 'M') {//motorway
//					objList.add(RoadNetworkAssignment.FREE_FLOW_SPEED_M_ROAD);
//					Double freeFlowTime = (double)feat.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_M_ROAD * 60; //in minutes
//					objList.add(freeFlowTime);
//					objList.add(false); //not a ferry
//				} else if (roadNumber.charAt(0) == 'A') {//A road
//					objList.add(RoadNetworkAssignment.FREE_FLOW_SPEED_A_ROAD);
//					Double freeFlowTime = (double)feat.getAttribute("LenNet") / RoadNetworkAssignment.FREE_FLOW_SPEED_A_ROAD * 60; //in minutes
//					objList.add(freeFlowTime);
//					objList.add(false); //not a ferry
//				} else if (roadNumber.charAt(0) == 'F') {//ferry
//					objList.add(RoadNetworkAssignment.AVERAGE_SPEED_FERRY);
//					Double freeFlowTime = (double)feat.getAttribute("LenNet") / RoadNetworkAssignment.AVERAGE_SPEED_FERRY * 60; //in minutes
//					objList.add(freeFlowTime);
//					objList.add(true); //ferry
//				} else {
//					System.err.println("Unknown road category for edge " + edge.getID());
//					objList.add(null);
//					objList.add(null);
//					objList.add(null);
//				}
//				Double volume = dailyVolume.get(edge.getID());
//				if (volume == null) volume = 0.0;
//				objList.add(volume);
//			} else {
//				System.err.println("No object assigned to the edge " + edge.getID());
//			}
//			//SimpleFeature feature = featureBuilder.build(type, objList, Integer.toString(edge.getID()));
//			SimpleFeature feature = SimpleFeatureBuilder.build(type,  objList, Integer.toString(edge.getID()));
//			//System.out.println(feature.toString());
//			features.add(feature);
//		}
//
////		SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
////		
////		return collection;
//		
//		
//		this.newNetworkShapefile.createSchema(type);
//
//		//Write the features to the shapefile using a Transaction
//		Transaction transaction = new DefaultTransaction("create");
//
//		String typeName = this.newNetworkShapefile.getTypeNames()[0];
//		SimpleFeatureSource featureSource = this.newNetworkShapefile.getFeatureSource(typeName);
//		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
//		/*
//		 * The Shapefile format has a couple limitations:
//		 * - "the_geom" is always first, and used for the geometry attribute name
//		 * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
//		 * - Attribute names are limited in length 
//		 * - Not all data types are supported (example Timestamp represented as Date)
//		 * 
//		 * Each data store has different limitations so check the resulting SimpleFeatureType.
//		 */
//		System.out.println("SHAPE:"+SHAPE_TYPE);
//
//		if (featureSource instanceof SimpleFeatureStore) {
//			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
//			/*
//			 * SimpleFeatureStore has a method to add features from a
//			 * SimpleFeatureCollection object, so the ListFeatureCollection
//			 * class is used to wrap the list of features.
//			 */
//			SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
//			featureStore.setTransaction(transaction);
//			try {
//				featureStore.addFeatures(collection);
//				transaction.commit();
//			} catch (Exception problem) {
//				problem.printStackTrace();
//				transaction.rollback();
//			} finally {
//				transaction.close();
//			}
//		} else {
//			System.err.println(typeName + " does not support read/write access");
//		}
//		
//		return featureSource.getFeatures();
//		
//	}
	
	
	/**
	 * Creates a new (unidirectional) road link (edge) between existing intersections (nodes).
	 * @param fromNode Start node of the new road link.
	 * @param toNode End node of the new road link.
	 * @param numberOfLanes Number of lanes in the road link.
	 * @param roadCategory Road category.
	 * @param length Length of the road link.
	 * @return Newly created edge.
	 */
	public Edge createNewRoadLink(Node fromNode, Node toNode, int numberOfLanes, char roadCategory, double length) {
		
		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();
		graphBuilder.importGraph(this.network);
		
		DirectedEdge directedEdge = (DirectedEdge) graphBuilder.buildEdge(fromNode, toNode);
		graphBuilder.addEdge(directedEdge);
		
		this.network = (DirectedGraph) graphBuilder.getGraph();
		
		//add edge to list
		this.edgeIDtoEdge[directedEdge.getID()] = directedEdge;
		this.numberOfLanes[directedEdge.getID()] = numberOfLanes;
		
		//calculate straight line distance between nodes
		SimpleFeature sf1 = (SimpleFeature)fromNode.getObject();
		Point point = (Point)sf1.getDefaultGeometry();
		SimpleFeature sf2 = (SimpleFeature)toNode.getObject();
		Point point2 = (Point)sf2.getDefaultGeometry();
		double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!
		if (length < distance) {
			LOGGER.printf(Level.WARN, "The length of the newly created link (%.2f) is smaller than the straight line distance (%.2f)!", length, distance);
		}
		
		//this.edgeLengths.put(directedEdge.getID(), length);
		this.edgeLengths[directedEdge.getID()] = length;
		
		if (roadCategory == 'A')
			this.edgesType[directedEdge.getID()] = EdgeType.AROAD;
		else if (roadCategory == 'M')
			this.edgesType[directedEdge.getID()] = EdgeType.MOTORWAY;
		else 
			LOGGER.warn("Newly created road link does not have correct road category.");
		
		//TODO urban/rural
		
		//create an object to add to edge
		//dynamically creates a feature type to describe the shapefile contents
		SimpleFeatureType type = createNewRoadLinkFeatureType();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
		//featureBuilder.reset();
		//build feature
		List<Object> objList = new ArrayList();
		objList.add(0); //null
		objList.add(roadCategory);
		objList.add(length);		
		SimpleFeature feature = featureBuilder.build(type, objList, Integer.toString(directedEdge.getID()));
		directedEdge.setObject(feature);
						
		//update blacklists of nodes as some nodes might now become accessible
//		for (int nodeId: startNodeBlacklist) {
//			DirectedNode node = (DirectedNode) this.nodeIDtoNode.get(nodeId);
//			if (node.getOutDegree() > 0) this.startNodeBlacklist.remove(node.getID());
//		}
//		for (int nodeId: endNodeBlacklist) {
//			DirectedNode node = (DirectedNode) this.nodeIDtoNode.get(nodeId);
//			if (node.getInDegree() > 0) this.endNodeBlacklist.remove(node.getID());
//		}
		this.createNodeBlacklists(); //just create them from scratch
		
		this.createEdgeToOtherDirectionEdgeMap(); //force re-creation of edge to other edge mapping
		
		this.calculateFreeFlowTravelTime(); //could be just for new edge
		
		//TODO determine to which zone this road link belong and set mapping here:
		//this.edgeToZone.put(directedEdge.getID(), zone)
		//TODO or alternatively, make sure the edge object contains a point geometry and then call this method:
		//this.mapEdgesToZones(zonesFeatureCollection);
		
		//check if one of the nodes of the edge are not mapped to any zone as this indicates potential problems
		//(e.g. when a node is on the bridge, so it is not contained within any zonal polygon).
		String zone1 = this.nodeToZone.get(directedEdge.getNodeA().getID());
		String zone2 = this.nodeToZone.get(directedEdge.getNodeB().getID());
		//if one node falls outside any zonal polygon, then assign edge to the zone of the other node
		if (zone1 == null && zone2 != null)
			this.edgeToZone.put(directedEdge.getID(), zone2);
		else if (zone2 == null && zone1 != null)
			this.edgeToZone.put(directedEdge.getID(), zone1);
		else if (zone1 != null && zone2 != null && zone1.equals(zone2))
			this.edgeToZone.put(directedEdge.getID(), zone1);
		else if (zone1 != null && zone2 != null && !zone1.equals(zone2)) //use the zone of nodeA
			this.edgeToZone.put(directedEdge.getID(), zone1);
		else
			LOGGER.warn("It was not possible to map new edge {} to any zone!", directedEdge.getID());
			
		return directedEdge;
	}
	
	
	/**
	 * This adds edge (including its object) to the network - useful for restoring from a list of removed edges (e.g. during disruption).
	 * @param edge Edge to be added to the network.
	 */
	public void addRoadLink(Edge edge) {
		
		int edgeID = edge.getID();
		
		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();
		graphBuilder.importGraph(this.network);
		
		graphBuilder.addEdge(edge);
		edge.setID(edgeID); //override with exact ID
		
		this.network = (DirectedGraph) graphBuilder.getGraph();
		
		//update
		this.edgeIDtoEdge[edgeID] = edge;
		this.createEdgeToOtherDirectionEdgeMap();
		
		//put edge length into map
		SimpleFeature feature = (SimpleFeature)edge.getObject();
		double length = (double) feature.getAttribute("LenNet");
		//this.edgeLengths.put(edgeID, length);
		this.edgeLengths[edgeID] = length;
		
		//this.addNumberOfLanes(); //this would overwrite edges with unusual number of lanes!
		//TODO
		//calculate number of lanes -> this should be stored in edge object
		
		//TODO
		//put in the edgetype array!
		
		this.calculateFreeFlowTravelTime(); //could be just for new edge
				
		//update node blacklists
		this.createNodeBlacklists();
		
		//map edge to a zone
		String zone1 = this.nodeToZone.get(edge.getNodeA().getID());
		String zone2 = this.nodeToZone.get(edge.getNodeB().getID());
		//if one node falls outside any zonal polygon, then assign edge to the zone of the other node
		if (zone1 == null && zone2 != null)
			this.edgeToZone.put(edge.getID(), zone2);
		else if (zone2 == null && zone1 != null)
			this.edgeToZone.put(edge.getID(), zone1);
		//if nodes fall to the same zone
		else if (zone1 != null && zone2 != null && zone1.equals(zone2))
			this.edgeToZone.put(edge.getID(), zone1);
		//if nodes are in different zones, use the zone of nodeA
		else if (zone1 != null && zone2 != null && !zone1.equals(zone2)) 
			this.edgeToZone.put(edge.getID(), zone1);
		else
			LOGGER.warn("It was not possible to map new edge {} to any zone!", edge.getID());
	}
	
	/**
	 * Removes an edge from the road network.
	 * @param edge Edge to remove from the road network.
	 */
	public void removeRoadLink(Edge edge) {
		
		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();
		graphBuilder.importGraph(this.network);
		
		graphBuilder.removeEdge(edge);
		
		this.network = (DirectedGraph) graphBuilder.getGraph();
		
		//update
		this.edgeIDtoEdge[edge.getID()] = null;
		//this.numberOfLanes.remove(edge.getID()); //TODO leave it for now, until edge object contains this information
		
		//this.edgeLengths.remove(edge.getID());
		this.edgeLengths[edge.getID()] = 0.0; //put zero length
		
		//update node blacklists
		this.createNodeBlacklists();
		
		//remove from edge to zone mapping
		this.edgeToZone.remove(edge.getID());
	}
	
	/**
	 * Getter method for the road network.
	 * @return Directed graph representation of the road network.
	 */
	public DirectedGraph getNetwork() {

		return network;
	}

	/**
	 * Getter method for the Dijkstra edge weighter.
	 * @return Dijkstra edge weighter.
	 */
	public DijkstraIterator.EdgeWeighter getDijkstraWeighter() {

		return dijkstraWeighter;
	}
	
	/**
	 * Getter method for the Dijkstra edge weighter with time.
	 * @param linkTravelTime Link travel times to use for edge weighting.
	 * @return Dijkstra edge weighter with time.
	 */
	public DijkstraIterator.EdgeWeighter getDijkstraTimeWeighter(double[] linkTravelTime) {

		//weight the edges of the graph using the free-flow travel time		
		DijkstraIterator.EdgeWeighter dijkstraTimeWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {

				if (edge == null) {
					//no edge provided (set maximum weight)
					return Double.MAX_VALUE;
					//return Double.POSITIVE_INFINITY;
				}
				double cost;
				if (linkTravelTime == null || linkTravelTime[edge.getID()] == 0.0) { //use default link travel time
					SimpleFeature feature = (SimpleFeature)edge.getObject();
					String roadNumber = (String) feature.getAttribute("RoadNumber");
					if (roadNumber.charAt(0) == 'M') //motorway
						cost = (double) feature.getAttribute("LenNet") / freeFlowSpeedMRoad * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'A') //A road
						cost = (double) feature.getAttribute("LenNet") / freeFlowSpeedARoad * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'F')//ferry
						cost = (double) feature.getAttribute("LenNet") / averageSpeedFerry * 60;  //travel time in minutes
					else {
						LOGGER.warn("Unknown road type for link {}", edge.getID());
						cost = Double.NaN;
					}
				} else //use provided travel time
					cost = linkTravelTime[edge.getID()]; 
				return cost;
			}
		};

		return dijkstraTimeWeighter;
	}

	/** Getter method for the AStar functions (edge cost and heuristic function) based on distance.
	 * @param destinationNode Destination node.
	 * @return AStar functions.
	 */
	public AStarIterator.AStarFunctions getAstarFunctions(Node destinationNode) {

		AStarIterator.AStarFunctions aStarFunctions = new  AStarIterator.AStarFunctions(destinationNode){

			@Override
			public double cost(AStarIterator.AStarNode aStarNode1, AStarIterator.AStarNode aStarNode2) {

				//Edge edge = aStarNode1.getNode().getEdge(aStarNode2.getNode()); // does not work, a directed version must be used!
				Edge edge = ((DirectedNode) aStarNode1.getNode()).getOutEdge((DirectedNode) aStarNode2.getNode());
				if (edge == null) {
					//no edge found in that direction (set maximum weight)
					//return Double.MAX_VALUE;
					return Double.POSITIVE_INFINITY;
				}
				SimpleFeature feature = (SimpleFeature)edge.getObject();
				double cost = (double) feature.getAttribute("LenNet");  //use actual physical length
				return cost;
			}

			@Override
			public double h(Node node) {

				//estimates the cheapest cost from the node 'node' to the node 'destinationNode'
				SimpleFeature feature = (SimpleFeature)node.getObject();
				Point point = (Point)feature.getDefaultGeometry();
				SimpleFeature feature2 = (SimpleFeature)destinationNode.getObject();
				Point point2 = (Point)feature2.getDefaultGeometry();
				double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!
				return distance;
			}
		};

		return aStarFunctions;
	}
	
	/** Getter method for the AStar functions (edge cost and heuristic function) based on travel time.
	 * @param destinationNode Destination node.
	 * @param linkTravelTime Link travel times to use for edge weighting.
	 * @return AStar functions.
	 */
	public AStarIterator.AStarFunctions getAstarFunctionsTime(Node destinationNode, double[] linkTravelTime) {

			AStarIterator.AStarFunctions aStarFunctions = new  AStarIterator.AStarFunctions(destinationNode){
			
			@Override
			public double cost(AStarIterator.AStarNode aStarNode1, AStarIterator.AStarNode aStarNode2) {

				//Edge edge = aStarNode1.getNode().getEdge(aStarNode2.getNode()); // does not work, a directed version must be used!
				Edge edge = ((DirectedNode) aStarNode1.getNode()).getOutEdge((DirectedNode) aStarNode2.getNode());
				//System.out.printf("Asking for cost from node %d to node %d \n", aStarNode1.getNode().getID(), aStarNode2.getNode().getID());
				if (edge == null) {
					//no edge found in that direction (set maximum weight)
					//System.out.println("Edge in that direction not found so setting max cost");
					//return Double.MAX_VALUE;
					return Double.POSITIVE_INFINITY;
				}
				double cost;
				if (linkTravelTime[edge.getID()] == 0.0) { //use default link travel time
					cost = RoadNetwork.this.freeFlowTravelTime[edge.getID()];
				} else //use provided travel time
					cost = linkTravelTime[edge.getID()]; 
				
				//System.out.println("Cost = " + cost);
				return cost;
			}

			@Override
			public double h(Node node) {

				//estimates the cheapest cost from the node 'node' to the node 'destinationNode'
				SimpleFeature feature = (SimpleFeature)node.getObject();
				Point point = (Point)feature.getDefaultGeometry();
				SimpleFeature feature2 = (SimpleFeature)destinationNode.getObject();
				Point point2 = (Point)feature2.getDefaultGeometry();
				double distance = point.distance(point2) / 1000.0; //straight line distance (from metres to kilometres)!
				return (distance / freeFlowSpeedMRoad * 60); //travel time in minutes
			}
		};

		return aStarFunctions;
	}
	
	/**
	 * Gets the fastest path between two nodes using astar algorithm and provided link travel times.
	 * Links which have no travel time provided will use free flow travel times.
	 * @param from Origin node.
	 * @param to Destination node.
	 * @param linkTravelTime The map with link travel times.
	 * @return Fastest path.
	 */
	public RoadPath getFastestPath(DirectedNode from, DirectedNode to, double[] linkTravelTime) {

		if (linkTravelTime == null) linkTravelTime = new double[this.maximumEdgeID];
		//if (linkTravelTime == null) linkTravelTime = this.freeFlowTravelTime;
		RoadPath path;

		//if from and to nodes are the same, return a single node path
		if (from.getID() == to.getID()) {
			path = new RoadPath();
			path.add(from);
		} else
			//find the shortest path using AStar algorithm
			try {
				//System.out.printf("Finding the shortest path from %d to %d using astar: \n", from.getID(), to.getID());
				AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(this.network, from, to, this.getAstarFunctionsTime(to, linkTravelTime));
				aStarPathFinder.calculate();
				Path aStarPath;
				aStarPath = aStarPathFinder.getPath();
				if (aStarPath != null) {
					aStarPath.reverse();
					//System.out.println(aStarPath);
					//System.out.println("The path as a list of nodes: " + aStarPath);
					//List listOfEdges = aStarPath.getEdges();
					//System.out.println("The path as a list of edges: " + listOfEdges);
					//System.out.println("Path size in the number of nodes: " + aStarPath.size());
					//System.out.println("Path size in the number of edges: " + listOfEdges.size());
					path = new RoadPath(aStarPath);
					//System.out.println("RoadPath: " + path.toString());
					//System.out.println("RoadPath edges: " + path.getEdges());
					//path.buildEdges();
					//System.out.println("RoadPath edges: " + path.getEdges());
				} else {
					//System.err.println("Could not find the shortest path using astar.");
					return null;
				}
//			} catch (WrongPathException e) {
//				//LOGGER.warn("Could not find the shortest path using astar. {}", e.getMessage());
//				path = null;
			} catch (Exception e) {
				LOGGER.trace("Could not find the shortest path using astar. {}", e.getMessage());
				path = null;
			} 

		if (path != null && !path.isValid()) {
			LOGGER.debug("Fastest path from {} to {} exists, but is not valid!", from.getID(), to.getID());
			return null;
		}
		return path;
	}
	
	/**
	 * Gets the fastest path between two nodes using Dijkstra's algorithm and provided link travel times.
	 * Links which have no travel time provided will use free flow travel times.
	 * @param from Origin node.
	 * @param to Destination node.
	 * @param linkTravelTime The map with link travel times.
	 * @return Fastest path.
	 */
	public RoadPath getFastestPathDijkstra(DirectedNode from, DirectedNode to, double[] linkTravelTime) {

		if (linkTravelTime == null) linkTravelTime = new double[this.maximumEdgeID];
		//if (linkTravelTime == null) linkTravelTime = this.freeFlowTravelTime;
		RoadPath path;
		//find the shortest path using Dijkstra algorithm
		try {
			//System.out.printf("Finding the shortest path from %d to %d using dijkstra: \n", from.getID(), to.getID());
			DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(this.network, from, this.getDijkstraTimeWeighter(linkTravelTime));
			pathFinder.calculate();
			Path dijkstraPath = pathFinder.getPath(to);
			if (dijkstraPath != null) {
			dijkstraPath.reverse();
			//System.out.println(dijkstraPath);
			//System.out.println("The path as a list of nodes: " + dijkstraPath);
			//List listOfEdges = dijkstraPath.getEdges();
			//System.out.println("The path as a list of edges: " + listOfEdges);
			//System.out.println("Path size in the number of nodes: " + dijkstraPath.size());
			//System.out.println("Path size in the number of edges: " + listOfEdges.size());
			path = new RoadPath(dijkstraPath);
			//System.out.println("RoadPath: " + path.toString());
			//System.out.println("RoadPath edges: " + path.getEdges());
			//path.buildEdges();
			//System.out.println("RoadPath edges: " + path.getEdges());
			} else {
				//System.err.println("Could not find the shortest path using Dijkstra.");
				return null;
			}
		} catch (Exception e) {
			LOGGER.warn(e);
			LOGGER.warn("Could not find the shortest path using Dijkstra.");
			return null;
		}
		if (path != null && !path.isValid()) {
			LOGGER.warn("Fastest path from {} to {} exists, but is not valid!", from.getID(), to.getID());
			return null;
		}
		return path;
	}
	
	/**
	 * Get car traffic counts data for each link (for combined counts return 1/2 of the count per direction).
	 * @return AADF traffic counts per link.
	 */
	public Integer[] getAADFCarTrafficCounts () {

		Integer[] counts = new Integer[this.maximumEdgeID];

		Iterator iter = this.getNetwork().getEdges().iterator();
		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.edgesType[edgeID];
			SimpleFeature sf = (SimpleFeature) edge.getObject(); 

			if (edgeType == EdgeType.FERRY) continue; //ferry

			//Long countPoint = (long) sf.getAttribute("CP");
			String direction = (String) sf.getAttribute("iDir");
			char dir = direction.charAt(0);

			long carCount = (long) sf.getAttribute("FdCar");

			//directional counts
			if (dir == 'N' || dir == 'S' || dir == 'W' || dir == 'E')	counts[edgeID] = (int) carCount;
			if (dir == 'C')												counts[edgeID] = (int) Math.round(carCount/2.0);

		}

		return counts;
	}
	
	/**
	 * Getter method for the number of lanes for each link.
	 * @return Link id to number of lanes mapping.
	 */
	public int[] getNumberOfLanes() {

		return this.numberOfLanes;
	}
	
	/**
	 * Getter method for the array saying if the edge is urban (true), rural (false), or unkown (null).
	 * Array index is edge ID (without -1 shift).
	 * @return Array saying if the edge is urban/rural/unkown.
	 */
	public Boolean[] getIsEdgeUrban() {

		return this.isEdgeUrban;
	}
	
	/**
	 * Getter method for the array saying if the edge is A-road, motorway, or ferry.
	 * Array index is edge ID (without -1 shift).
	 * @return Map between the edge ID and whether the edge is ferry.
	 */
	public EdgeType[]  getEdgesType() {

		return this.edgesType;
	}
	
	/**
	 * Getter method for the node to zone mapping.
	 * @return Node to zone mapping.
	 */
	public HashMap<Integer, String> getNodeToZone() {

		return this.nodeToZone;
	}
	
	/**
	 * Getter method for the edge to zone mapping.
	 * @return Node to zone mapping.
	 */
	public HashMap<Integer, String> getEdgeToZone() {

		return this.edgeToZone;
	}
	
	/**
	 * Getter method for the zone to nodes mapping.
	 * @return Zone to nodes mapping.
	 */
	public HashMap<String, List<Integer>> getZoneToNodes() {

		return this.zoneToNodes;
	}
	
	/**
	 * Getter method for the zone to area codes mapping.
	 * @return Zone to area codes mapping.
	 */
	public HashMap<String, List<String>> getZoneToAreaCodes() {

		return this.zoneToAreaCodes;
	}
	
	/**
	 * Getter method for the area code to population mapping.
	 * @return Area code to population mapping.
	 */
	public HashMap<String, Integer> getAreaCodeToPopulation() {

		return this.areaCodeToPopulation;
	}
	
	/**
	 * Getter method for the area code to the nearest node mapping.
	 * @return Area code to the nearest node mapping.
	 */
	public HashMap<String, Integer> getAreaCodeToNearestNodeID() {

		return this.areaCodeToNearestNodeID;
	}
	
	/**
	 * Getter method for the freight zone to LAD mapping.
	 * @return Area code to the nearest node mapping.
	 */
	public HashMap<Integer, String> getFreightZoneToLAD() {

		return this.freightZoneToLAD;
	}
	
	/**
	 * Getter method for the freight zone to the nearest node mapping.
	 * @return Area code to the nearest node mapping.
	 */
	public HashMap<Integer, Integer> getFreightZoneToNearestNode() {

		return this.freightZoneToNearestNode;
	}

	/**
	 * Getter method for the LAD zone to workplace zones mapping.
	 * @return Zone to workplace code mapping.
	 */
	public HashMap<String, List<String>> getZoneToWorkplaceCodes() {

		return this.zoneToWorkplaceCodes;
	}
	
	/**
	 * Getter method for the workplace code to the nearest node mapping.
	 * @return Workplace code to the nearest node mapping.
	 */
	public HashMap<String, Integer> getWorkplaceZoneToNearestNode() {

		return this.workplaceZoneToNearestNodeID;
	}
	
	/**
	 * Getter method for the workplace zone to population mapping.
	 * @return Workplace zone to population mapping.
	 */
	public HashMap<String, Integer> getWorkplaceCodeToPopulation() {

		return this.workplaceZoneToPopulation;
	}
	
	/**
	 * Getter method for the node to gravitating population mapping.
	 * @return Node to gravitating population mapping.
	 */
	public int[] getNodeToGravitatingPopulation() {

		return this.nodeToGravitatingPopulation;
	}
	
	/**
	 * Population gravitating to a node.
	 * @param node Node to which the population gravitates.
	 * @return Gravitating population.
	 */
	public int getGravitatingPopulation(int node) {
		
		return this.nodeToGravitatingPopulation[node];
	}
	
	/**
	 * Workplace population gravitating to a node.
	 * @param node Node to which the workplace population gravitates.
	 * @return Gravitating workplace population.
	 */
	public int getGravitatingWorkplacePopulation(int node) {
		
		return this.nodeToGravitatingWorkplacePopulation[node];
	}
	
	/**
	 * Getter method for the node to average access/egress distance mapping [in metres].
	 * @return Node ID to average access/egress distance mapping.
	 */
	public double[] getNodeToAverageAccessEgressDistance() {

		return this.nodeToAverageAccessEgressDistance;
	}
	
	/**
	 * Getter method for the node to average access/egress distance mapping for freight [in metres].
	 * @return Node ID to average access/egress distance mapping for freight.
	 */
	public double[] getNodeToAverageAccessEgressDistanceFreight() {

		return this.nodeToAverageAccessEgressDistanceFreight;
	}
	
	/**
	 * Average access/egress distance to access a node that has gravitating population.
	 * @param node Node to which
	 * @return Gravitating population.
	 */
	public double getAverageAcessEgressDistance(int node) {
		
		return this.nodeToAverageAccessEgressDistance[node];
	}
	
	/**
	 * Average access/egress distance to access a node that has gravitating population.
	 * @param node Node to which
	 * @return Gravitating population.
	 */
	public double getAverageAcessEgressDistanceFreight(int node) {
		
		//TODO there could be some distance if a node has no gravitating population but is the nearest node to a point-based freight zone
		return this.nodeToAverageAccessEgressDistanceFreight[node];
	}
	
	/**
	 * Getter method for nodeID to node mapping.
	 * @return Node ID to node mapping.
	 */
	public Node[] getNodeIDtoNode() {
		
		return this.nodeIDtoNode;
	}
	
	/**
	 * Getter method for edgeID to edge mapping.
	 * @return Edge ID to edge mapping.
	 */
	public Edge[] getEdgeIDtoEdge() {
		
		return this.edgeIDtoEdge;
	}
	
	/**
	 * Getter method for maximum edge ID.
	 * @return Maximum edge ID.
	 */
	public int getMaximumEdgeID() {
		
		return this.maximumEdgeID;
	}
	
	/**
	 * Getter method for maximum node ID.
	 * @return Maximum node ID.
	 */
	public int getMaximumNodeID() {
		
		return this.maximumNodeID;
	}
	
	/**
	 * Getter method for edgeID to other direction edgeID mapping.
	 * @return Edge ID to other direction edge ID mapping.
	 */
	public Integer[] getEdgeIDtoOtherDirectionEdgeID() {
		
		return this.edgeIDtoOtherDirectionEdgeID;
	}
	
	
	public boolean[] getStartNodeBlacklist() {
		
		return this.startNodeBlacklist;
	}
	
	public boolean[] getEndNodeBlacklist() {
		
		return this.endNodeBlacklist;
	}
	
	/**
	 * Finds out if the node is blacklisted as a path start node.
	 * @param nodeId Node ID.
	 * @return Whether nodes is blacklisted or not.
	 */
	public boolean isBlacklistedAsStartNode(int nodeId) {
		
		return this.startNodeBlacklist[nodeId]; 
	}
	
	/**
	 * Finds out if the node is blacklisted as a path end node.
	 * @param nodeId Node ID.
	 * @return Whether nodes is blacklisted or not.
	 */
	public boolean isBlacklistedAsEndNode(int nodeId) {
		
		return this.endNodeBlacklist[nodeId]; 
	}
	
	/**
	 * Getter method for free flow travel time.
	 * @return Free flow travel time.
	 */
	public double[] getFreeFlowTravelTime() {
		
		return this.freeFlowTravelTime;
	}
	
	/**
	 * Gets edge length for a given edge ID.
	 * @param edgeID Edge ID.
	 * @return Edge length.
	 */
	public double getEdgeLength(int edgeID) {
		
		//return this.edgeLengths.get(edgeID);
		return this.edgeLengths[edgeID];
	}
	
	public ShapefileDataStore getZonesShapefile () {
		
		return this.zonesShapefile;
	}
	
	public ShapefileDataStore getAADFShapefile () {
		
		return this.AADFshapefile;
	}
	
	public ShapefileDataStore getNetworkShapefile () {
		
		return this.networkShapefile;
	}
	
	public ShapefileDataStore getNewNetworkShapefile () {
		
		return this.newNetworkShapefile;
	}
	
	public ShapefileDataStore getNodesShapefile () {
		
		return this.nodesShapefile;
	}
	
	public double getFreeFlowSpeedMRoad() {
		
		return this.freeFlowSpeedMRoad;
	}
	
	public double getFreeFlowSpeedARoad() {
		
		return this.freeFlowSpeedARoad;
	}
	
	public double getAverageSpeedFerry() {
		
		return this.averageSpeedFerry;
	}
	
	public int getNumberOfLanesMRoad() {
		
		return this.numberOfLanesMRoad;
	}
	
	public int getNumberOfLanesARoad() {
		
		return this.numberOfLanesARoad;
	}
			
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	
		Iterator edgesIterator = network.getEdges().iterator();
		StringBuilder builder = new StringBuilder("E=[");
		while (edgesIterator.hasNext()) {
			DirectedEdge edge = (DirectedEdge) edgesIterator.next();
			builder.append(edge.getID());
			builder.append('(');
			builder.append(edge.getNodeA().getID());
			builder.append("->");
			builder.append(edge.getNodeB().getID());
			builder.append("), ");
		}
		builder.delete(builder.length()-3, builder.length()-1);
		builder.append(']');

		return builder.toString();
	}

	/**
	 * Builds a directed graph representation of the road network
	 */
	private void build() throws IOException {

		//get feature collections from the shapefiles
//		SimpleFeatureCollection networkFeatureCollection = networkShapefile.getFeatureSource().getFeatures();
//		SimpleFeatureCollection AADFfeatureCollection = AADFshapefile.getFeatureSource().getFeatures();
//		SimpleFeatureCollection nodesFeatureCollection = nodesShapefile.getFeatureSource().getFeatures();
//		SimpleFeatureCollection zonesFeatureCollection = zonesShapefile.getFeatureSource().getFeatures();
		
		//caching a large shapefile in memory greatly increases the speed!
		CachingFeatureSource cache = new CachingFeatureSource(AADFshapefile.getFeatureSource());
		SimpleFeatureCollection AADFfeatureCollection = cache.getFeatures();
		
		CachingFeatureSource cache2 = new CachingFeatureSource(networkShapefile.getFeatureSource());
		SimpleFeatureCollection networkFeatureCollection = cache2.getFeatures();
		CachingFeatureSource cache3 = new CachingFeatureSource(nodesShapefile.getFeatureSource());
		SimpleFeatureCollection nodesFeatureCollection = cache3.getFeatures();
		CachingFeatureSource cache4 = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		SimpleFeatureCollection zonesFeatureCollection = cache4.getFeatures();

		LOGGER.info("Creating undirected graph...");
		
		//create undirected graph with features from the network shapefile
		Graph undirectedGraph = createUndirectedGraph(networkFeatureCollection);
		
		//graph builder to build a directed graph
		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();

		LOGGER.info("Adding nodes to directed graph...");
		
		//create nodes of the directed graph directly from the nodes shapefile
		addNodesToGraph(graphBuilder, nodesFeatureCollection);
		
		LOGGER.info("Adding edges to directed graph...");

		//for each edge from the undirected graph create (usually) two directed edges in the directed graph
		//and assign the data from the count points
		addEdgesToGraph(graphBuilder, undirectedGraph, AADFfeatureCollection);
		
		//set the instance field with the generated directed graph
		this.network = (DirectedGraph) graphBuilder.getGraph();

		LOGGER.info("Creating direct access maps for nodes and edges...");
		
		createDirectAccessNodeMap();
		createDirectAccessEdgeMap();
		createEdgeToOtherDirectionEdgeMap();
		
		LOGGER.info("Mapping nodes to LAD zones...");
		
		//map the nodes to zones
		mapNodesToZones(zonesFeatureCollection);
		
		LOGGER.info("Mapping edges to LAD zones...");
		
		//map the edges to zones
		mapEdgesToZones(zonesFeatureCollection);
		
		LOGGER.info("Populating blacklists with unallowed starting/ending node IDs...");
		createNodeBlacklists();
		LOGGER.debug("Start node blacklist: " + this.startNodeBlacklist);
		LOGGER.debug("End node blacklist: " + this.endNodeBlacklist);
		
		LOGGER.info("Determining edges type...");
		
		determineEdgesType();
		
		LOGGER.info("Determining the number of lanes...");
		
		//set number of lanes
		addNumberOfLanes();
		
		LOGGER.info("Storing edge lenghts into the instance map...");
		
		//store edge lengths to the instance map
		storeEdgesLengths();
		
		LOGGER.info("Determining whether edges are urban or rural...");
		
		determineEdgesUrbanRural();
		
		LOGGER.trace("Undirected graph representation of the road network:");
		LOGGER.trace(undirectedGraph);
		LOGGER.trace("Directed graph representation of the road network:");
		LOGGER.trace(network);
	}

	/**
	 * Creates an undirected graph representation of the network from the feature collection.
	 * @param featureCollection Feature collection describing the road network.
	 * @return Undirected graph representation of the road network.
	 */
	private Graph createUndirectedGraph(SimpleFeatureCollection featureCollection) {

		//create a linear graph generator
		DirectedLineStringGraphGenerator lineStringGenerator = new DirectedLineStringGraphGenerator();
		//wrap it in a feature graph generator
		FeatureGraphGenerator featureGenerator = new FeatureGraphGenerator(lineStringGenerator);
		//throw all the features into the graph generator
		SimpleFeatureIterator iter = featureCollection.features();
		try {
			while(iter.hasNext()){
				SimpleFeature feature = iter.next();
				featureGenerator.add(feature);
			}
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
		//build the graph
		Graph graph = featureGenerator.getGraph();
		return graph;
	}

	/**
	 * Builds the nodes of the directed graph.
	 * @param graphBuilder Directed graph builder.
	 * @param nodesFeatureCollection Feature collection with the nodes.
	 */
	private void addNodesToGraph(BasicDirectedLineGraphBuilder graphBuilder, SimpleFeatureCollection nodesFeatureCollection) {

		//iterate through the nodes and add them to the directed graph
		SimpleFeatureIterator iter = nodesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				DirectedNode node = (DirectedNode) graphBuilder.buildNode();
				SimpleFeature sf = iter.next();
				node.setObject(sf);
				//override internally assigned node IDs with the ones from the shapefile
				node.setID(Math.toIntExact((long) sf.getAttribute("nodeID")));
				graphBuilder.addNode(node);
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}

	/** Builds the edges of the directed graph.
	 * @param graphBuilder Directed graph builder.
	 * @param sourceGraph Undirected graph.
	 * @param AADFfeatureCollection Feature collection with count points.
	 */
	private void addEdgesToGraph(BasicDirectedLineGraphBuilder graphBuilder, Graph sourceGraph, SimpleFeatureCollection AADFfeatureCollection) {

		DirectedGraph graphDestination = (DirectedGraph) graphBuilder.getGraph();

		//iterate through the edges of the source graph
		Iterator edgesIterator = sourceGraph.getEdges().iterator();
		
		while (edgesIterator.hasNext()) {
			Edge edge = (Edge) edgesIterator.next();
			SimpleFeature edgeFeature = (SimpleFeature) edge.getObject();
			MultiLineString mls = (MultiLineString) edgeFeature.getDefaultGeometry();
			//System.out.println(edgeFeature.getDefaultGeometryProperty()); //very detailed
			//System.out.println(mls.toText());
			//System.out.println(mls.getGeometryN(0));
			Coordinate c1 = mls.getCoordinates()[0];
			Coordinate c2 = mls.getCoordinates()[1];

			//iterate through the nodes of the destination graph to find the ones that equal the
			//start and end coordinates of the edge of the source graph
			Node nodeA = null;
			Node nodeB = null;
			Iterator nodesIterator = graphDestination.getNodes().iterator();
			while (nodesIterator.hasNext() && (nodeA == null || nodeB == null)) {
				Node node = (Node) nodesIterator.next();
				SimpleFeature feat = (SimpleFeature) node.getObject();
				Point point = (Point) feat.getDefaultGeometry();
				Coordinate coord = point.getCoordinate();
				if (c1.equals(coord)) nodeA = node;
				if (c2.equals(coord)) nodeB = node;	
			}
			if (nodeA == null || nodeB == null) LOGGER.warn("There is a road link for which start or end node is missing in the nodes shapefile.");

			//add edge A->B or B->A or both (depending on the direction information contained in the AADF count points)
			DirectedEdge directedEdge = null;
			DirectedEdge directedEdge2 = null;
			//iterate through AADFs to find counts with the same CP as the edge
			SimpleFeatureIterator iterator = AADFfeatureCollection.features();
		
			boolean firstMatch = false, secondMatch = false;
			char dir = '\u0000';
			try {
				while (iterator.hasNext()) {
					SimpleFeature countFeature = iterator.next();
					long cp =  (long) countFeature.getAttribute("CP");
					double cn = (double) edgeFeature.getAttribute("CP");

					//if count point matches the edge
					if (cp == cn) {
						
						if (firstMatch == false) firstMatch = true;
						else secondMatch = true;
						
						//get the coordinates of the nodes
						SimpleFeature featA = (SimpleFeature) nodeA.getObject();
						SimpleFeature featB = (SimpleFeature) nodeB.getObject();
						Point pointA = (Point) featA.getDefaultGeometry();
						Point pointB = (Point) featB.getDefaultGeometry();
						Coordinate coordA = pointA.getCoordinate(); 
						Coordinate coordB = pointB.getCoordinate();

						//get the direction information from the count point and create directed edge(s)
						String direction = (String) countFeature.getAttribute("iDir");
						dir = direction.charAt(0);
						switch (dir) {
						case 'N': //North
							if (coordB.y > coordA.y) directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
							else					 directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
							directedEdge.setObject(countFeature);
							graphBuilder.addEdge(directedEdge);	break; 
						case 'S': //South
							if (coordB.y < coordA.y) directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
							else					 directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
							directedEdge.setObject(countFeature);
							graphBuilder.addEdge(directedEdge); break; 
						case 'W': //West
							if (coordB.x < coordA.x) directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
							else 					 directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
							directedEdge.setObject(countFeature);
							graphBuilder.addEdge(directedEdge); break; 
						case 'E': //East
							if (coordB.x > coordA.x) directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
							else					 directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
							directedEdge.setObject(countFeature);
							graphBuilder.addEdge(directedEdge); break; 
						case 'C': //Combined directions
							directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
							directedEdge.setObject(countFeature);
							graphBuilder.addEdge(directedEdge);
							directedEdge2 = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
							directedEdge2.setObject(countFeature); // add the same count point to the other direction
							graphBuilder.addEdge(directedEdge2); break; 
						default: LOGGER.warn("Unrecognized direction character.");
						}
					}
					
					if (firstMatch == true && dir == 'C' || secondMatch == true) break;
					
				} //while AADFs
			}
			finally {
				iterator.close();
			}

			if (directedEdge == null) {
				LOGGER.warn("No count point data found for this edge (RoadNumber = '{}', ferry = {}).", 
						edgeFeature.getAttribute("RoadNumber"), edgeFeature.getAttribute("ferry"));

				//still need to create two edges for the ferry line
				directedEdge = (DirectedEdge) graphBuilder.buildEdge(nodeA, nodeB);
				directedEdge.setObject(edge.getObject()); // put the edge from the source graph as the object (contains a distance attribute LenNet used for routing)
				graphBuilder.addEdge(directedEdge);
				directedEdge2 = (DirectedEdge) graphBuilder.buildEdge(nodeB, nodeA);
				directedEdge2.setObject(edge.getObject()); // add the same edge with all the attributes to the other direction (not really a nice solution)
				graphBuilder.addEdge(directedEdge2);
			}
		} //while edges
	}
	
	/**
	 * Maps the nodes of the graph to the zone codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapNodesToZones(SimpleFeatureCollection zonesFeatureCollection) {

		this.nodeToZone = new HashMap<Integer, String>();
		this.zoneToNodes = new HashMap<String, List<Integer>>();
		
		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				
				Iterator nodeIter = (Iterator) network.getNodes().iterator();
				while (nodeIter.hasNext()) {
				
						Node node = (Node) nodeIter.next();
						//if the nodes has already been mapped to its zone, skip it
						if (this.nodeToZone.containsKey(node.getID())) continue;
												
						SimpleFeature sfn = (SimpleFeature) node.getObject();
						Point point = (Point) sfn.getDefaultGeometry();
						//if the polygon contains the node, put that relationship into the maps
						if (polygon.contains(point)) {
							
							nodeToZone.put(node.getID(), (String) sf.getAttribute("CODE"));
							
							List<Integer> listOfNodes = zoneToNodes.get((String) sf.getAttribute("CODE"));
							if (listOfNodes == null) {
								listOfNodes = new ArrayList<Integer>();
								zoneToNodes.put((String) sf.getAttribute("CODE"), listOfNodes);
							}
							listOfNodes.add(node.getID());
						}
				}
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
	
	/**
	 * Maps the edges of the graph to the zone codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapEdgesToZones(SimpleFeatureCollection zonesFeatureCollection) {

		this.edgeToZone = new HashMap<Integer, String>();

		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			//iterate over zones
			while (iter.hasNext()) {
				SimpleFeature featureZone = iter.next();
				MultiPolygon polygon = (MultiPolygon) featureZone.getDefaultGeometry();	

				//iterate over edges	
				for (Object o: this.network.getEdges()) {
					Edge edge = (Edge) o;
					
					//if edge already mapped to zone, skip that edge
					if (this.edgeToZone.containsKey(edge.getID())) continue;
					
					//check if one of the nodes of the edge are not mapped to any zone as this indicates potential problems
					//(e.g. when a node is on the bridge, so it is not contained within any zonal polygon).
					String zone1 = this.nodeToZone.get(edge.getNodeA().getID());
					String zone2 = this.nodeToZone.get(edge.getNodeB().getID());
					
					//if one node falls outside any zonal polygon, then assign edge to the zone of the other node
					if (zone1 == null && zone2 != null) {
						this.edgeToZone.put(edge.getID(), zone2);
						continue;
					}
					if (zone2 == null && zone1 != null) {
						this.edgeToZone.put(edge.getID(), zone1);
						continue;
					}
					//if both nodes fall to the same zone, assign to that zone
					if (zone1 != null && zone2 != null && zone1.equals(zone2)) {
						this.edgeToZone.put(edge.getID(), zone1);
						continue;
					}
					
					//decide zone from the location of the count point object
					String countPointZone = null;
					SimpleFeature sf = (SimpleFeature)edge.getObject();
					if (sf.getDefaultGeometry() instanceof Point) { //TODO this will ignore ferries, but also new road links!
						Point countPoint = (Point) sf.getDefaultGeometry();

						if (polygon.contains(countPoint)) {
							countPointZone = (String) featureZone.getAttribute("CODE");
							this.edgeToZone.put(edge.getID(), countPointZone);
							continue;
						}
					}
				} 
			}
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
		
		//Some edges may still remain unmapped to a zone, e.g. edges for which count point falls outside any polygon (such as river).
		//Iterate again over edges to find unmapped ones and map them using heuristic rules.
		for (Object o: this.network.getEdges()) {
			Edge edge = (Edge) o;
			//if edge is unmapped
			if (!this.edgeToZone.containsKey(edge.getID())) {
				SimpleFeature sf = (SimpleFeature) edge.getObject();
				String roadNumber = (String) sf.getAttribute("RoadNumber");
				if (roadNumber.charAt(0) == 'A' || roadNumber.charAt(0) == 'M') { //if a road and not a ferry
					String zone1 = this.nodeToZone.get(edge.getNodeA().getID());
					if (zone1 != null) { //assign to the zone of nodeA
						this.edgeToZone.put(edge.getID(), zone1);
						LOGGER.debug("Assigning edge {} to zone {} of node {}.", edge.getID(), zone1, edge.getNodeA());
					} else { //assign to the zone of the related edge
						DirectedNode nodeA = (DirectedNode)edge.getNodeA();
						Integer otherEdgeID = this.edgeIDtoOtherDirectionEdgeID[edge.getID()];
						List<Edge> inEdges = nodeA.getInEdges();
						Integer relatedEdgeID = null;
						for (Edge e: inEdges)
							if (e.getID() != otherEdgeID) {
								relatedEdgeID = e.getID();
								break;
							}
						String zone = this.edgeToZone.get(relatedEdgeID);
						this.edgeToZone.put(edge.getID(), zone);
						LOGGER.debug("Assigning edge {} to zone {} of related edge {}", edge.getID(), zone, relatedEdgeID);
					}
				}
			}
		}
	}
	
	private void createDirectAccessNodeMap() {
		
		this.nodeIDtoNode = new Node[this.maximumNodeID + 1];
		
		Iterator nodeIter = (Iterator) network.getNodes().iterator();
		while (nodeIter.hasNext()) {
		
				Node node = (Node) nodeIter.next();
				this.nodeIDtoNode[node.getID()] = node;
		}		
	}
	
	private void createDirectAccessEdgeMap() {
		
		this.edgeIDtoEdge = new Edge[this.maximumEdgeID];
		
		Iterator edgeIter = (Iterator) network.getEdges().iterator();
		while (edgeIter.hasNext()) {
		
				Edge edge = (Edge) edgeIter.next();
				this.edgeIDtoEdge[edge.getID()] = edge;
		}		
	}
	
	private void createEdgeToOtherDirectionEdgeMap() {
		
		this.edgeIDtoOtherDirectionEdgeID = new Integer[this.maximumEdgeID];
		
		Iterator edgeIter = (Iterator) network.getEdges().iterator();
		while (edgeIter.hasNext()) {
		
				DirectedEdge edge = (DirectedEdge) edgeIter.next();
				SimpleFeature sf = (SimpleFeature) edge.getObject();
				String roadNumber = (String) sf.getAttribute("RoadNumber");
				long countPoint;
				if (roadNumber.charAt(0) == 'F')
								countPoint = Math.round((double) sf.getAttribute("CP"));
				else
								countPoint = (long) sf.getAttribute("CP");
				double length1 = (double) sf.getAttribute("LenNet");
				
				//get edges from node B to node A
				DirectedNode nodeA = (DirectedNode) edge.getNodeA();
				DirectedNode nodeB = (DirectedNode) edge.getNodeB();
				List otherEdges = nodeB.getOutEdges(nodeA);
				
				for (Object o: otherEdges) {
					DirectedEdge edge2 = (DirectedEdge) o;
					SimpleFeature sf2 = (SimpleFeature) edge2.getObject();
					String roadNumber2 = (String) sf2.getAttribute("RoadNumber");
					double length2 = (double) sf.getAttribute("LenNet");
					long countPoint2;
					if (roadNumber2.charAt(0) == 'F')
									countPoint2 = Math.round((double) sf2.getAttribute("CP"));
					else
									countPoint2 = (long) sf2.getAttribute("CP");
				
					if (countPoint == countPoint2) //if there is a count point match
						this.edgeIDtoOtherDirectionEdgeID[edge.getID()] = edge2.getID();
						if (length1 != length2) //if lenghts are different something may be wrong (possible with more edges with 0 CP)
							LOGGER.warn("Edge to other direction edge have different lengths!");
				}
	
//				if (this.edgeIDtoOtherDirectionEdgeID.get(edge.getID()) == null)
//					this.edgeIDtoOtherDirectionEdgeID.put(edge.getID(), null);
		}		
	}
	
	private void createNodeBlacklists() {

		this.startNodeBlacklist = new boolean[this.maximumNodeID + 1];
		this.endNodeBlacklist = new boolean[this.maximumNodeID + 1];

		Iterator nodeIter = (Iterator) network.getNodes().iterator();
		while (nodeIter.hasNext()) {
			DirectedNode node = (DirectedNode) nodeIter.next();
			if (node.getOutDegree() == 0) this.startNodeBlacklist[node.getID()] = true;
			if (node.getInDegree() == 0) this.endNodeBlacklist[node.getID()] = true;
		}		
	}
	
	/**
	 * Calculates default number of lanes from road type.
	 */
	private void addNumberOfLanes() {
		
		this.numberOfLanes = new int[this.maximumEdgeID];
		
		//iterate through all the edges in the graph
		Iterator iter = this.network.getEdges().iterator();
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			int edgeID = edge.getID();
			EdgeType edgeType = this.edgesType[edgeID];
			Integer lanes = 0;
			if (edgeType == EdgeType.MOTORWAY) //motorway
				lanes = numberOfLanesMRoad;
			else if (edgeType == EdgeType.AROAD) //A-road
				lanes = numberOfLanesARoad;
			else if (edgeType == EdgeType.FERRY)//ferry
				lanes = 0;
			else {
				LOGGER.warn("Link with unknown road type: {}", edgeID);
				lanes = 0;
			}
			
			this.numberOfLanes[edgeID] = lanes;
		}
	}
		
	/**
	 * Determines if edges are urban, rural or unknown.
	 */
	private void determineEdgesUrbanRural() {
		
		this.isEdgeUrban = new Boolean[this.maximumEdgeID];
		
		//iterate through all the edges in the graph
		Iterator iter = this.network.getEdges().iterator();
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			String cat = (String) sf.getAttribute("RCat"); //road category (PM, PR, Pu, PU, TM, TR, Tu, TU), use Pu, PU, Tu, TU as urban, otherwise rural

			//if no roadCategory information, assume it is ferry and set null
			if (cat == null) {
				LOGGER.trace("No road category information (e.g. ferry), so setting null.");
				isEdgeUrban[edge.getID()] = null;
				continue;
			}
			//if second character is U, the edge is urban, otherwise rural
			if (cat.toUpperCase().charAt(1) == 'U') 
				isEdgeUrban[edge.getID()] = Boolean.TRUE;
			else									
				isEdgeUrban[edge.getID()] = Boolean.FALSE;
		}
	}
	
	/**
	 * Determines if edges are ferry or not.
	 */
	private void determineEdgesType() {
		
		this.edgesType = new EdgeType[this.maximumEdgeID];
		
		//iterate through all the edges in the graph
		Iterator iter = this.network.getEdges().iterator();
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.toUpperCase().charAt(0) == 'A') //A-road
				this.edgesType[edge.getID()] = EdgeType.AROAD;
			else if (roadNumber.toUpperCase().charAt(0) == 'M') //Motorway
				this.edgesType[edge.getID()] = EdgeType.MOTORWAY;
			else if (roadNumber.toUpperCase().charAt(0) == 'F') //ferry
				this.edgesType[edge.getID()] = EdgeType.FERRY;
			else
				LOGGER.warn("Edge {} has unknown type (aroad/motorway/ferry)", edge.getID());
		}
	}
	
	/**
	 * Store edge lengths to the instance map.
	 */
	private void storeEdgesLengths() {
		
		//this.edgeLengths = new HashMap<Integer, Double>();
		this.edgeLengths = new double[this.maximumEdgeID];
		
		//iterate through all the edges in the graph
		Iterator iter = this.network.getEdges().iterator();
		while(iter.hasNext()) {
			
			Edge edge = (Edge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double length = (double) sf.getAttribute("LenNet");
			//this.edgeLengths.put(edge.getID(), length);
			this.edgeLengths[edge.getID()] = length;
		}
	}
		
	/**
	 * Loads code area population data.
	 * @param areaCodeFileName File with area code population data.
	 */
	private void loadAreaCodePopulationData(String areaCodeFileName) throws IOException {

		this.zoneToAreaCodes = new HashMap<String, List<String>>();
		this.areaCodeToPopulation = new HashMap<String, Integer>();
		
		CSVParser parser = new CSVParser(new FileReader(areaCodeFileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			String areaCode = record.get("area_code");
			String zoneCode = record.get("zone_code");
			int population = Integer.parseInt(record.get("population"));
			
			List<String> areaCodesList = zoneToAreaCodes.get(zoneCode);
			if (areaCodesList == null) {
				areaCodesList = new ArrayList<String>();
				zoneToAreaCodes.put(zoneCode, areaCodesList);
			}
			areaCodesList.add(areaCode);
			
			areaCodeToPopulation.put(areaCode, population);
		} 
		parser.close(); 
	}
	
	/**
	 * Loads workplace zone population data.
	 * @param workplaceZoneFileName File with workplace zone population data.
	 */
	private void loadWorkplaceZonePopulationData(String workplaceZoneFileName) throws IOException {

		this.zoneToWorkplaceCodes = new HashMap<String, List<String>>();
		this.workplaceZoneToPopulation = new HashMap<String, Integer>();
		
		CSVParser parser = new CSVParser(new FileReader(workplaceZoneFileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			String workplaceCode = record.get("workplace_code");
			String zoneCode = record.get("lad_code");
			int population = Integer.parseInt(record.get("population"));
			
			List<String> workplaceCodesList = zoneToWorkplaceCodes.get(zoneCode);
			if (workplaceCodesList == null) {
				workplaceCodesList = new ArrayList<String>();
				zoneToWorkplaceCodes.put(zoneCode, workplaceCodesList);
			}
			workplaceCodesList.add(workplaceCode);
			
			workplaceZoneToPopulation.put(workplaceCode, population);
		} 
		parser.close(); 
	}
	
	/**
	 * Loads code area to nearest node mapping.
	 * @param areaCodeNearestNodeFile File with area code nearest neighbour.
	 */
	private void loadAreaCodeNearestNodeAndDistance(String areaCodeNearestNodeFile) throws IOException {

		this.areaCodeToNearestNodeID = new HashMap<String, Integer>();
		this.areaCodeToNearestNodeDistance = new HashMap<String, Double>();
		
		CSVParser parser = new CSVParser(new FileReader(areaCodeNearestNodeFile), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			String areaCode = record.get("area_code");
			int nodeID = Integer.parseInt(record.get("nodeID"));
			double distance = Double.parseDouble(record.get("distance"));
			areaCodeToNearestNodeID.put(areaCode, nodeID);
			areaCodeToNearestNodeDistance.put(areaCode, distance);
		} 
		parser.close(); 
	}
	
	/**
	 * Loads workplace zone to nearest node mapping.
	 * @param workplaceZoneNearestNodeFile File with workplace zone nearest neighbours.
	 */
	private void loadWorkplaceZoneNearestNodeAndDistance(String workplaceZoneNearestNodeFile) throws IOException {

		this.workplaceZoneToNearestNodeID = new HashMap<String, Integer>();
		this.workplaceZoneToNearestNodeDistance = new HashMap<String, Double>();
		
		CSVParser parser = new CSVParser(new FileReader(workplaceZoneNearestNodeFile), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			String workplaceCode = record.get("workplace_code");
			int nodeID = Integer.parseInt(record.get("nodeID"));
			double distance = Double.parseDouble(record.get("distance"));
			workplaceZoneToNearestNodeID.put(workplaceCode, nodeID);
			workplaceZoneToNearestNodeDistance.put(workplaceCode, distance);
		} 
		parser.close(); 
	}

	/**
	 * Loads freight zone to LAD zone mapping (for freight zone that are LADs).
	 * @param freightZoneToLADfile File with freight zone to LAD mapping.
	 */
	private void loadFreightZoneToLAD(String freightZoneToLADfile) throws IOException {

		this.freightZoneToLAD = new HashMap<Integer, String>();
			
		CSVParser parser = new CSVParser(new FileReader(freightZoneToLADfile), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			int freightZone = Integer.parseInt(record.get("freight_code"));
			String lad = record.get("lad_code");
			freightZoneToLAD.put(freightZone, lad);
		} 
		parser.close(); 
	}
	
	/**
	 * Loads freight zone to nearest node mapping (for freight zones that are points: airports, distribution centres, ports).
	 * @param freightZoneNearestNodeFile File with area code nearest neighbour.
	 */
	private void loadFreightZoneNearestNodeAndDistance(String freightZoneNearestNodeFile) throws IOException {

		this.freightZoneToNearestNode = new HashMap<Integer, Integer>();
		this.freightZoneToNearestNodeDistance = new HashMap<Integer, Double>();
		
		CSVParser parser = new CSVParser(new FileReader(freightZoneNearestNodeFile), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			int freightZone = Integer.parseInt(record.get("freight_code"));
			int nodeID = Integer.parseInt(record.get("nodeID"));
			double distance = Double.parseDouble(record.get("distance"));
			freightZoneToNearestNode.put(freightZone, nodeID);
			freightZoneToNearestNodeDistance.put(freightZone, distance);
		} 
		parser.close(); 
	}
	
	/**
	 * Calculates the population gravitating to each node by summing up the population of area codes
	 * which have been mapped to this node using the nearest neighbour method.
	 * Nodes with 0 gravitating population are not a member of this map!
	 */
	private void calculateNodeGravitatingPopulation() {
		
		int[] total = new int[this.maximumNodeID + 1];
		
		for (String areaCode: this.areaCodeToNearestNodeID.keySet()) {
			
			int node = this.areaCodeToNearestNodeID.get(areaCode);
			int population = this.areaCodeToPopulation.get(areaCode);
			
			total[node] += population;
		}
		
		this.nodeToGravitatingPopulation = total;
	}
	
	/**
	 * Calculates the workplace population gravitating to each node by summing up the population of workplace zones
	 * which have been mapped to this node using the nearest neighbour method.
	 * Nodes with 0 gravitating population are not a member of this map!
	 */
	private void calculateNodeGravitatingWorkplacePopulation() {
		
		int[] total = new int[this.maximumNodeID + 1];
		
		for (String workplaceZone: this.workplaceZoneToNearestNodeID.keySet()) {
			
			int node = this.workplaceZoneToNearestNodeID.get(workplaceZone);
			int population = this.workplaceZoneToPopulation.get(workplaceZone);
			
			total[node] += population;
		}
		
		this.nodeToGravitatingWorkplacePopulation = total;
	}
	
	/**
	 * Calculates average access/egress distance to each node that has a gravitating population
	 */
	private void calculateNodeAccessEgressDistance() {
		
		double[] map = new double[this.maximumNodeID + 1];
		
		for (String areaCode: this.areaCodeToNearestNodeID.keySet()) {
			
			int node = this.areaCodeToNearestNodeID.get(areaCode);
			int population = this.areaCodeToPopulation.get(areaCode);
			double distance = this.areaCodeToNearestNodeDistance.get(areaCode) * this.accessEgressFactor;
			double gravitatingPopulation = this.nodeToGravitatingPopulation[node];
			
			double weightedDistance = distance * population / gravitatingPopulation;
			map[node] += weightedDistance;
		}
				
		this.nodeToAverageAccessEgressDistance = map;
	}
	
	/**
	 * Calculates average access/egress distance to each node that has a gravitating workplace population
	 * This is used only for freight zones that are LADs (not points such as ports, airports and distribution centres).
	 * Nodes with 0 gravitating population are not a member of this map!
	 */
	private void calculateNodeAccessEgressDistanceFreight() {
		
		double[] map = new double[this.maximumNodeID + 1];
		
		for (String workplaceZone: this.workplaceZoneToNearestNodeID.keySet()) {
			
			int node = this.workplaceZoneToNearestNodeID.get(workplaceZone);
			int population = this.workplaceZoneToPopulation.get(workplaceZone);
			double distance = this.workplaceZoneToNearestNodeDistance.get(workplaceZone) * this.accessEgressFactor;
			double gravitatingPopulation = this.nodeToGravitatingWorkplacePopulation[node];
			
			double weightedDistance = distance * population / gravitatingPopulation;
			map[node] += weightedDistance;
		}
		
		this.nodeToAverageAccessEgressDistanceFreight = map;
	}
	
	/**
	 * For each zone (LAD) sorts the list of contained nodes based on the gravitating population.
	 */
	public void sortGravityNodes() {
		
		int[] nodeToPop = this.nodeToGravitatingPopulation;
		
		Comparator<Integer> c = new Comparator<Integer>() {
		    public int compare(Integer s, Integer s2) {
		    	Integer population = nodeToPop[s];
		    	Integer population2 = nodeToPop[s2];
	    	
		    	return population2.compareTo(population);
		    	}
		};
		
		for (String LAD: this.zoneToNodes.keySet()) {
			List list = this.zoneToNodes.get(LAD);
			Collections.sort(list, c);
		}
	}
	
	/**
	 * For each zone (LAD) sorts the list of contained nodes based on the gravitating workplace population.
	 */
	public void sortGravityNodesFreight() {
		
		int[] nodeToPop = this.nodeToGravitatingWorkplacePopulation;
		
		Comparator<Integer> c = new Comparator<Integer>() {
		    public int compare(Integer s, Integer s2) {
		    	Integer population = nodeToPop[s];
		    	Integer population2 = nodeToPop[s2];
		    			    	
		    	return population2.compareTo(population);
		    	}
		};
		
		for (String LAD: this.zoneToNodes.keySet()) {
			List list = this.zoneToNodes.get(LAD);
			Collections.sort(list, c);
		}
	}
	
	/**
	 * Prompts the user for the name and path to use for the output shapefile.
	 * @param fileName Name of the shapefile.
	 * @return Name and path for the shapefile as a new File object.
	 */
	private static File getNewShapeFile(String fileName) {

		//String defaultPath = "./output.shp";
		String defaultPath = "./temp/" + fileName + ".shp";
		JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
		chooser.setDialogTitle("Save shapefile");
		chooser.setSelectedFile(new File(defaultPath));

		int returnVal = chooser.showSaveDialog(null);
		if (returnVal != JFileDataStoreChooser.APPROVE_OPTION) {
			// the user cancelled the dialog
			System.exit(0);
		}

		File newFile = chooser.getSelectedFile();
		return newFile;
	}

	/**
	 * Creates the schema for the shapefile.
	 * @return SimpleFeature type.
	 */
	private static SimpleFeatureType createFeatureType() {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem crs;
		try {
			//British National Grid
			crs = CRS.decode("EPSG:27700", false);
			//builder.setCRS(DefaultGeographicCRS.WGS84);
			builder.setCRS(crs);
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		}
		builder.setName("Location");

		//add attributes in order
		builder.add("the_geom", LineString.class);
		builder.add("EdgeID", Integer.class);
		builder.add("Anode", Integer.class);
		builder.add("Bnode", Integer.class);
		builder.add("CP", Integer.class);
		builder.add("RoadNumber", String.class);
		builder.length(1).add("iDir", String.class);
		builder.add("SRefE", Integer.class);
		builder.add("SRefN", Integer.class);
		builder.add("Distance", Double.class);
		builder.add("FFspeed", Double.class);
		builder.add("FFtime", Double.class);
		builder.add("IsFerry", Boolean.class);
		
		//build the type
		final SimpleFeatureType SIMPLE_FEATURE_TYPE = builder.buildFeatureType();
		//return the type
		return SIMPLE_FEATURE_TYPE;
	}
	
	/**
	 * Creates a custom schema for the network.
	 * @param linkDataLabel The label for the link data (e.g. "DayVolume").
	 * @return SimpleFeature type.
	 */
	public static SimpleFeatureType createCustomFeatureType(String linkDataLabel) {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		
		CoordinateReferenceSystem crs;
		try {
			//British National Grid
			crs = CRS.decode("EPSG:27700", false);
			//builder.setCRS(DefaultGeographicCRS.WGS84);
			builder.setCRS(crs);
		} catch (NoSuchAuthorityCodeException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		}
		builder.setName("Location");

		//add attributes in order
		builder.add("the_geom", LineString.class);
		builder.add("EdgeID", Integer.class);
		builder.add("Anode", Integer.class);
		builder.add("Bnode", Integer.class);
		builder.add("CP", Integer.class);
		builder.add("RoadNumber", String.class);
		builder.length(1).add("iDir", String.class);
		builder.add("SRefE", Integer.class);
		builder.add("SRefN", Integer.class);
		builder.add("Distance", Double.class);
		builder.length(6).add("FFspeed", Double.class);
		builder.length(6).add("FFtime", Double.class);
		builder.add("IsFerry", Boolean.class);
		builder.add("Lanes", Integer.class);
		builder.length(6).add(linkDataLabel, Double.class); //e.g. "DayVolume"

		//build the type
		final SimpleFeatureType SIMPLE_FEATURE_TYPE = builder.buildFeatureType();
		//return the type
		return SIMPLE_FEATURE_TYPE;
	}
	
	/**
	 * Creates the schema for the object assigned to new road links
	 * @return SimpleFeature type.
	 */
	private static SimpleFeatureType createNewRoadLinkFeatureType() {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

		builder.setName("New road link");
		
		//TODO A point might be added as a geometry type to mimic a count point
		//and to help determine where the road link is located (e.g. which zone)
		
		//add attributes in order
		builder.add("CP", Long.class);
		//builder.length(1).add("iDir", String.class);
		builder.add("RoadNumber", String.class );
		builder.add("LenNet", Double.class);
		//builder.add("IsFerry", Boolean.class);

		//build the type
		final SimpleFeatureType SIMPLE_FEATURE_TYPE = builder.buildFeatureType();
		//return the type
		return SIMPLE_FEATURE_TYPE;
	}
}
