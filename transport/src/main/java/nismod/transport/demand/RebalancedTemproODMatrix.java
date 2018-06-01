package nismod.transport.demand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.graph.structure.DirectedEdge;

import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.network.road.Trip;
import nismod.transport.network.road.TripTempro;
import nismod.transport.zone.NodeMatrix;
import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix (Tempro based) created by directly scaling flows using traffic counts.
 * @author Milan Lovric
 *
 */
public class RebalancedTemproODMatrix extends RealODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(RebalancedTemproODMatrix.class);
	
	private List<String> origins;
	private List<String> destinations;
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Zoning zoning;
	
	private List<Double> RMSNvalues;

	/**
	 * Constructor for a rebalanced OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @param rna Road network assignment.
	 * @param rsg Route set generator.
	 * @param zoning Zoning system.
	 */
	public RebalancedTemproODMatrix(List<String> origins, List<String> destinations, RoadNetworkAssignment rna, RouteSetGenerator rsg, Zoning zoning) {

		super();
		
		this.rna = rna;
		this.origins = new ArrayList<String>();
		this.destinations = new ArrayList<String>();
		this.rsg = rsg;
		this.zoning = zoning;
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

		for (int i=0; i<number; i++) 
			this.scaleToTrafficCounts();
	}


	/**
	 * Creates a unit OD matrix (all ones).
	 */
	public void createUnitMatrix() {

		for (String origin: this.getOrigins()) 
			for (String destination: this.getDestinations())
				this.setFlow(origin, destination, 1);
	}

	/**
	 * Scales OD matrix to traffic counts.
	 */
	public void scaleToTrafficCounts() {
			
		this.rna.resetLinkVolumes();
		this.rna.resetTripStorages();
		ODMatrix odm = new ODMatrix(this);
		
		odm.printMatrixFormatted();
		
		this.rna.assignPassengerFlowsTempro(odm, zoning, rsg);
		this.rna.updateLinkVolumePerVehicleType();
		
		double RMSN = this.rna.calculateRMSNforSimulatedVolumes();
		this.RMSNvalues.add(RMSN);
		System.out.printf("RMSN before scaling = %.2f%% %n", RMSN);
		
		RealODMatrix sf = this.getScalingFactors();
		sf.printMatrixFormatted("Scaling factors:", 5);
		
		this.scaleMatrixValue(sf);
		this.printMatrixFormatted("OD matrix after scaling:", 2);
		
		//this.modifyNodeMatrixProbabilitiesForInterzonalTrips();
	}
	
	/**
	 * Scales OD matrix to traffic counts.
	 */
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
	
	
	
	/**
	 * Calculates scaling factors for OD pairs.
	 * @return Scaling factors.
	 */
	public RealODMatrix getScalingFactors() {
		
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
		
		RealODMatrix factors = new RealODMatrix();
		ODMatrix counter = new ODMatrix();
		RealODMatrix scalingFactors = new RealODMatrix();
		
		for (Trip t: tripList) 
			if (t instanceof TripTempro && t.getVehicle().equals(VehicleType.CAR))	{
				
				TripTempro temproTrip = (TripTempro) t;
				int multiplier = temproTrip.getMultiplier();
			
				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				Route route = t.getRoute();
				
				//get current factor and count
				double factor = factors.getFlow(originZone, destinationZone); 
				int count = counter.getFlow(originZone, destinationZone);
				
				for (DirectedEdge edge: route.getEdges())
					if (linkFactors.get(edge.getID()) != null){
					factor += linkFactors.get(edge.getID()) * multiplier;
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
	
	/**
	 * Gets the list of origins.
	 * @return List of origins.
	 */
	@Override
	public List<String> getOrigins() {
		
		return this.origins;
	}
	
	/**
	 * Gets the list of destinations.
	 * @return List of destinations.
	 */
	@Override
	public List<String> getDestinations() {
		
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
