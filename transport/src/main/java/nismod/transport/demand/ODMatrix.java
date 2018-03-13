/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix
 * @author Milan Lovric
 *
 */
public class ODMatrix {
	
	private final static Logger LOGGER = Logger.getLogger(ODMatrix.class.getName());
	
	private MultiKeyMap matrix;
	
	public ODMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 */
	public ODMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		int flow;
		for (CSVRecord record : parser) { 
			//System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				flow = Integer.parseInt(record.get(destination));
				this.setFlow(record.get(0), destination, flow);			
			}
		}
		parser.close(); 
	}
	
	/**
	 * Constructor that rounds the flows of a real-valued OD matrix. 
	 * @param realMatrix Origin-destination matrix with real-valued flows.
	 */
	public ODMatrix(RealODMatrix realMatrix) {
		
		matrix = new MultiKeyMap();
		for (String o: realMatrix.getOrigins())
			for (String d: realMatrix.getDestinations()) {
				int flow = (int) Math.round(realMatrix.getFlow(o, d));
				this.setFlow(o, d, flow);
			}
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getFlow(String originZone, String destinationZone) {
		
		Integer flow = (Integer) matrix.get(originZone, destinationZone);
		if (flow == null) return 0;
		else return flow;
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, int flow) {
	
		if (flow != 0)		matrix.put(originZone, destinationZone, flow);
		//do not store zero flows into the matrix (skip zero flow or remove if already exists)
		else // flow == 0 
			if (this.getFlow(originZone, destinationZone) != 0)
							matrix.removeMultiKey(originZone, destinationZone);
	}
	
	/**
	 * Calculates the number of trips starting in each origin zone
	 * @return Number of trip starts.
	 */
	public HashMap<String, Integer> calculateTripStarts() {
		
		HashMap<String, Integer> tripStarts = new HashMap<String, Integer>();
		
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
	
			Integer number = tripStarts.get(origin);
			if (number == null) number = 0;
			number += this.getFlow(origin, destination);
			tripStarts.put(origin, number);
		}
		
		return tripStarts;
	}
	
	/**
	 * Calculates the number of trips ending in each destination zone
	 * @return Number of trip ends.
	 */
	public HashMap<String, Integer> calculateTripEnds() {
		
		HashMap<String, Integer> tripEnds = new HashMap<String, Integer>();
		
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
	
			Integer number = tripEnds.get(destination);
			if (number == null) number = 0;
			number += this.getFlow(origin, destination);
			tripEnds.put(destination, number);
		}
		
		return tripEnds;
	}
	
	/**
	 * Prints the full matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		List<String> firstKeyList = this.getOrigins();
		List<String> secondKeyList = this.getDestinations();
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin    "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.printf("%-10s", o);
			for (String s: secondKeyList) System.out.printf("%10d", this.getFlow(o,s));
			System.out.println();
		}
	}
	
	
	/**
	 * Prints the matrix as a formatted table, with a print message.
	 * @param s Print message
	 */
	public void printMatrixFormatted(String s) {
				
		System.out.println(s);
		this.printMatrixFormatted();
	}
	
	
	/**
	 * Gets the keyset of the multimap.
	 * @return Key set.
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getOrigins() {
		
		Set<String> firstKey = new HashSet<String>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list and sort them
		List<String> firstKeyList = new ArrayList(firstKey);
		Collections.sort(firstKeyList);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getDestinations() {
		
		Set<String> secondKey = new HashSet<String>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			String destination = (String) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/**
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalFlow() {
		
		int totalFlow = 0;
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			totalFlow += this.getFlow(origin, destination);
		}
	
		return totalFlow;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(ODMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			difference += Math.abs(this.getFlow(origin, destination) - other.getFlow(origin, destination));
		}
	
		return difference;
	}
	

	/**
	 * Scales (and rounds) matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double flow = this.getFlow(origin, destination);
			flow = flow * factor;
			this.setFlow(origin, destination, (int) Math.round(flow));
		}		
	}
	
	/**
	 * Creates a unit OD matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return
	 */
	public static ODMatrix createUnitMatrix(List<String> origins, List<String> destinations) {
		
		ODMatrix odm = new ODMatrix();
		
		for (String origin: origins)
			for (String destination: destinations)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones List of origin zones.
	 * @return Unit OD matrix.
	 */
	public static ODMatrix createUnitMatrix(List<String> zones) {
		
		ODMatrix odm = new ODMatrix();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param origins Set of origin zones.
	 * @return Unit OD matrix.
	 */
	public static ODMatrix createUnitMatrix(Set<String> zones) {
		
		ODMatrix odm = new ODMatrix();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	
	/**
	 * Sums the elements of a matrix subset (provided as two lists of origins and destinations).
	 * @param origins List of origin zones (a subset).
	 * @param destinations List of desintation zones (a subset).
	 * @return
	 */
	public int sumMatrixSubset(List<String> origins, List<String> destinations) {
		
		int sum = 0;
		for (String origin: origins)
			for (String destination: destinations)
				sum += this.getFlow(origin, destination);
		
		return sum;
	}
	
	/**
	 * @param ladODMatrix LAD to LAD OD matrix.
	 * @param temproWeights TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * 
	 * @return TEMPro based OD matrix.
	 */
	public static ODMatrix createTEMProFromLadMatrix(ODMatrix ladODMatrix, ODMatrix baseTempro, Zoning zoning) {
		
		ODMatrix temproMatrix = new ODMatrix();
		
		for (MultiKey mk: ladODMatrix.getKeySet()) {
			String originLAD = (String) mk.getKey(0);
			String destinationLAD = (String) mk.getKey(1);
			
			int ladFlow = ladODMatrix.getFlow(originLAD, destinationLAD);
					
			//get tempro zones contained within originLAD and destinationLAD
			List<String> temproOrigins = zoning.getLADToListOfContainedZones().get(originLAD);
			List<String> temproDestinations = zoning.getLADToListOfContainedZones().get(destinationLAD);
			
			//sum all elements of the base tempro submatrix
			int temproSum = baseTempro.sumMatrixSubset(temproOrigins, temproDestinations);
			
			//disaggregate LAD flow using the weights of the underlying tempro submatrix
			for (String origin: temproOrigins)
				for (String destination: temproDestinations) {
					int temproFlow = (int) Math.round(1.0 * baseTempro.getFlow(origin, destination) / temproSum * ladFlow);
					temproMatrix.setFlow(origin, destination, temproFlow);
				}
		}
		
		return temproMatrix;
	}
	
	/**
	 * @param temproMatrix TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * 
	 * @return LAD based OD matrix.
	 */
	public static ODMatrix createLadMatrixFromTEMProMatrix(ODMatrix temproMatrix, Zoning zoning) {
		
		ODMatrix ladMatrix = new ODMatrix();
		
		for (MultiKey mk: temproMatrix.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			int flow = temproMatrix.getFlow(origin, destination);
			
			String originLAD = zoning.getZoneToLADMap().get(origin);
			String destinationLAD = zoning.getZoneToLADMap().get(destination);
			
			Integer flowLAD = ladMatrix.getFlow(originLAD, destinationLAD);
			if (flowLAD == null) flowLAD = 0;
			flowLAD += flow;
			ladMatrix.setFlow(originLAD, destinationLAD, flowLAD);
		}
		
		return ladMatrix;
	}
	
	@Override
	public ODMatrix clone() {

		ODMatrix odm = new ODMatrix();
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			odm.setFlow(origin, destination, this.getFlow(origin, destination));
		}

		return odm;
	}
}
