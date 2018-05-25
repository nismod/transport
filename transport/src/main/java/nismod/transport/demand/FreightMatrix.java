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
 * Origin-destination matrix for freight vehicles (following the format of DfT's BYFM 2006 study).
 * @author Milan Lovric
 *
 */
public class FreightMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(FreightMatrix.class);
	
	private MultiKeyMap matrix;
	
	public FreightMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public FreightMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		int origin, destination, vehicleType, flow;
		for (CSVRecord record : parser) {
			destination  = Integer.parseInt(record.get(0)); //destination is first in DfT's matrix!
			origin = Integer.parseInt(record.get(1)); //origin is second in DfT's matrix!
			vehicleType = Integer.parseInt(record.get(2));
			flow = Integer.parseInt(record.get(3));
			this.setFlow(origin, destination, vehicleType, flow);			
			
		}
		parser.close(); 
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
		
		if (flow != 0)		matrix.put(origin, destination, vehicleType, flow);
		//do not store zero flows into the matrix (skip zero flow or remove if already exists)
		else // flow == 0 
			if (matrix.get(origin, destination, vehicleType) != null) 
				matrix.removeMultiKey(origin, destination, vehicleType);
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
		
		//System.out.println(matrix.toString());
		
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
				for (int v=0; v<=3; v++)
					if (this.getFlow(o, d, v) != 0) //print only if there is a flow
						System.out.printf("%6d%12d%12d%7d\n", o, d, v, this.getFlow(o, d, v));
	}
	
	/**
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<Integer> getOrigins() {
		
		Set<Integer> firstKey = new HashSet<Integer>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
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
			int destination = (int) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<Integer> secondKeyList = new ArrayList(secondKey);
		Collections.sort(secondKeyList);
		
		return secondKeyList;
	}
	
	/**
	 * Gets the sorted list of vehicle types.
	 * @return List of vehicle types.
	 */
	public List<Integer> getVehicleTypes() {
		
		Set<Integer> thirdKey = new HashSet<Integer>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			int vehicle = (int) ((MultiKey)mk).getKey(2);
			thirdKey.add(vehicle);
		}
		//put them into a list and sort them
		List<Integer> thirdKeyList = new ArrayList(thirdKey);
		Collections.sort(thirdKeyList);
		
		return thirdKeyList;
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
	
	/**
	 * Multiplies each value of the matrix with a scaling factor.
	 * @param scale Scaling factor.
	 * @return Scaled freight matrix.
	 */
	public FreightMatrix getScaledMatrix(double scale) {
		
		FreightMatrix scaled = new FreightMatrix();
		for (MultiKey mk: this.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			int flow = (int) Math.round(this.getFlow(origin, destination, vehicleType) * scale);
			scaled.setFlow(origin, destination, vehicleType, flow);
		}
		return scaled;
	}
	
	/**
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.debug("Saving freight OD matrix.");
		
		List<Integer> firstKeyList = this.getDestinations();
		List<Integer> secondKeyList = this.getOrigins();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("destination");
		header.add("origin");
		header.add("vehicleType");
		header.add("flow");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (Integer d: firstKeyList)
				for (Integer o: secondKeyList)
					for (Integer v=0; v<=3; v++)
						if (this.getFlow(o, d, v) != 0) {//print only if there is a flow
							record.clear();
							record.add(d.toString());
							record.add(o.toString());
							record.add(v.toString());
							record.add(String.format("%d", this.getFlow(o, d, v)));
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
