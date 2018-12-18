/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the SkimMatrixFreight class
 * @author Milan Lovric
 *
 */
public class SkimMatrixFreightTest {
	
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
		
		//  O   D   V   C
		//-----------------
		//  1   1   1   0.50
		//  1   1   2   1.00
		//  1   1   3   5.00
		//  1   2   1   2.00
		//  1   2   2   5.00
		//  1   2   3   6.00
		//  2   1   1   2.50
		//  2   1   2   1.50
		//  2   1   3   1.00
		
		SkimMatrixFreightMultiKey skimMatrixFreight = new SkimMatrixFreightMultiKey();
		
		skimMatrixFreight.setCost(1, 1, 1, 0.50);
		skimMatrixFreight.setCost(1, 1, 2, 1.00);
		skimMatrixFreight.setCost(1, 1, 3, 5.00);
		skimMatrixFreight.setCost(1, 2, 1, 2.00);
		skimMatrixFreight.setCost(1, 2, 2, 5.00);
		skimMatrixFreight.setCost(1, 2, 3, 6.00);
		skimMatrixFreight.setCost(2, 1, 1, 2.50);
		skimMatrixFreight.setCost(2, 1, 2, 1.50);
		skimMatrixFreight.setCost(2, 1, 3, 1.00);
		
		skimMatrixFreight.printMatrix();
		skimMatrixFreight.printMatrixFormatted();
		skimMatrixFreight.printMatrixFormatted("Skim matrix freight");
			
		boolean condition = 		
				skimMatrixFreight.getCost(1, 1, 1) == 0.50 &&
				skimMatrixFreight.getCost(1, 1, 2) == 1.00 &&
				skimMatrixFreight.getCost(1, 1, 3) == 5.00 &&
				skimMatrixFreight.getCost(1, 2, 1) == 2.00 &&
				skimMatrixFreight.getCost(1, 2, 2) == 5.00 &&
				skimMatrixFreight.getCost(1, 2, 3) == 6.00 &&
				skimMatrixFreight.getCost(2, 1, 1) == 2.50 &&
				skimMatrixFreight.getCost(2, 1, 2) == 1.50 &&
				skimMatrixFreight.getCost(2, 1, 3) == 1.00;
	
		assertTrue("All matrix elements are correct", condition);
		
		skimMatrixFreight.saveMatrixFormatted("./temp/skimMatrixFreight.csv");
		
		
		SkimMatrixFreightMultiKey skimMatrixFreight2 = new SkimMatrixFreightMultiKey("./src/test/resources/testdata/costSkimMatrixFreight.csv");
			
//		System.out.println(skimMatrixFreight2.getKeySet());
//		for (MultiKey mk: skimMatrixFreight2.getKeySet()) {
//			System.out.println(mk);
//			System.out.println("origin = " + mk.getKey(0));
//			System.out.println("destination = " + mk.getKey(1));
//			System.out.println("vehicleType = " + mk.getKey(2));
//			System.out.println("cost = " + skimMatrixFreight2.getCost((int)mk.getKey(0), (int)mk.getKey(1), (int)mk.getKey(2)));
//		}
	
		skimMatrixFreight2.printMatrixFormatted();
		
		condition = skimMatrixFreight2.getCost(854, 1312, 3) == 1.67 &&
					skimMatrixFreight2.getCost(855, 1312, 2) == 0.01 &&
					skimMatrixFreight2.getCost(1312, 855, 2) == 1.64 &&
					skimMatrixFreight2.getCost(867, 1312, 1) == 0.42 &&
					skimMatrixFreight2.getCost(1312, 867, 1) == 2.88 &&
					skimMatrixFreight2.getCost(867, 867, 1) == 3.00;
					
		assertTrue("Selected matrix elements are correct", condition);
		
		SkimMatrixFreightMultiKey skimMatrixFreight3 = new SkimMatrixFreightMultiKey("./temp/skimMatrixFreight.csv");
		
		double diff = skimMatrixFreight3.getAbsoluteDifference(skimMatrixFreight);
		
