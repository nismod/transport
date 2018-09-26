/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.utility.ConfigReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RealODMatrix class
 * @author Milan Lovric
 *
 */
public class RealODMatrixTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		
		//   1   2   3   4
		//-----------------
		//1 123 234 345 456
		//2 321 432 543 654
		//3 987 876 765 654
		//4 456 567 678 789
		
		RealODMatrix passengerODMatrix = new RealODMatrix();
		
		passengerODMatrix.setFlow("1", "1", 123.0);
		passengerODMatrix.setFlow("1", "2", 234.0);
		passengerODMatrix.setFlow("1", "3", 345.0);
		passengerODMatrix.setFlow("1", "4", 456.0);
		passengerODMatrix.setFlow("2", "1", 321.0);
		passengerODMatrix.setFlow("2", "2", 432.0);
		passengerODMatrix.setFlow("2", "3", 543.0);
		passengerODMatrix.setFlow("2", "4", 654.0);
		passengerODMatrix.setFlow("3", "1", 987.0);
		passengerODMatrix.setFlow("3", "2", 876.0);
		passengerODMatrix.setFlow("3", "3", 765.0);
		passengerODMatrix.setFlow("3", "4", 654.0);
		passengerODMatrix.setFlow("4", "1", 456.0);
		passengerODMatrix.setFlow("4", "2", 567.0);
		passengerODMatrix.setFlow("4", "3", 678.0);
		passengerODMatrix.setFlow("4", "4", 987.0);
	
		passengerODMatrix.printMatrixFormatted("OD matrix:", 2);
		RealODMatrix copy = passengerODMatrix.clone();
		copy.printMatrixFormatted("Cloned matrix:", 2);
		
		passengerODMatrix.setFlow("4",  "4", 0.0);
		passengerODMatrix.printMatrixFormatted("OD matrix:", 2);
		copy.printMatrixFormatted("Cloned matrix:", 2);		

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
		
		final double DELTA = 0.000001;
		
		assertEquals("An element of the cloned matrix is correct", 987, copy.getFlow("4",  "4"), DELTA);
		
		System.out.println("Origins: " + passengerODMatrix.getSortedOrigins());
		System.out.println("Destinations: " + passengerODMatrix.getSortedDestinations());
		System.out.println("Trip starts: " + passengerODMatrix.calculateTripStarts());
		System.out.println("Trip ends: " + passengerODMatrix.calculateTripEnds());
		
		RealODMatrix passengerODMatrix2 = new RealODMatrix("./src/test/resources/testdata/csvfiles/passengerODM.csv");
		passengerODMatrix2.printMatrixFormatted(2);

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
		passengerODMatrix2.printMatrixFormatted("After scaling:", 2);
		
		
		List<String> zones = new ArrayList<String>();
		zones.add("E06000045");
		zones.add("E07000086");
		zones.add("E07000091");
		zones.add("E06000046");
		
		RealODMatrix odm = RealODMatrix.createUnitMatrix(zones);
		odm.printMatrixFormatted(2);
		
		RealODMatrix odm2 = RealODMatrix.createUnitMatrix(zones, zones);
		odm2.printMatrixFormatted(2);
		
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
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		//E06000045 (E02003552, E02003553)
		//E07000091  (E02004801, E02004800)
		
		RealODMatrix tempro = new RealODMatrix();
		tempro.setFlow("E02004800", "E02003552", 1);
		tempro.setFlow("E02004800", "E02003553", 2);
		tempro.setFlow("E02004801", "E02003552", 3);
		tempro.setFlow("E02004801", "E02003553", 4);
		
		tempro.printMatrixFormatted("Tempro matrix:", 2);
		RealODMatrix ladMatrix = RealODMatrix.createLadMatrixFromTEMProMatrix(tempro, zoning);
		ladMatrix.printMatrixFormatted("Lad matrix:", 2);
		//ladMatrix.saveMatrixFormatted("ladFromTempro.csv");
		
		List<String> origins = new ArrayList<String>();
		List<String> destinations = new ArrayList<String>();
		origins.add("E02004800");
		origins.add("E02004801");
		destinations.add("E02003553");
		System.out.println("Matrix subset sum: " + tempro.sumMatrixSubset(origins, destinations));
		assertEquals("Matrix subset sum is correct", 6, tempro.sumMatrixSubset(origins, destinations), DELTA);
		
		ladMatrix.scaleMatrixValue(2);
		ladMatrix.printMatrixFormatted("Lad matrix after scaling:", 2);
		
		RealODMatrix tempro2 = RealODMatrix.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		tempro2.printMatrixFormatted("New tempro from LAD:", 2);
		
		RealODMatrix temproODM = new RealODMatrix("./src/test/resources/testdata/csvfiles/temproODM.csv");
		//temproODM.printMatrixFormatted("tempro", 2);
		ladMatrix = RealODMatrix.createLadMatrixFromTEMProMatrix(temproODM, zoning);
		ladMatrix.printMatrixFormatted("from tempro to LAD:", 2);

		assertEquals("Sum of flows of two matrices should be equal", temproODM.getSumOfFlows(), ladMatrix.getSumOfFlows(), DELTA);
		assertEquals("Sum of int flows of two matrices should be equal", temproODM.getTotalIntFlow(), ladMatrix.getTotalIntFlow());
		
		
		RealODMatrix real = new RealODMatrix();
		real.setFlow("E02004800", "E02003552", 1.0);
		real.setFlow("E02004800", "E02003553", 2.0);
		real.setFlow("E02004801", "E02003552", 3.0);
		real.setFlow("E02004801", "E02003553", 4.0);
		real.printMatrix();
		
		double difference = real.getAbsoluteDifference(tempro);
		assertEquals("Difference between equal matrices is 0", 0.0, difference, DELTA);

		origins = new ArrayList<String>();
		origins.add("E02004800");
		destinations = new ArrayList<String>();
		destinations.add("E02003552");
		destinations.add("E02003553");
		tempro.sumMatrixSubset(origins, destinations);
		
		tempro.printMatrixFormatted(2);
		tempro.deleteInterzonalFlows("E02004800");
		tempro.printMatrixFormatted(2);
		tempro.saveMatrixFormatted("./temp/realtempro.csv");
		tempro.saveMatrixFormatted2("./temp/realtempro2.csv");
	}
}
