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
 * Skim matrix for storing inter-zonal travel times or costs (for freight vehicles).
 * @author Milan Lovric
 *
 */
public class SkimMatrixFreight {
	
	private final static Logger LOGGER = LogManager.getLogger(SkimMatrixFreight.class);
	
	private MultiKeyMap matrix;
		
	public SkimMatrixFreight() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads freight skim matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public SkimMatrixFreight(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		//System.out.println("keySet = " + keySet);
		int origin, destination, vehicleType;
		double cost;
		for (CSVRecord record : parser) {
			destination  = Integer.parseInt(record.get(0));
			origin = Integer.parseInt(record.get(1));
			vehicleType = Integer.parseInt(record.get(2));
			cost = Double.parseDouble(record.get(3));
			this.setCost(origin, destination, vehicleType, cost);			
			
		}
		parser.close(); 
	}
	
	/**
	 * Gets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @return Origin-destination cost.
	 */
	public Double getCost(int originZone, int destinationZone, int vehicleType) {
		
		Double cost = (Double) matrix.get(originZone, destinationZone, vehicleType);
		//if (cost == null) return 0.0;
		//else return cost;
		return cost;
	}
	
	/**
	 * Sets cost for a given origin-destination pair and a vehicle type.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param vehicleType Vehicle type.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(int originZone, int destinationZone, int vehicleType, double cost) {
		
		matrix.put(originZone, destinationZone, vehicleType, cost);
	}
	
	/**
	 * Prints the matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		Set<Integer> firstKey = new HashSet<Integer>();
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
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
	
		//formatted print
		System.out.printf("%6s%12s%12s%7s\n", "origin", "destination", "vehicleType", "cost");
		for (int o: firstKeyList)
			for (int d: secondKeyList)
				for (int v=1; v<=3; v++)
					if (this.getCost(o, d, v) != null && this.getCost(o, d, v) > 0.0) //print only if there is a cost
						System.out.printf("%6d%12d%12d%7.2f\n", o, d, v, this.getCost(o, d, v));
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
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.debug("Saving freight skim matrix.");
		
		Set<Integer> firstKey = new HashSet<Integer>();
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			int destination = (int) ((MultiKey)mk).getKey(1);
			firstKey.add(destination);
			secondKey.add(origin);
		}
	
		//put them to a list and sort them
		List<Integer> firstKeyList = new ArrayList<Integer>(firstKey);
		List<Integer> secondKeyList = new ArrayList<Integer>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
	
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
					for (Integer v=1; v<=3; v++) {
						Double cost = this.getCost(o, d, v);
						record.clear();
						record.add(d.toString());
						record.add(o.toString());
						record.add(v.toString());
						if (cost != null)	record.add(String.format("%.2f", cost));
						else				//record.add("N/A");
											continue; //do not save record if unknown cost
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
				
	/**
	 * Gets the keyset of the multimap.
	 * @return Keyset.
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets average OD cost (ignores empty matrix cells).
	 * @return Average cost.
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		for (Object cost: matrix.values()) averageCost += (double) cost;
		averageCost /= matrix.size();
		
		return averageCost;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return Average cost.
	 */
	public double getAverageCost(FreightMatrix flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (MultiKey mk: flows.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			averageCost += flows.getFlow(origin, destination, vehicleType) * (double) matrix.get(origin, destination, vehicleType);
			totalFlows += flows.getFlow(origin, destination, vehicleType);
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrixFreight other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			difference += Math.abs(this.getCost(origin, destination, vehicleType) - other.getCost(origin, destination, vehicleType));
		}
	
		return difference;
	}
}
