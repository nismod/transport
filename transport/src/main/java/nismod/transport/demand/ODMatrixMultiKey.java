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

import org.locationtech.jts.geom.Point;

import nismod.transport.zone.Zoning;

/**
 * Origin-destination matrix for passenger vehicles.
 * @author Milan Lovric
 *
 */
public class ODMatrixMultiKey implements AssignableODMatrix {
	
	private final static Logger LOGGER = LogManager.getLogger(ODMatrixMultiKey.class);
	
	private MultiKeyMap matrix;
	
	public ODMatrixMultiKey() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads OD matrix from an input csv file.
	 * @param fileName Path to the input file.
	 * @throws FileNotFoundException if any.
	 * @throws IOException if any.
	 */
	public ODMatrixMultiKey(String fileName) throws FileNotFoundException, IOException {
		
		LOGGER.info("Reading OD matrix from file: {}", fileName);

		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();

		if (keySet.contains("origin") &&  keySet.contains("destination") && keySet.contains("flow")) { //use list format
			for (CSVRecord record : parser) { 
				String origin = record.get(0);
				String destination = record.get(1);
				int flow = Integer.parseInt(record.get(2));
				this.setFlow(origin, destination, flow);
			}
		} else { //use matrix format

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
		}
		parser.close();
		LOGGER.debug("Finished reading OD matrix from file.");
	}
	
