/**
 * 
 */
package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Tests for the RoadNetwork class
 * @author Milan Lovric
 *
 */
public class RoadNetworkTest {

	private static final double EPSILON = 1e-15;

	public static void main( String[] args ) throws IOException	{

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		DirectedGraph rn = roadNetwork.getNetwork();

		//visualise the shapefiles
		roadNetwork.visualise();
	}

	@Test
	public void test() throws IOException {

		final URL zonesUrl = new URL("file://src/test/resources/minitestdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/minitestdata/network.shp");
		final URL nodesUrl = new URL("file://src/test/resources/minitestdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/minitestdata/AADFdirected.shp");

		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl);
		DirectedGraph rn = roadNetwork.getNetwork();

		//TEST NODE AND EDGE CREATION
		System.out.println("\n\n*** Testing node and edge creation ***");

		Iterator iter = rn.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 55) nodeA = node;
			if (node.getID() == 40) nodeB = node;
		}
		System.out.println(nodeA);
		System.out.println(nodeB);
		assertEquals("Node ID is correct", 55, nodeA.getID());
		assertEquals("Node ID is correct", 40, nodeB.getID());
		DirectedEdge edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		DirectedEdge edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		SimpleFeature sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "W", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 56374L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "E", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 1.1, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 6, nodeB.getDegree());
		assertEquals("Node out degree is correct", 3, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 3, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 87) nodeA = node;
			if (node.getID() == 105) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 87, nodeA.getID());
		assertEquals("Node ID is correct", 105, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 86003L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "C", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 95) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 95, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48513L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "N", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.4, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 4, nodeA.getDegree());
		assertEquals("Node out degree is correct", 2, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeA.getInDegree());
		System.out.println("node " + nodeB.getID() + " degree: " + nodeB.getDegree());
		System.out.println("node " + nodeB.getID() + " out degree: " + nodeB.getOutDegree());
		System.out.println("node " + nodeB.getID() + " in degree: " + nodeB.getInDegree());
		assertEquals("Node degree is correct", 4, nodeB.getDegree());
		assertEquals("Node out degree is correct", 2, nodeB.getOutDegree());
		assertEquals("Node in degree is correct", 2, nodeB.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 48) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 48, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		assertNull("Expecting no edge in this direction", edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeBA.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48317L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.3, sf.getAttribute("LenNet"));

		System.out.println("node " + nodeA.getID() + " degree: " + nodeA.getDegree());
		System.out.println("node " + nodeA.getID() + " out degree: " + nodeA.getOutDegree());
		System.out.println("node " + nodeA.getID() + " in degree: " + nodeA.getInDegree());
		assertEquals("Node degree is correct", 2, nodeA.getDegree());
		assertEquals("Node out degree is correct", 1, nodeA.getOutDegree());
		assertEquals("Node in degree is correct", 1, nodeA.getInDegree());

		iter = rn.getNodes().iterator();
		nodeA=null; nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 82) nodeA = node;
			if (node.getID() == 95) nodeB = node;
		}
		System.out.println(nodeA.getID());
		System.out.println(nodeB.getID());
		assertEquals("Node ID is correct", 82, nodeA.getID());
		assertEquals("Node ID is correct", 95, nodeB.getID());
		edgeAB = (DirectedEdge) nodeA.getOutEdge(nodeB);
		edgeBA = (DirectedEdge) nodeB.getOutEdge(nodeA);
		System.out.println(edgeAB);
		System.out.println(edgeBA);
		sf = (SimpleFeature) edgeAB.getObject(); 
		System.out.println(sf.getAttribute("CP"));
		System.out.println(sf.getAttribute("iDir"));
		System.out.println(sf.getAttribute("LenNet"));
		assertEquals("Edge CP is correct", 48456L, sf.getAttribute("CP"));
		assertEquals("Edge direction is correct", "S", sf.getAttribute("iDir"));
		assertEquals("Edge length is correct", 0.1, sf.getAttribute("LenNet"));
		assertNull("Expecting no edge in this direction", edgeBA);

		//TEST SHORTEST PATH ALGORITHMS
		System.out.println("\n\n*** Testing the shortest path algorithms ***");
		
		System.out.println("\n*** Dijkstra ***");
		//set source and destination node
		iter = rn.getNodes().iterator();
		Node from = null, to = null;
		while (iter.hasNext() && (from == null || to == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == 86) from = node;
			if (node.getID() == 19) to = node;
		}

		//find the shortest path using Dijkstra
		System.out.println("Source node: " + from.getID() + " | Destination node: " + to.getID());
		DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(rn, from, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		Path path = pathFinder.getPath(to);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		List listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(to));

		double sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		int[] expectedNodeList = new int[] {86, 87, 105, 95, 48, 19};
		int[] expectedEdgeList = new int[] {81, 61, 67, 74, 77};
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(to), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(to), EPSILON);

		//reverse direction
		System.out.println("Source node: " + to.getID() + " | Destination node: " + from.getID());
		pathFinder = new DijkstraShortestPathFinder(rn, to, roadNetwork.getDijkstraWeighter());
		pathFinder.calculate();
		path = pathFinder.getPath(from);
		path.reverse();
		System.out.println("The path as a list of nodes nodes: " + path);
		listOfEdges = path.getEdges();
		System.out.println("The path as a list of edges: " + listOfEdges);
		System.out.println("Path size in the number of nodes: " + path.size());
		System.out.println("Path size in the number of edges: " + listOfEdges.size());
		System.out.printf("Total path length in km: %.3f\n", pathFinder.getCost(from));

		sum = 0;
		for (Object o: listOfEdges) {
			//DirectedEdge e = (DirectedEdge) o;
			Edge e = (Edge) o;
			System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
			sf = (SimpleFeature) e.getObject();
			double length = (double) sf.getAttribute("LenNet");
			System.out.println(length);
			sum += length;
		}
		System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

		//compare with expected values
		expectedNodeList = new int[] {19, 48, 82, 95, 105, 87, 86};
		expectedEdgeList = new int[] {78, 60, 69, 68, 62, 82};
		assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
		assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
		assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(from), EPSILON);
		assertEquals("The shortest path length is correct", 2.1, pathFinder.getCost(from), EPSILON);

		System.out.println("\n*** AStar ***");
		
		//find the shortest path using AStar algorithm
		try {
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, from, to, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath;
			aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {86, 87, 105, 95, 48, 19};
			expectedEdgeList = new int[] {81, 61, 67, 74, 77};
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
		}


		//reverse path
		try {
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(rn, to, from, roadNetwork.getAstarFunctions(to));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			aStarPath.reverse();
			System.out.println(aStarPath);
			System.out.println("The path as a list of nodes nodes: " + aStarPath);
			listOfEdges = aStarPath.getEdges();
			System.out.println("The path as a list of edges: " + listOfEdges);
			System.out.println("Path size in the number of nodes: " + aStarPath.size());
			System.out.println("Path size in the number of edges: " + listOfEdges.size());
			sum = 0;
			for (Object o: listOfEdges) {
				//DirectedEdge e = (DirectedEdge) o;
				Edge e = (Edge) o;
				System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
				sf = (SimpleFeature) e.getObject();
				double length = (double) sf.getAttribute("LenNet");
				System.out.println(length);
				sum += length;
			}
			System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

			//compare with expected values
			expectedNodeList = new int[] {19, 48, 82, 95, 105, 87, 86};
			expectedEdgeList = new int[] {78, 60, 69, 68, 62, 82};
			assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
			assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
			assertEquals("The shortest path length is correct", 2.1, sum, EPSILON);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
