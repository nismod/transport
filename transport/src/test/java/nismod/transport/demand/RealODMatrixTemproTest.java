/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.imaging.ImageWriteException;
import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.decision.Intervention;
import nismod.transport.decision.Intervention.InterventionType;
import nismod.transport.decision.RoadDevelopment;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSet;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.PropertiesReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RealODMatrixTempro class
 * @author Milan Lovric
 *
 */
public class RealODMatrixTemproTest {
	
	public static void main( String[] args ) throws FileNotFoundException, IOException, ImageWriteException {
		
		final String configFile = "./src/main/full/config/configB1baseline.properties";
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
		
//		final String outputFolder = props.getProperty("outputFolder");
//		
//		//create output directory
//	     File file = new File(outputFolder);
//	        if (!file.exists()) {
//	            if (file.mkdirs()) {
//	                System.out.println("Output directory is created.");
//	            } else {
//	                System.err.println("Failed to create output directory.");
//	            }
//	        }

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		
		String[] arcZones = new String[]{"E07000008", "E07000177", "E07000099", "E06000055", "E06000056", "E07000012", "E06000032",
										"E07000179", "E07000004", "E07000180", "E07000181", "E07000155", "E06000042", "E07000178",
										"E06000030", "E07000151", "E07000154", "E07000156", "E07000009", "E07000242", "E07000011",
										"E07000243", "E06000037"};
		
		Integer[] B1Edges = new Integer[] {90000, 90001, 90002, 90003, 90004, 90005, 90006, 90007, 90008, 90009};
		List<Integer> B1EdgesList = Arrays.asList(B1Edges);
		
		List<String> arcLADZonesList = Arrays.asList(arcZones);
		List<String> arcTemproZonesList = new ArrayList<String>();
		
		int count = 0;
		for (String lad: arcLADZonesList) {
			List<String> tempro = zoning.getLADToListOfContainedZones().get(lad);
			count += tempro.size();
			arcTemproZonesList.addAll(tempro);
		}
		System.out.println("Count of tempro zones: " + count);
		System.out.println("Size of tempro zones list: " + arcTemproZonesList.size());
		
		RealODMatrixTempro odm = new RealODMatrixTempro(temproODMatrixFile, zoning);
		
		HashMap<String, Integer> tripOrigins = odm.calculateTripStarts();
		HashMap<String, Integer> tripDestinations = odm.calculateTripEnds();
		
//		String outputFolder = "./output/main/routechoice/B1/baseline/";
//		String megaOutputFile = outputFolder + "allRailDemands.csv";
		String outputFile = "tripEnds.csv";
		
		String NEW_LINE_SEPARATOR = "\n";
			ArrayList<String> outputHeader = new ArrayList<String>();
			outputHeader.add("temproZone");
			outputHeader.add("origins");
			outputHeader.add("destinations");

			FileWriter fileWriter = null;
			CSVPrinter csvFilePrinter = null;
			CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
			try {
				fileWriter = new FileWriter(outputFile);
				csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
				csvFilePrinter.printRecord(outputHeader);
				ArrayList<String> record = new ArrayList<String>();				
				
				for (String zoneID: tripOrigins.keySet()) {
					record.clear();
					record.add(zoneID);
					record.add(Integer.toString(tripOrigins.get(zoneID)));
					record.add(Integer.toString(tripDestinations.get(zoneID)));

					csvFilePrinter.printRecord(record);	
				}		
			} catch (Exception e) {
			} finally {
				try {
					fileWriter.flush();
					fileWriter.close();
					csvFilePrinter.close();
				} catch (IOException e) {
				}
			}
				
		RealODMatrixTempro odm2 = RealODMatrixTempro.createUnitMatrix(arcTemproZonesList, zoning);
		System.out.println("Expected size of the unit matrix: " + count*count);
		System.out.println("Size of the unit matrix: " + odm2.getTotalIntFlow());
		
		RealODMatrixTempro odm3 = new RealODMatrixTempro(zoning);
		
		for (String originZone: arcTemproZonesList)
			for (String destinationZone: arcTemproZonesList)
				if (odm.getIntFlow(originZone, destinationZone) > 0)
					odm3.setFlow(originZone, destinationZone, 1.0);
		
		System.out.println("Size of the arc matrix: " + odm3.getTotalIntFlow());
		odm3.saveMatrixFormatted2("arcTemproZonesMatrix.csv");
		
		//check that all flows are within the arc LAD zones
		List<String> origins = odm3.getUnsortedOrigins();
		List<String> destinations = odm3.getUnsortedDestinations();
		
		for (String originZone: origins) 
			for (String destinationZone: destinations) 
				if (odm3.getIntFlow(originZone, destinationZone) != 0) {
					
					String originLAD = zoning.getZoneToLADMap().get(originZone);
					String destinationLAD = zoning.getZoneToLADMap().get(destinationZone);
					
					if (!arcLADZonesList.contains(originLAD) || !arcLADZonesList.contains(destinationLAD))
						System.err.println("We have an LAD that is not among the Arc onces!");
					
					int origin = zoning.getTemproCodeToIDMap().get(originZone);
					int destination = zoning.getTemproCodeToIDMap().get(destinationZone);
					
					int originNode = zoning.getZoneIDToNearestNodeIDMap()[origin];
					int destinationNode = zoning.getZoneIDToNearestNodeIDMap()[destination];
					
					String oLAD = roadNetwork.getNodeToZone().get(originNode);
					String dLAD = roadNetwork.getNodeToZone().get(destinationNode);
					
					if (!oLAD.equals(originLAD)) {
						System.out.println("The nearest node maps to a different LAD!");
						System.out.printf("originLAD: %s ", originLAD);
						System.out.printf("originTemproZone: %s ", originZone);
						System.out.printf("originTemproZone ID: %d ", origin);
						System.out.printf("nearest node ID: %d ", originNode);
						System.out.printf("LAD from roadNetwork: %s \n", oLAD);
					}
					
					if (!dLAD.equals(destinationLAD)) {
						System.out.println("The nearest node maps to a different LAD!");
						System.out.printf("destinationLAD: %s ", destinationLAD);
						System.out.printf("destinationTemproZone: %s ", destinationZone);
						System.out.printf("destinationTemproZone ID: %d ", destination);
						System.out.printf("nearest node ID: %d ", destinationNode);
						System.out.printf("LAD from roadNetwork: %s \n", dLAD);
					}
					
					if (!arcLADZonesList.contains(oLAD))
						System.out.println("We have LAD outside of Arc LADs: " + oLAD);
					
					if (!arcLADZonesList.contains(dLAD))
						System.out.println("We have LAD outside of Arc LADs: " + dLAD);
			}
			
		//set route generation parameters
		Properties params = new Properties();
		params.setProperty("ROUTE_LIMIT", "5");
		params.setProperty("GENERATION_LIMIT", "20");
		params.setProperty("INITIAL_ROUTE_CAPACITY", "10");
		params.setProperty("INITIAL_OUTER_CAPACITY", "18000");
		params.setProperty("INITIAL_INNER_CAPACITY", "2000");
		params.setProperty("MAXIMUM_EDGE_ID", "200000");
		params.setProperty("MAXIMUM_NODE_ID", "13415");
		params.setProperty("MAXIMUM_TEMPRO_ZONE_ID", "7700");
		params.setProperty("MAXIMUM_LAD_ZONE_ID", "380");
		
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, params);
		rsg.generateRouteSetForODMatrixTemproDistanceBased(odm3, zoning, 1, 1);
		
