package nismod.transport.air;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class stores information about an airport. 
 * @author Milan Lovric
 *
 */
public class Airport {
	
	private final static Logger LOGGER = LogManager.getLogger(Airport.class);
	
	private String iataCode; // three letter code
	private String caaName; //airport name in CAA dataset
	private String naptanName; //longer NaPTAN name (for UK airports only)
	private String ourAirportsName; //longer name (from ourAirports dataset)
	private int easting; //for UK airports
	private int northing; //for UK airports
	private double longitude;
	private double latitude;
	private String ladCode; //for UK airports
	private String ladName; //for UK airports
	private Locale country;
	private int terminalCapacity;
	private int runwayCapacity;
	
	public static enum AirportType {
		UK, //United Kingdom
		EU, //International (EU)
		INT //International (non-EU)
	}
	
	/**
	 * Constructor for the airport.
	 * @param iataCode Airport IATA code.
	 * @param caaName Airport name in CCA datasets.
	 * @param naptanName Airport name in NaPTAN (for UK airpots).
	 * @param ourAirportsName Airport name in ourAirports dataset.
	 * @param easting Easting coordinate.
	 * @param northing Northing coordinate.
	 * @param longitude Longitude coordinate.
	 * @param latitude Latitude coordinate.
	 * @param ladCode LAD code of the zone in which the airport is located (for UK airports).
	 * @param ladName LAD name of the zone in which the airport is located (for UK airports).
	 * @param countryCode Code of the country in which the airport is located.
	 * @param terminalCapacity Airport terminal capacity (max number of passengers that can be processed).
	 * @param runwayCapacity Airport runway capacity (max number of flights that can be processed).
	 */
	public Airport(String iataCode, String caaName, String naptanName, String ourAirportsName, int easting, int northing, 
				   double longitude, double latitude, String ladCode, String ladName, String countryCode, int terminalCapacity, int runwayCapacity) {
		
		this.iataCode = iataCode;
		this.caaName = caaName;
		this.naptanName = naptanName;
		this.ourAirportsName = ourAirportsName;
		this.easting = easting;
		this.northing = northing;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ladCode = ladCode;
		this.ladName = ladName;
		this.country = new Locale("", countryCode);
		this.terminalCapacity = terminalCapacity;
		this.runwayCapacity = runwayCapacity;
	}
	
	/**
	 * Constructor for an airport.
	 * @param airport Airport which data is going to be copied.
	 */
	public Airport(Airport airport) {
		
		this(airport.getIataCode(), airport.getCAAName(), airport.getNaPTANName(), airport.getOurAirportsName(),
			 airport.getEasting(), airport.getNorthing(), airport.getLongitude(), airport.getLatitude(), airport.getLADCode(),
			 airport.getLADName(), airport.getCountry().getISO3Country(), airport.getTerminalCapacity(), airport.getRunwayCapacity());
	}
	
	/**
	 * Getter method for the IATA code of the station.
	 * @return IATA code.
	 */
	public String getIataCode() {
		
		return this.iataCode;
	}
	
	/**
	 * Getter method for the airport CAA name.
	 * @return CAA name.
	 */
	public String getCAAName() {
		
		return this.caaName;
	}
	
	/**
	 * Getter method for the airport NaPTAN name.
	 * @return NaPTAN name.
	 */
	public String getNaPTANName() {
		
		return this.naptanName;
	}
	
	/**
	 * Getter method for the ourAirports name.
	 * @return OurAirports name.
	 */
	public String getOurAirportsName() {
		
		return this.ourAirportsName;
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
	 * Getter method for longitude.
	 * @return Longitude.
	 */
	public double getLongitude() {
		
		return this.longitude;
	}
	
	/**
	 * Getter method for Latitude.
	 * @return Latitude.
	 */
	public double getLatitude() {
		
		return this.latitude;
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
	
	/**
	 * Getter method for the country in which the airport is located.
	 * @return Country locale.
	 */
	public Locale getCountry() {
		
		return this.country;
	}
	
	/**
	 * Getter method for the airport terminal capacity.
	 * @return Terminal capacity.
	 */
	public int getTerminalCapacity() {
		
		return this.terminalCapacity;
	}
	
	/**
	 * Getter method for the airport runway capacity.
	 * @return Runway capacity.
	 */
	public int getRunwayCapacity() {
		
		return this.runwayCapacity;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(this.iataCode);
		sb.append(", ");
		sb.append(this.caaName);
		sb.append(", ");
		sb.append(this.naptanName);
		sb.append(", ");
		sb.append(this.ourAirportsName);
		sb.append(", ");
		sb.append(this.easting);
		sb.append(", ");
		sb.append(this.northing);
		sb.append(", ");
		sb.append(this.longitude);
		sb.append(", ");
		sb.append(String.format("%.2f", this.latitude));
		sb.append(", ");
		sb.append(this.ladCode);
		sb.append(", ");
		sb.append(this.ladName);
		sb.append(", ");
		sb.append(this.country.getISO3Country());
		sb.append(", ");
		sb.append(this.country.getDisplayName());
		sb.append(", ");
		sb.append(this.terminalCapacity);
		sb.append(", ");
		sb.append(this.runwayCapacity);

		return sb.toString();
	}

}
