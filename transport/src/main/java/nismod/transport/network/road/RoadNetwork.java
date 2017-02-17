/**
 * 
 */
package nismod.transport.network.road;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.collection.ListFeatureCollection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
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
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * A routable road network built from the shapefiles.
 * @author Milan Lovric
 *
 */
public class RoadNetwork {

	private DirectedGraph network;
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore networkShapefile;
	private ShapefileDataStore nodesShapefile;
	private ShapefileDataStore AADFshapefile;
	private DijkstraIterator.EdgeWeighter dijkstraWeighter;
	private DijkstraIterator.EdgeWeighter dijkstraTimeWeighter;
	private HashMap<Integer, String> nodeToZone;
	private HashMap<String, List<Integer>> zoneToNodes;
	private HashMap<String, List<String>> zoneToAreaCodes;
	private HashMap<String, Integer> areaCodeToNearestNode;
	private HashMap<String, Integer> areaCodeToPopulation;
	private HashMap<Integer, Integer> freightZoneToNearestNode;
	private HashMap<Integer, String> freightZoneToLAD;

	/**
	 * @param zonesUrl Url for the shapefile with zone polygons
	 * @param networkUrl Url for the shapefile with road network
	 * @param nodesUrl Url for the shapefile with nodes
	 * @param AADFurl Url for the shapefile with AADF counts
	 * @throws IOException 
	 */
	public RoadNetwork(URL zonesUrl, URL networkUrl, URL nodesUrl, URL AADFurl, String areaCodeFileName, String areaCodeNearestNodeFile) throws IOException {

		zonesShapefile = new ShapefileDataStore(zonesUrl);
		networkShapefile = new ShapefileDataStore(networkUrl);
		nodesShapefile = new ShapefileDataStore(nodesUrl);
		AADFshapefile = new ShapefileDataStore(AADFurl);

		//build the graph
		this.build();

		//weight the edges of the graph using the physical distance of each road segment		
		dijkstraWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {
				SimpleFeature sf = (SimpleFeature) edge.getObject(); 
				double length = (double) sf.getAttribute("LenNet");
				return length;
			}
		};
		
