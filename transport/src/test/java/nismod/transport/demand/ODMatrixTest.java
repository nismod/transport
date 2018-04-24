/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
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

	@Test
	public void test() throws FileNotFoundException, IOException {
		
		//   1   2   3   4
		//-----------------
		//1 123 234 345 456
		//2 321 432 543 654
		//3 987 876 765 654
		//4 456 567 678 789
		
		ODMatrix passengerODMatrix = new ODMatrix();
		
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
		ODMatrix copy = passengerODMatrix.clone();
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
		
		System.out.println("Origins: " + passengerODMatrix.getOrigins());
		System.out.println("Destinations: " + passengerODMatrix.getDestinations());
		System.out.println("Trip starts: " + passengerODMatrix.calculateTripStarts());
		System.out.println("Trip ends: " + passengerODMatrix.calculateTripEnds());
		
		ODMatrix passengerODMatrix2 = new ODMatrix("./src/test/resources/testdata/csvfiles/passengerODM.csv");
		passengerODMatrix2.printMatrixFormatted();
//		System.out.println(passengerODMatrix2.getKeySet());
//		for (MultiKey mk: passengerODMatrix2.getKeySet()) {
//			System.out.println(mk);
//			System.out.println("origin = " + mk.getKey(0));
//			System.out.println("destination = " + mk.getKey(1));
//			System.out.println("flow = " + passengerODMatrix2.getFlow((String)mk.getKey(0), (String)mk.getKey(1)));
//		}
		
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
		
		System.out.println("Origins: " + passengerODMatrix2.getOrigins());
		System.out.println("Destinations: " + passengerODMatrix2.getDestinations());
		System.out.println("Trip starts: " + passengerODMatrix2.calculateTripStarts());
		System.out.println("Trip ends: " + passengerODMatrix2.calculateTripEnds());
		
		passengerODMatrix2.scaleMatrixValue(2.0);
		passengerODMatrix2.printMatrixFormatted("After scaling:");
		
		
		List<String> zones = new ArrayList<String>();
		zones.add("E06000045");
		zones.add("E07000086");
		zones.add("E07000091");
		zones.add("E06000046");
		
		ODMatrix odm = ODMatrix.createUnitMatrix(zones);
		odm.printMatrixFormatted();
		
		ODMatrix odm2 = ODMatrix.createUnitMatrix(zones, zones);
		odm2.printMatrixFormatted();
		
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
		
		ODMatrix tempro = new ODMatrix();
		tempro.setFlow("E02004800", "E02003552", 1);
		tempro.setFlow("E02004800", "E02003553", 2);
		tempro.setFlow("E02004801", "E02003552", 3);
		tempro.setFlow("E02004801", "E02003553", 4);
		
		tempro.printMatrixFormatted("Tempro matrix:");
		ODMatrix ladMatrix = ODMatrix.createLadMatrixFromTEMProMatrix(tempro, zoning);
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
		
		ODMatrix tempro2 = ODMatrix.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		tempro2.printMatrixFormatted("New tempro from LAD:");
		
		//ODMatrix fullODM = new ODMatrix("./src/main/resources/data/csvfiles/balancedODMatrix.csv");
		//fullODM.printMatrixFormatted("Full ODM:");
		//ODMatrix subset = fullODM.getMatrixSubset(zones, zones);
		//subset.printMatrixFormatted("Fast-track subset:");
		//subset.saveMatrixFormatted("fastTrackODM.csv");
		
		ODMatrix temproODM = new ODMatrix("./src/test/resources/testdata/csvfiles/temproODM.csv");
		temproODM.printMatrixFormatted("tempro");
		ladMatrix = ODMatrix.createLadMatrixFromTEMProMatrix(temproODM, zoning);
		ladMatrix.printMatrixFormatted("from tempro to LAD:");
		//ladMatrix.saveMatrixFormatted("tempro2LAD.csv");
		//ladMatrix.scaleMatrixValue(0.1);
		//ladMatrix.saveMatrixFormatted("tempro2LADscaled.csv");
		
	}
}
