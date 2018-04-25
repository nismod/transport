package nismod.transport.optimisation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.RandomSingleton;

/**
 * Implements SPSA optimisation algorithm (Simultaneous Perturbation Stochastic Approximation). 
 * http://www.jhuapl.edu/SPSA/
 * @author Milan Lovric
  */
public class SPSA3 {
	
	private final static Logger LOGGER = LogManager.getLogger(SPSA3.class);
	
	//maximum and minimum values of OD matrix flows (i.e. constraints)
	public static final double THETA_MAX_FLOW = 10000000.0;
	public static final double THETA_MIN_FLOW = 0.0;
	//maximum and minimum values of node probabilities (i.e. constraints)
	public static final double THETA_MAX_PROBABILITY = 1.0;
	public static final double THETA_MIN_PROBABILITY = 0.0;
	
	//SPSA parameters
	private double a1;
	private double A1;
	private double c1;
	private double a2;
	private double A2;
	private double c2;
	private double alpha;
	private double gamma;
	
	private RealODMatrix thetaEstimate; //contains the final result
	private RealODMatrix deltas;
	private RealODMatrix gradientApproximation;
	
	private HashMap<Integer, Double> thetaEstimateStart; //contains the final result
	private HashMap<Integer, Integer> deltasStart;
	private HashMap<Integer, Double> gradientApproximationStart;
	
	private HashMap<Integer, Double> thetaEstimateEnd; //contains the final result
	private HashMap<Integer, Integer> deltasEnd;
	private HashMap<Integer, Double> gradientApproximationEnd;
	
	private List<Double> lossFunctionValues;
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Properties routeChoiceParams;

	public SPSA3() {
	}
	
	/**
	 * Initialise the SPSA algorithm with starting values.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param routeChoiceParams Route choice parameters.
	 * @param initialTheta Initial OD matrix.
	 * @param initialThetaStart Initial start node probabilities.
	 * @param initialThetaEnd Initial end node probabilities.
	 * @param a1 SPSA parameter for OD estimation.
	 * @param A1 SPSA parameter for OD estimation.
	 * @param c1 SPSA parameter for OD estimation.
	 * @param a2 SPSA parameter for nodes probability estimation.
	 * @param A2 SPSA parameter for nodes probability estimation.
	 * @param c2 SPSA parameter for nodes probability estimation.
	 * @param alpha SPSA parameter.
	 * @param gamma SPSA parameter.
	 */
	public void initialise(RoadNetworkAssignment rna, RouteSetGenerator rsg, Properties routeChoiceParams, RealODMatrix initialTheta, HashMap<Integer, Double> initialThetaStart, HashMap<Integer, Double> initialThetaEnd, double a1, double A1, double c1, double a2, double A2, double c2, double alpha, double gamma) {
			
		this.rna = rna;
		this.rsg = rsg;
		this.routeChoiceParams = routeChoiceParams;
		
		this.thetaEstimate = initialTheta.clone();
		this.thetaEstimateStart = new HashMap<Integer, Double>(initialThetaStart);
		this.thetaEstimateEnd = new HashMap<Integer, Double>(initialThetaEnd);
		
		this.deltas = new RealODMatrix();
		this.deltasStart = new HashMap<Integer, Integer>();
		this.deltasEnd = new HashMap<Integer, Integer>();
		
		this.gradientApproximation = new RealODMatrix();
		this.gradientApproximationStart = new HashMap<Integer, Double>();
		this.gradientApproximationEnd = new HashMap<Integer, Double>();
		
		this.lossFunctionValues = new ArrayList<Double>();
		
		this.a1 = a1;
		this.A1 = A1;
		this.c1 = c1;
		this.a2 = a2;
		this.A2 = A2;
		this.c2 = c2;
		this.alpha = alpha;
		this.gamma = gamma;
	}
		
