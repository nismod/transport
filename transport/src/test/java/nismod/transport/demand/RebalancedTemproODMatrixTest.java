/**
 * 
 */
package nismod.transport.demand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sanselan.ImageWriteException;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RebalancedODMatrix class
 * @author Milan Lovric
 *
 */
public class RebalancedTemproODMatrixTest {
	
	private final static Logger LOGGER = LogManager.getLogger(RebalancedTemproODMatrixTest.class);
	
	public static void main( String[] args ) throws FileNotFoundException, IOException, ImageWriteException {
		
		final String configFile = "./src/main/full/config/config.properties";
		//final String configFile = "./src/test/config/testConfig.properties";
		//final String configFile = "./src/test/config/miniTestConfig.properties";
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

		//create route set generator
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		
		
		//CREATING UNIT TEMPRO MATRIX
		
		RealODMatrixTempro temproODM = RealODMatrixTempro.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Unit matrix after inter-zonal deletion: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproUnitMatrix.csv");
				
		
		/*
		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		//RealODMatrix rm = new RealODMatrix(temproODMatrixFile);
		RealODMatrixTempro rm2 = new RealODMatrixTempro(temproODMatrixFile, zoning);
		rm2.saveMatrixFormatted("testingSparse200kmMatrix.csv");
				
		
		//generate single node routes
		rsg.generateSingleNodeRoutes();
		LOGGER.debug(rsg.getStatistics());
		//read tempro routes
		rsg.readRoutesBinaryWithoutValidityCheck(temproRoutesFile);
		LOGGER.debug(rsg.getStatistics());
		//rsg.generateRouteSetZoneToZone("E06000053", "E06000053");
		//rsg.generateRouteSetZoneToZoneTempro("E02006781", "E02006781", zoning);
		//rsg.printStatistics();
	
		 */
		 		
		//change the zone to node mapping a bit
//		zoning.getZoneToNearestNodeIDMap().put("E02003553", 9);
//		zoning.getZoneToNearestNodeIDMap().put("E02003559", 8);
//		zoning.getZoneToNearestNodeIDMap().put("E02003565", 7);
//		zoning.getZoneToNearestNodeIDMap().put("E02003576", 48);
//		zoning.getZoneToNearestNodeIDMap().put("E02003578", 95);
//		zoning.getZoneToNearestNodeIDMap().put("E02003577", 87);
//		zoning.getZoneToNearestNodeIDMap().put("E02003571", 86);
//		zoning.getZoneToNearestNodeIDMap().put("E02003569", 102);
//		zoning.getZoneToNearestNodeIDMap().put("E02003567", 96);
		
		/*
		
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
		System.out.println("Tempro zone ID to tempro zone code map: " + zoning.getZoneIDToCodeMap()); //oout of memory java heap space!
		System.out.println(zoning.getZoneToSortedListOfNodeAndDistancePairs());
		System.out.println(zoning.getZoneToListOfContaintedNodes());
		*/
		
		/*
		ODMatrix temproODM = ODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
		rna.assignPassengerFlowsTempro(temproODM, zoning, rsg);
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
		
		//calculate and save distance skim matrix
		SkimMatrix distanceSkimMatrix = rna.calculateDistanceSkimMatrixTempro();
		distanceSkimMatrix.saveMatrixFormatted(outputFolder + "distanceSkimMatrix.csv");
		//load distance skim matrix
		//SkimMatrix distanceSkimMatrix = new SkimMatrix(outputFolder + "distanceSkimMatrix.csv");
		
		//save routes binary
		//rsg.saveRoutesBinary(outputFolder + "routesTempro.dat", false);
		*/	
		
		/*
		
		//EstimatedODMatrix odmpa = new EstimatedODMatrix("./src/main/resources/data/passengerProductionsAttractions.csv", distanceSkimMatrix, BIN_LIMITS_KM, OTLD);
		final String productionsAttractionsTemproFile = props.getProperty("productionsAttractionsTemproFile");
		EstimatedODMatrix odmpa = new EstimatedODMatrix(productionsAttractionsTemproFile, distanceSkimMatrix, BIN_LIMITS_KM, OTLD);

		odmpa.printMatrixFormatted();
		odmpa.getBinIndexMatrix().printMatrixFormatted("Bin index matrix:");
		
		odmpa.createUnitMatrix();
		odmpa.printMatrixFormatted("Unit matrix:");
		
		odmpa.scaleToProductions();
		odmpa.printMatrixFormatted("Scaled to productions:");
		
		odmpa.scaleToAttractions();
		odmpa.printMatrixFormatted("Scaled to attractions:");
		
		//odmpa.deleteInterzonalFlows("E06000053");
		//odmpa.printMatrixFormatted("After deleting interzonal flows for zone E06000053");
		
		odmpa.scaleToObservedTripLenghtDistribution();
		odmpa.printMatrixFormatted("Scaled to observed trip length distribution:");
		
		for (int i=0; i<10; i++) odmpa.iterate();
		
		odmpa.printMatrixFormatted("After 10 further iterations:");
		odmpa.saveMatrixFormatted("ODMatrix.csv");
			 
		*/
		
		//roadNetwork.sortGravityNodes(); //probably not relevant
		
		//ODMatrix temproODM1 = ODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
		//System.out.println("Full unit matrix: " + temproODM1.getSumOfFlows());
		//temproODM1.saveMatrixFormatted("testTemproUnitMatrix.csv");
		//temproODM.deleteInterzonalFlows("E02006781"); //Isle of Scilly
		//temproODM.saveMatrixFormatted("fullScaleTemproUnitMatrix.csv");
		
		
		//CREATING SPARSE TEMPRO OD MATRICES
		/*
		ODMatrix temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 10000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 10 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix10km.csv");
		
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 100000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 100 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix100km.csv");
		
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 150000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 150 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix150km.csv");
		*/
		
		
		/*
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 200000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 200 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix200km.csv");
		
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 300000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 300 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix300km.csv");
		
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 400000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 400 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix400km.csv");
		
		temproODM = ODMatrix.createSparseUnitMatrix(zoning.getZoneCodeToIDMap().keySet(), zoning.getZoneToCentroid(), 500000);
		//System.out.println("Sparse unit matrix: " + temproODM.getSumOfFlows());
		temproODM.deleteInterzonalFlows("E02006781");
		System.out.println("Sparse unit matrix after inter-zonal deletion with 500 km threshold: " + temproODM.getSumOfFlows());
		temproODM.saveMatrixFormatted("fullSCaleTemproSparseUnitMatrix500km.csv");
		
		*/
		
		/*
//		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		RebalancedTemproODMatrix rodm = new RebalancedTemproODMatrix(temproODMatrixFile, rna, rsg, zoning, props);
		
		System.out.println("Created rodm");
		
		rodm.iterate(10); //all matrices will be saved, the latest one is the one
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> RMSNvalues = rodm.getRMSNvalues();
		for (int i = 0; i < RMSNvalues.size(); i++) lineDataset.addValue(RMSNvalues.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "RMSN values");
		line.setSize(600, 400);
		line.setVisible(true);
		line.saveToPNG("temproRebalancing.png");
		*/
		
		/*
//		ODMatrix todm = new ODMatrix(outputFolder + "balancedTemproODMatrix.csv");
//		RealODMatrix temproODM = RealODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
			
		RebalancedTemproODMatrix rodm = new RebalancedTemproODMatrix(temproODM.getOrigins(), temproODM.getDestinations(), rna, rsg, zoning, props);
		
		temproODM = null;
		
		rodm.deleteInterzonalFlows("E02006781"); //Isle of Scilly in Tempro
		rodm.printMatrixFormatted("Initial rebalanced matrix:", 2);
		
		//TODO
		//set rodm to todm
		
		rodm.iterate(10);
		
		//for (int i=0; i<30; i++)	rodm.scaleNodeMatricesToTrafficCounts();
		
		
		//round values
		ODMatrix odm = new ODMatrix(rodm);
		odm.printMatrixFormatted("Rounded values:");
		odm.saveMatrixFormatted("rebalancedODMtempro.csv");
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> RMSNvalues = rodm.getRMSNvalues();
		for (int i = 0; i < RMSNvalues.size(); i++) lineDataset.addValue(RMSNvalues.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "RMSN values");
		line.setSize(600, 400);
		line.setVisible(true);
		line.saveToPNG("rebalancing.png");
		
		*/
		
		
		/*
		
		//increase particular interzonal flows
		//odm.setFlow("E02003554", "E02003554", 300);
		
		System.out.println("Gravitating population: ");
		System.out.println("Node 40 = " + roadNetwork.getGravitatingPopulation(40));
		System.out.println("Node 55 = " + roadNetwork.getGravitatingPopulation(55));
		System.out.println("Node 9 = " + roadNetwork.getGravitatingPopulation(9));
	
		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripStorages();

		//assign passenger flows
		rna.assignPassengerFlowsTempro(odm, zoning, rsg); //routing version with tempro zones
		rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation

		//calculate RMSN
		double RMSN = rna.calculateRMSNforSimulatedVolumes();
		System.out.println("RMSN after scaling = " +  RMSN);
		
		rodm.getRMSNvalues().add(RMSN);
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> RMSNvalues = rodm.getRMSNvalues();
		for (int i = 0; i < RMSNvalues.size(); i++) lineDataset.addValue(RMSNvalues.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "RMSN values");
		line.setSize(600, 400);
		line.setVisible(true);
		
//		Map<Integer, Integer> map1 = rna.calculateAbsDifferenceCarCounts();
//		Map<Integer, Double> map2 = new HashMap<Integer, Double>();
//		for (Integer link: map1.keySet()) map2.put(link, (double) map1.get(link)); 
		
		
//		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
//			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted(zone);
//		}
//		
//		rodm.modifyNodeMatrixProbabilitiesForInterzonalTrips();
		
		
		
		final URL temproUrl = new URL(props.getProperty("temproZonesUrl"));
		//NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "CountDiff", null);
		NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "CountDiff", null, temproUrl);
		
		
		*/
		
		/*
		
		
//		ODMatrix temproODM = ODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
		RealODMatrix temproODM = RealODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
//		RebalancedODMatrix rodm = new RebalancedODMatrix(temproODM.getOrigins(), temproODM.getDestinations(), rna, rsg, zoning);
//		
//		rodm.iterate(100);
		
		//round values
		ODMatrix odm = new ODMatrix(temproODM);

		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripStorages();

		//assign passenger flows
		rna.assignPassengerFlowsTempro(odm, zoning, rsg); //routing version with tempro zones
		rna.expandTripList(); //if fractional assignment used
		rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation
				
		for (int i=0; i<30; i++) {


			List<String> origins = temproODM.getOrigins();
			List<String> destinations = temproODM.getDestinations();

			for (String origin: origins)
				for (String destination: destinations) {
					double flow = temproODM.getFlow(origin, destination);
					if (flow >= 1.0) {
						Integer originNode = zoning.getZoneToNearestNodeIDMap().get(origin);
						Integer destinationNode = zoning.getZoneToNearestNodeIDMap().get(destination);
						RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
						if (rs == null) {
							System.err.printf("Cannot fetch route set between node %d and node %d! %n", originNode, destinationNode);
							continue;
						}

						Route foundRoute = null;
						if (rs != null) foundRoute = rs.getChoiceSet().get(0);

						double averageCorrectionFactor = 0.0;
						int edgeCounter = 0;
						for (DirectedEdge edge: foundRoute.getEdges()) {

							double linkVolume = rna.getLinkVolumePerVehicleType().get(VehicleType.CAR).get(edge.getID());
							Integer expectedCount = roadNetwork.getAADFCarTrafficCounts().get(edge.getID());
							if (expectedCount == null) continue; //no counts
							double correctionFactor = expectedCount / linkVolume;
							averageCorrectionFactor += correctionFactor;
							edgeCounter++;
						}
						if (edgeCounter == 0) continue; //there is no correction factor
						averageCorrectionFactor /= edgeCounter;

						double newflow = flow * averageCorrectionFactor;
						temproODM.setFlow(origin, destination, newflow);
					}
				}

			temproODM.printMatrixFormatted("Matrix after scaling: ");

			//round values
			odm = new ODMatrix(temproODM);

			//reset as we are re-using the same road network assignment
			rna.resetLinkVolumes();
			rna.resetTripStorages();

			//assign passenger flows
			rna.assignPassengerFlowsTempro(odm, zoning, rsg); //routing version with tempro zones
			rna.expandTripList(); //if fractional assignment used
			rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation

			//calculate RMSN
			double RMSN = rna.calculateRMSNforSimulatedVolumes();
			System.out.println("RMSN after scaling = " +  RMSN);

		}
		*/
	}

	@Test
	public void test() throws FileNotFoundException, IOException {

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

		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		
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

		//create route set generator
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		rsg.generateSingleNodeRoutes();
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.calculateAllPathsizes();
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
				
		roadNetwork.sortGravityNodes();

		ODMatrix passengerODM = new ODMatrix(temproODMatrixFile);
		passengerODM = ODMatrix.createUnitMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations());
					
		RebalancedTemproODMatrix rodm = new RebalancedTemproODMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations(), rna, rsg, zoning, props);
		//rodm.printMatrixFormatted("Initial rebalanced matrix:", 2);
		rodm.iterate(2);
		
		System.out.println("RMSN values: " + rodm.getRMSNvalues());
	}
}
