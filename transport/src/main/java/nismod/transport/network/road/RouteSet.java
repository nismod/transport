package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedNode;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.utility.RandomSingleton;

/**
 * RouteSet is a choice set of possible routes between an origin and a destination node.
 * @author Milan Lovric
 *
 */
public class RouteSet {
	
	private final static Logger LOGGER = LogManager.getLogger(RouteSet.class);
	
	//private DirectedNode originNode;
	//private DirectedNode destinationNode;
	private List<Route> choiceSet;
	//private ArrayList<Double> probabilities;
	private double[] probabilities;
	private double[] pathsizes;
	private RoadNetwork roadNetwork;
	
	/**
	 * Constructor.
	 * @param roadNetwork Road network.
	 */
	public RouteSet(RoadNetwork roadNetwork) {
		//this.originNode = originNode;
		//this.destinationNode = destinationNode;
		this.roadNetwork = roadNetwork;
		
		this.choiceSet = new ArrayList<Route>(RouteSetGenerator.INITIAL_ROUTE_SET_CAPACITY);
	}
	
	/**
	 * Adds a route to the choice set.
	 * @param route Route to be added.
	 */
	public void addRoute(Route route) {
		
		//TODO
		//test route validity
		if (!route.isValid()) {
			LOGGER.warn("Trying to add a non-valid route to the choice set. Ignoring request.");
			return;
		}
		//test that origin and destination nodes match the first and the last node of the route
		if (!this.choiceSet.isEmpty()) { //if not empty

			//check that origin and destination nodes are ok
			if (!route.getOriginNode().equals(this.getOriginNode()) || !route.getDestinationNode().equals(this.getDestinationNode())) {
				LOGGER.warn("Trying to add a route with wrong origin/destination node to the choice set. Ignoring request.");
				return;
			}
			//check that route does not already exist
			for (Route r: this.choiceSet)
				if (r.equals(route)) {
					LOGGER.trace("Trying to add a duplicate route to the choice set. Ignoring request.");
					return;
				}
		}
		//add route to the choice set
		this.choiceSet.add(route);
	}
	
	/**
	 * Adds a route to the choice set.
	 * @param route Route to be added.
	 */
	public void addRouteWithoutValidityCheck(Route route) {
		
		//TODO
		//test that origin and destination nodes match the first and the last node of the route
		if (!this.choiceSet.isEmpty()) { //if not empty

			//check that origin and destination nodes are ok
			if (!route.getOriginNode().equals(this.getOriginNode()) || !route.getDestinationNode().equals(this.getDestinationNode())) {
				LOGGER.warn("Trying to add a route with wrong origin/destination node to the choice set. Ignoring request.");
				return;
			}
			//check that route does not already exist
			for (Route r: this.choiceSet)
				if (r.equals(route)) {
					LOGGER.trace("Trying to add a duplicate route to the choice set. Ignoring request.");
					return;
				}
		}
		//add route to the choice set
		this.choiceSet.add(route);
	}
	
	/**
	 * Adds a route to the choice set.
	 * @param route Route to be added.
	 */
	public void addRouteWithoutValidityAndEndNodesCheck(Route route) {
		
		//TODO
		//test that origin and destination nodes match the first and the last node of the route
		if (!this.choiceSet.isEmpty()) { //if not empty

			//check that route does not already exist
			for (Route r: choiceSet)
				if (r.equals(route)) {
					LOGGER.trace("Trying to add a duplicate route to the choice set. Ignoring request.");
					return;
				}
		}
		//add route to the choice set
		this.choiceSet.add(route);
	}
	
	/**
	 * Adds a route to the choice set.
	 * @param route Route to be added.
	 */
	public void addRouteWithoutAnyChecks(Route route) {
		
		//add route to the choice set
		this.choiceSet.add(route);
	}
		
	/**
	 * Prints the entire choice set.
	 */
	public void printChoiceSet() {
		
		if (this.choiceSet.isEmpty()) return;
		
		System.out.printf("Choice set for origin node %d and destination node %d: \n", getOriginNode().getID(), getDestinationNode().getID());
		for (Route r: this.choiceSet) 
			System.out.println(r.toString());
	}
	
