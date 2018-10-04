package nismod.transport.demand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import nismod.transport.network.road.TripTempro;
import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix (Tempro based) created by directly scaling flows using traffic counts.
 * @author Milan Lovric
 *
 */
public class RebalancedTemproODMatrix extends RealODMatrixTempro {
	
	private final static Logger LOGGER = LogManager.getLogger(RebalancedTemproODMatrix.class);
	
	private List<String> origins;
	private List<String> destinations;
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Zoning zoning;
	private Properties params;
	private List<Double> RMSNvalues;

	/**
	 * Constructor for a rebalanced OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param zoning Zoning system.
	 */
	public RebalancedTemproODMatrix(List<String> origins, List<String> destinations, RoadNetworkAssignment rna, RouteSetGenerator rsg, Zoning zoning, Properties params) {

		super(zoning);
		
		this.rna = rna;
		this.origins = new ArrayList<String>();
		this.destinations = new ArrayList<String>();
		this.rsg = rsg;
		this.zoning = zoning;
		this.params = params;
		this.RMSNvalues = new ArrayList<Double>();
				
		for (String zone: origins) this.origins.add(zone);
		for (String zone: destinations) this.destinations.add(zone);
		this.createUnitMatrix();
	}

	/**
	 * Constructor for a rebalanced OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param fileName Path to the file with the initial OD matrix. 
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param zoning Zoning system.
	 * @throws IOException if any.
	 * @throws FileNotFoundException if any. 
	 */
	public RebalancedTemproODMatrix(String fileName, RoadNetworkAssignment rna, RouteSetGenerator rsg, Zoning zoning, Properties params) throws FileNotFoundException, IOException {

		super(fileName, zoning);
		
		this.rna = rna;
		this.origins = super.getSortedOrigins(); //expensive operation, so copy to local field
		this.destinations = super.getSortedDestinations(); //expensive operation, so copy to local field
		this.rsg = rsg;
		this.zoning = zoning;
		this.params = params;
		this.RMSNvalues = new ArrayList<Double>();
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
			this.saveMatrixFormatted2(file + "/temproODMafterIteration" + i + ".csv");
			this.rna.saveLinkTravelTimes(2015, file + "/linkTravelTimesAfterIteration" + i + ".csv");
		}
		
