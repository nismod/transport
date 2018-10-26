package nismod.transport.optimisation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.demand.RealODMatrixTempro;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.zone.Zoning;

/**
 * Implements SPSA optimisation algorithm (Simultaneous Perturbation Stochastic Approximation).
 * This version optimises Tempro level OD matrix.
 * http://www.jhuapl.edu/SPSA/
 * @author Milan Lovric
  */
public class SPSA5 {
	
	private final static Logger LOGGER = LogManager.getLogger(SPSA5.class);
	
	//maximum and minimum values of OD matrix flows (i.e. constraints)
	public static final double THETA_MAX = 30000.0; //10000000.0;
	public static final double THETA_MIN = 0.0;
	
	//SPSA parameters
	private double a;
	private double A; 
	private double c; 
	private double alpha;
	private double gamma;
	
	private RealODMatrixTempro thetaEstimate; //contains the final result
	private RealODMatrixTempro deltas;
	private RealODMatrixTempro gradientApproximation;
	
	private List<Double> lossFunctionValues;
	
	private RoadNetworkAssignment rna;
	private Zoning zoning;
	private RouteSetGenerator rsg;
	private Properties props;

	public SPSA5(Properties props) {
		this.props = props;
	}
	
	/**
	 * Initialise the SPSA algorithm with starting values.
	 * @param rna Road network assignment.
	 * @param zoning Zoning system for tempro zones.
	 * @param rsg Route set generator with routes to be used in assignment.
	 * @param initialTheta Initial OD matrix.
	 * @param a SPSA parameter.
	 * @param A SPSA parameter.
	 * @param c SPSA parameter.
	 * @param alpha SPSA parameter.
	 * @param gamma SPSA parameter.
	 */
	public void initialise(RoadNetworkAssignment rna, Zoning zoning, RouteSetGenerator rsg, RealODMatrixTempro initialTheta, double a, double A, double c, double alpha, double gamma) {
			
		this.rna = rna;
		this.zoning = zoning;
		this.rsg = rsg;
		this.thetaEstimate = initialTheta.clone();
		this.deltas = new RealODMatrixTempro(zoning);
		this.gradientApproximation = new RealODMatrixTempro(zoning);
		
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
		
		//create output directory
		final String outputFolder = props.getProperty("outputFolder");
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                LOGGER.debug("Output directory is created.");
	            } else {
	            	LOGGER.error("Failed to create output directory.");
	            }
	        }
		
		int k = 1; //counter

		do {
			//evaluate loss function for the current theta
			double loss = this.lossFunction(thetaEstimate);

			//store
			this.lossFunctionValues.add(loss);

			this.rna.printRMSNstatistic();
			this.rna.printGEHstatistic();
			LOGGER.debug("Iteration {} loss function = {}", k, loss);
				
			//calculate gain coefficients
			double ak = a / Math.pow(A + k, alpha);
			double ck = c / Math.pow(k, gamma);
			System.out.printf("ak = %.5f, ck = %.5f %n", ak, ck);
						
			//generate deltas	
			this.generateDeltas(thetaEstimate.getUnsortedOrigins(), thetaEstimate.getUnsortedDestinations());
			//deltas.printMatrixFormatted("Deltas: ");

			//calculate shifted thetas
			RealODMatrixTempro thetaPlus = this.shiftTheta(thetaEstimate, ck, deltas);
			RealODMatrixTempro thetaMinus = this.shiftTheta(thetaEstimate, -1 * ck, deltas);
			//thetaPlus.printMatrixFormatted("Theta plus: ");
			//thetaMinus.printMatrixFormatted("Theta minus: ");
			
			//evaluate loss function
			double yPlus = this.lossFunction(thetaPlus);
			double yMinus = this.lossFunction(thetaMinus);
			//System.out.printf("yPlus = %.5f, yMinus = %.5f %n", yPlus, yMinus);
						
			//approximate gradient
	//		this.approximateGradient(yPlus, yMinus, ck, deltas);
			this.approximateGradientActualChangeInX(yPlus, yMinus, thetaPlus, thetaMinus);
			//gradientApproximation.printMatrixFormatted("Gradient approximation: ");
			
			//estimate new theta
			this.updateThetaEstimate(thetaEstimate, ak, gradientApproximation);
			//thetaEstimate.printMatrixFormatted("New theta estimate: ");
			
			this.thetaEstimate.saveMatrixFormatted2(file + "/temproODMafterSPSAIteration" + k + ".csv");
			this.rna.saveLinkTravelTimes(2015, file + "/linkTravelTimesAfterSPSAIteration" + k + ".csv");
			
			k++;

		} while (k <= maxIterations);
		
		//evaluate loss function for the final theta
		double loss = this.lossFunction(thetaEstimate);
		//store
		this.lossFunctionValues.add(loss);
		
		this.rna.printRMSNstatistic();
		this.rna.printGEHstatistic();
		LOGGER.debug("Iteration {} loss function = {}", k, loss);
		
		LOGGER.info("SPSA stopped. Maximum number of iterations reached.");
	}
	
	/**
	 * @return Loss function evaluations for all iterations.
	 */
	public List<Double> getLossFunctionEvaluations() {
		
		return this.lossFunctionValues;
		
	}
	
	/**
	 * Getter function for the optimisation result (OD matrix).
	 * @return Estimated OD matrix.
	 */
	public RealODMatrixTempro getThetaEstimate() {
		
		return this.thetaEstimate;
	}
	
	/**
	 * Generates deltas using the Rademacher distribution (i.e. random -1 or 1).
	 * @param origins The list of origins for which deltas need to be generated.
	 * @param destinations The list of destinations for which deltas need to be generated.
	 */
	private void generateDeltas(List<String> origins, List<String> destinations) {
		
		RandomSingleton rng = RandomSingleton.getInstance();
		
		for (String origin: origins)
			for (String destination: destinations){
				double delta = Math.round(rng.nextDouble()) * 2.0 - 1.0;
				deltas.setFlow(origin, destination, delta);
		}
	}

	/**
	 * Calculate new OD matrix with shifted values.
	 * @param theta Current OD matrix.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private RealODMatrixTempro shiftTheta(RealODMatrixTempro theta, double ck, RealODMatrixTempro deltas) {
		
		RealODMatrixTempro shiftedTheta = new RealODMatrixTempro(zoning);
		
		List<String> origins = theta.getUnsortedOrigins();
		List<String> destinations = theta.getUnsortedDestinations();
		
		for (String origin: origins)
			for (String destination: destinations){
		
			double flow = theta.getFlow(origin, destination);
			double delta = deltas.getFlow(origin, destination);
			double newFlow = flow + ck * delta;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX);
			newFlow = Math.max(newFlow, THETA_MIN);
			
			//if original flow is 0, do not shift
			if (flow > 0) 	shiftedTheta.setFlow(origin, destination, newFlow);
			else 			shiftedTheta.setFlow(origin, destination, 0.0);
		}
		
		return shiftedTheta;
	}
		
	/**
	 * Calculate the loss function for a given theta (OD matrix).
	 * @param theta OD matrix.
	 * @return loss function based on the percentage of edges with invalid/valid GEH statistic.
	 */
	private double lossFunction(RealODMatrixTempro theta) {
		
		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripList();
		
		//assign passenger flows
		rna.assignPassengerFlowsRouteChoiceTemproDistanceBased(theta, zoning, rsg, props); //routing version with tempro zones

		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation
		rna.updateLinkTravelTimes(0.9);
		
		//calculate GEH
		HashMap<Integer, Double> GEH = rna.calculateGEHStatisticForCarCounts(rna.getVolumeToFlowFactor());
		
		int validFlows = 0;
		int suspiciousFlows = 0;
		int invalidFlows = 0;
		for (Integer edgeID: GEH.keySet()) {
			if (GEH.get(edgeID) < 5.0) validFlows++;
			else if (GEH.get(edgeID) < 10.0) suspiciousFlows++;
			else invalidFlows++;
		}

		LOGGER.trace("validFlows = {}", validFlows);
		LOGGER.trace("suspiciousFlows = {}", suspiciousFlows);
		LOGGER.trace("invalidFlows = {}", invalidFlows);
		LOGGER.trace("GEH size = {}", GEH.size());
		LOGGER.trace("loss function = {}", 1.0 * (invalidFlows - validFlows) / GEH.size() * 100);
		
		return 1.0 * (invalidFlows - validFlows) / GEH.size() * 100;
	}
	
	/**
	 * Calculate the loss function of the latest theta estimate (OD matrix).
	 * @return Loss function.
	 */
	public double lossFunction() {
		
		return lossFunction(this.thetaEstimate);
	}
	
	/**
	 * Approximate the gradient.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradient(double yPlus, double yMinus, double ck, RealODMatrixTempro deltas) {
		
		List<String> origins = deltas.getUnsortedOrigins();
		List<String> destinations = deltas.getUnsortedDestinations();
		
		for (String origin: origins)
			for (String destination: destinations){
		
				double delta = deltas.getFlow(origin, destination);
				double grad = (yPlus - yMinus) / (2 * ck * delta);
				
				this.gradientApproximation.setFlow(origin, destination, grad);
			}
	}
	
	/**
	 * Approximate the gradient.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradientActualChangeInX(double yPlus, double yMinus, RealODMatrixTempro thetaPlus, RealODMatrixTempro thetaMinus) {
		
		List<String> origins = thetaPlus.getUnsortedOrigins();
		List<String> destinations = thetaPlus.getUnsortedDestinations();
		
		for (String origin: origins)
			for (String destination: destinations){
		
			double xPlus = thetaPlus.getFlow(origin, destination);
			double xMinus = thetaMinus.getFlow(origin, destination);
			
			double grad = (yPlus - yMinus) / (xPlus - xMinus);
					
			if (Double.isFinite(grad)) 	this.gradientApproximation.setFlow(origin, destination, grad);
			else 				  		this.gradientApproximation.setFlow(origin, destination, 0.0);
		}
	}
	
	/**
	 * Obtain new theta estimate.
	 * @param theta Old theta (OD matrix).
	 * @param ak Gain.
	 * @param gradient Gradient.
	 */
	private void updateThetaEstimate(RealODMatrixTempro theta, double ak, RealODMatrixTempro gradient) {
		
		List<String> origins = theta.getUnsortedOrigins();
		List<String> destinations = theta.getUnsortedDestinations();
		
		for (String origin: origins)
			for (String destination: destinations){
		
			double flow = theta.getFlow(origin, destination);
			double grad = gradient.getFlow(origin, destination);
			double newFlow = flow - ak * grad;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX);
			newFlow = Math.max(newFlow, THETA_MIN);
			
			theta.setFlow(origin, destination, newFlow);
		}
	}

}