	/**
	 * Run the algorithm.
	 * @param maxIterations Maximum number of iterations.
	 */
	public void runSPSA(int maxIterations) {
		
		int k = 1; //counter

		do {		
			//evaluate loss function for the current theta
			double loss = this.lossFunction(this.thetaEstimate, this.thetaEstimateStart, this.thetaEstimateEnd);
			//store
			this.lossFunctionValues.add(loss);
			
			//calculate gain coefficients
			double ak1 = a1 / Math.pow(A1 + k, alpha);
			double ck1 = c1 / Math.pow(k, gamma);
			double ak2 = a2 / Math.pow(A2 + k, alpha);
			double ck2 = c2 / Math.pow(k, gamma);
			//System.out.printf("ak1 = %.5f, ck1 = %.5f %n", ak1, ck1);
			//System.out.printf("ak2 = %.5f, ck2 = %.5f %n", ak2, ck2);
						
			//generate deltas	
			generateDeltas(thetaEstimate.getKeySet());
			//this.deltas.printMatrixFormatted("Deltas: ");
			//System.out.println("Start deltas: " + this.deltasStart.values());
			//System.out.println("End deltas: " + this.deltasEnd.values());

			//calculate shifted thetas
			RealODMatrix thetaPlus = this.shiftTheta(this.thetaEstimate, ck1, this.deltas);
			RealODMatrix thetaMinus = this.shiftTheta(this.thetaEstimate, -1 * ck1, this.deltas);
			//thetaPlus.printMatrixFormatted("Theta plus: ");
			//thetaMinus.printMatrixFormatted("Theta minus: ");
			HashMap<Integer, Double> thetaStartPlus = shiftTheta(this.thetaEstimateStart, ck2, this.deltasStart, true);
			HashMap<Integer, Double> thetaEndPlus = shiftTheta(this.thetaEstimateEnd, ck2, this.deltasEnd, false);
			HashMap<Integer, Double> thetaStartMinus = shiftTheta(this.thetaEstimateStart, -1 * ck2, this.deltasStart, true);
			HashMap<Integer, Double> thetaEndMinus = shiftTheta(this.thetaEstimateEnd, -1 * ck2, this.deltasEnd, false);
			//System.out.println("Theta start plus: " + thetaStartPlus);
			//System.out.println("Theta start minus: " + thetaStartMinus);
			//System.out.println("Theta end plus: " + thetaEndPlus);
			//System.out.println("Theta end minus: " + thetaEndMinus);
						
			//evaluate loss function
			double yPlus = lossFunction(thetaPlus, thetaStartPlus, thetaEndPlus);
			double yMinus = lossFunction(thetaMinus, thetaStartMinus, thetaEndMinus);
			//System.out.printf("yPlus = %.5f, yMinus = %.5f %n", yPlus, yMinus);
						
			//approximate gradient
			approximateGradient(yPlus, yMinus, ck1, this.deltas);
			//gradientApproximation.printMatrixFormatted("Gradient approximation: ");
			approximateGradient(this.gradientApproximationStart, yPlus, yMinus, ck2, this.deltasStart);
			approximateGradient(this.gradientApproximationEnd, yPlus, yMinus, ck2, this.deltasEnd);
			//System.out.println("Gradient approximation start: " + this.gradientApproximationStart);
			//System.out.println("Gradient approximation end: " + this.gradientApproximationEnd);
				
			//estimate new theta
			this.updateThetaEstimate(this.thetaEstimate, ak1, this.gradientApproximation);
			//thetaEstimate.printMatrixFormatted("New theta estimate: ");
			updateThetaEstimate(this.thetaEstimateStart, ak2, this.gradientApproximationStart, true);
			updateThetaEstimate(this.thetaEstimateEnd, ak2, this.gradientApproximationEnd, false);
			//System.out.println("New theta estimate start: " + this.thetaEstimateStart);
			//System.out.println("New theta estimate end: " + this.thetaEstimateEnd);
			
			k++;

		} while (k <= maxIterations);
		
		//evaluate loss function for the final theta
		double loss = this.lossFunction(this.thetaEstimate, this.thetaEstimateStart, this.thetaEstimateEnd);
		//store
		this.lossFunctionValues.add(loss);
		
		System.out.println("SPSA stopped. Maximum number of iterations reached.");
	}
	
	/**
	 * Getter function for loss function evaluations for all iterations.
	 * @return Loss function evaluations for all iterations.
	 */
	public List<Double> getLossFunctionEvaluations() {
		
		return this.lossFunctionValues;
		
	}
	
	/**
	 * Getter function for the optimisation result (OD matrix).
	 * @return Estimated OD matrix.
	 */
	public RealODMatrix getThetaEstimate() {
		
		return this.thetaEstimate;
	}
	
	/**
	 * Getter function for the optimisation result (start nodes probabilities).
	 * @return Estimated start nodes probabilities.
	 */
	public HashMap<Integer, Double> getThetaEstimateStart() {
		
		return this.thetaEstimateStart;
	}
	
	/**
	 * Getter function for the optimisation result (end nodes probabilities).
	 * @return Estimated end nodes probabilities.
	 */
	public HashMap<Integer, Double> getThetaEstimateEnd() {
		
		return this.thetaEstimateEnd;
	}
	
