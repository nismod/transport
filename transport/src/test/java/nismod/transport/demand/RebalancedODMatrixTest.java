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
import java.util.List;
import java.util.Properties;

import org.apache.sanselan.ImageWriteException;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.visualisation.LineVisualiser;

/**
 * Tests for the RebalancedODMatrix class
 * @author Milan Lovric
 *
 */
public class RebalancedODMatrixTest {
	
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
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.printStatistics();
		rsg.generateRouteSetZoneToZone("E06000053", "E06000053");
		rsg.printStatistics();
		
		roadNetwork.sortGravityNodes();

		ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);
//		rna.assignPassengerFlowsRouteChoice(passengerODM, rsg, props);
//		rna.updateLinkVolumePerVehicleType();
//		rna.printRMSNstatistic();
//		rna.printGEHstatistic();
//		rna.resetLinkVolumes();
//		rna.resetTripStorages();
				
		passengerODM = ODMatrix.createUnitMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations());
		passengerODM.deleteInterzonalFlows("E06000053"); //delete flows from/to Isle of Scilly
		passengerODM.printMatrixFormatted("Initial OD matrix:");
		
		RebalancedODMatrix rodm = new RebalancedODMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations(), rna, rsg, props);
		rodm.deleteInterzonalFlows("E06000053");
		rodm.printMatrixFormatted("Initial rebalanced matrix:", 2);
		
		rodm.iterate(50);
		
		//round values
		ODMatrix odm = new ODMatrix(rodm);
		odm.printMatrixFormatted("Rounded values:");
		odm.saveMatrixFormatted("rebalancedODM2.csv");
		
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		List<Double> RMSNvalues = rodm.getRMSNvalues();
		for (int i = 0; i < RMSNvalues.size(); i++) lineDataset.addValue(RMSNvalues.get(i), "RMSN", Integer.toString(i));
		LineVisualiser line = new LineVisualiser(lineDataset, "RMSN values");
		line.setSize(600, 400);
		line.setVisible(true);
		line.saveToPNG("rebalancing.png");
		
	
		//NetworkVisualiser.visualise(roadNetwork, "Network with count comparison", rna.calculateDirectionAveragedAbsoluteDifferenceCarCounts(), "CountDiff", null);
		
		
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

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
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
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		
		roadNetwork.sortGravityNodes();

		ODMatrix passengerODM = new ODMatrix(baseYearODMatrixFile);
		passengerODM = ODMatrix.createUnitMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations());
					
		RebalancedODMatrix rodm = new RebalancedODMatrix(passengerODM.getSortedOrigins(), passengerODM.getSortedDestinations(), rna, rsg, props);
		rodm.printMatrixFormatted("Initial rebalanced matrix:", 2);
		
		rodm.iterate(2);
		System.out.println("RMSN values: " + rodm.getRMSNvalues());
		
		//round values
		ODMatrix odm = new ODMatrix(rodm);
		odm.printMatrixFormatted("Rounded values:");
	}
}
