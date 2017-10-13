/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.closeTo;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.matcher.Matchers;

import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.SkimMatrix;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * Tests for the RoadNetworkAssignment class
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignmentTest {

	public static void main( String[] args ) throws IOException	{

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		
		final String areaCodeFileName = "./src/test/resources/minitestdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/minitestdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/minitestdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/minitestdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/minitestdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/minitestdata/freightZoneToNearestNode.csv";
		
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);

		//visualise the shapefiles
		//roadNetwork.visualise("Mini Test Area");
		
		//export to shapefile
		//roadNetwork.exportToShapefile("miniOutputNetwork");

//		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
//		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
//		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
//		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		
		final URL zonesUrl2 = new URL("file://src/main/resources/data/zones.shp");
		final URL networkUrl2 = new URL("file://src/main/resources/data/network.shp");
		final URL networkUrl2New = new URL("file://src/main/resources/data/fullNetworkWithEdgeIDs.shp");
		final URL nodesUrl2 = new URL("file://src/main/resources/data/nodes.shp");
		final URL AADFurl2 = new URL("file://src/main/resources/data/AADFdirected2015.shp");
		
		final String areaCodeFileName2 = "./src/main/resources/data/population_OA_GB.csv";
		final String areaCodeNearestNodeFile2 = "./src/main/resources/data/nearest_node_OA_GB.csv";
		final String workplaceZoneFileName2 = "./src/main/resources/data/workplacePopulationFakeSC.csv";
		final String workplaceZoneNearestNodeFile2 = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
		final String freightZoneToLADfile2 = "./src/main/resources/data/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile2 = "./src/main/resources/data/freightZoneToNearestNode.csv";

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName2, areaCodeNearestNodeFile2, workplaceZoneFileName2, workplaceZoneNearestNodeFile2, freightZoneToLADfile2, freightZoneNearestNodeFile2);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrl2New);
		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");
		
		RoadNetworkAssignment roadNetworkAssignment = new RoadNetworkAssignment(roadNetwork2, null, null, null, null, null);
		
		//ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMfull.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMtempro.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/balancedODMatrixOldLengths.csv");
		ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/balancedODMatrix.csv");
		passengerODM.printMatrix();

/*
		FreightMatrix freightMatrix = new FreightMatrix("./src/main/resources/data/freightMatrix.csv");	
		freightMatrix.printMatrixFormatted();
*/
		
		//read routes
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork2);
		//rsg.readRoutes("completeRoutesNewest.txt");
		rsg.readRoutes("./src/main/resources/data/all5routestop10/all5routestop10.txt");
		rsg.printStatistics();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		//assign passenger flows
		long timeNow = System.currentTimeMillis();
		roadNetworkAssignment.assignPassengerFlowsRouteChoice(passengerODM, rsg, params);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
/*
 		//assign freight flows
		timeNow = System.currentTimeMillis();
		roadNetworkAssignment.assignFreightFlows(freightMatrix);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Freight flows assigned in %d seconds.\n", timeNow / 1000);
		
		roadNetworkAssignment.saveAssignmentResults(2015, "assignment2015passengerAndFreigh.csv");
*/
		
