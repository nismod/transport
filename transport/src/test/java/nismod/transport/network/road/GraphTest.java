package nismod.transport.network.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geotools.graph.build.line.BasicDirectedLineGraphBuilder;
import org.geotools.graph.path.AStarShortestPathFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.AStarIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.junit.Test;

/**
 * This class demonstrates the problem with the GeoTools implementation of the AStar algorithm:
 * When there are no directed paths from origin to destination node, but there is an undirected path,
 * AStar will return this undirected path, ignoring the fact that there may be edges with different directions in it.
 * MyAStar implementation solves this problem by changing the step (cont) function of the AStarIterator to look only at
 * outRelated nodes as opposed to all related nodes.
 * @author Milan Lovric
 *
 */
public class GraphTest {

	private static final double EPSILON = 1e-15;

	@Test
	public void test() throws IOException {

		BasicDirectedLineGraphBuilder graphBuilder = new BasicDirectedLineGraphBuilder();

		DirectedNode node1 = (DirectedNode) graphBuilder.buildNode();
		DirectedNode node2 = (DirectedNode) graphBuilder.buildNode();
		DirectedNode node3 = (DirectedNode) graphBuilder.buildNode();
		DirectedNode node4 = (DirectedNode) graphBuilder.buildNode();

		node1.setID(1);
		node2.setID(2);
		node3.setID(3);
		node4.setID(4);

		graphBuilder.addNode(node1);
		graphBuilder.addNode(node2);
		graphBuilder.addNode(node3);
		graphBuilder.addNode(node4);

		DirectedEdge edge1 = (DirectedEdge) graphBuilder.buildEdge(node1, node2);
		DirectedEdge edge2 = (DirectedEdge) graphBuilder.buildEdge(node2, node3);
		DirectedEdge edge3 = (DirectedEdge) graphBuilder.buildEdge(node3, node4);
		DirectedEdge edge4 = (DirectedEdge) graphBuilder.buildEdge(node4, node2);

		edge1.setID(12);
		edge2.setID(23);
		edge3.setID(34);
		edge4.setID(42);

		graphBuilder.addEdge(edge1);
		graphBuilder.addEdge(edge2);
		graphBuilder.addEdge(edge3);
		graphBuilder.addEdge(edge4);

		edge1.setObject(new Double(1.0));
		edge2.setObject(new Double(1.0));
		edge3.setObject(new Double(1.0));
		edge4.setObject(new Double(0.4));

		DirectedGraph graph = (DirectedGraph) graphBuilder.getGraph();
		System.out.println("Graph nodes and edges:");
		System.out.println(graph.toString());

		System.out.println("Out related nodes to node 2: ");
		Iterator iter = node2.getOutRelated();
		while (iter.hasNext()) {
			Node n = (Node) iter.next();
			System.out.println(n);
		}
		System.out.println("In related nodes to node 2: ");
		iter = node2.getInRelated();
		while (iter.hasNext()) {
			Node n = (Node) iter.next();
			System.out.println(n);
		}
		System.out.println("All related nodes to node 2: ");
		iter = node2.getRelated();
		while (iter.hasNext()) {
			Node n = (Node) iter.next();
			System.out.println(n);
		}

		//weight the edges of the graph using the object (Double)		
		EdgeWeighter dijkstraWeighter = new EdgeWeighter() {
			@Override
			public double getWeight(org.geotools.graph.structure.Edge edge) {

				if (edge == null) System.err.println("Ooops null!");

				double length = (double) edge.getObject(); 
				return length;
			}
		};

		//find the shortest path using Dijkstra algorithm
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using Dijkstra:\n", node1.getID(), node4.getID());
			DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(graph, node1, dijkstraWeighter);
			pathFinder.calculate();
			Path path = pathFinder.getPath(node4);
			if (path != null) {
				path.reverse();
				System.out.println("The path as a list of nodes nodes: " + path);
				List listOfEdges = path.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				System.out.printf("Total path length: %.3f\n", pathFinder.getCost(node4));

				double sum = 0;
				for (Object o: listOfEdges) {
					//DirectedEdge e = (DirectedEdge) o;
					Edge e = (Edge) o;
					System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
					double length = (double) e.getObject();
					System.out.println(length);
					sum += length;
				}
				System.out.printf("Sum of edge lengths: %.3f\n\n", sum);

				//compare with expected values
				int[] expectedNodeList = new int[] {1, 2, 3, 4};
				int[] expectedEdgeList = new int[] {12, 23, 34};
				assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), path.toString());
				assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
				assertEquals("The shortest path length equals the sum of edge lengths", sum, pathFinder.getCost(node4), EPSILON);
				assertEquals("The shortest path length is correct", 3.0, pathFinder.getCost(node4), EPSILON);

			} else
				System.err.println("Could not find the shortest path using Dijkstra.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//find the shortest path using AStar algorithm
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using AStar:\n", node1.getID(), node4.getID());
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(graph, node1, node4, getAstarFunctions(node4));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			if (aStarPath != null) {
				aStarPath.reverse();
				System.out.println(aStarPath);
				System.out.println("The path as a list of nodes: " + aStarPath);
				List listOfEdges = aStarPath.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				double sum = 0;
				for (Object o: listOfEdges) {
					DirectedEdge e = (DirectedEdge) o;
					System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
					double length = (double) e.getObject();
					System.out.println(length);
					sum += length;
				}
				System.out.printf("Sum of edge lengths: %.3f\n", sum);
				
				//compare with expected values
				int[] expectedNodeList = new int[] {1, 2, 3, 4};
				int[] expectedEdgeList = new int[] {12, 23, 34};
				assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
				assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
				assertEquals("The shortest path length is correct", 3.0, sum, EPSILON);
			} else
				System.out.println("Could not find the shortest path using AStar.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//find the shortest path using my AStar algorithm
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using my AStar:\n", node1.getID(), node4.getID());
			MyAStarShortestPathFinder aStarPathFinder = new MyAStarShortestPathFinder(graph, node1, node4, getMyAstarFunctions(node4));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			if (aStarPath != null) {
				aStarPath.reverse();
				System.out.println(aStarPath);
				System.out.println("The path as a list of nodes: " + aStarPath);
				List listOfEdges = aStarPath.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				double sum = 0;
				for (Object o: listOfEdges) {
					DirectedEdge e = (DirectedEdge) o;
					System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
					double length = (double) e.getObject();;
					System.out.println(length);
					sum += length;
				}
				System.out.printf("Sum of edge lengths: %.3f\n\n", sum);
				
				//compare with expected values
				int[] expectedNodeList = new int[] {1, 2, 3, 4};
				int[] expectedEdgeList = new int[] {12, 23, 34};
				assertEquals("The list of nodes in the shortest path is correct", Arrays.toString(expectedNodeList), aStarPath.toString());
				assertEquals("The list of edges in the shortest path is correct", Arrays.toString(expectedEdgeList), listOfEdges.toString());
				assertEquals("The shortest path length is correct", 3.0, sum, EPSILON);
			} else
				System.out.println("Could not find the shortest path using my AStar.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//remove edge
		//edge2.setObject(Double.POSITIVE_INFINITY);
		//edge2.setObject(Double.MAX_VALUE);
		//edge2.setObject(100.0);

		//edge3.setObject(Double.POSITIVE_INFINITY);
		graphBuilder.removeEdge(edge2);

		//find the shortest path using Dijkstra algorithm
		Path path = null;
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using Dijkstra:\n", node1.getID(), node4.getID());
			DijkstraShortestPathFinder pathFinder = new DijkstraShortestPathFinder(graph, node1, dijkstraWeighter);
			pathFinder.calculate();
			path = pathFinder.getPath(node4);
			if (path != null) {
				path.reverse();
				System.out.println("The path as a list of nodes nodes: " + path);
				List listOfEdges = path.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				System.out.printf("Total path length: %.3f\n", pathFinder.getCost(node4));
			} else
				System.out.println("Could not find the shortest path using Dijkstra.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNull("There should be no shortest path after edge removal", path);

		//find the shortest path using AStar algorithm
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using AStar:\n", node1.getID(), node4.getID());
			AStarShortestPathFinder aStarPathFinder = new AStarShortestPathFinder(graph, node1, node4, getAstarFunctions(node4));
			aStarPathFinder.calculate();
			Path aStarPath = aStarPathFinder.getPath();
			if (aStarPath != null) {
				aStarPath.reverse();
				System.out.println(aStarPath);
				System.out.println("The path as a list of nodes: " + aStarPath);
				List listOfEdges = aStarPath.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				double sum = 0;
				for (Object o: listOfEdges) {
					DirectedEdge e = (DirectedEdge) o;
					System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
					double length = (double) e.getObject();
					System.out.println(length);
					sum += length;
				}
				System.out.printf("Sum of edge lengths: %.3f\n", sum);
			} else
				System.out.println("Could not find the shortest path using AStar.");
		} catch (Exception e) {
			e.printStackTrace();
		}

		//find the shortest path using my AStar algorithm
		Path aStarPath = null;
		try {
			System.out.printf("\nFinding the shortest path from node %d to node %d using my AStar:\n", node1.getID(), node4.getID());
			MyAStarShortestPathFinder aStarPathFinder = new MyAStarShortestPathFinder(graph, node1, node4, getMyAstarFunctions(node4));
			aStarPathFinder.calculate();
			aStarPath = aStarPathFinder.getPath();
			if (aStarPath != null) {
				aStarPath.reverse();
				System.out.println(aStarPath);
				System.out.println("The path as a list of nodes: " + aStarPath);
				List listOfEdges = aStarPath.getEdges();
				System.out.println("The path as a list of edges: " + listOfEdges);
				double sum = 0;
				for (Object o: listOfEdges) {
					DirectedEdge e = (DirectedEdge) o;
					System.out.print(e.getID() + "|" + e.getNodeA() + "->" + e.getNodeB() + "|");
					double length = (double) e.getObject();;
					System.out.println(length);
					sum += length;
				}
				System.out.printf("Sum of edge lengths: %.3f\n\n", sum);
			} else
				System.out.println("Could not find the shortest path using my AStar.");
						
		} catch (WrongPathException e) {
			System.err.println("Could not find the shortest path using my AStar.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNull("There should be no shortest path after edge removal", aStarPath);
	}

	/** Getter method for the AStar functions (edge cost and heuristic function).
	 * @param to Destination node.
	 * @return AStar functions.
	 */
	private static AStarIterator.AStarFunctions getAstarFunctions(Node destinationNode) {

		AStarIterator.AStarFunctions aStarFunctions = new  AStarIterator.AStarFunctions(destinationNode){
			@Override
			public double cost(AStarIterator.AStarNode aStarNode1, AStarIterator.AStarNode aStarNode2) {
				System.out.printf("Inspecting the cost from node %d to node %d \n", aStarNode1.getNode().getID(), aStarNode2.getNode().getID());
				Edge edge = ((DirectedNode) aStarNode1.getNode()).getOutEdge((DirectedNode) aStarNode2.getNode());
				if (edge == null) {
					//no edge found in that direction (set maximum weight)
					System.out.printf("No edge found from node %d to node %d \n", aStarNode1.getNode().getID(), aStarNode2.getNode().getID());
					return Double.POSITIVE_INFINITY;
				} else {
					double cost = (double) edge.getObject();  //use edge weight
					return cost;
				}
			}
			@Override
			public double h(Node node) {
				return 0.0; //equivalent to Dijkstra
			}
		};
		return aStarFunctions;
	}

	/** Getter method for the AStar functions (edge cost and heuristic function) based on distance.
	 * @param to Destination node.
	 * @return AStar functions.
	 */
	private static MyAStarIterator.AStarFunctions getMyAstarFunctions(Node destinationNode) {

		MyAStarIterator.AStarFunctions aStarFunctions = new  MyAStarIterator.AStarFunctions(destinationNode){

			@Override
			public double cost(MyAStarIterator.AStarNode aStarNode1, MyAStarIterator.AStarNode aStarNode2) {

				System.out.printf("Inspecting the cost from node %d to node %d \n", aStarNode1.getNode().getID(), aStarNode2.getNode().getID());
				//Edge edge = aStarNode1.getNode().getEdge(aStarNode2.getNode()); // does not work, a directed version must be used!
				Edge edge = ((DirectedNode) aStarNode1.getNode()).getOutEdge((DirectedNode) aStarNode2.getNode());
				if (edge == null) {
					//no edge found in that direction (set maximum weight)
					System.out.printf("No edge found from node %d to node %d \n", aStarNode1.getNode().getID(), aStarNode2.getNode().getID());
					return Double.POSITIVE_INFINITY;
				} else {
					double cost = (double) edge.getObject();  //use actual physical length
					return cost;
				}
			}

			@Override
			public double h(Node node) {

				return 0.0; //equivalent to Dijkstra
			}
		};

		return aStarFunctions;
	}
}
