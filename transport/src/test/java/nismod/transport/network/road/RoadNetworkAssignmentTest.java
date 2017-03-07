/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

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
		
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);

		//visualise the shapefiles
		//roadNetwork.visualise("Mini Test Area");
		
		//export to shapefile
		//roadNetwork.exportToShapefile("miniOutputNetwork");

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName2 = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile2 = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName2 = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile2 = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName2, areaCodeNearestNodeFile2, workplaceZoneFileName2, workplaceZoneNearestNodeFile2);
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");
		
		RoadNetworkAssignment roadNetworkAssignment = new RoadNetworkAssignment(roadNetwork2, null, null, null, null, null);
		ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		passengerODM.printMatrix(); 
		
		for (int i = 0; i < 5; i++) {
			roadNetworkAssignment.resetLinkVolumesInPCU();
			roadNetworkAssignment.assignPassengerFlows(passengerODM);
			HashMap<Integer, Double> oldTravelTimes = roadNetworkAssignment.getCopyOfLinkTravelTimes();
			roadNetworkAssignment.updateLinkTravelTimes(0.9);
			System.out.println("Difference in link travel times: " + roadNetworkAssignment.getAbsoluteDifferenceInLinkTravelTimes(oldTravelTimes));
		}		

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
				
		System.out.println("Total energy consumptions:");
		System.out.println(roadNetworkAssignment.calculateEnergyConsumptions());
		
		System.out.println("Peak-hour link point capacities:");
		System.out.println(roadNetworkAssignment.calculatePeakLinkPointCapacities());
		
		System.out.println("Peak-hour link densities:");
		System.out.println(roadNetworkAssignment.calculatePeakLinkDensities());
		
		roadNetworkAssignment.saveAssignmentResults(2015, "assignment2015.csv");
	}

	@Test
	public void miniTest() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/minitestdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/minitestdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/test/resources/minitestdata/passengerODM.csv";
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);
			
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
		rna.getPathStorage();
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
		for (int key: rna.getLinkTravelTimes().keySet()) 			
			assertTrue(rna.getLinkTravelTimes().get(key) >= rna.getLinkFreeFlowTravelTimes().get(key));	
	}

	@Test
	public void test() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);

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
		for (int key: rna.getLinkTravelTimes().keySet()) 			
			assertTrue(rna.getLinkTravelTimes().get(key) >= rna.getLinkFreeFlowTravelTimes().get(key));	
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
		final String baseYearODMatrixFile = "./src/test/resources/testdata/passengerODM.csv";
		final String freightMatrixFile = "./src/test/resources/testdata/FreightMatrix.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile);

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlows(odm);
		
		//assign freight flows
		FreightMatrix fm = new FreightMatrix(freightMatrixFile);
		rna.assignFreightFlows(fm);
		
		//TEST OUTPUT AREA PROBABILITIES
		System.out.println("\n\n*** Testing output area probabilities ***");
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		//test the probability of one output area from each LAD
		assertEquals("The probability of the workplace zone E33040972 is correct", (double)372/112852, rna.getWorkplaceZoneProbabilities().get("E33040972"), EPSILON);
		assertEquals("The probability of the workplace zone E33037947 is correct", (double)215/64219, rna.getWorkplaceZoneProbabilities().get("E33037947"), EPSILON);
		assertEquals("The probability of the workplace zone E33038064 is correct", (double)390/75620, rna.getWorkplaceZoneProbabilities().get("E33038064"), EPSILON);
		assertEquals("The probability of the workplace zone E33041117 is correct", (double)449/56591, rna.getWorkplaceZoneProbabilities().get("E33041117"), EPSILON);

		//test that the sum of probabilities of output areas in each LAD zone is 1.0
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
		rna.getPathStorageFreight();
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
		for (int key: rna.getLinkTravelTimes().keySet()) 			
			assertTrue(rna.getLinkTravelTimes().get(key) >= rna.getLinkFreeFlowTravelTimes().get(key));
		
		//TEST SKIM MATRIX FOR FREIGHT
		System.out.println("\n\n*** Testing skim matrices for freight ***");
		System.out.println("Cost skim matrix for freight (in £):");
		rna.calculateCostSkimMatrixFreight().printMatrixFormatted();
		System.out.println("Time skim matrix for freight (in min):");
		rna.calculateTimeSkimMatrixFreight().printMatrixFormatted();
	}
}