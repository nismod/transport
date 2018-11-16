package nismod.transport.zone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.graph.structure.Node;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the Zoning class.
 * @author Milan Lovric
  */
public class ZoningTest {

	public static void main(String[] args) throws IOException {

		
		final String configFile = "./src/main/full/config/config.properties";
		//final String configFile = "./src/test/config/testConfig.properties";
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
		final String freightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		final String outputFolder = props.getProperty("outputFolder");
		final String assignmentResultsFile = props.getProperty("assignmentResultsFile");
		
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                System.out.println("Output directory is created.");
	            } else {
	                System.err.println("Failed to create output directory.");
	            }
	        }

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Number of TEMPRO zones: " + zoning.getZoneToNearestNodeIDMap().size());
		System.out.println("Number of nodes: " + zoning.getNodeToZoneMap().size());
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		System.out.println(zoning.getZoneToNearestNodeIDMap().keySet());
		System.out.println("Nodes to zones in which they are located: " + zoning.getNodeToZoneMap());
		
		//check if any zones are assigned to nodes that belong to other zones (which is possible if they are closer to the centroid).
		int counter = 0;
		for (String zone: zoning.getZoneToNearestNodeIDMap().keySet()) {
			
			Integer nodeID = zoning.getZoneToNearestNodeIDMap().get(zone);
			String zoneOfNode = zoning.getNodeToZoneMap().get(nodeID);
			if (!zone.equals(zoneOfNode)) {
				counter++;
				System.out.printf("Zone %s is assigned to node %d which is located in zone %s. %n", zone, nodeID, zoneOfNode);
			}
		}
		System.out.println("Number of cross-assigned zones: " + counter);
		
		System.out.println("Tempro zone to LAD zone map: " + zoning.getZoneToLADMap());
		System.out.println("Tempro zone code to tempro zone ID map: " + zoning.getZoneCodeToIDMap());
		System.out.println("Tempro zone ID to tempro zone code map: " + zoning.getZoneIDToCodeMap());
		
		System.out.println(zoning.getZoneToSortedListOfNodeAndDistancePairs());
		System.out.println(zoning.getZoneToListOfContainedNodes());
		
		double[][] matrix = zoning.getZoneToNodeDistanceMatrix();
		System.out.println("matrix length: " + matrix.length);
		System.out.println("matrix width: " + matrix[0].length);
		System.out.println("number of tempro zones: " + zoning.getZoneIDToCodeMap().size());
		System.out.println("number of nodes: " + zoning.getNodeToZoneMap().size());
		
		
//		for (int i = 0; i < matrix.length; i++) {
//			for (int j = 0; j < matrix[i].length; j++)
//				System.out.print(matrix[i][j] + " ");
//			System.out.println();
//		}
		
