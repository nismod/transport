package nismod.transport.network.road;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.io.FileUtils;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

public class RouteSetGeneratorTest {

	public static void main(String[] args) throws IOException {

		//log error
		FileOutputStream f = new FileOutputStream("err.txt");
		System.setErr(new PrintStream(f));

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";

		//create a road network
		//RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		//RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
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


		/*

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlNew);

		RoadNetworkAssignment rna2 = new RoadNetworkAssignment(roadNetwork2, null, null, null, null, null);

		//export to shapefile
		//roadNetwork2.exportToShapefile("testOutputNetwork");

		//visualise the shapefiles
		//roadNetwork2.visualise("Midi Test Area");

		ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");

		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork2);
		routes.generateRouteSetWithLinkElimination(passengerODM, 2);
		routes.saveRoutes("testRoutes.txt", false);
		routes.printChoiceSets();
		routes.printStatistics();

		 */

		/*

		Route r1 = new Route();
		Route r2 = new Route();
		Route r3 = new Route();
		Route r4 = new Route();

		DirectedNode n1 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(7);
		DirectedNode n2 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(8);
		DirectedNode n3 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(27);
		DirectedNode n4 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(9);
		DirectedNode n5 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(55);
		DirectedNode n6 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(40);

		DirectedEdge e1 = (DirectedEdge) n1.getOutEdge(n2);
		DirectedEdge e2 = (DirectedEdge) n2.getOutEdge(n4);
		DirectedEdge e3 = (DirectedEdge) n4.getOutEdge(n6);
		DirectedEdge e4 = (DirectedEdge) n4.getOutEdge(n5);
		DirectedEdge e5 = (DirectedEdge) n5.getOutEdge(n6);
		DirectedEdge e6 = (DirectedEdge) n1.getOutEdge(n3);
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n1);

		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);
		r1.calculateTravelTime(rna2.getLinkTravelTimes());
		r1.calculateLength();
		r1.calculateUtility();

		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		r2.calculateTravelTime(rna2.getLinkTravelTimes());
		r2.calculateLength();
		r2.calculateUtility();

		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e3);
		r3.calculateTravelTime(rna2.getLinkTravelTimes());
		r3.calculateLength();
		r3.calculateUtility();

		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e4);
		r4.addEdge(e5);
		r4.calculateTravelTime(rna2.getLinkTravelTimes());
		r4.calculateLength();
		r4.calculateUtility();

		DirectedNode originNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(7);
		DirectedNode destinationNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(40);

		RouteSet rs = new RouteSet(originNode, destinationNode);
		rs.addRoute(r1);
		rs.addRoute(r2);
		rs.addRoute(r3);
		rs.addRoute(r4);

		rs.sortRoutesOnUtility();
		rs.printChoiceSet();
		rs.calculateProbabilities();

		int[] choiceFrequency = new int[4];

		for (int i=0; i<1000; i++) {

			Route chosenRoute = rs.choose();
			int choiceIndex = rs.getIndexOfRoute(chosenRoute);
			choiceFrequency[choiceIndex]++;
		}

		System.out.println("Choice frequencies: ");
		System.out.println(Arrays.toString(choiceFrequency));

		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork2);

		routes.addRoute(r1);
		routes.addRoute(r2);
		routes.addRoute(r3);
		routes.addRoute(r4);

		routes.printChoiceSets();
		//routes.saveRoutes("routes.txt", false);

//		RouteSetGenerator routes2 = new RouteSetGenerator(roadNetwork2);
//		routes2.generateRouteSetWithLinkElimination(7, 40);
//		routes2.generateRouteSetWithLinkElimination(40, 7);
//		routes2.generateRouteSetWithLinkElimination(7, 8);
//		routes2.generateRouteSetWithLinkElimination(7, 7);
//		routes2.printChoiceSets();
//		
		RouteSetGenerator routes3 = new RouteSetGenerator(roadNetwork2);
		//routes3.readRoutes("routes.txt");
//		routes3.readRoutes("routesSouthamptonToNewForest.txt");
//		routes3.printStatistics();
//		routes3.printChoiceSets();
		routes3.clearRoutes();
		routes3.generateRouteSetWithLinkElimination("E06000045", "E07000091", 3);
		//routes3.saveRoutes("routesSouthamptonToNewForestTop3Nodes.txt", false);
		routes3.printStatistics();
		routes3.printChoiceSets();
//		
//		RouteSetGenerator routes4 = new RouteSetGenerator(roadNetwork);
//		routes4.generateRouteSetWithLinkElimination("E06000045", "E06000045");
//		routes4.printStatistics();
//		routes4.printChoiceSets();


		 */
		final URL zonesUrl3 = new URL("file://src/main/resources/data/zones.shp");
		final URL networkUrl3 = new URL("file://src/main/resources/data/network.shp");
		final URL networkUrl3New = new URL("file://src/main/resources/data/fullNetworkWithEdgeIDs.shp");
		final URL nodesUrl3 = new URL("file://src/main/resources/data/nodes.shp");
		final URL AADFurl3 = new URL("file://src/main/resources/data/AADFdirected2015.shp");

		final String areaCodeFileName3 = "./src/main/resources/data/population_OA_GB.csv";
		final String areaCodeNearestNodeFile3 = "./src/main/resources/data/nearest_node_OA_GB.csv";
		final String workplaceZoneFileName3 = "./src/main/resources/data/workplacePopulationFakeSC.csv";
		final String workplaceZoneNearestNodeFile3 = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
		final String freightZoneToLADfile3 = "./src/main/resources/data/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile3 = "./src/main/resources/data/freightZoneToNearestNode.csv";

		//create a road network
		RoadNetwork roadNetwork3 = new RoadNetwork(zonesUrl3, networkUrl3, nodesUrl3, AADFurl3, areaCodeFileName3, areaCodeNearestNodeFile3, workplaceZoneFileName3, workplaceZoneNearestNodeFile3, freightZoneToLADfile3, freightZoneNearestNodeFile3, props);
		roadNetwork3.replaceNetworkEdgeIDs(networkUrl3New);
		
		roadNetwork3.sortGravityNodes();

		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");

		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");

		//roadNetwork3.exportToShapefile("fullNetworkWithEdgeIDs");

		//RoadNetworkAssignment roadNetworkAssignment = new RoadNetworkAssignment(roadNetwork3, null, null, null, null, null);

		//ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMfull.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMtempro.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/balancedODMatrixOldLengths.csv");

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator routes5 = new RouteSetGenerator(roadNetwork3, params);
		//routes5.generateRouteSetWithLinkElimination(19, 79);
		//routes5.generateRouteSetWithLinkElimination("E06000046", "E06000046");
		//routes5.generateRouteSetWithLinkElimination("E06000045", "E06000046", 3);
		//routes5.generateRouteSetWithLinkElimination(passengerODM, 3);
		//routes5.saveRoutes("completeRoutesNewest.txt", false);
		//routes5.printStatistics();
		//routes5.printChoiceSets();

		//routes5.readRoutes("completeRoutesNewest.txt");
		//routes5.readRoutes("./src/main/resources/data/routes5of190top10.txt");
		
		long timeNow = System.currentTimeMillis();

		/*
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        //return name.endsWith(".txt");
		    	return name.endsWith(".dat");
		    }
		};

		//File folder = new File("./src/main/resources/data/routes");
		File folder = new File("./src/main/resources/data/freightRoutes");
		File[] listOfFiles = folder.listFiles(filter);

		for (int i = 0; i < listOfFiles.length; i++) {
		    File file = listOfFiles[i];
		    if (file.isFile() && file.getName().endsWith("top10.dat")) {
		    	System.out.print(i + ":");
		    	routes5.readRoutesBinary(file.getPath());
		    }
		}
		*/
		
		//routes5.readRoutes("./src/main/resources/data/all5routestop10/all5routestop10.txt");
		//routes5.readRoutes("./src/main/resources/data/routesCombined/routesCombined.txt");
		//routes5.readRoutesBinary("./src/main/resources/data/routesCombined/routesCombined.dat");
		//routes5.readRoutesBinaryWithoutValidityCheck("./src/main/resources/data/routesCombined/routesCombined.dat");
	
		//routes5.readRoutesBinary("./src/main/resources/data/freightRoutes/freightRoutes130of176top10.dat");
		//routes5.readRoutesBinaryWithoutValidityCheck("./src/main/resources/data/freightRoutes/freightRoutes26of176top10.dat");
		//routes5.readRoutesBinary("./src/main/resources/data/freightRoutes/freightRoutes130and134.dat");
		//routes5.readRoutesBinary("./src/main/resources/data/freightRoutes/freightRoutes.dat");
		routes5.readRoutesBinaryWithoutValidityCheck("./src/main/resources/data/freightRoutes/freightRoutes.dat");
		
		
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Routes loaded into memory in %d seconds.\n", timeNow / 1000);

		routes5.printStatistics();
		//routes5.printChoiceSets();
		
		//routes5.saveRoutesBinary("routesCombined.dat", false);
	
//		FreightMatrix freightMatrix = new FreightMatrix("./src/main/resources/data/freightMatrix.csv");	
//		routes5.generateRouteSetForFreightMatrix(freightMatrix, 10);
//		routes5.saveRoutes("freightRoutes.txt", false);
		
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Freight routes generated in %d seconds.\n", timeNow / 1000);
		
		f.flush();
		f.close();
	}