//		//for (int i = 0; i < 5; i++) {
//		for (int i = 0; i < 1; i++) {
//			roadNetworkAssignment.resetLinkVolumes();
//			
//			long timeNow = System.currentTimeMillis();
//			roadNetworkAssignment.assignPassengerFlows(passengerODM);
//			timeNow = System.currentTimeMillis() - timeNow;
//			System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
//				
//			HashMap<Integer, Double> oldTravelTimes = roadNetworkAssignment.getCopyOfLinkTravelTimes();
//			roadNetworkAssignment.updateLinkTravelTimes(0.9);
//			System.out.println("Difference in link travel times: " + roadNetworkAssignment.getAbsoluteDifferenceInLinkTravelTimes(oldTravelTimes));
//		}		

		System.out.println("Nodes:");
		System.out.println(roadNetwork2.getNetwork().getNodes());
		System.out.println("Node to zone mapping: ");
		System.out.println(roadNetwork2.getNodeToZone());
		System.out.println("Zone to nodes mapping: ");
		System.out.println(roadNetwork2.getZoneToNodes());
		
		System.out.println("Path storage: ");
		//System.out.println(roadNetworkAssigment.getPathStorage());
		System.out.println(roadNetworkAssignment.getPathStorage().keySet());
		for (Object mk: roadNetworkAssignment.getPathStorage().keySet()) {
			System.out.println(mk);
			System.out.println("origin = " + ((MultiKey)mk).getKey(0));
			System.out.println("destination = " + ((MultiKey)mk).getKey(1));
			List list = (List) roadNetworkAssignment.getPathStorage().get((String)((MultiKey)mk).getKey(0), (String)((MultiKey)mk).getKey(1));
			System.out.println("number of paths = " + list.size());
		}
		
		System.out.println("Route storage: ");
		//System.out.println(roadNetworkAssigment.getPathStorage());
		System.out.println(roadNetworkAssignment.getRouteStorage().keySet());
		for (Object mk: roadNetworkAssignment.getRouteStorage().keySet()) {
			System.out.println(mk);
			System.out.println("origin = " + ((MultiKey)mk).getKey(0));
			System.out.println("destination = " + ((MultiKey)mk).getKey(1));
			List list = (List) roadNetworkAssignment.getRouteStorage().get((String)((MultiKey)mk).getKey(0), (String)((MultiKey)mk).getKey(1));
			System.out.println("number of paths = " + list.size());
		}
	
		
		System.out.println("Link volumes in PCU: ");
		System.out.println(roadNetworkAssignment.getLinkVolumesInPCU());	
		
		System.out.println("Free-flow travel times: ");
		System.out.println(roadNetworkAssignment.getLinkFreeFlowTravelTimes());	
		
		System.out.println("Travel times: ");
		System.out.println(roadNetworkAssignment.getLinkTravelTimes());	
		
		System.out.println("Time skim matrix:");
		SkimMatrix timeSkimMatrix = new SkimMatrix();
		roadNetworkAssignment.updateTimeSkimMatrix(timeSkimMatrix);
		timeSkimMatrix.printMatrixFormatted();
		
		System.out.println("Cost skim matrix:");
		roadNetworkAssignment.calculateCostSkimMatrix().printMatrixFormatted();
		
		System.out.println("Distance skim matrix:");
		SkimMatrix distanceSkimMatrix = roadNetworkAssignment.calculateDistanceSkimMatrix();
		distanceSkimMatrix.printMatrixFormatted();
		//distanceSkimMatrix.saveMatrixFormatted("distanceSkimMatrix.csv");
		
		System.out.println("Distance skim matrix from routes:");
		distanceSkimMatrix = roadNetworkAssignment.calculateDistanceSkimMatrixFromRoutes();
		distanceSkimMatrix.printMatrixFormatted();
		//distanceSkimMatrix.saveMatrixFormatted("distanceSkimMatrix.csv");
				
		System.out.println("Total energy consumptions:");
		System.out.println(roadNetworkAssignment.calculateEnergyConsumptions());
		
		System.out.println("Peak-hour link point capacities:");
		System.out.println(roadNetworkAssignment.calculatePeakLinkPointCapacities());
		
		System.out.println("Peak-hour link densities:");
		System.out.println(roadNetworkAssignment.calculatePeakLinkDensities());
		
		//roadNetworkAssignment.saveAssignmentResults(2015, "assignment2015balanced.csv");
		
		System.out.printf("RMSN for counts: %.2f%%", roadNetworkAssignment.calculateRMSNforCounts());
		System.out.printf("RMSN for freight: %.2f%%", roadNetworkAssignment.calculateRMSNforFreightCounts());
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
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);	
		
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);
		
		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlows(odm);
		
		//TEST OUTPUT AREA PROBABILITIES
		System.out.println("\n\n*** Testing output area probabilities ***");
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		//test the probability of one output area from each LAD
		assertEquals("The probability of the output area E00086552 is correct", (double)430/236882, rna.getAreaCodeProbabilities().get("E00086552"), EPSILON);

		//test that the sum of probabilities of output areas in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToAreaCodes().keySet()) {
			double probabilitySum = 0.0;
			for(Iterator<String> iter = roadNetwork.getZoneToAreaCodes().get(zone).iterator(); iter.hasNext(); ) {
				String areaCode = iter.next();
				probabilitySum += rna.getAreaCodeProbabilities().get(areaCode);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST ENERGY UNIT COSTS
		System.out.println("\n\n*** Testing the setter for the electricity unit cost ***");

		System.out.println("Energy unit costs:\t\t" + rna.getEnergyUnitCosts());
		System.out.println("Energy consumptions per 100 km:\t" + rna.getEnergyConsumptionsPer100km());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		rna.setEnergyUnitCost(RoadNetworkAssignment.EngineType.ELECTRICITY, 0.20);
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EngineType.ELECTRICITY), EPSILON);

		//TEST PATH STORAGE
		System.out.println("\n\n*** Testing path storage ***");

		//check that the number of paths for a given OD equals the flow (the number of trips in the OD matrix).
		System.out.println("Path storage: " + rna.getPathStorage());
		
		//for each OD
		for (MultiKey mk: odm.getKeySet()) {
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);
			List<Path> pathList = rna.getPathStorage().get(originZone, destinationZone);
			int flow = odm.getFlow(originZone, destinationZone);
			assertEquals("The number of paths equals the flow", flow, pathList.size());
		}

		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes()));

		//after assignment the link travel times should be greater or equal than the free flow travel times.
		rna.updateLinkTravelTimes();
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		for (int key: rna.getLinkTravelTimes().keySet()) {
			double actual = rna.getLinkTravelTimes().get(key);
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			//if freeFlow time is larger, it is only due to calculation error, so it has to be very close:
			if (freeFlow > actual)	assertThat(actual, closeTo(freeFlow, PRECISION));
		}
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
		
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		
		//create a new road network assignment
		//rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);
		
		rna.resetLinkVolumes();
		rna.resetPathStorages();
		rna.resetTripStartEndCounters();
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);
		rsg.generateRouteSet(odm);
		//rsg.generateRouteSet(31, 82);
		//rsg.generateRouteSetWithRandomLinkEliminationRestricted(31, 82);
		//rsg.printChoiceSets();
		System.out.println(rsg.getRouteSet(31, 82).getChoiceSet());

