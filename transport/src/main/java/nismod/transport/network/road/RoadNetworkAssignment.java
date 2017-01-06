/**
 * 
 */
package nismod.transport.network.road;

import java.util.HashMap;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import nismod.transport.demand.ODMatrix;

/**
 * Network assignment of origin-destination flows
 * @author Milan Lovric
 *
 */
public class RoadNetworkAssignment {
	
	public static final double SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph
	
	private RoadNetwork roadNetwork;
	private HashMap<Integer, Double> linkVolumes;
	private HashMap<Integer, HashMap<String, Double>> linkVolumesPerVehicleType;
	private HashMap<Integer, Double> linkTravelTime;
	
	/**
	 * @param roadNetwork
	 */
	public RoadNetworkAssignment(RoadNetwork roadNetwork) {
		
		this.roadNetwork = roadNetwork;
		this.linkVolumes = new HashMap<Integer, Double>();
		this.linkVolumesPerVehicleType = new HashMap<Integer, HashMap<String, Double>>();
		this.linkTravelTime = new HashMap<Integer, Double>();
	}
	
	/**
	 * @param passengerODM
	 */
	public void assignPassengerFlows(ODMatrix passengerODM) {
		
	}
	
	/**
	 * @param freightODM
	 */
	public void assignFreightFlows(ODMatrix freightODM) {
		
	}
	
	public void updateLinkTravelTimes() {
		
	}
	
}