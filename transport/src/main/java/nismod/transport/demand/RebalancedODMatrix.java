package nismod.transport.demand;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.Trip;

/**
 * Origin-destination matrix (LAD based) created by directly scaling flows using traffic counts.
 * @author Milan Lovric
 *
 */
public class RebalancedODMatrix extends RealODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(RebalancedODMatrix.class);
	
	private List<String> origins;
	private List<String> destinations;
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Properties params;
	private List<Double> RMSNvalues;

	/**
	 * Constructor for a rebalanced OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param params Properties.
	 */
	public RebalancedODMatrix(List<String> origins, List<String> destinations, RoadNetworkAssignment rna, RouteSetGenerator rsg, Properties params) {

		super();
		
		this.rna = rna;
		this.origins = new ArrayList<String>();
		this.destinations = new ArrayList<String>();
		this.rsg = rsg;
		this.params = params;
		this.RMSNvalues = new ArrayList<Double>();
		
		
		for (String zone: origins) this.origins.add(zone);
		for (String zone: destinations) this.destinations.add(zone);
		this.createUnitMatrix();
	}

	/**
	 * Iterates scaling to traffic counts.
	 * @param number Number of iterations.
	 */
	public void iterate(int number) {
		
		final String outputFolder = params.getProperty("outputFolder");
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                LOGGER.debug("Output directory is created.");
	            } else {
	            	LOGGER.error("Failed to create output directory.");
	            }
	        }

		for (int i=0; i<number; i++) {
			this.assignAndCalculateRMSN();
			this.scaleToTrafficCounts();
			this.saveMatrixFormatted(file + "/ODMafterIteration" + i + ".csv");
		}
		
		//assign ones more to get the latest RMSN
		this.assignAndCalculateRMSN();
	}


	/**
	 * Creates a unit OD matrix (all ones).
	 */
	public void createUnitMatrix() {

		for (String origin: this.origins) 
			for (String destination: this.destinations)
				this.setFlow(origin, destination, 1.0);
	}

	
	/**
	 * Assigns OD matrix and calculates RMSN with traffic counts.
	 */
	public void assignAndCalculateRMSN() {
		
		this.rna.resetLinkVolumes();
		this.rna.resetTripList();
		ODMatrix odm = new ODMatrix(this);
		//	odm.printMatrixFormatted();
		
		final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL"));
		
		if (flagUseRouteChoiceModel) {
			rna.assignPassengerFlowsRouteChoice(odm, rsg, params);
		} else {
			rna.assignPassengerFlowsRouting(odm, rsg, params);
		}
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		rna.updateLinkVolumePerVehicleType();
		rna.updateLinkTravelTimes(0.9);
		
		double RMSN = this.rna.calculateRMSNforSimulatedVolumes();
		this.RMSNvalues.add(RMSN);
		this.rna.printRMSNstatistic();
		//LOGGER.info("RMSN before scaling = {}%", RMSN);
		this.rna.printGEHstatistic();
	}
	
	/**
	 * Scales OD matrix to traffic counts.
	 */
	public void scaleToTrafficCounts() {
			
		RealODMatrix sf = this.getScalingFactors();
		sf.printMatrixFormatted("Scaling factors:", 5);
		
		this.scaleMatrixValue(sf);
	//	this.printMatrixFormatted("OD matrix after scaling:", 2);
	}
	
	
	/**
	 * Calculates scaling factors for OD pairs.
	 * @return Scaling factors.
	 */
	public RealODMatrix getScalingFactors() {
		
		List<Trip> tripList = this.rna.getTripList();
		LOGGER.trace("Trip list size: {}", tripList.size());
		
		Map<Integer, Integer> volumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.CAR);
		Map<Integer, Integer> counts = this.rna.getAADFCarTrafficCounts();
		Map<Integer, Double> linkFactors = new HashMap<Integer, Double>();
		
		LOGGER.trace("volumes = {}", volumes);
		LOGGER.trace("counts = {}", counts);
				
		//scaling link factors can only be calculated for links that have counts and flow > 0
		for (Integer edgeID: volumes.keySet()) {
			Integer count = counts.get(edgeID);
			Integer volume = volumes.get(edgeID);
			if (count == null) continue; //skip link with no count (e.g. ferry lines)
			if (volume == 0.0) continue; //also skip as it would create infinite factor
			linkFactors.put(edgeID, 1.0 * count / volume);
		}
		LOGGER.trace("link factors = {}", linkFactors);
		
		RealODMatrix factors = new RealODMatrix();
		ODMatrix counter = new ODMatrix();
		RealODMatrix scalingFactors = new RealODMatrix();
		
		for (Trip t: tripList) 
			if (t.getVehicle().equals(VehicleType.CAR))	{
				
				int multiplier = t.getMultiplier();  
			
				String originZone = t.getOriginLAD(rna.getRoadNetwork().getNodeToZone());
				String destinationZone = t.getDestinationLAD(rna.getRoadNetwork().getNodeToZone());
				Route route = t.getRoute();
				
				//get current factor and count
				double factor = factors.getFlow(originZone, destinationZone); 
				int count = counter.getFlow(originZone, destinationZone);
				
				for (int edgeID: route.getEdges().toArray())
					if (linkFactors.get(edgeID) != null){
					factor += linkFactors.get(edgeID) * multiplier;
					count += multiplier;
					}
				
				//update factor sum and count
				factors.setFlow(originZone, destinationZone, factor);
				counter.setFlow(originZone, destinationZone, count);
			}
		
		//calculate scaling factors by dividing factor sum and counter
		for (Object mk: factors.getKeySet()) {
			String originZone = (String) ((MultiKey)mk).getKey(0);
			String destinationZone = (String) ((MultiKey)mk).getKey(1);
	
			double scalingFactor;
			if (counter.getFlow(originZone, destinationZone) == 0) 
				scalingFactor = 1.0; //there were no (non-empty) routes between these two zones
			else 
				scalingFactor = 1.0 * factors.getFlow(originZone, destinationZone) / counter.getFlow(originZone, destinationZone);
		
			scalingFactors.setFlow(originZone, destinationZone, scalingFactor);
		}
		
		return scalingFactors;
	}
	
//	/**
//	 * Gets the list of origins.
//	 * @return List of origins.
//	 */
//	@Override
//	public List<String> getOrigins() {
//		
//		return this.origins;
//	}
//	
//	/**
//	 * Gets the list of destinations.
//	 * @return List of destinations.
//	 */
//	@Override
//	public List<String> getDestinations() {
//		
//		return this.destinations;
//	}
	
	/**
	 * Gets the list of RMSN values over all performed rebalancing iterations.
	 * @return List of RMSN values.
	 */
	public List<Double> getRMSNvalues() {
		
		return this.RMSNvalues;
	}
}
