/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the EstimatedODMatrix class
 * @author Milan Lovric
 *
 */
public class EstimatedODMatrixTest {
	
	public static void main( String[] args ) throws FileNotFoundException, IOException {
		
		double[] BIN_LIMITS_KM = {0.0, 0.621371, 1.242742, 3.106855, 6.21371, 15.534275, 31.06855, 62.1371, 93.20565, 155.34275, 217.47985};
		double[] OTLD = {0.05526, 0.16579, 0.34737, 0.21053, 0.15789, 0.03947, 0.01579, 0.00432, 0.00280, 0.00063, 0.00014};
		
		//final String configFile = "./src/main/config/config.properties";
		final String configFile = "./src/test/config/testConfig.properties";
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
				
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");
		final String vehicleTypeToPCUFile = props.getProperty("vehicleTypeToPCUFile");
		final String timeOfDayDistributionFile = props.getProperty("timeOfDayDistributionFile");
		final String baseFuelConsumptionRatesFile = props.getProperty("baseFuelConsumptionRatesFile");
		final int BASE_YEAR = Integer.parseInt(props.getProperty("baseYear"));
	
		//create a road network assignment
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, 
															InputFileReader.readEnergyUnitCostsFile(energyUnitCostsFile).get(BASE_YEAR),
															InputFileReader.readEngineTypeFractionsFile(engineTypeFractionsFile).get(BASE_YEAR),
															InputFileReader.readAVFractionsFile(AVFractionsFile).get(BASE_YEAR),
															InputFileReader.readVehicleTypeToPCUFile(vehicleTypeToPCUFile),
															InputFileReader.readEnergyConsumptionParamsFile(baseFuelConsumptionRatesFile),
															InputFileReader.readTimeOfDayDistributionFile(timeOfDayDistributionFile),
															null,
															null,
															null,
															null,
															props);
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		ODMatrix temproODM = ODMatrix.createUnitMatrix(zoning.getZoneCodeToIDMap().keySet());
		rna.assignPassengerFlowsTempro(temproODM, zoning, rsg);
		rna.calculateDistanceSkimMatrixTempro().printMatrixFormatted();
				
		//SkimMatrix distanceSkimMatrix = new SkimMatrix("./distanceSkimMatrix.csv");
		SkimMatrix distanceSkimMatrix = rna.calculateDistanceSkimMatrixTempro();
				
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
		//odmpa.saveMatrixFormatted(outputFolder + "balancedTemproODMatrix.csv");
		ODMatrix estimatedODM = new ODMatrix(odmpa);
		