	/**
	 * Calculates choice probabilities using logit formula.
	 */
	public void calculateProbabilities() {
		
		this.probabilities = new double[this.choiceSet.size()];

		//if just one route in the route set, set probability to 1.0
		if (this.choiceSet.size() == 1) this.probabilities[0] = 1.0;

		else { //otherwise calculate probabilities using logit formula

			double sum = 0.0;

			//all routes need to have a utility calculated
			for (Route r: this.choiceSet) {
				if (!r.getEdges().isEmpty() && Double.compare(r.getUtility(), 0.0d) == 0)
					//System.err.printf("Route %d does not have a calculated utility! %n", r.getID());
					LOGGER.warn("Route with edges does not have a calculated utility! Probabilities will be wrongly calculated.");
				else
					sum += Math.exp(r.getUtility());
			}

			for (int index = 0; index < this.choiceSet.size(); index++) {
				double probability = Math.exp(this.choiceSet.get(index).getUtility()) / sum;
				this.probabilities[index] = probability;
			}
			/*
		System.out.println("Utility / Probability");
		for (int index = 0; index < choiceSet.size(); index++)
			System.out.printf("%.2f / %.2f \n", choiceSet.get(index).getUtility(), probabilities.get(index));
			 */

		}
	}
	
	/**
	 * Re-calculates utilities for all the routes.
	 * @param vht Vehicle type.
	 * @param et Engine type.
	 * @param linkTravelTime Link travel times.
	 * @param energyConsumptionParameters Base year energy consumption parameters.
	 * @param relativeFuelEfficiency Relative fuel efficiency compared to the base year.
	 * @param energyUnitCosts Energy unit costs.
	 * @param linkCharges Congestion charges.
	 * @param params Route choice parameters.
	 */
	public void calculateUtilities(VehicleType vht, EngineType et, Map<Integer, Double> linkTravelTime, HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters, HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency, Map<EnergyType, Double> energyUnitCosts, HashMap<String, HashMap<Integer, Double>> linkCharges, Properties params) {
		
		//re-calculate utility for all the routes
		for (Route r: this.choiceSet)
			r.calculateUtility(vht, et, linkTravelTime, energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, linkCharges, params);
		
		//correct for correlation with path-size
		this.correctUtilitiesWithPathSize();
		
		//sort routes on utility (optional)
		//this.sortRoutesOnUtility();
	}
	
	/**
	 * Getter method for choice probabilities.
	 * @return Choice probabilities.
	 */
	public ArrayList<Double> getProbabilitiesAsList() {

		ArrayList<Double> arrayList = new ArrayList<Double>(this.probabilities.length);
		for (double p: this.probabilities) arrayList.add(p);
		return arrayList;
	}
	
	/**
	 * Getter method for choice probabilities.
	 * @return Choice probabilities.
	 */
	public double[] getProbabilities() {

		return this.probabilities;
	}
	
	/**
	 * Getter method for pathsizes.
	 * @return Choice pathsizes.
	 */
	public double[] getPathsizes() {

		return this.pathsizes;
	}
		
	/**
	 * Getter method for choice utilities.
	 * @return Choice utilities.
	 */
	public ArrayList<Double> getUtilities() {
		
		ArrayList<Double> utilities = new ArrayList<Double>();
		
		for (Route r: this.choiceSet) {
			utilities.add(r.getUtility());
		}
		
		return utilities;
	}
	
	/**
	 * Calculate path sizes (also calculates route lengths if they had not been calculated before).
	 */
	public void calculatePathsizes() {
		
		this.pathsizes = new double[this.choiceSet.size()];

		//if just one route in the route set, set pathsize to 1.0
		if (this.choiceSet.size() == 1) this.pathsizes[0] = 1.0;

		else { //otherwise calculate pathsizes
			for (int index = 0; index < this.choiceSet.size(); index++) {
				Route i = this.choiceSet.get(index);
				if (i.getEdges().isEmpty()) {
					LOGGER.warn("There shouldn't be a single node route in a route set with multiple routes!");
					continue; //do not calculate for single node routes without edges (pathSize will be zero)
				}
				double pathSize = 0.0;
				for (int a: i.getEdges().toArray()) {
					double edgeLength = roadNetwork.getEdgeLength(a);
					if (Double.compare(i.getLength(), 0.0d) == 0) 
						i.calculateLength(); //calculate only once (length is not going to change)
					double firstTerm = edgeLength / i.getLength();
					double secondTerm = 0.0;
					for (Route j: this.choiceSet)
						if (j.getEdges().contains(a)) {
							if (Double.compare(j.getLength(), 0.0d) == 0) 
								j.calculateLength(); //calculate only once (length is not going to change)
							secondTerm += i.getLength() / j.getLength();
						}
					pathSize += firstTerm / secondTerm;
				}
				this.pathsizes[index] = pathSize;
			}
		}
	}
	
	/**
	 * Correct each route's utility with (pre-calculated) pathsize (a measure of correlation with other routes in the choice set).
	 */
	private void correctUtilitiesWithPathSize() {
		
		if (this.choiceSet.size() == 1) return; //not needed for just one route
		
		for (int index = 0; index < this.choiceSet.size(); index++) {
			//correct utility with pathsize
			Route i = this.choiceSet.get(index);
			double utility = i.getUtility();
			
			//if pathsizes have not been pre-calculated, calculate here
			if (this.pathsizes == null) this.calculatePathsizes();
			
			double pathsize = this.pathsizes[index];
			if (pathsize > 0)
				utility += Math.log(pathsize);
			i.setUtility(utility);
		}
	}
	
