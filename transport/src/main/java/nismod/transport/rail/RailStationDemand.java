package nismod.transport.rail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nismod.transport.rail.RailDemandModel.ElasticityArea;
import nismod.transport.rail.RailStation.RailModeType;

/**
 * This class stores passenger rail demand = station usage data (entries + exists).
 * @author Milan Lovric
 *
 */
public class RailStationDemand {

	private final static Logger LOGGER = LogManager.getLogger(RailStationDemand.class);

	private Map<Integer, RailStation> railDemandMap; //maps NLC to station object
	private List<RailStation> railDemandList; //list of station objects
	private List<String> header;

	public RailStationDemand(String fileName) throws FileNotFoundException, IOException {

		LOGGER.info("Reading rail station demand from the file: {}", fileName);

		this.railDemandMap = new HashMap<Integer, RailStation>();
		this.railDemandList = new ArrayList<RailStation>();
		this.header = new ArrayList<String>();
		
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		for (Map.Entry<String, Integer> e: parser.getHeaderMap().entrySet()) {
			this.header.add(e.getKey());
		}
		if (this.header.contains("year")) this.header.remove("year"); //model outputs will also contain year column

		for (CSVRecord record : parser) { 
			int nlc = Integer.parseInt(record.get("NLC"));
			RailModeType mode = RailModeType.valueOf(record.get("Mode"));
			String stationName = record.get("Station");
			String naptanName = record.get("NaPTANname");
			int easting = Integer.parseInt(record.get("Easting"));
			int northing = Integer.parseInt(record.get("Northing"));
			int yearUsage = Integer.parseInt(record.get("YearUsage"));
			double dayUsage = Double.parseDouble(record.get("DayUsage"));
			int runDays = Integer.parseInt(record.get("RunDays"));
			String ladCode = record.get("LADcode");
			String ladName = record.get("LADname");
			ElasticityArea area = ElasticityArea.valueOf(record.get("Area"));

			//create station object and store into map and list
			RailStation station = new RailStation(nlc, mode, stationName, naptanName, easting, northing, yearUsage, dayUsage, runDays, ladCode, ladName, area);
			this.railDemandMap.put(nlc, station);
			this.railDemandList.add(station);
		}

		parser.close();
		LOGGER.debug("Finished reading rail station demand from the file.");
	}

	/**
	 * Constructor for empty rail station demand.
	 * @param header
	 */
	public RailStationDemand(List<String> header) {

		this.railDemandMap = new HashMap<Integer, RailStation>();
		this.railDemandList = new ArrayList<RailStation>();
		this.header = header;
	}

	/**
	 * Add a rail station data to the rail demand.
	 * @param station
	 */
	public void addStation(RailStation station) {

		this.railDemandMap.put(station.getNLC(), station);
		this.railDemandList.add(station);
	}

	/**
	 * Getter method for the header.
	 * @return header
	 */
	public List<String> getHeader() {

		return this.header;
	}

	/**
	 * Getter method for the rail demand map.
	 * @return Rail demand map.
	 */
	public Map<Integer, RailStation> getRailDemandMap() {

		return this.railDemandMap;
	}

	/**
	 * Getter method for the rail demand list.
	 * @return Rail demand list
	 */
	public List<RailStation> getRailDemandList() {

		return this.railDemandList;
	}

	/**
	 * Print rail demand.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemand(String message) {

		System.out.println(message);
		System.out.println(this.header);
		for (RailStation station: this.railDemandList)
			System.out.println(station.toString());
	}

	/**
	 * Print rail demand sorted on NLC.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemandNLCSorted(String message) {

		this.sortStationsOnNLC();
		System.out.println(message);
		System.out.println(this.header);
		for (RailStation station: this.railDemandList)
			System.out.println(station.toString());
	}

	/**
	 * Print rail demand sorted on station name.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemandNameSorted(String message) {

		this.sortStationsOnName();
		System.out.println(message);
		System.out.println(this.header);
		for (RailStation station: this.railDemandList)
			System.out.println(station.toString());
	}

	/**
	 * Print rail demand sorted on station usage.
	 * @param message Message to print before the demand.
	 */
	public void printRailDemandUsageSorted(String message) {

		this.sortStationsOnUsage();
		System.out.println(message);
		System.out.println(this.header);
		for (RailStation station: this.railDemandList)
			System.out.println(station.toString());
	}
	
