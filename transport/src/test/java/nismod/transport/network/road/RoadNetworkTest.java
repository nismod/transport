/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import nismod.transport.network.road.RoadNetwork.EdgeType;
import nismod.transport.utility.ConfigReader;
import nismod.transport.visualisation.NetworkVisualiser;

/**
 * Tests for the RoadNetworkAssignment class
 * @author Milan Lovric
 *
 */
public class RoadNetworkTest {

	private static final double EPSILON = 1e-15;

	public static void main( String[] args ) throws IOException	{

		final String configFile = "./src/test/config/miniTestConfig.properties";
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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		//visualise the shapefiles
		NetworkVisualiser.visualise(roadNetwork, "Mini Test Area");
	
//		//export to shapefile
		roadNetwork.exportToShapefile("miniOutputNetwork");

		final String configFile2 = "./src/test/config/testConfig.properties";
		Properties props2 = ConfigReader.getProperties(configFile2);
		
		final String areaCodeFileName2 = props2.getProperty("areaCodeFileName");
		final String areaCodeNearestNodeFile2 = props2.getProperty("areaCodeNearestNodeFile");
		final String workplaceZoneFileName2 = props2.getProperty("workplaceZoneFileName");
		final String workplaceZoneNearestNodeFile2 = props2.getProperty("workplaceZoneNearestNodeFile");
		final String freightZoneToLADfile2 = props2.getProperty("freightZoneToLADfile");
		final String freightZoneNearestNodeFile2 = props2.getProperty("freightZoneNearestNodeFile");

		final URL zonesUrl2 = new URL(props2.getProperty("zonesUrl"));
		final URL networkUrl2 = new URL(props2.getProperty("networkUrl"));
		final URL networkUrlFixedEdgeIDs2 = new URL(props2.getProperty("networkUrlFixedEdgeIDs"));
		final URL nodesUrl2 = new URL(props2.getProperty("nodesUrl"));
		final URL AADFurl2 = new URL(props2.getProperty("AADFurl"));

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName2, areaCodeNearestNodeFile2, workplaceZoneFileName2, workplaceZoneNearestNodeFile2, freightZoneToLADfile2, freightZoneNearestNodeFile2, props2);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs2);
				
		//visualise the shapefiles
		NetworkVisualiser.visualise(roadNetwork2, "Midi Test Area");

		//create new road link
		Node fromNode = roadNetwork2.getNodeIDtoNode()[86];
		Node toNode = roadNetwork2.getNodeIDtoNode()[48];
		DirectedEdge newEdge = (DirectedEdge) roadNetwork2.createNewRoadLink(fromNode, toNode, 2, 'A', 0.6);
		DirectedEdge newEdge2 = (DirectedEdge) roadNetwork2.createNewRoadLink(toNode, fromNode, 2, 'A', 0.6);

		//export to shapefile
		roadNetwork2.exportToShapefile("midiOutputNetwork");

		//visualise updated network 
		NetworkVisualiser.visualise(roadNetwork2, "Midi Test Area With New Road Link");

