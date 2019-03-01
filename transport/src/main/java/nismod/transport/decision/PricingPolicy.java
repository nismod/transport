package nismod.transport.decision;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import nismod.transport.network.road.RoadNetworkAssignment.TimeOfDay;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;

/**
 * A class that encapsulates the pricing policy for congestion charging intervention.
 * @author Milan Lovric
 *
 */
public class PricingPolicy {
	
	private EnumMap<VehicleType, EnumMap<TimeOfDay, double[]>> policy;
	private String policyName;
	private List<Integer> policyEdges;
	
	/**
	 * Reads congestion charge file which contains charges that depend on vehicle type and time of day (hour).
	 * @param policyName Name of the policy.
	 * @param fileName File name.
	 * @param maxEdgeID Maximum edge ID.
	 * @param edgeIDs List of edge IDs affected by the policy.
	 * @return Map with congestion charges.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public PricingPolicy (String policyName, String fileName, int maxEdgeID, List<Integer> edgeIDs) throws FileNotFoundException, IOException {

		this.policyName = policyName;
		this.policy = new EnumMap<>(VehicleType.class);
		this.policyEdges = new ArrayList<Integer>();
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("vehicleType");
		//System.out.println("keySet = " + keySet);
		Double charge;
		for (CSVRecord record : parser) {
			//System.out.println(record);
			VehicleType vht = VehicleType.valueOf(record.get(0));
			
			//fetch inner map
			EnumMap<TimeOfDay, double[]> map = this.policy.get(vht);
			if (map == null) {
				map = new EnumMap<TimeOfDay, double[]>(TimeOfDay.class);
				this.policy.put(vht, map);
			}
			//get the charge amount
			for (String time: keySet) {
				//System.out.println("Time of day = " + time);
				charge = Double.parseDouble(record.get(time));
				//create array
				double[] linkCharges = new double[maxEdgeID];
				for (int ID: edgeIDs) {
					linkCharges[ID] = charge;
					this.policyEdges.add(ID);
				}
				map.put(TimeOfDay.valueOf(time), linkCharges);			
			}
		}
		parser.close(); 
	}
	
	/**
	 * Get the entire pricing policy (for combinations of vehicle type and time of day).
	 * @return Pricing policy.
	 */
	public EnumMap<VehicleType, EnumMap<TimeOfDay, double[]>> getPolicy() {
		
		return this.policy;
	}
		
	/**
	 * Get link charges for a particular combination of vehicle type and time of day.
	 * @param vht Vehicle type.
	 * @param time Time of day.
	 * @return Array with link charges.
	 */
	public double[] getLinkCharges(VehicleType vht, TimeOfDay time) {
		
		//fetch inner map
		EnumMap<TimeOfDay, double[]> map = this.policy.get(vht);
		if (map == null) return null;
				
		return map.get(time);
	}
	
	/**
	 * Return policy name.
	 * @return Policy name.
	 */
	public String getPolicyName() {
		
		return this.policyName;
	}
	
	/**
	 * Return policy edges.
	 * @return Policy edges.
	 */
	public List<Integer> getPolicyEdges() {
		
		return this.policyEdges;
	}
}