	/**
	 * Calculates yearly zonal usage (the sum for all stations within LAD).
	 * @return Yearly zonal usage.
	 */
	public HashMap<String, Integer> calculateYearlyZonalUsageTotal() {
		
		HashMap<String, Integer> zonalUsage = new HashMap<String, Integer>();
		
		for (RailStation station: this.railDemandList) {
			
			String zone = station.getLADCode();
			
			//fetch current usage
			Integer usage = zonalUsage.get(zone);
			if (usage == null) usage = 0;
			
			//add station usage
			usage += station.getYearlyUsage();
			zonalUsage.put(zone, usage);
		}
		
		return zonalUsage;
	}
	
	/**
	 * Calculates yearly zonal usage (the average for all stations within LAD).
	 * @return Yearly zonal usage per station.
	 */
	public HashMap<String, Integer> calculateYearlyZonalUsageAverage() {
		
		HashMap<String, Integer> zonalUsageTotal = new HashMap<String, Integer>();
		HashMap<String, Integer> numberOfStationsPerLAD = new HashMap<String, Integer>();
		HashMap<String, Integer> zonalUsageAverage = new HashMap<String, Integer>();
	
		//calculate number of stations per LAD and total zonal usage
		for (RailStation station: this.railDemandList) {
			
			String zone = station.getLADCode();
			
			//fetch current usage
			Integer usage = zonalUsageTotal.get(zone);
			if (usage == null) usage = 0;
			
			//add station usage
			usage += station.getYearlyUsage();
			zonalUsageTotal.put(zone, usage);
			
			//fetch current number of stations
			Integer number = numberOfStationsPerLAD.get(zone);
			if (number == null) number = 0;
			
			//increase number of stations
			number++;
			numberOfStationsPerLAD.put(zone, number);
		}
		
		//calculate average zonal usage
		for (String zone: numberOfStationsPerLAD.keySet()) {
			
			Integer averageUsage = (int) Math.round((double) zonalUsageTotal.get(zone) / numberOfStationsPerLAD.get(zone));
			zonalUsageAverage.put(zone, averageUsage);
		}
		
		return zonalUsageAverage;
	}
	
	/**
	 * Calculates daily zonal usage (the sum for all stations within LAD).
	 * @return Daily zonal usage.
	 */
	public HashMap<String, Double> calculateDailyZonalUsageTotal() {
		
		HashMap<String, Double> zonalUsage = new HashMap<String, Double>();
		
		for (RailStation station: this.railDemandList) {
			
			String zone = station.getLADCode();
			
			//fetch current usage
			Double usage = zonalUsage.get(zone);
			if (usage == null) usage = 0.0;
			
			//add station usage
			usage += station.getDayUsage();
			zonalUsage.put(zone, usage);
		}
		
		return zonalUsage;
	}
	
	/**
	 * Calculates daily zonal usage (the average for all stations within LAD).
	 * @return Daily zonal usage per station.
	 */
	public HashMap<String, Double> calculateDailyZonalUsageAverage() {
		
		HashMap<String, Double> zonalUsageTotal = new HashMap<String, Double>();
		HashMap<String, Integer> numberOfStationsPerLAD = new HashMap<String, Integer>();
		HashMap<String, Double> zonalUsageAverage = new HashMap<String, Double>();
	
		//calculate number of stations per LAD and total zonal usage
		for (RailStation station: this.railDemandList) {
			
			String zone = station.getLADCode();
			
			//fetch current usage
			Double usage = zonalUsageTotal.get(zone);
			if (usage == null) usage = 0.0;
			
			//add station usage
			usage += station.getDayUsage();
			zonalUsageTotal.put(zone, usage);
			
			//fetch current number of stations
			Integer number = numberOfStationsPerLAD.get(zone);
			if (number == null) number = 0;
			
			//increase number of stations
			number++;
			numberOfStationsPerLAD.put(zone, number);
		}
		
		//calculate average zonal usage
		for (String zone: numberOfStationsPerLAD.keySet()) {
			
			Double averageUsage = zonalUsageTotal.get(zone) / numberOfStationsPerLAD.get(zone);
			zonalUsageAverage.put(zone, averageUsage);
		}
		
		return zonalUsageAverage;
	}
	
	/**
	 * Creates a list of stations within each LAD.
	 * @return List of stations within each LAD.
	 */
	public HashMap<String, List<RailStation>> createListOfStationsWithinEachLAD() {
		
		HashMap<String, List<RailStation>> map = new HashMap<String, List<RailStation>>();
		
		for (RailStation station: this.railDemandList) {
			
			String zone = station.getLADCode();
			
			//fetch current list
			List<RailStation> list = map.get(zone);
			if (list == null) {
				list = new ArrayList<RailStation>();
				map.put(zone,  list);
			}
			
			//add station to the right list
			list.add(station);
		}
		
		return map;
	}

