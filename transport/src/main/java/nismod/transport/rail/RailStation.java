package nismod.transport.rail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about a rail station. 
 * @author Milan Lovric
 *
 */
public class RailStation {
	
	private final static Logger LOGGER = LogManager.getLogger(RailStation.class);
	
	private int nlc; //National Location Code
	private RailModeType mode;
	private String stationName;
	private String naptanName; //longer NaPTAN name
	private int easting;
	private int northing;
	private int yearUsage;
	private double dayUsage;
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
	public RailStation(int nlc, RailModeType mode, String stationName, String naptanName, int easting, int northing, int yearUsage, double dayUsage, int runDays, String ladCode, String ladName) {
		
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
	 * Constructor for a station.
	 * @param station Rail station which data is going to be copied.
	 */
	public RailStation(RailStation station) {
		
		this(station.getNLC(), station.getMode(), station.getName(), station.getNaPTANName(),
			 station.getEasting(), station.getNorthing(), station.getYearlyUsage(), 
			 station.getDayUsage(), station.getRunDays(), station.getLADCode(), station.getLADName());
	}
	
	/**
	 * Getter method for the NLC (National Location Code) of the station.
	 * @return NLC.
	 */
	public int getNLC() {
		
		return this.nlc;
	}
	
	/**
	 * Getter method for the rail mode type.
	 * @return Rail mode type.
	 */
	public RailModeType getMode() {
		
		return this.mode;
	}
	
	/**
	 * Getter method for the station name.
	 * @return Name.
	 */
	public String getName() {
		
		return this.stationName;
	}
	
	/**
	 * Getter method for the station NaPTAN name.
	 * @return NaPTAN name.
	 */
	public String getNaPTANName() {
		
		return this.naptanName;
	}
	
	/**
	 * Getter method for yearly usage.
	 * @return Yearly usage.
	 */
	public int getYearlyUsage() {
		
		return this.yearUsage;
	}
	
	/**
	 * Setter method for yearly usage.
	 * @param usage Yearly usage.
	 */
	public void setYearlyUsage(int usage) {
		
		this.yearUsage = usage;
	}
	
	/**
	 * Setter method for daily usage.
	 * @param usage Daily usage.
	 */
	public void setDailyUsage(double usage) {
		
		this.dayUsage = usage;
	}
	
	/**
	 * Getter method for daily usage.
	 * @return Daily usage.
	 */
	public double getDayUsage() {
		
		return this.dayUsage;
	}
	
	/**
	 * Getter method for number of operational days.
	 * @return Number of operaiontal days.
	 */
	public int getRunDays() {
		
		return this.runDays;
	}
	
	/**
	 * Getter method for easting.
	 * @return Easting.
	 */
	public int getEasting() {
		
		return this.easting;
	}
	
	/**
	 * Getter method for Northing.
	 * @return Northing.
	 */
	public int getNorthing() {
		
		return this.northing;
	}
	
	/**
	 * Getter method for the LAD code in which station is located.
	 * @return LAD code.
	 */
	public String getLADCode() {
		
		return this.ladCode;
	}
	
	/**
	 * Getter method for the LAD name in which station is located.
	 * @return LAD name.
	 */
	public String getLADName() {
		
		return this.ladName;
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
		sb.append(String.format("%.2f", this.dayUsage));
		sb.append(", ");
		sb.append(this.runDays);
		sb.append(", ");
		sb.append(this.ladCode);
		sb.append(", ");
		sb.append(this.ladName);

		return sb.toString();
	}

}
