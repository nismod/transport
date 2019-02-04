package nismod.transport.rail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.rail.Station.RailModeType;

/**
 * This class stores passenger rail demand = station usage data (entries + exists).
 * @author Milan Lovric
 *
 */
public class RailStationDemand {
	
	private final static Logger LOGGER = LogManager.getLogger(RailStationDemand.class);
	
	private HashMap<Integer, Station> railDemandMap; //maps NLC to station object
	private ArrayList<Station> railDemandList; //list of station objects
	
	public RailStationDemand(String fileName) throws FileNotFoundException, IOException {
	
		LOGGER.info("Reading rail station demand from the file: {}", fileName);
		
		this.railDemandMap = new HashMap<Integer, Station>();
		this.railDemandList = new ArrayList<Station>();
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();

		for (CSVRecord record : parser) { 
		
			int nlc = Integer.parseInt(record.get(0));
			RailModeType mode = RailModeType.valueOf(record.get(1));
			String stationName = record.get(2);
			String naptanName = record.get(3);
			int easting = Integer.parseInt(record.get(4));
			int northing = Integer.parseInt(record.get(5));
			int yearUsage = Integer.parseInt(record.get(6));
			int dayUsage = Integer.parseInt(record.get(7));
			int runDays = Integer.parseInt(record.get(8));
			String ladCode = record.get(9);
			String ladName = record.get(10);
			
			Station station = new Station(nlc, mode, stationName, naptanName, easting, northing, yearUsage, dayUsage, runDays, ladCode, ladName);
			this.railDemandMap.put(nlc, station);
			this.railDemandList.add(station);
		}

		parser.close();
		LOGGER.debug("Finished reading rail station demand from the file.");
	}
	
	/**
	 * Print rail demand.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemand(String message) {
		
		System.out.println(message);
		for (Station station: this.railDemandList) {
			
			System.out.println(station.toString());
		}
	}
	
	/**
	 * Print rail demand sorted on NLC.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemandNLCSorted(String message) {
	
		this.sortStationsOnNLC();
		System.out.println(message);
		for (Station station: this.railDemandList) {
			System.out.println(station.toString());
		}
	}
	
	/**
	 * Print rail demand sorted on station name.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemandNameSorted(String message) {
		
		this.sortStationsOnName();
		System.out.println(message);
		for (Station station: this.railDemandList) {
			
			System.out.println(station.toString());
		}
	}
	
	/**
	 * Sorts stations on NLC in an ascending order.
	 */
	private void sortStationsOnNLC() {

		Comparator<Station> c = new Comparator<Station>() {
		public int compare(Station s, Station s2) {
		    	Integer nlc = s.getNLC();
		       	Integer nlc2 = s2.getNLC();
		       	return nlc.compareTo(nlc2);
		    	}
		};
		
		Collections.sort(this.railDemandList, c);
	}
	
	/**
	 * Sorts stations on station name in an ascending order.
	 */
	private void sortStationsOnName() {

		Comparator<Station> c = new Comparator<Station>() {
		public int compare(Station s, Station s2) {
		    	String name = s.getName();
		       	String name2 = s2.getName();
		       	return name.compareTo(name2);
		    	}
		};
		
		Collections.sort(this.railDemandList, c);
	}
	
}