//		RoadPath rp = roadNetwork.getFastestPath((DirectedNode)roadNetwork.getNodeIDtoNode().get(31), 
//				(DirectedNode)roadNetwork.getNodeIDtoNode().get(82),
//				null);
//		
////		RoadPath rp = roadNetwork.getFastestPath((DirectedNode)roadNetwork.getNodeIDtoNode().get(31), 
////									(DirectedNode)roadNetwork.getNodeIDtoNode().get(82),
////									rna.getLinkTravelTimes());
//		System.out.println(rp);
//		System.out.println("Is it valid: " + rp.isValid());
//		System.out.println(rp.buildEdges());
//		Route route = new Route(rp);
//		System.out.println(route.isValid());
//		System.out.println(route.getFormattedString());
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
	}

	@Test
	public void test() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlfixedEdgeIDs = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlfixedEdgeIDs);
		
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlows(odm);
		
		//TEST OUTPUT AREA PROBABILITIES
		System.out.println("\n\n*** Testing output area probabilities ***");
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		//test the probability of one output area from each LAD
		assertEquals("The probability of the output area E00116864 is correct", (double)299/176462, rna.getAreaCodeProbabilities().get("E00116864"), EPSILON);
		assertEquals("The probability of the output area E00086552 is correct", (double)430/236882, rna.getAreaCodeProbabilities().get("E00086552"), EPSILON);
		assertEquals("The probability of the output area E00115160 is correct", (double)370/125199, rna.getAreaCodeProbabilities().get("E00115160"), EPSILON);
		assertEquals("The probability of the output area E00172724 is correct", (double)666/138265, rna.getAreaCodeProbabilities().get("E00172724"), EPSILON);

		//test that the sum of probabilities of output areas in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToAreaCodes().keySet()) {

			double probabilitySum = 0.0;
			for(Iterator<String> iter = roadNetwork.getZoneToAreaCodes().get(zone).iterator(); iter.hasNext(); ) {
				String areaCode = iter.next();
				probabilitySum += rna.getAreaCodeProbabilities().get(areaCode);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST NODE PROBABILITIES
		System.out.println("\n\n*** Testing node probabilities ***");
		
		//test the probability of one node from one LAD
		assertEquals("The probability of node 60 is correct", (double)1656/234671, rna.getNodeProbabilities().get(60), EPSILON);

		//test that the sum of probabilities of nodes in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {

			double probabilitySum = 0.0;
			for(Iterator<Integer> iter = roadNetwork.getZoneToNodes().get(zone).iterator(); iter.hasNext(); ) {
				Integer node = iter.next();
				probabilitySum += rna.getNodeProbabilities().get(node);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST ENERGY UNIT COSTS
		System.out.println("\n\n*** Testing the setter for the electricity unit cost ***");
		
		System.out.println("Energy unit costs:\t\t" + rna.getEnergyUnitCosts());
		System.out.println("Energy consumptions per 100 km:\t" + rna.getEnergyConsumptionsPer100km());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		
		rna.setEnergyUnitCost(RoadNetworkAssignment.EngineType.ELECTRICITY, 0.20);
		
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EngineType.ELECTRICITY), EPSILON);
		
		//TEST PATH STORAGE
		System.out.println("\n\n*** Testing path storage ***");
		
		double totalDistance = 0.0;
		//check that the number of paths for a given OD equals the flow (the number of trips in the OD matrix).
		rna.getPathStorage();
		//for each OD
		for (MultiKey mk: odm.getKeySet()) {
					//System.out.println(mk);
					String originZone = (String) mk.getKey(0);
					String destinationZone = (String) mk.getKey(1);
					List<Path> pathList = rna.getPathStorage().get(originZone, destinationZone);
					int flow = odm.getFlow(originZone, destinationZone);
					assertEquals("The number of paths equals the flow", flow, pathList.size());
					
					for (Path p: pathList) 
						for (Object e: p.getEdges())
							totalDistance += (double)((SimpleFeature)(((Edge)e).getObject())).getAttribute("LenNet");
		}
		System.out.println("Total distance = " + totalDistance);
		
		System.out.println("Distance skim matrix: ");
		rna.calculateDistanceSkimMatrix().printMatrixFormatted();
		
		System.out.println("Time skim matrix: ");
		rna.calculateTimeSkimMatrix().printMatrixFormatted();
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing trip starts/ends ***");
		
		System.out.println("Trip starts: " + rna.calculateLADTripStarts());
		System.out.println("Trip ends: " + rna.calculateLADTripEnds());
		System.out.println("OD matrix:");
		odm.printMatrixFormatted();
		System.out.println("Trip starts from OD matrix: " + odm.calculateTripStarts());
		System.out.println("Trip ends from OD matrix: " + odm.calculateTripEnds());
		
		//trip starts and trip ends should match OD flows
		HashMap<String, Integer> tripStarts = rna.calculateLADTripStarts();
		HashMap<String, Integer> tripStartsFromODM = odm.calculateTripStarts();
		for (String LAD: tripStarts.keySet()) {
			assertEquals("Trip starts should match flows from each LAD", tripStarts.get(LAD), tripStartsFromODM.get(LAD));
		}
		HashMap<String, Integer> tripEnds = rna.calculateLADTripEnds();
		HashMap<String, Integer> tripEndsFromODM = odm.calculateTripEnds();
		for (String LAD: tripEnds.keySet()) {
			assertEquals("Trip ends should match flows to each LAD", tripEnds.get(LAD), tripEndsFromODM.get(LAD));		
		}
			
		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes()));

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		HashMap<Integer, Double> averagedTravelTimes = rna.getCopyOfLinkTravelTimes();
		rna.updateLinkTravelTimes();
		for (int key: averagedTravelTimes.keySet()) {
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			double updated = rna.getLinkTravelTimes().get(key);
			double averaged = averagedTravelTimes.get(key);
			assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		for (int key: rna.getLinkTravelTimes().keySet()) {
			if (rna.getLinkTravelTimes().get(key) < rna.getLinkFreeFlowTravelTimes().get(key)) {
				System.err.println("For link id = " + key);
				System.err.println("Link volume in PCU: " + rna.getLinkVolumesInPCU().get(key));
				System.err.println("Link travel time " + rna.getLinkTravelTimes().get(key));
				System.err.println("Free-flow Link travel time " + rna.getLinkFreeFlowTravelTimes().get(key));
			}
			double actual = rna.getLinkTravelTimes().get(key);
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			//if freeFlow time is larger, it is only due to calculation error, so it has to be very close:
			if (freeFlow > actual)	assertThat(actual, closeTo(freeFlow, PRECISION));
		}
						
		System.out.println("Time skim matrix: ");
		rna.calculateTimeSkimMatrix().printMatrixFormatted();
		
		System.out.println("Distance skim matrix:");
		rna.calculateDistanceSkimMatrix().printMatrixFormatted();
				
		//rna.saveAssignmentResults(2015, "testAssignmentResults.csv");
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
		
		
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		rna.resetLinkVolumes();
		rna.resetPathStorages();
		rna.resetTripStartEndCounters();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);
		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		//rsg.calculateAllUtilities(rna.getLinkTravelTimes(), params);
		
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
		
		
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		rna.resetLinkVolumes();
		rna.resetPathStorages();
		rna.resetTripStartEndCounters();
		
		//set route choice parameters
		params.setProperty("TIME", "-1.0");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("INTERSECTIONS", "-3.1");
		
		//rsg.calculateAllUtilities(rna.getLinkTravelTimes(), params);
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
	}
	
	@Test
	public void testFreight() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
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
		
