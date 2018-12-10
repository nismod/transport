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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the ODMatrix class
 * @author Milan Lovric
 *
 */
public class ODMatrixTest {

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
		
		//   1   2   3   4
		//-----------------
		//1 123 234 345 456
		//2 321 432 543 654
		//3 987 876 765 654
		//4 456 567 678 789
		
		ODMatrixMultiKey passengerODMatrix = new ODMatrixMultiKey();
		
		passengerODMatrix.setFlow("1", "1", 123);
		passengerODMatrix.setFlow("1", "2", 234);
		passengerODMatrix.setFlow("1", "3", 345);
		passengerODMatrix.setFlow("1", "4", 456);
		passengerODMatrix.setFlow("2", "1", 321);
		passengerODMatrix.setFlow("2", "2", 432);
		passengerODMatrix.setFlow("2", "3", 543);
		passengerODMatrix.setFlow("2", "4", 654);
		passengerODMatrix.setFlow("3", "1", 987);
		passengerODMatrix.setFlow("3", "2", 876);
		passengerODMatrix.setFlow("3", "3", 765);
		passengerODMatrix.setFlow("3", "4", 654);
		passengerODMatrix.setFlow("4", "1", 456);
		passengerODMatrix.setFlow("4", "2", 567);
		passengerODMatrix.setFlow("4", "3", 678);
		passengerODMatrix.setFlow("4", "4", 987);
	
		passengerODMatrix.printMatrixFormatted("OD matrix:");
		ODMatrixMultiKey copy = passengerODMatrix.clone();
		copy.printMatrixFormatted("Cloned matrix:");
		
		passengerODMatrix.setFlow("4",  "4", 0);
		passengerODMatrix.printMatrixFormatted("OD matrix:");
		copy.printMatrixFormatted("Cloned matrix:");		

		boolean condition = 		
				passengerODMatrix.getFlow("1", "1") == 123 &&
				passengerODMatrix.getFlow("1", "2") == 234 &&
				passengerODMatrix.getFlow("1", "3") == 345 &&
				passengerODMatrix.getFlow("1", "4") == 456 &&
				passengerODMatrix.getFlow("2", "1") == 321 &&
				passengerODMatrix.getFlow("2", "2") == 432 &&
				passengerODMatrix.getFlow("2", "3") == 543 &&
				passengerODMatrix.getFlow("2", "4") == 654 &&
				passengerODMatrix.getFlow("3", "1") == 987 &&
				passengerODMatrix.getFlow("3", "2") == 876 &&
				passengerODMatrix.getFlow("3", "3") == 765 &&
				passengerODMatrix.getFlow("3", "4") == 654 &&
				passengerODMatrix.getFlow("4", "1") == 456 &&
				passengerODMatrix.getFlow("4", "2") == 567 &&
				passengerODMatrix.getFlow("4", "3") == 678 &&
				passengerODMatrix.getFlow("4", "4") == 0;
	
		assertTrue("All matrix elements are correct", condition);
		
		assertEquals("An element of the cloned matrix is correct", 987, copy.getFlow("4",  "4"));
		
		System.out.println("Origins: " + passengerODMatrix.getSortedOrigins());
		System.out.println("Destinations: " + passengerODMatrix.getSortedDestinations());
		System.out.println("Trip starts: " + passengerODMatrix.calculateTripStarts());
		System.out.println("Trip ends: " + passengerODMatrix.calculateTripEnds());
		
		ODMatrixMultiKey passengerODMatrix2 = new ODMatrixMultiKey("./src/test/resources/testdata/csvfiles/passengerODM.csv");
		passengerODMatrix2.printMatrixFormatted();
		
		condition = passengerODMatrix2.getFlow("E06000045", "E06000045") == 5000 &&
					passengerODMatrix2.getFlow("E06000045", "E07000086") == 5500 &&
					passengerODMatrix2.getFlow("E06000045", "E07000091") == 2750 &&
					passengerODMatrix2.getFlow("E06000045", "E06000046") == 150 &&
					passengerODMatrix2.getFlow("E07000086", "E06000045") == 6500 &&
					passengerODMatrix2.getFlow("E07000086", "E07000086") == 5500 &&
					passengerODMatrix2.getFlow("E07000086", "E07000091") == 900 &&
					passengerODMatrix2.getFlow("E07000086", "E06000046") == 120 &&
					passengerODMatrix2.getFlow("E07000091", "E06000045") == 4560 &&
					passengerODMatrix2.getFlow("E07000091", "E07000086") == 1400 &&
					passengerODMatrix2.getFlow("E07000091", "E07000091") == 6000 &&
					passengerODMatrix2.getFlow("E07000091", "E06000046") == 100 &&
					passengerODMatrix2.getFlow("E06000046", "E06000045") == 200 &&
					passengerODMatrix2.getFlow("E06000046", "E07000086") == 150 &&
					passengerODMatrix2.getFlow("E06000046", "E07000091") == 90 &&
					passengerODMatrix2.getFlow("E06000046", "E06000046") == 1000;
				
