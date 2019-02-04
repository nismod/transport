package nismod.transport.rail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about a station. 
 * @author Milan Lovric
 *
 */
public class Station {
	
	private final static Logger LOGGER = LogManager.getLogger(Station.class);
	
	private int nlc; //National Location Code
	private RailModeType mode;
	private String stationName;
	private String naptanName; //longer NaPTAN name
	private int easting;
	private int northing;
	private int yearUsage;
	private int dayUsage;
	private int runDays;
	private String ladCode;
	private String ladName;
	
	public static enum RailModeType {
		NRAIL, //National Rail (including London Overground, TfL rail)
		TUBE, //London Underground
		DLR, //Docklands Light Railway
		TRAM //Trams
	}
	
	/**
	 * Constructor for a station.
	 * @param nlc National Location Code.
	 * @param mode Which mode is served by this station.
	 * @param stationName Station name.
	 * @param naptanName Longer name from NaPTAN.
	 * @param easting Easting coordinate.
	 * @param northing Northing coordinate.
	 * @param yearUsage Yearly station usage (entries and exits combined).
	 * @param dayUsage Daily station usage (yearly usage divided by the number of operational days in a year).
	 * @param runDays The number of operational days in a year.
	 * @param ladCode LAD code of the zone in which the station is located.
	 * @param ladName LAD name of the zone in which the station is located.
	 */
	public Station(int nlc, RailModeType mode, String stationName, String naptanName, int easting, int northing, int yearUsage, int dayUsage, int runDays, String ladCode, String ladName) {
		
		this.nlc = nlc;
		this.mode = mode;
		this.stationName = stationName;
		this.naptanName = naptanName;
		this.easting = easting;
		this.northing = northing;
		this.yearUsage = yearUsage;
		this.dayUsage = dayUsage;
		this.runDays = runDays;
		this.ladCode = ladCode;
		this.ladName = ladName;
	}
	
	/**
	 * Getter method for the NLC (National Location Code) of the station.
	 * @return NLC.
	 */
	public int getNLC() {
		
		return this.nlc;
	}
	
	/**
	 * Getter method for the station name.
	 * @return Name.
	 */
	public String getName() {
		
		return this.stationName;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.nlc);
		sb.append(", ");
		sb.append(this.mode);
		sb.append(", ");
		sb.append(this.stationName);
		sb.append(", ");
		sb.append(this.naptanName);
		sb.append(", ");
		sb.append(this.easting);
		sb.append(", ");
		sb.append(this.northing);
		sb.append(", ");
		sb.append(this.yearUsage);
		sb.append(", ");
		sb.append(this.dayUsage);
		sb.append(", ");
		sb.append(this.runDays);
		sb.append(", ");
		sb.append(this.ladCode);
		sb.append(", ");
		sb.append(this.ladName);

		return sb.toString();
	}

}
