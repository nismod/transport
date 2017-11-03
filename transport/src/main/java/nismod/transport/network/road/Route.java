package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Route is a sequence of directed edges with a choice utility
 * @author Milan Lovric
 *
 */
public class Route {
	
	//default route-choice parameters
	public static final double PARAM_TIME = -1.5;
	public static final double PARAM_LENGTH = -1.0;
	public static final double PARAM_INTERSECTIONS = -0.1;
	
	//initial route size for arraylist
	public static final int INITIAL_ROUTE_SIZE = 10;

	private static int counter = 0;
	private int id;
	private List<DirectedEdge> edges;
	private Double length;
	private Double time;
	private Double utility;
	private Node singleNode;
	
	public Route() {
	
		this.edges = new ArrayList<DirectedEdge>(INITIAL_ROUTE_SIZE);
		this.id = ++Route.counter;
	}
	
	/**
	 * Construtor from a given path.
	 * @param path A path from which to construct a route.
	 */
	public Route(RoadPath path) {

		if (path == null) {
			System.err.println("Route constructur: Path is null!");
			return;
		}
		
		if (!path.isValid()) {
			System.err.println("Route constructor: Path is not valid!");
			return;
		}
		
		List<DirectedEdge> builtEdges = (List<DirectedEdge>) path.getEdges(); //builds edges
		
		if (builtEdges == null) {
			System.err.println("Route constructor: Edge list is null!");
			return;
		}
		if (builtEdges.isEmpty()) {
			if (path.getFirst().equals(path.getLast()))
				this.singleNode = path.getFirst(); 	//single node path can be accepted
			else {
				System.err.println("Route constructor: Path has no edges!");
				return;
			}
		}
		
		this.edges = builtEdges; //store reference to the already built list
			
		this.id = ++Route.counter;
		//System.out.println("Constructing a route from path (nodes): " + path.toString());
		//System.out.println("Constructing a route from path (edges): " + path.getEdges());
		
		//this.edges = new ArrayList<DirectedEdge>(INITIAL_ROUTE_SIZE);
		//for (Object o: path.getEdges()) {
		//	DirectedEdge edge = (DirectedEdge) o;
		//	this.addEdge(edge);
		//}
	}
	
	public List<DirectedEdge> getEdges() {
		
		return	this.edges;
	}
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 * @return true if edge addition was successful, false otherwise.
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
	
	/**
	 * Adds a directed edge to the end of the current route.
	 * @param edge Directed edge to be added.
	 */
	public void addEdgeWithoutValidityCheck(DirectedEdge edge) {

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
	 * @param linkTravelTime Link travel times.
	 * @param params Route choice parameters.
	 */
	public void calculateUtility(HashMap<Integer, Double> linkTravelTime, Properties params) {
		
		if (this.length == null) this.calculateLength();
		if (this.time == null) this.calculateTravelTime(linkTravelTime);
		
		double length = this.getLength();
		double time = this.getTime();
		int intersec = this.getNumberOfIntersections();
		
		double paramTime, paramLength, paramIntersections;
		if (params == null) { //use default parameters
			paramTime = this.PARAM_TIME;
			paramLength = this.PARAM_LENGTH;
			paramIntersections = this.PARAM_INTERSECTIONS;			
		} else {
			paramTime = Double.parseDouble(params.getProperty("TIME"));
			paramLength = Double.parseDouble(params.getProperty("LENGTH"));
			paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));			
		}
		
		double utility = paramTime * time + paramLength * length + paramIntersections * intersec;  
		this.utility = utility;
	}
	
	public boolean isEmpty() {
		
		return this.edges.isEmpty();
	}
	
	public Double getLength() {
		
		return length;
	}
	
	public Double getTime() {
	
		return time;
	}
	
	public int getNumberOfIntersections() {
		
		if (this.isEmpty())
			return 0;
		else 
			return (this.edges.size() - 1);
	}
	
	public Double getUtility() {
		
		//if (this.utility == null) 
		//	System.err.println("Route utility needs to be first calculated using link travel time and other variables!");
		return utility;
	}
	
	public void setUtility(double utility) {
		
		this.utility = utility;
	}
	
	public DirectedNode getOriginNode() {
		
		if (edges.isEmpty()) 	return (DirectedNode) this.singleNode;
		else 					return this.edges.get(0).getInNode();
	}
	
	public DirectedNode getDestinationNode() {
		if (edges.isEmpty()) 	return (DirectedNode) this.singleNode;
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

		if (edges.isEmpty() && this.singleNode == null) return null;

		StringBuilder sb = new StringBuilder();

		if (this.singleNode != null) {
			sb.append("(");
			sb.append(this.getOriginNode().getID());
			sb.append(")->(");
			sb.append(this.getDestinationNode().getID());
			sb.append(")");
		} else {
			for (DirectedEdge edge: edges) {
				sb.append("(");
				sb.append(edge.getInNode().getID());
				sb.append(")-");
				sb.append(edge.getID());
				sb.append("->");
				//sb.append("(");
				//sb.append(edge.getOutNode().getID());
				//sb.append(")");
			}
			sb.append("(");
			sb.append(edges.get(edges.size()-1).getOutNode());
			sb.append(")");
		}

		return sb.toString();
	}
	
	public String getFormattedString() {
		
		if (edges.isEmpty() && this.singleNode == null) return null;
		
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