//		final URL zonesUrl = new URL("file://src/main/resources/data/zones.shp");
//		final URL networkUrl = new URL("file://src/main/resources/data/network.shp");
//		final URL nodesUrl = new URL("file://src/main/resources/data/nodes.shp");
//		final URL AADFurl = new URL("file://src/main/resources/data/AADFdirected2015.shp");
//		final String areaCodeFileName = "./src/main/resources/data/population_OA_GB.csv";
//		final String areaCodeNearestNodeFile = "./src/main/resources/data/nearest_node_OA_GB.csv";
//		final String workplaceZoneFileName = "./src/main/resources/data/workplacePopulationFakeSC.csv";
//		final String workplaceZoneNearestNodeFile = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
//		final String freightZoneToLADfile = "./src/main/resources/data/freightZoneToLAD.csv";
//		final String freightZoneNearestNodeFile = "./src/main/resources/data/freightZoneToNearestNode.csv";
//		final String baseYearODMatrixFile = "./src/main/resources/data/balancedODMatrix.csv";
//		final String freightMatrixFile = "./src/main/resources/data/freightMatrix.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		//rna.assignPassengerFlows(odm);
		
		//assign freight flows
		FreightMatrix fm = new FreightMatrix(freightMatrixFile);
		rna.assignFreightFlows(fm);
		
		//rna.saveAssignmentResults(2015, "testAssignmentResultsWithFreight.csv");
		
		//TEST OUTPUT AREA PROBABILITIES
		System.out.println("\n\n*** Testing workplace zone probabilities ***");
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		//test the probability of one workplace zone from each LAD
		assertEquals("The probability of the workplace zone E33040972 is correct", (double)372/112852, rna.getWorkplaceZoneProbabilities().get("E33040972"), EPSILON);
		assertEquals("The probability of the workplace zone E33037947 is correct", (double)215/64219, rna.getWorkplaceZoneProbabilities().get("E33037947"), EPSILON);
		assertEquals("The probability of the workplace zone E33038064 is correct", (double)390/75620, rna.getWorkplaceZoneProbabilities().get("E33038064"), EPSILON);
		assertEquals("The probability of the workplace zone E33041117 is correct", (double)449/56591, rna.getWorkplaceZoneProbabilities().get("E33041117"), EPSILON);

		//test that the sum of probabilities of workplace zones in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToWorkplaceCodes().keySet()) {

			double probabilitySum = 0.0;
			for(Iterator<String> iter = roadNetwork.getZoneToWorkplaceCodes().get(zone).iterator(); iter.hasNext(); ) {
				String areaCode = iter.next();
				probabilitySum += rna.getWorkplaceZoneProbabilities().get(areaCode);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST PATH STORAGE FOR FREIGHT
		System.out.println("\n\n*** Testing path storage for freight ***");
		
		//check that the number of paths for a given OD equals the flow (the number of trips in the OD matrix).
		//System.out.println(rna.getPathStorageFreight());
				
		//for each OD
		for (MultiKey mk: fm.getKeySet()) {
					//System.out.println(mk);
					int originFreightZone = (int) mk.getKey(0);
					int destinationFreightZone = (int) mk.getKey(1);
					VehicleType vht = VehicleType.values()[(int)mk.getKey(2)]; 
					List<Path> pathList = rna.getPathStorageFreight().get(vht).get(originFreightZone, destinationFreightZone);
					
//					int flow = 0;
//					//sum flows for each vehicle type
//					for (RoadNetworkAssignment.VehicleType vehType: RoadNetworkAssignment.VehicleType.values())
//						flow += fm.getFlow(originFreightZone, destinationFreightZone, vehType.ordinal());
					//get flow for that vehicle type
					int flow = fm.getFlow(originFreightZone, destinationFreightZone, vht.ordinal());
	
					assertEquals("The number of paths equals the flow", flow, pathList.size());
		}
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing freight trip starts/ends ***");
		System.out.println(rna.calculateFreightLADTripStarts());
		System.out.println(rna.calculateFreightLADTripEnds());	
	
		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes()));

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		HashMap<Integer, Double> averagedTravelTimes = rna.getCopyOfLinkTravelTimes();
		rna.updateLinkTravelTimes();
		for (int key: averagedTravelTimes.keySet()) {
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			double updated = rna.getLinkTravelTimes().get(key);
			double averaged = averagedTravelTimes.get(key);
			assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		for (int key: rna.getLinkTravelTimes().keySet()) {			
			double actual = rna.getLinkTravelTimes().get(key);
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			//if freeFlow time is larger, it is only due to calculation error, so it has to be very close:
			if (freeFlow > actual)	assertThat(actual, closeTo(freeFlow, PRECISION));
		}
		
		//TEST SKIM MATRIX FOR FREIGHT
		System.out.println("\n\n*** Testing skim matrices for freight ***");
		System.out.println("Cost skim matrix for freight (in £):");
		rna.calculateCostSkimMatrixFreight().printMatrixFormatted();
		System.out.println("Time skim matrix for freight (in min):");
		rna.calculateTimeSkimMatrixFreight().printMatrixFormatted();
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforFreightCounts());
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
		final String baseYearODMatrixFile = "./src/main/resources/data/passengerODMfull.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		
		odm.printMatrixFormatted();
		
		long timeNow = System.currentTimeMillis();
		rna.assignPassengerFlows(odm);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
		
		//TEST OUTPUT AREA PROBABILITIES
		System.out.println("\n\n*** Testing output area probabilities ***");
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		//test the probability of one output area from each LAD
		assertEquals("The probability of the output area E00116864 is correct", (double)299/176462, rna.getAreaCodeProbabilities().get("E00116864"), EPSILON);
		assertEquals("The probability of the output area E00086552 is correct", (double)430/236882, rna.getAreaCodeProbabilities().get("E00086552"), EPSILON);
		assertEquals("The probability of the output area E00115160 is correct", (double)370/125199, rna.getAreaCodeProbabilities().get("E00115160"), EPSILON);
		assertEquals("The probability of the output area E00172724 is correct", (double)666/138265, rna.getAreaCodeProbabilities().get("E00172724"), EPSILON);

		//test that the sum of probabilities of output areas in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToAreaCodes().keySet()) {

			double probabilitySum = 0.0;
			for(Iterator<String> iter = roadNetwork.getZoneToAreaCodes().get(zone).iterator(); iter.hasNext(); ) {
				String areaCode = iter.next();
				probabilitySum += rna.getAreaCodeProbabilities().get(areaCode);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST ENERGY UNIT COSTS
		System.out.println("\n\n*** Testing the setter for the electricity unit cost ***");
		
		System.out.println("Energy unit costs:\t\t" + rna.getEnergyUnitCosts());
		System.out.println("Energy consumptions per 100 km:\t" + rna.getEnergyConsumptionsPer100km());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		
		rna.setEnergyUnitCost(RoadNetworkAssignment.EngineType.ELECTRICITY, 0.20);
		
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EngineType.ELECTRICITY), EPSILON);
		
		//TEST PATH STORAGE
		System.out.println("\n\n*** Testing path storage ***");
		
		double totalDistance = 0.0;
		//check that the number of paths for a given OD equals the flow (the number of trips in the OD matrix).
		rna.getPathStorage();
		//for each OD
		for (MultiKey mk: odm.getKeySet()) {
					//System.out.println(mk);
					String originZone = (String) mk.getKey(0);
					String destinationZone = (String) mk.getKey(1);
					List<Path> pathList = rna.getPathStorage().get(originZone, destinationZone);
					int flow = odm.getFlow(originZone, destinationZone);
					assertEquals("The number of paths equals the flow", flow, pathList.size());
					
					for (Path p: pathList) 
						for (Object e: p.getEdges())
							totalDistance += (double)((SimpleFeature)(((Edge)e).getObject())).getAttribute("LenNet");
		}
		System.out.println("Total distance = " + totalDistance);
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing trip starts/ends ***");
		
		System.out.println("Trip starts: " + rna.calculateLADTripStarts());
		System.out.println("Trip ends: " + rna.calculateLADTripEnds());
		System.out.println("OD matrix:");
		odm.printMatrixFormatted();
		System.out.println("Trip starts from OD matrix: " + odm.calculateTripStarts());
		System.out.println("Trip ends from OD matrix: " + odm.calculateTripEnds());
		
		//trip starts and trip ends should match OD flows
		HashMap<String, Integer> tripStarts = rna.calculateLADTripStarts();
		HashMap<String, Integer> tripStartsFromODM = odm.calculateTripStarts();
		for (String LAD: tripStarts.keySet()) {
			assertEquals("Trip starts should match flows from each LAD", tripStarts.get(LAD), tripStartsFromODM.get(LAD));
		}
		HashMap<String, Integer> tripEnds = rna.calculateLADTripEnds();
		HashMap<String, Integer> tripEndsFromODM = odm.calculateTripEnds();
		for (String LAD: tripEnds.keySet()) {
			assertEquals("Trip ends should match flows to each LAD", tripEnds.get(LAD), tripEndsFromODM.get(LAD));		
		}
			
		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes()));

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		HashMap<Integer, Double> averagedTravelTimes = rna.getCopyOfLinkTravelTimes();
		rna.updateLinkTravelTimes();
		for (int key: averagedTravelTimes.keySet()) {
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			double updated = rna.getLinkTravelTimes().get(key);
			double averaged = averagedTravelTimes.get(key);
			assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes());
		for (int key: rna.getLinkTravelTimes().keySet()) {			
			double actual = rna.getLinkTravelTimes().get(key);
			double freeFlow = rna.getLinkFreeFlowTravelTimes().get(key);
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			//if freeFlow time is larger, it is only due to calculation error, so it has to be very close:
			if (freeFlow > actual)	assertThat(actual, closeTo(freeFlow, PRECISION));
		}
		
		System.out.println("Time skim matrix: ");
		rna.calculateTimeSkimMatrix().printMatrixFormatted();
		
		System.out.println("Distance skim matrix:");
		rna.calculateDistanceSkimMatrix().printMatrixFormatted();
				
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforCounts());
	}
}