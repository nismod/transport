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
	
	private final static Logger LOGGER = LogManager.getLogger();
	
	public final static int MAX_FREIGHT_ZONE_ID = 1388;
	public final static int MAX_VEHICLE_ID = 3;
	
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
				for (int v=1; v<=3; v++)
					if (this.getFlow(o, d, v) != 0) //print only if there is a flow
						System.out.printf("%6d%12d%12d%7d\n", o, d, v, this.getFlow(o, d, v));
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
	 * Gets the sorted list of origins.
	 * @return List of origins.
	 */
	public List<Integer> getSortedOrigins() {
		
		LOGGER.debug("Getting the sorted origins.");
		
		Set<Integer> firstKey = new HashSet<Integer>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list and sort them
		List<Integer> firstKeyList = new ArrayList(firstKey);
		Collections.sort(firstKeyList);
	
		LOGGER.trace("Origins sorted and returned.");
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<Integer> getSortedDestinations() {
		
		LOGGER.debug("Getting the sorted destinations.");
		
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			int destination = (int) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<Integer> secondKeyList = new ArrayList(secondKey);
		Collections.sort(secondKeyList);
		
		LOGGER.trace("Origins sorted and returned.");
		return secondKeyList;
	}
	
	/**
	 * Gets the unsorted list of origins.
	 * @return List of origins.
	 */
	public List<Integer> getUnsortedOrigins() {
		
		LOGGER.debug("Getting the unsorted origins.");
		
		Set<Integer> firstKey = new HashSet<Integer>();
		
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			int origin = (int) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list
		List<Integer> firstKeyList = new ArrayList(firstKey);

		return firstKeyList;
	}
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destinations.
	 */
	public List<Integer> getUnsortedDestinations() {
		
		LOGGER.debug("Getting the unsorted destinations.");
		
		Set<Integer> secondKey = new HashSet<Integer>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			int destination = (int) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list
		List<Integer> secondKeyList = new ArrayList(secondKey);
		
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
	 * Creates a unit freight matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return Unit freight matrix.
	 */
	public static FreightMatrix createUnitMatrix(List<Integer> origins, List<Integer> destinations) {
		
		FreightMatrix fm = new FreightMatrix();
		
		for (int origin: origins)
			for (int destination: destinations)
				for (int vehicleType = 1; vehicleType <= 3; vehicleType++)
					fm.setFlow(origin, destination, vehicleType, 1);
		
		return fm;
	}
	
	/**
	 * Creates a unit freight matrix for the specific DfT BYFM 2006 zoning system.
	 * @return Unit BYFM freight matrix.
	 */
	public static FreightMatrix createUnitBYFMMatrix() {
		
		List<Integer> origins = new ArrayList<Integer>();
		for (int i=1; i<=33; i++) origins.add(i);
		for (int i=101; i<=123; i++) origins.add(i);
		for (int i=201; i<=243; i++) origins.add(i);
		for (int i=301; i<=321; i++) origins.add(i);
		for (int i=401; i<=440; i++) origins.add(i);
		for (int i=501; i<=534; i++) origins.add(i);
		for (int i=601; i<=645; i++) origins.add(i);
		for (int i=701; i<=748; i++) origins.add(i);
		for (int i=801; i<=867; i++) origins.add(i);
		for (int i=901; i<=922; i++) origins.add(i);
		for (int i=1001; i<=1032; i++) origins.add(i);
		for (int i=1111; i<=1115; i++) origins.add(i);
		for (int i=1201; i<=1256; i++) origins.add(i);
		for (int i=1301; i<=1388; i++) origins.add(i);
		
		List<Integer> destinations = new ArrayList<Integer>(origins);
		FreightMatrix fm = FreightMatrix.createUnitMatrix(origins, destinations);
		fm.deleteInterzonalFlows(645); //delete flows from/to Isle of Scilly (645=E06000053)
		
		return fm;
	}
	
	/**
	 * Deletes all inter-zonal flows to/from a particular zone (leaving only intra-zonal flows)
	 * @param zone Zone for which inter-zonal flows need to be deleted from the freight matrix.
	 */
	public void deleteInterzonalFlows(int zone) {
		
		LOGGER.debug("Deleting inter-zonal flows from/to zone {}...", zone);
		
		List<Integer> origins = this.getUnsortedOrigins();
		List<Integer> destinations = this.getUnsortedDestinations();
			
		for (Integer origin: origins)
			for (Integer destination: destinations)
				for (int vehicleType = 1; vehicleType <= 3; vehicleType++)
					if (origin.equals(zone) && !destination.equals(zone) || !origin.equals(zone) && destination.equals(zone)) //this will leave only intra-zonal flow
						this.setFlow(origin, destination, vehicleType, 0);
			
		LOGGER.debug("Done deleting inter-zonal flows.");
	}
	
	/**
	 * Gets the keyset of the multimap.
	 * @return Key set.
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow() {
		
		int totalFlow = 0;
		for (MultiKey mk: this.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			totalFlow += this.getFlow(origin, destination, vehicleType);
		}
	
		return totalFlow;
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
	 * Scales matrix flows using a real-valued scaling matrix.
	 * @param scale Scaling factors.
	 */
	public void scaleMatrix(SkimMatrixFreight scale) {
		
		FreightMatrix scaled = new FreightMatrix();
		
		for (MultiKey mk: this.getKeySet()) {
			int origin = (int) mk.getKey(0);
			int destination = (int) mk.getKey(1);
			int vehicleType = (int) mk.getKey(2);
			int flow = (int) Math.round(this.getFlow(origin, destination, vehicleType) * scale.getCost(origin,  destination,  vehicleType));
			scaled.setFlow(origin, destination, vehicleType, flow);
		}
		
		this.matrix = scaled.matrix;
	}
	
	/**
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.debug("Saving freight OD matrix.");
		
		List<Integer> firstKeyList = this.getSortedDestinations();
		List<Integer> secondKeyList = this.getSortedOrigins();
	
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
					for (Integer v=1; v<=3; v++)
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
