package nismod.transport.network.road;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.junit.Test;

import nismod.transport.demand.ODMatrix;

public class RouteSetGeneratorTest {

	public RouteSetGeneratorTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		
		//log error
	    FileOutputStream f = new FileOutputStream("err.txt");
	    System.setErr(new PrintStream(f));
		
		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);

		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork, null, null, null, null, null);
		
		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");

		
		/*
		
		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlNew);
		
		RoadNetworkAssignment rna2 = new RoadNetworkAssignment(roadNetwork2, null, null, null, null, null);

		//export to shapefile
		//roadNetwork2.exportToShapefile("testOutputNetwork");
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Midi Test Area");
		
		ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		
		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork2);
		routes.generateRouteSetWithLinkElimination(passengerODM, 2);
		routes.saveRoutes("testRoutes.txt", false);
		routes.printChoiceSets();
		routes.printStatistics();
		
		*/
		
		/*
				
		Route r1 = new Route();
		Route r2 = new Route();
		Route r3 = new Route();
		Route r4 = new Route();
		
		DirectedNode n1 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(7);
		DirectedNode n2 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(8);
		DirectedNode n3 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(27);
		DirectedNode n4 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(9);
		DirectedNode n5 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(55);
		DirectedNode n6 = (DirectedNode) roadNetwork2.getNodeIDtoNode().get(40);
			
		DirectedEdge e1 = (DirectedEdge) n1.getOutEdge(n2);
		DirectedEdge e2 = (DirectedEdge) n2.getOutEdge(n4);
		DirectedEdge e3 = (DirectedEdge) n4.getOutEdge(n6);
		DirectedEdge e4 = (DirectedEdge) n4.getOutEdge(n5);
		DirectedEdge e5 = (DirectedEdge) n5.getOutEdge(n6);
		DirectedEdge e6 = (DirectedEdge) n1.getOutEdge(n3);
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n1);
		
		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);
		r1.calculateTravelTime(rna2.getLinkTravelTimes());
		r1.calculateLength();
		r1.calculateUtility();
				
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		r2.calculateTravelTime(rna2.getLinkTravelTimes());
		r2.calculateLength();
		r2.calculateUtility();
		
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e3);
		r3.calculateTravelTime(rna2.getLinkTravelTimes());
		r3.calculateLength();
		r3.calculateUtility();
		
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e4);
		r4.addEdge(e5);
		r4.calculateTravelTime(rna2.getLinkTravelTimes());
		r4.calculateLength();
		r4.calculateUtility();
		
		DirectedNode originNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(7);
		DirectedNode destinationNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(40);
		
		RouteSet rs = new RouteSet(originNode, destinationNode);
		rs.addRoute(r1);
		rs.addRoute(r2);
		rs.addRoute(r3);
		rs.addRoute(r4);

		rs.sortRoutesOnUtility();
		rs.printChoiceSet();
		rs.calculateProbabilities();
		
		int[] choiceFrequency = new int[4];
		
		for (int i=0; i<1000; i++) {
		
			Route chosenRoute = rs.choose();
			int choiceIndex = rs.getIndexOfRoute(chosenRoute);
			choiceFrequency[choiceIndex]++;
		}
		
		System.out.println("Choice frequencies: ");
		System.out.println(Arrays.toString(choiceFrequency));
		
		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork2);
		
		routes.addRoute(r1);
		routes.addRoute(r2);
		routes.addRoute(r3);
		routes.addRoute(r4);
		
		routes.printChoiceSets();
		//routes.saveRoutes("routes.txt", false);
		
//		RouteSetGenerator routes2 = new RouteSetGenerator(roadNetwork2);
//		routes2.generateRouteSetWithLinkElimination(7, 40);
//		routes2.generateRouteSetWithLinkElimination(40, 7);
//		routes2.generateRouteSetWithLinkElimination(7, 8);
//		routes2.generateRouteSetWithLinkElimination(7, 7);
//		routes2.printChoiceSets();
//		
		RouteSetGenerator routes3 = new RouteSetGenerator(roadNetwork2);
		//routes3.readRoutes("routes.txt");
//		routes3.readRoutes("routesSouthamptonToNewForest.txt");
//		routes3.printStatistics();
//		routes3.printChoiceSets();
		routes3.clearRoutes();
		routes3.generateRouteSetWithLinkElimination("E06000045", "E07000091", 3);
		//routes3.saveRoutes("routesSouthamptonToNewForestTop3Nodes.txt", false);
		routes3.printStatistics();
		routes3.printChoiceSets();
//		
//		RouteSetGenerator routes4 = new RouteSetGenerator(roadNetwork);
//		routes4.generateRouteSetWithLinkElimination("E06000045", "E06000045");
//		routes4.printStatistics();
//		routes4.printChoiceSets();
	
	
	*/
		final URL zonesUrl3 = new URL("file://src/main/resources/data/zones.shp");
		final URL networkUrl3 = new URL("file://src/main/resources/data/network.shp");
		final URL networkUrl3New = new URL("file://src/main/resources/data/fullNetworkWithEdgeIDs.shp");
		final URL nodesUrl3 = new URL("file://src/main/resources/data/nodes.shp");
		final URL AADFurl3 = new URL("file://src/main/resources/data/AADFdirected2015.shp");
		
		final String areaCodeFileName3 = "./src/main/resources/data/population_OA_GB.csv";
		final String areaCodeNearestNodeFile3 = "./src/main/resources/data/nearest_node_OA_GB.csv";
		final String workplaceZoneFileName3 = "./src/main/resources/data/workplacePopulationFakeSC.csv";
		final String workplaceZoneNearestNodeFile3 = "./src/main/resources/data/nearest_node_WZ_GB_fakeSC.csv";
		final String freightZoneToLADfile3 = "./src/main/resources/data/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile3 = "./src/main/resources/data/freightZoneToNearestNode.csv";

		//create a road network
		RoadNetwork roadNetwork3 = new RoadNetwork(zonesUrl3, networkUrl3, nodesUrl3, AADFurl3, areaCodeFileName3, areaCodeNearestNodeFile3, workplaceZoneFileName3, workplaceZoneNearestNodeFile3, freightZoneToLADfile3, freightZoneNearestNodeFile3);
		roadNetwork3.replaceNetworkEdgeIDs(networkUrl3New);
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Test Area");
		
		//export to shapefile
		//roadNetwork2.exportToShapefile("outputNetwork");
		
		//roadNetwork3.exportToShapefile("fullNetworkWithEdgeIDs");
		
		//RoadNetworkAssignment roadNetworkAssignment = new RoadNetworkAssignment(roadNetwork3, null, null, null, null, null);
		
		//ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMfull.csv");
		//ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/passengerODMtempro.csv");
		ODMatrix passengerODM = new ODMatrix("./src/main/resources/data/balancedODMatrixOldLengths.csv");
		
		RouteSetGenerator routes5 = new RouteSetGenerator(roadNetwork3);
		//routes5.generateRouteSetWithLinkElimination(19, 79);
		//routes5.generateRouteSetWithLinkElimination("E06000046", "E06000046");
		//routes5.generateRouteSetWithLinkElimination("E06000045", "E06000046", 3);
		//routes5.generateRouteSetWithLinkElimination(passengerODM, 3);
		//routes5.saveRoutes("completeRoutesNewest.txt", false);
		//routes5.printStatistics();
		//routes5.printChoiceSets();
		
		routes5.readRoutes("completeRoutesNewest.txt");
		routes5.printStatistics();
//		routes5.printChoiceSets();
	}
	
	@Test
	public void test() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlNew);
		DirectedGraph rn = roadNetwork.getNetwork();
		ODMatrix passengerODM = new ODMatrix("./src/test/resources/testdata/passengerODM.csv");
		
		RouteSetGenerator routes = new RouteSetGenerator(roadNetwork);
		
		//routes.generateRouteSetWithLinkElimination(7, 40);
		//routes.generateRouteSetWithLinkElimination("E06000045", "E07000091", 2);
		routes.generateRouteSetWithLinkElimination(passengerODM, 3);
		routes.saveRoutes("testRoutes.txt", false);
//		routes.printChoiceSets();
//		routes.printStatistics();
		routes.clearRoutes();
		routes.readRoutes("testRoutes.txt");
		routes.printChoiceSets();
		routes.printStatistics();
	}
}
