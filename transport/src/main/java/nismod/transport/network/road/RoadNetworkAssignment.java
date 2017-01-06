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
	
	public static final double  SPEED_LIMIT_M_ROAD = 112.65; //70mph = 31.29mps = 112.65kph
	public static final double SPEED_LIMIT_A_ROAD = 96.56; //60mph = 26.82mps = 96.56kph
	
	private RoadNetwork roadNetwork;
	private HashMap<String, Double> linkVolumes;
	private HashMap<String, HashMap<String, Double>> linkVolumesPerVehicleType;
	private HashMap<String, Double> linkTravelTime;
	
	public void assignPassengerFlows(ODMatrix passengerODM) {
		
	}
	
	public void assignFreightFlows(ODMatrix freightODM) {
		
	}
	
	public void updateLinkTravelTimes() {
		
	}
	
}