	/**
	 * Sorts stations on NLC in an ascending order.
	 */
	private void sortStationsOnNLC() {

		Comparator<RailStation> c = new Comparator<RailStation>() {
			public int compare(RailStation s, RailStation s2) {
				Integer nlc = s.getNLC();
				Integer nlc2 = s2.getNLC();
				return nlc.compareTo(nlc2);
			}
		};

		Collections.sort(this.railDemandList, c);
	}

	/**
	 * Saves rail station demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveRailStationDemand(int year, String outputFile) {

		LOGGER.debug("Saving rail station demand to a file.");

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> outputHeader = new ArrayList<String>();
		outputHeader.add("year");
		outputHeader.addAll(this.header);

		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(outputHeader);
			ArrayList<String> record = new ArrayList<String>();

			for (RailStation station: this.railDemandList) {
				record.clear();
				record.add(Integer.toString(year));
				record.add(String.valueOf(station.getNLC()));
				record.add(station.getMode().name());
				record.add(station.getName());
				record.add(station.getNaPTANName());
				record.add(String.valueOf(station.getEasting()));
				record.add(String.valueOf(station.getNorthing()));
				record.add(String.valueOf(station.getYearlyUsage()));
				record.add(String.valueOf(station.getDayUsage()));
				record.add(String.valueOf(station.getRunDays()));
				record.add(station.getLADCode());
				record.add(station.getLADName());
				record.add(String.valueOf(station.getArea()));

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
	 * Saves zonal rail station demand to an output file.
	 * @param year Year of the data.
	 * @param outputFile Output file name (with path).
	 */
	public void saveZonalRailStationDemand(int year, String outputFile) {

		LOGGER.debug("Saving zonal rail station demand to a file.");
		
		HashMap<String, Integer> zonalUsageTotal = this.calculateYearlyZonalUsageTotal();
		HashMap<String, Integer> zonalUsageAverage = this.calculateYearlyZonalUsageAverage();
		HashMap<String, Double> dailyZonalUsageTotal = this.calculateDailyZonalUsageTotal();
		HashMap<String, Double> dailyZonalUsageAverage = this.calculateDailyZonalUsageAverage();
		HashMap<String, List<RailStation>> zoneToList = this.createListOfStationsWithinEachLAD();

		String NEW_LINE_SEPARATOR = "\n";
		ArrayList<String> outputHeader = new ArrayList<String>();
		outputHeader.add("year");
		outputHeader.add("LADcode");
		outputHeader.add("yearTotal");
		outputHeader.add("yearAvg");
		outputHeader.add("dayTotal");
		outputHeader.add("dayAvg");
		outputHeader.add("stationsNo");
		
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);
		try {
			fileWriter = new FileWriter(outputFile);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			csvFilePrinter.printRecord(outputHeader);
			ArrayList<String> record = new ArrayList<String>();

			for (String zone: zonalUsageTotal.keySet()) {
				record.clear();
				record.add(Integer.toString(year));
				record.add(zone);
				record.add(Integer.toString(zonalUsageTotal.get(zone)));
				record.add(Double.toString(zonalUsageAverage.get(zone)));
				record.add(Double.toString(dailyZonalUsageTotal.get(zone)));
				record.add(Double.toString(dailyZonalUsageAverage.get(zone)));
				record.add(Integer.toString(zoneToList.get(zone).size()));
				
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
	 * Sorts stations on station name in an ascending order.
	 */
	private void sortStationsOnName() {

		Comparator<RailStation> c = new Comparator<RailStation>() {
			public int compare(RailStation s, RailStation s2) {
				String name = s.getName();
				String name2 = s2.getName();
				return name.compareTo(name2);
			}
		};

		Collections.sort(this.railDemandList, c);
	}

	/**
	 * Sorts stations on usage in a descending order.
	 */
	private void sortStationsOnUsage() {

		Comparator<RailStation> c = new Comparator<RailStation>() {
			public int compare(RailStation s, RailStation s2) {
				Integer usage = s.getYearlyUsage();
				Integer usage2 = s2.getYearlyUsage();
				return usage2.compareTo(usage);
			}
		};

		Collections.sort(this.railDemandList, c);
	}
}
