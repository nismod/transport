package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
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
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 */
	public void addEdge(DirectedEdge edge) {
		
		this.edges.add(edge);		
	}
	
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
		
		return this.edges.get(0).getInNode();
	}
	
	public DirectedNode getDestinationNode() {
		
		return this.edges.get(edges.size()).getOutNode();	
	}
	
	public int getID() {
		
		return id;
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
	public String toString() {
			
		if (edges.isEmpty()) return null;
		
		StringBuilder sb = new StringBuilder();
		for (DirectedEdge edge: edges) {
			sb.append("(");
			sb.append(edge.getInNode().getID());
			sb.append(")--");
			sb.append(edge.getID());
			sb.append("->");
		}
		sb.append("(");
		sb.append(edges.get(edges.size()-1).getOutNode());
		sb.append(")");
		
		return sb.toString();
	}
}