		assertTrue("All matrix elements are correct", condition);
		
		System.out.println("Origins: " + passengerODMatrix2.getSortedOrigins());
		System.out.println("Destinations: " + passengerODMatrix2.getSortedDestinations());
		System.out.println("Trip starts: " + passengerODMatrix2.calculateTripStarts());
		System.out.println("Trip ends: " + passengerODMatrix2.calculateTripEnds());
		
		passengerODMatrix2.scaleMatrixValue(2.0);
		passengerODMatrix2.printMatrixFormatted("After scaling:");
		
		
		List<String> zones = new ArrayList<String>();
		zones.add("E06000045");
		zones.add("E07000086");
		zones.add("E07000091");
		zones.add("E06000046");
		
		Set<String> zoneSet = new HashSet<String>();
		zones.add("E06000045");
		zones.add("E07000086");
		zones.add("E07000091");
		zones.add("E06000046");
		
		ODMatrixMultiKey odm = ODMatrixMultiKey.createUnitMatrix(zones);
		odm.printMatrixFormatted();
		
		ODMatrixMultiKey odm2 = ODMatrixMultiKey.createUnitMatrix(zones, zones);
		odm2.printMatrixFormatted();
		
		ODMatrixMultiKey odm3 = ODMatrixMultiKey.createUnitMatrix(zoneSet);
		odm3.printMatrixFormatted();
		
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
		
		//E06000045 (E02003552, E02003553)
		//E07000091  (E02004801, E02004800)
		
		ODMatrixMultiKey tempro = new ODMatrixMultiKey();
		tempro.setFlow("E02004800", "E02003552", 1);
		tempro.setFlow("E02004800", "E02003553", 2);
		tempro.setFlow("E02004801", "E02003552", 3);
		tempro.setFlow("E02004801", "E02003553", 4);
		
		tempro.printMatrixFormatted("Tempro matrix:");
		ODMatrixMultiKey ladMatrix = ODMatrixMultiKey.createLadMatrixFromTEMProMatrix(tempro, zoning);
		ladMatrix.printMatrixFormatted("Lad matrix:");
		//ladMatrix.saveMatrixFormatted("ladFromTempro.csv");
		
		List<String> origins = new ArrayList<String>();
		List<String> destinations = new ArrayList<String>();
		origins.add("E02004800");
		origins.add("E02004801");
		destinations.add("E02003553");
		System.out.println("Matrix subset sum: " + tempro.sumMatrixSubset(origins, destinations));
		assertEquals("Matrix subset sum is correct", 6, tempro.sumMatrixSubset(origins, destinations));
		
		ladMatrix.scaleMatrixValue(2);
		ladMatrix.printMatrixFormatted("Lad matrix after scaling:");
		
		ODMatrixMultiKey tempro2 = ODMatrixMultiKey.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		tempro2.printMatrixFormatted("New tempro from LAD:");
		
		ODMatrixMultiKey temproODM = new ODMatrixMultiKey("./src/test/resources/testdata/csvfiles/temproODM.csv");
		//temproODM.printMatrixFormatted("tempro");
		ladMatrix = ODMatrixMultiKey.createLadMatrixFromTEMProMatrix(temproODM, zoning);
		ladMatrix.printMatrixFormatted("from tempro to LAD:");
		
		assertEquals("Sum of flows of two matrices should be equal", temproODM.getSumOfFlows(), ladMatrix.getSumOfFlows());
		
		RealODMatrix real = new RealODMatrix();
		real.setFlow("E02004800", "E02003552", 1.0);
		real.setFlow("E02004800", "E02003553", 2.0);
		real.setFlow("E02004801", "E02003552", 3.0);
		real.setFlow("E02004801", "E02003553", 4.0);
		real.printMatrix();
		
		ODMatrixMultiKey matrix = new ODMatrixMultiKey(real);
		
