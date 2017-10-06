package nismod.transport.network.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedNode;

import nismod.transport.utility.RandomSingleton;

/**
 * RouteSet is a choice set of possible routes between origin and destination node
 * @author Milan Lovric
 *
 */
public class RouteSet {
	
	private DirectedNode originNode;
	private DirectedNode destinationNode;
	private List<Route> choiceSet;
	private ArrayList<Double> probabilities;
	private HashMap<Integer, Double> linkTravelTime;
	private Properties params;
	
	/**
	 * @param originNode
	 * @param destinationNode
	 */
	public RouteSet(DirectedNode originNode, DirectedNode destinationNode) {
		this.originNode = originNode;
		this.destinationNode = destinationNode;
		
		choiceSet = new ArrayList<Route>();
	}
	
	/**
	 * @param originNode
	 * @param destinationNode
	 */
	public RouteSet(DirectedNode originNode, DirectedNode destinationNode, HashMap<Integer, Double> linkTravelTime) {
		this.originNode = originNode;
		this.destinationNode = destinationNode;
		this.linkTravelTime = linkTravelTime;
		choiceSet = new ArrayList<Route>();
	}
	
	/**
	 * Adds a route to the choice set.
	 * @param route Route to be added.
	 */
	public void addRoute(Route route) {
		
		//TODO
		//test route validity
		if (!route.isValid()) {
			//System.err.println("Trying to add a non-valid route to the choice set. Ignoring request.");
			return;
		}
			
		//test that origin and destination nodes match the first and the last node of the route
		if (!route.getOriginNode().equals(this.originNode) || !route.getDestinationNode().equals(this.destinationNode)) {
			//System.err.println("Trying to add a route with wrong origin/destination node to the choice set. Ignoring request.");
			return;
		}
	
		for (Route r: choiceSet)
			if (r.equals(route)) {
				//System.err.println("Trying to add a duplicate route to the choice set. Ignoring request.");
				return;
			}
	
		//add route to the choice set
		choiceSet.add(route);
	}
		
	/**
	 * Sorts routes on their utility in a descending order.
	 */
	public void sortRoutesOnUtility() {

		Comparator<Route> c = new Comparator<Route>() {
		public int compare(Route r, Route r2) {
		    	Double utility = r.getUtility();
		       	Double utility2 = r2.getUtility();
		       	return utility2.compareTo(utility);
		    	}
		};
		
		Collections.sort(choiceSet, c);
		
		//need to re-calculate probabilities as the order of the choice set has changed
		this.calculateProbabilities();
	}
	
	/**
	 * Prints the entire choice set.
	 */
	public void printChoiceSet() {
		
		System.out.printf("Choice set for origin node %d and destination node %d: \n", originNode.getID(), destinationNode.getID());
		for (Route r: choiceSet) 
			System.out.println(r.toString());
	}
	
	/**
	 * Calculates choice probabilities using logit formula.
	 */
	public void calculateProbabilities(HashMap<Integer, Double> linkTravelTime, Properties params) {
		
		//store arguments into instance fields
		this.linkTravelTime = linkTravelTime;
		this.params = params;
				
		ArrayList<Double> probabilities = new ArrayList<Double>();
		for (Route r: choiceSet) probabilities.add(0.0);
		
		double sum = 0.0;
		
		//all routes need to have utility calculated, if not calculate it
		for (Route r: choiceSet) {
						
			if (r.getUtility() == null) r.calculateUtility(linkTravelTime, params);
			sum += Math.exp(r.getUtility());
		}
		
		for (int index = 0; index < choiceSet.size(); index++) {
			double probability = Math.exp(choiceSet.get(index).getUtility()) / sum;
			probabilities.set(index, probability);
		}
		/*
		System.out.println("Utility / Probability");
		for (int index = 0; index < choiceSet.size(); index++)
			System.out.printf("%.2f / %.2f \n", choiceSet.get(index).getUtility(), probabilities.get(index));
		*/
		this.probabilities = probabilities;
	}
	
	/**
	 * Re-calculates utilities for all the routes.
	 */
	public void calculateUtilities(HashMap<Integer, Double> linkTravelTime, Properties params) {
		
		//store arguments into instance fields
		this.linkTravelTime = linkTravelTime;
		this.params = params;
		
		//re-calculate utility for all the routes
		for (Route r: choiceSet)
			r.calculateUtility(linkTravelTime, params);
	}
	