//		final URL zonesUrl3 = new URL("file://src/main/resources/data/zones.shp");
//		final URL networkUrl3 = new URL("file://src/main/resources/data/network.shp");
//		final URL nodesUrl3 = new URL("file://src/main/resources/data/nodes.shp");
//		final URL AADFurl3 = new URL("file://src/main/resources/data/AADFdirected2015.shp");
//
//		long timeNow = System.currentTimeMillis();
//
//		//create a road network
//		RoadNetwork roadNetwork3 = new RoadNetwork(zonesUrl3, networkUrl3, nodesUrl3, AADFurl3, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
//
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Road network built in %d seconds.\n", timeNow / 1000);
//
//		//visualise the shapefiles
//		NetworkVisualiser.visualise(roadNetwork3, "Major Road Network");
//
//		//export to shapefile
//		roadNetwork3.exportToShapefile("fullOutputNetwork");
	}

	@BeforeClass
	public static void initialise() {
	
	    File file = new File("./temp");
	    if (!file.exists()) {
	        if (file.mkdir()) {
	            System.out.println("Temp directory is created.");
	        } else {
	            System.err.println("Failed to create temp directory.");
	        }
	    }
	}
	
	@Test
	public void miniTest() throws IOException {

		final String configFile = "./src/test/config/miniTestConfig.properties";
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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		DirectedGraph rn = roadNetwork.getNetwork();

		//TEST NODE AND EDGE CREATION
		System.out.println("\n\n*** Testing node and edge creation ***");

		Iterator iter = rn.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 55) nodeA = node;
			if (node.getID() == 40) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 55, nodeA.getID());
		assertEquals("Node ID is correct", 40, nodeB.getID());
		DirectedEdge edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		DirectedEdge edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		SimpleFeature sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "W", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "E", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 6, nodeB.getDegree());
		assertEquals("Node out degree is correct", 3, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 3, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 87) nodeA = node;
			if (node.getID() == 105) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 87, nodeA.getID());
		assertEquals("Node ID is correct", 105, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 95) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 95, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48513L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "N", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		assertNull("Expecting no edge in this direction", edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48317L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 2, nodeA.getDegree());
		assertEquals("Node out degree is correct", 1, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 1, nodeA.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 95) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 95, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48456L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.1, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);

		//TEST NODE BLACKLISTS
		iter = rn.getNodes().iterator();
		while (iter.hasNext()) {
			DirectedNode node = (DirectedNode) iter.next();
			System.out.printf("Node %d has %d in degree and %d out degree. \n", node.getID(), node.getInDegree(), node.getOutDegree());
			System.out.printf("Blacklisted as start node is %b, blacklisted as end node is %b \n", roadNetwork.isBlacklistedAsStartNode(node.getID()), roadNetwork.isBlacklistedAsEndNode(node.getID()));		
		}

		//TEST EDGE TO OTHER DIRECTION EDGE MAPPING
		System.out.println("\n\n*** Testing edge to other direction edge mapping ***");
		
		System.out.println(rn.getNodes());
		System.out.println(rn.getEdges());
		
		int count = 0;
		for (int edgeID=1; edgeID < roadNetwork.getMaximumEdgeID(); edgeID++)
			if (roadNetwork.getEdgeIDtoEdge()[edgeID] != null) {
				assertEquals("Array element points to the right edge", edgeID, roadNetwork.getEdgeIDtoEdge()[edgeID].getID());
				count++;
			}
		assertEquals("Number of edges in direct array is correct", rn.getEdges().size(), count);
		
		count = 0;
		for (int edgeID=1; edgeID < roadNetwork.getMaximumEdgeID(); edgeID++)
			if (roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[edgeID] != null) {
				count++;
			}
		assertEquals("Number of opposite edges in direct array is correct", 19*2, count);
		
		assertEquals("Array element points to the right edge", 66, (int) roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[65]);
		assertEquals("Array element points to the right edge", 65, (int) roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[66]);
		assertNull("Unidirectional edge does not have opposite direction edge", roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[77]);
		assertNull("Unidirectional edge does not have opposite direction edge", roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[64]);
		assertNull("Unidirectional edge does not have opposite direction edge", roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[90]);
	
		//System.out.println(Arrays.toString(roadNetwork.getEdgeIDtoEdge()));
		//System.out.println(Arrays.toString(roadNetwork.getEdgeIDtoOtherDirectionEdgeID()));
		
		int edgeID = 66;
		System.out.println(roadNetwork.getFreeFlowTravelTime()[edgeID]);
		System.out.println(roadNetwork.getEdgeLength(edgeID));
		System.out.println(roadNetwork.getIsEdgeUrban()[edgeID]);
		System.out.println(roadNetwork.getNumberOfLanes()[edgeID]);
		System.out.println(roadNetwork.getEdgesType()[edgeID]);
		
		//TEST NODE GRAVITATING POPULATION
		System.out.println("\n\n*** Testing node gravitating population ***");

		System.out.println(roadNetwork.getNodeToGravitatingPopulation());

		//node 9 -> area code (population): E00087214(488), E00086616(416), E00087213(299), E00086589(324), E00087208(388)
		assertEquals("Gravitating population is correct", (488 + 416 + 299 + 324 + 388) , (int) roadNetwork.getGravitatingPopulation(9));
		
		System.out.println("Zone to sorted nodes: " + roadNetwork.getZoneToNodes());

		//find the node with maximum gravitating population
		for (String LAD: roadNetwork.getZoneToNodes().keySet()) {
			List<Integer> list = roadNetwork.getZoneToNodes().get(LAD);
			Iterator<Integer> iterator = (Iterator<Integer>) list.iterator();
			int maxPopulation = 0;
			Integer maxNode = null;
			while (iterator.hasNext()) {
				Integer node = iterator.next();
				int population = roadNetwork.getGravitatingPopulation(node);
				if (population >  maxPopulation) {
					maxPopulation = population;
					maxNode = node;
				}
			}
			assertEquals("Max gravity node is the first node in the sorted list of nodes", maxNode, roadNetwork.getZoneToNodes().get(LAD).get(0));
			assertEquals("Max gravitating population is correct", maxPopulation, (int) roadNetwork.getGravitatingPopulation(maxNode));
		}

		System.out.println("Node to average access/egress distance:");
		System.out.println(roadNetwork.getNodeToAverageAccessEgressDistance());
		
		double accessEgressFactor = Double.parseDouble(props.getProperty("ACCESS_EGRESS_LAD_DISTANCE_SCALING_FACTOR"));

		//node 9 -> area code (population): E00087214(488), E00086616(416), E00087213(299), E00086589(324), E00087208(388)
		assertEquals("Average access/egress distance is correct", (488 * 1086.68018 + 416 * 331.586619 + 299 * 968.4972698 + 324 * 125.6523948 + 388 * 1401.943333) * accessEgressFactor / (488 + 416 + 299 + 324 + 388) , (double) roadNetwork.getAverageAcessEgressDistance(9), 1e-5);

		//TEST SHORTEST PATH ALGORITHMS
		System.out.println("\n\n*** Testing the shortest path algorithms ***");

		System.out.println("The whole network: " + roadNetwork.toString());

		System.out.println("\n*** Dijkstra ***");
		//set source and destination node
		iter = rn.getNodes().iterator();
		Node from = null, to = null;
		while (iter.hasNext() && (from == null || to == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 86) from = node;
			if (node.getID() == 19) to = node;
		}

		//find the shortest path using Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		Path path = pathFinder.getPath(to);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		List listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(to));

		double sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		int[] expectedNodeList = new int[] {86, 87, 105, 95, 48, 19}; //node IDs are persistent
		int[] expectedEdgeList = new int[] {58, 93, 71, 64, 67}; //edge IDs are persistent after edge ID replacement
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(to), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(to), EPSILON);
		
		//test the fastest path method from the road network
		path = roadNetwork.getFastestPathDijkstra((DirectedNode)from, (DirectedNode)to, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());
		
		//reverse direction
		System.out.println("Source node: " + to.getID() + " | Destination node: " + from.getID());
		pathFinder = new DijkstraShortestPathFinder(rn, to, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		path = pathFinder.getPath(from);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(from));

		sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		expectedNodeList = new int[] {19, 48, 82, 95, 105, 87, 86}; //node IDs are persistent
		expectedEdgeList = new int[] {68, 90, 77, 72, 94, 59}; //edge IDs are persistent after edge ID replacement
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(from), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(from), EPSILON);

		//test the fastest path method from the road network
		path = roadNetwork.getFastestPathDijkstra((DirectedNode)to, (DirectedNode)from, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());
		
		//find the shortest path using Time Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		DijkstraShortestPathFinder pathTimeFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraTimeWeighter(roadNetwork.getFreeFlowTravelTime()));
		pathTimeFinder.calculate();
		Path path2 = pathTimeFinder.getPath(to);
		path2.reverse();
		System.out.println("The path as a list of nodes nodes: " + path2);
		listOfEdges = path2.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path2.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path time in min: %.3f\n", pathTimeFinder.getCost(to));
		
		path = roadNetwork.getFastestPathDijkstra((DirectedNode)from, (DirectedNode)to, roadNetwork.getFreeFlowTravelTime());
		assertTrue("Two paths are the same", path.equals(path2));
								
		System.out.println("\n*** AStar ***");

		//find the shortest path using AStar algorithm
		try {

			System.out.printf("Finding the shortest path from %d to %d using astar: \n", from.getID(), to.getID());

			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath;
			aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {86, 87, 105, 95, 48, 19}; //node IDs are persistent
			expectedEdgeList = new int[] {58, 93, 71, 64, 67}; //edge IDs are persistent after edge ID replacement
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Could not find the shortest path using astar.");
		}
			
		//test the AStar fastest path method from the road network
		path = roadNetwork.getFastestPath((DirectedNode)from, (DirectedNode)to, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());
	
		//reverse path
		try {
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, to, from, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {19, 48, 82, 95, 105, 87, 86}; //persistent
			expectedEdgeList = new int[] {68, 90, 77, 72, 94, 59}; //edge IDs are persistent after edge ID replacement
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//test the AStar fastest path method from the road network
		path = roadNetwork.getFastestPath((DirectedNode)to, (DirectedNode)from, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());

		//TEST ADDING NEW ROAD LINKS
		System.out.println("\n\n*** Testing addition of new links ***");

		Node fromNode = roadNetwork.getNodeIDtoNode()[86];
		Node toNode = roadNetwork.getNodeIDtoNode()[48];

		DirectedEdge newEdge = (DirectedEdge) roadNetwork.createNewRoadLink(fromNode, toNode, 2, 'A', 0.6);

		//find path from node 86 to node 19
		from = roadNetwork.getNodeIDtoNode()[86];
		to = roadNetwork.getNodeIDtoNode()[19];

		//find the shortest path using AStar algorithm
		try {
			System.out.printf("Finding the shortest path from %d to %d using astar: \n", from.getID(), to.getID());

			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath;
			aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {86, 48, 19}; //node IDs are persistent
			expectedEdgeList = new int[] {newEdge.getID(), 67}; //edge IDs are persistent after edge ID replacement
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 1.3, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Could not find the shortest path using astar.");
		}
		
		//test the AStar fastest path method from the road network
		path = roadNetwork.getFastestPath((DirectedNode)from, (DirectedNode)to, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());
	
		//find the shortest path using time-based Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		//pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraTimeWeighter());
		pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraTimeWeighter(null));
		pathFinder.calculate();
		path = pathFinder.getPath(to);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(to));

		sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			double time;
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') //motorway
				time = length / roadNetwork.getFreeFlowSpeedMRoad() * 60;  //travel time in minutes
			else if (roadNumber.charAt(0) == 'A') //A road
				time = length / roadNetwork.getFreeFlowSpeedARoad() * 60;  //travel time in minutes
			else if (roadNumber.charAt(0) == 'F') //ferry
				time = length / roadNetwork.getAverageSpeedFerry() * 60;  //travel time in minutes
			else { //unknown road type
				System.err.println("Uknown road type for link " + e.getID());
				time = Double.NaN;
			}
			System.out.println(time);
			sum += time;
		}
		System.out.printf("Sum of edge travel times: %.3f\n\n", sum);

		//compare with expected values
		expectedNodeList = new int[] {86, 48, 19}; //node IDs are persistent
		expectedEdgeList = new int[] {newEdge.getID(), 67}; //edge IDs are persistent after edge ID replacement
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path travel time equals the sum of edge travel times", sum, pathFinder.getCost(to), EPSILON);
		
		//test the fastest path method from the road network
		path = roadNetwork.getFastestPathDijkstra((DirectedNode)from, (DirectedNode)to, roadNetwork.getFreeFlowTravelTime());
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), path.getEdges().toString());
	}

	@Test
	public void test() throws IOException {

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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		DirectedGraph rn = roadNetwork.getNetwork();

		//TEST NODE AND EDGE CREATION
		System.out.println("\n\n*** Testing edge creation for duplicate edges ***");

		Iterator iter = rn.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 89) nodeA = node;
			if (node.getID() == 39) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 89, nodeA.getID());
		assertEquals("Node ID is correct", 39, nodeB.getID());

		//total edges between nodes 89 and 39
		List listOfEdges = nodeA.getEdges(nodeB);
		System.out.printf("The number of edges between node %d and node %d is %d (both directions).\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The number of edges is correct", 4, listOfEdges.size());

		//directed edges from node 89 to node 39
		listOfEdges = nodeA.getOutEdges(nodeB);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeA.getID(), nodeB.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The link is double", 2, listOfEdges.size()); //order is not persistent!
		DirectedEdge e1 = (DirectedEdge) listOfEdges.get(0);
		DirectedEdge e2 = (DirectedEdge) listOfEdges.get(1);
		System.out.println(e1);
		System.out.println(e2);
		SimpleFeature sf1 = (SimpleFeature) e1.getObject();
		SimpleFeature sf2 = (SimpleFeature) e2.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 70107L, sf1.getAttribute("CP")); //cannot guarantee order
		assertTrue("Edge CP is correct", sf1.getAttribute("CP").equals(70107L) || sf1.getAttribute("CP").equals(74474L));
		assertEquals("Edge direction is correct", "C", sf1.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf1.getAttribute("LenNet"));
		System.out.println(sf2.getAttribute("CP"));
		System.out.println(sf2.getAttribute("iDir"));
		System.out.println(sf2.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 74474L, sf2.getAttribute("CP")); //cannot guarantee order
		assertTrue("Edge CP is correct", sf2.getAttribute("CP").equals(70107L) || sf2.getAttribute("CP").equals(74474L));
		assertEquals("Edge direction is correct", "C", sf2.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf2.getAttribute("LenNet"));

		//directed edges from node 39 to node 89
		listOfEdges = nodeB.getOutEdges(nodeA);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The edge is double", 2, listOfEdges.size()); //order is not persistent!
		e1 = (DirectedEdge) listOfEdges.get(0);
		e2 = (DirectedEdge) listOfEdges.get(1);
		System.out.println(e1);
		System.out.println(e2);
		sf1 = (SimpleFeature) e1.getObject();
		sf2 = (SimpleFeature) e2.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 70107L, sf1.getAttribute("CP")); //order is not persistent!
		assertTrue("Edge CP is correct", sf1.getAttribute("CP").equals(70107L) || sf1.getAttribute("CP").equals(74474L));
		assertEquals("Edge direction is correct", "C", sf1.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf1.getAttribute("LenNet"));
		System.out.println(sf2.getAttribute("CP"));
		System.out.println(sf2.getAttribute("iDir"));
		System.out.println(sf2.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 74474L, sf2.getAttribute("CP")); //order is not persistent!
		assertTrue("Edge CP is correct", sf2.getAttribute("CP").equals(70107L) || sf2.getAttribute("CP").equals(74474L));
		assertEquals("Edge direction is correct", "C", sf2.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf2.getAttribute("LenNet"));

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 106) nodeA = node;
			if (node.getID() == 84) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 106, nodeA.getID());
		assertEquals("Node ID is correct", 84, nodeB.getID());

		//total edges between nodes 106 and 84
		listOfEdges = nodeA.getEdges(nodeB);
		System.out.printf("The number of edges between node %d and node %d is %d (both directions).\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The number of edges is correct", 3, listOfEdges.size());

		//directed edges from node 106 to node 84
		listOfEdges = nodeA.getOutEdges(nodeB);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeA.getID(), nodeB.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The edge is double", 2, listOfEdges.size());
		e1 = (DirectedEdge) listOfEdges.get(0);
		e2 = (DirectedEdge) listOfEdges.get(1);
		System.out.println(e1);
		System.out.println(e2);
		sf1 = (SimpleFeature) e1.getObject();
		sf2 = (SimpleFeature) e2.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 70084L, sf1.getAttribute("CP"));
		//assertEquals("Edge direction is correct", "C", sf1.getAttribute("iDir"));
		assertTrue("Edge CP and direction are correct", sf1.getAttribute("CP").equals(70084L) && sf1.getAttribute("iDir").equals("C") ||
				sf1.getAttribute("CP").equals(70083L) && sf1.getAttribute("iDir").equals("N"));
		assertEquals("Edge length is correct", 0.5, sf1.getAttribute("LenNet"));
		System.out.println(sf2.getAttribute("CP"));
		System.out.println(sf2.getAttribute("iDir"));
		System.out.println(sf2.getAttribute("LenNet"));
		//assertEquals("Edge CP is correct", 70083L, sf2.getAttribute("CP"));
		//assertEquals("Edge direction is correct", "N", sf2.getAttribute("iDir"));
		assertTrue("Edge CP and direction are correct", sf2.getAttribute("CP").equals(70084L) && sf2.getAttribute("iDir").equals("C") ||
				sf2.getAttribute("CP").equals(70083L) && sf2.getAttribute("iDir").equals("N"));
		assertEquals("Edge length is correct", 0.5, sf2.getAttribute("LenNet"));

		//just one edge from node 84 to node 106
		listOfEdges = nodeB.getOutEdges(nodeA);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The edge is single", 1, listOfEdges.size());
		e1 = (DirectedEdge) listOfEdges.get(0);
		System.out.println(e1);
		sf1 = (SimpleFeature) e1.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 70084L, sf1.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf1.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.5, sf1.getAttribute("LenNet"));

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 84) nodeA = node;
			if (node.getID() == 85) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 84, nodeA.getID());
		assertEquals("Node ID is correct", 85, nodeB.getID());

		//total edges between nodes 84 and 85
		listOfEdges = nodeA.getEdges(nodeB);
		System.out.printf("The number of edges between node %d and node %d is %d (both directions).\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The number of edges is correct", 2, listOfEdges.size());

		//just one edge from node 84 to node 85
		listOfEdges = nodeA.getOutEdges(nodeB);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeA.getID(), nodeB.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The edge is single", 1, listOfEdges.size());
		e1 = (DirectedEdge) listOfEdges.get(0);
		System.out.println(e1);
		sf1 = (SimpleFeature) e1.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 38210L, sf1.getAttribute("CP"));
		assertEquals("Edge direction is correct", "N", sf1.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.5, sf1.getAttribute("LenNet"));

		//just one edge from node 85 to node 84
		listOfEdges = nodeB.getOutEdges(nodeA);
		System.out.printf("The number of directed edges from node %d to node %d is %d.\n", nodeB.getID(), nodeA.getID(), listOfEdges.size());
		System.out.println("Edges: " + listOfEdges.toString());
		assertEquals("The edge is single", 1, listOfEdges.size());
		e1 = (DirectedEdge) listOfEdges.get(0);
		System.out.println(e1);
		sf1 = (SimpleFeature) e1.getObject();
		System.out.println(sf1.getAttribute("CP"));
		System.out.println(sf1.getAttribute("iDir"));
		System.out.println(sf1.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 70085L, sf1.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf1.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.5, sf1.getAttribute("LenNet"));

		//TEST NODE TO ZONE MAPPING
		System.out.println("\n\n*** Testing node to zone mapping ***");

		System.out.println("Nodes " + roadNetwork.getNetwork().getNodes());
		int nodeNumber = roadNetwork.getNetwork().getNodes().size();
		System.out.println("Nodes to zones mapping: " + roadNetwork.getNodeToZone());
		assertEquals("All nodes except node 76 are mapped", nodeNumber-1, roadNetwork.getNodeToZone().size());

		HashMap<String, List<Integer>> zoneToNodes = roadNetwork.getZoneToNodes();
		System.out.println("Zone to nodes mapping: " + zoneToNodes);
		System.out.println("The number of nodes in each zone:");
		for (String key: zoneToNodes.keySet()) {
			System.out.println(key + ": " + zoneToNodes.get(key).size());
		}
		boolean condition = 
				zoneToNodes.get("E07000091").size() == 38 &&
				zoneToNodes.get("E06000045").size() == 34 &&
				zoneToNodes.get("E06000046").size() == 31 &&
				zoneToNodes.get("E07000086").size() == 18;
		assertTrue("The number of nodes in each zone is correct", condition);

		Integer[] array = (Integer[]) zoneToNodes.get("E07000091").toArray(new Integer[0]);
		Integer[] expectedArray1 = {3, 4, 5, 6,	13,	14, 15,	16,	20,	21,	22,	23,	39,	41,	42,	43,	44,	56,	57,	62,	63,	64,	73,	77,	78,	79,	88,	89,	90,	91,	92,	97,	110, 111, 112, 117, 120, 122};
		Arrays.sort(array);
		Arrays.sort(expectedArray1);
		assertTrue("Nodes are correctly mapped to zone E07000091", Arrays.equals(expectedArray1, array));

		array = (Integer[]) zoneToNodes.get("E06000045").toArray(new Integer[0]);
		Integer[] expectedArray2 = {1, 2, 7, 8,	9, 10, 11, 12, 19, 26, 27, 30, 31, 37, 40, 45, 46, 47, 48, 55, 58, 59, 60, 61, 68, 82, 83, 86, 87, 95, 96, 102, 105, 109};
		Arrays.sort(array);
		Arrays.sort(expectedArray2);
		assertTrue("Nodes are correctly mapped to zone E06000045", Arrays.equals(expectedArray2, array));

		array = (Integer[]) zoneToNodes.get("E06000046").toArray(new Integer[0]);
		Integer[] expectedArray3 = {24, 25,	28,	29,	49, 50,	51,	52,	53,	54,	65,	66,	67,	71,	72,	80,	81,	84,	85,	93,	99,	100, 101, 103, 106,	113, 114, 115, 116, 118, 119};
		Arrays.sort(array);
		Arrays.sort(expectedArray3);
		assertTrue("Nodes are correctly mapped to zone E06000046", Arrays.equals(expectedArray3, array));

		array = (Integer[]) zoneToNodes.get("E07000086").toArray(new Integer[0]);
		Integer[] expectedArray4 = {17,	18,	32,	33,	34, 35,	36,	38,	69, 70,	74,	75, 94,	98,	104, 107, 108, 121};
		Arrays.sort(array);
		Arrays.sort(expectedArray4);
		assertTrue("Nodes are correctly mapped to zone E07000086", Arrays.equals(expectedArray4, array));
		
		//TEST EDGE TO ZONE MAPPING
		
		System.out.println("\n\n*** Testing edge to zone mapping ***");
		HashMap<Integer, String> map = roadNetwork.getEdgeToZone();
		
		for (Integer edgeID: map.keySet())
			System.out.printf("Edge %d is in zone %s %n", edgeID, map.get(edgeID));
	
		assertEquals("Edge is correctly mapped to zone", "E07000091", map.get(537));
		assertEquals("Edge is correctly mapped to zone", "E06000046", map.get(610));
		assertEquals("Edge is correctly mapped to zone", "E06000045", map.get(701));
		assertEquals("Edge is correctly mapped to zone", "E07000086", map.get(752));
		
		//TEST NODE GRAVITATING POPULATION
		System.out.println("\n\n*** Testing node gravitating population ***");

		//System.out.println("Node to gravitating population: \n" + roadNetwork.getNodeToGravitatingPopulation());

		//node 60 -> area codes (population): E00086593(281), E00086587(402), E00086591(389), E00086592(290), E00086627(294)
		assertEquals("Gravitating population is correct", (281 + 402 + 389 + 290 + 294) , (int) roadNetwork.getGravitatingPopulation(60));

		System.out.println("Zone to sorted nodes: " + roadNetwork.getZoneToNodes());

		//find the node with maximum gravitating population
		for (String LAD: roadNetwork.getZoneToNodes().keySet()) {
			List<Integer> list = roadNetwork.getZoneToNodes().get(LAD);
			Iterator<Integer> iterator = (Iterator<Integer>) list.iterator();
			int maxPopulation = 0;
			Integer maxNode = null;
			while (iterator.hasNext()) {
				Integer node = iterator.next();
				int population = roadNetwork.getGravitatingPopulation(node);
				if (population >  maxPopulation) {
					maxPopulation = population;
					maxNode = node;
				}
			}
			assertEquals("Max gravity node is the first node in the sorted list of nodes", maxNode, roadNetwork.getZoneToNodes().get(LAD).get(0));
			assertEquals("Max gravitating population is correct", maxPopulation, (int) roadNetwork.getGravitatingPopulation(maxNode));
		}		

		//TEST NODE BLACKLISTS
		iter = rn.getNodes().iterator();
		while (iter.hasNext()) {
			DirectedNode node = (DirectedNode) iter.next();
			System.out.printf("Node %d has %d in degree and %d out degree. \n", node.getID(), node.getInDegree(), node.getOutDegree());
			System.out.printf("Blacklisted as start node is %b, blacklisted as end node is %b \n", roadNetwork.isBlacklistedAsStartNode(node.getID()), roadNetwork.isBlacklistedAsEndNode(node.getID()));		
		}

		//TEST NUMBER OF LANES
		System.out.println("\n\n*** Testing the number of lanes ***");

		iter = rn.getEdges().iterator();
		while (iter.hasNext()) {
			DirectedEdge edge = (DirectedEdge) iter.next();
			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			String roadNumber = (String) sf.getAttribute("RoadNumber");
			if (roadNumber.charAt(0) == 'M') {//motorway
				assertEquals("The number of lanes is correct for the road type", roadNetwork.getNumberOfLanesMRoad(), roadNetwork.getNumberOfLanes()[edge.getID()]);
			} else if (roadNumber.charAt(0) == 'A') {//A road
				assertEquals("The number of lanes is correct for the road type", roadNetwork.getNumberOfLanesARoad(), roadNetwork.getNumberOfLanes()[edge.getID()]);
			} else {//ferry
				assertEquals("The number of lanes for ferries is 0", 0, roadNetwork.getNumberOfLanes()[edge.getID()]);
			}
		}
		
		//TEST WHETHER EDGES ARE URBAN OR RURAL
		System.out.println("\n\n*** Testing if the edges are urban or rural ***");
		System.out.println(roadNetwork.getIsEdgeUrban());
		
		Edge edge = roadNetwork.getEdgeIDtoEdge()[541];
		SimpleFeature sfcat = (SimpleFeature) edge.getObject();
		String cat = (String) sfcat.getAttribute("RCat"); //road category (PM, PR, Pu, PU, TM, TR, Tu, TU), use Pu, PU, Tu, TU as urban, otherwise rural

		System.out.println("Cat = " + cat);
		System.out.println("Second char = " + cat.toUpperCase().charAt(1));
		if (cat.toUpperCase().charAt(1) == 'U')
			System.out.println("Edge is urban!");
		if (cat.toUpperCase().charAt(1) == 'R')
			System.out.println("Edge is rural!");
		
		System.out.println("should be false: " + roadNetwork.getIsEdgeUrban()[541]);
		
		assertTrue("Edge is not urban", !roadNetwork.getIsEdgeUrban()[541]);
		assertTrue("Edge is not urban", !roadNetwork.getIsEdgeUrban()[540]);
		
		assertTrue("Edge is urban", roadNetwork.getIsEdgeUrban()[616]);
		assertTrue("Edge is urban", roadNetwork.getIsEdgeUrban()[615]);
		
		assertNull("Edge is neither urban nor rural", roadNetwork.getIsEdgeUrban()[555]);
		assertNull("Edge is neither urban nor rural", roadNetwork.getIsEdgeUrban()[556]);
		
		//TEST WHETHER EDGE IS FERRY
		assertTrue("Edge is ferry", roadNetwork.getEdgesType()[555] == EdgeType.FERRY);
		assertTrue("Edge is ferry", roadNetwork.getEdgesType()[556] == EdgeType.FERRY);
		assertTrue("Edge is not ferry", roadNetwork.getEdgesType()[541] != EdgeType.FERRY);
		assertTrue("Edge is not ferry", roadNetwork.getEdgesType()[540] != EdgeType.FERRY);
		
		//TEST EDGE TO OTHER DIRECTION EDGE MAPPING
		System.out.println("\n\n*** Testing edge to other direction edge mapping ***");

		System.out.println(roadNetwork.getEdgeIDtoEdge());
		System.out.println(roadNetwork.getEdgeIDtoOtherDirectionEdgeID());

		//TEST PATH CREATION FROM A LIST OF NODES
		System.out.println("\n\n*** Testing path creation from a list of nodes using RoadPath***");

		Path cPath = new RoadPath();
		DirectedNode node1 = (DirectedNode) roadNetwork.getNodeIDtoNode()[6];
		DirectedNode node2 = (DirectedNode) roadNetwork.getNodeIDtoNode()[89];
		DirectedNode node3 = (DirectedNode) roadNetwork.getNodeIDtoNode()[39];
		cPath.add(node1);
		cPath.add(node2);
		cPath.add(node3);
		System.out.println(cPath);
		System.out.println("Is path valid? " + cPath.isValid());
		assertTrue("Path 6 -> 89 -> 39 should be valid", cPath.isValid());

		ArrayList<Node> pathList = new ArrayList<Node>();
		//48 -> 95
		pathList.add((DirectedNode) roadNetwork.getNodeIDtoNode()[48]);
		pathList.add((DirectedNode) roadNetwork.getNodeIDtoNode()[95]);
		cPath = new RoadPath(pathList);
		System.out.println(cPath);
		System.out.println("Is path valid? " + cPath.isValid()); //this path should not be valid (GeoTools Path.isValid method ignores edge direction!)
		assertTrue("Path 48 -> 95 should not be valid", !cPath.isValid());

		System.out.println("Edges: " + cPath.getEdges());
		System.out.println("First node: " + cPath.getFirst());
		System.out.println("Last node: " + cPath.getLast());
		//DirectedEdge edge = (DirectedEdge) cPath.getEdges().get(0);
		//System.out.println("Directed edge: " + edge);
		org.junit.Assert.assertNull("There are no edges for invalid path", cPath.getEdges());

		cPath.reverse();
		System.out.println(cPath);
		System.out.println("Is path valid? " + cPath.isValid());
		assertTrue("Path 95 -> 48 should be valid", cPath.isValid());

		//48 -> 82 ->95
		pathList.clear();
		pathList.add((DirectedNode) roadNetwork.getNodeIDtoNode()[48]);
		pathList.add((DirectedNode) roadNetwork.getNodeIDtoNode()[82]);
		pathList.add((DirectedNode) roadNetwork.getNodeIDtoNode()[95]);
		cPath = new RoadPath(pathList);
		System.out.println(cPath);
		System.out.println("Is path valid? " + cPath.isValid());
		assertTrue("Path 48 -> 82 -> 95 should be valid", cPath.isValid());
		cPath.reverse();
		System.out.println(cPath);
		System.out.println("Is path valid? " + cPath.isValid());
		assertTrue("Path 95 -> 82 -> 48 should not be valid", !cPath.isValid());

		//TEST EDGE ID REPLACEMENT
		System.out.println("\n\n*** Testing edge ID replacement ***");

		System.out.println("Directed graph representation of the road network before replacement:");
		System.out.println(roadNetwork.getNetwork());
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		System.out.println("Directed graph representation of the road network after replacement:");
		System.out.println(roadNetwork.getNetwork());

		//TEST SHORTEST PATH ALGORITHMS
		System.out.println("\n\n*** Testing the shortest path algorithms ***");

		System.out.println("The whole network: " + roadNetwork.toString());

		System.out.println("\n*** Dijkstra ***");
		//set source and destination node
		iter = rn.getNodes().iterator();
		Node from = null, to = null;
		while (iter.hasNext() && (from == null || to == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 83) from = node;
			if (node.getID() == 31) to = node;
		}

		//find the shortest path using Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		Path path = pathFinder.getPath(to);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(to));

		double sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			SimpleFeature sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		int[] expectedNodeList = new int[] {83, 82, 95, 105, 87, 86, 102, 30, 31}; //node IDs are persistent
		int[] expectedEdgeList = new int[] {700, 789, 538, 770, 628, 784, 679, 774}; //edge IDs are persistent after edge ID replacement
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(to), EPSILON);
		assertEquals("The shortest path length is correct", 8.8, pathFinder.getCost(to), EPSILON);
		
		//export to shapefile
		roadNetwork.exportToShapefile("./temp/testNetwork.shp");
		SimpleFeatureType sf = roadNetwork.createCustomFeatureType("DayVolume");
		System.out.println(sf.getType("DayVolume"));
		System.out.println(sf.indexOf("DayVolume"));
		
		double[] freeFlowTime = roadNetwork.getFreeFlowTravelTime();
		HashMap<Integer, Double> travelTimeMap = new HashMap<Integer, Double>();
		for (int i=1; i < freeFlowTime.length; i++)
			travelTimeMap.put(i, freeFlowTime[i]);
				
		SimpleFeatureCollection sfc = roadNetwork.createNetworkFeatureCollection(travelTimeMap, "FreeFlow", "./temp/features.shp");
		System.out.println(sfc.getSchema());
	}

	//@Test
	public void fullTest() throws IOException {

		final URL zonesUrl = new URL("file://src/main/resources/data/zones.shp");
		final URL networkUrl = new URL("file://src/main/resources/data/network.shp");
		final URL nodesUrl = new URL("file://src/main/resources/data/nodes.shp");
		final URL AADFurl = new URL("file://src/main/resources/data/AADFdirected2015.shp");

		final String areaCodeFileName = "./src/main/resources/data/population_OA_GB.csv";
		final String areaCodeNearestNodeFile = "./src/main/resources/data/nearest_node_OA_GB.csv";
		final String workplaceZoneFileName = "./src/main/resources/data/workplacePopulationFakeSC.csv";
		final String workplaceZoneNearestNodeFile = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
		final String freightZoneToLADfile = "./src/main/resources/data/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/main/resources/data/freightZoneToNearestNode.csv";
		
		final String assignmentParamsFile = "./src/test/resources/testdata/assignment.properties";
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(assignmentParamsFile);
			// load properties file
			props.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl,  areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		DirectedGraph rn = roadNetwork.getNetwork();

		//TEST NODE AND EDGE CREATION
		System.out.println("\n\n*** Testing node and edge creation ***");

		Iterator iter = rn.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 5923) nodeA = node;
			if (node.getID() == 4443) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 5923, nodeA.getID());
		assertEquals("Node ID is correct", 4443, nodeB.getID());
		DirectedEdge edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		DirectedEdge edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		SimpleFeature sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "W", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "E", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 6, nodeA.getDegree());
		assertEquals("Node out degree is correct", 3, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 3, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 6, nodeB.getDegree());
		assertEquals("Node out degree is correct", 3, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 3, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 9522) nodeA = node;
			if (node.getID() == 11540) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 9522, nodeA.getID());
		assertEquals("Node ID is correct", 11540, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 10611) nodeA = node;
			if (node.getID() == 5548) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 10611, nodeA.getID());
		assertEquals("Node ID is correct", 5548, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48513L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "N", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 9350) nodeA = node;
			if (node.getID() == 5548) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 9350, nodeA.getID());
		assertEquals("Node ID is correct", 5548, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		assertNull("Expecting no edge in this direction", edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48317L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 9350) nodeA = node;
			if (node.getID() == 10611) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 9350, nodeA.getID());
		assertEquals("Node ID is correct", 10611, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48456L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.1, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);


		//test that there are no nodes with outdegree 0, because that means it is not possible to go anywhere from this node!
		//also check that there are no nodes with indegree 0, because that means it is not possible to reach this node!

		iter = rn.getNodes().iterator();
		while (iter.hasNext()) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getInDegree() == 0) System.err.printf("Node %d has in degree 0!\n", node.getID());
			if (node.getOutDegree() == 0) System.err.printf("Node %d has out degree 0!\n", node.getID());
		}

		//TEST NODE TO ZONE MAPPING
		System.out.println("\n\n*** Testing node to zone mapping ***");

		System.out.println("Nodes " + roadNetwork.getNetwork().getNodes());
		int nodeNumber = roadNetwork.getNetwork().getNodes().size();
		System.out.println("Nodes to zones mapping: " + roadNetwork.getNodeToZone());
		assertEquals("All nodes except 68 nodes are mapped", nodeNumber-68, roadNetwork.getNodeToZone().size());

		HashMap<String, List<Integer>> zoneToNodes = roadNetwork.getZoneToNodes();
		System.out.println("Zone to nodes mapping: " + zoneToNodes);
		System.out.println("The number of nodes in each zone:");
		for (String key: zoneToNodes.keySet()) {
			System.out.println(key + ": " + zoneToNodes.get(key).size());
		}
		boolean condition = 
				zoneToNodes.get("E07000091").size() == 39 &&
				zoneToNodes.get("E06000045").size() == 36 &&
				zoneToNodes.get("E06000046").size() == 31 &&
				zoneToNodes.get("E07000086").size() == 18;
		assertTrue("The number of nodes in each zone is correct", condition);

		Integer[] array = (Integer[]) zoneToNodes.get("E06000046").toArray(new Integer[0]);
		Integer[] expectedArray3 = {2398, 5561, 2399, 12155, 7878, 12156, 7871, 5564, 7879, 12490, 5563, 11165, 5806, 10429, 9520, 9519, 11545, 11408, 11346, 8185, 11307, 5807, 12463, 5562, 9348, 2384, 8184, 2383, 12158, 12157, 9349};
		Arrays.sort(array);
		Arrays.sort(expectedArray3);
		assertTrue("Nodes are correctly mapped to zone E06000046", Arrays.equals(expectedArray3, array));

		//System.out.println(roadNetwork.getAreaCodeToNearestNode());
		//System.out.println(roadNetwork.getAreaCodeToPopulation());
		//System.out.println(roadNetwork.getWorkplaceZoneToNearestNode());
		//System.out.println(roadNetwork.getWorkplaceCodeToPopulation());

		System.out.println("Zone to output area mapping: ");
		System.out.println(roadNetwork.getZoneToAreaCodes());
		System.out.println("Output areas in zone E06000046: ");
		System.out.println(roadNetwork.getZoneToAreaCodes().get("E06000046"));

		System.out.println("Zone to workplace zone mapping: ");
		System.out.println(roadNetwork.getZoneToWorkplaceCodes());
		System.out.println("Workplace zones in zone E06000046: ");
		System.out.println(roadNetwork.getZoneToWorkplaceCodes().get("E06000046"));

		//TEST NODE GRAVITATING POPULATION
		System.out.println("\n\n*** Testing node gravitating population ***");

		//System.out.println("Node to gravitating population: \n" + roadNetwork.getNodeToGravitatingPopulation());

		//node 7058 -> area codes (population): E00086587(402), E00086591(389), E00086592(290)
		assertEquals("Gravitating population is correct", (402 + 389 + 290) , (int) roadNetwork.getGravitatingPopulation(7058));

		System.out.println("Zone to sorted nodes: " + roadNetwork.getZoneToNodes());

		//find the node with maximum gravitating population
		for (String LAD: roadNetwork.getZoneToNodes().keySet()) {
			List<Integer> list = roadNetwork.getZoneToNodes().get(LAD);
			Iterator<Integer> iterator = (Iterator<Integer>) list.iterator();
			int maxPopulation = 0;
			Integer maxNode = null;
			while (iterator.hasNext()) {
				Integer node = iterator.next();
				Integer population = roadNetwork.getGravitatingPopulation(node);
				if (population >  maxPopulation) {
					maxPopulation = population;
					maxNode = node;
				}
			}
			assertEquals("Max gravity node is the first node in the sorted list of nodes", maxNode, roadNetwork.getZoneToNodes().get(LAD).get(0));
			assertEquals("Max gravitating population is correct", maxPopulation, (int) roadNetwork.getGravitatingPopulation(maxNode));
		}

		//TEST NODE BLACKLISTS
		System.out.println("\n\n*** Testing node blacklists ***");

		System.out.println("Start node blacklist : " + roadNetwork.getStartNodeBlacklist());
		System.out.println("End node blacklist : " + roadNetwork.getEndNodeBlacklist());

		iter = rn.getNodes().iterator();
		while (iter.hasNext()) {
			DirectedNode node = (DirectedNode) iter.next();
			//			System.out.printf("Node %d has %d in degree and %d out degree. \n", node.getID(), node.getInDegree(), node.getOutDegree());
			//			System.out.printf("Blacklisted as start node is %b, blacklisted as end node is %b \n", roadNetwork.isBlacklistedAsStartNode(node.getID()), roadNetwork.isBlacklistedAsEndNode(node.getID()));		

			//if node is blacklisted as either start or end node, check if it has any gravitating population, as this could potentially create problems in path finding
			if (roadNetwork.isBlacklistedAsStartNode(node.getID()) || roadNetwork.isBlacklistedAsEndNode(node.getID())) {
				if (roadNetwork.getGravitatingPopulation(node.getID()) > 0) 
					System.err.printf("Blacklisted node %d has gravitating population!\n", node.getID());
				if (roadNetwork.getGravitatingWorkplacePopulation(node.getID()) > 0) 
					System.err.printf("Blacklisted node %d has gravitating workplace population!\n", node.getID());
			}
		}

		//TEST SHORTEST PATH ALGORITHMS
		System.out.println("\n\n*** Testing the shortest path algorithms ***");

		System.out.println("The whole network: " + roadNetwork.toString());

		System.out.println("\n*** Dijkstra ***");
		//set source and destination node
		iter = rn.getNodes().iterator();
		Node from = null, to = null;
		while (iter.hasNext() && (from == null || to == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 9521) from = node;
			if (node.getID() == 1643) to = node;
		}

		//find the shortest path using Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		Path path = pathFinder.getPath(to);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		List listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(to));

		double sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		int[] expectedNodeList = new int[] {9521, 9522, 11540, 10611, 5548, 1643}; //node IDs are persistent
		int[] expectedEdgeList = new int[] {81, 61, 67, 74, 77}; //cannot check as edge IDs are not persistent
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		//assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(to), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(to), EPSILON);

		//reverse direction
		System.out.println("Source node: " + to.getID() + " | Destination node: " + from.getID());
		pathFinder = new DijkstraShortestPathFinder(rn, to, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		path = pathFinder.getPath(from);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(from));

		sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		expectedNodeList = new int[] {1643, 5548, 9350, 10611, 11540, 9522, 9521}; //node IDs are persistent
		expectedEdgeList = new int[] {78, 60, 69, 68, 62, 82}; //cannot check as edge IDs are not persistent
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		//assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(from), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(from), EPSILON);

		System.out.println("\n*** AStar ***");

		//find the shortest path using AStar algorithm
		try {
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath;
			aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {9521, 9522, 11540, 10611, 5548, 1643}; //node IDs are persistent
			expectedEdgeList = new int[] {81, 61, 67, 74, 77}; //cannot check as edge IDs are not persistent
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			//assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
		}

		//reverse path
		try {
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, to, from, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {1643, 5548, 9350, 10611, 11540, 9522, 9521}; //persistent
			expectedEdgeList = new int[] {78, 60, 69, 68, 62, 82}; //not persistent
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			//assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
