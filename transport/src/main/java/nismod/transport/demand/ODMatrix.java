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
 * Origin-destination matrix
 * @author Milan Lovric
 *
 */
public class ODMatrix {
	
	private MultiKeyMap matrix;
	
	public ODMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file
	 * @param filePath Path to the input file
	 * @throws IOException 
	 * @throws FileNotFoundException 
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
				matrix.put(record.get(0), destination, flow);			
			}
		} parser.close(); 
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getFlow(String originZone, String destinationZone) {
		
		return (int) matrix.get(originZone, destinationZone);
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, int flow) {
		
		matrix.put(originZone, destinationZone, flow);
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
		
		Set<String> firstKey = new HashSet<String>();
		Set<String> secondKey = new HashSet<String>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<String> firstKeyList = new ArrayList(firstKey);
		List<String> secondKeyList = new ArrayList(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin   "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.print(o);
			for (String s: secondKeyList) System.out.printf("%10d", matrix.get(o,s));
			System.out.println();
		}
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
	public double getAbsoluteDifference(ODMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			difference += Math.abs(this.getFlow(origin, destination) - other.getFlow(origin, destination));
		}
	
		return difference;
	}
}
