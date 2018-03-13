package nismod.transport.zone;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import nismod.transport.decision.CongestionCharging;

/**
 * Node to node matrix with joint probability (used for assigning inter-zonal TEMPRo flows between the nodes contained in that zone). 
 * @author Milan Lovric
 *
 */
public class NodeMatrix {
	
	private final static Logger LOGGER = Logger.getLogger(NodeMatrix.class.getName());
	
	private MultiKeyMap matrix;
	
	public NodeMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Gets the probability for a given origin-destination node pair.
	 * @param originNode Origin node.
	 * @param destinationNode Destination node.
	 * @return Origin-destination probability.
	 */
	public double getValue(Integer originNode, Integer destinationNode) {
		
		Double value = (Double) matrix.get(originNode, destinationNode);
		if (value == null) return 0.0;
		else return value;
	}
	
	/**
	 * Sets the probability for a given origin-destination node pair.
	 * @param originNode Origin node.
	 * @param destinationNode Destination node.
	 * @param value Origin-destination probability.
	 */
	public void setValue(Integer originNode, Integer destinationNode, double value) {
	
		matrix.put(originNode,  destinationNode, value);
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
		
		List<Integer> firstKeyList = this.getOrigins();
		List<Integer> secondKeyList = this.getDestinations();
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin    "); for (Integer i: secondKeyList) System.out.printf("%10d",i);
		System.out.println();
		for (Integer o: firstKeyList) {
			System.out.printf("%-10d", o);
			for (Integer d: secondKeyList) System.out.printf("%10.3f", this.getValue(o,d));
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
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<Integer> getOrigins() {
		
		Set<Integer> firstKey = new HashSet<Integer>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			Integer origin = (Integer) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list and sort them
		List<Integer> firstKeyList = new ArrayList(firstKey);
		Collections.sort(firstKeyList);
		
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<Integer> getDestinations() {
		
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			Integer destination = (Integer) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<Integer> secondKeyList = new ArrayList(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(NodeMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			difference += Math.abs(this.getValue(origin, destination) - other.getValue(origin, destination));
		}
	
		return difference;
	}
	
	/**
	 * Scales matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double flow = this.getValue(origin, destination);
			flow = flow * factor;
			//this.setFlow(origin, destination, (int) Math.round(flow));
			this.setValue(origin, destination, flow);
			
		}		
	}
	
	/**
	 * Normalises matrix values so they sum up to one.
	 */
	public void normalise() {
		
		double sum = 0.0;
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double value = this.getValue(origin, destination);
			sum += value;
		}
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double value = this.getValue(origin, destination);
			this.setValue(origin, destination, value / sum);
		}
	}
	
	/**
	 * Normalises matrix values with zero diagonal values. 
	 */
	public void normaliseWithZeroDiagonal() {
		
		double sum = 0.0;
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double value = this.getValue(origin, destination);
			if (origin.intValue() != destination.intValue()) sum += value;
		}
		System.out.println("sum = " + sum);
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double value = this.getValue(origin, destination);
			if (origin.intValue() == destination.intValue()) 
				this.setValue(origin, destination, 0.0);
			else
				this.setValue(origin, destination, value / sum);
		}
	}
	
	/**
	 * Scales matrix values with another matrix (element-wise multiplication).
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(NodeMatrix scalingMatrix) {
		
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			double flow = this.getValue(origin, destination);
			double scalingFactor = scalingMatrix.getValue(origin, destination);
			flow = flow * scalingFactor;
			//this.setFlow(origin, destination, (int) Math.round(flow));
			this.setValue(origin, destination, flow);
		}		
	}
	
//	/**
//	 * Rounds OD matrix values.
//	 */
//	public void roundMatrixValues() {
//
//		for (MultiKey mk: this.getKeySet()) {
//			String origin = (String) mk.getKey(0);
//			String destination = (String) mk.getKey(1);
//			double flow = (int) Math.round(this.getFlow(origin, destination));
//			this.setFlow(origin, destination, flow);
//		}
//	}
	
	public void printMatrixFormatted(String s) {
		
		System.out.println(s);
		this.printMatrixFormatted();
	}
	
	/**
	 * Creates a unit OD matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return
	 */
	public static NodeMatrix createUnitMatrix(List<Integer> origins, List<Integer> destinations) {
		
		NodeMatrix odm = new NodeMatrix();
		
		for (Integer origin: origins)
			for (Integer destination: destinations)
				odm.setValue(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones List of zones.
	 * @return Unit OD matrix.
	 */
	public static NodeMatrix createUnitMatrix(List<Integer> zones) {
		
		NodeMatrix odm = new NodeMatrix();
		
		for (Integer origin: zones)
			for (Integer destination: zones)
				odm.setValue(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of zones.
	 * @return Unit OD matrix.
	 */
	public static NodeMatrix createUnitMatrix(Set<Integer> zones) {
		
		NodeMatrix odm = new NodeMatrix();
		
		for (Integer origin: zones)
			for (Integer destination: zones)
				odm.setValue(origin, destination, 1.0);
		
		return odm;
	}
	
	@Override
	public NodeMatrix clone() {

		NodeMatrix odm = new NodeMatrix();
		
		for (MultiKey mk: this.getKeySet()) {
			Integer origin = (Integer) mk.getKey(0);
			Integer destination = (Integer) mk.getKey(1);
			odm.setValue(origin, destination, this.getValue(origin, destination));
		}

		return odm;
	}
	
	/**
	 * Saves the matrix into a csv file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		Set<Integer> firstKey = new HashSet<Integer>();
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			Integer origin = (Integer) ((MultiKey)mk).getKey(0);
			Integer destination = (Integer) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<Integer> firstKeyList = new ArrayList<Integer>(firstKey);
		List<Integer> secondKeyList = new ArrayList<Integer>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		for (Integer s: secondKeyList) header.add(String.valueOf(s));
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (Integer origin: firstKeyList) {
				record.clear();
				record.add(String.valueOf(origin));
				//for (String destination: secondKeyList) record.add(String.format("%.2f", this.getFlow(origin, destination)));
				for (Integer destination: secondKeyList) record.add(String.format("%d", (int) Math.round(this.getValue(origin, destination))));
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			System.err.println("Error in CsvFileWriter!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.err.println("Error while flushing/closing fileWriter/csvPrinter!");
				e.printStackTrace();
			}
		}
	}
}
