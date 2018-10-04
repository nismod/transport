package nismod.transport.optimisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.demand.ODMatrix;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.utility.RandomSingleton;

/**
 * Implements SPSA optimisation algorithm (Simultaneous Perturbation Stochastic Approximation).
 * This version of the algorithm keeps OD matrix constant, but optimises start and end node probabilities
 * (the probability of a trip starting/ending at a particular node within LAD).
 * http://www.jhuapl.edu/SPSA/ 
 * @author Milan Lovric
  */
public class SPSA2 {
	
	private final static Logger LOGGER = LogManager.getLogger(SPSA2.class);
	
	//maximum and minimum values of node probabilities (i.e. constraints)
	public static final double THETA_MAX = 1.0;
	public static final double THETA_MIN = 0.0;
	
	//SPSA parameters
	private double a;
	private double A; 
	private double c; 
	private double alpha;
	private double gamma;
	
	private HashMap<Integer, Double> thetaEstimateStart; //contains the final result
	private HashMap<Integer, Integer> deltasStart;
	private HashMap<Integer, Double> gradientApproximationStart;
	
	private HashMap<Integer, Double> thetaEstimateEnd; //contains the final result
	private HashMap<Integer, Integer> deltasEnd;
	private HashMap<Integer, Double> gradientApproximationEnd;
	
	private List<Double> lossFunctionValues;
	private RoadNetworkAssignment rna;
	private Properties props;
	private ODMatrix odm;

	public SPSA2() {
	}
	
	/**
	 * Initialise the SPSA algorithm with starting values.
	 * @param rna Road network assignment
	 * @param props Parameters from the config file.
	 * @param odm Origin-destination matrix.
	 * @param initialThetaStart Initial start node probabilities.
	 * @param initialThetaEnd Initial end node probabilities.
	 * @param a SPSA parameter.
	 * @param A SPSA parameter.
	 * @param c SPSA parameter.
	 * @param alpha SPSA parameter.
	 * @param gamma SPSA parameter.
	 */
	public void initialise(RoadNetworkAssignment rna, Properties props, ODMatrix odm, HashMap<Integer, Double> initialThetaStart, HashMap<Integer, Double> initialThetaEnd, double a, double A, double c, double alpha, double gamma) {
			
		this.rna = rna;
		this.props = props;
		this.odm = odm;
		this.thetaEstimateStart = new HashMap<Integer, Double>(initialThetaStart);
		this.thetaEstimateEnd = new HashMap<Integer, Double>(initialThetaEnd);
		this.deltasStart = new HashMap<Integer, Integer>();
		this.deltasEnd = new HashMap<Integer, Integer>();
		this.gradientApproximationStart = new HashMap<Integer, Double>();
		this.gradientApproximationEnd = new HashMap<Integer, Double>();
		
		this.lossFunctionValues = new ArrayList<Double>();
		
		this.a = a;
		this.A = A;
		this.c = c;
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
			double loss = this.lossFunction(thetaEstimateStart, thetaEstimateEnd);
			//store
			this.lossFunctionValues.add(loss);
			
			//calculate gain coefficients
			double ak = a / Math.pow(A + k, alpha);
			double ck = c / Math.pow(k, gamma);
			System.out.printf("ak = %.5f, ck = %.5f %n", ak, ck);
						
			//generate deltas	
			this.generateDeltas();
			System.out.println("Start deltas: " + this.deltasStart.values());
			System.out.println("End deltas: " + this.deltasEnd.values());

			//calculate shifted thetas
			HashMap<Integer, Double> thetaStartPlus = shiftTheta(this.thetaEstimateStart, ck, this.deltasStart, true);
			HashMap<Integer, Double> thetaEndPlus = shiftTheta(this.thetaEstimateEnd, ck, this.deltasEnd, false);
			HashMap<Integer, Double> thetaStartMinus = shiftTheta(this.thetaEstimateStart, -1 * ck, this.deltasStart, true);
			HashMap<Integer, Double> thetaEndMinus = shiftTheta(this.thetaEstimateEnd, -1 * ck, this.deltasEnd, false);

			System.out.println("Theta start plus: " + thetaStartPlus);
			System.out.println("Theta start minus: " + thetaStartMinus);
			System.out.println("Theta end plus: " + thetaEndPlus);
			System.out.println("Theta end minus: " + thetaEndMinus);
						
			//evaluate loss function
			double yPlus = lossFunction(thetaStartPlus, thetaEndPlus);
			double yMinus = lossFunction(thetaStartMinus, thetaEndMinus);
			System.out.printf("yPlus = %.5f, yMinus = %.5f %n", yPlus, yMinus);
						
			//approximate gradient
			approximateGradient(this.gradientApproximationStart, yPlus, yMinus, ck, this.deltasStart);
			approximateGradient(this.gradientApproximationEnd, yPlus, yMinus, ck, this.deltasEnd);
			//approximate gradient using the actual change in x (i.e. probabilities) after normalisation
			//approximateGradient(this.gradientApproximationStart, yPlus, yMinus, thetaStartPlus, thetaStartMinus);
			//approximateGradient(this.gradientApproximationEnd, yPlus, yMinus, thetaEndPlus, thetaEndMinus);
			System.out.println("Gradient approximation start: " + this.gradientApproximationStart);
			System.out.println("Gradient approximation end: " + this.gradientApproximationEnd);
			
			//estimate new theta
			updateThetaEstimate(this.thetaEstimateStart, ak, this.gradientApproximationStart, true);
			updateThetaEstimate(this.thetaEstimateEnd, ak, this.gradientApproximationEnd, false);
			System.out.println("New theta estimate start: " + this.thetaEstimateStart);
			System.out.println("New theta estimate end: " + this.thetaEstimateEnd);
			
			k++;

		} while (k <= maxIterations);
		