		final double DELTA = 0.000001;
		double difference = matrix.getAbsoluteDifference(tempro);
		assertEquals("Difference between equal matrices is 0", 0.0, difference, DELTA);

		origins = new ArrayList<String>();
		origins.add("E02004800");
		destinations = new ArrayList<String>();
		destinations.add("E02003552");
		destinations.add("E02003553");
		tempro.getMatrixSubset(origins, destinations).printMatrixFormatted("Matrix subset:");
		
		tempro.printMatrixFormatted();
		tempro.deleteInterzonalFlows("E02004800");
		tempro.printMatrixFormatted();
		tempro.saveMatrixFormatted("./temp/tempro.csv");
		tempro.saveMatrixFormatted2("./temp/tempro2.csv");
		
	}
	
	@Test
	public void miniTest() throws FileNotFoundException, IOException {
		
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		ODMatrixArray miniODM = new ODMatrixArray("./src/test/resources/minitestdata/csvfiles/passengerODM.csv", zoning);
		
		miniODM.printMatrixFormatted("mini ODM");
		miniODM.saveMatrixFormatted("./temp/miniODM.csv");
		miniODM.saveMatrixFormattedList("./temp/miniODMList.csv");
		assertEquals("Trips ends are equal", miniODM.calculateTripEnds(), miniODM.calculateTripStarts());
		
		int totalFlow = miniODM.getTotalFlow();
		miniODM.deleteInterzonalFlows("E06000045");
		assertEquals("Flows are still the same", totalFlow, miniODM.getTotalFlow());
		
		miniODM.getUnsortedDestinations();
		miniODM.getUnsortedOrigins();
		miniODM.getSortedOrigins();
		miniODM.getSortedDestinations();
		
		int flow1 = miniODM.getFlow("E06000045", "E06000045");
		int flow2 = miniODM.getFlow(45, 45);
		int flow3 = miniODM.getIntFlow(45, 45);
		assertEquals("Flows are equal", flow1, flow2);
		assertEquals("Flows are equal", flow2, flow3);
		miniODM.setFlow(45, 45, 100);
		assertEquals("Flows are correct", 100, miniODM.getFlow(45,  45));
		miniODM.setFlow("E06000045", "E06000045", 200);
		assertEquals("Flows are correct", 200, miniODM.getFlow(45,  45));
		
		miniODM.scaleMatrixValue(0.5);
		assertEquals("Flows are correct", 100, miniODM.getFlow(45,  45));

		ODMatrixArray miniODM2 = new ODMatrixArray("./temp/miniODMList.csv", zoning);
		int diff = miniODM.getAbsoluteDifference(miniODM2);
		assertEquals("Absolute difference is correct", 4900, diff);
				
		ODMatrixArrayTempro miniTemproODM = new ODMatrixArrayTempro("./src/test/resources/minitestdata/csvfiles/temproODM.csv", zoning);
		
		miniTemproODM.printMatrixFormatted("mini tempro ODM");
				
		miniODM2.setFlow(45, 45, 317683);
		ODMatrixArrayTempro miniTemproODM2 = ODMatrixArray.createTEMProFromLadMatrix(miniODM2, miniTemproODM, zoning);
		diff = miniTemproODM2.getAbsoluteDifference(miniTemproODM);
		assertEquals("Matrices are the same", 0, diff);
		
		ODMatrixArrayTempro miniTemproODM3 = ODMatrixArrayTempro.createTEMProFromLadMatrix(miniODM2, miniTemproODM, zoning);
		diff = miniTemproODM3.getAbsoluteDifference(miniTemproODM);
		assertEquals("Matrices are the same", 0, diff);
		
		ODMatrixArray odm2 = ODMatrixArray.createLadMatrixFromTEMProMatrix(miniTemproODM2, zoning);
		RealODMatrix odm3 = ODMatrixArrayTempro.createLadMatrixFromTEMProMatrix(miniTemproODM3, zoning);
		assertEquals("Total flows are the same", odm2.getTotalIntFlow(), odm3.getTotalIntFlow());
		
		miniTemproODM2.getSortedOrigins();
		miniTemproODM2.getSortedDestinations();
		miniTemproODM2.getUnsortedOrigins();
		miniTemproODM2.getUnsortedDestinations();
		
		assertEquals("Trips starts are equal", miniTemproODM2.calculateTripStarts(), miniTemproODM3.calculateTripStarts());
		assertEquals("Trips ends are equal", miniTemproODM2.calculateTripEnds(), miniTemproODM3.calculateTripEnds());

	}
}