	@Test
	public void test() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		
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
		DirectedGraph rn = roadNetwork.getNetwork();
		ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(83, 31);
		rsg.printChoiceSets();
		rsg.printStatistics();
		
		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork, params);

		/*
		//routes.generateRouteSetWithLinkElimination(7, 40);
		//routes.generateRouteSetWithLinkElimination("E06000045", "E07000091", 2);
		routes.generateRouteSetWithLinkElimination(passengerODM, 3);
		routes.saveRoutes("testRoutes.txt", false);
		routes.printChoiceSets();
		routes.printStatistics();
		routes.clearRoutes();
		routes.readRoutes("testRoutes.txt");
		routes.printChoiceSets();
		routes.printStatistics();
		 */
		
		routes.generateRouteSetWithRandomLinkEliminationRestricted(116, 106);
		routes.printChoiceSets();
		routes.printStatistics();
		
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 10);
		routes.printChoiceSets();
		routes.printStatistics();
		
		//generate all route sets
		routes.clearRoutes();
		
		//has to be sorted!
		roadNetwork.sortGravityNodes();
		
		routes.generateRouteSetForODMatrix(passengerODM, 1, 1, 3);
		routes.printStatistics();
		int totalRouteSets = routes.getNumberOfRouteSets();
		int totalRoutes = routes.getNumberOfRoutes();

		//generate first slice
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 1, 3, 3);
		routes.printStatistics();
		int routeSets1 = routes.getNumberOfRouteSets();
		int routes1 = routes.getNumberOfRoutes();

		//generate second slice
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 2, 3, 3);
		routes.printStatistics();
		int routeSets2 = routes.getNumberOfRouteSets();
		int routes2 = routes.getNumberOfRoutes();

		//generate third slice
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 3, 3, 3);
		routes.printStatistics();
		int routeSets3 = routes.getNumberOfRouteSets();
		int routes3 = routes.getNumberOfRoutes();

		System.out.printf("%d route sets, %d routes \n", totalRouteSets, totalRoutes);
		System.out.printf("%d route sets, %d routes \n", routeSets1, routes1);
		System.out.printf("%d route sets, %d routes \n", routeSets2, routes2);
		System.out.printf("%d route sets, %d routes \n", routeSets3, routes3);
		System.out.printf("Total routes of all the slices: %d \n", routes1 + routes2 + routes3);

		assertEquals("The sum of route sets generated across OD matrix slices is equal to the total number of route sets", totalRouteSets, routeSets1 + routeSets2 + routeSets3);
		//however, due to randomness in the link elimination generation, the total number of routes will typically not be the same!
		//assertEquals("The sum of routes generated across OD matrix slices is equal to the total", totalRoutes, routes1 + routes2 + routes3);
		
		rsg.printChoiceSets();
		rsg.saveRoutes("testRoutesASCII.txt",  false);
		rsg.saveRoutesBinary("testRoutesBinary.dat",  false);
				
		RouteSetGenerator rsg2 = new RouteSetGenerator(roadNetwork, params);
		//rsg2.readRoutes("testRoutesASCII.txt");
		rsg2.readRoutesWithoutValidityCheck("testRoutesASCII.txt");
		rsg2.printChoiceSets();
		
		rsg2.clearRoutes();
		//rsg2.readRoutesBinary("testRoutesBinary.dat");
		rsg2.readRoutesBinaryWithoutValidityCheck("testRoutesBinary.dat");
		rsg2.printChoiceSets();
		
		routes.saveRoutes("testRoutesASCII.txt", false);
		routes.saveRoutesBinary("testRoutesBinary.dat", false);
	}

	@Test
	public void miniTest() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL networkUrlfixedEdgeIDs = new URL("file://src/test/resources/minitestdata/miniOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/minitestdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/minitestdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/test/resources/minitestdata/passengerODM.csv";
		
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
		roadNetwork.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);	
		roadNetwork.makeEdgesAdmissible();

		//TEST ROUTE SET GENERATION
		System.out.println("\n\n*** Testing route set generation ***");

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		//rsg.generateRouteSet(odm);
		//rsg.generateRouteSet(31, 82);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(31, 82);
		rsg.printChoiceSets();
		rsg.printStatistics();
		
		HashMap<Integer, Double> linkTravelTimes = new HashMap<Integer, Double>();
		DirectedNode node1 = (DirectedNode)roadNetwork.getNodeIDtoNode().get(48);
		DirectedNode node2 = (DirectedNode)roadNetwork.getNodeIDtoNode().get(82);
		DirectedEdge edge = (DirectedEdge) node1.getOutEdge(node2);
		System.out.println(edge.getID());
		
		//linkTravelTimes.put(edge.getID(), Double.POSITIVE_INFINITY); //setting maximum travel time does not block or aStar!
		roadNetwork.removeRoadLink(edge);

		RoadPath rp = roadNetwork.getFastestPath((DirectedNode)roadNetwork.getNodeIDtoNode().get(31), 
				(DirectedNode)roadNetwork.getNodeIDtoNode().get(82),
				linkTravelTimes);

		if (rp != null) {
			System.out.println(rp);
			System.out.println("Is it valid: " + rp.isValid());
			System.out.println(rp.buildEdges());
			Route route = new Route(rp);
			System.out.println(route.isValid());
			System.out.println(route.getFormattedString());
		} else {
			System.err.println("Could not find the shortest path using astar.");
		}
		
		assertNull("RoadPath should be null after removing important edge", rp);
			
		rsg.printChoiceSets();
		//remove routes
		List<Route> removedRoutes = new ArrayList<Route>();
		rsg.removeRoutesWithEdge(90, removedRoutes);
		rsg.printChoiceSets();
		System.out.println("Removed routes:");
		System.out.println(removedRoutes);
		assertTrue("Choice set should be empty", rsg.getRouteSet(31, 82).getChoiceSet().isEmpty());
	}
	
	@Test
	public void testFreight() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String freightMatrixFile = "./src/test/resources/testdata/freightMatrix.csv";
		
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
		
		roadNetwork.makeEdgesAdmissible();
		roadNetwork.sortGravityNodesFreight(); //must!
		
		System.out.println("Start node blacklist: " + roadNetwork.getStartNodeBlacklist());
		System.out.println("End node blacklist: " + roadNetwork.getEndNodeBlacklist());

		System.out.println(roadNetwork.getZoneToNodes());
		
		
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, props);

		FreightMatrix fm = new FreightMatrix(freightMatrixFile);
		
		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(48, 67);
		//rsg.printChoiceSets();
		rsg.printStatistics();
		
		rsg.clearRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 10);
		//rsg.printChoiceSets();
		rsg.printStatistics();

		System.out.println("Origins: " + fm.getOrigins().size());
		System.out.println(fm.getOrigins());
		System.out.println("Destinations: " + fm.getDestinations().size());
		System.out.println(fm.getDestinations());
		System.out.println("Vehicles: " + fm.getVehicleTypes().size());
		System.out.println(fm.getVehicleTypes());
			
		rsg.clearRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 1, 1, 10);
		rsg.printStatistics();
		int totalRouteSets = rsg.getNumberOfRouteSets();
		int totalRoutes = rsg.getNumberOfRoutes();

		//generate first slice
		rsg.clearRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 1, 3, 10);
		rsg.printStatistics();
				
		//generate second slice
		rsg.generateRouteSetForFreightMatrix(fm, 2, 3, 10);
		rsg.printStatistics();
				
		//generate third slice
		rsg.generateRouteSetForFreightMatrix(fm, 3, 3, 10);
		rsg.printStatistics();
		int totalRouteSetsFromSlices = rsg.getNumberOfRouteSets();
		int totalRoutesFromSlices = rsg.getNumberOfRoutes();
		
				
		System.out.printf("%d route sets, %d routes \n", totalRouteSets, totalRoutes);
		System.out.printf("%d route sets from slices, %d routes from slices \n", totalRouteSetsFromSlices, totalRoutesFromSlices);
		
		rsg.printChoiceSets();
		
		assertEquals("The number of route sets generated across freight matrix slices is equal to the total number of route sets", totalRouteSets, totalRouteSetsFromSlices);
		
		//set route choice parameters
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");

		//assign freight flows
		rna.assignFreightFlowsRouteChoice(fm, rsg, params);
	
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforFreightCounts());
		
		//rna.saveAssignmentResults(2015, "testAssignmentResultsWithFreight.csv");
	
	}
	
	//@Test
	public void fullTest() throws IOException {

		final URL zonesUrl = new URL("file://src/main/resources/data/zones.shp");
		final URL networkUrl = new URL("file://src/main/resources/data/network.shp");
		final URL networkUrlFixedEdges = new URL("file://src/main/resources/data/fullNetworkWithEdgeIDs.shp");
		final URL nodesUrl = new URL("file://src/main/resources/data/nodes.shp");
		final URL AADFurl = new URL("file://src/main/resources/data/AADFdirected2015.shp");
		final String areaCodeFileName = "./src/main/resources/data/population_OA_GB.csv";
		final String areaCodeNearestNodeFile = "./src/main/resources/data/nearest_node_OA_GB.csv";
		final String workplaceZoneFileName = "./src/main/resources/data/workplacePopulationFakeSC.csv";
		final String workplaceZoneNearestNodeFile = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
		final String freightZoneToLADfile = "./src/main/resources/data/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/main/resources/data/freightZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/main/resources/data/balancedODMatrix.csv";
		
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
		
		DirectedNode  node1 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(7293);
		DirectedNode  node2 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(12175);
		
		System.out.println("Before replacement:");
		System.out.printf("Node %d out edges: %s \n", node1.getID(), node1.getOutEdges());
		System.out.printf("Node %d edge to node %d: %s \n", node1.getID(), node2.getID(), node1.getEdge(node2));
		System.out.printf("Node %d out edge to node %d: %s \n", node1.getID(), node2.getID(), node1.getOutEdge(node2));
		System.out.printf("Node %d edges to node %d: %s \n", node1.getID(), node2.getID(), node1.getEdges(node2));
		
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdges);
		roadNetwork.makeEdgesAdmissible();
		
		System.out.println("After replacement:");
		System.out.printf("Node %d out edges: %s \n", node1.getID(), node1.getOutEdges());
		System.out.printf("Node %d edge to node %d: %s \n", node1.getID(), node2.getID(), node1.getEdge(node2));
		System.out.printf("Node %d out edge to node %d: %s \n", node1.getID(), node2.getID(), node1.getOutEdge(node2));
		System.out.printf("Node %d edges to node %d: %s \n", node1.getID(), node2.getID(), node1.getEdges(node2));

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, props);

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		//rsg.generateRouteSet(odm);
		//rsg.generateRouteSet(31, 82);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(7293, 11913);
		rsg.printChoiceSets();
		rsg.printStatistics();

		rsg.clearRoutes();
		rsg.generateRouteSetZoneToZone("E07000202", "E07000203", 10);
		
		//assign passenger flows
		ODMatrix odm = new ODMatrix();
		odm.setFlow("E07000202", "E07000203", 1000000);
		odm.printMatrixFormatted();

		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		long timeNow = System.currentTimeMillis();
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned with route choice in %d seconds.\n", timeNow / 1000);
		
		timeNow = System.currentTimeMillis();
		rna.assignPassengerFlows(odm);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
	}
}