	/**
	 * Saves node probabilities to an output file.
	 * @param outputFile Output file name (with path).
	 */
	public void saveNodeProbabilities(String outputFile) {

		Set<Integer> nodes = this.rna.getRoadNetwork().getNodeIDtoNode().keySet();

		String NEW_LINE_SEPARATOR = System.lineSeparator();
		ArrayList<String> header = new ArrayList<String>();
		header.add("nodeID");
		header.add("startProb");
		header.add("nodeProb");
		
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);

			for (int nodeID: nodes) {
				ArrayList<String> record = new ArrayList<String>();
				record.add(Integer.toString(nodeID));
				Double startProb = this.getThetaEstimateStart().get(nodeID);
				if (startProb == null) startProb = 0.0;
				record.add(Double.toString(startProb));
				Double endProb = this.getThetaEstimateEnd().get(nodeID);
				if (endProb == null) endProb = 0.0;
				record.add(Double.toString(endProb));
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
	/**
	 * Generates deltas using the Rademacher distribution (i.e. random -1 or 1).
	 */
	private void generateDeltas(Set<MultiKey> keySet) {
		
		RandomSingleton rng = RandomSingleton.getInstance();

		for (MultiKey mk: keySet) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
							
			double delta = Math.round(rng.nextDouble()) * 2.0 - 1.0;
			deltas.setFlow(origin, destination, delta);
		}
		for (Integer nodeID: this.thetaEstimateStart.keySet()) {
			int delta = (int) Math.round(rng.nextDouble()) * 2 - 1;
			this.deltasStart.put(nodeID, delta);
		}
		for (Integer nodeID: this.thetaEstimateEnd.keySet()) {
			int delta = (int) Math.round(rng.nextDouble()) * 2 - 1;
			this.deltasEnd.put(nodeID, delta);
		}
	}
	

	/**
	 * Calculate new OD matrix with shifted values.
	 * @param theta Current OD matrix.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private RealODMatrix shiftTheta(RealODMatrix theta, double ck, RealODMatrix deltas) {
		
		RealODMatrix shiftedTheta = new RealODMatrix();
		
		for (MultiKey mk: theta.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double flow = theta.getFlow(origin, destination);
			double delta = deltas.getFlow(origin, destination);
			double newFlow = flow + ck * delta;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX_FLOW);
			newFlow = Math.max(newFlow, THETA_MIN_FLOW);
			
			shiftedTheta.setFlow(origin, destination, newFlow);
		}
		
		return shiftedTheta;
	}

	/**
	 * Calculate new node probabilities with shifted values.
	 * @param theta Current node probabilities.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private HashMap<Integer, Double> shiftTheta(HashMap<Integer, Double> theta, double ck, HashMap<Integer, Integer> deltas, boolean startNodes) {
		
		HashMap<Integer, Double> shiftedTheta = new HashMap<Integer, Double>();
		
		for (Integer nodeID: theta.keySet()) {
		
			double probability = theta.get(nodeID);
			double delta = deltas.get(nodeID);
			double newProbability = probability + ck * delta;
			
			//apply constraints
			newProbability = Math.min(newProbability, THETA_MAX_PROBABILITY);
			newProbability = Math.max(newProbability, THETA_MIN_PROBABILITY);
				
			shiftedTheta.put(nodeID, newProbability);
		}
		
		//normalise probabilities (PER ZONE!)
		RoadNetwork roadNetwork = rna.getRoadNetwork();

		if (startNodes) {

			for (String zone: roadNetwork.getZoneToNodes().keySet()) {
				double sum = 0.0;
				//System.out.println(roadNetwork.getZoneToNodes().get(zone));
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsStartNode(nodeID)) sum += shiftedTheta.get(nodeID);
				}
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsStartNode(nodeID)) shiftedTheta.put(nodeID, shiftedTheta.get(nodeID) / sum);
					else shiftedTheta.put(nodeID, 0.0);
				}
			}

		} else { //end nodes

			for (String zone: roadNetwork.getZoneToNodes().keySet()) {
				double sum = 0.0;
				//System.out.println(roadNetwork.getZoneToNodes().get(zone));
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsEndNode(nodeID)) sum += shiftedTheta.get(nodeID);
				}
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsEndNode(nodeID)) shiftedTheta.put(nodeID, shiftedTheta.get(nodeID) / sum);
					else shiftedTheta.put(nodeID, 0.0);
				}
			}
		}
		
		return shiftedTheta;
	}
		
	/**
	 * Calculate the loss function for given thetas (OD matrix and node probabilities).
	 * @param thetaStart Start node probabilities.
	 * @param thestEnd End node probabilities.
	 * @return RMSN for the difference between volumes and traffic counts.
	 */
	private double lossFunction(RealODMatrix theta, HashMap<Integer, Double> thetaStart, HashMap<Integer, Double> thetaEnd) {
		
		//round values
		ODMatrix odm = new ODMatrix(theta);
		
		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripStorages();
		
		rna.setStartNodeProbabilities(thetaStart);
		rna.setEndNodeProbabilities(thetaEnd);
		
		//assign passenger flows
		if (rna.getFlagUseRouteChoiceModel())
			rna.assignPassengerFlowsRouteChoice(odm, rsg, routeChoiceParams); //route choice version
		else 
			rna.assignPassengerFlowsRouting(odm, null); //routing version
		rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation
		
		//calculate RMSN
		double RMSN = rna.calculateRMSNforSimulatedVolumes();
		
		return RMSN;
	}
	