	/**
	 * Re-calculates utilities for all the routes.
	 */
	public void calculateUtilities() {
		
		//re-calculate utility for all the routes
		for (Route r: choiceSet)
			r.calculateUtility(this.linkTravelTime, this.params);
	}
	
	
	/**
	 * Calculates choice probabilities using logit formula.
	 */
	public void calculateProbabilities() {
		
		if (this.linkTravelTime == null) { 
			System.err.println("Before calculating probabilities of a choice set, set link travel times to be used for the calculation!");
			return;
		}
		if (this.params == null) { 
			System.err.println("Before calculating probabilities of a choice set, set parameter values to be used for the calculation!");
			return;
		}
				
		calculateProbabilities(this.linkTravelTime, this.params);
	}
	
	/**
	 * Getter method for choice probabilities.
	 * @return Choice probabilities.
	 */
	public ArrayList<Double> getProbabilities() {
		
		return this.probabilities;
	}
	
	public ArrayList<Double> getUtilities() {
		
		ArrayList<Double> utilities = new ArrayList<Double>();
		
		for (Route r: this.choiceSet) {
			utilities.add(r.getUtility());
		}
		
		return utilities;
		
	}
	
	
//	/**
//	 * Chooses a route based on the probabilities.
//	 * @return Chosen route.
//	 */
//	public Route choose() {
//		
//		//probabilities must be calculated at least once
//		if (probabilities == null) {
//			this.calculateProbabilities();
//			this.sortRoutesOnUtility();
//		}
//		
//		RandomSingleton rng = RandomSingleton.getInstance();
//			
//		//choose route
//		double cumulativeProbability = 0.0;
//		double random = rng.nextDouble();
//		int chosenIndex = -1;
//		for (int index = 0; index < choiceSet.size(); index++) {
//			cumulativeProbability += this.probabilities.get(index);
//			if (Double.compare(cumulativeProbability, random) > 0) {
//				chosenIndex = index;
//				break;
//			}
//		}
//		if (chosenIndex == -1) return null;
//		else return choiceSet.get(chosenIndex);
//	}
	
	/**
	 * Chooses a route based on the probabilities.
	 * @param params The parameters of the route choice model.
	 * @return Chosen route.
	 */
	public Route choose(Properties params) {
		
		//probabilities must be calculated at least once
		if (probabilities == null) {
			this.calculateProbabilities(this.linkTravelTime, params);
			this.sortRoutesOnUtility();
		}
		
		RandomSingleton rng = RandomSingleton.getInstance();
			
		//choose route
		double cumulativeProbability = 0.0;
		double random = rng.nextDouble();
		int chosenIndex = -1;
		for (int index = 0; index < choiceSet.size(); index++) {
			cumulativeProbability += this.probabilities.get(index);
			if (Double.compare(cumulativeProbability, random) > 0) {
				chosenIndex = index;
				break;
			}
		}
		if (chosenIndex == -1) return null;
		else return choiceSet.get(chosenIndex);
	}
	
	/**
	 * Gets index of a route in the choice set.
	 * @param route The route which index is sought for.
	 * @return
	 */
	public int getIndexOfRoute(Route route) {
		
		return choiceSet.indexOf(route); //assuming no route repetition in the choice set
	}
	
	public DirectedNode getOriginNode() {
		
		return this.originNode;
	}
	
	public DirectedNode getDestinationNode() {
		
		return this.destinationNode;
	}
	
	public List<Route> getChoiceSet() {
		
		return this.choiceSet;
	}
	
	public int getSize() {

		return this.choiceSet.size();
	}
	
	public void setLinkTravelTime(HashMap<Integer, Double> linkTravelTime) {
		
		this.linkTravelTime = linkTravelTime;
	}
	
	public void setParameters(Properties params) {
		
		this.params = params;
	}
	
	public void printStatistics() {
		
		System.out.printf("Statistics for route set from %d to %d: %d distinct routes. \n", this.originNode.getID(), this.destinationNode.getID(), this.choiceSet.size());
		
	}
	
	public void printProbabilities() {

		System.out.println("Probabilities: ");
		System.out.println(this.probabilities);
	}
	
	public void printUtilities() {

		System.out.println("Utilities: ");
		System.out.println(this.getUtilities());
	}
}