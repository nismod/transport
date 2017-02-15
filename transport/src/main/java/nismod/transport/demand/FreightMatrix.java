/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Origin-destination freight matrix
 * @author Milan Lovric
 *
 */
public class FreightMatrix {
	
	private MultiKeyMap matrix;
	
	public FreightMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file
	 * @param filePath Path to the input file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public FreightMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		int origin, destination, vehicleType, flow;
		for (CSVRecord record : parser) {
			destination  = Integer.parseInt(record.get(0));
			origin = Integer.parseInt(record.get(1));
			vehicleType = Integer.parseInt(record.get(2));
			flow = Integer.parseInt(record.get(3));
			matrix.put(origin, destination, vehicleType, flow);			
			
		} parser.close(); 
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param origin Freight origin.
	 * @param destination Freight destination.
	 * @param vehicleType Vehicle type.
	 * @return Origin-destination flow.
	 */
	public int getFlow(int origin, int destination, int vehicleType) {
		
		Object flow = matrix.get(origin, destination, vehicleType);
		if (flow == null) flow = 0;
		return (int) flow;
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param origin Freight origin.
	 * @param destination Freight destination.
	 * @param vehicleType Vehicle type.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(int origin, int destination, int vehicleType, int flow) {
		
		matrix.put(origin, destination, vehicleType, flow);
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
		
		System.out.println(matrix.toString());
		
		Set<Integer> firstKey = new HashSet<Integer>();
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract origin and destination keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<Integer> firstKeyList = new ArrayList(firstKey);
		List<Integer> secondKeyList = new ArrayList(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.printf("%6s%12s%12s%7s\n", "origin", "destination", "vehicleType", "flow");
		for (int o: firstKeyList)
			for (int d: secondKeyList)
				for (int v=1; v<=3; v++)
					if (this.getFlow(o, d, v) != 0) //print only if there is a flow
						System.out.printf("%6d%12d%12d%7d\n", o, d, v, this.getFlow(o, d, v));
	}
	
	/**
	 * Gets the keyset of the multimap.
	 * @return Key set.
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(FreightMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			difference += Math.abs(this.getFlow(origin, destination, vehicleType) - other.getFlow(origin, destination, vehicleType));
		}
	
		return difference;
	}
}
