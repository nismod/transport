package nismod.transport.network.road;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.junit.Test;

public class RouteSetTest {

	@Test
	public void test() throws IOException	{

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		
		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlNew);
		
		//create routes
		Route r1 = new Route();
		Route r2 = new Route();
		Route r3 = new Route();
		Route r4 = new Route();
		
		DirectedNode n1 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(7);
		DirectedNode n2 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(8);
		DirectedNode n3 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(27);
		DirectedNode n4 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(9);
		DirectedNode n5 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(55);
		DirectedNode n6 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(40);
			
		DirectedEdge e1 = (DirectedEdge) n1.getOutEdge(n2);
		DirectedEdge e2 = (DirectedEdge) n2.getOutEdge(n4);
		DirectedEdge e3 = (DirectedEdge) n4.getOutEdge(n6);
		DirectedEdge e4 = (DirectedEdge) n4.getOutEdge(n5);
		DirectedEdge e5 = (DirectedEdge) n5.getOutEdge(n6);
		DirectedEdge e6 = (DirectedEdge) n1.getOutEdge(n3);
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n2);
		
		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);
		//r1.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
					
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		//r2.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e2);
		r3.addEdge(e3);
		//r3.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e2);
		r4.addEdge(e4);
		r4.addEdge(e5);
		//r4.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		DirectedNode originNode = (DirectedNode)roadNetwork.getNodeIDtoNode().get(7);
		DirectedNode destinationNode = (DirectedNode)roadNetwork.getNodeIDtoNode().get(40);
		
		RouteSet rs = new RouteSet(originNode, destinationNode);
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		HashMap<String, Double> consumption = new HashMap<String, Double>();
		consumption.put("A", 1.11932239320862);
		consumption.put("B", 0.0440047704089497);
		consumption.put("C", -0.0000813834474888197);
		consumption.put("D", 2.44908328418021E-06);
		double unitCost = 1.17;
		
		//rs.addRoute(r1);
		rs.addRoute(r4);
		rs.addRoute(r2);
		rs.addRoute(r3);
	
		rs.printChoiceSet();
		rs.printStatistics();
				
		//rs.calculateUtilities(roadNetwork.getFreeFlowTravelTime(), params);
		rs.setLinkTravelTime(roadNetwork.getFreeFlowTravelTime());
		rs.setParameters(params);
		rs.calculateUtilities(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null, params);
		rs.printUtilities();
	
		for (double utility: rs.getUtilities())
			assertThat(0.0, greaterThanOrEqualTo(utility)); //all utilities should be negative (or 0)
		
		rs.calculateProbabilities();
		rs.printProbabilities();
		
		//test that the sum of probabilities is 1.0
		final double EPSILON = 1e-11; //may fail for higher accuracy
		double probabilitySum = 0.0;
		for (double probability: rs.getProbabilities()) 
			probabilitySum += probability;
		
		System.out.printf("The sum of probabilites is: %.12f.%n", probabilitySum);
		assertEquals("The sum of probabilities is 1.0", 1.0, probabilitySum, EPSILON);
		
		rs.sortRoutesOnUtility();
		rs.printUtilities();
		rs.printProbabilities();

		for (double utility: rs.getUtilities())
			assertThat(0.0, greaterThanOrEqualTo(utility)); //all utilities should be negative (or 0)

		//test that the sum of probabilities is 1.0
		probabilitySum = 0.0;
		for (double probability: rs.getProbabilities()) 
			probabilitySum += probability;
		
		//check that probabilities are also sorted after sorting the utilities
		ArrayList<Double> sorted = new ArrayList<Double>(rs.getProbabilities());
		Collections.sort(sorted, Collections.reverseOrder());
		assertEquals ("Probabilities list is sorted", sorted, rs.getProbabilities());
		
		int[] choiceFrequency = new int[4];
		for (int i=0; i<1000; i++) {
			Route chosenRoute = rs.choose(null);
			int choiceIndex = rs.getIndexOfRoute(chosenRoute);
			choiceFrequency[choiceIndex]++;
		}
		
		System.out.println("Choice frequencies: ");
		System.out.println(Arrays.toString(choiceFrequency));
		
		RoadPath rp = roadNetwork.getFastestPath(n1, n6, null);
		System.out.println("Fastest path: " + rp);
		System.out.println("Edges: " + rp.getEdges());
		for (Object o: rp.getEdges()) {
			DirectedEdge e = (DirectedEdge) o;
			System.out.println(e.getInNode() + "->" + e.getOutNode());
		}
		Route newRoute = new Route(rp);
		System.out.println(newRoute.isValid());
		
		rs.addRoute(newRoute);
		rs.printChoiceSet();
		rs.printStatistics();
		//all routes need to have re-calculated utility and path size after the new route is added!
		rs.calculateUtilities(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null, params);
		rs.printUtilities();
		rs.calculateProbabilities(roadNetwork.getFreeFlowTravelTime(), params);
		rs.printProbabilities();
		rs.sortRoutesOnUtility();
		rs.printChoiceSet();
		rs.printUtilities();
		rs.printProbabilities();
		Route chosenRoute = rs.choose(params);
		System.out.println("Chosen route: " + chosenRoute.toString());
	}
}