	/**
	 * Corrects utility with path size for a particular route within the choice set.
	 * @param routeIndex index of the route (list element) within the choice set
	 */
	public void correctUtilityWithPathSize(int routeIndex) {

		Route i = this.choiceSet.get(routeIndex);
		double pathSize = 0.0;
		for (int a: i.getEdges().toArray()) {

			//SimpleFeature sf = (SimpleFeature) a.getObject();
			//double edgeLength = (double) sf.getAttribute("LenNet"); //in [km]
			double edgeLength = roadNetwork.getEdgeLength(a);
			double firstTerm = edgeLength / i.getLength();

			double secondTerm = 0.0;
			for (Route j: this.choiceSet)
				if (j.getEdges().contains(a))
					secondTerm += i.getLength() / j.getLength();

			pathSize += firstTerm / secondTerm;
		}
		//correct utility with pathsize
		double utility = i.getUtility();
		utility += Math.log(pathSize);
		i.setUtility(utility);
	}
	
	/**
	 * Sorts routes on their utility in a descending order.
	 */
	private void sortRoutesOnUtility() {

		Comparator<Route> c = new Comparator<Route>() {
		public int compare(Route r, Route r2) {
		    	Double utility = r.getUtility();
		       	Double utility2 = r2.getUtility();
		       	return utility2.compareTo(utility);
		    	}
		};
		
		Collections.sort(choiceSet, c);
		
		//need to re-calculate probabilities as the order of the choice set has changed
		//this.calculateProbabilities();
	}
	
	/**
	 * Chooses a route based on the probabilities.
	 * @param params Parameters of the route choice model.
	 * @return Chosen route.
	 */
	public Route choose(Properties params) {
		
		//probabilities must be calculated at least once
		if (probabilities == null) {
			LOGGER.error("Cannot choose the route before the choice probabilities are calculated!");
			return null;
		}
				
		RandomSingleton rng = RandomSingleton.getInstance();
			
		//choose route
		double cumulativeProbability = 0.0;
		double random = rng.nextDouble();
		int chosenIndex = -1;
		for (int index = 0; index < choiceSet.size(); index++) {
			cumulativeProbability += this.probabilities[index];
			if (Double.compare(cumulativeProbability, random) > 0) {
				chosenIndex = index;
				break;
			}
		}
		if (chosenIndex == -1) return null;
		else return choiceSet.get(chosenIndex);
	}
	
	/**
	 * Gets the index of a route in the choice set.
	 * @param route The route which index is sought for.
	 * @return Route index.
	 */
	public int getIndexOfRoute(Route route) {
		
		return choiceSet.indexOf(route); //assuming no route repetition in the choice set
	}
	
	/**
	 * @return Origin node of the choice set.
	 */
	public DirectedNode getOriginNode() {
		
		DirectedNode originNode = null;
		if (this.choiceSet != null)
			originNode = this.choiceSet.get(0).getOriginNode(); //get first edge and its origin node
				
		return originNode;
	}
	
	/**
	 * @return Destination node of the choice set.
	 */
	public DirectedNode getDestinationNode() {
		
		DirectedNode destinationNode = null;
		if (this.choiceSet != null)
			destinationNode = this.choiceSet.get(0).getDestinationNode(); //get first route and its destination node
				
		return destinationNode;
	}
	
	/**
	 * Getter method for the choice set.
	 * @return Choice set (list of routes).
	 */
	public List<Route> getChoiceSet() {
		
		return this.choiceSet;
	}
	
	/**
	 * @return Size of the choice set (number of routes).
	 */
	public int getSize() {

		return this.choiceSet.size();
	}
	
	/**
	 * Prints statistic for the route set (choice set size for each node pair).
	 */
	public void printStatistics() {
		
		System.out.printf("Statistics for route set from %d to %d: %d distinct routes. \n", this.getOriginNode().getID(), this.getDestinationNode().getID(), this.choiceSet.size());
		
	}
	
	/**
	 * Prints probabilities for the route set.
	 */
	public void printProbabilities() {

		System.out.println("Probabilities: ");
		System.out.println(Arrays.toString(this.probabilities));
	}
	
	/**
	 * Prints pathsizes for the route set.
	 */
	public void printPathsizes() {

		System.out.println("Pathsizes: ");
		System.out.println(Arrays.toString(this.pathsizes));
	}
	
	/**
	 * Prints utilities for the route set.
	 */
	public void printUtilities() {

		System.out.println("Utilities: ");
		System.out.println(this.getUtilities());
	}
}