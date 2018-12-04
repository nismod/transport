/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.Trip;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the SkimMatrix class
 * @author Milan Lovric
 *
 */
public class SkimMatrixTest {
	
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
	public void test() throws FileNotFoundException, IOException {
		
		SkimMatrixMultiKey skimMatrix = new SkimMatrixMultiKey(null);
		
		skimMatrix.setCost("E06000045", "E06000045", 6.2);
		skimMatrix.setCost("E06000045", "E07000086", 5.5);
		skimMatrix.setCost("E06000045", "E07000091", 12.55); 
		skimMatrix.setCost("E06000045", "E06000046", 4.8);
		skimMatrix.setCost("E07000086", "E06000045", 6.5);
		skimMatrix.setCost("E07000086", "E07000086", 5.5); 
		skimMatrix.setCost("E07000086", "E07000091", 9.0);
		skimMatrix.setCost("E07000086", "E06000046", 12.0);
		skimMatrix.setCost("E07000091", "E06000045", 4.56);
		skimMatrix.setCost("E07000091", "E07000086", 14.0);
		skimMatrix.setCost("E07000091", "E07000091", 6.0);
		skimMatrix.setCost("E07000091", "E06000046", 10.0);
		skimMatrix.setCost("E06000046", "E06000045", 20.43);
		skimMatrix.setCost("E06000046", "E07000086", 15.12);
		skimMatrix.setCost("E06000046", "E07000091", 9.4);
		//skimMatrix.setCost("E06000046", "E06000046", 6.2);
		
		skimMatrix.printMatrixFormatted();
		skimMatrix.saveMatrixFormatted("./temp/skimMatrix.csv");
		
		SkimMatrixMultiKey skimMatrix2 = new SkimMatrixMultiKey("./temp/skimMatrix.csv", null);
		double diff = skimMatrix2.getAbsoluteDifference(skimMatrix);
		
		final double DELTA = 0.000001;
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		double averageCost = skimMatrix2.getAverageCost();
		double sumOfCosts = skimMatrix2.getSumOfCosts();
		skimMatrix2.printMatrixFormatted();
		skimMatrix2.printMatrixFormatted("Skim matrix");
		
		ODMatrix odMatrix = new ODMatrix();
		odMatrix.setFlow("E06000045", "E06000045", 10);
		odMatrix.setFlow("E06000045", "E07000086", 10);
		odMatrix.setFlow("E06000045", "E07000091", 10); 
		odMatrix.setFlow("E06000045", "E06000046", 10);
		odMatrix.setFlow("E07000086", "E06000045", 10);
		odMatrix.setFlow("E07000086", "E07000086", 10); 
		odMatrix.setFlow("E07000086", "E07000091", 10);
		odMatrix.setFlow("E07000086", "E06000046", 10);
		odMatrix.setFlow("E07000091", "E06000045", 10);
		odMatrix.setFlow("E07000091", "E07000086", 10);
		odMatrix.setFlow("E07000091", "E07000091", 10);
		odMatrix.setFlow("E07000091", "E06000046", 10);
		odMatrix.setFlow("E06000046", "E06000045", 10);
		odMatrix.setFlow("E06000046", "E07000086", 10);
		odMatrix.setFlow("E06000046", "E07000091", 10);
		odMatrix.setFlow("E06000046", "E06000046", 10);
		
		double demandCosts = skimMatrix2.getSumOfCosts(odMatrix);
		assertEquals("Sum of weighted matrix costs is the same", sumOfCosts * 10, demandCosts, DELTA);
		
		double demandWeigthedCost = skimMatrix2.getAverageCost(odMatrix);
		assertEquals("Sum of weighted matrix costs is the same", averageCost, demandWeigthedCost, DELTA);
		
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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		SkimMatrix skimMatrixArray = new SkimMatrixArray(zoning);
		
		skimMatrixArray.setCost("E06000045", "E06000045", 6.2);
		skimMatrixArray.setCost("E06000045", "E07000086", 5.5);
		skimMatrixArray.setCost("E06000045", "E07000091", 12.55); 
		skimMatrixArray.setCost("E06000045", "E06000046", 4.8);
		skimMatrixArray.setCost("E07000086", "E06000045", 6.5);
		skimMatrixArray.setCost("E07000086", "E07000086", 5.5); 
		skimMatrixArray.setCost("E07000086", "E07000091", 9.0);
		skimMatrixArray.setCost("E07000086", "E06000046", 12.0);
		skimMatrixArray.setCost("E07000091", "E06000045", 4.56);
		skimMatrixArray.setCost("E07000091", "E07000086", 14.0);
		skimMatrixArray.setCost("E07000091", "E07000091", 6.0);
		skimMatrixArray.setCost("E07000091", "E06000046", 10.0);
		skimMatrixArray.setCost("E06000046", "E06000045", 20.43);
		skimMatrixArray.setCost("E06000046", "E07000086", 15.12);
		skimMatrixArray.setCost("E06000046", "E07000091", 9.4);
		//skimMatrixArray.setCost("E06000046", "E06000046", 6.2);
		
		skimMatrixArray.printMatrixFormatted();
		
		SkimMatrix skimMatrixArray2 = new SkimMatrixArray("./temp/skimMatrix.csv", zoning);
		diff = skimMatrixArray2.getAbsoluteDifference(skimMatrixArray);
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
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

		//assign passenger flows
		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		ODMatrix odm = new ODMatrix(baseYearODMatrixFile);
		rna.assignPassengerFlowsRouting(odm, null, props);
		//rna.assignPassengerFlowsRouteChoice(odm, null, props);
		odm.printMatrixFormatted("OD matrix:");
		rna.calculateAssignedODMatrix().printMatrixFormatted("Assigned matrix:");
		rna.calculateTimeSkimMatrix().printMatrixFormatted("Time skim matrix:");
	}
}
