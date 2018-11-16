/**
 * 
 */
package nismod.transport.network.road;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertEquals;
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
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.math3.stat.Frequency;
import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.demand.EstimatedODMatrix;
import nismod.transport.demand.FreightMatrix;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrixTempro;
import nismod.transport.demand.SkimMatrixFreight;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RoadNetworkAssignment class
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignmentTest {

	public static void main( String[] args ) throws IOException	{
		
		//final String configFile = "./src/main/full/config/config.properties";
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
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String temproRoutesFile = props.getProperty("temproRoutesFile");
		final String outputFolder = props.getProperty("outputFolder");
		
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
		
		
		
		
		//set assignment fraction
		//rna.setAssignmentFraction(0.1);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		//read passenger car flows
		//ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
			
		//RealODMatrixTempro odm = RealODMatrixTempro.createUnitMatrix(zoning);
		//odm.deleteInterzonalFlows("E02006781"); //Isle of Scilly in Tempro
		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		RealODMatrixTempro odm = new RealODMatrixTempro(temproODMatrixFile, zoning);
		
		
		//odm.scaleMatrixValue(8.0);
		//odm.printMatrixFormatted("Tempro unit OD matrix:", 2);
		
		//read routes
		long timeNow = System.currentTimeMillis();
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
//		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Routes read in %d milliseconds.\n", timeNow);
//		rsg.printStatistics();
		
		//read routes
		timeNow = System.currentTimeMillis();
		rsg.readRoutesBinaryWithoutValidityCheck(temproRoutesFile);
//		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Routes read in %d milliseconds.\n", timeNow);
		rsg.printStatistics();
		
		rsg.calculateAllPathsizes();
		rsg.generateSingleNodeRoutes();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		params.setProperty("DISTANCE_THRESHOLD", "200000.0");
		
		
//		HashMap<TimeOfDay, RouteSetGenerator> routeStorage = new HashMap<TimeOfDay, RouteSetGenerator>();
//		for (TimeOfDay hour: TimeOfDay.values()) {
//			routeStorage.put(hour, new RouteSetGenerator(roadNetwork));
//		}
//		
		//assign passenger flows
		timeNow = System.currentTimeMillis();
		//rna.assignPassengerFlowsHourlyRouting(odm, routeStorage);
		//for (TimeOfDay hour: TimeOfDay.values()) {
		//	routeStorage.get(hour).printStatistics();
		//}
		
		//rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		rna.assignPassengerFlowsRouteChoiceTemproDistanceBased(odm, zoning, rsg, params);
		
		
		
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Passenger flows assigned in %d seconds.\n", timeNow / 1000);
		
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		rna.updateLinkVolumePerVehicleType();
		
		rna.printRMSNstatistic();
		rna.printGEHstatistic();
		rna.printHourlyGEHstatistic();
		
		int count = 0;
		for (Trip trip:	rna.getTripList()) {
				if (trip instanceof TripMinor) {
					count++;
					System.out.println(((TripMinor)trip).getLength());
				}
		}
		
		System.out.println(rna.getTripList().size());
		System.out.println(count);
		System.out.println(zoning.getZoneIDToCodeMap().keySet());
		
		System.out.println("Total flows: " + odm.getSumOfFlows());
		System.out.println(odm.getUnsortedOrigins().size());
		System.out.println(odm.getUnsortedDestinations().size());
		
//		rna.updateLinkVolumeInPCU();
//		rna.updateLinkVolumeInPCUPerTimeOfDay();
//		rna.updateLinkVolumePerVehicleType();
//		rna.updateLinkTravelTimes();
//		
//		rna.saveZonalVehicleKilometres(2015, "zonalvkms.csv");
//		rna.saveZonalVehicleKilometresWithAccessEgress(2015, "zonalvkmsAccessEgress.csv");
//				
//		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleType());
//		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(false));
//		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(true));
//		
//		rna.saveZonalCarEnergyConsumptions(2015, 0.5, "zonalEnergyConsumptions.csv");
//		rna.saveOriginDestinationCarElectricityConsumption("ODElectricityConsumtions.csv");
		
		
		//rna.saveLinkTravelTimes(2015, "linkTravelTimes.csv");
		//rna.saveTotalEnergyConsumptions(2015, "totalEnergy.csv");
		//rna.saveZonalCarEnergyConsumptions(2015, 0.5, "zonalEnergy.csv");
			
		//roadNetworkAssignment.saveHourlyCarVolumes(2015, "hourlyCarVolumesAstar.csv");
		//roadNetworkAssignment.saveHourlyCarVolumes(2015, "hourlyCarVolumes.csv");

		//roadNetworkAssignment.saveAssignmentResults(2015, "assignmentResultsAstar.csv");
		//roadNetworkAssignment.saveAssignmentResults(2015, "assignmentResults.csv");
		
		//roadNetworkAssignment.saveZonalVehicleKilometres(2015, "zonalVehicleKilometres.csv");
		
		/*
		
		//do some trip list processing
		timeNow = System.currentTimeMillis();
		ArrayList<Trip> tripList = rna.getTripList();
		int count = 0;
		for (Trip t: tripList) 
			if (t.getEngine() == EngineType.ICE_PETROL) count++;
		System.out.println("The number of petrol trips: " + count);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Collection processing in %d milliseconds.\n", timeNow);
		
		timeNow = System.currentTimeMillis();
		Long countOfPetrolTrips = tripList.stream().filter(t -> t.getEngine() == EngineType.ICE_PETROL).count();
		System.out.println("The number of petrol trips with Java streams: " + countOfPetrolTrips);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Stream processing in %d milliseconds.\n", timeNow);
		
		timeNow = System.currentTimeMillis();
		Long countOfPetrolTrips2 = tripList.parallelStream().filter(t -> t.getEngine() == EngineType.ICE_PETROL).count();
		System.out.println("The number of petrol trips with Java streams: " + countOfPetrolTrips2);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Parallel stream processing in %d milliseconds.\n", timeNow);
		
		//rna.saveAssignmentResults(2015, outputFolder + "car" + assignmentResultsFile);
		
		System.out.printf("RMSN for counts (0.25 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(0.25));
		System.out.printf("RMSN for counts (0.5 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(0.5));
		System.out.printf("RMSN for counts (0.7 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(0.75));
		System.out.printf("RMSN for counts: %.2f%% %n", rna.calculateRMSNforSimulatedVolumes());
		System.out.printf("RMSN for counts (2.0 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(2.0));
		System.out.printf("RMSN for counts (3.0 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(3.0));
		System.out.printf("RMSN for counts (4.0 expansion factor): %.2f%% %n", rna.calculateRMSNforExpandedSimulatedVolumes(4.0));
	
		
		*/
		
		/*
		
		//clear the routes
		rsg.clearRoutes();
		
		//reset link volumes
		rna.resetLinkVolumes();
		
		//clear the trip list
		rna.resetTripStorages();
				
				
		
		// FREIGHT ASSIGNMENT //
		
		//sort nodes based on workplace zone population!
		roadNetwork.sortGravityNodesFreight();
		
		//FreightMatrix freightMatrix = new FreightMatrix("./src/main/resources/data/freightMatrix.csv");	
		FreightMatrix freightMatrix = new FreightMatrix(freightMatrixFile);
		freightMatrix.printMatrixFormatted();
		
		//read routes
		long timeNow = System.currentTimeMillis();
		//rsg.readRoutes("./src/main/resources/data/routesFreight/routesFreight.txt");
		//rsg.readRoutesBinary("./src/main/resources/data/freightRoutes/freightRoutes.dat");
		//rsg.readRoutesBinaryWithoutValidityCheck("./src/main/resources/data/freightRoutesTop5/freightRoutesTop5.dat");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		rsg.readRoutesBinaryWithoutValidityCheck(freightRoutesFile);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Freight routes read in %d seconds.\n", timeNow / 1000);
		rsg.printStatistics();
		
 		//assign freight flows
		timeNow = System.currentTimeMillis();
//		roadNetworkAssignment.assignFreightFlows(freightMatrix);
		rna.assignFreightFlowsRouteChoice(freightMatrix, rsg, props);
		timeNow = System.currentTimeMillis() - timeNow;
		System.out.printf("Freight flows assigned in %d seconds.\n", timeNow / 1000);
		
		//roadNetworkAssignment.saveAssignmentResults(2015, "assignment2015passengerAndFreigh.csv");

		
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
		System.out.println(roadNetwork.getNetwork().getNodes());
		System.out.println("Node to zone mapping: ");
		System.out.println(roadNetwork.getNodeToZone());
		System.out.println("Zone to nodes mapping: ");
		System.out.println(roadNetwork.getZoneToNodes());
		
		System.out.println("Trip list: ");
		tripList = rna.getTripList();
		Frequency freq = new Frequency();
		for (Trip trip: tripList) {
			//System.out.println(trip.toString());
			freq.addValue(trip.getEngine());
		}
		System.out.println("Frequencies: ");
		System.out.println(freq);
		
		freq = new Frequency();
		for (Trip trip: tripList) {
			//System.out.println(trip.toString());
			freq.addValue(trip.getTimeOfDay());
		}
		System.out.println("Frequencies: ");
		System.out.println(freq);

		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumePerVehicleType();
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		
		System.out.println("Link volumes in PCU: ");
		System.out.println(rna.getLinkVolumeInPCU());
		
		System.out.println("Link volume per vehicle Type: ");
		System.out.println(rna.getLinkVolumePerVehicleType());
		
		System.out.println("Free-flow travel times: ");
		System.out.println(rna.getLinkFreeFlowTravelTimes());	
		
		System.out.println("Peak-hour travel times: ");
		System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));	
		
		System.out.println("Time skim matrix:");
		SkimMatrix timeSkimMatrix = new SkimMatrix();
		rna.updateTimeSkimMatrix(timeSkimMatrix);
		timeSkimMatrix.printMatrixFormatted();
		
		System.out.println("Cost skim matrix:");
		rna.calculateCostSkimMatrix().printMatrixFormatted();
		
		System.out.println("Distance skim matrix:");
		SkimMatrix distanceSkimMatrix = rna.calculateDistanceSkimMatrix();
		distanceSkimMatrix.printMatrixFormatted();
		//distanceSkimMatrix.saveMatrixFormatted("distanceSkimMatrix.csv");
		
		System.out.println("Distance skim matrix for freight:");
		SkimMatrixFreight distanceSkimMatrixFreight = rna.calculateDistanceSkimMatrixFreight();
		distanceSkimMatrixFreight.printMatrixFormatted();
		//distanceSkimMatrix.saveMatrixFormatted("distanceSkimMatrix.csv");
				
		System.out.println("Total energy consumptions:");
		System.out.println(rna.calculateEnergyConsumptions());
		
		System.out.println("Zonal car energy consumptions:");
		System.out.println(rna.calculateZonalCarEnergyConsumptions(0.5));
				
		System.out.println("Peak-hour link point capacities:");
		System.out.println(rna.calculatePeakLinkPointCapacities());
		
		System.out.println("Peak-hour link densities:");
		System.out.println(rna.calculatePeakLinkDensities());
		
		//roadNetworkAssignment.saveZonalCarEnergyConsumptions(2015, 0.85, "zonalCarEnergyConsumption85.csv");
		//roadNetworkAssignment.saveZonalCarEnergyConsumptions(2015, 0.5, "zonalCarEnergyConsumption50.csv");
		rna.saveAssignmentResults(2015, outputFolder + assignmentResultsFile);

		System.out.printf("RMSN for freight: %.2f%% %n", rna.calculateRMSNforFreightCounts());
		
//		for (double expansionFactor = 0.1; expansionFactor < 5.0; expansionFactor += 0.1) {
//			System.out.printf("Expansion factor: %.2f RMSN for counts: %.2f%% %n", expansionFactor, rna.calculateRMSNforExpandedSimulatedVolumes(expansionFactor));
//		}
  		
  		*/
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
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
		
		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlowsRouting(odm, null, props);
		
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
		System.out.println("Energy consumptions:\t" + rna.getEnergyConsumptionParameters());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		rna.setEnergyUnitCost(RoadNetworkAssignment.EnergyType.ELECTRICITY, 0.20);
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EnergyType.ELECTRICITY), EPSILON);

		//TEST TRIP STORAGE
		System.out.println("\n\n*** Testing trip storage ***");

		//check that the number of trips for a given OD equals the flow (the number of trips in the OD matrix).
		//for each OD
		for (MultiKey mk: odm.getKeySet()) {
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);
			List<Trip> tripList = rna.getTripList();
			int counter = 0;
			for (Trip trip: tripList) {
				String originLAD = trip.getOriginLAD(roadNetwork.getNodeToZone());
				String destinationLAD = trip.getDestinationLAD(roadNetwork.getNodeToZone());
				if (originZone.equals(originLAD) && destinationZone.equals(destinationLAD)) counter++;
			}
			int flow = odm.getFlow(originZone, destinationZone);
			assertEquals("The number of paths equals the flow", flow, counter);
		}
		
		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		//System.out.println(Arrays.toString(rna.getLinkFreeFlowTravelTimes()));
		//System.out.println(Arrays.toString(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM))); //get times for the peak hour
		//assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)));
		
		for (int i=1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			assertEquals(actual, freeFlow, PRECISION);
		}

		//after assignment the link travel times should be greater or equal than the free flow travel times.
		rna.updateLinkTravelTimes();
		//System.out.println(rna.getLinkFreeFlowTravelTimes());
		//System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		for (int i=1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			//if freeFlow time is larger, it is only due to calculation error, so it has to be very close:
			if (freeFlow > actual)	assertThat(actual, closeTo(freeFlow, PRECISION));
		}
		
		//test saving and reading link travel times per time of day
		rna.saveLinkTravelTimes(2015, "./temp/miniTestLinkTravelTimes.csv");
		Map<TimeOfDay, Map<Integer, Double>> loadedLinkTravelTimes = InputFileReader.readLinkTravelTimeFile(2015, "./temp/miniTestLinkTravelTimes.csv");
		//System.out.println(loadedLinkTravelTimes);
		Map<TimeOfDay, double[]> linkTravelTimes = rna.getLinkTravelTimes();
		
		//compare them
		final double PRECISION = 1e-4;
		for (TimeOfDay hour: linkTravelTimes.keySet()) {
			double[] linkTimes = linkTravelTimes.get(hour);
			Map<Integer, Double> loadedLinkTimes = loadedLinkTravelTimes.get(hour);
			for (int edgeID: loadedLinkTimes.keySet()) 
				assertEquals("Link travel time is correct", linkTimes[edgeID], loadedLinkTimes.get(edgeID), PRECISION);			
		}
		
		rna.loadLinkTravelTimes(2015, "./temp/miniTestLinkTravelTimes.csv");
		linkTravelTimes = rna.getLinkTravelTimes();
		for (TimeOfDay hour: loadedLinkTravelTimes.keySet()) {
			double[] linkTimes = linkTravelTimes.get(hour);
			Map<Integer, Double> loadedLinkTimes = loadedLinkTravelTimes.get(hour);
			for (int edgeID: loadedLinkTimes.keySet()) 
				assertEquals("Link travel time is correct", linkTimes[edgeID], loadedLinkTimes.get(edgeID), PRECISION);			
		}
					
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
		
		//hourly assignment with routing
		rna.resetLinkVolumes();
		rna.resetTripList();
		rna.assignPassengerFlowsHourlyRouting(odm, null, props);
	
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
		rna.calculateLADTripEnds();
		rna.calculateLADTripStarts();
					
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		
		//create a new road network assignment
		//rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);
		
		rna.resetLinkVolumes();
		rna.resetTripList();

		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "10");
		params.setProperty("INITIAL_OUTER_CAPACITY", "23");
		params.setProperty("INITIAL_INNER_CAPACITY", "23");
			
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetForODMatrix(odm);
		//rsg.saveRoutesBinary("passengerRoutesMini.dat", false);
		//rsg.generateRouteSet(31, 82);
		//rsg.generateRouteSetWithRandomLinkEliminationRestricted(31, 82);
		//rsg.printChoiceSets();
		rsg.calculateAllPathsizes();
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
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
		
		//TEST VEHICLE KILOMETRES
		System.out.println("\n\n*** Testing vehicle kilometres ***");
		
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumePerVehicleType();
		
		System.out.println("Vehicle kilometres:");
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleType());
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(false));
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(true));
			
		System.out.println("Link volumes in PCU:");
		System.out.println(rna.getLinkVolumeInPCU());
		
		double vehicleKilometres = 0.0;
		for (int edgeID: roadNetwork.getEdgeIDtoEdge().keySet()) {
			
			double volume  = rna.getLinkVolumeInPCU().get(edgeID);
			double length = roadNetwork.getEdgeLength(edgeID);
			vehicleKilometres += volume * length; 
		}
		System.out.println("Total vehicle-kilometres: " + vehicleKilometres);
		assertEquals("Vehicle kilometres are correct", vehicleKilometres, rna.calculateZonalVehicleKilometresPerVehicleType().get("E06000045").get(VehicleType.CAR), EPSILON);
		
		double vehicleKilometresWithAccessEgress = 0.0;
		for (Trip trip: rna.getTripList()) {
			int originNode = trip.getOriginNode().getID();
			int destinationNode = trip.getDestinationNode().getID();
			double access = roadNetwork.getNodeToAverageAccessEgressDistance().get(originNode) / 1000;
			double egress = roadNetwork.getNodeToAverageAccessEgressDistance().get(destinationNode) / 1000;
			int volume = trip.getMultiplier();
			vehicleKilometresWithAccessEgress += volume * (access + egress); 
			
			for (int edgeID: trip.getRoute().getEdges().toArray()) {
				double length = roadNetwork.getEdgeLength(edgeID);
				vehicleKilometresWithAccessEgress += volume * length; 
			}
		
		}
		System.out.println("Total vehicle-kilometres: " + vehicleKilometresWithAccessEgress);
		assertEquals("Vehicle kilometres with access and egress are correct", vehicleKilometresWithAccessEgress, rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(true).get("E06000045").get(VehicleType.CAR), EPSILON*100);
		
		System.out.println("Trip list: ");
		ArrayList<Trip> tripList = rna.getTripList();
		Frequency freq = new Frequency();
		for (Trip trip: tripList) {
			System.out.println(trip.toString());
			freq.addValue(trip.getEngine());
		}
		
		System.out.println("Frequencies for engine type: ");
		System.out.println(freq);
		
		//frequencies per time of day
		freq = new Frequency();
		for (Trip trip: tripList) {
			freq.addValue(trip.getTimeOfDay());
		}
		System.out.println("Frequencies for time of day: ");
		System.out.println(freq);
		
		System.out.println("Link volumes in PCU per time of day: ");
		Map<TimeOfDay, Map<Integer, Double>> map = rna.calculateLinkVolumeInPCUPerTimeOfDay(tripList);
		System.out.println(map);
		
		rna.updateLinkTravelTimes();
	
		System.out.println("Link travel times per time of day: ");
		Map<TimeOfDay, double[]> times = rna.getLinkTravelTimes();
		System.out.println(times);
		for (TimeOfDay hour: TimeOfDay.values()) {
			System.out.println(hour);
			//System.out.println(Arrays.toString(times.get(hour)));
		}
	
		//compare volumes in PCU calculated during the assignment with those calculated from the trip list
		Map<Integer, Double> linkVolumesInPCU = rna.getLinkVolumeInPCU();
		for (Integer edgeID: linkVolumesInPCU.keySet()) {
			double sum = 0.0;
			for (TimeOfDay hour: map.keySet()) {
				Map<Integer, Double> hourlyMap = map.get(hour);
				Double volume = hourlyMap.get(edgeID);
				if (volume == null) volume = 0.0;
				sum += volume;
			}
		assertEquals("PCU flows for each edge are correct", linkVolumesInPCU.get(edgeID), sum, EPSILON);
		}
		
		//rna.saveHourlyCarVolumes(2015, "minitestHourlyVolumes.csv");
		
		//TEST ASSIGNMENT WITH TEMPRO ZONES
		System.out.println("\n\n*** Testing assignment with route choice ***");
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		
		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		ODMatrix temproODM = new ODMatrix(temproODMatrixFile);
		
		temproODM.printMatrixFormatted("Tempro OD Matrix");

		rna.assignPassengerFlowsTempro(temproODM, zoning, rsg, props);
		System.out.println("Trip list size: " + rna.getTripList().size());
		rna.updateLinkVolumePerVehicleType();
		System.out.println(rna.getLinkVolumePerVehicleType());
		System.out.println(roadNetwork.getAADFCarTrafficCounts());
		System.out.println(rna.calculateRMSNforSimulatedVolumes());
		
		int sumVolumes = rna.getLinkVolumePerVehicleType().get(VehicleType.CAR).values().stream().mapToInt(Number::intValue).sum();
		int sumCounts = roadNetwork.getAADFCarTrafficCounts().values().stream().mapToInt(Number::intValue).sum();;
		System.out.println("Sum of volumes : " + sumVolumes);
		System.out.println("Sum of counts : " + sumCounts);
		
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
		
		ODMatrix temproODM2 = ODMatrix.createUnitMatrix(temproODM.getSortedOrigins());
		rna.resetTripList();
		rna.resetLinkVolumes();
		rna.assignPassengerFlowsTempro(temproODM2, zoning, rsg, props);
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
		
		rna.resetLinkVolumes();
		rna.resetTripList();
		rna.assignPassengerFlowsRouteChoiceTempro(temproODM2, zoning, rsg, params);
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
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
		
		//these were not mapped because the count point falls on the river between the two zones
		roadNetwork.getEdgeToZone().put(718, "E07000091"); //New Forest
		roadNetwork.getEdgeToZone().put(719, "E07000091");
					
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

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlowsRouting(odm, null, props);
		
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
		//assertEquals("The probability of node 60 is correct", (double)1656/234671, rna.getNodeProbabilities().get(60), EPSILON);
		assertEquals("The probability of node 60 is correct", (double)1656/234671, rna.getStartNodeProbabilities().get(60), EPSILON);
		assertEquals("The probability of node 60 is correct", (double)1656/234671, rna.getEndNodeProbabilities().get(60), EPSILON);

		//test that the sum of probabilities of nodes in each LAD zone is 1.0
		for (String zone: roadNetwork.getZoneToNodes().keySet()) {

//			double probabilitySum = 0.0;
//			for(Iterator<Integer> iter = roadNetwork.getZoneToNodes().get(zone).iterator(); iter.hasNext(); ) {
//				Integer node = iter.next();
//				probabilitySum += rna.getNodeProbabilities().get(node);
//			}
//			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
//			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
			
			double probabilitySum = 0.0;
			for(Iterator<Integer> iter = roadNetwork.getZoneToNodes().get(zone).iterator(); iter.hasNext(); ) {
				Integer node = iter.next();
				probabilitySum += rna.getStartNodeProbabilities().get(node);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
			
			probabilitySum = 0.0;
			for(Iterator<Integer> iter = roadNetwork.getZoneToNodes().get(zone).iterator(); iter.hasNext(); ) {
				Integer node = iter.next();
				probabilitySum += rna.getEndNodeProbabilities().get(node);
			}
			System.out.printf("The sum of probabilites for zone %s is: %.12f.\n", zone, probabilitySum);
			assertEquals("The sum of probabilities for zone " + zone + " is 1.0", 1.0, probabilitySum, EPSILON);
		}
		
		//TEST ENERGY UNIT COSTS
		System.out.println("\n\n*** Testing the setter for the electricity unit cost ***");
		
		System.out.println("Energy unit costs:\t\t" + rna.getEnergyUnitCosts());
		System.out.println("Energy consumptions:\t" + rna.getEnergyConsumptionParameters());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		
		rna.setEnergyUnitCost(RoadNetworkAssignment.EnergyType.ELECTRICITY, 0.20);
		
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EnergyType.ELECTRICITY), EPSILON);
		
		//check that the number of trips for a given OD equals the flow (the number of trips in the OD matrix).
		//for each OD
		for (MultiKey mk: odm.getKeySet()) {
			String originZone = (String) mk.getKey(0);
			String destinationZone = (String) mk.getKey(1);
			List<Trip> tripList = rna.getTripList();
			int counter = 0;
			for (Trip trip: tripList) {
				String originLAD = trip.getOriginLAD(roadNetwork.getNodeToZone());
				String destinationLAD = trip.getDestinationLAD(roadNetwork.getNodeToZone());
				if (originZone.equals(originLAD) && destinationZone.equals(destinationLAD)) counter++;
			}
			int flow = odm.getFlow(originZone, destinationZone);
			assertEquals("The number of paths equals the flow", flow, counter);
		}
		
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
		//System.out.println(rna.getLinkFreeFlowTravelTimes());
		//System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)); //get times for the peak hour
		//assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)));
		for (int i=1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			assertEquals(actual, freeFlow, PRECISION);
		}

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		Map<TimeOfDay, double[]> averagedTravelTimes = rna.getCopyOfLinkTravelTimes();
		rna.updateLinkTravelTimes();
		for (TimeOfDay hour: TimeOfDay.values())
			for (int i=1; i < averagedTravelTimes.get(hour).length; i++) {
				double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
				double updated = rna.getLinkTravelTimes().get(hour)[i];
				double averaged = averagedTravelTimes.get(hour)[i];
				assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		for (int i=1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			if (rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i] < rna.getLinkFreeFlowTravelTimes()[i]) {
				System.err.println("For link id = " + i);
				System.err.println("Link volume in PCU: " + rna.getLinkVolumeInPCU().get(i));
				System.err.println("Link travel time " + rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i]);
				System.err.println("Free-flow Link travel time " + rna.getLinkFreeFlowTravelTimes()[i]);
			}
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
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
		
		System.out.println("Zonal car energy consumptions:");
		System.out.println(rna.calculateZonalCarEnergyConsumptions(0.85));
		
		System.out.println("Total car energy consumptions:");
		System.out.println(rna.calculateCarEnergyConsumptions());
		
		System.out.println("Total energy consumptions:");
		System.out.println(rna.calculateEnergyConsumptions());
		
		System.out.println("Vehicle kilometres:");
		rna.updateLinkVolumeInPCU();
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleType());
		
		//rna.saveZonalCarEnergyConsumptions(2015, 0.85 , "testZonalCarEnergyConsumptions.csv");
		//rna.saveAssignmentResults(2015, "testAssignmentResults.csv");
		
		//calculate RMSN statistic
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
				
		//calculate GEH statistic
		rna.updateLinkVolumePerVehicleType();
		HashMap<Integer, Double> GEH = rna.calculateGEHStatisticForCarCounts(1/24.0);
				
		int validFlows = 0;
		int suspiciousFlows = 0;
		int invalidFlows = 0;
		for (Integer edgeID: GEH.keySet()) {
			if (GEH.get(edgeID) < 5.0) validFlows++;
			else if (GEH.get(edgeID) < 10.0) suspiciousFlows++;
			else invalidFlows++;
		}
		System.out.printf("Percentage of edges with valid flows (GEH < 5.0) is: %.0f%% %n", (double) validFlows / GEH.size() * 100);
		System.out.printf("Percentage of edges with suspicious flows (5.0 <= GEH < 10.0) is: %.0f%% %n", (double) suspiciousFlows / GEH.size() * 100);
		System.out.printf("Percentage of edges with invalid flows (GEH >= 10.0) is: %.0f%% %n", (double) invalidFlows / GEH.size() * 100);
		
		rna.printGEHstatistic(1/24.0);
		rna.printGEHstatistic(0.1);
		rna.printHourlyGEHstatistic();
			
		//various differences between volumes and counts
		rna.calculateDifferenceCarCounts();
		rna.calculateAbsDifferenceCarCounts();
		rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts();
		rna.calculateMADforExpandedSimulatedVolumes(1.0);
		rna.calculateGEHStatisticPerTimeOfDay(TimeOfDay.EIGHTAM);
		
		//densities, capacities, capacity utilisation
		rna.calculatePeakLinkDensities();
		rna.calculatePeakLinkPointCapacities();
		rna.calculatePeakLinkCapacityUtilisation();
		rna.calculateDirectionAveragedPeakLinkCapacityUtilisation();
		
		//TEST HOURLY ASSIGNMENT WITH ROUTING
		
		//hourly assignment
		rna.resetLinkVolumes();
		rna.resetTripList();
		rna.assignPassengerFlowsHourlyRouting(odm, null, props);
	
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing trip starts/ends for hourly assignment with routing ***");
		
		System.out.println("Trip starts: " + rna.calculateLADTripStarts());
		System.out.println("Trip ends: " + rna.calculateLADTripEnds());
		System.out.println("OD matrix:");
		odm.printMatrixFormatted();
		System.out.println("Trip starts from OD matrix: " + odm.calculateTripStarts());
		System.out.println("Trip ends from OD matrix: " + odm.calculateTripEnds());
		
		//trip starts and trip ends should match OD flows
		tripStarts = rna.calculateLADTripStarts();
		tripStartsFromODM = odm.calculateTripStarts();
		for (String LAD: tripStarts.keySet()) {
			assertEquals("Trip starts should match flows from each LAD", tripStarts.get(LAD), tripStartsFromODM.get(LAD));
		}
		tripEnds = rna.calculateLADTripEnds();
		tripEndsFromODM = odm.calculateTripEnds();
		for (String LAD: tripEnds.keySet()) {
			assertEquals("Trip ends should match flows to each LAD", tripEnds.get(LAD), tripEndsFromODM.get(LAD));		
		}
		
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		//rsg.readRoutes("./src/test/resources/testdata/testRoutes.txt");
		rsg.readRoutes("./src/test/resources/testdata/allRoutes.txt");
		rsg.calculateAllPathsizes();
		//rsg.calculateAllUtilities(rna.getLinkTravelTimes(), params);
		
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
		
		//TEST ASSIGNMENT WITH ROUTE CHOICE
		System.out.println("\n\n*** Testing assignment with route choice ***");
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		//set route choice parameters
		params.setProperty("TIME", "-1.0");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-3.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		params.setProperty("DISTANCE_THRESHOLD", "20000.0");
		
		//rsg.calculateAllUtilities(rna.getLinkTravelTimes(), params);
		rna.assignPassengerFlowsRouteChoice(odm, rsg, params);

		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing trip starts/ends for assignment with route choice ***");
		
		System.out.println("Trip starts: " + rna.calculateLADTripStarts());
		System.out.println("Trip ends: " + rna.calculateLADTripEnds());
		System.out.println("OD matrix:");
		odm.printMatrixFormatted();
		System.out.println("Trip starts from OD matrix: " + odm.calculateTripStarts());
		System.out.println("Trip ends from OD matrix: " + odm.calculateTripEnds());
		
		//trip starts and trip ends should match OD flows
		tripStarts = rna.calculateLADTripStarts();
		tripStartsFromODM = odm.calculateTripStarts();
		for (String LAD: tripStarts.keySet()) {
			assertEquals("Trip starts should match flows from each LAD", tripStarts.get(LAD), tripStartsFromODM.get(LAD));
		}
		tripEnds = rna.calculateLADTripEnds();
		tripEndsFromODM = odm.calculateTripEnds();
		for (String LAD: tripEnds.keySet()) {
			assertEquals("Trip ends should match flows to each LAD", tripEnds.get(LAD), tripEndsFromODM.get(LAD));		
		}
		
		//TEST VEHICLE KILOMETRES
		System.out.println("\n\n*** Testing vehicle kilometres ***");
		
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumePerVehicleType();

		System.out.println("Vehicle kilometres:");
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleType());
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(false));
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTripList(true));

		System.out.println("Link volumes in PCU:");
		System.out.println(rna.getLinkVolumeInPCU());

		for (String zone: roadNetwork.getZoneToNodes().keySet()) {

			double zonalVehicleKilometres = 0.0;
			for (int edgeID: roadNetwork.getEdgeIDtoEdge().keySet()) {

				String fetchedZone = roadNetwork.getEdgeToZone().get(edgeID);
				if (fetchedZone != null && fetchedZone.equals(zone)) {

					Double volume  = rna.getLinkVolumeInPCU().get(edgeID);
					if (volume == null) {
						System.out.println("No volume for edge " + edgeID);
						volume = 0.0;
					}
					double length = roadNetwork.getEdgeLength(edgeID);
					zonalVehicleKilometres += volume * length;
				}
			}
			System.out.printf("Total vehicle-kilometres for zone %s: %f %n", zone, zonalVehicleKilometres);
			assertEquals("Zonal vehicle kilometres are correct", zonalVehicleKilometres, rna.calculateZonalVehicleKilometresPerVehicleType().get(zone).get(VehicleType.CAR), EPSILON);
		}
		
		//TEST TRIP LIST
		System.out.println("\n\n*** Testing trip list ***");
		
		System.out.println("Trip list: ");
		ArrayList<Trip> tripList = rna.getTripList();
		Frequency freq = new Frequency();
		for (Trip trip: tripList) {
			//System.out.println(trip.toString());
			freq.addValue(trip.getEngine());
		}
		
		System.out.println("Frequencies: ");
		System.out.println(freq);
		
		int count = 0;
		for (Trip t: tripList) 
			if (t.getEngine() == EngineType.ICE_PETROL) count++;
		System.out.println("The number of petrol trips: " + count);
		
		Long countOfPetrolTrips = tripList.stream().filter(t -> t.getEngine() == EngineType.ICE_PETROL).count();
		System.out.println("The number of petrol trips with Java streams: " + countOfPetrolTrips);
		
		Long countOfPetrolTrips2 = tripList.parallelStream().filter(t -> t.getEngine() == EngineType.ICE_PETROL).count();
		System.out.println("The number of petrol trips with parallel Java streams: " + countOfPetrolTrips2);
		
		//frequencies per time of day
		freq = new Frequency();
		for (Trip trip: tripList) {
			freq.addValue(trip.getTimeOfDay());
		}
		System.out.println("Frequencies per time of day: ");
		System.out.println(freq);
		
		System.out.println("Link volumes in PCU per time of day: ");
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		Map<TimeOfDay, Map<Integer, Double>> map = rna.getLinkVolumeInPCUPerTimeOfDay();
		
		System.out.println(map);
		
		//compare volumes in PCU calculated during the assignment with those calculated from the trip list
		Map<Integer, Double> linkVolumesInPCU = rna.getLinkVolumeInPCU();
				
		for (Integer edgeID: linkVolumesInPCU.keySet()) {
			double sum = 0.0;
			for (TimeOfDay hour: map.keySet()) {
				Map<Integer, Double> hourlyMap = map.get(hour);
				Double volume = hourlyMap.get(edgeID);
				if (volume == null) volume = 0.0;
				sum += volume;
			}
		assertEquals("PCU flows for each edge are correct", linkVolumesInPCU.get(edgeID), sum, EPSILON);
		}
		
		//rna.saveHourlyCarVolumes(2015, "hourlyCarVolumes.csv");
		
		System.out.println("Time skim matrix: ");
		rna.calculateTimeSkimMatrix().printMatrixFormatted();
		
		System.out.println("Distance skim matrix:");
		rna.calculateDistanceSkimMatrix().printMatrixFormatted();
		
		System.out.println("Cost skim matrix: ");
		rna.calculateCostSkimMatrix().printMatrixFormatted();
		
		
		//TEST ASSIGNMENT WITH TEMPRO ZONES
		System.out.println("\n\n*** Testing assignment with tempro zones ***");
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		System.out.println("Zones to nearest Nodes: " + zoning.getZoneToNearestNodeIDMap());
		System.out.println("Zones to nearest nodes distances: " + zoning.getZoneToNearestNodeDistanceMap());
		
		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		ODMatrix temproODM = new ODMatrix(temproODMatrixFile);
		
		//temproODM.printMatrixFormatted("Tempro OD Matrix");
		rna.assignPassengerFlowsTempro(temproODM, zoning, rsg, props);
		rna.calculateDistanceSkimMatrixTempro();
		
		ODMatrix temproODM2 = ODMatrix.createUnitMatrix(temproODM.getSortedOrigins());
		rna.resetTripList();
		rna.assignPassengerFlowsTempro(temproODM2, zoning, rsg, props);
		rna.calculateDistanceSkimMatrixTempro();
		
		rna.calculateLinkVolumePerVehicleType(rna.getTripList());
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(false, false));
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(true, false));
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(false, true));
		System.out.println(rna.calculateZonalVehicleKilometresPerVehicleTypeFromTemproTripList(true, true));
		
		rna.resetTripList();
		rna.resetLinkVolumes();
		rna.assignPassengerFlowsRouteChoiceTempro(temproODM2, zoning, rsg, params);
		rna.calculateDistanceSkimMatrixTempro();
		
		rsg.generateSingleNodeRoutes();
		rna.resetTripList();
		rna.resetLinkVolumes();
		rna.assignPassengerFlowsRouteChoiceTemproDistanceBased(temproODM2, zoning, rsg, params);
		rna.calculateDistanceSkimMatrixTempro();
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing LAD trip starts/ends for assignment with tempro route choice ***");
		
		System.out.println("Trip starts: " + rna.calculateLADTripStarts());
		System.out.println("Trip ends: " + rna.calculateLADTripEnds());
		System.out.println("Tempro to LAD OD matrix:");
		ODMatrix t2odm = ODMatrix.createLadMatrixFromTEMProMatrix(temproODM2, zoning);
		t2odm.printMatrixFormatted();
		System.out.println("Trip starts from OD matrix: " + t2odm.calculateTripStarts());
		System.out.println("Trip ends from OD matrix: " + t2odm.calculateTripEnds());
		
		//trip starts and trip ends should match OD flows
		tripStarts = rna.calculateLADTripStarts();
		tripStartsFromODM = t2odm.calculateTripStarts();
		for (String LAD: tripStarts.keySet()) {
			assertEquals("Trip starts should match flows from each LAD", tripStarts.get(LAD), tripStartsFromODM.get(LAD));
		}
		tripEnds = rna.calculateLADTripEnds();
		tripEndsFromODM = t2odm.calculateTripEnds();
		for (String LAD: tripEnds.keySet()) {
			assertEquals("Trip ends should match flows to each LAD", tripEnds.get(LAD), tripEndsFromODM.get(LAD));		
		}
			
		//test zonal temporal trip starts
		System.out.println(rna.calculateZonalTemporalTripStartsForElectricVehicles());
		System.out.println(rna.calculateZonalTemporalCarElectricityConsumptions(1.0));
		
		//observed trip length distribution
		System.out.println("Trip length distributions:");
		System.out.println(Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, false, true)));
		System.out.println(Arrays.toString(rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, true, true)));
		System.out.println(Arrays.toString(EstimatedODMatrix.OTLD));
		System.out.println(Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, false, true)));
		System.out.println(Arrays.toString(rna.getObservedTripLengthDistribution(EstimatedODMatrix.BIN_LIMITS_KM, true, true)));
		
		double sum = 0.0;
		double[] frequencies = rna.getObservedTripLengthFrequencies(EstimatedODMatrix.BIN_LIMITS_KM, false, true);
		for (double f: frequencies) sum += f;
		assertEquals("Total number of trips equals sum of frequences", temproODM2.getTotalIntFlow(), sum, EPSILON);
		
		//SAVING METHODS
		rna.saveAssignmentResults(2015, "./temp/testAssignmentResults.csv");
		rna.saveHourlyCarVolumes(2015, "./temp/testHourlyCarVolumes.csv");
		rna.saveLinkTravelTimes(2015, "./temp/testLinkTravelTimes.csv");
		rna.saveOriginDestinationCarElectricityConsumption("./temp/testODCarElectricityConsumption.csv");
		rna.savePeakLinkPointCapacities(2015, "./temp/testPeakLinkPointCapacities.csv");
		rna.saveTotalCO2Emissions(2015, "./temp/testTotalCO2Emissions.csv");
		rna.saveTotalEnergyConsumptions(2015, "./temp/testTotalEnergyConsumptions.csv");
		rna.saveZonalCarEnergyConsumptions(2015, 0.5, "./temp/testZonalCarEnergyConsumptions.csv");
		rna.saveZonalVehicleKilometres(2015, "./temp/testZonalVehicleKilometres.csv");
		rna.saveZonalVehicleKilometresWithAccessEgress(2015, "./temp/testZonalVehicleKilometresWithAccessEgress.csv");
		rna.saveZonalTemporalTripStartsForEVs(2015, "./temp/testZonalTemporalEVTripStarts.csv");
		rna.saveZonalTemporalCarElectricity(2015, 1.0, "./temp/testZonalTemporalCarElectricityConsumption.csv");
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
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

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		//rna.assignPassengerFlows(odm);
		
		//assign freight flows
		FreightMatrix fm = new FreightMatrix(baseYearFreightMatrixFile);
		fm.printMatrixFormatted();
		rna.assignFreightFlowsRouting(fm, null, props);
		rna.saveAssignmentResults(2015, "./temp/testAssignmentResultsWithFreight.csv");
		
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
		
		//TEST COUNTERS OF TRIP STARTS/ENDS
		System.out.println("\n\n*** Testing freight trip starts/ends ***");
		System.out.println(rna.calculateFreightLADTripStarts());
		System.out.println(rna.calculateFreightLADTripEnds());	
	
		//TEST LINK TRAVEL TIMES
		System.out.println("\n\n*** Testing link travel times ***");

		//before assignment link travel times should be equal to free flow travel times
		//System.out.println(rna.getLinkFreeFlowTravelTimes());
		//System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		//assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)));
		for (int i=1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
			//assertTrue(actual >= freeFlow);
			//assertThat(actual, greaterThanOrEqualTo(freeFlow));
			final double PRECISION = 1e-6;
			assertEquals(actual, freeFlow, PRECISION);
		}

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		double[] averagedTravelTimes = rna.getCopyOfLinkTravelTimes().get(TimeOfDay.EIGHTAM);
		rna.updateLinkTravelTimes();
		for (int i=1; i < averagedTravelTimes.length; i++) {
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
			double updated = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double averaged = averagedTravelTimes[i];
			assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		for (int i = 1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {			
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
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
		
		System.out.println("Distance skim matrix for freight:");
		SkimMatrixFreight distanceSkimMatrixFreight = rna.calculateDistanceSkimMatrixFreight();
		distanceSkimMatrixFreight.printMatrixFormatted();
		
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforFreightCounts());
		
		//TEST TRIP LIST
		System.out.println("\n\n*** Testing trip list ***");
		
		System.out.println("Trip list: ");
		ArrayList<Trip> tripList = rna.getTripList();
		Frequency freq = new Frequency();
		for (Trip trip: tripList) {
			//System.out.println(trip.toString());
			freq.addValue(trip.getVehicle());
		}
		
		System.out.println("Frequencies: ");
		System.out.println(freq);
		
		//check that the number of trips for a given OD equals the flow (the number of trips in the OD matrix).
		//for each OD
		for (MultiKey mk: fm.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicle = (int) mk.getKey(2);
			
			int counter = 0;
			for (Trip trip: tripList) {
				int originZone = trip.getFreightOriginZone();
				int destinationZone = trip.getFreightDestinationZone();
				VehicleType vht = trip.getVehicle(); 
				if (originZone == origin && destinationZone == destination && vehicle == vht.getValue()) counter++;
			}
			int flow = fm.getFlow(origin, destination, vehicle);
			assertEquals("The number of paths equals the flow", flow, counter);
		}
		
		//hourly assignment with routing
		rna.resetLinkVolumes();
		rna.resetTripList();
		rna.assignFreightFlowsHourlyRouting(fm, null, props);
		
		//hourly assignment with routing
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		//read freight routes
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		
		System.out.println(freightRoutesFile);
		rsg.printStatistics();
		
		rsg.readRoutesBinaryWithoutValidityCheck(freightRoutesFile);
		rsg.printStatistics();
		
		rsg.generateSingleNodeRoutes();
		rsg.generateRouteSetForFreightMatrix(fm, 5);
		rsg.printStatistics();
		rsg.calculateAllPathsizes();
				
		rna.assignFreightFlowsRouteChoice(fm, rsg, props);
		
		rna.calculateDistanceSkimMatrixFreight().printMatrixFormatted();
		rna.calculateFreightLADTripEnds();
		rna.calculateFreightLADTripStarts();

		rna.printRMSNstatisticFreight();
		rna.printGEHstatisticFreight();
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

		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null, null, null, null, null, null, null, null, null, props);

		//assign passenger flows
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		
		odm.printMatrixFormatted();
		
		long timeNow = System.currentTimeMillis();
		rna.assignPassengerFlowsRouting(odm, null, props);
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
		System.out.println("Energy consumptions:\t" + rna.getEnergyConsumptionParameters());
		System.out.println("Engine type fractions:\t\t" + rna.getEngineTypeFractions());
		
		rna.setEnergyUnitCost(RoadNetworkAssignment.EnergyType.ELECTRICITY, 0.20);
		
		assertEquals("asdf", 0.20, (double) rna.getEnergyUnitCosts().get(RoadNetworkAssignment.EnergyType.ELECTRICITY), EPSILON);
		
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
		System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)); //get times for the peak hour
		assertTrue(rna.getLinkFreeFlowTravelTimes().equals(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)));

		//check weighted averaging for travel times
		rna.updateLinkTravelTimes(0.9);
		Map<TimeOfDay, double[]> averagedTravelTimes = rna.getCopyOfLinkTravelTimes();
		rna.updateLinkTravelTimes();
		for (TimeOfDay hour: TimeOfDay.values())
			for (int i=1; i < averagedTravelTimes.get(hour).length; i++) {
				double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
				double updated = rna.getLinkTravelTimes().get(hour)[i];
				double averaged = averagedTravelTimes.get(hour)[i];
				assertEquals("Averaged travel time should be correct", 0.9*updated + 0.1*freeFlow, averaged, EPSILON);
		}
		
		//after assignment and update the link travel times should be greater or equal than the free flow travel times.
		System.out.println(rna.getLinkFreeFlowTravelTimes());
		System.out.println(rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM));
		for (int i = 1; i < rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM).length; i++) {
			if (rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i] < rna.getLinkFreeFlowTravelTimes()[i]) {
				System.err.println("For link id = " + i);
				System.err.println("Link volume in PCU: " + rna.getLinkVolumeInPCU().get(i));
				System.err.println("Link travel time " + rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i]);
				System.err.println("Free-flow Link travel time " + rna.getLinkFreeFlowTravelTimes()[i]);
			}
			double actual = rna.getLinkTravelTimes().get(TimeOfDay.EIGHTAM)[i];
			double freeFlow = rna.getLinkFreeFlowTravelTimes()[i];
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
				
		System.out.printf("RMSN: %.2f%%\n", rna.calculateRMSNforSimulatedVolumes());
	}
}