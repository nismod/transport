package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.graph.structure.DirectedEdge;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.Trip;
import nismod.transport.network.road.TripTempro;
import nismod.transport.zone.Zoning;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route;
import nismod.transport.network.road.RouteSetGenerator;

/**
 * Origin-destination matrix created by directly scaling flows using traffic counts.
 * @author Milan Lovric
 *
 */
public class RebalancedODMatrix extends RealODMatrix {
	
	private final static Logger LOGGER = Logger.getLogger(RebalancedODMatrix.class.getName());
	
	private List<String> origins;
	private List<String> destinations;
	private RoadNetworkAssignment rna;
	private RouteSetGenerator rsg;
	private Zoning zoning;

	/**
	 * Constructor for rebalanced OD matrix that uses network assignment and traffic counts for matrix rebalancing.
	 * @param rna
	 */
	public RebalancedODMatrix(List<String> origins, List<String> destinations, RoadNetworkAssignment rna, RouteSetGenerator rsg, Zoning zoning) {

		super();
		
		this.rna = rna;
		this.origins = new ArrayList<String>();
		this.destinations = new ArrayList<String>();
		this.rsg = rsg;
		this.zoning = zoning;
		
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
		System.out.printf("RMSN before scaling = %.2f%% %n", RMSN);
		
		RealODMatrix sf = this.getScalingFactors();
		sf.printMatrixFormatted("Scaling factors:");
		
		this.scaleMatrixValue(sf);
		this.printMatrixFormatted("OD matrix after scaling:");
	}
	
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
			
				String originZone = temproTrip.getOriginTemproZone();
				String destinationZone = temproTrip.getDestinationTemproZone();
				Route route = t.getRoute();
				
				//get current factor and count
				double factor = factors.getFlow(originZone, destinationZone); 
				int count = counter.getFlow(originZone, destinationZone);
				
				for (DirectedEdge edge: route.getEdges())
					if (linkFactors.get(edge.getID()) != null){
					factor += linkFactors.get(edge.getID());
					count++;
					}
				
				//update factor sum and count
				factors.setFlow(originZone, destinationZone, factor);
				counter.setFlow(originZone, destinationZone, count);
			}
		
		//calculate scaling factors by dividing factor sum and counter
		for (Object mk: factors.getKeySet()) {
			String originZone = (String) ((MultiKey)mk).getKey(0);
			String destinationZone = (String) ((MultiKey)mk).getKey(1);
	
			double scalingFactor = 1.0 * factors.getFlow(originZone, destinationZone) / counter.getFlow(originZone, destinationZone);
			scalingFactors.setFlow(originZone, destinationZone, scalingFactor);
		}
		
		return scalingFactors;
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
	
//	/**
//	 * Prints the matrix as a formatted table.
//	 */
//	@Override
//	public void printMatrixFormatted() {
//
//		List<String> firstKeyList = this.getOrigins();
//		List<String> secondKeyList = this.getDestinations();
//		//System.out.println(firstKeyList);
//		//System.out.println(secondKeyList);
//
//		//formatted print
//		System.out.print("origin    "); for (String s: secondKeyList) System.out.printf("%10s",s);	System.out.println("  Product.");
//		for (String o: firstKeyList) {
//			System.out.printf("%-10s", o);
//			for (String s: secondKeyList) System.out.printf("%10.2f", this.getFlow(o,s));
//			System.out.printf("%10d\n", this.productions.get(o));
//		}
//		System.out.print("Attract.  "); for (String s: secondKeyList) System.out.printf("%10d", this.attractions.get(s));
//		System.out.println();
//	}
//	
//	@Override
//	public void printMatrixFormatted(String message) {
//		
//		System.out.println(message);
//		this.printMatrixFormatted();
//	}
	

}