		//evaluate loss function for the final theta
		double loss = this.lossFunction(this.thetaEstimateStart, this.thetaEstimateEnd);
		//store
		this.lossFunctionValues.add(loss);
		
		System.out.println("SPSA stopped. Maximum number of iterations reached");
	}
	
	/**
	 * @return Loss function evaluations for all iterations.
	 */
	public List<Double> getLossFunctionEvaluations() {
		
		return this.lossFunctionValues;
		
	}
	
	/**
	 * Generates deltas using the Rademacher distribution (i.e. random -1 or 1).
	 */
	private void generateDeltas() {
		
		RandomSingleton rng = RandomSingleton.getInstance();
		
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
			newProbability = Math.min(newProbability, THETA_MAX);
			newProbability = Math.max(newProbability, THETA_MIN);

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
	 * Calculate the loss function for a given theta (node probabilities).
	 * @param thetaStart Start node probabilities.
	 * @param thetaEnd End node probabilities.
	 * @return RMSN for the difference between volumes and traffic counts.
	 */
	private double lossFunction(HashMap<Integer, Double> thetaStart, HashMap<Integer, Double> thetaEnd) {
		
		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripStorages();
		
		rna.setStartNodeProbabilities(thetaStart);
		rna.setEndNodeProbabilities(thetaEnd);
		
		//assign passenger flows
		rna.assignPassengerFlowsRouting(this.odm, null, props); //routing version
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
		
		return lossFunction(this.thetaEstimateStart, this.thetaEstimateEnd);
	}
	
	/**
	 * Approximate the gradient.
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
	 * Approximate the gradient.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradient(HashMap<Integer, Double> gradientApproximation, double yPlus, double yMinus, HashMap<Integer, Double> thetaPlus,  HashMap<Integer, Double> thetaMinus) {
		
		for (Integer nodeID: this.thetaEstimateStart.keySet()) {
			double xPlus = thetaPlus.get(nodeID);
			double xMinus = thetaMinus.get(nodeID);
			double grad = (yPlus - yMinus) / (xPlus - xMinus);
			gradientApproximation.put(nodeID, grad);
		}
	}
	
	/**
	 * Obtain new theta estimate.
	 * @param theta Old theta (OD matrix).
	 * @param ak Gain.
	 * @param gradient Gradient.
	 */
	private void updateThetaEstimate(HashMap<Integer, Double> theta, double ak, HashMap<Integer, Double> gradient, boolean startNodes) {
		
		for (Integer nodeID: theta.keySet()) {
		
			double prob = theta.get(nodeID);
			double grad = gradient.get(nodeID);
			double newProb = prob - ak * grad;
			
			//apply constraints
			newProb = Math.min(newProb, THETA_MAX);
			newProb = Math.max(newProb, THETA_MIN);
			
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

}
