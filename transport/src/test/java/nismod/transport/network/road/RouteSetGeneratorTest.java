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
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;

public class RouteSetGeneratorTest {

	public static void main(String[] args) throws IOException {

		//log error
		//FileOutputStream f = new FileOutputStream("err.txt");
		//System.setErr(new PrintStream(f));

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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		roadNetwork.makeEdgesAdmissible();
		
		//load base year matrices
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		FreightMatrix fm = new FreightMatrix(baseYearFreightMatrixFile);
		
		odm.printMatrixFormatted("Passenger OD matrix:");

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);

		long timeNow = System.currentTimeMillis();
		rsg.generateRouteSetForODMatrix(odm);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Routes generated in %d seconds.\n", timeNow / 1000);
		
		rsg.printStatistics();
		rsg.printChoiceSets();
		//rsg.saveRoutesBinary("passengerRoutes.dat", false);
		
		rsg.clearRoutes();
		
		timeNow = System.currentTimeMillis();
		rsg.generateRouteSetForFreightMatrix(fm, 5);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Freight routes generated in %d seconds.\n", timeNow / 1000);
		
		rsg.printStatistics();
		rsg.printChoiceSets();
		rsg.saveRoutesBinary("miniFreightRoutes.dat", false);
					
		//timeNow = System.currentTimeMillis();
		//rsg.readRoutesBinaryWithoutValidityCheck("./src/main/resources/data/freightRoutes/freightRoutes.dat");
		//timeNow = System.currentTimeMillis() - timeNow;
		//System.out.printf("Routes loaded into memory in %d seconds.\n", timeNow / 1000);


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
		
			
		//f.flush();
		//f.close();
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		DirectedGraph rn = roadNetwork.getNetwork();
		ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);

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

		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		roadNetwork.makeEdgesAdmissible();
		roadNetwork.sortGravityNodesFreight(); //must!
		
		System.out.println("Start node blacklist: " + roadNetwork.getStartNodeBlacklist());
		System.out.println("End node blacklist: " + roadNetwork.getEndNodeBlacklist());

		System.out.println(roadNetwork.getZoneToNodes());
			
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
	
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, 
															InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile).get(BASE_YEAR),
															InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR),
															InputFileReader.readAVFractionsFile(AVFractionsFile).get(BASE_YEAR),
															InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile),
															InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile),
															InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile).get(BASE_YEAR),
															null,
															null,
															null,
															null,
															props);
		
		FreightMatrix fm = new FreightMatrix(baseYearFreightMatrixFile);
		
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
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, null, props);

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
		rna.assignPassengerFlowsRouting(odm, null);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
	}
}