	/**
	 * Constructor that rounds the flows of a real-valued OD matrix. 
	 * @param realMatrix Origin-destination matrix with real-valued flows.
	 */
	public ODMatrixMultiKey(RealODMatrix realMatrix) {
		
		matrix = new MultiKeyMap();
		for (String o: realMatrix.getUnsortedOrigins())
			for (String d: realMatrix.getUnsortedDestinations()) {
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
	 * Gets the flow for a given origin-destination pair as a whole number.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination flow.
	 */
	public int getIntFlow(String originZone, String destinationZone) {
		
		return getFlow(originZone, destinationZone);
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
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
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
	public List<String> getSortedOrigins() {
		
		LOGGER.debug("Getting the sorted origins.");
			
		Set<String> firstKey = new HashSet<String>();
		
		LOGGER.trace("Extracting row keysets.");
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list and sort them
		List<String> firstKeyList = new ArrayList(firstKey);
		LOGGER.trace("Sorting the origins.");
		Collections.sort(firstKeyList);
		
		LOGGER.trace("Origins sorted and returned.");
		return firstKeyList;
	}
	
	/**
	 * Gets the sorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getSortedDestinations() {
		
		LOGGER.debug("Getting the sorted destinations.");
		
		Set<String> secondKey = new HashSet<String>();
		
		LOGGER.debug("Extracting column keysets.");
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			String destination = (String) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list and sort them
		List<String> secondKeyList = new ArrayList(secondKey);
		LOGGER.debug("Sorting the destinations.");
		Collections.sort(secondKeyList);
		
		LOGGER.debug("Destinations sorted and returned.");
		return secondKeyList;
	}
	
	/**
	 * Gets the unsorted list of origins.
	 * @return List of origins.
	 */
	public List<String> getUnsortedOrigins() {
		
		LOGGER.trace("Getting the unsorted origins.");
			
		Set<String> firstKey = new HashSet<String>();
				
		//extract row keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			firstKey.add(origin);
		}
		//put them into a list
		List<String> firstKeyList = new ArrayList(firstKey);
				
		return firstKeyList;
	}
	
	/**
	 * Gets the unsorted list of destinations.
	 * @return List of destinations.
	 */
	public List<String> getUnsortedDestinations() {
		
		LOGGER.trace("Getting the unsorted destinations.");
		
		Set<String> secondKey = new HashSet<String>();
		
		//extract column keysets
		for (Object mk: matrix.keySet()) {
			String destination = (String) ((MultiKey)mk).getKey(1);
			secondKey.add(destination);
		}
		//put them into a list
		List<String> secondKeyList = new ArrayList(secondKey);
		
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
	 * Gets sum of all the flows in the matrix.
	 * @return Sum of all the flows in the matrix (i.e. number of trips).
	 */
	public int getTotalIntFlow() {
		
		return getTotalFlow();
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(ODMatrixMultiKey other) {
		
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
	 * Gets sum of all the flows.
	 * @return Sum of flows.
	 */
	public int getSumOfFlows() {
		
		int sumOfFlows = 0;
		for (Object flow: matrix.values()) sumOfFlows += (int) flow;
		
		return sumOfFlows;
	}
	
	/**
	 * Creates a new OD matrix (a matrix subset) for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return Matrix subset.
	 */
	public ODMatrixMultiKey getMatrixSubset(List<String> origins, List<String> destinations) {
		
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
		for (String origin: origins)
			for (String destination: destinations)
				odm.setFlow(origin, destination, this.getFlow(origin, destination));
		
		return odm;
	}
	
	/**
	 * Creates a unit OD matrix for given lists of origin and destination zones.
	 * @param origins List of origin zones.
	 * @param destinations List of destination zones.
	 * @return Unit OD matrix.
	 */
	public static ODMatrixMultiKey createUnitMatrix(List<String> origins, List<String> destinations) {
		
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
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
	public static ODMatrixMultiKey createUnitMatrix(List<String> zones) {
		
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
		
		return odm;
	}
	
	/**
	 * Creates a quadratic unit OD matrix for a given lists of zones.
	 * @param zones Set of origin zones.
	 * @return Unit OD matrix.
	 */
	public static ODMatrixMultiKey createUnitMatrix(Set<String> zones) {
		
		LOGGER.info("Creating the unit matrix for {} x {} zones.", zones.size(), zones.size());
		
		System.gc();
		
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
		for (String origin: zones)
			for (String destination: zones)
				odm.setFlow(origin, destination, 1);
	
		LOGGER.debug("Done creating the unit matrix.");
		return odm;
	}
	
	/**
	 * Creates a unit OD matrix for a given lists of zones with a distance threshold.
	 * If straight line distance between origin and destination zone centroids is larger than threshold that flow is zero.
	 * @param zones Set of origin zones.
	 * @param centroids List of zone centroids.
	 * @param threshold Distance threshold in [m].
	 * @return Unit OD matrix.
	 */
	public static ODMatrixMultiKey createSparseUnitMatrix(Set<String> zones, HashMap<String, Point> centroids, double threshold) {
		
		LOGGER.info("Creating the sparse unit matrix for {} x {} zones with threshold set to {}.", zones.size(), zones.size(), threshold);
		
		System.gc();
		
		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
		for (String origin: zones) {
			for (String destination: zones) {
				Point originCentroid = centroids.get(origin);
				Point destinationCentroid = centroids.get(destination);
				if (originCentroid.isWithinDistance(destinationCentroid, threshold))
					odm.setFlow(origin, destination, 1);
			}
		}
	
		LOGGER.debug("Done creating the unit matrix.");
		return odm;
	}
		
	/**
	 * Sums the elements of a matrix subset (provided as two lists of origins and destinations).
	 * @param origins List of origin zones (a subset).
	 * @param destinations List of destination zones (a subset).
	 * @return Sum of the subset.
	 */
	public int sumMatrixSubset(List<String> origins, List<String> destinations) {
		
		int sum = 0;
		for (String origin: origins)
			for (String destination: destinations)
				sum += this.getFlow(origin, destination);
		
		return sum;
	}
	
	/**
	 * Creates tempro OD matrix from LAD OD matrix.
	 * @param ladODMatrix LAD to LAD OD matrix.
	 * @param baseTempro TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return TEMPro based OD matrix.
	 */
	public static ODMatrixMultiKey createTEMProFromLadMatrix(ODMatrixMultiKey ladODMatrix, ODMatrixMultiKey baseTempro, Zoning zoning) {
		
		LOGGER.info("Dissaggregating LAD OD matrix to TEMPro OD matrix.");
		
		ODMatrixMultiKey temproMatrix = new ODMatrixMultiKey();
		
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
					int temproFlow = 0;
					if (temproSum > 0) temproFlow = (int) Math.round(1.0 * baseTempro.getFlow(origin, destination) / temproSum * ladFlow);
					temproMatrix.setFlow(origin, destination, temproFlow);
				}
		}
		
		LOGGER.debug("Finished disaggregating LAD OD matrix to TEMPro OD matrix.");
		return temproMatrix;
	}
	
	/**
	 * Creates LAD OD matrix from TEMPro OD matrix.
	 * @param temproMatrix TEMPro ODMatrix used as weights to disaggregate LAD matrix.
	 * @param zoning Zoning system with mapping between TEMPro and LAD zones.
	 * @return LAD based OD matrix.
	 */
	public static ODMatrixMultiKey createLadMatrixFromTEMProMatrix(ODMatrixMultiKey temproMatrix, Zoning zoning) {
		
		ODMatrixMultiKey ladMatrix = new ODMatrixMultiKey();
		
		for (MultiKey mk: temproMatrix.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			int flow = temproMatrix.getFlow(origin, destination);
			
			String originLAD = zoning.getZoneToLADMap().get(origin);
			String destinationLAD = zoning.getZoneToLADMap().get(destination);
			
			int flowLAD = ladMatrix.getFlow(originLAD, destinationLAD);
			flowLAD += flow;
			ladMatrix.setFlow(originLAD, destinationLAD, flowLAD);
		}
		
		return ladMatrix;
	}
	
	/**
	 * Deletes all inter-zonal flows to/from a particular zone (leaving only intra-zonal flows)
	 * @param zone Zone for which inter-zonal flows need to be deleted from the origin-destination matrix.
	 */
	public void deleteInterzonalFlows(String zone) {
		
		LOGGER.debug("Deleting inter-zonal flows from/to zone {}...", zone);
		
		List<String> origins = this.getUnsortedOrigins();
		List<String> destinations = this.getUnsortedDestinations();
			
		for (String origin: origins)
			for (String destination: destinations)
				if (origin.equals(zone) && !destination.equals(zone) || !origin.equals(zone) && destination.equals(zone)) { //this will leave intra-zonal flow
				this.setFlow(origin, destination, 0);
		}
		
		LOGGER.debug("Done deleting inter-zonal flows.");
		
	}
	
	@Override
	public ODMatrixMultiKey clone() {

		ODMatrixMultiKey odm = new ODMatrixMultiKey();
		
		for (MultiKey mk: this.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			odm.setFlow(origin, destination, this.getFlow(origin, destination));
		}

		return odm;
	}
	
	
	
	/**
	 * Saves the matrix into a csv file.
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted(String outputFile) {
		
		LOGGER.info("Saving passenger OD matrix to a csv file.");
		
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
				for (String destination: secondKeyList) record.add(String.format("%d", this.getFlow(origin, destination)));
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
		
		LOGGER.debug("OD matrix saved to a file.");
	}
	
	/**
	 * Saves the matrix into a csv file. Uses a list format (origin, destination, flow).
	 * @param outputFile Path to the output file.
	 */
	public void saveMatrixFormatted2(String outputFile) {
		
		LOGGER.info("Saving OD matrix to a csv file using list format...");
		
		List<String> firstKeyList = this.getSortedOrigins();
		List<String> secondKeyList = this.getSortedDestinations();
	
		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> header = new ArrayList<String>();
		header.add("origin");
		header.add("destination");
		header.add("flow");
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(header);
			ArrayList<String> record = new ArrayList<String>();
			for (String origin: firstKeyList)
				for (String destination: secondKeyList) {
					int flow = this.getFlow(origin, destination);
					if (flow > 0) {
						record.clear();
						record.add(origin);
						record.add(destination);
						record.add(String.format("%d", flow));
						csvFilePrinter.printRecord(record);
					}
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
		
		LOGGER.debug("OD matrix saved to a csv file.");
	}
}
