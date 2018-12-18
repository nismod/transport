package nismod.transport.demand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.Trip;

/**
 * Freight matrix created by directly scaling flows using traffic counts.
 * Base on DfT's BYFM 2006 zoning system (LAD + distribution centres + seaports + airports).
 * @author Milan Lovric
 *
 */
public class RebalancedFreightMatrix extends FreightMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(RebalancedFreightMatrix.class);
	
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Properties params;
	private Map<VehicleType, List<Double>> RMSNvalues; //per vehicle type and iteration
	private Map<VehicleType, Integer[]> counts; //freight traffic counts
 
	/**
	 * Constructor for a rebalanced freight matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param params Properties.
	 */
	public RebalancedFreightMatrix(RoadNetworkAssignment rna, RouteSetGenerator rsg, Properties params) {

		super();
		
		this.rna = rna;
		this.rsg = rsg;
		this.params = params;
		this.RMSNvalues = new EnumMap<VehicleType, List<Double>>(VehicleType.class);
		this.RMSNvalues.put(VehicleType.VAN, new ArrayList<Double>());
		this.RMSNvalues.put(VehicleType.RIGID, new ArrayList<Double>());
		this.RMSNvalues.put(VehicleType.ARTIC, new ArrayList<Double>());
		
		//calculate and store traffic count data
		this.counts = this.rna.getAADFFreightTrafficCounts();
			
		this.createUnitMatrix();
				
//		List<Integer> origins = Arrays.asList(854, 855, 866, 867, 1312, 1313);
//		List<Integer> destinations = Arrays.asList(854, 855, 866, 867, 1312, 1313);
//		
//		for (int origin: origins)
//			for (int destination: destinations)
//				for (int vehicleType = 1; vehicleType <= FreightMatrix.MAX_VEHICLE_ID; vehicleType++)
//					this.setFlow(origin, destination, vehicleType, 1);
	}
	
	/**
	 * Constructor for a rebalanced freight matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param fileName Path to the file with the initial OD matrix. 
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param params Properties.
	 * @throws IOException if any.
	 * @throws FileNotFoundException if any.
	 */
	public RebalancedFreightMatrix(String fileName, RoadNetworkAssignment rna, RouteSetGenerator rsg, Properties params) throws FileNotFoundException, IOException {

		super(fileName);
		
		this.rna = rna;
		this.rsg = rsg;
		this.params = params;
		this.RMSNvalues = new EnumMap<VehicleType, List<Double>>(VehicleType.class);
		this.RMSNvalues.put(VehicleType.VAN, new ArrayList<Double>());
		this.RMSNvalues.put(VehicleType.RIGID, new ArrayList<Double>());
		this.RMSNvalues.put(VehicleType.ARTIC, new ArrayList<Double>());

		//calculate and store traffic count data
		this.counts = this.rna.getAADFFreightTrafficCounts();
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
			this.saveMatrixFormatted(file + "/freightMatrixAfterIteration" + i + ".csv");
			this.rna.saveLinkTravelTimes(2015, file + "/linkTravelTimesWithFreightAfterIteration" + i + ".csv");
		}
		
		//assign ones more to get the latest RMSN
		this.assignAndCalculateRMSN();
	}


	/**
	 * Creates a unit OD matrix (all ones).
	 */
	public void createUnitMatrix() {
		
		LOGGER.debug("Creating the unit freight matrix...");
		for (int origin = 1; origin <= FreightMatrix.MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= FreightMatrix.MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= FreightMatrix.MAX_VEHICLE_ID; vehicleType++)
					
					//use only allowed zone IDs from DfT's BYFM
					if (((origin >= 1 && origin <= 867) || (origin >= 901 && origin <= 922) || (origin >= 1001 && origin <= 1032) ||	(origin >= 1111 && origin <= 1115) || (origin >= 1201 && origin <= 1256) ||	(origin >= 1301 && origin <= 1388))
						&& ((destination >= 1 && destination <= 867) || (destination >= 901 && destination <= 922) || (destination >= 1001 && destination <= 1032) || (destination >= 1111 && destination <= 1115) || (destination >= 1201 && destination <= 1256) || (destination >= 1301 && destination <= 1388)))
						
						this.setFlow(origin, destination, vehicleType, 1);
	}
	
	/**
	 * Assigns OD matrix and calculates RMSN with traffic counts.
	 */
	public void assignAndCalculateRMSN() {
		
		this.rna.resetLinkVolumes();
		this.rna.resetTripList();
		//FreightMatrix odm = new FreightMatrix(this);
		//	odm.printMatrixFormatted();
		
		final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL"));
		
		if (flagUseRouteChoiceModel) {
			rna.assignFreightFlowsRouteChoice(this, rsg, params);
		} else {
			rna.assignFreightFlowsRouting(this, rsg, params);
		}
		rna.updateLinkVolumeInPCU();
		rna.updateLinkVolumeInPCUPerTimeOfDay();
		rna.updateLinkVolumePerVehicleType();
		rna.updateLinkTravelTimes(0.9);
		
		Map<VehicleType, Double> RMSN = this.rna.calculateRMSNforFreightCounts();
		this.RMSNvalues.get(VehicleType.VAN).add(RMSN.get(VehicleType.VAN));
		this.RMSNvalues.get(VehicleType.RIGID).add(RMSN.get(VehicleType.RIGID));
		this.RMSNvalues.get(VehicleType.ARTIC).add(RMSN.get(VehicleType.ARTIC));
		
		this.rna.printRMSNstatisticFreight();
		//LOGGER.info("RMSN before scaling = {}%", RMSN);
		this.rna.printGEHstatisticFreight();
	}
	
	/**
	 * Scales OD matrix to traffic counts.
	 */
	public void scaleToTrafficCounts() {
			
		SkimMatrixFreightArray sf = this.getScalingFactors();
		//sf.printMatrixFormatted("Scaling factors:");
		LOGGER.debug("Total matrix flow before scaling: {}",  this.getTotalIntFlow());
		
		this.scaleMatrix(sf);
		//this.printMatrixFormatted("Freight matrix after scaling:");
		LOGGER.debug("Total matrix flow after scaling: {}",  this.getTotalIntFlow());

	}
	
	
	/**
	 * Calculates scaling factors for OD pairs.
	 * @return Scaling factors.
	 */
	public SkimMatrixFreightArray getScalingFactors() {
		
		List<Trip> tripList = this.rna.getTripList();
		LOGGER.debug("Trip list size: {}", tripList.size());
		
		int[] vanVolumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.VAN);
		int[] rigidVolumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.RIGID);
		int[] articVolumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.ARTIC);
		
		Integer[] vanCounts = this.counts.get(VehicleType.VAN);
		Integer[] rigidCounts = this.counts.get(VehicleType.RIGID);
		Integer[] articCounts = this.counts.get(VehicleType.ARTIC);
		
		Double[] vanLinkFactors = new Double[vanVolumes.length];
		Double[] rigidLinkFactors = new Double[rigidVolumes.length];
		Double[] articLinkFactors = new Double[articVolumes.length];
		
		LOGGER.trace("van volumes = {}", vanVolumes);
		LOGGER.trace("van counts = {}", Arrays.toString(vanCounts));
		LOGGER.trace("rigid volumes = {}", rigidVolumes);
		LOGGER.trace("rigid counts = {}", Arrays.toString(rigidCounts));
		LOGGER.trace("artic volumes = {}", articVolumes);
		LOGGER.trace("artic counts = {}", Arrays.toString(articCounts));
				
		//scaling link factors can only be calculated for links that have counts and flow > 0
		for (int edgeID = 1; edgeID < vanVolumes.length; edgeID++) {
			Integer vanCount = vanCounts[edgeID];
			Integer rigidCount = rigidCounts[edgeID];
			Integer articCount = articCounts[edgeID];
			
			Integer vanVolume = vanVolumes[edgeID];
			Integer rigidVolume = rigidVolumes[edgeID];
			Integer articVolume = articVolumes[edgeID];
			//skip link with no count (e.g. ferry lines)
			//also skip as it would create infinite factor
			if (vanCount != null && vanVolume > 0)
				vanLinkFactors[edgeID] = 1.0 * vanCount / vanVolume;
			if (rigidCount != null && rigidVolume > 0)
				rigidLinkFactors[edgeID] = 1.0 * rigidCount / rigidVolume;
			if (articCount != null && articVolume > 0)
				articLinkFactors[edgeID] = 1.0 * articCount / articVolume;
		}
		LOGGER.trace("van link factors = {}", Arrays.toString(vanLinkFactors));
		LOGGER.trace("rigid link factors = {}", Arrays.toString(rigidLinkFactors));
		LOGGER.trace("artic link factors = {}", Arrays.toString(articLinkFactors));
		
		SkimMatrixFreightArray factors = new SkimMatrixFreightArray();
		SkimMatrixFreightArray counter = new SkimMatrixFreightArray();
		SkimMatrixFreightArray scalingFactors = new SkimMatrixFreightArray();
		
		for (Trip t: tripList) {

			int multiplier = t.getMultiplier(); 
			int origin = t.getOrigin();
			int destination = t.getDestination();
			int vehicleType = t.getVehicle().getValue();
			Route route = t.getRoute();
				
			//get current factor and count
			double factor = factors.getCost(origin, destination, vehicleType); 
			double count = counter.getCost(origin, destination, vehicleType);
			
			if (t.getVehicle() == VehicleType.VAN) {
				for (int edgeID: route.getEdges().toArray())
					if (vanLinkFactors[edgeID] != null) {
						factor += vanLinkFactors[edgeID] * multiplier;
						count += multiplier;
					}
			} else if (t.getVehicle() == VehicleType.RIGID) {
				for (int edgeID: route.getEdges().toArray())
					if (rigidLinkFactors[edgeID] != null) {
						factor += rigidLinkFactors[edgeID] * multiplier;
						count += multiplier;
					}
			} else if (t.getVehicle() == VehicleType.ARTIC) {
				for (int edgeID: route.getEdges().toArray())
					if (articLinkFactors[edgeID] != null) {
						factor += articLinkFactors[edgeID] * multiplier;
						count += multiplier;
					}
			}
			//update factor sum and count
			factors.setCost(origin, destination, vehicleType, factor);
			counter.setCost(origin, destination, vehicleType, count);
		}
		
		//calculate scaling factors by dividing factor sum and counter
		for (int origin = 1; origin <= MAX_FREIGHT_ZONE_ID; origin++)
			for (int destination = 1; destination <= MAX_FREIGHT_ZONE_ID; destination++)
				for (int vehicleType = 1; vehicleType <= MAX_VEHICLE_ID; vehicleType++) {
	
					double scalingFactor;
					if (counter.getCost(origin, destination, vehicleType) == 0) 
						scalingFactor = 1.0; //there were no (non-empty) routes between these two zones
					else 
						scalingFactor = 1.0 * factors.getCost(origin, destination, vehicleType) / counter.getCost(origin, destination, vehicleType);
					
					scalingFactors.setCost(origin, destination, vehicleType, scalingFactor);
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
	public Map<VehicleType, List<Double>> getRMSNvalues() {
		
		return this.RMSNvalues;
	}
}