		final double DELTA = 0.000001;
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		FreightMatrix freightMatrix = new FreightMatrix();
		freightMatrix.setFlow(1, 1, 1, 10);
		freightMatrix.setFlow(1, 1, 2, 10);
		freightMatrix.setFlow(1, 1, 3, 10);
		freightMatrix.setFlow(1, 2, 1, 10);
		freightMatrix.setFlow(1, 2, 2, 10);
		freightMatrix.setFlow(1, 2, 3, 10);
		freightMatrix.setFlow(2, 1, 1, 10);
		freightMatrix.setFlow(2, 1, 2, 10);
		freightMatrix.setFlow(2, 1, 3, 10);

		double averageCost = skimMatrixFreight.getAverageCost();
		double demandWeigthedCost = skimMatrixFreight.getAverageCost(freightMatrix);
		assertEquals("Sum of weighted matrix costs is the same", averageCost, demandWeigthedCost, DELTA);
		
		SkimMatrixFreightArray skimMatrixFreight4 = new SkimMatrixFreightArray("./temp/skimMatrixFreight.csv");
		skimMatrixFreight4.printMatrixFormatted("Array based matrix:");
		
		diff = skimMatrixFreight4.getAbsoluteDifference(skimMatrixFreight);
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		averageCost = skimMatrixFreight4.getAverageCost();
		demandWeigthedCost = skimMatrixFreight4.getAverageCost(freightMatrix);
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
		
		SkimMatrixFreightArray smFreight = new SkimMatrixFreightArray();
		
		smFreight.setCost(1, 1, 1, 0.50);
		smFreight.setCost(1, 1, 2, 1.00);
		smFreight.setCost(1, 1, 3, 5.00);
		smFreight.setCost(1, 2, 1, 2.00);
		smFreight.setCost(1, 2, 2, 5.00);
		smFreight.setCost(1, 2, 3, 6.00);
		smFreight.setCost(2, 1, 1, 2.50);
		smFreight.setCost(2, 1, 2, 1.50);
		smFreight.setCost(2, 1, 3, 1.00);
		
		smFreight.printMatrixFormatted("Array skim matrix:");
		smFreight.saveMatrixFormatted("./temp/skimMatrixFreight.csv");
		
		SkimMatrixFreight smFreight2 = new SkimMatrixFreightArray("./temp/skimMatrixFreight.csv");
		smFreight2.printMatrixFormatted("Array skim matrix2:");
		diff = smFreight2.getAbsoluteDifference(smFreight);
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
				
		SkimMatrixFreight skimMatrixFreightMultiKey = new SkimMatrixFreightMultiKey("./temp/skimMatrixFreight.csv");
		skimMatrixFreightMultiKey.printMatrixFormatted("Multikey skim matrix:");
		
		final double EPSILON = 1e-5;
		assertEquals("Getter method works", 0.5, skimMatrixFreightMultiKey.getCost(1,1,1), EPSILON);
		
		diff = skimMatrixFreightMultiKey.getAbsoluteDifference(smFreight2);
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		System.out.println("Average cost: " + ((SkimMatrixFreightArray)smFreight2).getAverageCost());
		System.out.println("Average cost: " + ((SkimMatrixFreightArray)smFreight2).getAverageCost(freightMatrix));
				
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
															InputFileReader.readTimeOfDayDistributionFreightFile(timeOfDayDistributionFreightFile).get(BASE_YEAR),
															null,
															null,
															null,
															null,
															props);

		//assign passenger flows
		final String baseYearFreightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		FreightMatrix fm = new FreightMatrix(baseYearFreightMatrixFile);
		rna.assignFreightFlowsRouting(fm, null, props);
		fm.printMatrixFormatted("Freight matrix:");
		
		FreightMatrix fmAssigned = rna.calculateAssignedFreightMatrix();
		diff = fmAssigned.getAbsoluteDifference(fm);
		assertEquals("Matrices are the same", 0.0, diff, DELTA);
		
		rna.calculateTimeSkimMatrixFreight().printMatrixFormatted("Time skim matrix:");
	}
}
