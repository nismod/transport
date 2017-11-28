package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	public static final double PARAM_COST = -3.6;
	public static final double PARAM_INTERSECTIONS = -0.1;

	public static final double AVERAGE_INTERSECTION_DELAY = 0.8; //[min] 
	
	//initial route size for arraylist
	public static final int INITIAL_ROUTE_CAPACITY = 10;

//	private static int counter = 0;
//	private int id;
	private ArrayList<DirectedEdge> edges;
	private Double length;
	private Double time;
	private Double cost;
	private Double utility;
	private Node singleNode;
	
	public Route() {
	
		this.edges = new ArrayList<DirectedEdge>(INITIAL_ROUTE_CAPACITY);
//		this.id = ++Route.counter;
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
		
		this.edges = (ArrayList<DirectedEdge>) builtEdges; //store reference to the already built list
			
//		this.id = ++Route.counter;
		//System.out.println("Constructing a route from path (nodes): " + path.toString());
		//System.out.println("Constructing a route from path (edges): " + path.getEdges());
		
		//this.edges = new ArrayList<DirectedEdge>(INITIAL_ROUTE_SIZE);
		//for (Object o: path.getEdges()) {
		//	DirectedEdge edge = (DirectedEdge) o;
		//	this.addEdge(edge);
		//}
	}
	
	/**
	 * Getter method for the list of edges.
	 * @return List of edges.
	 */
	public List<DirectedEdge> getEdges() {
		
		return	this.edges;
	}
	
	
	/**
	 * Trims edges list to size.
	 */
	public void trimToSize() {
		
		this.edges.trimToSize();
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
//				System.err.printf("Trying to add an edge %d that is not connected to the last edge %d in the route (id = %d)\n", edge.getID(), lastEdge.getID(), this.getID());
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
	public void calculateTravelTime(Map<Integer, Double> linkTravelTime, double avgIntersectionDelay) {
		
		double travelTime = 0.0;
		for (DirectedEdge edge: edges) {
			double time = linkTravelTime.get(edge.getID());
			travelTime += time;
		}
		travelTime += this.getNumberOfIntersections() * avgIntersectionDelay;
		this.time = travelTime;
	}
	
	/**
	 * Calculates the length of the route.
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
	 * Calculates the cost of the route.
	 */
	public void calculateCost(double consumptionPer100km, double unitCost, HashMap<Integer, Double> linkCharges){
	
		double cost = 0.0;
		for (DirectedEdge edge: edges) {
			SimpleFeature sf = (SimpleFeature) edge.getObject();
			double len = (double) sf.getAttribute("LenNet"); //in [km]
			cost += len / 100 * consumptionPer100km * unitCost ;
			
			//if congestion charged
			if (linkCharges != null && linkCharges.containsKey(edge.getID())) 
					cost += linkCharges.get(edge.getID());
		}
		this.cost = cost;
	}
	
	/**
	 * Calculates the utility of the route.
	 * @param linkTravelTime Link travel times.
	 * @param avgIntersectionDelay Average intersection delay.
	 * @param consumptionPer100km Fuel consumption per 100 km.
	 * @param unitCost Unit cost of fuel.
	 * @param params Route choice parameters.
	 */
	public void calculateUtility(Map<Integer, Double> linkTravelTime, double consumptionPer100km, double unitCost, HashMap<Integer, Double> linkCharges, Properties params) {
		
		if (this.length == null) this.calculateLength(); //calculate only once (length is not going to change)
				
		double avgIntersectionDelay;
		if (params == null) { //use default parameters
			avgIntersectionDelay = AVERAGE_INTERSECTION_DELAY;
		} else {
			avgIntersectionDelay = Double.parseDouble(params.getProperty("AVG_INTERSECTION_DELAY"));
		}
	
		this.calculateTravelTime(linkTravelTime, avgIntersectionDelay); //always (re)calculate
		
		this.calculateCost(consumptionPer100km, unitCost, linkCharges); //always (re)calculate
		
		double length = this.getLength();
		double time = this.getTime();
		double cost = this.getCost();
		int intersec = this.getNumberOfIntersections();
		
		double paramTime, paramLength, paramCost, paramIntersections;
		if (params == null) { //use default parameters
			paramTime = PARAM_TIME;
			paramLength = PARAM_LENGTH;
			paramCost = PARAM_COST;
			paramIntersections = PARAM_INTERSECTIONS;
			avgIntersectionDelay = AVERAGE_INTERSECTION_DELAY;
		} else {
			paramTime = Double.parseDouble(params.getProperty("TIME"));
			paramLength = Double.parseDouble(params.getProperty("LENGTH"));
			paramCost = Double.parseDouble(params.getProperty("COST"));
			paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));
			avgIntersectionDelay = Double.parseDouble(params.getProperty("AVG_INTERSECTION_DELAY"));
		}
		
		double utility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersec;  
		this.utility = utility;
	}
	
	public boolean isEmpty() {
		
		return this.edges.isEmpty();
	}
	
	public boolean contains(Edge edge) {
		
		return this.edges.contains(edge);
	}
	
	public boolean contains(int edgeID) {
		
		for (Edge edge: this.edges)
			if (edge.getID() == edgeID) return true;
		return false;
	}

	public Double getLength() {
		
		return length;
	}
	
	public Double getTime() {
	
		return time;
	}
	
	public Double getCost() {
		
		return cost;
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
	
//	public int getID() {
//		
//		return id;
//	}
	
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
	public boolean equals(Object obj) { //TODO override hashCode too
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
