/**
 * 
 */
package nismod.transport.demand;

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

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Origin-destination matrix
 * @author Milan Lovric
 *
 */
public class RealODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(RealODMatrix.class);
	
	private MultiKeyMap matrix;
	
	public RealODMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 */
	public RealODMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		double flow;
		for (CSVRecord record : parser) { 
			//System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				flow = Double.parseDouble(record.get(destination));
				this.setFlow(record.get(0), destination, flow);			
			}
		}
		parser.close(); 
	}
	
	/**
	 * Gets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public double getFlow(String originZone, String destinationZone) {
		
		Double flow = (Double) matrix.get(originZone, destinationZone);
		if (flow == null) return 0.0;
		else return flow;
	}
	
	/**
	 * Sets the flow for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param flow Origin-destination flow.
	 */
	public void setFlow(String originZone, String destinationZone, double flow) {
	
		/*
		if (flow > 0.0)		matrix.put(originZone, destinationZone, flow);
		//do not store zero flows into the matrix (skip zero flow or remove if already exists)
		else // flow == 0 
			if (this.getFlow(originZone, destinationZone) > 0.0)
				matrix.removeMultiKey(originZone, destinationZone);
		*/
		
		//we need to store all values (even 0 and negative, as this is used for deltas in the SPSA algorithm!
		matrix.put(originZone,  destinationZone, flow);
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
			number += (int) Math.round(this.getFlow(origin, destination));
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
			number += (int) Math.round(this.getFlow(origin, destination));
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
			for (String s: secondKeyList) System.out.printf("%10.2f", this.getFlow(o,s));
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
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(RealODMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			difference += Math.abs(this.getFlow(origin, destination) - other.getFlow(origin, destination));
		}
	
		return difference;
	}
	
	/**
	 * Scales matrix values with a scaling factor.
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(double factor) {
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double flow = this.getFlow(origin, destination);
			flow = flow * factor;
			//this.setFlow(origin, destination, (int) Math.round(flow));
			this.setFlow(origin, destination, flow);
			
		}		
	}
	
	/**
	 * Scales matrix values with another matrix (element-wise multiplication).
	 * @param factor Scaling factor.
	 */
	public void scaleMatrixValue(RealODMatrix scalingMatrix) {
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double flow = this.getFlow(origin, destination);
			double scalingFactor = scalingMatrix.getFlow(origin, destination);
			flow = flow * scalingFactor;
			//this.setFlow(origin, destination, (int) Math.round(flow));
			this.setFlow(origin, destination, flow);
		}		
	}
	
	/**
	 * Rounds OD matrix values.
	 */
	public void roundMatrixValues() {

		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			double flow = (int) Math.round(this.getFlow(origin, destination));
			this.setFlow(origin, destination, flow);
		}
	}
	
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
	public static RealODMatrix createUnitMatrix(List<String> origins, List<String> destinations) {
		
		RealODMatrix odm = new RealODMatrix();
		
		for (String origin: origins)
			for (String destination: destinations)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones List of zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix createUnitMatrix(List<String> zones) {
		
		RealODMatrix odm = new RealODMatrix();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of zones.
	 * @return Unit OD matrix.
	 */
	public static RealODMatrix createUnitMatrix(Set<String> zones) {
		
		RealODMatrix odm = new RealODMatrix();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1.0);
		
		return odm;
	}
	
	@Override
	public RealODMatrix clone() {

		RealODMatrix odm = new RealODMatrix();
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			odm.setFlow(origin, destination, this.getFlow(origin, destination));
		}

		return odm;
	}
	
	/**
	 * Saves the matrix into a csv file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
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
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		for (String s: secondKeyList) header.add(s);
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String origin: firstKeyList) {
				record.clear();
				record.add(origin);
				//for (String destination: secondKeyList) record.add(String.format("%.2f", this.getFlow(origin, destination)));
				for (String destination: secondKeyList) record.add(String.format("%d", (int) Math.round(this.getFlow(origin, destination))));
				csvFilePrinter.printRecord(record);
			}
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
}