		rsg.printStatistics();
		rsg.generateSingleNodeRoutes();
		System.out.println("Route set statistics after single nodes:");
		rsg.printStatistics();
		
		System.out.println("Network size before interventions: " + roadNetwork.getNetwork().getEdges().size());
		
		//load interventions
		List<Intervention> interventions = new ArrayList<Intervention>();
		
		for (Object o: props.keySet()) {
			String key = (String) o;
			if (key.startsWith("interventionFile")) {
				//System.out.println(key);
				String interventionFile = props.getProperty(key);
				Properties p = PropertiesReader.getProperties(interventionFile);
				String type = p.getProperty("type");
				//System.out.println(type);
				
				if (type.equals(InterventionType.RoadDevelopment.toString())) {
					Intervention rd = new RoadDevelopment(interventionFile);
					rd.install(roadNetwork);
				}
									
			}
		}

		System.out.println("Network size after road development interventions: " + roadNetwork.getNetwork().getEdges().size());
		
		String temproOxford = "E02005947";
		String temproMiltonKeynes = "E02003472";
		String temproCambridge = "E02003726";
		
		int temproOxfordID = zoning.getTemproCodeToIDMap().get(temproOxford);
		int temproMiltonKeynesID = zoning.getTemproCodeToIDMap().get(temproMiltonKeynes);
		int temproCambridgeID = zoning.getTemproCodeToIDMap().get(temproCambridge);
		