//		System.out.println(zoning.getZoneToNodeMatrix());
//		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
//			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted(zone);
//		}
		
		//System.out.println("LAD to Tempro zone map: " + zoning.getLADToListOfContainedZones());
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		//test mapping for a few zones
		assertEquals("Zone E02004779 is mapped to the correct node", 79, zoning.getZoneToNearestNodeIDMap().get("E02004779").intValue());
		System.out.println("Distance: " + zoning.getZoneToNearestNodeDistanceMap().get("E02004779"));
		
		assertEquals("Zone E02004795 is mapped to the correct node", 63, zoning.getZoneToNearestNodeIDMap().get("E02004795").intValue());
		assertEquals("Zone E02003568 is mapped to the correct node", 30, zoning.getZoneToNearestNodeIDMap().get("E02003568").intValue());
		
		System.out.println(zoning.getZoneToSortedListOfNodeAndDistancePairs());
		
		System.out.println(zoning.getLADToListOfContainedZones());
		assertEquals("Tempro zone E02004794 is mapped to correct LAD zone", "E07000091", zoning.getZoneToLADMap().get("E02004794"));
		
		assertEquals("LAD zone Eastleigh contains correct number of Tempro zones", 15 ,zoning.getLADToListOfContainedZones().get("E07000086").size());
		assertEquals("LAD zone Isle of Wight contains correct number of Tempro zones", 18, zoning.getLADToListOfContainedZones().get("E06000046").size());
		assertEquals("LAD zone New Forest contains correct number of Tempro zones", 23, zoning.getLADToListOfContainedZones().get("E07000091").size());
		assertEquals("LAD zone Southampton contains correct number of Tempro zones", 32, zoning.getLADToListOfContainedZones().get("E06000045").size());
		
		System.out.println(zoning.getLADToName());
		System.out.println(zoning.getNodeToZoneMap());
		
		int size1 = roadNetwork.getNetwork().getNodes().size();
		int size2 = zoning.getNodeToZoneMap().size();
		assertEquals("All nodes from roadNetwork are mapped to Tempro zones", size1, size2);
		
		assertNull("Zone E02003593 contains no nodes", zoning.getZoneToListOfContainedNodes().get("E02003593"));
		
		List<Integer> listOfNodes = zoning.getZoneToListOfContainedNodes().get("E02003554");
		Collections.sort(listOfNodes);
		int[] expectedNodeList = new int[] {9, 40, 55};
		assertEquals("The list of nodes in the tempro zone is correct", Arrays.toString(expectedNodeList), listOfNodes.toString());
			
		Point centroid = zoning.getZoneToCentroid().get("E02003559");
		int nearestNodeID = zoning.getZoneToNearestNodeIDMap().get("E02003559");
		assertEquals("Nearest node ID is correct", 40, nearestNodeID);
		Node nearestNode = roadNetwork.getNodeIDtoNode()[nearestNodeID];
		SimpleFeature sfn = (SimpleFeature) nearestNode.getObject();
		Point point = (Point) sfn.getDefaultGeometry();
		double DELTA = 0.000001;
		double distance = zoning.getZoneToNearestNodeDistanceMap().get("E02003559");
		double expectedDistance = centroid.distance(point);
		assertEquals("Distance to nearest node is correct", expectedDistance, distance, DELTA);
		
		int zoneID = zoning.getZoneCodeToIDMap().get("E02003559");
		double distance2 = zoning.getZoneToNodeDistanceMatrix()[zoneID-1][nearestNodeID-1];
		assertEquals("Distance to nearest node is correct", expectedDistance, distance2, DELTA);
	
		Pair<Integer, Double> pair = zoning.getZoneToSortedListOfNodeAndDistancePairs().get("E02003559").get(0);
		assertEquals("Nearest node ID is correct", 40, (int)pair.getLeft());
		assertEquals("Distance to nearest node is correct", expectedDistance, (double)pair.getRight(), DELTA);

		//testing tempro zone mapping to the nearest node out of LAD top 5 nodes
		
		roadNetwork.sortGravityNodes();
		String originZone = "E02004779";
		String originLAD = zoning.getZoneToLADMap().get(originZone); //E07000091
		
		List<Integer> listOfOriginNodes = new ArrayList<Integer>(roadNetwork.getZoneToNodes().get(originLAD)); //the list is already sorted
		System.out.println(listOfOriginNodes);
		System.out.println(listOfOriginNodes.subList(0, 5));
		
		int[] expectedTopNodesList = new int[] {63, 117, 97, 6, 91};
		assertEquals("The list of top 5 nodes is correct", Arrays.toString(expectedTopNodesList), listOfOriginNodes.subList(0, 5).toString());
		
		//removing blacklisted nodes
		for (Integer on: roadNetwork.getZoneToNodes().get(originLAD))
			//check if any of the nodes is blacklisted
			if (roadNetwork.getStartNodeBlacklist().contains(on)) 
				listOfOriginNodes.remove(on);

		int originNode = 0;
		int interzonalTopNodes = 5;
		//make a choice based on the gravitating population size
		int originNodesToConsider = interzonalTopNodes<listOfOriginNodes.size()?interzonalTopNodes:listOfOriginNodes.size();

		//chose the node out of the top nodes in the LAD that is the closest to the tempro zone!
		double minDistance = Double.MAX_VALUE;
		for (int j=0; j<originNodesToConsider; j++) {
						
			Integer nodeID = listOfOriginNodes.get(j);
			int originZoneID = zoning.getZoneCodeToIDMap().get(originZone);
			double dist = zoning.getZoneToNodeDistanceMatrix()[originZoneID - 1][nodeID - 1];

			System.out.printf("Distance to node %d is %f %n", nodeID, dist);
			if (dist < minDistance) {
				minDistance = dist;
				originNode = nodeID;
			}
		}
		System.out.printf("Minimum distance is to node %d and it equals %f %n", originNode, minDistance);
		assertEquals("The closest node among the top nodes is correct", 6, originNode);

		assertEquals("The closest node among the top nodes is correct", 6, zoning.getZoneToNearestNodeIDFromLADTopNodesMap().get(originZone).intValue());
		
		//test zone to zone distances
		Set<Integer> zonesSet = zoning.getZoneIDToCodeMap().keySet();
		double [][] zoneDistances = zoning.getZoneToZoneDistanceMatrix();
		
		final double EPSILON = 1e-5;
		for (Integer zoneID1: zonesSet) {
			for (Integer zoneID2: zonesSet) {
				if (zoneID1 == zoneID2) assertEquals("Distance between the same zones should be zero", 0.0, zoneDistances[zoneID1-1][zoneID2-1], EPSILON);
				else assertEquals("Distance is a symetric measure", zoneDistances[zoneID2-1][zoneID1-1], zoneDistances[zoneID1-1][zoneID2-1], EPSILON);
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		
		//test mapping for a few zones
		assertEquals("Zone E02003560 is mapped to the correct node", 31, zoning.getZoneToNearestNodeIDMap().get("E02003560").intValue());
		System.out.println("Distance: " + zoning.getZoneToNearestNodeDistanceMap().get("E02003560"));
		
		assertEquals("Zone E02004795 is mapped to the correct node", 27, zoning.getZoneToNearestNodeIDMap().get("E02003561").intValue());
		assertEquals("Zone E02003568 is mapped to the correct node", 105, zoning.getZoneToNearestNodeIDMap().get("E02003580").intValue());
		
		System.out.println(zoning.getZoneToNearestNodeIDMap().keySet());
	}
}
