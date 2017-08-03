package nismod.transport.network.road;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;

public class RouteSetTest {

	public static void main( String[] args ) throws IOException	{
		
//		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
//		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
//		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
//		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");

		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		
		//create a road network
		//RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);

		final URL zonesUrl2 = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl2 = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl2 = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl2 = new URL("file://src/test/resources/testdata/AADFdirected.shp");

		//create a road network
		RoadNetwork roadNetwork2 = new RoadNetwork(zonesUrl2, networkUrl2, nodesUrl2, AADFurl2, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork2.replaceNetworkEdgeIDs(networkUrlNew);
		RoadNetworkAssignment rna = new RoadNetworkAssignment(roadNetwork2, null, null, null, null, null);
		
		//visualise the shapefiles
		//roadNetwork2.visualise("Midi Test Area");
				
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
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n2);
		
		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);
		r1.calculateTravelTime(rna.getLinkTravelTimes());
		r1.calculateLength();
		r1.calculateUtility();
				
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		r2.calculateTravelTime(rna.getLinkTravelTimes());
		r2.calculateLength();
		r2.calculateUtility();
		
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e2);
		r3.addEdge(e3);
		r3.calculateTravelTime(rna.getLinkTravelTimes());
		r3.calculateLength();
		r3.calculateUtility();
		
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e2);
		r4.addEdge(e4);
		r4.addEdge(e5);
		r4.calculateTravelTime(rna.getLinkTravelTimes());
		r4.calculateLength();
		r4.calculateUtility();
		
		DirectedNode originNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(7);
		DirectedNode destinationNode = (DirectedNode)roadNetwork2.getNodeIDtoNode().get(40);
		
		RouteSet rs = new RouteSet(originNode, destinationNode);
		//rs.addRoute(r1);
		rs.addRoute(r2);
		rs.addRoute(r3);
		rs.addRoute(r4);

		rs.sortRoutesOnUtility();
		rs.printChoiceSet();
		rs.printStatistics();
		rs.calculateProbabilities();
		
		int[] choiceFrequency = new int[4];
		
		for (int i=0; i<1000; i++) {
		
			Route chosenRoute = rs.choose();
			int choiceIndex = rs.getIndexOfRoute(chosenRoute);
			choiceFrequency[choiceIndex]++;
		}
		
		System.out.println("Choice frequencies: ");
		System.out.println(Arrays.toString(choiceFrequency));
		
		RoadPath rp = roadNetwork2.getFastestPath(n1, n6, null);
		System.out.println("Fastest path: " + rp);
		System.out.println("Edges: " + rp.getEdges());
		for (Object o: rp.getEdges()) {
			DirectedEdge e = (DirectedEdge) o;
			System.out.println(e.getInNode() + "->" + e.getOutNode());
		}
		Route newRoute = new Route(rp);
		System.out.println(newRoute.isValid());
		rs.addRoute(newRoute);
		rs.printChoiceSet();
		rs.printStatistics();
	}
}
