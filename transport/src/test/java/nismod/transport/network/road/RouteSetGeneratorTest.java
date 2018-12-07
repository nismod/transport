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
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrixMultiKey;
import nismod.transport.demand.RealODMatrixTempro;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

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
			
		System.out.println("Edge Ids:");
		System.out.println(Arrays.toString(roadNetwork.getEdgeIDtoEdge()));
				
		//load base year matrices
		ODMatrixMultiKey odm = new ODMatrixMultiKey(baseYearODMatrixFile);
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
		//rsg.printChoiceSets();
		
		rsg.saveRoutesBinary("miniFreightRoutes.dat", false);
		rsg.saveRoutesBinaryShort("miniFreightRoutesShort.dat", false);
		
		rsg.clearRoutes();
		
		rsg.readRoutesBinaryShortWithoutValidityCheck("miniFreightRoutesShort.dat");
		rsg.printStatistics();
		
		rsg.clearRoutes();
		
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
		ODMatrixMultiKey passengerODM = new ODMatrixMultiKey(baseYearODMatrixFile);

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		params.setProperty("INITIAL_OUTER_CAPACITY", "163");
		params.setProperty("INITIAL_INNER_CAPACITY", "163");
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
		
		routes.generateRouteSetWithLinkElimination(116, 106);
		routes.printStatistics();
		routes.clearRoutes();
		
		routes.generateRouteSetWithRandomLinkEliminationRestricted(116, 106);
		routes.printChoiceSets();
		routes.printStatistics();
		
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 10);
		//routes.printChoiceSets();
		routes.printStatistics();
		
		routes.clearRoutes();
		routes.generateRouteSetForODMatrix(passengerODM, 1, 3);
		routes.printStatistics();
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
		rsg.saveRoutes("./temp/testRoutesASCII.txt",  false);
		rsg.saveRoutesBinary("./temp/testRoutesBinary.dat",  false);
				
		RouteSetGenerator rsg2 = new RouteSetGenerator(roadNetwork, params);
		rsg2.readRoutes("./temp/testRoutesASCII.txt");
		int ns1 = rsg2.getNumberOfRouteSets();
		int nr1 = rsg2.getNumberOfRoutes();
		
		rsg2.clearRoutes();
		rsg2.readRoutesWithoutValidityCheck("./temp/testRoutesASCII.txt");
		int ns2 = rsg2.getNumberOfRouteSets();
		int nr2 = rsg2.getNumberOfRoutes();
		
		assertEquals("Number of route sets is the same", ns1, ns2);
		assertEquals("Number of route is the same", nr1, nr2);
		
		rsg2.clearRoutes();
		rsg2.readRoutesBinary("./temp/testRoutesBinary.dat");
		ns1 = rsg2.getNumberOfRouteSets();
		nr1 = rsg2.getNumberOfRoutes();
		
		rsg2.clearRoutes();
		rsg2.readRoutesBinaryWithoutValidityCheck("./temp/testRoutesBinary.dat");
		ns2 = rsg2.getNumberOfRouteSets();
		nr2 = rsg2.getNumberOfRoutes();
		
		assertEquals("Number of route sets is the same", ns1, ns2);
		assertEquals("Number of route is the same", nr1, nr2);
		
		rsg2.clearRoutes();
		rsg2.readRoutesWithoutValidityCheck("./temp/testRoutesASCII.txt");
		ns2 = rsg2.getNumberOfRouteSets();
		nr2 = rsg2.getNumberOfRoutes();
		
		assertEquals("Number of route sets is the same", ns1, ns2);
		assertEquals("Number of route is the same", nr1, nr2);
		
		rsg2.saveRoutesBinaryShort("./temp/testRoutesBinaryShort.dat", false);
		rsg2.clearRoutes();
		rsg2.readRoutesBinaryShortWithoutValidityCheck("./temp/testRoutesBinaryShort.dat");
		int ns3 = rsg2.getNumberOfRouteSets();
		int nr3 = rsg2.getNumberOfRoutes();
				
		assertEquals("Number of route sets is the same", ns3, ns2);
		assertEquals("Number of route is the same", nr3, nr2);
		
		routes.saveRoutes("./temp/testRoutesASCII.txt", false);
		routes.saveRoutesBinary("./temp/testRoutesBinary.dat", false);
		
		//add single node routes
		rsg2.clearRoutes();
		rsg2.generateSingleNodeRoutes();
		rsg2.printStatistics();
		//rsg2.printChoiceSets();
		assertEquals("The number of single node routes should equal the number of nodes in the graph", roadNetwork.getNetwork().getNodes().size(), rsg2.getNumberOfRoutes());
		
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		rsg2.clearRoutes();
		
		ODMatrixMultiKey tempro1 = new ODMatrixMultiKey();
		tempro1.setFlow("E02004800", "E02003552", 1);
		tempro1.setFlow("E02004800", "E02003553", 2);
		tempro1.setFlow("E02004801", "E02003552", 3);
		tempro1.setFlow("E02004801", "E02003553", 4);
		rsg2.generateRouteSetForODMatrixTempro(tempro1, zoning);
		rsg2.printStatistics();
		
		RealODMatrixTempro tempro = new RealODMatrixTempro(zoning);
		tempro.setFlow("E02004800", "E02003552", 1.0);
		tempro.setFlow("E02004800", "E02003553", 2.0);
		tempro.setFlow("E02004801", "E02003552", 3.0);
		tempro.setFlow("E02004801", "E02003553", 4.0);
			
		rsg2.clearRoutes();
		rsg2.generateRouteSetForODMatrixTempro(tempro, zoning, 1, 1);
		rsg2.printStatistics();
		
		rsg2.clearRoutes();
		rsg2.generateRouteSetForODMatrixTemproDistanceBased(tempro, zoning, 1, 1);
		rsg2.printStatistics();
		
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		rsg.clearRoutes();
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.printStatistics();
		
		rsg.saveRoutesBinaryGZIPped("./temp/passengerRoutes.dat.gz", false);
		
		rsg2.clearRoutes();
		rsg2.readRoutesBinaryGZIPpedWithoutValidityCheck("./temp/passengerRoutes.dat.gz");
		rsg2.printStatistics();
		
		ns1 = rsg.getNumberOfRouteSets();
		nr1 = rsg.getNumberOfRoutes();
		ns2 = rsg2.getNumberOfRouteSets();
		nr2 = rsg2.getNumberOfRoutes();
		assertEquals("Number of route sets is the same", ns1, ns2);
		assertEquals("Number of route is the same", nr1, nr2);
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
		params.setProperty("INITIAL_OUTER_CAPACITY", "23");
		params.setProperty("INITIAL_INNER_CAPACITY", "23");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		//rsg.generateRouteSet(odm);
		//rsg.generateRouteSet(31, 82);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(31, 82);
		rsg.printChoiceSets();
		rsg.printStatistics();
		
		int maximumEdgeID = Integer.parseInt(props.getProperty("MAXIMUM_EDGE_ID"));
		double[] linkTravelTimes = new double[maximumEdgeID];
		DirectedNode node1 = (DirectedNode)roadNetwork.getNodeIDtoNode()[48];
		DirectedNode node2 = (DirectedNode)roadNetwork.getNodeIDtoNode()[82];
		DirectedEdge edge = (DirectedEdge) node1.getOutEdge(node2);
		System.out.println(edge.getID());
		
		//linkTravelTimes.put(edge.getID(), Double.POSITIVE_INFINITY); //setting maximum travel time does not block or aStar!
		roadNetwork.removeRoadLink(edge);

		RoadPath rp = roadNetwork.getFastestPath((DirectedNode)roadNetwork.getNodeIDtoNode()[31], 
				(DirectedNode)roadNetwork.getNodeIDtoNode()[82],
				linkTravelTimes);

		if (rp != null) {
			System.out.println(rp);
			System.out.println("Is it valid: " + rp.isValid());
			System.out.println(rp.buildEdges());
			Route route = new Route(rp, roadNetwork);
			System.out.println(route.isValid());
			System.out.println(route.getFormattedString());
		} else {
			System.err.println("Could not find the shortest path using astar.");
		}
		
		assertNull("RoadPath should be null after removing important edge", rp);
			
		//rsg.printChoiceSets(); //this will not work as routes are not consistent with the roadNetwork after edge removal
		
		//remove routes
		List<Route> removedRoutes = new ArrayList<Route>();
		rsg.removeRoutesWithEdge(90, removedRoutes);
		rsg.printChoiceSets();
		
		System.out.println("Removed routes:");
		for (Route r: removedRoutes)
			System.out.println(r.getFormattedStringEdgeIDsOnly());
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		roadNetwork.makeEdgesAdmissible();
		roadNetwork.sortGravityNodesFreight(); //must!
		
		System.out.println("Start node blacklist: " + roadNetwork.getStartNodeBlacklist());
		System.out.println("End node blacklist: " + roadNetwork.getEndNodeBlacklist());

		System.out.println(roadNetwork.getZoneToNodes());
			
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String timeOfDayDistributionFreightFile = props.getProperty("timeOfDayDistributionFreightFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final String relativeFuelEfficiencyFile = props.getProperty("relativeFuelEfficiencyFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
	
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork,
															zoning,
															InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile).get(BASE_YEAR),
															InputFileReader.readUnitCO2EmissionFile(unitCO2EmissionsFile).get(BASE_YEAR),
															InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR),
															InputFileReader.readAVFractionsFile(AVFractionsFile).get(BASE_YEAR),
															InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile),
															InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile),
															InputFileReader.readRelativeFuelEfficiencyFile(relativeFuelEfficiencyFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile).get(BASE_YEAR),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFreightFile).get(BASE_YEAR),
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
		params.setProperty("INITIAL_OUTER_CAPACITY", "163");
		params.setProperty("INITIAL_INNER_CAPACITY", "163");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetWithRandomLinkEliminationRestricted(48, 67);
		//rsg.printChoiceSets();
		rsg.printStatistics();
		
		rsg.clearRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 10);
		//rsg.printChoiceSets();
		rsg.printStatistics();

		System.out.println("Origins: " + fm.getSortedOrigins().size());
		System.out.println(fm.getSortedOrigins());
		System.out.println("Destinations: " + fm.getSortedDestinations().size());
		System.out.println(fm.getSortedDestinations());
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
		
		assertEquals("The number of route sets generated across freight matrix slices is equal to the total number of route sets", totalRouteSets, totalRouteSetsFromSlices);
		
		rsg.generateSingleNodeRoutes();
		rsg.calculateAllPathsizes();
		
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

		rsg.clearRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 1, 3);
		rsg.printStatistics();
		rsg.clearRoutes();
		rsg.generateRouteSetBetweenFreightZones(854, 855);
		rsg.printStatistics();
		rsg.generateRouteSetBetweenFreightZones(854, 855, 1);
		rsg.printStatistics();
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		DirectedNode  node1 = (DirectedNode) roadNetwork.getNodeIDtoNode()[7293];
		DirectedNode  node2 = (DirectedNode) roadNetwork.getNodeIDtoNode()[12175];
		
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
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, zoning, null, null, null, null, null, null, null, null, null, null, null, null, null, props);

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
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
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
		rna.assignPassengerFlowsRouting(odm, null, props);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
	}
}