		int nodeOxford = zoning.getZoneIDToNearestNodeIDMap()[temproOxfordID];
		int nodeMiltonKeynes = zoning.getZoneIDToNearestNodeIDMap()[temproMiltonKeynesID];
		int nodeCambridge = zoning.getZoneIDToNearestNodeIDMap()[temproCambridgeID];
		
		System.out.println("Node Oxford: " + nodeOxford);
		System.out.println("Node Milton Keynes: " + nodeMiltonKeynes);
		System.out.println("Node Cambridge: " + nodeCambridge);
		
		//generate new route set
		RouteSetGenerator rsg2 = new RouteSetGenerator(roadNetwork, params);
		rsg2.generateRouteSetForODMatrixTemproDistanceBased(odm3, zoning, 1, 1);
		rsg2.printStatistics();
		rsg2.generateSingleNodeRoutes();
		System.out.println("Route set statistics after single nodes:");
		rsg2.printStatistics();
				
		int counter = 0;
		int counterB1 = 0;
		
		for (String originZone: origins) 
			for (String destinationZone: destinations)
				if (odm3.getIntFlow(originZone, destinationZone) != 0){
				
				int origin = zoning.getTemproCodeToIDMap().get(originZone);
				int destination = zoning.getTemproCodeToIDMap().get(destinationZone);
				
				int originNode = zoning.getZoneIDToNearestNodeIDMap()[origin];
				int destinationNode = zoning.getZoneIDToNearestNodeIDMap()[destination];
				
				if (originNode != zoning.getZoneToNearestNodeIDMap().get(originZone)) 
					System.err.println("Wrong node ID");
				if (destinationNode != zoning.getZoneToNearestNodeIDMap().get(destinationZone))
					System.err.println("Wrong node ID");

				RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
				RouteSet rs2 = rsg2.getRouteSet(originNode, destinationNode);
				
				if (rs == null) {
					System.err.printf("Cannot fetch route set between node %d and node %d from rsg! \n", originNode, destinationNode);
					
				}
				if (rs2 == null) {
					System.err.printf("Cannot fetch route set between node %d and node %d from rsg2! \n", originNode, destinationNode);
					
				}
				
				List<Route> list = rs.getChoiceSet();
				List<Route> list2 = rs2.getChoiceSet();
				
				for (Route r: list2)
					if (!list.contains(r)) {
						//System.out.println(r.toString());
						counter++;
						
						//check if the route contains any of the B1 edges
						for (int edge: B1EdgesList) 
							if (r.getEdges().contains(edge)) {
								counterB1++;
								break;
							}
					}
			}
			
		System.out.println("Number of new routes after road development: " + counter);
		System.out.println("Number of new routes that use new road links: " + counterB1);
				
		System.out.println("Oxford to Milton Keynes before:");
		rsg.getRouteSet(nodeOxford, nodeMiltonKeynes).printChoiceSet();
				
		System.out.println("Oxford to Milton Keynes after B1:");
		rsg2.getRouteSet(nodeOxford, nodeMiltonKeynes).printChoiceSet();
		
		System.out.println("Milton Keynes to Oxford before:");
		rsg.getRouteSet(nodeMiltonKeynes, nodeOxford).printChoiceSet();
				
