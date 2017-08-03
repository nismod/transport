package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Route is a sequence of directed edges with a choice utility
 * @author Milan Lovric
 *
 */
public class Route {
	
	public static double paramTime = -1.5;
	public static double paramLength = -1.0;
	public static double paramInter = -0.1;

	private static int counter = 0;
	private int id;
	private ArrayList<DirectedEdge> edges;
	private double length;
	private double time;
	private double utility;
	
	public Route() {
	
		this.edges = new ArrayList<DirectedEdge>();
		this.id = ++Route.counter;
	}
	
	public Route(RoadPath path) {

		if (path == null) {
			System.err.println("Route constructur: Path is null!");
			return;
		}
		
		if (path.getEdges() == null) {
			System.err.println("Route constructor: Path has no edges!");
			return;
		}
		
		if (!path.isValid()) {
			System.err.println("Route constructor: Path is not valid!");
			return;
		}
		
		this.edges = new ArrayList<DirectedEdge>();
		this.id = ++Route.counter;
		
		//System.out.println("Constructing a route from path (nodes): " + path.toString());
		//System.out.println("Constructing a route from path (edges): " + path.getEdges());
				
		for (Object o: path.getEdges()) {
			DirectedEdge edge = (DirectedEdge) o;
			this.addEdge(edge);
		}
	}
	
	public List<DirectedEdge> getEdges() {
		
		return	this.edges;
	}
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 * @return true if edge addition was sucessful, false otherwise.
	 */
	public boolean addEdge(DirectedEdge edge) {

		if (this.edges.isEmpty()) this.edges.add(edge);
		else {
			DirectedEdge lastEdge = this.edges.get(this.edges.size()-1);
			if (!lastEdge.getOutNode().equals(edge.getInNode())) {
				System.err.printf("Trying to add an edge %d that is not connected to the last edge %d in the route (id = %d)\n", edge.getID(), lastEdge.getID(), this.getID());
				System.err.printf("(%d)-%d->(%d), (%d)-%d->(%d)", lastEdge.getInNode().getID(), lastEdge.getID(), lastEdge.getOutNode().getID(), edge.getInNode().getID(), edge.getID(), edge.getOutNode().getID());
				return false;
			} else
				this.edges.add(edge);
		}
		return true;
	}
	
//	/**
//	 * Adds a directed edge to the end of the current route.
//	 * @param edge Directed edge to be added.
//	 */
//	public void addEdge(DirectedEdge edge) {
//		
//		this.edges.add(edge);
//		if (!this.isValid()) {
//			System.err.println("Added edge that makes route invalid. Removing!");
//			this.edges.remove(edge);
//		}
//	}
	
	/**
	 * Calculates the route travel time based on link travel times.
	 * @param linkTravelTime Link travel times.
	 */
	public void calculateTravelTime(HashMap<Integer, Double> linkTravelTime) {
		
		double travelTime = 0.0;
		for (DirectedEdge edge: edges) {
			double time = linkTravelTime.get(edge.getID());
			travelTime += time;
		}
		this.time = travelTime;
	}
	
	/**
	 * Calculates the lenght of the route.
	 */
	public void calculateLength(){
	
		double length = 0.0;
		for (DirectedEdge edge: edges) {
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double len = (double) sf.getAttribute("LenNet"); //in [km]
			length += len;
		}
		this.length = length;
	}
	
	/**
	 * Calculates the utility of the route.
	 */
	public void calculateUtility() {
		
		double length = this.getLength();
		double time = this.getTime();
		double intersec = this.getNumberOfIntersections();
		
		double utility = paramTime * time + paramLength * length + paramInter * intersec;  
		this.utility = utility;
	}
	
	public boolean isEmpty() {
		
		return this.edges.isEmpty();
	}
	
	public double getLength() {
		
		return length;
	}
	
	public double getTime() {
		
		return time;
	}
	
	public int getNumberOfIntersections() {
		
		return (this.edges.size() - 1);
	}
	
	public double getUtility() {
		
		return utility;
	}
	
	public DirectedNode getOriginNode() {
		
		if (edges.isEmpty()) 	return null;
		else 					return this.edges.get(0).getInNode();
	}
	
	public DirectedNode getDestinationNode() {
		if (edges.isEmpty()) 	return null;
		else					return this.edges.get(edges.size() - 1).getOutNode();	
	}
	
	public int getID() {
		
		return id;
	}
	
	public boolean isValid() {
		
		if (this.edges.size() == 1) return true;
		
		for (int i = 1; i < this.edges.size(); i++) {
			DirectedEdge edge1 = this.edges.get(i-1);
			DirectedEdge edge2 = this.edges.get(i);
			if (!edge1.getOutNode().equals(edge2.getInNode())) return false; //if edges are not connected
		}
		return true;
	}
	
//	@Override
//	public String toString() {
//		
//		
//		if (edges.isEmpty()) return null;
//		
//		StringBuilder sb = new StringBuilder();
//		for (DirectedEdge edge: edges) {
//			sb.append(edge.getID());
//			sb.append("->");
//		}
//		sb.delete(sb.length()-2, sb.length()); //delete last arrow
//		
//		return sb.toString();
//	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (!Route.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final Route other = (Route) obj;
	    if ((this.edges == null) ? (other.edges != null) : !this.edges.equals(other.edges)) {
	        return false;
	    }
	    return true;
	}
	
	@Override
	public String toString() {
			
		if (edges.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		for (DirectedEdge edge: edges) {
			sb.append("(");
			sb.append(edge.getInNode().getID());
			sb.append(")--");
			sb.append(edge.getID());
			sb.append("->");
			
			sb.append("(");
			sb.append(edge.getOutNode().getID());
			sb.append(")");
		}
		sb.append("(");
		sb.append(edges.get(edges.size()-1).getOutNode());
		sb.append(")");
		
		return sb.toString();
	}
	
	public String getFormattedString() {
		
		if (edges.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		
		//origin and destination node IDs
		sb.append(this.getOriginNode().getID());
		sb.append(":");
		sb.append(this.getDestinationNode().getID());
		sb.append(":");
				
		for (DirectedEdge edge: edges) {
			sb.append(edge.getID());
			sb.append("-");
		}
		sb.delete(sb.length()-1, sb.length()); //delete last dash
		
		return sb.toString();
	}
}