		//assign ones more to get the latest RMSN
		this.assignAndCalculateRMSN();
	}


	/**
	 * Creates a unit OD matrix (all ones).
	 */
	public void createUnitMatrix() {

		for (String origin: this.getSortedOrigins()) 
			for (String destination: this.getSortedDestinations())
				this.setFlow(origin, destination, 1.0);
	}
	
	/**
	 * Assigns OD matrix and calculates RMSN with traffic counts.
	 */
	public void assignAndCalculateRMSN() {
		
		this.rna.resetLinkVolumes();
		this.rna.resetTripList();
		//ODMatrix odm = new ODMatrix(this);
		//	odm.printMatrixFormatted();
		
		/*
		final Boolean flagUseRouteChoiceModel = Boolean.parseBoolean(params.getProperty("USE_ROUTE_CHOICE_MODEL"));
		
		if (flagUseRouteChoiceModel) {
			this.rna.assignPassengerFlowsRouteChoiceTempro(this, zoning, rsg, params);
		} else {
			this.rna.assignPassengerFlowsTempro(this, zoning, rsg);
		}
		*/
		
		//use combined Tempro/LAD model
		this.rna.assignPassengerFlowsRouteChoiceTemproDistanceBased(this, zoning, rsg, params);
		
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
			
		RealODMatrixTempro sf = this.getScalingFactors();
		//sf.printMatrixFormatted("Scaling factors:", 5);
		
		this.scaleMatrixValue(sf);
		//this.printMatrixFormatted("OD matrix after scaling:", 2);
		//this.modifyNodeMatrixProbabilitiesForInterzonalTrips();
	}
	
	/**
	 * Scales OD matrix to traffic counts.
	 */
	/*
	public void scaleNodeMatricesToTrafficCounts() {
			
		this.rna.resetLinkVolumes();
		this.rna.resetTripStorages();
		ODMatrix odm = new ODMatrix(this);
		
		odm.printMatrixFormatted();
		
		this.rna.assignPassengerFlowsTempro(odm, zoning, rsg);
		this.rna.updateLinkVolumePerVehicleType();
		
		double RMSN = this.rna.calculateRMSNforSimulatedVolumes();
		this.RMSNvalues.add(RMSN);
		System.out.printf("RMSN before scaling = %.2f%% %n", RMSN);
		
		this.modifyNodeMatrixProbabilitiesForInterzonalTrips();
	}
	*/
		
	/**
	 * Calculates scaling factors for OD pairs.
	 * @return Scaling factors.
	 */
	public RealODMatrixTempro getScalingFactors() {
		
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
		
		RealODMatrixTempro factors = new RealODMatrixTempro(zoning);
		RealODMatrixTempro counter = new RealODMatrixTempro(zoning);
		RealODMatrixTempro scalingFactors = new RealODMatrixTempro(zoning);
		
		for (Trip t: tripList) 
			if (t instanceof TripTempro && t.getVehicle().equals(VehicleType.CAR))	{
				
				TripTempro temproTrip = (TripTempro) t;
				int multiplier = temproTrip.getMultiplier();
			
				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				Route route = t.getRoute();
				
				//get current factor and count
				double factor = factors.getFlow(originZone, destinationZone); 
				double count = counter.getFlow(originZone, destinationZone);
				
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
		for (String originZone: origins)
			for (String destinationZone: destinations) {

				double scalingFactor;
				if (Double.compare(counter.getIntFlow(originZone, destinationZone), 0.0d) == 0) 
					scalingFactor = 1.0; //there were no (non-empty) routes between these two zones
				else 
					scalingFactor = 1.0 * factors.getFlow(originZone, destinationZone) / counter.getFlow(originZone, destinationZone);
		
			scalingFactors.setFlow(originZone, destinationZone, scalingFactor);
		}
		
		return scalingFactors;
	}
	
	/*
	public void modifyNodeMatrixProbabilitiesForInterzonalTrips() {
		
		List<Trip> tripList = this.rna.getTripList();
		System.out.println("Trip list size: " + tripList.size());
		
		Map<Integer, Integer> volumes = this.rna.getLinkVolumePerVehicleType().get(VehicleType.CAR);
		Map<Integer, Integer> counts = this.rna.getAADFCarTrafficCounts();
		Map<Integer, Double> linkFactors = new HashMap<Integer, Double>();
		
		System.out.println("volumes = " + volumes);
		System.out.println("counts = " + counts);
				
		//scaling link factors can only be calculated for links that have counts and flow > 0
		for (Integer edgeID: volumes.keySet()) {
			Integer count = counts.get(edgeID);
			Integer volume = volumes.get(edgeID);
			if (count == null) continue; //skip link with no count (e.g. ferry lines)
			if (volume == 0.0) continue; //also skip as it would create infinite factor
			linkFactors.put(edgeID, 1.0 * count / volume);
		}
		System.out.println("link factors = " + linkFactors);
		
		
		HashMap<String, NodeMatrix> zoneToSumFactors = new HashMap<String, NodeMatrix>();
		HashMap<String, NodeMatrix> zoneToCounters = new HashMap<String, NodeMatrix>();
		HashMap<String, NodeMatrix> zoneToScalingFactors = new HashMap<String, NodeMatrix>();		
		
		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
			zoneToSumFactors.put(zone, new NodeMatrix());
			zoneToCounters.put(zone, new NodeMatrix());
			zoneToScalingFactors.put(zone, new NodeMatrix());
		}
		
		for (Trip t: tripList) 
			if (t instanceof TripTempro && t.getVehicle().equals(VehicleType.CAR))	{
				
				TripTempro temproTrip = (TripTempro) t;
			
				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				Route route = t.getRoute();
				int multiplier = temproTrip.getMultiplier();
				
				//only if inter-zonal
				if (!originZone.equals(destinationZone)) continue; //skip if not inter-zonal
				
				if (zoning.getZoneToNodeMatrix().get(originZone) == null) continue; //skip if no NodeMatrix (too few nodes).
				
				Integer originNode = temproTrip.getOriginNode().getID();
				Integer destinationNode = temproTrip.getDestinationNode().getID();
								
				//get current sum factor and count NodeMatrix
				NodeMatrix sum = zoneToSumFactors.get(originZone); 
				NodeMatrix count = zoneToCounters.get(originZone);
				
				double s = sum.getValue(originNode, destinationNode);
				double c = count.getValue(originNode, destinationNode);
				
				for (DirectedEdge edge: route.getEdges())
					if (linkFactors.get(edge.getID()) != null){
					s += linkFactors.get(edge.getID() * multiplier);
					c += multiplier;
					}
				
				//update factor sum and count
				sum.setValue(originNode, destinationNode, s);
				count.setValue(originNode, destinationNode, c);
			}
		
		//calculate scaling factors by dividing factor sum and counter
		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
			NodeMatrix sum = zoneToSumFactors.get(zone); 
			NodeMatrix count = zoneToCounters.get(zone);
			NodeMatrix scaling = zoneToScalingFactors.get(zone);
			
			for (Object mk: sum.getKeySet()) {
				Integer originNode = (Integer) ((MultiKey)mk).getKey(0);
				Integer destinationNode = (Integer) ((MultiKey)mk).getKey(1);
				double scalingFactor;
				if (count.getValue(originNode, destinationNode) == 0) 
					scalingFactor = 1.0; //there were no (non-empty) routes between these two zones
				else 
					scalingFactor = 1.0 * sum.getValue(originNode, destinationNode) / count.getValue(originNode, destinationNode);
				scaling.setValue(originNode, destinationNode, scalingFactor);
			}		
		}

		for (String zone: zoning.getZoneToNodeMatrix().keySet()) {
			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted(zone);
			zoneToScalingFactors.get(zone).printMatrixFormatted("scaling factors:");
			zoning.getZoneToNodeMatrix().get(zone).scaleMatrixValue(zoneToScalingFactors.get(zone));
			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted("after scaling");
			zoning.getZoneToNodeMatrix().get(zone).normaliseWithZeroDiagonal();
			zoning.getZoneToNodeMatrix().get(zone).printMatrixFormatted("after normalising:");
		}
	}
	*/
	
	/**
	 * Gets the list of origins.
	 * @return List of origins.
	 */
	@Override
	public List<String> getSortedOrigins() {
		
		return this.origins;
	}
	
	/**
	 * Gets the list of destinations.
	 * @return List of destinations.
	 */
	@Override
	public List<String> getSortedDestinations() {
		
		return this.destinations;
	}
	
	/**
	 * Gets the list of RMSN values over all performed rebalancing iterations.
	 * @return List of RMSN values.
	 */
	public List<Double> getRMSNvalues() {
		
		return this.RMSNvalues;
	}
}