		rna.resetTripStorages();
		rna.resetLinkVolumes();
		rna.assignPassengerFlowsTempro(estimatedODM, zoning, rsg);
		double RMSN = rna.calculateRMSNforSimulatedVolumes();
		System.out.printf("RMSN = %.2f%% %n", RMSN);
	}

	@Test
	public void test() throws FileNotFoundException, IOException {

		HashMap<String, Integer> productions = new HashMap<String, Integer>();
		productions.put("1", 400);
		productions.put("2", 460);
		productions.put("3", 400);
		productions.put("4", 700);

		HashMap<String, Integer> attractions = new HashMap<String, Integer>();
		attractions.put("1", 260);
		attractions.put("2", 400);
		attractions.put("3", 500);
		attractions.put("4", 800);

		double[] BIN_LIMITS = {0, 4, 8, 12, 16, 20, 24};
		double[] OTLD = {0.25, 0.5, 0.15, 0.1, 0.0, 0.0};
		//{365, 962, 160, 150, 230, 95}; 
		//{0.074923547, 0.372579001, 0.072884811, 0.127930683, 0.22069317, 0.130988787};
		//{147, 731, 143, 251, 433, 257};

		SkimMatrix distanceSkimMatrix = new SkimMatrix();

		distanceSkimMatrix.setCost("1", "1", 1.2);
		distanceSkimMatrix.setCost("1", "2", 9.4);
		distanceSkimMatrix.setCost("1", "3", 16.0);
		distanceSkimMatrix.setCost("1", "4", 20.56);
		distanceSkimMatrix.setCost("2", "1", 8.31);
		distanceSkimMatrix.setCost("2", "2", 3.2);
		distanceSkimMatrix.setCost("2", "3", 12.0);
		distanceSkimMatrix.setCost("2", "4", 16.4);
		distanceSkimMatrix.setCost("3", "1", 15.3);
		distanceSkimMatrix.setCost("3", "2", 15.6);
		distanceSkimMatrix.setCost("3", "3", 4.1);
		distanceSkimMatrix.setCost("3", "4", 6.54);
		distanceSkimMatrix.setCost("4", "1", 23.6);
		distanceSkimMatrix.setCost("4", "2", 19.5);
		distanceSkimMatrix.setCost("4", "3", 6.78);
		distanceSkimMatrix.setCost("4", "4", 7.5);

		//distanceSkimMatrix.setCost("4",  "4", 0);
		distanceSkimMatrix.printMatrixFormatted();
		
		EstimatedODMatrix odmpa = new EstimatedODMatrix(productions, attractions, distanceSkimMatrix, BIN_LIMITS, OTLD);

		odmpa.printMatrixFormatted();
		odmpa.getBinIndexMatrix().printMatrixFormatted();
		
//		odmpa.deleteInterzonalFlows("3");
//		odmpa.printMatrixFormatted("After deletion of interzonal flows for zone 3:");
		
		for (int i=0; i<1; i++) {

			odmpa.scaleToProductions();
			odmpa.printMatrixFormatted();

			odmpa.scaleToAttractions();
			odmpa.printMatrixFormatted();

			System.out.println("TLD = " + Arrays.toString(odmpa.getTripLengthDistribution()));
			System.out.println("OTLD = " + Arrays.toString(odmpa.getObservedTripLengthDistribution()));
			odmpa.updateTripLengthDistribution();
			System.out.println("TLD after update = " + Arrays.toString(odmpa.getTripLengthDistribution()));

			odmpa.scaleToObservedTripLenghtDistribution();
			
			odmpa.roundMatrixValues();
			
			odmpa.updateTripLengthDistribution();
			System.out.println("TLD after scaling to OTLD = " + Arrays.toString(odmpa.getTripLengthDistribution()));
			odmpa.printMatrixFormatted();
		}
/*
		SkimMatrix distanceSkimMatrix2 = new SkimMatrix();

		distanceSkimMatrix2.setCost("E06000045", "E06000045", 0.5);
		distanceSkimMatrix2.setCost("E06000045", "E07000086", 1.1);
		distanceSkimMatrix2.setCost("E06000045", "E07000091", 2.2); 
		distanceSkimMatrix2.setCost("E06000045", "E06000046", 4.8);
		distanceSkimMatrix2.setCost("E07000086", "E06000045", 6.5);
		distanceSkimMatrix2.setCost("E07000086", "E07000086", 5.5); 
		distanceSkimMatrix2.setCost("E07000086", "E07000091", 9.0);
		distanceSkimMatrix2.setCost("E07000086", "E06000046", 12.0);
		distanceSkimMatrix2.setCost("E07000091", "E06000045", 4.56);
		distanceSkimMatrix2.setCost("E07000091", "E07000086", 14.0);
		distanceSkimMatrix2.setCost("E07000091", "E07000091", 36.0);
		distanceSkimMatrix2.setCost("E07000091", "E06000046", 10.0);
		distanceSkimMatrix2.setCost("E06000046", "E06000045", 20.43);
		distanceSkimMatrix2.setCost("E06000046", "E07000086", 15.12);
		distanceSkimMatrix2.setCost("E06000046", "E07000091", 69.4);
		distanceSkimMatrix2.setCost("E06000046", "E06000046", 3.4);

		System.out.println("Distance skim matrix:");
		distanceSkimMatrix.printMatrixFormatted();

		double[] BIN_LIMITS_KM2 = {0.0, 0.621371, 1.242742, 3.106855, 6.21371, 15.534275, 31.06855, 62.1371, 93.20565, 155.34275, 217.47985};
		double[] OTLD2 = {0.05526, 0.16579, 0.34737, 0.21053, 0.15789, 0.03947, 0.01579, 0.00432, 0.00280, 0.00063, 0.00014};

		EstimatedODMAtrix odmpa2 = new EstimatedODMAtrix("./src/test/resources/testdata/passengerProductionsAttractions.csv", distanceSkimMatrix2, BIN_LIMITS_KM2, OTLD2);

		odmpa2.createUnitMatrix();
		odmpa2.scaleToProductions();
		odmpa2.scaleToAttractions();
		odmpa2.scaleToObservedTripLenghtDistribution();
		odmpa2.roundMatrixValues();
		odmpa2.printMatrixFormatted();

		System.out.println("Bin index matrix:");
		odmpa2.getBinIndexMatrix().printMatrixFormatted();

		System.out.println("Trip length distribution: " + Arrays.toString(odmpa2.getTripLengthDistribution()));
		odmpa2.updateTripLengthDistribution();
		System.out.println("Trip length distribution after scaling to OTLD: " + Arrays.toString(odmpa2.getTripLengthDistribution()));
		System.out.println("Observed trip length distribution: " + Arrays.toString(odmpa2.getObservedTripLengthDistribution()));

		for (int i=0; i<10; i++) {

			odmpa2.iterate();
			
			System.out.println("TLD = " + Arrays.toString(odmpa2.getTripLengthDistribution()));
			System.out.println("OTLD = " + Arrays.toString(odmpa2.getObservedTripLengthDistribution()));
			System.out.println(	Arrays.stream(odmpa2.getTripLengthDistribution()).sum() );
			System.out.println(	Arrays.stream(odmpa2.getObservedTripLengthDistribution()).sum() );
		}
		
		odmpa2.printMatrixFormatted("Final OD matrix:");
		
		odmpa2.deleteInterzonalFlows("E07000086");
		odmpa2.printMatrixFormatted("After deletion of interzonal flows from/to E07000086:");
	
		*/
	}
}
