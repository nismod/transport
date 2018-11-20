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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.sanselan.ImageWriteException;
import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RealODMatrixTempro class
 * @author Milan Lovric
 *
 */
public class RealODMatrixTemproTest {
	
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

		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		
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

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		RealODMatrixTempro temproODM = new RealODMatrixTempro(temproODMatrixFile, zoning);
		//temproODM.deleteInterzonalFlows("E02006781");

//		//RealODMatrix ladODM = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(temproODM, zoning);
//		//ladODM.printMatrixFormatted("LAD matrix:", 2);
//		//ladODM.saveMatrixFormatted("ladFromTemproODM.csv");
//		
//		temproODM.saveMatrixFormatted2("temproMatrixListBased199.csv");
//		//temproODM.saveMatrixFormatted3("temproMatrixListBased198b.csv");
//		
//		System.in.read(); System.in.read();
//		
//		long timeNow;
//		
//		timeNow = System.currentTimeMillis();
//		RealODMatrixTempro tempro1 = new RealODMatrixTempro(temproODMatrixFile, zoning);
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Tempro matrix read in %d milliseconds.\n", timeNow);
//		System.out.println("Total flow: " + tempro1.getTotalIntFlow());
//		
//		System.in.read(); System.in.read();
//
//		timeNow = System.currentTimeMillis();
//		RealODMatrixTempro tempro2 = new RealODMatrixTempro("temproMatrixListBased199.csv", zoning);
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Tempro matrix read in %d milliseconds.\n", timeNow);
//		System.out.println("Total flow: " + tempro2.getTotalIntFlow());
//				
//		System.in.read(); System.in.read();
//		
//		timeNow = System.currentTimeMillis();
//		ODMatrix tempro3 = new ODMatrix(temproODMatrixFile);
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Tempro matrix read in %d milliseconds.\n", timeNow);
//		System.out.println("Total flow: " + tempro3.getTotalIntFlow());
//		
//		System.in.read(); System.in.read();
//		
//		timeNow = System.currentTimeMillis();
//		ODMatrix tempro4 = new ODMatrix("temproMatrixListBased199.csv");
//		timeNow = System.currentTimeMillis() - timeNow;
//		System.out.printf("Tempro matrix read in %d milliseconds.\n", timeNow);
//		System.out.println("Total flow: " + tempro4.getTotalIntFlow());
		
			
		System.in.read(); System.in.read();
		
		
		final String ladMatrixFile = props.getProperty("baseYearODMatrixFile");
		ODMatrix ladMatrix = new ODMatrix(ladMatrixFile);
		ladMatrix.scaleMatrixValue(2);
	
		ODMatrix baseTempro = new ODMatrix(temproODMatrixFile);
		RealODMatrixTempro todm = RealODMatrixTempro.createTEMProFromLadMatrix(ladMatrix, temproODM, zoning);
		ODMatrix odm = ODMatrix.createTEMProFromLadMatrix(ladMatrix, baseTempro, zoning);
		