		//weight the edges of the graph using the free-flow travel time		
		dijkstraTimeWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {
				SimpleFeature feature = (SimpleFeature) edge.getObject(); 
				double length = (double) feature.getAttribute("LenNet");
				double cost;
				String roadNumber = (String) feature.getAttribute("RoadNumber");
					if (roadNumber.charAt(0) == 'M') //motorway
						cost = length / RoadNetworkAssignment.SPEED_LIMIT_M_ROAD * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'A') //A road
						cost = length / RoadNetworkAssignment.SPEED_LIMIT_A_ROAD * 60;  //travel time in minutes
					else //ferry
						cost = length / RoadNetworkAssignment.AVERAGE_SPEED_FERRY * 60;  //travel time in minutes
				return cost;
			}
		};
		
		this.loadAreaCodePopulationData(areaCodeFileName);
		this.loadAreaCodeNearestNode(areaCodeNearestNodeFile);
		
		this.freightZoneToLAD = new HashMap<Integer, String>();
		this.freightZoneToLAD.put(855, "E06000045");
		this.freightZoneToLAD.put(854, "E07000086");
		this.freightZoneToLAD.put(866, "E07000091");
		this.freightZoneToLAD.put(867, "E06000046");
		this.freightZoneToNearestNode = new HashMap<Integer, Integer>();
		this.freightZoneToNearestNode.put(1312, 86);
		this.freightZoneToNearestNode.put(1313, 115);
			
		//System.out.println(this.zoneToAreaCodes);
		//System.out.println(this.areaCodeToPopulation);
		//System.out.println(this.areaCodeToNearestNode);
	}

	/**
	 * Visualises the road network as loaded from shapefiles.
	 * @param mapTitle Map title for the window.
	 * @throws IOException
	 */
	public void visualise(String mapTitle) throws IOException {

		//create a map
		MapContent map = new MapContent();
		//set windows title
		map.setTitle(mapTitle);

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

	/**
	 * Exports a directed multigraph representation of the network as a shapefile.
	 * @param fileName The name of the output shapefile.
	 * @throws IOException
	 */
	public void exportToShapefile(String fileName) throws IOException {

		if (network == null) {
			System.err.println("The network is empty.");
			return;
		}

		//get an output file name and create the new shapefile
		File newFile = getNewShapeFile(fileName);

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		Map<String, Serializable> params = new HashMap<>();
		params.put("url", newFile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

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
			if (feat != null) { //has a count point
				objList.add(feat.getAttribute("CP"));
				objList.add(feat.getAttribute("iDir"));
				objList.add(feat.getAttribute("S Ref E"));
				objList.add(feat.getAttribute("S Ref N"));
				objList.add(feat.getAttribute("LenNet"));
				String roadNumber = (String) feat.getAttribute("RoadNumber");
				if (roadNumber.charAt(0) == 'M') {//motorway
					objList.add(RoadNetworkAssignment.SPEED_LIMIT_M_ROAD);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_M_ROAD * 60; //in minutes
					objList.add(freeFlowTime);
				} else { //A road
					objList.add(RoadNetworkAssignment.SPEED_LIMIT_A_ROAD);
					Double freeFlowTime = (double)feat.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_A_ROAD * 60; //in minutes
					objList.add(freeFlowTime);
				}
				objList.add(false); //not a ferry
			} else { //no count point, assume it is ferry (TODO could be link with no count point)
				objList.add(0); //null
				objList.add(0); //null
				objList.add(0); //null
				objList.add(0); //null
				objList.add(10);
				final Double SPEED_LIMIT = 30.0; //30 kph
				Double freeFlowTime = 20.0; //TODO actual ferry travel time needed
				objList.add(freeFlowTime);
				objList.add(true); //is a ferry
			}
			SimpleFeature feature = featureBuilder.build(type, objList, Integer.toString(edge.getID()));
			//System.out.println(feature.toString());
			features.add(feature);
		}

		newDataStore.createSchema(type);

		//Write the features to the shapefile using a Transaction
		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
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
		System.out.println("SHAPE:"+SHAPE_TYPE);

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
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			System.err.println(typeName + " does not support read/write access");
		}
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
	 * @return Dijkstra edge weighter with time.
	 */
	public DijkstraIterator.EdgeWeighter getDijkstraTimeWeighter() {

		return dijkstraTimeWeighter;
	}

	/** Getter method for the AStar functions (edge cost and heuristic function) based on distance.
	 * @param to Destination node.
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
					return Double.MAX_VALUE;
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
	 * @param to Destination node.
	 * @param linkTravelTime Link travel times.
	 * @return AStar functions.
	 */
	public AStarIterator.AStarFunctions getAstarFunctionsTime(Node destinationNode, HashMap<Integer, Double> linkTravelTime) {

		AStarIterator.AStarFunctions aStarFunctions = new  AStarIterator.AStarFunctions(destinationNode){

			@Override
			public double cost(AStarIterator.AStarNode aStarNode1, AStarIterator.AStarNode aStarNode2) {

				//Edge edge = aStarNode1.getNode().getEdge(aStarNode2.getNode()); // does not work, a directed version must be used!
				Edge edge = ((DirectedNode) aStarNode1.getNode()).getOutEdge((DirectedNode) aStarNode2.getNode());
				if (edge == null) {
					//no edge found in that direction (set maximum weight)
					return Double.MAX_VALUE;
				}
				double cost;
				if (linkTravelTime.get(edge.getID()) == null) { //use default link travel time
					SimpleFeature feature = (SimpleFeature)edge.getObject();
					String roadNumber = (String) feature.getAttribute("RoadNumber");
					if (roadNumber.charAt(0) == 'M') //motorway
						cost = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_M_ROAD * 60;  //travel time in minutes
					else if (roadNumber.charAt(0) == 'A') //A road
						cost = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.SPEED_LIMIT_A_ROAD * 60;  //travel time in minutes
					else //ferry
						cost = (double) feature.getAttribute("LenNet") / RoadNetworkAssignment.AVERAGE_SPEED_FERRY * 60;  //travel time in minutes
				} else //use provided travel time
					cost = linkTravelTime.get(edge.getID()); 
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
				return (distance / RoadNetworkAssignment.SPEED_LIMIT_M_ROAD * 60); //travel time in minutes
			}
		};

		return aStarFunctions;
	}
	
	/**
	 * Getter method for the node to zone mapping.
	 * @return Node to zone mapping.
	 */
	public HashMap<Integer, String> getNodeToZone() {

		return this.nodeToZone;
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
	public HashMap<String, Integer> getAreaCodeToNearestNode() {

		return this.areaCodeToNearestNode;
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
	 * @throws IOException 
	 */
	private void build() throws IOException {

		//get feature collections from the shapefiles
		SimpleFeatureCollection networkFeatureCollection = networkShapefile.getFeatureSource().getFeatures();
		SimpleFeatureCollection nodesFeatureCollection = nodesShapefile.getFeatureSource().getFeatures();
		SimpleFeatureCollection AADFfeatureCollection = AADFshapefile.getFeatureSource().getFeatures();
		SimpleFeatureCollection zonesFeatureCollection = zonesShapefile.getFeatureSource().getFeatures();

		//create undirected graph with features from the network shapefile
		Graph undirectedGraph = createUndirectedGraph(networkFeatureCollection);

		//graph builder to build a directed graph
		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();

		//create nodes of the directed graph directly from the nodes shapefile
		addNodesToGraph(graphBuilder, nodesFeatureCollection);

		//for each edge from the undirected graph create (usually) two directed edges in the directed graph
		//and assign the data from the count points
		addEdgesToGraph(graphBuilder, undirectedGraph, AADFfeatureCollection);
		
		//set the instance field with the generated directed graph
		this.network = (DirectedGraph) graphBuilder.getGraph();

		//map the nodes to zones
		mapNodesToZones(zonesFeatureCollection);
		
		System.out.println("Undirected graph representation of the road network:");
		System.out.println(undirectedGraph);
		System.out.println("Directed graph representation of the road network:");
		System.out.println(network);
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
			if (nodeA == null || nodeB == null) System.err.println("There is a road link for which start or end node is missing in the nodes shapefile.");

			//add edge A->B or B->A or both (depending on the direction information contained in the AADF count points)
			DirectedEdge directedEdge = null;
			DirectedEdge directedEdge2 = null;
			//iterate through AADFs to find counts with the same CP as the edge
			SimpleFeatureIterator iterator = AADFfeatureCollection.features();
			try {
				while (iterator.hasNext()) {
					SimpleFeature countFeature = iterator.next();
					long cp =  (long) countFeature.getAttribute("CP");
					double cn = (double) edgeFeature.getAttribute("CP_Number");

					//if count point matches the edge
					if (cp == cn) {
						//get the coordinates of the nodes
						SimpleFeature featA = (SimpleFeature) nodeA.getObject();
						SimpleFeature featB = (SimpleFeature) nodeB.getObject();
						Point pointA = (Point) featA.getDefaultGeometry();
						Point pointB = (Point) featB.getDefaultGeometry();
						Coordinate coordA = pointA.getCoordinate(); 
						Coordinate coordB = pointB.getCoordinate();

						//get the direction information from the count point and create directed edge(s)
						String direction = (String) countFeature.getAttribute("iDir");
						char dir = direction.charAt(0);
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
						default: System.err.println("Unrecognized direction character.");
						}
					}
				} //while AADFs
			}
			finally {
				iterator.close();
			}

			if (directedEdge == null) {
				System.err.printf("No count point data found for this edge (RoadNumber = '%s', ferry = %d).\n", 
						edgeFeature.getAttribute("RoadNumber"), edgeFeature.getAttribute("ferry"));

				//still need to create to edges for the ferry line
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
	 * Loads code area population data.
	 * @param areaCodeFileName File with area code population data.
	 * @throws IOException 
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
	 * Loads code area to nearest node mapping.
	 * @param areaCodeNearestNodeFile File with area code nearest neighbour.
	 * @throws IOException 
	 */
	private void loadAreaCodeNearestNode(String areaCodeNearestNodeFile) throws IOException {

		this.areaCodeToNearestNode = new HashMap<String, Integer>();
		
		CSVParser parser = new CSVParser(new FileReader(areaCodeNearestNodeFile), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		for (CSVRecord record : parser) {
			//System.out.println(record);
			String areaCode = record.get("area_code");
			int nodeID = Integer.parseInt(record.get("nodeID"));
			areaCodeToNearestNode.put(areaCode, nodeID);
		} 
		parser.close(); 
	}
	
	/**
	 * Prompts the user for the name and path to use for the output shapefile.
	 * @param fileName Name of the shapefile.
	 * @return Name and path for the shapefile as a new File object.
	 */
	private static File getNewShapeFile(String fileName) {

		//String defaultPath = "./output.shp";
		String defaultPath = "./" + fileName + ".shp";
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
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		builder.setName("Location");

		//add attributes in order
		builder.add("the_geom", LineString.class);
		builder.add("EdgeID",Integer.class);
		builder.add("Anode",Integer.class);
		builder.add("Bnode",Integer.class);
		builder.add("CountPoint", Integer.class);
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
}