	/**
	 * Calculate the loss function of the latest theta estimate (OD matrix).
	 * @return RMSN for the difference between volumes and traffic counts.
	 */
	public double lossFunction() {
		
		return lossFunction(this.thetaEstimate, this.thetaEstimateStart, this.thetaEstimateEnd);
	}
	
	/**
	 * Approximate the gradient for node probabilities.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradient(HashMap<Integer, Double> gradientApproximation, double yPlus, double yMinus, double ck, HashMap<Integer, Integer> deltas) {
		
		for (Integer nodeID: deltas.keySet()) {
			double delta = deltas.get(nodeID);
			double grad = (yPlus - yMinus) / (2 * ck * delta);
			gradientApproximation.put(nodeID, grad);
		}
	}
	
	/**
	 * Approximate the gradient for OD matrix.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradient(double yPlus, double yMinus, double ck, RealODMatrix deltas) {
		
		for (MultiKey mk: deltas.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double delta = deltas.getFlow(origin, destination);
			double grad = (yPlus - yMinus) / (2 * ck * delta);
			this.gradientApproximation.setFlow(origin, destination, grad);
		}
	}
	
	/**
	 * Obtain new theta estimate.
	 * @param theta Old theta (node probabilities).
	 * @param ak Gain.
	 * @param gradient Gradient.
	 */
	private void updateThetaEstimate(HashMap<Integer, Double> theta, double ak, HashMap<Integer, Double> gradient, boolean startNodes) {
		
		for (Integer nodeID: theta.keySet()) {
		
			double prob = theta.get(nodeID);
			double grad = gradient.get(nodeID);
			double newProb = prob - ak * grad;
			
			//apply constraints
			newProb = Math.min(newProb, THETA_MAX_PROBABILITY);
			newProb = Math.max(newProb, THETA_MIN_PROBABILITY);
			
			theta.put(nodeID, newProb);
		}
		
		//normalise probabilities (PER ZONE!)
		RoadNetwork roadNetwork = rna.getRoadNetwork();
		
		if (startNodes) {

			for (String zone: roadNetwork.getZoneToNodes().keySet()) {
				double sum = 0.0;
				//System.out.println(roadNetwork.getZoneToNodes().get(zone));
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsStartNode(nodeID)) sum += theta.get(nodeID);
				}
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsStartNode(nodeID)) theta.put(nodeID, theta.get(nodeID) / sum);
					else theta.put(nodeID, 0.0);
				}
			}

		} else { //end nodes

			for (String zone: roadNetwork.getZoneToNodes().keySet()) {
				double sum = 0.0;
				//System.out.println(roadNetwork.getZoneToNodes().get(zone));
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsEndNode(nodeID)) sum += theta.get(nodeID);
				}
				for (Integer nodeID: roadNetwork.getZoneToNodes().get(zone)) {
					if (!roadNetwork.isBlacklistedAsEndNode(nodeID)) theta.put(nodeID, theta.get(nodeID) / sum);
					else theta.put(nodeID, 0.0);
				}
			}
		} 
	}

	/**
	 * Obtain new theta estimate.
	 * @param theta Old theta (OD matrix).
	 * @param ak Gain.
	 * @param gradient Gradient.
	 */
	private void updateThetaEstimate(RealODMatrix theta, double ak, RealODMatrix gradient) {
		
		for (MultiKey mk: theta.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double flow = theta.getFlow(origin, destination);
			double grad = gradient.getFlow(origin, destination);
			double newFlow = flow - ak * grad;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX_FLOW);
			newFlow = Math.max(newFlow, THETA_MIN_FLOW);
			
			theta.setFlow(origin, destination, newFlow);
		}
	}
}