		System.out.println("Milton Keynes to Oxford after B1:");
		rsg2.getRouteSet(nodeMiltonKeynes, nodeOxford).printChoiceSet();
		
		System.out.println("Oxford to Cambridge before:");
		rsg.getRouteSet(nodeOxford, nodeCambridge).printChoiceSet();
				
		System.out.println("Oxford to Cambridge after B1:");
		rsg2.getRouteSet(nodeOxford, nodeCambridge).printChoiceSet();
		
		System.out.println("Cambridge to Oxford before:");
		rsg.getRouteSet(nodeCambridge, nodeOxford).printChoiceSet();
				
		System.out.println("Cambridge to Oxford after B1:");
		rsg2.getRouteSet(nodeCambridge, nodeOxford).printChoiceSet();
		
		//copy new routes from rsg2 into rsg
		
		for (String originZone: origins) 
			for (String destinationZone: destinations)
				if (odm3.getIntFlow(originZone, destinationZone) != 0){
				
				int origin = zoning.getTemproCodeToIDMap().get(originZone);
				int destination = zoning.getTemproCodeToIDMap().get(destinationZone);
				
				int originNode = zoning.getZoneIDToNearestNodeIDMap()[origin];
				int destinationNode = zoning.getZoneIDToNearestNodeIDMap()[destination];
				
				if (originNode != zoning.getZoneToNearestNodeIDMap().get(originZone)) 
					System.err.println("Wrong node ID");
				if (destinationNode != zoning.getZoneToNearestNodeIDMap().get(destinationZone))
					System.err.println("Wrong node ID");

				RouteSet rs = rsg.getRouteSet(originNode, destinationNode);
				RouteSet rs2 = rsg2.getRouteSet(originNode, destinationNode);
				
				if (rs == null) {
					System.err.printf("Cannot fetch route set between node %d and node %d from rsg! \n", originNode, destinationNode);
					
				}
				if (rs2 == null) {
					System.err.printf("Cannot fetch route set between node %d and node %d from rsg2! \n", originNode, destinationNode);
					
				}
				
				List<Route> list = rs.getChoiceSet();
				List<Route> list2 = rs2.getChoiceSet();
				
				for (Route r: list2)
					rs.addRoute(r);
					
			}
		
		System.out.println("Route set 1 after adding the new routes: ");
		rsg.printStatistics();
		
		
		
		/*
		//RealODMatrixTempro temproODM = new RealODMatrixTempro(temproODMatrixFile, zoning);
		RealODMatrixTempro temproODM = new RealODMatrixTempro("temproMatrixListBased198WithMinor4.csv", zoning);
		System.out.println("Sum of flows: " + temproODM.getSumOfFlows());
		
		RealODMatrix lad = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(temproODM, zoning);
		
		System.out.println("Sum of flows: " + lad.getSumOfFlows());
		lad.saveMatrixFormatted2("ladFromTempro198ODMWithMinor4.csv");
		*/
		
//		RealODMatrixTempro temproODM2 = RealODMatrixTempro.createTEMProFromLadMatrix(lad, temproODM, zoning);
//		temproODM2.saveMatrixFormatted2("temproWithMinorRecreated.csv");
				
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
		
			
//		System.in.read(); System.in.read();
//		
//		
//		final String ladMatrixFile = props.getProperty("baseYearODMatrixFile");
//		ODMatrix ladMatrix = new ODMatrix(ladMatrixFile);
//		ladMatrix.scaleMatrixValue(2);
//	
//		ODMatrix baseTempro = new ODMatrix(temproODMatrixFile);
//		RealODMatrixTempro todm = RealODMatrixTempro.createTEMProFromLadMatrix(ladMatrix, temproODM, zoning);
//		ODMatrix odm = ODMatrix.createTEMProFromLadMatrix(ladMatrix, baseTempro, zoning);
//		
//		System.out.println(todm.getTotalIntFlow());
//		System.out.println(odm.getTotalIntFlow());
		
		
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
		
		int numberOfZones = zoning.getTemproIDToCodeMap().length - 1;
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
		
		int numberOfZones = zoning.getTemproIDToCodeMap().length - 1;
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