		System.out.println(todm.getTotalIntFlow());
		System.out.println(odm.getTotalIntFlow());
		
		
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
	public void minitest() throws FileNotFoundException, IOException {
		
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
		
		RealODMatrixTempro tempro = new RealODMatrixTempro(zoning);
		tempro.setFlow("E02003565", "E02003568", 1.0);
		tempro.setFlow("E02003565", "E02003569", 2.0);
		tempro.setFlow("E02003566", "E02003568", 3.0);
		tempro.setFlow("E02003566", "E02003569", 4.0);
		//tempro.printMatrixFormatted("Tempro matrix:", 2);
		
		RealODMatrix real = new RealODMatrix();
		real.setFlow("E06000045", "E06000045", 10.0);
		real.printMatrixFormatted("LAD matrix", 2);
		
		RealODMatrix ladMatrix = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(tempro, zoning);
		ladMatrix.printMatrixFormatted("Lad matrix:", 2);
		
		RealODMatrixTempro tempro2 = RealODMatrixTempro.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		//tempro2.printMatrixFormatted("Tempro matrix 2",  2);
		
		final double DELTA = 0.00001;
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro2), DELTA);

		HashMap<String, Integer> tripEnds = tempro.calculateTripEnds();
		HashMap<String, Integer> tripStarts = tempro.calculateTripStarts();
		int sumTripEnds = 0, sumTripStarts = 0;
		for (int num: tripEnds.values()) sumTripEnds += num;
		for (int num: tripStarts.values()) sumTripStarts += num;
		assertEquals("Sum of trips ends and trip starts is equal", sumTripEnds, sumTripStarts);
				
		RealODMatrixTempro tempro3 = tempro.clone();
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro3), DELTA);
		
		tempro.setFlow("E02003565", "E02003568", 1.1);
		tempro.setFlow("E02003565", "E02003569", 2.4);
		tempro.setFlow("E02003566", "E02003568", 3.5);
		tempro.setFlow("E02003566", "E02003569", 4.9);
		
		tempro.ceilMatrixValues(); //2, 3, 4, 5
		assertEquals("Cell value is correct", 2, tempro.getFlow("E02003565", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 3, tempro.getFlow("E02003565", "E02003569"), DELTA);
		assertEquals("Cell value is correct", 4, tempro.getFlow("E02003566", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 5, tempro.getFlow("E02003566", "E02003569"), DELTA);
		
		tempro.setFlow("E02003565", "E02003568", 1.1);
		tempro.setFlow("E02003565", "E02003569", 2.4);
		tempro.setFlow("E02003566", "E02003568", 3.5);
		tempro.setFlow("E02003566", "E02003569", 4.9);
		
		tempro.floorMatrixValues(); //1, 2, 3, 4
		assertEquals("Cell value is correct", 1, tempro.getFlow("E02003565", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 2, tempro.getFlow("E02003565", "E02003569"), DELTA);
		assertEquals("Cell value is correct", 3, tempro.getFlow("E02003566", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 4, tempro.getFlow("E02003566", "E02003569"), DELTA);
		
		tempro.setFlow("E02003565", "E02003568", 1.1);
		tempro.setFlow("E02003565", "E02003569", 2.4);
		tempro.setFlow("E02003566", "E02003568", 3.5);
		tempro.setFlow("E02003566", "E02003569", 4.9);
		
		tempro.roundMatrixValues(); //1, 2, 4, 5
		assertEquals("Cell value is correct", 1, tempro.getFlow("E02003565", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 2, tempro.getFlow("E02003565", "E02003569"), DELTA);
		assertEquals("Cell value is correct", 4, tempro.getFlow("E02003566", "E02003568"), DELTA);
		assertEquals("Cell value is correct", 5, tempro.getFlow("E02003566", "E02003569"), DELTA);
		
		assertEquals("After rounding should be the same", tempro.getFlow("E02003565", "E02003568"), tempro.getIntFlow("E02003565", "E02003568"), DELTA);
		assertEquals("After rounding total flows should be the same", tempro.getTotalIntFlow(), tempro.getSumOfFlows(), DELTA);

		double before = tempro.getTotalIntFlow();
		tempro.scaleMatrixValue(2.0);
		assertEquals("After scaling total flows should be as expected", before * 2.0, tempro.getTotalIntFlow(), DELTA);
		
		tempro.scaleMatrixValue(tempro);

		ArrayList<String> origins, destinations;
		origins = new ArrayList<String>();
		origins.add("E02003565");
		destinations = new ArrayList<String>();
		destinations.add("E02003568");
		destinations.add("E02003569");
		
		tempro.sumMatrixSubset(origins, destinations);
		
		int numberOfZones = zoning.getZoneIDToCodeMap().length - 1;
		System.out.println("Number of zones = " + numberOfZones);
		
		//unit matrix
		tempro = RealODMatrixTempro.createUnitMatrix(zoning);
		assertEquals("Total flows for unit matrix should be as expected", numberOfZones*numberOfZones, tempro.getSumOfFlows(), DELTA);
		
		tempro.deleteInterzonalFlows("E02003565");
		//unit1.printMatrixFormatted(2);
		assertEquals("Total flows after interzonal deletion should be as expected", numberOfZones*numberOfZones - 2*(numberOfZones-1), tempro.getSumOfFlows(), DELTA);

		tempro.saveMatrixFormatted("./temp/unitTempro.csv");
		tempro.saveMatrixFormatted2("./temp/unitTempro2.csv");
		tempro.saveMatrixFormatted3("./temp/unitTempro3.csv");
		
		tempro = new RealODMatrixTempro("./temp/unitTempro.csv", zoning);
		int flow1 = tempro.getTotalIntFlow();
		
		tempro = new RealODMatrixTempro("./temp/unitTempro2.csv", zoning);
		int flow2 = tempro.getTotalIntFlow();
		assertEquals("Total flows should be the same for two formats", 	flow1, flow2);
	
		tempro = RealODMatrixTempro.createUnitMatrix(origins, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 1.0, tempro.getSumOfFlows(), DELTA);

		tempro = RealODMatrixTempro.createUnitMatrix(origins, destinations, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 2.0, tempro.getSumOfFlows(), DELTA);
	}
	
	//@Test
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

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		//E06000045 (E02003552, E02003553)
		//E07000091  (E02004801, E02004800)
		
		RealODMatrixTempro tempro = new RealODMatrixTempro(zoning);
		tempro.setFlow("E02004800", "E02003552", 1.0);
		tempro.setFlow("E02004800", "E02003553", 2.0);
		tempro.setFlow("E02004801", "E02003552", 3.0);
		tempro.setFlow("E02004801", "E02003553", 4.0);
		//tempro.printMatrixFormatted("Tempro matrix:", 2);
		
		RealODMatrix real = new RealODMatrix();
		real.setFlow("E07000091", "E06000045", 10.0);
		real.printMatrixFormatted("LAD matrix", 2);
		
		RealODMatrix ladMatrix = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(tempro, zoning);
		ladMatrix.printMatrixFormatted("Lad matrix:", 2);
		
		RealODMatrixTempro tempro2 = RealODMatrixTempro.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		//tempro2.printMatrixFormatted("Tempro matrix 2",  2);
		
		final double DELTA = 0.00001;
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro2), DELTA);

		HashMap<String, Integer> tripEnds = tempro.calculateTripEnds();
		HashMap<String, Integer> tripStarts = tempro.calculateTripStarts();
		int sumTripEnds = 0, sumTripStarts = 0;
		for (int num: tripEnds.values()) sumTripEnds += num;
		for (int num: tripStarts.values()) sumTripStarts += num;
		assertEquals("Sum of trips ends and trip starts is equal", sumTripEnds, sumTripStarts);
				
		RealODMatrixTempro tempro3 = tempro.clone();
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro3), DELTA);
		
		tempro.setFlow("E02004800", "E02003552", 1.1);
		tempro.setFlow("E02004800", "E02003553", 2.4);
		tempro.setFlow("E02004801", "E02003552", 3.5);
		tempro.setFlow("E02004801", "E02003553", 4.9);
		tempro2.setFlow("E02004800", "E02003552", 1.1);
		tempro2.setFlow("E02004800", "E02003553", 2.4);
		tempro2.setFlow("E02004801", "E02003552", 3.5);
		tempro2.setFlow("E02004801", "E02003553", 4.9);
		tempro3.setFlow("E02004800", "E02003552", 1.1);
		tempro3.setFlow("E02004800", "E02003553", 2.4);
		tempro3.setFlow("E02004801", "E02003552", 3.5);
		tempro3.setFlow("E02004801", "E02003553", 4.9);
		
		tempro.ceilMatrixValues(); //2, 3, 4, 5
		assertEquals("Cell value is correct", 2, tempro.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 3, tempro.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 4, tempro.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 5, tempro.getFlow("E02004801", "E02003553"), DELTA);
		
		tempro2.floorMatrixValues(); //1, 2, 3, 4
		assertEquals("Cell value is correct", 1, tempro2.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 2, tempro2.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 3, tempro2.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 4, tempro2.getFlow("E02004801", "E02003553"), DELTA);
		
		tempro3.roundMatrixValues(); //1, 2, 4, 5
		assertEquals("Cell value is correct", 1, tempro3.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 2, tempro3.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 4, tempro3.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 5, tempro3.getFlow("E02004801", "E02003553"), DELTA);
		
		assertEquals("After rounding should be the same", tempro3.getFlow("E02004800", "E02003552"), tempro3.getIntFlow("E02004800", "E02003552"), DELTA);
		assertEquals("After rounding total flows should be the same", tempro3.getTotalIntFlow(), tempro3.getSumOfFlows(), DELTA);

		double before = tempro.getTotalIntFlow();
		tempro.scaleMatrixValue(2.0);
		assertEquals("After scaling total flows should be as expected", before * 2.0, tempro.getTotalIntFlow(), DELTA);
		
		tempro.scaleMatrixValue(tempro2);

		ArrayList<String> origins, destinations;
		origins = new ArrayList<String>();
		origins.add("E02004800");
		destinations = new ArrayList<String>();
		destinations.add("E02003552");
		destinations.add("E02003553");
		
		tempro.sumMatrixSubset(origins, destinations);
		
		int numberOfZones = zoning.getZoneIDToCodeMap().length - 1;
		System.out.println("Number of zones = " + numberOfZones);
		RealODMatrixTempro unit = RealODMatrixTempro.createUnitMatrix(zoning);
		assertEquals("Total flows for unit matrix should be as expected", numberOfZones*numberOfZones, unit.getSumOfFlows(), DELTA);
		
		unit.deleteInterzonalFlows("E02004800");
		//unit1.printMatrixFormatted(2);
		assertEquals("Total flows after interzonal deletion should be as expected", numberOfZones*numberOfZones - 2*(numberOfZones-1), unit.getSumOfFlows(), DELTA);

		unit.saveMatrixFormatted("./temp/unitTempro.csv");
		unit.saveMatrixFormatted2("./temp/unitTempro2.csv");
		unit.saveMatrixFormatted3("./temp/unitTempro3.csv");
		
		unit = new RealODMatrixTempro("./temp/unitTempro.csv", zoning);
		int flow1 = unit.getTotalIntFlow();
		
		unit = new RealODMatrixTempro("./temp/unitTempro2.csv", zoning);
		int flow2 = unit.getTotalIntFlow();
		assertEquals("Total flows should be the same for two formats", 	flow1, flow2);
	
		unit = RealODMatrixTempro.createUnitMatrix(origins, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 1.0, unit.getSumOfFlows(), DELTA);

		unit = RealODMatrixTempro.createUnitMatrix(origins, destinations, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 2.0, unit.getSumOfFlows(), DELTA);
	}
}
